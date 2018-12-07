package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import com.fasterxml.jackson.annotation.JsonInclude
import com.querydsl.core.types.dsl.Expressions
import io.orangebuffalo.accounting.simpleaccounting.services.business.DocumentService
import io.orangebuffalo.accounting.simpleaccounting.services.business.ExpenseService
import io.orangebuffalo.accounting.simpleaccounting.services.business.TimeService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Expense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.QExpense
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.accounting.simpleaccounting.web.api.EntityNotFoundException
import io.orangebuffalo.accounting.simpleaccounting.web.api.integration.*
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
@RequestMapping("/api/v1/user/workspaces/{workspaceId}/expenses")
class ExpenseApiController(
    private val extensions: ApiControllersExtensions,
    private val expenseService: ExpenseService,
    private val documentService: DocumentService,
    private val timeService: TimeService
) {

    @PostMapping
    fun createExpense(
        @PathVariable workspaceId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): Mono<ExpenseDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        expenseService.saveExpense(
            Expense(
                category = getValidCategory(workspace, request.category),
                title = request.title,
                timeRecorded = timeService.currentTime(),
                datePaid = request.datePaid,
                currency = request.currency,
                originalAmount = request.originalAmount,
                amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0,
                actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency ?: 0,
                notes = request.notes,
                percentOnBusiness = request.percentOnBusiness ?: 100,
                attachments = getValidAttachments(workspace, request.attachments),
                reportedAmountInDefaultCurrency = 0
            )
        ).let(::mapExpenseDto)
    }

    private suspend fun getValidAttachments(
        workspace: Workspace,
        attachmentIds: List<Long>?
    ): List<Document> {
        val attachments = attachmentIds?.let { documentService.getDocumentsByIds(it) } ?: emptyList()
        attachments.forEach { attachment ->
            if (attachment.workspace != workspace) {
                throw EntityNotFoundException("Document ${attachment.id} is not found")
            }
        }
        return attachments
    }

    private fun getValidCategory(
        workspace: Workspace,
        category: Long
    ) = workspace.categories.asSequence()
        .firstOrNull { workspaceCategory -> workspaceCategory.id == category }
        ?: throw EntityNotFoundException("Category $category is not found")

    @GetMapping
    @PageableApi(ExpensePageableApiDescriptor::class)
    fun getExpenses(
        @PathVariable workspaceId: Long,
        pageRequest: ApiPageRequest
    ): Mono<Page<Expense>> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        expenseService.getExpenses(workspace, pageRequest.page, pageRequest.predicate)
    }

    @GetMapping("{expenseId}")
    fun getExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long
    ): Mono<ExpenseDto> = extensions.toMono {
        val workspace = extensions.getAccessibleWorkspace(workspaceId)
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")
        mapExpenseDto(expense)
    }

    @PutMapping("{expenseId}")
    fun updateExpense(
        @PathVariable workspaceId: Long,
        @PathVariable expenseId: Long,
        @RequestBody @Valid request: EditExpenseDto
    ): Mono<ExpenseDto> = extensions.toMono {

        val workspace = extensions.getAccessibleWorkspace(workspaceId)

        // todo optimistic locking. etag?
        val expense = expenseService.getExpenseByIdAndWorkspace(expenseId, workspace)
            ?: throw EntityNotFoundException("Expense $expenseId is not found")

        expense.apply {
            category = getValidCategory(workspace, request.category)
            title = request.title
            datePaid = request.datePaid
            currency = request.currency
            originalAmount = request.originalAmount
            amountInDefaultCurrency = request.amountInDefaultCurrency ?: 0
            actualAmountInDefaultCurrency = request.actualAmountInDefaultCurrency ?: 0
            notes = request.notes
            percentOnBusiness = request.percentOnBusiness ?: 100
            attachments = getValidAttachments(workspace, request.attachments)
        }.let {
            expenseService.saveExpense(it)
        }.let {
            mapExpenseDto(it)
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ExpenseDto(
    val category: Long,
    val title: String,
    val timeRecorded: Instant,
    val datePaid: LocalDate,
    val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long,
    val actualAmountInDefaultCurrency: Long,
    val reportedAmountInDefaultCurrency: Long,
    val attachments: List<Long>,
    val percentOnBusiness: Int,
    val notes: String?,
    val id: Long,
    val version: Int,
    val status: ExpenseStatus?
)

enum class ExpenseStatus {
    FINALIZED,
    PENDING_CONVERSION,
    PENDING_ACTUAL_RATE
}

data class EditExpenseDto(
    val category: Long,
    val datePaid: LocalDate,
    @field:NotBlank val title: String,
    @field:NotBlank val currency: String,
    val originalAmount: Long,
    val amountInDefaultCurrency: Long?,
    val actualAmountInDefaultCurrency: Long?,
    val attachments: List<Long>?,
    val percentOnBusiness: Int?,
    @field:Size(max = 1024) val notes: String?
)

private fun mapExpenseDto(source: Expense) = ExpenseDto(
    category = source.category.id!!,
    title = source.title,
    datePaid = source.datePaid,
    timeRecorded = source.timeRecorded,
    currency = source.currency,
    originalAmount = source.originalAmount,
    amountInDefaultCurrency = source.amountInDefaultCurrency,
    actualAmountInDefaultCurrency = source.actualAmountInDefaultCurrency,
    attachments = source.attachments.map { it.id!! },
    percentOnBusiness = source.percentOnBusiness,
    reportedAmountInDefaultCurrency = source.reportedAmountInDefaultCurrency,
    notes = source.notes,
    id = source.id!!,
    version = source.version,
    status = getExpenseStatus(source)
)

private fun getExpenseStatus(expense: Expense): ExpenseStatus {
    return when {
        expense.reportedAmountInDefaultCurrency > 0 -> ExpenseStatus.FINALIZED
        expense.amountInDefaultCurrency > 0 -> ExpenseStatus.PENDING_ACTUAL_RATE
        else -> ExpenseStatus.PENDING_CONVERSION
    }
}

class ExpensePageableApiDescriptor : PageableApiDescriptor<Expense, QExpense> {
    override fun mapEntityToDto(entity: Expense) = mapExpenseDto(entity)

    override fun getSupportedFilters() = apiFilters(QExpense.expense) {
        byApiField("freeSearchText", String::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                Expressions.anyOf(
                    notes.containsIgnoreCase(value),
                    title.containsIgnoreCase(value),
                    category.name.containsIgnoreCase(value)
                )
            }
        }

        byApiField("status", ExpenseStatus::class) {
            onOperator(PageableApiFilterOperator.EQ) { value ->
                when (value) {
                    ExpenseStatus.FINALIZED -> reportedAmountInDefaultCurrency.gt(0)
                    ExpenseStatus.PENDING_ACTUAL_RATE -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.gt(0))
                    ExpenseStatus.PENDING_CONVERSION -> reportedAmountInDefaultCurrency.eq(0)
                        .and(amountInDefaultCurrency.eq(0))
                }
            }
        }
    }
}