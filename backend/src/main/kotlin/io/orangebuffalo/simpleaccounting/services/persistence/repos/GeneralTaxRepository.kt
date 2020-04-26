package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.GeneralTax
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface GeneralTaxRepository : LegacyAbstractEntityRepository<GeneralTax>, QuerydslPredicateExecutor<GeneralTax> {
    fun findByIdAndWorkspace(id: Long, workspace: Workspace): GeneralTax?
    fun findByIdAndWorkspaceId(id: Long, workspaceId: Long): GeneralTax?
    fun existsByIdAndWorkspaceId(id: Long, workspaceId: Long): Boolean
}
