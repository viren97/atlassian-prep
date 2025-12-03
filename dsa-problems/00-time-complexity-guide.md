# Time Complexity Analysis - Complete Guide

## Big O Notation Fundamentals

### What is Big O?
Big O notation describes the **upper bound** of an algorithm's growth rate. It tells us how the runtime or space requirements grow as input size increases.

### Key Rules

1. **Drop Constants**: O(2n) → O(n)
2. **Drop Lower Order Terms**: O(n² + n) → O(n²)
3. **Different Inputs = Different Variables**: O(a + b), not O(n)
4. **Worst Case (usually)**: Unless specified otherwise

---

## Common Time Complexities (Best to Worst)

```
O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(n³) < O(2ⁿ) < O(n!)
```

### Visual Comparison (n = 1000)

| Complexity | Operations | Example |
|------------|------------|---------|
| O(1) | 1 | Array access |
| O(log n) | ~10 | Binary search |
| O(n) | 1,000 | Linear scan |
| O(n log n) | ~10,000 | Merge sort |
| O(n²) | 1,000,000 | Nested loops |
| O(n³) | 1,000,000,000 | 3 nested loops |
| O(2ⁿ) | 2^1000 ≈ ∞ | Subsets |
| O(n!) | Astronomically large | Permutations |

---

## O(1) - Constant Time

**Operations don't depend on input size.**

```python
# Examples
arr[i]                  # Array access
map[key]                # HashMap get/put
stack.push() / pop()    # Stack operations
queue.offer() / poll()  # Queue operations
set.add() / contains()  # HashSet operations
```

### Common O(1) Operations:
- Array index access/update
- HashMap/HashSet operations (average)
- Push/pop from stack
- Linked list head insertion
- Math operations

---

## O(log n) - Logarithmic Time

**Input size halves each iteration.**

```python
# Binary Search
def binary_search(arr, target):
    left, right = 0, len(arr) - 1
    while left <= right:              # O(log n) iterations
        mid = (left + right) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    return -1
```

### Common O(log n) Operations:
- Binary search
- Balanced BST operations (TreeMap/TreeSet)
- Heap push/pop
- Binary exponentiation
- Finding digits of a number

### Key Insight:
```
log₂(n) = number of times you can divide n by 2 until you reach 1

n = 1024 → log₂(1024) = 10 (divide by 2 ten times)
n = 1,000,000 → log₂(1M) ≈ 20
```

---

## O(n) - Linear Time

**Visit each element once.**

```python
# Linear scan
def find_max(arr):
    max_val = arr[0]
    for num in arr:           # O(n)
        max_val = max(max_val, num)
    return max_val

# Two pointers (still O(n))
def two_sum_sorted(arr, target):
    left, right = 0, len(arr) - 1
    while left < right:       # O(n) total iterations
        curr_sum = arr[left] + arr[right]
        if curr_sum == target:
            return [left, right]
        elif curr_sum < target:
            left += 1
        else:
            right -= 1
```

### Common O(n) Operations:
- Linear search
- Array traversal
- Linked list traversal
- Two pointers technique
- Sliding window
- Building a HashMap from array
- Counting sort (when range is O(n))

---

## O(n log n) - Linearithmic Time

**Divide and conquer, or sorting.**

```python
# Merge Sort
def merge_sort(arr):
    if len(arr) <= 1:
        return arr
    
    mid = len(arr) // 2
    left = merge_sort(arr[:mid])    # T(n/2)
    right = merge_sort(arr[mid:])   # T(n/2)
    
    return merge(left, right)       # O(n) merge

# T(n) = 2T(n/2) + O(n) = O(n log n)
```

### Why O(n log n)?
```
Level 0:    [n elements]                    → n work
Level 1:    [n/2] [n/2]                     → n work
Level 2:    [n/4] [n/4] [n/4] [n/4]         → n work
...
Level log n: [1] [1] ... [1]               → n work

Total: n × log n levels = O(n log n)
```

### Common O(n log n) Operations:
- Merge sort
- Quick sort (average)
- Heap sort
- TimSort (Python/Java default)
- Building a heap from array
- Sorting-based problems

---

## O(n²) - Quadratic Time

**Nested loops over the input.**

```python
# Bubble Sort
def bubble_sort(arr):
    n = len(arr)
    for i in range(n):            # O(n)
        for j in range(n - i - 1): # O(n)
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]

# Brute force two sum
def two_sum_brute(arr, target):
    n = len(arr)
    for i in range(n):            # O(n)
        for j in range(i + 1, n): # O(n)
            if arr[i] + arr[j] == target:
                return [i, j]
```

### Common O(n²) Operations:
- Bubble sort, Selection sort, Insertion sort
- Brute force pair finding
- Comparing all pairs
- 2D matrix traversal (n × n)
- DP with 2D table

---

## O(2ⁿ) - Exponential Time

**Recursive problems with branching.**

```python
# Fibonacci (naive)
def fib(n):
    if n <= 1:
        return n
    return fib(n - 1) + fib(n - 2)  # Two branches per call

# Generating all subsets
def subsets(nums):
    result = []
    def backtrack(index, current):
        if index == len(nums):
            result.append(current[:])
            return
        # Include
        current.append(nums[index])
        backtrack(index + 1, current)
        current.pop()
        # Exclude
        backtrack(index + 1, current)
    backtrack(0, [])
    return result  # 2^n subsets
```

### Common O(2ⁿ) Operations:
- Generating all subsets
- Recursive Fibonacci (without memoization)
- Solving problems by trying all combinations
- Backtracking (worst case)

---

## O(n!) - Factorial Time

**Permutations and arrangements.**

```python
# Generate all permutations
def permutations(nums):
    result = []
    def backtrack(current, remaining):
        if not remaining:
            result.append(current[:])
            return
        for i in range(len(remaining)):
            current.append(remaining[i])
            backtrack(current, remaining[:i] + remaining[i+1:])
            current.pop()
    backtrack([], nums)
    return result  # n! permutations
```

### Common O(n!) Operations:
- Generating all permutations
- Traveling Salesman (brute force)
- N-Queens (brute force)

---

## Space Complexity

### Stack Space (Recursion)

```python
# O(n) space - linear recursion depth
def factorial(n):
    if n <= 1:
        return 1
    return n * factorial(n - 1)  # n calls on stack

# O(log n) space - binary recursion depth
def binary_search_recursive(arr, target, left, right):
    if left > right:
        return -1
    mid = (left + right) // 2
    if arr[mid] == target:
        return mid
    elif arr[mid] < target:
        return binary_search_recursive(arr, target, mid + 1, right)
    else:
        return binary_search_recursive(arr, target, left, mid - 1)
```

### Auxiliary Space

| Data Structure | Space |
|----------------|-------|
| Extra array | O(n) |
| HashMap | O(n) |
| Recursion stack | O(depth) |
| 2D DP table | O(n²) or O(n) optimized |

---

## Amortized Analysis

**Average time per operation over many operations.**

### ArrayList (Dynamic Array)

```python
# Appending to ArrayList
# Most appends: O(1)
# Occasional resize: O(n)
# Amortized: O(1) per append

# Why? 
# After resize from n to 2n:
# - n items copied (O(n) work)
# - Next n appends are O(1)
# - Average: O(n) / n = O(1) per operation
```

### HashMap

```
Average: O(1) for get/put
Worst case: O(n) with many collisions
Amortized: O(1) with good hash function
```

---

## Master Theorem Cheat Sheet

For recurrences of form: **T(n) = aT(n/b) + f(n)**

| Case | Condition | Complexity |
|------|-----------|------------|
| 1 | f(n) < n^(log_b(a)) | O(n^(log_b(a))) |
| 2 | f(n) = n^(log_b(a)) | O(n^(log_b(a)) × log n) |
| 3 | f(n) > n^(log_b(a)) | O(f(n)) |

### Common Recurrences

| Recurrence | Complexity | Example |
|------------|------------|---------|
| T(n) = T(n/2) + O(1) | O(log n) | Binary search |
| T(n) = T(n-1) + O(1) | O(n) | Linear recursion |
| T(n) = T(n-1) + O(n) | O(n²) | Selection sort |
| T(n) = 2T(n/2) + O(n) | O(n log n) | Merge sort |
| T(n) = 2T(n/2) + O(1) | O(n) | Tree traversal |
| T(n) = 2T(n-1) + O(1) | O(2ⁿ) | Fibonacci naive |

---

## Interview Tips: Complexity Analysis

### 1. Identify the Dominant Operation
```python
def example(arr):
    # O(n) - sorting dominates
    arr.sort()                    # O(n log n)
    for x in arr:                 # O(n)
        print(x)
# Total: O(n log n) + O(n) = O(n log n)
```

### 2. Count Nested Loops
```python
for i in range(n):        # O(n)
    for j in range(n):    # × O(n)
        for k in range(n): # × O(n)
            pass
# Total: O(n³)

for i in range(n):        # O(n)
    for j in range(i):    # × O(i) average
        pass
# Total: 0+1+2+...+(n-1) = n(n-1)/2 = O(n²)
```

### 3. Recognize Common Patterns

| Pattern | Complexity |
|---------|------------|
| Loop from 1 to n | O(n) |
| Nested loops | O(n²) or O(n³) |
| Divide in half each time | O(log n) |
| Sort + iterate | O(n log n) |
| HashMap solution | O(n) usually |
| Recursion with memoization | Depends on states |

### 4. Quick Estimation for Constraints

| Max n | Target Complexity |
|-------|------------------|
| n ≤ 10 | O(n!), O(2ⁿ) ok |
| n ≤ 20-25 | O(2ⁿ) ok |
| n ≤ 100 | O(n³) ok |
| n ≤ 1,000 | O(n²) ok |
| n ≤ 100,000 | O(n log n) or O(n) |
| n ≤ 10,000,000 | O(n) or O(log n) |

---

## Common Algorithm Complexities Reference

### Sorting

| Algorithm | Best | Average | Worst | Space |
|-----------|------|---------|-------|-------|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) |
| Counting Sort | O(n+k) | O(n+k) | O(n+k) | O(k) |
| Radix Sort | O(nk) | O(nk) | O(nk) | O(n+k) |

### Searching

| Algorithm | Average | Worst |
|-----------|---------|-------|
| Linear Search | O(n) | O(n) |
| Binary Search | O(log n) | O(log n) |
| Hash Table | O(1) | O(n) |
| BST Search | O(log n) | O(n) |

### Graph Algorithms

| Algorithm | Complexity |
|-----------|------------|
| BFS | O(V + E) |
| DFS | O(V + E) |
| Dijkstra | O((V + E) log V) |
| Bellman-Ford | O(VE) |
| Floyd-Warshall | O(V³) |
| Topological Sort | O(V + E) |
| Kruskal's MST | O(E log E) |
| Prim's MST | O(E log V) |

### Data Structure Operations

| Operation | Array | Linked List | Hash Table | BST | Heap |
|-----------|-------|-------------|------------|-----|------|
| Access | O(1) | O(n) | O(1)* | O(log n) | - |
| Search | O(n) | O(n) | O(1)* | O(log n) | O(n) |
| Insert | O(n) | O(1) | O(1)* | O(log n) | O(log n) |
| Delete | O(n) | O(1) | O(1)* | O(log n) | O(log n) |
| Min/Max | O(n) | O(n) | O(n) | O(log n) | O(1) |

*Average case, assuming good hash function

