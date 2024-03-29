
**Glide**

#### 大纲
* glide特点、优势
* glide加载图片的原理
* glide缓存原理
* 缓存策略
* 其他问题   [](../aQA/answer/android/GlideQA.md)


#### Glide介绍
优点：
* 高效的缓存策略。
* 内存开销小。默认Bitmap格式是RGB_565。
* 支持 Gif、WebP、缩略图，甚至Video
* 图片显示效果为渐变，更加平滑


#### 加载原理(流程)

> glide工作流程大概可以分为3个阶段：
> 1、准备阶段。这里会创建Glide实例，绑定组件生命周期。
> 2、获取图片阶段。判断是否有该图片的请求缓存，如果有则从2种缓存中获取到这个请求，没用则重新创建请求。然后执行这个请求job。执行job时会尝试从磁盘读取文件。
> 首先读取被处理、转换过的磁盘文件(ResourceCacheGenerator)，如果没有便读取原始文件(DataCacheGenerator)，如果还是没有读取到便要从服务器
> 重新获取图片(SourceGenerator)。最后底层通过HttpURLConnection建立连接，获取图片。根据设置是否缓存原始数据文件。接着对文件处理解码之类，转换成可
> 供ImageView使用的数据Bitmap，或者BitmapDrawable。
> 3、设置图片阶段。为into的那个view设置就结束了。



请求图片流程参考下面blog：  
https://blog.csdn.net/FooTyzZ/article/details/89642968

##### 1、构建请求阶段
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

3、Glide的`load()`方法：
* 创建RequestBuilder实例，并且指定资源类型为`Drawable.class`。
* 将glide实例，请求url，RequestManager等核心对象关联绑定。

4、Glide的`into()`方法：
> 处理ImageView设置的ScaleType。(`RequestBuilder#into(View view)`)
> 将imageView包装成DrawableImageViewTarget。(`RequestBuilder#buildImageViewTarget()`)
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
* 判断请求链接是否等于null，
* 如果是在加载完成后的重复加载，可以跳过重新请求，直接使用上次加载的资源；(`onResourceReady()`)
* 发起一次新的请求需要确定资源的宽度与高度。如果资源宽高已有，则执行到`onSizeReady()`,否则执行`target.getSize(this)`,最后重
  新回调`onSizeReady()`。而且这个target是ViewTarget。在前面into是构建了这样一个参数。
* 在`onSizeReady()`,通过Engine#load()，完成资源的请求。

4、Engine#load()
* 将资源请求地址，宽高等包装成一个EngineKey。这个EngineKey的解释是：用于多路复用负载的仅在内存中的缓存键。
* 从内存中取出对应的资源(`loadFromMemory()`)，如果资源存在，通过SingleRequest#onResourceReady()回调出去，设置图片，请求结束。
* 调用`waitForExistingOrStartNewJob()`请求图片。

5、Engine#waitForExistingOrStartNewJob()
* 如果请求过该资源，从缓存Job中获取到已经构建好的EngineJob(EngineJob对应的是EngineKey)。通过`addCallback()`将回调设置到EngineJob内
  部，从而添加到`cbs`中。因为请求过的原因如果成功，`hasResource`值为true，开启线程执行CallResourceReady。最终会从内存中获取到资源通
  过回调回到SingleRequest#onResourceReady()。
* 如果没有请求过该资源，将构建新的EngineJob、DecodeJob。同样的通过`addCallback()`添加到EngineJob的`cbs`以便后续使用，然后EngineJob启
  动DecodeJob。DecodeJob是一个runnable，被启动后直接看它的run方法。而它run方法调用了一个runWrapped()方法。

6、DecodeJob#runWrapped() DecodeJob#run()
```text
  private void runWrapped() {
    switch (runReason) {
      case INITIALIZE: // 第一次请求资源。
        stage = getNextStage(Stage.INITIALIZE);
        currentGenerator = getNextGenerator();
        runGenerators();
        break;
      case SWITCH_TO_SOURCE_SERVICE: // 从磁盘缓存策略切换为服务器资源。即从服务器获取资源。
        runGenerators();
        break;
      case DECODE_DATA: // 其他线程获取数据后，切回原来线程(存在需要切换多次的情况)。可以理解为在子线程拿到了资源，要切回主线程处理。
        decodeFromRetrievedData();
        break;
      default:
        throw new IllegalStateException("Unrecognized run reason: " + runReason);
    }
  }
```
> 这里有点意思，较为仔细说下。这个`runWrapped()`，包括后面要提到的`getNextStage()`，`getNextGenerator()`。先看下代码：  
getNextStage()：
```text
 private Stage getNextStage(Stage current) {
    switch (current) {
      case INITIALIZE: //初始化状态，第一次执行
        return diskCacheStrategy.decodeCachedResource()
            ? Stage.RESOURCE_CACHE
            : getNextStage(Stage.RESOURCE_CACHE);
      case RESOURCE_CACHE:  //从缓存资源中解码获取数据。(指磁盘缓存的被转换过，解码过的数据)
        return diskCacheStrategy.decodeCachedData()
            ? Stage.DATA_CACHE
            : getNextStage(Stage.DATA_CACHE);
      case DATA_CACHE:  // 从缓存的源数据中解码获取数据。(指磁盘缓存未被转换过的原始数据)
        // Skip loading from source if the user opted to only retrieve the resource from cache.
        return onlyRetrieveFromCache ? Stage.FINISHED : Stage.SOURCE;
      case SOURCE: // 从获取的源数据解码获取数据。(指从服务器获取到的源数据)
      case FINISHED: // 结束状态
        return Stage.FINISHED;
      default:
        throw new IllegalArgumentException("Unrecognized stage: " + current);
    }
  }
```
getNextGenerator():  
```text
private DataFetcherGenerator getNextGenerator() {
    switch (stage) {
      case RESOURCE_CACHE: // 转换过的数据缓存 -> 
        return new ResourceCacheGenerator(decodeHelper, this);
      case DATA_CACHE: // 原始数据缓存 -> 
        return new DataCacheGenerator(decodeHelper, this);
      case SOURCE: // 服务器的数据 ->
        return new SourceGenerator(decodeHelper, this);
      case FINISHED:
        return null;
      default:
        throw new IllegalStateException("Unrecognized stage: " + stage);
    }
  }
```
这部分是有点状态机的感觉。3个方法，按照前后顺序我称之为`1号状态机`，`2号状态机`，`3号状态机`。结合方法内的注释，1号状态机负责整个流程的步骤：比如是否为
第一次?是否获取到了数据?是否开始处理数据?。2号发动机则详细负责获取怎么样数据：要缓存的转换过的图?还是缓存的原图?还是服务器上的图。3号发动机则负责获取数
据：如要缓存原图你就找DataCacheGenerator，要服务器的图就找SourceGenerator，等等。    

回到runWrapped()，第一次请求图片自然走*INITIALIZE*的case。这里返回的stage是`Stage.RESOURCE_CACHE`。也就是说
`diskCacheStrategy.decodeCachedResource()`值等于true。这里说下，对于默认磁盘缓存策略(DiskCacheStrategy)，这个值就是true。具体可以追溯到
SingleRequest#onSizeReady()的engine.load()部分，默认的diskCacheStrategy为：   
`@NonNull private DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.AUTOMATIC;`
它的初始化代码
```text
public static final DiskCacheStrategy AUTOMATIC =
      new DiskCacheStrategy() {
        @Override
        public boolean isDataCacheable(DataSource dataSource) {
          return dataSource == DataSource.REMOTE;
        }
        @Override
        public boolean isResourceCacheable(
            boolean isFromAlternateCacheKey, DataSource dataSource, EncodeStrategy encodeStrategy) {
          return ((isFromAlternateCacheKey && dataSource == DataSource.DATA_DISK_CACHE)
                  || dataSource == DataSource.LOCAL)
              && encodeStrategy == EncodeStrategy.TRANSFORMED;
        }
        @Override
        public boolean decodeCachedResource() {
          return true;
        }
        @Override
        public boolean decodeCachedData() {
          return true;
        }
      };
```
经过1、2、3状态后会执行到`runGenerators()`方法。通过`currentGenerator.startNext()`获取到资源。这个currentGenerator此时是
ResourceCacheGenerator的实例，来执行到它的startNext()方法。因为接下会有一个重复`startNext()`，又回到`runGenerators()`以及3个状态机调用的流
程(这里面是一个循环)，所以接下会以步骤流程的形式说明：

* 第一次startNext()是ResourceCacheGenerator：尝试获取cacheKey，但是很明显，第一次请求必然是没有cache的，所以会直接返回false，执行到循环体。此
  时stage = RESOURCE_CACHE，currentGenerator = ResourceCacheGenerator。循环体执行后：stage = DATA_CACHE，currentGenerator =
  DataCacheGenerator。如果找到则会进入到fetcher.loadData()中，这里的fetcher是ByteBufferFetcher。最后回调onDataReady()返回出去。
  
* 第二次startNext()是DataCacheGenerator：第一次请求modelLoaders未被初始化，也没有缓存，直接返回false，再次进入循环体。此时stage = 
  RESOURCE_CACHE，currentGenerator = DataCacheGenerator。再次循环后，stage = SOURCE，currentGenerator = SourceGenerator。
  因为stage = SOURCE，所以会执行循环体的reschedule()。reschedule()方法此时将runReason = SWITCH_TO_SOURCE_SERVICE。同时执行一个callback。
  这个callback是EngineJob。callback.reschedule()就是EngineJob使用线程池再次执行这个DecodeJob。于是再次回到DecodeJob的run方法，再到它的
  runWrapped()。这次不同的是runReason = SWITCH_TO_SOURCE_SERVICE。所以直接调用runGenerators(),最后来到SourceGenerator#startNext()。(
  因为前面的状态都保存起来了，这次一步到位)

7、SourceGenerator#startNext()：
* 第一次请求dataToCache等于null，不会执行里面的逻辑
* sourceCacheGenerator是默认值，不会return true
* started默认是false，会不会执行循环里面的逻辑主要看hasNextModelLoader()
* hasNextModelLoader()主要看helper.getLoadData()
* 其他2个判断loadData.fetcher.getDataSource()与loadData.fetcher.getDataClass()

8、SourceGenerator#startNext()##helper.getLoadData()  
> 这个getLoadData还是有必要单独抽出来提下，<https://blog.csdn.net/FooTyzZ/article/details/89642968>这部分也有提到。差距不是很大。从
>  getLoadData()#glideContext.getRegistry().getModelLoaders(model)处开始

这有几个步骤需要提下：
* 确定model是什么。通过逆推方式知道model就是请求图片的url，String类型。(第一阶段)
* 确定modelLoaders保存的实例都是些什么。 (第二阶段)
* 确定modelLoaders.get的ModelLoader.buildLoadData()是什么。 (第三阶段)

8.1  第二阶段说下逆推思路，类方法流程大概为这样：
> 从glideContext.getRegistry().getModelLoaders(model)开始，进到getModelLoaders() -> Registry#getModelLoaders(model) ->
> ModelLoaderRegistry#getModelLoaders(model){有一次筛选，标记为筛选1} -> getModelLoadersForClass(String.class) -> 
> MultiModelLoaderFactory#build(){有一次筛选，表记为筛选2}。

8.2 筛选2：
```text
synchronized <Model> List<ModelLoader<Model, ?>> build(@NonNull Class<Model> modelClass) {
    try {
      List<ModelLoader<Model, ?>> loaders = new ArrayList<>();
      for (Entry<?, ?> entry : entries) {
        if (alreadyUsedEntries.contains(entry)) {
          continue;
        }
        if (entry.handles(modelClass)) {
          alreadyUsedEntries.add(entry);
          loaders.add(this.<Model, Object>build(entry));
          alreadyUsedEntries.remove(entry);
        }
      }
      return loaders;
    } catch (Throwable t) {
      alreadyUsedEntries.clear();
      throw t;
    }
  }
```
> 此次筛选对象为`entries`。先清楚entries中add的是什么。全文搜索entries.add()，发现它只有在一个add()方法调用，add()方法有2处调用：append(..)与
> prepend(..)。通过跟踪这2个方法就知道，最终会指向`Registry`的append()与prepend()方法。最终会发现只需要关注`append()`方法就够了。
> Registry#append()在new Glide时有大量调用，调用位置在GlideBuilder#build()。例如：
```text
registry
        .append(int.class, InputStream.class, resourceLoaderStreamFactory)
        .append(int.class, ParcelFileDescriptor.class, resourceLoaderFileDescriptorFactory)
        .append(Integer.class, InputStream.class, resourceLoaderStreamFactory)
        .append(Integer.class, ParcelFileDescriptor.class, resourceLoaderFileDescriptorFactory)
        .append(Integer.class, Uri.class, resourceLoaderUriFactory)
        .append(int.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(Integer.class, AssetFileDescriptor.class, resourceLoaderAssetFileDescriptorFactory)
        .append(int.class, Uri.class, resourceLoaderUriFactory)
```
到这已经知道`entries`的数据来源了。回到MultiModelLoaderFactory#build()。分析它的筛选内容,也就是`entry.handles(modelClass)`的结果。点进去可以
发现这个方法就是判断是否为同一个类型的。modelClass是方法入参，是图片的url，String.class。也就是说在众多的`registry.append(...)`中找到一个参数为
String.class的才符合。结合这个条件，能得出符合条件的Entry有：
> .append(String.class, InputStream.class, new DataUrlLoader.StreamFactory<String>())
  .append(String.class, InputStream.class, new StringLoader.StreamFactory())
  .append(String.class, ParcelFileDescriptor.class, new StringLoader.FileDescriptorFactory())
  .append(String.class, AssetFileDescriptor.class, new StringLoader.AssetFileDescriptorFactory())
 
8.3 *this.<Model, Object>build(entry)* 
> 这一步的build()实际上是执行了`entry.factory.build(this)`。entry.factory就是registry.append(..)方法参数中的第三个参数。分别跟踪这四个
> factory后得出对应的4个ModelLoader为：
* DataUrlLoader.StreamFactor -> DataUrlLoader
* StringLoader.StreamFactory -> StringLoader(Uri.class, InputStream.class)
* StringLoader.FileDescriptorFactory() -> StringLoader(Uri.class, ParcelFileDescriptor.class)
* StringLoader.AssetFileDescriptorFactory -> StringLoader(Uri.class, AssetFileDescriptor.class)

虽然有4个Entry符合条件，但return的loaders总体上是2个类别，DataUrlLoader与StringLoader。此方法结束，带着loaders返回来到标记为筛选1的方法。

8.4 *ModelLoaderRegistry#getModelLoaders*
```text
public <A> List<ModelLoader<A, ?>> getModelLoaders(@NonNull A model) {
    List<ModelLoader<A, ?>> modelLoaders = getModelLoadersForClass(getClass(model));
    if (modelLoaders.isEmpty()) {
      throw new NoModelLoaderAvailableException(model);
    }
    int size = modelLoaders.size();
    boolean isEmpty = true;
    List<ModelLoader<A, ?>> filteredLoaders = Collections.emptyList();
    for (int i = 0; i < size; i++) {
      ModelLoader<A, ?> loader = modelLoaders.get(i);
      if (loader.handles(model)) {
        if (isEmpty) {
          filteredLoaders = new ArrayList<>(size - i);
          isEmpty = false;
        }
        filteredLoaders.add(loader);
      }
    }
    if (filteredLoaders.isEmpty()) {
      throw new NoModelLoaderAvailableException(model, modelLoaders);
    }
    return filteredLoaders;
  }
```
同样根据`handles(model)`方法判断DataUrlLoader与StringLoader。结果发现只有StringLoader是符合的。方法返回到DecodeHelper.getLoadData，此时第
一阶段结束，modelLoaders是3个StringLoader实例。

8.5 *modelLoader.buildLoadData()* (第三阶段)   
> 这里的modelLoader.buildLoadData()对应就是StringLoader#buildLoadData()。进到方法里面发现要找一 个uriLoader的东西，以及uriLoader.handles()
> 和handles.buildLoadData(uri, width, height, options)。  
> uriLoader是在new StringLoader时候赋值的，可以看到`8.3`处。具体创建时机是在 MultiModelLoaderFactory#build(..)。注意这个是2个参数，前面看个
> 类的build方法是一个参数的，但是它们逻辑判断是一样的。这个方法就是从`entries`中众多的元素中找到符合条件的那个，单参数build找的是第一参数是
> String.class。双参数方法则是第一，第二参数分别为Uri.class与InputStream.class或 Uri.class与ParcelFileDescriptor.class或Uri.class与
> AssetFileDescriptor.class。经查找后会发现这3套组合只有Uri.class + InputStream.class 符合并且数量不止一个。然后便会执行
> 到`return factory.build(loaders, throwableListPool);`这里new了一个MultiModelLoader实例返回。也就是说在StringLoader#buildLoadData()中
> 的uriLoader是MultiModelLoader。

8.6 *uriLoader.handles()*
> 此过程是在第一二参数是Uri.class + InputStream.class构建的entries中找它们的Factory的handles方法。这里总结一下。从entries中找Factory一共有3次。
* 第一次是在MultiModelLoaderFactory#handles，判断全部entries构建的第一个参数是否为String.class.
* 第二次是在MultiModelLoaderFactory#handles，判断全部entries构建的第一，第二参数是否为3大组合，即Uri.class+InputStream.class、
  ParcelFileDescriptor.class、AssetFileDescriptor.class。
* 第三次也就是这次，在MultiModelLoader#handles，判断在第二次的条件下过滤的entries中自己的handles()方法，参数为Uri。
> 经过筛选后，能得到factory只有一个：*UrlUriLoader*

8.7 *uriLoader.buildLoadData()*
> 来到UrlUriLoader#buildLoadData()。发现又是一个`urlLoader.buildLoadData(glideUrl,...)`与找StringLoader的uriLoader思路找UrlUriLoader的
> uriLoader来源，发现在UrlUriLoader的构建代码如下：
```text
    public ModelLoader<Uri, InputStream> build(MultiModelLoaderFactory multiFactory) {
      return new UrlUriLoader<>(multiFactory.build(GlideUrl.class, InputStream.class));
    }
```
在entries找factory前面已经找过3次了，这是第四次。最终得到UrlUriLoader的uriLoader是HttpGlideUrlLoader。

8.8 *HttpGlideUrlLoader#buildLoadData()*
> 这里终于找到了要找的东西`return new LoadData<>(url, new HttpUrlFetcher(url, timeout))`

到这里返回了一个LoadData实例。LoadData里面的fetcher是HttpUrlFetcher。第三阶段结束。整个helper.getLoadData()方法return。返回到
`SourceGenerator#startNext()`。

9、知道了`loadData.fetcher`是HttpUrlFetcher，对于第7点的关键点就有结果了。在SourceGenerator#startNext()中while部分的if判断的3个条件：
* loadData不等于null；
* loadData.fetcher.getDataSource()等于DataSource.REMOTE，结果与默认磁盘缓存策略isDataCacheable()结果相同，所以它是true。
* 第三个判断(这点不看也可以，因为这是或上第二个条件，2者有一个条件成立即可)
所以会执行startNextLoad()方法，同时将started赋值为true，最后return。
  
10、*SourceGenerator#startNextLoad()*
> `loadData.fetcher`的fetcher是HttpUrlFetcher，通过HttpUrlFetcher请求图片资源。看到HttpUrlFetcher#loadData()代码
```text
  public void loadData(
      @NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
    long startTime = LogTime.getLogTime();
    try {
      InputStream result = loadDataWithRedirects(glideUrl.toURL(), 0, null, glideUrl.getHeaders());
      callback.onDataReady(result);
    } catch (IOException e) {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Failed to load data for url", e);
      }
      callback.onLoadFailed(e);
    } finally {
      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Finished http url fetcher fetch in " + LogTime.getElapsedMillis(startTime));
      }
    }
  }
```

11、*HttpUrlFetcher#loadDataWithRedirects()*
* 构建HttpURLConnection
* 获取图片的Stream
* 将资源callback(callback是在SourceGenerator中声明传入的)

12、*SourceGenerator#onDataReadyInternal()*
* 将获取的资源赋值给dataToCache
* `cb.reschedule()`切换到原始线程(其中cb指DecodeJob)

> 注意`cb.reschedule()`在前面的*第6.*，也就是说状态机状态转换地方说明过。结果就是会再次重新执行一次DecodeJob#run(),来到
> SourceGenerator#startNext()。但是此时的`dataToCache`不再试null了，执行到cacheData()方法，将图片资源(原始)写入磁盘缓存。成功保存到
> 磁盘后，初始化DataCacheGenerator实例，执行它的startNext()读取成功保存到磁盘的文件缓存，成功读取return true结束。

13、*DataCacheGenerator#startNext()*
> modelLoaders未被初始化，等于null；sourceIdIndex自加等于0，而cacheKeys的size是等于1的，所以会执行第一个while的所有逻辑。
* 从cacheKey中获取sourceId，根据这个id创建DataCacheKey，并以此作为key从DiskCache获取到之前的图片资源，赋值给cacheFile
* 初始化modelLoaders`helper.getModelLoaders(cacheFile)` (二阶段)
* 初始化loadData (三阶段)
* 执行loadData.fetcher.loadData()
* 最后return true；同样外面执行它的SourceGenerator#startNext()也return true。

这里与`SourceGenerator#startNext()`的那三个阶段是一样的，具体参照*第8.的二阶段与三阶段*。注意的是SourceGenerator中的`model`是String.class，这
里的`model`是File.class。接着就是到Glide类中寻找，根据具体的条件过滤。这里得到的`modelLoaders`是`ByteBufferFileLoader`,`loadData`是
`LoadData`实例，`loadData.fetcher`是`ByteBufferFetcher`。

14、*ByteBufferFetcher#loadData*
* 读取文件资源
* 携带数据callback
> 这里callback回到 DataCacheGenerator#onDataReady()，然后再一个`cb.onDataFetcherReady()`回到`SourceGenerator#onDataFetcherReady()`。
> 在onDataFetcherReady()方法回到DecodeJob#onDataFetcherReady()。

15、*DecodeJob#onDataFetcherReady()*
* 判断当前线程是否是最初的那条线程。如果不是，赋值runReason`RunReason.DECODE_DATA`，借助`callback.reschedule(this)`一直返回到那条线程。这样
  即是重新运行的run方法，因为runReason = `RunReason.DECODE_DATA`，也会直接执行decodeFromRetrievedData()方法，不会再去请求资源。
* 原始资源解码decodeFromRetrievedData()。

16、*DecodeJob#decodeFromRetrievedData*
* 解码decodeFromData().(在解码时会回调到`onResourceDecoded()`，deferredEncodeManager会被初始化)
* 重新编码以及释放资源notifyEncodeAndRelease()

17、*DecodeJob#notifyEncodeAndRelease()*
* 执行notifyComplete()，完成请求，准备设置图片环节。
* stage赋值Stage.ENCODE，执行DeferredEncodeManager#encode(..)将转换过的数据添加到磁盘缓存中(DiskLruCache)，这里是缓存处理后的文件资源；
* 执行onEncodeComplete()，状态初始化。如stage，model，currentGenerator等等
> notifyComplete()方法就是通过callback回到EngineJob#onResourceReady()方法。

18、 再次回到*EngineJob#onResourceReady()*
> 在这里初始化一些常量后调用`notifyCallbacksOfResult()`方法。在这个方法里，1、首先校验取消状态，如果取消了就将资源回收；2、重新将图片resource包装
> 成一个EngineResource，通过`engineJobListener.onEngineJobComplete()`回调到`Engine`，将EngineResource添加到ActiveResources保存(内存缓存)
> 最后通过遍历`cbs`的回调，返回到SingleRequest#onResourceReady()准备为View设置图片。(`cbs`怎么来的可以看*第5、*)


##### 3、设置图片阶段
1、*SingleRequest#onResourceReady(...) 回调接口方法，3个参数*
* 校验获取的资源合法性
* 执行onResourceReady()。注意这个方法是SingleRequest的私有方法，前面那个同名方法是ResourceCallback的接口方法。

2、*SingleRequest#onResourceReady()* 类私有方法，4个参数
* 将当前的status设置为Status.COMPLETE
* 其他监听器的成功回调(onResourceReady())
* 为View设置图片(`target.onResourceReady(result, animation)`)
> 这里的target要追溯到Glide#into()方法(*前面第4、*)，就是在那里创建并且传递过来的。target是`DrawableImageViewTarget`示例。但是
> onResourceReady()是父类方法，DrawableImageViewTarget并没重写。它的父类是`ImageViewTarget`。在这里调用`setResourceInternal()`方法，
> 设置图片以及图片。设置图片的方法`setResource()`是抽象方法，由它的子类DrawableImageViewTarget实现。

3、*DrawableImageViewTarget#setResource()*
```
  protected void setResource(@Nullable Drawable resource) {
    view.setImageDrawable(resource);
  }
```
> `view`就是Glide.into(view)的那个view。到此，glide加载图片的流程已经全部走完。


#### 缓存原理
glide从缓存中获取图片有2种形式，内存缓存与磁盘缓存。

##### 源码分析
内存缓存又分2种：ActiveResources与LruResourceCache。

> 从内存缓存获取图片在整个加载流程中可追溯到`Engine#load()`方法。这里正是`into()`之后的流程。
```text
 // 省略代码...
    EngineKey key =
        keyFactory.buildKey(
            model,
            signature,
            width,
            height,
            transformations,
            resourceClass,
            transcodeClass,
            options);
    EngineResource<?> memoryResource;
    synchronized (this) {
      memoryResource = loadFromMemory(key, isMemoryCacheable, startTime);
      if (memoryResource == null) {
        return waitForExistingOrStartNewJob(
   // 省略代码...
```
loadFromMemory()分别从2个方法分别获取图片缓存，分别是loadFromActiveResources()，对应就是ActiveResources实现；以及loadFromCache()，对应就
是MemoryCache。但MemoryCache是一个接口，初始化是LruResourceCache完成的。

1、loadFromActiveResources()
> 这方法就是通过对应的key从ActiveResources中get对应的资源。如果获取到，对应的resource内部计数器+1(通过这个计数器来表示资源的活跃程度)，返回此资源。

1.1、ActiveResources
> 1、这个类内部维护了一个HashMap<Key, ResourceWeakReference>用来保存图片Resource，所以在ActiveResources中的缓存又称弱引用缓存。
  ActiveResources提供activate()方法(保存)，deactivate()方法(删除)，get()方法(读取)用来增删查。

> 2、除维护一个HashMap用来保存图片Resource外，还维护一个ReferenceQueue队列，定期将map中保存的resource转移到LruResourceCache中。
> ActiveResources内部会开启一个线程，通过`cleanReferenceQueue()`方法循环清理ReferenceQueue中保存的resource。cleanReferenceQueue方法调用
  cleanupActiveReference()将map中对应的那个资源移除，达到queue与map数据同步；最后通过listener将移除的resource转移到LruResourceCache中。

2、loadFromCache()
> 从LruResourceCache中获取对应的resource，获取成功后，该resource计数器+1，同时将该resource从cache转移到ActiveResources中。

2.1、LruResourceCache
> 继承了LruCache，应用了LRU(最近最少使用)淘汰算法。内部通过LinkedHashMap来实现LRU。还实现了MemoryCache接口。它的几个核心方法：
* get() 根据key从LinkedHashMap取出value。
* put() 根据key保存value到LinkedHashMap。先判断当前缓存容量是否已满，如果满了则回收掉，否则添加到LinkedHashMap。更新缓存map内的元素数量
* trimToSize(int maxSize)  传入一个size，维持map内元素在size范围内。超出的元素从开始依次移除(回收)。

3、EngineResource
> 请求数据的包装类，作为缓存的实体保存在ActiveResources中，内部维护一个计数器`acquired`作为图片加载的次数。每请求加载一次，调用一
> 次acquire()方法，acquired+1；当页面结束会调用release()方法，acquired-1，当acquired等于0时，这个resource将被移出ActiveResources，转
> 移到LruResourceCache。当内存紧张调用recycle()释放掉(回收)

3.1、拓展(ComponentCallbacks,ComponentCallbacks2)
> ComponentCallbacks2继承ComponentCallbacks(都是接口)，用于内存管理。四大组件都有实现这个接口。Glide类同样实现该接口用于管理内存。通过回调它的
> `onTrimMemory()`执行`trimMemory()`方法，而Glide类持有了LruResourceCache的实例，间接更新LruResourceCache中内存缓存数量。同时更新内存缓
> 存的还有BitmapPool。


4、磁盘缓存：
> 读取过程： ResourceCacheGenerator(获取转换过的资源) -> DataCacheGenerator(原始数据) -> SourceGenerator()(服务器数据)
> 写入过程：SourceGenerator#cacheData()(原始数据) -> DecodeJob#notifyEncodeAndRelease()(获取转换过的资源)

4.1、DiskCache
> 在写入原始数据时调用的DecodeHelper#getDiskCache().put()或者写入转换过的数据调用的DiskCacheProvider#getDiskCache().put()都是
> LazyDiskCacheProvider来实现。DiskCacheProvider是一个接口，唯一实现类就是LazyDiskCacheProvider。LazyDiskCacheProvider#getDiskCache()
> 有2种类型。优先级高的是由`factory.build()`构建出来，另一个是DiskCacheAdapter(它里面的方法几乎都是空方法，不用去看)。`factory`是
> DiskCache.Factory，它的实现是InternalCacheDiskCacheFactory(在GlideBuilder类初始化)。InternalCacheDiskCacheFactory规定了一个叫
> "image_manager_disk_cache"，大小为250M的文件目录。可见磁盘缓存的图片是放在data/data/.../cache/image_manager_disk_cache目录下。它继
> 承DiskLruCacheFactory，build()方法返回的是DiskLruCacheWrapper实例。DiskLruCacheWrapper是DiskLruCache的包装类。真正去做读写缓存的是这个
> DiskLruCache类与DiskLruCacheWrapper。

4.2、DiskLruCache与DiskLruCacheWrapper
> DiskLruCache应用了LRU算法，内部维护一个LinkedHashMap<String, Entry>。Entry是由图片文件、存储路径、文件大小等属性封装而成。然后提供了Editor与
> Value2个类对外暴露，间接访问Entry。Editor用于写入保存，Value用于读取。每次读取图片时优先从LinkedHashMap中查找，找不到再检查Entry对象内的文件是否
> 存在。最后封装成Value对象返回。保存图片时构建一个Entry对象添加到LinkedHashMap，然后封装成Editor返回。

> DiskLruCacheWrapper核心方法：
* put() 保存缓存文件到磁盘，配合DiskLruCache#edit()将文件包装成一个Editor，通过`editor.getFile(0)`得到file，最后调用`writer(file)`写入到磁
  盘。调用editor.commit()更新DiskLruCache。
* get() 从磁盘读取缓存文件,调用DiskLruCache#get()拿到包装好的Value对象，通过`value.getFile(0)`得到File文件。

> DiskLruCache核心方法
* open()  DiskLruCacheWrapper第一次调用getDiskCache()方法时会调用open方法。核心就是读取journal文件，将磁盘的缓存写到内存(LinkedHashMap)
* edit()  根据key生成一个Entry实例添加到LinkedHashMap，然后包装成一个Editor返回，同时写入一条“DIRTY”日志
* completeEdit()  释放编辑锁；处理临时的tmp文件；校验文件length；同时写入一条“CLEAN”日志
* get()  从LinkedHashMap获得目标Entry，包装成Value返回；同时写入一条“READ”日志
* remove()  从LinkedHashMap移除目标Entry，以及从磁盘删除目标文件；同时写入一条“REMOVE”日志


4.3、DiskLruCache中LinkedHashMap的恢复
> DiskLruCache内部维护一个LinkedHashMap，每次读取都优先从map获取，相当于从磁盘读取切换成了从内存读取，速度上有很大的提升。但随着(应用)页面结束，
> DiskLruCache也会被释放销毁，这个LinkedHashMap也会销毁，之前的缓存也全部回收了。但为了实现依旧能从内存中读取，创建DiskLruCache时会初始化这个
> LinkedHashMap。借助journal日志文件，重新将缓存添加到map中。

4.4、DiskLruCache的journal日志文件机制
> 主要用于恢复LinkedHashMap的缓存对象。每次初始化DiskLruCache时会访问journal文件。journal文件保存的是操作记录，比如put操作会生成一条“DIRTY”
> 与一条“CLEAN”记录；get操作会生成一条“READ”记录；remove操作生成一条“REMOVE”记录。读取journal文件时就能将缓存添加到LinkedHashMap。

4.5 Editor与Value
> 提供Editor与Value的好处就是方便灵活，能够做到统一处理。无论保存的资源是bitmap是drawable还是其他，存储在磁盘后都成了文件。再统一包装成Entry，Entry
> 包含了在内存中的文件缓存引用，在磁盘中的缓存路径，文件大小等等属性。

##### 总结
> 以一个新图片资源请求为例，它的存取流程为：首先从内存缓存中的弱引用缓存集ActiveResources中读取，然后尝试到LruResourceCache中获取。如果无法读取到目
> 标资源将会从磁盘缓存中读取；先尝试从转换过的磁盘缓存中读取，然后尝试从原始资源(未转换过)缓存中读取。如果都无法获取，将请求服务器，从服务器获取图片。请求
> 图片下来后先把原始图片资源(未解码、未转换)缓存到磁盘(如果允许做磁盘缓存)，然后对图片进行转换，解码，再缓存转换过的图片资源到磁盘。在设置，显示图片之前，
> 再保存到弱引用缓存集中。当加载图片的页面结束后，回调RequestManager#onDestroy()方法(Glide能绑定页面的生命周期)。在这里遍历所有的target，执行
> clear(target)，通过target获取对应的request，执行request.clear()。也就是SingleRequest#clear()。最终弱引用缓存集中的EngineResource执
> 行release()方法。内部的计数器自减，当计数器等于0时，通过回调保存到LruCache中，完成从弱引用缓存到LruCache的转移。


#### 缓存策略
> AUTOMATIC：默认策略。根据图片资源的数据来决定是否缓存原始图片和转换后的图片。这种策略会尽可能地减少磁盘存储空间的使用，同时保证图片的加载性能。
> ALL：缓存原始图片和转换后图片；
> SOURCE：只缓存原始图片；
> RESULT：只缓存转换后图片；
> NONE：都不缓存；
