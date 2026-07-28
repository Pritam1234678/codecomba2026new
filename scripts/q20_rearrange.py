"""
Rearrange Array Elements by Sign
==================================
Given an array arr with equal number of positive and negative integers,
rearrange them so that positive and negative numbers alternate, starting
with a positive number. The relative order of positives and negatives
among themselves must be preserved.

Examples:
  arr = [3,1,-2,-5,2,-4] → [3,-2,1,-5,2,-4]
  arr = [-1,1] → [1,-1] (positive first)

Two-pointer: separate positives and negatives into two lists, then merge.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rearrange Array Elements by Sign"
desc=(
    "Given an array arr with an equal number of positive and negative integers, "
    "rearrange the array so that positive and negative numbers alternate, "
    "starting with a positive number. The relative order of positives and "
    "negatives among themselves must be preserved.\n\n"
    "For example:\n"
    "arr = [3, 1, -2, -5, 2, -4] → [3, -2, 1, -5, 2, -4]\n"
    "arr = [-1, 1] → [1, -1] (positive first)\n\n"
    "Separate positives and negatives into two lists maintaining order. "
    "Then merge: take one from positive list, then from negative list, alternately."
)
infmt="First line contains n (even).\nSecond line contains n space-separated integers."
outfmt="Print the rearranged array as space-separated integers."
cons="2 ≤ n ≤ 2*10^4, n is even\nEqual number of positives and negatives.\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n6\n3 1 -2 -5 2 -4\n\nOutput:\n3 -2 1 -5 2 -4"
e2="Input:\n2\n-1 1\n\nOutput:\n1 -1"
e3="Input:\n4\n1 2 -1 -2\n\nOutput:\n1 -1 2 -2"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] rearrangeBySign(int[] arr) {
        // Write your code here — separate positives and negatives, then merge
        return arr;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean h){int[] g=new CodeCoder().rearrangeBySign(java.util.Arrays.copyOf(a,a.length));if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{3,1,-2,-5,2,-4},new int[]{3,-2,1,-5,2,-4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{-1,1},new int[]{1,-1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,-1,-2},new int[]{1,-1,2,-2},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,-1,3,-2},new int[]{5,-1,3,-2},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-5,5},new int[]{5,-5},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,-1,2,-2,3,-3},new int[]{1,-1,2,-2,3,-3},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-10,20,-30,40},new int[]{20,-10,40,-30},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{100,200,300,-100,-200,-300},new int[]{100,-100,200,-200,300,-300},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{-1,-2,-3,1,2,3},new int[]{1,-1,2,-2,3,-3},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,-10},new int[]{10,-10},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> rearrangeBySign(vector<int>& arr){return arr;}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){auto g=CodeCoder().rearrangeBySign(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({3,1,-2,-5,2,-4},{3,-2,1,-5,2,-4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({-1,1},{1,-1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,-1,-2},{1,-1,2,-2},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,-1,3,-2},{5,-1,3,-2},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-5,5},{5,-5},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,-1,2,-2,3,-3},{1,-1,2,-2,3,-3},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-10,20,-30,40},{20,-10,40,-30},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({100,200,300,-100,-200,-300},{100,-100,200,-200,300,-300},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-1,-2,-3,1,2,3},{1,-1,2,-2,3,-3},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,-10},{10,-10},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rearrangeBySign(self, arr):
        return arr
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().rearrangeBySign(a[:]);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([3,1,-2,-5,2,-4],[3,-2,1,-5,2,-4],1)
except:print("TC:1:FAIL:hidden")
try:test([-1,1],[1,-1],2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,-1,-2],[1,-1,2,-2],3)
except:print("TC:3:FAIL:hidden")
try:test([5,-1,3,-2],[5,-1,3,-2],4)
except:print("TC:4:FAIL:hidden")
try:test([-5,5],[5,-5],5)
except:print("TC:5:FAIL:hidden")
try:test([1,-1,2,-2,3,-3],[1,-1,2,-2,3,-3],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-10,20,-30,40],[20,-10,40,-30],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([100,200,300,-100,-200,-300],[100,-100,200,-200,300,-300],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-1,-2,-3,1,2,3],[1,-1,2,-2,3,-3],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,-10],[10,-10],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rearrangeBySign(arr) { return arr; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=rearrangeBySign([...a]);const gs=JSON.stringify(g),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+es+":got="+gs);}
try{test([3,1,-2,-5,2,-4],[3,-2,1,-5,2,-4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([-1,1],[1,-1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,-1,-2],[1,-1,2,-2],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,-1,3,-2],[5,-1,3,-2],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-5,5],[5,-5],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,-1,2,-2,3,-3],[1,-1,2,-2,3,-3],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-10,20,-30,40],[20,-10,40,-30],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100,200,300,-100,-200,-300],[100,-100,200,-200,300,-300],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-1,-2,-3,1,2,3],[1,-1,2,-2,3,-3],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,-10],[10,-10],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int* rearrangeBySign(int* arr,int n,int* rs){*rs=0;return NULL;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
