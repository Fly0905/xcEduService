package com.xuecheng.api.learning;

import com.xuecheng.framework.domain.learning.response.GetMediaResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "录播课程学习管理", tags = "录播课程学习管理")
public interface CourseLearningControllerApi {

    @ApiOperation("获取课程学习地址") // courseId用来校验 学生的相关权限
    GetMediaResult getMedia(String courseId, String teachplanId);

}