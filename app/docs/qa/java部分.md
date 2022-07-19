
### 1、基础知识

##### 说说  ==、equals 和 hashcode
简单说说，无非是什么，有什么用，有什么区别。参考回答：
> 它们都是用来判断对象是否相等。
> 不同点：`双等号(==)`对于基本数据类型，只要它们的值相同，结果就是true，否则false。对于非基本数据类型对象，是判断
> 是否为同一个内存地址，同一个引用；`equals`是Object中的方法，并且Object中equals默认使用==来判断，判断内容是否相
> 同，也就是值比较，==是引用比较；  
> equals与hashcode也是比较2个对象内容是否相等，并且都是Object中的方法。如果2个对象相等，hashcode一定相等，equals
> 也是返回true；如果只有equals相等，2个对象也是相等，它们的hashCode()也一定相等；如果只有hashcode相等，2个对象不
> 一定相等，equal也不一定相等。equal对应是准确性，hashcode对应是效率。(hashcode是当前对象的地址值计算转化来的)

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
问题有点模糊，其实是需要分情况讨论的。
> 只重写equal，那么2个对象比较的就是hashcode，这个值是由内存地址转化而来，不同对象一定不同。这会导致创建的2个对象所
> 有属性都相同，但是因为hashcode没有重写，程序识别为2个不同的对象，这可能会与预期结果不同。  
> 只重写hashcode，那么比较的其实是对象的地址值(equals默认==实现)，不同对象也一定不同，同样无法满足所有情景；   
>

##### HashMap和HashSet为什么必须同时重写hashcode和equals。
##### HashCode的作用
##### Switch能否用string做参数
> Java 1.7之前只能支持byte、short、int、char或其封装类及enum类型，1.7及以上才支持string，boolean类型也是不支持

##### java基本数据类型有哪些，int， long占几个字节
##### Java面向对象的三个特征与含义。
##### java有什么特性，继承有什么用处，多态有什么用处。
##### new String创建了几个对象
##### String、StringBuffer与StringBuilder的区别。
##### Comparable 和 Comparator 的区别
##### try catch finally，try里有return，finally还执行么
##### 静态类与费静态类的区别。
##### 静态方法，静态对象为什么不能继承。


### 2、数据结构、集合容器

##### Hashmap如何解决散列碰撞
##### Hashmap底层为什么是线程不安全的
##### ArrayList、LinkedList、Vector的区别
##### Map、Set、List、Queue、Stack的特点与用法。
##### MHashMap和HashTable的区别。
##### HashMap和ConcurrentHashMap的区别，HashMap的底层。
##### TreeMap、HashMap、LinkedHashMap的区别。
##### 讲讲LinkedHashMap的数据结构
##### 说说HashMap的原理
##### HashMap查找的时间复杂度是多少？


### 3、泛型

##### 泛型常用特点，List<String>能否转为List<Object>。
##### 泛型是怎么解析的，比如在retrofit中的泛型是怎么解析的
##### 泛型有什么优点
##### 泛型为什么要擦除？kotlin的泛型了解吗？泛型的pecs原则


### 4、锁

##### 平常有用到什么锁，synchronized底层原理是什么
##### synchronized是公平锁还是非公平锁,ReteranLock是公平锁吗？是怎么实现的
##### 锁之间的区别
##### 说说你对volatile字段有什么用途？
##### JMM可见性，原子性，有序性，synchronized可以保证什么
##### AQS了解吗？


### 5、线程、线程池

##### 线程间同步的方法
##### 如何让两个线程循环交替打印
##### 线程池了解多少？拒绝策略有几种,为什么有newSingleThread


### 6、加密

##### 有用过什么加密算法？AES,RAS什么原理？


### 7、反射

##### 反射的作用与原理。
##### 反射可以反射final修饰的字段吗
##### 反射是什么，在哪里用到，怎么利用反射创建一个对象。


### 8、jvm

##### 对象加载的过程，属性先加载还是方法先加载
##### PathClassLoader与DexClassLoader有什么区别
##### Jvm的内存结构，Jvm的垃圾回收，方法区有什么东西
##### 拉圾回收的GCRoot是什么
##### 说说Java的内存分区
##### 讲讲你对垃圾回收机制的了解，老年代有什么算法？
##### JVM类加载机制了解吗，类什么时候会被加载？类加载的过程具体生命周期是怎样的？