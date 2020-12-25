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

> fork() 机制   
父进程通过 fork() 可以孵化出一个子进程。相当于是一个进程变成了两个进程。同时具有以下特点：  
1.这两个进程代码一致，而且代码执行到的位置也一致。  
2.区别是进程ID（PID）不一样。  
3.一次调用，两次返回。父进程返回的是子进程的 PID，从而让父进程可以跟踪子进程的状态，以子进程 PID 作为函数参数。子进程返回的是0。


###### 1、zygote进程与system_server进程
* zygote进程开始  
> Android系统是基于 Linux内核的，系统启动时，除去硬件相关，内核等的初始化工作后，系统会启动init进程。Linux系统(用户空间)中的
进程都是init 进程的子进程，Android 的zygote进程也是在系统启动的过程，由init进程创建的。Linux的init进程会执行到位于
platform/system/core目录下的init程序，这个init程序即是 Android 内的init进程对应的程序。文件路径为
platform/system/core/init.cpp。(系统版本不一样，所有文件路径或方法可能存在差异，以版本号为准，当前为8和9)这些底层文件都是由c++
实现。(个人学识有限，看不懂c++文件，只能照葫芦画瓢，翻翻源码，作作记录)。在 init.cpp的主函数main()方法中，使用
epoll机制+死循环维持init(Android)进程一直运行。主函数一部分解析了有关zygote进程的配置文件：
platform/system/core/rootdir/init.rc，从而fork出zygote进程。   
一句话概括：Linux的init进程执行到platform/system/core/init.cpp，在这个文件的主函数解析init.rc文件fork出zygote进程。

* zygote进程初始化
> zygote进程被孵化后会执行到platform/frameworks/base/cmds/app_process/app_main.cpp文件。app_main.cpp的主函数中
首先创建AppRuntime对象(继承至AndroidRuntime)，最后执行`runtime.start("com.android.internal.os.ZygoteInit", args, zygote)`
在frameworks/base/core/jni/AndroidRuntime.cpp文件主要执行工作有：   
1、调用AndroidRuntime的startVM()创建虚拟机;  
2、调用startReg()注册Android方法(JNI函数);   
3、JIN反射调用com.android.internal.os.ZygoteInit的main()方法;  
进入到ZygoteInit的main方法后，进程由c层(native层)进入到了java层。在ZygoteInit.java的main方法中主要完成的工作有：  
1、调用preload()预加载类和资源  
2、通过ZygoteServer()创建服务端的Socket对象(该对象会用于与AMS等服务的跨进程通信)   
3、fork出system_server进程   
4、调用runSelectLoop(..)进入无限等待过程。

* system_server进程



###### 2、Handle消息机制

###### 3、Binder了解
《binder》 https://blog.csdn.net/universus/article/details/6211589

##### View的绘制流程与自定义view手法


