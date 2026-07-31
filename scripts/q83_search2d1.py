"""
Search a 2D Matrix
==================
Given an m x n integer matrix where:
  - each row is sorted in ascending order (left to right), and
  - the first integer of each row is greater than the last integer of the
    previous row,
return true if a target value is in the matrix, false otherwise.

Examples:
  matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 3  -> true
  matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 13 -> false

Treat the matrix as one sorted array of length m*n: index = row*n + col.
Binary search on [0, m*n-1]: mid -> row = mid/n, col = mid%n.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows m, cols n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Search a 2D Matrix"
desc=(
    "Given an m x n integer matrix where each row is sorted in ascending order "
    "from left to right, and the first integer of each row is greater than the "
    "last integer of the previous row, return true if a given target is present "
    "in the matrix, otherwise return false.\n\n"
    "For example:\n"
    "matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 3  -> true\n"
    "matrix = [[1,3,5,7],[10,11,16,20],[23,30,34,60]], target = 13 -> false\n\n"
    "Because the matrix is effectively sorted as one flat array of length m*n, "
    "binary search works directly: for a mid index, row = mid / n and "
    "col = mid % n. Compare the element at (row, col) with target. Runs in "
    "O(log(m*n))."
)
infmt="First line contains m and n. Then m lines follow, each with n space-separated integers. The last line contains the target value."
outfmt="Print 'true' if target is in the matrix, else 'false'."
cons="1 ≤ m, n ≤ 100\n-10^4 ≤ matrix[i][j], target ≤ 10^4\nRows are sorted; first element of each row > last element of the previous row."
e1="Input:\n3 4\n1 3 5 7\n10 11 16 20\n23 30 34 60\n3\n\nOutput:\ntrue"
e2="Input:\n3 4\n1 3 5 7\n10 11 16 20\n23 30 34 60\n13\n\nOutput:\nfalse"
e3="Input:\n1 1\n5\n5\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean searchMatrix(int[][] matrix, int target) {
        // Write your code here — binary search over the flattened matrix
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int t,boolean e,int tc,boolean h){boolean r=new CodeCoder().searchMatrix(g,t);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:mat="+Arrays.deepToString(g)+":target="+t+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},3,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},13,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},60,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},0,false,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{5}},5,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{5}},2,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},23,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},34,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}},100,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{-10,-7,-3,0,2,5},{8,9,12,15,18,20},{25,28,30,33,36,40}},15,true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool searchMatrix(vector<vector<int>>& matrix,int target){return false;}};
// USER_CODE_END
void test(vector<vector<int>> g,int t,bool e,int tc,bool h=false){bool r=CodeCoder().searchMatrix(g,t);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(r?"true":"false")<<"\\n";}
int main(){
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},3,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},13,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},60,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},0,false,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{5}},5,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{5}},2,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},23,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},34,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,3,5,7},{10,11,16,20},{23,30,34,60}},100,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{-10,-7,-3,0,2,5},{8,9,12,15,18,20},{25,28,30,33,36,40}},15,true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def searchMatrix(self, matrix, target):
        return False
# USER_CODE_END
def test(g,t,e,tc,h=False):r=CodeCoder().searchMatrix(g,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:mat={g}:target={t}:exp={e}:got={r}"))
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],3,True,1)
except:print("TC:1:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],13,False,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],60,True,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],0,False,4)
except:print("TC:4:FAIL:hidden")
try:test([[5]],5,True,5)
except:print("TC:5:FAIL:hidden")
try:test([[5]],2,False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],23,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],34,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],100,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[-10,-7,-3,0,2,5],[8,9,12,15,18,20],[25,28,30,33,36,40]],15,True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function searchMatrix(matrix, target) { return false; }
// USER_CODE_END
function test(g,t,e,tc,h){if(h===undefined)h=false;const r=searchMatrix(g,t);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],3,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],13,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],60,true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],0,false,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[5]],5,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[5]],2,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],23,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],34,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,3,5,7],[10,11,16,20],[23,30,34,60]],100,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[-10,-7,-3,0,2,5],[8,9,12,15,18,20],[25,28,30,33,36,40]],15,true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
bool searchMatrix(int* arr,int m,int n,int target) {
    // Write your code here — arr is the matrix flattened row-major (m rows, n cols)
    return false;
}
// USER_CODE_END

void runTest(int* a,int m,int n,int t,bool e,int tc,int h){
    bool r=searchMatrix(a,m,n,t);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",r?"true":"false");}
}
int main(){
    int t1[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t1,3,4,3,true,1,0);
    int t2[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t2,3,4,13,false,2,0);
    int t3[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t3,3,4,60,true,3,0);
    int t4[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t4,3,4,0,false,4,0);
    int t5[]={5};runTest(t5,1,1,5,true,5,0);
    int t6[]={5};runTest(t6,1,1,2,false,6,1);
    int t7[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t7,3,4,23,true,7,1);
    int t8[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t8,3,4,34,true,8,1);
    int t9[]={1,3,5,7,10,11,16,20,23,30,34,60};runTest(t9,3,4,100,false,9,1);
    int t10[]={-10,-7,-3,0,2,5,8,9,12,15,18,20,25,28,30,33,36,40};runTest(t10,3,6,15,true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
