/**
 * Task Scheduler - LLD Implementation
 * 
 * Design a Task Scheduler that can schedule and execute tasks at specified times.
 * Supports one-time tasks, recurring tasks, task priorities, and task dependencies.
 * 
 * Features:
 * 1. One-time and recurring task scheduling
 * 2. Task priorities (CRITICAL > HIGH > MEDIUM > LOW)
 * 3. Task dependencies (Task B runs after Task A)
 * 4. Retry with exponential backoff
 * 5. Task lifecycle management
 * 
 * Time Complexity:
 * - Schedule: O(log n) - PriorityQueue insertion
 * - Cancel: O(n) - Queue search
 * - Get next: O(1) - Queue peek
 */
package lld.taskscheduler

import java.time.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.pow

// ==================== Enums ====================

/**
 * Task lifecycle states.
 * 
 * State Transitions:
 * PENDING → SCHEDULED → RUNNING → COMPLETED
 *                    ↘         ↘ (retry)
 *                     → CANCELLED  → FAILED
 * WAITING_FOR_DEPENDENCY → SCHEDULED (when deps complete)
 */
enum class TaskStatus {
    PENDING,                  // Task created but not yet in queue
    SCHEDULED,                // Task is in the priority queue
    RUNNING,                  // Task is currently executing
    COMPLETED,                // Task finished successfully
    FAILED,                   // Task failed after all retries exhausted
    CANCELLED,                // Task was manually cancelled
    WAITING_FOR_DEPENDENCY    // Task waiting for dependent tasks to complete
}

/**
 * Task priority levels for scheduling order.
 * Higher value = higher priority = executes first when times are equal.
 */
enum class Priority(val value: Int) {
    LOW(1),        // Background tasks, batch jobs
    MEDIUM(5),     // Normal tasks (default)
    HIGH(10),      // Important tasks
    CRITICAL(100)  // Urgent tasks, system-critical operations
}

// ==================== Schedule Interface ====================

/**
 * Strategy interface for different scheduling patterns.
 */
interface Schedule {
    fun getNextExecutionTime(from: Instant): Instant?
    fun isRecurring(): Boolean
}

/**
 * One-time schedule - executes once at specified time.
 */
class OneTimeSchedule(
    private val executionTime: Instant
) : Schedule {
    
    private var executed = false
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        if (executed) return null
        return if (executionTime.isAfter(from)) executionTime else null
    }
    
    override fun isRecurring(): Boolean = false
    
    fun markExecuted() {
        executed = true
    }
}

/**
 * Interval schedule - executes repeatedly at fixed intervals.
 * 
 * Example: Every 5 minutes starting now
 */
class IntervalSchedule(
    private val interval: Duration,
    private val startTime: Instant = Instant.now()
) : Schedule {
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        var next = startTime
        while (next.isBefore(from) || next == from) {
            next = next.plus(interval)
        }
        return next
    }
    
    override fun isRecurring(): Boolean = true
}

/**
 * Daily schedule - executes at specific time each day.
 * 
 * Example: Every day at 9:00 AM
 */
class DailySchedule(
    private val timeOfDay: LocalTime,
    private val timezone: ZoneId = ZoneId.systemDefault()
) : Schedule {
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        val fromZoned = from.atZone(timezone)
        var nextDate = fromZoned.toLocalDate()
        var nextDateTime = LocalDateTime.of(nextDate, timeOfDay)
        
        if (nextDateTime.atZone(timezone).toInstant().isBefore(from) ||
            nextDateTime.atZone(timezone).toInstant() == from) {
            nextDate = nextDate.plusDays(1)
            nextDateTime = LocalDateTime.of(nextDate, timeOfDay)
        }
        
        return nextDateTime.atZone(timezone).toInstant()
    }
    
    override fun isRecurring(): Boolean = true
}

/**
 * Weekly schedule - executes on specific day and time each week.
 */
class WeeklySchedule(
    private val dayOfWeek: DayOfWeek,
    private val timeOfDay: LocalTime,
    private val timezone: ZoneId = ZoneId.systemDefault()
) : Schedule {
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        val fromZoned = from.atZone(timezone)
        var nextDate = fromZoned.toLocalDate()
        
        while (nextDate.dayOfWeek != dayOfWeek) {
            nextDate = nextDate.plusDays(1)
        }
        
        var nextDateTime = LocalDateTime.of(nextDate, timeOfDay)
        
        if (nextDateTime.atZone(timezone).toInstant().isBefore(from) ||
            nextDateTime.atZone(timezone).toInstant() == from) {
            nextDate = nextDate.plusWeeks(1)
            nextDateTime = LocalDateTime.of(nextDate, timeOfDay)
        }
        
        return nextDateTime.atZone(timezone).toInstant()
    }
    
    override fun isRecurring(): Boolean = true
}

// ==================== Task ====================

/**
 * Represents a schedulable task.
 * 
 * Tasks are compared for PriorityQueue ordering:
 * 1. First by nextExecutionTime (earlier = higher priority)
 * 2. Then by priority value (higher = comes first)
 * 
 * @property id Unique identifier (auto-generated UUID)
 * @property name Human-readable task name
 * @property command The actual work to execute (lambda)
 * @property schedule When/how often to run
 * @property priority Execution priority for ties
 * @property dependencies Task IDs that must complete first
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: () -> Unit,
    val schedule: Schedule,
    val priority: Priority = Priority.MEDIUM,
    var status: TaskStatus = TaskStatus.PENDING,
    var retryCount: Int = 0,
    val maxRetries: Int = 3,
    val dependencies: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    var lastExecutedAt: Instant? = null,
    var nextExecutionTime: Instant? = null
) : Comparable<Task> {
    
    init {
        nextExecutionTime = schedule.getNextExecutionTime(Instant.now())
    }
    
    /**
     * Comparison for PriorityQueue ordering.
     * 
     * Returns negative if THIS task should execute BEFORE other.
     * 
     * Example:
     *   Task A: time=10:00, priority=LOW(1)
     *   Task B: time=10:00, priority=HIGH(10)
     *   Result: B executes first (10 > 1)
     */
    override fun compareTo(other: Task): Int {
        // First compare by execution time (earlier first)
        val timeComparison = compareValues(nextExecutionTime, other.nextExecutionTime)
        if (timeComparison != 0) return timeComparison
        
        // Then by priority (higher value first - note reversed comparison)
        return other.priority.value.compareTo(priority.value)
    }
}

data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val executionTime: Duration,
    val error: Throwable? = null
)

// ==================== Task Listener (Observer Pattern) ====================

interface TaskListener {
    fun onTaskScheduled(task: Task)
    fun onTaskStarted(task: Task)
    fun onTaskCompleted(task: Task, result: TaskResult)
    fun onTaskFailed(task: Task, result: TaskResult)
    fun onTaskCancelled(task: Task)
}

open class TaskListenerAdapter : TaskListener {
    override fun onTaskScheduled(task: Task) {}
    override fun onTaskStarted(task: Task) {}
    override fun onTaskCompleted(task: Task, result: TaskResult) {}
    override fun onTaskFailed(task: Task, result: TaskResult) {}
    override fun onTaskCancelled(task: Task) {}
}

// ==================== Task Executor ====================

/**
 * Executes tasks and manages execution lifecycle.
 * 
 * Execution Flow:
 * 1. Notify listeners: task started
 * 2. Set status to RUNNING
 * 3. Execute task.command()
 * 4. On success: status = COMPLETED, notify
 * 5. On failure: status = FAILED, increment retryCount, notify
 */
class TaskExecutor(
    private val listeners: MutableList<TaskListener> = mutableListOf()
) {
    
    fun execute(task: Task): TaskResult {
        notifyStarted(task)
        
        val startTime = Instant.now()
        
        return try {
            task.status = TaskStatus.RUNNING
            task.command()
            task.lastExecutedAt = Instant.now()
            task.status = TaskStatus.COMPLETED
            
            val result = TaskResult(
                taskId = task.id,
                success = true,
                executionTime = Duration.between(startTime, Instant.now())
            )
            notifyCompleted(task, result)
            result
            
        } catch (e: Exception) {
            task.status = TaskStatus.FAILED
            task.retryCount++
            
            val result = TaskResult(
                taskId = task.id,
                success = false,
                executionTime = Duration.between(startTime, Instant.now()),
                error = e
            )
            notifyFailed(task, result)
            result
        }
    }
    
    fun addListener(listener: TaskListener) {
        listeners.add(listener)
    }
    
    private fun notifyStarted(task: Task) = listeners.forEach { it.onTaskStarted(task) }
    private fun notifyCompleted(task: Task, result: TaskResult) = 
        listeners.forEach { it.onTaskCompleted(task, result) }
    private fun notifyFailed(task: Task, result: TaskResult) = 
        listeners.forEach { it.onTaskFailed(task, result) }
}

// ==================== Retry Strategy ====================

/**
 * Strategy interface for calculating retry delays.
 */
interface RetryStrategy {
    fun getNextRetryDelay(retryCount: Int): Duration?
}

/**
 * Exponential backoff retry strategy.
 * 
 * Formula: delay = baseDelay * (multiplier ^ retryCount)
 * Capped at maxDelay.
 * 
 * Example (default values):
 * Retry 0: 1000 * 2^0 = 1000ms (1 second)
 * Retry 1: 1000 * 2^1 = 2000ms (2 seconds)
 * Retry 2: 1000 * 2^2 = 4000ms (4 seconds)
 * ...capped at 60 seconds
 */
class ExponentialBackoffRetry(
    private val baseDelayMs: Long = 1000,
    private val maxDelayMs: Long = 60000,
    private val multiplier: Double = 2.0
) : RetryStrategy {
    
    override fun getNextRetryDelay(retryCount: Int): Duration {
        val delay = (baseDelayMs * multiplier.pow(retryCount.toDouble())).toLong()
        return Duration.ofMillis(minOf(delay, maxDelayMs))
    }
}

/**
 * Fixed delay retry - same delay every time.
 */
class FixedDelayRetry(
    private val delayMs: Long = 5000
) : RetryStrategy {
    
    override fun getNextRetryDelay(retryCount: Int): Duration {
        return Duration.ofMillis(delayMs)
    }
}

// ==================== Task Scheduler ====================

/**
 * Main Task Scheduler class.
 * 
 * === Code Flow: schedule(task) ===
 * 1. Check dependencies - if not met, status = WAITING_FOR_DEPENDENCY
 * 2. Register task in taskRegistry
 * 3. If ready, add to priority queue
 * 4. Notify listeners
 * 
 * === Code Flow: processQueue() (runs every 100ms) ===
 * 1. Check tasks waiting for dependencies
 * 2. Poll tasks from queue where scheduledTime <= now
 * 3. Submit each task to executor thread pool
 * 
 * === Code Flow: executeTask(task) ===
 * 1. Execute via TaskExecutor
 * 2. On success: reschedule if recurring, check dependent tasks
 * 3. On failure: schedule retry with backoff, or mark FAILED
 */
class TaskScheduler(
    private val threadPoolSize: Int = 4,
    private val retryStrategy: RetryStrategy = ExponentialBackoffRetry()
) {
    private val taskQueue = PriorityBlockingQueue<Task>()
    private val taskRegistry = ConcurrentHashMap<String, Task>()
    private val completedTasks = ConcurrentHashMap<String, TaskResult>()
    
    private val executorService = Executors.newFixedThreadPool(threadPoolSize)
    private val schedulerThread = Executors.newSingleThreadScheduledExecutor()
    
    private val taskExecutor = TaskExecutor()
    private val running = AtomicBoolean(false)
    private val lock = ReentrantLock()
    
    private val listeners = mutableListOf<TaskListener>()
    
    // ==================== Public API ====================
    
    fun schedule(task: Task): String {
        lock.withLock {
            // Check if dependencies are met
            if (!areDependenciesMet(task)) {
                task.status = TaskStatus.WAITING_FOR_DEPENDENCY
            } else {
                task.status = TaskStatus.SCHEDULED
            }
            
            taskRegistry[task.id] = task
            
            if (task.status == TaskStatus.SCHEDULED) {
                taskQueue.offer(task)
            }
            
            listeners.forEach { it.onTaskScheduled(task) }
            return task.id
        }
    }
    
    fun scheduleWithDelay(
        name: String,
        delay: Duration,
        priority: Priority = Priority.MEDIUM,
        command: () -> Unit
    ): String {
        val task = Task(
            name = name,
            command = command,
            schedule = OneTimeSchedule(Instant.now().plus(delay)),
            priority = priority
        )
        return schedule(task)
    }
    
    fun scheduleRecurring(
        name: String,
        interval: Duration,
        priority: Priority = Priority.MEDIUM,
        command: () -> Unit
    ): String {
        val task = Task(
            name = name,
            command = command,
            schedule = IntervalSchedule(interval),
            priority = priority
        )
        return schedule(task)
    }
    
    fun cancel(taskId: String): Boolean {
        lock.withLock {
            val task = taskRegistry[taskId] ?: return false
            
            if (task.status == TaskStatus.RUNNING) {
                return false // Can't cancel running task
            }
            
            task.status = TaskStatus.CANCELLED
            taskQueue.remove(task)
            listeners.forEach { it.onTaskCancelled(task) }
            return true
        }
    }
    
    fun getTaskStatus(taskId: String): TaskStatus? = taskRegistry[taskId]?.status
    
    fun getTask(taskId: String): Task? = taskRegistry[taskId]
    
    fun getAllTasks(): List<Task> = taskRegistry.values.toList()
    
    fun getPendingTasks(): List<Task> = taskRegistry.values.filter { 
        it.status in listOf(TaskStatus.PENDING, TaskStatus.SCHEDULED) 
    }
    
    fun addListener(listener: TaskListener) {
        listeners.add(listener)
        taskExecutor.addListener(listener)
    }
    
    // ==================== Lifecycle ====================
    
    fun start() {
        if (running.getAndSet(true)) return
        
        schedulerThread.scheduleAtFixedRate(
            { processQueue() },
            0,
            100, // Check every 100ms
            TimeUnit.MILLISECONDS
        )
        
        println("TaskScheduler started")
    }
    
    fun shutdown() {
        running.set(false)
        schedulerThread.shutdown()
        executorService.shutdown()
        
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
        
        println("TaskScheduler shutdown complete")
    }
    
    // ==================== Internal Processing ====================
    
    private fun processQueue() {
        if (!running.get()) return
        
        val now = Instant.now()
        
        // Check for tasks waiting on dependencies
        checkDependencies()
        
        // Process ready tasks
        while (true) {
            val task = taskQueue.peek() ?: break
            
            val nextTime = task.nextExecutionTime ?: break
            if (nextTime.isAfter(now)) break
            
            taskQueue.poll()
            
            if (task.status == TaskStatus.CANCELLED) continue
            
            executorService.submit {
                executeTask(task)
            }
        }
    }
    
    private fun executeTask(task: Task) {
        val result = taskExecutor.execute(task)
        
        if (result.success) {
            handleSuccessfulExecution(task)
        } else {
            handleFailedExecution(task, result)
        }
    }
    
    private fun handleSuccessfulExecution(task: Task) {
        completedTasks[task.id] = TaskResult(task.id, true, Duration.ZERO)
        
        // If recurring, schedule next execution
        if (task.schedule.isRecurring()) {
            task.nextExecutionTime = task.schedule.getNextExecutionTime(Instant.now())
            if (task.nextExecutionTime != null) {
                task.status = TaskStatus.SCHEDULED
                task.retryCount = 0
                taskQueue.offer(task)
            }
        }
        
        // Check if any dependent tasks can now run
        checkDependencies()
    }
    
    private fun handleFailedExecution(task: Task, result: TaskResult) {
        if (task.retryCount < task.maxRetries) {
            // Schedule retry with backoff
            val retryDelay = retryStrategy.getNextRetryDelay(task.retryCount)
            task.nextExecutionTime = Instant.now().plus(retryDelay)
            task.status = TaskStatus.SCHEDULED
            taskQueue.offer(task)
            
            println("Task ${task.name} failed, retry ${task.retryCount}/${task.maxRetries} in ${retryDelay?.toMillis()}ms")
        } else {
            println("Task ${task.name} failed after ${task.maxRetries} retries")
            completedTasks[task.id] = result
        }
    }
    
    private fun checkDependencies() {
        taskRegistry.values
            .filter { it.status == TaskStatus.WAITING_FOR_DEPENDENCY }
            .forEach { task ->
                if (areDependenciesMet(task)) {
                    task.status = TaskStatus.SCHEDULED
                    task.nextExecutionTime = task.schedule.getNextExecutionTime(Instant.now())
                    taskQueue.offer(task)
                }
            }
    }
    
    private fun areDependenciesMet(task: Task): Boolean {
        return task.dependencies.all { depId ->
            val depResult = completedTasks[depId]
            depResult != null && depResult.success
        }
    }
}

// ==================== Usage Example ====================

fun main() {
    val scheduler = TaskScheduler(threadPoolSize = 2)
    
    scheduler.addListener(object : TaskListenerAdapter() {
        override fun onTaskStarted(task: Task) {
            println("[${Instant.now()}] Started: ${task.name}")
        }
        
        override fun onTaskCompleted(task: Task, result: TaskResult) {
            println("[${Instant.now()}] Completed: ${task.name} in ${result.executionTime.toMillis()}ms")
        }
        
        override fun onTaskFailed(task: Task, result: TaskResult) {
            println("[${Instant.now()}] Failed: ${task.name} - ${result.error?.message}")
        }
    })
    
    scheduler.start()
    
    // Schedule a one-time task
    scheduler.scheduleWithDelay(
        name = "Send Welcome Email",
        delay = Duration.ofSeconds(2),
        priority = Priority.HIGH
    ) {
        println("Sending welcome email...")
        Thread.sleep(500)
    }
    
    // Schedule a recurring task
    scheduler.scheduleRecurring(
        name = "Health Check",
        interval = Duration.ofSeconds(5),
        priority = Priority.LOW
    ) {
        println("Performing health check...")
    }
    
    // Schedule task with dependencies
    val task1Id = scheduler.schedule(Task(
        name = "Download Data",
        command = { 
            println("Downloading data...")
            Thread.sleep(1000)
        },
        schedule = OneTimeSchedule(Instant.now().plusSeconds(1))
    ))
    
    scheduler.schedule(Task(
        name = "Process Data",
        command = { println("Processing downloaded data...") },
        schedule = OneTimeSchedule(Instant.now().plusSeconds(1)),
        dependencies = listOf(task1Id)
    ))
    
    // Schedule a task that will fail and retry
    var failCount = 0
    scheduler.schedule(Task(
        name = "Flaky Task",
        command = {
            failCount++
            if (failCount < 3) {
                throw RuntimeException("Simulated failure $failCount")
            }
            println("Flaky task finally succeeded!")
        },
        schedule = OneTimeSchedule(Instant.now().plusSeconds(3)),
        maxRetries = 5
    ))
    
    // Keep running for demo
    Thread.sleep(30000)
    scheduler.shutdown()
}

