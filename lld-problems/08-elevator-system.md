# Elevator System - LLD

## Problem Statement
Design an elevator system for a building with multiple elevators serving multiple floors.

---

## Code Flow Walkthrough

### `requestElevator(floor, direction)` - External Request

```
CALL: system.requestElevator(floor=5, direction=UP)
(Person at floor 5 wants to go up)

STEP 1: Create Request
├── request = ElevatorRequest(
│   │   sourceFloor = 5,
│   │   direction = UP,
│   │   timestamp = now
│   )

STEP 2: Dispatch to Best Elevator
├── elevator = dispatcher.dispatch(request, elevators)
│   ├── 
│   ├── // NearestElevatorDispatcher:
│   │   └── Return elevator closest to floor 5
│   ├── 
│   ├── // LOOKDispatcher (smarter):
│   │   ├── Priority 1: Elevator going UP, below floor 5
│   │   │   └── Will pass floor 5 on its way
│   │   ├── Priority 2: Idle elevator nearest to floor 5
│   │   │   └── Can respond quickly
│   │   └── Priority 3: Least loaded elevator
│   │       └── Balance load
│   ├── 
│   └── Returns: Elevator #2 (going UP from floor 3)

STEP 3: Add Destination to Elevator
├── elevator.addDestination(floor=5)
│   ├── destinationFloors.add(5)  // TreeSet, sorted
│   └── Elevator will stop at floor 5

STEP 4: Return Assigned Elevator
└── Return elevator #2

ELEVATOR STATE:
├── Before: destinations=[7, 10], floor=3, going UP
├── After:  destinations=[5, 7, 10], floor=3, going UP
└── Will stop at: 5 (pickup), 7, 10
```

### `elevator.move()` - Elevator Movement Cycle

```
RUNS: Continuously in elevator's thread

LOOP:
├── STEP 1: Check Destinations
│   ├── IF destinationFloors.isEmpty():
│   │   ├── direction = IDLE
│   │   └── Wait for new request
│   └── ELSE: proceed to move

├── STEP 2: Determine Direction
│   ├── IF any destination > currentFloor AND direction != DOWN:
│   │   └── direction = UP
│   ├── ELSE IF any destination < currentFloor AND direction != UP:
│   │   └── direction = DOWN
│   └── ELSE:
│       └── Reverse direction (end of run)

├── STEP 3: Move One Floor
│   ├── IF direction == UP:
│   │   └── currentFloor++
│   ├── ELSE IF direction == DOWN:
│   │   └── currentFloor--
│   ├── state = MOVING
│   └── Thread.sleep(floorTravelTime)  // Simulate travel

├── STEP 4: Check If Should Stop
│   ├── shouldStop = destinationFloors.contains(currentFloor)
│   ├── // Or using LOOK algorithm:
│   │   └── shouldStop = hasRequestAtCurrentFloor()
│   ├── IF shouldStop:
│   │   ├── openDoors()
│   │   ├── Thread.sleep(doorOpenTime)
│   │   ├── destinationFloors.remove(currentFloor)
│   │   └── closeDoors()

└── REPEAT loop

EXAMPLE TIMELINE:
├── T=0:  Floor 3, destinations=[5,7,10], UP
├── T=1:  Floor 4, no stop
├── T=2:  Floor 5, STOP (pickup passenger)
├── T=3:  Doors open/close
├── T=4:  Floor 6, no stop
├── T=5:  Floor 7, STOP (drop passenger)
├── ...
```

### LOOK Algorithm Dispatch Logic

```
SCENARIO: Request at floor 8, direction UP
Elevators:
├── E1: floor=3, direction=UP, destinations=[5,10]
├── E2: floor=12, direction=DOWN, destinations=[6]
├── E3: floor=8, direction=IDLE
├── E4: floor=7, direction=UP, destinations=[15]

STEP 1: Check "Moving Towards" (Priority 1)
├── E1: floor=3 < 8, going UP, request is UP
│   └── Will pass floor 8 → CANDIDATE ✓
├── E2: floor=12 > 8, going DOWN
│   └── Wrong direction, won't stop for UP request
├── E4: floor=7 < 8, going UP, request is UP
│   └── Will pass floor 8 → CANDIDATE ✓

STEP 2: Select Best from Candidates
├── E1: distance = 8 - 3 = 5 floors
├── E4: distance = 8 - 7 = 1 floor
└── Winner: E4 (closer)

STEP 3: If No "Moving Towards" Candidates
├── Check idle elevators (Priority 2)
│   ├── E3: floor=8, IDLE
│   └── Nearest idle → E3

STEP 4: Fallback
├── All else equal → least loaded elevator
└── Balance across elevators

RESULT:
├── E4 is assigned (1 floor away, already going UP)
├── E4 will stop at 8 before continuing to 15
└── Passenger picked up efficiently
```

### Internal Request (Inside Elevator)

```
CALL: elevator.selectFloor(12)
(Passenger inside elevator presses floor 12)

STEP 1: Validate Floor
├── IF floor < 0 OR floor > maxFloor:
│   └── Ignore invalid request
├── IF floor == currentFloor:
│   └── Already here, open doors

STEP 2: Add to Destinations
├── destinationFloors.add(12)  // TreeSet for sorted order
└── Elevator will visit floor 12

STEP 3: Optimize Order (automatic via TreeSet)
├── Before: destinations=[5, 10], currentFloor=3, UP
├── After:  destinations=[5, 10, 12]
└── Will visit in order: 5, 10, 12 (UP direction)

REVERSE DIRECTION SCENARIO:
├── currentFloor=8, direction=UP, destinations=[10]
├── Passenger presses 3
├── destinations=[3, 10]
├── Continue UP to 10 first (LOOK algorithm)
├── Then reverse to DOWN, go to 3
└── No zigzag: complete one direction first
```

### Maintenance Mode Flow

```
CALL: elevator.setMaintenance(true)

STEP 1: Update State
├── state = MAINTENANCE
├── // Stop accepting new requests

STEP 2: Complete Current Trip (graceful)
├── IF destinationFloors.isNotEmpty():
│   ├── Continue to next destination
│   ├── Let passengers off
│   └── Then enter full maintenance

STEP 3: Dispatcher Excludes
├── dispatcher.dispatch(request, elevators):
│   ├── Filter: elevators.filter { it.state != MAINTENANCE }
│   └── Maintenance elevator not assigned

EMERGENCY STOP:
├── CALL: elevator.emergencyStop()
├── state = STOPPED
├── Open doors immediately
└── Sound alarm
```

---

## Requirements

### Functional Requirements
1. Multiple elevators in a building
2. Handle up/down requests from floors
3. Handle floor selection from inside elevator
4. Efficient elevator dispatching algorithm
5. Display current floor and direction
6. Support maintenance mode

### Non-Functional Requirements
1. Minimize wait time
2. Thread-safe operations
3. Extensible scheduling algorithms

---

## Class Diagram

```
┌─────────────────────────────────────┐
│          ElevatorSystem             │
├─────────────────────────────────────┤
│ - elevators: List<Elevator>         │
│ - dispatcher: ElevatorDispatcher    │
│ - floors: Int                       │
├─────────────────────────────────────┤
│ + requestElevator(floor, direction) │
│ + getElevatorStatus(): List<Status> │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│            Elevator                 │
├─────────────────────────────────────┤
│ - id: Int                           │
│ - currentFloor: Int                 │
│ - direction: Direction              │
│ - state: ElevatorState              │
│ - destinationFloors: Set<Int>       │
├─────────────────────────────────────┤
│ + addDestination(floor: Int)        │
│ + move()                            │
│ + openDoors()                       │
│ + closeDoors()                      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│    <<interface>> ElevatorDispatcher │
├─────────────────────────────────────┤
│ + dispatch(request): Elevator       │
└─────────────────────────────────────┘
```

---

## Kotlin Implementation

### Enums and Data Classes

```kotlin
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

// ==================== Enums ====================

enum class Direction {
    UP, DOWN, IDLE
}

enum class ElevatorState {
    MOVING,
    STOPPED,
    DOORS_OPEN,
    MAINTENANCE
}

enum class DoorState {
    OPEN, CLOSED, OPENING, CLOSING
}

// ==================== Request ====================

data class ElevatorRequest(
    val id: String = UUID.randomUUID().toString(),
    val sourceFloor: Int,
    val direction: Direction,
    val timestamp: Long = System.currentTimeMillis()
)

data class InternalRequest(
    val destinationFloor: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// ==================== Status ====================

data class ElevatorStatus(
    val elevatorId: Int,
    val currentFloor: Int,
    val direction: Direction,
    val state: ElevatorState,
    val destinationFloors: Set<Int>,
    val passengerCount: Int
)
```

### Elevator Class

```kotlin
// ==================== Elevator ====================

class Elevator(
    val id: Int,
    private val minFloor: Int = 0,
    private val maxFloor: Int = 10,
    private val capacity: Int = 10
) {
    var currentFloor: Int = minFloor
        private set
    
    var direction: Direction = Direction.IDLE
        private set
    
    var state: ElevatorState = ElevatorState.STOPPED
        private set
    
    var doorState: DoorState = DoorState.CLOSED
        private set
    
    private val destinationFloors = TreeSet<Int>()
    private var passengerCount = 0
    private val lock = ReentrantLock()
    
    private val listeners = mutableListOf<ElevatorListener>()
    
    // ==================== External Requests ====================
    
    fun addDestination(floor: Int): Boolean {
        if (floor < minFloor || floor > maxFloor) return false
        if (state == ElevatorState.MAINTENANCE) return false
        
        lock.withLock {
            destinationFloors.add(floor)
            updateDirection()
            notifyDestinationAdded(floor)
            return true
        }
    }
    
    fun addPickup(floor: Int, requestDirection: Direction): Boolean {
        if (floor < minFloor || floor > maxFloor) return false
        if (state == ElevatorState.MAINTENANCE) return false
        
        lock.withLock {
            // Only add if we can service this request efficiently
            if (canServiceRequest(floor, requestDirection)) {
                destinationFloors.add(floor)
                updateDirection()
                return true
            }
            return false
        }
    }
    
    private fun canServiceRequest(floor: Int, requestDirection: Direction): Boolean {
        if (direction == Direction.IDLE) return true
        
        return when (direction) {
            Direction.UP -> floor >= currentFloor || requestDirection == Direction.UP
            Direction.DOWN -> floor <= currentFloor || requestDirection == Direction.DOWN
            Direction.IDLE -> true
        }
    }
    
    // ==================== Movement ====================
    
    fun step() {
        lock.withLock {
            when (state) {
                ElevatorState.STOPPED -> processNextMove()
                ElevatorState.MOVING -> move()
                ElevatorState.DOORS_OPEN -> closeDoors()
                ElevatorState.MAINTENANCE -> { /* Do nothing */ }
            }
        }
    }
    
    private fun processNextMove() {
        if (destinationFloors.isEmpty()) {
            direction = Direction.IDLE
            return
        }
        
        updateDirection()
        state = ElevatorState.MOVING
    }
    
    private fun move() {
        val targetFloor = getNextTargetFloor() ?: run {
            state = ElevatorState.STOPPED
            direction = Direction.IDLE
            return
        }
        
        // Move one floor
        currentFloor += if (targetFloor > currentFloor) 1 else -1
        notifyFloorChanged(currentFloor)
        
        // Check if we've arrived
        if (currentFloor == targetFloor) {
            arriveAtFloor()
        }
    }
    
    private fun getNextTargetFloor(): Int? {
        if (destinationFloors.isEmpty()) return null
        
        return when (direction) {
            Direction.UP -> destinationFloors.higher(currentFloor - 1) 
                ?: destinationFloors.lower(currentFloor + 1)
            Direction.DOWN -> destinationFloors.lower(currentFloor + 1) 
                ?: destinationFloors.higher(currentFloor - 1)
            Direction.IDLE -> destinationFloors.firstOrNull()
        }
    }
    
    private fun arriveAtFloor() {
        destinationFloors.remove(currentFloor)
        state = ElevatorState.DOORS_OPEN
        doorState = DoorState.OPEN
        notifyDoorsOpened()
        
        // Update direction if no more stops in current direction
        updateDirection()
    }
    
    private fun updateDirection() {
        direction = when {
            destinationFloors.isEmpty() -> Direction.IDLE
            destinationFloors.any { it > currentFloor } && 
                (direction == Direction.UP || direction == Direction.IDLE) -> Direction.UP
            destinationFloors.any { it < currentFloor } && 
                (direction == Direction.DOWN || direction == Direction.IDLE) -> Direction.DOWN
            destinationFloors.any { it > currentFloor } -> Direction.UP
            destinationFloors.any { it < currentFloor } -> Direction.DOWN
            else -> Direction.IDLE
        }
    }
    
    private fun closeDoors() {
        doorState = DoorState.CLOSED
        state = ElevatorState.STOPPED
        notifyDoorsClosed()
    }
    
    // ==================== Passenger Management ====================
    
    fun boardPassenger(): Boolean {
        if (doorState != DoorState.OPEN) return false
        if (passengerCount >= capacity) return false
        
        passengerCount++
        return true
    }
    
    fun exitPassenger(): Boolean {
        if (doorState != DoorState.OPEN) return false
        if (passengerCount <= 0) return false
        
        passengerCount--
        return true
    }
    
    // ==================== Status ====================
    
    fun getStatus(): ElevatorStatus {
        return lock.withLock {
            ElevatorStatus(
                elevatorId = id,
                currentFloor = currentFloor,
                direction = direction,
                state = state,
                destinationFloors = destinationFloors.toSet(),
                passengerCount = passengerCount
            )
        }
    }
    
    fun setMaintenance(enabled: Boolean) {
        lock.withLock {
            state = if (enabled) ElevatorState.MAINTENANCE else ElevatorState.STOPPED
        }
    }
    
    fun getDestinationCount(): Int = destinationFloors.size
    
    // ==================== Listeners ====================
    
    fun addListener(listener: ElevatorListener) {
        listeners.add(listener)
    }
    
    private fun notifyFloorChanged(floor: Int) {
        listeners.forEach { it.onFloorChanged(this, floor) }
    }
    
    private fun notifyDoorsOpened() {
        listeners.forEach { it.onDoorsOpened(this) }
    }
    
    private fun notifyDoorsClosed() {
        listeners.forEach { it.onDoorsClosed(this) }
    }
    
    private fun notifyDestinationAdded(floor: Int) {
        listeners.forEach { it.onDestinationAdded(this, floor) }
    }
}

// ==================== Listener ====================

interface ElevatorListener {
    fun onFloorChanged(elevator: Elevator, floor: Int) {}
    fun onDoorsOpened(elevator: Elevator) {}
    fun onDoorsClosed(elevator: Elevator) {}
    fun onDestinationAdded(elevator: Elevator, floor: Int) {}
}
```

### Dispatcher Strategies

```kotlin
// ==================== Dispatcher Interface ====================

interface ElevatorDispatcher {
    fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator?
}

// ==================== Nearest Elevator ====================

/**
 * Simple dispatcher: assigns the nearest available elevator.
 * 
 * Pros: Simple, predictable
 * Cons: Doesn't consider direction or existing destinations
 * 
 * Time Complexity: O(e) where e = number of elevators
 */
class NearestElevatorDispatcher : ElevatorDispatcher {
    override fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator? {
        return elevators
            .filter { it.state != ElevatorState.MAINTENANCE }
            .minByOrNull { abs(it.currentFloor - request.sourceFloor) }
    }
}

// ==================== LOOK Algorithm (Elevator Algorithm) ====================

/**
 * LOOK Algorithm - Smart elevator dispatching.
 * 
 * === How It Works ===
 * The LOOK algorithm (similar to disk scheduling) services requests
 * in one direction until no more requests in that direction, then reverses.
 * 
 * === Priority Order ===
 * 1. Elevator already moving towards request in same direction
 *    - Most efficient: no direction change needed
 * 2. Idle elevator nearest to request
 *    - No current tasks, can service immediately  
 * 3. Least loaded elevator
 *    - Fallback: balance load across elevators
 * 
 * === Example ===
 * Elevator at floor 5, going UP, destinations: [7, 10]
 * Request at floor 8 going UP → ASSIGN (on the way)
 * Request at floor 3 going DOWN → DON'T assign (wrong direction)
 * 
 * === Time Complexity ===
 * O(e) where e = number of elevators
 */
class LookDispatcher : ElevatorDispatcher {
    override fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator? {
        val availableElevators = elevators.filter { it.state != ElevatorState.MAINTENANCE }
        
        // Priority 1: Elevator already moving towards this floor in same direction
        // This is the most efficient assignment - no wasted travel
        val movingTowards = availableElevators.find { elevator ->
            when (elevator.direction) {
                Direction.UP -> elevator.currentFloor < request.sourceFloor && 
                               request.direction == Direction.UP
                Direction.DOWN -> elevator.currentFloor > request.sourceFloor && 
                                 request.direction == Direction.DOWN
                Direction.IDLE -> false
            }
        }
        if (movingTowards != null) return movingTowards
        
        // Priority 2: Idle elevator (nearest one)
        // No current tasks, can respond quickly
        val idleElevator = availableElevators
            .filter { it.direction == Direction.IDLE }
            .minByOrNull { abs(it.currentFloor - request.sourceFloor) }
        if (idleElevator != null) return idleElevator
        
        // Priority 3: Any available elevator (least loaded)
        // Balance load when no ideal option exists
        return availableElevators.minByOrNull { it.getDestinationCount() }
    }
}

// ==================== SCAN (Full Sweep) ====================

class ScanDispatcher : ElevatorDispatcher {
    override fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator? {
        val available = elevators.filter { it.state != ElevatorState.MAINTENANCE }
        
        // Find elevator that will reach this floor soonest
        return available.minByOrNull { elevator ->
            calculateETAScore(elevator, request.sourceFloor, request.direction)
        }
    }
    
    private fun calculateETAScore(elevator: Elevator, targetFloor: Int, direction: Direction): Int {
        val distance = abs(elevator.currentFloor - targetFloor)
        val stops = elevator.getDestinationCount()
        
        // Penalize if moving away from target
        val directionPenalty = when {
            elevator.direction == Direction.IDLE -> 0
            elevator.direction == Direction.UP && targetFloor < elevator.currentFloor -> 20
            elevator.direction == Direction.DOWN && targetFloor > elevator.currentFloor -> 20
            else -> 0
        }
        
        return distance + stops * 2 + directionPenalty
    }
}
```

### Elevator System

```kotlin
// ==================== Elevator System ====================

class ElevatorSystem(
    private val numElevators: Int,
    private val numFloors: Int,
    private val dispatcher: ElevatorDispatcher = LookDispatcher()
) {
    private val elevators: List<Elevator>
    private val pendingRequests = ConcurrentLinkedQueue<ElevatorRequest>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var running = false
    
    init {
        elevators = (1..numElevators).map { id ->
            Elevator(id, minFloor = 0, maxFloor = numFloors - 1)
        }
    }
    
    // ==================== Public API ====================
    
    fun requestElevator(floor: Int, direction: Direction): ElevatorRequest {
        require(floor in 0 until numFloors) { "Invalid floor" }
        
        val request = ElevatorRequest(sourceFloor = floor, direction = direction)
        
        // Try to assign immediately
        val elevator = dispatcher.dispatch(request, elevators)
        if (elevator != null) {
            elevator.addPickup(floor, direction)
            println("Elevator ${elevator.id} assigned to floor $floor ($direction)")
        } else {
            pendingRequests.offer(request)
            println("Request queued: floor $floor ($direction)")
        }
        
        return request
    }
    
    fun selectFloor(elevatorId: Int, floor: Int): Boolean {
        val elevator = elevators.find { it.id == elevatorId } ?: return false
        return elevator.addDestination(floor)
    }
    
    fun getStatus(): List<ElevatorStatus> {
        return elevators.map { it.getStatus() }
    }
    
    fun setMaintenance(elevatorId: Int, enabled: Boolean) {
        elevators.find { it.id == elevatorId }?.setMaintenance(enabled)
    }
    
    // ==================== Simulation ====================
    
    fun start() {
        if (running) return
        running = true
        
        // Run simulation step every 500ms
        executor.scheduleAtFixedRate({
            step()
        }, 0, 500, TimeUnit.MILLISECONDS)
        
        println("Elevator system started with $numElevators elevators, $numFloors floors")
    }
    
    fun stop() {
        running = false
        executor.shutdown()
    }
    
    private fun step() {
        // Move all elevators
        elevators.forEach { it.step() }
        
        // Process pending requests
        val iterator = pendingRequests.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            val elevator = dispatcher.dispatch(request, elevators)
            if (elevator != null) {
                elevator.addPickup(request.sourceFloor, request.direction)
                iterator.remove()
            }
        }
    }
    
    fun displayStatus() {
        println("\n=== Elevator Status ===")
        elevators.forEach { elevator ->
            val status = elevator.getStatus()
            println("Elevator ${status.elevatorId}: " +
                    "Floor ${status.currentFloor}, " +
                    "Direction: ${status.direction}, " +
                    "State: ${status.state}, " +
                    "Destinations: ${status.destinationFloors}")
        }
    }
}
```

### Usage Example

```kotlin
fun main() {
    val system = ElevatorSystem(
        numElevators = 3,
        numFloors = 15,
        dispatcher = LookDispatcher()
    )
    
    // Add listeners for logging
    system.getStatus().forEachIndexed { index, _ ->
        // Could add listeners here
    }
    
    system.start()
    
    // Simulate requests
    system.requestElevator(5, Direction.UP)
    system.requestElevator(10, Direction.DOWN)
    system.requestElevator(3, Direction.UP)
    
    // Select floor from inside elevator
    system.selectFloor(1, 8)
    system.selectFloor(2, 1)
    
    // Display status periodically
    repeat(10) {
        Thread.sleep(1000)
        system.displayStatus()
    }
    
    system.stop()
}
```

---

## Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Strategy** | `ElevatorDispatcher` | Pluggable scheduling algorithms |
| **Observer** | `ElevatorListener` | Notify of elevator events |
| **State** | `ElevatorState` | Manage elevator behavior |
| **Singleton** | Can apply to `ElevatorSystem` | Single building system |

---

## Scheduling Algorithms

| Algorithm | Description | Best For |
|-----------|-------------|----------|
| **FCFS** | First Come First Served | Simple, fair |
| **SSTF** | Shortest Seek Time First | Low average wait |
| **SCAN** | Sweep in one direction | Balanced |
| **LOOK** | Like SCAN, but reverses at last request | Efficient |

---

## Interview Discussion Points

### Q: How to handle rush hour (morning/evening)?
**A:** 
- Zoning: Dedicate elevators to floor ranges
- Express mode: Skip intermediate floors
- Pre-position elevators at lobby in morning

### Q: How to make this distributed?
**A:**
- Central controller with heartbeats from elevators
- State synchronization via message queue
- Failover to local mode if controller down

### Q: How to add emergency mode?
**A:**
- All elevators go to ground floor
- Override all requests
- Open doors and disable movement

---

## Time Complexity

| Operation | Complexity |
|-----------|------------|
| Request Elevator | O(E) where E = elevators |
| Add Destination | O(log D) with TreeSet |
| Get Next Floor | O(1) with TreeSet |
| Step/Move | O(1) |

