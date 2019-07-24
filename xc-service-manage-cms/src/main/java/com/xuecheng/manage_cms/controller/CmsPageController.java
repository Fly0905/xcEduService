package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsPageControllerApi;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.service.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试Page查询
 */
@RestController
@RequestMapping("/cms/page")
public class CmsPageController implements CmsPageControllerApi {

    @Autowired
    private PageService pageService;

    // 暂时定义静态数据
    @Override
    @GetMapping("/list/{page}/{size}")
    public QueryResponseResult findList(@PathVariable("page") int page,
                                        @PathVariable("size") int size,
                                        QueryPageRequest queryPageRequest) {
//        //暂时采用测试数据，测试接口是否可以正常运行
//        QueryResult queryResult = new QueryResult();
//        //静态数据列表
//        List list = new ArrayList();
//
//        CmsPage cmsPage = new CmsPage();
//        cmsPage.setPageName("测试页面");
//
//        list.add(cmsPage);
//
//        queryResult.setList(list);
//        queryResult.setTotal(1);
//
//        QueryResponseResult queryResponseResult = new
//                QueryResponseResult(CommonCode.SUCCESS, queryResult);

        return pageService.findList(page, size, queryPageRequest);
    }

    //通过@RequestBody可以将请求体中的JSON字符串绑定到相应的bean上 (json 转换成CmsPage对象)
    @Override
    @PostMapping("/add")
    public CmsPageResult add(@RequestBody CmsPage cmsPage) {
        return pageService.add(cmsPage);
    }

    @Override
    @GetMapping("/get/{id}")
    public CmsPage findById(@PathVariable("id") String id) {
        return pageService.getById(id);
    }

    // 注意要使用RequestBody
    @Override
    @PutMapping("/edit/{id}")//这里使用put方法，http 方法中put表示更新
    public CmsPageResult edit(@PathVariable("id") String id, @RequestBody CmsPage cmsPage) {
        return pageService.update(id,cmsPage);
    }

    @Override
    @DeleteMapping("/del/{id}")
    public ResponseResult delete(@PathVariable("id") String id) {
        return pageService.delete(id);
    }


    @Override
    @PostMapping("/postPage/{pageId}")
    public ResponseResult post(@PathVariable("pageId") String pageId) {
        return pageService.post(pageId);
    }

    @Override
    @PostMapping("/save")
    public CmsPageResult save(@RequestBody CmsPage cmsPage) {
        return pageService.save(cmsPage);
    }

    @Override
    @PostMapping("/postPageQuick")
    public CmsPostPageResult postPageQuick(@RequestBody CmsPage cmsPage) {
        return pageService.postPageQuick(cmsPage);
    }
}
