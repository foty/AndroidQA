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

* 请求头：为请求报文添加附加信息，由键值对组成，每行一对，名和值之间使用冒号分隔。如：Connection: keep-alive

* 请求数据：请求数据与请求头之间必须有个空行。表示请求头结束。请求数据可以为空(GET请求没有请求数据)。

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


##### TCP/IP

即TCP/IP协议模型(Transmission Control Protocol/Internet Protocol)，包含了一系列构成互联网基础的网络协议，是Internet的核心协
议。TCP/IP的参考模型将协议分成四个层次，从下到上分别是链路层(网络接口层)、网络层、传输层和应用层。


**连接时的三次握手与断连时的4次挥手**



##### 报文传输过程


#### Https


#### 计算机的网络模型
<https://blog.csdn.net/weixin_34179762/article/details/88729259>


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