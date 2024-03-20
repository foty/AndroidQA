
###
* 流程   [](../../../android/事件分发.md)
* 处理事件冲突

##### 说说事件分发流程
> 事件从最顶层Activity开始、再到里面的ViewGroup、最终再到目标View依次传递。   
> 事件产生传递到Activity时，activity会对事件进行分发(调用dispatchTouchEvent()方法)，将事件传递到内部的ViewGroup中。
> ViewGroup判断是否需要拦截这个事件(onInterceptTouchEvent()方法)。如果选择拦截事件的话，那么事件将会进入到
> 这个ViewGroup的onTouchEvent()方法，由自己处理事件，并且往后所有事件都不会再往下传递。如果不拦截事件，会遍历所有子view，
> 直到将事件传递到目标View，进入到View的onTouchEvent()处理事件。如果ViewGroup或者最后的View都没有消费事件，
> 事件进到activity的onTouchEvent()方法，交给activity处理事件。


##### 说说事件冲突处理办法：
1、内部拦截法：
> 子View在dispatchTouchEvent方法中通过调用requestDisallowInterceptTouchEvent(true)方法，禁止父View拦截事件。
> 并在合适的场景将其置为 false允许拦截。

2、外部拦截法：
> 由父View通过重写onInterceptTouchEvent方法，在合适的场景拦截事件。