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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.BookingEntity
import com.example.data.local.TournamentEntity
import com.example.data.remote.FirestoreReservationRepository
import com.example.data.remote.FirestoreTournamentsAndMatchmakingRepository
import com.example.data.remote.OpenPlaySkillQueueDocument
import com.example.ui.theme.AvailableGreen
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GCashBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OpticVolt
import com.example.ui.theme.OpticVoltDarkText
import com.example.ui.theme.PeakAmber
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Push notification categories supported by PicklePlay's Firebase Cloud Messaging (FCM) integration.
 */
enum class FcmNotificationCategory(
    val topicName: String,
    val channelId: String,
    val channelName: String,
    val badgeLabel: String,
    val displayTitle: String,
    val actionButtonText: String,
    val targetSectionKey: String
) {
    UPCOMING_RESERVATION(
        topicName = "upcoming_reservations",
        channelId = "pickleplay_fcm_reservations_channel",
        channelName = "Upcoming Court Reservations (FCM)",
        badgeLabel = "UPCOMING RESERVATION",
        displayTitle = "Upcoming Reservations",
        actionButtonText = "View Reservation",
        targetSectionKey = "MY_RESERVATIONS"
    ),
    TOURNAMENT_START(
        topicName = "tournament_starts",
        channelId = "pickleplay_fcm_tournaments_channel",
        channelName = "Tournament Starts & Bracket Calls (FCM)",
        badgeLabel = "TOURNAMENT START",
        displayTitle = "Tournament Starts",
        actionButtonText = "Open Tournament",
        targetSectionKey = "GAMES"
    ),
    MATCHMAKING_UPDATE(
        topicName = "matchmaking_updates",
        channelId = "pickleplay_fcm_matchmaking_channel",
        channelName = "Open Play Matchmaking Updates (FCM)",
        badgeLabel = "MATCHMAKING UPDATE",
        displayTitle = "Matchmaking Updates",
        actionButtonText = "Open Skill Queue",
        targetSectionKey = "GAMES"
    );

    companion object {
        fun fromRaw(raw: String?): FcmNotificationCategory {
            if (raw.isNullOrBlank()) return UPCOMING_RESERVATION
            val normalized = raw.trim().uppercase()
            return when {
                normalized.contains("TOURNAMENT") -> TOURNAMENT_START
                normalized.contains("MATCHMAKING") || normalized.contains("QUEUE") || normalized.contains("OPEN_PLAY") -> MATCHMAKING_UPDATE
                else -> UPCOMING_RESERVATION
            }
        }
    }
}

/**
 * Data model representing an FCM push notification received or dispatched via Firebase Cloud Messaging.
 */
data class FcmPushPayload(
    val notificationId: Int,
    val fcmMessageId: String,
    val category: FcmNotificationCategory,
    val topic: String,
    val title: String,
    val body: String,
    val referenceCode: String,
    val subtitle: String,
    val actionLabel: String = category.actionButtonText,
    val targetSectionKey: String = category.targetSectionKey,
    val dataPayload: Map<String, String> = emptyMap(),
    val timestampLabel: String = "Just now",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * Android [FirebaseMessagingService] implementation that receives Firebase Cloud Messaging (FCM)
 * push notifications for upcoming court reservations, tournament starts, and matchmaking updates,
 * as well as device registration token refreshes.
 */
class PicklePlayFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmPushNotificationManager.onNewFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        FcmPushNotificationManager.handleRemoteMessage(applicationContext, remoteMessage)
    }
}

/**
 * Central manager for Firebase Cloud Messaging (FCM) token registration, topic subscriptions,
 * Firestore outbox synchronization (`fcm_push_notifications`), and push notification delivery
 * across Upcoming Reservations, Tournament Starts, and Matchmaking Updates.
 */
object FcmPushNotificationManager {

    const val DEFAULT_FCM_CHANNEL_ID = "pickleplay_fcm_push_channel"
    const val DEFAULT_FCM_CHANNEL_NAME = "PicklePlay FCM Push Notifications"
    const val COLLECTION_FCM_TOKENS = "fcm_device_tokens"
    const val COLLECTION_FCM_NOTIFICATIONS = "fcm_push_notifications"

    private var nextNotificationId = 8100
    private var firestoreListener: ListenerRegistration? = null

    private val defaultSeededMessages: List<FcmPushPayload> = listOf(
        FcmPushPayload(
            notificationId = 8097,
            fcmMessageId = "fcm-msg-res-20260928-105",
            category = FcmNotificationCategory.UPCOMING_RESERVATION,
            topic = FcmNotificationCategory.UPCOMING_RESERVATION.topicName,
            title = "Upcoming Reservation Reminder: Court 4 at 06:00 PM",
            body = "Reminder for Jonel P.: Your Smash Pickle Club reservation (#PKL-20260929-00105) on Court 4 starts at 6:00 PM - 7:00 PM. Have your QR Pass ready for check-in!",
            referenceCode = "PKL-20260929-00105",
            subtitle = "Smash Pickle Club • Court 4 • Starts in 30 mins",
            dataPayload = mapOf(
                "category" to FcmNotificationCategory.UPCOMING_RESERVATION.name,
                "bookingCode" to "PKL-20260929-00105",
                "courtName" to "Court 4",
                "facilityName" to "Smash Pickle Club"
            ),
            timestampLabel = "5m ago",
            isRead = false
        ),
        FcmPushPayload(
            notificationId = 8098,
            fcmMessageId = "fcm-msg-trn-20260928-001",
            category = FcmNotificationCategory.TOURNAMENT_START,
            topic = FcmNotificationCategory.TOURNAMENT_START.topicName,
            title = "Tournament Starting: PicklePlay Open 2026",
            body = "Quarter Finals bracket is now LIVE on Court 2! Team Jonel / Carlo vs. Mark / James (DUPR 3.5 – 4.0 Doubles). Report to Court 2 desk now.",
            referenceCode = "TOURNEY-OPEN-2026",
            subtitle = "Smash Pickle Club • Court 2 • Quarter Finals",
            dataPayload = mapOf(
                "category" to FcmNotificationCategory.TOURNAMENT_START.name,
                "tournamentId" to "1",
                "tournamentName" to "PicklePlay Open 2026",
                "activeCourt" to "Court 2",
                "activeRound" to "Quarter Finals"
            ),
            timestampLabel = "3m ago",
            isRead = false
        ),
        FcmPushPayload(
            notificationId = 8099,
            fcmMessageId = "fcm-msg-mmk-20260928-350",
            category = FcmNotificationCategory.MATCHMAKING_UPDATE,
            topic = FcmNotificationCategory.MATCHMAKING_UPDATE.topicName,
            title = "Matchmaking Update: DUPR 3.5 Queue (3/4 Players)",
            body = "1 spot left in DUPR 3.5 Competitive Kitchen Queue at Smash Pickle Club (Court 3). Maria L., Carlo V., and Jon D. are ready!",
            referenceCode = "QUEUE-3.5",
            subtitle = "DUPR 3.5 • Court 3 • 3/4 Queued",
            dataPayload = mapOf(
                "category" to FcmNotificationCategory.MATCHMAKING_UPDATE.name,
                "queueId" to "queue_3_5",
                "skillLevel" to "3.5",
                "courtAssignment" to "Court 3"
            ),
            timestampLabel = "1m ago",
            isRead = false
        )
    )

    private val _pushEnabled = MutableStateFlow(true)
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    private val _fcmToken = MutableStateFlow("fcm_live_token_pkl_2026_98a7bc41")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()

    private val _subscribedTopics = MutableStateFlow(
        setOf(
            FcmNotificationCategory.UPCOMING_RESERVATION.topicName,
            FcmNotificationCategory.TOURNAMENT_START.topicName,
            FcmNotificationCategory.MATCHMAKING_UPDATE.topicName
        )
    )
    val subscribedTopics: StateFlow<Set<String>> = _subscribedTopics.asStateFlow()

    private val _receivedMessages = MutableStateFlow<List<FcmPushPayload>>(defaultSeededMessages)
    val receivedMessages: StateFlow<List<FcmPushPayload>> = _receivedMessages.asStateFlow()

    private val _latestPushBanner = MutableStateFlow<FcmPushPayload?>(null)
    val latestPushBanner: StateFlow<FcmPushPayload?> = _latestPushBanner.asStateFlow()

    /**
     * Initializes FCM notification channels, requests the current device registration token from
     * [FirebaseMessaging], subscribes to active topics, and attaches a Firestore listener for
     * cloud push payloads.
     */
    fun ensureInitialized(context: Context?) {
        if (context != null) {
            ensureNotificationChannels(context)
        }
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled = true
            messaging.token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    onNewFcmToken(context, token)
                }
            }
            _subscribedTopics.value.forEach { topic ->
                messaging.subscribeToTopic(topic)
            }
        }
        attachFirestorePushListener(context)
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultChannel = NotificationChannel(
                DEFAULT_FCM_CHANNEL_ID,
                DEFAULT_FCM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Firebase Cloud Messaging push alerts for PicklePlay"
                enableVibration(true)
            }
            manager.createNotificationChannel(defaultChannel)

            FcmNotificationCategory.entries.forEach { category ->
                val channel = NotificationChannel(
                    category.channelId,
                    category.channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "FCM push alerts for ${category.displayTitle}"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun onNewFcmToken(context: Context?, token: String) {
        val cleanToken = token.trim().ifBlank { return }
        _fcmToken.value = cleanToken
        runCatching {
            val firestore = FirestoreReservationRepository.ensureFirestoreInstance(context)
            firestore?.collection(COLLECTION_FCM_TOKENS)
                ?.document(cleanToken.take(36))
                ?.set(
                    mapOf(
                        "token" to cleanToken,
                        "topics" to _subscribedTopics.value.toList(),
                        "updatedAtMillis" to System.currentTimeMillis(),
                        "platform" to "android"
                    ),
                    SetOptions.merge()
                )
        }
    }

    fun setPushEnabled(context: Context?, enabled: Boolean) {
        _pushEnabled.value = enabled
        if (!enabled) {
            _latestPushBanner.value = null
        } else if (context != null) {
            ensureInitialized(context)
        }
    }

    fun isCategorySubscribed(category: FcmNotificationCategory): Boolean {
        return _subscribedTopics.value.contains(category.topicName)
    }

    fun toggleTopicSubscription(
        context: Context?,
        category: FcmNotificationCategory,
        subscribed: Boolean? = null
    ) {
        val current = _subscribedTopics.value
        val shouldSubscribe = subscribed ?: !current.contains(category.topicName)
        val next = if (shouldSubscribe) {
            current + category.topicName
        } else {
            current - category.topicName
        }
        _subscribedTopics.value = next

        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            if (shouldSubscribe) {
                messaging.subscribeToTopic(category.topicName)
            } else {
                messaging.unsubscribeFromTopic(category.topicName)
            }
        }
        if (context != null) {
            onNewFcmToken(context, _fcmToken.value)
        }
    }

    /**
     * Handles an incoming [RemoteMessage] from [PicklePlayFirebaseMessagingService] or a constructed
     * FCM [RemoteMessage] and routes it into the app's notification state and OS status bar.
     */
    fun handleRemoteMessage(context: Context?, remoteMessage: RemoteMessage): FcmPushPayload {
        val data = remoteMessage.data
        val category = FcmNotificationCategory.fromRaw(
            data["category"] ?: data["type"] ?: remoteMessage.from
        )
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: when (category) {
                FcmNotificationCategory.UPCOMING_RESERVATION ->
                    "Upcoming Reservation: ${data["courtName"] ?: "Court 4"}"
                FcmNotificationCategory.TOURNAMENT_START ->
                    "Tournament Starting: ${data["tournamentName"] ?: "PicklePlay Open 2026"}"
                FcmNotificationCategory.MATCHMAKING_UPDATE ->
                    "Matchmaking Update: DUPR ${data["skillLevel"] ?: "3.5"} Queue"
            }

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "Tap to view your live PicklePlay update."

        val referenceCode = data["referenceCode"]
            ?: data["bookingCode"]
            ?: data["tournamentId"]?.let { "TOURNEY-$it" }
            ?: data["queueId"]?.uppercase()
            ?: "FCM-${System.currentTimeMillis() % 10000}"

        val subtitle = data["subtitle"]
            ?: listOfNotNull(
                data["facilityName"],
                data["courtName"] ?: data["activeCourt"] ?: data["courtAssignment"],
                data["timeSlot"] ?: data["activeRound"] ?: data["skillLevel"]?.let { "DUPR $it" }
            ).joinToString(" • ").ifBlank { category.displayTitle }

        val messageId = remoteMessage.messageId
            ?: data["messageId"]
            ?: "fcm-${category.name.lowercase()}-${System.currentTimeMillis()}"

        return deliverFcmPayload(
            context = context,
            category = category,
            title = title,
            body = body,
            referenceCode = referenceCode,
            subtitle = subtitle,
            fcmMessageId = messageId,
            dataPayload = data
        )
    }

    /**
     * Sends an FCM push notification for an **Upcoming Reservation** (e.g., 30-minute court reminder,
     * newly booked court confirmation, or QR check-in window opening).
     */
    fun sendUpcomingReservationPush(
        context: Context?,
        booking: BookingEntity? = null,
        reminderLeadText: String = "Starts in 30 mins"
    ): FcmPushPayload {
        val facilityName = booking?.facilityName ?: "Smash Pickle Club"
        val courtName = booking?.courtName ?: "Court 4"
        val dateLabel = booking?.dateLabel ?: "September 29, 2026"
        val timeRange = booking?.timeRangeLabel ?: "6:00 PM - 7:00 PM"
        val bookingCode = booking?.bookingCode ?: "PKL-20260929-00105"
        val playerName = booking?.playerName ?: "Jonel P."

        val title = "Upcoming Reservation: $courtName ($reminderLeadText)"
        val body = "Hi $playerName! Your court reservation #$bookingCode at $facilityName ($courtName • $dateLabel, $timeRange) is coming up. Tap to open your Digital QR Pass."
        val subtitle = "$facilityName • $courtName • $reminderLeadText"

        val dataMap = mapOf(
            "category" to FcmNotificationCategory.UPCOMING_RESERVATION.name,
            "title" to title,
            "body" to body,
            "bookingCode" to bookingCode,
            "referenceCode" to bookingCode,
            "facilityName" to facilityName,
            "courtName" to courtName,
            "dateLabel" to dateLabel,
            "timeSlot" to timeRange,
            "subtitle" to subtitle
        )

        val remoteMessage = RemoteMessage.Builder("${FcmNotificationCategory.UPCOMING_RESERVATION.topicName}@gcm.googleapis.com")
            .setMessageId("fcm-res-${System.currentTimeMillis()}")
            .setData(dataMap)
            .build()

        return handleRemoteMessage(context, remoteMessage)
    }

    /**
     * Sends an FCM push notification for a **Tournament Start** or bracket match call on an assigned court.
     */
    fun sendTournamentStartPush(
        context: Context?,
        tournament: TournamentEntity? = null,
        customStatusLabel: String? = null
    ): FcmPushPayload {
        val tournamentId = tournament?.id ?: 1
        val tournamentName = tournament?.name ?: "PicklePlay Open 2026"
        val facilityName = tournament?.facilityName ?: "Smash Pickle Club • Quezon City"
        val division = tournament?.division ?: "Doubles"
        val skillCap = tournament?.skillCap ?: "DUPR 3.5 – 4.0"
        val activeRound = customStatusLabel ?: tournament?.activeRound ?: "Quarter Finals"
        val activeCourt = tournament?.activeCourt ?: "Court 2"
        val teamA = tournament?.teamA ?: "Jonel / Carlo"
        val teamB = tournament?.teamB ?: "Mark / James"
        val refCode = "TOURNEY-$tournamentId"

        val title = "Tournament Starting: $tournamentName ($activeRound)"
        val body = "$tournamentName ($division • $skillCap) is starting now at $facilityName! Matchup: $teamA vs. $teamB on $activeCourt."
        val subtitle = "$activeCourt • $activeRound • $skillCap"

        val dataMap = mapOf(
            "category" to FcmNotificationCategory.TOURNAMENT_START.name,
            "title" to title,
            "body" to body,
            "tournamentId" to tournamentId.toString(),
            "tournamentName" to tournamentName,
            "referenceCode" to refCode,
            "facilityName" to facilityName,
            "activeCourt" to activeCourt,
            "activeRound" to activeRound,
            "subtitle" to subtitle
        )

        val remoteMessage = RemoteMessage.Builder("${FcmNotificationCategory.TOURNAMENT_START.topicName}@gcm.googleapis.com")
            .setMessageId("fcm-trn-${System.currentTimeMillis()}")
            .setData(dataMap)
            .build()

        return handleRemoteMessage(context, remoteMessage)
    }

    /**
     * Sends an FCM push notification for a real-time **Matchmaking Update** in an Open Play skill queue.
     */
    fun sendMatchmakingUpdatePush(
        context: Context?,
        queueDoc: OpenPlaySkillQueueDocument? = null,
        eventHeadline: String? = null
    ): FcmPushPayload {
        val skillLevel = queueDoc?.skillLevel ?: "3.5"
        val tierTitle = queueDoc?.tierTitle ?: "DUPR $skillLevel • Competitive Kitchen Queue"
        val courtAssignment = queueDoc?.courtAssignment ?: "Court 3"
        val facilityName = queueDoc?.facilityName ?: "Smash Pickle Club • Quezon City"
        val filledSlots = queueDoc?.filledSlots ?: 4
        val maxPlayers = queueDoc?.maxPlayersPerPod ?: 4
        val isReady = filledSlots >= maxPlayers
        val playerNames = queueDoc?.queuedPlayers?.joinToString(", ") { it.playerName }
            ?.ifBlank { "Jonel P., Maria L., Carlo V., Jon D." }
            ?: "Jonel P., Maria L., Carlo V., Jon D."
        val refCode = "QUEUE-${skillLevel.uppercase()}"

        val statusSummary = eventHeadline ?: if (isReady) {
            "Match Ready (4/4)! Assigned to $courtAssignment"
        } else {
            "$filledSlots/$maxPlayers Players Queued on $courtAssignment"
        }

        val title = "Matchmaking Update (DUPR $skillLevel): $statusSummary"
        val body = "$tierTitle at $facilityName — $statusSummary. Players in pod: $playerNames."
        val subtitle = "DUPR $skillLevel • $courtAssignment • $filledSlots/$maxPlayers Players"

        val dataMap = mapOf(
            "category" to FcmNotificationCategory.MATCHMAKING_UPDATE.name,
            "title" to title,
            "body" to body,
            "queueId" to (queueDoc?.queueId ?: "queue_3_5"),
            "skillLevel" to skillLevel,
            "referenceCode" to refCode,
            "facilityName" to facilityName,
            "courtAssignment" to courtAssignment,
            "subtitle" to subtitle
        )

        val remoteMessage = RemoteMessage.Builder("${FcmNotificationCategory.MATCHMAKING_UPDATE.topicName}@gcm.googleapis.com")
            .setMessageId("fcm-mmk-${System.currentTimeMillis()}")
            .setData(dataMap)
            .build()

        return handleRemoteMessage(context, remoteMessage)
    }

    private fun deliverFcmPayload(
        context: Context?,
        category: FcmNotificationCategory,
        title: String,
        body: String,
        referenceCode: String,
        subtitle: String,
        fcmMessageId: String,
        dataPayload: Map<String, String>
    ): FcmPushPayload {
        val id = nextNotificationId++
        val payload = FcmPushPayload(
            notificationId = id,
            fcmMessageId = fcmMessageId,
            category = category,
            topic = category.topicName,
            title = title,
            body = body,
            referenceCode = referenceCode,
            subtitle = subtitle,
            dataPayload = dataPayload,
            timestampLabel = "Just now",
            isRead = false
        )

        _receivedMessages.value = listOf(payload) + _receivedMessages.value
            .filterNot { it.fcmMessageId == fcmMessageId }
            .take(24)

        if (_pushEnabled.value && _subscribedTopics.value.contains(category.topicName)) {
            _latestPushBanner.value = payload
            if (context != null) {
                postSystemFcmNotification(context, payload)
            }
        }

        // Persist to Firestore 'fcm_push_notifications' collection for real-time multi-device sync
        runCatching {
            val firestore = FirestoreReservationRepository.ensureFirestoreInstance(context)
            firestore?.collection(COLLECTION_FCM_NOTIFICATIONS)
                ?.document(fcmMessageId)
                ?.set(
                    mapOf(
                        "notificationId" to payload.notificationId,
                        "fcmMessageId" to payload.fcmMessageId,
                        "category" to payload.category.name,
                        "topic" to payload.topic,
                        "title" to payload.title,
                        "body" to payload.body,
                        "referenceCode" to payload.referenceCode,
                        "subtitle" to payload.subtitle,
                        "createdAtMillis" to payload.createdAtMillis
                    ),
                    SetOptions.merge()
                )
        }

        return payload
    }

    private fun attachFirestorePushListener(context: Context?) {
        if (firestoreListener != null) return
        val firestore = FirestoreReservationRepository.ensureFirestoreInstance(context) ?: return
        runCatching {
            firestoreListener = firestore
                .collection(COLLECTION_FCM_NOTIFICATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    snapshot.documents.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val msgId = (data["fcmMessageId"] as? String) ?: doc.id
                        if (_receivedMessages.value.none { it.fcmMessageId == msgId }) {
                            val cat = FcmNotificationCategory.fromRaw(data["category"] as? String)
                            val item = FcmPushPayload(
                                notificationId = (data["notificationId"] as? Number)?.toInt() ?: nextNotificationId++,
                                fcmMessageId = msgId,
                                category = cat,
                                topic = (data["topic"] as? String) ?: cat.topicName,
                                title = (data["title"] as? String) ?: cat.displayTitle,
                                body = (data["body"] as? String) ?: "",
                                referenceCode = (data["referenceCode"] as? String) ?: "FCM",
                                subtitle = (data["subtitle"] as? String) ?: cat.displayTitle,
                                timestampLabel = "Cloud Sync"
                            )
                            _receivedMessages.value = listOf(item) + _receivedMessages.value.take(24)
                        }
                    }
                }
        }
    }

    private fun postSystemFcmNotification(context: Context, payload: FcmPushPayload) {
        try {
            ensureNotificationChannels(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("fcm_message_id", payload.fcmMessageId)
                putExtra("fcm_category", payload.category.name)
                putExtra("fcm_reference_code", payload.referenceCode)
                putExtra("fcm_target_section", payload.targetSectionKey)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                payload.notificationId,
                launchIntent,
                pendingIntentFlags
            )

            val notification = NotificationCompat.Builder(context, payload.category.channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setSubText("FCM • ${payload.category.badgeLabel}")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(payload.body)
                        .setBigContentTitle(payload.title)
                        .setSummaryText(payload.subtitle)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(payload.notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ runtime permission not granted; in-app FCM banner still shown
        } catch (_: Throwable) {
            // Safe fallback on any platform notification edge cases
        }
    }

    fun dismissLatestBanner() {
        _latestPushBanner.value = null
    }

    fun markAllRead() {
        _receivedMessages.value = _receivedMessages.value.map { it.copy(isRead = true) }
    }

    fun resetForTesting(context: Context? = null) {
        _pushEnabled.value = true
        _subscribedTopics.value = setOf(
            FcmNotificationCategory.UPCOMING_RESERVATION.topicName,
            FcmNotificationCategory.TOURNAMENT_START.topicName,
            FcmNotificationCategory.MATCHMAKING_UPDATE.topicName
        )
        _receivedMessages.value = defaultSeededMessages
        _latestPushBanner.value = null
        if (context != null) {
            ensureNotificationChannels(context)
        }
    }
}

@Composable
fun fcmCategoryColors(category: FcmNotificationCategory): Pair<Color, Color> {
    return when (category) {
        FcmNotificationCategory.UPCOMING_RESERVATION -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        FcmNotificationCategory.TOURNAMENT_START -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        FcmNotificationCategory.MATCHMAKING_UPDATE -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
    }
}

/**
 * Floating Heads-Up Banner displayed when a new FCM push notification arrives for
 * an upcoming reservation, tournament start, or matchmaking update.
 */
@Composable
fun FcmPushHeadsUpBanner(
    payload: FcmPushPayload,
    onOpenTarget: (FcmPushPayload) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeFg) = fcmCategoryColors(payload.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fcm_push_heads_up_banner"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF062E22)),
        border = BorderStroke(1.5.dp, OpticVolt),
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
                        color = OpticVolt,
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "FCM Push Alert",
                                tint = OpticVoltDarkText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "FCM PUSH • ${payload.category.badgeLabel}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpticVolt,
                        modifier = Modifier.testTag("fcm_banner_category_label")
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("fcm_banner_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss FCM Banner",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "TOPIC: /topics/${payload.topic}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = payload.referenceCode,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.testTag("fcm_banner_reference_code")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = payload.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.testTag("fcm_banner_title")
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = payload.body,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFD6F5E6),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("fcm_banner_body")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payload.subtitle,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = Color(0xFF99F6E4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onOpenTarget(payload)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OpticVolt,
                        contentColor = OpticVoltDarkText
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("fcm_banner_action_button")
                ) {
                    Text(
                        text = payload.actionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Interactive Firebase Cloud Messaging (FCM) Push Notification Center Card.
 * Allows users to manage topic subscriptions and receive/trigger push notifications for:
 * 1. Upcoming Reservations (`UPCOMING_RESERVATION`)
 * 2. Tournament Starts (`TOURNAMENT_START`)
 * 3. Matchmaking Updates (`MATCHMAKING_UPDATE`)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FcmPushNotificationCenterCard(
    modifier: Modifier = Modifier,
    bookings: List<BookingEntity> = emptyList(),
    tournaments: List<TournamentEntity> = emptyList(),
    queues: List<OpenPlaySkillQueueDocument> = emptyList(),
    initialCategoryFilter: String = "ALL",
    onNavigateToTarget: (FcmPushPayload) -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FcmPushNotificationManager.ensureInitialized(context)
    }

    val pushEnabled by FcmPushNotificationManager.pushEnabled.collectAsState()
    val fcmToken by FcmPushNotificationManager.fcmToken.collectAsState()
    val subscribedTopics by FcmPushNotificationManager.subscribedTopics.collectAsState()
    val messages by FcmPushNotificationManager.receivedMessages.collectAsState()
    val latestBanner by FcmPushNotificationManager.latestPushBanner.collectAsState()

    var selectedCategoryFilter by remember(initialCategoryFilter) {
        mutableStateOf(initialCategoryFilter)
    }

    var hasOsPermission by remember {
        mutableStateOf(LocalReservationNotificationService.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasOsPermission = granted
        FcmPushNotificationManager.setPushEnabled(context, granted)
    }

    val filteredMessages = remember(messages, selectedCategoryFilter) {
        if (selectedCategoryFilter.equals("ALL", ignoreCase = true)) {
            messages
        } else {
            messages.filter { it.category.name.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    val nextUpcomingBooking = remember(bookings) {
        bookings.firstOrNull { !it.status.equals("CANCELLED", ignoreCase = true) }
    }
    val nextTournament = remember(tournaments) {
        tournaments.firstOrNull { it.isJoinedByUser } ?: tournaments.firstOrNull()
    }
    val activeQueue = remember(queues) {
        queues.firstOrNull { it.isCurrentUserJoined } ?: queues.firstOrNull()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fcm_push_center_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header Banner with FCM Cloud Status & Master Switch
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
                                    .testTag("fcm_status_badge"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Firebase Cloud Messaging Active",
                                    tint = OpticVoltDarkText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (pushEnabled) {
                                        "FIREBASE CLOUD MESSAGING (FCM) • ACTIVE"
                                    } else {
                                        "FIREBASE CLOUD MESSAGING (FCM) • PAUSED"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpticVoltDarkText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "FCM Push Notifications Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.testTag("fcm_center_title")
                        )
                        Text(
                            text = "Real-time push alerts for Upcoming Reservations, Tournament Starts & Matchmaking Updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD6F5E6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "FCM Token: ${fcmToken.take(24)}... • ${subscribedTopics.size}/3 Topics Subscribed",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFFA7F3D0),
                            modifier = Modifier.testTag("fcm_device_token_text")
                        )
                    }

                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasOsPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                FcmPushNotificationManager.setPushEnabled(context, enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OpticVoltDarkText,
                            checkedTrackColor = OpticVolt
                        ),
                        modifier = Modifier.testTag("fcm_push_master_switch")
                    )
                }
            }

            // 2. Inline Heads-Up Banner if a new FCM Push was just received
            latestBanner?.let { activePush ->
                FcmPushHeadsUpBanner(
                    payload = activePush,
                    onOpenTarget = onNavigateToTarget,
                    onDismiss = { FcmPushNotificationManager.dismissLatestBanner() }
                )
            }

            // 3. FCM Topic Subscriptions Row
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SUBSCRIBED FCM PUSH TOPICS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FcmNotificationCategory.entries.forEach { category ->
                        val isSubscribed = subscribedTopics.contains(category.topicName)
                        FilterChip(
                            selected = isSubscribed,
                            onClick = {
                                FcmPushNotificationManager.toggleTopicSubscription(context, category)
                            },
                            label = {
                                Text(
                                    text = "${category.displayTitle} (${if (isSubscribed) "ON" else "OFF"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (category) {
                                        FcmNotificationCategory.UPCOMING_RESERVATION -> Icons.Default.CalendarMonth
                                        FcmNotificationCategory.TOURNAMENT_START -> Icons.Default.EmojiEvents
                                        FcmNotificationCategory.MATCHMAKING_UPDATE -> Icons.Default.Groups
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDCFCE7),
                                selectedLabelColor = Color(0xFF065F46),
                                selectedLeadingIconColor = Color(0xFF047857)
                            ),
                            modifier = Modifier.testTag("fcm_topic_chip_${category.name}")
                        )
                    }
                }
            }

            // 4. Instant FCM Push Dispatch Actions (Reservations, Tournaments, Matchmaking)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DISPATCH REAL-TIME FCM PUSH NOTIFICATION",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            FcmPushNotificationManager.sendUpcomingReservationPush(
                                context = context,
                                booking = nextUpcomingBooking
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF047857),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("fcm_send_reservation_push_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Push Upcoming Reservation",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            FcmPushNotificationManager.sendTournamentStartPush(
                                context = context,
                                tournament = nextTournament
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB45309),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("fcm_send_tournament_push_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Push Tournament Start",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val currentQueues = queues.ifEmpty {
                                FirestoreTournamentsAndMatchmakingRepository.queuesFlow.value
                            }
                            val targetQueue = activeQueue ?: currentQueues.firstOrNull()
                            FcmPushNotificationManager.sendMatchmakingUpdatePush(
                                context = context,
                                queueDoc = targetQueue
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("fcm_send_matchmaking_push_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Push Matchmaking Update",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 5. Category Filter Row & Recent FCM Push Feed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT FCM PUSH NOTIFICATIONS (${filteredMessages.size})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    modifier = Modifier.testTag("fcm_recent_messages_header")
                )
                Text(
                    text = "Mark All Read",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GCashBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { FcmPushNotificationManager.markAllRead() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("fcm_mark_all_read_button")
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterOptions = listOf(
                    "ALL" to "All (${messages.size})",
                    FcmNotificationCategory.UPCOMING_RESERVATION.name to "Reservations",
                    FcmNotificationCategory.TOURNAMENT_START.name to "Tournaments",
                    FcmNotificationCategory.MATCHMAKING_UPDATE.name to "Matchmaking"
                )
                filterOptions.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedCategoryFilter.equals(key, ignoreCase = true),
                        onClick = { selectedCategoryFilter = key },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("fcm_filter_chip_$key")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredMessages.take(5).forEachIndexed { index, msg ->
                    val (catBg, catFg) = fcmCategoryColors(msg.category)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fcm_message_item_$index"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (!msg.isRead) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (!msg.isRead) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = catBg,
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = msg.category.badgeLabel,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = catFg,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                            .testTag("fcm_message_category_$index")
                                    )
                                }
                                Text(
                                    text = "${msg.referenceCode} • ${msg.timestampLabel}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.testTag("fcm_message_reference_$index")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = msg.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.testTag("fcm_message_title_$index")
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = msg.body,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.testTag("fcm_message_body_$index")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "FCM Topic: /topics/${msg.topic} • ${msg.subtitle}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        }
    }
}
