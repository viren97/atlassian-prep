# Koko Eating Bananas

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 50.8%
- **Acceptance Rate**: 49.1%
- **Topics**: Array, Binary Search
- **LeetCode Link**: https://leetcode.com/problems/koko-eating-bananas

## Problem Description

Koko loves to eat bananas. There are `n` piles of bananas, the `i-th` pile has `piles[i]` bananas. The guards have gone and will come back in `h` hours.

Koko can decide her bananas-per-hour eating speed of `k`. Each hour, she chooses some pile of bananas and eats `k` bananas from that pile. If the pile has less than `k` bananas, she eats all of them instead and will not eat any more bananas during this hour.

Koko likes to eat slowly but still wants to finish eating all the bananas before the guards return.

Return the minimum integer `k` such that she can eat all the bananas within `h` hours.

## Examples

### Example 1:
```
Input: piles = [3,6,7,11], h = 8
Output: 4
Explanation:
- Pile 1 (3 bananas): ceil(3/4) = 1 hour
- Pile 2 (6 bananas): ceil(6/4) = 2 hours
- Pile 3 (7 bananas): ceil(7/4) = 2 hours
- Pile 4 (11 bananas): ceil(11/4) = 3 hours
Total: 1 + 2 + 2 + 3 = 8 hours ≤ 8 ✓
```

### Example 2:
```
Input: piles = [30,11,23,4,20], h = 5
Output: 30
Explanation: Each pile must be eaten in 1 hour, so k = max(piles) = 30
```

### Example 3:
```
Input: piles = [30,11,23,4,20], h = 6
Output: 23
```

## Constraints

- `1 <= piles.length <= 10^4`
- `piles.length <= h <= 10^9`
- `1 <= piles[i] <= 10^9`

## Approach

### Key Insight:
- This is a binary search on the answer
- If Koko can finish with speed k, she can also finish with any speed > k
- If Koko cannot finish with speed k, she cannot finish with any speed < k
- This monotonic property enables binary search

### Search Space:
- Minimum: 1 (eat at least 1 banana per hour)
- Maximum: max(piles) (can finish any pile in 1 hour)

### Binary Search:
- For a given speed k, calculate total hours needed
- If hours ≤ h, try smaller k (search left)
- If hours > h, need larger k (search right)

## Solution

```python
from typing import List
import math

class Solution:
    def minEatingSpeed(self, piles: List[int], h: int) -> int:
        def hours_needed(speed: int) -> int:
            """Calculate total hours to eat all piles at given speed."""
            return sum(math.ceil(pile / speed) for pile in piles)
        
        # Binary search on speed k
        left, right = 1, max(piles)
        
        while left < right:
            mid = (left + right) // 2
            
            if hours_needed(mid) <= h:
                # Can finish in time, try smaller speed
                right = mid
            else:
                # Cannot finish, need faster speed
                left = mid + 1
        
        return left
```

## Solution Without math.ceil

```python
from typing import List

class Solution:
    def minEatingSpeed(self, piles: List[int], h: int) -> int:
        def hours_needed(speed: int) -> int:
            # ceil(a/b) = (a + b - 1) // b for positive integers
            return sum((pile + speed - 1) // speed for pile in piles)
        
        left, right = 1, max(piles)
        
        while left < right:
            mid = (left + right) // 2
            
            if hours_needed(mid) <= h:
                right = mid
            else:
                left = mid + 1
        
        return left
```

## Solution with Early Termination

```python
from typing import List

class Solution:
    def minEatingSpeed(self, piles: List[int], h: int) -> int:
        def can_finish(speed: int) -> bool:
            """Check if Koko can finish all bananas at given speed within h hours."""
            hours = 0
            for pile in piles:
                hours += (pile + speed - 1) // speed
                if hours > h:  # Early termination
                    return False
            return True
        
        left, right = 1, max(piles)
        
        while left < right:
            mid = (left + right) // 2
            
            if can_finish(mid):
                right = mid
            else:
                left = mid + 1
        
        return left
```

## Detailed Solution with Comments

```python
from typing import List

class Solution:
    def minEatingSpeed(self, piles: List[int], h: int) -> int:
        """
        Binary Search on Answer Pattern
        
        Key observations:
        1. If speed k works, all speeds > k also work (monotonic)
        2. We want the MINIMUM k that works
        3. Binary search finds the boundary
        
        For each pile p:
        - Hours to finish = ceil(p / k)
        - If p = 7, k = 4: ceil(7/4) = 2 hours
        
        Search space:
        - min = 1 (must eat at least 1 banana/hour)
        - max = max(piles) (eating fastest pile in 1 hour is enough)
        """
        
        def calculate_hours(speed):
            """Time to eat all piles at given speed."""
            total = 0
            for pile in piles:
                # ceil division without importing math
                total += (pile + speed - 1) // speed
            return total
        
        # Binary search bounds
        lo = 1
        hi = max(piles)
        
        # Find minimum speed where hours <= h
        while lo < hi:
            mid = lo + (hi - lo) // 2  # Avoid overflow
            
            hours = calculate_hours(mid)
            
            if hours <= h:
                # mid is a valid speed, but maybe we can go slower
                hi = mid
            else:
                # mid is too slow, need to eat faster
                lo = mid + 1
        
        return lo
```

## Complexity Analysis

### Time Complexity: O(n × log(max(piles)))
- Binary search: O(log(max(piles))) iterations
- Each iteration calculates hours: O(n)
- Total: O(n × log(max(piles)))

### Space Complexity: O(1)
- Only using a few variables

## Visual Example

```
piles = [3, 6, 7, 11], h = 8

Binary search: [1, 11]

Step 1: mid = 6
  Hours: ceil(3/6) + ceil(6/6) + ceil(7/6) + ceil(11/6) = 1+1+2+2 = 6 ≤ 8 ✓
  Search left: [1, 6]

Step 2: mid = 3
  Hours: ceil(3/3) + ceil(6/3) + ceil(7/3) + ceil(11/3) = 1+2+3+4 = 10 > 8 ✗
  Search right: [4, 6]

Step 3: mid = 5
  Hours: ceil(3/5) + ceil(6/5) + ceil(7/5) + ceil(11/5) = 1+2+2+3 = 8 ≤ 8 ✓
  Search left: [4, 5]

Step 4: mid = 4
  Hours: ceil(3/4) + ceil(6/4) + ceil(7/4) + ceil(11/4) = 1+2+2+3 = 8 ≤ 8 ✓
  Search left: [4, 4]

Result: 4
```

## Key Patterns & Techniques

1. **Binary Search on Answer**: Search for optimal value in a monotonic function
2. **Ceiling Division**: `ceil(a/b) = (a + b - 1) // b` for positive integers
3. **Search Space Boundaries**: Identify min and max possible answers
4. **Early Termination**: Stop calculating if already exceeded limit

## Template for Binary Search on Answer

```python
def binary_search_answer(condition):
    """
    Find minimum value x such that condition(x) is True.
    Assumes: condition(x) = False for x < answer
             condition(x) = True for x >= answer
    """
    lo, hi = MIN_ANSWER, MAX_ANSWER
    
    while lo < hi:
        mid = lo + (hi - lo) // 2
        
        if condition(mid):
            hi = mid  # mid works, try smaller
        else:
            lo = mid + 1  # mid doesn't work, try larger
    
    return lo
```

## Common Mistakes to Avoid

1. **Wrong search bounds**: min should be 1, not 0 (division by zero)
2. **Using `<=` instead of `<`** in while loop for this variant
3. **Integer overflow**: Use `lo + (hi - lo) // 2` instead of `(lo + hi) // 2`
4. **Wrong ceiling division**: `pile // speed` is floor, not ceiling

## Related Problems

- [875. Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/) (This problem)
- [1011. Capacity To Ship Packages Within D Days](https://leetcode.com/problems/capacity-to-ship-packages-within-d-days/)
- [410. Split Array Largest Sum](https://leetcode.com/problems/split-array-largest-sum/)
- [1283. Find the Smallest Divisor Given a Threshold](https://leetcode.com/problems/find-the-smallest-divisor-given-a-threshold/)

