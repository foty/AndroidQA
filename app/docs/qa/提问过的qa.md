
### QA大乱炖(按时间先后)

##### view的绘制发生在哪个生命周期
> 在Activity的onResume()回调之后。(ActivityThread的handleResumeActivity()内)

##### 在Activity的onResume()，能获得view的宽高吗?为什么
> 可以。通过View.post()。当调用View.post()时，会监测View是否绘制完成，如果完成的话，可以直接获取到宽高；如果未完成绘
> 制，那么会将这个runnable放到一个待处理队列中。待绘制完成后便可以执行这些action。此时是可以获得View的宽高了。

##### Looper死循环为什么不会发生ANR
> 第一、主线程也就是ActivityThread，启动了Looper的死循环。不断从消息队列中获取消息，处理消息。进而产生应用的各种活动，
> 应用的活动是各种消息事件来驱动的。当消息队列没有消息时，Looper进入空闲状态，就不会产生任何活动。当Looper循环停止，应用
> 也将结束掉。这是Looper需要死循环的原因；   
> 第二、ANR是需要当前事件超过一定时间没有得到处理，或者没有完成才会产生。只要事件处理及时，就不会发生ANR。ANR其实跟Looper
> 是没有直接关系的。

##### 为什么会出现ANR，什么是ANR
> 当前的事件超过一定时间没有得到处理，或者没有完成，就会造成ANR。比如：
* Activity中的触摸事件超过 5s；  
* Service为 20s；  
* BroadCastReceiver 10s；  
* ContentProvider 10s；

##### 如何解决ANR
> 可以通过导出系统的ANR日志文件(trace文件)分析定位；

##### UI卡顿是怎样产生的
> 处理某个消息事件耗时太长，导致新来的事件不能及时得到处理，就会出现卡顿。一般情况下，事件处理在16ms内完成，就不会感到卡
> 顿。超过16ms的时间越多，感觉就越卡，甚至出现ANR。

##### 同步消息与异步消息
> Handler中的消息分为同步消息、异步消息，屏障消息(同步屏障)。区别是异步消息会有一个特殊的参数。发送异步消息只需要在创
> 建Handler实例时，传递一个为值true的async参数。或者使用`msg.setAsynchronous(true)`。

##### 什么是同步屏障
> 作用：阻碍队列中同步消息的屏障，让异步消息优先执行。
> 设置同步屏障，其实就是发送一个target == null的msg，进入到队列。当检测到这是一个同步屏障(消息)后，
> MessageQueue就会寻找此后队列中的异步消息处理，忽略掉队列中的同步消息。如果队列一直不存在异步消息，那么线程进入阻塞状态。    

##### 为什么有同步屏障(异步消息)
> 异步消息与屏障消息是为了避免界面卡顿。在View的绘制过程中(屏幕大概每16ms要刷新一次),如果前面有其他大量消息，要等到前面消
> 息都处理完才去刷新屏幕，那么屏幕要卡住很久。这不是合理的情况。这时同步屏障与异步消息就能发挥作用。
> 当屏幕需要刷新时，并不是马上就去执行view的绘制，而是会先插入一个同步屏障，然后监听刷新信号(async)。当信号来时会发送一个异
> 步消息，这个消息就是执行绘制。当绘制流程开始时就会移除掉同步屏障。
> 移除同步屏障方法： removeSyncBarrier()

##### Glide的3级缓存是什么？
> ActiveResources(活跃资源缓存)、LruResourceCache(Lru资源缓存)、DiskCache(磁盘缓存)

##### 为什么设计成2级内存缓存？
> 1、保护这部分资源不会被LruCache算法回收(活跃资源缓存ActiveResource是用弱引用缓存的资源)；
> 2、使用频率高的资源将不会在LruCache中查找，相当于替LruCache减压。

##### Glide的内存管理
> 

##### 说说activity的newIntent方法
> 1、Activity的启动模式设置为SingleTop时，如果该activity在栈顶，再次启动同个activity，这时会调用onNewIntent方法；
> 2、Activity的启动模式设置为singleTask时，并且已经创建过实例。再次启动同个activity时会触发onNewIntent()。此时的生
> 命周期为：onNewIntent() -> onRestart() -> onStart() -> onResume()。

