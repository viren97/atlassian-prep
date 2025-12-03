# Count Vowel Strings in Ranges

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 67.9%
- **Topics**: Array, String, Prefix Sum
- **LeetCode Link**: https://leetcode.com/problems/count-vowel-strings-in-ranges

## Problem Description

You are given a **0-indexed** array of strings `words` and a 2D array of integers `queries`.

Each query `queries[i] = [li, ri]` asks us to find the number of strings present in the range `li` to `ri` (both **inclusive**) of `words` that start **and** end with a vowel.

Return an array `ans` of size `queries.length`, where `ans[i]` is the answer to the `i-th` query.

**Note:** The vowel letters are `'a'`, `'e'`, `'i'`, `'o'`, and `'u'`.

## Examples

### Example 1:
```
Input: words = ["aba","bcb","ece","aa","e"], queries = [[0,2],[1,4],[1,1]]
Output: [2,3,0]
Explanation:
The strings starting and ending with a vowel are "aba", "ece", "aa" and "e".
- Query [0,2]: "aba" and "ece" → 2
- Query [1,4]: "ece", "aa", "e" → 3
- Query [1,1]: "bcb" → 0
```

### Example 2:
```
Input: words = ["a","e","i"], queries = [[0,2],[0,1],[2,2]]
Output: [3,2,1]
Explanation: Every string satisfies the conditions, so we return [3,2,1].
```

## Constraints

- `1 <= words.length <= 10^5`
- `1 <= words[i].length <= 40`
- `words[i]` consists only of lowercase English letters.
- `sum(words[i].length) <= 3 * 10^5`
- `1 <= queries.length <= 10^5`
- `0 <= li <= ri < words.length`

## Approach

### Key Insight:
- For each query, we need count of valid words in a range
- Multiple queries on same data → use **Prefix Sum**
- Precompute prefix sums of valid words, then answer each query in O(1)

### Algorithm:
1. Create a prefix sum array where `prefix[i]` = count of valid words in `words[0..i-1]`
2. For each query `[l, r]`, answer = `prefix[r+1] - prefix[l]`

## Solution

```python
from typing import List

class Solution:
    def vowelStrings(self, words: List[str], queries: List[List[int]]) -> List[int]:
        vowels = {'a', 'e', 'i', 'o', 'u'}
        
        def is_valid(word):
            return word[0] in vowels and word[-1] in vowels
        
        n = len(words)
        # prefix[i] = count of valid words in words[0..i-1]
        prefix = [0] * (n + 1)
        
        for i in range(n):
            prefix[i + 1] = prefix[i] + (1 if is_valid(words[i]) else 0)
        
        # Answer queries
        result = []
        for l, r in queries:
            # Count in range [l, r] = prefix[r+1] - prefix[l]
            result.append(prefix[r + 1] - prefix[l])
        
        return result
```

## Solution with List Comprehension

```python
from typing import List

class Solution:
    def vowelStrings(self, words: List[str], queries: List[List[int]]) -> List[int]:
        vowels = set('aeiou')
        
        # Build prefix sum array
        n = len(words)
        prefix = [0] * (n + 1)
        
        for i, word in enumerate(words):
            prefix[i + 1] = prefix[i] + (word[0] in vowels and word[-1] in vowels)
        
        return [prefix[r + 1] - prefix[l] for l, r in queries]
```

## Solution Using itertools.accumulate

```python
from typing import List
from itertools import accumulate

class Solution:
    def vowelStrings(self, words: List[str], queries: List[List[int]]) -> List[int]:
        vowels = set('aeiou')
        
        # Create boolean array: 1 if valid, 0 if not
        valid = [1 if w[0] in vowels and w[-1] in vowels else 0 for w in words]
        
        # Prefix sum with leading 0
        prefix = [0] + list(accumulate(valid))
        
        return [prefix[r + 1] - prefix[l] for l, r in queries]
```

## Brute Force Solution (For Comparison)

```python
from typing import List

class Solution:
    def vowelStrings(self, words: List[str], queries: List[List[int]]) -> List[int]:
        """O(n * q) - Too slow for constraints"""
        vowels = set('aeiou')
        
        def is_valid(word):
            return word[0] in vowels and word[-1] in vowels
        
        result = []
        for l, r in queries:
            count = sum(1 for i in range(l, r + 1) if is_valid(words[i]))
            result.append(count)
        
        return result
```

## Complexity Analysis

### Prefix Sum Solution:
- **Time**: O(n + q)
  - Build prefix array: O(n)
  - Answer q queries: O(q)
- **Space**: O(n) for prefix array

### Brute Force:
- **Time**: O(n × q) - each query scans range
- **Space**: O(1)

## Visual Example

```
words = ["aba", "bcb", "ece", "aa", "e"]
          ✓      ✗      ✓     ✓    ✓
valid = [  1,     0,     1,    1,   1]

Prefix sum:
index:    0   1   2   3   4   5
prefix = [0,  1,  1,  2,  3,  4]
              │   │   │   │   │
              └───┴───┴───┴───┴── cumulative count

Query [0, 2]:
- Count = prefix[3] - prefix[0] = 2 - 0 = 2 ✓

Query [1, 4]:
- Count = prefix[5] - prefix[1] = 4 - 1 = 3 ✓

Query [1, 1]:
- Count = prefix[2] - prefix[1] = 1 - 1 = 0 ✓
```

## Key Patterns & Techniques

1. **Prefix Sum for Range Queries**: Precompute cumulative sums
2. **Range Sum Formula**: `sum(l, r) = prefix[r+1] - prefix[l]`
3. **Boolean to Integer**: Use 1/0 for valid/invalid in sum
4. **Set Membership**: O(1) check for vowels

## Why Prefix Sum Works

```
For array: [a₀, a₁, a₂, a₃, a₄]
prefix:    [0, a₀, a₀+a₁, a₀+a₁+a₂, a₀+a₁+a₂+a₃, a₀+a₁+a₂+a₃+a₄]
           [0,  1,   2,      3,           4,              5]

Sum from index l to r:
= a_l + a_{l+1} + ... + a_r
= prefix[r+1] - prefix[l]

Example: sum from index 1 to 3
= a₁ + a₂ + a₃
= (a₀ + a₁ + a₂ + a₃) - (a₀)
= prefix[4] - prefix[1]
```

## Common Mistakes to Avoid

1. **Off-by-one errors**: Remember `prefix[r+1] - prefix[l]`, not `prefix[r] - prefix[l-1]`
2. **Empty word handling**: Check `word[0]` and `word[-1]` safely
3. **Using wrong vowel check**: Make sure to check BOTH first AND last character
4. **Index out of bounds**: Prefix array needs size `n + 1`

## Edge Cases

1. Single character words: `"a"` is valid (first = last = vowel)
2. Query for single element: `[i, i]` should work correctly
3. All words valid or all invalid
4. Query spanning entire array: `[0, n-1]`

## Related Problems

- [303. Range Sum Query - Immutable](https://leetcode.com/problems/range-sum-query-immutable/)
- [304. Range Sum Query 2D - Immutable](https://leetcode.com/problems/range-sum-query-2d-immutable/)
- [238. Product of Array Except Self](https://leetcode.com/problems/product-of-array-except-self/)
- [560. Subarray Sum Equals K](https://leetcode.com/problems/subarray-sum-equals-k/)

