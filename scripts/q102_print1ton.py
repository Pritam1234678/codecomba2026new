"""
Print 1 to N without using loops
=================================
Given an integer n, print the numbers 1 through n (in order) without using any
loop. Return them as a single string with space separation.

Examples:
  n = 10 -> "1 2 3 4 5 6 7 8 9 10"
  n = 5  -> "1 2 3 4 5"

Use recursion: print(i) calls print(i+1) until i > n.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the function writes the result into the provided char* out buffer.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Print 1 to N without using loops"
desc=(
    "Given a positive integer n, print the numbers 1, 2, ..., n in increasing "
    "order WITHOUT using any loop (no for/while). Return them as a single "
    "space-separated string.\n\n"
    "For example:\n"
    "n = 10 -> \"1 2 3 4 5 6 7 8 9 10\"\n"
    "n = 5  -> \"1 2 3 4 5\"\n\n"
    "Use recursion: a helper function printNumbers(i) emits i and then calls "
    "itself with i+1, stopping when i exceeds n."
)
infmt="A single integer n."
outfmt="Print the numbers 1..n separated by single spaces (no trailing spaces)."
cons="1 ≤ n ≤ 1000"
e1="Input:\n10\n\nOutput:\n1 2 3 4 5 6 7 8 9 10"
e2="Input:\n5\n\nOutput:\n1 2 3 4 5"
e3="Input:\n1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String print1toN(int n) {
        // Write your code here — recursive, no loops
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(int n,String e,int tc,boolean hd){String r=new CodeCoder().print1toN(n);if(e.equals(r))System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp=\""+e+"\":got=\""+r+"\"");}
public static void main(String[] a){
try{test(10,"1 2 3 4 5 6 7 8 9 10",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(5,"1 2 3 4 5",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,"1",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(3,"1 2 3",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(8,"1 2 3 4 5 6 7 8",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(15,"1 2 3 4 5 6 7 8 9 10 11 12 13 14 15",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(2,"1 2",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(7,"1 2 3 4 5 6 7",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(12,"1 2 3 4 5 6 7 8 9 10 11 12",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(6,"1 2 3 4 5 6",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string print1toN(int n){return "";}};
// USER_CODE_END
void test(int n,string e,int tc,bool hd=false){string r=CodeCoder().print1toN(n);if(e==r)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp=\\""<<e<<"\\":got=\\""<<r<<"\\"\\n";}
int main(){
try{test(10,"1 2 3 4 5 6 7 8 9 10",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(5,"1 2 3 4 5",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,"1",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(3,"1 2 3",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(8,"1 2 3 4 5 6 7 8",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(15,"1 2 3 4 5 6 7 8 9 10 11 12 13 14 15",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(2,"1 2",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(7,"1 2 3 4 5 6 7",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(12,"1 2 3 4 5 6 7 8 9 10 11 12",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(6,"1 2 3 4 5 6",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def print1toN(self, n):
        return ""
# USER_CODE_END
def test(n,e,tc,hd=False):r=CodeCoder().print1toN(n);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={n}:exp={e!r}:got={r!r}"))
try:test(10,"1 2 3 4 5 6 7 8 9 10",1)
except:print("TC:1:FAIL:hidden")
try:test(5,"1 2 3 4 5",2)
except:print("TC:2:FAIL:hidden")
try:test(1,"1",3)
except:print("TC:3:FAIL:hidden")
try:test(3,"1 2 3",4)
except:print("TC:4:FAIL:hidden")
try:test(8,"1 2 3 4 5 6 7 8",5)
except:print("TC:5:FAIL:hidden")
try:test(15,"1 2 3 4 5 6 7 8 9 10 11 12 13 14 15",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(2,"1 2",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(7,"1 2 3 4 5 6 7",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(12,"1 2 3 4 5 6 7 8 9 10 11 12",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(6,"1 2 3 4 5 6",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function print1toN(n) { return ""; }
// USER_CODE_END
function test(n,e,tc,hd){if(hd===undefined)hd=false;const r=print1toN(n);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+JSON.stringify(e)+":got="+JSON.stringify(r));}
try{test(10,"1 2 3 4 5 6 7 8 9 10",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(5,"1 2 3 4 5",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,"1",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(3,"1 2 3",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(8,"1 2 3 4 5 6 7 8",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(15,"1 2 3 4 5 6 7 8 9 10 11 12 13 14 15",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(2,"1 2",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(7,"1 2 3 4 5 6 7",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(12,"1 2 3 4 5 6 7 8 9 10 11 12",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(6,"1 2 3 4 5 6",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
void print1toN(int n,char* out) {
    // Write your code here — write "1 2 ... n" into out, null-terminate
    out[0]='\\0';
}
// USER_CODE_END

void runTest(int n,const char* e,int tc,int hd){
    char buf[16384];buf[0]='\\0';
    print1toN(n,buf);
    int ok=1;int i=0;while(e[i]!='\\0'){if(buf[i]!=e[i]){ok=0;break;}i++;}
    if(ok&&buf[i]!='\\0')ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e,buf);}
}
int main(){
    runTest(10,"1 2 3 4 5 6 7 8 9 10",1,0);
    runTest(5,"1 2 3 4 5",2,0);
    runTest(1,"1",3,0);
    runTest(3,"1 2 3",4,0);
    runTest(8,"1 2 3 4 5 6 7 8",5,0);
    runTest(15,"1 2 3 4 5 6 7 8 9 10 11 12 13 14 15",6,1);
    runTest(2,"1 2",7,1);
    runTest(7,"1 2 3 4 5 6 7",8,1);
    runTest(12,"1 2 3 4 5 6 7 8 9 10 11 12",9,1);
    runTest(6,"1 2 3 4 5 6",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
