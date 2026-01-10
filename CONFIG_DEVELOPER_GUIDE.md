# OELib 配置系统使用指南

这份指南帮助你快速上手 OELib 的配置：如何声明、读取、修改、显示配置界面，以及做一些进阶的扩展与迁移。

## 快速开始

### 写一个配置

下面用“游戏设置”为例。它包含常用的几种字段类型，能覆盖大多数场景。

```java
public record GameSettingsConfig(
        boolean enabled,
        int maxCount,
        double speed,
        String title,
        Mode mode,
        List<String> tags,
        Map<String, Integer> options,
        Optional<String> extra,
        Dynamic<?> rules
) {
    public enum Mode { SIMPLE, ADVANCED }

    public static final ConfigUnit<GameSettingsConfig> UNIT_CLIENT = ConfigRecordCodecBuilder.createClient(
            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "game_settings"),
            instance -> instance.group(
                    ConfigField.bool("enabled").defaultValue(true).comment("是否启用").forGetter(GameSettingsConfig::enabled),
                    ConfigField.intRange("maxCount", 1, 100).defaultValue(10).comment("数量上限").forGetter(GameSettingsConfig::maxCount),
                    ConfigField.doubleRange("speed", 0.1, 10.0).defaultValue(1.0).comment("速度倍率").forGetter(GameSettingsConfig::speed),
                    ConfigField.string("title").defaultValue("My Game").comment("标题文本").forGetter(GameSettingsConfig::title),
                    ConfigField.enumValue("mode", Mode.class).defaultValue(Mode.SIMPLE).comment("模式选择").forGetter(GameSettingsConfig::mode),
                    ConfigField.list("tags", Codec.STRING).defaultValue(List.of("tag1", "tag2")).comment("标签列表").forGetter(GameSettingsConfig::tags),
                    ConfigField.map("options", Codec.STRING, Codec.INT).defaultValue(Map.of("optA", 1)).comment("键值选项").forGetter(GameSettingsConfig::options),
                    ConfigField.optional("extra", Codec.STRING).comment("可选附加项").forGetter(GameSettingsConfig::extra),
                    ConfigField.dynamic("rules").comment("自由结构规则").forGetter(GameSettingsConfig::rules)
            ).apply(instance, GameSettingsConfig::new),
            meta -> meta.directory(OELib.MODID).fileName("game_settings").format(ConfigStorageFormat.TOML)
    );
    
    public static final ConfigUnit<GameSettingsConfig> UNIT_SERVER = ConfigRecordCodecBuilder.createServer(
            // .....
    );

    public static void register() {
        ConfigManager.registerClient(UNIT_CLIENT);
        ConfigManager.registerServer(UNIT_SERVER, player -> player.hasPermissions(4)); // 4 级权限才可在客户端修改服务端配置
    }
}
```

### 读取与保存

```java
var cfg = GameSettingsConfig.UNIT.get();
boolean enabled = cfg.enabled();
GameSettingsConfig.UNIT.save();
```

## 单项读写（更改某一个字段）

如果只想改某一项，可以在配置类里加一个“访问器”，直接对单个字段赋值并保存。

```java
public static final ConfigAccess<GameSettingsConfig> ACCESS = new ConfigAccess<>(UNIT);

// 改一个开关
GameSettingsConfig.ACCESS.set(GameSettingsConfig::enabled, false);

// 改一个字符串
GameSettingsConfig.ACCESS.set(GameSettingsConfig::title, "New Title");

// 改一个 Map
var opts = new java.util.HashMap<>(GameSettingsConfig.UNIT.get().options());
opts.put("optB", 2);
GameSettingsConfig.ACCESS.set(GameSettingsConfig::options, opts);
```

## 配置屏幕

- Fabric
  - 参考: [OELibModMenu.java](file:///f:/code/mcmod/project/OELib-mod/fabric/src/main/java/cc/sighs/oelib/fabric/modmenu/OELibModMenu.java)
- NeoForge
  - 参考: [OELibNeoForgeClient.java](file:///f:/code/mcmod/project/OELib-mod/neoforge/src/main/java/cc/sighs/oelib/neoforge/OELibNeoForgeClient.java)

## 事件监听

参考入口：
- Fabric: [FabricConfigEvent.java](file:///f:/code/mcmod/project/OELib-mod/fabric/src/main/java/cc/sighs/oelib/fabric/config/event/FabricConfigEvent.java)
- NeoForge: [NeoForgeConfigEvent.java](file:///f:/code/mcmod/project/OELib-mod/fabric/src/main/java/cc/sighs/oelib/neoforge/config/event/NeoForgeConfigEvent.java)

## 字段类型速览（ConfigField）

上面的示例已经涵盖所有常用字段，下面是快速清单：
- bool("key")：布尔
- intRange("key", min, max)：整数范围
- doubleRange("key", min, max)：小数范围
- string("key")：字符串
- enumValue("key", YourEnum.class)：枚举
- list("key", elementCodec)：列表
- map("key", keyCodec, valueCodec)：映射
- optional("key", elementCodec)：可选值
- dynamic("key")：保留自由结构（JSON/TOML），适合不固定的高级配置

## 自定义字段（FieldBuilder）

如果现有字段不够用，可以自己扩展一个。它的作用是：
- 定义数据格式（Codec）
- 设置默认值与注释
- 给 UI 一个“展示提示”（滑条、文本、下拉等）
- 统一校验与生成界面行为

## 配置迁移（DataFix）

当你改了键名、结构或默认值，老玩家的配置文件可能不匹配。为此提供数据修复（迁移）机制：
- rename(fromPath, toPath)：把旧键改名为新键
- setIfMissing(path, value)：如果缺少这个键，就补一个默认值

注册一个迁移链：

```java
import cc.sighs.oelib.config.datafix.ConfigFixRegistry;
import cc.sighs.oelib.config.datafix.ConfigFixRegistry.FixContext;
import net.minecraft.resources.ResourceLocation;

ConfigFixRegistry.register(
        ResourceLocation.fromNamespaceAndPath("oelib", "game_settings"),
        1, // 当前版本号
        builder -> builder
                .fix(0, 1, dyn -> new FixContext(dyn)
                        .rename("titleOld", "title")
                        .setIfMissing("maxCount", FixContext.json("10"))
                )
);
```

迁移会在注册/加载时自动执行，写回新格式。文件里有一个 `__cfg_version` 字段，用来判断是否需要升级。