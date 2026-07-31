"""
String to Integer (atoi)
===========================
Implement the myAtoi function that converts a string to a 32-bit signed integer.

Rules:
1. Skip leading whitespace.
2. Read optional +/- sign.
3. Read digits until non-digit.
4. Clamp to [-2^31, 2^31-1].

Examples:
  "42" → 42
  "   -42" → -42
  "4193 with words" → 4193
  "words and 987" → 0

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="String to Integer (atoi)"
desc=(
    "Implement the myAtoi(string s) function, which converts a string to a "
    "32-bit signed integer.\n\n"
    "The algorithm is as follows:\n"
    "1. Read in and ignore any leading whitespace.\n"
    "2. Check if the next character is '-' or '+', and read the sign.\n"
    "3. Read in digits until a non-digit character is reached.\n"
    "4. If no digits were read, the result is 0.\n"
    "5. Clamp the result to the range [-2^31, 2^31 - 1].\n\n"
    "For example:\n"
    "\"42\" → 42\n"
    "\"   -42\" → -42\n"
    "\"4193 with words\" → 4193\n"
    "\"words and 987\" → 0 (no digits found)"
)
infmt="Single line containing string s."
outfmt="Print the parsed integer."
cons="0 ≤ |s| ≤ 200\ns consists of English letters, digits, '+', '-', and spaces."
e1="Input:\n42\n\nOutput:\n42"
e2="Input:\n   -42\n\nOutput:\n-42"
e3="Input:\n4193 with words\n\nOutput:\n4193"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"String, Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int myAtoi(String s) {
        // Write your code here — parse int with clamping
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,int e,int tc,boolean h){int g=new CodeCoder().myAtoi(s);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("42",42,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("   -42",-42,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("4193 with words",4193,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("words and 987",0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("-91283472332",-2147483648,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("2147483648",2147483647,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("+42",42,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("  +  413",0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("0",0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("  -42abc",-42,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int myAtoi(string s){return 0;}};
// USER_CODE_END
void test(string s,int e,int tc,bool h=false){int g=CodeCoder().myAtoi(s);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("42",42,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("   -42",-42,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("4193 with words",4193,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("words and 987",0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("-91283472332",-2147483648,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("2147483648",2147483647,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("+42",42,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("  +  413",0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("0",0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("  -42abc",-42,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def myAtoi(self, s):
        return 0
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().myAtoi(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={repr(s)}:exp={e}:got={g}"))
try:test("42",42,1)
except:print("TC:1:FAIL:hidden")
try:test("   -42",-42,2)
except:print("TC:2:FAIL:hidden")
try:test("4193 with words",4193,3)
except:print("TC:3:FAIL:hidden")
try:test("words and 987",0,4)
except:print("TC:4:FAIL:hidden")
try:test("-91283472332",-2147483648,5)
except:print("TC:5:FAIL:hidden")
try:test("2147483648",2147483647,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("+42",42,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("  +  413",0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("0",0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("  -42abc",-42,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function myAtoi(s) { return 0; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=myAtoi(s);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("42",42,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("   -42",-42,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("4193 with words",4193,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("words and 987",0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("-91283472332",-2147483648,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("2147483648",2147483647,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("+42",42,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("  +  413",0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("0",0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("  -42abc",-42,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <limits.h>

// USER_CODE_START
int myAtoi(char* s) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(char* s,int e,int tc,int h){
    int g=myAtoi(s);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%d:got=%d\\n",tc,s,e,g);}
}
int main(){
    runTest("42",42,1,0);
    runTest("   -42",-42,2,0);
    runTest("4193 with words",4193,3,0);
    runTest("words and 987",0,4,0);
    runTest("-91283472332",INT_MIN,5,0);
    runTest("2147483648",INT_MAX,6,1);
    runTest("+42",42,7,1);
    runTest("  +  413",0,8,1);
    runTest("0",0,9,1);
    runTest("  -42abc",-42,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
