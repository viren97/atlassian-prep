/**
 * Parking Lot System - LLD Implementation
 * 
 * Design a parking lot system with:
 * - Multiple floors
 * - Different spot sizes (Compact, Regular, Large, Electric)
 * - Vehicle types (Motorcycle, Car, Truck, Electric)
 * - Parking/unparking with ticket generation
 * - Fee calculation with different pricing strategies
 * 
 * Design Patterns: Factory, Strategy, Builder
 */
package lld.parkinglot

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.ceil

// ==================== Enums ====================

enum class VehicleType {
    MOTORCYCLE,
    CAR,
    TRUCK,
    ELECTRIC_CAR
}

enum class SpotType {
    COMPACT,    // For motorcycles
    REGULAR,    // For cars
    LARGE,      // For trucks
    ELECTRIC    // For electric cars with charging
}

// ==================== Vehicle ====================

/**
 * Base class for vehicles.
 */
abstract class Vehicle(
    val licensePlate: String,
    val vehicleType: VehicleType
) {
    /**
     * Check if this vehicle can fit in the given spot type.
     */
    abstract fun canFitIn(spotType: SpotType): Boolean
    
    /**
     * Get compatible spot types (ordered by preference).
     */
    abstract fun getCompatibleSpotTypes(): List<SpotType>
}

class Motorcycle(licensePlate: String) : Vehicle(licensePlate, VehicleType.MOTORCYCLE) {
    override fun canFitIn(spotType: SpotType): Boolean = true // Can fit anywhere
    override fun getCompatibleSpotTypes() = listOf(SpotType.COMPACT, SpotType.REGULAR, SpotType.LARGE)
}

class Car(licensePlate: String) : Vehicle(licensePlate, VehicleType.CAR) {
    override fun canFitIn(spotType: SpotType): Boolean = 
        spotType in listOf(SpotType.REGULAR, SpotType.LARGE)
    override fun getCompatibleSpotTypes() = listOf(SpotType.REGULAR, SpotType.LARGE)
}

class Truck(licensePlate: String) : Vehicle(licensePlate, VehicleType.TRUCK) {
    override fun canFitIn(spotType: SpotType): Boolean = spotType == SpotType.LARGE
    override fun getCompatibleSpotTypes() = listOf(SpotType.LARGE)
}

class ElectricCar(licensePlate: String) : Vehicle(licensePlate, VehicleType.ELECTRIC_CAR) {
    override fun canFitIn(spotType: SpotType): Boolean = 
        spotType in listOf(SpotType.ELECTRIC, SpotType.REGULAR, SpotType.LARGE)
    override fun getCompatibleSpotTypes() = listOf(SpotType.ELECTRIC, SpotType.REGULAR, SpotType.LARGE)
}

// ==================== Parking Spot ====================

/**
 * Represents a single parking spot.
 * 
 * Thread-safe using ReentrantReadWriteLock:
 * - Read lock for checking availability
 * - Write lock for parking/unparking
 */
class ParkingSpot(
    val id: String,
    val spotType: SpotType,
    val floorNumber: Int
) {
    private var vehicle: Vehicle? = null
    private val lock = ReentrantReadWriteLock()
    
    val isAvailable: Boolean
        get() = lock.read { vehicle == null }
    
    /**
     * Park a vehicle in this spot.
     * 
     * @return true if parked successfully, false if occupied or incompatible
     */
    fun park(vehicle: Vehicle): Boolean {
        if (!vehicle.canFitIn(spotType)) return false
        
        return lock.write {
            if (this.vehicle != null) return@write false
            this.vehicle = vehicle
            true
        }
    }
    
    /**
     * Remove vehicle from spot.
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

// ==================== Parking Floor ====================

/**
 * Represents a floor in the parking lot.
 */
class ParkingFloor(
    val floorNumber: Int,
    compactSpots: Int = 10,
    regularSpots: Int = 20,
    largeSpots: Int = 5,
    electricSpots: Int = 5
) {
    private val spots = mutableListOf<ParkingSpot>()
    private val spotCounter = AtomicInteger(0)
    
    init {
        repeat(compactSpots) { spots.add(createSpot(SpotType.COMPACT)) }
        repeat(regularSpots) { spots.add(createSpot(SpotType.REGULAR)) }
        repeat(largeSpots) { spots.add(createSpot(SpotType.LARGE)) }
        repeat(electricSpots) { spots.add(createSpot(SpotType.ELECTRIC)) }
    }
    
    private fun createSpot(type: SpotType): ParkingSpot {
        val id = "F${floorNumber}-${type.name[0]}${spotCounter.incrementAndGet()}"
        return ParkingSpot(id, type, floorNumber)
    }
    
    /**
     * Find an available spot of the given type.
     */
    fun findAvailableSpot(spotType: SpotType): ParkingSpot? {
        return spots.firstOrNull { it.spotType == spotType && it.isAvailable }
    }
    
    /**
     * Get count of available spots by type.
     */
    fun getAvailableCount(): Map<SpotType, Int> {
        return spots.filter { it.isAvailable }
            .groupingBy { it.spotType }
            .eachCount()
    }
    
    /**
     * Get total spots by type.
     */
    fun getTotalCount(): Map<SpotType, Int> {
        return spots.groupingBy { it.spotType }.eachCount()
    }
    
    fun getSpotById(spotId: String): ParkingSpot? {
        return spots.find { it.id == spotId }
    }
}

// ==================== Ticket & Receipt ====================

data class ParkingTicket(
    val ticketId: String = UUID.randomUUID().toString(),
    val vehicleLicensePlate: String,
    val vehicleType: VehicleType,
    val spotId: String,
    val floorNumber: Int,
    val entryTime: Instant = Instant.now()
)

data class ParkingReceipt(
    val ticketId: String,
    val entryTime: Instant,
    val exitTime: Instant,
    val duration: Duration,
    val spotType: SpotType,
    val fee: Double
)

// ==================== Pricing Strategy ====================

/**
 * Strategy interface for fee calculation.
 */
interface PricingStrategy {
    fun calculateFee(vehicleType: VehicleType, duration: Duration): Double
}

/**
 * Hourly rate pricing.
 */
class HourlyPricing(
    private val rates: Map<VehicleType, Double> = mapOf(
        VehicleType.MOTORCYCLE to 1.0,
        VehicleType.CAR to 2.0,
        VehicleType.TRUCK to 3.0,
        VehicleType.ELECTRIC_CAR to 2.5
    )
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        val hours = ceil(duration.toMinutes() / 60.0)
        val rate = rates[vehicleType] ?: 2.0
        return hours * rate
    }
}

/**
 * Flat rate pricing per entry.
 */
class FlatRatePricing(
    private val flatRate: Double = 10.0
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        return flatRate
    }
}

/**
 * Tiered pricing with different rates for different durations.
 */
class TieredPricing(
    private val firstHourRate: Double = 5.0,
    private val subsequentHourRate: Double = 2.0,
    private val dailyMaxRate: Double = 25.0
) : PricingStrategy {
    
    override fun calculateFee(vehicleType: VehicleType, duration: Duration): Double {
        val hours = ceil(duration.toMinutes() / 60.0)
        
        if (hours <= 0) return 0.0
        if (hours <= 1) return firstHourRate
        
        val fee = firstHourRate + (hours - 1) * subsequentHourRate
        return minOf(fee, dailyMaxRate)
    }
}

// ==================== Parking Lot ====================

/**
 * Main Parking Lot class.
 * 
 * === Code Flow: parkVehicle(vehicle) ===
 * 1. Get compatible spot types for vehicle
 * 2. Search floors for available spot
 * 3. Park vehicle in spot (thread-safe)
 * 4. Generate and store ticket
 * 
 * === Code Flow: unparkVehicle(ticket) ===
 * 1. Validate ticket
 * 2. Find spot and unpark vehicle
 * 3. Calculate fee using pricing strategy
 * 4. Generate receipt
 */
class ParkingLot(
    private val floors: List<ParkingFloor>,
    private val pricingStrategy: PricingStrategy = HourlyPricing()
) {
    private val activeTickets = ConcurrentHashMap<String, ParkingTicket>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * Park a vehicle and return a ticket.
     * 
     * @return Ticket if parked successfully, null if lot is full
     */
    fun parkVehicle(vehicle: Vehicle): ParkingTicket? {
        lock.write {
            // Find compatible spot
            val compatibleTypes = vehicle.getCompatibleSpotTypes()
            
            for (floor in floors) {
                for (spotType in compatibleTypes) {
                    val spot = floor.findAvailableSpot(spotType)
                    if (spot != null && spot.park(vehicle)) {
                        val ticket = ParkingTicket(
                            vehicleLicensePlate = vehicle.licensePlate,
                            vehicleType = vehicle.vehicleType,
                            spotId = spot.id,
                            floorNumber = floor.floorNumber
                        )
                        activeTickets[ticket.ticketId] = ticket
                        return ticket
                    }
                }
            }
            
            return null // No spot available
        }
    }
    
    /**
     * Unpark a vehicle and return a receipt.
     */
    fun unparkVehicle(ticket: ParkingTicket): ParkingReceipt {
        lock.write {
            if (!activeTickets.containsKey(ticket.ticketId)) {
                throw IllegalArgumentException("Invalid ticket: ${ticket.ticketId}")
            }
            
            val floor = floors.getOrNull(ticket.floorNumber)
                ?: throw IllegalStateException("Floor not found: ${ticket.floorNumber}")
            
            val spot = floor.getSpotById(ticket.spotId)
                ?: throw IllegalStateException("Spot not found: ${ticket.spotId}")
            
            spot.unpark()
            activeTickets.remove(ticket.ticketId)
            
            val exitTime = Instant.now()
            val duration = Duration.between(ticket.entryTime, exitTime)
            val fee = pricingStrategy.calculateFee(ticket.vehicleType, duration)
            
            return ParkingReceipt(
                ticketId = ticket.ticketId,
                entryTime = ticket.entryTime,
                exitTime = exitTime,
                duration = duration,
                spotType = spot.spotType,
                fee = fee
            )
        }
    }
    
    /**
     * Get available spots count per type.
     */
    fun getAvailableSpots(): Map<SpotType, Int> {
        return lock.read {
            floors.flatMap { it.getAvailableCount().entries }
                .groupingBy { it.key }
                .fold(0) { acc, entry -> acc + entry.value }
        }
    }
    
    /**
     * Check if lot has space for a vehicle type.
     */
    fun hasSpaceFor(vehicle: Vehicle): Boolean {
        return lock.read {
            val compatibleTypes = vehicle.getCompatibleSpotTypes()
            floors.any { floor ->
                compatibleTypes.any { type ->
                    floor.findAvailableSpot(type) != null
                }
            }
        }
    }
    
    /**
     * Get total capacity.
     */
    fun getTotalCapacity(): Map<SpotType, Int> {
        return floors.flatMap { it.getTotalCount().entries }
            .groupingBy { it.key }
            .fold(0) { acc, entry -> acc + entry.value }
    }
}

// ==================== Parking Lot Builder ====================

class ParkingLotBuilder {
    private val floors = mutableListOf<ParkingFloor>()
    private var pricingStrategy: PricingStrategy = HourlyPricing()
    
    fun addFloor(
        compactSpots: Int = 10,
        regularSpots: Int = 20,
        largeSpots: Int = 5,
        electricSpots: Int = 5
    ) = apply {
        floors.add(ParkingFloor(
            floorNumber = floors.size,
            compactSpots = compactSpots,
            regularSpots = regularSpots,
            largeSpots = largeSpots,
            electricSpots = electricSpots
        ))
    }
    
    fun withPricing(strategy: PricingStrategy) = apply {
        this.pricingStrategy = strategy
    }
    
    fun build(): ParkingLot {
        require(floors.isNotEmpty()) { "Parking lot must have at least one floor" }
        return ParkingLot(floors.toList(), pricingStrategy)
    }
}

// ==================== Usage Example ====================

fun main() {
    println("=== Parking Lot System ===\n")
    
    // Build parking lot
    val parkingLot = ParkingLotBuilder()
        .addFloor(compactSpots = 5, regularSpots = 10, largeSpots = 3, electricSpots = 2)
        .addFloor(compactSpots = 5, regularSpots = 10, largeSpots = 3, electricSpots = 2)
        .withPricing(TieredPricing(firstHourRate = 5.0, subsequentHourRate = 2.0))
        .build()
    
    println("Parking lot created:")
    println("Total capacity: ${parkingLot.getTotalCapacity()}")
    println("Available spots: ${parkingLot.getAvailableSpots()}\n")
    
    // Park vehicles
    val car1 = Car("ABC-123")
    val car2 = Car("DEF-456")
    val truck = Truck("TRK-001")
    val electricCar = ElectricCar("EV-001")
    val motorcycle = Motorcycle("BIKE-001")
    
    val ticket1 = parkingLot.parkVehicle(car1)
    println("Parked car1: ${ticket1?.spotId}")
    
    val ticket2 = parkingLot.parkVehicle(car2)
    println("Parked car2: ${ticket2?.spotId}")
    
    val ticket3 = parkingLot.parkVehicle(truck)
    println("Parked truck: ${ticket3?.spotId}")
    
    val ticket4 = parkingLot.parkVehicle(electricCar)
    println("Parked electric car: ${ticket4?.spotId}")
    
    val ticket5 = parkingLot.parkVehicle(motorcycle)
    println("Parked motorcycle: ${ticket5?.spotId}")
    
    println("\nAvailable spots after parking: ${parkingLot.getAvailableSpots()}")
    
    // Simulate time passing (in real scenario)
    Thread.sleep(100)
    
    // Unpark a vehicle
    if (ticket1 != null) {
        val receipt = parkingLot.unparkVehicle(ticket1)
        println("\n=== Receipt ===")
        println("Ticket: ${receipt.ticketId}")
        println("Duration: ${receipt.duration.toMinutes()} minutes")
        println("Spot type: ${receipt.spotType}")
        println("Fee: $${receipt.fee}")
    }
    
    println("\nAvailable spots after unparking: ${parkingLot.getAvailableSpots()}")
}

