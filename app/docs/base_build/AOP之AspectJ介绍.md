### AspectJ 介绍

* 1、是什么
* 2、能干什么
* 3、怎么用

#### 1、是什么

是一个AOP织入框架(静态织入),在编译期注入代码(.java -> .class过程)。 [AspectJ官方](http://www.eclipse.org/aspectj/)

优点：

* 切入点固定
* 正则表达式
* 性能偏低

#### 2、能干什么

- 统计埋点
- 日志打印/打点
- 数据校验
- 行为拦截
- 性能监控
- 动态权限控制

#### 3、怎么用

使用AspectJ一定要先了解几个知识：

* 切面：使用`@Aspect`标识的类称之为一个切面。一个切面通常包含一个独立的功能实现。
* 切点：声明需要织入的位置。
* 通知：对声明切点的监听，一般把需要织入的功能逻辑代码编写在这里。

##### 3.1切面

这个没什么好说的，切记使用`@Aspect`标识声明是一个切面即可。

##### 3.2切点
所谓切点，就是有特殊标志`@PointCut`标注的方法。如：
```text
    @Pointcut("execution(xxxxx * * (..))")
    public void LoginFilter() {}
```
其中：
* 方法名称可以任意
* 必须使用`@Pointcut()`注解修饰
* 注解内的value是一种正则表达式的匹配规则

关于正则匹配：通常格式为：`@Pointcut("[切点关键字] ([限定符] [静态修饰] [final] [返回值类型] [包名.类..方法(参数...)] [throws 异常类型])")`

* @Pointcut：切点固定标识
* 关键字：常用的就是`execution`，用来匹配方法执行的连接点。还有`call`,`within`，`target`等。
* 限定符：`public`、`protected`、`private`
* 静态：`static`
* final关键字：`final`
* 返回值：返回值类型，多数情况使用`*`通配符，表示所有类型。
* [包名.类.方法(参数...)]：具体路径。可以是具体的一个方法或一个类或一个注解。
* throws 异常类型：抛出指定异常。

比如这个切点：
```text
@Pointcut("execution(void androidx.appcompat.app.AppCompatActivity.onCreate(..))")
public void activityOnCreate() {
}
```
表示：AppCompatActivity的onCreate()方法执行。

其他常用的关键字含义：

* execution：用来匹配方法执行的连接点。使用频率最高。
* call：用来匹配方法回调的连接点。有点类似execution。都是作用在方法上。
* within：用来匹配指定类型内的连接点，可以是类名，也可以是包名；如：`@Pointcut("within(xxx.xxx.xxx.xx *)")`
* target：用来匹配当前目标对象的连接点；注意是目标对象的匹配；

[更多切点用法](https://blog.csdn.net/zhengchao1991/article/details/53391244)
[切点用法II](https://blog.51cto.com/u_12004792/3138400)
[切点用法III](https://www.jianshu.com/p/49d2be4c508d)

##### 3.3通知
通知表示对切点的监听处理。处理逻辑一般写在这个位置。  

通知的标识有 ``@Before``，``@After``，``@Around``。分别代表在切点执行前，执行后，执行中。


###### 3.4
实现一个AOP大概可分为以下步骤：

* 1、注解模块：声明指定功能的注解。比如一个登录拦截的注解，在需要使用登录拦截功能时，只添加上注解即可实现拦截功能；

* 2、辅助类辅助工具类：主要根据功能来设计，常见的设计有接口回调，管理类等等，主要用于沟通切面与主业务功能之间的联系；

* 3、切面模块：aop的核心。功能上就是切点，通知的编写。需要有专门的框架知识储备(如AspectJ)。
  
[](https://www.jianshu.com/p/49d2be4c508d)
[简单例子](AOP之AspectJ简单使用.md)
[AspectJ.pdf](https://github.com/hyvenzhu/Android-Demos/blob/master/AspectJDemo/AspectJ.pdf)
