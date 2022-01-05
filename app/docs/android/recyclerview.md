#### RecyclerView

* RecyclerView缓存
* 相关问题

##### 一、RecyclerView缓存
1、缓存，存的是什么。  
> 存的是ViewHolder。这块可以对比ListView优化的写法。就是自己写一个ViewHolder类。RecyclerView的ViewHolder其实是同样的道理。

2、四个级别缓存:

* Scrap: 屏幕内的缓存view，可以直接使用，
* Cache: 刚刚移出屏幕的view，也是可以直接使用的，一般有一个固定的容量，默认是2(上下各1个)。
* ViewCacheExtension: 用于自定义缓存的东西
* RecycledViewPool: 存放当Cache容量满了之后，根据规则将Cache先缓存的view移出Cache的view。存放的来自Cache的缓存view。

3、缓存策略：  
首先在Scrap -> Cache -> ViewCacheExtension -> RecycledViewPool -> createViewHolder()

4、展现原理
recyclerview是一个view，先看构造方法。继承ViewGroup，ViewGroup又是继承View，那么必然少不了它的几个重要方法，measure，layout，draw等等。
recyclerview的使用步骤：创建实例，设置LayoutManager，设置adapter，设置数据后适配器notify即可。这里按照这么几个阶段分析：

构造阶段(创建实例)：  
* 设置ScrollContainer(能否滚动)
* 设置焦点
* 读取默认配置信息，包括水平或垂直滚动因子，滑动速度等等
* item动画监听器
* 初始化适配管理器(AdapterHelper)
* 初始化子view帮助类
* 读取xml配置的属性信息，具体有什么属性看能是设置什么属性就知道了。
* 创建LayoutManager

setLayoutManager阶段：
* LayoutManager的校验：比如是否同一个，是否为null等等。
* 绑定RecyclerView，attach到Window(dispatchAttachedToWindow())
* 更新Recycler缓存大小以及requestLayout()。(等会专门看下这个Recycler是什么东西)

测量阶段

摆放阶段

绘制阶段

setAdapter阶段：
* 解冻。解冻之后，recyclerview才能重新layout与滚动
* 


5、缓存原理

##### 二、 相关问题

* RecyclerView的多级缓存机制,每一级缓存具体作用是什么,分别在什么场景下会用到哪些缓存
* RecyclerView的滑动回收复用机制
* RecyclerView的刷新回收复用机制
* RecyclerView 为什么要预布局
* ListView 与 RecyclerView区别
* RecyclerView性能优化