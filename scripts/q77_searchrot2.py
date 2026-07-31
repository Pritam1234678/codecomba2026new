"""
Search in Rotated Sorted Array II
===================================
Given a sorted array that has been rotated at an unknown pivot, and a target,
return true if target exists in the array. The array MAY contain duplicates.

Examples:
  arr = [2,5,6,0,0,1,2], target = 0 → true
  arr = [2,5,6,0,0,1,2], target = 3 → false

Binary search with duplicate handling: if arr[low]==arr[mid]==arr[high],
shrink both ends.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Search in Rotated Sorted Array II"
desc=(
    "Given a sorted array arr that has been rotated at some unknown pivot, and a "
    "target integer, return true if the target exists in the array, false otherwise.\n\n"
    "Unlike the classic problem, the array MAY contain duplicate elements.\n\n"
    "For example:\n"
    "arr = [2,5,6,0,0,1,2], target = 0 → true\n"
    "arr = [2,5,6,0,0,1,2], target = 3 → false\n\n"
    "Binary search with duplicate handling: if arr[low] == arr[mid] == arr[high], "
    "we cannot determine which side is sorted — shrink both ends (low++, high--). "
    "Otherwise proceed with the standard rotated binary search."
)
infmt="First line contains n.\nSecond line contains n space-separated rotated sorted integers.\nThird line contains target."
outfmt="Print 'true' if target exists, otherwise 'false'."
cons="1 ≤ n ≤ 5000\n-10^4 ≤ arr[i], target ≤ 10^4\nArray is rotated sorted, may contain duplicates."
e1="Input:\n7\n2 5 6 0 0 1 2\n0\n\nOutput:\ntrue"
e2="Input:\n7\n2 5 6 0 0 1 2\n3\n\nOutput:\nfalse"
e3="Input:\n1\n1\n0\n\nOutput:\nfalse"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean search(int[] arr, int target) {
        // Write your code here — rotated binary search with duplicates
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int t,boolean e,int tc,boolean h){boolean g=new CodeCoder().search(a,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":target="+t+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{2,5,6,0,0,1,2},0,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,5,6,0,0,1,2},3,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},0,false,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},1,true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},1,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1},2,true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{3,1,1},3,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,1,1,2,0},2,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{4,5,6,7,0,1,2},0,true,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,2,2,3,2,2,2},3,true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool search(vector<int>& arr,int t){return false;}};
// USER_CODE_END
void test(vector<int> a,int t,bool e,int tc,bool h=false){bool g=CodeCoder().search(a,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test({2,5,6,0,0,1,2},0,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,5,6,0,0,1,2},3,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},0,false,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},1,true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,1,1,1},1,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1},2,true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({3,1,1},3,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,1,1,2,0},2,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({4,5,6,7,0,1,2},0,true,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,2,2,3,2,2,2},3,true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def search(self, arr, target):
        return False
# USER_CODE_END
def test(a,t,e,tc,h=False):g=CodeCoder().search(a,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:target={t}:exp={e}:got={g}"))
try:test([2,5,6,0,0,1,2],0,True,1)
except:print("TC:1:FAIL:hidden")
try:test([2,5,6,0,0,1,2],3,False,2)
except:print("TC:2:FAIL:hidden")
try:test([1],0,False,3)
except:print("TC:3:FAIL:hidden")
try:test([1],1,True,4)
except:print("TC:4:FAIL:hidden")
try:test([1,1,1,1,1],1,True,5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1],2,True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([3,1,1],3,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,1,1,2,0],2,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([4,5,6,7,0,1,2],0,True,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,2,2,3,2,2,2],3,True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function search(arr, target) { return false; }
// USER_CODE_END
function test(a,t,e,tc,h){if(h===undefined)h=false;const g=search(a,t);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([2,5,6,0,0,1,2],0,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,5,6,0,0,1,2],3,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],0,false,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],1,true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,1,1,1],1,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1],2,true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([3,1,1],3,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,1,1,2,0],2,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([4,5,6,7,0,1,2],0,true,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,2,2,3,2,2,2],3,true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
bool search(int* arr,int n,int target) {
    // Write your code here
    return false;
}
// USER_CODE_END

void runTest(int* a,int n,int t,bool e,int tc,int h){
    bool g=search(a,n,t);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}
}
int main(){
    int t1[]={2,5,6,0,0,1,2};runTest(t1,7,0,true,1,0);
    int t2[]={2,5,6,0,0,1,2};runTest(t2,7,3,false,2,0);
    int t3[]={1};runTest(t3,1,0,false,3,0);
    int t4[]={1};runTest(t4,1,1,true,4,0);
    int t5[]={1,1,1,1,1};runTest(t5,5,1,true,5,0);
    int t6[]={1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,1,1,1};runTest(t6,18,2,true,6,1);
    int t7[]={3,1,1};runTest(t7,3,3,true,7,1);
    int t8[]={0,1,1,2,0};runTest(t8,5,2,true,8,1);
    int t9[]={4,5,6,7,0,1,2};runTest(t9,7,0,true,9,1);
    int t10[]={2,2,2,3,2,2,2};runTest(t10,7,3,true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
