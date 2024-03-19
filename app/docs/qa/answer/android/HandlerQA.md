
### Handler的几个问题

* Handler、Message、MessageQueue内部方法分析
* Handler更新UI问题
* Looper、ThreadLocal
* 同步屏障


##### 简述Handler的基本原理
> Handler将Message发送到Looper的消息队列中，即MessageQueue，通过Looper的循环读取Message，处理Message。然后调用Message.target，
> 也就是的Handler的dispatchMessage()方法，将该消息回调到handleMessage()方法中，然后完成更新UI操作。

##### Handler是如何能够线程切换，发送Message的?
> 创建Handler实例时会从当前线程获取Looper，MessageQueue等。当使用Handler实例发送消息时，msg会进入到MessageQueue，最后由Looper取出msg分发
> 到对应Handler的dispatchMessage()方法。如果在线程A创建的Handler，那么Looper，MessageQueue也都在线程A上。如果在线程B使用这个Handler实例
> 发出消息，但是Looper、MessageQueue是在线程A启动的，最终msg也是回到了线程A中被处理。于是就完成了线程B -> 线程A 的切换。

##### 主线程的Handler是怎么判断收到的消息是哪个Handler传来的？
> 通过Message的target来标注。

##### 页面A、B分别创建HandlerA、B。在A启动一个延时任务后马上跳转B，B也启动一个延时任务，同时执行removeCallbacksAndMessages()，A和B延时任务会如何
> 页面A的延时任务会执行，B的延时任务不会执行。调用removeCallbacksAndMessages()会移除掉message。

##### MessageQueue获取消息是怎么等待?
> 通过native方法`nativePollOnce()`来阻塞线程实现消息等待。`nativePollOnce()`则是由native层epoll机制中的epoll_wait()函数等待的。

##### 多个线程给MessageQueue发消息，如何保证线程安全?
> 用锁(synchronized)

##### 多个Handler发消息时，消息队列如何保证线程安全？
> 用synchronized代码块去进行同步

##### 为什么 MessageQueue 不设置消息上限，message上限怎么办。
> 个人理解是应用的活动是由消息事件驱动的，无法计算出一个应用活动需要多少消息事件，所以不设置上限。其实在调用sendMessage()的时候，是向message这个
> 链表的尾端插入一个message，这个长度是没有限制的。但是如果你不断通过new message的方式去调用sendMessage()，是会出现内存溢出的问题；可以使
> 用Message.obtain()方法代替new对象来获取消息，此方法在消息池中获得一个消息时，消息池中缓存的消息数就减1。当消息池中的消息数大
> 于MAX_POOL_SIZE的时候，则消息池中的消息数不加一，也不将消息添加到消息池中，而这个消息池主要用来重复利用从而避免更多的内存消耗。

##### Handler#post(Runnable) 是如何执行的?
> 先通过getPostMessage()将Runnable对象转换成Message对象(Runnable对象是Message的callback),赋值给到Message.callback。然后入队列，
> 最后在dispatchMessage的时候回调message.callback.run()。其他没有特别的地方。

##### Handler#sendMessage() 和 Handler#postDelay()的区别？
> 首先共同点是俩者都会执行到sendMessageDelayed()方法。#sendMessage()是直接传递一个Message对象，不需要额外再处理参数；#postDelay()传
> 递的是Runnable对象，需要通过getPostMessage()将Runnable参数转换成Message参数。把Runnable对象赋值给Message的callback属性。
> 最后消息分发的时候回调Runnable的run方法。

##### Message.callback 与 Handler.callback 哪个优先？
>  优先级： Message.callback > Handler.callback(mCallback)

##### Handler.callback和handleMessage()都存在，但callback返回true，handleMessage()还会执行么？
> 不会，有return关键字。

##### 为什么建议用obtain方法创建Message?
> Message本身包含两个Message对象，一个是sPool，一个是next，但通过看源码可知道sPool是一个static对象，是所有对象共有，Message.sPool就是一
> 个单链表结构，Message就是单链表中的一个节点。使用obtain方法，取的是Message的sPool，改变sPool指向sPool的next，取出sPool本身，并清空该
> Message的flags和next。

好处
> 可避免重复创建多个实例对象，提升效率，可以取消息池子之前已经存在的消息对象。

##### 我们能在主线程直接new无参handler吗?
> 可以，主线程的Looper在ActivityThread初始化的时候就被初始化了。

##### IdleHandler是什么？怎么使用，能解决什么问题？
> 空闲监听器，是一个线程空闲(没有消息处理)时执行逻辑的接口，有一个Boolean类型返回值的方法。当返回false时，会移除自己，不再执行。当消息队列空闲时
> 会回调它的queueIdle()方法。使用方法:调用MessageQueue#addIdleHandler()方法。能解决什么问题：主线程能做的，它都能做，适合优先级没有那么
> 高的一些任务，也不能太耗时；像在一些第三方工具上。

##### Handler内存泄漏的原因？
> MessageQueue持有Message，Message持有activity

##### 非UI线程真的不能操作View吗? (子线程可以更新UI吗)
> 可以。准确来讲操作View其实并不是限定UI线程，而是源线程。就是创建View所在的线程与操作View的线程是不是同一个。如果在子线程创建View，那在
> 这个子线程操作这个View是完全可以的。

##### 子线程中怎么使用Handler?
> 在创建Handler对象前调用`Looper.prepare();`,在创建完后调用`Looper.loop();`。


##### 为什么主线程不用调用looper.prepare()和looper.looper()?
> 因为主线程的Looper已经在ActivityThread初始化的时候就调用了这2个方法。

##### 一个线程有几个looper，几个handler，如果你说一个，他会问，如何保证looper唯一?
> 1个，多个Handler。通过ThreadLocal保证looper唯一。

如何保证：    
ThreadLocal的特性是当某线程使用ThreadLocal存储数据后，只有在该线程可以读取到存储的数据(谁保存谁访问)。
> 1、ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，Looper为value保存。
> 2、保存Looper方法只能被调用一次。

##### ThreadLocal的原理，以及在Looper是如何应用的？
> 前面ThreadLocal介绍。ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，
> Looper为value保存。保证Looper的唯一性。

##### 如果我们的子线程(创建Handler)没有消息处理的情况下，我们如何优化looper？
> 如果子线程是长期的，可以参考主线程Looper，让其阻塞，需要时在唤醒。如果是短期的，可以直接调用Looper#quit()退出，释放内存，结束线程。

##### 我们的looper通过什么实现与线程关联?
> ThreadLocal。ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，
> Looper为value保存。

##### Looper会一直消耗系统资源吗?
> 不会，looper获取msg的过程使用到了epoll机制。epoll机制是一种高效的IO多路复用机制，当线程空闲时，它会进入休眠状态，不会消耗大量的CPU资源。

##### Looper中延迟消息谁来唤醒Looper？
> epoll机制？(涉及到底层，应该是epoll机制中有个定时api)

##### 为什么不用 wait 而用 epoll 呢？
> java中的 wait/notify 也能实现阻塞等待消息的功能。但是当阻塞/唤醒控制写在native层时，只使用java的 wait/notify就不够了。

为什么用epoll不用select？
> select只有在调用方法知道函数由返回后，内核才对所有监视的文件描述符进行扫描，而epoll则先通过epoll_ctl()来注册一个文件描述符，
> 一旦基于某个文件描述符就绪时，内核会采用类似callback的回调机制，迅速激活这个文件描述符，当进程调用epoll_wait() 时便得到通知。
> 相比下没有通过遍历文件描述符，而是通过监听回调的的机制。epoll机制是一种高效的IO多路复用机制，当线程空闲时，它会进入休眠状态，不会消耗大量的CPU资源。

简单说说epoll
> 是Linux上I/O多路复用机制，可以同时监控多个描述符，当某个描述符就绪(读或写就绪)，则立刻通知相应程序进行读或写操作。



##### 主线程是一直处于死循环状态，那么android中其他组件，比如activity的生命周期等是如何在主线程执行的？
> 通过binder+Handler发送消息到主线程，再根据消息分发到Handler处理。在ActivityThread中的Looper进入循环之前有个`attach()`的方法。
> 这里会建立起一个本地与服务端的Binder通信，对应的服务端就是ApplicationThread。ApplicationThread呢又会发起与AMS的binder通信。
> AMS就是应用进程的生命周期等的实际管理者。最终就是AMS处理完流程后通过Binder与ApplicationThread通信，ApplicationThread通
> 过Handler发送消息到MessageQueue中。MessageQueue有了消息便不在阻塞，将消息分发到对应的Handler处理，这些消息就包括activity的生命周期等。

##### Looper死循环为什么不阻塞主线程?
> 1、Looper的死循环是一种通俗的说法，它其实是在循环消息处理，不断取出消息，分发下去，执行相关的操作。没有msg则利用epoll机制，
> 让CPU沉睡，来保障当前线程一直在运行而不中断或者卡死。如果主线程的Looper退出了循环，那么代表着这个应用也退出了；
> 2、应用活动是通过各种事件驱动的，也就需要Looper不断从消息队列取出事件，分发事件，处理事件；
> 3、应用活动不是只靠一条主线程，还有其他线程共同作用。比如binder线程。当其他线程发送消息进入到主线程消息队列，也会通过Looper分发到对应的Handler处理。

##### 为什么Looper死循环应用，UI不卡顿(anr)
> Looper的死循环与UI卡顿(ANR)是2码事。
> 1、Looper上的死循环，是不断获取msg，然后分发出去消费掉；在没有msg时，Looper处于空闲状态，线程进入阻塞释放CPU执行权，等待唤醒。
> Looper的死循环是必要的。
> 2、android中的UI活动是事件循环来驱动的，每绘制一帧都是一次消息事件。如果在处理这个消息时的某个方法耗时太长，新来的事件不能及时得到处理，
> 就会出现卡顿、卡死现象，甚至出现ANR(anr是指AMS和WMS检测App的响应时间，如果App在特定时间无法相应屏幕触摸或键盘输入时间，或者特定事件没有处
> 理完毕，就会出现ANR)。所以卡顿现象是Looper取出消息之后的消息处理的情况所致，跟Looper无关。
> 3、Handler还有消息屏障，确保屏障消息可以优先处理，因此感觉不到卡顿。

##### Looper死循环为什么不会发生ANR
> 第一、主线程也就是ActivityThread，启动了Looper，Looper进入循环。不断从消息队列中获取消息，处理消息。进而产生应用的各种活动，
> 应用的活动是各种消息事件来驱动的。当消息队列没有消息时，Looper进入空闲状态，就不会产生任何活动。当Looper循环停止，应用也将结束掉。这
> 也是Looper需要死循环的原因；   
> 第二、ANR是需要当前事件超过一定时间没有得到处理，或者没有完成才会产生。只要事件处理及时，就不会发生ANR。ANR其实跟Looper
> 是没有直接关系的。

##### UI卡顿是怎样产生的
> 处理某个消息事件耗时太长，导致新来的事件不能及时得到处理，就会出现卡顿。一般情况下，事件处理在16ms内完成，就不会感到卡
> 顿。超过16ms的时间越多，感觉就越卡，甚至出现ANR。

##### 为什么会出现ANR，什么是ANR
> 当前的事件超过一定时间没有得到处理，或者没有完成，就会造成ANR。比如：
* Activity中的触摸事件超过 5s；
* Service为 20s；
* BroadCastReceiver 10s；
* ContentProvider 10s；

##### 如何解决ANR
> 可以通过导出系统的ANR日志文件(trace文件)分析定位；


##### 同步屏障问题
> Handler消息分为同步消息、异步消息。发送异步消息只需要在创建Handler实例时，传递一个为值true的async参数。或者使用`msg.setAsynchronous(true)`。
> 同步屏障就是阻碍队列中同步消息的屏障，让异步消息优先执行。设置同步屏障，其实就是发送一个target == null的msg，进入到队列。当检测到这是一个同步
> 屏障(消息)后，MessageQueue就会寻找此后队列中的异步消息处理，忽略掉队列中的同步消息。如果队列一直不存在异步消息，那么线程进入阻塞状态。    

应用之一就是在(ViewRootImpl)view的绘制，卡顿等问题上：
> 我们知道屏幕大概每16ms要刷新一次，如果前面有大量消息，要等到消息都处理完了才去刷新屏幕，那么屏幕要卡住很久，这显然不是用
> 户需要的结果。这时同步屏障就派上用场了。  
移除同步屏障方法： removeSyncBarrier()

##### 同步消息与异步消息
> Handler中的消息分为同步消息、异步消息，屏障消息(同步屏障)。区别是异步消息会有一个特殊的参数。发送异步消息只需要在创
> 建Handler实例时，传递一个为值true的async参数。或者使用`msg.setAsynchronous(true)`。

##### 什么是同步屏障
> 作用：阻碍队列中同步消息的屏障，让异步消息优先执行。
> 设置同步屏障，其实就是发送一个target == null的msg，进入到队列。当检测到这是一个同步屏障(消息)后，
> MessageQueue就会寻找此后队列中的异步消息处理，忽略掉队列中的同步消息。如果队列一直不存在异步消息，那么线程进入阻塞状态。

##### 为什么有同步屏障(异步消息)
> 异步消息与屏障消息是为了避免界面卡顿。   
> 在View的绘制过程中(屏幕大概每16ms要刷新一次),如果前面有其他大量消息，要等到前面消息都处理完才去刷新屏幕，那么屏幕要卡住很久。这不是合理的
> 情况。这时同步屏障与异步消息就能发挥作用。当屏幕需要刷新时，并不是马上就去执行view的绘制，而是会先插入一个同步屏障，然后监听刷新信号(async)。
> 当信号来时会发送一个异步消息，这个消息就是执行绘制。当绘制流程开始时就会移除掉同步屏障。移除同步屏障方法：removeSyncBarrier()。


