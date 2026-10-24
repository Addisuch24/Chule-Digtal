package com.chuledigita.mobile.notifications

import com.chuledigita.mobile.ui.navigation.Screen
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationNavigationManagerTest {

    private lateinit var manager: NotificationNavigationManager

    @Before
    fun setUp() {
        manager = NotificationNavigationManager()
    }

    @Test
    fun testResolveRoute_forOrderNotifications() {
        val routeWithId = manager.resolveRoute("ORDER_CREATED", "ORDER", "42")
        assertEquals(Screen.OrderDetails.createRoute("42"), routeWithId)

        val routeWithoutId = manager.resolveRoute("ORDER_VERIFIED", "ORDER", "")
        assertEquals(Screen.Orders.route, routeWithoutId)
    }

    @Test
    fun testResolveRoute_forBusinessNoteNotifications() {
        val routeWithId = manager.resolveRoute("BUSINESS_NOTE_CREATED", "NOTE", "99")
        assertEquals(Screen.NoteDetails.createRoute("99"), routeWithId)

        val routeWithoutId = manager.resolveRoute("BUSINESS_NOTE_CREATED", "NOTE", null)
        assertEquals(Screen.Notes.route, routeWithoutId)
    }

    @Test
    fun testResolveRoute_forCustomerNotifications() {
        val routeWithId = manager.resolveRoute("CUSTOMER_UPDATED", "CUSTOMER", "15")
        assertEquals(Screen.CustomerDetails.createRoute("15"), routeWithId)

        val deletedRoute = manager.resolveRoute("CUSTOMER_DELETED", "CUSTOMER", "15")
        assertEquals(Screen.Customers.route, deletedRoute)
    }

    @Test
    fun testResolveRoute_forFinancialMovements() {
        val route = manager.resolveRoute("FINANCIAL_MOVEMENT_CREATED", "MOVEMENT", "777")
        assertEquals(Screen.FinancialMovementDetails.createRoute("777"), route)

        val routeWithoutId = manager.resolveRoute("PAYMENT_RECORDED", "PAYMENT", null)
        assertEquals(Screen.Finance.route, routeWithoutId)
    }

    @Test
    fun testResolveRoute_sanitizesMaliciousEntityId() {
        // If entity ID contains malicious characters or attempt to inject path traversal
        val maliciousRoute = manager.resolveRoute("ORDER_CREATED", "ORDER", "../malicious/path")
        assertEquals(Screen.Orders.route, maliciousRoute)
    }

    @Test
    fun testPendingDestination_savePeekConsume() {
        assertNull(manager.peekPendingDestination())

        manager.setPendingDestination(Screen.OrderDetails.createRoute("123"), "notif_1")
        assertEquals(Screen.OrderDetails.createRoute("123"), manager.peekPendingDestination())

        // Peeking does not consume
        assertEquals(Screen.OrderDetails.createRoute("123"), manager.peekPendingDestination())

        // Consuming returns and clears
        val consumed = manager.consumePendingDestination()
        assertEquals(Screen.OrderDetails.createRoute("123"), consumed)
        assertNull(manager.peekPendingDestination())
        assertNull(manager.consumePendingDestination())
    }

    @Test
    fun testIntentDeDuplication() {
        val key = "notif_100_ORDER_CREATED_42"
        assertFalse(manager.isIntentAlreadyHandled(key))

        manager.markIntentHandled(key)
        assertTrue(manager.isIntentAlreadyHandled(key))

        // Different intent is not handled
        assertFalse(manager.isIntentAlreadyHandled("notif_101_ORDER_CREATED_43"))
    }
}
