# OELib 事件库（跨平台 · 轻量 · 高性能）

OELib 事件库是一个跨平台（Fabric/Forge/NeoForge）、轻量、高性能的事件分发组件。  
它以注解监听的友好写法提供统一事件模型，同时在实现层面使用编译期/运行时优化，让分发开销在极小量级。

---

## 一、定位与整体思路

OELib 的事件库围绕以下核心定位设计：

- **跨平台统一**：在 common-api 定义事件与总线，平台侧仅桥接原生事件或注入点。
- **轻量实现**：最小依赖、最少状态；事件是简单数据对象，分发用纯数组迭代。
- **高性能分发**：注册时构建方法引用（LambdaMetafactory），分发走缓存的 `Handler[]`，避免反射热路径。
- **并发安全**：支持多线程分发；注册/注销与分发并行安全，但推荐在初始化期完成注册。

在 API 设计上，OELib 把事件系统拆分成几块：

- 事件类型：`Event`、`CancellableEvent`
- 监听注解：`@Subscribe`（携带 priority / side / receiveCanceled）
- 执行顺序控制：`EventPriority`
- 逻辑侧控制：`EventSide`
- 总线与分发：`EventBus`
- 自动注册：`EventAutoRegistration`

---

## 二、核心类型与语义

### 2.1 Event / CancellableEvent

文件：
- [`Event`](../common-api/src/main/java/cc/sighs/oelib/event/Event.java)
- [`CancellableEvent`](../common-api/src/main/java/cc/sighs/oelib/event/CancellableEvent.java)

`Event` 是一个纯标记接口，所有要通过 `EventBus` 分发的事件都必须实现它。  
OELib 不强制继承具体基类（如 `BaseEvent`），你可以根据自己 Mod 的领域建模。

`CancellableEvent` 继承自 `Event`，增加了：

- `boolean isCanceled()`
- `void setCanceled(boolean canceled)`
- `default void cancel()`

`EventBus` 在分发时会识别 `CancellableEvent`，并按监听器的 `receiveCanceled` 配置决定是否在取消后继续调用。  
需要注意：

- 取消状态没有做并发保护，默认假设在主线程（或单线程）中使用。
- 取消是“协作式”的：总线不会自动阻断外部逻辑，你仍然需要在桥接 / 调用处根据取消状态决定是否继续处理原生事件。

### 2.2 监听注解 Subscribe

文件：
- [`Subscribe`](../common-api/src/main/java/cc/sighs/oelib/event/Subscribe.java)

`@Subscribe` 用于标记事件监听方法，要求：

- 方法返回类型必须是 `void`
- 必须且只能有一个参数，类型实现了 `Event`
- 方法可以是实例方法，也可以是静态方法

注解参数：

- `priority: EventPriority`  
  按优先级从高到低执行：`HIGHEST > HIGH > NORMAL > LOW > LOWEST`。

- `side: EventSide`  
  控制监听在哪个逻辑侧注册：
  - `CLIENT`：仅在 `Platform.isClient()` 为 true 时注册
  - `SERVER`：仅在 `Platform.isServer()` 为 true 时注册
  - `BOTH`：两个侧都会注册

- `receiveCanceled: boolean`  
  - 对于 `CancellableEvent`，当事件已取消且该值为 `false` 时，本监听不会再收到事件。
  - 对于非可取消事件，此设置无影响。

### 2.3 Priority / Side 枚举

文件：
- [`EventPriority`](../common-api/src/main/java/cc/sighs/oelib/event/EventPriority.java)
- [`EventSide`](../common-api/src/main/java/cc/sighs/oelib/event/EventSide.java)

设计上：

- Priority 是监听器的前后关系控制，用于解决多个 Mod/子系统都在同一事件上监听时的顺序问题。
- Side 控制的是逻辑侧，不是物理线程；OELib 不自动区分渲染线程 / 逻辑线程，只根据平台接口判断 client/server。

---

## 三、事件总线 EventBus 的实现与使用

文件：
- [`EventBus`](../common-api/src/main/java/cc/sighs/oelib/event/EventBus.java)

### 3.1 运行时模型

EventBus 在运行时维护两层结构：

1. `directHandlers: Map<Class<? extends Event>, Handler[]>`  
   - 每个事件类型（包括抽象基类接口）对应一个按 priority 排序好的 `Handler[]`
   - 注册 / 注销时只操作这一层结构，并递增一个 `cacheEpoch`

2. `dispatch: ClassValue<CachedDispatch>`  
   - 对于每个“实际事件类”，缓存它对应的“完整 handler 数组”
   - 这个数组是根据继承树（父类 + 接口）收集到的 Handler 合并后的结果
   - 当发现 `cacheEpoch` 变化时，会重算一次并更新缓存

分发时的热路径大致等价于：

```java
Handler[] handlers = getDispatchHandlers(event.getClass());
for (Handler h : handlers) { h.invoke(event); }
```

其中 `Handler.invoke` 是用 `LambdaMetafactory` 生成的，性能接近手写方法调用。

### 3.2 线程安全与并发模型

- `post` / `postInternal`：
  - 可以从多个线程同时调用。
  - 内部只读 `directHandlers` 与 per-class 的缓存数组（Handler[]），不会对同一 Handler 做写操作。

- `register` / `unregister`：
  - 使用 `ConcurrentHashMap` + 复制数组的方式更新 `directHandlers`，不会在读线程上加锁。
  - 每次变更会递增 `cacheEpoch`，触发对应事件类在下一次分发时重建缓存。

经验上：

- **推荐**在 Mod 初始化阶段完成所有注册，运行期只做少量、明确的注销。
- **不推荐**在每 tick 或每请求中频繁注册 / 注销，这会在高并发下带来明显开销（见性能一节）。

### 3.3 异步事件 postAsync

- `postAsync` 会在一个可配置的 `Executor` 上异步调用 `postInternal`。
- 出于线程安全与语义明确的考虑：
  - 目前明确禁止对 `CancellableEvent` 调用 `postAsync`（会抛出异常）。
  - 建议仅对“纯数据事件”使用，且事件对象本身设计为不可变。
- 大部分和 Minecraft 世界状态有关的事件仍然应当在主线程上分发。

---

## 四、自动注册与包结构建议

文件：
- [`EventAutoRegistration`](../common-api/src/main/java/cc/sighs/oelib/event/EventAutoRegistration.java)
- [`AnnotationScanUtil`](../common-api/src/main/java/cc/sighs/oelib/util/AnnotationScanUtil.java)

自动注册的工作流程：

1. 平台端提供注解扫描器（Fabric / Forge 各有实现），通过 `ServiceLoader` 加载。
2. `AnnotationScanUtil` 调用平台扫描器，在指定包中扫描所有类。
3. 找出至少包含一个 `@Subscribe` 方法的类。
4. 对每个类：
   - 如果有非静态 `@Subscribe` 方法，则 new 一个默认构造实例，注册为实例监听器。
   - 否则，作为静态监听类注册。

### 4.1 推荐包结构

为了让扫描足够精确、性能可控，建议：

- 把所有事件监听器定义在一个或少数几个专门的包下，例如：
  - `your.modid.event`
  - `your.modid.event.client`
  - `your.modid.event.server`
- 初始化时只对这些包调用：

```java
EventAutoRegistration.registerBasePackage("your.modid.event");
int registered = EventAutoRegistration.registerAllListeners();
```

这样可以避免扫描整个 Mod 包结构，也更利于维护（事件相关代码都放在一个区域）。

### 4.2 自动注册的注意点

- 实例监听类必须有“无参构造器”才能被自动实例化。
- 静态监听类只要方法满足 `@Subscribe` 要求，不需要构造器。
- 自动注册只看方法上的 `@Subscribe`，不关心类上是否有其它标记。
- 若你需要非常精细的控制顺序或生命周期，也可以完全不用自动注册，手动调用 `EventBus.register(...)` 即可。

---

## 五、性能特性与实测数据

基准测试代码：

- 单线程、多参数场景：[`EventBusJmhBenchmarks`](../common-api/src/jmh/java/cc/sighs/oelib/event/benchmark/EventBusJmhBenchmarks.java)
- 并发与缓存失效场景：[`EventBusConcurrentBenchmarks`](../common-api/src/jmh/java/cc/sighs/oelib/event/benchmark/EventBusConcurrentBenchmarks.java)

以下数据仅用于把握数量级，来自当前 JDK / 机器环境下的一组 JMH 测试（`Mode=AverageTime`，单位 ns/op）：

- **基线（直接方法调用）**
  - baselineDirectInvoke：约 1–2 ns/op

- **单线程分发（监听数量与继承深度的影响）**
  - `listenerCount = 1, depth = 0`：约 14–30 ns/op
  - `listenerCount = 32, depth = 0`：约 200–250 ns/op
  - `listenerCount = 256, depth = 0`：约 1.7–2.1 μs/op
  - `listenerCount = 256, depth = 2`：约 5–6 μs/op

- **可取消事件**
  - 在相同监听数量下，`cancellable = true` 和 `false` 的开销非常接近，取消状态检查影响较小。

- **多线程分发**
  - 4 线程：32 监听时约 200–300 ns/op，256 监听时约 1.9 μs/op
  - 16 线程：32 监听约 500–600 ns/op，256 监听约 4–5 μs/op

- **频繁注册 / 注销 + 分发（极端场景）**
  - 在每次分发前都做一次注册 + 注销（模拟缓存频繁失效）时，开销会来到 100 μs 以上这一量级。
  - 这是一个刻意构造的“反模式”场景，主要用于验证缓存失效行为。

### 5.1 从数据得到的实践建议

结合上述数据，可以给出以下经验性建议：

1. **不要刻意追求“零成本总线”**  
   在常见使用场景下（少量到几十个监听），绝大部分时间仍然花在监听方法自身逻辑中，而不是 EventBus 的调度。

2. **适度控制单个事件的监听数量**  
   - 从 1 个到几十个监听，分发开销的线性增长是预期内的。
   - 极端场景（几百个以上监听）一般来自架构设计问题，而不是事件系统问题。

3. **避免在热路径频繁 register / unregister**  
   - 每次修改监听集合都会触发相关事件类型的缓存重建。
   - 初始化期（如 FML/Fabric 初始化回调）做完注册，运行时尽量只做偶发性的注销是最佳实践。

4. **异步事件要谨慎使用**  
   - 目前 `postAsync` 明确不支持 `CancellableEvent`。
   - 如果需要在后台线程做耗时操作，更推荐的方式是：
     - 在主线程分发一个“开始处理”事件，收集必要参数。
     - 在后台线程处理完毕后，再通过安全的方式将结果同步回主线程。

---

## 六、使用示例（从零到一）

下面以一个简单的示例展示如何从零开始接入 OELib 事件系统。

### 6.1 定义事件

```java
package your.modid.event;

import cc.sighs.oelib.event.Event;
import cc.sighs.oelib.event.CancellableEvent;

public final class CustomEvents {

    public static final class SimpleEvent implements Event {
        public final int value;

        public SimpleEvent(int value) {
            this.value = value;
        }
    }

    public static final class CancelableEvent implements CancellableEvent {
        private boolean canceled;

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }
    }
}
```

### 6.2 编写监听器

```java
package your.modid.event;

import cc.sighs.oelib.event.*;

public final class CustomEventListeners {

    @Subscribe(priority = EventPriority.HIGH, side = EventSide.SERVER)
    public void onSimple(CustomEvents.SimpleEvent event) {
        // 处理逻辑
    }

    @Subscribe(receiveCanceled = true)
    public void onCancelable(CustomEvents.CancelableEvent event) {
        if (!event.isCanceled()) {
            // 决定是否取消
            event.cancel();
        } else {
            // 即便事件被取消，由于 receiveCanceled = true，仍然可以做收尾工作
        }
    }
}
```

### 6.3 注册监听器

#### 手动注册

在你的初始化入口（如 Mod 初始化类）中：

```java
EventBus.register(new CustomEventListeners());
```

当你需要静态监听时：

```java
EventBus.register(CustomEventListeners.class);
```

#### 自动注册（推荐版本）

如果你将所有监听器都放在 `your.modid.event` 包下，可以在初始化阶段执行：

```java
EventAutoRegistration.registerBasePackage("your.modid.event");
int registered = EventAutoRegistration.registerAllListeners();
```

这样可以避免手动维护长长的注册列表，只需要保证监听器类：

- 放在指定的包下
- 符合 `@Subscribe` 要求
- 若使用实例方法，有无参构造器

### 6.4 触发事件

在合适的时机（例如某个游戏逻辑、命令、网络消息处理内）：

```java
CustomEvents.SimpleEvent event = new CustomEvents.SimpleEvent(42);
EventBus.post(event);
```

如果你需要根据事件是否被取消来决定后续逻辑：

```java
CustomEvents.CancelableEvent event = new CustomEvents.CancelableEvent();
EventBus.post(event);
if (event.isCanceled()) {
    // 不再继续默认行为
}
```

---

## 七、总结与最佳实践清单

可以把 OELib 的事件系统理解为：

- 写法上接近注解事件总线
- 实现上尽可能接近“直接方法调用”的性能
- 功能上覆盖跨侧 / 取消 / 优先级等常见需求

在项目中使用时，可以参考以下清单：

1. 所有事件类型实现 `Event`，需要取消的实现 `CancellableEvent`。
2. 监听方法使用 `@Subscribe`，遵守：`void` 返回 + 单参数。
3. 按用途合理使用 `priority`、`side`，避免滥用最高优先级。
4. 把所有监听器放在一个或少数几个包下，并使用 `EventAutoRegistration` 扫描注册。
5. 在初始化阶段完成注册，运行期避免频繁 register/unregister。
6. 对异步场景谨慎使用 `postAsync`，不要传入 `CancellableEvent`。
7. 如遇到性能问题，优先检查监听器本身逻辑，其次再看是否存在异常的监听数量或注册模式。

在遵守上述原则的前提下，OELib 的事件系统可以在绝大多数 Mod 场景中提供足够低的开销和良好的可维护性。 
