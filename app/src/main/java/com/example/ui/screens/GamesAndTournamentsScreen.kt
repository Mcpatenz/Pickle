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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.TournamentEntity
import com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository
import com.example.ui.components.MatchmakerFeatureCard
import com.example.ui.components.PickleballTournamentBracketVisualizer
import com.example.ui.components.TournamentBracketStore
import com.example.ui.components.UpcomingTournamentsFirestoreList
import com.example.ui.components.buildMatchmakerOpenSlotGame
import com.example.ui.components.filterOpenPlayGamesBySkill
import com.example.ui.components.resolveOpenPlaySessionMetrics
import com.example.ui.components.resolveSkillBandLabel
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.MaintenanceRedBg
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber

val DefaultOpenPlayGamesList = listOf(
    OpenPlayGameEntity(
        id = 1,
        title = "Saturday Open Play",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        courtName = "Court 1 & Court 2",
        dayLabel = "Saturday",
        dateLabel = "Oct 3, 2026",
        timeRange = "5:00 PM - 7:00 PM",
        skillLevel = "Intermediate",
        pricePerPlayer = 150,
        maxPlayers = 6,
        playersCsv = "Jon,Maria,Carlo",
        isJoinedByUser = false,
        hostName = "Coach Anton"
    ),
    OpenPlayGameEntity(
        id = 2,
        title = "Morning Dink & Coffee Social",
        facilityId = 1,
        facilityName = "Smash Pickle Club",
        courtName = "Court 4",
        dayLabel = "Sunday",
        dateLabel = "September 27, 2026",
        timeRange = "8:00 AM - 10:00 AM",
        skillLevel = "All Levels",
        pricePerPlayer = 120,
        maxPlayers = 6,
        playersCsv = "Jonel,Sofia,Miggy,Tricia",
        isJoinedByUser = true,
        hostName = "Jonel P."
    ),
    OpenPlayGameEntity(
        id = 3,
        title = "BGC DUPR 3.5+ Competitive Rotation",
        facilityId = 2,
        facilityName = "BGC Dink & Rally Club",
        courtName = "Center Court 1",
        dayLabel = "Monday",
        dateLabel = "September 28, 2026",
        timeRange = "7:00 PM - 9:00 PM",
        skillLevel = "Advanced",
        pricePerPlayer = 200,
        maxPlayers = 6,
        playersCsv = "Mark,James,Kevin,Rico,Luis",
        isJoinedByUser = false,
        hostName = "Mark S."
    ),
    OpenPlayGameEntity(
        id = 4,
        title = "Beginner Kitchen Rules & Rally",
        facilityId = 3,
        facilityName = "Palms Rooftop Pickleball",
        courtName = "Court 2",
        dayLabel = "Tuesday",
        dateLabel = "September 29, 2026",
        timeRange = "6:00 PM - 8:00 PM",
        skillLevel = "Beginner",
        pricePerPlayer = 130,
        maxPlayers = 6,
        playersCsv = "Aya,Dan",
        isJoinedByUser = false,
        hostName = "Coach Bea"
    )
)

val DefaultTournamentsList = listOf(
    TournamentEntity(
        id = 1,
        name = "PicklePlay Open 2026",
        facilityName = "Smash Pickle Club • Quezon City",
        dateRange = "Sept 28 – 30, 2026",
        division = "Doubles",
        format = "Knockout",
        skillCap = "DUPR 3.5 – 4.0",
        entryFee = 850,
        prizePool = 50000,
        registeredCount = 16,
        maxTeams = 16,
        isJoinedByUser = true,
        activeRound = "Quarter Finals",
        activeCourt = "Court 2",
        teamA = "Jonel / Carlo",
        teamB = "Mark / James",
        set1A = 11,
        set1B = 8,
        set2A = 8,
        set2B = 11,
        set3A = 11,
        set3B = 7,
        matchSubmitted = false
    ),
    TournamentEntity(
        id = 2,
        name = "Metro Manila Mixed Doubles Cup",
        facilityName = "BGC Dink & Rally Club • Taguig",
        dateRange = "Oct 10 – 11, 2026",
        division = "Mixed Doubles",
        format = "Round Robin",
        skillCap = "Intermediate 3.0+",
        entryFee = 750,
        prizePool = 35000,
        registeredCount = 11,
        maxTeams = 16,
        isJoinedByUser = false,
        activeRound = "Group Stage Pool A",
        activeCourt = "Court 1",
        teamA = "Maria / Jon",
        teamB = "Sofia / Miggy",
        set1A = 11,
        set1B = 9,
        set2A = 11,
        set2B = 6,
        set3A = 0,
        set3B = 0,
        matchSubmitted = true
    ),
    TournamentEntity(
        id = 3,
        name = "QC Night Singles Ladder League",
        facilityName = "Katipunan Kitchen Yard • QC",
        dateLabelOrRange = "Oct 18, 2026"
    )
)

private fun TournamentEntity(
    id: Int,
    name: String,
    facilityName: String,
    dateLabelOrRange: String
): TournamentEntity = TournamentEntity(
    id = id,
    name = name,
    facilityName = facilityName,
    dateRange = dateLabelOrRange,
    division = "Singles",
    format = "League",
    skillCap = "Open Division",
    entryFee = 500,
    prizePool = 20000,
    registeredCount = 9,
    maxTeams = 12,
    isJoinedByUser = false,
    activeRound = "Round 1",
    activeCourt = "Court 3",
    teamA = "Paolo D.",
    teamB = "Rico T.",
    set1A = 11,
    set1B = 4,
    set2A = 11,
    set2B = 7,
    set3A = 0,
    set3B = 0,
    matchSubmitted = true
)

@Composable
fun GamesAndTournamentsScreen(
    openPlayGames: List<OpenPlayGameEntity> = DefaultOpenPlayGamesList,
    tournaments: List<TournamentEntity> = DefaultTournamentsList,
    onToggleJoinOpenPlay: (OpenPlayGameEntity) -> Unit = {},
    onHostOpenPlay: (String, String, String, String, String, String, Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onPostOpenSlotsForGame: (String, String, String, String, String, String, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onRegisterTournament: (TournamentEntity, String) -> Unit = { _, _ -> },
    onSubmitScore: (TournamentEntity, Int, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Open Play, 1 = Tournaments
    var selectedSkillFilter by remember { mutableStateOf("All") }
    var onlyWithVacancy by remember { mutableStateOf(false) }
    var selectedTourneyFilter by remember { mutableStateOf("All") }
    var showHostDialog by remember { mutableStateOf(false) }

    var localGames by remember {
        mutableStateOf(if (openPlayGames.isNotEmpty()) openPlayGames else DefaultOpenPlayGamesList)
    }
    LaunchedEffect(openPlayGames) {
        if (openPlayGames.isNotEmpty()) {
            localGames = openPlayGames
        }
    }

    var localTournaments by remember {
        mutableStateOf(if (tournaments.isNotEmpty()) tournaments else DefaultTournamentsList)
    }
    LaunchedEffect(tournaments) {
        if (tournaments.isNotEmpty()) {
            localTournaments = tournaments
        }
    }

    val filteredGames = remember(localGames, selectedSkillFilter, onlyWithVacancy) {
        filterOpenPlayGamesBySkill(
            games = localGames,
            skillFilter = selectedSkillFilter,
            onlyOpenSlots = onlyWithVacancy
        )
    }

    val filteredTournaments = localTournaments.filter {
        selectedTourneyFilter == "All" ||
            it.division.equals(selectedTourneyFilter, ignoreCase = true) ||
            it.format.equals(selectedTourneyFilter, ignoreCase = true)
    }

    val totalVacantSpots = remember(localGames) {
        localGames.sumOf { resolveOpenPlaySessionMetrics(it).vacantCount }
    }
    val joinedSessionsCount = remember(localGames) {
        localGames.count { it.isJoinedByUser }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("games_tournaments_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Segmented Mode Switcher: Open Play vs Tournaments
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "Open Play & Tournaments",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Join skill-matched open play sessions or track DUPR tournament brackets & schedules",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        SegmentButton(
                            text = "🏓 Open Play (${localGames.size})",
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("subtab_open_play")
                        )
                        SegmentButton(
                            text = "🏆 Tournaments (${localTournaments.size})",
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("subtab_tournaments")
                        )
                    }
                }
            }
        }

        // FIREBASE CLOUD MESSAGING (FCM) PUSH NOTIFICATIONS HUB (Reservations, Tournament Starts, Matchmaking Updates)
        item {
            com.example.notifications.FcmPushNotificationCenterCard(
                tournaments = localTournaments,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (selectedTab == 0) {
            // OPEN PLAY MATCHMAKING SUMMARY BANNER
            item {
                Surface(
                    color = EmeraldDark,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("open_play_matchmaking_summary")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = OpticVolt,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SKILL-BASED OPEN PLAY MATCHMAKING",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = OpticVoltDarkText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${localGames.size} Sessions • $totalVacantSpots Spots Vacant • $joinedSessionsCount Joined",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.testTag("open_play_total_vacancy_summary")
                            )
                            Text(
                                text = "Your Profile: Intermediate (DUPR 3.5) • Matched by skill level & live court vacancy",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6F5E6)
                            )
                        }
                    }
                }
            }

            // REAL-TIME FIRESTORE OPEN PLAY SKILL-LEVEL MATCHMAKING SCREEN
            item {
                OpenPlayMatchmakingScreen(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // MATCHMAKER FEATURE HUB (Post Open Slots & Local Skill-Level Filtering)
            item {
                MatchmakerFeatureCard(
                    games = localGames,
                    selectedSkillFilter = selectedSkillFilter,
                    onlyOpenSlots = onlyWithVacancy,
                    showEmbeddedSessions = false,
                    onSelectSkillFilter = { selectedSkillFilter = it },
                    onToggleOnlyOpenSlots = { onlyWithVacancy = !onlyWithVacancy },
                    onToggleJoinGame = { targetGame ->
                        val currentPlayers = targetGame.playersCsv
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toMutableList()
                        val nextJoined = !targetGame.isJoinedByUser
                        if (nextJoined) {
                            if (currentPlayers.size < targetGame.maxPlayers && currentPlayers.none { it.startsWith("Jonel", ignoreCase = true) }) {
                                currentPlayers.add("Jonel")
                            }
                        } else {
                            currentPlayers.removeAll { it.startsWith("Jonel", ignoreCase = true) }
                        }
                        localGames = localGames.map {
                            if (it.id == targetGame.id) {
                                it.copy(
                                    playersCsv = currentPlayers.joinToString(","),
                                    isJoinedByUser = nextJoined
                                )
                            } else it
                        }
                        onToggleJoinOpenPlay(targetGame)
                    },
                    onPostOpenSlots = { title, facility, court, day, time, skill, fee, openSlotsNeeded, maxPlayers ->
                        val nextId = (localGames.maxOfOrNull { it.id } ?: 4) + 1
                        val newGame = buildMatchmakerOpenSlotGame(
                            newId = nextId,
                            title = title,
                            facilityName = facility,
                            courtName = court,
                            dayLabel = day,
                            timeRange = time,
                            skillLevel = skill,
                            pricePerPlayer = fee,
                            openSlotsNeeded = openSlotsNeeded,
                            maxPlayers = maxPlayers
                        )
                        localGames = listOf(newGame) + localGames
                        onHostOpenPlay(title, facility, court, day, time, skill, fee)
                        onPostOpenSlotsForGame(title, facility, court, day, time, skill, fee, openSlotsNeeded, maxPlayers)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // OPEN PLAY FILTER & CREATE BAR
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val skills = listOf("All", "Beginner", "Intermediate", "Advanced", "All Levels")
                            items(skills) { skill ->
                                FilterChip(
                                    selected = selectedSkillFilter == skill,
                                    onClick = { selectedSkillFilter = skill },
                                    label = { Text(skill) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("open_play_filter_$skill")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = onlyWithVacancy,
                                    onClick = { onlyWithVacancy = !onlyWithVacancy },
                                    label = { Text("Has Vacancy ($totalVacantSpots)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AvailableGreen,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("open_play_vacancy_filter_chip")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showHostDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldDark,
                                contentColor = OpticVolt
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("host_open_play_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Host Game", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(filteredGames, key = { it.id }) { game ->
                OpenPlaySessionCard(
                    game = game,
                    onToggleJoin = {
                        val currentPlayers = game.playersCsv
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toMutableList()
                        val nextJoined = !game.isJoinedByUser
                        if (nextJoined) {
                            if (currentPlayers.size < game.maxPlayers && currentPlayers.none { it.startsWith("Jonel", ignoreCase = true) }) {
                                currentPlayers.add("Jonel")
                            }
                        } else {
                            currentPlayers.removeAll { it.startsWith("Jonel", ignoreCase = true) }
                        }
                        localGames = localGames.map {
                            if (it.id == game.id) {
                                it.copy(
                                    playersCsv = currentPlayers.joinToString(","),
                                    isJoinedByUser = nextJoined
                                )
                            } else it
                        }
                        onToggleJoinOpenPlay(game)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            // TOURNAMENTS TAB
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tournament_banner_1790412115656),
                        contentDescription = "PicklePlay Open 2026 Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF041E14).copy(alpha = 0.9f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "OFFICIAL DUPR SANCTIONED • BRACKETS & SCHEDULES",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpticVoltDarkText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "PicklePlay Open 2026 • Season Series",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "View live brackets, court match schedules & tournament progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )
                    }
                }
            }

            // UPCOMING TOURNAMENTS LIST & REGISTRATION ('tournaments' FIRESTORE COLLECTION)
            item {
                UpcomingTournamentsFirestoreList(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    tournaments = localTournaments,
                    onRegisterTournament = { updatedTournament, partner ->
                        localTournaments = localTournaments.map {
                            if (it.id == updatedTournament.id) updatedTournament else it
                        }
                        TournamentBracketStore.syncWithTournamentEntity(updatedTournament)
                        onRegisterTournament(updatedTournament, partner)
                    }
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("All", "Doubles", "Mixed Doubles", "Singles", "Knockout", "Round Robin", "League")
                    items(formats) { fmt ->
                        FilterChip(
                            selected = selectedTourneyFilter == fmt,
                            onClick = { selectedTourneyFilter = fmt },
                            label = { Text(fmt) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("tournament_filter_$fmt")
                        )
                    }
                }
            }

            items(filteredTournaments, key = { it.id }) { tournament ->
                TournamentBracketCard(
                    tournament = tournament,
                    onRegister = { partner ->
                        val newTeam = if (tournament.division == "Singles") "Jonel" else "Jonel / ${partner.ifBlank { "Carlo" }}"
                        val updated = tournament.copy(
                            isJoinedByUser = true,
                            registeredCount = (tournament.registeredCount + 1).coerceAtMost(tournament.maxTeams),
                            teamA = newTeam
                        )
                        localTournaments = localTournaments.map { if (it.id == tournament.id) updated else it }
                        TournamentBracketStore.syncWithTournamentEntity(updated)
                        onRegisterTournament(tournament, partner)
                    },
                    onSubmitScore = { s1A, s1B, s2A, s2B, s3A, s3B ->
                        val updated = tournament.copy(
                            set1A = s1A,
                            set1B = s1B,
                            set2A = s2A,
                            set2B = s2B,
                            set3A = s3A,
                            set3B = s3B,
                            matchSubmitted = true
                        )
                        localTournaments = localTournaments.map { if (it.id == tournament.id) updated else it }
                        TournamentBracketStore.syncWithTournamentEntity(updated)
                        onSubmitScore(tournament, s1A, s1B, s2A, s2B, s3A, s3B)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showHostDialog) {
        HostOpenPlayDialog(
            onDismiss = { showHostDialog = false },
            onConfirm = { title, club, court, day, time, skill, fee, maxPlayers ->
                val newId = (localGames.maxOfOrNull { it.id } ?: 4) + 1
                val created = OpenPlayGameEntity(
                    id = newId,
                    title = title,
                    facilityId = 1,
                    facilityName = club,
                    courtName = court,
                    dayLabel = day,
                    dateLabel = "Upcoming $day",
                    timeRange = time,
                    skillLevel = skill,
                    pricePerPlayer = fee,
                    maxPlayers = maxPlayers,
                    playersCsv = "Jonel|$skill|3.5",
                    isJoinedByUser = true,
                    hostName = "Jonel P."
                )
                localGames = localGames + created
                onHostOpenPlay(title, club, court, day, time, skill, fee)
                showHostDialog = false
            }
        )
    }
}

@Composable
private fun SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) EmeraldPrimary else Color.Transparent
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
fun OpenPlaySessionCard(
    game: OpenPlayGameEntity,
    onToggleJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = remember(game.playersCsv, game.maxPlayers, game.skillLevel, game.isJoinedByUser) {
        resolveOpenPlaySessionMetrics(game)
    }
    val openSpots = metrics.vacantCount
    val vacancyColor = when {
        openSpots == 0 -> MaintenanceRed
        openSpots == 1 -> PeakAmber
        else -> AvailableGreen
    }
    val vacancyBg = when {
        openSpots == 0 -> MaintenanceRedBg
        openSpots == 1 -> PeakAmber.copy(alpha = 0.16f)
        else -> AvailableGreenBg
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("open_play_card_${game.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🏓 ${game.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${game.facilityName} • ${game.courtName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Session Skill Level & DUPR Rating Band Badge
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = metrics.sessionSkillBand,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = EmeraldPrimary,
                            modifier = Modifier
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("open_play_skill_badge_${game.id}")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Session Vacancy Pill Badge
                    Surface(
                        color = vacancyBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, vacancyColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = metrics.vacancyBadgeText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = vacancyColor,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .testTag("open_play_vacancy_badge_${game.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${game.dayLabel} • ${game.timeRange}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = EmeraldPrimary
                )
                Text(
                    text = "Host: ${game.hostName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Session Vacancy Header + Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Players (${metrics.filledCount}/${metrics.maxPlayers} Filled • $openSpots Open)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("open_play_vacancy_text_${game.id}")
                )
                Text(
                    text = if (openSpots > 0) "Vacancy: $openSpots of ${metrics.maxPlayers} slots available" else "Session Full",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = vacancyColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(metrics.vacancyRatio.coerceIn(0.08f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(EmeraldPrimary)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Player Roster Slot Matrix with Player Skill Levels & DUPR Badges
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metrics.slots.chunked(3).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val isFilled = slot.isFilled
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!isFilled && !game.isJoinedByUser && openSpots > 0) {
                                            Modifier.clickable { onToggleJoin() }
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .testTag("open_play_slot_${game.id}_${slot.slotIndex}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isFilled) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFilled) EmeraldPrimary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (isFilled) EmeraldPrimary else Color.Gray.copy(alpha = 0.4f))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = slot.playerName ?: "OPEN SLOT",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isFilled) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isFilled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isFilled) {
                                            "${slot.skillLevel.take(3)}. • DUPR ${slot.duprRating}"
                                        } else {
                                            "Vacant • ${game.skillLevel.take(3)}."
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isFilled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("open_play_player_skill_${game.id}_${slot.slotIndex}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₱${game.pricePerPlayer} / player",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "Includes court & balls • Skill: ${game.skillLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onToggleJoin,
                    enabled = game.isJoinedByUser || openSpots > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (game.isJoinedByUser) AvailableGreen else EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("join_game_button_${game.id}")
                ) {
                    Text(
                        text = when {
                            game.isJoinedByUser -> "JOINED ✓ (LEAVE)"
                            openSpots == 0 -> "SESSION FULL"
                            else -> "JOIN GAME"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentBracketCard(
    tournament: TournamentEntity,
    onRegister: (String) -> Unit,
    onSubmitScore: (Int, Int, Int, Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var s1A by remember(tournament.id, tournament.set1A) { mutableStateOf(tournament.set1A.toString()) }
    var s1B by remember(tournament.id, tournament.set1B) { mutableStateOf(tournament.set1B.toString()) }
    var s2A by remember(tournament.id, tournament.set2A) { mutableStateOf(tournament.set2A.toString()) }
    var s2B by remember(tournament.id, tournament.set2B) { mutableStateOf(tournament.set2B.toString()) }
    var s3A by remember(tournament.id, tournament.set3A) { mutableStateOf(tournament.set3A.toString()) }
    var s3B by remember(tournament.id, tournament.set3B) { mutableStateOf(tournament.set3B.toString()) }
    var partnerInput by remember { mutableStateOf("Carlo") }

    val bracketMap by TournamentBracketStore.brackets.collectAsState()
    val bracketState = bracketMap[tournament.id]
        ?: remember(tournament) { TournamentBracketStore.getBracketState(tournament) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EmeraldDark,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${tournament.division.uppercase()} • ${tournament.format.uppercase()}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = OpticVolt,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Prize Pool: ₱${tournament.prizePool}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PeakAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tournament.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${tournament.facilityName} • ${tournament.dateRange}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Tournament Progress & Stage Stepper Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tournament_progress_section_${tournament.id}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tournament Progress • ${bracketState.activeRound}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${bracketState.progressPercent}% Complete",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("tournament_progress_badge_${tournament.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((bracketState.progressPercent / 100f).coerceIn(0.15f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(EmeraldPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stage Pills: Quarter Finals -> Semi Finals -> Finals
                    val stages = listOf("Quarter Finals", "Semi Finals", "Finals")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stages.forEach { stage ->
                            val isCurrent = bracketState.activeRound.contains(stage.substringBefore(" "), ignoreCase = true)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = stage,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Interactive Visual Knockout Bracket Tree Component
            PickleballTournamentBracketVisualizer(
                tournament = tournament,
                bracketState = bracketState
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Match Schedules & Bracket Tree List
            Text(
                text = "Match Schedules & Bracket Tree",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("tournament_schedule_header_${tournament.id}")
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("tournament_schedule_list_${tournament.id}")
            ) {
                bracketState.matches.forEach { match ->
                    val statusColor = when (match.status) {
                        "COMPLETED" -> AvailableGreen
                        "LIVE" -> PeakAmber
                        else -> GCashBlue
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tournament_match_row_${tournament.id}_${match.matchId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${match.matchId} • ${match.roundName}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${match.courtName} • ${match.scheduledTime}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${match.teamA} vs ${match.teamB}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = match.scoreSummary + (match.winnerTeam?.let { " • Winner: $it ✓" } ?: ""),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (match.winnerTeam != null) AvailableGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = statusColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = match.status,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Official Match Scoreboard Card matching Section 8 of prompt
            Surface(
                color = EmeraldDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bracketState.activeRound.uppercase(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OpticVolt
                        )
                        Text(
                            text = tournament.activeCourt,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Team A vs Team B
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tournament.teamA,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            color = OpticVolt,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "VS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = OpticVoltDarkText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = tournament.teamB,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "OFFICIAL 3-SET SCORE (11-POINT GAMES)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFFA3B8B0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Interactive 3-Set Score Pills (11 - 8, 8 - 11, 11 - 7)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SetScorePill(
                            setLabel = "Set 1",
                            scoreA = s1A,
                            scoreB = s1B,
                            onStepA = { s1A = ((s1A.toIntOrNull() ?: 11) + 1).coerceAtMost(21).toString() },
                            onStepB = { s1B = ((s1B.toIntOrNull() ?: 8) + 1).coerceAtMost(21).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        SetScorePill(
                            setLabel = "Set 2",
                            scoreA = s2A,
                            scoreB = s2B,
                            onStepA = { s2A = ((s2A.toIntOrNull() ?: 8) + 1).coerceAtMost(21).toString() },
                            onStepB = { s2B = ((s2B.toIntOrNull() ?: 11) + 1).coerceAtMost(21).toString() },
                            modifier = Modifier.weight(1f)
                        )
                        SetScorePill(
                            setLabel = "Set 3",
                            scoreA = s3A,
                            scoreB = s3B,
                            onStepA = { s3A = ((s3A.toIntOrNull() ?: 11) + 1).coerceAtMost(21).toString() },
                            onStepB = { s3B = ((s3B.toIntOrNull() ?: 7) + 1).coerceAtMost(21).toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onSubmitScore(
                                s1A.toIntOrNull() ?: 11,
                                s1B.toIntOrNull() ?: 8,
                                s2A.toIntOrNull() ?: 8,
                                s2B.toIntOrNull() ?: 11,
                                s3A.toIntOrNull() ?: 11,
                                s3B.toIntOrNull() ?: 7
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OpticVolt,
                            contentColor = OpticVoltDarkText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_score_button_${tournament.id}")
                    ) {
                        Text(
                            text = if (tournament.matchSubmitted) "Update Official Score ✓" else "Submit Score",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Registration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Entry Fee: ₱${tournament.entryFee} • ${tournament.registeredCount}/${tournament.maxTeams} Teams",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = tournament.skillCap,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!tournament.isJoinedByUser) {
                    OutlinedButton(
                        onClick = { onRegister(partnerInput) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        modifier = Modifier.testTag("join_tournament_button_${tournament.id}")
                    ) {
                        Text("Join Tournament", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                } else {
                    Surface(
                        color = AvailableGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "REGISTERED ✓",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AvailableGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetScorePill(
    setLabel: String,
    scoreA: String,
    scoreB: String,
    onStepA: () -> Unit,
    onStepB: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF123A2A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = setLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = Color(0xFFA3B8B0)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = scoreA,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = OpticVolt,
                    modifier = Modifier
                        .clickable { onStepA() }
                        .padding(horizontal = 4.dp)
                )
                Text(
                    text = " - ",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = scoreB,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White,
                    modifier = Modifier
                        .clickable { onStepB() }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostOpenPlayDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("Friday Night Dink & Social") }
    var facility by remember { mutableStateOf("Smash Pickle Club") }
    var court by remember { mutableStateOf("Court 2") }
    var day by remember { mutableStateOf("Friday") }
    var time by remember { mutableStateOf("6:00 PM - 8:00 PM") }
    var skill by remember { mutableStateOf("Intermediate") }
    var fee by remember { mutableStateOf("150") }
    var maxPlayers by remember { mutableIntStateOf(6) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("host_open_play_dialog"),
        title = { Text("Host Open Play Session") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_open_play_title_input")
                )
                OutlinedTextField(
                    value = facility,
                    onValueChange = { facility = it },
                    label = { Text("Club / Facility") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("host_open_play_facility_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = court,
                        onValueChange = { court = it },
                        label = { Text("Court") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("host_open_play_court_input")
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Day") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("host_open_play_day_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time Slot") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("host_open_play_time_input")
                    )
                    OutlinedTextField(
                        value = fee,
                        onValueChange = { fee = it },
                        label = { Text("Fee (₱)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("host_open_play_fee_input")
                    )
                }

                Text(
                    text = "Player Skill Level Requirement:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val skillOptions = listOf("Beginner", "Intermediate", "Advanced", "All Levels")
                    skillOptions.forEach { option ->
                        FilterChip(
                            selected = skill.equals(option, ignoreCase = true),
                            onClick = { skill = option },
                            label = { Text(option, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("host_skill_chip_$option")
                        )
                    }
                }

                Text(
                    text = "Session Capacity (Max Players):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 6, 8).forEach { cap ->
                        FilterChip(
                            selected = maxPlayers == cap,
                            onClick = { maxPlayers = cap },
                            label = { Text("$cap Players (${cap - 1} Open)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = OpticVolt
                            ),
                            modifier = Modifier.testTag("host_max_players_chip_$cap")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.ifBlank { "Weekend Open Play" },
                        facility.ifBlank { "Smash Pickle Club" },
                        court.ifBlank { "Court 2" },
                        day.ifBlank { "Saturday" },
                        time.ifBlank { "6:00 PM - 8:00 PM" },
                        skill,
                        fee.toIntOrNull() ?: 150,
                        maxPlayers
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_host_open_play_button")
            ) {
                Text("Create Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
