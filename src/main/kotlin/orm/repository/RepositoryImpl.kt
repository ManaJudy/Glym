package com.mana.glym.orm.repository

import com.mana.glym.orm.EntityManager

class RepositoryImpl<I : Number, E>(
    override val entityManger: EntityManager,
    override val c: Class<E>
) : Repository<I, E> {

    override fun findAll(): List<E> = entityManger.findAll(c)
    override fun findById(id: I): E? = entityManger.findOneById(c, id)
    override fun findLast(): E? = entityManger.findLast(c)
    override fun save(entity: E): E? = entityManger.save(c, entity)
    override fun update(entity: E): E? = entityManger.update(c, entity)
    override fun delete(entity: E) = entityManger.delete(c, entity)

}