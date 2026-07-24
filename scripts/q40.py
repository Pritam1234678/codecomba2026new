import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Fibonacci Number"
desc = "The Fibonacci numbers, commonly denoted F(n), form a sequence where each number is the sum of the two preceding ones, starting from 0 and 1. That is, F(0)=0, F(1)=1, and for n > 1, F(n)=F(n-1)+F(n-2). Given n, calculate F(n)."
infmt = "Single line containing integer n."
outfmt = "Print the nth Fibonacci number."
cons = "0 \u2264 n \u2264 30"
e1 = "Input:\n2\n\nOutput:\n1\n\nExplanation: F(2)=F(1)+F(0)=1+0=1"
e2 = "Input:\n3\n\nOutput:\n2"
e3 = "Input:\n4\n\nOutput:\n3"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True, "Math, Recursion", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public int fib(int n) {
        // Write your code here
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int n, int e, int tc, boolean h) {
        int g = new Solution().fib(n);
        if (g == e) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:n=" + n + ":expected=" + e + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(0, 0, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(1, 1, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(5, 5, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(10, 55, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(3, 2, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(20, 6765, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    int fib(int n) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

void test(int n, int e, int tc, bool h = false) {
    int g = Solution().fib(n);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:n=" << n << ":expected=" << e << ":got=" << g << "\\n";
}

int main() {
    try { test(0, 0, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test(1, 1, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test(5, 5, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test(10, 55, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test(3, 2, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test(20, 6765, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def fib(self, n):
        # Write your code here
        return 0
# USER_CODE_END

def test(n, e, tc, hidden=False):
    g = Solution().fib(n)
    if g == e: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}")

try: test(0, 0, 1)
except: print("TC:1:FAIL:hidden")
try: test(1, 1, 2)
except: print("TC:2:FAIL:hidden")
try: test(5, 5, 3)
except: print("TC:3:FAIL:hidden")
try: test(10, 55, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test(3, 2, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test(20, 6765, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function fib(n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

function test(n, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = fib(n);
    if (g === e) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:n=" + n + ":expected=" + e + ":got=" + g);
}

try { test(0, 0, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test(1, 1, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test(5, 5, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test(10, 55, 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test(3, 2, 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test(20, 6765, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>

// USER_CODE_START
int fib(int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void test(int n, int e, int tc, int h) {
    int g = fib(n);
    if (g == e) { if (h) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
    else { if (h) printf("TC:%d:FAIL:hidden\\n", tc); else printf("TC:%d:FAIL:n=%d:expected=%d:got=%d\\n", tc, n, e, g); }
}

int main() {
    test(0, 0, 1, 0); test(1, 1, 2, 0); test(5, 5, 3, 0);
    test(10, 55, 4, 1); test(3, 2, 5, 1); test(20, 6765, 6, 1);
    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
