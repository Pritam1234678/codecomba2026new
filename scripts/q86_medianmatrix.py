"""
Median in a Row-Wise Sorted Matrix
====================================
Given an r x c matrix that is sorted in ascending order row by row, find the
median of all r*c elements. The median is defined as the element that lands at
index (r*c)/2 (0-indexed) once all elements are merged into one sorted array.

Examples:
  matrix = [[1,3,5],[2,6,9],[3,6,9]]  -> sorted [1,2,3,3,5,6,6,9,9] -> median 5 (idx 4)
  matrix = [[1],[2],[3]]             -> sorted [1,2,3]              -> median 2 (idx 1)

Efficient O(r * log(max-min)): binary search on the ANSWER value between the
global min and max. For a candidate mid, count elements <= mid across every row
using binary search (each row is sorted). If that count <= (r*c)/2, the median
lies to the right of mid; otherwise to the left. The answer is the smallest
value whose count exceeds (r*c)/2.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the matrix is passed flattened row-major as int* arr with rows r, cols c.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Median in a Row-Wise Sorted Matrix"
desc=(
    "Given an r x c matrix whose rows are each sorted in ascending order, "
    "return the median of all r*c elements. The median is the element at index "
    "(r*c)/2 (0-indexed) in the fully merged sorted array.\n\n"
    "For example:\n"
    "matrix = [[1,3,5],[2,6,9],[3,6,9]] -> merged [1,2,3,3,5,6,6,9,9] -> median 5 (index 4)\n"
    "matrix = [[1],[2],[3]]            -> merged [1,2,3]             -> median 2 (index 1)\n\n"
    "A naive merge is O(r*c log(r*c)). Instead, binary search on the ANSWER "
    "value in [globalMin, globalMax]: for a candidate mid, count how many "
    "elements are <= mid by binary-searching each (sorted) row. If that count "
    "is <= (r*c)/2, the median is to the right of mid; else to the left. "
    "Return the smallest value whose running count exceeds (r*c)/2."
)
infmt="First line contains r and c (rows and columns). Then r lines follow, each with c space-separated integers (rows are sorted ascending)."
outfmt="Print the median — the element at index (r*c)/2 in the sorted combined array."
cons="1 ≤ r, c ≤ 100\nEach row is sorted in ascending order."
e1="Input:\n3 3\n1 3 5\n2 6 9\n3 6 9\n\nOutput:\n5"
e2="Input:\n3 1\n1\n2\n3\n\nOutput:\n2"
e3="Input:\n3 3\n1 3 4\n2 5 6\n7 8 9\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Binary Search, Matrix",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int median(int[][] matrix, int r, int c) {
        // Write your code here — binary search on the answer value
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[][] g,int r,int c,int e,int tc,boolean h){int m=new CodeCoder().median(g,r,c);if(m==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:mat="+Arrays.deepToString(g)+":exp="+e+":got="+m);}
public static void main(String[] a){
try{test(new int[][]{{1,3,5},{2,6,9},{3,6,9}},3,3,5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1},{2},{3}},3,1,2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,2,3,4,5},{6,7,8,9,10}},2,5,6,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,3,4},{2,5,6},{7,8,9}},3,3,5,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{5,10,15,20,25},{30,35,40,45,50}},2,5,30,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,2,3},{4,5,6},{7,8,9}},3,3,5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[][]{{11,13,15},{16,17,19},{21,23,25}},3,3,17,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[][]{{1},{1},{1}},3,1,1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[][]{{1,5},{2,6},{3,7},{4,8}},4,2,5,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[][]{{2,6,12,18},{3,7,13,19},{4,8,14,20},{5,9,15,21}},4,4,12,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int median(vector<vector<int>>& matrix,int r,int c){return 0;}};
// USER_CODE_END
void test(vector<vector<int>> g,int r,int c,int e,int tc,bool h=false){int m=CodeCoder().median(g,r,c);if(m==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<m<<"\\n";}
int main(){
try{test({{1,3,5},{2,6,9},{3,6,9}},3,3,5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1},{2},{3}},3,1,2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,2,3,4,5},{6,7,8,9,10}},2,5,6,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,3,4},{2,5,6},{7,8,9}},3,3,5,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{5,10,15,20,25},{30,35,40,45,50}},2,5,30,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3},{4,5,6},{7,8,9}},3,3,5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{11,13,15},{16,17,19},{21,23,25}},3,3,17,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{1},{1},{1}},3,1,1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{1,5},{2,6},{3,7},{4,8}},4,2,5,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{2,6,12,18},{3,7,13,19},{4,8,14,20},{5,9,15,21}},4,4,12,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def median(self, matrix, r, c):
        return 0
# USER_CODE_END
def test(g,r,c,e,tc,h=False):m=CodeCoder().median(g,r,c);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if m==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:mat={g}:exp={e}:got={m}"))
try:test([[1,3,5],[2,6,9],[3,6,9]],3,3,5,1)
except:print("TC:1:FAIL:hidden")
try:test([[1],[2],[3]],3,1,2,2)
except:print("TC:2:FAIL:hidden")
try:test([[1,2,3,4,5],[6,7,8,9,10]],2,5,6,3)
except:print("TC:3:FAIL:hidden")
try:test([[1,3,4],[2,5,6],[7,8,9]],3,3,5,4)
except:print("TC:4:FAIL:hidden")
try:test([[5,10,15,20,25],[30,35,40,45,50]],2,5,30,5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3],[4,5,6],[7,8,9]],3,3,5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[11,13,15],[16,17,19],[21,23,25]],3,3,17,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[1],[1],[1]],3,1,1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[1,5],[2,6],[3,7],[4,8]],4,2,5,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[2,6,12,18],[3,7,13,19],[4,8,14,20],[5,9,15,21]],4,4,12,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function median(matrix, r, c) { return 0; }
// USER_CODE_END
function test(g,r,c,e,tc,h){if(h===undefined)h=false;const m=median(g,r,c);if(m===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+m);}
try{test([[1,3,5],[2,6,9],[3,6,9]],3,3,5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1],[2],[3]],3,1,2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,2,3,4,5],[6,7,8,9,10]],2,5,6,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,3,4],[2,5,6],[7,8,9]],3,3,5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[5,10,15,20,25],[30,35,40,45,50]],2,5,30,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3],[4,5,6],[7,8,9]],3,3,5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[11,13,15],[16,17,19],[21,23,25]],3,3,17,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[1],[1],[1]],3,1,1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[1,5],[2,6],[3,7],[4,8]],4,2,5,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[2,6,12,18],[3,7,13,19],[4,8,14,20],[5,9,15,21]],4,4,12,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int median(int* arr,int r,int c) {
    // Write your code here — arr is the matrix flattened row-major (r rows, c cols)
    return 0;
}
// USER_CODE_END

void runTest(int* a,int r,int c,int e,int tc,int h){
    int m=median(a,r,c);
    if(m==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,m);}
}
int main(){
    int t1[]={1,3,5,2,6,9,3,6,9};runTest(t1,3,3,5,1,0);
    int t2[]={1,2,3};runTest(t2,3,1,2,2,0);
    int t3[]={1,2,3,4,5,6,7,8,9,10};runTest(t3,2,5,6,3,0);
    int t4[]={1,3,4,2,5,6,7,8,9};runTest(t4,3,3,5,4,0);
    int t5[]={5,10,15,20,25,30,35,40,45,50};runTest(t5,2,5,30,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9};runTest(t6,3,3,5,6,1);
    int t7[]={11,13,15,16,17,19,21,23,25};runTest(t7,3,3,17,7,1);
    int t8[]={1,1,1};runTest(t8,3,1,1,8,1);
    int t9[]={1,5,2,6,3,7,4,8};runTest(t9,4,2,5,9,1);
    int t10[]={2,6,12,18,3,7,13,19,4,8,14,20,5,9,15,21};runTest(t10,4,4,12,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
