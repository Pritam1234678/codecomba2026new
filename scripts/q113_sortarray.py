"""
Sort An Array (Implement Merge / Quick Sort)
==============================================
Given an integer array nums, return the array sorted in ascending order. You
must implement the sort yourself using Merge Sort or Quick Sort (no built-in
sort function).

Examples:
  nums = [5,2,3,1]    -> [1,2,3,5]
  nums = [5,1,1,2,0,0] -> [0,0,1,1,2,5]

Both merge sort and quicksort achieve O(n log n) average time.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the result is returned via int* returnSize: int* sortArray(int* nums, int n, int* rs).)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sort An Array"
desc=(
    "Given an integer array nums, return the array sorted in ascending order. "
    "You MUST implement the sorting yourself using Merge Sort or Quick Sort — "
    "calling a built-in sort function is not allowed.\n\n"
    "For example:\n"
    "nums = [5,2,3,1]     -> [1,2,3,5]\n"
    "nums = [5,1,1,2,0,0] -> [0,0,1,1,2,5]\n\n"
    "Merge sort: split the array in half, sort each half recursively, then "
    "merge the two sorted halves. Quick sort: pick a pivot, partition so "
    "smaller elements go left and larger go right, then recurse on both sides. "
    "Both run in O(n log n) average time and O(n) or O(log n) extra space."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the sorted array in ascending order (space-separated)."
cons="1 ≤ n ≤ 5*10^4\n-10^5 ≤ nums[i] ≤ 10^5"
e1="Input:\n4\n5 2 3 1\n\nOutput:\n1 2 3 5"
e2="Input:\n6\n5 1 1 2 0 0\n\nOutput:\n0 0 1 1 2 5"
e3="Input:\n1\n7\n\nOutput:\n7"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"EASY",True,"Array, Sorting, Merge Sort, Quick Sort",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] sortArray(int[] nums) {
        // Write your code here — merge/quick sort, return sorted array
        return nums;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean hd){int[] r=new CodeCoder().sortArray(a.clone());boolean ok=Arrays.equals(r,e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+a.length+":arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(r));}
public static void main(String[] a){
try{test(new int[]{5,2,3,1},new int[]{1,2,3,5},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5,1,1,2,0,0},new int[]{0,0,1,1,2,5},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{3,2,1},new int[]{1,2,3},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-2,3,0,-5,4},new int[]{-5,-2,0,3,4},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{9,8,7,6,5,4,3,2,1},new int[]{1,2,3,4,5,6,7,8,9},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,1,1,1},new int[]{1,1,1,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{10,10,5,10,5},new int[]{5,5,10,10,10},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-100,100,0,-50,50},new int[]{-100,-50,0,50,100},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,1},new int[]{1,2},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> sortArray(vector<int>& nums){return nums;}};
// USER_CODE_END
 void test(vector<int> a,vector<int> e,int tc,bool hd=false){vector<int> r=CodeCoder().sortArray(a);bool ok=(r==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:n="<<a.size()<<":exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(int i=0;i<(int)r.size();i++){if(i)cout<<",";cout<<r[i];}cout<<"]\\n";}}
int main(){
try{test({5,2,3,1},{1,2,3,5},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5,1,1,2,0,0},{0,0,1,1,2,5},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({3,2,1},{1,2,3},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-2,3,0,-5,4},{-5,-2,0,3,4},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({9,8,7,6,5,4,3,2,1},{1,2,3,4,5,6,7,8,9},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,1,1,1},{1,1,1,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({10,10,5,10,5},{5,5,10,10,10},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-100,100,0,-50,50},{-100,-50,0,50,100},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,1},{1,2},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sortArray(self, nums):
        return nums
# USER_CODE_END
def test(a,e,tc,hd=False):r=CodeCoder().sortArray(list(a));ok=(r==e);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:n={len(a)}:arr={a}:exp={e}:got={r}"))
try:test([5,2,3,1],[1,2,3,5],1)
except:print("TC:1:FAIL:hidden")
try:test([5,1,1,2,0,0],[0,0,1,1,2,5],2)
except:print("TC:2:FAIL:hidden")
try:test([1],[1],3)
except:print("TC:3:FAIL:hidden")
try:test([3,2,1],[1,2,3],4)
except:print("TC:4:FAIL:hidden")
try:test([-2,3,0,-5,4],[-5,-2,0,3,4],5)
except:print("TC:5:FAIL:hidden")
try:test([9,8,7,6,5,4,3,2,1],[1,2,3,4,5,6,7,8,9],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,1,1,1],[1,1,1,1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,10,5,10,5],[5,5,10,10,10],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-100,100,0,-50,50],[-100,-50,0,50,100],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,1],[1,2],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sortArray(nums) { return nums; }
// USER_CODE_END
function test(a,e,tc,hd){if(hd===undefined)hd=false;const r=sortArray(a.slice());let ok=r.length===e.length&&r.every((v,i)=>v===e[i]);if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+a.length+":arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(r));}
try{test([5,2,3,1],[1,2,3,5],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,1,1,2,0,0],[0,0,1,1,2,5],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([3,2,1],[1,2,3],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-2,3,0,-5,4],[-5,-2,0,3,4],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([9,8,7,6,5,4,3,2,1],[1,2,3,4,5,6,7,8,9],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,1,1,1],[1,1,1,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,10,5,10,5],[5,5,10,10,10],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-100,100,0,-50,50],[-100,-50,0,50,100],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,1],[1,2],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* sortArray(int* nums,int n,int* rs) {
    // Write your code here — sort ascending, set *rs = n, return malloc'd array
    *rs = 0; return NULL;
}
// USER_CODE_END

int cmp(const void* a,const void* b){return *(int*)a-*(int*)b;}
void runTest(int* a,int n,int* e,int tc,int hd){
    int rs=0;int* r=sortArray(a,n,&rs);
    int ok=(rs==n);
    if(ok)for(int i=0;i<n;i++){if(r[i]!=e[i]){ok=0;break;}}
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:n=%d:exp=[",tc,n);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",e[i]);}printf("]:got=[");for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",r[i]);}printf("]\\n");}
    free(r);
}
int main(){
    int a1[]={5,2,3,1};int e1[]={1,2,3,5};runTest(a1,4,e1,1,0);
    int a2[]={5,1,1,2,0,0};int e2[]={0,0,1,1,2,5};runTest(a2,6,e2,2,0);
    int a3[]={1};int e3[]={1};runTest(a3,1,e3,3,0);
    int a4[]={3,2,1};int e4[]={1,2,3};runTest(a4,3,e4,4,0);
    int a5[]={-2,3,0,-5,4};int e5[]={-5,-2,0,3,4};runTest(a5,5,e5,5,0);
    int a6[]={9,8,7,6,5,4,3,2,1};int e6[]={1,2,3,4,5,6,7,8,9};runTest(a6,9,e6,6,1);
    int a7[]={1,1,1,1};int e7[]={1,1,1,1};runTest(a7,4,e7,7,1);
    int a8[]={10,10,5,10,5};int e8[]={5,5,10,10,10};runTest(a8,5,e8,8,1);
    int a9[]={-100,100,0,-50,50};int e9[]={-100,-50,0,50,100};runTest(a9,5,e9,9,1);
    int a10[]={2,1};int e10[]={1,2};runTest(a10,2,e10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
