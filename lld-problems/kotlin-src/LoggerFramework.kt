/**
 * Logger Framework - LLD Implementation
 * 
 * Design a logging framework with:
 * - Multiple log levels (DEBUG, INFO, WARN, ERROR, FATAL)
 * - Multiple sinks (Console, File, Remote)
 * - Async logging support
 * - Log rotation
 * - Configurable formatting
 * 
 * Design Patterns: Strategy, Decorator, Composite, Singleton, Builder
 */
package lld.logger

import java.io.FileWriter
import java.io.PrintWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

// ==================== Log Level ====================

/**
 * Log levels in order of severity.
 * Messages are logged only if their level >= logger's configured level.
 */
enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4)
}

// ==================== Log Entry ====================

/**
 * Represents a single log entry with all context.
 */
data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String,
    val loggerName: String,
    val threadName: String = Thread.currentThread().name,
    val throwable: Throwable? = null,
    val context: Map<String, String> = emptyMap()
)

// ==================== Log Formatter ====================

/**
 * Strategy interface for formatting log entries.
 */
interface LogFormatter {
    fun format(entry: LogEntry): String
}

/**
 * Simple text formatter.
 * Output: [2024-01-15 10:30:45] [INFO] [LoggerName] Message
 */
class SimpleFormatter : LogFormatter {
    private val dateFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    
    override fun format(entry: LogEntry): String {
        val sb = StringBuilder()
        sb.append("[${dateFormatter.format(entry.timestamp)}] ")
        sb.append("[${entry.level.name.padEnd(5)}] ")
        sb.append("[${entry.loggerName}] ")
        sb.append(entry.message)
        
        entry.throwable?.let { ex ->
            sb.append("\n${ex.javaClass.name}: ${ex.message}")
            ex.stackTrace.take(10).forEach { frame ->
                sb.append("\n    at $frame")
            }
        }
        
        return sb.toString()
    }
}

/**
 * JSON formatter for structured logging.
 */
class JsonFormatter : LogFormatter {
    override fun format(entry: LogEntry): String {
        val parts = mutableListOf(
            "\"timestamp\":\"${entry.timestamp}\"",
            "\"level\":\"${entry.level}\"",
            "\"logger\":\"${entry.loggerName}\"",
            "\"thread\":\"${entry.threadName}\"",
            "\"message\":\"${escapeJson(entry.message)}\""
        )
        
        if (entry.context.isNotEmpty()) {
            val contextJson = entry.context.entries.joinToString(",") { 
                "\"${it.key}\":\"${escapeJson(it.value)}\"" 
            }
            parts.add("\"context\":{$contextJson}")
        }
        
        entry.throwable?.let { ex ->
            parts.add("\"error\":\"${escapeJson(ex.toString())}\"")
        }
        
        return "{${parts.joinToString(",")}}"
    }
    
    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

// ==================== Log Sink ====================

/**
 * Strategy interface for log output destinations.
 */
interface LogSink {
    fun write(entry: LogEntry)
    fun close()
}

/**
 * Console sink - writes to stdout/stderr.
 */
class ConsoleSink(
    private val formatter: LogFormatter = SimpleFormatter(),
    private val useStderr: Boolean = false
) : LogSink {
    
    override fun write(entry: LogEntry) {
        val output = formatter.format(entry)
        if (useStderr || entry.level.value >= LogLevel.ERROR.value) {
            System.err.println(output)
        } else {
            println(output)
        }
    }
    
    override fun close() {} // Nothing to close
}

/**
 * File sink - writes to a file.
 */
class FileSink(
    private val filePath: String,
    private val formatter: LogFormatter = SimpleFormatter(),
    private val append: Boolean = true
) : LogSink {
    
    private var writer: PrintWriter? = null
    
    init {
        writer = PrintWriter(FileWriter(filePath, append), true)
    }
    
    override fun write(entry: LogEntry) {
        writer?.println(formatter.format(entry))
    }
    
    override fun close() {
        writer?.close()
    }
}

/**
 * Async sink - wraps another sink for non-blocking writes.
 * 
 * === How It Works ===
 * - Uses bounded queue to buffer log entries
 * - Dedicated daemon thread processes queue
 * - Falls back to sync write if queue is full
 * 
 * === Code Flow ===
 * 1. write() offers entry to queue (non-blocking)
 * 2. Worker thread polls queue and writes to delegate
 * 3. On shutdown, drains queue before closing
 */
class AsyncSink(
    private val delegate: LogSink,
    queueSize: Int = 10000
) : LogSink {
    
    private val queue = LinkedBlockingQueue<LogEntry>(queueSize)
    private val running = AtomicBoolean(true)
    private val workerThread: Thread
    
    init {
        workerThread = Thread {
            while (running.get() || queue.isNotEmpty()) {
                try {
                    val entry = queue.poll(100, TimeUnit.MILLISECONDS)
                    entry?.let { delegate.write(it) }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }.apply {
            name = "AsyncLogger-Worker"
            isDaemon = true
            start()
        }
    }
    
    override fun write(entry: LogEntry) {
        if (!queue.offer(entry)) {
            // Queue full - fall back to sync write
            delegate.write(entry)
        }
    }
    
    override fun close() {
        running.set(false)
        workerThread.join(5000)
        delegate.close()
    }
}

/**
 * Composite sink - writes to multiple sinks.
 */
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

// ==================== Logger ====================

/**
 * Main Logger class.
 * 
 * === Code Flow: log(level, message) ===
 * 1. Check if level >= configured level
 * 2. Create LogEntry with all context
 * 3. Format the entry
 * 4. Write to all sinks
 */
class Logger(
    val name: String,
    var level: LogLevel = LogLevel.INFO,
    private val sinks: MutableList<LogSink> = mutableListOf()
) {
    
    fun addSink(sink: LogSink) {
        sinks.add(sink)
    }
    
    fun debug(message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, message, throwable)
    }
    
    fun info(message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, message, throwable)
    }
    
    fun warn(message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, message, throwable)
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message, throwable)
    }
    
    fun fatal(message: String, throwable: Throwable? = null) {
        log(LogLevel.FATAL, message, throwable)
    }
    
    fun log(msgLevel: LogLevel, message: String, throwable: Throwable? = null) {
        // Filter by level
        if (msgLevel.value < level.value) return
        
        val entry = LogEntry(
            timestamp = Instant.now(),
            level = msgLevel,
            message = message,
            loggerName = name,
            throwable = throwable,
            context = MDC.getCopyOfContext()
        )
        
        sinks.forEach { it.write(entry) }
    }
    
    fun close() {
        sinks.forEach { it.close() }
    }
}

// ==================== MDC (Mapped Diagnostic Context) ====================

/**
 * Thread-local context for adding metadata to log entries.
 * 
 * Usage:
 *   MDC.put("requestId", "abc-123")
 *   MDC.put("userId", "user-456")
 *   logger.info("Processing request")  // Includes requestId and userId
 *   MDC.clear()
 */
object MDC {
    private val context = ThreadLocal<MutableMap<String, String>>()
    
    fun put(key: String, value: String) {
        getOrCreate()[key] = value
    }
    
    fun get(key: String): String? = context.get()?.get(key)
    
    fun remove(key: String) {
        context.get()?.remove(key)
    }
    
    fun clear() {
        context.remove()
    }
    
    fun getCopyOfContext(): Map<String, String> {
        return context.get()?.toMap() ?: emptyMap()
    }
    
    private fun getOrCreate(): MutableMap<String, String> {
        var map = context.get()
        if (map == null) {
            map = mutableMapOf()
            context.set(map)
        }
        return map
    }
}

// ==================== Logger Factory ====================

/**
 * Factory for creating and managing loggers.
 * 
 * Usage:
 *   val logger = LoggerFactory.getLogger("MyClass")
 *   logger.info("Hello!")
 */
object LoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()
    private var defaultLevel = LogLevel.INFO
    private var defaultSinks = mutableListOf<LogSink>(ConsoleSink())
    
    fun getLogger(name: String): Logger {
        return loggers.getOrPut(name) {
            Logger(name, defaultLevel, defaultSinks.toMutableList())
        }
    }
    
    fun getLogger(clazz: Class<*>): Logger {
        return getLogger(clazz.simpleName)
    }
    
    fun setDefaultLevel(level: LogLevel) {
        defaultLevel = level
    }
    
    fun addDefaultSink(sink: LogSink) {
        defaultSinks.add(sink)
    }
    
    fun configure(init: LoggerConfig.() -> Unit) {
        val config = LoggerConfig().apply(init)
        defaultLevel = config.level
        defaultSinks = config.sinks.toMutableList()
    }
}

class LoggerConfig {
    var level: LogLevel = LogLevel.INFO
    val sinks = mutableListOf<LogSink>()
    
    fun console(formatter: LogFormatter = SimpleFormatter()) {
        sinks.add(ConsoleSink(formatter))
    }
    
    fun file(path: String, formatter: LogFormatter = SimpleFormatter()) {
        sinks.add(FileSink(path, formatter))
    }
    
    fun async(sink: LogSink) {
        sinks.add(AsyncSink(sink))
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Logger Framework Example ===\n")
    
    // Configure logger factory
    LoggerFactory.configure {
        level = LogLevel.DEBUG
        console(SimpleFormatter())
    }
    
    // Get logger
    val logger = LoggerFactory.getLogger("Main")
    
    // Basic logging
    logger.debug("This is a debug message")
    logger.info("This is an info message")
    logger.warn("This is a warning")
    logger.error("This is an error")
    
    println()
    
    // Logging with MDC context
    MDC.put("requestId", "req-12345")
    MDC.put("userId", "user-789")
    
    logger.info("Processing user request")
    logger.info("Request completed")
    
    MDC.clear()
    
    println()
    
    // Logging with exception
    try {
        throw RuntimeException("Something went wrong!")
    } catch (e: Exception) {
        logger.error("Failed to process", e)
    }
    
    println()
    
    // JSON formatter example
    val jsonLogger = Logger(
        name = "JsonLogger",
        level = LogLevel.INFO,
        sinks = mutableListOf(ConsoleSink(JsonFormatter()))
    )
    
    println("JSON formatted log:")
    jsonLogger.info("User logged in")
    
    // Cleanup
    logger.close()
    jsonLogger.close()
}

