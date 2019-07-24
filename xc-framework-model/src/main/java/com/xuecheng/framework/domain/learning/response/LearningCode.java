package com.xuecheng.framework.domain.learning.response;


import com.xuecheng.framework.model.response.ResultCode;
import lombok.ToString;

@ToString
public enum LearningCode implements ResultCode {
    LEARNING_GETMEDIA_ERROR(false,23001,"获取学习地址失败！"),
    CHOOSECOURSE_USERISNULL(false, 23002, "选课失败，用户为空!"),
    CHOOSECOURSE_TASKISNULL(false, 23003, "选课失败，任务为空!");


    boolean success;
    int code;
    String message;

    private LearningCode(boolean success, int code, String message){
        this.success = success;
        this.code = code;
        this.message = message;
    }

    @Override
    public boolean success() {
        return success;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
