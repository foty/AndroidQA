### 基本知识(String、equal)

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
> 只重写equal，那么2个对象比较的就是hashcode，这个值是由内存地址转化而来，不同对象一定不同。这导致创建的2个对象所
> 有属性都相同，预期也是，无法满足所有情景；  
> 只重写hashcode，那么比较的其实是对象的地址值(equals默认==实现)，不同对象也一定不同，同样无法满足所有情景；   
> 

##### HashMap和HashSet为什么必须同时重写hashcode和equals。

### 数据结构、集合容器(HashMap、)
### 线程、线程池
### 泛型
### 锁
### 反射
### jvm