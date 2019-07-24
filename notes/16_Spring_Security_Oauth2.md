Spring Security Oauth2

## 一、用户认证需求分析

什么是用户身份认证？
用户身份认证即用户去访问系统资源时系统要求验证用户的身份信息，身份合法方可继续访问。常见的用户身份认
证表现形式有：用户名密码登录，指纹打卡等方式。
什么是用户授权？
用户认证通过后去访问系统的资源，**系统会判断用户是否拥有访问资源的权限，只允许访问有权限的系统资源，没**
**有权限的资源将无法访问，这个过程叫用户授权**。

**本项目包括多个子项目，如：学习系统，教学管理中心、系统管理中心等，为了提高用户体验性需要实现用户只认证一次便可以在多个拥有访问权限的系统中访问，这个功能叫做单点登录**。

![1560856852187](assets/1560856852187.png)



第三方登录认证: 一个微信用户没有在学成在线注册，本系统可以通过请求微信系统来验证该用户的身份，验证通过后该用户便可在
本系统学习，它的基本流程如下： 

![1560857201694](assets/1560857201694.png)

![1560857212944](assets/1560857212944.png)

## 二、用户认证技术方案-单点登录

分布式系统要实现单点登录，通常将认证系统独立抽取出来，并且将用户身份信息存储在单独的存储介质，比如：
MySQL、Redis，考虑性能要求，通常存储在Redis中，如下图：

![1560857250751](assets/1560857250751.png)

单点登录的特点是：
1、认证系统为独立的系统。
2、各子系统通过Http或其它协议与认证系统通信，完成用户认证。
3、用户身份信息存储在Redis集群。

Java中有很多用户认证的框架都可以实现单点登录：
1、Apache Shiro；    2、CAS；    3 、Spring security CAS

## 三、用户认证技术方案-Oauth2协议

  第三方认证技术方案最主要是解决认证协议的通用标准 问题，因为要实现 跨系统认证，各系统之间要遵循一定的接口协议。

第三方认证的技术方案协议: Oauth2。

![1560857780000](assets/1560857780000.png)

流程:

1、**客户端请求第三方授权**
用户进入XXX网站的登录页面，点击微信的图标以微信账号登录系统，用户是自己在微信里信息的资源拥有者。

2、**资源拥有者同意给客户端授权**
资源拥有者扫描二维码表示资源拥有者同意给客户端授权，微信会对资源拥有者的身份进行验证， 验证通过后，微信会询问用户是否给授权黑马程序员访问自己的微信数据，用户点击“确认登录”表示同意授权，微信认证服务器会颁发一个授权码，并重定向到XXX网站。

3、**客户端获取到授权码，请求认证服务器申请令牌**
此过程用户看不到，客户端应用程序请求认证服务器，请求携带授权码。
4、**认证服务器向客户端响应令牌**
认证服务器验证了客户端请求的授权码，如果合法则给客户端颁发令牌，令牌是客户端访问资源的通行证。
此交互过程用户看不到，当客户端拿到令牌后，用户在黑马程序员看到已经登录成功。
5、**客户端请求资源服务器的资源**
客户端携带令牌访问资源服务器的资源。XXX网站携带令牌请求访问微信服务器获取用户的基本信息。
6、**资源服务器返回受保护资源**
资源服务器校验令牌的合法性，如果合法则向用户响应资源信息内容。
注意：资源服务器和认证服务器可以是一个服务也可以分开的服务，如果是分开的服务资源服务器通常要请求认证
服务器来校验令牌的合法性。



**Oauth2包括以下角色**：
1、客户端
本身不存储资源，需要通过资源拥有者的授权去请求资源服务器的资源，比如：学成在线Android客户端、学成在
线Web客户端（浏览器端）、微信客户端等。
2、资源拥有者
通常为用户，也可以是应用程序，即该资源的拥有者。
3、授权服务器（也称认证服务器） 
用来对资源拥有的身份进行认证、对访问资源进行授权。客户端要想访问资源需要通过认证服务器由资源拥有者授
权后方可访问。
4、资源服务器
存储资源的服务器，比如，学成网用户管理服务器存储了学成网的用户信息，学成网学习服务器存储了学生的学习
信息，微信的资源服务存储了微信的用户信息等。客户端最终访问资源服务器获取资源信息。



**本项目使用Oauth2实现如下目标**：
1、学成在线访问第三方系统的资源
2、外部系统访问学成在线的资源
3、学成在线前端（客户端） 访问学成在线微服务的资源。
4、学成在线微服务之间访问资源，例如：微服务A访问微服务B的资源，B访问A的资源。

## 四、用户认证技术方案-SpringSecurityOauth2

本项目采用 Spring Security + Oauth2完成用户认证及用户授权，Spring Security 是一个强大的和高度可定制的身份验证和访问控制框架，Spring Security 框架集成了Oauth2协议，下图是项目认证架构图：

![1560858032872](assets/1560858032872.png)

1、用户请求认证服务完成认证。
2、认证服务下发用户身份令牌，拥有身份令牌表示身份合法。
3、用户携带令牌请求资源服务，请求资源服务必先经过网关。
4、网关校验用户身份令牌的合法，不合法表示用户没有登录，如果合法则放行继续访问。
5、资源服务获取令牌，根据令牌完成授权。
6、资源服务完成授权则响应资源信息。

## 五、SpringSecurityOauth2研究-搭建认证服务器

从资料中导入工程，并导入数据库。

![1560858074606](assets/1560858074606.png)

![1560858444167](assets/1560858444167.png)

## 六、SpringSecurityOauth2研究-Oauth2授权码模式-申请令牌 

授权码模式流程:

1、客户端请求第三方授权 

2、用户(资源拥有者)同意给客户端授权 

3、客户端获取到授权码，请求认证服务器申请
令牌 

4、认证服务器向客户端响应令牌 

5、客户端请求资源服务器的资源，资源服务校验令牌合法性，完成授权

6、资源服务器返回受保护资源



**获取授权码**

1、启动微服务 `UcenterAuthApplication`

2、使用Get请求得到授权码

```
localhost:40400/auth/oauth/authorize?client_id=XcWebApp&response_type=code&scop=app&redirect_uri=http://localhost
```

![1560860226394](assets/1560860226394.png)

![1560868920373](assets/1560868920373.png)

![1560868949799](assets/1560868949799.png)

```c
CUOfEf
```

3、由授权码得到令牌

![1560869202231](assets/1560869202231.png)



![1560869271710](assets/1560869271710.png)

得到结果:

```json
{
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6IlhjV2ViQXBwIiwic2NvcGUiOlsiYXBwIl0sIm5hbWUiOm51bGwsInV0eXBlIjpudWxsLCJpZCI6bnVsbCwiZXhwIjoxNTYwOTEyNTM2LCJqdGkiOiJmODExODA5MS0zYTZkLTQ1M2YtYjU5OC02Yjk4MWYxYWY5YTYiLCJjbGllbnRfaWQiOiJYY1dlYkFwcCJ9.HximmHTRcvrB7bgR-AYEbjy0eJs6AIg0mFs_FTQI9x2FLmXF5xSoMjMbUzI9paDIcJFJOAn0TrFH7O3Oc-cwotprOACSsZwDJQIBEk3eJmJAztYvsDWmJigC2siLUMwea8YpWMPS3v0jYSRjZnIw5D1bonyOsUjyaAnLlKvd7irh21F0u0MV3J5SeTBlTS-Sdn1KcrAajaZV9kYCif4QYhzOzqKsuVihIp6hIkLjpzPKdr0xF2Ig72mHH8TP57OQe7WnDCg-i_e7480WNWmnGpl6Qbowdaa8nuBnn0VFOyXdA-76fQIHYguJY3EJSKVq3AdotcjD7JJ9xSDhVzq9NA",
    "token_type": "bearer",
    "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOm51bGwsInVzZXJwaWMiOm51bGwsInVzZXJfbmFtZSI6IlhjV2ViQXBwIiwic2NvcGUiOlsiYXBwIl0sImF0aSI6ImY4MTE4MDkxLTNhNmQtNDUzZi1iNTk4LTZiOTgxZjFhZjlhNiIsIm5hbWUiOm51bGwsInV0eXBlIjpudWxsLCJpZCI6bnVsbCwiZXhwIjoxNTYwOTEyNTM2LCJqdGkiOiJkNjk2ZjRlYi00Y2NhLTRlYWUtODQ0Mi1kNTQ3ODM2ZWYyMzEiLCJjbGllbnRfaWQiOiJYY1dlYkFwcCJ9.QMsMujMsZlVyA_4lECku4MV5VWaKVDY4xcVSrRzdJXVz3xSnWlRMefxmzqybJ3HXY5EYEcAdW0P1Qu_Uv2RxUhBTd8i9wUcGCWV5LSwzaigJNla7BkYC2NQ1XVRiR_I5N8GkWXK6XrKPAsHCKASxD-8Q5D5Gi2z3zXyPqW2zOC4i7xRr_bfh9dcP3cYW9pRlPTRTO4wvxZTnQlc0SO-ikDRRE_vNb3PZ9iZRE-m6oIk4BrIpEcOGjN8HDxWdbO2KUXENJ4Q7ZqbgaslEkxyuAWcKlA0z6cS81Sbb5_95KeMEiaCESfaMrn7LP_oAbDNR63lNXhqLoZ6WR74NQO6WbA",
    "expires_in": 43198,
    "scope": "app",
    "jti": "f8118091-3a6d-453f-b598-6b981f1af9a6"
}
```



## 七、SpringSecurityOauth2研究-Oauth2授权码模式-资源服务授权测试

资源服务拥有要访问的受保护资源，客户端携带令牌访问资源服务，如果令牌合法则可成功访问资源服务中的资
源，如下图：

![1560871817538](assets/1560871817538.png)

1、客户端请求认证服务申请令牌
2、认证服务生成令牌
认证服务采用非对称加密算法，使用私钥生成令牌。
3、客户端携带令牌访问资源服务
客户端在Http header 中添加： Authorization：Bearer 令牌。
4、资源服务请求认证服务校验令牌的有效性
资源服务接收到令牌，使用公钥校验令牌的合法性。
5、令牌有效，资源服务向客户端响应资源信息



这里使用课程服务`xc-service-manage-course`来测试授权认证:

步骤: 

1、配置公钥
认证服务生成令牌采用非对称加密算法，认证服务采用私钥加密生成令牌，对外向资源服务提供公钥，资源服务使
用公钥 来校验令牌的合法性。
将公钥拷贝到 publickey.txt文件中，将此文件拷贝到资源服务工程的classpath下

![1560871990578](assets/1560871990578.png)

2、添加依赖(`xc-service-manage-course`服务中)

```xml
<!--资源服务中用到认证-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-oauth2</artifactId>
</dependency>
```

3、在config包下创建`ResourceServerConfig`类：

```java
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)//激活方法上的PreAuthorize注解
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    //公钥
    private static final String PUBLIC_KEY = "publickey.txt";

    //定义JwtTokenStore，使用jwt令牌
    @Bean
    public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    //定义JJwtAccessTokenConverter，使用jwt令牌
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setVerifierKey(getPubKey());
        return converter;
    }

    /**
     * 获取非对称加密公钥 Key
     * @return 公钥 Key
     */
    private String getPubKey() {
        Resource resource = new ClassPathResource(PUBLIC_KEY);
        try {
            InputStreamReader inputStreamReader = new
                    InputStreamReader(resource.getInputStream());
            BufferedReader br = new BufferedReader(inputStreamReader);
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException ioe) {
            return null;
        }
    }

    //Http安全配置，对每个到达系统的http请求链接进行校验
    @Override
    public void configure(HttpSecurity http) throws Exception {
        //所有请求必须认证通过
        http.authorizeRequests().anyRequest().authenticated();
    }
}
```



如果直接访问，会报错，没有认证:

![1560872796088](assets/1560872796088.png)

![1560872787314](assets/1560872787314.png)

在postman中加上参数就可以获取到:

即 在http header中添加 `Authorization： Bearer 令牌`  （注意Bearer后面跟上一个空格）

![1560872736899](assets/1560872736899.png)

## 八、SpringSecurityOauth2研究-解决swagger-ui无法访问

如果不进行配置，`swagger-ui`也无法访问，需要解决一下:

![1560873099584](assets/1560873099584.png)

修改授权配置类ResourceServerConfig的configure方法：
针对`swagger-ui`的请求路径进行放行：

```java
//Http安全配置，对每个到达系统的http请求链接进行校验
@Override
public void configure(HttpSecurity http) throws Exception {

    //http.authorizeRequests().anyRequest().authenticated(); //所有请求必须认证通过

    //所有请求必须认证通过 (解决swagger-ui也不能访问的问题)
    http.authorizeRequests()
        //下边的路径放行
        .antMatchers("/v2/api-docs", "/swagger-resources/configuration/ui",
                     "/swagger-resources","/swagger-resources/configuration/security",
                     "/swagger-ui.html","/webjars/**").permitAll()
        .anyRequest().authenticated();

}
```

重启测试:

![1560873312564](assets/1560873312564.png)

通过上边的配置虽然可以访问swagger-ui，但是无法进行单元测试，除非去掉认证的配置或在上边配置中添加所有
请求均放行（"/**"）。

## 九、SpringSecurityOauth2研究-Oauth2密码模式授权

密码模式（Resource Owner Password Credentials）与授权码模式的区别是申请令牌不再使用授权码，而是直接
通过用户名和密码即可申请令牌。

密码模式（Resource Owner Password Credentials）与授权码模式的区别是申请令牌不再使用授权码，而是直接
通过用户名和密码即可申请令牌。
测试如下：
Post请求：http://localhost:40400/auth/oauth/token
参数：
grant_type：密码模式授权填写password
username：账号
password：密码
并且此链接需要使用 http Basic认证。

![1560874124674](assets/1560874124674.png)

![1560874140112](assets/1560874140112.png)

## 十、SpringSecurityOauth2研究-校验令牌&刷新令牌

校验令牌:

![1560874214205](assets/1560874214205.png)

刷新令牌是当令牌快过期时重新生成一个令牌，它于授权码授权和密码授权生成令牌不同，刷新令牌不需要授权码
也不需要账号和密码，只需要一个刷新令牌、客户端id和客户端密码。

测试如下：
Post：http://localhost:40400/auth/oauth/token
参数：

grant_type： 固定为 refresh_token
refresh_token：刷新令牌（**注意不是access_token，而是refresh_token**）

![1560874370526](assets/1560874370526.png)

## 十一、SpringSecurityOauth2研究-JWT研究-JWT介绍

传统校验令牌的方法，如下图：

![1560921901861](assets/1560921901861.png)

传统授权方法的问题是用户每次请求资源服务，资源服务都需要携带令牌访问认证服务去校验令牌的合法性，并根
据令牌获取用户的相关信息，性能低下。

使用JWT的思路是，用户认证通过会得到一个JWT令牌，JWT令牌中已经包括了用户相关的信息，客户端只需要携带
JWT访问资源服务，资源服务根据事先约定的算法自行完成令牌校验，无需每次都请求认证服务完成授权。
JWT令牌授权过程如下图：

![1560922159453](assets/1560922159453.png)

JWT令牌的优点：
1、jwt基于json，非常方便解析。
2、可以在令牌中自定义丰富的内容，易扩展。
3、通过**非对称加密算法**及数字签名技术，JWT防止篡改，安全性高。
4、资源服务使用JWT可不依赖认证服务即可完成授权。
缺点：
１、JWT令牌较长，占存储空间比较大。



JWT令牌由三部分组成，每部分中间使用点（.）分隔，比如：xxxxx.yyyyy.zzzzz

* Header： 头部包括令牌的类型（即JWT）及使用的哈希算法（如HMAC SHA256或RSA）
* Payload ： 第二部分是负载，内容也是一个json对象，**它是存放有效信息的地方**(用户信息)，它可以存放jwt提供的现成字段
* Signature：第三部分是签名，此部分用于防止jwt内容被篡改。



## 十二、SpringSecurityOauth2研究-JWT研究-生成私钥和公钥

1、生成私钥

```shell
keytool -genkeypair -alias xckey -keyalg RSA -keypass xuecheng -keystore xc.keystore -storepass xuechengkeystore
```

![1560922950207](assets/1560922950207.png)

查看证书信息

```shell
keytool -list -keystore xc.keystore
```

![1560923027691](assets/1560923027691.png)

2、导出公钥:

安装openSSL: <http://slproweb.com/products/Win32OpenSSL.html>

![1560923623723](assets/1560923623723.png)

![1560923656582](assets/1560923656582.png)

![1560923824969](assets/1560923824969.png)

```c
D:\xcEduService01\jwt>keytool -list -rfc --keystore xc.keystore | openssl x509 -inform pem -pubkey
输入密钥库口令:  xuechengkeystore
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjnyoVu8WCrDrgbfnD2lp
B3FnBhN1Kb9ZqKGR8J4Y2vAR4gM7iOgOO94eTE0/sMXy3usp/mfh+7Oco2/5d6GU
QbIp0oMm/XjUVaeKcinT3XVfgz97c9RTTPcefHY5g2kkWhtYoZFG6IDPkxjJ7BFc
MGG5ZKfBK9z21jjdhGPjXCCNNatyhr8INKPsQMJbupKKMObGgR66nm1cF13CQP3O
CgMo2tHPMINv/txp5natJI7Kb53b6t7ycyP5/KFSYKIxofA5kX9tpSeEpoGjFu/o
Vm68Tg1pnC+Du2ziYkk2iG+Ae5nryRAjbmCWL56wBDgKkWJ9zddKt/URMRBOVomr
6QIDAQAB
-----END PUBLIC KEY-----
-----BEGIN CERTIFICATE-----
MIIDazCCAlOgAwIBAgIEfrTtpDANBgkqhkiG9w0BAQsFADBmMQswCQYDVQQGEwJj
bjERMA8GA1UECBMIY2hhbmdzaGExETAPBgNVBAcTCGNoYW5nc2hhMQ8wDQYDVQQK
EwZ6eHp4aW4xDzANBgNVBAsTBnp4enhpbjEPMA0GA1UEAxMGenh6eGluMB4XDTE5
MDYxOTA1NDA0OVoXDTE5MDkxNzA1NDA0OVowZjELMAkGA1UEBhMCY24xETAPBgNV
BAgTCGNoYW5nc2hhMREwDwYDVQQHEwhjaGFuZ3NoYTEPMA0GA1UEChMGenh6eGlu
MQ8wDQYDVQQLEwZ6eHp4aW4xDzANBgNVBAMTBnp4enhpbjCCASIwDQYJKoZIhvcN
AQEBBQADggEPADCCAQoCggEBAI58qFbvFgqw64G35w9paQdxZwYTdSm/WaihkfCe
GNrwEeIDO4joDjveHkxNP7DF8t7rKf5n4fuznKNv+XehlEGyKdKDJv141FWninIp
0911X4M/e3PUU0z3Hnx2OYNpJFobWKGRRuiAz5MYyewRXDBhuWSnwSvc9tY43YRj
41wgjTWrcoa/CDSj7EDCW7qSijDmxoEeup5tXBddwkD9zgoDKNrRzzCDb/7caeZ2
rSSOym+d2+re8nMj+fyhUmCiMaHwOZF/baUnhKaBoxbv6FZuvE4NaZwvg7ts4mJJ
Warning:
JKS 密钥库使用专用格式。建议使用 "keytool -importkeystore -srckeystore xc.keystore -destkeystore xc.keystore -deststoretype pkcs12" 迁NohvgHuZ68kQI25gli+esAQ4CpFifc3XSrf1ETEQTlaJq+kCAwEAAaMhMB8wHQYD
VR0OBBYEFMFAavkuS4imfgayU1k9gZh95EC/MA0GCSqGSIb3DQEBCwUAA4IBAQAO
OMmIdvF2Mpnq2cXYUMYJ+8Oie1dvIhJKpTkUn1L9+CW1rdTrmbv8x0XUw2kQd2RW
l3m2W9AhwxCY/wD9/yc/dI3TWPzausmxHXsk8N0HC/B9mIUO1Mw8BJo4L53NLnjf
Z2SRcQ2eci7Y5XwCBEWIOaUjAanRdvggMufn7SCqvIGypm5An6HD7LEyCyNtDphy
Zc0w1MA8cEKi+LrH+lmGo8SSElVyoZzgFhAaNJnCFSA6T1k9WrO8VV7fQ8Pf0q3e
dsiDB7LJl2F9wmoyVrMCTu30HWwXLFy//eNGjjK4u+yNQEWTlYwHXMEyqwR8n/7f
MxryhwXEKlg6YoLSdB6g
-----END CERTIFICATE-----
```



**然后将私钥导入到认证服务`xc-service-ucenter-auth`以及将公钥导入到`xc-service-manage-course`(资源服务)**



1、将公钥拷贝到资源服务类路径中的`publickey.txt`文件中。

![1560924003417](assets/1560924003417.png)

2、将之前的私钥文件放到认证服务中。

![1560924120257](assets/1560924120257.png)



## 十三、SpringSecurityOauth2研究-JWT研究-生成JWT令牌&验证JWT令牌

在`xc-service-ucenter-auth`中测试。

生成jwt令牌:

```java
@Test
public void testJWT(){
    //证书文件
    String key_location = "xc.keystore"; // 类路径下，就是刚刚生成的私钥的文件
    //密钥库密码
    String keystore_password = "xuechengkeystore";
    //访问证书路径
    ClassPathResource resource = new ClassPathResource(key_location);
    //密钥工厂
    KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(resource,
                                                                   keystore_password.toCharArray());

    //密钥的密码，此密码和别名要匹配
    String keyPassword = "xuecheng";
    //密钥别名
    String alias = "xckey";
    //密钥对（密钥和公钥）
    KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias,keyPassword.toCharArray());
    //私钥
    RSAPrivateKey aPrivate = (RSAPrivateKey) keyPair.getPrivate();


    //定义payload信息 (jwt第二部分的内容)
    Map<String, Object> tokenMap = new HashMap<>();
    tokenMap.put("id", "123");
    tokenMap.put("name", "mrt");
    tokenMap.put("roles", "r01,r02");
    tokenMap.put("ext", "1");


    //可以生成JWT令牌了
    Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(aPrivate));

    //取出jwt令牌
    String token = jwt.getEncoded();
    System.out.println("token = "+token);
}
```

测试结果: 

![1560925018956](assets/1560925018956.png)

运行结果（生成的jwt令牌）内容:

```java
token = eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHQiOiIxIiwicm9sZXMiOiJyMDEscjAyIiwibmFtZSI6Im1ydCIsImlkIjoiMTIzIn0.ajVHKM2Dts5zVpBUoIIStPQ1aWTVlEeF1hw7DcGKINt4Wj6wgfkxTRHbt7s4x42-w0BCfNMpf4b5wjXEhH9NTPT-BxsxznJQxJiEnqAn8RtbdWTDel1LjTlgm9PQ4uiLD-ksoKqgN8gsypcJAuw_nokI463cif8ueEAW5SqujSNMJExI9MkZBBf4TVAX1Fjq42UUFWpdGbM-WnPi3bwDqE2palKrmEUtHI1AixBWCuDyfT-Zxk6TISoDsY6qGfzR7O3GUfnq2SIfE63UxDMAREAloBgveHSH4Wjtik7vLMOUs9n-Y6yXmcSUIt2FdZWTLs03IRi1pRzb7L8DBul5zw
```



使用公钥进行校验，此时**将刚刚用openSSL生成的公钥(刚刚已经拷贝到了`xc-service-manage-course`中)**的内容拷贝出来进行校验:

```java
  //使用公钥校验JWT令牌
    @Test
    public void testVerifyJWT(){
        //jwt令牌
        String jwtString = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHQiOiIxIiwicm9sZXMiOiJyMDEscjAyIiwibmFtZSI6Im1ydCIsImlkIjoiMTIzIn0.ajVHKM2Dts5zVpBUoIIStPQ1aWTVlEeF1hw7DcGKINt4Wj6wgfkxTRHbt7s4x42-w0BCfNMpf4b5wjXEhH9NTPT-BxsxznJQxJiEnqAn8RtbdWTDel1LjTlgm9PQ4uiLD-ksoKqgN8gsypcJAuw_nokI463cif8ueEAW5SqujSNMJExI9MkZBBf4TVAX1Fjq42UUFWpdGbM-WnPi3bwDqE2palKrmEUtHI1AixBWCuDyfT-Zxk6TISoDsY6qGfzR7O3GUfnq2SIfE63UxDMAREAloBgveHSH4Wjtik7vLMOUs9n-Y6yXmcSUIt2FdZWTLs03IRi1pRzb7L8DBul5zw";

        //公钥 (使用openSSL生成的， 在xc-service-manage-course下的publickey.txt下有，从那里拷贝即可)
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjnyoVu8WCrDrgbfnD2lpB3FnBhN1Kb9ZqKGR8J4Y2vAR4gM7iOgOO94eTE0/sMXy3usp/mfh+7Oco2/5d6GUQbIp0oMm/XjUVaeKcinT3XVfgz97c9RTTPcefHY5g2kkWhtYoZFG6IDPkxjJ7BFcMGG5ZKfBK9z21jjdhGPjXCCNNatyhr8INKPsQMJbupKKMObGgR66nm1cF13CQP3OCgMo2tHPMINv/txp5natJI7Kb53b6t7ycyP5/KFSYKIxofA5kX9tpSeEpoGjFu/oVm68Tg1pnC+Du2ziYkk2iG+Ae5nryRAjbmCWL56wBDgKkWJ9zddKt/URMRBOVomr6QIDAQAB-----END PUBLIC KEY-----";
        //校验jwt  (如果出错，就是校验失败)
        Jwt jwt = JwtHelper.decodeAndVerify(jwtString, new RsaVerifier(publickey));

        //拿到当初jwt中自定义的内容(第二部分)

        //获取jwt原始内容
        String claims = jwt.getClaims();
        System.out.println(claims); //校验失败

        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }
```

运行结果:

![1560925491574](assets/1560925491574.png)

## 十四、认证接口开发-需求分析

认证接口需求分析:

![1560927437920](assets/1560927437920.png)

执行流程：
1、用户登录，请求认证服务
2、认证服务认证通过，生成jwt令牌，将jwt令牌及相关信息写入Redis，并且将身份令牌写入cookie
3、用户访问资源页面，带着cookie到网关
4、网关从cookie获取token，并查询Redis校验token,如果token不存在则拒绝访问，否则放行
5、用户退出，请求认证服务，清除redis中的token，并且删除cookie中的token
使用redis存储用户的身份令牌有以下作用：
1、实现用户退出注销功能，服务端清除令牌后，即使客户端请求携带token也是无效的。
2、由于jwt令牌过长，不宜存储在cookie中，所以将jwt令牌存储在redis，由客户端请求服务端获取并在客户端存
储。

![1560927625134](assets/1560927625134.png)



## 十五、认证接口开发-Redis配置



下载安装 `Redis`:

<https://github.com/microsoftarchive/redis/releases/tag/win-3.2.100>

解压下载的`zip`包:

![1560929382798](assets/1560929382798.png)

进入目录，启动redis服务:

```shell
redis-server redis.windows.conf
```

![1560929425537](assets/1560929425537.png)

注册redis为服务:

```shell
redis-server --service-install redis.windows-service.conf --loglevel verbose
```

![1560929527030](assets/1560929527030.png)

常用的redis服务命令如下：
进入redis安装目录：
卸载服务：`redis-server.exe --service-uninstall`
开启服务：`redis-server.exe --service-start`
停止服务：`redis-server.exe --service-stop`



安装客户端管理工具并测试: `Redis Desktop Manager`。

测试:

![1560929932909](assets/1560929932909.png)



在认证服务配置`redis`:

![1560931179185](assets/1560931179185.png)

程序测试Redis：

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRedis {


    //使用Spring提供的Redis工具
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedis(){
        //定义key
        String key = "user_token:9734b68f-cf5e-456f-9bd6-df578c711390";

        //定义Map (value)
        Map<String,String> mapValue = new HashMap<>();
        mapValue.put("id","101");
        mapValue.put("username","zxin");
        String value = JSON.toJSONString(mapValue);

        //向redis中存储字符串
        stringRedisTemplate.boundValueOps(key).set(value,60, TimeUnit.SECONDS); // 60秒过期时间

        //读取过期时间，已过期返回‐2
        Long expire = stringRedisTemplate.getExpire(key);

        //根据key获取value
        String s = stringRedisTemplate.opsForValue().get(key);
        System.out.println(s);
    }

}

```

测试结果:

![1560931424931](assets/1560931424931.png)

## 十六、认证接口开发-Api接口定义

认证服务需要实现的功能如下：
1、登录接口
前端post提交账号、密码等，用户身份校验通过，生成令牌，并将令牌存储到redis。
将令牌写入cookie。
2、退出接口
校验当前用户的身份为合法并且为已登录状态。
将令牌从redis删除。
删除cookie中的令牌。
业务流程如下：

![1560931760637](assets/1560931760637.png)

定义API:

```java
@Api(value = "用户认证", tags = "用户认证接口")
public interface AuthControllerApi {

    /**
     * 请求: LoginRequest : 账号、密码、验证码
     * 相应: LoginResult :  操作码、令牌号
     * @param loginRequest
     * @return
     */
    @ApiOperation("登录")
    LoginResult login(LoginRequest loginRequest);

    /**
     * 1、在Redis中请求令牌
     * 2、清除Cookie
     * @return
     */
    @ApiOperation("退出")
    ResponseResult logout();
}
```

在接口内部要调用`Spring Security`来申请令牌，颁发令牌后，我们还需要自己调用redis进行保存。

## 十七、认证接口开发-申请令牌测试

为了不破坏Spring Security的代码，我们在Service方法中通过RestTemplate请求Spring Security所暴露的申请令
牌接口来申请令牌，下边是测试代码：

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestCallSecurity {

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    RestTemplate restTemplate;

    //远程请求Spring Security远程请求令牌
    @Test
    public void testClient(){
        //这里使用LoadBalance，可以直接从Eureka中获取对应的服务(因为Spring Security在认证服务中)
        //从Eureka中获取认证服务的一个实例的地址
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        //获取uri 即: http://ip:port
        URI uri = serviceInstance.getUri();
        //得到令牌的地址: http://localhost:40400/auth/oauth/token
        String authUrl = uri + "/auth/oauth/token";

        //下面要开始请求了，但是需要带上Header和Body，所以需要先定义Header和Body

        // (1)、定义Header
        LinkedMultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        String httpBasic = getHttpBasic("XcWebApp", "XcWebApp");
        header.add("Authorization", httpBasic);
        // (2)、定义Body
        LinkedMultiValueMap<String, String>body = new LinkedMultiValueMap<>();
        body.add("grant_type","password");
        //暂时写死在  认证服务的UserDetailsServiceImpl中的，后面要写到数据库的
        body.add("username","itcast");
        body.add("password","123");


        //组装一个Http实例，将body和header放到其中
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, header);


        //因为如果密码/用户名错误，就会抛出异常，终止返回，返回结果得不到信息(错误信息)，所以这里设置(400/401也要返回信息(错误信息))
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            //让400/401不报错，也要返回数据
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if(response.getRawStatusCode()!=400 && response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });

        //可以开始调用了
        //调用  参数: String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, httpEntity, Map.class);

        //获取申请令牌信息
        Map jwtMsg = exchange.getBody();
        System.out.println(jwtMsg);
    }

    //即获取到  由格式  "用户名:密码"  的base64编码  （HttpBasic认证的方法）
    private String getHttpBasic(String clientId, String clientSecret) {
        String str = clientId + ":" + clientSecret;
        byte[] encode = Base64Utils.encode(str.getBytes());
        return "Basic " + new String(encode) ; // 提交请求的格式   "Basic 编码"    (注意有一个空格)
    }
}
```

结果:

![1560935781565](assets/1560935781565.png)

**注意这里的Http Basic使用Base64编码(以及格式(用户名:密码))的理由**:

![1560934522270](assets/1560934522270.png)

## 十八、认证接口开发-接口开发-service

Service做两件事:

* 1、向Spring Security申请令牌  (上一步已经测试过)
* 2、将令牌存到Redis（这里将短的身份令牌作为key，整个令牌json作为value存入）



```java
@Service
@Slf4j
public class AuthService {

    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    LoadBalancerClient loadBalancerClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    /**
     * 1、申请令牌  (之前在test包中已经测试过)
     * 2、将令牌存储到 redis  (注意是整个content)
     */
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        //申请令牌
        AuthToken authToken = applyToken(username, password, clientId, clientSecret);
        if (authToken == null) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }

        //将 token存储到redis
        String access_token = authToken.getAccess_token(); // user_token : 短的身份(jtl作为key) ，整个令牌(body都存到redis)

        String content = JSON.toJSONString(authToken);

        //存入redis
        boolean saveTokenResult = saveToken(access_token, content, tokenValiditySeconds);
        if (!saveTokenResult) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }

    //认证方法
    private AuthToken applyToken(String username, String password, String clientId, String
            clientSecret) {

        //选中认证服务的地址
        ServiceInstance serviceInstance =
                loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        if (serviceInstance == null) {
            log.error("choose an auth instance fail");
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_AUTHSERVER_NOTFOUND);
        }
        //获取令牌的url
        String path = serviceInstance.getUri().toString() + "/auth/oauth/token";
        //1、定义body
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password"); //授权方式
        formData.add("username", username);  //账号
        formData.add("password", password);  //密码
        //2、定义头
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add("Authorization", httpbasic(clientId, clientSecret));
        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if (response.getRawStatusCode() != 400 && response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });
        Map map = null;
        try {
            //http请求spring security的申请令牌接口
            ResponseEntity<Map> mapResponseEntity = restTemplate.exchange(path,
                    HttpMethod.POST, new HttpEntity<>(formData, header), Map.class);

            map = mapResponseEntity.getBody(); //得到结果

        } catch (RestClientException e) {
            e.printStackTrace();
            log.error("request oauth_token_password error: {}", e.getMessage());
            e.printStackTrace();
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }

        //判断
        if (map == null ||
                map.get("access_token") == null ||
                map.get("refresh_token") == null ||
                map.get("jti") == null) {  //jti是jwt令牌的唯一标识作为用户身份令牌
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }

        AuthToken authToken = new AuthToken();

        //访问令牌(jwt)  长的那个
        authToken.setJwt_token((String) map.get("access_token"));

        //jti，作为用户的身份标识 （短的那个，用来做redis的key，并存到cookie中）
        authToken.setAccess_token((String) map.get("jti"));

        //刷新令牌
        authToken.setRefresh_token((String) map.get("refresh_token"));

        return authToken;
    }

    //存储令牌到redis
    private boolean saveToken(String access_token, String content, long ttl) {
        //令牌名称
        String key = "user_token:" + access_token; // key
        //保存到令牌到redis
        stringRedisTemplate.boundValueOps(key).set(content, ttl, TimeUnit.SECONDS);
        //获取过期时间
        Long expire = stringRedisTemplate.getExpire(key);

        return expire > 0; // 存储成功expire会 > 0
    }

    //获取httpbasic认证串
    private String httpbasic(String clientId, String clientSecret) {
        //将客户端id和客户端密码拼接，按“客户端id:客户端密码”
        String string = clientId + ":" + clientSecret;
        //进行base64编码
        byte[] encode = Base64Utils.encode(string.getBytes());
        return "Basic " + new String(encode);
    }
}
```



## 十九、认证接口开发-接口开发-controller

Controller主要是要注意，将那个**短的身份令牌**存到Cookie。

```java
@RestController
public class AuthController implements AuthControllerApi {

    @Value("${auth.clientId}")
    String clientId;

    @Value("${auth.clientSecret}")
    String clientSecret;

    @Value("${auth.cookieDomain}")
    String cookieDomain;

    @Value("${auth.cookieMaxAge}")
    int cookieMaxAge;

    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    AuthService authService;

    @Override
    @PostMapping("/userlogin")
    public LoginResult login(LoginRequest loginRequest) {
        //校验账号是否输入
        if (loginRequest == null || StringUtils.isEmpty(loginRequest.getUsername())) {
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        //校验密码是否输入
        if (StringUtils.isEmpty(loginRequest.getPassword())) {
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        //申请令牌
        AuthToken authToken = authService.login(loginRequest.getUsername(),
                loginRequest.getPassword(), clientId, clientSecret);

        //将令牌写入cookie
        //得到token (那个短的令牌，存到Cookie)
        String access_token = authToken.getAccess_token();

        //将访问令牌存储到cookie
        saveCookie(access_token);

        return new LoginResult(CommonCode.SUCCESS, access_token);
    }

    //将令牌保存到cookie
    private void saveCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getResponse();
        //添加cookie 认证令牌，最后一个参数设置为false，表示允许浏览器获取
        // HttpServletResponse response,String domain,String path, String name,  String value, int maxAge,boolean httpOnly
        CookieUtil.addCookie(response, cookieDomain, "/", "uid", token, cookieMaxAge, false);
    }


    @Override
    public ResponseResult logout() {
        return null;
    }
}
```



## 二十、认证接口开发-接口测试



使用postman测试:

认证服务默认都要校验用户的身份信息，这里需要将登录url放行。

在WebSecurityConfig类中重写 configure(WebSecurity web)方法，如下：

![1560946537421](assets/1560946537421.png)

结果:

![1560947026365](assets/1560947026365.png)

![1560947058288](assets/1560947058288.png)



测试写入Cookie:

cookie最终会写到xuecheng.com域名下，可通过nginx代理进行认证，测试cookie是否写成功。
**配置nginx代理**
在`ucenter.xuecheng.com`下配置代理路径:

![1560947163908](assets/1560947163908.png)

![1560947209142](assets/1560947209142.png)

```properties
	#认证
    location ^~ /openapi/auth/ {
    	proxy_pass http://auth_server_pool/auth/;
    }
	#认证服务
	upstream auth_server_pool{
		server 127.0.0.1:40400 weight=10;
	}
```

测试Cookie （**这里nginx感觉出了点bug，一开始不行，然后后面突然又可以了**）:

![1560949406151](assets/1560949406151.png)

![1560949428371](assets/1560949428371.png)