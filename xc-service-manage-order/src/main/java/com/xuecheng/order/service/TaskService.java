package com.xuecheng.order.service;


import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    XcTaskRepository xcTaskRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    XcTaskHisRepository xcTaskHisRepository;

    //查询前n条任务
    public List<XcTask> findXcTaskList(Date updateTime, int size) {

        // 按照时间降序排序
        Sort sort = new Sort(Sort.Direction.DESC, "updateTime");
        //设置分页参数
        Pageable pageable = PageRequest.of(0, size, sort);

        //查询前n条任务
        Page<XcTask> all = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);

        List<XcTask> contentList = all.getContent();

        return contentList;
    }


    //发布消息 (RabbitMQ)
    public void publish(XcTask xcTask, String ex, String routingKey){
        //更新时间
        Optional<XcTask> optionalXcTask = xcTaskRepository.findById(xcTask.getId());
        if(optionalXcTask.isPresent()){
            rabbitTemplate.convertAndSend(ex, routingKey, xcTask);
            XcTask one = optionalXcTask.get();
            one.setUpdateTime(new Date()); //更新时间
            xcTaskRepository.save(one);
        }
    }


    @Transactional //一定要记得加上
    public int getTask(String taskId, int version){
        int count = xcTaskRepository.updateTaskVersion(taskId, version);
        return count;
    }


    //完成任务
    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> optional = xcTaskRepository.findById(taskId);
        if(optional.isPresent()){
            //当前未完成任务
            XcTask xcTask = optional.get();
            //历史任务
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask, xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);  // 原有的里面删除
        }
    }
}
