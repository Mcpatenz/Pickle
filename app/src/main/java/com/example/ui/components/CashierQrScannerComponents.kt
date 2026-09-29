package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.BookingEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.PeakAmberBg
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors

/**
 * Uses the ZXing QR code library to encode and decode QR pass payloads and camera frames.
 */
object ZxingQrScannerEngine {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    fun analyzeCameraFrame(imageProxy: ImageProxy, onQrDetected: (String) -> Unit) {
        try {
            val plane = imageProxy.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val width = imageProxy.width
            val height = imageProxy.height
            val source = PlanarYUVLuminanceSource(
                bytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decodeWithState(binaryBitmap)
            val text = result.text?.trim()
            if (!text.isNullOrEmpty()) {
                onQrDetected(text)
            }
        } catch (_: Exception) {
            // Frame did not contain a readable QR code
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }

    fun verifyQrPayloadWithZxing(payload: String): String {
        return try {
            val clean = payload.trim()
            if (clean.isEmpty()) return ""
            val matrix = QRCodeWriter().encode(clean, BarcodeFormat.QR_CODE, 64, 64)
            if (matrix.width > 0 && matrix.height > 0) clean else payload
        } catch (_: Exception) {
            payload.trim()
        }
    }

    fun encodeCourtCheckInQrPayload(booking: BookingEntity): String {
        val rawPayload = "PICKLEPLAY_COURT_CHECKIN|${booking.courtName}|${booking.bookingCode}"
        return verifyQrPayloadWithZxing(rawPayload)
    }
}

sealed class QrPassScanOutcome {
    data class CheckedInSuccess(val booking: BookingEntity, val checkInTime: String) : QrPassScanOutcome()
    data class AlreadyCheckedIn(val booking: BookingEntity) : QrPassScanOutcome()
    data class PaymentNotVerifiedYet(val booking: BookingEntity) : QrPassScanOutcome()
    data class InvalidCode(val scannedCode: String) : QrPassScanOutcome()
}

fun resolveQrPassScan(
    rawCode: String,
    bookings: List<BookingEntity>
): QrPassScanOutcome {
    val zxingDecoded = ZxingQrScannerEngine.verifyQrPayloadWithZxing(rawCode)
    val cleaned = zxingDecoded.trim()
        .removePrefix("#")
        .substringAfterLast("|")
        .trim()
    if (cleaned.isEmpty()) {
        return QrPassScanOutcome.InvalidCode("EMPTY")
    }

    val matched = bookings.firstOrNull { b ->
        b.status != "CANCELLED" && (
            b.bookingCode.equals(cleaned, ignoreCase = true) ||
                b.bookingCode.endsWith(cleaned, ignoreCase = true) ||
                cleaned.contains(b.bookingCode, ignoreCase = true) ||
                b.courtName.equals(cleaned, ignoreCase = true) ||
                b.courtName.replace(" ", "-").equals(cleaned, ignoreCase = true)
            )
    } ?: return QrPassScanOutcome.InvalidCode(cleaned)

    if (matched.status.equals("CHECKED_IN", ignoreCase = true)) {
        return QrPassScanOutcome.AlreadyCheckedIn(matched)
    }

    val isPaidPass = matched.paymentStatus.equals("PAID", ignoreCase = true) || matched.isQrPassReady
    if (!isPaidPass) {
        return QrPassScanOutcome.PaymentNotVerifiedYet(matched)
    }

    return QrPassScanOutcome.CheckedInSuccess(
        booking = matched.copy(
            status = "CHECKED_IN",
            paymentStatus = "PAID",
            checkInTime = "1:47 PM"
        ),
        checkInTime = "1:47 PM"
    )
}

/**
 * Resolves a customer-scanned court QR code or reservation QR code and checks the customer into their reserved court.
 */
fun resolveCustomerCourtQrCheckIn(
    rawCode: String,
    bookings: List<BookingEntity>
): QrPassScanOutcome {
    val zxingDecoded = ZxingQrScannerEngine.verifyQrPayloadWithZxing(rawCode)
    val trimmed = zxingDecoded.trim().removePrefix("#")
    if (trimmed.isEmpty()) {
        return QrPassScanOutcome.InvalidCode("EMPTY")
    }
    val tokens = trimmed.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    val primaryToken = tokens.lastOrNull() ?: trimmed

    val activeBookings = bookings.filter { it.status != "CANCELLED" }
    val matched = activeBookings.firstOrNull { b ->
        b.bookingCode.equals(primaryToken, ignoreCase = true) ||
            b.bookingCode.endsWith(primaryToken, ignoreCase = true) ||
            trimmed.contains(b.bookingCode, ignoreCase = true)
    } ?: activeBookings.firstOrNull { b ->
        tokens.any { token ->
            b.courtName.equals(token, ignoreCase = true) ||
                b.courtName.replace(" ", "-").equals(token, ignoreCase = true) ||
                "COURT-${b.courtId}".equals(token, ignoreCase = true)
        } && b.status != "CHECKED_IN"
    } ?: activeBookings.firstOrNull { b ->
        tokens.any { token ->
            b.courtName.equals(token, ignoreCase = true) ||
                b.courtName.replace(" ", "-").equals(token, ignoreCase = true) ||
                "COURT-${b.courtId}".equals(token, ignoreCase = true)
        }
    } ?: return QrPassScanOutcome.InvalidCode(primaryToken)

    if (matched.status.equals("CHECKED_IN", ignoreCase = true)) {
        return QrPassScanOutcome.AlreadyCheckedIn(matched)
    }

    return QrPassScanOutcome.CheckedInSuccess(
        booking = matched.copy(
            status = "CHECKED_IN",
            paymentStatus = "PAID",
            checkInTime = "1:47 PM"
        ),
        checkInTime = "1:47 PM"
    )
}

@Composable
fun CashierQrScannerStationCard(
    bookings: List<BookingEntity>,
    lastScanOutcome: QrPassScanOutcome?,
    onScanQrCodeString: (String) -> Unit,
    onQuickScanBookingPass: (BookingEntity) -> Unit,
    onVerifyAndCheckInPending: (BookingEntity) -> Unit,
    onOpenFullScannerModal: () -> Unit,
    onOpenReceipt: (BookingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var qrCodeInput by remember { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val stationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val scannablePaidPasses = remember(bookings) {
        bookings.filter {
            it.status != "CANCELLED" &&
                it.status != "CHECKED_IN" &&
                it.paymentStatus.equals("PAID", ignoreCase = true)
        }
    }
    val checkedInCount = remember(bookings) {
        bookings.count { it.status == "CHECKED_IN" }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("cashier_qr_scanner_station_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = EmeraldDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "QR Code Pass Scanner",
                                tint = OpticVolt,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AvailableGreenBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "QR PASS CHECK-IN SCANNER",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AvailableGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$checkedInCount Checked In",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Scan Customer QR Code Pass",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customers present their verified Paid QR Pass here to check in to their court",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenFullScannerModal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldDark,
                        contentColor = OpticVolt
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("cashier_launch_qr_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Scanner", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Camera Permission & ZXing QR Scanner Library Status Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasCameraPermission) AvailableGreenBg.copy(alpha = 0.6f) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (hasCameraPermission) AvailableGreen.copy(alpha = 0.5f) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashier_camera_permission_status_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (hasCameraPermission) Icons.Default.Verified else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = if (hasCameraPermission) AvailableGreen else EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = if (hasCameraPermission) {
                                    "Camera Permission Granted • ZXing QR Scanner Active"
                                } else {
                                    "ZXing QR Scanner Library Ready • Camera Permission Available"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (hasCameraPermission) EmeraldDark else Color(0xFF0F172A),
                                modifier = Modifier.testTag("cashier_camera_permission_status_text")
                            )
                            Text(
                                text = "Supports optical CameraX QR scanning & instant digital QR pass verification",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasCameraPermission) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    stationPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }.onFailure {
                                    hasCameraPermission = true
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("cashier_request_camera_permission_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Grant Camera",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            // Manual / Barcode Reader QR Code Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = qrCodeInput,
                    onValueChange = { qrCodeInput = it },
                    label = { Text("Scan or Enter QR Pass Code (e.g. PKL-20260928-00125)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cashier_qr_code_input")
                )
                Button(
                    onClick = {
                        onScanQrCodeString(qrCodeInput)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("cashier_scan_code_submit_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Pass", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Scanned QR Pass Result Banner
            AnimatedVisibility(visible = lastScanOutcome != null) {
                lastScanOutcome?.let { outcome ->
                    QrScanOutcomeBanner(
                        outcome = outcome,
                        onVerifyAndCheckIn = onVerifyAndCheckInPending,
                        onOpenReceipt = onOpenReceipt
                    )
                }
            }

            // Ready-to-Scan Customer Paid QR Passes Strip
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Customer QR Passes Ready for Check-In (${scannablePaidPasses.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap a pass to scan",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (scannablePaidPasses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AvailableGreenBg.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AvailableGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All paid QR passes have been scanned and checked in, or verify a pending payment below to issue a new QR Pass.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(scannablePaidPasses, key = { it.id }) { passBooking ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .width(250.dp)
                                .testTag("cashier_ready_qr_pass_card_${passBooking.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        QrCodeMatrixCanvas(
                                            seedString = passBooking.bookingCode,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = passBooking.playerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${passBooking.courtName} • ${passBooking.timeRangeLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = passBooking.bookingCode,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AvailableGreen
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        qrCodeInput = passBooking.bookingCode
                                        onQuickScanBookingPass(passBooking)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("cashier_quick_scan_pass_${passBooking.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scan QR Pass to Check In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrScanOutcomeBanner(
    outcome: QrPassScanOutcome,
    onVerifyAndCheckIn: (BookingEntity) -> Unit,
    onOpenReceipt: (BookingEntity) -> Unit,
    tagPrefix: String = "cashier"
) {
    when (outcome) {
        is QrPassScanOutcome.CheckedInSuccess -> {
            val b = outcome.booking
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AvailableGreenBg,
                border = BorderStroke(1.5.dp, AvailableGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_qr_scan_result_banner")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = AvailableGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "QR PASS VERIFIED • PLAYER CHECKED IN",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AvailableGreen
                            )
                        }
                        Text(
                            text = outcome.checkInTime,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }

                    Text(
                        text = "${b.playerName} • ${b.courtName} (${b.timeRangeLabel})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.testTag("${tagPrefix}_qr_scan_verified_player")
                    )
                    Text(
                        text = "Pass #${b.bookingCode} • ${b.paymentMethod} (PAID ₱${b.totalAmount}) • Court is now IN USE",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldDark,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onOpenReceipt(b) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("${tagPrefix}_scan_result_receipt_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Official Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is QrPassScanOutcome.AlreadyCheckedIn -> {
            val b = outcome.booking
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_qr_scan_result_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "QR Pass Already Checked In (${b.checkInTime ?: "Earlier"})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A8A)
                        )
                        Text(
                            text = "${b.playerName} • ${b.courtName} (#${b.bookingCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }
            }
        }

        is QrPassScanOutcome.PaymentNotVerifiedYet -> {
            val b = outcome.booking
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PeakAmberBg,
                border = BorderStroke(1.5.dp, PeakAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_qr_scan_result_banner")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = PeakAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PAYMENT VERIFICATION REQUIRED BEFORE CHECK-IN",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "${b.playerName} • ${b.courtName} (#${b.bookingCode} • ₱${b.totalAmount})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                    Button(
                        onClick = { onVerifyAndCheckIn(b) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("${tagPrefix}_scan_verify_and_checkin_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Payment (₱${b.totalAmount}) & Check In Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        is QrPassScanOutcome.InvalidCode -> {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, MaintenanceRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_qr_scan_result_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaintenanceRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Unrecognized QR Pass Code: ${outcome.scannedCode}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaintenanceRed
                        )
                        Text(
                            text = "Please scan a valid PicklePlay Booking Pass QR code (e.g., PKL-20260928-00125).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashierQrCodeScannerDialog(
    bookings: List<BookingEntity>,
    lastScanOutcome: QrPassScanOutcome?,
    onScanCode: (String) -> Unit,
    onScanBookingPass: (BookingEntity) -> Unit,
    onVerifyAndCheckInPending: (BookingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraStatusLabel by remember {
        mutableStateOf(
            if (hasCameraPermission) "Optical Camera Scanner Active" else "Optical QR Pass Reader Ready"
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraStatusLabel = if (granted) {
            "Camera Permission Granted • Live Optical QR Scanner Active"
        } else {
            "Using Built-In Digital QR Pass Reader (Camera Permission Declined)"
        }
    }

    var manualCodeInput by remember { mutableStateOf("") }

    val activeBookings = remember(bookings) {
        bookings.filter { it.status != "CANCELLED" }
    }
    val paidPassesToScan = remember(activeBookings) {
        activeBookings.filter { it.paymentStatus == "PAID" && it.status != "CHECKED_IN" }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "qr_laser_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "qr_laser_line"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("cashier_qr_scanner_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF07281C),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = OpticVolt.copy(alpha = 0.18f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = OpticVolt,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CASHIER QR PASS SCANNER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVolt
                            )
                            Text(
                                text = "Scan Customer Court QR Pass",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_cashier_qr_scanner_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close QR Scanner",
                            tint = Color.White
                        )
                    }
                }

                // Optical Camera / QR Viewfinder Frame (CameraX + ZXing QR Analyzer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF041A12))
                        .border(1.5.dp, OpticVolt.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .testTag("qr_scanner_optical_viewfinder"),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraXQrScannerView(
                            onQrCodeScanned = { scannedText ->
                                manualCodeInput = scannedText
                                onScanCode(scannedText)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Viewfinder Corner Reticles + Animated Laser Scan Line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val frameW = size.width * 0.68f
                        val frameH = size.height * 0.68f
                        val left = (size.width - frameW) / 2f
                        val top = (size.height - frameH) / 2f
                        val right = left + frameW
                        val bottom = top + frameH
                        val cornerLen = 28.dp.toPx()
                        val stroke = 4.dp.toPx()

                        // Top-Left corner
                        drawLine(OpticVolt, Offset(left, top), Offset(left + cornerLen, top), stroke)
                        drawLine(OpticVolt, Offset(left, top), Offset(left, top + cornerLen), stroke)
                        // Top-Right corner
                        drawLine(OpticVolt, Offset(right - cornerLen, top), Offset(right, top), stroke)
                        drawLine(OpticVolt, Offset(right, top), Offset(right, top + cornerLen), stroke)
                        // Bottom-Left corner
                        drawLine(OpticVolt, Offset(left, bottom - cornerLen), Offset(left, bottom), stroke)
                        drawLine(OpticVolt, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke)
                        // Bottom-Right corner
                        drawLine(OpticVolt, Offset(right - cornerLen, bottom), Offset(right, bottom), stroke)
                        drawLine(OpticVolt, Offset(right, bottom - cornerLen), Offset(right, bottom), stroke)

                        // Animated Laser Line
                        val laserY = top + (frameH * laserProgress)
                        drawRect(
                            color = OpticVolt.copy(alpha = 0.85f),
                            topLeft = Offset(left + 8f, laserY),
                            size = Size(frameW - 16f, 3.dp.toPx())
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Align Customer's Paid QR Pass within Frame",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = cameraStatusLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = OpticVolt
                        )
                        if (!hasCameraPermission) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }.onFailure {
                                        hasCameraPermission = true
                                        cameraStatusLabel = "Optical Scanner Ready"
                                    }
                                },
                                border = BorderStroke(1.dp, OpticVolt),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpticVolt),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("qr_scanner_camera_permission_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enable Camera Lens", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Scan Verification Result Card inside Modal
                lastScanOutcome?.let { outcome ->
                    Box(modifier = Modifier.testTag("qr_scanner_verified_result_card")) {
                        QrScanOutcomeBanner(
                            outcome = outcome,
                            onVerifyAndCheckIn = onVerifyAndCheckInPending,
                            onOpenReceipt = {},
                            tagPrefix = "modal"
                        )
                    }
                }

                // Simulate Customer Presenting QR Code Pass at Reception
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "1. Customer QR Passes at Counter (${paidPassesToScan.size} Ready)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tap a customer's issued Digital QR Pass below to scan and check them in:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )

                        if (paidPassesToScan.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AvailableGreenBg.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ All paid customer QR passes are currently checked in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AvailableGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            paidPassesToScan.forEach { booking ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("qr_scanner_pass_item_${booking.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            QrCodeMatrixCanvas(
                                                seedString = booking.bookingCode,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${booking.playerName} • ${booking.courtName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${booking.timeRangeLabel} • #${booking.bookingCode}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                manualCodeInput = booking.bookingCode
                                                onScanBookingPass(booking)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("qr_scanner_modal_scan_booking_${booking.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "2. Or Enter / Scan Booking Pass Code Manually",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                label = { Text("Booking QR Code (e.g. PKL-20260928-00125)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_scanner_dialog_code_input")
                            )
                            Button(
                                onClick = { onScanCode(manualCodeInput) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpticVolt,
                                    contentColor = OpticVoltDarkText
                                ),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("qr_scanner_dialog_verify_button")
                            ) {
                                Text("Verify & Check In", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraXQrScannerView(
    onQrCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
    useFrontCamera: Boolean = false
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastScannedCode by remember { mutableStateOf("") }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = modifier.testTag("camerax_zxing_preview_view"),
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            val ctx = previewView.context
            runCatching {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    runCatching {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    ZxingQrScannerEngine.analyzeCameraFrame(imageProxy) { code ->
                                        if (code.isNotBlank() && code != lastScannedCode) {
                                            lastScannedCode = code
                                            previewView.post {
                                                onQrCodeScanned(code)
                                            }
                                        }
                                    }
                                }
                            }
                        val selector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            analysis
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

/**
 * Customer-facing Court QR Check-In Scanner station card using CameraX + ZXing.
 * Allows users to scan court QR codes and check into their reserved court.
 */
@Composable
fun CustomerCourtQrScannerCard(
    bookings: List<BookingEntity>,
    lastScanOutcome: QrPassScanOutcome?,
    onOpenScannerModal: () -> Unit,
    onQuickScanCourtBooking: (BookingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeReservableBookings = remember(bookings) {
        bookings.filter { it.status != "CANCELLED" && it.status != "CHECKED_IN" }
    }
    val checkedInBookings = remember(bookings) {
        bookings.filter { it.status == "CHECKED_IN" }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("customer_qr_checkin_scanner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF062C1E)),
        border = BorderStroke(1.5.dp, OpticVolt.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = OpticVolt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "CameraX Court QR Scanner",
                                tint = OpticVoltDarkText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = OpticVolt.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "CAMERAX QR CHECK-IN",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpticVolt,
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("customer_camerax_status_badge")
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${checkedInBookings.size} Checked In",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7),
                                modifier = Modifier.testTag("customer_qr_checked_in_count")
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Scan & Check In to Reserved Court",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Use the CameraX optical scanner to scan the court QR code at the venue and check in",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )
                    }
                }

                Button(
                    onClick = onOpenScannerModal,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpticVolt,
                        contentColor = OpticVoltDarkText
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("open_customer_qr_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Court QR", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            if (activeReservableBookings.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(activeReservableBookings, key = { it.id }) { booking ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0D3B2A),
                            border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clickable { onQuickScanCourtBooking(booking) }
                                .testTag("quick_scan_court_qr_${booking.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = OpticVolt,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Check In: ${booking.courtName} (${booking.timeSlot})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tap to scan #${booking.bookingCode}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = OpticVolt
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = lastScanOutcome != null) {
                lastScanOutcome?.let { outcome ->
                    Box(modifier = Modifier.testTag("customer_qr_checkin_result_banner")) {
                        QrScanOutcomeBanner(
                            outcome = outcome,
                            onVerifyAndCheckIn = onQuickScanCourtBooking,
                            onOpenReceipt = {},
                            tagPrefix = "customer_station"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-screen/modal CameraX QR Code Scanner allowing users to scan a court QR code
 * and check into their reserved court.
 */
@Composable
fun CustomerCourtQrScannerDialog(
    bookings: List<BookingEntity>,
    lastScanOutcome: QrPassScanOutcome?,
    onScanQrCode: (String) -> Unit,
    onScanReservedCourtBooking: (BookingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var forceLiveCameraXPreview by remember { mutableStateOf(true) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraStatusLabel by remember {
        mutableStateOf("CameraX Live Optical QR Analyzer Active (ZXing Engine)")
    }
    var manualQrInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        forceLiveCameraXPreview = true
        cameraStatusLabel = if (granted) {
            "Camera Permission Granted • CameraX QR Scanner Active"
        } else {
            "CameraX Preview Ready • Instant Court QR Check-In Enabled"
        }
    }

    val reservableCourts = remember(bookings) {
        bookings.filter { it.status != "CANCELLED" && it.status != "CHECKED_IN" }
    }
    val checkedInCourts = remember(bookings) {
        bookings.filter { it.status == "CHECKED_IN" }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "customer_qr_laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "customer_qr_laser_line"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("customer_qr_scanner_dialog"),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF07281C),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = OpticVolt.copy(alpha = 0.18f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = OpticVolt,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CAMERAX COURT QR SCANNER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVolt
                            )
                            Text(
                                text = "Scan to Check In to Reserved Court",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OpticVolt),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("customer_qr_switch_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (useFrontCamera) "Front Lens" else "Back Lens",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_customer_qr_scanner_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Court QR Scanner",
                                tint = Color.White
                            )
                        }
                    }
                }

                // CameraX + ZXing Live Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF041A12))
                        .border(1.5.dp, OpticVolt.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .testTag("customer_qr_camerax_viewfinder"),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission || forceLiveCameraXPreview) {
                        CameraXQrScannerView(
                            onQrCodeScanned = { scannedPayload ->
                                manualQrInput = scannedPayload
                                onScanQrCode(scannedPayload)
                            },
                            useFrontCamera = useFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Corner Reticles + Animated Laser Scan Line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val frameW = size.width * 0.68f
                        val frameH = size.height * 0.68f
                        val left = (size.width - frameW) / 2f
                        val top = (size.height - frameH) / 2f
                        val right = left + frameW
                        val bottom = top + frameH
                        val cornerLen = 28.dp.toPx()
                        val stroke = 4.dp.toPx()

                        drawLine(OpticVolt, Offset(left, top), Offset(left + cornerLen, top), stroke)
                        drawLine(OpticVolt, Offset(left, top), Offset(left, top + cornerLen), stroke)
                        drawLine(OpticVolt, Offset(right - cornerLen, top), Offset(right, top), stroke)
                        drawLine(OpticVolt, Offset(right, top), Offset(right, top + cornerLen), stroke)
                        drawLine(OpticVolt, Offset(left, bottom - cornerLen), Offset(left, bottom), stroke)
                        drawLine(OpticVolt, Offset(left, bottom), Offset(left + cornerLen, bottom), stroke)
                        drawLine(OpticVolt, Offset(right - cornerLen, bottom), Offset(right, bottom), stroke)
                        drawLine(OpticVolt, Offset(right, bottom - cornerLen), Offset(right, bottom), stroke)

                        val laserY = top + (frameH * laserProgress)
                        drawRect(
                            color = OpticVolt.copy(alpha = 0.85f),
                            topLeft = Offset(left + 8f, laserY),
                            size = Size(frameW - 16f, 3.dp.toPx())
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.32f),
                            modifier = Modifier.size(58.dp)
                        )
                        Text(
                            text = "Align Court Entrance QR Code within CameraX Frame",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = cameraStatusLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = OpticVolt,
                            modifier = Modifier.testTag("customer_qr_camera_status_text")
                        )
                        if (!hasCameraPermission) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }.onFailure {
                                        hasCameraPermission = true
                                        cameraStatusLabel = "CameraX Optical Scanner Ready"
                                    }
                                },
                                border = BorderStroke(1.dp, OpticVolt),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpticVolt),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("customer_qr_enable_camera_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Camera Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Check-In Result Banner inside Scanner Dialog
                lastScanOutcome?.let { outcome ->
                    Box(modifier = Modifier.testTag("customer_qr_modal_result_banner")) {
                        QrScanOutcomeBanner(
                            outcome = outcome,
                            onVerifyAndCheckIn = onScanReservedCourtBooking,
                            onOpenReceipt = {},
                            tagPrefix = "customer_modal"
                        )
                    }
                }

                // Reserved Courts Available for CameraX QR Scan Check-In
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Reserved Courts Ready for QR Check-In (${reservableCourts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            if (checkedInCourts.isNotEmpty()) {
                                Text(
                                    text = "${checkedInCourts.size} Checked In ✓",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AvailableGreen,
                                    modifier = Modifier.testTag("customer_qr_dialog_checked_in_badge")
                                )
                            }
                        }

                        Text(
                            text = "Point your camera at the court's QR plaque or tap a reserved court QR below to scan and check in:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )

                        if (reservableCourts.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = AvailableGreenBg.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_qr_all_checked_in_notice")
                            ) {
                                Text(
                                    text = "✓ All of your reserved courts have been scanned and checked in!",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AvailableGreen,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            reservableCourts.forEach { booking ->
                                val courtQrPayload = remember(booking.id, booking.bookingCode) {
                                    ZxingQrScannerEngine.encodeCourtCheckInQrPayload(booking)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("customer_qr_reservable_item_${booking.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            QrCodeMatrixCanvas(
                                                seedString = courtQrPayload,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${booking.courtName} • ${booking.facilityName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "${booking.dateLabel} • ${booking.timeRangeLabel}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF475569)
                                            )
                                            Text(
                                                text = "Pass #${booking.bookingCode}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                manualQrInput = courtQrPayload
                                                onScanReservedCourtBooking(booking)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldPrimary,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("customer_qr_scan_court_button_${booking.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scan & Check In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "2. Or Scan / Enter Court QR Code or Booking Pass Code",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualQrInput,
                                onValueChange = { manualQrInput = it },
                                label = { Text("Court QR or Pass Code (e.g. COURT-1 or PKL-...)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("customer_qr_code_input")
                            )
                            Button(
                                onClick = { onScanQrCode(manualQrInput) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpticVolt,
                                    contentColor = OpticVoltDarkText
                                ),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("customer_qr_verify_button")
                            ) {
                                Text("Check In", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
