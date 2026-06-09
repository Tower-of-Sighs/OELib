# 光学（Optic）

## 引子

在我们平时编写的代码中，往往会出现许多状态不断发生改变的代码，所以难免会出现这样的情况：

```text
刚创建的时候是正确的

传了几个方法之后变了

最后发现不知道是谁改的
```

为了减少这种问题，函数式编程提倡一种思想：

> 如果一个对象创建出来之后根本不能修改，哪还有这么多问题啊。

事实上，Java 这些年也在拥抱不可变性方面做了很多努力：记录类、蜜蜂接口等等。

Record 就是其中最典型的代表。

借助记录类，我们可以使用一行代码定义一个不可变对象：

```java
public record Address(String street, String city, String postcode) {}
```

编译器会自动生成：`equals()` `hashCode()` `toString()`， 同时：类是 final 的，字段是 final 的，对象一旦创建，其状态便无法改变。

这鼓励了一种面向数据（DOP）的编程风格，而这种风格也是函数式程序员长期以来所倡导的。

然而理想很丰满，现实很骨感，Java 的 Record，可谓是地狱啊。

乍一看倒还好，从 Java 16 开始逐步引入的模式匹配功能，使我们能够优雅地解构这些记录类：

```java
if (employee instanceof Employee(var id, var name, Address(var street, _, _))) {
    System.out.println(name + " lives on " + street);
}
```

我们可以深入嵌套结构，提取所需内容，并在单个表达式中将值绑定到变量。结合密封接口，我们还能得到编译器可以验证的详尽 switch 表达式：

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}

 String describe(Shape shape) {
    return switch (shape) {
        case Circle(var r) -> "A circle with radius " + r;
        case Rectangle(var w, var h) -> "A " + w + " by " + h + " rectangle";
        case Triangle(var a, var b, var c) -> "A triangle";
    };
}
```

这可太棒了，我们的 Java 已经成为了一种可靠的面向数据编程语言，其核心就在于不可变性。

有个问题：由上文得知，我们读取嵌套的不可变数据结构很优雅，但我想写入呢？

我们的 Address 也是一个记录类。

我们要给这位员工搬个家，城市不变，邮政编码不变，只换到另一条街道，在可变的世界中非常简单，set 一下就行了。

问题是，我们的记录类不给 set 啊，只能重新 new 一遍这个记录类。

现在看上去还好，new 的不多，如果我要给一个公司的一个部门的一个员工搬家呢，oh no，至少四个 new，我们必须从公司到地址，重建每一条记录类，修改一个字符串少说二十行代码，这就是嵌套拷贝构造(copy constructor cascade)。

这简直太糟了不是吗，你可能会想：用 withFoo 不行吗，Lombok 甚至还为我们提供了 `@With` 注解，没错，你可以这样做。

虽然有所帮助，但并不能解决实际的问题，我们仍需要将更新后的值逐层传递：

```java
var newAddress = manager.address().withStreet("100 New Street");
var newEmployee = manager.withAddress(newAddress);
var newDept = dept.withEmployee(newEmployee);
...
```

某些时候，上级拨了款，一层层传递下去，中间有人不小心把一部分流向了自己的口袋，哦那就糟了。

实际上我们这段代码也是如此，仪式依然存在，样板代码依然存在，而且随着嵌套层级的增加，出错的可能性（例如意外复制错误的字段、忘记更新中间层）也随之增加，只不过是从下到上而已。

Java 在干嘛呢，看戏吗，实际上并不是，前面提到 Java 16 引入了不那么“模式匹配”的“模式匹配”。

模式匹配使我们能够逐层深入，忽略不关心的字段，并精确提取所需内容。它具有声明式、可组合性和优雅性。

但是，Java 中可没有 “模式设置” 的概念，也还是只能读不能写啊。

但但是，Java 难道真的无动于衷吗，来看 [JEP468](https://openjdk.org/jeps/468)，哇哦，它引入了 `with` 关键字，你可以这样写了：

```java
Address updated = oldAddress with { street = "100 New Street"; };
```

那么嵌套记录怎么办呢，好吧，还是不太行，你不能这样写：

```java
employee with { address.street = "100 New Street" } // No! not supported
```

要更新嵌套字段，必须在每个层级使用表达式 with 链式调用：

```java
Employee updated = employee with {
    address = address with { street = "100 New Street"; };
};
```

虽然比之前简洁了不少，但如果嵌套层级越来越深，这种方式也会难以管理。但 JEP468 仍然是一个值得欢迎的补充，但它解决的是语法问题，而非可组合性问题。

面对如此困难的问题，不少人会选择把字段设为非 final，短期来看，没问题，但可变性的问题我们开头已经说了，一个项目可能会有很多雷点： 比如线程安全问题、防御性拷贝，或者更令人不安的量子纠缠中的 “spooky action at a distance（远距离幽灵作用）” ———— 当你以为某个对象归你所有时，它却在某处被变成了别人的形状。

## 光学：孩子们别怕，我来了

难道真的没人能战胜嵌套更新吗，我看未必。在我们 Haskell 中，对此问题有了优雅的解决方法，那就是 **光学**(Optics)，虽然其他语言也有类似的解决方法，比如 Scala 的 Monocle，F# 的属性访问表达式。

Optics 的独特之处在于组合 ：它能够将小型、聚焦的访问器组合成更大的访问器，从而自动处理任意深度。

接下来
