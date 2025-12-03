# Word Search

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 45.3%
- **Topics**: Array, String, Backtracking, Depth-First Search, Matrix
- **LeetCode Link**: https://leetcode.com/problems/word-search

## Problem Description

Given an `m x n` grid of characters `board` and a string `word`, return `true` if `word` exists in the grid.

The word can be constructed from letters of sequentially adjacent cells, where adjacent cells are horizontally or vertically neighboring. The same letter cell may not be used more than once.

## Examples

### Example 1:
```
Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word = "ABCCED"
Output: true

Grid:
A B C E
S F C S
A D E E

Path: A(0,0) → B(0,1) → C(0,2) → C(1,2) → E(2,2) → D(2,1)
```

### Example 2:
```
Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word = "SEE"
Output: true

Path: S(1,3) → E(2,3) → E(2,2)
```

### Example 3:
```
Input: board = [["A","B","C","E"],["S","F","C","S"],["A","D","E","E"]], word = "ABCB"
Output: false

Cannot reuse B at (0,1)
```

## Constraints

- `m == board.length`
- `n = board[i].length`
- `1 <= m, n <= 6`
- `1 <= word.length <= 15`
- `board` and `word` consists of only lowercase and uppercase English letters.

## Approach

### Key Insight:
- Use DFS/Backtracking from each cell
- Mark visited cells to prevent reuse
- Restore cells after backtracking

### Algorithm:
1. Try starting from each cell in the grid
2. DFS to explore all 4 directions
3. Mark cell as visited, recurse, then unmark (backtrack)
4. Return true if any path matches the word

## Solution

```python
from typing import List

class Solution:
    def exist(self, board: List[List[str]], word: str) -> bool:
        m, n = len(board), len(board[0])
        
        def dfs(row, col, index):
            # Found complete word
            if index == len(word):
                return True
            
            # Out of bounds or wrong character
            if (row < 0 or row >= m or 
                col < 0 or col >= n or 
                board[row][col] != word[index]):
                return False
            
            # Mark as visited
            temp = board[row][col]
            board[row][col] = '#'
            
            # Explore all 4 directions
            found = (dfs(row + 1, col, index + 1) or
                     dfs(row - 1, col, index + 1) or
                     dfs(row, col + 1, index + 1) or
                     dfs(row, col - 1, index + 1))
            
            # Backtrack: restore the cell
            board[row][col] = temp
            
            return found
        
        # Try starting from each cell
        for i in range(m):
            for j in range(n):
                if dfs(i, j, 0):
                    return True
        
        return False
```

## Solution with Separate Visited Set

```python
from typing import List

class Solution:
    def exist(self, board: List[List[str]], word: str) -> bool:
        m, n = len(board), len(board[0])
        visited = set()
        
        def dfs(row, col, index):
            if index == len(word):
                return True
            
            if (row < 0 or row >= m or 
                col < 0 or col >= n or 
                (row, col) in visited or
                board[row][col] != word[index]):
                return False
            
            visited.add((row, col))
            
            # Explore neighbors
            directions = [(0, 1), (0, -1), (1, 0), (-1, 0)]
            for dr, dc in directions:
                if dfs(row + dr, col + dc, index + 1):
                    return True
            
            visited.remove((row, col))
            return False
        
        for i in range(m):
            for j in range(n):
                if dfs(i, j, 0):
                    return True
        
        return False
```

## Optimized Solution with Pruning

```python
from typing import List
from collections import Counter

class Solution:
    def exist(self, board: List[List[str]], word: str) -> bool:
        m, n = len(board), len(board[0])
        
        # Optimization: Check if board has enough characters
        board_count = Counter(char for row in board for char in row)
        word_count = Counter(word)
        
        for char, count in word_count.items():
            if board_count[char] < count:
                return False
        
        # Optimization: Start from rarer end
        if board_count[word[0]] > board_count[word[-1]]:
            word = word[::-1]
        
        def dfs(row, col, index):
            if index == len(word):
                return True
            
            if (row < 0 or row >= m or 
                col < 0 or col >= n or 
                board[row][col] != word[index]):
                return False
            
            temp = board[row][col]
            board[row][col] = '#'
            
            found = (dfs(row + 1, col, index + 1) or
                     dfs(row - 1, col, index + 1) or
                     dfs(row, col + 1, index + 1) or
                     dfs(row, col - 1, index + 1))
            
            board[row][col] = temp
            return found
        
        for i in range(m):
            for j in range(n):
                if dfs(i, j, 0):
                    return True
        
        return False
```

## Complexity Analysis

### Time Complexity: O(m × n × 4^L)
- m × n starting positions
- At each position, explore up to 4 directions
- Maximum depth L (word length)
- With pruning, often much better in practice

### Space Complexity: O(L)
- Recursion stack depth = word length
- O(m × n) if using separate visited set

## Visual Example

```
Board:        Word: "ABCCED"
A B C E
S F C S
A D E E

DFS from (0,0) 'A':
(0,0)A → (0,1)B → (0,2)C → ?

From (0,2)C:
- Up: out of bounds
- Down: (1,2)C ✓ → continue
- Left: (0,1)B, already visited
- Right: (0,3)E, but we need 'C'

(0,0)A → (0,1)B → (0,2)C → (1,2)C → ?

From (1,2)C:
- Down: (2,2)E ✓ → continue

(0,0)A → (0,1)B → (0,2)C → (1,2)C → (2,2)E → ?

From (2,2)E:
- Left: (2,1)D ✓ → continue
- index = 6 = len(word) → FOUND!
```

## Key Patterns & Techniques

1. **DFS + Backtracking**: Explore paths and undo choices
2. **In-place Marking**: Use '#' to mark visited (modify board temporarily)
3. **4-Direction Traversal**: Standard grid pattern
4. **Early Termination**: Return immediately when found

## Backtracking Template

```python
def backtrack(state):
    if is_solution(state):
        return True  # or record solution
    
    for choice in get_choices(state):
        if is_valid(choice):
            make_choice(choice)
            if backtrack(new_state):
                return True
            undo_choice(choice)
    
    return False
```

## Common Mistakes to Avoid

1. **Forgetting to backtrack**: Must restore the cell after DFS
2. **Wrong base case**: Check `index == len(word)` BEFORE bounds check
3. **Missing direction**: Check all 4 directions
4. **Reusing cells**: Must mark visited cells

## Optimizations

1. **Character frequency check**: Early reject if impossible
2. **Start from rarer character**: Reduces branching
3. **Reverse word if first char is common**: Better pruning
4. **Check first character match**: Only start DFS if board[i][j] == word[0]

## Edge Cases

1. Single character word
2. Word longer than grid size
3. Word with repeated characters
4. Character not in grid

## Related Problems

- [212. Word Search II](https://leetcode.com/problems/word-search-ii/)
- [980. Unique Paths III](https://leetcode.com/problems/unique-paths-iii/)
- [37. Sudoku Solver](https://leetcode.com/problems/sudoku-solver/)
- [51. N-Queens](https://leetcode.com/problems/n-queens/)

