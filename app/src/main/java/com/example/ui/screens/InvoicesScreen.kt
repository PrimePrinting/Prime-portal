package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    invoices: List<InvoiceEntity>,
    onPayInvoice: (InvoiceEntity) -> Unit,
    onRevertPayment: ((InvoiceEntity) -> Unit)? = null,
    onOpenGenerateInvoiceDialog: () -> Unit = {},
    onSimulateInvoiceNotification: () -> Unit = {}
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
        // Compact Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                // Top Row: Title + Total Balance Tag + New Invoice Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Invoices & Payments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Outstanding: \$${String.format("%.2f", totalUnpaid)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = onSimulateInvoiceNotification,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("simulate_invoice_notif_button")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Test Alert", modifier = Modifier.size(16.dp))
                        }

                        Button(
                            onClick = onOpenGenerateInvoiceDialog,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("open_generate_invoice_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search & Filters Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by invoice # or items...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Tabs as compact chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UNPAID", "OVERDUE", "PAID", "ALL").forEach { tab ->
                        FilterChip(
                            selected = selectedFilterTab == tab,
                            onClick = { selectedFilterTab = tab },
                            label = { Text(tab, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredInvoices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No invoices match your criteria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredInvoices) { invoice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        invoice.invoiceNumber,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        "Due: ${invoice.dueDate} • Issued: ${invoice.issueDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
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
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = when (invoice.status) {
                                            InvoiceStatus.PAID -> MaterialTheme.colorScheme.onTertiaryContainer
                                            InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                invoice.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

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
                                            "Paid on ${invoice.paymentDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { viewPdfInvoice = invoice },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("PDF", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                                    }

                                    if (invoice.status == InvoiceStatus.UNPAID || invoice.status == InvoiceStatus.OVERDUE) {
                                        Button(
                                            onClick = { onPayInvoice(invoice) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Pay", fontSize = 11.sp)
                                        }
                                    } else if (invoice.status == InvoiceStatus.PAID && onRevertPayment != null) {
                                        OutlinedButton(
                                            onClick = { onRevertPayment(invoice) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Revert", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Invoice Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewPdfInvoice = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Text("Invoice No: ${inv.invoiceNumber}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Issue Date: ${inv.issueDate} • Due: ${inv.dueDate}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Itemized Breakdown:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text(inv.itemsSummary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax:", style = MaterialTheme.typography.bodySmall)
                        Text("\$${String.format("%.2f", inv.tax)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", fontWeight = FontWeight.Bold)
                        Text("\$${String.format("%.2f", inv.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    // Digital Authenticity QR Code Stamp
                    val qrPayload = remember(inv) {
                        QrCodeGenerator.createPayload("INVOICE", inv.invoiceNumber, inv.issueDate, inv.amount.toString())
                    }
                    val qrBitmap = remember(qrPayload) {
                        QrCodeGenerator.generateQrBitmap(qrPayload, sizePx = 256)
                    }

                    if (qrBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Verification QR Code",
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(4.dp))
                                    .padding(2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Digital Authenticity Stamp",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Cryptographically certified. Scan via Document Verification Scanner to verify.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewPdfInvoice = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF")
                    }
                }
            }
        }
    }
}
