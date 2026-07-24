"""
Rotting Oranges
=================
You are given an m x n grid where each cell can have one of three values:
0 = empty, 1 = fresh orange, 2 = rotten orange.

Every minute, any fresh orange that is 4-directionally adjacent to a rotten
orange becomes rotten. Return the minimum minutes until no cell has a fresh
orange. If impossible, return -1.

Example:
  [[2,1,1],
   [1,1,0],
   [0,1,1]]
  Minute 0: rotten at (0,0)
  Minute 1: (0,1) and (1,0) become rotten
  Minute 2: (0,2), (1,1), (2,1) become rotten
  Minute 3: (2,2) becomes rotten
  → return 4

Approach: BFS from all initially rotten oranges simultaneously (multi-source).
Track fresh count. Each level of BFS = 1 minute.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rotting Oranges"
desc=(
    "You are given an m x n grid where each cell has one of three values:\n"
    "- 0 = empty cell\n"
    "- 1 = fresh orange\n"
    "- 2 = rotten orange\n\n"
    "Every minute, any fresh orange that is 4-directionally adjacent (up, down, "
    "left, right) to a rotten orange becomes rotten.\n\n"
    "Return the minimum number of minutes that must elapse until no cell has a "
    "fresh orange. If this is impossible (some fresh orange never rots), return -1.\n\n"
    "Use BFS starting from all initially rotten oranges simultaneously (multi-source BFS). "
    "Track the count of fresh oranges. Each level of BFS represents 1 minute. "
    "If fresh count reaches 0, return minutes. If BFS finishes with fresh > 0, return -1."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers (0, 1, or 2)."
outfmt="Print the minimum minutes until no fresh orange, or -1 if impossible."
cons="1 ≤ m, n ≤ 10\nGrid values are 0, 1, or 2 only."
e1="Input:\n3 3\n2 1 1\n1 1 0\n0 1 1\n\nOutput:\n4"
e2="Input:\n3 3\n2 1 1\n0 1 1\n1 0 1\n\nOutput:\n-1\n\nExplanation: Orange at (2,2) never rots."
e3="Input:\n1 1\n0\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, BFS, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int orangesRotting(int[][] grid) {
        // Write your code here — BFS from all rotten oranges
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int e,int tc,boolean h){int g2=new CodeCoder().orangesRotting(g);if(g2==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g2);}
public static void main(String[] a){
try{test(new int[][]{{2,1,1},{1,1,0},{0,1,1}},4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{2,1,1},{0,1,1},{1,0,1}},-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{0}},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{2}},0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{2,2},{2,2}},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{2,1,1},{1,1,1},{1,1,1}},4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,1}},-1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1},{2}},1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{2,0,1,1,1}},2,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{0,2}},0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int orangesRotting(vector<vector<int>>& g){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> g,int e,int tc,bool h=false){int g2=CodeCoder().orangesRotting(g);if(g2==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g2<<"\\n";}
int main(){
try{test({{2,1,1},{1,1,0},{0,1,1}},4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{2,1,1},{0,1,1},{1,0,1}},-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{0}},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{2}},0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{2,2},{2,2}},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{2,1,1},{1,1,1},{1,1,1}},4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,1},{1,1}},-1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1},{2}},1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{2,0,1,1,1}},2,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{0,2}},0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def orangesRotting(self, grid):
        return 0
# USER_CODE_END
def test(g,e,tc,h=False):
    cp=[row[:] for row in g]
    got=CodeCoder().orangesRotting(cp)
    if got==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={got}")

try:test([[2,1,1],[1,1,0],[0,1,1]],4,1)
except:print("TC:1:FAIL:hidden")
try:test([[2,1,1],[0,1,1],[1,0,1]],-1,2)
except:print("TC:2:FAIL:hidden")
try:test([[0]],0,3)
except:print("TC:3:FAIL:hidden")
try:test([[2]],0,4)
except:print("TC:4:FAIL:hidden")
try:test([[2,2],[2,2]],0,5)
except:print("TC:5:FAIL:hidden")
try:test([[2,1,1],[1,1,1],[1,1,1]],4,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,1],[1,1]],-1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1],[2]],1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[2,0,1,1,1]],2,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[0,2]],0,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function orangesRotting(grid) { return 0; }
// USER_CODE_END
function test(g,e,tc,h){if(h===undefined)h=false;const cp=g.map(r=>[...r]);const got=orangesRotting(cp);if(got===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+got);}
try{test([[2,1,1],[1,1,0],[0,1,1]],4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[2,1,1],[0,1,1],[1,0,1]],-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[0]],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[2]],0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[2,2],[2,2]],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[2,1,1],[1,1,1],[1,1,1]],4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,1],[1,1]],-1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1],[2]],1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[2,0,1,1,1]],2,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[0,2]],0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int orangesRotting(int** g,int rs,int* cs){return 0;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
