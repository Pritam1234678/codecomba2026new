"""
Remove Consecutive Characters
===============================
Given a string s, remove all consecutive duplicate characters — keep only one
occurrence of each character run.

Examples:
  s = "aabaa" → "aba"
  s = "aaaa" → "a"
  s = "geeksforgeeks" → "geksforgeks"

Iterate and keep char if different from previous.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Consecutive Characters"
desc=(
    "Given a string s, remove all consecutive duplicate characters so that each "
    "sequence of identical adjacent characters is reduced to a single character.\n\n"
    "For example:\n"
    "s = \"aabaa\" → the runs are \"aa\", \"b\", \"aa\" → result = \"aba\"\n"
    "s = \"aaaa\" → result = \"a\"\n"
    "s = \"geeksforgeeks\" → result = \"geksforgeks\"\n\n"
    "Iterate through the string. Keep the current character if it is different "
    "from the previously kept character; otherwise skip it."
)
infmt="Single line containing string s."
outfmt="Print the string with consecutive duplicates removed."
cons="1 ≤ |s| ≤ 10^4\ns consists of lowercase English letters."
e1="Input:\naabaa\n\nOutput:\naba"
e2="Input:\naaaa\n\nOutput:\na"
e3="Input:\ngeeksforgeeks\n\nOutput:\ngeksforgeks"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"String",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String removeConsecutive(String s) {
        // Write your code here — keep char if different from previous
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(String s,String e,int tc,boolean h){String g=new CodeCoder().removeConsecutive(s);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("aabaa","aba",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("aaaa","a",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("geeksforgeeks","geksforgeks",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("abc","abc",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("a","a",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("aaabbbccc","abc",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("ababab","ababab",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("zzz","z",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("banana","banana",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("hello","helo",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string removeConsecutive(string s){return "";}};
// USER_CODE_END
void test(string s,string e,int tc,bool h=false){string g=CodeCoder().removeConsecutive(s);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("aabaa","aba",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("aaaa","a",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("geeksforgeeks","geksforgeks",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("abc","abc",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("a","a",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("aaabbbccc","abc",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("ababab","ababab",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("zzz","z",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("banana","banana",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("hello","helo",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def removeConsecutive(self, s):
        return ""
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().removeConsecutive(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:exp={repr(e)}:got={repr(g)}"))
try:test("aabaa","aba",1)
except:print("TC:1:FAIL:hidden")
try:test("aaaa","a",2)
except:print("TC:2:FAIL:hidden")
try:test("geeksforgeeks","geksforgeks",3)
except:print("TC:3:FAIL:hidden")
try:test("abc","abc",4)
except:print("TC:4:FAIL:hidden")
try:test("a","a",5)
except:print("TC:5:FAIL:hidden")
try:test("aaabbbccc","abc",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("ababab","ababab",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("zzz","z",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("banana","banana",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("hello","helo",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function removeConsecutive(s) { return ""; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=removeConsecutive(s);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test("aabaa","aba",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("aaaa","a",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("geeksforgeeks","geksforgeks",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("abc","abc",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("a","a",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("aaabbbccc","abc",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("ababab","ababab",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("zzz","z",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("banana","banana",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("hello","helo",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void removeConsecutive(char* s,char* out) {
    // Write your code here — store result in 'out'
    out[0]='\\0';
}
// USER_CODE_END

void runTest(char* s,char* e,int tc,int h){
    char out[20000]={0};
    removeConsecutive(s,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%s:got=%s\\n",tc,s,e,out);}
}
int main(){
    runTest("aabaa","aba",1,0);
    runTest("aaaa","a",2,0);
    runTest("geeksforgeeks","geksforgeks",3,0);
    runTest("abc","abc",4,0);
    runTest("a","a",5,0);
    runTest("aaabbbccc","abc",6,1);
    runTest("ababab","ababab",7,1);
    runTest("zzz","z",8,1);
    runTest("banana","banana",9,1);
    runTest("hello","helo",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
