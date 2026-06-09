# Chapter 6: 可选值与子类型

第 5 章的 `update` 假设目标字段一定存在且类型确定。但配置字段的值可能为空、可能是多个子类型之一。本章介绍条件更新。

## 6.1 Optional 字段

字段声明为 `Optional<T>` 时，值可能存在也可能不存在。

```java
record ServerConfig(
        String name,
        Optional<Integer> maxPlayers
) {}
```

用 `ifPresent` 只在有值时更新：

```java
ServerConfig.UNIT.ifPresent(ServerConfig::maxPlayers, v -> v + 1);
```

`maxPlayers` 当前为 `Optional.of(5)` → 更新为 `Optional.of(6)`。当前为 `Optional.empty()` → 跳过，值不变。

### 有值才变换的行为

```java
ServerConfig withValue = new ServerConfig("test", Optional.of(5));
ServerConfig empty = new ServerConfig("test", Optional.empty());

ServerConfig.UNIT.ifPresent(ServerConfig::maxPlayers, v -> v + 1);
// withValue  → maxPlayers = Optional.of(6)
// empty      → maxPlayers = Optional.empty()（不受影响）
```

## 6.2 密封类型（Sealed Interface）

当字段的类型是一个密封接口时，运行时值可能是多个子类型之一。

```java
sealed interface Mode permits ModeA, ModeB {}
record ModeA(int value) implements Mode {}
record ModeB(String label) implements Mode {}

record ServerConfig(Mode mode, String name) {}
```

用 `whenSubtype` 仅在特定子类型时更新：

```java
ServerConfig.UNIT.whenSubtype(ServerConfig::mode, ModeA.class,
        m -> new ModeA(m.value() + 1));
```

`mode` 当前是 `ModeA(5)` → 更新为 `ModeA(6)`. 当前是 `ModeB("x")` → 跳过，值不变。

`whenSubtype` 的匹配原理：运行时检查 `subtypeClass.isInstance(value)`，匹配则应用变换，不匹配则原样返回。

## 6.3 编译期未知的结构

当字段的结构在编译期完全未知时（如插件加载的数据、用户自定义规则块），用 `ConfigField.dynamic(key)` 声明，值类型为 `Dynamic<?>`（Mojang 序列化体系中的自描述值）：

```java
record PluginConfig(
        String name,
        Dynamic<?> pluginData
) {}

ConfigField.dynamic("pluginData").forGetter(PluginConfig::pluginData);
```

`Dynamic` 字段使用 `Codec.PASSTHROUGH`，接受任何可序列化的值，在序列化时原样透传。

通过 `DynamicOps` 提取值：

```java
Dynamic<?> data = PluginConfig.UNIT.get().pluginData();
int value = data.asInt(0);
```
**如果你不知道什么是 `Dynamic` ，那么不建议你使用此类型。**

## 6.4 条件更新 vs 直接更新

| 场景 | 方法 |
|------|------|
| 字段确定存在 | `UNIT.update(getter, modifier)` |
| Optional 字段 | `UNIT.ifPresent(getter, modifier)` |
| 密封类型匹配 | `UNIT.whenSubtype(getter, class, modifier)` |

条件更新的内部实现基于 `ConfigAffine`——一个可空条件访问器（比 Prism 弱，只有 `preview` + `set`，没有从焦点逆向构造整体的能力，因此称为 Affine）。日常使用中通过 `ifPresent` 和 `whenSubtype` 调用，不需要直接使用 Affine。

## 下一步

标量字段和可选字段的处理都覆盖了。第 7 章转向集合字段：List 的全部元素转大写、Map 的值统一加倍、条件筛选和计数。
