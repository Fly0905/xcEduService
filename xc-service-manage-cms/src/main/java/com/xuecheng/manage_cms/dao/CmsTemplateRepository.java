package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 静态化模板操作
 */
public interface CmsTemplateRepository extends MongoRepository<CmsTemplate,String> {
}