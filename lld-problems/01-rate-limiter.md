# Rate Limiter - LLD

## Problem Statement
Design a Rate Limiter that can limit the number of requests a user/client can make to an API within a specified time window.

---

## Flow Diagrams

### High-Level Request Flow

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│  Client  │────▶│ API Gateway  │────▶│ Rate Limiter │────▶│  Server  │
└──────────┘     └──────────────┘     └──────────────┘     └──────────┘
                                              │
                                              ▼
                                      ┌──────────────┐
                                      │   Allowed?   │
                                      └──────────────┘
                                         │      │
                                    Yes ─┘      └─ No
                                    │              │
                                    ▼              ▼
                              ┌──────────┐   ┌────────────┐
                              │ Process  │   │ Return 429 │
                              │ Request  │   │ Too Many   │
                              └──────────┘   └────────────┘
```

### Token Bucket Algorithm Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      TOKEN BUCKET ALGORITHM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Request Arrives                                                    │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                    │
│   │ Check Bucket│                                                    │
│   │   Tokens    │                                                    │
│   └─────────────┘                                                    │
│         │                                                            │
│         ├── tokens > 0? ──Yes──▶ ┌─────────────┐                    │
│         │                        │ Consume 1   │                    │
│         │                        │   Token     │                    │
│         │                        └─────────────┘                    │
│         │                              │                            │
│         │                              ▼                            │
│         │                        ┌─────────────┐                    │
│         │                        │  ALLOWED ✓  │                    │
│         │                        └─────────────┘                    │
│         │                                                            │
│         └── tokens = 0? ──No───▶ ┌─────────────┐                    │
│                                  │ REJECTED ✗  │                    │
│                                  │  (429)      │                    │
│                                  └─────────────┘                    │
│                                                                      │
│   Background Process (every second):                                │
│   ┌────────────────────────────────────────┐                        │
│   │  Add tokens at refill rate             │                        │
│   │  tokens = min(tokens + rate, capacity) │                        │
│   └────────────────────────────────────────┘                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Sliding Window Algorithm Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SLIDING WINDOW ALGORITHM                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Timeline:  [-------- Window (60 sec) --------]                    │
│              │                                  │                    │
│   Requests:  ●  ●    ●  ●●●   ●    ●  ●   ●   │ ← New Request      │
│              │                                  │                    │
│              └─ Old requests (remove) ──────────┘                    │
│                                                                      │
│   Algorithm Steps:                                                   │
│   ┌────────────────────────────────────────────────────────────┐    │
│   │ 1. Get current timestamp                                    │    │
│   │ 2. Remove all requests older than (now - window_size)       │    │
│   │ 3. Count remaining requests in window                       │    │
│   │ 4. If count < max_requests → ALLOW, add timestamp           │    │
│   │    Else → REJECT (429)                                      │    │
│   └────────────────────────────────────────────────────────────┘    │
│                                                                      │
│   Request comes at T=65s (window=60s, max=5):                       │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │ Timestamps: [10, 25, 40, 55, 62]                              │  │
│   │                │   │   │   │   │                              │  │
│   │ Remove: T < 5  ✗   ✗                                         │  │
│   │ Keep: T >= 5           ✓   ✓   ✓                             │  │
│   │ Count = 3, max = 5 → ALLOWED ✓                               │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Fixed Window Algorithm Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                     FIXED WINDOW ALGORITHM                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Time:    0:00    1:00    2:00    3:00                             │
│            │       │       │       │                                 │
│   Windows: [  W1  ][  W2  ][  W3  ][ W4 ...                         │
│            │       │       │                                         │
│   Limit:   5 req   5 req   5 req                                    │
│                                                                      │
│   Algorithm:                                                         │
│   ┌────────────────────────────────────────────┐                    │
│   │ 1. Calculate window = timestamp / window_size                   │
│   │ 2. If new window → reset counter to 0      │                    │
│   │ 3. If counter < max → ALLOW, counter++     │                    │
│   │    Else → REJECT                           │                    │
│   └────────────────────────────────────────────┘                    │
│                                                                      │
│   Edge Case (Burst at window boundary):                             │
│   Window 1 end    Window 2 start                                    │
│        │               │                                             │
│   ●●●●●│               │●●●●● ← 10 requests in 2 seconds!           │
│        └───────────────┘                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                      │
│  ┌──────────┐        ┌─────────────────────┐                        │
│  │  Client  │───────▶│  RateLimiterManager │                        │
│  │ (API)    │        │                     │                        │
│  └──────────┘        │  ┌───────────────┐  │                        │
│       │              │  │ Endpoint Map  │  │                        │
│       │              │  │ /api/users →  │──┼───┐                    │
│       │              │  │ /api/orders → │  │   │                    │
│       │              │  └───────────────┘  │   │                    │
│       │              └─────────────────────┘   │                    │
│       │                                        │                    │
│       │              ┌─────────────────────────▼──────┐             │
│       │              │     RateLimiterFactory         │             │
│       │              │                                │             │
│       │              │  ┌─────────┐ ┌─────────────┐  │             │
│       │              │  │ Token   │ │  Sliding    │  │             │
│       │              │  │ Bucket  │ │  Window     │  │             │
│       │              │  └─────────┘ └─────────────┘  │             │
│       │              │  ┌─────────┐ ┌─────────────┐  │             │
│       │              │  │ Fixed   │ │   Leaky     │  │             │
│       │              │  │ Window  │ │   Bucket    │  │             │
│       │              │  └─────────┘ └─────────────┘  │             │
│       │              └───────────────────────────────┘             │
│       │                                                            │
│       ▼                                                            │
│  ┌─────────────┐                                                   │
│  │  Response   │                                                   │
│  │  200 OK  or │                                                   │
│  │  429 Error  │                                                   │
│  └─────────────┘                                                   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Code Flow Walkthrough

### Token Bucket: `isAllowed()` Step-by-Step

```
CALL: limiter.isAllowed("user-123")

STEP 1: Acquire Lock
├── lock.withLock { ... }
├── Purpose: Ensure thread-safe access to bucket state
└── Only one thread can modify bucket at a time

STEP 2: Get or Create Bucket
├── buckets.getOrPut("user-123") { Bucket(tokens=100, timestamp=now) }
├── IF bucket exists → return existing bucket
├── IF bucket doesn't exist → create new bucket with full capacity
└── New users start with full token allowance (fair start)

STEP 3: Lazy Token Refill
├── Calculate elapsed time: elapsedSeconds = (now - lastRefill) / 1000.0
├── Calculate tokens earned: tokensToAdd = elapsedSeconds * refillRate
├── Example: 5 seconds × 10 tokens/sec = 50 tokens earned
├── Update bucket: tokens = min(capacity, current + tokensToAdd)
├── Cap at capacity to prevent unlimited accumulation
└── Update timestamp for next calculation

STEP 4: Check Token Availability
├── IF tokens >= 1:
│   ├── Consume: tokens -= 1
│   ├── Return: RateLimitResult(allowed=true, remaining=tokens)
│   └── Request proceeds to server
├── ELSE (tokens < 1):
│   ├── Calculate wait time: (1 - tokens) / refillRate * 1000
│   ├── Example: Need 0.5 tokens, rate=10/sec → wait 50ms
│   ├── Return: RateLimitResult(allowed=false, retryAfter=50)
│   └── Client receives 429 Too Many Requests
```

### Sliding Window: `isAllowed()` Step-by-Step

```
CALL: limiter.isAllowed("user-123")
CONFIG: maxRequests=100, windowSizeMs=60000 (1 minute)

STEP 1: Acquire Lock
├── lock.withLock { ... }
└── Ensure atomic read-modify-write

STEP 2: Calculate Window Boundaries
├── now = System.currentTimeMillis()  // e.g., 1000060000
├── windowStart = now - windowSizeMs   // e.g., 1000000000
└── Only requests within [windowStart, now] count

STEP 3: Get Request Timestamps
├── timestamps = requestLogs.getOrPut("user-123") { mutableListOf() }
└── List stores timestamp of each request from this client

STEP 4: Clean Expired Timestamps
├── timestamps.removeAll { it < windowStart }
├── Example: [999990000, 1000010000, 1000050000]
├── After cleanup: [1000010000, 1000050000]
├── Time Complexity: O(k) where k = expired entries
└── Memory freed for old requests

STEP 5: Check Request Count
├── IF timestamps.size < maxRequests:
│   ├── timestamps.add(now)  // Record this request
│   ├── remaining = maxRequests - timestamps.size
│   └── Return: allowed=true
├── ELSE (at limit):
│   ├── Find oldest timestamp still in window
│   ├── Calculate when it expires: oldest + windowSize - now
│   └── Return: allowed=false, retryAfter=timeUntilOldestExpires
```

### Fixed Window: `isAllowed()` Step-by-Step

```
CALL: limiter.isAllowed("user-123")
CONFIG: maxRequests=100, windowSizeMs=60000

STEP 1: Determine Current Window
├── now = System.currentTimeMillis()  // e.g., 1000065000
├── currentWindow = now / windowSizeMs  // e.g., 16667 (window ID)
└── All requests in same minute share same window ID

STEP 2: Get or Reset Counter (Atomic Operation)
├── counters.compute("user-123") { key, existing ->
│   ├── existingWindow = existing?.windowStart / windowSizeMs
│   ├── IF existing == null OR existingWindow != currentWindow:
│   │   └── Create new: WindowCounter(windowStart=now, count=0)
│   └── ELSE: return existing counter
│ }
└── Atomic: prevents race condition between read and write

STEP 3: Increment and Check
├── currentCount = counter.count.incrementAndGet()  // Atomic +1
├── IF currentCount <= maxRequests:
│   └── Return: allowed=true, remaining=(max - count)
├── ELSE (over limit):
│   ├── counter.count.decrementAndGet()  // Undo the increment
│   ├── windowEnd = (currentWindow + 1) * windowSizeMs
│   ├── retryAfter = windowEnd - now
│   └── Return: allowed=false, retryAfter=timeUntilNextWindow
```

### Leaky Bucket: `isAllowed()` Step-by-Step

```
CALL: limiter.isAllowed("user-123")
CONFIG: bucketCapacity=100, leakRate=10/sec

CONCEPTUAL MODEL:
- Bucket fills with "water" (requests)
- Water leaks out at constant rate
- If bucket overflows → reject request

STEP 1: Acquire Lock
└── lock.withLock { ... }

STEP 2: Get or Create Bucket
├── bucket = buckets.getOrPut("user-123") {
│   LeakyBucket(waterLevel=0.0, lastLeakTimestamp=now)
│ }
└── New buckets start empty (unlike Token Bucket which starts full)

STEP 3: Leak Water (Process Previous Requests)
├── elapsedSeconds = (now - lastLeakTimestamp) / 1000.0
├── leaked = elapsedSeconds * leakRate
├── Example: 2 seconds × 10/sec = 20 units leaked
├── waterLevel = max(0, waterLevel - leaked)
├── Can't go below 0 (empty bucket)
└── Update timestamp

STEP 4: Try to Add Water (New Request)
├── IF waterLevel < capacity:
│   ├── waterLevel += 1  // Add this request
│   ├── remaining = capacity - waterLevel
│   └── Return: allowed=true
├── ELSE (bucket full):
│   ├── timeToLeak = (1 / leakRate) * 1000
│   ├── Example: 1/10 = 100ms to process one request
│   └── Return: allowed=false, retryAfter=100ms
```

### RateLimiterManager: Multi-Endpoint Flow

```
SETUP:
manager.registerEndpoint("/api/search", SLIDING_WINDOW, config1)
manager.registerEndpoint("/api/upload", TOKEN_BUCKET, config2)

CALL: manager.isAllowed("user-123", "/api/search")

STEP 1: Lookup Endpoint Limiter
├── limiter = endpointLimiters["/api/search"]
├── IF limiter == null:
│   └── throw IllegalArgumentException("Endpoint not registered")
└── Each endpoint has its own rate limiter instance

STEP 2: Create Composite Key
├── key = "user-123:/api/search"
└── Same user has separate limits per endpoint

STEP 3: Delegate to Limiter
├── result = limiter.isAllowed(key)
└── Returns RateLimitResult from specific algorithm

RESULT:
- User can hit /api/search 100 times/min (sliding window)
- User can hit /api/upload 10 times (token bucket)
- Limits are independent per endpoint
```

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

/**
 * Configuration for rate limiters.
 * 
 * @property maxRequests Maximum requests allowed in the window (for sliding/fixed window)
 * @property windowSizeMs Time window size in milliseconds (default: 1 minute)
 * @property bucketCapacity Maximum tokens in bucket (for token/leaky bucket)
 * @property refillRatePerSecond Tokens added per second (for token bucket) or 
 *                               requests processed per second (for leaky bucket)
 * 
 * Time Complexity: O(1) for creation
 * Space Complexity: O(1)
 */
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
 * Token Bucket Algorithm Implementation
 * 
 * === How It Works ===
 * - Each client has a "bucket" that holds tokens (like coins in a piggy bank)
 * - Tokens are added at a fixed rate (refillRatePerSecond)
 * - Each request consumes one token
 * - If no tokens available, request is rejected
 * - Bucket has max capacity to prevent unlimited accumulation
 * 
 * === Visual Representation ===
 *         ┌─────────────┐
 *    ────▶│ ● ● ● ● ●   │ ◀── Tokens refill over time
 *         │   BUCKET    │
 *         │ capacity=10 │
 *         └──────┬──────┘
 *                │
 *                ▼ consume 1 token per request
 *          [Request allowed if tokens > 0]
 * 
 * === Time Complexity ===
 * - isAllowed(): O(1) - constant time operations
 * - refillTokens(): O(1) - simple arithmetic
 * 
 * === Space Complexity ===
 * - O(n) where n = number of unique clients
 * - Each client stores: tokens (Double) + timestamp (Long) = ~16 bytes
 * 
 * === Pros ===
 * - Handles burst traffic well (can use saved tokens)
 * - Smooth rate limiting over time
 * - Memory efficient per client
 * 
 * === Cons ===
 * - Requires storing state per client
 * - Clock skew can affect accuracy in distributed systems
 */
class TokenBucketRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    /**
     * Represents a token bucket for a single client.
     * Uses "lazy refill" - tokens are calculated on-demand, not via background thread.
     */
    private data class Bucket(
        var tokens: Double,           // Current token count (can be fractional during refill)
        var lastRefillTimestamp: Long // Last time tokens were calculated/refilled
    )
    
    // ConcurrentHashMap for thread-safe client bucket storage
    // Each clientId maps to their own bucket
    private val buckets = ConcurrentHashMap<String, Bucket>()
    
    // ReentrantLock ensures atomic read-modify-write operations on bucket state
    private val lock = ReentrantLock()
    
    /**
     * Check if a request from the given client should be allowed.
     * 
     * Algorithm Steps:
     * 1. Get or create bucket for client (new clients start with full bucket)
     * 2. Refill tokens based on time elapsed since last request
     * 3. If tokens >= 1: consume token and allow request
     * 4. If tokens < 1: reject and calculate retry time
     * 
     * @param clientId Unique identifier for the client (e.g., IP, user ID, API key)
     * @return RateLimitResult containing allowed status and retry information
     * 
     * Thread Safety: Uses ReentrantLock to ensure atomic operations
     * Time Complexity: O(1)
     */
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            // Get existing bucket or create new one with full capacity
            // New clients get the benefit of full tokens initially
            val bucket = buckets.getOrPut(clientId) {
                Bucket(
                    tokens = config.bucketCapacity.toDouble(),
                    lastRefillTimestamp = System.currentTimeMillis()
                )
            }
            
            // Lazy refill: Calculate tokens that should have been added
            // since the last request (instead of using a background thread)
            refillTokens(bucket)
            
            return if (bucket.tokens >= 1) {
                // Consume one token for this request
                bucket.tokens -= 1
                RateLimitResult(
                    allowed = true,
                    remainingRequests = bucket.tokens.toInt()
                )
            } else {
                // Calculate how long client should wait before retrying
                // Formula: (tokens_needed / refill_rate) * 1000ms
                // tokens_needed = 1 - current_tokens (we need at least 1 token)
                val waitTimeMs = ((1 - bucket.tokens) / config.refillRatePerSecond * 1000).toLong()
                RateLimitResult(
                    allowed = false,
                    remainingRequests = 0,
                    retryAfterMs = waitTimeMs
                )
            }
        }
    }
    
    /**
     * Refill tokens in the bucket based on elapsed time.
     * 
     * This implements "lazy refill" - instead of a background thread adding
     * tokens periodically, we calculate how many tokens should have been
     * added since the last check.
     * 
     * Formula: tokensToAdd = elapsedSeconds * refillRatePerSecond
     * 
     * Example: If 2.5 seconds passed and refillRate is 10 tokens/sec:
     *          tokensToAdd = 2.5 * 10 = 25 tokens
     * 
     * The bucket is capped at bucketCapacity to prevent unlimited accumulation.
     * 
     * @param bucket The bucket to refill
     */
    private fun refillTokens(bucket: Bucket) {
        val now = System.currentTimeMillis()
        
        // Calculate time elapsed since last refill (in seconds)
        val elapsedSeconds = (now - bucket.lastRefillTimestamp) / 1000.0
        
        // Calculate tokens to add: time * rate
        val tokensToAdd = elapsedSeconds * config.refillRatePerSecond
        
        // Add tokens but cap at bucket capacity (use minOf to enforce ceiling)
        bucket.tokens = minOf(
            config.bucketCapacity.toDouble(),  // Maximum allowed
            bucket.tokens + tokensToAdd         // Current + earned
        )
        
        // Update timestamp for next calculation
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

