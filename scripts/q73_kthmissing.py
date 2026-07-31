"""
Kth Missing Positive Number
=============================
Given an array arr of positive integers sorted in strictly increasing order,
and an integer k, return the kth positive integer that is missing from the array.

Examples:
  arr = [2,3,4,7,11], k = 5 → 9
  (missing: 1,5,6,8,9,10,... → 5th missing is 9)
  arr = [1,2,3,4], k = 2 → 6
  arr = [5,6,7], k = 3 → 3

Observation: for index i, the number of missing positives before arr[i] is arr[i]-(i+1).
Binary search for the position where missing count >= k.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Kth Missing Positive Number"
desc=(
    "Given an array arr of positive integers sorted in strictly increasing order, "
    "and an integer k, return the kth positive integer that is missing from this array.\n\n"
    "For example:\n"
    "arr = [2,3,4,7,11], k = 5 → 9\n"
    "The missing positives are: 1, 5, 6, 8, 9, 10, ... → the 5th missing is 9.\n"
    "arr = [1,2,3,4], k = 2 → 6 (missing: 5, 6, ... → 2nd is 6)\n"
    "arr = [5,6,7], k = 3 → 3 (missing: 1, 2, 3, ... → 3rd is 3)\n\n"
    "Key observation: before index i, the count of missing numbers is arr[i] - (i + 1). "
    "Binary search for the first index where missing count >= k. "
    "The answer is then computed from that position."
)
infmt="First line contains n.\nSecond line contains n space-separated sorted integers.\nThird line contains k."
outfmt="Print the kth missing positive integer."
cons="1 ≤ n ≤ 1000\n1 ≤ arr[i] ≤ 1000\n1 ≤ k ≤ 1000"
e1="Input:\n5\n2 3 4 7 11\n5\n\nOutput:\n9"
e2="Input:\n4\n1 2 3 4\n2\n\nOutput:\n6"
e3="Input:\n3\n5 6 7\n3\n\nOutput:\n3"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int findKthPositive(int[] arr, int k) {
        // Write your code here — binary search on missing count
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int k,int e,int tc,boolean h){int g=new CodeCoder().findKthPositive(a,k);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":k="+k+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{2,3,4,7,11},5,9,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4},2,6,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,6,7},3,3,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},1,2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2},1,1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},10,15,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,5,8},4,6,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{3,4,5,6,7},2,2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,2},1,3,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,20,30},3,3,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int findKthPositive(vector<int>& arr,int k){return 0;}};
// USER_CODE_END
void test(vector<int> a,int k,int e,int tc,bool h=false){int g=CodeCoder().findKthPositive(a,k);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({2,3,4,7,11},5,9,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4},2,6,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,6,7},3,3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},1,2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2},1,1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},10,15,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,5,8},4,6,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({3,4,5,6,7},2,2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2},1,3,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,20,30},3,3,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findKthPositive(self, arr, k):
        return 0
# USER_CODE_END
def test(a,k,e,tc,h=False):g=CodeCoder().findKthPositive(a,k);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:k={k}:exp={e}:got={g}"))
try:test([2,3,4,7,11],5,9,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4],2,6,2)
except:print("TC:2:FAIL:hidden")
try:test([5,6,7],3,3,3)
except:print("TC:3:FAIL:hidden")
try:test([1],1,2,4)
except:print("TC:4:FAIL:hidden")
try:test([2],1,1,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],10,15,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([2,5,8],4,6,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([3,4,5,6,7],2,2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2],1,3,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,20,30],3,3,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findKthPositive(arr, k) { return 0; }
// USER_CODE_END
function test(a,k,e,tc,h){if(h===undefined)h=false;const g=findKthPositive(a,k);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([2,3,4,7,11],5,9,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4],2,6,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,6,7],3,3,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],1,2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2],1,1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],10,15,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,5,8],4,6,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([3,4,5,6,7],2,2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2],1,3,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,20,30],3,3,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int findKthPositive(int* arr,int n,int k) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int k,int e,int tc,int h){
    int g=findKthPositive(a,n,k);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={2,3,4,7,11};runTest(t1,5,5,9,1,0);
    int t2[]={1,2,3,4};runTest(t2,4,2,6,2,0);
    int t3[]={5,6,7};runTest(t3,3,3,3,3,0);
    int t4[]={1};runTest(t4,1,1,2,4,0);
    int t5[]={2};runTest(t5,1,1,1,5,0);
    int t6[]={1,2,3,4,5};runTest(t6,5,10,15,6,1);
    int t7[]={2,5,8};runTest(t7,3,4,6,7,1);
    int t8[]={3,4,5,6,7};runTest(t8,5,2,2,8,1);
    int t9[]={1,2};runTest(t9,2,1,3,9,1);
    int t10[]={10,20,30};runTest(t10,3,3,3,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
