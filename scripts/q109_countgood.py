"""
Count Good Numbers
====================
A digit string of length n is "good" if:
  - digits at EVEN indices (0-based) are even (0,2,4,6,8) — 5 choices each
  - digits at ODD indices are prime (2,3,5,7) — 4 choices each
Count the number of good digit strings of length exactly n, modulo 10^9 + 7.

Examples:
  n = 1 -> 5
  n = 2 -> 20
  n = 3 -> 100

Answer = 5^(number of even indices) * 4^(number of odd indices) mod (10^9+7).
Even indices count = (n+1)/2, odd indices count = n/2. Use fast modular
exponentiation (recursion: power(a, e) = power(a, e/2)^2 * (a if e odd else 1)).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Good Numbers"
desc=(
    "A digit string is good if the digits at EVEN indices (0-based) are all "
    "even (0, 2, 4, 6, 8 — 5 choices) and the digits at ODD indices are all "
    "prime (2, 3, 5, 7 — 4 choices). Given a length n, count how many good "
    "digit strings of length exactly n exist, modulo 10^9 + 7.\n\n"
    "For example:\n"
    "n = 1 -> 5\n"
    "n = 2 -> 20\n"
    "n = 3 -> 100\n\n"
    "Every position is independent, so the answer is 5^(evenCount) * "
    "4^(oddCount) mod (10^9+7), where evenCount = (n+1)/2 and oddCount = n/2. "
    "Use fast modular exponentiation (recursive: power(a, e) = "
    "power(a, e/2)^2 * (a if e is odd else 1) mod M)."
)
infmt="A single integer n (the length of the digit string)."
outfmt="Print the number of good digit strings of length n modulo 10^9 + 7."
cons="1 ≤ n ≤ 10^9\nReturn the answer modulo 10^9 + 7."
e1="Input:\n1\n\nOutput:\n5"
e2="Input:\n2\n\nOutput:\n20"
e3="Input:\n3\n\nOutput:\n100"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"EASY",True,"Math, Recursion, Fast Exponentiation",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countGoodNumbers(long n) {
        // Write your code here — 5^even * 4^odd mod (1e9+7)
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(long n,long e,int tc,boolean hd){int r=new CodeCoder().countGoodNumbers(n);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(1,5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(2,20,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(3,100,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(4,400,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(5,2000,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(50,564908303,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(7,40000,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(10,3200000,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(100,564490093,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(21,999641607,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countGoodNumbers(long long n){return 0;}};
// USER_CODE_END
void test(long long n,long long e,int tc,bool hd=false){int r=CodeCoder().countGoodNumbers(n);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test(1,5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(2,20,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(3,100,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(4,400,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(5,2000,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(50,564908303,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(7,40000,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(10,3200000,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(100,564490093,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(21,999641607,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countGoodNumbers(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,hd=False):r=CodeCoder().countGoodNumbers(n);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={n}:exp={e}:got={r}"))
try:test(1,5,1)
except:print("TC:1:FAIL:hidden")
try:test(2,20,2)
except:print("TC:2:FAIL:hidden")
try:test(3,100,3)
except:print("TC:3:FAIL:hidden")
try:test(4,400,4)
except:print("TC:4:FAIL:hidden")
try:test(5,2000,5)
except:print("TC:5:FAIL:hidden")
try:test(50,564908303,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(7,40000,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(10,3200000,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(100,564490093,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(21,999641607,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countGoodNumbers(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,hd){if(hd===undefined)hd=false;const r=countGoodNumbers(n);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test(1,5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(2,20,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(3,100,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(4,400,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(5,2000,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(50,564908303,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(7,40000,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(10,3200000,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(100,564490093,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(21,999641607,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int countGoodNumbers(long long n) {
    // Write your code here — 5^even * 4^odd mod (1e9+7)
    return 0;
}
// USER_CODE_END

void runTest(long long n,long long e,int tc,int hd){
    int r=countGoodNumbers(n);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%d\\n",tc,e,r);}
}
int main(){
    runTest(1,5,1,0);
    runTest(2,20,2,0);
    runTest(3,100,3,0);
    runTest(4,400,4,0);
    runTest(5,2000,5,0);
    runTest(50,564908303,6,1);
    runTest(7,40000,7,1);
    runTest(10,3200000,8,1);
    runTest(100,564490093,9,1);
    runTest(21,999641607,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
