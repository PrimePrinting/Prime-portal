package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PortalRepository(private val db: AppDatabase) {
    val invoices: Flow<List<InvoiceEntity>> = db.invoiceDao().getAllInvoices()
    val unpaidInvoices: Flow<List<InvoiceEntity>> = db.invoiceDao().getUnpaidInvoices()
    val deliveries: Flow<List<DeliveryEntity>> = db.deliveryDao().getAllDeliveries()
    val products: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val orders: Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    val quotes: Flow<List<QuoteRequestEntity>> = db.quoteDao().getAllQuotes()
    val ledgerEntries: Flow<List<LedgerEntryEntity>> = db.ledgerDao().getAllLedgerEntries()
    val referrals: Flow<List<ReferralEntity>> = db.referralDao().getAllReferrals()
    val notifications: Flow<List<NotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun payInvoice(invoiceId: Long, paymentMethod: String): Boolean {
        val invoice = db.invoiceDao().getInvoiceById(invoiceId) ?: return false
        val today = "2026-08-09"
        val updated = invoice.copy(
            status = InvoiceStatus.PAID,
            paymentMethod = paymentMethod,
            paymentDate = today
        )
        db.invoiceDao().updateInvoice(updated)

        // Add to ledger
        val currentLedger = db.ledgerDao().getAllLedgerEntries().first()
        val currentBalance = currentLedger.firstOrNull()?.runningBalance ?: 1240.0
        val newBalance = currentBalance - invoice.amount
        db.ledgerDao().insertLedgerEntry(
            LedgerEntryEntity(
                date = today,
                referenceNo = "PAY-${invoice.invoiceNumber}",
                description = "Online Payment via $paymentMethod for Invoice ${invoice.invoiceNumber}",
                type = LedgerType.PAYMENT,
                amount = -invoice.amount,
                runningBalance = newBalance
            )
        )

        // Add Notification
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Payment Received",
                message = "Payment of \$${String.format("%.2f", invoice.amount)} for Invoice ${invoice.invoiceNumber} was successful.",
                timestamp = "Just now",
                category = NotificationCategory.INVOICE
            )
        )
        return true
    }

    suspend fun revertPayment(invoiceId: Long): Boolean {
        val invoice = db.invoiceDao().getInvoiceById(invoiceId) ?: return false
        val newStatus = if (invoice.invoiceNumber.endsWith("001")) InvoiceStatus.OVERDUE else InvoiceStatus.UNPAID
        val updated = invoice.copy(
            status = newStatus,
            paymentMethod = null,
            paymentDate = null
        )
        db.invoiceDao().updateInvoice(updated)
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Payment Reverted",
                message = "Payment for Invoice ${invoice.invoiceNumber} has been reverted to ${newStatus.name}.",
                timestamp = "Just now",
                category = NotificationCategory.INVOICE
            )
        )
        return true
    }

    suspend fun resetAllPayments() {
        val invoicesList = db.invoiceDao().getAllInvoices().first()
        invoicesList.forEach { inv ->
            if (inv.invoiceNumber == "INV-2026-001") {
                db.invoiceDao().updateInvoice(inv.copy(status = InvoiceStatus.OVERDUE, paymentMethod = null, paymentDate = null))
            } else if (inv.invoiceNumber == "INV-2026-002") {
                db.invoiceDao().updateInvoice(inv.copy(status = InvoiceStatus.UNPAID, paymentMethod = null, paymentDate = null))
            }
        }
    }

    suspend fun createOrder(cartItems: List<CartItem>, shippingAddress: String): OrderEntity {
        val today = "2026-08-09"
        val totalAmount = cartItems.sumOf { it.product.unitPrice * it.quantity }
        val orderNo = "ORD-" + (1000..9999).random()
        val summary = cartItems.joinToString(", ") { "${it.quantity}x ${it.product.name}" }

        val order = OrderEntity(
            orderNumber = orderNo,
            date = today,
            totalAmount = totalAmount,
            status = OrderStatus.CONFIRMED,
            itemsSummary = summary,
            shippingAddress = shippingAddress
        )
        val orderId = db.orderDao().insertOrder(order)

        // Create initial delivery entry
        db.deliveryDao().insertDelivery(
            DeliveryEntity(
                orderId = orderNo,
                trackingNumber = "TRK-" + (100000..999999).random(),
                status = DeliveryStatus.PROCESSING,
                driverName = "David Miller",
                driverPhone = "+1 (555) 382-9102",
                vehicleNo = "TRK-882",
                estimatedArrival = "Tomorrow, 2:00 PM",
                deliveryAddress = shippingAddress,
                currentStep = 1
            )
        )

        // Create Invoice for Order
        val invoiceNo = "INV-2026-" + (300..999).random()
        db.invoiceDao().insertInvoice(
            InvoiceEntity(
                invoiceNumber = invoiceNo,
                issueDate = today,
                dueDate = "2026-08-23",
                amount = totalAmount,
                tax = totalAmount * 0.08,
                status = InvoiceStatus.UNPAID,
                itemsSummary = summary,
                description = "Invoice generated for Order $orderNo"
            )
        )

        // Add Notification
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Order Confirmed!",
                message = "Order $orderNo (\$${String.format("%.2f", totalAmount)}) has been placed successfully.",
                timestamp = "Just now",
                category = NotificationCategory.DELIVERY
            )
        )

        return order
    }

    suspend fun createQuoteRequest(category: String, details: String, quantity: Int) {
        val today = "2026-08-09"
        val quoteNo = "QREQ-" + (2000..9999).random()
        db.quoteDao().insertQuote(
            QuoteRequestEntity(
                quoteNumber = quoteNo,
                requestDate = today,
                category = category,
                details = details,
                targetQuantity = quantity,
                status = QuoteStatus.PENDING
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Quote Request Submitted",
                message = "Your quotation request $quoteNo for $category ($quantity units) is under review by our sales team.",
                timestamp = "Just now",
                category = NotificationCategory.QUOTE
            )
        )
    }

    suspend fun createReferral(name: String, company: String, email: String) {
        val today = "2026-08-09"
        db.referralDao().insertReferral(
            ReferralEntity(
                refereeName = name,
                refereeCompany = company,
                email = email,
                status = ReferralStatus.INVITED,
                rewardAmount = 50.0,
                dateInvited = today
            )
        )
        db.notificationDao().insertNotification(
            NotificationEntity(
                title = "Referral Invitation Sent",
                message = "Invitation sent to $name at $company ($email). You'll earn \$50 credit upon their first order!",
                timestamp = "Just now",
                category = NotificationCategory.SYSTEM
            )
        )
    }

    suspend fun markNotificationAsRead(id: Long) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun seedSampleDataIfEmpty() {
        val existingInvoices = db.invoiceDao().getAllInvoices().first()
        if (existingInvoices.isNotEmpty()) return

        // Seed Invoices
        db.invoiceDao().insertInvoices(
            listOf(
                InvoiceEntity(
                    invoiceNumber = "INV-2026-001",
                    issueDate = "2026-07-15",
                    dueDate = "2026-08-01",
                    amount = 840.50,
                    tax = 62.26,
                    status = InvoiceStatus.OVERDUE,
                    itemsSummary = "50x Heavy Duty Storage Containers, 10x Industrial Pallets",
                    description = "Monthly warehouse materials batch order #49102."
                ),
                InvoiceEntity(
                    invoiceNumber = "INV-2026-002",
                    issueDate = "2026-08-02",
                    dueDate = "2026-08-20",
                    amount = 399.50,
                    tax = 29.59,
                    status = InvoiceStatus.UNPAID,
                    itemsSummary = "2x Ergonomic Office Chairs, 4x Dual Monitor Arms",
                    description = "Office furniture expansion request #50211."
                ),
                InvoiceEntity(
                    invoiceNumber = "INV-2026-003",
                    issueDate = "2026-06-10",
                    dueDate = "2026-06-25",
                    amount = 1250.00,
                    tax = 92.59,
                    status = InvoiceStatus.PAID,
                    itemsSummary = "1x Server Rack System, 20x Cat6 Ethernet Bundles",
                    description = "IT Infrastructure cabling supplies.",
                    paymentMethod = "Corporate Visa ****4920",
                    paymentDate = "2026-06-24"
                ),
                InvoiceEntity(
                    invoiceNumber = "INV-2026-004",
                    issueDate = "2026-05-01",
                    dueDate = "2026-05-15",
                    amount = 620.00,
                    tax = 45.92,
                    status = InvoiceStatus.PAID,
                    itemsSummary = "100x Eco Safety Gloves, 25x Hard Hats",
                    description = "Safety gear replenishment.",
                    paymentMethod = "Bank Wire Transfer",
                    paymentDate = "2026-05-12"
                )
            )
        )

        // Seed Deliveries
        db.deliveryDao().insertDeliveries(
            listOf(
                DeliveryEntity(
                    orderId = "ORD-1084",
                    trackingNumber = "TRK-992381",
                    status = DeliveryStatus.OUT_FOR_DELIVERY,
                    driverName = "Alex Rivera",
                    driverPhone = "+1 (555) 892-4112",
                    vehicleNo = "VAN-402",
                    estimatedArrival = "Today, 3:30 PM",
                    deliveryAddress = "Building 4, Dock B, 742 Evergreen Terrace, Springfield, OR",
                    currentStep = 3
                ),
                DeliveryEntity(
                    orderId = "ORD-1072",
                    trackingNumber = "TRK-881204",
                    status = DeliveryStatus.PROCESSING,
                    driverName = "Sam Harris",
                    driverPhone = "+1 (555) 720-1923",
                    vehicleNo = "TRK-109",
                    estimatedArrival = "Aug 12, 10:00 AM",
                    deliveryAddress = "742 Evergreen Terrace, Springfield, OR",
                    currentStep = 1
                ),
                DeliveryEntity(
                    orderId = "ORD-1010",
                    trackingNumber = "TRK-771902",
                    status = DeliveryStatus.DELIVERED,
                    driverName = "John Higgins",
                    driverPhone = "+1 (555) 203-9182",
                    vehicleNo = "VAN-201",
                    estimatedArrival = "Delivered Aug 5, 2:15 PM",
                    deliveryAddress = "742 Evergreen Terrace, Springfield, OR",
                    currentStep = 4
                )
            )
        )

        // Seed Products
        db.productDao().insertProducts(
            listOf(
                ProductEntity(
                    name = "Heavy Duty Storage Container",
                    sku = "IND-STR-01",
                    category = "Industrial Supplies",
                    unitPrice = 16.81,
                    stockQuantity = 450,
                    description = "Stackable, impact-resistant industrial storage bins with reinforced handles.",
                    unit = "Box"
                ),
                ProductEntity(
                    name = "Industrial Wooden Pallet (48x40)",
                    sku = "IND-PLT-02",
                    category = "Industrial Supplies",
                    unitPrice = 24.50,
                    stockQuantity = 120,
                    description = "Standard 4-way entry hardwood shipping pallet. GMA grade A capacity 4,600 lbs.",
                    unit = "Pallet"
                ),
                ProductEntity(
                    name = "Ergonomic Mesh Office Chair",
                    sku = "OFF-CHR-08",
                    category = "Office Furniture",
                    unitPrice = 199.75,
                    stockQuantity = 35,
                    description = "Adjustable lumbar support, 3D armrests, and breathable high-grade mesh.",
                    unit = "Chair"
                ),
                ProductEntity(
                    name = "Dual Monitor Gas Spring Arm",
                    sku = "OFF-MNT-02",
                    category = "Office Furniture",
                    unitPrice = 49.90,
                    stockQuantity = 80,
                    description = "Full motion desk mount for dual screens up to 32 inches with integrated cable routing.",
                    unit = "Unit"
                ),
                ProductEntity(
                    name = "42U Server Rack Cabinet",
                    sku = "IT-RACK-42",
                    category = "Electronics & IT",
                    unitPrice = 850.00,
                    stockQuantity = 12,
                    description = "Enclosed server enclosure with lockable mesh doors and high-airflow fans.",
                    unit = "Unit"
                ),
                ProductEntity(
                    name = "Cat6 Ethernet Cable Roll (1000ft)",
                    sku = "IT-CBL-C6",
                    category = "Electronics & IT",
                    unitPrice = 140.00,
                    stockQuantity = 60,
                    description = "Pure copper 23AWG solid bulk cable, plenum CMP rated.",
                    unit = "Spool"
                ),
                ProductEntity(
                    name = "High-Visibility Safety Vests (Pack of 10)",
                    sku = "SAF-VST-10",
                    category = "Safety Gear",
                    unitPrice = 35.00,
                    stockQuantity = 200,
                    description = "ANSI/ISEA Class 2 compliant reflective safety vests with 5 pockets.",
                    unit = "Pack"
                )
            )
        )

        // Seed Orders
        db.orderDao().insertOrders(
            listOf(
                OrderEntity(
                    orderNumber = "ORD-1084",
                    date = "2026-08-07",
                    totalAmount = 840.50,
                    status = OrderStatus.IN_TRANSIT,
                    itemsSummary = "50x Heavy Duty Storage Container, 10x Industrial Wooden Pallet",
                    shippingAddress = "Building 4, Dock B, Springfield, OR"
                ),
                OrderEntity(
                    orderNumber = "ORD-1072",
                    date = "2026-08-05",
                    totalAmount = 399.50,
                    status = OrderStatus.PENDING,
                    itemsSummary = "2x Ergonomic Mesh Office Chair, 4x Dual Monitor Gas Spring Arm",
                    shippingAddress = "Main Office, 742 Evergreen Terrace, Springfield, OR"
                ),
                OrderEntity(
                    orderNumber = "ORD-1010",
                    date = "2026-07-28",
                    totalAmount = 1250.00,
                    status = OrderStatus.DELIVERED,
                    itemsSummary = "1x 42U Server Rack Cabinet, 2x Cat6 Cable Roll",
                    shippingAddress = "IT Data Center, Springfield, OR"
                )
            )
        )

        // Seed Quotes
        db.quoteDao().insertQuotes(
            listOf(
                QuoteRequestEntity(
                    quoteNumber = "QREQ-8821",
                    requestDate = "2026-08-04",
                    category = "Custom Bulk Packaging",
                    details = "Custom printed corrugated shipping boxes (24x18x12 in) with company logo branding.",
                    targetQuantity = 2500,
                    status = QuoteStatus.QUOTED,
                    quotedPrice = 3125.00,
                    salesNotes = "Bulk tier discount applied (15% off). Production lead time 7 business days."
                ),
                QuoteRequestEntity(
                    quoteNumber = "QREQ-8890",
                    requestDate = "2026-08-08",
                    category = "Solar Powered Warehouse Lighting",
                    details = "High-bay LED fixture retrofit units with smart motion sensors.",
                    targetQuantity = 80,
                    status = QuoteStatus.PENDING,
                    salesNotes = "Engineering team is currently reviewing lumen requirements."
                )
            )
        )

        // Seed Ledger
        db.ledgerDao().insertLedgerEntries(
            listOf(
                LedgerEntryEntity(
                    date = "2026-08-02",
                    referenceNo = "INV-2026-002",
                    description = "Office expansion furniture batch order",
                    type = LedgerType.INVOICE,
                    amount = 399.50,
                    runningBalance = 1240.00
                ),
                LedgerEntryEntity(
                    date = "2026-07-15",
                    referenceNo = "INV-2026-001",
                    description = "Warehouse storage bins & pallets order",
                    type = LedgerType.INVOICE,
                    amount = 840.50,
                    runningBalance = 840.50
                ),
                LedgerEntryEntity(
                    date = "2026-06-24",
                    referenceNo = "PAY-INV-2026-003",
                    description = "Payment received - Visa ****4920",
                    type = LedgerType.PAYMENT,
                    amount = -1250.00,
                    runningBalance = 0.0
                ),
                LedgerEntryEntity(
                    date = "2026-06-10",
                    referenceNo = "INV-2026-003",
                    description = "IT Infrastructure Server Rack & Cabling",
                    type = LedgerType.INVOICE,
                    amount = 1250.00,
                    runningBalance = 1250.00
                )
            )
        )

        // Seed Referrals
        db.referralDao().insertReferrals(
            listOf(
                ReferralEntity(
                    refereeName = "Marcus Vance",
                    refereeCompany = "Apex Logistics Corp",
                    email = "m.vance@apexlogistics.io",
                    status = ReferralStatus.COMPLETED,
                    rewardAmount = 150.0,
                    dateInvited = "2026-06-12"
                ),
                ReferralEntity(
                    refereeName = "Sarah Jenkins",
                    refereeCompany = "Nexus Distributing Co.",
                    email = "sjenkins@nexusdist.com",
                    status = ReferralStatus.SIGNED_UP,
                    rewardAmount = 100.0,
                    dateInvited = "2026-07-02"
                ),
                ReferralEntity(
                    refereeName = "David K. Chen",
                    refereeCompany = "Chen Manufacturing",
                    email = "dchen@chenmfg.com",
                    status = ReferralStatus.INVITED,
                    rewardAmount = 50.0,
                    dateInvited = "2026-08-01"
                )
            )
        )

        // Seed Notifications
        db.notificationDao().insertNotifications(
            listOf(
                NotificationEntity(
                    title = "Driver Out For Delivery",
                    message = "Order #ORD-1084 is out for delivery with driver Alex Rivera (Van-402). ETA: Today 3:30 PM.",
                    timestamp = "10 mins ago",
                    category = NotificationCategory.DELIVERY,
                    isRead = false,
                    actionTargetId = "ORD-1084"
                ),
                NotificationEntity(
                    title = "Quotation Ready for Review",
                    message = "Quote QREQ-8821 for Custom Bulk Packaging is ready! Total: \$3,125.00.",
                    timestamp = "2 hours ago",
                    category = NotificationCategory.QUOTE,
                    isRead = false,
                    actionTargetId = "QREQ-8821"
                ),
                NotificationEntity(
                    title = "Invoice Overdue Reminder",
                    message = "Invoice INV-2026-001 (\$840.50) was due on Aug 1. Tap to pay now.",
                    timestamp = "Yesterday",
                    category = NotificationCategory.INVOICE,
                    isRead = false,
                    actionTargetId = "INV-2026-001"
                ),
                NotificationEntity(
                    title = "Referral Reward Credited!",
                    message = "You earned \$150 credit because Apex Logistics placed their first order!",
                    timestamp = "3 days ago",
                    category = NotificationCategory.SYSTEM,
                    isRead = true
                )
            )
        )
    }
}
