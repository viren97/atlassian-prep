# Design a Food Rating System

## Problem Information
- **Difficulty**: Medium
- **Frequency**: 43.1%
- **Acceptance Rate**: 44.9%
- **Topics**: Hash Table, Design, Heap (Priority Queue), Ordered Set
- **LeetCode Link**: https://leetcode.com/problems/design-a-food-rating-system

## Problem Description

Design a food rating system that can do the following:

- **Modify** the rating of a food item listed in the system.
- Return the highest-rated food item for a type of cuisine in the system.

Implement the `FoodRatings` class:

- `FoodRatings(String[] foods, String[] cuisines, int[] ratings)` Initializes the system. The food items are described by `foods`, `cuisines` and `ratings`, all of which have a length of `n`.
  - `foods[i]` is the name of the `i-th` food,
  - `cuisines[i]` is the type of cuisine of the `i-th` food, and
  - `ratings[i]` is the initial rating of the `i-th` food.
- `void changeRating(String food, int newRating)` Changes the rating of the food item with the name `food`.
- `String highestRated(String cuisine)` Returns the name of the food item that has the highest rating for the given type of `cuisine`. If there is a tie, return the item with the **lexicographically smaller** name.

Note that a string `x` is lexicographically smaller than string `y` if `x` comes before `y` in dictionary order.

## Examples

### Example 1:
```
Input
["FoodRatings", "highestRated", "highestRated", "changeRating", "highestRated", "changeRating", "highestRated"]
[[["kimchi", "miso", "sushi", "moussaka", "ramen", "bulgogi"], ["korean", "japanese", "japanese", "greek", "japanese", "korean"], [9, 12, 8, 15, 14, 7]], ["korean"], ["japanese"], ["sushi", 16], ["japanese"], ["ramen", 16], ["japanese"]]
Output
[null, "kimchi", "ramen", null, "sushi", null, "ramen"]

Explanation
FoodRatings foodRatings = new FoodRatings(["kimchi", "miso", "sushi", "moussaka", "ramen", "bulgogi"], ["korean", "japanese", "japanese", "greek", "japanese", "korean"], [9, 12, 8, 15, 14, 7]);
foodRatings.highestRated("korean"); // return "kimchi"
                                    // "kimchi" is the highest rated korean food with a rating of 9.
foodRatings.highestRated("japanese"); // return "ramen"
                                      // "ramen" is the highest rated japanese food with a rating of 14.
foodRatings.changeRating("sushi", 16); // "sushi" now has a rating of 16.
foodRatings.highestRated("japanese"); // return "sushi"
                                      // "sushi" is the highest rated japanese food with a rating of 16.
foodRatings.changeRating("ramen", 16); // "ramen" now has a rating of 16.
foodRatings.highestRated("japanese"); // return "ramen"
                                      // Both "sushi" and "ramen" have a rating of 16.
                                      // However, "ramen" is lexicographically smaller than "sushi".
```

## Constraints

- `1 <= n <= 2 * 10^4`
- `n == foods.length == cuisines.length == ratings.length`
- `1 <= foods[i].length, cuisines[i].length <= 10`
- `foods[i]`, `cuisines[i]` consist of lowercase English letters.
- `1 <= ratings[i] <= 10^8`
- All the strings in `foods` are **distinct**.
- `food` will be the name of a food item in the system across all calls to `changeRating`.
- `cuisine` will be a type of cuisine of **at least one** food item in the system across all calls to `highestRated`.
- At most `2 * 10^4` calls **in total** will be made to `changeRating` and `highestRated`.

## Approach

### Data Structures Needed:
1. **food_to_cuisine**: Map food name → cuisine (to find cuisine when rating changes)
2. **food_to_rating**: Map food name → current rating
3. **cuisine_to_foods**: Map cuisine → sorted structure of (rating, food_name)

### Key Challenge:
- Need to efficiently find max-rated food per cuisine
- Need to handle rating updates
- Tie-breaker: lexicographically smaller name

### Options:
1. **SortedList/TreeSet**: O(log n) for insert/delete/find max
2. **Heap with Lazy Deletion**: O(log n) insert, O(1)* find max

## Solution 1: Using SortedList (sortedcontainers)

```python
from sortedcontainers import SortedList
from typing import List

class FoodRatings:
    def __init__(self, foods: List[str], cuisines: List[str], ratings: List[int]):
        self.food_to_cuisine = {}  # food -> cuisine
        self.food_to_rating = {}   # food -> rating
        self.cuisine_to_foods = {} # cuisine -> SortedList of (-rating, food)
        
        for food, cuisine, rating in zip(foods, cuisines, ratings):
            self.food_to_cuisine[food] = cuisine
            self.food_to_rating[food] = rating
            
            if cuisine not in self.cuisine_to_foods:
                self.cuisine_to_foods[cuisine] = SortedList()
            
            # Store as (-rating, food) so highest rating comes first
            # Negative rating for descending order, food name for ascending (tie-breaker)
            self.cuisine_to_foods[cuisine].add((-rating, food))
    
    def changeRating(self, food: str, newRating: int) -> None:
        cuisine = self.food_to_cuisine[food]
        old_rating = self.food_to_rating[food]
        
        # Remove old entry
        self.cuisine_to_foods[cuisine].remove((-old_rating, food))
        
        # Add new entry
        self.food_to_rating[food] = newRating
        self.cuisine_to_foods[cuisine].add((-newRating, food))
    
    def highestRated(self, cuisine: str) -> str:
        # First element has highest rating (most negative) and smallest name (tie-breaker)
        return self.cuisine_to_foods[cuisine][0][1]
```

## Solution 2: Using Heap with Lazy Deletion

```python
import heapq
from typing import List

class FoodRatings:
    def __init__(self, foods: List[str], cuisines: List[str], ratings: List[int]):
        self.food_to_cuisine = {}
        self.food_to_rating = {}
        self.cuisine_to_heap = {}  # cuisine -> heap of (-rating, food)
        
        for food, cuisine, rating in zip(foods, cuisines, ratings):
            self.food_to_cuisine[food] = cuisine
            self.food_to_rating[food] = rating
            
            if cuisine not in self.cuisine_to_heap:
                self.cuisine_to_heap[cuisine] = []
            
            heapq.heappush(self.cuisine_to_heap[cuisine], (-rating, food))
    
    def changeRating(self, food: str, newRating: int) -> None:
        cuisine = self.food_to_cuisine[food]
        self.food_to_rating[food] = newRating
        
        # Lazy deletion: just add new entry, old one will be ignored later
        heapq.heappush(self.cuisine_to_heap[cuisine], (-newRating, food))
    
    def highestRated(self, cuisine: str) -> str:
        heap = self.cuisine_to_heap[cuisine]
        
        # Pop stale entries (lazy deletion)
        while heap:
            neg_rating, food = heap[0]
            # Check if this entry is still valid
            if self.food_to_rating[food] == -neg_rating:
                return food
            # Stale entry, remove it
            heapq.heappop(heap)
        
        return ""  # Should never reach here
```

## Solution 3: Using Dictionary and Linear Search (Simple but Slow)

```python
from typing import List

class FoodRatings:
    def __init__(self, foods: List[str], cuisines: List[str], ratings: List[int]):
        self.food_to_cuisine = {}
        self.food_to_rating = {}
        self.cuisine_to_foods = {}  # cuisine -> set of food names
        
        for food, cuisine, rating in zip(foods, cuisines, ratings):
            self.food_to_cuisine[food] = cuisine
            self.food_to_rating[food] = rating
            
            if cuisine not in self.cuisine_to_foods:
                self.cuisine_to_foods[cuisine] = set()
            self.cuisine_to_foods[cuisine].add(food)
    
    def changeRating(self, food: str, newRating: int) -> None:
        self.food_to_rating[food] = newRating
    
    def highestRated(self, cuisine: str) -> str:
        foods = self.cuisine_to_foods[cuisine]
        
        # Find max by (rating DESC, name ASC)
        best_food = None
        best_rating = -1
        
        for food in foods:
            rating = self.food_to_rating[food]
            if rating > best_rating or (rating == best_rating and food < best_food):
                best_rating = rating
                best_food = food
        
        return best_food
```

## Complexity Analysis

### SortedList Solution:
- **\_\_init\_\_**: O(n log n) - n insertions
- **changeRating**: O(log n) - remove + insert
- **highestRated**: O(1) - access first element

### Heap with Lazy Deletion:
- **\_\_init\_\_**: O(n log n)
- **changeRating**: O(log n) - heap push
- **highestRated**: O(k log n) amortized, where k = stale entries popped

### Space Complexity: O(n)
- All solutions store n food items
- Heap solution may have extra stale entries

## Key Patterns & Techniques

1. **Multiple Hash Maps**: Track different relationships between entities
2. **SortedList/TreeSet**: Maintain sorted order with efficient updates
3. **Lazy Deletion**: Defer deletion until query time
4. **Negative Rating Trick**: Use negative values for max-heap behavior

## Sorting Key Explanation

```python
# We want: highest rating first, then lexicographically smallest name

# Using tuple (-rating, name):
# - Negative rating: smaller (more negative) = higher original rating
# - Name: smaller string wins tie-breaker

# Example:
# ("sushi", 16) → (-16, "sushi")
# ("ramen", 16) → (-16, "ramen")
# Sorted: [(-16, "ramen"), (-16, "sushi")]  # ramen < sushi alphabetically
```

## Common Mistakes to Avoid

1. **Forgetting tie-breaker**: Must return lexicographically smaller name
2. **Not updating all data structures**: When rating changes, update everything
3. **Wrong heap order**: Use negative rating for max-heap behavior
4. **Stale entries in heap**: Must validate entries in lazy deletion approach

## Trade-offs

| Approach | changeRating | highestRated | Space |
|----------|--------------|--------------|-------|
| SortedList | O(log n) | O(1) | O(n) |
| Heap + Lazy | O(log n) | O(k log n)* | O(n+m) |
| Linear Search | O(1) | O(n) | O(n) |

*k = number of stale entries for that cuisine

## Related Problems

- [355. Design Twitter](https://leetcode.com/problems/design-twitter/)
- [1244. Design A Leaderboard](https://leetcode.com/problems/design-a-leaderboard/)
- [2034. Stock Price Fluctuation](https://leetcode.com/problems/stock-price-fluctuation/)

