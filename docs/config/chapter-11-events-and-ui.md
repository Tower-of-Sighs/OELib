# Chapter 11: 事件与自定义 UI 组件

前面章节覆盖了配置的定义、生命周期、修改和迁移。本章介绍两个扩展点：事件系统（监听配置变更）和自定义 UI 控件（替换默认的配置编辑控件）。

## 11.1 配置事件

OELib Config 在配置生命周期的关键节点通过 `EventBus` 发布事件。

### 事件类型

| 事件 | 触发时机 |
|------|----------|
| `ConfigLoadEvent` | 从磁盘加载完成后 |
| `ConfigSaveEvent.Pre` | 写入磁盘之前 |
| `ConfigSaveEvent.Post` | 写入磁盘之后 |
| `ConfigChangedEvent` | 内存值被替换后 |
| `ConfigSyncEvent` | 服务端同步到客户端后 |

### 订阅

事件监听通过 `@Subscribe` 注解编写。将监听器放在指定包下，通过 `EventAutoRegistration` 扫描注册：

```java
import cc.sighs.oelib.config.api.ConfigChangedEvent;
import cc.sighs.oelib.event.EventAutoRegistration;
import cc.sighs.oelib.event.Subscribe;

public class ConfigLogger {
    @Subscribe
    public void onChanged(ConfigChangedEvent<?> event) {
        System.out.println("Config " + event.unit().id() + " changed");
    }
}

// 在 Mod 初始化时：
EventAutoRegistration.registerBasePackage("mymod.event");
```

`EventBus` 的完整使用说明（手动注册、优先级、侧过滤等）见[事件系统文档](../EVENT.md)。

### 应用场景

- **缓存同步**：配置变更后刷新依赖该配置的缓存
- **日志审计**：记录每次配置变更的旧值和新值
- **跨 Mod 联动**：监听其他 Mod 的配置变更事件

## 11.2 自定义 UI 组件

配置屏幕为每个字段自动选择 UI 控件：intRange 显示滑块、bool 显示复选框、enumValue 显示下拉框。这些默认控件通过 `ConfigUiHint` 绑定。如果默认控件不够用，可以通过 `ConfigWidgetRegistry` 注册自定义控件。

### 注册

```java
import cc.sighs.oelib.config.ui.ConfigWidgetRegistry;
import cc.sighs.oelib.config.ui.ConfigUiHint;
import cc.sighs.oelib.config.ui.ConfigWidgetRegistry.CustomWidgetHandle;
import cc.sighs.oelib.config.ui.ConfigWidgetRegistry.CustomWidgetContext;
import net.minecraft.resources.Identifier;

var hint = ConfigUiHint.custom(
    Identifier.fromNamespaceAndPath("mymod", "color_picker")
);

ConfigWidgetRegistry.register(hint.widgetId(), context -> {
    return new CustomWidgetHandle() {
        @Override
        public AbstractWidget widget() {
            return new EditBox(font, context.x(), context.y(),
                context.width(), context.height(), text);
        }
    };
});
```

### 与字段关联

自定义字段构建器中设置 uiHint：

```java
public class ColorFieldBuilder extends BaseFieldBuilder<Integer, ColorFieldBuilder> {
    public ColorFieldBuilder(String key) {
        super(key, Codec.INT);
        metaBuilder.uiHint(ConfigUiHint.custom(
            Identifier.fromNamespaceAndPath("mymod", "color_picker")
        ));
    }
}
```

### CustomWidgetContext

| 方法 | 说明 |
|------|------|
| `screen()` | 父级 ConfigScreen |
| `meta()` | 字段元数据 |
| `working()` | 正在编辑的 JSON 对象 |
| `currentValue()` | 当前值 |
| `x()`, `y()` | 控件位置 |
| `width()`, `height()` | 控件尺寸 |
| `onValueChanged()` | 值变更时调用 |

### 类型回退注册

按字段的 Java 类型注册回退控件：

```java
ConfigWidgetRegistry.register(RgbColor.class, context -> {
    return new CustomWidgetHandle() { ... };
});
```

当字段没有显式 `uiHint` 且类型匹配时自动使用。

### 通用自定义

不注册自定义控件类型，直接通过 Dynamic 字段配合 JSON 编辑也是一种选择——虽然 UI 退化为纯文本框，但不需要任何扩展注册。
