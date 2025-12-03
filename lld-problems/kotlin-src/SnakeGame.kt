/**
 * Snake Game - LLD Implementation
 * 
 * Classic Nokia snake game with:
 * - Snake movement using Deque
 * - Collision detection using Set
 * - Food spawning
 * - Score tracking
 */
package lld.snakegame

import java.util.*

// ==================== Direction ====================

enum class Direction {
    UP, DOWN, LEFT, RIGHT;
    
    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

// ==================== Position ====================

data class Position(val x: Int, val y: Int) {
    fun move(direction: Direction): Position = when (direction) {
        Direction.UP -> Position(x, y - 1)
        Direction.DOWN -> Position(x, y + 1)
        Direction.LEFT -> Position(x - 1, y)
        Direction.RIGHT -> Position(x + 1, y)
    }
}

// ==================== Food ====================

data class Food(
    val position: Position,
    val value: Int = 10
)

// ==================== Game Status ====================

enum class GameStatus {
    RUNNING,
    GAME_OVER,
    WON
}

// ==================== Move Result ====================

data class MoveResult(
    val status: GameStatus,
    val newHead: Position?,
    val ateFood: Boolean = false,
    val score: Int = 0
)

// ==================== Snake ====================

/**
 * Snake entity with movement and collision logic.
 * 
 * === Data Structure ===
 * - body: Deque<Position> - O(1) addFirst/removeLast
 * - bodySet: Set<Position> - O(1) collision check
 * 
 * === Movement Algorithm ===
 * 1. Calculate new head position
 * 2. Check self-collision
 * 3. Add new head to front
 * 4. Remove tail (unless growing)
 */
class Snake(
    initialPosition: Position,
    initialLength: Int = 3,
    initialDirection: Direction = Direction.RIGHT
) {
    private val body: Deque<Position> = ArrayDeque()
    private val bodySet: MutableSet<Position> = mutableSetOf()
    private var currentDirection: Direction = initialDirection
    
    init {
        var pos = initialPosition
        for (i in 0 until initialLength) {
            body.addLast(pos)
            bodySet.add(pos)
            pos = pos.move(initialDirection.opposite())
        }
    }
    
    fun getHead(): Position = body.first()
    
    fun getTail(): Position = body.last()
    
    fun getBody(): List<Position> = body.toList()
    
    fun getLength(): Int = body.size
    
    fun getDirection(): Direction = currentDirection
    
    /**
     * Change direction (prevents 180° turns).
     */
    fun setDirection(direction: Direction) {
        if (direction != currentDirection.opposite()) {
            currentDirection = direction
        }
    }
    
    /**
     * Move snake and return if collision occurred.
     * 
     * @param grow If true, don't remove tail (snake grows)
     * @return New head position, or null if self-collision
     */
    fun move(grow: Boolean = false): Position? {
        val newHead = getHead().move(currentDirection)
        
        // Check self-collision (excluding tail if not growing)
        val effectiveBodySet = if (grow) bodySet else bodySet - getTail()
        if (newHead in effectiveBodySet) {
            return null // Self collision
        }
        
        // Add new head
        body.addFirst(newHead)
        bodySet.add(newHead)
        
        // Remove tail if not growing
        if (!grow) {
            val oldTail = body.removeLast()
            bodySet.remove(oldTail)
        }
        
        return newHead
    }
    
    /**
     * Check if position is part of snake body.
     */
    fun occupies(position: Position): Boolean = position in bodySet
}

// ==================== Game Board ====================

/**
 * Game board managing the game state.
 */
class GameBoard(
    val width: Int = 20,
    val height: Int = 20
) {
    var snake: Snake = Snake(Position(width / 2, height / 2))
        private set
    var food: Food? = null
        private set
    var score: Int = 0
        private set
    var status: GameStatus = GameStatus.RUNNING
        private set
    
    private val random = Random()
    
    init {
        spawnFood()
    }
    
    /**
     * Spawn food at random empty position.
     */
    fun spawnFood() {
        val emptyPositions = mutableListOf<Position>()
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pos = Position(x, y)
                if (!snake.occupies(pos)) {
                    emptyPositions.add(pos)
                }
            }
        }
        
        if (emptyPositions.isEmpty()) {
            status = GameStatus.WON
            return
        }
        
        val foodPos = emptyPositions[random.nextInt(emptyPositions.size)]
        food = Food(foodPos)
    }
    
    /**
     * Process one game tick.
     */
    fun tick(direction: Direction? = null): MoveResult {
        if (status != GameStatus.RUNNING) {
            return MoveResult(status, null, false, score)
        }
        
        // Update direction if provided
        direction?.let { snake.setDirection(it) }
        
        // Check if will eat food
        val nextHead = snake.getHead().move(snake.getDirection())
        val willEatFood = food?.position == nextHead
        
        // Move snake
        val newHead = snake.move(grow = willEatFood)
        
        // Check wall collision
        if (newHead == null || !isInBounds(newHead)) {
            status = GameStatus.GAME_OVER
            return MoveResult(GameStatus.GAME_OVER, newHead, false, score)
        }
        
        // Handle food
        if (willEatFood) {
            score += food?.value ?: 0
            spawnFood()
        }
        
        return MoveResult(status, newHead, willEatFood, score)
    }
    
    /**
     * Check if position is within bounds.
     */
    private fun isInBounds(position: Position): Boolean {
        return position.x in 0 until width && position.y in 0 until height
    }
    
    /**
     * Reset the game.
     */
    fun reset() {
        snake = Snake(Position(width / 2, height / 2))
        score = 0
        status = GameStatus.RUNNING
        spawnFood()
    }
    
    /**
     * Render board as string (for console display).
     */
    fun render(): String {
        val sb = StringBuilder()
        
        // Top border
        sb.append("+" + "-".repeat(width) + "+\n")
        
        for (y in 0 until height) {
            sb.append("|")
            for (x in 0 until width) {
                val pos = Position(x, y)
                val char = when {
                    pos == snake.getHead() -> '@'  // Head
                    snake.occupies(pos) -> 'O'     // Body
                    food?.position == pos -> '*'   // Food
                    else -> ' '                    // Empty
                }
                sb.append(char)
            }
            sb.append("|\n")
        }
        
        // Bottom border
        sb.append("+" + "-".repeat(width) + "+")
        
        return sb.toString()
    }
}

// ==================== Game Controller ====================

/**
 * Game controller with input handling.
 */
class SnakeGameController(
    private val board: GameBoard = GameBoard()
) {
    private val inputQueue = ArrayDeque<Direction>()
    
    fun queueInput(direction: Direction) {
        if (inputQueue.size < 3) {  // Limit input buffer
            inputQueue.add(direction)
        }
    }
    
    fun tick(): MoveResult {
        val direction = inputQueue.pollFirst()
        return board.tick(direction)
    }
    
    fun getBoard(): GameBoard = board
    
    fun reset() {
        inputQueue.clear()
        board.reset()
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Snake Game ===\n")
    
    val board = GameBoard(width = 15, height = 10)
    
    println("Initial state:")
    println(board.render())
    println("Score: ${board.score}")
    println("Snake length: ${board.snake.getLength()}")
    
    // Simulate some moves
    val moves = listOf(
        Direction.RIGHT,
        Direction.RIGHT,
        Direction.DOWN,
        Direction.DOWN,
        Direction.LEFT,
        Direction.UP
    )
    
    println("\n--- Simulating moves ---\n")
    
    for (move in moves) {
        println("Moving ${move}...")
        val result = board.tick(move)
        
        if (result.ateFood) {
            println("  🍎 Ate food! Score: ${result.score}")
        }
        
        if (result.status == GameStatus.GAME_OVER) {
            println("  💀 Game Over!")
            break
        }
    }
    
    println("\nFinal state:")
    println(board.render())
    println("Score: ${board.score}")
    println("Snake length: ${board.snake.getLength()}")
    
    println("\n--- Snake Data Structure Demo ---")
    
    val snake = Snake(Position(5, 5), initialLength = 4)
    println("Snake body: ${snake.getBody()}")
    println("Head: ${snake.getHead()}")
    println("Tail: ${snake.getTail()}")
    println("Length: ${snake.getLength()}")
    
    // Move without growing
    snake.move(grow = false)
    println("\nAfter move (no grow): ${snake.getBody()}")
    
    // Move with growing
    snake.move(grow = true)
    println("After move (grow): ${snake.getBody()}")
    println("New length: ${snake.getLength()}")
    
    // Test direction change
    snake.setDirection(Direction.DOWN)
    snake.move()
    println("After DOWN: ${snake.getBody()}")
    
    // Can't go opposite direction
    snake.setDirection(Direction.UP)  // Should be ignored
    println("Direction after trying UP: ${snake.getDirection()}")
}

