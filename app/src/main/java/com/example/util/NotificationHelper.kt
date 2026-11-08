package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DeliveryStatus

object NotificationHelper {
    const val CHANNEL_DELIVERIES_ID = "channel_delivery_updates"
    const val CHANNEL_INVOICES_ID = "channel_invoice_alerts"

    const val EXTRA_NAV_SCREEN = "extra_target_screen"
    const val EXTRA_TARGET_ID = "extra_target_id"

    const val SCREEN_DELIVERIES = "DELIVERIES"
    const val SCREEN_INVOICES = "INVOICES"

    /**
     * Creates system notification channels required for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Delivery Updates Channel
            val deliveryChannel = NotificationChannel(
                CHANNEL_DELIVERIES_ID,
                "Delivery Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts for shipment dispatch, driver transit, and delivery completion."
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Invoices & Billing Channel
            val invoiceChannel = NotificationChannel(
                CHANNEL_INVOICES_ID,
                "Invoices & Billing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for newly generated invoices, statement updates, and payment reminders."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(deliveryChannel)
            notificationManager.createNotificationChannel(invoiceChannel)
        }
    }

    /**
     * Checks if the app has permission to post notifications (required on Android 13+ / API 33).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends a local notification when a delivery status changes (e.g. Processing -> Shipped -> Out for Delivery -> Delivered).
     */
    fun sendDeliveryStatusNotification(
        context: Context,
        orderId: String,
        trackingNumber: String,
        newStatus: DeliveryStatus,
        driverName: String,
        eta: String,
        address: String
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_SCREEN, SCREEN_DELIVERIES)
            putExtra(EXTRA_TARGET_ID, orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusLabel = newStatus.name.replace("_", " ")
        val contentTitle = "Delivery Update: $orderId"
        val contentText = "Status changed to $statusLabel • ETA: $eta"

        val bigText = StringBuilder().apply {
            append("Order $orderId ($trackingNumber) is now $statusLabel.")
            if (driverName.isNotBlank()) {
                append("\nDriver: $driverName")
            }
            if (eta.isNotBlank()) {
                append("\nEstimated Arrival: $eta")
            }
            if (address.isNotBlank()) {
                append("\nDestination: $address")
            }
        }.toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_DELIVERIES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                "Track Delivery",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Handled gracefully if permission revoked concurrently
        }
    }

    /**
     * Sends a local notification when a new invoice is generated.
     */
    fun sendNewInvoiceNotification(
        context: Context,
        invoiceNumber: String,
        amount: Double,
        dueDate: String,
        itemsSummary: String,
        description: String = ""
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_SCREEN, SCREEN_INVOICES)
            putExtra(EXTRA_TARGET_ID, invoiceNumber)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            invoiceNumber.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = String.format("%.2f", amount)
        val contentTitle = "New Invoice Generated: $invoiceNumber"
        val contentText = "Amount: \$$formattedAmount • Due: $dueDate"

        val bigText = StringBuilder().apply {
            append("A new invoice ($invoiceNumber) has been generated for your account.\n")
            append("Total Amount: \$$formattedAmount\n")
            append("Due Date: $dueDate\n")
            if (itemsSummary.isNotBlank()) {
                append("Items: $itemsSummary\n")
            }
            if (description.isNotBlank()) {
                append("Details: $description")
            }
        }.toString().trim()

        val notification = NotificationCompat.Builder(context, CHANNEL_INVOICES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification,
                "View & Pay",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(invoiceNumber.hashCode(), notification)
        } catch (_: SecurityException) {
            // Handled gracefully if permission revoked concurrently
        }
    }
}
