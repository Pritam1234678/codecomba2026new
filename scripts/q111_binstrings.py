"""
Generate Binary Strings Without Consecutive 1s
================================================
Given a positive integer n, generate ALL binary strings of length n that have
NO two consecutive '1's. Return them as a single space-separated string, in
sorted (lexicographic) order.

Examples:
  n = 2 -> "00 01 10"
  n = 3 -> "000 001 010 100 101"

Recursive backtracking: at each position place '0', and place '1' only if the
previous character is not '1'. Collect all completed strings of length n.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the function writes the space-separated result into the char* out buffer.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Generate Binary Strings Without Consecutive 1s"
desc=(
    "Given a positive integer n, generate ALL binary strings of length n that "
    "contain NO two consecutive '1' characters. Return them as a single string "
    "with the strings separated by single spaces, in sorted (lexicographic) "
    "order.\n\n"
    "For example:\n"
    "n = 2 -> \"00 01 10\"\n"
    "n = 3 -> \"000 001 010 100 101\"\n\n"
    "Use recursive backtracking: at each position always try placing '0', and "
    "place '1' only when the previously placed character is not '1'. When the "
    "built string reaches length n, add it to the result. The natural "
    "0-before-1 ordering already produces lexicographic order."
)
infmt="A single integer n (length of the binary strings)."
outfmt="Print all valid binary strings of length n, space-separated, in sorted order."
cons="1 ≤ n ≤ 8\nAnswer is 2^n strings minus the invalid ones."
e1="Input:\n2\n\nOutput:\n00 01 10"
e2="Input:\n3\n\nOutput:\n000 001 010 100 101"
e3="Input:\n1\n\nOutput:\n0 1"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"String, Recursion, Backtracking",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

n6="000000 000001 000010 000100 000101 001000 001001 001010 010000 010001 010010 010100 010101 100000 100001 100010 100100 100101 101000 101001 101010"
n7="0000000 0000001 0000010 0000100 0000101 0001000 0001001 0001010 0010000 0010001 0010010 0010100 0010101 0100000 0100001 0100010 0100100 0100101 0101000 0101001 0101010 1000000 1000001 1000010 1000100 1000101 1001000 1001001 1001010 1010000 1010001 1010010 1010100 1010101"
n8="00000000 00000001 00000010 00000100 00000101 00001000 00001001 00001010 00010000 00010001 00010010 00010100 00010101 00100000 00100001 00100010 00100100 00100101 00101000 00101001 00101010 01000000 01000001 01000010 01000100 01000101 01001000 01001001 01001010 01010000 01010001 01010010 01010100 01010101 10000000 10000001 10000010 10000100 10000101 10001000 10001001 10001010 10010000 10010001 10010010 10010100 10010101 10100000 10100001 10100010 10100100 10100101 10101000 10101001 10101010"

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String generateBinary(int n) {
        // Write your code here — recursive backtracking
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(int n,String e,int tc,boolean hd){String r=new CodeCoder().generateBinary(n);if(e.equals(r))System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp=\\""+e+"\\":got=\\""+r+"\\"");}
public static void main(String[] a){
try{test(2,"00 01 10",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(3,"000 001 010 100 101",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,"0 1",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(4,"0000 0001 0010 0100 0101 1000 1001 1010",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(5,"00000 00001 00010 00100 00101 01000 01001 01010 10000 10001 10010 10100 10101",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(6,""""+n6+"""",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(7,""""+n7+"""",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(8,""""+n8+"""",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(9,"",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(10,"",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string generateBinary(int n){return "";}};
// USER_CODE_END
 void test(int n,string e,int tc,bool hd=false){string r=CodeCoder().generateBinary(n);if(e==r)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp=\\""<<e<<"\\":got=\\""<<r<<"\\"\\n";}
int main(){
try{test(2,"00 01 10",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(3,"000 001 010 100 101",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,"0 1",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(4,"0000 0001 0010 0100 0101 1000 1001 1010",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(5,"00000 00001 00010 00100 00101 01000 01001 01010 10000 10001 10010 10100 10101",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(6,""""+n6+"""",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(7,""""+n7+"""",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(8,""""+n8+"""",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(9,"",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(10,"",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def generateBinary(self, n):
        return ""
# USER_CODE_END
def test(n,e,tc,hd=False):r=CodeCoder().generateBinary(n);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={n}:exp={e!r}:got={r!r}"))
try:test(2,"00 01 10",1)
except:print("TC:1:FAIL:hidden")
try:test(3,"000 001 010 100 101",2)
except:print("TC:2:FAIL:hidden")
try:test(1,"0 1",3)
except:print("TC:3:FAIL:hidden")
try:test(4,"0000 0001 0010 0100 0101 1000 1001 1010",4)
except:print("TC:4:FAIL:hidden")
try:test(5,"00000 00001 00010 00100 00101 01000 01001 01010 10000 10001 10010 10100 10101",5)
except:print("TC:5:FAIL:hidden")
try:test(6,""""+n6+"""",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(7,""""+n7+"""",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(8,""""+n8+"""",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(9,"",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(10,"",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function generateBinary(n) { return ""; }
// USER_CODE_END
function test(n,e,tc,hd){if(hd===undefined)hd=false;const r=generateBinary(n);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+JSON.stringify(e)+":got="+JSON.stringify(r));}
try{test(2,"00 01 10",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(3,"000 001 010 100 101",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,"0 1",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(4,"0000 0001 0010 0100 0101 1000 1001 1010",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(5,"00000 00001 00010 00100 00101 01000 01001 01010 10000 10001 10010 10100 10101",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(6,""""+n6+"""",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(7,""""+n7+"""",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(8,""""+n8+"""",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(9,"",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(10,"",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
void generateBinary(int n,char* out) {
    // Write your code here — write space-separated strings into out
    out[0]='\\0';
}
// USER_CODE_END

void runTest(int n,const char* e,int tc,int hd){
    char buf[65536];buf[0]='\\0';
    generateBinary(n,buf);
    int ok=1;int i=0;while(e[i]!='\\0'){if(buf[i]!=e[i]){ok=0;break;}i++;}
    if(ok&&buf[i]!='\\0')ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:exp=%s:got=%s\\n",tc,n,e,buf);}
}
int main(){
    runTest(2,"00 01 10",1,0);
    runTest(3,"000 001 010 100 101",2,0);
    runTest(1,"0 1",3,0);
    runTest(4,"0000 0001 0010 0100 0101 1000 1001 1010",4,0);
    runTest(5,"00000 00001 00010 00100 00101 01000 01001 01010 10000 10001 10010 10100 10101",5,0);
    runTest(6,""""+n6+"""",6,1);
    runTest(7,""""+n7+"""",7,1);
    runTest(8,""""+n8+"""",8,1);
    runTest(9,"",9,1);
    runTest(10,"",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
