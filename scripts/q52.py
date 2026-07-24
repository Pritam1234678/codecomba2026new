"""
Prime Factors of a Number
=========================
Given an integer n, find all its prime factors. A prime factor is a factor
of n that is also a prime number.

Examples:
  n = 12  → prime factors: 2, 2, 3   (since 2 × 2 × 3 = 12)
  n = 315 → prime factors: 3, 3, 5, 7 (since 3 × 3 × 5 × 7 = 315)
  n = 17  → prime factors: 17          (17 is prime itself)

Algorithm: Repeatedly divide n by the smallest prime (2), then 3, then 5, etc.
For each divisor, keep dividing n by it while it divides n completely.
After sqrt(n) check, if remaining n > 1, it's also a prime factor.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder.
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Prime Factors of a Number"
desc = (
    "Given a positive integer n, find all its prime factors and print them "
    "in ascending order. A prime factor is a factor of n that is also a prime number.\n\n"
    "For example, n = 12 can be written as 2 × 2 × 3, so its prime factors are [2, 2, 3].\n\n"
    "Algorithm:\n"
    "1. While n is divisible by 2, print 2 and divide n by 2.\n"
    "2. For odd numbers i from 3 to sqrt(n), while n is divisible by i, print i and divide n by i.\n"
    "3. If the remaining n > 1, it is also a prime factor.\n\n"
    "A prime number (like 17) has only itself as a prime factor."
)
infmt = "Single line containing integer n."
outfmt = "Print all prime factors separated by spaces in ascending order."
cons = (
    "2 \u2264 n \u2264 10^9"
)
e1 = (
    "Input:\n"
    "12\n\n"
    "Output:\n"
    "2 2 3\n\n"
    "Explanation: 12 = 2 × 2 × 3. Start by dividing by 2 (twice), then by 3."
)
e2 = (
    "Input:\n"
    "315\n\n"
    "Output:\n"
    "3 3 5 7\n\n"
    "Explanation: 315 = 3 × 105 = 3 × 3 × 35 = 3 × 3 × 5 × 7."
)
e3 = (
    "Input:\n"
    "17\n\n"
    "Output:\n"
    "17\n\n"
    "Explanation: 17 is prime, so the only prime factor is 17 itself."
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True,
     "Math, Number Theory", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] primeFactors(int n) {
        // Write your code here — find all prime factors of n
        return new int[0];
    }
}
// USER_CODE_END

public class Main {
    static void test(int n, int[] expected, int tc, boolean hidden) {
        int[] got = new CodeCoder().primeFactors(n);
        if (Arrays.equals(got, expected)) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:n=" + n
                + ":expected=" + Arrays.toString(expected)
                + ":got=" + Arrays.toString(got));
        }
    }

    public static void main(String[] args) {
        try { test(12, new int[]{2, 2, 3}, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test(315, new int[]{3, 3, 5, 7}, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test(17, new int[]{17}, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test(100, new int[]{2, 2, 5, 5}, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(2, new int[]{2}, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(64, new int[]{2, 2, 2, 2, 2, 2}, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test(9973, new int[]{9973}, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test(1024, new int[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2}, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test(123456, new int[]{2, 2, 2, 2, 2, 2, 3, 643}, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test(999999937, new int[]{999999937}, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    vector<int> primeFactors(int n) {
        // Write your code here — find all prime factors of n
        return {};
    }
};
// USER_CODE_END

void test(int n, vector<int> expected, int tc, bool hidden = false) {
    vector<int> got = CodeCoder().primeFactors(n);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:n=" << n << ":expected=[";
        for (size_t i = 0; i < expected.size(); i++) { if (i) cout << ","; cout << expected[i]; }
        cout << "]:got=[";
        for (size_t i = 0; i < got.size(); i++) { if (i) cout << ","; cout << got[i]; }
        cout << "]\n";
    }
}

int main() {
    try { test(12, {2, 2, 3}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test(315, {3, 3, 5, 7}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test(17, {17}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test(100, {2, 2, 5, 5}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test(2, {2}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test(64, {2,2,2,2,2,2}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test(9973, {9973}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test(1024, {2,2,2,2,2,2,2,2,2,2}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test(123456, {2,2,2,2,2,2,3,643}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test(999999937, {999999937}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def primeFactors(self, n):
        # Write your code here — find all prime factors of n
        return []
# USER_CODE_END

def test(n, expected, tc, hidden=False):
    got = CodeCoder().primeFactors(n)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:n={n}:expected={expected}:got={got}")

try: test(12, [2, 2, 3], 1)
except: print("TC:1:FAIL:hidden")
try: test(315, [3, 3, 5, 7], 2)
except: print("TC:2:FAIL:hidden")
try: test(17, [17], 3)
except: print("TC:3:FAIL:hidden")
try: test(100, [2, 2, 5, 5], 4)
except: print("TC:4:FAIL:hidden")
try: test(2, [2], 5)
except: print("TC:5:FAIL:hidden")
try: test(64, [2,2,2,2,2,2], 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test(9973, [9973], 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test(1024, [2,2,2,2,2,2,2,2,2,2], 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test(123456, [2,2,2,2,2,2,3,643], 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test(999999937, [999999937], 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function primeFactors(n) {
    // Write your code here — find all prime factors of n
    return [];
}
// USER_CODE_END

function test(n, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = primeFactors(n);
    const gs = JSON.stringify(got);
    const es = JSON.stringify(expected);
    if (gs === es) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:n=" + n + ":expected=" + es + ":got=" + gs);
    }
}

try { test(12, [2, 2, 3], 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test(315, [3, 3, 5, 7], 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test(17, [17], 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test(100, [2, 2, 5, 5], 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test(2, [2], 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test(64, [2,2,2,2,2,2], 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test(9973, [9973], 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test(1024, [2,2,2,2,2,2,2,2,2,2], 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test(123456, [2,2,2,2,2,2,3,643], 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test(999999937, [999999937], 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
// Write a function that returns an array of all prime factors of n in ascending order.
// Store the count in *returnSize.
int* primeFactors(int n, int* returnSize) {
    // Write your code here
    *returnSize = 0;
    return NULL;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void runTest(int n, int* expected, int expN, int tc, int hidden) {
    int retSize;
    int* got = primeFactors(n, &retSize);
    int ok = (retSize == expN && arrEq(got, expected, retSize));
    if (ok) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:n=%d:expected=%d factors:got=%d factors\n", tc, n, expN, retSize);
    }
}

int main() {
    int e1[] = {2, 2, 3};
    runTest(12, e1, 3, 1, 0);

    int e2[] = {3, 3, 5, 7};
    runTest(315, e2, 4, 2, 0);

    int e3[] = {17};
    runTest(17, e3, 1, 3, 0);

    int e4[] = {2, 2, 5, 5};
    runTest(100, e4, 4, 4, 0);

    int e5[] = {2};
    runTest(2, e5, 1, 5, 0);

    int e6[] = {2,2,2,2,2,2};
    runTest(64, e6, 6, 6, 1);

    int e7[] = {9973};
    runTest(9973, e7, 1, 7, 1);

    int e8[] = {2,2,2,2,2,2,2,2,2,2};
    runTest(1024, e8, 10, 8, 1);

    int e9[] = {2,2,2,2,2,2,3,643};
    runTest(123456, e9, 8, 9, 1);

    int e10[] = {999999937};
    runTest(999999937, e10, 1, 10, 1);

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

print(f"\nPrime Factors of a Number (pid={pid}) — done!")
cur.close()
conn.close()
