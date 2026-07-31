"""
Rotate Matrix (90°)
=====================
Given an n x n matrix, rotate it 90 degrees clockwise in-place.

Examples:
  matrix = [[1,2,3],[4,5,6],[7,8,9]] → [[7,4,1],[8,5,2],[9,6,3]]
  matrix = [[5,1,9,11],[2,4,8,10],[13,3,6,7],[15,14,12,16]] → [[15,13,2,5],[14,3,4,1],[12,6,8,9],[16,7,10,11]]

Approach: transpose then reverse each row.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rotate Matrix (90\u00b0)"
desc=(
    "You are given an n x n 2D matrix representing an image. Rotate the image by "
    "90 degrees clockwise and return the rotated matrix.\n\n"
    "For example:\n"
    "matrix = [[1,2,3],[4,5,6],[7,8,9]]\n"
    "Rotated 90\u00b0 clockwise: [[7,4,1],[8,5,2],[9,6,3]]\n\n"
    "A 90\u00b0 clockwise rotation transforms cell (i,j) to (j, n-1-i).\n\n"
    "Simple approach: first compute the transpose of the matrix (swap mat[i][j] "
    "with mat[j][i]), then reverse each row."
)
infmt="First line contains n.\nNext n lines each contain n space-separated integers."
outfmt="Print the rotated matrix, n lines with n space-separated integers each."
cons="1 ≤ n ≤ 20\n-1000 ≤ matrix[i][j] ≤ 1000"
e1="Input:\n3\n1 2 3\n4 5 6\n7 8 9\n\nOutput:\n7 4 1\n8 5 2\n9 6 3"
e2="Input:\n1\n1\n\nOutput:\n1"
e3="Input:\n2\n1 2\n3 4\n\nOutput:\n3 1\n4 2"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[][] rotate90(int[][] matrix) {
        // Write your code here — transpose then reverse rows
        return matrix;
    }
}
// USER_CODE_END

public class Main {
static boolean eq(int[][] a,int[][] b){for(int i=0;i<a.length;i++)for(int j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
static void test(int[][] m,int[][] e,int tc,boolean h){int[][] g=new CodeCoder().rotate90(m);if(eq(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+Arrays.deepToString(g));}
public static void main(String[] a){
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},new int[][]{{7,4,1},{8,5,2},{9,6,3}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1}},new int[][]{{1}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2},{3,4}},new int[][]{{3,1},{4,2}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,1,1},{2,2,2},{3,3,3}},new int[][]{{3,2,1},{3,2,1},{3,2,1}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{-1,-2},{-3,-4}},new int[][]{{-3,-1},{-4,-2}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}},new int[][]{{13,9,5,1},{14,10,6,2},{15,11,7,3},{16,12,8,4}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{5,1,9,11},{2,4,8,10},{13,3,6,7},{15,14,12,16}},new int[][]{{15,13,2,5},{14,3,4,1},{12,6,8,9},{16,7,10,11}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},new int[][]{{0,0},{0,0}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,2},{2,1}},new int[][]{{2,1},{1,2}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{100,200,300},{400,500,600},{700,800,900}},new int[][]{{700,400,100},{800,500,200},{900,600,300}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<vector<int>> rotate90(vector<vector<int>>& m){return m;}};
// USER_CODE_END
bool eq(vector<vector<int>>& a,vector<vector<int>>& b){for(size_t i=0;i<a.size();i++)for(size_t j=0;j<a[0].size();j++)if(a[i][j]!=b[i][j])return false;return true;}
void test(vector<vector<int>> m,vector<vector<int>> e,int tc,bool h=false){auto g=CodeCoder().rotate90(m);if(eq(g,e))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL\\n";}
int main(){
try{test({{1,2,3},{4,5,6},{7,8,9}},{{7,4,1},{8,5,2},{9,6,3}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1}},{{1}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2},{3,4}},{{3,1},{4,2}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,1,1},{2,2,2},{3,3,3}},{{3,2,1},{3,2,1},{3,2,1}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{-1,-2},{-3,-4}},{{-3,-1},{-4,-2}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}},{{13,9,5,1},{14,10,6,2},{15,11,7,3},{16,12,8,4}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{5,1,9,11},{2,4,8,10},{13,3,6,7},{15,14,12,16}},{{15,13,2,5},{14,3,4,1},{12,6,8,9},{16,7,10,11}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},{{0,0},{0,0}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,2},{2,1}},{{2,1},{1,2}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{100,200,300},{400,500,600},{700,800,900}},{{700,400,100},{800,500,200},{900,600,300}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rotate90(self, matrix):
        return matrix
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().rotate90(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got={g}"))
try:test([[1,2,3],[4,5,6],[7,8,9]],[[7,4,1],[8,5,2],[9,6,3]],1)
except:print("TC:1:FAIL:hidden")
try:test([[1]],[[1]],2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2],[3,4]],[[3,1],[4,2]],3)
except:print("TC:3:FAIL:hidden")
try:test([[1,1,1],[2,2,2],[3,3,3]],[[3,2,1],[3,2,1],[3,2,1]],4)
except:print("TC:4:FAIL:hidden")
try:test([[-1,-2],[-3,-4]],[[-3,-1],[-4,-2]],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]],[[13,9,5,1],[14,10,6,2],[15,11,7,3],[16,12,8,4]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[5,1,9,11],[2,4,8,10],[13,3,6,7],[15,14,12,16]],[[15,13,2,5],[14,3,4,1],[12,6,8,9],[16,7,10,11]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,0],[0,0]],[[0,0],[0,0]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,2],[2,1]],[[2,1],[1,2]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[100,200,300],[400,500,600],[700,800,900]],[[700,400,100],[800,500,200],[900,600,300]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rotate90(matrix) { return matrix; }
// USER_CODE_END
function eq(a,b){return JSON.stringify(a)===JSON.stringify(b);}
function test(m,e,tc,h){if(h===undefined)h=false;const g=rotate90(m);if(eq(g,e))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g));}
try{test([[1,2,3],[4,5,6],[7,8,9]],[[7,4,1],[8,5,2],[9,6,3]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1]],[[1]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2],[3,4]],[[3,1],[4,2]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,1,1],[2,2,2],[3,3,3]],[[3,2,1],[3,2,1],[3,2,1]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[-1,-2],[-3,-4]],[[-3,-1],[-4,-2]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]],[[13,9,5,1],[14,10,6,2],[15,11,7,3],[16,12,8,4]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[5,1,9,11],[2,4,8,10],[13,3,6,7],[15,14,12,16]],[[15,13,2,5],[14,3,4,1],[12,6,8,9],[16,7,10,11]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,0],[0,0]],[[0,0],[0,0]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,2],[2,1]],[[2,1],[1,2]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[100,200,300],[400,500,600],[700,800,900]],[[700,400,100],[800,500,200],[900,600,300]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int** rotate90(int** m,int n,int* cs) {
    // Write your code here — return the rotated matrix
    *cs=n;return NULL;
}
// USER_CODE_END

int eq(int** a,int** b,int n){for(int i=0;i<n;i++)for(int j=0;j<n;j++)if(a[i][j]!=b[i][j])return 0;return 1;}
void runTest(int** m,int n,int** e,int tc,int h){
    int cs;int** g=rotate90(m,n,&cs);
    if(g&&cs==n&&eq(g,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}
}
int main(){
    int m0a[]={1,2,3},m0b[]={4,5,6},m0c[]={7,8,9};
    int e0a[]={7,4,1},e0b[]={8,5,2},e0c[]={9,6,3};
    int* m0[]={m0a,m0b,m0c};int* e0[]={e0a,e0b,e0c};
    runTest(m0,3,e0,1,0);

    int m1a[]={1};int e1a[]={1};
    int* m1[]={m1a};int* e1[]={e1a};
    runTest(m1,1,e1,2,0);

    int m2a[]={1,2},m2b[]={3,4};
    int e2a[]={3,1},e2b[]={4,2};
    int* m2[]={m2a,m2b};int* e2[]={e2a,e2b};
    runTest(m2,2,e2,3,0);

    int m3a[]={1,1,1},m3b[]={2,2,2},m3c[]={3,3,3};
    int e3a[]={3,2,1},e3b[]={3,2,1},e3c[]={3,2,1};
    int* m3[]={m3a,m3b,m3c};int* e3[]={e3a,e3b,e3c};
    runTest(m3,3,e3,4,0);

    int m4a[]={-1,-2},m4b[]={-3,-4};
    int e4a[]={-3,-1},e4b[]={-4,-2};
    int* m4[]={m4a,m4b};int* e4[]={e4a,e4b};
    runTest(m4,2,e4,5,0);

    int m5a[]={1,2,3,4},m5b[]={5,6,7,8},m5c[]={9,10,11,12},m5d[]={13,14,15,16};
    int e5a[]={13,9,5,1},e5b[]={14,10,6,2},e5c[]={15,11,7,3},e5d[]={16,12,8,4};
    int* m5[]={m5a,m5b,m5c,m5d};int* e5[]={e5a,e5b,e5c,e5d};
    runTest(m5,4,e5,6,1);

    int m6a[]={0,0},m6b[]={0,0};
    int* m6[]={m6a,m6b};int* e6[]={m6a,m6b};
    runTest(m6,2,e6,7,1);

    int m7a[]={1,2},m7b[]={2,1};
    int e7a[]={2,1},e7b[]={1,2};
    int* m7[]={m7a,m7b};int* e7[]={e7a,e7b};
    runTest(m7,2,e7,8,1);

    int m8a[]={100,200,300},m8b[]={400,500,600},m8c[]={700,800,900};
    int e8a[]={700,400,100},e8b[]={800,500,200},e8c[]={900,600,300};
    int* m8[]={m8a,m8b,m8c};int* e8[]={e8a,e8b,e8c};
    runTest(m8,3,e8,9,1);

    int m9a[]={1,2,3,4},m9b[]={5,6,7,8},m9c[]={9,10,11,12},m9d[]={13,14,15,16};
    int e9a[]={13,9,5,1},e9b[]={14,10,6,2},e9c[]={15,11,7,3},e9d[]={16,12,8,4};
    int* m9[]={m9a,m9b,m9c,m9d};int* e9[]={e9a,e9b,e9c,e9d};
    runTest(m9,4,e9,10,1);

    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
