package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BookingEntity
import com.example.data.local.CourtEntity
import com.example.data.local.FacilityEntity
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.GCashBlueBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.example.viewmodel.AppSection
import com.example.viewmodel.AuthUserSession
import com.example.viewmodel.CourtOccupancyStatus
import com.example.viewmodel.UserRole

val DefaultClubCourts: List<CourtEntity> = listOf(
    CourtEntity(1, 1, "Court 1", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(2, 1, "Court 2", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(3, 1, "Court 3", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(4, 1, "Court 4", "Pro Cushion Indoor", true, "ACTIVE", 200, 250, 350, 300, 380),
    CourtEntity(5, 1, "Court 5", "Acrylic Covered Outdoor", false, "ACTIVE", 180, 220, 300, 260, 320),
    CourtEntity(6, 1, "Court 6", "Acrylic Covered Outdoor", false, "ACTIVE", 180, 220, 300, 260, 320)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    facilities: List<FacilityEntity>,
    courts: List<CourtEntity>,
    bookings: List<BookingEntity>,
    openPlayGames: List<OpenPlayGameEntity>,
    tournaments: List<TournamentEntity>,
    userProfile: UserProfileEntity?,
    authSession: AuthUserSession? = null,
    isAuthFormVisible: Boolean = false,
    courtBookingTimeLimits: Map<Int, Int> = emptyMap(),
    expiredCourtIds: Set<Int> = emptySet(),
    inUseCourtIds: Set<Int> = setOf(1, 4),
    businessName: String? = null,
    businessAddress: String? = null,
    businessContactNumber: String? = null,
    onExpireCourtTimeLimit: (Int) -> Unit = {},
    onSelectCourtToBook: (Int) -> Unit,
    onAddCourtToMyClub: () -> Unit = {},
    onSaveMyClubProfile: (FacilityEntity, String, String, String, String, String, Boolean, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onOpenBookingPass: (BookingEntity) -> Unit,
    onNavigateSection: (AppSection) -> Unit,
    onCopyPromoCode: (String) -> Unit,
    onOpenLoginModal: () -> Unit = {},
    onOpenRegisterModal: () -> Unit = {},
    onOpenRoleDashboard: () -> Unit = {},
    onSwitchRole: (UserRole) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val myClub = facilities.firstOrNull()
    val displayClubName = businessName?.takeIf { it.isNotBlank() } ?: myClub?.name ?: "Smash Pickle Club"
    val displayClubAddress = businessAddress?.takeIf { it.isNotBlank() } ?: myClub?.address ?: "Tomas Morato Ave, Quezon City"
    val displayContact = businessContactNumber?.takeIf { it.isNotBlank() } ?: "+63 917 555 0199"
    val myCourts = courts.filter { it.facilityId == (myClub?.id ?: 1) }.ifEmpty { DefaultClubCourts }
    val activeCourtsCount = myCourts.count { it.status == "ACTIVE" }

    var courtSearchQuery by remember { mutableStateOf("") }
    var courtStatusFilter by remember { mutableStateOf("All") }

    fun resolveCourtStatus(court: CourtEntity): CourtOccupancyStatus {
        val remainingSeconds = courtBookingTimeLimits[court.id] ?: 0
        return when {
            expiredCourtIds.contains(court.id) -> CourtOccupancyStatus.AVAILABLE
            remainingSeconds > 0 -> CourtOccupancyStatus.BOOKED
            inUseCourtIds.contains(court.id) -> CourtOccupancyStatus.IN_USE
            bookings.any { it.courtId == court.id && it.status == "CHECKED_IN" } -> CourtOccupancyStatus.IN_USE
            bookings.any { it.courtId == court.id && it.status == "CONFIRMED" } -> CourtOccupancyStatus.BOOKED
            else -> CourtOccupancyStatus.AVAILABLE
        }
    }

    val filteredMyCourts = remember(
        myCourts,
        courtSearchQuery,
        courtStatusFilter,
        courtBookingTimeLimits,
        expiredCourtIds,
        inUseCourtIds,
        bookings
    ) {
        filterCourtsBySearchAndStatus(
            courts = myCourts,
            searchQuery = courtSearchQuery,
            statusFilter = courtStatusFilter,
            statusResolver = ::resolveCourtStatus
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Dedicated Club Header (Business Name, Address & Contact Number)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OFFICIAL CLUB COURT APP",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayClubName,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Club Location",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$displayClubAddress • 📞 $displayContact • ${myClub?.operatingHours ?: "6:00 AM – 11:00 PM"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Club Amenities Bar
                val amenities = (myClub?.amenitiesCsv ?: "Air-conditioned,Parking,Pro Shop,Café")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    amenities.take(5).forEach { amenity ->
                        val emoji = when {
                            amenity.contains("Air", true) -> "❄️"
                            amenity.contains("Park", true) -> "🚗"
                            amenity.contains("Shop", true) -> "🛍️"
                            amenity.contains("Caf", true) -> "☕"
                            else -> "🏓"
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$emoji $amenity",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Club Hero Banner with Live Court Count & Instant Booking CTA
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(210.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateSection(AppSection.COURTS) }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_court_1790412087151),
                    contentDescription = myClub?.name ?: "Our Pickleball Courts",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color(0xFF041E14).copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$activeCourtsCount OF ${myCourts.size} COURTS OPEN TODAY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = OpticVoltDarkText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "₱200 – ₱350 / hr",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Reserve Your Court Time Slot",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${myClub?.surfaceType ?: "Pro Cushion Acrylic"} • Instant GCash, Maya & Venue Booking",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigateSection(AppSection.COURTS) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpticVolt,
                                    contentColor = OpticVoltDarkText
                                ),
                                modifier = Modifier.testTag("hero_book_court_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsTennis,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Book a Court Now",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2b. Landing Page Log In & Register Card (Opens Dashboard Based on Account Role)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("landing_auth_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDark)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (authSession == null) {
                        Surface(
                            color = OpticVolt.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACCOUNT ACCESS • CUSTOMER REGISTRATION",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVolt,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Welcome to ${myClub?.name ?: "Smash Pickle Club"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in to open your dashboard, or register a new Customer account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )

                        if (!isAuthFormVisible) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenLoginModal,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("landing_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = OpticVolt,
                                        contentColor = OpticVoltDarkText
                                    )
                                ) {
                                    Text(
                                        text = "Log In",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = onOpenRegisterModal,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("landing_register_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, OpticVolt),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text(
                                        text = "Register",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = OpticVolt.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SIGNED IN • ${authSession.role.displayName.uppercase()}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpticVolt,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = authSession.fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${authSession.email} • ${authSession.role.dashboardLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6F5E6)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onOpenRoleDashboard,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(48.dp)
                                    .testTag("landing_open_role_dashboard_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpticVolt,
                                    contentColor = OpticVoltDarkText
                                )
                            ) {
                                Text(
                                    text = authSession.role.dashboardLabel,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("landing_logout_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text(
                                    text = "Log Out",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (authSession.role == UserRole.CUSTOMER) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onNavigateSection(AppSection.MY_RESERVATIONS) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("landing_my_reservations_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, OpticVolt),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpticVolt)
                            ) {
                                Text(
                                    text = "My Reservations (Upcoming & Past Bookings)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Action Pills (Book Court, Open Play, Tournaments, Owner Dashboard)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Book Court",
                    subtitle = "${myCourts.size} club courts",
                    icon = Icons.Default.CalendarToday,
                    accent = EmeraldPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_book_court"),
                    onClick = { onNavigateSection(AppSection.COURTS) }
                )
                QuickActionCard(
                    title = "Open Play",
                    subtitle = "${openPlayGames.size} games live",
                    icon = Icons.Default.Groups,
                    accent = Color(0xFF0284C7),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_open_play"),
                    onClick = { onNavigateSection(AppSection.GAMES) }
                )
                QuickActionCard(
                    title = "Tourneys",
                    subtitle = "₱50K Open",
                    icon = Icons.Default.EmojiEvents,
                    accent = PeakAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_tournaments"),
                    onClick = { onNavigateSection(AppSection.GAMES) }
                )
            }
        }

        // 4. OUR PICKLEBALL COURTS (QR Pass & Add Court removed from landing page; only Admin can add courts)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "All Courts (${myCourts.size} Total)",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Tap any available court to reserve your slot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                CourtSearchAndFilterBar(
                    searchQuery = courtSearchQuery,
                    onSearchQueryChange = { courtSearchQuery = it },
                    selectedFilter = courtStatusFilter,
                    onFilterSelected = { courtStatusFilter = it },
                    allCount = myCourts.size,
                    availableCount = myCourts.count { resolveCourtStatus(it) == CourtOccupancyStatus.AVAILABLE },
                    bookedCount = myCourts.count { resolveCourtStatus(it) == CourtOccupancyStatus.BOOKED },
                    inUseCount = myCourts.count { resolveCourtStatus(it) == CourtOccupancyStatus.IN_USE },
                    tagPrefix = "home_court"
                )
            }
        }

        if (filteredMyCourts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No courts match \"$courtSearchQuery\" ($courtStatusFilter)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                courtSearchQuery = ""
                                courtStatusFilter = "All"
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("home_reset_court_filters_button")
                        ) {
                            Text("Reset Search & Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredMyCourts, key = { it.id }) { court ->
                val remainingSeconds = courtBookingTimeLimits[court.id] ?: 0
                val occupancyStatus = resolveCourtStatus(court)
                MyClubCourtCard(
                    court = court,
                    occupancyStatus = occupancyStatus,
                    remainingBookingSeconds = remainingSeconds,
                    onExpireTimeLimit = { onExpireCourtTimeLimit(court.id) },
                    onBookCourt = { onSelectCourtToBook(court.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 6. Active Promotions & Club Member Perks
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Club Promotions & Member Perks",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCopyPromoCode("PICKLE50") }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = OpticVolt,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "₱50 OFF Court Reservations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OpticVoltDarkText
                            )
                            Text(
                                text = "Tap to apply club promo code PICKLE50 at checkout",
                                style = MaterialTheme.typography.bodySmall,
                                color = OpticVoltDarkText.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            color = EmeraldDark,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PICKLE50",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = OpticVolt,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. Club Open Play & Tournament Highlights Carousel
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Club Open Play & Tournaments",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Explore All →",
                        style = MaterialTheme.typography.titleSmall,
                        color = EmeraldPrimary,
                        modifier = Modifier.clickable { onNavigateSection(AppSection.GAMES) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(openPlayGames.take(3), key = { it.id }) { game ->
                        val players = game.playersCsv.split(",").filter { it.isNotBlank() }
                        Surface(
                            modifier = Modifier
                                .width(268.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onNavigateSection(AppSection.GAMES) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        color = EmeraldPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = game.skillLevel.uppercase(),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "₱${game.pricePerPlayer}/player",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "🏓 ${game.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${game.dayLabel} • ${game.timeRange} • ${game.courtName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Players (${players.size}/${game.maxPlayers}): ${players.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    tournaments.firstOrNull()?.let { tourney ->
                        item {
                            Surface(
                                modifier = Modifier
                                    .width(268.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onNavigateSection(AppSection.GAMES) },
                                shape = RoundedCornerShape(18.dp),
                                color = EmeraldDark
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Surface(
                                        color = OpticVolt,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "TOURNAMENT • ${tourney.activeRound.uppercase()}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpticVoltDarkText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tourney.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${tourney.teamA} VS ${tourney.teamB}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = OpticVolt
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Live Score: ${tourney.set1A}-${tourney.set1B}, ${tourney.set2A}-${tourney.set2B}, ${tourney.set3A}-${tourney.set3B}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD6F5E6)
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

fun filterCourtsBySearchAndStatus(
    courts: List<CourtEntity>,
    searchQuery: String,
    statusFilter: String,
    statusResolver: (CourtEntity) -> CourtOccupancyStatus
): List<CourtEntity> {
    val trimmed = searchQuery.trim()
    return courts.filter { court ->
        val occupancy = statusResolver(court)
        val matchesStatus = when (statusFilter) {
            "Available" -> occupancy == CourtOccupancyStatus.AVAILABLE
            "Booked" -> occupancy == CourtOccupancyStatus.BOOKED
            "In-Use" -> occupancy == CourtOccupancyStatus.IN_USE
            else -> true
        }
        val sanitizedSurface = court.courtType.replace("Championship", "Pro Cushion", ignoreCase = true)
        val indoorLabel = if (court.isIndoor) "Indoor" else "Outdoor"
        val statusText = when (occupancy) {
            CourtOccupancyStatus.AVAILABLE -> "Available Blue"
            CourtOccupancyStatus.BOOKED -> "Booked Red"
            CourtOccupancyStatus.IN_USE -> "In-Use Green"
        }
        val matchesQuery = trimmed.isEmpty() ||
            court.name.contains(trimmed, ignoreCase = true) ||
            sanitizedSurface.contains(trimmed, ignoreCase = true) ||
            indoorLabel.contains(trimmed, ignoreCase = true) ||
            statusText.contains(trimmed, ignoreCase = true)
        matchesStatus && matchesQuery
    }
}

@Composable
fun CourtSearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    allCount: Int,
    availableCount: Int,
    bookedCount: Int,
    inUseCount: Int,
    tagPrefix: String = "court",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Search courts by name, surface, or status...",
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Courts",
                    tint = EmeraldPrimary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("${tagPrefix}_search_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${tagPrefix}_search_bar")
        )

        Spacer(modifier = Modifier.height(8.dp))

        val filterItems = listOf(
            Triple("All", "All ($allCount)", EmeraldPrimary),
            Triple("Available", "Available ($availableCount)", GCashBlue),
            Triple("Booked", "Booked ($bookedCount)", MaintenanceRed),
            Triple("In-Use", "In-Use ($inUseCount)", AvailableGreen)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${tagPrefix}_filter_chips_row")
        ) {
            items(filterItems, key = { it.first }) { (filterKey, displayLabel, accentColor) ->
                val isSelected = selectedFilter == filterKey
                val chipTag = "${tagPrefix}_filter_chip_${filterKey.lowercase().replace("-", "_")}"
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filterKey) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filterKey != "All") {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else accentColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = displayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = accentColor.copy(alpha = 0.55f),
                        selectedBorderColor = accentColor
                    ),
                    modifier = Modifier.testTag(chipTag)
                )
            }
        }
    }
}

@Composable
fun CourtColorLegendRow(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("court_status_legend"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaintenanceRedBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaintenanceRed.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("legend_red_booked")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaintenanceRed)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Red: Booked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaintenanceRed
                    )
                }
            }
            Surface(
                color = AvailableGreenBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("legend_green_in_use")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AvailableGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Green: In-Use",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AvailableGreen
                    )
                }
            }
            Surface(
                color = GCashBlueBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GCashBlue.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("legend_blue_available")
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
                        text = "Blue: Available",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GCashBlue
                    )
                }
            }
        }
    }
}

@Composable
fun MyClubCourtCard(
    court: CourtEntity,
    occupancyStatus: CourtOccupancyStatus = CourtOccupancyStatus.AVAILABLE,
    remainingBookingSeconds: Int = 0,
    onExpireTimeLimit: () -> Unit = {},
    onBookCourt: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Red for already booked (disabled during booking time limit), Green for already in use, Blue for available
    val isAlreadyBooked = occupancyStatus == CourtOccupancyStatus.BOOKED
    val isInUse = occupancyStatus == CourtOccupancyStatus.IN_USE
    val isCourtEnabled = !isAlreadyBooked

    val statusPrimaryColor = when (occupancyStatus) {
        CourtOccupancyStatus.BOOKED -> MaintenanceRed
        CourtOccupancyStatus.IN_USE -> AvailableGreen
        CourtOccupancyStatus.AVAILABLE -> GCashBlue
    }
    val statusBgColor = when (occupancyStatus) {
        CourtOccupancyStatus.BOOKED -> MaintenanceRedBg
        CourtOccupancyStatus.IN_USE -> AvailableGreenBg
        CourtOccupancyStatus.AVAILABLE -> GCashBlueBg
    }
    val timeFormatted = if (remainingBookingSeconds > 0) {
        val m = remainingBookingSeconds / 60
        val s = remainingBookingSeconds % 60
        "%02d:%02d".format(m, s)
    } else {
        "00:00"
    }
    val statusBadgeText = when (occupancyStatus) {
        CourtOccupancyStatus.BOOKED -> if (remainingBookingSeconds > 0) {
            "ALREADY BOOKED • $timeFormatted LEFT"
        } else {
            "ALREADY BOOKED"
        }
        CourtOccupancyStatus.IN_USE -> "ALREADY IN USE"
        CourtOccupancyStatus.AVAILABLE -> "AVAILABLE"
    }
    val sanitizedCourtType = court.courtType.replace("Championship", "Pro Cushion", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isCourtEnabled) { onBookCourt() }
            .testTag("club_court_card_${court.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusBgColor.copy(alpha = 0.35f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = statusPrimaryColor.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusPrimaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = court.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isAlreadyBooked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = statusBgColor,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, statusPrimaryColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = statusBadgeText,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusPrimaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$sanitizedCourtType • ${if (court.isIndoor) "❄️ Air-Conditioned Indoor" else "☀️ Covered Outdoor"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Off-Peak ₱${court.morningPrice}/hr  •  Midday ₱${court.middayPrice}/hr  •  Peak ₱${court.peakPrice}/hr",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusPrimaryColor
                    )
                    if (isAlreadyBooked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Disabled during booking time limit ($timeFormatted remaining). Automatically enables after time limit.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaintenanceRed
                        )
                    } else if (isInUse) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Players currently on court • Select another slot to book ahead",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = AvailableGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onBookCourt,
                        enabled = isCourtEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusPrimaryColor,
                            contentColor = Color.White,
                            disabledContainerColor = MaintenanceRed.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier.testTag("book_court_button_${court.id}")
                    ) {
                        Text(
                            text = when (occupancyStatus) {
                                CourtOccupancyStatus.BOOKED -> "Booked"
                                CourtOccupancyStatus.IN_USE -> "In Use"
                                CourtOccupancyStatus.AVAILABLE -> "Reserve"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isAlreadyBooked) {
                        TextButton(
                            onClick = onExpireTimeLimit,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("expire_time_limit_button_${court.id}")
                        ) {
                            Text(
                                text = "End Time Limit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GCashBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditMyClubDialog(
    facility: FacilityEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(facility.name) }
    var city by remember { mutableStateOf(facility.city) }
    var address by remember { mutableStateOf(facility.address) }
    var operatingHours by remember { mutableStateOf(facility.operatingHours) }
    var surfaceType by remember { mutableStateOf(facility.surfaceType) }
    var isIndoor by remember { mutableStateOf(facility.isIndoor) }
    var amenitiesCsv by remember { mutableStateOf(facility.amenitiesCsv) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize My Pickleball Court") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("My Club / Court Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / Area") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = operatingHours,
                    onValueChange = { operatingHours = it },
                    label = { Text("Operating Hours") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = surfaceType,
                    onValueChange = { surfaceType = it },
                    label = { Text("Court Surface Type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amenitiesCsv,
                    onValueChange = { amenitiesCsv = it },
                    label = { Text("Amenities (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Air-Conditioned Indoor Facility", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isIndoor, onCheckedChange = { isIndoor = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, city, address, operatingHours, surfaceType, isIndoor, amenitiesCsv)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save My Court Info")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
