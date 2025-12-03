# Tennis Court Booking (Meeting Rooms Variant)

## Problem Information
- **Difficulty**: Medium-Hard
- **Company**: Atlassian
- **Topics**: Greedy, Sorting, Heap, Intervals, Simulation

## Problem Description

Implement a function that given a list of tennis court bookings with start and finish times, returns a plan assigning each booking to a specific court, ensuring each court is used by only one booking at a time and using the minimum number of courts.

### Part A: Basic Assignment

```kotlin
data class BookingRecord(
    val id: Int,
    val startTime: Int,
    val finishTime: Int
)

data class CourtAssignment(
    val bookingId: Int,
    val courtId: Int
)
```

### Example:
```
Bookings: [(1, 0, 30), (2, 5, 10), (3, 15, 20)]

Output: 
- Booking 1 → Court 1
- Booking 2 → Court 2  (overlaps with booking 1)
- Booking 3 → Court 1  (booking 1 still running, but booking 2 finished)

Courts needed: 2
```

---

## Part A: Basic Solution

```kotlin
import java.util.PriorityQueue

data class BookingRecord(
    val id: Int,
    val startTime: Int,
    val finishTime: Int
)

data class CourtAssignment(
    val bookingId: Int,
    val courtId: Int
)

fun assignCourts(bookings: List<BookingRecord>): List<CourtAssignment> {
    if (bookings.isEmpty()) return emptyList()
    
    // Sort bookings by start time
    val sortedBookings = bookings.sortedBy { it.startTime }
    
    // Min heap of (endTime, courtId)
    val courtEndTimes = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
    
    val assignments = mutableListOf<CourtAssignment>()
    var nextCourtId = 1
    
    for (booking in sortedBookings) {
        // Check if any court is free (earliest ending court finished before this booking starts)
        if (courtEndTimes.isNotEmpty() && courtEndTimes.peek().first <= booking.startTime) {
            // Reuse the court that freed up earliest
            val (_, courtId) = courtEndTimes.poll()
            assignments.add(CourtAssignment(booking.id, courtId))
            courtEndTimes.offer(Pair(booking.finishTime, courtId))
        } else {
            // Need a new court
            assignments.add(CourtAssignment(booking.id, nextCourtId))
            courtEndTimes.offer(Pair(booking.finishTime, nextCourtId))
            nextCourtId++
        }
    }
    
    return assignments
}
```

### Python Implementation

```python
import heapq
from dataclasses import dataclass
from typing import List

@dataclass
class BookingRecord:
    id: int
    start_time: int
    finish_time: int

@dataclass
class CourtAssignment:
    booking_id: int
    court_id: int

def assign_courts(bookings: List[BookingRecord]) -> List[CourtAssignment]:
    if not bookings:
        return []
    
    # Sort by start time
    sorted_bookings = sorted(bookings, key=lambda b: b.start_time)
    
    # Min heap: (end_time, court_id)
    court_heap = []
    assignments = []
    next_court_id = 1
    
    for booking in sorted_bookings:
        # Check if any court is free
        if court_heap and court_heap[0][0] <= booking.start_time:
            # Reuse court
            _, court_id = heapq.heappop(court_heap)
            assignments.append(CourtAssignment(booking.id, court_id))
            heapq.heappush(court_heap, (booking.finish_time, court_id))
        else:
            # New court needed
            assignments.append(CourtAssignment(booking.id, next_court_id))
            heapq.heappush(court_heap, (booking.finish_time, next_court_id))
            next_court_id += 1
    
    return assignments


# Usage
bookings = [
    BookingRecord(1, 0, 30),
    BookingRecord(2, 5, 10),
    BookingRecord(3, 15, 20)
]
assignments = assign_courts(bookings)
for a in assignments:
    print(f"Booking {a.booking_id} → Court {a.court_id}")
```

---

## Part B: With Maintenance Time After Each Booking

After each booking, a fixed amount of time `X` is needed to maintain the court before it can be rented again.

```kotlin
fun assignCourtsWithMaintenance(
    bookings: List<BookingRecord>,
    maintenanceTime: Int
): List<CourtAssignment> {
    if (bookings.isEmpty()) return emptyList()
    
    val sortedBookings = bookings.sortedBy { it.startTime }
    
    // Min heap: (availableTime, courtId)
    // availableTime = finishTime + maintenanceTime
    val courtAvailability = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
    
    val assignments = mutableListOf<CourtAssignment>()
    var nextCourtId = 1
    
    for (booking in sortedBookings) {
        // Court is available only after finish time + maintenance
        if (courtAvailability.isNotEmpty() && 
            courtAvailability.peek().first <= booking.startTime) {
            val (_, courtId) = courtAvailability.poll()
            assignments.add(CourtAssignment(booking.id, courtId))
            // Next available = finish + maintenance
            courtAvailability.offer(Pair(booking.finishTime + maintenanceTime, courtId))
        } else {
            assignments.add(CourtAssignment(booking.id, nextCourtId))
            courtAvailability.offer(Pair(booking.finishTime + maintenanceTime, nextCourtId))
            nextCourtId++
        }
    }
    
    return assignments
}
```

```python
def assign_courts_with_maintenance(
    bookings: List[BookingRecord],
    maintenance_time: int
) -> List[CourtAssignment]:
    if not bookings:
        return []
    
    sorted_bookings = sorted(bookings, key=lambda b: b.start_time)
    
    # (available_time, court_id)
    court_heap = []
    assignments = []
    next_court_id = 1
    
    for booking in sorted_bookings:
        if court_heap and court_heap[0][0] <= booking.start_time:
            _, court_id = heapq.heappop(court_heap)
            assignments.append(CourtAssignment(booking.id, court_id))
            heapq.heappush(court_heap, 
                (booking.finish_time + maintenance_time, court_id))
        else:
            assignments.append(CourtAssignment(booking.id, next_court_id))
            heapq.heappush(court_heap, 
                (booking.finish_time + maintenance_time, next_court_id))
            next_court_id += 1
    
    return assignments
```

---

## Part C: Maintenance After X Amount of Usage (Durability)

Court needs maintenance after `durability` bookings, with `maintenanceTime` duration.

```kotlin
data class Court(
    val id: Int,
    var usageCount: Int = 0,
    var availableAt: Int = 0
)

fun assignCourtsWithDurability(
    bookings: List<BookingRecord>,
    maintenanceTime: Int,
    durability: Int  // Maintenance needed after this many bookings
): List<CourtAssignment> {
    if (bookings.isEmpty()) return emptyList()
    
    val sortedBookings = bookings.sortedBy { it.startTime }
    
    // Min heap: (availableTime, courtId, usageCount)
    val courtHeap = PriorityQueue<Triple<Int, Int, Int>>(compareBy { it.first })
    val courtUsage = mutableMapOf<Int, Int>()  // courtId -> usage count
    
    val assignments = mutableListOf<CourtAssignment>()
    var nextCourtId = 1
    
    for (booking in sortedBookings) {
        var assignedCourtId: Int? = null
        var newAvailableTime: Int
        
        // Find an available court
        while (courtHeap.isNotEmpty() && courtHeap.peek().first <= booking.startTime) {
            val (availableTime, courtId, _) = courtHeap.poll()
            
            if (availableTime <= booking.startTime) {
                assignedCourtId = courtId
                break
            }
        }
        
        if (assignedCourtId != null) {
            // Reuse existing court
            val usage = courtUsage.getOrDefault(assignedCourtId, 0) + 1
            courtUsage[assignedCourtId] = usage
            
            // Check if maintenance is needed
            newAvailableTime = if (usage % durability == 0) {
                booking.finishTime + maintenanceTime  // Add maintenance time
            } else {
                booking.finishTime
            }
            
            assignments.add(CourtAssignment(booking.id, assignedCourtId))
            courtHeap.offer(Triple(newAvailableTime, assignedCourtId, usage))
        } else {
            // Need new court
            assignedCourtId = nextCourtId++
            courtUsage[assignedCourtId] = 1
            
            // First booking, check if durability is 1
            newAvailableTime = if (durability == 1) {
                booking.finishTime + maintenanceTime
            } else {
                booking.finishTime
            }
            
            assignments.add(CourtAssignment(booking.id, assignedCourtId))
            courtHeap.offer(Triple(newAvailableTime, assignedCourtId, 1))
        }
    }
    
    return assignments
}
```

```python
def assign_courts_with_durability(
    bookings: List[BookingRecord],
    maintenance_time: int,
    durability: int
) -> List[CourtAssignment]:
    if not bookings:
        return []
    
    sorted_bookings = sorted(bookings, key=lambda b: b.start_time)
    
    # (available_time, court_id, usage_count)
    court_heap = []
    court_usage = {}  # court_id -> usage_count
    assignments = []
    next_court_id = 1
    
    for booking in sorted_bookings:
        assigned_court_id = None
        
        # Find available court
        temp = []
        while court_heap:
            available_time, court_id, usage = heapq.heappop(court_heap)
            if available_time <= booking.start_time and assigned_court_id is None:
                assigned_court_id = court_id
            else:
                temp.append((available_time, court_id, usage))
        
        # Put back non-used courts
        for item in temp:
            heapq.heappush(court_heap, item)
        
        if assigned_court_id is not None:
            usage = court_usage.get(assigned_court_id, 0) + 1
            court_usage[assigned_court_id] = usage
            
            # Check if maintenance needed
            if usage % durability == 0:
                new_available = booking.finish_time + maintenance_time
            else:
                new_available = booking.finish_time
            
            assignments.append(CourtAssignment(booking.id, assigned_court_id))
            heapq.heappush(court_heap, (new_available, assigned_court_id, usage))
        else:
            # New court
            assigned_court_id = next_court_id
            next_court_id += 1
            court_usage[assigned_court_id] = 1
            
            new_available = (booking.finish_time + maintenance_time 
                           if durability == 1 else booking.finish_time)
            
            assignments.append(CourtAssignment(booking.id, assigned_court_id))
            heapq.heappush(court_heap, (new_available, assigned_court_id, 1))
    
    return assignments
```

---

## Part D: Just Count Minimum Courts (No Assignment)

Simplified version - just count, don't track assignments.

```kotlin
fun minCourtsNeeded(bookings: List<BookingRecord>): Int {
    if (bookings.isEmpty()) return 0
    
    // Extract and sort start/end times
    val events = mutableListOf<Pair<Int, Int>>()  // (time, type: 1=start, -1=end)
    
    for (booking in bookings) {
        events.add(Pair(booking.startTime, 1))   // Start event
        events.add(Pair(booking.finishTime, -1)) // End event
    }
    
    // Sort by time, end events before start events at same time
    events.sortWith(compareBy({ it.first }, { it.second }))
    
    var currentCourts = 0
    var maxCourts = 0
    
    for ((_, type) in events) {
        currentCourts += type
        maxCourts = maxOf(maxCourts, currentCourts)
    }
    
    return maxCourts
}
```

```python
def min_courts_needed(bookings: List[BookingRecord]) -> int:
    if not bookings:
        return 0
    
    events = []
    for b in bookings:
        events.append((b.start_time, 1))   # Start
        events.append((b.finish_time, -1))  # End
    
    # Sort: by time, then ends before starts
    events.sort(key=lambda x: (x[0], x[1]))
    
    current = 0
    max_courts = 0
    
    for _, event_type in events:
        current += event_type
        max_courts = max(max_courts, current)
    
    return max_courts
```

---

## Part E: Check if Two Bookings Conflict

```kotlin
fun bookingsConflict(b1: BookingRecord, b2: BookingRecord): Boolean {
    // Two intervals overlap if one starts before the other ends
    return b1.startTime < b2.finishTime && b2.startTime < b1.finishTime
}

// With maintenance time
fun bookingsConflictWithMaintenance(
    b1: BookingRecord, 
    b2: BookingRecord,
    maintenanceTime: Int
): Boolean {
    val b1EffectiveEnd = b1.finishTime + maintenanceTime
    val b2EffectiveEnd = b2.finishTime + maintenanceTime
    
    return b1.startTime < b2EffectiveEnd && b2.startTime < b1EffectiveEnd
}
```

```python
def bookings_conflict(b1: BookingRecord, b2: BookingRecord) -> bool:
    """Check if two bookings overlap"""
    return b1.start_time < b2.finish_time and b2.start_time < b1.finish_time

def bookings_conflict_with_maintenance(
    b1: BookingRecord, 
    b2: BookingRecord,
    maintenance_time: int
) -> bool:
    """Check conflict considering maintenance time"""
    b1_end = b1.finish_time + maintenance_time
    b2_end = b2.finish_time + maintenance_time
    return b1.start_time < b2_end and b2.start_time < b1_end
```

---

## Complexity Analysis

| Part | Time | Space |
|------|------|-------|
| A: Basic | O(n log n) | O(n) |
| B: With Maintenance | O(n log n) | O(n) |
| C: With Durability | O(n log n) | O(n) |
| D: Count Only | O(n log n) | O(n) |
| E: Conflict Check | O(1) | O(1) |

---

## Complete Solution Class

```kotlin
class TennisCourtScheduler {
    
    data class BookingRecord(val id: Int, val startTime: Int, val finishTime: Int)
    data class CourtAssignment(val bookingId: Int, val courtId: Int)
    
    // Part A
    fun assignCourts(bookings: List<BookingRecord>) = 
        assignCourtsInternal(bookings, 0, Int.MAX_VALUE)
    
    // Part B
    fun assignCourtsWithMaintenance(bookings: List<BookingRecord>, maintenanceTime: Int) =
        assignCourtsInternal(bookings, maintenanceTime, Int.MAX_VALUE)
    
    // Part C
    fun assignCourtsWithDurability(
        bookings: List<BookingRecord>, 
        maintenanceTime: Int, 
        durability: Int
    ) = assignCourtsInternal(bookings, maintenanceTime, durability)
    
    private fun assignCourtsInternal(
        bookings: List<BookingRecord>,
        maintenanceTime: Int,
        durability: Int
    ): List<CourtAssignment> {
        // Implementation as shown in Part C
        // ... (full implementation above)
    }
    
    // Part D
    fun minCourtsNeeded(bookings: List<BookingRecord>): Int {
        // Line sweep implementation
    }
    
    // Part E
    fun bookingsConflict(b1: BookingRecord, b2: BookingRecord, maintenanceTime: Int = 0): Boolean {
        val b1End = b1.finishTime + maintenanceTime
        val b2End = b2.finishTime + maintenanceTime
        return b1.startTime < b2End && b2.startTime < b1End
    }
}
```

