/**
 * Search Autocomplete System - LLD Implementation
 * 
 * Design an autocomplete system using Trie with:
 * - Word insertion with frequency
 * - Prefix search with top-k results
 * - Frequency-based ranking
 * 
 * Time Complexity:
 * - Insert: O(L) where L = word length
 * - Search: O(P + K) where P = prefix length, K = suggestions
 */
package lld.autocomplete

import java.util.*

// ==================== Trie Node ====================

/**
 * Trie node with children and word metadata.
 */
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
    var frequency = 0
    var word: String? = null
    
    // Cached top suggestions for this prefix (optimization)
    val topSuggestions = PriorityQueue<Pair<String, Int>>(
        compareBy { it.second }  // Min heap by frequency
    )
}

// ==================== Trie ====================

/**
 * Trie data structure optimized for autocomplete.
 * 
 * === Structure ===
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
 * === Code Flow: insert(word, frequency) ===
 * 1. Normalize word (lowercase, trim)
 * 2. Traverse/create path for each character
 * 3. Mark end of word with frequency
 * 4. Update top suggestions along the path
 * 
 * === Code Flow: search(prefix) ===
 * 1. Navigate to prefix node
 * 2. Return cached top suggestions (O(1)!)
 * OR collect all words under prefix (O(n))
 */
class Trie(private val maxSuggestionsPerNode: Int = 10) {
    private val root = TrieNode()
    
    /**
     * Insert a word with its frequency.
     */
    fun insert(word: String, frequency: Int = 1) {
        if (word.isBlank()) return
        
        var current = root
        val normalizedWord = word.lowercase().trim()
        
        for (char in normalizedWord) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        
        current.isEndOfWord = true
        current.frequency += frequency
        current.word = normalizedWord
        
        // Update top suggestions along the path
        updateSuggestions(normalizedWord, current.frequency)
    }
    
    /**
     * Update cached suggestions from root to word.
     */
    private fun updateSuggestions(word: String, frequency: Int) {
        var current = root
        
        for (char in word) {
            current = current.children[char] ?: return
            
            // Update this node's top suggestions
            val suggestions = current.topSuggestions
            
            // Remove old entry if exists
            suggestions.removeIf { it.first == word }
            
            // Add with new frequency
            suggestions.add(Pair(word, frequency))
            
            // Keep only top K
            while (suggestions.size > maxSuggestionsPerNode) {
                suggestions.poll()  // Remove lowest frequency
            }
        }
    }
    
    /**
     * Search for words with given prefix.
     * 
     * @param prefix The prefix to search for
     * @param limit Maximum number of results
     * @return List of (word, frequency) pairs sorted by frequency desc
     */
    fun search(prefix: String, limit: Int = 10): List<Pair<String, Int>> {
        if (prefix.isBlank()) return emptyList()
        
        var current = root
        val normalizedPrefix = prefix.lowercase().trim()
        
        // Navigate to prefix node
        for (char in normalizedPrefix) {
            current = current.children[char] ?: return emptyList()
        }
        
        // Return cached suggestions (optimization)
        return current.topSuggestions
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Search without optimization - collects all words.
     */
    fun searchAll(prefix: String): List<Pair<String, Int>> {
        if (prefix.isBlank()) return emptyList()
        
        var current = root
        val normalizedPrefix = prefix.lowercase().trim()
        
        for (char in normalizedPrefix) {
            current = current.children[char] ?: return emptyList()
        }
        
        // Collect all words under this node
        val results = mutableListOf<Pair<String, Int>>()
        collectWords(current, results)
        
        return results.sortedByDescending { it.second }
    }
    
    /**
     * DFS to collect all words under a node.
     */
    private fun collectWords(node: TrieNode, results: MutableList<Pair<String, Int>>) {
        if (node.isEndOfWord && node.word != null) {
            results.add(Pair(node.word!!, node.frequency))
        }
        
        for ((_, child) in node.children) {
            collectWords(child, results)
        }
    }
    
    /**
     * Check if a word exists.
     */
    fun contains(word: String): Boolean {
        var current = root
        
        for (char in word.lowercase()) {
            current = current.children[char] ?: return false
        }
        
        return current.isEndOfWord
    }
    
    /**
     * Get frequency of a word.
     */
    fun getFrequency(word: String): Int {
        var current = root
        
        for (char in word.lowercase()) {
            current = current.children[char] ?: return 0
        }
        
        return if (current.isEndOfWord) current.frequency else 0
    }
    
    /**
     * Increment frequency of a word.
     */
    fun incrementFrequency(word: String) {
        if (contains(word)) {
            insert(word, 1)  // Adds to existing frequency
        }
    }
}

// ==================== Autocomplete Service ====================

/**
 * Autocomplete service with search history tracking.
 */
class AutocompleteService(
    private val maxSuggestions: Int = 10
) {
    private val trie = Trie(maxSuggestions)
    private val searchHistory = mutableListOf<String>()
    
    /**
     * Add words to the index.
     */
    fun addWord(word: String, frequency: Int = 1) {
        trie.insert(word, frequency)
    }
    
    /**
     * Add multiple words.
     */
    fun addWords(words: List<String>) {
        words.forEach { trie.insert(it, 1) }
    }
    
    /**
     * Get suggestions for prefix.
     */
    fun getSuggestions(prefix: String): List<String> {
        return trie.search(prefix, maxSuggestions).map { it.first }
    }
    
    /**
     * Record a search selection (for learning).
     */
    fun recordSelection(word: String) {
        searchHistory.add(word)
        trie.incrementFrequency(word)
    }
    
    /**
     * Get recent searches.
     */
    fun getRecentSearches(limit: Int = 5): List<String> {
        return searchHistory.takeLast(limit).reversed().distinct()
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Search Autocomplete System ===\n")
    
    val autocomplete = AutocompleteService(maxSuggestions = 5)
    
    // Add dictionary words with initial frequencies
    val words = mapOf(
        "apple" to 100,
        "application" to 80,
        "app" to 150,
        "apply" to 60,
        "appreciate" to 40,
        "approach" to 70,
        "banana" to 50,
        "ball" to 90,
        "balloon" to 30
    )
    
    words.forEach { (word, freq) ->
        autocomplete.addWord(word, freq)
    }
    
    println("Dictionary loaded with ${words.size} words\n")
    
    // Search tests
    val prefixes = listOf("app", "ap", "ba", "ball")
    
    for (prefix in prefixes) {
        val suggestions = autocomplete.getSuggestions(prefix)
        println("Suggestions for '$prefix': $suggestions")
    }
    
    println("\n--- Simulating user selections ---")
    
    // User selects "application" when searching "app"
    autocomplete.recordSelection("application")
    autocomplete.recordSelection("application")
    autocomplete.recordSelection("application")
    
    println("User selected 'application' 3 times")
    
    // Frequency should have increased
    val newSuggestions = autocomplete.getSuggestions("app")
    println("New suggestions for 'app': $newSuggestions")
    
    println("\nRecent searches: ${autocomplete.getRecentSearches()}")
    
    println("\n--- Trie Direct Usage ---")
    
    val trie = Trie()
    
    // Simulate search history
    trie.insert("how to cook pasta", 50)
    trie.insert("how to code", 100)
    trie.insert("how to learn kotlin", 30)
    trie.insert("what is kotlin", 80)
    trie.insert("where is", 20)
    
    println("Search 'how': ${trie.search("how").map { it.first }}")
    println("Search 'what': ${trie.search("what").map { it.first }}")
    
    // Word exists check
    println("\nContains 'how to code': ${trie.contains("how to code")}")
    println("Contains 'how to': ${trie.contains("how to")}")
    
    // Get frequency
    println("Frequency of 'how to code': ${trie.getFrequency("how to code")}")
}

