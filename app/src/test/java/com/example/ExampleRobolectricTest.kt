package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Customer Portal", appName)
  }

  @Test
  fun `notification channels created successfully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.NotificationHelper.createNotificationChannels(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val deliveryChannel = notificationManager.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_DELIVERIES_ID)
    val invoiceChannel = notificationManager.getNotificationChannel(com.example.util.NotificationHelper.CHANNEL_INVOICES_ID)
    org.junit.Assert.assertNotNull(deliveryChannel)
    org.junit.Assert.assertNotNull(invoiceChannel)
    assertEquals("Delivery Updates", deliveryChannel.name)
    assertEquals("Invoices & Billing", invoiceChannel.name)
  }
}
