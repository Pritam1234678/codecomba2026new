import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Combination Sum"
desc = "Given an array of distinct integers candidates and a target integer target, return a list of all unique combinations of candidates where the chosen numbers sum to target. You may use the same number an unlimited number of times."
infmt = "First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target."
outfmt = "Print each combination on a new line as space-separated integers."
cons = "1 \u2264 n \u2264 30\n2 \u2264 candidates[i] \u2264 40\n1 \u2264 target \u2264 40"
e1 = "Input:\n4\n2 3 6 7\n7\n\nOutput:\n2 2 3\n7"
e2 = "Input:\n3\n2 3 5\n8\n\nOutput:\n2 2 2 2\n2 3 3\n3 5"
e3 = "Input:\n1\n2\n1\n\nOutput:\n"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, active, topics, example1, example2, example3) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Backtracking", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

for lang, code in [("JAVA", '''import java.util.*;
// USER_CODE_START
class Solution { public List<List<Integer>> combinationSum(int[] candidates, int target) { return new ArrayList<>(); } }
// USER_CODE_END
public class Main {
static void test(int[] c, int t, int es, int tc, boolean h){int g=new Solution().combinationSum(c,t).size();if(g==es)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:expected="+es+" combos:got="+g);}
public static void main(String[] a){
try{test(new int[]{2,3,6,7},7,2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,3,5},8,3,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{2},1,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{2,3,5,7},7,2,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2},2,1,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{2,3,6,7},1,0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''),
("CPP", '''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: vector<vector<int>> combinationSum(vector<int>& c, int t) { return {}; } };
// USER_CODE_END
void test(vector<int> c, int t, int es, int tc, bool h=false){int g=Solution().combinationSum(c,t).size();if(g==es)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:expected="<<es<<":got="<<g<<"\\n";}
int main(){
try{test({2,3,6,7},7,2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,3,5},8,3,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({2},1,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({2,3,5,7},7,2,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2},2,1,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({2,3,6,7},1,0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''),
("PYTHON", '''# USER_CODE_START
class Solution: def combinationSum(self, c, t): return []
# USER_CODE_END
def test(c,t,es,tc,h=False):g=Solution().combinationSum(c,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if len(g)==es else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:got size={len(g)}"))
try:test([2,3,6,7],7,2,1)
except:print("TC:1:FAIL:hidden")
try:test([2,3,5],8,3,2)
except:print("TC:2:FAIL:hidden")
try:test([2],1,0,3)
except:print("TC:3:FAIL:hidden")
try:test([2,3,5,7],7,2,4,True)
except:print("TC:4:FAIL:hidden")
try:test([2],2,1,5,True)
except:print("TC:5:FAIL:hidden")
try:test([2,3,6,7],1,0,6,True)
except:print("TC:6:FAIL:hidden")'''),
("JAVASCRIPT", '''// USER_CODE_START
function combinationSum(c, t) { return []; }
// USER_CODE_END
function test(c,t,es,tc,h){if(h===undefined)h=false;const g=combinationSum(c,t);if(g.length===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got size="+g.length);}
try{test([2,3,6,7],7,2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,3,5],8,3,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([2],1,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([2,3,5,7],7,2,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2],2,1,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2,3,6,7],1,0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''),
("C", '''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int** combinationSum(int* candidates, int candidatesSize, int target, int* returnSize, int** returnColumnSizes) {
    // Write your code here
    *returnSize = 0;
    return NULL;
}
// USER_CODE_END

int main() {
    int rs, rcs[100];
    int* rcp = rcs;
    int c1[] = {2,3,6,7};
    int** r1 = combinationSum(c1, 4, 7, &rs, &rcp);
    if (rs == 2) printf("TC:1:PASS\\n"); else printf("TC:1:FAIL:hidden\\n");

    int c2[] = {2,3,5};
    rcp = rcs;
    int** r2 = combinationSum(c2, 3, 8, &rs, &rcp);
    if (rs == 3) printf("TC:2:PASS\\n"); else printf("TC:2:FAIL:hidden\\n");

    int c3[] = {2};
    rcp = rcs;
    int** r3 = combinationSum(c3, 1, 1, &rs, &rcp);
    if (rs == 0) printf("TC:3:PASS\\n"); else printf("TC:3:FAIL:hidden\\n");

    int c4[] = {2,3,5,7};
    rcp = rcs;
    int** r4 = combinationSum(c4, 4, 7, &rs, &rcp);
    if (rs == 2) printf("TC:4:PASS:hidden\\n"); else printf("TC:4:FAIL:hidden\\n");

    int c5[] = {2};
    rcp = rcs;
    int** r5 = combinationSum(c5, 1, 2, &rs, &rcp);
    if (rs == 1) printf("TC:5:PASS:hidden\\n"); else printf("TC:5:FAIL:hidden\\n");

    int c6[] = {2,3,6,7};
    rcp = rcs;
    int** r6 = combinationSum(c6, 4, 1, &rs, &rcp);
    if (rs == 0) printf("TC:6:PASS:hidden\\n"); else printf("TC:6:FAIL:hidden\\n");

    return 0;
}''')]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 for {title} (pid={pid})")
cur.close()
conn.close()
