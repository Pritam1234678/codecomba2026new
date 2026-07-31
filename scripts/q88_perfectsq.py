"""
Valid Perfect Square
=====================
Given a positive integer num, return true if num is a perfect square,
otherwise return false. Do not use any built-in square-root library.

Examples:
  num = 16 -> true   (4 * 4)
  num = 14 -> false

Binary search on [1, num]: if mid * mid == num return true; if mid * mid < num
search the right half, else the left half. Guard against overflow by comparing
mid with num / mid instead of computing mid * mid.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Valid Perfect Square"
desc=(
    "Given a positive integer num, return true if num is a perfect square, "
    "otherwise return false. You must NOT use any built-in square-root "
    "function/library.\n\n"
    "For example:\n"
    "num = 16 -> true  (4 * 4)\n"
    "num = 14 -> false\n\n"
    "Binary search the answer in the range [1, num]: if mid * mid == num, num "
    "is a perfect square; if mid * mid < num, look right; else look left. To "
    "avoid integer overflow, compare mid with num / mid (or use a 64-bit type) "
    "rather than forming mid * mid directly. Runs in O(log num)."
)
infmt="A single integer num (1 ≤ num ≤ 2^31 - 1)."
outfmt="Print 'true' if num is a perfect square, else 'false'."
cons="1 ≤ num ≤ 2^31 - 1\nDo not use built-in square-root functions."
e1="Input:\n16\n\nOutput:\ntrue"
e2="Input:\n14\n\nOutput:\nfalse"
e3="Input:\n1\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean isPerfectSquare(int num) {
        // Write your code here — binary search, no Math.sqrt
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int t,boolean e,int tc,boolean h){boolean r=new CodeCoder().isPerfectSquare(t);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:num="+t+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(16,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(14,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(25,true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(9,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(2,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1000000,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(2147395600,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(2147395599,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(2147483647,false,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool isPerfectSquare(int num){return false;}};
// USER_CODE_END
void test(int t,bool e,int tc,bool h=false){bool r=CodeCoder().isPerfectSquare(t);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(r?"true":"false")<<"\\n";}
int main(){
try{test(16,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(14,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(25,true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(9,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(2,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1000000,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(2147395600,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(2147395599,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(2147483647,false,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def isPerfectSquare(self, num):
        return False
# USER_CODE_END
def test(t,e,tc,h=False):r=CodeCoder().isPerfectSquare(t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:num={t}:exp={e}:got={r}"))
try:test(16,True,1)
except:print("TC:1:FAIL:hidden")
try:test(14,False,2)
except:print("TC:2:FAIL:hidden")
try:test(1,True,3)
except:print("TC:3:FAIL:hidden")
try:test(25,True,4)
except:print("TC:4:FAIL:hidden")
try:test(9,True,5)
except:print("TC:5:FAIL:hidden")
try:test(2,False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(1000000,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(2147395600,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(2147395599,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(2147483647,False,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function isPerfectSquare(num) { return false; }
// USER_CODE_END
function test(t,e,tc,h){if(h===undefined)h=false;const r=isPerfectSquare(t);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test(16,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(14,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(25,true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(9,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(2,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1000000,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(2147395600,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(2147395599,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(2147483647,false,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
bool isPerfectSquare(int num) {
    // Write your code here — binary search, no sqrt()
    return false;
}
// USER_CODE_END

void runTest(int t,bool e,int tc,int h){
    bool r=isPerfectSquare(t);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",r?"true":"false");}
}
int main(){
    runTest(16,true,1,0);
    runTest(14,false,2,0);
    runTest(1,true,3,0);
    runTest(25,true,4,0);
    runTest(9,true,5,0);
    runTest(2,false,6,1);
    runTest(1000000,true,7,1);
    runTest(2147395600,true,8,1);
    runTest(2147395599,false,9,1);
    runTest(2147483647,false,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
