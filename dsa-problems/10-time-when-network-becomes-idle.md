# The Time When the Network Becomes Idle

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 60.5%
- **Acceptance Rate**: 53.7%
- **Topics**: Array, Breadth-First Search, Graph
- **LeetCode Link**: https://leetcode.com/problems/the-time-when-the-network-becomes-idle

## Problem Description

There is a network of `n` servers, labeled from `0` to `n - 1`. You are given a 2D integer array `edges`, where `edges[i] = [ui, vi]` indicates there is a message channel between servers `ui` and `vi`, and they can pass any number of messages to each other directly in one second. You are also given a **0-indexed** integer array `patience`, where `patience[i]` is the patience of the `i-th` server.

All servers are **connected**, i.e., a message can be passed from one server to any other server(s) directly or indirectly through the message channels.

The server labeled `0` is the **master** server. The rest are **data** servers. Each data server needs to send its message to the master server for processing and wait for a reply. Messages move between servers optimally, so every message takes the **least amount of time** to arrive at the master server. The master server will process all newly arrived messages instantly and send a reply to the originating server via the **reversed path** the message had gone through.

At the beginning of second `0`, each data server sends its message to be processed. Starting from second `1`, at the beginning of **every** second, each data server will check if it has received a reply to the message it sent (including any previously resent messages) from the master server:

- If it has not, it will **resend** the message periodically. The data server `i` will resend the message every `patience[i]` second(s), i.e., the data server `i` will resend a message at second `patience[i]`, `2 * patience[i]`, `3 * patience[i]`, and so on.
- Otherwise, **no more resending** will occur from this server.

The network becomes **idle** when there are **no messages** passing between servers or arriving at servers.

Return the **earliest second** starting from which the network becomes idle.

## Examples

### Example 1:
```
Input: edges = [[0,1],[1,2]], patience = [0,2,1]
Output: 8
Explanation:
- At second 0, servers 1 and 2 send their messages. Message paths: 1→0, 2→1→0
- Server 1: distance = 1, round trip = 2
- Server 2: distance = 2, round trip = 4
- Server 1 (patience=2): Resends at second 2. First reply arrives at second 2.
  Reply from resent message arrives at second 4. Last message received at second 4.
- Server 2 (patience=1): Resends at seconds 1, 2, 3. First reply arrives at second 4.
  Messages sent at 1, 2, 3 have replies at 5, 6, 7.
- Network idle at second 8 (after all messages processed)
```

### Example 2:
```
Input: edges = [[0,1],[0,2],[1,2]], patience = [0,10,10]
Output: 3
Explanation:
Both servers 1 and 2 are directly connected to 0 (distance = 1, round trip = 2).
Since patience = 10 > round trip = 2, no resending occurs.
Last reply arrives at second 2. Network idle at second 3.
```

## Constraints

- `n == patience.length`
- `2 <= n <= 10^5`
- `patience[0] == 0`
- `1 <= patience[i] <= 10^5` for `1 <= i < n`
- `1 <= edges.length <= min(10^5, n * (n - 1) / 2)`
- `edges[i].length == 2`
- `0 <= ui, vi < n`
- `ui != vi`
- There are no duplicate edges.
- Each server can directly or indirectly reach another server.

## Approach

### Key Insights:
1. Use BFS from node 0 to find shortest distance to all servers
2. Round trip time = 2 × distance
3. For each server, find when its LAST resent message's reply arrives
4. Network becomes idle 1 second after all replies are received

### For each server i:
- `round_trip = 2 * dist[i]`
- Last resend time = largest multiple of patience[i] that is < round_trip
- Last resend time = `((round_trip - 1) // patience[i]) * patience[i]`
- Last reply time = last_resend_time + round_trip
- Network idle = max(all last reply times) + 1

## Solution

```python
from collections import deque, defaultdict
from typing import List

class Solution:
    def networkBecomesIdle(self, edges: List[List[int]], patience: List[int]) -> int:
        n = len(patience)
        
        # Build adjacency list
        graph = defaultdict(list)
        for u, v in edges:
            graph[u].append(v)
            graph[v].append(u)
        
        # BFS from node 0 to find shortest distances
        dist = [-1] * n
        dist[0] = 0
        queue = deque([0])
        
        while queue:
            node = queue.popleft()
            for neighbor in graph[node]:
                if dist[neighbor] == -1:
                    dist[neighbor] = dist[node] + 1
                    queue.append(neighbor)
        
        # Calculate when network becomes idle
        max_time = 0
        
        for i in range(1, n):  # Skip master server (index 0)
            round_trip = 2 * dist[i]
            p = patience[i]
            
            # Last resend happens at the largest multiple of p that is < round_trip
            # If round_trip <= p, no resend (last_resend = 0)
            if round_trip <= p:
                last_resend = 0
            else:
                # Number of resends before first reply = (round_trip - 1) // p
                # Last resend time = ((round_trip - 1) // p) * p
                last_resend = ((round_trip - 1) // p) * p
            
            # Time when last reply arrives
            last_reply_time = last_resend + round_trip
            max_time = max(max_time, last_reply_time)
        
        # Network is idle 1 second after last message is received
        return max_time + 1
```

## Detailed Solution with Comments

```python
from collections import deque, defaultdict
from typing import List

class Solution:
    def networkBecomesIdle(self, edges: List[List[int]], patience: List[int]) -> int:
        """
        Timeline for server i:
        - t=0: Send first message
        - t=patience[i]: Resend if no reply yet
        - t=2*patience[i]: Resend if no reply yet
        - ...
        - t=round_trip: First reply arrives, stop resending
        
        Last resend time = largest k*patience[i] such that k*patience[i] < round_trip
        Last reply arrives at: last_resend + round_trip
        """
        n = len(patience)
        
        # Step 1: Build graph
        graph = defaultdict(list)
        for u, v in edges:
            graph[u].append(v)
            graph[v].append(u)
        
        # Step 2: BFS to find shortest distance from master (node 0) to all nodes
        dist = [float('inf')] * n
        dist[0] = 0
        queue = deque([0])
        
        while queue:
            curr = queue.popleft()
            for next_node in graph[curr]:
                if dist[next_node] == float('inf'):
                    dist[next_node] = dist[curr] + 1
                    queue.append(next_node)
        
        # Step 3: Calculate last message arrival time for each server
        latest_time = 0
        
        for server in range(1, n):
            round_trip = 2 * dist[server]
            p = patience[server]
            
            # Calculate last resend time
            # Resend happens at: p, 2p, 3p, ... as long as < round_trip
            # Last resend = floor((round_trip - 1) / p) * p
            
            if p >= round_trip:
                # No resending - first reply arrives before any resend
                last_message_time = round_trip
            else:
                # How many resends happen?
                # Resends at: p, 2p, ..., kp where kp < round_trip
                # k = floor((round_trip - 1) / p)
                num_resends = (round_trip - 1) // p
                last_resend_time = num_resends * p
                last_message_time = last_resend_time + round_trip
            
            latest_time = max(latest_time, last_message_time)
        
        # Network is idle at the next second after last message
        return latest_time + 1
```

## Complexity Analysis

### Time Complexity: O(V + E)
- Building graph: O(E)
- BFS: O(V + E)
- Calculating times: O(V)
- Where V = number of servers, E = number of edges

### Space Complexity: O(V + E)
- Graph adjacency list: O(V + E)
- Distance array: O(V)
- BFS queue: O(V)

## Visual Example

```
Network:  0 --- 1 --- 2
Patience: [0,   2,    1]

Distances from master (0):
- Node 1: dist = 1, round_trip = 2
- Node 2: dist = 2, round_trip = 4

Server 1 (patience = 2):
- t=0: Send message
- t=2: First reply arrives, no resend needed (2 >= round_trip)
- Last message at t=2

Server 2 (patience = 1):
- t=0: Send message
- t=1: Resend (1 < 4)
- t=2: Resend (2 < 4)  
- t=3: Resend (3 < 4)
- t=4: First reply arrives, stop resending
- Last resend at t=3, reply arrives at t=3+4=7
- Last message at t=7

Network idle at: max(2, 7) + 1 = 8
```

## Key Patterns & Techniques

1. **BFS for Shortest Path**: Find minimum distance from source to all nodes
2. **Mathematical Formula**: Calculate last resend time without simulation
3. **Modular Arithmetic**: Finding largest multiple less than a value

## Formula Derivation

```
For server i:
- Round trip time: R = 2 * distance[i]
- Patience: P = patience[i]

Resends occur at: P, 2P, 3P, ... while time < R

Last resend time = k * P where k is largest integer such that k*P < R
k = floor((R - 1) / P)
last_resend = k * P = floor((R - 1) / P) * P

Special case: If R <= P, no resend occurs (last_resend = 0)

Last reply time = last_resend + R
```

## Common Mistakes to Avoid

1. Forgetting the "+1" at the end (idle is AFTER last message)
2. Using <= instead of < for resend condition
3. Not handling the case where patience >= round_trip (no resends)
4. Including master server (index 0) in calculations

## Related Problems

- [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/)
- [1334. Find the City With the Smallest Number of Neighbors at a Threshold Distance](https://leetcode.com/problems/find-the-city-with-the-smallest-number-of-neighbors-at-a-threshold-distance/)
- [994. Rotting Oranges](https://leetcode.com/problems/rotting-oranges/)

