"""
Jump Game
===========
You are given an integer array nums. You start at index 0. Each element
nums[i] represents the maximum jump length from position i.

Return true if you can reach the last index, otherwise false.

Examples:
  nums = [2,3,1,1,4] → true  (jump 2→index2, then 1→index3, then 1→index4)
  nums = [3,2,1,0,4] → false (stuck at index 3 with value 0)

Approach: Greedy — track the furthest reachable index. Iterate through
array, update maxReach = max(maxReach, i + nums[i]). If i > maxReach, return false.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Jump Game"
desc=(
    "You are given an integer array nums. You start at index 0 and each element "
    "nums[i] represents the maximum jump length forward from position i.\n\n"
    "Return true if you can reach the last index, otherwise return false.\n\n"
    "For example:\n"
    "nums = [2,3,1,1,4]\n"
    "  - From index 0, you can jump up to 2 steps. Jump to index 1 (value 3).\n"
    "  - From index 1, you can jump up to 3 steps. Jump directly to index 4 (last).\n"
    "  → true\n\n"
    "nums = [3,2,1,0,4]\n"
    "  - From index 0, jump to index 3 (value 0). From index 3 you cannot move.\n"
    "  - Any other path also ends up stuck at index 3.\n"
    "  → false\n\n"
    "Greedy approach: maintain a variable maxReach = furthest index reachable. "
    "Iterate through the array. At each position i, if i > maxReach, return false. "
    "Otherwise update maxReach = max(maxReach, i + nums[i]). If maxReach >= last index, return true."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print 'true' if you can reach the last index, otherwise 'false'."
cons="1 ≤ n ≤ 10^4\n0 ≤ nums[i] ≤ 10^5"
e1="Input:\n5\n2 3 1 1 4\n\nOutput:\ntrue\n\nExplanation: Jump 0→1→4, or 0→2→3→4."
e2="Input:\n5\n3 2 1 0 4\n\nOutput:\nfalse\n\nExplanation: Stuck at index 3 with value 0, cannot reach last."
e3="Input:\n1\n0\n\nOutput:\ntrue\n\nExplanation: Already at last index."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Greedy, DP",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean canJump(int[] nums) {
        // Write your code here — greedy max reach
        return false;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] n,boolean e,int tc,boolean h){boolean g=new CodeCoder().canJump(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{2,3,1,1,4},true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,2,1,0,4},false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3},true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,1},false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,0,0},true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1,0,0},false,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,0,0,0,0,0,0,0,0,0,0},true,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,0,1,0,1},false,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool canJump(vector<int>& n){return false;}};
// USER_CODE_END
void test(vector<int> n,bool e,int tc,bool h=false){bool g=CodeCoder().canJump(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test({2,3,1,1,4},true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,2,1,0,4},false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3},true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,1},false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,0,0},true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({5,4,3,2,1,0,0},false,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,0,0,0,0,0,0,0,0,0,0},true,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,0,1,0,1},false,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def canJump(self, nums):
        return False
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().canJump(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={e}:got={g}"))
try:test([2,3,1,1,4],True,1)
except:print("TC:1:FAIL:hidden")
try:test([3,2,1,0,4],False,2)
except:print("TC:2:FAIL:hidden")
try:test([0],True,3)
except:print("TC:3:FAIL:hidden")
try:test([1],True,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3],True,5)
except:print("TC:5:FAIL:hidden")
try:test([0,1],False,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([2,0,0],True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([5,4,3,2,1,0,0],False,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([10,0,0,0,0,0,0,0,0,0,0],True,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,0,1,0,1],False,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function canJump(nums) { return false; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=canJump(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([2,3,1,1,4],true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,2,1,0,4],false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3],true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,1],false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,0,0],true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([5,4,3,2,1,0,0],false,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,0,0,0,0,0,0,0,0,0,0],true,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,0,1,0,1],false,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool canJump(int* n,int s){return false;}
// USER_CODE_END
void run(int* n,int s,bool e,int tc,int h){bool g=canJump(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}}
int main(){
int t1[]={2,3,1,1,4};run(t1,5,true,1,0);
int t2[]={3,2,1,0,4};run(t2,5,false,2,0);
int t3[]={0};run(t3,1,true,3,0);
int t4[]={1};run(t4,1,true,4,0);
int t5[]={1,2,3};run(t5,3,true,5,0);
int t6[]={0,1};run(t6,2,false,6,1);
int t7[]={2,0,0};run(t7,3,true,7,1);
int t8[]={5,4,3,2,1,0,0};run(t8,7,false,8,1);
int t9[]={10,0,0,0,0,0,0,0,0,0,0};run(t9,11,true,9,1);
int t10[]={2,0,1,0,1};run(t10,5,false,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
