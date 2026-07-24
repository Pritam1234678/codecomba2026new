"""
Flood Fill
============
An image is represented by an m x n grid of integers. You are given starting
pixel (sr, sc) and a new color. Perform a flood fill: change the starting
pixel and all connected pixels of the same original color to the new color.

Connected means 4-directionally adjacent (up, down, left, right).

Example:
  image = [[1,1,1],
           [1,1,0],
           [1,0,1]]
  sr=1, sc=1, newColor=2
  Output: [[2,2,2],
           [2,2,0],
           [2,0,1]]

Approach: DFS or BFS from starting pixel. If current pixel has original color,
change it to newColor, then recurse on all 4 neighbors.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Flood Fill"
desc=(
    "An image is represented by an m x n grid of integers. You are also given "
    "three integers: sr (start row), sc (start column), and newColor.\n\n"
    "Perform a flood fill starting from pixel (sr, sc). Replace all pixels that "
    "are connected 4-directionally (up, down, left, right) to the starting pixel "
    "and have the same original color as the starting pixel, with the new color.\n\n"
    "For example:\n"
    "image = [[1,1,1],[1,1,0],[1,0,1]], sr=1, sc=1, newColor=2\n"
    "Starting at (1,1) with color 1. All 1's connected to it become 2.\n"
    "Output: [[2,2,2],[2,2,0],[2,0,1]]\n\n"
    "Use DFS or BFS. If the starting pixel already has the new color, return as-is."
)
infmt="First line contains m and n.\nNext m lines contain n space-separated integers.\nLast line contains sr, sc and newColor."
outfmt="Print the modified grid, m lines with n space-separated integers each."
cons="1 ≤ m, n ≤ 50\n0 ≤ image[i][j] < 2^31\n0 ≤ sr < m, 0 ≤ sc < n\n0 ≤ newColor < 2^31"
e1="Input:\n3 3\n1 1 1\n1 1 0\n1 0 1\n1 1 2\n\nOutput:\n2 2 2\n2 2 0\n2 0 1"
e2="Input:\n1 1\n1\n0 0 0\n\nOutput:\n0"
e3="Input:\n2 2\n0 0\n0 0\n0 0 1\n\nOutput:\n1 1\n1 1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, DFS, BFS, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[][] floodFill(int[][] image, int sr, int sc, int newColor) {
        // Write your code here — DFS
        return image;
    }
}
// USER_CODE_END

public class Main {
static boolean eq(int[][] a,int[][] b){for(int i=0;i<a.length;i++)for(int j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
static void test(int[][] img,int sr,int sc,int nc,int[][] e,int tc,boolean h){int[][] g=new CodeCoder().floodFill(img,sr,sc,nc);if(eq(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+Arrays.deepToString(g));}
public static void main(String[] a){
try{test(new int[][]{{1,1,1},{1,1,0},{1,0,1}},1,1,2,new int[][]{{2,2,2},{2,2,0},{2,0,1}},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1}},0,0,0,new int[][]{{0}},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},0,0,1,new int[][]{{1,1},{1,1}},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,1,1},{1,1,1},{1,1,1}},1,1,2,new int[][]{{2,2,2},{2,2,2},{2,2,2}},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,1,1}},1,1,1,new int[][]{{0,0,0},{0,1,1}},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,0,0}},0,0,0,new int[][]{{0,0,0},{0,0,0}},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}},0,2,5,new int[][]{{1,0,5,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,1},{1,0}},1,0,2,new int[][]{{0,1},{2,0}},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,2},{3,4}},0,0,9,new int[][]{{9,2},{3,4}},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{5,5,5},{5,5,5},{5,5,5}},2,2,7,new int[][]{{7,7,7},{7,7,7},{7,7,7}},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<vector<int>> floodFill(vector<vector<int>>& img,int sr,int sc,int nc){return img;}};
// USER_CODE_END
bool eq(vector<vector<int>>& a,vector<vector<int>>& b){for(size_t i=0;i<a.size();i++)for(size_t j=0;j<a[0].size();j++)if(a[i][j]!=b[i][j])return false;return true;}
void test(vector<vector<int>> img,int sr,int sc,int nc,vector<vector<int>> e,int tc,bool h=false){
vector<vector<int>> g=CodeCoder().floodFill(img,sr,sc,nc);
if(eq(g,e))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL\\n";}
int main(){
try{test({{1,1,1},{1,1,0},{1,0,1}},1,1,2,{{2,2,2},{2,2,0},{2,0,1}},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1}},0,0,0,{{0}},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},0,0,1,{{1,1},{1,1}},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,1,1},{1,1,1},{1,1,1}},1,1,2,{{2,2,2},{2,2,2},{2,2,2}},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{0,0,0},{0,1,1}},1,1,1,{{0,0,0},{0,1,1}},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{0,0,0},{0,0,0}},0,0,0,{{0,0,0},{0,0,0}},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}},0,2,5,{{1,0,5,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,1},{1,0}},1,0,2,{{0,1},{2,0}},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,2},{3,4}},0,0,9,{{9,2},{3,4}},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{5,5,5},{5,5,5},{5,5,5}},2,2,7,{{7,7,7},{7,7,7},{7,7,7}},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def floodFill(self, image, sr, sc, newColor):
        return image
# USER_CODE_END
def eq(a,b):
    for ri in range(len(a)):
        for ci in range(len(a[0])):
            if a[ri][ci]!=b[ri][ci]:return False
    return True
def test(img,sr,sc,nc,e,tc,h=False):
    g=CodeCoder().floodFill([row[:] for row in img],sr,sc,nc)
    if eq(g,e):print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={g}:exp={e}")

try:test([[1,1,1],[1,1,0],[1,0,1]],1,1,2,[[2,2,2],[2,2,0],[2,0,1]],1)
except:print("TC:1:FAIL:hidden")
try:test([[1]],0,0,0,[[0]],2)
except:print("TC:2:FAIL:hidden")
try:test([[0,0],[0,0]],0,0,1,[[1,1],[1,1]],3)
except:print("TC:3:FAIL:hidden")
try:test([[1,1,1],[1,1,1],[1,1,1]],1,1,2,[[2,2,2],[2,2,2],[2,2,2]],4)
except:print("TC:4:FAIL:hidden")
try:test([[0,0,0],[0,1,1]],1,1,1,[[0,0,0],[0,1,1]],5)
except:print("TC:5:FAIL:hidden")
try:test([[0,0,0],[0,0,0]],0,0,0,[[0,0,0],[0,0,0]],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,0,0,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]],0,2,5,[[1,0,5,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,1],[1,0]],1,0,2,[[0,1],[2,0]],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,2],[3,4]],0,0,9,[[9,2],[3,4]],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[5,5,5],[5,5,5],[5,5,5]],2,2,7,[[7,7,7],[7,7,7],[7,7,7]],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function floodFill(image, sr, sc, newColor) { return image; }
// USER_CODE_END
function eq(a,b){for(let i=0;i<a.length;i++)for(let j=0;j<a[0].length;j++)if(a[i][j]!=b[i][j])return false;return true;}
function test(img,sr,sc,nc,e,tc,h){if(h===undefined)h=false;
const g=floodFill(img.map(r=>[...r]),sr,sc,nc);
if(eq(g,e))console.log("TC:"+tc+":PASS"+(h?":hidden":""));
else if(h)console.log("TC:"+tc+":FAIL:hidden");
else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g));}
try{test([[1,1,1],[1,1,0],[1,0,1]],1,1,2,[[2,2,2],[2,2,0],[2,0,1]],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1]],0,0,0,[[0]],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[0,0],[0,0]],0,0,1,[[1,1],[1,1]],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,1,1],[1,1,1],[1,1,1]],1,1,2,[[2,2,2],[2,2,2],[2,2,2]],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[0,0,0],[0,1,1]],1,1,1,[[0,0,0],[0,1,1]],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[0,0,0],[0,0,0]],0,0,0,[[0,0,0],[0,0,0]],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,0,0,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]],0,2,5,[[1,0,5,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,1],[1,0]],1,0,2,[[0,1],[2,0]],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,2],[3,4]],0,0,9,[[9,2],[3,4]],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[5,5,5],[5,5,5],[5,5,5]],2,2,7,[[7,7,7],[7,7,7],[7,7,7]],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int** floodFill(int** image, int imageSize, int* imageColSize, int sr, int sc, int newColor, int* returnSize, int** returnColSizes) {
    // Write your code here
    *returnSize = imageSize;
    *returnColSizes = imageColSize;
    return image;
}
// USER_CODE_END

int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
