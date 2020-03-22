package io.orangebuffalo.simpleaccounting.web

import io.orangebuffalo.simpleaccounting.WithSaMockUser
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.simpleaccounting.mockCurrentDate
import io.orangebuffalo.simpleaccounting.mockCurrentTime
import io.orangebuffalo.simpleaccounting.services.business.TimeService
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@DslMarker
annotation class FilteringTestApiDslMarker

inline fun <reified T : Any> generateFilteringApiTests(
    init: FilteringApiTestCasesBuilder<T>.() -> Unit
): Collection<FilteringApiTestCase> {
    val testCasesBuilder = FilteringApiTestCasesBuilderImpl(T::class)
    init(testCasesBuilder)
    return testCasesBuilder.buildTestCases()
}

@FilteringTestApiDslMarker
interface FilteringApiTestCasesBuilder<T : Any> {
    var baseUrl: String

    fun entityMatcher(init: EntityMatcher<T>.() -> Unit)

    fun defaultEntityProvider(init: EntitiesRegistry.() -> T)

    fun filtering(init: FilteringTestCaseBuilder<T>.() -> Unit)

    fun sorting(init: SortingTestCaseBuilder<T>.() -> Unit)
}

@FilteringTestApiDslMarker
interface EntityMatcher<T : Any> {
    fun responseFields(vararg fieldNames: String)
    fun entityFields(vararg fieldExtractor: (entity: T) -> Any?)
}

@FilteringTestApiDslMarker
interface FilteringTestCaseBuilder<T : Any> {
    fun entity(init: FilteringEntityBuilder<T>.() -> Unit)

    @FilteringTestApiDslMarker
    interface FilteringEntityBuilder<T : Any> {
        fun configure(init: EntitiesRegistry.(entity: T) -> Unit)
        fun skippedOn(filter: String)
    }
}

@FilteringTestApiDslMarker
interface SortingTestCaseBuilder<T : Any> {
    fun ascendingBy(apiField: String, init: EntitiesBuilder<T>.() -> Unit)
    fun default(init: EntitiesBuilder<T>.() -> Unit)

    @FilteringTestApiDslMarker
    interface EntitiesBuilder<T : Any> {
        fun goes(init: EntitiesRegistry.(entity: T) -> Unit)
    }
}

@FilteringTestApiDslMarker
interface EntitiesRegistry {
    val workspace: Workspace
    val workspaceOwner: PlatformUser
    val entities: List<Any>
    fun <T : Any> save(entity: T): T
}

abstract class FilteringApiTestCase : TestData {
    abstract fun execute(client: WebTestClient)
}

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@MockitoSettings(strictness = Strictness.LENIENT)
abstract class AbstractFilteringApiTest {

    @Autowired
    lateinit var client: WebTestClient

    @MockBean
    lateinit var timeService: TimeService

    @ParameterizedTest
    @MethodSource("createTestCases")
    @WithSaMockUser(userName = "Fry")
    fun testFilteringApi(testCase: FilteringApiTestCase) {
        mockCurrentDate(timeService)
        mockCurrentTime(timeService)
        testCase.execute(client)
    }
}
