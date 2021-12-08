#### 四大组件合集

- 问题收录来源:  
  <http://www.xiangxueketang.cn/enjoy/removal/article_7?bd_vid=8255047947312038939>

##### Activity

* 说下Activity生命周期
* onStart 和 onResume、onPause 和 onStop 的区别
* activity的启动模式和使用场景
* Activity A 启动另一个Activity B 会调用哪些方法？如果B是透明主题的又或则是个DialogActivity呢
* Activity A跳转Activity B，再按返回键，生命周期执行的顺序
* 说下onSaveInstanceState()方法的作用 ? 何时会被调用？
* onSaveInstanceState(),onRestoreInstanceState的掉用时机
* Activity的启动流程
* 横竖屏切换,按home键,按返回键,锁屏与解锁屏幕,跳转透明Activity界面,启动一个 Theme 为 Dialog 的 Activity，弹出Dialog时Activity的生命周期
* Activity之间传递数据的方式Intent是否有大小限制，如果传递的数据量偏大，有哪些方案
* Activity的onNewIntent()方法什么时候会执行
* 显示启动和隐式启动
* scheme使用场景,协议格式,如何使用
* ANR 的四种场景
* onCreate和onRestoreInstance方法中恢复数据时的区别
* activity间传递数据的方式
* 跨App启动Activity的方式,注意事项
* Activity任务栈是什么
* 有哪些Activity常用的标记位Flags
* Activity的数据是怎么保存的,进程被Kill后,保存的数据怎么恢复的


##### Service  

* service 的生命周期，两种启动方式的区别
* Service的两种启动方式？区别在哪
* 如何保证Service不被杀死 ？
* Service与Activity怎么实现通信
* IntentService是什么,IntentService原理，应用场景及其与Service的区别
* Service 的 onStartCommand 方法有几种返回值?各代表什么意思?
* bindService和startService混合使用的生命周期以及怎么关闭
* 用过哪些系统Service ？
* 了解ActivityManagerService吗？发挥什么作用


##### BroadcastReceiver

* 广播的分类和使用场景
* 广播的两种注册方式的区别
* 广播发送和接收的原理
* 本地广播和全局广播的区别


##### ContentProvider

* 什么是ContentProvider及其使用
* ContentProvider的权限管理
* ContentProvider,ContentResolver,ContentObserver之间的关系
* ContentProvider的实现原理
* ContentProvider的优点
* Uri是什么