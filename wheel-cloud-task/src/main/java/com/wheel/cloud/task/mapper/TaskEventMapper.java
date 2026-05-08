package com.wheel.cloud.task.mapper;

import com.wheel.cloud.task.entity.TaskEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskEventMapper {

    int insert(TaskEvent event);

    List<TaskEvent> selectByTaskId(@Param("taskId") Long taskId);
}
