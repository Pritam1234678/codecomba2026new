import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Longest Substring Without Repeating Characters"
desc="Given a string s, find the length of the longest substring without repeating characters."
infmt="Single line containing string s."
outfmt="Print the length of the longest substring without repeating characters."
cons="0 ≤ |s| ≤ 5 × 10^4\ns consists of English letters, digits, symbols and spaces."
tl=5.0; ml=256; level="MEDIUM"; topics="String, Sliding Window, Hash Table"
e1="Input:\nabcabcbb\n\nOutput:\n3\n\nExplanation: The answer is \"abc\" with length 3."
e2="Input:\nbbbbb\n\nOutput:\n1\n\nExplanation: The answer is \"b\" with length 1."
e3="Input:\npwwkew\n\nOutput:\n3\n\nExplanation: The answer is \"wke\" with length 3."

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public int lengthOfLongestSubstring(String s) { return 0; } }
// USER_CODE_END
public class Main {
static void test(String s, int e, int t, boolean h){int g=new Solution().lengthOfLongestSubstring(s);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+s+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("abcabcbb",3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("bbbbb",1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("pwwkew",3,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("",0,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(" ",1,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("au",2,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int lengthOfLongestSubstring(string s) { return 0; } };
// USER_CODE_END
void test(string s, int e, int t, bool h=false){int g=Solution().lengthOfLongestSubstring(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<s<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("abcabcbb",3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("bbbbb",1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("pwwkew",3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("",0,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(" ",1,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("au",2,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def lengthOfLongestSubstring(self, s): return 0
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().lengthOfLongestSubstring(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={repr(s)}:expected={e}:got={g}"))
try:test("abcabcbb",3,1)
except:print("TC:1:FAIL:hidden")
try:test("bbbbb",1,2)
except:print("TC:2:FAIL:hidden")
try:test("pwwkew",3,3)
except:print("TC:3:FAIL:hidden")
try:test("",0,4,True)
except:print("TC:4:FAIL:hidden")
try:test(" ",1,5,True)
except:print("TC:5:FAIL:hidden")
try:test("au",2,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function lengthOfLongestSubstring(s) { return 0; }
// USER_CODE_END
function test(s,e,t,h){const g=lengthOfLongestSubstring(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${e}:got=${g}`);}
try{test("abcabcbb",3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("bbbbb",1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("pwwkew",3,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("",0,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(" ",1,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("au",2,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
#include <string.h>
// USER_CODE_START
int lengthOfLongestSubstring(char* s) { return 0; }
// USER_CODE_END
void test(char* s, int e, int t, int h){int g=lengthOfLongestSubstring(s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%s:expected=%d:got=%d\\n",t,s,e,g);}}
int main(){test("abcabcbb",3,1,0);test("bbbbb",1,2,0);test("pwwkew",3,3,0);test("",0,4,1);test(" ",1,5,1);test("au",2,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
