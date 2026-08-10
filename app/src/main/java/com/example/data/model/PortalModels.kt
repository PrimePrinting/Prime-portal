package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvoiceStatus {
    UNPAID, OVERDUE, PAID, PARTIAL
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val amount: Double,
    val tax: Double = 0.0,
    val status: InvoiceStatus,
    val itemsSummary: String,
    val description: String = "",
    val paymentMethod: String? = null,
    val paymentDate: String? = null
)

enum class DeliveryStatus {
    PROCESSING, SHIPPED, OUT_FOR_DELIVERY, DELIVERED
}

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val trackingNumber: String,
    val status: DeliveryStatus,
    val driverName: String,
    val driverPhone: String,
    val vehicleNo: String,
    val estimatedArrival: String,
    val deliveryAddress: String,
    val currentStep: Int = 2 // 1: Order Placed, 2: Dispatched, 3: Out for Delivery, 4: Delivered
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String,
    val category: String,
    val unitPrice: Double,
    val stockQuantity: Int,
    val description: String,
    val unit: String = "Unit"
)

enum class OrderStatus {
    PENDING, CONFIRMED, IN_TRANSIT, DELIVERED, CANCELLED
}

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val date: String,
    val totalAmount: Double,
    val status: OrderStatus,
    val itemsSummary: String,
    val shippingAddress: String
)

data class CartItem(
    val product: ProductEntity,
    var quantity: Int
)

enum class QuoteStatus {
    PENDING, QUOTED, APPROVED, REJECTED
}

@Entity(tableName = "quote_requests")
data class QuoteRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteNumber: String,
    val requestDate: String,
    val category: String,
    val details: String,
    val targetQuantity: Int,
    val status: QuoteStatus,
    val quotedPrice: Double? = null,
    val salesNotes: String? = null
)

enum class LedgerType {
    INVOICE, PAYMENT, CREDIT_MEMO
}

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val referenceNo: String,
    val description: String,
    val type: LedgerType,
    val amount: Double,
    val runningBalance: Double
)

enum class ReferralStatus {
    INVITED, SIGNED_UP, COMPLETED
}

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val refereeName: String,
    val refereeCompany: String,
    val email: String,
    val status: ReferralStatus,
    val rewardAmount: Double,
    val dateInvited: String
)

enum class NotificationCategory {
    DELIVERY, INVOICE, QUOTE, SYSTEM
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: String,
    val category: NotificationCategory,
    val isRead: Boolean = false,
    val actionTargetId: String? = null
)
