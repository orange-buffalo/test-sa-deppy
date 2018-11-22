package io.orangebuffalo.accounting.simpleaccounting.web.api.integration

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.accounting.simpleaccounting.web.api.ApiValidationException
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class ApiPageRequestResolver(
        adapterRegistry: ReactiveAdapterRegistry
) : HandlerMethodArgumentResolverSupport(adapterRegistry) {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return checkParameterTypeNoReactiveWrapper(parameter) { type ->
            type == ApiPageRequest::class.java
        }
    }

    override fun resolveArgument(
            parameter: MethodParameter,
            bindingContext: BindingContext,
            exchange: ServerWebExchange): Mono<Any> {

        val annotation = parameter.annotatedElement.getAnnotation(PageableApi::class.java)
                ?: throw IllegalArgumentException("Missing @PageableApi at ${parameter.method}")

        return validateAndGetParameter("limit", 10, exchange.request.queryParams)
                .map { PageRequest.of(0, it) }
                .flatMap { pageRequest ->
                    validateAndGetParameter("page", 1, exchange.request.queryParams)
                            .map { pageNumber -> PageRequest.of(pageNumber - 1, pageRequest.pageSize) }
                }
                .flatMap { pageRequest ->
                    validateAndGetSort(exchange.request.queryParams)
                            .map { sort -> PageRequest.of(
                                    pageRequest.pageNumber,
                                    pageRequest.pageSize,
                                    sort)
                            }
                }
                .map { page -> ApiPageRequest(page = page, predicate = Expressions.TRUE.isTrue) }
                .fold({ pageRequest -> Mono.just(pageRequest) }, { error -> Mono.error(error) })
    }

    private fun validateAndGetSort(queryParams: MultiValueMap<String, String>): Result<Sort, ApiValidationException> {
        return Result.of {
            Sort.by(Sort.Direction.DESC, "id")
        }
    }

    private fun validateAndGetParameter(
            paramName: String,
            defaultValue: Int,
            queryParams: MultiValueMap<String, String>): Result<Int, ApiValidationException> {

        val param = queryParams[paramName]
        if (param != null && param.size > 1) {
            return Result.error(ApiValidationException("Only a single '$paramName' parameter is supported"))
        }

        return Result.of {
            if (param != null) {
                val paramValue = param[0]
                try {
                    paramValue.toInt()
                } catch (e: NumberFormatException) {
                    throw ApiValidationException("Invalid '$paramName' parameter value '$paramValue'")
                }
            } else {
                defaultValue
            }
        }
    }
}