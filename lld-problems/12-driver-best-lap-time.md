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

class DriverStats(
    val driverId: String
) {
    private val lapTimes = mutableListOf<Double>()
    private var lastDelta: Double = 0.0
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Add a lap time and return the improvement delta.
     * Delta = previousAverage - currentLapTime
     * Positive delta = improvement (faster)
     * Negative delta = slower
     */
    fun addLapTime(lapTime: Double): Double {
        lock.write {
            val previousAverage = if (lapTimes.isEmpty()) {
                lapTime // First lap, no improvement possible
            } else {
                lapTimes.average()
            }
            
            lapTimes.add(lapTime)
            
            // Delta = previous average - current time
            // Positive means improvement (faster than average)
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

