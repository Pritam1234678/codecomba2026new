"""
Minimize Max Distance to Gas Station
======================================
Given a sorted array stations of gas station positions on a line, and an
integer k, you may add k MORE gas stations anywhere along the line. Minimize
the maximum distance between adjacent gas stations (after adding the k new
ones) and return that minimized maximum distance (a double).

Examples:
  stations = [1,2,3,4,5,6,7,8,9,10], k = 9 -> 0.5
  stations = [1,5,10], k = 1 -> 4.0

Binary search the answer d in (0, max-gap]. For a candidate d, the number of
new stations needed in a gap of length len is ceil(len/d) - 1. Sum over all
gaps; if the total <= k, d is feasible (try smaller); else we need a larger d.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the stations array is passed with its length n: int* stations, int n, int k.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Minimize Max Distance to Gas Station"
desc=(
    "You are given a sorted integer array stations where stations[i] is the "
    "position of the i-th gas station on a number line, and an integer k. You "
    "may add k MORE gas stations anywhere on the line (at any real position). "
    "Your goal is to MINIMIZE the maximum distance between any two adjacent gas "
    "stations (after adding the k new stations). Return that minimized maximum "
    "distance as a double.\n\n"
    "For example:\n"
    "stations = [1,2,3,4,5,6,7,8,9,10], k = 9 -> 0.5\n"
    "stations = [1,5,10], k = 1 -> 4.0\n\n"
    "Binary search the answer d in (0, maxGap]. For a candidate d, each existing "
    "gap of length len needs max(0, ceil(len/d) - 1) extra stations. If the "
    "total extra stations needed across all gaps <= k, d is feasible (try a "
    "smaller d); otherwise d is too small. An answer within 1e-6 of the optimum "
    "is accepted."
)
infmt="First line contains n and k (number of existing stations and extra stations to add). Second line contains n space-separated positions (sorted ascending)."
outfmt="Print the minimized maximum distance (double, e.g. 2.5)."
cons="2 ≤ n ≤ 100\n1 ≤ k ≤ 10^6\n1 ≤ stations[i] ≤ 10^8\nstations is sorted ascending."
e1="Input:\n10 9\n1 2 3 4 5 6 7 8 9 10\n\nOutput:\n0.5"
e2="Input:\n3 1\n1 5 10\n\nOutput:\n4.0"
e3="Input:\n2 3\n1 10\n\nOutput:\n2.25"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search, Double",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public double minmaxGasDist(int[] stations, int k) {
        // Write your code here — binary search on the max distance
        return 0.0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] s,int k,double e,int tc,boolean hd){double r=new CodeCoder().minmaxGasDist(s,k);boolean ok=Math.abs(r-e)<=1e-4;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:stations="+Arrays.toString(s)+":k="+k+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},9,0.5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},4,0.5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,5,10},1,4.0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,5,10},2,2.5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,10},3,2.25,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,10,20,30},5,4.5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,100},10,9.0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},5,1.0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50,60,70,80,90,100},10,5.0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,50,100},4,16.666667,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:double minmaxGasDist(vector<int>& stations,int k){return 0.0;}};
// USER_CODE_END
void test(vector<int> s,int k,double e,int tc,bool hd=false){double r=CodeCoder().minmaxGasDist(s,k);bool ok=fabs(r-e)<=1e-4;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,2,3,4,5,6,7,8,9,10},9,0.5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},4,0.5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,5,10},1,4.0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,5,10},2,2.5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,10},3,2.25,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,10,20,30},5,4.5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,100},10,9.0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},5,1.0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,20,30,40,50,60,70,80,90,100},10,5.0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,50,100},4,16.666667,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minmaxGasDist(self, stations, k):
        return 0.0
# USER_CODE_END
def test(s,k,e,tc,hd=False):
    r=CodeCoder().minmaxGasDist(s,k);ok=abs(r-e)<=1e-4
    print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:stations={s}:k={k}:exp={e}:got={r}"))
try:test([1,2,3,4,5,6,7,8,9,10],9,0.5,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4,5],4,0.5,2)
except:print("TC:2:FAIL:hidden")
try:test([1,5,10],1,4.0,3)
except:print("TC:3:FAIL:hidden")
try:test([1,5,10],2,2.5,4)
except:print("TC:4:FAIL:hidden")
try:test([1,10],3,2.25,5)
except:print("TC:5:FAIL:hidden")
try:test([1,10,20,30],5,4.5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,100],10,9.0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],5,1.0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([10,20,30,40,50,60,70,80,90,100],10,5.0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,50,100],4,16.666667,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minmaxGasDist(stations, k) { return 0.0; }
// USER_CODE_END
function test(s,k,e,tc,hd){if(hd===undefined)hd=false;const r=minmaxGasDist(s,k);const ok=Math.abs(r-e)<=1e-4;if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,2,3,4,5,6,7,8,9,10],9,0.5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],4,0.5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,5,10],1,4.0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,5,10],2,2.5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,10],3,2.25,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,10,20,30],5,4.5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,100],10,9.0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],5,1.0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,20,30,40,50,60,70,80,90,100],10,5.0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,50,100],4,16.666667,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <math.h>

// USER_CODE_START
double minmaxGasDist(int* stations,int n,int k) {
    // Write your code here — return the minimized max distance
    return 0.0;
}
// USER_CODE_END

void runTest(int* s,int n,int k,double e,int tc,int hd){
    double r=minmaxGasDist(s,n,k);
    if(fabs(r-e)<=1e-4){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%.6f:got=%.6f\\n",tc,e,r);}
}
int main(){
    int t1[]={1,2,3,4,5,6,7,8,9,10};runTest(t1,10,9,0.5,1,0);
    int t2[]={1,2,3,4,5};runTest(t2,5,4,0.5,2,0);
    int t3[]={1,5,10};runTest(t3,3,1,4.0,3,0);
    int t4[]={1,5,10};runTest(t4,3,2,2.5,4,0);
    int t5[]={1,10};runTest(t5,2,3,2.25,5,0);
    int t6[]={1,10,20,30};runTest(t6,4,5,4.5,6,1);
    int t7[]={1,100};runTest(t7,2,10,9.0,7,1);
    int t8[]={1,2,3,4,5,6,7,8,9,10};runTest(t8,10,5,1.0,8,1);
    int t9[]={10,20,30,40,50,60,70,80,90,100};runTest(t9,10,10,5.0,9,1);
    int t10[]={1,50,100};runTest(t10,3,4,16.666667,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
