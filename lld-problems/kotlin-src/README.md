# LLD Problems - Kotlin Source Files

This folder contains **Kotlin implementations** of all LLD (Low-Level Design) problems with comprehensive documentation.

## 📁 Files

| File | Problem | Key Concepts |
|------|---------|--------------|
| `RateLimiter.kt` | Rate Limiter | Token Bucket, Sliding Window, Fixed Window, Leaky Bucket |
| `TaskScheduler.kt` | Task Scheduler | PriorityQueue, Recurring Tasks, Dependencies, Retry |
| `InMemoryCache.kt` | LRU/LFU Cache | HashMap + DoublyLinkedList, Eviction Policies |
| `PubSubSystem.kt` | Pub-Sub Messaging | Observer Pattern, Message Filtering, Async Delivery |
| `ParkingLot.kt` | Parking Lot | Factory, Strategy, Multi-floor, Pricing |
| `LoggerFramework.kt` | Logger | Sinks, Formatters, Async Logging, MDC |
| `SnakeLadder.kt` | Snake & Ladder | Game Logic, Sealed Classes, Turn Management |
| `ElevatorSystem.kt` | Elevator | LOOK Algorithm, Dispatching, State Management |
| `UndoRedoSystem.kt` | Undo-Redo | Command Pattern, Stack Operations, Merging |
| `SearchAutocomplete.kt` | Autocomplete | Trie, Prefix Search, Frequency Ranking |
| `SnakeGame.kt` | Snake Game | Deque + Set, Collision Detection |

## 🚀 How to Use

### In IDE (Recommended)
1. Open this folder in IntelliJ IDEA or Android Studio
2. Files will have full syntax highlighting and navigation
3. Use `Ctrl+Click` to navigate to definitions
4. Use `Ctrl+Q` for quick documentation

### Run Examples
Each file has a `main()` function demonstrating usage:

```bash
# Using kotlinc (if installed)
kotlinc RateLimiter.kt -include-runtime -d RateLimiter.jar
java -jar RateLimiter.jar

# Or use IDE's run button
```

## 📚 Documentation Style

Each file follows this structure:

```kotlin
/**
 * Problem Name - LLD Implementation
 * 
 * Problem description and features
 * 
 * Design Patterns: Pattern1, Pattern2
 * Time Complexity: O(...)
 * Space Complexity: O(...)
 */

// ==================== Section Name ====================

/**
 * Class/Function documentation
 * 
 * === Code Flow ===
 * Step-by-step execution explanation
 */
class MyClass { ... }

// ==================== Usage Example ====================

fun main() { ... }
```

## 🎯 Key Design Patterns

| Pattern | Used In |
|---------|---------|
| **Strategy** | Rate Limiter, Cache, Pricing, Scheduling |
| **Factory** | Rate Limiter, Parking Lot |
| **Observer** | Pub-Sub, Task Scheduler, Elevator |
| **Command** | Undo-Redo |
| **Decorator** | Async Logger |
| **Singleton** | Logger Factory |
| **Builder** | Cache, Parking Lot, Game |
| **State** | Task Status, Game Status |

## 🔧 Data Structures

| Structure | Used For |
|-----------|----------|
| `HashMap + DoublyLinkedList` | LRU Cache |
| `HashMap + Frequency Buckets` | LFU Cache |
| `PriorityQueue` | Task Scheduler |
| `Trie` | Autocomplete |
| `Deque + HashSet` | Snake Game |
| `TreeSet` | Elevator Destinations |
| `ConcurrentHashMap` | Thread-safe storage |

## ⚡ Thread Safety

Most implementations include thread-safe operations:

- `ReentrantLock` / `ReentrantReadWriteLock`
- `ConcurrentHashMap`
- `AtomicBoolean` / `AtomicInteger`
- `LinkedBlockingQueue`

## 📖 Study Tips

1. **Start with the main() function** - See how classes are used
2. **Read class documentation** - Understand the data structures
3. **Follow Code Flow sections** - Step-by-step execution
4. **Compare algorithms** - e.g., LRU vs LFU, Token vs Sliding Window
5. **Practice drawing diagrams** - Visualize data structures

## 🎓 Interview Tips

When explaining in interviews:

1. **Start with requirements** - What needs to be supported?
2. **Discuss trade-offs** - Why this algorithm/data structure?
3. **Draw diagrams** - Visual explanation helps
4. **Mention complexity** - Time and space
5. **Discuss extensions** - How would you scale this?

---

Good luck with your interviews! 🚀

