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
由上代码可知，显示启动直接指明需要跳转的目标activity，相当于告诉了目的地，直接去就好。隐式启动，并没有指定目标activity，而是通过设置action，category
等 属性，在清单文件中匹配activity中的IntentFilter。显示启动多用于应用内部页面的跳转。而隐式启动更多倾向于外部页面(比如系统的，或者另一个app中的页面)。
2种方式的区别更多在于应用场景的不同。   
* 显示启动： 应用内部跳转启动，无法启动应用外的页面，用途范围小于隐式启动。
* 隐式启动： 能启动应用内的页面，也能启动应用外部的页面，具有一定保护作用。


三、启动模式(!)
四种启动模式: 
* standard 标准(默认)模式：如同砌砖，每次都创建新的act都放在当前栈顶；如果存在相同的act，也同样创建。
* singleTop 栈顶复用模式：当栈内不存在此activity时，创建新的act放到栈顶；如果存在，直接复用已存在的activity，不会重复创建新的相同act。
* singleTask 栈内复用模式：当栈内不存在此activity时，创建新的act放到栈顶；如果存在，将会把此activity上面的所有act移除栈，该act成为栈顶实例。
* singleInstance 单例模式：这里的单例是指栈单例。也就是说一个任务栈只存在一个act实例。如果该act存在也是直接复用，不会重新创建。

启动模式的应用场景  
* 标准模式： 
* 栈顶复用：适合启动频繁的页面。比如消息通知之类的，每个通知查看都启动一个新页面还是很烦的。
* 栈内复用：适合有入口主页的界面，由主页散发到其他功能。比如浏览器主页，无论从哪启动浏览器，浏览器器主页只有一个；同时回到主页，将其他的页面关闭。
* 单例：适合带有独立工作性质的页面。比如闹钟。

##### 2、activity问题收录
生命周期问题：   
* Activity生命周期
> onCreate() - onStart() - onResume() - onPause() - onStop() - onDestroy()  onRestart()
  
* onStart 和 onResume、onPause 和 onStop 的区别?
> onStart()可以见不可以交互，onResume()可见可以交互，onPause()可见不可交互；onStop()不可见不可交互。
  
* Activity A启动另一个Activity B会调用哪些方法?如果B是透明主题的又或则是个DialogActivity呢?  
> (A)onPause() ->  (B)onCreate() -> (B)onStart() -> (B)onResume() -> (A)onStop()
  如果B是透明或者DialogActivity：(A)onPause() ->  (B)onCreate() -> (B)onStart() -> (B)onResume() (A不会再走onStop())
  
* Activity A跳转Activity B，再按返回键，生命周期执行的顺序?
> (A)onPause() -> (B)onCreate() -> (B)onStart() -> (B)onResume() -> (A)onStop()
  按下返回键： (B)onPause() -> (A)onRestart() -> (A)onStart() -> (A)onResume() -> (B)onStop() -> (B)onDestroy()
 
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
> activity的启动模式设置为singleInstance，并且已经创建过实例，是处于onPause、onStop状态。要从新回到onResume()状态时会触发onNewIntent()。
  此时生命周期为： onNewIntent() -> onRestart() -> onStart() -> onResume()。

* onCreate()和onRestoreInstance()方法中恢复数据时的区别?
> onCreate()中也有Bundle参数，也能做数据恢复，但是这个Bundle可能为null。onRestoreInstance()是与onSaveInstanceState()一一对应的，
 和onRestoreInstance()的Bundle是一定不会为null的。相对于onCreate()中恢复数据，使用onRestoreInstance()更加安全，稳定。

* Activity的数据是怎么保存的,进程被Kill后,保存的数据怎么恢复的
> 在onSaveInstanceState()保存，在onRestoreInstance()恢复。

启动方式问题：  
* 显示启动和隐式启动
> 显示启动是直接指定目标activity跳转，隐式启动是通过清单文件的Filter一一匹配寻找activity  。 
 
* scheme使用场景,协议格式,如何使用。

数据传递(交互)问题    
* Activity之间传递数据的方式Intent是否有大小限制，如果传递的数据量偏大，有哪些方案?
* activity间传递数据的方式
* 跨App启动Activity的方式,注意事项

启动模式问题  
* Activity任务栈是什么
* activity的启动模式和使用场景
* 有哪些Activity常用的标记位Flags


#### Service  

* service 的生命周期，两种启动方式的区别
* Service的两种启动方式？区别在哪
* 如何保证Service不被杀死 ？
* Service与Activity怎么实现通信
* IntentService是什么,IntentService原理，应用场景及其与Service的区别
* Service 的 onStartCommand 方法有几种返回值?各代表什么意思?
* bindService和startService混合使用的生命周期以及怎么关闭
* 用过哪些系统Service ？
* 了解ActivityManagerService吗？发挥什么作用


#### BroadcastReceiver

* 广播的分类和使用场景
* 广播的两种注册方式的区别
* 广播发送和接收的原理
* 本地广播和全局广播的区别


#### ContentProvider

* 什么是ContentProvider及其使用
* ContentProvider的权限管理
* ContentProvider,ContentResolver,ContentObserver之间的关系
* ContentProvider的实现原理
* ContentProvider的优点
* Uri是什么