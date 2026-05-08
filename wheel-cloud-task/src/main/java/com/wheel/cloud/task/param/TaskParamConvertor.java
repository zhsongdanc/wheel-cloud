package com.wheel.cloud.task.param;

import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.entity.TaskTypeConfig;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TaskParamConvertor {

    /**
     * 将入参与类型配置合并，构建任务实体的公共基础字段
     * 以下字段由调用方负责填充：status, waitStrategy, wakeupContext, parentId, rootId, taskBizId
     */
    public Task toTask(CreateTaskParam param, TaskTypeConfig typeConfig) {
        Task task = new Task();
        task.setTaskName(param.getTaskName());
        task.setTaskTypeCode(param.getTaskTypeCode());
        task.setTaskBizId(param.getTaskBizId());
        task.setExecutorType(param.getExecutorType() != null
                ? param.getExecutorType() : typeConfig.getExecutorType());
        task.setCreator(param.getCreator());
        task.setAssignee(param.getAssignee());
        task.setPriority(param.getPriority() != null
                ? param.getPriority() : typeConfig.getPriority());
        task.setRetryCount(0);
        task.setMaxRetry(typeConfig.getMaxRetryCount());
        task.setVersion(0);
        task.setExecutionRound(1);

        LocalDateTime scheduledTime = param.getScheduledTime() != null
                ? param.getScheduledTime()
                : LocalDateTime.now();
        task.setScheduledTime(scheduledTime);
        task.setExpireTime(param.getExpireTime());
        task.setInputData(param.getInputData());
        return task;
    }
}
