import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Valid Palindrome"
desc="A phrase is a palindrome if, after converting all uppercase letters to lowercase and removing all non-alphanumeric characters, it reads the same forward and backward. Given a string s, return true if it is a palindrome, otherwise false."
infmt="Single line containing string s."
outfmt="Print 'true' if palindrome, otherwise 'false'."
cons="1 ≤ |s| ≤ 2 × 10^5\ns consists of printable ASCII characters."
tl=3.0; ml=256; level="EASY"; topics="String, Two Pointers"
e1="Input:\nA man, a plan, a canal: Panama\n\nOutput:\ntrue"
e2="Input:\nrace a car\n\nOutput:\nfalse"
e3="Input:\n \n\nOutput:\ntrue\n\nExplanation: Empty string after cleaning is a palindrome."

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public boolean isPalindrome(String s) { return false; } }
// USER_CODE_END
public class Main {
static void test(String s, boolean e, int t, boolean h){boolean g=new Solution().isPalindrome(s);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+s+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("A man, a plan, a canal: Panama",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("race a car",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(" ",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("",true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("ab_a",true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("0P",false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: bool isPalindrome(string s) { return false; } };
// USER_CODE_END
void test(string s, bool e, int t, bool h=false){bool g=Solution().isPalindrome(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<s<<":expected="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test("A man, a plan, a canal: Panama",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("race a car",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(" ",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("",true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("ab_a",true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("0P",false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def isPalindrome(self, s): return False
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().isPalindrome(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={repr(s)}:expected={e}:got={g}"))
try:test("A man, a plan, a canal: Panama",True,1)
except:print("TC:1:FAIL:hidden")
try:test("race a car",False,2)
except:print("TC:2:FAIL:hidden")
try:test(" ",True,3)
except:print("TC:3:FAIL:hidden")
try:test("",True,4,True)
except:print("TC:4:FAIL:hidden")
try:test("ab_a",True,5,True)
except:print("TC:5:FAIL:hidden")
try:test("0P",False,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function isPalindrome(s) { return false; }
// USER_CODE_END
function test(s,e,t,h){const g=isPalindrome(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${e}:got=${g}`);}
try{test("A man, a plan, a canal: Panama",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("race a car",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(" ",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("",true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("ab_a",true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("0P",false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
bool isPalindrome(char* s) { return false; }
// USER_CODE_END
void test(char* s, bool e, int t, int h){bool g=isPalindrome(s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%s:expected=%s:got=%s\\n",t,s,e?"true":"false",g?"true":"false");}}
int main(){test("A man, a plan, a canal: Panama",true,1,0);test("race a car",false,2,0);test(" ",true,3,0);test("",true,4,1);test("ab_a",true,5,1);test("0P",false,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
