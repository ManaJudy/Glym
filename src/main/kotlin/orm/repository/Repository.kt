package com.mana.glym.orm.repository

import com.mana.glym.orm.EntityManager


interface Repository<I : Number, E> {
    val entityManger: EntityManager
    val c: Class<E>

    fun findAll(): List<E>
    fun findById(id: I): E?
    fun findLast(): E?
    fun save(entity: E): E?
    fun update(entity: E): E?
    fun delete(entity: E)
}