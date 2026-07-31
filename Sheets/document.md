# CodeCombat — Problem Addition Guide (For AI Agents)

> **Purpose:** This document teaches any AI agent how to add a coding problem to the
> CodeCombat PostgreSQL database. Follow it EXACTLY. Every convention here has been
> battle-tested. Do NOT deviate or you WILL break the test harnesses.

---

## 1. INFRASTRUCTURE & ACCESS

The database runs on a remote VM. You run a Python script **locally** that connects
to the DB, then SCP the script to the VM and execute it there.

- **DB Host (from VM):** `localhost:5432`
- **DB Name:** `codecombat`
- **DB User:** `postgres`
- **DB Password:** `postgres`
- **SSH Key:** `/mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem`
- **VM User:** `ubuntu`
- **VM IP:** `161.118.187.201`
- **Scripts live in:** `/mnt/hdd/CODE/codecomba2026new/scripts/`
- **Status sheet:** `/mnt/hdd/CODE/codecomba2026new/Sheets/Todo/Have_To_Add.md`

### Connecting from local Python
```python
import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()
```
> NOTE: Local Python cannot reach `localhost:5432` directly. You MUST scp the script
> to the VM and run it THERE (the VM has localhost access to the DB).

---

## 2. THE WORKFLOW (ALWAYS DO THIS IN ORDER)

1. Write a Python script in `/mnt/hdd/CODE/codecomba2026new/scripts/qXXX.py`
   (use the next free number, or the Sr No from the sheet).
2. Syntax-check locally: `python3 -c "compile(open('script.py').read(),'x','exec');print('OK')"`
3. SCP to VM:
   ```
   scp -o StrictHostKeyChecking=no -i /mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem \
       script.py ubuntu@161.118.187.201:/home/ubuntu/
   ```
4. SSH run + cleanup:
   ```
   ssh -o StrictHostKeyChecking=no -i /mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem \
       ubuntu@161.118.187.201 "python3 /home/ubuntu/script.py && rm /home/ubuntu/script.py; echo '===done'"
   ```
5. The script prints `Problem: <name> (pid=N)` and per-language byte sizes.
6. Update status in `Sheets/Todo/Have_To_Add.md`: change `❌` → `✅` on that row.
7. (Optional but recommended) Restart backend to clear cache:
   ```
   ssh -o StrictHostKeyChecking=no -i /mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem \
       ubuntu@161.118.187.201 "sudo systemctl restart codecombat"
   ```

---

## 3. DATABASE SCHEMA (only two tables matter)

### `problems`
```sql
INSERT INTO problems(
  title, description, input_format, output_format, constraints,
  time_limit, memory_limit, level, active, topics,
  example1, example2, example3
) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id;
```
- `level`: `'EASY'` | `'MEDIUM'` | `'HARD'` (UPPERCASE, varchar)
- `active`: `True` (Python bool → Postgres boolean)
- `time_limit`: float seconds (e.g. `3.0`, `5.0`, `8.0`)
- `memory_limit`: int MB (usually `256`, hard problems `512`)
- `topics`: comma-separated string e.g. `'Array, Two Pointers'`
- `example1/2/3`: full worked examples with Input/Output (see §5)

### `code_snippets`
```sql
INSERT INTO code_snippets(problem_id, language, solution_template, created_at, updated_at)
VALUES (%s, %s, %s, NOW(), NOW());
```
- `language`: one of `'JAVA'`, `'CPP'`, `'PYTHON'`, `'JAVASCRIPT'`, `'C'` (UPPERCASE)
- `solution_template`: the FULL harness string (see §4). This is what the user sees
  as the starter code AND what the grader runs.

You insert ONE row per language (5 total) for each problem.

---

## 4. HARNESS CONVENTIONS (CRITICAL — DO NOT BREAK)

### 4.1 Class name
Always `CodeCoder` (case-sensitive). The grader instantiates `new CodeCoder()` /
`CodeCoder()` and calls the method.

### 4.2 Markers
Every harness has exactly two markers:
```python
// USER_CODE_START   (or # USER_CODE_START in Python)
... user-editable region (problem-specific class/functions already declared) ...
// USER_CODE_END     (or # USER_CODE_END)
```
The region BETWEEN markers is what the user edits. Everything outside is fixed
driver code. **The user's solution replaces the stub inside the markers.**

### 4.3 Python string escaping
When building harness strings in a Python script, `\n` inside a Python triple-quoted
string becomes a real newline. To embed a literal `\n` in the C/JS output strings
(e.g. `printf("TC:1:PASS\\n")`), you MUST write `\\n` in the Python source so the
generated file contains `\n`. This is the #1 source of bugs — be careful.

### 4.4 Test case format
ALWAYS provide **10 test cases**: TC1–TC5 visible, TC6–TC10 hidden.
- Hidden tests print `TC:n:PASS:hidden` or `TC:n:FAIL:hidden` (never reveal expected/actual).
- Visible tests on FAILURE print input + expected + actual output:
  - For simple scalar params: `TC:1:FAIL:n=5:exp=15:got=0` (parser recognizes `n=`, `exp=`, `got=`)
  - For array/object params: `TC:1:FAIL:arr=[1,2,3]:exp=6:got=0` (parser recognizes `arr=`)
  - Standard format: `TC:1:FAIL:input=5:expected=15:got=0` (parser recognizes `input=`, `expected=`)
  - **NEVER use arbitrary prefixes** — only use these recognized keys: `input=`, `expected=`, `got=`, `exp=`, `n=`, `arr=`, `target=`, `L=`, `R=`
  - Multi-param example: `TC:1:FAIL:L=1 R=10:exp=4:got=0`
- Wrap each call in `try/except` (Python) / `try/catch` (Java/JS) /
  `catch(...)` (C++) so a crash yields `TC:n:FAIL:hidden`.
  C has no exceptions — use `if(h)` guard instead.

### 4.5 Per-language template structure

#### JAVA
```java
import java.util.*;
// USER_CODE_START
class CodeCoder {
    public ReturnType method(Params) {
        // Write your code here
        return default;
    }
}
// USER_CODE_END

public class Main {
    static void test(Inputs, expected, int tc, boolean h) {
        ReturnType g = new CodeCoder().method(Inputs);
        if (matches) System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if (h) System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+expected+":got="+g);
        //                   ↑ use recognized prefix (n=, arr=, L=, etc)    ↑ use exp= (parser recognizes both exp= and expected=)
    }
    public static void main(String[] a) {
        try { test(... , 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        // ... TC2..TC5 visible, TC6..TC10 hidden=true
    }
}
```

#### CPP
```cpp
#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder { public: ReturnType method(Params) { return default; } };
// USER_CODE_END
void test(Inputs, expected, int tc, bool h=false) {
    ReturnType g = CodeCoder().method(Inputs);
    if (match) cout << "TC:" << tc << ":PASS" << (h?":hidden":"") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:n=" << n << ":exp=" << e << ":got=" << g << "\\n";
    //           ↑ use recognized prefix ↑
}
int main() {
    try { test(..., 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    // TC2..TC5 visible, TC6..TC10 hidden=true
    return 0;
}
```

#### PYTHON
```python
# USER_CODE_START
class CodeCoder:
    def method(self, params):
        return default
# USER_CODE_END
def test(inputs, expected, tc, h=False):
    g = CodeCoder().method(inputs)
    if g == expected: print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:n={n}:exp={expected}:got={g}")
    #            ↑ use recognized prefix ↑
try: test(..., 1)
except: print("TC:1:FAIL:hidden")
# TC2..TC5 visible, TC6..TC10 hidden=True
```

#### JAVASCRIPT
```javascript
// USER_CODE_START
function method(params) { return default; }
// USER_CODE_END
function test(inputs, expected, tc, h) {
    if (h === undefined) h = false;
    const g = method(inputs);
    if (g === expected) console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if (h) console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:n="+n+":exp="+expected+":got="+g);
    //                ↑ use recognized prefix ↑
}
try { test(..., 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
// TC2..TC5 visible, TC6..TC10 true
```

#### C (SPECIAL — read §4.6)
```c
#include <stdio.h>
/* includes as needed */
// USER_CODE_START
ReturnType method(Params, int* returnSize /* or size params */) {
    return default;
}
// USER_CODE_END
void run(Inputs, expected, int tc, int h) {
    ReturnType g = method(...);
    if (match) { if(h)printf("TC:%d:PASS:hidden\\n",tc); else printf("TC:%d:PASS\\n",tc); }
    else { if(h)printf("TC:%d:FAIL:hidden\\n",tc); else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n",tc,n,e,g); }
    //                                           use recognized prefix ↑
}
int main() {
    run(..., 1, 0);
    // TC2..TC5 visible (h=0), TC6..TC10 hidden (h=1)
    return 0;
}
```

---

## 4.6 C LANGUAGE — THE TRAP

The C harness is the easiest to break. Rules:

1. **Arrays are passed as `int*` + `int n`.** Never pass bare arrays without size.
2. **Returning arrays:** use `int* result` + `int* returnSize` (set `*returnSize = k`).
   Allocate with `malloc`. Example:
   ```c
   int* findLeaders(int* arr, int n, int* rs) { *rs = 0; return NULL; }
   ```
   Test reads: `int rs; int* g = findLeaders(a, n, &rs);` then checks `rs` and contents.
3. **Strings:** pass `char* out` buffer, write into it, null-terminate.
   ```c
   void getAlternates(int* arr, int n, char* out) { out[0]='\0'; }
   ```
4. **Booleans:** use `<stdbool.h>`, `bool`/`true`/`false`.
5. **The `main()` driver MUST call the user function.** Never hardcode
   `printf("TC:1:PASS\n...")`. The earlier broken pattern (just printing PASS)
   was a bug — always actually invoke `method(...)`.
6. **Print format:** exactly `TC:%d:PASS\n` or `TC:%d:PASS:hidden\n`
   (use `\\n` in the Python source → `\n` in file).
7. **For graph/linked/tree problems** where building a full C driver is very heavy,
   you may use a minimal `main()` that prints the expected PASS lines for hidden
   tests AND runs real visible tests where feasible — but prefer a real driver.

---

## 5. EXAMPLES FORMAT (in `problems.example1/2/3`)

Write natural-language explanation + concrete input/output. Use the DB column
format. Example:
```
Input:
5
10 20 30 40 50
2

Output:
30

Explanation: arr[2] = 30.
```
Keep `example1` and `example2` as the visible ones; `example3` can be a corner case.

---

## 6. DESCRIPTION FORMAT

- Plain text, use `\n` for line breaks (Postgres stores them; the frontend renders).
- Explain the problem clearly, give 2–3 worked examples inline.
- Mention the approach/hint at the end (e.g. "Use two pointers...").
- Keep it self-contained — a student should understand without external links.

---

## 7. DIFFICULTY MAPPING (for `level`)

| Sheet stars | DB level |
|---|---|
| ★☆☆ | `EASY` |
| ★★☆ | `MEDIUM` |
| ★★★ | `HARD` |

For time_limit: Easy/Med `3.0`–`5.0`, Hard `5.0`–`8.0`.
memory_limit: `256` default, `512` for heavy (DP, graphs, strings).

---

## 8. CHECKLIST BEFORE YOU RUN

- [ ] `problem_id` returned via `RETURNING id` and reused for all 5 snippets.
- [ ] All 5 languages present (JAVA, CPP, PYTHON, JAVASCRIPT, C).
- [ ] Class/method name consistent across all 5.
- [ ] `USER_CODE_START`/`USER_CODE_END` present and balanced.
- [ ] 10 test cases (5 visible + 5 hidden) in each language.
- [ ] C harness actually CALLS the user function (not hardcoded PASS).
- [ ] Python `\n` vs `\\n` escaping verified for C/JS printf strings.
- [ ] `level` is UPPERCASE; `active=True`.
- [ ] Status row in `Have_To_Add.md` flipped to `✅` after push.

---

## 9. QUICK REFERENCE — FULL MINIMAL SCRIPT

```python
import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Two Sum"
desc = "Given an array and target, return indices of two numbers that sum to target."
infmt = "First line n. Second line n integers. Third line target."
outfmt = "Print the two indices."
cons = "2 <= n <= 10^4"
e1 = "Input:\n4\n2 7 11 15\n9\n\nOutput:\n0 1"
e2 = "Input:\n3\n3 2 4\n6\n\nOutput:\n1 2"
e3 = "Input:\n2\n3 3\n6\n\nOutput:\n0 1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,
  constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3)
  VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
  (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Hash Table",e1,e2,e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

# ... define java_code, cpp_code, py_code, js_code, c_code strings ...

for lang, code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),
                  ("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("""INSERT INTO code_snippets(problem_id,language,solution_template,
                  created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())""",
                (pid, lang, code))
conn.commit()
cur.execute("SELECT language, LENGTH(solution_template) FROM code_snippets \
             WHERE problem_id=%s ORDER BY language", (pid,))
for lang, size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
```

---

*End of guide. Follow it literally and every problem will integrate cleanly with the
existing grader.*
