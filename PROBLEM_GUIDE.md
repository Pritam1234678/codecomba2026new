# CodeCombat 2026 — Problem Addition Guide

## Overview

This guide explains the exact format and steps to add a new coding problem to the platform. Give this file to any AI and it can generate problems in the correct format.

---

## Architecture

- **Database**: PostgreSQL
- **Tables**: `problems` (problem metadata) + `code_snippets` (harnesses per language)
- **Judge Engine**: ProcessBuilder (local execution, no Docker)
- **Languages supported**: JAVA, CPP, C, PYTHON, JAVASCRIPT
- **Test case format**: Harness prints `TC:N:PASS` or `TC:N:FAIL:input=...:expected=...:got=...` to stdout
- **No stdin**: Harnesses have test cases hardcoded — user code is injected between markers

---

## Step 1: Create the Problem (via API or DB)

### API Endpoint
```
POST /api/admin/problems/contest/{contestId}
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json
```

### Request Body
```json
{
  "title": "Two Sum",
  "description": "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.",
  "inputFormat": "First line contains integer n.\nSecond line contains n integers.\nThird line contains target integer.",
  "outputFormat": "Print indices of two numbers whose sum equals target.",
  "constraints": "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\nOnly one valid answer exists.",
  "timeLimit": 5.0,
  "memoryLimit": 256,
  "level": "MEDIUM",
  "active": true,
  "example1": "Input: nums = [2,7,11,15], target = 9\nOutput: [0,1]\nExplanation: nums[0] + nums[1] = 9",
  "example2": "Input: nums = [3,2,4], target = 6\nOutput: [1,2]\nExplanation: nums[1] + nums[2] = 6",
  "example3": "Input: nums = [3,3], target = 6\nOutput: [0,1]\nExplanation: nums[0] + nums[1] = 6"
}
```

### Fields Explanation

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | Yes | Problem title |
| description | Text | Yes | Problem statement |
| inputFormat | Text | No | How input is formatted (shown to user) |
| outputFormat | Text | No | Expected output format |
| constraints | Text | No | Constraints (newline separated) |
| timeLimit | Double | Yes | Time limit in seconds (e.g., 5.0) |
| memoryLimit | Integer | Yes | Memory limit in MB (e.g., 256) |
| level | String | Yes | `EASY`, `MEDIUM`, or `HARD` |
| active | Boolean | Yes | `true` to make visible |
| example1 | Text | No | First example (shown to user) |
| example2 | Text | No | Second example |
| example3 | Text | No | Third example |

### Direct SQL (alternative)
```sql
INSERT INTO problems (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, active, contest_id, example1, example2, example3)
VALUES (
  'Two Sum',
  'Given an array of integers...',
  'First line contains...',
  'Print indices...',
  '2 <= nums.length <= 10^4',
  5.0,
  256,
  'MEDIUM',
  true,
  1,  -- contest_id
  'Input: nums = [2,7,11,15]...',
  'Input: nums = [3,2,4]...',
  NULL
);
```

---

## Step 2: Create Code Harnesses (5 languages)

### API Endpoint
```
POST /api/problems/{problemId}/snippets/bulk
Authorization: Bearer <admin_jwt_token>
Content-Type: application/json
```

### Request Body (array of 5 snippets)
```json
[
  { "language": "JAVA", "solutionTemplate": "..." },
  { "language": "CPP", "solutionTemplate": "..." },
  { "language": "C", "solutionTemplate": "..." },
  { "language": "PYTHON", "solutionTemplate": "..." },
  { "language": "JAVASCRIPT", "solutionTemplate": "..." }
]
```

### Direct SQL (alternative)
```sql
INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
VALUES (3, 'JAVA', '<full harness code>', NOW(), NOW());
```

---

## Step 3: Harness Format Rules (CRITICAL)

### Core Rules

1. **Single complete runnable file** — no external dependencies, no stdin reading
2. **Must contain markers**: `// USER_CODE_START` and `// USER_CODE_END` (or `# USER_CODE_START` / `# USER_CODE_END` for Python)
3. **User's code replaces** the section between markers on submission
4. **Test cases hardcoded** inside the harness — NO stdin/cin/scanf/input()/readFileSync
5. **Output format**: Each test prints exactly one line in format:
   - PASS: `TC:<number>:PASS` or `TC:<number>:PASS:hidden`
   - FAIL (visible): `TC:<number>:FAIL:input=<input_repr>:expected=<expected>:got=<actual>`
   - FAIL (hidden): `TC:<number>:FAIL:hidden`
6. **Minimum 8 visible + 2 hidden** test cases (10 total recommended)
7. **Hidden test cases** should test edge cases users can't see

### Output Format Details

```
TC:1:PASS                                    ← visible, passed
TC:2:FAIL:input=[4,2,0,3]:expected=4:got=0   ← visible, failed (shows debug info)
TC:9:PASS:hidden                             ← hidden, passed
TC:10:FAIL:hidden                            ← hidden, failed (no debug info shown)
```

**IMPORTANT**: The `input=`, `expected=`, `got=` parts are ONLY for visible failed test cases. Hidden test cases should NEVER expose input/expected/got.

---

## Harness Templates (All 5 Languages)

### Template: JAVA

```java
import java.util.*;

public class Main {

    // USER_CODE_START
    public static int solveProblem(int[] input) {
        // Write your solution here
        return 0;
    }
    // USER_CODE_END

    static void test(int[] input, int expected, int tc, boolean hidden) {
        int result = solveProblem(input);
        if (result == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else {
            if (hidden) {
                System.out.println("TC:" + tc + ":FAIL:hidden");
            } else {
                System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(input) + ":expected=" + expected + ":got=" + result);
            }
        }
    }

    public static void main(String[] args) {
        // Visible test cases (user can see input/expected/got on failure)
        test(new int[]{...}, expectedValue, 1, false);
        test(new int[]{...}, expectedValue, 2, false);
        // ... up to 8 visible

        // Hidden test cases (user only sees PASS/FAIL)
        test(new int[]{...}, expectedValue, 9, true);
        test(new int[]{...}, expectedValue, 10, true);
    }
}
```

### Template: CPP

```cpp
#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
int solveProblem(vector<int>& input) {
    // Write your solution here
    return 0;
}
// USER_CODE_END

void test(vector<int> input, int expected, int tc, bool hidden = false) {
    int result = solveProblem(input);
    if (result == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else {
        if (hidden) {
            cout << "TC:" << tc << ":FAIL:hidden\n";
        } else {
            cout << "TC:" << tc << ":FAIL:input=[";
            for (int i = 0; i < (int)input.size(); i++) { if (i) cout << ","; cout << input[i]; }
            cout << "]:expected=" << expected << ":got=" << result << "\n";
        }
    }
}

int main() {
    test({...}, expected, 1);
    test({...}, expected, 2);
    // ... visible tests

    test({...}, expected, 9, true);
    test({...}, expected, 10, true);
    return 0;
}
```

### Template: C

```c
#include <stdio.h>

// USER_CODE_START
int solveProblem(int* arr, int n) {
    // Write your solution here
    return 0;
}
// USER_CODE_END

void test(int* arr, int n, int expected, int tc, int hidden) {
    int result = solveProblem(arr, n);
    if (result == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) {
            printf("TC:%d:FAIL:hidden\n", tc);
        } else {
            printf("TC:%d:FAIL:input=[", tc);
            for (int i = 0; i < n; i++) { if (i) printf(","); printf("%d", arr[i]); }
            printf("]:expected=%d:got=%d\n", expected, result);
        }
    }
}

int main() {
    int h1[] = {...}; test(h1, sizeof(h1)/sizeof(int), expected, 1, 0);
    // ... visible

    int h9[] = {...}; test(h9, sizeof(h9)/sizeof(int), expected, 9, 1);
    // ... hidden
    return 0;
}
```

### Template: PYTHON

```python
# USER_CODE_START
def solve_problem(arr):
    # Write your solution here
    return 0
# USER_CODE_END

def test(arr, expected, tc, hidden=False):
    result = solve_problem(arr)
    if result == expected:
        suffix = ":hidden" if hidden else ""
        print(f"TC:{tc}:PASS{suffix}")
    else:
        if hidden:
            print(f"TC:{tc}:FAIL:hidden")
        else:
            print(f"TC:{tc}:FAIL:input={arr}:expected={expected}:got={result}")

# Visible
test([...], expected, 1)
test([...], expected, 2)

# Hidden
test([...], expected, 9, hidden=True)
test([...], expected, 10, hidden=True)
```

### Template: JAVASCRIPT

```javascript
// USER_CODE_START
function solveProblem(arr) {
    // Write your solution here
    return 0;
}
// USER_CODE_END

function test(arr, expected, tc, hidden = false) {
    const result = solveProblem(arr);
    if (result === expected) {
        const suffix = hidden ? ':hidden' : '';
        console.log(`TC:${tc}:PASS${suffix}`);
    } else {
        if (hidden) {
            console.log(`TC:${tc}:FAIL:hidden`);
        } else {
            console.log(`TC:${tc}:FAIL:input=[${arr}]:expected=${expected}:got=${result}`);
        }
    }
}

// Visible
test([...], expected, 1);
test([...], expected, 2);

// Hidden
test([...], expected, 9, true);
test([...], expected, 10, true);
```

---

## Step 4: Clear Cache After Adding

After inserting harnesses, clear the Redis/Valkey cache:

```bash
redis-cli DEL "snippet:<problemId>:JAVA" "snippet:<problemId>:CPP" "snippet:<problemId>:PYTHON" "snippet:<problemId>:JAVASCRIPT" "snippet:<problemId>:C" "snippets:user:<problemId>"
```

Or restart the backend (cache has 60-min TTL anyway).

---

## Complete Example: Adding "Trapping Rain Water" (Problem 2)

### SQL for problem:
```sql
INSERT INTO problems (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, active, contest_id, example1, example2, example3)
VALUES (
  'Trapping Rain Water',
  'Given n non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.',
  'First line contains integer n (number of bars).\nSecond line contains n space-separated non-negative integers representing the height of each bar.',
  'Print a single integer — the total units of water that can be trapped.',
  '1 <= n <= 2 * 10^4\n0 <= height[i] <= 10^5',
  5.0,
  256,
  'MEDIUM',
  true,
  1,
  'Input:\n12\n0 1 0 2 1 0 1 3 2 1 2 1\n\nOutput:\n6\n\nExplanation: The elevation map [0,1,0,2,1,0,1,3,2,1,2,1] traps 6 units of water.',
  'Input:\n4\n4 2 0 3\n\nOutput:\n4',
  'Input:\n1\n5\n\nOutput:\n0'
);
```

### Java harness for it:
```sql
INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
VALUES (2, 'JAVA', 'import java.util.*;

public class Main {

    // USER_CODE_START
    public static int trap(int[] height) {
        // Write your solution here
        return 0;
    }
    // USER_CODE_END

    static void test(int[] h, int expected, int tc, boolean hidden) {
        int result = trap(h);
        if (result == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else {
            if (hidden) {
                System.out.println("TC:" + tc + ":FAIL:hidden");
            } else {
                System.out.println("TC:" + tc + ":FAIL:input=" + java.util.Arrays.toString(h) + ":expected=" + expected + ":got=" + result);
            }
        }
    }

    public static void main(String[] args) {
        test(new int[]{0,1,0,2,1,0,1,3,2,1,2,1}, 6, 1, false);
        test(new int[]{4,2,0,3}, 4, 2, false);
        test(new int[]{1}, 0, 3, false);
        test(new int[]{3,0,2,0,4}, 7, 4, false);
        test(new int[]{0,0,0,0}, 0, 5, false);
        test(new int[]{5,4,3,2,1}, 0, 6, false);
        test(new int[]{1,2,3,4,5}, 0, 7, false);
        test(new int[]{2,0,2}, 2, 8, false);
        test(new int[]{0,1,0,2,1,0,1,3,2,1,2,1}, 6, 9, true);
        test(new int[]{100,0,100}, 100, 10, true);
    }
}', NOW(), NOW());
```

---

## Common Pitfalls (DO NOT DO)

| ❌ Wrong | ✅ Correct |
|----------|-----------|
| `Scanner sc = new Scanner(System.in)` | Hardcode test cases directly |
| `cin >> n` | Hardcode in code |
| `input()` in Python | Hardcode arrays |
| `readFileSync('/dev/stdin')` | Hardcode |
| `TC:1:PASS:hidden:input=[...]` | Hidden should NEVER show input |
| `score/100` in output | Only `TC:N:STATUS` format |
| Missing `// USER_CODE_END` marker | Always include both markers |
| Using `System.exit(0)` | Let main() return naturally |
| Test cases that depend on order of output | Use deterministic expected values |

---

## Function Signature Patterns

### For problems returning a single value (int, string):
```java
// USER_CODE_START
public static int solve(int[] arr) { return 0; }
// USER_CODE_END
```

### For problems returning an array:
```java
// USER_CODE_START
public int[] twoSum(int[] nums, int target) { return new int[]{}; }
// USER_CODE_END
```
For array returns, compare using `Arrays.equals()` (Java), `==` operator (Python/CPP), or element-wise comparison.

### For problems with multiple parameters:
```java
// USER_CODE_START
public static int solve(int[] arr, int k) { return 0; }
// USER_CODE_END
```

---

## Checklist Before Publishing

- [ ] Problem created in `problems` table with correct `contest_id`
- [ ] All 5 language harnesses added to `code_snippets`
- [ ] Each harness has `// USER_CODE_START` and `// USER_CODE_END` markers
- [ ] No stdin reading anywhere (Scanner, cin, scanf, input(), readFileSync)
- [ ] At least 8 visible + 2 hidden test cases
- [ ] Expected values verified manually (run the correct solution against all TCs)
- [ ] Edge cases covered (empty array, single element, all same, negative numbers)
- [ ] Redis cache cleared for the problem
- [ ] Backend restarted or cache TTL waited out (60 min)
- [ ] Test a submission from the frontend — verify PASS/FAIL output works
