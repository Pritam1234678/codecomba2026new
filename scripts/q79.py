"""
Longest Increasing Subsequence
================================
Given an integer array nums, return the length of the longest strictly increasing
subsequence (LIS). A subsequence is a sequence that can be derived from the array
by deleting some or no elements without changing the order of the remaining elements.

Examples:
  nums = [10,9,2,5,3,7,101,18] → LIS = [2,5,7,101], length = 4
  nums = [0,1,0,3,2,3] → LIS = [0,1,2,3], length = 4
  nums = [7,7,7,7,7,7] → LIS = [7], length = 1

DP approach: dp[i] = LIS ending at i.
  For each j < i, if nums[j] < nums[i], dp[i] = max(dp[i], dp[j] + 1)

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Longest Increasing Subsequence"
desc=(
    "Given an integer array nums, return the length of the longest strictly increasing "
    "subsequence (LIS).\n\n"
    "A subsequence is obtained by deleting some elements without changing the order. "
    "For example, [2,5,7,101] is a subsequence of [10,9,2,5,3,7,101,18] because "
    "2 < 5 < 7 < 101 and they appear in that order.\n\n"
    "A classic DP solution: let dp[i] be the LIS length ending at index i. "
    "For each i, check every j < i; if nums[j] < nums[i], dp[i] = max(dp[i], dp[j] + 1). "
    "The answer is the max value in dp.\n\n"
    "For an O(n log n) approach, maintain a patience-sorting tails array."
)
infmt="First line contains integer n.\nSecond line contains n space-separated integers."
outfmt="Print the length of the longest increasing subsequence."
cons="1 \u2264 n \u2264 2500\n-10^4 \u2264 nums[i] \u2264 10^4"
e1="Input:\n8\n10 9 2 5 3 7 101 18\n\nOutput:\n4\n\nExplanation: [2,5,7,101] length 4."
e2="Input:\n6\n0 1 0 3 2 3\n\nOutput:\n4\n\nExplanation: [0,1,2,3] length 4."
e3="Input:\n6\n7 7 7 7 7 7\n\nOutput:\n1\n\nExplanation: All same, LIS = [7] length 1."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Dynamic Programming, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int lengthOfLIS(int[] nums) {
        // Write your code here — DP or patience sorting
        return 0;
    }
}
// USER_CODE_END
public class Main {
static void test(int[] n,int e,int tc,boolean h){int g=new CodeCoder().lengthOfLIS(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{10,9,2,5,3,7,101,18},4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,1,0,3,2,3},4,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{7,7,7,7,7,7},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},5,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,3,6,7,9,4,10,5,6},6,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{3,10,2,1,20},3,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,20,10,30,20,40},4,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{-10,-5,0,5,10,15,20,25,30},9,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int lengthOfLIS(vector<int>& n){return 0;}};
// USER_CODE_END
void test(vector<int> n,int e,int tc,bool h=false){int g=CodeCoder().lengthOfLIS(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({10,9,2,5,3,7,101,18},4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,1,0,3,2,3},4,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({7,7,7,7,7,7},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},5,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({5,4,3,2,1},1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,3,6,7,9,4,10,5,6},6,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({3,10,2,1,20},3,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,20,10,30,20,40},4,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({-10,-5,0,5,10,15,20,25,30},9,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def lengthOfLIS(self, nums):
        return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().lengthOfLIS(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test([10,9,2,5,3,7,101,18],4,1)
except:print("TC:1:FAIL:hidden")
try:test([0,1,0,3,2,3],4,2)
except:print("TC:2:FAIL:hidden")
try:test([7,7,7,7,7,7],1,3)
except:print("TC:3:FAIL:hidden")
try:test([1],1,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],5,5)
except:print("TC:5:FAIL:hidden")
try:test([5,4,3,2,1],1,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,3,6,7,9,4,10,5,6],6,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([3,10,2,1,20],3,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([10,20,10,30,20,40],4,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([-10,-5,0,5,10,15,20,25,30],9,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code=r'''// USER_CODE_START
function lengthOfLIS(nums) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=lengthOfLIS(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+JSON.stringify(n)+":expected="+e+":got="+g);}
try{test([10,9,2,5,3,7,101,18],4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,1,0,3,2,3],4,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([7,7,7,7,7,7],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],5,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([5,4,3,2,1],1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,3,6,7,9,4,10,5,6],6,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([3,10,2,1,20],3,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,20,10,30,20,40],4,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([-10,-5,0,5,10,15,20,25,30],9,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code=r'''#include <stdio.h>
// USER_CODE_START
int lengthOfLIS(int* n,int s){return 0;}
// USER_CODE_END
void runTest(int* n,int s,int e,int tc,int h){int g=lengthOfLIS(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:expected=%d:got=%d\\n",tc,e,g);}}
int main(){
int t1[]={10,9,2,5,3,7,101,18};runTest(t1,8,4,1,0);
int t2[]={0,1,0,3,2,3};runTest(t2,6,4,2,0);
int t3[]={7,7,7,7,7,7};runTest(t3,6,1,3,0);
int t4[]={1};runTest(t4,1,1,4,0);
int t5[]={1,2,3,4,5};runTest(t5,5,5,5,0);
int t6[]={5,4,3,2,1};runTest(t6,5,1,6,1);
int t7[]={1,3,6,7,9,4,10,5,6};runTest(t7,9,6,7,1);
int t8[]={3,10,2,1,20};runTest(t8,5,3,8,1);
int t9[]={10,20,10,30,20,40};runTest(t9,6,4,9,1);
int t10[]={-10,-5,0,5,10,15,20,25,30};runTest(t10,9,9,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
