"""
Find Peak Element
==================
A peak element is an element that is strictly greater than its neighbors.
Given a 0-indexed array nums, return the index of ANY peak element.
Imagine nums[-1] = nums[n] = -infinity, so endpoints can be peaks.
Must run in O(log n) using binary search.

Examples:
  nums = [1,2,3,1]          -> 2  (value 3)
  nums = [1,2,1,3,5,6,4]    -> 1 or 5  (values 2 or 6)
  nums = [1]                -> 0

Binary search: if nums[mid] < nums[mid+1], a peak is to the right (mid+1..);
otherwise a peak is at mid or to the left.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
NOTE: the harness validates that the returned index is a real peak
(strictly greater than both neighbors), so any valid peak answer passes.
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find Peak Element"
desc=(
    "A peak element is an element that is strictly greater than its neighbors. "
    "Given a 0-indexed integer array nums, return the index of ANY peak "
    "element.\n\n"
    "For edge elements, imagine nums[-1] = nums[n] = -infinity, so an endpoint "
    "can be a peak if it is greater than its single neighbor. There may be "
    "multiple peaks in the array — returning the index of any one of them is "
    "correct.\n\n"
    "For example:\n"
    "nums = [1,2,3,1]        -> 2 (value 3)\n"
    "nums = [1,2,1,3,5,6,4]  -> 1 or 5 (values 2 or 6)\n"
    "nums = [1]              -> 0\n\n"
    "Use binary search in O(log n): if nums[mid] < nums[mid+1], a peak must lie "
    "to the right of mid; otherwise a peak is at mid or to its left. Return the "
    "index, not the value."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the index of any peak element."
cons="1 ≤ n ≤ 1000\n-2^31 ≤ nums[i] ≤ 2^31-1\nnums[i] != nums[i+1] for all valid i (adjacent elements are distinct)."
e1="Input:\n4\n1 2 3 1\n\nOutput:\n2"
e2="Input:\n7\n1 2 1 3 5 6 4\n\nOutput:\n5"
e3="Input:\n1\n1\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int findPeakElement(int[] nums) {
        // Write your code here — binary search for any peak index
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int tc,boolean h){int idx=new CodeCoder().findPeakElement(a);int n=a.length;boolean ok=(idx>=0&&idx<n&&(idx==0||a[idx]>a[idx-1])&&(idx==n-1||a[idx]>a[idx+1]));if(ok)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":idx="+idx);}
public static void main(String[] a){
try{test(new int[]{1,2,3,1},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,1,3,5,6,4},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,3,2,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,1,2,3,1},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int findPeakElement(vector<int>& nums){return 0;}};
// USER_CODE_END
void test(vector<int> a,int tc,bool h=false){int idx=CodeCoder().findPeakElement(a);int n=a.size();bool ok=(idx>=0&&idx<n&&(idx==0||a[idx]>a[idx-1])&&(idx==n-1||a[idx]>a[idx+1]));if(ok)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:idx="<<idx<<"\\n";}
int main(){
try{test({1,2,3,1},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,1,3,5,6,4},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,3,2,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3,4,5},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,4,3,2,1},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,1,2,3,1},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findPeakElement(self, nums):
        return 0
# USER_CODE_END
def test(a,tc,h=False):
    idx=CodeCoder().findPeakElement(a);n=len(a)
    ok=0<=idx<n and (idx==0 or a[idx]>a[idx-1]) and (idx==n-1 or a[idx]>a[idx+1])
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:idx={idx}:arr={a}"))
try:test([1,2,3,1],1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,1,3,5,6,4],2)
except:print("TC:2:FAIL:hidden")
try:test([1],3)
except:print("TC:3:FAIL:hidden")
try:test([5],4)
except:print("TC:4:FAIL:hidden")
try:test([1,2],5)
except:print("TC:5:FAIL:hidden")
try:test([1,3,2,1],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([2,1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,5],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,4,3,2,1],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,1,2,3,1],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findPeakElement(nums) { return 0; }
// USER_CODE_END
function test(a,tc,h){if(h===undefined)h=false;const idx=findPeakElement(a);const n=a.length;const ok=(idx>=0&&idx<n&&(idx===0||a[idx]>a[idx-1])&&(idx===n-1||a[idx]>a[idx+1]));if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:idx="+idx);}
try{test([1,2,3,1],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,1,3,5,6,4],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,3,2,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,5],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,4,3,2,1],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,1,2,3,1],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int findPeakElement(int* arr,int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int tc,int h){
    int idx=findPeakElement(a,n);
    int ok=(idx>=0&&idx<n&&(idx==0||a[idx]>a[idx-1])&&(idx==n-1||a[idx]>a[idx+1]));
    if(ok){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:idx=%d\\n",tc,idx);}
}
int main(){
    int t1[]={1,2,3,1};runTest(t1,4,1,0);
    int t2[]={1,2,1,3,5,6,4};runTest(t2,7,2,0);
    int t3[]={1};runTest(t3,1,3,0);
    int t4[]={5};runTest(t4,1,4,0);
    int t5[]={1,2};runTest(t5,2,5,0);
    int t6[]={1,3,2,1};runTest(t6,4,6,1);
    int t7[]={2,1};runTest(t7,2,7,1);
    int t8[]={1,2,3,4,5};runTest(t8,5,8,1);
    int t9[]={5,4,3,2,1};runTest(t9,5,9,1);
    int t10[]={1,2,3,1,2,3,1};runTest(t10,7,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
