package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example. R
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.NotificationEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.CardIndigo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.GCashBlueBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MayaMint
import com.example.ui.theme.MayaMintBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.BookingDateOption
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.BusinessSettingsLocalStore
import com.example.viewmodel.MockEmailNotificationService
import com.example.viewmodel.ReservationDurationOption
import com.example.viewmodel.ReservationEmailSummary
import com.example.viewmodel.TimeSlotOption
import java.io.File
import kotlin.math.abs

@Composable
fun FacilityHeroDrawableRes(imageKey: String): Int {
    return when (imageKey) {
        "indoor_hero" -> R.drawable.img_hero_court_1790412087151
        "outdoor_club" -> R.drawable.img_outdoor_club_1790412102314
        "tournament_arena" -> R.drawable.img_tournament_banner_1790412115656
        else -> R.drawable.img_hero_court_1790412087151
    }
}

@Composable
fun BookingCheckoutDialog(
    facility: FacilityEntity,
    court: CourtEntity,
    dateOption: BookingDateOption,
    timeSlot: TimeSlotOption,
    lockSecondsRemaining: Int,
    courtFee: Int,
    userProfile: UserProfileEntity?,
    promoCodeInput: String,
    appliedPromoDiscount: Int,
    selectedPaymentMethod: String,
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    isWalkInMode: Boolean = false,
    onToggleWalkInMode: (Boolean) -> Unit = {},
    onPromoCodeChange: (String) -> Unit,
    onPaymentMethodSelect: (String) -> Unit,
    onConfirmReservation: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var localWalkInMode by remember(isWalkInMode) { mutableStateOf(isWalkInMode) }
    var activePaymentMethod by remember(selectedPaymentMethod, localWalkInMode) {
        val initial = if (localWalkInMode) {
            "Cash on Hand"
        } else {
            when {
                selectedPaymentMethod.equals("Maya", ignoreCase = true) ||
                    selectedPaymentMethod.equals("PayMaya", ignoreCase = true) -> "PayMaya"
                selectedPaymentMethod.equals("GCash", ignoreCase = true) -> "GCash"
                selectedPaymentMethod.equals("Cash on Hand", ignoreCase = true) ||
                    selectedPaymentMethod.equals("Pay at Venue", ignoreCase = true) -> "Cash on Hand"
                else -> "GCash"
            }
        }
        mutableStateOf(initial)
    }
    val effectiveBusinessSettings = remember(businessSettings, activePaymentMethod) {
        BusinessSettingsLocalStore.resolveSettings(context, businessSettings)
    }
    val memberDiscountPct = userProfile?.discountPercent ?: 0
    val memberDiscountAmt = (courtFee * memberDiscountPct) / 100
    val totalDiscount = (memberDiscountAmt + appliedPromoDiscount).coerceAtMost(courtFee - 50)
    val serviceFee = 20
    val finalTotal = (courtFee - totalDiscount) + serviceFee
    var confirmedCheckoutEmail by remember { mutableStateOf<ReservationEmailSummary?>(null) }

    val mins = lockSecondsRemaining / 60
    val secs = lockSecondsRemaining % 60
    val lockFormatted = "%02d:%02d".format(mins, secs)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Booking Summary",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (localWalkInMode) {
                                "Walk-In Booking • Cash on Hand at Cashier"
                            } else {
                                "Admin Approval → Upload Receipt & Ref # → Cashier Verify → QR Pass"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_checkout_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close checkout")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Walk-In vs Online Booking Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!localWalkInMode) GCashBlue else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (!localWalkInMode) OpticVolt else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                localWalkInMode = false
                                onToggleWalkInMode(false)
                            }
                            .testTag("checkout_mode_online_booking")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Online Booking",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (!localWalkInMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Admin Approve → Receipt Upload",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (!localWalkInMode) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (localWalkInMode) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (localWalkInMode) OpticVolt else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                localWalkInMode = true
                                activePaymentMethod = "Cash on Hand"
                                onPaymentMethodSelect("Cash on Hand")
                                onToggleWalkInMode(true)
                            }
                            .testTag("checkout_mode_walk_in_booking")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Walk-In Booking",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (localWalkInMode) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Cash on Hand at Cashier",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (localWalkInMode) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                confirmedCheckoutEmail?.let { emailSummary ->
                    MockEmailNotificationSummaryCard(
                        summary = emailSummary,
                        onResend = {
                            confirmedCheckoutEmail = MockEmailNotificationService.resendEmail(emailSummary)
                        },
                        onDismiss = { confirmedCheckoutEmail = null }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Real-time temporary slot lock banner
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PeakAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockClock,
                            contentDescription = "Slot Locked",
                            tint = PeakAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Slot temporarily locked for you",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "Prevents double-booking while paying",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                        Text(
                            text = lockFormatted,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Court & Schedule Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = facility.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${court.name} • ${court.courtType}",
                            style = MaterialTheme.typography.titleSmall,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateOption.fullLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = timeSlot.rangeLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Promo Code Input
                OutlinedTextField(
                    value = promoCodeInput,
                    onValueChange = onPromoCodeChange,
                    label = { Text("Promo Code (Try PICKLE50 or FIRSTDINK)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("promo_code_input"),
                    trailingIcon = {
                        if (appliedPromoDiscount > 0) {
                            Text(
                                text = "-₱$appliedPromoDiscount",
                                color = AvailableGreen,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fee Breakdown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeeRow(label = "Court Fee (${timeSlot.periodTier})", amount = "₱$courtFee")
                    if (memberDiscountAmt > 0) {
                        FeeRow(
                            label = "${userProfile?.membershipTier} Member Discount ($memberDiscountPct%)",
                            amount = "-₱$memberDiscountAmt",
                            highlightColor = AvailableGreen
                        )
                    }
                    if (appliedPromoDiscount > 0) {
                        FeeRow(
                            label = "Promo Code (${promoCodeInput.uppercase()})",
                            amount = "-₱$appliedPromoDiscount",
                            highlightColor = AvailableGreen
                        )
                    }
                    FeeRow(label = "Service Fee", amount = "₱$serviceFee")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₱$finalTotal",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = EmeraldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode of Payment Selection (GCash, PayMaya, Pay at Venue — Credit/Debit Card removed)
                Text(
                    text = "Mode of Payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val paymentOptions = listOf(
                    PaymentOptionSpec("GCash", "Scan GCash QR Code • Instant PH e-Wallet", GCashBlue, GCashBlueBg, "GCASH"),
                    PaymentOptionSpec("PayMaya", "Scan PayMaya QR Code • Instant Maya Wallet", MayaMint, MayaMintBg, "PAYMAYA"),
                    PaymentOptionSpec("Pay at Venue", "Cash payment at club reception", EmeraldPrimary, AvailableGreenBg, "VENUE")
                )

                paymentOptions.forEach { option ->
                    val isSelected = activePaymentMethod == option.name
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                activePaymentMethod = option.name
                                onPaymentMethodSelect(option.name)
                            }
                            .testTag("payment_option_${option.badge}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) option.bgTint.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) option.accentColor else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    activePaymentMethod = option.name
                                    onPaymentMethodSelect(option.name)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = option.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = option.accentColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = option.badge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Display GCash or PayMaya QR Code retrieved from local settings when selected
                if (activePaymentMethod == "GCash" || activePaymentMethod == "PayMaya") {
                    Spacer(modifier = Modifier.height(12.dp))
                    PaymentQrCodeDisplayCard(
                        paymentMethod = activePaymentMethod,
                        amount = finalTotal,
                        businessSettings = effectiveBusinessSettings,
                        tagPrefix = "checkout"
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_venue_business_info_card"),
                        shape = RoundedCornerShape(14.dp),
                        color = AvailableGreenBg.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, EmeraldPrimary)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Pay at Counter • ${effectiveBusinessSettings.businessName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "${effectiveBusinessSettings.businessAddress} • ${effectiveBusinessSettings.contactNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val playerDisplayName = userProfile?.fullName ?: "Jonel P."
                        confirmedCheckoutEmail = MockEmailNotificationService.sendReservationConfirmationEmail(
                            recipientName = playerDisplayName,
                            facilityName = effectiveBusinessSettings.businessName.ifBlank { facility.name },
                            facilityAddress = effectiveBusinessSettings.businessAddress.ifBlank { facility.address },
                            contactNumber = effectiveBusinessSettings.contactNumber,
                            courtName = court.name,
                            courtType = court.courtType,
                            dateLabel = dateOption.fullLabel,
                            durationLabel = "1 Hour",
                            timeRangeLabel = timeSlot.rangeLabel,
                            paymentMethod = activePaymentMethod,
                            courtFee = courtFee,
                            serviceFee = serviceFee,
                            discountAmount = totalDiscount,
                            totalAmount = finalTotal
                        )
                        onConfirmReservation()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_reservation_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Confirm Reservation • ₱$finalTotal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentQrCodeDisplayCard(
    paymentMethod: String,
    amount: Int,
    businessSettings: BusinessPaymentSettings,
    tagPrefix: String = "payment",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storedQr = remember(paymentMethod, businessSettings) {
        BusinessSettingsLocalStore.getStoredQrCode(
            context = context,
            paymentMethod = paymentMethod,
            fallback = businessSettings
        )
    }
    val isGcash = storedQr.paymentMethod.equals("GCash", ignoreCase = true)
    val accentColor = if (isGcash) GCashBlue else MayaMint
    val bgTint = if (isGcash) GCashBlueBg else MayaMintBg
    val methodLabel = storedQr.paymentMethod
    val qrBitmap = remember(storedQr.bitmap) { storedQr.bitmap.asImageBitmap() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_qr_code_card"),
        shape = RoundedCornerShape(18.dp),
        color = bgTint.copy(alpha = 0.45f),
        border = BorderStroke(1.5.dp, accentColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = accentColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SCAN TO PAY VIA ${methodLabel.uppercase()} QR",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("${tagPrefix}_qr_header_badge")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = storedQr.businessName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("${tagPrefix}_business_name_text")
            )
            Text(
                text = storedQr.businessAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("${tagPrefix}_business_address_text")
            )
            Text(
                text = storedQr.contactNumber,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("${tagPrefix}_business_details_text")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .testTag("${tagPrefix}_${methodLabel.lowercase()}_qr_box"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "$methodLabel QR Code",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("${tagPrefix}_${methodLabel.lowercase()}_qr_image")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Amount to Pay: ₱$amount",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = accentColor
            )
            Text(
                text = storedQr.qrFileName,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier.testTag("${tagPrefix}_qr_file_name_text")
            )
            if (!storedQr.qrUri.isNullOrBlank()) {
                Text(
                    text = storedQr.qrUri,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("${tagPrefix}_qr_uri_text")
                )
            }
            Text(
                text = if (storedQr.isUploaded) {
                    "✓ Official $methodLabel QR Retrieved from Local Settings (${storedQr.contactNumber})"
                } else {
                    "$methodLabel Account: ${storedQr.contactNumber} • Scan QR in your $methodLabel app"
                },
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class PaymentOptionSpec(
    val name: String,
    val subtitle: String,
    val accentColor: Color,
    val bgTint: Color,
    val badge: String
)

@Composable
private fun FeeRow(
    label: String,
    amount: String,
    highlightColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = highlightColor
        )
    }
}

@Composable
fun MockEmailNotificationSummaryCard(
    summary: ReservationEmailSummary,
    onResend: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("email_notification_summary_card"),
        shape = RoundedCornerShape(18.dp),
        color = AvailableGreenBg.copy(alpha = 0.72f),
        border = BorderStroke(1.5.dp, AvailableGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EmeraldPrimary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("email_notification_status_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Sent",
                            tint = OpticVolt,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOCK EMAIL SERVICE • ${summary.status}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onResend != null) {
                        Text(
                            text = "Resend",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = GCashBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onResend() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("email_notification_resend_button")
                        )
                    }
                    if (onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("email_notification_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss email notification",
                                tint = EmeraldDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Reservation Confirmation Email Sent ✓",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldDark
            )

            Text(
                text = "To: ${summary.recipientName} <${summary.recipientEmail}>",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GCashBlue,
                modifier = Modifier.testTag("email_notification_recipient")
            )

            Text(
                text = "Subject: ${summary.subject}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldDark,
                modifier = Modifier.testTag("email_notification_subject")
            )

            HorizontalDivider(color = AvailableGreen.copy(alpha = 0.35f))

            // Structured Reservation Details Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "RESERVATION DETAILS SUMMARY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                Text(
                    text = "Booking Code: ${summary.bookingCode}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F1D18),
                    modifier = Modifier.testTag("email_summary_booking_code")
                )
                Text(
                    text = "Player: ${summary.recipientName} (${summary.recipientEmail})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF0F1D18),
                    modifier = Modifier.testTag("email_summary_player_name")
                )
                Text(
                    text = "Club: ${summary.facilityName} • ${summary.facilityAddress} • ${summary.contactNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF4A5D54),
                    modifier = Modifier.testTag("email_summary_facility")
                )
                Text(
                    text = "Court: ${summary.courtName} (${summary.courtType})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldDark,
                    modifier = Modifier.testTag("email_summary_court")
                )
                Text(
                    text = "Schedule: ${summary.dateLabel} • ${summary.timeRangeLabel} (${summary.durationLabel})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF0F1D18),
                    modifier = Modifier.testTag("email_summary_schedule")
                )
                Text(
                    text = "Payment: ${summary.paymentMethod} (${summary.paymentStatus})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GCashBlue,
                    modifier = Modifier.testTag("email_summary_payment")
                )
                Text(
                    text = "Total Paid: ₱${summary.totalAmount} (Court ₱${summary.courtFee} + Service ₱${summary.serviceFee})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.testTag("email_summary_total")
                )
            }

            Text(
                text = summary.bodyPreview,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Color(0xFF334E43),
                modifier = Modifier.testTag("email_notification_body")
            )
        }
    }
}

@Composable
fun DigitalBookingPassDialog(
    booking: BookingEntity,
    emailSummary: ReservationEmailSummary? = null,
    onAddToCalendar: () -> Unit,
    onSimulateQrCheckIn: (BookingEntity) -> Unit,
    onCancelBooking: (BookingEntity, Int) -> Unit,
    onSubmitRating: (BookingEntity, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCancelOptions by remember { mutableStateOf(false) }
    var showCameraXScanner by remember { mutableStateOf(false) }
    var lastScanOutcome by remember { mutableStateOf<QrPassScanOutcome?>(null) }
    var selectedStars by remember { mutableIntStateOf(booking.userRating ?: 5) }
    var reviewComment by remember { mutableStateOf(booking.userReview ?: "") }
    val resolvedEmailSummary = emailSummary ?: MockEmailNotificationService.latestSentEmail.value

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF07281C),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PICKLEPLAY PASS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OpticVolt,
                        letterSpacing = 1.5.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Pass",
                            tint = Color.White
                        )
                    }
                }

                // Confirmed Status Pill
                val isCheckedIn = booking.status == "CHECKED_IN"
                val isCancelled = booking.status == "CANCELLED"
                Surface(
                    color = when {
                        isCancelled -> MaintenanceRed
                        isCheckedIn -> OpticVolt
                        else -> AvailableGreen
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isCheckedIn) OpticVoltDarkText else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isCancelled -> "BOOKING CANCELLED"
                                isCheckedIn -> "CHECKED IN AT ${booking.checkInTime ?: "1:47 PM"} ✓"
                                else -> "BOOKING CONFIRMED ✓"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isCheckedIn) OpticVoltDarkText else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                resolvedEmailSummary?.let { summary ->
                    MockEmailNotificationSummaryCard(
                        summary = summary,
                        onResend = { MockEmailNotificationService.resendEmail(summary) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // White Inner Ticket Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = booking.facilityName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4A5D54)
                        )
                        Text(
                            text = booking.courtName,
                            style = MaterialTheme.typography.displayMedium,
                            color = EmeraldDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = booking.dateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF0F1D18)
                        )
                        Text(
                            text = booking.timeRangeLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EmeraldPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Perforated Ticket Divider
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        ) {
                            drawLine(
                                color = Color(0xFFDCE3DD),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Deterministic Scannable QR Code Matrix
                        Box(
                            modifier = Modifier
                                .size(176.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(2.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrCodeMatrixCanvas(
                                seedString = booking.bookingCode,
                                modifier = Modifier.size(148.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Booking #${booking.bookingCode}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F1D18)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Player: ${booking.playerName} • ${booking.paymentMethod} (${booking.paymentStatus})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4A5D54),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please show this QR code at the reception.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddToCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_to_calendar_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticVolt),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpticVolt)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Calendar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!isCheckedIn && !isCancelled) {
                        Button(
                            onClick = { onSimulateQrCheckIn(booking) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pass_qr_checkin_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OpticVolt,
                                contentColor = OpticVoltDarkText
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Staff Check-In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!isCheckedIn && !isCancelled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showCameraXScanner = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pass_open_camerax_scanner_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Scan Court QR Code with CameraX to Check In",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Post-game Review & Rating Section
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = Color(0xFF123829),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (booking.userRating != null) "Your Court Review (${booking.userRating} ★)" else "Rate Court & Facility Experience",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            for (star in 1..5) {
                                IconButton(
                                    onClick = {
                                        selectedStars = star
                                        onSubmitRating(booking, star, reviewComment)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (star <= selectedStars) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Rate $star stars",
                                        tint = OpticVolt
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancellation & Refund Policy Section
                if (!isCancelled && !isCheckedIn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (showCancelOptions) "Hide Cancellation Policy" else "Need to cancel? View Refund Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA3B8B0),
                        modifier = Modifier
                            .clickable { showCancelOptions = !showCancelOptions }
                            .padding(vertical = 6.dp)
                            .testTag("toggle_cancel_policy_button")
                    )

                    AnimatedVisibility(visible = showCancelOptions) {
                        Surface(
                            color = Color(0xFF1A2E26),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Configurable Cancellation Policy",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• 24+ hours before: 100% refund\n• 12–24 hours: 50% refund\n• <12 hours: No refund",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6F5E6)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onCancelBooking(booking, 26) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, OpticVolt)
                                    ) {
                                        Text("Cancel (>24h 100%)", fontSize = 11.sp, color = OpticVolt)
                                    }
                                    OutlinedButton(
                                        onClick = { onCancelBooking(booking, 16) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, PeakAmber)
                                    ) {
                                        Text("Cancel (12-24h 50%)", fontSize = 11.sp, color = PeakAmber)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCameraXScanner) {
        CustomerCourtQrScannerDialog(
            bookings = listOf(booking),
            lastScanOutcome = lastScanOutcome,
            onScanQrCode = { rawCode ->
                val outcome = resolveCustomerCourtQrCheckIn(rawCode, listOf(booking))
                lastScanOutcome = outcome
                if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                    onSimulateQrCheckIn(outcome.booking)
                }
            },
            onScanReservedCourtBooking = { target ->
                val payload = ZxingQrScannerEngine.encodeCourtCheckInQrPayload(target)
                val outcome = resolveCustomerCourtQrCheckIn(payload, listOf(booking))
                lastScanOutcome = outcome
                if (outcome is QrPassScanOutcome.CheckedInSuccess) {
                    onSimulateQrCheckIn(outcome.booking)
                }
            },
            onDismiss = { showCameraXScanner = false }
        )
    }
}

@Composable
fun QrCodeMatrixCanvas(
    seedString: String,
    modifier: Modifier = Modifier
) {
    val gridSize = 21
    val hash = abs(seedString.hashCode())
    Canvas(modifier = modifier) {
        val cellSize = size.width / gridSize

        fun isFinderPattern(r: Int, c: Int): Boolean {
            val topLeft = r in 0..6 && c in 0..6
            val topRight = r in 0..6 && c in (gridSize - 7) until gridSize
            val bottomLeft = r in (gridSize - 7) until gridSize && c in 0..6
            return topLeft || topRight || bottomLeft
        }

        fun drawFinderSquare(startRow: Int, startCol: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isInnerCore = r in 2..4 && c in 2..4
                    if (isBorder || isInnerCore) {
                        drawRoundRect(
                            color = Color(0xFF062C1E),
                            topLeft = Offset((startCol + c) * cellSize, (startRow + r) * cellSize),
                            size = Size(cellSize * 0.95f, cellSize * 0.95f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }

        drawFinderSquare(0, 0)
        drawFinderSquare(0, gridSize - 7)
        drawFinderSquare(gridSize - 7, 0)

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!isFinderPattern(r, c)) {
                    val bitSeed = (hash xor (r * 31 + c * 17) xor (seedString[(r + c) % seedString.length].code * 13))
                    if (bitSeed % 2 == 0 || (r == 6 && c % 2 == 0) || (c == 6 && r % 2 == 0)) {
                        drawRoundRect(
                            color = Color(0xFF0F1D18),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f),
                            cornerRadius = CornerRadius(1.5f, 1.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    notifications: List<NotificationEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notifications & Alerts",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close notifications")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(420.dp)
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = EmeraldPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = item.category,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.timeAgo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvailableCourtReservationSheet(
    court: CourtEntity,
    facilityName: String,
    defaultPlayerName: String,
    baseHourlyRate: Int,
    durationOptions: List<ReservationDurationOption>,
    selectedPaymentMethod: String = "GCash",
    businessSettings: BusinessPaymentSettings = BusinessPaymentSettings(),
    defaultRecipientEmail: String = "",
    dateLabel: String = "Monday, Sep 28, 2026",
    timeSlotLabel: String = "2:00 PM",
    dateOptions: List<BookingDateOption> = emptyList(),
    selectedDateOption: BookingDateOption? = null,
    onSelectDateOption: (BookingDateOption) -> Unit = {},
    timeSlotOptions: List<TimeSlotOption> = emptyList(),
    selectedTimeSlotOption: TimeSlotOption? = null,
    bookedTimeSlotsForCourt: Set<String> = emptySet(),
    onSelectTimeSlotOption: (TimeSlotOption) -> Unit = {},
    isWalkInBooking: Boolean = false,
    onWalkInModeChange: (Boolean) -> Unit = {},
    onPaymentMethodSelect: (String) -> Unit = {},
    onEmailNotificationSent: (ReservationEmailSummary) -> Unit = {},
    onConfirmReservation: (playerName: String, durationOption: ReservationDurationOption) -> Unit,
    onOpenFullSchedule: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveBusinessSettings = remember(businessSettings) {
        val localStored = BusinessSettingsLocalStore.load(context, fallback = businessSettings)
        if (businessSettings != BusinessPaymentSettings() && businessSettings.lastSavedAt >= localStored.lastSavedAt) {
            businessSettings
        } else {
            localStored
        }
    }
    var playerName by remember(court.id, defaultPlayerName) { mutableStateOf(defaultPlayerName) }
    var recipientEmail by remember(court.id, defaultRecipientEmail) { mutableStateOf(defaultRecipientEmail) }
    var selectedDuration by remember(court.id) {
        mutableStateOf(durationOptions.find { it.id == "1h" } ?: durationOptions.first())
    }
    var localWalkInMode by remember(isWalkInBooking) { mutableStateOf(isWalkInBooking) }
    var localPaymentMethod by remember(selectedPaymentMethod, localWalkInMode) {
        val initial = when {
            localWalkInMode || selectedPaymentMethod.equals("Cash on Hand", ignoreCase = true) -> "Cash on Hand"
            selectedPaymentMethod.equals("Maya", ignoreCase = true) ||
                selectedPaymentMethod.equals("PayMaya", ignoreCase = true) -> "PayMaya"
            selectedPaymentMethod.equals("Pay at Venue", ignoreCase = true) -> "Pay at Venue"
            else -> "GCash"
        }
        mutableStateOf(initial)
    }
    var showNameError by remember { mutableStateOf(false) }
    var confirmedEmailSummary by remember(court.id) { mutableStateOf<ReservationEmailSummary?>(null) }

    val sanitizedCourtType = court.courtType.replace("Championship", "Pro Cushion", ignoreCase = true)
    val courtFee = (baseHourlyRate * selectedDuration.hoursMultiplier).toInt().coerceAtLeast(100)
    val serviceFee = 20
    val totalAmount = courtFee + serviceFee
    val resolvedPreviewEmail = recipientEmail.trim().ifEmpty {
        defaultRecipientEmail.trim().ifEmpty {
            MockEmailNotificationService.formatDefaultEmail(playerName.trim().ifEmpty { defaultPlayerName.ifEmpty { "Jonel P." } })
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("available_court_reservation_sheet"),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Bottom sheet drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )
                    }

                    // Header Row with Blue Available Badge & Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = GCashBlueBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, GCashBlue.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(GCashBlue)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "BLUE • AVAILABLE COURT",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GCashBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "₱$baseHourlyRate/hr",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GCashBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reserve ${court.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${effectiveBusinessSettings.businessName.ifBlank { facilityName }} • $sanitizedCourtType • ${if (court.isIndoor) "Indoor Air-Con" else "Covered Outdoor"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_available_court_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close reservation sheet"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Booking Type Selector: Online Booking vs Walk-In Booking (Cash on Hand at Cashier)
                    Text(
                        text = "Booking Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    localWalkInMode = false
                                    onWalkInModeChange(false)
                                    if (localPaymentMethod == "Cash on Hand") {
                                        localPaymentMethod = "GCash"
                                        onPaymentMethodSelect("GCash")
                                    }
                                }
                                .testTag("booking_mode_online"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (!localWalkInMode) GCashBlue else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (!localWalkInMode) GCashBlue else MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Online Booking",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!localWalkInMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Admin Approval → Receipt Upload",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (!localWalkInMode) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    localWalkInMode = true
                                    onWalkInModeChange(true)
                                    localPaymentMethod = "Cash on Hand"
                                    onPaymentMethodSelect("Cash on Hand")
                                }
                                .testTag("booking_mode_walkin"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (localWalkInMode) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (localWalkInMode) EmeraldPrimary else MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Walk-In Booking",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (localWalkInMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Cash on Hand at Cashier",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (localWalkInMode) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Select Day & Available Time Slot inside Sheet when provided
                    if (dateOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Select Available Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dateOptions.forEach { dOpt ->
                                val isSelected = selectedDateOption?.isoDate == dOpt.isoDate
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) EmeraldDark else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isSelected) OpticVolt else MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .clickable { onSelectDateOption(dOpt) }
                                        .testTag("sheet_date_option_${dOpt.isoDate}")
                                ) {
                                    Text(
                                        text = "${dOpt.dayShort} ${dOpt.dayNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (timeSlotOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Select Available Time Slot",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            timeSlotOptions.forEach { slotOpt ->
                                val isBooked = bookedTimeSlotsForCourt.contains(slotOpt.startTime)
                                val isSelected = selectedTimeSlotOption?.startTime == slotOpt.startTime
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isBooked -> MaintenanceRed.copy(alpha = 0.14f)
                                        isSelected -> GCashBlue
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isBooked -> MaintenanceRed.copy(alpha = 0.4f)
                                            isSelected -> OpticVolt
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    ),
                                    modifier = Modifier
                                        .clickable(enabled = !isBooked) { onSelectTimeSlotOption(slotOpt) }
                                        .testTag("sheet_slot_option_${slotOpt.startTime.replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = if (isBooked) "${slotOpt.startTime} (Booked)" else slotOpt.startTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isBooked -> MaintenanceRed
                                            isSelected -> Color.White
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Player Name & Email Notification Input
                    Text(
                        text = "1. Enter Your Name & Email for Notification",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = {
                            playerName = it
                            if (it.isNotBlank()) showNameError = false
                        },
                        label = { Text("Player / Booking Name") },
                        placeholder = { Text("Enter your full name (e.g., Juan Dela Cruz)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GCashBlue
                            )
                        },
                        trailingIcon = {
                            if (playerName.isNotEmpty()) {
                                IconButton(onClick = { playerName = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear name"
                                    )
                                }
                            }
                        },
                        isError = showNameError,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("available_court_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it },
                        label = { Text("Email Address for Confirmation Summary") },
                        placeholder = { Text("Auto-filled: $resolvedPreviewEmail") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            if (recipientEmail.isNotEmpty()) {
                                IconButton(onClick = { recipientEmail = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear email"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("available_court_email_input")
                    )
                    if (showNameError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Please enter your name to complete the court reservation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaintenanceRed
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Reservation Duration Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Select Reservation Duration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = GCashBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedDuration.label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GCashBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { option ->
                            val isSelected = selectedDuration.id == option.id
                            val optionPrice = (baseHourlyRate * option.hoursMultiplier).toInt().coerceAtLeast(100)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedDuration = option }
                                    .testTag("duration_option_${option.id}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) GCashBlue else GCashBlueBg.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) OpticVolt else GCashBlue.copy(alpha = 0.55f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else GCashBlue
                                    )
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "₱$optionPrice",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) OpticVolt else EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Mode of Payment Selection
                    Text(
                        text = if (localWalkInMode) {
                            "3. Mode of Payment (Walk-In Requires Cash on Hand at Cashier)"
                        } else {
                            "3. Select Mode of Payment"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sheetPaymentOptions = if (localWalkInMode) {
                        listOf(
                            PaymentOptionSpec("Cash on Hand", "Pay Cash at Cashier Counter", EmeraldPrimary, AvailableGreenBg, "CASH_ON_HAND")
                        )
                    } else {
                        listOf(
                            PaymentOptionSpec("GCash", "Scan GCash QR Code", GCashBlue, GCashBlueBg, "GCASH"),
                            PaymentOptionSpec("PayMaya", "Scan PayMaya QR Code", MayaMint, MayaMintBg, "PAYMAYA"),
                            PaymentOptionSpec("Cash on Hand", "Walk-In Cash at Cashier", EmeraldPrimary, AvailableGreenBg, "VENUE")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sheetPaymentOptions.forEach { option ->
                            val isSelected = localPaymentMethod == option.name
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        localPaymentMethod = option.name
                                        onPaymentMethodSelect(option.name)
                                    }
                                    .testTag("sheet_payment_option_${option.badge}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) option.accentColor else option.bgTint.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = option.accentColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else option.accentColor
                                    )
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Display GCash or PayMaya QR Code when selected
                    if (localPaymentMethod == "GCash" || localPaymentMethod == "PayMaya") {
                        Spacer(modifier = Modifier.height(12.dp))
                        PaymentQrCodeDisplayCard(
                            paymentMethod = localPaymentMethod,
                            amount = totalAmount,
                            businessSettings = effectiveBusinessSettings,
                            tagPrefix = "sheet"
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sheet_venue_business_info_card"),
                            shape = RoundedCornerShape(14.dp),
                            color = AvailableGreenBg.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, EmeraldPrimary)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Pay at Counter • ${effectiveBusinessSettings.businessName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "${effectiveBusinessSettings.businessAddress} • ${effectiveBusinessSettings.contactNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Live Reservation Summary Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Reserved For",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = playerName.trim().ifEmpty { "Enter your name above" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playerName.isBlank()) MaintenanceRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Court & Duration",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${court.name} • ${selectedDuration.label} ($localPaymentMethod)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GCashBlue
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Email Notification To",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = resolvedPreviewEmail,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.testTag("sheet_preview_email_text")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total (incl. ₱$serviceFee service fee)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₱$totalAmount",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Once confirmed, a reservation summary email is sent to $resolvedPreviewEmail, your QR Pass is issued, and ${court.name} turns Red (Booked) for ${selectedDuration.label}.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Triggered Mock Email Notification Summary Card (displayed immediately once booking is confirmed in the bottom sheet)
                    confirmedEmailSummary?.let { emailSummary ->
                        MockEmailNotificationSummaryCard(
                            summary = emailSummary,
                            onResend = {
                                val resent = MockEmailNotificationService.resendEmail(emailSummary)
                                confirmedEmailSummary = resent
                                onEmailNotificationSent(resent)
                            },
                            onDismiss = { confirmedEmailSummary = null }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 5. Confirm Reservation Button
                    Button(
                        onClick = {
                            val finalPlayerName = playerName.trim().ifEmpty {
                                defaultPlayerName.trim().ifEmpty { "Jonel P." }
                            }
                            val finalEmail = recipientEmail.trim().ifEmpty {
                                defaultRecipientEmail.trim().ifEmpty {
                                    MockEmailNotificationService.formatDefaultEmail(finalPlayerName)
                                }
                            }
                            showNameError = false
                            val sentEmailSummary = MockEmailNotificationService.sendReservationConfirmationEmail(
                                recipientName = finalPlayerName,
                                recipientEmail = finalEmail,
                                facilityName = effectiveBusinessSettings.businessName.ifBlank { facilityName },
                                facilityAddress = effectiveBusinessSettings.businessAddress,
                                contactNumber = effectiveBusinessSettings.contactNumber,
                                courtName = court.name,
                                courtType = sanitizedCourtType,
                                dateLabel = dateLabel,
                                durationLabel = selectedDuration.label,
                                timeRangeLabel = "$timeSlotLabel • ${selectedDuration.label}",
                                paymentMethod = localPaymentMethod,
                                courtFee = courtFee,
                                serviceFee = serviceFee,
                                discountAmount = 0,
                                totalAmount = totalAmount
                            )
                            confirmedEmailSummary = sentEmailSummary
                            onEmailNotificationSent(sentEmailSummary)
                            onPaymentMethodSelect(localPaymentMethod)
                            onConfirmReservation(finalPlayerName, selectedDuration)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GCashBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_available_court_reservation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Confirm Reservation (${selectedDuration.label} • ₱$totalAmount)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onOpenFullSchedule,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_full_schedule_from_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Choose Specific Date & Time Slot Instead",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
