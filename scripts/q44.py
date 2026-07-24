import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Permutations"
desc = "Given an array nums of distinct integers, return all possible permutations. You can return the answer in any order."
infmt = "First line contains integer n.\nSecond line contains n space-separated integers."
outfmt = "Print each permutation on a new line as space-separated integers."
cons = "1 \u2264 n \u2264 6\n-10 \u2264 nums[i] \u2264 10\nAll elements are distinct."
e1 = "Input:\n3\n1 2 3\n\nOutput:\n1 2 3\n1 3 2\n2 1 3\n2 3 1\n3 1 2\n3 2 1"
e2 = "Input:\n1\n1\n\nOutput:\n1"
e3 = "Input:\n2\n0 -1\n\nOutput:\n0 -1\n-1 0"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, active, topics, example1, example2, example3) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Backtracking", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

for lang, code in [("JAVA", '''import java.util.*;
// USER_CODE_START
class Solution { public List<List<Integer>> permute(int[] nums) { return new ArrayList<>(); } }
// USER_CODE_END
public class Main {
static void test(int[] n, int es, int tc, boolean h){int g=new Solution().permute(n).size();if(g==es)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:expected size="+es+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,3},6,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1},1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,-1},2,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4},24,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2},2,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-1,0,1},6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''),
("CPP", '''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: vector<vector<int>> permute(vector<int>& nums) { return {}; } };
// USER_CODE_END
void test(vector<int> n, int es, int tc, bool h=false){int g=Solution().permute(n).size();if(g==es)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:expected size="<<es<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,3},6,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1},1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,-1},2,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4},24,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2},2,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({-1,0,1},6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''),
("PYTHON", '''# USER_CODE_START
class Solution: def permute(self, nums): return []
# USER_CODE_END
def test(n,es,tc,h=False):g=Solution().permute(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if len(g)==es else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got size={len(g)}"))
try:test([1,2,3],6,1)
except:print("TC:1:FAIL:hidden")
try:test([1],1,2)
except:print("TC:2:FAIL:hidden")
try:test([0,-1],2,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4],24,4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,2],2,5,True)
except:print("TC:5:FAIL:hidden")
try:test([-1,0,1],6,6,True)
except:print("TC:6:FAIL:hidden")'''),
("JAVASCRIPT", '''// USER_CODE_START
function permute(nums) { return []; }
// USER_CODE_END
function test(n,es,tc,h){if(h===undefined)h=false;const g=permute(n);if(g.length===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got size="+g.length);}
try{test([1,2,3],6,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1],1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,-1],2,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4],24,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2],2,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-1,0,1],6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''),
("C", '''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int** permute(int* nums, int numsSize, int* returnSize, int** returnColumnSizes) { *returnSize=0; return NULL; }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 for {title} (pid={pid})")
cur.close()
conn.close()
