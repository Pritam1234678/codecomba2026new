"""
Remove Duplicates from Array
===============================
Given a sorted array arr, remove duplicate elements in-place and return
the count of unique elements.

Examples:
  arr = [1, 1, 2] → 2 (first two elements become [1, 2])
  arr = [0, 0, 1, 1, 1, 2, 2, 3, 3, 4] → 5

Two-pointer approach: j tracks unique position, i scans.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Duplicates from Array"
desc=(
    "Given a sorted integer array arr, remove the duplicate elements in-place such "
    "that each unique element appears only once. Return the count of unique elements.\n\n"
    "For example:\n"
    "arr = [1, 1, 2] → unique count = 2, first 2 elements become [1, 2]\n"
    "arr = [0, 0, 1, 1, 1, 2, 2, 3, 3, 4] → unique count = 5\n\n"
    "Use a two-pointer approach. Maintain a position pointer j for the next unique "
    "element. Iterate i from 1 to n-1. If arr[i] != arr[i-1], place arr[i] at arr[j++]."
)
infmt="First line contains n.\nSecond line contains n space-separated sorted integers."
outfmt="Print the count of unique elements."
cons="1 ≤ n ≤ 3*10^4\n-100 ≤ arr[i] ≤ 100\nArray is sorted in non-decreasing order."
e1="Input:\n3\n1 1 2\n\nOutput:\n2"
e2="Input:\n10\n0 0 1 1 1 2 2 3 3 4\n\nOutput:\n5"
e3="Input:\n1\n42\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int removeDuplicates(int[] arr) {
        // Write your code here — two-pointer
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().removeDuplicates(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,1,2},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,0,1,1,1,2,2,3,3,4},5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{42},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3},3,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,1,1},1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-5,-5,-4,-3,-3},3,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{0,0,0},1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,0,1,2},4,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,5,5,5,5,5},1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100,100,101,101,102},3,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int removeDuplicates(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().removeDuplicates(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,1,2},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,0,1,1,1,2,2,3,3,4},5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({42},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3},3,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,1,1},1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({-5,-5,-4,-3,-3},3,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({0,0,0},1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,0,1,2},4,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,5,5,5,5,5},1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100,100,101,101,102},3,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def removeDuplicates(self, arr):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().removeDuplicates(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,1,2],2,1)
except:print("TC:1:FAIL:hidden")
try:test([0,0,1,1,1,2,2,3,3,4],5,2)
except:print("TC:2:FAIL:hidden")
try:test([42],1,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3],3,4)
except:print("TC:4:FAIL:hidden")
try:test([1,1,1,1],1,5)
except:print("TC:5:FAIL:hidden")
try:test([-5,-5,-4,-3,-3],3,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,0,0],1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1,0,1,2],4,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,5,5,5,5,5],1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100,100,101,101,102],3,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function removeDuplicates(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=removeDuplicates(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,1,2],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,0,1,1,1,2,2,3,3,4],5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([42],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3],3,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,1,1],1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-5,-5,-4,-3,-3],3,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,0,0],1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,0,1,2],4,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,5,5,5,5,5],1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100,100,101,101,102],3,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int removeDuplicates(int* arr,int n) {
    // Write your code here — two-pointer
    return 0;
}
// USER_CODE_END

void run(int* a,int n,int e,int tc,int h){int g=removeDuplicates(a,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int t1[]={1,1,2};run(t1,3,2,1,0);
int t2[]={0,0,1,1,1,2,2,3,3,4};run(t2,10,5,2,0);
int t3[]={42};run(t3,1,1,3,0);
int t4[]={1,2,3};run(t4,3,3,4,0);
int t5[]={1,1,1,1};run(t5,4,1,5,0);
int t6[]={-5,-5,-4,-3,-3};run(t6,5,3,6,1);
int t7[]={0,0,0};run(t7,3,1,7,1);
int t8[]={-1,0,1,2};run(t8,4,4,8,1);
int t9[]={5,5,5,5,5,5};run(t9,6,1,9,1);
int t10[]={100,100,101,101,102};run(t10,5,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
