"""
Maximum Nesting Depth of the Parentheses
===========================================
Given a valid parentheses string s, return the maximum nesting depth of s.
The depth is the maximum number of nested open parentheses.

Examples:
  s = "(1+(2*3)+((8)/4))+1" → 3
  s = "(1)+((2))+(((3)))" → 3
  s = "1+(2*3)/(2-1)" → 1

Track depth: increment on '(', decrement on ')', keep max.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Maximum Nesting Depth of the Parentheses"
desc=(
    "Given a valid parentheses string s, return the maximum nesting depth of s.\n\n"
    "The nesting depth of a position in the string is the number of open "
    "parentheses around it. The maximum nesting depth is the maximum depth "
    "over all positions.\n\n"
    "For example:\n"
    "s = \"(1+(2*3)+((8)/4))+1\" → max depth = 3\n"
    "s = \"(1)+((2))+(((3)))\" → max depth = 3\n"
    "s = \"1+(2*3)/(2-1)\" → max depth = 1\n\n"
    "Simple approach: maintain a depth counter. Increment by 1 when you see '(', "
    "decrement by 1 when you see ')'. Track the maximum value the counter reaches."
)
infmt="Single line containing valid parentheses string s."
outfmt="Print the maximum nesting depth."
cons="1 ≤ |s| ≤ 100\ns consists of digits, '+', '-', '*', '/', '(', ')'. s is valid."
e1="Input:\n(1+(2*3)+((8)/4))+1\n\nOutput:\n3"
e2="Input:\n(1)+((2))+(((3)))\n\nOutput:\n3"
e3="Input:\n1+(2*3)/(2-1)\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"String, Stack",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int maxDepth(String s) {
        // Write your code here — track depth with counter
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,int e,int tc,boolean h){int g=new CodeCoder().maxDepth(s);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("(1+(2*3)+((8)/4))+1",3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("(1)+((2))+(((3)))",3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("1+(2*3)/(2-1)",1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("()",1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("((()))",3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("()()()",1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("((((()))))",5,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("(())",2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("(a(b)c)",2,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("((a))((b))",2,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int maxDepth(string s){return 0;}};
// USER_CODE_END
void test(string s,int e,int tc,bool h=false){int g=CodeCoder().maxDepth(s);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("(1+(2*3)+((8)/4))+1",3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("(1)+((2))+(((3)))",3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("1+(2*3)/(2-1)",1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("()",1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("((()))",3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("()()()",1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("((((()))))",5,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("(())",2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("(a(b)c)",2,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("((a))((b))",2,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maxDepth(self, s):
        return 0
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().maxDepth(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:exp={e}:got={g}"))
try:test("(1+(2*3)+((8)/4))+1",3,1)
except:print("TC:1:FAIL:hidden")
try:test("(1)+((2))+(((3)))",3,2)
except:print("TC:2:FAIL:hidden")
try:test("1+(2*3)/(2-1)",1,3)
except:print("TC:3:FAIL:hidden")
try:test("()",1,4)
except:print("TC:4:FAIL:hidden")
try:test("((()))",3,5)
except:print("TC:5:FAIL:hidden")
try:test("()()()",1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("((((()))))",5,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("(())",2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("(a(b)c)",2,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("((a))((b))",2,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maxDepth(s) { return 0; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=maxDepth(s);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("(1+(2*3)+((8)/4))+1",3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("(1)+((2))+(((3)))",3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("1+(2*3)/(2-1)",1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("()",1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("((()))",3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("()()()",1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("((((()))))",5,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("(())",2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("(a(b)c)",2,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("((a))((b))",2,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
int maxDepth(char* s) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(char* s,int e,int tc,int h){
    int g=maxDepth(s);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%d:got=%d\\n",tc,s,e,g);}
}
int main(){
    runTest("(1+(2*3)+((8)/4))+1",3,1,0);
    runTest("(1)+((2))+(((3)))",3,2,0);
    runTest("1+(2*3)/(2-1)",1,3,0);
    runTest("()",1,4,0);
    runTest("((()))",3,5,0);
    runTest("()()()",1,6,1);
    runTest("((((()))))",5,7,1);
    runTest("(())",2,8,1);
    runTest("(a(b)c)",2,9,1);
    runTest("((a))((b))",2,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
