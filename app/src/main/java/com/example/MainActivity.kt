package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceEntity
import com.example.ui.PortalViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val viewModel: PortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CustomerPortalApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalApp(viewModel: PortalViewModel) {
    var currentScreen by remember { mutableStateOf(PortalScreen.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    val customerInfo by viewModel.customerInfo.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val unpaidInvoices by viewModel.unpaidInvoices.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val quotes by viewModel.quotes.collectAsStateWithLifecycle()
    val ledgerEntries by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val referrals by viewModel.referrals.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()

    val selectedPaymentInvoice by viewModel.selectedInvoiceForPayment.collectAsStateWithLifecycle()
    val selectedTrackingDelivery by viewModel.selectedDeliveryForTracking.collectAsStateWithLifecycle()
    val showCartSheet by viewModel.showCartBottomSheet.collectAsStateWithLifecycle()
    val showNewQuoteSheet by viewModel.showNewQuoteBottomSheet.collectAsStateWithLifecycle()
    val showInviteSheet by viewModel.showInviteReferralBottomSheet.collectAsStateWithLifecycle()
    val showNotificationsDialog by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
    val showStatementExportDialog by viewModel.showStatementExportDialog.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customer_portal_main_scaffold"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (currentScreen) {
                                PortalScreen.HOME -> if (customerInfo.companyName.isNotEmpty()) customerInfo.companyName else "Customer Portal"
                                PortalScreen.INVOICES -> "Invoices & Payments"
                                PortalScreen.ORDERS -> "Product Orders"
                                PortalScreen.DELIVERIES -> "Deliveries & Tracking"
                                PortalScreen.QUOTES -> "Custom Quotes"
                                PortalScreen.REFERRALS -> "Referral Program"
                                PortalScreen.STATEMENTS -> "Account Statements"
                                PortalScreen.PROFILE -> "Company Profile"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showNotificationsDialog.value = true },
                        modifier = Modifier.testTag("top_bar_notification_bell")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotificationsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text("$unreadNotificationsCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            PortalBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it },
                unpaidCount = unpaidInvoices.size,
                cartCount = cartItems.sumOf { it.quantity },
                deliveryCount = deliveries.count { it.status.name != "DELIVERED" }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                PortalScreen.HOME -> HomeScreen(
                    customerInfo = customerInfo,
                    unpaidInvoices = unpaidInvoices,
                    activeDeliveriesCount = deliveries.count { it.status.name != "DELIVERED" },
                    unreadNotificationsCount = unreadNotificationsCount,
                    onNavigateToScreen = { screen -> currentScreen = screen },
                    onOpenNotifications = { viewModel.showNotificationsDialog.value = true },
                    onPayInvoice = { inv -> viewModel.selectedInvoiceForPayment.value = inv }
                )
                PortalScreen.INVOICES -> InvoicesScreen(
                    invoices = invoices,
                    onPayInvoice = { inv -> viewModel.selectedInvoiceForPayment.value = inv },
                    onRevertPayment = { inv -> viewModel.revertPayment(inv.id) }
                )
                PortalScreen.ORDERS -> OrdersScreen(
                    products = products,
                    orders = orders,
                    cartItems = cartItems,
                    onAddToCart = { product -> viewModel.addToCart(product) },
                    onOpenCart = { viewModel.showCartBottomSheet.value = true }
                )
                PortalScreen.DELIVERIES -> DeliveriesScreen(
                    deliveries = deliveries,
                    onTrackDelivery = { del -> viewModel.selectedDeliveryForTracking.value = del }
                )
                PortalScreen.QUOTES -> QuotesScreen(
                    quotes = quotes,
                    onOpenNewQuoteSheet = { viewModel.showNewQuoteBottomSheet.value = true }
                )
                PortalScreen.REFERRALS -> ReferralsScreen(
                    customerInfo = customerInfo,
                    referrals = referrals,
                    onOpenInviteSheet = { viewModel.showInviteReferralBottomSheet.value = true }
                )
                PortalScreen.STATEMENTS -> StatementsScreen(
                    customerInfo = customerInfo,
                    ledgerEntries = ledgerEntries,
                    onOpenExportDialog = { viewModel.showStatementExportDialog.value = true }
                )
                PortalScreen.PROFILE -> ProfileScreen(
                    customerInfo = customerInfo,
                    onUpdateProfile = { name, email, phone, addr ->
                        viewModel.updateCustomerProfile(name, email, phone, addr)
                    },
                    onNavigateToScreen = { screen -> currentScreen = screen }
                )
            }
        }
    }

    // Modal Bottom Sheets & Dialogs
    if (selectedPaymentInvoice != null) {
        PaymentBottomSheet(
            invoice = selectedPaymentInvoice!!,
            onDismiss = { viewModel.selectedInvoiceForPayment.value = null },
            onConfirmPayment = { method ->
                viewModel.submitPayment(selectedPaymentInvoice!!.id, method)
            }
        )
    }

    if (showCartSheet) {
        NewOrderBottomSheet(
            cartItems = cartItems,
            onUpdateQuantity = { prodId, delta -> viewModel.updateCartQuantity(prodId, delta) },
            onDismiss = { viewModel.showCartBottomSheet.value = false },
            onSubmitOrder = { address -> viewModel.submitOrder(address) }
        )
    }

    if (showNewQuoteSheet) {
        NewQuoteBottomSheet(
            onDismiss = { viewModel.showNewQuoteBottomSheet.value = false },
            onSubmitQuote = { cat, det, qty -> viewModel.submitQuoteRequest(cat, det, qty) }
        )
    }

    if (showInviteSheet) {
        InviteReferralBottomSheet(
            onDismiss = { viewModel.showInviteReferralBottomSheet.value = false },
            onSubmitInvite = { name, company, email ->
                viewModel.submitReferralInvite(name, company, email)
            }
        )
    }

    if (selectedTrackingDelivery != null) {
        TrackingDialog(
            delivery = selectedTrackingDelivery!!,
            onDismiss = { viewModel.selectedDeliveryForTracking.value = null }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { viewModel.showNotificationsDialog.value = false },
            onMarkAsRead = { id -> viewModel.markNotificationAsRead(id) },
            onMarkAllRead = { viewModel.markAllNotificationsAsRead() }
        )
    }

    if (showStatementExportDialog) {
        StatementExportDialog(
            customerInfo = customerInfo,
            onDismiss = { viewModel.showStatementExportDialog.value = false },
            onExportComplete = { msg ->
                viewModel.showStatementExportDialog.value = false
            }
        )
    }
}
