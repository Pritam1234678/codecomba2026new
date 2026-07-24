"""
Robot Return to Origin
=======================
There is a robot starting at position (0,0) facing north on a 2D plane.
Given a string moves representing the sequence of moves (U=up, D=down,
L=left, R=right), determine if the robot returns to (0,0) after all moves.

Examples:
  "UD" → true  (up then down, back to origin)
  "LL" → false (moved left twice, position (-2,0))
  "RRDD" → false (moved right twice then down twice, position (2,-2))
  "LDRRLRUULR" → true

Simply track x,y coordinates and adjust per move. At end check if x==0 && y==0.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Robot Return to Origin"
desc=(
    "There is a robot starting at position (0,0) on a 2D plane. "
    "Given a string moves representing the sequence of moves where:\n"
    "- 'U' = up (y+1), 'D' = down (y-1)\n"
    "- 'L' = left (x-1), 'R' = right (x+1)\n\n"
    "Return true if the robot returns to (0,0) after completing all moves, "
    "otherwise false.\n\n"
    "For example:\n"
    "- \"UD\" → true: up to (0,1), down back to (0,0)\n"
    "- \"LL\" → false: left to (-1,0), then left to (-2,0)\n"
    "- \"RRDD\" → false: right to (2,0), down to (2,-2)\n\n"
    "Simple approach: track x and y, increment/decrement based on move, "
    "return x == 0 && y == 0 at the end."
)
infmt="Single line containing the moves string."
outfmt="Print 'true' if robot returns to origin, otherwise 'false'."
cons="1 ≤ |moves| ≤ 2*10^4\nmoves consists of characters 'U', 'D', 'L', 'R' only."
e1="Input:\nUD\n\nOutput:\ntrue\n\nExplanation: U→(0,1), D→(0,0)."
e2="Input:\nLL\n\nOutput:\nfalse\n\nExplanation: L→(-1,0), L→(-2,0). Not at origin."
e3="Input:\nRRDD\n\nOutput:\nfalse"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"String, Simulation",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean judgeCircle(String moves) {
        // Write your code here — track x,y
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(String m,boolean e,int tc,boolean h){boolean g=new CodeCoder().judgeCircle(m);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+m+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("UD",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("LL",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("RRDD",false,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("LDRRLRUULR",true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("",true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("URDL",true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("UUUU",false,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("UUDDLRLR",true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("RLRUDDUL",true,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("LLLLRRRR",true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool judgeCircle(string m){return false;}};
// USER_CODE_END
void test(string m,bool e,int tc,bool h=false){bool g=CodeCoder().judgeCircle(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:input="<<m<<":exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test("UD",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("LL",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("RRDD",false,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("LDRRLRUULR",true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("",true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("URDL",true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("UUUU",false,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("UUDDLRLR",true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("RLRUDDUL",true,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("LLLLRRRR",true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def judgeCircle(self, moves):
        return False
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().judgeCircle(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:input={m}:exp={e}:got={g}"))
try:test("UD",True,1)
except:print("TC:1:FAIL:hidden")
try:test("LL",False,2)
except:print("TC:2:FAIL:hidden")
try:test("RRDD",False,3)
except:print("TC:3:FAIL:hidden")
try:test("LDRRLRUULR",True,4)
except:print("TC:4:FAIL:hidden")
try:test("",True,5)
except:print("TC:5:FAIL:hidden")
try:test("URDL",True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("UUUU",False,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("UUDDLRLR",True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("RLRUDDUL",True,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("LLLLRRRR",True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function judgeCircle(moves) { return false; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=judgeCircle(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:input="+m+":exp="+e+":got="+g);}
try{test("UD",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("LL",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("RRDD",false,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("LDRRLRUULR",true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("",true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("URDL",true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("UUUU",false,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("UUDDLRLR",true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("RLRUDDUL",true,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("LLLLRRRR",true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
bool judgeCircle(char* m){return false;}
// USER_CODE_END
void run(char* m,bool e,int tc,int h){bool g=judgeCircle(m);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:input=%s:exp=%s:got=%s\\n",tc,m,e?"true":"false",g?"true":"false");}}
int main(){
run("UD",true,1,0);run("LL",false,2,0);run("RRDD",false,3,0);run("LDRRLRUULR",true,4,0);run("",true,5,0);
run("URDL",true,6,1);run("UUUU",false,7,1);run("UUDDLRLR",true,8,1);run("RLRUDDUL",true,9,1);run("LLLLRRRR",true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
