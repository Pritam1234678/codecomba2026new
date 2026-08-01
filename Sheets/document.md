# CodeCombat — Problem Addition Guide (For AI Agents)

> **PURPOSE:** This document defines the EXACT format for adding coding problems.
> Every deviation from this document causes SILENT BUGS in production.
> The harness output is parsed by a regex-based parser — if your WA output doesn't
> match the recognized prefixes, the frontend shows NO debug information.
> **DO NOT BE CREATIVE. COPY THE TEMPLATES VERBATIM.**

---

## 1. INFRASTRUCTURE & ACCESS

The database runs on a remote VM. You write a Python script **locally**, SCP to VM, run there.

- **DB Host (from VM):** `localhost:5432`
- **DB Name:** `codecombat`
- **DB User:** `postgres`
- **DB Password:** `postgres`
- **SSH Key:** `/mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem`
- **VM User:** `ubuntu`
- **VM IP:** `161.118.187.201`
- **Scripts live in:** `/mnt/hdd/CODE/codecomba2026new/scripts/`
- **Status sheet:** `/mnt/hdd/CODE/codecomba2026new/Sheets/Todo/Have_To_Add.md`

```python
import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()
```

> NOTE: Run scripts on the VM. Local Python cannot reach `localhost:5432`.

---

## 2. WORKFLOW

1. Write script in `/mnt/hdd/CODE/codecomba2026new/scripts/qXXX.py` (next free number from the sheet).
2. Syntax-check: `python3 -c "compile(open('script.py').read(),'x','exec');print('OK')"`
3. SCP to VM:
   ```
   scp -o StrictHostKeyChecking=no -i /mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem \
       script.py ubuntu@161.118.187.201:/home/ubuntu/scripts/
   ```
4. Run:
   ```
   ssh -o StrictHostKeyChecking=no -i /mnt/hdd/CODE/codecomba2026new/cc-vm_key.pem \
       ubuntu@161.118.187.201 "python3 /home/ubuntu/scripts/script.py; echo '===done'"
   ```
5. Update status sheet: `❌` → `✅`.

---

## 3. DATABASE SCHEMA

### `problems`
```sql
INSERT INTO problems(
  title, description, input_format, output_format, constraints,
  time_limit, memory_limit, level, active, topics,
  example1, example2, example3
) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id;
```
- `level`: `'EASY'` | `'MEDIUM'` | `'HARD'` (UPPERCASE only)
- `active`: `True`
- `time_limit`: float seconds (2.0–8.0)
- `memory_limit`: int MB (256 default, 512 for heavy problems)
- `topics`: comma-separated string: `'Array, Two Pointers'`
- `example1/2/3`: text with Input/Output blocks (see §7)

### `code_snippets`
```sql
INSERT INTO code_snippets(problem_id, language, solution_template, created_at, updated_at)
VALUES (%s, %s, %s, NOW(), NOW());
```
- `language`: `'JAVA'` | `'CPP'` | `'PYTHON'` | `'JAVASCRIPT'` | `'C'` (UPPERCASE)
- `solution_template`: the FULL harness. Always insert 5 rows per problem.

---

## 4. CLASS NAME AND MARKERS

**Class name:** `CodeCoder` — ALWAYS. Case-sensitive. In all 5 languages.

**User-editable region:**
```
// USER_CODE_START     (or # USER_CODE_START in Python)
... starter code (class/function with return stub) ...
// USER_CODE_END       (or # USER_CODE_END)
```

The user replaces the content BETWEEN these markers. Everything outside is the test driver (uneditable).

---

## 5. WA FAIL OUTPUT FORMAT — MANDATORY (READ THIS 3 TIMES)

The parser extracts debug info from WA (Wrong Answer) output lines. It uses `:` as delimiter
and recognizes **only these prefixes**:

### Recognized prefixes and what they map to

| Prefix | Parser field | When to use |
|--------|-------------|-------------|
| `n=` | `tc.input` | Single integer parameter |
| `arr=` | `tc.input` | Array/list parameter |
| `target=` | `tc.input` | Target value parameter |
| `s=` | `tc.input` | String parameter |
| `L=` | `tc.input` | Left bound parameter |
| `R=` | `tc.input` | Right bound parameter |
| `exp=` | `tc.expected` | Expected output (PREFERRED — always use `exp=` not `expected=`) |
| `got=` | `tc.got` | Actual output from user's code |

### The ONLY valid WA FAIL format:

```
TC:{N}:FAIL:<input_prefix>=<value>:exp=<expected>:got=<actual>
```

**Every visible WA line MUST contain ALL THREE:**
1. At least ONE input prefix with value (`n=5` or `arr=[1,2,3]` etc.)
2. `exp=<expected_value>` (the correct answer)
3. `got=<actual_value>` (what the user's code returned)

### FORBIDDEN patterns — NEVER use these:

| Forbidden | Why it breaks |
|-----------|---------------|
| `TC:1:FAIL:n=5:got=0` — missing `exp=` | User can't compare expected vs got |
| `TC:1:FAIL:got=5` — no input prefix | User doesn't know what input was |
| `TC:1:FAIL:n=5` — no exp/got | Completely useless |
| `TC:1:FAIL:exp=3:got=5` — no input | User can't reproduce the failure |
| `TC:1:FAIL:hidden` — for visible tests | Never hide visible test info |
| `TC:1:FAIL:exp=[...]...got=...` — print only first element | **FP arrays in C/CPP — print COMPLETE arrays** |

### For "any valid answer" problems (e.g. parity, arrangements where multiple outputs are correct):

Omit `exp=` (there is no single correct answer). Still include input + got:
```
TC:1:FAIL:arr=[4,2,5,7]:got=[4,5,2,7]     ← input + got only, exp= omitted
```

---

## 6. PER-LANGUAGE WA FAIL LINES — COPY EXACTLY

You MUST copy the EXACT WA failure line from the template below for your problem type.
Only change variable names to match your parameters.

### 6A. SCALAR return (int, long, char, bool)

The function returns a single value. Example: input is `n` (int), output is `int`.

**JAVA:**
```java
System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);
```

**CPP:**
```cpp
cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";
```

**PYTHON:**
```python
print(f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}")
```

**JAVASCRIPT:**
```javascript
console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);
```

**C:**
```c
printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n", tc, n, e, g);
```

### 6B. ARRAY/LIST return

The function returns an array/list. Example: input is `int[] arr`, output is `int[]`.

**Print arrays as `[1,2,3]` format, complete (all elements, never truncated).**

**JAVA:**
```java
System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(arr)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));
```

**CPP:**
```cpp
cout<<"TC:"<<tc<<":FAIL:arr=[";
for(int i=0;i<(int)arr.size();i++){if(i)cout<<",";cout<<arr[i];}
cout<<"]:exp=[";
for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}
cout<<"]:got=[";
for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}
cout<<"]\\n";
```

**PYTHON:**
```python
print(f"TC:{tc}:FAIL:arr={arr}:exp={e}:got={g}")
```

**JAVASCRIPT:**
```javascript
console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(arr)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));
```

**C — array-return function stores result via `int*` + `int* returnSize`:**
```c
printf("TC:%d:FAIL:n=%d:arr=[", tc, n);
for(int i=0;i<n;i++){if(i)printf(",");printf("%d",arr[i]);}
printf("]:exp=[");
for(int i=0;i<n;i++){if(i)printf(",");printf("%d",e[i]);}
printf("]:got=[");
for(int i=0;i<*rs;i++){if(i)printf(",");printf("%d",res[i]);}
printf("]\\n");
free(res);  // ALWAYS free malloc'd result
```

### 6C. STRING return

The function returns a String. Use quoting to distinguish from numbers.

**JAVA:**
```java
System.out.println("TC:"+tc+":FAIL:s=\""+s+"\":exp=\""+e+"\":got=\""+g+"\"");
```

**CPP:**
```cpp
cout<<"TC:"<<tc<<":FAIL:s=\""<<s<<"\":exp=\""<<e<<"\":got=\""<<g<<"\"\\n";
```

**PYTHON:**
```python
print(f"TC:{tc}:FAIL:s={s!r}:exp={e!r}:got={g!r}")
```

**JAVASCRIPT:**
```javascript
console.log("TC:"+tc+":FAIL:s="+JSON.stringify(s)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));
```

**C — string buffer output:**
```c
printf("TC:%d:FAIL:s=%s:exp=%s:got=%s\\n", tc, s, e, buf);
```

### 6D. ANY-VALID-ANSWER (no single expected output)

Only show input + got. Omit `exp=`.

**JAVA:**
```java
System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(arr)+":got="+Arrays.toString(g));
```

**CPP:**
```cpp
cout<<"TC:"<<tc<<":FAIL:arr=[";
for(int i=0;i<(int)arr.size();i++){if(i)cout<<",";cout<<arr[i];}
cout<<"]:got=[";
for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}
cout<<"]\\n";
```

**PYTHON:**
```python
print(f"TC:{tc}:FAIL:arr={arr}:got={g}")
```

**JAVASCRIPT:**
```javascript
console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(arr)+":got="+JSON.stringify(g));
```

**C:**
```c
printf("TC:%d:FAIL:n=%d:arr=[",tc,n);
for(int i=0;i<n;i++){if(i)printf(",");printf("%d",arr[i]);}
printf("]:got=[");
for(int i=0;i<*rs;i++){if(i)printf(",");printf("%d",res[i]);}
printf("]\\n");
free(res);
```

### 6E. MULTI-PARAM input

When the function takes multiple distinct inputs, chain prefixes with `:`:
```
TC:1:FAIL:L=1:R=10:target=5:exp=4:got=0
```

Do this instead of combining into one prefix.

---

## 7. TEST CASE REQUIREMENTS

ALWAYS provide **exactly 10 test cases**: TC1–TC5 = visible, TC6–TC10 = hidden.

### Hidden test output
```
TC:N:PASS:hidden    (if AC)
TC:N:FAIL:hidden    (if WA — NEVER reveal expected/actual for hidden tests)
```

### Test function signature — standard pattern

Each test function takes `(inputs, expected, tc_number, hidden_flag)`.
- On PASS: print `TC:N:PASS` or `TC:N:PASS:hidden`
- On FAIL (visible): print the full debug line from §6
- On FAIL (hidden): print `TC:N:FAIL:hidden` ONLY
- Wrap every call in try/except so runtime errors become `TC:N:FAIL:hidden`

### C test driver — CRITICAL RULES

The C harness is the #1 source of production bugs. These rules are ABSOLUTE:

1. **The test function MUST call the user's function.** Never hardcode PASS lines.
   ```c
   // WRONG — don't do this:
   printf("TC:1:PASS\\n");
   // RIGHT:
   int g = userFunction(input); if(g==expected) printf("TC:1:PASS\\n"); ...
   ```

2. **Arrays:** `int* arr, int n` — always pass size with pointer.
3. **Returning arrays:** Use `int* func(int* arr, int n, int* rs)`. Set `*rs = count`,
   return `malloc(rs * sizeof(int))`. Always `free()` the result after checking.
4. **Strings (char* args):** Use `const char*` parameter.
5. **Strings (output buffer):** `void func(int n, char* out)`. Write into `out`, null-terminate.
6. **printf escape in Python:** Use `\\n` (double backslash) so generated code has `\n`.
   - In Python triple quotes: `printf("TC:1:PASS\\n")` → in generated C file: `printf("TC:1:PASS\n")`
7. **Booleans:** `#include <stdbool.h>`, use `bool`/`true`/`false`.
8. **Graph/tree C drivers are VERY heavy.** If you can't build one, DO NOT generate
   a fake C harness. Ask first.

---

## 8. FULL HARNESS TEMPLATES (per-language, per-return-type)

### 8.1 SCALAR RETURN (int/long/char/bool)

#### JAVA
```java
import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int solve(int n) {
        // Write your code here
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n, int e, int tc, boolean h) {
    int g = new CodeCoder().solve(n);
    if(g == e) System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h) System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);
}
public static void main(String[] a) {
try{test(5, 15, 1, false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(10, 55, 2, false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1, 1, 3, false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(3, 6, 4, false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(7, 28, 5, false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(100, 5050, 6, true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(50, 1275, 7, true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(20, 210, 8, true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(0, 0, 9, true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(99, 4950, 10, true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}
```

#### CPP
```cpp
#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int solve(int n){return 0;}};
// USER_CODE_END
void test(int n,int e,int tc,bool h=false){
    int g=CodeCoder().solve(n);
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";
}
int main(){
try{test(5,15,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(10,55,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(3,6,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(7,28,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,5050,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(50,1275,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(20,210,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(0,0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(99,4950,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}
```

#### PYTHON
```python
# USER_CODE_START
class CodeCoder:
    def solve(self, n):
        return 0
# USER_CODE_END
def test(n, e, tc, h=False):
    g=CodeCoder().solve(n)
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
try:test(5,15,1)
except:print("TC:1:FAIL:hidden")
try:test(10,55,2)
except:print("TC:2:FAIL:hidden")
try:test(1,1,3)
except:print("TC:3:FAIL:hidden")
try:test(3,6,4)
except:print("TC:4:FAIL:hidden")
try:test(7,28,5)
except:print("TC:5:FAIL:hidden")
try:test(100,5050,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(50,1275,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(20,210,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(0,0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(99,4950,10,hidden=True)
except:print("TC:10:FAIL:hidden")
```

#### JAVASCRIPT
```javascript
// USER_CODE_START
function solve(n) { return 0; }
// USER_CODE_END
function test(n, e, tc, h) {
    if(h===undefined)h=false;
    const g=solve(n);
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);
}
try{test(5,15,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(10,55,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(3,6,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(7,28,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,5050,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(50,1275,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(20,210,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(0,0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(99,4950,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}
```

#### C
```c
#include <stdio.h>

// USER_CODE_START
int solve(int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void run(int n, int e, int tc, int h) {
    int g = solve(n);
    if(g == e) {
        if(h) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if(h) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n", tc, n, e, g);
    }
}
int main() {
    run(5, 15, 1, 0);
    run(10, 55, 2, 0);
    run(1, 1, 3, 0);
    run(3, 6, 4, 0);
    run(7, 28, 5, 0);
    run(100, 5050, 6, 1);
    run(50, 1275, 7, 1);
    run(20, 210, 8, 1);
    run(0, 0, 9, 1);
    run(99, 4950, 10, 1);
    return 0;
}
```

### 8.2 ARRAY RETURN — C SPECIAL TEMPLATE

```c
#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* sortArray(int* nums, int n, int* rs) {
    // Write your code here. Set *rs = result size.
    // Allocate result with malloc(*rs * sizeof(int)).
    *rs = 0; return NULL;
}
// USER_CODE_END

void run(int* a, int n, int* e, int tc, int h) {
    int rs = 0;
    int* g = sortArray(a, n, &rs);
    int ok = (rs == n);
    if(ok) for(int i=0;i<n;i++) { if(g[i] != e[i]) { ok=0; break; } }
    if(ok) {
        if(h) printf("TC:%d:PASS:hidden\\n",tc);
        else printf("TC:%d:PASS\\n",tc);
    } else {
        if(h) printf("TC:%d:FAIL:hidden\\n",tc);
        else {
            printf("TC:%d:FAIL:arr=[",tc);
            for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
            printf("]:exp=[");
            for(int i=0;i<n;i++){if(i)printf(",");printf("%d",e[i]);}
            printf("]:got=[");
            for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",g[i]);}
            printf("]\\n");
        }
    }
    free(g);
}
int main() {
    int a1[]={5,2,3,1};int e1[]={1,2,3,5};run(a1,4,e1,1,0);
    int a2[]={5,1,1,2,0,0};int e2[]={0,0,1,1,2,5};run(a2,6,e2,2,0);
    int a3[]={1};int e3[]={1};run(a3,1,e3,3,0);
    int a4[]={3,2,1};int e4[]={1,2,3};run(a4,3,e4,4,0);
    int a5[]={-2,3,0,-5,4};int e5[]={-5,-2,0,3,4};run(a5,5,e5,5,0);
    int a6[]={9,8,7,6,5,4,3,2,1};int e6[]={1,2,3,4,5,6,7,8,9};run(a6,9,e6,6,1);
    // ... TC7–TC10 hidden
    return 0;
}
```

### 8.3 ANY-VALID-ANSWER TEMPLATE

For problems where the validator checks a PROPERTY (not equality with a specific array).
NO `exp=` in the output. Example: parity check, any valid arrangement, etc.

#### JAVA
```java
static void test(int[] a, int tc, boolean h) {
    int[] g = new CodeCoder().solve(a.clone());
    boolean ok = /* check property of g */;
    if(ok) System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h) System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":got="+Arrays.toString(g));
}
```

#### CPP
```cpp
void test(vector<int> a,int tc,bool h=false){
    vector<int> g=CodeCoder().solve(a);
    bool ok=/* check property */;
    if(ok)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:got=[";for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}cout<<"]\\n";}
}
```

#### PYTHON
```python
def test(a, tc, h=False):
    g=CodeCoder().solve(list(a))
    ok=/* check property */
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:got={g}"))
```

#### JAVASCRIPT
```javascript
function test(a, tc, h) {
    if(h===undefined)h=false;
    const g=solve(a.slice());
    let ok=/* check property */;
    if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":got="+JSON.stringify(g));
}
```

#### C
```c
void run(int* a, int n, int tc, int h) {
    int rs=0;
    int* g=solve(a,n,&rs);
    int ok=(rs==n);
    if(ok)for(int i=0;i<n;i++){/* check property of g */;if(!ok)break;}
    if(ok){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{
        if(h)printf("TC:%d:FAIL:hidden\\n",tc);
        else{
            printf("TC:%d:FAIL:arr=[",tc);
            for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
            printf("]:got=[");
            for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",g[i]);}
            printf("]\\n");
        }
    }
    free(g);
}
```

---

## 9. PYTHON ESCAPING RULES

When embedding harness code in a Python script (inside `'''...''''` or `"""..."""`):

| You write in Python source | Generated code gets |
|---|---|
| `\\n` | `\n` (literal newline in C/JS string) |
| `\\t` | `\t` |
| `\\\\` | `\\` |
| `\"` | `"` (inside Python single-quoted string) |
| `\\"` | `\"` (literal backslash-quote) |

**Examples:**
```python
# Python source:       Generated C code:
c_code = '''printf("TC:1:PASS\\n");'''   # → printf("TC:1:PASS\n");
c_code = '''printf("TC:1:FAIL:n=%d\\n",n);'''  # → printf("TC:1:FAIL:n=%d\n",n);
c_code = '''printf("\\\"hello\\\"\\n");'''  # → printf("\"hello\"\n");
```

**For C code with array loop print inside printf format string:**
```python
c_code = '''
printf("TC:%d:FAIL:arr=[",tc);
for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
printf("]:exp=[");
for(int i=0;i<n;i++){if(i)printf(",");printf("%d",e[i]);}
printf("]:got=[");
for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",g[i]);}
printf("]\\n");
'''  # Note: only the last \\n needs escaping; the ] inside printf format string does NOT
```

---

## 10. DESCRIPTION AND EXAMPLES FORMAT

### `example1/2/3` (DB column):
```
Input:
5
1 2 3 4 5
3

Output:
2

Explanation: arr[2] = 3.
```

### `description` (DB column):
- Plain text, natural `\n` line breaks.
- Explain the problem clearly, give 2–3 concrete examples inline.
- Mention the approach/hint at the end.
- Self-contained — no external links needed.

### `constraints` (DB column):
```
1 ≤ n ≤ 10^5
-10^9 ≤ arr[i] ≤ 10^9
```

---

## 11. DIFFICULTY MAPPING

| Sheet stars | DB level  | Time limit |
|------------|-----------|------------|
| ★☆☆         | `EASY`    | 3.0        |
| ★★☆         | `MEDIUM`  | 5.0        |
| ★★★         | `HARD`    | 5.0–8.0    |

memory_limit: `256` default, `512` for heavy (DP, graphs, large strings).

---

## 12. MANDATORY CHECKLIST — VERIFY ALL BEFORE RUNNING

### Structure
- [ ] All 5 languages present (JAVA, CPP, PYTHON, JAVASCRIPT, C)
- [ ] `CodeCoder` class used in ALL 5 languages
- [ ] Method name identical across all 5 languages
- [ ] `USER_CODE_START`/`USER_CODE_END` markers present and balanced
- [ ] 10 test cases (5 visible + 5 hidden) in EVERY language

### WA FAIL output — CHECK EVERY LANGUAGE
- [ ] Hidden tests print ONLY `TC:N:FAIL:hidden` (no debug info leaked)
- [ ] Visible FAIL has at least ONE recognized input prefix
- [ ] Visible FAIL has `exp=` prefix (unless any-valid-answer problem)
- [ ] Visible FAIL has `got=` prefix
- [ ] Array output prints FULL arrays, not just first element or size
- [ ] String output uses proper quoting (`JSON.stringify`/`!r`/`\"...\"`)
- [ ] C output uses `printf("...\\n")` with proper escaping in Python source

### C-specific
- [ ] C harness ACTUALLY CALLS the user function (no hardcoded PASS)
- [ ] Array-return functions have `int* returnSize` parameter
- [ ] `free()` called on every `malloc`'d result
- [ ] Array parameters passed as `int*` + `int n`
- [ ] String parameters use `const char*`

### Python script
- [ ] `\\n` escaping correct for all C/JS printf statements
- [ ] `level` is UPPERCASE: `'EASY'` / `'MEDIUM'` / `'HARD'`
- [ ] `active=True`
- [ ] `pid` from `RETURNING id` reused for all 5 snippet inserts
- [ ] `conn.commit()` called after inserts
- [ ] Script prints pid and byte sizes for verification

---

## 13. FULL MINIMAL SCRIPT TEMPLATE

```python
import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Problem Title"
desc = "Problem description..."
infmt = "Input format description"
outfmt = "Output format description"
cons = "Constraints..."
e1 = "Input:\n...\n\nOutput:\n..."
e2 = "Input:\n...\n\nOutput:\n..."
e3 = "Input:\n...\n\nOutput:\n..."

# Check if exists, else insert
cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,
      constraints,time_limit,memory_limit,level,active,topics,
      example1,example2,example3)
      VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
      (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True,
       "Array, Searching", e1, e2, e3))
    pid = cur.fetchone()[0]
    print(f"Created {title} (pid={pid})")

# --- Define harness code strings — COPY TEMPLATES FROM §8 ABOVE ---

java_code = '''...'''
cpp_code = '''...'''
py_code = '''...'''
js_code = '''...'''
c_code = '''...'''

for lang, code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),
                    ("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("""INSERT INTO code_snippets(problem_id,language,solution_template,
                  created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())""",
                (pid, lang, code))

conn.commit()

cur.execute("SELECT language, LENGTH(solution_template) FROM code_snippets \
             WHERE problem_id=%s ORDER BY language", (pid,))
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\n{title} (pid={pid}) — done!")
cur.close()
conn.close()
```

---

**REMEMBER:** The parser extracts debug info from WA output lines using exact prefix matching.
If you deviate from `exp=` or `got=` or the recognized input prefixes, the parser silently fails
and users see NO debug info. COPY THE TEMPLATES. DO NOT IMPROVISE.
