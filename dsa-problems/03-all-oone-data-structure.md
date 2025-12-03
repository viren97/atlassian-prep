# All O'one Data Structure

## Problem Information
- **Difficulty**: Hard
- **Frequency**: 80.0%
- **Acceptance Rate**: 44.1%
- **Topics**: Hash Table, Linked List, Design, Doubly-Linked List
- **LeetCode Link**: https://leetcode.com/problems/all-oone-data-structure

## Problem Description

Design a data structure to store the strings' count with the ability to return the strings with minimum and maximum counts.

Implement the `AllOne` class:
- `AllOne()` Initializes the object of the data structure.
- `inc(String key)` Increments the count of the string `key` by 1. If `key` does not exist in the data structure, insert it with count 1.
- `dec(String key)` Decrements the count of the string `key` by 1. If the count of `key` becomes 0, remove it from the data structure. It is guaranteed that `key` exists in the data structure before the decrement.
- `getMaxKey()` Returns one of the keys with the maximal count. If no element exists, return an empty string `""`.
- `getMinKey()` Returns one of the keys with the minimum count. If no element exists, return an empty string `""`.

**Note:** Each function must run in **O(1)** average time complexity.

## Examples

### Example 1:
```
Input
["AllOne", "inc", "inc", "getMaxKey", "getMinKey", "inc", "getMaxKey", "getMinKey"]
[[], ["hello"], ["hello"], [], [], ["leet"], [], []]
Output
[null, null, null, "hello", "hello", null, "hello", "leet"]

Explanation
AllOne allOne = new AllOne();
allOne.inc("hello");
allOne.inc("hello");
allOne.getMaxKey(); // return "hello"
allOne.getMinKey(); // return "hello"
allOne.inc("leet");
allOne.getMaxKey(); // return "hello"
allOne.getMinKey(); // return "leet"
```

## Constraints

- `1 <= key.length <= 10`
- `key` consists of lowercase English letters.
- It is guaranteed that for each call to `dec`, `key` is existing in the data structure.
- At most `5 * 10^4` calls will be made to `inc`, `dec`, `getMaxKey`, and `getMinKey`.

## Approach

### Key Insights:
1. We need O(1) for all operations
2. A simple hash map won't give O(1) min/max
3. We need to maintain keys grouped by their counts
4. Use a doubly-linked list of "buckets" where each bucket contains keys with the same count

### Data Structures:
1. **Hash Map**: key → bucket node (for O(1) access to key's bucket)
2. **Doubly-Linked List**: Each node is a "bucket" containing:
   - count value
   - set of keys with that count
   - prev/next pointers

### Algorithm:
- Keep buckets sorted by count (head = min, tail = max)
- When incrementing/decrementing, move keys between adjacent buckets
- Create/delete buckets as needed

## Solution

```python
class Bucket:
    def __init__(self, count=0):
        self.count = count
        self.keys = set()
        self.prev = None
        self.next = None

class AllOne:
    def __init__(self):
        # Dummy head and tail for easier list manipulation
        self.head = Bucket(float('-inf'))  # min sentinel
        self.tail = Bucket(float('inf'))   # max sentinel
        self.head.next = self.tail
        self.tail.prev = self.head
        
        # key -> bucket mapping
        self.key_bucket = {}
    
    def _insert_bucket_after(self, new_bucket, prev_bucket):
        """Insert new_bucket after prev_bucket in the linked list"""
        new_bucket.prev = prev_bucket
        new_bucket.next = prev_bucket.next
        prev_bucket.next.prev = new_bucket
        prev_bucket.next = new_bucket
    
    def _remove_bucket(self, bucket):
        """Remove bucket from the linked list"""
        bucket.prev.next = bucket.next
        bucket.next.prev = bucket.prev
    
    def _remove_key_from_bucket(self, key, bucket):
        """Remove key from bucket, and remove bucket if empty"""
        bucket.keys.remove(key)
        if not bucket.keys:
            self._remove_bucket(bucket)
    
    def inc(self, key: str) -> None:
        if key not in self.key_bucket:
            # New key, should have count 1
            # Check if bucket with count 1 exists after head
            if self.head.next.count != 1:
                # Create new bucket with count 1
                new_bucket = Bucket(1)
                self._insert_bucket_after(new_bucket, self.head)
            
            self.head.next.keys.add(key)
            self.key_bucket[key] = self.head.next
        else:
            # Existing key, increment its count
            current_bucket = self.key_bucket[key]
            new_count = current_bucket.count + 1
            
            # Check if next bucket has the new count
            if current_bucket.next.count != new_count:
                # Create new bucket
                new_bucket = Bucket(new_count)
                self._insert_bucket_after(new_bucket, current_bucket)
            
            # Move key to the next bucket
            current_bucket.next.keys.add(key)
            self.key_bucket[key] = current_bucket.next
            
            # Remove key from current bucket
            self._remove_key_from_bucket(key, current_bucket)
    
    def dec(self, key: str) -> None:
        current_bucket = self.key_bucket[key]
        new_count = current_bucket.count - 1
        
        if new_count == 0:
            # Remove key entirely
            del self.key_bucket[key]
        else:
            # Check if prev bucket has the new count
            if current_bucket.prev.count != new_count:
                # Create new bucket
                new_bucket = Bucket(new_count)
                self._insert_bucket_after(new_bucket, current_bucket.prev)
            
            # Move key to the prev bucket
            current_bucket.prev.keys.add(key)
            self.key_bucket[key] = current_bucket.prev
        
        # Remove key from current bucket
        self._remove_key_from_bucket(key, current_bucket)
    
    def getMaxKey(self) -> str:
        if self.tail.prev == self.head:
            return ""
        # Return any key from the max bucket
        return next(iter(self.tail.prev.keys))
    
    def getMinKey(self) -> str:
        if self.head.next == self.tail:
            return ""
        # Return any key from the min bucket
        return next(iter(self.head.next.keys))
```

## Alternative Solution (Using OrderedDict-like structure)

```python
from collections import defaultdict

class AllOne:
    def __init__(self):
        self.key_count = {}  # key -> count
        self.count_keys = defaultdict(set)  # count -> set of keys
        self.min_count = 0
        self.max_count = 0
    
    def _update_min_max(self):
        # Find new min (O(n) in worst case, but amortized O(1))
        if not self.key_count:
            self.min_count = self.max_count = 0
            return
        
        while self.min_count > 0 and not self.count_keys[self.min_count]:
            self.min_count += 1
        
        while self.max_count > 0 and not self.count_keys[self.max_count]:
            self.max_count -= 1
    
    def inc(self, key: str) -> None:
        if key in self.key_count:
            old_count = self.key_count[key]
            new_count = old_count + 1
            
            self.count_keys[old_count].remove(key)
            self.count_keys[new_count].add(key)
            self.key_count[key] = new_count
            
            if old_count == self.min_count and not self.count_keys[old_count]:
                self.min_count = new_count
            self.max_count = max(self.max_count, new_count)
        else:
            self.key_count[key] = 1
            self.count_keys[1].add(key)
            self.min_count = 1
            self.max_count = max(self.max_count, 1)
    
    def dec(self, key: str) -> None:
        old_count = self.key_count[key]
        new_count = old_count - 1
        
        self.count_keys[old_count].remove(key)
        
        if new_count == 0:
            del self.key_count[key]
        else:
            self.count_keys[new_count].add(key)
            self.key_count[key] = new_count
        
        self._update_min_max()
    
    def getMaxKey(self) -> str:
        if not self.key_count:
            return ""
        return next(iter(self.count_keys[self.max_count]))
    
    def getMinKey(self) -> str:
        if not self.key_count:
            return ""
        return next(iter(self.count_keys[self.min_count]))
```

## Complexity Analysis

### Doubly-Linked List Solution:
- **inc**: O(1) - All operations are constant time
- **dec**: O(1) - All operations are constant time
- **getMaxKey**: O(1) - Direct access to tail's previous bucket
- **getMinKey**: O(1) - Direct access to head's next bucket

### Space Complexity: O(n)
- Where n is the number of unique keys
- Each key is in exactly one bucket

## Key Patterns & Techniques

1. **Doubly-Linked List with Sentinel Nodes**: Simplifies edge cases at head/tail
2. **Bucket Design Pattern**: Grouping items with the same property
3. **Hash Map + Linked List Combination**: O(1) access + O(1) order maintenance
4. **Lazy Operations**: Only create/delete buckets when necessary

## Visual Representation

```
                 Doubly-Linked List (sorted by count)
                 
Head (min) ←→ Bucket(1) ←→ Bucket(2) ←→ Bucket(5) ←→ Tail (max)
                 ↓            ↓            ↓
             {"leet"}    {"hello"}    {"world"}
                         {"foo"}
                         
key_bucket HashMap:
  "leet"  → Bucket(1)
  "hello" → Bucket(2)
  "foo"   → Bucket(2)
  "world" → Bucket(5)
```

## Common Mistakes to Avoid

1. Not handling the case when a bucket becomes empty
2. Not creating new buckets when incrementing/decrementing to non-adjacent counts
3. Forgetting to update the key_bucket mapping when moving keys
4. Not handling edge cases (empty data structure, single key)

## Related Problems

- [146. LRU Cache](https://leetcode.com/problems/lru-cache/)
- [460. LFU Cache](https://leetcode.com/problems/lfu-cache/)
- [355. Design Twitter](https://leetcode.com/problems/design-twitter/)

