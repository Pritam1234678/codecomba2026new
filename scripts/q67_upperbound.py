"""
Upper Bound
=============
Given a sorted array arr of size n and an integer X, find the upper bound of X
— the smallest index i such that arr[i] > X. If no element is greater than X,
return n.

Examples:
  arr = [1,2,8,10,11,12,19], X = 8 → index 3 (arr[3]=10 > 8)
  arr = [1,2,8,10,11,12,19], X = 19 → 7 (no element > 19)
  arr = [1,2,8,10,11,12,19], X = 5 → 2 (arr[2]=8 > 5)

Binary search variant.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Upper Bound"
desc=(
    "Given a sorted array arr of size n and an integer X, find the upper bound "
    "of X in the array.\n\n"
    "The upper bound is the smallest index i such that arr[i] > X (strictly greater). "
    "If no element is strictly greater than X, return n.\n\n"
    "For example:\n"
    "arr = [1,2,8,10,11,12,19], X = 8 → upper bound = 3 (arr[3] = 10 is the first element > 8)\n"
    "arr = [1,2,8,10,11,12,19], X = 19 → 7 (no element > 19)\n"
    "arr = [1,2,8,10,11,12,19], X = 5 → 2 (arr[2] = 8 > 5)\n\n"
    "Use binary search: if arr[mid] > X, move high to mid. Otherwise move low to mid+1."
)
infmt="First line contains n.\nSecond line contains n space-separated sorted integers.\nThird line contains X."
outfmt="Print the upper bound index."
cons="1 ≤ n ≤ 10^5\n-10^4 ≤ arr[i], X ≤ 10^4\narr is sorted ascending."
e1="Input:\n7\n1 2 8 10 11 12 19\n8\n\nOutput:\n3"
e2="Input:\n7\n1 2 8 10 11 12 19\n19\n\nOutput:\n7"
e3="Input:\n7\n1 2 8 10 11 12 19\n5\n\nOutput:\n2"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int upperBound(int[] arr, int X) {
        // Write your code here — binary search, first index with arr[i] > X
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int x,int e,int tc,boolean h){int g=new CodeCoder().upperBound(a,x);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":X="+x+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,8,10,11,12,19},8,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,8,10,11,12,19},19,7,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,8,10,11,12,19},5,2,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,3,5,7},6,3,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,3,5,7},0,0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1},1,4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-5,-3,0,2,4},0,3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{2,4,6,8},10,4,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5},5,1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,3,5,7},3,2,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int upperBound(vector<int>& arr,int X){return 0;}};
// USER_CODE_END
void test(vector<int> a,int x,int e,int tc,bool h=false){int g=CodeCoder().upperBound(a,x);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,8,10,11,12,19},8,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,8,10,11,12,19},19,7,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,8,10,11,12,19},5,2,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,3,5,7},6,3,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,3,5,7},0,0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1},1,4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-5,-3,0,2,4},0,3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({2,4,6,8},10,4,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5},5,1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,3,5,7},3,2,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def upperBound(self, arr, X):
        return 0
# USER_CODE_END
def test(a,x,e,tc,h=False):g=CodeCoder().upperBound(a,x);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:X={x}:exp={e}:got={g}"))
try:test([1,2,8,10,11,12,19],8,3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,8,10,11,12,19],19,7,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,8,10,11,12,19],5,2,3)
except:print("TC:3:FAIL:hidden")
try:test([1,3,5,7],6,3,4)
except:print("TC:4:FAIL:hidden")
try:test([1,3,5,7],0,0,5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1],1,4,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-5,-3,0,2,4],0,3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([2,4,6,8],10,4,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5],5,1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,3,5,7],3,2,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function upperBound(arr, X) { return 0; }
// USER_CODE_END
function test(a,x,e,tc,h){if(h===undefined)h=false;const g=upperBound(a,x);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,2,8,10,11,12,19],8,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,8,10,11,12,19],19,7,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,8,10,11,12,19],5,2,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,3,5,7],6,3,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,3,5,7],0,0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1],1,4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-5,-3,0,2,4],0,3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([2,4,6,8],10,4,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5],5,1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,3,5,7],3,2,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int upperBound(int* arr,int n,int X) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int x,int e,int tc,int h){
    int g=upperBound(a,n,x);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={1,2,8,10,11,12,19};runTest(t1,7,8,3,1,0);
    int t2[]={1,2,8,10,11,12,19};runTest(t2,7,19,7,2,0);
    int t3[]={1,2,8,10,11,12,19};runTest(t3,7,5,2,3,0);
    int t4[]={1,3,5,7};runTest(t4,4,6,3,4,0);
    int t5[]={1,3,5,7};runTest(t5,4,0,0,5,0);
    int t6[]={1,1,1,1};runTest(t6,4,1,4,6,1);
    int t7[]={-5,-3,0,2,4};runTest(t7,5,0,3,7,1);
    int t8[]={2,4,6,8};runTest(t8,4,10,4,8,1);
    int t9[]={5};runTest(t9,1,5,1,9,1);
    int t10[]={1,3,5,7};runTest(t10,4,3,2,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
