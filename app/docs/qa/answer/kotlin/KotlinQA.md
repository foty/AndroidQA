
##### 什么是协程？
> 1、一个以高效和简单的方式管理并发的框架，线程框架或者并发设计模式。特点就是能够以阻塞(同步)方式写出非阻塞(异步)的代码。       
> 2、核心就是一段程序能够被挂起，并在稍后再在挂起的位置恢复。其实也是“接口回调”的特殊形式。挂起和恢复依赖于内部状态机实
> 现。协程是依赖于线程池API的，在一个线程可以创建多个协程，并且协程运行时并不会阻塞当前线程。所以也有协程是一个轻量级线
> 程框架的说法。     
> 3、使用 launch与async来创建、启动协程。

##### 协程的原理
> 挂起函数。其实也是`接口回调`的特殊形式。挂起和恢复依赖于内部状态机实现。协程是依赖于线程池API的，在一个线程可以创建多个
> 协程，并且协程运行时并不会阻塞当前线程。所以也有协程是一个轻量级线程框架的说法。   

##### Kotlin内置标准函数let、run的原理是什么
> 1、标准let内置函数内部对泛型进行了let函数扩展，意味着所有的类型都等于泛型，所以任何地方都是可以使用let函数的；
> 2、所有类型let其实是一个匿名的Lambda表达式。Lambda表达式的特点是：最后一行会自动被认为是返回值类型。所以在表达式
> 返回Boolean，那么当前的let函数就是Boolean类型。

##### Kotlin语言泛型的形变是什么？
> 形变一共分为三个：不变，协变，逆变。
> 不变：可以是生产者，也可以是消费者，此泛型没有任何泛型继承相关的概。
> 协变：只能是生产者。此泛型有泛型继承相关的概念存在。表示可以接收此泛型类型的子类型，使用`out`标识；
> 逆变：只能是消费者，此泛型有泛型父类转子类的强转相关的概念存在。表示可以接收此泛型类型的父类型，使用`in`标识