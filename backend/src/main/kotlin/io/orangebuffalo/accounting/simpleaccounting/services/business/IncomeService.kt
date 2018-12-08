package io.orangebuffalo.accounting.simpleaccounting.services.business

import com.querydsl.core.types.Predicate
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Income
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QIncome
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos.IncomeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class IncomeService(
    private val incomeRepository: IncomeRepository
) {

    suspend fun saveIncome(income: Income): Income {
        val defaultCurrency = income.category.workspace.defaultCurrency
        if (defaultCurrency == income.currency) {
            income.amountInDefaultCurrency = income.originalAmount
            income.reportedAmountInDefaultCurrency = income.originalAmount
        }

        return withDbContext {
            incomeRepository.save(income)
        }
    }

    suspend fun getIncomes(
        workspace: Workspace,
        page: Pageable,
        filter: Predicate
    ): Page<Income> = withDbContext {
        incomeRepository.findAll(QIncome.income.category.workspace.eq(workspace).and(filter), page)
    }

    suspend fun getIncomeByIdAndWorkspace(id: Long, workspace: Workspace): Income? = withDbContext {
        incomeRepository.findByIdAndCategoryWorkspace(id, workspace)
    }
}