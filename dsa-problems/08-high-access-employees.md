# High-Access Employees

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 60.5%
- **Acceptance Rate**: 46.1%
- **Topics**: Array, Hash Table, String, Sorting
- **LeetCode Link**: https://leetcode.com/problems/high-access-employees

## Problem Description

You are given a 2D **0-indexed** array of strings, `access_times`, with size `n`. For each `i` where `0 <= i <= n - 1`, `access_times[i][0]` represents the name of an employee, and `access_times[i][1]` represents the access time of that employee. All entries in `access_times` are within the same day.

The access time is represented as **four digits** using a **24-hour** time format, for example, `"0800"` or `"2250"`.

An employee is said to be **high-access** if they have accessed the system **three or more** times within a **one-hour period**.

Times with exactly one hour of difference are **not** considered part of the same one-hour period. For example, `"0815"` and `"0915"` are not part of the same one-hour period.

Access times at the start and end of the day are **not** counted within the same one-hour period. For example, `"0005"` and `"2350"` are not part of the same one-hour period.

Return a list that contains the names of **high-access** employees with any order you want.

## Examples

### Example 1:
```
Input: access_times = [["a","0549"],["b","0457"],["a","0532"],["a","0621"],["b","0540"]]
Output: ["a"]
Explanation: 
"a" has three access times in the one-hour period of [05:32, 06:31] which are 05:32, 05:49, and 06:21.
But "b" only has two access times in the one-hour period, so "b" is not in the answer.
```

### Example 2:
```
Input: access_times = [["d","0002"],["c","0808"],["c","0829"],["e","0215"],["d","1508"],["d","1444"],["d","1410"],["c","0809"]]
Output: ["c","d"]
Explanation: 
"c" has three access times in the one-hour period of [08:08, 09:07] which are 08:08, 08:09, and 08:29.
"d" has three access times in the one-hour period of [14:10, 15:09] which are 14:10, 14:44, and 15:08.
```

### Example 3:
```
Input: access_times = [["cd","1025"],["ab","1025"],["cd","1046"],["cd","1055"],["ab","1124"],["ab","1120"]]
Output: ["ab","cd"]
Explanation: 
"ab" has three access times in the one-hour period of [10:25, 11:24] which are 10:25, 11:20, and 11:24.
"cd" has three access times in the one-hour period of [10:25, 11:24] which are 10:25, 10:46, and 10:55.
```

## Constraints

- `1 <= access_times.length <= 100`
- `access_times[i].length == 2`
- `1 <= access_times[i][0].length <= 10`
- `access_times[i][0]` consists only of English small letters.
- `access_times[i][1].length == 4`
- `access_times[i][1]` is in 24-hour time format.
- `access_times[i][1]` consists only of `'0'` to `'9'`.

## Approach

### Key Insights:
1. Group access times by employee
2. For each employee, sort their access times
3. Check if any 3 consecutive times fall within a one-hour window
4. "Within one hour" means `time[i+2] - time[i] < 60 minutes` (exclusive)

### Algorithm:
1. Build a map: employee → list of access times (converted to minutes)
2. Sort each employee's access times
3. Use sliding window: check if any 3 consecutive times span < 60 minutes
4. Collect employees that qualify as "high-access"

## Solution

```python
from collections import defaultdict
from typing import List

class Solution:
    def findHighAccessEmployees(self, access_times: List[List[str]]) -> List[str]:
        # Group times by employee
        employee_times = defaultdict(list)
        
        for name, time in access_times:
            # Convert time string to minutes since midnight
            minutes = int(time[:2]) * 60 + int(time[2:])
            employee_times[name].append(minutes)
        
        high_access = []
        
        for name, times in employee_times.items():
            # Sort access times
            times.sort()
            
            # Check for 3+ accesses within any one-hour window
            for i in range(len(times) - 2):
                # Check if times[i] and times[i+2] are within one hour (exclusive)
                # "Within one hour" means difference < 60, not <= 60
                if times[i + 2] - times[i] < 60:
                    high_access.append(name)
                    break
        
        return high_access
```

## Alternative Solution (Using Sliding Window explicitly)

```python
from collections import defaultdict
from typing import List

class Solution:
    def findHighAccessEmployees(self, access_times: List[List[str]]) -> List[str]:
        def time_to_minutes(time_str: str) -> int:
            return int(time_str[:2]) * 60 + int(time_str[2:])
        
        # Group by employee
        employee_times = defaultdict(list)
        for name, time in access_times:
            employee_times[name].append(time_to_minutes(time))
        
        result = []
        
        for name, times in employee_times.items():
            times.sort()
            n = len(times)
            
            if n < 3:
                continue
            
            # Sliding window approach
            left = 0
            for right in range(n):
                # Shrink window if outside one-hour range
                while times[right] - times[left] >= 60:
                    left += 1
                
                # If window has 3+ elements, it's high-access
                if right - left + 1 >= 3:
                    result.append(name)
                    break
        
        return result
```

## Solution with Detailed Comments

```python
from collections import defaultdict
from typing import List

class Solution:
    def findHighAccessEmployees(self, access_times: List[List[str]]) -> List[str]:
        """
        A one-hour period is defined as [T, T+60) - inclusive start, exclusive end.
        So times at T and T+60 are NOT in the same period.
        
        For 3 times to be in a one-hour period:
        - Sort them: t1, t2, t3
        - Need: t3 - t1 < 60 (strictly less than)
        """
        
        # Step 1: Group access times by employee
        employee_times = defaultdict(list)
        
        for name, time_str in access_times:
            # Convert "HHMM" to minutes since midnight
            hours = int(time_str[0:2])
            mins = int(time_str[2:4])
            total_minutes = hours * 60 + mins
            employee_times[name].append(total_minutes)
        
        # Step 2: Find high-access employees
        high_access = []
        
        for name, times in employee_times.items():
            if len(times) < 3:
                continue  # Can't have 3+ accesses
            
            # Sort times
            times.sort()
            
            # Step 3: Check consecutive triplets
            # If sorted times[i], times[i+1], times[i+2] span < 60 minutes,
            # then all 3 are within a one-hour window
            for i in range(len(times) - 2):
                if times[i + 2] - times[i] < 60:
                    high_access.append(name)
                    break  # Found, no need to check more
        
        return high_access
```

## Complexity Analysis

### Time Complexity: O(n log n)
- Grouping: O(n)
- For each employee with k times: O(k log k) for sorting
- Total: O(n + Σk_i log k_i) = O(n log n) in worst case

### Space Complexity: O(n)
- Hash map stores all access times

## Key Patterns & Techniques

1. **Grouping with Hash Map**: Organize data by key (employee name)
2. **Sorting for Ordered Analysis**: Sort times to check consecutive elements
3. **Sliding Window**: Efficient way to find elements within a range
4. **Time Conversion**: Convert time strings to numeric values for comparison

## Visual Example

```
Employee "a" times: ["0549", "0532", "0621"]
Convert to minutes: [349, 332, 381]
Sorted: [332, 349, 381]

Check triplet (332, 349, 381):
- 381 - 332 = 49 < 60 ✓
- All three within one hour!

One-hour window starting at 332 (05:32):
[332, 392) = [05:32, 06:32)
Times in window: 332 (05:32), 349 (05:49), 381 (06:21)
Count = 3 → High-access!
```

## Edge Cases

1. Employee with < 3 accesses → Not high-access
2. Exactly 60 minutes difference → NOT within same period (< 60, not <= 60)
3. Multiple employees qualifying
4. Times at midnight boundaries

## Common Mistakes to Avoid

1. **Using <= 60 instead of < 60**: The problem says times exactly one hour apart are NOT in the same period
2. **Not sorting the times**: Must sort to check consecutive triplets
3. **Checking all pairs instead of triplets**: Only need to check if 3+ times are within an hour
4. **Time conversion errors**: Be careful with "HHMM" format

## Why Check Only Consecutive Triplets?

After sorting, if times[i+2] - times[i] < 60, then:
- times[i+1] is between times[i] and times[i+2]
- So times[i+1] - times[i] < 60 and times[i+2] - times[i+1] < 60
- All three are within the same one-hour window

If times[i+2] - times[i] >= 60, no window starting at or after times[i] can include both times[i] and times[i+2].

## Related Problems

- [220. Contains Duplicate III](https://leetcode.com/problems/contains-duplicate-iii/)
- [239. Sliding Window Maximum](https://leetcode.com/problems/sliding-window-maximum/)
- [1438. Longest Continuous Subarray With Absolute Diff Less Than or Equal to Limit](https://leetcode.com/problems/longest-continuous-subarray-with-absolute-diff-less-than-or-equal-to-limit/)

