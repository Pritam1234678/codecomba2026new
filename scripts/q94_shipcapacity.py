"""
Capacity To Ship Packages Within D Days
========================================
A conveyor belt has packages that must be shipped from one port to another
within days days. The i-th package on the belt has a weight weights[i]. Each
day we load the belt with packages in the given order, but the total weight
loaded each day cannot exceed the ship's capacity. Return the LEAST capacity
that will ship all packages within days days.

Examples:
  weights = [1,2,3,4,5,6,7,8,9,10], days = 5 -> 15
  weights = [3,2,2,4,1,4], days = 3 -> 6

Binary search the capacity in [max(weights), sum(weights)]. For a candidate
cap, simulate the greedy packing: go through the weights in order, accumulating
into the current day's load; when the next weight would exceed cap, start a new
day. If the number of days used <= days, cap is feasible (try smaller); else we
need a bigger cap.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the weights array is passed with its length n: int* weights, int n, int days.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Capacity To Ship Packages Within D Days"
desc=(
    "A conveyor belt has packages that must be shipped within days days. The "
    "i-th package on the belt has weight weights[i]. Each day you load the belt "
    "with packages IN GIVEN ORDER, but the total weight loaded in one day cannot "
    "exceed the ship's capacity. Return the LEAST capacity needed to ship all "
    "packages within days days.\n\n"
    "For example:\n"
    "weights = [1,2,3,4,5,6,7,8,9,10], days = 5 -> 15\n"
    "weights = [3,2,2,4,1,4], days = 3 -> 6\n\n"
    "Binary search the capacity in [max(weights), sum(weights)]. For a candidate "
    "capacity cap, simulate greedy packing: walk the weights in order, summing "
    "into the current day's load; if the next weight would exceed cap, start a "
    "new day. If the total days used <= days, cap is feasible (try a smaller "
    "one); otherwise we need a larger capacity. Runs in O(n * log(sum))."
)
infmt="First line contains n (number of packages) and days. Second line contains n space-separated weights."
outfmt="Print the minimum ship capacity that ships all packages within days days."
cons="1 ≤ n ≤ 5*10^4\n1 ≤ weights[i] ≤ 500\n1 ≤ days ≤ n"
e1="Input:\n10 5\n1 2 3 4 5 6 7 8 9 10\n\nOutput:\n15"
e2="Input:\n6 3\n3 2 2 4 1 4\n\nOutput:\n6"
e3="Input:\n5 1\n1 2 3 4 5\n\nOutput:\n15"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int shipWithinDays(int[] weights, int days) {
        // Write your code here — binary search the least feasible capacity
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] w,int d,int e,int tc,boolean hd){int r=new CodeCoder().shipWithinDays(w,d);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:weights="+Arrays.toString(w)+":days="+d+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},5,15,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,2,2,4,1,4},3,6,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3,1,1},4,3,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},5,50,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,15,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{5,5,5,5,5},5,5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1,1,1,1,1,1},10,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{2,4,6,8,10},3,12,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{3,3,3,3,3,3,3,3},4,6,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},10,10,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int shipWithinDays(vector<int>& weights,int days){return 0;}};
// USER_CODE_END
void test(vector<int> w,int d,int e,int tc,bool hd=false){int r=CodeCoder().shipWithinDays(w,d);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,2,3,4,5,6,7,8,9,10},5,15,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,2,2,4,1,4},3,6,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3,1,1},4,3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({10,20,30,40,50},5,50,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,15,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({5,5,5,5,5},5,5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,1,1,1,1,1,1,1,1,1},10,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({2,4,6,8,10},3,12,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({3,3,3,3,3,3,3,3},4,6,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},10,10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def shipWithinDays(self, weights, days):
        return 0
# USER_CODE_END
def test(w,d,e,tc,hd=False):r=CodeCoder().shipWithinDays(w,d);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:weights={w}:days={d}:exp={e}:got={r}"))
try:test([1,2,3,4,5,6,7,8,9,10],5,15,1)
except:print("TC:1:FAIL:hidden")
try:test([3,2,2,4,1,4],3,6,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3,1,1],4,3,3)
except:print("TC:3:FAIL:hidden")
try:test([10,20,30,40,50],5,50,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],1,15,5)
except:print("TC:5:FAIL:hidden")
try:test([5,5,5,5,5],5,5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,1,1,1,1,1,1,1,1,1],10,1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([2,4,6,8,10],3,12,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([3,3,3,3,3,3,3,3],4,6,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],10,10,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function shipWithinDays(weights, days) { return 0; }
// USER_CODE_END
function test(w,d,e,tc,hd){if(hd===undefined)hd=false;const r=shipWithinDays(w,d);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,2,3,4,5,6,7,8,9,10],5,15,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,2,2,4,1,4],3,6,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3,1,1],4,3,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([10,20,30,40,50],5,50,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],1,15,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([5,5,5,5,5],5,5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,1,1,1,1,1,1,1,1,1],10,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([2,4,6,8,10],3,12,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([3,3,3,3,3,3,3,3],4,6,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],10,10,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int shipWithinDays(int* weights,int n,int days) {
    // Write your code here — return the least feasible capacity
    return 0;
}
// USER_CODE_END

void runTest(int* w,int n,int d,int e,int tc,int hd){
    int r=shipWithinDays(w,n,d);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={1,2,3,4,5,6,7,8,9,10};runTest(t1,10,5,15,1,0);
    int t2[]={3,2,2,4,1,4};runTest(t2,6,3,6,2,0);
    int t3[]={1,2,3,1,1};runTest(t3,5,4,3,3,0);
    int t4[]={10,20,30,40,50};runTest(t4,5,5,50,4,0);
    int t5[]={1,2,3,4,5};runTest(t5,5,1,15,5,0);
    int t6[]={5,5,5,5,5};runTest(t6,5,5,5,6,1);
    int t7[]={1,1,1,1,1,1,1,1,1,1};runTest(t7,10,10,1,7,1);
    int t8[]={2,4,6,8,10};runTest(t8,5,3,12,8,1);
    int t9[]={3,3,3,3,3,3,3,3};runTest(t9,8,4,6,9,1);
    int t10[]={1,2,3,4,5,6,7,8,9,10};runTest(t10,10,10,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
