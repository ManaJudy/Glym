package com.mana.glym.orm

import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.LinkedBlockingQueue

class DataSource(val url: String, val username: String, val password: String, val driver: String, val poolSize: Int = 10) {
    val pool = LinkedBlockingQueue<Connection>(poolSize)
    init {
        try {
            Class.forName(driver)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("JDBC Driver not found: $driver", e)
        }
        repeat(poolSize) {
            val conn = DriverManager.getConnection(url, username, password)
            pool.offer(conn)
        }
    }

    @Synchronized
    fun getConnection(): Connection = pool.take()

    @Synchronized
    fun releaseConnection(connection: Connection) {
        if (!connection.isClosed) pool.offer(connection)
    }

    fun closeAll() {
        pool.forEach { it.close() }
        pool.clear()
    }
}
