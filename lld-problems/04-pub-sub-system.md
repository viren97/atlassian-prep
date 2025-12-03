# Pub-Sub System - LLD

## Problem Statement
Design a Publish-Subscribe messaging system where publishers can send messages to topics and subscribers receive messages from topics they're subscribed to.

---

## Flow Diagrams

### High-Level Pub-Sub Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PUB-SUB MESSAGE FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Publishers                    Broker                 Subscribers   │
│   ──────────                    ──────                 ───────────   │
│                                                                      │
│   ┌──────────┐              ┌─────────────┐          ┌──────────┐   │
│   │Publisher │──publish()──▶│   Topic A   │──notify──▶│Subscriber│   │
│   │    1     │              │             │          │    1     │   │
│   └──────────┘              │ ┌─────────┐ │          └──────────┘   │
│                             │ │Messages │ │                         │
│   ┌──────────┐              │ │ Queue   │ │          ┌──────────┐   │
│   │Publisher │──publish()──▶│ └─────────┘ │──notify──▶│Subscriber│   │
│   │    2     │              └─────────────┘          │    2     │   │
│   └──────────┘                    │                  └──────────┘   │
│                                   │                                  │
│                             ┌─────────────┐          ┌──────────┐   │
│   ┌──────────┐              │   Topic B   │──notify──▶│Subscriber│   │
│   │Publisher │──publish()──▶│             │          │    3     │   │
│   │    3     │              └─────────────┘          └──────────┘   │
│   └──────────┘                                                      │
│                                                                      │
│   Key Points:                                                        │
│   • Publishers don't know about subscribers                         │
│   • Subscribers don't know about publishers                         │
│   • Broker handles all routing                                      │
│   • Topics decouple publishers from subscribers                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Message Publishing Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PUBLISH MESSAGE FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Publisher                                                          │
│       │                                                              │
│       │ publish("orders", message)                                  │
│       ▼                                                              │
│   ┌─────────────────┐                                               │
│   │  MessageBroker  │                                               │
│   └────────┬────────┘                                               │
│            │                                                         │
│            ▼                                                         │
│   ┌─────────────────┐                                               │
│   │ Topic exists?   │                                               │
│   └────────┬────────┘                                               │
│            │                                                         │
│       Yes ─┼─ No ──────▶ Create Topic (or throw error)              │
│            │                                                         │
│            ▼                                                         │
│   ┌─────────────────┐                                               │
│   │ Add to Queue    │                                               │
│   │ (if persistent) │                                               │
│   └────────┬────────┘                                               │
│            │                                                         │
│            ▼                                                         │
│   ┌─────────────────────────────────────────────────────────┐       │
│   │              For each Subscriber:                       │       │
│   │                      │                                  │       │
│   │              ┌───────┴───────┐                          │       │
│   │              │               │                          │       │
│   │         Sync Mode       Async Mode                      │       │
│   │              │               │                          │       │
│   │              ▼               ▼                          │       │
│   │    ┌─────────────┐  ┌─────────────────┐                │       │
│   │    │Call directly│  │Submit to        │                │       │
│   │    │onMessage()  │  │ExecutorService  │                │       │
│   │    └─────────────┘  └─────────────────┘                │       │
│   │                             │                          │       │
│   │                             ▼                          │       │
│   │                    ┌─────────────────┐                 │       │
│   │                    │Worker Thread    │                 │       │
│   │                    │calls onMessage()│                 │       │
│   │                    └─────────────────┘                 │       │
│   └─────────────────────────────────────────────────────────┘       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Subscription Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SUBSCRIPTION FLOW                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Subscribe:                                                         │
│   ──────────                                                         │
│   ┌────────────┐    subscribe("orders")   ┌─────────────────┐       │
│   │ Subscriber │─────────────────────────▶│  MessageBroker  │       │
│   └────────────┘                          └────────┬────────┘       │
│                                                    │                 │
│                                                    ▼                 │
│                                           ┌─────────────────┐       │
│                                           │ Get/Create      │       │
│                                           │ Topic "orders"  │       │
│                                           └────────┬────────┘       │
│                                                    │                 │
│                                                    ▼                 │
│                                           ┌─────────────────┐       │
│                                           │ Add subscriber  │       │
│                                           │ to topic's list │       │
│                                           └────────┬────────┘       │
│                                                    │                 │
│                                                    ▼                 │
│                                           ┌─────────────────┐       │
│                                           │ Return success  │       │
│                                           └─────────────────┘       │
│                                                                      │
│   After Subscribe - Message Delivery:                               │
│   ───────────────────────────────────                               │
│                                                                      │
│   ┌──────────┐     ┌───────────┐     ┌────────────┐                 │
│   │ New Msg  │────▶│  Topic    │────▶│ Subscriber │                 │
│   │ arrives  │     │"orders"   │     │ onMessage()│                 │
│   └──────────┘     │           │     └────────────┘                 │
│                    │ ┌───────┐ │                                    │
│                    │ │ Sub 1 │ │                                    │
│                    │ │ Sub 2 │──────▶ All subscribers               │
│                    │ │ Sub 3 │ │      receive message               │
│                    │ └───────┘ │                                    │
│                    └───────────┘                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Message Filtering Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    MESSAGE FILTERING                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Subscriber with Filter:                                           │
│   subscribe("orders", filter = { msg -> msg.headers["region"]=="US"})│
│                                                                      │
│   Message Delivery with Filter:                                     │
│                                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ Message: { topic: "orders", headers: {region: "EU"} }       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │                    For each Subscriber:                       │ │
│   │                                                               │ │
│   │  Subscriber 1 (no filter)     Subscriber 2 (filter: US only) │ │
│   │         │                              │                      │ │
│   │         ▼                              ▼                      │ │
│   │  ┌─────────────┐               ┌─────────────┐               │ │
│   │  │   DELIVER   │               │Apply Filter │               │ │
│   │  │   Message   │               │region=="US"?│               │ │
│   │  └─────────────┘               └──────┬──────┘               │ │
│   │                                       │                       │ │
│   │                                  No (EU≠US)                   │ │
│   │                                       │                       │ │
│   │                                       ▼                       │ │
│   │                                ┌─────────────┐               │ │
│   │                                │   SKIP      │               │ │
│   │                                │   Message   │               │ │
│   │                                └─────────────┘               │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Handling Slow Subscribers

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SLOW SUBSCRIBER HANDLING                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Problem: Slow subscriber blocks other subscribers                 │
│                                                                      │
│   Solution 1: Async Delivery with Queue per Subscriber              │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                                                             │   │
│   │  Topic ──▶ ┌────────────────┐ ──▶ Subscriber 1 (fast)      │   │
│   │            │ Queue: [M1,M2] │                               │   │
│   │            └────────────────┘                               │   │
│   │                                                             │   │
│   │        ──▶ ┌────────────────┐ ──▶ Subscriber 2 (slow)      │   │
│   │            │ Queue: [M1...] │     Processing M1...          │   │
│   │            │ (backing up)   │                               │   │
│   │            └────────────────┘                               │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   Solution 2: Drop messages if queue full                           │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                                                             │   │
│   │  if (subscriberQueue.isFull()) {                           │   │
│   │      log.warn("Dropping message for slow subscriber")      │   │
│   │      // Or: disconnect subscriber                          │   │
│   │  } else {                                                   │   │
│   │      subscriberQueue.add(message)                          │   │
│   │  }                                                          │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   Solution 3: Backpressure - notify publisher to slow down          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Requirements

### Functional Requirements
1. Create/delete topics
2. Publishers can publish messages to topics
3. Subscribers can subscribe/unsubscribe to topics
4. Deliver messages to all subscribers of a topic
5. Support message filtering
6. Support different delivery modes (sync/async)

### Non-Functional Requirements
1. Messages delivered in order (per topic)
2. Thread-safe operations
3. Handle slow subscribers
4. Scalable to many topics/subscribers

---

## Class Diagram

```
┌─────────────────────────────────────┐
│            Message<T>               │
├─────────────────────────────────────┤
│ - id: String                        │
│ - topic: String                     │
│ - payload: T                        │
│ - timestamp: Instant                │
│ - headers: Map<String, String>      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       <<interface>>                 │
│       Subscriber<T>                 │
├─────────────────────────────────────┤
│ + onMessage(message: Message<T>)    │
│ + getId(): String                   │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│            Topic<T>                 │
├─────────────────────────────────────┤
│ - name: String                      │
│ - subscribers: Set<Subscriber<T>>   │
├─────────────────────────────────────┤
│ + addSubscriber(sub: Subscriber)    │
│ + removeSubscriber(sub: Subscriber) │
│ + publish(message: Message<T>)      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│          MessageBroker              │
├─────────────────────────────────────┤
│ - topics: Map<String, Topic>        │
├─────────────────────────────────────┤
│ + createTopic(name: String)         │
│ + deleteTopic(name: String)         │
│ + publish(topic, message)           │
│ + subscribe(topic, subscriber)      │
│ + unsubscribe(topic, subscriber)    │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Data Classes

```kotlin
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Message ====================

data class Message<T>(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val payload: T,
    val timestamp: Instant = Instant.now(),
    val headers: Map<String, String> = emptyMap()
)

// ==================== Subscriber Interface ====================

interface Subscriber<T> {
    val id: String
    fun onMessage(message: Message<T>)
    fun onError(message: Message<T>, error: Throwable) {
        println("Error processing message ${message.id}: ${error.message}")
    }
}

// ==================== Message Filter ====================

fun interface MessageFilter<T> {
    fun matches(message: Message<T>): Boolean
}

// Subscriber with filter
data class FilteredSubscriber<T>(
    val subscriber: Subscriber<T>,
    val filter: MessageFilter<T>? = null
)
```

### Topic Implementation

```kotlin
// ==================== Topic ====================

/**
 * Represents a message topic that subscribers can listen to.
 * 
 * === Thread Safety ===
 * - ConcurrentHashMap for subscriber storage
 * - ExecutorService for async message delivery
 * - ReentrantReadWriteLock for complex operations
 * 
 * === Message Delivery ===
 * - Async (default): Messages delivered via ExecutorService
 * - Sync: Direct method call, blocks publisher
 * 
 * === Message History ===
 * - Maintains last N messages for replay on subscribe
 * - Useful for late-joining subscribers
 * - Bounded to prevent memory issues
 * 
 * Time Complexity:
 * - addSubscriber: O(1)
 * - publish: O(n) where n = subscribers
 * - removeSubscriber: O(1)
 */
class Topic<T>(
    val name: String,
    private val executor: ExecutorService = Executors.newCachedThreadPool()
) {
    // Thread-safe subscriber storage: subscriberId → (subscriber, filter)
    private val subscribers = ConcurrentHashMap<String, FilteredSubscriber<T>>()
    
    // Bounded message history for replay functionality
    private val messageHistory = ConcurrentLinkedQueue<Message<T>>()
    private val historyLimit = 1000  // Prevent unbounded growth
    
    private val lock = ReentrantReadWriteLock()
    
    fun addSubscriber(
        subscriber: Subscriber<T>,
        filter: MessageFilter<T>? = null,
        replayFromBeginning: Boolean = false
    ) {
        lock.write {
            subscribers[subscriber.id] = FilteredSubscriber(subscriber, filter)
        }
        
        // Replay historical messages if requested
        if (replayFromBeginning) {
            messageHistory.forEach { message ->
                deliverToSubscriber(subscriber, message, filter)
            }
        }
    }
    
    fun removeSubscriber(subscriberId: String): Boolean {
        return lock.write {
            subscribers.remove(subscriberId) != null
        }
    }
    
    fun publish(message: Message<T>) {
        // Store in history
        messageHistory.offer(message)
        while (messageHistory.size > historyLimit) {
            messageHistory.poll()
        }
        
        // Deliver to all subscribers asynchronously
        lock.read {
            subscribers.values.forEach { filtered ->
                executor.submit {
                    deliverToSubscriber(filtered.subscriber, message, filtered.filter)
                }
            }
        }
    }
    
    fun publishSync(message: Message<T>) {
        messageHistory.offer(message)
        
        lock.read {
            subscribers.values.forEach { filtered ->
                deliverToSubscriber(filtered.subscriber, message, filtered.filter)
            }
        }
    }
    
    private fun deliverToSubscriber(
        subscriber: Subscriber<T>,
        message: Message<T>,
        filter: MessageFilter<T>?
    ) {
        try {
            if (filter == null || filter.matches(message)) {
                subscriber.onMessage(message)
            }
        } catch (e: Exception) {
            subscriber.onError(message, e)
        }
    }
    
    fun getSubscriberCount(): Int = subscribers.size
    
    fun getSubscriberIds(): Set<String> = subscribers.keys.toSet()
    
    fun shutdown() {
        executor.shutdown()
    }
}
```

### Message Broker

```kotlin
// ==================== Message Broker ====================

class MessageBroker {
    private val topics = ConcurrentHashMap<String, Topic<Any>>()
    private val lock = ReentrantReadWriteLock()
    
    fun createTopic(name: String): Topic<Any> {
        return lock.write {
            topics.getOrPut(name) { Topic(name) }
        }
    }
    
    fun deleteTopic(name: String): Boolean {
        return lock.write {
            topics.remove(name)?.also { it.shutdown() } != null
        }
    }
    
    fun getTopic(name: String): Topic<Any>? = topics[name]
    
    fun getTopicNames(): Set<String> = topics.keys.toSet()
    
    @Suppress("UNCHECKED_CAST")
    fun <T> publish(topicName: String, payload: T, headers: Map<String, String> = emptyMap()) {
        val topic = topics[topicName] 
            ?: throw IllegalArgumentException("Topic not found: $topicName")
        
        val message = Message(
            topic = topicName,
            payload = payload as Any,
            headers = headers
        )
        
        topic.publish(message)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> subscribe(
        topicName: String,
        subscriber: Subscriber<T>,
        filter: MessageFilter<T>? = null
    ) {
        val topic = topics[topicName]
            ?: throw IllegalArgumentException("Topic not found: $topicName")
        
        topic.addSubscriber(
            subscriber as Subscriber<Any>,
            filter as? MessageFilter<Any>
        )
    }
    
    fun unsubscribe(topicName: String, subscriberId: String): Boolean {
        val topic = topics[topicName] ?: return false
        return topic.removeSubscriber(subscriberId)
    }
    
    fun shutdown() {
        topics.values.forEach { it.shutdown() }
        topics.clear()
    }
}
```

### Subscriber Implementations

```kotlin
// ==================== Concrete Subscribers ====================

// Simple logging subscriber
class LoggingSubscriber<T>(
    override val id: String = UUID.randomUUID().toString()
) : Subscriber<T> {
    override fun onMessage(message: Message<T>) {
        println("[${message.timestamp}] ${message.topic}: ${message.payload}")
    }
}

// Batching subscriber - collects messages and processes in batches
class BatchingSubscriber<T>(
    override val id: String = UUID.randomUUID().toString(),
    private val batchSize: Int = 10,
    private val maxWaitMs: Long = 5000,
    private val onBatch: (List<Message<T>>) -> Unit
) : Subscriber<T> {
    
    private val batch = mutableListOf<Message<T>>()
    private val lock = ReentrantReadWriteLock()
    private var lastFlush = System.currentTimeMillis()
    
    override fun onMessage(message: Message<T>) {
        lock.write {
            batch.add(message)
            
            if (batch.size >= batchSize || 
                System.currentTimeMillis() - lastFlush > maxWaitMs) {
                flush()
            }
        }
    }
    
    private fun flush() {
        if (batch.isNotEmpty()) {
            onBatch(batch.toList())
            batch.clear()
            lastFlush = System.currentTimeMillis()
        }
    }
}

// Async subscriber with its own queue
class AsyncSubscriber<T>(
    override val id: String = UUID.randomUUID().toString(),
    private val handler: (Message<T>) -> Unit,
    queueCapacity: Int = 1000
) : Subscriber<T> {
    
    private val queue = LinkedBlockingQueue<Message<T>>(queueCapacity)
    private val running = java.util.concurrent.atomic.AtomicBoolean(true)
    
    init {
        Thread {
            while (running.get()) {
                try {
                    val message = queue.poll(100, TimeUnit.MILLISECONDS)
                    message?.let { handler(it) }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.start()
    }
    
    override fun onMessage(message: Message<T>) {
        if (!queue.offer(message)) {
            println("Queue full, dropping message: ${message.id}")
        }
    }
    
    fun stop() {
        running.set(false)
    }
}
```

### Usage Example

```kotlin
fun main() {
    val broker = MessageBroker()
    
    // Create topics
    broker.createTopic("orders")
    broker.createTopic("notifications")
    
    // Create subscribers
    val orderLogger = LoggingSubscriber<Any>("order-logger")
    val orderProcessor = object : Subscriber<Any> {
        override val id = "order-processor"
        override fun onMessage(message: Message<Any>) {
            println("Processing order: ${message.payload}")
        }
    }
    
    // Subscribe with filter (only high-value orders)
    broker.subscribe("orders", orderLogger)
    broker.subscribe("orders", orderProcessor) { message ->
        val payload = message.payload
        payload is Map<*, *> && (payload["amount"] as? Int ?: 0) > 100
    }
    
    // Publish messages
    broker.publish("orders", mapOf("id" to 1, "amount" to 50))
    broker.publish("orders", mapOf("id" to 2, "amount" to 200)) // Only this triggers orderProcessor
    
    Thread.sleep(1000)
    broker.shutdown()
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Observer** | `Subscriber` interface | Notify subscribers of messages |
| **Strategy** | `MessageFilter` | Pluggable filtering logic |
| **Decorator** | `BatchingSubscriber` | Add batching behavior |
| **Singleton** | `MessageBroker` can be | Central message coordination |

---

## Interview Discussion Points

### Q: How do you handle slow subscribers?
**A:**
- Use async delivery with per-subscriber queues
- Implement backpressure (bounded queues)
- Drop messages or dead-letter queue when full
- Consider pull vs push model

### Q: How to ensure message ordering?
**A:**
- Single thread per topic for publishing
- Sequence numbers on messages
- Ordered delivery per subscriber queue
- Partition by key for parallel processing

### Q: How would you scale this?
**A:**
- Partition topics across nodes
- Use consistent hashing for topic assignment
- Replicate for fault tolerance
- Consider Kafka-like architecture

---

## Time Complexity

| Operation | Complexity |
|-----------|------------|
| Create Topic | O(1) |
| Subscribe | O(1) |
| Publish | O(n) where n = subscribers |
| Unsubscribe | O(1) |

