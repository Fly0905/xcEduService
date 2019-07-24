package com.xuecheng.test.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 发布/订阅 模式下的手机短信消费者
 * 下面去掉了注释
 */
public class Consumer02_subscribe_sms {

    //改变一下队列即可
    private static final String QUEUE_INFORM_SMS = "fanout_queue_inform_sms";
    private static final String EXCHANGE_FANOUT_INFORM = "exchange_fanout_inform";


    public static void main(String[] args) throws IOException, TimeoutException {
        //创建一个与MQ的连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");//rabbitmq默认虚拟机名称为“/”，虚拟机相当于一个独立的mq服务器
        //创建一个连接
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_FANOUT_INFORM, BuiltinExchangeType.FANOUT);
        channel.queueDeclare(QUEUE_INFORM_SMS, true, false, false, null);
        channel.queueBind(QUEUE_INFORM_SMS, EXCHANGE_FANOUT_INFORM, "");
        //定义消费方法
        DefaultConsumer defaultConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                long deliveryTag = envelope.getDeliveryTag();
                String exchange = envelope.getExchange();
                //消息内容
                String message = new String(body, "utf-8");
                System.out.println(message);
            }
        };
        channel.basicConsume(QUEUE_INFORM_SMS, true, defaultConsumer);
    }
}
