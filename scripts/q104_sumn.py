"""
Sum of first N numbers
=======================
Given a positive integer n, return the sum of the first n natural numbers:
1 + 2 + 3 + ... + n.

Examples:
  n = 5 -> 15
  n = 10 -> 55

Use recursion (or the closed formula n*(n+1)/2): sum(n) = n + sum(n-1).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sum of first N numbers"
desc=(
    "Given a positive integer n, return the sum of the first n natural "
    "numbers: 1 + 2 + 3 + ... + n.\n\n"
    "For example:\n"
    "n = 5  -> 15  (1+2+3+4+5)\n"
    "n = 10 -> 55\n\n"
    "Recursive definition: sum(n) = n + sum(n-1), with sum(1) = 1. You may "
    "also use the closed form n*(n+1)/2 — but avoid writing an explicit loop."
)
infmt="A single integer n."
outfmt="Print the sum 1 + 2 + ... + n."
cons="1 ≤ n ≤ 10^6\nUse a 64-bit type for the sum (n*(n+1)/2 can exceed 32-bit)."
e1="Input:\n5\n\nOutput:\n15"
e2="Input:\n10\n\nOutput:\n55"
e3="Input:\n1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long sumOfSeries(int n) {
        // Write your code here — return 1+2+...+n
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,long e,int tc,boolean hd){long r=new CodeCoder().sumOfSeries(n);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(5,15,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(10,55,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(3,6,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(100,5050,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(1000,500500,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(2,3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(50,1275,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(10000,50005000,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(7,28,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:long long sumOfSeries(int n){return 0;}};
// USER_CODE_END
void test(int n,long long e,int tc,bool hd=false){long long r=CodeCoder().sumOfSeries(n);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test(5,15,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(10,55,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(3,6,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(100,5050,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(1000,500500,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(2,3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(50,1275,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(10000,50005000,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(7,28,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sumOfSeries(self, n):
        return 0
# USER_CODE_END
def test(n,e,tc,hd=False):r=CodeCoder().sumOfSeries(n);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={n}:exp={e}:got={r}"))
try:test(5,15,1)
except:print("TC:1:FAIL:hidden")
try:test(10,55,2)
except:print("TC:2:FAIL:hidden")
try:test(1,1,3)
except:print("TC:3:FAIL:hidden")
try:test(3,6,4)
except:print("TC:4:FAIL:hidden")
try:test(100,5050,5)
except:print("TC:5:FAIL:hidden")
try:test(1000,500500,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(2,3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(50,1275,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(10000,50005000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(7,28,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sumOfSeries(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,hd){if(hd===undefined)hd=false;const r=sumOfSeries(n);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test(5,15,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(10,55,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(3,6,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(100,5050,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(1000,500500,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(2,3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(50,1275,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(10000,50005000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(7,28,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
long long sumOfSeries(int n) {
    // Write your code here — return 1+2+...+n
    return 0;
}
// USER_CODE_END

void runTest(int n,long long e,int tc,int hd){
    long long r=sumOfSeries(n);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n",tc,e,r);}
}
int main(){
    runTest(5,15,1,0);
    runTest(10,55,2,0);
    runTest(1,1,3,0);
    runTest(3,6,4,0);
    runTest(100,5050,5,0);
    runTest(1000,500500,6,1);
    runTest(2,3,7,1);
    runTest(50,1275,8,1);
    runTest(10000,50005000,9,1);
    runTest(7,28,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
