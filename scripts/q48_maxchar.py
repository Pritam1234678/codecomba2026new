"""
Maximum Occuring Character
=============================
Given a string s of lowercase characters, find the character that appears
the most. If multiple characters have the same frequency, return the one
that appears first (leftmost in the string).

Examples:
  s = "testsample" → 't' (appears 3 times)
  s = "geeksforgeeks" → 'e' (appears 4 times)

Count frequencies with a 26-size array.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder (returns char)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Maximum Occuring Character"
desc=(
    "Given a string s consisting of lowercase English letters, find the character "
    "that occurs the maximum number of times.\n\n"
    "If two or more characters have the same maximum frequency, return the character "
    "that appears earliest in the string (the leftmost one).\n\n"
    "For example:\n"
    "s = \"testsample\" → character 't' occurs 3 times, which is maximum → return 't'\n"
    "s = \"geeksforgeeks\" → 'e' occurs 4 times → return 'e'\n\n"
    "Count the frequency of each character using a frequency array of size 26. "
    "Then find the character with the highest frequency, breaking ties by "
    "leftmost appearance."
)
infmt="Single line containing string s."
outfmt="Print the character with maximum frequency."
cons="1 ≤ |s| ≤ 10^5\ns consists of lowercase English letters only."
e1="Input:\ntestsample\n\nOutput:\nt"
e2="Input:\ngeeksforgeeks\n\nOutput:\ne"
e3="Input:\nabc\n\nOutput:\na"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"String, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public char maxOccuringChar(String s) {
        // Write your code here — count frequencies, return leftmost max
        return ' ';
    }
}
// USER_CODE_END

public class Main {
static void test(String s,char e,int tc,boolean h){char g=new CodeCoder().maxOccuringChar(s);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+s+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("testsample",'t',1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("geeksforgeeks",'e',2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("abc",'a',3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("aaaa",'a',4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("aabbcc",'a',5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("zzzxx",'z',6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("ababab",'a',7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("z",'z',8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("banana",'a',9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("hello",'l',10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:char maxOccuringChar(string s){return ' ';}};
// USER_CODE_END
void test(string s,char e,int tc,bool h=false){char g=CodeCoder().maxOccuringChar(s);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("testsample",'t',1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("geeksforgeeks",'e',2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("abc",'a',3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("aaaa",'a',4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("aabbcc",'a',5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("zzzxx",'z',6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("ababab",'a',7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("z",'z',8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("banana",'a',9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("hello",'l',10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maxOccuringChar(self, s):
        return ' '
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().maxOccuringChar(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:exp={e}:got={g}"))
try:test("testsample",'t',1)
except:print("TC:1:FAIL:hidden")
try:test("geeksforgeeks",'e',2)
except:print("TC:2:FAIL:hidden")
try:test("abc",'a',3)
except:print("TC:3:FAIL:hidden")
try:test("aaaa",'a',4)
except:print("TC:4:FAIL:hidden")
try:test("aabbcc",'a',5)
except:print("TC:5:FAIL:hidden")
try:test("zzzxx",'z',6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("ababab",'a',7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("z",'z',8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("banana",'a',9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("hello",'l',10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maxOccuringChar(s) { return ' '; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=maxOccuringChar(s);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("testsample",'t',1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("geeksforgeeks",'e',2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("abc",'a',3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("aaaa",'a',4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("aabbcc",'a',5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("zzzxx",'z',6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("ababab",'a',7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("z",'z',8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("banana",'a',9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("hello",'l',10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
char maxOccuringChar(char* s) {
    // Write your code here
    return ' ';
}
// USER_CODE_END

void runTest(char* s,char e,int tc,int h){
    char g=maxOccuringChar(s);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%c:got=%c\\n",tc,s,e,g);}
}
int main(){
    runTest("testsample",'t',1,0);
    runTest("geeksforgeeks",'e',2,0);
    runTest("abc",'a',3,0);
    runTest("aaaa",'a',4,0);
    runTest("aabbcc",'a',5,0);
    runTest("zzzxx",'z',6,1);
    runTest("ababab",'a',7,1);
    runTest("z",'z',8,1);
    runTest("banana",'a',9,1);
    runTest("hello",'l',10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
