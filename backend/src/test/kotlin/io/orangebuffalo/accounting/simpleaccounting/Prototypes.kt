package io.orangebuffalo.accounting.simpleaccounting

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.*
import java.time.Instant
import java.time.LocalDate

class Prototypes {
    companion object {
        fun fry() = platformUser(
            userName = "Fry",
            passwordHash = "qwertyHash",
            isAdmin = false
        )

        fun farnsworth() = platformUser(
            userName = "Farnsworth",
            passwordHash = "scienceBasedHash",
            isAdmin = true
        )

        fun zoidberg() = platformUser(
            userName = "Zoidberg",
            passwordHash = "??",
            isAdmin = false
        )

        fun bender() = platformUser(
            userName = "Bender",
            passwordHash = "011101010101101001",
            isAdmin = false
        )

        fun platformUser(
            userName: String = "Farnsworth",
            passwordHash: String = "nopassword",
            isAdmin: Boolean = false,
            documentsStorage: String? = null
        ) = PlatformUser(
            userName = userName,
            passwordHash = passwordHash,
            isAdmin = isAdmin,
            documentsStorage = documentsStorage
        )

        fun workspace(
            name: String = "Planet Express",
            owner: PlatformUser = platformUser(),
            taxEnabled: Boolean = true,
            multiCurrencyEnabled: Boolean = true,
            defaultCurrency: String = "USD",
            categories: MutableList<Category> = ArrayList()
        ) = Workspace(
            name = name,
            owner = owner,
            taxEnabled = taxEnabled,
            multiCurrencyEnabled = multiCurrencyEnabled,
            defaultCurrency = defaultCurrency,
            categories = categories
        )

        fun tax(
            title: String = "Tax",
            rateInBps: Int = 10_00,
            description: String? = null,
            workspace: Workspace = workspace()
        ) = Tax(
            title = title,
            workspace = workspace,
            rateInBps = rateInBps,
            description = description
        )

        fun category(
            name: String = "Delivery",
            description: String? = null,
            workspace: Workspace = workspace(),
            income: Boolean = true,
            expense: Boolean = true
        ) = Category(
            name = name,
            workspace = workspace,
            income = income,
            expense = expense,
            description = description
        )

        fun expense(
            category: Category? = category(),
            workspace: Workspace = workspace(),
            title: String = "Expense",

            timeRecorded: Instant = MOCK_TIME,
            datePaid: LocalDate = MOCK_DATE,
            currency: String = "USD",
            originalAmount: Long = 100,
            amountInDefaultCurrency: Long = 100,
            actualAmountInDefaultCurrency: Long = 100,
            attachments: Set<Document> = setOf(),
            percentOnBusiness: Int = 100,
            tax: Tax? = null,
            taxRateInBps: Int? = null,
            taxAmount: Long? = null,
            reportedAmountInDefaultCurrency: Long = 100,
            notes: String? = null
        ) = Expense(
            workspace = workspace,
            category = category,
            title = title,
            datePaid = datePaid,
            timeRecorded = timeRecorded,
            currency = currency,
            originalAmount = originalAmount,
            amountInDefaultCurrency = amountInDefaultCurrency,
            actualAmountInDefaultCurrency = actualAmountInDefaultCurrency,
            reportedAmountInDefaultCurrency = reportedAmountInDefaultCurrency,
            percentOnBusiness = percentOnBusiness,
            tax = tax,
            attachments = attachments,
            taxAmount = taxAmount,
            taxRateInBps = taxRateInBps,
            notes = notes
        )

        fun income(
            category: Category = category(),
            workspace: Workspace = category.workspace,
            title: String = "Income",
            timeRecorded: Instant = MOCK_TIME,
            dateReceived: LocalDate = MOCK_DATE,
            currency: String = "USD",
            originalAmount: Long = 100,
            amountInDefaultCurrency: Long = 100,
            reportedAmountInDefaultCurrency: Long = 100,
            attachments: Set<Document> = setOf(),
            notes: String? = null,
            tax: Tax? = null,
            taxRateInBps: Int? = null,
            taxAmount: Long? = null
        ) = Income(
            category = category,
            workspace = workspace,
            taxAmount = taxAmount,
            reportedAmountInDefaultCurrency = reportedAmountInDefaultCurrency,
            tax = tax,
            notes = notes,
            taxRateInBps = taxRateInBps,
            attachments = attachments,
            amountInDefaultCurrency = amountInDefaultCurrency,
            currency = currency,
            dateReceived = dateReceived,
            originalAmount = originalAmount,
            timeRecorded = timeRecorded,
            title = title
        )

        fun document(
            name: String = "Slurm Receipt",
            notes: String? = null,
            workspace: Workspace = workspace(),
            timeUploaded: Instant = MOCK_TIME,
            storageProviderId: String = "test-storage",
            storageProviderLocation: String? = "test-location",
            sizeInBytes: Long? = null
        ): Document = Document(
            name = name,
            workspace = workspace,
            storageProviderId = storageProviderId,
            storageProviderLocation = storageProviderLocation,
            timeUploaded = timeUploaded,
            sizeInBytes = sizeInBytes,
            notes = notes
        )

        fun taxPayment(
            workspace: Workspace = workspace(),
            timeRecorded: Instant = MOCK_TIME,
            datePaid: LocalDate = MOCK_DATE,
            reportingDate: LocalDate = MOCK_DATE,
            amount: Long = 100,
            title: String = "Tax Payment",
            attachments: Set<Document> = setOf(),
            notes: String? = null
        ): TaxPayment = TaxPayment(
            workspace = workspace,
            timeRecorded = timeRecorded,
            datePaid = datePaid,
            reportingDate = reportingDate,
            amount = amount,
            title = title,
            attachments = attachments,
            notes = notes
        )

        fun customer(
            name: String = "customer",
            workspace: Workspace = workspace()
        ): Customer = Customer(
            name = name,
            workspace = workspace
        )

        fun invoice(
            income: Income? = null,
            customer: Customer = customer(),
            title: String = "invoice",
            timeRecorded: Instant = MOCK_TIME,
            dateIssued: LocalDate = MOCK_DATE,
            dateSent: LocalDate? = null,
            datePaid: LocalDate? = null,
            dateCancelled: LocalDate? = null,
            dueDate: LocalDate = MOCK_DATE,
            currency: String = "USD",
            amount: Long = 100,
            attachments: Set<Document> = setOf(),
            notes: String? = null,
            tax: Tax? = null
        ): Invoice = Invoice(
            income = income,
            customer = customer,
            title = title,
            timeRecorded = timeRecorded,
            dateIssued = dateIssued,
            dateSent = dateSent,
            datePaid = datePaid,
            dateCancelled = dateCancelled,
            dueDate = dueDate,
            currency = currency,
            amount = amount,
            attachments = attachments,
            notes = notes,
            tax = tax
        )

        fun workspaceAccessToken(
            workspace: Workspace = workspace(),
            timeCreated: Instant = MOCK_TIME,
            validTill: Instant = MOCK_TIME,
            revoked: Boolean = false,
            token: String = "token"
        ): WorkspaceAccessToken = WorkspaceAccessToken(
            workspace = workspace,
            timeCreated = timeCreated,
            validTill = validTill,
            revoked = revoked,
            token = token
        )
    }
}