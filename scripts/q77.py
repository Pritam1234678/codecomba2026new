"""
House Robber
=============
You are a professional robber planning to rob houses along a street.
Each house has a certain amount of money stashed. The only constraint is
that adjacent houses have security systems connected, so you cannot rob
two adjacent houses in the same night.

Given an integer array nums representing the money at each house,
return the maximum amount of money you can rob without alerting the police.

Examples:
  nums = [1, 2, 3, 1] → max = 4 (rob house 1 → 1, house 3 → 3, total 4)
  nums = [2, 7, 9, 3, 1] → max = 12 (rob house 1 → 2, house 3 → 9, house 5 → 1)

DP approach: dp[i] = max money up to house i.
  Either skip house i: dp[i-1]
  Or rob house i: nums[i] + dp[i-2]
  dp[i] = max(dp[i-1], nums[i] + dp[i-2])

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="House Robber"
desc=(
    "You are a professional robber planning to rob houses along a street. "
    "Each house has a certain amount of money stashed. The only constraint stopping you "
    "is that adjacent houses have security systems connected — if you rob two adjacent "
    "houses on the same night, the police will be alerted.\n\n"
    "Given an integer array nums where nums[i] represents the amount of money at house i, "
    "return the maximum amount of money you can rob without alerting the police.\n\n"
    "For example:\n"
    "- nums = [1, 2, 3, 1]: rob house 0 (1) and house 2 (3) = 4. "
    "Or rob house 1 (2) and house 3 (1) = 3. Max is 4.\n"
    "- nums = [2, 7, 9, 3, 1]: rob house 0 (2), house 2 (9), house 4 (1) = 12.\n\n"
    "DP formula: for each house, you either skip it (keep previous max) or rob it "
    "(its value + max from 2 houses ago)."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the maximum amount you can rob."
cons="1 \u2264 n \u2264 100\n0 \u2264 nums[i] \u2264 400"
e1="Input:\n4\n1 2 3 1\n\nOutput:\n4\n\nExplanation: Rob house 0 (1) and house 2 (3) = 4."
e2="Input:\n5\n2 7 9 3 1\n\nOutput:\n12\n\nExplanation: Rob house 0 (2), house 2 (9), house 4 (1) = 12."
e3="Input:\n1\n5\n\nOutput:\n5\n\nExplanation: Only one house to rob."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Dynamic Programming",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int rob(int[] nums) {
        // Write your code here — DP: skip or rob
        return 0;
    }
}
// USER_CODE_END
public class Main {
static void test(int[] n,int e,int tc,boolean h){int g=new CodeCoder().rob(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,3,1},4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,7,9,3,1},12,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5},5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2},2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2,1,1,2},4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,1,1,10,1,10,1,1,10},30,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0,0,0,0},0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{400,399,398,397,396},1200,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,3,1,3,100},103,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int rob(vector<int>& n){return 0;}};
// USER_CODE_END
void test(vector<int> n,int e,int tc,bool h=false){int g=CodeCoder().rob(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({1,2,3,1},4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,7,9,3,1},12,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5},5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2},2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2,1,1,2},4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,1,1,10,1,10,1,1,10},30,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0,0,0,0},0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({400,399,398,397,396},1200,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,3,1,3,100},103,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def rob(self, nums):
        return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().rob(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test([1,2,3,1],4,1)
except:print("TC:1:FAIL:hidden")
try:test([2,7,9,3,1],12,2)
except:print("TC:2:FAIL:hidden")
try:test([5],5,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2],2,4)
except:print("TC:4:FAIL:hidden")
try:test([2,1,1,2],4,5)
except:print("TC:5:FAIL:hidden")
try:test([],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,1,1,10,1,10,1,1,10],30,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,0,0],0,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([400,399,398,397,396],1200,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,3,1,3,100],103,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code=r'''// USER_CODE_START
function rob(nums) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=rob(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+JSON.stringify(n)+":expected="+e+":got="+g);}
try{test([1,2,3,1],4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,7,9,3,1],12,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5],5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2],2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2,1,1,2],4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,1,1,10,1,10,1,1,10],30,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,0,0],0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([400,399,398,397,396],1200,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,3,1,3,100],103,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code=r'''#include <stdio.h>
// USER_CODE_START
int rob(int* n,int s){return 0;}
// USER_CODE_END
void runTest(int* n,int s,int e,int tc,int h){int g=rob(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:expected=%d:got=%d\\n",tc,e,g);}}
int main(){
int t1[]={1,2,3,1};runTest(t1,4,4,1,0);
int t2[]={2,7,9,3,1};runTest(t2,5,12,2,0);
int t3[]={5};runTest(t3,1,5,3,0);
int t4[]={1,2};runTest(t4,2,2,4,0);
int t5[]={2,1,1,2};runTest(t5,4,4,5,0);
int t6[]={};runTest(t6,0,0,6,1);
int t7[]={10,1,1,10,1,10,1,1,10};runTest(t7,9,30,7,1);
int t8[]={0,0,0,0};runTest(t8,4,0,8,1);
int t9[]={400,399,398,397,396};runTest(t9,5,1200,9,1);
int t10[]={1,3,1,3,100};runTest(t10,5,103,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
