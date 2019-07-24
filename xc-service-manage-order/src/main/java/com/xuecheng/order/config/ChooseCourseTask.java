package com.xuecheng.order.config;


import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class ChooseCourseTask {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    TaskService taskService;

    @Scheduled(cron = "0/3 * * * * *")
    //每隔一分钟扫描消息表，向MQ发送消息
    public void sendChoosecourseTask(){
        //取出当前时间一分钟之前的时间
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.MINUTE , -1); //当前时间一分钟之前(前面的)时间
        Date time = calendar.getTime();
        List<XcTask> xcTaskList = taskService.findXcTaskList(time, 100);//前100
//        System.out.println(xcTaskList);
        for(XcTask xcTask : xcTaskList ){
            String ex = xcTask.getMqExchange();
            String routingKey = xcTask.getMqRoutingkey();

            //调用乐观锁方法校验任务是否可以执行 (版本号方式)
            if(taskService.getTask(xcTask.getId(), xcTask.getVersion()) > 0){
                //发送选课消息
                taskService.publish(xcTask, ex, routingKey);
                log.info("send choose course task id:{}", xcTask.getId());
            }
        }
    }

    //接受学习服务发送的消息
    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE)
    public void receiveFinishChoosecourseTask(XcTask xcTask){
        if(xcTask != null && StringUtils.isNotEmpty(xcTask.getId())){
            taskService.finishTask(xcTask.getId());
        }
    }

}
