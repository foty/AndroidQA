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
* Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES):  /storage/emulated/0/Pictures
* Context.getExternalCacheDir():  /storage/emulated/0/Android/data/应用包名/cache
* Context.getExternalFilesDir(Environment.DIRECTORY_PICTURES):  /storage/emulated/0/Android/data/应用包名/files/Pictures
* Context.getCacheDir().getPath():  /data/user/0/应用包名/cache/MyPhoto

<p>

##### 已知“onSaveInstanceState 会在系统意外杀死 Activity 时调用，或者横纵屏切换的时候调用”。那么随着Android SDK版本的变化，这一方法的调用时机有哪些变化？     

Activity的onSaveInstanceState回调时机，取决于app的targetSdkVersion：    
* targetSdkVersion低于11的app，onSaveInstanceState方法会在Activity.onPause之前回调；
* targetSdkVersion低于28的app，则会在onStop之前回调；
* 28之后，onSaveInstanceState在onStop回调之后才回调
<p>

##### 内存泄漏  
引起内存泄漏的常见地方或者操作有handler，单例，(广播、消息事件)注册与反注册、Cursor操作，IO流操作等。


##### Activity的启动流程 
启动流程涉及到以下多个点
* android 中的消息机制
* binder通信
* zygote进程与system_server进程的启动
* 同步屏障(消息机制)

###### 1、zygote进程与system_server进程
* zygote进程开始  
> Android系统是基于 Linux 内核的，Linux 系统中的进程都是 init 进程的子进程，Android 的zygote进程也是在系统启动
的过程，由init进程创建的。系统启动时，除去硬件相关的初始化工作外，Linux的init进程会执行到位于
platform/system/core目录下的init程序，这个init程序即是 Android 内的init进程对应的程序。文件路径为
platform/system/core/init.cpp。这些底层文件都是由c++实现。个人学识有限，看不懂c++文件，依葫芦画瓢。

###### 2、Handle消息机制

###### 3、Binder了解
《binder》 https://blog.csdn.net/universus/article/details/6211589

##### View的绘制流程与自定义view手法


