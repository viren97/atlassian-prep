# Design Patterns Cheatsheet - Kotlin

## Creational Patterns

---

### 1. Singleton

**Intent**: Ensure a class has only one instance with global access.

#### Variation 1: Object Declaration (Recommended)
```kotlin
// Thread-safe, lazy by default
object DatabaseConnection {
    init {
        println("Initializing database connection")
    }
    
    fun query(sql: String): List<String> {
        return listOf("result1", "result2")
    }
}

// Usage
DatabaseConnection.query("SELECT * FROM users")
```

#### Variation 2: Companion Object
```kotlin
class Logger private constructor() {
    companion object {
        val instance: Logger by lazy { Logger() }
        
        // Or direct initialization
        // val instance = Logger()
    }
    
    fun log(message: String) = println(message)
}

// Usage
Logger.instance.log("Hello")
```

#### Variation 3: Lazy Initialization (Thread-safe)
```kotlin
class ConfigManager private constructor() {
    companion object {
        @Volatile
        private var instance: ConfigManager? = null
        
        fun getInstance(): ConfigManager {
            return instance ?: synchronized(this) {
                instance ?: ConfigManager().also { instance = it }
            }
        }
    }
}
```

#### Variation 4: Enum Singleton
```kotlin
enum class AppState {
    INSTANCE;
    
    var isLoggedIn: Boolean = false
    
    fun login() { isLoggedIn = true }
    fun logout() { isLoggedIn = false }
}

// Usage
AppState.INSTANCE.login()
```

#### Variation 5: Singleton with Parameters
```kotlin
class Database private constructor(val connectionString: String) {
    companion object {
        @Volatile
        private var instance: Database? = null
        
        fun getInstance(connectionString: String): Database {
            return instance ?: synchronized(this) {
                instance ?: Database(connectionString).also { instance = it }
            }
        }
    }
}
```

#### Variation 6: Singleton using Dependency Injection
```kotlin
// Using Koin
val appModule = module {
    single { DatabaseConnection() }
}

// Using Dagger/Hilt
@Singleton
class UserRepository @Inject constructor() { }
```

---

### 2. Factory Method

**Intent**: Define interface for creating objects, let subclasses decide which class to instantiate.

#### Variation 1: Simple Factory
```kotlin
// Products
interface Button {
    fun render()
    fun onClick(action: () -> Unit)
}

class WindowsButton : Button {
    override fun render() = println("Windows Button")
    override fun onClick(action: () -> Unit) { action() }
}

class MacButton : Button {
    override fun render() = println("Mac Button")
    override fun onClick(action: () -> Unit) { action() }
}

// Simple Factory
object ButtonFactory {
    fun createButton(os: String): Button {
        return when (os.lowercase()) {
            "windows" -> WindowsButton()
            "mac" -> MacButton()
            else -> throw IllegalArgumentException("Unknown OS: $os")
        }
    }
}

// Usage
val button = ButtonFactory.createButton("windows")
```

#### Variation 2: Factory Method Pattern
```kotlin
// Abstract Creator
abstract class Dialog {
    abstract fun createButton(): Button
    
    fun render() {
        val button = createButton()
        button.render()
    }
}

// Concrete Creators
class WindowsDialog : Dialog() {
    override fun createButton(): Button = WindowsButton()
}

class MacDialog : Dialog() {
    override fun createButton(): Button = MacButton()
}

// Usage
val dialog: Dialog = WindowsDialog()
dialog.render()
```

#### Variation 3: Companion Object Factory
```kotlin
interface Vehicle {
    fun drive()
    
    companion object Factory {
        fun create(type: String): Vehicle = when (type) {
            "car" -> Car()
            "bike" -> Bike()
            else -> throw IllegalArgumentException()
        }
    }
}

class Car : Vehicle {
    override fun drive() = println("Driving car")
}

class Bike : Vehicle {
    override fun drive() = println("Riding bike")
}

// Usage
val vehicle = Vehicle.create("car")
```

#### Variation 4: Sealed Class Factory
```kotlin
sealed class Notification {
    abstract fun send(message: String)
    
    class Email(val address: String) : Notification() {
        override fun send(message: String) = println("Email to $address: $message")
    }
    
    class SMS(val phone: String) : Notification() {
        override fun send(message: String) = println("SMS to $phone: $message")
    }
    
    class Push(val token: String) : Notification() {
        override fun send(message: String) = println("Push to $token: $message")
    }
    
    companion object {
        fun create(type: String, target: String): Notification = when (type) {
            "email" -> Email(target)
            "sms" -> SMS(target)
            "push" -> Push(target)
            else -> throw IllegalArgumentException()
        }
    }
}
```

#### Variation 5: Generic Factory
```kotlin
inline fun <reified T : Any> create(): T {
    return when (T::class) {
        Car::class -> Car() as T
        Bike::class -> Bike() as T
        else -> throw IllegalArgumentException("Unknown type: ${T::class}")
    }
}

// Usage
val car: Car = create()
```

---

### 3. Abstract Factory

**Intent**: Create families of related objects without specifying concrete classes.

```kotlin
// Abstract Products
interface Button {
    fun paint()
}

interface Checkbox {
    fun paint()
}

// Concrete Products - Windows Family
class WindowsButton : Button {
    override fun paint() = println("Windows Button")
}

class WindowsCheckbox : Checkbox {
    override fun paint() = println("Windows Checkbox")
}

// Concrete Products - Mac Family
class MacButton : Button {
    override fun paint() = println("Mac Button")
}

class MacCheckbox : Checkbox {
    override fun paint() = println("Mac Checkbox")
}

// Abstract Factory
interface GUIFactory {
    fun createButton(): Button
    fun createCheckbox(): Checkbox
}

// Concrete Factories
class WindowsFactory : GUIFactory {
    override fun createButton(): Button = WindowsButton()
    override fun createCheckbox(): Checkbox = WindowsCheckbox()
}

class MacFactory : GUIFactory {
    override fun createButton(): Button = MacButton()
    override fun createCheckbox(): Checkbox = MacCheckbox()
}

// Client
class Application(private val factory: GUIFactory) {
    private lateinit var button: Button
    private lateinit var checkbox: Checkbox
    
    fun createUI() {
        button = factory.createButton()
        checkbox = factory.createCheckbox()
    }
    
    fun paint() {
        button.paint()
        checkbox.paint()
    }
}

// Usage
val factory: GUIFactory = if (System.getProperty("os.name").contains("Windows")) {
    WindowsFactory()
} else {
    MacFactory()
}
val app = Application(factory)
app.createUI()
app.paint()
```

---

### 4. Builder

**Intent**: Construct complex objects step by step.

#### Variation 1: Named/Default Parameters (Kotlin Idiomatic)
```kotlin
// Kotlin's built-in solution - often no Builder needed!
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val age: Int = 0,
    val phone: String? = null,
    val address: String? = null,
    val isActive: Boolean = true
)

// Usage - very clean!
val user = User(
    id = 1,
    name = "John",
    email = "john@example.com",
    age = 30
)
```

#### Variation 2: apply/also Builder Pattern
```kotlin
class HttpRequest {
    var url: String = ""
    var method: String = "GET"
    var headers: MutableMap<String, String> = mutableMapOf()
    var body: String? = null
    var timeout: Int = 30000
}

// Usage with apply
val request = HttpRequest().apply {
    url = "https://api.example.com/users"
    method = "POST"
    headers["Content-Type"] = "application/json"
    body = """{"name": "John"}"""
    timeout = 5000
}
```

#### Variation 3: DSL Builder
```kotlin
class HtmlBuilder {
    private val content = StringBuilder()
    
    fun head(block: HeadBuilder.() -> Unit) {
        content.append("<head>")
        content.append(HeadBuilder().apply(block).build())
        content.append("</head>")
    }
    
    fun body(block: BodyBuilder.() -> Unit) {
        content.append("<body>")
        content.append(BodyBuilder().apply(block).build())
        content.append("</body>")
    }
    
    fun build(): String = "<html>$content</html>"
}

class HeadBuilder {
    private var title = ""
    
    fun title(text: String) { title = text }
    fun build() = "<title>$title</title>"
}

class BodyBuilder {
    private val elements = mutableListOf<String>()
    
    fun h1(text: String) { elements.add("<h1>$text</h1>") }
    fun p(text: String) { elements.add("<p>$text</p>") }
    fun build() = elements.joinToString("")
}

fun html(block: HtmlBuilder.() -> Unit): String {
    return HtmlBuilder().apply(block).build()
}

// Usage
val page = html {
    head {
        title("My Page")
    }
    body {
        h1("Welcome")
        p("This is a paragraph")
    }
}
```

#### Variation 4: Traditional Builder
```kotlin
class Pizza private constructor(
    val size: String,
    val cheese: Boolean,
    val pepperoni: Boolean,
    val mushrooms: Boolean
) {
    class Builder(private val size: String) {
        private var cheese: Boolean = false
        private var pepperoni: Boolean = false
        private var mushrooms: Boolean = false
        
        fun cheese() = apply { cheese = true }
        fun pepperoni() = apply { pepperoni = true }
        fun mushrooms() = apply { mushrooms = true }
        
        fun build() = Pizza(size, cheese, pepperoni, mushrooms)
    }
}

// Usage
val pizza = Pizza.Builder("large")
    .cheese()
    .pepperoni()
    .build()
```

#### Variation 5: Builder with Required Parameters
```kotlin
class EmailMessage private constructor(
    val to: String,
    val subject: String,
    val body: String,
    val cc: List<String>,
    val bcc: List<String>,
    val attachments: List<String>
) {
    class Builder(
        private val to: String,      // Required
        private val subject: String  // Required
    ) {
        private var body: String = ""
        private var cc: List<String> = emptyList()
        private var bcc: List<String> = emptyList()
        private var attachments: List<String> = emptyList()
        
        fun body(body: String) = apply { this.body = body }
        fun cc(vararg addresses: String) = apply { cc = addresses.toList() }
        fun bcc(vararg addresses: String) = apply { bcc = addresses.toList() }
        fun attachments(vararg files: String) = apply { attachments = files.toList() }
        
        fun build() = EmailMessage(to, subject, body, cc, bcc, attachments)
    }
}

// Usage
val email = EmailMessage.Builder("john@example.com", "Hello")
    .body("How are you?")
    .cc("jane@example.com")
    .build()
```

---

### 5. Prototype

**Intent**: Create new objects by copying existing ones.

#### Variation 1: Data Class copy()
```kotlin
data class Employee(
    val id: Long,
    val name: String,
    val department: String,
    val salary: Double
)

// Usage - Kotlin data classes have built-in copy!
val original = Employee(1, "John", "Engineering", 100000.0)
val clone = original.copy(name = "Jane", id = 2)
```

#### Variation 2: Cloneable Interface
```kotlin
interface Prototype<T> {
    fun clone(): T
}

class Document(
    var title: String,
    var content: String,
    var formatting: MutableMap<String, String> = mutableMapOf()
) : Prototype<Document> {
    
    override fun clone(): Document {
        return Document(
            title = this.title,
            content = this.content,
            formatting = this.formatting.toMutableMap()  // Deep copy
        )
    }
}

// Usage
val original = Document("Report", "Content here")
original.formatting["font"] = "Arial"

val clone = original.clone()
clone.title = "Report Copy"
```

#### Variation 3: Prototype Registry
```kotlin
object ShapeRegistry {
    private val shapes = mutableMapOf<String, Shape>()
    
    fun register(key: String, shape: Shape) {
        shapes[key] = shape
    }
    
    fun get(key: String): Shape? {
        return shapes[key]?.clone()
    }
}

interface Shape {
    fun clone(): Shape
    fun draw()
}

data class Circle(var radius: Double) : Shape {
    override fun clone() = copy()
    override fun draw() = println("Circle with radius $radius")
}

data class Rectangle(var width: Double, var height: Double) : Shape {
    override fun clone() = copy()
    override fun draw() = println("Rectangle $width x $height")
}

// Usage
ShapeRegistry.register("small-circle", Circle(10.0))
ShapeRegistry.register("big-rectangle", Rectangle(100.0, 50.0))

val circle = ShapeRegistry.get("small-circle")
```

---

## Structural Patterns

---

### 6. Adapter

**Intent**: Convert interface of a class to another interface clients expect.

#### Variation 1: Class Adapter (Inheritance)
```kotlin
// Existing interface
interface MediaPlayer {
    fun play(filename: String)
}

// Adaptee - incompatible interface
class AdvancedMediaPlayer {
    fun playMp4(filename: String) = println("Playing MP4: $filename")
    fun playVlc(filename: String) = println("Playing VLC: $filename")
}

// Adapter
class MediaAdapter(private val advancedPlayer: AdvancedMediaPlayer) : MediaPlayer {
    override fun play(filename: String) {
        when {
            filename.endsWith(".mp4") -> advancedPlayer.playMp4(filename)
            filename.endsWith(".vlc") -> advancedPlayer.playVlc(filename)
            else -> println("Unsupported format")
        }
    }
}

// Usage
val player: MediaPlayer = MediaAdapter(AdvancedMediaPlayer())
player.play("movie.mp4")
```

#### Variation 2: Extension Function Adapter
```kotlin
// Third-party class we can't modify
class LegacyPrinter {
    fun printText(text: String) = println("Legacy: $text")
}

// Our interface
interface ModernPrinter {
    fun print(document: Document)
}

// Adapter as extension function
fun LegacyPrinter.toModernPrinter(): ModernPrinter {
    val legacy = this
    return object : ModernPrinter {
        override fun print(document: Document) {
            legacy.printText(document.content)
        }
    }
}

// Usage
val modernPrinter = LegacyPrinter().toModernPrinter()
```

#### Variation 3: Lambda Adapter
```kotlin
// Target interface
fun interface DataProcessor {
    fun process(data: String): String
}

// Adaptee
class LegacyProcessor {
    fun legacyProcess(input: String, callback: (String) -> Unit) {
        callback(input.uppercase())
    }
}

// Adapter
fun LegacyProcessor.toDataProcessor(): DataProcessor {
    return DataProcessor { data ->
        var result = ""
        legacyProcess(data) { result = it }
        result
    }
}
```

---

### 7. Decorator

**Intent**: Attach additional responsibilities to an object dynamically.

#### Variation 1: Interface Delegation
```kotlin
interface Coffee {
    fun cost(): Double
    fun description(): String
}

class SimpleCoffee : Coffee {
    override fun cost() = 2.0
    override fun description() = "Simple coffee"
}

// Decorators using delegation
class MilkDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun cost() = coffee.cost() + 0.5
    override fun description() = "${coffee.description()}, milk"
}

class SugarDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun cost() = coffee.cost() + 0.2
    override fun description() = "${coffee.description()}, sugar"
}

class WhipDecorator(private val coffee: Coffee) : Coffee by coffee {
    override fun cost() = coffee.cost() + 0.7
    override fun description() = "${coffee.description()}, whip"
}

// Usage
val coffee: Coffee = WhipDecorator(MilkDecorator(SimpleCoffee()))
println("${coffee.description()} costs $${coffee.cost()}")
// Output: Simple coffee, milk, whip costs $3.2
```

#### Variation 2: Extension Functions as Decorators
```kotlin
data class Message(val content: String)

fun Message.encrypt(): Message = Message("encrypted(${this.content})")
fun Message.compress(): Message = Message("compressed(${this.content})")
fun Message.addTimestamp(): Message = Message("[${System.currentTimeMillis()}] ${this.content}")

// Usage - chain decorators
val message = Message("Hello")
    .addTimestamp()
    .compress()
    .encrypt()
```

#### Variation 3: Higher-Order Function Decorator
```kotlin
// Decorator for functions
fun <T, R> ((T) -> R).logged(): (T) -> R = { input ->
    println("Input: $input")
    val result = this(input)
    println("Output: $result")
    result
}

fun <T, R> ((T) -> R).timed(): (T) -> R = { input ->
    val start = System.currentTimeMillis()
    val result = this(input)
    println("Execution time: ${System.currentTimeMillis() - start}ms")
    result
}

// Usage
val process: (Int) -> Int = { it * 2 }
val decoratedProcess = process.logged().timed()
decoratedProcess(5)
```

---

### 8. Facade

**Intent**: Provide unified interface to a set of interfaces in a subsystem.

```kotlin
// Complex subsystem classes
class CPU {
    fun freeze() = println("CPU freezing")
    fun jump(position: Long) = println("CPU jumping to $position")
    fun execute() = println("CPU executing")
}

class Memory {
    fun load(position: Long, data: ByteArray) = println("Memory loading data at $position")
}

class HardDrive {
    fun read(lba: Long, size: Int): ByteArray {
        println("Hard drive reading $size bytes from $lba")
        return ByteArray(size)
    }
}

// Facade
class ComputerFacade {
    private val cpu = CPU()
    private val memory = Memory()
    private val hardDrive = HardDrive()
    
    companion object {
        private const val BOOT_ADDRESS = 0L
        private const val BOOT_SECTOR = 0L
        private const val SECTOR_SIZE = 512
    }
    
    fun start() {
        cpu.freeze()
        memory.load(BOOT_ADDRESS, hardDrive.read(BOOT_SECTOR, SECTOR_SIZE))
        cpu.jump(BOOT_ADDRESS)
        cpu.execute()
    }
}

// Usage - simple interface
val computer = ComputerFacade()
computer.start()
```

---

### 9. Proxy

**Intent**: Provide surrogate or placeholder for another object.

#### Variation 1: Virtual Proxy (Lazy Loading)
```kotlin
interface Image {
    fun display()
}

class RealImage(private val filename: String) : Image {
    init {
        loadFromDisk()
    }
    
    private fun loadFromDisk() {
        println("Loading $filename from disk...")
        Thread.sleep(1000)  // Simulate slow loading
    }
    
    override fun display() = println("Displaying $filename")
}

class ImageProxy(private val filename: String) : Image {
    private val realImage: RealImage by lazy { RealImage(filename) }
    
    override fun display() {
        realImage.display()  // Loads only when needed
    }
}

// Usage
val image: Image = ImageProxy("large_photo.jpg")
// Image not loaded yet
image.display()  // Now it loads
```

#### Variation 2: Protection Proxy
```kotlin
interface Document {
    fun read(): String
    fun write(content: String)
}

class SecureDocument(private val content: String) : Document {
    override fun read() = content
    override fun write(content: String) = println("Writing: $content")
}

class DocumentProxy(
    private val document: Document,
    private val userRole: String
) : Document {
    
    override fun read(): String {
        return document.read()  // Everyone can read
    }
    
    override fun write(content: String) {
        if (userRole == "admin") {
            document.write(content)
        } else {
            throw SecurityException("Only admins can write")
        }
    }
}
```

#### Variation 3: Caching Proxy
```kotlin
interface UserService {
    fun getUser(id: Long): User
}

class UserServiceImpl : UserService {
    override fun getUser(id: Long): User {
        println("Fetching user $id from database...")
        Thread.sleep(100)
        return User(id, "User $id")
    }
}

class CachingUserServiceProxy(private val service: UserService) : UserService {
    private val cache = mutableMapOf<Long, User>()
    
    override fun getUser(id: Long): User {
        return cache.getOrPut(id) {
            println("Cache miss for user $id")
            service.getUser(id)
        }
    }
}
```

#### Variation 4: Kotlin Delegated Properties as Proxy
```kotlin
import kotlin.reflect.KProperty

class LoggingDelegate<T>(private var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        println("Getting ${property.name}: $value")
        return value
    }
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        println("Setting ${property.name}: $value -> $newValue")
        value = newValue
    }
}

class User {
    var name: String by LoggingDelegate("John")
    var age: Int by LoggingDelegate(0)
}

// Usage
val user = User()
user.name = "Jane"  // Prints: Setting name: John -> Jane
println(user.name)  // Prints: Getting name: Jane
```

---

### 10. Composite

**Intent**: Compose objects into tree structures.

```kotlin
interface FileSystemItem {
    val name: String
    fun getSize(): Long
    fun display(indent: String = "")
}

class File(
    override val name: String,
    private val size: Long
) : FileSystemItem {
    override fun getSize() = size
    override fun display(indent: String) {
        println("$indent📄 $name ($size bytes)")
    }
}

class Directory(override val name: String) : FileSystemItem {
    private val children = mutableListOf<FileSystemItem>()
    
    fun add(item: FileSystemItem) = children.add(item)
    fun remove(item: FileSystemItem) = children.remove(item)
    
    override fun getSize(): Long = children.sumOf { it.getSize() }
    
    override fun display(indent: String) {
        println("$indent📁 $name (${getSize()} bytes)")
        children.forEach { it.display("$indent  ") }
    }
}

// Usage
val root = Directory("root").apply {
    add(File("readme.txt", 100))
    add(Directory("src").apply {
        add(File("main.kt", 500))
        add(File("utils.kt", 300))
    })
    add(Directory("test").apply {
        add(File("test.kt", 200))
    })
}
root.display()
```

---

### 11. Bridge

**Intent**: Decouple abstraction from implementation.

```kotlin
// Implementor
interface Renderer {
    fun renderCircle(radius: Float)
    fun renderSquare(side: Float)
}

class VectorRenderer : Renderer {
    override fun renderCircle(radius: Float) = println("Drawing circle as vectors, radius: $radius")
    override fun renderSquare(side: Float) = println("Drawing square as vectors, side: $side")
}

class RasterRenderer : Renderer {
    override fun renderCircle(radius: Float) = println("Drawing circle as pixels, radius: $radius")
    override fun renderSquare(side: Float) = println("Drawing square as pixels, side: $side")
}

// Abstraction
abstract class Shape(protected val renderer: Renderer) {
    abstract fun draw()
    abstract fun resize(factor: Float)
}

class Circle(
    renderer: Renderer,
    private var radius: Float
) : Shape(renderer) {
    override fun draw() = renderer.renderCircle(radius)
    override fun resize(factor: Float) { radius *= factor }
}

class Square(
    renderer: Renderer,
    private var side: Float
) : Shape(renderer) {
    override fun draw() = renderer.renderSquare(side)
    override fun resize(factor: Float) { side *= factor }
}

// Usage
val vectorCircle = Circle(VectorRenderer(), 5f)
val rasterCircle = Circle(RasterRenderer(), 5f)

vectorCircle.draw()  // Drawing circle as vectors
rasterCircle.draw()  // Drawing circle as pixels
```

---

### 12. Flyweight

**Intent**: Share common state between objects to reduce memory.

```kotlin
// Flyweight - shared state
data class TreeType(
    val name: String,
    val color: String,
    val texture: String  // Heavy resource
)

// Flyweight Factory
object TreeTypeFactory {
    private val treeTypes = mutableMapOf<String, TreeType>()
    
    fun getTreeType(name: String, color: String, texture: String): TreeType {
        val key = "$name-$color"
        return treeTypes.getOrPut(key) {
            println("Creating new tree type: $key")
            TreeType(name, color, texture)
        }
    }
}

// Context - unique state
class Tree(
    private val x: Int,
    private val y: Int,
    private val type: TreeType  // Shared flyweight
) {
    fun draw() = println("Drawing ${type.name} at ($x, $y)")
}

// Forest using flyweights
class Forest {
    private val trees = mutableListOf<Tree>()
    
    fun plantTree(x: Int, y: Int, name: String, color: String, texture: String) {
        val type = TreeTypeFactory.getTreeType(name, color, texture)
        trees.add(Tree(x, y, type))
    }
    
    fun draw() = trees.forEach { it.draw() }
}

// Usage - 1M trees but only few TreeType objects
val forest = Forest()
repeat(1_000_000) { i ->
    forest.plantTree(i % 100, i / 100, "Oak", "Green", "oak_texture.png")
}
```

---

## Behavioral Patterns

---

### 13. Strategy

**Intent**: Define family of algorithms and make them interchangeable.

#### Variation 1: Interface Strategy
```kotlin
interface PaymentStrategy {
    fun pay(amount: Double)
}

class CreditCardPayment(private val cardNumber: String) : PaymentStrategy {
    override fun pay(amount: Double) = println("Paid $$amount with credit card $cardNumber")
}

class PayPalPayment(private val email: String) : PaymentStrategy {
    override fun pay(amount: Double) = println("Paid $$amount with PayPal ($email)")
}

class CryptoPayment(private val wallet: String) : PaymentStrategy {
    override fun pay(amount: Double) = println("Paid $$amount with crypto wallet $wallet")
}

class ShoppingCart {
    private var paymentStrategy: PaymentStrategy? = null
    
    fun setPaymentStrategy(strategy: PaymentStrategy) {
        paymentStrategy = strategy
    }
    
    fun checkout(amount: Double) {
        paymentStrategy?.pay(amount) ?: throw IllegalStateException("No payment method set")
    }
}

// Usage
val cart = ShoppingCart()
cart.setPaymentStrategy(CreditCardPayment("1234-5678"))
cart.checkout(100.0)
```

#### Variation 2: Lambda Strategy (Kotlin Idiomatic)
```kotlin
class Sorter<T> {
    fun sort(items: MutableList<T>, strategy: (T, T) -> Int) {
        items.sortWith { a, b -> strategy(a, b) }
    }
}

// Usage
val numbers = mutableListOf(5, 2, 8, 1, 9)

val sorter = Sorter<Int>()
sorter.sort(numbers) { a, b -> a - b }  // Ascending
sorter.sort(numbers) { a, b -> b - a }  // Descending
```

#### Variation 3: Sealed Class Strategy
```kotlin
sealed class CompressionStrategy {
    abstract fun compress(data: ByteArray): ByteArray
    
    object Zip : CompressionStrategy() {
        override fun compress(data: ByteArray) = data // zip implementation
    }
    
    object Gzip : CompressionStrategy() {
        override fun compress(data: ByteArray) = data // gzip implementation
    }
    
    class Custom(private val algorithm: (ByteArray) -> ByteArray) : CompressionStrategy() {
        override fun compress(data: ByteArray) = algorithm(data)
    }
}

class Compressor(private var strategy: CompressionStrategy = CompressionStrategy.Zip) {
    fun setStrategy(strategy: CompressionStrategy) { this.strategy = strategy }
    fun compress(data: ByteArray) = strategy.compress(data)
}
```

---

### 14. Observer

**Intent**: Define one-to-many dependency between objects.

#### Variation 1: Interface Observer
```kotlin
interface Observer<T> {
    fun update(value: T)
}

class Observable<T> {
    private val observers = mutableListOf<Observer<T>>()
    
    fun addObserver(observer: Observer<T>) = observers.add(observer)
    fun removeObserver(observer: Observer<T>) = observers.remove(observer)
    
    fun notifyObservers(value: T) {
        observers.forEach { it.update(value) }
    }
}

// Usage
class NewsPublisher : Observable<String>()

class EmailSubscriber(private val email: String) : Observer<String> {
    override fun update(value: String) {
        println("Sending '$value' to $email")
    }
}

val publisher = NewsPublisher()
publisher.addObserver(EmailSubscriber("john@example.com"))
publisher.addObserver(EmailSubscriber("jane@example.com"))
publisher.notifyObservers("Breaking News!")
```

#### Variation 2: Lambda Observer
```kotlin
class EventEmitter<T> {
    private val listeners = mutableListOf<(T) -> Unit>()
    
    fun on(listener: (T) -> Unit) {
        listeners.add(listener)
    }
    
    fun off(listener: (T) -> Unit) {
        listeners.remove(listener)
    }
    
    fun emit(value: T) {
        listeners.forEach { it(value) }
    }
}

// Usage
val onClick = EventEmitter<String>()
onClick.on { println("Button clicked: $it") }
onClick.emit("Submit")
```

#### Variation 3: Kotlin Flow (Reactive)
```kotlin
import kotlinx.coroutines.flow.*

class StockPriceService {
    private val _priceFlow = MutableSharedFlow<Double>()
    val priceFlow: SharedFlow<Double> = _priceFlow
    
    suspend fun updatePrice(price: Double) {
        _priceFlow.emit(price)
    }
}

// Usage
val service = StockPriceService()

// Observer 1
service.priceFlow.collect { price ->
    println("Display: $$price")
}

// Observer 2
service.priceFlow.collect { price ->
    if (price > 100) println("Alert: Price above $100!")
}
```

#### Variation 4: Kotlin Delegates.observable
```kotlin
import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("Initial") { prop, old, new ->
        println("${prop.name} changed from $old to $new")
    }
    
    var age: Int by Delegates.vetoable(0) { _, old, new ->
        new >= 0  // Reject negative values
    }
}
```

---

### 15. Command

**Intent**: Encapsulate request as an object.

```kotlin
// Command interface
interface Command {
    fun execute()
    fun undo()
}

// Receiver
class TextEditor {
    var text = StringBuilder()
    
    fun write(str: String) {
        text.append(str)
    }
    
    fun delete(length: Int) {
        if (length <= text.length) {
            text.delete(text.length - length, text.length)
        }
    }
}

// Concrete Commands
class WriteCommand(
    private val editor: TextEditor,
    private val text: String
) : Command {
    override fun execute() = editor.write(text)
    override fun undo() = editor.delete(text.length)
}

class DeleteCommand(
    private val editor: TextEditor,
    private val length: Int
) : Command {
    private var deletedText = ""
    
    override fun execute() {
        deletedText = editor.text.takeLast(length)
        editor.delete(length)
    }
    
    override fun undo() = editor.write(deletedText)
}

// Invoker
class CommandManager {
    private val history = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()
    
    fun execute(command: Command) {
        command.execute()
        history.add(command)
        redoStack.clear()
    }
    
    fun undo() {
        if (history.isNotEmpty()) {
            val command = history.removeLast()
            command.undo()
            redoStack.add(command)
        }
    }
    
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.removeLast()
            command.execute()
            history.add(command)
        }
    }
}

// Usage
val editor = TextEditor()
val manager = CommandManager()

manager.execute(WriteCommand(editor, "Hello "))
manager.execute(WriteCommand(editor, "World"))
println(editor.text)  // Hello World

manager.undo()
println(editor.text)  // Hello 

manager.redo()
println(editor.text)  // Hello World
```

---

### 16. State

**Intent**: Allow object to alter behavior when internal state changes.

```kotlin
// State interface
interface OrderState {
    fun next(order: Order)
    fun prev(order: Order)
    fun printStatus()
}

// Concrete States
object OrderedState : OrderState {
    override fun next(order: Order) { order.state = ShippedState }
    override fun prev(order: Order) { println("Order is in its initial state") }
    override fun printStatus() = println("Order placed, waiting to ship")
}

object ShippedState : OrderState {
    override fun next(order: Order) { order.state = DeliveredState }
    override fun prev(order: Order) { order.state = OrderedState }
    override fun printStatus() = println("Order shipped, in transit")
}

object DeliveredState : OrderState {
    override fun next(order: Order) { println("Order already delivered") }
    override fun prev(order: Order) { order.state = ShippedState }
    override fun printStatus() = println("Order delivered!")
}

// Context
class Order {
    var state: OrderState = OrderedState
    
    fun nextState() = state.next(this)
    fun prevState() = state.prev(this)
    fun printStatus() = state.printStatus()
}

// Usage
val order = Order()
order.printStatus()  // Order placed
order.nextState()
order.printStatus()  // Order shipped
order.nextState()
order.printStatus()  // Order delivered
```

#### Sealed Class State Machine
```kotlin
sealed class TrafficLightState {
    abstract val duration: Long
    abstract fun next(): TrafficLightState
    
    object Red : TrafficLightState() {
        override val duration = 5000L
        override fun next() = Green
    }
    
    object Yellow : TrafficLightState() {
        override val duration = 2000L
        override fun next() = Red
    }
    
    object Green : TrafficLightState() {
        override val duration = 4000L
        override fun next() = Yellow
    }
}

class TrafficLight {
    var state: TrafficLightState = TrafficLightState.Red
        private set
    
    fun change() {
        state = state.next()
    }
}
```

---

### 17. Template Method

**Intent**: Define skeleton of algorithm, let subclasses override specific steps.

```kotlin
abstract class DataMiner {
    // Template method
    fun mine(path: String) {
        val file = openFile(path)
        val rawData = extractData(file)
        val data = parseData(rawData)
        val analysis = analyzeData(data)
        sendReport(analysis)
        closeFile(file)
    }
    
    abstract fun openFile(path: String): Any
    abstract fun extractData(file: Any): String
    abstract fun parseData(rawData: String): Map<String, Any>
    abstract fun closeFile(file: Any)
    
    // Default implementation - can be overridden
    open fun analyzeData(data: Map<String, Any>): String {
        return "Analyzed: ${data.size} items"
    }
    
    open fun sendReport(analysis: String) {
        println("Report: $analysis")
    }
}

class PDFMiner : DataMiner() {
    override fun openFile(path: String) = "PDF:$path"
    override fun extractData(file: Any) = "PDF content"
    override fun parseData(rawData: String) = mapOf("type" to "pdf")
    override fun closeFile(file: Any) = println("Closed PDF")
}

class CSVMiner : DataMiner() {
    override fun openFile(path: String) = "CSV:$path"
    override fun extractData(file: Any) = "CSV content"
    override fun parseData(rawData: String) = mapOf("type" to "csv")
    override fun closeFile(file: Any) = println("Closed CSV")
}
```

---

### 18. Chain of Responsibility

**Intent**: Pass request along chain of handlers.

```kotlin
abstract class Handler {
    var next: Handler? = null
    
    fun setNext(handler: Handler): Handler {
        next = handler
        return handler
    }
    
    abstract fun handle(request: Request): Boolean
    
    protected fun passToNext(request: Request): Boolean {
        return next?.handle(request) ?: false
    }
}

data class Request(val type: String, val amount: Double)

class Manager : Handler() {
    override fun handle(request: Request): Boolean {
        return if (request.type == "leave" && request.amount <= 2) {
            println("Manager approved ${request.amount} days leave")
            true
        } else {
            passToNext(request)
        }
    }
}

class Director : Handler() {
    override fun handle(request: Request): Boolean {
        return if (request.type == "leave" && request.amount <= 5) {
            println("Director approved ${request.amount} days leave")
            true
        } else {
            passToNext(request)
        }
    }
}

class CEO : Handler() {
    override fun handle(request: Request): Boolean {
        return if (request.type == "leave") {
            println("CEO approved ${request.amount} days leave")
            true
        } else {
            println("Request type ${request.type} not supported")
            false
        }
    }
}

// Usage
val chain = Manager().apply {
    setNext(Director()).setNext(CEO())
}

chain.handle(Request("leave", 1.0))   // Manager approved
chain.handle(Request("leave", 4.0))   // Director approved
chain.handle(Request("leave", 10.0))  // CEO approved
```

---

### 19. Visitor

**Intent**: Define new operation without changing classes of elements.

```kotlin
interface DocumentElement {
    fun accept(visitor: DocumentVisitor)
}

class TextElement(val text: String) : DocumentElement {
    override fun accept(visitor: DocumentVisitor) = visitor.visit(this)
}

class ImageElement(val url: String) : DocumentElement {
    override fun accept(visitor: DocumentVisitor) = visitor.visit(this)
}

class TableElement(val rows: Int, val cols: Int) : DocumentElement {
    override fun accept(visitor: DocumentVisitor) = visitor.visit(this)
}

// Visitor interface
interface DocumentVisitor {
    fun visit(element: TextElement)
    fun visit(element: ImageElement)
    fun visit(element: TableElement)
}

// Concrete Visitors
class HtmlExporter : DocumentVisitor {
    val html = StringBuilder()
    
    override fun visit(element: TextElement) {
        html.append("<p>${element.text}</p>")
    }
    
    override fun visit(element: ImageElement) {
        html.append("<img src='${element.url}'/>")
    }
    
    override fun visit(element: TableElement) {
        html.append("<table>${element.rows}x${element.cols}</table>")
    }
}

class PlainTextExporter : DocumentVisitor {
    val text = StringBuilder()
    
    override fun visit(element: TextElement) {
        text.append(element.text + "\n")
    }
    
    override fun visit(element: ImageElement) {
        text.append("[Image: ${element.url}]\n")
    }
    
    override fun visit(element: TableElement) {
        text.append("[Table: ${element.rows}x${element.cols}]\n")
    }
}

// Usage
val elements = listOf(
    TextElement("Hello World"),
    ImageElement("photo.jpg"),
    TableElement(3, 4)
)

val htmlExporter = HtmlExporter()
elements.forEach { it.accept(htmlExporter) }
println(htmlExporter.html)
```

---

## Quick Reference

| Pattern | When to Use | Kotlin Feature |
|---------|-------------|----------------|
| Singleton | Single instance | `object` declaration |
| Factory | Create objects | Companion object, sealed class |
| Builder | Complex construction | Named params, DSL, `apply` |
| Adapter | Interface conversion | Extension functions |
| Decorator | Add behavior | Delegation (`by`), extensions |
| Strategy | Interchangeable algorithms | Lambdas, function types |
| Observer | Event handling | Flow, Delegates.observable |
| State | State machine | Sealed class |
| Command | Undo/redo, queue | Data class for commands |

