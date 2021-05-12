##### 启动流程相关

> 了解fork() 机制   
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