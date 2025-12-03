# Task Scheduler - LLD

## Problem Statement
Design a Task Scheduler that can schedule and execute tasks at specified times. Support one-time tasks, recurring tasks, task priorities, and task dependencies.

---

## Requirements

### Functional Requirements
1. Schedule one-time tasks for future execution
2. Schedule recurring tasks (daily, weekly, cron-like)
3. Support task priorities
4. Handle task dependencies (Task B runs after Task A)
5. Cancel scheduled tasks
6. Retry failed tasks with backoff

### Non-Functional Requirements
1. Tasks should execute at or near scheduled time
2. Thread-safe for concurrent scheduling
3. Persist tasks for recovery after restart
4. Scalable to handle many tasks

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│              Task                        │
├─────────────────────────────────────────┤
│ - id: String                            │
│ - name: String                          │
│ - command: () -> Unit                   │
│ - scheduledTime: Instant                │
│ - priority: Priority                    │
│ - status: TaskStatus                    │
│ - retryCount: Int                       │
│ - maxRetries: Int                       │
│ - dependencies: List<String>            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│         <<interface>>                   │
│           Schedule                      │
├─────────────────────────────────────────┤
│ + getNextExecutionTime(): Instant?      │
│ + isRecurring(): Boolean                │
└─────────────────────────────────────────┘
         △
         │
    ┌────┴────┬─────────────┐
    │         │             │
┌───┴───┐ ┌───┴───┐  ┌──────┴──────┐
│OneTime│ │Daily  │  │CronSchedule │
│Schedule│ │Schedule│  │             │
└───────┘ └───────┘  └─────────────┘

┌─────────────────────────────────────────┐
│           TaskScheduler                 │
├─────────────────────────────────────────┤
│ - taskQueue: PriorityQueue<Task>        │
│ - executorService: ExecutorService      │
│ - taskRegistry: Map<String, Task>       │
├─────────────────────────────────────────┤
│ + schedule(task: Task)                  │
│ + cancel(taskId: String)                │
│ + start()                               │
│ + shutdown()                            │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│         TaskExecutor                    │
├─────────────────────────────────────────┤
│ + execute(task: Task): TaskResult       │
│ + retry(task: Task)                     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│    <<interface>> TaskListener           │
├─────────────────────────────────────────┤
│ + onTaskStarted(task: Task)             │
│ + onTaskCompleted(task: Task)           │
│ + onTaskFailed(task: Task, error: Ex)   │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Data Classes and Enums

```kotlin
import java.time.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.pow

// ==================== Enums ====================

enum class TaskStatus {
    PENDING,
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    WAITING_FOR_DEPENDENCY
}

enum class Priority(val value: Int) {
    LOW(1),
    MEDIUM(5),
    HIGH(10),
    CRITICAL(100)
}

// ==================== Task ====================

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
    
    override fun compareTo(other: Task): Int {
        // First compare by execution time
        val timeComparison = compareValues(nextExecutionTime, other.nextExecutionTime)
        if (timeComparison != 0) return timeComparison
        
        // Then by priority (higher priority first)
        return other.priority.value.compareTo(priority.value)
    }
}

data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val executionTime: Duration,
    val error: Throwable? = null
)
```

### Schedule Implementations

```kotlin
// ==================== Schedule Interface ====================

interface Schedule {
    fun getNextExecutionTime(from: Instant): Instant?
    fun isRecurring(): Boolean
}

// ==================== One-Time Schedule ====================

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

// ==================== Interval Schedule ====================

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

// ==================== Daily Schedule ====================

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

// ==================== Weekly Schedule ====================

class WeeklySchedule(
    private val dayOfWeek: DayOfWeek,
    private val timeOfDay: LocalTime,
    private val timezone: ZoneId = ZoneId.systemDefault()
) : Schedule {
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        val fromZoned = from.atZone(timezone)
        var nextDate = fromZoned.toLocalDate()
        
        // Find next occurrence of the day
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

// ==================== Cron Schedule (Simplified) ====================

class CronSchedule(
    private val cronExpression: String // Format: "minute hour dayOfMonth month dayOfWeek"
) : Schedule {
    
    private val parts = cronExpression.split(" ")
    
    init {
        require(parts.size == 5) { "Invalid cron expression" }
    }
    
    override fun getNextExecutionTime(from: Instant): Instant? {
        // Simplified implementation - just handle basic cases
        val minute = parseField(parts[0], 0..59)
        val hour = parseField(parts[1], 0..23)
        
        val fromZoned = from.atZone(ZoneId.systemDefault())
        var candidate = fromZoned.withMinute(minute.first()).withSecond(0).withNano(0)
        
        if (hour.isNotEmpty()) {
            candidate = candidate.withHour(hour.first())
        }
        
        while (candidate.toInstant().isBefore(from) || candidate.toInstant() == from) {
            candidate = candidate.plusDays(1)
        }
        
        return candidate.toInstant()
    }
    
    private fun parseField(field: String, range: IntRange): List<Int> {
        return when {
            field == "*" -> range.toList()
            field.contains(",") -> field.split(",").map { it.toInt() }
            field.contains("-") -> {
                val (start, end) = field.split("-").map { it.toInt() }
                (start..end).toList()
            }
            else -> listOf(field.toInt())
        }
    }
    
    override fun isRecurring(): Boolean = true
}
```

### Task Executor

```kotlin
// ==================== Task Listener ====================

interface TaskListener {
    fun onTaskScheduled(task: Task)
    fun onTaskStarted(task: Task)
    fun onTaskCompleted(task: Task, result: TaskResult)
    fun onTaskFailed(task: Task, result: TaskResult)
    fun onTaskCancelled(task: Task)
}

// Default implementation for convenience
open class TaskListenerAdapter : TaskListener {
    override fun onTaskScheduled(task: Task) {}
    override fun onTaskStarted(task: Task) {}
    override fun onTaskCompleted(task: Task, result: TaskResult) {}
    override fun onTaskFailed(task: Task, result: TaskResult) {}
    override fun onTaskCancelled(task: Task) {}
}

// ==================== Task Executor ====================

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

interface RetryStrategy {
    fun getNextRetryDelay(retryCount: Int): Duration?
}

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

class FixedDelayRetry(
    private val delayMs: Long = 5000
) : RetryStrategy {
    
    override fun getNextRetryDelay(retryCount: Int): Duration {
        return Duration.ofMillis(delayMs)
    }
}
```

### Main Task Scheduler

```kotlin
// ==================== Task Scheduler ====================

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
    
    fun getTaskStatus(taskId: String): TaskStatus? {
        return taskRegistry[taskId]?.status
    }
    
    fun getTask(taskId: String): Task? {
        return taskRegistry[taskId]
    }
    
    fun getAllTasks(): List<Task> {
        return taskRegistry.values.toList()
    }
    
    fun getPendingTasks(): List<Task> {
        return taskRegistry.values.filter { 
            it.status in listOf(TaskStatus.PENDING, TaskStatus.SCHEDULED) 
        }
    }
    
    fun addListener(listener: TaskListener) {
        listeners.add(listener)
        taskExecutor.addListener(listener)
    }
    
    // ==================== Lifecycle ====================
    
    fun start() {
        if (running.getAndSet(true)) return
        
        // Schedule the main processing loop
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
            
            // Submit task for execution
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
            
            println("Task ${task.name} failed, retry ${task.retryCount}/${task.maxRetries} in ${retryDelay.toMillis()}ms")
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
```

### Usage Example

```kotlin
fun main() {
    val scheduler = TaskScheduler(threadPoolSize = 2)
    
    // Add a listener for monitoring
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
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | `Schedule` interface, `RetryStrategy` | Different scheduling/retry behaviors |
| **Observer** | `TaskListener` | Notify interested parties of task events |
| **Command** | `Task.command` | Encapsulate task execution |
| **Factory** | Schedule creation | Create different schedule types |
| **State** | `TaskStatus` | Manage task lifecycle states |

---

## State Diagram

```
                    ┌─────────┐
                    │ PENDING │
                    └────┬────┘
                         │ schedule()
                         ▼
          ┌──────────────────────────────┐
          │                              │
          ▼                              ▼
┌─────────────────┐            ┌─────────────────────────┐
│   SCHEDULED     │            │ WAITING_FOR_DEPENDENCY  │
└────────┬────────┘            └───────────┬─────────────┘
         │ execute()                       │ dependencies met
         ▼                                 │
   ┌───────────┐                          │
   │  RUNNING  │◄─────────────────────────┘
   └─────┬─────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│COMPLETED│ │ FAILED │──retry──┐
└────────┘ └────────┘          │
                               ▼
                         ┌───────────┐
                         │ SCHEDULED │
                         └───────────┘

     cancel() from any state
            │
            ▼
      ┌───────────┐
      │ CANCELLED │
      └───────────┘
```

---

## Interview Discussion Points

### Q: How do you handle task persistence?
**A:** Options include:
1. Database storage (PostgreSQL, MySQL)
2. Redis for fast access
3. Write-ahead logging
4. Checkpoint mechanism for recovery

### Q: How would you scale this?
**A:** 
1. Distributed task queue (Kafka, RabbitMQ)
2. Multiple scheduler instances with leader election
3. Partitioned task queues by priority/type
4. Separate workers for task execution

### Q: How do you prevent duplicate execution?
**A:**
1. Idempotent task design
2. Distributed locks (Redis/Zookeeper)
3. Task deduplication by ID
4. At-least-once vs exactly-once semantics

### Q: How do you handle long-running tasks?
**A:**
1. Heartbeat mechanism
2. Task timeout configuration
3. Checkpoint/resume capability
4. Separate thread pool for long tasks

---

## Time & Space Complexity

| Operation | Time Complexity |
|-----------|----------------|
| Schedule task | O(log n) - PriorityQueue insertion |
| Cancel task | O(n) - Queue search |
| Get next task | O(1) - Queue peek |
| Check dependencies | O(d) - d = number of dependencies |

---

## Edge Cases

1. **Clock changes** (DST, NTP sync)
2. **Scheduler restart** - recover pending tasks
3. **Circular dependencies** - detect and reject
4. **Task execution longer than interval**
5. **Memory pressure** with many scheduled tasks
6. **Timezone handling** for global deployments

