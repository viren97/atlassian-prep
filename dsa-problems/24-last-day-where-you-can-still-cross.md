# Last Day Where You Can Still Cross

## Problem Information
- **Difficulty**: Hard
- **Frequency**: 43.1%
- **Acceptance Rate**: 62.3%
- **Topics**: Array, Binary Search, Depth-First Search, Breadth-First Search, Union Find, Matrix
- **LeetCode Link**: https://leetcode.com/problems/last-day-where-you-can-still-cross

## Problem Description

There is a **1-indexed** binary matrix where `0` represents land and `1` represents water. You are given integers `row` and `col` representing the number of rows and columns in the matrix, respectively.

Initially on day `0`, the **entire** matrix is **land**. However, each day a new cell becomes flooded with **water**. You are given a **1-indexed** 2D array `cells`, where `cells[i] = [ri, ci]` represents that on the `i-th` day, the cell on the `ri-th` row and `ci-th` column (**1-indexed**) will be covered with **water** (i.e., changed to `1`).

You want to find the **last** day that it is possible to walk from the **top** to the **bottom** by only walking on land cells. You can start from **any** cell in the top row and end at **any** cell in the bottom row. You can only travel in the **four cardinal directions** (left, right, up, and down).

Return the **last** day where it is possible to walk from the **top** to the **bottom** by only walking on land cells.

## Examples

### Example 1:
```
Input: row = 2, col = 2, cells = [[1,1],[2,1],[1,2],[2,2]]
Output: 2
Explanation: 
Day 0: All land
Day 1: (1,1) flooded
Day 2: (1,1), (2,1) flooded - can still cross via (1,2) to (2,2)
Day 3: (1,1), (2,1), (1,2) flooded - cannot cross
```

### Example 2:
```
Input: row = 2, col = 2, cells = [[1,1],[1,2],[2,1],[2,2]]
Output: 1
Explanation: After day 2, the entire top row is flooded, cannot start.
```

### Example 3:
```
Input: row = 3, col = 3, cells = [[1,2],[2,1],[3,3],[2,2],[1,1],[1,3],[2,3],[3,2],[3,1]]
Output: 3
```

## Constraints

- `2 <= row, col <= 2 * 10^4`
- `4 <= row * col <= 2 * 10^4`
- `cells.length == row * col`
- `1 <= ri <= row`
- `1 <= ci <= col`
- All the values of `cells` are **unique**.

## Approach

### Key Insight:
- If we can cross on day d, we can cross on any day < d (fewer flooded cells)
- If we cannot cross on day d, we cannot cross on any day > d
- This monotonic property enables **binary search**

### Approach 1: Binary Search + BFS/DFS
- Binary search on the answer (day)
- For each day, check if path exists using BFS/DFS

### Approach 2: Reverse Union-Find
- Process cells in reverse (add land instead of water)
- Use Union-Find to track connected components
- Find the first day when top connects to bottom

## Solution 1: Binary Search + BFS

```python
from typing import List
from collections import deque

class Solution:
    def latestDayToCross(self, row: int, col: int, cells: List[List[int]]) -> int:
        def can_cross(day):
            """Check if we can cross from top to bottom on given day."""
            # Create grid with first 'day' cells flooded
            grid = [[0] * col for _ in range(row)]
            for i in range(day):
                r, c = cells[i]
                grid[r - 1][c - 1] = 1  # 1-indexed to 0-indexed
            
            # BFS from any land cell in top row
            queue = deque()
            for c in range(col):
                if grid[0][c] == 0:
                    queue.append((0, c))
                    grid[0][c] = 1  # Mark as visited
            
            while queue:
                r, c = queue.popleft()
                
                if r == row - 1:  # Reached bottom row
                    return True
                
                for dr, dc in [(0, 1), (0, -1), (1, 0), (-1, 0)]:
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < row and 0 <= nc < col and grid[nr][nc] == 0:
                        grid[nr][nc] = 1  # Mark as visited
                        queue.append((nr, nc))
            
            return False
        
        # Binary search on the day
        left, right = 1, len(cells)
        
        while left < right:
            mid = (left + right + 1) // 2  # Upper mid for finding last valid
            if can_cross(mid):
                left = mid
            else:
                right = mid - 1
        
        return left
```

## Solution 2: Union-Find (Reverse Process)

```python
from typing import List

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
        if px == py:
            return
        if self.rank[px] < self.rank[py]:
            px, py = py, px
        self.parent[py] = px
        if self.rank[px] == self.rank[py]:
            self.rank[px] += 1
    
    def connected(self, x, y):
        return self.find(x) == self.find(y)

class Solution:
    def latestDayToCross(self, row: int, col: int, cells: List[List[int]]) -> int:
        # Virtual nodes: TOP = row * col, BOTTOM = row * col + 1
        TOP = row * col
        BOTTOM = row * col + 1
        
        uf = UnionFind(row * col + 2)
        
        # Start with all cells flooded
        grid = [[1] * col for _ in range(row)]
        
        # Process cells in reverse order (adding land)
        for day in range(len(cells) - 1, -1, -1):
            r, c = cells[day][0] - 1, cells[day][1] - 1  # Convert to 0-indexed
            grid[r][c] = 0  # Make it land
            
            cell_id = r * col + c
            
            # Connect to virtual TOP if in first row
            if r == 0:
                uf.union(cell_id, TOP)
            
            # Connect to virtual BOTTOM if in last row
            if r == row - 1:
                uf.union(cell_id, BOTTOM)
            
            # Connect to adjacent land cells
            for dr, dc in [(0, 1), (0, -1), (1, 0), (-1, 0)]:
                nr, nc = r + dr, c + dc
                if 0 <= nr < row and 0 <= nc < col and grid[nr][nc] == 0:
                    neighbor_id = nr * col + nc
                    uf.union(cell_id, neighbor_id)
            
            # Check if TOP is connected to BOTTOM
            if uf.connected(TOP, BOTTOM):
                return day  # This is the last day we can cross
        
        return 0
```

## Solution 3: Binary Search + DFS

```python
from typing import List

class Solution:
    def latestDayToCross(self, row: int, col: int, cells: List[List[int]]) -> int:
        def can_cross(day):
            # Create flooded set
            flooded = set((cells[i][0] - 1, cells[i][1] - 1) for i in range(day))
            
            # DFS from top row
            visited = set()
            
            def dfs(r, c):
                if r == row - 1:
                    return True
                
                for dr, dc in [(0, 1), (0, -1), (1, 0), (-1, 0)]:
                    nr, nc = r + dr, c + dc
                    if (0 <= nr < row and 0 <= nc < col and 
                        (nr, nc) not in flooded and (nr, nc) not in visited):
                        visited.add((nr, nc))
                        if dfs(nr, nc):
                            return True
                
                return False
            
            for c in range(col):
                if (0, c) not in flooded and (0, c) not in visited:
                    visited.add((0, c))
                    if dfs(0, c):
                        return True
            
            return False
        
        left, right = 1, len(cells)
        
        while left < right:
            mid = (left + right + 1) // 2
            if can_cross(mid):
                left = mid
            else:
                right = mid - 1
        
        return left
```

## Complexity Analysis

### Binary Search + BFS/DFS:
- **Time**: O(row × col × log(row × col))
  - Binary search: O(log(row × col)) iterations
  - Each BFS/DFS: O(row × col)
- **Space**: O(row × col) for grid and visited

### Union-Find:
- **Time**: O(row × col × α(row × col)) ≈ O(row × col)
  - α is inverse Ackermann function (nearly constant)
- **Space**: O(row × col) for Union-Find structure

## Visual Example

```
row=2, col=2, cells=[[1,1],[2,1],[1,2],[2,2]]

Day 0:        Day 1:        Day 2:        Day 3:
L L           W L           W L           W W
L L           L L           W L           W L

Day 2: Can still cross via (1,2) → (2,2)
Day 3: Cannot cross - no path from top to bottom

Answer: 2
```

## Key Patterns & Techniques

1. **Binary Search on Answer**: When checking condition is monotonic
2. **Reverse Processing with Union-Find**: Convert "remove edges" to "add edges"
3. **Virtual Nodes**: Connect boundary cells to virtual sources/sinks
4. **Grid to Graph**: Cell index = row × col + col_index

## Binary Search Variant: Finding Last Valid

```python
# We want LAST day where can_cross is True
# After answer, can_cross becomes False

# Use upper mid: (left + right + 1) // 2
while left < right:
    mid = (left + right + 1) // 2
    if can_cross(mid):
        left = mid      # mid is valid, try higher
    else:
        right = mid - 1  # mid invalid, try lower

return left
```

## Common Mistakes to Avoid

1. **1-indexed vs 0-indexed**: cells are 1-indexed!
2. **Wrong binary search variant**: Need to find LAST valid day
3. **Forgetting to mark visited**: Can lead to infinite loops
4. **Wrong virtual node connections**: TOP connects to first row, BOTTOM to last row

## Edge Cases

1. Minimum grid size
2. Entire row flooded on first day
3. Path blocked by single cell
4. Diagonal path needed (not allowed - 4-directional only)

## Related Problems

- [200. Number of Islands](https://leetcode.com/problems/number-of-islands/)
- [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/)
- [1631. Path With Minimum Effort](https://leetcode.com/problems/path-with-minimum-effort/)
- [778. Swim in Rising Water](https://leetcode.com/problems/swim-in-rising-water/)

