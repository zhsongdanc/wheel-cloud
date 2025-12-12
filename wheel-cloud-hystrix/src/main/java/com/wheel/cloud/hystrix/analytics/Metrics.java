package com.wheel.cloud.hystrix.analytics;

import lombok.Data;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

@Data
public class Metrics {
    /**
     * 10s内的成功请求,按时间顺序入队
     */
    private BlockingQueue<InvokeInfo> successInvokeQueue = new SynchronousQueue<>(true);


    private BlockingQueue<InvokeInfo> failedInvokeQueue = new SynchronousQueue<>(true);


    public float getFailureRate() {
        return failedInvokeQueue.size() / (float) getTotalRequestCount();
    }

    public long getTotalRequestCount() {
        return successInvokeQueue.size() + failedInvokeQueue.size();
    }

    public void reInitialization() {
        successInvokeQueue.clear();
        failedInvokeQueue.clear();
    }

    public void clearAllBeforeTimestamp(long timestamp) {
        clearBeforeTimestamp(successInvokeQueue, timestamp);
        clearBeforeTimestamp(failedInvokeQueue, timestamp);
    }

    private void clearBeforeTimestamp(BlockingQueue<InvokeInfo> blockingQueue, long timestamp) {
        while (!blockingQueue.isEmpty() && Objects.nonNull(blockingQueue.peek()) && blockingQueue.peek().getStartTime() < timestamp) {
            blockingQueue.poll();
        }
    }

}
