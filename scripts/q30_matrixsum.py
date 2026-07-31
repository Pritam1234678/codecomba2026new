"""
Sum of Elements in a Matrix
=============================
Given an m x n matrix, compute the sum of all its elements.

Examples:
  matrix = [[1,2,3],[4,5,6]] → 21
  matrix = [[10,20],[30,40]] → 100

Simply iterate through all cells and add.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sum of Elements in a Matrix"
desc=(
    "Given an m x n matrix, compute the sum of all elements in the matrix.\n\n"
    "For example:\n"
    "matrix = [[1,2,3],[4,5,6]] → sum = 1+2+3+4+5+6 = 21\n"
    "matrix = [[10,20],[30,40]] → sum = 10+20+30+40 = 100\n\n"
    "Iterate through each row and each column, adding every element to a running total."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers."
outfmt="Print the sum of all matrix elements."
cons="1 ≤ m, n ≤ 100\n-10^6 ≤ matrix[i][j] ≤ 10^6\nSum fits in 32-bit int."
e1="Input:\n2 3\n1 2 3\n4 5 6\n\nOutput:\n21"
e2="Input:\n2 2\n10 20\n30 40\n\nOutput:\n100"
e3="Input:\n1 1\n5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int matrixSum(int[][] matrix) {
        // Write your code here — sum all elements
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,int e,int tc,boolean h){int g=new CodeCoder().matrixSum(m);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[][]{{1,2,3},{4,5,6}},21,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{10,20},{30,40}},100,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{5}},5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{-1,-2},{-3,-4}},-10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0}},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4,5},{6,7,8,9,10}},55,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1000000,1000000},{1000000,1000000}},4000000,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{-1000000,1000000}},0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1},{2},{3},{4}},10,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{7,7,7},{7,7,7}},42,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int matrixSum(vector<vector<int>>& m){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> m,int e,int tc,bool h=false){int g=CodeCoder().matrixSum(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({{1,2,3},{4,5,6}},21,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{10,20},{30,40}},100,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{5}},5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{-1,-2},{-3,-4}},-10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{0,0},{0,0}},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4,5},{6,7,8,9,10}},55,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1000000,1000000},{1000000,1000000}},4000000,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{-1000000,1000000}},0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1},{2},{3},{4}},10,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{7,7,7},{7,7,7}},42,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def matrixSum(self, matrix): return 0
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().matrixSum(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([[1,2,3],[4,5,6]],21,1)
except:print("TC:1:FAIL:hidden")
try:test([[10,20],[30,40]],100,2)
except:print("TC:2:FAIL:hidden")
try:test([[5]],5,3)
except:print("TC:3:FAIL:hidden")
try:test([[-1,-2],[-3,-4]],-10,4)
except:print("TC:4:FAIL:hidden")
try:test([[0,0],[0,0]],0,5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4,5],[6,7,8,9,10]],55,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1000000,1000000],[1000000,1000000]],4000000,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[-1000000,1000000]],0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1],[2],[3],[4]],10,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[7,7,7],[7,7,7]],42,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function matrixSum(matrix) { return 0; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=matrixSum(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([[1,2,3],[4,5,6]],21,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[10,20],[30,40]],100,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[5]],5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[-1,-2],[-3,-4]],-10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[0,0],[0,0]],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4,5],[6,7,8,9,10]],55,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1000000,1000000],[1000000,1000000]],4000000,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[-1000000,1000000]],0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1],[2],[3],[4]],10,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[7,7,7],[7,7,7]],42,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int matrixSum(int** m,int rs,int* cs){return 0;}
// USER_CODE_END
void run(int* rows[],int rs,int cs,int e,int tc,int h){int csArr[10]={cs};int* pcs=csArr;int g=matrixSum(rows,rs,pcs);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int r0a[]={1,2,3},r0b[]={4,5,6};int* m0[]={r0a,r0b};run(m0,2,3,21,1,0);
int r1a[]={10,20},r1b[]={30,40};int* m1[]={r1a,r1b};run(m1,2,2,100,2,0);
int r2a[]={5};int* m2[]={r2a};run(m2,1,1,5,3,0);
int r3a[]={-1,-2},r3b[]={-3,-4};int* m3[]={r3a,r3b};run(m3,2,2,-10,4,0);
int r4a[]={0,0},r4b[]={0,0};int* m4[]={r4a,r4b};run(m4,2,2,0,5,0);
int r5a[]={1,2,3,4,5},r5b[]={6,7,8,9,10};int* m5[]={r5a,r5b};run(m5,2,5,55,6,1);
int r6a[]={1000000,1000000},r6b[]={1000000,1000000};int* m6[]={r6a,r6b};run(m6,2,2,4000000,7,1);
int r7a[]={-1000000,1000000};int* m7[]={r7a};run(m7,1,2,0,8,1);
int r8a[]={1},r8b[]={2},r8c[]={3},r8d[]={4};int* m8[]={r8a,r8b,r8c,r8d};run(m8,4,1,10,9,1);
int r9a[]={7,7,7},r9b[]={7,7,7};int* m9[]={r9a,r9b};run(m9,2,3,42,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
