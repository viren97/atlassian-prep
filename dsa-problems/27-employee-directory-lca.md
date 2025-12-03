# Employee Directory - Closest Common Group (LCA Variant)

## Problem Information
- **Difficulty**: Medium-Hard
- **Company**: Atlassian
- **Topics**: Tree, LCA, N-ary Tree, Concurrency, Design

## Problem Description

At Atlassian, there are multiple groups, and each group can have one or more subgroups. Every employee is part of a group.

Design a system that could find the **closest common parent group** given a target set of employees in the organization.

### Example:
```
                    Atlassian (root)
                    /           \
               Engineering      Product
               /    |    \         |
            Backend Frontend  Platform   Design
              |       |                    |
           [Alice]  [Bob]               [Charlie]
                    [Dave]
```

**Query**: `getCommonGroup([Alice, Bob])` → `Engineering`
**Query**: `getCommonGroup([Alice, Charlie])` → `Atlassian`
**Query**: `getCommonGroup([Bob, Dave])` → `Frontend`

---

## Part A: Basic Solution

### Data Model

```kotlin
data class Employee(
    val id: String,
    val name: String,
    val groupId: String
)

data class Group(
    val id: String,
    val name: String,
    val parentId: String?,  // null for root
    val subGroups: MutableList<String> = mutableListOf(),
    val employees: MutableList<String> = mutableListOf()
)

class EmployeeDirectory {
    private val groups = mutableMapOf<String, Group>()
    private val employees = mutableMapOf<String, Employee>()
    private val groupParent = mutableMapOf<String, String?>()
    
    fun addGroup(group: Group) {
        groups[group.id] = group
        groupParent[group.id] = group.parentId
    }
    
    fun addEmployee(employee: Employee) {
        employees[employee.id] = employee
        groups[employee.groupId]?.employees?.add(employee.id)
    }
    
    /**
     * Find closest common parent group for a set of employees
     * Time: O(H * K) where H = height of tree, K = number of employees
     */
    fun getCommonGroupForEmployees(employeeIds: List<String>): String? {
        if (employeeIds.isEmpty()) return null
        if (employeeIds.size == 1) {
            return employees[employeeIds[0]]?.groupId
        }
        
        // Get group for each employee
        val groupIds = employeeIds.mapNotNull { employees[it]?.groupId }
        if (groupIds.isEmpty()) return null
        
        return findLCA(groupIds)
    }
    
    /**
     * Find LCA of multiple groups using ancestor set approach
     */
    private fun findLCA(groupIds: List<String>): String? {
        if (groupIds.isEmpty()) return null
        if (groupIds.size == 1) return groupIds[0]
        
        // Get ancestors of first group
        val ancestors = getAncestors(groupIds[0])
        
        // For each other group, keep only common ancestors
        for (i in 1 until groupIds.size) {
            val currentAncestors = getAncestors(groupIds[i])
            ancestors.retainAll(currentAncestors)
        }
        
        // Find the deepest common ancestor
        // Ancestors are ordered from node to root, so first common one is deepest
        for (groupId in getAncestors(groupIds[0])) {
            if (groupId in ancestors) {
                return groupId
            }
        }
        
        return null
    }
    
    /**
     * Get all ancestors of a group (including itself)
     * Returns list from node to root
     */
    private fun getAncestors(groupId: String): MutableSet<String> {
        val ancestors = mutableSetOf<String>()
        var current: String? = groupId
        
        while (current != null) {
            ancestors.add(current)
            current = groupParent[current]
        }
        
        return ancestors
    }
}
```

### Python Implementation

```python
from typing import List, Optional, Set, Dict
from dataclasses import dataclass, field

@dataclass
class Employee:
    id: str
    name: str
    group_id: str

@dataclass
class Group:
    id: str
    name: str
    parent_id: Optional[str] = None
    sub_groups: List[str] = field(default_factory=list)
    employees: List[str] = field(default_factory=list)

class EmployeeDirectory:
    def __init__(self):
        self.groups: Dict[str, Group] = {}
        self.employees: Dict[str, Employee] = {}
        self.group_parent: Dict[str, Optional[str]] = {}
    
    def add_group(self, group: Group):
        self.groups[group.id] = group
        self.group_parent[group.id] = group.parent_id
    
    def add_employee(self, employee: Employee):
        self.employees[employee.id] = employee
        if employee.group_id in self.groups:
            self.groups[employee.group_id].employees.append(employee.id)
    
    def get_common_group_for_employees(self, employee_ids: List[str]) -> Optional[str]:
        """
        Find closest common parent group for a set of employees
        """
        if not employee_ids:
            return None
        
        if len(employee_ids) == 1:
            emp = self.employees.get(employee_ids[0])
            return emp.group_id if emp else None
        
        # Get group for each employee
        group_ids = []
        for emp_id in employee_ids:
            if emp_id in self.employees:
                group_ids.append(self.employees[emp_id].group_id)
        
        if not group_ids:
            return None
        
        return self._find_lca(group_ids)
    
    def _find_lca(self, group_ids: List[str]) -> Optional[str]:
        """Find LCA of multiple groups"""
        if not group_ids:
            return None
        if len(group_ids) == 1:
            return group_ids[0]
        
        # Get ancestors of first group
        common_ancestors = self._get_ancestors(group_ids[0])
        
        # Intersect with ancestors of each other group
        for group_id in group_ids[1:]:
            ancestors = self._get_ancestors(group_id)
            common_ancestors &= ancestors
        
        # Find deepest common ancestor
        for ancestor in self._get_ancestors_ordered(group_ids[0]):
            if ancestor in common_ancestors:
                return ancestor
        
        return None
    
    def _get_ancestors(self, group_id: str) -> Set[str]:
        """Get all ancestors including self"""
        ancestors = set()
        current = group_id
        
        while current is not None:
            ancestors.add(current)
            current = self.group_parent.get(current)
        
        return ancestors
    
    def _get_ancestors_ordered(self, group_id: str) -> List[str]:
        """Get ancestors from node to root"""
        ancestors = []
        current = group_id
        
        while current is not None:
            ancestors.append(current)
            current = self.group_parent.get(current)
        
        return ancestors
```

---

## Part B: Shared Groups / Employees in Multiple Groups

When employees can be in multiple groups or groups can be shared across orgs:

```kotlin
data class Employee(
    val id: String,
    val name: String,
    val groupIds: MutableSet<String> = mutableSetOf()  // Multiple groups
)

class EmployeeDirectoryV2 {
    private val groups = mutableMapOf<String, Group>()
    private val employees = mutableMapOf<String, Employee>()
    private val groupParent = mutableMapOf<String, String?>()
    
    /**
     * When employee is in multiple groups, we need to find
     * ONE common group across all possible paths.
     * 
     * Strategy: For each employee, consider all their groups.
     * Find LCA that is common to at least one group path for each employee.
     */
    fun getCommonGroupForEmployees(employeeIds: List<String>): String? {
        if (employeeIds.isEmpty()) return null
        
        // For each employee, get all possible ancestor sets
        val employeeAncestorSets = employeeIds.map { empId ->
            val employee = employees[empId] ?: return null
            
            // Union of all ancestors from all groups this employee belongs to
            val allAncestors = mutableSetOf<String>()
            for (groupId in employee.groupIds) {
                allAncestors.addAll(getAncestors(groupId))
            }
            allAncestors
        }
        
        // Find common ancestors across all employees
        var commonAncestors = employeeAncestorSets[0].toMutableSet()
        for (ancestorSet in employeeAncestorSets.drop(1)) {
            commonAncestors.retainAll(ancestorSet)
        }
        
        if (commonAncestors.isEmpty()) return null
        
        // Find the deepest common ancestor (one with maximum depth)
        return commonAncestors.maxByOrNull { getDepth(it) }
    }
    
    private fun getDepth(groupId: String): Int {
        var depth = 0
        var current: String? = groupId
        while (current != null) {
            depth++
            current = groupParent[current]
        }
        return depth
    }
    
    private fun getAncestors(groupId: String): Set<String> {
        val ancestors = mutableSetOf<String>()
        var current: String? = groupId
        while (current != null) {
            ancestors.add(current)
            current = groupParent[current]
        }
        return ancestors
    }
}
```

---

## Part C: Thread-Safe with Concurrent Updates

```kotlin
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class ThreadSafeEmployeeDirectory {
    private val groups = mutableMapOf<String, Group>()
    private val employees = mutableMapOf<String, Employee>()
    private val groupParent = mutableMapOf<String, String?>()
    
    private val lock = ReentrantReadWriteLock()
    
    // ============ READ OPERATIONS ============
    
    fun getCommonGroupForEmployees(employeeIds: List<String>): String? {
        return lock.read {
            // All read operations happen under read lock
            if (employeeIds.isEmpty()) return@read null
            
            val groupIds = employeeIds.mapNotNull { employees[it]?.groupId }
            if (groupIds.isEmpty()) return@read null
            
            findLCA(groupIds)
        }
    }
    
    fun getEmployee(employeeId: String): Employee? {
        return lock.read { employees[employeeId] }
    }
    
    fun getGroup(groupId: String): Group? {
        return lock.read { groups[groupId] }
    }
    
    // ============ WRITE OPERATIONS ============
    
    fun addGroup(group: Group) {
        lock.write {
            groups[group.id] = group
            groupParent[group.id] = group.parentId
            
            // Update parent's subgroups
            group.parentId?.let { parentId ->
                groups[parentId]?.subGroups?.add(group.id)
            }
        }
    }
    
    fun removeGroup(groupId: String) {
        lock.write {
            val group = groups[groupId] ?: return@write
            
            // Remove from parent's subgroups
            group.parentId?.let { parentId ->
                groups[parentId]?.subGroups?.remove(groupId)
            }
            
            // Move employees to parent group
            val parentId = group.parentId
            for (empId in group.employees) {
                employees[empId]?.let { emp ->
                    if (parentId != null) {
                        employees[empId] = emp.copy(groupId = parentId)
                        groups[parentId]?.employees?.add(empId)
                    }
                }
            }
            
            // Remove the group
            groups.remove(groupId)
            groupParent.remove(groupId)
        }
    }
    
    fun addEmployee(employee: Employee) {
        lock.write {
            employees[employee.id] = employee
            groups[employee.groupId]?.employees?.add(employee.id)
        }
    }
    
    fun moveEmployee(employeeId: String, newGroupId: String) {
        lock.write {
            val employee = employees[employeeId] ?: return@write
            
            // Remove from old group
            groups[employee.groupId]?.employees?.remove(employeeId)
            
            // Add to new group
            employees[employeeId] = employee.copy(groupId = newGroupId)
            groups[newGroupId]?.employees?.add(employeeId)
        }
    }
    
    // ============ PRIVATE HELPERS ============
    
    private fun findLCA(groupIds: List<String>): String? {
        if (groupIds.isEmpty()) return null
        if (groupIds.size == 1) return groupIds[0]
        
        val ancestors = getAncestors(groupIds[0]).toMutableSet()
        
        for (i in 1 until groupIds.size) {
            ancestors.retainAll(getAncestors(groupIds[i]))
        }
        
        // Find deepest common ancestor
        for (groupId in getAncestorsOrdered(groupIds[0])) {
            if (groupId in ancestors) {
                return groupId
            }
        }
        
        return null
    }
    
    private fun getAncestors(groupId: String): Set<String> {
        val ancestors = mutableSetOf<String>()
        var current: String? = groupId
        while (current != null) {
            ancestors.add(current)
            current = groupParent[current]
        }
        return ancestors
    }
    
    private fun getAncestorsOrdered(groupId: String): List<String> {
        val ancestors = mutableListOf<String>()
        var current: String? = groupId
        while (current != null) {
            ancestors.add(current)
            current = groupParent[current]
        }
        return ancestors
    }
}
```

### Python with Threading

```python
import threading
from typing import List, Optional

class ThreadSafeEmployeeDirectory:
    def __init__(self):
        self.groups = {}
        self.employees = {}
        self.group_parent = {}
        self._lock = threading.RWLock()  # Use rwlock from threading-utils
        # Or use threading.Lock() for simplicity
        self._lock = threading.Lock()
    
    def get_common_group_for_employees(self, employee_ids: List[str]) -> Optional[str]:
        with self._lock:  # Read lock
            if not employee_ids:
                return None
            
            group_ids = [
                self.employees[eid].group_id 
                for eid in employee_ids 
                if eid in self.employees
            ]
            
            return self._find_lca(group_ids) if group_ids else None
    
    def add_group(self, group):
        with self._lock:  # Write lock
            self.groups[group.id] = group
            self.group_parent[group.id] = group.parent_id
    
    def add_employee(self, employee):
        with self._lock:
            self.employees[employee.id] = employee
            if employee.group_id in self.groups:
                self.groups[employee.group_id].employees.append(employee.id)
    
    def move_employee(self, employee_id: str, new_group_id: str):
        with self._lock:
            if employee_id not in self.employees:
                return
            
            employee = self.employees[employee_id]
            old_group_id = employee.group_id
            
            # Remove from old group
            if old_group_id in self.groups:
                self.groups[old_group_id].employees.remove(employee_id)
            
            # Update employee
            employee.group_id = new_group_id
            
            # Add to new group
            if new_group_id in self.groups:
                self.groups[new_group_id].employees.append(employee_id)
```

---

## Part D: Flat Structure (No Subgroups)

When there are no subgroups, just groups with employees:

```kotlin
class FlatEmployeeDirectory {
    private val groups = mutableMapOf<String, MutableSet<String>>()  // groupId -> employeeIds
    private val employeeToGroup = mutableMapOf<String, String>()     // employeeId -> groupId
    
    fun addGroup(groupId: String) {
        groups.putIfAbsent(groupId, mutableSetOf())
    }
    
    fun addEmployee(employeeId: String, groupId: String) {
        groups.getOrPut(groupId) { mutableSetOf() }.add(employeeId)
        employeeToGroup[employeeId] = groupId
    }
    
    /**
     * In a flat structure, common group exists only if all employees
     * are in the SAME group.
     * 
     * If employees are in different groups, there's no common group
     * (unless we have a virtual "root" that contains all groups)
     */
    fun getCommonGroupForEmployees(employeeIds: List<String>): String? {
        if (employeeIds.isEmpty()) return null
        
        val groupIds = employeeIds.mapNotNull { employeeToGroup[it] }.toSet()
        
        return if (groupIds.size == 1) {
            groupIds.first()
        } else {
            // All employees not in same group - return root/company level
            "ROOT"  // Or null if no virtual root
        }
    }
    
    /**
     * Alternative: Return list of groups that contain all requested employees
     */
    fun getGroupsContainingAllEmployees(employeeIds: List<String>): List<String> {
        if (employeeIds.isEmpty()) return emptyList()
        
        val employeeSet = employeeIds.toSet()
        
        return groups.filter { (_, employees) ->
            employees.containsAll(employeeSet)
        }.keys.toList()
    }
}
```

---

## Complexity Analysis

| Operation | Part A | Part B | Part C |
|-----------|--------|--------|--------|
| Add Group | O(1) | O(1) | O(1) |
| Add Employee | O(1) | O(1) | O(1) |
| Get Common Group | O(K × H) | O(K × H × G) | O(K × H) |

Where:
- K = number of employees in query
- H = height of organization tree
- G = max groups per employee (Part B)

---

## Interview Tips

1. **Start simple**: Basic LCA for single group per employee
2. **Discuss trade-offs**: Read-heavy vs Write-heavy
3. **Mention caching**: For frequent queries, cache LCA results
4. **Consider scale**: For very large orgs, consider:
   - Binary lifting for O(log H) LCA
   - Euler tour + RMQ for O(1) LCA queries

