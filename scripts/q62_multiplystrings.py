"""
Multiply Two Strings
=======================
Given two non-negative integers represented as strings num1 and num2,
return the product of num1 and num2 as a string.

Examples:
  num1 = "2", num2 = "3" → "6"
  num1 = "123", num2 = "456" → "56088"

Use digit-by-digit multiplication with a result array.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Multiply Two Strings"
desc=(
    "Given two non-negative integers num1 and num2 represented as strings, "
    "return the product of num1 and num2, also represented as a string.\n\n"
    "For example:\n"
    "num1 = \"2\", num2 = \"3\" → \"6\"\n"
    "num1 = \"123\", num2 = \"456\" → \"56088\"\n\n"
    "Approach: use a result array of size len1+len2. For each digit pair "
    "(i, j), the product contributes to result[i+j] and result[i+j+1]. "
    "Process carries, then convert to string skipping leading zeros."
)
infmt="First line contains num1.\nSecond line contains num2."
outfmt="Print the product as a string."
cons="1 ≤ |num1|, |num2| ≤ 200\nnum1 and num2 contain only digits, no leading zeros except '0' itself."
e1="Input:\n2\n3\n\nOutput:\n6"
e2="Input:\n123\n456\n\nOutput:\n56088"
e3="Input:\n0\n123\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"String, Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String multiply(String num1, String num2) {
        // Write your code here — digit-by-digit multiplication
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(String n1,String n2,String e,int tc,boolean h){String g=new CodeCoder().multiply(n1,n2);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n1="+n1+" n2="+n2+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("2","3","6",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("123","456","56088",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("0","123","0",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("9","9","81",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("10","10","100",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("999","999","998001",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("123456789","987654321","121932631112635269",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("1","1","1",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("100","100","10000",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("25","25","625",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string multiply(string n1,string n2){return "";}};
// USER_CODE_END
void test(string n1,string n2,string e,int tc,bool h=false){string g=CodeCoder().multiply(n1,n2);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n1="<<n1<<" n2="<<n2<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("2","3","6",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("123","456","56088",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("0","123","0",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("9","9","81",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("10","10","100",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("999","999","998001",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("123456789","987654321","121932631112635269",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("1","1","1",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("100","100","10000",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("25","25","625",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def multiply(self, num1, num2):
        return ""
# USER_CODE_END
def test(n1,n2,e,tc,h=False):g=CodeCoder().multiply(n1,n2);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n1={n1}:n2={n2}:exp={e}:got={g}"))
try:test("2","3","6",1)
except:print("TC:1:FAIL:hidden")
try:test("123","456","56088",2)
except:print("TC:2:FAIL:hidden")
try:test("0","123","0",3)
except:print("TC:3:FAIL:hidden")
try:test("9","9","81",4)
except:print("TC:4:FAIL:hidden")
try:test("10","10","100",5)
except:print("TC:5:FAIL:hidden")
try:test("999","999","998001",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test("123456789","987654321","121932631112635269",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test("1","1","1",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test("100","100","10000",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test("25","25","625",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function multiply(num1, num2) { return ""; }
// USER_CODE_END
function test(n1,n2,e,tc,h){if(h===undefined)h=false;const g=multiply(n1,n2);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test("2","3","6",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("123","456","56088",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("0","123","0",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("9","9","81",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("10","10","100",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("999","999","998001",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("123456789","987654321","121932631112635269",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("1","1","1",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("100","100","10000",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("25","25","625",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void multiply(char* n1,char* n2,char* out) {
    // Write your code here — store result in 'out'
    out[0]='\\0';
}
// USER_CODE_END

void runTest(char* n1,char* n2,char* e,int tc,int h){
    char out[500]={0};
    multiply(n1,n2,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n1=%s n2=%s:exp=%s:got=%s\\n",tc,n1,n2,e,out);}
}
int main(){
    runTest("2","3","6",1,0);
    runTest("123","456","56088",2,0);
    runTest("0","123","0",3,0);
    runTest("9","9","81",4,0);
    runTest("10","10","100",5,0);
    runTest("999","999","998001",6,1);
    runTest("123456789","987654321","121932631112635269",7,1);
    runTest("1","1","1",8,1);
    runTest("100","100","10000",9,1);
    runTest("25","25","625",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
