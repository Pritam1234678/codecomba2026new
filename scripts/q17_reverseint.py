"""
Reverse Integer
=================
Given a signed 32-bit integer x, return x with its digits reversed.
If reversing x causes the value to go outside the signed 32-bit integer
range [-2^31, 2^31 - 1], return 0.

Examples:
  x = 123 → 321
  x = -123 → -321
  x = 120 → 21
  x = 1534236469 → 0 (overflows)

Pop last digit, build result, check overflow before multiplying by 10.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reverse Integer"
desc=(
    "Given a signed 32-bit integer x, return x with its digits reversed.\n\n"
    "If reversing x causes the value to go outside the signed 32-bit integer "
    "range [-2^31, 2^31 - 1], return 0.\n\n"
    "For example:\n"
    "x = 123 → reverse = 321\n"
    "x = -123 → reverse = -321\n"
    "x = 120 → reverse = 21 (leading zero dropped)\n"
    "x = 1534236469 → reverse = 0 (overflow)\n\n"
    "Repeatedly pop the last digit using x % 10, build result. Check for overflow "
    "before each multiplication: if result > INT_MAX/10 or (result == INT_MAX/10 && digit > 7) → overflow."
)
infmt="Single line containing integer x."
outfmt="Print the reversed integer, or 0 if overflow."
cons="-2^31 ≤ x ≤ 2^31-1"
e1="Input:\n123\n\nOutput:\n321"
e2="Input:\n-123\n\nOutput:\n-321"
e3="Input:\n120\n\nOutput:\n21"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder {
    public int reverse(int x) { return 0; }
}
// USER_CODE_END
public class Main {
static void test(int x,int e,int tc,boolean h){int g=new CodeCoder().reverse(x);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:x="+x+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(123,321,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(-123,-321,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(120,21,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(0,0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(1534236469,0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(-2147483648,0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1463847412,2147483641,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(-1463847412,-2147483641,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(10,1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(-1,-1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int reverse(int x){return 0;}};
// USER_CODE_END
void test(int x,int e,int tc,bool h=false){int g=CodeCoder().reverse(x);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:x="<<x<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(123,321,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(-123,-321,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(120,21,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(0,0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(1534236469,0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(-2147483648,0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1463847412,2147483641,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(-1463847412,-2147483641,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(10,1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(-1,-1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def reverse(self, x): return 0
# USER_CODE_END
def test(x,e,tc,h=False):g=CodeCoder().reverse(x);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:x={x}:exp={e}:got={g}"))
try:test(123,321,1)
except:print("TC:1:FAIL:hidden")
try:test(-123,-321,2)
except:print("TC:2:FAIL:hidden")
try:test(120,21,3)
except:print("TC:3:FAIL:hidden")
try:test(0,0,4)
except:print("TC:4:FAIL:hidden")
try:test(1534236469,0,5)
except:print("TC:5:FAIL:hidden")
try:test(-2147483648,0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(1463847412,2147483641,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(-1463847412,-2147483641,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(10,1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(-1,-1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function reverse(x) { return 0; }
// USER_CODE_END
function test(x,e,tc,h){if(h===undefined)h=false;const g=reverse(x);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:x="+x+":exp="+e+":got="+g);}
try{test(123,321,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(-123,-321,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(120,21,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(0,0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(1534236469,0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(-2147483648,0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1463847412,2147483641,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(-1463847412,-2147483641,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(10,1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(-1,-1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <limits.h>
// USER_CODE_START
int reverse(int x){return 0;}
// USER_CODE_END
void run(int x,int e,int tc,int h){int g=reverse(x);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:x=%d:exp=%d:got=%d\\n",tc,x,e,g);}}
int main(){
run(123,321,1,0);run(-123,-321,2,0);run(120,21,3,0);run(0,0,4,0);run(1534236469,0,5,0);
run(-2147483648,0,6,1);run(1463847412,2147483641,7,1);run(-1463847412,-2147483641,8,1);run(10,1,9,1);run(-1,-1,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
