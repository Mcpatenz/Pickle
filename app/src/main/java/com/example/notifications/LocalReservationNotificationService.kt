package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.BookingEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReservationStatusLocalAlert(
    val id: Int,
    val bookingId: Int,
    val bookingCode: String,
    val playerName: String,
    val facilityName: String,
    val courtName: String,
    val dateLabel: String,
    val timeRangeLabel: String,
    val previousStatus: String,
    val newStatus: String,
    val title: String,
    val message: String,
    val timestampLabel: String = "Just now",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

object LocalReservationNotificationService {
    const val CHANNEL_ID = "pickleplay_reservation_status_channel"
    const val CHANNEL_NAME = "Reservation Status Updates"
    const val CHANNEL_DESCRIPTION = "Local notifications alerting customers when their court reservation status changes (e.g., Pending to Approved, Verified, Paid, or Completed)."

    private var nextNotificationId = 4100

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _alerts = MutableStateFlow<List<ReservationStatusLocalAlert>>(
        listOf(
            ReservationStatusLocalAlert(
                id = 4099,
                bookingId = 3,
                bookingCode = "PKL-20260928-00142",
                playerName = "Jonel P.",
                facilityName = "Smash Pickle Club",
                courtName = "Court 4",
                dateLabel = "Monday, Sep 28, 2026",
                timeRangeLabel = "4:00 PM - 5:00 PM",
                previousStatus = "Pending",
                newStatus = "Approved",
                title = "Reservation Status Updated: Pending → Approved",
                message = "Your reservation #PKL-20260928-00142 for Court 4 (Monday, Sep 28, 2026 • 4:00 PM - 5:00 PM) is now Approved! Upload your payment receipt to get verified.",
                timestampLabel = "Recent",
                isRead = false
            )
        )
    )
    val alerts: StateFlow<List<ReservationStatusLocalAlert>> = _alerts.asStateFlow()

    private val _latestAlert = MutableStateFlow<ReservationStatusLocalAlert?>(null)
    val latestAlert: StateFlow<ReservationStatusLocalAlert?> = _latestAlert.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        if (!enabled) {
            _latestAlert.value = null
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun mapStatusToDisplayLabel(rawStatus: String, paymentStatus: String = ""): String {
        return when (rawStatus.uppercase()) {
            "PENDING_ADMIN_APPROVAL", "PENDING" -> "Pending"
            "APPROVED_AWAITING_PAYMENT", "APPROVED" -> "Approved"
            "PENDING_CASHIER_VERIFICATION", "VERIFIED" -> "Verified"
            "CONFIRMED", "PAID" -> "Paid"
            "CHECKED_IN", "COMPLETED" -> "Completed"
            "CANCELLED", "DECLINED" -> "Cancelled"
            else -> if (paymentStatus.equals("PAID", ignoreCase = true)) "Paid" else "Pending"
        }
    }

    fun notifyReservationStatusChanged(
        context: Context?,
        booking: BookingEntity,
        previousStatus: String,
        newStatus: String,
        customTitle: String? = null,
        customMessage: String? = null
    ): ReservationStatusLocalAlert {
        val prevLabel = mapStatusToDisplayLabel(previousStatus)
        val nextLabel = mapStatusToDisplayLabel(newStatus, booking.paymentStatus)

        val resolvedTitle = customTitle
            ?: "Reservation Status Updated: $prevLabel → $nextLabel"

        val resolvedMessage = customMessage ?: when (nextLabel) {
            "Approved" ->
                "Great news, ${booking.playerName}! Booking #${booking.bookingCode} for ${booking.courtName} (${booking.dateLabel} • ${booking.timeRangeLabel}) moved from $prevLabel to Approved. Please complete your ₱${booking.totalAmount} payment."
            "Verified" ->
                "Booking #${booking.bookingCode} for ${booking.courtName} moved from $prevLabel to Verified. Cashier is reviewing your ₱${booking.totalAmount} ${booking.paymentMethod} payment."
            "Paid" ->
                "Payment confirmed for #${booking.bookingCode}! Status updated from $prevLabel to Paid. Your Digital QR Pass for ${booking.courtName} is now unlocked."
            "Completed" ->
                "Check-in verified for #${booking.bookingCode} at ${booking.courtName}! Status updated from $prevLabel to Completed. Enjoy your game!"
            "Cancelled" ->
                "Reservation #${booking.bookingCode} for ${booking.courtName} (${booking.dateLabel}) status changed from $prevLabel to Cancelled."
            else ->
                "Reservation #${booking.bookingCode} for ${booking.courtName} (${booking.dateLabel} • ${booking.timeRangeLabel}) updated from $prevLabel to $nextLabel."
        }

        val alertId = nextNotificationId++
        val alert = ReservationStatusLocalAlert(
            id = alertId,
            bookingId = booking.id,
            bookingCode = booking.bookingCode,
            playerName = booking.playerName,
            facilityName = booking.facilityName,
            courtName = booking.courtName,
            dateLabel = booking.dateLabel,
            timeRangeLabel = booking.timeRangeLabel,
            previousStatus = prevLabel,
            newStatus = nextLabel,
            title = resolvedTitle,
            message = resolvedMessage,
            timestampLabel = "Just now",
            isRead = false
        )

        _alerts.value = listOf(alert) + _alerts.value.take(19)
        if (_notificationsEnabled.value) {
            _latestAlert.value = alert
            if (context != null) {
                postSystemNotification(context, alert)
            }
        }
        return alert
    }

    private fun postSystemNotification(
        context: Context,
        alert: ReservationStatusLocalAlert
    ) {
        try {
            ensureNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("booking_code", alert.bookingCode)
                putExtra("new_status", alert.newStatus)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                alert.id,
                launchIntent,
                pendingIntentFlags
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(alert.message)
                        .setBigContentTitle(alert.title)
                        .setSummaryText("${alert.facilityName} • ${alert.courtName} (${alert.previousStatus} → ${alert.newStatus})")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(alert.id, notification)
        } catch (_: SecurityException) {
            // Permission not yet granted on Android 13+; in-app notification still active
        } catch (_: Throwable) {
            // Ignore any platform-specific notification manager edge cases
        }
    }

    fun dismissLatestAlert() {
        _latestAlert.value = null
    }

    fun markAllAlertsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }

    fun clearAlerts() {
        _alerts.value = emptyList()
        _latestAlert.value = null
    }
}

@Composable
fun statusPillColors(statusLabel: String): Pair<Color, Color> {
    return when (statusLabel.uppercase()) {
        "PENDING" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "APPROVED" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "VERIFIED" -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        "PAID" -> Color(0xFFD1FAE5) to Color(0xFF047857)
        "COMPLETED" -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        "CANCELLED" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        else -> Color(0xFFF1F5F9) to Color(0xFF334155)
    }
}

@Composable
fun StatusTransitionBadge(
    previousStatus: String,
    newStatus: String,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val (prevBg, prevFg) = statusPillColors(previousStatus)
    val (nextBg, nextFg) = statusPillColors(newStatus)

    Row(
        modifier = modifier.then(
            if (testTag != null) Modifier.testTag(testTag) else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = prevBg,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, prevFg.copy(alpha = 0.3f))
        ) {
            Text(
                text = previousStatus,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = prevFg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "to",
            tint = Color(0xFF64748B),
            modifier = Modifier.size(13.dp)
        )
        Surface(
            color = nextBg,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, nextFg.copy(alpha = 0.45f))
        ) {
            Text(
                text = newStatus,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = nextFg,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun LocalReservationStatusHeadsUpBanner(
    alert: ReservationStatusLocalAlert,
    onViewReservations: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("local_reservation_heads_up_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Local Notification Alert",
                                tint = OpticVolt,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "LOCAL NOTIFICATION • STATUS UPDATE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpticVolt
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("local_notification_banner_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification banner",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusTransitionBadge(
                    previousStatus = alert.previousStatus,
                    newStatus = alert.newStatus,
                    testTag = "local_notification_banner_transition"
                )
                Text(
                    text = "#${alert.bookingCode}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("local_notification_banner_title")
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("local_notification_banner_message")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onDismiss()
                        onViewReservations()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("local_notification_banner_view_button")
                ) {
                    Text(
                        text = "View Reservation",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerLocalNotificationCenterCard(
    bookings: List<BookingEntity>,
    onSimulateStatusUpdate: (BookingEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alerts by LocalReservationNotificationService.alerts.collectAsState()
    val notificationsEnabled by LocalReservationNotificationService.notificationsEnabled.collectAsState()

    var hasOsPermission by remember {
        mutableStateOf(LocalReservationNotificationService.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasOsPermission = granted
        LocalReservationNotificationService.setNotificationsEnabled(granted)
        if (granted) {
            LocalReservationNotificationService.ensureNotificationChannel(context)
        }
    }

    val pendingCandidate = remember(bookings) {
        bookings.firstOrNull {
            it.status.equals("PENDING_ADMIN_APPROVAL", ignoreCase = true) ||
                it.status.equals("PENDING", ignoreCase = true)
        } ?: bookings.firstOrNull()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_local_notification_center_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFD1FAE5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (notificationsEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (notificationsEnabled) {
                                    Icons.Default.NotificationsActive
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = "Local Notification Alerts",
                                tint = if (notificationsEnabled) Color(0xFF047857) else Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Local Reservation Status Alerts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                color = if (notificationsEnabled) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = if (notificationsEnabled) "ACTIVE" else "MUTED",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (notificationsEnabled) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                        .testTag("local_notification_status_pill")
                                )
                            }
                        }
                        Text(
                            text = "Instant Android local notifications when bookings change (e.g. Pending → Approved)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasOsPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            LocalReservationNotificationService.ensureNotificationChannel(context)
                            LocalReservationNotificationService.setNotificationsEnabled(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier.testTag("local_notification_enable_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row to test/trigger status update notification (Pending -> Approved)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        LocalReservationNotificationService.ensureNotificationChannel(context)
                        onSimulateStatusUpdate(pendingCandidate)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF047857),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_status_update_notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notify Status Update (Pending → Approved)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasOsPermission) {
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GCashBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("request_post_notifications_permission_button")
                    ) {
                        Text(
                            text = "Allow OS Alerts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GCashBlue
                        )
                    }
                }
            }

            if (alerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT RESERVATION STATUS ALERTS (${alerts.size})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Mark All Read",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GCashBlue,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { LocalReservationNotificationService.markAllAlertsRead() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("mark_all_local_alerts_read_button")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    alerts.take(3).forEachIndexed { index, alert ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_status_alert_item_$index"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (!alert.isRead) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (!alert.isRead) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusTransitionBadge(
                                        previousStatus = alert.previousStatus,
                                        newStatus = alert.newStatus,
                                        testTag = "local_status_alert_transition_$index"
                                    )
                                    Text(
                                        text = "${alert.bookingCode} • ${alert.timestampLabel}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.testTag("local_status_alert_title_$index")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.testTag("local_status_alert_body_$index")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
