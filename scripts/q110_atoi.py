"""
Recursive Implementation of atoi()
====================================
Given a string s representing a valid integer (no leading/trailing spaces,
no non-digit characters except an optional leading '+' or '-'), convert it to
an integer and return it. If the string is invalid, return -1. The conversion
must be done using recursion.

Examples:
  s = "123"  -> 123
  s = "-123" -> -123
  s = "12a"  -> -1  (invalid)

Recursive conversion: parse from the left, result = result * 10 + digit.
Handle the sign, then recurse over the remaining digits.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the function takes the char* string.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Recursive Implementation of atoi()"
desc=(
    "Given a string s that represents an integer, convert it to its integer "
    "value using RECURSION and return it. The string may start with an "
    "optional '+' or '-' sign followed by digits only. There are no spaces. "
    "If the string contains any character that is not a digit or a leading "
    "sign, it is invalid and you must return -1.\n\n"
    "For example:\n"
    "s = \"123\"  -> 123\n"
    "s = \"-123\" -> -123\n"
    "s = \"12a\"  -> -1  (invalid)\n\n"
    "Recursive idea: convert the first character to its digit value, then "
    "recurse on the rest of the string with result = result * 10 + digit. "
    "Apply the sign at the end."
)
infmt="A single string s (length up to 10)."
outfmt="Print the parsed integer, or -1 if the string is invalid."
cons="1 ≤ |s| ≤ 10\nValid strings look like [sign]digits, e.g. 123, -456, +78."
e1="Input:\n123\n\nOutput:\n123"
e2="Input:\n-123\n\nOutput:\n-123"
e3="Input:\n12a\n\nOutput:\n-1"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"String, Recursion",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int myAtoi(String s) {
        // Write your code here — recursive conversion, -1 if invalid
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,int e,int tc,boolean hd){int r=new CodeCoder().myAtoi(s);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s=\\""+s+"\\":exp="+e+":got="+r);}
public static void main(String[] a){
try{test("123",123,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("-123",-123,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("12a",-1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("0",0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("-7",-7,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("+45",45,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("987654321",987654321,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("-2147483648",-2147483648,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("12 34",-1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("abc",-1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int myAtoi(string s){return 0;}};
// USER_CODE_END
 void test(string s,int e,int tc,bool hd=false){int r=CodeCoder().myAtoi(s);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s=\""<<s<<"\":exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test("123",123,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("-123",-123,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("12a",-1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("0",0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("-7",-7,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("+45",45,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("987654321",987654321,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("-2147483648",-2147483648,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("12 34",-1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("abc",-1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def myAtoi(self, s):
        return 0
# USER_CODE_END
def test(s,e,tc,hd=False):r=CodeCoder().myAtoi(s);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:s={s!r}:exp={e}:got={r}"))
try:test("123",123,1)
except:print("TC:1:FAIL:hidden")
try:test("-123",-123,2)
except:print("TC:2:FAIL:hidden")
try:test("12a",-1,3)
except:print("TC:3:FAIL:hidden")
try:test("0",0,4)
except:print("TC:4:FAIL:hidden")
try:test("-7",-7,5)
except:print("TC:5:FAIL:hidden")
try:test("+45",45,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("987654321",987654321,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("-2147483648",-2147483648,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("12 34",-1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("abc",-1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function myAtoi(s) { return 0; }
// USER_CODE_END
function test(s,e,tc,hd){if(hd===undefined)hd=false;const r=myAtoi(s);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:s="+JSON.stringify(s)+":exp="+e+":got="+r);}
try{test("123",123,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("-123",-123,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("12a",-1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("0",0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("-7",-7,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("+45",45,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("987654321",987654321,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("-2147483648",-2147483648,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("12 34",-1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("abc",-1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <limits.h>

// USER_CODE_START
int myAtoi(const char* s) {
    // Write your code here — recursive conversion, -1 if invalid
    return 0;
}
// USER_CODE_END

void runTest(const char* s,int e,int tc,int hd){
    int r=myAtoi(s);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%d:got=%d\\n",tc,s,e,r);}
}
int main(){
    runTest("123",123,1,0);
    runTest("-123",-123,2,0);
    runTest("12a",-1,3,0);
    runTest("0",0,4,0);
    runTest("-7",-7,5,0);
    runTest("+45",45,6,1);
    runTest("987654321",987654321,7,1);
    runTest("-2147483648",-2147483648,8,1);
    runTest("12 34",-1,9,1);
    runTest("abc",-1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
