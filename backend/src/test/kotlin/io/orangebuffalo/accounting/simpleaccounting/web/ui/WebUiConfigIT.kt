package io.orangebuffalo.accounting.simpleaccounting.web.ui

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
internal class WebUiConfigIT(
    @Autowired val client: WebTestClient
) {

    @Test
    fun `Should serve static resources from root without authentication`() {
        client.get().uri("/favicon.ico")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isNotEmpty()
            }
    }

    @Test
    fun `Should serve css without authentication`() {
        client.get().uri("/static/css/some.css")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Should serve js without authentication`() {
        client.get().uri("/static/js/some.js")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Should serve fonts without authentication`() {
        client.get().uri("/static/fonts/some.ttf")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Should serve images without authentication`() {
        client.get().uri("/static/img/some.png")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `Should serve app page without authentication`() {
        client.get().uri("/")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }

    @Test
    fun `Should serve routed app page without authentication`() {
        client.get().uri("/workspaces")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }

    @Test
    fun `Should serve routed app page with nested path without authentication`() {
        client.get().uri("/workspaces/42/edit")
            .accept(TEXT_HTML)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith {
                assertThat(it.responseBody).isNotBlank()
            }
    }
}