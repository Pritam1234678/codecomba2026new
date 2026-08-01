"""
Sort Array By Parity II
=========================
Given an array nums of even length, half of the elements are even and half are
odd. Rearrange the array so that nums[i] is even when i is even, and odd when
i is odd. Return any valid arrangement.

Examples:
  nums = [4,2,5,7] -> [4,5,2,7]  (or any valid arrangement)
  nums = [2,3,1,0] -> [2,3,0,1]

Two-pointer approach: maintain an even pointer (0) and an odd pointer (1). If
nums[even] is odd and nums[odd] is even, swap them; advance each pointer past
correctly-placed positions.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the result is returned via int* returnSize: int* sortParity(int* nums, int n, int* rs).)
The harness checks the parity condition rather than one fixed arrangement.
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sort Array By Parity II"
desc=(
    "Given an array nums of even length n where exactly half the elements are "
    "even and half are odd, rearrange the array so that nums[i] is EVEN when i "
    "is even, and ODD when i is odd. Return any valid arrangement.\n\n"
    "For example:\n"
    "nums = [4,2,5,7] -> [4,5,2,7]  (multiple valid answers exist)\n"
    "nums = [2,3,1,0] -> [2,3,0,1]\n\n"
    "Two-pointer approach: keep an even pointer starting at 0 and an odd "
    "pointer starting at 1. If nums[evenPtr] is odd while nums[oddPtr] is "
    "even, swap them. Advance the even pointer by 2 whenever its position is "
    "correct, and the odd pointer by 2 likewise. O(n) time, O(1) extra space."
)
infmt="First line contains n (even). Second line contains n space-separated integers (n/2 even and n/2 odd)."
outfmt="Print any rearrangement where even positions hold even numbers and odd positions hold odd numbers."
cons="2 ≤ n ≤ 10^4, n is even\n0 ≤ nums[i] ≤ 1000\nExactly n/2 elements are even and n/2 are odd."
e1="Input:\n4\n4 2 5 7\n\nOutput:\n4 5 2 7"
e2="Input:\n4\n2 3 1 0\n\nOutput:\n2 3 0 1"
e3="Input:\n2\n1 0\n\nOutput:\n0 1"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Sorting, Two Pointers",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] sortParity(int[] nums) {
        // Write your code here — even at even indices, odd at odd indices
        return nums;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int tc,boolean hd){int[] r=new CodeCoder().sortParity(a.clone());boolean ok=true;for(int i=0;i<r.length;i++){if((i%2==0)!=(r[i]%2==0)){ok=false;break;}}if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":got="+Arrays.toString(r));}
public static void main(String[] a){
try{test(new int[]{4,2,5,7},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,3,1,0},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,0},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{0,1,2,3},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{3,4,1,2,7,0},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{5,6,5,6,5,6,5,6},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{0,0,1,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{2,4,6,1,3,5},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,1,1,2,2,2},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{9,7,5,3,1,2,4,6,8,10},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> sortParity(vector<int>& nums){return nums;}};
// USER_CODE_END
 void test(vector<int> a,int tc,bool hd=false){vector<int> r=CodeCoder().sortParity(a);bool ok=true;for(int i=0;i<(int)r.size();i++){if((i%2==0)!=(r[i]%2==0)){ok=false;break;}}if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:got=[";for(int i=0;i<(int)r.size();i++){if(i)cout<<",";cout<<r[i];}cout<<"]\\n";}}
int main(){
try{test({4,2,5,7},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,3,1,0},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,0},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({0,1,2,3},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({3,4,1,2,7,0},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({5,6,5,6,5,6,5,6},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({0,0,1,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({2,4,6,1,3,5},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,1,1,2,2,2},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({9,7,5,3,1,2,4,6,8,10},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sortParity(self, nums):
        return nums
# USER_CODE_END
def test(a,tc,hd=False):
    r=CodeCoder().sortParity(list(a));ok=all((i%2==0)==(v%2==0) for i,v in enumerate(r))
    print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:arr={a}:got={r}"))
try:test([4,2,5,7],1)
except:print("TC:1:FAIL:hidden")
try:test([2,3,1,0],2)
except:print("TC:2:FAIL:hidden")
try:test([1,0],3)
except:print("TC:3:FAIL:hidden")
try:test([0,1,2,3],4)
except:print("TC:4:FAIL:hidden")
try:test([3,4,1,2,7,0],5)
except:print("TC:5:FAIL:hidden")
try:test([5,6,5,6,5,6,5,6],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,0,1,1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([2,4,6,1,3,5],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,1,1,2,2,2],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([9,7,5,3,1,2,4,6,8,10],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sortParity(nums) { return nums; }
// USER_CODE_END
function test(a,tc,hd){if(hd===undefined)hd=false;const r=sortParity(a.slice());let ok=r.every((v,i)=>(i%2===0)===(v%2===0));if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":got="+JSON.stringify(r));}
try{test([4,2,5,7],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,3,1,0],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,0],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([0,1,2,3],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([3,4,1,2,7,0],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([5,6,5,6,5,6,5,6],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,0,1,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([2,4,6,1,3,5],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,1,1,2,2,2],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([9,7,5,3,1,2,4,6,8,10],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* sortParity(int* nums,int n,int* rs) {
    // Write your code here — even at even indices, odd at odd indices
    *rs = 0; return NULL;
}
// USER_CODE_END

void runTest(int* a,int n,int tc,int hd){
    int rs=0;int* r=sortParity(a,n,&rs);
    int ok=(rs==n);
    if(ok)for(int i=0;i<n;i++){if((i%2==0)!=(r[i]%2==0)){ok=0;break;}}
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:got=[");for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",r[i]);}printf("]\\n");}
    free(r);
}
int main(){
    int t1[]={4,2,5,7};runTest(t1,4,1,0);
    int t2[]={2,3,1,0};runTest(t2,4,2,0);
    int t3[]={1,0};runTest(t3,2,3,0);
    int t4[]={0,1,2,3};runTest(t4,4,4,0);
    int t5[]={3,4,1,2,7,0};runTest(t5,6,5,0);
    int t6[]={5,6,5,6,5,6,5,6};runTest(t6,8,6,1);
    int t7[]={0,0,1,1};runTest(t7,4,7,1);
    int t8[]={2,4,6,1,3,5};runTest(t8,6,8,1);
    int t9[]={1,1,1,2,2,2};runTest(t9,6,9,1);
    int t10[]={9,7,5,3,1,2,4,6,8,10};runTest(t10,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
