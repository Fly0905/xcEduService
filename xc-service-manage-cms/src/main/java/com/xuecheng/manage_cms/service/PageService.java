package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    CmsConfigRepository cmsConfigRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    CmsSiteRepository cmsSiteRepository;

    /**
     * 页面列表分页查询
     *
     * @param page             当前页码,页码从1开始
     * @param size             页面显示条数
     * @param queryPageRequest 查询条件(暂时没有用到查询条件
     * @return 页面列表
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        if (queryPageRequest == null) {
            queryPageRequest = new QueryPageRequest();
        }
        CmsPage cmsPage = new CmsPage();

        //站点ID
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //模板ID
        if (StringUtils.isNotEmpty(queryPageRequest.getTemplateId())) {
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        //页面别名
        if (StringUtils.isNotEmpty(queryPageRequest.getPageAliase())) {
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }

        //页面名称模糊查询，需要自定义字符串的匹配器实现模糊查询
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());

        Example<CmsPage> example = Example.of(cmsPage, exampleMatcher);

        if (page <= 0) {
            page = 1;
        }
        page = page - 1;  //为了适应mongodb的接口将页码减1

        if (size <= 0) {
            size = 10;
        }
        //分页对象
        Pageable pageable = PageRequest.of(page, size);
        //分页查询
        Page<CmsPage> all = cmsPageRepository.findAll(example, pageable);

        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());
        //返回结果
        return new QueryResponseResult(CommonCode.SUCCESS, cmsPageQueryResult);
    }

//    public CmsPageResult add(CmsPage cmsPage) {
//        // 校验页面名称、站点Id、页面webPath的唯一性
//        // 根据页面名称、站点Id、页面WebPath去cms_page集合，如果查到说明这个页面已经存在，如果查不到再继续添加
//        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
//
//        if (cmsPage1 == null) {
//            // 调用dao新增页面
//            cmsPage.setPageId(null); // 先设置为null，然后通过MongoDB自动生成一个主键
//            cmsPageRepository.save(cmsPage);
//            return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
//        }
//
//        return new CmsPageResult(CommonCode.FAIL, null); // 添加失败
//    }

    //添加页面
    public CmsPageResult add(CmsPage cmsPage) {
        //校验cmsPage是否为空
        if (cmsPage == null) {
            //抛出异常，非法请求
            //...
        }
        //根据页面名称查询（页面名称已在mongodb创建了唯一索引）
        CmsPage cmsPage1 =
                cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(),
                        cmsPage.getSiteId(), cmsPage.getPageWebPath());

        //校验页面是否存在，已存在则抛出异常
        if (cmsPage1 != null) {
            //抛出异常，已存在相同的页面名称
            //...
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }

        cmsPage.setPageId(null);//添加页面主键由spring data 自动生成
        CmsPage save = cmsPageRepository.save(cmsPage);
        //返回结果
        CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, save);
        return cmsPageResult;
    }


    /**
     * 根据id查询页面
     *
     * @param id
     * @return
     */
    public CmsPage getById(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;  //返回空
    }

    /**
     * 更新页面信息
     *
     * @param id
     * @param cmsPage
     * @return
     */
    public CmsPageResult update(String id, CmsPage cmsPage) {
        //根据id查询页面信息
        CmsPage one = this.getById(id);
        if (one != null) {
            //更新模板id
            one.setTemplateId(cmsPage.getTemplateId());
            //更新所属站点
            one.setSiteId(cmsPage.getSiteId());
            //更新页面别名
            one.setPageAliase(cmsPage.getPageAliase());
            //更新页面名称
            one.setPageName(cmsPage.getPageName());
            //更新访问路径
            one.setPageWebPath(cmsPage.getPageWebPath());
            //更新物理路径
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            //更新数据URL
            one.setDataUrl(cmsPage.getDataUrl());

            //执行更新
            cmsPageRepository.save(one);
            //返回成功
            return new CmsPageResult(CommonCode.SUCCESS, one);
        }
        //返回失败
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    /**
     * 删除操作
     *
     * @param id
     * @return
     */
    public ResponseResult delete(String id) {
        CmsPage one = this.getById(id);
        if (one != null) {
            //删除页面
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     * 页面静态化
     * 1、获取页面模型数据
     * 2、获取页面模板
     * 3、执行静态化
     */
    public String getPageHtml(String pageId) {
        //1. 获取页面模型数据
        Map model = this.getModelByPageId(pageId);
        if (model == null) {
            //获取页面模型数据为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        //2. 获取页面模板
        String templateContent = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(templateContent)) {
            //页面模板为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //3. 执行静态化, 需要传入页面模型数据和模板
        String html = generateHtml(templateContent, model);
        if (StringUtils.isEmpty(html)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }

    //1、获取页面模型数据
    public Map getModelByPageId(String pageId) {
        //查询页面信息
        CmsPage cmsPage = this.getById(pageId);
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //取出dataUrl
        String dataUrl = cmsPage.getDataUrl();
        if (StringUtils.isEmpty(dataUrl)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        // 远程调用  这个就是testfreemarker下测试 banner使用那个数据dataUrl的时候
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;
    }

    //2、获取页面模板
    public String getTemplateByPageId(String pageId) {
        //查询页面信息
        CmsPage cmsPage = this.getById(pageId);
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //页面模板
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            //页面模板为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if (optional.isPresent()) {
//            CmsTemplate cmsTemplate = optional.get();
//            //模板文件id
//            String templateFileId = cmsTemplate.getTemplateFileId();
//            //从GridFS中取出模板文件内容
//            GridFSFile gridFSFile =
//                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
//
//            if(gridFSFile == null){
//                System.out.println("eeee");
//            }
//
//            //打开下载流对象
//            GridFSDownloadStream gridFSDownloadStream =
//                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
//            //创建GridFsResource
//            GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);


            CmsTemplate cmsTemplate = optional.get();
            //获取模板文件id
            String templateFileId = cmsTemplate.getTemplateFileId();
            //从GridFS中取模板文件内容
            //根据文件id查询文件
            GridFSFile gridFSFile =
                    gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));

            //打开一个下载流对象
            GridFSDownloadStream gridFSDownloadStream =
                    gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建GridFsResource对象，获取流
            GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
            //从流中取数据
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //3、页面静态化
    public String generateHtml(String template, Map model) {
        try {
            //生成配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //模板加载器
            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            stringTemplateLoader.putTemplate("template", template);
            //配置模板加载器
            configuration.setTemplateLoader(stringTemplateLoader);
            //获取模板
            Template template1 = configuration.getTemplate("template");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template1, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 页面发布
    public ResponseResult post(String pageId) {
        // 执行页面静态化
        String pageHtml = this.getPageHtml(pageId);
        // 将页面静态化文件存储到GridFs中
        if (StringUtils.isEmpty(pageHtml)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        //保存静态化文件
        CmsPage cmsPage = saveHtml(pageId, pageHtml);
        // 向MQ发消息
        sendPostPage(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //保存html到GridFs
    // 1、将html文件内容保存到GridFs中
    // 2、将html文件id更新到cmsPage中
    private CmsPage saveHtml(String pageId, String htmlContent) {
        //先得到页面信息
        CmsPage cmsPage = this.getById(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }

//        CmsPage cmsPage = optional.get();
//        //存储之前先删除
//        String htmlFileId = cmsPage.getHtmlFileId();
//        if(StringUtils.isNotEmpty(htmlFileId)){
//            gridFsTemplate.delete(Query.query(Criteria.where("_id").is(htmlFileId)));
//        }

        ObjectId objectId = null;
        try {
            //将htmlContent内容转成输入流
            InputStream inputStream = IOUtils.toInputStream(htmlContent, "utf-8");
            //将html文件内容保存到GridFS
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //文件id
        String fileId = objectId.toString();
        //将文件id(htmlId)存储到cmspage中
        cmsPage.setHtmlFileId(fileId);
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    // 发送MQ消息
    private void sendPostPage(String pageId) {
        CmsPage cmsPage = this.getById(pageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("pageId", pageId);
        // 消息内容
        String msg = JSON.toJSONString(msgMap);
        // 获取站点id作为routingKey
        String siteId = cmsPage.getSiteId();
        // 发消息
        this.rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,
                siteId, msg);
    }

    //添加页面，如果已存在则更新页面
    public CmsPageResult save(CmsPage cmsPage) {
        //校验页面是否存在，根据页面名称、站点Id、页面webpath查询
        CmsPage one =
                cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(),
                        cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (one != null) {
            //更新
            return this.update(one.getPageId(), cmsPage);
        }
        //添加
        return this.add(cmsPage);
    }

    // 一键页面发布
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {

        // 1、将页面信息存储到cms_page集合中
        //添加页面
        CmsPageResult save = this.save(cmsPage); // 返回的是CmsPageResult
        if(!save.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        CmsPage saveCmsPage = save.getCmsPage();

        //要布的页面id
        String pageId = saveCmsPage.getPageId();

        // 2、执行页面发布(先静态化、保存GridFs、向MQ发送消息)
        ResponseResult responseResult = this.post(pageId);
        if(!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面的url
        //页面url=站点域名+站点webpath+页面webpath+页面名称
        //站点id
        String siteId = saveCmsPage.getSiteId();
        //查询站点信息
        CmsSite cmsSite = findCmsSiteById(siteId);
        //站点域名
        String siteDomain = cmsSite.getSiteDomain();
        //站点web路径
        String siteWebPath = cmsSite.getSiteWebPath();
        //页面web路径
        String pageWebPath = saveCmsPage.getPageWebPath();
        //页面名称
        String pageName = saveCmsPage.getPageName();
        //页面的web访问地址
        String pageUrl = siteDomain+siteWebPath+pageWebPath+pageName;
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);
    }

    //根据id查询站点信息
    public CmsSite findCmsSiteById(String siteId){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }
}