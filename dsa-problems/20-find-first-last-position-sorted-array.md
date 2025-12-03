# Find First and Last Position of Element in Sorted Array

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 46.8%
- **Topics**: Array, Binary Search
- **LeetCode Link**: https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array

## Problem Description

Given an array of integers `nums` sorted in non-decreasing order, find the starting and ending position of a given `target` value.

If `target` is not found in the array, return `[-1, -1]`.

You must write an algorithm with **O(log n)** runtime complexity.

## Examples

### Example 1:
```
Input: nums = [5,7,7,8,8,10], target = 8
Output: [3,4]
```

### Example 2:
```
Input: nums = [5,7,7,8,8,10], target = 6
Output: [-1,-1]
```

### Example 3:
```
Input: nums = [], target = 0
Output: [-1,-1]
```

## Constraints

- `0 <= nums.length <= 10^5`
- `-10^9 <= nums[i] <= 10^9`
- `nums` is a non-decreasing array.
- `-10^9 <= target <= 10^9`

## Approach

### Key Insight:
- Use two binary searches: one for leftmost position, one for rightmost
- Binary search for leftmost: find first occurrence where `nums[i] >= target`
- Binary search for rightmost: find first occurrence where `nums[i] > target`, then subtract 1

### Algorithm:
1. Find leftmost index where `nums[i] == target`
2. Find rightmost index where `nums[i] == target`
3. Return `[left, right]` or `[-1, -1]` if not found

## Solution

```python
from typing import List

class Solution:
    def searchRange(self, nums: List[int], target: int) -> List[int]:
        def find_left(nums, target):
            """Find leftmost index where nums[i] >= target"""
            left, right = 0, len(nums)
            while left < right:
                mid = (left + right) // 2
                if nums[mid] < target:
                    left = mid + 1
                else:
                    right = mid
            return left
        
        def find_right(nums, target):
            """Find leftmost index where nums[i] > target"""
            left, right = 0, len(nums)
            while left < right:
                mid = (left + right) // 2
                if nums[mid] <= target:
                    left = mid + 1
                else:
                    right = mid
            return left
        
        left_idx = find_left(nums, target)
        
        # Check if target exists
        if left_idx == len(nums) or nums[left_idx] != target:
            return [-1, -1]
        
        right_idx = find_right(nums, target) - 1
        
        return [left_idx, right_idx]
```

## Solution Using bisect Module

```python
from typing import List
import bisect

class Solution:
    def searchRange(self, nums: List[int], target: int) -> List[int]:
        left_idx = bisect.bisect_left(nums, target)
        
        # Check if target exists
        if left_idx == len(nums) or nums[left_idx] != target:
            return [-1, -1]
        
        right_idx = bisect.bisect_right(nums, target) - 1
        
        return [left_idx, right_idx]
```

## Solution with Single Binary Search Function

```python
from typing import List

class Solution:
    def searchRange(self, nums: List[int], target: int) -> List[int]:
        def binary_search(nums, target, find_first):
            """
            find_first=True: find first occurrence
            find_first=False: find last occurrence
            """
            left, right = 0, len(nums) - 1
            result = -1
            
            while left <= right:
                mid = (left + right) // 2
                
                if nums[mid] == target:
                    result = mid
                    if find_first:
                        right = mid - 1  # Keep searching left
                    else:
                        left = mid + 1   # Keep searching right
                elif nums[mid] < target:
                    left = mid + 1
                else:
                    right = mid - 1
            
            return result
        
        first = binary_search(nums, target, True)
        if first == -1:
            return [-1, -1]
        
        last = binary_search(nums, target, False)
        return [first, last]
```

## Detailed Solution with Comments

```python
from typing import List

class Solution:
    def searchRange(self, nums: List[int], target: int) -> List[int]:
        """
        Two binary searches:
        1. Find leftmost target (lower bound)
        2. Find rightmost target (upper bound - 1)
        """
        
        def lower_bound(arr, target):
            """
            Find smallest index i such that arr[i] >= target
            Returns len(arr) if all elements < target
            """
            lo, hi = 0, len(arr)
            while lo < hi:
                mid = lo + (hi - lo) // 2
                if arr[mid] < target:
                    lo = mid + 1
                else:
                    hi = mid
            return lo
        
        def upper_bound(arr, target):
            """
            Find smallest index i such that arr[i] > target
            Returns len(arr) if all elements <= target
            """
            lo, hi = 0, len(arr)
            while lo < hi:
                mid = lo + (hi - lo) // 2
                if arr[mid] <= target:
                    lo = mid + 1
                else:
                    hi = mid
            return lo
        
        # Find range
        left = lower_bound(nums, target)
        
        # Verify target exists
        if left >= len(nums) or nums[left] != target:
            return [-1, -1]
        
        right = upper_bound(nums, target) - 1
        
        return [left, right]
```

## Complexity Analysis

### Time Complexity: O(log n)
- Two binary searches, each O(log n)
- Total: O(2 × log n) = O(log n)

### Space Complexity: O(1)
- Only using constant extra space

## Visual Example

```
nums = [5, 7, 7, 8, 8, 10], target = 8
index:  0  1  2  3  4   5

Find lower_bound(8):
- lo=0, hi=6, mid=3: nums[3]=8 >= 8, hi=3
- lo=0, hi=3, mid=1: nums[1]=7 < 8, lo=2
- lo=2, hi=3, mid=2: nums[2]=7 < 8, lo=3
- lo=3, hi=3: return 3 ✓

Find upper_bound(8):
- lo=0, hi=6, mid=3: nums[3]=8 <= 8, lo=4
- lo=4, hi=6, mid=5: nums[5]=10 > 8, hi=5
- lo=4, hi=5, mid=4: nums[4]=8 <= 8, lo=5
- lo=5, hi=5: return 5

Result: [3, 5-1] = [3, 4] ✓
```

## Key Patterns & Techniques

1. **Two Binary Searches**: Find lower and upper bounds
2. **Lower Bound**: First element >= target
3. **Upper Bound**: First element > target
4. **Boundary Handling**: Check if target actually exists

## Binary Search Variants

```
For array: [1, 2, 2, 2, 3, 4], target = 2

lower_bound (bisect_left):  returns index 1 (first 2)
upper_bound (bisect_right): returns index 4 (first element > 2)

For non-existent target = 2.5:
lower_bound: returns index 4 (first element >= 2.5)
upper_bound: returns index 4 (same - no element = 2.5)
```

## Common Mistakes to Avoid

1. **Off-by-one errors**: Right bound is `upper_bound - 1`
2. **Not checking if target exists**: lower_bound might point to different element
3. **Using wrong comparison operators**: `<` vs `<=`
4. **Infinite loops**: Make sure search space shrinks

## Edge Cases

1. Empty array → `[-1, -1]`
2. Target not in array → `[-1, -1]`
3. Single element matching → `[0, 0]`
4. All elements are target → `[0, n-1]`
5. Target at boundaries → check both ends

## Related Problems

- [35. Search Insert Position](https://leetcode.com/problems/search-insert-position/)
- [704. Binary Search](https://leetcode.com/problems/binary-search/)
- [278. First Bad Version](https://leetcode.com/problems/first-bad-version/)
- [744. Find Smallest Letter Greater Than Target](https://leetcode.com/problems/find-smallest-letter-greater-than-target/)

