package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CourseMarketRepository courseMarketRepository;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Autowired
    CoursePubRepository coursePubRepository;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;

    //查询课程计划
    public TeachplanNode findTeachplanList(String courseId) {
        TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        return teachplanNode;
    }

    // 添加课程计划，主要是处理parentId
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        //校验课程id和课程计划名称
        if (teachplan == null ||
                StringUtils.isEmpty(teachplan.getCourseid()) ||
                StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //取出课程id
        String courseid = teachplan.getCourseid();
        //取出父结点id
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)) {
            //如果父结点为空则获取根结点
            parentid = getTeachplanRoot(courseid);
        }
        //取出父结点信息
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(parentid);
        if (!teachplanOptional.isPresent()) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //父结点
        Teachplan teachplanParent = teachplanOptional.get();
        //父结点级别
        String parentGrade = teachplanParent.getGrade();
        //设置父结点
        teachplan.setParentid(parentid);
        teachplan.setStatus("0");//未发布
        //子结点的级别，根据父结点来判断
        if (parentGrade.equals("1")) {
            teachplan.setGrade("2");
        } else if (parentGrade.equals("2")) {
            teachplan.setGrade("3");
        }
        //设置课程id
        teachplan.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取课程根结点，如果没有则添加根结点
    private String getTeachplanRoot(String courseId) {
        //校验课程id
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()) {
            return null;
        }
        CourseBase courseBase = optional.get();
        //取出课程计划根结点
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId,
                "0");
        if (teachplanList == null || teachplanList.size() == 0) {
            //新增一个根结点
            Teachplan teachplanRoot = new Teachplan();
            teachplanRoot.setCourseid(courseId);
            teachplanRoot.setPname(courseBase.getName());
            teachplanRoot.setParentid("0");
            teachplanRoot.setGrade("1");//1级
            teachplanRoot.setStatus("0");//未发布
            teachplanRepository.save(teachplanRoot);
            return teachplanRoot.getId();
        }
        Teachplan teachplan = teachplanList.get(0);
        return teachplan.getId();
    }

    public QueryResponseResult<CourseInfo> findCourseList(String company_id, int page,
                                                          int size,
                                                          CourseListRequest courseListRequest) {
        if (courseListRequest == null) {
            courseListRequest = new CourseListRequest();
        }
        if (page <= 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }

        //将公司id参数传入dao
        courseListRequest.setCompanyId(company_id);

        // 设置分页参数
        PageHelper.startPage(page, size);
        // 分页查询
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);

        List<CourseInfo> result = courseListPage.getResult();// 查询结果
        long total = courseListPage.getTotal();// 总的记录数

        // 构造返回结果
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        queryResult.setList(result);
        queryResult.setTotal(total);
        return new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
    }

    //添加课程提交
    @Transactional
    public AddCourseResult addCourseBase(CourseBase courseBase) {
        //课程状态默认为未发布
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS, courseBase.getId());
    }

    // 根据课程ID查询课程信息
    public CourseBase getCoursebaseById(String courseId) {
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Transactional
    public ResponseResult updateCoursebase(String id, CourseBase courseBase) {
        CourseBase one = this.getCoursebaseById(id);
        if (one == null) {
            //抛出异常.. ,不存在这个课程信息，这里抛出参数错误异常
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //修改课程信息
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //查询课程营销信息
    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (!optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    @Transactional
    public CourseMarket updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket one = this.getCourseMarketById(id);
        if (one != null) {
            one.setCharge(courseMarket.getCharge());
            one.setStartTime(courseMarket.getStartTime());//课程有效期，开始时间
            one.setEndTime(courseMarket.getEndTime());//课程有效期，结束时间
            one.setPrice(courseMarket.getPrice());
            one.setQq(courseMarket.getQq());
            one.setValid(courseMarket.getValid());
            courseMarketRepository.save(one);
        } else {
            //添加课程营销信息
            one = new CourseMarket();
            BeanUtils.copyProperties(courseMarket, one);
            //设置课程id
            one.setId(id);
            courseMarketRepository.save(one);
        }
        return one;
    }

    // 添加图片和课程关联
    @Transactional
    public ResponseResult saveCoursePic(String courseId, String pic) {
        //查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if (picOptional.isPresent()) {
            coursePic = picOptional.get();
        }
        //没有课程图片则新建对象
        if (coursePic == null) {
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        //保存课程图片
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    // 查询课程图片
    public CoursePic findCoursePic(String courseId) {
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if (picOptional.isPresent()) {
            CoursePic coursePic = picOptional.get();
            return coursePic;
        }
        return null;
    }

    //删除课程图片
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        //执行删除，返回1表示删除成功，返回0表示删除失败
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result > 0) {
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //获取课程详情页面信息，包括基本信息、图片、营销、课程计划
    public CourseView getCourseView(String id) {
        CourseView courseView = new CourseView();

        //查询课程基本信息
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            CourseBase courseBase = courseBaseOptional.get();
            courseView.setCourseBase(courseBase);
        }
        //查询课程营销信息
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()) {
            CourseMarket courseMarket = courseMarketOptional.get();
            courseView.setCourseMarket(courseMarket);
        }
        //查询课程图片信息
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()) {
            CoursePic coursePic = picOptional.get();
            courseView.setCoursePic(coursePic);
        }
        //查询课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);

        return courseView;
    }

    //根据id查询课程基本信息
    public CourseBase findCourseBaseById(String courseId) {
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if (baseOptional.isPresent()) {
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }

    // 课程预览
    public CoursePublishResult preview(String courseId) {
        // 1、请求cms添加页面 (需要远程调用Feign)

        CourseBase one = this.findCourseBaseById(courseId);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();  //站点

        cmsPage.setSiteId(publish_siteId);//课程预览站点
        cmsPage.setTemplateId(publish_templateId);//模板
        cmsPage.setPageName(courseId + ".html");   //页面名称
        cmsPage.setPageAliase(one.getName());//页面别名
        cmsPage.setPageWebPath(publish_page_webpath);   //页面访问路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//页面存储路径

        // 发布url + 课程id
        cmsPage.setDataUrl(publish_dataUrlPre + courseId);//数据url
        //远程请求cms保存页面信息

        CmsPageResult cmsPageResult = cmsPageClient.saveCmsPage(cmsPage);
        if (!cmsPageResult.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        //页面id
        String pageId = cmsPageResult.getCmsPage().getPageId();

        // 拼装页面预览的url (页面url)
        String pageUrl = previewUrl + pageId;

        // 返回 CoursePublishResult，携带pageUrl
        return new CoursePublishResult(CommonCode.SUCCESS, pageUrl);
    }

    //课程发布
    @Transactional
    public CoursePublishResult publish(String courseId) {
        // 1、调用cms一键发布接口课程详情页面发布到服务端
        CmsPostPageResult cmsPostPageResult = publish_page(courseId);
        if (!cmsPostPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }

        // 2、保存课程的发布状态为 "已发布"
        CourseBase courseBase = saveCoursePubState(courseId);
        if (courseBase == null) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }

        // 3、保存课程索引信息....缓存课程信息...(之前是待做，在day_11中学完了ES后补充)

        //创建课程索引信息 (创建CoursePub对象)
        CoursePub coursePub = createCoursePub(courseId);

        // 数据库保存CoursePub
        //向数据库保存课程索引信息
        CoursePub newCoursePub = saveCoursePub(courseId, coursePub);
        if (newCoursePub == null) {
            //创建课程索引信息失败
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }

        // 4、最后一定要返回页面url
        String pageUrl = cmsPostPageResult.getPageUrl(); //获取发布之后的result

        // 5、后面新增的(媒资中(TeachplanMediaPub中)保存媒资信息，之后由logstash采集到ES)
        saveTeachplanMediaPub(courseId);

        return new CoursePublishResult(CommonCode.SUCCESS, pageUrl);
    }

    //保存媒资信息(新增的方法)
    private void saveTeachplanMediaPub(String courseId){
        //查询课程媒资信息
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
        //先删除，然后存储到teachplanMediaPub
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for(TeachplanMedia teachplanMedia : teachplanMediaList){
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia, teachplanMediaPub);
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }

    private CoursePub saveCoursePub(String courseId, CoursePub coursePub) {

        CoursePub coursePubNew = null;
        //根据课程id查询coursePub
        Optional<CoursePub> coursePubOptional = coursePubRepository.findById(courseId);
        if (coursePubOptional.isPresent()) {
            coursePubNew = coursePubOptional.get();
        } else {
            coursePubNew = new CoursePub();
        }

        //将coursePub对象中的信息保存到coursePubNew中
        BeanUtils.copyProperties(coursePub, coursePubNew);
        coursePubNew.setId(courseId);
        //时间戳,给logstach使用
        coursePubNew.setTimestamp(new Date());
        //发布时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(date);
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }

    // 创建课程索引信息
    private CoursePub createCoursePub(String courseId) {
        CoursePub coursePub = new CoursePub();
        //根据课程id查询course_base
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        // 这里一开始写错了 (课程资料中写错了)，写成了courseBaseOptional == null
        if (baseOptional.isPresent()) {
            CourseBase courseBase = baseOptional.get();
            //将courseBase属性拷贝到CoursePub中
            BeanUtils.copyProperties(courseBase, coursePub);
        }

        //查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if (picOptional.isPresent()) {
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic, coursePub);
        }

        //课程营销信息
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(courseId);
        if (marketOptional.isPresent()) {
            CourseMarket courseMarket = marketOptional.get();
            BeanUtils.copyProperties(courseMarket, coursePub);
        }
        //课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(courseId);
        String jsonString = JSON.toJSONString(teachplanNode);
        //将课程计划信息json串保存到 course_pub中
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    //发布课程正式页面
    public CmsPostPageResult publish_page(String courseId) {
        // 为了下面获取名称
        CourseBase courseBaseById = this.findCourseBaseById(courseId);

        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();

        cmsPage.setSiteId(publish_siteId);//  //站点, 课程预览站点
        cmsPage.setTemplateId(publish_templateId); //模板
        cmsPage.setPageName(courseId + ".html");//页面名称
        cmsPage.setPageAliase(courseBaseById.getName());//页面别名
        cmsPage.setPageWebPath(publish_page_webpath);//页面访问路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//页面存储路径
        cmsPage.setDataUrl(publish_dataUrlPre + courseId);//数据url

        // 远程调用cms发布页面
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        return cmsPostPageResult;
    }

    //更新课程发布状态
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBase = this.findCourseBaseById(courseId);
        //更新发布状态
        courseBase.setStatus("202002"); // 这个状态在数据库中是已经发布的意思
        CourseBase save = courseBaseRepository.save(courseBase);
        return save;
    }

    //保存课程计划于媒资文件的关联信息
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //课程计划
        String teachplanId = teachplanMedia.getTeachplanId();

        //查询课程计划
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(teachplanId);
        if (!teachplanOptional.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }

        Teachplan teachplan = teachplanOptional.get();
        //只允许为叶子结点课程计划选择视频
        String grade = teachplan.getGrade();
        if (StringUtils.isEmpty(grade) || !grade.equals("3")) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }

        TeachplanMedia one = null;
        Optional<TeachplanMedia> teachplanMediaOptional = teachplanMediaRepository.findById(teachplanId);
        if (teachplanMediaOptional.isPresent()) {
            one = teachplanMediaOptional.get();
        } else {
            one = new TeachplanMedia();
        }

        //保存媒资信息与课程计划信息
        one.setTeachplanId(teachplanId);
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}