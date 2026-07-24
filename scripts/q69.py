"""
Star / Number Pattern Printing
================================
Given an integer n, print a star pattern or number pattern depending on
the problem variant. For this problem, print a right-angled triangle pattern
of stars (*) with n rows.

Example for n = 5:
*
* *
* * *
* * * *
* * * * *

For n = 3:
*
* *
* * *

The pattern has n rows. Row i contains i stars separated by spaces.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Star / Number Pattern Printing"
desc=(
    "Given an integer n, print a right-angled triangle pattern of stars (*) with n rows.\n\n"
    "For n = 5, the output should be:\n"
    "*\n"
    "* *\n"
    "* * *\n"
    "* * * *\n"
    "* * * * *\n\n"
    "Each row i (1-indexed) contains exactly i stars separated by single spaces. "
    "There are no leading or trailing spaces on any line. "
    "Use nested loops: outer loop for rows, inner loop for stars in each row."
)
infmt="Single line containing integer n."
outfmt="Print n lines, each containing i stars separated by spaces."
cons="1 \u2264 n \u2264 100"
e1="Input:\n5\n\nOutput:\n*\n* *\n* * *\n* * * *\n* * * * *"
e2="Input:\n3\n\nOutput:\n*\n* *\n* * *"
e3="Input:\n1\n\nOutput:\n*"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Simulation, Pattern",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String printPattern(int n) {
        // Write your code here — build pattern as a single string
        // Each row: i stars separated by spaces, rows separated by newline
        return "";
    }
}
// USER_CODE_END

public class Main {
    static void test(int n, String expected, int tc, boolean hidden) {
        String got = new CodeCoder().printPattern(n).trim();
        if (got.equals(expected.trim())) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:n=" + n + ":expected=\\n" + expected + ":got=\\n" + got);
        }
    }
    public static void main(String[] a) {
        try { test(1, "*", 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(2, "*\\n* *", 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(3, "*\\n* *\\n* * *", 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(4, "*\\n* *\\n* * *\\n* * * *", 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(5, "*\\n* *\\n* * *\\n* * * *\\n* * * * *", 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(6, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *", 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(7, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *", 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(8, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *", 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(9, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *", 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(10, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *\\n* * * * * * * * * *", 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    string printPattern(int n) {
        // Write your code here — build pattern string
        return "";
    }
};
// USER_CODE_END

void test(int n, string e, int tc, bool h = false) {
    string g = CodeCoder().printPattern(n);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:n=" << n << ":got=" << g << ":exp=" << e << "\\n";
}
int main() {
string e1="*",e2="*\\n* *",e3="*\\n* *\\n* * *",e4="*\\n* *\\n* * *\\n* * * *",e5="*\\n* *\\n* * *\\n* * * *\\n* * * * *";
string e6=e5+"\\n* * * * * *",e7=e6+"\\n* * * * * * *",e8=e7+"\\n* * * * * * * *",e9=e8+"\\n* * * * * * * * *",e10=e9+"\\n* * * * * * * * * *";
try{test(1,e1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(2,e2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(3,e3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(4,e4,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(5,e5,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(6,e6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(7,e7,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(8,e8,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(9,e9,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(10,e10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def printPattern(self, n):
        # Write your code here — build pattern string
        return ""
# USER_CODE_END

def test(n, e, tc, hidden=False):
    g = CodeCoder().printPattern(n).strip()
    if g == e.strip(): print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:n={n}:got={repr(g)}:exp={repr(e)}")

try: test(1, "*", 1)
except: print("TC:1:FAIL:hidden")
try: test(2, "*\\n* *", 2)
except: print("TC:2:FAIL:hidden")
try: test(3, "*\\n* *\\n* * *", 3)
except: print("TC:3:FAIL:hidden")
try: test(4, "*\\n* *\\n* * *\\n* * * *", 4)
except: print("TC:4:FAIL:hidden")
try: test(5, "*\\n* *\\n* * *\\n* * * *\\n* * * * *", 5)
except: print("TC:5:FAIL:hidden")
try: test(6, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *", 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test(7, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *", 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test(8, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *", 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test(9, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *", 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test(10, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *\\n* * * * * * * * * *", 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function printPattern(n) {
    // Write your code here — build pattern string
    return "";
}
// USER_CODE_END

function test(n, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = printPattern(n);
    if (g === e) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:n=" + n + ":got=" + JSON.stringify(g) + ":exp=" + JSON.stringify(e));
}

try { test(1, "*", 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test(2, "*\\n* *", 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test(3, "*\\n* *\\n* * *", 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test(4, "*\\n* *\\n* * *\\n* * * *", 4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test(5, "*\\n* *\\n* * *\\n* * * *\\n* * * * *", 5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test(6, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *", 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test(7, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *", 7, true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test(8, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *", 8, true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test(9, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *", 9, true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test(10, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *\\n* * * * * * * * * *", 10, true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void printPattern(int n, char* out) {
    // Write your code here — store result in 'out'
    // Format: each row has i stars separated by spaces, rows separated by newline
    out[0] = '\\0';
}
// USER_CODE_END

void runTest(int n, char* e, int tc, int hidden) {
    char out[5000] = {0};
    printPattern(n, out);
    if (strcmp(out, e) == 0) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:n=%d:got=%s:exp=%s\\n", tc, n, out, e);
    }
}
int main() {
    runTest(1, "*", 1, 0);
    runTest(2, "*\\n* *", 2, 0);
    runTest(3, "*\\n* *\\n* * *", 3, 0);
    runTest(4, "*\\n* *\\n* * *\\n* * * *", 4, 0);
    runTest(5, "*\\n* *\\n* * *\\n* * * *\\n* * * * *", 5, 0);
    runTest(6, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *", 6, 1);
    runTest(7, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *", 7, 1);
    runTest(8, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *", 8, 1);
    runTest(9, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *", 9, 1);
    runTest(10, "*\\n* *\\n* * *\\n* * * *\\n* * * * *\\n* * * * * *\\n* * * * * * *\\n* * * * * * * *\\n* * * * * * * * *\\n* * * * * * * * * *", 10, 1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
