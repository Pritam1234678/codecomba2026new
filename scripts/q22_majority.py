"""
Majority Element I (Boyer-Moore)
==================================
Given an array arr of size n, find the majority element — the element that
appears more than n/2 times. The problem guarantees such an element exists.

Examples:
  arr = [3,2,3] → 3 (appears 2 times > 3/2)
  arr = [2,2,1,1,1,2,2] → 2

Boyer-Moore voting: candidate + count. Count cancels out non-majority.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Majority Element I"
desc=(
    "Given an array arr of size n, find the majority element. "
    "The majority element is the element that appears more than n/2 times. "
    "You may assume that the majority element always exists in the array.\n\n"
    "For example:\n"
    "arr = [3, 2, 3] → majority = 3 (appears 2 times, 2 > 3/2)\n"
    "arr = [2, 2, 1, 1, 1, 2, 2] → majority = 2\n\n"
    "Use the Boyer-Moore majority vote algorithm: maintain a candidate and a count. "
    "When count is 0, set candidate to current element. If current == candidate, "
    "increment count; otherwise decrement. The final candidate is the majority."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the majority element."
cons="1 ≤ n ≤ 5*10^4\n-10^9 ≤ arr[i] ≤ 10^9\nMajority element always exists."
e1="Input:\n3\n3 2 3\n\nOutput:\n3"
e2="Input:\n7\n2 2 1 1 1 2 2\n\nOutput:\n2"
e3="Input:\n1\n5\n\nOutput:\n5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"MEDIUM",True,"Array, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int majorityElement(int[] arr) {
        // Write your code here — Boyer-Moore voting
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int e,int tc,boolean h){int g=new CodeCoder().majorityElement(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{3,2,3},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,2,1,1,1,2,2},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5},5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,1,1,2,2,2,2},2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-1,-1,-1,5,5},-1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{6,6,6,6,6,6},6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,1,2,1,2,1,2,1},1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1000000000,1000000000,-1},1000000000,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,0,1,1},0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{3,3,3,3,2,2,2,2,3},3,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int majorityElement(vector<int>& arr){return 0;}};
// USER_CODE_END
void test(vector<int> a,int e,int tc,bool h=false){int g=CodeCoder().majorityElement(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({3,2,3},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,2,1,1,1,2,2},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5},5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,1,1,2,2,2,2},2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-1,-1,-1,5,5},-1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({6,6,6,6,6,6},6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,1,2,1,2,1,2,1},1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1000000000,1000000000,-1},1000000000,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,0,1,1},0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({3,3,3,3,2,2,2,2,3},3,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def majorityElement(self, arr): return 0
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().majorityElement(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([3,2,3],3,1)
except:print("TC:1:FAIL:hidden")
try:test([2,2,1,1,1,2,2],2,2)
except:print("TC:2:FAIL:hidden")
try:test([5],5,3)
except:print("TC:3:FAIL:hidden")
try:test([1,1,1,2,2,2,2],2,4)
except:print("TC:4:FAIL:hidden")
try:test([-1,-1,-1,5,5],-1,5)
except:print("TC:5:FAIL:hidden")
try:test([6,6,6,6,6,6],6,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,1,2,1,2,1,2,1],1,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1000000000,1000000000,-1],1000000000,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,0,1,1],0,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([3,3,3,3,2,2,2,2,3],3,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function majorityElement(arr) { return 0; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=majorityElement(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([3,2,3],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,2,1,1,1,2,2],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5],5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,1,1,2,2,2,2],2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-1,-1,-1,5,5],-1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([6,6,6,6,6,6],6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,1,2,1,2,1,2,1],1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1000000000,1000000000,-1],1000000000,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,0,1,1],0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([3,3,3,3,2,2,2,2,3],3,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
int majorityElement(int* arr,int n){return 0;}
// USER_CODE_END
void run(int* a,int n,int e,int tc,int h){int g=majorityElement(a,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}}
int main(){
int t1[]={3,2,3};run(t1,3,3,1,0);
int t2[]={2,2,1,1,1,2,2};run(t2,7,2,2,0);
int t3[]={5};run(t3,1,5,3,0);
int t4[]={1,1,1,2,2,2,2};run(t4,7,2,4,0);
int t5[]={-1,-1,-1,5,5};run(t5,5,-1,5,0);
int t6[]={6,6,6,6,6,6};run(t6,6,6,6,1);
int t7[]={1,2,1,2,1,2,1,2,1};run(t7,9,1,7,1);
int t8[]={1000000000,1000000000,-1};run(t8,3,1000000000,8,1);
int t9[]={0,0,0,1,1};run(t9,5,0,9,1);
int t10[]={3,3,3,3,2,2,2,2,3};run(t10,9,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
