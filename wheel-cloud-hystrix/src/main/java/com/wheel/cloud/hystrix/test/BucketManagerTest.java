package com.wheel.cloud.hystrix.test;

import com.wheel.cloud.hystrix.analytics.BucketManager;

public class BucketManagerTest {
    public static void main(String[] args) throws InterruptedException {
        BucketManager counter = new BucketManager();
        int threadCount = 100; // 100个并发线程
        int requestPerThread = 10000; // 每个线程1万次请求

        // 启动100个线程，并发记录请求
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < requestPerThread; j++) {
                    counter.recordSingle(System.currentTimeMillis(), j % 2 == 0); // 一半成功，一半失败
                }
            }).start();
        }

        // 等待所有线程执行完成
        Thread.sleep(5000);

        // 统计总请求数
        long total = counter.getTotalRequestInWindow();
        System.out.println("实际总请求数：" + total);
        System.out.println("期望总请求数：" + threadCount * requestPerThread);
        // 线程安全的情况下，实际值应等于期望值（允许±0，LongAdder无计数丢失）
    }
}
