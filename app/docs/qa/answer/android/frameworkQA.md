
### 
* activity启动过程
* zygote与system_server
* Launcher
* ActivityThread
* Application
* AMS

##### 描述activity的启动过程?
启动Activity，通常有两种情况，一种是通过startActivity启动，另一种是通过Launcher启动。
> 1、Linux内核的init进程fork出zygote进程：zygote会创建虚拟机、注册一些底层方法，然后会反射调用ZygoteInit的main方法；[此时进程从C层到了java层]
> 2、ZygoteInit会加载一些必要类(加载器)和资源，然后fork出system_server进程，然后进入到SystemServer的main方法；
> 3、在SystemServer的main方法，它也是做一些初始化的工作。例如：加载一些so库、初始化系统context、准备Looper、创建并启动一些关
> 键服务，例如SystemServiceManager、AMS、PMS(PackageManagerService)、PMS(电源管理服务)等；
> 4、当AMS启动后执行的工作有：设置Installer、安装系统Provider、设置WindowManager、注册系统服务到ServiceManager。例如，
> activity、permission、cpuinfo等等，方便统一管理、startSystemUi、调起HomeActivity()方法，启动Launcher(也就是默认桌面,由系统创建)。
> Launcher启动意味着系统已经初始化完成，并提供了用户与设备交互的入口。
> 5、Launcher启动后，它会判断应用进程是否存在：
> 如果存在，则直接进入到ActivityThread，也就是主线程，在这里调用performLaunchActivity()方法启动目标activity，回调各种生命周期方法；
> 如果不存在，最终会由zygote再次fork出一个子进程，也就是ActivityThread。执行
> 到ActivityThread的main方法。在ActivityThread，它会通过AIDL与AMS通信，创建出Application，接着就是启动目标Activity的。随后Looper进入循环；

##### 说说ActivityThread
每个应用有一个ActivityThread；是应用的入口；
> 在APP进程中
是AMS的缓存中心
ActivityThread中的List<ActivityRecord> activtes放了activity的启动记录

##### 应用内activity与activity的跳转是跨进程通信，还是同一个进程内通信？
> 是跨进程通信；因为都要经过AMS。

##### AMS


《Android Framework学习手册》：
开机Init进程
开机启动Zygote进程
开机启动SystemServer进程
Binder驱动
AMS的启动过程
PMS的启动过程
Launcher 的启动过程
Android 四大组件
Android 系统服务 - Input 事件的分发过程
Android 底层渲染 - 屏幕刷新机制源码分析
Android 源码分析实战


AMS源码分析
Activity生命周期管理
onActivityResult执行过程
AMS中Activity栈管理详解

深入PMS源码：
1.PMS的启动过程和执行流程
2.APK的安装和卸载源码分析
3.PMS中intent-filter的匹配架构

WMS：
1.WMS的诞生
2.WMS的重要成员和Window的添加过程
3.Window的删除过程