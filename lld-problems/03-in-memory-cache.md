# In-Memory Cache (LRU/LFU) - LLD

## Problem Statement
Design an in-memory cache system with support for different eviction policies (LRU, LFU), TTL (Time-To-Live), and thread-safe operations.

---

## Flow Diagrams

### High-Level Cache Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CACHE OPERATIONS                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   GET Operation:                                                     │
│   ┌──────────┐     ┌─────────────┐     ┌─────────────────────────┐  │
│   │  Client  │────▶│  Cache.get  │────▶│     Key exists?         │  │
│   └──────────┘     └─────────────┘     └─────────────────────────┘  │
│                                                │                     │
│                                           Yes ─┼─ No                 │
│                                                │    │                │
│                           ┌────────────────────┘    │                │
│                           ▼                         ▼                │
│                    ┌─────────────┐          ┌─────────────┐         │
│                    │ TTL valid?  │          │ Return NULL │         │
│                    └─────────────┘          │ (Cache Miss)│         │
│                           │                 └─────────────┘         │
│                      Yes ─┼─ No                                      │
│                           │    │                                     │
│          ┌────────────────┘    └────────────┐                       │
│          ▼                                  ▼                        │
│   ┌─────────────┐                    ┌─────────────┐                │
│   │ Update LRU  │                    │Remove Entry │                │
│   │ (move front)│                    │ Return NULL │                │
│   └─────────────┘                    └─────────────┘                │
│          │                                                           │
│          ▼                                                           │
│   ┌─────────────┐                                                   │
│   │Return Value │                                                   │
│   │(Cache Hit)  │                                                   │
│   └─────────────┘                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### PUT Operation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PUT OPERATION                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   cache.put(key, value)                                             │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────┐                                               │
│   │ Key exists?     │                                               │
│   └─────────────────┘                                               │
│         │                                                            │
│    Yes ─┼─ No                                                        │
│         │    │                                                       │
│         │    └──────────────────────┐                               │
│         ▼                           ▼                                │
│   ┌───────────────┐         ┌─────────────────┐                     │
│   │ Update value  │         │ Cache full?     │                     │
│   │ Move to front │         └─────────────────┘                     │
│   └───────────────┘               │                                  │
│                              Yes ─┼─ No                              │
│                                   │    │                             │
│                   ┌───────────────┘    └──────────┐                 │
│                   ▼                               ▼                  │
│           ┌─────────────────┐            ┌─────────────┐            │
│           │ EVICT (LRU/LFU) │            │ Add entry   │            │
│           │ Remove tail/min │            │ to front    │            │
│           └─────────────────┘            └─────────────┘            │
│                   │                                                  │
│                   ▼                                                  │
│           ┌─────────────┐                                           │
│           │ Add new     │                                           │
│           │ entry       │                                           │
│           └─────────────┘                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### LRU Cache Internal Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                     LRU CACHE STRUCTURE                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   HashMap (O(1) lookup)        Doubly Linked List (O(1) eviction)   │
│   ┌─────────────────┐          ┌─────────────────────────────────┐  │
│   │ Key │ Node Ref  │          │                                 │  │
│   ├─────┼───────────┤          │  HEAD                    TAIL   │  │
│   │ "A" │  ──────────┼────────▶│   │                       │     │  │
│   │ "B" │  ──────────┼───────┐ │   ▼                       ▼     │  │
│   │ "C" │  ──────────┼────┐  │ │ ┌───┐    ┌───┐    ┌───┐    ┌───┐│  │
│   └─────┴───────────┘    │  └─┼▶│ A │◀──▶│ B │◀──▶│ C │◀──▶│ D ││  │
│                          │    │ └───┘    └───┘    └───┘    └───┘│  │
│                          └────┼───────────────────────┘         │  │
│                               │  Most                   Least   │  │
│                               │  Recent                 Recent  │  │
│                               │  (Get/Put)              (Evict) │  │
│                               └─────────────────────────────────┘  │
│                                                                      │
│   On GET "B":                                                        │
│   1. Lookup in HashMap: O(1)                                         │
│   2. Remove B from current position                                  │
│   3. Add B to HEAD                                                   │
│   Result: HEAD → B → A → C → D → TAIL                               │
│                                                                      │
│   On EVICT (capacity full):                                          │
│   1. Remove node at TAIL (D)                                         │
│   2. Remove from HashMap                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### LFU Cache Internal Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                     LFU CACHE STRUCTURE                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Key Map              Frequency Map (LinkedHashSet per frequency)  │
│   ┌─────────────┐      ┌─────────────────────────────────────────┐  │
│   │ Key │ Freq  │      │ Freq │ Keys (insertion order)           │  │
│   ├─────┼───────┤      ├──────┼──────────────────────────────────┤  │
│   │ "A" │  3    │      │  1   │ [D] ← evict first (oldest)       │  │
│   │ "B" │  2    │      │  2   │ [B, E] ← E is newer than B       │  │
│   │ "C" │  3    │      │  3   │ [A, C]                           │  │
│   │ "D" │  1    │      └──────┴──────────────────────────────────┘  │
│   │ "E" │  2    │                                                   │
│   └─────┴───────┘      minFrequency = 1                             │
│                                                                      │
│   On GET "B":                                                        │
│   1. Lookup key: freq = 2                                           │
│   2. Remove B from freq[2]                                          │
│   3. Add B to freq[3]                                               │
│   4. Update key map: B → 3                                          │
│   5. If freq[2] empty and minFreq=2, update minFreq                 │
│                                                                      │
│   On EVICT:                                                          │
│   1. Get minFrequency (1)                                           │
│   2. Remove oldest key from freq[1] → "D"                           │
│   3. Remove D from key map                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### TTL Expiration Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      TTL EXPIRATION                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Two Strategies:                                                    │
│                                                                      │
│   1. LAZY EXPIRATION (on access):                                   │
│      ┌─────────────────────────────────────────────────────────┐    │
│      │  GET request → Check TTL → If expired → Delete → Miss   │    │
│      │                          → If valid  → Return → Hit     │    │
│      └─────────────────────────────────────────────────────────┘    │
│                                                                      │
│   2. ACTIVE EXPIRATION (background thread):                         │
│      ┌─────────────────────────────────────────────────────────┐    │
│      │  Every N seconds:                                        │    │
│      │  1. Scan entries                                         │    │
│      │  2. Remove expired entries                               │    │
│      │  3. Sleep N seconds                                      │    │
│      └─────────────────────────────────────────────────────────┘    │
│                                                                      │
│   Timeline Example:                                                  │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │ T=0    PUT(A, val, ttl=5s)   Entry created                   │  │
│   │ T=2    GET(A) → HIT          2 < 5, valid                    │  │
│   │ T=6    GET(A) → MISS         6 > 5, expired, deleted         │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Requirements

### Functional Requirements
1. Put/Get/Delete operations
2. Support LRU (Least Recently Used) eviction
3. Support LFU (Least Frequently Used) eviction
4. TTL support for automatic expiration
5. Maximum capacity with automatic eviction
6. Clear all entries

### Non-Functional Requirements
1. O(1) time complexity for get/put operations
2. Thread-safe for concurrent access
3. Configurable eviction policy
4. Memory efficient

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│         <<interface>>                   │
│             Cache<K, V>                 │
├─────────────────────────────────────────┤
│ + get(key: K): V?                       │
│ + put(key: K, value: V)                 │
│ + put(key: K, value: V, ttl: Duration)  │
│ + remove(key: K): V?                    │
│ + clear()                               │
│ + size(): Int                           │
│ + containsKey(key: K): Boolean          │
└─────────────────────────────────────────┘
              △
              │ implements
    ┌─────────┴─────────┐
    │                   │
┌───┴───────┐     ┌─────┴─────┐
│ LRUCache  │     │ LFUCache  │
└───────────┘     └───────────┘

┌─────────────────────────────────────────┐
│          CacheEntry<V>                  │
├─────────────────────────────────────────┤
│ - value: V                              │
│ - createdAt: Instant                    │
│ - expiresAt: Instant?                   │
│ - frequency: Int (for LFU)              │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│         DoublyLinkedList<K>             │
├─────────────────────────────────────────┤
│ + addFirst(key: K): Node<K>             │
│ + remove(node: Node<K>)                 │
│ + removeLast(): K?                      │
│ + moveToFirst(node: Node<K>)            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│           CacheBuilder                  │
├─────────────────────────────────────────┤
│ + maxSize(size: Int)                    │
│ + evictionPolicy(policy: EvictionPolicy)│
│ + defaultTTL(ttl: Duration)             │
│ + build(): Cache<K, V>                  │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Interfaces and Data Classes

```kotlin
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
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

data class CacheStats(
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val size: Int,
    val maxSize: Int
) {
    val hitRate: Double get() = if (hits + misses > 0) hits.toDouble() / (hits + misses) else 0.0
}
```

### Doubly Linked List for LRU

```kotlin
// ==================== Doubly Linked List ====================

class DoublyLinkedListNode<K>(
    val key: K,
    var prev: DoublyLinkedListNode<K>? = null,
    var next: DoublyLinkedListNode<K>? = null
)

class DoublyLinkedList<K> {
    private val head = DoublyLinkedListNode<K?>(null)  // Dummy head
    private val tail = DoublyLinkedListNode<K?>(null)  // Dummy tail
    private var count = 0
    
    init {
        head.next = tail as DoublyLinkedListNode<K?>
        tail.prev = head
    }
    
    fun addFirst(key: K): DoublyLinkedListNode<K> {
        val node = DoublyLinkedListNode(key)
        
        @Suppress("UNCHECKED_CAST")
        val headNext = head.next as DoublyLinkedListNode<K>
        
        node.prev = head as DoublyLinkedListNode<K>
        node.next = headNext
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
        
        node.prev = head as DoublyLinkedListNode<K>
        node.next = headNext
        head.next = node as DoublyLinkedListNode<K?>
        headNext.prev = node
        
        count++
    }
    
    fun isEmpty(): Boolean = count == 0
    
    fun size(): Int = count
}
```

### LRU Cache Implementation

```kotlin
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
 *   │ "B" │ node_ref ├───────────────────┘           │
 *   │ "C" │ node_ref ├───────────────────────────────┘
 *   └─────┴──────────┘        MRU ◀──────────────▶ LRU
 * 
 * === Operations ===
 * - GET: O(1) - HashMap lookup + move node to head
 * - PUT: O(1) - HashMap insert + add node to head + evict if full
 * - REMOVE: O(1) - HashMap delete + remove node from list
 * - EVICT: O(1) - Remove tail node + delete from HashMap
 * 
 * === Why Doubly Linked List? ===
 * - Need O(1) removal from middle (when accessed)
 * - Need O(1) insertion at head (on access/insert)
 * - Need O(1) removal from tail (eviction)
 * - Singly linked list would require O(n) for middle removal
 * 
 * === Thread Safety ===
 * Uses ReentrantReadWriteLock:
 * - Multiple readers can access simultaneously
 * - Writers get exclusive access
 * - Prevents read-write conflicts
 * 
 * @param maxSize Maximum entries before eviction
 * @param defaultTTL Optional time-to-live for entries
 */
class LRUCache<K, V>(
    private val maxSize: Int,
    private val defaultTTL: Duration? = null
) : Cache<K, V> {
    
    /**
     * Combines the linked list node (for ordering) with the cache entry (for data).
     * Allows O(1) access to both from a single HashMap lookup.
     */
    private data class NodeEntry<K, V>(
        val node: DoublyLinkedListNode<K>,  // Position in access order list
        val entry: CacheEntry<V>             // Actual cached value + metadata
    )
    
    private val cache = HashMap<K, NodeEntry<K, V>>()  // O(1) key lookup
    private val accessOrder = DoublyLinkedList<K>()    // Maintains LRU ordering
    private val lock = ReentrantReadWriteLock()        // Thread safety
    
    // Statistics for monitoring cache performance
    private var hits = 0L      // Successful gets
    private var misses = 0L    // Failed gets (key not found or expired)
    private var evictions = 0L // Entries removed due to capacity
    
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
    
    // Cleanup expired entries (can be called periodically)
    fun cleanupExpired() = lock.write {
        val expiredKeys = cache.filter { it.value.entry.isExpired() }.keys
        expiredKeys.forEach { removeInternal(it) }
    }
}
```

### LFU Cache Implementation

```kotlin
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
 *   │ "D" │ val_D │    │ "D" │  1  │    └─────┴─────────────────┘
 *   │ "E" │ val_E │    │ "E" │  2  │    minFrequency = 1
 *   └─────┴───────┘    └─────┴─────┘
 * 
 * === Operations ===
 * - GET: O(1) - lookup + increment frequency + move between buckets
 * - PUT: O(1) - add with frequency=1 + evict if needed
 * - EVICT: O(1) - remove oldest key from minFrequency bucket
 * 
 * === Why LinkedHashSet for Buckets? ===
 * - Maintains insertion order (FIFO within same frequency)
 * - O(1) add/remove operations
 * - When evicting from minFrequency bucket, oldest entry goes first
 * 
 * === LFU vs LRU ===
 * - LFU: Evicts least frequently accessed (good for stable access patterns)
 * - LRU: Evicts least recently accessed (good for temporal locality)
 * - LFU better when some items are "hot" and accessed repeatedly
 * 
 * @param maxSize Maximum entries before eviction
 * @param defaultTTL Optional time-to-live for entries
 */
class LFUCache<K, V>(
    private val maxSize: Int,
    private val defaultTTL: Duration? = null
) : Cache<K, V> {
    
    private val cache = HashMap<K, CacheEntry<V>>()           // Key → Value
    private val keyFrequency = HashMap<K, Int>()               // Key → Access count
    private val frequencyBuckets = HashMap<Int, LinkedHashSet<K>>()  // Freq → Keys
    private var minFrequency = 0  // Tracks minimum for O(1) eviction
    
    private val lock = ReentrantReadWriteLock()
    
    // Statistics
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
```

### Cache Builder

```kotlin
// ==================== Cache Builder ====================

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
            EvictionPolicy.FIFO -> LRUCache(maxSize, defaultTTL) // FIFO is LRU without access updates
            EvictionPolicy.TTL -> LRUCache(maxSize, defaultTTL)
        }
    }
}

// Extension function for easy builder access
fun <K, V> buildCache(init: CacheBuilder<K, V>.() -> Unit): Cache<K, V> {
    return CacheBuilder<K, V>().apply(init).build()
}
```

### Thread-Safe Wrapper (Decorator Pattern)

```kotlin
// ==================== Thread-Safe Cache Decorator ====================

class ThreadSafeCache<K, V>(
    private val delegate: Cache<K, V>
) : Cache<K, V> {
    
    private val lock = ReentrantReadWriteLock()
    
    override fun get(key: K): V? = lock.read { delegate.get(key) }
    
    override fun put(key: K, value: V) = lock.write { delegate.put(key, value) }
    
    override fun put(key: K, value: V, ttl: Duration) = lock.write { delegate.put(key, value, ttl) }
    
    override fun remove(key: K): V? = lock.write { delegate.remove(key) }
    
    override fun clear() = lock.write { delegate.clear() }
    
    override fun size(): Int = lock.read { delegate.size() }
    
    override fun containsKey(key: K): Boolean = lock.read { delegate.containsKey(key) }
    
    override fun keys(): Set<K> = lock.read { delegate.keys() }
    
    override fun getStats(): CacheStats = lock.read { delegate.getStats() }
}
```

### Loading Cache (Compute if Absent)

```kotlin
// ==================== Loading Cache ====================

/**
 * Cache that automatically loads values when not present
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
```

### Usage Example

```kotlin
fun main() {
    println("=== LRU Cache Example ===")
    
    // Using builder
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
            "User-$userId" // Simulate DB fetch
        }
    )
    
    println(userCache.get(1)) // Loads from "database"
    println(userCache.get(1)) // Returns cached value
    println(userCache.get(2)) // Loads from "database"
    
    println("\n=== Cache Stats ===")
    println(lruCache.getStats())
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | `EvictionPolicy` | Different eviction algorithms |
| **Decorator** | `ThreadSafeCache` | Add thread safety transparently |
| **Builder** | `CacheBuilder` | Fluent cache configuration |
| **Proxy** | `LoadingCache` | Add auto-loading capability |

---

## Data Structure Choices

### LRU Cache
```
HashMap + Doubly Linked List

HashMap: O(1) lookup
┌─────────┬──────────────────┐
│  Key    │  (Node, Entry)   │
├─────────┼──────────────────┤
│  "a"    │  (node1, val1)   │
│  "b"    │  (node2, val2)   │
└─────────┴──────────────────┘

Doubly Linked List: O(1) reorder
HEAD <-> node1 <-> node2 <-> node3 <-> TAIL
(MRU)                              (LRU - evict)
```

### LFU Cache
```
HashMap + Frequency Buckets

keyFrequency: key -> frequency
┌─────┬───────┐
│ "a" │   3   │
│ "b" │   1   │
└─────┴───────┘

frequencyBuckets: frequency -> LinkedHashSet<Key>
┌─────┬───────────────┐
│  1  │  {b, c}       │  <- minFrequency (evict from here)
│  2  │  {}           │
│  3  │  {a}          │
└─────┴───────────────┘
```

---

## Interview Discussion Points

### Q: Why use Doubly Linked List for LRU?
**A:**
- Need O(1) removal from middle of list
- Need O(1) insertion at head
- Need O(1) removal from tail
- Singly linked list would require O(n) for removal

### Q: How do you handle concurrent access?
**A:**
- `ReentrantReadWriteLock` for read-heavy workloads
- `ConcurrentHashMap` for simpler cases
- Consider lock striping for high concurrency
- Atomic operations where possible

### Q: How would you implement distributed cache?
**A:**
- Use Redis/Memcached for shared state
- Consistent hashing for partitioning
- Cache-aside, write-through, write-behind patterns
- Handle network partitions gracefully

### Q: What about cache warming?
**A:**
- Pre-load frequently accessed data on startup
- Asynchronous background warming
- Gradual warming to avoid thundering herd

---

## Time & Space Complexity

| Operation | LRU | LFU |
|-----------|-----|-----|
| Get | O(1) | O(1) |
| Put | O(1) | O(1) |
| Remove | O(1) | O(1) |
| Eviction | O(1) | O(1) |

**Space Complexity:** O(n) where n = capacity

---

## Edge Cases

1. **Null values** - decide if allowed
2. **Concurrent modifications** during iteration
3. **TTL expiration** during get operation
4. **Capacity = 0** edge case
5. **Memory pressure** - consider soft references
6. **Hot keys** - single key accessed very frequently

