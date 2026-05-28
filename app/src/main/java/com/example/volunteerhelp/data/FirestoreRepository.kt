package com.example.volunteerhelp.data

import com.example.volunteerhelp.model.Campaign
import com.example.volunteerhelp.model.CampaignStatus
import com.example.volunteerhelp.model.CampaignType
import com.example.volunteerhelp.model.HelpRequest
import com.example.volunteerhelp.model.HelpRequestStatus
import com.example.volunteerhelp.model.ProfileStats
import com.example.volunteerhelp.model.Report
import com.example.volunteerhelp.model.User
import com.example.volunteerhelp.model.UserRole
import com.example.volunteerhelp.util.Constants
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun generateId(collection: String): String = firestore.collection(collection).document().id

    suspend fun createUser(user: User) {
        val usernameLower = user.usernameLowercase.ifBlank { normalizeUsername(user.username) }
        if (usernameLower.isBlank()) throw IllegalArgumentException("Нікнейм не може бути порожнім")
        val userWithUsername = user.copy(username = user.username.trim(), usernameLowercase = usernameLower)
        val userRef = firestore.collection(Constants.USERS_COLLECTION).document(user.id)
        val usernameRef = firestore.collection(Constants.USERNAMES_COLLECTION).document(usernameLower)
        firestore.runTransaction { transaction ->
            if (transaction.get(usernameRef).exists()) {
                throw IllegalStateException("Такий нікнейм уже використовується")
            }
            transaction.set(userRef, userWithUsername)
            transaction.set(
                usernameRef,
                mapOf("userId" to user.id, "username" to userWithUsername.username, "createdAt" to user.createdAt)
            )
        }.await()
    }

    suspend fun getUser(userId: String): User? {
        val user = firestore.collection(Constants.USERS_COLLECTION).document(userId).get().await().toUser()
            ?: return null
        return if (user.role == UserRole.DONOR.name) user.copy(rating = approvedPointsForDonor(user.id)) else user
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(Constants.USERS_COLLECTION).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUser())
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateProfile(user: User, previousUsernameLowercase: String) {
        val newUsernameLower = normalizeUsername(user.username)
        val updated = user.copy(username = user.username.trim(), usernameLowercase = newUsernameLower)
        val userRef = firestore.collection(Constants.USERS_COLLECTION).document(user.id)
        firestore.runTransaction { transaction ->
            val current = transaction.get(userRef).toObject(User::class.java)
                ?: throw IllegalStateException("Профіль не знайдено")
            val oldUsernameLower = previousUsernameLowercase.ifBlank { current.usernameLowercase }
            if (newUsernameLower.isBlank()) throw IllegalArgumentException("Нікнейм не може бути порожнім")
            if (oldUsernameLower != newUsernameLower) {
                val newUsernameRef = firestore.collection(Constants.USERNAMES_COLLECTION).document(newUsernameLower)
                val newUsernameDoc = transaction.get(newUsernameRef)
                if (newUsernameDoc.exists() && newUsernameDoc.getString("userId") != user.id) {
                    throw IllegalStateException("Такий нікнейм уже використовується")
                }
                if (oldUsernameLower.isNotBlank()) {
                    transaction.delete(firestore.collection(Constants.USERNAMES_COLLECTION).document(oldUsernameLower))
                }
                transaction.set(
                    newUsernameRef,
                    mapOf("userId" to user.id, "username" to updated.username, "createdAt" to System.currentTimeMillis())
                )
            }
            transaction.set(userRef, updated)
        }.await()
    }

    suspend fun verifyVolunteer(userId: String) {
        val verifiedAt = System.currentTimeMillis()
        firestore.collection(Constants.USERS_COLLECTION).document(userId)
            .set(mapOf("isVerified" to true, "verifiedAt" to verifiedAt), SetOptions.merge())
            .await()
    }

    suspend fun isUsernameAvailable(username: String, currentUserId: String? = null): Boolean {
        val doc = firestore.collection(Constants.USERNAMES_COLLECTION).document(normalizeUsername(username)).get().await()
        return !doc.exists() || doc.getString("userId") == currentUserId
    }

    suspend fun searchUsers(query: String): List<User> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return firestore.collection(Constants.USERS_COLLECTION).get().await().documents
            .mapNotNull { it.toUser() }
            .filter { user ->
                listOf(user.name, user.username, user.usernameLowercase, user.email, user.city, user.region)
                    .any { it.lowercase().contains(normalized) }
            }
            .sortedWith(compareByDescending<User> { it.isVerified }.thenBy { it.name.lowercase() })
    }

    suspend fun getFollowers(userId: String): List<User> {
        val follows = firestore.collection(Constants.FOLLOWS_COLLECTION)
            .whereEqualTo("followingId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followerId") }

        return follows.mapNotNull { getUser(it) }
            .sortedWith(compareByDescending<User> { it.isVerified }.thenBy { it.name.lowercase() })
    }

    suspend fun getFollowing(userId: String): List<User> {
        val follows = firestore.collection(Constants.FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("followingId") }

        return follows.mapNotNull { getUser(it) }
            .sortedWith(compareByDescending<User> { it.isVerified }.thenBy { it.name.lowercase() })
    }

    suspend fun followUser(currentUserId: String, targetUserId: String) {
        require(currentUserId != targetUserId) { "Не можна підписатися на себе" }
        val followId = "${currentUserId}_${targetUserId}"
        val followRef = firestore.collection(Constants.FOLLOWS_COLLECTION).document(followId)
        val currentUserRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
        val targetUserRef = firestore.collection(Constants.USERS_COLLECTION).document(targetUserId)
        firestore.runTransaction { transaction ->
            if (!transaction.get(followRef).exists()) {
                transaction.set(followRef, mapOf("id" to followId, "followerId" to currentUserId, "followingId" to targetUserId, "createdAt" to System.currentTimeMillis()))
                transaction.update(currentUserRef, "followingCount", FieldValue.increment(1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(1))
            }
        }.await()
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        val followId = "${currentUserId}_${targetUserId}"
        val followRef = firestore.collection(Constants.FOLLOWS_COLLECTION).document(followId)
        val currentUserRef = firestore.collection(Constants.USERS_COLLECTION).document(currentUserId)
        val targetUserRef = firestore.collection(Constants.USERS_COLLECTION).document(targetUserId)
        firestore.runTransaction { transaction ->
            if (transaction.get(followRef).exists()) {
                transaction.delete(followRef)
                transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1))
                transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1))
            }
        }.await()
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return firestore.collection(Constants.FOLLOWS_COLLECTION).document("${currentUserId}_${targetUserId}")
            .get().await().exists()
    }

    fun observeFollowersCount(userId: String): Flow<Int> = observeCount(Constants.FOLLOWS_COLLECTION, "followingId", userId)

    fun observeFollowingCount(userId: String): Flow<Int> = observeCount(Constants.FOLLOWS_COLLECTION, "followerId", userId)

    fun observeFollowingIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection(Constants.FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.getString("followingId") }.toSet())
            }
        awaitClose { listener.remove() }
    }

    fun observeUserRating(userId: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(Constants.HELP_REQUESTS_COLLECTION)
            .whereEqualTo("donorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val rating = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toHelpRequest() }
                    .filter { it.status == HelpRequestStatus.APPROVED.name }
                    .sumOf { calculatePoints(it.type, it.amount) }
                trySend(rating)
            }
        awaitClose { listener.remove() }
    }

    suspend fun createCampaign(campaign: Campaign) {
        firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(campaign.id).set(campaign).await()
    }

    suspend fun getCampaign(campaignId: String): Campaign? {
        return firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(campaignId).get().await().toCampaign()
    }

    fun observeCampaign(campaignId: String): Flow<Campaign?> = callbackFlow {
        val listener = firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(campaignId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toCampaign())
            }
        awaitClose { listener.remove() }
    }

    fun observeActiveCampaigns(): Flow<List<Campaign>> = observeCampaigns(activeOnly = true)

    fun observeFeedCampaigns(): Flow<List<Campaign>> = observeCampaigns(activeOnly = false)

    fun observeVolunteerCampaigns(volunteerId: String): Flow<List<Campaign>> = callbackFlow {
        val listener = firestore.collection(Constants.CAMPAIGNS_COLLECTION)
            .whereEqualTo("volunteerId", volunteerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toCampaign() }.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun closeCampaign(campaignId: String, volunteerId: String) {
        val campaignRef = firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(campaignId)
        val userRef = firestore.collection(Constants.USERS_COLLECTION).document(volunteerId)
        firestore.runTransaction { transaction ->
            val campaign = transaction.get(campaignRef).toObject(Campaign::class.java)
                ?: throw IllegalStateException("Збір не знайдено")
            if (campaign.volunteerId != volunteerId) throw IllegalStateException("Недостатньо прав для закриття збору")
            transaction.update(campaignRef, "status", CampaignStatus.CLOSED.name)
            transaction.update(userRef, "closedCampaignsCount", FieldValue.increment(1))
        }.await()
    }

    suspend fun createHelpRequest(helpRequest: HelpRequest) {
        firestore.collection(Constants.HELP_REQUESTS_COLLECTION).document(helpRequest.id).set(helpRequest).await()
    }

    fun observePendingHelpRequests(volunteerId: String): Flow<List<HelpRequest>> = callbackFlow {
        val listener = firestore.collection(Constants.HELP_REQUESTS_COLLECTION)
            .whereEqualTo("volunteerId", volunteerId)
            .whereEqualTo("status", HelpRequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toHelpRequest() }.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun observeDonorHelpRequests(donorId: String): Flow<List<HelpRequest>> = callbackFlow {
        val listener = firestore.collection(Constants.HELP_REQUESTS_COLLECTION)
            .whereEqualTo("donorId", donorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toHelpRequest() }.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun approveHelpRequest(helpRequestId: String, volunteerId: String) {
        val helpRequestRef = firestore.collection(Constants.HELP_REQUESTS_COLLECTION).document(helpRequestId)
        firestore.runTransaction { transaction ->
            val helpRequest = transaction.get(helpRequestRef).toObject(HelpRequest::class.java)
                ?: throw IllegalStateException("Заявку не знайдено")
            if (helpRequest.volunteerId != volunteerId) throw IllegalStateException("Недостатньо прав для підтвердження")
            if (helpRequest.status != HelpRequestStatus.PENDING.name) throw IllegalStateException("Заявка вже оброблена")
            val campaignRef = firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(helpRequest.campaignId)
            val donorRef = firestore.collection(Constants.USERS_COLLECTION).document(helpRequest.donorId)
            val campaign = transaction.get(campaignRef).toObject(Campaign::class.java)
                ?: throw IllegalStateException("Збір не знайдено")
            transaction.update(helpRequestRef, "status", HelpRequestStatus.APPROVED.name)
            if (helpRequest.type == CampaignType.FINANCIAL.name) {
                val newAmount = campaign.currentAmount + helpRequest.amount
                val newStatus = if (campaign.targetAmount > 0 && newAmount >= campaign.targetAmount) CampaignStatus.COMPLETED.name else campaign.status
                transaction.update(campaignRef, mapOf("currentAmount" to newAmount, "status" to newStatus))
                transaction.update(donorRef, "totalHelpAmount", FieldValue.increment(helpRequest.amount))
            } else {
                transaction.update(campaignRef, "currentAmount", campaign.currentAmount + 1.0)
            }
            transaction.update(donorRef, "rating", FieldValue.increment(calculatePoints(helpRequest.type, helpRequest.amount).toLong()))
        }.await()
    }

    suspend fun rejectHelpRequest(helpRequestId: String, volunteerId: String) {
        val helpRequestRef = firestore.collection(Constants.HELP_REQUESTS_COLLECTION).document(helpRequestId)
        firestore.runTransaction { transaction ->
            val helpRequest = transaction.get(helpRequestRef).toObject(HelpRequest::class.java)
                ?: throw IllegalStateException("Заявку не знайдено")
            if (helpRequest.volunteerId != volunteerId) throw IllegalStateException("Недостатньо прав для відхилення")
            transaction.update(helpRequestRef, "status", HelpRequestStatus.REJECTED.name)
        }.await()
    }

    suspend fun createReport(report: Report) {
        val reportRef = firestore.collection(Constants.REPORTS_COLLECTION).document(report.id)
        val campaignRef = firestore.collection(Constants.CAMPAIGNS_COLLECTION).document(report.campaignId)
        firestore.runTransaction { transaction ->
            val campaign = transaction.get(campaignRef).toObject(Campaign::class.java)
                ?: throw IllegalStateException("Збір не знайдено")
            if (campaign.volunteerId != report.volunteerId) throw IllegalStateException("Недостатньо прав для додавання звіту")
            transaction.set(reportRef, report.copy(campaignTitle = report.campaignTitle.ifBlank { campaign.title }))
            transaction.update(campaignRef, "status", CampaignStatus.CLOSED.name)
        }.await()
    }

    fun observeReports(campaignId: String): Flow<List<Report>> = callbackFlow {
        val listener = firestore.collection(Constants.REPORTS_COLLECTION)
            .whereEqualTo("campaignId", campaignId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toReport() }.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun observeAllReports(): Flow<List<Report>> = callbackFlow {
        val listener = firestore.collection(Constants.REPORTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toReport() }.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    suspend fun getProfileStats(userId: String, role: String): ProfileStats {
        val campaigns = firestore.collection(Constants.CAMPAIGNS_COLLECTION).whereEqualTo("volunteerId", userId).get().await().documents.mapNotNull { it.toCampaign() }
        val reports = firestore.collection(Constants.REPORTS_COLLECTION).whereEqualTo("volunteerId", userId).get().await().documents.mapNotNull { it.toReport() }
        val donorRequests = firestore.collection(Constants.HELP_REQUESTS_COLLECTION).whereEqualTo("donorId", userId).get().await().documents.mapNotNull { it.toHelpRequest() }
        val volunteerRequests = firestore.collection(Constants.HELP_REQUESTS_COLLECTION).whereEqualTo("volunteerId", userId).get().await().documents.mapNotNull { it.toHelpRequest() }
        val rating = donorRequests.filter { it.status == HelpRequestStatus.APPROVED.name }.sumOf { calculatePoints(it.type, it.amount) }
        val title = donorTitle(rating)
        return ProfileStats(
            totalCampaigns = campaigns.size,
            activeCampaigns = campaigns.count { it.status == CampaignStatus.ACTIVE.name },
            completedCampaigns = campaigns.count { it.status == CampaignStatus.COMPLETED.name },
            closedCampaigns = campaigns.count { it.status == CampaignStatus.CLOSED.name },
            reportsCount = reports.size,
            totalRaisedAmount = campaigns.sumOf { it.currentAmount },
            pendingHelpRequestsCount = volunteerRequests.count { it.status == HelpRequestStatus.PENDING.name },
            approvedHelpRequestsCount = volunteerRequests.count { it.status == HelpRequestStatus.APPROVED.name },
            approvedHelpCount = donorRequests.count { it.status == HelpRequestStatus.APPROVED.name },
            pendingHelpCount = donorRequests.count { it.status == HelpRequestStatus.PENDING.name },
            rejectedHelpCount = donorRequests.count { it.status == HelpRequestStatus.REJECTED.name },
            totalDonatedAmount = donorRequests.filter { it.status == HelpRequestStatus.APPROVED.name }.sumOf { it.amount },
            rating = rating,
            level = if (role == UserRole.DONOR.name) "Рівень ${rating / 250 + 1}" else "Волонтерська сторінка",
            title = title
        )
    }

    private fun observeCampaigns(activeOnly: Boolean): Flow<List<Campaign>> = callbackFlow {
        val query = if (activeOnly) {
            firestore.collection(Constants.CAMPAIGNS_COLLECTION).whereEqualTo("status", CampaignStatus.ACTIVE.name)
        } else {
            firestore.collection(Constants.CAMPAIGNS_COLLECTION)
        }
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.toCampaign() }.sortedByDescending { it.createdAt })
        }
        awaitClose { listener.remove() }
    }

    private fun observeCount(collection: String, field: String, value: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection(collection).whereEqualTo(field, value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }

    private fun calculatePoints(type: String, amount: Double): Int {
        return if (type == CampaignType.FINANCIAL.name) maxOf(1, amount.toInt() / 10) else 10
    }

    private suspend fun approvedPointsForDonor(userId: String): Int {
        return firestore.collection(Constants.HELP_REQUESTS_COLLECTION)
            .whereEqualTo("donorId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toHelpRequest() }
            .filter { it.status == HelpRequestStatus.APPROVED.name }
            .sumOf { calculatePoints(it.type, it.amount) }
    }

    private fun donorTitle(rating: Int): String = when {
        rating >= 1000 -> "Надійний партнер"
        rating >= 500 -> "Активний благодійник"
        rating >= 100 -> "Добрий помічник"
        else -> "Новачок допомоги"
    }

    private fun normalizeUsername(username: String): String = username.trim().removePrefix("@").lowercase()

    private fun DocumentSnapshot.toUser(): User? = toObject(User::class.java)
    private fun DocumentSnapshot.toCampaign(): Campaign? = toObject(Campaign::class.java)
    private fun DocumentSnapshot.toHelpRequest(): HelpRequest? = toObject(HelpRequest::class.java)
    private fun DocumentSnapshot.toReport(): Report? = toObject(Report::class.java)
}
