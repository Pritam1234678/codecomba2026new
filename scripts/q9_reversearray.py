"""
Reverse an Array
==================
Given an array arr of size n, reverse it in-place.

Examples:
  arr = [1, 2, 3, 4, 5] → [5, 4, 3, 2, 1]
  arr = [10, 20] → [20, 10]

Use two-pointer swap: left=0, right=n-1, swap and move inward.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder (returns reversed array)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reverse an Array"
desc=(
    "Given an array arr of size n, reverse it in-place and return the reversed array.\n\n"
    "For example:\n"
    "arr = [1, 2, 3, 4, 5] → reversed = [5, 4, 3, 2, 1]\n"
    "arr = [10, 20] → reversed = [20, 10]\n\n"
    "Use two pointers: left = 0, right = n-1. Swap arr[left] and arr[right], "
    "then left++, right-- until left >= right."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the reversed array as space-separated integers."
cons="1 ≤ n ≤ 10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n5 4 3 2 1"
e2="Input:\n2\n10 20\n\nOutput:\n20 10"
e3="Input:\n1\n42\n\nOutput:\n42"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] reverseArray(int[] arr) {
        // Write your code here — reverse in-place and return
        return arr;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean h){int[] g=new CodeCoder().reverseArray(java.util.Arrays.copyOf(a,a.length));if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5},new int[]{5,4,3,2,1},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10,20},new int[]{20,10},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{42},new int[]{42},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-5,-4,-3},new int[]{-3,-4,-5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,1},new int[]{1,1,1},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,3,5,7,9},new int[]{9,7,5,3,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{100,200,300,400},new int[]{400,300,200,100},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,0,1},new int[]{1,0,-1},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,10,15,20,25,30},new int[]{30,25,20,15,10,5},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{0,0,0,0},new int[]{0,0,0,0},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> reverseArray(vector<int>& arr){return arr;}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){auto g=CodeCoder().reverseArray(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,2,3,4,5},{5,4,3,2,1},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10,20},{20,10},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({42},{42},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-5,-4,-3},{-3,-4,-5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,1},{1,1,1},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,3,5,7,9},{9,7,5,3,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({100,200,300,400},{400,300,200,100},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,0,1},{1,0,-1},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,10,15,20,25,30},{30,25,20,15,10,5},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({0,0,0,0},{0,0,0,0},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def reverseArray(self, arr):
        return arr
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().reverseArray(a[:]);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,2,3,4,5],[5,4,3,2,1],1)
except:print("TC:1:FAIL:hidden")
try:test([10,20],[20,10],2)
except:print("TC:2:FAIL:hidden")
try:test([42],[42],3)
except:print("TC:3:FAIL:hidden")
try:test([-5,-4,-3],[-3,-4,-5],4)
except:print("TC:4:FAIL:hidden")
try:test([1,1,1],[1,1,1],5)
except:print("TC:5:FAIL:hidden")
try:test([1,3,5,7,9],[9,7,5,3,1],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([100,200,300,400],[400,300,200,100],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1,0,1],[1,0,-1],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,10,15,20,25,30],[30,25,20,15,10,5],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0,0,0,0],[0,0,0,0],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function reverseArray(arr) { return arr; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=reverseArray([...a]);const gs=JSON.stringify(g),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+es+":got="+gs);}
try{test([1,2,3,4,5],[5,4,3,2,1],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10,20],[20,10],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([42],[42],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-5,-4,-3],[-3,-4,-5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,1],[1,1,1],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,3,5,7,9],[9,7,5,3,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([100,200,300,400],[400,300,200,100],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,0,1],[1,0,-1],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,10,15,20,25,30],[30,25,20,15,10,5],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0,0,0,0],[0,0,0,0],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
void reverseArray(int* arr,int n) {
    // Write your code here — reverse in-place
}
// USER_CODE_END

int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int* a,int n,int* e,int en,int tc,int h){
    int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];
    reverseArray(cp,n);
    if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}
}
int main(){
int t1[]={1,2,3,4,5},e1[]={5,4,3,2,1};run(t1,5,e1,5,1,0);
int t2[]={10,20},e2[]={20,10};run(t2,2,e2,2,2,0);
int t3[]={42},e3[]={42};run(t3,1,e3,1,3,0);
int t4[]={-5,-4,-3},e4[]={-3,-4,-5};run(t4,3,e4,3,4,0);
int t5[]={1,1,1},e5[]={1,1,1};run(t5,3,e5,3,5,0);
int t6[]={1,3,5,7,9},e6[]={9,7,5,3,1};run(t6,5,e6,5,6,1);
int t7[]={100,200,300,400},e7[]={400,300,200,100};run(t7,4,e7,4,7,1);
int t8[]={-1,0,1},e8[]={1,0,-1};run(t8,3,e8,3,8,1);
int t9[]={5,10,15,20,25,30},e9[]={30,25,20,15,10,5};run(t9,6,e9,6,9,1);
int t10[]={0,0,0,0},e10[]={0,0,0,0};run(t10,4,e10,4,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
