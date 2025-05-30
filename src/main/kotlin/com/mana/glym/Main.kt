package com.mana.glym

import com.mana.glym.orm.proxy.ProxyFactory
import com.mana.glym.orm.testEntity.User

fun main() {
    val test = User()
    test.id = 1
    test.name ="test"
    test.email ="test@test.com"

    val user = ProxyFactory.createProxy(User::class.java) {return@createProxy test }
    println(user.javaClass.name)
    println(user)
}