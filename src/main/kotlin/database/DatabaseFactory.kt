package com.example.database

import com.example.database.tables.Albumes
import com.example.database.tables.Artistas
import com.example.database.tables.Tracks
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("database.driverClassName").getString()
        val jdbcURL = config.property("database.jdbcURL").getString()
        val username = config.property("database.username").getString()
        val password = config.property("database.password").getString()
        val maxPoolSize = config.property("database.maxPoolSize").getString().toInt()

        val hikariConfig = HikariConfig().apply {
            this.driverClassName = driverClassName
            this.jdbcUrl = jdbcURL
            this.username = username
            this.password = password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        // NO crear las tablas automáticamente porque ya existen en PostgreSQL
        // Si necesitas crearlas desde Exposed (solo para testing):
        // transaction {
        //     SchemaUtils.create(Artistas, Albumes, Tracks)
        // }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}