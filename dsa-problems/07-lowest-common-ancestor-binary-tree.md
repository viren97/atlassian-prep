# Lowest Common Ancestor of a Binary Tree

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 69.4%
- **Acceptance Rate**: 66.8%
- **Topics**: Tree, Depth-First Search, Binary Tree
- **LeetCode Link**: https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree

## Problem Description

Given a binary tree, find the lowest common ancestor (LCA) of two given nodes in the tree.

According to the definition of LCA on Wikipedia: "The lowest common ancestor is defined between two nodes `p` and `q` as the lowest node in `T` that has both `p` and `q` as descendants (where we allow **a node to be a descendant of itself**)."

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
- `p` and `q` will exist in the tree.

## Approach

### Key Insight:
The LCA has a special property - it's the node where:
- One target is in the left subtree AND another is in the right subtree, OR
- The node itself is one of the targets AND the other is in a subtree

### Recursive Algorithm:
1. If current node is null or matches p or q, return current node
2. Recursively search left and right subtrees
3. If both searches return non-null, current node is LCA
4. If only one returns non-null, propagate that result up

## Solution 1: Recursive DFS

```python
class TreeNode:
    def __init__(self, x):
        self.val = x
        self.left = None
        self.right = None

class Solution:
    def lowestCommonAncestor(self, root: 'TreeNode', p: 'TreeNode', q: 'TreeNode') -> 'TreeNode':
        # Base case: null node or found p or q
        if root is None or root == p or root == q:
            return root
        
        # Search in left and right subtrees
        left = self.lowestCommonAncestor(root.left, p, q)
        right = self.lowestCommonAncestor(root.right, p, q)
        
        # If both left and right are non-null, root is the LCA
        if left and right:
            return root
        
        # If only one is non-null, return that one
        return left if left else right
```

## Solution 2: Iterative with Parent Pointers

```python
class Solution:
    def lowestCommonAncestor(self, root: 'TreeNode', p: 'TreeNode', q: 'TreeNode') -> 'TreeNode':
        # Stack for DFS traversal
        stack = [root]
        
        # Parent pointers
        parent = {root: None}
        
        # Build parent pointers until we find both p and q
        while p not in parent or q not in parent:
            node = stack.pop()
            
            if node.left:
                parent[node.left] = node
                stack.append(node.left)
            
            if node.right:
                parent[node.right] = node
                stack.append(node.right)
        
        # Get all ancestors of p
        ancestors = set()
        while p:
            ancestors.add(p)
            p = parent[p]
        
        # Find first ancestor of q that's also ancestor of p
        while q not in ancestors:
            q = parent[q]
        
        return q
```

## Solution 3: Iterative without Parent Pointers (Stack with State)

```python
class Solution:
    def lowestCommonAncestor(self, root: 'TreeNode', p: 'TreeNode', q: 'TreeNode') -> 'TreeNode':
        BOTH_PENDING = 2
        LEFT_DONE = 1
        BOTH_DONE = 0
        
        stack = [(root, BOTH_PENDING)]
        one_node_found = False
        LCA_index = -1
        
        while stack:
            parent_node, parent_state = stack[-1]
            
            if parent_state != BOTH_DONE:
                if parent_state == BOTH_PENDING:
                    # Check if current node is p or q
                    if parent_node == p or parent_node == q:
                        if one_node_found:
                            return stack[LCA_index][0]
                        else:
                            one_node_found = True
                            LCA_index = len(stack) - 1
                    
                    # Move to left child
                    child = parent_node.left
                else:
                    # Move to right child
                    child = parent_node.right
                
                # Update state
                stack[-1] = (parent_node, parent_state - 1)
                
                if child:
                    stack.append((child, BOTH_PENDING))
            else:
                # Both children processed, pop from stack
                if one_node_found and LCA_index == len(stack) - 1:
                    LCA_index -= 1
                stack.pop()
        
        return None
```

## Understanding the Recursive Solution

```
Example: p = 5, q = 1

                3  ← LCA (left returns 5, right returns 1)
               / \
              5   1
             / \
            6   2

Step-by-step:
1. Start at node 3
2. Go left to node 5
   - Node 5 == p, return 5 immediately
3. Go right to node 1
   - Node 1 == q, return 1 immediately
4. Back at node 3:
   - left = 5 (non-null)
   - right = 1 (non-null)
   - Both non-null! Node 3 is the LCA
```

## Visual Walkthrough

```
For p = 5, q = 4:

        3
       / \
      5   1
     / \ 
    6   2
       / \
      7   4

Call tree:
LCA(3, 5, 4)
├── LCA(5, 5, 4)  → returns 5 (node == p)
│   ├── (short-circuits, doesn't continue down)
└── LCA(1, 5, 4)
    ├── LCA(0, 5, 4) → returns None
    └── LCA(8, 5, 4) → returns None
    → returns None

At node 5: returns 5 immediately (since 5 == p)
At node 3: left = 5, right = None → returns 5

BUT WAIT! The recursive solution actually works correctly:

At node 5: returns 5 (since node == p)
This 5 is returned all the way up.

Actually, let me trace again more carefully:
- We call LCA(3, 5, 4)
- Since 3 != 5 and 3 != 4, we continue
- left = LCA(5, 5, 4)
  - Since 5 == p, return 5 immediately
- So left = 5 (non-null)
- right = LCA(1, 5, 4)
  - 1 != 5 and 1 != 4
  - left = LCA(0, 5, 4) = None
  - right = LCA(8, 5, 4) = None
  - return None (neither found in this subtree)
- So right = None
- left = 5, right = None → return left = 5

But wait, where did we find 4? Let me re-trace:

Actually, node 4 is under node 5! Let me trace again:

LCA(3, 5, 4)
├── left = LCA(5, 5, 4) → returns 5 (node == p)
│   Note: We return immediately without searching subtree!
└── right = LCA(1, 5, 4) → returns None

Result: 5

This works because when we find p=5, we know:
- If q=4 is in the tree and not in right subtree of 3, it must be under p=5
- So p=5 is the LCA
```

## Complexity Analysis

### Recursive Solution:
- **Time**: O(n) - visit each node at most once
- **Space**: O(h) - recursion stack, where h is tree height
  - O(n) worst case for skewed tree
  - O(log n) for balanced tree

### Parent Pointer Solution:
- **Time**: O(n) - build parent map + traverse ancestors
- **Space**: O(n) - parent map storage

## Key Patterns & Techniques

1. **Postorder Traversal**: Process children before parent for bottom-up information flow
2. **Bubble Up Results**: Return found nodes up the call stack
3. **Parent Pointer Construction**: Alternative approach for ancestor queries
4. **Two-set Intersection**: Finding common ancestor via set intersection

## Common Mistakes to Avoid

1. Not handling the case where p or q is the LCA itself
2. Continuing to search after finding a node (in recursive solution, we short-circuit)
3. Confusing BST LCA (which can use value comparison) with general binary tree LCA

## Related Problems

- [235. Lowest Common Ancestor of a Binary Search Tree](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-search-tree/)
- [1644. Lowest Common Ancestor of a Binary Tree II](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree-ii/)
- [1676. Lowest Common Ancestor of a Binary Tree IV](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree-iv/)
- [1650. Lowest Common Ancestor of a Binary Tree III](https://leetcode.com/problems/lowest-common-ancestor-of-a-binary-tree-iii/)

