kotlin（协程） [](kotlin/Kotlin基础.md)

HashMap [](data_structure/HashMap.md)、ArrayList [](data_structure/ArrayList.md)

自定义view、view的绘制流程 [](android/View.md)、事件分发机制 [](android/事件分发.md)

handler [](android/Handler.md)

tcp/ip、http [](http/net.md)

jetpack（viewBinding、viewModel [](jetpack/ViewModel.md)、lifecycle [](jetpack/Lifecycle.md)）

锁、Synchronized [](java/并发(锁).md)、线程池 [](java/线程池.md)

liveData [](third_frame/livedata.md)

okhttp [](third_frame/OkHttp3.md)、glide [](third_frame/glide.md)


设计模式（观察者、装饰）
观察者模式：又称发布/订阅模式。定义一种一对多的依赖关系，让一个对象状态发生改变，所有具有依赖关系的对象都能够得到通知。
优点：
观察者与被观察者是抽象耦合，容易扩展
缺点：
通知顺序问题。避免出现循环通知；
观察者数量问题。观察者数量一旦非常多，通知所有的观察者的效率会降低。

装饰模式：
在不改变现有对象结构的情况下，动态地增加对象额外功能。
优点：
可以动态的扩展功能，同时也可以选择不同的装饰器实现不同功能，结构灵活。
缺点：
业务庞大，装饰层很多，维护困难。
耦合性问题。装饰一般使用继承或者实现关系。


mvp、mvvm
MVVM的本质是数据驱动编程。view层、model层完全隔离，view层与viewModel层也是完全隔离，通过通知、databind实现数据驱
动UI。
MVP其实就是MVC的一种升级。本质上它并没有解决代码耦合的问题，P层必须持有V层实例，才能刷新UI。只不过是从前UI逻辑在视图控
制器中写，现在搬到了Presenter中写而已。

内存泄露
内存泄露的原因：
当需要被回收变量的内存还被其他变量引用持有，导致内存回收失败的现象称之为内存泄露。内存泄露的本质就是长生命周期的对象持有
了短生命周期对象的引用。导致短生命周期对象结束时无法被回收。
具体原因：
1、单例模式导致的内存泄漏。因为单例的生命周期和应用的生命周期是一致的，如果往单例模式里面传了一个生命周期比较短的对象，比
如Activity。
2、静态变量导致的内存泄漏。
3、Handler内存泄漏。一般指匿名内部类的Handler和具名Handler的handleMessage方法里面持有外部引用的时候才会导致内存
泄漏。
4、匿名内部类也会导致内存泄漏。匿名内部类会持有外部类的引用，如果在匿名内部类里面做了耗时操作而在合适的时间关闭，就会导
致Activity无法退出。
5、资源释放内存泄露。比如文件、数据库、流的打开和关闭，

内存泄露优化
[](third_frame/LeakCanary.md)


包体积优化
1、开启混淆，资源压缩。ProGuard
2、无用资源剔除。
3、图片处理。压缩。
4、移除无用第三方库。
5、so库优化。只保留一种类型的so库。armeabi
6、resources.arsc资源混淆。其实就是将资源路径变短，比如res/drawable/login变为r/d/l。开源工具AndResGuard。
7、so动态加载。按需加载，插件化思想。开源框架SoLoader。
8、插件化。也是按需加载。
8、dex文件分包优化。开源框架ReDex
<https://mp.weixin.qq.com/s/_gnT2kjqpfMFs0kqAg4Qig>

组件化 
组件化架构的目的就是让每个业务模块变得相对独立，各个组件在组件模式下可以独立开发调试，集成模式下又可以集成到“app壳工程”中，
从而得到一个具有完整功能的APP。
解决问题：
业务组件，如何实现单独运行调试 动态配置清单文件，启动入口，依赖
业务组件间 没有依赖，如何实现页面的跳转 ARouter
业务组件间 没有依赖，如何实现组件间通信/方法调用
业务组件间 没有依赖，如何获取fragment实例
业务组件不能反向依赖壳工程，如何获取Application实例、如何获取Application onCreate()回调(用于任务初始化)
<https://blog.csdn.net/dongrimaomaoyu/article/details/123204769>

模块化
就是将业务拆分成多个模块放在不同的Module里面，每个功能的代码都在自己所属的module中添加。
解决问题：
如何拆分项目
模块之间的通信 arouter
模块内的代码隔离
模块在调试与发布模式之间的切换

netty



