package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.PortalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PortalUiState {
    object Loading : PortalUiState()
    object Success : PortalUiState()
}

data class CustomerInfo(
    val companyName: String = "Acme Global Enterprise",
    val customerCode: String = "CUST-98231",
    val tier: String = "Gold Partner",
    val accountBalance: Double = 1240.00,
    val creditLimit: Double = 10000.00,
    val totalReferralEarned: Double = 250.00,
    val contactName: String = "John Doe",
    val email: String = "john.doe@acme.com",
    val phone: String = "+1 (555) 234-5678",
    val address: String = "100 Industrial Parkway, Suite 400, Chicago, IL 60601",
    val taxId: String = "EIN-98-341209",
    val accountManager: String = "Sarah Jenkins",
    val memberSince: String = "January 2023",
    val status: String = "Active (Good Standing)"
)

class PortalViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = PortalRepository(db)

    val customerInfo = MutableStateFlow(CustomerInfo())

    val invoices: StateFlow<List<InvoiceEntity>> = repository.invoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidInvoices: StateFlow<List<InvoiceEntity>> = repository.unpaidInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveries: StateFlow<List<DeliveryEntity>> = repository.deliveries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.orders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quotes: StateFlow<List<QuoteRequestEntity>> = repository.quotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = repository.ledgerEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val referrals: StateFlow<List<ReferralEntity>> = repository.referrals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Shopping Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Dialog & Sheet States
    val selectedInvoiceForPayment = MutableStateFlow<InvoiceEntity?>(null)
    val selectedDeliveryForTracking = MutableStateFlow<DeliveryEntity?>(null)
    val showCartBottomSheet = MutableStateFlow(false)
    val showNewQuoteBottomSheet = MutableStateFlow(false)
    val showInviteReferralBottomSheet = MutableStateFlow(false)
    val showNotificationsDialog = MutableStateFlow(false)
    val showStatementExportDialog = MutableStateFlow(false)

    // User Message/Toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            repository.resetAllPayments()
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // Cart management
    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            current[existingIndex] = current[existingIndex].copy(quantity = current[existingIndex].quantity + 1)
        } else {
            current.add(CartItem(product = product, quantity = 1))
        }
        _cartItems.value = current
        _userMessage.value = "Added ${product.name} to order cart"
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        val current = _cartItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == productId }
        if (existingIndex >= 0) {
            val newQty = current[existingIndex].quantity + delta
            if (newQty <= 0) {
                current.removeAt(existingIndex)
            } else {
                current[existingIndex] = current[existingIndex].copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // Payment
    fun submitPayment(invoiceId: Long, paymentMethod: String) {
        viewModelScope.launch {
            val success = repository.payInvoice(invoiceId, paymentMethod)
            if (success) {
                selectedInvoiceForPayment.value = null
                _userMessage.value = "Payment successful! Invoice status updated."
            } else {
                _userMessage.value = "Payment failed. Please try again."
            }
        }
    }

    fun revertPayment(invoiceId: Long) {
        viewModelScope.launch {
            val success = repository.revertPayment(invoiceId)
            if (success) {
                _userMessage.value = "Payment reverted successfully. Invoice set back to unpaid/overdue."
            }
        }
    }

    // Checkout
    fun submitOrder(shippingAddress: String) {
        if (_cartItems.value.isEmpty()) return
        viewModelScope.launch {
            val order = repository.createOrder(_cartItems.value, shippingAddress)
            clearCart()
            showCartBottomSheet.value = false
            _userMessage.value = "Order ${order.orderNumber} placed successfully!"
        }
    }

    // Request Quote
    fun submitQuoteRequest(category: String, details: String, quantity: Int) {
        viewModelScope.launch {
            repository.createQuoteRequest(category, details, quantity)
            showNewQuoteBottomSheet.value = false
            _userMessage.value = "Quotation request submitted to sales representative!"
        }
    }

    // Invite Referral
    fun submitReferralInvite(name: String, company: String, email: String) {
        viewModelScope.launch {
            repository.createReferral(name, company, email)
            showInviteReferralBottomSheet.value = false
            _userMessage.value = "Referral invite sent to $name!"
        }
    }

    fun updateCustomerProfile(contactName: String, email: String, phone: String, address: String) {
        customerInfo.value = customerInfo.value.copy(
            contactName = contactName,
            email = email,
            phone = phone,
            address = address
        )
        _userMessage.value = "Profile information updated successfully!"
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _userMessage.value = "All notifications marked as read"
        }
    }
}
