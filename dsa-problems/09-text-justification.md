# Text Justification

## Problem Information
- **Difficulty**: Hard
- **Frequency**: 60.5%
- **Acceptance Rate**: 48.1%
- **Topics**: Array, String, Simulation
- **LeetCode Link**: https://leetcode.com/problems/text-justification

## Problem Description

Given an array of strings `words` and a width `maxWidth`, format the text such that each line has exactly `maxWidth` characters and is fully (left and right) justified.

You should pack your words in a greedy approach; that is, pack as many words as you can in each line. Pad extra spaces `' '` when necessary so that each line has exactly `maxWidth` characters.

Extra spaces between words should be distributed as evenly as possible. If the number of spaces on a line does not divide evenly between words, the empty slots on the left will be assigned more spaces than the slots on the right.

For the last line of text, it should be left justified, and no extra space is inserted between words.

**Note:**
- A word is defined as a character sequence consisting of non-space characters only.
- Each word's length is guaranteed to be greater than 0 and not exceed `maxWidth`.
- The input array `words` contains at least one word.

## Examples

### Example 1:
```
Input: words = ["This", "is", "an", "example", "of", "text", "justification."], maxWidth = 16
Output:
[
   "This    is    an",
   "example  of text",
   "justification.  "
]
```

### Example 2:
```
Input: words = ["What","must","be","acknowledgment","shall","be"], maxWidth = 16
Output:
[
  "What   must   be",
  "acknowledgment  ",
  "shall be        "
]
Explanation: Note that the last line is "shall be    " instead of "shall     be", because the last line must be left-justified instead of fully-justified.
Also, note that the second line is "acknowledgment  " with 2 spaces at the end (to make it 16 chars).
```

### Example 3:
```
Input: words = ["Science","is","what","we","understand","well","enough","to","explain","to","a","computer.","Art","is","everything","else","we","do"], maxWidth = 20
Output:
[
  "Science  is  what we",
  "understand well    ",  (Note: actually "understand well" should be "understand well      ")
  "enough to explain to",
  "a  computer.  Art is",
  "everything  else  we",
  "do                  "
]
```

## Constraints

- `1 <= words.length <= 300`
- `1 <= words[i].length <= 20`
- `words[i]` consists of only English letters and symbols.
- `1 <= maxWidth <= 100`
- `words[i].length <= maxWidth`

## Approach

### Algorithm Steps:
1. **Greedy Line Packing**: Pack as many words as possible into each line
2. **Calculate Spaces**: Determine total spaces and distribute them evenly
3. **Handle Last Line**: Left-justify with single spaces

### Space Distribution:
- Total spaces = maxWidth - total_word_lengths
- If n words in line: n-1 gaps between words
- Base spaces per gap = total_spaces // (n-1)
- Extra spaces = total_spaces % (n-1) (distribute to leftmost gaps)

## Solution

```python
from typing import List

class Solution:
    def fullJustify(self, words: List[str], maxWidth: int) -> List[str]:
        result = []
        current_line = []
        current_length = 0  # Length of words only (no spaces)
        
        for word in words:
            # Check if we can add this word to current line
            # Need: current_length + len(word) + number_of_spaces <= maxWidth
            # Minimum spaces needed = len(current_line) (one space between each word)
            if current_length + len(word) + len(current_line) > maxWidth:
                # Can't fit, justify current line and start new one
                result.append(self.justify_line(current_line, current_length, maxWidth))
                current_line = []
                current_length = 0
            
            # Add word to current line
            current_line.append(word)
            current_length += len(word)
        
        # Handle last line (left-justified)
        last_line = ' '.join(current_line)
        last_line += ' ' * (maxWidth - len(last_line))
        result.append(last_line)
        
        return result
    
    def justify_line(self, words: List[str], words_length: int, maxWidth: int) -> str:
        """Fully justify a line of words."""
        if len(words) == 1:
            # Single word - left justify
            return words[0] + ' ' * (maxWidth - len(words[0]))
        
        # Calculate space distribution
        total_spaces = maxWidth - words_length
        gaps = len(words) - 1
        
        # Base spaces per gap and extra spaces for leftmost gaps
        spaces_per_gap = total_spaces // gaps
        extra_spaces = total_spaces % gaps
        
        # Build the justified line
        result = []
        for i, word in enumerate(words[:-1]):  # All words except last
            result.append(word)
            # Add spaces: base + 1 extra for first 'extra_spaces' gaps
            spaces = spaces_per_gap + (1 if i < extra_spaces else 0)
            result.append(' ' * spaces)
        
        result.append(words[-1])  # Add last word (no spaces after)
        
        return ''.join(result)
```

## Alternative Solution (More Compact)

```python
from typing import List

class Solution:
    def fullJustify(self, words: List[str], maxWidth: int) -> List[str]:
        result = []
        line = []
        line_length = 0
        
        i = 0
        while i < len(words):
            # Try to fit as many words as possible
            if line_length + len(line) + len(words[i]) <= maxWidth:
                line.append(words[i])
                line_length += len(words[i])
                i += 1
            else:
                # Justify and add current line
                result.append(self.format_line(line, line_length, maxWidth, is_last=False))
                line = []
                line_length = 0
        
        # Last line - left justified
        result.append(self.format_line(line, line_length, maxWidth, is_last=True))
        
        return result
    
    def format_line(self, words: List[str], words_length: int, maxWidth: int, is_last: bool) -> str:
        if is_last or len(words) == 1:
            # Left justify
            return ' '.join(words).ljust(maxWidth)
        
        # Full justify
        total_spaces = maxWidth - words_length
        gaps = len(words) - 1
        space_per_gap, extra = divmod(total_spaces, gaps)
        
        parts = []
        for i, word in enumerate(words[:-1]):
            parts.append(word)
            parts.append(' ' * (space_per_gap + (1 if i < extra else 0)))
        parts.append(words[-1])
        
        return ''.join(parts)
```

## Solution with Detailed Step-by-Step

```python
from typing import List

class Solution:
    def fullJustify(self, words: List[str], maxWidth: int) -> List[str]:
        """
        Algorithm:
        1. Greedily pack words into lines
        2. For each complete line (not last):
           - Calculate total spaces needed
           - Distribute evenly, with extras going to left gaps
        3. For last line:
           - Left justify with single spaces between words
           - Pad with spaces at end
        """
        result = []
        n = len(words)
        i = 0
        
        while i < n:
            # Step 1: Find how many words fit in this line
            line_start = i
            line_length = len(words[i])
            i += 1
            
            while i < n and line_length + 1 + len(words[i]) <= maxWidth:
                line_length += 1 + len(words[i])  # +1 for minimum space
                i += 1
            
            line_words = words[line_start:i]
            
            # Step 2: Format the line
            is_last_line = (i == n)
            is_single_word = len(line_words) == 1
            
            if is_last_line or is_single_word:
                # Left justify
                line = ' '.join(line_words)
                line += ' ' * (maxWidth - len(line))
            else:
                # Full justify
                words_only_length = sum(len(w) for w in line_words)
                total_spaces = maxWidth - words_only_length
                gaps = len(line_words) - 1
                
                space_width, extra = divmod(total_spaces, gaps)
                
                line_parts = []
                for j, word in enumerate(line_words[:-1]):
                    line_parts.append(word)
                    spaces = space_width + (1 if j < extra else 0)
                    line_parts.append(' ' * spaces)
                line_parts.append(line_words[-1])
                
                line = ''.join(line_parts)
            
            result.append(line)
        
        return result
```

## Complexity Analysis

### Time Complexity: O(n × maxWidth)
- We process each word once: O(n)
- Building each line involves string operations: O(maxWidth) per line
- Total: O(n × maxWidth) or O(total_characters)

### Space Complexity: O(maxWidth)
- Each line is at most maxWidth characters
- We build lines one at a time

## Visual Example

```
Words: ["This", "is", "an", "example", "of", "text", "justification."]
maxWidth: 16

Line 1: Pack "This", "is", "an"
- Word lengths: 4 + 2 + 2 = 8
- Spaces needed: 16 - 8 = 8
- Gaps: 2
- Spaces per gap: 8 // 2 = 4
- Extra: 8 % 2 = 0
- Result: "This    is    an"

Line 2: Pack "example", "of", "text"
- Word lengths: 7 + 2 + 4 = 13
- Spaces needed: 16 - 13 = 3
- Gaps: 2
- Spaces per gap: 3 // 2 = 1
- Extra: 3 % 2 = 1 (goes to first gap)
- Result: "example  of text"

Line 3 (last): Pack "justification."
- Single word on last line
- Left justify: "justification." + 2 spaces
- Result: "justification.  "
```

## Key Patterns & Techniques

1. **Greedy Packing**: Fit as many words as possible per line
2. **Space Distribution**: Use divmod for base + extra pattern
3. **Edge Cases**: Single word lines, last line
4. **String Building**: Use list + join for efficiency

## Edge Cases

1. **Single word per line**: Left justify with trailing spaces
2. **Last line**: Left justify regardless of word count
3. **Words that exactly fill the line**: No extra spaces needed
4. **Single word in entire input**: Just that word padded with spaces

## Common Mistakes to Avoid

1. Not handling single-word lines correctly
2. Forgetting to left-justify the last line
3. Off-by-one errors in space distribution
4. Not accounting for the minimum one space between words when packing

## Related Problems

- [1592. Rearrange Spaces Between Words](https://leetcode.com/problems/rearrange-spaces-between-words/)
- [1624. Largest Substring Between Two Equal Characters](https://leetcode.com/problems/largest-substring-between-two-equal-characters/)
- [1071. Greatest Common Divisor of Strings](https://leetcode.com/problems/greatest-common-divisor-of-strings/)

