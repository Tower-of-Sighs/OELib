# Chapter 1: 为什么 OELib Config 存在

OELib Config 面向喜欢函数式写法、偏向写出类型安全配置的 Minecraft Mod 开发者。

## 1.1 设计思路

传统 Mod 配置库定义配置类的方式是从字段开始的：

```java
public class MyConfig {
    private int port = 25565;
}
```

OELib Config 的思路是**先有 Codec，后有配置**。Codec 同时承载编码和解码的能力，从 Codec 出发，系统自动推导出配置的默认值、序列化、校验规则、版本迁移策略、UI 控件类型和翻译键。

你只描述一次数据结构，其余全部自动完成。

## 1.2 核心优势

### 类型安全的字段访问

字段通过 Record 的 getter 方法引用索引。编译器会验证路径和类型——如果字段被重命名或类型不匹配，直接编译失败，不会等到运行时才发现。

### 零模板代码的持久化

加载时机、缓存策略、写回时机、变更事件派发——整套生命周期由 `ConfigUnit` 自动管理。

### 即 Schema 的字段声明

字段的声明链（字段名 → 默认值 → getter 引用）同时服务于序列化、UI 编辑路径、翻译键和校验路径。同一个名字，同一个来源。

### 三层分离

```
字段声明层   ConfigField          定义类型、默认值、校验、元数据
运行时管理层  ConfigUnit           管理加载、缓存、持久化、事件
修改操作层    update / ifPresent    类型安全的字段修改，自动校验和持久化
```

## 1.3 最终效果展示

```java
import java.lang.invoke.MethodHandles;

record ServerConfig(int port, List<String> whitelist) {}

var def = ConfigSchema.defineServer(MethodHandles.lookup(), id, ServerConfig.class,
    meta -> meta.fileName("server"),
    schema -> schema.group(
        ConfigField.intRange("port", 1024, 65535)
            .defaultValue(25565)
            .forGetter(ServerConfig::port),
        ConfigField.list("whitelist", Codec.STRING)
            .defaultValue(List.of())
            .forGetter(ServerConfig::whitelist)
    ).apply(schema, ServerConfig::new)
);

ConfigUnit<ServerConfig> unit = def.unit();
```

从这段代码出发，系统自动完成：配置文件生成、默认值填充、范围校验、翻译键生成。

## 1.4 文档结构

- [第 2 章](chapter-2-first-config.md)：创建第一个配置文件
- [第 3 章](chapter-3-fields.md)：字段（Field）与配置元数据
- [第 4 章](chapter-4-config-unit.md)：ConfigUnit：配置的运行时入口
- [第 5 章](chapter-5-update.md)：修改配置值
- [第 6 章](chapter-6-conditional.md)：可选值与子类型
- [第 7 章](chapter-7-collections.md)：操作集合字段
- [第 8 章](chapter-8-mutation.md)：批量更新与 ConfigMutation
- [第 9 章](chapter-9-migration.md)：配置文件版本迁移
- [第 10 章](chapter-10-optics.md)：Optics 架构揭秘
- [第 11 章](chapter-11-events-and-ui.md)：事件与自定义 UI 组件
