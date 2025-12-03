# Nth Highest Salary

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 50.8%
- **Acceptance Rate**: 38.0%
- **Topics**: Database
- **LeetCode Link**: https://leetcode.com/problems/nth-highest-salary

## Problem Description

Write a SQL query to find the `n-th` highest salary from the `Employee` table. If there is no `n-th` highest salary, return `null`.

### Table: Employee
```
+-------------+------+
| Column Name | Type |
+-------------+------+
| id          | int  |
| salary      | int  |
+-------------+------+
id is the primary key for this table.
Each row contains information about the salary of an employee.
```

## Examples

### Example 1:
```
Input: 
Employee table:
+----+--------+
| id | salary |
+----+--------+
| 1  | 100    |
| 2  | 200    |
| 3  | 300    |
+----+--------+
n = 2

Output: 
+------------------------+
| getNthHighestSalary(2) |
+------------------------+
| 200                    |
+------------------------+
```

### Example 2:
```
Input: 
Employee table:
+----+--------+
| id | salary |
+----+--------+
| 1  | 100    |
+----+--------+
n = 2

Output: 
+------------------------+
| getNthHighestSalary(2) |
+------------------------+
| null                   |
+------------------------+
```

## Constraints

- `1 <= n <= 10^9`

## Approach

### Key Considerations:
1. Need to handle DISTINCT salaries (same salary shouldn't count twice)
2. Return NULL if Nth highest doesn't exist
3. Need to create a function (MySQL syntax)

### Methods:
1. **LIMIT with OFFSET**: Skip (N-1) rows, take 1
2. **Subquery counting**: Find salary with exactly (N-1) salaries above it
3. **Window functions**: Use DENSE_RANK() (MySQL 8.0+)

## Solution 1: LIMIT with OFFSET (MySQL)

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  SET N = N - 1;
  RETURN (
    SELECT DISTINCT salary
    FROM Employee
    ORDER BY salary DESC
    LIMIT 1 OFFSET N
  );
END
```

## Solution 2: Using Subquery

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  RETURN (
    SELECT DISTINCT e1.salary
    FROM Employee e1
    WHERE (N - 1) = (
      SELECT COUNT(DISTINCT e2.salary)
      FROM Employee e2
      WHERE e2.salary > e1.salary
    )
  );
END
```

## Solution 3: Using Window Function (MySQL 8.0+)

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  RETURN (
    SELECT DISTINCT salary
    FROM (
      SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) as rnk
      FROM Employee
    ) ranked
    WHERE rnk = N
  );
END
```

## Solution 4: Using CTE (MySQL 8.0+)

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  RETURN (
    WITH RankedSalaries AS (
      SELECT DISTINCT salary,
             DENSE_RANK() OVER (ORDER BY salary DESC) as rnk
      FROM Employee
    )
    SELECT salary
    FROM RankedSalaries
    WHERE rnk = N
  );
END
```

## Solution 5: Alternative LIMIT Approach

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  RETURN (
    SELECT (
      SELECT DISTINCT salary
      FROM Employee
      ORDER BY salary DESC
      LIMIT N, 1
    ) AS getNthHighestSalary
  );
END
-- Note: LIMIT N, 1 means skip N rows, return 1
-- But we want to skip N-1, so this would return (N+1)th
-- This approach requires adjusting N
```

## Complexity Analysis

### LIMIT with OFFSET:
- **Time**: O(n log n) for sorting + O(k) for skipping
- **Space**: O(n) for sorting

### Subquery Method:
- **Time**: O(n²) - for each row, count rows with higher salary
- **Space**: O(1)

### Window Function:
- **Time**: O(n log n) for sorting
- **Space**: O(n) for ranking

## Understanding the Solutions

### Why DISTINCT?
```
Salaries: [300, 200, 200, 100]

With DISTINCT:
1st highest: 300
2nd highest: 200 (both 200s count as same rank)
3rd highest: 100

Without DISTINCT:
1st: 300, 2nd: 200, 3rd: 200, 4th: 100
```

### Why N = N - 1?
```
OFFSET works with 0-based index:
- OFFSET 0: skip 0 rows, get 1st row
- OFFSET 1: skip 1 row, get 2nd row
- OFFSET N-1: skip N-1 rows, get Nth row
```

### Why DENSE_RANK vs RANK vs ROW_NUMBER?
```
Salaries: [300, 200, 200, 100]

DENSE_RANK: 1, 2, 2, 3  (no gaps, ties get same rank)
RANK:       1, 2, 2, 4  (gaps after ties)
ROW_NUMBER: 1, 2, 3, 4  (always unique, ties arbitrary)

For "Nth highest", DENSE_RANK is correct.
```

## Key SQL Concepts

### Window Functions
```sql
-- DENSE_RANK() OVER (ORDER BY salary DESC)
-- Assigns ranks without gaps for ties
SELECT salary,
       DENSE_RANK() OVER (ORDER BY salary DESC) as rnk
FROM Employee;

-- Result:
-- salary | rnk
--   300  |  1
--   200  |  2
--   200  |  2
--   100  |  3
```

### LIMIT and OFFSET
```sql
-- LIMIT count OFFSET skip
SELECT * FROM table LIMIT 1 OFFSET 2;  -- Skip 2, get 1

-- Alternative syntax
SELECT * FROM table LIMIT 2, 1;  -- Same: skip 2, get 1
```

## Common Mistakes to Avoid

1. **Forgetting DISTINCT**: Same salary appears multiple times
2. **Off-by-one in OFFSET**: OFFSET is 0-based
3. **Not handling NULL**: When Nth salary doesn't exist
4. **Using RANK instead of DENSE_RANK**: RANK has gaps after ties
5. **MySQL version issues**: Window functions require 8.0+

## Edge Cases

1. **N > number of distinct salaries**: Return NULL
2. **N = 1**: Return highest salary
3. **All salaries same**: Only 1st highest exists
4. **Negative or 0 for N**: Should handle (though constraints say N >= 1)

## PostgreSQL Version

```sql
CREATE OR REPLACE FUNCTION getNthHighestSalary(N INT) 
RETURNS TABLE (salary INT) AS $$
BEGIN
  RETURN QUERY
  SELECT DISTINCT e.salary
  FROM Employee e
  ORDER BY e.salary DESC
  LIMIT 1 OFFSET N - 1;
END;
$$ LANGUAGE plpgsql;
```

## Related Problems

- [176. Second Highest Salary](https://leetcode.com/problems/second-highest-salary/)
- [184. Department Highest Salary](https://leetcode.com/problems/department-highest-salary/)
- [185. Department Top Three Salaries](https://leetcode.com/problems/department-top-three-salaries/)
- [196. Delete Duplicate Emails](https://leetcode.com/problems/delete-duplicate-emails/)

## Python Equivalent (for Understanding)

```python
def getNthHighestSalary(salaries, n):
    """
    Python equivalent to understand the logic.
    """
    # Get distinct salaries
    distinct_salaries = sorted(set(salaries), reverse=True)
    
    # Return Nth highest or None
    if n <= len(distinct_salaries):
        return distinct_salaries[n - 1]
    return None

# Example
salaries = [100, 200, 300, 200]
print(getNthHighestSalary(salaries, 2))  # Output: 200
```

