package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceEntity
import com.example.data.model.NotificationCategory
import com.example.ui.PortalViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.NotificationHelper

class MainActivity : ComponentActivity() {
    private val viewModel: PortalViewModel by viewModels()
    private val targetScreenState = mutableStateOf<PortalScreen?>(null)
    private val targetIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            MyApplicationTheme {
                CustomerPortalApp(
                    viewModel = viewModel,
                    incomingScreen = targetScreenState.value,
                    incomingTargetId = targetIdState.value,
                    onClearIncoming = {
                        targetScreenState.value = null
                        targetIdState.value = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val screenName = intent?.getStringExtra(NotificationHelper.EXTRA_NAV_SCREEN)
        val targetId = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_ID)
        if (screenName != null) {
            when (screenName) {
                NotificationHelper.SCREEN_DELIVERIES -> targetScreenState.value = PortalScreen.DELIVERIES
                NotificationHelper.SCREEN_INVOICES -> targetScreenState.value = PortalScreen.INVOICES
            }
            targetIdState.value = targetId
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalApp(
    viewModel: PortalViewModel,
    incomingScreen: PortalScreen? = null,
    incomingTargetId: String? = null,
    onClearIncoming: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(PortalScreen.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    // System notification permission handling
    var isNotificationPermissionGranted by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isNotificationPermissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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

    val deliveryNotificationsEnabled by viewModel.deliveryNotificationsEnabled.collectAsStateWithLifecycle()
    val invoiceNotificationsEnabled by viewModel.invoiceNotificationsEnabled.collectAsStateWithLifecycle()

    val selectedPaymentInvoice by viewModel.selectedInvoiceForPayment.collectAsStateWithLifecycle()
    val selectedTrackingDelivery by viewModel.selectedDeliveryForTracking.collectAsStateWithLifecycle()
    val showCartSheet by viewModel.showCartBottomSheet.collectAsStateWithLifecycle()
    val showNewQuoteSheet by viewModel.showNewQuoteBottomSheet.collectAsStateWithLifecycle()
    val showInviteSheet by viewModel.showInviteReferralBottomSheet.collectAsStateWithLifecycle()
    val showNotificationsDialog by viewModel.showNotificationsDialog.collectAsStateWithLifecycle()
    val showStatementExportDialog by viewModel.showStatementExportDialog.collectAsStateWithLifecycle()
    val showGenerateInvoiceDialog by viewModel.showGenerateInvoiceDialog.collectAsStateWithLifecycle()

    val currentVerificationResult by viewModel.currentVerificationResult.collectAsStateWithLifecycle()
    val verificationHistory by viewModel.verificationHistory.collectAsStateWithLifecycle()
    val isVerifying by viewModel.isVerifying.collectAsStateWithLifecycle()

    val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val githubOwner by viewModel.githubOwner.collectAsStateWithLifecycle()
    val githubRepo by viewModel.githubRepo.collectAsStateWithLifecycle()

    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    // Handle incoming deep navigation from Notification tap
    LaunchedEffect(incomingScreen, incomingTargetId, deliveries) {
        if (incomingScreen != null) {
            currentScreen = incomingScreen
            if (incomingScreen == PortalScreen.DELIVERIES && incomingTargetId != null) {
                val matchingDelivery = deliveries.firstOrNull { it.orderId == incomingTargetId }
                if (matchingDelivery != null) {
                    viewModel.selectedDeliveryForTracking.value = matchingDelivery
                }
            }
            onClearIncoming()
        }
    }

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                                PortalScreen.VERIFICATION -> "Document Verification"
                                PortalScreen.UPDATES -> "Software Updates & APK"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { currentScreen = PortalScreen.UPDATES },
                        modifier = Modifier.testTag("top_bar_update_icon")
                    ) {
                        BadgedBox(
                            badge = {
                                if (updateCheckResult?.isUpdateAvailable == true) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text("New")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Software Updates",
                                tint = if (updateCheckResult?.isUpdateAvailable == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { currentScreen = PortalScreen.VERIFICATION },
                        modifier = Modifier.testTag("top_bar_qr_scanner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Verify Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                colors = TopAppBarDefaults.topAppBarColors(
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
                    onRevertPayment = { inv -> viewModel.revertPayment(inv.id) },
                    onOpenGenerateInvoiceDialog = { viewModel.showGenerateInvoiceDialog.value = true },
                    onSimulateInvoiceNotification = { viewModel.simulateNewInvoiceGenerated() }
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
                    onTrackDelivery = { del -> viewModel.selectedDeliveryForTracking.value = del },
                    onAdvanceDelivery = { del -> viewModel.advanceDeliveryStatus(del) },
                    onSimulateDeliveryNotification = { viewModel.simulateDeliveryStatusChange() }
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
                    onNavigateToScreen = { screen -> currentScreen = screen },
                    onOpenNotifications = { viewModel.showNotificationsDialog.value = true }
                )
                PortalScreen.VERIFICATION -> DocumentVerificationScreen(
                    currentResult = currentVerificationResult,
                    verificationHistory = verificationHistory,
                    isVerifying = isVerifying,
                    onVerifyPayload = { payload -> viewModel.verifyQrCode(payload) },
                    onClearCurrentVerification = { viewModel.clearCurrentVerification() },
                    onClearHistory = { viewModel.clearVerificationHistory() },
                    onNavigateToScreen = { screen -> currentScreen = screen },
                    onBack = { currentScreen = PortalScreen.HOME }
                )
                PortalScreen.UPDATES -> AppUpdateScreen(
                    checkResult = updateCheckResult,
                    isCheckingUpdate = isCheckingUpdate,
                    downloadStatus = downloadStatus,
                    githubOwner = githubOwner,
                    githubRepo = githubRepo,
                    canRequestInstall = viewModel.canRequestPackageInstalls(),
                    onCheckForUpdates = { viewModel.checkForUpdates(silent = false) },
                    onDownloadApk = { viewModel.downloadLatestApk() },
                    onCancelDownload = { viewModel.cancelDownload() },
                    onInstallApk = { viewModel.installDownloadedApk() },
                    onOpenInstallSettings = { viewModel.openInstallPermissionSettings() },
                    onUpdateRepoConfig = { owner, repo -> viewModel.updateGitHubRepoConfig(owner, repo) },
                    onBack = { currentScreen = PortalScreen.HOME }
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
            onDismiss = { viewModel.selectedDeliveryForTracking.value = null },
            onAdvanceDelivery = { del -> viewModel.advanceDeliveryStatus(del) }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            notifications = notifications,
            deliveryNotificationsEnabled = deliveryNotificationsEnabled,
            invoiceNotificationsEnabled = invoiceNotificationsEnabled,
            onToggleDeliveryNotifications = { viewModel.deliveryNotificationsEnabled.value = it },
            onToggleInvoiceNotifications = { viewModel.invoiceNotificationsEnabled.value = it },
            isPermissionGranted = isNotificationPermissionGranted,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onTestDeliveryAlert = { viewModel.simulateDeliveryStatusChange() },
            onTestInvoiceAlert = { viewModel.simulateNewInvoiceGenerated() },
            onDismiss = { viewModel.showNotificationsDialog.value = false },
            onMarkAsRead = { id -> viewModel.markNotificationAsRead(id) },
            onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
            onNavigateToTarget = { category, targetId ->
                when (category) {
                    NotificationCategory.DELIVERY -> {
                        currentScreen = PortalScreen.DELIVERIES
                        if (targetId != null) {
                            val match = deliveries.firstOrNull { it.orderId == targetId }
                            if (match != null) viewModel.selectedDeliveryForTracking.value = match
                        }
                    }
                    NotificationCategory.INVOICE -> {
                        currentScreen = PortalScreen.INVOICES
                    }
                    NotificationCategory.QUOTE -> {
                        currentScreen = PortalScreen.QUOTES
                    }
                    NotificationCategory.SYSTEM -> {
                        // Keep current screen
                    }
                }
            }
        )
    }

    if (showGenerateInvoiceDialog) {
        GenerateInvoiceDialog(
            onDismiss = { viewModel.showGenerateInvoiceDialog.value = false },
            onGenerateInvoice = { amount, dueDate, itemsSummary, description ->
                viewModel.generateNewInvoice(amount, dueDate, itemsSummary, description)
            }
        )
    }

    if (showStatementExportDialog) {
        StatementExportDialog(
            customerInfo = customerInfo,
            onDismiss = { viewModel.showStatementExportDialog.value = false },
            onExportComplete = {
                viewModel.showStatementExportDialog.value = false
            }
        )
    }
}
