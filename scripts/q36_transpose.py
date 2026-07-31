"""
Transpose Matrix
==================
Given an m x n matrix, return its transpose — the matrix flipped over its
main diagonal. Transpose[i][j] = matrix[j][i]. Result is n x m.

Examples:
  matrix = [[1,2,3],[4,5,6]] → [[1,4],[2,5],[3,6]]
  matrix = [[1,2],[3,4]] → [[1,3],[2,4]]

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Transpose Matrix"
desc=(
    "Given an m x n matrix, return the transpose of the matrix.\n\n"
    "The transpose of a matrix is obtained by flipping it over its main diagonal, "
    "switching the row and column indices. The result has dimensions n x m, where "
    "result[i][j] = matrix[j][i].\n\n"
    "For example:\n"
    "matrix = [[1,2,3],[4,5,6]] → transpose = [[1,4],[2,5],[3,6]]\n"
    "matrix = [[1,2],[3,4]] → transpose = [[1,3],[2,4]]\n\n"
    "Create a new n x m result matrix and fill result[i][j] = matrix[j][i]."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers."
outfmt="Print the transposed matrix, n lines with m space-separated integers each."
cons="1 ≤ m, n ≤ 1000\n-10^9 ≤ matrix[i][j] ≤ 10^9"
e1="Input:\n2 3\n1 2 3\n4 5 6\n\nOutput:\n1 4\n2 5\n3 6"
e2="Input:\n2 2\n1 2\n3 4\n\nOutput:\n1 3\n2 4"
e3="Input:\n1 1\n5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[][] transpose(int[][] matrix) {
        // Write your code here — result[i][j] = matrix[j][i]
        return new int[0][0];
    }
}
// USER_CODE_END

public class Main {
static boolean eq(int[][] a,int[][] b){for(int i=0;i<a.length;i++)for(int j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
static void test(int[][] m,int[][] e,int tc,boolean h){int[][] g=new CodeCoder().transpose(m);if(eq(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+Arrays.deepToString(g));}
public static void main(String[] a){
try{test(new int[][]{{1,2,3},{4,5,6}},new int[][]{{1,4},{2,5},{3,6}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,2},{3,4}},new int[][]{{1,3},{2,4}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{5}},new int[][]{{5}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,2}},new int[][]{{1},{2}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1},{2}},new int[][]{{1,2}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4},{5,6,7,8}},new int[][]{{1,5},{2,6},{3,7},{4,8}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{-1,-2},{-3,-4}},new int[][]{{-1,-3},{-2,-4}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0},{0,0}},new int[][]{{0,0,0},{0,0,0}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,2,3}},new int[][]{{1},{2},{3}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{10}},new int[][]{{10}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<vector<int>> transpose(vector<vector<int>>& m){return {};}};
// USER_CODE_END
bool eq(vector<vector<int>>& a,vector<vector<int>>& b){for(size_t i=0;i<a.size();i++)for(size_t j=0;j<a[0].size();j++)if(a[i][j]!=b[i][j])return false;return true;}
void test(vector<vector<int>> m,vector<vector<int>> e,int tc,bool h=false){auto g=CodeCoder().transpose(m);if(eq(g,e))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL\\n";}
int main(){
try{test({{1,2,3},{4,5,6}},{{1,4},{2,5},{3,6}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,2},{3,4}},{{1,3},{2,4}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{5}},{{5}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,2}},{{1},{2}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1},{2}},{{1,2}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4},{5,6,7,8}},{{1,5},{2,6},{3,7},{4,8}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-1,-2},{-3,-4}},{{-1,-3},{-2,-4}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,0},{0,0},{0,0}},{{0,0,0},{0,0,0}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,2,3}},{{1},{2},{3}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{10}},{{10}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def transpose(self, matrix):
        return []
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().transpose(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got={g}"))
try:test([[1,2,3],[4,5,6]],[[1,4],[2,5],[3,6]],1)
except:print("TC:1:FAIL:hidden")
try:test([[1,2],[3,4]],[[1,3],[2,4]],2)
except:print("TC:2:FAIL:hidden")
try:test([[5]],[[5]],3)
except:print("TC:3:FAIL:hidden")
try:test([[1,2]],[[1],[2]],4)
except:print("TC:4:FAIL:hidden")
try:test([[1],[2]],[[1,2]],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4],[5,6,7,8]],[[1,5],[2,6],[3,7],[4,8]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-1,-2],[-3,-4]],[[-1,-3],[-2,-4]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,0],[0,0],[0,0]],[[0,0,0],[0,0,0]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,2,3]],[[1],[2],[3]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[10]],[[10]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function transpose(matrix) { return []; }
// USER_CODE_END
function eq(a,b){return JSON.stringify(a)===JSON.stringify(b);}
function test(m,e,tc,h){if(h===undefined)h=false;const g=transpose(m);if(eq(g,e))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g));}
try{test([[1,2,3],[4,5,6]],[[1,4],[2,5],[3,6]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,2],[3,4]],[[1,3],[2,4]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[5]],[[5]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,2]],[[1],[2]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1],[2]],[[1,2]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4],[5,6,7,8]],[[1,5],[2,6],[3,7],[4,8]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-1,-2],[-3,-4]],[[-1,-3],[-2,-4]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,0],[0,0],[0,0]],[[0,0,0],[0,0,0]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,2,3]],[[1],[2],[3]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[10]],[[10]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int** transpose(int** m,int rs,int cs,int* rrs,int* rcs){return NULL;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
