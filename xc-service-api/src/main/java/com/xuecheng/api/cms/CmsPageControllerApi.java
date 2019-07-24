package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * Cms页面查询接口 API定义
 */
@Api(value = "cms页面管理接口", tags = "cms页面管理接口，提供页面的增、删、改、查")
public interface CmsPageControllerApi {

    /**
     * 使用分页查询
     * @param page             当前页面
     * @param size             每页显示数目
     * @param queryPageRequest
     * @return QueryResponseResult(返回类型)
     */
    @ApiOperation("分页查询页面列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "页码", required = true, paramType = "path", dataType = "int"),
            @ApiImplicitParam(name = "size", value = "每页记录数", required = true, paramType = "path", dataType = "int")
    })
    QueryResponseResult findList(int page, int size,
                                 QueryPageRequest queryPageRequest);

    // 新增页面
    @ApiOperation("添加页面")
    CmsPageResult add(CmsPage cmsPage);

    /**
     * 通过ID查询页面
     *
     * @param id
     * @return
     */
    @ApiOperation("通过ID查询页面")
    CmsPage findById(String id);

    /**
     * 修改页面
     *
     * @param id
     * @param cmsPage
     * @return
     */
    @ApiOperation("修改页面")
    CmsPageResult edit(String id, CmsPage cmsPage);

    /**
     * 删除页面
     *
     * @param id
     * @return
     */
    @ApiOperation("通过ID删除页面")
    ResponseResult delete(String id);


    @ApiOperation("发布页面")
    ResponseResult post(String pageId);

    @ApiOperation("保存页面")
    CmsPageResult save(CmsPage cmsPage);

    @ApiOperation("一键发布页面")
    CmsPostPageResult postPageQuick(CmsPage cmsPage);
}