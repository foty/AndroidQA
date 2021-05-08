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
* android 中的消息机制(另开篇幅)
* 同步屏障(消息机制。参见Handler部分)
* binder通信(另开篇幅)
* zygote进程
* system_server进程
* Launcher

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
一句话概括：Linux的init进程执行到platform/system/core/init.cpp，在这个文件的主函数解析init.rc文件fork出zygote进程。进程变化为：init(Linux) ->
init(Android) -> zygote。

* zygote进程初始化
> zygote进程被孵化后会执行到platform/frameworks/base/cmds/app_process/app_main.cpp文件。app_main.cpp的主函数中
首先创建AppRuntime对象(继承至AndroidRuntime)，最后执行`runtime.start("com.android.internal.os.ZygoteInit", args, zygote)`
在frameworks/base/core/jni/AndroidRuntime.cpp文件主要执行工作有：   
1、调用AndroidRuntime的startVM()创建虚拟机;  
2、调用startReg()注册Android方法(JNI函数);   
3、JIN反射调用com.android.internal.os.ZygoteInit的main()方法;   

进入到ZygoteInit的main方法后，进程由c层(native层)进入到了java层。在ZygoteInit.java的main方法中主要完成的工作有：  
>1、调用preload()预加载类和资源  
2、通过ZygoteServer()创建服务端的对象 LocalServerSocket (该对象会用于与AMS等服务的跨进程通信)   
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
6、启动相关服务(引导服务、核心服务、其他服务)  
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
ActivityManagerService通常称AMS，主要管理应用进程的生命周期以及四大组件等。AMS是在初始化system_server进程时启动的。AMS在服务
启动后都干了些什么:
在[SystemServer]startBootstrapServices():  
```
 mActivityManagerService = mSystemServiceManager.startService(
 ActivityManagerService.Lifecycle.class).getService(); 1、启动ActivityManagerService
 mActivityManagerService.setSystemServiceManager(mSystemServiceManager); 2、设置SystemServiceManager,归入自己管理
 mActivityManagerService.setInstaller(installer); 3、设置Installer
 ...
  mActivityManagerService.initPowerManagement(); 4、初始化电源管理
 ...
 mActivityManagerService.setSystemProcess(); //设置系统进程
```

[SystemServer]startCoreServices():
```
 mActivityManagerService.setUsageStatsManager(LocalServices.getService(UsageStatsManagerInternal.class));//
```

[SystemServer]startOtherServices():
```
 mActivityManagerService.installSystemProviders(); //安装系统Provider
 ...
 mActivityManagerService.setWindowManager(wm); //设置wm
 ...
 mActivityManagerService.systemReady(() -> {。。。}
 ...
 mActivityManagerService.startObservingNativeCrashes() // 启动native异常监听器
```

先从ActivityManagerService启动开始。服务启动必会执行到的start()方法。AMS实际执行的start()：
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

#setSystemServiceManager()与#setInstaller()都仅是为AMS自己内部对象赋值。看setSystemProcess()。  
[ActivityManagerService]setSystemProcess()这个方法主要有  
1、将AMS注册到 ServiceManager。方便统一管理  
2、其他服务注册到 ServiceManager，如activity、procstats、meminfo、gfxinfo、dbinfo、cpuinfo、permission、processinfo。  
3、创建ProcessRecord对象维护当前进程的相关信息。   

[ActivityManagerService]systemReady(..)，主要是完成AMS的最后收尾工作   
1、调起一些关键服务(如AppOpsService)SystemReady()相关的函数，杀死一些常驻进程(没有FLAG_PERSISTENT标志)  
2、执行goingCallback.run()里面的逻辑。  
>监听native的crash情况：startObservingNativeCrashes();  
准备WeView的：mWebViewUpdateService.prepareWebViewInSystemServer();  
启动系统UI：startSystemUi(context, windowManagerF);  
调用一系列服务的systemRunning()方法;

3、为系统启动HomeActivity，发送广播等完成后续工作，启动Launcher。AMS启动结束 

到此，AMS的启动，准备工作完成同时启动服务，发送广播完成后续工作。


###### 3、Launcher 
Launcher的启动可以追溯到 HomeActivity的启动，即` startHomeActivityLocked(currentUserId, "systemReady")`。这个方法关
注以下几个步骤   
1、getHomeIntent()。设置Category: addCategory(Intent.CATEGORY_HOME)  
2、resolveActivityInfo()。通过AppGlobals.getPackageManager()来获取合适的ActivityInfo  
3、启动。实际启动是ActivityStartController的实例。这里的一系列调用流程大概如下：   

ActivityStartController#startHomeActivity ->ActivityStarter#execute()->
ActivityStarter#startActivity() ->
ActivityStarter#startActivity() ->
ActivityStarter#startActivity() ->
ActivityStarter#startActivityUnchecked() ->
ActivityStackSupervisor#resumeFocusedStackTopActivityLocked()->
ActivityStack#resumeTopActivityUncheckedLocked()->
ActivityStack#resumeTopActivityInnerLocked()->
ActivityStackSupervisor#startSpecificActivityLocked()-> 如果进程已经启动就启动activity:realStartActivityLocked();
如果进程没有启动就先启动进程: (AMS)mService.startProcessLocked()。先挑从进程启动的开始: ->
AMS#startProcessLocked() -> 这里经历多个startProcess()重载方法 ->
AMS#startProcess() ->
Process#Process.start() ->
ZygoteProcess#start() ->
ZygoteProcess#startViaZygote() ->
ZygoteProcess#zygoteSendArgsAndGetResult() ->(这里有个插曲是openZygoteSocketIfNeeded(),会发起Socket连接,由ZygoteState中
的LocalSocket完成)
ZygoteProcess#attemptZygoteSendArgsAndGetResult()。
```
 private Process.ProcessStartResult attemptZygoteSendArgsAndGetResult(
            ZygoteState zygoteState, String msgStr) throws ZygoteStartFailedEx {
        try {
            final BufferedWriter zygoteWriter = zygoteState.mZygoteOutputWriter;
            final DataInputStream zygoteInputStream = zygoteState.mZygoteInputStream;

            zygoteWriter.write(msgStr); // 与zygote进程中的socket通信，完成数据交换
            zygoteWriter.flush();

            // Always read the entire result from the input stream to avoid leaving
            // bytes in the stream for future process starts to accidentally stumble
            // upon.
            Process.ProcessStartResult result = new Process.ProcessStartResult();
            result.pid = zygoteInputStream.readInt();
            result.usingWrapper = zygoteInputStream.readBoolean();

            if (result.pid < 0) {
                throw new ZygoteStartFailedEx("fork() failed");
            }

            return result;
        } catch (IOException ex) {
            zygoteState.close();
            Log.e(LOG_TAG, "IO Exception while communicating with Zygote - "
                    + ex.toString());
            throw new ZygoteStartFailedEx(ex);
        }
    }
```
执行完`zygoteWriter.write(msgStr);zygoteWriter.flush()`后，进程间的socket已经完成。在Zygote进程流程时就提到过，zygote会开启socket，等待来自AMS的
连接，完成相对应的任务。关键代码为：
```
 caller = zygoteServer.runSelectLoop(abiList);
```
看到ZygoteServer的runSelectLoop方法：
```
Runnable runSelectLoop(String abiList) {
        ArrayList<FileDescriptor> fds = new ArrayList<FileDescriptor>();
        ArrayList<ZygoteConnection> peers = new ArrayList<ZygoteConnection>();
        fds.add(mServerSocket.getFileDescriptor());
        peers.add(null);
        while (true) { //无限循环，除非发生连接
            StructPollfd[] pollFds = new StructPollfd[fds.size()];
            for (int i = 0; i < pollFds.length; ++i) {
                pollFds[i] = new StructPollfd();
                pollFds[i].fd = fds.get(i);
                pollFds[i].events = (short) POLLIN;
            }
            try {
                Os.poll(pollFds, -1);
            } catch (ErrnoException ex) {
                throw new RuntimeException("poll failed", ex);
            }
            for (int i = pollFds.length - 1; i >= 0; --i) {
                if ((pollFds[i].revents & POLLIN) == 0) {
                    continue;
                }
                if (i == 0) {
                    ZygoteConnection newPeer = acceptCommandPeer(abiList);
                    peers.add(newPeer);
                    fds.add(newPeer.getFileDesciptor());
                } else {
                    try {
                        ZygoteConnection connection = peers.get(i);  // 获取连接，返回
                        final Runnable command = connection.processOneCommand(this);
                        if (mIsForkChild) {
                            // We're in the child. We should always have a command to run at this
                            // stage if processOneCommand hasn't called "exec".
                            if (command == null) {
                                throw new IllegalStateException("command == null");
                            }
                            return command; 
                        } else {
                            // We're in the server - we should never have any commands to run.
                            if (command != null) {
                                throw new IllegalStateException("command != null");
                            }
                            // We don't know whether the remote side of the socket was closed or
                            // not until we attempt to read from it from processOneCommand. This shows up as
                            // a regular POLLIN event in our regular processing loop.
                            if (connection.isClosedByPeer()) {
                                connection.closeSocket();
                                peers.remove(i);
                                fds.remove(i);
                            }
                        }
                    } catch (Exception e) {
                       // 省略代码
                    } finally {
                       // 省略代码
                    }
                }
            }
        }
    }
```
从socket中读取到通信信息后会执行到ZygoteConnection#processOneCommand(),从而再fork出一个子进程，通过handleChildProc() -> 
ZygoteInit.zygoteInit()。到这个ZygoteInit就有点似曾相识了:与zygote fork出system_server进程走的同样的流程,最后同过反射获取到SystemServer，
执行它的main方法。这里的区别就是反射获取的是ActivityThread，而不是SystemServer。

###### 3.1 ActivityThread (Activity启动)
ActivityThread是Android应用程序的入口，也是任何一个进程的主线程入口。可能会有人理解为ActivityThread就是主线程。
但其实ActivityThread并不是线程，但可以理解为ActivityThread所在的线程就是主线程。下面是ActivityThread类的核心
方法。可以看到Android系统把每个应用程序当成java应用来看待：以`main(String[] args)`作为程序的入口。应用启动后会
执行它的`main(String[] args)`。除去log的输出，初始化外，核心代码就一句：`Looper.loop();`。loop()里就是维护着一
个无线循环，不断从自己的MessageQueue取出Message，然后分发出去。如果没有消息时，会进入阻塞状态。当消息来了又被唤醒
分发消息事件。中间阻塞与唤醒是通过`MessageQueue #nativePollOnce()`与 `MessageQueue #nativeWake()`来实现，
都是native方法。
main方法：
```
public static void main(String[] args) {
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "ActivityThreadMain");
        // CloseGuard defaults to true and can be quite spammy.  We
        // disable it here, but selectively enable it later (via
        // StrictMode) on debug builds, but using DropBox, not logs.
        
        CloseGuard.setEnabled(false);
        Environment.initForCurrentUser();
        
        // Set the reporter for event logging in libcore
        EventLogger.setReporter(new EventLoggingReporter());

        // Make sure TrustedCertificateStore looks in the right place for CA certificates
        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());
        TrustedCertificateStore.setDefaultUserDirectory(configDir);
        Process.setArgV0("<pre-initialized>");
        Looper.prepareMainLooper();

        // Find the value for {@link #PROC_START_SEQ_IDENT} if provided on the command line.
        // It will be in the format "seq=114"
        long startSeq = 0;
        if (args != null) {
            for (int i = args.length - 1; i >= 0; --i) {
                if (args[i] != null && args[i].startsWith(PROC_START_SEQ_IDENT)) {
                    startSeq = Long.parseLong(
                            args[i].substring(PROC_START_SEQ_IDENT.length()));
                }
            }
        }
        ActivityThread thread = new ActivityThread();
        thread.attach(false, startSeq);

        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
        if (false) {
            Looper.myLooper().setMessageLogging(new
                    LogPrinter(Log.DEBUG, "ActivityThread"));
        }
        // End of event ActivityThreadMain.
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }
```
ActivityThread 的main方法准备环境,准备Looper,Looper启动。  
看到`attach`,这里执行流程如下：  
ActivityThread#attach()-> 
IActivityManager#attachApplication() IActivityManager是一个aidl文件:
```
public class ActivityManagerService extends IActivityManager.Stub
        implements Watchdog.Monitor, BatteryStatsImpl.BatteryCallback {
        //。。。。
        }
```
可想而知，这是一个本地binder，真正逻辑在他的服务端。而IActivityManager的服务端就是AMS,这里真正调用也就是AMS#attachApplication() ->
AMS#attachApplicationLocked():这个方法做了一件很重要的事情就是创建Application，后面在看这段。这个方法的后面一段：
```
// See if the top visible activity is waiting to run in this process...
if (normalMode) { // 如果栈顶的activity等待运行启动
    try {
         if (mStackSupervisor.attachApplicationLocked(app)) {
            didSomething = true;
         }
    } catch (Exception e) {
        Slog.wtf(TAG, "Exception  thrown launching activities in " + app, e);
        badApp = true;
    }
}
```
 ->  ActivityStackSupervisor#attachApplicationLocked(app):这里会取出栈内所有的activity记录与参数app比对，然后执行到
ActivityStackSupervisor#realStartActivityLocked() :前面分析Launcher分叉处的另一边就是这个方法 -> 
ClientLifecycleManager#scheduleTransaction() 
```
void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
     final IApplicationThread client = transaction.getClient();
     transaction.schedule();
        if (!(client instanceof Binder)) {
            // If client is not an instance of Binder - it's a remote call and at this point it is
            // safe to recycle the object. All objects used for local calls will be recycled after
            // the transaction is executed on client in ActivityThread.
            transaction.recycle();
        }
}
```
-> ClientTransaction#schedule()
```
public void schedule() throws RemoteException {
    mClient.scheduleTransaction(this);
}
```
->
mClient.scheduleTransaction(this): mClient是IApplicationThread的实例,所以对应是它的服务端ApplicationThread调用方法,下面是具体方法
```
 @Override
 public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
     ActivityThread.this.scheduleTransaction(transaction);
 }
```
 ->  ActivityThread?#scheduleTransaction()  -> 
可以看到，实际上又是ActivityThread去调用scheduleTransaction(),但是在ActivityThread类没有找到scheduleTransaction()这个方法。最后在它的父类
ClientTransactionHandler 中发现了这个方法：发送一个`EXECUTE_TRANSACTION`的事件消息。
```
void scheduleTransaction(ClientTransaction transaction) {
    transaction.preExecute(this);
    sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction);
}
```
-> TransactionExecutor#execute(transaction):这里会由`transaction.getCallbacks()`,然后遍历所有的callbacks，并调用它的execute()和
postExecute()方法。transaction的addCallback()在ActivityStackSupervisor#realStartActivityLocked()方法中有调用，传入的对象是
LaunchActivityItem -> LaunchActivityItem#execute() ->
ClientTransactionHandler#handleLaunchActivity(): 这是个抽象方法，由子类实现，即ActivityThread ->
ActivityThread#handleLaunchActivity() ->
ActivityThread#performLaunchActivity():最终在这里完成activity的启动。
```
 private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
      
      //省略部分代码。。
  
      ContextImpl appContext = createBaseContextForActivity(r);
      Activity activity = null;
      try {
          java.lang.ClassLoader cl = appContext.getClassLoader();  //创建activity
          activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);
          StrictMode.incrementExpectedActivityCount(activity.getClass());
          r.intent.setExtrasClassLoader(cl);
          r.intent.prepareToEnterProcess();
          if (r.state != null) {
              r.state.setClassLoader(cl);
          }
      }
    
      // 这里有个创建Window的步骤，提供给activity依附。涉及到View的知识
       Window window = null;
                if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
                    window = r.mPendingRemoveWindow;
                    r.mPendingRemoveWindow = null;
                    r.mPendingRemoveWindowManager = null;
                }
                appContext.setOuterContext(activity);
                activity.attach(appContext, this, getInstrumentation(), r.token,
                        r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config,
                        r.referrer, r.voiceInteractor, window, r.configCallback);
 
      try { // 获取Application
            Application app = r.packageInfo.makeApplication(false, mInstrumentation);
       }
       //省略部分代码。。
 
      activity.mCalled = false;  //调用onCreate(),这里跟Application的onCreate()方法调用一样的。
      if (r.isPersistable()) {
           mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);
      } else {
           mInstrumentation.callActivityOnCreate(activity, r.state);
      }     
      //省略部分代码。。
 }
```
Instrumentation内的方法
```
public void callActivityOnCreate(Activity activity, Bundle icicle) {
    prePerformCreate(activity);
    activity.performCreate(icicle);
    postPerformCreate(activity);
}
```
```
    final void performCreate(Bundle icicle) {
        performCreate(icicle, null);
    }

    @UnsupportedAppUsage
    final void performCreate(Bundle icicle, PersistableBundle persistentState) {
        dispatchActivityPreCreated(icicle);
        mCanEnterPictureInPicture = true;
        // initialize mIsInMultiWindowMode and mIsInPictureInPictureMode before onCreate
        final int windowingMode = getResources().getConfiguration().windowConfiguration
                .getWindowingMode();
        mIsInMultiWindowMode = inMultiWindowMode(windowingMode);
        mIsInPictureInPictureMode = windowingMode == WINDOWING_MODE_PINNED;
        restoreHasCurrentPermissionRequest(icicle);
        if (persistentState != null) {
            onCreate(icicle, persistentState);  //onCreate()方法执行
        } else {
            onCreate(icicle);
        }
        // 省略代码
    }
```
调用了Activity的performCreate(icicle)，最后调用自己的onCreate()方法，也就是创建Activity时重写的那个onCreate()方法。至此，Activity被创建启动，开始
Activity的生命周期。    
本小结源码跟踪是关联Launcher的启动，但是同时也是Activity的启动流程，二者都是类似的。Launcher先fork出一个新进程供ActivityThread运行，activity是依赖
ActivityThread的活动。总的说Launcher启动可以分为2个步骤：
* zygote fork新进程，ActivityThread启动；
* Activity启动；


###### 3.2 Application的创建。   
前面在ActivityThread的执行流程中提到调用了AMS服务端的方法，并在过程中创建了Application。创建Application的代码片段在AMS#attachApplicationLocked()
```
 thread.bindApplication( /* 省略参数 */  );
```
thread是IApplicationThread的实例。这个IApplicationThread是一个本地binder，它的服务端是ApplicationThread，是ActivityThread的一个内部类。 ->
ApplicationThread#bindApplication(): 这个方法最终会将application数据通过Handler发送类型 `H.BIND_APPLICATION`发送出
去,最后到handleBindApplication()方法处理。 -> 
ApplicationThread#handleBindApplication(): 在这个方法里会创建出Application以及调用他的onCreate()方法。先看创建。在
`app = data.info.makeApplication(data.restrictedBackupMode, null);`。data也是在ActivityThread的一个内部类：
```
 static final class AppBindData {
        LoadedApk info;
        // 省略其他
        }
```
LoadedApk#makeApplication()方法，这里instrumentation = null。
```
public Application makeApplication(boolean forceDefaultAppClass,Instrumentation instrumentation) {
        if (mApplication != null) { //已创建过直接返回创建好的，
            return mApplication;
        }
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "makeApplication");

        Application app = null;
        String appClass = mApplicationInfo.className;
        if (forceDefaultAppClass || (appClass == null)) {
            appClass = "android.app.Application";
        }
        try {
            java.lang.ClassLoader cl = getClassLoader();
            if (!mPackageName.equals("android")) {
                Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER,
                        "initializeJavaContextClassLoader");
                initializeJavaContextClassLoader();
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
            }
            ContextImpl appContext = ContextImpl.createAppContext(mActivityThread, this); // 创建上下文
            app = mActivityThread.mInstrumentation.newApplication(  //见Instrumentation#newApplication()
                    cl, appClass, appContext);
            appContext.setOuterContext(app);
        } catch (Exception e) {
            if (!mActivityThread.mInstrumentation.onException(app, e)) {
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                throw new RuntimeException(
                    "Unable to instantiate application " + appClass
                    + ": " + e.toString(), e);
            }
        }
        mActivityThread.mAllApplications.add(app); 
        mApplication = app;
        if (instrumentation != null) { //这里为null，不会调用callApplicationOnCreate()
            try {
                instrumentation.callApplicationOnCreate(app);
            } catch (Exception e) {
                if (!instrumentation.onException(app, e)) {
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    throw new RuntimeException(
                        "Unable to create application " + app.getClass().getName()
                        + ": " + e.toString(), e);
                }
            }
        }
        // Rewrite the R 'constants' for all library apks.
        SparseArray<String> packageIdentifiers = getAssets().getAssignedPackageIdentifiers();
        final int N = packageIdentifiers.size();
        for (int i = 0; i < N; i++) {
            final int id = packageIdentifiers.keyAt(i);
            if (id == 0x01 || id == 0x7f) {
                continue;
            }
            rewriteRValues(getClassLoader(), packageIdentifiers.valueAt(i), id);
        }
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        return app;
    }
```
Instrumentation#newApplication():
```
  public Application newApplication(ClassLoader cl, String className, Context context)
            throws InstantiationException, IllegalAccessException, 
            ClassNotFoundException {
        Application app = getFactory(context.getPackageName())
                .instantiateApplication(cl, className);
        app.attach(context); //绑定上下文
        return app;
  }
```
以上2个方法就是Application的具体创建过程。接下来将会调用到Application的onCreate()方法。    
Application的onCreate()方法被调用片段代码为: `mInstrumentation.callApplicationOnCreate(app);`mInstrumentation是
Instrumentation的对象实例，这个完整方法为：
```
  public void callApplicationOnCreate(Application app) {
        app.onCreate();
  }
``` 
在Application创建出来后，执行了它的onCreate()方法。

##### Handle消息机制
详情见Handler部分，以及相关问题。

##### Binder了解
见<TestLink>项目下的《binder》部分，以及blog: https://blog.csdn.net/universus/article/details/6211589

##### View的绘制流程与自定义view手法
从源码开始到view有以下几个点：
* Window、WindowManager和WMS(WindowManagerService)
* view的绘制


##### Window、PhoneWindow、DecorView
在跟踪ActivityThread启动activity最后阶段的时候就有提到过window，就是在ActivityThread#performLaunchActivity()，看到这段代码：
```
   Window window = null;
   if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
       window = r.mPendingRemoveWindow;
       r.mPendingRemoveWindow = null;
       r.mPendingRemoveWindowManager = null;
   }
   appContext.setOuterContext(activity);
   activity.attach(appContext, this, getInstrumentation(), r.token,
           r.ident, app, r.intent, r.activityInfo, title, r.parent,
           r.embeddedID, r.lastNonConfigurationInstances, config,
           r.referrer, r.voiceInteractor, window, r.configCallback);
```
跟踪到Activity#attach()：
```
    @UnsupportedAppUsage
    final void attach(...){
    
    // ...省略代码
    
    mWindow = new PhoneWindow(this, window, activityConfigCallback); // 创建PhoneWindow的实例
    mWindow.setWindowControllerCallback(mWindowControllerCallback);
    mWindow.setCallback(this);
    mWindow.setOnWindowDismissedCallback(this);
    mWindow.getLayoutInflater().setPrivateFactory(this);
    
    //...省略代码
    
    mWindow.setWindowManager((WindowManager)context.getSystemService(Context.WINDOW_SERVICE),  // 关联WindowManager
                mToken, mComponent.flattenToString(),
                (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
    if (mParent != null) {
         mWindow.setContainer(mParent.getWindow());
    }
    mWindowManager = mWindow.getWindowManager(); // 是WindowManagerImpl的实例
    mCurrentConfig = config;
    mWindow.setColorMode(info.colorMode);
    mWindow.setPreferMinimalPostProcessing((info.flags & ActivityInfo.FLAG_PREFER_MINIMAL_POST_PROCESSING) != 0);
    }
```
看到`mWindow.setWindowManager()`
```
  public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
         boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated;
        if (wm == null) {
            wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        }
        mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
  }    
  public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
      return new WindowManagerImpl(mContext, parentWindow);
  }
```
attach()方法实际还是做初始化的事情，mWindow是PhoneWindow实例(Window本身是一个抽象类)，mWindowManager是WindowManager对象，但WindowManager是
一个接口，最后获取的实例它的子类WindowManagerImpl。   
performLaunchActivity()之后会进入到Activity生命周期，体现就是走Activity#onCreate()方法。设置布局的入口在`setContentView(R.layout.activity_main);`:
看下setContentView():
```
  @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
  }
```
这里看的源代码是android 11，对应api版本是30。不同版本的api实现可能有些不一样。`setContentView()`这里使用了委托，委托给AppCompatDelegate，AppCompatDelegate
是一个抽象类，实现在AppCompatDelegateImpl。看到这个类的`setContentView`:
```
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        LayoutInflater.from(mContext).inflate(resId, contentParent);
        mOriginalWindowCallback.onContentChanged();
    }
```
看到AppCompatDelegateImpl#ensureSubDecor()：
```
    private void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            mSubDecor = createSubDecor(); // 创建mSubDecor,确保mSubDecor不是null
            // If a title was set before we installed the decor, propagate it now
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                if (mDecorContentParent != null) {
                    mDecorContentParent.setWindowTitle(title);  // 标题
                } else if (peekSupportActionBar() != null) {
                    peekSupportActionBar().setWindowTitle(title);
                } else if (mTitleView != null) {
                    mTitleView.setText(title);
                }
            }
            applyFixedSizeWindow();
            onSubDecorInstalled(mSubDecor);
            mSubDecorInstalled = true;
           
            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
            if (!mIsDestroyed && (st == null || st.menu == null)) {
                invalidatePanelMenu(FEATURE_SUPPORT_ACTION_BAR);
            }
        }
    }
```
mSubDecor是ViewGroup实例。猜测是容纳传入View的容器，但是不是Window还不能确定。跟踪`createSubDecor()`,方法略长，只挑选些关键部分，省略部分代码以
及注释，log等：   
```
    private ViewGroup createSubDecor() {
       
        // 省略代码。。。主要就是通过TypedArray获取主题样式。比如ActionBar，windowNoTitle等设置

        mWindow.getDecorView();  // 获取DecorView。DecorView继承至FrameLayout，也是一个容器来的
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup subDecor = null;
        if (!mWindowNoTitle) { // 如果有标题栏
            if (mIsFloating) { // 悬浮模式              
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_dialog_title_material, null);
                mHasActionBar = mOverlayActionBar = false;
                
            } else if (mHasActionBar) { // 有ActionBar              
                TypedValue outValue = new TypedValue();
                mContext.getTheme().resolveAttribute(R.attr.actionBarTheme, outValue, true);
                Context themedContext;
                if (outValue.resourceId != 0) {
                    themedContext = new ContextThemeWrapper(mContext, outValue.resourceId);
                } else {
                    themedContext = mContext;
                }
                subDecor = (ViewGroup) LayoutInflater.from(themedContext).inflate(R.layout.abc_screen_toolbar, null);
                mDecorContentParent = (DecorContentParent) subDecor.findViewById(R.id.decor_content_parent);
                mDecorContentParent.setWindowCallback(getWindowCallback());

                // 设置样式到mDecorContentParent，如进度条，ActionBar等等
            }
        } else {
            if (mOverlayActionMode) {
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_screen_simple_overlay_action_mode, null);
            } else {
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_screen_simple, null);
            }
            // (。。。。省略代码) 设置应用window的监听器(主要用于布局展示View)，有版本方法限制，分水岭版本号为21。                  
        }
        if (mDecorContentParent == null) {  // 找到title
            mTitleView = (TextView) subDecor.findViewById(R.id.title);
        }
        // Make the decor optionally fit system windows, like the window's decor(适应系统窗口)
        ViewUtils.makeOptionalFitsSystemWindows(subDecor);
        
        final ContentFrameLayout contentView = (ContentFrameLayout) subDecor.findViewById(
                R.id.action_bar_activity_content);               
        final ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content); //
        if (windowContentView != null) {
            // 如果已经有view容器添加到decorView，要把这些内容迁移到新的容器中
            // There might be Views already added to the Window's content view so we need to
            // migrate them to our content view         
            while (windowContentView.getChildCount() > 0) {
                final View child = windowContentView.getChildAt(0);
                windowContentView.removeViewAt(0);
                contentView.addView(child);
            }
            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            windowContentView.setId(View.NO_ID);
            contentView.setId(android.R.id.content); //将新的容器id设置为R.id.content。

            // The decorContent may have a foreground drawable set (windowContentOverlay).
            // Remove this as we handle it ourselves
            if (windowContentView instanceof FrameLayout) {
                ((FrameLayout) windowContentView).setForeground(null);
            }
        }
        // Now set the Window's content view with the decor
        mWindow.setContentView(subDecor);  //  将新的容器设置到window。

        contentView.setAttachListener(new ContentFrameLayout.OnAttachListener() {
            @Override
            public void onAttachedFromWindow() {}
            @Override
            public void onDetachedFromWindow() {
                dismissPopups();
            }
        });
        return subDecor;
    }
```
看到摘选代码中的`mWindow.getDecorView();`，前面说过Window的实现是在PhoneWindow,所以要在PhoneWindow类找getDecorView()。经过方法调用，最终来到
PhoneWindow的`installDecor()`。
```
    private void installDecor() {
        mForceDecorInstall = false;
        if (mDecor == null) {
            mDecor = generateDecor(-1);
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        } else {
            mDecor.setWindow(this);
        }
        if (mContentParent == null) {
            mContentParent = generateLayout(mDecor);            
             // 。。。省略一些样式默认设置代码
         }
    }
```
installDecor()保证了mDecor不会为null，并且设置一些系统相关样式属性参数。`generateDecor(-1);`方法主要是先获取Context。是使用，然后new出DecorView对象。另外
`generateLayout(mDecor)`这里有一点要注意：
```
protected ViewGroup generateLayout(DecorView decor){
 // 省略前面代码。。。
 
 // 此前一部分是Inflate decor逻辑。其实就是Inflate layoutResource。layoutResource为系统xml文件，这个文件有一个id为content的FrameLayout
 // 这也是后面可以直接findViewById(ID_ANDROID_CONTENT)的原因。
 mDecor.startChanging();
 mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
 ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
 if (contentParent == null) {
    throw new RuntimeException("Window couldn't find content container view");
 }
 // 省略代码。。。
 return contentParent;
 }
```
返回的是从decor中找到id为ID_ANDROID_CONTENT的一个View。ID_ANDROID_CONTENT还有另外一个身份就是com.android.internal.R.id.content。这个后面会用到。   
回到`createSubDecor()`。mWindow.getDecorView()执行完毕。继续往下执行。整个方法后面一部分核心还是inflate出一个View作为新的window容器，也就是subDecor。到
后面
>final ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content);

这里找到id为R.id.content的View，如果能找到并且它原来有其他的子view，要把这些内容迁移到新的容器(contentView)。contentView是subDecor的第一个子view，
是一个ContentFrameLayout。随后将contentView的id设置为android.R.id.content。最后将新的容器设置给Window(`mWindow.setContentView(subDecor);`)，
PhoneWindow#setContentView(),最终或调用下面方法：
```
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            view.setLayoutParams(params);
            final Scene newScene = new Scene(mContentParent, view);
            transitionTo(newScene);
        } else {
            mContentParent.addView(view, params);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
        mContentParentExplicitlySet = true;
    }
```
前面第一个if else中`installDecor()`前面已经有看过，下面看到`FEATURE_CONTENT_TRANSITIONS`这个flag，大概翻译就是内容过渡标志。我的理解是类似activity的转场
动画。带有这个标志是内容中的某个控件或元素支持过渡动画。如果需要内容过渡则通过Scene添加view，否则直接添加view到mContentParent。在Scene也可以看到：
```
    public Scene(ViewGroup sceneRoot, View layout) {
        mSceneRoot = sceneRoot;
        mLayout = layout;
    }
```
```
    public void enter() {
        // Apply layout change, if any
        if (mLayoutId > 0 || mLayout != null) {
            // empty out parent container before adding to it
            getSceneRoot().removeAllViews();
            if (mLayoutId > 0) {
                LayoutInflater.from(mContext).inflate(mLayoutId, mSceneRoot);
            } else {
                mSceneRoot.addView(mLayout); // mSceneRoot就是mContentParent
            }
        }
        if (mEnterAction != null) {
            mEnterAction.run();
        }
        setCurrentScene(mSceneRoot, this);
    }
```
最终还是会将subDecor添加到mContentParent容器中去。到此做个小结：  
* Window 是一个抽象类，每个Activity都有一个Window，具体实现类为PhoneWindow。
* PhoneWindow 是Window的实现类，所有具体的绘制逻辑都在这个类中，Window或者说PhoneWindow处在同一个层级上。PhoneWindow内部有一个DecorView的实例。
* DecorView 继承FrameLayout，是所有视图的根view。它的inflate逻辑取根据系统主题样式由系统创建。它有个id为`android.R.id.content`的子View。
* id为android.R.id.content的View(mContentParent/contentView,在不同类中有不同的名称) DecorView中的一个子view，实质也是一个FrameLayout，在构建时可能
会被替换为ContentFrameLayout(也是继承FrameLayout)，但id不会被改变。开发中为activity设置的ContentView，就是它的子View。

subDecor添加完mContentParent后，一直返回到最开始地方，也就是AppCompatDelegateImpl#setContentView()中的ensureSubDecor(),再贴一遍代码：
```
    public void setContentView(View v) {
        ensureSubDecor(); // 完成了Decor的创建初始化工作
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mOriginalWindowCallback.onContentChanged();
    }
```
剩下逻辑就是将自己绘制的xml生成的View添加到contentParent容器中。到此Activity#setContentView(R.layout.xx)流程跟踪结束，同时对Window，PhoneWindow，
DecorView也有一个比较清楚的认识。这里额外做一个小测试，递归打印View的父类处理：从自己的xml文件开始：
```
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/tvHello"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```
打印代码为
```
       View view =  findViewById(R.id.tvHello);
       Log.d("TAG", "view= "+ view);
       while (view != null) {
           view = (View) view.getParent();
          Log.d("TAG", "parent= "+ view);
       }
```
输出结果：
```
D: view= androidx.appcompat.widget.AppCompatTextView{2d62ef0 V.ED.... ......ID 0,0-0,0 #7f07008d app:id/tvHello}
D: parent= androidx.constraintlayout.widget.ConstraintLayout{351f4c69 V.E..... ......I. 0,0-0,0}
D: parent= androidx.appcompat.widget.ContentFrameLayout{29d1b6ee V.E..... ......I. 0,0-0,0 #1020002 android:id/content}
D: parent= androidx.appcompat.widget.ActionBarOverlayLayout{1e1b978f V.E..... ......I. 0,0-0,0 #7f070030 app:id/decor_content_parent}
D: parent= android.widget.FrameLayout{1874a1c V.E..... ......I. 0,0-0,0}
D: parent= android.widget.LinearLayout{cd74625 V.E..... ......I. 0,0-0,0}
D: parent= com.android.internal.policy.impl.PhoneWindow$DecorView{34a653fa V.E..... R.....ID 0,0-0,0}
D: parent= null
```
tvHello是一个TextView，它的父布局是一个ConstraintLayout。再往上就是ContentFrameLayout，这表示DecorView中id=content的子view的是被替换过的。view的最
根父view是DecorView。  
Activity的onCreate()方法结束，进入到onResume()。但是在这之前在ActivityThread会先执行handleResumeActivity():
```
 public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
            String reason) {
            
        // 省略代码。。。
        
        if (r.window == null && !a.mFinished && willBeVisible) {
            r.window = r.activity.getWindow(); // window与activity关联
            View decor = r.window.getDecorView();
            decor.setVisibility(View.INVISIBLE);
            ViewManager wm = a.getWindowManager();
            WindowManager.LayoutParams l = r.window.getAttributes();
            a.mDecor = decor;
            l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
            l.softInputMode |= forwardBit;
            if (r.mPreserveWindow) {
                a.mWindowAdded = true;
                r.mPreserveWindow = false;
                // Normally the ViewRoot sets up callbacks with the Activity
                // in addView->ViewRootImpl#setView. If we are instead reusing
                // the decor view we have to notify the view root that the
                // callbacks may have changed.
                ViewRootImpl impl = decor.getViewRootImpl();
                if (impl != null) {
                    impl.notifyChildRebuilt();
                }
            }
            if (a.mVisibleFromClient) {
                if (!a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l); // 将decor添加到wm中。
                } else {
                    // The activity will get a callback for this {@link LayoutParams} change
                    // earlier. However, at that time the decor will not be set (this is set
                    // in this method), so no action will be taken. This call ensures the
                    // callback occurs with the decor set.
                    a.onWindowAttributesChanged(l);
                }
            }
            // 。。。省略代码
        }   
 }                       
```
在这个方法中，会将activity与window关联，开始添加doctor`wm.addView(decor, l)`。前面就知道，这里的WindowManager实现是WindowManagerImpl实例。而
WindowManagerImpl中的add逻辑又是交给WindowManagerGlobal处理。看到WindowManagerGlobal#addView();
```
    public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow, int userId) {
        // 省略代码。。。
        synchronized (mLock) {
         // 省略代码。。。
            root = new ViewRootImpl(view.getContext(), display);
            view.setLayoutParams(wparams);
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);
            try {
                root.setView(view, wparams, panelParentView, userId);
            } catch (RuntimeException e) {
                if (index >= 0) {
                    removeViewLocked(index, true);
                }
                throw e;
            }
        }
    }
```
看到` root.setView(...);`这里引出了一个新的类:ViewRootImpl。看到它的setView()方法：
```
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView,  int userId) {
        synchronized (this) {
            if (mView == null) {
                mView = view;
                //省略代码。。。。
                
                // Schedule the first layout -before- adding to the window
                // manager, to make sure we do the relayout before receiving
                // any other events from the system.
                requestLayout(); // 关注这一句即可。
                InputChannel inputChannel = null;
                // 。。。省略代码
            }
            。。。
        }
        。。。
}           
```
重点是`requestLayout()`方法：
```html
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            checkThread();
            mLayoutRequested = true;
            scheduleTraversals();
        }
    }
```
checkThread()就是检查当前线程是否是`original thread`,否则会抛出一个常见的异常
>"Only the original thread that created a view hierarchy can touch its views."

看到`scheduleTraversals()`方法：
```html
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null); // 关键在mTraversalRunnable这个Runnable
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
```
`mTraversalRunnable`是一个Runnable，他的run方法逻辑只执行了一个方法`doTraversal();`
```
    void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier); // 移除同步屏障
            if (mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            performTraversals();
            if (mProfile) {
                Debug.stopMethodTracing();
                mProfile = false;
            }
        }
    }
```
`doTraversal()`会执行到`performTraversals()`,到这里才是真真正正的开始绘制。下面开始跟踪理解这个方法。(注：这个方法又大又长，会删除部分没意义的log，debug
日志，注释等)。
```
    private void performTraversals() {
        // cache mView since it is used so much below...
        final View host = mView; mView在setView()方法被赋值，这个mView/host就是DecorView的实例。

        if (host == null || !mAdded) // mAdded在setView()会被赋值为true。
            return;

        mIsInTraversal = true; //是否在循环绘制
        mWillDrawSoon = true;  // 是否马上绘制
        boolean windowSizeMayChange = false; // 窗口是否发生改变
        WindowManager.LayoutParams lp = mWindowAttributes;

        int desiredWindowWidth; // 预期窗口宽度
        int desiredWindowHeight; // 预期窗口高度

        final int viewVisibility = getHostVisibility(); // 根view是否可见
        final boolean viewVisibilityChanged = !mFirst   // view可见状态是否发生变化
                && (mViewVisibility != viewVisibility || mNewSurfaceNeeded
                || mAppVisibilityChanged);
        mAppVisibilityChanged = false;
        final boolean viewUserVisibilityChanged = !mFirst &&
                ((mViewVisibility == View.VISIBLE) != (viewVisibility == View.VISIBLE));

        WindowManager.LayoutParams params = null;
        CompatibilityInfo compatibilityInfo =
                mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        if (compatibilityInfo.supportsScreen() == mLastInCompatMode) {
            params = lp;
            mFullRedrawNeeded = true;
            mLayoutRequested = true;
            if (mLastInCompatMode) {
                params.privateFlags &= ~WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = false;
            } else {
                params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = true;
            }
        }

        Rect frame = mWinFrame;
        if (mFirst) {
            mFullRedrawNeeded = true;
            mLayoutRequested = true;

            final Configuration config = mContext.getResources().getConfiguration();
            if (shouldUseDisplaySize(lp)) {
                // NOTE -- system code, won't try to do compat mode.
                Point size = new Point();
                mDisplay.getRealSize(size);
                desiredWindowWidth = size.x;
                desiredWindowHeight = size.y;
            } else if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                    || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                // For wrap content, we have to remeasure later on anyways. Use size consistent with
                // below so we get best use of the measure cache.
                desiredWindowWidth = dipToPx(config.screenWidthDp);
                desiredWindowHeight = dipToPx(config.screenHeightDp);
            } else {
                // After addToDisplay, the frame contains the frameHint from window manager, which
                // for most windows is going to be the same size as the result of relayoutWindow.
                // Using this here allows us to avoid remeasuring after relayoutWindow
                desiredWindowWidth = frame.width();
                desiredWindowHeight = frame.height();
            }

            // We used to use the following condition to choose 32 bits drawing caches:
            // PixelFormat.hasAlpha(lp.format) || lp.format == PixelFormat.RGBX_8888
            // However, windows are now always 32 bits by default, so choose 32 bits
            mAttachInfo.mUse32BitDrawingCache = true;
            mAttachInfo.mWindowVisibility = viewVisibility;
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mLastConfigurationFromResources.setTo(config);
            mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
            // Set the layout direction if it has not been set before (inherit is the default)
            if (mViewLayoutDirectionInitial == View.LAYOUT_DIRECTION_INHERIT) {
                host.setLayoutDirection(config.getLayoutDirection());
            }
            host.dispatchAttachedToWindow(mAttachInfo, 0);
            mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
            dispatchApplyInsets(host);
        } else {
            desiredWindowWidth = frame.width();
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) {
                if (DEBUG_ORIENTATION) Log.v(mTag, "View " + host + " resized to: " + frame);
                mFullRedrawNeeded = true;
                mLayoutRequested = true;
                windowSizeMayChange = true;
            }
        }

        if (viewVisibilityChanged) {
            mAttachInfo.mWindowVisibility = viewVisibility;
            host.dispatchWindowVisibilityChanged(viewVisibility);
            if (viewUserVisibilityChanged) {
                host.dispatchVisibilityAggregated(viewVisibility == View.VISIBLE);
            }
            if (viewVisibility != View.VISIBLE || mNewSurfaceNeeded) {
                endDragResizing();
                destroyHardwareResources();
            }
        }

        // Non-visible windows can't hold accessibility focus.
        if (mAttachInfo.mWindowVisibility != View.VISIBLE) {
            host.clearAccessibilityFocus();// 清楚焦点
        }

        // Execute enqueued actions on every traversal in case a detached view enqueued an action
        getRunQueue().executeActions(mAttachInfo.mHandler);

        boolean cutoutChanged = false;

        boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
        if (layoutRequested) {

            final Resources res = mView.getContext().getResources();

            if (mFirst) {
                // make sure touch mode code executes by setting cached value
                // to opposite of the added touch mode.
                mAttachInfo.mInTouchMode = !mAddedTouchMode;
                ensureTouchModeLocally(mAddedTouchMode);
            } else {
                if (!mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout)) {
                    cutoutChanged = true;
                }
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                        || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    windowSizeMayChange = true;

                    if (shouldUseDisplaySize(lp)) {
                        // NOTE -- system code, won't try to do compat mode.
                        Point size = new Point();
                        mDisplay.getRealSize(size);
                        desiredWindowWidth = size.x;
                        desiredWindowHeight = size.y;
                    } else {
                        Configuration config = res.getConfiguration();
                        desiredWindowWidth = dipToPx(config.screenWidthDp);
                        desiredWindowHeight = dipToPx(config.screenHeightDp);
                    }
                }
            }

            // Ask host how big it wants to be
            windowSizeMayChange |= measureHierarchy(host, lp, res,
                    desiredWindowWidth, desiredWindowHeight);
        }

        if (collectViewAttributes()) {
            params = lp;
        }
        if (mAttachInfo.mForceReportNewAttributes) {
            mAttachInfo.mForceReportNewAttributes = false;
            params = lp;
        }

        if (mFirst || mAttachInfo.mViewVisibilityChanged) {
            mAttachInfo.mViewVisibilityChanged = false;
            int resizeMode = mSoftInputMode &
                    WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
            // If we are in auto resize mode, then we need to determine
            // what mode to use now.
            if (resizeMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
                final int N = mAttachInfo.mScrollContainers.size();
                for (int i=0; i<N; i++) {
                    if (mAttachInfo.mScrollContainers.get(i).isShown()) {
                        resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                    }
                }
                if (resizeMode == 0) {
                    resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                }
                if ((lp.softInputMode &
                        WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != resizeMode) {
                    lp.softInputMode = (lp.softInputMode &
                            ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) |
                            resizeMode;
                    params = lp;
                }
            }
        }

        if (mApplyInsetsRequested) {
            dispatchApplyInsets(host);
            if (mLayoutRequested) {
                // Short-circuit catching a new layout request here, so
                // we don't need to go through two layout passes when things
                // change due to fitting system windows, which can happen a lot.
                windowSizeMayChange |= measureHierarchy(host, lp,
                        mView.getContext().getResources(),
                        desiredWindowWidth, desiredWindowHeight);
            }
        }

        if (layoutRequested) {
            // Clear this now, so that if anything requests a layout in the
            // rest of this function we will catch it and re-run a full
            // layout pass.
            mLayoutRequested = false;
        }

        boolean windowShouldResize = layoutRequested && windowSizeMayChange
            && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.width() < desiredWindowWidth && frame.width() != mWidth)
                || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.height() < desiredWindowHeight && frame.height() != mHeight));
        windowShouldResize |= mDragResizing && mResizeMode == RESIZE_MODE_FREEFORM;

        // If the activity was just relaunched, it might have unfrozen the task bounds (while
        // relaunching), so we need to force a call into window manager to pick up the latest
        // bounds.
        windowShouldResize |= mActivityRelaunched;

        // Determine whether to compute insets.
        // If there are no inset listeners remaining then we may still need to compute
        // insets in case the old insets were non-empty and must be reset.
        final boolean computesInternalInsets =
                mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners()
                || mAttachInfo.mHasNonEmptyGivenInternalInsets;

        boolean insetsPending = false;
        int relayoutResult = 0;
        boolean updatedConfiguration = false;

        final int surfaceGenerationId = mSurface.getGenerationId();

        final boolean isViewVisible = viewVisibility == View.VISIBLE;
        final boolean windowRelayoutWasForced = mForceNextWindowRelayout;
        boolean surfaceSizeChanged = false;
        boolean surfaceCreated = false;
        boolean surfaceDestroyed = false;
        /* True if surface generation id changes. */
        boolean surfaceReplaced = false;

        final boolean windowAttributesChanged = mWindowAttributesChanged;
        if (windowAttributesChanged) {
            mWindowAttributesChanged = false;
            params = lp;
        }

        if (params != null) {
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0
                    && !PixelFormat.formatHasAlpha(params.format)) {
                params.format = PixelFormat.TRANSLUCENT;
            }
            adjustLayoutParamsForCompatibility(params);
            controlInsetsForCompatibility(params);
        }

        if (mFirst || windowShouldResize || viewVisibilityChanged || cutoutChanged || params != null
                || mForceNextWindowRelayout) {
            mForceNextWindowRelayout = false;

            if (isViewVisible) {
                // If this window is giving internal insets to the window
                // manager, and it is being added or changing its visibility,
                // then we want to first give the window manager "fake"
                // insets to cause it to effectively ignore the content of
                // the window during layout.  This avoids it briefly causing
                // other windows to resize/move based on the raw frame of the
                // window, waiting until we can finish laying out this window
                // and get back to the window manager with the ultimately
                // computed insets.
                insetsPending = computesInternalInsets && (mFirst || viewVisibilityChanged);
            }

            if (mSurfaceHolder != null) {
                mSurfaceHolder.mSurfaceLock.lock();
                mDrawingAllowed = true;
            }

            boolean hwInitialized = false;
            boolean dispatchApplyInsets = false;
            boolean hadSurface = mSurface.isValid();

            try {
                if (DEBUG_LAYOUT) {
                    Log.i(mTag, "host=w:" + host.getMeasuredWidth() + ", h:" +
                            host.getMeasuredHeight() + ", params=" + params);
                }

                if (mAttachInfo.mThreadedRenderer != null) {
                    // relayoutWindow may decide to destroy mSurface. As that decision
                    // happens in WindowManager service, we need to be defensive here
                    // and stop using the surface in case it gets destroyed.
                    if (mAttachInfo.mThreadedRenderer.pause()) {
                        // Animations were running so we need to push a frame
                        // to resume them
                        mDirty.set(0, 0, mWidth, mHeight);
                    }
                    mChoreographer.mFrameInfo.addFlags(FrameInfo.FLAG_WINDOW_LAYOUT_CHANGED);
                }
                relayoutResult = relayoutWindow(params, viewVisibility, insetsPending);

                if (DEBUG_LAYOUT) Log.v(mTag, "relayout: frame=" + frame.toShortString()
                        + " cutout=" + mPendingDisplayCutout.get().toString()
                        + " surface=" + mSurface);

                // If the pending {@link MergedConfiguration} handed back from
                // {@link #relayoutWindow} does not match the one last reported,
                // WindowManagerService has reported back a frame from a configuration not yet
                // handled by the client. In this case, we need to accept the configuration so we
                // do not lay out and draw with the wrong configuration.
                if (!mPendingMergedConfiguration.equals(mLastReportedMergedConfiguration)) {
                    if (DEBUG_CONFIGURATION) Log.v(mTag, "Visible with new config: "
                            + mPendingMergedConfiguration.getMergedConfiguration());
                    performConfigurationChange(mPendingMergedConfiguration, !mFirst,
                            INVALID_DISPLAY /* same display */);
                    updatedConfiguration = true;
                }

                cutoutChanged = !mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout);
                surfaceSizeChanged = (relayoutResult
                        & WindowManagerGlobal.RELAYOUT_RES_SURFACE_RESIZED) != 0;
                final boolean alwaysConsumeSystemBarsChanged =
                        mPendingAlwaysConsumeSystemBars != mAttachInfo.mAlwaysConsumeSystemBars;
                final boolean colorModeChanged = hasColorModeChanged(lp.getColorMode());
                surfaceCreated = !hadSurface && mSurface.isValid();
                surfaceDestroyed = hadSurface && !mSurface.isValid();
                surfaceReplaced = (surfaceGenerationId != mSurface.getGenerationId())
                        && mSurface.isValid();

                if (cutoutChanged) {
                    mAttachInfo.mDisplayCutout.set(mPendingDisplayCutout);
                    if (DEBUG_LAYOUT) {
                        Log.v(mTag, "DisplayCutout changing to: " + mAttachInfo.mDisplayCutout);
                    }
                    // Need to relayout with content insets.
                    dispatchApplyInsets = true;
                }
                if (alwaysConsumeSystemBarsChanged) {
                    mAttachInfo.mAlwaysConsumeSystemBars = mPendingAlwaysConsumeSystemBars;
                    dispatchApplyInsets = true;
                }
                if (updateCaptionInsets()) {
                    dispatchApplyInsets = true;
                }
                if (dispatchApplyInsets || mLastSystemUiVisibility !=
                        mAttachInfo.mSystemUiVisibility || mApplyInsetsRequested) {
                    mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
                    dispatchApplyInsets(host);
                    // We applied insets so force contentInsetsChanged to ensure the
                    // hierarchy is measured below.
                    dispatchApplyInsets = true;
                }
                if (colorModeChanged && mAttachInfo.mThreadedRenderer != null) {
                    mAttachInfo.mThreadedRenderer.setWideGamut(
                            lp.getColorMode() == ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
                }

                if (surfaceCreated) {
                    // If we are creating a new surface, then we need to
                    // completely redraw it.
                    mFullRedrawNeeded = true;
                    mPreviousTransparentRegion.setEmpty();

                    // Only initialize up-front if transparent regions are not
                    // requested, otherwise defer to see if the entire window
                    // will be transparent
                    if (mAttachInfo.mThreadedRenderer != null) {
                        try {
                            hwInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
                            if (hwInitialized && (host.mPrivateFlags
                                            & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) == 0) {
                                // Don't pre-allocate if transparent regions
                                // are requested as they may not be needed
                                mAttachInfo.mThreadedRenderer.allocateBuffers();
                            }
                        } catch (OutOfResourcesException e) {
                            handleOutOfResourcesException(e);
                            return;
                        }
                    }
                    notifySurfaceCreated();
                } else if (surfaceDestroyed) {
                    // If the surface has been removed, then reset the scroll
                    // positions.
                    if (mLastScrolledFocus != null) {
                        mLastScrolledFocus.clear();
                    }
                    mScrollY = mCurScrollY = 0;
                    if (mView instanceof RootViewSurfaceTaker) {
                        ((RootViewSurfaceTaker) mView).onRootViewScrollYChanged(mCurScrollY);
                    }
                    if (mScroller != null) {
                        mScroller.abortAnimation();
                    }
                    // Our surface is gone
                    if (mAttachInfo.mThreadedRenderer != null &&
                            mAttachInfo.mThreadedRenderer.isEnabled()) {
                        mAttachInfo.mThreadedRenderer.destroy();
                    }
                } else if ((surfaceReplaced
                        || surfaceSizeChanged || windowRelayoutWasForced || colorModeChanged)
                        && mSurfaceHolder == null
                        && mAttachInfo.mThreadedRenderer != null
                        && mSurface.isValid()) {
                    mFullRedrawNeeded = true;
                    try {
                        // Need to do updateSurface (which leads to CanvasContext::setSurface and
                        // re-create the EGLSurface) if either the Surface changed (as indicated by
                        // generation id), or WindowManager changed the surface size. The latter is
                        // because on some chips, changing the consumer side's BufferQueue size may
                        // not take effect immediately unless we create a new EGLSurface.
                        // Note that frame size change doesn't always imply surface size change (eg.
                        // drag resizing uses fullscreen surface), need to check surfaceSizeChanged
                        // flag from WindowManager.
                        mAttachInfo.mThreadedRenderer.updateSurface(mSurface);
                    } catch (OutOfResourcesException e) {
                        handleOutOfResourcesException(e);
                        return;
                    }
                }

                if (!surfaceCreated && surfaceReplaced) {
                    notifySurfaceReplaced();
                }

                final boolean freeformResizing = (relayoutResult
                        & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_FREEFORM) != 0;
                final boolean dockedResizing = (relayoutResult
                        & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_DOCKED) != 0;
                final boolean dragResizing = freeformResizing || dockedResizing;
                if (mDragResizing != dragResizing) {
                    if (dragResizing) {
                        mResizeMode = freeformResizing
                                ? RESIZE_MODE_FREEFORM
                                : RESIZE_MODE_DOCKED_DIVIDER;
                        final boolean backdropSizeMatchesFrame =
                                mWinFrame.width() == mPendingBackDropFrame.width()
                                        && mWinFrame.height() == mPendingBackDropFrame.height();
                        // TODO: Need cutout?
                        startDragResizing(mPendingBackDropFrame, !backdropSizeMatchesFrame,
                                mLastWindowInsets.getSystemWindowInsets().toRect(),
                                mLastWindowInsets.getStableInsets().toRect(), mResizeMode);
                    } else {
                        // We shouldn't come here, but if we come we should end the resize.
                        endDragResizing();
                    }
                }
                if (!mUseMTRenderer) {
                    if (dragResizing) {
                        mCanvasOffsetX = mWinFrame.left;
                        mCanvasOffsetY = mWinFrame.top;
                    } else {
                        mCanvasOffsetX = mCanvasOffsetY = 0;
                    }
                }
            } catch (RemoteException e) {
            }

            if (DEBUG_ORIENTATION) Log.v(
                    TAG, "Relayout returned: frame=" + frame + ", surface=" + mSurface);

            mAttachInfo.mWindowLeft = frame.left;
            mAttachInfo.mWindowTop = frame.top;

            // !!FIXME!! This next section handles the case where we did not get the
            // window size we asked for. We should avoid this by getting a maximum size from
            // the window session beforehand.
            if (mWidth != frame.width() || mHeight != frame.height()) {
                mWidth = frame.width();
                mHeight = frame.height();
            }

            if (mSurfaceHolder != null) {
                // The app owns the surface; tell it about what is going on.
                if (mSurface.isValid()) {
                    // XXX .copyFrom() doesn't work!
                    //mSurfaceHolder.mSurface.copyFrom(mSurface);
                    mSurfaceHolder.mSurface = mSurface;
                }
                mSurfaceHolder.setSurfaceFrameSize(mWidth, mHeight);
                mSurfaceHolder.mSurfaceLock.unlock();
                if (surfaceCreated) {
                    mSurfaceHolder.ungetCallbacks();

                    mIsCreating = true;
                    SurfaceHolder.Callback[] callbacks = mSurfaceHolder.getCallbacks();
                    if (callbacks != null) {
                        for (SurfaceHolder.Callback c : callbacks) {
                            c.surfaceCreated(mSurfaceHolder);
                        }
                    }
                }

                if ((surfaceCreated || surfaceReplaced || surfaceSizeChanged
                        || windowAttributesChanged) && mSurface.isValid()) {
                    SurfaceHolder.Callback[] callbacks = mSurfaceHolder.getCallbacks();
                    if (callbacks != null) {
                        for (SurfaceHolder.Callback c : callbacks) {
                            c.surfaceChanged(mSurfaceHolder, lp.format,
                                    mWidth, mHeight);
                        }
                    }
                    mIsCreating = false;
                }

                if (surfaceDestroyed) {
                    notifyHolderSurfaceDestroyed();
                    mSurfaceHolder.mSurfaceLock.lock();
                    try {
                        mSurfaceHolder.mSurface = new Surface();
                    } finally {
                        mSurfaceHolder.mSurfaceLock.unlock();
                    }
                }
            }

            final ThreadedRenderer threadedRenderer = mAttachInfo.mThreadedRenderer;
            if (threadedRenderer != null && threadedRenderer.isEnabled()) {
                if (hwInitialized
                        || mWidth != threadedRenderer.getWidth()
                        || mHeight != threadedRenderer.getHeight()
                        || mNeedsRendererSetup) {
                    threadedRenderer.setup(mWidth, mHeight, mAttachInfo,
                            mWindowAttributes.surfaceInsets);
                    mNeedsRendererSetup = false;
                }
            }

            if (!mStopped || mReportNextDraw) {
                boolean focusChangedDueToTouchMode = ensureTouchModeLocally(
                        (relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);
                if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
                        || mHeight != host.getMeasuredHeight() || dispatchApplyInsets ||
                        updatedConfiguration) {
                    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);

                    if (DEBUG_LAYOUT) Log.v(mTag, "Ooops, something changed!  mWidth="
                            + mWidth + " measuredWidth=" + host.getMeasuredWidth()
                            + " mHeight=" + mHeight
                            + " measuredHeight=" + host.getMeasuredHeight()
                            + " dispatchApplyInsets=" + dispatchApplyInsets);

                     // Ask host how big it wants to be
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);

                    // Implementation of weights from WindowManager.LayoutParams
                    // We just grow the dimensions as needed and re-measure if
                    // needs be
                    int width = host.getMeasuredWidth();
                    int height = host.getMeasuredHeight();
                    boolean measureAgain = false;

                    if (lp.horizontalWeight > 0.0f) {
                        width += (int) ((mWidth - width) * lp.horizontalWeight);
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }
                    if (lp.verticalWeight > 0.0f) {
                        height += (int) ((mHeight - height) * lp.verticalWeight);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }

                    if (measureAgain) {
                        if (DEBUG_LAYOUT) Log.v(mTag,
                                "And hey let's measure once more: width=" + width
                                + " height=" + height);
                        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    }

                    layoutRequested = true;
                }
            }
        } else {
            // Not the first pass and no window/insets/visibility change but the window
            // may have moved and we need check that and if so to update the left and right
            // in the attach info. We translate only the window frame since on window move
            // the window manager tells us only for the new frame but the insets are the
            // same and we do not want to translate them more than once.
            maybeHandleWindowMove(frame);
        }

        if (surfaceSizeChanged || surfaceReplaced || surfaceCreated || windowAttributesChanged) {
            // If the surface has been replaced, there's a chance the bounds layer is not parented
            // to the new layer. When updating bounds layer, also reparent to the main VRI
            // SurfaceControl to ensure it's correctly placed in the hierarchy.
            //
            // This needs to be done on the client side since WMS won't reparent the children to the
            // new surface if it thinks the app is closing. WMS gets the signal that the app is
            // stopping, but on the client side it doesn't get stopped since it's restarted quick
            // enough. WMS doesn't want to keep around old children since they will leak when the
            // client creates new children.
            updateBoundsLayer(surfaceReplaced);
        }

        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        boolean triggerGlobalLayoutListener = didLayout
                || mAttachInfo.mRecomputeGlobalAttributes;
        if (didLayout) {
            performLayout(lp, mWidth, mHeight);

            // By this point all views have been sized and positioned
            // We can compute the transparent area

            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
                // start out transparent
                // TODO: AVOID THAT CALL BY CACHING THE RESULT?
                host.getLocationInWindow(mTmpLocation);
                mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1],
                        mTmpLocation[0] + host.mRight - host.mLeft,
                        mTmpLocation[1] + host.mBottom - host.mTop);

                host.gatherTransparentRegion(mTransparentRegion);
                if (mTranslator != null) {
                    mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
                }

                if (!mTransparentRegion.equals(mPreviousTransparentRegion)) {
                    mPreviousTransparentRegion.set(mTransparentRegion);
                    mFullRedrawNeeded = true;
                    // reconfigure window manager
                    try {
                        mWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
                    } catch (RemoteException e) {
                    }
                }
            }

            if (DBG) {
                System.out.println("======================================");
                System.out.println("performTraversals -- after setFrame");
                host.debug();
            }
        }

        if (surfaceDestroyed) {
            notifySurfaceDestroyed();
        }

        if (triggerGlobalLayoutListener) {
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        if (computesInternalInsets) {
            // Clear the original insets.
            final ViewTreeObserver.InternalInsetsInfo insets = mAttachInfo.mGivenInternalInsets;
            insets.reset();

            // Compute new insets in place.
            mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
            mAttachInfo.mHasNonEmptyGivenInternalInsets = !insets.isEmpty();

            // Tell the window manager.
            if (insetsPending || !mLastGivenInsets.equals(insets)) {
                mLastGivenInsets.set(insets);

                // Translate insets to screen coordinates if needed.
                final Rect contentInsets;
                final Rect visibleInsets;
                final Region touchableRegion;
                if (mTranslator != null) {
                    contentInsets = mTranslator.getTranslatedContentInsets(insets.contentInsets);
                    visibleInsets = mTranslator.getTranslatedVisibleInsets(insets.visibleInsets);
                    touchableRegion = mTranslator.getTranslatedTouchableArea(insets.touchableRegion);
                } else {
                    contentInsets = insets.contentInsets;
                    visibleInsets = insets.visibleInsets;
                    touchableRegion = insets.touchableRegion;
                }

                try {
                    mWindowSession.setInsets(mWindow, insets.mTouchableInsets,
                            contentInsets, visibleInsets, touchableRegion);
                } catch (RemoteException e) {
                }
            }
        }

        if (mFirst) {
            if (sAlwaysAssignFocus || !isInTouchMode()) {
                // handle first focus request
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(mTag, "First: mView.hasFocus()=" + mView.hasFocus());
                }
                if (mView != null) {
                    if (!mView.hasFocus()) {
                        mView.restoreDefaultFocus();
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(mTag, "First: requested focused view=" + mView.findFocus());
                        }
                    } else {
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(mTag, "First: existing focused view=" + mView.findFocus());
                        }
                    }
                }
            } else {
                // Some views (like ScrollView) won't hand focus to descendants that aren't within
                // their viewport. Before layout, there's a good change these views are size 0
                // which means no children can get focus. After layout, this view now has size, but
                // is not guaranteed to hand-off focus to a focusable child (specifically, the edge-
                // case where the child has a size prior to layout and thus won't trigger
                // focusableViewAvailable).
                View focused = mView.findFocus();
                if (focused instanceof ViewGroup
                        && ((ViewGroup) focused).getDescendantFocusability()
                                == ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    focused.restoreDefaultFocus();
                }
            }
        }

        final boolean changedVisibility = (viewVisibilityChanged || mFirst) && isViewVisible;
        final boolean hasWindowFocus = mAttachInfo.mHasWindowFocus && isViewVisible;
        final boolean regainedFocus = hasWindowFocus && mLostWindowFocus;
        if (regainedFocus) {
            mLostWindowFocus = false;
        } else if (!hasWindowFocus && mHadWindowFocus) {
            mLostWindowFocus = true;
        }

        if (changedVisibility || regainedFocus) {
            // Toasts are presented as notifications - don't present them as windows as well
            boolean isToast = (mWindowAttributes == null) ? false
                    : (mWindowAttributes.type == TYPE_TOAST);
            if (!isToast) {
                host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
            }
        }

        mFirst = false;
        mWillDrawSoon = false;
        mNewSurfaceNeeded = false;
        mActivityRelaunched = false;
        mViewVisibility = viewVisibility;
        mHadWindowFocus = hasWindowFocus;

        mImeFocusController.onTraversal(hasWindowFocus, mWindowAttributes);

        // Remember if we must report the next draw.
        if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
            reportNextDraw();
        }
        if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_BLAST_SYNC) != 0) {
            reportNextDraw();
            setUseBLASTSyncTransaction();
            mSendNextFrameToWm = true;
        }

        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw() || !isViewVisible;

        if (!cancelDraw) {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).startChangingAnimations();
                }
                mPendingTransitions.clear();
            }

            performDraw();
        } else {
            if (isViewVisible) {
                // Try again
                scheduleTraversals();
            } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).endChangingAnimations();
                }
                mPendingTransitions.clear();
            }
        }

        if (mAttachInfo.mContentCaptureEvents != null) {
            notifyContentCatpureEvents();
        }

        mIsInTraversal = false;
    }
```