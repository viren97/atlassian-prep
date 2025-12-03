# Atlassian LLD (Low-Level Design) Problems

This folder contains detailed solutions for Low-Level Design problems commonly asked in Atlassian interviews.

## Problem Index

| # | Problem | Key Concepts | Difficulty |
|---|---------|--------------|------------|
| 01 | [Rate Limiter](./01-rate-limiter.md) | Token Bucket, Sliding Window, Strategy Pattern | Medium |
| 02 | [Task Scheduler](./02-task-scheduler.md) | Priority Queue, Cron, Observer Pattern | Medium |
| 03 | [In-Memory Cache (LRU/LFU)](./03-in-memory-cache.md) | Doubly Linked List, Hash Map, Decorator | Medium |
| 04 | [Pub-Sub System](./04-pub-sub-system.md) | Observer Pattern, Message Queue | Medium |
| 05 | [Parking Lot](./05-parking-lot.md) | Builder Pattern, Strategy, Factory | Medium |
| 06 | [Logger Framework](./06-logger-framework.md) | Strategy, Decorator, Composite | Medium |
| 07 | [Snake and Ladder](./07-snake-ladder.md) | State Machine, Game Design | Easy |
| 08 | [Elevator System](./08-elevator-system.md) | State Pattern, Scheduling | Hard |
| 09 | [Undo-Redo System](./09-undo-redo-system.md) | Command Pattern, Stack | Medium |
| 10 | [Search Autocomplete](./10-search-autocomplete.md) | Trie, Priority Queue | Medium |
| 11 | [Meeting Scheduler](./11-meeting-scheduler.md) | Interval Trees, Calendar | Medium |
| 12 | [Driver Best Lap Time](./12-driver-best-lap-time.md) | Real-time Analytics, Streaming | Medium |
| 13 | [Customer Support Ticketing](./13-customer-support-ticketing.md) | Rating System, Sorting, Export | Medium |
| 14 | [Middleware Router](./14-middleware-router.md) | Trie, Path Matching, Wildcards | Medium |
| 15 | [Cost Explorer](./15-cost-explorer.md) | Billing, Proration, Reports | Medium |
| 16 | [Snake Game (Nokia)](./16-snake-game-lld.md) | Game Loop, Collision Detection | Medium |

## Design Patterns Covered

| Pattern | Problems |
|---------|----------|
| **Strategy** | Rate Limiter, Cache, Parking Lot, Logger |
| **Observer** | Task Scheduler, Pub-Sub, Snake Game |
| **Factory** | Rate Limiter, Parking Lot |
| **Builder** | Parking Lot, Logger, Cache |
| **Decorator** | Cache (Thread-safe), Logger (Async) |
| **Command** | Undo-Redo, Task Scheduler |
| **State** | Elevator, Snake Game |
| **Singleton** | Logger Factory, Message Broker |
| **Composite** | Logger (Multiple Sinks) |

## Key Data Structures

| Data Structure | Use Cases |
|----------------|-----------|
| **Hash Map** | O(1) lookups - Cache, Rate Limiter, Router |
| **Doubly Linked List** | LRU Cache, Order maintenance |
| **Priority Queue/Heap** | Task Scheduler, Autocomplete |
| **Trie** | Autocomplete, Router path matching |
| **Deque** | Snake body, Sliding window |
| **Set** | Fast membership check, Snake collision |

## Problem Categories

### System Design Components
- Rate Limiter (API Gateway)
- In-Memory Cache
- Task Scheduler
- Pub-Sub System
- Logger Framework
- Middleware Router

### Game Design
- Snake and Ladder
- Snake Game (Nokia)

### Real-World Applications
- Parking Lot System
- Elevator System
- Meeting Scheduler
- Customer Support Ticketing
- Cost Explorer

### Algorithm-Focused
- Search Autocomplete (Trie)
- Undo-Redo (Command Pattern)
- Driver Best Lap Time (Streaming)

## How to Approach LLD Problems

### 1. Clarify Requirements
- Functional requirements (what should it do?)
- Non-functional requirements (performance, scalability)
- Edge cases and constraints

### 2. Identify Core Entities
- What are the main objects/classes?
- What are their relationships?
- What behaviors do they need?

### 3. Choose Design Patterns
- Which patterns fit the problem?
- Don't force patterns - use when appropriate
- Focus on SOLID principles

### 4. Define Interfaces First
- Design by contract
- Keep interfaces minimal
- Think about extensibility

### 5. Consider Thread Safety
- What operations need to be atomic?
- Where do race conditions occur?
- Use appropriate synchronization

### 6. Implement Incrementally
- Start with basic functionality
- Add features one by one
- Test at each step

## Common Interview Tips

1. **Start with requirements clarification** - Ask questions!
2. **Draw class diagrams** before coding
3. **Explain your design decisions** as you go
4. **Consider extensibility** - "What if we need to add..."
5. **Discuss trade-offs** - Time vs Space, Simplicity vs Flexibility
6. **Handle edge cases** - Empty inputs, concurrent access
7. **Write clean, readable code** - Good naming, small methods

## Recommended Study Order

### Week 1: Foundation
1. In-Memory Cache (LRU) - Master Hash Map + DLL
2. Rate Limiter - Multiple algorithms
3. Logger Framework - Design patterns

### Week 2: Intermediate
4. Parking Lot - Builder + Strategy
5. Task Scheduler - Priority Queue + Observers
6. Pub-Sub System - Event-driven design

### Week 3: Advanced
7. Elevator System - State machines
8. Middleware Router - Trie-based routing
9. Search Autocomplete - Trie + Ranking

### Week 4: Practice
10. Snake Game - Apply all concepts
11. Customer Support Ticketing - Real-world scenario
12. Cost Explorer - Business logic

## Additional Resources

- [Atlassian Engineering Blog](https://www.atlassian.com/engineering)
- [Design Patterns - Refactoring Guru](https://refactoring.guru/design-patterns)
- [Java Concurrency in Practice](https://jcip.net/)

---

Good luck with your Atlassian interview preparation! 🚀

Remember: The goal isn't just to solve the problem, but to demonstrate clean code, good design decisions, and the ability to communicate your thought process.

