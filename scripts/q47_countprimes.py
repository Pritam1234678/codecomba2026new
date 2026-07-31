"""
Count Primes in Range L to R
==============================
Given a range [L, R], count the number of prime numbers in that range (inclusive).

Examples:
  L=1, R=10 → primes: 2,3,5,7 → count = 4
  L=5, R=7 → primes: 5,7 → count = 2
  L=1, R=1 → count = 0

Approach: Sieve of Eratosthenes up to R, then count primes in [L, R].

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Primes in range L to R"
desc=(
    "Given two integers L and R (L ≤ R), count how many prime numbers exist in "
    "the inclusive range [L, R].\n\n"
    "For example:\n"
    "L=1, R=10 → primes in [1,10] are 2,3,5,7 → count = 4\n"
    "L=5, R=7 → primes are 5,7 → count = 2\n"
    "L=1, R=1 → no primes → count = 0\n\n"
    "Approach: use the Sieve of Eratosthenes to mark all primes up to R, "
    "then count how many primes fall within [L, R]. A prime is a number "
    "greater than 1 divisible only by 1 and itself."
)
infmt="First line contains L.\nSecond line contains R."
outfmt="Print the count of primes in [L, R]."
cons="1 ≤ L ≤ R ≤ 10^6"
e1="Input:\n1\n10\n\nOutput:\n4"
e2="Input:\n5\n7\n\nOutput:\n2"
e3="Input:\n1\n1\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Math, Number Theory, Sieve",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countPrimesInRange(int L, int R) {
        // Write your code here — sieve up to R, count primes in [L,R]
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int L,int R,int e,int tc,boolean h){int g=new CodeCoder().countPrimesInRange(L,R);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:L="+L+" R="+R+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(1,10,4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(5,7,2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,1,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(2,2,1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(10,20,4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(1,100,25,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(90,100,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(2,50,15,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(100,200,21,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(999900,1000000,8,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countPrimesInRange(int L,int R){return 0;}};
// USER_CODE_END
void test(int L,int R,int e,int tc,bool h=false){int g=CodeCoder().countPrimesInRange(L,R);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:L="<<L<<" R="<<R<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(1,10,4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(5,7,2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,1,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(2,2,1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(10,20,4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(1,100,25,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(90,100,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(2,50,15,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(100,200,21,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(999900,1000000,8,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countPrimesInRange(self, L, R):
        return 0
# USER_CODE_END
def test(L,R,e,tc,h=False):g=CodeCoder().countPrimesInRange(L,R);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:L={L}:R={R}:exp={e}:got={g}"))
try:test(1,10,4,1)
except:print("TC:1:FAIL:hidden")
try:test(5,7,2,2)
except:print("TC:2:FAIL:hidden")
try:test(1,1,0,3)
except:print("TC:3:FAIL:hidden")
try:test(2,2,1,4)
except:print("TC:4:FAIL:hidden")
try:test(10,20,4,5)
except:print("TC:5:FAIL:hidden")
try:test(1,100,25,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(90,100,1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(2,50,15,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(100,200,21,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(999900,1000000,8,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countPrimesInRange(L, R) { return 0; }
// USER_CODE_END
function test(L,R,e,tc,h){if(h===undefined)h=false;const g=countPrimesInRange(L,R);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:L="+L+":R="+R+":exp="+e+":got="+g);}
try{test(1,10,4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(5,7,2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,1,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(2,2,1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(10,20,4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(1,100,25,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(90,100,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(2,50,15,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(100,200,21,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(999900,1000000,8,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int countPrimesInRange(int L,int R) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int L,int R,int e,int tc,int h){
    int g=countPrimesInRange(L,R);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:L=%d R=%d:exp=%d:got=%d\\n",tc,L,R,e,g);}
}
int main(){
    runTest(1,10,4,1,0);
    runTest(5,7,2,2,0);
    runTest(1,1,0,3,0);
    runTest(2,2,1,4,0);
    runTest(10,20,4,5,0);
    runTest(1,100,25,6,1);
    runTest(90,100,1,7,1);
    runTest(2,50,15,8,1);
    runTest(100,200,21,9,1);
    runTest(999900,1000000,8,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
