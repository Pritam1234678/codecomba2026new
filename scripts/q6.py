import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Reverse Words in a String"
desc="Given an input string s, reverse the order of the words. A word is defined as a sequence of non-space characters. The words in s will be separated by at least one space. Return a string of the words in reverse order concatenated by a single space. Remove leading, trailing, and extra spaces between words."
infmt="Single line containing string s."
outfmt="Print the reversed string with words separated by single space."
cons="1 ≤ |s| ≤ 10^4\ns contains English letters, digits, and spaces."
tl=5.0; ml=256; level="MEDIUM"; topics="String, Two Pointers"
e1="Input:\nthe sky is blue\n\nOutput:\nblue is sky the"
e2="Input:\n  hello world  \n\nOutput:\nworld hello"
e3="Input:\na good   example\n\nOutput:\nexample good a"

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public String reverseWords(String s) { return ""; } }
// USER_CODE_END
public class Main {
static void test(String s, String e, int t, boolean h){String g=new Solution().reverseWords(s);if(g.equals(e))System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+s+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("the sky is blue","blue is sky the",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("  hello world  ","world hello",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("a good   example","example good a",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("  ","",4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("hello","hello",5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("EPI","IPE",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: string reverseWords(string s) { return ""; } };
// USER_CODE_END
void test(string s, string e, int t, bool h=false){string g=Solution().reverseWords(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<s<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("the sky is blue","blue is sky the",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("  hello world  ","world hello",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a good   example","example good a",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("  ","",4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("hello","hello",5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("EPI","IPE",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def reverseWords(self, s): return ""
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().reverseWords(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={repr(s)}:expected={repr(e)}:got={repr(g)}"))
try:test("the sky is blue","blue is sky the",1)
except:print("TC:1:FAIL:hidden")
try:test("  hello world  ","world hello",2)
except:print("TC:2:FAIL:hidden")
try:test("a good   example","example good a",3)
except:print("TC:3:FAIL:hidden")
try:test("  ","",4,True)
except:print("TC:4:FAIL:hidden")
try:test("hello","hello",5,True)
except:print("TC:5:FAIL:hidden")
try:test("EPI","IPE",6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function reverseWords(s) { return ""; }
// USER_CODE_END
function test(s,e,t,h){const g=reverseWords(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${JSON.stringify(e)}:got=${JSON.stringify(g)}`);}
try{test("the sky is blue","blue is sky the",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("  hello world  ","world hello",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a good   example","example good a",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("  ","",4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("hello","hello",5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("EPI","IPE",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
#include <string.h>
// USER_CODE_START
void reverseWords(char* s) { }
// USER_CODE_END
void test(char* s, char* e, int t, int h){char cpy[10005];strcpy(cpy,s);reverseWords(cpy);if(strcmp(cpy,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%s:expected=%s:got=%s\\n",t,s,e,cpy);}}
int main(){char s1[100]="the sky is blue";test(s1,"blue is sky the",1,0);char s2[100]="  hello world  ";test(s2,"world hello",2,0);char s3[100]="a good   example";test(s3,"example good a",3,0);char s4[100]="  ";test(s4,"",4,1);char s5[100]="hello";test(s5,"hello",5,1);char s6[100]="EPI";test(s6,"IPE",6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
