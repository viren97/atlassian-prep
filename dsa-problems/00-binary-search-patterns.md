# Binary Search Patterns - Complete Guide

## Binary Search Fundamentals

### Basic Binary Search

```python
def binary_search(arr, target):
    left, right = 0, len(arr) - 1
    
    while left <= right:
        mid = left + (right - left) // 2  # Avoid overflow
        
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return -1  # Not found
```

### Key Insight
Binary search works when there's a **monotonic property** - at some point, the answer changes from True to False (or vice versa).

```
[False, False, False, True, True, True, True]
                      ^
                      Target: First True
```

---

## Template 1: Find Exact Match

```python
def binary_search(arr, target):
    left, right = 0, len(arr) - 1
    
    while left <= right:
        mid = (left + right) // 2
        
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return -1
```

**Use when**: Looking for exact element.

---

## Template 2: Find First/Leftmost (Lower Bound)

```python
def find_first(arr, target):
    """Find first position where arr[i] >= target"""
    left, right = 0, len(arr)
    
    while left < right:
        mid = (left + right) // 2
        
        if arr[mid] < target:
            left = mid + 1
        else:
            right = mid
    
    return left  # First position >= target


def find_first_equal(arr, target):
    """Find first occurrence of target"""
    pos = find_first(arr, target)
    if pos < len(arr) and arr[pos] == target:
        return pos
    return -1
```

**Use when**: Find first occurrence, insert position, or lower bound.

---

## Template 3: Find Last/Rightmost (Upper Bound)

```python
def find_last(arr, target):
    """Find first position where arr[i] > target"""
    left, right = 0, len(arr)
    
    while left < right:
        mid = (left + right) // 2
        
        if arr[mid] <= target:
            left = mid + 1
        else:
            right = mid
    
    return left  # First position > target


def find_last_equal(arr, target):
    """Find last occurrence of target"""
    pos = find_last(arr, target)
    if pos > 0 and arr[pos - 1] == target:
        return pos - 1
    return -1
```

**Use when**: Find last occurrence, upper bound.

---

## Template 4: Binary Search on Answer

```python
def binary_search_on_answer(check, lo, hi):
    """
    Find minimum/maximum value that satisfies condition.
    check(x) returns True/False
    """
    while lo < hi:
        mid = (lo + hi) // 2
        
        if check(mid):
            hi = mid  # For finding minimum valid answer
        else:
            lo = mid + 1
    
    return lo


# For maximum valid answer (condition flips)
def find_maximum(check, lo, hi):
    while lo < hi:
        mid = (lo + hi + 1) // 2  # Bias towards right
        
        if check(mid):
            lo = mid
        else:
            hi = mid - 1
    
    return lo
```

---

## Pattern 1: Find First and Last Position

**LeetCode 34**: Given sorted array, find first and last position of target.

```python
def searchRange(nums, target):
    def find_left():
        left, right = 0, len(nums)
        while left < right:
            mid = (left + right) // 2
            if nums[mid] < target:
                left = mid + 1
            else:
                right = mid
        return left
    
    def find_right():
        left, right = 0, len(nums)
        while left < right:
            mid = (left + right) // 2
            if nums[mid] <= target:
                left = mid + 1
            else:
                right = mid
        return left - 1
    
    left = find_left()
    if left == len(nums) or nums[left] != target:
        return [-1, -1]
    
    return [left, find_right()]
```

---

## Pattern 2: Search in Rotated Sorted Array

**LeetCode 33**: Search in rotated sorted array.

```python
def search(nums, target):
    left, right = 0, len(nums) - 1
    
    while left <= right:
        mid = (left + right) // 2
        
        if nums[mid] == target:
            return mid
        
        # Left half is sorted
        if nums[left] <= nums[mid]:
            if nums[left] <= target < nums[mid]:
                right = mid - 1
            else:
                left = mid + 1
        # Right half is sorted
        else:
            if nums[mid] < target <= nums[right]:
                left = mid + 1
            else:
                right = mid - 1
    
    return -1
```

### Find Minimum in Rotated Array

```python
def findMin(nums):
    left, right = 0, len(nums) - 1
    
    while left < right:
        mid = (left + right) // 2
        
        if nums[mid] > nums[right]:
            # Minimum is in right half
            left = mid + 1
        else:
            # Minimum is in left half (including mid)
            right = mid
    
    return nums[left]
```

---

## Pattern 3: Koko Eating Bananas / Minimize Maximum

**LeetCode 875**: Find minimum eating speed to finish in h hours.

```python
def minEatingSpeed(piles, h):
    def can_finish(speed):
        hours = 0
        for pile in piles:
            hours += (pile + speed - 1) // speed  # Ceiling division
        return hours <= h
    
    left, right = 1, max(piles)
    
    while left < right:
        mid = (left + right) // 2
        
        if can_finish(mid):
            right = mid
        else:
            left = mid + 1
    
    return left
```

### Similar Problems

**Ship Packages in D Days** (LeetCode 1011):

```python
def shipWithinDays(weights, days):
    def can_ship(capacity):
        d = 1
        current_load = 0
        for w in weights:
            if current_load + w > capacity:
                d += 1
                current_load = w
            else:
                current_load += w
        return d <= days
    
    left, right = max(weights), sum(weights)
    
    while left < right:
        mid = (left + right) // 2
        if can_ship(mid):
            right = mid
        else:
            left = mid + 1
    
    return left
```

---

## Pattern 4: Peak Element

**LeetCode 162**: Find a peak element.

```python
def findPeakElement(nums):
    left, right = 0, len(nums) - 1
    
    while left < right:
        mid = (left + right) // 2
        
        if nums[mid] > nums[mid + 1]:
            # Peak is on left side (including mid)
            right = mid
        else:
            # Peak is on right side
            left = mid + 1
    
    return left
```

---

## Pattern 5: Search in Matrix

### Sorted Matrix (Row and Column)

**LeetCode 74**: Search in 2D matrix (each row sorted, first of row > last of prev row).

```python
def searchMatrix(matrix, target):
    if not matrix:
        return False
    
    m, n = len(matrix), len(matrix[0])
    left, right = 0, m * n - 1
    
    while left <= right:
        mid = (left + right) // 2
        row, col = mid // n, mid % n
        val = matrix[row][col]
        
        if val == target:
            return True
        elif val < target:
            left = mid + 1
        else:
            right = mid - 1
    
    return False
```

### Search Matrix II (LeetCode 240)

Rows sorted, columns sorted, but no guarantee about row relationships.

```python
def searchMatrix(matrix, target):
    if not matrix:
        return False
    
    # Start from top-right corner
    row, col = 0, len(matrix[0]) - 1
    
    while row < len(matrix) and col >= 0:
        if matrix[row][col] == target:
            return True
        elif matrix[row][col] > target:
            col -= 1
        else:
            row += 1
    
    return False
```

---

## Pattern 6: Split Array Largest Sum

**LeetCode 410**: Split array into m subarrays to minimize max sum.

```python
def splitArray(nums, m):
    def can_split(max_sum):
        splits = 1
        current_sum = 0
        for num in nums:
            if current_sum + num > max_sum:
                splits += 1
                current_sum = num
            else:
                current_sum += num
        return splits <= m
    
    left, right = max(nums), sum(nums)
    
    while left < right:
        mid = (left + right) // 2
        
        if can_split(mid):
            right = mid
        else:
            left = mid + 1
    
    return left
```

---

## Pattern 7: Median of Two Sorted Arrays

**LeetCode 4**: Find median of two sorted arrays (O(log(min(m,n)))).

```python
def findMedianSortedArrays(nums1, nums2):
    # Ensure nums1 is smaller
    if len(nums1) > len(nums2):
        nums1, nums2 = nums2, nums1
    
    m, n = len(nums1), len(nums2)
    left, right = 0, m
    
    while left <= right:
        partition1 = (left + right) // 2
        partition2 = (m + n + 1) // 2 - partition1
        
        maxLeft1 = float('-inf') if partition1 == 0 else nums1[partition1 - 1]
        minRight1 = float('inf') if partition1 == m else nums1[partition1]
        
        maxLeft2 = float('-inf') if partition2 == 0 else nums2[partition2 - 1]
        minRight2 = float('inf') if partition2 == n else nums2[partition2]
        
        if maxLeft1 <= minRight2 and maxLeft2 <= minRight1:
            # Found correct partition
            if (m + n) % 2 == 0:
                return (max(maxLeft1, maxLeft2) + min(minRight1, minRight2)) / 2
            else:
                return max(maxLeft1, maxLeft2)
        elif maxLeft1 > minRight2:
            right = partition1 - 1
        else:
            left = partition1 + 1
    
    return 0.0
```

---

## Pattern 8: Square Root / Nth Root

```python
def mySqrt(x):
    if x < 2:
        return x
    
    left, right = 1, x // 2
    
    while left <= right:
        mid = (left + right) // 2
        sq = mid * mid
        
        if sq == x:
            return mid
        elif sq < x:
            left = mid + 1
        else:
            right = mid - 1
    
    return right  # Floor of sqrt


def nthRoot(x, n):
    """Find floor of nth root of x"""
    if x < 2:
        return x
    
    left, right = 1, x
    
    while left <= right:
        mid = (left + right) // 2
        power = mid ** n
        
        if power == x:
            return mid
        elif power < x:
            left = mid + 1
        else:
            right = mid - 1
    
    return right
```

---

## Pattern 9: Aggressive Cows / Max Min Distance

**LeetCode 1552**: Place balls in positions to maximize minimum distance.

```python
def maxDistance(position, m):
    position.sort()
    
    def can_place(min_dist):
        count = 1
        last = position[0]
        for i in range(1, len(position)):
            if position[i] - last >= min_dist:
                count += 1
                last = position[i]
        return count >= m
    
    left, right = 1, position[-1] - position[0]
    
    while left < right:
        mid = (left + right + 1) // 2  # Bias right for maximum
        
        if can_place(mid):
            left = mid
        else:
            right = mid - 1
    
    return left
```

---

## Python bisect Module

```python
import bisect

# bisect_left: index where element should be inserted (leftmost position)
# Returns first index where arr[i] >= target
arr = [1, 3, 3, 3, 5, 7]
bisect.bisect_left(arr, 3)   # Returns 1
bisect.bisect_left(arr, 4)   # Returns 4

# bisect_right (same as bisect): index after all existing entries
# Returns first index where arr[i] > target
bisect.bisect_right(arr, 3)  # Returns 4
bisect.bisect(arr, 3)        # Returns 4

# insort_left / insort_right: insert while maintaining sorted order
bisect.insort_left(arr, 4)   # arr = [1, 3, 3, 3, 4, 5, 7]


# Common patterns with bisect
def count_in_range(arr, lo, hi):
    """Count elements in [lo, hi]"""
    return bisect.bisect_right(arr, hi) - bisect.bisect_left(arr, lo)


def exists(arr, target):
    """Check if target exists"""
    idx = bisect.bisect_left(arr, target)
    return idx < len(arr) and arr[idx] == target


def floor(arr, target):
    """Find largest element <= target"""
    idx = bisect.bisect_right(arr, target) - 1
    return arr[idx] if idx >= 0 else None


def ceiling(arr, target):
    """Find smallest element >= target"""
    idx = bisect.bisect_left(arr, target)
    return arr[idx] if idx < len(arr) else None
```

---

## Quick Reference: Which Template?

| Problem Type | Template | Key Insight |
|-------------|----------|-------------|
| Find exact match | `left <= right` | Return when found |
| Find first ≥ target | `left < right`, go left | Lower bound |
| Find last ≤ target | `left < right`, go right | Upper bound |
| Minimize max | `left < right`, check mid | Binary search on answer |
| Maximize min | `left < right`, mid+1 bias | Binary search on answer |
| Rotated array | Check which half sorted | Determine search direction |
| Peak finding | Compare mid with mid+1 | Go towards larger element |

## Common Mistakes to Avoid

1. **Overflow**: Use `mid = left + (right - left) // 2` instead of `(left + right) // 2`
2. **Infinite loop**: Ensure `left < right` and update `left = mid + 1` or `right = mid`
3. **Off-by-one**: For max problems, use `mid = (left + right + 1) // 2`
4. **Wrong boundary**: Initialize `right` to `len(arr)` for insertion index, `len(arr) - 1` for element search

