# CodeCombat 2026 — Coding Agent Guide

You are a coding agent. Your task: read questions from the Excel sheet and insert them into the CodeCombat PostgreSQL database. No API calls. Direct DB inserts only.

---

## Step 1: Connect to Database

The PostgreSQL database runs on a remote VM. Connect via SSH tunnel or directly:

```bash
# SSH tunnel (if DB only allows localhost)
ssh -L 5432:localhost:5432 ubuntu@161.118.187.201 -i cc-vm_key.pem

# Then connect
PGPASSWORD=postgres psql -h localhost -U postgres -d codecombat
```

Or use Python:
```python
import psycopg2
conn = psycopg2.connect(
    host="161.118.187.201",  # or localhost if tunneled
    port=5432,
    dbname="codecombat",
    user="postgres",
    password="postgres"
)
```

---

## Step 2: Read the Excel Sheet

```python
import openpyxl
wb = openpyxl.load_workbook("Sheets/Deloitte_100_Coding_Questions.xlsx")
ws = wb.active

for row in range(2, ws.max_row + 1):
    name = ws.cell(row, 4).value   # Question Name
    diff = ws.cell(row, 5).value   # Difficulty
    topic = ws.cell(row, 2).value  # Topic
    if name and str(name).strip() != "None":
        # Generate problem JSON + harnesses for this question
        pass
```

---

## Step 3: Generate Problem JSON

For each question, create a JSON object with these fields. **You generate the content yourself** based on your knowledge of the problem. The JSON must have this exact structure:

```json
{
  "title": "Two Sum",
  "description": "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target. You may assume that each input has exactly one solution, and you may not use the same element twice. You can return the answer in any order.",
  "inputFormat": "First line contains integer n (size of array).\nSecond line contains n space-separated integers representing nums.\nThird line contains the integer target.",
  "outputFormat": "Print two space-separated integers — the indices i and j.",
  "constraints": "2 ≤ n ≤ 10^4\n-10^9 ≤ nums[i] ≤ 10^9\n-10^9 ≤ target ≤ 10^9\nOnly one valid answer exists.",
  "timeLimit": 5.0,
  "memoryLimit": 256,
  "level": "EASY",
  "active": true,
  "topics": "Array, Hash Table",
  "example1": "Input:\n4\n2 7 11 15\n9\n\nOutput:\n0 1\n\nExplanation: nums[0] + nums[1] = 2 + 7 = 9",
  "example2": "Input:\n3\n3 2 4\n6\n\nOutput:\n1 2",
  "example3": "Input:\n2\n3 3\n6\n\nOutput:\n0 1"
}
```

### Field Rules

| Field | Type | Rule |
|-------|------|------|
| title | String | Short, clear problem name |
| description | String | 60-120 words, self-contained. Pure-function statement |
| inputFormat | String | How user should read input (newline-separated) |
| outputFormat | String | What user should output |
| constraints | String | Newline-separated constraints with proper bounds |
| timeLimit | Double | EASY=3.0, MEDIUM=5.0, HARD=8.0 |
| memoryLimit | Integer | 128 to 512 (MB) |
| level | String | `EASY`, `MEDIUM`, or `HARD` |
| active | Boolean | Always `true` |
| topics | String | Comma-separated from this list ONLY: `Array, String, Two Pointers, Sliding Window, Binary Search, Hash Table, Linked List, Stack, Queue, Tree, Binary Tree, BST, Heap, Graph, Dynamic Programming, Greedy, Sorting, Bit Manipulation, Math, Recursion, Backtracking, DFS, BFS, Union Find, Trie, Divide and Conquer, Simulation` |
| example1-3 | String | Format: `Input:\n<data>\n\nOutput:\n<result>\n\nExplanation: <text>` (use \n for newlines) |

---

## Step 4: Insert Problem into DB

```sql
INSERT INTO problems (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, active, topics, example1, example2, example3)
VALUES (
  'Two Sum',
  'Given an array of integers nums...',
  'First line contains integer n...',
  'Print indices...',
  '2 ≤ n ≤ 10^4...',
  5.0,
  256,
  'EASY',
  true,
  'Array, Hash Table',
  'Input:\n4\n2 7 11 15\n9\n\nOutput:\n0 1',
  'Input:\n3\n3 2 4\n6\n\nOutput:\n1 2',
  'Input:\n2\n3 3\n6\n\nOutput:\n0 1'
)
RETURNING id;
```

Or in Python:
```python
cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    RETURNING id
""", (title, desc, infmt, outfmt, constraints, timeLimit, memLimit, level, True, topics, ex1, ex2, ex3))
problem_id = cur.fetchone()[0]
```

---

## Step 5: Generate Harnesses (All 5 Languages)

For each problem, generate **5 complete runnable harness files**. Each harness is ONE file that tests the candidate's code. **The candidate's code replaces only the section between USER_CODE markers.**

### CRITICAL RULES (apply to all 5 languages)

1. **NO stdin** — no Scanner, cin, scanf, input(), readFileSync anywhere in the code
2. **Hardcoded test data** — all test inputs inside the code itself
3. **Markers** — exactly `// USER_CODE_START` and `// USER_CODE_END` (Python uses `#`)
4. **LeetCode-style canvas between markers**:
   - JAVA, CPP, PYTHON → `class Solution` with ONE public method
   - JAVASCRIPT, C → a single top-level function
5. **Hidden driver outside markers** — calls `new Solution().method(...)` or `method(...)`
6. **6 test cases** — first 3 visible (hidden=false), last 3 hidden (hidden=true)
7. **Output format** — one line per test, never print anything else:
   - VISIBLE PASS → `TC:<n>:PASS`
   - HIDDEN PASS → `TC:<n>:PASS:hidden`
   - VISIBLE FAIL → `TC:<n>:FAIL:input=<repr>:expected=<val>:got=<val>`
   - HIDDEN FAIL → `TC:<n>:FAIL:hidden`
8. **Hidden tests NEVER print input/expected/got**
9. **try-catch around EVERY test() call** (Java, C++, Python, JS) — prevents a crash in one test from killing all remaining tests
10. Compare scalars with `==`. Compare arrays element-wise (`Arrays.equals` in Java, `==` in Python, `JSON.stringify` in JS)
11. Keep all printed values free of `:` colon characters (use `=` instead for separation)

### Test Case Design Rules

- 6 test cases minimum per harness
- First 3: small, visible — user can see input/expected on failure
- Last 3: edge cases, larger inputs, hidden
- Each test input must be VALID under the problem's constraints
- Edge cases: smallest input, largest, negative values, all-same, empty (if allowed)
- Expected values MUST be correct — compute them manually or run a correct solution

---

### JAVA Harness Template

```java
import java.util.*;

// USER_CODE_START
class Solution {
    public int[] twoSum(int[] nums, int target) {
        // Write your code here
        return new int[]{0, 0};
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int target, int[] expected, int tc, boolean hidden) {
        int[] got = new Solution().twoSum(nums, target);
        // Sort both for order-independent comparison
        java.util.Arrays.sort(got);
        java.util.Arrays.sort(expected);
        if (Arrays.equals(got, expected))
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(nums) + " target=" + target
                + ":expected=" + Arrays.toString(expected) + ":got=" + Arrays.toString(got));
    }

    public static void main(String[] a) {
        // Visible tests (user sees input/expected on failure)
        try { test(new int[]{2,7,11,15}, 9, new int[]{0,1}, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:input=[2,7,11,15] target=9:expected=[0,1]:got=ERR"); }
        try { test(new int[]{3,2,4}, 6, new int[]{1,2}, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:input=[3,2,4] target=6:expected=[1,2]:got=ERR"); }
        try { test(new int[]{3,3}, 6, new int[]{0,1}, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:input=[3,3] target=6:expected=[0,1]:got=ERR"); }
        // Hidden tests (user only sees PASS/FAIL)
        try { test(new int[]{1,2,3,4,5}, 9, new int[]{3,4}, 4, true); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{-1,-2,-3,-4,-5}, -8, new int[]{2,4}, 5, true); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{0,4,3,0}, 0, new int[]{0,3}, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}
```

**Java specifics:**
- `public class Main` is the entry point
- `class Solution` is package-private (no `public`), placed above `Main`
- Use `Arrays.equals()` for array comparison
- Use `Arrays.toString()` for printing arrays

---

### CPP Harness Template

```cpp
#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        // Write your code here
        return {0, 0};
    }
};
// USER_CODE_END

void test(vector<int> nums, int target, vector<int> expected, int tc, bool hidden = false) {
    Solution sol;
    vector<int> got = sol.twoSum(nums, target);
    sort(got.begin(), got.end());
    sort(expected.begin(), expected.end());
    if (got == expected)
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\n";
    else {
        cout << "TC:" << tc << ":FAIL:input=[";
        for (size_t i = 0; i < nums.size(); i++) { if (i) cout << ","; cout << nums[i]; }
        cout << "] target=" << target << ":expected=[";
        for (size_t i = 0; i < expected.size(); i++) { if (i) cout << ","; cout << expected[i]; }
        cout << "]:got=[";
        for (size_t i = 0; i < got.size(); i++) { if (i) cout << ","; cout << got[i]; }
        cout << "]\n";
    }
}

int main() {
    try { test({2,7,11,15}, 9, {0,1}, 1); }
    catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test({3,2,4}, 6, {1,2}, 2); }
    catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test({3,3}, 6, {0,1}, 3); }
    catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test({1,2,3,4,5}, 9, {3,4}, 4, true); }
    catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test({-1,-2,-3,-4,-5}, -8, {2,4}, 5, true); }
    catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test({0,4,3,0}, 0, {0,3}, 6, true); }
    catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    return 0;
}
```

**CPP specifics:**
- `#include <bits/stdc++.h>` + `using namespace std`
- Method signature uses `vector<int>&` (reference) for array params
- Use `try { ... } catch (...) { ... }` — C++ has no `finally`
- Sort expected+got before comparing for order-independent problems

---

### PYTHON Harness Template

```python
# USER_CODE_START
class Solution:
    def twoSum(self, nums, target):
        # Write your code here
        return [0, 0]
# USER_CODE_END

def test(nums, target, expected, tc, hidden=False):
    got = Solution().twoSum(nums, target)
    got.sort()
    expected.sort()
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:input={nums} target={target}:expected={expected}:got={got}")

try: test([2,7,11,15], 9, [0,1], 1)
except: print("TC:1:FAIL:hidden")
try: test([3,2,4], 6, [1,2], 2)
except: print("TC:2:FAIL:hidden")
try: test([3,3], 6, [0,1], 3)
except: print("TC:3:FAIL:hidden")
try: test([1,2,3,4,5], 9, [3,4], 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([-1,-2,-3,-4,-5], -8, [2,4], 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([0,4,3,0], 0, [0,3], 6, hidden=True)
except: print("TC:6:FAIL:hidden")
```

**Python specifics:**
- `# USER_CODE_START` / `# USER_CODE_END` markers
- No imports needed for basic types
- `try: ... except: ...` — bare except catches all exceptions
- hidden tests explicitly pass `hidden=True`

---

### JAVASCRIPT Harness Template

```javascript
// USER_CODE_START
function twoSum(nums, target) {
    // Write your code here
    return [0, 0];
}
// USER_CODE_END

function test(nums, target, expected, tc, hidden = false) {
    const got = twoSum(nums, target);
    got.sort((a, b) => a - b);
    expected.sort((a, b) => a - b);
    const gotStr = JSON.stringify(got);
    const expStr = JSON.stringify(expected);
    if (gotStr === expStr)
        console.log(`TC:${tc}:PASS` + (hidden ? ':hidden' : ''));
    else if (hidden)
        console.log(`TC:${tc}:FAIL:hidden`);
    else
        console.log(`TC:${tc}:FAIL:input=${JSON.stringify(nums)} target=${target}:expected=${expStr}:got=${gotStr}`);
}

try { test([2,7,11,15], 9, [0,1], 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([3,2,4], 6, [1,2], 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([3,3], 6, [0,1], 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([1,2,3,4,5], 9, [3,4], 4, true); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([-1,-2,-3,-4,-5], -8, [2,4], 5, true); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([0,4,3,0], 0, [0,3], 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
```

**JavaScript specifics:**
- Top-level function (no class needed)
- `JSON.stringify()` for array comparison — `===` on arrays checks reference not value
- Use `console.log()` (not `print()`)

---

### C Harness Template

```c
#include <stdio.h>

// USER_CODE_START
int* twoSum(int* nums, int n, int target, int* returnSize) {
    // Write your code here
    static int res[2];
    res[0] = 0; res[1] = 0;
    *returnSize = 2;
    return res;
}
// USER_CODE_END

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void test(int* nums, int n, int target, int* expected, int expN, int tc, int hidden) {
    int retSize;
    int* got = twoSum(nums, n, target, &retSize);
    if (retSize == expN && arrEq(got, expected, retSize)) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else {
            printf("TC:%d:FAIL:input=[", tc);
            for (int i = 0; i < n; i++) { if (i) printf(","); printf("%d", nums[i]); }
            printf("] target=%d:expected=[", target);
            for (int i = 0; i < expN; i++) { if (i) printf(","); printf("%d", expected[i]); }
            printf("]:got=[");
            for (int i = 0; i < retSize; i++) { if (i) printf(","); printf("%d", got[i]); }
            printf("]\n");
        }
    }
}

int main() {
    int t1[] = {2,7,11,15}; int e1[] = {0,1}; test(t1, 4, 9, e1, 2, 1, 0);
    int t2[] = {3,2,4};      int e2[] = {1,2}; test(t2, 3, 6, e2, 2, 2, 0);
    int t3[] = {3,3};        int e3[] = {0,1}; test(t3, 2, 6, e3, 2, 3, 0);
    int t4[] = {1,2,3,4,5};  int e4[] = {3,4}; test(t4, 5, 9, e4, 2, 4, 1);
    int t5[] = {-1,-2,-3,-4,-5}; int e5[] = {2,4}; test(t5, 5, -8, e5, 2, 5, 1);
    int t6[] = {0,4,3,0};    int e6[] = {0,3}; test(t6, 4, 0, e6, 2, 6, 1);
    return 0;
}
```

**C specifics:**
- NO try-catch in C — the platform's judge handles RE on crash
- Arrays must be passed as `(pointer, size)` pairs: `int* nums, int n`
- Return arrays via `static` buffer + `*returnSize` pointer, or just return a scalar
- Print format: `printf()` with `%d`, `%s`, etc.
- `hidden` parameter is `int` (0 or 1), not `boolean`

---

## Step 6: Insert Harnesses into DB

```sql
INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
VALUES
  (69, 'JAVA', '<entire java harness>', NOW(), NOW()),
  (69, 'CPP', '<entire cpp harness>', NOW(), NOW()),
  (69, 'PYTHON', '<entire python harness>', NOW(), NOW()),
  (69, 'JAVASCRIPT', '<entire javascript harness>', NOW(), NOW()),
  (69, 'C', '<entire c harness>', NOW(), NOW());
```

Or in Python:
```python
for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("""
        INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
        VALUES (%s, %s, %s, NOW(), NOW())
    """, (problem_id, lang, code))
```

---

## Step 7: Clear Cache After Each Insert

```bash
redis-cli -a <password> --no-auth-warning DEL "snippet:<problemId>:JAVA" "snippet:<problemId>:CPP" "snippet:<problemId>:PYTHON" "snippet:<problemId>:JAVASCRIPT" "snippet:<problemId>:C"
```

Or restart the backend (cache auto-clears after 60 min TTL).

---

## Data Structures (ListNode / TreeNode)

For problems about linked lists or trees, use serialization:

- **ListNode**: `[1, 2, 3]` for list 1→2→3. Empty = `[]`.
- **TreeNode**: Level-order BFS. `[1, null, 2, 3]`. Empty = `[]`.

In the harness:
1. **Real node type ABOVE markers** — driver + Solution both compile against it
2. **Commented definition INSIDE markers** — LeetCode-style for candidate
3. **build/serialize helpers OUTSIDE markers** — driver converts arrays ↔ nodes

Example for Java ListNode problem:
```java
class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
// USER_CODE_START
// class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
class Solution {
    public ListNode reverseList(ListNode head) { return head; }
}
// USER_CODE_END
public class Main {
    static ListNode build(int[] a) { /* array → linked list */ }
    static int[] ser(ListNode h) { /* linked list → array */ }
    static void test(int[] input, int[] expected, int tc, boolean hidden) {
        int[] got = ser(new Solution().reverseList(build(input)));
        // compare arrays, print TC:N:...
    }
    public static void main(String[] a) { /* calls to test() with try-catch */ }
}
```

---

## Complete Batch Script Template

Here is a complete Python script structure to batch-process all questions:

```python
import openpyxl
import psycopg2

# Connect to DB
conn = psycopg2.connect(host="161.118.187.201", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

# Read sheet
wb = openpyxl.load_workbook("Sheets/Deloitte_100_Coding_Questions.xlsx")
ws = wb.active

for row in range(2, ws.max_row + 1):
    name = str(ws.cell(row, 4).value or "").strip()
    diff = str(ws.cell(row, 5).value or "").strip()
    topic = str(ws.cell(row, 2).value or "").strip()
    if not name or name == "None":
        continue

    # ── YOU GENERATE THESE VALUES ──
    title = name  # or a cleaned version
    description = "..."  # generate based on your knowledge of this problem
    input_format = "..."
    output_format = "..."
    constraints = "..."
    time_limit = 3.0 if "EASY" in diff else 5.0 if "MEDIUM" in diff else 8.0
    memory_limit = 256
    level = "EASY" if "EASY" in diff else "MEDIUM" if "MEDIUM" in diff else "HARD"
    topics = topic  # or assign appropriate topics from the list
    example1, example2, example3 = "...", "...", "..."

    # Insert problem
    cur.execute("""
        INSERT INTO problems (title, description, input_format, output_format, constraints,
            time_limit, memory_limit, level, active, topics, example1, example2, example3)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id
    """, (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, True, topics, example1, example2, example3)))
    pid = cur.fetchone()[0]

    # ── YOU GENERATE THESE 5 HARNESSES ──
    java_code = "..."  # full harness
    cpp_code = "..."   # full harness
    py_code = "..."    # full harness
    js_code = "..."    # full harness
    c_code = "..."     # full harness

    # Insert snippets
    for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
        cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s,%s,%s,NOW(),NOW())", (pid, lang, code))

    conn.commit()
    print(f"Added: {title} (pid={pid})")

cur.close()
conn.close()
print("ALL DONE")
```

---

## Important Reminders

1. **harness code is RAW SQL string** — escape single quotes by doubling them: `''` for `'`
2. **Every test() call wrapped in try-catch** (Java/C++/Python/JS)
3. **Hidden tests never show input/expected/got** — only `TC:N:FAIL:hidden`
4. **6 test cases minimum** — 3 visible, 3 hidden
5. **Expected values must be CORRECT** — compute them carefully
6. **All 5 harnesses must exist** for the problem to work
7. Restart backend or clear Valkey cache after bulk insert
