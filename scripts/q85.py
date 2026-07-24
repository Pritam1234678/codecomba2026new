"""
Counting Bits
===============
Given an integer n, return an array ans of length n+1 where ans[i] is the
number of 1 bits (population count) in the binary representation of i.

Examples:
  n = 2 → [0, 1, 1]
    binary: 0→0, 1→1, 2→10 (one '1')

  n = 5 → [0, 1, 1, 2, 1, 2]
    binary: 0→0, 1→1, 2→10, 3→11, 4→100, 5→101

DP approach: For even i, bits[i] = bits[i/2].
For odd i, bits[i] = bits[i/2] + 1.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Counting Bits"
desc=(
    "Given an integer n, return an array ans of length n + 1 where ans[i] is the "
    "number of 1 bits (also called the population count) in the binary representation of i.\n\n"
    "For example:\n"
    "n = 2 → [0, 1, 1]\n"
    "  0 in binary is '0' → 0 ones\n"
    "  1 in binary is '1' → 1 one\n"
    "  2 in binary is '10' → 1 one\n\n"
    "n = 5 → [0, 1, 1, 2, 1, 2]\n\n"
    "A DP approach: observe that bits[i] = bits[i >> 1] + (i & 1). "
    "For even numbers, right-shift by 1 gives the same number of bits. "
    "For odd numbers, add 1 for the LSB."
)
infmt="Single line containing integer n."
outfmt="Print n+1 space-separated integers."
cons="0 ≤ n ≤ 10^5"
e1="Input:\n2\n\nOutput:\n0 1 1"
e2="Input:\n5\n\nOutput:\n0 1 1 2 1 2"
e3="Input:\n0\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Dynamic Programming, Bit Manipulation",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] countBits(int n) {
        // Write your code here — DP: bits[i] = bits[i>>1] + (i&1)
        return new int[0];
    }
}
// USER_CODE_END

public class Main {
static void test(int n,int[] e,int tc,boolean h){int[] g=new CodeCoder().countBits(n);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(2,new int[]{0,1,1},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(5,new int[]{0,1,1,2,1,2},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(0,new int[]{0},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,new int[]{0,1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(3,new int[]{0,1,1,2},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(8,new int[]{0,1,1,2,1,2,2,3,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(10,new int[]{0,1,1,2,1,2,2,3,1,2,2},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(15,new int[]{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(20,new int[]{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(7,new int[]{0,1,1,2,1,2,2,3},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> countBits(int n){return {};}};
// USER_CODE_END
void test(int n,vector<int> e,int tc,bool h=false){auto g=CodeCoder().countBits(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:n="<<n<<":got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test(2,{0,1,1},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(5,{0,1,1,2,1,2},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(0,{0},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,{0,1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(3,{0,1,1,2},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(8,{0,1,1,2,1,2,2,3,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(10,{0,1,1,2,1,2,2,3,1,2,2},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(15,{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(20,{0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(7,{0,1,1,2,1,2,2,3},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countBits(self, n):
        return []
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().countBits(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
try:test(2,[0,1,1],1)
except:print("TC:1:FAIL:hidden")
try:test(5,[0,1,1,2,1,2],2)
except:print("TC:2:FAIL:hidden")
try:test(0,[0],3)
except:print("TC:3:FAIL:hidden")
try:test(1,[0,1],4)
except:print("TC:4:FAIL:hidden")
try:test(3,[0,1,1,2],5)
except:print("TC:5:FAIL:hidden")
try:test(8,[0,1,1,2,1,2,2,3,1],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(10,[0,1,1,2,1,2,2,3,1,2,2],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(15,[0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(20,[0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(7,[0,1,1,2,1,2,2,3],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countBits(n) { return []; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=countBits(n);const gs=JSON.stringify(g),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+es+":got="+gs);}
try{test(2,[0,1,1],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(5,[0,1,1,2,1,2],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(0,[0],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,[0,1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(3,[0,1,1,2],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(8,[0,1,1,2,1,2,2,3,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(10,[0,1,1,2,1,2,2,3,1,2,2],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(15,[0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(20,[0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(7,[0,1,1,2,1,2,2,3],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int* countBits(int n,int* rs){*rs=0;return NULL;}
// USER_CODE_END
int arrEq(int*a,int*b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int n,int* e,int en,int tc,int h){int rs;int*g=countBits(n,&rs);if(rs==en&&arrEq(g,e,rs)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int e1[]={0,1,1};run(2,e1,3,1,0);
int e2[]={0,1,1,2,1,2};run(5,e2,6,2,0);
int e3[]={0};run(0,e3,1,3,0);
int e4[]={0,1};run(1,e4,2,4,0);
int e5[]={0,1,1,2};run(3,e5,4,5,0);
int e6[]={0,1,1,2,1,2,2,3,1};run(8,e6,9,6,1);
int e7[]={0,1,1,2,1,2,2,3,1,2,2};run(10,e7,11,7,1);
int e8[]={0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4};run(15,e8,16,8,1);
int e9[]={0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2};run(20,e9,21,9,1);
int e10[]={0,1,1,2,1,2,2,3};run(7,e10,8,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
