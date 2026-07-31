"""
K-th element of two sorted Arrays
===================================
Given two sorted arrays a and b of sizes n and m, and an integer k (1-indexed),
return the k-th smallest element of the merged sorted array.

Examples:
  a = [2,3,6,7,9], b = [1,4,8,10], k = 5 -> 6
  a = [100,112,256,349,770], b = [72,86,113,119,265,445,892], k = 7 -> 256

Use the partition-based binary search: take i elements from array a and k-i
elements from array b. Binary search i in [max(0,k-m), min(k,n)] such that
a[i-1] <= b[k-i]. Then the answer is max(a[i-1], b[k-i-1]) (guarding out-of-
range sides with -infinity). Runs in O(log(min(n,m))).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C both arrays are passed with their lengths: int* a, int n, int* b, int m, int k.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="K-th element of two sorted Arrays"
desc=(
    "Given two sorted arrays a and b of sizes n and m respectively, and an "
    "integer k (1-indexed), find and return the k-th smallest element of the "
    "merged sorted array.\n\n"
    "For example:\n"
    "a = [2,3,6,7,9], b = [1,4,8,10], k = 5 -> 6\n"
    "a = [100,112,256,349,770], b = [72,86,113,119,265,445,892], k = 7 -> 256\n\n"
    "Binary search how many elements to take from array a (say i) and how many "
    "from array b (k - i). The valid cut satisfies a[i-1] <= b[k-i] and "
    "b[k-i-1] <= a[i]. Then the k-th element is max(a[i-1], b[k-i-1]), treating "
    "out-of-range indices as -infinity. Runs in O(log(min(n,m)))."
)
infmt="First line contains n, m and k. Second line contains n space-separated integers (sorted a). Third line contains m space-separated integers (sorted b)."
outfmt="Print the k-th smallest element of the merged sorted array (1-indexed k)."
cons="1 ≤ n, m ≤ 10^5\n1 ≤ k ≤ n + m\nArrays a and b are each sorted in ascending order."
e1="Input:\n5 4 5\n2 3 6 7 9\n1 4 8 10\n\nOutput:\n6"
e2="Input:\n5 7 7\n100 112 256 349 770\n72 86 113 119 265 445 892\n\nOutput:\n256"
e3="Input:\n5 5 5\n1 2 3 4 5\n6 7 8 9 10\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int kthElement(int[] a, int[] b, int k) {
        // Write your code here — partition binary search, k is 1-indexed
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] b,int k,int e,int tc,boolean hd){int r=new CodeCoder().kthElement(a,b,k);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:a="+Arrays.toString(a)+":b="+Arrays.toString(b)+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{2,3,6,7,9},new int[]{1,4,8,10},5,6,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{100,112,256,349,770},new int[]{72,86,113,119,265,445,892},7,256,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{6,7,8,9,10},5,5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{6,7,8,9,10},8,8,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,3,5,7},new int[]{2,4,6,8},4,4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{10,20,30,40},new int[]{5,15,25,35},3,15,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{3,4,5,6},3,3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3},new int[]{1,2,3},4,2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,6,7},new int[]{1,2,3,4},2,2,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,5,9},new int[]{2,6,10,14,18},6,10,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int kthElement(vector<int>& a,vector<int>& b,int k){return 0;}};
// USER_CODE_END
void test(vector<int> a,vector<int> b,int k,int e,int tc,bool hd=false){int r=CodeCoder().kthElement(a,b,k);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({2,3,6,7,9},{1,4,8,10},5,6,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({100,112,256,349,770},{72,86,113,119,265,445,892},7,256,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{6,7,8,9,10},5,5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{6,7,8,9,10},8,8,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,3,5,7},{2,4,6,8},4,4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({10,20,30,40},{5,15,25,35},3,15,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2},{3,4,5,6},3,3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3},{1,2,3},4,2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,6,7},{1,2,3,4},2,2,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,5,9},{2,6,10,14,18},6,10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def kthElement(self, a, b, k):
        return 0
# USER_CODE_END
def test(a,b,k,e,tc,hd=False):r=CodeCoder().kthElement(a,b,k);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:a={a}:b={b}:k={k}:exp={e}:got={r}"))
try:test([2,3,6,7,9],[1,4,8,10],5,6,1)
except:print("TC:1:FAIL:hidden")
try:test([100,112,256,349,770],[72,86,113,119,265,445,892],7,256,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3,4,5],[6,7,8,9,10],5,5,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5],[6,7,8,9,10],8,8,4)
except:print("TC:4:FAIL:hidden")
try:test([1,3,5,7],[2,4,6,8],4,4,5)
except:print("TC:5:FAIL:hidden")
try:test([10,20,30,40],[5,15,25,35],3,15,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2],[3,4,5,6],3,3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3],[1,2,3],4,2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,6,7],[1,2,3,4],2,2,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,5,9],[2,6,10,14,18],6,10,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function kthElement(a, b, k) { return 0; }
// USER_CODE_END
function test(a,b,k,e,tc,hd){if(hd===undefined)hd=false;const r=kthElement(a,b,k);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([2,3,6,7,9],[1,4,8,10],5,6,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([100,112,256,349,770],[72,86,113,119,265,445,892],7,256,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3,4,5],[6,7,8,9,10],5,5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],[6,7,8,9,10],8,8,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,3,5,7],[2,4,6,8],4,4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([10,20,30,40],[5,15,25,35],3,15,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2],[3,4,5,6],3,3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3],[1,2,3],4,2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,6,7],[1,2,3,4],2,2,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,5,9],[2,6,10,14,18],6,10,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int kthElement(int* a,int n,int* b,int m,int k) {
    // Write your code here — partition binary search, k is 1-indexed
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int* b,int m,int k,int e,int tc,int hd){
    int r=kthElement(a,n,b,m,k);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int a1[]={2,3,6,7,9};int b1[]={1,4,8,10};runTest(a1,5,b1,4,5,6,1,0);
    int a2[]={100,112,256,349,770};int b2[]={72,86,113,119,265,445,892};runTest(a2,5,b2,7,7,256,2,0);
    int a3[]={1,2,3,4,5};int b3[]={6,7,8,9,10};runTest(a3,5,b3,5,5,5,3,0);
    int a4[]={1,2,3,4,5};int b4[]={6,7,8,9,10};runTest(a4,5,b4,5,8,8,4,0);
    int a5[]={1,3,5,7};int b5[]={2,4,6,8};runTest(a5,4,b5,4,4,4,5,0);
    int a6[]={10,20,30,40};int b6[]={5,15,25,35};runTest(a6,4,b6,4,3,15,6,1);
    int a7[]={1,2};int b7[]={3,4,5,6};runTest(a7,2,b7,4,3,3,7,1);
    int a8[]={1,2,3};int b8[]={1,2,3};runTest(a8,3,b8,3,4,2,8,1);
    int a9[]={5,6,7};int b9[]={1,2,3,4};runTest(a9,3,b9,4,2,2,9,1);
    int a10[]={1,5,9};int b10[]={2,6,10,14,18};runTest(a10,3,b10,5,6,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
