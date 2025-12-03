# Rate Limiter - LLD

## Problem Statement
Design a Rate Limiter that can limit the number of requests a user/client can make to an API within a specified time window.

---

## Requirements

### Functional Requirements
1. Limit requests per user/client to N requests per time window
2. Support multiple rate limiting algorithms (Token Bucket, Sliding Window, Fixed Window)
3. Return whether a request is allowed or should be throttled
4. Support different limits for different API endpoints

### Non-Functional Requirements
1. Low latency - O(1) for checking rate limit
2. Thread-safe for concurrent requests
3. Accurate rate limiting
4. Extensible for new algorithms

---

## Class Diagram

```
┌─────────────────────────────────────┐
│         <<interface>>               │
│          RateLimiter                │
├─────────────────────────────────────┤
│ + isAllowed(clientId: String): Bool │
│ + getRemainingRequests(): Int       │
└─────────────────────────────────────┘
              △
              │ implements
    ┌─────────┼─────────┬────────────────┐
    │         │         │                │
┌───┴───┐ ┌───┴───┐ ┌───┴───┐    ┌───────┴───────┐
│Token  │ │Sliding│ │Fixed  │    │   Leaky       │
│Bucket │ │Window │ │Window │    │   Bucket      │
│Limiter│ │Limiter│ │Limiter│    │   Limiter     │
└───────┘ └───────┘ └───────┘    └───────────────┘

┌─────────────────────────────────────┐
│       RateLimiterFactory            │
├─────────────────────────────────────┤
│ + create(type, config): RateLimiter │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       RateLimiterConfig             │
├─────────────────────────────────────┤
│ - maxRequests: Int                  │
│ - windowSizeMs: Long                │
│ - bucketCapacity: Int               │
│ - refillRate: Int                   │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       RateLimiterManager            │
├─────────────────────────────────────┤
│ - limiters: Map<String, RateLimiter>│
├─────────────────────────────────────┤
│ + isAllowed(clientId, endpoint): Bool│
│ + registerEndpoint(endpoint, config)│
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Interfaces and Data Classes

```kotlin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// ==================== Data Classes ====================

data class RateLimiterConfig(
    val maxRequests: Int = 100,
    val windowSizeMs: Long = 60_000, // 1 minute
    val bucketCapacity: Int = 100,
    val refillRatePerSecond: Int = 10
)

data class RateLimitResult(
    val allowed: Boolean,
    val remainingRequests: Int,
    val retryAfterMs: Long = 0
)

enum class RateLimiterType {
    TOKEN_BUCKET,
    SLIDING_WINDOW,
    FIXED_WINDOW,
    LEAKY_BUCKET
}

// ==================== Interface ====================

interface RateLimiter {
    fun isAllowed(clientId: String): RateLimitResult
    fun getRemainingRequests(clientId: String): Int
    fun reset(clientId: String)
}
```

### Token Bucket Algorithm

```kotlin
/**
 * Token Bucket Algorithm:
 * - Bucket holds tokens up to max capacity
 * - Tokens are added at a fixed rate
 * - Each request consumes one token
 * - Request allowed only if tokens available
 * 
 * Pros: Handles burst traffic well
 * Cons: Memory for storing bucket state per client
 */
class TokenBucketRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    private data class Bucket(
        var tokens: Double,
        var lastRefillTimestamp: Long
    )
    
    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val lock = ReentrantLock()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            val bucket = buckets.getOrPut(clientId) {
                Bucket(
                    tokens = config.bucketCapacity.toDouble(),
                    lastRefillTimestamp = System.currentTimeMillis()
                )
            }
            
            // Refill tokens based on time elapsed
            refillTokens(bucket)
            
            return if (bucket.tokens >= 1) {
                bucket.tokens -= 1
                RateLimitResult(
                    allowed = true,
                    remainingRequests = bucket.tokens.toInt()
                )
            } else {
                val waitTimeMs = ((1 - bucket.tokens) / config.refillRatePerSecond * 1000).toLong()
                RateLimitResult(
                    allowed = false,
                    remainingRequests = 0,
                    retryAfterMs = waitTimeMs
                )
            }
        }
    }
    
    private fun refillTokens(bucket: Bucket) {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - bucket.lastRefillTimestamp) / 1000.0
        val tokensToAdd = elapsedSeconds * config.refillRatePerSecond
        
        bucket.tokens = minOf(
            config.bucketCapacity.toDouble(),
            bucket.tokens + tokensToAdd
        )
        bucket.lastRefillTimestamp = now
    }
    
    override fun getRemainingRequests(clientId: String): Int {
        return buckets[clientId]?.tokens?.toInt() ?: config.bucketCapacity
    }
    
    override fun reset(clientId: String) {
        buckets.remove(clientId)
    }
}
```

### Sliding Window Algorithm

```kotlin
/**
 * Sliding Window Log Algorithm:
 * - Maintains timestamp of each request
 * - Counts requests within the sliding window
 * - More accurate than fixed window
 * 
 * Pros: Very accurate, no boundary issues
 * Cons: Memory intensive (stores all timestamps)
 */
class SlidingWindowRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    private val requestLogs = ConcurrentHashMap<String, MutableList<Long>>()
    private val lock = ReentrantLock()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - config.windowSizeMs
            
            val timestamps = requestLogs.getOrPut(clientId) { mutableListOf() }
            
            // Remove expired timestamps
            timestamps.removeAll { it < windowStart }
            
            return if (timestamps.size < config.maxRequests) {
                timestamps.add(now)
                RateLimitResult(
                    allowed = true,
                    remainingRequests = config.maxRequests - timestamps.size
                )
            } else {
                val oldestTimestamp = timestamps.minOrNull() ?: now
                val retryAfter = oldestTimestamp + config.windowSizeMs - now
                RateLimitResult(
                    allowed = false,
                    remainingRequests = 0,
                    retryAfterMs = maxOf(0, retryAfter)
                )
            }
        }
    }
    
    override fun getRemainingRequests(clientId: String): Int {
        val now = System.currentTimeMillis()
        val windowStart = now - config.windowSizeMs
        val timestamps = requestLogs[clientId] ?: return config.maxRequests
        val validCount = timestamps.count { it >= windowStart }
        return config.maxRequests - validCount
    }
    
    override fun reset(clientId: String) {
        requestLogs.remove(clientId)
    }
}
```

### Fixed Window Algorithm

```kotlin
/**
 * Fixed Window Counter Algorithm:
 * - Divides time into fixed windows
 * - Counts requests per window
 * - Resets counter at window boundary
 * 
 * Pros: Memory efficient, simple
 * Cons: Burst at window boundaries
 */
class FixedWindowRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    private data class WindowCounter(
        var windowStart: Long,
        var count: AtomicInteger
    )
    
    private val counters = ConcurrentHashMap<String, WindowCounter>()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        val now = System.currentTimeMillis()
        val currentWindow = now / config.windowSizeMs
        
        val counter = counters.compute(clientId) { _, existing ->
            val existingWindow = existing?.windowStart?.div(config.windowSizeMs)
            if (existing == null || existingWindow != currentWindow) {
                WindowCounter(now, AtomicInteger(0))
            } else {
                existing
            }
        }!!
        
        val currentCount = counter.count.incrementAndGet()
        
        return if (currentCount <= config.maxRequests) {
            RateLimitResult(
                allowed = true,
                remainingRequests = config.maxRequests - currentCount
            )
        } else {
            counter.count.decrementAndGet()
            val windowEnd = (currentWindow + 1) * config.windowSizeMs
            RateLimitResult(
                allowed = false,
                remainingRequests = 0,
                retryAfterMs = windowEnd - now
            )
        }
    }
    
    override fun getRemainingRequests(clientId: String): Int {
        val counter = counters[clientId] ?: return config.maxRequests
        return maxOf(0, config.maxRequests - counter.count.get())
    }
    
    override fun reset(clientId: String) {
        counters.remove(clientId)
    }
}
```

### Leaky Bucket Algorithm

```kotlin
/**
 * Leaky Bucket Algorithm:
 * - Requests enter a queue (bucket)
 * - Processed at a fixed rate (leak rate)
 * - Excess requests overflow (rejected)
 * 
 * Pros: Smooth output rate, good for APIs needing constant rate
 * Cons: Doesn't handle bursts well
 */
class LeakyBucketRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    private data class LeakyBucket(
        var waterLevel: Double,
        var lastLeakTimestamp: Long
    )
    
    private val buckets = ConcurrentHashMap<String, LeakyBucket>()
    private val lock = ReentrantLock()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            
            val bucket = buckets.getOrPut(clientId) {
                LeakyBucket(waterLevel = 0.0, lastLeakTimestamp = now)
            }
            
            // Leak water based on time elapsed
            leak(bucket, now)
            
            return if (bucket.waterLevel < config.bucketCapacity) {
                bucket.waterLevel += 1
                RateLimitResult(
                    allowed = true,
                    remainingRequests = (config.bucketCapacity - bucket.waterLevel).toInt()
                )
            } else {
                val timeToLeak = (1.0 / config.refillRatePerSecond * 1000).toLong()
                RateLimitResult(
                    allowed = false,
                    remainingRequests = 0,
                    retryAfterMs = timeToLeak
                )
            }
        }
    }
    
    private fun leak(bucket: LeakyBucket, now: Long) {
        val elapsedSeconds = (now - bucket.lastLeakTimestamp) / 1000.0
        val leaked = elapsedSeconds * config.refillRatePerSecond
        bucket.waterLevel = maxOf(0.0, bucket.waterLevel - leaked)
        bucket.lastLeakTimestamp = now
    }
    
    override fun getRemainingRequests(clientId: String): Int {
        val bucket = buckets[clientId] ?: return config.bucketCapacity
        return (config.bucketCapacity - bucket.waterLevel).toInt()
    }
    
    override fun reset(clientId: String) {
        buckets.remove(clientId)
    }
}
```

### Factory Pattern

```kotlin
/**
 * Factory for creating rate limiters
 * Demonstrates: Factory Pattern
 */
object RateLimiterFactory {
    
    fun create(type: RateLimiterType, config: RateLimiterConfig): RateLimiter {
        return when (type) {
            RateLimiterType.TOKEN_BUCKET -> TokenBucketRateLimiter(config)
            RateLimiterType.SLIDING_WINDOW -> SlidingWindowRateLimiter(config)
            RateLimiterType.FIXED_WINDOW -> FixedWindowRateLimiter(config)
            RateLimiterType.LEAKY_BUCKET -> LeakyBucketRateLimiter(config)
        }
    }
}
```

### Rate Limiter Manager (Multiple Endpoints)

```kotlin
/**
 * Manages rate limiters for multiple endpoints
 * Each endpoint can have different rate limit configurations
 */
class RateLimiterManager {
    
    private val endpointLimiters = ConcurrentHashMap<String, RateLimiter>()
    private val defaultConfig = RateLimiterConfig()
    private val defaultType = RateLimiterType.TOKEN_BUCKET
    
    fun registerEndpoint(
        endpoint: String,
        type: RateLimiterType = defaultType,
        config: RateLimiterConfig = defaultConfig
    ) {
        endpointLimiters[endpoint] = RateLimiterFactory.create(type, config)
    }
    
    fun isAllowed(clientId: String, endpoint: String): RateLimitResult {
        val limiter = endpointLimiters[endpoint]
            ?: throw IllegalArgumentException("Endpoint not registered: $endpoint")
        
        // Create composite key: clientId + endpoint
        val key = "$clientId:$endpoint"
        return limiter.isAllowed(key)
    }
    
    fun isAllowedGlobal(clientId: String): Boolean {
        // Check if client is allowed across ALL endpoints
        return endpointLimiters.all { (endpoint, _) ->
            isAllowed(clientId, endpoint).allowed
        }
    }
}
```

### Usage Example

```kotlin
fun main() {
    // Example 1: Simple Token Bucket
    val config = RateLimiterConfig(
        bucketCapacity = 10,
        refillRatePerSecond = 2
    )
    
    val limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config)
    
    // Simulate requests
    repeat(15) { i ->
        val result = limiter.isAllowed("user-123")
        println("Request ${i + 1}: allowed=${result.allowed}, remaining=${result.remainingRequests}")
    }
    
    println("\n--- Multiple Endpoints Example ---\n")
    
    // Example 2: Multiple Endpoints with different limits
    val manager = RateLimiterManager()
    
    // Register endpoints with different configs
    manager.registerEndpoint(
        endpoint = "/api/search",
        type = RateLimiterType.SLIDING_WINDOW,
        config = RateLimiterConfig(maxRequests = 100, windowSizeMs = 60_000)
    )
    
    manager.registerEndpoint(
        endpoint = "/api/upload",
        type = RateLimiterType.TOKEN_BUCKET,
        config = RateLimiterConfig(bucketCapacity = 5, refillRatePerSecond = 1)
    )
    
    // Test requests
    val searchResult = manager.isAllowed("user-456", "/api/search")
    println("Search API: allowed=${searchResult.allowed}")
    
    val uploadResult = manager.isAllowed("user-456", "/api/upload")
    println("Upload API: allowed=${uploadResult.allowed}")
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | Different rate limiting algorithms | Swap algorithms without changing client code |
| **Factory** | `RateLimiterFactory` | Centralize object creation |
| **Singleton** | `RateLimiterFactory` (object) | Single factory instance |

---

## Algorithm Comparison

| Algorithm | Burst Handling | Memory | Accuracy | Use Case |
|-----------|---------------|--------|----------|----------|
| Token Bucket | ✅ Excellent | Medium | Good | API rate limiting with bursts |
| Sliding Window | ❌ Poor | High | ✅ Excellent | Strict rate limiting |
| Fixed Window | ⚠️ Edge issues | ✅ Low | Good | Simple rate limiting |
| Leaky Bucket | ❌ Poor | Medium | Good | Constant rate processing |

---

## Interview Discussion Points

### Q: Why use Strategy pattern here?
**A:** The Strategy pattern allows us to:
1. Add new algorithms without modifying existing code (Open-Closed Principle)
2. Switch algorithms at runtime
3. Test each algorithm independently
4. Reuse the same interface across different implementations

### Q: How would you make this distributed?
**A:** For distributed rate limiting:
1. Use Redis for shared state (INCR with TTL for fixed window)
2. Use Redis Lua scripts for atomic operations
3. Consider eventual consistency vs strong consistency trade-offs
4. Use consistent hashing for partitioning

### Q: What about thread safety?
**A:** Multiple approaches:
1. `ConcurrentHashMap` for thread-safe maps
2. `ReentrantLock` for complex operations
3. `AtomicInteger/AtomicLong` for counters
4. Consider lock-free algorithms for high performance

### Q: How to handle rate limit headers in HTTP?
**A:** Standard headers:
- `X-RateLimit-Limit`: Max requests allowed
- `X-RateLimit-Remaining`: Requests left in window
- `X-RateLimit-Reset`: Time when limit resets
- `Retry-After`: Seconds to wait before retry

---

## Time & Space Complexity

| Algorithm | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Token Bucket | O(1) | O(n) where n = clients |
| Sliding Window | O(k) cleanup | O(n × k) where k = requests per window |
| Fixed Window | O(1) | O(n) |
| Leaky Bucket | O(1) | O(n) |

---

## Edge Cases to Handle

1. **Clock skew** in distributed systems
2. **Burst at window boundaries** (Fixed Window)
3. **Memory cleanup** for inactive clients
4. **Race conditions** in concurrent access
5. **Configuration hot reload**
6. **Graceful degradation** when rate limiter fails

