# Chapter 5: 修改配置值

前几章介绍了配置的创建、字段声明和生命周期管理。本章介绍如何修改配置中的值。

## 5.1 修改单个字段

`ConfigUnit` 提供了修改单个字段的方法：

```java
UNIT.update(MyConfig::port, v -> v + 1);
UNIT.update(MyConfig::name, v -> v + "_v2");
```

第一个参数是 Record 的 getter 方法引用，指向要修改的字段。第二个参数是变换函数——接收当前值，返回新值。

每次修改的流程：读取当前值 → 应用变换 → 校验 → 持久化 → 事件。全部自动执行。

### 设为固定值

如果新值已知且不依赖当前值，仍然可以用变换函数形式，或配合批量提交（见第 8 章）：

```java
UNIT.update(MyConfig::port, v -> 8080);
```

## 5.2 字段定位

`update` 的第一个参数是 getter 方法引用，编译器会验证该 getter 是否属于当前配置 Record。以下两种写法等价：

```java
// 直接传入 getter
UNIT.update(MyConfig::port, v -> v + 1);

// 通过 DEFINITION 创建字段引用后再用
var portField = RecordLensBuilder.lens(MyConfig.class, MyConfig::port);
UNIT.update(portField, v -> v + 1);
```

第二种方式将字段引用存为变量，在需要反复使用同一字段时避免重复解析。字段引用内部称为 ConfigLens——它知道如何从 Record 中读取和写入该字段，支持组合（compose）以访问嵌套字段。

### 组合访问嵌套字段

当配置包含嵌套 Record 时，可以通过 compose 组合两个字段引用：

```java
record AppConfig(DatabaseConfig database, String name) {}
record DatabaseConfig(String host, int port) {}

var dbField = RecordLensBuilder.lens(AppConfig.class, AppConfig::database);
var hostField = RecordLensBuilder.lens(DatabaseConfig.class, DatabaseConfig::host);
var dbHostField = dbField.compose(hostField);

UNIT.update(dbHostField, v -> v + "-replica");
```

组合后的字段引用 `dbHostField` 直通 `AppConfig.database.host`。`set` 操作从内向外重建 Record，仅修改目标字段，其他字段不受影响。

## 5.3 修改过程

每次 `update` 调用触发：

1. 从缓存中读取当前 Record
2. 用 getter 提取字段值
3. 应用变换函数
4. 将新值写回 Record
5. 执行所有注册的 validator
6. 写入磁盘
7. 派发变更事件

如果 validator 抛出异常，第 6 步被取消，当前缓存值不变。

## 5.4 集合字段

对于 List 和 Map 字段，`update` 接收的是整个集合，而非集合中的元素：

```java
UNIT.update(MyConfig::names, list -> {
    var copy = new ArrayList<>(list);
    copy.add("new_name");
    return copy;
});
```

集合中每个元素的批量操作（全部转大写、条件替换等）由第 7 章介绍。

## 下一步

本章介绍的是"确定存在"的字段的修改。配置中还存在两类特殊情况：字段可能为空、或多个子类型之一。下一章处理这种条件更新场景。
