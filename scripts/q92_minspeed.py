"""
Minimum Speed to Arrive on Time
================================
Given a distance array dist (km per leg) and a float hour, find the minimum
positive INTEGER speed (km/h) so the whole trip finishes within hour hours.
Leg i takes dist[i] / speed hours, BUT because you can only depart on the hour,
each leg except the LAST is rounded UP to the next integer hour. The last leg
is exact (no waiting). If even an infinite speed cannot meet the deadline
(i.e. hour < number of legs - 1), return -1.

Examples:
  dist = [1,3,2], hour = 6   -> 1
  dist = [1,3,2], hour = 2.7 -> 3
  dist = [1,3,2], hour = 1.9 -> -1   (impossible)

Binary search the integer speed in [1, big]. For candidate s, total =
sum(ceil(dist[i]/s) for i < n-1) + dist[n-1]/s. If total <= hour, s is
feasible (try smaller); else too small.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C: int* dist, int n, double hour.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Minimum Speed to Arrive on Time"
desc=(
    "You are given a floating-point array dist where dist[i] is the distance (in "
    "kilometers) of the i-th leg of a trip. A car travelling at speed km/h takes "
    "dist[i] / speed hours for leg i. However, you can only depart a leg on the "
    "hour, so every leg EXCEPT the last is rounded UP (ceil) to whole hours; the "
    "last leg is taken exactly.\n\n"
    "Return the minimum positive INTEGER speed such that the whole trip finishes "
    "within hour hours. If it is impossible (even at infinite speed), return -1.\n\n"
    "For example:\n"
    "dist = [1,3,2], hour = 6   -> 1\n"
    "dist = [1,3,2], hour = 2.7 -> 3\n"
    "dist = [1,3,2], hour = 1.9 -> -1  (need at least 2 hours for 2 ceiling legs)\n\n"
    "Binary search the integer speed in [1, large]. For candidate s, total time = "
    "sum(ceil(dist[i]/s) for all but the last leg) + dist[last]/s. If total <= "
    "hour, s is feasible (try smaller); otherwise s is too small."
)
infmt="First line n and hour (hour may be fractional). Second line n space-separated distances."
outfmt="Print the minimum integer speed so the trip finishes within hour (or -1 if impossible)."
cons="1 ≤ n ≤ 10^5\n1 ≤ dist[i] ≤ 10^5\n1 ≤ hour ≤ 10^9\nReturn -1 when hour < n - 1."
e1="Input:\n3 6\n1 3 2\n\nOutput:\n1"
e2="Input:\n3 2.7\n1 3 2\n\nOutput:\n3"
e3="Input:\n3 1.9\n1 3 2\n\nOutput:\n-1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int minSpeed(int[] dist, double hour) {
        // Write your code here — binary search the minimum feasible integer speed
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] d,double h,int e,int tc,boolean hd){int r=new CodeCoder().minSpeed(d,h);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:dist="+Arrays.toString(d)+":hour="+h+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,3,2},6.0,1,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,3,2},2.7,3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,3,2},1.9,-1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,5,5},3.0,5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2,4,6,8},10.0,2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},5.0,1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,10},0.5,-1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{3,2,1},3.0,3,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{7,9,3,5},12.0,3,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100,100,100},5.0,100,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int minSpeed(vector<int>& dist,double hour){return 0;}};
// USER_CODE_END
void test(vector<int> d,double h,int e,int tc,bool hd=false){int r=CodeCoder().minSpeed(d,h);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,3,2},6.0,1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,3,2},2.7,3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,3,2},1.9,-1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,5,5},3.0,5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2,4,6,8},10.0,2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1,1},5.0,1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,10},0.5,-1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({3,2,1},3.0,3,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({7,9,3,5},12.0,3,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100,100,100},5.0,100,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def minSpeed(self, dist, hour):
        return 0
# USER_CODE_END
def test(d,h,e,tc,hd=False):r=CodeCoder().minSpeed(d,h);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:dist={d}:hour={h}:exp={e}:got={r}"))
try:test([1,3,2],6.0,1,1)
except:print("TC:1:FAIL:hidden")
try:test([1,3,2],2.7,3,2)
except:print("TC:2:FAIL:hidden")
try:test([1,3,2],1.9,-1,3)
except:print("TC:3:FAIL:hidden")
try:test([5,5,5],3.0,5,4)
except:print("TC:4:FAIL:hidden")
try:test([2,4,6,8],10.0,2,5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1,1],5.0,1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,10],0.5,-1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([3,2,1],3.0,3,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([7,9,3,5],12.0,3,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100,100,100],5.0,100,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function minSpeed(dist, hour) { return 0; }
// USER_CODE_END
function test(d,h,e,tc,hd){if(hd===undefined)hd=false;const r=minSpeed(d,h);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,3,2],6.0,1,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,3,2],2.7,3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,3,2],1.9,-1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,5,5],3.0,5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2,4,6,8],10.0,2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1,1],5.0,1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,10],0.5,-1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([3,2,1],3.0,3,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([7,9,3,5],12.0,3,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100,100,100],5.0,100,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int minSpeed(int* dist,int n,double hour) {
    // Write your code here — return the minimum feasible integer speed, or -1
    return 0;
}
// USER_CODE_END

void runTest(int* d,int n,double h,int e,int tc,int hd){
    int r=minSpeed(d,n,h);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={1,3,2};runTest(t1,3,6.0,1,1,0);
    int t2[]={1,3,2};runTest(t2,3,2.7,3,2,0);
    int t3[]={1,3,2};runTest(t3,3,1.9,-1,3,0);
    int t4[]={5,5,5};runTest(t4,3,3.0,5,4,0);
    int t5[]={2,4,6,8};runTest(t5,4,10.0,2,5,0);
    int t6[]={1,1,1,1,1};runTest(t6,5,5.0,1,6,1);
    int t7[]={10,10};runTest(t7,2,0.5,-1,7,1);
    int t8[]={3,2,1};runTest(t8,3,3.0,3,8,1);
    int t9[]={7,9,3,5};runTest(t9,4,12.0,3,9,1);
    int t10[]={100,100,100};runTest(t10,3,5.0,100,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
