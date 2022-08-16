### 四大组件合集

- 问题收录来源:  
  <http://www.xiangxueketang.cn/enjoy/removal/article_7?bd_vid=8255047947312038939>

#### Activity

##### 1、关于activity需要知道的
一、生明周期  
[查看图片](../图片/img_activity生命周期.png)
简单说就是：   
onCreate() - onStart() - onResume() - onPause() - onStop() - onDestroy()

二、启动方式  
主要分显示启动与隐式启动。  
显示启动代码：
```
Intent intent = new Intent(this,B.class);
startActivity(intent)
```
隐式启动代码：
```
Intent intent = new Intent();
intent.setAction("x.x.ACTION"); // 必填
intent.addCategory("填写Category"); // 可选
intent.setData("填写Data"); //可选
startActivity(intent)
```
由上代码可知，显示启动直接指明需要跳转的目标activity，相当于告诉了目的地，直接去就好。隐式启动，并没有指定目标act，
而是通过设置action，category等 属性，在清单文件中匹配activity中的IntentFilter。显示启动多用于应用内部页面的跳转。
而隐式启动更多倾向于外部页面(比如系统的，或者另一个app中的页面)。2种方式的区别更多在于应用场景的不同。   
* 显示启动： 应用内部跳转启动，无法启动应用外的页面，用途范围小于隐式启动。
* 隐式启动： 能启动应用内的页面，也能启动应用外部的页面，具有一定保护作用。

三、启动模式(!)
主要有四种启动模式:
* standard 标准(默认)模式：如同砌砖，每次都创建新的act都放在当前栈顶；如果存在相同的act，也同样创建。
* singleTop 栈顶复用模式：当栈内不存在此activity时，创建新的act放到栈顶；如果存在，直接复用已存在的activity，不会
重复创建新的相同act。
* singleTask 栈内复用模式：当栈内不存在此activity时，创建新的act放到栈顶；如果存在，将会把此activity上面的所有
act移除栈，该act成为栈顶实例。
* singleInstance 单例模式：这里的单例是指栈单例。也就是说一个任务栈只存在一个act实例。如果该act存在也是直接复用，不
会重新创建。

启动模式的应用场景
* 标准模式： 
* 栈顶复用：适合启动频繁的页面。比如消息通知之类的，每个通知查看都启动一个新页面还是很烦的。
* 栈内复用：适合有入口主页的界面，由主页散发到其他功能。比如浏览器主页，无论从哪启动浏览器，浏览器器主页只有一个；同时
回到主页，将其他的页面关闭。
* 单例：适合带有独立工作性质的页面。比如闹钟。


#### Service  

##### 1、关于Service基础知识
service是四大基础组件之一，为后台服务。

一、生命周期
service的生命周期跟启动方式有关。启动service有2种方式，startService与 bindService。  
startService: onCreate() -> onStartCommand() -> onDestroy();  
bindService: onCreate() -> onBind() -> onUnBind() -> onDestroy()


#### BroadcastReceiver
 ...


#### ContentProvider
....


#### 问题收录

生命周期问题：
* Activity生命周期
> onCreate() - onStart() - onResume() - onPause() - onStop() - onDestroy()  onRestart()

* onStart 和 onResume、onPause 和 onStop 的区别?
> onStart()可以见不可以交互，onResume()可见可以交互，onPause()可见不可交互；onStop()不可见不可交互。

* Activity A启动另一个Activity B会调用哪些方法?如果B是透明主题的又或则是个DialogActivity呢?
> (A)onPause() ->  (B)onCreate() -> (B)onStart() -> (B)onResume() -> (A)onStop()
如果B是透明或者DialogActivity：(A)onPause() ->  (B)onCreate() -> (B)onStart() -> (B)onResume()
(A不会再走onStop())

* Activity A跳转Activity B，再按返回键，生命周期执行的顺序?
> (A)onPause() -> (B)onCreate() -> (B)onStart() -> (B)onResume() -> (A)onStop()
按下返回键： (B)onPause() -> (A)onRestart() -> (A)onStart() -> (A)onResume() -> (B)onStop() -> 
(B)onDestroy()

* 横竖屏切换,按home键,按返回键,锁屏与解锁屏幕,弹出Dialog时Activity的生命周期
> 按home键、锁屏: onPause() -> onSaveInstanceState() -> onStop()
解锁屏幕：onRestart() -> onStart() -> onResume()
弹出dialog的activity：onPause()
返回键：onPause() -> onStop() -> onDestroy()
切横屏、切竖屏：onPause() -> onSaveInstanceState() -> onStop() -> onDestroy() -> onCreate() -> onStart()
-> onRestoreInstanceState() -> onResume()

* onSaveInstanceState(),onRestoreInstanceState的调用时机?
> 横竖屏切换时调用，按下home键，锁屏时等等。目的是保存当前的数据，从而能在页面恢复后重新取出。

* Activity的onNewIntent()方法什么时候会执行?
> activity的启动模式设置为singleInstance，并且已经创建过实例，是处于onPause、onStop状态。要从新回到onResume()
状态时会触发onNewIntent()。此时生命周期为： onNewIntent() -> onRestart() -> onStart() -> onResume()。

* onCreate()和onRestoreInstance()方法中恢复数据时的区别?
> onCreate()中也有Bundle参数，也能做数据恢复，但是这个Bundle可能为null。onRestoreInstance()是与
onSaveInstanceState()一一对应的，和onRestoreInstance()的Bundle是一定不会为null的。相对于onCreate()中恢复数
据，使用onRestoreInstance()更加安全，稳定。

* Activity的数据是怎么保存的,进程被Kill后,保存的数据怎么恢复的
> 在onSaveInstanceState()保存，在onRestoreInstance()恢复。

启动方式问题：
* 显示启动和隐式启动
> 显示启动是直接指定目标activity跳转，隐式启动是通过清单文件的Filter一一匹配寻找activity  。

* scheme使用场景,协议格式,如何使用。
> scheme也就是配置activity的intent-filter。通常用作跨应用跳转。配置内容包括action、category、data。其中data又
> 包括：host、path、port、scheme。其中scheme是协议名称，host是主机地址、port是端口号、path是具体路径。
(scheme://host:port/path?...)

数据传递(交互)问题
* Activity之间传递数据的方式Intent是否有大小限制?如果传递的数据量偏大，有哪些方案?
> 限制在1MB以内。大数据量传递可以：
> 1、通过压缩传递数据。比如对象转成json字符创传递；将不必要的属性使用transient修饰；
> 2、通过事件消息传递。比如广播，eventbus，livedataBus等。

* activity间传递数据的方式?
> 1、intent传递；2、intent中的Bundle传递；3、公共静态变量；4、数据保存-获取形式传递，如sp，数据库等等；5、事件
总线传递，如广播，各种bus等。

* 跨App启动Activity的方式,注意事项。
> 使用隐式启动activity即可。注意配置正确filter信息。

启动模式问题
* Activity任务栈是什么?
> 任务栈也就是task，保存启动的activity实例。是一个栈结构，栈顶的activity实例就是显示在屏幕页面。

* activity的启动模式和使用场景?
> 看上面的[三、启动模式(!)]

* 有哪些Activity常用的标记位Flags?
> FLAG_ ACTIVITY_NEW_TASK: 指定Activity启动模式为"singleTask"
> FLAG_ACTIVITY_SINGLE_TOP: 指定Activity启动模式为"singleTop"

* taskAffinity，allowTaskReparenting的用法。 
> 用于指定当前Act所关联的Task，allowTaskReparenting用于配置是否允许该act可以更换从属task，默认是false。表示不允
> 许。通常配合一起使用，实现把一个应用程序的Act移到另一个应用程序的Task中。


* Service的两种启动方式?区别在哪?
> 生命周期不多说。主要看下2者的区别(序号1为start方式，序号2为bind方式)：
> 1、生命周期不一样；方式1的生命周期为onCreate() -> onStartCommand() -> onDestroy()；方式2的生命周期是
onCreate() -> onBind() -> onUnBind() -> onDestroy()。   
> 2、停止方式不一样。方式1启动之后只有调用stopService()能停止服务。与调用者无关，即使调用者已经结束。方式2通
> 过onUnbind()解除绑定，如果调用者结束该服务也会结束停止。

* bindService()和startService()混合使用的生命周期以及怎么关闭?
> 开启时先start后bind，停止时先stop后unbind，最后走才destroy(其实这个顺序可以调换)。

* 如何保证Service不被杀死?
> 保活问题

* Service与Activity怎么实现通信?
> 1、binder(aidl); 2、消息bus，比如广播、xxBus等。

* IntentService是什么,IntentService原理，应用场景及其与Service的区别?
> IntentService和service都是Android的Service基本组件，IntentService继承Service。启动方式都是一样。区别在
> 于IntentService内部会开启一个子线程，通过Handler发送消息通知到`onHandleIntent()`方法中处理。并且处理完自己的
> 任务后，service自动停止。换个说法就是如果要在service中执行耗时操作，可以直接使用IntentService。

* Service的onStartCommand()方法有几种返回值?各代表什么意思?
> 4种。
> START_NOT_STICKY： 进程被系统强制杀掉之后，不会重新创建service。    
> START_STICKY_COMPATIBILITY：(兼容版本)进程被系统杀死后，不确保是否能重新执行onStartCommand方法。   
> START_STICKY： 进程被系统强制杀掉之后，会重新创建service，并执行onStartCommand()回调方法，但不会保存Intent，
所以重建的onStartCommand()回调方法的Intent参数为null。    
> START_REDELIVER_INTENT： 进程被系统强制杀掉之后，service会重建service并且会保存Intent，重建的intent能获取到
相关信息。


* 广播的分类和使用场景
> 有序广播、无序广播(没有优先级之分)；系统广播、非系统广播；

* 广播的两种注册方式的区别?
> 2种注册方式为静态注册与动态注册。    
静态注册：会在清单文件中配置广播的相关信息(action name)，直接使用`sendBroadcast(Intent)`发送广播。   
动态注册：不配置到清单文件，直接new出相关Receiver实例，通过`registerReceiver()`方法注册，同样使用
`sendBroadcast(Intent)`发送广播。注册的广播记 得最后需要注销广播。

区别：    
> 1、生命周期不同。动态广播是非常驻型广播，跟随Activity生命周期；而静态广播是常驻型广播，与activity无关。    
> 2、优先级不同。在同优先级的情况下，动态广播接收器优先级比静态广播接收器高。

* 广播发送和接收的原理。
> 先说总结：广播的发送和接收原理是在AMS+Binder+Handler共同作用下的一套通信机制。首先通过`sendBroadcast()`方法将一
> 个广播以Binder机制发送到AMS,AMS从广播接收器注册列表中找到对应的广播，通过Binder传递给广播接收器的分发器
> (ReceiverDispatcher)。分发器通过Handler发送广播到指定的接收器中，也就是`onReceive()`回调方法。

* 本地广播和全局广播的区别。
> 本地广播：广播发送和接收只在应用内，与其他应用无关；只能动态注册，不能静态注册。典型例子就是
`LocalBroadcastManager`类。     
> 全局广播：可以发送到和接收其他应用的广播，能动态注册，也能静态注册。


* 什么是ContentProvider及其使用
> 即内容提供者，也是四大组件之一，用于进程间数据(共享)交互(增删改查)。(跨进程通信)。

* ContentProvider,ContentResolver,ContentObserver之间的关系?
> ContentProvider内容提供者；ContentObserver内容观察者，作用是观察指定Uri数据库的变化，从而能做出响应的处理；
> ContentResolver内容解析器，用于解析从内容提供者获取到的数据。ContentProvider获取到数据交给ContentResolver解
> 析，同时Uri数据库发生变化时可以通过ContentObserver监听做处理。

* ContentProvider的实现原理?
> ContentProvider是进程间共享数据，它的主要共享方式就是通过binder。非要说原理的话就是进程启动时，ContentProvider
> 就会被创建并注册到AMS，并且啊，ContentProvider的启动比Application的启动还早。当需要使用某一个ContentProvider
> 时，根据Uri找到指定的ContentProvider(binder形式)，就可以增删改查了。

* ContentProvider的优点?
> 系统API,进程间通信安全简单,因为它是数据共享的方式。

* Uri是什么?
> 统一资源标识符。标识ContentProvider。说白了就是名片。通过Uri找到对应的那个ContentProvider。格式为：
`schema/authority/path/id`。中文释义：纲要/权威/路径(表名)/id。schema通常固定为：content://。
> 如："content://com.carson.provider/User/1"
