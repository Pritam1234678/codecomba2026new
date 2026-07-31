"""
Rotate String
===============
Given two strings s and goal, return true if s can become goal after some
number of shifts on s. A shift moves the leftmost character to the rightmost
position.

Examples:
  s = "abcde", goal = "cdeab" → true (shift twice: abcde→bcdea→cdeab)
  s = "abcde", goal = "abced" → false

Approach: if lengths differ → false. Check if goal is a substring of s+s.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rotate String"
desc=(
    "Given two strings s and goal, return true if and only if s can become goal "
    "after some number of shifts on s.\n\n"
    "A shift on s consists of moving the leftmost character of s to the rightmost "
    "position. For example, if s = \"abcde\", then it will be \"bcdea\" after one shift.\n\n"
    "For example:\n"
    "s = \"abcde\", goal = \"cdeab\" → true (abcde → bcdea → cdeab, two shifts)\n"
    "s = \"abcde\", goal = \"abced\" → false (no shift can produce this)\n\n"
    "Approach: if the lengths differ, return false. Otherwise, check whether goal "
    "is a substring of s concatenated with itself (s + s)."
)
infmt="First line contains string s.\nSecond line contains string goal."
outfmt="Print 'true' if s can rotate to goal, otherwise 'false'."
cons="1 ≤ |s|, |goal| ≤ 100\ns and goal consist of lowercase English letters."
e1="Input:\nabcde\ncdeab\n\nOutput:\ntrue"
e2="Input:\nabcde\nabced\n\nOutput:\nfalse"
e3="Input:\na\na\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"String",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean rotateString(String s, String goal) {
        // Write your code here — check if goal is substring of s+s
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,String g,boolean e,int tc,boolean h){boolean g2=new CodeCoder().rotateString(s,g);if(g2==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+" goal="+g+":exp="+e+":got="+g2);}
public static void main(String[] a){
try{test("abcde","cdeab",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("abcde","abced",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("a","a",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("ab","ba",true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("abc","cba",false,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("abcabc","bcabca",true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("abc","abcd",false,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("","",true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("aa","aa",true,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("abca","cabc",true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool rotateString(string s,string goal){return false;}};
// USER_CODE_END
void test(string s,string g,bool e,int tc,bool h=false){bool g2=CodeCoder().rotateString(s,g);if(g2==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g2?"true":"false")<<"\\n";}
int main(){
try{test("abcde","cdeab",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("abcde","abced",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a","a",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("ab","ba",true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("abc","cba",false,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("abcabc","bcabca",true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("abc","abcd",false,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("","",true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("aa","aa",true,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("abca","cabc",true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rotateString(self, s, goal):
        return False
# USER_CODE_END
def test(s,g,e,tc,h=False):g2=CodeCoder().rotateString(s,g);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g2==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:goal={g}:exp={e}:got={g2}"))
try:test("abcde","cdeab",True,1)
except:print("TC:1:FAIL:hidden")
try:test("abcde","abced",False,2)
except:print("TC:2:FAIL:hidden")
try:test("a","a",True,3)
except:print("TC:3:FAIL:hidden")
try:test("ab","ba",True,4)
except:print("TC:4:FAIL:hidden")
try:test("abc","cba",False,5)
except:print("TC:5:FAIL:hidden")
try:test("abcabc","bcabca",True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("abc","abcd",False,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("","",True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("aa","aa",True,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("abca","cabc",True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rotateString(s, goal) { return false; }
// USER_CODE_END
function test(s,g,e,tc,h){if(h===undefined)h=false;const g2=rotateString(s,g);if(g2===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g2);}
try{test("abcde","cdeab",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("abcde","abced",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a","a",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("ab","ba",true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("abc","cba",false,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("abcabc","bcabca",true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("abc","abcd",false,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("","",true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("aa","aa",true,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("abca","cabc",true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>

// USER_CODE_START
bool rotateString(char* s,char* goal) {
    // Write your code here
    return false;
}
// USER_CODE_END

void runTest(char* s,char* g,bool e,int tc,int h){
    bool g2=rotateString(s,g);
    if(g2==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s goal=%s:exp=%s:got=%s\\n",tc,s,g,e?"true":"false",g2?"true":"false");}
}
int main(){
    runTest("abcde","cdeab",true,1,0);
    runTest("abcde","abced",false,2,0);
    runTest("a","a",true,3,0);
    runTest("ab","ba",true,4,0);
    runTest("abc","cba",false,5,0);
    runTest("abcabc","bcabca",true,6,1);
    runTest("abc","abcd",false,7,1);
    runTest("","",true,8,1);
    runTest("aa","aa",true,9,1);
    runTest("abca","cabc",true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
