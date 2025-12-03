/**
 * Pub-Sub Messaging System - LLD Implementation
 * 
 * Design a Publish-Subscribe messaging system with:
 * - Topics for message routing
 * - Publishers and Subscribers
 * - Message filtering
 * - Sync and async delivery modes
 * 
 * Design Patterns: Observer, Strategy
 */
package lld.pubsub

import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Data Classes ====================

/**
 * Message with generic payload.
 */
data class Message<T>(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val payload: T,
    val timestamp: Instant = Instant.now(),
    val headers: Map<String, String> = emptyMap()
)

/**
 * Subscriber interface - implement to receive messages.
 */
interface Subscriber<T> {
    fun onMessage(message: Message<T>)
    fun getId(): String
}

/**
 * Message filter predicate.
 */
typealias MessageFilter<T> = (Message<T>) -> Boolean

/**
 * Subscriber with optional filter.
 */
data class FilteredSubscriber<T>(
    val subscriber: Subscriber<T>,
    val filter: MessageFilter<T>? = null
)

// ==================== Topic ====================

/**
 * Represents a message topic.
 * 
 * === Code Flow: publish(message) ===
 * 1. Acquire write lock
 * 2. Add to message history (bounded)
 * 3. For each subscriber:
 *    a. Apply filter (if any)
 *    b. Deliver async (thread pool) or sync
 * 
 * === Thread Safety ===
 * - ConcurrentHashMap for subscriber storage
 * - ExecutorService for async delivery
 * - ReentrantReadWriteLock for complex operations
 */
class Topic<T>(
    val name: String,
    private val executor: ExecutorService = Executors.newCachedThreadPool()
) {
    private val subscribers = ConcurrentHashMap<String, FilteredSubscriber<T>>()
    private val messageHistory = ConcurrentLinkedQueue<Message<T>>()
    private val historyLimit = 1000
    
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Add a subscriber with optional filter.
     */
    fun addSubscriber(subscriber: Subscriber<T>, filter: MessageFilter<T>? = null) {
        lock.write {
            subscribers[subscriber.getId()] = FilteredSubscriber(subscriber, filter)
        }
    }
    
    /**
     * Remove a subscriber.
     */
    fun removeSubscriber(subscriberId: String) {
        lock.write {
            subscribers.remove(subscriberId)
        }
    }
    
    /**
     * Publish message to all matching subscribers.
     * 
     * @param message Message to publish
     * @param async If true, deliver asynchronously (non-blocking)
     */
    fun publish(message: Message<T>, async: Boolean = true) {
        lock.write {
            // Add to history (bounded)
            messageHistory.add(message)
            if (messageHistory.size > historyLimit) {
                messageHistory.poll()
            }
            
            // Notify subscribers
            subscribers.values.forEach { filteredSub ->
                // Apply filter
                val shouldDeliver = filteredSub.filter?.invoke(message) ?: true
                
                if (shouldDeliver) {
                    if (async) {
                        executor.submit {
                            try {
                                filteredSub.subscriber.onMessage(message)
                            } catch (e: Exception) {
                                println("Error delivering to ${filteredSub.subscriber.getId()}: ${e.message}")
                            }
                        }
                    } else {
                        filteredSub.subscriber.onMessage(message)
                    }
                }
            }
        }
    }
    
    /**
     * Get subscriber count.
     */
    fun getSubscriberCount(): Int = lock.read { subscribers.size }
    
    /**
     * Replay historical messages to a new subscriber.
     */
    fun replayHistory(subscriber: Subscriber<T>, filter: MessageFilter<T>? = null) {
        lock.read {
            messageHistory.forEach { message ->
                val shouldDeliver = filter?.invoke(message) ?: true
                if (shouldDeliver) {
                    subscriber.onMessage(message)
                }
            }
        }
    }
    
    /**
     * Shutdown executor.
     */
    fun shutdown() {
        executor.shutdown()
    }
}

// ==================== Message Broker ====================

/**
 * Central message broker managing topics.
 * 
 * Usage:
 *   broker.createTopic("orders")
 *   broker.subscribe("orders", mySubscriber)
 *   broker.publish("orders", Message(...))
 */
class MessageBroker {
    private val topics = ConcurrentHashMap<String, Topic<Any>>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Create a new topic.
     */
    fun createTopic(topicName: String) {
        lock.write {
            if (topics.containsKey(topicName)) {
                throw IllegalArgumentException("Topic already exists: $topicName")
            }
            topics[topicName] = Topic(topicName)
        }
    }
    
    /**
     * Delete a topic.
     */
    fun deleteTopic(topicName: String) {
        lock.write {
            val topic = topics.remove(topicName)
            topic?.shutdown()
        }
    }
    
    /**
     * Get or create a topic.
     */
    fun getOrCreateTopic(topicName: String): Topic<Any> {
        return topics.getOrPut(topicName) { Topic(topicName) }
    }
    
    /**
     * Subscribe to a topic.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> subscribe(
        topicName: String,
        subscriber: Subscriber<T>,
        filter: MessageFilter<T>? = null
    ) {
        val topic = getOrCreateTopic(topicName)
        topic.addSubscriber(
            subscriber as Subscriber<Any>,
            filter?.let { f -> { msg: Message<Any> -> f(msg as Message<T>) } }
        )
    }
    
    /**
     * Unsubscribe from a topic.
     */
    fun unsubscribe(topicName: String, subscriberId: String) {
        topics[topicName]?.removeSubscriber(subscriberId)
    }
    
    /**
     * Publish to a topic.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> publish(topicName: String, message: Message<T>, async: Boolean = true) {
        val topic = topics[topicName]
            ?: throw IllegalArgumentException("Topic not found: $topicName")
        topic.publish(message as Message<Any>, async)
    }
    
    /**
     * Get all topic names.
     */
    fun getTopics(): Set<String> = topics.keys.toSet()
    
    /**
     * Shutdown all topics.
     */
    fun shutdown() {
        topics.values.forEach { it.shutdown() }
        topics.clear()
    }
}

// ==================== Simple Subscriber Implementation ====================

/**
 * Simple subscriber that executes a callback.
 */
class SimpleSubscriber<T>(
    private val id: String = UUID.randomUUID().toString(),
    private val handler: (Message<T>) -> Unit
) : Subscriber<T> {
    
    override fun onMessage(message: Message<T>) {
        handler(message)
    }
    
    override fun getId(): String = id
}

// ==================== Logging Subscriber ====================

/**
 * Subscriber that logs all messages.
 */
class LoggingSubscriber<T>(
    private val id: String = "logger-${UUID.randomUUID()}"
) : Subscriber<T> {
    
    private val messages = mutableListOf<Message<T>>()
    
    override fun onMessage(message: Message<T>) {
        messages.add(message)
        println("[${message.timestamp}] Topic: ${message.topic}, Payload: ${message.payload}")
    }
    
    override fun getId(): String = id
    
    fun getMessages(): List<Message<T>> = messages.toList()
}

// ==================== Batching Subscriber ====================

/**
 * Subscriber that batches messages and processes them together.
 */
class BatchingSubscriber<T>(
    private val id: String = UUID.randomUUID().toString(),
    private val batchSize: Int = 10,
    private val flushIntervalMs: Long = 5000,
    private val batchProcessor: (List<Message<T>>) -> Unit
) : Subscriber<T> {
    
    private val buffer = mutableListOf<Message<T>>()
    private val lock = ReentrantReadWriteLock()
    private var lastFlush = System.currentTimeMillis()
    
    override fun onMessage(message: Message<T>) {
        lock.write {
            buffer.add(message)
            
            val shouldFlush = buffer.size >= batchSize ||
                    System.currentTimeMillis() - lastFlush > flushIntervalMs
            
            if (shouldFlush) {
                flush()
            }
        }
    }
    
    override fun getId(): String = id
    
    fun flush() {
        lock.write {
            if (buffer.isNotEmpty()) {
                batchProcessor(buffer.toList())
                buffer.clear()
                lastFlush = System.currentTimeMillis()
            }
        }
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Pub-Sub System Example ===\n")
    
    val broker = MessageBroker()
    
    // Create topics
    broker.createTopic("orders")
    broker.createTopic("notifications")
    
    // Create subscribers
    val orderProcessor = SimpleSubscriber<Map<String, Any>>("order-processor") { msg ->
        println("Processing order: ${msg.payload}")
    }
    
    val notificationLogger = LoggingSubscriber<String>("notification-logger")
    
    // High-value order filter
    val highValueFilter: MessageFilter<Map<String, Any>> = { msg ->
        (msg.payload["amount"] as? Int ?: 0) > 100
    }
    
    // Subscribe
    broker.subscribe("orders", orderProcessor, highValueFilter)
    broker.subscribe("notifications", notificationLogger)
    
    println("Publishing messages...\n")
    
    // Publish orders
    broker.publish("orders", Message(
        topic = "orders",
        payload = mapOf("orderId" to "001", "amount" to 50)
    ))
    println("Published low-value order (filtered out)")
    
    broker.publish("orders", Message(
        topic = "orders",
        payload = mapOf("orderId" to "002", "amount" to 500)
    ))
    println("Published high-value order (delivered)")
    
    // Publish notifications
    broker.publish("notifications", Message(
        topic = "notifications",
        payload = "Welcome to our system!"
    ))
    
    broker.publish("notifications", Message(
        topic = "notifications",
        payload = "Your order has been shipped!"
    ))
    
    // Wait for async delivery
    Thread.sleep(1000)
    
    println("\n=== Notification Log ===")
    notificationLogger.getMessages().forEach { msg ->
        println("  - ${msg.payload}")
    }
    
    // Cleanup
    broker.shutdown()
    
    println("\n=== Pub-Sub Demo Complete ===")
}

