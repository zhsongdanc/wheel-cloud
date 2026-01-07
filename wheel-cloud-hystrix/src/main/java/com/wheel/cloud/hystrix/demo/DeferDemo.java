package com.wheel.cloud.hystrix.demo;

import java.util.Date;

/**
 * defer 方法的作用演示
 * 
 * 这个类展示了为什么需要 defer，以及不使用 defer 的问题
 */
public class DeferDemo {

    /**
     * 问题场景：不使用 defer 的情况
     * 
     * 假设我们这样创建 Observable：
     * Observable<String> observable = Observable.fromCallable(() -> {
     *     System.out.println("执行时间：" + new Date());
     *     return "结果";
     * });
     * 
     * 问题：Observable 在创建时就已经确定了执行逻辑，但实际执行是在 subscribe 时。
     * 如果多次 subscribe，会有什么问题？
     */
    public void problemWithoutDefer() {
        /*
        // 创建 Observable（此时还没有执行）
        Observable<String> observable = Observable.fromCallable(() -> {
            System.out.println("执行时间：" + new Date());
            return "结果";
        });
        
        // 第一次订阅
        observable.subscribe(result -> System.out.println("第一次：" + result));
        
        // 第二次订阅
        observable.subscribe(result -> System.out.println("第二次：" + result));
        
        // 问题：两次订阅会执行两次，但这是正常的。
        // 真正的问题在于：如果 Observable 链中包含状态（如检查熔断器状态），
        // 不使用 defer 会导致状态在创建时就被"捕获"，而不是在每次订阅时重新检查。
        */
    }

    /**
     * 解决方案：使用 defer
     * 
     * defer 的作用：延迟创建 Observable，直到有订阅者订阅时才创建
     * 
     * 关键区别：
     * - 不使用 defer：Observable 在创建时就确定了执行逻辑
     * - 使用 defer：Observable 在每次订阅时才创建，可以捕获最新的状态
     */
    public void solutionWithDefer() {
        /*
        // 使用 defer：每次订阅时都会重新创建 Observable
        Observable<String> observable = Observable.defer(() -> {
            // 这个 lambda 在每次订阅时都会执行
            System.out.println("创建 Observable 的时间：" + new Date());
            
            // 在这里可以检查最新的状态（如熔断器状态）
            if (circuitBreaker.allowRequest()) {
                return Observable.fromCallable(() -> {
                    System.out.println("执行业务逻辑的时间：" + new Date());
                    return "结果";
                });
            } else {
                return Observable.error(new RuntimeException("熔断器打开"));
            }
        });
        
        // 第一次订阅：会执行 defer 中的 lambda，创建新的 Observable
        observable.subscribe(result -> System.out.println("第一次：" + result));
        
        // 第二次订阅：会再次执行 defer 中的 lambda，创建新的 Observable
        // 此时可以检查最新的熔断器状态
        observable.subscribe(result -> System.out.println("第二次：" + result));
        */
    }

    /**
     * 在 Hystrix 中的使用场景
     * 
     * 为什么 Hystrix 需要使用 defer：
     * 
     * 1. 每次调用 execute() 都需要重新检查熔断器状态
     *    - 不使用 defer：熔断器状态在创建 Observable 时就被"捕获"，后续调用无法感知状态变化
     *    - 使用 defer：每次订阅时重新检查，可以感知最新的熔断器状态
     * 
     * 2. 每次调用都需要重新执行业务逻辑
     *    - 不使用 defer：如果 Observable 被缓存，多次调用可能共享同一个执行结果
     *    - 使用 defer：每次订阅都创建新的 Observable，确保每次调用都重新执行
     * 
     * 3. 支持多次订阅
     *    - 如果同一个 Observable 被多次订阅，不使用 defer 可能导致状态混乱
     *    - 使用 defer：每次订阅都创建新的 Observable，互不影响
     */
    public void hystrixUseCase() {
        /*
        // Hystrix 中的实际使用
        Observable<T> executionObservable = Observable.defer(() -> {
            // 每次订阅时，都会重新执行这个 lambda
            // 1. 可以检查最新的熔断器状态
            // 2. 可以获取最新的配置
            // 3. 可以创建新的执行上下文
            
            return Observable.fromCallable(() -> {
                // 实际的业务逻辑
                return run();
            })
            .subscribeOn(threadPoolScheduler)
            .timeout(timeout, TimeUnit.MILLISECONDS);
        });
        
        // 第一次调用 execute()
        T result1 = executionObservable.toBlocking().single();
        
        // 第二次调用 execute()：会重新执行 defer 中的 lambda
        // 此时可以检查最新的熔断器状态、配置等
        T result2 = executionObservable.toBlocking().single();
        */
    }

    /**
     * 对比示例：不使用 defer 的问题
     */
    public void comparisonExample() {
        /*
        // ========== 场景1：不使用 defer ==========
        boolean circuitOpen = false;  // 初始状态：关闭
        
        Observable<String> withoutDefer = Observable.fromCallable(() -> {
            // 问题：这个 lambda 在创建 Observable 时就确定了
            // 即使后续 circuitOpen 变为 true，这个 Observable 仍然会执行
            if (circuitOpen) {
                throw new RuntimeException("熔断器打开");
            }
            return "执行结果";
        });
        
        // 创建 Observable 后，熔断器状态改变
        circuitOpen = true;
        
        // 订阅：仍然会执行，因为创建时 circuitOpen 是 false
        withoutDefer.subscribe(
            result -> System.out.println("结果：" + result),
            error -> System.out.println("错误：" + error.getMessage())
        );
        // 输出：结果：执行结果（错误！应该被熔断）
        
        // ========== 场景2：使用 defer ==========
        boolean circuitOpen2 = false;  // 初始状态：关闭
        
        Observable<String> withDefer = Observable.defer(() -> {
            // defer：每次订阅时都会重新执行这个 lambda
            // 可以捕获最新的 circuitOpen2 状态
            if (circuitOpen2) {
                return Observable.error(new RuntimeException("熔断器打开"));
            }
            return Observable.fromCallable(() -> "执行结果");
        });
        
        // 创建 Observable 后，熔断器状态改变
        circuitOpen2 = true;
        
        // 订阅：会重新执行 defer 中的 lambda，检查最新的 circuitOpen2 状态
        withDefer.subscribe(
            result -> System.out.println("结果：" + result),
            error -> System.out.println("错误：" + error.getMessage())
        );
        // 输出：错误：熔断器打开（正确！）
        */
    }
}



