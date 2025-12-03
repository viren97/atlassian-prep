# Parking Lot System - LLD

## Problem Statement
Design a parking lot system that can manage multiple floors, different vehicle types, and handle parking/unparking operations.

---

## Requirements

### Functional Requirements
1. Multiple floors with multiple spots per floor
2. Different spot sizes (Compact, Regular, Large)
3. Different vehicle types (Motorcycle, Car, Truck)
4. Park and unpark vehicles
5. Find available spots
6. Calculate parking fees
7. Display available spots per floor

### Non-Functional Requirements
1. Thread-safe for concurrent operations
2. Efficient spot allocation
3. Extensible for new vehicle types

---

## Class Diagram

```
┌─────────────────────────────────────┐
│           ParkingLot                │
├─────────────────────────────────────┤
│ - floors: List<ParkingFloor>        │
│ - entryPanels: List<EntryPanel>     │
│ - exitPanels: List<ExitPanel>       │
├─────────────────────────────────────┤
│ + parkVehicle(vehicle): Ticket?     │
│ + unparkVehicle(ticket): Receipt    │
│ + getAvailableSpots(): Map          │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│          ParkingFloor               │
├─────────────────────────────────────┤
│ - floorNumber: Int                  │
│ - spots: List<ParkingSpot>          │
├─────────────────────────────────────┤
│ + findAvailableSpot(type): Spot?    │
│ + getAvailableCount(): Map          │
└─────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│          ParkingSpot                │
├─────────────────────────────────────┤
│ - id: String                        │
│ - spotType: SpotType                │
│ - vehicle: Vehicle?                 │
│ - isAvailable: Boolean              │
├─────────────────────────────────────┤
│ + park(vehicle): Boolean            │
│ + unpark(): Vehicle?                │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│     <<abstract>> Vehicle            │
├─────────────────────────────────────┤
│ - licensePlate: String              │
│ - vehicleType: VehicleType          │
└─────────────────────────────────────┘
         △
         │
    ┌────┼────┬────────┐
    │    │    │        │
   Car  Bike  Truck  Electric
```

---

## Kotlin Implementation

### Enums and Data Classes

```kotlin
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// ==================== Enums ====================

enum class VehicleType {
    MOTORCYCLE, CAR, TRUCK, ELECTRIC_CAR
}

enum class SpotType(val size: Int) {
    COMPACT(1),      // For motorcycles
    REGULAR(2),      // For cars
    LARGE(3),        // For trucks
    ELECTRIC(2)      // For electric cars with charging
}

// ==================== Vehicle Classes ====================

abstract class Vehicle(
    val licensePlate: String,
    val vehicleType: VehicleType
) {
    abstract fun canFitIn(spotType: SpotType): Boolean
}

class Motorcycle(licensePlate: String) : Vehicle(licensePlate, VehicleType.MOTORCYCLE) {
    override fun canFitIn(spotType: SpotType): Boolean = true // Can fit anywhere
}

class Car(licensePlate: String) : Vehicle(licensePlate, VehicleType.CAR) {
    override fun canFitIn(spotType: SpotType): Boolean = 
        spotType in listOf(SpotType.REGULAR, SpotType.LARGE)
}

class Truck(licensePlate: String) : Vehicle(licensePlate, VehicleType.TRUCK) {
    override fun canFitIn(spotType: SpotType): Boolean = spotType == SpotType.LARGE
}

class ElectricCar(licensePlate: String) : Vehicle(licensePlate, VehicleType.ELECTRIC_CAR) {
    override fun canFitIn(spotType: SpotType): Boolean = 
        spotType in listOf(SpotType.REGULAR, SpotType.LARGE, SpotType.ELECTRIC)
}

// ==================== Vehicle Factory ====================

object VehicleFactory {
    fun create(type: VehicleType, licensePlate: String): Vehicle {
        return when (type) {
            VehicleType.MOTORCYCLE -> Motorcycle(licensePlate)
            VehicleType.CAR -> Car(licensePlate)
            VehicleType.TRUCK -> Truck(licensePlate)
            VehicleType.ELECTRIC_CAR -> ElectricCar(licensePlate)
        }
    }
}
```

### Parking Spot

```kotlin
// ==================== Parking Spot ====================

class ParkingSpot(
    val id: String,
    val spotType: SpotType,
    val floorNumber: Int
) {
    private var vehicle: Vehicle? = null
    private val lock = ReentrantReadWriteLock()
    
    val isAvailable: Boolean
        get() = lock.read { vehicle == null }
    
    fun park(vehicle: Vehicle): Boolean {
        if (!vehicle.canFitIn(spotType)) return false
        
        return lock.write {
            if (this.vehicle != null) return@write false
            this.vehicle = vehicle
            true
        }
    }
    
    fun unpark(): Vehicle? {
        return lock.write {
            val parked = vehicle
            vehicle = null
            parked
        }
    }
    
    fun getVehicle(): Vehicle? = lock.read { vehicle }
    
    override fun toString(): String = "Spot($id, $spotType, available=$isAvailable)"
}
```

### Parking Floor

```kotlin
// ==================== Parking Floor ====================

class ParkingFloor(
    val floorNumber: Int,
    compactSpots: Int,
    regularSpots: Int,
    largeSpots: Int,
    electricSpots: Int = 0
) {
    private val spots = mutableListOf<ParkingSpot>()
    private val spotCounter = AtomicInteger(0)
    
    init {
        repeat(compactSpots) { addSpot(SpotType.COMPACT) }
        repeat(regularSpots) { addSpot(SpotType.REGULAR) }
        repeat(largeSpots) { addSpot(SpotType.LARGE) }
        repeat(electricSpots) { addSpot(SpotType.ELECTRIC) }
    }
    
    private fun addSpot(type: SpotType) {
        val id = "F${floorNumber}-${type.name[0]}${spotCounter.incrementAndGet()}"
        spots.add(ParkingSpot(id, type, floorNumber))
    }
    
    fun findAvailableSpot(vehicle: Vehicle): ParkingSpot? {
        // Priority: Find the smallest suitable spot
        val suitableTypes = SpotType.values()
            .filter { vehicle.canFitIn(it) }
            .sortedBy { it.size }
        
        for (spotType in suitableTypes) {
            val spot = spots
                .filter { it.spotType == spotType && it.isAvailable }
                .firstOrNull()
            if (spot != null) return spot
        }
        return null
    }
    
    fun getAvailableSpotsCount(): Map<SpotType, Int> {
        return spots
            .filter { it.isAvailable }
            .groupBy { it.spotType }
            .mapValues { it.value.size }
    }
    
    fun getTotalSpotsCount(): Map<SpotType, Int> {
        return spots
            .groupBy { it.spotType }
            .mapValues { it.value.size }
    }
    
    fun getAllSpots(): List<ParkingSpot> = spots.toList()
}
```

### Ticket and Receipt

```kotlin
// ==================== Ticket ====================

data class ParkingTicket(
    val ticketId: String = UUID.randomUUID().toString(),
    val vehicleLicensePlate: String,
    val spotId: String,
    val floorNumber: Int,
    val entryTime: Instant = Instant.now()
)

// ==================== Receipt ====================

data class ParkingReceipt(
    val ticket: ParkingTicket,
    val exitTime: Instant = Instant.now(),
    val duration: Duration,
    val totalFee: Double
)
```

### Pricing Strategy

```kotlin
// ==================== Pricing Strategy ====================

interface PricingStrategy {
    fun calculateFee(vehicleType: VehicleType, duration: Duration): Double
}

class HourlyPricing(
    private val rates: Map<VehicleType, Double> = mapOf(
        VehicleType.MOTORCYCLE to 10.0,
        VehicleType.CAR to 20.0,
        VehicleType.TRUCK to 30.0,
        VehicleType.ELECTRIC_CAR to 25.0
    )
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        val hours = Math.ceil(duration.toMinutes() / 60.0)
        val rate = rates[vehicleType] ?: 20.0
        return hours * rate
    }
}

class FlatRatePricing(
    private val flatRate: Double = 50.0
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        return flatRate
    }
}

class TimedPricing(
    private val firstHourRate: Double = 30.0,
    private val additionalHourRate: Double = 15.0
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        val hours = Math.ceil(duration.toMinutes() / 60.0).toInt()
        if (hours <= 0) return 0.0
        return firstHourRate + (hours - 1) * additionalHourRate
    }
}
```

### Parking Lot

```kotlin
// ==================== Parking Lot ====================

class ParkingLot private constructor(
    val name: String,
    private val floors: List<ParkingFloor>,
    private val pricingStrategy: PricingStrategy
) {
    private val activeTickets = ConcurrentHashMap<String, ParkingTicket>()
    private val vehicleToTicket = ConcurrentHashMap<String, ParkingTicket>()
    private val ticketToSpot = ConcurrentHashMap<String, ParkingSpot>()
    
    private val lock = ReentrantReadWriteLock()
    
    // ==================== Park/Unpark ====================
    
    fun parkVehicle(vehicle: Vehicle): ParkingTicket? {
        lock.write {
            // Check if vehicle already parked
            if (vehicleToTicket.containsKey(vehicle.licensePlate)) {
                println("Vehicle ${vehicle.licensePlate} is already parked")
                return null
            }
            
            // Find available spot
            for (floor in floors) {
                val spot = floor.findAvailableSpot(vehicle)
                if (spot != null && spot.park(vehicle)) {
                    val ticket = ParkingTicket(
                        vehicleLicensePlate = vehicle.licensePlate,
                        spotId = spot.id,
                        floorNumber = floor.floorNumber
                    )
                    
                    activeTickets[ticket.ticketId] = ticket
                    vehicleToTicket[vehicle.licensePlate] = ticket
                    ticketToSpot[ticket.ticketId] = spot
                    
                    println("Vehicle ${vehicle.licensePlate} parked at ${spot.id}")
                    return ticket
                }
            }
            
            println("No available spot for vehicle ${vehicle.licensePlate}")
            return null
        }
    }
    
    fun unparkVehicle(ticketId: String): ParkingReceipt? {
        lock.write {
            val ticket = activeTickets.remove(ticketId) ?: run {
                println("Invalid ticket: $ticketId")
                return null
            }
            
            val spot = ticketToSpot.remove(ticketId) ?: return null
            val vehicle = spot.unpark() ?: return null
            vehicleToTicket.remove(vehicle.licensePlate)
            
            val exitTime = Instant.now()
            val duration = Duration.between(ticket.entryTime, exitTime)
            val fee = pricingStrategy.calculateFee(vehicle.vehicleType, duration)
            
            val receipt = ParkingReceipt(
                ticket = ticket,
                exitTime = exitTime,
                duration = duration,
                totalFee = fee
            )
            
            println("Vehicle ${vehicle.licensePlate} unparked. Fee: $${fee}")
            return receipt
        }
    }
    
    // ==================== Query Methods ====================
    
    fun getAvailableSpots(): Map<Int, Map<SpotType, Int>> {
        return floors.associate { floor ->
            floor.floorNumber to floor.getAvailableSpotsCount()
        }
    }
    
    fun getTotalSpots(): Map<Int, Map<SpotType, Int>> {
        return floors.associate { floor ->
            floor.floorNumber to floor.getTotalSpotsCount()
        }
    }
    
    fun isVehicleParked(licensePlate: String): Boolean {
        return vehicleToTicket.containsKey(licensePlate)
    }
    
    fun getVehicleLocation(licensePlate: String): String? {
        val ticket = vehicleToTicket[licensePlate] ?: return null
        return "Floor ${ticket.floorNumber}, Spot ${ticket.spotId}"
    }
    
    fun displayStatus() {
        println("\n=== Parking Lot: $name ===")
        floors.forEach { floor ->
            println("Floor ${floor.floorNumber}:")
            floor.getAvailableSpotsCount().forEach { (type, count) ->
                val total = floor.getTotalSpotsCount()[type] ?: 0
                println("  $type: $count/$total available")
            }
        }
        println("Active vehicles: ${activeTickets.size}")
    }
    
    // ==================== Builder ====================
    
    class Builder(private val name: String) {
        private val floors = mutableListOf<ParkingFloor>()
        private var pricingStrategy: PricingStrategy = HourlyPricing()
        
        fun addFloor(
            compactSpots: Int = 0,
            regularSpots: Int = 0,
            largeSpots: Int = 0,
            electricSpots: Int = 0
        ) = apply {
            floors.add(
                ParkingFloor(
                    floorNumber = floors.size + 1,
                    compactSpots = compactSpots,
                    regularSpots = regularSpots,
                    largeSpots = largeSpots,
                    electricSpots = electricSpots
                )
            )
        }
        
        fun pricingStrategy(strategy: PricingStrategy) = apply {
            this.pricingStrategy = strategy
        }
        
        fun build(): ParkingLot {
            require(floors.isNotEmpty()) { "At least one floor required" }
            return ParkingLot(name, floors, pricingStrategy)
        }
    }
    
    companion object {
        fun builder(name: String) = Builder(name)
    }
}
```

### Usage Example

```kotlin
fun main() {
    // Build parking lot
    val parkingLot = ParkingLot.builder("Downtown Mall Parking")
        .addFloor(compactSpots = 5, regularSpots = 10, largeSpots = 3, electricSpots = 2)
        .addFloor(compactSpots = 5, regularSpots = 15, largeSpots = 5)
        .pricingStrategy(HourlyPricing())
        .build()
    
    parkingLot.displayStatus()
    
    // Park vehicles
    val car1 = Car("ABC-123")
    val bike1 = Motorcycle("BIKE-001")
    val truck1 = Truck("TRUCK-99")
    val electric1 = ElectricCar("ELEC-001")
    
    val ticket1 = parkingLot.parkVehicle(car1)
    val ticket2 = parkingLot.parkVehicle(bike1)
    val ticket3 = parkingLot.parkVehicle(truck1)
    val ticket4 = parkingLot.parkVehicle(electric1)
    
    parkingLot.displayStatus()
    
    // Check location
    println("\nCar location: ${parkingLot.getVehicleLocation("ABC-123")}")
    
    // Simulate time passing
    Thread.sleep(2000)
    
    // Unpark
    ticket1?.let {
        val receipt = parkingLot.unparkVehicle(it.ticketId)
        println("Receipt: $receipt")
    }
    
    parkingLot.displayStatus()
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Factory** | `VehicleFactory` | Create vehicle instances |
| **Strategy** | `PricingStrategy` | Pluggable pricing algorithms |
| **Builder** | `ParkingLot.Builder` | Complex object construction |
| **Singleton** | Can be applied to ParkingLot | Single parking lot instance |

---

## Interview Discussion Points

### Q: How would you handle multiple entry/exit points?
**A:** Add `EntryPanel` and `ExitPanel` classes that interact with `ParkingLot`. Each panel could be associated with specific floors.

### Q: How to optimize spot finding?
**A:** Use separate queues per spot type for O(1) allocation. Or use a min-heap sorted by floor distance from entry.

### Q: How to handle reservations?
**A:** Add a `ReservationService` with time slots, and mark spots as reserved during those periods.

---

## Time Complexity

| Operation | Complexity |
|-----------|------------|
| Park Vehicle | O(F × S) where F=floors, S=spots per floor |
| Unpark Vehicle | O(1) with ticket |
| Find Spot | O(S) per floor |
| Get Available | O(F × S) |

