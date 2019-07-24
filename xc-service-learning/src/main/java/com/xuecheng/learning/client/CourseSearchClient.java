package com.xuecheng.learning.client;


import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient("xc-service-search") // 可以直接用一个工具类的常量
@FeignClient(XcServiceList.XC_SERVICE_SEARCH)
public interface CourseSearchClient {

    //根据课程计划id查询媒资  (可以根据swagger-ui就可以查询到)
    @GetMapping("/search/course/getmedia/{teachplanId}")
    TeachplanMediaPub getmedia(@PathVariable("teachplanId") String teachplanId);

}
