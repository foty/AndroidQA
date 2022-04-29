#### 构建技术之APT

* 什么是APT
* APT使用场景  
* 实现APT

##### 1、APT(注释处理器)
> APT(Annotation Processing Tool的简称)，可以在代码编译期解析注解，并且生成新的java文件，减少手动的代码输入。现在有很多主流库都用上了APT，比
> 如Dagger2,ButterKnife,EventBus3,等等。一句话描述：apt在编译期间通过注解生成java文件代码的技术。

这里有提一下另一个东西--AOP。aop是一种编程思想，一种方法论。apt是一种代码生成技术(同属插桩技术)。更多看[AOP](构建技术之AOP.md)


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
@Retention 保留周期，有3个选择：
* SOURCE：只保留在源文件，当编译成.class文件时被编译器丢弃。
* CLASS：能保留到.class文件中，当jvm加载class文件时被丢弃。
* RUNTIME：不仅保留在.class文件中jvm加载完class文件后仍然保留。

@Target 作用目标
> 常见的有：
* METHOD：  方法
* FIELD： 成员变量
* TYPE：  类、接口、枚举、等
* PACKAGE： 包

2、注解处理类
> 注解处理

3、应用注解到项目

