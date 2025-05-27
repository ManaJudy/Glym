package com.mana.glym.orm.repository

import com.mana.glym.orm.EntityManager
import java.lang.reflect.Proxy

@Suppress("unchecked_cast")
object RepositoryFactory {
    fun<I : Number, E : Any, R : Repository<I, E>> create (
        entityManager: EntityManager,
        repositoryInterface: Class<R>,
        entityClass: Class<E>
    ): R {
        val target = RepositoryImpl<I, E>(entityManager, entityClass)
        return Proxy.newProxyInstance(
            repositoryInterface.classLoader,
            arrayOf(repositoryInterface)
        ) { _, method, args ->
            val realMethod = target::class.java.methods.find { it.name == method.name && it.parameterCount == (args?.size ?: 0) }
                ?: throw UnsupportedOperationException("Méthode ${method.name} non supportée")
            realMethod.invoke(target, *(args ?: emptyArray()))
        } as R
    }
}