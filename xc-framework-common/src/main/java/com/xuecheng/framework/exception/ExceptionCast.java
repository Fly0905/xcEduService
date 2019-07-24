package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

public class ExceptionCast {

    //使用此静态方法抛出自定义异常，更加的方便
    public static void cast(ResultCode resultCode){
        throw new CustomException(resultCode);
    }

}