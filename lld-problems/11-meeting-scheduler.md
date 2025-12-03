# Meeting Scheduler - LLD

## Problem Statement
Design a meeting scheduler that can book meetings, find available slots, and handle conflicts.

---

## Code Flow Walkthrough

### `scheduleMeeting(attendees, duration)` - Schedule Meeting

```
CALL: scheduler.scheduleMeeting(
    attendees = ["Alice", "Bob", "Carol"],
    duration = 60.minutes,
    preferredRange = 9:00-17:00
)

STEP 1: Get Each Attendee's Calendar
├── calendars = attendees.map { calendarRegistry[it] }
└── Each calendar has list of existing meetings

STEP 2: Find Common Available Slots
├── findCommonSlots(calendars, date, duration):
│   ├── 
│   ├── FOR each calendar:
│   │   ├── Get available slots for the day
│   │   │   ├── Start with full day: [9:00-17:00]
│   │   │   ├── Subtract each meeting time
│   │   │   └── Result: gaps between meetings
│   │   └── Alice: [9:00-10:00, 11:00-12:00, 14:00-17:00]
│   ├── 
│   ├── Intersect all calendars:
│   │   ├── Alice: [9:00-10:00, 11:00-12:00, 14:00-17:00]
│   │   ├── Bob:   [9:30-12:00, 13:00-15:00]
│   │   ├── Carol: [9:00-11:00, 13:00-17:00]
│   │   └── Common: [9:30-10:00, 14:00-15:00]
│   ├── 
│   ├── Filter by duration (60 min needed):
│   │   ├── 9:30-10:00 (30 min) ✗ too short
│   │   └── 14:00-15:00 (60 min) ✓ fits!
│   └── Return [14:00-15:00]

STEP 3: Select Best Slot
├── slot = availableSlots.first()  // or let user choose
└── Selected: 14:00-15:00

STEP 4: Create Meeting
├── meeting = Meeting(
│   │   id = UUID,
│   │   title = "Team Sync",
│   │   attendees = ["Alice", "Bob", "Carol"],
│   │   timeSlot = 14:00-15:00,
│   │   organizer = "Alice"
│   )

STEP 5: Add to All Calendars
├── FOR each attendee:
│   ├── calendar = calendarRegistry[attendee]
│   └── calendar.addMeeting(meeting)
└── All attendees now have this meeting blocked

STEP 6: Send Invitations (optional)
├── FOR each attendee:
│   └── notificationService.sendInvite(attendee, meeting)
```

### `hasConflict(newSlot)` - Conflict Detection

```
CALL: calendar.hasConflict(TimeSlot(10:00, 11:00))

EXISTING MEETINGS:
├── Meeting A: 9:00-10:00
├── Meeting B: 10:30-11:30
└── Meeting C: 14:00-15:00

CONFLICT CHECK ALGORITHM:
├── FOR each existingMeeting in meetings:
│   ├── existing = 9:00-10:00
│   │   ├── Overlap? new.start(10:00) < existing.end(10:00) AND
│   │   │            new.end(11:00) > existing.start(9:00)
│   │   ├── 10:00 < 10:00? NO
│   │   └── No overlap ✓
│   ├── 
│   ├── existing = 10:30-11:30
│   │   ├── Overlap? 10:00 < 11:30 AND 11:00 > 10:30
│   │   ├── 10:00 < 11:30? YES
│   │   ├── 11:00 > 10:30? YES
│   │   └── CONFLICT! ✗
│   └── Return true (has conflict)

OVERLAP FORMULA:
├── Two slots overlap if:
│   └── slot1.start < slot2.end AND slot1.end > slot2.start
├── 
├── Visual:
│   ├── [====A====]         (9:00-10:00)
│   ├──        [====NEW====] (10:00-11:00)
│   ├──           [====B====] (10:30-11:30)
│   └── NEW overlaps with B!
```

### `getAvailableSlots(date, duration)` - Find Free Time

```
CALL: calendar.getAvailableSlots(date=Jan15, duration=30.minutes)

STEP 1: Get Working Hours
├── workStart = 9:00
├── workEnd = 17:00
└── Total window: 8 hours

STEP 2: Get Meetings for Date
├── meetings = getMeetingsForDate(Jan15)
├── Sort by start time
└── [Meeting(9:30-10:00), Meeting(11:00-12:00), Meeting(14:00-16:00)]

STEP 3: Find Gaps
├── gaps = []
├── 
├── Gap before first meeting:
│   ├── 9:00 to 9:30 = 30 min
│   └── gaps.add(9:00-9:30)
├── 
├── Gap between meetings:
│   ├── 10:00 to 11:00 = 60 min
│   ├── gaps.add(10:00-11:00)
│   ├── 12:00 to 14:00 = 120 min
│   └── gaps.add(12:00-14:00)
├── 
├── Gap after last meeting:
│   ├── 16:00 to 17:00 = 60 min
│   └── gaps.add(16:00-17:00)

STEP 4: Filter by Duration
├── FOR each gap:
│   ├── IF gap.duration >= 30.minutes:
│   │   └── Keep gap
│   └── ELSE: discard (too short)
├── 
└── Result: [9:00-9:30, 10:00-11:00, 12:00-14:00, 16:00-17:00]

STEP 5: Split into Slots (optional)
├── For 30-min slots from 12:00-14:00:
│   └── [12:00-12:30, 12:30-13:00, 13:00-13:30, 13:30-14:00]
```

### Recurring Meeting Flow

```
CALL: scheduler.scheduleRecurring(
    attendees = ["Team"],
    recurrence = Weekly(MONDAY, 10:00, duration=60min),
    until = Dec31
)

STEP 1: Generate Occurrences
├── occurrences = recurrence.getOccurrences(from=today, until=Dec31)
├── Result: [Jan8, Jan15, Jan22, Jan29, Feb5, ...]
└── Each Monday at 10:00

STEP 2: Check Conflicts for Each
├── FOR each date in occurrences:
│   ├── slot = TimeSlot(date, 10:00, 11:00)
│   ├── IF anyAttendeeHasConflict(slot):
│   │   ├── conflictDates.add(date)
│   │   └── Mark as conflict
│   └── ELSE: validDates.add(date)

STEP 3: Create Recurring Meeting
├── meeting = RecurringMeeting(
│   │   recurrence = Weekly(MONDAY, 10:00),
│   │   exceptions = conflictDates,  // Skip these
│   │   instances = validDates
│   )

STEP 4: Handle Exceptions
├── User can:
│   ├── Skip specific occurrence
│   ├── Reschedule specific occurrence
│   └── Cancel entire series
```

---

## Requirements

### Functional Requirements
1. Book meetings with participants
2. Check availability of users
3. Find common available slots
4. Cancel/reschedule meetings
5. Recurring meetings support
6. Meeting room management

### Non-Functional Requirements
1. Fast availability checks
2. Handle concurrent bookings
3. Conflict detection

---

## Class Diagram

```
┌─────────────────────────────────────┐
│            Meeting                  │
├─────────────────────────────────────┤
│ - id: String                        │
│ - title: String                     │
│ - organizer: User                   │
│ - participants: List<User>          │
│ - startTime: Instant                │
│ - endTime: Instant                  │
│ - room: MeetingRoom?                │
│ - recurrence: Recurrence?           │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│            Calendar                 │
├─────────────────────────────────────┤
│ - userId: String                    │
│ - meetings: List<Meeting>           │
├─────────────────────────────────────┤
│ + addMeeting(meeting)               │
│ + removeMeeting(meetingId)          │
│ + getAvailableSlots(date): List     │
│ + hasConflict(start, end): Boolean  │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│        MeetingScheduler             │
├─────────────────────────────────────┤
│ - calendars: Map<UserId, Calendar>  │
│ - rooms: List<MeetingRoom>          │
├─────────────────────────────────────┤
│ + scheduleMeeting(...): Meeting?    │
│ + findAvailableSlots(...): List     │
│ + cancelMeeting(meetingId)          │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Data Classes

```kotlin
import java.time.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== User ====================

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String
)

// ==================== Time Slot ====================

data class TimeSlot(
    val start: Instant,
    val end: Instant
) {
    init {
        require(end.isAfter(start)) { "End time must be after start time" }
    }
    
    fun overlaps(other: TimeSlot): Boolean {
        return start.isBefore(other.end) && end.isAfter(other.start)
    }
    
    fun contains(instant: Instant): Boolean {
        return !instant.isBefore(start) && instant.isBefore(end)
    }
    
    fun duration(): Duration = Duration.between(start, end)
}

// ==================== Meeting Room ====================

data class MeetingRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val capacity: Int,
    val hasVideoConference: Boolean = false,
    val floor: Int = 1
)

// ==================== Recurrence ====================

enum class RecurrenceType {
    DAILY, WEEKLY, BIWEEKLY, MONTHLY
}

data class Recurrence(
    val type: RecurrenceType,
    val endDate: LocalDate? = null,
    val occurrences: Int? = null
)

// ==================== Meeting ====================

data class Meeting(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val organizer: User,
    val participants: List<User>,
    val timeSlot: TimeSlot,
    val room: MeetingRoom? = null,
    val recurrence: Recurrence? = null,
    val createdAt: Instant = Instant.now()
) {
    fun hasParticipant(userId: String): Boolean {
        return organizer.id == userId || participants.any { it.id == userId }
    }
}

// ==================== Meeting Request ====================

data class MeetingRequest(
    val title: String,
    val description: String = "",
    val organizer: User,
    val participants: List<User>,
    val duration: Duration,
    val preferredStartTime: Instant? = null,
    val preferredTimeRange: TimeSlot? = null,
    val requiresRoom: Boolean = false,
    val roomCapacity: Int = 0,
    val recurrence: Recurrence? = null
)
```

### Calendar

```kotlin
// ==================== Calendar ====================

/**
 * User's calendar managing their meetings.
 * 
 * === Thread Safety ===
 * Uses ReentrantReadWriteLock:
 * - Read operations (getMeetings, hasConflict): concurrent
 * - Write operations (addMeeting, removeMeeting): exclusive
 * 
 * === Conflict Detection ===
 * Two time slots conflict if they overlap:
 *   slot1.start < slot2.end AND slot1.end > slot2.start
 * 
 * === Available Slots Algorithm ===
 * 1. Get all meetings for the day (sorted by start time)
 * 2. Find gaps between meetings within working hours
 * 3. Break gaps into slots of requested duration
 * 
 * Time Complexity:
 * - addMeeting: O(n log n) due to sorting
 * - hasConflict: O(n) linear scan
 * - getAvailableSlots: O(n) where n = meetings that day
 */
class Calendar(val userId: String) {
    // Meetings stored sorted by start time for efficient gap finding
    private val meetings = mutableListOf<Meeting>()
    private val lock = ReentrantReadWriteLock()
    
    // ==================== Meeting Management ====================
    
    /**
     * Add a meeting to the calendar if no conflict exists.
     * 
     * @param meeting The meeting to add
     * @return true if added, false if conflicts with existing meeting
     */
    fun addMeeting(meeting: Meeting): Boolean {
        lock.write {
            // Check for conflicts before adding
            if (hasConflict(meeting.timeSlot)) {
                return false
            }
            meetings.add(meeting)
            // Keep sorted for efficient gap-finding
            meetings.sortBy { it.timeSlot.start }
            return true
        }
    }
    
    fun removeMeeting(meetingId: String): Meeting? {
        lock.write {
            val index = meetings.indexOfFirst { it.id == meetingId }
            return if (index >= 0) meetings.removeAt(index) else null
        }
    }
    
    fun getMeeting(meetingId: String): Meeting? {
        return lock.read {
            meetings.find { it.id == meetingId }
        }
    }
    
    fun getMeetings(date: LocalDate, timezone: ZoneId = ZoneId.systemDefault()): List<Meeting> {
        val dayStart = date.atStartOfDay(timezone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(timezone).toInstant()
        
        return lock.read {
            meetings.filter { meeting ->
                val slot = meeting.timeSlot
                slot.start.isBefore(dayEnd) && slot.end.isAfter(dayStart)
            }
        }
    }
    
    fun getMeetings(range: TimeSlot): List<Meeting> {
        return lock.read {
            meetings.filter { it.timeSlot.overlaps(range) }
        }
    }
    
    // ==================== Availability ====================
    
    fun hasConflict(slot: TimeSlot): Boolean {
        return lock.read {
            meetings.any { it.timeSlot.overlaps(slot) }
        }
    }
    
    fun getAvailableSlots(
        date: LocalDate,
        slotDuration: Duration,
        workingHoursStart: LocalTime = LocalTime.of(9, 0),
        workingHoursEnd: LocalTime = LocalTime.of(18, 0),
        timezone: ZoneId = ZoneId.systemDefault()
    ): List<TimeSlot> {
        val dayStart = LocalDateTime.of(date, workingHoursStart).atZone(timezone).toInstant()
        val dayEnd = LocalDateTime.of(date, workingHoursEnd).atZone(timezone).toInstant()
        
        val dayMeetings = getMeetings(date, timezone).sortedBy { it.timeSlot.start }
        val availableSlots = mutableListOf<TimeSlot>()
        
        var currentStart = dayStart
        
        for (meeting in dayMeetings) {
            // Gap before this meeting
            if (currentStart.isBefore(meeting.timeSlot.start)) {
                val gapEnd = meeting.timeSlot.start
                addSlotsInRange(currentStart, gapEnd, slotDuration, availableSlots)
            }
            currentStart = maxOf(currentStart, meeting.timeSlot.end)
        }
        
        // Gap after last meeting
        if (currentStart.isBefore(dayEnd)) {
            addSlotsInRange(currentStart, dayEnd, slotDuration, availableSlots)
        }
        
        return availableSlots
    }
    
    private fun addSlotsInRange(
        start: Instant,
        end: Instant,
        duration: Duration,
        slots: MutableList<TimeSlot>
    ) {
        var slotStart = start
        while (slotStart.plus(duration).isBefore(end) || slotStart.plus(duration) == end) {
            slots.add(TimeSlot(slotStart, slotStart.plus(duration)))
            slotStart = slotStart.plus(Duration.ofMinutes(30)) // 30-min increments
        }
    }
    
    fun getBusySlots(date: LocalDate, timezone: ZoneId = ZoneId.systemDefault()): List<TimeSlot> {
        return getMeetings(date, timezone).map { it.timeSlot }
    }
}
```

### Meeting Scheduler

```kotlin
// ==================== Meeting Scheduler ====================

class MeetingScheduler {
    private val calendars = ConcurrentHashMap<String, Calendar>()
    private val meetings = ConcurrentHashMap<String, Meeting>()
    private val rooms = mutableListOf<MeetingRoom>()
    private val roomCalendars = ConcurrentHashMap<String, Calendar>()
    
    private val lock = ReentrantReadWriteLock()
    
    // ==================== User Management ====================
    
    fun registerUser(user: User) {
        calendars.putIfAbsent(user.id, Calendar(user.id))
    }
    
    fun getCalendar(userId: String): Calendar? = calendars[userId]
    
    // ==================== Room Management ====================
    
    fun addRoom(room: MeetingRoom) {
        lock.write {
            rooms.add(room)
            roomCalendars[room.id] = Calendar(room.id)
        }
    }
    
    fun getAvailableRooms(
        slot: TimeSlot,
        minCapacity: Int = 0,
        requiresVideoConference: Boolean = false
    ): List<MeetingRoom> {
        return lock.read {
            rooms.filter { room ->
                room.capacity >= minCapacity &&
                (!requiresVideoConference || room.hasVideoConference) &&
                !roomCalendars[room.id]!!.hasConflict(slot)
            }
        }
    }
    
    // ==================== Meeting Scheduling ====================
    
    fun scheduleMeeting(
        title: String,
        organizer: User,
        participants: List<User>,
        slot: TimeSlot,
        room: MeetingRoom? = null,
        description: String = ""
    ): Meeting? {
        lock.write {
            // Ensure all users are registered
            val allUsers = listOf(organizer) + participants
            allUsers.forEach { registerUser(it) }
            
            // Check conflicts for all participants
            val allParticipants = allUsers.distinctBy { it.id }
            for (user in allParticipants) {
                val calendar = calendars[user.id]!!
                if (calendar.hasConflict(slot)) {
                    println("Conflict detected for user: ${user.name}")
                    return null
                }
            }
            
            // Check room availability if specified
            if (room != null) {
                val roomCalendar = roomCalendars[room.id]
                if (roomCalendar?.hasConflict(slot) == true) {
                    println("Room ${room.name} is not available")
                    return null
                }
            }
            
            // Create meeting
            val meeting = Meeting(
                title = title,
                description = description,
                organizer = organizer,
                participants = participants,
                timeSlot = slot,
                room = room
            )
            
            // Add to all calendars
            allParticipants.forEach { user ->
                calendars[user.id]!!.addMeeting(meeting)
            }
            
            // Add to room calendar
            room?.let {
                roomCalendars[it.id]?.addMeeting(meeting)
            }
            
            meetings[meeting.id] = meeting
            return meeting
        }
    }
    
    fun cancelMeeting(meetingId: String): Boolean {
        lock.write {
            val meeting = meetings.remove(meetingId) ?: return false
            
            // Remove from all participant calendars
            val allUsers = listOf(meeting.organizer) + meeting.participants
            allUsers.forEach { user ->
                calendars[user.id]?.removeMeeting(meetingId)
            }
            
            // Remove from room calendar
            meeting.room?.let {
                roomCalendars[it.id]?.removeMeeting(meetingId)
            }
            
            return true
        }
    }
    
    // ==================== Availability ====================
    
    fun findCommonAvailableSlots(
        users: List<User>,
        date: LocalDate,
        duration: Duration,
        timezone: ZoneId = ZoneId.systemDefault()
    ): List<TimeSlot> {
        users.forEach { registerUser(it) }
        
        // Get available slots for each user
        val userSlots = users.map { user ->
            calendars[user.id]!!.getAvailableSlots(date, duration, timezone = timezone)
        }
        
        if (userSlots.isEmpty()) return emptyList()
        
        // Find intersection of all available slots
        var commonSlots = userSlots.first().toSet()
        
        for (slots in userSlots.drop(1)) {
            commonSlots = commonSlots.intersect(slots.toSet())
        }
        
        return commonSlots.sortedBy { it.start }
    }
    
    fun suggestMeetingTimes(
        request: MeetingRequest,
        dates: List<LocalDate>,
        timezone: ZoneId = ZoneId.systemDefault()
    ): List<TimeSlot> {
        val allUsers = listOf(request.organizer) + request.participants
        
        return dates.flatMap { date ->
            findCommonAvailableSlots(allUsers, date, request.duration, timezone)
        }.take(5) // Return top 5 suggestions
    }
    
    // ==================== Quick Scheduling ====================
    
    fun scheduleMeetingWithAutoRoom(
        title: String,
        organizer: User,
        participants: List<User>,
        slot: TimeSlot,
        minCapacity: Int = 0
    ): Meeting? {
        val requiredCapacity = participants.size + 1
        val actualCapacity = maxOf(minCapacity, requiredCapacity)
        
        val availableRoom = getAvailableRooms(slot, actualCapacity).firstOrNull()
        
        return scheduleMeeting(title, organizer, participants, slot, availableRoom)
    }
}
```

### Usage Example

```kotlin
fun main() {
    val scheduler = MeetingScheduler()
    
    // Add rooms
    scheduler.addRoom(MeetingRoom(name = "Conference A", capacity = 10, hasVideoConference = true))
    scheduler.addRoom(MeetingRoom(name = "Conference B", capacity = 6))
    scheduler.addRoom(MeetingRoom(name = "Huddle Room", capacity = 4))
    
    // Create users
    val alice = User(name = "Alice", email = "alice@example.com")
    val bob = User(name = "Bob", email = "bob@example.com")
    val charlie = User(name = "Charlie", email = "charlie@example.com")
    
    scheduler.registerUser(alice)
    scheduler.registerUser(bob)
    scheduler.registerUser(charlie)
    
    println("=== Meeting Scheduler Demo ===\n")
    
    // Schedule a meeting
    val tomorrow = LocalDate.now().plusDays(1)
    val meetingStart = tomorrow.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant()
    val meetingEnd = meetingStart.plus(Duration.ofHours(1))
    
    val meeting1 = scheduler.scheduleMeetingWithAutoRoom(
        title = "Project Kickoff",
        organizer = alice,
        participants = listOf(bob, charlie),
        slot = TimeSlot(meetingStart, meetingEnd)
    )
    
    meeting1?.let {
        println("Scheduled: ${it.title}")
        println("  Room: ${it.room?.name ?: "No room"}")
        println("  Time: ${it.timeSlot}")
        println("  Participants: ${it.participants.map { p -> p.name }}")
    }
    
    // Find available slots
    println("\n--- Available Slots for Team ---")
    val availableSlots = scheduler.findCommonAvailableSlots(
        users = listOf(alice, bob, charlie),
        date = tomorrow,
        duration = Duration.ofMinutes(30)
    )
    
    availableSlots.take(5).forEach { slot ->
        println("  ${slot.start} - ${slot.end}")
    }
    
    // Try to schedule conflicting meeting
    println("\n--- Attempting Conflicting Meeting ---")
    val conflictMeeting = scheduler.scheduleMeeting(
        title = "Conflicting Meeting",
        organizer = bob,
        participants = listOf(alice),
        slot = TimeSlot(meetingStart, meetingEnd) // Same time as meeting1
    )
    
    if (conflictMeeting == null) {
        println("Meeting could not be scheduled (conflict detected)")
    }
    
    // View calendar
    println("\n--- Alice's Calendar for Tomorrow ---")
    scheduler.getCalendar(alice.id)?.getMeetings(tomorrow)?.forEach { m ->
        println("  ${m.title}: ${m.timeSlot}")
    }
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Repository** | Calendar, MeetingScheduler | Data access abstraction |
| **Factory** | Could add for meeting creation | Complex object creation |
| **Observer** | Could add for notifications | Meeting updates |
| **Strategy** | Could add for slot finding | Different algorithms |

---

## Interview Discussion Points

### Q: How to handle timezone complexity?
**A:**
- Store all times in UTC
- Convert to user's timezone for display
- Handle DST transitions carefully
- Show timezone in meeting invites

### Q: How to optimize availability queries?
**A:**
- Pre-compute free/busy times
- Use interval trees for O(log n) conflict detection
- Cache common availability patterns
- Batch availability queries

### Q: How to handle recurring meetings?
**A:**
- Store recurrence pattern, not individual instances
- Generate instances on-demand
- Handle exceptions (skip dates)
- Efficient conflict detection with patterns

---

## Time Complexity

| Operation | Complexity |
|-----------|------------|
| Schedule Meeting | O(P × M) where P=participants, M=meetings |
| Find Available Slots | O(P × M) |
| Cancel Meeting | O(P) |
| Conflict Check | O(M) per user, O(log M) with interval tree |

