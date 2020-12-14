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
```

* **dir_query_key**   
  访问dir服务器的key，可以在自己所部署服务器的schat/servers/spush/tmpl/dir_serv.tmpl里找到对应的配置值
* **validate_digest**  
  访问schat服务的connect_serv公钥sha2摘要数目（只针对与connect_serv使用类型3加密，其他DES,AES加密的无需配置）。如果填0则表示无需校验
* **pub_sha2_xx**  
  访问schat服务的connect_serv公钥sha2摘要（只针对与connect_serv使用类型3加密，其他DES,AES加密的不会读取）。如果想要访问N个独立的schat服务那么应该配置N个
  摘要(一般只会有一个)，序号从0开始。 
  * 获得摘要的方法：进入自己部署的schat服务器schat/servers/connect_serv/cfg 执行``openssl dgst -sha256 rsa_public_key.pem`` 将获得的hash值填入即可.
