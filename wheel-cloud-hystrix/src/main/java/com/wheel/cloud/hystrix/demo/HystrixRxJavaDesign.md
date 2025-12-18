# Hystrix 如何融入 RxJava 的核心设计

## 一、核心设计思想

Hystrix 将**所有执行逻辑都包装成 Observable**，通过 RxJava 的链式操作实现：
- 线程隔离
- 超时控制
- 熔断检查
- 降级处理
- 请求缓存
- 指标收集

## 二、主流程解析

### 1. 执行入口

```java
// 同步执行
public T execute() {
    return toObservable().toBlocking().single();
}

// 异步执行
public Observable<T> toObservable() {
    // 构建 Observable 链
}
```

**关键点**：`execute()` 内部调用 `toObservable()`，然后通过 `toBlocking().single()` 阻塞等待结果。

### 2. Observable 链的构建顺序

```
请求进入
  ↓
检查熔断器（allowRequest）
  ↓
创建执行 Observable（defer + fromCallable）
  ↓
线程隔离（subscribeOn）
  ↓
超时控制（timeout）
  ↓
错误处理（onErrorResumeNext）
  ↓
降级逻辑（getFallbackObservable）
  ↓
请求缓存（cache）
  ↓
指标收集（doOnSubscribe/doOnNext/doOnError）
  ↓
返回结果
```

### 3. 线程隔离的实现

**核心**：使用 RxJava 的 `Scheduler` 封装线程池

```java
Observable.fromCallable(() -> run())
    .subscribeOn(threadPoolScheduler)  // 在指定线程池执行
```

**Hystrix 的设计**：
- 每个 `CommandGroup` 对应一个线程池
- 通过 `Schedulers.from(threadPoolExecutor)` 创建 Scheduler
- 所有业务逻辑都在这个线程池中执行，实现线程隔离

### 4. 超时控制的实现

**核心**：使用 RxJava 的 `timeout()` 操作符

```java
Observable.fromCallable(() -> run())
    .timeout(1000, TimeUnit.MILLISECONDS)
    .onErrorResumeNext(error -> {
        if (error instanceof TimeoutException) {
            return getFallbackObservable();  // 超时后降级
        }
        return Observable.error(error);
    });
```

**关键点**：
- `timeout()` 会在指定时间后抛出 `TimeoutException`
- 通过 `onErrorResumeNext()` 捕获超时异常，执行降级逻辑

### 5. 降级逻辑的实现

**核心**：使用 `onErrorResumeNext()` 操作符

```java
executionObservable
    .onErrorResumeNext(error -> {
        if (shouldAttemptFallback(error)) {
            return getFallbackObservable();  // 返回降级 Observable
        }
        return Observable.error(error);
    });
```

**降级 Observable 的构建**：
```java
Observable.defer(() -> {
    T fallbackResult = getFallback();
    return Observable.just(fallbackResult);
})
.subscribeOn(fallbackScheduler)  // 降级也有独立线程池
.timeout(fallbackTimeout, TimeUnit.MILLISECONDS);
```

### 6. 熔断检查的时机

**关键点**：熔断检查在**构建 Observable 时**进行，而不是在执行时

```java
public Observable<T> toObservable() {
    // 1. 先检查熔断器
    if (!circuitBreaker.allowRequest()) {
        return Observable.error(new RuntimeException("Circuit breaker is OPEN"))
                .onErrorResumeNext(e -> getFallbackObservable());
    }
    
    // 2. 然后构建执行 Observable
    Observable<T> executionObservable = Observable.defer(() -> {
        // ...
    });
    
    // ...
}
```

**为什么这样设计**：
- 如果熔断器打开，直接返回降级 Observable，不执行实际业务逻辑
- 避免在业务逻辑执行过程中检查熔断器，减少不必要的资源消耗

### 7. 指标收集的实现

**核心**：使用 `doOn*` 系列操作符

```java
Observable<T> withMetrics = source
    .doOnSubscribe(() -> metrics.markRequestStart())
    .doOnNext(result -> metrics.markRequestSuccess())
    .doOnError(error -> metrics.markRequestFailure(error))
    .doOnTerminate(() -> metrics.markRequestEnd());
```

**关键点**：
- `doOnSubscribe`：订阅时触发（请求开始）
- `doOnNext`：成功时触发
- `doOnError`：失败时触发
- `doOnTerminate`：无论成功或失败都会触发（请求结束）

### 8. 请求缓存的实现

**核心**：使用 `ReplaySubject` 或 `cache()` 操作符

```java
// 方式1：使用 cache()
Observable<T> cached = source.cache();

// 方式2：使用 ReplaySubject
ReplaySubject<T> cacheSubject = ReplaySubject.create();
source.subscribe(cacheSubject);
Observable<T> cached = cacheSubject.asObservable();
```

**Hystrix 的设计**：
- 相同请求参数的请求可以共享同一个 Observable
- 使用 `ReplaySubject` 缓存结果，后续相同请求直接返回缓存

## 三、RxJava 的优势

### 1. 链式编程
所有逻辑通过链式调用组织，代码清晰易读。

### 2. 异步非阻塞
Observable 天然支持异步，可以轻松实现异步调用。

### 3. 错误处理
通过 `onErrorResumeNext()`、`onErrorReturn()` 等操作符统一处理错误。

### 4. 线程调度
通过 `subscribeOn()`、`observeOn()` 灵活控制线程切换。

### 5. 操作符丰富
`timeout()`、`retry()`、`cache()` 等操作符直接支持常用功能。

## 四、关键设计模式

### 1. 装饰器模式
通过 Observable 链式操作，层层装饰原始逻辑：
- 原始逻辑：`run()`
- 装饰1：线程隔离（`subscribeOn`）
- 装饰2：超时控制（`timeout`）
- 装饰3：降级处理（`onErrorResumeNext`）
- 装饰4：指标收集（`doOn*`）

### 2. 责任链模式
Observable 链就是一个责任链：
- 每个操作符负责一个功能
- 按顺序执行，前一个失败则进入下一个处理

### 3. 观察者模式
Observable 本身就是观察者模式的实现：
- 业务逻辑是 Observable
- 指标收集、监控是 Observer
- 通过 `doOn*` 操作符注册观察者

## 五、总结

Hystrix 使用 RxJava 的核心思路：
1. **将所有逻辑包装成 Observable**：业务逻辑、降级逻辑、缓存逻辑都是 Observable
2. **通过链式操作组织功能**：线程隔离、超时、降级、指标收集都是 Observable 链上的节点
3. **利用 RxJava 的特性**：Scheduler 实现线程隔离，timeout 实现超时，onErrorResumeNext 实现降级
4. **统一错误处理**：所有错误都通过 Observable 的错误流处理，统一且清晰

这种设计的优势：
- **代码清晰**：逻辑通过链式调用组织，一目了然
- **易于扩展**：新增功能只需在链上添加操作符
- **统一抽象**：所有逻辑都是 Observable，统一处理
- **异步友好**：天然支持异步，性能更好

