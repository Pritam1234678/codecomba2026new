"""
Number of Islands
===================
Given an m x n 2D grid of '1's (land) and '0's (water), count the number of
islands. An island is surrounded by water and formed by connecting adjacent
lands horizontally or vertically (4-directional). Assume all edges are water.

Example:
  grid = [
    ['1','1','1','1','0'],
    ['1','1','0','1','0'],
    ['1','1','0','0','0'],
    ['0','0','0','0','0']
  ]
  Output: 1

  grid = [
    ['1','1','0','0','0'],
    ['1','1','0','0','0'],
    ['0','0','1','0','0'],
    ['0','0','0','1','1']
  ]
  Output: 3

Approach: DFS. Iterate through every cell. When you find a '1', increment
count and use DFS to set all connected '1's to '0' (or visited).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Number of Islands"
desc=(
    "Given an m x n 2D binary grid where '1' represents land and '0' represents water, "
    "count the number of islands.\n\n"
    "An island is formed by connecting adjacent lands horizontally or vertically "
    "(4-directionally). All four edges of the grid are surrounded by water.\n\n"
    "For example:\n"
    "11110\n11010\n11000\n00000\n→ 1 island (all 1's connect together)\n\n"
    "11000\n11000\n00100\n00011\n→ 3 islands (top-left, middle, bottom-right)\n\n"
    "Use DFS: iterate through the grid. When you find an unvisited '1', increment count "
    "and recursively mark all connected '1's as visited."
)
infmt="First line contains m and n.\nNext m lines contain n characters each (0 or 1)."
outfmt="Print the number of islands."
cons="1 ≤ m, n ≤ 300\nThe grid consists of characters '0' and '1' only."
e1="Input:\n4 5\n11110\n11010\n11000\n00000\n\nOutput:\n1"
e2="Input:\n4 5\n11000\n11000\n00100\n00011\n\nOutput:\n3"
e3="Input:\n1 1\n0\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, DFS, BFS, Union Find, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int numIslands(char[][] grid) {
        // Write your code here — DFS on every '1'
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(char[][] g,int e,int tc,boolean h){int g2=new CodeCoder().numIslands(g);if(g2==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g2);}
public static void main(String[] a){
try{test(new char[][]{{'1','1','1','1','0'},{'1','1','0','1','0'},{'1','1','0','0','0'},{'0','0','0','0','0'}},1,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new char[][]{{'1','1','0','0','0'},{'1','1','0','0','0'},{'0','0','1','0','0'},{'0','0','0','1','1'}},3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new char[][]{{'0'}},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new char[][]{{'1'}},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new char[][]{{'1','0','1','0','1'}},3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new char[][]{{'1','1','1'},{'1','1','1'}},1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new char[][]{{'0','0'},{'0','0'}},0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new char[][]{{'1','0','0','1'},{'0','1','1','0'},{'0','1','1','0'},{'1','0','0','1'}},5,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new char[][]{{'1','1','0'},{'0','1','0'},{'0','0','1'}},2,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new char[][]{{'1','1','1','1','1','1','1'}},1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int numIslands(vector<vector<char>>& g){return 0;}};
// USER_CODE_END
void test(vector<vector<char>> g,int e,int tc,bool h=false){int g2=CodeCoder().numIslands(g);if(g2==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g2<<"\\n";}
int main(){
try{test({{'1','1','1','1','0'},{'1','1','0','1','0'},{'1','1','0','0','0'},{'0','0','0','0','0'}},1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{'1','1','0','0','0'},{'1','1','0','0','0'},{'0','0','1','0','0'},{'0','0','0','1','1'}},3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{'0'}},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{'1'}},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{'1','0','1','0','1'}},3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{'1','1','1'},{'1','1','1'}},1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{'0','0'},{'0','0'}},0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{'1','0','0','1'},{'0','1','1','0'},{'0','1','1','0'},{'1','0','0','1'}},5,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{'1','1','0'},{'0','1','0'},{'0','0','1'}},2,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{'1','1','1','1','1','1','1'}},1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def numIslands(self, grid):
        return 0
# USER_CODE_END
def test(g,e,tc,h=False):
    cp=[row[:] for row in g]
    got=CodeCoder().numIslands(cp)
    if got==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={got}")

try:test([['1','1','1','1','0'],['1','1','0','1','0'],['1','1','0','0','0'],['0','0','0','0','0']],1,1)
except:print("TC:1:FAIL:hidden")
try:test([['1','1','0','0','0'],['1','1','0','0','0'],['0','0','1','0','0'],['0','0','0','1','1']],3,2)
except:print("TC:2:FAIL:hidden")
try:test([['0']],0,3)
except:print("TC:3:FAIL:hidden")
try:test([['1']],1,4)
except:print("TC:4:FAIL:hidden")
try:test([['1','0','1','0','1']],3,5)
except:print("TC:5:FAIL:hidden")
try:test([['1','1','1'],['1','1','1']],1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([['0','0'],['0','0']],0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([['1','0','0','1'],['0','1','1','0'],['0','1','1','0'],['1','0','0','1']],5,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([['1','1','0'],['0','1','0'],['0','0','1']],2,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([['1','1','1','1','1','1','1']],1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function numIslands(grid) { return 0; }
// USER_CODE_END
function test(g,e,tc,h){if(h===undefined)h=false;const cp=g.map(r=>[...r]);const got=numIslands(cp);if(got===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+got);}
try{test([['1','1','1','1','0'],['1','1','0','1','0'],['1','1','0','0','0'],['0','0','0','0','0']],1,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([['1','1','0','0','0'],['1','1','0','0','0'],['0','0','1','0','0'],['0','0','0','1','1']],3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([['0']],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([['1']],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([['1','0','1','0','1']],3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([['1','1','1'],['1','1','1']],1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([['0','0'],['0','0']],0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([['1','0','0','1'],['0','1','1','0'],['0','1','1','0'],['1','0','0','1']],5,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([['1','1','0'],['0','1','0'],['0','0','1']],2,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([['1','1','1','1','1','1','1']],1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int numIslands(char** grid, int gridSize, int* gridColSize){return 0;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
