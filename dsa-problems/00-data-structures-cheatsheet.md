# Data Structures Library Cheatsheet

A practical guide to using built-in data structures in interviews. Covers Python, Java, and Kotlin with common use cases.

---

## Quick Reference Table

| Need | Python | Java | Kotlin |
|------|--------|------|--------|
| Dynamic Array | `list` | `ArrayList` | `MutableList` |
| Fixed Array | `list` | `int[]` | `IntArray` |
| Stack | `list` (append/pop) | `Deque` | `ArrayDeque` |
| Queue | `deque` | `Deque` | `ArrayDeque` |
| HashMap | `dict` | `HashMap` | `HashMap` |
| HashSet | `set` | `HashSet` | `HashSet` |
| Sorted Map | N/A (use sorted()) | `TreeMap` | `TreeMap` |
| Sorted Set | N/A (use sorted()) | `TreeSet` | `TreeSet` |
| Heap (Min) | `heapq` | `PriorityQueue` | `PriorityQueue` |
| Heap (Max) | `heapq` (negate) | `PriorityQueue` (Comparator) | `PriorityQueue` |
| Deque | `deque` | `ArrayDeque` | `ArrayDeque` |
| LinkedList | N/A (deque) | `LinkedList` | `LinkedList` |
| Counter | `Counter` | `HashMap` | `groupingBy` |
| Sorted List | `sortedcontainers` | N/A | N/A |

---

## 1. HashMap / Dictionary

### Python
```python
from collections import defaultdict, Counter

# Basic operations
d = {}
d = dict()
d['key'] = 'value'          # Insert/Update
val = d['key']              # Access (throws KeyError if missing)
val = d.get('key', default) # Access with default
del d['key']                # Delete
'key' in d                  # Check existence

# DefaultDict - auto-initialize missing keys
d = defaultdict(int)        # Default value: 0
d = defaultdict(list)       # Default value: []
d = defaultdict(set)        # Default value: set()
d['missing'] += 1           # No KeyError!

# Counter - frequency counting
from collections import Counter
nums = [1, 2, 2, 3, 3, 3]
count = Counter(nums)       # {3: 3, 2: 2, 1: 1}
count.most_common(2)        # [(3, 3), (2, 2)]
count['new'] += 1           # Safe increment
```

### Java
```java
import java.util.*;

// Basic HashMap
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);                    // Insert/Update
int val = map.get("key");             // Access (returns null if missing)
int val = map.getOrDefault("key", 0); // With default
map.remove("key");                    // Delete
map.containsKey("key");               // Check existence

// Compute if absent - useful for grouping
map.computeIfAbsent("key", k -> new ArrayList<>()).add(item);

// Frequency counting
Map<Integer, Integer> freq = new HashMap<>();
for (int num : nums) {
    freq.merge(num, 1, Integer::sum); // Increment or set to 1
}
// Or
freq.put(num, freq.getOrDefault(num, 0) + 1);
```

### Kotlin
```kotlin
// Basic HashMap
val map = mutableMapOf<String, Int>()
val map = hashMapOf<String, Int>()
map["key"] = 1                        // Insert/Update
val value = map["key"]                // Returns null if missing
val value = map.getOrDefault("key", 0)
map.remove("key")
"key" in map

// GetOrPut - compute if absent
val groups = mutableMapOf<String, MutableList<Int>>()
groups.getOrPut("key") { mutableListOf() }.add(item)

// Frequency counting
val freq = mutableMapOf<Int, Int>()
nums.forEach { freq[it] = freq.getOrDefault(it, 0) + 1 }

// Or use groupingBy
val freq = nums.groupingBy { it }.eachCount()
```

---

## 2. Sorted Map (TreeMap)

**Use case**: When you need keys in sorted order, range queries, floor/ceiling operations.

### Java (Primary choice for sorted maps)
```java
import java.util.*;

TreeMap<Integer, String> map = new TreeMap<>();

// Basic operations - O(log n)
map.put(5, "five");
map.put(1, "one");
map.put(10, "ten");

// Iterate in sorted order
for (Map.Entry<Integer, String> e : map.entrySet()) {
    // Keys come out: 1, 5, 10
}

// POWERFUL METHODS - O(log n)
map.firstKey();              // Smallest key: 1
map.lastKey();               // Largest key: 10
map.floorKey(7);             // Greatest key ≤ 7: 5
map.ceilingKey(7);           // Smallest key ≥ 7: 10
map.lowerKey(5);             // Greatest key < 5: 1
map.higherKey(5);            // Smallest key > 5: 10

// Range operations
map.headMap(5);              // Keys < 5: {1}
map.tailMap(5);              // Keys ≥ 5: {5, 10}
map.subMap(1, 10);           // Keys in [1, 10): {1, 5}

// First/Last entries
map.firstEntry();            // Entry with smallest key
map.lastEntry();             // Entry with largest key
map.pollFirstEntry();        // Remove and return smallest
map.pollLastEntry();         // Remove and return largest

// Descending order
NavigableMap<Integer, String> desc = map.descendingMap();
```

### Python (No built-in TreeMap)
```python
# Use sortedcontainers library (pip install sortedcontainers)
from sortedcontainers import SortedDict

sd = SortedDict()
sd[5] = "five"
sd[1] = "one"
sd[10] = "ten"

# Iterate in sorted order
for key in sd:
    print(key, sd[key])  # 1, 5, 10

# Find floor/ceiling manually
import bisect
keys = list(sd.keys())
idx = bisect.bisect_left(keys, 7)  # Floor
idx = bisect.bisect_right(keys, 7) # Ceiling

# Alternative: Use regular dict + sorted()
d = {5: "five", 1: "one", 10: "ten"}
for key in sorted(d.keys()):
    print(key, d[key])
```

### Kotlin
```kotlin
import java.util.TreeMap

val map = TreeMap<Int, String>()
map[5] = "five"
map[1] = "one"
map[10] = "ten"

// Same API as Java
map.firstKey()              // 1
map.lastKey()               // 10
map.floorKey(7)             // 5
map.ceilingKey(7)           // 10
```

### Common Use Cases for TreeMap

```java
// 1. Finding the nearest key
TreeMap<Integer, String> map = new TreeMap<>();
Integer floor = map.floorKey(target);   // Closest ≤ target
Integer ceil = map.ceilingKey(target);  // Closest ≥ target

// 2. Range counting (keys in [lo, hi])
int count = map.subMap(lo, true, hi, true).size();

// 3. Stock price at timestamp (find latest before or at time)
TreeMap<Integer, Integer> prices = new TreeMap<>();
Integer latestTime = prices.floorKey(queryTime);
if (latestTime != null) {
    int price = prices.get(latestTime);
}

// 4. Interval scheduling with TreeMap
TreeMap<Integer, Integer> intervals = new TreeMap<>(); // start -> end
Integer prevEnd = intervals.floorKey(newStart);
if (prevEnd == null || intervals.get(prevEnd) <= newStart) {
    // No overlap with previous
}
Integer nextStart = intervals.ceilingKey(newStart);
if (nextStart == null || newEnd <= nextStart) {
    // No overlap with next
}
```

---

## 3. Sorted Set (TreeSet)

**Use case**: Maintain unique sorted elements, find floor/ceiling values.

### Java
```java
import java.util.*;

TreeSet<Integer> set = new TreeSet<>();

// Basic operations - O(log n)
set.add(5);
set.add(1);
set.add(10);
set.remove(5);
set.contains(5);

// Sorted iteration
for (int num : set) {
    // 1, 5, 10
}

// POWERFUL METHODS - O(log n)
set.first();                // Smallest: 1
set.last();                 // Largest: 10
set.floor(7);               // Greatest ≤ 7: 5
set.ceiling(7);             // Smallest ≥ 7: 10
set.lower(5);               // Greatest < 5: 1
set.higher(5);              // Smallest > 5: 10

// Range operations
set.headSet(5);             // Elements < 5
set.tailSet(5);             // Elements ≥ 5
set.subSet(1, 10);          // Elements in [1, 10)

// Poll operations (remove and return)
set.pollFirst();            // Remove smallest
set.pollLast();             // Remove largest
```

### Python (Use sortedcontainers)
```python
from sortedcontainers import SortedList

sl = SortedList([5, 1, 10])

# O(log n) operations
sl.add(7)                   # Insert
sl.remove(5)                # Remove
5 in sl                     # Contains

# Access by index - O(log n)
sl[0]                       # Smallest
sl[-1]                      # Largest

# Binary search
sl.bisect_left(7)           # Index where 7 would be inserted (left)
sl.bisect_right(7)          # Index where 7 would be inserted (right)

# Floor/Ceiling
import bisect
idx = sl.bisect_right(7) - 1  # Floor index
if idx >= 0: floor_val = sl[idx]

idx = sl.bisect_left(7)       # Ceiling index
if idx < len(sl): ceil_val = sl[idx]
```

### Common Use Cases for TreeSet

```java
// 1. Finding K closest elements
TreeSet<Integer> set = new TreeSet<>();
Integer floor = set.floor(target);
Integer ceil = set.ceiling(target);
// Compare |floor - target| vs |ceil - target|

// 2. Sliding window with distinct sorted elements
TreeSet<Integer> window = new TreeSet<>();
for (int i = 0; i < nums.length; i++) {
    // Add to window
    window.add(nums[i]);
    
    // Check condition (e.g., max - min <= limit)
    if (window.last() - window.first() > limit) {
        window.remove(nums[i - k]);  // Remove old element
    }
}

// 3. Contains Duplicate III - find if |nums[i] - nums[j]| <= t
TreeSet<Long> set = new TreeSet<>();
for (int i = 0; i < nums.length; i++) {
    Long floor = set.floor((long) nums[i]);
    Long ceil = set.ceiling((long) nums[i]);
    if ((floor != null && nums[i] - floor <= t) ||
        (ceil != null && ceil - nums[i] <= t)) {
        return true;
    }
    set.add((long) nums[i]);
    if (i >= k) set.remove((long) nums[i - k]);
}
```

---

## 4. Heap / Priority Queue

### Min Heap

#### Python
```python
import heapq

# Min heap by default
heap = []
heapq.heappush(heap, 5)
heapq.heappush(heap, 1)
heapq.heappush(heap, 10)

min_val = heap[0]           # Peek min - O(1)
min_val = heapq.heappop(heap)  # Pop min - O(log n)

# Heapify existing list - O(n)
nums = [5, 1, 10, 3]
heapq.heapify(nums)

# N smallest/largest - O(n log k)
heapq.nsmallest(3, nums)
heapq.nlargest(3, nums)
```

#### Java
```java
import java.util.*;

// Min heap by default
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(1);
minHeap.offer(10);

int min = minHeap.peek();     // Peek - O(1)
int min = minHeap.poll();     // Pop - O(log n)
minHeap.size();
minHeap.isEmpty();
```

### Max Heap

#### Python
```python
import heapq

# Negate values for max heap
max_heap = []
heapq.heappush(max_heap, -5)
heapq.heappush(max_heap, -1)

max_val = -max_heap[0]        # Peek max
max_val = -heapq.heappop(max_heap)  # Pop max
```

#### Java
```java
// Max heap with reverse comparator
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
// Or
PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
```

### Heap with Custom Objects

#### Python
```python
import heapq

# Heap of tuples (sorts by first element, then second, etc.)
heap = []
heapq.heappush(heap, (distance, node_id))
heapq.heappush(heap, (5, "task1"))
heapq.heappush(heap, (1, "task2"))

dist, node = heapq.heappop(heap)  # (1, "task2")

# For complex sorting, use tuple with negation
# Max heap by priority, then min by timestamp
heapq.heappush(heap, (-priority, timestamp, task))
```

#### Java
```java
// Heap with custom comparator
PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> {
    if (a[0] != b[0]) return a[0] - b[0];  // Sort by first element
    return a[1] - b[1];  // Then by second
});

// Using Comparator.comparing
PriorityQueue<Task> heap = new PriorityQueue<>(
    Comparator.comparingInt(Task::getPriority)
              .thenComparing(Task::getTimestamp)
);
```

### Common Heap Use Cases

```java
// 1. K Largest Elements
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
for (int num : nums) {
    minHeap.offer(num);
    if (minHeap.size() > k) {
        minHeap.poll();
    }
}
// minHeap now contains k largest elements

// 2. Merge K Sorted Lists
PriorityQueue<ListNode> heap = new PriorityQueue<>(
    (a, b) -> a.val - b.val
);
for (ListNode head : lists) {
    if (head != null) heap.offer(head);
}
while (!heap.isEmpty()) {
    ListNode node = heap.poll();
    // Process node
    if (node.next != null) heap.offer(node.next);
}

// 3. Top K Frequent Elements
Map<Integer, Integer> freq = new HashMap<>();
for (int num : nums) freq.merge(num, 1, Integer::sum);

PriorityQueue<Integer> heap = new PriorityQueue<>(
    (a, b) -> freq.get(a) - freq.get(b)  // Min heap by frequency
);
for (int num : freq.keySet()) {
    heap.offer(num);
    if (heap.size() > k) heap.poll();
}
```

---

## 5. Deque (Double-Ended Queue)

### Python
```python
from collections import deque

dq = deque()
dq = deque([1, 2, 3])

# O(1) operations at both ends
dq.append(4)         # Add to right
dq.appendleft(0)     # Add to left
dq.pop()             # Remove from right
dq.popleft()         # Remove from left

# Access
dq[0]                # Left end
dq[-1]               # Right end

# Rotate
dq.rotate(1)         # Rotate right
dq.rotate(-1)        # Rotate left
```

### Java
```java
import java.util.*;

Deque<Integer> dq = new ArrayDeque<>();

// Add operations
dq.addFirst(1);      // Add to front
dq.addLast(2);       // Add to back
dq.offerFirst(0);    // Add to front (returns boolean)
dq.offerLast(3);     // Add to back (returns boolean)

// Remove operations
dq.removeFirst();    // Remove from front (throws if empty)
dq.removeLast();     // Remove from back
dq.pollFirst();      // Remove from front (returns null if empty)
dq.pollLast();       // Remove from back

// Peek operations
dq.peekFirst();      // View front
dq.peekLast();       // View back
dq.getFirst();       // View front (throws if empty)
dq.getLast();        // View back
```

### Common Deque Use Cases

```python
# 1. Sliding Window Maximum
from collections import deque

def maxSlidingWindow(nums, k):
    dq = deque()  # Store indices of useful elements (decreasing order)
    result = []
    
    for i, num in enumerate(nums):
        # Remove indices outside window
        while dq and dq[0] <= i - k:
            dq.popleft()
        
        # Remove smaller elements (they're useless)
        while dq and nums[dq[-1]] < num:
            dq.pop()
        
        dq.append(i)
        
        if i >= k - 1:
            result.append(nums[dq[0]])  # Front has max
    
    return result

# 2. BFS with Deque (0-1 BFS)
# For graphs with edge weights 0 or 1
dq = deque([(start, 0)])  # (node, distance)
while dq:
    node, dist = dq.popleft()
    for neighbor, weight in graph[node]:
        new_dist = dist + weight
        if weight == 0:
            dq.appendleft((neighbor, new_dist))  # Add to front
        else:
            dq.append((neighbor, new_dist))       # Add to back
```

---

## 6. Sorting with Custom Comparators

### Python
```python
# Sort list in place
nums.sort()                          # Ascending
nums.sort(reverse=True)              # Descending

# Sort by custom key
items.sort(key=lambda x: x[1])       # By second element
items.sort(key=lambda x: (x[0], -x[1]))  # By first asc, second desc

# Sort strings by length, then alphabetically
words.sort(key=lambda x: (len(x), x))

# sorted() returns new list
sorted_nums = sorted(nums)
sorted_items = sorted(items, key=lambda x: x[1])

# Sort with custom comparison (Python 3)
from functools import cmp_to_key
def compare(a, b):
    if a + b > b + a:
        return -1
    elif a + b < b + a:
        return 1
    return 0
nums.sort(key=cmp_to_key(compare))
```

### Java
```java
import java.util.*;

// Sort array
Arrays.sort(nums);                                    // Primitive array
Arrays.sort(nums, Collections.reverseOrder());       // Object array descending

// Sort with lambda
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);       // By first element
Arrays.sort(intervals, (a, b) -> {
    if (a[0] != b[0]) return a[0] - b[0];            // By first, then second
    return a[1] - b[1];
});

// Sort list
Collections.sort(list);
Collections.sort(list, Collections.reverseOrder());
list.sort((a, b) -> a.getValue() - b.getValue());

// Using Comparator.comparing
list.sort(Comparator.comparingInt(Person::getAge)
                    .thenComparing(Person::getName));

// Sort map by value
List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

// Or get sorted keys by value
List<String> sortedKeys = map.entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .map(Map.Entry::getKey)
    .collect(Collectors.toList());
```

### Kotlin
```kotlin
// Sort list
val sorted = list.sorted()                    // Returns new list
val sortedDesc = list.sortedDescending()
list.sortBy { it.age }                        // In-place by property
list.sortByDescending { it.age }

// Sort with multiple criteria
list.sortedWith(compareBy({ it.age }, { it.name }))

// Sort map by value
val sortedByValue = map.entries.sortedBy { it.value }

// Custom comparator
val sorted = list.sortedWith { a, b ->
    when {
        a.priority != b.priority -> b.priority - a.priority
        else -> a.name.compareTo(b.name)
    }
}
```

---

## 7. Common Interview Patterns with Data Structures

### Pattern 1: Two Sum / Three Sum (HashMap)

```python
# Two Sum - O(n)
def twoSum(nums, target):
    seen = {}  # value -> index
    for i, num in enumerate(nums):
        complement = target - num
        if complement in seen:
            return [seen[complement], i]
        seen[num] = i
```

### Pattern 2: Sliding Window with HashMap

```python
# Longest Substring Without Repeating Characters
def lengthOfLongestSubstring(s):
    char_index = {}
    max_len = 0
    left = 0
    
    for right, char in enumerate(s):
        if char in char_index and char_index[char] >= left:
            left = char_index[char] + 1
        char_index[char] = right
        max_len = max(max_len, right - left + 1)
    
    return max_len
```

### Pattern 3: Top K Elements (Heap)

```python
import heapq
from collections import Counter

def topKFrequent(nums, k):
    count = Counter(nums)
    # Min heap of size k
    return heapq.nlargest(k, count.keys(), key=count.get)
```

### Pattern 4: Merge Intervals (Sorting)

```python
def merge(intervals):
    intervals.sort(key=lambda x: x[0])
    merged = [intervals[0]]
    
    for start, end in intervals[1:]:
        if start <= merged[-1][1]:
            merged[-1][1] = max(merged[-1][1], end)
        else:
            merged.append([start, end])
    
    return merged
```

### Pattern 5: LRU Cache (HashMap + DoublyLinkedList)

```python
from collections import OrderedDict

class LRUCache:
    def __init__(self, capacity):
        self.cache = OrderedDict()
        self.capacity = capacity
    
    def get(self, key):
        if key not in self.cache:
            return -1
        self.cache.move_to_end(key)
        return self.cache[key]
    
    def put(self, key, value):
        if key in self.cache:
            self.cache.move_to_end(key)
        self.cache[key] = value
        if len(self.cache) > self.capacity:
            self.cache.popitem(last=False)
```

### Pattern 6: Stock Price / Time Series (TreeMap)

```java
// Stock price at any timestamp
class StockPrice {
    TreeMap<Integer, Integer> timestampPrice = new TreeMap<>();
    TreeMap<Integer, Integer> priceCount = new TreeMap<>();  // For min/max
    int latestTime = 0;
    
    void update(int timestamp, int price) {
        latestTime = Math.max(latestTime, timestamp);
        
        if (timestampPrice.containsKey(timestamp)) {
            int oldPrice = timestampPrice.get(timestamp);
            removePrice(oldPrice);
        }
        
        timestampPrice.put(timestamp, price);
        addPrice(price);
    }
    
    int current() {
        return timestampPrice.get(latestTime);
    }
    
    int maximum() {
        return priceCount.lastKey();
    }
    
    int minimum() {
        return priceCount.firstKey();
    }
}
```

---

## 8. Quick Copy-Paste Templates

### Frequency Counter
```python
from collections import Counter
freq = Counter(nums)
most_common = freq.most_common(k)
```

### Graph Adjacency List
```python
from collections import defaultdict
graph = defaultdict(list)
for u, v in edges:
    graph[u].append(v)
    graph[v].append(u)  # For undirected
```

### BFS Template
```python
from collections import deque
queue = deque([start])
visited = {start}
while queue:
    node = queue.popleft()
    for neighbor in graph[node]:
        if neighbor not in visited:
            visited.add(neighbor)
            queue.append(neighbor)
```

### Binary Search Template
```python
left, right = 0, len(arr) - 1
while left < right:
    mid = (left + right) // 2
    if condition(mid):
        right = mid
    else:
        left = mid + 1
return left
```

### Union-Find Template
```python
class UnionFind:
    def __init__(self, n):
        self.parent = list(range(n))
        self.rank = [0] * n
    
    def find(self, x):
        if self.parent[x] != x:
            self.parent[x] = self.find(self.parent[x])
        return self.parent[x]
    
    def union(self, x, y):
        px, py = self.find(x), self.find(y)
        if px == py: return False
        if self.rank[px] < self.rank[py]:
            px, py = py, px
        self.parent[py] = px
        if self.rank[px] == self.rank[py]:
            self.rank[px] += 1
        return True
```

