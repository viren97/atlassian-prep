# Lowest Common Ancestor of a Binary Tree III

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 82.5%
- **Topics**: Hash Table, Two Pointers, Tree, Binary Tree
- **LeetCode Link**: https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree-iii

## Problem Description

Given two nodes of a binary tree `p` and `q`, return their lowest common ancestor (LCA).

Each node will have a reference to its parent node. The definition for `Node` is below:

```
class Node {
    public int val;
    public Node left;
    public Node right;
    public Node parent;
}
```

According to the definition of LCA on Wikipedia: "The lowest common ancestor of two nodes p and q in a tree T is the lowest node that has both p and q as descendants (where we allow **a node to be a descendant of itself**)."

## Examples

### Example 1:
```
        3
       / \
      5   1
     / \ / \
    6  2 0  8
      / \
     7   4

Input: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 1
Output: 3
Explanation: The LCA of nodes 5 and 1 is 3.
```

### Example 2:
```
        3
       / \
      5   1
     / \ / \
    6  2 0  8
      / \
     7   4

Input: root = [3,5,1,6,2,0,8,null,null,7,4], p = 5, q = 4
Output: 5
Explanation: The LCA of nodes 5 and 4 is 5, since a node can be a descendant of itself.
```

### Example 3:
```
Input: root = [1,2], p = 1, q = 2
Output: 1
```

## Constraints

- The number of nodes in the tree is in the range `[2, 10^5]`.
- `-10^9 <= Node.val <= 10^9`
- All `Node.val` are **unique**.
- `p != q`
- `p` and `q` exist in the tree.

## Approach

### Key Insight:
Since each node has a parent pointer, this becomes similar to finding the intersection of two linked lists!

### Approach 1: HashSet
- Traverse from p to root, storing all ancestors
- Traverse from q to root, find first common ancestor

### Approach 2: Two Pointers (Like Linked List Intersection)
- Start two pointers at p and q
- Move each pointer up to parent
- When a pointer reaches root (null), redirect to the other starting point
- They will meet at LCA

## Solution 1: HashSet Approach

```python
class Node:
    def __init__(self, val):
        self.val = val
        self.left = None
        self.right = None
        self.parent = None

class Solution:
    def lowestCommonAncestor(self, p: 'Node', q: 'Node') -> 'Node':
        # Store all ancestors of p
        ancestors = set()
        
        # Traverse from p to root
        current = p
        while current:
            ancestors.add(current)
            current = current.parent
        
        # Find first ancestor of q that's also ancestor of p
        current = q
        while current:
            if current in ancestors:
                return current
            current = current.parent
        
        return None  # Should never reach here if p and q are in same tree
```

## Solution 2: Two Pointers (Optimal)

```python
class Solution:
    def lowestCommonAncestor(self, p: 'Node', q: 'Node') -> 'Node':
        """
        Similar to finding intersection of two linked lists.
        
        Key insight: 
        - Distance from p to root = a
        - Distance from q to root = b
        - Distance from LCA to root = c
        
        If we traverse: p → root → q → LCA
                   and: q → root → p → LCA
        Both paths have the same length: (a + b + c)
        
        So the pointers will meet at LCA!
        """
        pointer_a = p
        pointer_b = q
        
        while pointer_a != pointer_b:
            # Move up, or jump to the other starting point
            pointer_a = pointer_a.parent if pointer_a else q
            pointer_b = pointer_b.parent if pointer_b else p
        
        return pointer_a
```

## Solution 3: Calculate Depths First

```python
class Solution:
    def lowestCommonAncestor(self, p: 'Node', q: 'Node') -> 'Node':
        # Calculate depth of p
        def get_depth(node):
            depth = 0
            while node:
                depth += 1
                node = node.parent
            return depth
        
        depth_p = get_depth(p)
        depth_q = get_depth(q)
        
        # Make p the deeper node
        if depth_p < depth_q:
            p, q = q, p
            depth_p, depth_q = depth_q, depth_p
        
        # Move p up until same depth as q
        diff = depth_p - depth_q
        for _ in range(diff):
            p = p.parent
        
        # Move both up together until they meet
        while p != q:
            p = p.parent
            q = q.parent
        
        return p
```

## Understanding Two Pointers Approach

```
Tree:
        3 (root)
       / \
      5   1
     /
    6

p = 6, q = 1, LCA = 3

Pointer paths:
pointer_a: 6 → 5 → 3 → None → 1 → 3
pointer_b: 1 → 3 → None → 6 → 5 → 3
                              ↑
                         Meet at LCA!

Path lengths:
- 6 to root: 3 steps (6→5→3→None)
- 1 to root: 2 steps (1→3→None)
- Total for pointer_a: 3 + 3 = 6 steps
- Total for pointer_b: 2 + 4 = 6 steps (meets at 3)
```

## Why Two Pointers Works

```
Let:
- a = distance from p to LCA
- b = distance from q to LCA
- c = distance from LCA to root

Path lengths to root:
- p to root = a + c
- q to root = b + c

Two pointer traversal:
- pointer_a: p → root → q → LCA = (a + c) + (b + 0) = a + b + c
- pointer_b: q → root → p → LCA = (b + c) + (a + 0) = a + b + c

Both pointers travel the same distance, meeting at LCA!

When pointer reaches None (root's parent), it jumps to the other node.
After one swap each, both pointers are at same "distance from LCA" away.
```

## Complexity Analysis

### HashSet Solution:
- **Time**: O(h) where h is the height of the tree
- **Space**: O(h) for storing ancestors

### Two Pointers Solution:
- **Time**: O(h) - each pointer traverses at most 2h nodes
- **Space**: O(1) - only two pointer variables

### Depth-based Solution:
- **Time**: O(h) - calculate depths + traverse
- **Space**: O(1)

## Key Patterns & Techniques

1. **Parent Pointer Traversal**: Move from node to ancestor
2. **Two Pointer Technique**: Like finding linked list intersection
3. **HashSet for Ancestor Storage**: O(h) space for O(1) lookup
4. **Depth Equalization**: Bring both nodes to same depth first

## Comparison with LCA without Parent Pointers

| Aspect | With Parent Pointer | Without Parent Pointer |
|--------|---------------------|----------------------|
| Traversal | Bottom-up | Top-down |
| Space | O(1) or O(h) | O(h) recursion stack |
| Approach | Two pointers or HashSet | DFS from root |
| Need root? | No | Yes |

## Common Mistakes to Avoid

1. **Infinite loop**: Make sure to handle None correctly in two pointers
2. **Wrong swap direction**: Jump to the OTHER starting node, not current
3. **Not considering node as its own ancestor**: LCA can be p or q itself

## Edge Cases

1. p is ancestor of q → LCA is p
2. q is ancestor of p → LCA is q
3. p and q are siblings → LCA is their parent
4. p and q are the same node → LCA is that node (though problem says p != q)

## Related Problems

- [236. Lowest Common Ancestor of a Binary Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree/)
- [160. Intersection of Two Linked Lists](https://leetcode.com/problems/intersection-of-two-linked-lists/)
- [235. Lowest Common Ancestor of a Binary Search Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/)
- [1644. Lowest Common Ancestor of a Binary Tree II](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree-ii/)

