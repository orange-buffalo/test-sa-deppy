package io.orangebuffalo.simpleaccounting.web.api.integration

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import com.querydsl.core.types.dsl.PathBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.core.MethodParameter
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.lang.reflect.AnnotatedElement
import kotlin.reflect.full.createInstance

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ApiPageRequestResolver")
internal class ApiPageRequestResolverTest {

    @Mock
    private lateinit var bindingContext: BindingContext

    @Mock
    private lateinit var pageableApiDescriptorResolver: PageableApiDescriptorResolver

    @Mock
    private lateinit var exchange: ServerWebExchange

    private lateinit var apiPageRequestResolver: ApiPageRequestResolver

    private lateinit var queryParams: MultiValueMap<String, String>

    @BeforeEach
    fun setup() {
        apiPageRequestResolver = ApiPageRequestResolver(ReactiveAdapterRegistry(), pageableApiDescriptorResolver)

        whenever(pageableApiDescriptorResolver.resolveDescriptor(any())) doAnswer { invocationOnMock ->
            val annotatedElement = invocationOnMock.arguments[0] as AnnotatedElement
            val pageableApi = annotatedElement.getAnnotation(PageableApi::class.java)
            pageableApi.descriptorClass.createInstance()
        }

        queryParams = LinkedMultiValueMap<String, String>().apply {
            val request = Mockito.mock(ServerHttpRequest::class.java)
            whenever(request.queryParams) doReturn this
            whenever(exchange.request) doReturn request
        }
    }

    @Test
    fun `should not support non-page-request parameters`() {
        assertFalse(apiPageRequestResolver.supportsParameter(getFirstMethodParameter("inapplicableMethod")))
    }

    @Test
    fun `should support page-request parameters`() {
        assertTrue(apiPageRequestResolver.supportsParameter(getFirstMethodParameter("apiPageMethodDefault")))
    }

    @Test
    fun `should return default page config if no parameters are specified in the request`() {
        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodDefault")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(0)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(10)
    }

    @Test
    fun `should return and error when multiple limit parameters supplied`() {
        queryParams.apply {
            add("limit", "20")
            add("limit", "10")
        }

        invokeResolveArgumentAndAssertValidationError(
            "Only a single 'limit' parameter is supported",
            "apiPageMethodDefault"
        )
    }

    @Test
    fun `should use provided limit to populate page size`() {
        queryParams.add("limit", "20")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodDefault")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(0)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(20)
    }

    @Test
    fun `should return and error when limit is not a valid int`() {
        queryParams.add("limit", "20$")

        invokeResolveArgumentAndAssertValidationError("Invalid 'limit' parameter value '20$'", "apiPageMethodDefault")
    }

    @Test
    fun `should return and error when multiple page parameters supplied`() {
        queryParams.apply {
            add("page", "1")
            add("page", "2")
        }

        invokeResolveArgumentAndAssertValidationError(
            "Only a single 'page' parameter is supported",
            "apiPageMethodDefault"
        )
    }

    @Test
    fun `should use provided page to populate page number`() {
        queryParams.add("page", "7")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodDefault")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.pageNumber).isEqualTo(6)
        assertThat(resolvedPageRequest.page.pageSize).isEqualTo(10)
    }

    @Test
    fun `should return and error when page is not a valid int`() {
        queryParams.add("page", "o_O")

        invokeResolveArgumentAndAssertValidationError("Invalid 'page' parameter value 'o_O'", "apiPageMethodDefault")
    }

    @Test
    fun `should set the default sorting to 'by ID' if default is not provided`() {
        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodDefault")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.sort).isNotNull
        assertThat(resolvedPageRequest.page.sort)
            .hasSize(1)
            .allSatisfy {
                assertThat(it.property).isEqualTo("id")
                assertThat(it.isDescending).isTrue()
            }
    }

    @Test
    fun `should fail on multiple sortBy parameters`() {
        queryParams.add("sortBy", "apiField desc")
        queryParams.add("sortBy", "apiField2 desc")

        invokeResolveArgumentAndAssertValidationError(
            "Only a single 'sortBy' parameter is supported",
            "apiPageMethodSortable"
        )
    }

    @Test
    fun `should fail on invalid sortBy if direction is not provided`() {
        queryParams.add("sortBy", "apiField")

        invokeResolveArgumentAndAssertValidationError(
            "'apiField' is not a valid sorting expression",
            "apiPageMethodSortable"
        )
    }

    @Test
    fun `should fail on invalid sortBy if extra data is provided`() {
        queryParams.add("sortBy", "apiField desc else")

        invokeResolveArgumentAndAssertValidationError(
            "'apiField desc else' is not a valid sorting expression",
            "apiPageMethodSortable"
        )
    }

    @Test
    fun `should fail on invalid sortBy if direction is not supported`() {
        queryParams.add("sortBy", "apiField greaterFirst")

        invokeResolveArgumentAndAssertValidationError(
            "'greaterFirst' is not a valid sorting direction",
            "apiPageMethodSortable"
        )
    }

    @Test
    fun `should fail on invalid sortBy if api field is not known`() {
        queryParams.add("sortBy", "someField desc")

        invokeResolveArgumentAndAssertValidationError(
            "Sorting by 'someField' is not supported",
            "apiPageMethodSortable"
        )
    }

    @Test
    fun `should set desc sorting by query parameter`() {
        queryParams.add("sortBy", "apiField desc")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodSortable")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.sort).isNotNull
        assertThat(resolvedPageRequest.page.sort)
            .hasSize(1)
            .allSatisfy {
                assertThat(it.property).isEqualTo("entityField")
                assertThat(it.isDescending).isTrue()
            }
    }

    @Test
    fun `should set asc sorting by query parameter`() {
        queryParams.add("sortBy", "apiField asc")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodSortable")

        assertThat(resolvedPageRequest.page).isNotNull
        assertThat(resolvedPageRequest.page.sort).isNotNull
        assertThat(resolvedPageRequest.page.sort)
            .hasSize(1)
            .allSatisfy {
                assertThat(it.property).isEqualTo("entityField")
                assertThat(it.isAscending).isTrue()
            }
    }

    @Test
    fun `should return always true predicate if descriptor does not support filtering`() {
        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodDefault")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("true = true")
    }

    @Test
    fun `should return always true predicate if queried filter is not supported`() {
        queryParams.add("apiFieldUnknown[eq]", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("true = true")
    }

    @Test
    fun `should return a predicate as created by filter if queried filter is supported`() {
        queryParams.add("apiField[eq]", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("testEntity.entityField = 42")
    }

    @Test
    fun `should fail if filter query is not valid`() {
        queryParams.add("apiField[eq", "42")

        invokeResolveArgumentAndAssertValidationError(
            "'apiField[eq' is not a valid filter expression",
            "apiPageMethodExtended"
        )
    }

    @Test
    fun `should return always true predicate if queried filter is not valid but field name is not known`() {
        queryParams.add("apiFieldUnknown[eq", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("true = true")
    }

    @Test
    fun `should fail if filter query operation is unknown`() {
        queryParams.add("apiField[op]", "42")

        invokeResolveArgumentAndAssertValidationError(
            "'op' is not a valid filter operator",
            "apiPageMethodExtended"
        )
    }

    @Test
    fun `should fail if filter operator is not supported`() {
        queryParams.add("anotherApiField[eq]", "test")

        invokeResolveArgumentAndAssertValidationError(
            "'eq' is not supported for 'anotherApiField'",
            "apiPageMethodExtended"
        )
    }

    @Test
    fun `should fail if cannot convert the input value`() {
        queryParams.add("apiField[eq]", "abc")

        invokeResolveArgumentAndAssertValidationError(
            "'abc' is not a valid filter value",
            "apiPageMethodExtended"
        )
    }

    @Test
    fun `should support 'eq' operator`() {
        queryParams.add("apiField[eq]", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("testEntity.entityField = 42")
    }

    @Test
    fun `should support 'goe' operator`() {
        queryParams.add("apiField[goe]", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("testEntity.entityField >= 42")
    }

    @Test
    fun `should support 'loe' operator`() {
        queryParams.add("apiField[loe]", "42")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull.hasToString("testEntity.entityField <= 42")
    }

    @Test
    fun `should support multiple filters`() {
        queryParams.add("apiField[loe]", "100")
        queryParams.add("apiField[goe]", "42")
        queryParams.add("anotherApiField[goe]", "abc")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull
            .hasToString(
                "testEntity.entityField <= 100 " +
                        "&& testEntity.entityField >= 42 " +
                        "&& testEntity.anotherEntityField = abc"
            )
    }

    @Test
    fun `should support operators with multiple values`() {
        queryParams.add("apiField[eq]", "42")
        queryParams.add("apiField[eq]", "44")
        queryParams.add("apiField[goe]", "20")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull
            .hasToString("testEntity.entityField = 42 && testEntity.entityField = 44 && testEntity.entityField >= 20")
    }

    @Test
    fun `should support enum values`() {
        queryParams.add("enumApiFiled[eq]", "ONE")

        val resolvedPageRequest = invokeResolveArgumentAndGetPageRequest("apiPageMethodExtended")

        assertThat(resolvedPageRequest.predicate).isNotNull
            .hasToString("testEntity.enumEntityField = ONE")
    }

    @Test
    fun `should fail if provided value for enum field is not valid`() {
        queryParams.add("enumApiFiled[eq]", "TWO")

        invokeResolveArgumentAndAssertValidationError(
            "'TWO' is not a valid filter value",
            "apiPageMethodExtended"
        )
    }

    private fun invokeResolveArgumentAndAssertValidationError(expectedMessage: String, methodName: String) {
        val resolvedArgument = apiPageRequestResolver.resolveArgument(
            getFirstMethodParameter(methodName),
            bindingContext,
            exchange
        )

        assertThat(resolvedArgument).isNotNull

        val actualException = assertThrows<ApiValidationException> {
            resolvedArgument.block()
        }
        assertThat(actualException.message).isEqualTo(expectedMessage)
    }

    private fun invokeResolveArgumentAndGetPageRequest(methodName: String): ApiPageRequest {
        val resolvedArgument = apiPageRequestResolver.resolveArgument(
            getFirstMethodParameter(methodName),
            bindingContext,
            exchange
        )

        assertThat(resolvedArgument).isNotNull

        val resolvedMonoValue = resolvedArgument.block()
        assertThat(resolvedMonoValue).isNotNull
        assertThat(resolvedMonoValue).isInstanceOf(ApiPageRequest::class.java)

        return resolvedMonoValue as ApiPageRequest
    }

    private fun getFirstMethodParameter(methodName: String): MethodParameter {
        return MethodParameter.forExecutable(
            ApiPageRequestResolverTestController::class.java.declaredMethods.first { it.name == methodName }, 0
        )
    }

    private class ApiPageRequestResolverTestController {

        @GetMapping
        fun apiPageMethodWithoutAnnotation(@Suppress("UNUSED_PARAMETER") request: ApiPageRequest): Mono<Any> {
            return Mono.empty()
        }

        @GetMapping
        fun inapplicableMethod(@Suppress("UNUSED_PARAMETER") body: String): Mono<Any> {
            return Mono.empty()
        }

        @GetMapping
        @PageableApi(ApiPageRequestResolverTestPageableApiDescriptorDefault::class)
        fun apiPageMethodDefault(@Suppress("UNUSED_PARAMETER") request: ApiPageRequest): Mono<ApiPageRequestResolverTestRepositoryEntity> {
            return Mono.empty()
        }

        @GetMapping
        @PageableApi(ApiPageRequestResolverTestPageableApiDescriptorExtended::class)
        fun apiPageMethodExtended(@Suppress("UNUSED_PARAMETER") request: ApiPageRequest): Mono<ApiPageRequestResolverTestRepositoryEntity> {
            return Mono.empty()
        }

        @GetMapping
        @PageableApi(ApiPageRequestResolverTestPageableApiDescriptorSortable::class)
        fun apiPageMethodSortable(@Suppress("UNUSED_PARAMETER") request: ApiPageRequest): Mono<ApiPageRequestResolverTestRepositoryEntity> {
            return Mono.empty()
        }
    }

    class ApiPageRequestResolverTestApiDto

    class ApiPageRequestResolverTestRepositoryEntity

    class ApiPageRequestResolverTestPageableApiDescriptorDefault :
        PageableApiDescriptor<ApiPageRequestResolverTestRepositoryEntity, PathBuilder<ApiPageRequestResolverTestRepositoryEntity>> {

        override suspend fun mapEntityToDto(entity: ApiPageRequestResolverTestRepositoryEntity) =
            ApiPageRequestResolverTestApiDto()
    }

    enum class ApiPageRequestResolverTestEnum {
        @Suppress("unused")
        ONE
    }

    class ApiPageRequestResolverTestPageableApiDescriptorExtended :
        PageableApiDescriptor<ApiPageRequestResolverTestRepositoryEntity, PathBuilder<ApiPageRequestResolverTestRepositoryEntity>> {

        private val qApiPageRequestResolverTestRepositoryEntity =
            PathBuilder(ApiPageRequestResolverTestRepositoryEntity::class.java, "testEntity")

        override suspend fun mapEntityToDto(entity: ApiPageRequestResolverTestRepositoryEntity) =
            ApiPageRequestResolverTestApiDto()

        override fun getSupportedFilters() = apiFilters(qApiPageRequestResolverTestRepositoryEntity) {
            mapApiFieldToEntityPath("apiField", java.lang.Long::class) {
                getNumber(
                    "entityField",
                    java.lang.Long::class.java
                )
            }

            byApiField("anotherApiField", String::class) {
                onOperator(PageableApiFilterOperator.GOE) { value -> getString("anotherEntityField").eq(value) }
            }

            byApiField("enumApiFiled", ApiPageRequestResolverTestEnum::class) {
                onOperator(PageableApiFilterOperator.EQ) { value ->
                    getEnum("enumEntityField", ApiPageRequestResolverTestEnum::class.java).eq(value)
                }
            }
        }
    }

    class ApiPageRequestResolverTestPageableApiDescriptorSortable :
        PageableApiDescriptor<ApiPageRequestResolverTestRepositoryEntity, PathBuilder<ApiPageRequestResolverTestRepositoryEntity>> {

        override suspend fun mapEntityToDto(entity: ApiPageRequestResolverTestRepositoryEntity) =
            ApiPageRequestResolverTestApiDto()

        override fun getSupportedSorting() = mapOf("apiField" to "entityField")
    }
}
