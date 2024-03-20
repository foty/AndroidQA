
##### Java核心基础

Java异常机制中，异常Exception与错误Error区别
序列Parcelable,Serializable的区别？
为什么Intent传递对象为什么需要序列化？

##### Java深入泛型与注解

Java的泛型中super 和 extends 有什么区别？
注解是什么？有哪些使用场景？

##### Java并发编程



##### Java虚拟机原理

StackOverFlow与OOM的区别？分别发生在什么时候，JVM栈中存储的是什么，堆存储的是什么？

##### Java反射类加载与动态代理

动态代理是什么？如何实现？
动态代理的方法怎么初始化的？
CGLIB动态代理


##### 网络编程




##### Kotlin



##### Android 高级UI



自定义View执行invalidate()方法,为什么有时候不会回调onDraw()


##### Android Framework


Binder线程池的工作过程是什么样？
AIDL 的全称是什么？如何工作？能处理哪些类型的数据？
Android中Pid&Uid的区别和联系

Handler如果没有消息处理是阻塞的还是非阻塞的？

##### Android组件内核

Activity如何保存状态的？
请描诉Activity的启动流程，从点击图标开始。



##### Android性能优化
一张图片100x100在内存中的大小？  占用的内存大小公式：分辨率 * 每个像素点的大小
内存优化，内存抖动和内存泄漏。
什么时候会发生内存泄漏？举几个例子
Bitmap压缩，质量100%与90%的区别？  质量压缩：改变图片的位深及透明度
TraceView的使用，查找CPU占用
内存泄漏查找
Android四大组件(以及Application)的onCreate/onReceiver方法中Thread.sleep()，会产生几个ANR?
当前项目中是如何进行性能优化分析的
冷启动、热启动的概念
优化View层次过深问题，选择哪个布局比较好？

##### 开源框架
组件化在项目中的意义
组件化中的ARouter原理
谈一下你对APT技术的理解
谈谈Glide框架的缓存机制设计
Android项目中使用Glide框架出现内存溢出，应该是什么原因？
Android如何发起网络请求，你有用过相关框架码？OkHttp框架解决了你什么问题？
RxJava框架线程切换的原理，RxJava1与RxJava2有哪些区别？
谈谈LiveData的生命周期是怎么监听的?
