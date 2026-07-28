"""
Leaders in Array
==================
Given array arr, find leaders — elements greater than all elements to their right.
The rightmost element is always a leader.

Examples:
  arr = [16,17,4,3,5,2] → [17,5,2]
  arr = [1,2,3,4,0] → [4,0]

Traverse from right to left keeping maxSoFar.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder (returns list of leaders in order)
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Leaders in Array"
desc=(
    "Given an array arr of size n, find all leaders in the array. "
    "An element is a leader if it is strictly greater than all elements to its right. "
    "The rightmost element is always a leader.\n\n"
    "For example:\n"
    "arr = [16, 17, 4, 3, 5, 2] → leaders = [17, 5, 2]\n"
    "  - 17 > max([4,3,5,2]) = 5 → leader\n"
    "  - 5 > max([2]) = 2 → leader\n"
    "  - 2 is rightmost → leader\n"
    "arr = [1, 2, 3, 4, 0] → leaders = [4, 0]\n\n"
    "Traverse from right to left, keep track of max seen so far. "
    "Every time current element > maxSoFar, it's a leader."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the leaders as space-separated integers (in the order they appear from left to right)."
cons="1 ≤ n ≤ 10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n6\n16 17 4 3 5 2\n\nOutput:\n17 5 2"
e2="Input:\n5\n1 2 3 4 0\n\nOutput:\n4 0"
e3="Input:\n3\n5 5 5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder {
    public int[] findLeaders(int[] arr) {
        // Write your code here — traverse right to left, track max
        return new int[0];
    }
}
// USER_CODE_END
public class Main {
static void test(int[] a,int[] e,int tc,boolean h){int[] g=new CodeCoder().findLeaders(a);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{16,17,4,3,5,2},new int[]{17,5,2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,0},new int[]{4,0},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,5,5},new int[]{5},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{10,9,8,7,6},new int[]{10,9,8,7,6},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{5},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-5,-4,-3,-2,-1},new int[]{-1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,0,0,0},new int[]{0},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-100,100,50,0},new int[]{100,50,0},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1000000000,999999999,500},new int[]{1000000000,999999999,500},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> findLeaders(vector<int>& arr){return {};}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){vector<int> g=CodeCoder().findLeaders(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({16,17,4,3,5,2},{17,5,2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,0},{4,0},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,5,5},{5},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},{1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({10,9,8,7,6},{10,9,8,7,6},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{5},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-5,-4,-3,-2,-1},{-1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,0,0,0},{0},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-100,100,50,0},{100,50,0},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1000000000,999999999,500},{1000000000,999999999,500},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findLeaders(self, arr): return []
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().findLeaders(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([16,17,4,3,5,2],[17,5,2],1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4,0],[4,0],2)
except:print("TC:2:FAIL:hidden")
try:test([5,5,5],[5],3)
except:print("TC:3:FAIL:hidden")
try:test([1],[1],4)
except:print("TC:4:FAIL:hidden")
try:test([10,9,8,7,6],[10,9,8,7,6],5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],[5],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-5,-4,-3,-2,-1],[-1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,0,0],[0],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-100,100,50,0],[100,50,0],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1000000000,999999999,500],[1000000000,999999999,500],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findLeaders(arr) { return []; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=findLeaders(a);const gs=JSON.stringify(g),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+es+":got="+gs);}
try{test([16,17,4,3,5,2],[17,5,2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,0],[4,0],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,5,5],[5],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],[1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([10,9,8,7,6],[10,9,8,7,6],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],[5],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-5,-4,-3,-2,-1],[-1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,0,0],[0],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-100,100,50,0],[100,50,0],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1000000000,999999999,500],[1000000000,999999999,500],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int* findLeaders(int* arr,int n,int* rs){*rs=0;return NULL;}
// USER_CODE_END
int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int* a,int n,int* e,int en,int tc,int h){int rs;int*g=findLeaders(a,n,&rs);if(rs==en&&arrEq(g,e,rs)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int t1[]={16,17,4,3,5,2},e1[]={17,5,2};run(t1,6,e1,3,1,0);
int t2[]={1,2,3,4,0},e2[]={4,0};run(t2,5,e2,2,2,0);
int t3[]={5,5,5},e3[]={5};run(t3,3,e3,1,3,0);
int t4[]={1},e4[]={1};run(t4,1,e4,1,4,0);
int t5[]={10,9,8,7,6},e5[]={10,9,8,7,6};run(t5,5,e5,5,5,0);
int t6[]={1,2,3,4,5},e6[]={5};run(t6,5,e6,1,6,1);
int t7[]={-5,-4,-3,-2,-1},e7[]={-1};run(t7,5,e7,1,7,1);
int t8[]={0,0,0,0},e8[]={0};run(t8,4,e8,1,8,1);
int t9[]={-100,100,50,0},e9[]={100,50,0};run(t9,4,e9,3,9,1);
int t10[]={1000000000,999999999,500},e10[]={1000000000,999999999,500};run(t10,3,e10,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
