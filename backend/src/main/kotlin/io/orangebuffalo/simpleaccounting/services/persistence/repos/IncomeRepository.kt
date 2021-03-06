package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import java.time.LocalDate

interface IncomeRepository : AbstractEntityRepository<Income>, IncomeRepositoryExt {
    fun findByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income?
}

interface IncomeRepositoryExt {
    fun getStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<IncomesStatistics>

    fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics>
}

data class IncomesStatistics(
    val categoryId: Long?,
    val totalAmount: Long,
    val finalizedCount: Long,
    val pendingCount: Long,

    /**
     * The difference between converted amount and income taxable amount over all incomes of this category
     */
    val currencyExchangeDifference: Long
)
