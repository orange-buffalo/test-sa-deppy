package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.domain.documents.DocumentsService
import io.orangebuffalo.simpleaccounting.domain.invoices.InvoicesService
import io.orangebuffalo.simpleaccounting.services.integration.EntityNotFoundException
import io.orangebuffalo.simpleaccounting.services.integration.executeInParallel
import io.orangebuffalo.simpleaccounting.services.integration.withDbContext
import io.orangebuffalo.simpleaccounting.services.persistence.entities.*
import io.orangebuffalo.simpleaccounting.services.persistence.repos.CurrenciesUsageStatistics
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomeRepository
import io.orangebuffalo.simpleaccounting.services.persistence.repos.IncomesStatistics
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class IncomeService(
    private val incomeRepository: IncomeRepository,
    private val workspaceService: WorkspaceService,
    private val generalTaxService: GeneralTaxService,
    private val categoryService: CategoryService,
    private val documentsService: DocumentsService,
    private val invoicesService: InvoicesService
) {
    suspend fun saveIncome(income: Income): Income {
        val workspace = workspaceService.getAccessibleWorkspace(income.workspaceId, WorkspaceAccessMode.READ_WRITE)
        validateCategoryAndAttachments(income)
        updateInvoiceIfLinked(income)

        val defaultCurrency = workspace.defaultCurrency
        if (defaultCurrency == income.currency) {
            income.convertedAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.incomeTaxableAmounts = AmountsInDefaultCurrency(income.originalAmount, null)
            income.useDifferentExchangeRateForIncomeTaxPurposes = false
        }

        if (!income.useDifferentExchangeRateForIncomeTaxPurposes) {
            income.incomeTaxableAmounts = income.convertedAmounts
        }

        val generalTax = getGeneralTax(income)
        income.generalTaxRateInBps = generalTax?.rateInBps

        val convertedAdjustedAmounts = calculateAdjustedAmount(income.convertedAmounts, generalTax)
        income.convertedAmounts.adjustedAmountInDefaultCurrency = convertedAdjustedAmounts.adjustedAmount

        val incomeTaxableAdjustedAmounts = calculateAdjustedAmount(income.incomeTaxableAmounts, generalTax)
        income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency = incomeTaxableAdjustedAmounts.adjustedAmount
        income.generalTaxAmount = incomeTaxableAdjustedAmounts.generalTaxAmount

        income.status = when {
            income.convertedAmounts.adjustedAmountInDefaultCurrency == null -> IncomeStatus.PENDING_CONVERSION
            income.incomeTaxableAmounts.adjustedAmountInDefaultCurrency == null ->
                IncomeStatus.PENDING_CONVERSION_FOR_TAXATION_PURPOSES
            else -> IncomeStatus.FINALIZED
        }

        return withDbContext { incomeRepository.save(income) }
    }

    private suspend fun updateInvoiceIfLinked(income: Income) {
        val invoiceId = income.linkedInvoiceId ?: return

        val invoice = invoicesService.getInvoiceByIdAndWorkspaceId(id = invoiceId, workspaceId = income.workspaceId)
            ?: throw EntityNotFoundException("Invoice $invoiceId is not found")

        invoice.datePaid = income.dateReceived
        invoicesService.saveInvoice(invoice, income.workspaceId)
    }

    private suspend fun getGeneralTax(income: Income): GeneralTax? =
        if (income.generalTaxId == null) null else generalTaxService.getValidGeneralTax(
            income.generalTaxId!!,
            income.workspaceId
        )

    private suspend fun validateCategoryAndAttachments(income: Income) = executeInParallel {
        step { validateIncomeCategory(income) }
        step { validateIncomeAttachments(income) }
    }

    private suspend fun validateIncomeAttachments(income: Income) {
        if (income.attachments.isNotEmpty()) {
            val attachmentsIds = income.attachments.map { it.documentId }
            documentsService.validateDocuments(income.workspaceId, attachmentsIds)
        }
    }

    private suspend fun validateIncomeCategory(income: Income) {
        if (income.categoryId != null) categoryService.validateCategory(income.categoryId!!, income.workspaceId)
    }

    private fun calculateAdjustedAmount(
        targetAmounts: AmountsInDefaultCurrency,
        generalTax: GeneralTax?
    ): AdjustedAmounts {
        val originalAmountInDefaultCurrency = targetAmounts.originalAmountInDefaultCurrency
            ?: return AdjustedAmounts(null, null)

        if (generalTax == null) {
            return AdjustedAmounts(
                generalTaxAmount = null,
                adjustedAmount = originalAmountInDefaultCurrency
            )
        }

        val baseAmountForAddedGeneralTax = originalAmountInDefaultCurrency.bpsBasePart(generalTax.rateInBps)
        return AdjustedAmounts(
            generalTaxAmount = originalAmountInDefaultCurrency.minus(baseAmountForAddedGeneralTax),
            adjustedAmount = baseAmountForAddedGeneralTax
        )
    }

    suspend fun getIncomeByIdAndWorkspace(incomeId: Long, workspace: Workspace): Income? =
        getIncomeByIdAndWorkspaceId(incomeId, workspace.id!!)

    suspend fun getIncomeByIdAndWorkspaceId(incomeId: Long, workspaceId: Long): Income? =
        withDbContext {
            incomeRepository.findByIdAndWorkspaceId(incomeId, workspaceId)
        }

    suspend fun getIncomesStatistics(
        fromDate: LocalDate,
        toDate: LocalDate,
        workspaceId: Long
    ): List<IncomesStatistics> = withDbContext {
        incomeRepository.getStatistics(fromDate, toDate, workspaceId)
    }

    suspend fun getCurrenciesUsageStatistics(workspace: Workspace): List<CurrenciesUsageStatistics> = withDbContext {
        incomeRepository.getCurrenciesUsageStatistics(workspace)
    }

    private data class AdjustedAmounts(
        val generalTaxAmount: Long?,
        val adjustedAmount: Long?
    )
}
