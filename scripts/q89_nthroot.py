"""
Find Nth Root of a Number
=========================
Given two integers n and m, find the nth root of m — the integer x such that
x^n == m. If no such integer exists (m is not a perfect nth power), return -1.

Examples:
  n = 3, m = 8  -> 2   (2^3 = 8)
  n = 3, m = 9  -> -1  (no integer x with x^3 = 9)

Binary search the answer in [1, m]: if mid^n == m return mid; if mid^n < m
search the right half; else the left half. If the loop ends with no exact
match, return -1. Use a 64-bit type (or compare mid with m^(1/n)-style
guards) to avoid overflow when computing mid^n.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find Nth Root of a Number"
desc=(
    "Given two integers n and m, return the integer nth root of m — the value "
    "x such that x^n == m. If m is NOT a perfect nth power (no integer x "
    "satisfies x^n == m), return -1.\n\n"
    "For example:\n"
    "n = 3, m = 8  -> 2  (2^3 = 8)\n"
    "n = 3, m = 9  -> -1 (no integer x with x^3 = 9)\n\n"
    "Binary search the answer in [1, m]. If mid^n == m, return mid. If mid^n < "
    "m, the root is to the right; otherwise to the left. If the search ends "
    "with no exact match, return -1. Compute mid^n in a 64-bit type (long long "
    "/ long) to avoid overflow."
)
infmt="Two integers n and m on a single line."
outfmt="Print the integer nth root of m if m is a perfect nth power, else -1."
cons="1 ≤ n, m ≤ 10^9\nReturn -1 when no integer nth root exists."
e1="Input:\n3 8\n\nOutput:\n2"
e2="Input:\n3 9\n\nOutput:\n-1"
e3="Input:\n2 16\n\nOutput:\n4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Math, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int nthRoot(int n, int m) {
        // Write your code here — binary search for x where x^n == m, else -1
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,int m,int e,int tc,boolean h){int r=new CodeCoder().nthRoot(n,m);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":m="+m+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(3,8,2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(3,9,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(2,16,4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(3,27,3,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(4,81,3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(2,10,-1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(3,1,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(5,32,2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(2,1000000,1000,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(3,64,4,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int nthRoot(int n,int m){return 0;}};
// USER_CODE_END
void test(int n,int m,int e,int tc,bool h=false){int r=CodeCoder().nthRoot(n,m);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test(3,8,2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(3,9,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(2,16,4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(3,27,3,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(4,81,3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(2,10,-1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(3,1,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(5,32,2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(2,1000000,1000,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(3,64,4,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def nthRoot(self, n, m):
        return 0
# USER_CODE_END
def test(n,m,e,tc,h=False):r=CodeCoder().nthRoot(n,m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:m={m}:exp={e}:got={r}"))
try:test(3,8,2,1)
except:print("TC:1:FAIL:hidden")
try:test(3,9,-1,2)
except:print("TC:2:FAIL:hidden")
try:test(2,16,4,3)
except:print("TC:3:FAIL:hidden")
try:test(3,27,3,4)
except:print("TC:4:FAIL:hidden")
try:test(4,81,3,5)
except:print("TC:5:FAIL:hidden")
try:test(2,10,-1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(3,1,1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(5,32,2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(2,1000000,1000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(3,64,4,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function nthRoot(n, m) { return 0; }
// USER_CODE_END
function test(n,m,e,tc,h){if(h===undefined)h=false;const r=nthRoot(n,m);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test(3,8,2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(3,9,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(2,16,4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(3,27,3,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(4,81,3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(2,10,-1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(3,1,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(5,32,2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(2,1000000,1000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(3,64,4,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int nthRoot(int n, int m) {
    // Write your code here — return x where x^n == m, else -1
    return 0;
}
// USER_CODE_END

void runTest(int n,int m,int e,int tc,int h){
    int r=nthRoot(n,m);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    runTest(3,8,2,1,0);
    runTest(3,9,-1,2,0);
    runTest(2,16,4,3,0);
    runTest(3,27,3,4,0);
    runTest(4,81,3,5,0);
    runTest(2,10,-1,6,1);
    runTest(3,1,1,7,1);
    runTest(5,32,2,8,1);
    runTest(2,1000000,1000,9,1);
    runTest(3,64,4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
