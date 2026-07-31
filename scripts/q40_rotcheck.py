"""
Determine Whether Matrix Can Be Obtained by Rotation
======================================================
Given two n x n matrices mat and target, determine whether target can be
obtained by rotating mat by 0, 90, 180, or 270 degrees clockwise.

Examples:
  mat = [[0,1],[1,0]], target = [[1,0],[0,1]] → true (90° rotation)
  mat = [[0,1],[1,1]], target = [[1,0],[0,1]] → false (no rotation works)

Check all 4 rotations. A 90° clockwise rotation: target[i][j] = mat[n-1-j][i].

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Determine Whether Matrix Can Be Obtained by Rotation"
desc=(
    "Given two n x n binary matrices mat and target, determine whether target can "
    "be obtained by rotating mat by 0, 90, 180, or 270 degrees clockwise.\n\n"
    "For example:\n"
    "mat = [[0,1],[1,0]], target = [[1,0],[0,1]] → true (rotate mat by 90° clockwise)\n"
    "mat = [[0,1],[1,1]], target = [[1,0],[0,1]] → false\n\n"
    "A 90° clockwise rotation transforms cell (i,j) to (j, n-1-i). "
    "To check all 4 rotations, repeatedly rotate mat 90° and compare with target. "
    "If any rotation matches, return true."
)
infmt="First line contains n.\nNext n lines: matrix mat.\nNext n lines: matrix target."
outfmt="Print 'true' if target can be obtained by rotating mat, otherwise 'false'."
cons="1 ≤ n ≤ 10\nmat and target consist of 0s and 1s."
e1="Input:\n2\n0 1\n1 0\n1 0\n0 1\n\nOutput:\ntrue"
e2="Input:\n2\n0 1\n1 1\n1 0\n0 1\n\nOutput:\nfalse"
e3="Input:\n1\n1\n1\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean canBeRotated(int[][] mat, int[][] target) {
        // Write your code here — check 0/90/180/270 rotations
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,int[][] t,boolean e,int tc,boolean h){boolean g=new CodeCoder().canBeRotated(m,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{0,1},{1,0}},new int[][]{{1,0},{0,1}},true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{0,1},{1,1}},new int[][]{{1,0},{0,1}},false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1}},new int[][]{{1}},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{0}},new int[][]{{1}},false,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1,0},{0,0}},new int[][]{{0,0},{0,1}},true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,1,0},{0,0,0}},new int[][]{{0,0,0},{0,1,0},{0,0,0}},true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,1},{0,0}},new int[][]{{1,0},{1,0}},true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,0,1},{0,0,0},{1,0,1}},new int[][]{{1,0,1},{0,0,0},{1,0,1}},true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,1}},new int[][]{{0,0},{0,0}},false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{0,1,1},{1,0,1},{1,1,0}},new int[][]{{1,1,0},{1,0,1},{0,1,1}},true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool canBeRotated(vector<vector<int>>& m,vector<vector<int>>& t){return false;}};
// USER_CODE_END
void test(vector<vector<int>> m,vector<vector<int>> t,bool e,int tc,bool h=false){bool g=CodeCoder().canBeRotated(m,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test({{0,1},{1,0}},{{1,0},{0,1}},true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{0,1},{1,1}},{{1,0},{0,1}},false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1}},{{1}},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{0}},{{1}},false,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1,0},{0,0}},{{0,0},{0,1}},true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{0,0,0},{0,1,0},{0,0,0}},{{0,0,0},{0,1,0},{0,0,0}},true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,1},{0,0}},{{1,0},{1,0}},true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,0,1},{0,0,0},{1,0,1}},{{1,0,1},{0,0,0},{1,0,1}},true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,1},{1,1}},{{0,0},{0,0}},false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{0,1,1},{1,0,1},{1,1,0}},{{1,1,0},{1,0,1},{0,1,1}},true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def canBeRotated(self, mat, target):
        return False
# USER_CODE_END
def test(m,t,e,tc,h=False):g=CodeCoder().canBeRotated(m,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[0,1],[1,0]],[[1,0],[0,1]],True,1)
except:print("TC:1:FAIL:hidden")
try:test([[0,1],[1,1]],[[1,0],[0,1]],False,2)
except:print("TC:2:FAIL:hidden")
try:test([[1]],[[1]],True,3)
except:print("TC:3:FAIL:hidden")
try:test([[0]],[[1]],False,4)
except:print("TC:4:FAIL:hidden")
try:test([[1,0],[0,0]],[[0,0],[0,1]],True,5)
except:print("TC:5:FAIL:hidden")
try:test([[0,0,0],[0,1,0],[0,0,0]],[[0,0,0],[0,1,0],[0,0,0]],True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,1],[0,0]],[[1,0],[1,0]],True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,0,1],[0,0,0],[1,0,1]],[[1,0,1],[0,0,0],[1,0,1]],True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,1],[1,1]],[[0,0],[0,0]],False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[0,1,1],[1,0,1],[1,1,0]],[[1,1,0],[1,0,1],[0,1,1]],True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function canBeRotated(mat, target) { return false; }
// USER_CODE_END
function test(m,t,e,tc,h){if(h===undefined)h=false;const g=canBeRotated(m,t);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[0,1],[1,0]],[[1,0],[0,1]],true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[0,1],[1,1]],[[1,0],[0,1]],false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1]],[[1]],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[0]],[[1]],false,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1,0],[0,0]],[[0,0],[0,1]],true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[0,0,0],[0,1,0],[0,0,0]],[[0,0,0],[0,1,0],[0,0,0]],true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,1],[0,0]],[[1,0],[1,0]],true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,0,1],[0,0,0],[1,0,1]],[[1,0,1],[0,0,0],[1,0,1]],true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,1],[1,1]],[[0,0],[0,0]],false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[0,1,1],[1,0,1],[1,1,0]],[[1,1,0],[1,0,1],[0,1,1]],true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool canBeRotated(int** m,int** t,int n,int* cs){return false;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
