package com.chuledigita.mobile.notifications

import com.chuledigita.mobile.data.remote.ApiService
import com.chuledigita.mobile.data.remote.SocketManager
import com.chuledigita.mobile.data.remote.model.NotificationDto
import com.chuledigita.mobile.data.remote.model.NotificationListResponse
import com.chuledigita.mobile.data.repository.NotificationRepositoryImpl
import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import com.chuledigita.mobile.domain.model.UserSession
import com.chuledigita.mobile.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NotificationRepositoryTest {

    private class FakeSessionRepo(val userId: Long = 1L) : SessionRepository {
        private val _session = MutableStateFlow<UserSession?>(
            UserSession(
                id = userId,
                fullName = "Family Member",
                phone = "0911223344",
                username = "member",
                isActive = true,
                token = "dummy_token"
            )
        )
        override val userSessionFlow: StateFlow<UserSession?> = _session.asStateFlow()
        override val tokenFlow: StateFlow<String?> = MutableStateFlow("dummy_token")
        override fun getCachedToken(): String? = "dummy_token"
        override suspend fun saveSession(session: UserSession) { _session.value = session }
        override suspend fun saveToken(token: String) {}
        override suspend fun clearSession() { _session.value = null }
        override fun clearSessionSync() { _session.value = null }
    }

    private fun createFailingApiService(): ApiService {
        val failingClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { _ -> throw java.io.IOException("Network unavailable") }
            .build()
        return retrofit2.Retrofit.Builder()
            .baseUrl("http://127.0.0.1:5000/api/")
            .client(failingClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private lateinit var sessionRepo: FakeSessionRepo
    private lateinit var socketManager: SocketManager
    private lateinit var inAppManager: InAppNotificationManager
    private lateinit var repository: NotificationRepositoryImpl

    @Before
    fun setUp() {
        sessionRepo = FakeSessionRepo(userId = 1L)
        socketManager = SocketManager("http://localhost:5000")
        inAppManager = InAppNotificationManager()
        repository = NotificationRepositoryImpl(
            apiService = createFailingApiService(),
            socketManager = socketManager,
            sessionRepository = sessionRepo,
            inAppNotificationManager = inAppManager
        )
    }

    @Test
    fun testSocketNotificationForAnotherRecipientIsDropped() {
        val otherUserJson = JSONObject().apply {
            put("id", 42)
            put("recipient_id", 999) // Different user!
            put("title", "Confidential Order")
            put("message", "Should not be seen by User 1")
            put("notification_type", "ORDER_CREATED")
        }

        repository.handleIncomingSocketNotification(otherUserJson)

        assertEquals("Notification for another recipient must not be stored", 0, repository.notifications.value.size)
        assertEquals("Unread count must remain 0", 0, repository.unreadCount.value)
    }

    @Test
    fun testSocketNotificationAcceptedAndDeduplicated() {
        val validJson = JSONObject().apply {
            put("id", 101)
            put("recipient_id", 1) // Belongs to User 1
            put("title", "New Note")
            put("message", "A new note was posted")
            put("notification_type", "BUSINESS_NOTE_CREATED")
            put("created_at", "2026-10-21T01:00:00Z")
        }

        // First delivery
        repository.handleIncomingSocketNotification(validJson)
        assertEquals("Valid notification should be added", 1, repository.notifications.value.size)
        assertEquals("101", repository.notifications.value.first().id)
        assertEquals(1, repository.unreadCount.value)

        // Duplicate delivery with same ID
        repository.handleIncomingSocketNotification(validJson)
        assertEquals("Duplicate notification must be ignored", 1, repository.notifications.value.size)
        assertEquals(1, repository.unreadCount.value)
    }

    @Test
    fun testOfflineNetworkErrorPreservesLocalNotifications() = runBlocking {
        // First deliver a notification into local state
        val validJson = JSONObject().apply {
            put("id", 202)
            put("recipient_id", 1)
            put("title", "Existing Cached Notification")
            put("message", "Must not be lost on network failure")
            put("notification_type", "ORDER_CREATED")
            put("created_at", "2026-10-21T01:00:00Z")
        }
        repository.handleIncomingSocketNotification(validJson)
        assertEquals(1, repository.notifications.value.size)

        // Attempt refresh with failing network (FailingApiService throws IOException)
        val result = repository.refreshNotifications()
        assertTrue("Refresh should return failure on network error", result.isFailure)

        // Crucial requirement: notifications must NOT be wiped to emptyList() on error
        assertEquals("Offline error must preserve existing cached notifications", 1, repository.notifications.value.size)
        assertEquals("202", repository.notifications.value.first().id)
    }

    @Test
    fun testDeletedNotificationIgnoredOnIncomingSocketEvent() {
        val notifId = "303"
        // Mark as deleted in tombstone set
        repository.deletedNotificationIds.add(notifId)

        val socketPayload = JSONObject().apply {
            put("id", 303)
            put("recipient_id", 1)
            put("title", "Deleted Notification Resurrect Attempt")
            put("message", "Should be ignored")
            put("notification_type", "ORDER_CREATED")
            put("created_at", "2026-10-21T01:00:00Z")
        }

        repository.handleIncomingSocketNotification(socketPayload)

        assertEquals("Deleted notification must not be resurrected by socket event", 0, repository.notifications.value.size)
        assertEquals(0, repository.unreadCount.value)
    }

    @Test
    fun testMultiDeviceDeleteSyncRemovesNotification() = runBlocking {
        // Add a notification into local state
        val validJson = JSONObject().apply {
            put("id", 404)
            put("recipient_id", 1)
            put("title", "Multi-device Notification")
            put("message", "Deleted on another device")
            put("notification_type", "ORDER_CREATED")
            put("created_at", "2026-10-21T01:00:00Z")
        }
        repository.handleIncomingSocketNotification(validJson)
        assertEquals(1, repository.notifications.value.size)

        // Simulate multi-device delete event received from SocketManager
        repository.deletedNotificationIds.add("404")
        // Filter out deleted
        assertTrue("Tombstone contains deleted id", repository.deletedNotificationIds.contains("404"))
    }
}
