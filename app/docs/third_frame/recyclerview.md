#### RecyclerView

* RecyclerView展现流程
* RecyclerView缓存
* 相关问题

##### 一、RecyclerView绘制
<https://blog.csdn.net/Panxuqing/article/details/104308834>
<https://blog.csdn.net/m0_37796683/article/details/104864318>

recyclerview是一个view，先看构造方法。继承ViewGroup，ViewGroup又是继承View，那么必然少不了它的几个重要方法，measure，layout，draw等等。
recyclerview的使用步骤：创建实例，设置LayoutManager，设置adapter，设置数据后适配器notify即可。这里按照这么几个阶段分析：

1、构造阶段(创建实例)：  
* 设置ScrollContainer(true),控制能否滚动
* 设置焦点
* 读取默认配置信息，包括水平或垂直滚动因子，滑动速度等等
* item动画监听器
* 初始化AdapterHelper，它是一个回调监听器，它有个比较重要的方法是根据position找到相应的ViewHolder。
* 初始化ChildHelper，同样也是回调监听器，它主要负责子view的增(添加子view到Recyclerview中)、删、查等操作。
* 读取xml配置的属性信息。
* 创建LayoutManager(createLayoutManager(...))、此方法需要在xml中指定LayoutManager才会执行，通过反射形式创建实例


2、setLayoutManager阶段：
* LayoutManager的校验：比如是否同一个LayoutManager，是否为null等等。
* 绑定RecyclerView，attach到Window(dispatchAttachedToWindow())
* 更新Recycler能够回收利用的数量(缓存大小) 
* requestLayout()。(这里requestLayout()并不会触发3大绘制流程)


3、setAdapter阶段：
* 解冻。解冻之后，recyclerview才能重新layout与滚动 
* 更新适配器,设置数据监听器(RecyclerViewDataObserver)。
* LayoutManager回收废弃的view，清空Recycler以保证正确的回调
* requestLayout()
> setAdapter()之后再次requestLayout()，这次会触发三大绘制流程，开始绘制View。


4、测量阶段 onMeasure()
* 如果 LayoutManager == null，走默认测量defaultOnMeasure(...)方法
* 其他基本都是交给Recyclerview.LayoutManager来测量。(mLayout.onMeasure(...)),实际上就是调用默认defaultOnMeasure()方法。 
* 如果recyclerview的宽高都是确定的或者adapter为空，可以直接return。只有rv的宽高不确定才需要测量item(子view)来确定rv的宽高。
* 如果 mState.mLayoutStep不是等于STRAT 将会执行到dispatchLayoutStep1()(默认情况下会执行step1)
* dispatchLayoutStep2()
* 如果height与width的mode不是等于EXACTLY并且有子View的width/height= 0，那么将会再次执行dispatchLayoutStep2()。
* 测量之后可以从子View获取到它的宽高，初步确定所有子view的一个大小情况,并将一个最小到最大的情况保存在一个Rect中(mTempRect)


关键方法1：dispatchLayoutStep1():
- 处理adapter更新
- 找到没有被移除的item保存起来(遍历子view创建ViewHolder并保存到`SimpleArrayMap<RecyclerView.ViewHolder, InfoRecord>`) 
- 进行预布局(第一次`mLayout.onLayoutChildren()`) 
- 处理动画，根据第一次保存信息与预布局对比结果，添加add动画或者remove动画
- 更新mState状态

关键方法2：dispatchLayoutStep2():
- layout子view，具体交给LayoutManager`mLayout.onLayoutChildren(mRecycler, mState)`，关键类在LayoutManager，关键方法是`fill()`,
  `layoutChunk()`,`addView()`最终还是调用ViewGroup的`addView()`。添加完成后调用`measureChildWithMargins()`测量子view(包括margin值)
  得到子view的一个位置坐标，通过坐标 将子view layout(`layoutDecoratedWithMargins()`)
- 更新mState状态(State.STEP_ANIMATIONS)

关键方法3：LinearLayoutManager#onLayoutChildren()：
- 获取锚点位置坐标，确定item布局的起始位置。
- 根据方向确定从底部开始或者从顶部开始布局
- 填充item(fill())
- 修复屏幕空白

关键方法4：fill()：
- 如果有需要，回收开始滚动后滑出屏幕的那些itemView
- 循环调用`layoutChunk()`,填充item。直到屏幕无法容下更多item或者没有更多的item
- 返回当次填充item消耗的空间

关键方法5：layoutChunk()：
- 获取到itemView，最后添加到recyclerview中。获取方法是`layoutState.next(recycler)`，item的缓存复用机制也是由这里开始
- 测量ItemView以及它的子view边距情况(measureChildWithMargins())
- 根据上面测量的边距，重新摆放itemView


5、摆放阶段 onLayout()
* 继承至View的onLayout()方法核心只做了一件事：dispatchLayout()。dispatchLayout()方法的核心是dispatchLayoutStep3()。但在这之前如果
  mState.mLayoutStep的值还是STEP_START，会再次执行dispatchLayoutStep1()，dispatchLayoutStep2()。如果adapter发生更新或者recyclerView的大小
  改变，也会重新执行dispatchLayoutStep2()。最后执行dispatchLayoutStep3()。
  

关键方法3：dispatchLayoutStep3(): (保存有关动画视图的信息，触发动画并进行任何必要的清理)
- 更新mState状态(State.STEP_START)
- 将遍历子view保存view的动画信息(保存到ViewInfoStore)，与step1类似。 
- 处理动画回调(执行动画)。
- 完成Layout阶段，重置状态设置


绘制阶段 onDraw()
- recyclerview的onDraw是绘制ItemDecoration，item则是交给ViewGroup(super.onDraw(c))完成。


>概述：
> RecyclerView  一个大容器(ViewGroup)，负责onDraw。
> LayoutManager 负责测量，添加，摆放itemView
> Recycler 负责holder的回收复用


##### 二、RecyclerView缓存
1、缓存，存的是什么。
> 存的是ViewHolder。这块可以对比ListView优化的写法。就是自己写一个ViewHolder类。RecyclerView的ViewHolder其实是同样的道理。

2、四个级别缓存:
* Scrap: 屏幕内的临时缓存的holder，保存进行布局时分离出来的，未移除的，不参与滚动回收的viewHolder，可以直接使用，分2类：AttachedScrap与ChangedScrap。
  ChangedScrap与AttachedScrap的区别是holder是否发生了改变。
* Cache: 刚刚移出屏幕的view，也是可以直接使用的，一般有一个固定的容量，默认是2(上下各1个)。
* ViewCacheExtension: 用于自定义缓存的东西
* RecycledViewPool: 存放当Cache容量满了之后，根据规则将Cache先缓存的view移出Cache的view。存放的来自Cache的缓存view。

3、缓存策略：  
首先在Scrap -> Cache -> ViewCacheExtension -> RecycledViewPool -> createViewHolder()

4、缓存机制：
<https://blog.csdn.net/m0_37796683/article/details/105141373>
<https://blog.csdn.net/zhying719/article/details/114826527>布局流程
> 从LayoutManager#layoutChunk()的layoutState.next()开始，RecyclerView.Recycler隆重登场。

关键方法next()：
- 首先判断缓存列表是否存在，有的话从缓存获取`nextViewFromScrapList()`
- 否则根据position从Recycler中获取。`getViewForPosition()`

> getViewForPosition()最终回来到tryGetViewHolderForPositionByDeadline()，这里才是复用机制的真正体现。

关键方法：tryGetViewHolderForPositionByDeadline()
- position合法性校验
- 如果是预布局的，通过position和id俩种方式从ChangedScrap中获取指定holder。(`getChangedScrapViewForPosition()`)
- 从AttachedScrap中精准匹配寻找指定holder；或者从ChildHelper寻找具有与位置匹配的有效的、隐藏的view。通过view获取holder，然后取消隐
  藏，分离出这个holder，添加到scrap区域，最后返回这个holder；再者从Cache中获取holder。
- 判断adapter.hasStableIds()，如果true，则通过id再次从AttachedScrap与Cache中获取holder()。
- 从ViewCacheExtension中获取holder。这是预留给开发者的缓存，开发者可以通过这个实现自己的缓存逻辑。使用很少。
- 从RecycledViewPool中获取holder。pool默认最大的容量是5。
- 如果还是没有找到指定的holder，主动创建一个holder，`adapter#createViewHolder()`。
- 绑定ViewHolder` mAdapter.bindViewHolder()`


##### 三、 相关问题
* RecyclerView的多级缓存机制,每一级缓存具体作用是什么,分别在什么场景下会用到哪些缓存
* RecyclerView的滑动回收复用机制

* RecyclerView 为什么要预布局
> 为添加，移除item做动画铺垫。

* ListView 与 RecyclerView区别
* RecyclerView性能优化