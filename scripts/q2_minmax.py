"""
Min and Max in Array
=====================
Given an array arr of size n, find the minimum and maximum elements in the array.

Examples:
  arr = [3, 1, 7, 5, 2]  → min = 1, max = 7
  arr = [10]              → min = 10, max = 10

Simply traverse the array keeping track of min and max.

10 test cases — 5 visible, 5 hidden. Class returns int[] with [min, max].
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Min and Max in Array"
desc=(
    "Given an array arr of size n, find the minimum and maximum elements present "
    "in the array.\n\n"
    "For example:\n"
    "arr = [3, 1, 7, 5, 2] → minimum = 1, maximum = 7\n"
    "arr = [10] → minimum = 10, maximum = 10\n\n"
    "Traverse the array once, maintaining two variables: minVal and maxVal. "
    "Initialize both to the first element. For each subsequent element, update "
    "minVal if it's smaller, and maxVal if it's larger."
)
infmt="First line contains integer n.\nSecond line contains n space-separated integers."
outfmt="Print the minimum and maximum separated by a space."
cons="1 ≤ n ≤ 10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n3 1 7 5 2\n\nOutput:\n1 7"
e2="Input:\n1\n10\n\nOutput:\n10 10"
e3="Input:\n4\n-5 -2 -8 -1\n\nOutput:\n-8 -1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] findMinMax(int[] arr) {
        // Write your code here — return int[2] with [min, max]
        return new int[0];
    }
}
// USER_CODE_END

public class Main {
static void test(int[] arr,int[] e,int tc,boolean h){int[] g=new CodeCoder().findMinMax(arr);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(arr)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{3,1,7,5,2},new int[]{1,7},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10},new int[]{10,10},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{-5,-2,-8,-1},new int[]{-8,-1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-100,0,100},new int[]{-100,100},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{500,400,300,200,100},new int[]{100,500},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},new int[]{1,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-1000000000,1000000000},new int[]{-1000000000,1000000000},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1000000000},new int[]{1000000000,1000000000},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,0,0},new int[]{0,0},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{-1,-2,-3,-4,-5},new int[]{-5,-1},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> findMinMax(vector<int>& arr){return {};}};
// USER_CODE_END
void test(vector<int> arr,vector<int> e,int tc,bool h=false){auto g=CodeCoder().findMinMax(arr);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({3,1,7,5,2},{1,7},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10},{10,10},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({-5,-2,-8,-1},{-8,-1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-100,0,100},{-100,100},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({500,400,300,200,100},{100,500},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1,1},{1,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-1000000000,1000000000},{-1000000000,1000000000},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1000000000},{1000000000,1000000000},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,0,0},{0,0},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({-1,-2,-3,-4,-5},{-5,-1},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findMinMax(self, arr):
        return []
# USER_CODE_END
def test(arr,e,tc,h=False):g=CodeCoder().findMinMax(arr);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={arr}:exp={e}:got={g}"))
try:test([3,1,7,5,2],[1,7],1)
except:print("TC:1:FAIL:hidden")
try:test([10],[10,10],2)
except:print("TC:2:FAIL:hidden")
try:test([-5,-2,-8,-1],[-8,-1],3)
except:print("TC:3:FAIL:hidden")
try:test([-100,0,100],[-100,100],4)
except:print("TC:4:FAIL:hidden")
try:test([500,400,300,200,100],[100,500],5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1,1],[1,1],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-1000000000,1000000000],[-1000000000,1000000000],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1000000000],[1000000000,1000000000],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,0,0],[0,0],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([-1,-2,-3,-4,-5],[-5,-1],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findMinMax(arr) { return []; }
// USER_CODE_END
function test(arr,e,tc,h){if(h===undefined)h=false;const g=findMinMax(arr);const gs=JSON.stringify(g),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+es+":got="+gs);}
try{test([3,1,7,5,2],[1,7],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10],[10,10],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([-5,-2,-8,-1],[-8,-1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-100,0,100],[-100,100],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([500,400,300,200,100],[100,500],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1,1],[1,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-1000000000,1000000000],[-1000000000,1000000000],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1000000000],[1000000000,1000000000],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,0,0],[0,0],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([-1,-2,-3,-4,-5],[-5,-1],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* findMinMax(int* arr, int n, int* rs) {
    // Write your code here — allocate 2 ints, store {min, max}
    *rs = 0;
    return NULL;
}
// USER_CODE_END

int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void runTest(int* arr,int n,int* e,int en,int tc,int h){
    int rs;int* g=findMinMax(arr,n,&rs);
    if(rs==en&&arrEq(g,e,rs)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}
}
int main(){
int t1[]={3,1,7,5,2},e1[]={1,7};runTest(t1,5,e1,2,1,0);
int t2[]={10},e2[]={10,10};runTest(t2,1,e2,2,2,0);
int t3[]={-5,-2,-8,-1},e3[]={-8,-1};runTest(t3,4,e3,2,3,0);
int t4[]={-100,0,100},e4[]={-100,100};runTest(t4,3,e4,2,4,0);
int t5[]={500,400,300,200,100},e5[]={100,500};runTest(t5,5,e5,2,5,0);
int t6[]={1,1,1,1,1},e6[]={1,1};runTest(t6,5,e6,2,6,1);
int t7[]={-1000000000,1000000000},e7[]={-1000000000,1000000000};runTest(t7,2,e7,2,7,1);
int t8[]={1000000000},e8[]={1000000000,1000000000};runTest(t8,1,e8,2,8,1);
int t9[]={0,0,0,0},e9[]={0,0};runTest(t9,4,e9,2,9,1);
int t10[]={-1,-2,-3,-4,-5},e10[]={-5,-1};runTest(t10,5,e10,2,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
