"""
Count Number Of Zeroes
=========================
Given an m x n matrix where each row is sorted in decreasing order, count
the total number of zeroes in the matrix.

Examples:
  matrix = [[1,1,1,0,0],
            [1,0,0,0,0],
            [1,1,0,0,0],
            [0,0,0,0,0]] → 12 zeroes

Efficient approach: start from top-right, move left while seeing 0,
move down otherwise. Count zeros as we go.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Count Number Of Zeroes"
desc=(
    "Given an m x n matrix where each row is sorted in non-increasing order "
    "(all 1s appear before all 0s in each row), count the total number of zeroes "
    "in the entire matrix.\n\n"
    "For example:\n"
    "matrix = [[1,1,1,0,0],[1,0,0,0,0],[1,1,0,0,0],[0,0,0,0,0]] → 12 zeroes\n\n"
    "Efficient O(m+n) approach: start at the top-right corner. If the current "
    "element is 0, it means everything to its left in the same row is also 0 — "
    "count the zeroes and move down. If the current element is 1, move left."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers (1s then 0s)."
outfmt="Print the total count of zeroes."
cons="1 ≤ m, n ≤ 1000\nEach row is non-increasing (1s first, then 0s)."
e1="Input:\n4 5\n1 1 1 0 0\n1 0 0 0 0\n1 1 0 0 0\n0 0 0 0 0\n\nOutput:\n12"
e2="Input:\n2 2\n1 1\n1 1\n\nOutput:\n0"
e3="Input:\n2 2\n0 0\n0 0\n\nOutput:\n4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int countZeroes(int[][] matrix) {
        // Write your code here — O(m+n) from top-right
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,int e,int tc,boolean h){int g=new CodeCoder().countZeroes(m);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{1,1,1,0,0},{1,0,0,0,0},{1,1,0,0,0},{0,0,0,0,0}},12,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,1}},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},4,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,0}},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{0},{0},{0}},3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,1,1,1,1},{1,1,1,1,1}},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,1,0,0,0,0},{1,1,0,0,0,0},{1,0,0,0,0,0}},11,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,1,1,0},{1,1,0,0},{1,0,0,0}},7,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{0,0,0},{0,0,0}},6,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{1,1},{1,0},{0,0}},4,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int countZeroes(vector<vector<int>>& m){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> m,int e,int tc,bool h=false){int g=CodeCoder().countZeroes(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({{1,1,1,0,0},{1,0,0,0,0},{1,1,0,0,0},{0,0,0,0,0}},12,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,1},{1,1}},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},4,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,0}},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{0},{0},{0}},3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,1,1,1,1},{1,1,1,1,1}},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,1,0,0,0,0},{1,1,0,0,0,0},{1,0,0,0,0,0}},11,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,1,1,0},{1,1,0,0},{1,0,0,0}},7,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{0,0,0},{0,0,0}},6,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{1,1},{1,0},{0,0}},4,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def countZeroes(self, matrix): return 0
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().countZeroes(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[1,1,1,0,0],[1,0,0,0,0],[1,1,0,0,0],[0,0,0,0,0]],12,1)
except:print("TC:1:FAIL:hidden")
try:test([[1,1],[1,1]],0,2)
except:print("TC:2:FAIL:hidden")
try:test([[0,0],[0,0]],4,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,0]],1,4)
except:print("TC:4:FAIL:hidden")
try:test([[0],[0],[0]],3,5)
except:print("TC:5:FAIL:hidden")
try:test([[1,1,1,1,1],[1,1,1,1,1]],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,1,0,0,0,0],[1,1,0,0,0,0],[1,0,0,0,0,0]],11,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,1,1,0],[1,1,0,0],[1,0,0,0]],7,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[0,0,0],[0,0,0]],6,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[1,1],[1,0],[0,0]],4,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function countZeroes(matrix) { return 0; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=countZeroes(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[1,1,1,0,0],[1,0,0,0,0],[1,1,0,0,0],[0,0,0,0,0]],12,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,1],[1,1]],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[0,0],[0,0]],4,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,0]],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[0],[0],[0]],3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,1,1,1,1],[1,1,1,1,1]],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,1,0,0,0,0],[1,1,0,0,0,0],[1,0,0,0,0,0]],11,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,1,1,0],[1,1,0,0],[1,0,0,0]],7,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[0,0,0],[0,0,0]],6,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[1,1],[1,0],[0,0]],4,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int countZeroes(int** m,int rs,int* cs){return 0;}
// USER_CODE_END
void run(int* rows[],int rs,int cs,int e,int tc,int h){int csArr[10]={cs};int* pcs=csArr;int g=countZeroes(rows,rs,pcs);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int r0a[]={1,1,1,0,0},r0b[]={1,0,0,0,0},r0c[]={1,1,0,0,0},r0d[]={0,0,0,0,0};int* m0[]={r0a,r0b,r0c,r0d};run(m0,4,5,12,1,0);
int r1a[]={1,1},r1b[]={1,1};int* m1[]={r1a,r1b};run(m1,2,2,0,2,0);
int r2a[]={0,0},r2b[]={0,0};int* m2[]={r2a,r2b};run(m2,2,2,4,3,0);
int r3a[]={1,0};int* m3[]={r3a};run(m3,1,2,1,4,0);
int r4a[]={0},r4b[]={0},r4c[]={0};int* m4[]={r4a,r4b,r4c};run(m4,3,1,3,5,0);
int r5a[]={1,1,1,1,1},r5b[]={1,1,1,1,1};int* m5[]={r5a,r5b};run(m5,2,5,0,6,1);
int r6a[]={1,1,0,0,0,0},r6b[]={1,1,0,0,0,0},r6c[]={1,0,0,0,0,0};int* m6[]={r6a,r6b,r6c};run(m6,3,6,11,7,1);
int r7a[]={1,1,1,0},r7b[]={1,1,0,0},r7c[]={1,0,0,0};int* m7[]={r7a,r7b,r7c};run(m7,3,4,7,8,1);
int r8a[]={0,0,0},r8b[]={0,0,0};int* m8[]={r8a,r8b};run(m8,2,3,6,9,1);
int r9a[]={1,1},r9b[]={1,0},r9c[]={0,0};int* m9[]={r9a,r9b,r9c};run(m9,3,2,4,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
