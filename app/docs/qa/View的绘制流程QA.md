
### View的绘制相关问题


##### 说说View的绘制流程
> view的绘制有三个步骤：测量（measure），布局（layout），绘制（draw）, 从DecorView自上而下遍历整个View树。
> 1、测量阶段： 父view调用子View的measure()方法把上一步获得的MeasureSpec(测量规格)信息传递过去，子View的measure()
> 方法调用View#onMeasure()。最后调用setMeasuredDimension()设置自身大小。   
> 2、布局阶段： layout()方法中会先调用setFrame()给自身的left，top，right，bottom属性赋值,于是自己在父View中的位置就
> 确定。然后会调用onLayout()方法，让子View(一般指ViewGroup)自己实现。    
> 3、绘制阶段：  一般分4步：1、绘制背景；2、绘制视图内容,回调onDraw()；3、绘制子View，回调dispatchDraw()，一般
> ViewGroup用的比较多；4、绘制装饰，比如滚动条之类的。

##### 首次View的绘制流程是在什么时候触发的？
> 考验api流程了，关键如下：ActivityThread#handleResumeActivity() -> WindowManagerImpl#addView()
> ->WindowManagerGlobal#addView()-> ViewRootImpl()setView() -> ViewRootImpl#requestLayout()。  
所以答案是在 ActivityThread的handleResumeActivity()方法。


##### Activity、PhoneWindow、DecorView、ViewRootImpl 的关系？
> 包含关系大概为：Activity[Window->PhoneWindow[DecorView]],而ViewRootImpl可以说是DecorView的管家，继承了
> ViewParent接口，用来掌管View的各种事件，包括 requestLayout、invalidate、dispatchInputEvent等等。


##### DecorView 的布局是什么样的？
> DecorView实际构建的布局是分情况的，具体看到PhoneWindow#generateLayout()方法，根据各种不同情况来选择不一样的布
> 局。但他们都会有一个id为content的FrameLayout布局，这个布局是setContentView(R.layout.xx)中的布局的直接父布局。


##### setContentView 的流程
> Activity#setContentView() ->委托AppCompatDelegateImpl#setContentView() -> 
> AppCompatDelegateImpl#ensureSubDecor()准备DecorView ->从DecorView找到id为content的布局，
> 将view add进去。


##### 说说自定义view的几个构造函数
> 常用到的有3个方法，其参数情况分别是1个,2个,3个。1个参数的方法通常在创建对象是调用，也就是在代码中new；2个参数方法
> 通常在写在xml文件中被解析加载时调用；最 后一个通常不会自动被调用，需要手动调用。


##### ViewGroup是怎么分发绘制的
> 大概是说ViewGroup的dispatchDraw()方法吧。`dispatchDraw()`是在View.draw()绘制步骤中onDraw()之后的下一步操作。
> 顾名思义就是分发绘制，在View中是一个空实现。能分发子view绘制也只有ViewGroup。在dispatchDraw()会获取到子view的数
> 量，分别调用它们的draw()方法完成子view的绘制。


##### onLayout() 和layout()的区别
> onLayout()是父view，一般都是ViewGroup，确定子view位置调用的方法，通常会配合onMeasure()使用；而layout()是确
> 定view本身位置调用的方法。一般只会重写onLayout()方法，不用重写layout()。


##### 如何触发重新绘制？
> 调用 API requestLayout()或invalidate。


##### requestLayout() 和 invalidate() 的流程、区别
> 1、首先看到View中的requestLayout()方法:
```
    public void requestLayout() {
        if (mMeasureCache != null) mMeasureCache.clear();
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
            ViewRootImpl viewRoot = getViewRootImpl();
            
            //判断是否在layout过程。ViewRootImpl#isInLayout()方法会返回一个boolean值mInLayout。这个值会在进入host.layout()之前设置为true，之
            // 后设置为false。
            if (viewRoot != null && viewRoot.isInLayout()) { //如果viewRoot不是空并且viewRoot在layout阶段
                if (!viewRoot.requestLayoutDuringLayout(this)) { //正在处理layout(),就直接return。
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;
        }
        
        // 没有在处理layout，设置flag PFLAG_FORCE_LAYOUT。
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;

        if (mParent != null && !mParent.isLayoutRequested()) {
        // mParent会通过调用View#assignParent()对mParent赋值，调用处在ViewRootImpl的setView()中`view.assignParent(this);`
        // 所以这个mParent就是ViewRootImpl。
            mParent.requestLayout();  // 调用 ViewRootImpl的requestLayout()
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    }
```
> 从View中的requestLayout()方法可以看出，最终还是走的ViewRootImpl#requestLayout()方法。而
> ViewRootImpl#requestLayout()前面知道，会去调用scheduleTraversals()，触发一个完整的绘制流程。主要就是三大
> 步骤：performMeasure()、performLayout()、performDraw()。   
首先是performMeasure()，它最终回到View中的measure()方法，在View.measure()，能否触发onMeasure()主要看下面这点：
```
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
     // ...
     
     // 调用了requestLayout()，会为mPrivateFlags设置PFLAG_FORCE_LAYOUT标志。
     final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
     // ...
     
     final boolean needsLayout = specChanged && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize);
     if (forceLayout || needsLayout) {
        int cacheIndex = forceLayout ? -1 : mMeasureCache.indexOfKey(key); // forceLayout为true，onMeasure()则会被调用。
            if (cacheIndex < 0 || sIgnoreMeasureCache) {
                onMeasure(widthMeasureSpec, heightMeasureSpec);
                mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
            }
     }
     mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
     // ....
}
```
> 对于`forceLayout`的值,在requestLayout()的时候就说到mPrivateFlags设置PFLAG_FORCE_LAYOUT。所以这里的
> forceLayout == true。然后是 performLayout()，也是会到View.layout()方法：
```
//....

  boolean changed = isLayoutModeOptical(mParent) ?setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
  if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
      onLayout(changed, l, t, r, b);

      if (shouldDrawRoundScrollbar()) {
          if(mRoundScrollbarRenderer == null) {
              mRoundScrollbarRenderer = new RoundScrollbarRenderer(this);
          }
      } else {
          mRoundScrollbarRenderer = null;
      }
      mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED; // 清除标志。
      //......
  }   
```
> 看到if中的条件，mPrivateFlags是否又被设置PFLAG_LAYOUT_REQUIRED标志。在performMeasure()流程中是设置了PFLAG_LAYOUT_REQUIRED标志，
> 所以onLayout()是一定能执行到的。最后看到 performDraw()。performDraw流程会经历：  
performDraw() -> draw() -> drawSoftware() -> View.draw()。前面都是发生在ViewRootImpl中，看到View.draw():
```
public void draw(Canvas canvas) {
     //..... 
     
     final int viewFlags = mViewFlags;
     boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
     boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
     if (!verticalEdges && !horizontalEdges) {
            onDraw(canvas);
     //.......      
     }
     
     //.......
 }           
```
> 可以看到这里的onDraw()的触发与是否有设置FADING_EDGE_HORIZONTAL，FADING_EDGE_VERTICAL2个标志位有关，整个类搜索后，没有发现哪个地方是一定设置此标志。
回到ViewRootImpl中看到draw():
```
private boolean draw(boolean fullRedrawNeeded) {
    //.......
    
    if (!dirty.isEmpty() || mIsAnimating || accessibilityFocusDirty) {
         if (mAttachInfo.mThreadedRenderer != null && mAttachInfo.mThreadedRenderer.isEnabled()) {
             //........
         }else{
            if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset,
                 scalingRequired, dirty, surfaceInsets)) {
                 return false;
            }
         }
    }
    //......
}
```
> 同样的也没有绝对的一个标志，类似`PFLAG_FORCE_LAYOUT`,`PFLAG_LAYOUT_REQUIRED`控制onLayout(),onMeasure()回调。从`drawSoftware()`上来看，
onDraw()跟dirty区域相关，或者在执行动画等等。

> 2、invalidate()  
看到View.invalidate()，间接调用几个方法后来到 invalidateInternal(...)方法：
```
void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,boolean fullInvalidate) {
      //.....
      
      mPrivateFlags |= PFLAG_DIRTY; // 设置标志
      if (invalidateCache) {
            mPrivateFlags |= PFLAG_INVALIDATED;
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
      }

      final AttachInfo ai = mAttachInfo;
      final ViewParent p = mParent;
      if (p != null && ai != null && l < r && t < b) {
            final Rect damage = ai.mTmpInvalRect;
            damage.set(l, t, r, b);
            p.invalidateChild(this, damage);
      }
      
      //......
    }
```
> 看到`p.invalidateChild()`，其中p就是mParent，也就是父布局的意思，经过view树向上递归，会来到
> ViewRootImpl.invalidateChild()方法，然后调用到invalidateChildInParent()方法：
```
public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
   checkThread();
   if (dirty == null) {
       invalidate();
       return null;
   } else if (dirty.isEmpty() && !mIsAnimating) {
       return null;
   }

   if (mCurScrollY != 0 || mTranslator != null) {
       mTempRect.set(dirty);
       dirty = mTempRect;
       if (mCurScrollY != 0) {
            dirty.offset(0, -mCurScrollY);
       }
       if (mTranslator != null) {
            mTranslator.translateRectInAppWindowToScreen(dirty);
       }
       if (mAttachInfo.mScalingRequired) {
           dirty.inset(-1, -1);
       }
   }
  invalidateRectOnScreen(dirty);
  return null;
}
```
> 上面主要就是对dirty区域重新计算校验，最后来到`invalidateRectOnScreen(dirty)`:
```
    private void invalidateRectOnScreen(Rect dirty) {
        final Rect localDirty = mDirty;
        localDirty.union(dirty.left, dirty.top, dirty.right, dirty.bottom);
        final float appScale = mAttachInfo.mApplicationScale;
        final boolean intersected = localDirty.intersect(0, 0,
                (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));
        if (!intersected) {
            localDirty.setEmpty();
        }
        if (!mWillDrawSoon && (intersected || mIsAnimating)) {
            scheduleTraversals();
        }
    }
```
> 可以看到，最后还是调用了`scheduleTraversals()`触发绘制流程，但是由于没有设置`PFLAG_FORCE_LAYOUT`,`PFLAG_LAYOUT_REQUIRED`，直接设置`PFLAG_DIRTY`
标志，不会走测量和布局的两个流程。  
所以 invalidate()与 requestLayout()都会触发View树重新绘制。但是invalidate()不会触发测量与Layout过程，
> 而requestLayout()一定能触发测量与layout过程。


##### invalidate() 和 postInvalidate()的区别?
> invalidate()在主线程中使用；postInvalidate()可以在非主线程使用，其中使用了handler作为桥梁。


##### 为什么onCreate()获取不到View的宽高?
> 因为view的绘制是在onResume()之后才开始去绘制的，具体在ActivityThread#handleResumeActivity()方法内。


##### 在onResume()使用`handler.postRunnable`能获取到View的宽高吗?
> 不能，onResume()触发在ActivityThread的performResumeActivity()方法，在handleResumeActivity()方法内，
> 通过`wm.addView(decor, l);`将WindowManager添加DecorView，开始绘制流程。performResumeActivity()
> 方法在addView()之前执行。等于说onResume回调时，还没开始绘制。


##### 在onResume()使用`view.postRunnable`能获取到View的宽高吗?
> 能，这个要区分一下上面一个问题。先看到View的这个方法
```
public boolean post(Runnable action) {
 final AttachInfo attachInfo = mAttachInfo;
 if (attachInfo != null) {
     return attachInfo.mHandler.post(action);
 }
 getRunQueue().post(action);
 return true;
    }
```
> 先判断mAttachInfo是否为空(或者说这个View是否初始化过(绘制))，不为空自然能拿到view的宽高了。如果为null则通过getRunQueue()来发送，通过源码可以发现在
在View#dispatchAttachWindow()和ViewRootImp#performTraversals()分别有调用，来执行里面的action。而performTraversals()是绘制的开始，并且会发送
同步屏障阻碍同步消息的执行。这时候getRunQueue()发送的消息就只能在同步屏障解除后才能执行了，这时候View已经绘制完了。


##### MeasureSpec是什么
> MeasureSpec是一个类，表示的是一个32位的整形值，它的前2位表示测量模式SpecMode，后30位表示某种测量模式下的规格
大小SpecSize。通常用来说明应该如何测量这个View。


##### 子View创建MeasureSpec创建规则是什么?
> 可以看到`2.1、测量模式`板块下的表格，父布局的MeasureSpec与view自身的宽高属性决定的。


##### 自定义View的wrap_content属性不起作用的原因?
> 在View的默认测量模式下，View的测量模式是AT_MOST或者EXACTLY时，view的大小会被设置成MeasureSpec的specSize。
> 而当view使用wrap_content属性时，最终view的MeasureSpec都会被转换成AT_MOST，也就是父布局的大小了。


##### View#post与Handler#post的区别
> 看到View.post()方法：
```
public boolean post(Runnable action) {
   final AttachInfo attachInfo = mAttachInfo;
   if (attachInfo != null) {
       return attachInfo.mHandler.post(action);
   }
   getRunQueue().post(action);
   return true;
}
```
> 可以看到View#post当View已经attach到window，直接调用UI线程的Handler发送runnable。如果View还未attach到window，则通过getRunQueue().post()。这个
getRunQueue()获取的是HandlerActionQueue实例。等到执行performTraversals()方法时再将runnable发送出去。   
RunQueue的作用类似于MessageQueue，可以保存runnable。可以看到HandlerActionQueue里的post():
```
    public void post(Runnable action) {
        postDelayed(action, 0);
    }
    public void postDelayed(Runnable action, long delayMillis) {
        final HandlerAction handlerAction = new HandlerAction(action, delayMillis);
        synchronized (this) {
            if (mActions == null) {
                mActions = new HandlerAction[4];
            }
            mActions = GrowingArrayUtils.append(mActions, mCount, handlerAction);
            mCount++;
        }
    }
```
> HandlerAction是一个静态内部类，只有2个成员变量：Runnable与delay time。在post方法仅仅是将构建的HandlerAction添加到HandlerAction数组中。那么里面的
runnable到底什么时候被执行呢?根据HandlerAction数组的调用情况，可以在这个类中找到一个方法`executeActions()`：
```
    public void executeActions(Handler handler) {
        synchronized (this) {
            final HandlerAction[] actions = mActions;
            for (int i = 0, count = mCount; i < count; i++) {
                final HandlerAction handlerAction = actions[i];
                handler.postDelayed(handlerAction.action, handlerAction.delay);
            }
            mActions = null;
            mCount = 0;
        }
    }
```
> 通过Handler将runnable发送出去执行。搜索executeActions方法的调用。发现在ViewRootImpl#performTraversals()和View#dispatchAttachedToWindow()有
调用。performTraversals()方法是View的绘制流程的开端,所以在执行绘制的过程中就会将HandlerAction数组保存的runnable执行。调用处代码为
`getRunQueue().executeActions(mAttachInfo.mHandler);`。在另外dispatchAttachedToWindow()的调用可以追溯到performTraversals()方法的
`host.dispatchAttachedToWindow(mAttachInfo, 0);`。并且调用时间比getRunQueue().executeActions()还要靠前。


##### getWidth()和getMeasureWidth()的区别
> getMeasuredWidth()方法获得的值是setMeasuredDimension方法设置的值，它的值在measure方法运行后就会确定。可以说是
> 一个预期值。getWidth()方法在View中计算规则是mRight-mLeft。这俩是在layout()方法中通过setFrame()会重新赋值，
> 是view的最终显示宽度。因为可能会有padding以及margin值存在。getWidth()的值是 <= getMeasuredWidth()的。


##### View的onAttachedToWindow,onDetachedFromWindow调用时机，使用场景是什么？
> 顾名思义，onAttachedToWindow()是在activity对应的window被添加的时候调用，onDetachedFromWindow()就是window分离的时候调用(OnDestroy)。
onAttachedToWindow()可以追溯到ActivityThread#handleResumeActivity()的WindowManager.addView(decor),也就是绘制流程那一套。
最终来到ViewRootImpl#performTraversals()的`host.dispatchAttachedToWindow(mAttachInfo, 0);`处。并且只会执行一次，回调时机
在3大流程之，前所以在这个方法去获取不到view的宽高。  
onDetachedFromWindow()可追溯到ActivityThread#handleDestroyActivity()的WindowManager.removeViewImmediate()。
最后到ViewRootImpl。ViewRootImpl#die() --> ViewRootImpl#doDie() -->ViewRootImpl#dispatchDetachedFromWindow()。


##### 相对布局、线性布局、帧布局效率关系?

##### 自定义View如何考虑机型适配。

##### 自定义View的优化方案。

##### 屏幕刷新机制
> 电脑系统中cpu负责计算数据，gpu负责渲染，显示器负责显示。显示器将数据显示不是一次性完成的，它是按从左到右，从上到下的
> 顺序显示在屏幕上。比如60Hz的屏幕完成一次刷新花费时间大概是16.6ms(1000/60)。

##### 为什么使用双缓冲
> 正常情况下，cpu/gpu完成计算渲染放在缓冲区，显示器取出显示，这一过程是连续的，重复的。在屏幕绘制时，对于一个缓冲区
> 一边修改一边取出就很容易出现问题(并发修改)。就会导致屏幕上显示画面的数据来自不同帧，画面撕裂。  
解决：使用双缓存机制。

##### 什么是双缓存。  
> 双缓存就是让计算渲染的cpu/gpu跟抓取数据的显示器使用2个不同的缓存空间。cpu/gpu完成数据放到后缓冲区，当需要显示时，
> 与显示器抓取数据的前缓冲区交换数据。从而避免一个画面的数据来源不同帧。注意，当屏幕刷新时，cpu/gpu才开始计算渲染，将
> 数据放到后缓冲区。此时 前缓冲区不会发生变化，当后缓冲区数据完成，前后缓冲区才开始交换数据。双缓冲其实使用一种用于解
> 决画面撕裂的处理机制。

##### 知道什么是VSync吗   
> 现在知道屏幕刷新使用了2个缓冲区。那何时是最佳的交换数据时机呢?这时候VSync就上场了。VSync的全称
> 是Vertical Synchronization,即垂直同步。通俗说VSync是一个时间中断的信号。系统每次拿到VSync时就开始准备刷新屏幕，
> 保证双缓冲在最佳时间点进行交换，避免出现画面撕裂。与VSync相关的还有2个：Triple Buffer和Choreographer。他们是
> 在android4.1的黄油项目"Project Butter"一同出现的概念。黄油项目对Android Display系统进行了重构。

##### 说说Choreographer 
> Choreographer是用于配合系统的VSync中断信号的一种机制，起到调度的作用。Choreographer接收系统的VSync信号，保证系
> 统收到VSync信号才开始绘制，让每次绘制拥有完整的16.6ms。业界一般用它监控应用的帧率。

##### Choreographer如何实现的同步
> Choreographer通过postCallback()方法将runnable放到内部的一个CallbackQueue数组中，然后开始请求VSync信号(借
> 助FrameDisplayEventReceiver)，收到信号后由FrameHandler发送消息执行doFrame()方法计算帧数时间，决定是否重新请
> 求VSync信号或调用doCallbacks()绘制此帧。在doCallbacks()方法内将CallbackQueue数组内的runnable取出依次执行。
> 于是doTraversal()就被执行了。

##### 什么是SurfaceView  
> SurfaceView是Android中一种比较特殊的视图(View)，它跟普通View最大的区别是它跟它的视图容器并不是在同一个视图层上，
> 它的UI显示也可以不在一个独立的线程中完成，所以对SurfaceView的绘制并不会影响到主线程的运行。


##### 为什么使用SurfaceView
> View是通过刷新来重绘视图，系统通过发出VSync信号来进行屏幕的重绘，刷新的时间间隔是16ms,如果可以在16ms以内将绘制工
> 作完成，则没有任何问题，如果绘制过程逻辑很复杂，并且界面更新还非常频繁，这时候就会造成界面的卡顿，影响用户体验，
> 为此Android提供了SurfaceView来解决这一问题。


##### View与surfaceView的区别?
> 1、View适用于主动更新的情况，而SurfaceView更适用于被动更新的情况，比如频繁刷新界面。  
2、View在主线程中刷新页面，而SurfaceView在子线程刷新页面。  
3、View在绘图时没有实现双缓冲机制，SurfaceView在底层机制中实现双缓冲机制。


##### LayoutInflate 的流程

##### Android中的动画有哪几类，它们的特点和区别是什么

##### 描述一下getX()、getRawX()、getTranslationX()

##### Interpolator和TypeEvaluator是什么，有什么用