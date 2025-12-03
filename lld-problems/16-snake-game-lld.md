# Snake Game (Nokia Classic) - LLD

## Problem Statement
Design the classic Snake game where the snake moves on a 2D board, grows when eating food, and the game ends when it hits itself or the wall.

---

## Rules
1. Every time `moveSnake()` is called, the snake moves up, down, left, or right
2. The snake's initial size is 3 and grows by 1 when eating food
3. Food is dropped at a random position on the board
4. The game ends when the snake hits itself or the boundary

---

## Code Flow Walkthrough

### `move(direction)` - Snake Movement

```
CALL: snake.move(Direction.RIGHT)

STEP 1: Validate Direction (Prevent 180°)
├── currentDirection = RIGHT
├── newDirection = RIGHT
├── IF newDirection == currentDirection.opposite():
│   ├── Example: Going RIGHT, can't go LEFT
│   └── Ignore input, continue current direction
└── Valid directions when going RIGHT: UP, DOWN, RIGHT

STEP 2: Calculate New Head Position
├── currentHead = body.first()  // e.g., Position(5, 5)
├── newHead = currentHead.move(direction)
│   ├── RIGHT: Position(6, 5)  // x+1
│   ├── LEFT:  Position(4, 5)  // x-1
│   ├── UP:    Position(5, 4)  // y-1
│   └── DOWN:  Position(5, 6)  // y+1
└── newHead = Position(6, 5)

STEP 3: Check Collisions
├── // Wall collision
├── IF newHead.x < 0 OR newHead.x >= width:
│   └── Return MoveResult(GAME_OVER, "Hit wall")
├── IF newHead.y < 0 OR newHead.y >= height:
│   └── Return MoveResult(GAME_OVER, "Hit wall")
├── 
├── // Self collision (excluding tail if not growing)
├── IF bodySet.contains(newHead):
│   ├── // Special case: tail will move away
│   ├── IF newHead == body.last() AND NOT growing:
│   │   └── OK (tail moves, spot becomes free)
│   └── ELSE:
│       └── Return MoveResult(GAME_OVER, "Hit self")

STEP 4: Check Food Collision
├── IF newHead == food.position:
│   ├── growing = true  // Don't remove tail this move
│   ├── score += food.value
│   ├── spawnNewFood()
│   └── // Snake grows by 1

STEP 5: Update Body
├── // Add new head
├── body.addFirst(newHead)
├── bodySet.add(newHead)
├── 
├── // Remove tail (unless growing)
├── IF NOT growing:
│   ├── oldTail = body.removeLast()
│   └── bodySet.remove(oldTail)
├── ELSE:
│   └── growing = false  // Growth applied

STEP 6: Return Result
└── Return MoveResult(
        status = ALIVE,
        newHead = Position(6, 5),
        ate = true/false,
        score = 50
    )

VISUAL:
Before: [H][@][@][@][T]      (H=head, T=tail, @=body)
        Position: [(5,5), (4,5), (3,5), (2,5), (1,5)]

Move RIGHT (no food):
After:  [H][@][@][@][T]
        Position: [(6,5), (5,5), (4,5), (3,5), (2,5)]
        Old tail (1,5) removed, new head (6,5) added

Move RIGHT (ate food):
After:  [H][@][@][@][@][T]    (snake grew!)
        Position: [(6,5), (5,5), (4,5), (3,5), (2,5), (1,5)]
        No tail removed, new head added
```

### Self-Collision Detection

```
BODY DATA STRUCTURES:
├── body: Deque<Position> = [(5,5), (4,5), (3,5), (2,5)]  // head→tail
├── bodySet: Set<Position> = {(5,5), (4,5), (3,5), (2,5)} // O(1) lookup

CHECK COLLISION:
├── newHead = Position(3, 5)  // Moving left into own body
├── bodySet.contains((3,5))? YES
├── Is (3,5) the tail? body.last() = (2,5), NO
└── COLLISION! Game Over

EDGE CASE - Head meets where tail WAS:
├── Snake moving RIGHT
├── body = [(5,5), (4,5), (3,5), (2,5)]
├── newHead = (6,5)
├── After move, tail (2,5) will be removed
├── 
├── Now snake moves DOWN:
├── newHead = (6,6)
├── body after = [(6,5), (5,5), (4,5), (3,5)]
├── (2,5) is no longer in body
├── 
├── If snake later reaches (2,5):
└── No collision (position is free now)
```

### Food Spawning Algorithm

```
CALL: game.spawnFood()

STEP 1: Get All Available Positions
├── allPositions = grid (10×10 = 100 positions)
├── occupiedPositions = snake.bodySet (e.g., 5 positions)
└── availablePositions = allPositions - occupiedPositions

STEP 2: Random Selection
├── IF availablePositions.isEmpty():
│   └── Return null (snake fills board = WIN!)
├── foodPosition = availablePositions.random()
└── Example: Position(7, 3)

STEP 3: Create Food
├── food = Food(
│   │   position = Position(7, 3),
│   │   value = 10  // Points for eating
│   )
└── Place on board

OPTIMIZATION (for large boards):
├── Instead of calculating all free positions:
├── DO:
│   ├── randomPos = Position(random(width), random(height))
├── WHILE bodySet.contains(randomPos)
└── Efficient when snake is small relative to board
```

### Game Loop Flow

```
MAIN GAME LOOP:

WHILE game.status == RUNNING:
    ├── 
    ├── STEP 1: Get Input
    │   ├── direction = inputQueue.poll() ?: snake.currentDirection
    │   └── Use last direction if no new input
    ├── 
    ├── STEP 2: Move Snake
    │   ├── result = snake.move(direction)
    │   ├── IF result.status == GAME_OVER:
    │   │   └── game.status = GAME_OVER
    │   └── CONTINUE
    ├── 
    ├── STEP 3: Check Win Condition
    │   ├── IF snake.length == boardSize:
    │   │   └── game.status = WON  // Filled entire board
    ├── 
    ├── STEP 4: Update Score
    │   ├── IF result.ate:
    │   │   └── score += food.value
    ├── 
    ├── STEP 5: Render
    │   ├── renderer.clear()
    │   ├── renderer.drawSnake(snake)
    │   ├── renderer.drawFood(food)
    │   ├── renderer.drawScore(score)
    │   └── renderer.present()
    ├── 
    └── STEP 6: Wait for Next Frame
        └── Thread.sleep(gameSpeed)  // e.g., 100ms

GAME SPEED PROGRESSION:
├── Initial: 200ms per move (slow)
├── Every 5 food eaten: speed -= 20ms
├── Minimum: 50ms per move (fast)
└── Increases difficulty as snake grows
```

### Direction Queue (Handling Fast Input)

```
PROBLEM: User presses RIGHT then DOWN very fast

WITHOUT QUEUE:
├── Frame 1: Process RIGHT (ok)
├── Frame 2: Process DOWN (ok)
├── But if both inputs arrive in same frame:
│   ├── Last input wins: DOWN
│   └── RIGHT input lost!

WITH INPUT QUEUE:
├── inputQueue = [RIGHT, DOWN]
├── Frame 1: poll() → RIGHT, process RIGHT
├── Frame 2: poll() → DOWN, process DOWN
└── All inputs processed in order

PREVENTING 180° VIA QUEUE:
├── inputQueue = [UP, DOWN]  // Invalid: UP then immediate DOWN
├── Frame 1: direction=RIGHT (current)
├── poll() → UP: valid (not opposite of RIGHT)
├── Process UP, now direction=UP
├── Frame 2: 
├── poll() → DOWN: INVALID (opposite of UP)
├── Ignore or skip
└── Snake continues UP
```

---

## Requirements

### Functional Requirements
1. Initialize game with board size
2. Move snake in four directions
3. Detect collision with walls and self
4. Food spawns at random positions (not on snake)
5. Snake grows when eating food
6. Track score

### Non-Functional Requirements
1. Efficient collision detection
2. Clean separation of concerns
3. Extensible for new features

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│            Direction (enum)              │
├─────────────────────────────────────────┤
│ UP, DOWN, LEFT, RIGHT                   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│            Position                      │
├─────────────────────────────────────────┤
│ - x: Int                                │
│ - y: Int                                │
├─────────────────────────────────────────┤
│ + move(direction): Position             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              Snake                       │
├─────────────────────────────────────────┤
│ - body: Deque<Position>                 │
│ - bodySet: Set<Position>                │
├─────────────────────────────────────────┤
│ + move(direction, grow): Boolean        │
│ + getHead(): Position                   │
│ + containsPosition(pos): Boolean        │
│ + getLength(): Int                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│            SnakeGame                     │
├─────────────────────────────────────────┤
│ - board: Board                          │
│ - snake: Snake                          │
│ - food: Position?                       │
│ - score: Int                            │
│ - gameOver: Boolean                     │
├─────────────────────────────────────────┤
│ + moveSnake(direction): MoveResult      │
│ + isGameOver(): Boolean                 │
│ + getScore(): Int                       │
│ + spawnFood()                           │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              Board                       │
├─────────────────────────────────────────┤
│ - width: Int                            │
│ - height: Int                           │
├─────────────────────────────────────────┤
│ + isValidPosition(pos): Boolean         │
│ + getRandomEmptyPosition(): Position    │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Data Classes

```kotlin
import java.util.*
import kotlin.random.Random

// ==================== Direction ====================

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);
    
    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

// ==================== Position ====================

data class Position(val x: Int, val y: Int) {
    fun move(direction: Direction): Position {
        return Position(x + direction.dx, y + direction.dy)
    }
    
    fun distanceTo(other: Position): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }
}

// ==================== Move Result ====================

enum class MoveResult {
    OK,           // Normal move
    FOOD_EATEN,   // Ate food, grew
    GAME_OVER,    // Hit wall or self
    INVALID_MOVE  // Invalid direction (opposite)
}
```

### Snake Implementation

```kotlin
// ==================== Snake ====================

/**
 * The snake entity with movement and collision logic.
 * 
 * === Data Structure ===
 * Uses Deque + Set for O(1) operations:
 * - Deque (body): Ordered positions, head at front, tail at back
 * - Set (bodySet): Fast O(1) collision detection
 * 
 * === Visual Representation ===
 * 
 *   HEAD                           TAIL
 *    @----O----O----O----O----O----O
 *    ↑
 *    Direction of movement
 * 
 * body = [head, ..., tail]
 * bodySet = {all positions} for O(1) collision check
 * 
 * === Movement Algorithm ===
 * 1. Calculate new head position
 * 2. Check for self-collision (excluding tail if not growing)
 * 3. Add new head to front of deque
 * 4. If not growing: remove tail from back
 * 5. Update bodySet accordingly
 * 
 * === Why Deque + Set? ===
 * - Deque: O(1) addFirst, removeLast (perfect for snake movement)
 * - Set: O(1) contains check (fast collision detection)
 * - Together: O(1) movement with O(1) collision check
 * 
 * === 180° Turn Prevention ===
 * Snake can't reverse direction (would cause instant self-collision).
 * Example: If going RIGHT, can't go LEFT (but UP/DOWN are ok).
 * 
 * @param initialPosition Starting position for snake head
 * @param initialLength Initial snake size (default 3)
 * @param initialDirection Starting movement direction
 */
class Snake(
    initialPosition: Position,
    initialLength: Int = 3,
    initialDirection: Direction = Direction.RIGHT
) {
    // Deque: head at front, tail at back - O(1) add/remove at both ends
    private val body: Deque<Position> = ArrayDeque()
    // Set: O(1) collision detection
    private val bodySet: MutableSet<Position> = mutableSetOf()
    private var currentDirection: Direction = initialDirection
    
    init {
        // Build initial snake body from head backwards
        var pos = initialPosition
        for (i in 0 until initialLength) {
            body.addLast(pos)
            bodySet.add(pos)
            // Move backwards from head to build tail
            pos = pos.move(initialDirection.opposite())
        }
    }
    
    fun getHead(): Position = body.first
    
    fun getTail(): Position = body.last
    
    fun getBody(): List<Position> = body.toList()
    
    fun getLength(): Int = body.size
    
    fun containsPosition(position: Position): Boolean = position in bodySet
    
    fun getCurrentDirection(): Direction = currentDirection
    
    /**
     * Move the snake in the given direction.
     * Returns false if the move would cause self-collision.
     */
    fun move(direction: Direction, grow: Boolean = false): Boolean {
        // Prevent 180-degree turn
        if (direction == currentDirection.opposite() && body.size > 1) {
            return false
        }
        
        currentDirection = direction
        val newHead = getHead().move(direction)
        
        // Check self-collision (excluding tail if not growing)
        // When not growing, the tail will move, so we temporarily remove it
        if (!grow) {
            val tail = body.last
            bodySet.remove(tail)
            
            if (newHead in bodySet) {
                bodySet.add(tail) // Restore for consistency
                return false
            }
            
            body.removeLast()
        } else {
            // When growing, check against full body
            if (newHead in bodySet) {
                return false
            }
        }
        
        // Add new head
        body.addFirst(newHead)
        bodySet.add(newHead)
        
        return true
    }
    
    /**
     * Check if the next position would cause collision.
     */
    fun wouldCollide(direction: Direction, excludeTail: Boolean = true): Boolean {
        val newHead = getHead().move(direction)
        
        if (excludeTail && body.size > 1) {
            // Exclude tail from collision check
            return newHead in bodySet && newHead != body.last
        }
        
        return newHead in bodySet
    }
}
```

### Board Implementation

```kotlin
// ==================== Board ====================

class Board(
    val width: Int,
    val height: Int
) {
    fun isValidPosition(position: Position): Boolean {
        return position.x in 0 until width && position.y in 0 until height
    }
    
    fun getRandomPosition(): Position {
        return Position(Random.nextInt(width), Random.nextInt(height))
    }
    
    fun getRandomEmptyPosition(occupiedPositions: Set<Position>): Position? {
        val allPositions = mutableListOf<Position>()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pos = Position(x, y)
                if (pos !in occupiedPositions) {
                    allPositions.add(pos)
                }
            }
        }
        
        return if (allPositions.isNotEmpty()) {
            allPositions[Random.nextInt(allPositions.size)]
        } else {
            null // Board is full
        }
    }
    
    fun getAllPositions(): List<Position> {
        val positions = mutableListOf<Position>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                positions.add(Position(x, y))
            }
        }
        return positions
    }
}
```

### Snake Game Implementation

```kotlin
// ==================== Snake Game ====================

class SnakeGame(
    width: Int = 20,
    height: Int = 20,
    initialSnakeLength: Int = 3
) {
    private val board = Board(width, height)
    private val snake: Snake
    private var food: Position? = null
    private var score = 0
    private var gameOver = false
    private var moveCount = 0
    
    private val gameListeners = mutableListOf<GameListener>()
    
    init {
        // Place snake in the middle of the board
        val startX = width / 2
        val startY = height / 2
        snake = Snake(Position(startX, startY), initialSnakeLength)
        
        // Spawn initial food
        spawnFood()
    }
    
    // ==================== Game Actions ====================
    
    fun moveSnake(direction: Direction): MoveResult {
        if (gameOver) return MoveResult.GAME_OVER
        
        // Check if trying to move opposite direction
        if (direction == snake.getCurrentDirection().opposite() && snake.getLength() > 1) {
            return MoveResult.INVALID_MOVE
        }
        
        // Calculate new head position
        val newHead = snake.getHead().move(direction)
        
        // Check wall collision
        if (!board.isValidPosition(newHead)) {
            gameOver = true
            notifyGameOver()
            return MoveResult.GAME_OVER
        }
        
        // Check if eating food
        val willEat = newHead == food
        
        // Move the snake
        val moveSuccess = snake.move(direction, grow = willEat)
        
        if (!moveSuccess) {
            // Self collision
            gameOver = true
            notifyGameOver()
            return MoveResult.GAME_OVER
        }
        
        moveCount++
        
        if (willEat) {
            score += 10
            spawnFood()
            notifyFoodEaten()
            return MoveResult.FOOD_EATEN
        }
        
        return MoveResult.OK
    }
    
    private fun spawnFood() {
        val occupiedPositions = snake.getBody().toSet()
        food = board.getRandomEmptyPosition(occupiedPositions)
        
        food?.let { notifyFoodSpawned(it) }
    }
    
    // ==================== Getters ====================
    
    fun isGameOver(): Boolean = gameOver
    
    fun getScore(): Int = score
    
    fun getSnakeBody(): List<Position> = snake.getBody()
    
    fun getSnakeHead(): Position = snake.getHead()
    
    fun getSnakeLength(): Int = snake.getLength()
    
    fun getFood(): Position? = food
    
    fun getMoveCount(): Int = moveCount
    
    fun getBoardWidth(): Int = board.width
    
    fun getBoardHeight(): Int = board.height
    
    // ==================== Display ====================
    
    fun render(): String {
        val sb = StringBuilder()
        
        // Top border
        sb.appendLine("+" + "-".repeat(board.width) + "+")
        
        for (y in 0 until board.height) {
            sb.append("|")
            for (x in 0 until board.width) {
                val pos = Position(x, y)
                val char = when {
                    pos == snake.getHead() -> '@'
                    snake.containsPosition(pos) -> 'O'
                    pos == food -> '*'
                    else -> ' '
                }
                sb.append(char)
            }
            sb.appendLine("|")
        }
        
        // Bottom border
        sb.appendLine("+" + "-".repeat(board.width) + "+")
        sb.appendLine("Score: $score | Length: ${snake.getLength()} | Moves: $moveCount")
        
        if (gameOver) {
            sb.appendLine("GAME OVER!")
        }
        
        return sb.toString()
    }
    
    // ==================== Event Listeners ====================
    
    interface GameListener {
        fun onFoodEaten(game: SnakeGame)
        fun onFoodSpawned(game: SnakeGame, position: Position)
        fun onGameOver(game: SnakeGame)
    }
    
    fun addListener(listener: GameListener) {
        gameListeners.add(listener)
    }
    
    private fun notifyFoodEaten() {
        gameListeners.forEach { it.onFoodEaten(this) }
    }
    
    private fun notifyFoodSpawned(position: Position) {
        gameListeners.forEach { it.onFoodSpawned(this, position) }
    }
    
    private fun notifyGameOver() {
        gameListeners.forEach { it.onGameOver(this) }
    }
}
```

### Game Controller (For Interactive Play)

```kotlin
// ==================== Game Controller ====================

class GameController(
    private val game: SnakeGame
) {
    private var autoMoveDirection: Direction = Direction.RIGHT
    private var running = false
    
    /**
     * Process keyboard input.
     */
    fun handleInput(input: Char): MoveResult {
        val direction = when (input.lowercaseChar()) {
            'w', 'k' -> Direction.UP
            's', 'j' -> Direction.DOWN
            'a', 'h' -> Direction.LEFT
            'd', 'l' -> Direction.RIGHT
            else -> return MoveResult.OK
        }
        
        autoMoveDirection = direction
        return game.moveSnake(direction)
    }
    
    /**
     * Perform one game tick (auto-move in current direction).
     */
    fun tick(): MoveResult {
        return game.moveSnake(autoMoveDirection)
    }
    
    /**
     * Start auto-play loop (for demo).
     */
    fun startAutoPlay(delayMs: Long = 200) {
        running = true
        
        Thread {
            while (running && !game.isGameOver()) {
                val result = tick()
                println(game.render())
                
                if (result == MoveResult.GAME_OVER) break
                
                Thread.sleep(delayMs)
            }
        }.start()
    }
    
    fun stop() {
        running = false
    }
}
```

### Usage Example

```kotlin
fun main() {
    // Create game
    val game = SnakeGame(width = 15, height = 10, initialSnakeLength = 3)
    
    // Add listener
    game.addListener(object : SnakeGame.GameListener {
        override fun onFoodEaten(game: SnakeGame) {
            println("Yum! Score: ${game.getScore()}")
        }
        
        override fun onFoodSpawned(game: SnakeGame, position: Position) {
            println("Food spawned at $position")
        }
        
        override fun onGameOver(game: SnakeGame) {
            println("Game Over! Final score: ${game.getScore()}")
        }
    })
    
    println("=== Initial State ===")
    println(game.render())
    
    // Simulate gameplay
    val moves = listOf(
        Direction.RIGHT,
        Direction.RIGHT,
        Direction.DOWN,
        Direction.DOWN,
        Direction.LEFT,
        Direction.LEFT,
        Direction.UP
    )
    
    for ((index, direction) in moves.withIndex()) {
        println("\n=== Move ${index + 1}: $direction ===")
        val result = game.moveSnake(direction)
        println("Result: $result")
        println(game.render())
        
        if (game.isGameOver()) break
    }
    
    println("\n=== Final Stats ===")
    println("Score: ${game.getScore()}")
    println("Length: ${game.getSnakeLength()}")
    println("Moves: ${game.getMoveCount()}")
}
```

### Output Example

```
=== Initial State ===
+---------------+
|               |
|               |
|               |
|               |
|       @OO     |
|               |
|          *    |
|               |
|               |
|               |
+---------------+
Score: 0 | Length: 3 | Moves: 0

=== Move 1: RIGHT ===
Result: OK
+---------------+
|               |
|               |
|               |
|               |
|        @OO    |
|               |
|          *    |
|               |
|               |
|               |
+---------------+
Score: 0 | Length: 3 | Moves: 1
...
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Observer** | `GameListener` | Notify UI of game events |
| **Command** | Direction enum | Encapsulate movement commands |
| **State** | Game state (over/running) | Manage game lifecycle |
| **MVC** | Controller/Game/Render | Separate concerns |

---

## Interview Discussion Points

### Q: How to optimize collision detection?
**A:** Use `Set` for O(1) body position lookup instead of O(n) list search.

### Q: How to handle wrap-around edges?
**A:** Modify `Board.isValidPosition()` to wrap coordinates:
```kotlin
fun wrapPosition(pos: Position): Position {
    return Position(
        (pos.x + width) % width,
        (pos.y + height) % height
    )
}
```

### Q: How to add obstacles?
**A:** Add `obstacles: Set<Position>` to Board and check in collision detection.

---

## Complexity Analysis

| Operation | Time Complexity |
|-----------|----------------|
| Move snake | O(1) with Set |
| Check collision | O(1) with Set |
| Spawn food | O(n) worst case |
| Render | O(w × h) |

**Space Complexity:** O(s) where s = snake length

