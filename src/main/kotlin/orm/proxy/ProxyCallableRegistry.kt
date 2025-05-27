package com.mana.glym.orm.proxy

import java.util.concurrent.Callable

object ProxyCallableRegistry {
    val callables: MutableMap<String, Callable<Any>> = mutableMapOf()

    fun register(id: String, callable: Callable<Any>) {
        callables[id] = callable
    }

    fun get(id: String): Callable<Any>? = callables[id]
}