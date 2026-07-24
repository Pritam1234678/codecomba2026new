"""
Count Primes
==============
Given an integer n, return the number of prime numbers that are strictly less than n.

Examples:
  n = 10 → primes less than 10 are [2,3,5,7] → output 4
  n = 0 → 0
  n = 1 → 0

Approach: Sieve of Eratosthenes.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Primes"
desc=(
    "Given an integer n, return the number of prime numbers that are strictly less than n.\n\n"
    "For example:\n"
    "n = 10 → primes less than 10 are [2, 3, 5, 7] → output 4.\n"
    "n = 0 or n = 1 → output 0 (no primes less than them).\n\n"
    "Use the Sieve of Eratosthenes: create a boolean array of size n, mark 0 and 1 as false, "
    "then for each i from 2 to sqrt(n), mark all multiples of i as composite. "
    "Count the remaining true values."
)
infmt="Single line containing integer n."
outfmt="Print the count of primes less than n."
cons="0 ≤ n ≤ 5*10^6"
e1="Input:\n10\n\nOutput:\n4"
e2="Input:\n0\n\nOutput:\n0"
e3="Input:\n2\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Math, Number Theory, Sieve",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countPrimes(int n) {
        // Write your code here — Sieve of Eratosthenes
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,int e,int tc,boolean h){int g=new CodeCoder().countPrimes(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(10,4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(0,0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(2,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(3,1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(20,8,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(100,25,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1,0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(30,10,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(50,15,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(5000,669,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countPrimes(int n){return 0;}};
// USER_CODE_END
void test(int n,int e,int tc,bool h=false){int g=CodeCoder().countPrimes(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(10,4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(0,0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(2,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(3,1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(20,8,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,25,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1,0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(30,10,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(50,15,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(5000,669,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countPrimes(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().countPrimes(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
try:test(10,4,1)
except:print("TC:1:FAIL:hidden")
try:test(0,0,2)
except:print("TC:2:FAIL:hidden")
try:test(2,0,3)
except:print("TC:3:FAIL:hidden")
try:test(3,1,4)
except:print("TC:4:FAIL:hidden")
try:test(20,8,5)
except:print("TC:5:FAIL:hidden")
try:test(100,25,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(1,0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(30,10,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(50,15,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(5000,669,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countPrimes(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=countPrimes(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
try{test(10,4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(0,0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(2,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(3,1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(20,8,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,25,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1,0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(30,10,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(50,15,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(5000,669,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
int countPrimes(int n){return 0;}
// USER_CODE_END
void run(int n,int e,int tc,int h){int g=countPrimes(n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n",tc,n,e,g);}}
int main(){
run(10,4,1,0);run(0,0,2,0);run(2,0,3,0);run(3,1,4,0);run(20,8,5,0);
run(100,25,6,1);run(1,0,7,1);run(30,10,8,1);run(50,15,9,1);run(5000,669,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
