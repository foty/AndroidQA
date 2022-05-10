感谢玩安卓<https://wanandroid.com/>问答板块提供方向以及相关资料，以此学习与总结。

##### Android 项目中asset目录和res的区别 
* res中存放可编译的资源文件,这种资源文件系统会在R.java里面自动生成该资源文件的ID,(除了raw,raw不会被编译);
* res下的文件夹都是有特殊含义的,如anim特指动画类,raw指音频类等等。不能顺便创建文件夹;
* res下的资源通过R.id.xx来访问;

* asset目录的打包后会原封不动的保存在apk包中，不会被编译成二进制;
* asset资源访问时使用AssetManager,以流形式操作;
* asset资源可以自由创建子文件夹，并且都有效，可以访问到;

<p>

##### Android下各API获取保存路径的实际文件路径
* Environment.getExternalStorageDirectory().getAbsolutePath():  SD卡根目录，即 /storage/emulated/0
* Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES): /storage/emulated/0/Pictures
* Context.getExternalCacheDir():  /storage/emulated/0/Android/data/应用包名/cache
* Context.getExternalFilesDir(Environment.DIRECTORY_PICTURES): /storage/emulated/0/Android/data/应用包名/files/Pictures
* Context.getCacheDir().getPath():  /data/user/0/应用包名/cache/MyPhoto

<p>

##### “onSaveInstanceState 会在系统意外杀死 Activity 时调用，或者横纵屏切换的时候调用”。那么随着Android SDK版本的变化，这方法的调用时机有哪些变化？
Activity的onSaveInstanceState回调时机，取决于app的targetSdkVersion：    
* targetSdkVersion低于11的app，onSaveInstanceState方法会在Activity.onPause之前回调；
* targetSdkVersion低于28的app，则会在onStop之前回调；
* 28之后，onSaveInstanceState在onStop回调之后才回调
<p>

##### 内存泄漏  
* 原因
* 解决方案
引起内存泄漏的常见地方或者操作有handler，单例，(广播、消息事件)注册与反注册，Cursor操作，IO流操作，Dialog，fragment等等。

<p>

##### Activity的启动流程 (API 28)
启动流程涉及到以下多个点
* android 中的消息机制(另开篇幅[](android/Handler.md))
* 同步屏障(消息机制。参见[](android/Handler.md))
* binder通信(另开篇幅)
* zygote进程、system_server进程、AMS、Launcher启动 (见[](framework/启动流程.md))
* ActivityThread与Application (见[](framework/启动流程.md))
<p>

##### Handle消息机制
详情见: [](android/Handler.md)部分，以及相关问题。

<p>

##### Binder了解
见<TestLink>项目下的《binder》部分，以及blog: https://blog.csdn.net/universus/article/details/6211589

<p>

##### View的绘制流程与自定义view手法 (API 30)
从源码开始到view有以下几个点：
* Window、WindowManager和WMS(WindowManagerService)
* view的绘制
详情见: [](android/View.md)

<p>

##### 并发(锁)
注意比较常见的同步代码的手段如：volatile，synchronize，cas。了解他们的概念，工作原理。
详细见: [](java/并发(锁).md)
<p>

##### 事件分发机制  
或许阅读recyclerview的源码对嵌套滚动会有更深刻的认识。
详细见 [](android/事件分发.md)
<p>

##### 全埋点技术系列
涉及到编译流程、字节码、ASM、gradle插件原理、APT等等
<p>

##### 谈谈gradle的Transform 和 APT(Annotation Processor Tool 注释处理器)   
[](base_build/构建技术之APT.md)
<p>

##### android中的动画(与view相关)
[](android/动画.md)
<p>

##### ANR是什么，产生的原因有哪些
ANR，即Application Not Responding，应用无响应。在AMS收到`SHOW_NOT_RESPONDING_UI_MSG`消息之后会弹出，产生原因有：
* 触摸事无响应(5s)
* Service超时(20s)
* 广播超时 (10s)
<p>

##### SharedPreferences有什么优缺点。
优点：
* 轻量，使用特简单方便；
* 以xml形式保存本地，软件卸载也会一并删除，无污染。

缺点：
* 存在磁盘读写，从内存保存到磁盘，或者从磁盘读取到内存，IO瓶颈；
* 多线程情况下效率低下；
* 不支持跨进程；
* 不适合存储大量数据；

改良：  
从commit()(同步写入:提交一次，写入到磁盘一次)提交优化到使用apply()提交(并非每次提交都会写入到磁盘，异步写入)。
需要跨进程，可以使用微信开源的MMKV。
<p>

##### 编译速度优化(具体方面，考虑实操)。
* transform增量编译 (最难的)
* 不使用动态版本号
* 源码抽成library放maven
* 修改开发工具配置
<p>

##### 哪些 Context调用 startActivity 需要设置NEW_TASK,为什么   
BroadcastReceiver，Service，Application，ContentProvider 中的Context启动activity都是需要设置NEW_TASK。原因如下：
1、Context调用startActivity(),实际上是它的实现类ContextImpl来启动的。也就是说在Context的包装类ContextWrapper中的mBase是ContextImpl的实例。 
2、在ContextImpl启动activity会检查是否含有NEW_TASK这一个flag。如果没有将会抛出异常。都知道activity是运行在任务栈中的，上面组件本身都没有任务栈，
所以必须要依附一个任务栈。
3、另外补充为什么Activity启动不需要设置。因为它集成Context后重写了startActivity()方法，默认新启动的activity属于自己同一个任务栈(4种启动模式中
的默认模式)。
<p>

##### 同步屏障问题?
看[](android/Handler.md)，要了解是什么，拿来干什么。
<p>

##### "Android16.6ms刷新一次屏幕"引出 View中VSYNC内容
[](android/View.md)的 vsync 部分
<p>

##### 泛型擦除?  
泛型类型在代码编译时会被擦除掉(一般情况下会被擦成Object类型，如果使用了上限通配符的话会被擦成extends右边的类型)。这么做的意义：
1、可以在使用泛型的时候根据传入泛型类型的不同而使用对应类型的API；
2、可以解决不必要的类型转换错误；    
为什么编译时要擦除泛型?那为什么在运行后还能获取到?  
编译时擦除泛型主要是为了兼容性吧，泛型在1.5版本后引入，兼容以前的版本。在运行后还能获取是因为编译器自动强转了类型。(查看前后字节码就能看到)，泛型擦除
并不是真正的擦除，只是为了需要在运行时擦除泛型，编译时泛型类型还是会保存下来的。
<p>

##### 匿名内部类访问的外部类局部变量为什么要用final修饰，jdk8为啥不需要了。
内部类使用的外部类局部变量是通过构造函数传递过去的，内部类使用的引用和外部类使用的并不是同一个(虽然值相同)。如果局部变量不是final的话，就可能会被修改，
就会导致内部类和外部类使用的变量不同步，所以需要添加final，不允许重新赋值。jdk8是自动添加了final，和原来的一样(语法糖)。
<p>

##### 事件分发机制中的分发机制顺序默认是逆序的，有没有方法可以修改分发顺序？
第一种： 重写dispatchTouchEvent()方法。
第二种：
1、调用ViewGroup#setChildrenDrawingOrderEnabled(true)来开启自定义顺序;
2、重写ViewGroup#getChildDrawingOrder()方法来决定返回的子View;
<p>

##### ButterKnife原理
后续再补原理。(更多黑科技看这儿<https://mp.weixin.qq.com/s/VIsip3Dw8LLyqBLUrgzVFA>)

<p>

###### Gradle中，BuildConfig这个类是如何生成的？
编译器自动生成的

<p>

##### Parcelable为什么效率会高于Serializable




