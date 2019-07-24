package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsPage;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 定义dao层的查询接口
 */
public interface CmsPageRepository extends MongoRepository<CmsPage, String> {

    CmsPage findByPageName(String pageName);

    /**
     * 根据页面名称、站点id、页面访问路径查询
     * @param pageName          页面名称
     * @param siteId            站点id
     * @param pageWebPath       页面访问路径
     * @return
     */

    CmsPage findByPageNameAndSiteIdAndPageWebPath(String pageName, String siteId, String pageWebPath);
}
