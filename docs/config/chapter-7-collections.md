# Chapter 7: 操作集合字段

前两章处理的是单个字段。配置中还有大量 List 和 Map 字段：白名单列表、速率限制映射、端口集合。本章介绍操作集合元素的 API。

## 7.1 List 元素更新

```java
record AppConfig(
        String name,
        List<String> whitelist,
        List<Integer> ports
) {}
```

### 全部元素

```java
UNIT.updateElements(AppConfig::whitelist, String::toLowerCase);
```

等价于对 whitelist 中的每个元素应用 `String::toLowerCase`，结果替换原 List：

```
["Alice", "Bob"]  →  ["alice", "bob"]
```

变换函数的类型是 `UnaryOperator<T>`——输入一个元素，输出一个元素。

### 条件元素

```java
UNIT.updateWhere(AppConfig::whitelist,
        name -> name.startsWith("@"),
        name -> name.substring(1)
);
```

只变换满足 predicate 的元素，不满足的保持不变：

```
["@admin", "Bob", "@root"]  →  ["admin", "Bob", "root"]
```

predicate 和 modifier 用标准 Java 组合：

```java
UNIT.updateWhere(AppConfig::ports,
        p -> p > 1024,
        p -> p + 1
);
```

### 与 `update` 的对比

`update(getter, modifier)` 替换整个 List。`updateElements` 和 `updateWhere` 只变换其中的元素，不改变列表长度。

```java
// update 替换整个 List
UNIT.update(AppConfig::whitelist, list -> {
    var copy = new ArrayList<>(list);
    copy.add("new_user");
    return copy;
});

// updateElements 只变换元素
UNIT.updateElements(AppConfig::whitelist, String::toLowerCase);
```

## 7.2 Map 值更新

Map 的 key 是标识，不应被批量修改。集合 API 对 Map 只提供值级别的操作。

```java
record AppConfig(
        String name,
        Map<String, Integer> rateLimits
) {}
```

### 全部值

```java
UNIT.updateValues(AppConfig::rateLimits, v -> v * 2);
```

遍历 Map 的所有 value，对每个应用 modifier，key 保持不变：

```
{"api": 100, "web": 50}  →  {"api": 200, "web": 100}
```

### 收集

```java
List<Integer> values = UNIT.getValues(AppConfig::rateLimits);
List<String> keys = UNIT.getKeys(AppConfig::rateLimits);
```

## 7.3 查询操作

不需要取出整个集合的查询。

```java
long n = UNIT.count(AppConfig::whitelist);            // 数量
boolean has = UNIT.anyMatch(AppConfig::whitelist,      // 存在
        name -> name.equals("@admin"));
boolean all = UNIT.allMatch(AppConfig::ports,          // 全部
        p -> p > 1024);
List<String> result = UNIT.getAll(AppConfig::whitelist);           // 收集全部
List<String> filtered = UNIT.getAllWhere(AppConfig::whitelist,     // 条件收集
        name -> name.startsWith("@"));
```

与 Stream API 的对应关系：

| 集合 API | Stream 等价 |
|----------|-------------|
| `UNIT.getAll(getter)` | `list.stream().toList()` |
| `UNIT.getAllWhere(getter, pred)` | `list.stream().filter(pred).toList()` |
| `UNIT.count(getter)` | `list.stream().count()` |
| `UNIT.anyMatch(getter, pred)` | `list.stream().anyMatch(pred)` |
| `UNIT.allMatch(getter, pred)` | `list.stream().allMatch(pred)` |
| `UNIT.updateElements(getter, op)` | `list.stream().map(op).toList()` + 提交 |
| `UNIT.updateWhere(getter, pred, op)` | `list.stream().map(x -> pred.test(x) ? op(x) : x).toList()` + 提交 |
| `UNIT.updateValues(getter, op)` | 遍历 values + 构造新 Map + 提交 |

区别在于 Stream 只操作内存，集合 API 把操作绑定到 ConfigUnit 的提交管道上。

## 下一步

本章介绍了集合字段的操作。接下来将所有这些操作——标量、条件、集合——统一到批量提交中，以及理解 `ConfigMutation` 如何将多个修改组合为一次事务。
