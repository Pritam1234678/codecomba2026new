"""
Reorder Data In Log Files
===========================
You are given an array of logs. Each log is a space-delimited string where the
first word is an identifier. All the rest after the identifier are either all
letters (letter-log) or all digits (digit-log). Reorder the logs:
  - ALL letter-logs come before any digit-log.
  - letter-logs are sorted lexicographically by their content; if contents are
    identical, sort by the identifier.
  - digit-logs keep their original relative order.
Return the reordered array.

Examples:
  ["dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"]
  -> ["let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"]

Separate into letter and digit logs; sort the letter logs with a key of
(content, identifier); append the digit logs unchanged.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the logs are passed as char** logs with int n and the result is returned
via char** and int* returnSize.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reorder Data In Log Files"
desc=(
    "You are given an array of logs. Each log is a space-delimited string "
    "whose first word is an identifier; everything after the identifier is "
    "either ALL letters (a letter-log) or ALL digits (a digit-log). Reorder "
    "the logs so that:\n"
    "1. All letter-logs come before any digit-log.\n"
    "2. Letter-logs are sorted lexicographically by their CONTENT; if two "
    "contents are equal, sort by the identifier.\n"
    "3. Digit-logs keep their original relative order.\n"
    "Return the reordered array.\n\n"
    "For example:\n"
    "[\"dig1 8 1 5 1\",\"let1 art can\",\"dig2 3 6\",\"let2 own kit dig\",\"let3 art zero\"]\n"
    "-> [\"let1 art can\",\"let3 art zero\",\"let2 own kit dig\",\"dig1 8 1 5 1\",\"dig2 3 6\"]\n\n"
    "Split the logs into letter logs and digit logs. Sort the letter logs "
    "using the tuple (content, identifier) as the key, then concatenate with "
    "the digit logs (unchanged) at the end."
)
infmt="First line contains n (number of logs). Then n lines follow, each a log string."
outfmt="Print each reordered log on its own line."
cons="1 ≤ n ≤ 100\nEach log's identifier and content consist of lowercase letters or digits."
e1="Input:\n5\ndig1 8 1 5 1\nlet1 art can\ndig2 3 6\nlet2 own kit dig\nlet3 art zero\n\nOutput:\nlet1 art can\nlet3 art zero\nlet2 own kit dig\ndig1 8 1 5 1\ndig2 3 6"
e2="Input:\n5\na1 9 2 3 1\ng1 act car\nzo4 4 7\nab1 off key dog\na8 act zoo\n\nOutput:\ng1 act car\na8 act zoo\nab1 off key dog\na1 9 2 3 1\nzo4 4 7"
e3="Input:\n4\na1 9 2 3\nab1 off key\na2 off key\na8 act zoo\n\nOutput:\na8 act zoo\na2 off key\nab1 off key\na1 9 2 3"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"String, Sorting, Custom Comparator",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String[] reorderLogs(String[] logs) {
        // Write your code here — letter logs first, then digit logs
        return logs;
    }
}
// USER_CODE_END

public class Main {
static void test(String[] a,String[] e,int tc,boolean hd){String[] g=new CodeCoder().reorderLogs(a.clone());boolean ok=Arrays.equals(g,e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] x){
try{test(new String[]{"dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"},new String[]{"let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new String[]{"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo"},new String[]{"g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new String[]{"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo","a2 act car"},new String[]{"a2 act car","g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new String[]{"j mo","5 m w","g 07","o 2 0","t q h"},new String[]{"5 m w","j mo","t q h","g 07","o 2 0"},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new String[]{"1 n u","r 527","j w","6 14","da pd d"},new String[]{"1 n u","da pd d","j w","r 527","6 14"},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new String[]{"b c d","a b c","1 2 3"},new String[]{"a b c","b c d","1 2 3"},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new String[]{"a 1 2 3"},new String[]{"a 1 2 3"},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new String[]{"let9 art can","let8 art can"},new String[]{"let8 art can","let9 art can"},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new String[]{"x y z","m n o","3 4 5","1 2 3"},new String[]{"m n o","x y z","3 4 5","1 2 3"},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new String[]{"k i","l o","0 1","2 3"},new String[]{"k i","l o","0 1","2 3"},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<string> reorderLogs(vector<string>& logs){return logs;}};
// USER_CODE_END
void test(vector<string> a,vector<string> e,int tc,bool hd=false){vector<string> g=CodeCoder().reorderLogs(a);bool ok=(g==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<"\\""<<a[i]<<"\\"";}cout<<"]:exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<"\\""<<e[i]<<"\\"";}cout<<"]:got=[";for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<"\\""<<g[i]<<"\\"";}cout<<"]\\n";}}
int main(){
try{test({"dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"},{"let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo"},{"g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo","a2 act car"},{"a2 act car","g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({"j mo","5 m w","g 07","o 2 0","t q h"},{"5 m w","j mo","t q h","g 07","o 2 0"},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({"1 n u","r 527","j w","6 14","da pd d"},{"1 n u","da pd d","j w","r 527","6 14"},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({"b c d","a b c","1 2 3"},{"a b c","b c d","1 2 3"},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({"a 1 2 3"},{"a 1 2 3"},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({"let9 art can","let8 art can"},{"let8 art can","let9 art can"},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({"x y z","m n o","3 4 5","1 2 3"},{"m n o","x y z","3 4 5","1 2 3"},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({"k i","l o","0 1","2 3"},{"k i","l o","0 1","2 3"},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def reorderLogs(self, logs):
        return logs
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().reorderLogs(list(a));ok=(g==e);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test(["dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"],["let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"],1)
except:print("TC:1:FAIL:hidden")
try:test(["a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo"],["g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"],2)
except:print("TC:2:FAIL:hidden")
try:test(["a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo","a2 act car"],["a2 act car","g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"],3)
except:print("TC:3:FAIL:hidden")
try:test(["j mo","5 m w","g 07","o 2 0","t q h"],["5 m w","j mo","t q h","g 07","o 2 0"],4)
except:print("TC:4:FAIL:hidden")
try:test(["1 n u","r 527","j w","6 14","da pd d"],["1 n u","da pd d","j w","r 527","6 14"],5)
except:print("TC:5:FAIL:hidden")
try:test(["b c d","a b c","1 2 3"],["a b c","b c d","1 2 3"],6,True)
except:print("TC:6:FAIL:hidden")
try:test(["a 1 2 3"],["a 1 2 3"],7,True)
except:print("TC:7:FAIL:hidden")
try:test(["let9 art can","let8 art can"],["let8 art can","let9 art can"],8,True)
except:print("TC:8:FAIL:hidden")
try:test(["x y z","m n o","3 4 5","1 2 3"],["m n o","x y z","3 4 5","1 2 3"],9,True)
except:print("TC:9:FAIL:hidden")
try:test(["k i","l o","0 1","2 3"],["k i","l o","0 1","2 3"],10,True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function reorderLogs(logs) { return logs; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=reorderLogs(a.slice());let ok=g.length===e.length&&g.every((v,i)=>v===e[i]);if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test(["dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"],["let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(["a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo"],["g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(["a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo","a2 act car"],["a2 act car","g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(["j mo","5 m w","g 07","o 2 0","t q h"],["5 m w","j mo","t q h","g 07","o 2 0"],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(["1 n u","r 527","j w","6 14","da pd d"],["1 n u","da pd d","j w","r 527","6 14"],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(["b c d","a b c","1 2 3"],["a b c","b c d","1 2 3"],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(["a 1 2 3"],["a 1 2 3"],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(["let9 art can","let8 art can"],["let8 art can","let9 art can"],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(["x y z","m n o","3 4 5","1 2 3"],["m n o","x y z","3 4 5","1 2 3"],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(["k i","l o","0 1","2 3"],["k i","l o","0 1","2 3"],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
char** reorderLogs(char** logs,int n,int* rs) {
    // Write your code here — letter logs first (content,id), then digit logs
    *rs = 0; return NULL;
}
// USER_CODE_END

void runTest(char** a,int n,char** e,int tc,int hd){
    int rs=0;char** g=reorderLogs(a,n,&rs);
    int ok=(rs==n);
    if(ok)for(int i=0;i<n;i++){if(strcmp(g[i],e[i])!=0){ok=0;break;}}
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("\\"%s\\"",a[i]);}printf("]:exp=[");for(int i=0;i<n;i++){if(i)printf(",");printf("\\"%s\\"",e[i]);}printf("]:got=[");for(int i=0;i<rs;i++){if(i)printf(",");printf("\\"%s\\"",g[i]);}printf("]\\n");}
    free(g);
}
int main(){
    char* a1[]={"dig1 8 1 5 1","let1 art can","dig2 3 6","let2 own kit dig","let3 art zero"};char* e1[]={"let1 art can","let3 art zero","let2 own kit dig","dig1 8 1 5 1","dig2 3 6"};runTest(a1,5,e1,1,0);
    char* a2[]={"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo"};char* e2[]={"g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"};runTest(a2,5,e2,2,0);
    char* a3[]={"a1 9 2 3 1","g1 act car","zo4 4 7","ab1 off key dog","a8 act zoo","a2 act car"};char* e3[]={"a2 act car","g1 act car","a8 act zoo","ab1 off key dog","a1 9 2 3 1","zo4 4 7"};runTest(a3,6,e3,3,0);
    char* a4[]={"j mo","5 m w","g 07","o 2 0","t q h"};char* e4[]={"5 m w","j mo","t q h","g 07","o 2 0"};runTest(a4,5,e4,4,0);
    char* a5[]={"1 n u","r 527","j w","6 14","da pd d"};char* e5[]={"1 n u","da pd d","j w","r 527","6 14"};runTest(a5,5,e5,5,0);
    char* a6[]={"b c d","a b c","1 2 3"};char* e6[]={"a b c","b c d","1 2 3"};runTest(a6,3,e6,6,1);
    char* a7[]={"a 1 2 3"};char* e7[]={"a 1 2 3"};runTest(a7,1,e7,7,1);
    char* a8[]={"let9 art can","let8 art can"};char* e8[]={"let8 art can","let9 art can"};runTest(a8,2,e8,8,1);
    char* a9[]={"x y z","m n o","3 4 5","1 2 3"};char* e9[]={"m n o","x y z","3 4 5","1 2 3"};runTest(a9,4,e9,9,1);
    char* a10[]={"k i","l o","0 1","2 3"};char* e10[]={"k i","l o","0 1","2 3"};runTest(a10,4,e10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
