# Atlassian DSA Problems - 6 Months

This folder contains detailed solutions for DSA (Data Structures and Algorithms) problems that have been asked at Atlassian in the last 6 months, based on LeetCode data.

**Source**: [LeetCode Company-wise Problems - Atlassian](https://github.com/liquidslr/leetcode-company-wise-problems/blob/main/Atlassian/3.%20Six%20Months.csv)

---

## 📚 Study Materials (Must Read First!)

### Fundamentals
| Resource | Description |
|----------|-------------|
| [**Time Complexity Guide**](./00-time-complexity-guide.md) | Big O analysis, common complexities, Master theorem, interview tips |
| [**Data Structures Cheatsheet**](./00-data-structures-cheatsheet.md) | Python/Java/Kotlin implementations, TreeMap, TreeSet, Heap, sorting |

### Algorithms
| Resource | Description |
|----------|-------------|
| [**Graph Algorithms**](./00-graph-algorithms.md) | DFS, BFS, Topological Sort, Dijkstra, Union-Find, MST, SCC |
| [**Tree Algorithms**](./00-tree-algorithms.md) | Traversals, LCA, BST operations, Trie, serialization |
| [**Binary Search Patterns**](./00-binary-search-patterns.md) | Templates, rotated array, binary search on answer, bisect |
| [**Backtracking Patterns**](./00-backtracking-patterns.md) | Subsets, permutations, combinations, N-Queens, Sudoku |
| [**Sliding Window & Two Pointers**](./00-sliding-window-two-pointers.md) | Fixed/variable window, fast-slow pointers, monotonic deque |

### Dynamic Programming
| Resource | Description |
|----------|-------------|
| [**DP Patterns**](./00-dp-patterns.md) | Recursive → Memo → Tabulation for all classic patterns (Knapsack, LCS, LIS, etc.) |

### System & Database
| Resource | Description |
|----------|-------------|
| [**Concurrency Guide**](./00-concurrency-guide.md) | Threads, locks, ExecutorService, atomics, CompletableFuture, Kotlin coroutines |
| [**SQL Cheatsheet**](./00-sql-cheatsheet.md) | JOINs, window functions (RANK, LAG), CTEs, optimization, interview queries |
| [**Design Patterns (Kotlin)**](./00-design-patterns-kotlin.md) | All 19 GoF patterns with multiple variations, Kotlin idioms |

---

## Problem Index

| # | Problem | Difficulty | Frequency | Topics |
|---|---------|------------|-----------|--------|
| 01 | [Rank Teams by Votes](./01-rank-teams-by-votes.md) | Medium | 100.0% | Array, Hash Table, String, Sorting |
| 02 | [Stock Price Fluctuation](./02-stock-price-fluctuation.md) | Medium | 96.9% | Hash Table, Design, Heap, Data Stream |
| 03 | [All O'one Data Structure](./03-all-oone-data-structure.md) | Hard | 80.0% | Hash Table, Linked List, Design |
| 04 | [Design Snake Game](./04-design-snake-game.md) | Medium | 80.0% | Array, Hash Table, Design, Queue |
| 05 | [Find Width of Columns of Grid](./05-find-width-of-columns-of-grid.md) | Easy | 73.6% | Array, Matrix |
| 06 | [Meeting Rooms II](./06-meeting-rooms-ii.md) | Medium | 69.4% | Array, Greedy, Sorting, Heap |
| 07 | [Lowest Common Ancestor of Binary Tree](./07-lowest-common-ancestor-binary-tree.md) | Medium | 69.4% | Tree, DFS, Binary Tree |
| 08 | [High-Access Employees](./08-high-access-employees.md) | Medium | 60.5% | Array, Hash Table, String, Sorting |
| 09 | [Text Justification](./09-text-justification.md) | Hard | 60.5% | Array, String, Simulation |
| 10 | [Time When Network Becomes Idle](./10-time-when-network-becomes-idle.md) | Medium | 60.5% | Array, BFS, Graph |
| 11 | [Can Place Flowers](./11-can-place-flowers.md) | Easy | 56.2% | Array, Greedy |
| 12 | [Russian Doll Envelopes](./12-russian-doll-envelopes.md) | Hard | 56.2% | Array, Binary Search, DP, Sorting |
| 13 | [Koko Eating Bananas](./13-koko-eating-bananas.md) | Medium | 50.8% | Array, Binary Search |
| 14 | [Merge Intervals](./14-merge-intervals.md) | Medium | 50.8% | Array, Sorting |
| 15 | [LCA of Binary Tree III](./15-lca-binary-tree-iii.md) | Medium | 43.1% | Hash Table, Two Pointers, Tree |
| 16 | [Design Food Rating System](./16-design-food-rating-system.md) | Medium | 43.1% | Hash Table, Design, Heap, Ordered Set |
| 17 | [Best Time to Buy and Sell Stock](./17-best-time-buy-sell-stock.md) | Easy | 43.1% | Array, Dynamic Programming |
| 18 | [Design Add and Search Words](./18-design-add-search-words.md) | Medium | 43.1% | String, DFS, Design, Trie |
| 19 | [Top K Frequent Elements](./19-top-k-frequent-elements.md) | Medium | 43.1% | Array, Hash Table, Heap, Bucket Sort |
| 20 | [Find First and Last Position](./20-find-first-last-position-sorted-array.md) | Medium | 43.1% | Array, Binary Search |
| 21 | [Count Vowel Strings in Ranges](./21-count-vowel-strings-in-ranges.md) | Medium | 43.1% | Array, String, Prefix Sum |
| 22 | [Word Search](./22-word-search.md) | Medium | 43.1% | Array, String, Backtracking, DFS |
| 23 | [Logger Rate Limiter](./23-logger-rate-limiter.md) | Easy | 43.1% | Hash Table, Design, Data Stream |
| 24 | [Last Day Where You Can Still Cross](./24-last-day-where-you-can-still-cross.md) | Hard | 43.1% | Binary Search, BFS, DFS, Union Find |
| 25 | [Nth Highest Salary](./25-nth-highest-salary.md) | Medium | 50.8% | Database (SQL) |

## Statistics

- **Total Problems**: 25
- **Easy**: 4 (16%)
- **Medium**: 17 (68%)
- **Hard**: 4 (16%)

## Key Topics to Focus On

Based on frequency, these are the most important topics for Atlassian interviews:

### High Priority
1. **Design Problems** - All O'one, Snake Game, Food Rating System, Add/Search Words, Logger
2. **Hash Tables** - Used extensively across problems
3. **Heap/Priority Queue** - Stock Price, Meeting Rooms, Food Rating, Top K
4. **Binary Search** - Koko Eating, Russian Doll, Last Day Cross, Find First/Last Position

### Medium Priority
5. **Trees** - LCA problems
6. **Arrays & Sorting** - Merge Intervals, Rank Teams
7. **BFS/DFS** - Word Search, Network Idle
8. **Greedy** - Can Place Flowers, Meeting Rooms

### Good to Know
9. **Union Find** - Last Day Cross
10. **Trie** - Add and Search Words
11. **Prefix Sum** - Count Vowel Strings
12. **Dynamic Programming** - Russian Doll Envelopes

## Common Patterns

### 1. Design Pattern
Problems like Stock Price Fluctuation, Snake Game, Food Rating System test your ability to:
- Choose appropriate data structures
- Balance time complexity across operations
- Handle edge cases

### 2. Binary Search on Answer
Problems like Koko Eating Bananas, Last Day Cross:
- Identify monotonic property
- Define search space
- Write check function

### 3. Interval Problems
Meeting Rooms II, Merge Intervals:
- Sort by start/end time
- Use heap or line sweep
- Handle overlapping intervals

### 4. Graph Traversal
Network Idle, Last Day Cross, Word Search:
- BFS for shortest path
- DFS for path finding
- Union Find for connectivity

## Recommended Study Order

1. **Week 1**: Easy problems + core patterns
   - Logger Rate Limiter (Hash Table, Design)
   - Can Place Flowers (Array, Greedy)
   - Best Time to Buy and Sell Stock (Array, DP)
   - Find Width of Columns (Array basics)

2. **Week 2**: Medium difficulty - Design
   - Stock Price Fluctuation
   - Design Snake Game
   - Design Food Rating System
   - Design Add and Search Words

3. **Week 3**: Medium difficulty - Algorithms
   - Merge Intervals
   - Meeting Rooms II
   - Koko Eating Bananas
   - Top K Frequent Elements
   - LCA of Binary Tree (both versions)

4. **Week 4**: Hard problems + practice
   - All O'one Data Structure
   - Text Justification
   - Russian Doll Envelopes
   - Last Day Where You Can Still Cross

## Tips for Atlassian Interviews

1. **Focus on System Design thinking**: Many problems test your ability to design efficient data structures
2. **Discuss trade-offs**: Time vs Space, different approaches
3. **Handle edge cases**: Empty inputs, single elements, boundary conditions
4. **Write clean, readable code**: Variable names, comments, modular functions
5. **Test your solution**: Walk through examples before submitting

## Additional Resources

- [LeetCode Atlassian Tag](https://leetcode.com/company/atlassian/)
- [Atlassian Engineering Blog](https://www.atlassian.com/engineering)

---

Good luck with your Atlassian interview preparation! 🚀

