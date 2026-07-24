import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Add Digits"
desc = "Given an integer num, repeatedly add all its digits until the result has only one digit, and return it."
infmt = "Single line containing integer num."
outfmt = "Print the single-digit result."
cons = "0 \u2264 num \u2264 2\u00b9\u00b9-1"
e1 = "Input:\n38\n\nOutput:\n2\n\nExplanation: 3+8=11, 1+1=2"
e2 = "Input:\n0\n\nOutput:\n0"
e3 = "Input:\n10\n\nOutput:\n1"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True, "Math", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

for lang, code in [("JAVA", '''import java.util.*;

// USER_CODE_START
class Solution {
    public int addDigits(int num) {
        // Write your code here
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int n, int e, int tc, boolean h) {
        int g = new Solution().addDigits(n);
        if (g == e) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:n=" + n + ":expected=" + e + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(38, 2, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(0, 0, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(10, 1, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(12345, 6, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(99, 9, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(100, 1, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''),
("CPP", '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    int addDigits(int num) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

void test(int n, int e, int tc, bool h) {
    int g = Solution().addDigits(n);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:n=" << n << ":expected=" << e << ":got=" << g << "\\n";
}

int main() {
    try { test(38, 2, 1, false); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test(0, 0, 2, false); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test(10, 1, 3, false); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test(12345, 6, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test(99, 9, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test(100, 1, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''),
("PYTHON", '''# USER_CODE_START
class Solution:
    def addDigits(self, num):
        # Write your code here
        return 0
# USER_CODE_END

def test(n, e, tc, hidden=False):
    g = Solution().addDigits(n)
    if g == e: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}")

try: test(38, 2, 1)
except: print("TC:1:FAIL:hidden")
try: test(0, 0, 2)
except: print("TC:2:FAIL:hidden")
try: test(10, 1, 3)
except: print("TC:3:FAIL:hidden")
try: test(12345, 6, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test(99, 9, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test(100, 1, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''),
("JAVASCRIPT", '''// USER_CODE_START
function addDigits(num) {
    // Write your code here
    return 0;
}
// USER_CODE_END

function test(n, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = addDigits(n);
    if (g === e) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:n=" + n + ":expected=" + e + ":got=" + g);
}

try { test(38, 2, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test(0, 0, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test(10, 1, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test(12345, 6, 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test(99, 9, 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test(100, 1, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''),
("C", '''#include <stdio.h>

// USER_CODE_START
int addDigits(int num) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void test(int n, int e, int tc, int h) {
    int g = addDigits(n);
    if (g == e) { if (h) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
    else { if (h) printf("TC:%d:FAIL:hidden\\n", tc); else printf("TC:%d:FAIL:n=%d:expected=%d:got=%d\\n", tc, n, e, g); }
}

int main() {
    test(38, 2, 1, 0); test(0, 0, 2, 0); test(10, 1, 3, 0);
    test(12345, 6, 4, 1); test(99, 9, 5, 1); test(100, 1, 6, 1);
    return 0;
}''')]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
