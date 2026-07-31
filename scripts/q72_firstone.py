"""
First 1 in a Sorted Binary Array
==================================
Given a sorted array arr containing only 0s and 1s (all 0s first, then all 1s),
find the index of the first occurrence of 1. If no 1 exists, return -1.

Examples:
  arr = [0,0,0,1,1,1] → index 3
  arr = [1,1,1] → index 0
  arr = [0,0,0] → -1

Binary search for the boundary.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="First 1 in a Sorted Binary Array"
desc=(
    "Given a sorted array arr containing only 0s and 1s (all 0s come first, "
    "followed by all 1s), find the index of the first occurrence of 1.\n\n"
    "If the array contains no 1s, return -1.\n\n"
    "For example:\n"
    "arr = [0,0,0,1,1,1] → first 1 at index 3\n"
    "arr = [1,1,1] → first 1 at index 0\n"
    "arr = [0,0,0] → -1 (no 1s)\n\n"
    "Use binary search to find the boundary between 0s and 1s. "
    "If arr[mid] == 1, the first 1 is at or before mid — search left. "
    "If arr[mid] == 0, the first 1 is after mid — search right."
)
infmt="First line contains n.\nSecond line contains n space-separated integers (0s then 1s)."
outfmt="Print the index of the first 1, or -1."
cons="1 ≤ n ≤ 10^5\narr[i] is either 0 or 1. Array is sorted."
e1="Input:\n6\n0 0 0 1 1 1\n\nOutput:\n3"
e2="Input:\n3\n1 1 1\n\nOutput:\n0"
e3="Input:\n3\n0 0 0\n\nOutput:\n-1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int firstOne(int[] arr) {
        // Write your code here — binary search for first 1
        return -1;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().firstOne(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{0,0,0,1,1,1},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,1,1},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,0,0},-1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{0,1},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0},-1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{0,0,0,0,0,1,1},5,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,0,1,1,1,1,1},2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,0,0,0,0,0,0,0,1},9,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{0,1,1,1},1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int firstOne(vector<int>& arr){return -1;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().firstOne(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({0,0,0,1,1,1},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,1,1},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,0,0},-1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({0,1},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0},-1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({0,0,0,0,0,1,1},5,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,0,1,1,1,1,1},2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,0,0,0,0,0,0,0,1},9,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({0,1,1,1},1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def firstOne(self, arr):
        return -1
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().firstOne(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([0,0,0,1,1,1],3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,1,1],0,2)
except:print("TC:2:FAIL:hidden")
try:test([0,0,0],-1,3)
except:print("TC:3:FAIL:hidden")
try:test([0,1],1,4)
except:print("TC:4:FAIL:hidden")
try:test([1],0,5)
except:print("TC:5:FAIL:hidden")
try:test([0],-1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,0,0,0,0,1,1],5,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,1,1,1,1,1],2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,0,0,0,0,0,0,0,1],9,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0,1,1,1],1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function firstOne(arr) { return -1; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=firstOne(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([0,0,0,1,1,1],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,1,1],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,0,0],-1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([0,1],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0],-1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,0,0,0,0,1,1],5,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,1,1,1,1,1],2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,0,0,0,0,0,0,0,1],9,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0,1,1,1],1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int firstOne(int* arr,int n) {
    // Write your code here
    return -1;
}
// USER_CODE_END

void runTest(int* a,int n,int e,int tc,int h){
    int g=firstOne(a,n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={0,0,0,1,1,1};runTest(t1,6,3,1,0);
    int t2[]={1,1,1};runTest(t2,3,0,2,0);
    int t3[]={0,0,0};runTest(t3,3,-1,3,0);
    int t4[]={0,1};runTest(t4,2,1,4,0);
    int t5[]={1};runTest(t5,1,0,5,0);
    int t6[]={0};runTest(t6,1,-1,6,1);
    int t7[]={0,0,0,0,0,1,1};runTest(t7,7,5,7,1);
    int t8[]={0,0,1,1,1,1,1};runTest(t8,7,2,8,1);
    int t9[]={0,0,0,0,0,0,0,0,0,1};runTest(t9,10,9,9,1);
    int t10[]={0,1,1,1};runTest(t10,4,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
