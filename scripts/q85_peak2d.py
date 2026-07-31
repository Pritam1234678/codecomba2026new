"""
Find Peak Element II
=====================
A peak in an m x n matrix is an element strictly greater than all of its
neighbors (up, down, left, right). Borders are treated as -infinity, so an
edge/corner cell can be a peak. Return the position of ANY peak.

We encode the position as a single integer:  r * n + c
(e.g. row 1, col 1 in a 3-column matrix -> 1*3 + 1 = 4).

Efficient O(m * log n): binary search on columns. In the middle column, find
the row r with the maximum value. If the left neighbor (r, mid-1) is larger,
the peak must be in the left half; if the right neighbor (r, mid+1) is larger,
it is in the right half; otherwise (r, mid) is a peak.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows m, cols n.)
The harness decodes flat = r*n + c and checks it is a real peak, so ANY valid
peak answer passes.
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find Peak Element II"
desc=(
    "A peak in an m x n matrix is an element that is strictly greater than all "
    "of its neighbors (up, down, left, and right). The matrix borders are "
    "treated as -infinity, so an edge or corner cell can be a peak if it beats "
    "its in-bounds neighbors. Return the position of ANY peak element.\n\n"
    "Return the position encoded as a single integer:  r * n + c  (row index "
    "times the number of columns, plus the column index). For example, the "
    "peak at row 1, column 1 in a 3-column matrix is 1*3 + 1 = 4.\n\n"
    "For example:\n"
    "mat = [[1,4],[3,2]]      -> peak at (0,1) -> 0*2+1 = 1\n"
    "mat = [[10,20,15],[21,30,14],[7,16,32]] -> peak at (1,1) -> 1*3+1 = 4\n\n"
    "Use binary search on columns in O(m * log n): in the middle column find "
    "the row r with the maximum value. If mat[r][mid-1] > mat[r][mid] the peak "
    "is to the left; if mat[r][mid+1] > mat[r][mid] it is to the right; "
    "otherwise (r, mid) is a peak."
)
infmt="First line contains m and n. Then m lines follow, each with n space-separated integers."
outfmt="Print r * n + c where (r, c) is ANY peak position (strictly greater than its 4 neighbors; borders are -inf)."
cons="1 ≤ m, n ≤ 500\nAdjacent cells in the same row/column are distinct.\nBorders are treated as -infinity."
e1="Input:\n2 2\n1 4\n3 2\n\nOutput:\n1"
e2="Input:\n3 3\n10 20 15\n21 30 14\n7 16 32\n\nOutput:\n4"
e3="Input:\n1 1\n5\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int findPeakGrid(int[][] mat) {
        // Write your code here — return r*n + c of any peak
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int tc,boolean h){int flat=new CodeCoder().findPeakGrid(g);int m=g.length,n=g[0].length;int r=flat/n,c=flat%n;boolean ok=(r>=0&&r<m&&c>=0&&c<n);if(ok){int v=g[r][c];if(r>0&&v<=g[r-1][c])ok=false;if(ok&&r<m-1&&v<=g[r+1][c])ok=false;if(ok&&c>0&&v<=g[r][c-1])ok=false;if(ok&&c<n-1&&v<=g[r][c+1])ok=false;}if(ok)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:mat="+Arrays.deepToString(g)+":flat="+flat);}
public static void main(String[] a){
try{test(new int[][]{{1,4},{3,2}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{10,20,15},{21,30,14},{7,16,32}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{9,8,7},{6,5,4},{3,2,1}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{5}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4,5}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1},{2},{3},{4},{5}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{10,11,12,13},{9,8,7,6},{5,4,3,2}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,2,1},{3,9,4},{1,5,1}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{1,3,2,4,1},{5,2,6,1,3},{7,1,8,2,9},{2,4,3,1,5}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int findPeakGrid(vector<vector<int>>& mat){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> g,int tc,bool h=false){int flat=CodeCoder().findPeakGrid(g);int m=g.size(),n=g[0].size();int r=flat/n,c=flat%n;bool ok=(r>=0&&r<m&&c>=0&&c<n);if(ok){int v=g[r][c];if(r>0&&v<=g[r-1][c])ok=false;if(ok&&r<m-1&&v<=g[r+1][c])ok=false;if(ok&&c>0&&v<=g[r][c-1])ok=false;if(ok&&c<n-1&&v<=g[r][c+1])ok=false;}if(ok)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:flat="<<flat<<"\\n";}
int main(){
try{test({{1,4},{3,2}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{10,20,15},{21,30,14},{7,16,32}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2,3},{4,5,6},{7,8,9}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{9,8,7},{6,5,4},{3,2,1}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{5}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4,5}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1},{2},{3},{4},{5}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{10,11,12,13},{9,8,7,6},{5,4,3,2}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,2,1},{3,9,4},{1,5,1}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{1,3,2,4,1},{5,2,6,1,3},{7,1,8,2,9},{2,4,3,1,5}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def findPeakGrid(self, mat):
        return 0
# USER_CODE_END
def test(g,tc,h=False):
    flat=CodeCoder().findPeakGrid(g);m=len(g);n=len(g[0]);r=flat//n;c=flat%n
    ok=0<=r<m and 0<=c<n
    if ok:
        v=g[r][c]
        if r>0 and v<=g[r-1][c]: ok=False
        if ok and r<m-1 and v<=g[r+1][c]: ok=False
        if ok and c>0 and v<=g[r][c-1]: ok=False
        if ok and c<n-1 and v<=g[r][c+1]: ok=False
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:mat={g}:flat={flat}"))
try:test([[1,4],[3,2]],1)
except:print("TC:1:FAIL:hidden")
try:test([[10,20,15],[21,30,14],[7,16,32]],2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2,3],[4,5,6],[7,8,9]],3)
except:print("TC:3:FAIL:hidden")
try:test([[9,8,7],[6,5,4],[3,2,1]],4)
except:print("TC:4:FAIL:hidden")
try:test([[5]],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4,5]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1],[2],[3],[4],[5]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[10,11,12,13],[9,8,7,6],[5,4,3,2]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,2,1],[3,9,4],[1,5,1]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[1,3,2,4,1],[5,2,6,1,3],[7,1,8,2,9],[2,4,3,1,5]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function findPeakGrid(mat) { return 0; }
// USER_CODE_END
function test(g,tc,h){if(h===undefined)h=false;const flat=findPeakGrid(g);const m=g.length,n=g[0].length;const r=Math.floor(flat/n),c=flat%n;let ok=(r>=0&&r<m&&c>=0&&c<n);if(ok){const v=g[r][c];if(r>0&&v<=g[r-1][c])ok=false;if(ok&&r<m-1&&v<=g[r+1][c])ok=false;if(ok&&c>0&&v<=g[r][c-1])ok=false;if(ok&&c<n-1&&v<=g[r][c+1])ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:flat="+flat);}
try{test([[1,4],[3,2]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[10,20,15],[21,30,14],[7,16,32]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2,3],[4,5,6],[7,8,9]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[9,8,7],[6,5,4],[3,2,1]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[5]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4,5]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1],[2],[3],[4],[5]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[10,11,12,13],[9,8,7,6],[5,4,3,2]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,2,1],[3,9,4],[1,5,1]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[1,3,2,4,1],[5,2,6,1,3],[7,1,8,2,9],[2,4,3,1,5]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int findPeakGrid(int* arr,int m,int n) {
    // Write your code here — arr is the matrix flattened row-major (m rows, n cols)
    // Return r * n + c of any peak position.
    return 0;
}
// USER_CODE_END

void runTest(int* a,int m,int n,int tc,int h){
    int flat=findPeakGrid(a,m,n);
    int r=flat/n,c=flat%n;
    int ok=(r>=0&&r<m&&c>=0&&c<n);
    if(ok){
        int v=a[r*n+c];
        if(r>0&&v<=a[(r-1)*n+c])ok=0;
        if(ok&&r<m-1&&v<=a[(r+1)*n+c])ok=0;
        if(ok&&c>0&&v<=a[r*n+c-1])ok=0;
        if(ok&&c<n-1&&v<=a[r*n+c+1])ok=0;
    }
    if(ok){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:flat=%d\\n",tc,flat);}
}
int main(){
    int t1[]={1,4,3,2};runTest(t1,2,2,1,0);
    int t2[]={10,20,15,21,30,14,7,16,32};runTest(t2,3,3,2,0);
    int t3[]={1,2,3,4,5,6,7,8,9};runTest(t3,3,3,3,0);
    int t4[]={9,8,7,6,5,4,3,2,1};runTest(t4,3,3,4,0);
    int t5[]={5};runTest(t5,1,1,5,0);
    int t6[]={1,2,3,4,5};runTest(t6,1,5,6,1);
    int t7[]={1,2,3,4,5};runTest(t7,5,1,7,1);
    int t8[]={10,11,12,13,9,8,7,6,5,4,3,2};runTest(t8,3,4,8,1);
    int t9[]={1,2,1,3,9,4,1,5,1};runTest(t9,3,3,9,1);
    int t10[]={1,3,2,4,1,5,2,6,1,3,7,1,8,2,9,2,4,3,1,5};runTest(t10,4,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
