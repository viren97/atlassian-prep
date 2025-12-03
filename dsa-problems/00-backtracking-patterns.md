# Backtracking Patterns - Complete Guide

## Backtracking Framework

```
def backtrack(state):
    if is_solution(state):
        add_to_result(state)
        return
    
    for choice in get_choices(state):
        if is_valid(choice, state):
            make_choice(choice, state)      # Choose
            backtrack(state)                 # Explore
            undo_choice(choice, state)       # Unchoose (Backtrack)
```

---

## Pattern 1: Subsets

### All Subsets (Power Set)

```python
def subsets(nums):
    result = []
    
    def backtrack(start, current):
        result.append(current[:])  # Add copy of current subset
        
        for i in range(start, len(nums)):
            current.append(nums[i])      # Choose
            backtrack(i + 1, current)    # Explore
            current.pop()                # Unchoose
    
    backtrack(0, [])
    return result

# Example: nums = [1, 2, 3]
# Output: [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]]
```

### Subsets with Duplicates

```python
def subsetsWithDup(nums):
    result = []
    nums.sort()  # Sort to handle duplicates
    
    def backtrack(start, current):
        result.append(current[:])
        
        for i in range(start, len(nums)):
            # Skip duplicates
            if i > start and nums[i] == nums[i - 1]:
                continue
            
            current.append(nums[i])
            backtrack(i + 1, current)
            current.pop()
    
    backtrack(0, [])
    return result
```

### Iterative Approach (Cascading)

```python
def subsets_iterative(nums):
    result = [[]]
    
    for num in nums:
        result += [subset + [num] for subset in result]
    
    return result
```

---

## Pattern 2: Permutations

### All Permutations

```python
def permute(nums):
    result = []
    
    def backtrack(current, remaining):
        if not remaining:
            result.append(current[:])
            return
        
        for i in range(len(remaining)):
            current.append(remaining[i])
            backtrack(current, remaining[:i] + remaining[i+1:])
            current.pop()
    
    backtrack([], nums)
    return result


# Alternative: Swap-based approach
def permute_swap(nums):
    result = []
    
    def backtrack(start):
        if start == len(nums):
            result.append(nums[:])
            return
        
        for i in range(start, len(nums)):
            nums[start], nums[i] = nums[i], nums[start]  # Swap
            backtrack(start + 1)
            nums[start], nums[i] = nums[i], nums[start]  # Undo swap
    
    backtrack(0)
    return result
```

### Permutations with Duplicates

```python
def permuteUnique(nums):
    result = []
    nums.sort()
    used = [False] * len(nums)
    
    def backtrack(current):
        if len(current) == len(nums):
            result.append(current[:])
            return
        
        for i in range(len(nums)):
            if used[i]:
                continue
            # Skip duplicates: only use if previous duplicate is used
            if i > 0 and nums[i] == nums[i - 1] and not used[i - 1]:
                continue
            
            used[i] = True
            current.append(nums[i])
            backtrack(current)
            current.pop()
            used[i] = False
    
    backtrack([])
    return result
```

---

## Pattern 3: Combinations

### Choose k from n

```python
def combine(n, k):
    result = []
    
    def backtrack(start, current):
        if len(current) == k:
            result.append(current[:])
            return
        
        # Pruning: need at least (k - len(current)) more elements
        for i in range(start, n - (k - len(current)) + 2):
            current.append(i)
            backtrack(i + 1, current)
            current.pop()
    
    backtrack(1, [])
    return result
```

### Combination Sum (Reuse Elements)

```python
def combinationSum(candidates, target):
    result = []
    
    def backtrack(start, current, remaining):
        if remaining == 0:
            result.append(current[:])
            return
        if remaining < 0:
            return
        
        for i in range(start, len(candidates)):
            current.append(candidates[i])
            # Can reuse same element, so pass i (not i+1)
            backtrack(i, current, remaining - candidates[i])
            current.pop()
    
    backtrack(0, [], target)
    return result
```

### Combination Sum II (No Reuse, Has Duplicates)

```python
def combinationSum2(candidates, target):
    result = []
    candidates.sort()
    
    def backtrack(start, current, remaining):
        if remaining == 0:
            result.append(current[:])
            return
        if remaining < 0:
            return
        
        for i in range(start, len(candidates)):
            # Skip duplicates
            if i > start and candidates[i] == candidates[i - 1]:
                continue
            
            current.append(candidates[i])
            backtrack(i + 1, current, remaining - candidates[i])
            current.pop()
    
    backtrack(0, [], target)
    return result
```

### Combination Sum III (k numbers from 1-9)

```python
def combinationSum3(k, n):
    result = []
    
    def backtrack(start, current, remaining):
        if len(current) == k:
            if remaining == 0:
                result.append(current[:])
            return
        
        for i in range(start, 10):
            if i > remaining:  # Pruning
                break
            current.append(i)
            backtrack(i + 1, current, remaining - i)
            current.pop()
    
    backtrack(1, [], n)
    return result
```

---

## Pattern 4: Partition Problems

### Palindrome Partitioning

```python
def partition(s):
    result = []
    
    def is_palindrome(start, end):
        while start < end:
            if s[start] != s[end]:
                return False
            start += 1
            end -= 1
        return True
    
    def backtrack(start, current):
        if start == len(s):
            result.append(current[:])
            return
        
        for end in range(start, len(s)):
            if is_palindrome(start, end):
                current.append(s[start:end + 1])
                backtrack(end + 1, current)
                current.pop()
    
    backtrack(0, [])
    return result
```

### Partition to K Equal Sum Subsets

```python
def canPartitionKSubsets(nums, k):
    total = sum(nums)
    if total % k != 0:
        return False
    
    target = total // k
    nums.sort(reverse=True)  # Start with largest for early pruning
    used = [False] * len(nums)
    
    def backtrack(index, count, current_sum):
        if count == k:
            return True
        
        if current_sum == target:
            # Found one subset, look for remaining
            return backtrack(0, count + 1, 0)
        
        for i in range(index, len(nums)):
            if used[i]:
                continue
            if current_sum + nums[i] > target:
                continue
            # Skip duplicates
            if i > 0 and nums[i] == nums[i - 1] and not used[i - 1]:
                continue
            
            used[i] = True
            if backtrack(i + 1, count, current_sum + nums[i]):
                return True
            used[i] = False
        
        return False
    
    return backtrack(0, 0, 0)
```

---

## Pattern 5: N-Queens

```python
def solveNQueens(n):
    result = []
    
    # Track attacked columns and diagonals
    cols = set()
    diag1 = set()  # row - col
    diag2 = set()  # row + col
    
    board = [['.'] * n for _ in range(n)]
    
    def backtrack(row):
        if row == n:
            result.append([''.join(r) for r in board])
            return
        
        for col in range(n):
            if col in cols or (row - col) in diag1 or (row + col) in diag2:
                continue
            
            # Place queen
            board[row][col] = 'Q'
            cols.add(col)
            diag1.add(row - col)
            diag2.add(row + col)
            
            backtrack(row + 1)
            
            # Remove queen
            board[row][col] = '.'
            cols.remove(col)
            diag1.remove(row - col)
            diag2.remove(row + col)
    
    backtrack(0)
    return result
```

---

## Pattern 6: Sudoku Solver

```python
def solveSudoku(board):
    rows = [set() for _ in range(9)]
    cols = [set() for _ in range(9)]
    boxes = [set() for _ in range(9)]
    empty = []
    
    # Initialize sets and find empty cells
    for i in range(9):
        for j in range(9):
            if board[i][j] == '.':
                empty.append((i, j))
            else:
                num = board[i][j]
                rows[i].add(num)
                cols[j].add(num)
                boxes[(i // 3) * 3 + j // 3].add(num)
    
    def backtrack(idx):
        if idx == len(empty):
            return True
        
        i, j = empty[idx]
        box_idx = (i // 3) * 3 + j // 3
        
        for num in '123456789':
            if num in rows[i] or num in cols[j] or num in boxes[box_idx]:
                continue
            
            # Place number
            board[i][j] = num
            rows[i].add(num)
            cols[j].add(num)
            boxes[box_idx].add(num)
            
            if backtrack(idx + 1):
                return True
            
            # Remove number
            board[i][j] = '.'
            rows[i].remove(num)
            cols[j].remove(num)
            boxes[box_idx].remove(num)
        
        return False
    
    backtrack(0)
```

---

## Pattern 7: Word Search

```python
def exist(board, word):
    rows, cols = len(board), len(board[0])
    
    def backtrack(i, j, k):
        if k == len(word):
            return True
        
        if i < 0 or i >= rows or j < 0 or j >= cols:
            return False
        if board[i][j] != word[k]:
            return False
        
        # Mark as visited
        temp = board[i][j]
        board[i][j] = '#'
        
        # Explore all 4 directions
        found = (backtrack(i + 1, j, k + 1) or
                 backtrack(i - 1, j, k + 1) or
                 backtrack(i, j + 1, k + 1) or
                 backtrack(i, j - 1, k + 1))
        
        # Restore
        board[i][j] = temp
        
        return found
    
    for i in range(rows):
        for j in range(cols):
            if backtrack(i, j, 0):
                return True
    
    return False
```

---

## Pattern 8: Generate Parentheses

```python
def generateParenthesis(n):
    result = []
    
    def backtrack(current, open_count, close_count):
        if len(current) == 2 * n:
            result.append(current)
            return
        
        if open_count < n:
            backtrack(current + '(', open_count + 1, close_count)
        
        if close_count < open_count:
            backtrack(current + ')', open_count, close_count + 1)
    
    backtrack('', 0, 0)
    return result
```

---

## Pattern 9: Letter Combinations of Phone Number

```python
def letterCombinations(digits):
    if not digits:
        return []
    
    phone = {
        '2': 'abc', '3': 'def', '4': 'ghi', '5': 'jkl',
        '6': 'mno', '7': 'pqrs', '8': 'tuv', '9': 'wxyz'
    }
    result = []
    
    def backtrack(index, current):
        if index == len(digits):
            result.append(current)
            return
        
        for char in phone[digits[index]]:
            backtrack(index + 1, current + char)
    
    backtrack(0, '')
    return result
```

---

## Pattern 10: Restore IP Addresses

```python
def restoreIpAddresses(s):
    result = []
    
    def backtrack(start, parts):
        if len(parts) == 4:
            if start == len(s):
                result.append('.'.join(parts))
            return
        
        # Each part can be 1-3 digits
        for length in range(1, 4):
            if start + length > len(s):
                break
            
            part = s[start:start + length]
            
            # Check validity
            if len(part) > 1 and part[0] == '0':  # No leading zeros
                continue
            if int(part) > 255:
                continue
            
            parts.append(part)
            backtrack(start + length, parts)
            parts.pop()
    
    backtrack(0, [])
    return result
```

---

## Quick Reference: Key Decisions

| Problem Type | Duplicates | Can Reuse | Sort First? |
|--------------|------------|-----------|-------------|
| Subsets | No | No | No |
| Subsets II | Yes | No | Yes |
| Permutations | No | N/A | No |
| Permutations II | Yes | N/A | Yes |
| Combination Sum | No | Yes | No |
| Combination Sum II | Yes | No | Yes |

## Pruning Techniques

1. **Sort first**: For duplicate handling and early termination
2. **Skip duplicates**: `if i > start and nums[i] == nums[i-1]: continue`
3. **Check bounds early**: Return immediately if impossible
4. **Use remaining**: Calculate if enough elements left
5. **Track visited**: Use set or modify input array

## Time Complexity

| Problem | Time Complexity |
|---------|-----------------|
| Subsets | O(n × 2^n) |
| Permutations | O(n × n!) |
| Combinations | O(k × C(n,k)) |
| N-Queens | O(n!) |
| Sudoku | O(9^m) where m = empty cells |

