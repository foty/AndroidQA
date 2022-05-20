### AspectJ简单使用

[参考](https://www.jianshu.com/p/9325a4f90605)

1、开始先添加依赖配置。  
在**项目根目录**下的`build.gradle`添加配置：
```groovy
buildscript {
    dependencies {
        // AspectJ
        classpath 'org.aspectj:aspectjtools:1.8.8'
        classpath 'org.aspectj:aspectjweaver:1.8.8'
    }
}
```
在**对应模块**下的`build.gradle`文件添加依赖：
```groovy
dependencies {
    implementation 'org.aspectj:aspectjrt:1.8.8'
}
```
同时在`android{}`块中添加配置代码：
```text
    // 配置AspectJ
    //获取log打印工具和构建配置
    final def log = project.logger
    final def variants = project.android.applicationVariants
    variants.all { variant ->
        if (!variant.buildType.isDebuggable()) {
            //判断是否debug，如果打release把return去掉就可以
            log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
//              return
        }
        //使aspectj配置生效
        JavaCompile javaCompile = variant.javaCompile
        javaCompile.doLast {
            String[] args = ["-showWeaveInfo",
                             "-1.8",
                             "-inpath", javaCompile.destinationDir.toString(),
                             "-aspectpath", javaCompile.classpath.asPath,
                             "-d", javaCompile.destinationDir.toString(),
                             "-classpath", javaCompile.classpath.asPath,
                             "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]
            log.debug "ajc args: " + Arrays.toString(args)

            MessageHandler handler = new MessageHandler(true);
            new Main().run(args, handler);
            //为了在编译时打印信息如警告、error等等
            for (IMessage message : handler.getMessages(null, true)) {
                switch (message.getKind()) {
                    case IMessage.ABORT:
                    case IMessage.ERROR:
                    case IMessage.FAIL:
                        log.error message.message, message.thrown
                        break;
                    case IMessage.WARNING:
                        log.warn message.message, message.thrown
                        break;
                    case IMessage.INFO:
                        log.info message.message, message.thrown
                        break;
                    case IMessage.DEBUG:
                        log.debug message.message, message.thrown
                        break;
                }
            }
        }
    }
```
以上是手动配置AspectJ的方式。还有就是别人集成好给我们使用的框架了。比如：
[Hujiang集成方案](https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx)
在**项目根目录**下的`build.gradle`添加配置：
```groovy
buildscript {
    dependencies {
        // 集成配置AspectJ
        classpath "com.hujiang.aspectjx:gradle-android-plugin-aspectjx:2.0.10"
    }
}
```
集成配置方法也需要添加依赖。在**模块(app)**下的`build.gradle`中文件添加依赖：
```groovy
dependencies {
    implementation 'org.aspectj:aspectjrt:1.8.8'
}
```
但是不需要在`android{}`块中添加配置了，直接引用插件。一般在`build.gradle`文件的顶部位置，添加`id 'android-aspectjx'`。
```groovy
plugins {
    // 项目其他插件...
    
    // 集成配置AspectJ
    id 'android-aspectjx'
}
```
如果在debug阶段注重编译速度，可以关闭代码织入。在模块下的 build.gradle 文件下
```text
aspectjx {
    //关闭AspectJX功能
    enabled false
}
```

2、开始编写代码逻辑。   
定义一个注解，用来标注切点。
```java
@Retention(RetentionPolicy.RUNTIME) // 保留策略，这里保存到class字节码级别
@Target(ElementType.METHOD)  //作用目标，这里是针对方法。
public @interface LoginFilter {
    int loginStatue() default 0; // 默认的状态，这个可有可无。
}
```
切面类编写。需要使用`@Aspect`标注。
```java
@Aspect
public class LoginFilterAspect {

    @Pointcut("execution(@com.example.aop.login.LoginFilter * * (..))") // 标注切点是谁。这里是指定LoginFilter这个注解,也可以指定某个方法
    public void LoginFilter() { // 声明找到这个切点时执行的函数
    }

    @Around("LoginFilter()") // 同上面的函数名称相同。表示在切入时执行下面的逻辑，还有 @Before 切入之前；@After 切入之后。
    public void handleLoginPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取用户实现的ILogin类，如果没有调用init()设置初始化就抛出异常。
        ILoginFilter iLoginFilter = LoginHelper.getInstance().getILoginFilter();
        if (iLoginFilter == null) throw new RuntimeException("ILoginFilter没有初始化");

        //先得到方法的签名methodSignature，然后得到@LoginFilter注解，如果注解为空，就不再往下走。
        Signature signature = joinPoint.getSignature();
        if (!(signature instanceof MemberSignature)) throw new RuntimeException("该注解只能用于方法上");

        MethodSignature methodSignature = (MethodSignature) signature;
        LoginFilter loginFilter = methodSignature.getMethod().getAnnotation(LoginFilter.class);
        if (loginFilter == null) return;

        //调用iLogin的isLogin()方法判断是否登录
        if (iLoginFilter.isLogin()) { //执行方法
            joinPoint.proceed();
        } else {
            iLoginFilter.login(loginFilter.loginStatue()); // 触发回调
        }
    }
}
```
在切面处理方法中，如果不拦截，想继续执行后面的方法，调用`joinPoint.proceed();`  
其他相关类：  
为了方便知道登录情况，编写一个接口，用来监听返回登录状态
```java
public interface ILoginFilter {

    /**
     * 登录逻辑
     * @param statue
     */
    void login(int statue);

    /**
     * 判断是否登录
     * @return
     */
    boolean isLogin();

}
```
一个辅助类，用于切面处理类与监听结果的桥梁。
```java
public class LoginHelper {
    private LoginHelper() {
    }

    private static LoginHelper instance;

    public static LoginHelper getInstance() {
        if (instance == null) {
            synchronized (LoginHelper.class) {
                if (null == instance) {
                    instance = new LoginHelper();
                }
            }
        }
        return instance;
    }

    private ILoginFilter iLoginFilter;

    public ILoginFilter getILoginFilter() {
        return iLoginFilter;
    }

    public void setILoginFilter(ILoginFilter iLoginFilter) {
        this.iLoginFilter = iLoginFilter;
    }
}
```
场景使用：初始化监听器。
```text
    private void initFilter() {
        ILoginFilter Filter = new ILoginFilter() {
            @Override
            public void login(int statue) { //拦截回调，登录失败回调
                switch (statue) {
                    case 0:
                        Toast.makeText(MainActivity.this, "其他问题", Toast.LENGTH_SHORT).show();
                        break;
                    case -1:
                        Toast.makeText(MainActivity.this, "状态验证失败，请先去登录！", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public boolean isLogin() { // 返回了false，不会执行click内的逻辑，而是回到到上面的login方法
                String s = tvEdit.getText().toString().trim();
                int num;
                if (TextUtils.isEmpty(s)) num = 1;
                try {
                    num = Integer.parseInt(s);
                } catch (Exception e) {
                    num = 1;
                }
                return num % 2 == 0;
            }
        };
        LoginHelper.getInstance().setILoginFilter(Filter);
    }
```
登录结果方法，添加注解
```text
    @LoginFilter(loginStatue = -1)
    private void extracted() {
        Toast.makeText(this, "状态验证成功，继续执行点击事件", Toast.LENGTH_LONG).show();
    }
```
最后在登录结果处理调用`extracted()`就可以了。
