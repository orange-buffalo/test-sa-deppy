package io.orangebuffalo.accounting.simpleaccounting.web.api.app

import io.orangebuffalo.accounting.simpleaccounting.services.business.TaxReportingService
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceAccessMode
import io.orangebuffalo.accounting.simpleaccounting.services.business.WorkspaceService
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.FinalizedTaxSummaryItem
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.PendingTaxSummaryItem
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reporting/")
class ReportingApiController(
    private val taxReportingService: TaxReportingService,
    private val workspaceService: WorkspaceService
) {

    @GetMapping("taxes")
    suspend fun getTaxReport(
        @PathVariable workspaceId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate
    ): TaxReportDto {
        val workspace = workspaceService.getAccessibleWorkspace(workspaceId, WorkspaceAccessMode.READ_ONLY)
        val report = taxReportingService.getTaxReport(fromDate, toDate, workspace)
        return TaxReportDto(
            finalizedCollectedTaxes = report.finalizedCollectedTaxes.map(::convertFinalizedTaxItem),
            finalizedPaidTaxes = report.finalizedPaidTaxes.map(::convertFinalizedTaxItem),
            pendingPaidTaxes = report.pendingPaidTaxes.map(::convertPendingTaxItem),
            pendingCollectedTaxes = report.pendingCollectedTaxes.map(::convertPendingTaxItem)
        )
    }

    private fun convertPendingTaxItem(item: PendingTaxSummaryItem) = PendingTaxSummaryItemDto(
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber
    )

    private fun convertFinalizedTaxItem(item: FinalizedTaxSummaryItem) = FinalizedTaxSummaryItemDto(
        taxAmount = item.taxAmount,
        tax = item.tax,
        includedItemsNumber = item.includedItemsNumber,
        includedItemsAmount = item.includedItemsAmount
    )
}

data class TaxReportDto(
    var finalizedCollectedTaxes: List<FinalizedTaxSummaryItemDto>,
    var finalizedPaidTaxes: List<FinalizedTaxSummaryItemDto>,
    var pendingCollectedTaxes: List<PendingTaxSummaryItemDto>,
    var pendingPaidTaxes: List<PendingTaxSummaryItemDto>
)

data class FinalizedTaxSummaryItemDto(
    var tax: Long,
    var taxAmount: Long,
    var includedItemsNumber: Long,
    var includedItemsAmount: Long
)

data class PendingTaxSummaryItemDto(
    var tax: Long,
    var includedItemsNumber: Long
)