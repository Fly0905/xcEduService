package com.xuecheng.test.rabbitmq;


import com.xuecheng.test.rabbitmq.config.RabbitmqConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class Producer05_topic_springboot {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    public void testSendEmail() {
        /**
         * 参数
         * 1、 交换机名称
         * 2、 routingKey
         * 3、 消息内容
         */
        String msg = "send email message to user";
        rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_TOPICS_INFORM,
                "inform.email", msg);
    }
}
