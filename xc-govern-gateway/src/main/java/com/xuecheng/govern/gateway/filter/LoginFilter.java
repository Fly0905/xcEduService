package com.xuecheng.govern.gateway.filter;


import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
