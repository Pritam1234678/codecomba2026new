"""
Sum of Array
==============
Given an array arr of size n, compute the sum of all its elements.

Examples:
  arr = [1, 2, 3, 4, 5] → sum = 15
  arr = [-5, 10, -3] → sum = 2

Simply iterate through the array and accumulate sum.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sum of Array"
desc=(
    "Given an array arr of size n, compute the sum of all its elements and return it.\n\n"
    "For example:\n"
    "arr = [1, 2, 3, 4, 5] → 1+2+3+4+5 = 15\n"
    "arr = [-5, 10, -3] → (-5)+10+(-3) = 2\n\n"
    "Simple approach: initialize sum = 0, iterate through the array, "
    "add each element to sum, and return sum."
)
infmt="First line contains integer n.\nSecond line contains n space-separated integers."
outfmt="Print the sum of all elements."
cons="1 ≤ n ≤ 10^4\n-10^6 ≤ arr[i] ≤ 10^6\nSum fits in a 32-bit integer."
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n15"
e2="Input:\n3\n-5 10 -3\n\nOutput:\n2"
e3="Input:\n1\n100\n\nOutput:\n100"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int sumOfArray(int[] arr) {
        // Write your code here — accumulate sum
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] arr,int e,int tc,boolean h){int g=new CodeCoder().sumOfArray(arr);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(arr)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5},15,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{-5,10,-3},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{100},100,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-1,-2,-3,-4,-5},-15,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{0,0,0},0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1000,2000,3000},6000,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-1000000,1000000},0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1,1,1,1,1,1},10,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{999999,1},1000000,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{-999999,-1},-1000000,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int sumOfArray(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> arr,int e,int tc,bool h=false){int g=CodeCoder().sumOfArray(arr);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,3,4,5},15,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({-5,10,-3},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({100},100,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-1,-2,-3,-4,-5},-15,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({0,0,0},0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1000,2000,3000},6000,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-1000000,1000000},0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,1,1,1,1,1,1,1,1,1},10,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({999999,1},1000000,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({-999999,-1},-1000000,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sumOfArray(self, arr):
        return 0
# USER_CODE_END
def test(arr,e,tc,h=False):g=CodeCoder().sumOfArray(arr);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={arr}:exp={e}:got={g}"))
try:test([1,2,3,4,5],15,1)
except:print("TC:1:FAIL:hidden")
try:test([-5,10,-3],2,2)
except:print("TC:2:FAIL:hidden")
try:test([100],100,3)
except:print("TC:3:FAIL:hidden")
try:test([-1,-2,-3,-4,-5],-15,4)
except:print("TC:4:FAIL:hidden")
try:test([0,0,0],0,5)
except:print("TC:5:FAIL:hidden")
try:test([1000,2000,3000],6000,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-1000000,1000000],0,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,1,1,1,1,1,1,1,1,1],10,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([999999,1],1000000,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([-999999,-1],-1000000,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sumOfArray(arr) { return 0; }
// USER_CODE_END
function test(arr,e,tc,h){if(h===undefined)h=false;const g=sumOfArray(arr);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,2,3,4,5],15,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([-5,10,-3],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([100],100,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-1,-2,-3,-4,-5],-15,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([0,0,0],0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1000,2000,3000],6000,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-1000000,1000000],0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,1,1,1,1,1,1,1,1,1],10,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([999999,1],1000000,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([-999999,-1],-1000000,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int sumOfArray(int* arr, int n) {
    // Write your code here — accumulate sum
    return 0;
}
// USER_CODE_END

void runTest(int* arr,int n,int e,int tc,int h){
    int g=sumOfArray(arr,n);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
int t1[]={1,2,3,4,5};runTest(t1,5,15,1,0);
int t2[]={-5,10,-3};runTest(t2,3,2,2,0);
int t3[]={100};runTest(t3,1,100,3,0);
int t4[]={-1,-2,-3,-4,-5};runTest(t4,5,-15,4,0);
int t5[]={0,0,0};runTest(t5,3,0,5,0);
int t6[]={1000,2000,3000};runTest(t6,3,6000,6,1);
int t7[]={-1000000,1000000};runTest(t7,2,0,7,1);
int t8[]={1,1,1,1,1,1,1,1,1,1};runTest(t8,10,10,8,1);
int t9[]={999999,1};runTest(t9,2,1000000,9,1);
int t10[]={-999999,-1};runTest(t10,2,-1000000,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
