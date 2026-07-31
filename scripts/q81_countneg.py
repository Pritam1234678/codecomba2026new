"""
Count Negative Numbers in a Sorted Matrix
==========================================
Given an m x n matrix grid sorted in non-increasing order both row-wise and
column-wise, return the number of negative numbers in grid.

Examples:
  grid = [[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]] -> 8
  grid = [[3,2],[1,0]]                                      -> 0

You can count in O(m + n) by walking the boundary from top-right: if the
current cell is negative, the whole column below is negative; otherwise move
left.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows m, cols n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Negative Numbers in a Sorted Matrix"
desc=(
    "Given an m x n matrix grid which is sorted in non-increasing order both "
    "row-wise and column-wise, return the number of negative numbers in grid.\n\n"
    "For example:\n"
    "grid = [[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]] -> 8\n"
    "grid = [[3,2],[1,0]] -> 0\n\n"
    "A simple O(m*n) scan works, but you can do it in O(m + n): start at the "
    "top-right cell. If it is negative, the entire column below it is negative, "
    "so add n - col and move down one row; otherwise move left one column. "
    "Repeat until you leave the grid."
)
infmt="First line contains m and n (rows and columns).\nThen m lines follow, each with n space-separated integers."
outfmt="Print the count of negative numbers in the matrix."
cons="1 ≤ m, n ≤ 100\n-100 ≤ grid[i][j] ≤ 100\nEach row and each column is sorted in non-increasing order."
e1="Input:\n4 4\n4 3 2 -1\n3 2 1 -1\n1 1 -1 -2\n-1 -1 -2 -3\n\nOutput:\n8"
e2="Input:\n2 2\n3 2\n1 0\n\nOutput:\n0"
e3="Input:\n2 3\n1 2 3\n4 5 6\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countNegatives(int[][] grid) {
        // Write your code here
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int e,int tc,boolean h){int r=new CodeCoder().countNegatives(g);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:grid="+Arrays.deepToString(g)+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[][]{{4,3,2,-1},{3,2,1,-1},{1,1,-1,-2},{-1,-1,-2,-3}},8,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{3,2},{1,0}},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2,3},{4,5,6}},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{-1,-2,-3},{-4,-5,-6}},6,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{5,-1},{4,-2}},2,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{7,6,5,4,3,2,1,-1}},1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{-1}},1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,0,0}},0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{2,1,0,-1,-2},{-3,-4,-5,-6,-7}},7,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{8,7,6},{5,4,3},{2,1,-1}},1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countNegatives(vector<vector<int>>& grid){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> g,int e,int tc,bool h=false){int r=CodeCoder().countNegatives(g);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({{4,3,2,-1},{3,2,1,-1},{1,1,-1,-2},{-1,-1,-2,-3}},8,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{3,2},{1,0}},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2,3},{4,5,6}},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{-1,-2,-3},{-4,-5,-6}},6,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{5,-1},{4,-2}},2,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{7,6,5,4,3,2,1,-1}},1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-1}},1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,0,0},{0,0,0}},0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{2,1,0,-1,-2},{-3,-4,-5,-6,-7}},7,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{8,7,6},{5,4,3},{2,1,-1}},1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countNegatives(self, grid):
        return 0
# USER_CODE_END
def test(g,e,tc,h=False):r=CodeCoder().countNegatives(g);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:grid={g}:exp={e}:got={r}"))
try:test([[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]],8,1)
except:print("TC:1:FAIL:hidden")
try:test([[3,2],[1,0]],0,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2,3],[4,5,6]],0,3)
except:print("TC:3:FAIL:hidden")
try:test([[-1,-2,-3],[-4,-5,-6]],6,4)
except:print("TC:4:FAIL:hidden")
try:test([[5,-1],[4,-2]],2,5)
except:print("TC:5:FAIL:hidden")
try:test([[7,6,5,4,3,2,1,-1]],1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-1]],1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,0,0],[0,0,0]],0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[2,1,0,-1,-2],[-3,-4,-5,-6,-7]],7,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[8,7,6],[5,4,3],[2,1,-1]],1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countNegatives(grid) { return 0; }
// USER_CODE_END
function test(g,e,tc,h){if(h===undefined)h=false;const r=countNegatives(g);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]],8,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[3,2],[1,0]],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2,3],[4,5,6]],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[-1,-2,-3],[-4,-5,-6]],6,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[5,-1],[4,-2]],2,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[7,6,5,4,3,2,1,-1]],1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-1]],1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,0,0],[0,0,0]],0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[2,1,0,-1,-2],[-3,-4,-5,-6,-7]],7,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[8,7,6],[5,4,3],[2,1,-1]],1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int countNegatives(int* arr,int m,int n) {
    // Write your code here — arr is the matrix flattened row-major (m rows, n cols)
    return 0;
}
// USER_CODE_END

void runTest(int* a,int m,int n,int e,int tc,int h){
    int r=countNegatives(a,m,n);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={4,3,2,-1,3,2,1,-1,1,1,-1,-2,-1,-1,-2,-3};runTest(t1,4,4,8,1,0);
    int t2[]={3,2,1,0};runTest(t2,2,2,0,2,0);
    int t3[]={1,2,3,4,5,6};runTest(t3,2,3,0,3,0);
    int t4[]={-1,-2,-3,-4,-5,-6};runTest(t4,2,3,6,4,0);
    int t5[]={5,-1,4,-2};runTest(t5,2,2,2,5,0);
    int t6[]={7,6,5,4,3,2,1,-1};runTest(t6,1,8,1,6,1);
    int t7[]={-1};runTest(t7,1,1,1,7,1);
    int t8[]={0,0,0,0,0,0};runTest(t8,2,3,0,8,1);
    int t9[]={2,1,0,-1,-2,-3,-4,-5,-6,-7};runTest(t9,2,5,7,9,1);
    int t10[]={8,7,6,5,4,3,2,1,-1};runTest(t10,3,3,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
