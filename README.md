# schat-client
schat client

### 说明
这是schat对应的客户端源码，包括了绝大部分逻辑，包括连接、加密和全部业务逻辑。少量的具体设置需要自己编译时和部署的schat服务端共同商定.

### 平台
* **Android**  目前只提供了android版本. 对应的apk文件下载:https://github.com/nmsoccer/schat-cli/releases 版本1.1

### 配置
src/asset/config文件进行了基本的配置。在编译配置时需要注意一些修改，基本内容如下：
```
app_name = schat
version = 0.0.4
dir_query_key = c_sssakb
#validate digest count if 0 no check
validate_digest = 2
pub_sha2_0 = b7718bd3d0612098a6290a0d503f3bb92cce39af1399f7fee48526095c996d46
pub_sha2_1 = a980b616ef3e781e3888d871835a1f710c0356915b48214d0a77d3377f2b765c
#https server self-signed cert
self_signed_cert_open = 1
bks_file = key.bks
bks_pass = xxxx
```

* **dir_query_key**   
  访问dir服务器的key，可以在自己所部署服务器的schat/servers/spush/tmpl/dir_serv.tmpl里找到对应的配置值
* **validate_digest**  
  访问schat服务的connect_serv公钥sha2摘要数目（只针对与connect_serv使用类型3加密，其他DES,AES加密的无需配置）。如果填0则表示无需校验
* **pub_sha2_xx**  
  访问schat服务的connect_serv公钥sha2摘要（只针对与connect_serv使用类型3加密，其他DES,AES加密的不会读取）。如果想要访问N个独立的schat服务那么应该配置N个
  摘要(一般只会有一个)，序号从0开始。 
  * 获得摘要的方法：进入自己部署的schat服务器schat/servers/connect_serv/cfg 执行``openssl dgst -sha256 rsa_public_key.pem`` 将获得的hash值填入即可.
* **self_signed_cert_open** 
  访问文件及dir等https服务器是否需要导入自签名证书。0则表示信任所有(一般没啥问题)，否则需要对服务器证书进行认证
* **bks_file**
  在self_signed_cert_open=1时起效，该文件表示导入服务器签发证书的keystore文件名，需要放入assets/目录下，即实际path为：assets/bks_file。导入服务器自签名证书步骤如下：
  * 服务器生成key及证书(这一步骤一般由部署服务器时完成)  
    进入服务器schat/servers/dir_serv/cfg
    ``openssl genrsa -out key.pem 2048``  
    ``openssl req -new -x509 -key key.pem -out cert.pem -days 3650``
  * 将服务器生成的cert.pem文件下载到android studio 所在本地  
  * 打开https://www.bouncycastle.org/latest_releases.html 页面下载BouncyCastle库，比如bcprov-jdk15on-167.jar到本地E:\software\
  * 打开cmd，执行keytool命令（keytool命令一般和javac在一个目录，安装了JDK就会有这玩意儿）
    ``keytool -importcert -trustcacerts -keystore key.bks -file cert.pem -storetype BKS -provider org.bouncycastle.jce.provider.BouncyCastleProvider -  providerpath E:\software\bcprov-jdk15on-167.jar ``
  * 这个过程会生成key.bks同时会要求输入store密码xxxx
  * 将key.bks填入bks_file，密码填入bks_pass
  * 将key.bks拷贝到config文件同级的assets/目录
