# Cost Explorer - LLD

## Problem Statement
Design a CostExplorer for a payment system that calculates the total cost a customer has to pay in a unit year. The system should be able to provide monthly and yearly cost estimates at any day of the year.

---

## Code Flow Walkthrough

### `getBillForMonth(yearMonth)` - Monthly Bill Calculation

```
CALL: explorer.getBillForMonth(YearMonth.of(2024, JANUARY))

STEP 1: Find Active Subscriptions
├── FOR each subscription in subscriptions:
│   ├── isActiveInMonth = subscription.startDate <= Jan31 AND
│   │                     (subscription.endDate == null OR endDate >= Jan1)
│   ├── 
│   ├── Subscription A: Jira, started Dec15, no end → ACTIVE
│   ├── Subscription B: Confluence, started Feb1 → NOT ACTIVE (future)
│   └── Subscription C: Bitbucket, ended Dec31 → NOT ACTIVE (past)
├── 
└── activeSubscriptions = [Subscription A]

STEP 2: Calculate Cost for Each Subscription
├── FOR each active subscription:
│   ├── 
│   ├── // Get pricing plan
│   ├── plan = pricingPlans[(Jira, STANDARD)]
│   ├── basePrice = plan.pricePerUser * subscription.userCount
│   ├── Example: $10/user × 50 users = $500
│   ├── 
│   ├── // Apply proration if partial month
│   ├── IF subscription.startDate > Jan1:
│   │   ├── daysInMonth = 31
│   │   ├── activeDays = 31 - 14 = 17 (started Jan15)
│   │   ├── prorationFactor = 17/31 = 0.548
│   │   └── proratedAmount = $500 × 0.548 = $274
│   ├── 
│   ├── // Add additional charges
│   ├── storageCharge = calculateStorageOverage(subscription)
│   ├── apiCharge = calculateAPIOverage(subscription)
│   └── lineItem = LineItem(
│           product = Jira,
│           baseAmount = $274,
│           storageCharge = $50,
│           apiCharge = $0,
│           total = $324
│       )

STEP 3: Apply Discounts
├── FOR each discount in discounts:
│   ├── IF discount.isApplicable(subscription, month):
│   │   ├── // Percentage discount
│   │   ├── IF discount.type == PERCENTAGE:
│   │   │   └── amount = total × (discount.percent / 100)
│   │   ├── // Fixed discount
│   │   ├── ELSE IF discount.type == FIXED:
│   │   │   └── amount = discount.amount
│   │   └── Apply to line item
│   └── 
├── Example: 10% annual commitment discount
└── discountAmount = $324 × 0.10 = $32.40

STEP 4: Generate Bill
└── Bill(
        yearMonth = Jan2024,
        lineItems = [LineItem(Jira, $324)],
        subtotal = $324,
        discounts = [Discount("Annual", -$32.40)],
        total = $291.60
    )
```

### Proration Calculation

```
SCENARIO: Subscription starts mid-month

Subscription: Confluence PREMIUM
├── Start Date: January 15, 2024
├── User Count: 100
├── Price: $20/user/month = $2000/month

PRORATION FOR JANUARY:
├── Total days in January: 31
├── Active days: Jan 15-31 = 17 days
├── Proration factor: 17/31 = 0.5484
├── Prorated amount: $2000 × 0.5484 = $1096.77

PRORATION FOR SUBSCRIPTION END:
├── End Date: March 20, 2024
├── Active days in March: 1-20 = 20 days
├── Proration factor: 20/31 = 0.6452
├── Prorated amount: $2000 × 0.6452 = $1290.32

FULL MONTH (FEBRUARY):
├── No proration needed
├── Full amount: $2000

TIMELINE:
├── Jan: $1096.77 (prorated start)
├── Feb: $2000.00 (full month)
├── Mar: $1290.32 (prorated end)
└── Total: $4387.09
```

### `getYearlyEstimate()` - Annual Projection

```
CALL: explorer.getYearlyEstimate(fromMonth=Jan2024)

STEP 1: Calculate Known Months
├── FOR month in [Jan, Feb, Mar] (past/current):
│   ├── bill = getBillForMonth(month)
│   └── knownCosts.add(bill.total)
├── 
├── Jan: $1500
├── Feb: $1650
├── Mar: $1600
└── knownTotal = $4750

STEP 2: Project Future Months
├── FOR month in [Apr, May, ..., Dec] (future):
│   ├── 
│   ├── // Use current subscriptions
│   ├── activeSubscriptions = getCurrentSubscriptions()
│   ├── 
│   ├── // Project cost (no proration for full months)
│   ├── monthlyProjection = SUM(subscription.monthlyRate)
│   ├── 
│   ├── // Apply known discounts
│   ├── discountedProjection = applyDiscounts(monthlyProjection)
│   └── 
├── 
├── Projected monthly: $1600
└── projectedTotal = 9 months × $1600 = $14,400

STEP 3: Account for Changes
├── // Pending subscription changes
├── IF pendingUpgrade(startDate=Jun1):
│   ├── Months Jun-Dec affected
│   └── Adjust projection: +$200/month × 7 = +$1400
├── 
├── IF pendingDowngrade(startDate=Sep1):
│   └── Adjust: -$100/month × 4 = -$400

STEP 4: Generate Estimate
└── YearlyEstimate(
        knownCosts = $4,750,
        projectedCosts = $14,400,
        adjustments = +$1,000,
        totalEstimate = $20,150,
        confidence = "Medium" // due to projections
    )
```

### Discount Application Logic

```
DISCOUNT TYPES:

1. PERCENTAGE DISCOUNT:
├── Input: 20% off Jira
├── baseAmount = $500
├── discountAmount = $500 × 0.20 = $100
└── finalAmount = $400

2. FIXED AMOUNT DISCOUNT:
├── Input: $50 off total
├── baseAmount = $500
├── discountAmount = $50
└── finalAmount = $450

3. TIERED DISCOUNT:
├── Input: 
│   ├── 1-10 users: 0% off
│   ├── 11-50 users: 10% off
│   └── 51+ users: 20% off
├── 75 users at $10/user = $750 base
├── Tier: 51+ → 20% off
├── discountAmount = $750 × 0.20 = $150
└── finalAmount = $600

4. COMMITMENT DISCOUNT:
├── Input: 15% off for annual commitment
├── Applies if: subscription.commitment == ANNUAL
├── baseAmount = $500/month × 12 = $6000/year
├── discountAmount = $6000 × 0.15 = $900
└── Annual cost = $5100

DISCOUNT STACKING:
├── Volume discount: -10%
├── Annual commitment: -15%
├── Promo code: -$50
├── 
├── baseAmount = $1000
├── After volume: $1000 × 0.90 = $900
├── After annual: $900 × 0.85 = $765
├── After promo: $765 - $50 = $715
└── Note: percentage discounts compound, fixed applied last
```

---

## Requirements

### Functional Requirements
1. Generate monthly bills (including future months for the unit year)
2. Calculate yearly cost estimates
3. Support different subscription tiers/plans
4. Handle prorated charges
5. Apply discounts and promotions
6. Support multiple products (Jira, Confluence, etc.)

### Non-Functional Requirements
1. Thread-safe calculations
2. Accurate prorating
3. Extensible pricing models

---

## Class Diagram

```
┌─────────────────────────────────────────┐
│           Subscription                   │
├─────────────────────────────────────────┤
│ - customerId: String                    │
│ - productId: String                     │
│ - tier: SubscriptionTier                │
│ - startDate: LocalDate                  │
│ - billingCycle: BillingCycle            │
│ - userCount: Int                        │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│          PricingPlan                     │
├─────────────────────────────────────────┤
│ - productId: String                     │
│ - tier: SubscriptionTier                │
│ - pricePerUser: Double                  │
│ - flatFee: Double                       │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│          CostExplorer                    │
├─────────────────────────────────────────┤
│ - subscriptions: List<Subscription>     │
│ - pricingPlans: Map                     │
│ - discounts: List<Discount>             │
├─────────────────────────────────────────┤
│ + getMonthlyReport(year): List<Bill>    │
│ + getYearlyCost(year): Double           │
│ + getProjectedCost(date): Double        │
│ + getBillForMonth(year, month): Bill    │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│              Bill                        │
├─────────────────────────────────────────┤
│ - month: YearMonth                      │
│ - lineItems: List<LineItem>             │
│ - subtotal: Double                      │
│ - discounts: Double                     │
│ - total: Double                         │
└─────────────────────────────────────────┘
```

---

## Kotlin Implementation

### Core Data Classes

```kotlin
import java.time.*
import java.time.temporal.ChronoUnit

// ==================== Enums ====================

enum class SubscriptionTier(val displayName: String) {
    FREE("Free"),
    STANDARD("Standard"),
    PREMIUM("Premium"),
    ENTERPRISE("Enterprise")
}

enum class BillingCycle(val months: Int) {
    MONTHLY(1),
    QUARTERLY(3),
    ANNUAL(12)
}

enum class Product(val displayName: String) {
    JIRA("Jira Software"),
    CONFLUENCE("Confluence"),
    BITBUCKET("Bitbucket"),
    TRELLO("Trello")
}

// ==================== Subscription ====================

data class Subscription(
    val id: String,
    val customerId: String,
    val product: Product,
    val tier: SubscriptionTier,
    val startDate: LocalDate,
    val endDate: LocalDate? = null, // null = ongoing
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val userCount: Int = 1,
    val additionalStorage: Int = 0 // in GB
)

// ==================== Pricing Plan ====================

data class PricingPlan(
    val product: Product,
    val tier: SubscriptionTier,
    val pricePerUserPerMonth: Double,
    val flatFeePerMonth: Double = 0.0,
    val storageIncludedGB: Int = 0,
    val additionalStoragePricePerGB: Double = 0.0
) {
    fun getMonthlyPrice(userCount: Int, additionalStorageGB: Int = 0): Double {
        val userCost = pricePerUserPerMonth * userCount
        val storageCost = if (additionalStorageGB > 0) {
            additionalStorageGB * additionalStoragePricePerGB
        } else 0.0
        return userCost + flatFeePerMonth + storageCost
    }
}

// ==================== Line Item ====================

data class LineItem(
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double,
    val product: Product? = null,
    val isProrated: Boolean = false
)

// ==================== Discount ====================

data class Discount(
    val code: String,
    val description: String,
    val percentOff: Double = 0.0,
    val amountOff: Double = 0.0,
    val validFrom: LocalDate,
    val validUntil: LocalDate,
    val applicableProducts: Set<Product> = emptySet() // empty = all products
) {
    fun isValidFor(date: LocalDate, product: Product): Boolean {
        val dateValid = !date.isBefore(validFrom) && !date.isAfter(validUntil)
        val productValid = applicableProducts.isEmpty() || product in applicableProducts
        return dateValid && productValid
    }
    
    fun calculateDiscount(amount: Double): Double {
        return if (percentOff > 0) {
            amount * (percentOff / 100)
        } else {
            minOf(amountOff, amount)
        }
    }
}

// ==================== Bill ====================

data class Bill(
    val month: YearMonth,
    val customerId: String,
    val lineItems: List<LineItem>,
    val subtotal: Double,
    val discounts: List<Pair<Discount, Double>>,
    val totalDiscount: Double,
    val total: Double,
    val isPaid: Boolean = false,
    val isProjected: Boolean = true // future months are projected
)
```

### Cost Explorer Implementation

```kotlin
// ==================== Cost Explorer ====================

/**
 * Calculates and projects costs for a customer's subscriptions.
 * 
 * === Features ===
 * - Monthly bill generation (actual and projected)
 * - Yearly cost estimates
 * - Proration for mid-month subscription changes
 * - Discount application
 * - Multi-product support (Jira, Confluence, etc.)
 * 
 * === Billing Flow ===
 * For each month:
 * 1. Find active subscriptions
 * 2. Calculate base cost (users × price)
 * 3. Apply prorating if subscription started/ended mid-month
 * 4. Add additional charges (storage, etc.)
 * 5. Apply applicable discounts
 * 6. Generate bill with line items
 * 
 * === Proration Formula ===
 * proratedAmount = fullMonthPrice × (activeDays / daysInMonth)
 * 
 * Example: $100/month subscription starting Jan 15:
 * activeDays = 17 (Jan 15-31)
 * proratedAmount = $100 × (17/31) = $54.84
 * 
 * === Time Complexity ===
 * - getMonthlyReport: O(12 × s × d) for full year
 *   where s = subscriptions, d = discounts
 * - getBillForMonth: O(s × d)
 */
class CostExplorer(
    private val customerId: String
) {
    private val subscriptions = mutableListOf<Subscription>()
    private val pricingPlans = mutableMapOf<Pair<Product, SubscriptionTier>, PricingPlan>()
    private val discounts = mutableListOf<Discount>()
    
    init {
        // Load default Atlassian-like pricing
        initializeDefaultPricingPlans()
    }
    
    private fun initializeDefaultPricingPlans() {
        // Jira pricing
        addPricingPlan(PricingPlan(Product.JIRA, SubscriptionTier.FREE, 0.0))
        addPricingPlan(PricingPlan(Product.JIRA, SubscriptionTier.STANDARD, 7.75, storageIncludedGB = 250))
        addPricingPlan(PricingPlan(Product.JIRA, SubscriptionTier.PREMIUM, 15.25, storageIncludedGB = 500))
        addPricingPlan(PricingPlan(Product.JIRA, SubscriptionTier.ENTERPRISE, 21.0, flatFeePerMonth = 100.0, storageIncludedGB = 1000))
        
        // Confluence pricing
        addPricingPlan(PricingPlan(Product.CONFLUENCE, SubscriptionTier.FREE, 0.0))
        addPricingPlan(PricingPlan(Product.CONFLUENCE, SubscriptionTier.STANDARD, 5.75, storageIncludedGB = 250))
        addPricingPlan(PricingPlan(Product.CONFLUENCE, SubscriptionTier.PREMIUM, 11.0, storageIncludedGB = 500))
    }
    
    fun addPricingPlan(plan: PricingPlan) {
        pricingPlans[Pair(plan.product, plan.tier)] = plan
    }
    
    fun addSubscription(subscription: Subscription) {
        subscriptions.add(subscription)
    }
    
    fun addDiscount(discount: Discount) {
        discounts.add(discount)
    }
    
    // ==================== Monthly Report ====================
    
    /**
     * Generate monthly bills for each month of the year.
     */
    fun getMonthlyReport(year: Int): List<Bill> {
        val today = LocalDate.now()
        val bills = mutableListOf<Bill>()
        
        for (month in 1..12) {
            val yearMonth = YearMonth.of(year, month)
            val bill = getBillForMonth(yearMonth)
            bills.add(bill)
        }
        
        return bills
    }
    
    /**
     * Get bill for a specific month.
     */
    fun getBillForMonth(yearMonth: YearMonth): Bill {
        val today = LocalDate.now()
        val isProjected = yearMonth.isAfter(YearMonth.from(today))
        
        val lineItems = mutableListOf<LineItem>()
        
        for (subscription in subscriptions) {
            // Check if subscription is active during this month
            if (!isSubscriptionActiveInMonth(subscription, yearMonth)) continue
            
            val plan = pricingPlans[Pair(subscription.product, subscription.tier)] ?: continue
            
            // Calculate prorated amount if subscription starts/ends mid-month
            val (amount, isProrated) = calculateMonthlyAmount(subscription, plan, yearMonth)
            
            if (amount > 0) {
                lineItems.add(LineItem(
                    description = "${subscription.product.displayName} - ${subscription.tier.displayName}",
                    quantity = subscription.userCount,
                    unitPrice = plan.pricePerUserPerMonth,
                    amount = amount,
                    product = subscription.product,
                    isProrated = isProrated
                ))
                
                // Add storage charges if applicable
                if (subscription.additionalStorage > 0 && plan.additionalStoragePricePerGB > 0) {
                    lineItems.add(LineItem(
                        description = "Additional Storage - ${subscription.additionalStorage}GB",
                        quantity = subscription.additionalStorage,
                        unitPrice = plan.additionalStoragePricePerGB,
                        amount = subscription.additionalStorage * plan.additionalStoragePricePerGB,
                        product = subscription.product
                    ))
                }
            }
        }
        
        val subtotal = lineItems.sumOf { it.amount }
        
        // Apply discounts
        val appliedDiscounts = mutableListOf<Pair<Discount, Double>>()
        val monthDate = yearMonth.atDay(1)
        
        for (discount in discounts) {
            val applicableItems = lineItems.filter { item ->
                item.product?.let { discount.isValidFor(monthDate, it) } ?: false
            }
            
            val applicableAmount = applicableItems.sumOf { it.amount }
            if (applicableAmount > 0) {
                val discountAmount = discount.calculateDiscount(applicableAmount)
                appliedDiscounts.add(Pair(discount, discountAmount))
            }
        }
        
        val totalDiscount = appliedDiscounts.sumOf { it.second }
        val total = maxOf(0.0, subtotal - totalDiscount)
        
        return Bill(
            month = yearMonth,
            customerId = customerId,
            lineItems = lineItems,
            subtotal = subtotal,
            discounts = appliedDiscounts,
            totalDiscount = totalDiscount,
            total = total,
            isProjected = isProjected
        )
    }
    
    private fun isSubscriptionActiveInMonth(subscription: Subscription, yearMonth: YearMonth): Boolean {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        
        // Subscription hasn't started yet
        if (subscription.startDate.isAfter(monthEnd)) return false
        
        // Subscription has ended
        subscription.endDate?.let { endDate ->
            if (endDate.isBefore(monthStart)) return false
        }
        
        return true
    }
    
    private fun calculateMonthlyAmount(
        subscription: Subscription,
        plan: PricingPlan,
        yearMonth: YearMonth
    ): Pair<Double, Boolean> {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val daysInMonth = yearMonth.lengthOfMonth()
        
        val effectiveStart = maxOf(subscription.startDate, monthStart)
        val effectiveEnd = minOf(subscription.endDate ?: monthEnd, monthEnd)
        
        if (effectiveStart.isAfter(effectiveEnd)) {
            return Pair(0.0, false)
        }
        
        val activeDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1
        val fullMonthPrice = plan.getMonthlyPrice(subscription.userCount, subscription.additionalStorage)
        
        // Check if full month or prorated
        return if (activeDays < daysInMonth) {
            val proratedAmount = fullMonthPrice * (activeDays.toDouble() / daysInMonth)
            Pair(proratedAmount, true)
        } else {
            Pair(fullMonthPrice, false)
        }
    }
    
    // ==================== Yearly Cost ====================
    
    /**
     * Get total yearly cost estimate.
     */
    fun getYearlyCost(year: Int): Double {
        return getMonthlyReport(year).sumOf { it.total }
    }
    
    /**
     * Get projected cost from a specific date until end of year.
     */
    fun getProjectedCostFromDate(fromDate: LocalDate): Double {
        val year = fromDate.year
        val fromMonth = fromDate.monthValue
        
        var total = 0.0
        
        for (month in fromMonth..12) {
            val bill = getBillForMonth(YearMonth.of(year, month))
            total += bill.total
        }
        
        return total
    }
    
    /**
     * Get cost breakdown by product.
     */
    fun getCostByProduct(year: Int): Map<Product, Double> {
        val breakdown = mutableMapOf<Product, Double>()
        
        for (bill in getMonthlyReport(year)) {
            for (item in bill.lineItems) {
                item.product?.let { product ->
                    breakdown[product] = breakdown.getOrDefault(product, 0.0) + item.amount
                }
            }
        }
        
        return breakdown
    }
    
    // ==================== Report Generation ====================
    
    /**
     * Generate a formatted cost report.
     */
    fun generateReport(year: Int): String {
        val bills = getMonthlyReport(year)
        val yearlyTotal = bills.sumOf { it.total }
        
        val sb = StringBuilder()
        sb.appendLine("╔════════════════════════════════════════════════════════════╗")
        sb.appendLine("║              COST EXPLORER REPORT - $year                   ║")
        sb.appendLine("║              Customer: $customerId                        ║")
        sb.appendLine("╠════════════════════════════════════════════════════════════╣")
        sb.appendLine()
        
        for (bill in bills) {
            val status = if (bill.isProjected) " (Projected)" else " (Actual)"
            sb.appendLine("  ${bill.month.month}$status")
            sb.appendLine("  " + "─".repeat(40))
            
            for (item in bill.lineItems) {
                val prorated = if (item.isProrated) " (prorated)" else ""
                sb.appendLine("    ${item.description}$prorated")
                sb.appendLine("      ${item.quantity} x $${String.format("%.2f", item.unitPrice)} = $${String.format("%.2f", item.amount)}")
            }
            
            if (bill.discounts.isNotEmpty()) {
                sb.appendLine("    ───")
                for ((discount, amount) in bill.discounts) {
                    sb.appendLine("    Discount: ${discount.description} -$${String.format("%.2f", amount)}")
                }
            }
            
            sb.appendLine("    ───")
            sb.appendLine("    Total: $${String.format("%.2f", bill.total)}")
            sb.appendLine()
        }
        
        sb.appendLine("╠════════════════════════════════════════════════════════════╣")
        sb.appendLine("║  YEARLY TOTAL: $${String.format("%.2f", yearlyTotal)}".padEnd(61) + "║")
        sb.appendLine("╚════════════════════════════════════════════════════════════╝")
        
        return sb.toString()
    }
}
```

### Usage Example

```kotlin
fun main() {
    val costExplorer = CostExplorer("CUSTOMER-001")
    
    // Add subscriptions
    costExplorer.addSubscription(Subscription(
        id = "SUB-001",
        customerId = "CUSTOMER-001",
        product = Product.JIRA,
        tier = SubscriptionTier.STANDARD,
        startDate = LocalDate.of(2025, 1, 15), // Started mid-January
        userCount = 25
    ))
    
    costExplorer.addSubscription(Subscription(
        id = "SUB-002",
        customerId = "CUSTOMER-001",
        product = Product.CONFLUENCE,
        tier = SubscriptionTier.STANDARD,
        startDate = LocalDate.of(2025, 1, 1),
        userCount = 20
    ))
    
    // Add a discount
    costExplorer.addDiscount(Discount(
        code = "ANNUAL10",
        description = "10% Annual Discount",
        percentOff = 10.0,
        validFrom = LocalDate.of(2025, 1, 1),
        validUntil = LocalDate.of(2025, 12, 31)
    ))
    
    // Generate report
    println(costExplorer.generateReport(2025))
    
    // Get specific month bill
    val janBill = costExplorer.getBillForMonth(YearMonth.of(2025, 1))
    println("\nJanuary 2025 Details:")
    println("  Subtotal: $${String.format("%.2f", janBill.subtotal)}")
    println("  Discounts: $${String.format("%.2f", janBill.totalDiscount)}")
    println("  Total: $${String.format("%.2f", janBill.total)}")
    
    // Cost by product
    println("\nCost by Product:")
    costExplorer.getCostByProduct(2025).forEach { (product, cost) ->
        println("  ${product.displayName}: $${String.format("%.2f", cost)}")
    }
    
    // Yearly total
    println("\nYearly Total: $${String.format("%.2f", costExplorer.getYearlyCost(2025))}")
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | Pricing calculation | Different pricing models |
| **Builder** | Bill construction | Complex bill generation |
| **Factory** | Default pricing plans | Create standard plans |
| **Composite** | Multiple subscriptions | Aggregate costs |

---

## Interview Discussion Points

### Q: How to handle currency conversion?
**A:** Add `CurrencyConverter` service and store prices in base currency.

### Q: How to handle plan changes mid-month?
**A:** 
- Prorate the old plan until change date
- Prorate the new plan from change date
- Track plan history per subscription

### Q: How to scale for millions of customers?
**A:**
- Cache calculated bills
- Pre-compute monthly bills on schedule
- Use database for storage vs in-memory

---

## Complexity Analysis

| Operation | Time Complexity |
|-----------|----------------|
| Get monthly bill | O(s × d) where s = subscriptions, d = discounts |
| Get yearly report | O(12 × s × d) |
| Add subscription | O(1) |

**Space Complexity:** O(s + p + d) where s = subscriptions, p = pricing plans, d = discounts

