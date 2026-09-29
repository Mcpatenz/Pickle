package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository
import com.example.data.remote.OpenPlaySkillQueueDocument
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.AvailableGreenBg
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MaintenanceRed
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber

/**
 * Real-time 'Open Play' Matchmaking Screen where users can join a queue for their skill level
 * (e.g., 3.0, 3.5, 4.0, All Levels), leveraging Firebase Firestore real-time updates.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OpenPlayMatchmakingScreen(
    modifier: Modifier = Modifier,
    playerName: String = "Jonel P.",
    initialSelectedSkillLevel: String = "All",
    onQueueUpdated: (OpenPlaySkillQueueDocument) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirestoreTournamentsAndMatchmakingRepository.ensureInitialized(context)
    }

    val queues by FirestoreTournamentsAndMatchmakingRepository.queuesFlow.collectAsState()
    val realtimeEventText by FirestoreTournamentsAndMatchmakingRepository.lastRealtimeQueueEvent.collectAsState()

    var selectedSkillFilter by remember(initialSelectedSkillLevel) {
        mutableStateOf(initialSelectedSkillLevel)
    }

    val filteredQueues = remember(queues, selectedSkillFilter) {
        if (selectedSkillFilter.equals("All", ignoreCase = true)) {
            queues
        } else {
            queues.filter {
                it.skillLevel.equals(selectedSkillFilter, ignoreCase = true) ||
                    it.tierTitle.contains(selectedSkillFilter, ignoreCase = true)
            }
        }
    }

    val totalPlayersQueued = remember(queues) {
        queues.sumOf { it.filledSlots }
    }
    val userJoinedQueuesCount = remember(queues) {
        queues.count { it.isCurrentUserJoined }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("open_play_matchmaking_screen"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("open_play_realtime_matchmaking_card"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Real-Time Matchmaking Header Banner
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
                                    .testTag("matchmaking_firestore_collection_badge"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Firestore Real-Time Matchmaking",
                                    tint = OpticVoltDarkText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "FIRESTORE REAL-TIME • '${FirestoreTournamentsAndMatchmakingRepository.COLLECTION_OPEN_PLAY_QUEUES}'",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpticVoltDarkText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Real-Time Open Play Skill Queues",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("matchmaking_screen_title")
                        )
                        Text(
                            text = "$totalPlayersQueued Players Queued • $userJoinedQueuesCount Active Queue Joined • Player: $playerName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6),
                            modifier = Modifier.testTag("matchmaking_screen_subtitle")
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpticVolt.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "LIVE SYNC",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Live Firestore Real-Time Event Feed Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("matchmaking_realtime_status_banner"),
                shape = RoundedCornerShape(12.dp),
                color = AvailableGreenBg,
                border = BorderStroke(1.dp, AvailableGreen.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Real-Time Update",
                        tint = AvailableGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = realtimeEventText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matchmaking_realtime_event_text")
                    )
                }
            }

            // 3. Skill-Level Queue Filter Selector (All, 3.0, 3.5, 4.0, All Levels)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Filter Matchmaking Queues by Skill Level:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        "All" to "All Queues",
                        "3.0" to "DUPR 3.0",
                        "3.5" to "DUPR 3.5",
                        "4.0" to "DUPR 4.0+",
                        "All Levels" to "All Levels"
                    )
                    filterOptions.forEach { (skillKey, label) ->
                        val isSelected = selectedSkillFilter.equals(skillKey, ignoreCase = true)
                        val tagSuffix = skillKey.lowercase().replace(".", "_").replace(" ", "_")
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSkillFilter = skillKey }
                                .testTag("matchmaking_skill_selector_$tagSuffix"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 4. Skill-Level Matchmaking Queue Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredQueues.forEach { queue ->
                    SkillMatchmakingQueueCard(
                        queue = queue,
                        playerName = playerName,
                        onToggleJoinQueue = {
                            val updated = if (queue.isCurrentUserJoined) {
                                FirestoreTournamentsAndMatchmakingRepository.leaveQueueForSkillLevel(
                                    context = context,
                                    queueIdOrSkillLevel = queue.queueId,
                                    playerName = playerName
                                )
                            } else {
                                FirestoreTournamentsAndMatchmakingRepository.joinQueueForSkillLevel(
                                    context = context,
                                    queueIdOrSkillLevel = queue.queueId,
                                    playerName = playerName,
                                    skillRating = queue.skillLevel
                                )
                            }
                            if (updated != null) {
                                onQueueUpdated(updated)
                            }
                        },
                        onSimulatePeerJoin = {
                            val updated = FirestoreTournamentsAndMatchmakingRepository.simulateIncomingRealtimePeerJoin(
                                context = context,
                                queueIdOrSkillLevel = queue.queueId
                            )
                            if (updated != null) {
                                onQueueUpdated(updated)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillMatchmakingQueueCard(
    queue: OpenPlaySkillQueueDocument,
    playerName: String,
    onToggleJoinQueue: () -> Unit,
    onSimulatePeerJoin: () -> Unit
) {
    val skillTagSuffix = queue.skillLevel.lowercase().replace(".", "_").replace(" ", "_")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("matchmaking_queue_card_${queue.queueId}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(
            width = if (queue.isCurrentUserJoined) 1.5.dp else 1.dp,
            color = if (queue.isCurrentUserJoined) AvailableGreen else EmeraldPrimary.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Queue Header: Skill Level & Live Pod Status
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
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = OpticVolt,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = queue.tierTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("matchmaking_queue_title_${queue.queueId}")
                        )
                        Text(
                            text = "${queue.facilityName} • ${queue.courtAssignment} • ${queue.sessionWindow}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (queue.isPodReady) AvailableGreenBg else EmeraldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (queue.isPodReady) AvailableGreen else EmeraldPrimary.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = "${queue.filledSlots}/${queue.maxPlayersPerPod} IN QUEUE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (queue.isPodReady) AvailableGreen else EmeraldPrimary,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("matchmaking_queue_count_${queue.queueId}")
                    )
                }
            }

            // Status Headline
            Text(
                text = queue.statusHeadline,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (queue.isPodReady) AvailableGreen else EmeraldPrimary,
                modifier = Modifier.testTag("matchmaking_queue_status_${queue.queueId}")
            )

            // 4-Player Matchmaking Pod Slots Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (0 until queue.maxPlayersPerPod).forEach { slotIndex ->
                    val entry = queue.queuedPlayers.getOrNull(slotIndex)
                    val isFilled = entry != null
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matchmaking_pod_slot_${queue.queueId}_$slotIndex"),
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            entry?.isCurrentUser == true -> AvailableGreenBg
                            isFilled -> EmeraldPrimary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                entry?.isCurrentUser == true -> AvailableGreen
                                isFilled -> EmeraldPrimary.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = entry?.playerName ?: "OPEN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isFilled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (entry != null) "DUPR ${entry.skillRating}" else "Slot #${slotIndex + 1}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = if (isFilled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Join Queue / Leave Queue + Simulate Live Peer Update Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSimulatePeerJoin,
                    enabled = !queue.isPodReady,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("matchmaking_simulate_peer_join_${queue.queueId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ Live Peer Join",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Button(
                    onClick = onToggleJoinQueue,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (queue.isCurrentUserJoined) MaintenanceRed else EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .testTag("matchmaking_join_queue_button_${queue.queueId}")
                ) {
                    Icon(
                        imageVector = if (queue.isCurrentUserJoined) Icons.Default.CheckCircle else Icons.Default.SportsTennis,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (queue.isCurrentUserJoined) "Joined Queue ✓ (Leave)" else "Join ${queue.skillLevel} Queue",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.testTag("matchmaking_join_skill_label_$skillTagSuffix")
                    )
                }
            }
        }
    }
}
