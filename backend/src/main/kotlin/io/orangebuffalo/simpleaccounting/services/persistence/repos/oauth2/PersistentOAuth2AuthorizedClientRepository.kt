package io.orangebuffalo.simpleaccounting.services.persistence.repos.oauth2

import io.orangebuffalo.simpleaccounting.services.persistence.entities.oauth2.PersistentOAuth2AuthorizedClient
import io.orangebuffalo.simpleaccounting.services.persistence.repos.AbstractEntityRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
interface PersistentOAuth2AuthorizedClientRepository :
    AbstractEntityRepository<PersistentOAuth2AuthorizedClient> {

    fun deleteByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String)

    fun findByClientRegistrationIdAndUserName(clientRegistrationId: String, userName: String):
            PersistentOAuth2AuthorizedClient?

}