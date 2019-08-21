package com.xuecheng.manage_cms.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 注意在生产方这边，只需要创建交换机即可，不需要创建队列
@Configuration
public class RabbitmqConfig {
    //交换机的名称
    public static final String EX_ROUTING_CMS_POSTPAGE = "ex_routing_cms_postpage";

    /**
     * 交换机配置使用direct类型
     *
     * @return the exchange
     */
    @Bean(EX_ROUTING_CMS_POSTPAGE)
    public Exchange EXCHANGE_TOPICS_INFORM() {
        return ExchangeBuilder.directExchange(EX_ROUTING_CMS_POSTPAGE).durable(true).build();
    }

}