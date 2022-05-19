#### AOP

*AOP介绍
AOP应用


1、什么是AOP? OOP?   
AOP是Aspect Oriented Programming，即面向切面编程。OOP 即面向对象编程。OOP主张将功能模块化，对象化；而AOP主张针对同一类问题的统一处理，通过预编
译或运行时实现程序功能。

2、使用场景   
权限验证、登录判断、埋点、监控(启动时间，路径，日志)、时间抖动、组件化....

3、AOP实现(编译时织入、运行时织入)   
AOP实现可分为以下步骤：      
* 注解模块：声明指定功能的注解。比如一个登录拦截的注解，在需要使用登录拦截功能时，只添加上注解即可实现拦截功能；
* 辅助类辅助工具类：主要根据功能来设计，常见的设计有接口回调，管理类等等，主要用于沟通切面与主业务功能之间的联系；
* 切面模块：aop的核心。功能上就是完成需要重复判断编写的逻辑等等。需要有专门的框架知识储备(如AspectJ)，具体可以参考
  <https://blog.csdn.net/zhengchao1991/article/details/53391244>

4、核心实现(三大派系)  
AspectJ、Asm、Javassist

##### 3、AspectJ  
代码生成框架,在编译期(.java转换成.class)时生成class文件。通常在AOP实现使用。
[](AOP之AspectJ介绍.md)
[](AOP之AspectJ简单使用.md)

##### 4、Javassist   
修改字节码框架，在编译期(.class -> .dex)发挥作用。常用在 Gradle Transform。

##### 5、Asm   
同Javassist一样也是字节码修改工具，常用在 Gradle Transform。

##### 6、Hook  

