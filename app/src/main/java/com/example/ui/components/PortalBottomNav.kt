package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

enum class PortalScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Overview", Icons.Filled.Home, Icons.Outlined.Home),
    INVOICES("invoices", "Invoices", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    ORDERS("orders", "Orders", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag),
    DELIVERIES("deliveries", "Deliveries", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    QUOTES("quotes", "Quotes", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    REFERRALS("referrals", "Referrals", Icons.Filled.CardGiftcard, Icons.Outlined.CardGiftcard),
    STATEMENTS("statements", "Statements", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PROFILE("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
    VERIFICATION("verification", "Verify", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    UPDATES("updates", "Updates", Icons.Filled.SystemUpdate, Icons.Outlined.SystemUpdate)
}

@Composable
fun PortalBottomBar(
    currentScreen: PortalScreen,
    onScreenSelected: (PortalScreen) -> Unit,
    unpaidCount: Int,
    cartCount: Int,
    deliveryCount: Int
) {
    NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        val primaryScreens = listOf(
            PortalScreen.HOME,
            PortalScreen.INVOICES,
            PortalScreen.ORDERS,
            PortalScreen.DELIVERIES,
            PortalScreen.PROFILE
        )

        primaryScreens.forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (screen == PortalScreen.INVOICES && unpaidCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("$unpaidCount")
                                }
                            } else if (screen == PortalScreen.ORDERS && cartCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("$cartCount")
                                }
                            } else if (screen == PortalScreen.DELIVERIES && deliveryCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                    Text("$deliveryCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.title
                        )
                    }
                },
                label = { Text(screen.title) }
            )
        }
    }
}
