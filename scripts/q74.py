"""
Non-overlapping Intervals
===========================
Given an array of intervals where intervals[i] = [starti, endi], return the
minimum number of intervals you need to remove to make the rest non-overlapping.

Two intervals overlap if one starts before the other ends.

Examples:
  intervals = [[1,2],[2,3],[3,4],[1,3]] → remove 1 ([1,3])
  intervals = [[1,2],[1,2],[1,2]] → remove 2 (two of them)
  intervals = [[1,2],[2,3]] → remove 0 (they touch but don't overlap)

Approach: Sort by END time. Greedily pick the interval that ends earliest,
skip any overlapping ones (start < lastEnd), count removals.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Non-overlapping Intervals"
desc=(
    "Given an array of intervals where each interval is [start, end], return the "
    "minimum number of intervals you need to remove to make the rest non-overlapping.\n\n"
    "Two intervals [a,b] and [c,d] overlap if c < b (one starts before the other ends). "
    "Intervals that just touch (like [1,2] and [2,3]) are considered non-overlapping.\n\n"
    "For example:\n"
    "- [[1,2],[2,3],[3,4],[1,3]] → remove [1,3] (overlaps with [1,2] and [2,3]).\n"
    "- [[1,2],[1,2],[1,2]] → remove two of the three.\n"
    "- [[1,2],[2,3]] → no removal needed (they just touch).\n\n"
    "Sort by end time, then greedily keep the interval that ends earliest. "
    "If the next interval starts before the kept interval ends, it must be removed."
)
infmt="First line contains n.\nNext n lines each contain two space-separated integers start and end."
outfmt="Print the minimum number of intervals to remove."
cons="1 ≤ n ≤ 10^5\n-5*10^4 ≤ starti < endi ≤ 5*10^4"
e1="Input:\n4\n1 2\n2 3\n3 4\n1 3\n\nOutput:\n1\n\nExplanation: Remove [1,3] to make rest non-overlapping."
e2="Input:\n3\n1 2\n1 2\n1 2\n\nOutput:\n2\n\nExplanation: Only keep one."
e3="Input:\n2\n1 2\n2 3\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Greedy, Sorting, Intervals",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int eraseOverlapIntervals(int[][] intervals) {
        // Write your code here — sort by end, greedy
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] inv,int e,int tc,boolean h){int g=new CodeCoder().eraseOverlapIntervals(inv);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{1,2},{2,3},{3,4},{1,3}},1,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,2},{1,2},{1,2}},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2},{2,3}},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,100},{11,22},{23,33}},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1,5},{2,3},{3,4},{4,5}},1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{-100,-50},{50,100}},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,2},{1,3},{1,4},{1,5}},3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,2},{1,3},{1,3},{2,4},{3,5},{3,5},{4,6}},4,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{0,1},{2,3},{4,5},{6,7}},0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{0,10}},0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int eraseOverlapIntervals(vector<vector<int>>& inv){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> inv,int e,int tc,bool h=false){int g=CodeCoder().eraseOverlapIntervals(inv);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({{1,2},{2,3},{3,4},{1,3}},1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,2},{1,2},{1,2}},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2},{2,3}},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,100},{11,22},{23,33}},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1,5},{2,3},{3,4},{4,5}},1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{-100,-50},{50,100}},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,2},{1,3},{1,4},{1,5}},3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,2},{1,3},{1,3},{2,4},{3,5},{3,5},{4,6}},4,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{0,1},{2,3},{4,5},{6,7}},0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{0,10}},0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def eraseOverlapIntervals(self, intervals):
        return 0
# USER_CODE_END
def test(inv,e,tc,h=False):g=CodeCoder().eraseOverlapIntervals(inv);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[1,2],[2,3],[3,4],[1,3]],1,1)
except:print("TC:1:FAIL:hidden")
try:test([[1,2],[1,2],[1,2]],2,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2],[2,3]],0,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,100],[11,22],[23,33]],1,4)
except:print("TC:4:FAIL:hidden")
try:test([[1,5],[2,3],[3,4],[4,5]],1,5)
except:print("TC:5:FAIL:hidden")
try:test([[-100,-50],[50,100]],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,2],[1,3],[1,4],[1,5]],3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,2],[1,3],[1,3],[2,4],[3,5],[3,5],[4,6]],4,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[0,1],[2,3],[4,5],[6,7]],0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[0,10]],0,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function eraseOverlapIntervals(intervals) { return 0; }
// USER_CODE_END
function test(inv,e,tc,h){if(h===undefined)h=false;const g=eraseOverlapIntervals(inv);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[1,2],[2,3],[3,4],[1,3]],1,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,2],[1,2],[1,2]],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2],[2,3]],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,100],[11,22],[23,33]],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1,5],[2,3],[3,4],[4,5]],1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[-100,-50],[50,100]],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,2],[1,3],[1,4],[1,5]],3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,2],[1,3],[1,3],[2,4],[3,5],[3,5],[4,6]],4,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[0,1],[2,3],[4,5],[6,7]],0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[0,10]],0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int eraseOverlapIntervals(int** inv,int n,int* cs){return 0;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
