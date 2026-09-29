package com.example.ui.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TournamentEntity
import com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText

enum class UpcomingTournamentsFilter(val label: String) {
    ALL("All Upcoming"),
    OPEN_SPOTS("Open Registration"),
    REGISTERED("My Registered")
}

/**
 * UI component that lists upcoming tournaments from the `'tournaments'` Firebase Firestore collection
 * and allows users to register for them in real time.
 */
@Composable
fun UpcomingTournamentsFirestoreList(
    modifier: Modifier = Modifier,
    tournaments: List<TournamentEntity>? = null,
    playerName: String = "Jonel P.",
    onRegisterTournament: (TournamentEntity, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirestoreTournamentsAndMatchmakingRepository.ensureInitialized(context)
    }

    val firestoreTournaments by FirestoreTournamentsAndMatchmakingRepository.tournamentsFlow.collectAsState()
    val lastRegistrationRecord by FirestoreTournamentsAndMatchmakingRepository.lastRegistrationRecord.collectAsState()

    var activeFilter by remember { mutableStateOf(UpcomingTournamentsFilter.ALL) }
    var locallyRegisteredIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var localPartnerMap by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val mergedTournaments = remember(
        tournaments,
        firestoreTournaments,
        locallyRegisteredIds,
        localPartnerMap
    ) {
        val base = if (!tournaments.isNullOrEmpty()) {
            val firestoreById = firestoreTournaments.associateBy { it.id }
            tournaments.map { passed ->
                val remote = firestoreById[passed.id]
                if (remote != null && remote.isJoinedByUser) remote else passed
            }
        } else {
            firestoreTournaments
        }

        base.map { item ->
            if (locallyRegisteredIds.contains(item.id) && !item.isJoinedByUser) {
                val partner = localPartnerMap[item.id] ?: "Carlo V."
                val teamLabel = if (item.division.equals("Singles", ignoreCase = true)) {
                    playerName
                } else {
                    "$playerName / $partner"
                }
                item.copy(
                    isJoinedByUser = true,
                    registeredCount = (item.registeredCount + 1).coerceAtMost(item.maxTeams),
                    teamA = teamLabel
                )
            } else {
                item
            }
        }
    }

    val displayedTournaments = remember(mergedTournaments, activeFilter) {
        when (activeFilter) {
            UpcomingTournamentsFilter.ALL -> mergedTournaments
            UpcomingTournamentsFilter.OPEN_SPOTS -> mergedTournaments.filter {
                !it.isJoinedByUser && it.registeredCount < it.maxTeams
            }
            UpcomingTournamentsFilter.REGISTERED -> mergedTournaments.filter { it.isJoinedByUser }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_tournaments_firestore_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("upcoming_tournaments_firestore_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Banner showing 'tournaments' Firestore Collection sync
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF064E3B), Color(0xFF047857))
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("upcoming_tournaments_collection_badge"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Firestore Tournaments Collection",
                                    tint = OpticVoltDarkText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "FIRESTORE COLLECTION: '${FirestoreTournamentsAndMatchmakingRepository.COLLECTION_TOURNAMENTS}'",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpticVoltDarkText
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Upcoming Tournaments & Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("upcoming_tournaments_header_title")
                        )
                        Text(
                            text = "${mergedTournaments.size} Sanctioned Events • ${mergedTournaments.count { it.isJoinedByUser }} Registered",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6),
                            modifier = Modifier.testTag("upcoming_tournaments_subtitle")
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${mergedTournaments.size} EVENTS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Registration Confirmation Feedback Banner
            val bannerMessage = feedbackMessage ?: lastRegistrationRecord?.let {
                "Registered ${it.registeredTeamLabel} for ${it.tournamentName} (${it.division}) • Saved to Firestore '${FirestoreTournamentsAndMatchmakingRepository.COLLECTION_TOURNAMENTS}/${it.tournamentId}'"
            }
            if (bannerMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upcoming_tournaments_registration_banner"),
                    shape = RoundedCornerShape(12.dp),
                    color = AvailableGreenBg,
                    border = BorderStroke(1.dp, AvailableGreen)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tournament Registration Confirmed",
                            tint = AvailableGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = bannerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upcoming_tournaments_registration_message")
                        )
                    }
                }
            }

            // Filter Pills: All Upcoming / Open Registration / My Registered
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UpcomingTournamentsFilter.entries.forEach { filterOption ->
                    val isSelected = activeFilter == filterOption
                    val tag = when (filterOption) {
                        UpcomingTournamentsFilter.ALL -> "upcoming_tournaments_filter_all"
                        UpcomingTournamentsFilter.OPEN_SPOTS -> "upcoming_tournaments_filter_open"
                        UpcomingTournamentsFilter.REGISTERED -> "upcoming_tournaments_filter_registered"
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeFilter = filterOption }
                            .testTag(tag),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Upcoming Tournament Items
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedTournaments.forEach { tournament ->
                    UpcomingTournamentItemCard(
                        tournament = tournament,
                        playerName = playerName,
                        onRegister = { partnerName ->
                            locallyRegisteredIds = locallyRegisteredIds + tournament.id
                            localPartnerMap = localPartnerMap + (tournament.id to partnerName)
                            val updated = FirestoreTournamentsAndMatchmakingRepository.registerForTournamentInFirestore(
                                context = context,
                                tournament = tournament,
                                playerName = playerName,
                                partnerName = partnerName
                            )
                            feedbackMessage =
                                "Registered ${updated.teamA} for ${updated.name} (${updated.division}) • Saved to Firestore '${FirestoreTournamentsAndMatchmakingRepository.COLLECTION_TOURNAMENTS}/${updated.id}'"
                            onRegisterTournament(updated, partnerName)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingTournamentItemCard(
    tournament: TournamentEntity,
    playerName: String,
    onRegister: (partnerName: String) -> Unit
) {
    val isSingles = tournament.division.equals("Singles", ignoreCase = true)
    var partnerInput by remember(tournament.id) { mutableStateOf("Carlo V.") }
    val spotsRemaining = (tournament.maxTeams - tournament.registeredCount).coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("upcoming_tournament_item_${tournament.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(
            width = if (tournament.isJoinedByUser) 1.5.dp else 1.dp,
            color = if (tournament.isJoinedByUser) AvailableGreen else EmeraldPrimary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Event Name & Division / Skill Cap Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = EmeraldDark,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = tournament.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("upcoming_tournament_name_${tournament.id}")
                        )
                        Text(
                            text = "${tournament.facilityName} • ${tournament.dateRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("upcoming_tournament_details_${tournament.id}")
                        )
                    }
                }

                Surface(
                    color = if (tournament.isJoinedByUser) AvailableGreenBg else EmeraldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (tournament.isJoinedByUser) AvailableGreen else EmeraldPrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (tournament.isJoinedByUser) {
                            "REGISTERED ✓"
                        } else {
                            "${tournament.division.uppercase()} • ${tournament.skillCap}"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (tournament.isJoinedByUser) AvailableGreen else EmeraldPrimary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("upcoming_tournament_status_badge_${tournament.id}")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Format, Entry Fee, Prize Pool & Team Slots Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Format: ${tournament.format} • Skill: ${tournament.skillCap}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${tournament.registeredCount}/${tournament.maxTeams} Teams ($spotsRemaining Open)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary,
                    modifier = Modifier.testTag("upcoming_tournament_spots_${tournament.id}")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Entry Fee: ₱${tournament.entryFee} • Prize Pool: ₱${tournament.prizePool}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Active Court: ${tournament.activeCourt}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = EmeraldPrimary
                )
            }

            // Registration Controls
            if (tournament.isJoinedByUser) {
                Surface(
                    color = AvailableGreenBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upcoming_tournament_registered_badge_${tournament.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AvailableGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Registered in Firestore ('tournaments/${tournament.id}') • Team: ${tournament.teamA}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isSingles) {
                        OutlinedTextField(
                            value = partnerInput,
                            onValueChange = { partnerInput = it },
                            label = { Text("Doubles Partner") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upcoming_tournament_partner_input_${tournament.id}")
                        )
                    } else {
                        Text(
                            text = "Player: $playerName (Singles Entry)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = { onRegister(partnerInput) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("upcoming_tournament_register_button_${tournament.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsTennis,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Register",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
