# Design Add and Search Words Data Structure

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 47.1%
- **Topics**: String, Depth-First Search, Design, Trie
- **LeetCode Link**: https://leetcode.com/problems/design-add-and-search-words-data-structure

## Problem Description

Design a data structure that supports adding new words and finding if a string matches any previously added string.

Implement the `WordDictionary` class:
- `WordDictionary()` Initializes the object.
- `void addWord(word)` Adds `word` to the data structure, it can be matched later.
- `bool search(word)` Returns `true` if there is any string in the data structure that matches `word` or `false` otherwise. `word` may contain dots `'.'` where dots can be matched with any letter.

## Examples

### Example 1:
```
Input
["WordDictionary","addWord","addWord","addWord","search","search","search","search"]
[[],["bad"],["dad"],["mad"],["pad"],["bad"],[".ad"],["b.."]]
Output
[null,null,null,null,false,true,true,true]

Explanation
WordDictionary wordDictionary = new WordDictionary();
wordDictionary.addWord("bad");
wordDictionary.addWord("dad");
wordDictionary.addWord("mad");
wordDictionary.search("pad"); // return False
wordDictionary.search("bad"); // return True
wordDictionary.search(".ad"); // return True (matches "bad", "dad", "mad")
wordDictionary.search("b.."); // return True (matches "bad")
```

## Constraints

- `1 <= word.length <= 25`
- `word` in `addWord` consists of lowercase English letters.
- `word` in `search` consists of `'.'` or lowercase English letters.
- There will be at most `3` dots in `word` for `search` queries.
- At most `10^4` calls will be made to `addWord` and `search`.

## Approach

### Key Insight:
- Use a Trie for efficient prefix matching
- Handle wildcard `.` with DFS/backtracking to try all children

### Data Structure:
- Trie node with children (dict or array) and end-of-word flag
- Each path from root represents a word

### Search Algorithm:
- For regular characters: follow the trie path
- For `.`: recursively try all possible children

## Solution

```python
class TrieNode:
    def __init__(self):
        self.children = {}
        self.is_end = False

class WordDictionary:
    def __init__(self):
        self.root = TrieNode()
    
    def addWord(self, word: str) -> None:
        node = self.root
        for char in word:
            if char not in node.children:
                node.children[char] = TrieNode()
            node = node.children[char]
        node.is_end = True
    
    def search(self, word: str) -> bool:
        return self._search_helper(word, 0, self.root)
    
    def _search_helper(self, word: str, index: int, node: TrieNode) -> bool:
        if index == len(word):
            return node.is_end
        
        char = word[index]
        
        if char == '.':
            # Wildcard: try all possible children
            for child in node.children.values():
                if self._search_helper(word, index + 1, child):
                    return True
            return False
        else:
            # Regular character: follow the path
            if char not in node.children:
                return False
            return self._search_helper(word, index + 1, node.children[char])
```

## Solution with Iterative Search (No Recursion for Non-Wildcard)

```python
class TrieNode:
    def __init__(self):
        self.children = {}
        self.is_end = False

class WordDictionary:
    def __init__(self):
        self.root = TrieNode()
    
    def addWord(self, word: str) -> None:
        node = self.root
        for char in word:
            if char not in node.children:
                node.children[char] = TrieNode()
            node = node.children[char]
        node.is_end = True
    
    def search(self, word: str) -> bool:
        def dfs(index, node):
            for i in range(index, len(word)):
                char = word[i]
                
                if char == '.':
                    # Try all children
                    for child in node.children.values():
                        if dfs(i + 1, child):
                            return True
                    return False
                else:
                    if char not in node.children:
                        return False
                    node = node.children[char]
            
            return node.is_end
        
        return dfs(0, self.root)
```

## Solution Using Array Instead of Dict

```python
class TrieNode:
    def __init__(self):
        self.children = [None] * 26
        self.is_end = False

class WordDictionary:
    def __init__(self):
        self.root = TrieNode()
    
    def addWord(self, word: str) -> None:
        node = self.root
        for char in word:
            index = ord(char) - ord('a')
            if node.children[index] is None:
                node.children[index] = TrieNode()
            node = node.children[index]
        node.is_end = True
    
    def search(self, word: str) -> bool:
        def dfs(word_index, node):
            if word_index == len(word):
                return node.is_end
            
            char = word[word_index]
            
            if char == '.':
                # Try all 26 possible children
                for child in node.children:
                    if child and dfs(word_index + 1, child):
                        return True
                return False
            else:
                index = ord(char) - ord('a')
                if node.children[index] is None:
                    return False
                return dfs(word_index + 1, node.children[index])
        
        return dfs(0, self.root)
```

## Complexity Analysis

### addWord:
- **Time**: O(m) where m is the word length
- **Space**: O(m) for new nodes

### search:
- **Time**: 
  - Without wildcards: O(m)
  - With wildcards: O(26^k × m) worst case, where k = number of dots
  - Since k ≤ 3, this is manageable
- **Space**: O(m) for recursion stack

### Total Space: O(n × m)
- n = number of words
- m = average word length

## Trie Visualization

```
After adding: "bad", "dad", "mad"

          root
         /  |  \
        b   d   m
        |   |   |
        a   a   a
        |   |   |
        d*  d*  d*

* = is_end = True

Search ".ad":
- At root, '.' matches b, d, m
- From b: 'a' found, 'd' found with is_end=True ✓
- Found match!
```

## Key Patterns & Techniques

1. **Trie Data Structure**: Efficient prefix-based storage
2. **DFS for Wildcard Matching**: Explore all possibilities
3. **Early Termination**: Stop as soon as a match is found
4. **Recursive Backtracking**: Try each path for wildcards

## Edge Cases

1. Empty string (depends on problem definition)
2. All wildcards: "..." matches any 3-letter word
3. No matching words
4. Word with wildcards at start, middle, end

## Common Mistakes to Avoid

1. Not marking `is_end` when adding words
2. Returning `True` before checking `is_end`
3. Not handling the case when wildcard has no children
4. Stack overflow for very long words (use iterative approach)

## Optimization: Group Words by Length

```python
class WordDictionary:
    def __init__(self):
        self.words_by_length = {}  # length -> set of words
    
    def addWord(self, word: str) -> None:
        length = len(word)
        if length not in self.words_by_length:
            self.words_by_length[length] = set()
        self.words_by_length[length].add(word)
    
    def search(self, word: str) -> bool:
        length = len(word)
        if length not in self.words_by_length:
            return False
        
        if '.' not in word:
            return word in self.words_by_length[length]
        
        # Use regex or manual matching for wildcards
        for candidate in self.words_by_length[length]:
            if self._matches(word, candidate):
                return True
        return False
    
    def _matches(self, pattern: str, word: str) -> bool:
        for p, w in zip(pattern, word):
            if p != '.' and p != w:
                return False
        return True
```

## Related Problems

- [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/)
- [212. Word Search II](https://leetcode.com/problems/word-search-ii/)
- [14. Longest Common Prefix](https://leetcode.com/problems/longest-common-prefix/)
- [720. Longest Word in Dictionary](https://leetcode.com/problems/longest-word-in-dictionary/)

