package com.wheel.cloud.hystrix.property;

import com.wheel.cloud.hystrix.spring.HalfOpenToOpenStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wheel.cloud.hystrix")
public class CommandProperty {

    private HalfOpenToOpenStrategy halfOpenToOpenStrategy;

    // 熔断器关闭状态下的时间窗口
    private long timeWindowWhenClosed = 10*1000;
    // 熔断器关闭状态下的失败比例阈值，达到这个比例断路器打开
    private float closedToOpenFailedRatio = 0.5f;
    // 熔断器关闭状态下的失败数量阈值，达到这个数量断路器打开
    private int closedToOpenMinTotalCount = 10;

    // 探测冷却期，每隔该时间才会转为half open 去探测
    private long coolDownTime = 5*1000;

    // half open状态最久持续时间，超出后强制改为OPEN状态
    private int halfOpenToOpenMaxTimeWindow = 3*1000;
    // half open状态可以发送的总的请求总数
    private int halfOpenTotalRequest = 20;
    // half open状态成功请求数阈值
    private int halfOpenToOpenMinSuccessCount = 3;
    // half open状态成功请求比例阈值
    private float halfOpenToOpenMinSuccessRatio = 0.75f;


    public HalfOpenToOpenStrategy getHalfOpenToOpenStrategy() {
        return halfOpenToOpenStrategy;
    }

    public void setHalfOpenToOpenStrategy(HalfOpenToOpenStrategy halfOpenToOpenStrategy) {
        this.halfOpenToOpenStrategy = halfOpenToOpenStrategy;
    }

    public long getTimeWindowWhenClosed() {
        return timeWindowWhenClosed;
    }

    public void setTimeWindowWhenClosed(long timeWindowWhenClosed) {
        this.timeWindowWhenClosed = timeWindowWhenClosed;
    }

    public float getClosedToOpenFailedRatio() {
        return closedToOpenFailedRatio;
    }

    public void setClosedToOpenFailedRatio(float closedToOpenFailedRatio) {
        this.closedToOpenFailedRatio = closedToOpenFailedRatio;
    }

    public int getClosedToOpenMinTotalCount() {
        return closedToOpenMinTotalCount;
    }

    public void setClosedToOpenMinTotalCount(int closedToOpenMinTotalCount) {
        this.closedToOpenMinTotalCount = closedToOpenMinTotalCount;
    }

    public long getCoolDownTime() {
        return coolDownTime;
    }

    public void setCoolDownTime(long coolDownTime) {
        this.coolDownTime = coolDownTime;
    }

    public int getHalfOpenToOpenMaxTimeWindow() {
        return halfOpenToOpenMaxTimeWindow;
    }

    public void setHalfOpenToOpenMaxTimeWindow(int halfOpenToOpenMaxTimeWindow) {
        this.halfOpenToOpenMaxTimeWindow = halfOpenToOpenMaxTimeWindow;
    }

    public int getHalfOpenTotalRequest() {
        return halfOpenTotalRequest;
    }

    public void setHalfOpenTotalRequest(int halfOpenTotalRequest) {
        this.halfOpenTotalRequest = halfOpenTotalRequest;
    }

    public int getHalfOpenToOpenMinSuccessCount() {
        return halfOpenToOpenMinSuccessCount;
    }

    public void setHalfOpenToOpenMinSuccessCount(int halfOpenToOpenMinSuccessCount) {
        this.halfOpenToOpenMinSuccessCount = halfOpenToOpenMinSuccessCount;
    }

    public float getHalfOpenToOpenMinSuccessRatio() {
        return halfOpenToOpenMinSuccessRatio;
    }

    public void setHalfOpenToOpenMinSuccessRatio(float halfOpenToOpenMinSuccessRatio) {
        this.halfOpenToOpenMinSuccessRatio = halfOpenToOpenMinSuccessRatio;
    }
}
