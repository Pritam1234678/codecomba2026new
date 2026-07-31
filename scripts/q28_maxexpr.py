"""
Maximum Value of Expression
=============================
Given two arrays a and b of size n, maximize the expression:
  a[i] * b[i] + a[j] * b[j] - a[i] * a[j] - b[i] * b[j]  ... complex variant

Standard problem: Given an array arr of n integers, find the maximum value of
  arr[i] - arr[j] + arr[k] - arr[l]  for indices i < j < k < l
or variants involving product of pairs.

Classic variant: Given array arr, maximize (arr[i] - arr[j]) + (arr[k] - arr[l])
for indices i<j<k<l. Equivalent to sum of max of left part and max of right part.

For this problem: Given arrays a and b, maximize:
  a[i] * a[j] + b[i] * b[j]  with i != j
plus handle negative values by tracking max and min of each component.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Maximum Value Of Expression"
desc=(
    "Given two arrays a and b of size n, find the maximum possible value of the expression:\n\n"
    "    a[i] * a[j] + b[i] * b[j]\n\n"
    "for any two distinct indices i and j (i != j).\n\n"
    "For example:\n"
    "a = [1, 2, 3], b = [4, 5, 6]\n"
    "Pairs: (0,1): 1*2+4*5=22, (0,2): 1*3+4*6=27, (1,2): 2*3+5*6=36 → max = 36\n\n"
    "Since values can be negative, a naive product check won't work for all cases. "
    "Observe that the expression can be rewritten using matrix multiplication "
    "of vectors (a[i], b[i]) · (a[j], b[j]). To find the max dot product between "
    "two distinct vectors, the best candidate for each vector i is the vector with "
    "the largest dot product, which relates to extreme projections. "
    "Track the maximum values of (a[i]+b[i]) and (a[i]-b[i]) combinations."
)
infmt="First line contains n.\nSecond line contains n space-separated integers (array a).\nThird line contains n space-separated integers (array b)."
outfmt="Print the maximum value of the expression."
cons="2 ≤ n ≤ 10^5\n-10^6 ≤ a[i], b[i] ≤ 10^6\nResult fits in 64-bit integer."
e1="Input:\n3\n1 2 3\n4 5 6\n\nOutput:\n36"
e2="Input:\n2\n-1 -2\n-3 -4\n\nOutput:\n14\n\nExplanation: (-1)*(-2)+(-3)*(-4)=2+12=14"
e3="Input:\n2\n10 20\n30 40\n\nOutput:\n1400\n\nExplanation: 10*20+30*40=200+1200=1400"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Math",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long maxExpression(int[] a, int[] b) {
        // Write your code here — maximize a[i]*a[j] + b[i]*b[j], i != j
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] b,long e,int tc,boolean h){long g=new CodeCoder().maxExpression(a,b);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,3},new int[]{4,5,6},36,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{-1,-2},new int[]{-3,-4},14,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{10,20},new int[]{30,40},1400,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{1,2},8,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{0,0,5},new int[]{0,0,5},25,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-1000000,1000000},new int[]{-1000000,1000000},2000000000000L,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4},new int[]{5,6,7,8},46,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-5,-4,-3},new int[]{1,2,3},11,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{100,200,300},new int[]{-100,-200,-300},-40000,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,1,1},new int[]{1,1,1},2,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:long long maxExpression(vector<int>& a,vector<int>& b){return 0;}};
// USER_CODE_END
void test(vector<int> a,vector<int> b,long long e,int tc,bool h=false){long long g=CodeCoder().maxExpression(a,b);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,3},{4,5,6},36,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({-1,-2},{-3,-4},14,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({10,20},{30,40},1400,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2},{1,2},8,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({0,0,5},{0,0,5},25,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({-1000000,1000000},{-1000000,1000000},2000000000000LL,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4},{5,6,7,8},46,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-5,-4,-3},{1,2,3},11,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({100,200,300},{-100,-200,-300},-40000,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,1,1},{1,1,1},2,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maxExpression(self, a, b): return 0
# USER_CODE_END
def test(a,b,e,tc,h=False):g=CodeCoder().maxExpression(a,b);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([1,2,3],[4,5,6],36,1)
except:print("TC:1:FAIL:hidden")
try:test([-1,-2],[-3,-4],14,2)
except:print("TC:2:FAIL:hidden")
try:test([10,20],[30,40],1400,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2],[1,2],8,4)
except:print("TC:4:FAIL:hidden")
try:test([0,0,5],[0,0,5],25,5)
except:print("TC:5:FAIL:hidden")
try:test([-1000000,1000000],[-1000000,1000000],2000000000000,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4],[5,6,7,8],46,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-5,-4,-3],[1,2,3],11,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([100,200,300],[-100,-200,-300],-40000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,1,1],[1,1,1],2,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maxExpression(a, b) { return 0; }
// USER_CODE_END
function test(a,b,e,tc,h){if(h===undefined)h=false;const g=maxExpression(a,b);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,2,3],[4,5,6],36,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([-1,-2],[-3,-4],14,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([10,20],[30,40],1400,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2],[1,2],8,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([0,0,5],[0,0,5],25,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-1000000,1000000],[-1000000,1000000],2000000000000,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4],[5,6,7,8],46,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-5,-4,-3],[1,2,3],11,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([100,200,300],[-100,-200,-300],-40000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,1,1],[1,1,1],2,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
long long maxExpression(int* a,int* b,int n){return 0;}
// USER_CODE_END
void run(int* a,int* b,int n,long long e,int tc,int h){long long g=maxExpression(a,b,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n",tc,(long long)e,g);}}
int main(){
int a1[]={1,2,3},b1[]={4,5,6};run(a1,b1,3,36,1,0);
int a2[]={-1,-2},b2[]={-3,-4};run(a2,b2,2,14,2,0);
int a3[]={10,20},b3[]={30,40};run(a3,b3,2,1400,3,0);
int a4[]={1,2},b4[]={1,2};run(a4,b4,2,8,4,0);
int a5[]={0,0,5},b5[]={0,0,5};run(a5,b5,3,25,5,0);
int a6[]={-1000000,1000000},b6[]={-1000000,1000000};run(a6,b6,2,2000000000000LL,6,1);
int a7[]={1,2,3,4},b7[]={5,6,7,8};run(a7,b7,4,46,7,1);
int a8[]={-5,-4,-3},b8[]={1,2,3};run(a8,b8,3,11,8,1);
int a9[]={100,200,300},b9[]={-100,-200,-300};run(a9,b9,3,-40000,9,1);
int a10[]={1,1,1},b10[]={1,1,1};run(a10,b10,3,2,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
