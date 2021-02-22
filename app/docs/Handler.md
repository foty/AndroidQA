##### Android下的消息机制
先了解几个机制下的对象  

###### ThreadLocal  
ThreadLocal 是用来存储指定线程的数据的。当某些数据的作用域是该指定线程并且该数据需要贯穿该线程的所有执行过程时就
可以使用ThreadLocal 存储数据。当某线程使用ThreadLocal存储数据后，只有该线程可以读取到存储的数据，除此线
程之外的其他线程是没办法读取到该数据的。就算是同一个ThreadLocal对象，任意线程对其的set()和get()方法的操作都是相
互独立互不影响的。 在创建handler对象的时候，在handler的构造方法内有一行`mLooper = Looper.myLooper();`代码用
来获取Looper对象。其中` Looper.myLooper()`内部就是使用 ThreadLocal来保存的一个Looper对象。ThreadLocal的谁
保存谁能访问的特性也是面试中 Handler能不能在子线程中创建的答案依据之一。

###### Handler 
正如名称意思，Handler就是处理者的意思,处理消息,发送消息。

###### MessageQueue
单向链表数据结构,存放Message，也就是发送出去的数据对象。

###### Looper 消费 Message
Looper，轮询器，负责从MessageQueue取出Message，并将它分发到指定的Handler进行处理。

###### 简述工作流程
先了解通常情况使用Handler的个常用姿势：
```姿势1
new Handler().post(new Runnable() {
    @Override
    public void run() {
         //todo
    }
});
```
```java 姿势2,3
Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
       // todo
    }
};
mHandler.sendMessage(new Message());  //2
mHandler.obtainMessage(1).sendToTarget(); //3
```
1、准备阶段。即创建Handler对象的过程。  
Handler的构造方法有分无参构造，1个参数构造，2个参数构造和3个参数构造方法，看2个参数跟3个参数的方法
```
```
    public Handler(@Nullable Callback callback, boolean async) {
        if (FIND_POTENTIAL_LEAKS) {
            final Class<? extends Handler> klass = getClass();
            if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
                    (klass.getModifiers() & Modifier.STATIC) == 0) {
                Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                    klass.getCanonicalName());
            }
        }
        mLooper = Looper.myLooper(); //获取Looper
        if (mLooper == null) {
            throw new RuntimeException(
                "Can't create handler inside thread " + Thread.currentThread()
                        + " that has not called Looper.prepare()");
        }
        mQueue = mLooper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```
public Handler(@NonNull Looper looper, @Nullable Callback callback, boolean async) {
    mLooper = looper;
    mQueue = looper.mQueue;
    mCallback = callback;
    mAsynchronous = async;
}
```
无参构造最终走向是2个参数的构造方法，这里也是选取2个参数的方法跟踪流程。  
创建Handler实例时获取了Looper对象。跟踪进去会发现`Looper#myLooper()`方法Looper实例是从ThreadLocal中获取的。而设置Looper可以追溯到ActivityThread的启动。
我们知道在执行ActivityThread#main()的时候就有调用`Looper.prepareMainLooper();`,这里就在ActivityThread所在的线程设置给了Looper再将这个Looper设置
给了ThreadLocal。这也是ActivityThread所在的现在就称之为主线的原因之一。所以当我们在Activity中创建的Handler，发送的消息时，都是运行在这条线程上的。并且可以
直接使用，不需要再做其他的操作，在Activity创建运行时，主线程的Looper就已经一直在运行了。接着看到`Looper.loop()`,看到这个方法：
```
    public static void loop() {
        final Looper me = myLooper();  //1 
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        //。。。省略代码
        
        final MessageQueue queue = me.mQueue; //2
        for (;;) {
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
            // 省略代码。。
            try {
                msg.target.dispatchMessage(msg);  //3 分发消息
                if (observer != null) {
                    observer.messageDispatched(token, msg);
                }
                dispatchEnd = needEndTime ? SystemClock.uptimeMillis() : 0;
            }
            //省略代码。。
        }    
```
分析：  
* 序号1：获取Looper，这里如果当前线程并没有设置Looper的话，me == null。也是保证了保存的looper与取出的looper是在同一个线程下的同一个looper。
* 序号2：从MessageQueue中取出消息对象。
* 序号3：将消息数据发送到对应的Handler去处理。
具体跟踪消息是怎么取出来的，看到MessageQueue#next()：
```
Message next() {
     //....省略代码
    for (;;) {
        //....省略代码
        nativePollOnce(ptr, nextPollTimeoutMillis);  //1 调用本地方法，阻塞nextPollTimeoutMillis毫秒
        // 。。。
        Message prevMsg = null;
        Message msg = mMessages; 2 取出msg
        if (msg != null && msg.target == null) {  // 设置同步屏障。
            // Stalled by a barrier.  Find the next asynchronous message in the queue.
            do {
               prevMsg = msg;
               msg = msg.next;  
            } while (msg != null && !msg.isAsynchronous()); // 开启了同步屏障后，只选择异步消息，同步消息不执行。
        }
        if (msg != null) {
            if (now < msg.when) { // 3是否延时
                // Next message is not ready.  Set a timeout to wake up when it is ready.
                nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
            } else {
                // Got a message.
                mBlocked = false;
                if (prevMsg != null) {
                    prevMsg.next = msg.next;
                } else {
                    mMessages = msg.next;
                }
                msg.next = null;
                if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                msg.markInUse();
                return msg;   // 4 返回msg。
            }
        } else {
            // No more messages.  //5 没有消息，继续阻塞不给返回
            nextPollTimeoutMillis = -1;
        }
        //省略代码。。。
   }
}
```
解析：  
取出消息的方法`Message#next()`整体上是处于一个死循环中。  
序号1-> 通过nativePollOnce()来阻塞线程。如果有消息来，线程会被唤醒，取出消息返回；如果没有消息，线程重新进入阻塞状态，直达被再次唤
醒。`nextPollTimeoutMillis`表示阻塞的时长。0表示立即返回，-1表示持续阻塞，除非被唤醒。`nativePollOnce()`是navive层方法，
通过epoll机制实现阻塞与唤醒。在java层对应的方法就是`nativePollOnce()`/`nativeWake()`。(太菜了，不懂linux，深入不下去，跳过吧)。  
2-> 取出msg。注意是否开启同步屏障   
3-> 处理延时消息  
4-> 返回msg,回到Looper处理。  
回到Looper#loop()的序号3。要想知道msg最后是怎么被处理的,就要先搞清楚这个`msg.target`。target是Message中的属性，试试能不能在它的构造方
法中找到什么。结果就是在使用Message的空构造，并没有任何处理逻辑。猜测在消息被发送后，处理的target。只能回到创建handler，发送消息开始。
看`Handler#sendMessage()`。一路跟踪后，它的调用链如下：   
Handler#sendMessage() -> Handler#sendMessageDelayed() -> Handler#sendMessageAtTime() -> Handler#enqueueMessage()
```
    private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg,
            long uptimeMillis) {
        msg.target = this;  //在这里赋值。
        msg.workSourceUid = ThreadLocalWorkSource.getUid();
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return queue.enqueueMessage(msg, uptimeMillis);
    }
``` 
看到上面了，`msg.target`就是Handler本身。所以`msg.target.dispatchMessage(msg)`最终是回到了Handler#dispatchMessage()。看下
Handler#dispatchMessage()方法:  
```
    public void dispatchMessage(@NonNull Message msg) {
        if (msg.callback != null) {
            handleCallback(msg); // 1
        } else {
            if (mCallback != null) { // 2
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg); // 3
        }
    }
    
    // 1的具体方法
    private static void handleCallback(Message message) {
        message.callback.run();
    }
```
首先判断msg的callback是否为空，如果不是null,则执行它的run方法。接着判断mCallback，mCallback是在创建Handler实例时候被赋值，最后才是我们重写了Handler
的dispatchMessage()后，消息最终执行到我们写的dispatchMessage()方法。总的来说就是一个回调顺序的优先级。相对应的是不同的使用构造方法，不同的发送消息方式：  
1、与构造方法无关，使用postXXX()方式发送消息使用此回调,常见的如 post()、postDelay()；
2、与创建对象时使用的构造方法有关，需要一个Handler.Callback对象；
3、默认情况，使用也是最多的。  
为了探究整个消息发送的流程，从消息的发送，取出，分发都有了，还差一个入队的步骤。跟踪进入到MessageQueue#enqueueMessage()方法：
```
    boolean enqueueMessage(Message msg, long when) {
         // 省略代码。。。
            msg.markInUse();
            msg.when = when;  //时间，与Looper取出msg的时间有关
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) { // 第一个msg或者插队msg
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;  //并不需要特殊处理,保持原来状态。
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p; 
                    p = p.next;
                    if (p == null || when < p.when) { //遍历这个消息队列，找到最尾那个消息，或者按照时间找到需要插队的位置
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;  // 入队
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) { 
                nativeWake(mPtr);  //如果有需要，唤醒Looper，不在阻塞。
            }
        }
        return true;  // msg入队成功返回
    }
```
msg入队列有2种情况：  
1、非正常入队：  
p == null -> 是第一个msg  
when == 0 -> when默认SystemClock.uptimeMillis()，不太可能为0  
when < p.when -> 准备入队msg的时间<当前队列处理的msg,可以认为前一个msg(mMessages)是延迟/异步消息，这个msg是插队。  
2、正常入队：  
p == null || when < p.when -> 找到最后一个msg，或者按照时间找到需要插队的位置  
整个msg入队的过程都不需要主动去改变Looper线程状态，保持原状态。


###### select、poll、epoll机制的一点理解
select、poll、epoll都是IO多路复用机制，可以同时监控多个描述符，当某个描述符就绪(读或写就绪)，则立刻通知相应程序进行读或写操作。本质上select、
poll、epoll都是同步I/O，即读写是阻塞的。

* select 该函数监控3类文件描述符，调用select函数后会阻塞，直到描述符fd准备就绪（有数据可读、可写、异常）或者超时，函数便返回。 当select函数返回后，可通过
遍历描述符集合，找到就绪的描述符。缺点：1、文件描述符个数受限：单进程能够监控的文件描述符的数量存在最大限制，在Linux上一般为1024，可以通过修改宏定义增大上限，
但同样存在效率低的弱势;2、性能衰减严重：IO随着监控的描述符数量增长，其性能会线性下降;

* poll  监控文件描述符数量上没有最大数量限制。和select函数一样，当poll函数返回后，可以通过遍历描述符集合，找到就绪的描述符。缺点：1、select和poll都需要
在返回后，通过遍历文件描述符来获取已经就绪的socket。同时连接的大量客户端在同一时刻可能只有很少的处于就绪状态，因此随着监视的描述符数量的增长，其性能会线性下降。

* epoll 在内核2.6中提出的，是select和poll的增强版。相对于select和poll来说，epoll更加灵活，没有描述符数量限制。epoll使用一个文件描述符管理多个描述符，
将用户空间的文件描述符的事件存放到内核的一个事件表中，这样在用户空间和内核空间的copy只需一次。epoll机制是Linux最高效的I/O复用机制，在一处等待多个文件句
柄的I/O事件。 epoll操作过程有3个方法，分别是epoll_create()->创建句柄， epoll_ctl()->操作监听的文件描述符，epoll_wait()->等待上报。

epoll的对比与优势：  
在 select/poll中，进程只有在调用一定的方法后，内核才对所有监视的文件描述符进行扫描，而epoll事先通过epoll_ctl()来注册一个文件描述符，一旦基于某个
文件描述符就绪时，内核会采用类似callback的回调机制，迅速激活这个文件描述符，当进程调用epoll_wait() 时便得到通知。(此处去掉了遍历文件描述符，而是
通过监听回调的的机制。这正是epoll的魅力所在。)   

1、监视的描述符数量不受限制；  
2、IO性能不会随着监视fd的数量增长而下降。epoll不同于select和poll轮询的方式，而是通过每个fd定义的回调函数来实现的，只有就绪的fd才会执行回调函数。



参考出处：
* http://gityuan.com/2015/12/06/linux_epoll/





###### Handler的几个问题
* 1、Handler 的基本原理
> 上面总结。  

* 2、子线程中怎么使用 Handler?
> 在创建Handelr对象前调用`Looper.prepare();`,在创建完后调用`Looper.loop();`。

* 3、MessageQueue获取消息是怎么等待?
> 通过native方法`nativePollOnce()`来阻塞线程实现消息等待。`nativePollOnce()`则是由native层epoll机制中的epoll_wait()函数等待的。

* 4、为什么不用 wait 而用 epoll 呢？  
> java中的 wait/notify 也能实现阻塞等待消息的功能。但是当阻塞/唤醒控制写在native层时，只使用java的 wait/notify就不够了。至于为什么用epoll不用select？
select只有在调用方法知道函数由返回后，内核才对所有监视的文件描述符进行扫描，而epoll则先通过epoll_ctl()来注册一个文件描述符，一旦基于某个文件描述符就绪时，
内核会采用类似callback的回调机制，迅速激活这个文件描述符，当进程调用epoll_wait() 时便得到通知。相比下没有通过遍历文件描述符，而是通过监听回调的的机制。
epoll机制是一种高效的IO多路复用机制，当线程空闲时，它会进入休眠状态，不会消耗大量的CPU资源。

* 5、多个线程给MessageQueue发消息，如何保证线程安全?
> 用锁(synchronized)

* 6、非UI线程真的不能操作 View 吗?
> 可以。准确来讲操作View其实并不是限定UI线程，而是源线程。就是创建View所在的线程与操作View的线程是不是同一个。如果在子线程创建View，那在这个子线程操作这
个View
是完全可以的。

* 7、一个线程有几个looper，几个handler，如果你说一个，他会问，如何保证looper唯一?     
> 1个，多个Handler。通过ThreadLocal保证looper唯一。1、ThreadLocal的特性是当某线程使用ThreadLocal存储数据后，只有在该线程可以读取到存储的数据(谁保存谁访
问)。ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，Looper为value保存。2、并且保存Looper方
法只能
被调用一次。

* 8、我们能在主线程直接new无参handler吗?
> 可以，主线程的Looper在ActivityThread初始化的时候就被初始化了。

* 9、主线程是一直处于死循环状态，那么android中其他组件，比如activity的生命周期等式如何在主线程执行的？  
> 通过binder+Handler发送消息到主线程，再根据消息分发到Handler处理。在ActivityThread中的Looper进入循环之前有个`attach()`的方法。这里会建立起一个本地与
服务端的Binder通信，对应的服务端就是ApplicationThread。ApplicationThread呢又会发起与AMS的binder通信。AMS就是应用进程的生命周期等的实际管理者。最终就
是AMS处理完流程后通过Binder与ApplicationThread通信，ApplicationThread通过Handler发送消息到MessageQueue中。MessageQueue有了消息便不在阻塞，将消
息分发到对应的
Handler处理，这些消息就包括activity的生命周期等。
  
* 10、为什么主线程不用调用looper.prepare()和looper.looper()?
> 因为主线程的Looper已经在ActivityThread初始化的时候就调用了这2个方法。

* 11、我们的looper通过什么实现与线程关联?
> ThreadLocal。ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，Looper为value保存。

* 12、为什么looper死循环应用，UI不卡顿(anr) (那你谈谈epoll机制)?
> Looper的死循环与UI卡顿(ANR)是2码事。Looper上的死循环，是不断获取msg，然后分发出去消费掉；在没有msg时，Looper处于空闲状态，线程进入阻塞释放CPU执行权，
等待唤醒。Looper的死循环是必要的。UI是事件循环来驱动的，每绘制一帧都是一次消息事件。如果在处理这个消息时的某个方法耗时太长，新来的事件不能及时得到处理，就会
出现卡顿、卡死现象，甚至出现ANR(anr是指AMS和WMS检测App的响应时间，如果App在特定时间无法相应屏幕触摸或键盘输入时间，或者特定事件没有处理完毕，就会出现ANR)。
所以卡顿现象是Looper取出消息之后的消息处理的情况所致，跟Looper无关。此外Handler还有消息屏障，确保屏障消息可以优先处理，因此感觉不到卡顿。

* 13、Looper死循环为什么不阻塞主线程?
> 要搞清楚一件事是Looper的死循环是一种通俗的说法，它其实是在循环消息处理，不断取出消息，分发下去，执行相关的操作。没有msg则利用epoll机制，让CPU沉睡，来保障当
前线程一直在运行而不中断或者卡死。如果循环结束了，那么应用也结束了。从某个角度上看，Looper的死循环确实是在阻塞主线程，没有消息时线程阻塞，CPU进入空闲状态。如果
主线程的Looper退出了循环，那么代表着这个应用也退出了。而且应用活动不仅仅是只靠一条主线程，还有其他线程共同作用。比如binder线程。当其他线程发送消息进入到主线
程消息队列，也会通过Looper分发到对应的Handler处理。

* 14、如果我们的子线程(创建Handler)没有消息处理的情况下，我们如何优化looper
> 如果子线程是长期的，可以参考主线程Looper，让其阻塞，需要时在唤醒。如果是短期的，可以直接调用Looper#quit()退出，释放内存，结束线程。

* 15、ThreadLocal的原理，以及在Looper是如何应用的？
> 前面ThreadLocal介绍。ThreadLocal内部会基于当前线程维护一个ThreadLocalMap(定制的哈希映射表)。表中实体对象以ThreadLocal为key，Looper为value保存。
保证Looper的唯一性。

* 16、Handler#post(Runnable) 是如何执行的?
> 先通过getPostMessage()将Runnable对象转换成Message对象(Runnable对象是Message的callback),赋值给到Message.callback。然后入队列，最后在
dispatchMessage的时候回调message.callback.run()。其他没有特别的地方。

* 17、Handler#sendMessage() 和 Handler#postDelay()的区别？  
> 首先共同点是俩者都会执行到sendMessageDelayed()方法。#sendMessage()是直接传递一个Message对象，不需要额外再处理参数；#postDelay()传递的是Runnable对象，
需要通过getPostMessage()将Runnable参数转换成Message参数。把Runnable对象赋值给Message的callback属性。最后消息分发的时候回调Runnable的run方法。

* 18、多个Handler发消息时，消息队列如何保证线程安全？
> 用synchronized代码块去进行同步

* 19、为什么 MessageQueue 不设置消息上限，message上限怎么办。
> 个人理解是应用的活动是由消息事件驱动的，无法计算出一个应用活动需要多少消息事件，所以不设置上限。其实在调用sendMessage()的时候，是向message这个链表的尾端
插入一个message，这个长度是没有限制的。但是如果你不断通过new message的方式去调用sendMessage()，是会出现内存溢出的问题；可以使用Message.obtain()方法
代替new对象来获取消息，此方法在消息池中获得一个消息时，消息池中缓存的消息数就减1。当消息池中的消息数大于MAX_POOL_SIZE的时候，则消息池中的消息数不加一，也不
将消息添加到消息池中，而这个消息池主要用来重复利用从而避免更多的内存消耗。

* 20、Handler内存泄漏的原因？
> MessageQueue持有Message，Message持有activity

* 21、Message.callback 与 Handler.callback 哪个优先？ 
>  优先级： Message.callback > Handler.callback(mCallback)

* 22、Handler.callback和handleMessage()都存在，但callback返回true，handleMessage()还会执行么？  
> 不会，有return关键字。

* 23、IdleHandler是什么？怎么使用，能解决什么问题？
> 空闲监听器，是一个线程空闲(没有消息处理)时执行逻辑的接口，有一个Boolean类型返回值的方法。当返回false时，会移除自己，不再执行。当消息队列空闲时会回调它
的queueIdle()方法。使用方法:调用MessageQueue#addIdleHandler()方法。能解决什么问题：主线程能做的，它都能做，适合优先级没有那么高的一些任务，
也不能太耗时；像在一些第三方工具上。

* 24、同步屏障问题  
> Handler消息分为同步消息、异步消息。发送异步消息只需要在创建Handler实例时，传递一个为值true的async参数。或者使用`msg.setAsynchronous(true)`。同步屏
障就是阻碍队列中同步消息的屏障，让异步消息优先执行。设置同步屏障，其实就是发送一个target == null的msg，进入到队列。当检测到这是一个同步屏障(消息)后，
MessageQueue就会寻找此后队列中的第一个异步消息处理，忽略掉队列中的同步消息。如果队列一直不存在异步消息，那么线程进入阻塞状态。    
应用之一就是在ViewRootImpl：我们知道屏幕大概每16ms要刷新一次，如果前面有大量消息，要等到消息都处理完了才去刷新屏幕，那么屏幕要卡住很久，这显然不是用户需要的
结果。这时同步屏障就派上用场了。  
移除同步屏障方法： removeSyncBarrier()

* 25、Looper会一直消耗系统资源吗?
> 不会，looper获取msg的过程使用到了epoll机制。epoll机制是一种高效的IO多路复用机制，当线程空闲时，它会进入休眠状态，不会消耗大量的CPU资源。

* 26、主线程的Handler是怎么判断收到的消息是哪个Handler传来的？
> 通过Message的target来标注。

* Handler机制流程、Looper中延迟消息谁来唤醒Looper？   
* Handler是如何能够线程切换，发送Message的?
* 为什么建议用obtain方法创建Message?
> Message本身包含两个Message对象，一个是sPool，一个是next，但通过看源码可知道sPool是一个static对象，是所有对象共有，Message.sPool就是一个单链表结构，
Message就是单链表中的一个节点。使用obtain方法，取的是Message的sPool，改变sPool指向sPool的next，取出sPool本身，并清空该Message的flags和next。这样
的好处是是可避免重复创建多个实例对象，可以取消息池子之前已经存在的消息。
