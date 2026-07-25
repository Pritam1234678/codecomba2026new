"""
Move Zeroes to End
====================
Given an array arr, move all 0s to the end while maintaining relative order of non-zero elements.

Examples:
  arr = [0,1,0,3,12] → [1,3,12,0,0]
  arr = [0,0,1] → [1,0,0]

Two-pointer: nonZeroIdx tracks position for next non-zero element.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Move Zeroes to End"
desc=(
    "Given an array arr, move all 0s to the end of the array while maintaining "
    "the relative order of the non-zero elements.\n\n"
    "For example:\n"
    "arr = [0,1,0,3,12] → after moving zeroes: [1,3,12,0,0]\n"
    "arr = [0,0,1] → after moving zeroes: [1,0,0]\n\n"
    "Use a two-pointer approach: maintain a nonZeroIdx pointer that tracks where "
    "the next non-zero element should go. Iterate through the array; whenever "
    "you find a non-zero element, place it at nonZeroIdx and increment."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the array with zeroes moved to the end."
cons="1 ≤ n ≤ 10^4\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n0 1 0 3 12\n\nOutput:\n1 3 12 0 0"
e2="Input:\n3\n0 0 1\n\nOutput:\n1 0 0"
e3="Input:\n3\n1 2 3\n\nOutput:\n1 2 3"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder {
    public void moveZeroes(int[] arr) {
        // Write your code here — two-pointer, maintain relative order
    }
}
// USER_CODE_END
public class Main {
static void test(int[] a,int[] e,int tc,boolean h){
    int[] cp=Arrays.copyOf(a,a.length);new CodeCoder().moveZeroes(cp);
    if(Arrays.equals(cp,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(cp));
}
public static void main(String[] a){
try{test(new int[]{0,1,0,3,12},new int[]{1,3,12,0,0},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,0,1},new int[]{1,0,0},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3},new int[]{1,2,3},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{0,0,0},new int[]{0,0,0},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,1,0,0,2,0,3,0},new int[]{1,2,3,0,0,0,0,0},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{4,0,5,0,6},new int[]{4,5,6,0,0},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,0,-2,0,-3},new int[]{-1,-2,-3,0,0},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,1,0,0,2,0,0,3},new int[]{1,2,3,0,0,0,0,0,0},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{0},new int[]{0},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void moveZeroes(vector<int>& arr){}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){CodeCoder().moveZeroes(a);if(a==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({0,1,0,3,12},{1,3,12,0,0},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,0,1},{1,0,0},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3},{1,2,3},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({0,0,0},{0,0,0},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1},{1},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,1,0,0,2,0,3,0},{1,2,3,0,0,0,0,0},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({4,0,5,0,6},{4,5,6,0,0},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,0,-2,0,-3},{-1,-2,-3,0,0},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,1,0,0,2,0,0,3},{1,2,3,0,0,0,0,0,0},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({0},{0},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def moveZeroes(self, arr):
        pass
# USER_CODE_END
def test(a,e,tc,h=False):cp=a[:];CodeCoder().moveZeroes(cp);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if cp==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={cp}"))
try:test([0,1,0,3,12],[1,3,12,0,0],1)
except:print("TC:1:FAIL:hidden")
try:test([0,0,1],[1,0,0],2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3],[1,2,3],3)
except:print("TC:3:FAIL:hidden")
try:test([0,0,0],[0,0,0],4)
except:print("TC:4:FAIL:hidden")
try:test([1],[1],5)
except:print("TC:5:FAIL:hidden")
try:test([0,1,0,0,2,0,3,0],[1,2,3,0,0,0,0,0],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([4,0,5,0,6],[4,5,6,0,0],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1,0,-2,0,-3],[-1,-2,-3,0,0],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,1,0,0,2,0,0,3],[1,2,3,0,0,0,0,0,0],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0],[0],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function moveZeroes(arr) { }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const cp=[...a];moveZeroes(cp);const gs=JSON.stringify(cp),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);}
try{test([0,1,0,3,12],[1,3,12,0,0],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,0,1],[1,0,0],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3],[1,2,3],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([0,0,0],[0,0,0],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1],[1],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,1,0,0,2,0,3,0],[1,2,3,0,0,0,0,0],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([4,0,5,0,6],[4,5,6,0,0],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,0,-2,0,-3],[-1,-2,-3,0,0],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,1,0,0,2,0,0,3],[1,2,3,0,0,0,0,0,0],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0],[0],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void moveZeroes(int* arr,int n){}
// USER_CODE_END
int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int* a,int n,int* e,int en,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];moveZeroes(cp,n);if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:got=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",cp[i]);}printf("]\\n");}}}
int main(){
int t1[]={0,1,0,3,12},e1[]={1,3,12,0,0};run(t1,5,e1,5,1,0);
int t2[]={0,0,1},e2[]={1,0,0};run(t2,3,e2,3,2,0);
int t3[]={1,2,3},e3[]={1,2,3};run(t3,3,e3,3,3,0);
int t4[]={0,0,0},e4[]={0,0,0};run(t4,3,e4,3,4,0);
int t5[]={1},e5[]={1};run(t5,1,e5,1,5,0);
int t6[]={0,1,0,0,2,0,3,0},e6[]={1,2,3,0,0,0,0,0};run(t6,8,e6,8,6,1);
int t7[]={4,0,5,0,6},e7[]={4,5,6,0,0};run(t7,5,e7,5,7,1);
int t8[]={-1,0,-2,0,-3},e8[]={-1,-2,-3,0,0};run(t8,5,e8,5,8,1);
int t9[]={0,0,1,0,0,2,0,0,3},e9[]={1,2,3,0,0,0,0,0,0};run(t9,9,e9,9,9,1);
int t10[]={0},e10[]={0};run(t10,1,e10,1,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
