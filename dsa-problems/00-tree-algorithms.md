# Tree Algorithms - Complete Implementation Guide

## Tree Node Definition

```python
class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val
        self.left = left
        self.right = right
```

---

## 1. Tree Traversals

### Inorder (Left → Root → Right)

```python
# Recursive
def inorder_recursive(root):
    result = []
    
    def traverse(node):
        if not node:
            return
        traverse(node.left)
        result.append(node.val)
        traverse(node.right)
    
    traverse(root)
    return result


# Iterative (Using Stack)
def inorder_iterative(root):
    result = []
    stack = []
    curr = root
    
    while curr or stack:
        # Go to leftmost node
        while curr:
            stack.append(curr)
            curr = curr.left
        
        # Process node
        curr = stack.pop()
        result.append(curr.val)
        
        # Go right
        curr = curr.right
    
    return result
```

### Preorder (Root → Left → Right)

```python
# Recursive
def preorder_recursive(root):
    result = []
    
    def traverse(node):
        if not node:
            return
        result.append(node.val)
        traverse(node.left)
        traverse(node.right)
    
    traverse(root)
    return result


# Iterative
def preorder_iterative(root):
    if not root:
        return []
    
    result = []
    stack = [root]
    
    while stack:
        node = stack.pop()
        result.append(node.val)
        
        # Push right first so left is processed first
        if node.right:
            stack.append(node.right)
        if node.left:
            stack.append(node.left)
    
    return result
```

### Postorder (Left → Right → Root)

```python
# Recursive
def postorder_recursive(root):
    result = []
    
    def traverse(node):
        if not node:
            return
        traverse(node.left)
        traverse(node.right)
        result.append(node.val)
    
    traverse(root)
    return result


# Iterative (Two Stacks)
def postorder_iterative(root):
    if not root:
        return []
    
    result = []
    stack1 = [root]
    stack2 = []
    
    while stack1:
        node = stack1.pop()
        stack2.append(node)
        
        if node.left:
            stack1.append(node.left)
        if node.right:
            stack1.append(node.right)
    
    while stack2:
        result.append(stack2.pop().val)
    
    return result


# Iterative (Single Stack)
def postorder_single_stack(root):
    if not root:
        return []
    
    result = []
    stack = [root]
    
    while stack:
        node = stack.pop()
        result.append(node.val)
        
        if node.left:
            stack.append(node.left)
        if node.right:
            stack.append(node.right)
    
    return result[::-1]  # Reverse at the end
```

### Level Order (BFS)

```python
from collections import deque

def levelOrder(root):
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


# Zigzag Level Order
def zigzagLevelOrder(root):
    if not root:
        return []
    
    result = []
    queue = deque([root])
    left_to_right = True
    
    while queue:
        level = deque()
        level_size = len(queue)
        
        for _ in range(level_size):
            node = queue.popleft()
            
            if left_to_right:
                level.append(node.val)
            else:
                level.appendleft(node.val)
            
            if node.left:
                queue.append(node.left)
            if node.right:
                queue.append(node.right)
        
        result.append(list(level))
        left_to_right = not left_to_right
    
    return result
```

---

## 2. Tree Properties

### Height/Depth of Tree

```python
def maxDepth(root):
    if not root:
        return 0
    
    return 1 + max(maxDepth(root.left), maxDepth(root.right))


# Iterative (BFS)
def maxDepth_bfs(root):
    if not root:
        return 0
    
    depth = 0
    queue = deque([root])
    
    while queue:
        depth += 1
        for _ in range(len(queue)):
            node = queue.popleft()
            if node.left:
                queue.append(node.left)
            if node.right:
                queue.append(node.right)
    
    return depth
```

### Check if Balanced

```python
def isBalanced(root):
    def height(node):
        if not node:
            return 0
        
        left_height = height(node.left)
        if left_height == -1:
            return -1
        
        right_height = height(node.right)
        if right_height == -1:
            return -1
        
        if abs(left_height - right_height) > 1:
            return -1
        
        return 1 + max(left_height, right_height)
    
    return height(root) != -1
```

### Check if Symmetric

```python
def isSymmetric(root):
    def isMirror(left, right):
        if not left and not right:
            return True
        if not left or not right:
            return False
        
        return (left.val == right.val and 
                isMirror(left.left, right.right) and 
                isMirror(left.right, right.left))
    
    return isMirror(root, root)
```

### Diameter of Binary Tree

```python
def diameterOfBinaryTree(root):
    diameter = 0
    
    def height(node):
        nonlocal diameter
        if not node:
            return 0
        
        left = height(node.left)
        right = height(node.right)
        
        # Update diameter
        diameter = max(diameter, left + right)
        
        return 1 + max(left, right)
    
    height(root)
    return diameter
```

---

## 3. Lowest Common Ancestor (LCA)

### LCA in Binary Tree

```python
def lowestCommonAncestor(root, p, q):
    if not root or root == p or root == q:
        return root
    
    left = lowestCommonAncestor(root.left, p, q)
    right = lowestCommonAncestor(root.right, p, q)
    
    if left and right:
        return root
    
    return left if left else right
```

### LCA in BST

```python
def lowestCommonAncestor_BST(root, p, q):
    while root:
        if p.val < root.val and q.val < root.val:
            root = root.left
        elif p.val > root.val and q.val > root.val:
            root = root.right
        else:
            return root
    return None
```

### LCA with Parent Pointers

```python
def lowestCommonAncestor_parent(p, q):
    # Using Two Pointers (like finding cycle in linked list)
    a, b = p, q
    
    while a != b:
        a = a.parent if a else q
        b = b.parent if b else p
    
    return a
```

---

## 4. Binary Search Tree (BST) Operations

### Search in BST

```python
def searchBST(root, val):
    while root:
        if val == root.val:
            return root
        elif val < root.val:
            root = root.left
        else:
            root = root.right
    return None
```

### Insert in BST

```python
def insertIntoBST(root, val):
    if not root:
        return TreeNode(val)
    
    if val < root.val:
        root.left = insertIntoBST(root.left, val)
    else:
        root.right = insertIntoBST(root.right, val)
    
    return root
```

### Delete from BST

```python
def deleteNode(root, key):
    if not root:
        return None
    
    if key < root.val:
        root.left = deleteNode(root.left, key)
    elif key > root.val:
        root.right = deleteNode(root.right, key)
    else:
        # Node to delete found
        if not root.left:
            return root.right
        if not root.right:
            return root.left
        
        # Node has two children: find inorder successor
        successor = root.right
        while successor.left:
            successor = successor.left
        
        root.val = successor.val
        root.right = deleteNode(root.right, successor.val)
    
    return root
```

### Validate BST

```python
def isValidBST(root):
    def validate(node, min_val, max_val):
        if not node:
            return True
        
        if node.val <= min_val or node.val >= max_val:
            return False
        
        return (validate(node.left, min_val, node.val) and 
                validate(node.right, node.val, max_val))
    
    return validate(root, float('-inf'), float('inf'))


# Inorder approach (should be strictly increasing)
def isValidBST_inorder(root):
    prev = float('-inf')
    
    def inorder(node):
        nonlocal prev
        if not node:
            return True
        
        if not inorder(node.left):
            return False
        
        if node.val <= prev:
            return False
        prev = node.val
        
        return inorder(node.right)
    
    return inorder(root)
```

### Kth Smallest in BST

```python
def kthSmallest(root, k):
    stack = []
    curr = root
    count = 0
    
    while curr or stack:
        while curr:
            stack.append(curr)
            curr = curr.left
        
        curr = stack.pop()
        count += 1
        
        if count == k:
            return curr.val
        
        curr = curr.right
    
    return -1
```

---

## 5. Path Problems

### Path Sum (Root to Leaf)

```python
def hasPathSum(root, targetSum):
    if not root:
        return False
    
    if not root.left and not root.right:
        return root.val == targetSum
    
    return (hasPathSum(root.left, targetSum - root.val) or 
            hasPathSum(root.right, targetSum - root.val))
```

### All Root to Leaf Paths

```python
def binaryTreePaths(root):
    paths = []
    
    def dfs(node, path):
        if not node:
            return
        
        path.append(str(node.val))
        
        if not node.left and not node.right:
            paths.append("->".join(path))
        else:
            dfs(node.left, path)
            dfs(node.right, path)
        
        path.pop()  # Backtrack
    
    dfs(root, [])
    return paths
```

### Max Path Sum (Any to Any)

```python
def maxPathSum(root):
    max_sum = float('-inf')
    
    def dfs(node):
        nonlocal max_sum
        if not node:
            return 0
        
        # Max sum from left and right subtrees (take only positive)
        left = max(0, dfs(node.left))
        right = max(0, dfs(node.right))
        
        # Update max_sum considering path through current node
        max_sum = max(max_sum, left + right + node.val)
        
        # Return max path sum starting from current node going down
        return max(left, right) + node.val
    
    dfs(root)
    return max_sum
```

---

## 6. Tree Construction

### Build Tree from Preorder and Inorder

```python
def buildTree(preorder, inorder):
    if not preorder or not inorder:
        return None
    
    # Create index map for inorder
    inorder_map = {val: idx for idx, val in enumerate(inorder)}
    preorder_idx = [0]  # Use list to maintain state
    
    def build(left, right):
        if left > right:
            return None
        
        # Root is next element in preorder
        root_val = preorder[preorder_idx[0]]
        preorder_idx[0] += 1
        root = TreeNode(root_val)
        
        # Find root position in inorder
        mid = inorder_map[root_val]
        
        # Build subtrees
        root.left = build(left, mid - 1)
        root.right = build(mid + 1, right)
        
        return root
    
    return build(0, len(inorder) - 1)
```

### Build Tree from Inorder and Postorder

```python
def buildTree(inorder, postorder):
    if not inorder or not postorder:
        return None
    
    inorder_map = {val: idx for idx, val in enumerate(inorder)}
    postorder_idx = [len(postorder) - 1]
    
    def build(left, right):
        if left > right:
            return None
        
        root_val = postorder[postorder_idx[0]]
        postorder_idx[0] -= 1
        root = TreeNode(root_val)
        
        mid = inorder_map[root_val]
        
        # Build right subtree first (postorder is Left-Right-Root)
        root.right = build(mid + 1, right)
        root.left = build(left, mid - 1)
        
        return root
    
    return build(0, len(inorder) - 1)
```

### Serialize and Deserialize Binary Tree

```python
class Codec:
    def serialize(self, root):
        """Encodes a tree to a single string."""
        result = []
        
        def preorder(node):
            if not node:
                result.append("null")
                return
            result.append(str(node.val))
            preorder(node.left)
            preorder(node.right)
        
        preorder(root)
        return ",".join(result)
    
    def deserialize(self, data):
        """Decodes your encoded data to tree."""
        nodes = iter(data.split(","))
        
        def build():
            val = next(nodes)
            if val == "null":
                return None
            node = TreeNode(int(val))
            node.left = build()
            node.right = build()
            return node
        
        return build()
```

---

## 7. Morris Traversal (O(1) Space)

### Morris Inorder

```python
def morrisInorder(root):
    result = []
    curr = root
    
    while curr:
        if not curr.left:
            result.append(curr.val)
            curr = curr.right
        else:
            # Find inorder predecessor
            pred = curr.left
            while pred.right and pred.right != curr:
                pred = pred.right
            
            if not pred.right:
                # Make curr the right child of its predecessor
                pred.right = curr
                curr = curr.left
            else:
                # Restore the tree
                pred.right = None
                result.append(curr.val)
                curr = curr.right
    
    return result
```

---

## 8. N-ary Tree

```python
class NaryNode:
    def __init__(self, val=None, children=None):
        self.val = val
        self.children = children if children else []


# Level Order
def levelOrder_nary(root):
    if not root:
        return []
    
    result = []
    queue = deque([root])
    
    while queue:
        level = []
        for _ in range(len(queue)):
            node = queue.popleft()
            level.append(node.val)
            queue.extend(node.children)
        result.append(level)
    
    return result


# Max Depth
def maxDepth_nary(root):
    if not root:
        return 0
    if not root.children:
        return 1
    return 1 + max(maxDepth_nary(child) for child in root.children)
```

---

## 9. Trie (Prefix Tree)

```python
class TrieNode:
    def __init__(self):
        self.children = {}
        self.is_end = False


class Trie:
    def __init__(self):
        self.root = TrieNode()
    
    def insert(self, word):
        node = self.root
        for char in word:
            if char not in node.children:
                node.children[char] = TrieNode()
            node = node.children[char]
        node.is_end = True
    
    def search(self, word):
        node = self._find_node(word)
        return node is not None and node.is_end
    
    def startsWith(self, prefix):
        return self._find_node(prefix) is not None
    
    def _find_node(self, prefix):
        node = self.root
        for char in prefix:
            if char not in node.children:
                return None
            node = node.children[char]
        return node


# Word Search with Wildcards (. matches any char)
class WordDictionary:
    def __init__(self):
        self.root = TrieNode()
    
    def addWord(self, word):
        node = self.root
        for char in word:
            if char not in node.children:
                node.children[char] = TrieNode()
            node = node.children[char]
        node.is_end = True
    
    def search(self, word):
        def dfs(node, i):
            if i == len(word):
                return node.is_end
            
            char = word[i]
            if char == '.':
                for child in node.children.values():
                    if dfs(child, i + 1):
                        return True
                return False
            else:
                if char not in node.children:
                    return False
                return dfs(node.children[char], i + 1)
        
        return dfs(self.root, 0)
```

---

## Quick Reference

| Operation | Time Complexity |
|-----------|-----------------|
| Traversal (any) | O(n) |
| Search in BST | O(h), O(log n) balanced |
| Insert in BST | O(h) |
| Delete in BST | O(h) |
| LCA | O(n) binary tree, O(h) BST |
| Build from traversals | O(n) |
| Trie insert/search | O(m), m = word length |

| Tree Property | How to Check |
|---------------|--------------|
| Is BST? | Inorder is sorted / bounds check |
| Is Balanced? | Height diff ≤ 1 at every node |
| Is Symmetric? | Mirror comparison |
| Is Complete? | BFS - no non-null after null |
| Is Full? | Every node has 0 or 2 children |

