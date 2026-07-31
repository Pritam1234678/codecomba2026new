"""
First Missing Positive
=========================
Given an unsorted integer array arr, find the smallest missing positive integer.

Examples:
  arr = [1,2,0] → 3
  arr = [3,4,-1,1] → 2
  arr = [7,8,9,11,12] → 1

Use index-marking technique: place each positive integer at its correct index.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="First Missing Positive"
desc=(
    "Given an unsorted integer array arr, find the smallest missing positive integer.\n\n"
    "For example:\n"
    "arr = [1, 2, 0] → smallest missing positive is 3\n"
    "arr = [3, 4, -1, 1] → smallest missing positive is 2\n"
    "arr = [7, 8, 9, 11, 12] → smallest missing positive is 1\n\n"
    "Use the index-marking technique (cycle sort style):\n"
    "1. Ignore numbers that are negative or larger than n.\n"
    "2. Place each number 1..n at index (number-1) via swapping.\n"
    "3. Scan for the first index i where arr[i] != i+1. Return i+1.\n"
    "4. If all are correct, return n+1."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the smallest missing positive integer."
cons="1 ≤ n ≤ 5*10^4\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n3\n1 2 0\n\nOutput:\n3"
e2="Input:\n4\n3 4 -1 1\n\nOutput:\n2"
e3="Input:\n5\n7 8 9 11 12\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int firstMissingPositive(int[] arr) {
        // Write your code here — index marking
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().firstMissingPositive(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,0},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,4,-1,1},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{7,8,9,11,12},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-1,-2,0},1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,3,4,5,6,7},1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,0,0,1},2,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1000000000},1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,1,1,1},2,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int firstMissingPositive(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().firstMissingPositive(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,0},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,4,-1,1},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({7,8,9,11,12},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-1,-2,0},1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,3,4,5,6,7},1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,0,0,1},2,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1000000000},1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,1,1,1},2,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def firstMissingPositive(self, arr): return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().firstMissingPositive(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,2,0],3,1)
except:print("TC:1:FAIL:hidden")
try:test([3,4,-1,1],2,2)
except:print("TC:2:FAIL:hidden")
try:test([7,8,9,11,12],1,3)
except:print("TC:3:FAIL:hidden")
try:test([1],2,4)
except:print("TC:4:FAIL:hidden")
try:test([-1,-2,0],1,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],6,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([2,3,4,5,6,7],1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,0,1],2,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1000000000],1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,1,1,1],2,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function firstMissingPositive(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=firstMissingPositive(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,2,0],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,4,-1,1],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([7,8,9,11,12],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-1,-2,0],1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,3,4,5,6,7],1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,0,1],2,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1000000000],1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,1,1,1],2,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int firstMissingPositive(int* arr,int n){return 0;}
// USER_CODE_END
void run(int* a,int n,int e,int tc,int h){int g=firstMissingPositive(a,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int t1[]={1,2,0};run(t1,3,3,1,0);
int t2[]={3,4,-1,1};run(t2,4,2,2,0);
int t3[]={7,8,9,11,12};run(t3,5,1,3,0);
int t4[]={1};run(t4,1,2,4,0);
int t5[]={-1,-2,0};run(t5,3,1,5,0);
int t6[]={1,2,3,4,5};run(t6,5,6,6,1);
int t7[]={2,3,4,5,6,7};run(t7,6,1,7,1);
int t8[]={0,0,0,1};run(t8,4,2,8,1);
int t9[]={1000000000};run(t9,1,1,9,1);
int t10[]={1,1,1,1};run(t10,4,2,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
