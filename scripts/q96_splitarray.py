"""
Split Array Largest Sum
========================
Given an integer array nums and an integer k, split nums into k non-empty
contiguous subarrays such that the LARGEST sum of any subarray is minimized.
Return that minimized largest sum.

Examples:
  nums = [7,2,5,10,8], k = 2 -> 18   (split [7,2,5] and [10,8])
  nums = [1,2,3,4,5], k = 2 -> 9     (split [1,2,3,4] and [5])

Binary search the answer in [max(nums), sum(nums)]. For a candidate cap,
greedily count the minimum number of contiguous groups so no group exceeds cap
(accumulate while cur + nums[i] <= cap, else start a new group). If the group
count <= k, cap is feasible (try smaller); else we need a larger cap.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the nums array is passed with its length n: int* nums, int n, int k.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Split Array Largest Sum"
desc=(
    "Given an integer array nums and an integer k, split nums into k non-empty "
    "contiguous subarrays such that the LARGEST sum of any subarray is "
    "minimized. Return the minimized largest sum.\n\n"
    "For example:\n"
    "nums = [7,2,5,10,8], k = 2 -> 18   (split into [7,2,5] and [10,8])\n"
    "nums = [1,2,3,4,5], k = 2 -> 9     (split into [1,2,3,4] and [5])\n\n"
    "Binary search the answer in [max(nums), sum(nums)]. For a candidate "
    "capacity cap, greedily count the minimum number of contiguous groups "
    "needed so that no group's sum exceeds cap. If that count <= k, cap is "
    "feasible (try a smaller one); otherwise we need a larger cap. Runs in "
    "O(n * log(sum(nums)))."
)
infmt="First line contains n and k. Second line contains n space-separated integers."
outfmt="Print the minimized largest sum of any subarray."
cons="1 ≤ n ≤ 10^4\n1 ≤ k ≤ n\n1 ≤ nums[i] ≤ 10^4"
e1="Input:\n5 2\n7 2 5 10 8\n\nOutput:\n18"
e2="Input:\n5 2\n1 2 3 4 5\n\nOutput:\n9"
e3="Input:\n3 3\n1 4 4\n\nOutput:\n4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int splitArray(int[] nums, int k) {
        // Write your code here — binary search the minimized largest sum
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] n,int k,int e,int tc,boolean hd){int r=new CodeCoder().splitArray(n,k);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:nums="+Arrays.toString(n)+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{7,2,5,10,8},2,18,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},2,9,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,4,4},3,4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{2,3,1,2,4,3},5,4,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{5,5,5,5,5},2,15,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},5,15,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40},2,60,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1,1,1,1,1,1},3,4,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{3,3,3,3,3,3,3,3},4,6,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,15,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int splitArray(vector<int>& nums,int k){return 0;}};
// USER_CODE_END
void test(vector<int> n,int k,int e,int tc,bool hd=false){int r=CodeCoder().splitArray(n,k);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({7,2,5,10,8},2,18,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},2,9,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,4,4},3,4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({2,3,1,2,4,3},5,4,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({5,5,5,5,5},2,15,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},5,15,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40},2,60,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,1,1,1,1,1,1,1,1,1},3,4,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({3,3,3,3,3,3,3,3},4,6,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,15,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def splitArray(self, nums, k):
        return 0
# USER_CODE_END
def test(n,k,e,tc,hd=False):r=CodeCoder().splitArray(n,k);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:nums={n}:k={k}:exp={e}:got={r}"))
try:test([7,2,5,10,8],2,18,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4,5],2,9,2)
except:print("TC:2:FAIL:hidden")
try:test([1,4,4],3,4,3)
except:print("TC:3:FAIL:hidden")
try:test([2,3,1,2,4,3],5,4,4)
except:print("TC:4:FAIL:hidden")
try:test([5,5,5,5,5],2,15,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],5,15,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,20,30,40],2,60,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,1,1,1,1,1,1,1,1,1],3,4,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([3,3,3,3,3,3,3,3],4,6,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5],1,15,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function splitArray(nums, k) { return 0; }
// USER_CODE_END
function test(n,k,e,tc,hd){if(hd===undefined)hd=false;const r=splitArray(n,k);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([7,2,5,10,8],2,18,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],2,9,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,4,4],3,4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([2,3,1,2,4,3],5,4,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([5,5,5,5,5],2,15,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],5,15,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40],2,60,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,1,1,1,1,1,1,1,1,1],3,4,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([3,3,3,3,3,3,3,3],4,6,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5],1,15,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int splitArray(int* nums,int n,int k) {
    // Write your code here — return the minimized largest sum
    return 0;
}
// USER_CODE_END

void runTest(int* n,int len,int k,int e,int tc,int hd){
    int r=splitArray(n,len,k);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={7,2,5,10,8};runTest(t1,5,2,18,1,0);
    int t2[]={1,2,3,4,5};runTest(t2,5,2,9,2,0);
    int t3[]={1,4,4};runTest(t3,3,3,4,3,0);
    int t4[]={2,3,1,2,4,3};runTest(t4,6,5,4,4,0);
    int t5[]={5,5,5,5,5};runTest(t5,5,2,15,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9,10};runTest(t6,10,5,15,6,1);
    int t7[]={10,20,30,40};runTest(t7,4,2,60,7,1);
    int t8[]={1,1,1,1,1,1,1,1,1,1};runTest(t8,10,3,4,8,1);
    int t9[]={3,3,3,3,3,3,3,3};runTest(t9,8,4,6,9,1);
    int t10[]={1,2,3,4,5};runTest(t10,5,1,15,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
