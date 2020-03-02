package io.orangebuffalo.simpleaccounting.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.orangebuffalo.simpleaccounting.web.api.authentication.JwtTokenAuthenticationConverter
import io.orangebuffalo.simpleaccounting.web.api.integration.ApiPageRequestResolver
import io.orangebuffalo.simpleaccounting.web.api.integration.PageableApiDescriptorResolver
import io.orangebuffalo.simpleaccounting.web.ui.SpaWebFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ReactiveAdapterRegistry
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Configuration
@EnableWebFluxSecurity
class WebConfig(
    @Autowired(required = false) private val adapterRegistry: ReactiveAdapterRegistry = ReactiveAdapterRegistry()
) : WebFluxConfigurer {

    @field:Autowired
    private lateinit var pageableApiDescriptorResolver: PageableApiDescriptorResolver

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        configurer.addCustomResolver(ApiPageRequestResolver(adapterRegistry, pageableApiDescriptorResolver))
    }

    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val objectMapper = Jackson2ObjectMapperBuilder()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .featuresToEnable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build<ObjectMapper>()

        configurer.defaultCodecs()
            .jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))

        configurer.defaultCodecs()
            .jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:META-INF/resources/assets/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))

        registry.addResourceHandler("/*favicon.ico")
            .addResourceLocations("classpath:META-INF/resources/favicon.ico")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))

        registry.addResourceHandler("/*index.html")
            .addResourceLocations("classpath:META-INF/resources/index.html")
            .setCacheControl(CacheControl.noCache())
    }

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        @Qualifier("jwtTokenAuthenticationFilter") jwtTokenAuthenticationFilter: AuthenticationWebFilter
    ): SecurityWebFilterChain {

        return http
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange()
            .pathMatchers("/api/auth/**").permitAll()
            .pathMatchers("/api/users/**").hasRole("ADMIN")
            .pathMatchers("/api/**").authenticated()
            .pathMatchers("/**").permitAll()
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(bearerAuthenticationEntryPoint())
            .and()
            .addFilterAt(jwtTokenAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .csrf().disable()
            .httpBasic().disable()
            .formLogin().disable()
            .logout().disable()
            .build()
    }

    private fun bearerAuthenticationEntryPoint() = ServerAuthenticationEntryPoint { exchange, _ ->
        Mono.fromRunnable {
            val response = exchange.response
            response.statusCode = HttpStatus.UNAUTHORIZED
            response.headers.set("WWW-Authenticate", "Bearer")
        }
    }

    @Bean
    fun jwtTokenAuthenticationConverter(): JwtTokenAuthenticationConverter {
        return JwtTokenAuthenticationConverter()
    }

    @Bean
    fun jwtTokenAuthenticationFilter(
        authenticationManager: ReactiveAuthenticationManager,
        jwtTokenAuthenticationConverter: JwtTokenAuthenticationConverter
    ): AuthenticationWebFilter {

        return AuthenticationWebFilter(authenticationManager).apply {
            setRequiresAuthenticationMatcher(
                AndServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/api/**"),
                    NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers("/api/auth/**"))
                )
            )
            setServerAuthenticationConverter(jwtTokenAuthenticationConverter)
            setAuthenticationFailureHandler(
                ServerAuthenticationEntryPointFailureHandler(bearerAuthenticationEntryPoint())
            )
        }
    }

    @Bean
    fun spaWebFilter() = SpaWebFilter()

}