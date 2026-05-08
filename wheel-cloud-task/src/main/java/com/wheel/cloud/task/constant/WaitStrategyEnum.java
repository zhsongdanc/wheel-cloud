package com.wheel.cloud.task.constant;

/**
 * 子任务等待策略
 * ALL_SUCCESS：所有子任务成功父任务才继续；任一失败则父任务立即失败
 * ALL_DONE：所有子任务进入终止状态后父任务继续，由父任务自行处理结果
 */
public enum WaitStrategyEnum {

    ALL_SUCCESS("ALL_SUCCESS", "全部成功"),
    ALL_DONE("ALL_DONE", "全部完成");

    private final String code;
    private final String description;

    WaitStrategyEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static WaitStrategyEnum fromCode(String code) {
        if (code == null) return null;
        for (WaitStrategyEnum s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}
