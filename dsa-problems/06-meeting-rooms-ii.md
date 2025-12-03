# Meeting Rooms II

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 69.4%
- **Acceptance Rate**: 52.1%
- **Topics**: Array, Two Pointers, Greedy, Sorting, Heap (Priority Queue), Prefix Sum
- **LeetCode Link**: https://leetcode.com/problems/meeting-rooms-ii

## Problem Description

Given an array of meeting time intervals `intervals` where `intervals[i] = [starti, endi]`, return the **minimum number of conference rooms** required.

## Examples

### Example 1:
```
Input: intervals = [[0,30],[5,10],[15,20]]
Output: 2
Explanation: 
Meeting 1: [0, 30] needs a room from time 0 to 30
Meeting 2: [5, 10] starts at 5, meeting 1 is still running, needs another room
Meeting 3: [15, 20] starts at 15, meeting 1 is still running but meeting 2 has ended
So we need 2 rooms maximum at any point
```

### Example 2:
```
Input: intervals = [[7,10],[2,4]]
Output: 1
Explanation: The meetings don't overlap, so 1 room is sufficient.
```

## Constraints

- `1 <= intervals.length <= 10^4`
- `0 <= starti < endi <= 10^6`

## Approach

### Approach 1: Min Heap (Priority Queue)
- Sort meetings by start time
- Use a min heap to track end times of ongoing meetings
- For each meeting, if the earliest ending meeting ends before this one starts, reuse that room
- Otherwise, allocate a new room

### Approach 2: Two Pointers (Chronological Ordering)
- Separate start and end times
- Sort both arrays
- Sweep through time, counting rooms needed

### Approach 3: Line Sweep
- Mark +1 at meeting start, -1 at meeting end
- Find maximum overlapping meetings at any point

## Solution 1: Min Heap Approach

```python
import heapq
from typing import List

class Solution:
    def minMeetingRooms(self, intervals: List[List[int]]) -> int:
        if not intervals:
            return 0
        
        # Sort by start time
        intervals.sort(key=lambda x: x[0])
        
        # Min heap to track end times of meetings in rooms
        # Each element represents a room's current meeting end time
        rooms = []
        
        for start, end in intervals:
            # If the earliest ending meeting has ended, reuse that room
            if rooms and rooms[0] <= start:
                heapq.heappop(rooms)
            
            # Add current meeting's end time to heap
            heapq.heappush(rooms, end)
        
        # Number of rooms is the heap size
        return len(rooms)
```

## Solution 2: Two Pointers (Chronological Ordering)

```python
from typing import List

class Solution:
    def minMeetingRooms(self, intervals: List[List[int]]) -> int:
        if not intervals:
            return 0
        
        # Separate and sort start/end times
        starts = sorted([i[0] for i in intervals])
        ends = sorted([i[1] for i in intervals])
        
        rooms_needed = 0
        end_ptr = 0
        
        for start in starts:
            # A meeting is starting
            if start < ends[end_ptr]:
                # No meeting has ended yet, need a new room
                rooms_needed += 1
            else:
                # A meeting has ended, reuse that room
                end_ptr += 1
        
        return rooms_needed
```

## Solution 3: Line Sweep Algorithm

```python
from typing import List
from collections import defaultdict

class Solution:
    def minMeetingRooms(self, intervals: List[List[int]]) -> int:
        events = defaultdict(int)
        
        for start, end in intervals:
            events[start] += 1  # Meeting starts, need a room
            events[end] -= 1    # Meeting ends, free a room
        
        rooms_needed = 0
        max_rooms = 0
        
        # Process events in chronological order
        for time in sorted(events.keys()):
            rooms_needed += events[time]
            max_rooms = max(max_rooms, rooms_needed)
        
        return max_rooms
```

## Solution 4: Using Sorted Events with Type

```python
from typing import List

class Solution:
    def minMeetingRooms(self, intervals: List[List[int]]) -> int:
        events = []
        
        for start, end in intervals:
            events.append((start, 1))   # 1 = meeting starts
            events.append((end, -1))    # -1 = meeting ends
        
        # Sort by time; if same time, process ends (-1) before starts (1)
        # This handles the case where one meeting ends exactly when another starts
        events.sort(key=lambda x: (x[0], x[1]))
        
        rooms_needed = 0
        max_rooms = 0
        
        for time, event_type in events:
            rooms_needed += event_type
            max_rooms = max(max_rooms, rooms_needed)
        
        return max_rooms
```

## Complexity Analysis

### Min Heap Approach:
- **Time**: O(n log n)
  - Sorting: O(n log n)
  - Each heap operation: O(log n), done n times
- **Space**: O(n) for the heap

### Two Pointers Approach:
- **Time**: O(n log n) for sorting
- **Space**: O(n) for the sorted arrays

### Line Sweep Approach:
- **Time**: O(n log n) for sorting events
- **Space**: O(n) for storing events

## Visual Example

```
Time:    0   5  10  15  20  25  30
         |---|---|---|---|---|---|
Meeting1: [=====================================]
Meeting2:     [====]
Meeting3:              [========]

Timeline with room assignment:
Room 1: [0--------30]
Room 2:   [5-10]  [15-20]

Max rooms needed at any time: 2
```

## Key Patterns & Techniques

1. **Interval Scheduling**: Classic pattern for meeting room problems
2. **Min Heap for Tracking End Times**: Efficiently find the earliest ending meeting
3. **Two Pointers**: Sweep through sorted start/end times
4. **Line Sweep / Event Processing**: Track changes at discrete time points

## Why Two Pointers Works

```
Sorted starts: [0, 5, 15]
Sorted ends:   [10, 20, 30]

Step 1: start=0, end_ptr points to 10
        0 < 10? Yes, need new room (rooms=1)
        
Step 2: start=5, end_ptr points to 10
        5 < 10? Yes, need new room (rooms=2)
        
Step 3: start=15, end_ptr points to 10
        15 < 10? No, a meeting ended, reuse room (end_ptr++)
        
Final: 2 rooms needed
```

## Edge Cases

1. Empty intervals → return 0
2. Single meeting → return 1
3. All meetings overlap → return n
4. No meetings overlap → return 1
5. Meeting ends exactly when another starts → can reuse room

## Common Mistakes to Avoid

1. Not handling the case where meeting end equals next meeting start
2. Forgetting to sort intervals
3. Off-by-one errors in two pointer approach
4. Not considering empty input

## Related Problems

- [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/)
- [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/)
- [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)
- [1094. Car Pooling](https://leetcode.com/problems/car-pooling/)

