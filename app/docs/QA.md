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

##### 已知“onSaveInstanceState 会在系统意外杀死 Activity 时调用，或者横纵屏切换的时候调用”。那么随着Android SDK版本的变化，这一方法的调用时机有哪些变化？     

Activity的onSaveInstanceState回调时机，取决于app的targetSdkVersion：    
* targetSdkVersion低于11的app，onSaveInstanceState方法会在Activity.onPause之前回调；
* targetSdkVersion低于28的app，则会在onStop之前回调；
* 28之后，onSaveInstanceState在onStop回调之后才回调
<p>

##### 内存泄漏  
* 原因
* 解决方案

引起内存泄漏的常见地方或者操作有handler，单例，(广播、消息事件)注册与反注册、Cursor操作，IO流操作等。

<p>

##### Activity的启动流程 (API 28)
启动流程涉及到以下多个点
* android 中的消息机制(另开篇幅handler)
* 同步屏障(消息机制。参见Handler.md)
* binder通信(另开篇幅)
* zygote进程、system_server进程、AMS、Launcher启动 (见启动流程.md)
* ActivityThread与Application (见启动流程.md)
<p>

##### Handle消息机制
详情见Handler.md部分，以及相关问题。

<p>

##### Binder了解
见<TestLink>项目下的《binder》部分，以及blog: https://blog.csdn.net/universus/article/details/6211589

<p>

##### View的绘制流程与自定义view手法 (API 30)
从源码开始到view有以下几个点：
* Window、WindowManager和WMS(WindowManagerService)
* view的绘制

以上详情见View.md

<p>

##### 并发(锁)
注意比较常见的同步代码的手段如：volatile，synchronize，cas。了解他们的概念，工作原理。

<p>

##### 下一课题ing