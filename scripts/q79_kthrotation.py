"""
Find kth Rotation
=================
Given a sorted array arr (strictly increasing) that has been right-rotated k
times, find k — the number of rotations. k equals the index of the minimum
element (the rotation point).

Examples:
  arr = [15, 18, 2, 3, 6, 12] -> 2
  arr = [7, 9, 11, 12, 5]     -> 4
  arr = [7, 9, 11, 12, 15]    -> 0  (already sorted)

Binary search: if arr[low] <= arr[high], the range is sorted so min is at low.
Otherwise compare arr[mid] with arr[high]: if arr[mid] > arr[high] the minimum
is in the right half, else in the left half. Runs in O(log n).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find kth Rotation"
desc=(
    "Given a sorted array arr (strictly increasing order) that has been "
    "right-rotated k times, find the value of k — the number of rotations.\n\n"
    "A right rotation moves the last element to the front and shifts the rest "
    "one step to the right. Equivalently, k is the index of the minimum "
    "element in the rotated array.\n\n"
    "For example:\n"
    "arr = [15, 18, 2, 3, 6, 12] -> 2\n"
    "arr = [7, 9, 11, 12, 5]     -> 4\n"
    "arr = [7, 9, 11, 12, 15]    -> 0 (already sorted, no rotation)\n\n"
    "Use binary search: if the current range is already sorted (arr[low] <= "
    "arr[high]), the minimum is at low. Otherwise, if arr[mid] > arr[high] the "
    "minimum lies in the right half; else it lies in the left half (including "
    "mid). This runs in O(log n)."
)
infmt="First line contains n.\nSecond line contains n space-separated rotated sorted integers."
outfmt="Print k — the number of right rotations (the index of the minimum element)."
cons="1 ≤ n ≤ 5000\n-5000 ≤ arr[i] ≤ 5000\nArray is strictly increasing, then right-rotated k times."
e1="Input:\n6\n15 18 2 3 6 12\n\nOutput:\n2"
e2="Input:\n5\n7 9 11 12 5\n\nOutput:\n4"
e3="Input:\n5\n7 9 11 12 15\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int findKRotation(int[] arr) {
        // Write your code here — binary search for index of minimum
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().findKRotation(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{15,18,2,3,6,12},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{7,9,11,12,5},4,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{7,9,11,12,15},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{2,1},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{5,6,7,8,9,1,2,3},5,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{3,4,5,1,2},3,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{6,7,8,9,10,1,2,3,4,5},5,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int findKRotation(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().findKRotation(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({15,18,2,3,6,12},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({7,9,11,12,5},4,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({7,9,11,12,15},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({2,1},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({5,6,7,8,9,1,2,3},5,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40,50},0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({3,4,5,1,2},3,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({6,7,8,9,10,1,2,3,4,5},5,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,4,5},0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findKRotation(self, arr):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().findKRotation(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([15,18,2,3,6,12],2,1)
except:print("TC:1:FAIL:hidden")
try:test([7,9,11,12,5],4,2)
except:print("TC:2:FAIL:hidden")
try:test([7,9,11,12,15],0,3)
except:print("TC:3:FAIL:hidden")
try:test([2,1],1,4)
except:print("TC:4:FAIL:hidden")
try:test([5,6,7,8,9,1,2,3],5,5)
except:print("TC:5:FAIL:hidden")
try:test([1],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,20,30,40,50],0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([3,4,5,1,2],3,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([6,7,8,9,10,1,2,3,4,5],5,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5],0,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findKRotation(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=findKRotation(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([15,18,2,3,6,12],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([7,9,11,12,5],4,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([7,9,11,12,15],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([2,1],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([5,6,7,8,9,1,2,3],5,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40,50],0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([3,4,5,1,2],3,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([6,7,8,9,10,1,2,3,4,5],5,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5],0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int findKRotation(int* arr,int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int e,int tc,int h){
    int g=findKRotation(a,n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={15,18,2,3,6,12};runTest(t1,6,2,1,0);
    int t2[]={7,9,11,12,5};runTest(t2,5,4,2,0);
    int t3[]={7,9,11,12,15};runTest(t3,5,0,3,0);
    int t4[]={2,1};runTest(t4,2,1,4,0);
    int t5[]={5,6,7,8,9,1,2,3};runTest(t5,8,5,5,0);
    int t6[]={1};runTest(t6,1,0,6,1);
    int t7[]={10,20,30,40,50};runTest(t7,5,0,7,1);
    int t8[]={3,4,5,1,2};runTest(t8,5,3,8,1);
    int t9[]={6,7,8,9,10,1,2,3,4,5};runTest(t9,10,5,9,1);
    int t10[]={1,2,3,4,5};runTest(t10,5,0,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
