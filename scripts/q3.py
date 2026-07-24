import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Armstrong Number"
desc="Given an integer n, check if it is an Armstrong number. An Armstrong number is a number that equals the sum of its own digits each raised to the power of the number of digits."
infmt="A single line containing integer n."
outfmt="Print 'true' if n is an Armstrong number, otherwise 'false'."
cons="1 ≤ n ≤ 10^9"
tl=3.0; ml=256; level="EASY"; topics="Math"
e1="Input:\n153\n\nOutput:\ntrue\n\nExplanation: 1^3 + 5^3 + 3^3 = 1 + 125 + 27 = 153"
e2="Input:\n123\n\nOutput:\nfalse\n\nExplanation: 1^3 + 2^3 + 3^3 = 1 + 8 + 27 = 36 ≠ 123"
e3="Input:\n9474\n\nOutput:\ntrue\n\nExplanation: 9^4 + 4^4 + 7^4 + 4^4 = 6561 + 256 + 2401 + 256 = 9474"

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public boolean isArmstrong(int n) { return false; } }
// USER_CODE_END
public class Main {
static void test(int n, boolean e, int t, boolean h){boolean g=new Solution().isArmstrong(n);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+n+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(153,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:input=153:expected=true:got=ERR");}
try{test(123,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:input=123:expected=false:got=ERR");}
try{test(9474,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:input=9474:expected=true:got=ERR");}
try{test(1,true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(370,true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: bool isArmstrong(int n) { return false; } };
// USER_CODE_END
void test(int n, bool e, int t, bool h=false){bool g=Solution().isArmstrong(n);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<n<<":expected="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test(153,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(123,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(9474,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(370,true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def isArmstrong(self, n): return False
# USER_CODE_END
def test(n,e,t,h=False):g=Solution().isArmstrong(n);print(f"TC:{t}:PASS"+((":hidden") if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={n}:expected={e}:got={g}"))
try:test(153,True,1)
except:print("TC:1:FAIL:hidden")
try:test(123,False,2)
except:print("TC:2:FAIL:hidden")
try:test(9474,True,3)
except:print("TC:3:FAIL:hidden")
try:test(1,True,4,True)
except:print("TC:4:FAIL:hidden")
try:test(370,True,5,True)
except:print("TC:5:FAIL:hidden")
try:test(100,False,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function isArmstrong(n) { return false; }
// USER_CODE_END
function test(n,e,t,h){const g=isArmstrong(n);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${n}:expected=${e}:got=${g}`);}
try{test(153,true,1,false);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(123,false,2,false);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(9474,true,3,false);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(370,true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool isArmstrong(int n) { return false; }
// USER_CODE_END
void test(int n, bool e, int t, int h){bool g=isArmstrong(n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%d:expected=%s:got=%s\\n",t,n,e?"true":"false",g?"true":"false");}}
int main(){test(153,true,1,0);test(123,false,2,0);test(9474,true,3,0);test(1,true,4,1);test(370,true,5,1);test(100,false,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
