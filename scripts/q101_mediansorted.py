"""
Median of Two Sorted Arrays
============================
Given two sorted arrays nums1 and nums2 of sizes n and m (either may be
empty), return the median of the two sorted arrays as a double. If the total
length is odd, the median is the middle element; if even, the average of the
two middle elements. Must run in O(log(min(n,m))).

Examples:
  nums1 = [1,3], nums2 = [2]     -> 2.0
  nums1 = [1,2], nums2 = [3,4]   -> 2.5

Partition-based binary search: split the total n+m elements into a left half
of (n+m+1)/2 elements, taking i from nums1 and j = left-i from nums2, with
i in [0,n] so that nums1[i-1] <= nums2[j] and nums2[j-1] <= nums1[i].

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C both arrays are passed with their lengths: int* a, int n, int* b, int m.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Median of Two Sorted Arrays"
desc=(
    "Given two sorted arrays nums1 and nums2 of sizes n and m (either may be "
    "empty), return the median of the two sorted arrays as a double. When the "
    "total length n+m is odd, the median is the middle element; when it is "
    "even, the median is the average of the two middle elements.\n\n"
    "For example:\n"
    "nums1 = [1,3], nums2 = [2]     -> 2.0\n"
    "nums1 = [1,2], nums2 = [3,4]   -> 2.5\n\n"
    "Use the partition binary search in O(log(min(n,m))): the left half of the "
    "merged array contains (n+m+1)/2 elements, of which we take i from nums1 "
    "and j = left-i from nums2. Binary search i in [0,n] until both "
    "nums1[i-1] <= nums2[j] and nums2[j-1] <= nums1[i] hold (treating "
    "out-of-range indices as -infinity / +infinity). The median is then derived "
    "from the max of the left cut and min of the right cut."
)
infmt="First line contains n and m. Second line contains n space-separated sorted integers (may be empty). Third line contains m space-separated sorted integers (may be empty)."
outfmt="Print the median as a double (e.g. 2.0 or 2.5)."
cons="0 ≤ n, m ≤ 1000\n1 ≤ n + m\n-10^6 ≤ nums1[i], nums2[j] ≤ 10^6\nEach array is sorted ascending."
e1="Input:\n2 1\n1 3\n2\n\nOutput:\n2.0"
e2="Input:\n2 2\n1 2\n3 4\n\nOutput:\n2.5"
e3="Input:\n0 1\n\n1\n\nOutput:\n1.0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search, Double",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public double findMedianSortedArrays(int[] a, int[] b) {
        // Write your code here — partition binary search, O(log(min(n,m)))
        return 0.0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] b,double e,int tc,boolean hd){double r=new CodeCoder().findMedianSortedArrays(a,b);boolean ok=Math.abs(r-e)<1e-9;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:a="+Arrays.toString(a)+":b="+Arrays.toString(b)+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,3},new int[]{2},2.0,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{3,4},2.5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,0},new int[]{0,0},0.0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{},new int[]{1},1.0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2},new int[]{},2.0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,3,5,7},new int[]{2,4,6,8},4.5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4},new int[]{5,6,7},4.0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3},new int[]{4,5},3.0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-5,0,5},new int[]{-3,3},0.0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,1,1},new int[]{1,1,1},1.0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:double findMedianSortedArrays(vector<int>& a,vector<int>& b){return 0.0;}};
// USER_CODE_END
void test(vector<int> a,vector<int> b,double e,int tc,bool hd=false){double r=CodeCoder().findMedianSortedArrays(a,b);bool ok=fabs(r-e)<1e-9;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,3},{2},2.0,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2},{3,4},2.5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,0},{0,0},0.0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({},{1},1.0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2},{},2.0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,3,5,7},{2,4,6,8},4.5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4},{5,6,7},4.0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3},{4,5},3.0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-5,0,5},{-3,3},0.0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,1,1},{1,1,1},1.0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findMedianSortedArrays(self, a, b):
        return 0.0
# USER_CODE_END
def test(a,b,e,tc,hd=False):
    r=CodeCoder().findMedianSortedArrays(a,b);ok=abs(r-e)<1e-9
    print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:a={a}:b={b}:exp={e}:got={r}"))
try:test([1,3],[2],2.0,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2],[3,4],2.5,2)
except:print("TC:2:FAIL:hidden")
try:test([0,0],[0,0],0.0,3)
except:print("TC:3:FAIL:hidden")
try:test([],[1],1.0,4)
except:print("TC:4:FAIL:hidden")
try:test([2],[],2.0,5)
except:print("TC:5:FAIL:hidden")
try:test([1,3,5,7],[2,4,6,8],4.5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4],[5,6,7],4.0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3],[4,5],3.0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-5,0,5],[-3,3],0.0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,1,1],[1,1,1],1.0,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findMedianSortedArrays(a, b) { return 0.0; }
// USER_CODE_END
function test(a,b,e,tc,hd){if(hd===undefined)hd=false;const r=findMedianSortedArrays(a,b);const ok=Math.abs(r-e)<1e-9;if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,3],[2],2.0,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2],[3,4],2.5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,0],[0,0],0.0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([],[1],1.0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2],[],2.0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,3,5,7],[2,4,6,8],4.5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4],[5,6,7],4.0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3],[4,5],3.0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-5,0,5],[-3,3],0.0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,1,1],[1,1,1],1.0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <math.h>
#include <limits.h>

// USER_CODE_START
double findMedianSortedArrays(int* a,int n,int* b,int m) {
    // Write your code here — return the median (double)
    return 0.0;
}
// USER_CODE_END

void runTest(int* a,int n,int* b,int m,double e,int tc,int hd){
    double r=findMedianSortedArrays(a,n,b,m);
    if(fabs(r-e)<1e-9){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%.6f:got=%.6f\\n",tc,e,r);}
}
int main(){
    int a1[]={1,3};int b1[]={2};runTest(a1,2,b1,1,2.0,1,0);
    int a2[]={1,2};int b2[]={3,4};runTest(a2,2,b2,2,2.5,2,0);
    int a3[]={0,0};int b3[]={0,0};runTest(a3,2,b3,2,0.0,3,0);
    int b4[]={1};runTest(NULL,0,b4,1,1.0,4,0);
    int a5[]={2};runTest(a5,1,NULL,0,2.0,5,0);
    int a6[]={1,3,5,7};int b6[]={2,4,6,8};runTest(a6,4,b6,4,4.5,6,1);
    int a7[]={1,2,3,4};int b7[]={5,6,7};runTest(a7,4,b7,3,4.0,7,1);
    int a8[]={1,2,3};int b8[]={4,5};runTest(a8,3,b8,2,3.0,8,1);
    int a9[]={-5,0,5};int b9[]={-3,3};runTest(a9,3,b9,2,0.0,9,1);
    int a10[]={1,1,1};int b10[]={1,1,1};runTest(a10,3,b10,3,1.0,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
