package com.xuecheng.order.dao;

import com.xuecheng.framework.domain.task.XcTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface XcTaskRepository extends JpaRepository<XcTask, String> {

    /**
     * 查询某个时间之间的前n条任务
     * 建议每个任务最多发3-5次,过多发送会浪费资源.如果反复失败,考虑人工处理
     *
     * @param pageable   分页对象,只取时间最久的数据
     * @param updateTime 记录每次读取到任务的时间
     * @return
     */

    Page<XcTask> findByUpdateTimeBefore(Pageable pageable, Date updateTime);

    Page<XcTask> findByUpdateTimeBeforeOrderByUpdateTimeDesc(Pageable pageable, Date updateTime);

    Page<XcTask> findByUpdateTimeBeforeAndVersionIsLessThanEqual(Pageable pageable, Date updateTime, Integer version);

    // 更新updateTime
    @Modifying
    @Query("update XcTask t set t.updateTime = :updateTime where t.id = :id")
    int updateTaskTime(@Param("id") String id, @Param("updateTime") Date updateTime);

    @Modifying
    @Query("update XcTask t set t.version = :version+1 where t.id = :id and t.version = :version")
    int updateTaskVersion(@Param("id") String id, @Param("version") int version);

}
