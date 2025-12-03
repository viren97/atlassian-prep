# Driver Best Lap Time - LLD

## Problem Statement
You are given continuous lap-time data in the form of pairs: (driver_id, lap_time).

Each time a driver's lap time is recorded, you must automatically infer their lap number (1st, 2nd, etc.), calculate their performance improvement, and determine which driver showed the most significant progress on the latest lap.

---

## Example

### Input Stream:
```
("Driver1", 100)
("Driver2", 90)
("Driver3", 70)
("Driver1", 110)
("Driver2", 95)
("Driver3", 50)
```

### Computation:
- Driver1: Lap1 = 100, Lap2 = 110 → average(100) = 100 → delta = 100–110 = -10 (no improvement)
- Driver2: Lap1 = 90, Lap2 = 95 → average(90) = 90 → delta = 90–95 = -5 (no improvement)
- Driver3: Lap1 = 70, Lap2 = 50 → average(70) = 70 → delta = 70–50 = +20 (improvement)

### Output:
```
Champion on last lap: ("Driver3", 20)
```

**Explanation:** Driver3 improved by 20 seconds over their previous average and had the most significant positive delta among all drivers on the last lap.

---

## Code Flow Walkthrough

### `addLapTime(driverId, lapTime)` - Record Lap

```
CALL: tracker.addLapTime("Hamilton", 95.5)

STEP 1: Get or Create Driver Stats
├── stats = driverStats.getOrPut("Hamilton") { DriverStats("Hamilton") }
└── New drivers start with empty lap history

STEP 2: Calculate Delta (Improvement)
├── stats.addLapTime(95.5):
│   ├── lock.write { ... }  // Thread-safe
│   ├── 
│   ├── // Calculate previous average
│   ├── IF lapTimes.isEmpty():
│   │   └── previousAverage = 95.5 (first lap = baseline)
│   ├── ELSE:
│   │   └── previousAverage = lapTimes.average()  // e.g., 98.0
│   ├── 
│   ├── // Record lap time
│   ├── lapTimes.add(95.5)
│   ├── 
│   ├── // Calculate delta
│   ├── delta = previousAverage - currentLap
│   ├── delta = 98.0 - 95.5 = +2.5 seconds
│   │   ├── Positive = FASTER than average (improvement!)
│   │   └── Negative = SLOWER than average
│   ├── 
│   ├── lastDelta = 2.5
│   └── Return 2.5

STEP 3: Return Result
└── Return LapResult(
        driverId = "Hamilton",
        lapTime = 95.5,
        delta = +2.5,
        isImprovement = true
    )

DELTA INTERPRETATION:
├── delta > 0: Faster than average → improving
├── delta = 0: Same as average → consistent
└── delta < 0: Slower than average → declining
```

### `getBestProgressingDriver()` - Find Most Improved

```
CALL: tracker.getBestProgressingDriver()

STEP 1: Calculate Progress Score for Each Driver
├── FOR each driver in driverStats:
│   ├── 
│   ├── // Method 1: Average of recent deltas
│   ├── recentDeltas = lastNDeltas(5)
│   ├── progressScore = recentDeltas.average()
│   ├── 
│   ├── // Method 2: Trend analysis
│   ├── OR progressScore = calculateTrend(lapTimes)
│   │   ├── Use linear regression
│   │   └── Negative slope = getting faster
│   ├── 
│   └── Store (driver, progressScore)

STEP 2: Find Maximum Progress
├── Sort by progressScore (descending)
├── Return driver with highest score
└── Ties: return first (or use secondary criteria)

EXAMPLE:
├── Hamilton: deltas=[+2.5, +1.0, +0.5, -0.2, +0.8] → avg=+0.92
├── Verstappen: deltas=[+0.3, +0.4, +0.6, +0.5, +0.7] → avg=+0.50
├── Leclerc: deltas=[-0.5, +1.5, +2.0, +1.0, +0.5] → avg=+0.90
└── Winner: Hamilton (+0.92 avg improvement)
```

### Lap Time Tracking with Best Lap

```
SCENARIO: Track best lap time across session

addLapTime("Hamilton", 98.5):
├── lapTimes=[98.5], bestLap=98.5, delta=0

addLapTime("Hamilton", 96.0):
├── lapTimes=[98.5, 96.0]
├── avg=98.5, delta=98.5-96.0=+2.5
├── bestLap=min(98.5, 96.0)=96.0
└── New best lap!

addLapTime("Hamilton", 97.0):
├── lapTimes=[98.5, 96.0, 97.0]
├── avg=97.25, delta=97.25-97.0=+0.25
└── bestLap=96.0 (unchanged)

addLapTime("Hamilton", 95.2):
├── lapTimes=[98.5, 96.0, 97.0, 95.2]
├── avg=96.63, delta=96.63-95.2=+1.43
├── bestLap=95.2 (NEW BEST!)
└── Improvement of 0.8s from previous best

LEADERBOARD:
├── Best Laps:
│   ├── 1. Hamilton: 95.2
│   ├── 2. Verstappen: 95.8
│   └── 3. Leclerc: 96.1
├── Most Improved (this session):
│   ├── 1. Hamilton: +2.5 avg delta
│   └── 2. Leclerc: +1.8 avg delta
```

### Concurrent Update Handling

```
SCENARIO: Multiple threads updating same driver

THREAD 1: addLapTime("Hamilton", 95.0)
THREAD 2: addLapTime("Hamilton", 96.0)

WITH ReentrantReadWriteLock:
├── Thread 1 acquires write lock
├── Thread 2 waits...
├── Thread 1:
│   ├── Calculate average from current lapTimes
│   ├── Add 95.0 to lapTimes
│   ├── Calculate delta
│   └── Release lock
├── Thread 2 acquires lock
├── Thread 2:
│   ├── Calculate average (now includes 95.0)
│   ├── Add 96.0 to lapTimes
│   └── Calculate delta correctly
└── Both updates are accurate

WITHOUT LOCK (race condition):
├── Both threads read same average
├── Both add to lapTimes concurrently
├── Possible: one write lost
└── Deltas calculated incorrectly
```

---

## Requirements

### Functional Requirements
1. Record lap times for multiple drivers
2. Auto-increment lap number for each driver
3. Calculate improvement (delta from previous average)
4. Find driver with maximum improvement on the latest lap
5. Support querying driver statistics

### Non-Functional Requirements
1. Thread-safe for concurrent updates
2. Efficient O(1) recording of lap times
3. Efficient O(n) for finding best performer

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│            DriverStats                   │
├─────────────────────────────────────────┤
│ - driverId: String                      │
│ - lapTimes: List<Double>                │
│ - lastDelta: Double                     │
├─────────────────────────────────────────┤
│ + addLapTime(time: Double): Double      │
│ + getAverageLapTime(): Double           │
│ + getLapCount(): Int                    │
│ + getLastDelta(): Double                │
│ + getBestLapTime(): Double              │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│          RaceTracker                     │
├─────────────────────────────────────────┤
│ - drivers: Map<String, DriverStats>     │
│ - latestLapDrivers: Set<String>         │
├─────────────────────────────────────────┤
│ + recordLapTime(driverId, time)         │
│ + getChampionOfLatestLap(): Pair        │
│ + getDriverStats(driverId): DriverStats │
│ + getAllDriverStats(): List             │
│ + resetLatestLapTracking()              │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Data Classes

```kotlin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Driver Stats ====================

/**
 * Tracks lap times and performance metrics for a single driver.
 * 
 * === Improvement Calculation ===
 * Delta = previousAverage - currentLapTime
 * - Positive delta = improvement (faster than previous average)
 * - Negative delta = slower than previous average
 * - Zero delta = same as previous average
 * 
 * === Example ===
 * Lap 1: 100s → avg=100, delta=0 (no previous)
 * Lap 2: 95s  → avg=100, delta=100-95=+5 (improved!)
 * Lap 3: 90s  → avg=97.5, delta=97.5-90=+7.5 (improved more!)
 * Lap 4: 100s → avg=95, delta=95-100=-5 (got slower)
 * 
 * === Thread Safety ===
 * Uses ReentrantReadWriteLock for concurrent access.
 * Multiple threads can read stats, writes are exclusive.
 * 
 * Time Complexity:
 * - addLapTime: O(n) due to average calculation
 * - getAverageLapTime: O(n)
 * - getBestLapTime: O(n)
 * 
 * Could optimize with running sum for O(1) average.
 */
class DriverStats(
    val driverId: String
) {
    private val lapTimes = mutableListOf<Double>()
    private var lastDelta: Double = 0.0
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Add a lap time and return the improvement delta.
     * 
     * Formula: delta = previousAverage - currentLapTime
     * - Positive delta = improvement (faster)
     * - Negative delta = slower
     * 
     * @param lapTime The lap time in seconds
     * @return The improvement delta (positive = faster than average)
     */
    fun addLapTime(lapTime: Double): Double {
        lock.write {
            // Calculate previous average before adding new lap
            val previousAverage = if (lapTimes.isEmpty()) {
                lapTime // First lap: use itself as baseline
            } else {
                lapTimes.average()
            }
            
            // Record the lap time
            lapTimes.add(lapTime)
            
            // Calculate delta: previous average - current time
            // If previous avg was 100 and current is 95:
            //   delta = 100 - 95 = +5 (5 seconds faster = improvement)
            lastDelta = previousAverage - lapTime
            
            return lastDelta
        }
    }
    
    fun getAverageLapTime(): Double = lock.read {
        if (lapTimes.isEmpty()) 0.0 else lapTimes.average()
    }
    
    fun getLapCount(): Int = lock.read { lapTimes.size }
    
    fun getLastDelta(): Double = lock.read { lastDelta }
    
    fun getBestLapTime(): Double = lock.read {
        lapTimes.minOrNull() ?: Double.MAX_VALUE
    }
    
    fun getWorstLapTime(): Double = lock.read {
        lapTimes.maxOrNull() ?: 0.0
    }
    
    fun getAllLapTimes(): List<Double> = lock.read { lapTimes.toList() }
    
    fun getLastLapTime(): Double? = lock.read { lapTimes.lastOrNull() }
    
    override fun toString(): String = lock.read {
        "DriverStats($driverId, laps=${lapTimes.size}, avg=${String.format("%.2f", getAverageLapTime())}, lastDelta=${String.format("%.2f", lastDelta)})"
    }
}
```

### Race Tracker

```kotlin
// ==================== Race Tracker ====================

class RaceTracker {
    private val drivers = ConcurrentHashMap<String, DriverStats>()
    private val latestLapDrivers = ConcurrentHashMap.newKeySet<String>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Record a lap time for a driver.
     * Returns the improvement delta for this lap.
     */
    fun recordLapTime(driverId: String, lapTime: Double): Double {
        val stats = drivers.computeIfAbsent(driverId) { DriverStats(it) }
        val delta = stats.addLapTime(lapTime)
        
        lock.write {
            latestLapDrivers.add(driverId)
        }
        
        return delta
    }
    
    /**
     * Get the driver with the most improvement on the latest lap.
     * Returns (driverId, improvement) or null if no recent laps.
     */
    fun getChampionOfLatestLap(): Pair<String, Double>? {
        lock.read {
            if (latestLapDrivers.isEmpty()) return null
            
            var champion: String? = null
            var maxImprovement = Double.NEGATIVE_INFINITY
            
            for (driverId in latestLapDrivers) {
                val stats = drivers[driverId] ?: continue
                val delta = stats.getLastDelta()
                
                if (delta > maxImprovement) {
                    maxImprovement = delta
                    champion = driverId
                }
            }
            
            return champion?.let { Pair(it, maxImprovement) }
        }
    }
    
    /**
     * Get champion considering only positive improvements.
     * Returns null if no driver improved.
     */
    fun getChampionWithPositiveImprovement(): Pair<String, Double>? {
        val champion = getChampionOfLatestLap()
        return if (champion != null && champion.second > 0) champion else null
    }
    
    /**
     * Reset the latest lap tracking for a new round.
     */
    fun resetLatestLapTracking() {
        lock.write {
            latestLapDrivers.clear()
        }
    }
    
    fun getDriverStats(driverId: String): DriverStats? = drivers[driverId]
    
    fun getAllDriverStats(): Map<String, DriverStats> = drivers.toMap()
    
    fun getDriverCount(): Int = drivers.size
    
    /**
     * Get leaderboard sorted by best lap time.
     */
    fun getLeaderboardByBestTime(): List<Pair<String, Double>> {
        return drivers.map { (id, stats) -> Pair(id, stats.getBestLapTime()) }
            .sortedBy { it.second }
    }
    
    /**
     * Get leaderboard sorted by average lap time.
     */
    fun getLeaderboardByAverage(): List<Pair<String, Double>> {
        return drivers.map { (id, stats) -> Pair(id, stats.getAverageLapTime()) }
            .sortedBy { it.second }
    }
    
    /**
     * Get all drivers who improved on their latest lap.
     */
    fun getImprovedDrivers(): List<Pair<String, Double>> {
        return lock.read {
            latestLapDrivers
                .mapNotNull { id ->
                    drivers[id]?.let { stats ->
                        val delta = stats.getLastDelta()
                        if (delta > 0) Pair(id, delta) else null
                    }
                }
                .sortedByDescending { it.second }
        }
    }
}
```

### Advanced Features

```kotlin
// ==================== Lap Record ====================

data class LapRecord(
    val driverId: String,
    val lapNumber: Int,
    val lapTime: Double,
    val delta: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// ==================== Enhanced Race Tracker with History ====================

class RaceTrackerWithHistory {
    private val drivers = ConcurrentHashMap<String, DriverStats>()
    private val lapHistory = mutableListOf<LapRecord>()
    private val lock = ReentrantReadWriteLock()
    
    fun recordLapTime(driverId: String, lapTime: Double): LapRecord {
        val stats = drivers.computeIfAbsent(driverId) { DriverStats(it) }
        val delta = stats.addLapTime(lapTime)
        val lapNumber = stats.getLapCount()
        
        val record = LapRecord(driverId, lapNumber, lapTime, delta)
        
        lock.write {
            lapHistory.add(record)
        }
        
        return record
    }
    
    fun getChampionOfLastRound(): Pair<String, Double>? {
        lock.read {
            if (lapHistory.isEmpty()) return null
            
            // Find the last lap number recorded
            val lastRecord = lapHistory.lastOrNull() ?: return null
            val lastTimestamp = lastRecord.timestamp
            
            // Consider laps within last 100ms as same round
            val roundThreshold = 100
            val lastRoundLaps = lapHistory.filter { 
                lastTimestamp - it.timestamp < roundThreshold 
            }
            
            return lastRoundLaps
                .maxByOrNull { it.delta }
                ?.let { Pair(it.driverId, it.delta) }
        }
    }
    
    fun getLapHistory(): List<LapRecord> = lock.read { lapHistory.toList() }
    
    fun getDriverLapHistory(driverId: String): List<LapRecord> {
        return lock.read {
            lapHistory.filter { it.driverId == driverId }
        }
    }
}
```

### Usage Example

```kotlin
fun main() {
    val tracker = RaceTracker()
    
    println("=== Recording Lap Times ===")
    
    // First round of laps
    tracker.recordLapTime("Driver1", 100.0)
    tracker.recordLapTime("Driver2", 90.0)
    tracker.recordLapTime("Driver3", 70.0)
    
    println("After first round:")
    tracker.getAllDriverStats().forEach { (id, stats) ->
        println("  $id: ${stats.getLapCount()} laps, avg=${stats.getAverageLapTime()}")
    }
    
    // Reset for next round
    tracker.resetLatestLapTracking()
    
    // Second round of laps
    val delta1 = tracker.recordLapTime("Driver1", 110.0)
    val delta2 = tracker.recordLapTime("Driver2", 95.0)
    val delta3 = tracker.recordLapTime("Driver3", 50.0)
    
    println("\nSecond round deltas:")
    println("  Driver1: $delta1 (${if (delta1 > 0) "improved" else "slower"})")
    println("  Driver2: $delta2 (${if (delta2 > 0) "improved" else "slower"})")
    println("  Driver3: $delta3 (${if (delta3 > 0) "improved" else "slower"})")
    
    // Find champion
    val champion = tracker.getChampionOfLatestLap()
    println("\n=== Champion of Latest Lap ===")
    println("$champion")
    
    // Leaderboards
    println("\n=== Leaderboard (by best time) ===")
    tracker.getLeaderboardByBestTime().forEachIndexed { index, (id, time) ->
        println("  ${index + 1}. $id: $time")
    }
    
    println("\n=== Improved Drivers ===")
    tracker.getImprovedDrivers().forEach { (id, improvement) ->
        println("  $id: +$improvement improvement")
    }
}
```

### Output

```
=== Recording Lap Times ===
After first round:
  Driver1: 1 laps, avg=100.0
  Driver2: 1 laps, avg=90.0
  Driver3: 1 laps, avg=70.0

Second round deltas:
  Driver1: -10.0 (slower)
  Driver2: -5.0 (slower)
  Driver3: 20.0 (improved)

=== Champion of Latest Lap ===
(Driver3, 20.0)

=== Leaderboard (by best time) ===
  1. Driver3: 50.0
  2. Driver2: 90.0
  3. Driver1: 100.0

=== Improved Drivers ===
  Driver3: +20.0 improvement
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Observer** | Can add listeners for lap events | Notify UI of updates |
| **Strategy** | Improvement calculation logic | Different delta algorithms |
| **Factory** | DriverStats creation | Consistent object creation |

---

## Interview Discussion Points

### Q: How would you handle real-time streaming data?
**A:**
- Use message queue (Kafka) for lap time events
- Process in micro-batches for efficiency
- Use windowed aggregations for "latest lap" concept

### Q: How to handle ties (same improvement)?
**A:** Options include:
1. First to record wins
2. Better overall average wins
3. Return all tied drivers

### Q: How would you scale this for millions of drivers?
**A:**
- Partition by driver ID
- Use distributed cache (Redis) for stats
- Pre-compute leaderboards periodically

---

## Complexity Analysis

| Operation | Time Complexity |
|-----------|----------------|
| Record lap time | O(n) for average calculation |
| Get champion | O(d) where d = drivers |
| Get leaderboard | O(d log d) for sorting |

**Space Complexity:** O(d × l) where d = drivers, l = laps per driver

---

## Edge Cases

1. First lap (no previous average)
2. All drivers got slower
3. Single driver in race
4. Very large/small lap times
5. Concurrent updates from multiple sources

