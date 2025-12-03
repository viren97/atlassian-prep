# Concurrency & Multithreading Guide - Java/Kotlin

## Fundamentals

### Thread vs Process
- **Process**: Independent execution unit with its own memory space
- **Thread**: Lightweight unit within a process, shares memory with other threads

### Thread States
```
NEW → RUNNABLE → RUNNING → BLOCKED/WAITING → TERMINATED
```

---

## 1. Creating Threads

### Java

```java
// Method 1: Extend Thread
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running: " + Thread.currentThread().getName());
    }
}

// Method 2: Implement Runnable (Preferred)
class MyRunnable implements Runnable {
    @Override
    public void run() {
        System.out.println("Runnable running");
    }
}

// Method 3: Lambda (Java 8+)
Thread thread = new Thread(() -> {
    System.out.println("Lambda thread");
});

// Method 4: Callable (returns value)
Callable<Integer> callable = () -> {
    return 42;
};

// Usage
MyThread t1 = new MyThread();
t1.start();  // Don't call run() directly!

Thread t2 = new Thread(new MyRunnable());
t2.start();
```

### Kotlin

```kotlin
// Method 1: Thread with lambda
val thread = Thread {
    println("Thread running: ${Thread.currentThread().name}")
}
thread.start()

// Method 2: thread() function (Kotlin stdlib)
import kotlin.concurrent.thread

thread {
    println("Simple thread")
}

// Method 3: Named thread with options
thread(name = "MyThread", isDaemon = true) {
    println("Named daemon thread")
}

// Method 4: With start = false
val t = thread(start = false) {
    println("Manual start")
}
t.start()
```

---

## 2. Synchronization

### Synchronized Keyword

```java
// Java - Synchronized method
public class Counter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}

// Java - Synchronized block (more granular)
public class Counter {
    private int count = 0;
    private final Object lock = new Object();
    
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

```kotlin
// Kotlin - @Synchronized annotation
class Counter {
    private var count = 0
    
    @Synchronized
    fun increment() {
        count++
    }
}

// Kotlin - synchronized() function
class Counter {
    private var count = 0
    private val lock = Any()
    
    fun increment() {
        synchronized(lock) {
            count++
        }
    }
}
```

### Volatile Keyword

```java
// Ensures visibility across threads (but NOT atomicity)
public class SharedFlag {
    private volatile boolean running = true;
    
    public void stop() {
        running = false;
    }
    
    public void run() {
        while (running) {
            // Do work
        }
    }
}
```

```kotlin
@Volatile
private var running = true
```

---

## 3. Locks (java.util.concurrent.locks)

### ReentrantLock

```java
import java.util.concurrent.locks.ReentrantLock;

public class Counter {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // Always in finally!
        }
    }
    
    // Try lock with timeout
    public boolean tryIncrement() {
        boolean acquired = false;
        try {
            acquired = lock.tryLock(1, TimeUnit.SECONDS);
            if (acquired) {
                count++;
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired) lock.unlock();
        }
        return false;
    }
}
```

### ReadWriteLock

```java
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache<K, V> {
    private final Map<K, V> map = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public V get(K key) {
        rwLock.readLock().lock();
        try {
            return map.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void put(K key, V value) {
        rwLock.writeLock().lock();
        try {
            map.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

### Kotlin Lock Extensions

```kotlin
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val lock = ReentrantLock()

fun safeOperation() {
    lock.withLock {
        // Thread-safe code
    }
}

// Read-write lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

val rwLock = ReentrantReadWriteLock()

fun read() = rwLock.read { /* read operation */ }
fun write() = rwLock.write { /* write operation */ }
```

---

## 4. Atomic Variables

```java
import java.util.concurrent.atomic.*;

// AtomicInteger - thread-safe integer
AtomicInteger count = new AtomicInteger(0);
count.incrementAndGet();      // ++count
count.getAndIncrement();      // count++
count.addAndGet(5);           // count += 5
count.compareAndSet(5, 10);   // CAS: if count==5, set to 10

// AtomicLong
AtomicLong longVal = new AtomicLong(0L);

// AtomicBoolean
AtomicBoolean flag = new AtomicBoolean(false);
flag.compareAndSet(false, true);

// AtomicReference - for objects
AtomicReference<String> ref = new AtomicReference<>("initial");
ref.compareAndSet("initial", "updated");

// AtomicIntegerArray
AtomicIntegerArray array = new AtomicIntegerArray(10);
array.incrementAndGet(0);  // Increment index 0

// LongAdder - better for high contention
LongAdder adder = new LongAdder();
adder.increment();
adder.sum();  // Get total
```

```kotlin
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.atomicfu.atomic  // Kotlin atomicfu library

// Using Java atomics
val count = AtomicInteger(0)
count.incrementAndGet()

// Kotlin atomicfu (more idiomatic)
val counter = atomic(0)
counter.incrementAndGet()
```

---

## 5. Thread Pools & Executors

### ExecutorService

```java
import java.util.concurrent.*;

// Fixed thread pool
ExecutorService executor = Executors.newFixedThreadPool(4);

// Single thread executor
ExecutorService single = Executors.newSingleThreadExecutor();

// Cached thread pool (creates threads as needed)
ExecutorService cached = Executors.newCachedThreadPool();

// Scheduled executor
ScheduledExecutorService scheduled = Executors.newScheduledThreadPool(2);

// Submit tasks
executor.submit(() -> {
    System.out.println("Task executed by: " + Thread.currentThread().getName());
});

// Submit with result
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(1000);
    return 42;
});

// Get result (blocks)
try {
    Integer result = future.get();  // Blocks until complete
    Integer resultWithTimeout = future.get(5, TimeUnit.SECONDS);
} catch (InterruptedException | ExecutionException | TimeoutException e) {
    e.printStackTrace();
}

// Shutdown
executor.shutdown();  // Wait for tasks to complete
executor.shutdownNow();  // Interrupt running tasks
executor.awaitTermination(60, TimeUnit.SECONDS);
```

### Custom ThreadPoolExecutor

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                      // Core pool size
    4,                      // Maximum pool size
    60L,                    // Keep-alive time
    TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(100),  // Work queue
    new ThreadPoolExecutor.CallerRunsPolicy()  // Rejection policy
);

// Rejection Policies:
// - AbortPolicy: throws RejectedExecutionException
// - CallerRunsPolicy: runs in caller's thread
// - DiscardPolicy: silently discards
// - DiscardOldestPolicy: discards oldest task
```

### CompletableFuture (Java 8+)

```java
import java.util.concurrent.CompletableFuture;

// Async execution
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Hello";
});

// Chaining
CompletableFuture<String> result = future
    .thenApply(s -> s + " World")           // Transform result
    .thenApply(String::toUpperCase);

// Async chaining
future.thenApplyAsync(s -> s + " World");

// Combine two futures
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "World");

CompletableFuture<String> combined = f1.thenCombine(f2, (s1, s2) -> s1 + " " + s2);

// Wait for all
CompletableFuture.allOf(f1, f2).join();

// Wait for any
CompletableFuture.anyOf(f1, f2).join();

// Handle errors
future.exceptionally(ex -> {
    System.out.println("Error: " + ex.getMessage());
    return "Default";
});

// Handle both success and failure
future.handle((result, ex) -> {
    if (ex != null) return "Error";
    return result;
});
```

### Kotlin Coroutines

```kotlin
import kotlinx.coroutines.*

// Launch coroutine (fire and forget)
GlobalScope.launch {
    delay(1000)
    println("Coroutine executed")
}

// Async (returns Deferred)
val deferred = GlobalScope.async {
    delay(1000)
    return@async 42
}
val result = deferred.await()

// Structured concurrency
runBlocking {
    launch {
        delay(1000)
        println("Task 1")
    }
    launch {
        delay(500)
        println("Task 2")
    }
}

// Parallel execution
suspend fun fetchData(): List<String> = coroutineScope {
    val data1 = async { fetchFromApi1() }
    val data2 = async { fetchFromApi2() }
    listOf(data1.await(), data2.await())
}

// Dispatchers
launch(Dispatchers.Default) { /* CPU-intensive */ }
launch(Dispatchers.IO) { /* I/O operations */ }
launch(Dispatchers.Main) { /* UI thread (Android) */ }

// WithContext - switch dispatcher
suspend fun fetchData() = withContext(Dispatchers.IO) {
    // Network call
}

// Timeout
withTimeout(1000L) {
    // Must complete within 1 second
}

withTimeoutOrNull(1000L) {
    // Returns null if timeout
}
```

---

## 6. Concurrent Collections

```java
// ConcurrentHashMap - thread-safe HashMap
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key", 1);
map.computeIfAbsent("key", k -> 1);
map.merge("key", 1, Integer::sum);  // Atomic increment

// CopyOnWriteArrayList - thread-safe for read-heavy
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// ConcurrentLinkedQueue - non-blocking queue
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
queue.offer("item");
queue.poll();

// BlockingQueue - blocking operations
BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(100);
blockingQueue.put("item");    // Blocks if full
blockingQueue.take();         // Blocks if empty
blockingQueue.offer("item", 1, TimeUnit.SECONDS);  // With timeout

// ArrayBlockingQueue - bounded
BlockingQueue<String> bounded = new ArrayBlockingQueue<>(10);

// PriorityBlockingQueue - priority ordering
PriorityBlockingQueue<Integer> pq = new PriorityBlockingQueue<>();

// ConcurrentSkipListMap - sorted, thread-safe TreeMap
ConcurrentSkipListMap<String, Integer> sortedMap = new ConcurrentSkipListMap<>();

// ConcurrentSkipListSet - sorted, thread-safe TreeSet
ConcurrentSkipListSet<String> sortedSet = new ConcurrentSkipListSet<>();
```

---

## 7. Synchronizers

### CountDownLatch

```java
// Wait for N events to complete
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        try {
            // Do work
        } finally {
            latch.countDown();  // Decrement count
        }
    });
}

latch.await();  // Wait until count reaches 0
latch.await(5, TimeUnit.SECONDS);  // With timeout
```

### CyclicBarrier

```java
// Wait for N threads to reach barrier point
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("All threads reached barrier");
});

for (int i = 0; i < 3; i++) {
    executor.submit(() -> {
        // Do phase 1 work
        barrier.await();  // Wait for others
        // Do phase 2 work
        barrier.await();  // Can be reused!
    });
}
```

### Semaphore

```java
// Limit concurrent access
Semaphore semaphore = new Semaphore(3);  // 3 permits

void accessResource() {
    try {
        semaphore.acquire();  // Get permit (blocks if none available)
        // Access limited resource
    } finally {
        semaphore.release();  // Release permit
    }
}

// Try acquire
if (semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
    try {
        // Access resource
    } finally {
        semaphore.release();
    }
}
```

### Phaser

```java
// More flexible than CyclicBarrier
Phaser phaser = new Phaser(3);  // 3 parties

executor.submit(() -> {
    phaser.arriveAndAwaitAdvance();  // Phase 0
    // Do work
    phaser.arriveAndAwaitAdvance();  // Phase 1
    // Do work
    phaser.arriveAndDeregister();    // Done
});
```

---

## 8. Common Patterns

### Producer-Consumer

```java
public class ProducerConsumer {
    private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);
    
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            queue.put(value++);  // Blocks if full
            System.out.println("Produced: " + value);
        }
    }
    
    public void consume() throws InterruptedException {
        while (true) {
            Integer value = queue.take();  // Blocks if empty
            System.out.println("Consumed: " + value);
        }
    }
}
```

### Thread-Safe Singleton

```java
// Double-checked locking
public class Singleton {
    private static volatile Singleton instance;
    
    private Singleton() {}
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Initialization-on-demand holder (better)
public class Singleton {
    private Singleton() {}
    
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}

// Enum singleton (best)
public enum Singleton {
    INSTANCE;
    
    public void doSomething() { }
}
```

```kotlin
// Kotlin object - thread-safe by default
object Singleton {
    fun doSomething() { }
}

// Lazy initialization
class Config {
    companion object {
        val instance: Config by lazy { Config() }
    }
}
```

### Read-Write Cache

```java
public class Cache<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public V get(K key) {
        return map.get(key);  // ConcurrentHashMap is thread-safe
    }
    
    public V computeIfAbsent(K key, Function<K, V> loader) {
        return map.computeIfAbsent(key, loader);
    }
    
    public void invalidate(K key) {
        map.remove(key);
    }
    
    public void invalidateAll() {
        map.clear();
    }
}
```

---

## 9. Deadlock Prevention

### What Causes Deadlock
1. **Mutual Exclusion** - Resource can only be held by one thread
2. **Hold and Wait** - Thread holds resource while waiting for another
3. **No Preemption** - Resources can't be forcibly taken
4. **Circular Wait** - Circular chain of threads waiting

### Prevention Strategies

```java
// 1. Lock ordering - always acquire locks in same order
Object lock1 = new Object();
Object lock2 = new Object();

// Always lock1 before lock2
synchronized (lock1) {
    synchronized (lock2) {
        // Safe
    }
}

// 2. Try-lock with timeout
ReentrantLock lockA = new ReentrantLock();
ReentrantLock lockB = new ReentrantLock();

boolean acquired = false;
try {
    acquired = lockA.tryLock(1, TimeUnit.SECONDS);
    if (acquired) {
        if (lockB.tryLock(1, TimeUnit.SECONDS)) {
            try {
                // Do work
            } finally {
                lockB.unlock();
            }
        }
    }
} finally {
    if (acquired) lockA.unlock();
}

// 3. Use concurrent collections instead of manual locking
ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
```

---

## 10. Best Practices

| Practice | Why |
|----------|-----|
| Prefer `ExecutorService` over raw `Thread` | Better resource management |
| Always release locks in `finally` | Prevent deadlocks |
| Use `volatile` for flags, atomics for counters | Ensure visibility |
| Prefer concurrent collections | Less error-prone |
| Use higher-level concurrency utilities | CountDownLatch, Semaphore |
| Keep synchronized blocks small | Reduce contention |
| Avoid nested locks | Prevent deadlocks |
| Use immutable objects when possible | Thread-safe by design |
| Document thread-safety guarantees | Help other developers |
| Test with multiple threads | Race conditions are subtle |

---

## Quick Reference

| Need | Use |
|------|-----|
| Simple counter | `AtomicInteger` |
| Thread-safe map | `ConcurrentHashMap` |
| Blocking queue | `LinkedBlockingQueue` |
| Read-heavy cache | `ReadWriteLock` |
| Wait for N tasks | `CountDownLatch` |
| Limit concurrent access | `Semaphore` |
| Async with result | `CompletableFuture` |
| Thread pool | `ExecutorService` |
| Kotlin async | Coroutines |

