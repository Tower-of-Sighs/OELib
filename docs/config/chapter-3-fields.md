# Chapter 3: 字段（Field）与配置元数据

第 2 章创建了一个配置文件。本章介绍 `ConfigField` 提供的全部字段类型和元数据系统。

## 3.1 字段类型总览

`ConfigField` 提供了以下工厂方法：

| 工厂 | 值类型 | 默认 UI |
|------|--------|---------|
| `intRange(key, min, max)` | `int` | slider |
| `doubleRange(key, min, max)` | `double` | slider |
| `bool(key)` | `boolean` | checkbox |
| `string(key)` | `String` | text box |
| `enumValue(key, class)` | `Enum` | dropdown |
| `list(key, elementCodec)` | `List<T>` | collapsible list |
| `map(key, keyCodec, valueCodec)` | `Map<K,V>` | collapsible map |
| `optional(key, elementCodec)` | `Optional<T>` | text box 或空 |
| `dynamic(key)` | `Dynamic<?>` | text box |

每个字段的声明链都以 `forGetter(getter)` 结尾：

```java
ConfigField.string("message")
        .defaultValue("Hello!")
        .forGetter(MyConfig::message);
```

`forGetter` 将字段与 Record 的 component 绑定，同时完成元数据注册。

## 3.2 标量字段

### intRange

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .forGetter(MyConfig::port);
```

取值范围由第二个和第三个参数定义。UI 默认显示为滑块。`.text()` 可切换为文本框：

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .text()
        .forGetter(MyConfig::port);
```

### doubleRange

```java
ConfigField.doubleRange("threshold", 0.0, 1.0)
        .defaultValue(0.5)
        .forGetter(MyConfig::threshold);
```

UI 滑块步长默认 0.01。

### bool

```java
ConfigField.bool("enabled")
        .defaultValue(true)
        .forGetter(MyConfig::enabled);
```

UI 渲染为复选框。

### string

```java
ConfigField.string("name")
        .defaultValue("default")
        .forGetter(MyConfig::name);
```

### enumValue

```java
public enum Mode { STABLE, BETA, EXPERIMENTAL }

ConfigField.enumValue("mode", Mode.class)
        .defaultValue(Mode.STABLE)
        .forGetter(MyConfig::mode);
```

以枚举常量的 `name()` 字符串序列化，UI 渲染为下拉框。

## 3.3 容器字段

### list

```java
ConfigField.list("whitelist", Codec.STRING)
        .defaultValue(List.of())
        .forGetter(MyConfig::whitelist);
```

UI 渲染为可折叠列表，支持增删行和拖拽排序。

### map

```java
ConfigField.map("limits", Codec.STRING, Codec.INT)
        .defaultValue(Map.of())
        .forGetter(MyConfig::limits);
```

UI 渲染为可折叠键值对列表。

### optional

```java
ConfigField.optional("greeting", Codec.STRING)
        .forGetter(MyConfig::greeting);
```

默认值为 `Optional.empty()`。文件中无值时不写该键，有值时才写入。

## 3.4 元数据

字段声明链中的可选方法用于填充字段元数据。

### comment

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .comment("The port the server listens on")
        .forGetter(MyConfig::port);
```

comment 写入配置文件的对应字段上方，纯文档用途。

### tooltip

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .tooltip()
        .forGetter(MyConfig::port);
```

开启 UI 中的悬浮提示，翻译键自动生成为 `config.<namespace>.<path>.<fieldKey>.tooltip`。

### validate

```java
ConfigField.intRange("port", 1024, 65535)
        .defaultValue(25565)
        .validate((port, entireConfig) ->
                port != null && port > 0
                        ? Optional.empty()
                        : Optional.of("port must be positive"))
        .forGetter(MyConfig::port);
```

返回 `Optional.empty()` 表示校验通过，`Optional.of(error)` 表示失败。校验在加载、保存、每次修改时自动执行。

### hiddenInUi

```java
ConfigField.bool("internalFlag")
        .defaultValue(false)
        .hiddenInUi()
        .forGetter(MyConfig::internalFlag);
```

该字段不在配置屏幕中显示，但仍参与序列化和校验。

## 3.5 Dynamic 字段

`ConfigField.dynamic(key)` 使用 `Codec.PASSTHROUGH`，接受任何可序列化结构：

```java
ConfigField.dynamic("metadata")
        .forGetter(MyConfig::metadata);
```

适用于结构在编译期未知的配置字段，如插件加载的数据或用户自定义规则块。

## 3.6 自定义字段

继承 `BaseFieldBuilder` 创建自定义字段类型：

```java
import cc.sighs.oelib.config.field.BaseFieldBuilder;
import com.mojang.serialization.Codec;

public record RgbColor(int red, int green, int blue) {}

public class ColorFieldBuilder extends BaseFieldBuilder<RgbColor, ColorFieldBuilder> {
    public ColorFieldBuilder(String key) {
        super(key, Codec.STRING.xmap(s -> {/* parse */}, c -> {/* format */}));
    }
}

// 使用
new ColorFieldBuilder("accentColor")
        .defaultValue(new RgbColor(255, 0, 0))
        .forGetter(MyConfig::accentColor);
```

继承 `BaseFieldBuilder<T, B>` 后自动获得 `comment`、`tooltip`、`defaultValue`、`validate`、`hiddenInUi` 等链式方法。

## 3.7 字段与 forGetter

`forGetter` 是声明链的终点。在此之前 builder 只是累加元数据；`forGetter` 被调用时同时完成：

1. 将字段绑定到 Record 的 getter（供序列化使用）
2. 将元数据注册到配置系统（供 UI、校验、迁移使用）
3. 确定字段在配置文件中的 key

因此字段的信息只在一处声明。翻译键自动派生、校验链式附加、序列化路径和 UI 编辑路径使用同一个 key。

## 下一步

本章覆盖了所有字段类型和元数据。字段声明是配置的静态层面——下一章聚焦运行时层面：ConfigUnit 的加载、缓存、持久化和事件生命周期。
