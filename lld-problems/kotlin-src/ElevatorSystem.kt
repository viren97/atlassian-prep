/**
 * Elevator System - LLD Implementation
 * 
 * Design an elevator system with:
 * - Multiple elevators
 * - Smart dispatching (LOOK algorithm)
 * - Floor requests and internal buttons
 * - Maintenance mode
 * 
 * Design Patterns: Strategy, State, Observer
 */
package lld.elevator

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
    val elevatorId: Int,
    val targetFloor: Int
)

// ==================== Elevator ====================

/**
 * Single elevator unit.
 * 
 * === Code Flow: move() ===
 * 1. Check if destinations exist
 * 2. Determine direction based on destinations
 * 3. Move one floor
 * 4. Check if should stop
 * 5. Open/close doors if stopping
 * 6. Remove floor from destinations
 */
class Elevator(
    val id: Int,
    private val minFloor: Int = 0,
    private val maxFloor: Int = 10
) {
    var currentFloor: Int = 0
        private set
    var direction: Direction = Direction.IDLE
        private set
    var state: ElevatorState = ElevatorState.STOPPED
        private set
    var doorState: DoorState = DoorState.CLOSED
        private set
    
    // TreeSet keeps destinations sorted
    private val upDestinations = TreeSet<Int>()
    private val downDestinations = TreeSet<Int>(reverseOrder())
    
    private val lock = ReentrantLock()
    private val listeners = mutableListOf<ElevatorListener>()
    
    /**
     * Add a destination floor.
     */
    fun addDestination(floor: Int) {
        lock.withLock {
            if (floor < minFloor || floor > maxFloor) return
            if (floor == currentFloor) return
            
            if (floor > currentFloor) {
                upDestinations.add(floor)
            } else {
                downDestinations.add(floor)
            }
            
            updateDirection()
        }
    }
    
    /**
     * Move one floor in current direction.
     */
    fun move() {
        lock.withLock {
            if (state == ElevatorState.MAINTENANCE) return
            if (direction == Direction.IDLE) return
            
            state = ElevatorState.MOVING
            
            when (direction) {
                Direction.UP -> {
                    if (currentFloor < maxFloor) currentFloor++
                }
                Direction.DOWN -> {
                    if (currentFloor > minFloor) currentFloor--
                }
                else -> {}
            }
            
            notifyFloorChanged()
            
            // Check if we should stop
            if (shouldStop()) {
                stop()
            }
        }
    }
    
    /**
     * Check if elevator should stop at current floor.
     */
    private fun shouldStop(): Boolean {
        return when (direction) {
            Direction.UP -> upDestinations.contains(currentFloor)
            Direction.DOWN -> downDestinations.contains(currentFloor)
            else -> false
        }
    }
    
    /**
     * Stop at current floor, open doors.
     */
    private fun stop() {
        state = ElevatorState.STOPPED
        openDoors()
        
        // Remove from destinations
        upDestinations.remove(currentFloor)
        downDestinations.remove(currentFloor)
        
        notifyPassengersServed()
        
        closeDoors()
        updateDirection()
    }
    
    private fun openDoors() {
        doorState = DoorState.OPENING
        state = ElevatorState.DOORS_OPEN
        doorState = DoorState.OPEN
    }
    
    private fun closeDoors() {
        doorState = DoorState.CLOSING
        doorState = DoorState.CLOSED
        state = ElevatorState.STOPPED
    }
    
    /**
     * Update direction based on remaining destinations.
     */
    private fun updateDirection() {
        direction = when {
            direction == Direction.UP && upDestinations.isNotEmpty() -> Direction.UP
            direction == Direction.DOWN && downDestinations.isNotEmpty() -> Direction.DOWN
            upDestinations.isNotEmpty() -> Direction.UP
            downDestinations.isNotEmpty() -> Direction.DOWN
            else -> Direction.IDLE
        }
    }
    
    fun getDestinationCount(): Int = lock.withLock {
        upDestinations.size + downDestinations.size
    }
    
    fun setMaintenance(enabled: Boolean) {
        lock.withLock {
            state = if (enabled) ElevatorState.MAINTENANCE else ElevatorState.STOPPED
        }
    }
    
    fun addListener(listener: ElevatorListener) {
        listeners.add(listener)
    }
    
    private fun notifyFloorChanged() {
        listeners.forEach { it.onFloorChanged(this, currentFloor) }
    }
    
    private fun notifyPassengersServed() {
        listeners.forEach { it.onPassengersServed(this, currentFloor) }
    }
    
    override fun toString(): String {
        return "Elevator($id, floor=$currentFloor, dir=$direction, state=$state)"
    }
}

// ==================== Elevator Listener ====================

interface ElevatorListener {
    fun onFloorChanged(elevator: Elevator, floor: Int)
    fun onPassengersServed(elevator: Elevator, floor: Int)
}

// ==================== Dispatcher ====================

/**
 * Strategy interface for elevator dispatching.
 */
interface ElevatorDispatcher {
    fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator?
}

/**
 * Simple nearest elevator dispatcher.
 */
class NearestElevatorDispatcher : ElevatorDispatcher {
    override fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator? {
        return elevators
            .filter { it.state != ElevatorState.MAINTENANCE }
            .minByOrNull { abs(it.currentFloor - request.sourceFloor) }
    }
}

/**
 * LOOK algorithm dispatcher.
 * 
 * === Priority Order ===
 * 1. Elevator already moving towards request in same direction
 * 2. Idle elevator nearest to request
 * 3. Least loaded elevator
 * 
 * === Why This Order? ===
 * - Minimizes wait time
 * - Reduces unnecessary direction changes
 * - Balances load across elevators
 */
class LookDispatcher : ElevatorDispatcher {
    override fun dispatch(request: ElevatorRequest, elevators: List<Elevator>): Elevator? {
        val availableElevators = elevators.filter { it.state != ElevatorState.MAINTENANCE }
        
        // Priority 1: Elevator already moving towards this floor in same direction
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
        
        // Priority 2: Idle elevator (nearest)
        val idleElevator = availableElevators
            .filter { it.direction == Direction.IDLE }
            .minByOrNull { abs(it.currentFloor - request.sourceFloor) }
        if (idleElevator != null) return idleElevator
        
        // Priority 3: Any available (least loaded)
        return availableElevators.minByOrNull { it.getDestinationCount() }
    }
}

// ==================== Elevator System ====================

/**
 * Main elevator system controller.
 * 
 * === Code Flow: requestElevator(floor, direction) ===
 * 1. Create request
 * 2. Dispatch to best elevator using strategy
 * 3. Add floor to elevator's destinations
 * 4. Return assigned elevator
 */
class ElevatorSystem(
    private val numElevators: Int = 4,
    private val numFloors: Int = 10,
    private val dispatcher: ElevatorDispatcher = LookDispatcher()
) {
    private val elevators = (0 until numElevators).map { 
        Elevator(it, 0, numFloors) 
    }
    
    private val pendingRequests = ConcurrentLinkedQueue<ElevatorRequest>()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    
    init {
        // Start simulation loop
        executor.scheduleAtFixedRate(
            { simulateStep() },
            0,
            500,
            TimeUnit.MILLISECONDS
        )
    }
    
    /**
     * Request an elevator from a floor.
     */
    fun requestElevator(floor: Int, direction: Direction): Elevator? {
        val request = ElevatorRequest(
            sourceFloor = floor,
            direction = direction
        )
        
        val elevator = dispatcher.dispatch(request, elevators)
        elevator?.addDestination(floor)
        
        return elevator
    }
    
    /**
     * Select floor from inside an elevator.
     */
    fun selectFloor(elevatorId: Int, floor: Int) {
        elevators.getOrNull(elevatorId)?.addDestination(floor)
    }
    
    /**
     * Get status of all elevators.
     */
    fun getStatus(): List<String> {
        return elevators.map { it.toString() }
    }
    
    /**
     * Set maintenance mode for an elevator.
     */
    fun setMaintenance(elevatorId: Int, enabled: Boolean) {
        elevators.getOrNull(elevatorId)?.setMaintenance(enabled)
    }
    
    /**
     * Simulation step - move all elevators.
     */
    private fun simulateStep() {
        elevators.forEach { it.move() }
    }
    
    /**
     * Shutdown the system.
     */
    fun shutdown() {
        executor.shutdown()
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Elevator System ===\n")
    
    val system = ElevatorSystem(numElevators = 3, numFloors = 10)
    
    // Add listeners for monitoring
    println("Initial status:")
    system.getStatus().forEach { println("  $it") }
    
    println("\nSimulating elevator requests...\n")
    
    // Person at floor 5 wants to go up
    val elevator1 = system.requestElevator(5, Direction.UP)
    println("Request from floor 5 going UP -> assigned to Elevator ${elevator1?.id}")
    
    // Person at floor 2 wants to go up
    val elevator2 = system.requestElevator(2, Direction.UP)
    println("Request from floor 2 going UP -> assigned to Elevator ${elevator2?.id}")
    
    // Person at floor 8 wants to go down
    val elevator3 = system.requestElevator(8, Direction.DOWN)
    println("Request from floor 8 going DOWN -> assigned to Elevator ${elevator3?.id}")
    
    // Internal button press - go to floor 10
    elevator1?.let { 
        system.selectFloor(it.id, 10)
        println("Elevator ${it.id}: Selected floor 10")
    }
    
    // Wait for simulation
    Thread.sleep(3000)
    
    println("\nStatus after 3 seconds:")
    system.getStatus().forEach { println("  $it") }
    
    // Put elevator 0 in maintenance
    system.setMaintenance(0, true)
    println("\nElevator 0 set to maintenance mode")
    
    // New request should avoid elevator 0
    val elevator4 = system.requestElevator(3, Direction.UP)
    println("Request from floor 3 going UP -> assigned to Elevator ${elevator4?.id}")
    
    Thread.sleep(2000)
    
    println("\nFinal status:")
    system.getStatus().forEach { println("  $it") }
    
    system.shutdown()
    println("\nElevator system shut down")
}

