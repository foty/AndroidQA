#### 构建技术之APT

* 什么是APT
* APT使用场景  
* 实现APT

##### 1、APT(注释处理器)
> APT(Annotation Processing Tool的简称)，可以在代码编译期解析注解，并且生成新的java文件，减少手动的代码输入。现在有很多主流库都用上了APT，比
> 如Dagger2,ButterKnife,EventBus3,等等。一句话描述：apt在编译期间通过注解生成java文件代码的技术。

这里有提一下另一个东西--AOP。aop是一种编程思想，一种方法论。apt是一种代码生成技术(同属插桩技术)。更多看[AOP](构建技术之AOP.md)


##### 2、使用场景
编译期自动生成类，借助反射技术获取实例使用。金典案例如`ButterKnife`。


##### 3、实现APT
参考<https://www.jianshu.com/p/7af58e8e3e18>  
参考<https://blog.csdn.net/u010982507/article/details/121192988>


1、创建注解
> 一般来说，为了更好地管理代码，通常会创建一个新的Library库来保存注解类。  
创建注解，声明作用目标以及保存周期，例子如下：
```java
@Retention(RetentionPolicy.CLASS) //保留时间
@Target(ElementType.FIELD)  //作用目标
public @interface FindView {
    int value();
}
```
对于新建注解添加上@Retention、*@Target2个注解,其中：   
*@Retention* (保留周期，有3种选择)
> * SOURCE：只保留在源文件，当编译成.class文件时被编译器丢弃。
> * CLASS：能保留到.class文件中，当jvm加载class文件时被丢弃。
> * RUNTIME：不仅保留在.class文件中jvm加载完class文件后仍然保留。

*@Target* (作用目标)
> 常见的有：
> * METHOD：  方法
> * FIELD： 成员变量
> * TYPE：  类、接口、枚举、等
> * PACKAGE： 包


2、注解处理类
> 注释处理类首先要添加依赖，使用`auto-service`。在项目的build.gradle文件中添加
```text
// 这里是做了分库处理，注解处理类当中会使用，需要引用进来
implementation project(':apt-annotation')
implementation 'com.google.auto.service:auto-service:1.0-rc6'
// 解决auto-service因为gradle版本问题无法自动生成META-INF文件
annotationProcessor "com.google.auto.service:auto-service:1.0-rc6"
```
接下来是注解处理类的编写，继承`AbstractProcessor`类，重写它的几个重要方法，添加自己的逻辑。下面是一个例子。
```java
import com.google.auto.service.AutoService;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Messager messager; //用来打印message

    /**
     * 初始化函数，会被注解处理工具调用。一般通过ProcessingEnviroment获取其他有用的工具类，如
     * Elements, Types和Filer等等。
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
    }

    /**
     * 指定注解处理器是给哪个注解使用的(使用对象)。通常添加到set集合返回
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new LinkedHashSet<>();
        // 添加FindView注解类
        set.add(FindView.class.getCanonicalName());
        return set;
    }

    /**
     * 指定java版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 生成的java代码模板
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "构建开始。。。。。"); // Diagnostic类比Log作用

        //第一步、得到当前的所有注解集合(使用了@FindView 标记),主要是通过遍历
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(FindView.class);
        messager.printMessage(Diagnostic.Kind.NOTE, "elements.size(): " + elements.size());

        //先将得到的注解保存
        HashMap<Integer, String> eMap = new HashMap<>();

        for (Element e : elements) {
            messager.printMessage(Diagnostic.Kind.NOTE, "e.getSimpleName(): " + e.getSimpleName());
            messager.printMessage(Diagnostic.Kind.NOTE, "e.toString(): " + e.toString());
            //取出注解的内容(就是对应的id)
            FindView a = e.getAnnotation(FindView.class);
            eMap.put(a.value(), e.getSimpleName().toString());

            messager.printMessage(Diagnostic.Kind.NOTE, "a.value()= " + a.value());
            // 对象类型
            String type = e.asType().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "type= " + type);

            //2、生成java文件。有2种办法：字符串拼接与javapoet库自动生成

            //2-1、获取到包名: getQualifiedName()
            String packageName = elementUtils.getPackageOf(e).getQualifiedName().toString(); // 获取全路径。
            messager.printMessage(Diagnostic.Kind.NOTE, "packageName: " + packageName);
            //2-2、获取类名
            String className = e.getEnclosingElement().getSimpleName().toString();
            messager.printMessage(Diagnostic.Kind.NOTE, "className: " + className);

            try {
                // 创建一个这样的文件，参数为类名
                JavaFileObject file = processingEnv.getFiler().createSourceFile(className + "_ViewFinding");
                // 开启写入流
                Writer writer = file.openWriter();
                //写入相关内容
                writer.write(" package " + packageName + ";\n\n"); //包名
                writer.write(" public class " + className + "_ViewFinding {" + "\n"); //类名
                writer.write("    public void find (" + packageName + "." + e.getEnclosingElement().getSimpleName() //方法
                        + " root ) { \n");
                writer.write("        root." + e.getSimpleName().toString() + " = (" + type + ")" + "(((android.app.Activity) root)"
                        + ".findViewById(" + a.value() + "));\n");
                writer.write("    }\n");
                writer.write("\n");
                writer.write(" }");
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        messager.printMessage(Diagnostic.Kind.NOTE, "构建结束");

        //3、完成生成，返回true
        return true;
    }
}
```
一个注解类核心方法有4个：
* init  初始化。做一些初始化操作，如打印工具，日志工具等。
* getSupportedAnnotationTypes  指定生效的注解。以Set形式返回
* getSupportedSourceVersion  指定java版本。看情况返回；没有特殊要求返回最新版本即可
* process  生成代码模板。核心中的核心

> 这里要注意下的是：
> 1、处理类要使用`@AutoService(Processor.class)`注解，否则不会自动生成javax.annotation.processing.Processor文件。那么需要自己手动在
> resources\META-INF\services\目录下配置。过程麻烦不说还可能配置错误。使用该注解后会自动在 build\classes\java\...\META-INF\...下自动生成
> Processor文件。
> 2、Processor的包名是`javax.annotation`,包括里面所有使用的有同名的都选择这个包下的类。之前就因为包名不同导致注解一直无法生效。
> 3、在process()方法中生成代码模板，如果觉得一个一个单词拼接麻烦可以使用`javapoet`框架，更加方便快捷。`javapoet`需要添加依赖:
> `implementation 'com.squareup:javapoet:1.10.0'`
> 4、messager打印的日志如果无法看到，尝试将已经自动生成的类删除后重试；或者将项目内注解相关的`sourceCompatibility`,`targetCompatibility`
> 与`getSupportedSourceVersion`3者统一。


3、应用注解到项目
> 主模块添加注解类、注解处理类依赖后，通过反射方式获取自动创建的类的对象，执行它的方法即可。
```text
implementation project(':apt-annotation')
annotationProcessor project(':apt-processor')
```
反射类：
```java
public class BindUtil {
    public static void bindView(Activity activity) {
        Class clazz = activity.getClass();
        try {
            // 反射获取自动创建的类
            Class bindViewClass = Class.forName(clazz.getName() + "_ViewFinding");
            Method method = bindViewClass.getMethod("find", activity.getClass());
            //执行它的find方法，也就是findViewById操作。
            method.invoke(bindViewClass.newInstance(), activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
```
编译完成后可在 build\generated\ap_generated_source\.. 看到生成的带_ViewFinding后缀的类文件。 这样就实现了一个类似ButterKnife的框架了。

