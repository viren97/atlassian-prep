# Graph Algorithms - Complete Implementation Guide

## Graph Representation

### Adjacency List (Most Common)

```python
# Using defaultdict
from collections import defaultdict

# Undirected graph
graph = defaultdict(list)
for u, v in edges:
    graph[u].append(v)
    graph[v].append(u)

# Directed graph
graph = defaultdict(list)
for u, v in edges:
    graph[u].append(v)

# Weighted graph
graph = defaultdict(list)
for u, v, weight in edges:
    graph[u].append((v, weight))
```

### Adjacency Matrix

```python
# For dense graphs or when checking edge existence frequently
n = number_of_nodes
matrix = [[0] * n for _ in range(n)]
for u, v in edges:
    matrix[u][v] = 1
    matrix[v][u] = 1  # For undirected
```

---

## 1. Depth-First Search (DFS)

### Recursive DFS

```python
def dfs_recursive(graph, start, visited=None):
    if visited is None:
        visited = set()
    
    visited.add(start)
    print(start)  # Process node
    
    for neighbor in graph[start]:
        if neighbor not in visited:
            dfs_recursive(graph, neighbor, visited)
    
    return visited
```

### Iterative DFS (Using Stack)

```python
def dfs_iterative(graph, start):
    visited = set()
    stack = [start]
    
    while stack:
        node = stack.pop()
        
        if node in visited:
            continue
        
        visited.add(node)
        print(node)  # Process node
        
        # Add neighbors to stack
        for neighbor in graph[node]:
            if neighbor not in visited:
                stack.append(neighbor)
    
    return visited
```

### DFS with Path Tracking

```python
def dfs_path(graph, start, end):
    """Find path from start to end"""
    visited = set()
    
    def dfs(node, path):
        if node == end:
            return path
        
        visited.add(node)
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                result = dfs(neighbor, path + [neighbor])
                if result:
                    return result
        
        return None
    
    return dfs(start, [start])
```

### DFS for Connected Components

```python
def count_components(n, edges):
    graph = defaultdict(list)
    for u, v in edges:
        graph[u].append(v)
        graph[v].append(u)
    
    visited = set()
    components = 0
    
    def dfs(node):
        visited.add(node)
        for neighbor in graph[node]:
            if neighbor not in visited:
                dfs(neighbor)
    
    for node in range(n):
        if node not in visited:
            dfs(node)
            components += 1
    
    return components
```

### DFS for Cycle Detection (Undirected)

```python
def has_cycle_undirected(graph, n):
    visited = set()
    
    def dfs(node, parent):
        visited.add(node)
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                if dfs(neighbor, node):
                    return True
            elif neighbor != parent:
                # Found a back edge (cycle)
                return True
        
        return False
    
    for node in range(n):
        if node not in visited:
            if dfs(node, -1):
                return True
    
    return False
```

### DFS for Cycle Detection (Directed)

```python
def has_cycle_directed(graph, n):
    WHITE, GRAY, BLACK = 0, 1, 2
    color = [WHITE] * n
    
    def dfs(node):
        color[node] = GRAY  # Being processed
        
        for neighbor in graph[node]:
            if color[neighbor] == GRAY:
                # Back edge to ancestor (cycle)
                return True
            if color[neighbor] == WHITE:
                if dfs(neighbor):
                    return True
        
        color[node] = BLACK  # Finished processing
        return False
    
    for node in range(n):
        if color[node] == WHITE:
            if dfs(node):
                return True
    
    return False
```

---

## 2. Breadth-First Search (BFS)

### Basic BFS

```python
from collections import deque

def bfs(graph, start):
    visited = set([start])
    queue = deque([start])
    
    while queue:
        node = queue.popleft()
        print(node)  # Process node
        
        for neighbor in graph[node]:
            if neighbor not in visited:
                visited.add(neighbor)
                queue.append(neighbor)
    
    return visited
```

### BFS for Shortest Path (Unweighted)

```python
def shortest_path_bfs(graph, start, end):
    if start == end:
        return [start]
    
    visited = set([start])
    queue = deque([(start, [start])])
    
    while queue:
        node, path = queue.popleft()
        
        for neighbor in graph[node]:
            if neighbor == end:
                return path + [neighbor]
            
            if neighbor not in visited:
                visited.add(neighbor)
                queue.append((neighbor, path + [neighbor]))
    
    return []  # No path found
```

### BFS with Distance Tracking

```python
def bfs_distances(graph, start):
    """Return distance from start to all reachable nodes"""
    distances = {start: 0}
    queue = deque([start])
    
    while queue:
        node = queue.popleft()
        
        for neighbor in graph[node]:
            if neighbor not in distances:
                distances[neighbor] = distances[node] + 1
                queue.append(neighbor)
    
    return distances
```

### Multi-Source BFS

```python
def multi_source_bfs(grid, sources):
    """BFS from multiple starting points simultaneously"""
    rows, cols = len(grid), len(grid[0])
    visited = set(sources)
    queue = deque(sources)
    distance = 0
    
    while queue:
        for _ in range(len(queue)):
            r, c = queue.popleft()
            
            for dr, dc in [(0, 1), (0, -1), (1, 0), (-1, 0)]:
                nr, nc = r + dr, c + dc
                if 0 <= nr < rows and 0 <= nc < cols:
                    if (nr, nc) not in visited and grid[nr][nc] == 0:
                        visited.add((nr, nc))
                        queue.append((nr, nc))
        
        distance += 1
    
    return distance - 1
```

### 0-1 BFS (Edges with weight 0 or 1)

```python
def bfs_01(graph, start, n):
    """
    For graphs with edge weights 0 or 1.
    Use deque: add weight-0 edges to front, weight-1 to back.
    """
    dist = [float('inf')] * n
    dist[start] = 0
    dq = deque([start])
    
    while dq:
        node = dq.popleft()
        
        for neighbor, weight in graph[node]:
            new_dist = dist[node] + weight
            if new_dist < dist[neighbor]:
                dist[neighbor] = new_dist
                if weight == 0:
                    dq.appendleft(neighbor)  # Front for 0-weight
                else:
                    dq.append(neighbor)      # Back for 1-weight
    
    return dist
```

### Level-Order BFS (Trees)

```python
def level_order(root):
    if not root:
        return []
    
    result = []
    queue = deque([root])
    
    while queue:
        level = []
        level_size = len(queue)
        
        for _ in range(level_size):
            node = queue.popleft()
            level.append(node.val)
            
            if node.left:
                queue.append(node.left)
            if node.right:
                queue.append(node.right)
        
        result.append(level)
    
    return result
```

---

## 3. Topological Sort

### Kahn's Algorithm (BFS-based)

```python
from collections import deque

def topological_sort_kahn(graph, n):
    """
    Returns topological order or empty list if cycle exists.
    graph[u] contains neighbors that u points to.
    """
    # Calculate in-degrees
    in_degree = [0] * n
    for u in range(n):
        for v in graph[u]:
            in_degree[v] += 1
    
    # Start with nodes that have no incoming edges
    queue = deque([i for i in range(n) if in_degree[i] == 0])
    result = []
    
    while queue:
        node = queue.popleft()
        result.append(node)
        
        for neighbor in graph[node]:
            in_degree[neighbor] -= 1
            if in_degree[neighbor] == 0:
                queue.append(neighbor)
    
    # Check if all nodes are included (no cycle)
    if len(result) != n:
        return []  # Cycle detected
    
    return result
```

### DFS-based Topological Sort

```python
def topological_sort_dfs(graph, n):
    """
    Post-order DFS, then reverse.
    """
    WHITE, GRAY, BLACK = 0, 1, 2
    color = [WHITE] * n
    result = []
    has_cycle = False
    
    def dfs(node):
        nonlocal has_cycle
        color[node] = GRAY
        
        for neighbor in graph[node]:
            if color[neighbor] == GRAY:
                has_cycle = True
                return
            if color[neighbor] == WHITE:
                dfs(neighbor)
        
        color[node] = BLACK
        result.append(node)  # Add to result when done
    
    for node in range(n):
        if color[node] == WHITE:
            dfs(node)
    
    if has_cycle:
        return []
    
    return result[::-1]  # Reverse for correct order
```

### Course Schedule Problem

```python
def canFinish(numCourses, prerequisites):
    """
    LeetCode 207: Can finish all courses?
    prerequisites[i] = [a, b] means b -> a (b must come before a)
    """
    graph = defaultdict(list)
    in_degree = [0] * numCourses
    
    for course, prereq in prerequisites:
        graph[prereq].append(course)
        in_degree[course] += 1
    
    queue = deque([i for i in range(numCourses) if in_degree[i] == 0])
    count = 0
    
    while queue:
        node = queue.popleft()
        count += 1
        
        for neighbor in graph[node]:
            in_degree[neighbor] -= 1
            if in_degree[neighbor] == 0:
                queue.append(neighbor)
    
    return count == numCourses
```

### Find All Topological Orderings

```python
def all_topological_sorts(graph, n):
    """Find all possible topological orderings"""
    in_degree = [0] * n
    for u in range(n):
        for v in graph[u]:
            in_degree[v] += 1
    
    result = []
    visited = [False] * n
    path = []
    
    def backtrack():
        if len(path) == n:
            result.append(path[:])
            return
        
        for node in range(n):
            if not visited[node] and in_degree[node] == 0:
                # Choose
                visited[node] = True
                path.append(node)
                for neighbor in graph[node]:
                    in_degree[neighbor] -= 1
                
                # Explore
                backtrack()
                
                # Unchoose
                visited[node] = False
                path.pop()
                for neighbor in graph[node]:
                    in_degree[neighbor] += 1
    
    backtrack()
    return result
```

---

## 4. Dijkstra's Algorithm (Shortest Path - Weighted)

```python
import heapq

def dijkstra(graph, start, n):
    """
    Shortest path from start to all nodes.
    graph[u] = [(v, weight), ...]
    Returns distances array.
    """
    dist = [float('inf')] * n
    dist[start] = 0
    
    # Min heap: (distance, node)
    heap = [(0, start)]
    
    while heap:
        d, u = heapq.heappop(heap)
        
        # Skip if we've found a better path
        if d > dist[u]:
            continue
        
        for v, weight in graph[u]:
            new_dist = dist[u] + weight
            if new_dist < dist[v]:
                dist[v] = new_dist
                heapq.heappush(heap, (new_dist, v))
    
    return dist


def dijkstra_with_path(graph, start, end, n):
    """Dijkstra with path reconstruction"""
    dist = [float('inf')] * n
    dist[start] = 0
    parent = [-1] * n
    
    heap = [(0, start)]
    
    while heap:
        d, u = heapq.heappop(heap)
        
        if u == end:
            break
        
        if d > dist[u]:
            continue
        
        for v, weight in graph[u]:
            new_dist = dist[u] + weight
            if new_dist < dist[v]:
                dist[v] = new_dist
                parent[v] = u
                heapq.heappush(heap, (new_dist, v))
    
    # Reconstruct path
    path = []
    node = end
    while node != -1:
        path.append(node)
        node = parent[node]
    
    return dist[end], path[::-1]
```

---

## 5. Bellman-Ford Algorithm

```python
def bellman_ford(edges, n, start):
    """
    Handles negative weights. Detects negative cycles.
    edges = [(u, v, weight), ...]
    """
    dist = [float('inf')] * n
    dist[start] = 0
    
    # Relax all edges (n-1) times
    for _ in range(n - 1):
        for u, v, weight in edges:
            if dist[u] != float('inf') and dist[u] + weight < dist[v]:
                dist[v] = dist[u] + weight
    
    # Check for negative cycle
    for u, v, weight in edges:
        if dist[u] != float('inf') and dist[u] + weight < dist[v]:
            return None  # Negative cycle exists
    
    return dist
```

---

## 6. Floyd-Warshall Algorithm

```python
def floyd_warshall(n, edges):
    """
    All-pairs shortest path.
    O(n³) time, O(n²) space.
    """
    # Initialize distance matrix
    dist = [[float('inf')] * n for _ in range(n)]
    
    for i in range(n):
        dist[i][i] = 0
    
    for u, v, weight in edges:
        dist[u][v] = weight
    
    # DP: try each intermediate node
    for k in range(n):
        for i in range(n):
            for j in range(n):
                if dist[i][k] + dist[k][j] < dist[i][j]:
                    dist[i][j] = dist[i][k] + dist[k][j]
    
    return dist
```

---

## 7. Union-Find (Disjoint Set Union)

```python
class UnionFind:
    def __init__(self, n):
        self.parent = list(range(n))
        self.rank = [0] * n
        self.count = n  # Number of components
    
    def find(self, x):
        if self.parent[x] != x:
            self.parent[x] = self.find(self.parent[x])  # Path compression
        return self.parent[x]
    
    def union(self, x, y):
        px, py = self.find(x), self.find(y)
        if px == py:
            return False  # Already connected
        
        # Union by rank
        if self.rank[px] < self.rank[py]:
            px, py = py, px
        self.parent[py] = px
        if self.rank[px] == self.rank[py]:
            self.rank[px] += 1
        
        self.count -= 1
        return True
    
    def connected(self, x, y):
        return self.find(x) == self.find(y)
    
    def get_count(self):
        return self.count


# Usage: Number of Connected Components
def count_components_uf(n, edges):
    uf = UnionFind(n)
    for u, v in edges:
        uf.union(u, v)
    return uf.get_count()


# Usage: Detect Cycle in Undirected Graph
def has_cycle_uf(n, edges):
    uf = UnionFind(n)
    for u, v in edges:
        if uf.connected(u, v):
            return True  # Adding edge creates cycle
        uf.union(u, v)
    return False
```

---

## 8. Minimum Spanning Tree

### Kruskal's Algorithm (Union-Find based)

```python
def kruskal(n, edges):
    """
    edges = [(u, v, weight), ...]
    Returns MST edges and total weight.
    """
    # Sort edges by weight
    edges.sort(key=lambda x: x[2])
    
    uf = UnionFind(n)
    mst = []
    total_weight = 0
    
    for u, v, weight in edges:
        if uf.union(u, v):
            mst.append((u, v, weight))
            total_weight += weight
            
            if len(mst) == n - 1:
                break
    
    return mst, total_weight
```

### Prim's Algorithm (Heap based)

```python
def prim(graph, n):
    """
    graph[u] = [(v, weight), ...]
    Start from node 0.
    """
    visited = [False] * n
    mst = []
    total_weight = 0
    
    # Min heap: (weight, u, v)
    heap = [(0, -1, 0)]  # Start from node 0
    
    while heap and len(mst) < n:
        weight, parent, u = heapq.heappop(heap)
        
        if visited[u]:
            continue
        
        visited[u] = True
        if parent != -1:
            mst.append((parent, u, weight))
            total_weight += weight
        
        for v, w in graph[u]:
            if not visited[v]:
                heapq.heappush(heap, (w, u, v))
    
    return mst, total_weight
```

---

## 9. Bipartite Check

```python
def is_bipartite(graph, n):
    """Check if graph can be 2-colored"""
    color = [-1] * n
    
    def bfs(start):
        queue = deque([start])
        color[start] = 0
        
        while queue:
            node = queue.popleft()
            
            for neighbor in graph[node]:
                if color[neighbor] == -1:
                    color[neighbor] = 1 - color[node]
                    queue.append(neighbor)
                elif color[neighbor] == color[node]:
                    return False
        
        return True
    
    for node in range(n):
        if color[node] == -1:
            if not bfs(node):
                return False
    
    return True
```

---

## 10. Strongly Connected Components (Kosaraju's)

```python
def kosaraju_scc(graph, n):
    """
    Find strongly connected components in directed graph.
    """
    # Step 1: Get finish order using DFS
    visited = [False] * n
    finish_order = []
    
    def dfs1(node):
        visited[node] = True
        for neighbor in graph[node]:
            if not visited[neighbor]:
                dfs1(neighbor)
        finish_order.append(node)
    
    for i in range(n):
        if not visited[i]:
            dfs1(i)
    
    # Step 2: Build reverse graph
    reverse_graph = defaultdict(list)
    for u in range(n):
        for v in graph[u]:
            reverse_graph[v].append(u)
    
    # Step 3: DFS on reverse graph in reverse finish order
    visited = [False] * n
    sccs = []
    
    def dfs2(node, component):
        visited[node] = True
        component.append(node)
        for neighbor in reverse_graph[node]:
            if not visited[neighbor]:
                dfs2(neighbor, component)
    
    for node in reversed(finish_order):
        if not visited[node]:
            component = []
            dfs2(node, component)
            sccs.append(component)
    
    return sccs
```

---

## Quick Reference: When to Use Which Algorithm

| Problem | Algorithm | Time Complexity |
|---------|-----------|-----------------|
| Traverse all nodes | DFS or BFS | O(V + E) |
| Shortest path (unweighted) | BFS | O(V + E) |
| Shortest path (weighted, positive) | Dijkstra | O((V + E) log V) |
| Shortest path (negative weights) | Bellman-Ford | O(VE) |
| All-pairs shortest path | Floyd-Warshall | O(V³) |
| Detect cycle (undirected) | DFS or Union-Find | O(V + E) |
| Detect cycle (directed) | DFS with colors | O(V + E) |
| Topological sort | Kahn's or DFS | O(V + E) |
| Connected components | DFS/BFS or Union-Find | O(V + E) |
| MST | Kruskal or Prim | O(E log E) |
| Bipartite check | BFS/DFS coloring | O(V + E) |
| SCC | Kosaraju or Tarjan | O(V + E) |

