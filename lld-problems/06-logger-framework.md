# Logger Framework - LLD

## Problem Statement
Design a logging framework that supports multiple log levels, multiple output destinations (sinks), and configurable formatting.

---

## Flow Diagrams

### High-Level Logger Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LOGGING FRAMEWORK FLOW                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Application Code                                                   │
│         │                                                            │
│         │ logger.info("User logged in", userId=123)                 │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │   Logger    │                                                   │
│   │  "UserSvc"  │                                                   │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              Check Log Level                                 │   │
│   │                                                             │   │
│   │  Logger Level: INFO                                         │   │
│   │  Message Level: INFO                                        │   │
│   │  INFO >= INFO? → YES, proceed                               │   │
│   └─────────────────────────────────────────────────────────────┘   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │Create       │                                                   │
│   │LogEntry     │  { timestamp, level, logger, message, context }   │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │  Formatter  │  "[2024-01-15 10:30:45] INFO UserSvc - User..."  │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                    Write to Sinks                            │   │
│   │                                                             │   │
│   │  ┌───────────┐  ┌───────────┐  ┌───────────┐               │   │
│   │  │ Console   │  │   File    │  │  Remote   │               │   │
│   │  │   Sink    │  │   Sink    │  │   Sink    │               │   │
│   │  └───────────┘  └───────────┘  └───────────┘               │   │
│   │       │              │              │                       │   │
│   │       ▼              ▼              ▼                       │   │
│   │   stdout         app.log      LogServer                     │   │
│   │                                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Log Level Filtering

```
┌─────────────────────────────────────────────────────────────────────┐
│                    LOG LEVEL HIERARCHY                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Level Priority (high to low):                                     │
│                                                                      │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  FATAL (5) ──▶ Critical errors, app may crash                 │ │
│   │    │                                                          │ │
│   │  ERROR (4) ──▶ Errors that need attention                     │ │
│   │    │                                                          │ │
│   │  WARN  (3) ──▶ Warning, something unexpected                  │ │
│   │    │                                                          │ │
│   │  INFO  (2) ──▶ General information                            │ │
│   │    │                                                          │ │
│   │  DEBUG (1) ──▶ Detailed debugging info                        │ │
│   │    │                                                          │ │
│   │  TRACE (0) ──▶ Very detailed tracing                          │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Filtering Rule:                                                    │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  if (messageLevel.priority >= loggerLevel.priority) {         │ │
│   │      // Log the message                                       │ │
│   │  } else {                                                     │ │
│   │      // Skip (message level too low)                          │ │
│   │  }                                                            │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Example: Logger level = WARN                                      │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  logger.debug("...")  → SKIP (DEBUG < WARN)                   │ │
│   │  logger.info("...")   → SKIP (INFO < WARN)                    │ │
│   │  logger.warn("...")   → LOG  (WARN >= WARN) ✓                 │ │
│   │  logger.error("...")  → LOG  (ERROR >= WARN) ✓                │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Async Logging Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    ASYNC LOGGING                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Problem: Sync logging blocks application thread                   │
│                                                                      │
│   Sync (Blocking):                                                  │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │ App Thread:  [──work──][LOG][──────work──────][LOG][─work─]   │ │
│   │                         │                       │              │ │
│   │                    blocks!                 blocks!             │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Async (Non-Blocking):                                             │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │ App Thread:  [──work──][Q][──────work──────][Q][───work───]   │ │
│   │                         │                    │                 │ │
│   │                    enqueue              enqueue                │ │
│   │                         │                    │                 │ │
│   │                         ▼                    ▼                 │ │
│   │ Log Thread:       [──LOG──]           [──LOG──]               │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Async Implementation:                                             │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │                                                               │ │
│   │  Application    BlockingQueue       Log Worker                │ │
│   │      │          ┌─────────┐            │                      │ │
│   │      │──offer──▶│ Entry 1 │            │                      │ │
│   │      │          │ Entry 2 │◀──take─────│                      │ │
│   │      │──offer──▶│ Entry 3 │            │                      │ │
│   │      │          └─────────┘            │                      │ │
│   │      │                                 │                      │ │
│   │  (non-blocking)              (writes to sinks)                │ │
│   │                                                               │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Log Rotation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FILE LOG ROTATION                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Before Rotation:                                                  │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  app.log (50MB) ← current, still writing                      │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Rotation Trigger (size > 50MB):                                   │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  1. Close current file                                        │ │
│   │  2. Rename: app.log → app.log.1                               │ │
│   │  3. Rename: app.log.1 → app.log.2 (shift existing)            │ │
│   │  4. Create new app.log                                        │ │
│   │  5. Delete oldest if > maxFiles                               │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   After Rotation (maxFiles=3):                                      │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  app.log   (0KB)  ← new, current                              │ │
│   │  app.log.1 (50MB) ← previous                                  │ │
│   │  app.log.2 (50MB) ← older                                     │ │
│   │  app.log.3 (deleted - exceeded maxFiles)                      │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
│   Alternative: Date-based rotation                                  │
│   ┌───────────────────────────────────────────────────────────────┐ │
│   │  app.2024-01-15.log                                           │ │
│   │  app.2024-01-14.log                                           │ │
│   │  app.2024-01-13.log                                           │ │
│   └───────────────────────────────────────────────────────────────┘ │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Requirements

### Functional Requirements
1. Multiple log levels (DEBUG, INFO, WARN, ERROR, FATAL)
2. Multiple sinks (Console, File, Remote)
3. Configurable log format
4. Namespace/category support
5. Async logging option
6. Log rotation for file sink

### Non-Functional Requirements
1. Thread-safe logging
2. Minimal performance impact
3. Extensible for new sinks
4. Easy configuration

---

## Class Diagram

```
┌─────────────────────────────────────┐
│           Logger                    │
├─────────────────────────────────────┤
│ - name: String                      │
│ - level: LogLevel                   │
│ - sinks: List<LogSink>              │
├─────────────────────────────────────┤
│ + debug(message: String)            │
│ + info(message: String)             │
│ + warn(message: String)             │
│ + error(message: String, ex?)       │
│ + log(level, message)               │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       <<interface>> LogSink         │
├─────────────────────────────────────┤
│ + write(entry: LogEntry)            │
│ + close()                           │
└─────────────────────────────────────┘
         △
         │
    ┌────┼────┬──────────┐
    │    │    │          │
Console File  Remote  AsyncSink
 Sink   Sink   Sink   (Decorator)

┌─────────────────────────────────────┐
│       <<interface>> LogFormatter    │
├─────────────────────────────────────┤
│ + format(entry: LogEntry): String   │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Classes

```kotlin
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

// ==================== Log Level ====================

enum class LogLevel(val priority: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);
    
    fun isEnabled(minLevel: LogLevel): Boolean = this.priority >= minLevel.priority
}

// ==================== Log Entry ====================

data class LogEntry(
    val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val loggerName: String,
    val message: String,
    val threadName: String = Thread.currentThread().name,
    val exception: Throwable? = null,
    val context: Map<String, String> = emptyMap()
)
```

### Log Formatter

```kotlin
// ==================== Formatter Interface ====================

interface LogFormatter {
    fun format(entry: LogEntry): String
}

// ==================== Default Formatter ====================

class DefaultLogFormatter(
    private val pattern: String = "[%timestamp] [%level] [%thread] %logger - %message"
) : LogFormatter {
    
    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())
    
    override fun format(entry: LogEntry): String {
        var result = pattern
            .replace("%timestamp", dateFormatter.format(entry.timestamp))
            .replace("%level", entry.level.name.padEnd(5))
            .replace("%thread", entry.threadName)
            .replace("%logger", entry.loggerName)
            .replace("%message", entry.message)
        
        // Add exception stack trace if present
        entry.exception?.let { ex ->
            result += "\n${ex.stackTraceToString()}"
        }
        
        return result
    }
}

// ==================== JSON Formatter ====================

class JsonLogFormatter : LogFormatter {
    override fun format(entry: LogEntry): String {
        val json = buildString {
            append("{")
            append("\"timestamp\":\"${entry.timestamp}\",")
            append("\"level\":\"${entry.level}\",")
            append("\"logger\":\"${entry.loggerName}\",")
            append("\"thread\":\"${entry.threadName}\",")
            append("\"message\":\"${escapeJson(entry.message)}\"")
            entry.exception?.let {
                append(",\"exception\":\"${escapeJson(it.stackTraceToString())}\"")
            }
            if (entry.context.isNotEmpty()) {
                append(",\"context\":{")
                append(entry.context.entries.joinToString(",") { 
                    "\"${it.key}\":\"${escapeJson(it.value)}\"" 
                })
                append("}")
            }
            append("}")
        }
        return json
    }
    
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
```

### Log Sinks

```kotlin
// ==================== Sink Interface ====================

interface LogSink {
    fun write(entry: LogEntry)
    fun close()
}

// ==================== Console Sink ====================

class ConsoleSink(
    private val formatter: LogFormatter = DefaultLogFormatter()
) : LogSink {
    
    override fun write(entry: LogEntry) {
        val output = if (entry.level.priority >= LogLevel.ERROR.priority) {
            System.err
        } else {
            System.out
        }
        output.println(formatter.format(entry))
    }
    
    override fun close() {
        // No-op for console
    }
}

// ==================== File Sink ====================

class FileSink(
    private val filePath: String,
    private val formatter: LogFormatter = DefaultLogFormatter(),
    private val maxSizeBytes: Long = 10 * 1024 * 1024, // 10MB
    private val maxFiles: Int = 5
) : LogSink {
    
    private var writer: PrintWriter
    private var currentSize: Long = 0
    private val lock = Any()
    
    init {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        currentSize = if (file.exists()) file.length() else 0
        writer = PrintWriter(FileWriter(file, true))
    }
    
    override fun write(entry: LogEntry) {
        synchronized(lock) {
            val formatted = formatter.format(entry)
            
            // Check if rotation needed
            if (currentSize + formatted.length > maxSizeBytes) {
                rotate()
            }
            
            writer.println(formatted)
            writer.flush()
            currentSize += formatted.length + 1
        }
    }
    
    private fun rotate() {
        writer.close()
        
        // Rotate files: log.4 -> delete, log.3 -> log.4, etc.
        for (i in maxFiles - 1 downTo 1) {
            val older = File("$filePath.$i")
            val newer = if (i == 1) File(filePath) else File("$filePath.${i - 1}")
            if (newer.exists()) {
                if (older.exists()) older.delete()
                newer.renameTo(older)
            }
        }
        
        // Create new file
        writer = PrintWriter(FileWriter(filePath))
        currentSize = 0
    }
    
    override fun close() {
        synchronized(lock) {
            writer.close()
        }
    }
}

// ==================== Async Sink (Decorator) ====================

/**
 * Decorator that adds asynchronous behavior to any LogSink.
 * 
 * === Why Async Logging? ===
 * Synchronous logging blocks the application thread during I/O:
 *   App Thread: [──work──][LOG][──work──][LOG][──work──]
 *                         ↑ blocks!
 * 
 * Async logging queues log entries for background processing:
 *   App Thread: [──work──][Q][────work────][Q][────work────]
 *   Log Thread:        [LOG]           [LOG]
 *                      ↑ non-blocking!
 * 
 * === Implementation ===
 * - Uses BlockingQueue to buffer log entries
 * - Dedicated daemon thread processes queue
 * - Falls back to sync write if queue is full
 * 
 * === Thread Safety ===
 * - LinkedBlockingQueue handles concurrent access
 * - AtomicBoolean for safe shutdown signaling
 * 
 * === Decorator Pattern ===
 * Wraps any LogSink without modifying it:
 *   AsyncSink(FileSink("app.log"))
 *   AsyncSink(ConsoleSink())
 * 
 * @param delegate The actual sink to write to
 * @param queueSize Maximum buffered entries (default 10000)
 */
class AsyncSink(
    private val delegate: LogSink,
    queueSize: Int = 10000
) : LogSink {
    
    // Bounded queue prevents OOM if logging faster than writing
    private val queue = LinkedBlockingQueue<LogEntry>(queueSize)
    private val running = AtomicBoolean(true)
    private val workerThread: Thread
    
    init {
        // Start background worker thread
        workerThread = Thread {
            // Continue until stopped AND queue is drained
            while (running.get() || queue.isNotEmpty()) {
                try {
                    // Poll with timeout to allow periodic shutdown check
                    val entry = queue.poll(100, TimeUnit.MILLISECONDS)
                    entry?.let { delegate.write(it) }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply {
            name = "AsyncLogger-Worker"
            isDaemon = true  // Don't prevent JVM shutdown
            start()
        }
    }
    
    /**
     * Queue entry for async writing.
     * Falls back to sync write if queue is full (backpressure).
     */
    override fun write(entry: LogEntry) {
        if (!queue.offer(entry)) {
            // Queue full - fall back to sync write
            // This prevents message loss at the cost of blocking
            delegate.write(entry)
        }
    }
    
    /**
     * Gracefully shutdown: drain queue, then close delegate.
     */
    override fun close() {
        running.set(false)
        workerThread.join(5000)  // Wait up to 5s for drain
        delegate.close()
    }
}

// ==================== Composite Sink ====================

class CompositeSink(
    private val sinks: List<LogSink>
) : LogSink {
    
    override fun write(entry: LogEntry) {
        sinks.forEach { it.write(entry) }
    }
    
    override fun close() {
        sinks.forEach { it.close() }
    }
}
```

### Logger Class

```kotlin
// ==================== Logger ====================

class Logger private constructor(
    val name: String,
    private var level: LogLevel,
    private val sink: LogSink
) {
    private val contextMap = ThreadLocal<MutableMap<String, String>>()
    
    // ==================== Logging Methods ====================
    
    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun debug(message: () -> String) { if (isEnabled(LogLevel.DEBUG)) log(LogLevel.DEBUG, message()) }
    
    fun info(message: String) = log(LogLevel.INFO, message)
    fun info(message: () -> String) { if (isEnabled(LogLevel.INFO)) log(LogLevel.INFO, message()) }
    
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun warn(message: String, exception: Throwable) = log(LogLevel.WARN, message, exception)
    
    fun error(message: String) = log(LogLevel.ERROR, message)
    fun error(message: String, exception: Throwable) = log(LogLevel.ERROR, message, exception)
    
    fun fatal(message: String) = log(LogLevel.FATAL, message)
    fun fatal(message: String, exception: Throwable) = log(LogLevel.FATAL, message, exception)
    
    fun log(level: LogLevel, message: String, exception: Throwable? = null) {
        if (!isEnabled(level)) return
        
        val entry = LogEntry(
            level = level,
            loggerName = name,
            message = message,
            exception = exception,
            context = contextMap.get()?.toMap() ?: emptyMap()
        )
        
        sink.write(entry)
    }
    
    fun isEnabled(level: LogLevel): Boolean = level.isEnabled(this.level)
    
    fun setLevel(level: LogLevel) {
        this.level = level
    }
    
    // ==================== Context (MDC) ====================
    
    fun putContext(key: String, value: String) {
        val map = contextMap.get() ?: mutableMapOf<String, String>().also { contextMap.set(it) }
        map[key] = value
    }
    
    fun removeContext(key: String) {
        contextMap.get()?.remove(key)
    }
    
    fun clearContext() {
        contextMap.remove()
    }
    
    // ==================== Factory ====================
    
    companion object {
        private val loggers = ConcurrentHashMap<String, Logger>()
        private var defaultSink: LogSink = ConsoleSink()
        private var defaultLevel: LogLevel = LogLevel.INFO
        
        fun getLogger(name: String): Logger {
            return loggers.getOrPut(name) {
                Logger(name, defaultLevel, defaultSink)
            }
        }
        
        fun getLogger(clazz: Class<*>): Logger {
            return getLogger(clazz.name)
        }
        
        inline fun <reified T> getLogger(): Logger {
            return getLogger(T::class.java)
        }
        
        fun configure(sink: LogSink, level: LogLevel = LogLevel.INFO) {
            defaultSink = sink
            defaultLevel = level
            // Update existing loggers
            loggers.values.forEach { it.setLevel(level) }
        }
        
        fun shutdown() {
            loggers.values.forEach { /* cleanup if needed */ }
            defaultSink.close()
        }
    }
}
```

### Logger Builder

```kotlin
// ==================== Logger Configuration ====================

class LoggerConfiguration {
    private var level: LogLevel = LogLevel.INFO
    private val sinks = mutableListOf<LogSink>()
    private var async: Boolean = false
    
    fun level(level: LogLevel) = apply { this.level = level }
    
    fun addConsoleSink(formatter: LogFormatter = DefaultLogFormatter()) = apply {
        sinks.add(ConsoleSink(formatter))
    }
    
    fun addFileSink(
        path: String,
        formatter: LogFormatter = DefaultLogFormatter(),
        maxSize: Long = 10 * 1024 * 1024
    ) = apply {
        sinks.add(FileSink(path, formatter, maxSize))
    }
    
    fun addSink(sink: LogSink) = apply {
        sinks.add(sink)
    }
    
    fun async(enabled: Boolean = true) = apply {
        this.async = enabled
    }
    
    fun configure() {
        require(sinks.isNotEmpty()) { "At least one sink required" }
        
        var compositeSink: LogSink = if (sinks.size == 1) {
            sinks.first()
        } else {
            CompositeSink(sinks)
        }
        
        if (async) {
            compositeSink = AsyncSink(compositeSink)
        }
        
        Logger.configure(compositeSink, level)
    }
}

fun configureLogger(init: LoggerConfiguration.() -> Unit) {
    LoggerConfiguration().apply(init).configure()
}
```

### Usage Example

```kotlin
fun main() {
    // Configure logger
    configureLogger {
        level(LogLevel.DEBUG)
        addConsoleSink()
        addFileSink("logs/app.log", JsonLogFormatter())
        async(true)
    }
    
    // Get logger
    val logger = Logger.getLogger("MainApp")
    
    // Basic logging
    logger.debug("Application starting...")
    logger.info("Server initialized on port 8080")
    logger.warn("Config file not found, using defaults")
    
    // With exception
    try {
        throw RuntimeException("Something went wrong")
    } catch (e: Exception) {
        logger.error("Failed to process request", e)
    }
    
    // Lazy logging (lambda evaluated only if level enabled)
    logger.debug { "Expensive computation: ${computeExpensiveValue()}" }
    
    // Context (MDC)
    logger.putContext("requestId", "req-12345")
    logger.putContext("userId", "user-789")
    logger.info("Processing user request")
    logger.clearContext()
    
    // Class-based logger
    val serviceLogger = Logger.getLogger(UserService::class.java)
    serviceLogger.info("Service ready")
    
    // Shutdown
    Logger.shutdown()
}

fun computeExpensiveValue(): String {
    Thread.sleep(100)
    return "computed"
}

class UserService
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | `LogFormatter`, `LogSink` | Pluggable formatting and output |
| **Decorator** | `AsyncSink` | Add async behavior transparently |
| **Composite** | `CompositeSink` | Multiple sinks as one |
| **Singleton** | `Logger.Companion` | Central logger factory |
| **Builder** | `LoggerConfiguration` | Fluent configuration |
| **Factory** | `Logger.getLogger()` | Create/retrieve loggers |

---

## Interview Discussion Points

### Q: How do you handle high-throughput logging?
**A:**
- Async logging with bounded queue
- Batch writes to file
- Sample logs at high volume
- Use lock-free data structures

### Q: How to implement log aggregation?
**A:**
- Add RemoteSink that sends to Elasticsearch/Splunk
- Use structured logging (JSON)
- Include correlation IDs
- Buffer and batch remote calls

### Q: How to handle sensitive data?
**A:**
- Implement `MaskingFormatter` decorator
- Regex-based PII detection
- Configurable masking patterns
- Audit logging for sensitive operations

---

## Time Complexity

| Operation | Sync | Async |
|-----------|------|-------|
| Log Write | O(1) + I/O | O(1) queue offer |
| Format | O(m) where m = message length |
| File Rotation | O(n) where n = max files |

