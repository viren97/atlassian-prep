/**
 * Undo-Redo System - LLD Implementation
 * 
 * Design an undo-redo system using the Command pattern.
 * Supports text editing operations with command merging.
 * 
 * Design Patterns: Command, Memento
 */
package lld.undoredo

import java.util.*

// ==================== Command Interface ====================

/**
 * Command interface for executable and undoable operations.
 */
interface Command {
    fun execute()
    fun undo()
    fun canMergeWith(other: Command): Boolean = false
    fun mergeWith(other: Command): Command = this
}

// ==================== Document (Receiver) ====================

/**
 * Text document that receives commands.
 */
class Document {
    private val content = StringBuilder()
    
    fun getText(): String = content.toString()
    
    fun setText(text: String) {
        content.clear()
        content.append(text)
    }
    
    fun insert(position: Int, text: String) {
        val pos = position.coerceIn(0, content.length)
        content.insert(pos, text)
    }
    
    fun delete(position: Int, length: Int) {
        val pos = position.coerceIn(0, content.length)
        val endPos = (pos + length).coerceAtMost(content.length)
        content.delete(pos, endPos)
    }
    
    fun replace(position: Int, length: Int, text: String) {
        delete(position, length)
        insert(position, text)
    }
    
    fun length(): Int = content.length
}

// ==================== Concrete Commands ====================

/**
 * Insert text command.
 */
class InsertTextCommand(
    private val document: Document,
    private val position: Int,
    private val text: String,
    private val timestamp: Long = System.currentTimeMillis()
) : Command {
    
    override fun execute() {
        document.insert(position, text)
    }
    
    override fun undo() {
        document.delete(position, text.length)
    }
    
    /**
     * Can merge with adjacent insertions (for smooth typing).
     */
    override fun canMergeWith(other: Command): Boolean {
        if (other !is InsertTextCommand) return false
        // Merge if: same document, adjacent position, within 1 second
        return other.position == position + text.length &&
               other.timestamp - timestamp < 1000
    }
    
    override fun mergeWith(other: Command): Command {
        if (!canMergeWith(other)) return this
        val otherInsert = other as InsertTextCommand
        return InsertTextCommand(
            document,
            position,
            text + otherInsert.text,
            timestamp
        )
    }
}

/**
 * Delete text command.
 */
class DeleteTextCommand(
    private val document: Document,
    private val position: Int,
    private val deletedText: String
) : Command {
    
    constructor(document: Document, position: Int, length: Int) : this(
        document,
        position,
        document.getText().substring(
            position.coerceIn(0, document.length()),
            (position + length).coerceAtMost(document.length())
        )
    )
    
    override fun execute() {
        document.delete(position, deletedText.length)
    }
    
    override fun undo() {
        document.insert(position, deletedText)
    }
}

/**
 * Replace text command.
 */
class ReplaceTextCommand(
    private val document: Document,
    private val position: Int,
    private val oldText: String,
    private val newText: String
) : Command {
    
    override fun execute() {
        document.replace(position, oldText.length, newText)
    }
    
    override fun undo() {
        document.replace(position, newText.length, oldText)
    }
}

/**
 * Composite command - batch multiple commands.
 */
class CompositeCommand(
    private val commands: List<Command>
) : Command {
    
    override fun execute() {
        commands.forEach { it.execute() }
    }
    
    override fun undo() {
        commands.reversed().forEach { it.undo() }
    }
}

// ==================== Command Listener ====================

interface CommandListener {
    fun onCommandExecuted(command: Command)
    fun onUndo(command: Command)
    fun onRedo(command: Command)
    fun onStateChanged(canUndo: Boolean, canRedo: Boolean)
}

// ==================== Command Manager ====================

/**
 * Manages command execution with undo/redo support.
 * 
 * === Data Structure ===
 * - undoStack: Commands that can be undone
 * - redoStack: Undone commands that can be redone
 * 
 * === Code Flow: execute(command) ===
 * 1. Try to merge with last command (for smooth typing)
 * 2. Execute the command
 * 3. Push to undo stack
 * 4. Clear redo stack (new action invalidates redo)
 * 5. Enforce history limit
 * 
 * === Code Flow: undo() ===
 * 1. Pop from undo stack
 * 2. Call command.undo()
 * 3. Push to redo stack
 * 
 * === Code Flow: redo() ===
 * 1. Pop from redo stack
 * 2. Call command.execute()
 * 3. Push to undo stack
 */
class CommandManager(
    private val historyLimit: Int = 100,
    private val enableMerging: Boolean = true
) {
    private val undoStack = ArrayDeque<Command>()
    private val redoStack = ArrayDeque<Command>()
    
    private val listeners = mutableListOf<CommandListener>()
    
    /**
     * Execute a command and add to history.
     */
    fun execute(command: Command) {
        // Try to merge with last command
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
        
        command.execute()
        undoStack.push(command)
        redoStack.clear()
        
        // Enforce history limit
        while (undoStack.size > historyLimit) {
            undoStack.removeLast()
        }
        
        notifyStateChanged()
        listeners.forEach { it.onCommandExecuted(command) }
    }
    
    /**
     * Undo the last command.
     */
    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        
        val command = undoStack.pop()
        command.undo()
        redoStack.push(command)
        
        notifyStateChanged()
        listeners.forEach { it.onUndo(command) }
        return true
    }
    
    /**
     * Redo the last undone command.
     */
    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        
        val command = redoStack.pop()
        command.execute()
        undoStack.push(command)
        
        notifyStateChanged()
        listeners.forEach { it.onRedo(command) }
        return true
    }
    
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    fun getUndoCount(): Int = undoStack.size
    
    fun getRedoCount(): Int = redoStack.size
    
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        notifyStateChanged()
    }
    
    fun addListener(listener: CommandListener) {
        listeners.add(listener)
    }
    
    private fun notifyStateChanged() {
        listeners.forEach { it.onStateChanged(canUndo(), canRedo()) }
    }
}

// ==================== Text Editor ====================

/**
 * Simple text editor with undo/redo support.
 */
class TextEditor {
    private val document = Document()
    private val commandManager = CommandManager()
    
    var cursorPosition: Int = 0
        private set
    
    fun getText(): String = document.getText()
    
    fun insert(text: String) {
        val command = InsertTextCommand(document, cursorPosition, text)
        commandManager.execute(command)
        cursorPosition += text.length
    }
    
    fun delete(length: Int) {
        if (cursorPosition == 0) return
        val deleteStart = (cursorPosition - length).coerceAtLeast(0)
        val deleteLen = cursorPosition - deleteStart
        val deletedText = document.getText().substring(deleteStart, cursorPosition)
        val command = DeleteTextCommand(document, deleteStart, deletedText)
        commandManager.execute(command)
        cursorPosition = deleteStart
    }
    
    fun replace(length: Int, newText: String) {
        val oldText = document.getText().substring(cursorPosition, 
            (cursorPosition + length).coerceAtMost(document.length()))
        val command = ReplaceTextCommand(document, cursorPosition, oldText, newText)
        commandManager.execute(command)
        cursorPosition += newText.length
    }
    
    fun moveCursor(position: Int) {
        cursorPosition = position.coerceIn(0, document.length())
    }
    
    fun undo(): Boolean {
        val result = commandManager.undo()
        cursorPosition = cursorPosition.coerceIn(0, document.length())
        return result
    }
    
    fun redo(): Boolean {
        val result = commandManager.redo()
        cursorPosition = cursorPosition.coerceIn(0, document.length())
        return result
    }
    
    fun canUndo() = commandManager.canUndo()
    fun canRedo() = commandManager.canRedo()
}

// ==================== Usage Example ====================

fun main() {
    println("=== Undo-Redo System ===\n")
    
    val editor = TextEditor()
    
    // Type "Hello World"
    editor.insert("Hello")
    println("After 'Hello': '${editor.getText()}'")
    
    editor.insert(" ")
    editor.insert("World")
    println("After ' World': '${editor.getText()}'")
    
    // Undo
    editor.undo()
    println("After undo: '${editor.getText()}'")
    
    editor.undo()
    println("After undo: '${editor.getText()}'")
    
    // Redo
    editor.redo()
    println("After redo: '${editor.getText()}'")
    
    // More typing
    editor.insert("!")
    println("After '!': '${editor.getText()}'")
    
    // Can't redo after new action
    println("Can redo? ${editor.canRedo()}")
    
    println("\n--- Document Operations ---")
    
    // Direct document manipulation
    val doc = Document()
    val manager = CommandManager()
    
    // Batch operation example
    val findReplace = CompositeCommand(listOf(
        InsertTextCommand(doc, 0, "Hello World"),
    ))
    
    manager.execute(findReplace)
    println("Document: '${doc.getText()}'")
    
    // Insert at position
    manager.execute(InsertTextCommand(doc, 5, " Beautiful"))
    println("After insert: '${doc.getText()}'")
    
    // Undo composite
    manager.undo()
    println("After undo: '${doc.getText()}'")
    
    manager.undo()
    println("After undo: '${doc.getText()}'")
    
    println("\nUndo count: ${manager.getUndoCount()}")
    println("Redo count: ${manager.getRedoCount()}")
}

