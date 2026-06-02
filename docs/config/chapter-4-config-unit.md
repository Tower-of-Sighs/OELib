# Chapter 4: ConfigUnit：配置的运行时入口

前两章创建了配置文件并声明了字段。`ConfigSchema` 描述了配置的静态结构，`ConfigUnit` 管理它在运行时的生命周期。

## 4.1 ConfigSchema 与 ConfigUnit 的分工

```
ConfigSchema          描述"配置长什么样"
  defineClient(...)     →  字段、类型、默认值、校验规则、元数据
  Definition<T>

ConfigUnit            管理"配置怎么活着"
  get() / save()       →  加载、缓存、持久化
  reload()             →  重载
```

`ConfigSchema` 在初始化阶段完成，之后不再变化。`ConfigUnit` 持有当前值，贯穿游戏运行全程。

获取方式：

```java
public static final ConfigSchema.Definition<MyConfig> DEFINITION = ConfigSchema.defineClient(...);
public static final ConfigUnit<MyConfig> UNIT = DEFINITION.unit();
```

## 4.2 懒加载与缓存

```java
MyConfig config = UNIT.get();
```

首次调用时检查磁盘文件。文件存在则解码并校验，不存在则返回默认值。成功的加载结果被缓存。

加载失败时的回退顺序：

1. 上一次校验通过的值
2. 当前缓存值
3. Codec 中定义的默认值

后续调用直接返回缓存值，不涉及 I/O。

## 4.3 持久化

### 自动保存

某些修改方法默认写入磁盘（后续章节会介绍）。以下手动保存方式提供对持久化时机的精确控制。

### 手动保存

```java
UNIT.save();
```

将当前内存值写入磁盘。写入前执行全部校验，失败则回滚到最后有效值。

### 重载

```java
UNIT.reload();
```

丢弃缓存，重新从磁盘读取。客户端配置在资源包重载时自动触发。

## 4.4 事件系统

`ConfigUnit` 在关键节点派发事件：

| 事件 | 触发时机 |
|------|----------|
| `onLoad` | 从磁盘加载完成后 |
| `beforeSave` | 写入磁盘之前 |
| `afterSave` | 写入磁盘之后 |
| `onChanged` | 内存中的值被替换后 |

事件监听通过 `@Subscribe` 注解编写，详见[事件系统文档](../EVENT.md)和第 11 章。

## 4.5 完整生命周期

```
DEFINITION = ConfigSchema.defineClient(...)
UNIT = DEFINITION.unit()
  │
  ├── 首次 get()
  │     ├── 文件存在 → 加载 → 校验 → 缓存 → onLoad 事件
  │     └── 文件不存在 → 默认值 → 缓存
  │
  ├── save()
  │     ├── 校验 → 写入 → beforeSave / afterSave 事件
  │     └── 失败 → 回滚
  │
  └── reload()
        └── 丢弃缓存 → 重新加载
```

## 4.6 信息查询

```java
Identifier id = UNIT.id();           // 配置的唯一标识
ConfigMeta meta = UNIT.meta();        // 配置文件元信息
T defaultValue = UNIT.getDefaultValue();  // Codec 定义的默认值
```

## 下一步

本章介绍了 ConfigUnit 的加载、持久化和事件。第 2 章到第 4 章覆盖了配置的静态定义和运行时的基本管理——配置文件已经可以创建、读取、保存和重载了。下一章开始介绍如何修改配置的值。
