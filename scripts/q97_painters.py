"""
Painter's Partition Problem
=============================
Given an array boards of length n (boards[i] = time/length of the i-th board)
and k painters, each painter paints a CONTIGUOUS set of boards, painting 1 unit
per unit time. All painters work in parallel. Minimize the maximum time any
painter takes, and return that minimized maximum time. (If k > n, still answer
max(boards) — a painter per board.)

Examples:
  boards = [5,10,30,20,15], k = 3 -> 35
  boards = [10,20,30,40], k = 2 -> 60

Binary search the answer (time) in [max(boards), sum(boards)]. For a candidate
time t, greedily count how many painters are needed so no painter's total
exceeds t (accumulate while cur + boards[i] <= t, else start a new painter).
If painters used <= k, t is feasible (try smaller); else we need a larger t.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the boards array is passed with its length n: int* boards, int n, int k.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Painter's Partition Problem"
desc=(
    "You have an array boards of length n where boards[i] is the length (and "
    "hence the time needed to paint it) of the i-th board. There are k "
    "painters; each painter paints a CONTIGUOUS set of boards and paints one "
    "unit per unit of time. All painters work in parallel. Find the minimum "
    "possible value of the MAXIMUM time any painter takes, and return it.\n\n"
    "For example:\n"
    "boards = [5,10,30,20,15], k = 3 -> 35\n"
    "boards = [10,20,30,40], k = 2 -> 60\n\n"
    "Binary search the answer time t in [max(boards), sum(boards)]. For a "
    "candidate t, greedily count the minimum number of painters needed so that "
    "no painter's total work exceeds t. If that count <= k, t is feasible (try "
    "a smaller t); otherwise we need a larger t. Runs in O(n * log(sum))."
)
infmt="First line contains n and k (number of boards and painters). Second line contains n space-separated board lengths."
outfmt="Print the minimized maximum time (board length sum) assigned to any painter."
cons="1 ≤ n ≤ 10^5\n1 ≤ k ≤ 10^5\n1 ≤ boards[i] ≤ 10^5"
e1="Input:\n5 3\n5 10 30 20 15\n\nOutput:\n35"
e2="Input:\n4 2\n10 20 30 40\n\nOutput:\n60"
e3="Input:\n4 4\n10 10 10 10\n\nOutput:\n10"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int minTime(int[] boards, int k) {
        // Write your code here — binary search the minimized maximum time
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] b,int k,int e,int tc,boolean hd){int r=new CodeCoder().minTime(b,k);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:boards="+Arrays.toString(b)+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{5,10,30,20,15},3,35,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10,20,30,40},2,60,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},2,9,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,5,5,5},2,10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{100,200,300,400},2,600,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9},3,17,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,10,10,10},4,10,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,8,11,3},2,14,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{3,6,6,7,9},2,16,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},3,60,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int minTime(vector<int>& boards,int k){return 0;}};
// USER_CODE_END
void test(vector<int> b,int k,int e,int tc,bool hd=false){int r=CodeCoder().minTime(b,k);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({5,10,30,20,15},3,35,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10,20,30,40},2,60,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3,4,5},2,9,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,5,5,5},2,10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({100,200,300,400},2,600,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9},3,17,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,10,10,10},4,10,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,8,11,3},2,14,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({3,6,6,7,9},2,16,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,20,30,40,50},3,60,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minTime(self, boards, k):
        return 0
# USER_CODE_END
def test(b,k,e,tc,hd=False):r=CodeCoder().minTime(b,k);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:boards={b}:k={k}:exp={e}:got={r}"))
try:test([5,10,30,20,15],3,35,1)
except:print("TC:1:FAIL:hidden")
try:test([10,20,30,40],2,60,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3,4,5],2,9,3)
except:print("TC:3:FAIL:hidden")
try:test([5,5,5,5],2,10,4)
except:print("TC:4:FAIL:hidden")
try:test([100,200,300,400],2,600,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9],3,17,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,10,10,10],4,10,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,8,11,3],2,14,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([3,6,6,7,9],2,16,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,20,30,40,50],3,60,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minTime(boards, k) { return 0; }
// USER_CODE_END
function test(b,k,e,tc,hd){if(hd===undefined)hd=false;const r=minTime(b,k);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([5,10,30,20,15],3,35,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10,20,30,40],2,60,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3,4,5],2,9,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,5,5,5],2,10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([100,200,300,400],2,600,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9],3,17,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,10,10,10],4,10,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,8,11,3],2,14,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([3,6,6,7,9],2,16,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,20,30,40,50],3,60,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int minTime(int* boards,int n,int k) {
    // Write your code here — return the minimized maximum time
    return 0;
}
// USER_CODE_END

void runTest(int* b,int n,int k,int e,int tc,int hd){
    int r=minTime(b,n,k);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={5,10,30,20,15};runTest(t1,5,3,35,1,0);
    int t2[]={10,20,30,40};runTest(t2,4,2,60,2,0);
    int t3[]={1,2,3,4,5};runTest(t3,5,2,9,3,0);
    int t4[]={5,5,5,5};runTest(t4,4,2,10,4,0);
    int t5[]={100,200,300,400};runTest(t5,4,2,600,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9};runTest(t6,9,3,17,6,1);
    int t7[]={10,10,10,10};runTest(t7,4,4,10,7,1);
    int t8[]={1,8,11,3};runTest(t8,4,2,14,8,1);
    int t9[]={3,6,6,7,9};runTest(t9,5,2,16,9,1);
    int t10[]={10,20,30,40,50};runTest(t10,5,3,60,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
