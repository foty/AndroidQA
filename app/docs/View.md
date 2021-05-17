#### View
* 加载(Window,DecorView...)
* 绘制
 
##### Window、PhoneWindow、DecorView
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
performLaunchActivity()之后会进入到Activity生命周期，体现就是走Activity#onCreate()方法。设置布局的入口在`setContentView(R.layout.activity_main);`:
看下setContentView():
```html
  @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
  }
```
这里看的源代码是android 11，对应api版本是30。不同版本的api实现可能有些不一样。`setContentView()`这里使用了委托，委托给AppCompatDelegate，AppCompatDelegate
是一个抽象类，实现在AppCompatDelegateImpl。看到这个类的`setContentView`:
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
installDecor()保证了mDecor不会为null，并且设置一些系统相关样式属性参数。`generateDecor(-1);`方法主要是先获取Context。是使用，然后new出DecorView对象。另外
`generateLayout(mDecor)`这里有一点要注意：
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
回到`createSubDecor()`。mWindow.getDecorView()执行完毕。继续往下执行。整个方法后面一部分核心还是inflate出一个View作为新的window容器，也就是subDecor。到
后面
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
前面第一个if else中`installDecor()`前面已经有看过，下面看到`FEATURE_CONTENT_TRANSITIONS`这个flag，大概翻译就是内容过渡标志。我的理解是类似activity的转场
动画。带有这个标志是内容中的某个控件或元素支持过渡动画。如果需要内容过渡则通过Scene添加view，否则直接添加view到mContentParent。在Scene也可以看到：
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
* id为android.R.id.content的View(mContentParent/contentView,在不同类中有不同的名称) DecorView中的一个子view，实质也是一个FrameLayout，在构建时可能
会被替换为ContentFrameLayout(也是继承FrameLayout)，但id不会被改变。开发中为activity设置的ContentView，就是它的子View。

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
tvHello是一个TextView，它的父布局是一个ConstraintLayout。再往上就是ContentFrameLayout，这表示DecorView中id=content的子view的是被替换过的。view的最
根父view是DecorView。  

View的绘制关键就3部分  
 onMeasure()  
 onSizeChanged()  
 onLayout()  
 onDraw()  

##### View的绘制
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
            mViews.add(view);
            mRoots.add(root);
            mParams.add(wparams);
            try {
                root.setView(view, wparams, panelParentView, userId);
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
                
                // Schedule the first layout -before- adding to the window
                // manager, to make sure we do the relayout before receiving
                // any other events from the system.
                requestLayout(); // 关注这一句即可。
                InputChannel inputChannel = null;
                // 。。。省略代码
            }
            。。。
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
`doTraversal()`会执行到`performTraversals()`,到这里才是真真正正的开始绘制。下面开始跟踪理解这个方法。(注：这个方法又大又长，会删除部分没意义的log，debug
日志，注释等)。
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
                // For wrap content, we have to remeasure later on anyways. Use size consistent with
                // below so we get best use of the measure cache.
                desiredWindowWidth = dipToPx(config.screenWidthDp); 
                desiredWindowHeight = dipToPx(config.screenHeightDp);
            } else {
                // After addToDisplay, the frame contains the frameHint from window manager, which
                // for most windows is going to be the same size as the result of relayoutWindow.
                // Using this here allows us to avoid remeasuring after relayoutWindow
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
measureHierarchy()方法更像是做了一个窗口大小优化工作，来保证这个view层级视图是最合适，最舒服的。同时会触发自定义控件中onMeasure()的回调。
```text
 private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
        if (mView == null) {
            return;
        }
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, "measure");
        try {
            mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);  // 注意了，这个mVeiw实际是DecorView。
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
总体上看`mView.measure(*,*)`这个方法主要还是做优化工作(一些详细看上面注释)，特别是针对测量规格，其中很重要的一点就是回触发onMeasure()回调。写过自定义View的
都知道，这是重要一环。把测量这一步骤交给开发者自己去决定。  
下面回到ViewRootImpl#measureHierarchy()方法，也就是接着执行完`performMeasure()`之后，再次判断测量结果是否是最后，最后返回表示窗口是否发生变化的boolean
结果。performMeasure()方法结束，流程重新回到performTraversals()
```
   
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
            // If we are in auto resize mode, then we need to determine
            // what mode to use now.
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
                // Short-circuit catching a new layout request here, so
                // we don't need to go through two layout passes when things
                // change due to fitting system windows, which can happen a lot.
                windowSizeMayChange |= measureHierarchy(host, lp,  //优化布局，前面跟踪分析过
                        mView.getContext().getResources(),
                        desiredWindowWidth, desiredWindowHeight); 
            }
        }

        if (layoutRequested) {
            // Clear this now, so that if anything requests a layout in the
            // rest of this function we will catch it and re-run a full
            // layout pass.
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

        // Determine whether to compute insets.
        // If there are no inset listeners remaining then we may still need to compute
        // insets in case the old insets were non-empty and must be reset.
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

            if (isViewVisible) {
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
                    // If we are creating a new surface, then we need to
                    // completely redraw it.
                    mFullRedrawNeeded = true;
                    mPreviousTransparentRegion.setEmpty();

                    // Only initialize up-front if transparent regions are not
                    // requested, otherwise defer to see if the entire window
                    // will be transparent
                    if (mAttachInfo.mThreadedRenderer != null) { // 预初始化
                        try {
                            hwInitialized = mAttachInfo.mThreadedRenderer.initialize(mSurface);
                            if (hwInitialized && (host.mPrivateFlags
                                            & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) == 0) {
                                // Don't pre-allocate if transparent regions
                                // are requested as they may not be needed
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
                        mAttachInfo.mThreadedRenderer.updateSurface(mSurface);
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
                mSurfaceHolder.setSurfaceFrameSize(mWidth, mHeight);
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
            // If the surface has been replaced, there's a chance the bounds layer is not parented
            // to the new layer. When updating bounds layer, also reparent to the main VRI
            // SurfaceControl to ensure it's correctly placed in the hierarchy.
            //
            // This needs to be done on the client side since WMS won't reparent the children to the
            // new surface if it thinks the app is closing. WMS gets the signal that the app is
            // stopping, but on the client side it doesn't get stopped since it's restarted quick
            // enough. WMS doesn't want to keep around old children since they will leak when the
            // client creates new children.
            updateBoundsLayer(surfaceReplaced); // 更新边界表面
        }

        final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
        boolean triggerGlobalLayoutListener = didLayout
                || mAttachInfo.mRecomputeGlobalAttributes;
        if (didLayout) {
            performLayout(lp, mWidth, mHeight); // 关键点之一，会触发onLayout()的回调。
```

```
            // By this point all views have been sized and positioned
            // We can compute the transparent area
            if ((host.mPrivateFlags & View.PFLAG_REQUEST_TRANSPARENT_REGIONS) != 0) {
                // start out transparent
                // TODO: AVOID THAT CALL BY CACHING THE RESULT?
                host.getLocationInWindow(mTmpLocation);
                mTransparentRegion.set(mTmpLocation[0], mTmpLocation[1],
                        mTmpLocation[0] + host.mRight - host.mLeft,
                        mTmpLocation[1] + host.mBottom - host.mTop);

                host.gatherTransparentRegion(mTransparentRegion);
                if (mTranslator != null) {
                    mTranslator.translateRegionInWindowToScreen(mTransparentRegion);
                }

                if (!mTransparentRegion.equals(mPreviousTransparentRegion)) {
                    mPreviousTransparentRegion.set(mTransparentRegion);
                    mFullRedrawNeeded = true;
                    // reconfigure window manager
                    try {
                        mWindowSession.setTransparentRegion(mWindow, mTransparentRegion);
                    } catch (RemoteException e) {
                    }
                }
            }

            if (DBG) {
                System.out.println("======================================");
                System.out.println("performTraversals -- after setFrame");
                host.debug();
            }
        }

        if (surfaceDestroyed) {
            notifySurfaceDestroyed();
        }

        if (triggerGlobalLayoutListener) {
            mAttachInfo.mRecomputeGlobalAttributes = false;
            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }

        if (computesInternalInsets) {
            // Clear the original insets.
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
                // handle first focus request
                if (DEBUG_INPUT_RESIZE) {
                    Log.v(mTag, "First: mView.hasFocus()=" + mView.hasFocus());
                }
                if (mView != null) {
                    if (!mView.hasFocus()) {
                        mView.restoreDefaultFocus();
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(mTag, "First: requested focused view=" + mView.findFocus());
                        }
                    } else {
                        if (DEBUG_INPUT_RESIZE) {
                            Log.v(mTag, "First: existing focused view=" + mView.findFocus());
                        }
                    }
                }
            } else {
                // Some views (like ScrollView) won't hand focus to descendants that aren't within
                // their viewport. Before layout, there's a good change these views are size 0
                // which means no children can get focus. After layout, this view now has size, but
                // is not guaranteed to hand-off focus to a focusable child (specifically, the edge-
                // case where the child has a size prior to layout and thus won't trigger
                // focusableViewAvailable).
                View focused = mView.findFocus();
                if (focused instanceof ViewGroup
                        && ((ViewGroup) focused).getDescendantFocusability()
                                == ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    focused.restoreDefaultFocus();
                }
            }
        }

        final boolean changedVisibility = (viewVisibilityChanged || mFirst) && isViewVisible;
        final boolean hasWindowFocus = mAttachInfo.mHasWindowFocus && isViewVisible;
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
            reportNextDraw();
        }
        if ((relayoutResult & WindowManagerGlobal.RELAYOUT_RES_BLAST_SYNC) != 0) {
            reportNextDraw();
            setUseBLASTSyncTransaction();
            mSendNextFrameToWm = true;
        }

        boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw() || !isViewVisible;

        if (!cancelDraw) {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).startChangingAnimations();
                }
                mPendingTransitions.clear();
            }
            performDraw();
        } else {
            if (isViewVisible) {
                // Try again
                scheduleTraversals();
            } else if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).endChangingAnimations();
                }
                mPendingTransitions.clear();
            }
        }

        if (mAttachInfo.mContentCaptureEvents != null) {
            notifyContentCatpureEvents();
        }
        mIsInTraversal = false;
    }
```
 