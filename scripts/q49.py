"""
Daily Temperatures
==================
Given an array of integers temperatures that represents the daily temperatures,
return an array answer where answer[i] is the number of days you have to wait
after the ith day to get a warmer temperature.

If there is no future day for which this is possible, put 0 instead.

Example:
  temperatures = [73, 74, 75, 71, 69, 72, 76, 73]
  Output:        [1,  1,  4,  2,  1,  1,  0,  0]

Explanation:
  Day 0 (73) → next warmer on day 1 (74) → wait 1 day
  Day 1 (74) → next warmer on day 2 (75) → wait 1 day
  Day 2 (75) → next warmer on day 6 (76) → wait 4 days
  Day 3 (71) → next warmer on day 5 (72) → wait 2 days
  Day 4 (69) → next warmer on day 5 (72) → wait 1 day
  Day 5 (72) → next warmer on day 6 (76) → wait 1 day
  Day 6 (76) → no warmer day → 0
  Day 7 (73) → no warmer day → 0

Approach: Use a monotonic decreasing stack. Iterate from right to left (or left to right),
maintaining a stack of indices with decreasing temperatures. When a warmer temperature
is found, pop and calculate the difference in indices.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2, json

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Daily Temperatures"
desc = (
    "Given an array of integers temperatures representing daily temperatures, "
    "return an array answer such that answer[i] is the number of days you have to wait "
    "after the ith day to get a warmer temperature. If there is no future day "
    "for which this is possible, keep answer[i] = 0.\n\n"
    "Example: temperatures = [73,74,75,71,69,72,76,73]\n"
    "Output: [1,1,4,2,1,1,0,0]\n\n"
    "Use a monotonic decreasing stack: iterate through indices, and for each day, "
    "check the stack. While the current temperature is warmer than the temperature "
    "at the index on top of the stack, pop and set the result for that popped index."
)
infmt = "First line contains integer n.\nSecond line contains n space-separated integers representing temperatures."
outfmt = "Print n space-separated integers — the number of days to wait for a warmer temperature."
cons = (
    "1 \u2264 n \u2264 10^5\n"
    "30 \u2264 temperatures[i] \u2264 100"
)
e1 = (
    "Input:\n"
    "8\n"
    "73 74 75 71 69 72 76 73\n\n"
    "Output:\n"
    "1 1 4 2 1 1 0 0\n\n"
    "Explanation: Day 0 (73) → next warmer day 1 (74) → wait 1. "
    "Day 2 (75) → need day 6 (76) → wait 4. "
    "Day 6 (76) → no warmer → 0."
)
e2 = (
    "Input:\n"
    "3\n"
    "30 40 50\n\n"
    "Output:\n"
    "1 1 0\n\n"
    "Explanation: Strictly increasing — each day is warmer than previous, "
    "except last which has no warmer future day."
)
e3 = (
    "Input:\n"
    "3\n"
    "90 80 70\n\n"
    "Output:\n"
    "0 0 0\n\n"
    "Explanation: Strictly decreasing — no warmer day ever comes for any day."
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True,
     "Array, Stack, Monotonic Stack", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] dailyTemperatures(int[] temperatures) {
        // Write your code here — use a monotonic stack
        return new int[0];
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] temps, int[] expected, int tc, boolean hidden) {
        int[] got = new CodeCoder().dailyTemperatures(temps);
        if (Arrays.equals(got, expected)) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(temps)
                + ":expected=" + Arrays.toString(expected) + ":got=" + Arrays.toString(got));
        }
    }

    public static void main(String[] args) {
        try { test(new int[]{73,74,75,71,69,72,76,73}, new int[]{1,1,4,2,1,1,0,0}, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test(new int[]{30,40,50}, new int[]{1,1,0}, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test(new int[]{90,80,70}, new int[]{0,0,0}, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test(new int[]{70}, new int[]{0}, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(new int[]{70,70,70,70}, new int[]{0,0,0,0}, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(new int[]{30,31,30,32,31,33}, new int[]{1,2,1,2,1,0}, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test(new int[]{100,90,80,85,90,95}, new int[]{0,0,1,1,1,0}, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test(new int[]{50,55,53,52,54,56}, new int[]{1,4,2,1,1,0}, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test(new int[]{30,30,30,31,30,30,32}, new int[]{3,2,1,3,1,1,0}, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test(new int[]{35,33,34,32,36,31,37}, new int[]{4,1,2,1,2,1,0}, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    vector<int> dailyTemperatures(vector<int>& temperatures) {
        // Write your code here — use a monotonic stack
        return {};
    }
};
// USER_CODE_END

void test(vector<int> temps, vector<int> expected, int tc, bool hidden = false) {
    vector<int> got = CodeCoder().dailyTemperatures(temps);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:got=[";
        for (size_t i = 0; i < got.size(); i++) { if (i) cout << ","; cout << got[i]; }
        cout << "]:expected=[";
        for (size_t i = 0; i < expected.size(); i++) { if (i) cout << ","; cout << expected[i]; }
        cout << "]\n";
    }
}

int main() {
    try { test({73,74,75,71,69,72,76,73}, {1,1,4,2,1,1,0,0}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test({30,40,50}, {1,1,0}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test({90,80,70}, {0,0,0}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test({70}, {0}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test({70,70,70,70}, {0,0,0,0}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test({30,31,30,32,31,33}, {1,2,1,2,1,0}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test({100,90,80,85,90,95}, {0,0,1,1,1,0}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test({50,55,53,52,54,56}, {1,4,2,1,1,0}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test({30,30,30,31,30,30,32}, {3,2,1,3,1,1,0}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test({35,33,34,32,36,31,37}, {4,1,2,1,2,1,0}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def dailyTemperatures(self, temperatures):
        # Write your code here — use a monotonic stack
        return []
# USER_CODE_END

def test(temps, expected, tc, hidden=False):
    got = CodeCoder().dailyTemperatures(temps)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:temps={temps}:expected={expected}:got={got}")

try: test([73,74,75,71,69,72,76,73], [1,1,4,2,1,1,0,0], 1)
except: print("TC:1:FAIL:hidden")
try: test([30,40,50], [1,1,0], 2)
except: print("TC:2:FAIL:hidden")
try: test([90,80,70], [0,0,0], 3)
except: print("TC:3:FAIL:hidden")
try: test([70], [0], 4)
except: print("TC:4:FAIL:hidden")
try: test([70,70,70,70], [0,0,0,0], 5)
except: print("TC:5:FAIL:hidden")
try: test([30,31,30,32,31,33], [1,2,1,2,1,0], 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([100,90,80,85,90,95], [0,0,1,1,1,0], 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([50,55,53,52,54,56], [1,4,2,1,1,0], 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([30,30,30,31,30,30,32], [3,2,1,3,1,1,0], 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([35,33,34,32,36,31,37], [4,1,2,1,2,1,0], 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function dailyTemperatures(temperatures) {
    // Write your code here — use a monotonic stack
    return [];
}
// USER_CODE_END

function test(temps, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = dailyTemperatures(temps);
    const gs = JSON.stringify(got);
    const es = JSON.stringify(expected);
    if (gs === es) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:temps=" + JSON.stringify(temps)
            + ":expected=" + es + ":got=" + gs);
    }
}

try { test([73,74,75,71,69,72,76,73], [1,1,4,2,1,1,0,0], 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([30,40,50], [1,1,0], 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([90,80,70], [0,0,0], 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([70], [0], 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([70,70,70,70], [0,0,0,0], 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([30,31,30,32,31,33], [1,2,1,2,1,0], 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test([100,90,80,85,90,95], [0,0,1,1,1,0], 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test([50,55,53,52,54,56], [1,4,2,1,1,0], 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test([30,30,30,31,30,30,32], [3,2,1,3,1,1,0], 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test([35,33,34,32,36,31,37], [4,1,2,1,2,1,0], 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* dailyTemperatures(int* temperatures, int temperaturesSize, int* returnSize) {
    // Write your code here — use a monotonic stack
    // Allocate and return an array of size temperaturesSize
    *returnSize = 0;
    return NULL;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void runTest(int* temps, int n, int* expected, int tc, int hidden) {
    int retSize;
    int* got = dailyTemperatures(temps, n, &retSize);
    int ok = (retSize == n && arrEq(got, expected, n));
    if (ok) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:hidden\n", tc);
    }
}

int main() {
    int t1[] = {73,74,75,71,69,72,76,73};
    int e1[] = {1,1,4,2,1,1,0,0};
    runTest(t1, 8, e1, 1, 0);

    int t2[] = {30,40,50};
    int e2[] = {1,1,0};
    runTest(t2, 3, e2, 2, 0);

    int t3[] = {90,80,70};
    int e3[] = {0,0,0};
    runTest(t3, 3, e3, 3, 0);

    int t4[] = {70};
    int e4[] = {0};
    runTest(t4, 1, e4, 4, 0);

    int t5[] = {70,70,70,70};
    int e5[] = {0,0,0,0};
    runTest(t5, 4, e5, 5, 0);

    int t6[] = {30,31,30,32,31,33};
    int e6[] = {1,2,1,2,1,0};
    runTest(t6, 6, e6, 6, 1);

    int t7[] = {100,90,80,85,90,95};
    int e7[] = {0,0,1,1,1,0};
    runTest(t7, 6, e7, 7, 1);

    int t8[] = {50,55,53,52,54,56};
    int e8[] = {1,4,2,1,1,0};
    runTest(t8, 6, e8, 8, 1);

    int t9[] = {30,30,30,31,30,30,32};
    int e9[] = {3,2,1,3,1,1,0};
    runTest(t9, 7, e9, 9, 1);

    int t10[] = {35,33,34,32,36,31,37};
    int e10[] = {4,1,2,1,2,1,0};
    runTest(t10, 7, e10, 10, 1);

    return 0;
}'''

for lang, code in [
    ("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code),
    ("JAVASCRIPT", js_code), ("C", c_code),
]:
    cur.execute(
        "INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) "
        "VALUES (%s, %s, %s, NOW(), NOW())",
        (pid, lang, code)
    )
conn.commit()

cur.execute(
    "SELECT language, LENGTH(solution_template) FROM code_snippets WHERE problem_id = %s ORDER BY language",
    (pid,)
)
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\nDaily Temperatures (pid={pid}) — done!")
cur.close()
conn.close()
