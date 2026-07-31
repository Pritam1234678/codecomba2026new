"""
Check if a Number is Armstrong
=================================
Given an integer n, check if it is an Armstrong number. An Armstrong number is
a number equal to the sum of its own digits each raised to the power of the
number of digits.

Examples:
  153 → 1^3 + 5^3 + 3^3 = 1 + 125 + 27 = 153 → true
  123 → 1^3 + 2^3 + 3^3 = 36 ≠ 123 → false
  9474 → 9^4 + 4^4 + 7^4 + 4^4 = 9474 → true

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Check if a Number is Armstrong"
desc=(
    "Given an integer n, determine whether it is an Armstrong number.\n\n"
    "An Armstrong number (also called narcissistic number) is a number that is "
    "equal to the sum of its own digits each raised to the power of the number of digits.\n\n"
    "For example:\n"
    "n = 153 → digits: 1, 5, 3 (3 digits). 1^3 + 5^3 + 3^3 = 1 + 125 + 27 = 153 → true\n"
    "n = 123 → 1^3 + 2^3 + 3^3 = 36 ≠ 123 → false\n"
    "n = 9474 → 4 digits: 9^4 + 4^4 + 7^4 + 4^4 = 6561 + 256 + 2401 + 256 = 9474 → true\n\n"
    "Algorithm: count the digits of n (call it d). Then sum each digit raised "
    "to the power d. Compare with original n."
)
infmt="Single line containing integer n."
outfmt="Print 'true' if n is an Armstrong number, otherwise 'false'."
cons="1 ≤ n ≤ 10^9"
e1="Input:\n153\n\nOutput:\ntrue"
e2="Input:\n123\n\nOutput:\nfalse"
e3="Input:\n9474\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Number Theory",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean isArmstrong(int n) {
        // Write your code here — sum of digits raised to count of digits
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int n,boolean e,int tc,boolean h){boolean g=new CodeCoder().isArmstrong(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(153,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(123,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(9474,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(370,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1634,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(8208,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(999,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(1000000000,false,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool isArmstrong(int n){return false;}};
// USER_CODE_END
void test(int n,bool e,int tc,bool h=false){bool g=CodeCoder().isArmstrong(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test(153,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(123,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(9474,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(370,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1634,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(8208,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(999,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(1000000000,false,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def isArmstrong(self, n):
        return False
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().isArmstrong(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
try:test(153,True,1)
except:print("TC:1:FAIL:hidden")
try:test(123,False,2)
except:print("TC:2:FAIL:hidden")
try:test(9474,True,3)
except:print("TC:3:FAIL:hidden")
try:test(1,True,4)
except:print("TC:4:FAIL:hidden")
try:test(370,True,5)
except:print("TC:5:FAIL:hidden")
try:test(100,False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(1634,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(8208,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(999,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(1000000000,False,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function isArmstrong(n) { return false; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=isArmstrong(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
try{test(153,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(123,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(9474,true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(370,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1634,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(8208,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(999,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(1000000000,false,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
#include <math.h>

// USER_CODE_START
bool isArmstrong(int n) {
    // Write your code here
    return false;
}
// USER_CODE_END

void runTest(int n,bool e,int tc,int h){
    bool g=isArmstrong(n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:exp=%s:got=%s\\n",tc,n,e?"true":"false",g?"true":"false");}
}
int main(){
    runTest(153,true,1,0);
    runTest(123,false,2,0);
    runTest(9474,true,3,0);
    runTest(1,true,4,0);
    runTest(370,true,5,0);
    runTest(100,false,6,1);
    runTest(1634,true,7,1);
    runTest(8208,true,8,1);
    runTest(999,false,9,1);
    runTest(1000000000,false,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
