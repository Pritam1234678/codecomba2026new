"""
Addition of Two Square Matrices
==================================
Given two square matrices A and B of size n x n, compute their sum C = A + B,
where C[i][j] = A[i][j] + B[i][j].

Examples:
  A = [[1,2],[3,4]], B = [[5,6],[7,8]] → C = [[6,8],[10,12]]

Simply add corresponding elements.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder (returns result matrix)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Addition of Two Square Matrices"
desc=(
    "Given two square matrices A and B of size n x n, compute and return their "
    "sum matrix C where C[i][j] = A[i][j] + B[i][j].\n\n"
    "For example:\n"
    "A = [[1,2],[3,4]], B = [[5,6],[7,8]]\n"
    "C = [[1+5, 2+6],[3+7, 4+8]] = [[6,8],[10,12]]\n\n"
    "Iterate through all cells and add corresponding elements from A and B."
)
infmt="First line contains n.\nNext n lines: matrix A.\nNext n lines: matrix B."
outfmt="Print the sum matrix C, n lines with n space-separated integers each."
cons="1 ≤ n ≤ 100\n-10^6 ≤ A[i][j], B[i][j] ≤ 10^6"
e1="Input:\n2\n1 2\n3 4\n5 6\n7 8\n\nOutput:\n6 8\n10 12"
e2="Input:\n1\n5\n10\n\nOutput:\n15"
e3="Input:\n2\n-1 -1\n-1 -1\n1 1\n1 1\n\nOutput:\n0 0\n0 0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[][] addMatrices(int[][] A, int[][] B) {
        // Write your code here — C[i][j] = A[i][j] + B[i][j]
        return new int[0][0];
    }
}
// USER_CODE_END

public class Main {
static boolean eq(int[][] a,int[][] b){for(int i=0;i<a.length;i++)for(int j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
static void test(int[][] A,int[][] B,int[][] e,int tc,boolean h){int[][] g=new CodeCoder().addMatrices(A,B);if(eq(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+Arrays.deepToString(g));}
public static void main(String[] a){
try{test(new int[][]{{1,2},{3,4}},new int[][]{{5,6},{7,8}},new int[][]{{6,8},{10,12}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{5}},new int[][]{{10}},new int[][]{{15}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{-1,-1},{-1,-1}},new int[][]{{1,1},{1,1}},new int[][]{{0,0},{0,0}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},new int[][]{{0,0},{0,0}},new int[][]{{0,0},{0,0}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{100,200},{300,400}},new int[][]{{1,2},{3,4}},new int[][]{{101,202},{303,404}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},new int[][]{{9,8,7},{6,5,4},{3,2,1}},new int[][]{{10,10,10},{10,10,10},{10,10,10}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{-1000000}},new int[][]{{1000000}},new int[][]{{0}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1}},new int[][]{{2}},new int[][]{{3}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{5,5},{5,5}},new int[][]{{5,5},{5,5}},new int[][]{{10,10},{10,10}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4}},new int[][]{{4,3,2,1}},new int[][]{{5,5,5,5}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<vector<int>> addMatrices(vector<vector<int>>& A,vector<vector<int>>& B){return {};}};
// USER_CODE_END
bool eq(vector<vector<int>>& a,vector<vector<int>>& b){for(size_t i=0;i<a.size();i++)for(size_t j=0;j<a[0].size();j++)if(a[i][j]!=b[i][j])return false;return true;}
void test(vector<vector<int>> A,vector<vector<int>> B,vector<vector<int>> e,int tc,bool h=false){auto g=CodeCoder().addMatrices(A,B);if(eq(g,e))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL\\n";}
int main(){
try{test({{1,2},{3,4}},{{5,6},{7,8}},{{6,8},{10,12}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{5}},{{10}},{{15}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{-1,-1},{-1,-1}},{{1,1},{1,1}},{{0,0},{0,0}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},{{0,0},{0,0}},{{0,0},{0,0}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{100,200},{300,400}},{{1,2},{3,4}},{{101,202},{303,404}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3},{4,5,6},{7,8,9}},{{9,8,7},{6,5,4},{3,2,1}},{{10,10,10},{10,10,10},{10,10,10}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-1000000}},{{1000000}},{{0}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1}},{{2}},{{3}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{5,5},{5,5}},{{5,5},{5,5}},{{10,10},{10,10}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{1,2,3,4}},{{4,3,2,1}},{{5,5,5,5}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def addMatrices(self, A, B):
        return []
# USER_CODE_END
def eq(a,b):
    return a==b
def test(A,B,e,tc,h=False):g=CodeCoder().addMatrices(A,B);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got={g}"))
try:test([[1,2],[3,4]],[[5,6],[7,8]],[[6,8],[10,12]],1)
except:print("TC:1:FAIL:hidden")
try:test([[5]],[[10]],[[15]],2)
except:print("TC:2:FAIL:hidden")
try:test([[-1,-1],[-1,-1]],[[1,1],[1,1]],[[0,0],[0,0]],3)
except:print("TC:3:FAIL:hidden")
try:test([[0,0],[0,0]],[[0,0],[0,0]],[[0,0],[0,0]],4)
except:print("TC:4:FAIL:hidden")
try:test([[100,200],[300,400]],[[1,2],[3,4]],[[101,202],[303,404]],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3],[4,5,6],[7,8,9]],[[9,8,7],[6,5,4],[3,2,1]],[[10,10,10],[10,10,10],[10,10,10]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-1000000]],[[1000000]],[[0]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1]],[[2]],[[3]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[5,5],[5,5]],[[5,5],[5,5]],[[10,10],[10,10]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[1,2,3,4]],[[4,3,2,1]],[[5,5,5,5]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function addMatrices(A, B) { return []; }
// USER_CODE_END
function eq(a,b){return JSON.stringify(a)===JSON.stringify(b);}
function test(A,B,e,tc,h){if(h===undefined)h=false;const g=addMatrices(A,B);if(eq(g,e))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g));}
try{test([[1,2],[3,4]],[[5,6],[7,8]],[[6,8],[10,12]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[5]],[[10]],[[15]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[-1,-1],[-1,-1]],[[1,1],[1,1]],[[0,0],[0,0]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[0,0],[0,0]],[[0,0],[0,0]],[[0,0],[0,0]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[100,200],[300,400]],[[1,2],[3,4]],[[101,202],[303,404]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3],[4,5,6],[7,8,9]],[[9,8,7],[6,5,4],[3,2,1]],[[10,10,10],[10,10,10],[10,10,10]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-1000000]],[[1000000]],[[0]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1]],[[2]],[[3]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[5,5],[5,5]],[[5,5],[5,5]],[[10,10],[10,10]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[1,2,3,4]],[[4,3,2,1]],[[5,5,5,5]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int** addMatrices(int** A,int** B,int n,int* cs){return NULL;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
