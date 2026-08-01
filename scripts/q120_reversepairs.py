"""
Reverse Pairs (Merge Sort)
============================
Given an integer array nums, return the number of reverse pairs. A reverse
pair is a pair of indices (i, j) with i < j and nums[i] > 2 * nums[j].
Use an O(n log n) merge-sort based algorithm.

Examples:
  nums = [1,3,2,3,1] -> 2   (pairs (3,1) and (3,1) with the two 3's)
  nums = [2,4,3,5,1] -> 3

Merge-sort approach: after the two halves are sorted, for each element in the
left half count how many elements in the right half are less than it / 2,
then merge normally. The counting step scans each element a constant number
of times per level, so total is O(n log n).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the array is passed as int* nums with length n; return int.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reverse Pairs"
desc=(
    "Given an integer array nums, return the number of REVERSE PAIRS in it. A "
    "reverse pair is a pair of indices (i, j) with i < j and "
    "nums[i] > 2 * nums[j].\n\n"
    "For example:\n"
    "nums = [1,3,2,3,1] -> 2   (both 3's form a pair with the last 1)\n"
    "nums = [2,4,3,5,1] -> 3\n\n"
    "Use the merge-sort based technique in O(n log n): after both halves of a "
    "range are sorted, for each element x in the left half count how many "
    "elements in the right half are smaller than x / 2 (use a 64-bit division "
    "guard so x > 2*y is checked without overflow), add that to the answer, "
    "and then perform a normal merge of the two halves."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the number of reverse pairs (i<j and nums[i] > 2*nums[j])."
cons="1 ≤ n ≤ 5*10^4\n-2^31 ≤ nums[i] ≤ 2^31 - 1\nUse long/64-bit when comparing nums[i] > 2*nums[j]."
e1="Input:\n5\n1 3 2 3 1\n\nOutput:\n2"
e2="Input:\n5\n2 4 3 5 1\n\nOutput:\n3"
e3="Input:\n5\n1 1 1 1 1\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Sorting, Merge Sort, Divide and Conquer",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int reversePairs(int[] nums) {
        // Write your code here — merge sort based count
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean hd){int g=new CodeCoder().reversePairs(a.clone());if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test(new int[]{1,3,2,3,1},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,4,3,5,1},3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{6,5,4,3,2,1},6,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2,4,5,3,1},3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{8,4,2,1},3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{5,5,5,5},0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{2147483647,2147483647,2147483647},0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,1,9,2,8,3,7,4},8,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int reversePairs(vector<int>& nums){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool hd=false){int g=CodeCoder().reversePairs(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,3,2,3,1},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,4,3,5,1},3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,1,1,1},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({6,5,4,3,2,1},6,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2,4,5,3,1},3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({8,4,2,1},3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({5,5,5,5},0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({2147483647,2147483647,2147483647},0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,1,9,2,8,3,7,4},8,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def reversePairs(self, nums):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().reversePairs(list(a));ok=(g==e);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,3,2,3,1],2,1)
except:print("TC:1:FAIL:hidden")
try:test([2,4,3,5,1],3,2)
except:print("TC:2:FAIL:hidden")
try:test([1,1,1,1,1],0,3)
except:print("TC:3:FAIL:hidden")
try:test([6,5,4,3,2,1],6,4)
except:print("TC:4:FAIL:hidden")
try:test([2,4,5,3,1],3,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],0,6,True)
except:print("TC:6:FAIL:hidden")
try:test([8,4,2,1],3,7,True)
except:print("TC:7:FAIL:hidden")
try:test([5,5,5,5],0,8,True)
except:print("TC:8:FAIL:hidden")
try:test([2147483647,2147483647,2147483647],0,9,True)
except:print("TC:9:FAIL:hidden")
try:test([10,1,9,2,8,3,7,4],8,10,True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function reversePairs(nums) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=reversePairs(a.slice());if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+e+":got="+g);}
try{test([1,3,2,3,1],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,4,3,5,1],3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,1,1,1],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([6,5,4,3,2,1],6,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2,4,5,3,1],3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([8,4,2,1],3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([5,5,5,5],0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([2147483647,2147483647,2147483647],0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,1,9,2,8,3,7,4],8,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int reversePairs(int* nums,int n) {
    // Write your code here — merge sort based count
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int e,int tc,int hd){
    int g=reversePairs(a,n);
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={1,3,2,3,1};runTest(t1,5,2,1,0);
    int t2[]={2,4,3,5,1};runTest(t2,5,3,2,0);
    int t3[]={1,1,1,1,1};runTest(t3,5,0,3,0);
    int t4[]={6,5,4,3,2,1};runTest(t4,6,6,4,0);
    int t5[]={2,4,5,3,1};runTest(t5,5,3,5,0);
    int t6[]={1,2,3,4,5};runTest(t6,5,0,6,1);
    int t7[]={8,4,2,1};runTest(t7,4,3,7,1);
    int t8[]={5,5,5,5};runTest(t8,4,0,8,1);
    int t9[]={2147483647,2147483647,2147483647};runTest(t9,3,0,9,1);
    int t10[]={10,1,9,2,8,3,7,4};runTest(t10,8,8,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
