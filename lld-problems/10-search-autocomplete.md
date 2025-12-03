# Search Autocomplete / Typeahead - LLD

## Problem Statement
Design a search autocomplete system that suggests completions as the user types.

---

## Code Flow Walkthrough

### `insert(word, frequency)` - Add Word to Trie

```
CALL: trie.insert("apple", frequency=5)

STEP 1: Normalize Input
├── normalizedWord = "apple".lowercase().trim()
└── Ensures consistent lookup

STEP 2: Traverse/Create Path
├── current = root
├── FOR each char in "apple":
│   ├── 'a': current = root.children.getOrPut('a') { TrieNode() }
│   ├── 'p': current = current.children.getOrPut('p') { TrieNode() }
│   ├── 'p': current = current.children.getOrPut('p') { TrieNode() }
│   ├── 'l': current = current.children.getOrPut('l') { TrieNode() }
│   └── 'e': current = current.children.getOrPut('e') { TrieNode() }
└── Creates nodes if they don't exist

STEP 3: Mark End of Word
├── current.isEndOfWord = true
├── current.frequency += frequency  // Accumulate if exists
└── current.word = "apple"

STEP 4: Update Top Suggestions
├── updateSuggestions("apple", frequency=5)
│   ├── Walk from root to "apple" node
│   ├── At each node, update topSuggestions list
│   ├── Keep sorted by frequency, limit to maxSuggestions
│   └── Enables O(1) suggestion retrieval

TRIE STRUCTURE AFTER:
        root
          │
          a
          │
          p
          │
          p
          │
          l
          │
          e [end, freq=5, word="apple"]
```

### `search(prefix)` - Get Autocomplete Suggestions

```
CALL: trie.search("app")
RETURNS: ["apple", "application", "app"]

STEP 1: Navigate to Prefix Node
├── current = root
├── FOR each char in "app":
│   ├── 'a': current = root.children['a']
│   │   └── IF null: return emptyList()
│   ├── 'p': current = current.children['p']
│   └── 'p': current = current.children['p']
└── current now points to node after "app"

STEP 2: Return Cached Suggestions (Optimized)
├── IF current.topSuggestions is cached:
│   └── Return topSuggestions  // O(1)!

STEP 3: OR Collect All Words (Non-optimized)
├── collectWords(current, prefix="app"):
│   ├── IF current.isEndOfWord:
│   │   └── Add (current.word, current.frequency) to results
│   ├── FOR each (char, child) in current.children:
│   │   └── collectWords(child, prefix + char)
│   └── Return all words with this prefix
├── Sort by frequency (descending)
└── Return top k results

EXAMPLE TRIE:
        root
          │
          a
          │
          p
          │
          p [end, "app", freq=3]
         / \
        l   i
        │   │
        e   c
        │   │
   [end]│   a
        │   │
      "apple"   t
       freq=5   │
                i
                │
                o
                │
                n [end, "application", freq=2]

search("app") → ["apple"(5), "app"(3), "application"(2)]
```

### Wildcard Search Flow

```
CALL: trie.searchWithWildcard("a*ple")
(* matches any single character)

STEP 1: Recursive Search with Wildcards
├── searchWildcard(node=root, pattern="a*ple", index=0)
│   ├── char = 'a': match exactly
│   │   └── Recurse: node=node.children['a'], pattern, index=1
│   ├── char = '*': match any
│   │   └── FOR each child in node.children:
│   │       └── Recurse: child, pattern, index=2
│   ├── char = 'p': match exactly
│   ├── char = 'l': match exactly
│   ├── char = 'e': match exactly
│   └── IF index == pattern.length AND node.isEndOfWord:
│       └── Add to results

MATCHES:
├── "apple" (a-p-p-l-e, * matches 'p')
├── "ample" (a-m-p-l-e, * matches 'm')
└── etc.
```

### Frequency Tracking and Ranking

```
SCENARIO: Search history updates word frequency

USER ACTIONS:
├── User searches "apple" → increment frequency
├── trie.search("app") returns ["apple", "app", "application"]
├── User selects "application"
├── trie.incrementFrequency("application")
│   ├── Navigate to "application" node
│   ├── node.frequency++
│   └── updateSuggestions() along path

AFTER MANY SELECTIONS:
├── "application" frequency: 50
├── "apple" frequency: 30
├── "app" frequency: 10
├── trie.search("app") returns ["application", "apple", "app"]
└── More frequent words rank higher

DECAY STRATEGY (optional):
├── Every N days: frequency *= 0.9
├── Old popular terms fade
└── Recent searches rank higher
```

---

## Requirements

### Functional Requirements
1. Return top-k suggestions for a prefix
2. Support adding new search terms
3. Rank by frequency/popularity
4. Handle updates efficiently

### Non-Functional Requirements
1. Fast response (<100ms)
2. Space efficient
3. Support millions of terms

---

## Class Diagram

```
┌─────────────────────────────────────┐
│            TrieNode                 │
├─────────────────────────────────────┤
│ - children: Map<Char, TrieNode>     │
│ - isEndOfWord: Boolean              │
│ - frequency: Int                    │
│ - word: String?                     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│          AutocompleteService        │
├─────────────────────────────────────┤
│ - trie: Trie                        │
│ - topK: Int                         │
├─────────────────────────────────────┤
│ + addWord(word: String, freq: Int)  │
│ + search(prefix: String): List      │
│ + incrementFrequency(word: String)  │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Trie Node

```kotlin
import java.util.*

// ==================== Trie Node ====================

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
    var frequency = 0
    var word: String? = null
    
    // For optimization: store top suggestions at each node
    val topSuggestions = mutableListOf<Suggestion>()
}

data class Suggestion(
    val word: String,
    val frequency: Int
) : Comparable<Suggestion> {
    override fun compareTo(other: Suggestion): Int {
        // Higher frequency first, then alphabetical
        val freqCompare = other.frequency.compareTo(frequency)
        return if (freqCompare != 0) freqCompare else word.compareTo(other.word)
    }
}
```

### Trie Implementation

```kotlin
// ==================== Trie ====================

/**
 * Trie (Prefix Tree) optimized for autocomplete.
 * 
 * === Data Structure ===
 * 
 *         root
 *        /    \
 *       a      b
 *      /|\      \
 *     p n t      a
 *    /  |  \      \
 *   p   d   e      l
 *  /              /  \
 * le            l    loon
 * 
 * "apple", "and", "ate", "ball", "balloon"
 * 
 * === Optimization: Pre-computed Top-K ===
 * Each node stores top-k suggestions for its prefix.
 * - Query "app" → immediately return cached suggestions
 * - No need to traverse entire subtree
 * 
 * === Time Complexity ===
 * - Insert: O(L) where L = word length
 * - Search with cached top-k: O(P) where P = prefix length
 * - Search without cache: O(P + N) where N = matching words
 * 
 * === Space Complexity ===
 * - O(ALPHABET_SIZE × N × L) worst case
 * - In practice: much less due to shared prefixes
 * 
 * @param maxSuggestionsPerNode How many suggestions to cache per node
 */
class Trie(private val maxSuggestionsPerNode: Int = 10) {
    private val root = TrieNode()
    
    // ==================== Insert ====================
    
    /**
     * Insert a word with its frequency into the trie.
     * Also updates cached top suggestions along the path.
     * 
     * @param word The word to insert
     * @param frequency Initial frequency (or increment if exists)
     */
    fun insert(word: String, frequency: Int = 1) {
        if (word.isBlank()) return
        
        var current = root
        val normalizedWord = word.lowercase().trim()
        
        // Traverse/create path for each character
        for (char in normalizedWord) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        
        // Mark end of word and update frequency
        current.isEndOfWord = true
        current.frequency += frequency
        current.word = normalizedWord
        
        // Update top suggestions along the path (optimization)
        updateSuggestions(normalizedWord, current.frequency)
    }
    
    private fun updateSuggestions(word: String, frequency: Int) {
        var current = root
        val suggestion = Suggestion(word, frequency)
        
        for (char in word) {
            current = current.children[char] ?: return
            
            // Update this node's top suggestions
            val existingIndex = current.topSuggestions.indexOfFirst { it.word == word }
            if (existingIndex >= 0) {
                current.topSuggestions[existingIndex] = suggestion
            } else {
                current.topSuggestions.add(suggestion)
            }
            
            // Keep sorted and limit size
            current.topSuggestions.sort()
            if (current.topSuggestions.size > maxSuggestionsPerNode) {
                current.topSuggestions.removeAt(current.topSuggestions.lastIndex)
            }
        }
    }
    
    // ==================== Search ====================
    
    fun searchPrefix(prefix: String, limit: Int = 10): List<Suggestion> {
        if (prefix.isBlank()) return emptyList()
        
        val normalizedPrefix = prefix.lowercase().trim()
        var current = root
        
        // Navigate to prefix node
        for (char in normalizedPrefix) {
            current = current.children[char] ?: return emptyList()
        }
        
        // Return cached top suggestions if available
        if (current.topSuggestions.isNotEmpty()) {
            return current.topSuggestions.take(limit)
        }
        
        // Otherwise, do DFS to find all words
        return findAllWords(current, limit)
    }
    
    private fun findAllWords(node: TrieNode, limit: Int): List<Suggestion> {
        val results = PriorityQueue<Suggestion>(compareBy { it.frequency })
        
        fun dfs(current: TrieNode) {
            if (current.isEndOfWord && current.word != null) {
                results.add(Suggestion(current.word!!, current.frequency))
                if (results.size > limit) {
                    results.poll() // Remove lowest frequency
                }
            }
            
            for (child in current.children.values) {
                dfs(child)
            }
        }
        
        dfs(node)
        return results.toList().sortedDescending()
    }
    
    // ==================== Other Operations ====================
    
    fun contains(word: String): Boolean {
        val normalizedWord = word.lowercase().trim()
        var current = root
        
        for (char in normalizedWord) {
            current = current.children[char] ?: return false
        }
        
        return current.isEndOfWord
    }
    
    fun getFrequency(word: String): Int {
        val normalizedWord = word.lowercase().trim()
        var current = root
        
        for (char in normalizedWord) {
            current = current.children[char] ?: return 0
        }
        
        return if (current.isEndOfWord) current.frequency else 0
    }
    
    fun incrementFrequency(word: String) {
        if (contains(word)) {
            insert(word, 1) // This will add to existing frequency
        }
    }
}
```

### Autocomplete Service

```kotlin
// ==================== Autocomplete Service ====================

class AutocompleteService(
    private val topK: Int = 10,
    maxSuggestionsPerNode: Int = 20
) {
    private val trie = Trie(maxSuggestionsPerNode)
    private val recentSearches = LinkedList<String>()
    private val maxRecentSearches = 100
    
    // ==================== Core Operations ====================
    
    fun addTerm(term: String, frequency: Int = 1) {
        trie.insert(term, frequency)
    }
    
    fun addTerms(terms: List<Pair<String, Int>>) {
        terms.forEach { (term, freq) -> trie.insert(term, freq) }
    }
    
    fun search(prefix: String): List<String> {
        if (prefix.isBlank()) {
            return recentSearches.take(topK)
        }
        
        return trie.searchPrefix(prefix, topK).map { it.word }
    }
    
    fun searchWithFrequency(prefix: String): List<Suggestion> {
        return trie.searchPrefix(prefix, topK)
    }
    
    // ==================== Tracking ====================
    
    fun recordSearch(term: String) {
        // Increment frequency
        if (trie.contains(term)) {
            trie.incrementFrequency(term)
        } else {
            trie.insert(term, 1)
        }
        
        // Update recent searches
        recentSearches.remove(term)
        recentSearches.addFirst(term)
        if (recentSearches.size > maxRecentSearches) {
            recentSearches.removeLast()
        }
    }
    
    fun getRecentSearches(): List<String> {
        return recentSearches.toList()
    }
}

// ==================== With Caching ====================

class CachedAutocompleteService(
    topK: Int = 10,
    private val cacheSize: Int = 1000
) {
    private val service = AutocompleteService(topK)
    private val cache = LinkedHashMap<String, List<String>>(cacheSize, 0.75f, true)
    
    fun search(prefix: String): List<String> {
        val normalizedPrefix = prefix.lowercase().trim()
        
        return cache.getOrPut(normalizedPrefix) {
            service.search(normalizedPrefix)
        }
    }
    
    fun addTerm(term: String, frequency: Int = 1) {
        service.addTerm(term, frequency)
        // Invalidate relevant cache entries
        cache.keys.removeIf { term.lowercase().startsWith(it) }
    }
    
    fun recordSearch(term: String) {
        service.recordSearch(term)
        // Could invalidate cache here if needed
    }
}
```

### Usage Example

```kotlin
fun main() {
    val autocomplete = AutocompleteService(topK = 5)
    
    // Add sample data
    val searchTerms = listOf(
        "apple" to 100,
        "application" to 80,
        "apply" to 60,
        "apartment" to 40,
        "banana" to 90,
        "ball" to 70,
        "balloon" to 50,
        "cat" to 85,
        "car" to 95,
        "card" to 65
    )
    
    autocomplete.addTerms(searchTerms)
    
    println("=== Search Autocomplete Demo ===\n")
    
    // Test searches
    val prefixes = listOf("a", "ap", "app", "b", "ba", "c", "ca")
    
    prefixes.forEach { prefix ->
        val suggestions = autocomplete.searchWithFrequency(prefix)
        println("Prefix '$prefix': ${suggestions.map { "${it.word}(${it.frequency})" }}")
    }
    
    // Record a search (increases frequency)
    println("\n--- Recording search 'apply' ---")
    autocomplete.recordSearch("apply")
    autocomplete.recordSearch("apply")
    autocomplete.recordSearch("apply")
    
    val updatedSuggestions = autocomplete.searchWithFrequency("app")
    println("After recording: ${updatedSuggestions.map { "${it.word}(${it.frequency})" }}")
    
    // Add new term
    println("\n--- Adding new term 'appetizer' ---")
    autocomplete.addTerm("appetizer", 75)
    
    val newSuggestions = autocomplete.search("app")
    println("Updated suggestions for 'app': $newSuggestions")
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Trie** | Core data structure | Efficient prefix matching |
| **Decorator** | `CachedAutocompleteService` | Add caching layer |
| **Factory** | Could add for different strategies | Create different rankers |

---

## Optimizations

| Technique | Benefit |
|-----------|---------|
| Pre-computed top-k at nodes | O(1) lookup for common prefixes |
| LRU Cache | Avoid recomputation |
| Frequency decay | Prefer recent searches |
| Sharding by first char | Distribute load |

---

## Interview Discussion Points

### Q: How to handle typos?
**A:**
- Edit distance (Levenshtein)
- Phonetic matching (Soundex)
- N-gram based fuzzy matching
- Pre-computed corrections

### Q: How to personalize?
**A:**
- Per-user search history
- User preferences weighting
- Collaborative filtering
- Context-aware ranking

### Q: How to scale to billions of terms?
**A:**
- Distributed trie (sharded by prefix)
- Hot prefixes in memory, rest on disk
- Caching layer (Redis)
- Pre-aggregated results

---

## Time & Space Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Insert | O(L) | O(L) where L = word length |
| Search | O(P + K) | O(K) where P = prefix, K = results |
| With cached top-k | O(P) | O(N × K) for caching |

---

## Edge Cases

1. **Empty prefix** - return popular/recent
2. **No matches** - return empty or fuzzy matches
3. **Special characters** - normalize or filter
4. **Case sensitivity** - normalize to lowercase
5. **Unicode/internationalization**

