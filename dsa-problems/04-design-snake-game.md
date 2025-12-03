# Design Snake Game

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 80.0%
- **Acceptance Rate**: 39.7%
- **Topics**: Array, Hash Table, Design, Queue, Simulation
- **LeetCode Link**: https://leetcode.com/problems/design-snake-game

## Problem Description

Design a Snake game that is played on a device with screen size `height x width`. Play the game online if you are not familiar with the game.

The snake is initially positioned at the top left corner `(0, 0)` with a length of 1 unit.

You are given an array `food` where `food[i] = (ri, ci)` is the row and column position of a piece of food that the snake can eat. When a snake eats a piece of food, its length and the game's score both increase by 1.

Each piece of food appears one by one on the screen, meaning the second piece of food will not appear until the snake eats the first piece of food.

When a piece of food appears on the screen, it is guaranteed that it will not appear on a block occupied by the snake.

The snake can move in four directions: `"U"` (up), `"D"` (down), `"L"` (left), `"R"` (right).

If the snake goes out of bounds or collides with itself (i.e., goes into a cell occupied by its body), the game is over and you should return `-1`.

Implement the `SnakeGame` class:
- `SnakeGame(int width, int height, int[][] food)` Initializes the object with a screen of size `height x width` and the positions of the `food`.
- `int move(String direction)` Returns the score of the game after applying one `direction` move by the snake. If the game is over, return `-1`.

## Examples

### Example 1:
```
Input
["SnakeGame", "move", "move", "move", "move", "move", "move"]
[[3, 2, [[1, 2], [0, 1]]], ["R"], ["D"], ["R"], ["U"], ["L"], ["U"]]
Output
[null, 0, 0, 1, 1, 2, -1]

Explanation
SnakeGame snakeGame = new SnakeGame(3, 2, [[1, 2], [0, 1]]);
snakeGame.move("R"); // return 0
snakeGame.move("D"); // return 0
snakeGame.move("R"); // return 1, snake eats the first piece of food. The second piece of food appears at (0, 1).
snakeGame.move("U"); // return 1
snakeGame.move("L"); // return 2, snake eats the second food. No more food appears.
snakeGame.move("U"); // return -1, game over because snake collides with border
```

## Visual Example

```
Initial State:        After "R":           After "D":
+---+---+---+         +---+---+---+        +---+---+---+
| S |   |   |         |   | S |   |        |   |   |   |
+---+---+---+         +---+---+---+        +---+---+---+
|   |   | F |         |   |   | F |        |   | S | F |
+---+---+---+         +---+---+---+        +---+---+---+

After "R" (eat):      After "U":           After "L" (eat):
+---+---+---+         +---+---+---+        +---+---+---+
|   | F |   |  →      | F | S |   |   →    | S | S |   |
+---+---+---+         +---+---+---+        +---+---+---+
|   | S | S |         |   | S |   |        |   | S |   |
+---+---+---+         +---+---+---+        +---+---+---+

S = Snake, F = Food
```

## Constraints

- `1 <= width, height <= 10^4`
- `1 <= food.length <= 50`
- `food[i].length == 2`
- `0 <= ri < height`
- `0 <= ci < width`
- `direction.length == 1`
- `direction` is `'U'`, `'D'`, `'L'`, or `'R'`.
- At most `10^4` calls will be made to `move`.

## Approach

### Key Insights:
1. Snake body is a sequence of positions - perfect for a **deque** (queue)
2. Need O(1) lookup for collision detection - use a **set**
3. Head moves first, then tail might follow (or not if eating)
4. Critical: Check collision with body AFTER removing tail (unless eating)

### Data Structures:
1. **Deque**: Store snake body positions (head at front, tail at back)
2. **Set**: Store snake body positions for O(1) collision check
3. **Food Index**: Track which food piece is next

### Algorithm:
1. Calculate new head position
2. Check if out of bounds → Game Over
3. Remove tail from body set (it will move)
4. Check if new head collides with body → Game Over
5. Add new head to front of deque and set
6. If food is at new head position:
   - Increment score and food index
   - Add tail back (snake grows)
7. Else remove tail from deque

## Solution

```python
from collections import deque
from typing import List

class SnakeGame:
    def __init__(self, width: int, height: int, food: List[List[int]]):
        self.width = width
        self.height = height
        self.food = food
        self.food_index = 0
        self.score = 0
        
        # Snake body: deque of (row, col) positions
        # Head is at front (index 0), tail is at back
        self.snake = deque([(0, 0)])
        
        # Set for O(1) collision detection
        self.snake_set = {(0, 0)}
        
        # Direction mappings
        self.directions = {
            'U': (-1, 0),
            'D': (1, 0),
            'L': (0, -1),
            'R': (0, 1)
        }
    
    def move(self, direction: str) -> int:
        # Calculate new head position
        head_row, head_col = self.snake[0]
        dr, dc = self.directions[direction]
        new_row, new_col = head_row + dr, head_col + dc
        
        # Check if out of bounds
        if new_row < 0 or new_row >= self.height or new_col < 0 or new_col >= self.width:
            return -1
        
        new_head = (new_row, new_col)
        
        # Check if eating food
        eating_food = (self.food_index < len(self.food) and 
                      self.food[self.food_index] == [new_row, new_col])
        
        # Remove tail from set FIRST (important for collision check)
        # The tail moves out, so it shouldn't cause collision
        tail = self.snake[-1]
        self.snake_set.remove(tail)
        
        # Check collision with body (after removing tail)
        if new_head in self.snake_set:
            return -1
        
        # Add new head
        self.snake.appendleft(new_head)
        self.snake_set.add(new_head)
        
        if eating_food:
            # Snake grows - add tail back
            self.snake_set.add(tail)
            self.snake.append(tail)
            self.food_index += 1
            self.score += 1
        else:
            # Snake moves - tail already removed from set, now remove from deque
            self.snake.pop()
        
        return self.score
```

## Alternative Solution (Cleaner Logic)

```python
from collections import deque
from typing import List

class SnakeGame:
    def __init__(self, width: int, height: int, food: List[List[int]]):
        self.width = width
        self.height = height
        self.food = deque([tuple(f) for f in food])  # Convert to deque of tuples
        
        self.snake = deque([(0, 0)])  # Snake body
        self.snake_set = {(0, 0)}     # For O(1) lookup
        self.score = 0
    
    def move(self, direction: str) -> int:
        moves = {'U': (-1, 0), 'D': (1, 0), 'L': (0, -1), 'R': (0, 1)}
        
        # Get new head position
        row, col = self.snake[0]
        dr, dc = moves[direction]
        new_head = (row + dr, col + dc)
        
        # Check boundaries
        if not (0 <= new_head[0] < self.height and 0 <= new_head[1] < self.width):
            return -1
        
        # Check if eating food
        if self.food and self.food[0] == new_head:
            self.food.popleft()
            self.score += 1
            # Don't remove tail - snake grows
        else:
            # Remove tail
            tail = self.snake.pop()
            self.snake_set.remove(tail)
        
        # Check self-collision (AFTER potentially removing tail)
        if new_head in self.snake_set:
            return -1
        
        # Add new head
        self.snake.appendleft(new_head)
        self.snake_set.add(new_head)
        
        return self.score
```

## Complexity Analysis

### Time Complexity:
- **\_\_init\_\_**: O(1)
- **move**: O(1) - All operations (deque operations, set operations) are O(1)

### Space Complexity: O(n + f)
- **n** = maximum length of snake
- **f** = number of food items
- Snake deque and set: O(n)
- Food array: O(f)

## Key Patterns & Techniques

1. **Deque for Snake Body**: Natural representation where operations are at both ends
2. **Set for O(1) Collision Detection**: Duplicate structure for fast lookups
3. **Order of Operations**: Critical to remove tail before checking collision
4. **Direction Mapping**: Using dictionary for clean direction handling

## Edge Cases to Handle

1. Snake collides with wall (out of bounds)
2. Snake collides with itself
3. Snake eats the last food (no more food to eat)
4. Snake can move into the position its tail just vacated

## Common Mistakes to Avoid

1. **Checking collision before removing tail**: The snake can move into where its tail was
2. **Not maintaining both deque and set**: Need both for O(1) operations
3. **Wrong order of operations**: Must handle tail removal before collision check
4. **Comparing list vs tuple**: Food might be list, snake positions might be tuples

## Visual State Machine

```
          ┌─────────────────────────────────────────┐
          │              move(direction)            │
          └─────────────────────────────────────────┘
                            │
                            ▼
                  ┌─────────────────┐
                  │ Calculate new   │
                  │ head position   │
                  └─────────────────┘
                            │
                            ▼
                  ┌─────────────────┐   Out of bounds
                  │ Check bounds?   │ ──────────────────► Return -1
                  └─────────────────┘
                            │ In bounds
                            ▼
                  ┌─────────────────┐
                  │ Remove tail     │
                  │ from set        │
                  └─────────────────┘
                            │
                            ▼
                  ┌─────────────────┐   Collision
                  │ Check collision │ ──────────────────► Return -1
                  │ with body?      │
                  └─────────────────┘
                            │ No collision
                            ▼
                  ┌─────────────────┐
                  │ Add new head    │
                  └─────────────────┘
                            │
                            ▼
                  ┌─────────────────┐   Yes
                  │ Eating food?    │ ────► Add tail back, increment score
                  └─────────────────┘
                            │ No
                            ▼
                  ┌─────────────────┐
                  │ Remove tail     │
                  │ from deque      │
                  └─────────────────┘
                            │
                            ▼
                     Return score
```

## Related Problems

- [353. Design Snake Game](https://leetcode.com/problems/design-snake-game/) (This problem)
- [289. Game of Life](https://leetcode.com/problems/game-of-life/)
- [146. LRU Cache](https://leetcode.com/problems/lru-cache/)

