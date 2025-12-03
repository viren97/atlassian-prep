# Design Patterns Cheatsheet - With Real-World Examples

> Design patterns are reusable solutions to common problems. This guide explains **WHEN** and **WHY** to use each pattern with real-world scenarios.

---

## Quick Reference: When to Use What?

| Problem | Pattern | Real-World Example |
|---------|---------|-------------------|
| Need only ONE instance | Singleton | Database connection, Logger |
| Create objects without specifying class | Factory | Payment processors, Notifications |
| Build complex objects step-by-step | Builder | HTTP requests, SQL queries |
| Add features without changing code | Decorator | Adding toppings to coffee |
| Switch algorithms at runtime | Strategy | Payment methods, Sorting |
| Notify multiple objects of changes | Observer | YouTube subscriptions, Event listeners |
| Convert incompatible interfaces | Adapter | Power plug adapters, API wrappers |
| Control access to an object | Proxy | Lazy loading images, Access control |
| Execute and undo operations | Command | Undo/Redo in text editors |
| Object behaves differently based on state | State | Order status, Traffic lights |

---

# CREATIONAL PATTERNS
*"How do we create objects?"*

---

## 1. Singleton Pattern

### What is it?
Ensures a class has **only ONE instance** throughout the application.

### Real-World Analogy
Think of the **President of a country** - there's only one at a time. You don't create a new president every time you need to talk to the government.

### When to Use?
- ✅ Database connections (expensive to create)
- ✅ Configuration/Settings manager
- ✅ Logger (one log file for entire app)
- ✅ Cache manager
- ✅ Thread pools

### When NOT to Use?
- ❌ When you need multiple instances
- ❌ When it makes testing difficult
- ❌ When it creates tight coupling

### Implementation

```kotlin
// ==========================================
// VARIATION 1: Object Declaration (Kotlin's way)
// Best for: Simple singletons without parameters
// ==========================================

object DatabaseConnection {
    private var connectionCount = 0
    
    init {
        println("Database connection initialized")
        // This runs only ONCE when first accessed
    }
    
    fun query(sql: String): List<String> {
        connectionCount++
        println("Executing query #$connectionCount: $sql")
        return listOf("result1", "result2")
    }
}

// Usage - anywhere in your app:
DatabaseConnection.query("SELECT * FROM users")
DatabaseConnection.query("SELECT * FROM orders")
// Both use the SAME instance!
```

```kotlin
// ==========================================
// VARIATION 2: Singleton with Parameters
// Best for: When singleton needs configuration
// ==========================================

class AppConfig private constructor(
    val apiUrl: String,
    val timeout: Int
) {
    companion object {
        @Volatile  // Ensures visibility across threads
        private var instance: AppConfig? = null
        
        fun initialize(apiUrl: String, timeout: Int): AppConfig {
            return instance ?: synchronized(this) {
                instance ?: AppConfig(apiUrl, timeout).also { instance = it }
            }
        }
        
        fun getInstance(): AppConfig {
            return instance ?: throw IllegalStateException("AppConfig not initialized!")
        }
    }
}

// Usage:
// At app startup:
AppConfig.initialize("https://api.example.com", 30)

// Anywhere else:
val config = AppConfig.getInstance()
println(config.apiUrl)
```

```kotlin
// ==========================================
// VARIATION 3: Lazy Singleton
// Best for: Expensive initialization, delay until needed
// ==========================================

class HeavyResource private constructor() {
    init {
        println("Loading heavy resource...")
        Thread.sleep(2000)  // Simulating expensive operation
    }
    
    companion object {
        val instance: HeavyResource by lazy {
            // This block runs only ONCE, when first accessed
            // Thread-safe by default
            HeavyResource()
        }
    }
    
    fun doWork() = println("Working...")
}

// Usage:
// Resource is NOT loaded yet
println("App started")
// Resource loads NOW (first access)
HeavyResource.instance.doWork()
// Uses same instance
HeavyResource.instance.doWork()
```

---

## 2. Factory Pattern

### What is it?
Creates objects **without exposing creation logic**. Client asks for what they need, factory decides how to create it.

### Real-World Analogy
Think of a **Pizza shop**. You say "I want a Margherita pizza" - you don't care how they make it, which oven they use, or who the chef is. The kitchen (factory) handles all that.

### When to Use?
- ✅ Creating objects based on conditions (user type, config, etc.)
- ✅ When object creation is complex
- ✅ When you want to decouple client from concrete classes
- ✅ Payment processors (Stripe, PayPal, etc.)
- ✅ Database drivers (MySQL, PostgreSQL, etc.)
- ✅ UI components for different platforms

### Implementation

```kotlin
// ==========================================
// SCENARIO: Payment Processing System
// User selects payment method, system creates appropriate processor
// ==========================================

// Step 1: Define what all payment processors can do
interface PaymentProcessor {
    fun processPayment(amount: Double): Boolean
    fun refund(transactionId: String): Boolean
}

// Step 2: Implement different payment processors
class CreditCardProcessor : PaymentProcessor {
    override fun processPayment(amount: Double): Boolean {
        println("Processing $$amount via Credit Card...")
        println("Connecting to bank... Validating card... Done!")
        return true
    }
    
    override fun refund(transactionId: String): Boolean {
        println("Refunding transaction $transactionId to card")
        return true
    }
}

class PayPalProcessor : PaymentProcessor {
    override fun processPayment(amount: Double): Boolean {
        println("Processing $$amount via PayPal...")
        println("Redirecting to PayPal... Authenticating... Done!")
        return true
    }
    
    override fun refund(transactionId: String): Boolean {
        println("Refunding via PayPal")
        return true
    }
}

class UPIProcessor : PaymentProcessor {
    override fun processPayment(amount: Double): Boolean {
        println("Processing ₹$amount via UPI...")
        println("Generating QR code... Waiting for payment... Done!")
        return true
    }
    
    override fun refund(transactionId: String): Boolean {
        println("Refunding to UPI ID")
        return true
    }
}

// Step 3: Factory that creates the right processor
object PaymentProcessorFactory {
    
    fun createProcessor(method: String): PaymentProcessor {
        return when (method.lowercase()) {
            "credit_card", "card" -> CreditCardProcessor()
            "paypal" -> PayPalProcessor()
            "upi" -> UPIProcessor()
            else -> throw IllegalArgumentException("Unknown payment method: $method")
        }
    }
}

// Step 4: Usage - Client code doesn't know about specific classes
fun checkout(amount: Double, paymentMethod: String) {
    // Factory creates the right processor
    val processor = PaymentProcessorFactory.createProcessor(paymentMethod)
    
    // Use it - same interface regardless of which processor
    if (processor.processPayment(amount)) {
        println("Payment successful!")
    }
}

// Usage:
checkout(99.99, "credit_card")  // Uses CreditCardProcessor
checkout(49.99, "paypal")        // Uses PayPalProcessor
checkout(199.0, "upi")           // Uses UPIProcessor
```

```kotlin
// ==========================================
// VARIATION: Factory with Sealed Classes
// Best for: Type-safe, exhaustive handling
// ==========================================

sealed class Notification {
    abstract fun send(userId: String, message: String)
    
    class Email(private val smtpServer: String) : Notification() {
        override fun send(userId: String, message: String) {
            println("📧 Sending email to $userId: $message")
        }
    }
    
    class SMS(private val twilioKey: String) : Notification() {
        override fun send(userId: String, message: String) {
            println("📱 Sending SMS to $userId: $message")
        }
    }
    
    class Push(private val firebaseToken: String) : Notification() {
        override fun send(userId: String, message: String) {
            println("🔔 Sending push notification to $userId: $message")
        }
    }
    
    companion object {
        fun create(type: String, config: String): Notification = when (type) {
            "email" -> Email(config)
            "sms" -> SMS(config)
            "push" -> Push(config)
            else -> throw IllegalArgumentException("Unknown notification type")
        }
    }
}

// Usage:
val notification = Notification.create("email", "smtp.gmail.com")
notification.send("user123", "Your order has shipped!")
```

---

## 3. Builder Pattern

### What is it?
Constructs **complex objects step-by-step**. Allows creating different representations using same building process.

### Real-World Analogy
Think of **ordering a custom burger**:
- "I want a burger" (base)
- "Add cheese" (optional)
- "Add bacon" (optional)
- "No onions" (optional)
- "Extra sauce" (optional)

You build it step by step, and at the end you get your customized burger.

### When to Use?
- ✅ Object has many optional parameters
- ✅ Object construction is complex
- ✅ You want readable object creation
- ✅ HTTP requests, SQL queries, test data

### When NOT to Use?
- ❌ Simple objects with few parameters
- ❌ All parameters are required (use constructor)

### Implementation

```kotlin
// ==========================================
// VARIATION 1: Kotlin's Built-in Way (Default Parameters)
// Best for: Most cases in Kotlin - simple and clean
// ==========================================

data class HttpRequest(
    val url: String,                          // Required
    val method: String = "GET",               // Optional with default
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeout: Int = 30000,
    val retries: Int = 3
)

// Usage - very readable!
val request1 = HttpRequest(
    url = "https://api.example.com/users"
)

val request2 = HttpRequest(
    url = "https://api.example.com/users",
    method = "POST",
    body = """{"name": "John"}""",
    headers = mapOf("Content-Type" to "application/json"),
    timeout = 5000
)
```

```kotlin
// ==========================================
// VARIATION 2: DSL Builder (Domain Specific Language)
// Best for: Complex nested structures, configuration
// ==========================================

// Building an HTML page using DSL
class Html {
    private val content = StringBuilder()
    
    fun head(block: Head.() -> Unit) {
        val head = Head().apply(block)
        content.append("<head>${head.build()}</head>")
    }
    
    fun body(block: Body.() -> Unit) {
        val body = Body().apply(block)
        content.append("<body>${body.build()}</body>")
    }
    
    fun build() = "<html>$content</html>"
}

class Head {
    private var title = ""
    fun title(text: String) { title = text }
    fun build() = "<title>$title</title>"
}

class Body {
    private val elements = mutableListOf<String>()
    fun h1(text: String) { elements.add("<h1>$text</h1>") }
    fun p(text: String) { elements.add("<p>$text</p>") }
    fun build() = elements.joinToString("\n")
}

// Helper function
fun html(block: Html.() -> Unit): String = Html().apply(block).build()

// Usage - reads like a document!
val page = html {
    head {
        title("My Website")
    }
    body {
        h1("Welcome!")
        p("This is my first paragraph.")
        p("This is another paragraph.")
    }
}
println(page)
```

```kotlin
// ==========================================
// VARIATION 3: Traditional Builder (Java-style)
// Best for: When you need validation during build
// ==========================================

class Email private constructor(
    val to: String,
    val subject: String,
    val body: String,
    val cc: List<String>,
    val bcc: List<String>,
    val attachments: List<String>,
    val isHtml: Boolean
) {
    class Builder(
        private val to: String,        // Required in constructor
        private val subject: String    // Required in constructor
    ) {
        private var body: String = ""
        private var cc: List<String> = emptyList()
        private var bcc: List<String> = emptyList()
        private var attachments: List<String> = emptyList()
        private var isHtml: Boolean = false
        
        fun body(body: String) = apply { this.body = body }
        fun cc(vararg addresses: String) = apply { cc = addresses.toList() }
        fun bcc(vararg addresses: String) = apply { bcc = addresses.toList() }
        fun attachments(vararg files: String) = apply { attachments = files.toList() }
        fun html() = apply { isHtml = true }
        
        fun build(): Email {
            // Validation
            require(to.contains("@")) { "Invalid email address" }
            require(subject.isNotBlank()) { "Subject cannot be blank" }
            
            return Email(to, subject, body, cc, bcc, attachments, isHtml)
        }
    }
    
    override fun toString() = "Email to: $to, subject: $subject"
}

// Usage:
val email = Email.Builder("john@example.com", "Meeting Tomorrow")
    .body("Hi John, let's meet at 3pm.")
    .cc("boss@example.com", "team@example.com")
    .attachments("agenda.pdf")
    .build()
```

---

# STRUCTURAL PATTERNS
*"How do we compose objects?"*

---

## 4. Decorator Pattern

### What is it?
**Adds new behavior** to objects dynamically by wrapping them, without modifying original class.

### Real-World Analogy
Think of **coffee at Starbucks**:
- Start with: Base coffee ($2)
- Add milk: +$0.50
- Add sugar: +$0.20
- Add whipped cream: +$0.70
- Add caramel: +$0.60

Each addition "decorates" the coffee with new features and price.

### When to Use?
- ✅ Add features to objects without inheritance
- ✅ Features can be combined in different ways
- ✅ Java I/O streams (BufferedInputStream, etc.)
- ✅ Adding logging, caching, validation to services

### Implementation

```kotlin
// ==========================================
// SCENARIO: Coffee Shop Ordering System
// ==========================================

// Base interface - what every coffee can do
interface Coffee {
    fun cost(): Double
    fun description(): String
}

// Basic coffee - the starting point
class SimpleCoffee : Coffee {
    override fun cost() = 2.00
    override fun description() = "Simple Coffee"
}

class Espresso : Coffee {
    override fun cost() = 2.50
    override fun description() = "Espresso"
}

// Decorator base - wraps any coffee
abstract class CoffeeDecorator(
    protected val coffee: Coffee
) : Coffee {
    override fun cost() = coffee.cost()
    override fun description() = coffee.description()
}

// Concrete decorators - each adds something
class MilkDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 0.50
    override fun description() = "${super.description()} + Milk"
}

class SugarDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 0.20
    override fun description() = "${super.description()} + Sugar"
}

class WhipCreamDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 0.70
    override fun description() = "${super.description()} + Whip Cream"
}

class CaramelDecorator(coffee: Coffee) : CoffeeDecorator(coffee) {
    override fun cost() = super.cost() + 0.60
    override fun description() = "${super.description()} + Caramel"
}

// Usage - compose decorators!
fun main() {
    // Simple coffee
    val basic: Coffee = SimpleCoffee()
    println("${basic.description()} = $${basic.cost()}")
    // Output: Simple Coffee = $2.0
    
    // Coffee with milk
    val withMilk: Coffee = MilkDecorator(SimpleCoffee())
    println("${withMilk.description()} = $${withMilk.cost()}")
    // Output: Simple Coffee + Milk = $2.5
    
    // Fancy coffee with everything!
    val fancy: Coffee = CaramelDecorator(
        WhipCreamDecorator(
            MilkDecorator(
                SugarDecorator(
                    Espresso()
                )
            )
        )
    )
    println("${fancy.description()} = $${fancy.cost()}")
    // Output: Espresso + Sugar + Milk + Whip Cream + Caramel = $4.5
}
```

```kotlin
// ==========================================
// REAL-WORLD: Decorating Services with Logging & Caching
// ==========================================

interface UserService {
    fun getUser(id: String): User?
    fun saveUser(user: User)
}

class UserServiceImpl(private val db: Database) : UserService {
    override fun getUser(id: String): User? {
        return db.findById(id)
    }
    
    override fun saveUser(user: User) {
        db.save(user)
    }
}

// Decorator: Add logging
class LoggingUserService(
    private val service: UserService
) : UserService {
    override fun getUser(id: String): User? {
        println("[LOG] Getting user: $id")
        val user = service.getUser(id)
        println("[LOG] Found: ${user != null}")
        return user
    }
    
    override fun saveUser(user: User) {
        println("[LOG] Saving user: ${user.id}")
        service.saveUser(user)
        println("[LOG] Saved successfully")
    }
}

// Decorator: Add caching
class CachingUserService(
    private val service: UserService
) : UserService {
    private val cache = mutableMapOf<String, User>()
    
    override fun getUser(id: String): User? {
        // Check cache first
        cache[id]?.let { 
            println("[CACHE] Hit for: $id")
            return it 
        }
        
        // Miss - fetch and cache
        println("[CACHE] Miss for: $id")
        return service.getUser(id)?.also { cache[id] = it }
    }
    
    override fun saveUser(user: User) {
        service.saveUser(user)
        cache[user.id] = user  // Update cache
    }
}

// Usage - compose decorators!
val db = Database()
val userService: UserService = LoggingUserService(
    CachingUserService(
        UserServiceImpl(db)
    )
)
// Now every call is: Logged → Cached → Actual service
```

---

## 5. Adapter Pattern

### What is it?
**Converts interface** of one class to another interface that client expects. Makes incompatible things work together.

### Real-World Analogy
Think of **power plug adapters** when traveling:
- Your laptop has a US plug
- European outlet has different shape
- Adapter converts US plug to work with European outlet

The laptop doesn't change, the outlet doesn't change, but they can now work together.

### When to Use?
- ✅ Integrating with third-party libraries
- ✅ Legacy code integration
- ✅ When you can't modify existing code
- ✅ API version compatibility

### Implementation

```kotlin
// ==========================================
// SCENARIO: Integrating Old Payment System with New Interface
// ==========================================

// New interface our system expects
interface ModernPaymentGateway {
    fun pay(amount: Double, currency: String): PaymentResult
    fun getStatus(transactionId: String): String
}

data class PaymentResult(
    val success: Boolean,
    val transactionId: String,
    val message: String
)

// Old third-party library we can't modify
// (Imagine this comes from a JAR file)
class LegacyPaymentSystem {
    fun makePayment(cents: Int): String {
        println("Legacy system processing ${cents} cents...")
        return "TXN_${System.currentTimeMillis()}"
    }
    
    fun checkPayment(txnId: String): Int {
        // Returns: 0 = pending, 1 = success, -1 = failed
        return 1
    }
}

// Adapter - makes legacy system work with our interface
class LegacyPaymentAdapter(
    private val legacySystem: LegacyPaymentSystem
) : ModernPaymentGateway {
    
    override fun pay(amount: Double, currency: String): PaymentResult {
        // Convert dollars to cents (legacy uses cents)
        val cents = (amount * 100).toInt()
        
        // Call legacy system
        val txnId = legacySystem.makePayment(cents)
        
        // Convert response to our format
        return PaymentResult(
            success = true,
            transactionId = txnId,
            message = "Payment processed via legacy system"
        )
    }
    
    override fun getStatus(transactionId: String): String {
        // Convert legacy status codes to readable strings
        return when (legacySystem.checkPayment(transactionId)) {
            0 -> "PENDING"
            1 -> "SUCCESS"
            -1 -> "FAILED"
            else -> "UNKNOWN"
        }
    }
}

// Usage - client uses modern interface, unaware of legacy system
fun processOrder(gateway: ModernPaymentGateway, amount: Double) {
    val result = gateway.pay(amount, "USD")
    if (result.success) {
        println("Order confirmed! Transaction: ${result.transactionId}")
    }
}

// With adapter:
val legacySystem = LegacyPaymentSystem()
val gateway: ModernPaymentGateway = LegacyPaymentAdapter(legacySystem)
processOrder(gateway, 99.99)
```

---

## 6. Proxy Pattern

### What is it?
Provides a **surrogate or placeholder** for another object to control access to it.

### Real-World Analogy
Think of a **credit card**:
- It's a proxy for your bank account
- You don't carry cash (real money)
- Card controls access (PIN, limit checks)
- Card can add features (logging purchases, fraud detection)

### Types of Proxy
1. **Virtual Proxy**: Lazy loading expensive objects
2. **Protection Proxy**: Access control
3. **Caching Proxy**: Cache results
4. **Logging Proxy**: Log all operations

### Implementation

```kotlin
// ==========================================
// SCENARIO 1: Virtual Proxy - Lazy Loading Images
// ==========================================

interface Image {
    fun display()
}

// Real image - expensive to load
class HighResolutionImage(private val filename: String) : Image {
    init {
        loadFromDisk()
    }
    
    private fun loadFromDisk() {
        println("Loading $filename from disk... (takes 3 seconds)")
        Thread.sleep(3000)
        println("$filename loaded!")
    }
    
    override fun display() {
        println("Displaying $filename")
    }
}

// Proxy - delays loading until actually needed
class ImageProxy(private val filename: String) : Image {
    private var realImage: HighResolutionImage? = null
    
    override fun display() {
        // Load only when display() is called
        if (realImage == null) {
            println("First access - loading image...")
            realImage = HighResolutionImage(filename)
        }
        realImage!!.display()
    }
}

// Usage:
fun main() {
    // Create proxy - image NOT loaded yet (instant)
    val image: Image = ImageProxy("vacation_photo.jpg")
    println("Proxy created")
    
    // ... later in the app ...
    
    // NOW the image loads
    image.display()
    
    // Second call - already loaded, instant
    image.display()
}
```

```kotlin
// ==========================================
// SCENARIO 2: Protection Proxy - Access Control
// ==========================================

interface Document {
    fun read(): String
    fun write(content: String)
    fun delete()
}

class SecureDocument(private var content: String) : Document {
    override fun read() = content
    override fun write(content: String) { this.content = content }
    override fun delete() { content = "" }
}

// Proxy that checks permissions
class DocumentProxy(
    private val document: Document,
    private val currentUserRole: String
) : Document {
    
    override fun read(): String {
        // Everyone can read
        log("READ")
        return document.read()
    }
    
    override fun write(content: String) {
        // Only editors and admins can write
        if (currentUserRole !in listOf("editor", "admin")) {
            throw SecurityException("You don't have permission to write!")
        }
        log("WRITE")
        document.write(content)
    }
    
    override fun delete() {
        // Only admins can delete
        if (currentUserRole != "admin") {
            throw SecurityException("Only admins can delete!")
        }
        log("DELETE")
        document.delete()
    }
    
    private fun log(operation: String) {
        println("[AUDIT] User ($currentUserRole) performed: $operation")
    }
}

// Usage:
val doc = SecureDocument("Secret content")

val viewerDoc = DocumentProxy(doc, "viewer")
viewerDoc.read()      // ✅ Works
viewerDoc.write("x")  // ❌ Throws SecurityException

val adminDoc = DocumentProxy(doc, "admin")
adminDoc.delete()     // ✅ Works
```

---

# BEHAVIORAL PATTERNS
*"How do objects communicate?"*

---

## 7. Strategy Pattern

### What is it?
Defines a **family of algorithms**, encapsulates each one, and makes them **interchangeable** at runtime.

### Real-World Analogy
Think of **getting to the airport**:
- Strategy 1: Drive yourself (cheapest, need parking)
- Strategy 2: Take Uber (convenient, expensive)
- Strategy 3: Take the metro (cheapest, takes longest)

You pick the strategy based on your situation (time, money, luggage).

### When to Use?
- ✅ Multiple ways to do the same thing
- ✅ Algorithms need to be swapped at runtime
- ✅ Avoid long if-else or switch statements
- ✅ Payment methods, sorting algorithms, compression

### Implementation

```kotlin
// ==========================================
// SCENARIO: E-commerce Pricing Strategies
// Different discounts for different customer types
// ==========================================

// Strategy interface
interface PricingStrategy {
    fun calculatePrice(basePrice: Double): Double
    fun description(): String
}

// Concrete strategies
class RegularPricing : PricingStrategy {
    override fun calculatePrice(basePrice: Double) = basePrice
    override fun description() = "Regular price (no discount)"
}

class MemberPricing : PricingStrategy {
    override fun calculatePrice(basePrice: Double) = basePrice * 0.9  // 10% off
    override fun description() = "Member discount (10% off)"
}

class PremiumPricing : PricingStrategy {
    override fun calculatePrice(basePrice: Double) = basePrice * 0.8  // 20% off
    override fun description() = "Premium member discount (20% off)"
}

class HolidaySalePricing(private val discountPercent: Int) : PricingStrategy {
    override fun calculatePrice(basePrice: Double) = 
        basePrice * (1 - discountPercent / 100.0)
    override fun description() = "Holiday Sale ($discountPercent% off)"
}

// Context - uses the strategy
class ShoppingCart {
    private val items = mutableListOf<Pair<String, Double>>()
    private var pricingStrategy: PricingStrategy = RegularPricing()
    
    fun addItem(name: String, price: Double) {
        items.add(name to price)
    }
    
    // Strategy can be changed at runtime!
    fun setPricingStrategy(strategy: PricingStrategy) {
        pricingStrategy = strategy
        println("Switched to: ${strategy.description()}")
    }
    
    fun checkout(): Double {
        val baseTotal = items.sumOf { it.second }
        val finalPrice = pricingStrategy.calculatePrice(baseTotal)
        
        println("\n--- Checkout ---")
        items.forEach { println("${it.first}: $${it.second}") }
        println("Base total: $$baseTotal")
        println("${pricingStrategy.description()}")
        println("Final price: $${"%.2f".format(finalPrice)}")
        
        return finalPrice
    }
}

// Usage:
fun main() {
    val cart = ShoppingCart()
    cart.addItem("Laptop", 1000.0)
    cart.addItem("Mouse", 50.0)
    
    // Regular customer
    cart.setPricingStrategy(RegularPricing())
    cart.checkout()  // $1050.00
    
    // Customer logs in - they're a premium member!
    cart.setPricingStrategy(PremiumPricing())
    cart.checkout()  // $840.00
    
    // Black Friday sale activated!
    cart.setPricingStrategy(HolidaySalePricing(30))
    cart.checkout()  // $735.00
}
```

```kotlin
// ==========================================
// KOTLIN IDIOMATIC: Strategy with Lambda
// Simpler when strategies are single functions
// ==========================================

class Sorter<T> {
    fun sort(items: MutableList<T>, strategy: (T, T) -> Int): List<T> {
        return items.sortedWith { a, b -> strategy(a, b) }
    }
}

// Usage with lambdas:
val numbers = mutableListOf(5, 2, 8, 1, 9)
val sorter = Sorter<Int>()

// Strategy 1: Ascending
val ascending = sorter.sort(numbers) { a, b -> a - b }
println(ascending)  // [1, 2, 5, 8, 9]

// Strategy 2: Descending
val descending = sorter.sort(numbers) { a, b -> b - a }
println(descending)  // [9, 8, 5, 2, 1]

// Strategy 3: By digit sum
val byDigitSum = sorter.sort(numbers) { a, b -> 
    a.toString().sumOf { it.digitToInt() } - b.toString().sumOf { it.digitToInt() }
}
```

---

## 8. Observer Pattern

### What is it?
Defines a **one-to-many relationship** where when one object changes, all its dependents are **notified automatically**.

### Real-World Analogy
Think of **YouTube subscriptions**:
- You subscribe to a channel (become an observer)
- When creator uploads a video (subject changes)
- All subscribers get notified
- You can unsubscribe anytime

### When to Use?
- ✅ Event handling systems
- ✅ UI updates when data changes
- ✅ Notifications, newsletters
- ✅ Stock price updates to multiple displays
- ✅ MVC/MVVM architecture

### Implementation

```kotlin
// ==========================================
// SCENARIO: Stock Price Monitoring System
// Multiple displays update when stock price changes
// ==========================================

// Observer interface - what observers must implement
interface StockObserver {
    fun update(stockSymbol: String, price: Double)
}

// Subject - the thing being observed
class StockMarket {
    private val observers = mutableListOf<StockObserver>()
    private val stockPrices = mutableMapOf<String, Double>()
    
    fun subscribe(observer: StockObserver) {
        observers.add(observer)
        println("New subscriber added. Total: ${observers.size}")
    }
    
    fun unsubscribe(observer: StockObserver) {
        observers.remove(observer)
        println("Subscriber removed. Total: ${observers.size}")
    }
    
    // When price changes, notify all observers
    fun updatePrice(symbol: String, price: Double) {
        stockPrices[symbol] = price
        notifyObservers(symbol, price)
    }
    
    private fun notifyObservers(symbol: String, price: Double) {
        observers.forEach { it.update(symbol, price) }
    }
}

// Concrete observers
class PriceDisplay(private val name: String) : StockObserver {
    override fun update(stockSymbol: String, price: Double) {
        println("[$name] $stockSymbol is now $${"%.2f".format(price)}")
    }
}

class PriceAlert(
    private val targetSymbol: String,
    private val targetPrice: Double
) : StockObserver {
    override fun update(stockSymbol: String, price: Double) {
        if (stockSymbol == targetSymbol && price <= targetPrice) {
            println("🚨 ALERT: $stockSymbol dropped to $$price! BUY NOW!")
        }
    }
}

class TradingBot : StockObserver {
    override fun update(stockSymbol: String, price: Double) {
        println("🤖 Bot analyzing $stockSymbol at $$price...")
        // Complex trading logic here
    }
}

// Usage:
fun main() {
    val market = StockMarket()
    
    // Create observers
    val mainDisplay = PriceDisplay("Main Screen")
    val mobileApp = PriceDisplay("Mobile App")
    val buyAlert = PriceAlert("AAPL", 150.0)
    val bot = TradingBot()
    
    // Subscribe
    market.subscribe(mainDisplay)
    market.subscribe(mobileApp)
    market.subscribe(buyAlert)
    market.subscribe(bot)
    
    // Price changes - all observers notified!
    println("\n--- Price Update 1 ---")
    market.updatePrice("AAPL", 155.0)
    
    println("\n--- Price Update 2 ---")
    market.updatePrice("AAPL", 148.0)  // Triggers alert!
    
    // Unsubscribe mobile app
    market.unsubscribe(mobileApp)
    
    println("\n--- Price Update 3 ---")
    market.updatePrice("GOOGL", 2800.0)  // Mobile won't get this
}
```

```kotlin
// ==========================================
// KOTLIN IDIOMATIC: Using Lambda Observers
// ==========================================

class EventEmitter<T> {
    private val listeners = mutableListOf<(T) -> Unit>()
    
    fun on(listener: (T) -> Unit) {
        listeners.add(listener)
    }
    
    fun off(listener: (T) -> Unit) {
        listeners.remove(listener)
    }
    
    fun emit(event: T) {
        listeners.forEach { it(event) }
    }
}

// Usage:
data class UserEvent(val userId: String, val action: String)

val userEvents = EventEmitter<UserEvent>()

// Add listeners
userEvents.on { event -> 
    println("Analytics: ${event.userId} did ${event.action}")
}

userEvents.on { event ->
    if (event.action == "purchase") {
        println("Send thank you email to ${event.userId}")
    }
}

// Emit events
userEvents.emit(UserEvent("user123", "login"))
userEvents.emit(UserEvent("user123", "purchase"))
```

---

## 9. Command Pattern

### What is it?
Encapsulates a **request as an object**, letting you parameterize, queue, log, and undo operations.

### Real-World Analogy
Think of a **restaurant order**:
- You tell the waiter what you want (command)
- Waiter writes it on a slip (command object)
- Slip goes to kitchen queue (queued)
- Chef executes the order (execute)
- If wrong, can be cancelled/redone (undo)

### When to Use?
- ✅ Undo/Redo functionality
- ✅ Transaction systems
- ✅ Task queues
- ✅ Macro recording
- ✅ Remote procedure calls

### Implementation

```kotlin
// ==========================================
// SCENARIO: Text Editor with Undo/Redo
// ==========================================

// Command interface
interface Command {
    fun execute()
    fun undo()
    fun description(): String
}

// Receiver - the thing being operated on
class TextDocument {
    private val content = StringBuilder()
    
    fun getText() = content.toString()
    
    fun insert(position: Int, text: String) {
        content.insert(position, text)
    }
    
    fun delete(position: Int, length: Int): String {
        val deleted = content.substring(position, position + length)
        content.delete(position, position + length)
        return deleted
    }
}

// Concrete commands
class InsertCommand(
    private val document: TextDocument,
    private val position: Int,
    private val text: String
) : Command {
    override fun execute() {
        document.insert(position, text)
    }
    
    override fun undo() {
        document.delete(position, text.length)
    }
    
    override fun description() = "Insert '$text' at position $position"
}

class DeleteCommand(
    private val document: TextDocument,
    private val position: Int,
    private val length: Int
) : Command {
    private var deletedText = ""
    
    override fun execute() {
        deletedText = document.delete(position, length)
    }
    
    override fun undo() {
        document.insert(position, deletedText)
    }
    
    override fun description() = "Delete $length chars at position $position"
}

// Invoker - manages command history
class TextEditor {
    private val document = TextDocument()
    private val history = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()
    
    fun executeCommand(command: Command) {
        command.execute()
        history.add(command)
        redoStack.clear()  // Clear redo stack on new command
        println("Executed: ${command.description()}")
        println("Document: '${document.getText()}'")
    }
    
    fun undo() {
        if (history.isEmpty()) {
            println("Nothing to undo!")
            return
        }
        val command = history.removeLast()
        command.undo()
        redoStack.add(command)
        println("Undone: ${command.description()}")
        println("Document: '${document.getText()}'")
    }
    
    fun redo() {
        if (redoStack.isEmpty()) {
            println("Nothing to redo!")
            return
        }
        val command = redoStack.removeLast()
        command.execute()
        history.add(command)
        println("Redone: ${command.description()}")
        println("Document: '${document.getText()}'")
    }
    
    fun type(text: String) {
        executeCommand(InsertCommand(document, document.getText().length, text))
    }
    
    fun deleteLastChars(count: Int) {
        val pos = document.getText().length - count
        if (pos >= 0) {
            executeCommand(DeleteCommand(document, pos, count))
        }
    }
}

// Usage:
fun main() {
    val editor = TextEditor()
    
    editor.type("Hello")
    editor.type(" World")
    editor.type("!")
    // Document: 'Hello World!'
    
    editor.undo()  // Remove '!'
    editor.undo()  // Remove ' World'
    // Document: 'Hello'
    
    editor.redo()  // Add back ' World'
    // Document: 'Hello World'
    
    editor.type("!!!")
    // Document: 'Hello World!!!'
}
```

---

## 10. State Pattern

### What is it?
Allows an object to **change its behavior** when its internal state changes. Object appears to change its class.

### Real-World Analogy
Think of a **vending machine**:
- **No money state**: Only accepts money
- **Has money state**: Can select product or get refund
- **Dispensing state**: Gives product, then returns to no money
- Same machine, different behavior based on state

### When to Use?
- ✅ Object behavior depends on state
- ✅ State transitions are complex
- ✅ Avoid large switch/if-else on state
- ✅ Order processing, game states, UI workflows

### Implementation

```kotlin
// ==========================================
// SCENARIO: Order Processing System
// Order behaves differently based on its status
// ==========================================

// State interface
interface OrderState {
    fun next(order: Order)
    fun cancel(order: Order)
    fun getStatus(): String
}

// Context - the order itself
class Order(val orderId: String) {
    var state: OrderState = PendingState()
        internal set
    
    fun nextStep() = state.next(this)
    fun cancel() = state.cancel(this)
    fun getStatus() = state.getStatus()
}

// Concrete states
class PendingState : OrderState {
    override fun next(order: Order) {
        println("Order ${order.orderId}: Payment confirmed, moving to CONFIRMED")
        order.state = ConfirmedState()
    }
    
    override fun cancel(order: Order) {
        println("Order ${order.orderId}: Cancelled while pending")
        order.state = CancelledState()
    }
    
    override fun getStatus() = "PENDING - Awaiting payment"
}

class ConfirmedState : OrderState {
    override fun next(order: Order) {
        println("Order ${order.orderId}: Shipped!")
        order.state = ShippedState()
    }
    
    override fun cancel(order: Order) {
        println("Order ${order.orderId}: Cancellation requested, processing refund...")
        order.state = CancelledState()
    }
    
    override fun getStatus() = "CONFIRMED - Preparing for shipment"
}

class ShippedState : OrderState {
    override fun next(order: Order) {
        println("Order ${order.orderId}: Delivered successfully!")
        order.state = DeliveredState()
    }
    
    override fun cancel(order: Order) {
        println("Order ${order.orderId}: Cannot cancel - already shipped!")
    }
    
    override fun getStatus() = "SHIPPED - In transit"
}

class DeliveredState : OrderState {
    override fun next(order: Order) {
        println("Order ${order.orderId}: Already delivered, no next step")
    }
    
    override fun cancel(order: Order) {
        println("Order ${order.orderId}: Cannot cancel - already delivered. Please initiate return.")
    }
    
    override fun getStatus() = "DELIVERED - Order complete"
}

class CancelledState : OrderState {
    override fun next(order: Order) {
        println("Order ${order.orderId}: Cannot proceed - order was cancelled")
    }
    
    override fun cancel(order: Order) {
        println("Order ${order.orderId}: Already cancelled")
    }
    
    override fun getStatus() = "CANCELLED"
}

// Usage:
fun main() {
    val order = Order("ORD-12345")
    
    println("Status: ${order.getStatus()}")  // PENDING
    
    order.nextStep()  // PENDING → CONFIRMED
    println("Status: ${order.getStatus()}")
    
    order.nextStep()  // CONFIRMED → SHIPPED
    println("Status: ${order.getStatus()}")
    
    order.cancel()    // Cannot cancel - already shipped!
    
    order.nextStep()  // SHIPPED → DELIVERED
    println("Status: ${order.getStatus()}")
}
```

---

## Quick Reference Summary

| Pattern | One-Line Description | Key Benefit |
|---------|---------------------|-------------|
| **Singleton** | Only one instance exists | Global access, shared state |
| **Factory** | Creates objects without specifying class | Decouples creation from usage |
| **Builder** | Builds complex objects step-by-step | Readable, flexible construction |
| **Decorator** | Adds behavior by wrapping | Add features without inheritance |
| **Adapter** | Converts one interface to another | Integrates incompatible code |
| **Proxy** | Controls access to an object | Lazy loading, security, caching |
| **Strategy** | Swappable algorithms | Runtime flexibility |
| **Observer** | Notifies dependents of changes | Loose coupling, event handling |
| **Command** | Encapsulates request as object | Undo/redo, queuing |
| **State** | Behavior changes with state | Clean state machine code |

---

## Interview Tips

1. **Always explain WHY** before showing code
2. **Use real-world analogies** - shows understanding
3. **Discuss trade-offs** - when NOT to use a pattern
4. **Start simple** - don't over-engineer
5. **Know Kotlin shortcuts** - object, by lazy, apply/also
