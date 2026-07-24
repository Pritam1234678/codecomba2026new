import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Pow(x, n)"
desc = "Implement pow(x, n), which calculates x raised to the power n (x^n)."
infmt = "First line contains double x.\nSecond line contains integer n."
outfmt = "Print x raised to power n (rounded to 5 decimal places)."
cons = "-100.0 < x < 100.0\n-2\u00b9\u00b9 \u2264 n \u2264 2\u00b9\u00b9-1\nn is a 32-bit signed integer."
e1 = "Input:\n2.00000\n10\n\nOutput:\n1024.00000"
e2 = "Input:\n2.10000\n3\n\nOutput:\n9.26100"
e3 = "Input:\n2.00000\n-2\n\nOutput:\n0.25000"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Math, Recursion", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public double myPow(double x, int n) {
        // Write your code here
        return 0.0;
    }
}
// USER_CODE_END

public class Main {
    static void test(double x, int n, double e, int tc, boolean h) {
        double g = new Solution().myPow(x, n);
        if (Math.abs(g - e) < 1e-5) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:x=" + x + " n=" + n + ":expected=" + e + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(2.0, 10, 1024.0, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(2.1, 3, 9.261, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(2.0, -2, 0.25, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(1.0, 1000000000, 1.0, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(-2.0, 3, -8.0, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(0.5, 2, 0.25, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    double myPow(double x, int n) {
        // Write your code here
        return 0.0;
    }
};
// USER_CODE_END

void test(double x, int n, double e, int tc, bool h = false) {
    double g = Solution().myPow(x, n);
    if (abs(g - e) < 1e-5) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:x=" << x << " n=" << n << ":expected=" << e << ":got=" << g << "\\n";
}

int main() {
    try { test(2.0, 10, 1024.0, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test(2.1, 3, 9.261, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test(2.0, -2, 0.25, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test(1.0, 1000000000, 1.0, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test(-2.0, 3, -8.0, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test(0.5, 2, 0.25, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def myPow(self, x, n):
        # Write your code here
        return 0.0
# USER_CODE_END

def test(x, n, e, tc, hidden=False):
    g = Solution().myPow(x, n)
    if abs(g - e) < 1e-5: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:x={x}:n={n}:expected={e}:got={g}")

try: test(2.0, 10, 1024.0, 1)
except: print("TC:1:FAIL:hidden")
try: test(2.1, 3, 9.261, 2)
except: print("TC:2:FAIL:hidden")
try: test(2.0, -2, 0.25, 3)
except: print("TC:3:FAIL:hidden")
try: test(1.0, 1000000000, 1.0, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test(-2.0, 3, -8.0, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test(0.5, 2, 0.25, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function myPow(x, n) {
    // Write your code here
    return 0.0;
}
// USER_CODE_END

function test(x, n, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = myPow(x, n);
    if (Math.abs(g - e) < 1e-5) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:x=" + x + ":n=" + n + ":expected=" + e + ":got=" + g);
}

try { test(2.0, 10, 1024.0, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test(2.1, 3, 9.261, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test(2.0, -2, 0.25, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test(1.0, 1000000000, 1.0, 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test(-2.0, 3, -8.0, 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test(0.5, 2, 0.25, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <math.h>

// USER_CODE_START
double myPow(double x, int n) {
    // Write your code here
    return 0.0;
}
// USER_CODE_END

void test(double x, int n, double e, int tc, int h) {
    double g = myPow(x, n);
    if (fabs(g - e) < 1e-5) { if (h) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
    else { if (h) printf("TC:%d:FAIL:hidden\\n", tc); else printf("TC:%d:FAIL:x=%.5f:n=%d:expected=%.5f:got=%.5f\\n", tc, x, n, e, g); }
}

int main() {
    test(2.0, 10, 1024.0, 1, 0); test(2.1, 3, 9.261, 2, 0); test(2.0, -2, 0.25, 3, 0);
    test(1.0, 1000000000, 1.0, 4, 1); test(-2.0, 3, -8.0, 5, 1); test(0.5, 2, 0.25, 6, 1);
    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
