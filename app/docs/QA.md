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
引起内存泄漏的常见地方或者操作有handler，单例，(广播、消息事件)注册与反注册、Cursor操作，IO流操作等。


##### Activity的启动流程 (API 28)
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

* 1、zygote进程  
> Android系统是基于 Linux内核的，系统启动时，除去硬件相关，内核等的初始化工作后，系统会启动init进程。Linux系统(用户空间)中的
进程都是init 进程的子进程，Android 的zygote进程也是在系统启动的过程，由init进程创建的。Linux的init进程会执行到位于
platform/system/core目录下的init程序，这个init程序即是 Android 内的init进程对应的程序。文件路径为
platform/system/core/init.cpp。(系统版本不一样，所有文件路径或方法可能存在差异，以版本号为准，当前为8和9)这些底层文件都是
由c++实现。(个人学识有限，看不懂c++文件，只能照葫芦画瓢，翻翻源码，作作记录)。在 init.cpp的主函数main()方法中，使用
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
>1、调用preload()预加载类和资源  
2、通过ZygoteServer()创建服务端的Socket对象(该对象会用于与AMS等服务的跨进程通信)   
3、调用forkSystemServer()fork出system_server进程   
4、直接运行fork的SystemServer的run()方法或者调用runSelectLoop(..)进入无限循环,等待通信请求返回再运行run()方法。

* 2、system_server进程
> system_server进程是由ZygoteInit的forkSystemServer()方法产生而来。从开始fork到system_server的初始化的流程:  
[ZygoteInit]forkSystemServer()-> [Zygote]forkSystemServer()-> [Zygote]nativeForkSystemServer() -> C层 ->
[ZygoteInit]handleSystemServerProcess() ->[ZygoteInit]zygoteInit() ->[RuntimeInit]applicationInit() ->
[RuntimeInit]findStaticMain() ->[SystemServer]main()。   

其中主要做的事情有：   
>1、C层: fork出system_server进程  
2、handleSystemServerProcess: 创建类加载器并赋予给当前线程  
3、zygoteInit: 通用信息配置(log,时区等)，zygote的初始化(JNI)  
4、applicationInit: 虚拟机参数设置(内存利用率)，解析参数argv(实际是ZygoteInit#forkSystemServer()的args[]参数)  
5、findStaticMain: 反射调用SystemServer的main方法。进入到SystemServer.java。   

SystemServer 的run方法里主要做的事情有：    
>1、调整时间，时区信息  
2、准备主线程looper  
3、加载android_servers.so库，初始化native服务  
4、初始化系统context  
5、创建、初始化系统服务管理器-SystemServiceManager  
6、启动相关服务(引导、核心、其他)  
7、Looper启动  

* 重点关注服务启动部分
> 一、引导服务#startBootstrapServices()
1、Installer  -需要在其他服务启动前完成前启动，以便能够创建有权限的目录   
2、DeviceIdentifiersPolicyService  -提供访问设备标识，需要提前注册访问设备标识策略   
3、ActivityManagerService  -(ams)   
4、PowerManagerService  -电源管理服务  
5、RecoverySystemService   -重启服务  
6、LightsService  -LEDs灯光管理服务  
7、DisplayManagerService   -显示管理服务(要在pms之前启动)  
8、PackageManagerService  -pms  
9、UserManagerService  -用户管理服务，创建 data/user/目录  
10、OverlayManagerService  -覆盖管理服务  
11、传感器服务，本地方法实现：native startSensorService()。并且使用独立线程完成，以便检查 

>二、核心服务#startCoreServices()   
1、BatteryService  -电池服务，跟踪电池电量，需要LightService  
2、UsageStatsService -使用状态服务，可跟踪应用程序使用统计  
3、WebViewUpdateService -webView相关，跟踪可更新的WebView是否处于就绪状态，并监视更新安装  
4、BinderCallsStatsService -跟踪在绑定器调用中花费的cpu时间

>三、其他服务#startOtherServices()，其他的服务很多，列举几个眼熟的  
1、NetworkManagementService  -网路相关  
2、WindowManagerService  -wms  
4、InputManagerService  -  
3、AlarmManagerService  -闹钟管理  


###### 2、ActivityManagerService启动
ActivityManagerService通常称AMS，主要管理应用进程的生命周期以及四大组件等。AMS是在初始化system_server进程时启动的： 
在[SystemServer]startBootstrapServices():  
```
 mActivityManagerService = mSystemServiceManager.startService(
 ActivityManagerService.Lifecycle.class).getService(); 1、启动ActivityManagerService
 mActivityManagerService.setSystemServiceManager(mSystemServiceManager); 2、设置SystemServiceManager,归入自己管理
 mActivityManagerService.setInstaller(installer); 3、
 ...
  mActivityManagerService.initPowerManagement();
 ...
 mActivityManagerService.setSystemProcess(); 

 
```
ActivityManagerService的start方法。
```
private void start() {
  removeAllProcessGroups(); //移除所有进程组
  mProcessCpuThread.start(); // CPU线程启动

  mBatteryStatsService.publish(); //关联电池服务
  mAppOpsService.publish(mContext);
  Slog.d("AppOps", "AppOpsService published");
  LocalServices.addService(ActivityManagerInternal.class, new LocalService());
  // Wait for the synchronized block started in mProcessCpuThread,
  // so that any other acccess to mProcessCpuTracker from main thread
  // will be blocked during mProcessCpuTracker initialization.
  try {
      mProcessCpuInitLatch.await();
  } catch (InterruptedException e) {
      Slog.wtf(TAG, "Interrupted wait during start", e);
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted wait during start");
  }
}
```
#setSystemServiceManager()与#setInstaller()都仅是为AMS自己的对象赋值。看setSystemProcess()：  



###### 、Handle消息机制

###### 、Binder了解
《binder》 https://blog.csdn.net/universus/article/details/6211589

##### View的绘制流程与自定义view手法


