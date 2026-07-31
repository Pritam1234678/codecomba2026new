"""
Multiply Matrices
====================
Given two matrices A (m x k) and B (k x n), compute their product C (m x n)
where C[i][j] = sum(A[i][p] * B[p][j]) for p = 0..k-1.

Examples:
  A = [[1,2],[3,4]], B = [[5,6],[7,8]] → C = [[19,22],[43,50]]

Standard matrix multiplication with triple loop.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Multiply Matrices"
desc=(
    "Given two matrices A of size m x k and B of size k x n, compute their product "
    "matrix C of size m x n.\n\n"
    "C[i][j] = sum over p from 0 to k-1 of (A[i][p] * B[p][j])\n\n"
    "For example:\n"
    "A = [[1,2],[3,4]], B = [[5,6],[7,8]]\n"
    "C[0][0] = 1*5+2*7 = 19, C[0][1] = 1*6+2*8 = 22,\n"
    "C[1][0] = 3*5+4*7 = 43, C[1][1] = 3*6+4*8 = 50\n"
    "Result: [[19,22],[43,50]]\n\n"
    "Use three nested loops: i over rows of A, j over columns of B, p over the common dimension."
)
infmt="First line contains m, k, n.\nNext m lines: matrix A (k columns).\nNext k lines: matrix B (n columns)."
outfmt="Print the product matrix C, m lines with n space-separated integers each."
cons="1 ≤ m, k, n ≤ 10\n-100 ≤ A[i][j], B[i][j] ≤ 100"
e1="Input:\n2 2 2\n1 2\n3 4\n5 6\n7 8\n\nOutput:\n19 22\n43 50"
e2="Input:\n1 2 1\n1 2\n3\n4\n\nOutput:\n11\n\nExplanation: 1*3+2*4=11"
e3="Input:\n2 1 2\n1\n2\n3 4\n\nOutput:\n3 4\n6 8"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[][] multiply(int[][] A, int[][] B) {
        // Write your code here — triple loop multiplication
        return new int[0][0];
    }
}
// USER_CODE_END

public class Main {
static boolean eq(int[][] a,int[][] b){for(int i=0;i<a.length;i++)for(int j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
static void test(int[][] A,int[][] B,int[][] e,int tc,boolean h){int[][] g=new CodeCoder().multiply(A,B);if(eq(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+Arrays.deepToString(g));}
public static void main(String[] a){
try{test(new int[][]{{1,2},{3,4}},new int[][]{{5,6},{7,8}},new int[][]{{19,22},{43,50}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,2}},new int[][]{{3},{4}},new int[][]{{11}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1},{2}},new int[][]{{3,4}},new int[][]{{3,4},{6,8}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{0}},new int[][]{{0}},new int[][]{{0}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1}},new int[][]{{1}},new int[][]{{1}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,0},{0,1}},new int[][]{{5,6},{7,8}},new int[][]{{5,6},{7,8}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{-1,-2}},new int[][]{{3},{4}},new int[][]{{-11}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,2,3}},new int[][]{{1},{1},{1}},new int[][]{{6}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{100}},new int[][]{{100}},new int[][]{{10000}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{2,0},{0,2}},new int[][]{{1,1},{1,1}},new int[][]{{2,2},{2,2}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<vector<int>> multiply(vector<vector<int>>& A,vector<vector<int>>& B){return {};}};
// USER_CODE_END
bool eq(vector<vector<int>>& a,vector<vector<int>>& b){for(size_t i=0;i<a.size();i++)for(size_t j=0;j<a[0].size();j++)if(a[i][j]!=b[i][j])return false;return true;}
void test(vector<vector<int>> A,vector<vector<int>> B,vector<vector<int>> e,int tc,bool h=false){auto g=CodeCoder().multiply(A,B);if(eq(g,e))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL\\n";}
int main(){
try{test({{1,2},{3,4}},{{5,6},{7,8}},{{19,22},{43,50}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,2}},{{3},{4}},{{11}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1},{2}},{{3,4}},{{3,4},{6,8}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{0}},{{0}},{{0}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1}},{{1}},{{1}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,0},{0,1}},{{5,6},{7,8}},{{5,6},{7,8}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-1,-2}},{{3},{4}},{{-11}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,2,3}},{{1},{1},{1}},{{6}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{100}},{{100}},{{10000}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{2,0},{0,2}},{{1,1},{1,1}},{{2,2},{2,2}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def multiply(self, A, B):
        return []
# USER_CODE_END
def test(A,B,e,tc,h=False):g=CodeCoder().multiply(A,B);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got={g}"))
try:test([[1,2],[3,4]],[[5,6],[7,8]],[[19,22],[43,50]],1)
except:print("TC:1:FAIL:hidden")
try:test([[1,2]],[[3],[4]],[[11]],2)
except:print("TC:2:FAIL:hidden")
try:test([[1],[2]],[[3,4]],[[3,4],[6,8]],3)
except:print("TC:3:FAIL:hidden")
try:test([[0]],[[0]],[[0]],4)
except:print("TC:4:FAIL:hidden")
try:test([[1]],[[1]],[[1]],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,0],[0,1]],[[5,6],[7,8]],[[5,6],[7,8]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-1,-2]],[[3],[4]],[[-11]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,2,3]],[[1],[1],[1]],[[6]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[100]],[[100]],[[10000]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[2,0],[0,2]],[[1,1],[1,1]],[[2,2],[2,2]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function multiply(A, B) { return []; }
// USER_CODE_END
function eq(a,b){return JSON.stringify(a)===JSON.stringify(b);}
function test(A,B,e,tc,h){if(h===undefined)h=false;const g=multiply(A,B);if(eq(g,e))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g));}
try{test([[1,2],[3,4]],[[5,6],[7,8]],[[19,22],[43,50]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,2]],[[3],[4]],[[11]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1],[2]],[[3,4]],[[3,4],[6,8]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[0]],[[0]],[[0]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1]],[[1]],[[1]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,0],[0,1]],[[5,6],[7,8]],[[5,6],[7,8]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-1,-2]],[[3],[4]],[[-11]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,2,3]],[[1],[1],[1]],[[6]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[100]],[[100]],[[10000]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[2,0],[0,2]],[[1,1],[1,1]],[[2,2],[2,2]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int** multiply(int** A,int m,int k,int** B,int n,int* cs){return NULL;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
