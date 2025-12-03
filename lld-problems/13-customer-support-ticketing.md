# Customer Support Ticketing System - LLD

## Problem Statement
Design a customer support ticketing system that allows customers to rate support agents. The system should:
1. Accept ratings for agents (out of 5)
2. Show all agents with their average ratings, ordered from highest to lowest
3. Find best agents for each month
4. Handle tie-breakers appropriately
5. Support exporting data in various formats

---

## Requirements

### Functional Requirements
1. Add rating for a support agent
2. Get all agents sorted by average rating (descending)
3. Get best agents for a specific month
4. Handle tie-breakers:
   - By number of ratings received
   - By lexicographically smallest name
5. Export agent ratings (CSV, JSON, XML)

### Non-Functional Requirements
1. Thread-safe operations
2. Efficient sorting and retrieval
3. Extensible for new export formats

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│              Rating                      │
├─────────────────────────────────────────┤
│ - agentId: String                       │
│ - score: Int (1-5)                      │
│ - timestamp: Instant                    │
│ - customerId: String?                   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│            AgentStats                    │
├─────────────────────────────────────────┤
│ - agentId: String                       │
│ - ratings: List<Rating>                 │
├─────────────────────────────────────────┤
│ + addRating(score: Int)                 │
│ + getAverageRating(): Double            │
│ + getRatingCount(): Int                 │
│ + getMonthlyAverage(month): Double      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│        TicketingSystem                   │
├─────────────────────────────────────────┤
│ - agents: Map<String, AgentStats>       │
├─────────────────────────────────────────┤
│ + addRating(agentId, score)             │
│ + getAgentsByRating(): List<AgentStats> │
│ + getBestAgentsForMonth(month): List    │
│ + exportRatings(format): String         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│    <<interface>> RatingsExporter         │
├─────────────────────────────────────────┤
│ + export(agents: List<AgentStats>): Str │
└─────────────────────────────────────────┘
         △
         │
    ┌────┼────┬─────────┐
    │    │    │         │
  CSV   JSON  XML    Custom
```

---

## Kotlin Implementation

### Core Data Classes

```kotlin
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Rating ====================

data class Rating(
    val agentId: String,
    val score: Int,
    val timestamp: Instant = Instant.now(),
    val customerId: String? = null
) {
    init {
        require(score in 1..5) { "Rating must be between 1 and 5" }
    }
    
    fun getYearMonth(): YearMonth {
        return YearMonth.from(timestamp.atZone(ZoneId.systemDefault()))
    }
}

// ==================== Agent Stats ====================

class AgentStats(
    val agentId: String
) {
    private val ratings = mutableListOf<Rating>()
    private val lock = ReentrantReadWriteLock()
    
    fun addRating(score: Int, customerId: String? = null): Rating {
        lock.write {
            val rating = Rating(agentId, score, customerId = customerId)
            ratings.add(rating)
            return rating
        }
    }
    
    fun getAverageRating(): Double = lock.read {
        if (ratings.isEmpty()) 0.0 else ratings.map { it.score }.average()
    }
    
    fun getRatingCount(): Int = lock.read { ratings.size }
    
    fun getTotalScore(): Int = lock.read { ratings.sumOf { it.score } }
    
    fun getMonthlyAverage(yearMonth: YearMonth): Double = lock.read {
        val monthlyRatings = ratings.filter { it.getYearMonth() == yearMonth }
        if (monthlyRatings.isEmpty()) 0.0 else monthlyRatings.map { it.score }.average()
    }
    
    fun getMonthlyRatingCount(yearMonth: YearMonth): Int = lock.read {
        ratings.count { it.getYearMonth() == yearMonth }
    }
    
    fun getAllRatings(): List<Rating> = lock.read { ratings.toList() }
    
    fun getRatingsForMonth(yearMonth: YearMonth): List<Rating> = lock.read {
        ratings.filter { it.getYearMonth() == yearMonth }
    }
    
    override fun toString(): String = lock.read {
        "AgentStats($agentId, avg=${String.format("%.2f", getAverageRating())}, count=${ratings.size})"
    }
}
```

### Agent Comparator with Tie-Breaking

```kotlin
// ==================== Agent Comparator ====================

/**
 * Compares agents by:
 * 1. Average rating (descending)
 * 2. Number of ratings (descending) - more ratings = more reliable
 * 3. Agent name (ascending) - lexicographically smallest
 */
class AgentComparator(
    private val yearMonth: YearMonth? = null // null for all-time
) : Comparator<AgentStats> {
    
    override fun compare(a1: AgentStats, a2: AgentStats): Int {
        val avg1 = if (yearMonth != null) a1.getMonthlyAverage(yearMonth) else a1.getAverageRating()
        val avg2 = if (yearMonth != null) a2.getMonthlyAverage(yearMonth) else a2.getAverageRating()
        
        // First: compare by average rating (descending)
        val avgComparison = avg2.compareTo(avg1)
        if (avgComparison != 0) return avgComparison
        
        // Second: compare by rating count (descending)
        val count1 = if (yearMonth != null) a1.getMonthlyRatingCount(yearMonth) else a1.getRatingCount()
        val count2 = if (yearMonth != null) a2.getMonthlyRatingCount(yearMonth) else a2.getRatingCount()
        
        val countComparison = count2.compareTo(count1)
        if (countComparison != 0) return countComparison
        
        // Third: compare by name (ascending)
        return a1.agentId.compareTo(a2.agentId)
    }
}
```

### Ticketing System

```kotlin
// ==================== Ticketing System ====================

class TicketingSystem {
    private val agents = ConcurrentHashMap<String, AgentStats>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Add a rating for an agent.
     */
    fun addRating(agentId: String, score: Int, customerId: String? = null): Rating {
        val stats = agents.computeIfAbsent(agentId) { AgentStats(it) }
        return stats.addRating(score, customerId)
    }
    
    /**
     * Get all agents sorted by average rating (descending).
     * Uses tie-breaker logic.
     */
    fun getAgentsByRating(): List<AgentStats> {
        return agents.values
            .filter { it.getRatingCount() > 0 }
            .sortedWith(AgentComparator())
    }
    
    /**
     * Get best agents for a specific month.
     */
    fun getBestAgentsForMonth(yearMonth: YearMonth): List<AgentStats> {
        return agents.values
            .filter { it.getMonthlyRatingCount(yearMonth) > 0 }
            .sortedWith(AgentComparator(yearMonth))
    }
    
    /**
     * Get best agents for a specific month (by year and month number).
     */
    fun getBestAgentsForMonth(year: Int, month: Int): List<AgentStats> {
        return getBestAgentsForMonth(YearMonth.of(year, month))
    }
    
    /**
     * Get the single best agent for a month (or null if no ratings).
     */
    fun getBestAgentForMonth(yearMonth: YearMonth): AgentStats? {
        return getBestAgentsForMonth(yearMonth).firstOrNull()
    }
    
    /**
     * Get all unique months that have ratings.
     */
    fun getMonthsWithRatings(): List<YearMonth> {
        return agents.values
            .flatMap { it.getAllRatings() }
            .map { it.getYearMonth() }
            .distinct()
            .sorted()
    }
    
    fun getAgent(agentId: String): AgentStats? = agents[agentId]
    
    fun getAllAgents(): List<AgentStats> = agents.values.toList()
    
    /**
     * Export ratings in specified format.
     */
    fun exportRatings(exporter: RatingsExporter, yearMonth: YearMonth? = null): String {
        val agentsList = if (yearMonth != null) {
            getBestAgentsForMonth(yearMonth)
        } else {
            getAgentsByRating()
        }
        return exporter.export(agentsList, yearMonth)
    }
}
```

### Export Functionality

```kotlin
// ==================== Exporter Interface ====================

interface RatingsExporter {
    fun export(agents: List<AgentStats>, yearMonth: YearMonth? = null): String
}

// ==================== CSV Exporter ====================

class CsvExporter : RatingsExporter {
    override fun export(agents: List<AgentStats>, yearMonth: YearMonth?): String {
        val sb = StringBuilder()
        sb.appendLine("Agent ID,Average Rating,Rating Count")
        
        for (agent in agents) {
            val avg = if (yearMonth != null) {
                agent.getMonthlyAverage(yearMonth)
            } else {
                agent.getAverageRating()
            }
            val count = if (yearMonth != null) {
                agent.getMonthlyRatingCount(yearMonth)
            } else {
                agent.getRatingCount()
            }
            sb.appendLine("${agent.agentId},${String.format("%.2f", avg)},$count")
        }
        
        return sb.toString()
    }
}

// ==================== JSON Exporter ====================

class JsonExporter : RatingsExporter {
    override fun export(agents: List<AgentStats>, yearMonth: YearMonth?): String {
        val sb = StringBuilder()
        sb.appendLine("[")
        
        agents.forEachIndexed { index, agent ->
            val avg = if (yearMonth != null) {
                agent.getMonthlyAverage(yearMonth)
            } else {
                agent.getAverageRating()
            }
            val count = if (yearMonth != null) {
                agent.getMonthlyRatingCount(yearMonth)
            } else {
                agent.getRatingCount()
            }
            
            sb.append("  {")
            sb.append("\"agentId\": \"${agent.agentId}\", ")
            sb.append("\"averageRating\": ${String.format("%.2f", avg)}, ")
            sb.append("\"ratingCount\": $count")
            sb.append("}")
            if (index < agents.size - 1) sb.append(",")
            sb.appendLine()
        }
        
        sb.appendLine("]")
        return sb.toString()
    }
}

// ==================== XML Exporter ====================

class XmlExporter : RatingsExporter {
    override fun export(agents: List<AgentStats>, yearMonth: YearMonth?): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<agents>")
        
        for (agent in agents) {
            val avg = if (yearMonth != null) {
                agent.getMonthlyAverage(yearMonth)
            } else {
                agent.getAverageRating()
            }
            val count = if (yearMonth != null) {
                agent.getMonthlyRatingCount(yearMonth)
            } else {
                agent.getRatingCount()
            }
            
            sb.appendLine("  <agent>")
            sb.appendLine("    <id>${agent.agentId}</id>")
            sb.appendLine("    <averageRating>${String.format("%.2f", avg)}</averageRating>")
            sb.appendLine("    <ratingCount>$count</ratingCount>")
            sb.appendLine("  </agent>")
        }
        
        sb.appendLine("</agents>")
        return sb.toString()
    }
}

// ==================== Monthly Report Exporter ====================

class MonthlyReportExporter : RatingsExporter {
    override fun export(agents: List<AgentStats>, yearMonth: YearMonth?): String {
        val sb = StringBuilder()
        val period = yearMonth?.format(DateTimeFormatter.ofPattern("MMMM yyyy")) ?: "All Time"
        
        sb.appendLine("=== Agent Ratings Report: $period ===")
        sb.appendLine()
        
        agents.forEachIndexed { index, agent ->
            val avg = if (yearMonth != null) {
                agent.getMonthlyAverage(yearMonth)
            } else {
                agent.getAverageRating()
            }
            val count = if (yearMonth != null) {
                agent.getMonthlyRatingCount(yearMonth)
            } else {
                agent.getRatingCount()
            }
            
            val stars = "★".repeat(avg.toInt()) + "☆".repeat(5 - avg.toInt())
            sb.appendLine("${index + 1}. ${agent.agentId}")
            sb.appendLine("   Rating: $stars (${String.format("%.2f", avg)}/5)")
            sb.appendLine("   Reviews: $count")
            sb.appendLine()
        }
        
        return sb.toString()
    }
}
```

### Usage Example

```kotlin
fun main() {
    val system = TicketingSystem()
    
    println("=== Adding Ratings ===")
    
    // Add ratings for multiple agents
    system.addRating("Alice", 5)
    system.addRating("Alice", 4)
    system.addRating("Alice", 5)
    
    system.addRating("Bob", 4)
    system.addRating("Bob", 4)
    system.addRating("Bob", 5)
    
    system.addRating("Charlie", 5)
    system.addRating("Charlie", 5)  // Same avg as Alice but fewer ratings
    
    system.addRating("Diana", 4)
    system.addRating("Diana", 5)
    system.addRating("Diana", 4)
    system.addRating("Diana", 5)
    system.addRating("Diana", 5)  // Same avg as Bob but more ratings
    
    // Get sorted agents
    println("\n=== Agents by Rating (with tie-breaker) ===")
    system.getAgentsByRating().forEachIndexed { index, agent ->
        println("${index + 1}. ${agent.agentId}: " +
                "avg=${String.format("%.2f", agent.getAverageRating())}, " +
                "count=${agent.getRatingCount()}")
    }
    
    // Export in different formats
    println("\n=== CSV Export ===")
    println(system.exportRatings(CsvExporter()))
    
    println("=== JSON Export ===")
    println(system.exportRatings(JsonExporter()))
    
    println("=== Monthly Report ===")
    println(system.exportRatings(MonthlyReportExporter()))
    
    // Monthly best
    val currentMonth = YearMonth.now()
    println("=== Best Agent for ${currentMonth} ===")
    val best = system.getBestAgentForMonth(currentMonth)
    println(best ?: "No ratings this month")
}
```

### Output

```
=== Adding Ratings ===

=== Agents by Rating (with tie-breaker) ===
1. Charlie: avg=5.00, count=2
2. Alice: avg=4.67, count=3
3. Diana: avg=4.60, count=5
4. Bob: avg=4.33, count=3

=== CSV Export ===
Agent ID,Average Rating,Rating Count
Charlie,5.00,2
Alice,4.67,3
Diana,4.60,5
Bob,4.33,3

=== JSON Export ===
[
  {"agentId": "Charlie", "averageRating": 5.00, "ratingCount": 2},
  {"agentId": "Alice", "averageRating": 4.67, "ratingCount": 3},
  {"agentId": "Diana", "averageRating": 4.60, "ratingCount": 5},
  {"agentId": "Bob", "averageRating": 4.33, "ratingCount": 3}
]

=== Monthly Report ===
=== Agent Ratings Report: All Time ===

1. Charlie
   Rating: ★★★★★ (5.00/5)
   Reviews: 2

2. Alice
   Rating: ★★★★☆ (4.67/5)
   Reviews: 3
...
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | `RatingsExporter` interface | Pluggable export formats |
| **Comparator** | `AgentComparator` | Custom sorting with tie-breakers |
| **Factory** | Agent creation | Consistent object instantiation |

---

## Interview Discussion Points

### Q: How to handle different tie-breaker strategies?
**A:** Make `AgentComparator` configurable:
```kotlin
enum class TieBreaker { BY_COUNT, BY_NAME, BY_TOTAL_SCORE }

class ConfigurableAgentComparator(
    private val tieBreakers: List<TieBreaker>
) : Comparator<AgentStats>
```

### Q: How to scale for millions of agents?
**A:**
- Use database with indexed queries
- Cache leaderboard and update incrementally
- Use approximate algorithms for real-time ranking

### Q: How to prevent rating fraud?
**A:**
- Rate limiting per customer
- Require verified ticket resolution
- Anomaly detection for unusual patterns

---

## Complexity Analysis

| Operation | Time Complexity |
|-----------|----------------|
| Add rating | O(1) |
| Get sorted agents | O(n log n) |
| Get monthly best | O(n log n) |
| Export | O(n) |

**Space Complexity:** O(a × r) where a = agents, r = ratings per agent

