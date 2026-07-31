"""
Search X in Sorted Array (Binary Search)
===========================================
Given a sorted array arr of size n and a target X, find the index of X
using binary search. Return -1 if not found.

Examples:
  arr = [-1,0,3,5,9,12], X = 9 → 4
  arr = [-1,0,3,5,9,12], X = 2 → -1

Standard binary search: O(log n).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Search X in Sorted Array"
desc=(
    "Given a sorted array arr of size n and a target integer X, find the index "
    "of X in arr using binary search. If X is not present, return -1.\n\n"
    "For example:\n"
    "arr = [-1, 0, 3, 5, 9, 12], X = 9 → index 4\n"
    "arr = [-1, 0, 3, 5, 9, 12], X = 2 → -1 (not present)\n\n"
    "Binary search: compare the middle element with X. If equal, return its index. "
    "If X is smaller, search the left half. If larger, search the right half. "
    "Repeat until found or the range is empty."
)
infmt="First line contains n.\nSecond line contains n space-separated sorted integers.\nThird line contains X."
outfmt="Print the index of X, or -1."
cons="1 ≤ n ≤ 10^5\n-10^4 ≤ arr[i], X ≤ 10^4\narr is sorted ascending."
e1="Input:\n6\n-1 0 3 5 9 12\n9\n\nOutput:\n4"
e2="Input:\n6\n-1 0 3 5 9 12\n2\n\nOutput:\n-1"
e3="Input:\n1\n0\n0\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int binarySearch(int[] arr, int X) {
        // Write your code here — standard binary search
        return -1;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int x,int e,int tc,boolean h){int g=new CodeCoder().binarySearch(a,x);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":X="+x+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{-1,0,3,5,9,12},9,4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{-1,0,3,5,9,12},2,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0},0,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},5,4,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-5,-4,-3},-3,2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,3,5,7,9},7,3,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,1,1,1},1,0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},60,-1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-100,-50,0,50,100},-50,1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,4,6,8,10},8,3,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int binarySearch(vector<int>& arr,int X){return -1;}};
// USER_CODE_END
void test(vector<int> a,int x,int e,int tc,bool h=false){int g=CodeCoder().binarySearch(a,x);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({-1,0,3,5,9,12},9,4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({-1,0,3,5,9,12},2,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0},0,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},5,4,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-5,-4,-3},-3,2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,3,5,7,9},7,3,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,1,1,1},1,0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({10,20,30,40,50},60,-1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-100,-50,0,50,100},-50,1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,4,6,8,10},8,3,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def binarySearch(self, arr, X):
        return -1
# USER_CODE_END
def test(a,x,e,tc,h=False):g=CodeCoder().binarySearch(a,x);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:X={x}:exp={e}:got={g}"))
try:test([-1,0,3,5,9,12],9,4,1)
except:print("TC:1:FAIL:hidden")
try:test([-1,0,3,5,9,12],2,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([0],0,0,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5],5,4,4)
except:print("TC:4:FAIL:hidden")
try:test([-5,-4,-3],-3,2,5)
except:print("TC:5:FAIL:hidden")
try:test([1,3,5,7,9],7,3,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,1,1,1],1,0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,20,30,40,50],60,-1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-100,-50,0,50,100],-50,1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,4,6,8,10],8,3,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function binarySearch(arr, X) { return -1; }
// USER_CODE_END
function test(a,x,e,tc,h){if(h===undefined)h=false;const g=binarySearch(a,x);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([-1,0,3,5,9,12],9,4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([-1,0,3,5,9,12],2,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0],0,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],5,4,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-5,-4,-3],-3,2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,3,5,7,9],7,3,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,1,1,1],1,0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,20,30,40,50],60,-1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-100,-50,0,50,100],-50,1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,4,6,8,10],8,3,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int binarySearch(int* arr,int n,int X) {
    // Write your code here
    return -1;
}
// USER_CODE_END

void runTest(int* a,int n,int x,int e,int tc,int h){
    int g=binarySearch(a,n,x);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={-1,0,3,5,9,12};runTest(t1,6,9,4,1,0);
    int t2[]={-1,0,3,5,9,12};runTest(t2,6,2,-1,2,0);
    int t3[]={0};runTest(t3,1,0,0,3,0);
    int t4[]={1,2,3,4,5};runTest(t4,5,5,4,4,0);
    int t5[]={-5,-4,-3};runTest(t5,3,-3,2,5,0);
    int t6[]={1,3,5,7,9};runTest(t6,5,7,3,6,1);
    int t7[]={1,1,1,1};runTest(t7,4,1,0,7,1);
    int t8[]={10,20,30,40,50};runTest(t8,5,60,-1,8,1);
    int t9[]={-100,-50,0,50,100};runTest(t9,5,-50,1,9,1);
    int t10[]={2,4,6,8,10};runTest(t10,5,8,3,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
