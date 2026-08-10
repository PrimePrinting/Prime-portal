package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    invoices: List<InvoiceEntity>,
    onPayInvoice: (InvoiceEntity) -> Unit,
    onRevertPayment: ((InvoiceEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("UNPAID") }
    var viewPdfInvoice by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filteredInvoices = invoices.filter { invoice ->
        val matchesTab = when (selectedFilterTab) {
            "UNPAID" -> invoice.status == InvoiceStatus.UNPAID || invoice.status == InvoiceStatus.OVERDUE
            "PAID" -> invoice.status == InvoiceStatus.PAID
            "OVERDUE" -> invoice.status == InvoiceStatus.OVERDUE
            else -> true
        }
        val matchesSearch = invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                invoice.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                invoice.description.contains(searchQuery, ignoreCase = true)
        matchesTab && matchesSearch
    }

    val totalUnpaid = invoices.filter { it.status == InvoiceStatus.UNPAID || it.status == InvoiceStatus.OVERDUE }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("invoices_screen")
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Invoices & Payments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Balance summary card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Outstanding Balance", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "\$${String.format("%.2f", totalUnpaid)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by invoice # or items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Tabs
                ScrollableTabRow(
                    selectedTabIndex = listOf("UNPAID", "OVERDUE", "PAID", "ALL").indexOf(selectedFilterTab),
                    edgePadding = 0.dp
                ) {
                    listOf("UNPAID", "OVERDUE", "PAID", "ALL").forEach { tab ->
                        Tab(
                            selected = selectedFilterTab == tab,
                            onClick = { selectedFilterTab = tab },
                            text = { Text(tab) }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (filteredInvoices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No invoices match your criteria.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredInvoices) { invoice ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        invoice.invoiceNumber,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Issue Date: ${invoice.issueDate} | Due: ${invoice.dueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (invoice.status) {
                                        InvoiceStatus.PAID -> MaterialTheme.colorScheme.tertiaryContainer
                                        InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        invoice.status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = when (invoice.status) {
                                            InvoiceStatus.PAID -> MaterialTheme.colorScheme.onTertiaryContainer
                                            InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                invoice.itemsSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (invoice.description.isNotEmpty()) {
                                Text(
                                    invoice.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "\$${String.format("%.2f", invoice.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (invoice.paymentDate != null) {
                                        Text(
                                            "Paid on ${invoice.paymentDate} (${invoice.paymentMethod})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewPdfInvoice = invoice },
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDF")
                                    }

                                    if (invoice.status == InvoiceStatus.UNPAID || invoice.status == InvoiceStatus.OVERDUE) {
                                        Button(
                                            onClick = { onPayInvoice(invoice) },
                                            contentPadding = PaddingValues(horizontal = 16.dp)
                                        ) {
                                            Text("Pay Now")
                                        }
                                    } else if (invoice.status == InvoiceStatus.PAID && onRevertPayment != null) {
                                        OutlinedButton(
                                            onClick = { onRevertPayment(invoice) },
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Revert Payment")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice PDF Preview Dialog
    if (viewPdfInvoice != null) {
        val inv = viewPdfInvoice!!
        Dialog(onDismissRequest = { viewPdfInvoice = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Official Invoice PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewPdfInvoice = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Invoice No: ${inv.invoiceNumber}", fontWeight = FontWeight.SemiBold)
                    Text("Issue Date: ${inv.issueDate}")
                    Text("Due Date: ${inv.dueDate}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Itemized Breakdown:", fontWeight = FontWeight.Bold)
                    Text(inv.itemsSummary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax:")
                        Text("\$${String.format("%.2f", inv.tax)}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", fontWeight = FontWeight.Bold)
                        Text("\$${String.format("%.2f", inv.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewPdfInvoice = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download PDF to Device")
                    }
                }
            }
        }
    }
}
