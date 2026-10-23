package com.chuledigita.mobile.notifications

import com.chuledigita.mobile.domain.model.NotificationItem
import com.chuledigita.mobile.domain.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InAppNotificationManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var manager: InAppNotificationManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        manager = InAppNotificationManager(scope = testScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createItem(id: String, title: String = "Test Title"): NotificationItem {
        return NotificationItem(
            id = id,
            title = title,
            message = "Test message for $id",
            date = "Oct 21, 2026",
            time = "10:00 AM",
            isRead = false,
            type = NotificationType.ORDER_CREATED,
            entityType = "ORDER",
            entityId = "100"
        )
    }

    @Test
    fun testSingleNotificationDisplay() {
        val item = createItem("1")
        manager.showNotification(item)

        val current = manager.currentPopUp.value
        assertNotNull("Pop-up should be visible", current)
        assertTrue("Pop-up should be Single", current is InAppPopUpState.Single)
        assertEquals("1", (current as InAppPopUpState.Single).item.id)
        assertTrue("Presented set should contain ID 1", manager.presentedNotificationIds.contains("1"))
    }

    @Test
    fun testDeduplicationPreventsRepeatedPopUp() {
        val item = createItem("10")
        manager.showNotification(item)
        assertEquals("10", (manager.currentPopUp.value as? InAppPopUpState.Single)?.item?.id)

        // Dismiss first pop-up
        manager.dismiss()
        assertNull("Pop-up should be dismissed", manager.currentPopUp.value)

        // Attempt to show the same notification ID again
        manager.showNotification(item)
        assertNull("Repeated notification ID must be deduplicated and not shown again", manager.currentPopUp.value)
    }

    @Test
    fun testOfflineRecoverySmallBatchShowsIndividually() {
        val item1 = createItem("21")
        val item2 = createItem("22")

        manager.showBatchFromRecovery(listOf(item1, item2))

        val current = manager.currentPopUp.value
        assertNotNull(current)
        assertTrue("Small batch (<=2) should show as Single", current is InAppPopUpState.Single)
        assertEquals("21", (current as InAppPopUpState.Single).item.id)

        // Dismiss advances to the second queued item
        manager.dismiss()
        val next = manager.currentPopUp.value
        assertNotNull(next)
        assertTrue("Second item should be displayed", next is InAppPopUpState.Single)
        assertEquals("22", (next as InAppPopUpState.Single).item.id)
    }

    @Test
    fun testOfflineRecoveryLargeBatchAggregatesIntoSummary() {
        val items = (1..5).map { createItem("batch-$it", "Batch Title $it") }

        manager.showBatchFromRecovery(items)

        val current = manager.currentPopUp.value
        assertNotNull("Pop-up should be visible", current)
        assertTrue("Large batch (>=3) must aggregate into Summary to prevent screen spam", current is InAppPopUpState.Summary)
        val summary = current as InAppPopUpState.Summary
        assertEquals(5, summary.count)
        assertEquals("batch-1", summary.latestItem.id)
    }

    @Test
    fun testClearSessionResetsAllState() {
        manager.showNotification(createItem("55"))
        assertNotNull(manager.currentPopUp.value)
        assertEquals(1, manager.presentedNotificationIds.size)

        manager.clearSession()
        assertNull("Pop-up state should be cleared on logout/clearSession", manager.currentPopUp.value)
        assertTrue("Presented IDs should be cleared", manager.presentedNotificationIds.isEmpty())
    }
}
