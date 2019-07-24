package com.xuecheng.framework.exception;

import com.google.common.collect.ImmutableMap;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.model.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 统一异常捕获类
 * 在spring 3.2中，新增了@ControllerAdvice 注解，
 * 可以用于定义@ExceptionHandler、@InitBinder、@ModelAttribute，并应用到所有@RequestMapping中。
 */
@ControllerAdvice // 控制器增强
@Slf4j
public class ExceptionCatch {

//    private static final Logger log = LoggerFactory.getLogger(ExceptionCatch.class);

    //捕获 CustomException异常
    @ExceptionHandler(CustomException.class)
    @ResponseBody // 将异常转换成json
    public ResponseResult customException(CustomException e) {
        e.printStackTrace();
        // 记录日志
        log.error("catch exception : {} exception: ", e.getMessage(), e);
        ResultCode resultCode = e.getResultCode();
        ResponseResult responseResult = new ResponseResult(resultCode);
        return responseResult;
    }

    //使用EXCEPTIONS存放异常类型和错误代码的映射，ImmutableMap的特点的一旦创建不可改变，并且线程安全
    private static ImmutableMap<Class<? extends Throwable>,ResultCode> exceptionMap;

    //使用builder来构建一个异常类型和错误代码的异常
    protected static ImmutableMap.Builder<Class<? extends Throwable>,ResultCode> builder = ImmutableMap.builder();

    static{
        //在这里加入一些基础的异常类型判断
        builder.put(HttpMessageNotReadableException.class, CommonCode.INVALID_PARAM);
    }

    //捕获Exception异常
    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseResult exception(Exception e) {
        e.printStackTrace();
        log.error("catch exception : {} \r\n exception: ",e.getMessage(), e);
        if(exceptionMap == null) {
            exceptionMap = builder.build(); // EXCEPTIONS构建完毕
        }
        // 从EXCEPTIONS中寻找异常类型对应的错误代码，如果找到了将错误代码响应给用户，如果找不到就返回给用户9999
        final ResultCode resultCode = exceptionMap.get(e.getClass());

        if (resultCode != null) {
            return new ResponseResult(resultCode);
        } else {
            return new ResponseResult(CommonCode.SERVER_ERROR);
        }
    }
}