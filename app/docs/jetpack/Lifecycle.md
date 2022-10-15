### Lifecycle

概述
原理
问题

#### 概述
Lifecycle是一个能够感知其他组件，比如Activity、fragment，生命周期的组件。能够让其他组件监听宿主生命周期状态。但是官
方目前并没有独立成库供其他框架使用，只是存在官方的ViewModel、 LiveData框架中。

#### 原理

##### 1、它的组成
- Lifecycle抽象类：声明了代表宿主上命周期状态的枚举类State和对应State的事件枚举类Event。主要作用就是保存宿主生命状态
和返回对应的事件状态。
- LifecycleRegistry：Lifecycle抽象类的唯一实现类。负责注册Observer，分发宿主状态事件。
- LifecycleObserver接口：有3个子接口继承，都是用来处理宿主生命周期变化后通知监听者的回调。
- LifecycleOwner接口：作用是为了让实现该接口的类是一个能够提供生命周期事件的宿主。

##### 2、工作原理

一般来说(fragment)
> 通过实现LifecycleOwner接口声明宿主，创建LifecycleRegistry实例，通过LifecycleRegistry实例添加观察者，并且在宿主
> 生命周期活动方法中分发不同生命周期事件。观察者就可以通过相应的生命周期事件回调处理不同的事情。(Fragment)

对于Activity
> 通过织入一个叫`ReportFragment`的fragment并且监听它的生命周期实现。这么做是为了兼容自己的activity不是继承
> ComponentActivity类的场景。

##### 3、观察者如何监听宿主(收到宿主状态改变事件)
创建监听对象，也就是LifecycleObserver，通过LifecycleRegistry实例添加到宿主的观察者集合中。从LifecycleObserver的
回调中可以收到宿主的状态改变事件。

