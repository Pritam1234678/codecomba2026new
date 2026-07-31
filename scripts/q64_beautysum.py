"""
Beauty of All Substrings
===========================
The beauty of a string is the difference between the frequency of the most
frequent character and the least frequent character that appears at least once.

Given a string s, return the sum of beauty of all its substrings.

Examples:
  s = "aabcb" → 5
  s = "aabcbaa" → 17

For each substring, compute maxFreq - minFreq (min over chars present).
Sum over all substrings.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Beauty Of All Substrings"
desc=(
    "The beauty of a string is defined as the difference between the frequency "
    "of the most frequent character and the frequency of the least frequent "
    "character that appears at least once in the string.\n\n"
    "Given a string s, return the sum of the beauty of ALL its substrings.\n\n"
    "For example:\n"
    "s = \"aabcb\" → sum of beauty of all substrings = 5\n"
    "s = \"aabcbaa\" → sum = 17\n\n"
    "Approach: iterate over all substrings. For each substring, maintain a "
    "character frequency count. Compute max frequency and min frequency "
    "(over characters present in the substring). Beauty = maxFreq - minFreq. "
    "Accumulate the sum."
)
infmt="Single line containing string s."
outfmt="Print the sum of beauty of all substrings."
cons="1 ≤ |s| ≤ 500\ns consists of lowercase English letters."
e1="Input:\naabcb\n\nOutput:\n5"
e2="Input:\naabcbaa\n\nOutput:\n17"
e3="Input:\na\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"String, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int beautySum(String s) {
        // Write your code here — all substrings, maxFreq - minFreq
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(String s,int e,int tc,boolean h){int g=new CodeCoder().beautySum(s);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("aabcb",5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("aabcbaa",17,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("a",0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("ab",0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("abc",0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("aaa",3,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("aabb",3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("abab",3,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("x",0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("abba",4,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int beautySum(string s){return 0;}};
// USER_CODE_END
void test(string s,int e,int tc,bool h=false){int g=CodeCoder().beautySum(s);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("aabcb",5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("aabcbaa",17,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a",0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("ab",0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("abc",0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("aaa",3,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("aabb",3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("abab",3,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("x",0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("abba",4,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def beautySum(self, s):
        return 0
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().beautySum(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:exp={e}:got={g}"))
try:test("aabcb",5,1)
except:print("TC:1:FAIL:hidden")
try:test("aabcbaa",17,2)
except:print("TC:2:FAIL:hidden")
try:test("a",0,3)
except:print("TC:3:FAIL:hidden")
try:test("ab",0,4)
except:print("TC:4:FAIL:hidden")
try:test("abc",0,5)
except:print("TC:5:FAIL:hidden")
try:test("aaa",3,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("aabb",3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("abab",3,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("x",0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("abba",4,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function beautySum(s) { return 0; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=beautySum(s);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("aabcb",5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("aabcbaa",17,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a",0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("ab",0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("abc",0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("aaa",3,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("aabb",3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("abab",3,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("x",0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("abba",4,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
int beautySum(char* s) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(char* s,int e,int tc,int h){
    int g=beautySum(s);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%d:got=%d\\n",tc,s,e,g);}
}
int main(){
    runTest("aabcb",5,1,0);
    runTest("aabcbaa",17,2,0);
    runTest("a",0,3,0);
    runTest("ab",0,4,0);
    runTest("abc",0,5,0);
    runTest("aaa",3,6,1);
    runTest("aabb",3,7,1);
    runTest("abab",3,8,1);
    runTest("x",0,9,1);
    runTest("abba",4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
