# Top K Frequent Elements

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 64.6%
- **Topics**: Array, Hash Table, Divide and Conquer, Sorting, Heap (Priority Queue), Bucket Sort, Counting, Quickselect
- **LeetCode Link**: https://leetcode.com/problems/top-k-frequent-elements

## Problem Description

Given an integer array `nums` and an integer `k`, return the `k` most frequent elements. You may return the answer in **any order**.

## Examples

### Example 1:
```
Input: nums = [1,1,1,2,2,3], k = 2
Output: [1,2]
Explanation: 1 appears 3 times, 2 appears 2 times
```

### Example 2:
```
Input: nums = [1], k = 1
Output: [1]
```

## Constraints

- `1 <= nums.length <= 10^5`
- `-10^4 <= nums[i] <= 10^4`
- `k` is in the range `[1, the number of unique elements in the array]`.
- It is **guaranteed** that the answer is **unique**.

## Follow up
Your algorithm's time complexity must be better than `O(n log n)`, where n is the array's size.

## Approach

### Multiple Solutions:
1. **Heap (Priority Queue)**: O(n log k)
2. **Bucket Sort**: O(n) - optimal
3. **Quickselect**: O(n) average
4. **Sorting**: O(n log n) - doesn't meet follow-up requirement

## Solution 1: Min Heap (O(n log k))

```python
from typing import List
from collections import Counter
import heapq

class Solution:
    def topKFrequent(self, nums: List[int], k: int) -> List[int]:
        # Count frequencies
        freq = Counter(nums)
        
        # Use min heap of size k
        # Store (frequency, element)
        min_heap = []
        
        for num, count in freq.items():
            heapq.heappush(min_heap, (count, num))
            if len(min_heap) > k:
                heapq.heappop(min_heap)  # Remove smallest
        
        return [num for count, num in min_heap]
```

## Solution 2: Bucket Sort (O(n)) - Optimal

```python
from typing import List
from collections import Counter

class Solution:
    def topKFrequent(self, nums: List[int], k: int) -> List[int]:
        # Count frequencies
        freq = Counter(nums)
        
        # Create buckets where index = frequency
        # Maximum possible frequency is len(nums)
        buckets = [[] for _ in range(len(nums) + 1)]
        
        for num, count in freq.items():
            buckets[count].append(num)
        
        # Collect k most frequent elements
        result = []
        for i in range(len(buckets) - 1, -1, -1):
            for num in buckets[i]:
                result.append(num)
                if len(result) == k:
                    return result
        
        return result
```

## Solution 3: Using heapq.nlargest

```python
from typing import List
from collections import Counter
import heapq

class Solution:
    def topKFrequent(self, nums: List[int], k: int) -> List[int]:
        freq = Counter(nums)
        # nlargest uses a min heap internally, O(n log k)
        return heapq.nlargest(k, freq.keys(), key=freq.get)
```

## Solution 4: Quickselect (O(n) Average)

```python
from typing import List
from collections import Counter
import random

class Solution:
    def topKFrequent(self, nums: List[int], k: int) -> List[int]:
        freq = Counter(nums)
        unique = list(freq.keys())
        
        def partition(left, right, pivot_index):
            pivot_freq = freq[unique[pivot_index]]
            # Move pivot to end
            unique[pivot_index], unique[right] = unique[right], unique[pivot_index]
            store_index = left
            
            for i in range(left, right):
                if freq[unique[i]] < pivot_freq:
                    unique[store_index], unique[i] = unique[i], unique[store_index]
                    store_index += 1
            
            # Move pivot to final position
            unique[right], unique[store_index] = unique[store_index], unique[right]
            return store_index
        
        def quickselect(left, right, k_smallest):
            if left == right:
                return
            
            pivot_index = random.randint(left, right)
            pivot_index = partition(left, right, pivot_index)
            
            if k_smallest == pivot_index:
                return
            elif k_smallest < pivot_index:
                quickselect(left, pivot_index - 1, k_smallest)
            else:
                quickselect(pivot_index + 1, right, k_smallest)
        
        n = len(unique)
        # We want k largest = (n - k) smallest in left partition
        quickselect(0, n - 1, n - k)
        return unique[n - k:]
```

## Solution 5: Simple Sorting (O(n log n))

```python
from typing import List
from collections import Counter

class Solution:
    def topKFrequent(self, nums: List[int], k: int) -> List[int]:
        freq = Counter(nums)
        # Sort by frequency descending
        sorted_nums = sorted(freq.keys(), key=lambda x: freq[x], reverse=True)
        return sorted_nums[:k]
```

## Complexity Analysis

| Solution | Time | Space |
|----------|------|-------|
| Min Heap | O(n log k) | O(n + k) |
| Bucket Sort | O(n) | O(n) |
| heapq.nlargest | O(n log k) | O(n + k) |
| Quickselect | O(n) avg, O(n²) worst | O(n) |
| Sorting | O(n log n) | O(n) |

## Visual Example: Bucket Sort

```
nums = [1,1,1,2,2,3], k = 2

Step 1: Count frequencies
freq = {1: 3, 2: 2, 3: 1}

Step 2: Create buckets (index = frequency)
buckets[0] = []
buckets[1] = [3]      ← frequency 1
buckets[2] = [2]      ← frequency 2
buckets[3] = [1]      ← frequency 3
buckets[4] = []
buckets[5] = []
buckets[6] = []

Step 3: Collect from high to low frequency
i=3: add 1, result=[1]
i=2: add 2, result=[1,2], len=k=2 ✓

Return [1, 2]
```

## Key Patterns & Techniques

1. **Counting + Data Structure**: Count first, then process
2. **Heap for Top K**: Min heap of size k is efficient
3. **Bucket Sort**: When range is bounded, achieve O(n)
4. **Quickselect**: Average O(n) for kth element problems

## Why Min Heap of Size K?

```
Using min heap of size k:
- Add element: O(log k)
- If size > k, pop minimum: O(log k)
- After processing all elements, heap contains k largest

Why min heap, not max heap?
- We want to remove SMALLEST frequent elements
- Min heap gives us quick access to smallest
- When heap exceeds size k, we remove the smallest (least frequent)
```

## Common Mistakes to Avoid

1. Using max heap with all elements (O(n log n))
2. Not handling ties correctly (problem guarantees unique answer)
3. Forgetting Counter is already O(n) time
4. Off-by-one errors in bucket sort indices

## Edge Cases

1. k = number of unique elements (return all)
2. All elements same (return that element)
3. k = 1 (return most frequent)
4. Negative numbers in array

## Related Problems

- [347. Top K Frequent Elements](https://leetcode.com/problems/top-k-frequent-elements/) (This problem)
- [692. Top K Frequent Words](https://leetcode.com/problems/top-k-frequent-words/)
- [215. Kth Largest Element in an Array](https://leetcode.com/problems/kth-largest-element-in-an-array/)
- [451. Sort Characters By Frequency](https://leetcode.com/problems/sort-characters-by-frequency/)

