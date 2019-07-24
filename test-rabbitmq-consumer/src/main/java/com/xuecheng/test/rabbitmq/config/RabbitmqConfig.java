package com.xuecheng.test.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitmqConfig {

    public static final String QUEUE_INFORM_EMAIL = "topic_queue_inform_email";
    public static final String QUEUE_INFORM_SMS = "topic_queue_inform_sms";
    public static final String EXCHANGE_TOPICS_INFORM = "exchange_topics_inform";
    public static final String ROUTINGKEY_EMAIL = "inform.#.email.#";
    public static final String ROUTINGKEY_SMS = "inform.#.sms.#";

    //声明交换机
    @Bean(EXCHANGE_TOPICS_INFORM) // 指明bean的名称
    public Exchange exchange() {
        //durable(true) 持久化，mq重启之后交换机还在
        return ExchangeBuilder.topicExchange(EXCHANGE_TOPICS_INFORM).durable(true).build();
    }

    //声明QUEUE_INFORM_EMAIL队列
    @Bean(QUEUE_INFORM_EMAIL)
    public Queue email_queue() {
        return new Queue(QUEUE_INFORM_EMAIL);
    }

    //声明QUEUE_INFORM_SMS队列
    @Bean(QUEUE_INFORM_SMS)
    public Queue sms_queue() {
        return new Queue(QUEUE_INFORM_SMS);
    }

    //ROUTINGKEY_EMAIL队列绑定交换机，指定routingKey
    @Bean
    public Binding email_binding_queue(@Qualifier(QUEUE_INFORM_EMAIL) Queue queue,
                                       @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTINGKEY_EMAIL).noargs();
    }

    //ROUTINGKEY_SMS队列绑定交换机，指定routingKey
    @Bean
    public Binding sms_binding_queue(@Qualifier(QUEUE_INFORM_SMS) Queue queue,
                                     @Qualifier(EXCHANGE_TOPICS_INFORM) Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTINGKEY_SMS).noargs();
    }
}
