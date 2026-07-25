"""
Find Element at a Given Index
===============================
Given an array arr of size n and an integer index, return the element at that index.
If the index is outside the valid range (0 to n-1), return -1 to indicate invalid index.

Examples:
  arr = [10, 20, 30, 40, 50], index = 2  →  30
  arr = [5, 15, 25], index = 5           →  -1 (out of bounds)

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find Element at a Given Index"
desc=(
    "Given an array arr of size n and an integer index, return the element present "
    "at that index in the array.\n\n"
    "If the given index is outside the valid range (0 to n-1), return -1 to indicate "
    "that the index is out of bounds.\n\n"
    "For example:\n"
    "arr = [10, 20, 30, 40, 50], index = 2 → element at index 2 is 30 → return 30\n"
    "arr = [5, 15, 25], index = 5 → index 5 is out of bounds (max index is 2) → return -1\n\n"
    "This is a basic array access problem. Simply check if index is between 0 and n-1. "
    "If yes, return arr[index]; otherwise return -1."
)
infmt="First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer index."
outfmt="Print the element at the given index, or -1 if out of bounds."
cons="1 ≤ n ≤ 1000\n-10^6 ≤ arr[i] ≤ 10^6\n-10^5 ≤ index ≤ 10^5"
e1="Input:\n5\n10 20 30 40 50\n2\n\nOutput:\n30\n\nExplanation: arr[2] = 30."
e2="Input:\n3\n5 15 25\n5\n\nOutput:\n-1\n\nExplanation: index 5 is out of bounds (max index is 2), so return -1."
e3="Input:\n1\n100\n0\n\nOutput:\n100"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int findElement(int[] arr, int index) {
        // Write your code here — check bounds, return element or -1
        return -1;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] arr,int idx,int e,int tc,boolean h){int g=new CodeCoder().findElement(arr,idx);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(arr)+":idx="+idx+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{10,20,30,40,50},2,30,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5,15,25},5,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{100},0,100,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-5,-10,-15},1,-10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{42},-1,-1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},9,10,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},10,-1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,0,0},1,0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-1000000,1000000},0,-1000000,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{7,8,9},3,-1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int findElement(vector<int>& arr,int idx){return -1;}};
// USER_CODE_END
void test(vector<int> arr,int idx,int e,int tc,bool h=false){int g=CodeCoder().findElement(arr,idx);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({10,20,30,40,50},2,30,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5,15,25},5,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({100},0,100,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-5,-10,-15},1,-10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({42},-1,-1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},9,10,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},10,-1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,0,0},1,0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-1000000,1000000},0,-1000000,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({7,8,9},3,-1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findElement(self, arr, index):
        # Write your code here — check bounds, return element or -1
        return -1
# USER_CODE_END
def test(arr,idx,e,tc,h=False):g=CodeCoder().findElement(arr,idx);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={arr}:idx={idx}:exp={e}:got={g}"))
try:test([10,20,30,40,50],2,30,1)
except:print("TC:1:FAIL:hidden")
try:test([5,15,25],5,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([100],0,100,3)
except:print("TC:3:FAIL:hidden")
try:test([-5,-10,-15],1,-10,4)
except:print("TC:4:FAIL:hidden")
try:test([42],-1,-1,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],9,10,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],10,-1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,0],1,0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-1000000,1000000],0,-1000000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([7,8,9],3,-1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findElement(arr, index) { return -1; }
// USER_CODE_END
function test(arr,idx,e,tc,h){if(h===undefined)h=false;const g=findElement(arr,idx);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([10,20,30,40,50],2,30,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,15,25],5,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([100],0,100,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-5,-10,-15],1,-10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([42],-1,-1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],9,10,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],10,-1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,0],1,0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-1000000,1000000],0,-1000000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([7,8,9],3,-1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int findElement(int* arr, int n, int index) {
    // Write your code here — check bounds, return element or -1
    return -1;
}
// USER_CODE_END

void runTest(int* arr,int n,int idx,int e,int tc,int h){
    int g=findElement(arr,n,idx);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:idx=%d:exp=%d:got=%d\\n",tc,idx,e,g);}
}
int main(){
int t1[]={10,20,30,40,50};runTest(t1,5,2,30,1,0);
int t2[]={5,15,25};runTest(t2,3,5,-1,2,0);
int t3[]={100};runTest(t3,1,0,100,3,0);
int t4[]={-5,-10,-15};runTest(t4,3,1,-10,4,0);
int t5[]={42};runTest(t5,1,-1,-1,5,0);
int t6[]={1,2,3,4,5,6,7,8,9,10};runTest(t6,10,9,10,6,1);
int t7[]={1,2,3,4,5,6,7,8,9,10};runTest(t7,10,10,-1,7,1);
int t8[]={0,0,0};runTest(t8,3,1,0,8,1);
int t9[]={-1000000,1000000};runTest(t9,2,0,-1000000,9,1);
int t10[]={7,8,9};runTest(t10,3,3,-1,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
