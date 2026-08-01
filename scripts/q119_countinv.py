"""
Count Inversions (Merge Sort)
===============================
Given an array arr of n integers, count the number of inversions. An inversion
is a pair (i, j) with i < j and arr[i] > arr[j]. Return the total count.

Examples:
  arr = [2,4,1,3,5] -> 3   (pairs (2,1),(4,1),(4,3))
  arr = [2,3,4,5,6] -> 0   (already sorted)

Use MERGE SORT: while merging two sorted halves, every element from the right
half that is taken before an element from the left half contributes an
inversion equal to the number of remaining left elements. Runs in O(n log n).
The count can be large — use a 64-bit integer.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the array is passed as int* arr with length n; return long long.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Inversions"
desc=(
    "Given an array arr of n integers, count the number of inversions in it. "
    "An inversion is a pair of indices (i, j) such that i < j and "
    "arr[i] > arr[j]. Return the total number of such pairs.\n\n"
    "For example:\n"
    "arr = [2,4,1,3,5] -> 3   (pairs (2,1), (4,1), (4,3))\n"
    "arr = [2,3,4,5,6] -> 0   (already sorted — no inversions)\n\n"
    "Use MERGE SORT: during the merge step, whenever an element from the right "
    "half is placed before the remaining elements of the left half, it forms "
    "one inversion with each of those remaining left elements. Accumulate the "
    "counts across all merges. Complexity O(n log n). The answer can exceed "
    "32 bits, so use a 64-bit integer."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the number of inversions."
cons="1 ≤ n ≤ 10^5\n1 ≤ arr[i] ≤ 10^5\nUse a 64-bit type for the count."
e1="Input:\n5\n2 4 1 3 5\n\nOutput:\n3"
e2="Input:\n5\n2 3 4 5 6\n\nOutput:\n0"
e3="Input:\n5\n5 4 3 2 1\n\nOutput:\n10"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Sorting, Merge Sort, Divide and Conquer",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long countInversions(int[] arr) {
        // Write your code here — merge sort based count
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,long e,int tc,boolean hd){long g=new CodeCoder().countInversions(a.clone());if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test(new int[]{2,4,1,3,5},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,3,4,5,6},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{10,10,10},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{8,4,2,1},6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,3,2,3,1},4,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{7,5,3,1,9,4},9,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{468,335,1,170,225,479,359,463,465,206},20,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,20,6,4,5},5,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:long long countInversions(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,long long e,int tc,bool hd=false){long long g=CodeCoder().countInversions(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({2,4,1,3,5},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,3,4,5,6},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({10,10,10},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,4,3,2,1},10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({8,4,2,1},6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,3,2,3,1},4,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({7,5,3,1,9,4},9,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({468,335,1,170,225,479,359,463,465,206},20,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,20,6,4,5},5,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countInversions(self, arr):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().countInversions(list(a));ok=(g==e);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([2,4,1,3,5],3,1)
except:print("TC:1:FAIL:hidden")
try:test([2,3,4,5,6],0,2)
except:print("TC:2:FAIL:hidden")
try:test([10,10,10],0,3)
except:print("TC:3:FAIL:hidden")
try:test([5,4,3,2,1],10,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],0,5)
except:print("TC:5:FAIL:hidden")
try:test([8,4,2,1],6,6,True)
except:print("TC:6:FAIL:hidden")
try:test([1,3,2,3,1],4,7,True)
except:print("TC:7:FAIL:hidden")
try:test([7,5,3,1,9,4],9,8,True)
except:print("TC:8:FAIL:hidden")
try:test([468,335,1,170,225,479,359,463,465,206],20,9,True)
except:print("TC:9:FAIL:hidden")
try:test([1,20,6,4,5],5,10,True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countInversions(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=countInversions(a.slice());if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+e+":got="+g);}
try{test([2,4,1,3,5],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,3,4,5,6],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([10,10,10],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,4,3,2,1],10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([8,4,2,1],6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,3,2,3,1],4,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([7,5,3,1,9,4],9,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([468,335,1,170,225,479,359,463,465,206],20,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,20,6,4,5],5,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
long long countInversions(int* arr,int n) {
    // Write your code here — merge sort based count
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,long long e,int tc,int hd){
    long long g=countInversions(a,n);
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n",tc,e,g);}
}
int main(){
    int t1[]={2,4,1,3,5};runTest(t1,5,3,1,0);
    int t2[]={2,3,4,5,6};runTest(t2,5,0,2,0);
    int t3[]={10,10,10};runTest(t3,3,0,3,0);
    int t4[]={5,4,3,2,1};runTest(t4,5,10,4,0);
    int t5[]={1,2,3,4,5};runTest(t5,5,0,5,0);
    int t6[]={8,4,2,1};runTest(t6,4,6,6,1);
    int t7[]={1,3,2,3,1};runTest(t7,5,4,7,1);
    int t8[]={7,5,3,1,9,4};runTest(t8,6,9,8,1);
    int t9[]={468,335,1,170,225,479,359,463,465,206};runTest(t9,10,20,9,1);
    int t10[]={1,20,6,4,5};runTest(t10,5,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
