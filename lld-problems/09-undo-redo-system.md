# Undo-Redo System - LLD

## Problem Statement
Design an Undo-Redo system using the Command Pattern that can track and reverse operations.

---

## Code Flow Walkthrough

### `execute(command)` - Execute and Track

```
CALL: manager.execute(InsertTextCommand("Hello"))

STEP 1: Check for Command Merging
├── IF enableMerging AND undoStack.isNotEmpty():
│   ├── lastCommand = undoStack.peek()
│   ├── IF lastCommand.canMergeWith(command):
│   │   ├── Example: Typing "H" then "e" → merge to "He"
│   │   ├── undoStack.pop()  // Remove last
│   │   ├── merged = lastCommand.mergeWith(command)
│   │   ├── merged.execute()
│   │   ├── undoStack.push(merged)
│   │   ├── redoStack.clear()
│   │   └── Return (merged command executed)
│   └── ELSE: proceed with separate command

STEP 2: Execute the Command
├── command.execute()
│   ├── InsertTextCommand.execute():
│   │   ├── previousState = document.getText()
│   │   ├── document.insert(position, "Hello")
│   │   └── Store previousState for undo

STEP 3: Track in Undo Stack
├── undoStack.push(command)
└── Command can now be undone

STEP 4: Clear Redo Stack
├── redoStack.clear()
└── New action invalidates previous redo history

STEP 5: Enforce History Limit
├── WHILE undoStack.size > historyLimit:
│   └── undoStack.removeLast()  // Remove oldest
└── Prevents unbounded memory growth

STACK STATE:
├── Before: undoStack=[A,B,C], redoStack=[X]
├── execute(D)
├── After:  undoStack=[D,A,B,C], redoStack=[]
└── Note: X is gone (can't redo after new action)
```

### `undo()` - Undo Last Action

```
CALL: manager.undo()

STEP 1: Check if Undo Available
├── IF undoStack.isEmpty():
│   └── Return false (nothing to undo)

STEP 2: Pop from Undo Stack
├── command = undoStack.pop()
└── Remove from undo history

STEP 3: Undo the Command
├── command.undo()
│   ├── InsertTextCommand.undo():
│   │   └── document.setText(previousState)
│   ├── DeleteTextCommand.undo():
│   │   └── document.insert(position, deletedText)
│   └── Restore to before this command

STEP 4: Push to Redo Stack
├── redoStack.push(command)
└── Can now redo this undone action

STEP 5: Notify Listeners
└── notifyStateChanged()

STACK STATE:
├── Before: undoStack=[D,C,B,A], redoStack=[]
├── undo()  // Undoes D
├── After:  undoStack=[C,B,A], redoStack=[D]
└── Document state: before D was executed
```

### `redo()` - Redo Undone Action

```
CALL: manager.redo()

STEP 1: Check if Redo Available
├── IF redoStack.isEmpty():
│   └── Return false (nothing to redo)

STEP 2: Pop from Redo Stack
├── command = redoStack.pop()

STEP 3: Re-execute the Command
├── command.execute()
└── Same as original execution

STEP 4: Push to Undo Stack
├── undoStack.push(command)

STEP 5: Notify Listeners
└── notifyStateChanged()

STACK STATE:
├── Before: undoStack=[C,B,A], redoStack=[D,E]
├── redo()  // Redoes D
├── After:  undoStack=[D,C,B,A], redoStack=[E]
└── Document state: after D was executed
```

### Command Merging Example (Typing)

```
SCENARIO: User types "Hello" one character at a time

TYPE 'H':
├── execute(InsertText("H", pos=0))
├── undoStack=[Insert("H")]
└── Document: "H"

TYPE 'e':
├── execute(InsertText("e", pos=1))
├── canMergeWith? Insert("H").canMerge(Insert("e"))
│   ├── Same type? Yes
│   ├── Adjacent position? 0+1=1, new pos=1, Yes
│   └── Within time threshold? Yes
├── Merge: Insert("H") + Insert("e") = Insert("He")
├── undoStack=[Insert("He")]
└── Document: "He"

TYPE 'l':
├── Merge: Insert("He") + Insert("l") = Insert("Hel")
├── undoStack=[Insert("Hel")]
└── Document: "Hel"

... continue typing ...

RESULT:
├── undoStack=[Insert("Hello")]
├── Single undo removes entire word
└── Better UX than undoing one character at a time
```

### Composite Command (Batch Operations)

```
SCENARIO: Find and Replace "cat" with "dog"

CREATE COMPOSITE:
├── composite = CompositeCommand([
│   │   DeleteText(pos=0, text="cat"),
│   │   InsertText(pos=0, text="dog"),
│   │   DeleteText(pos=20, text="cat"),
│   │   InsertText(pos=20, text="dog")
│   ])

EXECUTE:
├── composite.execute():
│   ├── FOR each subCommand in commands:
│   │   └── subCommand.execute()
│   └── All changes happen together

UNDO:
├── composite.undo():
│   ├── FOR each subCommand in commands.reversed():
│   │   └── subCommand.undo()
│   └── All changes reversed in opposite order
└── Single undo reverts entire find/replace
```

---

## Requirements

### Functional Requirements
1. Execute commands that can be undone
2. Undo last executed command
3. Redo last undone command
4. Support composite commands (group)
5. Command history with limit

### Non-Functional Requirements
1. O(1) for undo/redo operations
2. Memory efficient
3. Thread-safe
4. Extensible for new commands

---

## Class Diagram

```
┌─────────────────────────────────────┐
│     <<interface>> Command           │
├─────────────────────────────────────┤
│ + execute()                         │
│ + undo()                            │
│ + getDescription(): String          │
└─────────────────────────────────────┘
         △
         │
    ┌────┼────┬──────────────┐
    │    │    │              │
  Insert Delete  Format   Composite
  Cmd    Cmd     Cmd      Command

┌─────────────────────────────────────┐
│         CommandManager              │
├─────────────────────────────────────┤
│ - undoStack: Stack<Command>         │
│ - redoStack: Stack<Command>         │
│ - historyLimit: Int                 │
├─────────────────────────────────────┤
│ + execute(command: Command)         │
│ + undo(): Boolean                   │
│ + redo(): Boolean                   │
│ + canUndo(): Boolean                │
│ + canRedo(): Boolean                │
│ + getHistory(): List<Command>       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│          TextEditor                 │
├─────────────────────────────────────┤
│ - content: StringBuilder            │
│ - commandManager: CommandManager    │
├─────────────────────────────────────┤
│ + insert(text, position)            │
│ + delete(start, end)                │
│ + undo()                            │
│ + redo()                            │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Command Interface

```kotlin
import java.util.*

// ==================== Command Interface ====================

interface Command {
    fun execute()
    fun undo()
    fun getDescription(): String
    fun canMergeWith(other: Command): Boolean = false
    fun mergeWith(other: Command): Command = this
}

// ==================== Reversible Command ====================

abstract class ReversibleCommand : Command {
    protected var isExecuted = false
    
    override fun execute() {
        if (!isExecuted) {
            doExecute()
            isExecuted = true
        }
    }
    
    override fun undo() {
        if (isExecuted) {
            doUndo()
            isExecuted = false
        }
    }
    
    protected abstract fun doExecute()
    protected abstract fun doUndo()
}
```

### Text Editor Commands

```kotlin
// ==================== Document Model ====================

class Document {
    private val content = StringBuilder()
    private var cursorPosition = 0
    
    fun getText(): String = content.toString()
    
    fun getLength(): Int = content.length
    
    fun getCursor(): Int = cursorPosition
    
    fun setCursor(position: Int) {
        cursorPosition = position.coerceIn(0, content.length)
    }
    
    fun insert(text: String, position: Int) {
        val pos = position.coerceIn(0, content.length)
        content.insert(pos, text)
        cursorPosition = pos + text.length
    }
    
    fun delete(start: Int, end: Int): String {
        val s = start.coerceIn(0, content.length)
        val e = end.coerceIn(s, content.length)
        val deleted = content.substring(s, e)
        content.delete(s, e)
        cursorPosition = s
        return deleted
    }
    
    fun replace(start: Int, end: Int, text: String): String {
        val deleted = delete(start, end)
        insert(text, start)
        return deleted
    }
    
    override fun toString(): String = content.toString()
}

// ==================== Insert Command ====================

class InsertCommand(
    private val document: Document,
    private val text: String,
    private val position: Int
) : ReversibleCommand() {
    
    override fun doExecute() {
        document.insert(text, position)
    }
    
    override fun doUndo() {
        document.delete(position, position + text.length)
    }
    
    override fun getDescription(): String = "Insert '$text' at $position"
    
    override fun canMergeWith(other: Command): Boolean {
        return other is InsertCommand && 
               other.position == position + text.length &&
               text.length < 20 // Only merge small inserts
    }
    
    override fun mergeWith(other: Command): Command {
        if (other is InsertCommand) {
            return InsertCommand(document, text + other.text, position)
        }
        return this
    }
}

// ==================== Delete Command ====================

class DeleteCommand(
    private val document: Document,
    private val start: Int,
    private val end: Int
) : ReversibleCommand() {
    
    private var deletedText: String = ""
    
    override fun doExecute() {
        deletedText = document.delete(start, end)
    }
    
    override fun doUndo() {
        document.insert(deletedText, start)
    }
    
    override fun getDescription(): String = "Delete from $start to $end"
}

// ==================== Replace Command ====================

class ReplaceCommand(
    private val document: Document,
    private val start: Int,
    private val end: Int,
    private val newText: String
) : ReversibleCommand() {
    
    private var oldText: String = ""
    
    override fun doExecute() {
        oldText = document.replace(start, end, newText)
    }
    
    override fun doUndo() {
        document.replace(start, start + newText.length, oldText)
    }
    
    override fun getDescription(): String = "Replace '$oldText' with '$newText'"
}

// ==================== Format Command ====================

data class TextFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Int = 12
)

class FormatCommand(
    private val document: Document,
    private val start: Int,
    private val end: Int,
    private val newFormat: TextFormat,
    private val formatStore: MutableMap<IntRange, TextFormat>
) : ReversibleCommand() {
    
    private var oldFormat: TextFormat? = null
    private val range = start..end
    
    override fun doExecute() {
        oldFormat = formatStore[range]
        formatStore[range] = newFormat
    }
    
    override fun doUndo() {
        oldFormat?.let { formatStore[range] = it } ?: formatStore.remove(range)
    }
    
    override fun getDescription(): String = "Format text from $start to $end"
}
```

### Composite Command

```kotlin
// ==================== Composite Command ====================

class CompositeCommand(
    private val commands: List<Command>,
    private val description: String = "Composite Command"
) : Command {
    
    override fun execute() {
        commands.forEach { it.execute() }
    }
    
    override fun undo() {
        // Undo in reverse order
        commands.reversed().forEach { it.undo() }
    }
    
    override fun getDescription(): String = description
}

// Builder for composite commands
class CompositeCommandBuilder(private val description: String) {
    private val commands = mutableListOf<Command>()
    
    fun add(command: Command) = apply { commands.add(command) }
    
    fun build(): CompositeCommand = CompositeCommand(commands.toList(), description)
}
```

### Command Manager

```kotlin
// ==================== Command Manager ====================

/**
 * Manages command execution with undo/redo capability.
 * 
 * === Data Structure ===
 * Uses two stacks:
 * - undoStack: Commands that can be undone
 * - redoStack: Commands that were undone and can be redone
 * 
 * === Visual Representation ===
 * 
 *   Initial: undoStack=[], redoStack=[]
 *   
 *   execute(A): undoStack=[A], redoStack=[]
 *   execute(B): undoStack=[B,A], redoStack=[]
 *   execute(C): undoStack=[C,B,A], redoStack=[]
 *   
 *   undo():    undoStack=[B,A], redoStack=[C]
 *   undo():    undoStack=[A], redoStack=[C,B]
 *   
 *   redo():    undoStack=[B,A], redoStack=[C]
 *   
 *   execute(D): undoStack=[D,B,A], redoStack=[] ← redo cleared!
 * 
 * === Command Merging ===
 * Consecutive similar commands can be merged:
 * - Typing "H", "e", "l", "l", "o" → single "Hello" command
 * - Reduces undo steps for better UX
 * 
 * === Time Complexity ===
 * - execute: O(1) amortized
 * - undo: O(1)
 * - redo: O(1)
 * 
 * @param historyLimit Maximum commands to remember (prevents memory issues)
 * @param enableMerging Whether to merge consecutive similar commands
 */
class CommandManager(
    private val historyLimit: Int = 100,
    private val enableMerging: Boolean = true
) {
    private val undoStack = ArrayDeque<Command>()  // Commands to undo
    private val redoStack = ArrayDeque<Command>()  // Commands to redo
    
    private val listeners = mutableListOf<CommandListener>()
    
    // ==================== Execute ====================
    
    /**
     * Execute a command and add to undo history.
     * 
     * Steps:
     * 1. Try to merge with last command (if enabled)
     * 2. Execute the command
     * 3. Push to undo stack
     * 4. Clear redo stack (can't redo after new action)
     * 5. Enforce history limit
     */
    fun execute(command: Command) {
        // Try to merge with last command (e.g., consecutive typing)
        if (enableMerging && undoStack.isNotEmpty()) {
            val lastCommand = undoStack.peek()
            if (lastCommand.canMergeWith(command)) {
                undoStack.pop()
                val merged = lastCommand.mergeWith(command)
                merged.execute()
                undoStack.push(merged)
                redoStack.clear()
                notifyStateChanged()
                return
            }
        }
        
        // Execute and track
        command.execute()
        undoStack.push(command)
        
        // New action invalidates redo history
        redoStack.clear()
        
        // Enforce history limit (remove oldest)
        while (undoStack.size > historyLimit) {
            undoStack.removeLast()
        }
        
        notifyStateChanged()
    }
    
    // ==================== Undo/Redo ====================
    
    fun undo(): Boolean {
        if (!canUndo()) return false
        
        val command = undoStack.pop()
        command.undo()
        redoStack.push(command)
        
        notifyStateChanged()
        notifyUndo(command)
        return true
    }
    
    fun redo(): Boolean {
        if (!canRedo()) return false
        
        val command = redoStack.pop()
        command.execute()
        undoStack.push(command)
        
        notifyStateChanged()
        notifyRedo(command)
        return true
    }
    
    // ==================== Status ====================
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    fun getUndoDescription(): String? = undoStack.peek()?.getDescription()
    
    fun getRedoDescription(): String? = redoStack.peek()?.getDescription()
    
    fun getHistory(): List<String> = undoStack.map { it.getDescription() }
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        notifyStateChanged()
    }
    
    // ==================== Listeners ====================
    
    fun addListener(listener: CommandListener) {
        listeners.add(listener)
    }
    
    private fun notifyStateChanged() {
        listeners.forEach { it.onStateChanged(canUndo(), canRedo()) }
    }
    
    private fun notifyUndo(command: Command) {
        listeners.forEach { it.onUndo(command) }
    }
    
    private fun notifyRedo(command: Command) {
        listeners.forEach { it.onRedo(command) }
    }
}

// ==================== Listener ====================

interface CommandListener {
    fun onStateChanged(canUndo: Boolean, canRedo: Boolean)
    fun onUndo(command: Command) {}
    fun onRedo(command: Command) {}
}
```

### Text Editor with Undo/Redo

```kotlin
// ==================== Text Editor ====================

class TextEditor(
    historyLimit: Int = 100
) {
    private val document = Document()
    private val commandManager = CommandManager(historyLimit)
    private val formatStore = mutableMapOf<IntRange, TextFormat>()
    
    // ==================== Edit Operations ====================
    
    fun insert(text: String, position: Int = document.getCursor()) {
        val command = InsertCommand(document, text, position)
        commandManager.execute(command)
    }
    
    fun delete(start: Int, end: Int) {
        val command = DeleteCommand(document, start, end)
        commandManager.execute(command)
    }
    
    fun replace(start: Int, end: Int, newText: String) {
        val command = ReplaceCommand(document, start, end, newText)
        commandManager.execute(command)
    }
    
    fun format(start: Int, end: Int, format: TextFormat) {
        val command = FormatCommand(document, start, end, format, formatStore)
        commandManager.execute(command)
    }
    
    // Batch operation
    fun replaceAll(oldText: String, newText: String) {
        val builder = CompositeCommandBuilder("Replace all '$oldText' with '$newText'")
        
        var content = document.getText()
        var index = content.indexOf(oldText)
        val indices = mutableListOf<Int>()
        
        while (index >= 0) {
            indices.add(index)
            index = content.indexOf(oldText, index + 1)
        }
        
        // Create commands in reverse order to maintain positions
        indices.reversed().forEach { pos ->
            builder.add(ReplaceCommand(document, pos, pos + oldText.length, newText))
        }
        
        if (indices.isNotEmpty()) {
            commandManager.execute(builder.build())
        }
    }
    
    // ==================== Undo/Redo ====================
    
    fun undo(): Boolean = commandManager.undo()
    
    fun redo(): Boolean = commandManager.redo()
    
    fun canUndo(): Boolean = commandManager.canUndo()
    
    fun canRedo(): Boolean = commandManager.canRedo()
    
    // ==================== Getters ====================
    
    fun getText(): String = document.getText()
    
    fun getHistory(): List<String> = commandManager.getHistory()
    
    fun addListener(listener: CommandListener) {
        commandManager.addListener(listener)
    }
}
```

### Usage Example

```kotlin
fun main() {
    val editor = TextEditor()
    
    // Add listener
    editor.addListener(object : CommandListener {
        override fun onStateChanged(canUndo: Boolean, canRedo: Boolean) {
            println("Can Undo: $canUndo, Can Redo: $canRedo")
        }
        
        override fun onUndo(command: Command) {
            println("Undid: ${command.getDescription()}")
        }
        
        override fun onRedo(command: Command) {
            println("Redid: ${command.getDescription()}")
        }
    })
    
    println("=== Text Editor Demo ===\n")
    
    // Type some text
    editor.insert("Hello")
    println("After insert 'Hello': ${editor.getText()}")
    
    editor.insert(" World")
    println("After insert ' World': ${editor.getText()}")
    
    editor.insert("!")
    println("After insert '!': ${editor.getText()}")
    
    // Undo
    println("\n--- Undo ---")
    editor.undo()
    println("After undo: ${editor.getText()}")
    
    editor.undo()
    println("After undo: ${editor.getText()}")
    
    // Redo
    println("\n--- Redo ---")
    editor.redo()
    println("After redo: ${editor.getText()}")
    
    // Replace
    println("\n--- Replace ---")
    editor.replace(0, 5, "Hi")
    println("After replace 'Hello' with 'Hi': ${editor.getText()}")
    
    // Undo replace
    editor.undo()
    println("After undo replace: ${editor.getText()}")
    
    // Delete
    println("\n--- Delete ---")
    editor.delete(5, 11)
    println("After delete ' World': ${editor.getText()}")
    
    // Show history
    println("\n--- History ---")
    editor.getHistory().forEachIndexed { index, desc ->
        println("${index + 1}. $desc")
    }
    
    // Undo all
    println("\n--- Undo All ---")
    while (editor.canUndo()) {
        editor.undo()
    }
    println("Final text: '${editor.getText()}'")
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Command** | `Command` interface | Encapsulate operations as objects |
| **Composite** | `CompositeCommand` | Group commands together |
| **Memento** | Implicit in commands | Store state for undo |
| **Observer** | `CommandListener` | Notify of state changes |

---

## Interview Discussion Points

### Q: How to persist undo history?
**A:**
- Serialize commands to JSON
- Store in database or file
- Recreate command objects on load
- Consider storing document snapshots

### Q: How to handle collaborative editing?
**A:**
- Operational Transformation (OT)
- CRDTs (Conflict-free Replicated Data Types)
- Server-side command ordering
- Version vectors for conflict detection

### Q: Memory optimization for large documents?
**A:**
- Store deltas instead of full state
- Periodic snapshots with incremental changes
- Command compression/merging
- LRU eviction of old history

---

## Time & Space Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Execute | O(1) | O(command) |
| Undo | O(1) | O(1) |
| Redo | O(1) | O(1) |
| History | O(n) | O(n × command) |

---

## Edge Cases

1. **Undo when nothing to undo**
2. **Redo after new command** (clears redo stack)
3. **Concurrent modifications**
4. **Command merging boundaries**
5. **History limit enforcement**

