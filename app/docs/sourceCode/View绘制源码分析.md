
### View的绘制流程源码跟踪


#### 1、(加载)Window、PhoneWindow、DecorView

在跟踪ActivityThread启动activity最后阶段的时候就有提到过window，就是在ActivityThread#performLaunchActivity()，看到这段代码：
```html
   Window window = null;
   if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
       window = r.mPendingRemoveWindow;
       r.mPendingRemoveWindow = null;
       r.mPendingRemoveWindowManager = null;
   }
   appContext.setOuterContext(activity);
   activity.attach(appContext, this, getInstrumentation(), r.token,
           r.ident, app, r.intent, r.activityInfo, title, r.parent,
           r.embeddedID, r.lastNonConfigurationInstances, config,
           r.referrer, r.voiceInteractor, window, r.configCallback);
```
跟踪到Activity#attach()：
```html
    @UnsupportedAppUsage
    final void attach(...){
    
    // ...省略代码
    
    mWindow = new PhoneWindow(this, window, activityConfigCallback); // 创建PhoneWindow的实例
    mWindow.setWindowControllerCallback(mWindowControllerCallback);
    mWindow.setCallback(this);
    mWindow.setOnWindowDismissedCallback(this);
    mWindow.getLayoutInflater().setPrivateFactory(this);
    
    //...省略代码
    
    mWindow.setWindowManager((WindowManager)context.getSystemService(Context.WINDOW_SERVICE),  // 关联WindowManager
                mToken, mComponent.flattenToString(),
                (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
    if (mParent != null) {
         mWindow.setContainer(mParent.getWindow());
    }
    mWindowManager = mWindow.getWindowManager(); // 是WindowManagerImpl的实例
    mCurrentConfig = config;
    mWindow.setColorMode(info.colorMode);
    mWindow.setPreferMinimalPostProcessing((info.flags & ActivityInfo.FLAG_PREFER_MINIMAL_POST_PROCESSING) != 0);
    }
```
看到`mWindow.setWindowManager()`
```html
  public void setWindowManager(WindowManager wm, IBinder appToken, String appName,
         boolean hardwareAccelerated) {
        mAppToken = appToken;
        mAppName = appName;
        mHardwareAccelerated = hardwareAccelerated;
        if (wm == null) {
            wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        }
        mWindowManager = ((WindowManagerImpl)wm).createLocalWindowManager(this);
  }    
  public WindowManagerImpl createLocalWindowManager(Window parentWindow) {
      return new WindowManagerImpl(mContext, parentWindow);
  }
```
attach()方法实际还是做初始化的事情，mWindow是PhoneWindow实例(Window本身是一个抽象类)，mWindowManager是WindowManager对象，但WindowManager是
一个接口，最后获取的实例它的子类WindowManagerImpl。   
performLaunchActivity()之后会进入到Activity生命周期，体现就是走Activity#onCreate()方法。设置布局的入口在`setContentView(R.layout.activity_main);`
看下setContentView():
```html
  @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
  }
```
这里看的源代码是android 11，对应api版本是30。不同版本的api实现可能有些不一样。`setContentView()`这里使用了委托，委托给AppCompatDelegate，
而AppCompatDelegate是一个抽象类，实现在AppCompatDelegateImpl。看到这个类的`setContentView`:
```html
    public void setContentView(int resId) {
        ensureSubDecor();
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        LayoutInflater.from(mContext).inflate(resId, contentParent);
        mOriginalWindowCallback.onContentChanged();
    }
```
看到AppCompatDelegateImpl#ensureSubDecor()：
```text
    private void ensureSubDecor() {
        if (!mSubDecorInstalled) {
            mSubDecor = createSubDecor(); // 创建mSubDecor,确保mSubDecor不是null
            // If a title was set before we installed the decor, propagate it now
            CharSequence title = getTitle();
            if (!TextUtils.isEmpty(title)) {
                if (mDecorContentParent != null) {
                    mDecorContentParent.setWindowTitle(title);  // 标题
                } else if (peekSupportActionBar() != null) {
                    peekSupportActionBar().setWindowTitle(title);
                } else if (mTitleView != null) {
                    mTitleView.setText(title);
                }
            }
            applyFixedSizeWindow();
            onSubDecorInstalled(mSubDecor);
            mSubDecorInstalled = true;
           
            PanelFeatureState st = getPanelState(FEATURE_OPTIONS_PANEL, false);
            if (!mIsDestroyed && (st == null || st.menu == null)) {
                invalidatePanelMenu(FEATURE_SUPPORT_ACTION_BAR);
            }
        }
    }
```
mSubDecor是ViewGroup实例。猜测是容纳传入View的容器，但是不是Window还不能确定。跟踪`createSubDecor()`,方法略长，只挑选些关键部分，省略部分代码以
及注释，log等：
```text
    private ViewGroup createSubDecor() {
       
        // 省略代码。。。主要就是通过TypedArray获取主题样式。比如ActionBar，windowNoTitle等设置

        mWindow.getDecorView();  // 获取DecorView。DecorView继承至FrameLayout，也是一个容器来的
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup subDecor = null;
        if (!mWindowNoTitle) { // 如果有标题栏
            if (mIsFloating) { // 悬浮模式              
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_dialog_title_material, null);
                mHasActionBar = mOverlayActionBar = false;
                
            } else if (mHasActionBar) { // 有ActionBar              
                TypedValue outValue = new TypedValue();
                mContext.getTheme().resolveAttribute(R.attr.actionBarTheme, outValue, true);
                Context themedContext;
                if (outValue.resourceId != 0) {
                    themedContext = new ContextThemeWrapper(mContext, outValue.resourceId);
                } else {
                    themedContext = mContext;
                }
                subDecor = (ViewGroup) LayoutInflater.from(themedContext).inflate(R.layout.abc_screen_toolbar, null);
                mDecorContentParent = (DecorContentParent) subDecor.findViewById(R.id.decor_content_parent);
                mDecorContentParent.setWindowCallback(getWindowCallback());

                // 设置样式到mDecorContentParent，如进度条，ActionBar等等
            }
        } else {
            if (mOverlayActionMode) {
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_screen_simple_overlay_action_mode, null);
            } else {
                subDecor = (ViewGroup) inflater.inflate(R.layout.abc_screen_simple, null);
            }
            // (。。。。省略代码) 设置应用window的监听器(主要用于布局展示View)，有版本方法限制，分水岭版本号为21。                  
        }
        if (mDecorContentParent == null) {  // 找到title
            mTitleView = (TextView) subDecor.findViewById(R.id.title);
        }
        // Make the decor optionally fit system windows, like the window's decor(适应系统窗口)
        ViewUtils.makeOptionalFitsSystemWindows(subDecor);
        
        final ContentFrameLayout contentView = (ContentFrameLayout) subDecor.findViewById(
                R.id.action_bar_activity_content);               
        final ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content); //
        if (windowContentView != null) {
            // 如果已经有view容器添加到decorView，要把这些内容迁移到新的容器中
            // There might be Views already added to the Window's content view so we need to
            // migrate them to our content view         
            while (windowContentView.getChildCount() > 0) {
                final View child = windowContentView.getChildAt(0);
                windowContentView.removeViewAt(0);
                contentView.addView(child);
            }
            // Change our content FrameLayout to use the android.R.id.content id.
            // Useful for fragments.
            windowContentView.setId(View.NO_ID);
            contentView.setId(android.R.id.content); //将新的容器id设置为R.id.content。

            // The decorContent may have a foreground drawable set (windowContentOverlay).
            // Remove this as we handle it ourselves
            if (windowContentView instanceof FrameLayout) {
                ((FrameLayout) windowContentView).setForeground(null);
            }
        }
        // Now set the Window's content view with the decor
        mWindow.setContentView(subDecor);  //  将新的容器设置到window。

        contentView.setAttachListener(new ContentFrameLayout.OnAttachListener() {
            @Override
            public void onAttachedFromWindow() {}
            @Override
            public void onDetachedFromWindow() {
                dismissPopups();
            }
        });
        return subDecor;
    }
```
看到摘选代码中的`mWindow.getDecorView();`，前面说过Window的实现是在PhoneWindow,所以要在PhoneWindow类找getDecorView()。经过方法调用，最终来到
PhoneWindow的`installDecor()`。
```text
    private void installDecor() {
        mForceDecorInstall = false;
        if (mDecor == null) {
            mDecor = generateDecor(-1);
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        } else {
            mDecor.setWindow(this);
        }
        if (mContentParent == null) {
            mContentParent = generateLayout(mDecor);            
             // 。。。省略一些样式默认设置代码
         }
    }
```
installDecor()保证了mDecor不会为null，并且设置一些系统相关样式属性参数。`generateDecor(-1);`方法主要是先获取Context。是使用，然后new出DecorView对象。
另外`generateLayout(mDecor)`这里有一点要注意：
```html
protected ViewGroup generateLayout(DecorView decor){
 // 省略前面代码。。。
 
 // 此前一部分是Inflate decor逻辑。其实就是Inflate layoutResource。layoutResource为系统xml文件，这个文件有一个id为content的FrameLayout
 // 这也是后面可以直接findViewById(ID_ANDROID_CONTENT)的原因。
 mDecor.startChanging();
 mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
 ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
 if (contentParent == null) {
    throw new RuntimeException("Window couldn't find content container view");
 }
 // 省略代码。。。
 return contentParent;
 }
```
返回的是从decor中找到id为ID_ANDROID_CONTENT的一个View。ID_ANDROID_CONTENT还有另外一个身份就是com.android.internal.R.id.content。这个后面会用到。   
回到`createSubDecor()`。mWindow.getDecorView()执行完毕。继续往下执行。整个方法后面一部分核心还是inflate出一个View作为新的window容器，也就是subDecor。
到后面
>final ViewGroup windowContentView = (ViewGroup) mWindow.findViewById(android.R.id.content);

这里找到id为R.id.content的View，如果能找到并且它原来有其他的子view，要把这些内容迁移到新的容器(contentView)。contentView是subDecor的第一个子view，
是一个ContentFrameLayout。随后将contentView的id设置为android.R.id.content。最后将新的容器设置给Window(`mWindow.setContentView(subDecor);`)，
PhoneWindow#setContentView(),最终或调用下面方法：
```html
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            view.setLayoutParams(params);
            final Scene newScene = new Scene(mContentParent, view);
            transitionTo(newScene);
        } else {
            mContentParent.addView(view, params);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
        mContentParentExplicitlySet = true;
    }
```
前面第一个if else中`installDecor()`前面已经有看过，下面看到`FEATURE_CONTENT_TRANSITIONS`这个flag，大概翻译就是内容过渡标志。我的理解是类似activity
的转场动画。带有这个标志是内容中的某个控件或元素支持过渡动画。如果需要内容过渡则通过Scene添加view，否则直接添加view到mContentParent。在Scene也可以看到：
```html
    public Scene(ViewGroup sceneRoot, View layout) {
        mSceneRoot = sceneRoot;
        mLayout = layout;
    }
```
```html
    public void enter() {
        // Apply layout change, if any
        if (mLayoutId > 0 || mLayout != null) {
            // empty out parent container before adding to it
            getSceneRoot().removeAllViews();
            if (mLayoutId > 0) {
                LayoutInflater.from(mContext).inflate(mLayoutId, mSceneRoot);
            } else {
                mSceneRoot.addView(mLayout); // mSceneRoot就是mContentParent
            }
        }
        if (mEnterAction != null) {
            mEnterAction.run();
        }
        setCurrentScene(mSceneRoot, this);
    }
```
最终还是会将subDecor添加到mContentParent容器中去。到此做个小结：
* Window 是一个抽象类，每个Activity都有一个Window，具体实现类为PhoneWindow。
* PhoneWindow 是Window的实现类，所有具体的绘制逻辑都在这个类中，Window或者说PhoneWindow处在同一个层级上。PhoneWindow内部有一个DecorView的实例。
* DecorView 继承FrameLayout，是所有视图的根view。它的inflate逻辑取根据系统主题样式由系统创建。它有个id为`android.R.id.content`的子View。
* id为android.R.id.content的View(mContentParent/contentView,在不同类中有不同的名称) DecorView中的一个子view，实质也是一个FrameLayout，在构建
  时可能会被替换为ContentFrameLayout(也是继承FrameLayout)，但id不会被改变。开发中为activity设置的ContentView，就是它的子View。

subDecor添加完mContentParent后，一直返回到最开始地方，也就是AppCompatDelegateImpl#setContentView()中的ensureSubDecor(),再贴一遍代码：
```text
    public void setContentView(View v) {
        ensureSubDecor(); // 完成了Decor的创建初始化工作
        ViewGroup contentParent = (ViewGroup) mSubDecor.findViewById(android.R.id.content);
        contentParent.removeAllViews();
        contentParent.addView(v);
        mOriginalWindowCallback.onContentChanged();
    }
```
剩下逻辑就是将自己绘制的xml生成的View添加到contentParent容器中。到此Activity#setContentView(R.layout.xx)流程跟踪结束，同时对Window，PhoneWindow，
DecorView也有一个比较清楚的认识。这里额外做一个小测试，递归打印View的父类处理：从自己的xml文件开始：
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <TextView
        android:id="@+id/tvHello"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```
打印代码为
```html
       View view =  findViewById(R.id.tvHello);
       Log.d("TAG", "view= "+ view);
       while (view != null) {
           view = (View) view.getParent();
          Log.d("TAG", "parent= "+ view);
       }
```
输出结果：
```text
D: view= androidx.appcompat.widget.AppCompatTextView{2d62ef0 V.ED.... ......ID 0,0-0,0 #7f07008d app:id/tvHello}
D: parent= androidx.constraintlayout.widget.ConstraintLayout{351f4c69 V.E..... ......I. 0,0-0,0}
D: parent= androidx.appcompat.widget.ContentFrameLayout{29d1b6ee V.E..... ......I. 0,0-0,0 #1020002 android:id/content}
D: parent= androidx.appcompat.widget.ActionBarOverlayLayout{1e1b978f V.E..... ......I. 0,0-0,0 #7f070030 app:id/decor_content_parent}
D: parent= android.widget.FrameLayout{1874a1c V.E..... ......I. 0,0-0,0}
D: parent= android.widget.LinearLayout{cd74625 V.E..... ......I. 0,0-0,0}
D: parent= com.android.internal.policy.impl.PhoneWindow$DecorView{34a653fa V.E..... R.....ID 0,0-0,0}
D: parent= null
```
tvHello是一个TextView，它的父布局是一个ConstraintLayout。再往上就是ContentFrameLayout，这表示DecorView中id=content的子view的是被替换过的。view
的最根父view是DecorView。

View的绘制关键就3部分  
onMeasure()  
onLayout()  
onSizeChanged()  
onDraw()


#### 2、View的绘制流程
Activity的onCreate()方法结束，进入到onResume()。但是在这之前在ActivityThread会先执行handleResumeActivity():
```text
 public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward,
            String reason) {
        // 省略代码。。。
        
        if (r.window == null && !a.mFinished && willBeVisible) {
            r.window = r.activity.getWindow(); // window与activity关联
            View decor = r.window.getDecorView();
            decor.setVisibility(View.INVISIBLE);
            ViewManager wm = a.getWindowManager();
            WindowManager.LayoutParams l = r.window.getAttributes();
            a.mDecor = decor;
            l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
            l.softInputMode |= forwardBit;
            if (r.mPreserveWindow) {
                a.mWindowAdded = true;
                r.mPreserveWindow = false;              
                ViewRootImpl impl = decor.getViewRootImpl();
                if (impl != null) {
                    impl.notifyChildRebuilt();
                }
            }
            if (a.mVisibleFromClient) {
                if (!a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l); // 将decor添加到wm中。关注点在这里
                } else {                  
                    a.onWindowAttributesChanged(l);
                }
            }
            // 。。。省略代码
        }   
 }                       
```
在这个方法中，会将activity与window关联，开始添加doctor`wm.addView(decor, l)`。前面就知道，这里的WindowManager实现是WindowManagerImpl实例。而
WindowManagerImpl中的add逻辑又是交给WindowManagerGlobal处理。看到WindowManagerGlobal#addView();
```text
    public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow, int userId) {
        // 省略代码。。。
        synchronized (mLock) {
         // 省略代码。。。
            root = new ViewRootImpl(view.getContext(), display);
            view.setLayoutParams(wparams);
            mViews.add(view); // mViews = view的list
            mRoots.add(root); // mRoots = ViewRootImpl的list
            mParams.add(wparams);
            try {
                root.setView(view, wparams, panelParentView, userId); // view是DecorView。
            } catch (RuntimeException e) {
                if (index >= 0) {
                    removeViewLocked(index, true);
                }
                throw e;
            }
        }
    }
```
看到`root.setView(...);`这里引出了一个新的类:ViewRootImpl。看到它的setView()方法：
```text
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView,  int userId) {
        synchronized (this) {
            if (mView == null) {
                mView = view;
                //省略代码。。。。
                
                requestLayout(); // 关注这一句即可。
                // InputChannel是Window与InputManagerService之间的通信桥梁，用来管理输入事件(事件分发)。手机硬件模块产生的屏幕事件通过
                // InputManagerService接收，service通过InputChannel将事件传递到window(activity)。
                InputChannel inputChannel = null; 
                // 。。。省略代码
            }
            //。。。
        }
        。。。省略代码
}           
```
重点是`requestLayout()`方法：
```html
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            checkThread();
            mLayoutRequested = true;
            scheduleTraversals();
        }
    }
```
checkThread()就是检查当前线程是否是`original thread`,否则会抛出一个常见的异常
>"Only the original thread that created a view hierarchy can touch its views."

看到`scheduleTraversals()`方法：
```html
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null); // 关键在mTraversalRunnable这个Runnable
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
```
`mTraversalRunnable`是一个Runnable，他的run方法逻辑只执行了一个方法`doTraversal();`
```text
    void doTraversal() {
        if (mTraversalScheduled) {
            mTraversalScheduled = false;
            mHandler.getLooper().getQueue().removeSyncBarrier(mTraversalBarrier); // 移除同步屏障
            if (mProfile) {
                Debug.startMethodTracing("ViewAncestor");
            }
            performTraversals();
            if (mProfile) {
                Debug.stopMethodTracing();
                mProfile = false;
            }
        }
    }
```
`doTraversal()`会执行到`performTraversals()`,到这里才是真真正正的开始绘制。下面开始跟踪理解这个方法。(注：这个方法又大又长，会删除部分没意义的log，
debug日志，注释等)。
```text
    private void performTraversals() {
        // cache mView since it is used so much below...
        final View host = mView; mView在setView()方法被赋值，这个mView/host就是DecorView的实例。
        if (host == null || !mAdded) // mAdded在setView()会被赋值为true。
            return;
            
        mIsInTraversal = true; //是否在循环绘制
        mWillDrawSoon = true;  // 是否马上绘制
        boolean windowSizeMayChange = false; // 窗口是否发生改变
        WindowManager.LayoutParams lp = mWindowAttributes;

        int desiredWindowWidth; // 预期窗口宽度
        int desiredWindowHeight; // 预期窗口高度

        final int viewVisibility = getHostVisibility(); // 根view是否可见
        final boolean viewVisibilityChanged = !mFirst   // view可见状态是否发生变化
                && (mViewVisibility != viewVisibility || mNewSurfaceNeeded
                || mAppVisibilityChanged);
        mAppVisibilityChanged = false;
        final boolean viewUserVisibilityChanged = !mFirst &&
                ((mViewVisibility == View.VISIBLE) != (viewVisibility == View.VISIBLE));

        WindowManager.LayoutParams params = null;
        CompatibilityInfo compatibilityInfo =
                mDisplay.getDisplayAdjustments().getCompatibilityInfo();
        if (compatibilityInfo.supportsScreen() == mLastInCompatMode) {
            params = lp;
            mFullRedrawNeeded = true; // 是否需要重新绘制
            mLayoutRequested = true; // 是否需要重新布局
            if (mLastInCompatMode) {
                params.privateFlags &= ~WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = false;
            } else {
                params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
                mLastInCompatMode = true;
            }
        }

        Rect frame = mWinFrame; // 保存窗口大小
        if (mFirst) { // 第一次绘制，都需要绘制与重新布局
            mFullRedrawNeeded = true;
            mLayoutRequested = true;

            final Configuration config = mContext.getResources().getConfiguration();
            if (shouldUseDisplaySize(lp)) { // 如果包含状态栏
                // NOTE -- system code, won't try to do compat mode.
                Point size = new Point();
                mDisplay.getRealSize(size); // 获取实际(可用)窗口大小，不包括状态栏大小
                desiredWindowWidth = size.x;
                desiredWindowHeight = size.y;
            } else if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                    || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) { // 如果view的LayoutParams是WRAP_CONTENT，则使用屏幕大小
                
                desiredWindowWidth = dipToPx(config.screenWidthDp); 
                desiredWindowHeight = dipToPx(config.screenHeightDp);
            } else {
               
                desiredWindowWidth = frame.width();  // 否则使用上次保存的大小
                desiredWindowHeight = frame.height();
            }

            // We used to use the following condition to choose 32 bits drawing caches:
            // PixelFormat.hasAlpha(lp.format) || lp.format == PixelFormat.RGBX_8888
            // However, windows are now always 32 bits by default, so choose 32 bits
            mAttachInfo.mUse32BitDrawingCache = true; // 默认使用32位绘制缓存
            mAttachInfo.mWindowVisibility = viewVisibility; // 设置窗口是否可见
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mLastConfigurationFromResources.setTo(config);
            mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
            // Set the layout direction if it has not been set before (inherit is the default)
            if (mViewLayoutDirectionInitial == View.LAYOUT_DIRECTION_INHERIT) { //如果之前没有设置布局方向，则设置布局方向
                host.setLayoutDirection(config.getLayoutDirection());
            }
            // 会触发View.onAttachedToWindow()的回调，并且只会执行一次，注意回调在3大流程之前。
            host.dispatchAttachedToWindow(mAttachInfo, 0); 
            mAttachInfo.mTreeObserver.dispatchOnWindowAttachedChange(true);
            dispatchApplyInsets(host);
        } else { // 不是第一次绘制
            desiredWindowWidth = frame.width(); //直接使用保存的窗口大小
            desiredWindowHeight = frame.height();
            if (desiredWindowWidth != mWidth || desiredWindowHeight != mHeight) { // 预期宽高与实际宽高不一样，需要重新绘制，布局
                if (DEBUG_ORIENTATION) Log.v(mTag, "View " + host + " resized to: " + frame);
                mFullRedrawNeeded = true;
                mLayoutRequested = true;
                windowSizeMayChange = true;
            }
        }

        if (viewVisibilityChanged) { // 如果窗口可见发生改变
            mAttachInfo.mWindowVisibility = viewVisibility;
            host.dispatchWindowVisibilityChanged(viewVisibility); // 触发监听器
            if (viewUserVisibilityChanged) {
                host.dispatchVisibilityAggregated(viewVisibility == View.VISIBLE);
            }
            if (viewVisibility != View.VISIBLE || mNewSurfaceNeeded) {
                endDragResizing();  // 通知所有监听器窗口大小调整已经结束。
                destroyHardwareResources(); // 释放相关硬件资源
            }
        }
        // Non-visible windows can't hold accessibility focus.
        if (mAttachInfo.mWindowVisibility != View.VISIBLE) {
            host.clearAccessibilityFocus();// 清除焦点
        }
        // Execute enqueued actions on every traversal in case a detached view enqueued an action
        getRunQueue().executeActions(mAttachInfo.mHandler);
        boolean cutoutChanged = false;
        
        boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
        if (layoutRequested) { // 需要重新布局
            final Resources res = mView.getContext().getResources();
            if (mFirst) {
                // make sure touch mode code executes by setting cached value
                // to opposite of the added touch mode.
                mAttachInfo.mInTouchMode = !mAddedTouchMode;  // 设置触控模式
                ensureTouchModeLocally(mAddedTouchMode);
            } else {  // (跟前面一段大概差不多)
            
                //这里的equals是判断内部的mInner是否相同，如果窗口需要重新设置大小，mPendingDisplayCutout内部的mInner可能会改变
                if (!mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout)) {
                    cutoutChanged = true;
                }
                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
                    windowSizeMayChange = true;
                    if (shouldUseDisplaySize(lp)) {
                        // NOTE -- system code, won't try to do compat mode.
                        Point size = new Point();
                        mDisplay.getRealSize(size);
                        desiredWindowWidth = size.x;
                        desiredWindowHeight = size.y;
                    } else {
                        Configuration config = res.getConfiguration();
                        desiredWindowWidth = dipToPx(config.screenWidthDp);
                        desiredWindowHeight = dipToPx(config.screenHeightDp);
                    }
                }
            }

            // Ask host how big it wants to be  测量整个view层级的大小，意味着会有遍历子view的操作。
            windowSizeMayChange |= measureHierarchy(host, lp, res,
                    desiredWindowWidth, desiredWindowHeight);
        }
```
这一段主要计算窗口的大小，到`measureHierarchy()`
```text
private boolean measureHierarchy(final View host, final WindowManager.LayoutParams lp,
            final Resources res, final int desiredWindowWidth, final int desiredWindowHeight) {
        int childWidthMeasureSpec;  //子view宽度的测量规格
        int childHeightMeasureSpec; //子view 高度的测量规格
        boolean windowSizeMayChange = false; // 窗口大小是否变化    
        
        boolean goodMeasure = false; // 相对完美的测量结果标识
        if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) { // 当且是WRAP_CONTENT的情况下优化
            // On large screens, we don't want to allow dialogs to just
            // stretch to fill the entire width of the screen to display
            // one line of text.  First try doing the layout at a smaller
            // size to see if it will fit.
            // 窗口大小尺寸优化处理。比如在大屏幕上，没必要让对话框使用拉伸来填充整个屏幕的宽度去显示一行文本。可以首先尝试在一个较小的布局大小看看是否合适。
            final DisplayMetrics packageMetrics = res.getDisplayMetrics();
            res.getValue(com.android.internal.R.dimen.config_prefDialogWidth, mTmpValue, true);
            int baseSize = 0;
            if (mTmpValue.type == TypedValue.TYPE_DIMENSION) { // 默认先使用一个基准值
                baseSize = (int)mTmpValue.getDimension(packageMetrics);
            }
            if (baseSize != 0 && desiredWindowWidth > baseSize) { // 基准值不能大于整个窗口的预期大小
                childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width); // 计算根窗口宽高的测量规格
                childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
                performMeasure(childWidthMeasureSpec, childHeightMeasureSpec); // 以当前的基准值获取的根窗口测量规去测量子view，应该
                // 是相当于一个标准的作用。
                
                if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL) == 0) { // 当前测量很完美
                    goodMeasure = true;
                } else { 说明当前使用的基准值不符合，需要重新定义基准值，重新测量
                    // Didn't fit in that size... try expanding a bit. 
                    baseSize = (baseSize+desiredWindowWidth)/2; 
                    childWidthMeasureSpec = getRootMeasureSpec(baseSize, lp.width);
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec); //关键点
                    if ((host.getMeasuredWidthAndState()&View.MEASURED_STATE_TOO_SMALL) == 0) { // 同样判断当前测量结果是否完美                      
                        goodMeasure = true;
                    }
                }
            }
        }
        if (!goodMeasure) { // 还没有找到一个完美的测量结果，不管了，直接使用窗口尺寸作为标准测量子view了。
            childWidthMeasureSpec = getRootMeasureSpec(desiredWindowWidth, lp.width);
            childHeightMeasureSpec = getRootMeasureSpec(desiredWindowHeight, lp.height);
            performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
            if (mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight()) {
                windowSizeMayChange = true;
            }
        }
        return windowSizeMayChange;
    }
```
measureHierarchy()方法更像是做了一个窗口大小优化工作，来保证这个view层级视图是最合适，最舒服的。同时会调用到performMeasure()方法，触发自定义控件
中onMeasure()的回调:
```text
 private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        if (mView == null) {
            return;
        }
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
        try {
            mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);  // 注意了，这个mView实际是DecorView。
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }
    }
```
在DecorView并没有`mView.measure(*,*)`这个方法，只能找它的父类FrameLayout->ViewGroup->View。最后会在View中找到：
```html
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean optical = isLayoutModeOptical(this); //是否是一个可见的viewGroup
        if (optical != isLayoutModeOptical(mParent)) { // 重新校准测量规格，
            Insets insets = getOpticalInsets();
            int oWidth  = insets.left + insets.right;
            int oHeight = insets.top  + insets.bottom;
            widthMeasureSpec  = MeasureSpec.adjust(widthMeasureSpec,  optical ? -oWidth  : oWidth);
            heightMeasureSpec = MeasureSpec.adjust(heightMeasureSpec, optical ? -oHeight : oHeight);
        }

        // Suppress sign extension for the low bytes 将当前的宽高规格转换后方式生成一个key，用来保存(使用LongSparseLongArray)
        long key = (long) widthMeasureSpec << 32 | (long) heightMeasureSpec & 0xffffffffL;
        if (mMeasureCache == null) mMeasureCache = new LongSparseLongArray(2);
        
        // 如果调用了requestLayout，forceLayout是为true的，因为会设置flag为PFLAG_FORCE_LAYOUT。
        final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT; // 是否需要强制布局。

        // Optimize layout by avoiding an extra EXACTLY pass when the view is
        // already measured as the correct size. In API 23 and below, this
        // extra pass is required to make LinearLayout re-distribute weight.
        
        // 优化布局
        final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec // 规格是否改变(当前的测量规格是否与缓存值相同)
                || heightMeasureSpec != mOldHeightMeasureSpec;
        final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY // 是否是MeasureSpec.EXACTLY模式
                && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
        final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec) // 规格(测量的宽高)是否匹配
                && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
        final boolean needsLayout = specChanged
                && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize); // 是否需要重新布局

        if (forceLayout || needsLayout) { // 需要重新布局
            // first clears the measured dimension flag // 清除测量结果标记
            mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET; 
            //解析所有RTL相关属性。猜测是去解析xml布局了，有布局方向，尺寸，drawable，padding等等
            resolveRtlPropertiesIfNeeded();

            int cacheIndex = forceLayout ? -1 : mMeasureCache.indexOfKey(key);
            if (cacheIndex < 0 || sIgnoreMeasureCache) { //如果需要重新布局触发onMeasure()回调(如果是低版本-19以下，强制执行，忽略forceLayout值)
                // measure ourselves, this should set the measured dimension flag back
                onMeasure(widthMeasureSpec, heightMeasureSpec);
                mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
            } else {  // 否则使用之前保存的值,用前面的key
                long value = mMeasureCache.valueAt(cacheIndex);
                // Casting a long to int drops the high 32 bits, no mask needed
                setMeasuredDimensionRaw((int) (value >> 32), (int) value);
                mPrivateFlags3 |= PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
            } 
            // flag not set, setMeasuredDimension() was not invoked, we raise
            // an exception to warn the developer //强制性判断，要求一定要调用setMeasuredDimensionRaw()方法，否则抛出异常
            if ((mPrivateFlags & PFLAG_MEASURED_DIMENSION_SET) != PFLAG_MEASURED_DIMENSION_SET) {
                throw new IllegalStateException("View with id " + getId() + ": "
                        + getClass().getName() + "#onMeasure() did not set the"
                        + " measured dimension by calling"
                        + " setMeasuredDimension()");
            }

            mPrivateFlags |= PFLAG_LAYOUT_REQUIRED; //更新测量结果重新标记
        }
        mOldWidthMeasureSpec = widthMeasureSpec;
        mOldHeightMeasureSpec = heightMeasureSpec;
        // 保存当前的测量规格，mMeasuredWidth、mMeasuredHeight的值会在setMeasuredDimensionRaw()方法重新赋值，同时更新测量标志mPrivateFlags。
        mMeasureCache.put(key, ((long) mMeasuredWidth) << 32 | 
                (long) mMeasuredHeight & 0xffffffffL); // suppress sign extension
    }
```
总体上看`mView.measure(*,*)`这个方法主要还是做优化工作(一些详细看上面注释)，特别是针对测量规格，其中很重要的一点就是回触发onMeasure()回调。写过自定义
View的都知道，这是重要一环。把测量这一步骤交给开发者自己去决定。  
下面回到ViewRootImpl#measureHierarchy()方法，也就是接着执行完`performMeasure()`之后，再次判断测量结果是否是最后，最后返回表示窗口是否发生变化的
boolean结果。performMeasure()方法结束，流程重新回到performTraversals()中，接着 measureHierarchy()往下：
```text
       // ...衔接 measureHierarchy()
       if (collectViewAttributes()) { // 保存View的属性
            params = lp;
       }
        if (mAttachInfo.mForceReportNewAttributes) {
            mAttachInfo.mForceReportNewAttributes = false;
            params = lp;
        }
        if (mFirst || mAttachInfo.mViewVisibilityChanged) { // 如果是第一次绘制，或者view的可见状态发生变化，需要重新调整布局属性
            mAttachInfo.mViewVisibilityChanged = false;
            int resizeMode = mSoftInputMode &
                    WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST;
                    
            // 如果我们处于自动调整大小模式，那么我们需要确定现在使用什么模式,重新调整布局参数。
            if (resizeMode == WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED) {
                final int N = mAttachInfo.mScrollContainers.size();
                for (int i=0; i<N; i++) {
                    if (mAttachInfo.mScrollContainers.get(i).isShown()) {
                        resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                    }
                }
                if (resizeMode == 0) {
                    resizeMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                }
                if ((lp.softInputMode &
                        WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) != resizeMode) {
                    lp.softInputMode = (lp.softInputMode &
                            ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST) |
                            resizeMode;
                    params = lp;
                }
            }
        }
        if (mApplyInsetsRequested) {// 是否接受重新测量请求，一般发生绘制时mApplyInsetsRequested的值都是true，
            dispatchApplyInsets(host); //分发请求，mApplyInsetsRequested值会重新等于false。
            if (mLayoutRequested) {
                windowSizeMayChange |= measureHierarchy(host, lp,  //优化布局，前面跟踪分析过
                
                        mView.getContext().getResources(),
                        desiredWindowWidth, desiredWindowHeight); 
            }
        }

        if (layoutRequested) {
            mLayoutRequested = false;
        }
        boolean windowShouldResize = layoutRequested && windowSizeMayChange  // 窗口是否需要重新设置大小
            && ((mWidth != host.getMeasuredWidth() || mHeight != host.getMeasuredHeight())
                || (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.width() < desiredWindowWidth && frame.width() != mWidth)
                || (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT &&
                        frame.height() < desiredWindowHeight && frame.height() != mHeight));
                        
        windowShouldResize |= mDragResizing && mResizeMode == RESIZE_MODE_FREEFORM;
        // If the activity was just relaunched, it might have unfrozen the task bounds (while
        // relaunching), so we need to force a call into window manager to pick up the latest
        // bounds.
        windowShouldResize |= mActivityRelaunched; //可能activity刚刚启动，需要重新获取一下，保证最新

        final boolean computesInternalInsets =  // 是否有内部计算监听器
                mAttachInfo.mTreeObserver.hasComputeInternalInsetsListeners()
                || mAttachInfo.mHasNonEmptyGivenInternalInsets;

        boolean insetsPending = false;
        int relayoutResult = 0;
        boolean updatedConfiguration = false;

        final int surfaceGenerationId = mSurface.getGenerationId();

        final boolean isViewVisible = viewVisibility == View.VISIBLE;
        final boolean windowRelayoutWasForced = mForceNextWindowRelayout;
        boolean surfaceSizeChanged = false;
        boolean surfaceCreated = false;
        boolean surfaceDestroyed = false;
        /* True if surface generation id changes. */
        boolean surfaceReplaced = false;

        final boolean windowAttributesChanged = mWindowAttributesChanged; // 窗口属性是否改变
        if (windowAttributesChanged) {
            mWindowAttributesChanged = false;
            params = lp;
        }
        if (params != null) {
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0
                    && !PixelFormat.formatHasAlpha(params.format)) {
                params.format = PixelFormat.TRANSLUCENT;
            }
            adjustLayoutParamsForCompatibility(params);
            controlInsetsForCompatibility(params);
        }
        
        // 如果是 第一次绘制||窗口需要调整大小||view可见性发生变化||重新计算过窗口大小||窗口属性发生改变||下次绘制需要重新布局
        if (mFirst || windowShouldResize || viewVisibilityChanged || cutoutChanged || params != null
                || mForceNextWindowRelayout) {
            mForceNextWindowRelayout = false;

            if (isViewVisible) { //如果这个View是可见的，
                // If this window is giving internal insets to the window
                // manager, and it is being added or changing its visibility,
                // then we want to first give the window manager "fake"
                // insets to cause it to effectively ignore the content of
                // the window during layout.  This avoids it briefly causing
                // other windows to resize/move based on the raw frame of the
                // window, waiting until we can finish laying out this window
                // and get back to the window manager with the ultimately
                // computed insets.
                insetsPending = computesInternalInsets && (mFirst || viewVisibilityChanged);
            }

            if (mSurfaceHolder != null) {
                mSurfaceHolder.mSurfaceLock.lock();
                mDrawingAllowed = true;
            }

            boolean hwInitialized = false;
            boolean dispatchApplyInsets = false;
            boolean hadSurface = mSurface.isValid();

            try {
                if (mAttachInfo.mThreadedRenderer != null) {
                    // relayoutWindow may decide to destroy mSurface. As that decision
                    // happens in WindowManager service, we need to be defensive here
                    // and stop using the surface in case it gets destroyed.
                    if (mAttachInfo.mThreadedRenderer.pause()) {
                        // Animations were running so we need to push a frame
                        // to resume them
                        mDirty.set(0, 0, mWidth, mHeight);
                    }
                    mChoreographer.mFrameInfo.addFlags(FrameInfo.FLAG_WINDOW_LAYOUT_CHANGED);
                }
                relayoutResult = relayoutWindow(params, viewVisibility, insetsPending); //计算窗口大小

                // If the pending {@link MergedConfiguration} handed back from
                // {@link #relayoutWindow} does not match the one last reported,
                // WindowManagerService has reported back a frame from a configuration not yet
                // handled by the client. In this case, we need to accept the configuration so we
                // do not lay out and draw with the wrong configuration.
                
                // 与之前保存的测量大小比较
                if (!mPendingMergedConfiguration.equals(mLastReportedMergedConfiguration)) {
                    performConfigurationChange(mPendingMergedConfiguration, !mFirst,INVALID_DISPLAY /* same display */);
                    updatedConfiguration = true;
                }
                cutoutChanged = !mPendingDisplayCutout.equals(mAttachInfo.mDisplayCutout);
                surfaceSizeChanged = (relayoutResult
                      & WindowManagerGlobal.RELAYOUT_RES_SURFACE_RESIZED) != 0;
                final boolean alwaysConsumeSystemBarsChanged =
                        mPendingAlwaysConsumeSystemBars != mAttachInfo.mAlwaysConsumeSystemBars;
                final boolean colorModeChanged = hasColorModeChanged(lp.getColorMode());
                surfaceCreated = !hadSurface && mSurface.isValid();
                surfaceDestroyed = hadSurface && !mSurface.isValid();
                surfaceReplaced = (surfaceGenerationId != mSurface.getGenerationId())
                        && mSurface.isValid();

                if (cutoutChanged) {//窗口大小发生过改变，重新保存一个新的值
                    mAttachInfo.mDisplayCutout.set(mPendingDisplayCutout);
                    // Need to relayout with content insets. 需要重新布局内容
                    dispatchApplyInsets = true;
                }
                if (alwaysConsumeSystemBarsChanged) {
                    mAttachInfo.mAlwaysConsumeSystemBars = mPendingAlwaysConsumeSystemBars;
                    dispatchApplyInsets = true;
                }
                if (updateCaptionInsets()) { // 更新完插入内容
                    dispatchApplyInsets = true;
                }
                if (dispatchApplyInsets || mLastSystemUiVisibility !=
                        mAttachInfo.mSystemUiVisibility || mApplyInsetsRequested) {
                    mLastSystemUiVisibility = mAttachInfo.mSystemUiVisibility;
                    dispatchApplyInsets(host); // 分发应用内容物
                    // We applied insets so force contentInsetsChanged to ensure the
                    // hierarchy is measured below.
                    dispatchApplyInsets = true;
                }
                if (colorModeChanged && mAttachInfo.mThreadedRenderer != null) {
                    mAttachInfo.mThreadedRenderer.setWideGamut(
                            lp.getColorMode() == ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT);
                }

                if (surfaceCreated) { // 创建surface，用来重新绘制
                    mFullRedrawNeeded = true;
                    mPreviousTransparentRegion.setEmpty();

                    if (mAttachInfo.mThreadedRenderer != null) { // 预初始化
                        try {
                            hwInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
                            if (hwInitialized && (host.mPrivateFlags
                                            & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) == 0) {
                                mAttachInfo.mThreadedRenderer.allocateBuffers();
                            }
                        } catch (OutOfResourcesException e) {
                            handleOutOfResourcesException(e);
                            return;
                        }
                    }
                    notifySurfaceCreated();
                } else if (surfaceDestroyed) {
                    // If the surface has been removed, then reset the scroll
                    // positions.
                    if (mLastScrolledFocus != null) {
                        mLastScrolledFocus.clear();
                    }
                    mScrollY = mCurScrollY = 0;
                    if (mView instanceof RootViewSurfaceTaker) {
                        ((RootViewSurfaceTaker) mView).onRootViewScrollYChanged(mCurScrollY);
                    }
                    if (mScroller != null) {
                        mScroller.abortAnimation();
                    }
                    // Our surface is gone
                    if (mAttachInfo.mThreadedRenderer != null &&
                            mAttachInfo.mThreadedRenderer.isEnabled()) {
                        mAttachInfo.mThreadedRenderer.destroy();
                    }
                } else if ((surfaceReplaced
                        || surfaceSizeChanged || windowRelayoutWasForced || colorModeChanged)
                        && mSurfaceHolder == null
                        && mAttachInfo.mThreadedRenderer != null
                        && mSurface.isValid()) {
                    mFullRedrawNeeded = true;
                    try {
                        // Need to do updateSurface (which leads to CanvasContext::setSurface and
                        // re-create the EGLSurface) if either the Surface changed (as indicated by
                        // generation id), or WindowManager changed the surface size. The latter is
                        // because on some chips, changing the consumer side's BufferQueue size may
                        // not take effect immediately unless we create a new EGLSurface.
                        // Note that frame size change doesn't always imply surface size change (eg.
                        // drag resizing uses fullscreen surface), need to check surfaceSizeChanged
                        // flag from WindowManager.
                        mAttachInfo.mThreadedRenderer.updateSurface(mSurface); // 更新
                    } catch (OutOfResourcesException e) {
                        handleOutOfResourcesException(e);
                        return;
                    }
                }

                if (!surfaceCreated && surfaceReplaced) {
                    notifySurfaceReplaced();
                }

                final boolean freeformResizing = (relayoutResult
                        & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_FREEFORM) != 0;
                final boolean dockedResizing = (relayoutResult
                        & WindowManagerGlobal.RELAYOUT_RES_DRAG_RESIZING_DOCKED) != 0;
                final boolean dragResizing = freeformResizing || dockedResizing;
                if (mDragResizing != dragResizing) {
                    if (dragResizing) {
                        mResizeMode = freeformResizing
                                ? RESIZE_MODE_FREEFORM
                                : RESIZE_MODE_DOCKED_DIVIDER;
                        final boolean backdropSizeMatchesFrame =
                                mWinFrame.width() == mPendingBackDropFrame.width()
                                        && mWinFrame.height() == mPendingBackDropFrame.height();
                        // TODO: Need cutout?
                        startDragResizing(mPendingBackDropFrame, !backdropSizeMatchesFrame,
                                mLastWindowInsets.getSystemWindowInsets().toRect(),
                                mLastWindowInsets.getStableInsets().toRect(), mResizeMode);
                    } else {
                        // We shouldn't come here, but if we come we should end the resize.
                        endDragResizing();
                    }
                }
                if (!mUseMTRenderer) { // 不使用使用MT渲染
                    if (dragResizing) {
                        mCanvasOffsetX = mWinFrame.left;
                        mCanvasOffsetY = mWinFrame.top;
                    } else {
                        mCanvasOffsetX = mCanvasOffsetY = 0;
                    }
                }
            } catch (RemoteException e) {
            }

            mAttachInfo.mWindowLeft = frame.left;
            mAttachInfo.mWindowTop = frame.top;

            // !!FIXME!! This next section handles the case where we did not get the
            // window size we asked for. We should avoid this by getting a maximum size from
            // the window session beforehand.
            if (mWidth != frame.width() || mHeight != frame.height()) { // 重新校准窗口大小
                mWidth = frame.width();
                mHeight = frame.height();
            }
            if (mSurfaceHolder != null) {
                // The app owns the surface; tell it about what is going on.
                if (mSurface.isValid()) { //可用
                    // XXX .copyFrom() doesn't work!
                    //mSurfaceHolder.mSurface.copyFrom(mSurface);
                    mSurfaceHolder.mSurface = mSurface;
                }
                mSurfaceHolder.setSurfaceFrameSize(mWidth, mHeight); // 设置表层大小
                mSurfaceHolder.mSurfaceLock.unlock();
                if (surfaceCreated) { // 调用底层资源工作
                    mSurfaceHolder.ungetCallbacks();
                    mIsCreating = true;
                    SurfaceHolder.Callback[] callbacks = mSurfaceHolder.getCallbacks();
                    if (callbacks != null) {
                        for (SurfaceHolder.Callback c : callbacks) {
                            c.surfaceCreated(mSurfaceHolder);
                        }
                    }
                }
                if ((surfaceCreated || surfaceReplaced || surfaceSizeChanged
                        || windowAttributesChanged) && mSurface.isValid()) {
                    SurfaceHolder.Callback[] callbacks = mSurfaceHolder.getCallbacks();
                    if (callbacks != null) {
                        for (SurfaceHolder.Callback c : callbacks) {
                            c.surfaceChanged(mSurfaceHolder, lp.format,
                                    mWidth, mHeight);
                        }
                    }
                    mIsCreating = false;
                }
                if (surfaceDestroyed) { // 表面被销毁，通知相关结束绘制，释放资源
                    notifyHolderSurfaceDestroyed();
                    mSurfaceHolder.mSurfaceLock.lock();
                    try {
                        mSurfaceHolder.mSurface = new Surface();
                    } finally {
                        mSurfaceHolder.mSurfaceLock.unlock();
                    }
                }
            }
            final ThreadedRenderer threadedRenderer = mAttachInfo.mThreadedRenderer; // 渲染线程
            if (threadedRenderer != null && threadedRenderer.isEnabled()) {
                if (hwInitialized
                        || mWidth != threadedRenderer.getWidth()
                        || mHeight != threadedRenderer.getHeight()
                        || mNeedsRendererSetup) {
                    threadedRenderer.setup(mWidth, mHeight, mAttachInfo, // 设置渲染器
                            mWindowAttributes.surfaceInsets);
                    mNeedsRendererSetup = false;
                }
            }
            if (!mStopped || mReportNextDraw) { // 没有停止绘制或者需要再次绘制
                boolean focusChangedDueToTouchMode = ensureTouchModeLocally( // 确保触摸已经被设置
                        (relayoutResult&WindowManagerGlobal.RELAYOUT_RES_IN_TOUCH_MODE) != 0);
                if (focusChangedDueToTouchMode || mWidth != host.getMeasuredWidth()
                        || mHeight != host.getMeasuredHeight() || dispatchApplyInsets ||
                        updatedConfiguration) {
                    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
                    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);

                     // Ask host how big it wants to be 使用校准后的大小再次测量
                    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);

                    // Implementation of weights from WindowManager.LayoutParams
                    // We just grow the dimensions as needed and re-measure if
                    // needs be
                    int width = host.getMeasuredWidth();
                    int height = host.getMeasuredHeight();
                    boolean measureAgain = false;

                    if (lp.horizontalWeight > 0.0f) { // 水平方向权重
                        width += (int) ((mWidth - width) * lp.horizontalWeight);
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width,
                                   MeasureSpec.EXACTLY);
                        measureAgain = true; //需要再测量一次
                    }
                    if (lp.verticalWeight > 0.0f) { // 垂直方向权重
                        height += (int) ((mHeight - height) * lp.verticalWeight);
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height,
                                MeasureSpec.EXACTLY);
                        measureAgain = true;
                    }

                    if (measureAgain) { // 再测量
                        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
                    }
                    layoutRequested = true; // 测量结束，需要重新布局
                }
            }
        } else {
            // Not the first pass and no window/insets/visibility change but the window
            // may have moved and we need check that and if so to update the left and right
            // in the attach info. We translate only the window frame since on window move
            // the window manager tells us only for the new frame but the insets are the
            // same and we do not want to translate them more than once.
            maybeHandleWindowMove(frame); // 检查窗口是否因为添加内容产生偏移，继而执行偏移动画
        }

        if (surfaceSizeChanged || surfaceReplaced || surfaceCreated || windowAttributesChanged) {
            // 如果表面大小改变，发生替换，重建，属性改变，需要重新更新边界表面
            updateBoundsLayer(surfaceReplaced);
        }

        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        boolean triggerGlobalLayoutListener = didLayout
                || mAttachInfo.mRecomputeGlobalAttributes;
        if (didLayout) {
            performLayout(lp, mWidth, mHeight); // 关键点之一，会触发onLayout()的回调。
            // 。。省略代码。。
            // 至此，所有视图的大小和位置都已经确定,可以开始计算透明区域
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
               // start out transparent
               host.getLocationInWindow(mTmpLocation); // 计算透明区域位置
               mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1],
                  mTmpLocation[0] + host.mRight - host.mLeft,
                  mTmpLocation[1] + host.mBottom - host.mTop);
               
               // 获取透明区域   
               host.gatherTransparentRegion(mTransparentRegion);
               if (mTranslator != null) { // 设置到屏幕上
                  mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
               }
               if (!mTransparentRegion.equals(mPreviousTransparentRegion)) { // 实际设置的透明区域与预估透明区域存在误差
                   mPreviousTransparentRegion.set(mTransparentRegion);
                   mFullRedrawNeeded = true;  // 重新绘制
                   // reconfigure window manager 重新配置window管理器
                   try {
                       mWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
                   } catch (RemoteException e) {
                   }
               }
            }
        }    
```

第二部分(从方法最开始到measureHierarchy()归为第一部分：主要内容就是测量)主要是对第一部分测量结果确认校准，利用底层创建surface，准备绘制线程，执行layout
等操作。要看具体的布局流程，到performLayout()方法:
```text
    private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,int desiredWindowHeight) {
        mScrollMayChange = true;
        mInLayout = true;
        final View host = mView;
        if (host == null) {
            return;
        }
        try { // 这里的host说过是DoctorView，但是DoctorView没有layout()方法，最终会到View.layout()
            host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
            mInLayout = false;
            // 处理在布局过程中，如果有请求重新布局，那么需要执行一个完整的请求测量、布局
            int numViewsRequestingLayout = mLayoutRequesters.size();
            if (numViewsRequestingLayout > 0) {
                ArrayList<View> validLayoutRequesters = getValidLayoutRequesters(mLayoutRequesters,false);
                if (validLayoutRequesters != null) {
                    mHandlingLayoutInLayoutRequest = true;
                    // 处理新的布局请求，然后测量和布局
                    int numValidRequests = validLayoutRequesters.size();
                    for (int i = 0; i < numValidRequests; ++i) {
                       final View view = validLayoutRequesters.get(i);
                       view.requestLayout();
                    }
                    // 测量
                    measureHierarchy(host, lp, mView.getContext().getResources(),
                            desiredWindowWidth, desiredWindowHeight);
                    mInLayout = true;
                    //布局
                    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
                    mHandlingLayoutInLayoutRequest = false;

                    // Check the valid requests again, this time without checking/clearing the
                    // layout flags, since requests happening during the second pass get noop'd
                    // 第二次检查有效的重新布局请求
                    validLayoutRequesters = getValidLayoutRequesters(mLayoutRequesters, true);
                    if (validLayoutRequesters != null) {
                        final ArrayList<View> finalRequesters = validLayoutRequesters;
                        // Post second-pass requests to the next frame
                        getRunQueue().post(new Runnable() {
                            @Override
                            public void run() {
                                int numValidRequests = finalRequesters.size();
                                for (int i = 0; i < numValidRequests; ++i) {
                                    final View view = finalRequesters.get(i);
                                    // 重新布局
                                    view.requestLayout();
                                }
                            }
                        });
                    }
                }
            }
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }
        mInLayout = false;
    }
```
View#layout()方法如下:
```text
 public void layout(int l, int t, int r, int b) {
        if ((mPrivateFlags3 & PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT) != 0) { //先判断是否测量完毕，否则会先去执行测量
            onMeasure(mOldWidthMeasureSpec, mOldHeightMeasureSpec);
            mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
        }
        int oldL = mLeft;
        int oldT = mTop;
        int oldB = mBottom;
        int oldR = mRight;
        boolean changed = isLayoutModeOptical(mParent) ? // 是否为ViewGroup并且使用LAYOUT_MODE_OPTICAL_BOUNDS的布局模式
                setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
        if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
            onLayout(changed, l, t, r, b);// 触发回调，也就是自定义View中会重写的onLayout()方法
            if (shouldDrawRoundScrollbar()) { //是否绘制在圆形移动设备上
                if(mRoundScrollbarRenderer == null) {
                    mRoundScrollbarRenderer = new RoundScrollbarRenderer(this);
                }
            } else {
                mRoundScrollbarRenderer = null;
            }
            mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
            ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnLayoutChangeListeners != null) {
                ArrayList<OnLayoutChangeListener> listenersCopy =
                        (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
                int numListeners = listenersCopy.size();
                for (int i = 0; i < numListeners; ++i) {
                    listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB); // 触发另一个回调onLayoutChange()
                }
            }
        }
        final boolean wasLayoutValid = isLayoutValid();
        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
        mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;

        if (!wasLayoutValid && isFocused()) { // 如果这个view无法布局(已经完成了或者脱离父布局)并且还持有焦点
            mPrivateFlags &= ~PFLAG_WANTS_FOCUS;
            if (canTakeFocus()) {
                clearParentsWantFocus(); //清除父布局焦点
            } else if (getViewRootImpl() == null || !getViewRootImpl().isInLayout()) {
                clearFocusInternal(null, /* propagate */ true, /* refocus */ false); // 清除子布局的焦点
                clearParentsWantFocus(); //清除父布局焦点
            } else if (!hasParentWantsFocus()) {
                // original requestFocus was likely on this view directly, so just clear focus
                clearFocusInternal(null, /* propagate */ true, /* refocus */ false);
            }          
        } else if ((mPrivateFlags & PFLAG_WANTS_FOCUS) != 0) {
            mPrivateFlags &= ~PFLAG_WANTS_FOCUS;
            View focused = findFocus();
            if (focused != null) {
                if (!restoreDefaultFocus() && !hasParentWantsFocus()) {
                    focused.clearFocusInternal(null, /* propagate */ true, /* refocus */ false);
                }
            }
        }
        if ((mPrivateFlags3 & PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT) != 0) {
            mPrivateFlags3 &= ~PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT;
            notifyEnterOrExitForAutoFillIfNeeded(true);
        }
        notifyAppearedOrDisappearedForContentCaptureIfNeeded(true);
    }
```
View#layout()做了2个事，1是将回调onLayout()给DecorView,onLayoutChange()；2是处理焦点问题。    
看到DecorView的onLayout()方法
```text
protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mApplyFloatingVerticalInsets) { //若有垂直方向的偏移
            offsetTopAndBottom(mFloatingInsets.top);
        }
        if (mApplyFloatingHorizontalInsets) {//水平方向的偏移
            offsetLeftAndRight(mFloatingInsets.left);
        }
        updateElevation(); //更新阴影设置
        mAllowUpdateElevation = true;

        if (changed
                && (mResizeMode == RESIZE_MODE_DOCKED_DIVIDER
                    || mDrawLegacyNavigationBarBackground)) {
            getViewRootImpl().requestInvalidateRootRenderNode();
        }
    }
```
DecorView的有super.onLayout(),会先执行父类的OnLayout(),看到FrameLayout的onLayout()。在FrameLayout#onLayout()会间接调用下面这个方法：
```text
void layoutChildren(int left, int top, int right, int bottom, boolean forceLeftGravity) {
        final int count = getChildCount(); // 获取子View的数量。
        // 计算padding值
        final int parentLeft = getPaddingLeftWithForeground();
        final int parentRight = right - left - getPaddingRightWithForeground();
        final int parentTop = getPaddingTopWithForeground();
        final int parentBottom = bottom - top - getPaddingBottomWithForeground();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();
                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }
                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;
                // 根据方向子View的实际边界,上下左右实际的位置
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL: // 水平居中
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT: // 居右
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                            break;
                        }
                    case Gravity.LEFT: // 默认居左
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }
                switch (verticalGravity) {
                    case Gravity.TOP: // 顶部
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL: //垂直居中
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM: // 底部
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:  // 默认顶部
                        childTop = parentTop + lp.topMargin;
                }
                // 调用子View的layout方法
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }
```
FrameLayout#onLayout()会对子view分别根据它们的padding、方向计算它们的实际边界，然后再调用子View的layout方法，完成所有view的遍历。   
FrameLayout的onLayout()就结束后，回到DecorView的onLayout()方法，设置垂直，水平方向偏移等。随后继续回到View.layout(),触发onLayoutChange监听器，处
理焦点等问题后方法结束。返回ViewRootImpl#performLayout()。(部分代码在方法中已经添加注释说明，可看到performLayout()方法)。整个performLayout()方法在
执行完onLayout()的逻辑，接着看是否有布局请求需要处理，有的话需要测量，布局等一套完整流程。

ViewRootImpl#performLayout()方法结束，流程又回到performTraversals(),前面分了2个部分，第一部分是测量，第二部分是校准以及布局，现在看第三部分：
```text
       if (surfaceDestroyed) {
          notifySurfaceDestroyed();
       }
       if (triggerGlobalLayoutListener) { //通知已注册的侦听器发生全局布局
          mAttachInfo.mRecomputeGlobalAttributes = false;
          mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
       }
       if (computesInternalInsets) { // 计算内部插图(只一些小控件之类的view)
          final ViewTreeObserver.InternalInsetsInfo insets = mAttachInfo.mGivenInternalInsets;
          insets.reset();

          // Compute new insets in place.
          mAttachInfo.mTreeObserver.dispatchOnComputeInternalInsets(insets);
          mAttachInfo.mHasNonEmptyGivenInternalInsets = !insets.isEmpty();

          // Tell the window manager.
          if (insetsPending || !mLastGivenInsets.equals(insets)) {
              mLastGivenInsets.set(insets);

              // Translate insets to screen coordinates if needed.
              final Rect contentInsets;
              final Rect visibleInsets;
              final Region touchableRegion;
              if (mTranslator != null) {
                  contentInsets = mTranslator.getTranslatedContentInsets(insets.contentInsets);
                  visibleInsets = mTranslator.getTranslatedVisibleInsets(insets.visibleInsets);
                  touchableRegion = mTranslator.getTranslatedTouchableArea(insets.touchableRegion);
              } else {
                  contentInsets = insets.contentInsets;
                  visibleInsets = insets.visibleInsets;
                  touchableRegion = insets.touchableRegion;
              }
              try {
                  mWindowSession.setInsets(mWindow, insets.mTouchableInsets,
                          contentInsets, visibleInsets, touchableRegion);
              } catch (RemoteException e) {
              }
           }
       }

       if (mFirst) {
          if (sAlwaysAssignFocus || !isInTouchMode()) {
          
             if (mView != null) {
               if (!mView.hasFocus()) {
                   mView.restoreDefaultFocus(); // 恢复默认焦点
               }
             } else {
               if (DEBUG_INPUT_RESIZE) {
                   Log.v(mTag, "First: existing focused view=" + mView.findFocus());
               }
             }
          }
       } else {
         View focused = mView.findFocus();
         if (focused instanceof ViewGroup
               && ((ViewGroup) focused).getDescendantFocusability()
                       == ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                  focused.restoreDefaultFocus();
               }
         }
       }
       // 可见性是否变化
       final boolean changedVisibility = (viewVisibilityChanged || mFirst) && isViewVisible;
       // 是否有窗口焦点
       final boolean hasWindowFocus = mAttachInfo.mHasWindowFocus && isViewVisible;
       // 是否恢复焦点
       final boolean regainedFocus = hasWindowFocus && mLostWindowFocus;
       if (regainedFocus) {
          mLostWindowFocus = false;
       } else if (!hasWindowFocus && mHadWindowFocus) {
          mLostWindowFocus = true;
       }
       if (changedVisibility || regainedFocus) {
          // Toasts are presented as notifications - don't present them as windows as well
          boolean isToast = (mWindowAttributes == null) ? false
           : (mWindowAttributes.type == TYPE_TOAST);
          if (!isToast) {
              host.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
          }
       }

       mFirst = false;
       mWillDrawSoon = false;
       mNewSurfaceNeeded = false;
       mActivityRelaunched = false;
       mViewVisibility = viewVisibility;
       mHadWindowFocus = hasWindowFocus;

       mImeFocusController.onTraversal(hasWindowFocus, mWindowAttributes);

       // Remember if we must report the next draw.
       if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_FIRST_TIME) != 0) {
         reportNextDraw(); // 延迟绘制完成
       }
       if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_BLAST_SYNC) != 0) {
          reportNextDraw();
          setUseBLASTSyncTransaction(); // 设置之后，会将缓冲区重定向到事务中
          mSendNextFrameToWm = true;
       }
       
       // 取消绘制
       boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw() || !isViewVisible;

       if (!cancelDraw) {
          if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
              for (int i = 0; i < mPendingTransitions.size(); ++i) {
                  mPendingTransitions.get(i).startChangingAnimations();
              }
              mPendingTransitions.clear();
          }
          performDraw(); // 触发onDraw()回调
       } else {
          if (isViewVisible) {
             // Try again
             scheduleTraversals();
          } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
              for (int i = 0; i < mPendingTransitions.size(); ++i) {
                 mPendingTransitions.get(i).endChangingAnimations(); // 结束动画
              }
              mPendingTransitions.clear();
          }
       }
       if (mAttachInfo.mContentCaptureEvents != null) {
         notifyContentCatpureEvents();
       }
       mIsInTraversal = false;  // 全局变量，遍历结束标志。
     }
```
performTraversals()方法的最后一部分主要是onDraw()，同时draw之前还夹杂了焦点的处理。看到performDraw()方法：   
ViewRootImpl#performDraw():
```text
    private void performDraw() { // 当前绘制完毕并且不需要下一次绘制，或者view等于null，就返回。不再处理draw。
        if (mAttachInfo.mDisplayState == Display.STATE_OFF && !mReportNextDraw) {
            return;
        } else if (mView == null) {
            return;
        }

        final boolean fullRedrawNeeded = mFullRedrawNeeded || mReportNextDraw;
        mFullRedrawNeeded = false;
        // 正在绘制标志。
        mIsDrawing = true;
        boolean usingAsyncReport = false;
        boolean reportNextDraw = mReportNextDraw;
        
        // 渲染线程不为空并且可用的情况下，获取全局视图树观察者的回调，并且在需要执行的时候，借助handler发送到队列执行。
        if (mAttachInfo.mThreadedRenderer != null && mAttachInfo.mThreadedRenderer.isEnabled()) {
            ArrayList<Runnable> commitCallbacks = mAttachInfo.mTreeObserver
                    .captureFrameCommitCallbacks();
            final boolean needFrameCompleteCallback = mNextDrawUseBLASTSyncTransaction ||
                (commitCallbacks != null && commitCallbacks.size() > 0) ||
                mReportNextDraw;
            usingAsyncReport = mReportNextDraw;
            if (needFrameCompleteCallback) {
                final Handler handler = mAttachInfo.mHandler;
                mAttachInfo.mThreadedRenderer.setFrameCompleteCallback((long frameNr) -> {
                        finishBLASTSync(!mSendNextFrameToWm);
                        handler.postAtFrontOfQueue(() -> {
                            if (reportNextDraw) {
                                // TODO: Use the frame number
                                pendingDrawFinished();
                            }
                            if (commitCallbacks != null) {
                                for (int i = 0; i < commitCallbacks.size(); i++) {
                                    commitCallbacks.get(i).run();
                                }
                            }
                        });});
            }
        }

        try {
            if (mNextDrawUseBLASTSyncTransaction) { // 开启事务提交，使用BLAST同步方式
                // We aren't prepared to handle overlapping use of mRtBLASTSyncTransaction
                // so if we are BLAST syncing we make sure the previous draw has
                // totally finished.
                if (mAttachInfo.mThreadedRenderer != null) { // 确保前一个View已经绘制完成
                    mAttachInfo.mThreadedRenderer.pause();
                }

                mNextReportConsumeBLAST = true;
                mNextDrawUseBLASTSyncTransaction = false;

                if (mBlastBufferQueue != null) {
                    mBlastBufferQueue.setNextTransaction(mRtBLASTSyncTransaction);
                }
            }
            boolean canUseAsync = draw(fullRedrawNeeded); // 注意关键方法draw()。
            if (usingAsyncReport && !canUseAsync) {
                mAttachInfo.mThreadedRenderer.setFrameCompleteCallback(null);
                usingAsyncReport = false;
                finishBLASTSync(true /* apply */);
            }
        } finally {
            mIsDrawing = false;
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }

        // 清除相关动画
        if (mAttachInfo.mPendingAnimatingRenderNodes != null) {
            final int count = mAttachInfo.mPendingAnimatingRenderNodes.size();
            for (int i = 0; i < count; i++) {
                mAttachInfo.mPendingAnimatingRenderNodes.get(i).endAllAnimators();
            }
            mAttachInfo.mPendingAnimatingRenderNodes.clear();
        }

        if (mReportNextDraw) {//
            mReportNextDraw = false;

            // 如果使用多线程渲染器，那么等待窗口框架绘制
            if (mWindowDrawCountDown != null) {
                try {
                    mWindowDrawCountDown.await();
                } catch (InterruptedException e) {
                    Log.e(mTag, "Window redraw count down interrupted!");
                }
                mWindowDrawCountDown = null;
            }

            if (mAttachInfo.mThreadedRenderer != null) {
                mAttachInfo.mThreadedRenderer.setStopped(mStopped);
            }
            
            // surface相关
            if (mSurfaceHolder != null && mSurface.isValid()) {
                SurfaceCallbackHelper sch = new SurfaceCallbackHelper(this::postDrawFinished);
                SurfaceHolder.Callback callbacks[] = mSurfaceHolder.getCallbacks();

                sch.dispatchSurfaceRedrawNeededAsync(mSurfaceHolder, callbacks);
            } else if (!usingAsyncReport) {
                if (mAttachInfo.mThreadedRenderer != null) {
                    mAttachInfo.mThreadedRenderer.fence();
                }
                pendingDrawFinished(); // 预绘制结束。
            }
        }
        if (mPerformContentCapture) {
            performContentCaptureInitialReport();
        }
    }
```
看到ViewRootImpl#draw()方法:
```text
    private boolean draw(boolean fullRedrawNeeded) {
        Surface surface = mSurface;
        if (!surface.isValid()) {
            return false;
        }
        if (!sFirstDrawComplete) { // 第一个view绘制，执行完添加的Runnable，志辉执行一次。
            synchronized (sFirstDrawHandlers) {
                sFirstDrawComplete = true;
                final int count = sFirstDrawHandlers.size();
                for (int i = 0; i< count; i++) {
                    mHandler.post(sFirstDrawHandlers.get(i));
                }
            }
        }
        
        // 偏移到滚动或者有焦点的位置
        scrollToRectOrFocus(null, false);

        if (mAttachInfo.mViewScrollChanged) {
            mAttachInfo.mViewScrollChanged = false;
            mAttachInfo.mTreeObserver.dispatchOnScrollChanged(); // 分发滚动事件
        }
        
        // 是否执行动画
        boolean animating = mScroller != null && mScroller.computeScrollOffset();
        final int curScrollY;
        if (animating) {
            curScrollY = mScroller.getCurrY();
        } else {
            curScrollY = mScrollY;
        }
        if (mCurScrollY != curScrollY) {
            mCurScrollY = curScrollY;
            fullRedrawNeeded = true;
            if (mView instanceof RootViewSurfaceTaker) {
                ((RootViewSurfaceTaker) mView).onRootViewScrollYChanged(mCurScrollY);
            }
        }

        final float appScale = mAttachInfo.mApplicationScale;
        final boolean scalingRequired = mAttachInfo.mScalingRequired;

        final Rect dirty = mDirty;
        if (mSurfaceHolder != null) { // 不需要绘制。
            // The app owns the surface, we won't draw.
            dirty.setEmpty();
            if (animating && mScroller != null) {
                mScroller.abortAnimation();
            }
            return false;
        }

        if (fullRedrawNeeded) {
            dirty.set(0, 0, (int) (mWidth * appScale + 0.5f), (int) (mHeight * appScale + 0.5f));
        }
        mAttachInfo.mTreeObserver.dispatchOnDraw();

        int xOffset = -mCanvasOffsetX;
        int yOffset = -mCanvasOffsetY + curScrollY;
        final WindowManager.LayoutParams params = mWindowAttributes;
        final Rect surfaceInsets = params != null ? params.surfaceInsets : null;
        if (surfaceInsets != null) {
            xOffset -= surfaceInsets.left;
            yOffset -= surfaceInsets.top;

            // Offset dirty rect for surface insets.
            dirty.offset(surfaceInsets.left, surfaceInsets.right);
        }

        boolean accessibilityFocusDirty = false;
        final Drawable drawable = mAttachInfo.mAccessibilityFocusDrawable;
        if (drawable != null) {
            final Rect bounds = mAttachInfo.mTmpInvalRect;
            final boolean hasFocus = getAccessibilityFocusedRect(bounds);
            if (!hasFocus) {
                bounds.setEmpty();
            }
            if (!bounds.equals(drawable.getBounds())) {
                accessibilityFocusDirty = true;
            }
        }

        mAttachInfo.mDrawingTime =
                mChoreographer.getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;

        boolean useAsyncReport = false;
        if (!dirty.isEmpty() || mIsAnimating || accessibilityFocusDirty) {
            if (mAttachInfo.mThreadedRenderer != null && mAttachInfo.mThreadedRenderer.isEnabled()) {
                // If accessibility focus moved, always invalidate the root.
                boolean invalidateRoot = accessibilityFocusDirty || mInvalidateRootRequested;
                mInvalidateRootRequested = false;

                // Draw with hardware renderer. 使用硬件渲染绘制
                mIsAnimating = false;

                if (mHardwareYOffset != yOffset || mHardwareXOffset != xOffset) {
                    mHardwareYOffset = yOffset;
                    mHardwareXOffset = xOffset;
                    invalidateRoot = true;
                }

                if (invalidateRoot) { // 执行
                    mAttachInfo.mThreadedRenderer.invalidateRoot();
                }
                dirty.setEmpty();
                // Stage the content drawn size now. It will be transferred to the renderer
                // shortly before the draw commands get send to the renderer.
                final boolean updated = updateContentDrawBounds();
                if (mReportNextDraw) {
                    // report next draw overrides setStopped()
                    // This value is re-sync'd to the value of mStopped
                    // in the handling of mReportNextDraw post-draw.
                    mAttachInfo.mThreadedRenderer.setStopped(false);
                }
                if (updated) { // 更新窗口
                    requestDrawWindow();
                }
                useAsyncReport = true;
                //硬件渲染绘制。
                mAttachInfo.mThreadedRenderer.draw(mView, mAttachInfo, this);
            } else {
                // If we get here with a disabled & requested hardware renderer, something went
                // wrong (an invalidate posted right before we destroyed the hardware surface
                // for instance) so we should just bail out. Locking the surface with software
                // rendering at this point would lock it forever and prevent hardware renderer
                // from doing its job when it comes back.
                // Before we request a new frame we must however attempt to reinitiliaze the
                // hardware renderer if it's in requested state. This would happen after an
                // eglTerminate() for instance.
                if (mAttachInfo.mThreadedRenderer != null && // 如果渲染线程发生异常
                        !mAttachInfo.mThreadedRenderer.isEnabled() &&
                        mAttachInfo.mThreadedRenderer.isRequested() &&
                        mSurface.isValid()) {

                    try { // 重新初始化硬件渲染
                        mAttachInfo.mThreadedRenderer.initializeIfNeeded(
                                mWidth, mHeight, mAttachInfo, mSurface, surfaceInsets);
                    } catch (OutOfResourcesException e) {
                        handleOutOfResourcesException(e);
                        return false;
                    }

                    mFullRedrawNeeded = true;
                    scheduleTraversals(); // 重新开始绘制。
                    return false;
                }
                if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset, // 软件渲染来绘制。
                        scalingRequired, dirty, surfaceInsets)) {
                    return false;
                }
            }
        }
        if (animating) { // 执行了动画，要重新绘制
            mFullRedrawNeeded = true;
            scheduleTraversals();
        }
        return useAsyncReport;
    }
```
ViewRootImpl#drawSoftware()方法:
```text
private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
            boolean scalingRequired, Rect dirty, Rect surfaceInsets) {

        // Draw with software renderer.
        final Canvas canvas;
        
        // 加回来被设置的偏移量。
        int dirtyXOffset = xoff;
        int dirtyYOffset = yoff;
        if (surfaceInsets != null) {
            dirtyXOffset += surfaceInsets.left;
            dirtyYOffset += surfaceInsets.top;
        }
        try {
            dirty.offset(-dirtyXOffset, -dirtyYOffset);
            final int left = dirty.left;
            final int top = dirty.top;
            final int right = dirty.right;
            final int bottom = dirty.bottom;

            canvas = mSurface.lockCanvas(dirty);
            // TODO: Do this in native 在底层执行
            canvas.setDensity(mDensity); // 设置密度值
        } catch (Surface.OutOfResourcesException e) {
            handleOutOfResourcesException(e);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(mTag, "Could not lock surface", e);
            // Don't assume this is due to out of memory, it could be
            // something else, and if it is something else then we could
            // kill stuff (or ourself) for no reason.
            mLayoutRequested = true;    // ask wm for a new surface next time.
            return false;
        } finally {
            dirty.offset(dirtyXOffset, dirtyYOffset);  // Reset to the original value.
        }
        try {
            if (!canvas.isOpaque() || yoff != 0 || xoff != 0) { // view的背景是透明的，或者xy有偏移，要清除
                canvas.drawColor(0, PorterDuff.Mode.CLEAR); // 清除目标区域。
            }
            dirty.setEmpty();
            mIsAnimating = false;
            mView.mPrivateFlags |= View.PFLAG_DRAWN;

            canvas.translate(-xoff, -yoff);
            if (mTranslator != null) {
                mTranslator.translateCanvas(canvas);
            }
            canvas.setScreenDensity(scalingRequired ? mNoncompatDensity : 0);
            // 回到DecorView的draw()方法,关键点
            mView.draw(canvas);
            drawAccessibilityFocusedDrawableIfNeeded(canvas);
        } finally {
            try {
                surface.unlockCanvasAndPost(canvas); // 释放资源
            } catch (IllegalArgumentException e) {
                mLayoutRequested = true;    // ask wm for a new surface next time.
                return false;
            }
        }
        return true;
    }
```
上面方法主要看`mView.draw(canvas);`，其他都是一些准备工作什么的。还是要强调一下，这里的mView是指DecorView的实例，在DecorView的draw(canvas)方法有
super的调用，而DecorView的直接父类FrameLayout是没有这个方法，ViewGroup中也是没有这个方法，而是在View中有这个方法，所以要看到View的中的这个方法：
```text
// DecorView中的简单draw()方法
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mMenuBackground != null) {
            mMenuBackground.draw(canvas);
        }
    }
```
View中的draw()方法:
```html
    public void draw(Canvas canvas) {
        final int privateFlags = mPrivateFlags;
        mPrivateFlags = (privateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN; // 先处理标志位，表示在绘制这个view了。
        /*
         * Draw traversal performs several drawing steps which must be executed
         * in the appropriate order:
         *      1. Draw the background  
         *      2. If necessary, save the canvas' layers to prepare for fading
         *      3. Draw view's content
         *      4. Draw children
         *      5. If necessary, draw the fading edges and restore layers
         *      6. Draw decorations (scrollbars for instance)
         *      7. If necessary, draw the default focus highlight
         */

        // Step 1, draw the background, if needed
        int saveCount;
        drawBackground(canvas);  // 画背景

        // skip step 2 & 5 if possible (common case)
        final int viewFlags = mViewFlags;
        boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
        boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
        
        if (!verticalEdges && !horizontalEdges) { // 如果不需要保存(不需要褪色)，直接按照步骤即可，这种绘制速度是比较快的。
            // Step 3, draw the content
            onDraw(canvas); // 触发onDraw()的回调

            // Step 4, draw the children
            dispatchDraw(canvas);

            drawAutofilledHighlight(canvas);

            // Overlay is part of the content and draws beneath Foreground
            if (mOverlay != null && !mOverlay.isEmpty()) {
                mOverlay.getOverlayView().dispatchDraw(canvas);
            }

            // Step 6, draw decorations (foreground, scrollbars)
            onDrawForeground(canvas);

            // Step 7, draw the default focus highlight
            drawDefaultFocusHighlight(canvas);

            if (isShowingLayoutBounds()) {
                debugDrawFocus(canvas);
            }
            return;
        }
        
        // 同样也是绘制，相对速度会慢一点
        boolean drawTop = false;
        boolean drawBottom = false;
        boolean drawLeft = false;
        boolean drawRight = false;

        float topFadeStrength = 0.0f;
        float bottomFadeStrength = 0.0f;
        float leftFadeStrength = 0.0f;
        float rightFadeStrength = 0.0f;

        // Step 2, save the canvas' layers  // 保存画布
        int paddingLeft = mPaddingLeft;

        final boolean offsetRequired = isPaddingOffsetRequired();
        if (offsetRequired) {
            paddingLeft += getLeftPaddingOffset();
        }
        int left = mScrollX + paddingLeft;
        int right = left + mRight - mLeft - mPaddingRight - paddingLeft;
        int top = mScrollY + getFadeTop(offsetRequired);
        int bottom = top + getFadeHeight(offsetRequired);

        if (offsetRequired) {
            right += getRightPaddingOffset();
            bottom += getBottomPaddingOffset();
        }
        final ScrollabilityCache scrollabilityCache = mScrollCache;
        final float fadeHeight = scrollabilityCache.fadingEdgeLength;
        int length = (int) fadeHeight;

        // clip the fade length if top and bottom fades overlap
        // overlapping fades produce odd-looking artifacts
        if (verticalEdges && (top + length > bottom - length)) { // 裁剪垂直方向
            length = (bottom - top) / 2;
        }

        // also clip horizontal fades if necessary
        if (horizontalEdges && (left + length > right - length)) { // 裁剪水平方向
            length = (right - left) / 2;
        }

        if (verticalEdges) {
            topFadeStrength = Math.max(0.0f, Math.min(1.0f, getTopFadingEdgeStrength()));
            drawTop = topFadeStrength * fadeHeight > 1.0f;
            bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, getBottomFadingEdgeStrength()));
            drawBottom = bottomFadeStrength * fadeHeight > 1.0f;
        }
        if (horizontalEdges) {
            leftFadeStrength = Math.max(0.0f, Math.min(1.0f, getLeftFadingEdgeStrength()));
            drawLeft = leftFadeStrength * fadeHeight > 1.0f;
            rightFadeStrength = Math.max(0.0f, Math.min(1.0f, getRightFadingEdgeStrength()));
            drawRight = rightFadeStrength * fadeHeight > 1.0f;
        }

        saveCount = canvas.getSaveCount(); // 保存
        int topSaveCount = -1;
        int bottomSaveCount = -1;
        int leftSaveCount = -1;
        int rightSaveCount = -1;
        
        int solidColor = getSolidColor();
        if (solidColor == 0) {
            if (drawTop) {
                topSaveCount = canvas.saveUnclippedLayer(left, top, right, top + length);
            }
            if (drawBottom) {
                bottomSaveCount = canvas.saveUnclippedLayer(left, bottom - length, right, bottom);
            }
            if (drawLeft) {
                leftSaveCount = canvas.saveUnclippedLayer(left, top, left + length, bottom);
            }
            if (drawRight) {
                rightSaveCount = canvas.saveUnclippedLayer(right - length, top, right, bottom);
            }
        } else {
            scrollabilityCache.setFadeColor(solidColor);
        }

        // Step 3, draw the content
        onDraw(canvas);

        // Step 4, draw the children
        dispatchDraw(canvas);

        // Step 5, draw the fade effect and restore layers // 绘制淡出效果和恢复层
        final Paint p = scrollabilityCache.paint;
        final Matrix matrix = scrollabilityCache.matrix;
        final Shader fade = scrollabilityCache.shader;

        // must be restored in the reverse order that they were saved
        if (drawRight) { // 右边
            matrix.setScale(1, fadeHeight * rightFadeStrength);
            matrix.postRotate(90);
            matrix.postTranslate(right, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            if (solidColor == 0) {
                canvas.restoreUnclippedLayer(rightSaveCount, p);

            } else {
                canvas.drawRect(right - length, top, right, bottom, p);
            }
        }
        if (drawLeft) {
            matrix.setScale(1, fadeHeight * leftFadeStrength);
            matrix.postRotate(-90);
            matrix.postTranslate(left, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            if (solidColor == 0) {
                canvas.restoreUnclippedLayer(leftSaveCount, p);
            } else {
                canvas.drawRect(left, top, left + length, bottom, p);
            }
        }
        if (drawBottom) {
            matrix.setScale(1, fadeHeight * bottomFadeStrength);
            matrix.postRotate(180);
            matrix.postTranslate(left, bottom);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            if (solidColor == 0) {
                canvas.restoreUnclippedLayer(bottomSaveCount, p);
            } else {
                canvas.drawRect(left, bottom - length, right, bottom, p);
            }
        }
        if (drawTop) {
            matrix.setScale(1, fadeHeight * topFadeStrength);
            matrix.postTranslate(left, top);
            fade.setLocalMatrix(matrix);
            p.setShader(fade);
            if (solidColor == 0) {
                canvas.restoreUnclippedLayer(topSaveCount, p);
            } else {
                canvas.drawRect(left, top, right, top + length, p);
            }
        }
        canvas.restoreToCount(saveCount);
        
        drawAutofilledHighlight(canvas);

        // Overlay is part of the content and draws beneath Foreground
        if (mOverlay != null && !mOverlay.isEmpty()) {
            mOverlay.getOverlayView().dispatchDraw(canvas);
        }

        // Step 6, draw decorations (foreground, scrollbars)
        onDrawForeground(canvas);

        // Step 7, draw the default focus highlight
        drawDefaultFocusHighlight(canvas);

        if (isShowingLayoutBounds()) {
            debugDrawFocus(canvas);
        }
    }
```
整个方法总结下来就是这么几个步骤：
* 画背景
* 如果有必要，保存画布的图层，为褪色做准备
* 画视图的内容(回调onDraw()方法)
* 画子view (回调dispatchDraw()，不过这个方法一遍实现的少，像在一些常见类，ViewGroup等有实现)
* 如果有必要，绘制褪色边缘和恢复层
* 绘制装饰(例如滚动条)
* 如果有必要，绘制默认的焦点高亮

View中的draw()走完，回到DecorView的draw()，最后如果mMenuBackground不为空，就绘制这个背景到画布上,DecorView#draw(canvas)方法结束，现在进度回到
ViewRootImpl#drawSoftware()的最后阶段，执行完一个高亮显示后，释放surface，return true结束。进度来到ViewRootImpl#draw()，如果drawSoftware()期间
产生异常的会，最终结果是return false。这样也会导致ViewRootImpl#draw()直接退出，而不会执行后面的动画。draw()方法结束，进度来到ViewRootImpl#
performDraw()的这段代码段`boolean canUseAsync = draw(fullRedrawNeeded);`位置，继续执行后面逻辑，performDraw()执行完毕，回到performTraversals()
,发出绘制完成的消息到整个视图结构。performTraversals()结束。绘制流程结束。

总结一波view绘制设计的API调用链图：  
[](../图片/img_view绘制.jpg)