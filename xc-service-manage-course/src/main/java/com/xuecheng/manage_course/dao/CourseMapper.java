package com.xuecheng.manage_course.dao;

import com.github.pagehelper.Page;
import com.xuecheng.framework.domain.course.CourseBase;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * Created by Administrator.
 */
@Mapper
@Repository
public interface CourseMapper {
   CourseBase findCourseBaseById(String id);

   //测试一下PageHelper的使用, 查询所有CourseBase
   Page<CourseBase> findCourseList();

   // 分页查询 CourseList, 传入的参数是查询的条件 ,查询条件CompanyId
   Page<CourseInfo> findCourseListPage(CourseListRequest courseListRequest);
}
