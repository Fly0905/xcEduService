# 用户认证 Zuul

## 一、用户认证-用户认证流程分析

![1560951148484](assets/1560951148484.png)

业务流程说明如下：

1、客户端请求认证服务进行认证。
2、认证服务认证通过向浏览器cookie写入token(身份令牌)

认证服务请求用户中心查询用户信息。
认证服务请求Spring Security申请令牌。
认证服务将token(身份令牌)和jwt令牌存储至redis中。
认证服务向cookie写入 token(身份令牌)。

3、前端携带token请求认证服务获取jwt令牌

前端获取到jwt令牌并存储在sessionStorage。
前端从jwt令牌中解析中用户信息并显示在页面。

4、前端携带cookie中的token身份令牌及jwt令牌访问资源服务

前端请求资源服务需要携带两个token，一个是cookie中的身份令牌，一个是http header中的jwt令牌
前端请求资源服务前在http header上添加jwt请求资源

5、网关校验token的合法性

用户请求必须携带token身份令牌和jwt令牌
网关校验redis中token是否合法，已过期则要求用户重新登录

6、资源服务校验jwt的合法性并完成授权
资源服务校验jwt令牌，完成授权，拥有权限的方法正常执行，没有权限的方法将拒绝访问。



## 二、用户认证-认证服务查询数据库-需求分析&搭建环境

认证服务根据数据库中的用户信息去校验用户的身份，即校验账号和密码是否匹配。
认证服务不直接连接数据库，而是通过用户中心服务去查询用户中心数据库。

![1547174362353](assets/1547174362353.png)

完整的流程图如下：

![1560953893486](assets/1560953893486.png)

创建xc_user数据库（MySQL）
导入`xc_user.sql`(已导入不用重复导入 )

导入“资料”-》`xc-service-ucenter.zip`

![1560954476832](assets/1560954476832.png)

## 三、用户认证-认证服务查询数据库-查询用户接口-接口定义 

完成用户中心根据**账号**查询用户信息接口功能。

此接口将来被用来查询用户信息及用户权限信息，所以这里定义扩展类型。

```java
@Data
@ToString
public class XcUserExt extends XcUser { //注意继承自XcUser

    //权限信息
    private List<XcMenu> permissions;

    //企业信息
    private String companyId;
}

```

接口如下:

```java
@Api(value = "用户中心", tags = "用户中心管理")
public interface UcenterControllerApi {

    @ApiOperation("根据用户账号查询用户信息")
    XcUserExt getUserExt(String username);

}
```

## 四、用户认证-认证服务查询数据库-查询用户接口-接口开发

根据账号查询用户信息。

两个DAO

```java
public interface XcUserRepository extends JpaRepository<XcUser, String> {
	XcUser findXcUserByUsername(String username);
}

public interface XcCompanyUserRepository extends JpaRepository<XcCompanyUser,String> {

    //根据用户id查询所属企业id
    XcCompanyUser findByUserId(String userId);

}
```



![1547175715909](assets/1547175715909.png)



`Service`:

```java
@Service
public class UserService {

    @Autowired
    XcUserRepository xcUserRepository;

    @Autowired
    XcCompanyUserRepository xcCompanyUserRepository;

    //根据用户账号查询用户信息
    public XcUser findXcUserByUsername(String username){
        return xcUserRepository.findXcUserByUsername(username);
    }

    public XcUserExt getUserExt(String username) {
        XcUser xcUser = findXcUserByUsername(username);
        if(xcUser == null){
            return null;
        }
        String userId = xcUser.getId();

        XcCompanyUser xcCompanyUser = xcCompanyUserRepository.findByUserId(userId);

        String companyId = null;
        if(xcCompanyUser != null) {
            companyId = xcCompanyUser.getCompanyId();

        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser, xcUserExt);
        xcUserExt.setCompanyId(companyId);
        return xcUserExt;
    }
}
```

`Controller`:

```java
@RestController
@RequestMapping("/ucenter")
public class UcenterController implements UcenterControllerApi {

    @Autowired
    UserService userService;

    @Override
    @GetMapping("/getuserext")
    public XcUserExt getUserExt(@RequestParam("username") String username) {
        XcUserExt xcUser = userService.getUserExt(username);
        return xcUser;
    }
}
```



简单测试:

![1560959271273](assets/1560959271273.png)

## 五、用户认证-认证服务查询数据库-调用查询用户接口



在认证服务`xc-service-ucenter-auth`中调用`xc-service-ucenter`根据账号查询用户信息:

feign远程调用:

```java

@FeignClient(XcServiceList.XC_SERVICE_UCENTER)
public interface UserClient {

    @GetMapping("/ucenter/getuserext")
    XcUserExt getUserext(@RequestParam("username") String username);
}

```

`UserDetailsServiceImpl`代码修改:

认证服务调用`spring security`接口申请令牌，spring security接口会调用`UserDetailsServiceImpl`从数据库查询用户，如果查询不到则返回 NULL，表示不存在；在`UserDetailsServiceImpl`中将正确的密码返回， spring security
会自动去比对输入密码的正确性。
1、修改`UserDetailsServiceImpl`的`loadUserByUsername`方法，**调用Ucenter服务的查询用户接口**

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    UserClient userClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //取出身份，如果身份为空说明没有认证
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //没有认证统一采用httpbasic认证，httpbasic中存储了client_id和client_secret，开始认证client_id和client_secret
        if (authentication == null) {
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(username);
            if (clientDetails != null) {
                //密码
                String clientSecret = clientDetails.getClientSecret();
                return new User(username, clientSecret, AuthorityUtils.commaSeparatedStringToAuthorityList(""));
            }
        }
        if (StringUtils.isEmpty(username)) {
            return null;
        }

        //远程调用 用户中心(xc-service-ucenter) 根据账号(username) 查询用户信息 (通过feign调用)
        //请求ucenter查询用户
        XcUserExt userext = userClient.getUserext(username);
        if(userext == null){
        //返回NULL表示用户不存在，Spring Security会抛出异常
            return null;
        }
        //从数据库查询用户正确的密码，Spring Security会去比对输入密码的正确性
        String password = userext.getPassword();
//        XcUserExt userext = new XcUserExt();
//        userext.setUsername("itcast");
//        userext.setPassword(new BCryptPasswordEncoder().encode("123"));
        userext.setPermissions(new ArrayList<>()); //TODO 权限先用静态的
//        if (userext == null) {
//            return null;
//        }
        //取出正确密码（hash值）
        //这里暂时使用静态密码
//       String password ="123";
        //用户权限，这里暂时使用静态数据，最终会从数据库读取
        //从数据库获取权限
        List<XcMenu> permissions = userext.getPermissions();
        List<String> user_permission = new ArrayList<>();
        permissions.forEach(item -> user_permission.add(item.getCode()));
//        user_permission.add("course_get_baseinfo");
//        user_permission.add("course_find_pic");
        String user_permission_string = StringUtils.join(user_permission.toArray(), ",");
        UserJwt userDetails = new UserJwt(username,
                password,
                AuthorityUtils.commaSeparatedStringToAuthorityList(user_permission_string));
        userDetails.setId(userext.getId());
        userDetails.setUtype(userext.getUtype());//用户类型
        userDetails.setCompanyId(userext.getCompanyId());//所属企业
        userDetails.setName(userext.getName());//用户名称
        userDetails.setUserpic(userext.getUserpic());//用户头像
       /* UserDetails userDetails = new org.springframework.security.core.userdetails.User(username,
                password,
                AuthorityUtils.commaSeparatedStringToAuthorityList(""));*/
//                AuthorityUtils.createAuthorityList("course_get_baseinfo","course_get_list"));
        return userDetails;
    }
}

```

修改部分:

![1560960438963](assets/1560960438963.png)



测试:

![1560960696255](assets/1560960696255.png)

(别人的) (密码也是111111)

![1547180336983](assets/1547180336983.png)



## 六、用户认证-认证服务查询数据库-Bcrypt介绍

测试Bcrypt加密: 每次都是一个随机的盐，即使密码相同，生成的串不同:

```javascript
/**
 * 测试使用BCrypt加密
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestBCrypt {

    @Test
    public void testPasswordEncoder() {
        String password = "111111";
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        for (int i = 0; i < 10; i++) {
            //每个计算出的Hash值都不一样
            String hashPass = passwordEncoder.encode(password);
            System.out.println(hashPass);
            //虽然每次计算的密码Hash值不一样但是校验是通过的
            boolean f = passwordEncoder.matches(password, hashPass);
            System.out.println(f);
        }
    }
}
```

![1560990552551](assets/1560990552551.png)

加载到Spring中:

![1547192256312](assets/1547192256312.png)



> 补充Bcrypt原理:
>
> * 虽然对同一个密码，每次生成的hash不一样，**但是hash中包含了salt**（hash产生过程：先随机生成salt，salt跟password进行hash）；
> * 在下次校验时，**从hash中取出salt**，salt跟password进行hash；得到的结果跟保存在DB中的hash进行比对，compareSync中已经实现了这一过程：bcrypt.compareSync(password, hashFromDB);


## 七、用户认证-认证服务查询数据库-解析申请令牌错误信息

 当账号输入错误应该返回用户不存在的信息，当密码错误要返回用户名或密码错误信息，业务流程图如下：

![1560991139837](assets/1560991139837.png)

![1560992265350](assets/1560992265350.png)

```java
//判断
if (map == null ||
    map.get("access_token") == null ||
    map.get("refresh_token") == null ||
    map.get("jti") == null) {  //jti是jwt令牌的唯一标识作为用户身份令牌
    //从spring security获取到返回的错误信息
    String error_description = (String) map.get("error_description");
    if(StringUtils.isNotEmpty(error_description)){
        if(error_description.equals("坏的凭证")){
            ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
        }else if(error_description.indexOf("UserDetailsService returned null") >= 0){
            ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
        }
    }
    ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
}
```

注意: **indexOf 结果就是查找的字符串所在的位置，从0记起，如果返回0说明查找的子串位于字符串开始**。（所以不能写成`>0`，而是`>=0`）

测试:

![1560992311434](assets/1560992311434.png)

![1560992328943](assets/1560992328943.png)

## 八、用户认证-认证服务查询数据库-用户登录前端

点击用户登录固定跳转到用户中心前端的登录页面，如下：

![1560993116353](assets/1560993116353.png)

进入`xc-ui-pc-learning`工程定义api方法，在base模块下定义login.js。

![1560993216128](assets/1560993216128.png)



最后测试:

![1560993566437](assets/1560993566437.png)

![1560993584386](assets/1560993584386.png)

## 九、前端显示当前用户-需求分析



![1560993644183](assets/1560993644183.png)

我们需要用`Cookie`中的`token`去认证服务中获取到`jwt`令牌。然后得到用户信息，然后存到`SessionStorage`中，然后显示出来。

![1560993662148](assets/1560993662148.png)

流程:

1、用户请求认证服务，登录成功。
2、用户登录成功，认证服务向cookie写入身份令牌，向redis写入`user_token`（身份令牌及授权jwt授权令牌）
3、**客户端携带cookie中的身份令牌请求认证服务获取jwt令牌**。
4、**客户端解析jwt令牌，并将解析的用户信息存储到sessionStorage中**。
jwt令牌中包括了用户的基本信息，客户端解析jwt令牌即可获取用户信息。
5、客户端从sessionStorage中读取用户信息，并在页头显示。

sessionStorage ：
sessionStorage 是H5的一个会话存储对象，在SessionStorage中保存的数据只在同一窗口或同一标签页中有效，
**在关闭窗口之后将会删除SessionStorage中的数据**。
seesionStorage的存储方式采用key/value的方式，可保存5M左右的数据（不同的浏览器会有区别）。

![1560993813045](assets/1560993813045.png)

### 1、Cookie、 LocalStorage 与 SessionStorage

**Cookie**

Cookie 是小甜饼的意思。顾名思义，cookie 确实非常小，它的大小限制为4KB左右。它的主要用途有保存登录信息，比如你登录某个网站市场可以看到“记住密码”，这通常就是通过在 Cookie 中存入一段辨别用户身份的数据来实现的。

**localStorage**

localStorage 是 HTML5 标准中新加入的技术，它并不是什么划时代的新东西。早在 IE 6 时代，就有一个叫 userData 的东西用于本地存储，而当时考虑到浏览器兼容性，更通用的方案是使用 Flash。而如今，localStorage 被大多数浏览器所支持，如果你的网站需要支持 IE6+，那以 userData 作为你的 polyfill 的方案是种不错的选择。

**sessionStorage**

sessionStorage 与 localStorage 的接口类似，但保存数据的生命周期与 localStorage 不同。做过后端开发的同学应该知道 Session 这个词的意思，直译过来是“会话”。而 sessionStorage 是一个前端的概念，它只是可以将一部分数据在当前会话中保存下来，刷新页面数据依旧存在。但当页面关闭后，sessionStorage 中的数据就会被清空。



![1547192066631](assets/1547192066631.png)





### 2、localStorage和sessionStorage的方法

**setItem存储value**

用途：将value存储到key字段

```js
sessionStorage.setItem("key", "value");     
localStorage.setItem("site", "java.cn");
```

**getItem获取value**

用途：获取指定key本地存储的值

```js
var value = sessionStorage.getItem("key");     
var site = localStorage.getItem("site");
```

**removeItem删除key**

用途：删除指定key本地存储的值

```js
sessionStorage.removeItem("key");     
localStorage.removeItem("site");
```

**clear清除所有的key/value**

用途：清除所有的key/value

```js
sessionStorage.clear();     
localStorage.clear();
```

**其他操作方法：点操作和[ ]**

web Storage不但可以用自身的setItem,getItem等方便存取，也可以像普通对象一样用点(.)操作符，及[]的方式进行数据存储，像如下的代码：

```js
var storage = window.localStorage; 
storage.key1 = "hello"; 
storage["key2"] = "world"; 
console.log(storage.key1); 
console.log(storage["key2"]);
```

**localStorage和sessionStorage的key和length属性实现遍历**

sessionStorage和localStorage提供的key()和length可以方便的实现存储的数据遍历，例如下面的代码：

```js
var storage = window.localStorage;
for(var i=0, len=storage.length; i<len;i++){
    var key = storage.key(i);     
    var value = storage.getItem(key);     
    console.log(key + "=" + value); 
}
```

 

## 十、前端显示当前用户-jwt查询接口

认证服务对外提供jwt查询接口，流程如下：

1、客户端携带cookie中的身份令牌请求认证服务获取jwt
2、认证服务根据身份令牌从redis中查询jwt令牌并返回给客户端。

```java
@Api(value = "用户认证", tags = "用户认证接口")
public interface AuthControllerApi {
    /**
     * 查询userjwt令牌 (根据cookie(cookie中有短令牌))
     */
    @ApiOperation("查询userjwt令牌") 
    JwtResult userjwt();
}
```

`Controller`:

```java
@Override
@GetMapping("/userjwt") 
public JwtResult userjwt() {

    //从Cookie中获取token
    String uid = getTokenFromCookie();
    if(uid == null){
        return new JwtResult(CommonCode.FAIL, null);
    }

    //根据身份令牌从redis中查询 整个jwt令牌
    AuthToken authToken = authService.getUserToken(uid);

    //返回authToken中的长 access_token给用户
    if(authToken != null){
        String jwt_token = authToken.getJwt_token();
        return new JwtResult(CommonCode.SUCCESS, jwt_token);
    }
    return null;
}
```

`Service`: (从`Redis`中根据短的获取整个jwt令牌):

```java
//根据key( 短的身份令牌从redis中查询整个jwt令牌)
public AuthToken getUserToken(String access_token) {

    String key = "user_token:" + access_token; // 之前存的时候的key

    String jsonString = stringRedisTemplate.opsForValue().get(key);

    AuthToken authToken = null;
    try {
        authToken = JSON.parseObject(jsonString, AuthToken.class);
    } catch (Exception e) {
        log.error("getUserToken from redis and execute JSON.parseObject error {}", e.getMessage());
        e.printStackTrace();
    }
    return authToken;
}
```



## 十一、前端显示当前用户-前端请求jwt

![1560996426273](assets/1560996426273.png)



![1560996496606](assets/1560996496606.png)

`xc-ui-pc-static-portal`下的`header.html`中: (获取到就设置到`sesssionStorage`中)

![1560996793992](assets/1560996793992.png)

调用服务端的接口:

![1560996851847](assets/1560996851847.png)



测试的问题:

![1560999783152](assets/1560999783152.png)

在nginx的`www.xuecheng.com`下面配置这个代理转发:

![1560999884621](assets/1560999884621.png)

重新启动nginx: (不要用`nginx.exe -s reload`，而是在在进程中停掉，然后再次双击启动nginx)

![1561000367228](assets/1561000367228.png)

`test2`是在数据库中的`name`:

![1561000543870](assets/1561000543870.png)

## 十二、用户退出-服务端

需求:

![1561002604605](assets/1561002604605.png)

![1561002624252](assets/1561002624252.png)

用户退出要以下动作：

1、删除redis中的token

2、删除cookie中的token

`Controller`:

```java
 //退出
@Override
@PostMapping("/userlogout")
public ResponseResult logout() {
    //取出身份令牌
    String uid = getTokenFromCookie();
    //删除redis中token
    boolean b = authService.delToken(uid);
    //可能会直接过期(过期了就是直接退出了)，所以不需要抛异常
    //        if(!b){
    //            ExceptionCast.cast(AuthCode.AUTH_LOGOUT_FAIL);
    //        }
    //清除cookie
    clearCookie(uid);

    return new ResponseResult(CommonCode.SUCCESS);
}

//清除cookie
private void clearCookie(String token){
    HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
    CookieUtil.addCookie(response, cookieDomain, "/", "uid", token, 0, false);
}
```

`Service`:

```java
//从redis中删除令牌
public boolean delToken(String access_token){
    String key = "user_token:" + access_token;
    stringRedisTemplate.delete(key);
    return true;
}
```

注意:

![1561003077791](assets/1561003077791.png)

## 十三、用户退出-前端

![1561003425074](assets/1561003425074.png)

`xc-ui-pc-learning`端:

![1561003547301](assets/1561003547301.png)

![1561003594394](assets/1561003594394.png)

整体图:



![1547189669542](assets/1547189669542.png)



测试:

需要在`header.html`前面引入一个`elment-ui`的库:

![1561004000432](assets/1561004000432.png)

![1561004036874](assets/1561004036874.png)



## 十四、网关-介绍网关及搭建网关工程



### 1、什么是网关？

服务网关是在微服务前边设置一道屏障，请求先到服务网关，网关会对请求进行过虑、校验、路由等处理。有了服
务网关可以提高微服务的安全性，网关校验请求的合法性，请求不合法将被拦截，拒绝访问。

### 2、Zuul与Nginx怎么配合使用？

Zuul与Nginx在实际项目中需要配合使用，如下图，Nginx的作用是反向代理、负载均衡，Zuul的作用是保障微服
务的安全访问，拦截微服务请求，校验合法性及负载均衡。



![1547189795215](assets/1547189795215.png)



### 3、@EnableZuulProxy

注意在启动类上使用`@EnableZuulProxy`注解标识此工程为Zuul网关，启动类代码如下：

![1561004407225](assets/1561004407225.png)



注意: **网关需要配置Erueka，网关是一个代理，网关要将请求转发到微服务中**（需要从Erueka中获取微服务地址）。



## 十五、网关-路由配置

**Zuul网关具有代理的功能，根据请求的url转发到微服务**，如下图：

路由就是将具体的请求转发到微服务。

客户端请求网关`/api/learning`，通过路由转发到`/learning`
客户端请求网关`/api/course`，通过路由转发到`/course`



![1561004673470](assets/1561004673470.png)



![1561006330061](assets/1561006330061.png)



**测试**

![1561007775251](assets/1561007775251.png)



先用原来的地址访问一下:

![1561008002628](assets/1561008002628.png)

然后通过网关来访问:

![1561008048273](assets/1561008048273.png)

## 十六、网关-过虑器

Zuul的核心就是过虑器，通过过虑器实现请求过虑，身份校验等。

### 1、ZuulFilter

自定义过虑器需要继承 ZuulFilter，ZuulFilter是一个抽象类，需要覆盖它的四个方法，如下：
1、 `shouldFilter`：返回一个Boolean值，判断该过滤器是否需要执行。返回true表示要执行此过虑器，否则不执行。 

2、 `run`：过滤器的业务逻辑。 

3、 `filterType`：返回字符串代表过滤器的类型，如下 pre：请求在被路由之前
执行 routing：在路由请求时调用 post：在routing和errror过滤器之后调用 error：处理请求时发生错误调用
4、 `filterOrder`：此方法返回整型数值，通过此数值来定义过滤器的执行顺序，数字越小优先级越高。



### 2、测试

过虑所有请求，判断头部信息是否有Authorization，如果没有则拒绝访问，否则转发到微服务。
定义过虑器，使用`@Component`标识为bean。



```java
@Component
public class LoginFilterTest extends ZuulFilter {

    //过虑器的类型
    @Override
    public String filterType() {

        /**
         pre：请求在被路由之前执行
         routing：在路由请求时调用
         post：在routing和errror过滤器之后调用
         error：处理请求时发生错误调用
         */
        return "pre";
    }

    //过虑器序号，越小越被优先执行
    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        //返回true表示要执行此过虑器
        return true;
    }

    //过虑器的内容
    //测试的需求：过虑所有请求，判断头部信息是否有Authorization，如果没有则拒绝访问，否则转发到微服务。
    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        //得到request
        HttpServletRequest request = requestContext.getRequest();
        //得到response
        HttpServletResponse response = requestContext.getResponse();
        //取出头部信息Authorization
        String authorization = request.getHeader("Authorization");
        if(StringUtils.isEmpty(authorization)){
            //拒绝访问
            requestContext.setSendZuulResponse(false);
            //设置响应代码
            requestContext.setResponseStatusCode(200);
            //构建响应的信息
            ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
            //转成json
            String jsonString = JSON.toJSONString(responseResult);
            requestContext.setResponseBody(jsonString);
            //转成json，设置contentType
            response.setContentType("application/json;charset=utf-8");
            return null;
        }
        return null;
    }
}
```



![1561009981351](assets/1561009981351.png)

## 十七、身份校验-身份校验过虑器编写

本小节实现网关连接Redis校验令牌：

1、从`cookie`查询用户身份令牌是否存在，不存在则拒绝访问
2、从`http header`查询jwt令牌是否存在，不存在则拒绝访问
3、从Redis查询`user_token`令牌是否过期，过期则拒绝访问

`LoginFilter`:

```java
@Component
@Slf4j
public class LoginFilter extends ZuulFilter {

    @Autowired
    AuthService authService;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {

        //上下文对象
        RequestContext requestContext = RequestContext.getCurrentContext();
        //请求对象
        HttpServletRequest request = requestContext.getRequest();

        //1、查询身份令牌
        String access_token = authService.getTokenFromCookie(request);
        if (access_token == null) {
            access_denied();
            return null;
        }

        //2、从redis中校验身份令牌是否过期
        long expire = authService.getExpire(access_token);
        if (expire <= 0) {
            access_denied();
            return null;
        }

        //3、查询jwt令牌
        String jwt = authService.getJwtFromHeader(request);
        if (jwt == null) {
            access_denied();
            return null;
        }
        return null;
    }

    private void access_denied() {
        //获取上下文对象
        RequestContext requestContext = RequestContext.getCurrentContext();
        requestContext.setSendZuulResponse(false); // 拒绝访问

        //设置响应内容
        ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
        String jsonString = JSON.toJSONString(responseResult);

        requestContext.setResponseBody(jsonString);
        requestContext.setResponseStatusCode(200);
        HttpServletResponse response = requestContext.getResponse();
        response.setContentType("application/json;charset=utf-8");
    }
}

```

`Service`:

```java
@Service
public class AuthService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    //从header中查询jwt令牌
    public String getJwtFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if(StringUtils.isEmpty(authorization)){
            return null;
        }
        if(!authorization.startsWith("Bearer ")){
            return null;
        }
        return authorization;
    }

    //返回是否过期 (查询是否过期)
    public long getExpire(String access_token) {
        //用key ( "user_token:短身份令牌")查询
        String key = "user_token:" + access_token;
        Long expire = stringRedisTemplate.getExpire(key);
        return expire;
    }

    //得到身份令牌(短的那个)
    public String getTokenFromCookie(HttpServletRequest request) {
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        String access_token = cookieMap.get("uid");
        if(StringUtils.isEmpty(access_token)){
            return null;
        }
        return access_token;
    }
}
```

## 十八、身份校验-测试

测试:
先配置一下 nginx的 代理：

因为是`/api`开头的，所以在`www.xuecheng.com`下配置代理:

![1561012716862](assets/1561012716862.png)

![1561012730146](assets/1561012730146.png)

```properties
#微服务网关
upstream api_server_pool{
	server 127.0.0.1:50201 weight=10;
}
#微服务网关
location /api {
	proxy_pass http://api_server_pool;
}
```



1、测试失败:

![1561013170319](assets/1561013170319.png)

2、测试成功

先得到令牌:

![1561013315715](assets/1561013315715.png)

在redis中也有:

![1561013372621](assets/1561013372621.png)

```json
{
  "access_token": "3d491486-06e0-41fa-b60d-d3a6af626650",
  "jwt_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOiIxIiwidXNlcnBpYyI6bnVsbCwidXNlcl9uYW1lIjoiaXRjYXN0Iiwic2NvcGUiOlsiYXBwIl0sIm5hbWUiOiJ0ZXN0MDIiLCJ1dHlwZSI6IjEwMTAwMiIsImlkIjoiNDkiLCJleHAiOjE1NjEwNTY0ODMsImp0aSI6IjNkNDkxNDg2LTA2ZTAtNDFmYS1iNjBkLWQzYTZhZjYyNjY1MCIsImNsaWVudF9pZCI6IlhjV2ViQXBwIn0.eqWft-sE12aNXFLDUX-5D9sGXWuyg_8VtAB6jr_x9Bdyp1Rq-DVvbKLo2-twal1ZZAQV0lCD62VbF7oysXNrF2bO5gch-iSZ_RdgkwNbvponysXonFkhu4Aj8uIaHKTZTSIxy2tVMzp-e3jFYmLubipFA30aOFxbagWbQhze9SyC4-p4IZP7mgOv3LMnksI2Y-Ja_bGJgbYc0n0HGJ0vCE0tKT0wkDnC0gkpMS7yoQvmQ0S4VXrj3Vf_alKRFuZDKVe431xMjqjVq00DhlXGcajH82EfcB4ELI2aXkoRM6mC513jv7dwQOCdHJ5i5u_gAymhijQ22-wfnkQZbuGsOg",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOiIxIiwidXNlcnBpYyI6bnVsbCwidXNlcl9uYW1lIjoiaXRjYXN0Iiwic2NvcGUiOlsiYXBwIl0sImF0aSI6IjNkNDkxNDg2LTA2ZTAtNDFmYS1iNjBkLWQzYTZhZjYyNjY1MCIsIm5hbWUiOiJ0ZXN0MDIiLCJ1dHlwZSI6IjEwMTAwMiIsImlkIjoiNDkiLCJleHAiOjE1NjEwNTY0ODMsImp0aSI6IjQzNGYyZjY1LTY3NWEtNDk5MS05NThjLTk3NWZlNzZjMDhhYyIsImNsaWVudF9pZCI6IlhjV2ViQXBwIn0.JxxcOmVrj4R-JwYAU4clNbmdSFZeUBl3JyoSQ4T4rMOlNPA9dvpYcf4WhPu671KNnznU7E_oaeEk2gOnYmRWs3Qt8_JDD_XXer7IjwZBM2aF5xKB6VP1yzEeYjybacIwzaA4nKHUrKAcdn8WAxDZrmepTTeOV8sTiynVQeaacPHdL_1gfrsZsit8aVE5qM9MrXy3qRXu1Tla6WhbuDjO3vpESkrW8ThsdraMpkHbHUhTa0eq2wPsCz2f8shmPz37WF44KZzLcFV9TEANjqESOaEutBrD3BAv1yHB2dshy32kDl1p5N_Krdc5Nm7O8MlUxopBBKQB1zLDK_Ai7icXCg"
}
```

测试成功:

```java
http://www.xuecheng.com/api/course/coursepic/list/4028e58161bd3b380161bd3bcd2f0000
```



![1561013529183](assets/1561013529183.png)



如果令牌错了也会报错:

![1561013664198](assets/1561013664198.png)