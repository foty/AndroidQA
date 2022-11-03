

##### 设计模式
观察者模式：又称发布/订阅模式。有一种一对多的依赖关系，让一个对象的状态发生改变，所有具有依赖关系的对象都能够得到通知。
优点：
观察者与被观察者是抽象耦合，容易扩展
缺点：
通知顺序问题。避免出现循环通知；
观察者数量问题。观察者数量一旦非常多，通知所有的观察者的效率会降低。

装饰模式：
在不改变现有对象结构的情况下，动态地增加对象额外功能。
优点：
可以动态的扩展功能，同时也可以选择不同的装饰器实现不同功能，结构灵活。
缺点：
业务庞大，装饰层很多，维护困难。
耦合性问题。装饰一般使用继承或者实现关系。

单例模式：
保证应用内一个类只存在一个实例。可以节省内存资源，提高性能。
实现方式：
懒汉式：先判断是否已经初始化。进阶版(双重锁校验法DCL)
饿汉式：直接初始化。进阶版(静态内部类)

策略模式：
对于一组行为或者方法，将每个行为封装到具有公共接口的独立类中，使得他们可以相互替换，可以在不影响到客户端情况下发生改变。
好处：
可以避免使用多重条件语句；增加代码阅读性。
缺点：
会产生比较多的策略类。

建造者：
将一个复杂对象的构建分离表示，这样可以使用同样的代码构建不同的对象。好处是对那些构成复杂又多变的对象有奇效，可以灵活修改
构建对象的构建部分。

##### 图片压缩(优化)
大小压缩：直接传入新图大小，生成新图片；[Bitmap.createBitmap()]
采样率压缩：压缩时图片的宽高按比例压缩，压缩后图片像素数会减少。[设置Options参数，BitmapFactory.decodeResource()]
质量压缩：特点是图片文件大小会减小，但是图片的像素数不会改变，占据的内存不会减少。[Bitmap.compress()]

##### kotlin（协程）
[](kotlin/Kotlin基础.md)

##### java
[](qa/java部分.md)

##### jetpack
viewBinding、viewModel [](jetpack/ViewModel.md)、
lifecycle [](jetpack/Lifecycle.md)）

##### HashMap、ArrayList
HashMap [](data_structure/HashMap.md)
##### HashMap的原理
> HashMap是基于数组+链表结构设计。数组中的每个元素都是一条链表的表头。数组默认长度为16。添加元素时，根据key的hash
> 值与数组长度-1的差做与运算，得到元素在数组中的位置。如果数组对应索引存在元素并且它们的hash值不一样，就将新元素添加到后
> 面(尾插法，jdk1.7是头插法)成为该链表的一个新节点。jdk1.8后，引入红黑树，当hash数组的容量大于64且链表长度大于8时，
> 链表转红黑树；当长度小于6的时，红黑树转链表。


##### HashMap中get()、put()如何实现的
put：
> 1、计算key的hash值；   
> 2、计算key在数组中的索引(hash&(arr.length-1)；   
> 3、如果当前索引对应的数组元素为null，直接在该索引上，充当链表头几点；如果不为null，比较key的hash值或者equals，
> 有一个不同时将新元素插入到链表成为新节点；如果都hash值跟equals都相等，替换value。

get:
> 1、计算key的hash值；   
> 2、计算key在数组中的索引(hash&(arr.length-1)；    
> 3、key的hash值判断，如果是链表头节点则返回头结点；如果不是，判断表头节点是否属于红黑树。如果是，遍历红黑树查找
> 到目标，否则遍历链表找到指定元素。


ArrayList [](data_structure/ArrayList.md)
底层数据结构是Object可变数组(自动扩容)，添加元素默认是在size位置。每次添加新元素都会判断是否需要扩容。数组进行扩容
时，会将老数组中的元素重新拷贝一份到新的数组中，新数组容量是原容量的1.5倍。

特点：
查询效率高，有索引
增删效率低
线程不安全

注意：删除元素时，数组容量不会改变。

##### 锁、Synchronized 
[](java/并发(锁).md)
概念：
用于同步代码块或者方法，能够保证原子性、可见性、有序性(通过阻塞实现按顺序执行)。是一种重量级别的锁。重量级锁会让它申请
的线程进入阻塞，影响效率。

原理：
> 对jvm层面的monitor对象加锁解锁实现同步。方法级别的同步是根据方法表中的同步标志判断是否是同步方法。如果是则会先去获
> 取monitor对象，执行完方法释放monitor对象；代码级别的同步是根据monitor-enter和monitor-exit指令完成。执行
> monitor-enter获取monitor对象，执行monitor-exit，释放monitor对象。

> 使用Synchronized修饰方法通过编译后，会在方法表结构中设置ACC_SYNCHRONIZED访问标识。每个对象都与一个monitor相关联，当且仅当
> monitor被线程持有时，monitor会处于锁定状态，也就是加锁。当方法执行时，线程将先尝试获取对象相关联的monitor所有权，
> 然后再执行方法，最后在方法完成(无论是正常执行还是非正常执行)时释放monitor所有权。在方法执行期间，线程持有了monitor
> 所有权，其它任何线程都无法再获得同一个对象相关联的monitor所有权。

死锁：


##### 线程池
[](java/线程池.md)

使用线程池的好处:
> 重用线程，减少新建-销毁线程的开销；  
> 有效控制线程并发数量，避免线程间抢占系统资源而导致阻塞；   
> 能够统一管理；

线程池核心参数：
> 核心线程数量、最大线程容量、线程存活时间、时间单位、创建线程工厂、工作队列、拒绝策略

线程池工作原理   
>1、当线程池中有任务需要执行时，线程池会判断当前池内线程数量有没有超过核心数量，没有就会新建线程进行任务执行(核心线程)；    
2、如果池中的线程数量超过核心线程数，这时候任务就会被放入任务队列中排队等待执行；   
3、如果任务队列的任务数量超过最大队列容量(入队失败)，但是线程池没有达到最大线程数，就会新建线程来执行任务(非核心线程)；   
4、如果超过了最大线程数(无法创建非核心线程)，就会执行拒绝策略；

>总结：提交顺序：核心线程池 > 队列 > 非核心线程池；执行顺序：核心线程池 > 非核心线程池 > 队列


##### view的绘制流程 
[](android/View.md)
简述版本：
> view的绘制有三个步骤：测量(measure)，布局(layout)和绘制(draw), 从DecorView自上而下遍历整个View树。
> 1、测量阶段： 通过调用View的measure()方法获取MeasureSpec(测量规格)信息，经过多次测量，对比最后调用
> setMeasuredDimension()来设置自身大小。子view还可以重写View#onMeasure()，自己设置大小。   
> 2、布局阶段： layout()方法中会先调用setFrame()给自身的left，top，right，bottom属性赋值,于是自己在父View中的位置就
> 确定。然后会调用onLayout()方法，让子View(一般指ViewGroup)自己实现。    
> 3、绘制阶段： 一般分4步：[1、绘制背景；2、绘制视图内容,回调onDraw()；3、绘制子View，回调dispatchDraw()，一般
> ViewGroup用的比较多；4、绘制装饰，比如滚动条之类的。]

##### 事件分发机制 
[](android/事件分发.md)

简述版：
> 事件先传到Activity、再传到ViewGroup、最终再传到目标View。事件传递到Activity中，首先执行dispatchTouchEvent()对
> 事件分发。将事件传递到ViewGroup。但是如果ViewGroup或者最后的View都没有消费事件，事件进到activity的
> onTouchEvent()方法，交给activity处理事件。事件传递到ViewGroup，先调用onInterceptTouchEvent()方法，判断是否
> 拦截事件。如果不拦截事件，会遍历所有子view，将事件传递到目标View，进入到View的onTouchEvent()处理事件；如果拦截事
> 件，那么事件将会进入到自己onTouchEvent()方法，并且往后所有事件都不会再往下传递，最终由该ViewGroup处理事件。

事件冲突：
内部拦截法：
> 子View在dispatchTouchEvent方法中通过调用requestDisallowInterceptTouchEvent(true)方法，禁止父View拦截事件。
> 并在合适的场景将其置为 false允许拦截。

外部拦截法：
> 由父View通过重写onInterceptTouchEvent方法，在合适的场景拦截事件。


##### git版本管理流程


##### handler
[](android/Handler.md)

工作原理简述版本：
> Handler调用post()或者postDelay()将Message发送到Looper的消息队列MessageQueue中，通过Looper内部的循环机制获
> 取Message，处理Message。然后Message的target属性，也就是Handler实例，调用dispatchMessage()方法，最后会执行到
> handleMessage()方法。这时我们只要在需要接收位置重写handleMessage()，就可以执行对应的操作。

IPC
[](android/binder.md)

##### liveData 
liveDataBus [](third_frame/livedata.md)
EventBus

##### Okhttp
[](third_frame/OkHttp3.md)
基本流程
> 1、通过OkhttpClient创立一个Call，并发起同步或者异步请求；
> 2、okhttp会通过Dispatcher对我们所有的RealCall(Call的具体实现类)请求进行统一管理，选择同步或者异步请求进行处理；
> 3、同步请求会加入到同步队列中执行，异步请求会加入到异步队列中请求；
> 4、经过5个拦截链后得到响应数据，返回给调用者。

##### OkHttp的基本实现原理
> 通过5个拦截器，3个双端队列(2个异步队列，一个同步队列)，结合责任链模式共同实现，将网络请求的各个阶段封装到各个链条中，实现每个层次的解耦。  
> 请求底层：通过socket发送http请求与接收响应。okHttp实现了连接池的概念，也就是同一个主机的多个请求。可以公用一个socket连接，而不是每次发送
> 完http请求就关闭底层socket。

##### recyclerview
复用机制
> recyclerview复用回收的是viewHolder结构。ViewHolder是用来包装view的，可以将它看成列表的ItemView。复用机制就是
> 将ViewHolder放到缓存，然后从缓存中取出来。recyclerview一共有4个缓存级别。分别是Scrap(片段)、Cache、自定义扩展
> 和RecycledViewPool。其中scrap分为attachScrap和changeScrap。区别是holder是否有变化，都是缓存还处于屏幕内的holder。
> cache是存放划出屏幕外的holder，一般是固定2个。上下各一个。自定义扩展是交给开发者自己实现的，没怎么用过。最后缓存池
> 是当cache满了之后，就会将holder移交到缓存池中。复用机制获取缓存策略是Scrap -> CachedView -> 
> ViewCacheExtension -> RecycledViewPool -> createViewHolder()。

##### Glide
[](third_frame/glide.md)
优点：
* 高效的缓存策略。
* 内存开销小。默认Bitmap格式是RGB_565。
* 支持 Gif、WebP、缩略图，甚至Video
* 图片显示效果为渐变，更加平滑

缓存原理
通过with方法，绑定生命周期；通过load方法构建请求，最后通过into方法发起请求，从缓存或者服务器中获取到图片资源，设置图片。
> glide从缓存中获取图片有2种形式，内存缓存与磁盘缓存(DiskLruCache)。内存缓存又分2种：ActiveResources(弱引用缓存集-HashMap
> )与LruResourceCache - LinkedHashMap


##### tcp/ip、http 
[](http/net.md)

Http：
即超文本传输协议(属于应用层)，基于TCP/IP协议传输数据，使用报文格式数据(请求行、请求头部、请求数据)

Https:
加密的Http。

协议缓存：


5层数据模型：
> 应用层(http)、传输层(tcp/udp)、网络层(ip)、链路层、物理层。

3次握手
> 1、客户端发送syn包给服务器，等待服务器确认；
> 2、服务端收到返回一个ask，同时发送一个syn包，服务器进入准备接受状态；
> 3、客户端收到ask很syn后，发送服务端一个ask，然后开始传输数据；

4次挥手
> 1、客户端向服务端发送一个fin包，客户端进入等待状态；
> 2、服务端收到fin包后，返回一个ask给客户端，服务端进入准备关闭状态；
> 3、服务端发送客户端一个fin包，服务端进入最后确认关闭状态；
> 4、客户端收到fin包后，返回一个ask。服务端收到ask后就关闭，客户端延迟一段时间后2msl后，关闭

通信加密
对称加密：使用同一个秘钥，发送方使用秘钥加密，接受方使用秘钥加密。DES、AES
非对称加密：有公钥私钥的说法，其中公钥是可以公开的。发送方使用公钥加密发送，只有接受方用私钥解密才能得到真实
数据信息。  RSA

协议缓存：



##### 动画
帧动画、创建一个xml文件，把图片放上去即可。
补间动画、4种：放缩、透明度、旋转、位移。每种动画都有一个专门的类对应
属性动画、与补间动画的区别就是补间动画没有改变view的位置，而属性动画真实改变了。

##### 持久化存储
1、文件(流)保存，read、write
2、SharedPreferences。commit(同步)/apply(异步) -> 微信开源框架-MMKV
3、数据库SQLite、GreenDao
4、ContentProvider存储数据
5、服务器(网络)获取

##### 内存泄露
内存泄露的原因：
当需要被回收变量的内存还被其他变量引用持有，导致内存回收失败的现象称之为内存泄露。内存泄露的本质就是长生命周期的对象持有
了短生命周期对象的引用。导致短生命周期对象结束时无法被回收。
具体原因：
1、单例模式导致的内存泄漏。因为单例的生命周期和应用的生命周期是一致的，如果往单例模式里面传了一个生命周期比较短的对象，比
如Activity。
2、静态变量导致的内存泄漏。
3、Handler内存泄漏。一般指匿名内部类的Handler和具名Handler的handleMessage方法里面持有外部引用的时候才会导致内存
泄漏。
4、匿名内部类也会导致内存泄漏。匿名内部类会持有外部类的引用，如果在匿名内部类里面做了耗时操作而在合适的时间关闭，就会导
致Activity无法退出。
5、资源释放内存泄露。比如文件、数据库、流的打开和关闭，

内存泄露优化
[](third_frame/LeakCanary.md)
工作原理
> WeakReference弱引用机制。每当GC时,弱引用持有的对象在某一时间点被确定可达性是弱可达的,那么它引用的对象就会被回收，
> 这个WeakReference会被加入到对应的ReferenceQueue。


##### 包体积优化
1、开启混淆，资源压缩。ProGuard
2、无用资源剔除。
3、图片处理。压缩。
4、移除无用第三方库。
5、so库优化。只保留一种类型的so库。armeabi
6、resources.arsc资源混淆。其实就是将资源路径变短，比如res/drawable/login变为r/d/l。开源工具AndResGuard。
7、so动态加载。按需加载，插件化思想。开源框架SoLoader。
8、插件化。也是按需加载。
8、dex文件分包优化。开源框架ReDex
<https://mp.weixin.qq.com/s/_gnT2kjqpfMFs0kqAg4Qig>

##### mvp、mvvm
MVVM的本质是数据驱动编程。view层、model层完全隔离，view层与viewModel层也是完全隔离，通过通知、databind实现数据驱
动UI。
MVP其实就是MVC的一种升级。本质上它并没有解决代码耦合的问题，P层必须持有V层实例，才能刷新UI。只不过是从前UI逻辑在视图控
制器中写，现在搬到了Presenter中写而已。

##### 组件化 
组件化架构的目的就是让每个业务模块变得相对独立，各个组件在组件模式下可以独立开发调试，集成模式下又可以集成到“app壳工程”中，
从而得到一个具有完整功能的APP。
解决问题：
业务组件，如何实现单独运行调试 动态配置清单文件，启动入口，依赖
业务组件间 没有依赖，如何实现页面的跳转 ARouter
业务组件间 没有依赖，如何实现组件间通信/方法调用
业务组件间 没有依赖，如何获取fragment实例
业务组件不能反向依赖壳工程，如何获取Application实例、如何获取Application onCreate()回调(用于任务初始化)
<https://blog.csdn.net/dongrimaomaoyu/article/details/123204769>

##### 模块化
就是将业务拆分成多个模块放在不同的Module里面，每个功能的代码都在自己所属的module中添加。
解决问题：
如何拆分项目
模块之间的通信 arouter
模块内的代码隔离
模块在调试与发布模式之间的切换

##### AOP
使用AspectJ
> 使用沪江的开源库，快速配置项目；
实现：
> 声明注解，标注切点；
> 编写切面，(@Pointcut、表示切入点，；@Around编写切入点执行逻辑。)
> 在需要使用位置使用注解即可。

##### Netty
> 基于socket，把服务器的ip，端口传入，与服务器开启一个channel管道实现连接，发送数据时往管道write数据即可；接收数据就是
它有提供一个专门处理消息的handler类，这这个类能接受到服务端传来的数据。


##### android底层架构
架构分为五层：从上到下依次是应用层、应用架构层(AMS活动管理器,PMS 包管理器,WMS 窗口管理器,)、系统运行库层(C/C++程序库和Android 运行时库)、
硬件抽象层和Linux内核层













线程池使用
Synchronized同步原理
设计模式
view的绘制原理
handler机制
事件分发机制
性能优化经验
netty
瘦身
AOP

activity生命周期、启动模式
广播
fragment懒加载 [setUserVisibleHint() + onHiddenChanged()]
说说流行框架()
动画有用过吗
eventBus粘性事件原理(保存，判断粘性事件取出同样的分发出去)
recyclerview复用
最难的问题，如何解决


