/**
 * In-Memory Cache - LLD Implementation
 * 
 * Design an in-memory cache system with support for:
 * - LRU (Least Recently Used) eviction
 * - LFU (Least Frequently Used) eviction
 * - TTL (Time-To-Live) expiration
 * - Thread-safe operations
 * 
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n) where n = capacity
 */
package lld.cache

import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Enums ====================

enum class EvictionPolicy {
    LRU,  // Least Recently Used
    LFU,  // Least Frequently Used
    FIFO, // First In First Out
    TTL   // Time To Live only
}

// ==================== Cache Entry ====================

/**
 * Wrapper for cached values with metadata.
 */
data class CacheEntry<V>(
    val value: V,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant? = null,
    var frequency: Int = 1,
    var lastAccessedAt: Instant = Instant.now()
) {
    fun isExpired(): Boolean {
        return expiresAt?.isBefore(Instant.now()) ?: false
    }
    
    fun updateAccess() {
        lastAccessedAt = Instant.now()
        frequency++
    }
}

// ==================== Cache Stats ====================

data class CacheStats(
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val size: Int,
    val maxSize: Int
) {
    val hitRate: Double 
        get() = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
}

// ==================== Cache Interface ====================

interface Cache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun put(key: K, value: V, ttl: Duration)
    fun remove(key: K): V?
    fun clear()
    fun size(): Int
    fun containsKey(key: K): Boolean
    fun keys(): Set<K>
    fun getStats(): CacheStats
}

// ==================== Doubly Linked List ====================

/**
 * Node for doubly linked list used in LRU cache.
 */
class DoublyLinkedListNode<K>(
    val key: K,
    var prev: DoublyLinkedListNode<K>? = null,
    var next: DoublyLinkedListNode<K>? = null
)

/**
 * Doubly linked list for maintaining LRU order.
 * 
 * Structure:
 * HEAD <-> node1 <-> node2 <-> node3 <-> TAIL
 * (MRU)                              (LRU)
 * 
 * Operations:
 * - addFirst: O(1) - Add to head (most recent)
 * - removeLast: O(1) - Remove from tail (evict LRU)
 * - remove: O(1) - Remove specific node
 * - moveToFirst: O(1) - Move accessed node to head
 */
class DoublyLinkedList<K> {
    private val head = DoublyLinkedListNode<K?>(null)
    private val tail = DoublyLinkedListNode<K?>(null)
    private var count = 0
    
    init {
        @Suppress("UNCHECKED_CAST")
        head.next = tail as DoublyLinkedListNode<K?>
        tail.prev = head
    }
    
    fun addFirst(key: K): DoublyLinkedListNode<K> {
        val node = DoublyLinkedListNode(key)
        
        @Suppress("UNCHECKED_CAST")
        val headNext = head.next as DoublyLinkedListNode<K>
        
        @Suppress("UNCHECKED_CAST")
        node.prev = head as DoublyLinkedListNode<K>
        node.next = headNext
        @Suppress("UNCHECKED_CAST")
        head.next = node as DoublyLinkedListNode<K?>
        headNext.prev = node
        
        count++
        return node
    }
    
    fun remove(node: DoublyLinkedListNode<K>) {
        val prevNode = node.prev
        val nextNode = node.next
        
        prevNode?.next = nextNode
        nextNode?.prev = prevNode
        
        node.prev = null
        node.next = null
        count--
    }
    
    fun removeLast(): K? {
        if (isEmpty()) return null
        
        @Suppress("UNCHECKED_CAST")
        val lastNode = tail.prev as? DoublyLinkedListNode<K> ?: return null
        
        if (lastNode == head) return null
        
        remove(lastNode)
        return lastNode.key
    }
    
    fun moveToFirst(node: DoublyLinkedListNode<K>) {
        remove(node)
        
        @Suppress("UNCHECKED_CAST")
        val headNext = head.next as DoublyLinkedListNode<K>
        
        @Suppress("UNCHECKED_CAST")
        node.prev = head as DoublyLinkedListNode<K>
        node.next = headNext
        @Suppress("UNCHECKED_CAST")
        head.next = node as DoublyLinkedListNode<K?>
        headNext.prev = node
        
        count++
    }
    
    fun isEmpty(): Boolean = count == 0
    
    fun size(): Int = count
}

// ==================== LRU Cache ====================

/**
 * LRU (Least Recently Used) Cache Implementation
 * 
 * === Data Structure ===
 * Uses HashMap + Doubly Linked List for O(1) all operations:
 * - HashMap: key → (node, entry) for O(1) lookup
 * - Doubly Linked List: maintains access order
 *   - Head: Most Recently Used (MRU)
 *   - Tail: Least Recently Used (LRU) - evict from here
 * 
 * === Visual Representation ===
 * 
 *   HashMap                    Doubly Linked List
 *   ┌─────┬──────────┐        
 *   │ "A" │ node_ref ├───────▶ HEAD ⟷ [A] ⟷ [B] ⟷ [C] ⟷ TAIL
 *   │ "B" │ node_ref │                         
 *   │ "C" │ node_ref │        MRU ◀──────────────▶ LRU
 *   └─────┴──────────┘        
 * 
 * === Code Flow: get(key) ===
 * 1. HashMap lookup: O(1)
 * 2. Check TTL expiration
 * 3. Move node to head (mark as recently used)
 * 4. Return value
 * 
 * === Code Flow: put(key, value) ===
 * 1. If exists: update value, move to head
 * 2. If at capacity: evict from tail
 * 3. Add new node at head
 * 4. Add to HashMap
 */
class LRUCache<K, V>(
    private val maxSize: Int,
    private val defaultTTL: Duration? = null
) : Cache<K, V> {
    
    private data class NodeEntry<K, V>(
        val node: DoublyLinkedListNode<K>,
        val entry: CacheEntry<V>
    )
    
    private val cache = HashMap<K, NodeEntry<K, V>>()
    private val accessOrder = DoublyLinkedList<K>()
    private val lock = ReentrantReadWriteLock()
    
    private var hits = 0L
    private var misses = 0L
    private var evictions = 0L
    
    override fun get(key: K): V? = lock.write {
        val nodeEntry = cache[key]
        
        if (nodeEntry == null) {
            misses++
            return null
        }
        
        // Check expiration
        if (nodeEntry.entry.isExpired()) {
            removeInternal(key)
            misses++
            return null
        }
        
        // Move to front (most recently used)
        accessOrder.moveToFirst(nodeEntry.node)
        nodeEntry.entry.updateAccess()
        
        hits++
        return nodeEntry.entry.value
    }
    
    override fun put(key: K, value: V) {
        val ttl = defaultTTL
        if (ttl != null) {
            put(key, value, ttl)
        } else {
            putInternal(key, value, null)
        }
    }
    
    override fun put(key: K, value: V, ttl: Duration) {
        putInternal(key, value, Instant.now().plus(ttl))
    }
    
    private fun putInternal(key: K, value: V, expiresAt: Instant?) = lock.write {
        // Remove existing entry if present
        cache[key]?.let { existing ->
            accessOrder.remove(existing.node)
        }
        
        // Evict if at capacity
        while (cache.size >= maxSize) {
            evict()
        }
        
        // Add new entry
        val node = accessOrder.addFirst(key)
        val entry = CacheEntry(value, expiresAt = expiresAt)
        cache[key] = NodeEntry(node, entry)
    }
    
    override fun remove(key: K): V? = lock.write {
        return removeInternal(key)
    }
    
    private fun removeInternal(key: K): V? {
        val nodeEntry = cache.remove(key) ?: return null
        accessOrder.remove(nodeEntry.node)
        return nodeEntry.entry.value
    }
    
    private fun evict() {
        val keyToRemove = accessOrder.removeLast()
        if (keyToRemove != null) {
            cache.remove(keyToRemove)
            evictions++
        }
    }
    
    override fun clear() = lock.write {
        cache.clear()
        while (!accessOrder.isEmpty()) {
            accessOrder.removeLast()
        }
    }
    
    override fun size(): Int = lock.read { cache.size }
    
    override fun containsKey(key: K): Boolean = lock.read { 
        val entry = cache[key]
        entry != null && !entry.entry.isExpired()
    }
    
    override fun keys(): Set<K> = lock.read { cache.keys.toSet() }
    
    override fun getStats(): CacheStats = lock.read {
        CacheStats(hits, misses, evictions, cache.size, maxSize)
    }
    
    fun cleanupExpired() = lock.write {
        val expiredKeys = cache.filter { it.value.entry.isExpired() }.keys
        expiredKeys.forEach { removeInternal(it) }
    }
}

// ==================== LFU Cache ====================

/**
 * LFU (Least Frequently Used) Cache Implementation
 * 
 * === Data Structure ===
 * Uses three HashMaps for O(1) operations:
 * - cache: key → CacheEntry (actual data)
 * - keyFrequency: key → access count
 * - frequencyBuckets: frequency → LinkedHashSet of keys
 * - minFrequency: tracks lowest frequency for O(1) eviction
 * 
 * === Visual Representation ===
 * 
 *   cache              keyFrequency      frequencyBuckets
 *   ┌─────┬───────┐    ┌─────┬─────┐    ┌─────┬─────────────────┐
 *   │ "A" │ val_A │    │ "A" │  3  │    │  1  │ {D} ← evict     │
 *   │ "B" │ val_B │    │ "B" │  2  │    │  2  │ {B, E}          │
 *   │ "C" │ val_C │    │ "C" │  3  │    │  3  │ {A, C}          │
 *   └─────┴───────┘    └─────┴─────┘    └─────┴─────────────────┘
 *                                       minFrequency = 1
 * 
 * === Code Flow: get(key) ===
 * 1. Lookup in cache
 * 2. Check TTL
 * 3. Update frequency: move from freq[n] to freq[n+1]
 * 4. Update minFrequency if needed
 * 5. Return value
 * 
 * === Code Flow: evict() ===
 * 1. Get bucket for minFrequency
 * 2. Remove oldest key from that bucket (LinkedHashSet preserves order)
 * 3. Remove from cache and keyFrequency
 */
class LFUCache<K, V>(
    private val maxSize: Int,
    private val defaultTTL: Duration? = null
) : Cache<K, V> {
    
    private val cache = HashMap<K, CacheEntry<V>>()
    private val keyFrequency = HashMap<K, Int>()
    private val frequencyBuckets = HashMap<Int, LinkedHashSet<K>>()
    private var minFrequency = 0
    
    private val lock = ReentrantReadWriteLock()
    
    private var hits = 0L
    private var misses = 0L
    private var evictions = 0L
    
    override fun get(key: K): V? = lock.write {
        val entry = cache[key]
        
        if (entry == null) {
            misses++
            return null
        }
        
        // Check expiration
        if (entry.isExpired()) {
            removeInternal(key)
            misses++
            return null
        }
        
        // Update frequency
        updateFrequency(key)
        entry.updateAccess()
        
        hits++
        return entry.value
    }
    
    override fun put(key: K, value: V) {
        val ttl = defaultTTL
        if (ttl != null) {
            put(key, value, ttl)
        } else {
            putInternal(key, value, null)
        }
    }
    
    override fun put(key: K, value: V, ttl: Duration) {
        putInternal(key, value, Instant.now().plus(ttl))
    }
    
    private fun putInternal(key: K, value: V, expiresAt: Instant?) = lock.write {
        if (maxSize <= 0) return
        
        // Update existing key
        if (cache.containsKey(key)) {
            cache[key] = CacheEntry(value, expiresAt = expiresAt)
            updateFrequency(key)
            return
        }
        
        // Evict if at capacity
        if (cache.size >= maxSize) {
            evict()
        }
        
        // Add new entry with frequency 1
        cache[key] = CacheEntry(value, expiresAt = expiresAt)
        keyFrequency[key] = 1
        frequencyBuckets.getOrPut(1) { LinkedHashSet() }.add(key)
        minFrequency = 1
    }
    
    /**
     * Update key's frequency: move from current bucket to next bucket.
     */
    private fun updateFrequency(key: K) {
        val currentFreq = keyFrequency[key] ?: return
        val newFreq = currentFreq + 1
        
        // Remove from current frequency bucket
        frequencyBuckets[currentFreq]?.remove(key)
        
        // If this was the min frequency bucket and it's now empty, increment min
        if (currentFreq == minFrequency && frequencyBuckets[currentFreq]?.isEmpty() == true) {
            minFrequency = newFreq
        }
        
        // Add to new frequency bucket
        keyFrequency[key] = newFreq
        frequencyBuckets.getOrPut(newFreq) { LinkedHashSet() }.add(key)
    }
    
    /**
     * Evict least frequently used key.
     * If tie, evict oldest (LinkedHashSet preserves insertion order).
     */
    private fun evict() {
        val minBucket = frequencyBuckets[minFrequency] ?: return
        val keyToEvict = minBucket.firstOrNull() ?: return
        
        minBucket.remove(keyToEvict)
        cache.remove(keyToEvict)
        keyFrequency.remove(keyToEvict)
        evictions++
    }
    
    override fun remove(key: K): V? = lock.write {
        return removeInternal(key)
    }
    
    private fun removeInternal(key: K): V? {
        val entry = cache.remove(key) ?: return null
        val freq = keyFrequency.remove(key) ?: return entry.value
        frequencyBuckets[freq]?.remove(key)
        return entry.value
    }
    
    override fun clear() = lock.write {
        cache.clear()
        keyFrequency.clear()
        frequencyBuckets.clear()
        minFrequency = 0
    }
    
    override fun size(): Int = lock.read { cache.size }
    
    override fun containsKey(key: K): Boolean = lock.read { 
        val entry = cache[key]
        entry != null && !entry.isExpired()
    }
    
    override fun keys(): Set<K> = lock.read { cache.keys.toSet() }
    
    override fun getStats(): CacheStats = lock.read {
        CacheStats(hits, misses, evictions, cache.size, maxSize)
    }
}

// ==================== Cache Builder ====================

/**
 * Fluent builder for cache creation.
 * 
 * Usage:
 *   val cache = buildCache<String, User> {
 *       maxSize(1000)
 *       evictionPolicy(EvictionPolicy.LRU)
 *       defaultTTL(Duration.ofMinutes(30))
 *   }
 */
class CacheBuilder<K, V> {
    private var maxSize: Int = 100
    private var evictionPolicy: EvictionPolicy = EvictionPolicy.LRU
    private var defaultTTL: Duration? = null
    
    fun maxSize(size: Int) = apply { this.maxSize = size }
    
    fun evictionPolicy(policy: EvictionPolicy) = apply { this.evictionPolicy = policy }
    
    fun defaultTTL(ttl: Duration) = apply { this.defaultTTL = ttl }
    
    fun build(): Cache<K, V> {
        return when (evictionPolicy) {
            EvictionPolicy.LRU -> LRUCache(maxSize, defaultTTL)
            EvictionPolicy.LFU -> LFUCache(maxSize, defaultTTL)
            EvictionPolicy.FIFO -> LRUCache(maxSize, defaultTTL)
            EvictionPolicy.TTL -> LRUCache(maxSize, defaultTTL)
        }
    }
}

fun <K, V> buildCache(init: CacheBuilder<K, V>.() -> Unit): Cache<K, V> {
    return CacheBuilder<K, V>().apply(init).build()
}

// ==================== Loading Cache ====================

/**
 * Cache that automatically loads values when not present.
 * 
 * Usage:
 *   val userCache = LoadingCache(lruCache) { userId ->
 *       database.getUser(userId)
 *   }
 *   
 *   val user = userCache.get(123) // Loads from DB if not cached
 */
class LoadingCache<K, V>(
    private val delegate: Cache<K, V>,
    private val loader: (K) -> V
) : Cache<K, V> by delegate {
    
    private val lock = ReentrantReadWriteLock()
    
    override fun get(key: K): V? {
        // Try to get from cache
        val cached = delegate.get(key)
        if (cached != null) return cached
        
        // Load and cache
        return lock.write {
            // Double-check after acquiring write lock
            delegate.get(key) ?: run {
                val loaded = loader(key)
                delegate.put(key, loaded)
                loaded
            }
        }
    }
    
    fun getOrLoad(key: K): V = get(key)!!
    
    fun refresh(key: K): V {
        val loaded = loader(key)
        delegate.put(key, loaded)
        return loaded
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== LRU Cache Example ===")
    
    val lruCache = buildCache<String, String> {
        maxSize(3)
        evictionPolicy(EvictionPolicy.LRU)
        defaultTTL(Duration.ofMinutes(5))
    }
    
    lruCache.put("a", "Apple")
    lruCache.put("b", "Banana")
    lruCache.put("c", "Cherry")
    
    println("Get 'a': ${lruCache.get("a")}") // Access 'a', makes it most recent
    
    lruCache.put("d", "Date") // Evicts 'b' (least recently used)
    
    println("Contains 'b': ${lruCache.containsKey("b")}") // false
    println("Contains 'a': ${lruCache.containsKey("a")}") // true
    
    println("\n=== LFU Cache Example ===")
    
    val lfuCache = LFUCache<String, Int>(3)
    
    lfuCache.put("x", 1)
    lfuCache.put("y", 2)
    lfuCache.put("z", 3)
    
    // Access 'x' and 'y' multiple times
    repeat(3) { lfuCache.get("x") }
    repeat(2) { lfuCache.get("y") }
    
    lfuCache.put("w", 4) // Evicts 'z' (least frequently used)
    
    println("Contains 'z': ${lfuCache.containsKey("z")}") // false
    println("Contains 'x': ${lfuCache.containsKey("x")}") // true
    
    println("\n=== Loading Cache Example ===")
    
    val userCache = LoadingCache(
        delegate = LRUCache<Int, String>(100),
        loader = { userId ->
            println("Loading user $userId from database...")
            "User-$userId"
        }
    )
    
    println(userCache.get(1)) // Loads from "database"
    println(userCache.get(1)) // Returns cached value
    println(userCache.get(2)) // Loads from "database"
    
    println("\n=== Cache Stats ===")
    println(lruCache.getStats())
}

