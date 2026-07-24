"""
GCD of Two Numbers (Euclidean Algorithm)
=========================================
Problem: Given two integers a and b, find their Greatest Common Divisor (GCD).
The GCD is the largest positive integer that divides both a and b.

Example: GCD(12, 8) = 4 (12 and 8 both divisible by 4)
         GCD(17, 5) = 1 (17 and 5 are co-prime)

All 5 language harnesses with 6 test cases (3 visible + 3 hidden).
C harness actually CALLS user function and verifies result.
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "GCD of Two Numbers"
desc = (
    "Given two positive integers a and b, find their GCD (Greatest Common Divisor) "
    "using the Euclidean algorithm.\n\n"
    "The Euclidean algorithm works by repeated division:\n"
    "1. If b == 0, then GCD(a, b) = a\n"
    "2. Otherwise, GCD(a, b) = GCD(b, a % b)\n\n"
    "For example, GCD(48, 18) = GCD(18, 12) = GCD(12, 6) = GCD(6, 0) = 6."
)
infmt = "First line contains integer a.\nSecond line contains integer b."
outfmt = "Print a single integer — the GCD of a and b."
cons = (
    "1 \u2264 a, b \u2264 10^9\n"
    "At least one of a or b is non-zero."
)
e1 = "Input:\n12\n8\n\nOutput:\n4\n\nExplanation: 12 and 8 are both divisible by 4. No larger integer divides both."
e2 = "Input:\n17\n5\n\nOutput:\n1\n\nExplanation: 17 and 5 are co-prime. Only 1 divides both."
e3 = "Input:\n48\n18\n\nOutput:\n6\n\nExplanation: GCD(48, 18) = 6 by repeated division."

# ── Insert problem ──
cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True, "Math", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

# ═══════════════════════════════════════════════════════════════
# JAVA HARNESS
# ═══════════════════════════════════════════════════════════════
java_code = r'''import java.util.*;

// USER_CODE_START
class Solution {
    public int gcd(int a, int b) {
        // Write your code here — implement Euclidean algorithm
        // Hint: while (b != 0) { int temp = b; b = a % b; a = temp; } return a;
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int a, int b, int expected, int tc, boolean hidden) {
        int got = new Solution().gcd(a, b);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:input=a=" + a + " b=" + b
                + ":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] args) {
        // ── Visible tests (user sees full input/expected on failure) ──
        try { test(12, 8, 4,   1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:input=a=12 b=8:expected=4:got=ERR"); }

        try { test(17, 5, 1,   2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:input=a=17 b=5:expected=1:got=ERR"); }

        try { test(48, 18, 6,  3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:input=a=48 b=18:expected=6:got=ERR"); }

        // ── Hidden tests (user sees only PASS/FAIL, no details) ──
        try { test(100, 100, 100,       4, true); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(1000000000, 1, 1,     5, true); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(54, 24, 6,            6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

# ═══════════════════════════════════════════════════════════════
# CPP HARNESS
# ═══════════════════════════════════════════════════════════════
cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    int gcd(int a, int b) {
        // Write your code here — implement Euclidean algorithm
        return 0;
    }
};
// USER_CODE_END

void test(int a, int b, int expected, int tc, bool hidden = false) {
    Solution sol;
    int got = sol.gcd(a, b);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:input=a=" << a << " b=" << b
             << ":expected=" << expected << ":got=" << got << "\n";
    }
}

int main() {
    try { test(12, 8, 4,      1); }
    catch (...) { cout << "TC:1:FAIL:hidden\n"; }

    try { test(17, 5, 1,      2); }
    catch (...) { cout << "TC:2:FAIL:hidden\n"; }

    try { test(48, 18, 6,     3); }
    catch (...) { cout << "TC:3:FAIL:hidden\n"; }

    try { test(100, 100, 100,       4, true); }
    catch (...) { cout << "TC:4:FAIL:hidden\n"; }

    try { test(1000000000, 1, 1,    5, true); }
    catch (...) { cout << "TC:5:FAIL:hidden\n"; }

    try { test(54, 24, 6,           6, true); }
    catch (...) { cout << "TC:6:FAIL:hidden\n"; }

    return 0;
}'''

# ═══════════════════════════════════════════════════════════════
# PYTHON HARNESS
# ═══════════════════════════════════════════════════════════════
py_code = r'''# USER_CODE_START
class Solution:
    def gcd(self, a, b):
        # Write your code here — implement Euclidean algorithm
        return 0
# USER_CODE_END

def test(a, b, expected, tc, hidden=False):
    got = Solution().gcd(a, b)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:input=a={a} b={b}:expected={expected}:got={got}")

# ── Visible tests ──
try: test(12, 8, 4, 1)
except: print("TC:1:FAIL:hidden")
try: test(17, 5, 1, 2)
except: print("TC:2:FAIL:hidden")
try: test(48, 18, 6, 3)
except: print("TC:3:FAIL:hidden")

# ── Hidden tests ──
try: test(100, 100, 100, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test(1000000000, 1, 1, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test(54, 24, 6, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

# ═══════════════════════════════════════════════════════════════
# JAVASCRIPT HARNESS
# ═══════════════════════════════════════════════════════════════
js_code = r'''// USER_CODE_START
function gcd(a, b) {
    // Write your code here — implement Euclidean algorithm
    return 0;
}
// USER_CODE_END

function test(a, b, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = gcd(a, b);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:input=a=" + a + " b=" + b
            + ":expected=" + expected + ":got=" + got);
    }
}

// ── Visible tests ──
try { test(12, 8, 4, 1); }
catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test(17, 5, 1, 2); }
catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test(48, 18, 6, 3); }
catch (e) { console.log("TC:3:FAIL:hidden"); }

// ── Hidden tests ──
try { test(100, 100, 100, 4, true); }
catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test(1000000000, 1, 1, 5, true); }
catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test(54, 24, 6, 6, true); }
catch (e) { console.log("TC:6:FAIL:hidden"); }'''

# ═══════════════════════════════════════════════════════════════
# C HARNESS
# ═══════════════════════════════════════════════════════════════
c_code = r'''#include <stdio.h>

// USER_CODE_START
int gcd(int a, int b) {
    // Write your code here — implement Euclidean algorithm
    // Return the greatest common divisor of a and b
    return 0;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void test(int a, int b, int expected, int tc, int hidden) {
    int got = gcd(a, b);
    if (got == expected) {
        if (hidden)
            printf("TC:%d:PASS:hidden\n", tc);
        else
            printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) {
            printf("TC:%d:FAIL:hidden\n", tc);
        } else {
            printf("TC:%d:FAIL:input=a=%d b=%d:expected=%d:got=%d\n",
                   tc, a, b, expected, got);
        }
    }
}

int main() {
    // Visible test cases
    test(12, 8, 4,         1, 0);   // Basic case
    test(17, 5, 1,         2, 0);   // Co-prime case
    test(48, 18, 6,        3, 0);   // Multi-step Euclidean

    // Hidden test cases
    test(100, 100, 100,    4, 1);   // Same numbers → GCD = number itself
    test(1000000000, 1, 1, 5, 1);   // Large a, b=1 → GCD = 1
    test(54, 24, 6,        6, 1);   // Medium numbers

    return 0;
}'''

# ── Insert all 5 snippets ──
for lang, code in [
    ("JAVA",       java_code),
    ("CPP",        cpp_code),
    ("PYTHON",     py_code),
    ("JAVASCRIPT", js_code),
    ("C",          c_code),
]:
    cur.execute(
        "INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) "
        "VALUES (%s, %s, %s, NOW(), NOW())",
        (pid, lang, code)
    )
conn.commit()

# ── Verify all 5 inserted ──
cur.execute(
    "SELECT language, LENGTH(solution_template) FROM code_snippets WHERE problem_id = %s ORDER BY language",
    (pid,)
)
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\nGCD of Two Numbers (pid={pid}) — all 5 harnesses inserted successfully!")
cur.close()
conn.close()
