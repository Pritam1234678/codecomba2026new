"""
Greatest Common Divisor of Strings
=====================================
For two strings s and t, we say "t divides s" if s is made by concatenating
one or more copies of t. Given two strings str1 and str2, return the largest
string x such that x divides both str1 and str2.

Examples:
  str1 = "ABCABC", str2 = "ABC" → "ABC"
  str1 = "ABABAB", str2 = "ABAB" → "AB"
  str1 = "LEET", str2 = "CODE" → ""

If str1 + str2 != str2 + str1, there is no common divisor → "".
Otherwise, the GCD length is gcd(len(str1), len(str2)).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Greatest Common Divisor of Strings"
desc=(
    "For two strings s and t, we say 't divides s' if and only if s = t + t + ... + t "
    "(i.e., s is made by concatenating one or more copies of t).\n\n"
    "Given two strings str1 and str2, return the largest string x such that x divides "
    "both str1 and str2.\n\n"
    "For example:\n"
    "- str1 = \"ABCABC\", str2 = \"ABC\" → \"ABC\" divides both.\n"
    "- str1 = \"ABABAB\", str2 = \"ABAB\" → \"AB\" (not \"ABAB\" because ABAB doesn't divide ABABAB).\n"
    "- str1 = \"LEET\", str2 = \"CODE\" → \"\" (no common divisor).\n\n"
    "Key observation: if str1 + str2 != str2 + str1, there is no common divisor. "
    "Otherwise, the answer is str1.substr(0, gcd(len1, len2))."
)
infmt="First line contains str1.\nSecond line contains str2."
outfmt="Print the greatest common divisor string, or empty line if none."
cons="1 ≤ |str1|, |str2| ≤ 1000\nBoth strings consist of uppercase English letters."
e1="Input:\nABCABC\nABC\n\nOutput:\nABC"
e2="Input:\nABABAB\nABAB\n\nOutput:\nAB"
e3="Input:\nLEET\nCODE\n\nOutput:\n\n(empty line)"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"String, Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String gcdOfStrings(String str1, String str2) {
        // Write your code here — check concatenation, then use GCD of lengths
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(String s1,String s2,String e,int tc,boolean h){String g=new CodeCoder().gcdOfStrings(s1,s2);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s1="+s1+" s2="+s2+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("ABCABC","ABC","ABC",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("ABABAB","ABAB","AB",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("LEET","CODE","",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("AAAAAA","AA","AA",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("ABCDABCD","ABCD","ABCD",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("ABABABAB","AB","AB",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("AAAA","AAA","A",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("ABC","ABCD","",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("A","B","",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("B","B","B",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string gcdOfStrings(string s1,string s2){return "";}};
// USER_CODE_END
void test(string s1,string s2,string e,int tc,bool h=false){string g=CodeCoder().gcdOfStrings(s1,s2);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s1="<<s1<<" s2="<<s2<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("ABCABC","ABC","ABC",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("ABABAB","ABAB","AB",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("LEET","CODE","",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("AAAAAA","AA","AA",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("ABCDABCD","ABCD","ABCD",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("ABABABAB","AB","AB",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("AAAA","AAA","A",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("ABC","ABCD","",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("A","B","",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("B","B","B",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def gcdOfStrings(self, str1, str2):
        return ""
# USER_CODE_END
def test(s1,s2,e,tc,h=False):g=CodeCoder().gcdOfStrings(s1,s2);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s1={s1}:s2={s2}:exp={repr(e)}:got={repr(g)}"))
try:test("ABCABC","ABC","ABC",1)
except:print("TC:1:FAIL:hidden")
try:test("ABABAB","ABAB","AB",2)
except:print("TC:2:FAIL:hidden")
try:test("LEET","CODE","",3)
except:print("TC:3:FAIL:hidden")
try:test("AAAAAA","AA","AA",4)
except:print("TC:4:FAIL:hidden")
try:test("ABCDABCD","ABCD","ABCD",5)
except:print("TC:5:FAIL:hidden")
try:test("ABABABAB","AB","AB",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("AAAA","AAA","A",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("ABC","ABCD","",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("A","B","",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("B","B","B",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function gcdOfStrings(str1, str2) { return ""; }
// USER_CODE_END
function test(s1,s2,e,tc,h){if(h===undefined)h=false;const g=gcdOfStrings(s1,s2);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:s1="+s1+":s2="+s2+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test("ABCABC","ABC","ABC",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("ABABAB","ABAB","AB",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("LEET","CODE","",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("AAAAAA","AA","AA",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("ABCDABCD","ABCD","ABCD",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("ABABABAB","AB","AB",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("AAAA","AAA","A",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("ABC","ABCD","",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("A","B","",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("B","B","B",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// USER_CODE_START
void gcdOfStrings(char* s1, char* s2, char* out) {
    // Write your code here — store result in 'out'
    out[0] = '\\0';
}
// USER_CODE_END

void run(char* s1, char* s2, char* e, int tc, int hidden) {
    char out[2000] = {0};
    gcdOfStrings(s1, s2, out);
    if (strcmp(out, e) == 0) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:s1=%s:s2=%s:exp=%s:got=%s\\n", tc, s1, s2, e, out);
    }
}
int main() {
    run("ABCABC","ABC","ABC",1,0);
    run("ABABAB","ABAB","AB",2,0);
    run("LEET","CODE","",3,0);
    run("AAAAAA","AA","AA",4,0);
    run("ABCDABCD","ABCD","ABCD",5,0);
    run("ABABABAB","AB","AB",6,1);
    run("AAAA","AAA","A",7,1);
    run("ABC","ABCD","",8,1);
    run("A","B","",9,1);
    run("B","B","B",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
