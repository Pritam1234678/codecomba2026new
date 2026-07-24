import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="String Compression"
desc="Given an array of characters chars, compress it in-place. Each group of consecutive repeating characters is replaced by the character followed by the count if count > 1. Return the new length of the compressed array."
infmt="First line contains integer n.\nSecond line contains n space-separated characters."
outfmt="Print the length of the compressed array."
cons="1 ≤ n ≤ 2000\nchars[i] is a lowercase English letter."
tl=5.0; ml=256; level="MEDIUM"; topics="String, Two Pointers"
e1="Input:\n7\na a b b c c c\n\nOutput:\n6\n\nExplanation: compressed is a2b2c3, length 6"
e2="Input:\n1\na\n\nOutput:\n1\n\nExplanation: compressed is a, length 1"
e3="Input:\n5\na b b b b b\n\nOutput:\n3\n\nExplanation: compressed is ab5, length 3"

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]; print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public int compress(char[] chars) { return 0; } }
// USER_CODE_END
public class Main {
static void test(char[] c, int e, int t, boolean h){char[] cp=Arrays.copyOf(c,c.length);int g=new Solution().compress(cp);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+Arrays.toString(c)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new char[]{'a','a','b','b','c','c','c'},6,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new char[]{'a'},1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new char[]{'a','b','b','b','b','b'},3,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new char[]{'a','a','a','a','a','a'},2,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new char[]{'a','b','c'},3,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new char[]{},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int compress(vector<char>& chars) { return 0; } };
// USER_CODE_END
void test(vector<char> c, int e, int t, bool h=false){vector<char> cp=c;int g=Solution().compress(cp);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input=[";for(size_t i=0;i<c.size();i++){if(i)cout<<",";cout<<c[i];}cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({'a','a','b','b','c','c','c'},6,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({'a'},1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({'a','b','b','b','b','b'},3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({'a','a','a','a','a','a'},2,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({'a','b','c'},3,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution:
    def compress(self, chars): return 0
# USER_CODE_END
def test(c,e,t,h=False):g=Solution().compress(c[:]);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={c}:expected={e}:got={g}"))
try:test(['a','a','b','b','c','c','c'],6,1)
except:print("TC:1:FAIL:hidden")
try:test(['a'],1,2)
except:print("TC:2:FAIL:hidden")
try:test(['a','b','b','b','b','b'],3,3)
except:print("TC:3:FAIL:hidden")
try:test(['a','a','a','a','a','a'],2,4,True)
except:print("TC:4:FAIL:hidden")
try:test(['a','b','c'],3,5,True)
except:print("TC:5:FAIL:hidden")
try:test([],0,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function compress(chars) { return 0; }
// USER_CODE_END
function test(c,e,t,h){const g=compress([...c]);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(c)}:expected=${e}:got=${g}`);}
try{test(['a','a','b','b','c','c','c'],6,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(['a'],1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(['a','b','b','b','b','b'],3,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(['a','a','a','a','a','a'],2,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(['a','b','c'],3,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
// USER_CODE_START
int compress(char* chars, int charsSize) { return 0; }
// USER_CODE_END
void test(char* c, int n, int e, int t, int h){char cp[2005];for(int i=0;i<n;i++)cp[i]=c[i];int g=compress(cp,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else{printf("TC:%d:FAIL:input=[",t);for(int i=0;i<n;i++){if(i)printf(",");printf("%c",c[i]);}printf("]:expected=%d:got=%d\\n",e,g);}}}
int main(){char t1[]={'a','a','b','b','c','c','c'};test(t1,7,6,1,0);char t2[]={'a'};test(t2,1,1,2,0);char t3[]={'a','b','b','b','b','b'};test(t3,6,3,3,0);char t4[]={'a','a','a','a','a','a'};test(t4,6,2,4,1);char t5[]={'a','b','c'};test(t5,3,3,5,1);char t6[]={};test(t6,0,0,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit(); print(f"Snippets inserted for {title}")
cur.close(); conn.close()
