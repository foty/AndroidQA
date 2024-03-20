### View

* 加载(Window,DecorView...)
* 绘制流程(原理)
* 测量模式
* LayoutParams
* 自定义控件
* 屏幕刷新机制(双缓存概念)
* VSync
* Choreographer


* 常见问题 [](../../aQA/answer/android/View绘制流程QA.md)

#### 加载与绘制流程
[](View绘制源码分析.md)  

简述版本：  
> view的绘制有三个步骤：测量(measure)，布局(layout)和绘制(draw), 从DecorView自上而下遍历整个View树。
> 1、测量阶段： 通过调用View的measure()方法获取MeasureSpec(测量规格)信息，经过多次测量，对比最后调用
> setMeasuredDimension()来设置自身大小。子view还可以重写View#onMeasure()，自己设置大小。   
> 2、布局阶段： layout()方法中会先调用setFrame()给自身的left，top，right，bottom属性赋值,于是自己在父View中的位置就
> 确定。然后会调用onLayout()方法，让子View(一般指ViewGroup)自己实现。    
> 3、绘制阶段： 一般分4步：1、绘制背景；2、绘制视图内容,回调onDraw()；3、绘制子View，回调dispatchDraw()，一般
> ViewGroup用的比较多；4、绘制装饰，比如滚动条之类的。


##### [绘制流程]View绘制核心方法performTraversals内各大流程方法调用顺序示意图
private void performTraversals() { 
 // 准备阶段
 
 // 测量阶段(measureHierarchy()->performMeasure()->mView.measure())
 
 // layout阶段()(performLayout() ->onLayout())
 
 // draw阶段(performDraw()->draw()->drawSoftware()->mView.draw())
}

一个Activity的View的绘制开始于setContentView()(PhoneWindow，DecorView在这之前已经创建完成)。DecorView将setContent的View添加成为自己的
一个子View。在这个阶段前，ViewRootImpl产生，DecorView将添加view这件事交给ViewRootImpl完成。接着便出现ViewRootImpl#requestLayout()、
ViewRootImpl#performTraversals()一系列方法的调用，从而产生三大绘制流程的出现。绘制过程从DecorView自身开始，递归到最后一个子view进行测量、布
局、绘制(DecorView是一个 FrameLayout)，从上到下， 从ViewGroup到view(顶层view一定是ViewGroup)，完成一系列的绘制。

#### 测量模式

测量模式是 MeasureSpec中的一部分。MeasureSpec是一个类，表示的是一个32位的整形值，它的前2位表示测量模式SpecMode，后30位表示某种测量模式下的规格
大小SpecSize。通常用来说明应该如何测量这个View。  
测量模式有三种:
* MeasureSpec.UNSPECIFIED  没有任何约束，可以是想要的任何大小(使用较少)
* MeasureSpec.EXACTLY      有一个精确的尺寸大小,比如100dp或者设置为match_parent时生效。
* MeasureSpec.AT_MOST      可以增大到任意大小(尽可能的大),当设置wrap_content时生效。

测量模式在View中与ViewGroup中又是有点区别的。在View中相对比较简单，通过测量规格根据`MeasureSpec.getMode()`API求出mode属于哪种就是哪种。相对ViewGroup
就要复杂一点点，子view的测量模式会根据具体情况进行相关转换。具体转换关系如下：
<table>
	<tr>
	    <th>模式</th>
	    <th>子View的宽高情况</th>
	    <th>转换结果</th>  
	</tr >
	<tr >
	    <td rowspan="3">EXACTLY</td>
	    <td>精确值</td>
	    <td>EXACTLY</td>
	</tr>
	<tr>
	    <td>wrap_content</td>
	    <td>AT_MOST</td>
	</tr>
	<tr>
	    <td>match_parent</td>
	    <td>EXACTLY</td>
	</tr>
	<tr>
	    <td rowspan="3">AT_MOST</td>
	    <td>精确值</td>
	    <td>EXACTLY</td>
	</tr>
	<tr>
	    <td>wrap_content</td>
	    <td>AT_MOST</td>
	</tr>
	<tr>
	    <td>match_parent</td>
	    <td>AT_MOST</td>
	</tr>
	<tr>
	    <td rowspan="3">UNSPECIFIED</td>
	    <td>精确值</td>
	    <td>EXACTLY</td>
	</tr>
	<tr>
	    <td>wrap_content</td>
	    <td>UNSPECIFIED</td>
	</tr>
	<tr>
	    <td>match_parent</td>
	    <td>UNSPECIFIED</td>
	</tr>
</table>

由上表可总结一下结论：
* 当子view有具体的宽高值(比如100px)，测量模式最终都会转换成 EXACTLY。
* 当为 EXACTLY + match_parent，测量模式不变，依然是 EXACTLY。
* 当为 EXACTLY + wrap_content，测量模式最终转换成 AT_MOST。
* 当为 AT_MOST时，除了有具体值，否则都是 AT_MOST。
* 当为 UNSPECIFIED时，除了有具体值，否则都是 UNSPECIFIED。

#### LayoutParams
ViewGroup的布局参数基本类，有多个重载方法：
* LayoutParams(Context c, AttributeSet attrs)：从提供的属性集的参数中提取值和上下文并映射XML属性到这组布局参数
* LayoutParams(int width, int height)： 使用参数创建一组宽高值。
* LayoutParams(LayoutParams source)： 会克隆源的宽度和高度值。
* LayoutParams()

#### View的保存与恢复
就是View中的2个API,还包括一个保存的对象SavedState，实现了Parcelable接口。一般将保存内容存放到这个对象里面。
* Parcelable onSaveInstanceState()
* void onRestoreInstanceState(Parcelable state)

#### 自定义控件掌握
会自定义view控件。提几个优化细节，或者比较偏的知识。  
1.都属于自定义属性方面的。获取自定义属性的方法为Context.obtainStyledAttributes(...)，有4个参数，但是一般都写2个，后面俩个都使用默认，那么对
于obtainStyledAttributes()方法:
* @AttrRes int defStyleAttr：第3个参数默认是构造方法中的参数defStyleAttr，指定一组attrs，配合styles，也可以达到第4个参数的作用。
* @StyleRes int defStyleRes：第4个参数，默认是0。用于指定一组styles，结果和配置自定义属性相同，但是不用在xml文件设置任何属性都能读取配置的内容。
* defStyleAttr与defStyleRes的优先级：只有当defStyleAttr=0时或没有相关属性时才会去defStyleRes寻找。

2.构造方法的使用    
对于自定义View的初始化方法通常会有2中形式：1、在参数最多的构造中调用，其他构造就使用`this.`形式调用对应参数不同的构造方法，这样只写一次初始化方法即可。
2、在每个构造方法都调用一次初始化方法。对于这2中形式的建议：
* 如果获取自定义属性时不会用到第3，第4个参数时，推荐第一种。其实这种情况下2种方式都是一样的，但是第一种只写一遍初始化方法，相对更好些。
* 如果继承系统已有的View，如TextView、Button、ProgressBar等等，建议使用第二种。因为如果是用第一种写法会将这些View的一些style给覆盖调，最终就可能导
致和系统内的button，progressBar存在差距。但这也是审美上的问题，具体看个人公司要求。

3.获取自定义属性的优化：
通常是这么写的：
```
TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.xxx,defStyleAttr, 0);
a.getColor(R.styleable.xxx, defaultxx);
....
a.recycle();
```
优化后：
```
TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.xxx,defStyleAttr, 0);
int n = c.getIndexCount();
for (int i = 0; i < n; i++) {
    int attr = c.getIndex(i);
    switch (attr){
        case xxx:
            break;
        .....    
    }
}
a.recycle();
```
这么写的好处是：当xml并没有设置一定的属性时，不会调用get###()方法，相对应的是接收这个值的对象不会被改变，换个说法就是，接受这个属性值的变量不会因为这个属性
没有被设置而被修改成使用它的默认值。当然如果这个变量默认值和该属性默认值相同就另外一回事了。

#### View屏幕的刷新原理
<https://www.jianshu.com/p/6c8045a9c015>

##### 显示系统的基础知识：
>在一个典型的显示系统中，一般包括CPU、GPU、Display三个部分， CPU负责计算帧数据，把计算好的数据交给GPU，GPU会对图形数据进行渲染，
渲染好后放到buffer(图像缓冲区)里存起来，然后Display(屏幕或显示器)负责把buffer里的数据呈现到屏幕上。简单说就是cpu计算好交给gpu，
gpu渲染好放到缓冲区，显示器从缓冲区取到数据显示在屏幕上。

##### 几个核心名词概念  
根据cpu、gup在屏幕刷新中作用产生几个特有名词：
* 屏幕刷新率：指一秒内屏幕刷新的次数。或者显示了几帧图像。单位hz。刷新率是固定不变的，取决于固件参数。
* 帧率：GPU一秒绘制的帧数，单位fps。Android系统采用60fps的帧率。

##### 屏幕刷新原理
前面提到了，cpu负责计算数据，gpu负责渲染，显示器负责显示。显示器将数据显示不是一次性完成的，它是按从左到右，从上到下的顺序显示在屏幕上。比如
60Hz的屏幕完成一次刷新花费时间大概是16.6ms(1000/60)。

##### 使用双缓冲原因   
正常情况下，cpu/gpu完成计算渲染放在缓冲区，显示器取出显示，这一过程是连续的，重复的。在屏幕绘制时，对于一个缓冲区一边修改一边取出就很容易出现
问题(并发修改)。就会导致屏幕上显示画面的数据来自不同帧，画面撕裂。  
解决：使用双缓存机制。

##### 什么是双缓存。  
双缓存就是让计算渲染的cpu/gpu跟抓取数据的显示器使用2个不同的缓存空间。cpu/gpu完成数据放到后缓冲区，当需要显示时，与显示器抓取数据的前缓冲区交换数据。
从而避免一个画面的数据来源不同帧。注意，当屏幕刷新时，cpu/gpu才开始计算渲染，将数据放到后缓冲区。此时 前缓冲区不会发生变化，当后缓冲区数据完成，前后缓
冲区才开始交换数据。双缓冲其实使用一种用于解决画面撕裂的处理机制。


#### VSync   
现在知道屏幕刷新使用了2个缓冲区。那何时是最佳的交换数据时机呢?这时候VSync就上场了。VSync的全称是Vertical Synchronization,即垂直同步。通俗说VSync
是一个时间中断的信号。系统每次拿到VSync时就开始准备刷新屏幕，保证双缓冲在最佳时间点进行交换，避免出现画面撕裂。与VSync相关的还有2个：Triple Buffer
和Choreographer。他们是在android4.1的黄油项目"Project Butter"一同出现的概念。黄油项目对Android Display系统进行了重构。


##### Choreographer机制原理  
Choreographer是用于配合系统的VSync中断信号的一种机制，起到调度的作用。Choreographer接收系统的VSync信号，保证系统收到VSync信号才开始绘制，让每次绘
制拥有完整的16.6ms。业界一般用它监控应用的帧率。

机制(为什么它能做到统一调度这些工作)：    
https://www.jianshu.com/p/c2d93861095a

* 初始化：单例模式(类似Handler，保存在ThreadLocal、有Looper)
* FrameDisplayEventReceiver:  V-Sync信号接收器
* FrameHandler: V-Sync信号接收器收到VSync信号后发送消息执行doFrame()方法。
* CallbackQueue[]数组: postCallBack()方法中的runnable就被放置到这个数组中。

Choreographer在ViewRootImpl的构造中被初始化。在绘制流程中的起始开始方法是performTraversals()或者说doTraversal()。而doTraversal()是放在一个
runnable中被执行的。执行这个runnable的就是Choreographer。在scheduleTraversals()方法：
```
void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
```
Choreographer通过postCallback()将runnable放到内部的一个CallbackQueue数组中，然后开始请求VSync信号(借助FrameDisplayEventReceiver)，收到信号后由
FrameHandler发送消息执行doFrame()方法计算帧数时间，决定是否重新请求VSync信号或调用doCallbacks()绘制此帧。在doCallbacks()方法内将CallbackQueue数组
内的runnable取出依次执行。于是doTraversal()就被执行了。


##### 参考资料
* <https://blog.csdn.net/huangqili1314/article/details/79824830> 
* <https://blog.csdn.net/qq_30993595/article/details/80931556>
* <https://www.cnblogs.com/huansky/p/11911549.html>
* <https://mp.weixin.qq.com/s/wy9V4wXUoEFZ6ekzuLJySQ>