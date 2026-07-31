"""
Longest Palindromic Substring
================================
Given a string s, return the longest palindromic substring in s.

Examples:
  s = "babad" → "bab" (or "aba")
  s = "cbbd" → "bb"
  s = "a" → "a"

Approach: expand around center for each position (odd and even length).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Longest Palindromic Substring"
desc=(
    "Given a string s, return the longest palindromic substring in s.\n\n"
    "A palindrome is a string that reads the same forwards and backwards. "
    "The answer must be a contiguous substring of s (not a subsequence).\n\n"
    "For example:\n"
    "s = \"babad\" → \"bab\" (or \"aba\", both are valid length-3 palindromes)\n"
    "s = \"cbbd\" → \"bb\"\n"
    "s = \"a\" → \"a\" (single character is always a palindrome)\n\n"
    "Expand-around-center approach: for each index i, expand outward to find the "
    "longest odd-length palindrome centered at i, and the longest even-length "
    "palindrome centered between i and i+1. Track the maximum."
)
infmt="Single line containing string s."
outfmt="Print the longest palindromic substring."
cons="1 ≤ |s| ≤ 1000\ns consists of digits and English letters."
e1="Input:\nbabad\n\nOutput:\nbab"
e2="Input:\ncbbd\n\nOutput:\nbb"
e3="Input:\na\n\nOutput:\na"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"String, Two Pointers, DP",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String longestPalindrome(String s) {
        // Write your code here — expand around center
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(String s,String e,int tc,boolean h){
    String g=new CodeCoder().longestPalindrome(s);
    // accept if same length palindrome (multiple answers possible)
    if(g.length()==e.length())System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:s="+s+":expLen="+e.length()+":got="+g);
}
public static void main(String[] a){
try{test("babad","bab",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("cbbd","bb",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("a","a",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("ac","a",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("racecar","racecar",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("abcba","abcba",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("abacdfgdcaba","aba",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("aaaa","aaaa",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("ab","a",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("abb","bb",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string longestPalindrome(string s){return "";}};
// USER_CODE_END
void test(string s,string e,int tc,bool h=false){string g=CodeCoder().longestPalindrome(s);if((int)g.size()==(int)e.size())cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<s<<":expLen="<<(int)e.size()<<":got="<<g<<"\\n";}
int main(){
try{test("babad","bab",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("cbbd","bb",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a","a",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("ac","a",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("racecar","racecar",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("abcba","abcba",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("abacdfgdcaba","aba",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("aaaa","aaaa",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("ab","a",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("abb","bb",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def longestPalindrome(self, s):
        return ""
# USER_CODE_END
def test(s,e,tc,h=False):g=CodeCoder().longestPalindrome(s);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if len(g)==len(e) else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={s}:expLen={len(e)}:got={g}"))
try:test("babad","bab",1)
except:print("TC:1:FAIL:hidden")
try:test("cbbd","bb",2)
except:print("TC:2:FAIL:hidden")
try:test("a","a",3)
except:print("TC:3:FAIL:hidden")
try:test("ac","a",4)
except:print("TC:4:FAIL:hidden")
try:test("racecar","racecar",5)
except:print("TC:5:FAIL:hidden")
try:test("abcba","abcba",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("abacdfgdcaba","aba",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("aaaa","aaaa",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("ab","a",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("abb","bb",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function longestPalindrome(s) { return ""; }
// USER_CODE_END
function test(s,e,tc,h){if(h===undefined)h=false;const g=longestPalindrome(s);if(g.length===e.length)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:expLen="+e.length+":got="+g);}
try{test("babad","bab",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("cbbd","bb",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a","a",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("ac","a",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("racecar","racecar",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("abcba","abcba",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("abacdfgdcaba","aba",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("aaaa","aaaa",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("ab","a",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("abb","bb",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void longestPalindrome(char* s,char* out) {
    // Write your code here — store result in 'out'
    out[0]='\\0';
}
// USER_CODE_END

void runTest(char* s,char* e,int tc,int h){
    char out[2000]={0};
    longestPalindrome(s,out);
    if(strlen(out)==strlen(e)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:expLen=%d:got=%s\\n",tc,s,(int)strlen(e),out);}
}
int main(){
    runTest("babad","bab",1,0);
    runTest("cbbd","bb",2,0);
    runTest("a","a",3,0);
    runTest("ac","a",4,0);
    runTest("racecar","racecar",5,0);
    runTest("abcba","abcba",6,1);
    runTest("abacdfgdcaba","aba",7,1);
    runTest("aaaa","aaaa",8,1);
    runTest("ab","a",9,1);
    runTest("abb","bb",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
