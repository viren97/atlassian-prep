# Parking Lot System - LLD

## Problem Statement
Design a parking lot system that can manage multiple floors, different vehicle types, and handle parking/unparking operations.

---

## Flow Diagrams

### Vehicle Entry Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      VEHICLE ENTRY FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌──────────┐     ┌──────────────┐     ┌─────────────────────────┐ │
│   │ Vehicle  │────▶│ Entry Panel  │────▶│    Parking Lot          │ │
│   │ Arrives  │     │              │     │    Manager              │ │
│   └──────────┘     └──────────────┘     └─────────────────────────┘ │
│                           │                       │                  │
│                           ▼                       ▼                  │
│                    ┌─────────────┐        ┌─────────────┐           │
│                    │Read License │        │Find Suitable│           │
│                    │   Plate     │        │    Spot     │           │
│                    └─────────────┘        └─────────────┘           │
│                                                  │                   │
│                                      ┌───────────┴────────────┐     │
│                                      │                        │     │
│                                 Spot Found              No Spot     │
│                                      │                        │     │
│                                      ▼                        ▼     │
│                              ┌─────────────┐         ┌────────────┐ │
│                              │Generate     │         │Display     │ │
│                              │  Ticket     │         │"LOT FULL"  │ │
│                              └─────────────┘         └────────────┘ │
│                                      │                              │
│                                      ▼                              │
│                              ┌─────────────┐                        │
│                              │Mark Spot    │                        │
│                              │ Occupied    │                        │
│                              └─────────────┘                        │
│                                      │                              │
│                                      ▼                              │
│                              ┌─────────────┐                        │
│                              │Open Gate    │                        │
│                              │Give Ticket  │                        │
│                              └─────────────┘                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Vehicle Exit Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                      VEHICLE EXIT FLOW                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌──────────┐     ┌──────────────┐                                 │
│   │ Vehicle  │────▶│  Exit Panel  │                                 │
│   │ at Exit  │     │              │                                 │
│   └──────────┘     └──────────────┘                                 │
│                           │                                          │
│                           ▼                                          │
│                    ┌─────────────┐                                  │
│                    │ Scan Ticket │                                  │
│                    └─────────────┘                                  │
│                           │                                          │
│                           ▼                                          │
│                    ┌─────────────────────────────────────┐          │
│                    │         Calculate Fee               │          │
│                    │                                     │          │
│                    │  Duration = exit_time - entry_time  │          │
│                    │  Fee = Duration × Rate_per_hour     │          │
│                    │       + Spot_type_multiplier        │          │
│                    └─────────────────────────────────────┘          │
│                           │                                          │
│                           ▼                                          │
│                    ┌─────────────┐                                  │
│                    │Process      │                                  │
│                    │ Payment     │                                  │
│                    └─────────────┘                                  │
│                           │                                          │
│                      ┌────┴────┐                                    │
│                      │         │                                    │
│                 Success      Failed                                 │
│                      │         │                                    │
│                      ▼         ▼                                    │
│               ┌───────────┐ ┌────────────┐                         │
│               │Free Spot  │ │Retry       │                         │
│               │Open Gate  │ │Payment     │                         │
│               │Give Receipt│ └────────────┘                         │
│               └───────────┘                                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Spot Allocation Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SPOT ALLOCATION STRATEGY                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Vehicle Type → Spot Type Mapping:                                 │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ MOTORCYCLE  →  COMPACT only                                  │   │
│   │ CAR         →  COMPACT, REGULAR                              │   │
│   │ TRUCK       →  LARGE only                                    │   │
│   │ ELECTRIC    →  ELECTRIC (with charger), REGULAR              │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   Allocation Algorithm:                                              │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │  findSpot(vehicle):                                          │  │
│   │                                                              │  │
│   │    for floor in floors (sorted by fill %):                   │  │
│   │      │                                                       │  │
│   │      └──▶ for spot in floor.spots:                          │  │
│   │             │                                                │  │
│   │             ├── spot.isAvailable?                           │  │
│   │             │        │                                       │  │
│   │             │   No ──┘                                       │  │
│   │             │   Yes                                          │  │
│   │             │    │                                           │  │
│   │             │    ▼                                           │  │
│   │             ├── spot.canFit(vehicle)?                       │  │
│   │             │        │                                       │  │
│   │             │   No ──┘                                       │  │
│   │             │   Yes                                          │  │
│   │             │    │                                           │  │
│   │             │    ▼                                           │  │
│   │             └── RETURN spot ✓                               │  │
│   │                                                              │  │
│   │    RETURN null (lot full)                                   │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Parking Lot Physical Layout

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PARKING LOT LAYOUT                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                        FLOOR 3                              │   │
│   │  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                 │   │
│   │  │ C │ C │ R │ R │ R │ R │ L │ L │ E │ E │                 │   │
│   │  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                 │   │
│   │   C = Compact  R = Regular  L = Large  E = Electric        │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              ▲                                      │
│                              │ Ramp                                 │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                        FLOOR 2                              │   │
│   │  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                 │   │
│   │  │ C │ C │ R │ R │ R │ R │ R │ R │ L │ L │                 │   │
│   │  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                 │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              ▲                                      │
│                              │ Ramp                                 │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │                        FLOOR 1 (Ground)                     │   │
│   │  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐                 │   │
│   │  │ C │ C │ C │ C │ R │ R │ R │ R │ R │ R │                 │   │
│   │  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘                 │   │
│   │                                                             │   │
│   │  ┌─────────┐                              ┌─────────┐       │   │
│   │  │ ENTRY   │                              │  EXIT   │       │   │
│   │  │ ──────▶ │                              │ ──────▶ │       │   │
│   │  └─────────┘                              └─────────┘       │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Fee Calculation Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    FEE CALCULATION                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Input: Ticket (entry_time, spot_type, vehicle_type)               │
│                                                                      │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │  1. Calculate Duration                                       │  │
│   │     hours = ceil((exit_time - entry_time) / 3600)            │  │
│   │                                                              │  │
│   │  2. Get Base Rate (per hour)                                 │  │
│   │     ┌──────────────┬────────┐                                │  │
│   │     │ Spot Type    │ Rate   │                                │  │
│   │     ├──────────────┼────────┤                                │  │
│   │     │ COMPACT      │ $2/hr  │                                │  │
│   │     │ REGULAR      │ $3/hr  │                                │  │
│   │     │ LARGE        │ $5/hr  │                                │  │
│   │     │ ELECTRIC     │ $4/hr  │                                │  │
│   │     └──────────────┴────────┘                                │  │
│   │                                                              │  │
│   │  3. Apply Multipliers                                        │  │
│   │     - Weekend: 1.2x                                          │  │
│   │     - Peak hours (9-11, 17-19): 1.5x                         │  │
│   │     - Member discount: 0.8x                                  │  │
│   │                                                              │  │
│   │  4. Final Fee = hours × rate × multipliers                   │  │
│   │                                                              │  │
│   └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│   Example:                                                           │
│   - Entry: 9:00 AM, Exit: 2:30 PM (5.5 hours → 6 hours)            │
│   - Spot: REGULAR ($3/hr)                                           │
│   - Peak hour entry: 1.5x                                           │
│   - Fee = 6 × $3 × 1.5 = $27                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Code Flow Walkthrough

### `parkVehicle(vehicle)` Step-by-Step

```
CALL: parkingLot.parkVehicle(Car(licensePlate="ABC-123"))

STEP 1: Find Compatible Spot Type
├── vehicle.getCompatibleSpotTypes():
│   ├── Car → [COMPACT, REGULAR, LARGE]
│   ├── Motorcycle → [COMPACT, REGULAR, LARGE]
│   ├── Truck → [LARGE]
│   └── Electric → [ELECTRIC, REGULAR, LARGE]
└── Returns list from smallest to largest (prefer exact fit)

STEP 2: Search Floors for Available Spot
├── FOR each floor in floors:
│   ├── FOR each spotType in compatibleTypes:
│   │   ├── spot = floor.findAvailableSpot(spotType):
│   │   │   ├── lock.read { ... }
│   │   │   ├── Filter spots: type matches AND isAvailable
│   │   │   └── Return first available OR null
│   │   ├── IF spot != null: break to Step 3
│   │   └── CONTINUE searching
│   └── CONTINUE to next floor
├── IF no spot found:
│   └── Return null (parking lot full for this vehicle type)

STEP 3: Park Vehicle in Spot
├── spot.park(vehicle):
│   ├── lock.write { ... }  // Exclusive access
│   ├── IF spot.vehicle != null: return false (race condition)
│   ├── spot.vehicle = vehicle
│   └── Return true
├── IF park failed (another thread took it):
│   └── Go back to Step 2, continue searching

STEP 4: Generate Ticket
├── ticket = ParkingTicket(
│   │   ticketId = UUID,
│   │   vehicleLicensePlate = "ABC-123",
│   │   spotId = "F1-R23",
│   │   floorNumber = 1,
│   │   entryTime = now
│   )
├── activeTickets[ticketId] = ticket
└── Return ticket

EXAMPLE SEARCH:
├── Car arrives, compatible: [COMPACT, REGULAR, LARGE]
├── Floor 1: COMPACT spots full, check REGULAR
├── Floor 1: REGULAR spot F1-R23 available!
├── Park car in F1-R23
└── Return ticket
```

### `unparkVehicle(ticket)` Step-by-Step

```
CALL: parkingLot.unparkVehicle(ticket)

STEP 1: Validate Ticket
├── IF !activeTickets.containsKey(ticket.ticketId):
│   └── throw InvalidTicketException("Ticket not found or already used")

STEP 2: Find the Spot
├── floor = floors[ticket.floorNumber]
├── spot = floor.getSpotById(ticket.spotId)
└── IF spot == null: throw SpotNotFoundException

STEP 3: Unpark Vehicle
├── spot.unpark():
│   ├── lock.write { ... }
│   ├── vehicle = spot.vehicle
│   ├── spot.vehicle = null
│   └── Return vehicle (or null if already empty)

STEP 4: Calculate Fee
├── duration = Duration.between(ticket.entryTime, now)
├── fee = pricingStrategy.calculateFee(vehicle.type, duration):
│   ├── HourlyPricing:
│   │   ├── hours = ceil(duration.toHours())
│   │   ├── rate = rates[vehicleType]  // e.g., $5/hour for Car
│   │   └── Return hours * rate
│   ├── FlatRatePricing:
│   │   └── Return flatRate per entry
│   └── TieredPricing:
│       ├── First 2 hours: $10
│       ├── 2-6 hours: $5/hour
│       └── 6+ hours: $25 flat

STEP 5: Generate Receipt
├── receipt = ParkingReceipt(
│   │   ticketId = ticket.ticketId,
│   │   entryTime = ticket.entryTime,
│   │   exitTime = now,
│   │   duration = duration,
│   │   fee = fee
│   )
├── activeTickets.remove(ticket.ticketId)
└── Return receipt

FEE CALCULATION EXAMPLE:
├── Entry: 10:00 AM
├── Exit: 2:30 PM
├── Duration: 4.5 hours → rounds to 5 hours
├── Car rate: $5/hour
├── Fee: 5 × $5 = $25
```

### `findAvailableSpot(spotType)` - Floor Level Search

```
CALL: floor.findAvailableSpot(SpotType.REGULAR)

STEP 1: Acquire Read Lock
├── lock.read { ... }
└── Multiple threads can search simultaneously

STEP 2: Filter Available Spots
├── spots.filter { spot ->
│   ├── spot.spotType == SpotType.REGULAR
│   └── spot.isAvailable (vehicle == null)
│ }
└── Returns list of matching available spots

STEP 3: Select Spot (Strategy)
├── Default: Return first available (spots.firstOrNull())
├── Alternative strategies:
│   ├── Nearest to entrance
│   ├── Nearest to elevator
│   └── Load balancing across floor

OPTIMIZATION:
├── Instead of scanning all spots:
│   ├── Maintain: availableSpotsByType[SpotType] = Set<Spot>
│   ├── On park: remove from set
│   ├── On unpark: add to set
│   └── O(1) to check availability
```

### Electric Vehicle Charging Flow

```
CALL: parkingLot.parkVehicle(ElectricCar("EV-123"))

STEP 1: Find Electric Spot (Preferred)
├── Search for SpotType.ELECTRIC first
├── Electric spots have charging capability
└── IF available: park in ELECTRIC spot

STEP 2: Fallback to Regular Spot
├── IF no ELECTRIC spots:
│   ├── Search REGULAR, then LARGE
│   └── EV can park but won't charge

STEP 3: Start Charging (if electric spot)
├── IF spot.spotType == ELECTRIC:
│   ├── spot.startCharging(vehicle):
│   │   ├── chargingStation.connect(vehicle)
│   │   └── Begin charging session
│   └── Track charging in ticket

STEP 4: On Unpark
├── IF was charging:
│   ├── spot.stopCharging()
│   ├── Calculate charging fee
│   └── Add to parking fee
```

### Concurrent Parking Handling

```
SCENARIO: Two cars arrive simultaneously, one REGULAR spot left

THREAD 1: parkVehicle(CarA)     THREAD 2: parkVehicle(CarB)
├── Find spot F1-R5             ├── Find spot F1-R5
├── Try to park...              ├── Try to park...
│   ├── lock.write              │   ├── wait for lock...
│   ├── Check: vehicle==null ✓  │   │
│   ├── Set: vehicle=CarA       │   │
│   └── Return true             │   │
├── Generate ticket             │   └── Acquires lock
│                               │   ├── Check: vehicle==null ✗
│                               │   └── Return false
│                               ├── Back to search
│                               ├── No more spots
│                               └── Return null (lot full)

KEY: The write lock ensures only one car gets the spot
```

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

/**
 * Represents a single parking spot with thread-safe operations.
 * 
 * === Thread Safety ===
 * Uses ReentrantReadWriteLock:
 * - read lock for isAvailable, getVehicle (allows concurrent reads)
 * - write lock for park, unpark (exclusive access)
 * 
 * === Vehicle Compatibility ===
 * Each spot type can fit certain vehicles:
 * - COMPACT: Motorcycles only
 * - REGULAR: Motorcycles, Cars
 * - LARGE: Motorcycles, Cars, Trucks
 * - ELECTRIC: Electric cars (has charging)
 * 
 * Time Complexity:
 * - park(): O(1)
 * - unpark(): O(1)
 * - isAvailable: O(1)
 */
class ParkingSpot(
    val id: String,           // Unique spot identifier (e.g., "F1-R5")
    val spotType: SpotType,   // Type determines which vehicles can park
    val floorNumber: Int      // For display/navigation purposes
) {
    private var vehicle: Vehicle? = null  // Currently parked vehicle
    private val lock = ReentrantReadWriteLock()
    
    // Property with read lock - multiple threads can check availability
    val isAvailable: Boolean
        get() = lock.read { vehicle == null }
    
    /**
     * Attempt to park a vehicle in this spot.
     * 
     * @param vehicle The vehicle to park
     * @return true if parked successfully, false if spot occupied or incompatible
     * 
     * Thread-safe: Uses write lock for exclusive access
     */
    fun park(vehicle: Vehicle): Boolean {
        // First check compatibility (no lock needed - vehicle is immutable)
        if (!vehicle.canFitIn(spotType)) return false
        
        return lock.write {
            // Double-check availability under lock (another thread might have parked)
            if (this.vehicle != null) return@write false
            this.vehicle = vehicle
            true
        }
    }
    
    /**
     * Remove vehicle from spot.
     * @return The vehicle that was parked, or null if spot was empty
     */
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

