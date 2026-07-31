"""
Find Row With Maximum 1's
==========================
Given an m x n binary matrix, return the 0-indexed row number that contains the
maximum number of 1's. If several rows tie for the most 1's, return the
smallest row index.

Examples:
  mat = [[0,1],[1,0]]             -> 0  (both rows have 1 one, tie -> smaller index)
  mat = [[0,0,0],[0,1,1]]         -> 1  (row 1 has 2 ones)
  mat = [[0,0],[1,1],[0,0]]       -> 1  (row 1 has 2 ones)

Because each row is binary and sorted, you can also binary-search for the first
1 in a row, but a linear count per row in O(m*n) is fine.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows m, cols n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find Row With Maximum 1's"
desc=(
    "Given an m x n binary matrix mat (every cell is 0 or 1), return the "
    "0-indexed row number that contains the maximum number of 1's. If several "
    "rows have the same maximum count of 1's, return the smallest row index.\n\n"
    "For example:\n"
    "mat = [[0,1],[1,0]]       -> 0  (both rows have one 1; tie -> index 0)\n"
    "mat = [[0,0,0],[0,1,1]]   -> 1  (row 1 has two 1's)\n"
    "mat = [[0,0],[1,1],[0,0]] -> 1  (row 1 has two 1's)\n\n"
    "Count the 1's in each row and track the row with the highest count "
    "(preferring the earlier row on a tie). Since rows are binary/sorted you "
    "may also binary-search for the first 1, but an O(m*n) scan is acceptable."
)
infmt="First line contains m and n (rows and columns).\nThen m lines follow, each with n space-separated 0/1 integers."
outfmt="Print the 0-indexed row number with the most 1's (smallest index on a tie)."
cons="1 ≤ m, n ≤ 100\nmat[i][j] is 0 or 1."
e1="Input:\n2 2\n0 1\n1 0\n\nOutput:\n0"
e2="Input:\n2 3\n0 0 0\n0 1 1\n\nOutput:\n1"
e3="Input:\n3 2\n0 0\n1 1\n0 0\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int rowWithMaxOnes(int[][] mat) {
        // Write your code here — return the row index with the most 1's
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int e,int tc,boolean h){int r=new CodeCoder().rowWithMaxOnes(g);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:mat="+Arrays.deepToString(g)+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[][]{{0,1},{1,0}},0,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,1,1}},1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{0,0},{1,1},{0,0}},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,1,1},{1,1,0},{1,0,0}},0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1},{0},{1}},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{0,0,0,0},{0,0,0,1},{1,1,1,1}},2,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,0,0,0,0},{0,1,0,0,0},{0,0,1,0,0},{0,0,0,1,0},{0,0,0,0,1}},0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,1},{0,0}},0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{0,1,0,1,0,1},{1,1,1,1,0,0},{0,0,0,0,0,1}},1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int rowWithMaxOnes(vector<vector<int>>& mat){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> g,int e,int tc,bool h=false){int r=CodeCoder().rowWithMaxOnes(g);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({{0,1},{1,0}},0,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{0,0,0},{0,1,1}},1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{0,0},{1,1},{0,0}},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,1,1},{1,1,0},{1,0,0}},0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1},{0},{1}},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{0,0,0,0},{0,0,0,1},{1,1,1,1}},2,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,0,0,0,0},{0,1,0,0,0},{0,0,1,0,0},{0,0,0,1,0},{0,0,0,0,1}},0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,1},{1,1},{0,0}},0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{0,1,0,1,0,1},{1,1,1,1,0,0},{0,0,0,0,0,1}},1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rowWithMaxOnes(self, mat):
        return 0
# USER_CODE_END
def test(g,e,tc,h=False):r=CodeCoder().rowWithMaxOnes(g);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:mat={g}:exp={e}:got={r}"))
try:test([[0,1],[1,0]],0,1)
except:print("TC:1:FAIL:hidden")
try:test([[0,0,0],[0,1,1]],1,2)
except:print("TC:2:FAIL:hidden")
try:test([[0,0],[1,1],[0,0]],1,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,1,1],[1,1,0],[1,0,0]],0,4)
except:print("TC:4:FAIL:hidden")
try:test([[0,0],[0,0]],0,5)
except:print("TC:5:FAIL:hidden")
try:test([[1],[0],[1]],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[0,0,0,0],[0,0,0,1],[1,1,1,1]],2,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0],[0,0,0,0,1]],0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,1],[1,1],[0,0]],0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[0,1,0,1,0,1],[1,1,1,1,0,0],[0,0,0,0,0,1]],1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rowWithMaxOnes(mat) { return 0; }
// USER_CODE_END
function test(g,e,tc,h){if(h===undefined)h=false;const r=rowWithMaxOnes(g);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([[0,1],[1,0]],0,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[0,0,0],[0,1,1]],1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[0,0],[1,1],[0,0]],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,1,1],[1,1,0],[1,0,0]],0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[0,0],[0,0]],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1],[0],[1]],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[0,0,0,0],[0,0,0,1],[1,1,1,1]],2,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,0,0,0,0],[0,1,0,0,0],[0,0,1,0,0],[0,0,0,1,0],[0,0,0,0,1]],0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,1],[1,1],[0,0]],0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[0,1,0,1,0,1],[1,1,1,1,0,0],[0,0,0,0,0,1]],1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int rowWithMaxOnes(int* arr,int m,int n) {
    // Write your code here — arr is the matrix flattened row-major (m rows, n cols)
    return 0;
}
// USER_CODE_END

void runTest(int* a,int m,int n,int e,int tc,int h){
    int r=rowWithMaxOnes(a,m,n);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={0,1,1,0};runTest(t1,2,2,0,1,0);
    int t2[]={0,0,0,0,1,1};runTest(t2,2,3,1,2,0);
    int t3[]={0,0,1,1,0,0};runTest(t3,3,2,1,3,0);
    int t4[]={1,1,1,1,1,0,1,0,0};runTest(t4,3,3,0,4,0);
    int t5[]={0,0,0,0};runTest(t5,2,2,0,5,0);
    int t6[]={1,0,1};runTest(t6,3,1,0,6,1);
    int t7[]={0,0,0,0,0,0,0,1,1,1,1,1};runTest(t7,3,4,2,7,1);
    int t8[]={1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1};runTest(t8,5,5,0,8,1);
    int t9[]={1,1,1,1,0,0};runTest(t9,3,2,0,9,1);
    int t10[]={0,1,0,1,0,1,1,1,1,1,0,0,0,0,0,0,0,1};runTest(t10,3,6,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
