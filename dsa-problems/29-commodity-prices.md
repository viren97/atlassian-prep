# Commodity Prices Stream

## Problem Information
- **Difficulty**: Medium
- **Company**: Atlassian
- **Topics**: Design, Hash Table, TreeMap, Heap, Data Stream

## Problem Description

You are given a stream of data points consisting of `<timestamp, commodityPrice>`. You need to return the `maxCommodityPrice` at any point in time.

**Requirements:**
1. Timestamps can be **out of order**
2. There can be **duplicate timestamps** - update the price if entry exists
3. Optimize for **frequent reads and writes**
4. Bonus: Reduce `getMaxCommodityPrice()` to O(1)

---

## Solution 1: TreeMap Based (Balanced)

Good for both reads and writes.

```kotlin
import java.util.TreeMap

class CommodityPriceTracker {
    // timestamp -> price
    private val priceByTimestamp = mutableMapOf<Long, Int>()
    
    // price -> count (for tracking max with duplicates)
    private val priceCount = TreeMap<Int, Int>()
    
    /**
     * Update or insert price at timestamp
     * Time: O(log n)
     */
    fun update(timestamp: Long, price: Int) {
        // If timestamp exists, remove old price from count
        priceByTimestamp[timestamp]?.let { oldPrice ->
            decrementPrice(oldPrice)
        }
        
        // Update timestamp -> price
        priceByTimestamp[timestamp] = price
        
        // Increment new price count
        priceCount[price] = priceCount.getOrDefault(price, 0) + 1
    }
    
    /**
     * Get maximum price
     * Time: O(log n) with TreeMap
     */
    fun getMaxCommodityPrice(): Int? {
        return priceCount.lastKey()  // Highest key in TreeMap
    }
    
    /**
     * Get minimum price
     * Time: O(log n)
     */
    fun getMinCommodityPrice(): Int? {
        return priceCount.firstKey()
    }
    
    /**
     * Get price at specific timestamp
     * Time: O(1)
     */
    fun getPriceAt(timestamp: Long): Int? {
        return priceByTimestamp[timestamp]
    }
    
    private fun decrementPrice(price: Int) {
        val count = priceCount[price] ?: return
        if (count == 1) {
            priceCount.remove(price)
        } else {
            priceCount[price] = count - 1
        }
    }
}
```

### Python Implementation

```python
from sortedcontainers import SortedDict
from typing import Optional

class CommodityPriceTracker:
    def __init__(self):
        self.price_by_timestamp = {}  # timestamp -> price
        self.price_count = SortedDict()  # price -> count
    
    def update(self, timestamp: int, price: int) -> None:
        """
        Update or insert price at timestamp
        Time: O(log n)
        """
        # Remove old price if exists
        if timestamp in self.price_by_timestamp:
            old_price = self.price_by_timestamp[timestamp]
            self._decrement_price(old_price)
        
        # Update
        self.price_by_timestamp[timestamp] = price
        self.price_count[price] = self.price_count.get(price, 0) + 1
    
    def get_max_commodity_price(self) -> Optional[int]:
        """
        Time: O(log n) with SortedDict
        """
        if not self.price_count:
            return None
        return self.price_count.peekitem(-1)[0]  # Last key (max)
    
    def get_min_commodity_price(self) -> Optional[int]:
        """
        Time: O(log n)
        """
        if not self.price_count:
            return None
        return self.price_count.peekitem(0)[0]  # First key (min)
    
    def get_price_at(self, timestamp: int) -> Optional[int]:
        """
        Time: O(1)
        """
        return self.price_by_timestamp.get(timestamp)
    
    def _decrement_price(self, price: int) -> None:
        if price not in self.price_count:
            return
        if self.price_count[price] == 1:
            del self.price_count[price]
        else:
            self.price_count[price] -= 1
```

---

## Solution 2: O(1) Max Price (With Variable Tracking)

```kotlin
class CommodityPriceTrackerOptimized {
    private val priceByTimestamp = mutableMapOf<Long, Int>()
    private val priceCount = sortedMapOf<Int, Int>()
    
    // Cache max price for O(1) access
    private var cachedMaxPrice: Int? = null
    private var maxPriceValid = false
    
    fun update(timestamp: Long, price: Int) {
        // Remove old price
        priceByTimestamp[timestamp]?.let { oldPrice ->
            decrementPrice(oldPrice)
            
            // Invalidate cache if we removed the max
            if (oldPrice == cachedMaxPrice) {
                maxPriceValid = false
            }
        }
        
        // Add new price
        priceByTimestamp[timestamp] = price
        priceCount[price] = priceCount.getOrDefault(price, 0) + 1
        
        // Update cached max if new price is >= current max
        if (!maxPriceValid || cachedMaxPrice == null || price >= cachedMaxPrice!!) {
            cachedMaxPrice = price
            maxPriceValid = true
        }
    }
    
    fun getMaxCommodityPrice(): Int? {
        if (!maxPriceValid && priceCount.isNotEmpty()) {
            cachedMaxPrice = priceCount.lastKey()
            maxPriceValid = true
        }
        return cachedMaxPrice
    }
    
    private fun decrementPrice(price: Int) {
        val count = priceCount[price] ?: return
        if (count == 1) {
            priceCount.remove(price)
        } else {
            priceCount[price] = count - 1
        }
    }
}
```

### Amortized O(1) with Lazy Deletion

```python
import heapq
from typing import Optional

class CommodityPriceTrackerO1:
    def __init__(self):
        self.price_by_timestamp = {}
        self.max_heap = []  # Max heap (store negative for max)
        self.min_heap = []  # Min heap
        
    def update(self, timestamp: int, price: int) -> None:
        """
        Time: O(log n) amortized
        """
        self.price_by_timestamp[timestamp] = price
        
        # Push to both heaps (lazy - we'll clean up on read)
        heapq.heappush(self.max_heap, (-price, timestamp))
        heapq.heappush(self.min_heap, (price, timestamp))
    
    def get_max_commodity_price(self) -> Optional[int]:
        """
        Time: O(1) amortized (with lazy deletion)
        """
        # Clean up stale entries
        while self.max_heap:
            price, timestamp = self.max_heap[0]
            price = -price  # Convert back from negative
            
            # Check if this is still valid
            if (timestamp in self.price_by_timestamp and 
                self.price_by_timestamp[timestamp] == price):
                return price
            
            heapq.heappop(self.max_heap)
        
        return None
    
    def get_min_commodity_price(self) -> Optional[int]:
        """
        Time: O(1) amortized
        """
        while self.min_heap:
            price, timestamp = self.min_heap[0]
            
            if (timestamp in self.price_by_timestamp and 
                self.price_by_timestamp[timestamp] == price):
                return price
            
            heapq.heappop(self.min_heap)
        
        return None
```

---

## Solution 3: Complete Implementation with All Features

```kotlin
import java.util.TreeMap
import java.util.PriorityQueue

class CommodityPriceSystem {
    
    // Core storage
    private val priceByTimestamp = mutableMapOf<Long, Int>()
    
    // For O(log n) min/max
    private val priceCount = TreeMap<Int, Int>()
    
    // For O(1) latest timestamp tracking
    private var latestTimestamp: Long? = null
    
    // For time-range queries (optional)
    private val timestampTree = TreeMap<Long, Int>()
    
    /**
     * Update price at timestamp
     * Time: O(log n)
     */
    fun update(timestamp: Long, price: Int) {
        // Handle existing timestamp
        val oldPrice = priceByTimestamp[timestamp]
        if (oldPrice != null) {
            decrementPrice(oldPrice)
        }
        
        // Update all data structures
        priceByTimestamp[timestamp] = price
        timestampTree[timestamp] = price
        incrementPrice(price)
        
        // Track latest timestamp
        if (latestTimestamp == null || timestamp > latestTimestamp!!) {
            latestTimestamp = timestamp
        }
    }
    
    /**
     * Get maximum price - O(log n)
     */
    fun getMaxPrice(): Int? = priceCount.lastKey()
    
    /**
     * Get minimum price - O(log n)
     */
    fun getMinPrice(): Int? = priceCount.firstKey()
    
    /**
     * Get current price (at latest timestamp) - O(1)
     */
    fun getCurrentPrice(): Int? {
        return latestTimestamp?.let { priceByTimestamp[it] }
    }
    
    /**
     * Get price at specific timestamp - O(1)
     */
    fun getPriceAt(timestamp: Long): Int? {
        return priceByTimestamp[timestamp]
    }
    
    /**
     * Get price at or before timestamp - O(log n)
     */
    fun getPriceAtOrBefore(timestamp: Long): Int? {
        return timestampTree.floorEntry(timestamp)?.value
    }
    
    /**
     * Get prices in time range - O(log n + k)
     */
    fun getPricesInRange(startTime: Long, endTime: Long): Map<Long, Int> {
        return timestampTree.subMap(startTime, true, endTime, true)
    }
    
    /**
     * Get max price in time range - O(k)
     */
    fun getMaxPriceInRange(startTime: Long, endTime: Long): Int? {
        return getPricesInRange(startTime, endTime).values.maxOrNull()
    }
    
    private fun incrementPrice(price: Int) {
        priceCount[price] = priceCount.getOrDefault(price, 0) + 1
    }
    
    private fun decrementPrice(price: Int) {
        val count = priceCount[price] ?: return
        if (count == 1) {
            priceCount.remove(price)
        } else {
            priceCount[price] = count - 1
        }
    }
}
```

```python
from sortedcontainers import SortedDict
from typing import Optional, Dict

class CommodityPriceSystem:
    def __init__(self):
        self.price_by_timestamp: Dict[int, int] = {}
        self.price_count = SortedDict()
        self.timestamp_tree = SortedDict()
        self.latest_timestamp: Optional[int] = None
    
    def update(self, timestamp: int, price: int) -> None:
        # Handle existing
        if timestamp in self.price_by_timestamp:
            old_price = self.price_by_timestamp[timestamp]
            self._decrement_price(old_price)
        
        # Update
        self.price_by_timestamp[timestamp] = price
        self.timestamp_tree[timestamp] = price
        self._increment_price(price)
        
        if self.latest_timestamp is None or timestamp > self.latest_timestamp:
            self.latest_timestamp = timestamp
    
    def get_max_price(self) -> Optional[int]:
        return self.price_count.peekitem(-1)[0] if self.price_count else None
    
    def get_min_price(self) -> Optional[int]:
        return self.price_count.peekitem(0)[0] if self.price_count else None
    
    def get_current_price(self) -> Optional[int]:
        if self.latest_timestamp is None:
            return None
        return self.price_by_timestamp.get(self.latest_timestamp)
    
    def get_price_at(self, timestamp: int) -> Optional[int]:
        return self.price_by_timestamp.get(timestamp)
    
    def get_price_at_or_before(self, timestamp: int) -> Optional[int]:
        idx = self.timestamp_tree.bisect_right(timestamp)
        if idx == 0:
            return None
        ts = self.timestamp_tree.peekitem(idx - 1)[0]
        return self.timestamp_tree[ts]
    
    def get_max_price_in_range(self, start: int, end: int) -> Optional[int]:
        prices = [
            self.timestamp_tree[ts] 
            for ts in self.timestamp_tree.irange(start, end)
        ]
        return max(prices) if prices else None
    
    def _increment_price(self, price: int) -> None:
        self.price_count[price] = self.price_count.get(price, 0) + 1
    
    def _decrement_price(self, price: int) -> None:
        if price not in self.price_count:
            return
        if self.price_count[price] == 1:
            del self.price_count[price]
        else:
            self.price_count[price] -= 1
```

---

## Complexity Summary

| Operation | TreeMap Solution | Heap Solution (Lazy) |
|-----------|-----------------|---------------------|
| update() | O(log n) | O(log n) |
| getMaxPrice() | O(log n) | O(1) amortized |
| getMinPrice() | O(log n) | O(1) amortized |
| getPriceAt() | O(1) | O(1) |
| Space | O(n) | O(n) |

---

## Comparison with Stock Price Problem

This problem is similar to LeetCode 2034 (Stock Price Fluctuation):

| Feature | Commodity Prices | Stock Price Fluctuation |
|---------|-----------------|------------------------|
| Out of order timestamps | ✓ | ✓ |
| Update existing | ✓ | ✓ |
| Get max | ✓ | ✓ |
| Get min | ✓ | ✓ |
| Get current (latest) | ✓ | ✓ |
| Time range queries | ✓ (extended) | ✗ |

The core solution is essentially the same - use `HashMap` for timestamp lookup and `TreeMap` (or sorted structure) for min/max tracking.

