# Chapter 2: 创建第一个配置文件

本章从空白开始，创建一个配置文件。完成后你可以在游戏配置目录下看到这个文件，并在代码中读取它的值。

## 2.1 定义 Record

所有配置都是一个 Java Record：

```java
import java.util.List;

public record GreetingConfig(
        String message,
        boolean enabled,
        int interval,
        List<String> targets
) {}
```

Record 的每个 component 对应配置文件中的一个字段。

## 2.2 定义 ConfigSchema

用 `ConfigSchema.defineClient` 声明配置的字段名、类型、默认值，并绑定到 Record 的 component 上：

```java
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record GreetingConfig(
        String message,
        boolean enabled,
        int interval,
        List<String> targets
) {
    private static final String FILE_NAME = "greeter-main";

    public static final ConfigSchema.Definition<GreetingConfig> DEFINITION = ConfigSchema.defineClient(
            Identifier.fromNamespaceAndPath("greeter", "main"),
            GreetingConfig.class,
            meta -> meta
                    .directory("greeter")
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML),
            schema -> schema.group(
                    ConfigField.string("message")
                            .defaultValue("Hello!")
                            .forGetter(GreetingConfig::message),
                    ConfigField.bool("enabled")
                            .defaultValue(true)
                            .forGetter(GreetingConfig::enabled),
                    ConfigField.intRange("interval", 1, 3600)
                            .defaultValue(60)
                            .forGetter(GreetingConfig::interval),
                    ConfigField.list("targets", Codec.STRING)
                            .defaultValue(List.of("@a"))
                            .forGetter(GreetingConfig::targets)
            ).apply(schema, GreetingConfig::new)
    );
}
```

`defineClient` 声明这是一个客户端配置（服务端用 `defineServer`）。参数依次是唯一标识符、Record 的 Class、元信息配置、字段声明。

每个字段的声明链：**字段名 → 默认值 → getter 引用**。`forGetter(GreetingConfig::message)` 将字段与 Record 的 component 绑定。

## 2.3 获取 ConfigUnit

`ConfigUnit` 是配置的运行时实例，直接挂载为 Record 的静态字段：

```java
public static final ConfigUnit<GreetingConfig> UNIT = DEFINITION.unit();
```

还需要一个注册方法，将配置加入游戏生命周期：

```java
public static void register() {
    ConfigManager.registerClient(UNIT);
}
```

## 2.4 完整示例

```java
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigSchema;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.field.ConfigField;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record GreetingConfig(
        String message,
        boolean enabled,
        int interval,
        List<String> targets
) {
    private static final String FILE_NAME = "greeter-main";

    public static final ConfigSchema.Definition<GreetingConfig> DEFINITION = ConfigSchema.defineClient(
            Identifier.fromNamespaceAndPath("greeter", "main"),
            GreetingConfig.class,
            meta -> meta
                    .directory("greeter")
                    .fileName(FILE_NAME)
                    .format(ConfigStorageFormat.TOML),
            schema -> schema.group(
                    ConfigField.string("message").defaultValue("Hello!").forGetter(GreetingConfig::message),
                    ConfigField.bool("enabled").defaultValue(true).forGetter(GreetingConfig::enabled),
                    ConfigField.intRange("interval", 1, 3600).defaultValue(60).forGetter(GreetingConfig::interval),
                    ConfigField.list("targets", Codec.STRING).defaultValue(List.of("@a")).forGetter(GreetingConfig::targets)
            ).apply(schema, GreetingConfig::new)
    );
    public static final ConfigUnit<GreetingConfig> UNIT = DEFINITION.unit();

    public static void register() {
        ConfigManager.registerClient(UNIT);
    }
}
```

在 Mod 主类中调用注册：

```java
@Mod("greeter")
public class GreeterMod {
    public GreeterMod() {
        GreetingConfig.register();
    }
}
```

之后通过 `GreetingConfig.UNIT.get()` 读取当前配置值：

```java
GreetingConfig config = GreetingConfig.UNIT.get();
String msg = config.message();
int interval = config.interval();
```

## 下一步

本章创建了一个可用的配置文件。下一章深入字段系统，了解每种字段类型的行为和元数据配置。
