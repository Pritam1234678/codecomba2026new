"""
Increasing Array
==================
Given an array arr of size n, you want to make it strictly increasing (each
element < next element). You can only increase elements. Find the minimum
total number of increments needed.

Examples:
  arr = [3,2,5,1,7] → total increments = 5
    [3,2,5,1,7] → [3,3,5,1,7] (cost 1) → [3,3,5,5,7] (cost 4) = 5

  arr = [1,2,3] → 0 (already increasing)

For each i from 1 to n-1, if arr[i] <= arr[i-1], increase arr[i] to arr[i-1]+1.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Increasing Array"
desc=(
    "Given an array arr of size n, you want to make the array strictly increasing "
    "(each element is strictly greater than the previous element). "
    "You are only allowed to increase the values of elements. "
    "Find the minimum total number of increments needed.\n\n"
    "For example:\n"
    "arr = [3, 2, 5, 1, 7] → total increments = 5\n"
    "  Step 1: arr[1]=2 < arr[0]=3, increase 2→3 (cost 1) → [3,3,5,1,7]\n"
    "  Step 2: arr[3]=1 < arr[2]=5, increase 1→5 (cost 4) → [3,3,5,5,7]\n"
    "  Now array is increasing (3<3<5<5<7), total cost = 1+4 = 5\n\n"
    "arr = [1, 2, 3] → already increasing → cost = 0\n\n"
    "Iterate from left to right. If arr[i] <= arr[i-1], add (arr[i-1]+1 - arr[i]) "
    "to the total and set arr[i] = arr[i-1]+1."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the minimum total number of increments."
cons="1 ≤ n ≤ 2*10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n3 2 5 1 7\n\nOutput:\n5"
e2="Input:\n3\n1 2 3\n\nOutput:\n0"
e3="Input:\n3\n5 4 3\n\nOutput:\n3\n\nExplanation: 5→5 (0), 4→6 (2), 3→7 (4) → total = 6. Or simpler: make arr[1]=6 (cost 2), arr[2]=7 (cost 4) total=6."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Greedy",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long minIncrements(int[] arr) {
        // Write your code here — greedy, count increments needed
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,long e,int tc,boolean h){long g=new CodeCoder().minIncrements(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{3,2,5,1,7},5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,4,3},6,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,1},2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-5,-10,-15},15,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1000000000,1,2,3},2999999994L,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,5,2,8,3,9},6,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,0,0,0,0,0},21,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{-100,-50,0,50,100},0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:long long minIncrements(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,long long e,int tc,bool h=false){long long g=CodeCoder().minIncrements(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({3,2,5,1,7},5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,4,3},6,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,1},2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({-5,-10,-15},15,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1000000000,1,2,3},2999999994LL,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,5,2,8,3,9},6,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,0,0,0,0,0},21,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({-100,-50,0,50,100},0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minIncrements(self, arr):
        return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().minIncrements(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([3,2,5,1,7],5,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],0,2)
except:print("TC:2:FAIL:hidden")
try:test([5,4,3],6,3)
except:print("TC:3:FAIL:hidden")
try:test([1],0,4)
except:print("TC:4:FAIL:hidden")
try:test([1,1,1],2,5)
except:print("TC:5:FAIL:hidden")
try:test([-5,-10,-15],15,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1000000000,1,2,3],2999999994,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,5,2,8,3,9],6,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,0,0,0,0,0],21,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([-100,-50,0,50,100],0,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minIncrements(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=minIncrements(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([3,2,5,1,7],5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,4,3],6,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,1],2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-5,-10,-15],15,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1000000000,1,2,3],2999999994,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,5,2,8,3,9],6,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,0,0,0,0,0],21,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([-100,-50,0,50,100],0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
long long minIncrements(int* arr,int n){return 0;}
// USER_CODE_END
void run(int* a,int n,long long e,int tc,int h){long long g=minIncrements(a,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n",tc,(long long)e,g);}}
int main(){
int t1[]={3,2,5,1,7};run(t1,5,5,1,0);
int t2[]={1,2,3};run(t2,3,0,2,0);
int t3[]={5,4,3};run(t3,3,6,3,0);
int t4[]={1};run(t4,1,0,4,0);
int t5[]={1,1,1};run(t5,3,2,5,0);
int t6[]={-5,-10,-15};run(t6,3,15,6,1);
int t7[]={1000000000,1,2,3};run(t7,4,2999999994LL,7,1);
int t8[]={1,5,2,8,3,9};run(t8,6,6,8,1);
int t9[]={0,0,0,0,0,0,0};run(t9,7,21,9,1);
int t10[]={-100,-50,0,50,100};run(t10,5,0,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
