## OELib Config System Developer Guide（已过时）

推荐阅读顺序：

1. 快速上手：最小可用配置
2. 存储格式与文件名
3. ConfigField 各种字段
4. ConfigUnit 的使用与事件
5. Dynamic 字段（高级扩展，放在最后详细讲）

---

## 1. 快速上手：最小可用配置

典型步骤：

1. 用 `ConfigRecordCodecBuilder` 描述一个配置结构。
2. 用 `ConfigManager.register` 注册一个 `ConfigUnit`。
3. 在代码里通过 `ConfigUnit.get()` 读取。

示例：客户端配置

```java
public final class ModConfigs {
    public static final ConfigUnit<ClientConfig> CLIENT =
            ConfigManager.register(
                    ResourceLocation.fromNamespaceAndPath(MyMod.MODID, "client"),
                    instance -> instance.group(
                            ConfigField.bool("enabled")
                                    .comment("Enable this feature")
                                    .forGetter(ClientConfig::enabled),
                            ConfigField.intRange("maxCount", 1, 100)
                                    .comment("Maximum count")
                                    .forGetter(ClientConfig::maxCount)
                    ).apply(instance, ClientConfig::new),
                    new ClientConfig(true, 10),
                    meta -> meta
                            .side(ConfigSide.CLIENT)
                            .format(ConfigStorageFormat.TOML)
            );

    public record ClientConfig(boolean enabled, int maxCount) {}
}
```

读取配置：

```java
boolean enabled = ModConfigs.CLIENT.get().enabled();
int max = ModConfigs.CLIENT.get().maxCount();
```

保存配置：

```java
ModConfigs.CLIENT.save();
```

---

## 2. 存储格式与文件名

枚举在 `ConfigStorageFormat`：

- `JSON`：标准 JSON，使用 `Gson` 严格解析。
- `JSON5`：使用 `json5-java` 解析 JSON5 语法，支持注释。
- `TOML`：使用 Night Config 的 TOML 实现，支持注释。

### 2.1 如何选择格式

在注册时通过 meta 自定义：

```java
meta
    .format(ConfigStorageFormat.TOML)
    .side(ConfigSide.CLIENT)
    .directory("mymod")
    .fileName("client");
```

格式常见用途：

- `TOML`：推荐给普通玩家编辑，支持注释、结构清晰，`ConfigField.comment` 会写入文件。
- `JSON`：机器友好，外部工具处理简单，不支持自动注释。
- `JSON5`：想写 JSON，但又希望支持注释时选择。

### 2.2 文件名与后缀规则

文件名由 `ConfigMeta` 控制：

- 默认：如果不指定 `fileName`，使用 `id.getPath()` （也就是 `ResourceLocation.fromNamespaceAndPath` 的第二个参数）作为基础名。
- 目录：如果设置了 `directory`，会在平台配置目录下创建对应子目录。

后缀规则：

- 如果 `fileName` 已包含后缀（包含 `.`）：
  - 读写都严格使用这个文件名，不做任何修改。
- 如果 `fileName` 不包含后缀：
  - 保存时：根据 `format` 枚举的小写值补后缀：
    - `JSON` → `.json`
    - `JSON5` → `.json5`
    - `TOML` → `.toml`
  - 加载时：
    - 优先尝试 `name + "." + format.name().toLowerCase()`。
    - 如果不存在，再尝试无后缀的文件名。

---

## 3. ConfigField：声明配置字段

`ConfigField` 是一组静态工厂，用来描述单个字段（类型 + 默认值 + UI + 权限等）。

常见字段：

- `ConfigField.bool("key")`
- `ConfigField.intRange("key", min, max)`
- `ConfigField.doubleRange("key", min, max)`
- `ConfigField.string("key")`
- `ConfigField.enumValue("key", MyEnum.class)`
- `ConfigField.custom("key", Codec<T>)`
- `ConfigField.dynamic("key")`（单独在下一节详细说明）

通用链式方法：

- `.comment(String)`：写入到 TOML/JSON5 的注释，或用于 UI 显示。
- `.ui(ConfigUiHint)`：指定 UI 控件类型（滑条、开关、下拉等）。
- `.defaultValue(T)`：字段自己的默认值（可选）。
- `.scope(ConfigScope)`：字段作用域，影响 UI 或同步策略。
- `.permissionLevel(int)`：需要的权限等级（通常用于服务端配置）。

### 3.1 一个综合示例

```java
public record GameplayConfig(
        boolean enabled,
        int maxStackSize,
        Mode mode
) {
    public enum Mode {
        SIMPLE,
        ADVANCED
    }

    public static ConfigCodec<GameplayConfig> codec() {
        return ConfigRecordCodecBuilder.create(
                "mymod:gameplay",
                instance -> instance.group(
                        ConfigField.bool("enabled")
                                .comment("Enable gameplay tweaks")
                                .ui(ConfigUiHint.toggle())
                                .defaultValue(true)
                                .forGetter(GameplayConfig::enabled),
                        ConfigField.intRange("maxStackSize", 1, 64)
                                .comment("Maximum item stack size")
                                .ui(ConfigUiHint.slider(1, 64, 1))
                                .defaultValue(16)
                                .forGetter(GameplayConfig::maxStackSize),
                        ConfigField.enumValue("mode", Mode.class)
                                .comment("Gameplay mode")
                                .ui(ConfigUiHint.dropdown(List.of("SIMPLE", "ADVANCED")))
                                .defaultValue(Mode.SIMPLE)
                                .forGetter(GameplayConfig::mode)
                ).apply(instance, GameplayConfig::new)
        );
    }
}
```

---

## 4. ConfigUnit：生命周期与事件

`ConfigUnit<T>` 封装了一份配置的完整生命周期：

- 懒加载：第一次调用 `get()` 时从磁盘读取。
- 保存：调用 `save()` 写回磁盘。
- 重载：调用 `reload()` 重新从磁盘加载。
- 事件：通过 `ConfigEvents` 向 SPI 分发。

常用方法：

- `T get()`：获取当前配置值（必要时触发加载）。
- `void save()`：将当前值写回磁盘。
- `void reload()`：从磁盘重新加载并触发事件。
- `ConfigMeta meta()`：访问元数据（文件名、格式、运行侧等）。

事件类型在 `cc.sighs.oelib.config.api`：

- `ConfigLoadEvent<T>`：加载完成后触发。
- `ConfigSaveEvent<T>`：保存前/保存后触发。
- `ConfigChangedEvent<T>`：值发生变化时触发（包括网络、GUI 等）。
- `ConfigSyncEvent<T>`：跨端同步时触发。

### 4.1 权限控制（服务端）

服务端配置的权限由“配置级别”和“字段级别”两种来源组成，遵循如下规则：

- 配置级别：`ConfigMeta.permissionLevel(int level)`，作为该配置的“全局权限”。
- 字段级别：在 `ConfigField.*` 的链式调用中设置 `.permissionLevel(int)`。

生效规则：

- 如果任意字段设置了 `permissionLevel > 0`，则以所有字段中“最大的权限等级”为准（覆盖配置级别权限）。
- 如果没有字段设置权限、而配置级别设置了 `permissionLevel > 0`，则使用配置级别权限。
- 如果两者都未设置（或为 0），则不做权限限制。

示例：

```java
public final class ModConfigs {
    public static void init() {
        ConfigManager.register(
                ResourceLocation.fromNamespaceAndPath("mymod", "server"),
                ServerConfig::codec,
                ServerConfig.defaultConfig(),
                meta -> meta
                        .side(ConfigSide.SERVER)
                        .permissionLevel(2) // 该配置的“全局权限”
        );
    }
}
```

---

## 5. Dynamic 字段（重点）

有些配置项在编译时无法确定具体结构，例如：

- 一个“任意 JSON” 区块，供高级用户写复杂条件。
- 一个“未来可能扩展”的结构，当前只要求保留原样。
- 需要允许模组间约定一段数据结构，但不想在当前模组里写死 Codec。

为此可以使用 `ConfigField.dynamic("key")`。

### 5.1 它到底是什么？

`ConfigField.dynamic` 返回的是一个基于 `com.mojang.serialization.Dynamic<?>` 的字段构造器。

简单理解：

- Dynamic 是 Mojang Codec 提供的一种“通用树节点”。
- 它可以在不同底层表示（JSON、TOML）之间自由转换。
- 你可以在运行时继续对它做结构化操作，比如：
  - 读取子字段：`dynamic.get("foo")`
  - 写入子字段：`dynamic.set("bar", value)`
  - 转换为某个类型：`dynamic.parse(someCodec)`

在 OELib 里：

- 当配置使用 JSON/JSON5 时，Dynamic 对应 JSON 树。
- 当配置使用 TOML 时，Dynamic 对应 TOML 的抽象树（通过 `TomlOps` 适配）。

### 5.2 声明一个 Dynamic 字段

示例：在配置里提供一个“高级规则”字段，允许用户写任意 JSON/TOML 结构。

```java
public record AdvancedConfig(
        boolean enabled,
        Dynamic<?> rules
) {
    public static ConfigCodec<AdvancedConfig> codec() {
        return ConfigRecordCodecBuilder.create(
                "mymod:advanced",
                instance -> instance.group(
                        ConfigField.bool("enabled")
                                .comment("Enable advanced rules")
                                .ui(ConfigUiHint.toggle())
                                .defaultValue(false)
                                .forGetter(AdvancedConfig::enabled),
                        ConfigField.dynamic("rules")
                                .comment("Advanced rule tree, structure is defined by you")
                                .defaultValue(new Dynamic<>(JsonOps.INSTANCE, JsonOps.INSTANCE.empty()))
                                .forGetter(AdvancedConfig::rules)
                ).apply(instance, AdvancedConfig::new)
        );
    }
}
```

关键点：

- Dynamic 字段本身只关心“这是一棵树”，不关心具体结构。
- 默认值可以用 `JsonOps.INSTANCE.empty()` 创建一个空树。
- 在 TOML 模式下，Dynamic 也能工作，因为底层通过 `DynamicOps` 适配。

### 5.3 在代码里读取 Dynamic

Dynamic 提供一套链式 API，配合 `DataResult` 使用。

示例：从规则中读取一个布尔开关 `rules.enabled` 和一个整数阈值 `rules.threshold`：

```java
public final class AdvancedRulesRuntime {
    public static boolean isRuleEnabled(AdvancedConfig config) {
        Dynamic<?> rules = config.rules();
        return rules.get("enabled")
                .asBoolean()
                .result()
                .orElse(false);
    }

    public static int threshold(AdvancedConfig config) {
        Dynamic<?> rules = config.rules();
        return rules.get("threshold")
                .asInt(0);
    }
}
```

常用方法（只列用法，不解释实现）：

- `dynamic.get(String key)`：获取子字段。
- `dynamic.asBoolean()` / `asInt()` / `asLong()` / `asDouble()` / `asString()`。
- `dynamic.result()`：转为 `Optional<T>`。
- `dynamic.orElse(T defaultValue)` / `asInt(defaultValue)`：带默认值的读取。

### 5.4 把 Dynamic 当成“原样存储块”

你也可以把 Dynamic 看成一个“黑盒存储”：

- 从外部 JSON/TOML 文件加载一段结构。
- 在运行时仅根据少数约定字段做轻量判断。
- 不对整个结构做强类型建模，避免频繁更改 Codec。

示例：只关心 `type` 字段，其余全部原样保留：

```java
public enum RuleType {
    SIMPLE,
    COMPLEX
}

public final class RuleInterpreter {
    public static RuleType type(AdvancedConfig config) {
        Dynamic<?> rules = config.rules();
        String name = rules.get("type")
                .asString()
                .result()
                .orElse("SIMPLE");
        try {
            return RuleType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return RuleType.SIMPLE;
        }
    }
}
```

这样可以让高级用户在配置里写更复杂的数据，而代码端只挑自己关心的部分。

### 5.5 Dynamic 与 JSON5 / TOML 的关系

- 如果配置格式是 `JSON` 或 `JSON5`：
  - Dynamic 底层对应 JSON 结构。
  - `JSON5` 模式下，解析时允许注释、尾逗号等宽松语法。
- 如果配置格式是 `TOML`：
  - Dynamic 底层对应 TOML 结构（通过 `TomlOps` 与 `CommentedConfig` 互转）。

你可以放心使用 Dynamic，不需要为不同格式写不同的解析逻辑。

### 5.6 一个完整的 Dynamic 示例（JSON）

假设有一个 JSON 配置：

```java
public static final ConfigUnit<Dynamic<?>> ADVANCED = ConfigManager.register(
        "mymod:advanced_rules",
        // 直接把整个 JSON 映射为一个 Dynamic 对象
        instance -> DynamicOps.LONG_DELAYED_CODEC.fieldOf("root").forGetter(d -> d),
        new Dynamic<>(JsonOps.INSTANCE, new JsonObject()),
        meta
            .format(ConfigStorageFormat.JSON)
            .directory("mymod")
            .fileName("advanced_rules")
);
```

配置文件 `advanced_rules.json` 内容示例：

```json5
{
  // 是否启用高级规则
  "enabled": true,

  // 阈值
  "threshold": 10,

  // 任意结构的规则树
  "rules": {
    // 这里可以随意扩展
    "condition": "player_health_below",
    "value": 8,
    "actions": [
      "send_message",
      "play_sound",
    ],
  },
}
```

在代码中（可以根据）：

```java
// 1. 获取整个配置文件的动态根节点
Dynamic<?> root = ModConfigs.ADVANCED.get();

// 2. 所有的字段都是动态提取的
// 对应 JSON 顶层的 "enabled"
boolean enabled = root.get("enabled").asBoolean().result().orElse(false);

// 对应 JSON 顶层的 "threshold"
int threshold = root.get("threshold").asInt(0);

// 3. 进入子层级 "rules"
Dynamic<?> rulesNode = root.get("rules");

// 对应 "rules" 内部的 "condition"
String condition = rulesNode.get("condition").asString().result().orElse("none");

// 对应 "rules" 内部的 "actions" 数组
List<String> actions = rulesNode.get("actions").asList(d -> d.asString(""));
```
