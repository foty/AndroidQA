##### Android下的消息机制(Handler)
* handler原理
* 常见面试问题   [](../aQA/answer/android/HandlerQA.md)


一、先了解几个机制下的对象    

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
``` 姿势2,3
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
    
    // 3参数构造方法
    public Handler(@NonNull Looper looper, @Nullable Callback callback, boolean async) {
        mLooper = looper;
        mQueue = looper.mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }
```
无参构造最终走向是2个参数的构造方法，这里也是选取2个参数的方法跟踪流程。  
创建Handler实例时获取了Looper对象。跟踪进去发现`Looper#myLooper()`方法Looper实例是从ThreadLocal中获取的。而设置Looper可以追溯到ActivityThread的启动
。我们知道在执行ActivityThread#main()的时候就有调用`Looper.prepareMainLooper();`,这里就在ActivityThread所在的线程设置给了Looper再将这个Looper设
置给了ThreadLocal。这也是ActivityThread所在的现在就称之为主线的原因之一。所以当我们在Activity中创建的Handler，发送的消息时，都是运行在这条线程上的。并且
可以直接使用，不需要再做其他的操作，在Activity创建运行时，主线程的Looper就已经一直在运行了。接着看到`Looper.loop()`,看到这个方法：
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
                if (prevMsg != null) { //如果是同步屏障，prevMsg就会被赋值，mMessages不会被改变，导致屏障一直存在，除非手动取消。
                    prevMsg.next = msg.next;
                } else {
                    mMessages = msg.next; //没有屏障，直接取队列下一个元素。
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
