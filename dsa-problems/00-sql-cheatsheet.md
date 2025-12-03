# SQL Cheatsheet - Interview Ready

## Basic Queries

### SELECT

```sql
-- Basic select
SELECT column1, column2 FROM table_name;

-- All columns
SELECT * FROM employees;

-- With alias
SELECT first_name AS name, salary AS pay FROM employees;

-- Distinct values
SELECT DISTINCT department FROM employees;

-- Limit results
SELECT * FROM employees LIMIT 10;
SELECT * FROM employees LIMIT 10 OFFSET 20;  -- Skip first 20

-- Ordering
SELECT * FROM employees ORDER BY salary DESC;
SELECT * FROM employees ORDER BY department ASC, salary DESC;

-- NULL handling
SELECT * FROM employees ORDER BY commission NULLS LAST;
SELECT * FROM employees ORDER BY commission NULLS FIRST;
```

### WHERE Clause

```sql
-- Comparison operators
SELECT * FROM employees WHERE salary > 50000;
SELECT * FROM employees WHERE department = 'Engineering';
SELECT * FROM employees WHERE hire_date >= '2020-01-01';

-- Multiple conditions
SELECT * FROM employees WHERE salary > 50000 AND department = 'Engineering';
SELECT * FROM employees WHERE salary > 100000 OR title = 'Manager';
SELECT * FROM employees WHERE NOT department = 'HR';

-- IN / NOT IN
SELECT * FROM employees WHERE department IN ('Engineering', 'Product', 'Design');
SELECT * FROM employees WHERE id NOT IN (SELECT manager_id FROM employees);

-- BETWEEN
SELECT * FROM employees WHERE salary BETWEEN 50000 AND 100000;
SELECT * FROM employees WHERE hire_date BETWEEN '2020-01-01' AND '2020-12-31';

-- LIKE (pattern matching)
SELECT * FROM employees WHERE name LIKE 'John%';     -- Starts with John
SELECT * FROM employees WHERE name LIKE '%son';      -- Ends with son
SELECT * FROM employees WHERE name LIKE '%smith%';   -- Contains smith
SELECT * FROM employees WHERE name LIKE 'J_hn';      -- J + any char + hn

-- NULL checks
SELECT * FROM employees WHERE manager_id IS NULL;
SELECT * FROM employees WHERE commission IS NOT NULL;

-- COALESCE (first non-null value)
SELECT name, COALESCE(commission, 0) AS commission FROM employees;
```

---

## JOINs

### Types of JOINs

```sql
-- INNER JOIN (only matching rows)
SELECT e.name, d.department_name
FROM employees e
INNER JOIN departments d ON e.department_id = d.id;

-- LEFT JOIN (all from left, matching from right)
SELECT e.name, d.department_name
FROM employees e
LEFT JOIN departments d ON e.department_id = d.id;

-- RIGHT JOIN (all from right, matching from left)
SELECT e.name, d.department_name
FROM employees e
RIGHT JOIN departments d ON e.department_id = d.id;

-- FULL OUTER JOIN (all from both)
SELECT e.name, d.department_name
FROM employees e
FULL OUTER JOIN departments d ON e.department_id = d.id;

-- CROSS JOIN (cartesian product)
SELECT e.name, p.project_name
FROM employees e
CROSS JOIN projects p;

-- Self JOIN
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

### Multiple JOINs

```sql
SELECT 
    e.name,
    d.department_name,
    p.project_name
FROM employees e
JOIN departments d ON e.department_id = d.id
JOIN employee_projects ep ON e.id = ep.employee_id
JOIN projects p ON ep.project_id = p.id;
```

---

## Aggregations

### Basic Aggregates

```sql
-- COUNT
SELECT COUNT(*) FROM employees;                    -- All rows
SELECT COUNT(commission) FROM employees;           -- Non-null values
SELECT COUNT(DISTINCT department) FROM employees;  -- Unique values

-- SUM, AVG, MIN, MAX
SELECT SUM(salary) FROM employees;
SELECT AVG(salary) FROM employees;
SELECT MIN(salary), MAX(salary) FROM employees;

-- With rounding
SELECT ROUND(AVG(salary), 2) AS avg_salary FROM employees;
```

### GROUP BY

```sql
-- Basic grouping
SELECT department, COUNT(*) AS employee_count
FROM employees
GROUP BY department;

-- Multiple columns
SELECT department, title, AVG(salary) AS avg_salary
FROM employees
GROUP BY department, title;

-- With ORDER BY
SELECT department, SUM(salary) AS total_salary
FROM employees
GROUP BY department
ORDER BY total_salary DESC;
```

### HAVING (Filter groups)

```sql
-- Departments with more than 10 employees
SELECT department, COUNT(*) AS count
FROM employees
GROUP BY department
HAVING COUNT(*) > 10;

-- Departments with average salary > 60000
SELECT department, AVG(salary) AS avg_salary
FROM employees
GROUP BY department
HAVING AVG(salary) > 60000;

-- Combine WHERE and HAVING
SELECT department, AVG(salary) AS avg_salary
FROM employees
WHERE hire_date >= '2020-01-01'
GROUP BY department
HAVING AVG(salary) > 60000;
```

**Execution Order**: FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY

---

## Window Functions (VERY IMPORTANT!)

### ROW_NUMBER, RANK, DENSE_RANK

```sql
-- ROW_NUMBER: unique sequential number
SELECT 
    name,
    department,
    salary,
    ROW_NUMBER() OVER (ORDER BY salary DESC) AS row_num
FROM employees;

-- RANK: same rank for ties, skips numbers
-- Salaries: 100, 100, 90 → Ranks: 1, 1, 3
SELECT 
    name,
    salary,
    RANK() OVER (ORDER BY salary DESC) AS rank
FROM employees;

-- DENSE_RANK: same rank for ties, no skips
-- Salaries: 100, 100, 90 → Ranks: 1, 1, 2
SELECT 
    name,
    salary,
    DENSE_RANK() OVER (ORDER BY salary DESC) AS dense_rank
FROM employees;

-- PARTITION BY (rank within groups)
SELECT 
    name,
    department,
    salary,
    RANK() OVER (PARTITION BY department ORDER BY salary DESC) AS dept_rank
FROM employees;
```

### Nth Highest Salary (Common Interview Question!)

```sql
-- Second highest salary
SELECT DISTINCT salary
FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;

-- Using window function
SELECT salary FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rank
    FROM employees
) ranked
WHERE rank = 2;

-- Nth highest (generic)
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
    SET N = N - 1;
    RETURN (
        SELECT DISTINCT salary
        FROM employees
        ORDER BY salary DESC
        LIMIT 1 OFFSET N
    );
END;
```

### Running Totals & Moving Averages

```sql
-- Running total
SELECT 
    date,
    amount,
    SUM(amount) OVER (ORDER BY date) AS running_total
FROM transactions;

-- Running total partitioned
SELECT 
    date,
    category,
    amount,
    SUM(amount) OVER (PARTITION BY category ORDER BY date) AS category_running_total
FROM transactions;

-- Moving average (last 3 rows)
SELECT 
    date,
    amount,
    AVG(amount) OVER (ORDER BY date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg
FROM transactions;

-- Moving average (last 7 days)
SELECT 
    date,
    amount,
    AVG(amount) OVER (ORDER BY date RANGE BETWEEN INTERVAL 7 DAY PRECEDING AND CURRENT ROW) AS weekly_avg
FROM transactions;
```

### LAG and LEAD

```sql
-- LAG: access previous row
SELECT 
    date,
    price,
    LAG(price, 1) OVER (ORDER BY date) AS prev_price,
    price - LAG(price, 1) OVER (ORDER BY date) AS price_change
FROM stock_prices;

-- LEAD: access next row
SELECT 
    date,
    price,
    LEAD(price, 1) OVER (ORDER BY date) AS next_price
FROM stock_prices;

-- With default value
SELECT 
    date,
    price,
    LAG(price, 1, 0) OVER (ORDER BY date) AS prev_price
FROM stock_prices;
```

### FIRST_VALUE, LAST_VALUE, NTH_VALUE

```sql
-- First and last salary in department
SELECT 
    name,
    department,
    salary,
    FIRST_VALUE(salary) OVER (PARTITION BY department ORDER BY salary) AS min_salary,
    LAST_VALUE(salary) OVER (
        PARTITION BY department 
        ORDER BY salary 
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS max_salary
FROM employees;
```

---

## Subqueries

### Scalar Subquery (returns single value)

```sql
-- Employees earning more than average
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);

-- With the max salary
SELECT name, salary
FROM employees
WHERE salary = (SELECT MAX(salary) FROM employees);
```

### Table Subquery

```sql
-- In FROM clause
SELECT dept, avg_salary
FROM (
    SELECT department AS dept, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
) dept_stats
WHERE avg_salary > 60000;
```

### Correlated Subquery (references outer query)

```sql
-- Employees earning more than their department average
SELECT name, salary, department
FROM employees e
WHERE salary > (
    SELECT AVG(salary)
    FROM employees
    WHERE department = e.department
);

-- Department with highest average salary
SELECT department
FROM employees e
GROUP BY department
HAVING AVG(salary) = (
    SELECT MAX(avg_sal)
    FROM (
        SELECT AVG(salary) AS avg_sal
        FROM employees
        GROUP BY department
    ) t
);
```

### EXISTS / NOT EXISTS

```sql
-- Employees who are managers
SELECT name
FROM employees e
WHERE EXISTS (
    SELECT 1 FROM employees
    WHERE manager_id = e.id
);

-- Employees who are not managers
SELECT name
FROM employees e
WHERE NOT EXISTS (
    SELECT 1 FROM employees
    WHERE manager_id = e.id
);
```

---

## Common Table Expressions (CTE)

```sql
-- Basic CTE
WITH high_earners AS (
    SELECT * FROM employees WHERE salary > 100000
)
SELECT department, COUNT(*) AS count
FROM high_earners
GROUP BY department;

-- Multiple CTEs
WITH 
dept_avg AS (
    SELECT department, AVG(salary) AS avg_salary
    FROM employees
    GROUP BY department
),
high_avg_depts AS (
    SELECT department FROM dept_avg WHERE avg_salary > 70000
)
SELECT e.*
FROM employees e
JOIN high_avg_depts h ON e.department = h.department;

-- Recursive CTE (for hierarchies)
WITH RECURSIVE employee_hierarchy AS (
    -- Base case: top-level managers
    SELECT id, name, manager_id, 1 AS level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- Recursive case
    SELECT e.id, e.name, e.manager_id, h.level + 1
    FROM employees e
    JOIN employee_hierarchy h ON e.manager_id = h.id
)
SELECT * FROM employee_hierarchy ORDER BY level, name;
```

---

## Set Operations

```sql
-- UNION (distinct values)
SELECT name FROM employees
UNION
SELECT name FROM contractors;

-- UNION ALL (include duplicates)
SELECT name FROM employees
UNION ALL
SELECT name FROM contractors;

-- INTERSECT (common to both)
SELECT department FROM employees
INTERSECT
SELECT department FROM budgets;

-- EXCEPT / MINUS (in first but not second)
SELECT department FROM employees
EXCEPT
SELECT department FROM contractors;
```

---

## Data Modification

```sql
-- INSERT
INSERT INTO employees (name, department, salary)
VALUES ('John Doe', 'Engineering', 75000);

-- INSERT multiple rows
INSERT INTO employees (name, department, salary) VALUES
    ('Jane Doe', 'Product', 80000),
    ('Bob Smith', 'Design', 70000);

-- INSERT from SELECT
INSERT INTO employee_archive
SELECT * FROM employees WHERE hire_date < '2020-01-01';

-- UPDATE
UPDATE employees
SET salary = salary * 1.1
WHERE department = 'Engineering';

-- UPDATE with JOIN
UPDATE employees e
SET salary = salary * 1.1
FROM departments d
WHERE e.department_id = d.id AND d.name = 'Engineering';

-- DELETE
DELETE FROM employees WHERE id = 123;

-- DELETE with subquery
DELETE FROM employees
WHERE department_id IN (
    SELECT id FROM departments WHERE is_active = false
);

-- UPSERT (INSERT or UPDATE)
INSERT INTO employees (id, name, salary)
VALUES (1, 'John', 75000)
ON CONFLICT (id) DO UPDATE
SET salary = EXCLUDED.salary;
```

---

## Indexing & Performance

### Index Types

```sql
-- B-tree index (default)
CREATE INDEX idx_employees_department ON employees(department);

-- Composite index
CREATE INDEX idx_employees_dept_salary ON employees(department, salary);

-- Unique index
CREATE UNIQUE INDEX idx_employees_email ON employees(email);

-- Partial index
CREATE INDEX idx_active_employees ON employees(department)
WHERE is_active = true;

-- Drop index
DROP INDEX idx_employees_department;
```

### Query Optimization Tips

```sql
-- Use EXPLAIN to analyze query
EXPLAIN SELECT * FROM employees WHERE department = 'Engineering';
EXPLAIN ANALYZE SELECT * FROM employees WHERE department = 'Engineering';

-- Avoid SELECT *
-- BAD
SELECT * FROM employees;
-- GOOD
SELECT id, name, department FROM employees;

-- Use indexes for WHERE, JOIN, ORDER BY columns
-- Index on (department) helps:
WHERE department = 'Engineering'
ORDER BY department
JOIN ... ON e.department = d.department

-- Composite index order matters
-- Index on (department, salary) helps:
WHERE department = 'Eng' AND salary > 50000  ✓
WHERE department = 'Eng'                      ✓
WHERE salary > 50000                          ✗ (need salary-first index)

-- Avoid functions on indexed columns
-- BAD (can't use index)
WHERE YEAR(hire_date) = 2020
-- GOOD
WHERE hire_date >= '2020-01-01' AND hire_date < '2021-01-01'

-- Use EXISTS instead of IN for large subqueries
-- Slower
WHERE id IN (SELECT employee_id FROM large_table)
-- Faster
WHERE EXISTS (SELECT 1 FROM large_table WHERE employee_id = employees.id)

-- Avoid N+1 queries - use JOINs
```

---

## Common Interview Queries

### 1. Second/Nth Highest Salary

```sql
-- Using OFFSET
SELECT DISTINCT salary FROM employees ORDER BY salary DESC LIMIT 1 OFFSET 1;

-- Using window function
SELECT salary FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rank
    FROM employees
) t WHERE rank = 2;

-- Handle null case
SELECT MAX(salary) AS SecondHighestSalary
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);
```

### 2. Find Duplicates

```sql
SELECT email, COUNT(*) AS count
FROM employees
GROUP BY email
HAVING COUNT(*) > 1;
```

### 3. Find Employees Without Manager

```sql
SELECT e.name
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id
WHERE m.id IS NULL AND e.manager_id IS NOT NULL;
```

### 4. Department with Highest Average Salary

```sql
SELECT department, AVG(salary) AS avg_salary
FROM employees
GROUP BY department
ORDER BY avg_salary DESC
LIMIT 1;
```

### 5. Employees Earning More Than Manager

```sql
SELECT e.name AS Employee
FROM employees e
JOIN employees m ON e.manager_id = m.id
WHERE e.salary > m.salary;
```

### 6. Consecutive Days/Numbers

```sql
-- Find 3+ consecutive login days
SELECT DISTINCT l1.user_id
FROM logins l1
JOIN logins l2 ON l1.user_id = l2.user_id AND l2.login_date = l1.login_date + 1
JOIN logins l3 ON l1.user_id = l3.user_id AND l3.login_date = l1.login_date + 2;

-- Using window functions
WITH numbered AS (
    SELECT 
        user_id,
        login_date,
        login_date - ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) * INTERVAL '1 day' AS grp
    FROM logins
)
SELECT user_id, COUNT(*) AS consecutive_days
FROM numbered
GROUP BY user_id, grp
HAVING COUNT(*) >= 3;
```

### 7. Running Total

```sql
SELECT 
    date,
    amount,
    SUM(amount) OVER (ORDER BY date) AS running_total
FROM transactions;
```

### 8. Year-over-Year Growth

```sql
WITH yearly AS (
    SELECT 
        EXTRACT(YEAR FROM date) AS year,
        SUM(revenue) AS total_revenue
    FROM sales
    GROUP BY EXTRACT(YEAR FROM date)
)
SELECT 
    year,
    total_revenue,
    LAG(total_revenue) OVER (ORDER BY year) AS prev_year,
    ROUND(100.0 * (total_revenue - LAG(total_revenue) OVER (ORDER BY year)) 
        / LAG(total_revenue) OVER (ORDER BY year), 2) AS yoy_growth_pct
FROM yearly;
```

### 9. Pivot Table (Rows to Columns)

```sql
SELECT 
    product,
    SUM(CASE WHEN month = 'Jan' THEN revenue ELSE 0 END) AS Jan,
    SUM(CASE WHEN month = 'Feb' THEN revenue ELSE 0 END) AS Feb,
    SUM(CASE WHEN month = 'Mar' THEN revenue ELSE 0 END) AS Mar
FROM sales
GROUP BY product;
```

### 10. Find Gaps in Sequence

```sql
SELECT 
    id + 1 AS gap_start,
    next_id - 1 AS gap_end
FROM (
    SELECT id, LEAD(id) OVER (ORDER BY id) AS next_id
    FROM numbers
) t
WHERE next_id - id > 1;
```

---

## Quick Reference

| Task | SQL |
|------|-----|
| Remove duplicates | `SELECT DISTINCT` or `GROUP BY` |
| Top N per group | `ROW_NUMBER() OVER (PARTITION BY ... ORDER BY ...)` |
| Running total | `SUM() OVER (ORDER BY ...)` |
| Previous row value | `LAG()` |
| Next row value | `LEAD()` |
| Rank with ties | `RANK()` or `DENSE_RANK()` |
| Filter groups | `HAVING` (after GROUP BY) |
| Filter rows | `WHERE` (before GROUP BY) |
| Combine results | `UNION` / `UNION ALL` |
| Default for NULL | `COALESCE(col, default)` |
| Conditional value | `CASE WHEN ... THEN ... END` |

