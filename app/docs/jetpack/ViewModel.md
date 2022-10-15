### ViewModel

概述
原理
问题

#### 概述
ViewModel的主要特性是以注重生命周期的方式存储和管理数据。作用是可以防止内存泄露。在组件的生命周期中ViewModel的数据
会一直保存在内存中，即便是在系统配置变更时也会存在。

#### 原理
其实没啥原理，就是一个数据保存组件。最大特点是能以注重生命周期的方式存储和管理数据。因为ViewModel的数据一直保存在内存
中。

#### 问题

##### ViewModel保存在哪里
ViewModelStore。里面有一个HashMap用来保存各种ViewModel。在顶层Activity ComponentActivity中会初始化。

#### ViewModel的生命周期如何与组件生命周期绑定
答案是结合Lifecycle

#####  ViewModel数据如何传递到页面(UI)
答案是通过事件总线工具。如livedata
