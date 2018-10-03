package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtAuthenticationToken
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private const val BEARER = "Bearer"

class JwtTokenAuthenticationConverter : java.util.function.Function<ServerWebExchange, Mono<Authentication>> {

    override fun apply(exchange: ServerWebExchange): Mono<Authentication> {
        return extractBearerToken(exchange)
                .map { JwtAuthenticationToken(it) }
    }

    private fun extractBearerToken(exchange: ServerWebExchange): Mono<String> {
        return Flux.fromIterable(exchange.request.headers.getValuesAsList(HttpHeaders.AUTHORIZATION))
                .filter { it.toLowerCase().startsWith(BEARER.toLowerCase()) }
                .map { it.substring(BEARER.length).trim() }
                .filter { it.isNotEmpty() }
                .next()
    }
}