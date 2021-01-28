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
给了ThreadLocal。随后执行`Looper.loop()`,看到这个方法：
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
* 序号2：从MessageQueue中取出消息对象
* 序号3：将消息数据发送到对应的Handler去处理
消息是怎么取出来的，看到MessageQueue#next()：
```
Message next() {
     //....省略代码
    for (;;) {
        //....省略代码
        nativePollOnce(ptr, nextPollTimeoutMillis);
        // 。。。
        Message prevMsg = null;
        Message msg = mMessages;
        if (msg != null && msg.target == null) {
            // Stalled by a barrier.  Find the next asynchronous message in the queue.
            do {
               prevMsg = msg;
               msg = msg.next;
            } while (msg != null && !msg.isAsynchronous());
        }
        if (msg != null) {
            if (now < msg.when) {
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
                return msg;
            }
        } else {
            // No more messages.
            nextPollTimeoutMillis = -1;
        }
        //省略代码。。。
   }
}
```











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
