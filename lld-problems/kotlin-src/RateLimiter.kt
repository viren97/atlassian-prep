/**
 * Rate Limiter - LLD Implementation
 * 
 * Design a Rate Limiter that can limit the number of requests a user/client 
 * can make to an API within a specified time window.
 * 
 * Algorithms implemented:
 * 1. Token Bucket - Handles burst traffic well
 * 2. Sliding Window - Most accurate, memory intensive
 * 3. Fixed Window - Simple, has boundary issues
 * 4. Leaky Bucket - Smooth output rate
 * 
 * Time Complexity: O(1) for all algorithms
 * Space Complexity: O(n) where n = number of clients
 */
package lld.ratelimiter

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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
 */
data class RateLimiterConfig(
    val maxRequests: Int = 100,
    val windowSizeMs: Long = 60_000, // 1 minute
    val bucketCapacity: Int = 100,
    val refillRatePerSecond: Int = 10
)

/**
 * Result of a rate limit check.
 * 
 * @property allowed Whether the request is allowed
 * @property remainingRequests Number of requests remaining in current window
 * @property retryAfterMs Milliseconds to wait before retrying (if not allowed)
 */
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

// ==================== Token Bucket Algorithm ====================

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
 * === Code Flow: isAllowed() ===
 * 1. Acquire lock (thread safety)
 * 2. Get or create bucket for client (new clients get full bucket)
 * 3. Lazy refill: calculate tokens earned since last check
 * 4. If tokens >= 1: consume token, return allowed
 * 5. If tokens < 1: calculate wait time, return rejected
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
     */
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            // Get existing bucket or create new one with full capacity
            val bucket = buckets.getOrPut(clientId) {
                Bucket(
                    tokens = config.bucketCapacity.toDouble(),
                    lastRefillTimestamp = System.currentTimeMillis()
                )
            }
            
            // Lazy refill: Calculate tokens that should have been added
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
     */
    private fun refillTokens(bucket: Bucket) {
        val now = System.currentTimeMillis()
        
        // Calculate time elapsed since last refill (in seconds)
        val elapsedSeconds = (now - bucket.lastRefillTimestamp) / 1000.0
        
        // Calculate tokens to add: time * rate
        val tokensToAdd = elapsedSeconds * config.refillRatePerSecond
        
        // Add tokens but cap at bucket capacity
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

// ==================== Sliding Window Algorithm ====================

/**
 * Sliding Window Log Algorithm
 * 
 * === How It Works ===
 * - Maintains timestamp of each request in a sliding time window
 * - Counts requests within the current window
 * - More accurate than fixed window (no boundary issues)
 * 
 * === Visual Representation ===
 * Timeline:  [-------- Window (60 sec) --------]
 *            │                                  │
 * Requests:  ●  ●    ●  ●●●   ●    ●  ●   ●   │ ← New Request
 *            │                                  │
 *            └─ Old requests (remove) ──────────┘
 * 
 * === Code Flow: isAllowed() ===
 * 1. Get current timestamp
 * 2. Remove all timestamps older than (now - window_size)
 * 3. Count remaining timestamps
 * 4. If count < max_requests → ALLOW, add timestamp
 * 5. Else → REJECT (429)
 * 
 * === Pros ===
 * - Very accurate, no boundary issues
 * - Precise rate limiting
 * 
 * === Cons ===
 * - Memory intensive (stores all timestamps)
 * - O(k) cleanup where k = expired entries
 */
class SlidingWindowRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    // Map of clientId to list of request timestamps
    private val requestLogs = ConcurrentHashMap<String, MutableList<Long>>()
    private val lock = ReentrantLock()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - config.windowSizeMs
            
            val timestamps = requestLogs.getOrPut(clientId) { mutableListOf() }
            
            // Remove expired timestamps (outside current window)
            timestamps.removeAll { it < windowStart }
            
            return if (timestamps.size < config.maxRequests) {
                timestamps.add(now)
                RateLimitResult(
                    allowed = true,
                    remainingRequests = config.maxRequests - timestamps.size
                )
            } else {
                // Calculate when oldest timestamp will expire
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

// ==================== Fixed Window Algorithm ====================

/**
 * Fixed Window Counter Algorithm
 * 
 * === How It Works ===
 * - Divides time into fixed windows (e.g., every minute)
 * - Counts requests per window
 * - Resets counter at window boundary
 * 
 * === Visual Representation ===
 * Time:    0:00    1:00    2:00    3:00
 *          │       │       │       │
 * Windows: [  W1  ][  W2  ][  W3  ][ W4 ...
 *          │       │       │
 * Limit:   5 req   5 req   5 req
 * 
 * === Edge Case (Burst at boundary) ===
 * Window 1 end    Window 2 start
 *      │               │
 * ●●●●●│               │●●●●● ← 10 requests in 2 seconds!
 *      └───────────────┘
 * 
 * === Pros ===
 * - Memory efficient (just a counter per client)
 * - Simple implementation
 * 
 * === Cons ===
 * - Burst at window boundaries
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
        
        // Atomic compute: get or reset counter
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
            counter.count.decrementAndGet() // Undo increment
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

// ==================== Leaky Bucket Algorithm ====================

/**
 * Leaky Bucket Algorithm
 * 
 * === How It Works ===
 * - Requests enter a queue (bucket)
 * - Processed at a fixed rate (leak rate)
 * - Excess requests overflow (rejected)
 * 
 * === Visual Representation ===
 *     Requests In (variable rate)
 *           │ │ │ │ │
 *           ▼ ▼ ▼ ▼ ▼
 *         ┌─────────┐
 *         │ ● ● ● ● │ ← Bucket (fills with requests)
 *         │ ● ● ●   │
 *         └────┬────┘
 *              │
 *              ▼ Leaks at fixed rate
 *         [Process request]
 * 
 * === Pros ===
 * - Smooth output rate
 * - Good for APIs needing constant rate
 * 
 * === Cons ===
 * - Doesn't handle bursts well
 */
class LeakyBucketRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {
    
    private data class LeakyBucket(
        var waterLevel: Double,       // Current "water" (pending requests) in bucket
        var lastLeakTimestamp: Long   // Last time we calculated leakage
    )
    
    private val buckets = ConcurrentHashMap<String, LeakyBucket>()
    private val lock = ReentrantLock()
    
    override fun isAllowed(clientId: String): RateLimitResult {
        lock.withLock {
            val now = System.currentTimeMillis()
            
            // New buckets start empty (unlike Token Bucket which starts full)
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
                // Bucket full - calculate time for one request to leak
                val timeToLeak = (1.0 / config.refillRatePerSecond * 1000).toLong()
                RateLimitResult(
                    allowed = false,
                    remainingRequests = 0,
                    retryAfterMs = timeToLeak
                )
            }
        }
    }
    
    /**
     * Leak water from bucket based on elapsed time.
     * Water leaks at refillRatePerSecond rate.
     */
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

// ==================== Factory Pattern ====================

/**
 * Factory for creating rate limiters.
 * 
 * Usage:
 *   val limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config)
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

// ==================== Rate Limiter Manager ====================

/**
 * Manages rate limiters for multiple endpoints.
 * Each endpoint can have different rate limit configurations.
 * 
 * Usage:
 *   manager.registerEndpoint("/api/search", SLIDING_WINDOW, searchConfig)
 *   manager.registerEndpoint("/api/upload", TOKEN_BUCKET, uploadConfig)
 *   
 *   val result = manager.isAllowed("user-123", "/api/search")
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
        return endpointLimiters.all { (endpoint, _) ->
            isAllowed(clientId, endpoint).allowed
        }
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Token Bucket Example ===")
    
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
    
    println("\n=== Multiple Endpoints Example ===")
    
    val manager = RateLimiterManager()
    
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
    
    val searchResult = manager.isAllowed("user-456", "/api/search")
    println("Search API: allowed=${searchResult.allowed}")
    
    val uploadResult = manager.isAllowed("user-456", "/api/upload")
    println("Upload API: allowed=${uploadResult.allowed}")
}

