import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Valid Anagram"
desc="Given two strings s and t, return true if t is an anagram of s, and false otherwise. An anagram is a word formed by rearranging the letters of another word, using all the original letters exactly once."
infmt="First line contains string s.\nSecond line contains string t."
outfmt="Print 'true' if t is an anagram of s, otherwise 'false'."
cons="1 ≤ |s|, |t| ≤ 5 × 10^4\ns and t consist of lowercase English letters only."
tl=3.0; ml=256; level="EASY"; topics="String, Hash Table"
e1="Input:\nanagram\nnagaram\n\nOutput:\ntrue"
e2="Input:\nrat\ncar\n\nOutput:\nfalse"
e3="Input:\na\na\n\nOutput:\ntrue"

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public boolean isAnagram(String s, String t) { return false; } }
// USER_CODE_END
public class Main {
static void test(String s, String t, boolean e, int tc, boolean h){boolean g=new Solution().isAnagram(s,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input=s="+s+" t="+t+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("anagram","nagaram",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("rat","car",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("a","a",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("","",true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("abc","cba",true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("listen","silent",true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: bool isAnagram(string s, string t) { return false; } };
// USER_CODE_END
void test(string s, string t, bool e, int tc, bool h=false){bool g=Solution().isAnagram(s,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:input=s="<<s<<" t="<<t<<":expected="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test("anagram","nagaram",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("rat","car",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a","a",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("","",true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("abc","cba",true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("listen","silent",true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def isAnagram(self, s, t): return False
# USER_CODE_END
def test(s,t,e,tc,h=False):g=Solution().isAnagram(s,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:input=s={s} t={t}:expected={e}:got={g}"))
try:test("anagram","nagaram",True,1)
except:print("TC:1:FAIL:hidden")
try:test("rat","car",False,2)
except:print("TC:2:FAIL:hidden")
try:test("a","a",True,3)
except:print("TC:3:FAIL:hidden")
try:test("","",True,4,True)
except:print("TC:4:FAIL:hidden")
try:test("abc","cba",True,5,True)
except:print("TC:5:FAIL:hidden")
try:test("listen","silent",True,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function isAnagram(s,t) { return false; }
// USER_CODE_END
function test(s,t,e,tc,h){const g=isAnagram(s,t);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:input=s=${s} t=${t}:expected=${e}:got=${g}`);}
try{test("anagram","nagaram",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("rat","car",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a","a",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("","",true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("abc","cba",true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("listen","silent",true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
bool isAnagram(char* s, char* t) { return false; }
// USER_CODE_END
void test(char* s, char* t, bool e, int tc, int h){bool g=isAnagram(s,t);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:input=s=%s t=%s:expected=%s:got=%s\\n",tc,s,t,e?"true":"false",g?"true":"false");}}
int main(){test("anagram","nagaram",true,1,0);test("rat","car",false,2,0);test("a","a",true,3,0);test("","",true,4,1);test("abc","cba",true,5,1);test("listen","silent",true,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
