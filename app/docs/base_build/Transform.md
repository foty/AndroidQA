### Transform


##### 1、编写 transform

使用gradle的transform。创建一个类继承`Transform`，注意包名是`com.android.build.api.transform.Transform`。拿一个以前的例子：

```text
class LogTransform extends Transform {
    private Project mProject

    LogTransform(Project mProject) {
        this.mProject = mProject
    }

    /**
     * transform的名字，也对应了该Transform所代表的Task名称.
     */
    @Override
    String getName() {
        return "LogTransForm"
    }

    /**
     * transform输入类型，可在TransformManager查看更多类型,常见的有
     * CONTENT_CLASS  class文件
     * CONTENT_JARS   jar文件
     * CONTENT_DEX   dex文件
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * transform输入文件所属范畴，可在TransformManager查看更多类型,常见的有
     * SCOPE_FULL_PROJECT  代表所有Project
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 表示是否支持增量编译
     */
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation trans) throws TransformException, InterruptedException, IOException {
        println("[transform] ------- start --------")
        // 获取输出目录
//        def out = trans.outputProvider
//        out.deleteAll()

        mProject.android.bootClasspath.each {
            println("[mProject.android.bootClasspath] ${it.absolutePath}")
        }

        trans.inputs.each { input ->
            // 获取到所有的目录文件类输入流
            input.directoryInputs.each { dirInput ->
                String path = dirInput.file.absolutePath
                println("[transform] path= $path]")

                // 注入代码(修改代码)
                InjectByJavassist.inject(path, mProject)

                def outDir = trans.outputProvider.getContentLocation(dirInput.name,
                        dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                outDir.deleteDir()
                // 复制到输出目录
                FileUtils.copyDirectory(dirInput.file, outDir)
            }
        }

        // jar文件直接复制到输出目录即可(注意这步不能没有修改就不写，否则运行会报错找不到类)
        trans.inputs.each { input ->
            // 所有的jar类型输入流
            input.jarInputs.each { jarInput ->
                def dest = trans.outputProvider.getContentLocation(jarInput.name,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                println("[transform] jar_output: $dest")
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        println("[transform] ------- end ---------")
    }
}
```
实现Transform需要实习他的几个抽象方法：

* getName()：transform 名称，也对应该Transform所代表的Task名称
* getInputTypes()：接受输入处理类型。有class、jar、dex等
* getScopes()：输入处理类型的范畴。如当前工程、当前工程+外部库等
* isIncremental()：是否增量编译
* transform()：核心步骤，处理字节码等逻辑


##### 对于transform()方法 

> 这个方法一般都是修改一些字节码之类的操作，可以使用javassist、asm或者其他。一般来说有一个大概的流程：

* 获取输出流。一般有jar类型、文件目录(.class文件)类型
* 修改
* 复制回输出目录。目录文件使用`FileUtils.copyDirectory()`，jar使用`FileUtils.copyFile`。所有的类型都要复制到输出目录，不管有没有修改。


##### 2、注册transform
在第一步编写好的plugin类的`apply()`方法注册transform。
```text
class LogsPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        //要想使用自定义的transform，就要先注册transform。
        //获取build.gradle中的android闭包
        AppExtension android = target.getExtensions().findByType(AppExtension.class)
        //注册transform
        android.registerTransform(new LogTransform(target))
    }
}
```
注意 plugin接口的包是`org.gradle.api.Plugin`，project也是`org.gradle.api`包下的。
