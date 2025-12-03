# Network Service Latency (Dijkstra's Algorithm)

## Problem Information
- **Difficulty**: Medium-Hard
- **Company**: Atlassian
- **Topics**: Graph, Dijkstra, Shortest Path, BFS, Floyd-Warshall

## Problem Description

You have N services in a network. You are given which servers are connected and the connection latency. For example `[1, 2, 200]` means service 1 calls service 2 and it has a latency of 200ms.

You are given Q queries where you have to output the minimum latency from Service X to Service Y.

### Example:
```
N = 4 services
Connections: [[1,2,100], [1,3,500], [2,3,100], [3,4,200]]

Query: minLatency(1, 4) → 400 (path: 1 → 2 → 3 → 4)
Query: minLatency(1, 3) → 200 (path: 1 → 2 → 3)
```

---

## Why Not BFS/DFS?

### BFS Limitation
- BFS finds shortest path by **number of edges**, not by **weight**
- BFS assumes all edges have equal weight
- For weighted graphs, BFS may find a path with fewer hops but higher total latency

```
Example: 1 --100--> 2 --100--> 3
         1 --500--------------> 3

BFS might return 1→3 (1 hop, 500ms)
But optimal is 1→2→3 (2 hops, 200ms)
```

### DFS Limitation
- DFS explores deeply, not optimally
- No guarantee of finding shortest path
- Would need to explore ALL paths and compare (exponential)

---

## Solution 1: Dijkstra's Algorithm (Single Source)

**Use when**: Few queries, or queries from same source

**Time**: O((V + E) log V) per query
**Space**: O(V + E)

### Kotlin Implementation

```kotlin
import java.util.PriorityQueue

class NetworkLatency(n: Int, connections: List<IntArray>) {
    
    // Adjacency list: node -> list of (neighbor, latency)
    private val graph = Array(n + 1) { mutableListOf<Pair<Int, Int>>() }
    
    init {
        for ((from, to, latency) in connections) {
            graph[from].add(Pair(to, latency))
            // If bidirectional, uncomment:
            // graph[to].add(Pair(from, latency))
        }
    }
    
    /**
     * Dijkstra's Algorithm
     * Returns minimum latency from source to target
     * Returns -1 if no path exists
     */
    fun minLatency(source: Int, target: Int): Int {
        // Distance array
        val dist = IntArray(graph.size) { Int.MAX_VALUE }
        dist[source] = 0
        
        // Min heap: (distance, node)
        val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
        pq.offer(Pair(0, source))
        
        while (pq.isNotEmpty()) {
            val (d, u) = pq.poll()
            
            // Early termination if we reached target
            if (u == target) {
                return d
            }
            
            // Skip if we've found a better path already
            if (d > dist[u]) continue
            
            // Explore neighbors
            for ((v, latency) in graph[u]) {
                val newDist = dist[u] + latency
                if (newDist < dist[v]) {
                    dist[v] = newDist
                    pq.offer(Pair(newDist, v))
                }
            }
        }
        
        return if (dist[target] == Int.MAX_VALUE) -1 else dist[target]
    }
    
    /**
     * Get all distances from source (useful for multiple queries from same source)
     */
    fun allDistancesFrom(source: Int): IntArray {
        val dist = IntArray(graph.size) { Int.MAX_VALUE }
        dist[source] = 0
        
        val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
        pq.offer(Pair(0, source))
        
        while (pq.isNotEmpty()) {
            val (d, u) = pq.poll()
            
            if (d > dist[u]) continue
            
            for ((v, latency) in graph[u]) {
                val newDist = dist[u] + latency
                if (newDist < dist[v]) {
                    dist[v] = newDist
                    pq.offer(Pair(newDist, v))
                }
            }
        }
        
        return dist
    }
}
```

### Python Implementation

```python
import heapq
from collections import defaultdict
from typing import List, Dict

class NetworkLatency:
    def __init__(self, n: int, connections: List[List[int]]):
        self.n = n
        self.graph = defaultdict(list)
        
        for from_node, to_node, latency in connections:
            self.graph[from_node].append((to_node, latency))
            # If bidirectional:
            # self.graph[to_node].append((from_node, latency))
    
    def min_latency(self, source: int, target: int) -> int:
        """
        Dijkstra's algorithm for single source shortest path
        Time: O((V + E) log V)
        """
        dist = {source: 0}
        heap = [(0, source)]  # (distance, node)
        
        while heap:
            d, u = heapq.heappop(heap)
            
            # Early termination
            if u == target:
                return d
            
            # Skip outdated entries
            if d > dist.get(u, float('inf')):
                continue
            
            for v, latency in self.graph[u]:
                new_dist = d + latency
                if new_dist < dist.get(v, float('inf')):
                    dist[v] = new_dist
                    heapq.heappush(heap, (new_dist, v))
        
        return dist.get(target, -1)
    
    def all_distances_from(self, source: int) -> Dict[int, int]:
        """Get all shortest distances from source"""
        dist = {source: 0}
        heap = [(0, source)]
        
        while heap:
            d, u = heapq.heappop(heap)
            
            if d > dist.get(u, float('inf')):
                continue
            
            for v, latency in self.graph[u]:
                new_dist = d + latency
                if new_dist < dist.get(v, float('inf')):
                    dist[v] = new_dist
                    heapq.heappush(heap, (new_dist, v))
        
        return dist


# Usage
network = NetworkLatency(4, [[1,2,100], [1,3,500], [2,3,100], [3,4,200]])
print(network.min_latency(1, 4))  # 400
print(network.min_latency(1, 3))  # 200
```

---

## Solution 2: Floyd-Warshall (All Pairs)

**Use when**: Many queries between different pairs, small graph (N ≤ 500)

**Time**: O(V³) preprocessing, O(1) per query
**Space**: O(V²)

```kotlin
class NetworkLatencyFloydWarshall(n: Int, connections: List<IntArray>) {
    
    private val INF = Int.MAX_VALUE / 2  // Avoid overflow
    private val dist = Array(n + 1) { IntArray(n + 1) { INF } }
    
    init {
        // Initialize diagonal
        for (i in 1..n) {
            dist[i][i] = 0
        }
        
        // Add edges
        for ((from, to, latency) in connections) {
            dist[from][to] = minOf(dist[from][to], latency)
        }
        
        // Floyd-Warshall
        for (k in 1..n) {
            for (i in 1..n) {
                for (j in 1..n) {
                    if (dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j]
                    }
                }
            }
        }
    }
    
    fun minLatency(source: Int, target: Int): Int {
        val d = dist[source][target]
        return if (d >= INF) -1 else d
    }
}
```

```python
class NetworkLatencyFloydWarshall:
    def __init__(self, n: int, connections: List[List[int]]):
        INF = float('inf')
        
        # Initialize distance matrix
        self.dist = [[INF] * (n + 1) for _ in range(n + 1)]
        
        for i in range(n + 1):
            self.dist[i][i] = 0
        
        for from_node, to_node, latency in connections:
            self.dist[from_node][to_node] = min(
                self.dist[from_node][to_node], 
                latency
            )
        
        # Floyd-Warshall: O(V³)
        for k in range(1, n + 1):
            for i in range(1, n + 1):
                for j in range(1, n + 1):
                    if self.dist[i][k] + self.dist[k][j] < self.dist[i][j]:
                        self.dist[i][j] = self.dist[i][k] + self.dist[k][j]
    
    def min_latency(self, source: int, target: int) -> int:
        d = self.dist[source][target]
        return -1 if d == float('inf') else int(d)
```

---

## Solution 3: Dijkstra with Path Reconstruction

```python
def min_latency_with_path(self, source: int, target: int):
    """
    Returns (min_latency, path)
    """
    dist = {source: 0}
    parent = {source: None}
    heap = [(0, source)]
    
    while heap:
        d, u = heapq.heappop(heap)
        
        if u == target:
            # Reconstruct path
            path = []
            node = target
            while node is not None:
                path.append(node)
                node = parent[node]
            return d, path[::-1]
        
        if d > dist.get(u, float('inf')):
            continue
        
        for v, latency in self.graph[u]:
            new_dist = d + latency
            if new_dist < dist.get(v, float('inf')):
                dist[v] = new_dist
                parent[v] = u
                heapq.heappush(heap, (new_dist, v))
    
    return -1, []
```

---

## Comparison of Approaches

| Approach | Preprocessing | Per Query | Best For |
|----------|---------------|-----------|----------|
| Dijkstra | O(1) | O((V+E) log V) | Few queries, sparse graph |
| Floyd-Warshall | O(V³) | O(1) | Many queries, small graph |
| BFS | O(1) | O(V+E) | Only unweighted graphs |

---

## Interview Discussion Points

### 1. Why Dijkstra over BFS?
- BFS works only for unweighted graphs (all edges = 1)
- Dijkstra handles weighted edges correctly
- BFS finds shortest by hop count, not by total weight

### 2. Why Dijkstra over DFS?
- DFS has no concept of "shortest" - explores arbitrarily
- Would need to explore ALL paths (exponential)
- Dijkstra always explores the current shortest first (greedy)

### 3. Time Complexity Analysis
```
Dijkstra with Binary Heap: O((V + E) log V)
- Each vertex extracted once: O(V log V)
- Each edge relaxed once: O(E log V)

Dijkstra with Fibonacci Heap: O(E + V log V)
- Better for dense graphs

Floyd-Warshall: O(V³)
- Better when Q queries and Q × (V+E)logV > V³
```

### 4. When to use Floyd-Warshall?
- Small graph (V ≤ 500)
- Many queries between different pairs
- Need all-pairs shortest paths
- Graph is dense (E ≈ V²)

### 5. Handling Negative Weights?
- Dijkstra: Does NOT work with negative weights
- Use Bellman-Ford: O(VE)
- Or Floyd-Warshall (handles negative, detects negative cycles)

---

## Edge Cases

1. **No path exists**: Return -1
2. **Source == Target**: Return 0
3. **Multiple edges between same nodes**: Take minimum
4. **Disconnected graph**: Handle unreachable nodes
5. **Self-loops**: Generally ignore or handle based on requirements

---

## Follow-up Questions

### Q: What if connections can be added dynamically?
- For single additions: Re-run Dijkstra from affected nodes
- For many additions: Consider incremental shortest path algorithms

### Q: What if we need K shortest paths?
- Use Yen's algorithm: O(KV(E + V log V))

### Q: What if latency changes over time?
- Use time-expanded graph
- Or maintain separate graphs for different time windows

