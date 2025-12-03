# Merge Intervals

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 50.8%
- **Acceptance Rate**: 49.4%
- **Topics**: Array, Sorting
- **LeetCode Link**: https://leetcode.com/problems/merge-intervals

## Problem Description

Given an array of `intervals` where `intervals[i] = [starti, endi]`, merge all overlapping intervals, and return an array of the non-overlapping intervals that cover all the intervals in the input.

## Examples

### Example 1:
```
Input: intervals = [[1,3],[2,6],[8,10],[15,18]]
Output: [[1,6],[8,10],[15,18]]
Explanation: Since intervals [1,3] and [2,6] overlap, merge them into [1,6].
```

### Example 2:
```
Input: intervals = [[1,4],[4,5]]
Output: [[1,5]]
Explanation: Intervals [1,4] and [4,5] are considered overlapping (they touch at 4).
```

## Constraints

- `1 <= intervals.length <= 10^4`
- `intervals[i].length == 2`
- `0 <= starti <= endi <= 10^4`

## Approach

### Key Insight:
- Sort intervals by start time
- After sorting, overlapping intervals will be adjacent
- Two intervals overlap if `current.start <= previous.end`

### Algorithm:
1. Sort intervals by start time
2. Initialize result with first interval
3. For each subsequent interval:
   - If it overlaps with last merged interval, extend the end
   - Otherwise, add it as a new interval

## Solution

```python
from typing import List

class Solution:
    def merge(self, intervals: List[List[int]]) -> List[List[int]]:
        if not intervals:
            return []
        
        # Sort by start time
        intervals.sort(key=lambda x: x[0])
        
        merged = [intervals[0]]
        
        for current in intervals[1:]:
            last = merged[-1]
            
            # Check if overlapping (current starts before last ends)
            if current[0] <= last[1]:
                # Merge: extend the end time
                last[1] = max(last[1], current[1])
            else:
                # No overlap: add new interval
                merged.append(current)
        
        return merged
```

## Solution with In-place Modification

```python
from typing import List

class Solution:
    def merge(self, intervals: List[List[int]]) -> List[List[int]]:
        intervals.sort(key=lambda x: x[0])
        
        result = []
        
        for interval in intervals:
            # If result is empty or no overlap with last interval
            if not result or result[-1][1] < interval[0]:
                result.append(interval)
            else:
                # Overlap: merge with last interval
                result[-1][1] = max(result[-1][1], interval[1])
        
        return result
```

## Solution with Explicit Variables

```python
from typing import List

class Solution:
    def merge(self, intervals: List[List[int]]) -> List[List[int]]:
        if len(intervals) <= 1:
            return intervals
        
        # Sort by start time
        intervals.sort(key=lambda x: x[0])
        
        result = []
        current_start = intervals[0][0]
        current_end = intervals[0][1]
        
        for i in range(1, len(intervals)):
            start, end = intervals[i]
            
            if start <= current_end:
                # Overlapping: extend current interval
                current_end = max(current_end, end)
            else:
                # No overlap: save current and start new
                result.append([current_start, current_end])
                current_start = start
                current_end = end
        
        # Don't forget the last interval
        result.append([current_start, current_end])
        
        return result
```

## Complexity Analysis

### Time Complexity: O(n log n)
- Sorting: O(n log n)
- Single pass through intervals: O(n)
- Total: O(n log n)

### Space Complexity: O(n)
- Result array: O(n) in worst case (no overlaps)
- Sorting: O(log n) or O(n) depending on implementation

## Visual Example

```
Input: [[1,3], [2,6], [8,10], [15,18]]

After sorting (already sorted):
[[1,3], [2,6], [8,10], [15,18]]

Step by step:
1. merged = [[1,3]]

2. Current: [2,6]
   - 2 <= 3? Yes, overlap!
   - merged[-1][1] = max(3, 6) = 6
   - merged = [[1,6]]

3. Current: [8,10]
   - 8 <= 6? No, no overlap
   - merged.append([8,10])
   - merged = [[1,6], [8,10]]

4. Current: [15,18]
   - 15 <= 10? No, no overlap
   - merged.append([15,18])
   - merged = [[1,6], [8,10], [15,18]]

Output: [[1,6], [8,10], [15,18]]
```

## Key Patterns & Techniques

1. **Sort + Scan**: Sort by one dimension, then linear scan
2. **Greedy Merging**: Always extend current interval if possible
3. **Overlap Condition**: Two sorted intervals overlap if `a.end >= b.start`

## Overlap Conditions

```
For intervals [a, b] and [c, d] where a <= c (sorted):

Overlap: c <= b
  Case 1: a---b        [1,5] and [3,7] → [1,7]
              c---d
  
  Case 2: a------b     [1,10] and [3,7] → [1,10]
           c--d
  
  Case 3: a---b        [1,5] and [5,7] → [1,7]
              c---d    (touching = overlapping)

No overlap: c > b
            a---b      [1,5] and [7,10] → separate
                 c---d
```

## Edge Cases

1. Empty input → return empty
2. Single interval → return as is
3. All intervals overlap → return single merged interval
4. No intervals overlap → return all intervals
5. Touching intervals (end == start) → should merge

## Common Mistakes to Avoid

1. Forgetting to sort the intervals
2. Using `<` instead of `<=` for overlap check (touching = overlapping)
3. Not taking `max` of ends when merging (nested intervals)
4. Forgetting to add the last interval (in explicit variable approach)

## Follow-up Questions

### 1. Insert and Merge
What if we need to insert a new interval and merge?
```python
def insert(intervals, newInterval):
    result = []
    i = 0
    n = len(intervals)
    
    # Add all intervals before newInterval
    while i < n and intervals[i][1] < newInterval[0]:
        result.append(intervals[i])
        i += 1
    
    # Merge overlapping intervals
    while i < n and intervals[i][0] <= newInterval[1]:
        newInterval[0] = min(newInterval[0], intervals[i][0])
        newInterval[1] = max(newInterval[1], intervals[i][1])
        i += 1
    
    result.append(newInterval)
    
    # Add remaining intervals
    while i < n:
        result.append(intervals[i])
        i += 1
    
    return result
```

### 2. Count Merged Intervals
How many intervals do we end up with after merging?
Just return `len(merged)` from the solution.

## Related Problems

- [57. Insert Interval](https://leetcode.com/problems/insert-interval/)
- [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/)
- [253. Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/)
- [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)
- [986. Interval List Intersections](https://leetcode.com/problems/interval-list-intersections/)

