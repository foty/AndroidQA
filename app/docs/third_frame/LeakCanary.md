### LeakCanary。

#### 作用
square公司开源的一个内存泄露检测的开源库。

#### 使用初始化
> 引用依赖即可，不用在application的onCreate中初始化了。因为它在内部定义了一个ContentProvider,然后在
> ContentProvider的里面进行的初始化。初始化代码：` AppWatcher.manualInstall(application)`。

随便一提：
> ContentProvider的onCreate执行时机比Application的onCreate执行时机还早。通过自定义一个ContentProvider完成自
> 己库的初始化操作确实挺好，但是每一个库都一个ContentProvider，显得多余。这时可以使用Jetpack推出了`App Startup`。

#### 检测对象
> activity、fragment、view-model

#### 检测时机
> 初始化时会注册LifecycleCallbacks到act或者fragment的生命周期中，当act或者fragment销毁时触发回调，开始检测
> 是否泄露。

#### 工作原理
> WeakReference弱引用机制。每当GC时,弱引用持有的对象在某一时间点被确定可达性是弱可达的,那么它引用的对象就会被回收，
> 这个WeakReference会被加入到对应的ReferenceQueue。

#### 实现监控内存泄露
> 开始检测时，比如act，会调用` objectWatcher.watch()`进行检测：   
> 1、将回收对象，特殊key(uuid)，时间等包装成一个自定义的WeakReference(KeyedWeakReference)；    
> 2、将key与KeyedWeakReference添加到监听对象map中；    
> 3、等待一段时间后(默认5秒)，检查map中的对象情况，如果map中的数量不为0(没有被回收)，手动触发GC；检查map中的对象
> 数量。   
> 4、如果此时map大小等于0，说明所有对象已经被回收；如果不等于0，说明发生泄露，则发送泄露通知以及dump内存，分析内存，
> 计算GC路径。

最后完成关键部分的关键类：   
dump内存-- AndroidHeapDumper。(VMDebug.dumpHprofData())    
分析内存服务-- HeapAnalyzerService。   
分析泄露对象计算GC路径-- HeapAnalyzer。   
