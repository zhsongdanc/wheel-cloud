package com.wheel.cloud.task.mapper;

import com.wheel.cloud.task.entity.TaskTypeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface TaskTypeConfigMapper {

    TaskTypeConfig selectByTaskTypeCode(@Param("taskTypeCode") String taskTypeCode);

    List<TaskTypeConfig> selectByTaskTypeCodes(@Param("taskTypeCodes") Collection<String> taskTypeCodes);
}
