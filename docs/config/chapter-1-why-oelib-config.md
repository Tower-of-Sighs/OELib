# Chapter 1: 为什么 OELib Config 存在

## 1.1 标新立异的 Codec 驱动配置

如果你写过 Minecraft Mod 的配置文件，大概率见过这样的模式：

```java
public class MyConfig {
    private int port = 25565;

    public void load() {
        // 手动读文件、解析、赋值
    }

    public void save() {
        // 手动序列化、写文件
    }
}
```

先定义一个类，再补一套序列化逻辑。配置的定义和读写是分离的两套代码，加一个字段需要同时在两处修改，漏掉一处就出 bug。

OELib Config 换了一个思路：**先有 Codec，后有配置**。

Codec 是 Mojang 序列化体系的核心抽象——它同时承载编码和解码的能力。在 OELib Config 中，Codec 不再是配置写完后的附属品，而是**整个配置系统的单一事实来源**。从 Codec 出发，系统自动推导出：

- 配置的默认值
- 序列化与反序列化
- 字段级别的校验规则
- 版本迁移策略
- 每个字段在 UI 上对应的控件类型
- 翻译键

你只描述一次数据结构，其余全部自动完成。

## 1.2 核心目标

**消除重复定义。** 字段类型只在一处声明——Codec 中。不再有"Java 字段声明一份、反序列化一份、序列化一份"的三份重复。

**类型安全的字段访问。** 字段通过 Record 的 getter 方法引用索引，而非字符串路径。编译器验证路径和类型，不存在 "Key 拼写错误导致运行时静默失败"。

**零模板代码的持久化。** 加载时机、缓存、写回、事件派发、客户端与服务端同步——这一整套生命周期由 `ConfigUnit` 自动管理。

## 1.3 最终效果展示

假设你需要配置监端口、白名单和数据库连接信息。下面是完整实现：

```java
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

record ServerConfig(
    int port,
    boolean whitelistEnabled,
    List<String> whitelist,
    Optional<Integer> maxPlayers,
    DatabaseConfig database
) {}

record DatabaseConfig(String host, int port, String username, String password) {}

var definition = ConfigSchema.defineServer(
    Identifier.fromNamespaceAndPath("mymod", "server"),
    ServerConfig.class,
    meta -> meta.fileName("mymod-server"),
    schema -> schema.group(

        ConfigField.intRange("port", 1024, 65535)
            .defaultValue(25565)
            .forGetter(ServerConfig::port),

        ConfigField.bool("whitelist_enabled")
            .defaultValue(false)
            .forGetter(ServerConfig::whitelistEnabled),

        ConfigField.list("whitelist", Codec.STRING)
            .defaultValue(List.of())
            .forGetter(ServerConfig::whitelist),

        ConfigField.optional("max_players", Codec.INT)
            .forGetter(ServerConfig::maxPlayers),

        ConfigSchema.record("database", DatabaseConfig.class,
            sub -> sub.group(
                ConfigField.string("host").defaultValue("localhost").forGetter(DatabaseConfig::host),
                ConfigField.intRange("port", 1, 65535).defaultValue(3306).forGetter(DatabaseConfig::port),
                ConfigField.string("username").defaultValue("root").forGetter(DatabaseConfig::username),
                ConfigField.string("password").defaultValue("").forGetter(DatabaseConfig::password)
            ).apply(sub, DatabaseConfig::new),
            ServerConfig::database
        )

    ).apply(schema, ServerConfig::new)
);

definition.registerServer(player -> player.hasPermission(4));
ConfigUnit<ServerConfig> unit = definition.unit();
```

注册完成后，从 Codec 出发的系统自动完成了：

- 在 `config/mymod-server.toml` 生成配置文件（如果不存在）
- 首次加载时自动填充所有默认值
- 每次修改后自动校验 `port` 是否在 1024-65535 之间
- 字段的翻译键自动生成为 `config.mymod.server.port` 等格式

运行时读写配置：

```java
// 读取
ServerConfig current = unit.get();

// 修改集合中的元素，自动持久化
unit.updateElements(ServerConfig::whitelist, String::toLowerCase);
unit.updateWhere(ServerConfig::whitelist,
    name -> name.startsWith("temp_"),
    name -> "archived_" + name
);

// 查询
long count = unit.count(ServerConfig::whitelist);
boolean hasAny = unit.anyMatch(ServerConfig::whitelist, name -> !name.isEmpty());
```

注意 `updateElements` 接受的是 Record 的 getter 方法引用（`ServerConfig::whitelist`），而非字符串。如果字段被重命名，编译器直接报错。每次修改自动执行 校验 → 持久化 → 事件派发，你不会写出一个损坏的配置文件。

整个例子中你不需要了解任何 optics 概念。这背后确实存在一套 optics 体系在支撑编译期安全的组合和零开销的字段访问，但它们被完全封装在内部。你只需要理解你的配置长得什么样，然后用 Codec 把它描述出来。

## 下一步

本章展示了 OELib Config 的全貌。下一章将从零开始，创建一个完整的配置文件，涵盖 Record 定义、Codec 编写、ConfigSchema 创建、ConfigUnit 获取、以及日常的读写操作。
