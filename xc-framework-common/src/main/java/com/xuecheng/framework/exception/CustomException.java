package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;


/**
 * 自定义异常类
 */
public class CustomException extends RuntimeException { // 继承RuntimeException可以让代码没有侵入性

    private ResultCode resultCode;

    public CustomException(ResultCode resultCode) {
        //异常信息为错误代码+异常信息
        super("错误代码：" + resultCode.code() + "错误信息：" + resultCode.message());
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return this.resultCode;
    }
}