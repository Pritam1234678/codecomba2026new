"""
Palindrome Number
===================
Given an integer x, return true if x is a palindrome integer.
A palindrome reads the same forwards and backwards.

Examples:
  121 → true  (121 reversed is 121)
  -121 → false (reversed is 121-, not same)
  10 → false (reversed is 01, not same)

Approach: Reverse half of the number and compare with the other half.
Negative numbers are never palindromes.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Palindrome Number"
desc=(
    "Given an integer x, return true if x is a palindrome integer.\n\n"
    "A palindrome integer reads the same forwards and backwards.\n\n"
    "For example:\n"
    "- 121 → true (121 reversed is 121)\n"
    "- -121 → false (reversed is 121-, minus at end)\n"
    "- 10 → false (reversed is 01, not same)\n\n"
    "Negative numbers are never palindromes. A number ending with 0 cannot "
    "be a palindrome unless it is 0 itself (since no integer starts with 0).\n\n"
    "To solve this without converting to string, reverse the last half of digits "
    "and compare with the first half. Only reverse half to avoid overflow issues."
)
infmt="Single line containing integer x."
outfmt="Print 'true' if palindrome, otherwise 'false'."
cons="-2^31 ≤ x ≤ 2^31-1"
e1="Input:\n121\n\nOutput:\ntrue"
e2="Input:\n-121\n\nOutput:\nfalse"
e3="Input:\n10\n\nOutput:\nfalse"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean isPalindrome(int x) {
        // Write your code here — reverse half and compare
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int x,boolean e,int tc,boolean h){boolean g=new CodeCoder().isPalindrome(x);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:x="+x+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(121,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(-121,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(10,false,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(0,true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(12321,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(-1,false,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(1234567899,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(2147447412,true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool isPalindrome(int x){return false;}};
// USER_CODE_END
void test(int x,bool e,int tc,bool h=false){bool g=CodeCoder().isPalindrome(x);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:x="<<x<<":exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test(121,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(-121,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(10,false,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(0,true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(12321,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(-1,false,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(1234567899,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(2147447412,true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def isPalindrome(self, x):
        return False
# USER_CODE_END
def test(x,e,tc,h=False):g=CodeCoder().isPalindrome(x);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:x={x}:exp={e}:got={g}"))
try:test(121,True,1)
except:print("TC:1:FAIL:hidden")
try:test(-121,False,2)
except:print("TC:2:FAIL:hidden")
try:test(10,False,3)
except:print("TC:3:FAIL:hidden")
try:test(0,True,4)
except:print("TC:4:FAIL:hidden")
try:test(12321,True,5)
except:print("TC:5:FAIL:hidden")
try:test(100,False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(1,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(-1,False,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(1234567899,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(2147447412,True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function isPalindrome(x) { return false; }
// USER_CODE_END
function test(x,e,tc,h){if(h===undefined)h=false;const g=isPalindrome(x);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:x="+x+":exp="+e+":got="+g);}
try{test(121,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(-121,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(10,false,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(0,true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(12321,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(-1,false,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(1234567899,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(2147447412,true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool isPalindrome(int x){return false;}
// USER_CODE_END
void run(int x,bool e,int tc,int h){bool g=isPalindrome(x);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:x=%d:exp=%s:got=%s\\n",tc,x,e?"true":"false",g?"true":"false");}}
int main(){
run(121,true,1,0);run(-121,false,2,0);run(10,false,3,0);run(0,true,4,0);run(12321,true,5,0);
run(100,false,6,1);run(1,true,7,1);run(-1,false,8,1);run(1234567899,false,9,1);run(2147447412,true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
