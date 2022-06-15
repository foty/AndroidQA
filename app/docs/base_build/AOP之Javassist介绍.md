### Javassist

[github官网项目](https://github.com/jboss-javassist/javassist)


* 1、是什么
* 2、能干什么
* 3、怎么用

#### 1、是什么

Javassist 是一个修改字节码操作的框架。在编译期(.class ->.dex **尚未编译成.dex之前**)实现代码注入。并且它有源代码级别的api，可以不需要很了解字节码知识。
另外Javassist的作用期与Gradle编译作用契合，所以Javassist通常配合gradle(transform)一起使用。比如自定义插件等。


#### 2、能干什么

参考[aop作用](构建技术之AOP.md)。


#### 3、怎么用
总共可以分为2个步骤：

1、写个插件(transform)，参考[](Gradle.md)、[](Transform.md)
2、编写javassist逻辑

##### 3.1 javassist修改

一个简单javassist使用例子
```text
class InjectByJavassist {
    static void inject(String path, Project project) {
        try {
            File dir = new File(path)
            if (dir.isDirectory()) {
                dir.eachFileRecurse { File file ->
                    if (file.name.endsWith('Activity.class')) {
                        doInject2(project, file, path)
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    private static void doInject2(Project project, File clsFile, String originPath) {
        println("[javassist] clsFile: $clsFile.absolutePath")
        println("[javassist] originPath: $originPath")

        String cls = new File(originPath).relativePath(clsFile).replace('/', '.')
        println("[javassist] cls: $cls")
        cls = cls.substring(0, cls.lastIndexOf('.class'))
        println("[javassist] after Cls: $cls")

        ClassPool pool = ClassPool.getDefault()
        // 加入当前路径
        pool.appendClassPath(originPath)
        pool.appendClassPath(project.android.bootClasspath[0].toString())

        CtClass cc = pool.get("com.example.compilebuildproject.MainActivity")
        CtMethod personFly = cc.getDeclaredMethod("onCreate")

        personFly.insertBefore("System.out.println(\"织入代码I\");")
        personFly.insertAfter("System.out.println(\"织入代码II\");")

        cc.writeFile(originPath)
        cc.detach()
    }
}    
```
步骤大概有

* 1、获取到目标文件的输出路径，提取到对应文件(路径格式化)
* 2、获取ClassPool对象操作字节码
* 3、添加相关文件路径到ClassPool
* 4、添加android相关的东西<相关jar、相关的包>
* 5、找到修改对象(类、方法)
* 6、修改
* 7、写入文件
* 8、释放资源

其中，前四个步骤是准备工作。查资料是说要构建这样一个环境，不然会报找不到相关类的错误。

完成之后，编译运行，能看到log输出的`织入代码I`,`织入代码II`信息。在`build/intermediates/javac/...Activity.java`能看到插入的2行输出语句。


参考   
[使用资料I-语法API](https://www.jianshu.com/p/43424242846b)
[案例实战I](https://www.jianshu.com/p/33d8a3165b07)
[javassist官方文档中文翻译](https://github.com/IndustriousSnail/javassist-learn)
