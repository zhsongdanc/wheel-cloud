-- 任务主表
CREATE TABLE IF NOT EXISTS t_task (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_biz_id           VARCHAR(128)  DEFAULT '',
    task_name             VARCHAR(256)  NOT NULL,
    task_type_code        VARCHAR(64)   NOT NULL,
    executor_type         VARCHAR(64)   DEFAULT NULL,
    wait_strategy         VARCHAR(32)   DEFAULT NULL,
    creator               VARCHAR(64)   DEFAULT NULL,
    assignee              VARCHAR(64)   DEFAULT NULL,
    priority              INT           DEFAULT 50,
    status                VARCHAR(32)   NOT NULL,
    sub_status            VARCHAR(64)   DEFAULT NULL,
    failure_reason        VARCHAR(512)  DEFAULT NULL,
    session_id            VARCHAR(128)  DEFAULT NULL,
    version               INT           DEFAULT 0,
    parent_id             BIGINT        DEFAULT NULL,
    parent_execution_round INT          DEFAULT NULL,
    root_id               BIGINT        DEFAULT NULL,
    execution_round       INT           DEFAULT 1,
    input_data            TEXT          DEFAULT NULL,
    output_data           TEXT          DEFAULT NULL,
    wakeup_context        TEXT          DEFAULT NULL,
    scheduled_time        DATETIME      DEFAULT NULL,
    start_time            DATETIME      DEFAULT NULL,
    end_time              DATETIME      DEFAULT NULL,
    expire_time           DATETIME      DEFAULT NULL,
    retry_count           INT           DEFAULT 0,
    max_retry             INT           DEFAULT 0,
    error_msg             VARCHAR(512)  DEFAULT NULL,
    create_time           DATETIME      DEFAULT NULL,
    update_time           DATETIME      DEFAULT NULL
);

-- 任务事件表（审计日志）
CREATE TABLE IF NOT EXISTS t_task_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id      BIGINT       NOT NULL,
    event_type   VARCHAR(64)  NOT NULL,
    from_status  VARCHAR(32)  DEFAULT NULL,
    to_status    VARCHAR(32)  DEFAULT NULL,
    operator     VARCHAR(64)  DEFAULT NULL,
    event_time   DATETIME     DEFAULT NULL,
    create_time  DATETIME     DEFAULT NULL
);

-- 任务重试日志表
CREATE TABLE IF NOT EXISTS t_task_retry_log (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id           BIGINT       NOT NULL,
    session_id        VARCHAR(128) DEFAULT NULL,
    retry_count       INT          DEFAULT 0,
    error_type        VARCHAR(64)  DEFAULT NULL,
    error_code        VARCHAR(64)  DEFAULT NULL,
    error_msg         VARCHAR(512) DEFAULT NULL,
    stack_trace       TEXT         DEFAULT NULL,
    execute_duration  INT          DEFAULT NULL,
    start_time        DATETIME     DEFAULT NULL,
    end_time          DATETIME     DEFAULT NULL,
    next_retry_time   DATETIME     DEFAULT NULL,
    retry_strategy    VARCHAR(32)  DEFAULT 'FIXED',
    create_time       DATETIME     DEFAULT NULL
);

-- 任务类型配置表
CREATE TABLE IF NOT EXISTS t_task_type_config (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type_code          VARCHAR(64)  NOT NULL UNIQUE,
    task_type_name          VARCHAR(128) DEFAULT NULL,
    executor_type           VARCHAR(64)  DEFAULT NULL,
    max_retry_count         INT          DEFAULT 0,
    retry_interval_seconds  INT          DEFAULT 60,
    retry_strategy          VARCHAR(32)  DEFAULT 'FIXED',
    timeout_seconds         INT          DEFAULT NULL,
    enabled                 INT          DEFAULT 1,
    priority                INT          DEFAULT 50,
    default_expire_seconds  INT          DEFAULT NULL,
    use_session             INT          DEFAULT 0,
    description             VARCHAR(512) DEFAULT NULL,
    rate_limit_key          VARCHAR(128) DEFAULT NULL,
    rate_limit_events       VARCHAR(256) DEFAULT NULL,
    create_time             DATETIME     DEFAULT NULL,
    update_time             DATETIME     DEFAULT NULL
);
