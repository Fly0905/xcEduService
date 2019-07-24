package com.xuecheng.auth.service;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.exception.ExceptionCast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
            //从spring security获取到返回的错误信息
            String error_description = (String) map.get("error_description");
            if (StringUtils.isNotEmpty(error_description)) {
                if (error_description.equals("坏的凭证")) {
                    ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
                } else if (error_description.contains("UserDetailsService returned null")) { //之前是indexOf("") >= 0
                    ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
                }
            }
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

    //从redis中删除令牌
    public boolean delToken(String access_token){
        String key = "user_token:" + access_token;
        stringRedisTemplate.delete(key);
        return true;
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
