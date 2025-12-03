# Snake and Ladder Game - LLD

## Problem Statement
Design a Snake and Ladder game that supports multiple players, configurable board size, and game rules.

---

## Requirements

### Functional Requirements
1. Support 2-4 players
2. Configurable board size (default 100)
3. Add snakes and ladders at positions
4. Roll dice and move players
5. Detect winner
6. Support game restart

### Non-Functional Requirements
1. Extensible for game variations
2. Clean separation of concerns
3. Easy to test

---

## Class Diagram

```
┌─────────────────────────────────────┐
│              Game                   │
├─────────────────────────────────────┤
│ - board: Board                      │
│ - players: List<Player>             │
│ - dice: Dice                        │
│ - currentPlayerIndex: Int           │
│ - status: GameStatus                │
├─────────────────────────────────────┤
│ + start()                           │
│ + playTurn(): TurnResult            │
│ + getWinner(): Player?              │
└─────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────┐
│             Board                   │
├─────────────────────────────────────┤
│ - size: Int                         │
│ - snakes: Map<Int, Int>             │
│ - ladders: Map<Int, Int>            │
├─────────────────────────────────────┤
│ + getNextPosition(pos, roll): Int   │
│ + addSnake(head, tail)              │
│ + addLadder(start, end)             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│             Player                  │
├─────────────────────────────────────┤
│ - id: String                        │
│ - name: String                      │
│ - position: Int                     │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       <<interface>> Dice            │
├─────────────────────────────────────┤
│ + roll(): Int                       │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Classes

```kotlin
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
    
    fun reset() {
        position = 0
    }
}

// ==================== Dice ====================

interface Dice {
    fun roll(): Int
}

class StandardDice(private val sides: Int = 6) : Dice {
    private val random = Random()
    
    override fun roll(): Int = random.nextInt(sides) + 1
}

class LoadedDice(private val values: List<Int>) : Dice {
    private var index = 0
    
    override fun roll(): Int {
        val value = values[index % values.size]
        index++
        return value
    }
}

class MultipleDice(private val count: Int = 2, private val sides: Int = 6) : Dice {
    private val random = Random()
    
    override fun roll(): Int = (1..count).sumOf { random.nextInt(sides) + 1 }
}
```

### Board

```kotlin
// ==================== Board Cell ====================

sealed class BoardCell {
    object Normal : BoardCell()
    data class Snake(val tail: Int) : BoardCell()
    data class Ladder(val top: Int) : BoardCell()
}

// ==================== Board ====================

class Board(
    val size: Int = 100
) {
    private val cells = mutableMapOf<Int, BoardCell>()
    
    init {
        // Initialize all cells as normal
        (1..size).forEach { cells[it] = BoardCell.Normal }
    }
    
    fun addSnake(head: Int, tail: Int) {
        require(head in 2..size) { "Snake head must be on board" }
        require(tail in 1 until head) { "Snake tail must be below head" }
        require(cells[head] == BoardCell.Normal) { "Position $head already occupied" }
        cells[head] = BoardCell.Snake(tail)
    }
    
    fun addLadder(start: Int, end: Int) {
        require(start in 1 until size) { "Ladder start must be on board" }
        require(end in (start + 1)..size) { "Ladder end must be above start" }
        require(cells[start] == BoardCell.Normal) { "Position $start already occupied" }
        cells[start] = BoardCell.Ladder(end)
    }
    
    fun getNextPosition(currentPosition: Int, diceRoll: Int): Pair<Int, MoveType> {
        val newPosition = currentPosition + diceRoll
        
        // Beyond board - don't move
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
    
    fun getSnakes(): Map<Int, Int> = cells.entries
        .filter { it.value is BoardCell.Snake }
        .associate { it.key to (it.value as BoardCell.Snake).tail }
    
    fun getLadders(): Map<Int, Int> = cells.entries
        .filter { it.value is BoardCell.Ladder }
        .associate { it.key to (it.value as BoardCell.Ladder).top }
}
```

### Turn Result

```kotlin
// ==================== Turn Result ====================

data class TurnResult(
    val player: Player,
    val diceRoll: Int,
    val startPosition: Int,
    val endPosition: Int,
    val moveType: MoveType,
    val isWinner: Boolean = false
) {
    override fun toString(): String {
        val moveDescription = when (moveType) {
            MoveType.NORMAL -> "moved to $endPosition"
            MoveType.SNAKE -> "bitten by snake! Slid down to $endPosition"
            MoveType.LADDER -> "found a ladder! Climbed up to $endPosition"
            MoveType.WIN -> "WINS THE GAME!"
        }
        return "${player.name} rolled $diceRoll from position $startPosition, $moveDescription"
    }
}
```

### Game

```kotlin
// ==================== Game ====================

class Game private constructor(
    private val board: Board,
    private val players: MutableList<Player>,
    private val dice: Dice
) {
    private var currentPlayerIndex = 0
    private var status = GameStatus.NOT_STARTED
    private var winner: Player? = null
    private val turnHistory = mutableListOf<TurnResult>()
    
    // ==================== Game Control ====================
    
    fun start() {
        require(players.size >= 2) { "Need at least 2 players" }
        require(status == GameStatus.NOT_STARTED) { "Game already started" }
        
        status = GameStatus.IN_PROGRESS
        currentPlayerIndex = 0
        players.forEach { it.reset() }
        turnHistory.clear()
        
        println("Game started with ${players.size} players!")
        println("Board size: ${board.size}")
        println("Snakes: ${board.getSnakes()}")
        println("Ladders: ${board.getLadders()}")
        println()
    }
    
    fun playTurn(): TurnResult? {
        if (status != GameStatus.IN_PROGRESS) return null
        
        val currentPlayer = players[currentPlayerIndex]
        val diceRoll = dice.roll()
        val startPosition = currentPlayer.position
        
        val (endPosition, moveType) = board.getNextPosition(startPosition, diceRoll)
        currentPlayer.move(endPosition)
        
        val isWinner = moveType == MoveType.WIN
        if (isWinner) {
            status = GameStatus.FINISHED
            winner = currentPlayer
        }
        
        val result = TurnResult(
            player = currentPlayer,
            diceRoll = diceRoll,
            startPosition = startPosition,
            endPosition = endPosition,
            moveType = moveType,
            isWinner = isWinner
        )
        
        turnHistory.add(result)
        
        // Move to next player if game continues
        if (!isWinner) {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        }
        
        return result
    }
    
    fun playGame(): Player {
        start()
        
        while (status == GameStatus.IN_PROGRESS) {
            val result = playTurn()
            result?.let { println(it) }
        }
        
        return winner!!
    }
    
    // ==================== Getters ====================
    
    fun getStatus(): GameStatus = status
    fun getWinner(): Player? = winner
    fun getCurrentPlayer(): Player = players[currentPlayerIndex]
    fun getPlayers(): List<Player> = players.toList()
    fun getTurnHistory(): List<TurnResult> = turnHistory.toList()
    
    fun getPlayerPositions(): Map<String, Int> {
        return players.associate { it.name to it.position }
    }
    
    // ==================== Builder ====================
    
    class Builder {
        private var boardSize: Int = 100
        private val snakes = mutableListOf<Pair<Int, Int>>()
        private val ladders = mutableListOf<Pair<Int, Int>>()
        private val players = mutableListOf<Player>()
        private var dice: Dice = StandardDice()
        
        fun boardSize(size: Int) = apply { boardSize = size }
        
        fun addSnake(head: Int, tail: Int) = apply { snakes.add(head to tail) }
        
        fun addLadder(start: Int, end: Int) = apply { ladders.add(start to end) }
        
        fun addPlayer(name: String) = apply { 
            players.add(Player(name = name)) 
        }
        
        fun addPlayers(vararg names: String) = apply {
            names.forEach { addPlayer(it) }
        }
        
        fun dice(dice: Dice) = apply { this.dice = dice }
        
        fun standardBoard() = apply {
            // Classic snakes and ladders setup
            addSnake(99, 54)
            addSnake(70, 55)
            addSnake(52, 42)
            addSnake(25, 2)
            addSnake(95, 72)
            
            addLadder(6, 25)
            addLadder(11, 40)
            addLadder(60, 85)
            addLadder(46, 90)
            addLadder(17, 69)
        }
        
        fun build(): Game {
            require(players.size >= 2) { "Need at least 2 players" }
            
            val board = Board(boardSize)
            snakes.forEach { (head, tail) -> board.addSnake(head, tail) }
            ladders.forEach { (start, end) -> board.addLadder(start, end) }
            
            return Game(board, players, dice)
        }
    }
    
    companion object {
        fun builder() = Builder()
    }
}
```

### Game Variations

```kotlin
// ==================== Game Variations ====================

// Variation: Need exact roll to win
class ExactWinBoard(size: Int = 100) : Board(size) {
    override fun getNextPosition(currentPosition: Int, diceRoll: Int): Pair<Int, MoveType> {
        val newPosition = currentPosition + diceRoll
        
        // Must land exactly on last square
        if (newPosition > size) {
            return Pair(currentPosition, MoveType.NORMAL)
        }
        
        return super.getNextPosition(currentPosition, diceRoll)
    }
}

// Variation: Bounce back if over
class BounceBackBoard(size: Int = 100) : Board(size) {
    override fun getNextPosition(currentPosition: Int, diceRoll: Int): Pair<Int, MoveType> {
        var newPosition = currentPosition + diceRoll
        
        // Bounce back from end
        if (newPosition > size) {
            newPosition = size - (newPosition - size)
        }
        
        if (newPosition == size) {
            return Pair(size, MoveType.WIN)
        }
        
        return super.getNextPosition(newPosition - diceRoll, diceRoll)
    }
}
```

### Usage Example

```kotlin
fun main() {
    // Create game with builder
    val game = Game.builder()
        .boardSize(100)
        .standardBoard() // Add classic snakes and ladders
        .addPlayers("Alice", "Bob", "Charlie")
        .dice(StandardDice(6))
        .build()
    
    // Play the game
    val winner = game.playGame()
    
    println("\n🏆 Winner: ${winner.name}!")
    println("\nFinal positions:")
    game.getPlayerPositions().forEach { (name, pos) ->
        println("  $name: $pos")
    }
    println("\nTotal turns: ${game.getTurnHistory().size}")
    
    // Interactive mode example
    println("\n=== Interactive Game ===\n")
    
    val interactiveGame = Game.builder()
        .boardSize(20) // Smaller board for demo
        .addSnake(18, 5)
        .addSnake(15, 8)
        .addLadder(3, 12)
        .addLadder(7, 16)
        .addPlayers("Player1", "Player2")
        .build()
    
    interactiveGame.start()
    
    // Simulate turns
    repeat(20) {
        if (interactiveGame.getStatus() == GameStatus.IN_PROGRESS) {
            val result = interactiveGame.playTurn()
            result?.let { println(it) }
        }
    }
    
    interactiveGame.getWinner()?.let {
        println("\n🎉 ${it.name} wins!")
    } ?: println("\nGame still in progress...")
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Builder** | `Game.Builder` | Complex game setup |
| **Strategy** | `Dice` interface | Different dice behaviors |
| **State** | `GameStatus` | Game state management |
| **Factory** | Can be added | Create board configurations |
| **Template Method** | Board subclasses | Different win conditions |

---

## State Diagram

```
     ┌────────────────┐
     │  NOT_STARTED   │
     └───────┬────────┘
             │ start()
             ▼
     ┌────────────────┐
     │  IN_PROGRESS   │◄──────┐
     └───────┬────────┘       │
             │                │
             │ playTurn()     │ no winner
             ▼                │
       ┌─────────────┐       │
       │ Check Winner├───────┘
       └──────┬──────┘
              │ winner found
              ▼
     ┌────────────────┐
     │   FINISHED     │
     └────────────────┘
```

---

## Interview Discussion Points

### Q: How would you add power-ups?
**A:** Create a `PowerUp` interface with implementations like `DoubleRoll`, `SkipSnake`, etc. Add power-up cells to the board.

### Q: How to persist game state?
**A:** Create `GameState` data class with all state, serialize to JSON/DB. Add `save()` and `load()` methods to Game.

### Q: How to add multiplayer over network?
**A:** Extract `GameController` interface, implement `LocalGameController` and `NetworkGameController`. Use events for state sync.

---

## Time Complexity

| Operation | Complexity |
|-----------|------------|
| Roll Dice | O(1) |
| Move Player | O(1) |
| Check Snake/Ladder | O(1) with HashMap |
| Start Game | O(p) where p = players |

