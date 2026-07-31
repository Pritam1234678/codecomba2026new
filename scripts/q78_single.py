"""
Single Element in a Sorted Array
==================================
Given a sorted array consisting of only integers where every element appears
exactly twice, except for one element which appears exactly once, find the
single element. Must run in O(log n) time.

Examples:
  arr = [1,1,2,3,3,4,4,8,8] → 2
  arr = [3,3,7,7,10,11,11] → 10

Binary search: use parity of mid index to decide which half contains the single.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Single Element in a Sorted Array"
desc=(
    "Given a sorted array arr consisting of integers where every element appears "
    "exactly twice, except for one element which appears exactly once, find the "
    "single element that appears only once.\n\n"
    "Your solution must run in O(log n) time and O(1) space.\n\n"
    "For example:\n"
    "arr = [1,1,2,3,3,4,4,8,8] → single element = 2\n"
    "arr = [3,3,7,7,10,11,11] → single element = 10\n\n"
    "Binary search trick: before the single element, pairs start at even indices "
    "(arr[2i] == arr[2i+1]). After the single element, pairs are misaligned. "
    "Use mid parity to decide which half to search."
)
infmt="First line contains n (odd).\nSecond line contains n space-separated sorted integers."
outfmt="Print the single element."
cons="1 ≤ n ≤ 10^5 (n is odd)\narr is sorted, every element appears twice except one."
e1="Input:\n9\n1 1 2 3 3 4 4 8 8\n\nOutput:\n2"
e2="Input:\n7\n3 3 7 7 10 11 11\n\nOutput:\n10"
e3="Input:\n1\n5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int singleElement(int[] arr) {
        // Write your code here — binary search with mid parity
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().singleElement(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,1,2,3,3,4,4,8,8},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,3,7,7,10,11,11},10,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5},5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,1,2},2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,2},1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,0,1,2,2,3,3},1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-5,-5,-3,-3,0,1,1},0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,1,2,2,3,3,4,4,5,5,6},6,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,1,2,2,3},3,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{7,7,8,8,9,9,10,11,11},10,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int singleElement(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().singleElement(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,1,2,3,3,4,4,8,8},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,3,7,7,10,11,11},10,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5},5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,1,2},2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,2},1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,0,1,2,2,3,3},1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-5,-5,-3,-3,0,1,1},0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,1,2,2,3,3,4,4,5,5,6},6,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,1,2,2,3},3,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({7,7,8,8,9,9,10,11,11},10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def singleElement(self, arr):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().singleElement(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,1,2,3,3,4,4,8,8],2,1)
except:print("TC:1:FAIL:hidden")
try:test([3,3,7,7,10,11,11],10,2)
except:print("TC:2:FAIL:hidden")
try:test([5],5,3)
except:print("TC:3:FAIL:hidden")
try:test([1,1,2],2,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,2],1,5)
except:print("TC:5:FAIL:hidden")
try:test([0,0,1,2,2,3,3],1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-5,-5,-3,-3,0,1,1],0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,1,2,2,3,3,4,4,5,5,6],6,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,1,2,2,3],3,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([7,7,8,8,9,9,10,11,11],10,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function singleElement(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=singleElement(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,1,2,3,3,4,4,8,8],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,3,7,7,10,11,11],10,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5],5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,1,2],2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,2],1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,0,1,2,2,3,3],1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-5,-5,-3,-3,0,1,1],0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,1,2,2,3,3,4,4,5,5,6],6,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,1,2,2,3],3,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([7,7,8,8,9,9,10,11,11],10,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int singleElement(int* arr,int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* a,int n,int e,int tc,int h){
    int g=singleElement(a,n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    int t1[]={1,1,2,3,3,4,4,8,8};runTest(t1,9,2,1,0);
    int t2[]={3,3,7,7,10,11,11};runTest(t2,7,10,2,0);
    int t3[]={5};runTest(t3,1,5,3,0);
    int t4[]={1,1,2};runTest(t4,3,2,4,0);
    int t5[]={1,2,2};runTest(t5,3,1,5,0);
    int t6[]={0,0,1,2,2,3,3};runTest(t6,7,1,6,1);
    int t7[]={-5,-5,-3,-3,0,1,1};runTest(t7,7,0,7,1);
    int t8[]={1,1,2,2,3,3,4,4,5,5,6};runTest(t8,11,6,8,1);
    int t9[]={1,1,2,2,3};runTest(t9,5,3,9,1);
    int t10[]={7,7,8,8,9,9,10,11,11};runTest(t10,9,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
