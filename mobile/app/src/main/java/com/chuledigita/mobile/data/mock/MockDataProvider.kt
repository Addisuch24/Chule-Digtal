package com.chuledigita.mobile.data.mock

import androidx.compose.runtime.mutableStateListOf
import com.chuledigita.mobile.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

object MockDataProvider {
    val familyMembers = listOf(
        FamilyMember("fm-1", "አበበ ከበደ", "Admin"),
        FamilyMember("fm-2", "ሀና አዲሱ", "Manager"),
        FamilyMember("fm-3", "ከበደ ገለታ", "Member")
    )

    var currentFamilyMember = familyMembers[0]

    val currentDemoFamilyMember = FamilyMember(
        id = "fm-1",
        name = "አበበ ከበደ",
        role = "Admin"
    )

    // Reactive state lists for Compose-aware UI updates.
    // Initialized as empty to ensure no writes occur during class loading (<clinit>).
    // Writes to these lists inside a read-only snapshot (like snapshotFlow) cause crashes.
    val customers = mutableStateListOf<Customer>()
    val activities = mutableStateListOf<Activity>()
    val financialMovements = mutableStateListOf<FinancialMovement>()
    val notifications = mutableStateListOf<NotificationItem>()
    val customerNotes = mutableStateListOf<CustomerNote>()
    val orders = mutableStateListOf<Order>()
    val mockNotes = mutableStateListOf<MockNote>()
    val mockOrders = mutableStateListOf<MockOrder>()
    val recentActivities = mutableStateListOf<RecentActivity>()

    /**
     * Seeds the mock data. This MUST be called from Application.onCreate() or another
     * safe entry point outside of any Jetpack Compose snapshots.
     */
    fun seedMockData() {
        if (customers.isNotEmpty()) return // Already seeded

        customers.addAll(listOf(
            Customer(
                id = "c-1", 
                name = "አበበ ከበደ (ደንበኛ)", 
                phone = "0911223344", 
                detailNote = "የጅምላ ገዢ", 
                createdBy = "አበበ ከበደ", 
                updatedBy = null, 
                createdDate = "16/09/2026", 
                updatedDate = "16/09/2026", 
                registrationDate = "16/09/2026",
                creditAmount = 5000.0,
                creditStatus = CreditStatus.UNPAID_CREDIT,
                actionHistory = listOf(
                    ActionHistory("ah-1", ActivityType.CUSTOMER_CREATED, "fm-1", "አበበ ከበደ", "ደንበኛው ተመዝግቧል።", "c-1", null, null, null, "16/09/2026", "10:00 AM"),
                    ActionHistory("ah-2", ActivityType.CREDIT_RECORDED, "fm-3", "ከበደ ገለታ", "የደንበኛው የዕዳ ሂሳብ ተመዝግቧል።", "c-1", 5000.0, null, "ተበድሮታል", "16/09/2026", "10:35 AM")
                )
            ),
            Customer(
                id = "c-2", 
                name = "Refined Customer 2", 
                phone = "0922334455", 
                detailNote = "ከባህር ዳር", 
                createdBy = "ከበደ ገለታ", 
                updatedBy = null, 
                createdDate = "17/09/2026", 
                updatedDate = "17/09/2026", 
                registrationDate = "17/09/2026",
                creditAmount = 2500.0,
                creditStatus = CreditStatus.PAID_CREDIT,
                actionHistory = listOf(
                    ActionHistory("ah-3", ActivityType.CUSTOMER_CREATED, "fm-3", "ከበደ ገለታ", "ደንበኛው ተመዝግቧል።", "c-2", null, null, null, "17/09/2026", "09:00 AM"),
                    ActionHistory("ah-4", ActivityType.CREDIT_PAID, "fm-2", "ሀና አዲሱ", "የደንበኛው ዕዳ ተከፍሏል።", "c-2", 2500.0, "ተበድሮታል", "ከፍሏል", "17/09/2026", "02:15 PM")
                )
            )
        ))

        orders.addAll(listOf(
            Order(
                id = "1",
                orderNumber = "ORD-4829",
                customerId = "c-1",
                customerName = "አበበ ከበደ (ደንበኛ)",
                description = "ዝርዝር: 5 ካርቶን ወተት\nመጠን: መካከለኛ",
                orderDate = "2023-11-15",
                orderTime = "09:30 AM",
                totalAmount = 1500.0,
                status = OrderStatus.PENDING,
                verificationStatus = VerificationStatus.PENDING,
                takenByFamilyMember = "አልማዝ ተሰማ"
            ),
            Order(
                id = "2",
                orderNumber = "ORD-8831",
                customerId = "c-2",
                customerName = "Refined Customer 2",
                description = "ዝርዝር: 10 ፓኮ ስኳር እና 5 ሊትር ዘይት\nመጠን: ትልቅ",
                orderDate = "2023-11-14",
                orderTime = "02:15 PM",
                totalAmount = 4500.0,
                status = OrderStatus.PROCESSING,
                verificationStatus = VerificationStatus.VERIFIED,
                takenByFamilyMember = "ዮሐንስ አበበ"
            ),
            Order(
                id = "3",
                orderNumber = "ORD-1204",
                customerId = "c-1",
                customerName = "አበበ ከበደ (ደንበኛ)",
                description = "ዝርዝር: 2 ኩንታል ዱቄት\nመጠን: 200kg",
                orderDate = "2023-11-12",
                orderTime = "11:00 AM",
                totalAmount = 7000.0,
                status = OrderStatus.COMPLETED,
                verificationStatus = VerificationStatus.VERIFIED,
                takenByFamilyMember = "ብሩክ ታደሰ",
                completionActor = "ዮሐንስ አበበ",
                completionDate = "2023-11-13",
                completionTime = "04:30 PM"
            )
        ))

        customerNotes.addAll(listOf(
            CustomerNote(
                id = "1",
                customerId = "c-1",
                note = "ለነገ ጧት 5 ካርቶን ወተት ለሱቅ 10 እንዲረከብ ማሳሰቢያ ተሰጥቷል።",
                date = "2023-11-15",
                time = "09:30 AM",
                authorName = "ዮሐንስ አበበ"
            ),
            CustomerNote(
                id = "2",
                customerId = "c-2",
                note = "የአቶ ከበደ የባንክ ማረጋገጫ ደረሰኝ በንግድ ባንክ በኩል ደርሶናል። እባክችሁ Verification Queue ላይ አረጋግጡ።",
                date = "2023-11-15",
                time = "10:15 AM",
                authorName = "አልማዝ ተሰማ"
            )
        ))

        mockNotes.addAll(listOf(
            MockNote("n-5", "የመጋዘን ቆጠራ ነገ ይካሄዳል።", "fm-2", "ሀና አዲሱ", "18/09/2026", "10:15 AM"),
            MockNote("n-4", "ለቀጣይ ሳምንት የምርት ትዕዛዝ ተልኳል።", "fm-1", "አበበ ከበደ", "17/09/2026", "04:30 PM"),
            MockNote("n-3", "የባንክ ሂሳብ መግለጫ መረጋገጥ አለበት።", "fm-3", "ከበደ ገለታ", "17/09/2026", "02:10 PM"),
            MockNote("n-2", "አዲስ ደንበኛ ለመመዝገብ ዝግጅት ተጠናቋል።", "fm-2", "ሀና አዲሱ", "16/09/2026", "11:45 AM"),
            MockNote("n-1", "የጤፍ ዋጋ ከአቅራቢው ጋር መወያየት አለብን።", "fm-1", "አበበ ከበደ", "16/09/2026", "09:20 AM")
        ))

        mockOrders.addAll(listOf(
            MockOrder(
                id = "mo-1",
                orderTitle = "የስንዴ ዱቄት ትዕዛዝ",
                orderDescription = "ባለ 25 ኪሎ ግራም የስንዴ ዱቄት ምርት",
                orderSize = "10 ማሸጊያ",
                createdById = "fm-1",
                createdByName = "አበበ ከበደ",
                createdDate = "15/09/2026",
                createdTime = "09:15 AM",
                status = MockOrderStatus.REGISTERED,
                actionHistory = mutableListOf(
                    OrderAction("oa-1", "mo-1", "ትዕዛዝ ተመዝግቧል", "fm-1", "አበበ ከበደ", "ትዕዛዙ ተመዝግቧል።", null, MockOrderStatus.REGISTERED, "15/09/2026", "09:15 AM")
                )
            )
        ))

        recentActivities.addAll(listOf(
            RecentActivity("ra-1", "አዲስ ደንበኛ ተመዝግቧል", "አበበ ከበደ", "10:00 AM", "CUSTOMER"),
            RecentActivity("ra-2", "የዕዳ ሂሳብ ተመዝግቧል", "ከበደ ገለታ", "10:35 AM", "CREDIT")
        ))
    }

    val dashboardSummary: DashboardSummary
        get() = DashboardSummary(
            totalCustomers = customers.size,
            pendingOrders = mockOrders.count { it.status == MockOrderStatus.REGISTERED },
            outstandingBalance = customers.sumOf { it.remainingBalanceAmount },
            unreadNotifications = notifications.count { !it.isRead }
        )

    fun addOrder(order: Order) {
        orders.add(0, order)
    }

    fun addFinancialMovement(movement: FinancialMovement) {
        financialMovements.add(0, movement)
    }

    fun addCustomerNote(note: CustomerNote) {
        customerNotes.add(0, note)
    }

    fun addNote(content: String) {
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm A", Locale.getDefault())
        val now = Date()
        
        val newNote = MockNote(
            id = "n-${System.currentTimeMillis()}",
            content = content,
            createdById = currentDemoFamilyMember.id,
            createdByName = currentDemoFamilyMember.name,
            createdDate = dateSdf.format(now),
            createdTime = timeSdf.format(now)
        )
        mockNotes.add(0, newNote)
    }

    fun getMockOrderById(id: String): MockOrder? {
        return mockOrders.find { it.id == id }
    }

    fun getMockNoteById(id: String): MockNote? {
        return mockNotes.find { it.id == id }
    }

    fun updateVerificationStatus(movementId: String, status: VerificationStatus, note: String?) {
        val index = financialMovements.indexOfFirst { it.id == movementId }
        if (index != -1) {
            financialMovements[index] = financialMovements[index].copy(
                verificationStatus = status,
                verificationNote = note
            )
        }
    }

    fun updateMockOrderStatus(orderId: String, newStatus: MockOrderStatus) {
        val index = mockOrders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            val order = mockOrders[index]
            val prevStatus = order.status
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val now = Date()
            val dateStr = dateSdf.format(now)
            val timeStr = timeSdf.format(now).uppercase(Locale.getDefault())

            val title = if (newStatus == MockOrderStatus.DELIVERED) "ትዕዛዝ ተረክቧል" else if (newStatus == MockOrderStatus.IN_PROGRESS) "ትዕዛዝ በሂደት ላይ" else "ትዕዛዝ ተመዝግቧል"
            val desc = if (newStatus == MockOrderStatus.DELIVERED) "ትዕዛዙ በተሳካ ሁኔታ ተጠናቆ ተረክቧል።" else if (newStatus == MockOrderStatus.IN_PROGRESS) "ትዕዛዙ በመከናወን ላይ ይገኛል።" else "ትዕዛዙ ተመዝግቧል።"

            val newAction = OrderAction(
                id = "oa-${System.currentTimeMillis()}",
                orderId = orderId,
                actionType = title,
                actorFamilyMemberId = currentDemoFamilyMember.id,
                actorFamilyMemberName = currentDemoFamilyMember.name,
                description = desc,
                previousStatus = prevStatus,
                newStatus = newStatus,
                date = dateStr,
                time = timeStr
            )

            mockOrders[index] = order.copy(
                status = newStatus,
                statusUpdatedById = currentDemoFamilyMember.id,
                statusUpdatedByName = currentDemoFamilyMember.name,
                statusUpdatedDate = dateStr,
                statusUpdatedTime = timeStr,
                actionHistory = (order.actionHistory + newAction).toMutableList()
            )
        }
    }

    fun addActionToHistory(
        customerId: String,
        type: ActivityType,
        description: String,
        amount: Double? = null,
        prev: String? = null,
        new: String? = null,
        date: String? = null,
        time: String? = null
    ) {
        val index = customers.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val now = Date()
            
            val finalDate = date ?: dateSdf.format(now)
            val finalTime = time ?: timeSdf.format(now).uppercase(Locale.getDefault())

            val newAction = ActionHistory(
                actionId = "ah-${System.currentTimeMillis()}",
                actionType = type,
                actorFamilyMemberId = currentFamilyMember.id,
                actorFamilyMemberName = currentFamilyMember.name,
                actionDescription = description,
                relatedCustomerId = customerId,
                amount = amount,
                previousStatus = prev,
                newStatus = new,
                date = finalDate,
                time = finalTime
            )
            
            val customer = customers[index]
            customers[index] = customer.copy(
                actionHistory = listOf(newAction) + customer.actionHistory
            )
        }
    }

    fun updateCreditStatus(customerId: String, status: CreditStatus) {
        val index = customers.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val customer = customers[index]
            val prevStatus = if (customer.creditStatus == CreditStatus.UNPAID_CREDIT) "ተበድሮታል" else null
            val newStatus = if (status == CreditStatus.PAID_CREDIT) "ከፍሏል" else "ተበድሮታል"
            
            customers[index] = customer.copy(creditStatus = status)
            
            if (status == CreditStatus.PAID_CREDIT) {
                addActionToHistory(
                    customerId = customerId,
                    type = ActivityType.CREDIT_PAID,
                    description = "የደንበኛው ዕዳ ተከፍሏል።",
                    amount = customer.creditAmount,
                    prev = prevStatus,
                    new = newStatus
                )
            }
        }
    }

    fun updateRemainingBalanceStatus(customerId: String, status: RemainingBalanceStatus) {
        val index = customers.indexOfFirst { it.id == customerId }
        if (index != -1) {
            val customer = customers[index]
            val prevStatus = if (customer.remainingBalanceStatus == RemainingBalanceStatus.REMAINING_BALANCE) "ቀሪ ሂሳብ" else null
            val newStatus = if (status == RemainingBalanceStatus.BALANCE_COLLECTED) "ተቀብሏል" else "ቀሪ ሂሳብ"

            customers[index] = customer.copy(remainingBalanceStatus = status)
            
            if (status == RemainingBalanceStatus.BALANCE_COLLECTED) {
                addActionToHistory(
                    customerId = customerId,
                    type = ActivityType.BALANCE_COLLECTED,
                    description = "የደንበኛው ቀሪ ሂሳብ ተሰጥቷል።",
                    amount = customer.remainingBalanceAmount,
                    prev = prevStatus,
                    new = newStatus
                )
            }
        }
    }

    fun addCustomer(name: String, phone: String?, note: String?) {
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = Date()
        val dateStr = dateSdf.format(now)
        
        val customer = Customer(
            id = "c-${System.currentTimeMillis()}",
            name = name,
            phone = phone,
            detailNote = note,
            createdBy = currentFamilyMember.name,
            createdDate = dateStr,
            updatedDate = dateStr,
            registrationDate = dateStr
        )
        
        customers.add(0, customer)
        
        addActionToHistory(
            customerId = customer.id,
            type = ActivityType.CUSTOMER_CREATED,
            description = "ደንበኛው ተመዝግቧል።",
            date = dateStr,
            time = timeSdf.format(now).uppercase(Locale.getDefault())
        )
    }

    fun updateCustomer(updatedCustomer: Customer) {
        val index = customers.indexOfFirst { it.id == updatedCustomer.id }
        if (index != -1) {
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val now = Date()
            val dateStr = dateSdf.format(now)
            
            val finalCustomer = updatedCustomer.copy(
                updatedDate = dateStr,
                updatedBy = currentFamilyMember.name
            )
            
            customers[index] = finalCustomer
            
            addActionToHistory(
                customerId = finalCustomer.id,
                type = ActivityType.CUSTOMER_EDITED,
                description = "የደንበኛው መረጃ ተስተካክሏል።",
                date = dateStr,
                time = timeSdf.format(now).uppercase(Locale.getDefault())
            )
        }
    }

    fun deleteCustomer(id: String) {
        customers.removeAll { it.id == id }
    }

    fun getCustomerById(id: String) = customers.find { it.id == id }

    fun getOrderById(id: String) = orders.find { it.id == id }

    fun getActivityById(id: String) = activities.find { it.id == id }
}
