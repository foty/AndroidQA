### Gradle

* gradle
* transform


##### gradle
一、gradle是什么?  
简单来说gradle就是一个构建(编译)工具，与ant是一样的。它使用的是groovy语言。

二、groovy语法   
想深入学习gradle，学习groovy语法是不可缺少的。对于开发来说，至少掌握了一种语言，那么学习其他语言来也是比较轻松的。不多做描述。

三、gradle的执行流程  
流程一般分为：初始化 --> 配置 --> 执行。  
初始化阶段gradle会先读取项目中的setting.gradle文件，从而解析出所有子项目的build.gradle文件，每一个子项目的build.gradle文件
都会被gradle加载解析后形成一个Project。setting.gradle中的`include()`方法可以添加需要构建的子项目。
对Project的了解程度对gradle开发还是很有帮助的。

##### Project
每一个项目的build.gradle文件都会被加载成一个Project。   
1、常用API。(比如apply、ext等等)
```groovy
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
```
在build.gradle文件常常能见到这样的代码来引入一些插件或者库，这里的`apply`其实就是调用Project中的apply()方法。

2、Extension。
* 1、Extension是什么?了解这个是什么东西先看一段代码：
```groovy
android {
    compileSdkVersion 30
    buildToolsVersion "30.0.0"
    defaultConfig {
        applicationId "com"
        minSdkVersion 21
        targetSdkVersion 30
        versionCode 1
        versionName "1.0"
    }
    buildTypes {
        release {
            minifyEnabled false
        }
    }
}
dependencies {
}
```
这是android 项目中build.gradle中很常见的东西。其中看到的`android`,`defaultConfig`就是Extension。翻译过来就是扩展。gradle可以识别出这
些Extension配置，从而读取到里面的内容。

* 2、Extension的原理。   
  鼠标放到build.gradle的`android`可以跳转到一个叫AppExtension的类。其实每一个配置，比如`android`背后都是对应着一个类，这个类声明
  可以配置的属性。gradle在解析过程中就可以读取到这些属性进行各种处理了。

* 3、自定义Extension。   
  在gradle中，可以通过Project的getExtensions()获取一个ExtensionContainer对象。通过这个对象管理Extension。比如增加跟查找。
  增加Extension: 看一个例子，在项目的build.gradle空白位置添加下面代码
```
// 声明一个对应类。
class Ob {
    int ver // 配置属性
    String name
    String toString() {
        return "name = ${name}, age = ${ver}"
    }
}
// 调用Project方法创建一个Extansion
getExtensions().create("obxx", Ob)
// 配置属性
obxx{
    name = "123"
    ver = 1
}
//检测结果
task testEX {
    println(project.obxx)
}
```
然后build项目或者使用命令`gradle testEX`就能看到输出结果。查找相关的Extension使用方法
`getExtensions().findByType()`更多关于Extension的看下面文章：  
<https://www.jianshu.com/p/58d86b4c0ee5>


四、task    
<https://www.heqiangfly.com/2016/03/13/development-tool-gradle-task/>    
gradle在配置阶段，其实就是配置Project对象，而Project对象则是由一个或多个task组成。可以说task是gradle执行的最小单元。
task部分最基础的学习部分有如何创建task与执行task。创建task其实在groovy语法学习上就有，更多查看groovy语法知识。而关于执行
task，这里是讲单独执行一个task，直接在studio的Terminal使用下面命令即可:
``
gradle task名称
``


五、plugin(插件)    
插件是类似一个工具，也类似一个第三方库。需要引入到项目中。这样做的好处是更加省事方便，高效，毕竟能重复利用。重点要掌握自定义插件。
<https://blog.csdn.net/qq_20798591/article/details/107061673>

##### 实现自定义插件的方法。
一般来说实现一个插件的开发有以下3种方式:
* gradle脚本语言实现。
* android项目内buildSrc目录下实现。
* 独立项目实现。

1、gradle脚本语言实现。   
这种方式是最简单一种方式。直接创建一个gradle脚本文件编写代码即可。甚至对工具也没有要求。可以使用AS，也可以使用idea。回归android方向，在
项目内的build.gradle文件内编写就能完成，然后记得添加`apply plugin: 插件名字`引入插件即可。如：
```groovy
apply plugin: Demo

class Demo implements Plugin<Project> {
    @Override
    void apply(Project target) {
        println 'Demo  ~~~~~~~'
        target.task("hello") {
            println('task  task  task!!!')
        }
    }
}
```
当同步项目时便能在build日志中看到自己写的print。


2、buildSrc实现。     
先说实现步骤：  
第一步：创建一个新的module(File -> New -> New Module)。注意module名字一定是`buildSrc`,然后选择Android Library或者java Library
都可以。因为到最后都是要修改目录结构的，选哪个影响不大。

第二步：修改目录结构。
* 在`src/main/`目录下创建groovy文件夹，用来存放插件代码。原来的java目录可以删除。
* 在`main`目录下创建`resources/META-INF/gradle-plugins`文件夹。用来存放插件的配置文件。
* 修改`build.gradle`文件，如果没有该文件，在`src/`目录下创建即可，注意是与src为同级目录。

buildSrc必要目录结构，文件大概如上述，剩下多余文件或目录可以删除。build.gradle文件修改内容如下：
```groovy
apply plugin: 'groovy'
repositories {
    google()
    mavenCentral()
    jcenter()
}
dependencies {
    implementation gradleApi() // gradle相关的API
    implementation localGroovy() // groovy语言支持
//    implementation 'com.android.tools.build:gradle:3.5.3'
}
```
最终目录结构如下：    
[](../图片/img_buildsrc.png)    
另外一点就是，创建好module，项目同步完成后，会自动在setting.gradle文件添加`include ':buildSrc'`，从而将buildSrc视为一个子项目。但是我
们这个是作为插件项目，不是android项目，所以应该将buildSrc的配置从setting.gradle删除。不然buildSrc会被编译2遍。一遍作为插件，一遍做为子
项目。

第三步：编写插件代码。在groovy文件夹下创建一个LogsPlugin.groovy文件(插件名称自定义，这里虽然是文件，但是在编辑文件时，会自动转成类的形式)。
```groovy
class LogsPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        println('-----------------------------')
        println('------- log log log   -------')
        println("-----------------------------")
    }
}
```
这里注意，可能你的AS对groovy语言的支持比不上对java语言的支持，很多时候是没有提示的。这个没办法，只能依靠熟练度来弥补。这里的`Plugin`是在
`org.gradle.api`包下的，不要引入错了。

第四步：配置插件信息。在`resources/META-INF/gradle-plugins`目录下创一个后缀为.properties的文件。注意文件名称将会被视为这个插件的名
称。文件内容指向应用的插件类名。如果插件类之上还有多层目录，也需要一并配置。检测方法就是按住`ctrl`键，鼠标左击类名。如果能够跳转则表示配置没
有问题；如果无法跳转则需要检查路径是否配置错误。例子的插件类直接在groovy目录下创建，没有多余的层级，所以不需要额外的路径：
```
// log.propertier
implementation-class=LogsPlugin
```

第五步：应用插件。在需要应用的项目(不是插件项目，我这的是`app`)的build.gradle配置该插件 `apply plugin: 'log'`。不出意外,同步项目后
能在build日志找到对应的日志。一个简单的插件开发就完成了。


3、独立项目实现。   
前面2种方式都有一个缺点就是只能在自己项目中使用，别的项目无法使用。这对于开发来说无疑是一个很致命的缺陷。第三种方式就是其实就是为了解决这个缺陷。
它将插件发布到类似Maven，repo等仓库，别的项目通过依赖的方式即可使用。第三种方式其实就是在buildSrc的基础升级，多做一步上传的操作。

##### transform。
<https://www.jianshu.com/p/cf90c557b866>
[简单创建一个transform](Transform.md)

1、是什么?
前面提到plugin插件是用来定制一个功能，感觉吧像是一个工具库，接入方式上像第三方库。但是有时候想在插件中完成对项目内容的修改，这就需要
在gradle构建过程中，拿到源文件才行。这时，就要用到transform功能了。transform是Android官方提供给开发者在项目构建阶段(.class -->
.dex转换期间)用来修改.class文件的一套标准API，即把输入的.class文件转变成目标字节码文件。经典的应用有字节码插桩、代码注入(DI)等。学习
transform主要就是学习它API。

2、能干什么?   
参考上段描述。

3、怎么做?
- 1、添加依赖 `implementation 'com.android.tools.build:gradle:3.5.3'`
- 2、编写逻辑

4、为什么用?   
解决一些复杂技术痛点，提高效率

应用场景:

##### 1、全埋点
涉及比较广，其实就是全局事件监听，页面停留时间等等一系列的东西，用到的技术无非apt、apo、插桩等等

##### 2、AOP(切面编程，方法论)
[](构建技术之AOP.md)  

参考资料：<https://www.jianshu.com/p/9fb07b2596f7>