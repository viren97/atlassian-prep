# Sliding Window & Two Pointers - Complete Guide

---

## Part 1: Two Pointers

### Pattern 1: Opposite Direction (Converging)

**Use case**: Sorted arrays, palindromes, pair finding

```python
# Two Sum in Sorted Array
def twoSum(nums, target):
    left, right = 0, len(nums) - 1
    
    while left < right:
        curr_sum = nums[left] + nums[right]
        
        if curr_sum == target:
            return [left, right]
        elif curr_sum < target:
            left += 1
        else:
            right -= 1
    
    return [-1, -1]


# Valid Palindrome
def isPalindrome(s):
    left, right = 0, len(s) - 1
    
    while left < right:
        while left < right and not s[left].isalnum():
            left += 1
        while left < right and not s[right].isalnum():
            right -= 1
        
        if s[left].lower() != s[right].lower():
            return False
        
        left += 1
        right -= 1
    
    return True


# Container With Most Water
def maxArea(height):
    left, right = 0, len(height) - 1
    max_water = 0
    
    while left < right:
        width = right - left
        h = min(height[left], height[right])
        max_water = max(max_water, width * h)
        
        # Move the shorter line
        if height[left] < height[right]:
            left += 1
        else:
            right -= 1
    
    return max_water
```

### Pattern 2: Three Sum / K Sum

```python
def threeSum(nums):
    nums.sort()
    result = []
    
    for i in range(len(nums) - 2):
        # Skip duplicates for first element
        if i > 0 and nums[i] == nums[i - 1]:
            continue
        
        left, right = i + 1, len(nums) - 1
        
        while left < right:
            total = nums[i] + nums[left] + nums[right]
            
            if total == 0:
                result.append([nums[i], nums[left], nums[right]])
                
                # Skip duplicates
                while left < right and nums[left] == nums[left + 1]:
                    left += 1
                while left < right and nums[right] == nums[right - 1]:
                    right -= 1
                
                left += 1
                right -= 1
            elif total < 0:
                left += 1
            else:
                right -= 1
    
    return result


# 3Sum Closest
def threeSumClosest(nums, target):
    nums.sort()
    closest = float('inf')
    
    for i in range(len(nums) - 2):
        left, right = i + 1, len(nums) - 1
        
        while left < right:
            total = nums[i] + nums[left] + nums[right]
            
            if abs(total - target) < abs(closest - target):
                closest = total
            
            if total < target:
                left += 1
            elif total > target:
                right -= 1
            else:
                return target
    
    return closest
```

### Pattern 3: Same Direction (Fast & Slow)

**Use case**: Linked lists, remove duplicates, partition

```python
# Remove Duplicates from Sorted Array
def removeDuplicates(nums):
    if not nums:
        return 0
    
    slow = 0
    
    for fast in range(1, len(nums)):
        if nums[fast] != nums[slow]:
            slow += 1
            nums[slow] = nums[fast]
    
    return slow + 1


# Remove Element
def removeElement(nums, val):
    slow = 0
    
    for fast in range(len(nums)):
        if nums[fast] != val:
            nums[slow] = nums[fast]
            slow += 1
    
    return slow


# Move Zeroes
def moveZeroes(nums):
    slow = 0
    
    for fast in range(len(nums)):
        if nums[fast] != 0:
            nums[slow], nums[fast] = nums[fast], nums[slow]
            slow += 1


# Linked List Cycle Detection (Floyd's)
def hasCycle(head):
    slow = fast = head
    
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
        
        if slow == fast:
            return True
    
    return False


# Find Cycle Start
def detectCycle(head):
    slow = fast = head
    
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
        
        if slow == fast:
            # Reset one pointer to head
            slow = head
            while slow != fast:
                slow = slow.next
                fast = fast.next
            return slow
    
    return None


# Middle of Linked List
def middleNode(head):
    slow = fast = head
    
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
    
    return slow
```

### Pattern 4: Merge Two Sorted

```python
# Merge Sorted Arrays
def merge(nums1, m, nums2, n):
    # Start from end to avoid overwriting
    p1, p2, p = m - 1, n - 1, m + n - 1
    
    while p1 >= 0 and p2 >= 0:
        if nums1[p1] > nums2[p2]:
            nums1[p] = nums1[p1]
            p1 -= 1
        else:
            nums1[p] = nums2[p2]
            p2 -= 1
        p -= 1
    
    # Copy remaining from nums2
    while p2 >= 0:
        nums1[p] = nums2[p2]
        p2 -= 1
        p -= 1


# Merge Two Sorted Lists
def mergeTwoLists(l1, l2):
    dummy = ListNode(0)
    curr = dummy
    
    while l1 and l2:
        if l1.val < l2.val:
            curr.next = l1
            l1 = l1.next
        else:
            curr.next = l2
            l2 = l2.next
        curr = curr.next
    
    curr.next = l1 if l1 else l2
    return dummy.next
```

### Pattern 5: Partition / Dutch National Flag

```python
# Sort Colors (Dutch National Flag)
def sortColors(nums):
    low, mid, high = 0, 0, len(nums) - 1
    
    while mid <= high:
        if nums[mid] == 0:
            nums[low], nums[mid] = nums[mid], nums[low]
            low += 1
            mid += 1
        elif nums[mid] == 1:
            mid += 1
        else:  # nums[mid] == 2
            nums[mid], nums[high] = nums[high], nums[mid]
            high -= 1


# Partition Array
def partition(nums, pivot):
    left, right = 0, len(nums) - 1
    i = 0
    
    while i <= right:
        if nums[i] < pivot:
            nums[left], nums[i] = nums[i], nums[left]
            left += 1
            i += 1
        elif nums[i] > pivot:
            nums[i], nums[right] = nums[right], nums[i]
            right -= 1
        else:
            i += 1
```

---

## Part 2: Sliding Window

### Fixed Size Window

```python
# Max Sum of Subarray of Size K
def maxSumSubarray(nums, k):
    if len(nums) < k:
        return 0
    
    # Initial window
    window_sum = sum(nums[:k])
    max_sum = window_sum
    
    # Slide the window
    for i in range(k, len(nums)):
        window_sum += nums[i] - nums[i - k]
        max_sum = max(max_sum, window_sum)
    
    return max_sum


# Find All Anagrams in String
def findAnagrams(s, p):
    from collections import Counter
    
    result = []
    p_count = Counter(p)
    window = Counter()
    
    for i, char in enumerate(s):
        window[char] += 1
        
        # Remove leftmost if window too big
        if i >= len(p):
            left_char = s[i - len(p)]
            window[left_char] -= 1
            if window[left_char] == 0:
                del window[left_char]
        
        if window == p_count:
            result.append(i - len(p) + 1)
    
    return result
```

### Variable Size Window - Shrinking

**Template**: Expand right, shrink left when invalid

```python
# Minimum Window Substring
def minWindow(s, t):
    from collections import Counter
    
    need = Counter(t)
    window = Counter()
    
    left = 0
    formed = 0
    required = len(need)
    
    min_len = float('inf')
    result = ""
    
    for right, char in enumerate(s):
        window[char] += 1
        
        if char in need and window[char] == need[char]:
            formed += 1
        
        # Shrink window while valid
        while formed == required:
            # Update result
            if right - left + 1 < min_len:
                min_len = right - left + 1
                result = s[left:right + 1]
            
            # Shrink from left
            left_char = s[left]
            window[left_char] -= 1
            
            if left_char in need and window[left_char] < need[left_char]:
                formed -= 1
            
            left += 1
    
    return result


# Minimum Size Subarray Sum (sum >= target)
def minSubArrayLen(target, nums):
    left = 0
    curr_sum = 0
    min_len = float('inf')
    
    for right in range(len(nums)):
        curr_sum += nums[right]
        
        while curr_sum >= target:
            min_len = min(min_len, right - left + 1)
            curr_sum -= nums[left]
            left += 1
    
    return min_len if min_len != float('inf') else 0
```

### Variable Size Window - Maximum Length

**Template**: Expand right, shrink left when invalid, track max when valid

```python
# Longest Substring Without Repeating Characters
def lengthOfLongestSubstring(s):
    char_index = {}
    left = 0
    max_len = 0
    
    for right, char in enumerate(s):
        if char in char_index and char_index[char] >= left:
            left = char_index[char] + 1
        
        char_index[char] = right
        max_len = max(max_len, right - left + 1)
    
    return max_len


# Longest Substring with At Most K Distinct Characters
def lengthOfLongestSubstringKDistinct(s, k):
    from collections import Counter
    
    window = Counter()
    left = 0
    max_len = 0
    
    for right, char in enumerate(s):
        window[char] += 1
        
        while len(window) > k:
            left_char = s[left]
            window[left_char] -= 1
            if window[left_char] == 0:
                del window[left_char]
            left += 1
        
        max_len = max(max_len, right - left + 1)
    
    return max_len


# Longest Repeating Character Replacement
def characterReplacement(s, k):
    count = {}
    left = 0
    max_count = 0  # Max frequency of any char in window
    max_len = 0
    
    for right, char in enumerate(s):
        count[char] = count.get(char, 0) + 1
        max_count = max(max_count, count[char])
        
        # Window is invalid if (length - max_count) > k
        while (right - left + 1) - max_count > k:
            count[s[left]] -= 1
            left += 1
        
        max_len = max(max_len, right - left + 1)
    
    return max_len


# Max Consecutive Ones III (flip at most k zeros)
def longestOnes(nums, k):
    left = 0
    zero_count = 0
    max_len = 0
    
    for right in range(len(nums)):
        if nums[right] == 0:
            zero_count += 1
        
        while zero_count > k:
            if nums[left] == 0:
                zero_count -= 1
            left += 1
        
        max_len = max(max_len, right - left + 1)
    
    return max_len
```

### Sliding Window Maximum (Monotonic Deque)

```python
from collections import deque

def maxSlidingWindow(nums, k):
    dq = deque()  # Store indices
    result = []
    
    for i in range(len(nums)):
        # Remove indices outside window
        while dq and dq[0] <= i - k:
            dq.popleft()
        
        # Remove smaller elements (they're useless)
        while dq and nums[dq[-1]] < nums[i]:
            dq.pop()
        
        dq.append(i)
        
        # Add to result once window is full
        if i >= k - 1:
            result.append(nums[dq[0]])
    
    return result
```

---

## Quick Reference Templates

### Two Pointers - Opposite Direction
```python
left, right = 0, len(arr) - 1
while left < right:
    # Process arr[left] and arr[right]
    if condition:
        left += 1
    else:
        right -= 1
```

### Two Pointers - Same Direction
```python
slow = 0
for fast in range(len(arr)):
    if condition:
        # Process or swap
        slow += 1
```

### Fixed Window
```python
# Initialize window of size k
for i in range(k, len(arr)):
    # Add arr[i], remove arr[i-k]
    # Update result
```

### Variable Window (Shrinking)
```python
left = 0
for right in range(len(arr)):
    # Expand: add arr[right]
    while invalid:
        # Shrink: remove arr[left]
        left += 1
    # Update result (for minimum)
```

### Variable Window (Maximum Length)
```python
left = 0
for right in range(len(arr)):
    # Expand: add arr[right]
    while invalid:
        # Shrink: remove arr[left]
        left += 1
    # Update result = max(result, right - left + 1)
```

---

## When to Use Which?

| Problem Type | Technique |
|-------------|-----------|
| Find pair with sum | Two pointers (sorted) |
| Check palindrome | Two pointers (opposite) |
| Remove duplicates | Two pointers (same dir) |
| Linked list cycle | Fast & slow pointers |
| Find middle | Fast & slow pointers |
| Sum of k elements | Fixed window |
| Min subarray with sum | Variable window (shrink) |
| Max substring with condition | Variable window (max len) |
| Window max/min | Monotonic deque |

## Common Mistakes

1. **Off-by-one**: Check `left < right` vs `left <= right`
2. **Duplicate handling**: Skip duplicates after finding answer
3. **Window size**: Remember `i >= k - 1` for results
4. **Counter cleanup**: Delete keys when count becomes 0
5. **Shrink condition**: Shrink while invalid, not if invalid

