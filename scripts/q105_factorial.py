"""
Factorial of a given number
============================
Given a positive integer n, return the factorial of n, defined as
n! = n * (n-1) * ... * 2 * 1, with 0! = 1.

Examples:
  n = 5 -> 120
  n = 3 -> 6

Use recursion: fact(n) = n * fact(n-1).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the result is returned as long long.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Factorial of a given number"
desc=(
    "Given a positive integer n, return the factorial of n: "
    "n! = n * (n-1) * ... * 2 * 1, and by convention 0! = 1.\n\n"
    "For example:\n"
    "n = 5 -> 120  (5*4*3*2*1)\n"
    "n = 3 -> 6    (3*2*1)\n\n"
    "Recursive definition: fact(n) = n * fact(n-1) with fact(0) = 1. Use a "
    "64-bit integer since factorials grow fast."
)
infmt="A single integer n."
outfmt="Print n! (as an integer)."
cons="0 ≤ n ≤ 20\nn! fits in a 64-bit integer."
e1="Input:\n5\n\nOutput:\n120"
e2="Input:\n3\n\nOutput:\n6"
e3="Input:\n0\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long factorial(int n) {
        // Write your code here — return n!
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,long e,int tc,boolean hd){long r=new CodeCoder().factorial(n);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(5,120,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(3,6,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(0,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(10,3628800,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(20,2432902008176640000L,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(7,5040,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(12,479001600,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(15,1307674368000L,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(6,720,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:long long factorial(int n){return 0;}};
// USER_CODE_END
void test(int n,long long e,int tc,bool hd=false){long long r=CodeCoder().factorial(n);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test(5,120,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(3,6,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(0,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(10,3628800,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(20,2432902008176640000LL,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(7,5040,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(12,479001600,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(15,1307674368000LL,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(6,720,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def factorial(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,hd=False):r=CodeCoder().factorial(n);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={n}:exp={e}:got={r}"))
try:test(5,120,1)
except:print("TC:1:FAIL:hidden")
try:test(3,6,2)
except:print("TC:2:FAIL:hidden")
try:test(0,1,3)
except:print("TC:3:FAIL:hidden")
try:test(1,1,4)
except:print("TC:4:FAIL:hidden")
try:test(10,3628800,5)
except:print("TC:5:FAIL:hidden")
try:test(20,2432902008176640000,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(7,5040,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(12,479001600,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(15,1307674368000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(6,720,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function factorial(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,hd){if(hd===undefined)hd=false;const r=factorial(n);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test(5,120,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(3,6,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(0,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(10,3628800,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(20,2432902008176640000,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(7,5040,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(12,479001600,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(15,1307674368000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(6,720,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
long long factorial(int n) {
    // Write your code here — return n!
    return 0;
}
// USER_CODE_END

void runTest(int n,long long e,int tc,int hd){
    long long r=factorial(n);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n",tc,e,r);}
}
int main(){
    runTest(5,120,1,0);
    runTest(3,6,2,0);
    runTest(0,1,3,0);
    runTest(1,1,4,0);
    runTest(10,3628800,5,0);
    runTest(20,2432902008176640000LL,6,1);
    runTest(7,5040,7,1);
    runTest(12,479001600,8,1);
    runTest(15,1307674368000LL,9,1);
    runTest(6,720,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
