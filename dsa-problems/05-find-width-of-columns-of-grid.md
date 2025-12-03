# Find the Width of Columns of a Grid

## Problem Information
- **Difficulty**: Easy
- **Frequency**: 73.6%
- **Acceptance Rate**: 69.3%
- **Topics**: Array, Matrix
- **LeetCode Link**: https://leetcode.com/problems/find-the-width-of-columns-of-a-grid

## Problem Description

You are given a **0-indexed** `m x n` integer matrix `grid`. The width of a column is the maximum length of its integers.

- For example, if `grid = [[-10], [3], [12]]`, the width of the only column is `3` since `-10` is of length `3`.

Return an integer array `ans` of size `n` where `ans[i]` is the width of the `i-th` column.

The length of an integer `x` with `len` digits is equal to `len` if `x` is non-negative, and `len + 1` if `x` is negative (to account for the negative sign).

## Examples

### Example 1:
```
Input: grid = [[1],[22],[333]]
Output: [3]
Explanation: In the 0th column, 333 is of length 3.
```

### Example 2:
```
Input: grid = [[-1,1,4],[-9999,999999,9999]]
Output: [5,6,4]
Explanation: 
In the 0th column, max length is 5 (for -9999, which has length 5 including the negative sign)
In the 1st column, max length is 6 (for 999999, which has length 6)
In the 2nd column, max length is 4 (for 9999, which has length 4)
```

### Example 3:
```
Input: grid = [[1]]
Output: [1]
Explanation: The only column has width 1.
```

## Constraints

- `m == grid.length`
- `n == grid[i].length`
- `1 <= m, n <= 100`
- `-10^9 <= grid[i][j] <= 10^9`

## Approach

### Key Insights:
1. For each column, find the maximum "width" of all numbers in that column
2. The width of a number is the length of its string representation (including negative sign)
3. We can use `len(str(num))` to get the width

### Algorithm:
1. Initialize result array with size = number of columns
2. For each column, iterate through all rows
3. Calculate the width of each number and keep track of maximum
4. Return the result array

## Solution

```python
from typing import List

class Solution:
    def findColumnWidth(self, grid: List[List[int]]) -> List[int]:
        m, n = len(grid), len(grid[0])
        result = []
        
        for col in range(n):
            max_width = 0
            for row in range(m):
                # len(str(num)) handles negative sign automatically
                width = len(str(grid[row][col]))
                max_width = max(max_width, width)
            result.append(max_width)
        
        return result
```

## One-liner Solution

```python
from typing import List

class Solution:
    def findColumnWidth(self, grid: List[List[int]]) -> List[int]:
        return [max(len(str(grid[row][col])) for row in range(len(grid))) 
                for col in range(len(grid[0]))]
```

## Alternative Solution (Using zip for column iteration)

```python
from typing import List

class Solution:
    def findColumnWidth(self, grid: List[List[int]]) -> List[int]:
        # zip(*grid) transposes the matrix, giving us columns as rows
        return [max(len(str(num)) for num in col) for col in zip(*grid)]
```

## Solution Without Using str() - Mathematical Approach

```python
from typing import List
import math

class Solution:
    def findColumnWidth(self, grid: List[List[int]]) -> List[int]:
        def get_width(num):
            if num == 0:
                return 1
            
            is_negative = num < 0
            num = abs(num)
            
            # Number of digits = floor(log10(num)) + 1
            digits = int(math.log10(num)) + 1
            
            return digits + (1 if is_negative else 0)
        
        m, n = len(grid), len(grid[0])
        result = []
        
        for col in range(n):
            max_width = 0
            for row in range(m):
                max_width = max(max_width, get_width(grid[row][col]))
            result.append(max_width)
        
        return result
```

## Complexity Analysis

### Time Complexity: O(m × n × d)
- **m** = number of rows
- **n** = number of columns
- **d** = average number of digits per number (for string conversion)
- We visit each cell once
- Converting a number to string is O(d) where d is the number of digits

### Space Complexity: O(n)
- Result array of size n
- String conversion creates temporary strings (O(d) each, but not stored)

## Key Patterns & Techniques

1. **Column-wise Iteration**: Iterate through columns as the outer loop
2. **String Conversion**: Simplest way to get the "display width" of a number
3. **Matrix Transpose with zip**: `zip(*grid)` is a Pythonic way to iterate columns

## Visual Example

```
Grid:
    Col 0    Col 1    Col 2
   +-------+--------+------+
   |  -1   |   1    |   4  |
   +-------+--------+------+
   | -9999 | 999999 | 9999 |
   +-------+--------+------+

Width calculation:
- Col 0: max(len("-1"), len("-9999")) = max(2, 5) = 5
- Col 1: max(len("1"), len("999999")) = max(1, 6) = 6
- Col 2: max(len("4"), len("9999")) = max(1, 4) = 4

Result: [5, 6, 4]
```

## Common Mistakes to Avoid

1. Forgetting to account for the negative sign
2. Using column index as row index (common indexing mistake)
3. Not handling edge cases like 0 (which has width 1)

## Related Problems

- [867. Transpose Matrix](https://leetcode.com/problems/transpose-matrix/)
- [2639. Find the Width of Columns of a Grid](https://leetcode.com/problems/find-the-width-of-columns-of-a-grid/) (This problem)
- [498. Diagonal Traverse](https://leetcode.com/problems/diagonal-traverse/)

