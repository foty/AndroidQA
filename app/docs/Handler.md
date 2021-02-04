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
        Message msg = mMessages;
        if (msg != null && msg.target == null) {
            // Stalled by a barrier.  Find the next asynchronous message in the queue.
            do {
               prevMsg = msg;
               msg = msg.next;  // 2 取出msg
            } while (msg != null && !msg.isAsynchronous());
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
序号1-> 通过nativePollOnce()来阻塞线程。如果有消息来，线程会被唤醒，取出消息返回；如果没有消息，线程重新进入阻塞状态，直达被再唤
醒。`nextPollTimeoutMillis`表示阻塞的时长。0表示立即返回，-1表示持续阻塞，除非被唤醒。`nativePollOnce()`是navive层方法，
通过epoll机制实现阻塞与唤醒。在java层对应的方法就是`nativePollOnce()`/`nativeWake()`。(太菜了，不懂linux，深入不下去，跳过吧)。  
2-> 取出msg。   
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
看到上面了，`msg.target`就是Handler本身。所以`msg.target.dispatchMessage(msg)`最终是回到了Handler#dispatchMessage()。而当我们重写
了Handler的dispatchMessage()后，消息最终到达我们写的dispatchMessage()方法。流程结束。   
为了探究整个消息发送的流程，跟踪进入到MessageQueue#enqueueMessage()方法：
```
    boolean enqueueMessage(Message msg, long when) {
         // 省略代码。。。
            msg.markInUse();
            msg.when = when;  //时间，与Looper取出msg的时间有关
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) { 
                nativeWake(mPtr);  //唤醒Looper，不在阻塞。
            }
        }
        return true;
    }
```
上面就是


###### select、poll、epoll
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




###### Handler面试的几个问题
Handler 的基本原理  
子线程中怎么使用 Handler  
MessageQueue 获取消息是怎么等待  
为什么不用 wait 而用 epoll 呢？  
多个线程给 MessageQueue 发消息，如何保证线程安全  
非 UI 线程真的不能操作 View 吗    
一个线程有几个looper，几个handler，如果你说一个，他会问，如何保证looper唯一        
> 1个  
我们能在主线程直接new无参handler吗  
> 可以  
主线程是一直处于死循环状态，那么android中其他组件，比如activity的生命周期等式如何在主线程执行的？  
>开启了子线程/新进程执行  
子线程能new handler吗?我们应该怎么样在子线程new handler  
> 在特殊情况处理下可以在子线程new handler。  
为什么主线程不用调用looper.prepar和looper.looper   
我们的looper通过什么实现与线程关联  
为什么looper死循环应用(UI)不卡顿(anr) (那你谈谈epoll机制)  
如果我们的子线程没有消息处理的情况下，我们如何优化looper   
Handler 怎么进行线程通信，原理是什么？  
ThreadLocal 的原理，以及在 Looper 是如何应用的？  
Handler#post(Runnable) 是如何执行的  
Handler#sendMessage() 和 Handler#postDelay() 的区别？  
多个 Handler 发消息时，消息队列如何保证线程安全？
为什么 MessageQueue 不设置消息上限，message上限怎么办。  
Looper 死循环为什么不阻塞主线程？  
Handler内存泄漏的原因？  
Message.callback 与 Handler.callback 哪个优先？  
Handler.callback 和 handlemessage() 都存在，但 callback 返回 true，handleMessage() 还会执行么？  
IdleHandler 是什么？怎么使用，能解决什么问题？  
同步屏障问题  
Looper会一直消耗系统资源吗？  
android的Handle机制，Looper关系，主线程的Handler是怎么判断收到的消息是哪个Handler传来的？  
Handler机制流程、Looper中延迟消息谁来唤醒Looper？  
handler机制中如何确保Looper的唯一性？  
Handler 是如何能够线程切换，发送Message的？
