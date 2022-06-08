### Javassist

[github官网项目](https://github.com/jboss-javassist/javassist)


* 1、是什么
* 2、能干什么
* 3、怎么用

#### 是什么

Javassist 是一个修改字节码操作的框架。在编译期(.class ->.dex **尚未编译成.dex之前**)实现代码注入。并且它有源代码级别的api，可以不需要很了解字节码知识。
另外Javassist的作用期与Gradle编译作用契合，所以Javassist通常配合gradle一起使用。比如自定义插件等。


#### 能干什么

参考[aop作用](构建技术之AOP.md)。


#### 怎么用

第一步写个插件
第二步编写javassist逻辑

参考   
[使用资料I-语法API](https://www.jianshu.com/p/43424242846b)
[案例实战I](https://www.jianshu.com/p/33d8a3165b07)
