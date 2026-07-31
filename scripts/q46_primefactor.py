"""
Prime Factorisation of a Number
=================================
Given an integer n, find its largest prime factor.

Examples:
  n = 12 → prime factors 2,2,3 → largest = 3
  n = 15 → prime factors 3,5 → largest = 5
  n = 7 → 7 is prime → largest = 7

Approach: repeatedly divide by 2, then odd numbers from 3 to sqrt(n).
If remaining n > 1, it's the largest prime factor.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Prime Factorisation of a Number"
desc=(
    "Given an integer n, find the largest prime factor of n.\n\n"
    "A prime factor is a factor of n that is also a prime number. "
    "This problem asks for the largest among all prime factors.\n\n"
    "For example:\n"
    "n = 12 → prime factors are 2, 2, 3 → largest = 3\n"
    "n = 15 → prime factors are 3, 5 → largest = 5\n"
    "n = 7 → 7 is prime itself → largest = 7\n\n"
    "Approach: divide n by 2 as many times as possible. Then for odd i from 3 "
    "to sqrt(n), divide n by i while divisible. The remaining n (if > 1) is "
    "the largest prime factor."
)
infmt="Single line containing integer n."
outfmt="Print the largest prime factor of n."
cons="2 ≤ n ≤ 10^9"
e1="Input:\n12\n\nOutput:\n3"
e2="Input:\n15\n\nOutput:\n5"
e3="Input:\n7\n\nOutput:\n7"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Number Theory",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int largestPrimeFactor(int n) {
        // Write your code here — divide by 2, then odd factors up to sqrt(n)
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,int e,int tc,boolean h){int g=new CodeCoder().largestPrimeFactor(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(12,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(15,5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(7,7,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(100,5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(2,2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(64,2,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(13195,29,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(1024,2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(999999937,999999937,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(315,7,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int largestPrimeFactor(int n){return 0;}};
// USER_CODE_END
void test(int n,int e,int tc,bool h=false){int g=CodeCoder().largestPrimeFactor(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(12,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(15,5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(7,7,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(100,5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(2,2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(64,2,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(13195,29,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(1024,2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(999999937,999999937,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(315,7,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def largestPrimeFactor(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().largestPrimeFactor(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
try:test(12,3,1)
except:print("TC:1:FAIL:hidden")
try:test(15,5,2)
except:print("TC:2:FAIL:hidden")
try:test(7,7,3)
except:print("TC:3:FAIL:hidden")
try:test(100,5,4)
except:print("TC:4:FAIL:hidden")
try:test(2,2,5)
except:print("TC:5:FAIL:hidden")
try:test(64,2,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(13195,29,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(1024,2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(999999937,999999937,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(315,7,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function largestPrimeFactor(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=largestPrimeFactor(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
try{test(12,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(15,5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(7,7,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(100,5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(2,2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(64,2,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(13195,29,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(1024,2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(999999937,999999937,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(315,7,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int largestPrimeFactor(int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int n,int e,int tc,int h){
    int g=largestPrimeFactor(n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n",tc,n,e,g);}
}
int main(){
    runTest(12,3,1,0);
    runTest(15,5,2,0);
    runTest(7,7,3,0);
    runTest(100,5,4,0);
    runTest(2,2,5,0);
    runTest(64,2,6,1);
    runTest(13195,29,7,1);
    runTest(1024,2,8,1);
    runTest(999999937,999999937,9,1);
    runTest(315,7,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
