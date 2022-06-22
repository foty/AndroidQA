### Android下的网络

* 1、http
* 2、https
* 3、状态码 [状态码详细](状态码.md)
* 4、计算机网络模型


#### 1、Http

http，即超文本传输协议，基于TCP/IP协议传输数据。想了解http需要了解以下：
* URL
* 报文
* TCP/IP
* 报文传输过程

##### URL 
URL就是常说的链接。如：`http://192.168.1.1:8080/getData?name=xx&city=xx` 。这个例子基本就是URL的
模板，[http://<host>:<port>/<path>?<...>] ==> [http://<ip>:<端口>/<访问资源以及参数>]。另外就是像
`https://www.baidu.com/` 这样的没有明显显示ip、端口，在请求访问时，dns会将域名 www.baidu.com 解析成百度
真实的ip地址。域名与IP地址是N:1的关系。


##### 报文

报文就是一段描述请求信息的数据。有请求报文与响应报文。

**请求报文**   
包括3部分：请求行、请求头部、请求数据(请求体)。

* 请求行： 请求行分为三个部分：请求方法、请求地址和协议版本。
> 请求方法有8种：GET、POST、PUT、DELETE、PATCH、HEAD、OPTIONS；请求地址也就是 URL了，端口和路径有时可以省略；协议版本的格式为：HTTP/主版本号.次版本
> 号。常用的有HTTP/1.0和HTTP/1.1

* 请求头：为请求报文添加附加信息，由键值对组成，每行一对，键和值之间使用冒号分隔。如：Connection: keep-alive

* 请求数据：请求数据与请求头之间必须有个空行。表示请求头结束。请求数据可以为空(GET请求没有请求数据部分)。

如下面是一个完整请求报文(POST):
```textmate
POST [请求地址]　HTTP/1.1 　　 -----请求行
Host: 
Connection: keep-alive  -----请求头
Content-Length: 25
Content-Type: 
User-Agent: 
...(省略更多)
　　空行
username=xxx&sex=1234　　------请求数据
```

**响应报文**   
包括状态行、响应头部、响应正文。

* 状态行：包括：协议版本，状态码，状态描述。
> 协议版本同请求行的协议版本；状态码即1xx，2xx...([详细状态码](状态码.md))；状态描述也就是状态码的简单描述。如：OK

* 响应头部：与请求头部类似，都是由键值对组成，每行一对；

* 响应正文：返回给客户端的数据。

如下面是一个完整响应报文(POST):
```textmate
HTTP/1.1 200 OK　　-----状态行
Server: nginx/1.18.0      ------响应头部
Date: Mon, 20 Jun 2022 07:06:11 GMT　　
Transfer-Encoding: chunked
expires: Wed, 31 Dec 1969 23:59:59 GMT
Cache-Control: no-cache
Pragma: no-cache
Content-Length: 4396
Connection: Keep-Alive
Content-Type: application/json;charset=UTF-8
...(省略更多)
　　空行
{"code":"200","msg":"成功","data":"data"}　　--------响应数据
```




##### 报文传输过程
<https://www.jianshu.com/p/dd7d8d2e6b3d>

 报文传输步骤大概可分为下面几步：

* 解析Url：根据链接解析出ip与端口。解析使用DNS实现。
* 建立连接通道。知道了IP与端口后，服务器与客户端开始建立TCP连接。
* 相互发送报文。即发送数据到服务器。
* 服务器解析报文。
* 服务器准备响应数据。
* 服务器发送响应报文给客户端。
* 关闭连接。


#### Https


#### 计算机的网络模型
<https://blog.csdn.net/weixin_34179762/article/details/88729259>

通常来说，计算机中的网络模型比较流传的有OSI参考模型(7层)、TCP/IP模型(4层)、计算机教学版本(教科书)则是按照(5层)版本来讲授。

##### 1、OSI参考 7层模型

从上到下可分为：

* 应用层
* 表示层
* 会话层
* 传输层
* 网络层
* 数据链路(网络接口)层
* 物理层


##### 2、计算机教学版 5层模型

* 应用层
* 传输层
* 网络层
* 数据链路(网络接口)层
* 物理层


##### 3、TCP/IP 4层模型

即TCP/IP协议模型(Transmission Control Protocol/Internet Protocol)，包含了一系列构成互联网基础的网络协议，是Internet的核心协
议。TCP/IP的参考模型将协议分成四个层次，从上到下：
* 应用层
* 传输层
* 网络层
* 链路层(网络接口层)

> TCP/IP 也有5层模型的说法。模型与教学版一样，多了一个物理层。

##### 网络模型每个层次对应的协议
这里选举教学版5层网络模型，OSI参考模型中的 [应用层、表示层、会话层] 对应这里的 [应用层]。

模型层次  |  协议  
:----: |  :----: 
应用层  |  DNS、URI、HTTP、TSL/SSL、SSH、FTP... 
传输层 |  TCP、UDP、SCTP、...  
网络层 |  IP、ICMP、... 
链路(网络接口)层 | 网卡  
物理层 |  (硬件)  

> 这里网络层部分的IP是指IP协议，不是IP地址。

##### (网络层) IP协议

IP协议是TCP/IP协议的核心，所有的TCP，UDP等的数据都以IP数据包格式传输。IP不是可靠的协议，它提供无连接的、不可靠的连接。
[更加详细的IP协议介绍](https://blog.csdn.net/qq_18543557/article/details/118654885)

<br>

**IP地址**
> 在数据链路层中通过MAC地址来识别不同的节点，而在IP层我们也要有一个类似的地址标识，这就是IP地址。IP地址是32位。但是IP地址并不指向一台主机，它是指向一
> 个网络接口。如果一台主机在多个网络上，就会有多个网络接口，也就是有多个IP地址。因此路由器有多个IP地址。

<br>


##### (传输层) TCP/UDP协议

TCP/UDP都是传输层应用广泛的协议，俩者都有各自的特点。

特点 | TCP |  UDP |
:----: | :----:|:----:
可靠性 | 可靠 | 不可靠
连接性 | 面向连接 | 无连接
报文  | 面向字节流 | 面向报文
效率 | 传输效率低 | 传输效率高
传输速度 | 慢  |  快
应用场景 | 对效率要求不高，但是必须要准确，有连接的场景  |  对效率要求高，准确度低的场景

使用TCP/UDP协议场景

**TCP**

应用层协议 | 应用场景 
:----: | :----:
SMTP | 邮件
HTTP | 万维网
FTP | 文件传输

<br>

**UDP**

应用层协议 | 应用场景
:----: | :----:
DNS | 域名解析
TFTP | 文件传输
NFS | 远程文件服务器

<br>

##### TCP的连接 (3次握手、4次挥手)

TCP是面向连接的。


<br>

---

#### 问题
怎么判断弱网环境，然后怎么处理弱网情况
> 加大超时时间，自动重试，复合链接

dns优化和缓存

Http & Https的区别？
Https 的三次握手是怎样的过程？
为啥要用Https？
对称加密 & 非对称加密？
Http 1.0 vs Http 2.0?
为什么需要三次握手？两次会有什么问题？为什么需要四次挥手，两次行不行？
DNS有啥缺点？为啥国内要用HttpDNS?
网络如何分层的？5层分别是啥？为啥要做5层分层？每层都分别干啥事情？