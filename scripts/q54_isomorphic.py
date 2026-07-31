"""
Isomorphic Strings
====================
Given two strings s and t, determine if they are isomorphic. Two strings are
isomorphic if the characters in s can be replaced to get t. All occurrences
of a character must be replaced with another character while preserving order.
No two characters may map to the same character, and a character may map to itself.

Examples:
  s = "egg", t = "add" → true (e→a, g→d)
  s = "foo", t = "bar" → false (f→b, o→a but o also →r)
  s = "paper", t = "title" → true

Use two maps to track s→t and t→s mappings.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Isomorphic Strings"
desc=(
    "Given two strings s and t, determine if they are isomorphic.\n\n"
    "Two strings s and t are isomorphic if the characters in s can be replaced "
    "to get t. All occurrences of a character must be replaced with another "
    "character while preserving the order of characters. No two characters may "
    "map to the same character, but a character may map to itself.\n\n"
    "For example:\n"
    "s = \"egg\", t = \"add\" → true (e maps to a, g maps to d)\n"
    "s = \"foo\", t = \"bar\" → false (o would need to map to both a and r)\n"
    "s = \"paper\", t = \"title\" → true\n\n"
    "Use two hash maps: one for s→t mapping and one for t→s mapping. "
    "If a mapping conflicts in either direction, return false."
)
infmt="First line contains string s.\nSecond line contains string t."
outfmt="Print 'true' if isomorphic, otherwise 'false'."
cons="1 ≤ |s|, |t| ≤ 5*10^4\ns and t consist of any valid ASCII characters."
e1="Input:\negg\nadd\n\nOutput:\ntrue"
e2="Input:\nfoo\nbar\n\nOutput:\nfalse"
e3="Input:\npaper\ntitle\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"String, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean isIsomorphic(String s, String t) {
        // Write your code here — two maps
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,String t,boolean e,int tc,boolean h){boolean g=new CodeCoder().isIsomorphic(s,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+" t="+t+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("egg","add",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("foo","bar",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("paper","title",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("a","a",true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("ab","aa",false,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("abc","xyz",true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("","",true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("ab","cd",true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("badc","baba",false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("aab","xxy",true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool isIsomorphic(string s,string t){return false;}};
// USER_CODE_END
void test(string s,string t,bool e,int tc,bool h=false){bool g=CodeCoder().isIsomorphic(s,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test("egg","add",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("foo","bar",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("paper","title",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("a","a",true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("ab","aa",false,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("abc","xyz",true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("","",true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("ab","cd",true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("badc","baba",false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("aab","xxy",true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def isIsomorphic(self, s, t):
        return False
# USER_CODE_END
def test(s,t,e,tc,h=False):g=CodeCoder().isIsomorphic(s,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:t={t}:exp={e}:got={g}"))
try:test("egg","add",True,1)
except:print("TC:1:FAIL:hidden")
try:test("foo","bar",False,2)
except:print("TC:2:FAIL:hidden")
try:test("paper","title",True,3)
except:print("TC:3:FAIL:hidden")
try:test("a","a",True,4)
except:print("TC:4:FAIL:hidden")
try:test("ab","aa",False,5)
except:print("TC:5:FAIL:hidden")
try:test("abc","xyz",True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("","",True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("ab","cd",True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("badc","baba",False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("aab","xxy",True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function isIsomorphic(s, t) { return false; }
// USER_CODE_END
function test(s,t,e,tc,h){if(h===undefined)h=false;const g=isIsomorphic(s,t);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("egg","add",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("foo","bar",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("paper","title",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("a","a",true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("ab","aa",false,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("abc","xyz",true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("","",true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("ab","cd",true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("badc","baba",false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("aab","xxy",true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>

// USER_CODE_START
bool isIsomorphic(char* s,char* t) {
    // Write your code here
    return false;
}
// USER_CODE_END

void runTest(char* s,char* t,bool e,int tc,int h){
    bool g=isIsomorphic(s,t);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s t=%s:exp=%s:got=%s\\n",tc,s,t,e?"true":"false",g?"true":"false");}
}
int main(){
    runTest("egg","add",true,1,0);
    runTest("foo","bar",false,2,0);
    runTest("paper","title",true,3,0);
    runTest("a","a",true,4,0);
    runTest("ab","aa",false,5,0);
    runTest("abc","xyz",true,6,1);
    runTest("","",true,7,1);
    runTest("ab","cd",true,8,1);
    runTest("badc","baba",false,9,1);
    runTest("aab","xxy",true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
