"""
Search a 2D Matrix II
======================
Given an m x n integer matrix where:
  - each row is sorted in ascending order (left to right), and
  - each column is sorted in ascending order (top to bottom),
return true if a target value is in the matrix, false otherwise.

Examples:
  matrix = [[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]], target = 5  -> true
  matrix = same, target = 20 -> false

Efficient O(m + n): start at the top-right cell. If it equals target return
true; if it is greater than target move left; if it is less than target move
down. (Rows and columns are each sorted, but the matrix is NOT one flat sorted
array, so the q83 trick does not apply.)

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows m, cols n.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Search a 2D Matrix II"
desc=(
    "Given an m x n integer matrix where each row is sorted in ascending order "
    "from left to right AND each column is sorted in ascending order from top "
    "to bottom, return true if a given target is present in the matrix, "
    "otherwise return false.\n\n"
    "For example:\n"
    "matrix = [[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],"
    "[18,21,23,26,30]], target = 5  -> true\n"
    "matrix = same, target = 20 -> false\n\n"
    "Because rows and columns are each sorted (but the whole matrix is not one "
    "flat sorted array), binary-search-the-flattened trick does not work. "
    "Instead use an O(m + n) search: start at the top-right cell. If it equals "
    "target, return true; if it is greater than target, move left one column; "
    "if it is less than target, move down one row."
)
infmt="First line contains m and n. Then m lines follow, each with n space-separated integers. The last line contains the target value."
outfmt="Print 'true' if target is in the matrix, else 'false'."
cons="1 ≤ m, n ≤ 300\n-10^9 ≤ matrix[i][j], target ≤ 10^9\nRows and columns are each sorted in ascending order."
e1="Input:\n5 5\n1 4 7 11 15\n2 5 8 12 19\n3 6 9 16 22\n10 13 14 17 24\n18 21 23 26 30\n5\n\nOutput:\ntrue"
e2="Input:\n5 5\n1 4 7 11 15\n2 5 8 12 19\n3 6 9 16 22\n10 13 14 17 24\n18 21 23 26 30\n20\n\nOutput:\nfalse"
e3="Input:\n1 1\n5\n5\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean searchMatrix(int[][] matrix, int target) {
        // Write your code here — O(m+n) search from top-right
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int t,boolean e,int tc,boolean h){boolean r=new CodeCoder().searchMatrix(g,t);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:mat="+Arrays.deepToString(g)+":target="+t+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},5,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},20,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},1,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},30,true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{5}},5,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{5}},2,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},15,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},18,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},100,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{1,4,7},{2,5,8},{3,6,9}},5,true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool searchMatrix(vector<vector<int>>& matrix,int target){return false;}};
// USER_CODE_END
void test(vector<vector<int>> g,int t,bool e,int tc,bool h=false){bool r=CodeCoder().searchMatrix(g,t);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(r?"true":"false")<<"\\n";}
int main(){
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},5,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},20,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},1,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},30,true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{5}},5,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{5}},2,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},15,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},18,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,4,7,11,15},{2,5,8,12,19},{3,6,9,16,22},{10,13,14,17,24},{18,21,23,26,30}},100,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{1,4,7},{2,5,8},{3,6,9}},5,true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def searchMatrix(self, matrix, target):
        return False
# USER_CODE_END
def test(g,t,e,tc,h=False):r=CodeCoder().searchMatrix(g,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:mat={g}:target={t}:exp={e}:got={r}"))
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],5,True,1)
except:print("TC:1:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],20,False,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],1,True,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],30,True,4)
except:print("TC:4:FAIL:hidden")
try:test([[5]],5,True,5)
except:print("TC:5:FAIL:hidden")
try:test([[5]],2,False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],15,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],18,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],100,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[1,4,7],[2,5,8],[3,6,9]],5,True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function searchMatrix(matrix, target) { return false; }
// USER_CODE_END
function test(g,t,e,tc,h){if(h===undefined)h=false;const r=searchMatrix(g,t);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],5,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],20,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],1,true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],30,true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[5]],5,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[5]],2,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],15,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],18,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,4,7,11,15],[2,5,8,12,19],[3,6,9,16,22],[10,13,14,17,24],[18,21,23,26,30]],100,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[1,4,7],[2,5,8],[3,6,9]],5,true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
    int t1[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t1,5,5,5,true,1,0);
    int t2[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t2,5,5,20,false,2,0);
    int t3[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t3,5,5,1,true,3,0);
    int t4[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t4,5,5,30,true,4,0);
    int t5[]={5};runTest(t5,1,1,5,true,5,0);
    int t6[]={5};runTest(t6,1,1,2,false,6,1);
    int t7[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t7,5,5,15,true,7,1);
    int t8[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t8,5,5,18,true,8,1);
    int t9[]={1,4,7,11,15,2,5,8,12,19,3,6,9,16,22,10,13,14,17,24,18,21,23,26,30};runTest(t9,5,5,100,false,9,1);
    int t10[]={1,4,7,2,5,8,3,6,9};runTest(t10,3,3,5,true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: { size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
