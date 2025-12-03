# Russian Doll Envelopes

## Problem Information
- **Difficulty**: Hard
- **Frequency**: 56.2%
- **Acceptance Rate**: 37.3%
- **Topics**: Array, Binary Search, Dynamic Programming, Sorting
- **LeetCode Link**: https://leetcode.com/problems/russian-doll-envelopes

## Problem Description

You are given a 2D array of integers `envelopes` where `envelopes[i] = [wi, hi]` represents the width and the height of an envelope.

One envelope can fit into another if and only if both the width and height of one envelope are greater than the other envelope's width and height.

Return the maximum number of envelopes you can Russian doll (i.e., put one inside the other).

**Note:** You cannot rotate an envelope.

## Examples

### Example 1:
```
Input: envelopes = [[5,4],[6,4],[6,7],[2,3]]
Output: 3
Explanation: The maximum number of envelopes you can Russian doll is 3 ([2,3] => [5,4] => [6,7]).
```

### Example 2:
```
Input: envelopes = [[1,1],[1,1],[1,1]]
Output: 1
Explanation: All envelopes are the same size, so only 1 can be selected.
```

## Constraints

- `1 <= envelopes.length <= 10^5`
- `envelopes[i].length == 2`
- `1 <= wi, hi <= 10^5`

## Approach

### Key Insight:
This is a 2D variant of Longest Increasing Subsequence (LIS).

### Strategy:
1. **Sort envelopes by width ascending**, then by **height descending** (for same width)
2. Find LIS on heights

### Why sort height descending for same width?
- For envelopes with same width, we can only pick one
- Sorting height descending prevents selecting multiple envelopes with same width
- Example: `[3,3], [3,4]` → sorted as `[3,4], [3,3]`
- LIS on heights [4,3] won't select both (4 > 3, not increasing)

## Solution 1: DP with Binary Search (Optimal)

```python
from typing import List
import bisect

class Solution:
    def maxEnvelopes(self, envelopes: List[List[int]]) -> int:
        # Sort by width ascending, then height descending
        envelopes.sort(key=lambda x: (x[0], -x[1]))
        
        # Find LIS on heights using binary search
        # dp[i] = smallest height that ends an increasing subsequence of length i+1
        dp = []
        
        for _, height in envelopes:
            # Find position to insert/replace
            pos = bisect.bisect_left(dp, height)
            
            if pos == len(dp):
                dp.append(height)
            else:
                dp[pos] = height
        
        return len(dp)
```

## Solution 2: Standard DP (O(n²) - TLE for large inputs)

```python
from typing import List

class Solution:
    def maxEnvelopes(self, envelopes: List[List[int]]) -> int:
        if not envelopes:
            return 0
        
        # Sort by width, then by height
        envelopes.sort(key=lambda x: (x[0], x[1]))
        
        n = len(envelopes)
        dp = [1] * n  # dp[i] = max envelopes ending at i
        
        for i in range(1, n):
            for j in range(i):
                # Check if envelope j can fit inside envelope i
                if envelopes[j][0] < envelopes[i][0] and envelopes[j][1] < envelopes[i][1]:
                    dp[i] = max(dp[i], dp[j] + 1)
        
        return max(dp)
```

## Solution with Detailed LIS Implementation

```python
from typing import List

class Solution:
    def maxEnvelopes(self, envelopes: List[List[int]]) -> int:
        """
        LIS using patience sorting / binary search
        
        Key insight: After sorting by (width ASC, height DESC),
        we only need to find LIS on heights.
        
        Why height DESC for same width?
        - [3,3] and [3,4] cannot nest (same width)
        - Sorted as [3,4], [3,3] → heights [4,3]
        - LIS of [4,3] is 1 (can't include both)
        """
        if not envelopes:
            return 0
        
        # Sort: width ascending, height descending (for same width)
        envelopes.sort(key=lambda x: (x[0], -x[1]))
        
        # Extract heights
        heights = [h for _, h in envelopes]
        
        # Find LIS length using binary search
        return self.lengthOfLIS(heights)
    
    def lengthOfLIS(self, nums: List[int]) -> int:
        """
        Find length of longest strictly increasing subsequence.
        Uses patience sorting with binary search.
        
        tails[i] = smallest ending element of all increasing subsequences
                   of length i+1
        """
        tails = []
        
        for num in nums:
            # Binary search for the position
            left, right = 0, len(tails)
            
            while left < right:
                mid = (left + right) // 2
                if tails[mid] < num:
                    left = mid + 1
                else:
                    right = mid
            
            # left is the insertion position
            if left == len(tails):
                tails.append(num)
            else:
                tails[left] = num
        
        return len(tails)
```

## Understanding the Sort Order

```
Example: [[5,4], [6,4], [6,7], [2,3]]

Step 1: Sort by (width ASC, height DESC)
- [2,3]
- [5,4]
- [6,7], [6,4]  ← same width 6, sort by height DESC

Sorted: [[2,3], [5,4], [6,7], [6,4]]
Heights: [3, 4, 7, 4]

Step 2: Find LIS on heights [3, 4, 7, 4]

Patience sorting:
- 3: tails = [3]
- 4: 4 > 3, append → tails = [3, 4]
- 7: 7 > 4, append → tails = [3, 4, 7]
- 4: replace position of 7 → tails = [3, 4, 4]

LIS length = 3

Actual sequence: [2,3] → [5,4] → [6,7]
```

## Why Descending Height Matters

```
Without descending height sort:
Envelopes: [[6,4], [6,7]]
Sorted (width ASC, height ASC): [[6,4], [6,7]]
Heights: [4, 7]
LIS: [4, 7] → length 2 ❌ WRONG!

[6,4] cannot fit inside [6,7] (same width!)

With descending height sort:
Sorted (width ASC, height DESC): [[6,7], [6,4]]
Heights: [7, 4]
LIS: [4] or [7] → length 1 ✓ CORRECT!
```

## Complexity Analysis

### Binary Search Solution:
- **Time**: O(n log n)
  - Sorting: O(n log n)
  - LIS with binary search: O(n log n)
- **Space**: O(n) for the tails array

### DP Solution:
- **Time**: O(n²) - Too slow for constraints
- **Space**: O(n)

## Key Patterns & Techniques

1. **2D to 1D Reduction**: Sort one dimension, find LIS on the other
2. **Descending Tie-breaker**: Prevents selecting multiple items with same primary key
3. **Patience Sorting**: Optimal algorithm for LIS length
4. **Binary Search for LIS**: O(n log n) vs O(n²)

## LIS with Binary Search Visualization

```
Heights: [3, 4, 7, 4]
tails array evolution:

Process 3: tails = [3]
           └─ Start new pile

Process 4: tails = [3, 4]
           └─ 4 > 3, extend

Process 7: tails = [3, 4, 7]
           └─ 7 > 4, extend

Process 4: tails = [3, 4, 4]
           └─ Replace 7 with 4 (position found by binary search)
           └─ This represents: we found another subsequence of length 3
              that ends with a smaller value (4 instead of 7)

Length of LIS = len(tails) = 3
```

## Common Mistakes to Avoid

1. **Forgetting descending sort for same width**: Critical for correctness
2. **Using ≤ instead of < for binary search**: Must be strictly increasing
3. **Not handling empty input**
4. **Using O(n²) DP**: Will TLE for n = 10^5

## Related Problems

- [300. Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/)
- [673. Number of Longest Increasing Subsequence](https://leetcode.com/problems/number-of-longest-increasing-subsequence/)
- [1691. Maximum Height by Stacking Cuboids](https://leetcode.com/problems/maximum-height-by-stacking-cuboids/)
- [1048. Longest String Chain](https://leetcode.com/problems/longest-string-chain/)

