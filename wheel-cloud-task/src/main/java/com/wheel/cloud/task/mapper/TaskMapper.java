package com.wheel.cloud.task.mapper;

import com.wheel.cloud.task.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {

    Long insert(Task task);

    int update(Task task);

    int updateIncludeNull(Task task);

    Task selectById(@Param("id") Long id);

    Task selectByBizIdOne(@Param("taskBizId") String taskBizId);

    List<Task> selectByParentId(@Param("parentId") Long parentId);

    List<Task> selectByBizIdAndTaskType(@Param("taskBizId") String taskBizId,
                                        @Param("taskTypeCode") String taskTypeCode);

    /** PENDING 且 scheduled_time <= scheduledBefore */
    List<Task> selectSchedulablePending(@Param("scheduledBefore") java.time.LocalDateTime scheduledBefore,
                                        @Param("limit") int limit);

    /** RETRYING 且 scheduled_time <= NOW() */
    List<Task> selectPendingRetry(@Param("limit") int limit);

    /** RUNNING 且 start_time < threshold（超时候选） */
    List<Task> selectAbnormalRunning(@Param("threshold") java.time.LocalDateTime threshold,
                                     @Param("limit") int limit);

    /** 按 status 分页查询 */
    List<Task> selectByStatus(@Param("status") String status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);
}
