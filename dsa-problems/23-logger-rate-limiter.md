# Logger Rate Limiter

## Problem Information
- **Difficulty**: Easy
- **Frequency**: 43.1%
- **Acceptance Rate**: 76.6%
- **Topics**: Hash Table, Design, Data Stream
- **LeetCode Link**: https://leetcode.com/problems/logger-rate-limiter

## Problem Description

Design a logger system that receives a stream of messages along with their timestamps. Each **unique** message should only be printed **at most every 10 seconds** (i.e. a message printed at timestamp `t` will prevent other identical messages from being printed until timestamp `t + 10`).

All messages will come in chronological order. Several messages may arrive at the same timestamp.

Implement the `Logger` class:
- `Logger()` Initializes the logger object.
- `bool shouldPrintMessage(int timestamp, string message)` Returns `true` if the message should be printed in the given timestamp, otherwise returns `false`.

## Examples

### Example 1:
```
Input
["Logger", "shouldPrintMessage", "shouldPrintMessage", "shouldPrintMessage", "shouldPrintMessage", "shouldPrintMessage", "shouldPrintMessage"]
[[], [1, "foo"], [2, "bar"], [3, "foo"], [8, "bar"], [10, "foo"], [11, "foo"]]
Output
[null, true, true, false, false, false, true]

Explanation
Logger logger = new Logger();
logger.shouldPrintMessage(1, "foo");  // return true, next allowed timestamp for "foo" is 1 + 10 = 11
logger.shouldPrintMessage(2, "bar");  // return true, next allowed timestamp for "bar" is 2 + 10 = 12
logger.shouldPrintMessage(3, "foo");  // return false, 3 < 11 (not enough time passed)
logger.shouldPrintMessage(8, "bar");  // return false, 8 < 12
logger.shouldPrintMessage(10, "foo"); // return false, 10 < 11
logger.shouldPrintMessage(11, "foo"); // return true, 11 >= 11, next allowed is 11 + 10 = 21
```

## Constraints

- `0 <= timestamp <= 10^9`
- Every timestamp will be passed in non-decreasing order (chronological order).
- `1 <= message.length <= 30`
- At most `10^4` calls will be made to `shouldPrintMessage`.

## Approach

### Key Insight:
- Store the next allowed timestamp for each message
- When a message comes, check if current timestamp >= next allowed timestamp

### Data Structure:
- Hash map: message → next allowed timestamp

## Solution

```python
class Logger:
    def __init__(self):
        self.message_timestamps = {}  # message -> next allowed timestamp
    
    def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
        if message not in self.message_timestamps:
            # New message, allow it
            self.message_timestamps[message] = timestamp + 10
            return True
        
        if timestamp >= self.message_timestamps[message]:
            # Enough time has passed, allow it
            self.message_timestamps[message] = timestamp + 10
            return True
        
        # Not enough time has passed
        return False
```

## Simplified Solution

```python
class Logger:
    def __init__(self):
        self.msg_time = {}  # message -> last printed timestamp
    
    def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
        if message not in self.msg_time or timestamp - self.msg_time[message] >= 10:
            self.msg_time[message] = timestamp
            return True
        return False
```

## Solution with Cleanup (Memory Optimization)

```python
from collections import deque

class Logger:
    """
    For streaming data, old messages may never be seen again.
    We can clean up old entries to save memory.
    """
    def __init__(self):
        self.msg_time = {}
        self.msg_queue = deque()  # (timestamp, message) pairs
    
    def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
        # Clean up messages older than 10 seconds
        while self.msg_queue and self.msg_queue[0][0] <= timestamp - 10:
            _, old_msg = self.msg_queue.popleft()
            # Only delete if this is the recorded timestamp
            if old_msg in self.msg_time and self.msg_time[old_msg] <= timestamp - 10:
                del self.msg_time[old_msg]
        
        if message not in self.msg_time:
            self.msg_time[message] = timestamp
            self.msg_queue.append((timestamp, message))
            return True
        
        return False
```

## Solution Using defaultdict

```python
from collections import defaultdict

class Logger:
    def __init__(self):
        # Default value -10 means any timestamp >= 0 will be allowed initially
        self.msg_time = defaultdict(lambda: -10)
    
    def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
        if timestamp >= self.msg_time[message] + 10:
            self.msg_time[message] = timestamp
            return True
        return False
```

## Thread-Safe Solution

```python
import threading

class Logger:
    def __init__(self):
        self.msg_time = {}
        self.lock = threading.Lock()
    
    def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
        with self.lock:
            if message not in self.msg_time or timestamp - self.msg_time[message] >= 10:
                self.msg_time[message] = timestamp
                return True
            return False
```

## Complexity Analysis

### Time Complexity:
- **shouldPrintMessage**: O(1) average (hash map operations)

### Space Complexity: O(n)
- n = number of unique messages
- With cleanup: O(m) where m = unique messages in last 10 seconds

## Visual Example

```
Timeline showing "foo" messages:

Time:  0   1   2   3   4   5   6   7   8   9  10  11  12  ...
       |   |   |   |   |   |   |   |   |   |   |   |   |
foo@1: |   ✓---|---|---|---|---|---|---|---|---|---|   |
                                                   ^
                                                allowed again at 11

foo@3:     |       ✗   (3 < 11, blocked)
foo@10:    |                               ✗       (10 < 11, blocked)
foo@11:    |                                   ✓---|---|--- (11 >= 11, allowed)
```

## Key Patterns & Techniques

1. **Hash Map for State Tracking**: Store per-message state
2. **Timestamp Comparison**: Check cooldown period
3. **Lazy Cleanup**: Only clean when necessary (memory optimization)
4. **Monotonic Timestamps**: Simplifies logic (no need to handle out-of-order)

## Storing "Next Allowed" vs "Last Printed"

```
Option 1: Store next allowed timestamp
- Check: timestamp >= next_allowed
- Update: next_allowed = timestamp + 10

Option 2: Store last printed timestamp
- Check: timestamp - last_printed >= 10
- Update: last_printed = timestamp

Both are equivalent, choose based on preference.
```

## Common Mistakes to Avoid

1. **Using `>` instead of `>=`**: At exactly timestamp 11, message should be allowed
2. **Not updating timestamp on allow**: Must record new timestamp when message is printed
3. **Off-by-one in cooldown**: Use 10, not 9 or 11
4. **Memory growth**: Consider cleanup for long-running systems

## Follow-up Questions

### What if timestamps are not monotonic?
Need to handle case where older timestamps arrive after newer ones.
```python
def shouldPrintMessage(self, timestamp: int, message: str) -> bool:
    if message in self.msg_time:
        last_time = self.msg_time[message]
        if abs(timestamp - last_time) < 10:
            return False
    self.msg_time[message] = timestamp
    return True
```

### What if we need to limit to N messages per 10 seconds?
Use a queue to track last N timestamps for each message.

## Edge Cases

1. First message for a given string → always allow
2. Same message at same timestamp → allow first, block rest
3. Message after exactly 10 seconds → allow (>= not >)
4. Very large timestamps → handle integer overflow if needed

## Related Problems

- [359. Logger Rate Limiter](https://leetcode.com/problems/logger-rate-limiter/) (This problem)
- [362. Design Hit Counter](https://leetcode.com/problems/design-hit-counter/)
- [933. Number of Recent Calls](https://leetcode.com/problems/number-of-recent-calls/)
- Rate Limiting in System Design

