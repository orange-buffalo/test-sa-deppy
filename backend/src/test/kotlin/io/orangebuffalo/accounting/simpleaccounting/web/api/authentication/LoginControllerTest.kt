package io.orangebuffalo.accounting.simpleaccounting.web.api.authentication

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.whenever
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.security.jwt.JwtService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager

private const val LOGIN_PATH = "/api/v1/auth/login"

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("When requesting $LOGIN_PATH, ")
@AutoConfigureWebTestClient
class LoginControllerTest {

    @MockBean
    lateinit var passwordEncoder: PasswordEncoder

    @MockBean
    lateinit var jwtService: JwtService

    @Autowired
    lateinit var client: WebTestClient

    @Test
    fun `should return a JWT token for valid user login credentials`() {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Fry"
                    && password == "qwertyHash"
                    && authorities.size == 1
                    && authorities.iterator().next().authority == "ROLE_USER"
        })) doReturn "jwtTokenForFry"

        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Fry",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFry")
    }

    @Test
    fun `should return a JWT token for valid admin login credentials`() {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn true

        whenever(jwtService.buildJwtToken(argThat {
            username == "Farnsworth"
                    && password == "qwertyHash"
                    && authorities.size == 1
                    && authorities.iterator().next().authority == "ROLE_ADMIN"
        })) doReturn "jwtTokenForFarnsworth"

        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Farnsworth",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isEqualTo("jwtTokenForFarnsworth")
    }

    @Test
    fun `should return 403 when user is unknown`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Roberto",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isForbidden
                .expectBody().isEmpty
    }

    @Test
    fun `should return 403 when password does not match`() {
        whenever(passwordEncoder.matches("qwerty", "qwertyHash")) doReturn false

        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Fry",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isForbidden
                .expectBody().isEmpty
    }

    @Test
    fun `should return 400 when request body is empty`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).containsIgnoringCase("request body is missing")
                }
    }

    @Test
    fun `should return 400 on invalid request body`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody("{}")
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).containsIgnoringCase("value failed for JSON property userName")
                }
    }

    @Test
    fun `should return 400 on non-json request body`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody("hello")
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).containsIgnoringCase("JSON decoding error")
                }
    }

    @Test
    fun `should return 400 when username is missing in a request`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "",
                        password = "qwerty"
                ))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody)
                            .containsIgnoringCase("username")
                            .containsIgnoringCase("must not be blank")
                }
    }

    @Test
    fun `Should return 400 when password is missing in a login request`() {
        client.post().uri(LOGIN_PATH)
                .contentType(APPLICATION_JSON)
                .syncBody(LoginRequest(
                        userName = "Fry",
                        password = ""
                ))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody)
                            .containsIgnoringCase("password")
                            .containsIgnoringCase("must not be blank")
                }
    }

    @TestConfiguration
    class Config {
        @Bean
        fun testSetupRunner(
                transactionTemplate: TransactionTemplate,
                entityManager: EntityManager): ApplicationRunner = ApplicationRunner { _ ->

            transactionTemplate.execute {
                entityManager.persist(PlatformUser(
                        userName = "Fry",
                        passwordHash = "qwertyHash",
                        isAdmin = false
                ))

                entityManager.persist(PlatformUser(
                        userName = "Farnsworth",
                        passwordHash = "qwertyHash",
                        isAdmin = true
                ))
            }
        }
    }
}