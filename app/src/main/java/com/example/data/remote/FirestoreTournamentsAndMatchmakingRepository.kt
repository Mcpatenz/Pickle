package com.example.data.remote

import android.content.Context
import com.example.data.local.PicklePlayDatabase
import com.example.data.local.TournamentEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a tournament registration record persisted to the `'tournaments'` Firestore collection.
 */
data class TournamentRegistrationRecord(
    val tournamentId: Int,
    val tournamentName: String,
    val playerName: String,
    val partnerName: String,
    val division: String,
    val skillCap: String,
    val registeredTeamLabel: String,
    val registeredAtMillis: Long = System.currentTimeMillis()
)

/**
 * Represents a player currently waiting in a real-time Open Play skill-level matchmaking queue.
 */
data class QueuedPlayerEntry(
    val playerName: String,
    val skillRating: String,
    val joinedAtLabel: String = "Just now",
    val isCurrentUser: Boolean = false
)

/**
 * Represents a real-time skill-level matchmaking queue document in Firestore (`open_play_queues` collection).
 */
data class OpenPlaySkillQueueDocument(
    val queueId: String,
    val skillLevel: String, // e.g., "3.0", "3.5", "4.0", "All Levels"
    val tierTitle: String,  // e.g., "Beginner / Intermediate (3.0)"
    val facilityName: String,
    val courtAssignment: String,
    val sessionWindow: String,
    val maxPlayersPerPod: Int = 4,
    val queuedPlayers: List<QueuedPlayerEntry>,
    val isCurrentUserJoined: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val filledSlots: Int
        get() = queuedPlayers.size.coerceAtMost(maxPlayersPerPod)

    val openSlots: Int
        get() = (maxPlayersPerPod - filledSlots).coerceAtLeast(0)

    val isPodReady: Boolean
        get() = filledSlots >= maxPlayersPerPod

    val statusHeadline: String
        get() = if (isPodReady) {
            "MATCH READY (4/4) • Assigned to $courtAssignment"
        } else {
            "$filledSlots/$maxPlayersPerPod in Queue • Waiting for $openSlots more player${if (openSlots == 1) "" else "s"}"
        }
}

/**
 * Repository managing:
 * 1. Upcoming tournaments and user registrations in the `'tournaments'` Firestore collection.
 * 2. Real-time `'Open Play'` skill-level matchmaking queues in the `'open_play_queues'` Firestore collection
 *    with live snapshot listeners.
 */
object FirestoreTournamentsAndMatchmakingRepository {

    const val COLLECTION_TOURNAMENTS = "tournaments"
    const val COLLECTION_OPEN_PLAY_QUEUES = "open_play_queues"

    private const val PREFS_NAME = "pickleplay_firestore_tournaments_matchmaking_prefs"
    private const val KEY_TOURNAMENTS_JSON = "firestore_tournaments_json"
    private const val KEY_QUEUES_JSON = "firestore_open_play_queues_json"

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tournamentsListener: ListenerRegistration? = null
    private var queuesListener: ListenerRegistration? = null

    val defaultSeededUpcomingTournaments: List<TournamentEntity> = listOf(
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
            registeredCount = 14,
            maxTeams = 16,
            isJoinedByUser = false,
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
            dateRange = "Oct 18, 2026",
            division = "Singles",
            format = "League",
            skillCap = "DUPR 3.0 – 4.5",
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
    )

    val defaultSeededSkillQueues: List<OpenPlaySkillQueueDocument> = listOf(
        OpenPlaySkillQueueDocument(
            queueId = "queue_3_0",
            skillLevel = "3.0",
            tierTitle = "DUPR 3.0 • Intermediate Rally Queue",
            facilityName = "Smash Pickle Club • Quezon City",
            courtAssignment = "Court 1 & Court 2",
            sessionWindow = "Live Now • Next Pod Starts in 5m",
            maxPlayersPerPod = 4,
            queuedPlayers = listOf(
                QueuedPlayerEntry("Sofia R.", "3.0", "2m ago", isCurrentUser = false),
                QueuedPlayerEntry("Miggy T.", "3.1", "1m ago", isCurrentUser = false)
            ),
            isCurrentUserJoined = false
        ),
        OpenPlaySkillQueueDocument(
            queueId = "queue_3_5",
            skillLevel = "3.5",
            tierTitle = "DUPR 3.5 • Competitive Kitchen Queue",
            facilityName = "Smash Pickle Club • Quezon City",
            courtAssignment = "Court 3",
            sessionWindow = "Live Now • High-Pace Rotation",
            maxPlayersPerPod = 4,
            queuedPlayers = listOf(
                QueuedPlayerEntry("Maria L.", "3.5", "4m ago", isCurrentUser = false),
                QueuedPlayerEntry("Carlo V.", "3.6", "3m ago", isCurrentUser = false),
                QueuedPlayerEntry("Jon D.", "3.5", "1m ago", isCurrentUser = false)
            ),
            isCurrentUserJoined = false
        ),
        OpenPlaySkillQueueDocument(
            queueId = "queue_4_0",
            skillLevel = "4.0",
            tierTitle = "DUPR 4.0+ • Advanced Premier Queue",
            facilityName = "BGC Dink & Rally Club • Taguig",
            courtAssignment = "Center Court 1",
            sessionWindow = "Live Now • Pro-Level Doubles",
            maxPlayersPerPod = 4,
            queuedPlayers = listOf(
                QueuedPlayerEntry("Mark S.", "4.1", "5m ago", isCurrentUser = false),
                QueuedPlayerEntry("James K.", "4.0", "2m ago", isCurrentUser = false)
            ),
            isCurrentUserJoined = false
        ),
        OpenPlaySkillQueueDocument(
            queueId = "queue_all",
            skillLevel = "All Levels",
            tierTitle = "All Levels (2.5 – 4.5) • Social Open Queue",
            facilityName = "Palms Rooftop Pickleball • Pasig",
            courtAssignment = "Court 4",
            sessionWindow = "Live Now • Social Mixer",
            maxPlayersPerPod = 4,
            queuedPlayers = listOf(
                QueuedPlayerEntry("Aya M.", "2.8", "3m ago", isCurrentUser = false)
            ),
            isCurrentUserJoined = false
        )
    )

    private val _tournamentsFlow = MutableStateFlow<List<TournamentEntity>>(defaultSeededUpcomingTournaments)
    val tournamentsFlow: StateFlow<List<TournamentEntity>> = _tournamentsFlow.asStateFlow()

    private val _lastRegistrationRecord = MutableStateFlow<TournamentRegistrationRecord?>(null)
    val lastRegistrationRecord: StateFlow<TournamentRegistrationRecord?> = _lastRegistrationRecord.asStateFlow()

    private val _queuesFlow = MutableStateFlow<List<OpenPlaySkillQueueDocument>>(defaultSeededSkillQueues)
    val queuesFlow: StateFlow<List<OpenPlaySkillQueueDocument>> = _queuesFlow.asStateFlow()

    private val _lastRealtimeQueueEvent = MutableStateFlow(
        "Firestore real-time listener active on '$COLLECTION_OPEN_PLAY_QUEUES' & '$COLLECTION_TOURNAMENTS'"
    )
    val lastRealtimeQueueEvent: StateFlow<String> = _lastRealtimeQueueEvent.asStateFlow()

    /**
     * Initializes Firebase Firestore listeners for both the `'tournaments'` collection
     * and the `'open_play_queues'` collection.
     */
    fun ensureInitialized(context: Context? = null): FirebaseFirestore? {
        val firestore = FirestoreReservationRepository.ensureFirestoreInstance(context)
        attachListenersIfNeeded(firestore)
        return firestore
    }

    private fun attachListenersIfNeeded(firestore: FirebaseFirestore?) {
        if (firestore == null) return
        if (tournamentsListener == null) {
            runCatching {
                tournamentsListener = firestore
                    .collection(COLLECTION_TOURNAMENTS)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val remoteTournaments = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            tournamentFromDocumentMap(data, doc.id)
                        }
                        if (remoteTournaments.isNotEmpty()) {
                            mergeRemoteTournaments(remoteTournaments)
                        }
                    }
            }
        }
        if (queuesListener == null) {
            runCatching {
                queuesListener = firestore
                    .collection(COLLECTION_OPEN_PLAY_QUEUES)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val remoteQueues = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            queueFromDocumentMap(data, doc.id)
                        }
                        if (remoteQueues.isNotEmpty()) {
                            mergeRemoteQueues(remoteQueues)
                        }
                    }
            }
        }
    }

    fun toTournamentFirestoreMap(tournament: TournamentEntity): Map<String, Any?> {
        return mapOf(
            "id" to tournament.id,
            "name" to tournament.name,
            "facilityName" to tournament.facilityName,
            "dateRange" to tournament.dateRange,
            "division" to tournament.division,
            "format" to tournament.format,
            "skillCap" to tournament.skillCap,
            "entryFee" to tournament.entryFee,
            "prizePool" to tournament.prizePool,
            "registeredCount" to tournament.registeredCount,
            "maxTeams" to tournament.maxTeams,
            "isJoinedByUser" to tournament.isJoinedByUser,
            "activeRound" to tournament.activeRound,
            "activeCourt" to tournament.activeCourt,
            "teamA" to tournament.teamA,
            "teamB" to tournament.teamB,
            "set1A" to tournament.set1A,
            "set1B" to tournament.set1B,
            "set2A" to tournament.set2A,
            "set2B" to tournament.set2B,
            "set3A" to tournament.set3A,
            "set3B" to tournament.set3B,
            "matchSubmitted" to tournament.matchSubmitted,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun tournamentFromDocumentMap(map: Map<String, Any?>, docId: String = ""): TournamentEntity? {
        val id = (map["id"] as? Number)?.toInt() ?: docId.toIntOrNull() ?: return null
        val name = (map["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return TournamentEntity(
            id = id,
            name = name,
            facilityName = (map["facilityName"] as? String) ?: "Smash Pickle Club • Quezon City",
            dateRange = (map["dateRange"] as? String) ?: "Upcoming",
            division = (map["division"] as? String) ?: "Doubles",
            format = (map["format"] as? String) ?: "Knockout",
            skillCap = (map["skillCap"] as? String) ?: "DUPR 3.5 – 4.0",
            entryFee = (map["entryFee"] as? Number)?.toInt() ?: 750,
            prizePool = (map["prizePool"] as? Number)?.toInt() ?: 30000,
            registeredCount = (map["registeredCount"] as? Number)?.toInt() ?: 8,
            maxTeams = (map["maxTeams"] as? Number)?.toInt() ?: 16,
            isJoinedByUser = (map["isJoinedByUser"] as? Boolean) ?: false,
            activeRound = (map["activeRound"] as? String) ?: "Round 1",
            activeCourt = (map["activeCourt"] as? String) ?: "Court 1",
            teamA = (map["teamA"] as? String) ?: "Jonel / Partner",
            teamB = (map["teamB"] as? String) ?: "Challenger Team",
            set1A = (map["set1A"] as? Number)?.toInt() ?: 0,
            set1B = (map["set1B"] as? Number)?.toInt() ?: 0,
            set2A = (map["set2A"] as? Number)?.toInt() ?: 0,
            set2B = (map["set2B"] as? Number)?.toInt() ?: 0,
            set3A = (map["set3A"] as? Number)?.toInt() ?: 0,
            set3B = (map["set3B"] as? Number)?.toInt() ?: 0,
            matchSubmitted = (map["matchSubmitted"] as? Boolean) ?: false
        )
    }

    fun toQueueFirestoreMap(queue: OpenPlaySkillQueueDocument): Map<String, Any?> {
        return mapOf(
            "queueId" to queue.queueId,
            "skillLevel" to queue.skillLevel,
            "tierTitle" to queue.tierTitle,
            "facilityName" to queue.facilityName,
            "courtAssignment" to queue.courtAssignment,
            "sessionWindow" to queue.sessionWindow,
            "maxPlayersPerPod" to queue.maxPlayersPerPod,
            "isCurrentUserJoined" to queue.isCurrentUserJoined,
            "queuedPlayers" to queue.queuedPlayers.map { player ->
                mapOf(
                    "playerName" to player.playerName,
                    "skillRating" to player.skillRating,
                    "joinedAtLabel" to player.joinedAtLabel,
                    "isCurrentUser" to player.isCurrentUser
                )
            },
            "updatedAtMillis" to queue.updatedAtMillis
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun queueFromDocumentMap(map: Map<String, Any?>, docId: String = ""): OpenPlaySkillQueueDocument? {
        val qId = (map["queueId"] as? String)?.ifBlank { docId } ?: docId.ifBlank { return null }
        val skillLevel = (map["skillLevel"] as? String) ?: "3.5"
        val rawPlayers = (map["queuedPlayers"] as? List<Map<String, Any?>>).orEmpty()
        val parsedPlayers = rawPlayers.mapNotNull { p ->
            val pName = (p["playerName"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            QueuedPlayerEntry(
                playerName = pName,
                skillRating = (p["skillRating"] as? String) ?: skillLevel,
                joinedAtLabel = (p["joinedAtLabel"] as? String) ?: "Just now",
                isCurrentUser = (p["isCurrentUser"] as? Boolean) ?: false
            )
        }
        return OpenPlaySkillQueueDocument(
            queueId = qId,
            skillLevel = skillLevel,
            tierTitle = (map["tierTitle"] as? String) ?: "DUPR $skillLevel Queue",
            facilityName = (map["facilityName"] as? String) ?: "Smash Pickle Club",
            courtAssignment = (map["courtAssignment"] as? String) ?: "Court 1",
            sessionWindow = (map["sessionWindow"] as? String) ?: "Live Now",
            maxPlayersPerPod = (map["maxPlayersPerPod"] as? Number)?.toInt() ?: 4,
            queuedPlayers = parsedPlayers,
            isCurrentUserJoined = (map["isCurrentUserJoined"] as? Boolean) ?: parsedPlayers.any { it.isCurrentUser },
            updatedAtMillis = (map["updatedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    /**
     * Syncs a list of [TournamentEntity] objects into the `'tournaments'` Firestore collection.
     */
    fun syncTournamentsToFirestore(context: Context?, tournaments: List<TournamentEntity>) {
        if (tournaments.isEmpty()) return
        val firestore = ensureInitialized(context)
        runCatching {
            tournaments.forEach { t ->
                firestore?.collection(COLLECTION_TOURNAMENTS)
                    ?.document(t.id.toString())
                    ?.set(toTournamentFirestoreMap(t), SetOptions.merge())
            }
        }
        _tournamentsFlow.value = tournaments
        if (context != null) {
            saveTournamentsToLocalPrefs(context, tournaments)
        }
    }

    /**
     * Registers a user for an upcoming tournament in the `'tournaments'` Firestore collection.
     */
    fun registerForTournamentInFirestore(
        context: Context?,
        tournament: TournamentEntity,
        playerName: String = "Jonel P.",
        partnerName: String = "Carlo V."
    ): TournamentEntity {
        val cleanPlayer = playerName.trim().ifBlank { "Jonel P." }
        val cleanPartner = partnerName.trim().ifBlank { "Carlo V." }
        val teamLabel = if (tournament.division.equals("Singles", ignoreCase = true)) {
            cleanPlayer
        } else {
            "$cleanPlayer / $cleanPartner"
        }

        val updatedRegisteredCount = if (tournament.isJoinedByUser) {
            tournament.registeredCount
        } else {
            (tournament.registeredCount + 1).coerceAtMost(tournament.maxTeams)
        }

        val updatedTournament = tournament.copy(
            isJoinedByUser = true,
            registeredCount = updatedRegisteredCount,
            teamA = teamLabel
        )

        val currentList = _tournamentsFlow.value
        val nextList = if (currentList.any { it.id == updatedTournament.id }) {
            currentList.map { if (it.id == updatedTournament.id) updatedTournament else it }
        } else {
            currentList + updatedTournament
        }
        _tournamentsFlow.value = nextList

        val record = TournamentRegistrationRecord(
            tournamentId = updatedTournament.id,
            tournamentName = updatedTournament.name,
            playerName = cleanPlayer,
            partnerName = cleanPartner,
            division = updatedTournament.division,
            skillCap = updatedTournament.skillCap,
            registeredTeamLabel = teamLabel
        )
        _lastRegistrationRecord.value = record
        _lastRealtimeQueueEvent.value =
            "Registered $teamLabel in '${updatedTournament.name}' (Firestore '$COLLECTION_TOURNAMENTS/${updatedTournament.id}')"

        val firestore = ensureInitialized(context)
        val docData = toTournamentFirestoreMap(updatedTournament) + mapOf(
            "lastRegisteredPlayer" to cleanPlayer,
            "lastRegisteredPartner" to cleanPartner,
            "lastRegisteredTeam" to teamLabel,
            "registeredAtMillis" to record.registeredAtMillis
        )
        runCatching {
            firestore?.collection(COLLECTION_TOURNAMENTS)
                ?.document(updatedTournament.id.toString())
                ?.set(docData, SetOptions.merge())
        }

        if (context != null) {
            saveTournamentsToLocalPrefs(context, nextList)
            repositoryScope.launch {
                runCatching {
                    val dao = PicklePlayDatabase.getDatabase(context).picklePlayDao()
                    dao.updateTournament(updatedTournament)
                }
            }
        }

        com.example.notifications.FcmPushNotificationManager.sendTournamentStartPush(
            context = context,
            tournament = updatedTournament,
            customStatusLabel = "${updatedTournament.activeRound} • Court ${updatedTournament.activeCourt.removePrefix("Court ").trim()}"
        )

        return updatedTournament
    }

    /**
     * Joins the real-time Open Play matchmaking queue for the specified skill level or queue ID,
     * persisting the update to the `'open_play_queues'` Firestore collection.
     */
    fun joinQueueForSkillLevel(
        context: Context?,
        queueIdOrSkillLevel: String,
        playerName: String = "Jonel P.",
        skillRating: String = "3.5"
    ): OpenPlaySkillQueueDocument? {
        val cleanName = playerName.trim().ifBlank { "Jonel P." }
        val targetQueue = findQueue(queueIdOrSkillLevel) ?: return null

        val existingWithoutUser = targetQueue.queuedPlayers.filterNot {
            it.isCurrentUser || it.playerName.equals(cleanName, ignoreCase = true)
        }
        val effectiveRating = skillRating.trim().ifBlank { targetQueue.skillLevel }
        val updatedPlayers = (existingWithoutUser + QueuedPlayerEntry(
            playerName = cleanName,
            skillRating = effectiveRating,
            joinedAtLabel = "Just now",
            isCurrentUser = true
        )).take(targetQueue.maxPlayersPerPod)

        val updatedQueue = targetQueue.copy(
            queuedPlayers = updatedPlayers,
            isCurrentUserJoined = true,
            updatedAtMillis = System.currentTimeMillis()
        )

        publishUpdatedQueue(context, updatedQueue)
        _lastRealtimeQueueEvent.value =
            "Joined ${updatedQueue.tierTitle} as $cleanName (DUPR $effectiveRating) • ${updatedQueue.statusHeadline}"
        com.example.notifications.FcmPushNotificationManager.sendMatchmakingUpdatePush(
            context = context,
            queueDoc = updatedQueue
        )
        return updatedQueue
    }

    /**
     * Leaves the real-time Open Play matchmaking queue for the specified skill level or queue ID,
     * persisting the update to the `'open_play_queues'` Firestore collection.
     */
    fun leaveQueueForSkillLevel(
        context: Context?,
        queueIdOrSkillLevel: String,
        playerName: String = "Jonel P."
    ): OpenPlaySkillQueueDocument? {
        val cleanName = playerName.trim().ifBlank { "Jonel P." }
        val targetQueue = findQueue(queueIdOrSkillLevel) ?: return null

        val updatedPlayers = targetQueue.queuedPlayers.filterNot {
            it.isCurrentUser || it.playerName.equals(cleanName, ignoreCase = true)
        }
        val updatedQueue = targetQueue.copy(
            queuedPlayers = updatedPlayers,
            isCurrentUserJoined = false,
            updatedAtMillis = System.currentTimeMillis()
        )

        publishUpdatedQueue(context, updatedQueue)
        _lastRealtimeQueueEvent.value =
            "Left ${updatedQueue.tierTitle} • ${updatedQueue.filledSlots}/${updatedQueue.maxPlayersPerPod} players in queue"
        return updatedQueue
    }

    /**
     * Simulates a real-time Firestore snapshot update when another peer player joins the skill queue.
     */
    fun simulateIncomingRealtimePeerJoin(
        context: Context?,
        queueIdOrSkillLevel: String,
        peerName: String = "Coach Marco",
        peerSkillRating: String? = null
    ): OpenPlaySkillQueueDocument? {
        val targetQueue = findQueue(queueIdOrSkillLevel) ?: return null
        if (targetQueue.queuedPlayers.size >= targetQueue.maxPlayersPerPod) {
            return targetQueue
        }
        val rating = peerSkillRating ?: targetQueue.skillLevel
        val uniquePeerName = if (targetQueue.queuedPlayers.any { it.playerName.equals(peerName, ignoreCase = true) }) {
            "Alex P."
        } else {
            peerName
        }
        val updatedPlayers = targetQueue.queuedPlayers + QueuedPlayerEntry(
            playerName = uniquePeerName,
            skillRating = rating,
            joinedAtLabel = "Live • Now",
            isCurrentUser = false
        )
        val updatedQueue = targetQueue.copy(
            queuedPlayers = updatedPlayers,
            updatedAtMillis = System.currentTimeMillis()
        )
        publishUpdatedQueue(context, updatedQueue)
        _lastRealtimeQueueEvent.value =
            "LIVE FIRESTORE UPDATE: $uniquePeerName (DUPR $rating) joined ${updatedQueue.tierTitle}! (${updatedQueue.filledSlots}/${updatedQueue.maxPlayersPerPod})"
        com.example.notifications.FcmPushNotificationManager.sendMatchmakingUpdatePush(
            context = context,
            queueDoc = updatedQueue
        )
        return updatedQueue
    }

    private fun findQueue(queueIdOrSkillLevel: String): OpenPlaySkillQueueDocument? {
        val key = queueIdOrSkillLevel.trim()
        return _queuesFlow.value.find {
            it.queueId.equals(key, ignoreCase = true) ||
                it.skillLevel.equals(key, ignoreCase = true) ||
                it.tierTitle.contains(key, ignoreCase = true)
        }
    }

    private fun publishUpdatedQueue(context: Context?, updatedQueue: OpenPlaySkillQueueDocument) {
        val nextQueues = _queuesFlow.value.map {
            if (it.queueId == updatedQueue.queueId) updatedQueue else it
        }
        _queuesFlow.value = nextQueues

        val firestore = ensureInitialized(context)
        runCatching {
            firestore?.collection(COLLECTION_OPEN_PLAY_QUEUES)
                ?.document(updatedQueue.queueId)
                ?.set(toQueueFirestoreMap(updatedQueue), SetOptions.merge())
        }
        if (context != null) {
            saveQueuesToLocalPrefs(context, nextQueues)
        }
    }

    private fun mergeRemoteTournaments(remoteList: List<TournamentEntity>) {
        val byId = _tournamentsFlow.value.associateBy { it.id }.toMutableMap()
        remoteList.forEach { remote ->
            val existing = byId[remote.id]
            if (existing != null && existing.isJoinedByUser && !remote.isJoinedByUser) {
                byId[remote.id] = existing
            } else {
                byId[remote.id] = remote
            }
        }
        _tournamentsFlow.value = byId.values.toList()
    }

    private fun mergeRemoteQueues(remoteList: List<OpenPlaySkillQueueDocument>) {
        val byId = _queuesFlow.value.associateBy { it.queueId }.toMutableMap()
        remoteList.forEach { remote ->
            val existing = byId[remote.queueId]
            if (existing != null && existing.isCurrentUserJoined && !remote.isCurrentUserJoined) {
                byId[remote.queueId] = existing
            } else {
                byId[remote.queueId] = remote
            }
        }
        _queuesFlow.value = byId.values.toList()
    }

    private fun saveTournamentsToLocalPrefs(context: Context, list: List<TournamentEntity>) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            list.forEach { t ->
                val obj = JSONObject()
                toTournamentFirestoreMap(t).forEach { (k, v) -> if (v != null) obj.put(k, v) }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_TOURNAMENTS_JSON, arr.toString()).apply()
        }
    }

    private fun saveQueuesToLocalPrefs(context: Context, list: List<OpenPlaySkillQueueDocument>) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            list.forEach { q ->
                val obj = JSONObject()
                obj.put("queueId", q.queueId)
                obj.put("skillLevel", q.skillLevel)
                obj.put("tierTitle", q.tierTitle)
                obj.put("facilityName", q.facilityName)
                obj.put("courtAssignment", q.courtAssignment)
                obj.put("sessionWindow", q.sessionWindow)
                obj.put("maxPlayersPerPod", q.maxPlayersPerPod)
                obj.put("isCurrentUserJoined", q.isCurrentUserJoined)
                obj.put("updatedAtMillis", q.updatedAtMillis)
                arr.put(obj)
            }
            prefs.edit().putString(KEY_QUEUES_JSON, arr.toString()).apply()
        }
    }

    /**
     * Resets repository state for deterministic unit and Robolectric tests.
     */
    fun resetForTesting(context: Context? = null) {
        _tournamentsFlow.value = defaultSeededUpcomingTournaments
        _lastRegistrationRecord.value = null
        _queuesFlow.value = defaultSeededSkillQueues
        _lastRealtimeQueueEvent.value =
            "Firestore real-time listener active on '$COLLECTION_OPEN_PLAY_QUEUES' & '$COLLECTION_TOURNAMENTS'"
        if (context != null) {
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            }
        }
    }
}
