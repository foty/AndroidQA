
##### Java核心基础



Java异常机制中，异常Exception与错误Error区别
序列Parcelable,Serializable的区别？
为什么Intent传递对象为什么需要序列化？

##### Java深入泛型与注解

Java的泛型中super 和 extends 有什么区别？
注解是什么？有哪些使用场景？

##### Java并发编程

ReentrantLock的实现原理
Synchronized的原理以及与ReentrantLock的区别。


##### Java虚拟机原理
描述JVM类加载过程
请描述new一个对象的流程
Java对象会不会分配到栈中？
GC的流程是怎么样的？介绍下GC回收机制与分代回收策略
Java中对象如何晋升到老年代？
判断对象是否被回收，有哪些GC算法，虚拟机使用最多的是什么算法？
Class会不会回收？用不到的Class怎么回收？
Java中有几种引用关系，它们的区别是什么？
描述JVM内存模型
StackOverFlow与OOM的区别？分别发生在什么时候，JVM栈中存储的是什么，堆存储的是什么？

##### Java反射类加载与动态代理
PathClassLoader与DexClassLoader的区别是什么？
什么是双亲委托机制，为什么需要双亲委托机制？
Android中加载类的方法有哪些？有什么区别？
ClassNotFound的有可能的原因是什么？
odex了解吗？解释型和编译型有什么区别？
说说反射的应用场景，哪些框架？
反射为什么慢？
动态代理是什么？如何实现？
动态代理的方法怎么初始化的？
CGLIB动态代理

##### 网络编程
请你描述TCP三次握手与四次挥手的过程与意义
谈谈你对TCP与UDP的区别是什么的理解
谈谈你对TCP 流量控制与拥塞控制的理解
谈谈你对Http与Https的关系理解
SSL握手的过程都经历过什么
谈谈你对Http的post与get请求区别的理解
输入一串URL到浏览器都经历过什么？
断点续传原理
如何保证下载文件的完整性

##### Kotlin
Kotlin内置标准函数let的原理是什么？
Kotlin语言的run高阶函数的原理是什么？
Kotlin语言泛型的形变是什么？
Kotlin协程在工作中有用过吗？

##### Android 高级UI
View的绘制原理
View绘制流程与自定义View注意点
自定义view与viewgroup的区别
View的绘制流程是从Activity的哪个生命周期方法开始执行的
Activity,Window,View三者的联系和区别
在onResume中是否可以测量宽高
如何更新UI，为什么子线程不能更新UI？
DecorView, ViewRootImpl,View之间的关系
自定义View执行invalidate()方法,为什么有时候不会回调onDraw()
invalidate() 和 postInvalicate() 区别

##### Android Framework
Android中多进程通信的方式有哪些？
描述下Binder机制原理？
为什么 Android 要采用 Binder 作为 IPC 机制？
Binder线程池的工作过程是什么样？
AIDL 的全称是什么？如何工作？能处理哪些类型的数据？
Android中Pid&Uid的区别和联系
Handler怎么进行线程通信，原理是什么？
ThreadLocal的原理，以及在Looper是如何应用的？
Handler如果没有消息处理是阻塞的还是非阻塞的？
handler.post(Runnable) runnable是如何执行的？

##### Android组件内核
Acitvity的生命周期，如何摧毁一个Activity?
Activity的4大启动模式，与开发中需要注意的问题，如onNewIntent() 的调用
Intent显示跳转与隐式跳转，如何使用？
Activity A跳转B，B跳转C，A不能直接跳转到C，A如何传递消息给C？
Activity如何保存状态的？
请描诉Activity的启动流程，从点击图标开始。
Service的生命周期是什么样的？
你会在什么情况下使用Service？
Service和Thread的区别？
IntentService与Service的区别？

##### Android性能优化
一张图片100x100在内存中的大小？
内存优化，内存抖动和内存泄漏。
什么时候会发生内存泄漏？举几个例子
Bitmap压缩，质量100%与90%的区别？
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
