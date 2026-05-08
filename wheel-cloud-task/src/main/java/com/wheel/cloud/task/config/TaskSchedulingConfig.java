package com.wheel.cloud.task.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启 Spring @Scheduled 定时任务
 */
@Configuration
@EnableScheduling
public class TaskSchedulingConfig {
}
