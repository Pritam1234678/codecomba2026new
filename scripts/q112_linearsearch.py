"""
Linear Search
==============
Given an array arr of n integers and a target value, return the FIRST index
(0-based) where target appears in arr, or -1 if it is not present.

Examples:
  arr = [1,2,3,4,5], target = 4 -> 3
  arr = [1,2,3,4,5], target = 6 -> -1

Scan the array from left to right and return the first matching index.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the array is passed as int* arr with its length n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Linear Search"
desc=(
    "Given an array arr of n integers and a target integer, return the FIRST "
    "index (0-based) at which target appears in arr, or -1 if target is not "
    "present in the array.\n\n"
    "For example:\n"
    "arr = [1,2,3,4,5], target = 4 -> 3\n"
    "arr = [1,2,3,4,5], target = 6 -> -1\n\n"
    "Simply scan the array from left to right and return the index of the "
    "first element equal to target. This is the classic O(n) linear search."
)
infmt="First line contains n. Second line contains n space-separated integers. Third line contains the target."
outfmt="Print the first 0-based index of target, or -1 if not found."
cons="1 ≤ n ≤ 10^5\n-10^6 ≤ arr[i], target ≤ 10^6"
e1="Input:\n5\n1 2 3 4 5\n4\n\nOutput:\n3"
e2="Input:\n5\n1 2 3 4 5\n6\n\nOutput:\n-1"
e3="Input:\n1\n5\n5\n\nOutput:\n0"

cur.execute("SELECT id FROM problems WHERE title = %s", (title,))
row = cur.fetchone()
if row:
    pid = row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id = %s", (pid,))
    print(f"Updating existing {title} (pid={pid})")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Searching",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Created problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int linearSearch(int[] arr, int target) {
        // Write your code here — return first index of target, or -1
        return -1;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int t,int e,int tc,boolean hd){int r=new CodeCoder().linearSearch(a,t);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":target="+t+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5},4,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},6,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},10,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5},5,0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{9,8,7},7,2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},10,9,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-1,0,1},0,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{100,200,300},150,-1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{3,3,3},3,0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,1,2,1},2,1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int linearSearch(vector<int>& arr,int target){return -1;}};
// USER_CODE_END
 void test(vector<int> a,int t,int e,int tc,bool hd=false){int r=CodeCoder().linearSearch(a,t);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:target="<<t<<":exp="<<e<<":got="<<r<<"\\n";}}
int main(){
try{test({1,2,3,4,5},4,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},6,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({10,20,30,40,50},10,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5},5,0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({9,8,7},7,2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},10,9,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-1,0,1},0,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({100,200,300},150,-1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({3,3,3},3,0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,1,2,1},2,1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def linearSearch(self, arr, target):
        return -1
# USER_CODE_END
def test(a,t,e,tc,hd=False):r=CodeCoder().linearSearch(a,t);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:arr={a}:target={t}:exp={e}:got={r}"))
try:test([1,2,3,4,5],4,3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4,5],6,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([10,20,30,40,50],10,0,3)
except:print("TC:3:FAIL:hidden")
try:test([5],5,0,4)
except:print("TC:4:FAIL:hidden")
try:test([9,8,7],7,2,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],10,9,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-1,0,1],0,1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([100,200,300],150,-1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([3,3,3],3,0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,1,2,1],2,1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function linearSearch(arr, target) { return -1; }
// USER_CODE_END
function test(a,t,e,tc,hd){if(hd===undefined)hd=false;const r=linearSearch(a,t);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":target="+t+":exp="+e+":got="+r);}
try{test([1,2,3,4,5],4,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],6,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([10,20,30,40,50],10,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5],5,0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([9,8,7],7,2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],10,9,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-1,0,1],0,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100,200,300],150,-1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([3,3,3],3,0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,1,2,1],2,1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int linearSearch(int* arr,int n,int target) {
    // Write your code here — return first index of target, or -1
    return -1;
}
// USER_CODE_END

void runTest(int* a,int n,int t,int e,int tc,int hd){
    int r=linearSearch(a,n,t);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:n=%d:arr=[",tc,n);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:target=%d:exp=%d:got=%d\\n",t,e,r);}
}
int main(){
    int t1[]={1,2,3,4,5};runTest(t1,5,4,3,1,0);
    int t2[]={1,2,3,4,5};runTest(t2,5,6,-1,2,0);
    int t3[]={10,20,30,40,50};runTest(t3,5,10,0,3,0);
    int t4[]={5};runTest(t4,1,5,0,4,0);
    int t5[]={9,8,7};runTest(t5,3,7,2,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9,10};runTest(t6,10,10,9,6,1);
    int t7[]={-1,0,1};runTest(t7,3,0,1,7,1);
    int t8[]={100,200,300};runTest(t8,3,150,-1,8,1);
    int t9[]={3,3,3};runTest(t9,3,3,0,9,1);
    int t10[]={1,2,1,2,1};runTest(t10,5,2,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
