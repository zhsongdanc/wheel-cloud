package com.wheel.cloud.task.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskParam {

    /** 任务名称（必填） */
    private String taskName;

    /** 任务类型编码（必填），对应 t_task_type_config.task_type_code */
    private String taskTypeCode;

    /** 业务唯一ID（可选） */
    private String taskBizId;

    /** 执行器类型（可选，空时从配置读取） */
    private String executorType;

    private String creator;
    private String assignee;

    /** 优先级 0-100（可选，空时从配置读取） */
    private Integer priority;

    /** 输入数据 JSON（可选） */
    private String inputData;

    /** 父任务ID（可选，非空则创建为子任务） */
    private Long parentTaskId;

    /** 计划触发时间（可选，空则立即调度） */
    private LocalDateTime scheduledTime;

    /** 到期时间（可选） */
    private LocalDateTime expireTime;
}
