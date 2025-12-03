# Best Time to Buy and Sell Stock

## Problem Information
- **Difficulty**: Easy
- **Frequency**: 43.1%
- **Acceptance Rate**: 55.3%
- **Topics**: Array, Dynamic Programming
- **LeetCode Link**: https://leetcode.com/problems/best-time-to-buy-and-sell-stock

## Problem Description

You are given an array `prices` where `prices[i]` is the price of a given stock on the `i-th` day.

You want to maximize your profit by choosing a **single day** to buy one stock and choosing a **different day in the future** to sell that stock.

Return the maximum profit you can achieve from this transaction. If you cannot achieve any profit, return `0`.

## Examples

### Example 1:
```
Input: prices = [7,1,5,3,6,4]
Output: 5
Explanation: Buy on day 2 (price = 1) and sell on day 5 (price = 6), profit = 6-1 = 5.
Note that buying on day 2 and selling on day 1 is not allowed because you must buy before you sell.
```

### Example 2:
```
Input: prices = [7,6,4,3,1]
Output: 0
Explanation: In this case, no transactions are done and the max profit = 0.
```

## Constraints

- `1 <= prices.length <= 10^5`
- `0 <= prices[i] <= 10^4`

## Approach

### Key Insight:
- We want to find the maximum difference `prices[j] - prices[i]` where `j > i`
- Track the minimum price seen so far
- At each day, calculate profit if we sell at current price

### Algorithm:
1. Initialize `min_price = infinity` and `max_profit = 0`
2. For each price:
   - Update `min_price` if current price is lower
   - Update `max_profit` if selling at current price gives better profit

## Solution

```python
from typing import List

class Solution:
    def maxProfit(self, prices: List[int]) -> int:
        min_price = float('inf')
        max_profit = 0
        
        for price in prices:
            if price < min_price:
                min_price = price
            elif price - min_price > max_profit:
                max_profit = price - min_price
        
        return max_profit
```

## Alternative Solution (More Readable)

```python
from typing import List

class Solution:
    def maxProfit(self, prices: List[int]) -> int:
        min_price = float('inf')
        max_profit = 0
        
        for price in prices:
            # Update minimum price seen so far
            min_price = min(min_price, price)
            
            # Calculate profit if we sell today
            profit = price - min_price
            
            # Update maximum profit
            max_profit = max(max_profit, profit)
        
        return max_profit
```

## Solution Using Kadane's Algorithm Perspective

```python
from typing import List

class Solution:
    def maxProfit(self, prices: List[int]) -> int:
        """
        Think of it as maximum subarray sum problem on daily changes.
        
        Daily changes: [0, -6, 4, -2, 3, -2] for [7,1,5,3,6,4]
        Maximum subarray sum = maximum profit
        """
        max_profit = 0
        current_profit = 0
        
        for i in range(1, len(prices)):
            # Daily change
            change = prices[i] - prices[i-1]
            
            # Either start fresh or continue from previous
            current_profit = max(0, current_profit + change)
            max_profit = max(max_profit, current_profit)
        
        return max_profit
```

## Brute Force Solution (O(n²) - For Understanding)

```python
from typing import List

class Solution:
    def maxProfit(self, prices: List[int]) -> int:
        max_profit = 0
        n = len(prices)
        
        for i in range(n):
            for j in range(i + 1, n):
                profit = prices[j] - prices[i]
                max_profit = max(max_profit, profit)
        
        return max_profit
```

## Complexity Analysis

### Optimal Solution:
- **Time**: O(n) - single pass through array
- **Space**: O(1) - only two variables

### Brute Force:
- **Time**: O(n²) - nested loops
- **Space**: O(1)

## Visual Example

```
Prices: [7, 1, 5, 3, 6, 4]
Index:   0  1  2  3  4  5

Step-by-step:
i=0: price=7, min_price=7, profit=0, max_profit=0
i=1: price=1, min_price=1, profit=0, max_profit=0
i=2: price=5, min_price=1, profit=4, max_profit=4
i=3: price=3, min_price=1, profit=2, max_profit=4
i=4: price=6, min_price=1, profit=5, max_profit=5 ✓
i=5: price=4, min_price=1, profit=3, max_profit=5

Best: Buy at 1, sell at 6, profit = 5
```

## Key Patterns & Techniques

1. **Track Running Minimum**: Keep track of best buy price so far
2. **One-Pass Algorithm**: Process each element once
3. **Greedy Approach**: At each point, consider selling at current price
4. **Kadane's Variant**: Can be viewed as max subarray on price changes

## Connection to Kadane's Algorithm

```
Prices:  [7, 1, 5, 3, 6, 4]
Changes: [_, -6, 4, -2, 3, -2]  (prices[i] - prices[i-1])

Maximum subarray sum of changes = maximum profit
Why? Sum of changes from i to j = prices[j] - prices[i]

Subarray [-6, 4, -2, 3] = -6 + 4 - 2 + 3 = -1
This equals prices[4] - prices[0] = 6 - 7 = -1

Best subarray: [4, -2, 3] = 5
This equals prices[4] - prices[1] = 6 - 1 = 5 ✓
```

## Edge Cases

1. **Empty or single element**: Return 0 (can't buy and sell)
2. **Descending prices**: Return 0 (no profitable transaction)
3. **All same prices**: Return 0
4. **Ascending prices**: Buy first day, sell last day

## Common Mistakes to Avoid

1. Selling before buying (must maintain i < j)
2. Returning negative profit (should return 0 if no profit possible)
3. Using O(n²) solution for large inputs (TLE)
4. Not handling edge case of single price

## Follow-up Problems

### Best Time to Buy and Sell Stock II (Multiple Transactions)
```python
def maxProfit(prices):
    profit = 0
    for i in range(1, len(prices)):
        if prices[i] > prices[i-1]:
            profit += prices[i] - prices[i-1]
    return profit
```

### Best Time to Buy and Sell Stock III (At Most 2 Transactions)
More complex DP solution required.

## Related Problems

- [122. Best Time to Buy and Sell Stock II](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-ii/)
- [123. Best Time to Buy and Sell Stock III](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iii/)
- [188. Best Time to Buy and Sell Stock IV](https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iv/)
- [53. Maximum Subarray](https://leetcode.com/problems/maximum-subarray/)

