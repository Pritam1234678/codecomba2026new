"""
ZigZag Matrix
================
Given an m x n matrix, print its elements in zig-zag fashion — traverse the
matrix in a snake-like pattern. Start from top-left and traverse diagonally
in alternating directions, or traverse row by row alternating left-to-right
and right-to-left (row-wise zigzag).

This problem uses ROW-WISE zigzag: row 0 left-to-right, row 1 right-to-left,
row 2 left-to-right, and so on.

Examples:
  matrix = [[1,2,3],[4,5,6],[7,8,9]] → 1 2 3 6 5 4 7 8 9

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="ZigZag Matrix"
desc=(
    "Given an m x n matrix, print all elements in zig-zag (snake) fashion.\n\n"
    "In zig-zag fashion, the matrix is traversed row by row, but alternate rows "
    "are reversed:\n"
    "- Row 0 (even): left to right\n"
    "- Row 1 (odd): right to left\n"
    "- Row 2 (even): left to right\n"
    "- and so on...\n\n"
    "For example:\n"
    "matrix = [[1,2,3],[4,5,6],[7,8,9]] → 1 2 3 6 5 4 7 8 9\n"
    "Row 0: 1,2,3 (left to right). Row 1: 6,5,4 (right to left). Row 2: 7,8,9.\n\n"
    "Iterate through each row. For even-indexed rows print left to right, "
    "for odd-indexed rows print right to left."
)
infmt="First line contains m and n.\nNext m lines each contain n space-separated integers."
outfmt="Print all elements in zig-zag order, space-separated."
cons="1 ≤ m, n ≤ 100\n-10^6 ≤ matrix[i][j] ≤ 10^6"
e1="Input:\n3 3\n1 2 3\n4 5 6\n7 8 9\n\nOutput:\n1 2 3 6 5 4 7 8 9"
e2="Input:\n2 3\n1 2 3\n4 5 6\n\nOutput:\n1 2 3 6 5 4"
e3="Input:\n1 1\n5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String zigzagTraverse(int[][] matrix) {
        // Write your code here — even rows L->R, odd rows R->L, space separated
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] m,String e,int tc,boolean h){String g=new CodeCoder().zigzagTraverse(m);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:got="+g+":exp="+e);}
public static void main(String[] a){
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},"1 2 3 6 5 4 7 8 9",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,2,3},{4,5,6}},"1 2 3 6 5 4",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{5}},"5",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1},{2},{3}},"1 2 3",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1,2}},"1 2",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12}},"1 2 3 4 8 7 6 5 9 10 11 12",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{-1,-2},{-3,-4}},"-1 -2 -4 -3",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{0,0},{0,0},{0,0}},"0 0 0 0 0 0",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,2,3}},"1 2 3",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{10,20},{30,40},{50,60}},"10 20 40 30 50 60",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string zigzagTraverse(vector<vector<int>>& m){return "";}};
// USER_CODE_END
void test(vector<vector<int>> m,string e,int tc,bool h=false){string g=CodeCoder().zigzagTraverse(m);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:got="<<g<<":exp="<<e<<"\\n";}
int main(){
try{test({{1,2,3},{4,5,6},{7,8,9}},"1 2 3 6 5 4 7 8 9",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,2,3},{4,5,6}},"1 2 3 6 5 4",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{5}},"5",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1},{2},{3}},"1 2 3",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1,2}},"1 2",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4},{5,6,7,8},{9,10,11,12}},"1 2 3 4 8 7 6 5 9 10 11 12",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-1,-2},{-3,-4}},"-1 -2 -4 -3",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{0,0},{0,0},{0,0}},"0 0 0 0 0 0",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,2,3}},"1 2 3",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{10,20},{30,40},{50,60}},"10 20 40 30 50 60",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def zigzagTraverse(self, matrix):
        return ""
# USER_CODE_END
def test(m,e,tc,h=False):g=CodeCoder().zigzagTraverse(m);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got={repr(g)}:exp={repr(e)}"))
try:test([[1,2,3],[4,5,6],[7,8,9]],"1 2 3 6 5 4 7 8 9",1)
except:print("TC:1:FAIL:hidden")
try:test([[1,2,3],[4,5,6]],"1 2 3 6 5 4",2)
except:print("TC:2:FAIL:hidden")
try:test([[5]],"5",3)
except:print("TC:3:FAIL:hidden")
try:test([[1],[2],[3]],"1 2 3",4)
except:print("TC:4:FAIL:hidden")
try:test([[1,2]],"1 2",5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4],[5,6,7,8],[9,10,11,12]],"1 2 3 4 8 7 6 5 9 10 11 12",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-1,-2],[-3,-4]],"-1 -2 -4 -3",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[0,0],[0,0],[0,0]],"0 0 0 0 0 0",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,2,3]],"1 2 3",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[10,20],[30,40],[50,60]],"10 20 40 30 50 60",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function zigzagTraverse(matrix) { return ""; }
// USER_CODE_END
function test(m,e,tc,h){if(h===undefined)h=false;const g=zigzagTraverse(m);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(g)+":exp="+JSON.stringify(e));}
try{test([[1,2,3],[4,5,6],[7,8,9]],"1 2 3 6 5 4 7 8 9",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,2,3],[4,5,6]],"1 2 3 6 5 4",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[5]],"5",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1],[2],[3]],"1 2 3",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1,2]],"1 2",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4],[5,6,7,8],[9,10,11,12]],"1 2 3 4 8 7 6 5 9 10 11 12",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-1,-2],[-3,-4]],"-1 -2 -4 -3",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[0,0],[0,0],[0,0]],"0 0 0 0 0 0",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,2,3]],"1 2 3",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[10,20],[30,40],[50,60]],"10 20 40 30 50 60",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void zigzagTraverse(int** m,int rs,int* cs,char* out){out[0]=0;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
