package com.example

import android.content.Context
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.ui.components.AvailableCourtReservationSheet
import com.example.ui.components.BookingCheckoutDialog
import com.example.ui.theme.PicklePlayTheme
import com.example.viewmodel.BookingDateOption
import com.example.viewmodel.BusinessPaymentSettings
import com.example.viewmodel.BusinessSettingsLocalStore
import com.example.viewmodel.MockEmailNotificationService
import com.example.viewmodel.ReservationDurationOption
import com.example.viewmodel.TimeSlotOption
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `save and retrieve admin business settings and qr codes locally`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val saved = BusinessSettingsLocalStore.save(
            context = context,
            settings = BusinessPaymentSettings(
                businessName = "Manila Dink Arena",
                address = "Makati CBD, Metro Manila",
                contactNumber = "+63 917 888 9900",
                gcashQrUploaded = true,
                paymayaQrUploaded = true,
                gcashQrUri = "local://gcash_manila_dink_qr.png",
                paymayaQrUri = "local://paymaya_manila_dink_qr.png",
                gcashFileName = "gcash_manila_dink_qr.png",
                paymayaFileName = "paymaya_manila_dink_qr.png"
            )
        )

        val retrieved = BusinessSettingsLocalStore.load(context)
        assertEquals("Manila Dink Arena", retrieved.businessName)
        assertEquals("Makati CBD, Metro Manila", retrieved.businessAddress)
        assertEquals("+63 917 888 9900", retrieved.contactNumber)
        assertEquals("gcash_manila_dink_qr.png", retrieved.gcashQrFileName)
        assertEquals("paymaya_manila_dink_qr.png", retrieved.paymayaQrFileName)
        assertTrue(retrieved.gcashQrUploaded)
        assertTrue(retrieved.paymayaQrUploaded)
        assertEquals(saved.businessName, retrieved.businessName)

        val gcashQr = BusinessSettingsLocalStore.getStoredQrCode(context, "GCash")
        assertEquals("GCash", gcashQr.paymentMethod)
        assertEquals("gcash_manila_dink_qr.png", gcashQr.qrFileName)
        assertNotNull(gcashQr.bitmap)

        val paymayaQr = BusinessSettingsLocalStore.getStoredQrCode(context, "PayMaya")
        assertEquals("PayMaya", paymayaQr.paymentMethod)
        assertEquals("paymaya_manila_dink_qr.png", paymayaQr.qrFileName)
        assertNotNull(paymayaQr.bitmap)
    }

    @Test
    fun `payment flow retrieves and displays stored GCash and PayMaya QR code images upon selection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BusinessSettingsLocalStore.save(
            context = context,
            settings = BusinessPaymentSettings(
                businessName = "BGC Smash Pickle Club",
                address = "32nd Street, BGC Taguig",
                contactNumber = "+63 917 555 1234",
                gcashQrUploaded = true,
                paymayaQrUploaded = true,
                gcashQrUri = "file://local/bgc_gcash_qr.png",
                paymayaQrUri = "file://local/bgc_paymaya_qr.png",
                gcashFileName = "bgc_gcash_qr.png",
                paymayaFileName = "bgc_paymaya_qr.png"
            )
        )

        val facility = FacilityEntity(
            id = 1,
            name = "BGC Smash Pickle Club",
            city = "Taguig",
            address = "32nd Street, BGC Taguig",
            distanceKm = 1.0,
            rating = 4.9,
            reviewCount = 120,
            courtCount = 6,
            isIndoor = true,
            surfaceType = "Pro Cushion",
            minPricePerHour = 250,
            amenitiesCsv = "Air-conditioned,Parking",
            operatingHours = "6:00 AM - 11:00 PM",
            imageKey = "indoor_hero"
        )
        val court = CourtEntity(
            id = 1,
            facilityId = 1,
            name = "Court 1",
            courtType = "Pro Cushion Indoor",
            isIndoor = true,
            status = "ACTIVE",
            morningPrice = 200,
            middayPrice = 250,
            peakPrice = 350,
            nightPrice = 300,
            weekendPrice = 350
        )

        composeTestRule.setContent {
            PicklePlayTheme {
                BookingCheckoutDialog(
                    facility = facility,
                    court = court,
                    dateOption = BookingDateOption("2026-09-28", "Mon", "28", "Monday, Sep 28, 2026", false),
                    timeSlot = TimeSlotOption("06:00 PM", "6:00 PM - 7:00 PM", "PEAK"),
                    lockSecondsRemaining = 295,
                    courtFee = 350,
                    userProfile = null,
                    promoCodeInput = "",
                    appliedPromoDiscount = 0,
                    selectedPaymentMethod = "GCash",
                    onPromoCodeChange = {},
                    onPaymentMethodSelect = {},
                    onConfirmReservation = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify GCash QR code image and stored metadata are displayed initially
        composeTestRule.onNodeWithTag("checkout_gcash_qr_image").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_qr_file_name_text").assertTextContains("bgc_gcash_qr.png")
        composeTestRule.onNodeWithTag("checkout_business_name_text").assertTextContains("BGC Smash Pickle Club")

        // Switch payment method to PayMaya and verify stored PayMaya QR code image is displayed
        composeTestRule.onNodeWithTag("payment_option_PAYMAYA").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("checkout_paymaya_qr_image").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_qr_file_name_text").assertTextContains("bgc_paymaya_qr.png")
    }

    @Test
    fun `bottom sheet booking confirmation triggers mock email notification service with reservation summary`() {
        MockEmailNotificationService.clearAll()

        val court = CourtEntity(
            id = 3,
            facilityId = 1,
            name = "Court 3",
            courtType = "Pro Cushion Indoor",
            isIndoor = true,
            status = "ACTIVE",
            morningPrice = 200,
            middayPrice = 250,
            peakPrice = 350,
            nightPrice = 300,
            weekendPrice = 350
        )
        val durationOptions = listOf(
            ReservationDurationOption("1h", "1 Hour", "Standard Session", 3600, 1.0),
            ReservationDurationOption("2h", "2 Hours", "Club Doubles", 7200, 2.0)
        )

        var confirmedPlayer = ""
        var confirmedDurationLabel = ""

        composeTestRule.setContent {
            PicklePlayTheme {
                AvailableCourtReservationSheet(
                    court = court,
                    facilityName = "Smash Pickle Club",
                    defaultPlayerName = "Carlos Mendoza",
                    baseHourlyRate = 250,
                    durationOptions = durationOptions,
                    selectedPaymentMethod = "GCash",
                    defaultRecipientEmail = "carlos.mendoza@pickleplay.ph",
                    dateLabel = "Monday, Sep 28, 2026",
                    timeSlotLabel = "2:00 PM",
                    onConfirmReservation = { playerName, duration ->
                        confirmedPlayer = playerName
                        confirmedDurationLabel = duration.label
                    },
                    onOpenFullSchedule = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Select 2 Hours duration and PayMaya payment method in the bottom sheet
        composeTestRule.onNodeWithTag("duration_option_2h").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("sheet_payment_option_PAYMAYA").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("available_court_email_input")
            .performScrollTo()
            .performTextReplacement("carlos.vip@pickleplay.ph")

        // Confirm reservation in the bottom sheet
        composeTestRule.onNodeWithTag("confirm_available_court_reservation_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("Carlos Mendoza", confirmedPlayer)
        assertEquals("2 Hours", confirmedDurationLabel)

        val sentEmail = MockEmailNotificationService.latestSentEmail.value
        assertNotNull(sentEmail)
        assertEquals("Carlos Mendoza", sentEmail?.recipientName)
        assertEquals("carlos.vip@pickleplay.ph", sentEmail?.recipientEmail)
        assertEquals("Court 3", sentEmail?.courtName)
        assertEquals("2 Hours", sentEmail?.durationLabel)
        assertEquals("PayMaya", sentEmail?.paymentMethod)
        assertEquals(520, sentEmail?.totalAmount)

        // Verify the Mock Email Notification Summary Card is displayed in the bottom sheet with reservation details
        composeTestRule.onNodeWithTag("email_notification_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("email_notification_recipient")
            .assertTextContains("To: Carlos Mendoza <carlos.vip@pickleplay.ph>")
        composeTestRule.onNodeWithTag("email_summary_court")
            .assertTextContains("Court: Court 3 (Pro Cushion Indoor)")
        composeTestRule.onNodeWithTag("email_summary_payment")
            .assertTextContains("Payment: PayMaya (PAID)")
        composeTestRule.onNodeWithTag("email_summary_total")
            .assertTextContains("Total Paid: ₱520 (Court ₱500 + Service ₱20)")
    }

    @Test
    fun `cashier dashboard qr code scanner scans customer paid qr pass to check in`() {
        var checkedInBookingCode: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.CashierDashboardScreen(
                    onCheckInBooking = { booking ->
                        checkedInBookingCode = booking.bookingCode
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify Cashier QR Scanner Station Card is displayed on Cashier Dashboard
        composeTestRule.onNodeWithTag("cashier_qr_scanner_station_card").assertIsDisplayed()

        // Open full Cashier QR Scanner Dialog and scan customer's paid QR pass (id = 1, PKL-20260928-00125)
        composeTestRule.onNodeWithTag("cashier_open_qr_scanner_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("cashier_qr_scanner_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("qr_scanner_modal_scan_booking_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("PKL-20260928-00125", checkedInBookingCode)
        composeTestRule.onNodeWithTag("qr_scanner_verified_result_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("modal_qr_scan_verified_player")
            .assertTextContains("Jonel P. • Court 3 (2:00 PM - 3:00 PM)")
    }

    @Test
    fun `admin dashboard add staff supports selecting cashier court side or maintenance role`() {
        val addedStaffRoles = mutableListOf<Pair<String, com.example.viewmodel.StaffRole>>()

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.AdminDashboardScreen(
                    onAddStaffAccount = { fullName, _, _, role ->
                        addedStaffRoles.add(fullName to role)
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Scroll LazyColumn to Staff & Cashier Accounts card and open Add Staff form
        composeTestRule.onNodeWithTag("admin_dashboard_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_cashier_management_card"))
        composeTestRule.onNodeWithTag("admin_add_cashier_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify all three staff roles (Cashier, Court Side, Maintenance) are displayed
        composeTestRule.onNodeWithTag("staff_role_option_CASHIER").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("staff_role_option_COURT_SIDE").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("staff_role_option_MAINTENANCE").performScrollTo().assertIsDisplayed()

        // Select Court Side role and add a Court Side staff member
        composeTestRule.onNodeWithTag("staff_role_option_COURT_SIDE").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("cashier_name_input").performScrollTo().performTextReplacement("Leo CourtSide")
        composeTestRule.onNodeWithTag("cashier_email_input").performScrollTo().performTextReplacement("leo.court@pickleplay.ph")
        composeTestRule.onNodeWithTag("confirm_add_cashier_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Re-open Add Staff form, select Maintenance role, and add a Maintenance staff member
        composeTestRule.onNodeWithTag("admin_add_cashier_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("staff_role_option_MAINTENANCE").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("cashier_name_input").performScrollTo().performTextReplacement("Noel Maintenance")
        composeTestRule.onNodeWithTag("cashier_email_input").performScrollTo().performTextReplacement("noel.maint@pickleplay.ph")
        composeTestRule.onNodeWithTag("confirm_add_cashier_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, addedStaffRoles.size)
        assertEquals("Leo CourtSide" to com.example.viewmodel.StaffRole.COURT_SIDE, addedStaffRoles[0])
        assertEquals("Noel Maintenance" to com.example.viewmodel.StaffRole.MAINTENANCE, addedStaffRoles[1])

        // Verify role badges are rendered in the staff list
        composeTestRule.onNodeWithTag("staff_role_badge_leo.court@pickleplay.ph")
            .performScrollTo()
            .assertTextContains("COURT SIDE")
        composeTestRule.onNodeWithTag("staff_role_badge_noel.maint@pickleplay.ph")
            .performScrollTo()
            .assertTextContains("MAINTENANCE")
    }

    @Test
    fun `customer reservation list displays visual status badges for Pending Verified Paid and Completed states`() {
        val sampleBookings = listOf(
            com.example.data.local.BookingEntity(
                id = 101,
                bookingCode = "PKL-20260928-00101",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 1,
                courtName = "Court 1",
                dateIso = "2026-09-28",
                dateLabel = "September 28, 2026",
                timeSlot = "02:00 PM",
                timeRangeLabel = "2:00 PM - 3:00 PM",
                playerName = "Jonel P.",
                courtFee = 250,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 270,
                paymentMethod = "GCash",
                paymentStatus = "AWAITING_APPROVAL",
                status = "PENDING_ADMIN_APPROVAL"
            ),
            com.example.data.local.BookingEntity(
                id = 102,
                bookingCode = "PKL-20260928-00102",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 2,
                courtName = "Court 2",
                dateIso = "2026-09-28",
                dateLabel = "September 28, 2026",
                timeSlot = "04:00 PM",
                timeRangeLabel = "4:00 PM - 5:00 PM",
                playerName = "Jonel P.",
                courtFee = 350,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 370,
                paymentMethod = "GCash",
                paymentStatus = "AWAITING_PAYMENT",
                status = "APPROVED_AWAITING_PAYMENT"
            ),
            com.example.data.local.BookingEntity(
                id = 103,
                bookingCode = "PKL-20260928-00103",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 3,
                courtName = "Court 3",
                dateIso = "2026-09-28",
                dateLabel = "September 28, 2026",
                timeSlot = "06:00 PM",
                timeRangeLabel = "6:00 PM - 7:00 PM",
                playerName = "Jonel P.",
                courtFee = 350,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 370,
                paymentMethod = "GCash",
                paymentStatus = "PAID",
                status = "CONFIRMED"
            ),
            com.example.data.local.BookingEntity(
                id = 104,
                bookingCode = "PKL-20260927-00104",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 4,
                courtName = "Court 4",
                dateIso = "2026-09-27",
                dateLabel = "September 27, 2026",
                timeSlot = "09:00 AM",
                timeRangeLabel = "9:00 AM - 10:00 AM",
                playerName = "Jonel P.",
                courtFee = 200,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 220,
                paymentMethod = "PayMaya",
                paymentStatus = "PAID",
                status = "CHECKED_IN",
                checkInTime = "8:50 AM"
            )
        )

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.MyReservationsScreen(
                    isAuthenticated = true,
                    authSession = null,
                    bookings = sampleBookings,
                    onBackToDashboard = {},
                    onBookNewCourt = {},
                    onOpenAuthModal = {},
                    onCancelPendingReservation = {},
                    onOpenPaymentDialog = {},
                    onOpenQrPass = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify Pending, Verified, and Paid status badges on upcoming reservations
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reservation_card_101"))
        composeTestRule.onNodeWithTag("reservation_state_badge_101").assertTextContains("Pending")

        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reservation_card_102"))
        composeTestRule.onNodeWithTag("reservation_state_badge_102").assertTextContains("Verified")

        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reservation_card_103"))
        composeTestRule.onNodeWithTag("reservation_state_badge_103").assertTextContains("Paid")

        // Tap the Completed status filter pill to view Completed reservations and verify Completed badge
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reservation_status_filter_bar"))
        composeTestRule.onNodeWithTag("status_filter_COMPLETED").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("reservation_card_104"))
        composeTestRule.onNodeWithTag("reservation_state_badge_104").assertTextContains("Completed")
    }

    @Test
    fun `open play matchmaking displays player skill levels session vacancy and supports joining and creating sessions`() {
        var joinedGameTitle: String? = null
        var hostedGameTitle: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.GamesAndTournamentsScreen(
                    onToggleJoinOpenPlay = { game ->
                        joinedGameTitle = game.title
                    },
                    onHostOpenPlay = { title, _, _, _, _, _, _ ->
                        hostedGameTitle = title
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify Open Play matchmaking summary, skill level badge, player skill level, and session vacancy
        composeTestRule.onNodeWithTag("open_play_matchmaking_summary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("open_play_card_1"))
        composeTestRule.onNodeWithTag("open_play_skill_badge_1").assertTextContains("Intermediate", substring = true)
        composeTestRule.onNodeWithTag("open_play_vacancy_badge_1").assertTextContains("3 SPOTS OPEN", substring = true)
        composeTestRule.onNodeWithTag("open_play_player_skill_1_0").assertTextContains("DUPR", substring = true)

        // Join existing Open Play session #1 and verify vacancy updates from 3 SPOTS OPEN to 2 SPOTS OPEN
        composeTestRule.onNodeWithTag("join_game_button_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Saturday Open Play", joinedGameTitle)
        composeTestRule.onNodeWithTag("open_play_vacancy_badge_1").assertTextContains("2 SPOTS OPEN", substring = true)

        // Create a new Open Play session via Host Game dialog
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("host_open_play_button"))
        composeTestRule.onNodeWithTag("host_open_play_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("host_open_play_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("host_open_play_title_input").performTextReplacement("Sunday Pro Dink Session")
        composeTestRule.onNodeWithTag("host_skill_chip_Advanced").performClick()
        composeTestRule.onNodeWithTag("confirm_host_open_play_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Sunday Pro Dink Session", hostedGameTitle)
    }

    @Test
    fun `tournament management module allows users to view match schedules and progress and admins to organize brackets`() {
        val showAdminView = androidx.compose.runtime.mutableStateOf(false)
        var updatedBracketRound: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                if (!showAdminView.value) {
                    com.example.ui.screens.GamesAndTournamentsScreen()
                } else {
                    com.example.ui.screens.AdminDashboardScreen(
                        onUpdateTournamentBracket = { _, activeRound, _, _, _ ->
                            updatedBracketRound = activeRound
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Switch to Tournaments tab in GamesAndTournamentsScreen and verify match schedules and tournament progress
        composeTestRule.onNodeWithTag("subtab_tournaments").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("tournament_card_1"))
        composeTestRule.onNodeWithTag("tournament_progress_section_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tournament_schedule_list_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("tournament_match_row_1_QF1").performScrollTo().assertIsDisplayed()

        // Switch to AdminDashboardScreen and verify Admin Tournament Bracket & Schedule Manager
        showAdminView.value = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_dashboard_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("admin_tournament_management_card"))
        composeTestRule.onNodeWithTag("admin_tournament_management_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_advance_round_button_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("Semi Finals", updatedBracketRound)
        composeTestRule.onNodeWithTag("admin_tournament_active_stage_1").assertTextContains("Semi Finals", substring = true)
    }

    @Test
    fun `local notification system alerts customers when reservation status updates from Pending to Approved`() {
        val pendingBooking = com.example.data.local.BookingEntity(
            id = 501,
            bookingCode = "PKL-20260928-50100",
            facilityId = 1,
            facilityName = "Smash Pickle Club",
            facilityLocation = "Quezon City",
            courtId = 4,
            courtName = "Court 4",
            dateIso = "2026-09-28",
            dateLabel = "Monday, Sep 28, 2026",
            timeSlot = "4:00 PM",
            timeRangeLabel = "4:00 PM - 5:00 PM",
            playerName = "Jonel P.",
            courtFee = 250,
            discountAmount = 0,
            serviceFee = 20,
            totalAmount = 270,
            paymentMethod = "GCash",
            paymentStatus = "AWAITING_APPROVAL",
            status = "PENDING_ADMIN_APPROVAL"
        )

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.MyReservationsScreen(
                    isAuthenticated = true,
                    authSession = null,
                    bookings = listOf(pendingBooking),
                    onBackToDashboard = {},
                    onBookNewCourt = {},
                    onOpenAuthModal = {},
                    onCancelPendingReservation = {},
                    onOpenPaymentDialog = {},
                    onOpenQrPass = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify local notification center card is displayed and active
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("customer_local_notification_center_card"))
        composeTestRule.onNodeWithTag("customer_local_notification_center_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("local_notification_status_pill").assertTextContains("ACTIVE", substring = true)

        // Trigger status update notification (Pending -> Approved)
        composeTestRule.onNodeWithTag("trigger_status_update_notification_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify the latest alert state in LocalReservationNotificationService
        val latestAlert = com.example.notifications.LocalReservationNotificationService.latestAlert.value
        assertTrue(latestAlert != null)
        assertEquals("Pending", latestAlert?.previousStatus)
        assertEquals("Approved", latestAlert?.newStatus)
        assertEquals("PKL-20260928-50100", latestAlert?.bookingCode)

        // Verify heads-up local notification banner and recent alert list item are displayed
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("local_reservation_heads_up_banner"))
        composeTestRule.onNodeWithTag("local_reservation_heads_up_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("local_notification_banner_title")
            .assertTextContains("Pending → Approved", substring = true)
        composeTestRule.onNodeWithTag("local_status_alert_title_0")
            .performScrollTo()
            .assertTextContains("Pending → Approved", substring = true)
    }

    @Test
    fun `camerax qr code scanner allows users to scan and check into their reserved court`() {
        val reservedBooking = com.example.data.local.BookingEntity(
            id = 701,
            bookingCode = "PKL-20260928-70100",
            facilityId = 1,
            facilityName = "Smash Pickle Club",
            facilityLocation = "Quezon City",
            courtId = 2,
            courtName = "Court 2",
            dateIso = "2026-09-28",
            dateLabel = "Monday, Sep 28, 2026",
            timeSlot = "6:00 PM",
            timeRangeLabel = "6:00 PM - 7:00 PM",
            playerName = "Jonel P.",
            courtFee = 300,
            discountAmount = 0,
            serviceFee = 20,
            totalAmount = 320,
            paymentMethod = "GCash",
            paymentStatus = "PAID",
            status = "CONFIRMED"
        )
        var checkedInBookingResult: com.example.data.local.BookingEntity? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.MyReservationsScreen(
                    isAuthenticated = true,
                    authSession = null,
                    bookings = listOf(reservedBooking),
                    onBackToDashboard = {},
                    onBookNewCourt = {},
                    onOpenAuthModal = {},
                    onCancelPendingReservation = {},
                    onOpenPaymentDialog = {},
                    onOpenQrPass = {},
                    onCheckInBooking = { updated -> checkedInBookingResult = updated }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify CameraX Court QR Check-In Scanner Card is displayed
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("customer_qr_checkin_scanner_card"))
        composeTestRule.onNodeWithTag("customer_qr_checkin_scanner_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("customer_camerax_status_badge")
            .assertTextContains("CAMERAX QR CHECK-IN", substring = true)

        // Open the full CameraX Court QR Scanner Modal
        composeTestRule.onNodeWithTag("open_customer_qr_scanner_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify CameraX viewfinder and PreviewView are rendered in the scanner dialog
        composeTestRule.onNodeWithTag("customer_qr_scanner_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("customer_qr_camerax_viewfinder").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camerax_zxing_preview_view").assertIsDisplayed()

        // Scan the reserved court QR code to check in
        composeTestRule.onNodeWithTag("customer_qr_scan_court_button_701")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify the booking was checked into the reserved court
        assertTrue(checkedInBookingResult != null)
        assertEquals("CHECKED_IN", checkedInBookingResult?.status)
        assertEquals("Court 2", checkedInBookingResult?.courtName)
        composeTestRule.onNodeWithTag("customer_modal_qr_scan_result_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("customer_modal_qr_scan_verified_player")
            .assertTextContains("Court 2", substring = true)

        // Close scanner dialog and verify checked-in status on the reservation screen
        composeTestRule.onNodeWithTag("close_customer_qr_scanner_dialog").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("customer_qr_checked_in_count")
            .assertTextContains("1 Checked In", substring = true)
    }

    @Test
    fun `room database entity and dao store and display past and upcoming pickleball court reservations and tournament bracket visualizer`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PicklePlayDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = db.picklePlayDao()

        val upcomingReservation = com.example.data.local.CourtReservationEntity(
            id = 801,
            bookingCode = "PKL-20261002-801",
            facilityId = 1,
            facilityName = "Smash Pickle Club",
            facilityLocation = "Quezon City",
            courtId = 3,
            courtName = "Court 3",
            dateIso = "2026-10-02",
            dateLabel = "October 2, 2026",
            timeSlot = "06:00 PM",
            timeRangeLabel = "6:00 PM - 7:00 PM",
            playerName = "Jonel P.",
            courtFee = 350,
            discountAmount = 0,
            serviceFee = 20,
            totalAmount = 370,
            paymentMethod = "GCash",
            paymentStatus = "PAID",
            status = "CONFIRMED",
            customerEmail = "customer@pickleplay.ph"
        )
        val pastReservation = com.example.data.local.CourtReservationEntity(
            id = 802,
            bookingCode = "PKL-20260920-802",
            facilityId = 2,
            facilityName = "BGC Dink & Rally Club",
            facilityLocation = "Taguig City",
            courtId = 2,
            courtName = "Court 2",
            dateIso = "2026-09-20",
            dateLabel = "September 20, 2026",
            timeSlot = "04:00 PM",
            timeRangeLabel = "4:00 PM - 5:00 PM",
            playerName = "Jonel P.",
            courtFee = 450,
            discountAmount = 0,
            serviceFee = 20,
            totalAmount = 470,
            paymentMethod = "PayMaya",
            paymentStatus = "PAID",
            status = "COMPLETED",
            checkInTime = "3:52 PM",
            customerEmail = "customer@pickleplay.ph"
        )

        val storedUpcoming: List<com.example.data.local.BookingEntity>
        val storedPast: List<com.example.data.local.BookingEntity>
        val storedAll: List<com.example.data.local.BookingEntity>
        kotlinx.coroutines.runBlocking {
            dao.insertBookings(listOf(upcomingReservation, pastReservation))
            storedUpcoming = dao.getUserUpcomingReservations("Jonel P.", "customer@pickleplay.ph").first()
            storedPast = dao.getUserPastReservations("Jonel P.", "customer@pickleplay.ph").first()
            storedAll = dao.getAllBookings().first()
        }
        db.close()

        assertEquals(1, storedUpcoming.size)
        assertEquals("PKL-20261002-801", storedUpcoming.first().bookingCode)
        assertTrue(storedUpcoming.first().isUpcomingBooking)

        assertEquals(1, storedPast.size)
        assertEquals("PKL-20260920-802", storedPast.first().bookingCode)
        assertTrue(storedPast.first().isPastBooking)

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.MyReservationsScreen(
                    isAuthenticated = true,
                    authSession = null,
                    bookings = storedAll,
                    onBackToDashboard = {},
                    onBookNewCourt = {},
                    onOpenAuthModal = {},
                    onCancelPendingReservation = {},
                    onOpenPaymentDialog = {},
                    onOpenQrPass = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify Room database upcoming & past reservations card displays both stored lists
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("room_reservations_summary_card"))
        composeTestRule.onNodeWithTag("room_reservations_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("room_reservations_counts_title")
            .assertTextContains("Upcoming (1) & Past (1)", substring = true)
        composeTestRule.onNodeWithTag("room_upcoming_reservation_row_801").assertIsDisplayed()
        composeTestRule.onNodeWithTag("room_past_reservation_row_802").assertIsDisplayed()

        // Verify the Compose Tournament Bracket Visualizer component displays rounds and advances a team
        composeTestRule.onNodeWithTag("my_reservations_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("local_pickleball_tournament_bracket_card"))
        composeTestRule.onNodeWithTag("tournament_bracket_visualizer_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bracket_column_qf_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bracket_visual_node_1_QF1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bracket_advance_team_a_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bracket_selected_match_teams_1")
            .assertTextContains("Winner:", substring = true)
    }

    @Test
    fun matchmakerFeaturePostsOpenSlotsFiltersBySkillLevelAndJoinsSession() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            com.example.data.local.PicklePlayDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = db.picklePlayDao()

        val seededSessions = com.example.ui.screens.DefaultOpenPlayGamesList
        val intermediateFromDao: List<com.example.data.local.OpenPlayGameEntity>
        val beginnerFromDao: List<com.example.data.local.OpenPlayGameEntity>
        kotlinx.coroutines.runBlocking {
            dao.insertOpenPlayGames(seededSessions)
            intermediateFromDao = dao.getOpenPlayGamesBySkillLevel("Intermediate").first()
            beginnerFromDao = dao.getOpenPlayGamesBySkillLevel("Beginner").first()
        }
        db.close()

        // Verify Room DAO skill-level filtering (includes matching skill + "All Levels")
        assertTrue(intermediateFromDao.any { it.skillLevel == "Intermediate" })
        assertTrue(beginnerFromDao.any { it.skillLevel == "Beginner" })
        assertTrue(seededSessions.first().hasOpenSlots)
        assertEquals(3, seededSessions.first().openSlotsCount)

        var postedSlotsCount = 0
        var joinedGameTitle = ""

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.GamesAndTournamentsScreen(
                    openPlayGames = seededSessions,
                    onToggleJoinOpenPlay = { joinedGameTitle = it.title },
                    onPostOpenSlotsForGame = { _, _, _, _, _, _, _, openSlotsNeeded, _ ->
                        postedSlotsCount = openSlotsNeeded
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify Matchmaker Feature Card is displayed
        composeTestRule.onNodeWithTag("matchmaker_feature_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matchmaker_filtered_stats_badge")
            .assertTextContains("Open Slots", substring = true)

        // 2. Test Local Skill-Level Filtering via Matchmaker skill chips
        composeTestRule.onNodeWithTag("matchmaker_skill_chip_Beginner").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Beginner session (id=4) should be matched
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("open_play_card_4"))
        composeTestRule.onNodeWithTag("open_play_card_4").assertIsDisplayed()

        // Reset skill filter back to All
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("matchmaker_skill_chip_All"))
        composeTestRule.onNodeWithTag("matchmaker_skill_chip_All").performClick()
        composeTestRule.waitForIdle()

        // 3. Test Joining an Existing Open Play Session
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("join_game_button_1"))
        composeTestRule.onNodeWithTag("join_game_button_1").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Saturday Open Play", joinedGameTitle)
        composeTestRule.onNodeWithTag("open_play_vacancy_badge_1")
            .assertTextContains("2 SPOTS OPEN", substring = true)
        composeTestRule.onNodeWithTag("open_play_vacancy_text_1")
            .assertTextContains("4/6 Filled", substring = true)

        // 4. Test Posting Open Slots for a New Game via Matchmaker
        composeTestRule.onNodeWithTag("games_tournaments_list")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("matchmaker_post_open_slots_button"))
        composeTestRule.onNodeWithTag("matchmaker_post_open_slots_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("matchmaker_post_slots_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("matchmaker_open_slots_chip_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("matchmaker_post_skill_chip_Intermediate").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("confirm_post_open_slots_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, postedSlotsCount)
        composeTestRule.onNodeWithTag("matchmaker_posted_banner").assertIsDisplayed()
    }

    @Test
    fun courtReservationFormCapturesDateTimeSlotAndCourtIdAndSubmitsReservation() {
        var capturedFormInput: com.example.ui.components.CourtReservationFormInput? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    com.example.ui.components.CourtReservationForm(
                        initialDateIso = "2026-09-28",
                        initialTimeSlot = "02:00 PM",
                        initialCourtId = 1,
                        initialPlayerName = "Jonel P.",
                        onSubmitReservation = { submitted ->
                            capturedFormInput = submitted
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify form fields for Court ID, Date, and Time Slot are displayed
        composeTestRule.onNodeWithTag("court_reservation_form_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_court_id_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_date_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_time_slot_input").performScrollTo().assertIsDisplayed()

        // 2. Capture user inputs for Court ID, Date, and Time Slot
        composeTestRule.onNodeWithTag("reservation_form_court_id_input").performScrollTo().performTextReplacement("4")
        composeTestRule.onNodeWithTag("reservation_form_date_input").performScrollTo().performTextReplacement("2026-09-30")
        composeTestRule.onNodeWithTag("reservation_form_time_slot_input").performScrollTo().performTextReplacement("06:00 PM")
        composeTestRule.waitForIdle()

        // Verify live summary reflects captured Court ID #4, Date 2026-09-30, and Time Slot 06:00 PM
        composeTestRule.onNodeWithTag("reservation_form_summary_court_id")
            .performScrollTo()
            .assertTextContains("ID #4 (Court 4)", substring = true)
        composeTestRule.onNodeWithTag("reservation_form_summary_date")
            .performScrollTo()
            .assertTextContains("2026-09-30", substring = true)
        composeTestRule.onNodeWithTag("reservation_form_summary_time_slot")
            .performScrollTo()
            .assertTextContains("06:00 PM", substring = true)

        // 3. Submit the Court Reservation Form
        composeTestRule.onNodeWithTag("reservation_form_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(capturedFormInput)
        assertEquals(4, capturedFormInput?.courtId)
        assertEquals("Court 4", capturedFormInput?.courtName)
        assertEquals("2026-09-30", capturedFormInput?.dateIso)
        assertEquals("September 30, 2026", capturedFormInput?.dateLabel)
        assertEquals("06:00 PM", capturedFormInput?.timeSlot)
        assertEquals("6:00 PM - 7:00 PM", capturedFormInput?.timeRangeLabel)

        // Verify confirmation banner is displayed with the captured reservation details
        composeTestRule.onNodeWithTag("reservation_form_submitted_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_submitted_details")
            .performScrollTo()
            .assertTextContains("Court ID #4 (Court 4) • 2026-09-30 • 06:00 PM", substring = true)
    }

    @Test
    fun courtReservationFormValidationLoadingIndicatorAndSnackbarFeedback() {
        var capturedFormInput: com.example.ui.components.CourtReservationFormInput? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    com.example.ui.components.CourtReservationForm(
                        initialDateIso = "2026-09-29",
                        initialTimeSlot = "02:00 PM",
                        initialCourtId = 1,
                        referenceDateIso = "2026-09-28",
                        onSubmitReservation = { submitted ->
                            capturedFormInput = submitted
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Validate empty Court ID blocks submission and shows error snackbar
        composeTestRule.onNodeWithTag("reservation_form_court_id_input").performScrollTo().performTextReplacement("")
        composeTestRule.onNodeWithTag("reservation_form_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertNull(capturedFormInput)
        composeTestRule.onNodeWithTag("reservation_form_court_id_error")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Court ID cannot be empty", substring = true)
        composeTestRule.onNodeWithTag("reservation_form_error_snackbar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_snackbar_message")
            .performScrollTo()
            .assertTextContains("Court ID cannot be empty", substring = true)

        // 2. Validate past date (not in the future) blocks submission and shows error snackbar
        composeTestRule.onNodeWithTag("reservation_form_court_id_input").performScrollTo().performTextReplacement("4")
        composeTestRule.onNodeWithTag("reservation_form_date_input").performScrollTo().performTextReplacement("2026-09-27")
        composeTestRule.onNodeWithTag("reservation_form_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertNull(capturedFormInput)
        composeTestRule.onNodeWithTag("reservation_form_date_error")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("must be in the future", substring = true)
        composeTestRule.onNodeWithTag("reservation_form_error_snackbar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_snackbar_message")
            .performScrollTo()
            .assertTextContains("must be in the future", substring = true)

        // 3. Validate invalid Time Slot blocks submission and shows error snackbar
        composeTestRule.onNodeWithTag("reservation_form_date_input").performScrollTo().performTextReplacement("2026-09-30")
        composeTestRule.onNodeWithTag("reservation_form_time_slot_input").performScrollTo().performTextReplacement("99:99 XX")
        composeTestRule.onNodeWithTag("reservation_form_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertNull(capturedFormInput)
        composeTestRule.onNodeWithTag("reservation_form_time_slot_error")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Invalid time slot", substring = true)
        composeTestRule.onNodeWithTag("reservation_form_error_snackbar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_snackbar_message")
            .performScrollTo()
            .assertTextContains("Invalid time slot", substring = true)

        // 4. Enter valid future Date, valid Time Slot, and non-empty Court ID -> verify visual loading indicator & success snackbar
        composeTestRule.onNodeWithTag("reservation_form_time_slot_input").performScrollTo().performTextReplacement("06:00 PM")
        composeTestRule.onNodeWithTag("reservation_form_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertNotNull(capturedFormInput)
        assertEquals(4, capturedFormInput?.courtId)
        assertEquals("2026-09-30", capturedFormInput?.dateIso)
        assertEquals("06:00 PM", capturedFormInput?.timeSlot)

        // Verify visual loading indicator and success snackbar are displayed
        composeTestRule.onNodeWithTag("reservation_form_loading_indicator").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_loading_spinner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_success_snackbar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("reservation_form_snackbar_message")
            .performScrollTo()
            .assertTextContains("Reservation confirmed! Court 4 (ID #4) reserved for 2026-09-30 at 06:00 PM.", substring = true)
    }

    @Test
    fun existingCourtReservationsListFiltersUpcoming7DaysAndCancelsDocumentInFirestore() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.data.remote.FirestoreReservationRepository.clearForTesting(context)

        val sampleReservations = listOf(
            com.example.data.local.BookingEntity(
                id = 501,
                bookingCode = "PKL-20260929-50101",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 1,
                courtName = "Court 1",
                dateIso = "2026-09-29", // Within upcoming 7 days of 2026-09-28
                dateLabel = "Tuesday, Sep 29, 2026",
                timeSlot = "4:00 PM",
                timeRangeLabel = "4:00 PM - 5:00 PM",
                playerName = "Jonel P.",
                courtFee = 350,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 370,
                paymentMethod = "GCash",
                paymentStatus = "PAID",
                status = "CONFIRMED_PAID"
            ),
            com.example.data.local.BookingEntity(
                id = 502,
                bookingCode = "PKL-20261004-50202",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 3,
                courtName = "Court 3",
                dateIso = "2026-10-04", // Within upcoming 7 days (day +6 from 2026-09-28)
                dateLabel = "Sunday, Oct 4, 2026",
                timeSlot = "6:00 PM",
                timeRangeLabel = "6:00 PM - 7:00 PM",
                playerName = "Jonel P.",
                courtFee = 380,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 400,
                paymentMethod = "Maya",
                paymentStatus = "UNPAID_PENDING_APPROVAL",
                status = "PENDING_ADMIN_APPROVAL"
            ),
            com.example.data.local.BookingEntity(
                id = 503,
                bookingCode = "PKL-20261012-50303",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 5,
                courtName = "Court 5",
                dateIso = "2026-10-12", // Beyond upcoming 7 days (14 days after 2026-09-28)
                dateLabel = "Monday, Oct 12, 2026",
                timeSlot = "7:00 PM",
                timeRangeLabel = "7:00 PM - 8:00 PM",
                playerName = "Jonel P.",
                courtFee = 350,
                discountAmount = 0,
                serviceFee = 20,
                totalAmount = 370,
                paymentMethod = "GCash",
                paymentStatus = "AWAITING_PAYMENT",
                status = "APPROVED_AWAITING_PAYMENT"
            )
        )

        com.example.data.remote.FirestoreReservationRepository.syncReservationsToFirestore(
            context = context,
            bookings = sampleReservations
        )

        var cancelledReservationCode: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    )
                ) {
                    com.example.ui.components.ExistingCourtReservationsList(
                        reservations = sampleReservations,
                        referenceDateIso = "2026-09-28",
                        onCancelReservation = { cancelled ->
                            cancelledReservationCode = cancelled.bookingCode
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Default filter is All Reservations -> all 3 items (501, 502, 503) are displayed
        composeTestRule.onNodeWithTag("existing_court_reservations_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservation_item_501").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservation_item_502").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservation_item_503").performScrollTo().assertIsDisplayed()

        // 2. Toggle filter to Upcoming 7 Days -> 501 and 502 remain displayed, 503 (Oct 12) is filtered out
        composeTestRule.onNodeWithTag("existing_reservations_filter_upcoming_7_days_chip")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("existing_reservation_item_501").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservation_item_502").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservation_item_503").assertDoesNotExist()

        // 3. Toggle back to All Reservations -> 503 is visible again
        composeTestRule.onNodeWithTag("existing_reservations_filter_all_chip")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("existing_reservation_item_503").performScrollTo().assertIsDisplayed()

        // 4. Click 'Cancel' button on item 501 -> removes document from Firestore and removes item from UI
        composeTestRule.onNodeWithTag("existing_reservation_cancel_button_501")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("PKL-20260929-50101", cancelledReservationCode)
        composeTestRule.onNodeWithTag("existing_reservation_item_501").assertDoesNotExist()
        composeTestRule.onNodeWithTag("existing_reservations_cancel_feedback_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("existing_reservations_cancel_feedback_message")
            .performScrollTo()
            .assertTextContains("PKL-20260929-50101", substring = true)

        // Verify document was removed from FirestoreReservationRepository
        val remainingDocs = com.example.data.remote.FirestoreReservationRepository.reservationsFlow.value
        org.junit.Assert.assertFalse(remainingDocs.any { it.bookingCode == "PKL-20260929-50101" })
        org.junit.Assert.assertTrue(remainingDocs.any { it.bookingCode == "PKL-20261004-50202" })
    }

    @Test
    fun userProfileScreenDisplaysUserNameSkillLevelAndPastReservationsHistory() {
        val sampleUserProfile = com.example.data.local.UserProfileEntity(
            id = 1,
            name = "Jonel",
            fullName = "Jonel P.",
            city = "Quezon City",
            skillLevel = "Intermediate",
            duprRating = 3.0,
            gamesPlayed = 42,
            wins = 27,
            winStreak = 5,
            preferredPosition = "Right Side",
            membershipTier = "Player",
            membershipPrice = 499,
            discountPercent = 10
        )

        val pastCourtReservations = listOf(
            com.example.data.local.BookingEntity(
                id = 801,
                bookingCode = "PKL-20260927-00104",
                facilityId = 1,
                facilityName = "Smash Pickle Club",
                facilityLocation = "Quezon City",
                courtId = 4,
                courtName = "Court 4",
                dateIso = "2026-09-27",
                dateLabel = "September 27, 2026",
                timeSlot = "09:00 AM",
                timeRangeLabel = "9:00 AM - 10:00 AM",
                playerName = "Jonel P.",
                courtFee = 350,
                discountAmount = 35,
                serviceFee = 20,
                totalAmount = 335,
                paymentMethod = "PayMaya",
                paymentStatus = "PAID",
                status = "CHECKED_IN",
                checkInTime = "8:50 AM"
            ),
            com.example.data.local.BookingEntity(
                id = 802,
                bookingCode = "PKL-20260920-00089",
                facilityId = 2,
                facilityName = "BGC Dink & Rally Club",
                facilityLocation = "Taguig City",
                courtId = 2,
                courtName = "Court 2",
                dateIso = "2026-09-20",
                dateLabel = "September 20, 2026",
                timeSlot = "04:00 PM",
                timeRangeLabel = "4:00 PM - 5:00 PM",
                playerName = "Jonel P.",
                courtFee = 450,
                discountAmount = 45,
                serviceFee = 20,
                totalAmount = 425,
                paymentMethod = "GCash",
                paymentStatus = "PAID",
                status = "COMPLETED",
                checkInTime = "3:52 PM",
                userRating = 5,
                userReview = "Awesome cushion surface and LED lighting!"
            )
        )

        var updatedRating: Double? = null
        var updatedSkillTier: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                com.example.ui.screens.UserProfileScreen(
                    userProfile = sampleUserProfile,
                    pastReservations = pastCourtReservations,
                    onUpdateSkillLevel = { tier, rating ->
                        updatedSkillTier = tier
                        updatedRating = rating
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify User's Name and Initial Skill Level (3.0) are displayed
        composeTestRule.onNodeWithTag("user_profile_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_name")
            .assertIsDisplayed()
            .assertTextContains("Jonel P.", substring = true)
        composeTestRule.onNodeWithTag("user_profile_skill_level_value")
            .assertIsDisplayed()
            .assertTextContains("3.0", substring = true)
        composeTestRule.onNodeWithTag("user_profile_skill_summary_text")
            .assertIsDisplayed()
            .assertTextContains("DUPR 3.0", substring = true)

        // 2. Switch Skill Level from 3.0 to 4.0 and verify display updates
        composeTestRule.onNodeWithTag("user_profile_skill_chip_4_0")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(4.0, updatedRating ?: 0.0, 0.01)
        assertEquals("Advanced", updatedSkillTier)
        composeTestRule.onNodeWithTag("user_profile_skill_level_value")
            .assertTextContains("4.0", substring = true)
        composeTestRule.onNodeWithTag("user_profile_skill_summary_text")
            .assertTextContains("DUPR 4.0 (Advanced)", substring = true)

        // 3. Verify History of Past Court Reservations is displayed
        composeTestRule.onNodeWithTag("user_profile_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("user_profile_past_reservations_section"))
        composeTestRule.onNodeWithTag("user_profile_past_reservations_header")
            .assertIsDisplayed()
            .assertTextContains("Past Court Reservations History", substring = true)

        composeTestRule.onNodeWithTag("user_profile_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("user_profile_past_reservation_item_801"))
        composeTestRule.onNodeWithTag("user_profile_past_reservation_item_801").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_past_reservation_court_801")
            .assertTextContains("Court 4 (Court ID #4) • Smash Pickle Club", substring = true)
        composeTestRule.onNodeWithTag("user_profile_past_reservation_date_801")
            .assertTextContains("2026-09-27", substring = true)

        composeTestRule.onNodeWithTag("user_profile_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("user_profile_past_reservation_item_802"))
        composeTestRule.onNodeWithTag("user_profile_past_reservation_item_802").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_profile_past_reservation_court_802")
            .assertTextContains("Court 2 (Court ID #2) • BGC Dink & Rally Club", substring = true)
        composeTestRule.onNodeWithTag("user_profile_past_reservation_date_802")
            .assertTextContains("2026-09-20", substring = true)
    }

    @Test
    fun upcomingTournamentsFirestoreRegistrationAndRealtimeOpenPlaySkillQueueMatchmaking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository.resetForTesting(context)

        var registeredTournamentId: Int? = null
        var joinedQueueId: String? = null

        composeTestRule.setContent {
            PicklePlayTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())
                ) {
                    com.example.ui.components.UpcomingTournamentsFirestoreList(
                        playerName = "Jonel P.",
                        onRegisterTournament = { tournament, _ ->
                            registeredTournamentId = tournament.id
                        }
                    )
                    com.example.ui.screens.OpenPlayMatchmakingScreen(
                        playerName = "Jonel P.",
                        onQueueUpdated = { updatedQueue ->
                            joinedQueueId = updatedQueue.queueId
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // 1. Verify Upcoming Tournaments list from 'tournaments' Firestore collection is displayed
        composeTestRule.onNodeWithTag("upcoming_tournaments_firestore_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("upcoming_tournament_item_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("upcoming_tournament_name_1")
            .assertTextContains("PicklePlay Open 2026", substring = true)

        // Register for Tournament #1 in the 'tournaments' Firestore collection
        composeTestRule.onNodeWithTag("upcoming_tournament_register_button_1")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, registeredTournamentId)
        composeTestRule.onNodeWithTag("upcoming_tournament_registered_badge_1")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("upcoming_tournaments_registration_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("upcoming_tournaments_registration_message")
            .assertTextContains("PicklePlay Open 2026", substring = true)

        val persistedTournaments = com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository.tournamentsFlow.value
        assertTrue(persistedTournaments.any { it.id == 1 && it.isJoinedByUser })

        // 2. Verify Real-Time Open Play Matchmaking Screen & join queue for skill level 3.5
        composeTestRule.onNodeWithTag("open_play_matchmaking_screen")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("matchmaking_skill_selector_3_5")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("matchmaking_queue_card_queue_3_5")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("matchmaking_join_queue_button_queue_3_5")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("queue_3_5", joinedQueueId)
        composeTestRule.onNodeWithTag("matchmaking_queue_status_queue_3_5")
            .performScrollTo()
            .assertTextContains("MATCH READY (4/4)", substring = true)
        composeTestRule.onNodeWithTag("matchmaking_realtime_event_text")
            .performScrollTo()
            .assertTextContains("Joined DUPR 3.5", substring = true)

        val activeQueues = com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository.queuesFlow.value
        val queue35 = activeQueues.find { it.queueId == "queue_3_5" }
        assertNotNull(queue35)
        assertTrue(queue35!!.isCurrentUserJoined)
        assertEquals(4, queue35.filledSlots)
    }
}


