package com.mana.glym.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(
    val name: String = "",
)
