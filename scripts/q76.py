"""
Climbing Stairs
================
You are climbing a staircase. It takes n steps to reach the top.
Each time you can either climb 1 or 2 steps. In how many distinct ways
can you climb to the top?

Examples:
  n = 2 → 2 ways: 1+1, 2
  n = 3 → 3 ways: 1+1+1, 1+2, 2+1

This is directly the Fibonacci sequence:
  ways(n) = ways(n-1) + ways(n-2)
  Base: ways(1) = 1, ways(2) = 2

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn = psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Climbing Stairs"
desc=(
    "You are climbing a staircase. It takes n steps to reach the top.\n\n"
    "Each time you can either climb 1 or 2 steps. In how many distinct ways "
    "can you climb to the top?\n\n"
    "For n = 2, you can climb: 1+1 (two single steps) or 2 (one double step) = 2 ways.\n"
    "For n = 3, you can climb: 1+1+1, 1+2, 2+1 = 3 ways.\n\n"
    "The number of ways for n steps is: ways(n) = ways(n-1) + ways(n-2)\n"
    "This is exactly the Fibonacci sequence starting at n=1: 1, 2, 3, 5, 8, 13, ..."
)
infmt="Single line containing integer n."
outfmt="Print the number of distinct ways to climb to the top."
cons="1 \u2264 n \u2264 45"
e1="Input:\n2\n\nOutput:\n2\n\nExplanation: 1+1 and 2."
e2="Input:\n3\n\nOutput:\n3\n\nExplanation: 1+1+1, 1+2, 2+1."
e3="Input:\n5\n\nOutput:\n8\n\nExplanation: Fibonacci at n=5 is 8."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Dynamic Programming, Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int climbStairs(int n) {
        // Write your code here — use Fibonacci-style DP
        return 0;
    }
}
// USER_CODE_END
public class Main {
static void test(int n,int e,int tc,boolean h){int g=new CodeCoder().climbStairs(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(2,2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(3,3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(4,5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(10,89,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(45,1836311903,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(20,10946,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(6,13,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(7,21,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(15,987,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int climbStairs(int n){return 0;}};
// USER_CODE_END
void test(int n,int e,int tc,bool h=false){int g=CodeCoder().climbStairs(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(2,2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(3,3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(4,5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(10,89,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(45,1836311903,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(20,10946,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(6,13,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(7,21,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(15,987,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def climbStairs(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().climbStairs(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test(2,2,1)
except:print("TC:1:FAIL:hidden")
try:test(3,3,2)
except:print("TC:2:FAIL:hidden")
try:test(1,1,3)
except:print("TC:3:FAIL:hidden")
try:test(4,5,4)
except:print("TC:4:FAIL:hidden")
try:test(10,89,5)
except:print("TC:5:FAIL:hidden")
try:test(45,1836311903,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(20,10946,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(6,13,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(7,21,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(15,987,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code=r'''// USER_CODE_START
function climbStairs(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=climbStairs(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":expected="+e+":got="+g);}
try{test(2,2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(3,3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(4,5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(10,89,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(45,1836311903,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(20,10946,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(6,13,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(7,21,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(15,987,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code=r'''#include <stdio.h>
// USER_CODE_START
int climbStairs(int n) { return 0; }
// USER_CODE_END
void runTest(int n,int e,int tc,int h){int g=climbStairs(n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:expected=%d:got=%d\\n",tc,n,e,g);}}
int main(){
runTest(2,2,1,0);runTest(3,3,2,0);runTest(1,1,3,0);runTest(4,5,4,0);runTest(10,89,5,0);
runTest(45,1836311903,6,1);runTest(20,10946,7,1);runTest(6,13,8,1);runTest(7,21,9,1);runTest(15,987,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall():
    print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
