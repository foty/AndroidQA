#### RecyclerView

1、四个级别缓存:

* Scrap: 屏幕内的缓存view，可以直接使用，
* Cache: 刚刚移出屏幕的view，也是可以直接使用的，一般有一个固定的容量，默认是2(上下各1个)。
* ViewCacheExtension: 用于自定义缓存的东西
* RecycledViewPool: 存放当Cache容量满了之后，根据规则将Cache先缓存的view移出Cache的view。存放的来自Cache的缓存view。

2、缓存策略：
首先在Scrap -> Cache -> ViewCacheExtension -> RecycledViewPool -> createViewHolder()


##### 相关问题

* RecyclerView的多级缓存机制,每一级缓存具体作用是什么,分别在什么场景下会用到哪些缓存
* RecyclerView的滑动回收复用机制
* RecyclerView的刷新回收复用机制
* RecyclerView 为什么要预布局
* ListView 与 RecyclerView区别
* RecyclerView性能优化