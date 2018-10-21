package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.CategoryRepository
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.WorkspaceRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val categoryRepository: CategoryRepository
) {

    fun getWorkspace(workspaceId: Long): Mono<Workspace> {
        return Mono.fromSupplier { workspaceRepository.findById(workspaceId) }
            .filter { it.isPresent }
            .map { it.get() }
            .subscribeOn(Schedulers.elastic())
    }

    fun createCategory(category: Category): Mono<Category> {
        return Mono.fromSupplier { categoryRepository.save(category) }
            .subscribeOn(Schedulers.elastic())
    }
}