package com.xuecheng.govern.gateway.service;


import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
