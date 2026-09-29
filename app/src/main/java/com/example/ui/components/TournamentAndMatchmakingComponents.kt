package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OpenPlayGameEntity
import com.example.data.local.TournamentEntity
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpenPlayPlayerSlotInfo(
    val slotIndex: Int,
    val playerName: String?,
    val skillLevel: String,
    val duprRating: String,
    val isHost: Boolean = false,
    val isCurrentUser: Boolean = false
) {
    val isFilled: Boolean
        get() = !playerName.isNullOrBlank()
}

data class OpenPlaySessionMetrics(
    val filledCount: Int,
    val maxPlayers: Int,
    val vacantCount: Int,
    val vacancyRatio: Float,
    val vacancyBadgeText: String,
    val vacancySummaryText: String,
    val sessionSkillBand: String,
    val slots: List<OpenPlayPlayerSlotInfo>
)

private val KnownPlayerSkillDirectory = mapOf(
    "jonel" to ("Intermediate" to "3.5"),
    "jonel p." to ("Intermediate" to "3.5"),
    "jon" to ("Intermediate" to "3.4"),
    "maria" to ("Intermediate" to "3.6"),
    "carlo" to ("Intermediate" to "3.7"),
    "sofia" to ("Beginner" to "2.8"),
    "miggy" to ("Intermediate" to "3.3"),
    "tricia" to ("Beginner" to "2.9"),
    "mark" to ("Advanced" to "4.2"),
    "james" to ("Advanced" to "4.1"),
    "kevin" to ("Advanced" to "4.0"),
    "rico" to ("Advanced" to "4.3"),
    "luis" to ("Advanced" to "4.1"),
    "aya" to ("Beginner" to "2.5"),
    "dan" to ("Beginner" to "2.7"),
    "coach anton" to ("Advanced" to "4.4"),
    "coach bea" to ("Intermediate" to "3.8"),
    "coach marco" to ("Advanced" to "4.3")
)

fun resolveSkillBandLabel(skillLevel: String): String {
    return when {
        skillLevel.contains("Beginner", ignoreCase = true) -> "Beginner • DUPR 2.0–2.9"
        skillLevel.contains("Intermediate", ignoreCase = true) -> "Intermediate • DUPR 3.0–3.9"
        skillLevel.contains("Advanced", ignoreCase = true) -> "Advanced • DUPR 4.0+"
        else -> "All Levels • DUPR 2.0–4.5"
    }
}

fun resolveOpenPlaySessionMetrics(
    game: OpenPlayGameEntity,
    currentUserName: String = "Jonel"
): OpenPlaySessionMetrics {
    val rawEntries = game.playersCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val defaultSkillForSession = when {
        game.skillLevel.contains("Beginner", ignoreCase = true) -> "Beginner"
        game.skillLevel.contains("Advanced", ignoreCase = true) -> "Advanced"
        else -> "Intermediate"
    }
    val defaultDuprForSession = when (defaultSkillForSession) {
        "Beginner" -> "2.6"
        "Advanced" -> "4.1"
        else -> "3.5"
    }

    val totalSlots = game.maxPlayers.coerceAtLeast(2)
    val slots = (0 until totalSlots).map { idx ->
        val raw = rawEntries.getOrNull(idx)
        if (raw == null) {
            OpenPlayPlayerSlotInfo(
                slotIndex = idx,
                playerName = null,
                skillLevel = game.skillLevel,
                duprRating = defaultDuprForSession
            )
        } else {
            // Support optional inline encoding like "Alex|Advanced|4.1" or "Alex (Intermediate)"
            val parts = raw.split("|").map { it.trim() }
            val cleanName = parts.firstOrNull()?.substringBefore("(")?.trim().orEmpty().ifBlank { raw }
            val lookup = KnownPlayerSkillDirectory[cleanName.lowercase()]
            val resolvedSkill = when {
                parts.size >= 2 && parts[1].isNotBlank() -> parts[1]
                raw.contains("(") && raw.contains(")") -> raw.substringAfter("(").substringBefore(")").trim()
                lookup != null -> lookup.first
                else -> defaultSkillForSession
            }
            val resolvedDupr = when {
                parts.size >= 3 && parts[2].isNotBlank() -> parts[2]
                lookup != null -> lookup.second
                else -> defaultDuprForSession
            }
            val isHost = game.hostName.startsWith(cleanName, ignoreCase = true) ||
                cleanName.startsWith(game.hostName.substringBefore(" "), ignoreCase = true)
            val isCurrent = cleanName.equals(currentUserName, ignoreCase = true) ||
                (game.isJoinedByUser && idx == rawEntries.lastIndex && cleanName.startsWith("Jonel", ignoreCase = true))

            OpenPlayPlayerSlotInfo(
                slotIndex = idx,
                playerName = cleanName,
                skillLevel = resolvedSkill,
                duprRating = resolvedDupr,
                isHost = isHost,
                isCurrentUser = isCurrent
            )
        }
    }

    val filledCount = rawEntries.size.coerceAtMost(totalSlots)
    val vacantCount = (totalSlots - filledCount).coerceAtLeast(0)
    val vacancyRatio = filledCount.toFloat() / totalSlots.toFloat()
    val vacancyBadgeText = when {
        vacantCount == 0 -> "FULL • 0 SPOTS LEFT"
        vacantCount == 1 -> "1 SPOT LEFT • HURRY"
        else -> "$vacantCount SPOTS OPEN"
    }
    val vacancySummaryText = "$filledCount/$totalSlots Players Joined • $vacantCount Vacant"

    return OpenPlaySessionMetrics(
        filledCount = filledCount,
        maxPlayers = totalSlots,
        vacantCount = vacantCount,
        vacancyRatio = vacancyRatio,
        vacancyBadgeText = vacancyBadgeText,
        vacancySummaryText = vacancySummaryText,
        sessionSkillBand = resolveSkillBandLabel(game.skillLevel),
        slots = slots
    )
}

data class TournamentMatchItem(
    val matchId: String,
    val roundName: String,
    val roundStageIndex: Int, // 1 = Quarter Finals, 2 = Semi Finals, 3 = Finals
    val matchLabel: String,
    val courtName: String,
    val scheduledTime: String,
    val teamA: String,
    val teamASkill: String,
    val teamB: String,
    val teamBSkill: String,
    val scoreSummary: String,
    val winnerTeam: String? = null,
    val status: String // "COMPLETED", "LIVE", "UPCOMING"
)

data class TournamentBracketState(
    val tournamentId: Int,
    val tournamentName: String,
    val activeRound: String,
    val progressPercent: Int,
    val championTeam: String? = null,
    val matches: List<TournamentMatchItem>
)

object TournamentBracketStore {
    private val _brackets = MutableStateFlow<Map<Int, TournamentBracketState>>(emptyMap())
    val brackets: StateFlow<Map<Int, TournamentBracketState>> = _brackets.asStateFlow()

    val stageOrder = listOf("Registration", "Quarter Finals", "Semi Finals", "Finals", "Completed")

    fun ensureBracketForTournament(tournament: TournamentEntity): TournamentBracketState {
        val currentMap = _brackets.value
        val existing = currentMap[tournament.id]
        if (existing != null) {
            return existing
        }
        val created = buildDefaultBracket(tournament)
        _brackets.value = currentMap + (tournament.id to created)
        return created
    }

    fun getBracketState(tournament: TournamentEntity): TournamentBracketState {
        return _brackets.value[tournament.id] ?: buildDefaultBracket(tournament).also { state ->
            _brackets.value = _brackets.value + (tournament.id to state)
        }
    }

    private fun buildDefaultBracket(tournament: TournamentEntity): TournamentBracketState {
        val defaultScore = "${tournament.set1A}-${tournament.set1B}, ${tournament.set2A}-${tournament.set2B}" +
            if (tournament.set3A > 0 || tournament.set3B > 0) ", ${tournament.set3A}-${tournament.set3B}" else ""
        val primaryWinner = if (tournament.matchSubmitted) {
            val setsWonA = listOf(
                tournament.set1A > tournament.set1B,
                tournament.set2A > tournament.set2B,
                tournament.set3A > tournament.set3B
            ).count { it }
            if (setsWonA >= 2) tournament.teamA else tournament.teamB
        } else null

        val matches = when (tournament.id) {
            1 -> listOf(
                TournamentMatchItem(
                    matchId = "QF1",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 1",
                    courtName = tournament.activeCourt.ifBlank { "Court 2" },
                    scheduledTime = "Sept 28 • 10:00 AM",
                    teamA = tournament.teamA,
                    teamASkill = "DUPR 3.6",
                    teamB = tournament.teamB,
                    teamBSkill = "DUPR 3.8",
                    scoreSummary = if (tournament.matchSubmitted) defaultScore else "Live: $defaultScore",
                    winnerTeam = primaryWinner,
                    status = if (tournament.matchSubmitted) "COMPLETED" else "LIVE"
                ),
                TournamentMatchItem(
                    matchId = "QF2",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 2",
                    courtName = "Court 3",
                    scheduledTime = "Sept 28 • 11:00 AM",
                    teamA = "Kevin / Rico",
                    teamASkill = "DUPR 3.9",
                    teamB = "Paolo / Luis",
                    teamBSkill = "DUPR 3.7",
                    scoreSummary = "11-9, 11-7",
                    winnerTeam = "Kevin / Rico",
                    status = "COMPLETED"
                ),
                TournamentMatchItem(
                    matchId = "QF3",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 3",
                    courtName = "Court 4",
                    scheduledTime = "Sept 28 • 12:00 PM",
                    teamA = "Anton / Marco",
                    teamASkill = "DUPR 3.9",
                    teamB = "Dan / Miggy",
                    teamBSkill = "DUPR 3.5",
                    scoreSummary = "11-6, 11-8",
                    winnerTeam = "Anton / Marco",
                    status = "COMPLETED"
                ),
                TournamentMatchItem(
                    matchId = "QF4",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 4",
                    courtName = "Court 1",
                    scheduledTime = "Sept 28 • 1:00 PM",
                    teamA = "Sofia / Bea",
                    teamASkill = "DUPR 3.6",
                    teamB = "Maria / Tricia",
                    teamBSkill = "DUPR 3.5",
                    scoreSummary = "9-11, 11-8, 11-9",
                    winnerTeam = "Sofia / Bea",
                    status = "COMPLETED"
                ),
                TournamentMatchItem(
                    matchId = "SF1",
                    roundName = "Semi Finals",
                    roundStageIndex = 2,
                    matchLabel = "Semi Final 1",
                    courtName = "Court 1",
                    scheduledTime = "Sept 29 • 2:00 PM",
                    teamA = primaryWinner ?: "Winner QF1 (${tournament.teamA})",
                    teamASkill = "DUPR 3.7",
                    teamB = "Kevin / Rico",
                    teamBSkill = "DUPR 3.9",
                    scoreSummary = "Scheduled • Best of 3",
                    winnerTeam = null,
                    status = "UPCOMING"
                ),
                TournamentMatchItem(
                    matchId = "SF2",
                    roundName = "Semi Finals",
                    roundStageIndex = 2,
                    matchLabel = "Semi Final 2",
                    courtName = "Court 2",
                    scheduledTime = "Sept 29 • 3:30 PM",
                    teamA = "Anton / Marco",
                    teamASkill = "DUPR 3.9",
                    teamB = "Sofia / Bea",
                    teamBSkill = "DUPR 3.6",
                    scoreSummary = "Scheduled • Best of 3",
                    winnerTeam = null,
                    status = "UPCOMING"
                ),
                TournamentMatchItem(
                    matchId = "FINAL",
                    roundName = "Finals",
                    roundStageIndex = 3,
                    matchLabel = "Championship Final",
                    courtName = "Court 1 (Center)",
                    scheduledTime = "Sept 30 • 5:00 PM",
                    teamA = "Winner SF1",
                    teamASkill = "DUPR 3.8+",
                    teamB = "Winner SF2",
                    teamBSkill = "DUPR 3.8+",
                    scoreSummary = "Grand Final • ₱${tournament.prizePool} Prize Pool",
                    winnerTeam = null,
                    status = "UPCOMING"
                )
            )
            else -> listOf(
                TournamentMatchItem(
                    matchId = "QF1",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 1",
                    courtName = tournament.activeCourt.ifBlank { "Court 1" },
                    scheduledTime = "${tournament.dateRange.substringBefore("–").trim()} • 9:30 AM",
                    teamA = tournament.teamA,
                    teamASkill = tournament.skillCap,
                    teamB = tournament.teamB,
                    teamBSkill = tournament.skillCap,
                    scoreSummary = defaultScore,
                    winnerTeam = primaryWinner ?: tournament.teamA,
                    status = if (tournament.matchSubmitted) "COMPLETED" else "LIVE"
                ),
                TournamentMatchItem(
                    matchId = "QF2",
                    roundName = "Quarter Finals",
                    roundStageIndex = 1,
                    matchLabel = "Quarter Final 2",
                    courtName = "Court 2",
                    scheduledTime = "${tournament.dateRange.substringBefore("–").trim()} • 11:00 AM",
                    teamA = "Team Alpha",
                    teamASkill = tournament.skillCap,
                    teamB = "Team Smash",
                    teamBSkill = tournament.skillCap,
                    scoreSummary = "11-7, 11-8",
                    winnerTeam = "Team Alpha",
                    status = "COMPLETED"
                ),
                TournamentMatchItem(
                    matchId = "SF1",
                    roundName = "Semi Finals",
                    roundStageIndex = 2,
                    matchLabel = "Semi Final 1",
                    courtName = "Court 1",
                    scheduledTime = "${tournament.dateRange.substringBefore("–").trim()} • 2:00 PM",
                    teamA = primaryWinner ?: tournament.teamA,
                    teamASkill = tournament.skillCap,
                    teamB = "Team Alpha",
                    teamBSkill = tournament.skillCap,
                    scoreSummary = "Scheduled • Best of 3",
                    winnerTeam = null,
                    status = "UPCOMING"
                ),
                TournamentMatchItem(
                    matchId = "FINAL",
                    roundName = "Finals",
                    roundStageIndex = 3,
                    matchLabel = "Championship Final",
                    courtName = "Court 1",
                    scheduledTime = "${tournament.dateRange.substringBefore("–").trim()} • 4:30 PM",
                    teamA = "Winner SF1",
                    teamASkill = tournament.skillCap,
                    teamB = "Top Seed Contender",
                    teamBSkill = tournament.skillCap,
                    scoreSummary = "Championship Match",
                    winnerTeam = null,
                    status = "UPCOMING"
                )
            )
        }

        val completedCount = matches.count { it.status == "COMPLETED" }
        val pct = ((completedCount.toFloat() / matches.size.coerceAtLeast(1)) * 100).toInt().coerceIn(20, 100)

        return TournamentBracketState(
            tournamentId = tournament.id,
            tournamentName = tournament.name,
            activeRound = tournament.activeRound,
            progressPercent = pct,
            championTeam = null,
            matches = matches
        )
    }

    fun syncWithTournamentEntity(tournament: TournamentEntity) {
        val current = getBracketState(tournament)
        val setsWonA = listOf(
            tournament.set1A > tournament.set1B,
            tournament.set2A > tournament.set2B,
            tournament.set3A > tournament.set3B
        ).count { it }
        val winner = if (tournament.matchSubmitted) {
            if (setsWonA >= 2) tournament.teamA else tournament.teamB
        } else null
        val scoreStr = "${tournament.set1A}-${tournament.set1B}, ${tournament.set2A}-${tournament.set2B}" +
            if (tournament.set3A > 0 || tournament.set3B > 0) ", ${tournament.set3A}-${tournament.set3B}" else ""

        val updatedMatches = current.matches.map { match ->
            when (match.matchId) {
                "QF1" -> match.copy(
                    teamA = tournament.teamA,
                    teamB = tournament.teamB,
                    courtName = tournament.activeCourt,
                    scoreSummary = if (tournament.matchSubmitted) scoreStr else "Live: $scoreStr",
                    winnerTeam = winner ?: match.winnerTeam,
                    status = if (tournament.matchSubmitted) "COMPLETED" else "LIVE"
                )
                "SF1" -> if (winner != null) {
                    match.copy(
                        teamA = winner,
                        status = if (match.status == "UPCOMING") "LIVE" else match.status
                    )
                } else match
                else -> match
            }
        }
        val completedCount = updatedMatches.count { it.status == "COMPLETED" }
        val pct = ((completedCount.toFloat() / updatedMatches.size.coerceAtLeast(1)) * 100).toInt().coerceIn(25, 100)
        val nextRound = if (tournament.matchSubmitted && current.activeRound.contains("Quarter", ignoreCase = true)) {
            "Semi Finals"
        } else {
            tournament.activeRound
        }
        _brackets.value = _brackets.value + (
            tournament.id to current.copy(
                activeRound = nextRound,
                progressPercent = pct,
                matches = updatedMatches
            )
            )
    }

    fun updateBracketMatch(
        tournament: TournamentEntity,
        matchId: String,
        teamA: String,
        teamB: String,
        courtName: String,
        scheduledTime: String,
        scoreSummary: String,
        winnerTeam: String?,
        status: String
    ): TournamentBracketState {
        val current = getBracketState(tournament)
        val updatedMatches = current.matches.map { m ->
            if (m.matchId == matchId) {
                m.copy(
                    teamA = teamA.trim().ifBlank { m.teamA },
                    teamB = teamB.trim().ifBlank { m.teamB },
                    courtName = courtName.trim().ifBlank { m.courtName },
                    scheduledTime = scheduledTime.trim().ifBlank { m.scheduledTime },
                    scoreSummary = scoreSummary.trim().ifBlank { m.scoreSummary },
                    winnerTeam = winnerTeam?.takeIf { it.isNotBlank() },
                    status = status
                )
            } else {
                m
            }
        }.let { list ->
            // Propagate winners to subsequent bracket rounds automatically
            val qf1Winner = list.find { it.matchId == "QF1" }?.winnerTeam
            val qf2Winner = list.find { it.matchId == "QF2" }?.winnerTeam
            val qf3Winner = list.find { it.matchId == "QF3" }?.winnerTeam
            val qf4Winner = list.find { it.matchId == "QF4" }?.winnerTeam
            val sf1Winner = list.find { it.matchId == "SF1" }?.winnerTeam
            val sf2Winner = list.find { it.matchId == "SF2" }?.winnerTeam

            list.map { item ->
                when (item.matchId) {
                    "SF1" -> item.copy(
                        teamA = qf1Winner ?: item.teamA,
                        teamB = qf2Winner ?: item.teamB
                    )
                    "SF2" -> item.copy(
                        teamA = qf3Winner ?: item.teamA,
                        teamB = qf4Winner ?: item.teamB
                    )
                    "FINAL" -> item.copy(
                        teamA = sf1Winner ?: item.teamA,
                        teamB = sf2Winner ?: item.teamB
                    )
                    else -> item
                }
            }
        }

        val finalWinner = updatedMatches.find { it.matchId == "FINAL" }?.winnerTeam
        val completedCount = updatedMatches.count { it.status == "COMPLETED" }
        val pct = if (finalWinner != null) {
            100
        } else {
            ((completedCount.toFloat() / updatedMatches.size.coerceAtLeast(1)) * 100).toInt().coerceIn(20, 95)
        }
        val resolvedRound = when {
            finalWinner != null -> "Completed • Champion: $finalWinner"
            updatedMatches.any { it.roundStageIndex == 3 && it.status == "LIVE" } -> "Finals"
            updatedMatches.all { it.roundStageIndex == 1 && it.status == "COMPLETED" } -> "Semi Finals"
            else -> current.activeRound
        }

        val newState = current.copy(
            activeRound = resolvedRound,
            progressPercent = pct,
            championTeam = finalWinner,
            matches = updatedMatches
        )
        _brackets.value = _brackets.value + (tournament.id to newState)
        return newState
    }

    fun shuffleBracketPairings(tournament: TournamentEntity): TournamentBracketState {
        val current = getBracketState(tournament)
        val seedTeams = listOf(
            tournament.teamA to "DUPR 3.6",
            tournament.teamB to "DUPR 3.8",
            "Kevin / Rico" to "DUPR 3.9",
            "Anton / Marco" to "DUPR 3.9",
            "Sofia / Bea" to "DUPR 3.6",
            "Paolo / Luis" to "DUPR 3.7",
            "Dan / Miggy" to "DUPR 3.5",
            "Maria / Tricia" to "DUPR 3.5"
        )
        // Rotate pairings deterministically so shuffling always produces a fresh, valid bracket
        val rotated = seedTeams.drop(1) + seedTeams.take(1)
        val updatedMatches = current.matches.mapIndexed { idx, match ->
            if (match.roundStageIndex == 1) {
                val pairA = rotated.getOrElse((idx * 2) % rotated.size) { tournament.teamA to "DUPR 3.6" }
                val pairB = rotated.getOrElse((idx * 2 + 1) % rotated.size) { tournament.teamB to "DUPR 3.8" }
                match.copy(
                    teamA = pairA.first,
                    teamASkill = pairA.second,
                    teamB = pairB.first,
                    teamBSkill = pairB.second,
                    courtName = "Court ${(idx % 4) + 1}"
                )
            } else {
                match
            }
        }
        val newState = current.copy(matches = updatedMatches)
        _brackets.value = _brackets.value + (tournament.id to newState)
        return newState
    }

    fun advanceRound(tournament: TournamentEntity): TournamentBracketState {
        val current = getBracketState(tournament)
        val nextRound: String
        val nextPct: Int
        val updatedMatches: List<TournamentMatchItem>

        when {
            current.activeRound.contains("Quarter", ignoreCase = true) ||
                current.activeRound.contains("Group", ignoreCase = true) ||
                current.activeRound.contains("Round 1", ignoreCase = true) -> {
                nextRound = "Semi Finals"
                nextPct = 72
                updatedMatches = current.matches.map { m ->
                    when (m.roundStageIndex) {
                        1 -> m.copy(
                            status = "COMPLETED",
                            winnerTeam = m.winnerTeam ?: m.teamA,
                            scoreSummary = if (m.scoreSummary.contains("Scheduled") || m.scoreSummary.contains("Live")) "11-8, 11-7" else m.scoreSummary
                        )
                        2 -> m.copy(
                            status = "LIVE",
                            teamA = if (m.matchId == "SF1") (current.matches.find { it.matchId == "QF1" }?.winnerTeam ?: tournament.teamA) else m.teamA,
                            scoreSummary = "Live on ${m.courtName} • 1st Set"
                        )
                        else -> m
                    }
                }
            }
            current.activeRound.contains("Semi", ignoreCase = true) -> {
                nextRound = "Finals"
                nextPct = 90
                val sf1Winner = current.matches.find { it.matchId == "SF1" }?.let { it.winnerTeam ?: it.teamA } ?: tournament.teamA
                val sf2Winner = current.matches.find { it.matchId == "SF2" }?.let { it.winnerTeam ?: it.teamA } ?: "Anton / Marco"
                updatedMatches = current.matches.map { m ->
                    when (m.roundStageIndex) {
                        1, 2 -> m.copy(
                            status = "COMPLETED",
                            winnerTeam = m.winnerTeam ?: m.teamA,
                            scoreSummary = if (m.scoreSummary.contains("Scheduled") || m.scoreSummary.contains("Live")) "11-9, 11-6" else m.scoreSummary
                        )
                        3 -> m.copy(
                            status = "LIVE",
                            teamA = sf1Winner,
                            teamB = sf2Winner,
                            scoreSummary = "Championship Live on ${m.courtName}"
                        )
                        else -> m
                    }
                }
            }
            else -> {
                val champ = current.matches.find { it.matchId == "FINAL" }?.let { it.winnerTeam ?: it.teamA } ?: tournament.teamA
                nextRound = "Completed"
                nextPct = 100
                updatedMatches = current.matches.map { m ->
                    if (m.roundStageIndex == 3) {
                        m.copy(
                            status = "COMPLETED",
                            winnerTeam = champ,
                            scoreSummary = "11-8, 9-11, 11-7 (Final)"
                        )
                    } else {
                        m.copy(status = "COMPLETED", winnerTeam = m.winnerTeam ?: m.teamA)
                    }
                }
                val completedState = current.copy(
                    activeRound = nextRound,
                    progressPercent = nextPct,
                    championTeam = champ,
                    matches = updatedMatches
                )
                _brackets.value = _brackets.value + (tournament.id to completedState)
                return completedState
            }
        }

        val newState = current.copy(
            activeRound = nextRound,
            progressPercent = nextPct,
            matches = updatedMatches
        )
        _brackets.value = _brackets.value + (tournament.id to newState)
        return newState
    }
}

private val DefaultAdminTournaments = listOf(
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
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTournamentManagementSection(
    tournaments: List<TournamentEntity> = emptyList(),
    onCreateTournament: (
        name: String,
        division: String,
        format: String,
        skillCap: String,
        dateRange: String,
        entryFee: Int,
        prizePool: Int,
        maxTeams: Int,
        activeCourt: String,
        teamA: String,
        teamB: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onUpdateTournamentBracket: (TournamentEntity, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var localTournaments by remember(tournaments) {
        mutableStateOf(if (tournaments.isNotEmpty()) tournaments else DefaultAdminTournaments)
    }
    var selectedTournamentId by remember {
        mutableIntStateOf(localTournaments.firstOrNull()?.id ?: 1)
    }
    val selectedTournament = localTournaments.find { it.id == selectedTournamentId }
        ?: localTournaments.firstOrNull()
        ?: DefaultAdminTournaments.first()

    val bracketMap by TournamentBracketStore.brackets.collectAsState()
    val bracketState = bracketMap[selectedTournament.id]
        ?: remember(selectedTournament) { TournamentBracketStore.getBracketState(selectedTournament) }

    var showCreateTournamentDialog by remember { mutableStateOf(false) }
    var editingMatch by remember { mutableStateOf<TournamentMatchItem?>(null) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_tournament_management_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                        shape = CircleShape,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tournament Bracket & Schedule Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Organize brackets, seed matchups, assign courts & advance rounds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showCreateTournamentDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldDark,
                        contentColor = OpticVolt
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("admin_create_tournament_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tournament Selector Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(localTournaments, key = { it.id }) { tourney ->
                    val isSelected = tourney.id == selectedTournament.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTournamentId = tourney.id },
                        label = {
                            Text(
                                text = "${tourney.name} (${tourney.division})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("admin_select_tournament_${tourney.id}")
                    )
                }
            }

            if (statusBanner != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AvailableGreenBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_tournament_status_banner")
                ) {
                    Text(
                        text = statusBanner!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AvailableGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Tournament Progress & Stage Controls
            Surface(
                color = EmeraldDark,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedTournament.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${selectedTournament.division} • ${selectedTournament.format} • ${selectedTournament.skillCap} • ${selectedTournament.registeredCount}/${selectedTournament.maxTeams} Teams",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6F5E6)
                            )
                        }
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${bracketState.progressPercent}% COMPLETE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = OpticVoltDarkText,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("admin_tournament_progress_${selectedTournament.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((bracketState.progressPercent / 100f).coerceIn(0.1f, 1f))
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(OpticVolt)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Stage: ${bracketState.activeRound}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = OpticVolt,
                            modifier = Modifier.testTag("admin_tournament_active_stage_${selectedTournament.id}")
                        )
                        if (bracketState.championTeam != null) {
                            Text(
                                text = "🏆 Champion: ${bracketState.championTeam}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = PeakAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Admin Bracket Organizer Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val updated = TournamentBracketStore.shuffleBracketPairings(selectedTournament)
                                val qf1 = updated.matches.firstOrNull()
                                if (qf1 != null) {
                                    onUpdateTournamentBracket(
                                        selectedTournament,
                                        updated.activeRound,
                                        qf1.courtName,
                                        qf1.teamA,
                                        qf1.teamB
                                    )
                                }
                                statusBanner = "✓ Auto-seeded & shuffled bracket pairings for ${selectedTournament.name}!"
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_shuffle_bracket_button_${selectedTournament.id}")
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp), tint = OpticVolt)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Seed / Shuffle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val updated = TournamentBracketStore.advanceRound(selectedTournament)
                                localTournaments = localTournaments.map {
                                    if (it.id == selectedTournament.id) it.copy(activeRound = updated.activeRound) else it
                                }
                                val activeMatch = updated.matches.firstOrNull { it.status == "LIVE" } ?: updated.matches.last()
                                onUpdateTournamentBracket(
                                    selectedTournament,
                                    updated.activeRound,
                                    activeMatch.courtName,
                                    activeMatch.teamA,
                                    activeMatch.teamB
                                )
                                statusBanner = "✓ Advanced ${selectedTournament.name} to ${updated.activeRound} (${updated.progressPercent}% complete)!"
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OpticVolt,
                                contentColor = OpticVoltDarkText
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_advance_round_button_${selectedTournament.id}")
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Advance Round", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Bracket Matchups & Court Schedules (${bracketState.matches.size} Matches)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bracketState.matches.forEach { match ->
                    val statusColor = when (match.status) {
                        "COMPLETED" -> AvailableGreen
                        "LIVE" -> PeakAmber
                        else -> GCashBlue
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_bracket_match_${match.matchId}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = EmeraldDark,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${match.matchId} • ${match.roundName.uppercase()}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = OpticVolt,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${match.courtName} • ${match.scheduledTime}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${match.teamA} vs ${match.teamB}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("admin_match_teams_${match.matchId}")
                                    )
                                    Text(
                                        text = "Score/Status: ${match.scoreSummary}" +
                                            (match.winnerTeam?.let { " • Winner: $it ✓" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (match.winnerTeam != null) AvailableGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedButton(
                                    onClick = { editingMatch = match },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("admin_edit_match_${match.matchId}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Organize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateTournamentDialog) {
        CreateTournamentAdminDialog(
            onDismiss = { showCreateTournamentDialog = false },
            onConfirm = { name, division, format, skillCap, dateRange, fee, prize, maxTeams, court, teamA, teamB ->
                val newId = (localTournaments.maxOfOrNull { it.id } ?: 3) + 1
                val created = TournamentEntity(
                    id = newId,
                    name = name,
                    facilityName = "Smash Pickle Club • Quezon City",
                    dateRange = dateRange,
                    division = division,
                    format = format,
                    skillCap = skillCap,
                    entryFee = fee,
                    prizePool = prize,
                    registeredCount = 4,
                    maxTeams = maxTeams,
                    isJoinedByUser = false,
                    activeRound = "Quarter Finals",
                    activeCourt = court,
                    teamA = teamA,
                    teamB = teamB,
                    set1A = 0,
                    set1B = 0,
                    set2A = 0,
                    set2B = 0,
                    set3A = 0,
                    set3B = 0,
                    matchSubmitted = false
                )
                localTournaments = localTournaments + created
                selectedTournamentId = created.id
                TournamentBracketStore.ensureBracketForTournament(created)
                onCreateTournament(name, division, format, skillCap, dateRange, fee, prize, maxTeams, court, teamA, teamB)
                statusBanner = "✓ Created tournament '$name' ($division • $format) with organized bracket!"
                showCreateTournamentDialog = false
            }
        )
    }

    editingMatch?.let { targetMatch ->
        EditBracketMatchAdminDialog(
            match = targetMatch,
            onDismiss = { editingMatch = null },
            onSave = { teamA, teamB, courtName, scheduledTime, scoreSummary, winnerTeam, status ->
                val updatedState = TournamentBracketStore.updateBracketMatch(
                    tournament = selectedTournament,
                    matchId = targetMatch.matchId,
                    teamA = teamA,
                    teamB = teamB,
                    courtName = courtName,
                    scheduledTime = scheduledTime,
                    scoreSummary = scoreSummary,
                    winnerTeam = winnerTeam,
                    status = status
                )
                localTournaments = localTournaments.map {
                    if (it.id == selectedTournament.id) {
                        it.copy(
                            activeRound = updatedState.activeRound,
                            activeCourt = courtName,
                            teamA = if (targetMatch.matchId == "QF1") teamA else it.teamA,
                            teamB = if (targetMatch.matchId == "QF1") teamB else it.teamB
                        )
                    } else it
                }
                onUpdateTournamentBracket(
                    selectedTournament,
                    updatedState.activeRound,
                    courtName,
                    teamA,
                    teamB
                )
                statusBanner = "✓ Updated ${targetMatch.matchId}: $teamA vs $teamB on $courtName ($scheduledTime)"
                editingMatch = null
            }
        )
    }
}

@Composable
private fun CreateTournamentAdminDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        division: String,
        format: String,
        skillCap: String,
        dateRange: String,
        entryFee: Int,
        prizePool: Int,
        maxTeams: Int,
        activeCourt: String,
        teamA: String,
        teamB: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("Smash Autumn Championship") }
    var division by remember { mutableStateOf("Doubles") }
    var format by remember { mutableStateOf("Knockout") }
    var skillCap by remember { mutableStateOf("DUPR 3.5 – 4.5") }
    var dateRange by remember { mutableStateOf("Oct 24 – 25, 2026") }
    var entryFee by remember { mutableStateOf("900") }
    var prizePool by remember { mutableStateOf("60000") }
    var maxTeams by remember { mutableStateOf("16") }
    var activeCourt by remember { mutableStateOf("Court 1") }
    var teamA by remember { mutableStateOf("Jonel / Carlo") }
    var teamB by remember { mutableStateOf("Kevin / Rico") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_create_tournament_dialog"),
        title = { Text("Organize New Tournament Bracket") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tournament Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_tourney_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = division,
                        onValueChange = { division = it },
                        label = { Text("Division") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_tourney_division_input")
                    )
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        label = { Text("Format") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_tourney_format_input")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = skillCap,
                        onValueChange = { skillCap = it },
                        label = { Text("Skill Cap (DUPR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dateRange,
                        onValueChange = { dateRange = it },
                        label = { Text("Date Range") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = entryFee,
                        onValueChange = { entryFee = it },
                        label = { Text("Entry Fee (₱)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prizePool,
                        onValueChange = { prizePool = it },
                        label = { Text("Prize Pool (₱)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { teamA = it },
                        label = { Text("Seed #1 Team A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { teamB = it },
                        label = { Text("Seed #2 Team B") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name.ifBlank { "PicklePlay Club Open" },
                        division.ifBlank { "Doubles" },
                        format.ifBlank { "Knockout" },
                        skillCap.ifBlank { "DUPR 3.5+" },
                        dateRange.ifBlank { "Oct 24 – 25, 2026" },
                        entryFee.toIntOrNull() ?: 800,
                        prizePool.toIntOrNull() ?: 50000,
                        maxTeams.toIntOrNull() ?: 16,
                        activeCourt.ifBlank { "Court 1" },
                        teamA.ifBlank { "Jonel / Carlo" },
                        teamB.ifBlank { "Mark / James" }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("admin_confirm_create_tournament_button")
            ) {
                Text("Create Bracket")
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
private fun EditBracketMatchAdminDialog(
    match: TournamentMatchItem,
    onDismiss: () -> Unit,
    onSave: (
        teamA: String,
        teamB: String,
        courtName: String,
        scheduledTime: String,
        scoreSummary: String,
        winnerTeam: String?,
        status: String
    ) -> Unit
) {
    var teamA by remember(match) { mutableStateOf(match.teamA) }
    var teamB by remember(match) { mutableStateOf(match.teamB) }
    var courtName by remember(match) { mutableStateOf(match.courtName) }
    var scheduledTime by remember(match) { mutableStateOf(match.scheduledTime) }
    var scoreSummary by remember(match) { mutableStateOf(match.scoreSummary) }
    var winnerTeam by remember(match) { mutableStateOf(match.winnerTeam ?: "") }
    var status by remember(match) { mutableStateOf(match.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("admin_edit_match_dialog"),
        title = { Text("Organize ${match.matchLabel} (${match.matchId})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = teamA,
                    onValueChange = { teamA = it },
                    label = { Text("Team A") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_match_team_a_input")
                )
                OutlinedTextField(
                    value = teamB,
                    onValueChange = { teamB = it },
                    label = { Text("Team B") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_match_team_b_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = courtName,
                        onValueChange = { courtName = it },
                        label = { Text("Assigned Court") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_match_court_input")
                    )
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Match Schedule") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_match_time_input")
                    )
                }
                OutlinedTextField(
                    value = scoreSummary,
                    onValueChange = { scoreSummary = it },
                    label = { Text("Match Score / Summary") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_edit_match_score_input")
                )

                Text(
                    text = "Quick Declare Winner & Advance Bracket:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            winnerTeam = teamA
                            status = "COMPLETED"
                            if (scoreSummary.contains("Scheduled") || scoreSummary.contains("Live")) {
                                scoreSummary = "11-8, 11-7"
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_declare_winner_a")
                    ) {
                        Text("Winner: $teamA", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            winnerTeam = teamB
                            status = "COMPLETED"
                            if (scoreSummary.contains("Scheduled") || scoreSummary.contains("Live")) {
                                scoreSummary = "8-11, 7-11"
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_declare_winner_b")
                    ) {
                        Text("Winner: $teamB", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        teamA,
                        teamB,
                        courtName,
                        scheduledTime,
                        scoreSummary,
                        winnerTeam.ifBlank { null },
                        status
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("admin_save_match_button")
            ) {
                Text("Save Bracket Match")
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
fun PickleballTournamentBracketVisualizer(
    tournament: TournamentEntity,
    bracketState: TournamentBracketState = TournamentBracketStore.getBracketState(tournament),
    onMatchSelected: (TournamentMatchItem) -> Unit = {},
    onAdvanceWinner: ((TournamentMatchItem, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val qfMatches = remember(bracketState.matches) {
        bracketState.matches.filter { it.roundStageIndex == 1 }
    }
    val sfMatches = remember(bracketState.matches) {
        bracketState.matches.filter { it.roundStageIndex == 2 }
    }
    val finalMatch = remember(bracketState.matches) {
        bracketState.matches.firstOrNull { it.roundStageIndex == 3 }
    }

    var selectedMatchId by remember(bracketState.tournamentId) {
        mutableStateOf(
            bracketState.matches.firstOrNull { it.status == "LIVE" }?.matchId
                ?: bracketState.matches.firstOrNull()?.matchId
                ?: "QF1"
        )
    }
    val selectedMatch = remember(bracketState.matches, selectedMatchId) {
        bracketState.matches.find { it.matchId == selectedMatchId }
            ?: bracketState.matches.firstOrNull()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tournament_bracket_visualizer_${tournament.id}"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF07261B),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Bracket Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = OpticVolt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LIVE BRACKET TREE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = OpticVoltDarkText,
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("bracket_tree_badge_${tournament.id}")
                            )
                        }
                        Text(
                            text = "${tournament.division} • ${tournament.format}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${tournament.name} Bracket Visualization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.testTag("bracket_visualizer_title_${tournament.id}")
                    )
                }

                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = bracketState.activeRound,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = OpticVolt,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("bracket_visualizer_active_round_${tournament.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Champion Banner if tournament has crowned a winner
            val resolvedChampion = bracketState.championTeam ?: finalMatch?.winnerTeam
            if (!resolvedChampion.isNullOrBlank()) {
                Surface(
                    color = PeakAmber.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PeakAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bracket_champion_banner_${tournament.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = PeakAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "TOURNAMENT CHAMPION CROWNED",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PeakAmber
                                )
                                Text(
                                    text = "🏆 $resolvedChampion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "₱${tournament.prizePool}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = OpticVolt
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Horizontally scrollable Multi-Column Knockout Bracket Tree with Canvas Connector Lines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("bracket_tree_scroll_container_${tournament.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // COLUMN 1: QUARTER FINALS
                Column(
                    modifier = Modifier
                        .width(176.dp)
                        .testTag("bracket_column_qf_${tournament.id}"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BracketRoundHeaderPill(
                        title = "QUARTER FINALS",
                        countLabel = "${qfMatches.size} Matches",
                        isActive = bracketState.activeRound.contains("Quarter", ignoreCase = true)
                    )
                    qfMatches.forEach { match ->
                        BracketMatchNodeCard(
                            tournamentId = tournament.id,
                            match = match,
                            isSelected = selectedMatch?.matchId == match.matchId,
                            onClick = {
                                selectedMatchId = match.matchId
                                onMatchSelected(match)
                            }
                        )
                    }
                }

                // CONNECTOR 1: Quarter Finals -> Semi Finals
                if (sfMatches.isNotEmpty()) {
                    BracketTreeForkConnector(
                        pairCount = sfMatches.size.coerceAtLeast(1),
                        totalHeightDp = if (qfMatches.size >= 4) 340 else 170,
                        isHighlighted = sfMatches.any { it.status == "LIVE" || it.status == "COMPLETED" },
                        modifier = Modifier.testTag("bracket_connector_qf_sf_${tournament.id}")
                    )

                    // COLUMN 2: SEMI FINALS
                    Column(
                        modifier = Modifier
                            .width(176.dp)
                            .testTag("bracket_column_sf_${tournament.id}"),
                        verticalArrangement = Arrangement.spacedBy(if (qfMatches.size >= 4) 46.dp else 14.dp)
                    ) {
                        BracketRoundHeaderPill(
                            title = "SEMI FINALS",
                            countLabel = "${sfMatches.size} Matches",
                            isActive = bracketState.activeRound.contains("Semi", ignoreCase = true)
                        )
                        sfMatches.forEach { match ->
                            BracketMatchNodeCard(
                                tournamentId = tournament.id,
                                match = match,
                                isSelected = selectedMatch?.matchId == match.matchId,
                                onClick = {
                                    selectedMatchId = match.matchId
                                    onMatchSelected(match)
                                }
                            )
                        }
                    }
                }

                // CONNECTOR 2: Semi Finals -> Championship Final
                if (finalMatch != null) {
                    BracketTreeForkConnector(
                        pairCount = 1,
                        totalHeightDp = if (sfMatches.size >= 2) 210 else 120,
                        isHighlighted = finalMatch.status == "LIVE" || finalMatch.status == "COMPLETED",
                        modifier = Modifier.testTag("bracket_connector_sf_final_${tournament.id}")
                    )

                    // COLUMN 3: GRAND FINAL
                    Column(
                        modifier = Modifier
                            .width(186.dp)
                            .testTag("bracket_column_final_${tournament.id}"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BracketRoundHeaderPill(
                            title = "🏆 GRAND FINAL",
                            countLabel = "₱${tournament.prizePool}",
                            isActive = bracketState.activeRound.contains("Final", ignoreCase = true) &&
                                !bracketState.activeRound.contains("Quarter", ignoreCase = true) &&
                                !bracketState.activeRound.contains("Semi", ignoreCase = true),
                            isChampionship = true
                        )
                        BracketMatchNodeCard(
                            tournamentId = tournament.id,
                            match = finalMatch,
                            isSelected = selectedMatch?.matchId == finalMatch.matchId,
                            isFinalNode = true,
                            onClick = {
                                selectedMatchId = finalMatch.matchId
                                onMatchSelected(finalMatch)
                            }
                        )
                    }
                }
            }

            // Interactive Selected Match Inspector & Quick Winner Advancement Controls
            selectedMatch?.let { activeMatch ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0E3828),
                    border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bracket_selected_match_panel_${tournament.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Matchup: ${activeMatch.matchId} (${activeMatch.roundName})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = OpticVolt,
                                modifier = Modifier.testTag("bracket_selected_match_label_${tournament.id}")
                            )
                            Text(
                                text = "${activeMatch.courtName} • ${activeMatch.scheduledTime}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFFD6F5E6)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${activeMatch.teamA} vs ${activeMatch.teamB} • ${activeMatch.scoreSummary}" +
                                (activeMatch.winnerTeam?.let { " • Winner: $it ✓" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.testTag("bracket_selected_match_teams_${tournament.id}")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    TournamentBracketStore.updateBracketMatch(
                                        tournament = tournament,
                                        matchId = activeMatch.matchId,
                                        teamA = activeMatch.teamA,
                                        teamB = activeMatch.teamB,
                                        courtName = activeMatch.courtName,
                                        scheduledTime = activeMatch.scheduledTime,
                                        scoreSummary = "11-8, 11-7",
                                        winnerTeam = activeMatch.teamA,
                                        status = "COMPLETED"
                                    )
                                    onAdvanceWinner?.invoke(activeMatch, activeMatch.teamA)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeMatch.winnerTeam == activeMatch.teamA) OpticVolt else Color(0xFF6EE7B7)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (activeMatch.winnerTeam == activeMatch.teamA) {
                                        OpticVolt.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bracket_advance_team_a_${tournament.id}")
                            ) {
                                Text(
                                    text = "Advance ${activeMatch.teamA}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    TournamentBracketStore.updateBracketMatch(
                                        tournament = tournament,
                                        matchId = activeMatch.matchId,
                                        teamA = activeMatch.teamA,
                                        teamB = activeMatch.teamB,
                                        courtName = activeMatch.courtName,
                                        scheduledTime = activeMatch.scheduledTime,
                                        scoreSummary = "8-11, 7-11",
                                        winnerTeam = activeMatch.teamB,
                                        status = "COMPLETED"
                                    )
                                    onAdvanceWinner?.invoke(activeMatch, activeMatch.teamB)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeMatch.winnerTeam == activeMatch.teamB) OpticVolt else Color(0xFF6EE7B7)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (activeMatch.winnerTeam == activeMatch.teamB) {
                                        OpticVolt.copy(alpha = 0.18f)
                                    } else {
                                        Color.Transparent
                                    },
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bracket_advance_team_b_${tournament.id}")
                            ) {
                                Text(
                                    text = "Advance ${activeMatch.teamB}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketRoundHeaderPill(
    title: String,
    countLabel: String,
    isActive: Boolean,
    isChampionship: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isChampionship -> PeakAmber.copy(alpha = 0.2f)
            isActive -> EmeraldPrimary.copy(alpha = 0.35f)
            else -> Color.White.copy(alpha = 0.08f)
        },
        border = BorderStroke(
            1.dp,
            when {
                isChampionship -> PeakAmber
                isActive -> OpticVolt
                else -> Color.White.copy(alpha = 0.18f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = if (isChampionship) PeakAmber else if (isActive) OpticVolt else Color.White
            )
            Text(
                text = countLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = Color(0xFFD6F5E6)
            )
        }
    }
}

@Composable
private fun BracketMatchNodeCard(
    tournamentId: Int,
    match: TournamentMatchItem,
    isSelected: Boolean,
    isFinalNode: Boolean = false,
    onClick: () -> Unit
) {
    val statusAccent = when (match.status) {
        "COMPLETED" -> AvailableGreen
        "LIVE" -> OpticVolt
        else -> Color(0xFF60A5FA)
    }
    val teamAWon = match.winnerTeam != null && match.winnerTeam.equals(match.teamA, ignoreCase = true)
    val teamBWon = match.winnerTeam != null && match.winnerTeam.equals(match.teamB, ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF124230) else Color(0xFF0B3123),
        border = BorderStroke(
            width = if (isSelected || isFinalNode) 1.5.dp else 1.dp,
            color = when {
                isSelected -> OpticVolt
                isFinalNode -> PeakAmber.copy(alpha = 0.8f)
                else -> statusAccent.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bracket_visual_node_${tournamentId}_${match.matchId}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.matchId} • ${match.courtName}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = if (isFinalNode) PeakAmber else OpticVolt
                )
                Surface(
                    color = statusAccent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = match.status,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        color = statusAccent,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Team A Slot
            BracketTeamSlotRow(
                teamName = match.teamA,
                skillBadge = match.teamASkill,
                isWinner = teamAWon,
                testTag = "bracket_node_team_a_${tournamentId}_${match.matchId}"
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Team B Slot
            BracketTeamSlotRow(
                teamName = match.teamB,
                skillBadge = match.teamBSkill,
                isWinner = teamBWon,
                testTag = "bracket_node_team_b_${tournamentId}_${match.matchId}"
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = match.scoreSummary,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = Color(0xFFA7F3D0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("bracket_node_score_${tournamentId}_${match.matchId}")
            )
        }
    }
}

@Composable
private fun BracketTeamSlotRow(
    teamName: String,
    skillBadge: String,
    isWinner: Boolean,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isWinner) AvailableGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (isWinner) OpticVolt.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isWinner) "✓ $teamName" else teamName,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isWinner) OpticVolt else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = skillBadge.replace("DUPR ", ""),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun BracketTreeForkConnector(
    pairCount: Int,
    totalHeightDp: Int,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (isHighlighted) OpticVolt else EmeraldPrimary.copy(alpha = 0.65f)
    Canvas(
        modifier = modifier
            .width(26.dp)
            .height(totalHeightDp.dp)
    ) {
        val strokePx = 2.dp.toPx()
        val midX = size.width * 0.5f
        val segHeight = size.height / pairCount.coerceAtLeast(1)

        for (i in 0 until pairCount.coerceAtLeast(1)) {
            val topY = segHeight * i + segHeight * 0.25f
            val bottomY = segHeight * i + segHeight * 0.75f
            val centerY = (topY + bottomY) * 0.5f

            // Top incoming horizontal arm
            drawLine(
                color = lineColor,
                start = Offset(0f, topY),
                end = Offset(midX, topY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            // Bottom incoming horizontal arm
            drawLine(
                color = lineColor,
                start = Offset(0f, bottomY),
                end = Offset(midX, bottomY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            // Vertical bridge connecting top and bottom arms
            drawLine(
                color = lineColor,
                start = Offset(midX, topY),
                end = Offset(midX, bottomY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            // Outgoing horizontal arm to next round node
            drawLine(
                color = lineColor,
                start = Offset(midX, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun LocalPickleballTournamentBracketCard(
    tournaments: List<TournamentEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val effectiveTournaments = remember(tournaments) {
        if (tournaments.isNotEmpty()) tournaments else DefaultAdminTournaments
    }
    var selectedTournamentId by remember {
        mutableIntStateOf(effectiveTournaments.firstOrNull()?.id ?: 1)
    }
    val activeTournament = effectiveTournaments.find { it.id == selectedTournamentId }
        ?: effectiveTournaments.first()

    val bracketMap by TournamentBracketStore.brackets.collectAsState()
    val bracketState = bracketMap[activeTournament.id]
        ?: remember(activeTournament) { TournamentBracketStore.getBracketState(activeTournament) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("local_pickleball_tournament_bracket_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.45f))
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
                            text = "Local Pickleball Competition Bracket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Interactive knockout bracket tree for local club tournaments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (effectiveTournaments.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(effectiveTournaments, key = { it.id }) { tourney ->
                        FilterChip(
                            selected = tourney.id == activeTournament.id,
                            onClick = { selectedTournamentId = tourney.id },
                            label = {
                                Text(
                                    text = "${tourney.name} (${tourney.division})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("select_competition_bracket_${tourney.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PickleballTournamentBracketVisualizer(
                tournament = activeTournament,
                bracketState = bracketState
            )
        }
    }
}

val MatchmakerSkillFilterOptions = listOf("All", "Beginner", "Intermediate", "Advanced", "All Levels")

fun filterOpenPlayGamesBySkill(
    games: List<OpenPlayGameEntity>,
    skillFilter: String,
    onlyOpenSlots: Boolean = false
): List<OpenPlayGameEntity> {
    val cleanSkill = skillFilter.trim()
    return games.filter { game ->
        val matchesSkill = cleanSkill.isEmpty() ||
            cleanSkill.equals("All", ignoreCase = true) ||
            game.skillLevel.equals(cleanSkill, ignoreCase = true)
        val metrics = resolveOpenPlaySessionMetrics(game)
        val matchesVacancy = !onlyOpenSlots || metrics.vacantCount > 0
        matchesSkill && matchesVacancy
    }
}

fun buildMatchmakerOpenSlotGame(
    newId: Int,
    title: String,
    facilityName: String,
    courtName: String,
    dayLabel: String,
    timeRange: String,
    skillLevel: String,
    pricePerPlayer: Int,
    openSlotsNeeded: Int,
    maxPlayers: Int = 6,
    hostName: String = "Jonel P.",
    hostShortName: String = "Jonel",
    hostDupr: String = "3.5"
): OpenPlayGameEntity {
    val safeOpenSlots = openSlotsNeeded.coerceIn(1, 7)
    val resolvedMaxPlayers = maxPlayers.coerceAtLeast(safeOpenSlots + 1)
    val initialFilled = (resolvedMaxPlayers - safeOpenSlots).coerceAtLeast(1)
    val companionPool = listOf(
        "Carlo|$skillLevel|3.7",
        "Maria|$skillLevel|3.6",
        "Jon|$skillLevel|3.4",
        "Sofia|$skillLevel|3.2",
        "Miggy|$skillLevel|3.3"
    )
    val rosterEntries = buildList {
        add("$hostShortName|$skillLevel|$hostDupr")
        for (idx in 0 until (initialFilled - 1)) {
            add(companionPool.getOrElse(idx) { "Player${idx + 2}|$skillLevel|$hostDupr" })
        }
    }
    return OpenPlayGameEntity(
        id = newId,
        title = title.ifBlank { "Matchmaker Open Play" },
        facilityId = 1,
        facilityName = facilityName.ifBlank { "Smash Pickle Club" },
        courtName = courtName.ifBlank { "Court 2" },
        dayLabel = dayLabel.ifBlank { "Saturday" },
        dateLabel = "Upcoming ${dayLabel.ifBlank { "Saturday" }}",
        timeRange = timeRange.ifBlank { "6:00 PM - 8:00 PM" },
        skillLevel = skillLevel.ifBlank { "Intermediate" },
        pricePerPlayer = pricePerPlayer.coerceAtLeast(0),
        maxPlayers = resolvedMaxPlayers,
        playersCsv = rosterEntries.joinToString(","),
        isJoinedByUser = true,
        hostName = hostName
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchmakerFeatureCard(
    games: List<OpenPlayGameEntity>,
    selectedSkillFilter: String,
    onlyOpenSlots: Boolean = false,
    userSkillLevel: String = "Intermediate",
    userDuprRating: Double = 3.5,
    showEmbeddedSessions: Boolean = false,
    onSelectSkillFilter: (String) -> Unit,
    onToggleOnlyOpenSlots: () -> Unit = {},
    onPostOpenSlots: (
        title: String,
        facilityName: String,
        courtName: String,
        dayLabel: String,
        timeRange: String,
        skillLevel: String,
        pricePerPlayer: Int,
        openSlotsNeeded: Int,
        maxPlayers: Int
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onToggleJoinGame: (OpenPlayGameEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPostSlotsDialog by remember { mutableStateOf(false) }
    var lastPostedBanner by remember { mutableStateOf<String?>(null) }

    val filteredGames = remember(games, selectedSkillFilter, onlyOpenSlots) {
        filterOpenPlayGamesBySkill(games, selectedSkillFilter, onlyOpenSlots)
    }
    val totalFilteredVacant = remember(filteredGames) {
        filteredGames.sumOf { resolveOpenPlaySessionMetrics(it).vacantCount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("matchmaker_feature_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = EmeraldDark,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MATCHMAKER • SKILL-LEVEL FILTER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = OpticVolt,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("matchmaker_badge")
                            )
                        }
                        Surface(
                            color = AvailableGreenBg,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${filteredGames.size} Games • $totalFilteredVacant Open Slots",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = AvailableGreen,
                                modifier = Modifier
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .testTag("matchmaker_filtered_stats_badge")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "PicklePlay Matchmaker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("matchmaker_header_title")
                    )
                    Text(
                        text = "Post open slots for your court game or filter open play sessions by local skill level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showPostSlotsDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldDark,
                        contentColor = OpticVolt
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("matchmaker_post_open_slots_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Post Open Slots", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (lastPostedBanner != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = AvailableGreenBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matchmaker_posted_banner")
                ) {
                    Text(
                        text = lastPostedBanner!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AvailableGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Local Skill-Level Filtering Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Local Skill-Level Filter:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedSkillFilter.equals(userSkillLevel, ignoreCase = true)) {
                        EmeraldPrimary
                    } else {
                        EmeraldPrimary.copy(alpha = 0.12f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedSkillFilter.equals(userSkillLevel, ignoreCase = true)) {
                                onSelectSkillFilter("All")
                            } else {
                                onSelectSkillFilter(userSkillLevel)
                            }
                        }
                        .testTag("matchmaker_my_skill_chip")
                ) {
                    Text(
                        text = "My Skill: $userSkillLevel (DUPR ${"%.1f".format(userDuprRating)})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedSkillFilter.equals(userSkillLevel, ignoreCase = true)) {
                            Color.White
                        } else {
                            EmeraldPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matchmaker_skill_filter_row")
            ) {
                items(MatchmakerSkillFilterOptions) { skill ->
                    val matchingCount = games.count {
                        skill == "All" || it.skillLevel.equals(skill, ignoreCase = true)
                    }
                    val isSelected = selectedSkillFilter.equals(skill, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSkillFilter(skill) },
                        label = {
                            Text(
                                text = "$skill ($matchingCount)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("matchmaker_skill_chip_$skill")
                    )
                }
                item {
                    FilterChip(
                        selected = onlyOpenSlots,
                        onClick = onToggleOnlyOpenSlots,
                        label = {
                            Text(
                                text = "Open Slots Only",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AvailableGreen,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("matchmaker_open_slots_only_chip")
                    )
                }
            }

            if (showEmbeddedSessions) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                if (filteredGames.isEmpty()) {
                    Text(
                        text = "No open play sessions match '$selectedSkillFilter'. Tap 'Post Open Slots' to create one!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("matchmaker_empty_state")
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("matchmaker_embedded_sessions_list")
                    ) {
                        filteredGames.forEach { session ->
                            val metrics = resolveOpenPlaySessionMetrics(session)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    if (session.isJoinedByUser) AvailableGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("matchmaker_session_row_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = session.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = EmeraldPrimary.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = session.skillLevel,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = EmeraldPrimary,
                                                    modifier = Modifier
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        .testTag("matchmaker_session_skill_${session.id}")
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${session.facilityName} • ${session.courtName} • ${session.dayLabel} (${session.timeRange})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${metrics.vacantCount} Open Slots (${metrics.filledCount}/${metrics.maxPlayers} Joined) • ₱${session.pricePerPlayer}/player • Host: ${session.hostName}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (metrics.vacantCount > 0) AvailableGreen else MaintenanceRed,
                                            modifier = Modifier.testTag("matchmaker_session_slots_${session.id}")
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onToggleJoinGame(session) },
                                        enabled = session.isJoinedByUser || metrics.vacantCount > 0,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (session.isJoinedByUser) AvailableGreen else EmeraldPrimary,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("matchmaker_join_button_${session.id}")
                                    ) {
                                        Text(
                                            text = when {
                                                session.isJoinedByUser -> "Joined ✓"
                                                metrics.vacantCount == 0 -> "Full"
                                                else -> "Join Slot"
                                            },
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

    if (showPostSlotsDialog) {
        PostOpenSlotsDialog(
            defaultSkillLevel = if (selectedSkillFilter != "All") selectedSkillFilter else userSkillLevel,
            onDismiss = { showPostSlotsDialog = false },
            onConfirm = { title, facility, court, day, time, skill, fee, openSlots, maxPlayers ->
                onPostOpenSlots(title, facility, court, day, time, skill, fee, openSlots, maxPlayers)
                lastPostedBanner = "✓ Posted '$title' ($skill) with $openSlots open slot(s) on $court!"
                showPostSlotsDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostOpenSlotsDialog(
    defaultSkillLevel: String = "Intermediate",
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        facilityName: String,
        courtName: String,
        dayLabel: String,
        timeRange: String,
        skillLevel: String,
        pricePerPlayer: Int,
        openSlotsNeeded: Int,
        maxPlayers: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("Doubles Matchmaker Game") }
    var facility by remember { mutableStateOf("Smash Pickle Club") }
    var court by remember { mutableStateOf("Court 3") }
    var day by remember { mutableStateOf("Saturday") }
    var time by remember { mutableStateOf("6:00 PM - 8:00 PM") }
    var skill by remember { mutableStateOf(defaultSkillLevel.ifBlank { "Intermediate" }) }
    var fee by remember { mutableStateOf("150") }
    var openSlotsNeeded by remember { mutableIntStateOf(2) }
    var maxPlayers by remember { mutableIntStateOf(4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("matchmaker_post_slots_dialog"),
        title = { Text("Post Open Slots for Game") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Game / Session Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matchmaker_game_title_input")
                )
                OutlinedTextField(
                    value = facility,
                    onValueChange = { facility = it },
                    label = { Text("Club / Facility") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("matchmaker_facility_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = court,
                        onValueChange = { court = it },
                        label = { Text("Court") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matchmaker_court_input")
                    )
                    OutlinedTextField(
                        value = day,
                        onValueChange = { day = it },
                        label = { Text("Day") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matchmaker_day_input")
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
                            .testTag("matchmaker_time_input")
                    )
                    OutlinedTextField(
                        value = fee,
                        onValueChange = { fee = it },
                        label = { Text("Fee (₱)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matchmaker_fee_input")
                    )
                }

                Text(
                    text = "Open Slots Needed (Looking for Players):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4, 5).forEach { slots ->
                        FilterChip(
                            selected = openSlotsNeeded == slots,
                            onClick = {
                                openSlotsNeeded = slots
                                if (maxPlayers <= slots) {
                                    maxPlayers = (slots + 1).coerceAtLeast(4)
                                }
                            },
                            label = {
                                Text(
                                    text = if (slots == 1) "1 Open Slot" else "$slots Open Slots",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldDark,
                                selectedLabelColor = OpticVolt
                            ),
                            modifier = Modifier.testTag("matchmaker_open_slots_chip_$slots")
                        )
                    }
                }

                Text(
                    text = "Required Skill Level Filter:",
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
                            modifier = Modifier.testTag("matchmaker_post_skill_chip_$option")
                        )
                    }
                }

                Text(
                    text = "Total Court Capacity:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 6, 8).forEach { cap ->
                        FilterChip(
                            selected = maxPlayers == cap,
                            onClick = {
                                maxPlayers = cap
                                if (openSlotsNeeded >= cap) {
                                    openSlotsNeeded = (cap - 1).coerceAtLeast(1)
                                }
                            },
                            label = { Text("$cap Players Max", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("matchmaker_max_players_chip_$cap")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resolvedMax = maxPlayers.coerceAtLeast(openSlotsNeeded + 1)
                    onConfirm(
                        title.ifBlank { "Doubles Matchmaker Game" },
                        facility.ifBlank { "Smash Pickle Club" },
                        court.ifBlank { "Court 3" },
                        day.ifBlank { "Saturday" },
                        time.ifBlank { "6:00 PM - 8:00 PM" },
                        skill.ifBlank { "Intermediate" },
                        fee.toIntOrNull() ?: 150,
                        openSlotsNeeded,
                        resolvedMax
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_post_open_slots_button")
            ) {
                Text("Post Open Slots")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


