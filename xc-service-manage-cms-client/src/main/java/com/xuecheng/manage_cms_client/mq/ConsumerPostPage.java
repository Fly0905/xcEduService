package com.xuecheng.manage_cms_client.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.service.PageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * 消息监听
 */
@Slf4j
@Component
public class ConsumerPostPage {

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    PageService pageService;

    @RabbitListener(queues = {"${xuecheng.mq.queue}"})
    public void postPage(String msg) {
        //解析消息 (msg中的json消息)
        Map map = JSON.parseObject(msg, Map.class);
        log.info("receive cms post page:{}", msg);
        //取出页面id
        String pageId = (String) map.get("pageId");
        //判断一下 查询页面信息
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if (!optional.isPresent()) {
            log.error("receive cms post page,cmsPage is null:{}", msg);
            return;
        }
        //将页面保存到服务器物理路径
        pageService.savePageToServerPath(pageId);
    }
}
