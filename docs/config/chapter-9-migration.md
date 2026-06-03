# Chapter 9: 配置文件版本迁移

> 本章涉及 OELib Config 的高级版本迁移特性。在阅读本篇之前，我们默认您已经具备一定的 Mojang DFU (DataFixerUpper) 基础，并了解 Codec 和 DynamicOps 的核心概念。
> 如果您目前的 Mod 尚未涉及复杂的跨版本配置变更（如字段拆分、重命名等），通常无需手动处理 Dynamic 树，可以先跳过本章。
> 由于目前国内/外社区关于 Mojang DFU 的详细生态教程较少，如果您在实际开发中不得不需要此功能，可以结合本章提供的代码示例，利用 AI 辅助工具（如 LLM）进行逐行概念拆解和代码行为分析。

配置文件的格式会随着 Mod 版本迭代而变化：字段被重命名、取值被限制范围、结构被重新组织。如果新版本的 Mod 直接读取旧版本的配置文件，轻则取到错误的值，重则解码失败导致配置被重置。

本章介绍 OELib Config 的迁移系统——在加载配置时自动将旧版格式转换为当前格式。

## 9.1 为什么需要迁移

假设一个 Mod 在 v1.0 中有如下配置：

```java
record AppConfig(String host, int timeout) {}
// 序列化后: { "host": "localhost", "timeout": 30 }
```

在 v2.0 中，`host` 被拆分为 `address` 和 `port`：

```java
record AppConfig(String address, int port, int timeout) {}
```

此时旧文件 `{ "host": "localhost", "timeout": 30 }` 无法直接解码为新 Record——`host` 字段不存在于新 Record 中，`address` 和 `port` 字段不存在于旧文件中。

迁移系统的职责：在加载时检测配置文件来自哪个版本，按需执行一系列变换，使其匹配当前版本的 schema。

## 9.2 两种迁移机制

OELib Config 提供两个层次的迁移：


| 机制           | 适用范围                | 执行时机                         |
| -------------- | ----------------------- | -------------------------------- |
| 字段级迁移     | 单个字段的值变换        | 每次`load()`，解码之后、校验之前 |
| 全局数据修复链 | 整个配置的 Dynamic 变换 | 注册时一次，解码之前             |

字段级迁移在 `BaseFieldBuilder.migrate()` 声明，全局链在 `ConfigFixRegistry.register()` 注册。

## 9.3 字段级迁移

### 声明

在字段声明链中附加 `migrate(version, transformer)`：

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .validate(...)
        .migrate(1, dynamic -> {
            int old = dynamic.asInt(25565);
            return dynamic.createInt(Math.abs(old));
        })
        .forGetter(AppConfig::port);
```

`migrate` 的第一个参数是版本号（升序整数），第二个参数是变换函数——接收 `Dynamic<?>`，返回变换后的 `Dynamic<?>`。

### 执行过程

每次从磁盘加载配置时，`ConfigUnit.load()` 按以下顺序执行：

```
文件 → 解码 → 字段级迁移 → 校验 → 缓存
```

字段级迁移的内部流程：

1. 收集所有字段声明的迁移项，按版本号升序排列
2. 将整个配置值编码为 `JsonObject`
3. 遍历每个迁移项：按字段路径取出对应的 `JsonElement`
4. 将 `JsonElement` 包装为 `Dynamic<JsonElement>`，应用变换
5. 将变换后的值写回 `JsonObject` 的同一条路径
6. 将完整的 `JsonObject` 重新解码为类型化 Record

字段级迁移被限定在单个字段的值上，无法访问其他字段。跨字段操作（拆分字段、重命名）需要使用全局链。

## 9.4 全局数据修复链

全局链在配置文件被解码之前执行，操作完整的 `Dynamic<?>` 树，可以任意修改整个文档结构。

### 注册

```java
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import com.mojang.serialization.Dynamic;

ConfigFixRegistry.register(
    Identifier.fromNamespaceAndPath("mymod", "app"),
    2,
    chain -> {
        chain.fix(0, 1, dyn -> {
            // v0→v1：将 "host" 拆分为 "address" + "port"
            // 提取旧值中的 host，保留玩家配置的值而非硬编码默认值
            String host = dyn.get("host").asString("localhost");
            return dyn.remove("host")
                    .set("address", dyn.createString(host))
                    .set("port", dyn.createInt(25565));
        });

        chain.fix(1, 2, dyn -> {
            // v1→v2：maxPlayers 值取绝对值
            return dyn.update("maxPlayers", val ->
                    dyn.createInt(Math.abs(val.asInt(0))));
        });
    }
);
```

`fix(fromVersion, toVersion, operator)` 注册一个从版本号 `fromVersion` 升级到下一版本的变换。`toVersion` 仅用于文档标注。版本链必须连续——如果缺少中间版本的 fix，执行中断。

### 格式无关性

迁移内部使用 JSON 作为中间格式操作配置树，但这不意味着迁移只支持 JSON 文件。DFU 的 `DynamicOps` 体系支持格式之间的无损转换——TOML 文件被 `TomlOps` 解析，迁移前通过 `dynamic.convert(JsonOps.INSTANCE)` 转为 JSON 树，操作完成后 `convert` 回原始格式。这套机制对开发者完全透明，**只需注册一次链，所有存储格式（TOML、JSON5、JSON）共享同一份迁移逻辑**。

### DFU Dynamic 常用 API


| 类别 | 方法                                    | 作用                               |
| ---- | --------------------------------------- | ---------------------------------- |
| 读取 | `get(key)`                              | 获取子字段，返回`OptionalDynamic`  |
| 读取 | `get(key).asInt(N)` / `asString(N)`     | 取子字段值，缺失返回默认值         |
| 读取 | `asInt(N)` / `asString(N)`              | 提取当前节点为标量，失败返回默认值 |
| 读取 | `asStream()` / `asList(fn)`             | 提取为列表                         |
| 读取 | `asMap(kFn, vFn)`                       | 提取为 Map                         |
| 读取 | `read(codec)`                           | 用 Codec 解码当前节点              |
| 写入 | `set(key, value)`                       | 设置子字段，返回新 Dynamic         |
| 写入 | `remove(key)`                           | 删除子字段，返回新 Dynamic         |
| 写入 | `update(key, fn)`                       | 字段存在时变换其值                 |
| 写入 | `renameField(old, new)`                 | 重命名字段                         |
| 创建 | `createInt(v)` / `createString(v)`      | 创建标量值的 Dynamic               |
| 创建 | `createList(stream)` / `createMap(map)` | 创建容器值的 Dynamic               |
| 创建 | `emptyList()` / `emptyMap()`            | 创建空容器                         |
| 转换 | `convert(outOps)`                       | 转换到其他 DynamicOps              |
| 映射 | `map(fn)`                               | 变换内部原始值                     |

示例：

```java
// 读取
String host = dyn.get("host").asString("localhost");
int port = dyn.get("port").asInt(25565);
int max = dyn.get("limits.max").asInt(100);  // 嵌套路径

// 写入
dyn = dyn.set("address", dyn.createString(host));
dyn = dyn.remove("old_field");
dyn = dyn.renameField("host", "address");

// 条件更新（字段存在才变换）
dyn = dyn.update("maxPlayers", val -> dyn.createInt(Math.abs(val.asInt(0))));

// 创建容器
dyn = dyn.set("ports", dyn.createList(
    Stream.of(dyn.createInt(80), dyn.createInt(443))
));
```

### 执行过程

全局链在配置单元注册时执行一次，直接操作文件内容：

```
磁盘文件 → 解析为 Dynamic → 提取 __cfg_version → 逐级应用链 → 解码 → 写回磁盘
```

`__cfg_version` 是 OELib Config 在写入文件时自动附加的版本号。加载时读取，与 `currentVersion` 比较，文件版本低于当前版本则执行迁移。

## 9.5 FixContext 工具

`ConfigFixRegistry.FixContext` 在 `ConfigPathUtil` 基础上提供 JSON 路径级别的便利操作。

### rename

```java
Dynamic<?> result = new FixContext(dynamic).rename("old.path", "new.path");
```

内部执行 `getJsonByPath` → `removeJsonByPath` → `setJsonByPath`。源路径不存在则不执行。

### setIfMissing

```java
Dynamic<?> result = new FixContext(dynamic).setIfMissing("timeout", FixContext.json("60"));
```

字段已存在时不覆盖，不存在则设置。

### json

```java
JsonElement port = FixContext.json("25565");
```

### 示例

```java
ConfigFixRegistry.register(
    Identifier.fromNamespaceAndPath("mymod", "server"),
    2,
    chain -> {
        chain.fix(0, 1, dyn ->
                new FixContext(dyn).rename("old_host", "address"));
        chain.fix(1, 2, dyn ->
                new FixContext(dyn).setIfMissing("port", FixContext.json("25565")));
    }
);
```

## 9.6 字段级迁移与全局链的配合

```
字段级迁移                         全局链
─────────────                     ────────
声明在字段定义中                    注册在独立代码中
操作单个字段的值                    操作整个文档
在每次加载时执行                    在注册时执行一次
适用于值格式变换                    适用于结构调整
（int 取绝对值、格式标准化）         （重命名字段、拆分字段）
```

全局链处理结构性变更，字段级迁移处理值级变更。两者版本号互不干扰——全局链的版本写入 `__cfg_version`，字段级迁移的版本仅用于排序。

配合使用：

```java
// 全局链：v0→v1 将 "host" 重命名为 "address" 并添加 "port"
ConfigFixRegistry.register(id, 1, chain ->
    chain.fix(0, 1, dyn -> {
        String host = dyn.get("host").asString("localhost");
        return dyn.remove("host")
                .set("address", dyn.createString(host))
                .set("port", dyn.createInt(25565));
    })
);

// 字段级迁移：v1 开始 address 的值统一转为小写
ConfigField.string("address")
    .defaultValue("localhost")
    .migrate(1, dyn -> dyn.map(v -> v.asString("").toLowerCase()))
    .forGetter(MyConfig::address);
```

## 下一步

本章介绍了配置的版本迁移。前九章覆盖了配置的定义、生命周期、修改、集合操作、批量提交和迁移。最后一章揭开这些 API 背后的光学理论：Lens、Prism、Traversal 和 Fold 如何构成整个系统的类型安全基础。
