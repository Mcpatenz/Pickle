package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CourtEntity
import com.example.ui.components.AuthLoginRegisterDialog
import com.example.ui.components.AvailableCourtReservationSheet
import com.example.ui.components.BookingCheckoutDialog
import com.example.ui.components.DigitalBookingPassDialog
import com.example.ui.components.MockEmailNotificationSummaryCard
import com.example.ui.components.NotificationsDialog
import com.example.ui.screens.AdminAndStaffScreen
import com.example.ui.screens.CashierDashboardScreen
import com.example.ui.screens.CourtsAndBookingScreen
import com.example.ui.screens.CourtsOverviewScreen
import com.example.ui.screens.CustomerDashboardScreen
import com.example.ui.screens.CustomerPaymentSubmissionDialog
import com.example.ui.screens.GamesAndTournamentsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyReservationsScreen
import com.example.ui.screens.ProfileAndPassScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PicklePlayTheme
import com.example.viewmodel.AppSection
import com.example.viewmodel.AuthUserSession
import com.example.viewmodel.PicklePlayViewModel
import com.example.viewmodel.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PicklePlayTheme {
                PicklePlayApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PicklePlayApp(viewModel: PicklePlayViewModel = viewModel()) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val facilities by viewModel.facilities.collectAsStateWithLifecycle()
    val courts by viewModel.courts.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val openPlayGames by viewModel.openPlayGames.collectAsStateWithLifecycle()
    val tournaments by viewModel.tournaments.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val showAuthModal by viewModel.showAuthModal.collectAsStateWithLifecycle()
    val authModalInitialTab by viewModel.authModalInitialTab.collectAsStateWithLifecycle()
    val registeredAccounts by viewModel.registeredAccounts.collectAsStateWithLifecycle()

    val selectedFacilityId by viewModel.selectedFacilityId.collectAsStateWithLifecycle()
    val selectedCourtId by viewModel.selectedCourtId.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedTimeSlot by viewModel.selectedTimeSlot.collectAsStateWithLifecycle()
    val lockSecondsRemaining by viewModel.lockSecondsRemaining.collectAsStateWithLifecycle()
    val courtBookingTimeLimits by viewModel.courtBookingTimeLimits.collectAsStateWithLifecycle()
    val expiredCourtIds by viewModel.expiredCourtIds.collectAsStateWithLifecycle()
    val inUseCourtIds by viewModel.inUseCourtIds.collectAsStateWithLifecycle()
    val showAvailableCourtSheet by viewModel.showAvailableCourtSheet.collectAsStateWithLifecycle()
    val selectedAvailableCourtId by viewModel.selectedAvailableCourtId.collectAsStateWithLifecycle()

    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
    val promoCodeInput by viewModel.promoCodeInput.collectAsStateWithLifecycle()
    val appliedPromoDiscount by viewModel.appliedPromoDiscount.collectAsStateWithLifecycle()

    val showCheckoutSheet by viewModel.showCheckoutSheet.collectAsStateWithLifecycle()
    val activeBookingPass by viewModel.activeBookingPass.collectAsStateWithLifecycle()
    val showNotificationsSheet by viewModel.showNotificationsSheet.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val businessSettings by viewModel.businessSettings.collectAsStateWithLifecycle()
    val clubProducts by viewModel.clubProducts.collectAsStateWithLifecycle()
    val lastReservedPlayerName by viewModel.lastReservedPlayerName.collectAsStateWithLifecycle()
    val latestEmailNotification by viewModel.latestEmailNotification.collectAsStateWithLifecycle()
    val isWalkInBookingMode by viewModel.isWalkInBooking.collectAsStateWithLifecycle()
    val bookingForPaymentUpload by viewModel.bookingForPaymentUpload.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // BackHandler for non-Home sections as mandated by guidelines
    BackHandler(enabled = currentSection != AppSection.HOME) {
        viewModel.navigateTo(AppSection.HOME)
    }

    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("top_screen_header_with_legend")
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.navigateTo(AppSection.HOME) }
                        ) {
                            Surface(
                                color = EmeraldDark,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SportsTennis,
                                        contentDescription = null,
                                        tint = OpticVolt,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PicklePlay",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Notification icon removed from landing page header per user request
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentSection == AppSection.HOME,
                    onClick = { viewModel.navigateTo(AppSection.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (currentSection == AppSection.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = OpticVolt
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.COURTS,
                    onClick = { viewModel.navigateTo(AppSection.COURTS) },
                    icon = {
                        Icon(
                            imageVector = if (currentSection == AppSection.COURTS) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Bookings"
                        )
                    },
                    label = { Text("Bookings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = OpticVolt
                    ),
                    modifier = Modifier.testTag("nav_courts")
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.GAMES,
                    onClick = { viewModel.navigateTo(AppSection.GAMES) },
                    icon = {
                        Icon(
                            imageVector = if (currentSection == AppSection.GAMES) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                            contentDescription = "Games"
                        )
                    },
                    label = { Text("Games") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = OpticVolt
                    ),
                    modifier = Modifier.testTag("nav_games")
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.COURT_OVERVIEW,
                    onClick = { viewModel.navigateTo(AppSection.COURT_OVERVIEW) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = "Court"
                        )
                    },
                    label = { Text("Court") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = OpticVolt
                    ),
                    modifier = Modifier.testTag("nav_court_overview")
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.PROFILE,
                    onClick = { viewModel.navigateTo(AppSection.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (currentSection == AppSection.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDark,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = OpticVolt
                    ),
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentSection) {
                AppSection.HOME -> {
                    HomeScreen(
                        facilities = facilities,
                        courts = courts,
                        bookings = bookings,
                        openPlayGames = openPlayGames,
                        tournaments = tournaments,
                        userProfile = userProfile,
                        authSession = authSession,
                        isAuthFormVisible = showAuthModal,
                        courtBookingTimeLimits = courtBookingTimeLimits,
                        expiredCourtIds = expiredCourtIds,
                        inUseCourtIds = inUseCourtIds,
                        businessName = businessSettings.businessName,
                        businessAddress = businessSettings.address,
                        businessContactNumber = businessSettings.contactNumber,
                        onExpireCourtTimeLimit = viewModel::expireCourtBookingTimeLimit,
                        onSelectCourtToBook = { courtId ->
                            viewModel.selectCourtToBook(courtId)
                        },
                        onAddCourtToMyClub = {
                            viewModel.addCourtToFacility(1, true, "Pro Cushion Indoor")
                        },
                        onSaveMyClubProfile = viewModel::saveMyClubProfile,
                        onOpenBookingPass = { booking ->
                            viewModel.openBookingPass(booking)
                        },
                        onNavigateSection = viewModel::navigateTo,
                        onCopyPromoCode = { code ->
                            viewModel.updatePromoCode(code)
                            viewModel.postToast("Applied promo code $code (-₱50) for your next booking!")
                        },
                        onOpenLoginModal = { viewModel.openAuthModal(isRegister = false) },
                        onOpenRegisterModal = { viewModel.openAuthModal(isRegister = true) },
                        onOpenRoleDashboard = viewModel::openRoleBasedDashboard,
                        onSwitchRole = viewModel::switchUserRole,
                        onLogout = viewModel::logout
                    )
                }

                AppSection.CUSTOMER_DASHBOARD -> {
                    val myClub = facilities.firstOrNull()
                    val primaryCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        RoleSessionBanner(
                            session = authSession,
                            onSwitchAccount = { viewModel.openAuthModal(isRegister = false) },
                            onLogout = viewModel::logout
                        )
                        CustomerDashboardScreen(
                            modifier = Modifier.weight(1f),
                            facility = myClub,
                            courts = primaryCourts,
                            bookings = bookings,
                            openPlayGames = openPlayGames,
                            userProfile = userProfile,
                            authSession = authSession,
                            lastReservedPlayerName = lastReservedPlayerName,
                            onSelectCourtToBook = viewModel::selectCourtToBook,
                            onOpenBookingPass = viewModel::openBookingPass,
                            onOpenMyReservations = viewModel::openMyReservationsScreen,
                            onOpenPaymentDialog = viewModel::openCustomerPaymentDialog,
                            onCancelPendingReservation = viewModel::cancelPendingCustomerReservation,
                            onToggleJoinOpenPlay = viewModel::toggleJoinOpenPlay,
                            onApplyPromoCode = { code ->
                                viewModel.updatePromoCode(code)
                                viewModel.postToast("Applied promo code $code (-₱50) for your next booking!")
                            },
                            onSimulateStatusUpdate = viewModel::triggerStatusUpdateAlert,
                            onCheckInBooking = viewModel::performQrCheckIn
                        )
                    }
                }

                AppSection.MY_RESERVATIONS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RoleSessionBanner(
                            session = authSession,
                            onSwitchAccount = { viewModel.openAuthModal(isRegister = false) },
                            onLogout = viewModel::logout
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            MyReservationsScreen(
                                isAuthenticated = authSession != null,
                                authSession = authSession,
                                bookings = bookings,
                                onBackToDashboard = {
                                    if (authSession != null) {
                                        viewModel.navigateTo(AppSection.CUSTOMER_DASHBOARD)
                                    } else {
                                        viewModel.navigateTo(AppSection.HOME)
                                    }
                                },
                                onBookNewCourt = { viewModel.navigateTo(AppSection.COURTS) },
                                onOpenAuthModal = { viewModel.openAuthModal(isRegister = false) },
                                onCancelPendingReservation = viewModel::cancelPendingCustomerReservation,
                                onOpenPaymentDialog = viewModel::openCustomerPaymentDialog,
                                onOpenQrPass = viewModel::openBookingPass,
                                onSimulateStatusUpdate = viewModel::triggerStatusUpdateAlert,
                                onCheckInBooking = viewModel::performQrCheckIn
                            )
                        }
                    }
                }

                AppSection.COURTS -> {
                    CourtsAndBookingScreen(
                        facilities = facilities,
                        courts = courts,
                        bookings = bookings,
                        selectedCourtId = selectedCourtId,
                        selectedDate = selectedDate,
                        selectedTimeSlot = selectedTimeSlot,
                        dateOptions = viewModel.dateOptions,
                        timeSlotOptions = viewModel.timeSlotOptions,
                        lockSecondsRemaining = lockSecondsRemaining,
                        courtBookingTimeLimits = courtBookingTimeLimits,
                        expiredCourtIds = expiredCourtIds,
                        inUseCourtIds = inUseCourtIds,
                        currentPlayerName = lastReservedPlayerName ?: authSession?.fullName,
                        selectedPaymentMethod = selectedPaymentMethod,
                        businessSettings = businessSettings,
                        onSelectPaymentMethod = viewModel::selectPaymentMethod,
                        onOpenBookingPass = viewModel::openBookingPass,
                        onExpireCourtTimeLimit = viewModel::expireCourtBookingTimeLimit,
                        onSelectDate = viewModel::selectDate,
                        onSelectCourt = viewModel::selectCourt,
                        onSelectTimeSlot = viewModel::selectTimeSlot,
                        calculateSlotPrice = viewModel::calculateSlotPrice,
                        onOpenCheckoutSheet = { viewModel.requestOpenCheckoutSheet() },
                        onSubmitReservationForm = viewModel::submitCourtReservationForm
                    )
                }

                AppSection.COURT_OVERVIEW -> {
                    CourtsOverviewScreen(
                        facilities = facilities,
                        courts = courts,
                        bookings = bookings,
                        courtBookingTimeLimits = courtBookingTimeLimits,
                        expiredCourtIds = expiredCourtIds,
                        inUseCourtIds = inUseCourtIds,
                        onSelectCourtToBook = viewModel::selectCourtToBook,
                        onExpireCourtTimeLimit = viewModel::expireCourtBookingTimeLimit
                    )
                }

                AppSection.GAMES -> {
                    GamesAndTournamentsScreen(
                        openPlayGames = openPlayGames,
                        tournaments = tournaments,
                        onToggleJoinOpenPlay = viewModel::toggleJoinOpenPlay,
                        onHostOpenPlay = viewModel::hostNewOpenPlay,
                        onPostOpenSlotsForGame = viewModel::postOpenSlotsForGame,
                        onRegisterTournament = viewModel::registerForTournament,
                        onSubmitScore = viewModel::submitMatchScore
                    )
                }

                AppSection.PROFILE -> {
                    UserProfileScreen(
                        userProfile = userProfile,
                        userNameOverride = authSession?.fullName,
                        allBookings = bookings,
                        onUpdateSkillLevel = viewModel::updateUserSkillLevel,
                        onUpdateUserName = viewModel::updateUserDisplayName,
                        onUpdatePreferences = viewModel::updatePlayerPreferences,
                        onSelectMembership = viewModel::selectMembershipTier,
                        onOpenBookingPass = viewModel::openBookingPass,
                        onRebookCourt = { booking ->
                            viewModel.selectCourtToBook(booking.courtId)
                        }
                    )
                }

                AppSection.CASHIER -> {
                    val myClub = facilities.firstOrNull()
                    val primaryCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        RoleSessionBanner(
                            session = authSession,
                            onSwitchAccount = { viewModel.openAuthModal(isRegister = false) },
                            onLogout = viewModel::logout
                        )
                        CashierDashboardScreen(
                            modifier = Modifier.weight(1f),
                            facility = myClub,
                            courts = primaryCourts,
                            bookings = bookings,
                            clubProducts = clubProducts,
                            businessSettings = businessSettings,
                            onCollectPayment = { b, method -> viewModel.collectCashierPayment(b, method) },
                            onVerifyPayment = viewModel::verifyBookingPaymentByCashier,
                            onCheckInBooking = viewModel::performQrCheckIn,
                            onCreateWalkInBooking = viewModel::createWalkInPosBooking
                        )
                    }
                }

                AppSection.ADMIN -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RoleSessionBanner(
                            session = authSession,
                            onSwitchAccount = { viewModel.openAuthModal(isRegister = false) },
                            onLogout = viewModel::logout
                        )
                        AdminAndStaffScreen(
                            facilities = facilities,
                            courts = courts,
                            bookings = bookings,
                            openPlayGames = openPlayGames,
                            userProfile = userProfile,
                            cashierAccounts = registeredAccounts.filter { it.role == UserRole.CASHIER },
                            onAddCashierAccount = { name, email, pass -> viewModel.addCashierFromAdmin(name, email, pass) },
                            onAddStaffAccount = viewModel::addStaffFromAdmin,
                            onRemoveCashierAccount = viewModel::removeCashierFromAdmin,
                            businessSettings = businessSettings,
                            onSaveBusinessSettings = viewModel::saveBusinessSettings,
                            onUploadGcashQr = viewModel::uploadGcashQrCode,
                            onUploadPaymayaQr = viewModel::uploadPayMayaQrCode,
                            clubProducts = clubProducts,
                            onAddClubProduct = viewModel::addClubProduct,
                            onRemoveClubProduct = viewModel::removeClubProduct,
                            onApproveBooking = viewModel::approveBookingByAdmin,
                            onRejectBooking = { b -> viewModel.rejectBookingByAdmin(b) },
                            onSaveMyClubProfile = viewModel::saveMyClubProfile,
                            onToggleCourtStatus = viewModel::toggleCourtStatus,
                            onUpdateCourtPricing = viewModel::saveCourtPricing,
                            onSaveCourtStatusAndPricing = viewModel::saveCourtStatusAndPricing,
                            onAddCourt = viewModel::addCourtToFacility,
                            onCheckInBooking = viewModel::performQrCheckIn,
                            onCollectCashierPayment = { b, method -> viewModel.collectCashierPayment(b, method) },
                            onCreateWalkInBooking = viewModel::createWalkInPosBooking,
                            onSelectCourtToBook = viewModel::selectCourtToBook,
                            onOpenBookingPass = viewModel::openBookingPass,
                            onToggleJoinOpenPlay = viewModel::toggleJoinOpenPlay,
                            tournaments = tournaments,
                            onCreateTournament = viewModel::createTournamentFromAdmin,
                            onUpdateTournamentBracket = viewModel::updateTournamentBracketFromAdmin
                        )
                    }
                }
            }
        }
    }

    // Log In Form (LoginScreen) / Customer Registration Form (RegisterScreen) Modal
    if (showAuthModal) {
        AuthLoginRegisterDialog(
            initialTab = authModalInitialTab,
            registeredAccounts = registeredAccounts,
            onLogin = viewModel::loginWithCredentials,
            onRegister = viewModel::registerCustomerAccount,
            onDismiss = viewModel::closeAuthModal
        )
    }

    // Interactive Available (Blue) Court Reservation Bottom Sheet / Dialog
    if (showAvailableCourtSheet) {
        val targetId = selectedAvailableCourtId ?: selectedCourtId
        val activeFacility = facilities.find { it.id == selectedFacilityId } ?: facilities.firstOrNull()
        val targetCourt = courts.find { it.id == targetId } ?: CourtEntity(
            id = targetId,
            facilityId = activeFacility?.id ?: 1,
            name = "Court $targetId",
            courtType = "Pro Cushion Indoor",
            isIndoor = true,
            status = "ACTIVE",
            morningPrice = 200,
            middayPrice = 250,
            peakPrice = 350,
            nightPrice = 300,
            weekendPrice = 380
        )
        val baseHourlyRate = viewModel.calculateSlotPrice(targetCourt, selectedDate, selectedTimeSlot)
        AvailableCourtReservationSheet(
            court = targetCourt,
            facilityName = activeFacility?.name ?: businessSettings.businessName,
            defaultPlayerName = authSession?.fullName ?: lastReservedPlayerName ?: "",
            baseHourlyRate = baseHourlyRate,
            durationOptions = viewModel.reservationDurationOptions,
            selectedPaymentMethod = selectedPaymentMethod,
            businessSettings = businessSettings,
            defaultRecipientEmail = authSession?.email ?: "",
            dateLabel = selectedDate.fullLabel,
            timeSlotLabel = selectedTimeSlot?.startTime ?: "2:00 PM",
            dateOptions = viewModel.dateOptions,
            selectedDateOption = selectedDate,
            onSelectDateOption = viewModel::selectDate,
            timeSlotOptions = viewModel.timeSlotOptions,
            selectedTimeSlotOption = selectedTimeSlot,
            bookedTimeSlotsForCourt = viewModel.getBookedTimeSlotsForCourt(targetCourt.id, selectedDate.isoDate),
            onSelectTimeSlotOption = viewModel::selectTimeSlot,
            isWalkInBooking = isWalkInBookingMode,
            onWalkInModeChange = viewModel::setWalkInBookingMode,
            onPaymentMethodSelect = viewModel::selectPaymentMethod,
            onConfirmReservation = { playerName, durationOption ->
                viewModel.confirmAvailableCourtQuickReservation(
                    court = targetCourt,
                    playerName = playerName,
                    durationOption = durationOption
                )
            },
            onOpenFullSchedule = {
                viewModel.openFullScheduleForCourt(targetCourt.id)
            },
            onDismiss = viewModel::closeAvailableCourtSheet
        )
    }

    // Checkout Modal
    if (showCheckoutSheet) {
        val activeFacility = facilities.find { it.id == selectedFacilityId } ?: facilities.firstOrNull()
        val activeCourt = courts.find { it.id == selectedCourtId } ?: courts.firstOrNull()
        val slot = selectedTimeSlot
        if (activeFacility != null && activeCourt != null && slot != null) {
            val baseFee = viewModel.calculateSlotPrice(activeCourt, selectedDate, slot)
            BookingCheckoutDialog(
                facility = activeFacility,
                court = activeCourt,
                dateOption = selectedDate,
                timeSlot = slot,
                lockSecondsRemaining = lockSecondsRemaining,
                courtFee = baseFee,
                userProfile = userProfile,
                promoCodeInput = promoCodeInput,
                appliedPromoDiscount = appliedPromoDiscount,
                selectedPaymentMethod = selectedPaymentMethod,
                businessSettings = businessSettings,
                isWalkInMode = isWalkInBookingMode,
                onToggleWalkInMode = viewModel::setWalkInBookingMode,
                onPromoCodeChange = viewModel::updatePromoCode,
                onPaymentMethodSelect = viewModel::selectPaymentMethod,
                onConfirmReservation = { viewModel.confirmCourtReservation() },
                onDismiss = { viewModel.setShowCheckoutSheet(false) }
            )
        }
    }

    // Customer Payment Proof & Reference Number Upload Dialog (for Admin-approved bookings)
    bookingForPaymentUpload?.let { targetBooking ->
        CustomerPaymentSubmissionDialog(
            booking = targetBooking,
            businessSettings = businessSettings,
            onSubmitPaymentProof = { paymentMethod, refNum, receiptUri, receiptFileName ->
                viewModel.submitCustomerPaymentProof(
                    booking = targetBooking,
                    paymentMethod = paymentMethod,
                    referenceNumber = refNum,
                    receiptUri = receiptUri,
                    receiptFileName = receiptFileName
                )
            },
            onDismiss = viewModel::closeCustomerPaymentDialog
        )
    }

    // Digital Booking Pass Modal
    activeBookingPass?.let { passBooking ->
        DigitalBookingPassDialog(
            booking = passBooking,
            emailSummary = latestEmailNotification,
            onAddToCalendar = {
                viewModel.postToast("Added ${passBooking.facilityName} (${passBooking.timeRangeLabel}) to Calendar!")
            },
            onSimulateQrCheckIn = { b ->
                viewModel.performQrCheckIn(b)
            },
            onCancelBooking = { b, hrs ->
                viewModel.cancelBookingWithPolicy(b, hrs)
            },
            onSubmitRating = { b, stars, comment ->
                viewModel.rateBooking(b, stars, comment)
            },
            onDismiss = { viewModel.openBookingPass(null) }
        )
    }

    // Triggered Mock Email Notification Summary Overlay (when sheets/dialogs close after confirming reservation)
    if (!showAvailableCourtSheet && !showCheckoutSheet && activeBookingPass == null) {
        latestEmailNotification?.let { emailSummary ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 84.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                MockEmailNotificationSummaryCard(
                    summary = emailSummary,
                    onResend = viewModel::resendLatestEmailNotification,
                    onDismiss = viewModel::dismissLatestEmailNotification
                )
            }
        }
    }

    // Notifications Modal
    if (showNotificationsSheet) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { viewModel.setShowNotificationsSheet(false) }
        )
    }
}

@Composable
private fun RoleSessionBanner(
    session: AuthUserSession?,
    onSwitchAccount: () -> Unit,
    onLogout: () -> Unit
) {
    if (session == null) return
    Surface(
        color = EmeraldDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ROLE: ${session.role.displayName.uppercase()} • ${session.role.dashboardLabel.uppercase()}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpticVolt
                )
                Text(
                    text = "${session.fullName} (${session.email})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("dashboard_logout_button")
                ) {
                    Text(
                        text = "Log Out",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
