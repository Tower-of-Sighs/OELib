# Chapter 8: 批量更新与 ConfigMutation

第 5 章的 `update`、第 6 章的 `ifPresent`、第 7 章的 `updateElements` 各自独立提交——每次操作触发一次校验、一次持久化、一次事件。当需要同时修改多个相关字段时，独立的多次提交有三个问题：

- 多次 I/O
- 非原子性（部分成功、部分失败）
- 多次事件可能引起连锁反应

`ConfigMutation` 将多个修改组合为一次原子提交。

## 8.1 为什么需要批量

```java
// 三次 update，三次独立提交
UNIT.update(MyConfig::port, v -> v + 1);
UNIT.update(MyConfig::name, v -> v + "_v2");
UNIT.update(MyConfig::threshold, v -> v * 2);
```

三次校验、三次磁盘写入、三次事件。如果第二次失败，第一次的修改已写入磁盘。

用 `updateAll` 合并为一次：

```java
UNIT.updateAll(
    ConfigMutation.map(MyConfig::port, v -> v + 1),
    ConfigMutation.map(MyConfig::name, v -> v + "_v2"),
    ConfigMutation.map(MyConfig::threshold, v -> v * 2)
);
```

三个变换在内存中依次应用到同一个初始值上，然后一次校验、一次持久化、一次事件。

## 8.2 Mutation 工厂

`ConfigMutation` 以静态工厂方法提供所有创建方式：

### set / map

```java
ConfigMutation.set(MyConfig::port, 25565);            // 设为固定值
ConfigMutation.map(MyConfig::port, v -> v + 1);        // 应用变换
```

### ifPresent

```java
ConfigMutation.ifPresent(MyConfig::greeting, v -> v + "!");
ConfigMutation.ifPresent(MyConfig::mode, ExperimentalMode.class, ExperimentalMode::upgrade);
```

### updateElements / updateWhere / updateValues

```java
ConfigMutation.updateElements(MyConfig::whitelist, String::toLowerCase);
ConfigMutation.updateWhere(MyConfig::whitelist, n -> n.startsWith("temp_"), n -> "archived_" + n);
ConfigMutation.updateValues(MyConfig::limits, v -> v * 2);
```

## 8.3 提交方式

### updateAll：自动持久化

```java
UNIT.updateAll(
    ConfigMutation.set(MyConfig::port, 25565),
    ConfigMutation.map(MyConfig::name, v -> v + "_v2"),
    ConfigMutation.ifPresent(MyConfig::greeting, v -> v + "!")
);
```

所有 mutation 按顺序应用，结果只校验一次、只写一次磁盘、只派发一次事件。

### updateAllNoSave：不自动持久化

```java
ConfigUnitOps.updateAllNoSave(UNIT,
    ConfigMutation.set(MyConfig::port, 25565),
    ConfigMutation.set(MyConfig::name, "restarted")
);
// ... 其他操作
UNIT.save();
```

适用于需要将配置修改与其他操作一起提交的场景。

### BatchMutator：条件批量

```java
ConfigUnitOps.withBatchNoSave(UNIT, batch -> {
    if (needsReset) {
        batch.update(MyConfig::port, v -> 25565);
    }
    batch.updateInt(MyConfig::retries, v -> v + 3);
});
if (batch.changed()) {
    UNIT.save();
}
```

`withBatchNoSave` 按条件决定执行哪些 mutation，`batch.changed()` 判断是否有实际变更。

## 8.4 原子性

| | `update` | `updateAll` |
|---|---|---|
| 校验次数 | 每次调用一次 | 所有 mutation 完成后一次 |
| 磁盘写入 | 每次调用一次 | 所有 mutation 完成后一次 |
| 变更事件 | 每次调用一次 | 所有 mutation 完成后一次 |
| 原子性 | 单字段 | 多字段整体 |

单字段修改用 `update`。多字段关联修改用 `updateAll`。

## 下一步

本章介绍了 ConfigMutation 的概念和用法。前八章覆盖了配置的定义、生命周期、修改和批量操作。第 9 章转向一个不同的话题：配置文件版本迁移。
