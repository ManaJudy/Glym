package com.mana.glym.annotation

enum class RelationType {
    OneToOne, OneToMany, ManyToOne, ManyToMany
}

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Relation(
    val column: String,
    val joinColumn: String,
    val type: RelationType
)
