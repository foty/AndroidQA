
### 1、基础知识

##### 说说  ==、equals 和 hashcode
简单说说，无非是什么，有什么用，有什么区别。参考回答：
> 它们都是用来判断对象是否相等。
> 不同点：`双等号(==)`对于基本数据类型，只要它们的值相同，结果就是true，否则false。对于非基本数据类型对象，是判断
> 是否为同一个内存地址，同一个引用；`equals`是Object中的方法，并且Object中equals默认使用==来判断，判断内容是否相
> 同，也就是值比较，==是引用比较；  
> equals与hashcode也是比较2个对象内容是否相等，并且都是Object中的方法。如果2个对象相等，hashcode一定相等，equals
> 也是返回true；如果只有equals相等，2个对象也是相等，它们的hashCode()也一定相等；如果只有hashcode相等，2个对象不
> 一定相等，equal也不一定相等。equal对应是准确性，hashcode对应是效率。

##### 为什么有了equals(hashcode)还要hashcode(equals)。
> 因为重写equal比较全面、复杂，效率就变低，而利用hashCode进行对比，只要生成一个hash值进行比较就可以，效率比较高。 
> 
> 因为hashCode并不完全可靠，有时候不同的对象他们生成的hashcode也会一样。(java中的hash函数返回的是int类型，对于
> hashcode的计算也是有限的，数据量大就有可能出现相同的hash值)

##### equals 和 hashcode如何协调比较。
> 当需要对比的时候，首先用hashCode去对比，如果hashCode不一样，则表示这两个对象肯定不相等，就不必再用equal去比较；
> 如果hashCode()相同，再对比他们的equal()。如果equal()也相同，则表示这两个对象是相同的。这样既能提高效率也能
> 保证准确性。

##### equals、hashcode为什么必须重写,只重写一个会怎样。
只重写一个无法保证准确性，无法满足所有需求，还会引发其他错误。
> 只重写equal，那么2个对象比较的就是hashcode，这个值是由内存地址转化而来，不同对象一定不同。这会导致创建的2个对象所
> 有属性都相同，但是因为hashcode没有重写，程序识别为2个不同的对象。即是equals相等了，hashcode也不等，违反hashcode
> 规则。  
> 只重写hashcode，那么比较的其实是对象的地址值(equals默认==实现)，不同对象也一定不同，甚至可以说2个对象永远不会相
> 等；

##### HashMap和HashSet为什么必须同时重写hashcode和equals。
> 一是hashcode和equals只重写其中一个都无法保证HashMap与HashSet内元素的准确性，就是无法判断2个对象是否相等；   
> 二会导致hash容器无法正常工作。(hash表是以key-value保存数据，key就是根据对象hashcode计算而来：
> 计算key下标的方式：(n - 1) & hash，n是数组长度。只重写equals会导致hashcode出现相同，导致无法保证key的唯一性)；
> 只重写hashcode会导致2个不同对象会被挂载到同一个key上，后挂载的会替换前挂载的，导致使用key获取的不是同一个对象。)

##### HashCode的作用
> 对象区别于其他对象的标识,hashcode是当前对象的地址值计算转化来的。

##### Switch能否用string做参数
> Java 1.7之前只能支持byte、short、int、char或其封装类及enum类型，1.7及以上才支持string，boolean类型也是不支持

##### java基本数据类型有哪些，int， long占几个字节
##### Java面向对象的三个特征与含义。
##### Java为什么能做到多态
##### java有什么特性，继承有什么用处，多态有什么用处。
##### Override和Overload的含义去区别。
##### new String("xx")创建了几个对象
##### String 是最基本的数据类型吗？
##### 是否可以继承 String 类？
##### int 和 Integer 区别？
##### String、StringBuffer与StringBuilder的区别。
##### &和&&的区别？
##### 值传递与引用传递？（Java 没有引用传递，只有值传递）
##### char 型变量中能不能存贮一个中文汉字
##### try catch finally，try里有return，finally还执行么
##### 静态类与费静态类的区别。
##### 静态方法，静态对象为什么不能继承，编译会报错么
##### Java中静态方法，能不能被子类重写？编译会报错么？
##### 在一个静态方法内调用一个非静态成员为什么是非法的？
##### foreach与正常for循环效率对比。
##### 装箱和拆箱的原理？
##### 匿名内部类能不能访问外部类的私有方法？
##### 匿名内部类编译后也是独立的外部类，它为何能访问外部类的私有方法？如果能访问，是不是破坏了java的语义？怎么做到的？


### 2、数据结构、集合容器

##### ArrayList、LinkedList、Vector的区别
##### Map、Set、List、Queue、Stack的特点与用法。
##### ArrayList 扩容机制？
##### TreeMap、HashMap、LinkedHashMap的区别。
##### 链表的查找的时间复杂度是多少
##### 讲讲LinkedHashMap的数据结构
##### HashMap中hash函数怎么实现的，还有哪些hash函数的实现方式
##### MHashMap和HashTable的区别。
##### HashMap和ConcurrentHashMap的区别，HashMap的底层。
##### 说说HashMap的原理
##### hashmap实现，扩容是怎么做的，怎么处理hash冲突，hashcode算法等
##### HashMap查找的时间复杂度是多少？
##### 为什么扩容是2的次幂
##### Hashmap如何解决散列碰撞
##### Hashmap底层为什么是线程不安全的
##### HashMap中get()、put()如何实现的
##### 解决hash冲突的时候，为什么用红黑树
##### 红黑树的效率高，为什么一开始不用红黑树存储
##### 为什么阀值是8才转为红黑树
##### 为什么退化为链表的阈值是6
##### 二分搜索树的特性和原理
##### 堆的实现，最大堆，最小堆，优先队列原理
##### Comparable 和 Comparator 的区别
##### LRU实现
##### ArrayMap 和 SparseArray
##### HashMap的数据结构,如何保证快速查找,容量为何要设计为2的n次方这样？对扩容有没有影响？
##### ConcurrentHashMap
##### HashTable，为何废弃


### 3、泛型

##### 泛型常用特点，List<String>能否转为List<Object>。
##### 泛型是怎么解析的，比如在retrofit中的泛型是怎么解析的
##### 泛型有什么优点
##### 泛型为什么要擦除？kotlin的泛型了解吗？泛型的pecs原则
##### 为何会有协变和逆变
##### 通配符


### 4、线程、多线程、线程池、同步

##### 实现多线程的两种方法。
> Thread与Runnable。

##### 线程间同步的方法
##### 多线程存在哪些问题
##### 线程池核心参数
##### 并发和并行区别
##### 线程的几种状态
##### 创建线程的几种方式
##### ThreadLocal的设计理念与作用。
##### ThreadPool用法与优势。
##### 谈谈线程死锁，如何有效的避免线程死锁？
##### 如何让两个线程循环交替打印
##### 三个线程依次打印，有哪些方式
##### 如何实现多线程中的同步
##### 怎么中止一个线程，Thread.Interrupt一定有效吗
##### 为什么用线程池
##### 线程池了解多少？拒绝策略有几种,为什么有newSingleThread
##### wait()和sleep()的区别
##### synchronized和Lock的使用、区别,原理；
##### 为什么会有线程安全？如何保证线程安全
##### interrupt()、interrupted()、isInterrupted() 区别
##### start()、run() 区别
##### 何为阻塞队列？
##### volatile，synchronized和volatile的区别？为何不用volatile替代synchronized？
##### 如何配置线程池的？核心线程数你一般是怎么配置的
##### AtomicInteger如何保证原子操作
##### CAS如何保证原子操作


### 5、锁

##### 平常有用到什么锁，synchronized底层原理是什么
##### synchronized是公平锁还是非公平锁,ReteranLock是公平锁吗？是怎么实现的
##### 锁之间的区别，同步锁举例
##### 说说你对volatile字段有什么用途？
##### JMM可见性，原子性，有序性，synchronized可以保证什么
##### 锁的分类，锁的几种状态，CAS原理
##### AQS了解吗？
##### 如何避免死锁
##### 公平锁、非公平锁、可重入锁
##### sleep()与wait()区别,run和start的区别,notify和notifyall区别,锁池,等待池


### 6、加密

##### 有用过什么加密算法？AES,RAS什么原理？
##### 对称加密和非对称加密，说说公钥


### 7、反射

##### 反射的作用与原理。
##### .什么是反射。
##### 反射可以反射final修饰的字段吗
##### 反射是什么，在哪里用到，怎么利用反射创建一个对象。
##### 反射机制的优缺点
##### Class.getField和getDeclaredField的区别，getDeclaredMethod和getMethod的区别


### 8、jvm

##### 说说 JVM 运行时数据区
##### 说说 JVM 内存区域
##### Java 对象如何创建的，对象创建过程
##### 对象加载的过程，属性先加载还是方法先加载
##### PathClassLoader与DexClassLoader有什么区别
##### Jvm的内存结构，Jvm的垃圾回收，方法区有什么东西
##### 拉圾回收的GCRoot是什么
##### 说说Java的内存分区
##### 讲讲你对垃圾回收机制的了解，老年代有什么算法？
##### JVM类加载机制了解吗，类什么时候会被加载？类加载的过程具体生命周期是怎样的？
##### Jvm的内存模型,每个里面都保存的什么
##### 类加载机制的几个阶段加载、验证、准备、解析、初始化、使用、卸载
##### 对象实例化时的顺序
##### 类加载器,双亲委派及其优势
##### 垃圾回收机制
##### 程序计数器为什么设计成私有
##### 虚拟机栈和本地方法栈为什么设计成私有
##### String.intern() 作用
##### 虚拟机如何解决内存分配并发问题
##### 内存分配策略
##### 对象的访问定位有哪几种
##### GC原理，有哪几种GC方式
##### VM的分区，各个分区的作用