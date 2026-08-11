# JavaDoc API Documentation Generation Specification (Oracle Style)

## Objective

Generate JavaDoc comments that conform to Oracle's official Javadoc conventions and describe the **API contract**, rather than implementation details or programming tutorials.

The generated documentation must be suitable for:

* Java API specifications
* Compatibility testing
* Alternative API implementations
* Public SDK documentation

Unless explicitly requested, omit `@author` and `@since`.

---

# Primary Rules (Highest Priority)

Always follow these rules before considering any stylistic preference.

## 1. Document the API Contract

Describe:

* observable behavior
* parameter constraints
* return value semantics
* boundary conditions
* exceptional conditions
* implementation-independent guarantees

Do **not** describe:

* implementation details
* algorithms
* internal data structures
* optimization strategies
* usage tutorials
* conceptual introductions
* common programming knowledge

The documentation should enable another engineer to implement the same API correctly.

---

## 2. Remain Implementation Independent

Documentation describes behavior shared by all conforming implementations.

If implementation-specific behavior must be documented, use a separate paragraph beginning with one of the following:

```text
On Windows systems,
```

or

```text
Implementation-Specific:
```

Never mix implementation notes into the primary API specification.

---

## 3. Use Oracle Writing Style

Method summaries must begin with a third-person verb phrase.

Preferred:

* Gets...
* Returns...
* Sets...
* Creates...
* Removes...
* Registers...
* Determines...

Avoid imperative voice.

Incorrect:

```
Get the value...
```

Correct:

```
Gets the value...
```

---

# Documentation Structure

Always generate comments using the following structure.

```java
/**
 * Summary sentence.
 *
 * <p>Additional description.
 *
 * @param ...
 * @return ...
 * @throws ...
 * @see ...
 */
```

Rules:

* The first sentence must be complete and concise.
* The first sentence becomes the summary table entry.
* Separate additional paragraphs using `<p>`.
* Do not insert blank HTML elements.
* Preserve standard Javadoc formatting.

---

# Tag Rules

Generate tags in the following order.

| Order | Tag                   |
| ----- | --------------------- |
| 1     | @version (types only) |
| 2     | @param                |
| 3     | @return               |
| 4     | @throws               |
| 5     | @see                  |
| 6     | @since                |
| 7     | @serial               |
| 8     | @deprecated           |

Unless requested:

* omit `@author`
* omit `@since`

---

# Tag Requirements

## @param

Generate one tag for every parameter.

Requirements:

* preserve declaration order
* describe meaning rather than repeating the name
* mention constraints when relevant

Example:

```
@param index the position of the element to retrieve
```

---

## @return

Required for every non-void method.

Describe:

* returned value
* special return cases
* nullability when part of the contract

Never use for constructors.

---

## @throws

Document:

* checked exceptions
* unchecked exceptions callers are expected to handle

Do not document:

* NullPointerException
* implementation-specific runtime exceptions

Use the most general contractual exception type whenever possible.

Prefer:

```
IndexOutOfBoundsException
```

instead of

```
ArrayIndexOutOfBoundsException
```

Sort multiple exceptions alphabetically.

---

## @deprecated

Always include a replacement.

Preferred:

```java
@deprecated Use {@link #newMethod()} instead.
```

If no replacement exists:

```java
@deprecated No replacement.
```

---

# Formatting Rules

Use `{@code ...}` (preferred) or `<code>...</code>` for:

* keywords
* package names
* class names
* interface names
* method names
* field names
* parameter names
* code fragments

Example:

```java
{@code List}
```

---

# Inline Links

Use `{@link}` only when navigation benefits the reader.

Rules:

* link only the first occurrence
* avoid unnecessary links to common `java.lang` classes
* do not over-link

---

# Writing Guidelines

Provide information beyond the API name.

Poor:

```java
Sets the value.
```

Better:

```java
Registers the value used during subsequent validation operations.
```

Avoid merely restating the method name.

---

Avoid Latin abbreviations.

Replace:

* i.e.
* e.g.
* aka

With:

* that is
* for example
* also known as

---

# Special Cases

## Package Documentation

Use:

```
package.html
```

Include:

1. Package summary
2. Package specification
3. Related documentation (optional)

---

## Anonymous Inner Classes

Do not generate JavaDoc.

Document the enclosing declaration instead.

---

## Default Constructors

Public and protected default constructors should be explicitly documented.

---

# Output Requirements

When generating JavaDoc:

1. Follow every rule in this specification.
2. Produce Oracle-style API documentation.
3. Generate complete and accurate comments.
4. Preserve valid Javadoc syntax.
5. Return **only** the JavaDoc comment block.
6. Do **not** include Markdown, explanations, or conversational text outside the generated comment.