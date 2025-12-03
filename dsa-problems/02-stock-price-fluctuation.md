# Stock Price Fluctuation

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 96.9%
- **Acceptance Rate**: 48.2%
- **Topics**: Hash Table, Design, Heap (Priority Queue), Data Stream, Ordered Set
- **LeetCode Link**: https://leetcode.com/problems/stock-price-fluctuation

## Problem Description

You are given a stream of records about a particular stock. Each record contains a timestamp and the corresponding price of the stock at that timestamp.

Unfortunately due to the volatile nature of the stock market, the records do not come in order. Even worse, some records may be incorrect. Another record with the same timestamp may appear later in the stream correcting the price of the previous wrong record.

Design an algorithm that:
- **Updates** the price of the stock at a particular timestamp, correcting the price from any previous records at the timestamp.
- Finds the **latest price** of the stock based on the current records. The latest price is the price at the latest timestamp recorded.
- Finds the **maximum price** the stock has been based on the current records.
- Finds the **minimum price** the stock has been based on the current records.

Implement the `StockPrice` class:
- `StockPrice()` Initializes the object with no price records.
- `void update(int timestamp, int price)` Updates the price of the stock at the given timestamp.
- `int current()` Returns the latest price of the stock.
- `int maximum()` Returns the maximum price of the stock.
- `int minimum()` Returns the minimum price of the stock.

## Examples

### Example 1:
```
Input
["StockPrice", "update", "update", "current", "maximum", "update", "maximum", "update", "minimum"]
[[], [1, 10], [2, 5], [], [], [1, 3], [], [4, 2], []]
Output
[null, null, null, 5, 10, null, 5, null, 2]

Explanation
StockPrice stockPrice = new StockPrice();
stockPrice.update(1, 10); // Timestamps are [1] with corresponding prices [10].
stockPrice.update(2, 5);  // Timestamps are [1,2] with corresponding prices [10,5].
stockPrice.current();     // return 5, the latest timestamp is 2 with the price being 5.
stockPrice.maximum();     // return 10, the maximum price is 10 at timestamp 1.
stockPrice.update(1, 3);  // The previous timestamp 1 had the wrong price, so it is updated to 3.
                          // Timestamps are [1,2] with corresponding prices [3,5].
stockPrice.maximum();     // return 5, the maximum price is 5 after the correction.
stockPrice.update(4, 2);  // Timestamps are [1,2,4] with corresponding prices [3,5,2].
stockPrice.minimum();     // return 2, the minimum price is 2 at timestamp 4.
```

## Constraints

- `1 <= timestamp, price <= 10^9`
- At most `10^5` calls will be made in total to `update`, `current`, `maximum`, and `minimum`.
- `current`, `maximum`, and `minimum` will be called only after `update` has been called at least once.

## Approach

### Key Insights:
1. Need to track timestamp → price mapping (for corrections)
2. Need to find max/min prices efficiently
3. Need to track the latest timestamp for `current()`

### Data Structures:
1. **Hash Map**: To store timestamp → price mapping
2. **Two Heaps**: Max heap for maximum, Min heap for minimum
3. **Variable**: Track the latest timestamp

### Algorithm:
- When updating, if timestamp exists, we need to handle the old price
- Use lazy deletion: keep stale entries in heaps, validate when querying

## Solution

```python
import heapq
from collections import defaultdict

class StockPrice:
    def __init__(self):
        self.timestamp_price = {}  # timestamp -> price
        self.latest_timestamp = 0
        self.max_heap = []  # (-price, timestamp) for max price
        self.min_heap = []  # (price, timestamp) for min price
    
    def update(self, timestamp: int, price: int) -> None:
        # Update the timestamp -> price mapping
        self.timestamp_price[timestamp] = price
        
        # Update latest timestamp
        self.latest_timestamp = max(self.latest_timestamp, timestamp)
        
        # Add to heaps (we'll do lazy deletion when querying)
        heapq.heappush(self.max_heap, (-price, timestamp))
        heapq.heappush(self.min_heap, (price, timestamp))
    
    def current(self) -> int:
        return self.timestamp_price[self.latest_timestamp]
    
    def maximum(self) -> int:
        # Lazy deletion: remove stale entries
        while self.max_heap:
            price, timestamp = self.max_heap[0]
            # Check if this entry is still valid
            if self.timestamp_price[timestamp] == -price:
                return -price
            # Stale entry, remove it
            heapq.heappop(self.max_heap)
        return -1  # Should never reach here based on constraints
    
    def minimum(self) -> int:
        # Lazy deletion: remove stale entries
        while self.min_heap:
            price, timestamp = self.min_heap[0]
            # Check if this entry is still valid
            if self.timestamp_price[timestamp] == price:
                return price
            # Stale entry, remove it
            heapq.heappop(self.min_heap)
        return -1  # Should never reach here based on constraints
```

## Alternative Solution (Using SortedList)

```python
from sortedcontainers import SortedList

class StockPrice:
    def __init__(self):
        self.timestamp_price = {}
        self.latest_timestamp = 0
        self.prices = SortedList()  # Maintains sorted order of all prices
    
    def update(self, timestamp: int, price: int) -> None:
        # If timestamp exists, remove old price from sorted list
        if timestamp in self.timestamp_price:
            old_price = self.timestamp_price[timestamp]
            self.prices.remove(old_price)
        
        # Update mapping and add new price
        self.timestamp_price[timestamp] = price
        self.prices.add(price)
        
        # Update latest timestamp
        self.latest_timestamp = max(self.latest_timestamp, timestamp)
    
    def current(self) -> int:
        return self.timestamp_price[self.latest_timestamp]
    
    def maximum(self) -> int:
        return self.prices[-1]
    
    def minimum(self) -> int:
        return self.prices[0]
```

## Solution Using Counter (Handling Duplicate Prices)

```python
import heapq
from collections import Counter

class StockPrice:
    def __init__(self):
        self.timestamp_price = {}
        self.latest_timestamp = 0
        self.max_heap = []
        self.min_heap = []
        self.price_count = Counter()  # Track count of each price
    
    def update(self, timestamp: int, price: int) -> None:
        # If updating existing timestamp, decrease count of old price
        if timestamp in self.timestamp_price:
            old_price = self.timestamp_price[timestamp]
            self.price_count[old_price] -= 1
        
        # Update mapping
        self.timestamp_price[timestamp] = price
        self.price_count[price] += 1
        
        # Update latest timestamp
        self.latest_timestamp = max(self.latest_timestamp, timestamp)
        
        # Add to heaps
        heapq.heappush(self.max_heap, -price)
        heapq.heappush(self.min_heap, price)
    
    def current(self) -> int:
        return self.timestamp_price[self.latest_timestamp]
    
    def maximum(self) -> int:
        while self.price_count[-self.max_heap[0]] == 0:
            heapq.heappop(self.max_heap)
        return -self.max_heap[0]
    
    def minimum(self) -> int:
        while self.price_count[self.min_heap[0]] == 0:
            heapq.heappop(self.min_heap)
        return self.min_heap[0]
```

## Complexity Analysis

### Heap-based Solution:
- **update**: O(log n) - heap push operation
- **current**: O(1) - direct lookup
- **maximum**: O(log n) amortized - may need to pop stale entries
- **minimum**: O(log n) amortized - may need to pop stale entries

### Space Complexity: O(n)
- Where n is the number of update calls
- Stores all prices in heaps (with potential stale entries)

### SortedList Solution:
- **update**: O(log n) - SortedList add/remove
- **current/maximum/minimum**: O(1)
- **Space**: O(n) for storing prices

## Key Patterns & Techniques

1. **Lazy Deletion**: Instead of removing outdated entries immediately, validate during queries
2. **Dual Heaps**: Using separate heaps for max and min operations
3. **Hash Map for O(1) Lookup**: Quick access to current price by timestamp
4. **SortedList/Ordered Set**: Alternative approach for maintaining sorted prices

## Common Mistakes to Avoid

1. Not handling price corrections (same timestamp, different price)
2. Not validating heap entries against current mapping
3. Forgetting that prices can be duplicated across different timestamps

## Related Problems

- [295. Find Median from Data Stream](https://leetcode.com/problems/find-median-from-data-stream/)
- [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/)
- [981. Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/)

