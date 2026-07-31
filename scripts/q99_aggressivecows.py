"""
Aggressive Cows
================
You have a farm with N stalls positioned along a straight line at coordinates
given by the array stalls. You have k aggressive cows that must be placed in
k different stalls so that the MINIMUM distance between any two cows is
maximized. Return that maximum possible minimum distance.

Examples:
  stalls = [1,2,4,8,9], k = 3 -> 3   (place at 1, 4, 8 / 2, 4, 8 / 2, 4, 9)
  stalls = [1,3,6,10,15], k = 4 -> 4 (place at 1, 6, 10, 15)

Sort the stalls, then binary search the distance d in [1, max-min]. For a
candidate d, greedily place a cow at the first stall, then at each next stall
that is at least d away from the last placed cow. If we can place k cows, d is
feasible (try larger); else d is too big.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the stalls array is passed with its length n: int* stalls, int n, int k.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Aggressive Cows"
desc=(
    "You own a farm with N stalls positioned along a straight line; stalls[i] "
    "is the coordinate of the i-th stall. You have k aggressive cows that must "
    "be placed into k different stalls so that they do not fight — i.e. the "
    "MINIMUM distance between any two cows must be as large as possible. "
    "Return that maximum possible minimum distance.\n\n"
    "For example:\n"
    "stalls = [1,2,4,8,9], k = 3 -> 3  (e.g. place cows at 1, 4, 8)\n"
    "stalls = [1,3,6,10,15], k = 4 -> 4 (e.g. place cows at 1, 6, 10, 15)\n\n"
    "Approach: sort the stalls. Binary search the minimum distance d in "
    "[1, max(stalls)-min(stalls)]. For a candidate d, greedily place a cow at "
    "the first stall, then each next cow at the earliest stall at least d away "
    "from the previously placed cow. If we manage to place k cows, d is "
    "feasible (try a larger d); otherwise d is too big. Runs in "
    "O(n log n + n log(range))."
)
infmt="First line contains n and k (number of stalls and cows). Second line contains n space-separated stall coordinates (unsorted allowed)."
outfmt="Print the maximum possible minimum distance between any two cows."
cons="2 ≤ k ≤ n ≤ 10^5\n1 ≤ stalls[i] ≤ 10^9"
e1="Input:\n5 3\n1 2 4 8 9\n\nOutput:\n3"
e2="Input:\n5 4\n1 3 6 10 15\n\nOutput:\n4"
e3="Input:\n6 3\n1 4 9 16 25 36\n\nOutput:\n15"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search, Greedy",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int maxDistance(int[] stalls, int k) {
        // Write your code here — binary search the max minimum distance
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] s,int k,int e,int tc,boolean hd){int r=new CodeCoder().maxDistance(s,k);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:stalls="+Arrays.toString(s)+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,2,4,8,9},3,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,8,4,9},3,3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},2,4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},3,2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{10,1,9,3,7},3,3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,3,6,10,15},4,4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,4,9,16,25,36},3,15,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,100,200,300,400},4,100,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{2,10,18,26},3,8,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,4,8,16,32},4,7,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int maxDistance(vector<int>& stalls,int k){return 0;}};
// USER_CODE_END
void test(vector<int> s,int k,int e,int tc,bool hd=false){int r=CodeCoder().maxDistance(s,k);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,2,4,8,9},3,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,8,4,9},3,3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3,4,5},2,4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},3,2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({10,1,9,3,7},3,3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,3,6,10,15},4,4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,4,9,16,25,36},3,15,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,100,200,300,400},4,100,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({2,10,18,26},3,8,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,4,8,16,32},4,7,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maxDistance(self, stalls, k):
        return 0
# USER_CODE_END
def test(s,k,e,tc,hd=False):r=CodeCoder().maxDistance(s,k);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:stalls={s}:k={k}:exp={e}:got={r}"))
try:test([1,2,4,8,9],3,3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,8,4,9],3,3,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3,4,5],2,4,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5],3,2,4)
except:print("TC:4:FAIL:hidden")
try:test([10,1,9,3,7],3,3,5)
except:print("TC:5:FAIL:hidden")
try:test([1,3,6,10,15],4,4,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,4,9,16,25,36],3,15,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,100,200,300,400],4,100,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([2,10,18,26],3,8,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,4,8,16,32],4,7,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maxDistance(stalls, k) { return 0; }
// USER_CODE_END
function test(s,k,e,tc,hd){if(hd===undefined)hd=false;const r=maxDistance(s,k);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,2,4,8,9],3,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,8,4,9],3,3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3,4,5],2,4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],3,2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([10,1,9,3,7],3,3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,3,6,10,15],4,4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,4,9,16,25,36],3,15,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,100,200,300,400],4,100,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([2,10,18,26],3,8,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,4,8,16,32],4,7,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int maxDistance(int* stalls,int n,int k) {
    // Write your code here — binary search the max minimum distance
    return 0;
}
// USER_CODE_END

void runTest(int* s,int n,int k,int e,int tc,int hd){
    int r=maxDistance(s,n,k);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={1,2,4,8,9};runTest(t1,5,3,3,1,0);
    int t2[]={1,2,8,4,9};runTest(t2,5,3,3,2,0);
    int t3[]={1,2,3,4,5};runTest(t3,5,2,4,3,0);
    int t4[]={1,2,3,4,5};runTest(t4,5,3,2,4,0);
    int t5[]={10,1,9,3,7};runTest(t5,5,3,3,5,0);
    int t6[]={1,3,6,10,15};runTest(t6,5,4,4,6,1);
    int t7[]={1,4,9,16,25,36};runTest(t7,6,3,15,7,1);
    int t8[]={1,100,200,300,400};runTest(t8,5,4,100,8,1);
    int t9[]={2,10,18,26};runTest(t9,4,3,8,9,1);
    int t10[]={1,2,4,8,16,32};runTest(t10,6,4,7,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
