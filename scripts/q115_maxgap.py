"""
Maximum Gap
=============
Given an integer array nums, return the maximum difference between two
successive elements in its sorted form. If the array has fewer than 2
elements, return 0. Solve it in O(n) time and O(n) extra space.

Examples:
  nums = [3,6,9,1]  -> 3  (sorted [1,3,6,9], gaps 2,3,3)
  nums = [1,1,1,1]  -> 0

Bucket (Pigeonhole) approach: with n elements and a range of max-min, there
are n-1 gaps total, so the answer is at least ceil((max-min)/(n-1)). Put each
element into a bucket of that size; the maximum gap can only occur between
consecutive NON-empty buckets (compare the previous bucket's max with the
current bucket's min).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the array is passed as int* nums with its length n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Maximum Gap"
desc=(
    "Given an integer array nums, return the maximum difference between two "
    "successive elements in the SORTED form of the array. If the array has "
    "fewer than 2 elements, return 0. You should aim for O(n) time and O(n) "
    "extra space.\n\n"
    "For example:\n"
    "nums = [3,6,9,1] -> 3   (sorted [1,3,6,9], successive gaps 2,3,3)\n"
    "nums = [10]      -> 0   (fewer than 2 elements)\n\n"
    "Bucket approach: with n elements the sorted gaps are n-1 in number, so "
    "the answer is at least ceil((max-min)/(n-1)). Create buckets of exactly "
    "that size and drop each element into its bucket (tracking each bucket's "
    "min and max). The maximum gap can then only occur between consecutive "
    "NON-empty buckets, by comparing the previous bucket's max with the "
    "current bucket's min."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the maximum gap between successive elements in sorted order (0 if n < 2)."
cons="1 ≤ n ≤ 10^5\n0 ≤ nums[i] ≤ 10^9"
e1="Input:\n4\n3 6 9 1\n\nOutput:\n3"
e2="Input:\n4\n1 1 1 1\n\nOutput:\n0"
e3="Input:\n1\n10\n\nOutput:\n0"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Sorting, Bucket Sort",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int maximumGap(int[] nums) {
        // Write your code here — O(n) bucket approach
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean hd){int r=new CodeCoder().maximumGap(a.clone());if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{3,6,9,1},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,1,1},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,3,100},97,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,10000000},9999999,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{3,6,9,1,15,2,20},6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,10,20,30,100,200},100,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,4,6,8,10,12,14,16,18,20},2,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int maximumGap(vector<int>& nums){return 0;}};
// USER_CODE_END
 void test(vector<int> a,int e,int tc,bool hd=false){int r=CodeCoder().maximumGap(a);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp="<<e<<":got="<<r<<"\\n";}}
int main(){
try{test({3,6,9,1},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,1,1},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,3,100},97,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,10000000},9999999,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({3,6,9,1,15,2,20},6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({5,4,3,2,1},1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,10,20,30,100,200},100,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,4,6,8,10,12,14,16,18,20},2,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maximumGap(self, nums):
        return 0
# USER_CODE_END
def test(a,e,tc,hd=False):r=CodeCoder().maximumGap(list(a));print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={r}"))
try:test([3,6,9,1],3,1)
except:print("TC:1:FAIL:hidden")
try:test([10],0,2)
except:print("TC:2:FAIL:hidden")
try:test([1,1,1,1],0,3)
except:print("TC:3:FAIL:hidden")
try:test([1,3,100],97,4)
except:print("TC:4:FAIL:hidden")
try:test([1,10000000],9999999,5)
except:print("TC:5:FAIL:hidden")
try:test([3,6,9,1,15,2,20],6,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5],1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([5,4,3,2,1],1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,10,20,30,100,200],100,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,4,6,8,10,12,14,16,18,20],2,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maximumGap(nums) { return 0; }
// USER_CODE_END
function test(a,e,tc,hd){if(hd===undefined)hd=false;const r=maximumGap(a.slice());if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+e+":got="+r);}
try{test([3,6,9,1],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,1,1],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,3,100],97,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,10000000],9999999,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([3,6,9,1,15,2,20],6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5],1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([5,4,3,2,1],1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,10,20,30,100,200],100,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,4,6,8,10,12,14,16,18,20],2,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int maximumGap(int* nums,int n) {
    // Write your code here — O(n) bucket approach, 0 if n < 2
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int e,int tc,int hd){
    int r=maximumGap(a,n);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:n=%d:arr=[",tc,n);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:exp=%d:got=%d\\n",e,r);}
}
int main(){
    int t1[]={3,6,9,1};runTest(t1,4,3,1,0);
    int t2[]={10};runTest(t2,1,0,2,0);
    int t3[]={1,1,1,1};runTest(t3,4,0,3,0);
    int t4[]={1,3,100};runTest(t4,3,97,4,0);
    int t5[]={1,10000000};runTest(t5,2,9999999,5,0);
    int t6[]={3,6,9,1,15,2,20};runTest(t6,7,6,6,1);
    int t7[]={1,2,3,4,5};runTest(t7,5,1,7,1);
    int t8[]={5,4,3,2,1};runTest(t8,5,1,8,1);
    int t9[]={1,10,20,30,100,200};runTest(t9,6,100,9,1);
    int t10[]={2,4,6,8,10,12,14,16,18,20};runTest(t10,10,2,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
