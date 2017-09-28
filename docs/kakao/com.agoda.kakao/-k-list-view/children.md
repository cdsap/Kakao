---
title: KListView.children - kakao
---

[kakao](../../index.html) / [com.agoda.kakao](../index.html) / [KListView](index.html) / [children](.)

# children

`inline fun <reified T : `[`KAdapterItem`](../-k-adapter-item/index.html)`<*>> children(function: T.() -> `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Performs given actions/assertion on all children in adapter

### Parameters

`T` - Type of all items. Must be registered via constructor.

`function` - Tail lambda which receiver will be matched item with given type T