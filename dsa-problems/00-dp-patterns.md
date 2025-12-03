# Dynamic Programming Patterns - Recursive → Memo → Iterative

## DP Problem-Solving Framework

### Step 1: Identify DP Problem
- Optimal substructure (optimal solution uses optimal sub-solutions)
- Overlapping subproblems (same subproblems solved multiple times)
- Keywords: "minimum", "maximum", "count ways", "is possible"

### Step 2: Conversion Path
```
Recursive (Brute Force)  →  Memoization (Top-Down)  →  Tabulation (Bottom-Up)  →  Space Optimized
     O(2^n)                     O(n) or O(n²)              O(n) or O(n²)            O(1) or O(n)
```

---

## Pattern 1: Fibonacci / Linear DP

### Problem: Nth Fibonacci Number

#### 1. Recursive (Exponential - O(2^n))
```python
def fib(n):
    if n <= 1:
        return n
    return fib(n - 1) + fib(n - 2)
```

#### 2. Memoization - Top Down (O(n) time, O(n) space)
```python
def fib(n, memo={}):
    if n <= 1:
        return n
    if n in memo:
        return memo[n]
    
    memo[n] = fib(n - 1, memo) + fib(n - 2, memo)
    return memo[n]

# Or with @cache decorator (Python 3.9+)
from functools import cache

@cache
def fib(n):
    if n <= 1:
        return n
    return fib(n - 1) + fib(n - 2)
```

#### 3. Tabulation - Bottom Up (O(n) time, O(n) space)
```python
def fib(n):
    if n <= 1:
        return n
    
    dp = [0] * (n + 1)
    dp[0], dp[1] = 0, 1
    
    for i in range(2, n + 1):
        dp[i] = dp[i - 1] + dp[i - 2]
    
    return dp[n]
```

#### 4. Space Optimized (O(n) time, O(1) space)
```python
def fib(n):
    if n <= 1:
        return n
    
    prev2, prev1 = 0, 1
    
    for i in range(2, n + 1):
        curr = prev1 + prev2
        prev2 = prev1
        prev1 = curr
    
    return prev1
```

---

## Pattern 2: Climbing Stairs / Count Ways

### Problem: Count ways to climb n stairs (1 or 2 steps at a time)

#### 1. Recursive
```python
def climbStairs(n):
    if n <= 2:
        return n
    return climbStairs(n - 1) + climbStairs(n - 2)
```

#### 2. Memoization
```python
def climbStairs(n, memo={}):
    if n <= 2:
        return n
    if n in memo:
        return memo[n]
    
    memo[n] = climbStairs(n - 1, memo) + climbStairs(n - 2, memo)
    return memo[n]
```

#### 3. Tabulation
```python
def climbStairs(n):
    if n <= 2:
        return n
    
    dp = [0] * (n + 1)
    dp[1], dp[2] = 1, 2
    
    for i in range(3, n + 1):
        dp[i] = dp[i - 1] + dp[i - 2]
    
    return dp[n]
```

#### 4. Space Optimized
```python
def climbStairs(n):
    if n <= 2:
        return n
    
    prev2, prev1 = 1, 2
    
    for i in range(3, n + 1):
        curr = prev1 + prev2
        prev2 = prev1
        prev1 = curr
    
    return prev1
```

---

## Pattern 3: House Robber / Max Non-Adjacent Sum

### Problem: Max sum of non-adjacent elements

#### 1. Recursive
```python
def rob(nums):
    def helper(i):
        if i < 0:
            return 0
        # Either rob current house + skip previous
        # Or skip current house
        return max(helper(i - 2) + nums[i], helper(i - 1))
    
    return helper(len(nums) - 1)
```

#### 2. Memoization
```python
def rob(nums):
    memo = {}
    
    def helper(i):
        if i < 0:
            return 0
        if i in memo:
            return memo[i]
        
        memo[i] = max(helper(i - 2) + nums[i], helper(i - 1))
        return memo[i]
    
    return helper(len(nums) - 1)
```

#### 3. Tabulation
```python
def rob(nums):
    if not nums:
        return 0
    if len(nums) == 1:
        return nums[0]
    
    n = len(nums)
    dp = [0] * n
    dp[0] = nums[0]
    dp[1] = max(nums[0], nums[1])
    
    for i in range(2, n):
        dp[i] = max(dp[i - 2] + nums[i], dp[i - 1])
    
    return dp[n - 1]
```

#### 4. Space Optimized
```python
def rob(nums):
    if not nums:
        return 0
    if len(nums) == 1:
        return nums[0]
    
    prev2 = nums[0]
    prev1 = max(nums[0], nums[1])
    
    for i in range(2, len(nums)):
        curr = max(prev2 + nums[i], prev1)
        prev2 = prev1
        prev1 = curr
    
    return prev1
```

---

## Pattern 4: 0/1 Knapsack

### Problem: Max value with weight capacity W

#### 1. Recursive
```python
def knapsack(weights, values, W):
    n = len(weights)
    
    def helper(i, remaining):
        if i < 0 or remaining == 0:
            return 0
        
        # Don't take item i
        result = helper(i - 1, remaining)
        
        # Take item i if it fits
        if weights[i] <= remaining:
            result = max(result, values[i] + helper(i - 1, remaining - weights[i]))
        
        return result
    
    return helper(n - 1, W)
```

#### 2. Memoization
```python
def knapsack(weights, values, W):
    n = len(weights)
    memo = {}
    
    def helper(i, remaining):
        if i < 0 or remaining == 0:
            return 0
        if (i, remaining) in memo:
            return memo[(i, remaining)]
        
        result = helper(i - 1, remaining)
        
        if weights[i] <= remaining:
            result = max(result, values[i] + helper(i - 1, remaining - weights[i]))
        
        memo[(i, remaining)] = result
        return result
    
    return helper(n - 1, W)
```

#### 3. Tabulation (O(nW) time, O(nW) space)
```python
def knapsack(weights, values, W):
    n = len(weights)
    # dp[i][w] = max value using first i items with capacity w
    dp = [[0] * (W + 1) for _ in range(n + 1)]
    
    for i in range(1, n + 1):
        for w in range(W + 1):
            # Don't take item i-1
            dp[i][w] = dp[i - 1][w]
            
            # Take item i-1 if it fits
            if weights[i - 1] <= w:
                dp[i][w] = max(dp[i][w], 
                              values[i - 1] + dp[i - 1][w - weights[i - 1]])
    
    return dp[n][W]
```

#### 4. Space Optimized (O(nW) time, O(W) space)
```python
def knapsack(weights, values, W):
    n = len(weights)
    dp = [0] * (W + 1)
    
    for i in range(n):
        # IMPORTANT: Iterate backwards to avoid using updated values
        for w in range(W, weights[i] - 1, -1):
            dp[w] = max(dp[w], values[i] + dp[w - weights[i]])
    
    return dp[W]
```

---

## Pattern 5: Unbounded Knapsack / Coin Change

### Problem: Min coins to make amount

#### 1. Recursive
```python
def coinChange(coins, amount):
    def helper(remaining):
        if remaining == 0:
            return 0
        if remaining < 0:
            return float('inf')
        
        min_coins = float('inf')
        for coin in coins:
            result = helper(remaining - coin)
            min_coins = min(min_coins, result + 1)
        
        return min_coins
    
    result = helper(amount)
    return result if result != float('inf') else -1
```

#### 2. Memoization
```python
def coinChange(coins, amount):
    memo = {}
    
    def helper(remaining):
        if remaining == 0:
            return 0
        if remaining < 0:
            return float('inf')
        if remaining in memo:
            return memo[remaining]
        
        min_coins = float('inf')
        for coin in coins:
            result = helper(remaining - coin)
            min_coins = min(min_coins, result + 1)
        
        memo[remaining] = min_coins
        return min_coins
    
    result = helper(amount)
    return result if result != float('inf') else -1
```

#### 3. Tabulation
```python
def coinChange(coins, amount):
    # dp[i] = min coins to make amount i
    dp = [float('inf')] * (amount + 1)
    dp[0] = 0
    
    for i in range(1, amount + 1):
        for coin in coins:
            if coin <= i:
                dp[i] = min(dp[i], dp[i - coin] + 1)
    
    return dp[amount] if dp[amount] != float('inf') else -1
```

### Variation: Count Ways to Make Amount

```python
def coinChangeWays(coins, amount):
    # Memoization
    memo = {}
    
    def helper(remaining, index):
        if remaining == 0:
            return 1
        if remaining < 0 or index >= len(coins):
            return 0
        if (remaining, index) in memo:
            return memo[(remaining, index)]
        
        # Use current coin OR skip to next coin
        memo[(remaining, index)] = (
            helper(remaining - coins[index], index) +  # Use (can reuse)
            helper(remaining, index + 1)                # Skip
        )
        return memo[(remaining, index)]
    
    return helper(amount, 0)


def coinChangeWays_tabulation(coins, amount):
    dp = [0] * (amount + 1)
    dp[0] = 1
    
    # Process each coin to avoid counting duplicates
    for coin in coins:
        for i in range(coin, amount + 1):
            dp[i] += dp[i - coin]
    
    return dp[amount]
```

---

## Pattern 6: Longest Common Subsequence (LCS)

### Problem: Find LCS of two strings

#### 1. Recursive
```python
def lcs(s1, s2):
    def helper(i, j):
        if i < 0 or j < 0:
            return 0
        
        if s1[i] == s2[j]:
            return 1 + helper(i - 1, j - 1)
        else:
            return max(helper(i - 1, j), helper(i, j - 1))
    
    return helper(len(s1) - 1, len(s2) - 1)
```

#### 2. Memoization
```python
def lcs(s1, s2):
    memo = {}
    
    def helper(i, j):
        if i < 0 or j < 0:
            return 0
        if (i, j) in memo:
            return memo[(i, j)]
        
        if s1[i] == s2[j]:
            memo[(i, j)] = 1 + helper(i - 1, j - 1)
        else:
            memo[(i, j)] = max(helper(i - 1, j), helper(i, j - 1))
        
        return memo[(i, j)]
    
    return helper(len(s1) - 1, len(s2) - 1)
```

#### 3. Tabulation
```python
def lcs(s1, s2):
    m, n = len(s1), len(s2)
    # dp[i][j] = LCS of s1[0:i] and s2[0:j]
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                dp[i][j] = 1 + dp[i - 1][j - 1]
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
    
    return dp[m][n]
```

#### 4. Space Optimized
```python
def lcs(s1, s2):
    m, n = len(s1), len(s2)
    prev = [0] * (n + 1)
    
    for i in range(1, m + 1):
        curr = [0] * (n + 1)
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                curr[j] = 1 + prev[j - 1]
            else:
                curr[j] = max(prev[j], curr[j - 1])
        prev = curr
    
    return prev[n]
```

#### Print LCS String
```python
def lcs_with_string(s1, s2):
    m, n = len(s1), len(s2)
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                dp[i][j] = 1 + dp[i - 1][j - 1]
            else:
                dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
    
    # Backtrack to find the string
    result = []
    i, j = m, n
    while i > 0 and j > 0:
        if s1[i - 1] == s2[j - 1]:
            result.append(s1[i - 1])
            i -= 1
            j -= 1
        elif dp[i - 1][j] > dp[i][j - 1]:
            i -= 1
        else:
            j -= 1
    
    return ''.join(reversed(result))
```

---

## Pattern 7: Longest Increasing Subsequence (LIS)

### Problem: Find length of LIS

#### 1. Recursive
```python
def lis(nums):
    def helper(i, prev_index):
        if i == len(nums):
            return 0
        
        # Don't take current element
        result = helper(i + 1, prev_index)
        
        # Take current element if valid
        if prev_index == -1 or nums[i] > nums[prev_index]:
            result = max(result, 1 + helper(i + 1, i))
        
        return result
    
    return helper(0, -1)
```

#### 2. Memoization
```python
def lis(nums):
    n = len(nums)
    memo = {}
    
    def helper(i, prev_index):
        if i == n:
            return 0
        if (i, prev_index) in memo:
            return memo[(i, prev_index)]
        
        result = helper(i + 1, prev_index)
        
        if prev_index == -1 or nums[i] > nums[prev_index]:
            result = max(result, 1 + helper(i + 1, i))
        
        memo[(i, prev_index)] = result
        return result
    
    return helper(0, -1)
```

#### 3. Tabulation O(n²)
```python
def lis(nums):
    if not nums:
        return 0
    
    n = len(nums)
    # dp[i] = length of LIS ending at index i
    dp = [1] * n
    
    for i in range(1, n):
        for j in range(i):
            if nums[j] < nums[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    
    return max(dp)
```

#### 4. Binary Search O(n log n)
```python
import bisect

def lis(nums):
    # tails[i] = smallest tail element for LIS of length i+1
    tails = []
    
    for num in nums:
        pos = bisect.bisect_left(tails, num)
        if pos == len(tails):
            tails.append(num)
        else:
            tails[pos] = num
    
    return len(tails)
```

---

## Pattern 8: Edit Distance

### Problem: Min operations to convert s1 to s2

#### 1. Recursive
```python
def editDistance(s1, s2):
    def helper(i, j):
        # Base cases
        if i < 0:
            return j + 1  # Insert remaining chars
        if j < 0:
            return i + 1  # Delete remaining chars
        
        if s1[i] == s2[j]:
            return helper(i - 1, j - 1)
        else:
            insert = helper(i, j - 1)      # Insert s2[j]
            delete = helper(i - 1, j)      # Delete s1[i]
            replace = helper(i - 1, j - 1) # Replace s1[i] with s2[j]
            return 1 + min(insert, delete, replace)
    
    return helper(len(s1) - 1, len(s2) - 1)
```

#### 2. Memoization
```python
def editDistance(s1, s2):
    memo = {}
    
    def helper(i, j):
        if i < 0:
            return j + 1
        if j < 0:
            return i + 1
        if (i, j) in memo:
            return memo[(i, j)]
        
        if s1[i] == s2[j]:
            memo[(i, j)] = helper(i - 1, j - 1)
        else:
            memo[(i, j)] = 1 + min(
                helper(i, j - 1),
                helper(i - 1, j),
                helper(i - 1, j - 1)
            )
        
        return memo[(i, j)]
    
    return helper(len(s1) - 1, len(s2) - 1)
```

#### 3. Tabulation
```python
def editDistance(s1, s2):
    m, n = len(s1), len(s2)
    # dp[i][j] = min operations for s1[0:i] and s2[0:j]
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    
    # Base cases
    for i in range(m + 1):
        dp[i][0] = i  # Delete all chars from s1
    for j in range(n + 1):
        dp[0][j] = j  # Insert all chars to empty s1
    
    for i in range(1, m + 1):
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                dp[i][j] = dp[i - 1][j - 1]
            else:
                dp[i][j] = 1 + min(
                    dp[i][j - 1],      # Insert
                    dp[i - 1][j],      # Delete
                    dp[i - 1][j - 1]   # Replace
                )
    
    return dp[m][n]
```

#### 4. Space Optimized
```python
def editDistance(s1, s2):
    m, n = len(s1), len(s2)
    prev = list(range(n + 1))
    
    for i in range(1, m + 1):
        curr = [i] + [0] * n
        for j in range(1, n + 1):
            if s1[i - 1] == s2[j - 1]:
                curr[j] = prev[j - 1]
            else:
                curr[j] = 1 + min(curr[j - 1], prev[j], prev[j - 1])
        prev = curr
    
    return prev[n]
```

---

## Pattern 9: Matrix Chain Multiplication / Interval DP

### Problem: Min cost to multiply matrices

#### 1. Recursive
```python
def matrixChainOrder(dims):
    """
    dims = [d0, d1, d2, ..., dn]
    Matrix i has dimensions dims[i-1] x dims[i]
    """
    n = len(dims) - 1  # Number of matrices
    
    def helper(i, j):
        if i == j:
            return 0
        
        min_cost = float('inf')
        for k in range(i, j):
            cost = (helper(i, k) + 
                   helper(k + 1, j) + 
                   dims[i - 1] * dims[k] * dims[j])
            min_cost = min(min_cost, cost)
        
        return min_cost
    
    return helper(1, n)
```

#### 2. Memoization
```python
def matrixChainOrder(dims):
    n = len(dims) - 1
    memo = {}
    
    def helper(i, j):
        if i == j:
            return 0
        if (i, j) in memo:
            return memo[(i, j)]
        
        min_cost = float('inf')
        for k in range(i, j):
            cost = (helper(i, k) + 
                   helper(k + 1, j) + 
                   dims[i - 1] * dims[k] * dims[j])
            min_cost = min(min_cost, cost)
        
        memo[(i, j)] = min_cost
        return min_cost
    
    return helper(1, n)
```

#### 3. Tabulation (Bottom-Up by Chain Length)
```python
def matrixChainOrder(dims):
    n = len(dims) - 1
    # dp[i][j] = min cost to multiply matrices i to j
    dp = [[0] * (n + 1) for _ in range(n + 1)]
    
    # l is chain length
    for l in range(2, n + 1):
        for i in range(1, n - l + 2):
            j = i + l - 1
            dp[i][j] = float('inf')
            
            for k in range(i, j):
                cost = (dp[i][k] + 
                       dp[k + 1][j] + 
                       dims[i - 1] * dims[k] * dims[j])
                dp[i][j] = min(dp[i][j], cost)
    
    return dp[1][n]
```

---

## Pattern 10: Partition DP / Palindrome Partitioning

### Problem: Min cuts for palindrome partitioning

#### 1. Recursive
```python
def minCut(s):
    def is_palindrome(l, r):
        while l < r:
            if s[l] != s[r]:
                return False
            l += 1
            r -= 1
        return True
    
    def helper(i):
        if i == len(s):
            return 0
        
        min_cuts = float('inf')
        for j in range(i, len(s)):
            if is_palindrome(i, j):
                cuts = 1 + helper(j + 1)
                min_cuts = min(min_cuts, cuts)
        
        return min_cuts
    
    return helper(0) - 1  # -1 because we count one extra cut
```

#### 2. Memoization
```python
def minCut(s):
    n = len(s)
    # Precompute palindrome status
    is_palin = [[False] * n for _ in range(n)]
    for i in range(n - 1, -1, -1):
        for j in range(i, n):
            if s[i] == s[j] and (j - i <= 2 or is_palin[i + 1][j - 1]):
                is_palin[i][j] = True
    
    memo = {}
    
    def helper(i):
        if i == n:
            return 0
        if i in memo:
            return memo[i]
        
        min_cuts = float('inf')
        for j in range(i, n):
            if is_palin[i][j]:
                cuts = 1 + helper(j + 1)
                min_cuts = min(min_cuts, cuts)
        
        memo[i] = min_cuts
        return min_cuts
    
    return helper(0) - 1
```

#### 3. Tabulation
```python
def minCut(s):
    n = len(s)
    
    # Precompute palindrome status
    is_palin = [[False] * n for _ in range(n)]
    for i in range(n - 1, -1, -1):
        for j in range(i, n):
            if s[i] == s[j] and (j - i <= 2 or is_palin[i + 1][j - 1]):
                is_palin[i][j] = True
    
    # dp[i] = min cuts for s[i:]
    dp = [0] * (n + 1)
    
    for i in range(n - 1, -1, -1):
        min_cuts = float('inf')
        for j in range(i, n):
            if is_palin[i][j]:
                min_cuts = min(min_cuts, 1 + dp[j + 1])
        dp[i] = min_cuts
    
    return dp[0] - 1
```

---

## Pattern 11: Grid DP

### Problem: Unique Paths (Count ways from top-left to bottom-right)

#### 1. Recursive
```python
def uniquePaths(m, n):
    def helper(i, j):
        if i == 0 and j == 0:
            return 1
        if i < 0 or j < 0:
            return 0
        
        return helper(i - 1, j) + helper(i, j - 1)
    
    return helper(m - 1, n - 1)
```

#### 2. Memoization
```python
def uniquePaths(m, n):
    memo = {}
    
    def helper(i, j):
        if i == 0 and j == 0:
            return 1
        if i < 0 or j < 0:
            return 0
        if (i, j) in memo:
            return memo[(i, j)]
        
        memo[(i, j)] = helper(i - 1, j) + helper(i, j - 1)
        return memo[(i, j)]
    
    return helper(m - 1, n - 1)
```

#### 3. Tabulation
```python
def uniquePaths(m, n):
    dp = [[1] * n for _ in range(m)]
    
    for i in range(1, m):
        for j in range(1, n):
            dp[i][j] = dp[i - 1][j] + dp[i][j - 1]
    
    return dp[m - 1][n - 1]
```

#### 4. Space Optimized
```python
def uniquePaths(m, n):
    dp = [1] * n
    
    for i in range(1, m):
        for j in range(1, n):
            dp[j] += dp[j - 1]
    
    return dp[n - 1]
```

### Minimum Path Sum

```python
def minPathSum(grid):
    m, n = len(grid), len(grid[0])
    
    # Tabulation
    dp = [[0] * n for _ in range(m)]
    dp[0][0] = grid[0][0]
    
    # First row
    for j in range(1, n):
        dp[0][j] = dp[0][j - 1] + grid[0][j]
    
    # First column
    for i in range(1, m):
        dp[i][0] = dp[i - 1][0] + grid[i][0]
    
    # Fill rest
    for i in range(1, m):
        for j in range(1, n):
            dp[i][j] = min(dp[i - 1][j], dp[i][j - 1]) + grid[i][j]
    
    return dp[m - 1][n - 1]


# Space optimized
def minPathSum_optimized(grid):
    m, n = len(grid), len(grid[0])
    dp = [float('inf')] * n
    dp[0] = 0
    
    for i in range(m):
        new_dp = [0] * n
        for j in range(n):
            if j == 0:
                new_dp[j] = dp[j] + grid[i][j]
            else:
                new_dp[j] = min(dp[j], new_dp[j - 1]) + grid[i][j]
        dp = new_dp
    
    return dp[n - 1]
```

---

## Pattern 12: Subset Sum / Partition

### Problem: Can array be partitioned into two equal sum subsets?

#### 1. Recursive
```python
def canPartition(nums):
    total = sum(nums)
    if total % 2 != 0:
        return False
    
    target = total // 2
    
    def helper(i, remaining):
        if remaining == 0:
            return True
        if i < 0 or remaining < 0:
            return False
        
        return helper(i - 1, remaining - nums[i]) or helper(i - 1, remaining)
    
    return helper(len(nums) - 1, target)
```

#### 2. Memoization
```python
def canPartition(nums):
    total = sum(nums)
    if total % 2 != 0:
        return False
    
    target = total // 2
    memo = {}
    
    def helper(i, remaining):
        if remaining == 0:
            return True
        if i < 0 or remaining < 0:
            return False
        if (i, remaining) in memo:
            return memo[(i, remaining)]
        
        memo[(i, remaining)] = (helper(i - 1, remaining - nums[i]) or 
                                helper(i - 1, remaining))
        return memo[(i, remaining)]
    
    return helper(len(nums) - 1, target)
```

#### 3. Tabulation
```python
def canPartition(nums):
    total = sum(nums)
    if total % 2 != 0:
        return False
    
    target = total // 2
    n = len(nums)
    
    # dp[i][j] = can we make sum j using first i elements?
    dp = [[False] * (target + 1) for _ in range(n + 1)]
    
    # Base case: sum 0 is always achievable
    for i in range(n + 1):
        dp[i][0] = True
    
    for i in range(1, n + 1):
        for j in range(1, target + 1):
            dp[i][j] = dp[i - 1][j]  # Don't take
            if j >= nums[i - 1]:
                dp[i][j] = dp[i][j] or dp[i - 1][j - nums[i - 1]]
    
    return dp[n][target]
```

#### 4. Space Optimized
```python
def canPartition(nums):
    total = sum(nums)
    if total % 2 != 0:
        return False
    
    target = total // 2
    dp = [False] * (target + 1)
    dp[0] = True
    
    for num in nums:
        # Iterate backwards!
        for j in range(target, num - 1, -1):
            dp[j] = dp[j] or dp[j - num]
    
    return dp[target]
```

---

## Pattern 13: Stock Problems

### Buy and Sell Stock with Cooldown

```python
# State: 0 = can buy, 1 = can sell, 2 = cooldown
def maxProfit(prices):
    memo = {}
    
    def helper(i, can_buy):
        if i >= len(prices):
            return 0
        if (i, can_buy) in memo:
            return memo[(i, can_buy)]
        
        if can_buy:
            # Buy or skip
            buy = -prices[i] + helper(i + 1, False)
            skip = helper(i + 1, True)
            memo[(i, can_buy)] = max(buy, skip)
        else:
            # Sell (with cooldown) or skip
            sell = prices[i] + helper(i + 2, True)  # i+2 for cooldown
            skip = helper(i + 1, False)
            memo[(i, can_buy)] = max(sell, skip)
        
        return memo[(i, can_buy)]
    
    return helper(0, True)


# Tabulation
def maxProfit_tab(prices):
    if not prices:
        return 0
    
    n = len(prices)
    # dp[i][0] = max profit at day i if we can buy
    # dp[i][1] = max profit at day i if we can sell
    dp = [[0] * 2 for _ in range(n + 2)]
    
    for i in range(n - 1, -1, -1):
        dp[i][1] = max(-prices[i] + dp[i + 1][0], dp[i + 1][1])  # Buy or skip
        dp[i][0] = max(prices[i] + dp[i + 2][1], dp[i + 1][0])   # Sell or skip
    
    return dp[0][1]
```

---

## Quick Conversion Guide

### Recursive → Memoization
1. Add memo dictionary/array
2. Check if state exists in memo at start
3. Store result in memo before returning

### Memoization → Tabulation
1. Create DP table with dimensions = number of changing parameters
2. Fill base cases (what recursive returns immediately)
3. Fill table in order that ensures dependencies are computed first
4. Usually: reverse the direction of recursion

### Space Optimization
1. Identify which previous states are actually needed
2. Usually only need previous row/column for 2D DP
3. **For 0/1 knapsack type**: iterate backwards to avoid overwriting
4. **For unbounded type**: iterate forwards (can reuse current row)

---

## DP State Identification Cheat Sheet

| Problem Type | State Variables |
|--------------|-----------------|
| Linear (Fibonacci, Climbing) | `dp[i]` = answer for first i items |
| Two Sequence (LCS, Edit) | `dp[i][j]` = answer for s1[0:i], s2[0:j] |
| Knapsack | `dp[i][w]` = answer using i items, capacity w |
| Interval (Matrix Chain) | `dp[i][j]` = answer for range [i, j] |
| Grid | `dp[i][j]` = answer reaching cell (i, j) |
| Partition | `dp[i][k]` = answer for first i items, k partitions |
| Bitmask | `dp[mask]` = answer with items in mask selected |

