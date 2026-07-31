"""
Koko Eating Bananas
====================
Koko has n piles of bananas; the i-th pile has piles[i] bananas. She can pick
an eating speed k (bananas/hour). Each hour she eats k bananas from one pile
(or the whole pile if it has fewer than k). The guards return in h hours. Find
the MINIMUM integer k so she can finish all bananas within h hours.

Examples:
  piles = [3,6,7,11], h = 8  -> 4
  piles = [30,11,23,4,20], h = 5 -> 30

Binary search on k in [1, max(piles)]. For a candidate k, total hours =
sum(ceil(pile / k)). If total <= h, k is feasible (try smaller); else k is too
small. ceil(pile/k) = (pile + k - 1) / k using integer division.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the piles array is passed with its length n: int* piles, int n, int h.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Koko Eating Bananas"
desc=(
    "Koko has n piles of bananas; the i-th pile has piles[i] bananas. She may "
    "choose any integer eating speed k (bananas per hour). Each hour she picks "
    "one pile and eats k bananas from it (or the whole pile if it has fewer "
    "than k). The guards come back in h hours. Return the MINIMUM integer k "
    "with which she can eat all the bananas within h hours.\n\n"
    "For example:\n"
    "piles = [3,6,7,11], h = 8       -> 4\n"
    "piles = [30,11,23,4,20], h = 5  -> 30\n\n"
    "Binary search the answer k in [1, max(piles)]. For a candidate k, total "
    "hours = sum(ceil(pile / k)) = sum((pile + k - 1) / k). If total <= h the "
    "speed is feasible (try a smaller k); otherwise k is too small. Runs in "
    "O(n * log(max(piles)))."
)
infmt="First line contains n (number of piles) and h (hours). Second line contains n space-separated pile sizes."
outfmt="Print the minimum integer eating speed k so all bananas are eaten within h hours."
cons="1 ≤ n ≤ 10^4\n1 ≤ piles[i] ≤ 10^9\n1 ≤ h ≤ 10^9 (h is always at least n, so a solution exists)."
e1="Input:\n4 8\n3 6 7 11\n\nOutput:\n4"
e2="Input:\n5 5\n30 11 23 4 20\n\nOutput:\n30"
e3="Input:\n5 5\n1 1 1 1 1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int minEatingSpeed(int[] piles, int h) {
        // Write your code here — binary search the minimum feasible k
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] p,int h,int e,int tc,boolean hd){int r=new CodeCoder().minEatingSpeed(p,h);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:piles="+Arrays.toString(p)+":h="+h+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{3,6,7,11},8,4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{30,11,23,4,20},5,30,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},5,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{10,20,30},6,10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{8,8,8,8},4,8,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{3,6,7,11},4,11,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8},8,8,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{10,10,10,10,10},10,5,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{2,3,4,5},5,4,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100},10,10,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int minEatingSpeed(vector<int>& piles,int h){return 0;}};
// USER_CODE_END
void test(vector<int> p,int h,int e,int tc,bool hd=false){int r=CodeCoder().minEatingSpeed(p,h);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({3,6,7,11},8,4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({30,11,23,4,20},5,30,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,1,1,1},5,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({10,20,30},6,10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({8,8,8,8},4,8,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({3,6,7,11},4,11,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8},8,8,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({10,10,10,10,10},10,5,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({2,3,4,5},5,4,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100},10,10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minEatingSpeed(self, piles, h):
        return 0
# USER_CODE_END
def test(p,h,e,tc,hd=False):r=CodeCoder().minEatingSpeed(p,h);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:piles={p}:h={h}:exp={e}:got={r}"))
try:test([3,6,7,11],8,4,1)
except:print("TC:1:FAIL:hidden")
try:test([30,11,23,4,20],5,30,2)
except:print("TC:2:FAIL:hidden")
try:test([1,1,1,1,1],5,1,3)
except:print("TC:3:FAIL:hidden")
try:test([10,20,30],6,10,4)
except:print("TC:4:FAIL:hidden")
try:test([8,8,8,8],4,8,5)
except:print("TC:5:FAIL:hidden")
try:test([3,6,7,11],4,11,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8],8,8,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,10,10,10,10],10,5,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([2,3,4,5],5,4,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100],10,10,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minEatingSpeed(piles, h) { return 0; }
// USER_CODE_END
function test(p,h,e,tc,hd){if(hd===undefined)hd=false;const r=minEatingSpeed(p,h);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([3,6,7,11],8,4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([30,11,23,4,20],5,30,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,1,1,1],5,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([10,20,30],6,10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([8,8,8,8],4,8,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([3,6,7,11],4,11,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8],8,8,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,10,10,10,10],10,5,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([2,3,4,5],5,4,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100],10,10,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int minEatingSpeed(int* piles,int n,int h) {
    // Write your code here — return the minimum feasible speed k
    return 0;
}
// USER_CODE_END

void runTest(int* p,int n,int h,int e,int tc,int hd){
    int r=minEatingSpeed(p,n,h);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={3,6,7,11};runTest(t1,4,8,4,1,0);
    int t2[]={30,11,23,4,20};runTest(t2,5,5,30,2,0);
    int t3[]={1,1,1,1,1};runTest(t3,5,5,1,3,0);
    int t4[]={10,20,30};runTest(t4,3,6,10,4,0);
    int t5[]={8,8,8,8};runTest(t5,4,4,8,5,0);
    int t6[]={3,6,7,11};runTest(t6,4,4,11,6,1);
    int t7[]={1,2,3,4,5,6,7,8};runTest(t7,8,8,8,7,1);
    int t8[]={10,10,10,10,10};runTest(t8,5,10,5,8,1);
    int t9[]={2,3,4,5};runTest(t9,4,5,4,9,1);
    int t10[]={100};runTest(t10,1,10,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
