
### LiveData

- 原理
- 问题

#### 什么是livedata
> LiveData是一种可观察的数据存储器类。与其他可观察类的特点是：LiveData具有生命周期感知能力。这种感知能力可确保
> LiveData仅更新处于活跃生命周期状态的应用组件观察者。

#### 工作原理

> 使用观察者模式，注册观察者并且绑定，当setValue或者postValue时更新数据，通知更新，实现数据的订阅与分发。

#### 如何感知生命周期

原因：
> 将Observer与LifeCycleOwner组合成新的观察者包装类，实现LifecycleEventObserver接口，并且添加到对应act
> 的Lifecycle中绑定，从而实现生命周期感知。

分析之前了解几个东西。
##### 1、Observer
> 一个观察者接口，只有一个onChanged()方法。通知更新时会携带最新数据结果返回。

##### 2、LifecycleBoundObserver
> LiveData实现生命周期感知的观察者包装类。继承ObserverWrapper，实现LifecycleEventObserver。   
> ObserverWrapper是个抽象类，感知到声明周期变化后，进行相应的操作。比如通知更新或者取消观察。   
> LifecycleEventObserver是个接口，继承LifecycleObserver，属于lifecycle中的一员。真正感知act的生命周期的能力就
> 是它实现的。每当周期状态变化，会通过onStateChanged()回调通知。具体原理就是lifecycle在顶层activity织入了一个
> ReportFragment，通过这个fragment感知act生命周期的变化，回到给LifecycleEventObserver的onStateChanged()。[这
> 个做法很glide绑定生命周期一样]

##### 3、SafeIterableMap
> LiveData内部维护的一个存放Observer对象的map集合。key是Observer，value是ObserverWrapper。


#### 实现bus时，自带的粘性事件导致数据倒灌(先setValue，再observer,还能收到事件)，如何解决
> 可以反射修改mVersion。使mLastVersion等于mVersion。(美团livedataBus方案)

#### 问题部分

* livedata为什么设计成粘性的
> 可设计者认为：所有的观察者，不管是否新注册的观察者，都需要知道LiveData中存储的数据，而且是最新数据。只要LiveData有
> 了最新数据，都需要告知观察者。所以才使用版本号比较`mVersion > mLastVersion`实现。

* livedata设计成粘性的原理
> LiveData有一个版本号对比，mVersion很mLastVersion。当m > l时，就会触发更新，将最新数据分发给观察者。

* LiveData如何实现订阅者模式，如何处理发送事件?
> 通过注册观察者，在数据更新时进行数据变更的通知(setValue()、postValue())