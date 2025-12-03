# Can Place Flowers

## Problem Information
- **Difficulty**: Easy
- **Frequency**: 56.2%
- **Acceptance Rate**: 28.9%
- **Topics**: Array, Greedy
- **LeetCode Link**: https://leetcode.com/problems/can-place-flowers

## Problem Description

You have a long flowerbed in which some of the plots are planted, and some are not. However, flowers cannot be planted in **adjacent** plots.

Given an integer array `flowerbed` containing `0`'s and `1`'s, where `0` means empty and `1` means not empty, and an integer `n`, return `true` if `n` new flowers can be planted in the `flowerbed` without violating the no-adjacent-flowers rule, and `false` otherwise.

## Examples

### Example 1:
```
Input: flowerbed = [1,0,0,0,1], n = 1
Output: true
Explanation: We can plant at index 2.
```

### Example 2:
```
Input: flowerbed = [1,0,0,0,1], n = 2
Output: false
Explanation: We can only plant 1 flower (at index 2).
```

### Example 3:
```
Input: flowerbed = [0,0,1,0,0], n = 2
Output: true
Explanation: We can plant at indices 0 and 4.
```

## Constraints

- `1 <= flowerbed.length <= 2 * 10^4`
- `flowerbed[i]` is `0` or `1`.
- There are no two adjacent flowers in `flowerbed`.
- `0 <= n <= flowerbed.length`

## Approach

### Greedy Strategy:
- Scan from left to right
- Plant a flower whenever possible (greedy)
- A position is plantable if:
  1. Current position is empty (0)
  2. Left neighbor is empty or out of bounds
  3. Right neighbor is empty or out of bounds

### Why Greedy Works:
- Planting early doesn't block more flowers than necessary
- If we skip a valid spot, we don't gain any advantage

## Solution

```python
from typing import List

class Solution:
    def canPlaceFlowers(self, flowerbed: List[int], n: int) -> bool:
        count = 0
        length = len(flowerbed)
        
        for i in range(length):
            # Check if we can plant at position i
            if flowerbed[i] == 0:
                # Check left neighbor (or treat as empty if out of bounds)
                left_empty = (i == 0) or (flowerbed[i - 1] == 0)
                # Check right neighbor (or treat as empty if out of bounds)
                right_empty = (i == length - 1) or (flowerbed[i + 1] == 0)
                
                if left_empty and right_empty:
                    # Plant the flower
                    flowerbed[i] = 1
                    count += 1
                    
                    # Early termination
                    if count >= n:
                        return True
        
        return count >= n
```

## Solution with Padding (Cleaner Logic)

```python
from typing import List

class Solution:
    def canPlaceFlowers(self, flowerbed: List[int], n: int) -> bool:
        # Add virtual empty plots at both ends
        flowerbed = [0] + flowerbed + [0]
        count = 0
        
        for i in range(1, len(flowerbed) - 1):
            if flowerbed[i - 1] == 0 and flowerbed[i] == 0 and flowerbed[i + 1] == 0:
                flowerbed[i] = 1
                count += 1
        
        return count >= n
```

## Solution Without Modifying Input

```python
from typing import List

class Solution:
    def canPlaceFlowers(self, flowerbed: List[int], n: int) -> bool:
        count = 0
        length = len(flowerbed)
        i = 0
        
        while i < length:
            if flowerbed[i] == 0:
                left_empty = (i == 0) or (flowerbed[i - 1] == 0)
                right_empty = (i == length - 1) or (flowerbed[i + 1] == 0)
                
                if left_empty and right_empty:
                    count += 1
                    i += 2  # Skip next position (can't plant adjacent)
                    continue
            
            i += 1
        
        return count >= n
```

## Solution Using Consecutive Zeros

```python
from typing import List

class Solution:
    def canPlaceFlowers(self, flowerbed: List[int], n: int) -> bool:
        """
        Count consecutive zeros and calculate how many flowers can be planted.
        For k consecutive zeros between flowers: can plant (k-1)//2 flowers
        For k consecutive zeros at the start: can plant k//2 flowers
        For k consecutive zeros at the end: can plant k//2 flowers
        """
        # Pad with zeros to simplify edge cases
        flowerbed = [0] + flowerbed + [0]
        
        count = 0
        zeros = 0
        
        for plot in flowerbed:
            if plot == 0:
                zeros += 1
            else:
                # Calculate flowers that can be planted in this zero-run
                # For zeros between two 1s: (zeros - 1) // 2
                if zeros > 0:
                    count += (zeros - 1) // 2
                zeros = 0
        
        # Handle trailing zeros
        if zeros > 0:
            count += (zeros - 1) // 2
        
        return count >= n
```

## Complexity Analysis

### Time Complexity: O(n)
- Single pass through the array
- Each element visited at most once

### Space Complexity: O(1)
- Only using a few variables
- (O(n) for the padding solution due to array copy)

## Visual Example

```
Flowerbed: [1, 0, 0, 0, 1], n = 1

Index:      0  1  2  3  4
Value:      1  0  0  0  1

Scan:
- i=0: value=1, skip (already planted)
- i=1: value=0, left=1 (not empty), can't plant
- i=2: value=0, left=0, right=0, PLANT! → [1, 0, 1, 0, 1]
- i=3: value=0, left=1 (not empty), can't plant
- i=4: value=1, skip

Planted: 1 flower ✓
```

## Key Patterns & Techniques

1. **Greedy Algorithm**: Plant whenever valid, don't look back
2. **Boundary Handling**: Treat out-of-bounds as empty
3. **Early Termination**: Return as soon as we've planted enough
4. **Padding Technique**: Add sentinel values to simplify edge cases

## Edge Cases

1. `n = 0`: Always return true (no flowers to plant)
2. Single element `[0]`: Can plant 1 flower
3. Single element `[1]`: Can plant 0 flowers
4. All zeros: Can plant `(length + 1) // 2` flowers
5. All ones: Can plant 0 flowers

## Formula for Consecutive Zeros

```
For k consecutive zeros:
- Between two flowers: can plant (k - 1) // 2
- At the start (before first flower): can plant k // 2
- At the end (after last flower): can plant k // 2
- Entire array is zeros: can plant (k + 1) // 2

Why? 
- Between flowers: need 1 empty on each side, so k-2 usable, plant every other
- At edges: only need 1 empty on one side
```

## Common Mistakes to Avoid

1. Not handling edge cases (first/last position)
2. Forgetting to mark planted positions
3. Not using early termination (wastes time)
4. Modifying input when shouldn't (check problem constraints)

## Related Problems

- [495. Teemo Attacking](https://leetcode.com/problems/teemo-attacking/)
- [1647. Minimum Deletions to Make Character Frequencies Unique](https://leetcode.com/problems/minimum-deletions-to-make-character-frequencies-unique/)

