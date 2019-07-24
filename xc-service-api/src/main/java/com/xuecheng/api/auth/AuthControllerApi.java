package com.xuecheng.api.auth;

import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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


    /**
     * 查询userjwt令牌 (根据cookie(cookie中有短令牌))
     */
    @ApiOperation("查询userjwt令牌")
    JwtResult userjwt();
}