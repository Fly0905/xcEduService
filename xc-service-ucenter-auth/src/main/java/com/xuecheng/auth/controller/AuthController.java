package com.xuecheng.auth.controller;

import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

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

    //从Cookie中取到短的身份令牌 token
    private String getTokenFromCookie() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        String uid = cookieMap.get("uid");
        return uid;
    }

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

}
