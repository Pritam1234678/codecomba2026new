"""
Guess Number Higher or Lower
==============================
We are playing the Guess Game. The game is as follows: I pick a number from
1 to n. You have to guess which number I picked. Every time you guess wrong,
I will tell you whether the number I picked is higher or lower than your guess.

You call a pre-defined API guess(int num) which returns:
  -1: my number is lower (your guess is too high)
   1: my number is higher (your guess is too low)
   0: congratulations, you guessed it!

Implement guessNumber(int n) to find the picked number using binary search.

Examples:
  n=10, pick=6 → 6
  n=1, pick=1 → 1

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Guess Number Higher or Lower"
desc=(
    "We are playing the Guess Game. The game is as follows: I pick a number from "
    "1 to n. You have to guess which number I picked.\n\n"
    "Every time you guess wrong, I will tell you whether the number I picked is "
    "higher or lower than your guess.\n\n"
    "You call a pre-defined API guess(int num) which returns one of three possible results:\n"
    "-1: My number is lower (your guess is too high)\n"
    " 1: My number is higher (your guess is too low)\n"
    " 0: Congratulations! You got it!\n\n"
    "Implement the function guessNumber(int n) that finds the picked number, "
    "using binary search. The picked number is always between 1 and n."
)
infmt="First line contains n.\nSecond line contains the hidden picked number (for testing)."
outfmt="Print the picked number."
cons="1 ≤ n ≤ 2^31 - 1\n1 ≤ pick ≤ n"
e1="Input:\n10\n6\n\nOutput:\n6"
e2="Input:\n1\n1\n\nOutput:\n1"
e3="Input:\n100\n57\n\nOutput:\n57"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
// The guess API is already defined for you:
// int guess(int num);  // returns -1, 1, or 0
class CodeCoder {
    public int guessNumber(int n) {
        // Write your code here — binary search using guess() API
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static int pick;

    static int guess(int num) {
        if (num > pick) return -1;
        if (num < pick) return 1;
        return 0;
    }

    static void test(int n, int p, int tc, boolean h) {
        pick = p;
        int g = new CodeCoder().guessNumber(n);
        if (g == p) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:n=" + n + ":exp=" + p + ":got=" + g);
    }

    public static void main(String[] a) {
        try { test(10, 6, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(1, 1, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(100, 57, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(5, 1, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(50, 50, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(1000, 999, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(2147483647, 1, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(2147483647, 2147483647, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(100, 50, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(1000000, 500000, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
// The guess API is already defined:
// int guess(int num);  // returns -1, 1, or 0
class CodeCoder {
public:
    int guessNumber(int n) {
        // Write your code here — binary search using guess() API
        return 0;
    }
};
// USER_CODE_END

int pick;

int guess(int num) {
    if (num > pick) return -1;
    if (num < pick) return 1;
    return 0;
}

void test(int n, int p, int tc, bool h = false) {
    pick = p;
    int g = CodeCoder().guessNumber(n);
    if (g == p) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:n=" << n << ":exp=" << p << ":got=" << g << "\\n";
}
int main() {
    try { test(10, 6, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test(1, 1, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test(100, 57, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test(5, 1, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test(50, 50, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test(1000, 999, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test(2147483647, 1, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test(2147483647, 2147483647, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test(100, 50, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test(1000000, 500000, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# The guess API is already defined for you:
# def guess(num: int) -> int:  # returns -1, 1, or 0
class CodeCoder:
    def guessNumber(self, n):
        # Write your code here — binary search using guess() API
        return 0
# USER_CODE_END

_pick = 0

def guess(num):
    if num > _pick: return -1
    if num < _pick: return 1
    return 0

def test(n, p, tc, h=False):
    global _pick
    _pick = p
    g = CodeCoder().guessNumber(n)
    if g == p: print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:n={n}:exp={p}:got={g}")

try: test(10, 6, 1)
except: print("TC:1:FAIL:hidden")
try: test(1, 1, 2)
except: print("TC:2:FAIL:hidden")
try: test(100, 57, 3)
except: print("TC:3:FAIL:hidden")
try: test(5, 1, 4)
except: print("TC:4:FAIL:hidden")
try: test(50, 50, 5)
except: print("TC:5:FAIL:hidden")
try: test(1000, 999, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test(2147483647, 1, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test(2147483647, 2147483647, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test(100, 50, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test(1000000, 500000, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// The guess API is already defined for you:
// function guess(num) { ... }  // returns -1, 1, or 0
function guessNumber(n) {
    // Write your code here — binary search using guess() API
    return 0;
}
// USER_CODE_END

let pick = 0;

function guess(num) {
    if (num > pick) return -1;
    if (num < pick) return 1;
    return 0;
}

function test(n, p, tc, h) {
    if (h === undefined) h = false;
    pick = p;
    const g = guessNumber(n);
    if (g === p) console.log("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
    else if (h) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:n=" + n + ":exp=" + p + ":got=" + g);
}
try { test(10, 6, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test(1, 1, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test(100, 57, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test(5, 1, 4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test(50, 50, 5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test(1000, 999, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test(2147483647, 1, 7, true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test(2147483647, 2147483647, 8, true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test(100, 50, 9, true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test(1000000, 500000, 10, true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>

// USER_CODE_START
// The guess API is already defined:
// int guess(int num);  // returns -1, 1, or 0
int guessNumber(int n) {
    // Write your code here — binary search using guess() API
    return 0;
}
// USER_CODE_END

int pick;

int guess(int num) {
    if (num > pick) return -1;
    if (num < pick) return 1;
    return 0;
}

void runTest(int n, int p, int tc, int h) {
    pick = p;
    int g = guessNumber(n);
    if (g == p) { if (h) printf("TC:%d:PASS:hidden\\n", tc); else printf("TC:%d:PASS\\n", tc); }
    else { if (h) printf("TC:%d:FAIL:hidden\\n", tc); else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n", tc, n, p, g); }
}
int main() {
    runTest(10, 6, 1, 0);
    runTest(1, 1, 2, 0);
    runTest(100, 57, 3, 0);
    runTest(5, 1, 4, 0);
    runTest(50, 50, 5, 0);
    runTest(1000, 999, 6, 1);
    runTest(2147483647, 1, 7, 1);
    runTest(2147483647, 2147483647, 8, 1);
    runTest(100, 50, 9, 1);
    runTest(1000000, 500000, 10, 1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
