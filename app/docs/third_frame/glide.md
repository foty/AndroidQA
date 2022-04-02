
### Glide

* glide特点、优势
* glide加载图片的原理
* glide缓存原理
* 其他问题

#### Glide


#### 加载原理
 
请求图片流程参考下面blog：  
<https://blog.csdn.net/FooTyzZ/article/details/89642968>

##### 1、构建阶段阶段
> Glide起始方法`with()`：

1、获取glide对象。   
glide对象是一个单例模式，核心位置方法为`initializeGlide()`。实际上是GlideBuilder类直接new出来的一个Glide对象。GlideBuilder#build()方法：
```text
return new Glide(
        context, // 上下文
        engine, // 
        memoryCache,
        bitmapPool,
        arrayPool,
        requestManagerRetriever,
        connectivityMonitorFactory,
        logLevel,
        defaultRequestOptionsFactory,
        defaultTransitionOptions,
        defaultRequestListeners,
        experiments);
```

2、获取RequestManager。  
> RequestManager是什么呢？注释解释为：“用于管理和启动 Glide 请求的类，可以使用活动、片段和连接生命周期事件来智能地停止、启动和重新启动请求。”简单说
> 就是能根据使用场景的生命周期，智能加载图片的一个管理类。

> Glide的`load()`方法：

* 创建RequestBuilder实例，并且指定资源类型为`Drawable`。
* 将glide实例，请求url，RequestManager等核心对象关联绑定。


3、Glide的`into()`方法：

> 处理ImageView设置的ScaleType。(`RequestBuilder#into(View view)`)
> 将imageView 包装成ViewTarget。(`RequestBuilder#buildImageViewTarget()`)
> 构建一个新的Request。(`RequestBuilder#buildRequest()`)
> 从target中get获取一个request，并与新的request对比。如果是同一个请并且没有缓存并且是完成状态，则使用前一个request再次请求。否则使用新request请求。

##### 2、请求阶段
> `RequestManager#track(target, request)`执行网络请求的开始。

1、无论是使用旧request(`previous.begin()`)还是使用新的request(`requestManager.track()`)，最后请求图片的都是`request.begin()`   

2、这个request是SingleRequest。下面看看为什么会是SingleRequest实例。
> request的具体实例可以追溯到`into()`方法的`RequestBuilder#buildRequestRecursive()`。在这个方法，存在2种request类型return。
* 1. 通过`buildThumbnailRequestRecursive()`方法构建的 mainRequest。
* 2. ErrorRequestCoordinator实例。但是这个实例同样会调用`setRequests()`将mainRequest传入。在执行`request.begin()`时，也是调用的
  mainRequest.begin。所以研究 mainRequest即可。
     
到`RequestBuilder#buildThumbnailRequestRecursive`方法。这个方法判断了是否处理缩略图。从返回结果来看，有2种类型(参考前面的mainRequest)：
* ThumbnailRequestCoordinator实例。但是同样会调用`setRequests()`将一个以`obtainRequest()`方法创建的实例 fullRequest传入。最终调用的也是
  这个fullRequest的begin()。可以参考前面的`ErrorRequestCoordinator`。
* `obtainRequest()`创建的实例。

来到`obtainRequest()`方法。在这很明确返回的Request是SingleRequest。
```text
return SingleRequest.obtain(
        context,
        glideContext,
        requestLock,
        model,
        transcodeClass,
        requestOptions,
        overrideWidth,
        overrideHeight,
        priority,
        target,
        targetListener,
        requestListeners,
        requestCoordinator,
        glideContext.getEngine(),
        transitionOptions.getTransitionFactory(),
        callbackExecutor);
```

3、SingleRequest#begin()




##### 3、设置图片阶段



#### 缓存原理


#### 其他问题
