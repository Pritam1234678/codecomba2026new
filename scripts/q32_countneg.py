"""
Count Negative Numbers in a Sorted Matrix
===========================================
Given an m x n matrix where each row is sorted in decreasing order,
count the number of negative numbers in the matrix.

Examples:
  matrix = [[4,3,2,-1],
            [3,2,1,-1],
            [1,1,-1,-2],
            [-1,-1,-2,-3]] → 8 negatives

Efficient O(m+n): start from bottom-left or top-right, move right on negative,
up on positive. Count negatives as we go.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Negative Numbers in a Sorted Matrix"
desc=(
    "Given an m x n matrix where each row is sorted in non-increasing (decreasing) "
    "order, count the number of negative numbers in the matrix.\n\n"
    "For example:\n"
    "matrix = [[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]] → 8 negative numbers\n\n"
    "Efficient O(m+n) approach: start at the bottom-left corner. If the current "
    "element is negative, all elements to its right in the same row are also "
    "negative — add the count and move up. If it's positive or zero, move right."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers (sorted decreasing)."
outfmt="Print the count of negative numbers."
cons="1 ≤ m, n ≤ 100\nEach row is sorted in non-increasing order."
e1="Input:\n4 4\n4 3 2 -1\n3 2 1 -1\n1 1 -1 -2\n-1 -1 -2 -3\n\nOutput:\n8"
e2="Input:\n2 2\n1 1\n1 1\n\nOutput:\n0"
e3="Input:\n2 2\n-1 -2\n-3 -4\n\nOutput:\n4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Matrix, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countNegatives(int[][] matrix) {
        // Write your code here — O(m+n) bottom-left
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,int e,int tc,boolean h){int g=new CodeCoder().countNegatives(m);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{4,3,2,-1},{3,2,1,-1},{1,1,-1,-2},{-1,-1,-2,-3}},8,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,1}},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{-1,-2},{-3,-4}},4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{5}},0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{-1}},1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{3,2},{1,0}},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{0,-1},{-1,-2}},3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{-1,-2,-3},{-1,-2,-3}},6,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{5,4,3,2,1}},0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{1,0,0,-1}},1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countNegatives(vector<vector<int>>& m){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> m,int e,int tc,bool h=false){int g=CodeCoder().countNegatives(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({{4,3,2,-1},{3,2,1,-1},{1,1,-1,-2},{-1,-1,-2,-3}},8,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,1},{1,1}},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{-1,-2},{-3,-4}},4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{5}},0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{-1}},1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{3,2},{1,0}},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{0,-1},{-1,-2}},3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{-1,-2,-3},{-1,-2,-3}},6,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{5,4,3,2,1}},0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{1,0,0,-1}},1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countNegatives(self, matrix): return 0
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().countNegatives(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]],8,1)
except:print("TC:1:FAIL:hidden")
try:test([[1,1],[1,1]],0,2)
except:print("TC:2:FAIL:hidden")
try:test([[-1,-2],[-3,-4]],4,3)
except:print("TC:3:FAIL:hidden")
try:test([[5]],0,4)
except:print("TC:4:FAIL:hidden")
try:test([[-1]],1,5)
except:print("TC:5:FAIL:hidden")
try:test([[3,2],[1,0]],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[0,-1],[-1,-2]],3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[-1,-2,-3],[-1,-2,-3]],6,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[5,4,3,2,1]],0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[1,0,0,-1]],1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countNegatives(matrix) { return 0; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=countNegatives(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[4,3,2,-1],[3,2,1,-1],[1,1,-1,-2],[-1,-1,-2,-3]],8,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,1],[1,1]],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[-1,-2],[-3,-4]],4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[5]],0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[-1]],1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[3,2],[1,0]],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[0,-1],[-1,-2]],3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[-1,-2,-3],[-1,-2,-3]],6,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[5,4,3,2,1]],0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[1,0,0,-1]],1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int countNegatives(int** m,int rs,int* cs){return 0;}
// USER_CODE_END
void run(int* rows[],int rs,int cs,int e,int tc,int h){int csArr[10]={cs};int* pcs=csArr;int g=countNegatives(rows,rs,pcs);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int r0a[]={4,3,2,-1},r0b[]={3,2,1,-1},r0c[]={1,1,-1,-2},r0d[]={-1,-1,-2,-3};int* m0[]={r0a,r0b,r0c,r0d};run(m0,4,4,8,1,0);
int r1a[]={1,1},r1b[]={1,1};int* m1[]={r1a,r1b};run(m1,2,2,0,2,0);
int r2a[]={-1,-2},r2b[]={-3,-4};int* m2[]={r2a,r2b};run(m2,2,2,4,3,0);
int r3a[]={5};int* m3[]={r3a};run(m3,1,1,0,4,0);
int r4a[]={-1};int* m4[]={r4a};run(m4,1,1,1,5,0);
int r5a[]={3,2},r5b[]={1,0};int* m5[]={r5a,r5b};run(m5,2,2,0,6,1);
int r6a[]={0,-1},r6b[]={-1,-2};int* m6[]={r6a,r6b};run(m6,2,2,3,7,1);
int r7a[]={-1,-2,-3},r7b[]={-1,-2,-3};int* m7[]={r7a,r7b};run(m7,2,3,6,8,1);
int r8a[]={5,4,3,2,1};int* m8[]={r8a};run(m8,1,5,0,9,1);
int r9a[]={1,0,0,-1};int* m9[]={r9a};run(m9,1,4,1,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
