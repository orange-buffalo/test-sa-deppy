package io.orangebuffalo.simpleaccounting.support

import io.orangebuffalo.simpleaccounting.services.persistence.entities.I18nSettings
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.persistence.EntityManager
import javax.transaction.Transactional

/**
 * Generates data for CI Tests during application startup.
 */
@Component
@Profile("ci-tests")
class CiTestsProfileData(private val entityManager: EntityManager) {

    @EventListener
    @Transactional
    fun createCiTestsData(event: ContextRefreshedEvent) {
       entityManager.persist(PlatformUser(
           userName = "Fry",
           passwordHash = "{noop}password",
           isAdmin = false,
           i18nSettings = I18nSettings(locale = "en_AU", language = "en")
       ))
    }
}
