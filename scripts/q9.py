import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Roman to Integer"
desc="Given a Roman numeral string s, convert it to an integer. Roman numerals are represented by I=1, V=5, X=10, L=50, C=100, D=500, M=1000. When a smaller numeral appears before a larger one, it is subtracted (e.g., IV=4, IX=9, XL=40, XC=90, CD=400, CM=900)."
infmt="Single line containing Roman numeral string s."
outfmt="Print the integer value."
cons="1 ≤ |s| ≤ 15\ns contains only characters I, V, X, L, C, D, M.\n1 ≤ result ≤ 3999"
tl=3.0; ml=256; level="EASY"; topics="String, Math"
e1="Input:\nIII\n\nOutput:\n3"
e2="Input:\nLVIII\n\nOutput:\n58\n\nExplanation: L=50, V=5, III=3."
e3="Input:\nMCMXCIV\n\nOutput:\n1994\n\nExplanation: M=1000, CM=900, XC=90, IV=4."

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public int romanToInt(String s) { return 0; } }
// USER_CODE_END
public class Main {
static void test(String s, int e, int t, boolean h){int g=new Solution().romanToInt(s);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+s+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("III",3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("LVIII",58,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("MCMXCIV",1994,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("I",1,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("M",1000,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("CM",900,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int romanToInt(string s) { return 0; } };
// USER_CODE_END
void test(string s, int e, int t, bool h=false){int g=Solution().romanToInt(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<s<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("III",3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("LVIII",58,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("MCMXCIV",1994,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("I",1,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("M",1000,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("CM",900,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def romanToInt(self, s): return 0
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().romanToInt(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={repr(s)}:expected={e}:got={g}"))
try:test("III",3,1)
except:print("TC:1:FAIL:hidden")
try:test("LVIII",58,2)
except:print("TC:2:FAIL:hidden")
try:test("MCMXCIV",1994,3)
except:print("TC:3:FAIL:hidden")
try:test("I",1,4,True)
except:print("TC:4:FAIL:hidden")
try:test("M",1000,5,True)
except:print("TC:5:FAIL:hidden")
try:test("CM",900,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function romanToInt(s) { return 0; }
// USER_CODE_END
function test(s,e,t,h){const g=romanToInt(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${e}:got=${g}`);}
try{test("III",3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("LVIII",58,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("MCMXCIV",1994,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("I",1,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("M",1000,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("CM",900,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
// USER_CODE_START
int romanToInt(char* s) { return 0; }
// USER_CODE_END
void test(char* s, int e, int t, int h){int g=romanToInt(s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%s:expected=%d:got=%d\\n",t,s,e,g);}}
int main(){test("III",3,1,0);test("LVIII",58,2,0);test("MCMXCIV",1994,3,0);test("I",1,4,1);test("M",1000,5,1);test("CM",900,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
