package io.orangebuffalo.simpleaccounting.services.persistence

import org.jooq.Configuration
import org.jooq.Result
import org.jooq.ResultQuery
import org.springframework.dao.DataAccessException
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.data.jdbc.core.convert.EntityRowMapper
import org.springframework.data.jdbc.core.convert.JdbcConverter
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity
import kotlin.reflect.KClass

private val jdbcConverterKey: KClass<JdbcConverter> = JdbcConverter::class
private val jdbcMappingContextKey: KClass<JdbcMappingContext> = JdbcMappingContext::class

fun Configuration.set(jdbcConverter: JdbcConverter): Configuration {
    data(jdbcConverterKey, jdbcConverter)
    return this
}

fun Configuration.set(jdbcMappingContext: JdbcMappingContext): Configuration {
    data(jdbcMappingContextKey, jdbcMappingContext)
    return this
}

fun <T : Any> Result<*>.asListOf(targetEntityType: KClass<T>): List<T> {
    val jdbcConverter = this.configuration().data(jdbcConverterKey) as JdbcConverter
    val jdbcMappingContext = this.configuration().data(jdbcMappingContextKey) as JdbcMappingContext
    val persistentEntity = jdbcMappingContext.getPersistentEntity(targetEntityType.java)
        ?: throw IllegalStateException("$targetEntityType is not a known entity type")

    @Suppress("UNCHECKED_CAST")
    val mapper = EntityRowMapper<T>(persistentEntity as RelationalPersistentEntity<T>, jdbcConverter)

    val resultSet = this.intoResultSet()
    var rowNumber = 1
    val result = mutableListOf<T>()
    while (resultSet.next()) {
        result.add(mapper.mapRow(resultSet, rowNumber++))
    }

    return result
}

inline fun <reified T : Any> Result<*>.asListOf(): List<T> = asListOf(T::class)

inline fun <reified T : Any> ResultQuery<*>.fetchListOf(): List<T> = fetch().asListOf()

fun <T : Any> ResultQuery<*>.fetchListOf(targetEntityType: KClass<T>): List<T> = fetch().asListOf(targetEntityType)

inline fun <reified T : Any> ResultQuery<*>.fetchOneOrNull(): T? = fetchOneOrNull(T::class)

fun <T : Any> ResultQuery<*>.fetchOneOrNull(targetEntityType: KClass<T>): T? {
    val results = fetch().asListOf(targetEntityType)
    return when {
        results.size > 1 -> throw IncorrectResultSizeDataAccessException("Fetched more that one results", results.size)
        results.isEmpty() -> null
        else -> results[0]
    }
}

