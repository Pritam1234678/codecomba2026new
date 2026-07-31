"""
Matrix Diagonal Sum
=====================
Given an n x n matrix, return the sum of all elements on the primary diagonal
(top-left to bottom-right) and secondary diagonal (top-right to bottom-left),
excluding the center element if it's counted twice.

Examples:
  matrix = [[1,2,3],
            [4,5,6],
            [7,8,9]] → 25 (1+5+9 + 3+5+7, but 5 counted once → 1+5+9+3+7 = 25)

Primary: mat[i][i]. Secondary: mat[i][n-1-i].
If n is odd, center counted once.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Matrix Diagonal Sum"
desc=(
    "Given a square matrix mat of size n x n, return the sum of all elements on "
    "both diagonals.\n\n"
    "Primary diagonal: elements mat[i][i] (top-left to bottom-right).\n"
    "Secondary diagonal: elements mat[i][n-1-i] (top-right to bottom-left).\n"
    "If n is odd, the center element belongs to both diagonals, so it should be "
    "counted only once.\n\n"
    "For example:\n"
    "matrix = [[1,2,3],[4,5,6],[7,8,9]]\n"
    "Primary: 1+5+9 = 15. Secondary: 3+5+7 = 15. Center 5 counted twice → 15+15-5 = 25.\n\n"
    "Iterate i from 0 to n-1, add mat[i][i] and mat[i][n-1-i]. If i == n-1-i, "
    "subtract the center once."
)
infmt="First line contains n.\nNext n lines each contain n space-separated integers."
outfmt="Print the diagonal sum."
cons="1 ≤ n ≤ 100\n0 ≤ mat[i][j] ≤ 100"
e1="Input:\n3\n1 2 3\n4 5 6\n7 8 9\n\nOutput:\n25"
e2="Input:\n1\n5\n\nOutput:\n5"
e3="Input:\n2\n1 2\n3 4\n\nOutput:\n10\n\nExplanation: 1+4+2+3=10"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int diagonalSum(int[][] mat) {
        // Write your code here — sum both diagonals, avoid double-count center
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,int e,int tc,boolean h){int g=new CodeCoder().diagonalSum(m);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},25,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{5}},5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2},{3,4}},10,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,1,1},{1,1,1},{1,1,1}},5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}},68,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{100}},100,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{7,8},{9,10}},34,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,0,0},{0,1,0},{0,0,1}},3,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2}},18,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int diagonalSum(vector<vector<int>>& mat){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> m,int e,int tc,bool h=false){int g=CodeCoder().diagonalSum(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({{1,2,3},{4,5,6},{7,8,9}},25,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{5}},5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2},{3,4}},10,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,1,1},{1,1,1},{1,1,1}},5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}},68,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{100}},100,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{7,8},{9,10}},34,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,0,0},{0,1,0},{0,0,1}},3,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2},{2,2,2,2,2}},18,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def diagonalSum(self, mat): return 0
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().diagonalSum(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[1,2,3],[4,5,6],[7,8,9]],25,1)
except:print("TC:1:FAIL:hidden")
try:test([[5]],5,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2],[3,4]],10,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,1,1],[1,1,1],[1,1,1]],5,4)
except:print("TC:4:FAIL:hidden")
try:test([[0,0],[0,0]],0,5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]],68,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[100]],100,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[7,8],[9,10]],34,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,0,0],[0,1,0],[0,0,1]],3,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2]],18,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function diagonalSum(mat) { return 0; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=diagonalSum(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[1,2,3],[4,5,6],[7,8,9]],25,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[5]],5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2],[3,4]],10,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,1,1],[1,1,1],[1,1,1]],5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[0,0],[0,0]],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]],68,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[100]],100,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[7,8],[9,10]],34,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,0,0],[0,1,0],[0,0,1]],3,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2],[2,2,2,2,2]],18,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int diagonalSum(int** mat,int n,int* cs){return 0;}
// USER_CODE_END
void run(int* rows[],int n,int e,int tc,int h){int csArr[10]={n};int* pcs=csArr;int g=diagonalSum(rows,n,pcs);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int r0a[]={1,2,3},r0b[]={4,5,6},r0c[]={7,8,9};int* m0[]={r0a,r0b,r0c};run(m0,3,25,1,0);
int r1a[]={5};int* m1[]={r1a};run(m1,1,5,2,0);
int r2a[]={1,2},r2b[]={3,4};int* m2[]={r2a,r2b};run(m2,2,10,3,0);
int r3a[]={1,1,1},r3b[]={1,1,1},r3c[]={1,1,1};int* m3[]={r3a,r3b,r3c};run(m3,3,5,4,0);
int r4a[]={0,0},r4b[]={0,0};int* m4[]={r4a,r4b};run(m4,2,0,5,0);
int r5a[]={1,2,3,4},r5b[]={5,6,7,8},r5c[]={9,10,11,12},r5d[]={13,14,15,16};int* m5[]={r5a,r5b,r5c,r5d};run(m5,4,68,6,1);
int r6a[]={100};int* m6[]={r6a};run(m6,1,100,7,1);
int r7a[]={7,8},r7b[]={9,10};int* m7[]={r7a,r7b};run(m7,2,34,8,1);
int r8a[]={1,0,0},r8b[]={0,1,0},r8c[]={0,0,1};int* m8[]={r8a,r8b,r8c};run(m8,3,3,9,1);
int r9a[]={2,2,2,2,2},r9b[]={2,2,2,2,2},r9c[]={2,2,2,2,2},r9d[]={2,2,2,2,2},r9e[]={2,2,2,2,2};int* m9[]={r9a,r9b,r9c,r9d,r9e};run(m9,5,18,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
