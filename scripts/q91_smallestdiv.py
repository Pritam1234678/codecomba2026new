"""
Find the Smallest Divisor Given a Threshold
============================================
Given an array nums and an integer threshold, find the smallest positive
integer divisor d such that the sum over all elements of ceil(num / d) is less
than or equal to threshold.

Examples:
  nums = [1,2,5,9], threshold = 6  -> 5
  nums = [2,3,5,7,11], threshold = 11 -> 3

Binary search the divisor d in [1, max(nums)]. For a candidate d, total =
sum(ceil(num / d)) = sum((num + d - 1) / d). If total <= threshold, d is
feasible (try a smaller d); else d is too small. Return the smallest feasible d.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the nums array is passed with its length n: int* nums, int n, int threshold.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Find the Smallest Divisor Given a Threshold"
desc=(
    "Given an array nums and an integer threshold, return the smallest positive "
    "integer divisor d such that the sum over every element of ceil(num / d) is "
    "less than or equal to threshold.\n\n"
    "For example:\n"
    "nums = [1,2,5,9], threshold = 6       -> 5\n"
    "nums = [2,3,5,7,11], threshold = 11   -> 3\n\n"
    "Binary search the answer d in [1, max(nums)]. For a candidate d, total = "
    "sum(ceil(num / d)) = sum((num + d - 1) / d using integer division). If "
    "total <= threshold the divisor is feasible (try a smaller d); otherwise d "
    "is too small. Runs in O(n * log(max(nums)))."
)
infmt="First line contains n (array length) and threshold. Second line contains n space-separated integers."
outfmt="Print the smallest positive divisor d so that sum(ceil(num / d)) <= threshold."
cons="1 ≤ n ≤ 5*10^4\n1 ≤ nums[i] ≤ 10^6\nn ≤ threshold ≤ 10^6"
e1="Input:\n4 6\n1 2 5 9\n\nOutput:\n5"
e2="Input:\n5 11\n2 3 5 7 11\n\nOutput:\n3"
e3="Input:\n5 15\n1 2 3 4 5\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int smallestDivisor(int[] nums, int threshold) {
        // Write your code here — binary search the smallest feasible divisor
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] p,int t,int e,int tc,boolean h){int r=new CodeCoder().smallestDivisor(p,t);if(r==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:nums="+Arrays.toString(p)+":threshold="+t+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{1,2,5,9},6,5,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,3,5,7,11},11,3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},15,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{10,20,30},6,10,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{8,8,8,8},4,8,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,5,9},10,2,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,3,5,7,11},20,2,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{100},7,15,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{44,22,11},5,22,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,10,10,10},10,5,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int smallestDivisor(vector<int>& nums,int threshold){return 0;}};
// USER_CODE_END
void test(vector<int> p,int t,int e,int tc,bool h=false){int r=CodeCoder().smallestDivisor(p,t);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({1,2,5,9},6,5,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,3,5,7,11},11,3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3,4,5},15,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({10,20,30},6,10,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({8,8,8,8},4,8,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,5,9},10,2,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,3,5,7,11},20,2,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({100},7,15,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({44,22,11},5,22,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,10,10,10},10,5,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def smallestDivisor(self, nums, threshold):
        return 0
# USER_CODE_END
def test(p,t,e,tc,h=False):r=CodeCoder().smallestDivisor(p,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if r==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:nums={p}:threshold={t}:exp={e}:got={r}"))
try:test([1,2,5,9],6,5,1)
except:print("TC:1:FAIL:hidden")
try:test([2,3,5,7,11],11,3,2)
except:print("TC:2:FAIL:hidden")
try:test([1,2,3,4,5],15,1,3)
except:print("TC:3:FAIL:hidden")
try:test([10,20,30],6,10,4)
except:print("TC:4:FAIL:hidden")
try:test([8,8,8,8],4,8,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,5,9],10,2,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([2,3,5,7,11],20,2,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([100],7,15,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([44,22,11],5,22,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,10,10,10],10,5,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function smallestDivisor(nums, threshold) { return 0; }
// USER_CODE_END
function test(p,t,e,tc,h){if(h===undefined)h=false;const r=smallestDivisor(p,t);if(r===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([1,2,5,9],6,5,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,3,5,7,11],11,3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3,4,5],15,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([10,20,30],6,10,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([8,8,8,8],4,8,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,5,9],10,2,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,3,5,7,11],20,2,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100],7,15,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([44,22,11],5,22,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,10,10,10],10,5,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int smallestDivisor(int* nums,int n,int threshold) {
    // Write your code here — return the smallest feasible divisor d
    return 0;
}
// USER_CODE_END

void runTest(int* p,int n,int t,int e,int tc,int h){
    int r=smallestDivisor(p,n,t);
    if(r==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={1,2,5,9};runTest(t1,4,6,5,1,0);
    int t2[]={2,3,5,7,11};runTest(t2,5,11,3,2,0);
    int t3[]={1,2,3,4,5};runTest(t3,5,15,1,3,0);
    int t4[]={10,20,30};runTest(t4,3,6,10,4,0);
    int t5[]={8,8,8,8};runTest(t5,4,4,8,5,0);
    int t6[]={1,2,5,9};runTest(t6,4,10,2,6,1);
    int t7[]={2,3,5,7,11};runTest(t7,5,20,2,7,1);
    int t8[]={100};runTest(t8,1,7,15,8,1);
    int t9[]={44,22,11};runTest(t9,3,5,22,9,1);
    int t10[]={10,10,10,10};runTest(t10,4,10,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
