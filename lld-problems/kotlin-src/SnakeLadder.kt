/**
 * Snake and Ladder Game - LLD Implementation
 * 
 * Design a Snake and Ladder game with:
 * - Multiple players (2-4)
 * - Configurable board size
 * - Snakes and ladders
 * - Turn-based gameplay
 * - Win detection
 */
package lld.snakeladder

import java.util.*

// ==================== Enums ====================

enum class GameStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
}

enum class MoveType {
    NORMAL,
    SNAKE,
    LADDER,
    WIN
}

// ==================== Player ====================

data class Player(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var position: Int = 0
) {
    fun move(newPosition: Int) {
        position = newPosition
    }
    
    fun isAtStart(): Boolean = position == 0
}

// ==================== Board Cell ====================

/**
 * Sealed class representing board cell types.
 */
sealed class BoardCell {
    object Normal : BoardCell()
    data class Snake(val tail: Int) : BoardCell()
    data class Ladder(val top: Int) : BoardCell()
}

// ==================== Board ====================

/**
 * Game board with snakes and ladders.
 * 
 * === Code Flow: getNextPosition(position, diceRoll) ===
 * 1. Calculate new position = current + roll
 * 2. If beyond board: stay in place
 * 3. If exactly at board size: WIN
 * 4. Check cell type (snake/ladder/normal)
 * 5. Return final position and move type
 */
class Board(
    val size: Int = 100
) {
    private val cells = mutableMapOf<Int, BoardCell>()
    
    init {
        (1..size).forEach { cells[it] = BoardCell.Normal }
    }
    
    /**
     * Add a snake at position head going down to tail.
     */
    fun addSnake(head: Int, tail: Int) {
        require(head in 2..size) { "Snake head must be on board" }
        require(tail in 1 until head) { "Snake tail must be below head" }
        require(cells[head] == BoardCell.Normal) { "Position $head already occupied" }
        cells[head] = BoardCell.Snake(tail)
    }
    
    /**
     * Add a ladder at position start going up to end.
     */
    fun addLadder(start: Int, end: Int) {
        require(start in 1 until size) { "Ladder start must be on board" }
        require(end in (start + 1)..size) { "Ladder end must be above start" }
        require(cells[start] == BoardCell.Normal) { "Position $start already occupied" }
        cells[start] = BoardCell.Ladder(end)
    }
    
    /**
     * Calculate next position after dice roll.
     * 
     * @param currentPosition Current player position
     * @param diceRoll Dice value (1-6)
     * @return Pair of (finalPosition, moveType)
     */
    fun getNextPosition(currentPosition: Int, diceRoll: Int): Pair<Int, MoveType> {
        val newPosition = currentPosition + diceRoll
        
        // Beyond board - stay in place
        if (newPosition > size) {
            return Pair(currentPosition, MoveType.NORMAL)
        }
        
        // Exact win
        if (newPosition == size) {
            return Pair(size, MoveType.WIN)
        }
        
        // Check for snake or ladder
        return when (val cell = cells[newPosition]) {
            is BoardCell.Snake -> Pair(cell.tail, MoveType.SNAKE)
            is BoardCell.Ladder -> Pair(cell.top, MoveType.LADDER)
            else -> Pair(newPosition, MoveType.NORMAL)
        }
    }
    
    fun getCell(position: Int): BoardCell? = cells[position]
    
    fun getSnakes(): Map<Int, Int> {
        return cells.filterValues { it is BoardCell.Snake }
            .mapValues { (it.value as BoardCell.Snake).tail }
    }
    
    fun getLadders(): Map<Int, Int> {
        return cells.filterValues { it is BoardCell.Ladder }
            .mapValues { (it.value as BoardCell.Ladder).top }
    }
}

// ==================== Dice ====================

interface Dice {
    fun roll(): Int
}

/**
 * Standard 6-sided dice.
 */
class StandardDice : Dice {
    private val random = Random()
    
    override fun roll(): Int = random.nextInt(6) + 1
}

/**
 * Multiple dice.
 */
class MultiDice(private val count: Int = 2) : Dice {
    private val random = Random()
    
    override fun roll(): Int = (1..count).sumOf { random.nextInt(6) + 1 }
}

/**
 * Fixed dice for testing.
 */
class FixedDice(private val values: List<Int>) : Dice {
    private var index = 0
    
    override fun roll(): Int {
        val value = values[index % values.size]
        index++
        return value
    }
}

// ==================== Turn Result ====================

data class TurnResult(
    val player: Player,
    val diceValue: Int,
    val previousPosition: Int,
    val newPosition: Int,
    val moveType: MoveType,
    val winner: Player? = null
)

// ==================== Game ====================

/**
 * Main game controller.
 * 
 * === Code Flow: playTurn() ===
 * 1. Validate game is in progress
 * 2. Get current player
 * 3. Roll dice
 * 4. Calculate new position (with snake/ladder check)
 * 5. Update player position
 * 6. Check win condition
 * 7. Advance to next player
 * 8. Return turn result
 */
class Game(
    private val board: Board = Board(),
    private val dice: Dice = StandardDice()
) {
    private val players = mutableListOf<Player>()
    private var currentPlayerIndex = 0
    private var status = GameStatus.NOT_STARTED
    private var winner: Player? = null
    
    fun addPlayer(name: String): Player {
        require(status == GameStatus.NOT_STARTED) { "Cannot add players after game started" }
        require(players.size < 4) { "Maximum 4 players allowed" }
        
        val player = Player(name = name)
        players.add(player)
        return player
    }
    
    fun start() {
        require(players.size >= 2) { "Need at least 2 players" }
        require(status == GameStatus.NOT_STARTED) { "Game already started" }
        
        status = GameStatus.IN_PROGRESS
    }
    
    /**
     * Play a single turn for the current player.
     */
    fun playTurn(): TurnResult {
        require(status == GameStatus.IN_PROGRESS) { "Game not in progress" }
        
        val player = players[currentPlayerIndex]
        val previousPosition = player.position
        val diceValue = dice.roll()
        
        val (newPosition, moveType) = board.getNextPosition(previousPosition, diceValue)
        player.move(newPosition)
        
        // Check win
        if (moveType == MoveType.WIN) {
            status = GameStatus.FINISHED
            winner = player
        }
        
        val result = TurnResult(
            player = player,
            diceValue = diceValue,
            previousPosition = previousPosition,
            newPosition = newPosition,
            moveType = moveType,
            winner = winner
        )
        
        // Move to next player
        if (status == GameStatus.IN_PROGRESS) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        }
        
        return result
    }
    
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]
    
    fun getStatus(): GameStatus = status
    
    fun getWinner(): Player? = winner
    
    fun getPlayers(): List<Player> = players.toList()
    
    fun reset() {
        players.forEach { it.move(0) }
        currentPlayerIndex = 0
        status = GameStatus.NOT_STARTED
        winner = null
    }
}

// ==================== Game Builder ====================

class GameBuilder {
    private val board = Board()
    private var dice: Dice = StandardDice()
    
    fun addSnake(head: Int, tail: Int) = apply {
        board.addSnake(head, tail)
    }
    
    fun addLadder(start: Int, end: Int) = apply {
        board.addLadder(start, end)
    }
    
    fun withDice(dice: Dice) = apply {
        this.dice = dice
    }
    
    fun build(): Game = Game(board, dice)
}

// ==================== Usage Example ====================

fun main() {
    println("=== Snake and Ladder Game ===\n")
    
    // Build game
    val game = GameBuilder()
        .addSnake(17, 7)
        .addSnake(54, 34)
        .addSnake(62, 19)
        .addSnake(98, 79)
        .addLadder(3, 22)
        .addLadder(8, 30)
        .addLadder(28, 84)
        .addLadder(58, 77)
        .build()
    
    // Add players
    game.addPlayer("Alice")
    game.addPlayer("Bob")
    
    // Start game
    game.start()
    
    println("Game started with ${game.getPlayers().size} players")
    println("Snakes: ${(game as Game).let { g -> 
        GameBuilder().build().let { Board().getSnakes() } // Just for display
    }}")
    println()
    
    // Play until someone wins (or max 100 turns)
    var turnCount = 0
    while (game.getStatus() == GameStatus.IN_PROGRESS && turnCount < 100) {
        val result = game.playTurn()
        turnCount++
        
        val moveDesc = when (result.moveType) {
            MoveType.SNAKE -> "🐍 Bitten by snake!"
            MoveType.LADDER -> "🪜 Climbed ladder!"
            MoveType.WIN -> "🏆 WINNER!"
            else -> ""
        }
        
        println("Turn $turnCount: ${result.player.name} rolled ${result.diceValue}, " +
                "moved ${result.previousPosition} → ${result.newPosition} $moveDesc")
        
        if (result.winner != null) {
            println("\n🎉 ${result.winner.name} wins the game!")
            break
        }
    }
    
    if (game.getStatus() != GameStatus.FINISHED) {
        println("\nGame ended without a winner after $turnCount turns")
    }
    
    println("\nFinal positions:")
    game.getPlayers().forEach { player ->
        println("  ${player.name}: ${player.position}")
    }
}

