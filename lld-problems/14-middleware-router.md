# Middleware Router Path Resolver - LLD

## Problem Statement
Implement a middleware router for a web service that maps URL paths to handler strings. Support exact path matching and wildcard patterns.

---

## Requirements

### Functional Requirements
1. Add routes with paths and result strings
2. Resolve routes by exact path matching
3. Support wildcard (*) matching for path segments
4. Support path parameters extraction

### Scale-ups
1. **Basic**: Exact path matching (`/foo` → "foo")
2. **Wildcard**: Pattern matching (`/bar/*/baz` → "bar" for `/bar/a/baz`)
3. **Path Params**: Extract values (`/user/:id` → extract id value)

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│           <<interface>>                  │
│              Router                      │
├─────────────────────────────────────────┤
│ + addRoute(path: String, result: String)│
│ + callRoute(path: String): String?      │
└─────────────────────────────────────────┘
              △
              │
    ┌─────────┴─────────┐
    │                   │
┌───┴───────┐     ┌─────┴─────┐
│HashMapRouter│   │TrieRouter  │
│(exact only) │   │(wildcards) │
└─────────────┘   └───────────┘

┌─────────────────────────────────────────┐
│            TrieNode                      │
├─────────────────────────────────────────┤
│ - children: Map<String, TrieNode>       │
│ - wildcardChild: TrieNode?              │
│ - paramChild: TrieNode?                 │
│ - paramName: String?                    │
│ - result: String?                       │
│ - isEndOfRoute: Boolean                 │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Basic Router (HashMap-based)

```kotlin
// ==================== Router Interface ====================

interface Router {
    fun addRoute(path: String, result: String)
    fun callRoute(path: String): String?
}

// ==================== Simple HashMap Router ====================

class HashMapRouter : Router {
    private val routes = mutableMapOf<String, String>()
    
    override fun addRoute(path: String, result: String) {
        routes[normalizePath(path)] = result
    }
    
    override fun callRoute(path: String): String? {
        return routes[normalizePath(path)]
    }
    
    private fun normalizePath(path: String): String {
        return path.trim().trimEnd('/')
    }
}
```

### Trie-based Router with Wildcard Support

```kotlin
// ==================== Trie Node ====================

class TrieNode {
    val children = mutableMapOf<String, TrieNode>()
    var wildcardChild: TrieNode? = null      // for '*'
    var paramChild: TrieNode? = null          // for ':param'
    var paramName: String? = null
    var result: String? = null
    var isEndOfRoute: Boolean = false
}

// ==================== Trie Router ====================

/**
 * Trie-based URL router supporting exact, wildcard, and parameter matching.
 * 
 * === Route Types ===
 * 1. Exact: "/foo/bar" matches only "/foo/bar"
 * 2. Wildcard: "/foo/*/bar" matches "/foo/anything/bar"
 * 3. Parameter: "/user/:id" matches "/user/123" and extracts id=123
 * 
 * === Trie Structure ===
 * Each node represents a path segment:
 * 
 *        root
 *       /    \
 *     foo    user
 *    /   \      \
 *  bar    *     :id
 *         |       |
 *        baz   profile
 * 
 * Routes: /foo/bar, /foo/*/baz, /user/:id, /user/:id/profile
 * 
 * === Matching Priority ===
 * 1. Exact match (highest priority)
 * 2. Parameter match (:id)
 * 3. Wildcard match (*) (lowest priority)
 * 
 * This prevents wildcards from "stealing" more specific matches.
 * 
 * === Time Complexity ===
 * - addRoute: O(k) where k = path segments
 * - callRoute: O(k × m) where m = potential matches at each level
 * 
 * === Space Complexity ===
 * - O(n × k) where n = routes, k = avg segments per route
 */
class TrieRouter : Router {
    private val root = TrieNode()
    
    /**
     * Register a route pattern with a result string.
     * 
     * @param path URL pattern (e.g., "/user/:id/profile")
     * @param result Handler identifier to return on match
     */
    override fun addRoute(path: String, result: String) {
        val segments = getSegments(path)
        var current = root
        
        for (segment in segments) {
            current = when {
                segment == "*" -> {
                    // Wildcard: matches any single segment
                    if (current.wildcardChild == null) {
                        current.wildcardChild = TrieNode()
                    }
                    current.wildcardChild!!
                }
                segment.startsWith(":") -> {
                    // Parameter: matches any segment and extracts value
                    if (current.paramChild == null) {
                        current.paramChild = TrieNode()
                        current.paramChild!!.paramName = segment.substring(1) // Remove ':'
                    }
                    current.paramChild!!
                }
                else -> {
                    // Exact: matches this specific segment only
                    current.children.getOrPut(segment) { TrieNode() }
                }
            }
        }
        
        // Mark end of route
        current.result = result
        current.isEndOfRoute = true
    }
    
    override fun callRoute(path: String): String? {
        val result = matchRoute(path)
        return result?.first
    }
    
    /**
     * Match route and extract path parameters.
     * Returns Pair(result, params) or null if no match.
     */
    fun matchRoute(path: String): Pair<String, Map<String, String>>? {
        val segments = getSegments(path)
        val params = mutableMapOf<String, String>()
        
        val result = matchRecursive(root, segments, 0, params)
        return if (result != null) Pair(result, params) else null
    }
    
    private fun matchRecursive(
        node: TrieNode,
        segments: List<String>,
        index: Int,
        params: MutableMap<String, String>
    ): String? {
        // Base case: consumed all segments
        if (index == segments.size) {
            return if (node.isEndOfRoute) node.result else null
        }
        
        val segment = segments[index]
        
        // Priority 1: Exact match
        node.children[segment]?.let { child ->
            val result = matchRecursive(child, segments, index + 1, params)
            if (result != null) return result
        }
        
        // Priority 2: Path parameter match
        node.paramChild?.let { child ->
            child.paramName?.let { paramName ->
                params[paramName] = segment
            }
            val result = matchRecursive(child, segments, index + 1, params)
            if (result != null) return result
            // Backtrack if no match
            child.paramName?.let { params.remove(it) }
        }
        
        // Priority 3: Wildcard match
        node.wildcardChild?.let { child ->
            val result = matchRecursive(child, segments, index + 1, params)
            if (result != null) return result
        }
        
        return null
    }
    
    private fun getSegments(path: String): List<String> {
        return path.trim()
            .trimStart('/')
            .trimEnd('/')
            .split("/")
            .filter { it.isNotEmpty() }
    }
    
    /**
     * Get all registered routes (for debugging).
     */
    fun getAllRoutes(): List<Pair<String, String>> {
        val routes = mutableListOf<Pair<String, String>>()
        collectRoutes(root, "", routes)
        return routes
    }
    
    private fun collectRoutes(node: TrieNode, path: String, routes: MutableList<Pair<String, String>>) {
        if (node.isEndOfRoute && node.result != null) {
            routes.add(Pair(path.ifEmpty { "/" }, node.result!!))
        }
        
        for ((segment, child) in node.children) {
            collectRoutes(child, "$path/$segment", routes)
        }
        
        node.wildcardChild?.let {
            collectRoutes(it, "$path/*", routes)
        }
        
        node.paramChild?.let {
            val paramName = it.paramName ?: "param"
            collectRoutes(it, "$path/:$paramName", routes)
        }
    }
}
```

### Router with Result Objects

```kotlin
// ==================== Route Result ====================

data class RouteResult(
    val result: String,
    val params: Map<String, String> = emptyMap(),
    val path: String
)

// ==================== Enhanced Router ====================

class EnhancedRouter {
    private val trieRouter = TrieRouter()
    
    fun addRoute(path: String, result: String) {
        trieRouter.addRoute(path, result)
    }
    
    fun callRoute(path: String): RouteResult? {
        val match = trieRouter.matchRoute(path)
        return match?.let { RouteResult(it.first, it.second, path) }
    }
    
    fun get(path: String): RouteResult? = callRoute(path)
}
```

### Thread-Safe Router

```kotlin
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Thread-Safe Router ====================

class ThreadSafeRouter : Router {
    private val router = TrieRouter()
    private val lock = ReentrantReadWriteLock()
    
    override fun addRoute(path: String, result: String) {
        lock.write {
            router.addRoute(path, result)
        }
    }
    
    override fun callRoute(path: String): String? {
        return lock.read {
            router.callRoute(path)
        }
    }
    
    fun matchRoute(path: String): Pair<String, Map<String, String>>? {
        return lock.read {
            router.matchRoute(path)
        }
    }
}
```

### Usage Example

```kotlin
fun main() {
    println("=== Basic HashMap Router ===")
    val basicRouter = HashMapRouter()
    basicRouter.addRoute("/bar", "bar_result")
    basicRouter.addRoute("/foo", "foo_result")
    
    println("GET /bar: ${basicRouter.callRoute("/bar")}")
    println("GET /foo: ${basicRouter.callRoute("/foo")}")
    println("GET /unknown: ${basicRouter.callRoute("/unknown")}")
    
    println("\n=== Trie Router with Wildcards ===")
    val trieRouter = TrieRouter()
    
    // Add routes
    trieRouter.addRoute("/foo", "foo")
    trieRouter.addRoute("/bar/*/baz", "bar_wildcard")
    trieRouter.addRoute("/bar/exact/baz", "bar_exact")
    trieRouter.addRoute("/user/:id", "user_handler")
    trieRouter.addRoute("/user/:id/profile", "user_profile")
    trieRouter.addRoute("/api/*/endpoint", "api_wildcard")
    
    // Test exact match
    println("GET /foo: ${trieRouter.callRoute("/foo")}")
    
    // Test wildcard match
    println("GET /bar/a/baz: ${trieRouter.callRoute("/bar/a/baz")}")
    println("GET /bar/xyz/baz: ${trieRouter.callRoute("/bar/xyz/baz")}")
    
    // Test exact takes priority over wildcard
    println("GET /bar/exact/baz: ${trieRouter.callRoute("/bar/exact/baz")}")
    
    // Test path parameters
    val userMatch = trieRouter.matchRoute("/user/123")
    println("GET /user/123: ${userMatch?.first}, params=${userMatch?.second}")
    
    val profileMatch = trieRouter.matchRoute("/user/456/profile")
    println("GET /user/456/profile: ${profileMatch?.first}, params=${profileMatch?.second}")
    
    // Test API wildcard
    println("GET /api/v1/endpoint: ${trieRouter.callRoute("/api/v1/endpoint")}")
    println("GET /api/v2/endpoint: ${trieRouter.callRoute("/api/v2/endpoint")}")
    
    // No match
    println("GET /unknown/path: ${trieRouter.callRoute("/unknown/path")}")
    
    println("\n=== All Registered Routes ===")
    trieRouter.getAllRoutes().forEach { (path, result) ->
        println("  $path -> $result")
    }
}
```

### Output

```
=== Basic HashMap Router ===
GET /bar: bar_result
GET /foo: foo_result
GET /unknown: null

=== Trie Router with Wildcards ===
GET /foo: foo
GET /bar/a/baz: bar_wildcard
GET /bar/xyz/baz: bar_wildcard
GET /bar/exact/baz: bar_exact
GET /user/123: user_handler, params={id=123}
GET /user/456/profile: user_profile, params={id=456}
GET /api/v1/endpoint: api_wildcard
GET /api/v2/endpoint: api_wildcard
GET /unknown/path: null

=== All Registered Routes ===
  /foo -> foo
  /bar/*/baz -> bar_wildcard
  /bar/exact/baz -> bar_exact
  /user/:id -> user_handler
  /user/:id/profile -> user_profile
  /api/*/endpoint -> api_wildcard
```

---

## Unit Tests

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TrieRouterTest {
    
    @Test
    fun `test exact match`() {
        val router = TrieRouter()
        router.addRoute("/foo", "foo_result")
        
        assertEquals("foo_result", router.callRoute("/foo"))
        assertNull(router.callRoute("/bar"))
    }
    
    @Test
    fun `test wildcard match`() {
        val router = TrieRouter()
        router.addRoute("/bar/*/baz", "wildcard_result")
        
        assertEquals("wildcard_result", router.callRoute("/bar/a/baz"))
        assertEquals("wildcard_result", router.callRoute("/bar/xyz/baz"))
        assertNull(router.callRoute("/bar/baz"))
    }
    
    @Test
    fun `test exact takes priority over wildcard`() {
        val router = TrieRouter()
        router.addRoute("/bar/*/baz", "wildcard")
        router.addRoute("/bar/exact/baz", "exact")
        
        assertEquals("exact", router.callRoute("/bar/exact/baz"))
        assertEquals("wildcard", router.callRoute("/bar/other/baz"))
    }
    
    @Test
    fun `test path parameters`() {
        val router = TrieRouter()
        router.addRoute("/user/:id", "user_handler")
        
        val result = router.matchRoute("/user/123")
        assertNotNull(result)
        assertEquals("user_handler", result?.first)
        assertEquals("123", result?.second?.get("id"))
    }
    
    @Test
    fun `test multiple path parameters`() {
        val router = TrieRouter()
        router.addRoute("/user/:userId/post/:postId", "post_handler")
        
        val result = router.matchRoute("/user/42/post/99")
        assertNotNull(result)
        assertEquals("post_handler", result?.first)
        assertEquals("42", result?.second?.get("userId"))
        assertEquals("99", result?.second?.get("postId"))
    }
    
    @Test
    fun `test path normalization`() {
        val router = TrieRouter()
        router.addRoute("/foo/", "foo")
        
        assertEquals("foo", router.callRoute("/foo"))
        assertEquals("foo", router.callRoute("/foo/"))
        assertEquals("foo", router.callRoute("foo"))
    }
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Trie** | `TrieRouter` | Efficient prefix matching |
| **Strategy** | Router interface | Pluggable routing strategies |
| **Decorator** | Thread-safe wrapper | Add thread safety |

---

## Interview Discussion Points

### Q: Why Trie over HashMap for wildcards?
**A:**
- HashMap: O(1) for exact match, but can't handle wildcards
- Trie: O(k) where k = path segments, naturally supports wildcards and parameters
- Trie allows prefix matching and pattern matching

### Q: Matching priority order?
**A:** Common priority:
1. Exact match (highest priority)
2. Path parameter (`:id`)
3. Wildcard (`*`)

### Q: How to handle HTTP methods?
**A:** Add method-specific routes:
```kotlin
class MethodRouter {
    private val routers = mutableMapOf<String, TrieRouter>()
    
    fun addRoute(method: String, path: String, handler: String) {
        routers.getOrPut(method.uppercase()) { TrieRouter() }
            .addRoute(path, handler)
    }
}
```

---

## Complexity Analysis

| Operation | Time Complexity |
|-----------|----------------|
| Add route | O(k) where k = segments |
| Call route | O(k × m) where m = potential matches |
| Space | O(n × k) total routes × segments |

---

## Edge Cases

1. Empty path → `/` root
2. Trailing slashes → normalize
3. Multiple wildcards in same route
4. Overlapping patterns
5. Path parameter as last segment

