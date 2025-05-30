package com.mana.glym.orm

import com.mana.glym.annotation.Id
import com.mana.glym.annotation.Relation
import com.mana.glym.annotation.RelationType
import com.mana.glym.annotation.Entity
import com.mana.glym.orm.proxy.ProxyFactory
import java.lang.reflect.Field
import java.sql.Connection
import java.sql.SQLException
import java.sql.ResultSet

@Suppress("unused", "unchecked_cast", "SqlNoDataSourceInspection", "SqlSourceToSinkFlow")
class EntityManager(val dataSource: DataSource) {

    fun <T> useConnection(block: (Connection) -> T): T {
        val connection = dataSource.getConnection()
        try {
            return block(connection)
        } finally {
            dataSource.releaseConnection(connection)
        }
    }

    fun<E> map(rs: ResultSet, c: Class<E>): E {
        val entity: E = c.declaredConstructors[0].newInstance() as E
        for(field in c.declaredFields) {
            field.setAccessible(true)
            if (field.isAnnotationPresent(Relation::class.java)) {
                val annotation = field.getAnnotation(Relation::class.java)
                val columnValue = rs.getObject(annotation.column)
                val proxy = when (annotation.type) {
                    RelationType.OneToOne, RelationType.ManyToOne -> ProxyFactory.createProxy(field.type) {
                        findOneBy(field.type, mapOf(annotation.joinColumn to columnValue))
                    }
                    RelationType.OneToMany, RelationType.ManyToMany -> ProxyFactory.createProxyList {
                        findBy(field.type, mapOf(annotation.joinColumn to columnValue))
                    }
                }
                field.set(entity, field.type.cast(proxy))
            }

            else field.set(entity, field.type.cast(rs.getObject(field.name)))
        }
        return entity
    }

    fun tableOf(c: Class<*>): String {
        return if (c.isAnnotationPresent(Entity::class.java))
            c.getAnnotation(Entity::class.java).name else c.simpleName
    }

    fun primaryKeyOf(c: Class<*>): Field? {
        return c.declaredFields.firstOrNull { it.isAnnotationPresent(Id::class.java) }
    }

    fun columnsOf(c: Class<*>): List<Field> {
        return c.declaredFields.filter { !it.isAnnotationPresent(Id::class.java) }
    }

    fun<E> findAll(c: Class<E>): List<E> = useConnection { connection ->
        val entities = mutableListOf<E>()
        val sql = "select * from ${tableOf(c)}"
        try {
            connection.prepareStatement(sql).use { stmt -> stmt.executeQuery().use { rs -> while (rs.next()) entities.add(map(rs, c))}}
            return@useConnection entities
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<I : Number, E> findOneById(c: Class<E>, id: I): E? = useConnection { connection ->
    val sql = "select * from ${tableOf(c)} where ${primaryKeyOf(c)!!.name} = ?"
        try {
            connection.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.executeQuery().use { rs -> return@useConnection if (rs.next()) map(rs, c) else null }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<E> findLast(c: Class<E>): E? = useConnection { connection ->
        val sql = "select * from ${tableOf(c)} order by ${primaryKeyOf(c)!!.name} desc limit 1"
        try {
            connection.prepareStatement(sql).use { stmt -> stmt.executeQuery().use { rs -> return@useConnection if (rs.next()) map(rs, c) else null }}
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<E> findOneBy(c: Class<E>, filters: Map<String, Any?>): E? = useConnection { connection ->
        val conditions = filters.keys.joinToString(" and ") { "$it = ?" }
        val sql = "select * from ${tableOf(c)} where $conditions limit 1"
        try {
            connection.prepareStatement(sql).use { stmt ->
                filters.values.forEachIndexed { i, value -> stmt.setObject(i + 1, value) }
                stmt.executeQuery().use { rs -> return@useConnection if (rs.next()) map(rs, c) else null }
            }
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun <E> findBy(c: Class<E>, filters: Map<String, Any?>): List<E> = useConnection { connection ->
        val entities = mutableListOf<E>()
        val conditions = filters.keys.joinToString(" and ") { "$it = ?" }
        val sql = "select * from ${tableOf(c)} where $conditions"
        try {
            connection.prepareStatement(sql).use { stmt ->
                filters.values.forEachIndexed { i, value -> stmt.setObject(i + 1, value) }
                stmt.executeQuery().use { rs -> while (rs.next()) entities.add(map(rs, c)) }
            }
            return@useConnection entities
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<E> save(c: Class<E>, entity: E): E? = useConnection { connection ->
        val columns = columnsOf(c)
        val sb = StringBuilder("insert into ${tableOf(c)} (")
            .append(columns.joinToString(",") { it.name })
            .append(") values (")
            .append(columns.joinToString(",") { "?" })
            .append(")")
        try {
            connection.prepareStatement(sb.toString()).use { stmt ->
                columns.forEachIndexed { i, column -> stmt.setObject(i + 1, column.get(entity)) }
                stmt.executeUpdate()
            }
            return@useConnection findLast(c)
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<E> update(c: Class<E>, entity: E): E? = useConnection { connection ->
        val columns = columnsOf(c)
        val primaryKey = primaryKeyOf(c)
        val sb = StringBuilder("update ${tableOf(c)} set ")
            .append(columns.joinToString(",") { "${it.name} = ?" })
            .append(" where ${primaryKey!!.name} = ?")
        try {
            connection.prepareStatement(sb.toString()).use { stmt ->
                columns.forEachIndexed { i, column -> stmt.setObject(i + 1, column.get(entity)) }
                stmt.setObject(columns.size + 1, primaryKey.get(entity))
                stmt.executeUpdate()
            }
            return@useConnection findOneById(c, primaryKey.get(entity) as Number)
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }

    fun<E> delete(c: Class<E>, entity: E): Unit = useConnection { connection ->
        val primaryKey = primaryKeyOf(c)
        val sql = "delete from ${tableOf(c)} where ${primaryKey!!.name} = ?"
        try {
            connection.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, primaryKey.get(entity))
                stmt.executeUpdate()
            }
        } catch (e: SQLException) {
            throw RuntimeException("Error SQL", e)
        }
    }
}
