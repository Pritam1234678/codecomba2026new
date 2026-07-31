"""
Minimum Number of Days to Make m Bouquets
==========================================
Given an integer array bloomDay, and integers m and k. You want to make m
bouquets; each bouquet needs exactly k ADJACENT (consecutive) flowers that
have bloomed. On day d, flower i has bloomed if bloomDay[i] <= d. Return the
minimum day number to wait to form m bouquets, or -1 if it is impossible
(i.e. when m * k > number of flowers).

Examples:
  bloomDay = [1,10,3,10,2], m = 3, k = 1 -> 3
  bloomDay = [1,10,3,10,2], m = 3, k = 2 -> -1  (only 5 flowers, need 6)

Greedy feasibility + binary search: first check m*k <= n. Binary search the
day in [1, max(bloomDay)]. For a candidate day d, scan the array counting
consecutive bloomed flowers; from a run of length L you can form L // k
bouquets. If total bouquets >= m, the day is feasible (try smaller); else you
need a later day.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Minimum Number of Days to Make m Bouquets"
desc=(
    "Given an integer array bloomDay and integers m and k. You want to make m "
    "bouquets, each requiring exactly k ADJACENT (consecutive) flowers that "
    "have bloomed. On day d a flower i has bloomed if bloomDay[i] <= d. Return "
    "the minimum day number to wait so you can form m bouquets, or -1 if it is "
    "impossible.\n\n"
    "For example:\n"
    "bloomDay = [1,10,3,10,2], m = 3, k = 1 -> 3\n"
    "bloomDay = [1,10,3,10,2], m = 3, k = 2 -> -1  (only 5 flowers but 3*2=6 needed)\n\n"
    "Approach: first if m*k > len(bloomDay) return -1. Otherwise binary search "
    "the answer day in [1, max(bloomDay)]. For a candidate day d scan the array "
    "and count consecutive bloomed flowers in runs; from a run of length L you "
    "can form floor(L / k) bouquets. Sum them and if total >= m the day is "
    "feasible (try a smaller day), otherwise the day is too early. Runs in "
    "O(n * log(max(bloomDay)))."
)
infmt="First line contains n, m and k (number of flowers, bouquets needed, flowers per bouquet).\nSecond line contains n space-separated bloomDay values."
outfmt="Print the minimum day to form m bouquets, or -1 if impossible (m*k > n)."
cons="1 ≤ n ≤ 10^4\n1 ≤ m, k ≤ 10^3\n1 ≤ bloomDay[i] ≤ 10^9"
e1="Input:\n5 3 1\n1 10 3 10 2\n\nOutput:\n3"
e2="Input:\n5 3 2\n1 10 3 10 2\n\nOutput:\n-1"
e3="Input:\n3 1 2\n1 4 2\n\nOutput:\n4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int minDays(int[] bloomDay, int m, int k) {
        // Write your code here — binary search the minimum feasible day
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] b,int m,int k,int e,int tc,boolean hd){int r=new CodeCoder().minDays(b,m,k);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:bloom="+Arrays.toString(b)+":m="+m+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,10,3,10,2},3,1,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,10,3,10,2},3,2,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,4,2},1,2,4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,5,5,5},2,2,5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},3,2,6,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},3,1,3,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},3,1,30,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{7,7,7,7,15,15},2,2,7,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},1,3,3,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},5,2,10,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int minDays(vector<int>& bloomDay,int m,int k){return 0;}};
// USER_CODE_END
void test(vector<int> b,int m,int k,int e,int tc,bool hd=false){int r=CodeCoder().minDays(b,m,k);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,10,3,10,2},3,1,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,10,3,10,2},3,2,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,4,2},1,2,4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,5,5,5},2,2,5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},3,2,6,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},3,1,3,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40,50},3,1,30,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({7,7,7,7,15,15},2,2,7,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,4,3,2,1},1,3,3,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},5,2,10,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minDays(self, bloomDay, m, k):
        return 0
# USER_CODE_END
def test(b,m,k,e,tc,hd=False):r=CodeCoder().minDays(b,m,k);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:bloom={b}:m={m}:k={k}:exp={e}:got={r}"))
try:test([1,10,3,10,2],3,1,3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,10,3,10,2],3,2,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([1,4,2],1,2,4,3)
except:print("TC:3:FAIL:hidden")
try:test([5,5,5,5],2,2,5,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],3,2,6,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],3,1,3,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,20,30,40,50],3,1,30,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([7,7,7,7,15,15],2,2,7,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,4,3,2,1],1,3,3,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],5,2,10,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minDays(bloomDay, m, k) { return 0; }
// USER_CODE_END
function test(b,m,k,e,tc,hd){if(hd===undefined)hd=false;const r=minDays(b,m,k);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,10,3,10,2],3,1,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,10,3,10,2],3,2,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,4,2],1,2,4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,5,5,5],2,2,5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],3,2,6,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],3,1,3,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40,50],3,1,30,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([7,7,7,7,15,15],2,2,7,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,4,3,2,1],1,3,3,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],5,2,10,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int minDays(int* bloomDay,int n,int m,int k) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* b,int n,int m,int k,int e,int tc,int h){
    int r=minDays(b,n,m,k);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={1,10,3,10,2};runTest(t1,5,3,1,3,1,0);
    int t2[]={1,10,3,10,2};runTest(t2,5,3,2,-1,2,0);
    int t3[]={1,4,2};runTest(t3,3,1,2,4,3,0);
    int t4[]={5,5,5,5};runTest(t4,4,2,2,5,4,0);
    int t5[]={1,2,3,4,5,6,7,8,9,10};runTest(t5,10,3,2,6,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9,10};runTest(t6,10,3,1,3,6,1);
    int t7[]={10,20,30,40,50};runTest(t7,5,3,1,30,7,1);
    int t8[]={7,7,7,7,15,15};runTest(t8,6,2,2,7,8,1);
    int t9[]={5,4,3,2,1};runTest(t9,5,1,3,3,9,1);
    int t10[]={1,2,3,4,5,6,7,8,9,10};runTest(t10,10,5,2,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
