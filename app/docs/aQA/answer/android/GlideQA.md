
### 

源码分析、知识点： [](../../../third_frame/glide.md)


##### 简单说说Glide
> glide工作流程大概可以分为3个阶段：
> 1、准备阶段。这里会创建Glide实例，绑定组件生命周期。
> 2、获取图片阶段。判断是否有该图片的请求缓存，如果有则从2种缓存中获取到这个请求，没用则重新创建请求。然后执行这个请求job。执行job时会尝试从磁盘读取文件。
> 首先读取被处理、转换过的磁盘文件(ResourceCacheGenerator)，如果没有便读取原始文件(DataCacheGenerator)，如果还是没有读取到便要从服务器
> 重新获取图片(SourceGenerator)。最后底层通过HttpURLConnection建立连接，获取图片。根据设置是否缓存原始数据文件。接着对文件处理解码之类，转换成可
> 供ImageView使用的数据Bitmap，或者BitmapDrawable。
> 3、设置图片阶段。为into的那个view设置就结束了。

##### 为什么选择Glide不选择其他的图片加载框架
> 生命周期管理、高效缓存、内存管理(BitmapPool)，ARGB_565，支持多种格式如png、jpg、gif(picasso不支持，fresco支持)


##### Glide对内存方面有什么特别做法吗
> 1、默认图片格式使用RGB_565，内存占比小
> 2、3级缓存
> 3、Bitmap缓存池，Bitmap复用。对BitmapFactory.Options属性设置开启Bitmap复用。可以避免频繁申请内存，避免OOM。

##### 为什么glide内存缓存要设计2层?
> 用弱引用缓存的资源都是当前活跃资源ActiveResource，保护这部分资源不会被LruCache算法回收，同时使用频率高的资源将不会在LruCache中查找，相当于
> 替LruCache减压。

##### glide如何与activity、fragment绑定生命周期?
> glide绑定页面的生命周期是由RequestManager实现的。在执行`Glide.with()`时会调用到RequestManagerRetriever.get(...)方法，参数可以是activity
> 或fragment,或者其他Context。在get()的过程中会创建一个Fragment(RequestManagerFragment)，这个fragment持有一个
> Lifecycle(ActivityFragmentLifecycle)，这个lifecycle绑定了fragment的生命周期，在构建RequestManager时会将这个fragment的lifecycle传入，
> 届时通过addListener()将RequestManager与lifecycle关联。达到RequestManager绑定内部fragment生命周期的效果。

##### DiskLruCache中LinkedHashMap如何恢复?
>初始化DiskLruCache时会借助journal日志文件，重新将缓存添加到这个LinkedHashMap中，完成对LinkedHashMap的初始化。

##### DiskLruCache的journal日志文件机制?
> 主要用于恢复LinkedHashMap的缓存对象。journal文件保存的是操作记录，比如put操作会生成一条“DIRTY”与一条“CLEAN”记录；get操作会生
> 成一条“READ”记录；remove操作生成一条“REMOVE”记录。初始化DiskLruCache时读取journal文件将缓存添加到LinkedHashMap。

##### Glide有哪些缓存策略?
> AUTOMATIC：默认策略。根据图片资源的数据来决定是否缓存原始图片和转换后的图片。这种策略会尽可能地减少磁盘存储空间的使用，同时保证图片的加载性能。
> ALL：缓存原始图片和转换后图片；
> SOURCE：只缓存原始图片；
> RESULT：只缓存转换后图片；
> NONE：都不缓存；