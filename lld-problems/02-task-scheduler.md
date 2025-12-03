# Task Scheduler - LLD

## Problem Statement
Design a Task Scheduler that can schedule and execute tasks at specified times. Support one-time tasks, recurring tasks, task priorities, and task dependencies.

---

## Flow Diagrams

### High-Level Task Scheduler Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TASK SCHEDULER OVERVIEW                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌────────────┐     ┌─────────────────┐     ┌──────────────────┐   │
│   │   Client   │────▶│  TaskScheduler  │────▶│  ExecutorService │   │
│   │ schedule() │     │                 │     │   (Thread Pool)  │   │
│   └────────────┘     │ ┌─────────────┐ │     └──────────────────┘   │
│                      │ │  Priority   │ │             │              │
│                      │ │   Queue     │ │             ▼              │
│                      │ │ ┌─────────┐ │ │     ┌──────────────────┐   │
│                      │ │ │Task 1   │ │ │     │  Worker Thread   │   │
│                      │ │ │(HIGH)   │◀─┼──────│  executes task   │   │
│                      │ │ ├─────────┤ │ │     └──────────────────┘   │
│                      │ │ │Task 2   │ │ │                            │
│                      │ │ │(MEDIUM) │ │ │                            │
│                      │ │ ├─────────┤ │ │                            │
│                      │ │ │Task 3   │ │ │                            │
│                      │ │ │(LOW)    │ │ │                            │
│                      │ │ └─────────┘ │ │                            │
│                      │ └─────────────┘ │                            │
│                      └─────────────────┘                            │
│                                                                      │
│   Scheduler Thread (runs continuously):                             │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │ while (running):                                             │  │
│   │   1. Peek top of priority queue                              │  │
│   │   2. If task.scheduledTime <= now:                           │  │
│   │        - Remove from queue                                   │  │
│   │        - Submit to executor                                  │  │
│   │   3. Else: wait until next task time                         │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Task Scheduling Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SCHEDULE TASK FLOW                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   scheduler.schedule(task)                                          │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │ Validate    │                                                   │
│   │ Task        │                                                   │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                Has Dependencies?                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│         │                           │                                │
│        Yes                         No                                │
│         │                           │                                │
│         ▼                           │                                │
│   ┌─────────────────┐               │                                │
│   │Check all deps   │               │                                │
│   │completed?       │               │                                │
│   └─────────────────┘               │                                │
│         │                           │                                │
│    Yes ─┼─ No                       │                                │
│         │    │                      │                                │
│         │    ▼                      │                                │
│         │  ┌───────────────┐        │                                │
│         │  │Add to pending │        │                                │
│         │  │(wait for deps)│        │                                │
│         │  └───────────────┘        │                                │
│         │                           │                                │
│         └───────────┬───────────────┘                                │
│                     │                                                │
│                     ▼                                                │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │           Calculate Execution Time                          │   │
│   │                                                             │   │
│   │  OneTime:  scheduledTime = specifiedTime                    │   │
│   │  Daily:    scheduledTime = next occurrence of time          │   │
│   │  Cron:     scheduledTime = parse cron expression            │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                     │                                                │
│                     ▼                                                │
│              ┌─────────────┐                                        │
│              │ Add to      │                                        │
│              │ Priority    │                                        │
│              │ Queue       │                                        │
│              └─────────────┘                                        │
│                     │                                                │
│                     ▼                                                │
│              ┌─────────────┐                                        │
│              │ Notify      │                                        │
│              │ Scheduler   │                                        │
│              │ Thread      │                                        │
│              └─────────────┘                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Task Execution Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TASK EXECUTION FLOW                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Task ready for execution                                          │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │Update Status│                                                   │
│   │ → RUNNING   │                                                   │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────┐                                                   │
│   │ Notify      │                                                   │
│   │ Listeners   │──▶ onTaskStarted(task)                           │
│   └─────────────┘                                                   │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                 Execute task.command()                      │   │
│   └─────────────────────────────────────────────────────────────┘   │
│         │                                                            │
│         ├─────────────────────────────┐                             │
│         │                             │                             │
│      Success                        Failure                         │
│         │                             │                             │
│         ▼                             ▼                             │
│   ┌─────────────┐           ┌─────────────────┐                    │
│   │Update Status│           │ Retry Count     │                    │
│   │ → COMPLETED │           │ < MaxRetries?   │                    │
│   └─────────────┘           └─────────────────┘                    │
│         │                       │         │                         │
│         │                      Yes       No                         │
│         │                       │         │                         │
│         │                       ▼         ▼                         │
│         │              ┌────────────┐ ┌────────────┐               │
│         │              │Calculate   │ │Update Status│               │
│         │              │Backoff     │ │ → FAILED    │               │
│         │              │Delay       │ └────────────┘               │
│         │              └────────────┘        │                      │
│         │                    │               │                      │
│         │                    ▼               │                      │
│         │              ┌────────────┐        │                      │
│         │              │Re-schedule │        │                      │
│         │              │with delay  │        │                      │
│         │              └────────────┘        │                      │
│         │                                    │                      │
│         ▼                                    ▼                      │
│   ┌─────────────┐                    ┌─────────────┐               │
│   │ Notify      │                    │ Notify      │               │
│   │ Listeners   │                    │ Listeners   │               │
│   │onCompleted()│                    │ onFailed()  │               │
│   └─────────────┘                    └─────────────┘               │
│         │                                                           │
│         ▼                                                           │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │              Is Recurring Task?                              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│         │                           │                                │
│        Yes                         No                                │
│         │                           │                                │
│         ▼                           ▼                                │
│   ┌─────────────┐              ┌─────────────┐                      │
│   │ Calculate   │              │    Done     │                      │
│   │ Next Run    │              │             │                      │
│   │ Re-schedule │              └─────────────┘                      │
│   └─────────────┘                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Retry with Backoff Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RETRY WITH BACKOFF                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Exponential Backoff:                                              │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │                                                              │  │
│   │  Attempt 1: Failed → Wait 1 second  → Retry                  │  │
│   │  Attempt 2: Failed → Wait 2 seconds → Retry                  │  │
│   │  Attempt 3: Failed → Wait 4 seconds → Retry                  │  │
│   │  Attempt 4: Failed → Wait 8 seconds → Retry                  │  │
│   │  Attempt 5: Failed → GIVE UP (max retries)                   │  │
│   │                                                              │  │
│   │  Formula: delay = baseDelay * (2 ^ attemptNumber)            │  │
│   │           delay = min(delay, maxDelay)  // Cap at max        │  │
│   │                                                              │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│   Timeline:                                                          │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │ T=0    T=1    T=3    T=7     T=15                            │  │
│   │  │      │      │      │       │                              │  │
│   │  ▼      ▼      ▼      ▼       ▼                              │  │
│   │ [X]    [X]    [X]    [X]     [✓]                             │  │
│   │ Fail   Fail   Fail   Fail   Success!                         │  │
│   │  └─1s──┘└─2s──┘└─4s──┘└─8s───┘                               │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Task Dependencies Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    TASK DEPENDENCIES                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Example: Task C depends on Task A and Task B                      │
│                                                                      │
│   ┌─────────┐     ┌─────────┐                                       │
│   │ Task A  │     │ Task B  │                                       │
│   │ (ready) │     │ (ready) │                                       │
│   └────┬────┘     └────┬────┘                                       │
│        │               │                                             │
│        │    depends    │                                             │
│        └───────┬───────┘                                             │
│                │                                                     │
│                ▼                                                     │
│   ┌─────────────────────┐                                           │
│   │       Task C        │                                           │
│   │  (waiting on A, B)  │                                           │
│   └─────────────────────┘                                           │
│                                                                      │
│   Execution Timeline:                                               │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │                                                              │  │
│   │  T=0          T=5          T=8          T=12                 │  │
│   │   │            │            │            │                   │  │
│   │   ▼            ▼            ▼            ▼                   │  │
│   │  Task A ─────▶ ✓                                             │  │
│   │  Task B ──────────────────▶ ✓                                │  │
│   │  Task C (blocked)          (blocked)    Start ────▶ ✓        │  │
│   │                                           │                   │  │
│   │                             A & B done ───┘                   │  │
│   │                                                              │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│   Dependency Check:                                                  │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │  fun canExecute(task: Task): Boolean {                       │  │
│   │      return task.dependencies.all { depId ->                 │  │
│   │          taskRegistry[depId]?.status == COMPLETED            │  │
│   │      }                                                       │  │
│   │  }                                                           │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

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

/**
 * Represents the lifecycle states of a task.
 * 
 * State Transitions:
 * PENDING → SCHEDULED → RUNNING → COMPLETED
 *                    ↘         ↘ (retry)
 *                     → CANCELLED  → FAILED
 *                     
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
 * 
 * Used in PriorityQueue comparison: tasks with same execution time
 * are ordered by priority (CRITICAL before HIGH before MEDIUM before LOW)
 */
enum class Priority(val value: Int) {
    LOW(1),        // Background tasks, batch jobs
    MEDIUM(5),     // Normal tasks (default)
    HIGH(10),      // Important tasks
    CRITICAL(100)  // Urgent tasks, system-critical operations
}

// ==================== Task ====================

/**
 * Represents a schedulable task with execution logic and metadata.
 * 
 * === Key Properties ===
 * @property id Unique identifier (auto-generated UUID)
 * @property name Human-readable task name for logging/monitoring
 * @property command The actual work to execute (lambda function)
 * @property schedule When/how often to run (OneTime, Daily, Interval, Cron)
 * @property priority Execution priority when multiple tasks are due
 * @property dependencies List of task IDs that must complete before this runs
 * 
 * === Comparable Implementation ===
 * Tasks are compared for PriorityQueue ordering:
 * 1. First by nextExecutionTime (earlier = higher priority)
 * 2. Then by priority value (higher = comes first)
 * 
 * This ensures time-critical tasks run on schedule while allowing
 * priority to break ties for concurrent tasks.
 * 
 * === Thread Safety ===
 * Task properties (status, retryCount, etc.) are mutable and should
 * only be modified while holding the scheduler's lock.
 */
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: () -> Unit,           // The work to execute
    val schedule: Schedule,             // Scheduling strategy
    val priority: Priority = Priority.MEDIUM,
    var status: TaskStatus = TaskStatus.PENDING,
    var retryCount: Int = 0,            // Current retry attempt
    val maxRetries: Int = 3,            // Max retries before marking FAILED
    val dependencies: List<String> = emptyList(),  // Task IDs this depends on
    val createdAt: Instant = Instant.now(),
    var lastExecutedAt: Instant? = null,
    var nextExecutionTime: Instant? = null
) : Comparable<Task> {
    
    init {
        // Calculate initial execution time based on schedule
        nextExecutionTime = schedule.getNextExecutionTime(Instant.now())
    }
    
    /**
     * Comparison for PriorityQueue ordering.
     * 
     * Returns negative if THIS task should execute BEFORE other.
     * 
     * Ordering logic:
     * 1. Earlier execution time wins (time comparison)
     * 2. If same time: higher priority.value wins
     *    - Note: We compare other.priority to this.priority (reversed)
     *    - This makes higher values come first
     * 
     * Example:
     *   Task A: time=10:00, priority=LOW(1)
     *   Task B: time=10:00, priority=HIGH(10)
     *   Result: B executes first (10 > 1)
     */
    override fun compareTo(other: Task): Int {
        // First compare by execution time (ascending - earlier first)
        val timeComparison = compareValues(nextExecutionTime, other.nextExecutionTime)
        if (timeComparison != 0) return timeComparison
        
        // Then by priority (descending - higher value first)
        // other.priority - this.priority gives descending order
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

/**
 * Executes tasks and manages execution lifecycle.
 * 
 * === Responsibilities ===
 * - Execute task command
 * - Track execution time
 * - Update task status
 * - Notify listeners of state changes
 * - Handle exceptions gracefully
 * 
 * === Execution Flow ===
 * 1. Notify listeners: task started
 * 2. Set status to RUNNING
 * 3. Execute task.command()
 * 4. On success: status = COMPLETED, notify completed
 * 5. On failure: status = FAILED, increment retryCount, notify failed
 * 
 * === Thread Safety ===
 * Each task execution runs in its own thread (from ExecutorService).
 * Listener notifications are synchronous within the executing thread.
 */
class TaskExecutor(
    private val listeners: MutableList<TaskListener> = mutableListOf()
) {
    
    /**
     * Execute a task and return the result.
     * 
     * @param task The task to execute
     * @return TaskResult containing success status, duration, and any error
     * 
     * Time Complexity: O(1) + task.command() complexity
     */
    fun execute(task: Task): TaskResult {
        notifyStarted(task)
        
        val startTime = Instant.now()
        
        return try {
            // Mark as running
            task.status = TaskStatus.RUNNING
            
            // Execute the actual work (this is where user logic runs)
            task.command()
            
            // Mark successful completion
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
            // Handle failure - don't throw, return result with error
            task.status = TaskStatus.FAILED
            task.retryCount++  // Increment for retry logic to check
            
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
 * Implements Strategy Pattern - allows pluggable retry behaviors.
 */
interface RetryStrategy {
    /**
     * Calculate the delay before the next retry attempt.
     * @param retryCount Current retry attempt (0 = first retry)
     * @return Duration to wait, or null if no more retries
     */
    fun getNextRetryDelay(retryCount: Int): Duration?
}

/**
 * Exponential backoff retry strategy.
 * 
 * === Formula ===
 * delay = baseDelay * (multiplier ^ retryCount)
 * delay = min(delay, maxDelay)  // Cap at maximum
 * 
 * === Example (default values) ===
 * Retry 0: 1000 * 2^0 = 1000ms (1 second)
 * Retry 1: 1000 * 2^1 = 2000ms (2 seconds)
 * Retry 2: 1000 * 2^2 = 4000ms (4 seconds)
 * Retry 3: 1000 * 2^3 = 8000ms (8 seconds)
 * ...capped at maxDelayMs (60 seconds)
 * 
 * === Why Exponential Backoff? ===
 * - Prevents thundering herd problem
 * - Gives failing systems time to recover
 * - Reduces load during outages
 * - Industry standard for distributed systems
 */
class ExponentialBackoffRetry(
    private val baseDelayMs: Long = 1000,    // Starting delay
    private val maxDelayMs: Long = 60000,    // Maximum delay cap
    private val multiplier: Double = 2.0      // Growth factor
) : RetryStrategy {
    
    override fun getNextRetryDelay(retryCount: Int): Duration {
        // Calculate: base * (multiplier ^ retryCount)
        val delay = (baseDelayMs * multiplier.pow(retryCount.toDouble())).toLong()
        // Cap at maximum to prevent extremely long waits
        return Duration.ofMillis(minOf(delay, maxDelayMs))
    }
}

/**
 * Fixed delay retry strategy - same delay for every retry.
 * 
 * Use when:
 * - System has predictable recovery time
 * - Simple retry logic is sufficient
 * - Don't want increasing delays
 */
class FixedDelayRetry(
    private val delayMs: Long = 5000  // 5 seconds default
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

