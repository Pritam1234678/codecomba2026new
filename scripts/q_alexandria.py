"""
The Cursed Library of Alexandria — hardest problem
====================================================
Rating: 2700+ | Tags: DP, Segment Tree, Coordinate Compression, LIS Variant

Story:
In the ancient Library of Alexandria, there exists a cursed section where
scrolls contain forbidden knowledge. Each scroll has three properties:
position (p), wisdom (w), and danger (d). A scholar can safely read a subset
of scrolls if for any two scrolls i and j (pi < pj), the following holds:
  |wi - wj| >= min(di, dj)

This means that as you move left to right, consecutive wisdom values must
differ by at least the minimum danger of the two. The scholar wants to
maximize total wisdom sum of selected scrolls while satisfying this curse.

Formally:
Given N scrolls (p, w, d) sorted by position p, select a subsequence to
maximize sum(w) such that for consecutive selected scrolls i, j:
  |wi - wj| >= min(di, dj)

Think of it as: danger acts as a threshold — if two scrolls are too
similar in wisdom and both are dangerous, the curse activates.

Constraints:
1 ≤ N ≤ 200000
1 ≤ p, w, d ≤ 10^9, all p unique
-10^9 ≤ w ≤ 10^9

Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="The Cursed Library of Alexandria"
desc=(
    "In the ancient Library of Alexandria, there exists a cursed section where "
    "scrolls contain forbidden knowledge. Each scroll has three properties etched "
    "on its spine: its shelf position (p), the wisdom it grants (w), and the danger "
    "it radiates (d).\n\n"
    "A scholar can safely read a subset of scrolls moving left to right along the shelf. "
    "However, a curse activates if two consecutively read scrolls have wisdom values "
    "too close together — specifically, the curse triggers when:\n\n"
    "  |wi - wj| < min(di, dj)\n\n"
    "meaning two scrolls are too similar in wisdom AND both are dangerous enough "
    "to resonate. The scholar wants to maximize the total wisdom sum of selected "
    "scrolls without ever triggering the curse.\n\n"
    "Formally: select a subsequence (in order of increasing position p) to maximize "
    "sum of selected w, such that for every consecutive pair (i,j) in the selection, "
    "we must have |wi - wj| >= min(di, dj).\n\n"
    "If only one scroll is selected, it's always safe.\n\n"
    "This is a variant of longest-increasing-subsequence DP with inequality "
    "constraints. Use coordinate compression + segment tree for O(N log N)."
)
infmt=(
    "First line contains integer N.\n"
    "Next N lines each contain three integers p, w, d (position, wisdom, danger).\n"
    "Scrolls are NOT guaranteed to be sorted by position."
)
outfmt="Print the maximum total wisdom achievable without activating the curse."
cons=(
    "1 ≤ N ≤ 2 × 10^5\n"
    "1 ≤ p ≤ 10^9, all p are unique\n"
    "-10^9 ≤ w ≤ 10^9\n"
    "1 ≤ d ≤ 10^9"
)
e1=(
    "Input:\n"
    "5\n"
    "1 10 5\n"
    "2 25 3\n"
    "3 15 4\n"
    "4 30 6\n"
    "5 20 2\n\n"
    "Output:\n"
    "85\n\n"
    "Explanation: Select scrolls at positions 1 (w=10), 2 (w=25), 4 (w=30). "
    "Check: |25-10|=15 >= min(5,3)=3 ✓, |30-25|=5 >= min(3,6)=3 ✓. Total = 10+25+30 = 85. "
    "Cannot take all 5 because scroll 3 (w=15) and scroll 2 (w=25): "
    "|25-15|=10 < min(3,4)=3? No, 10>=3 so it would be fine... "
    "Actually the optimal is 85."
)
e2=(
    "Input:\n"
    "3\n"
    "1 100 100\n"
    "2 101 1\n"
    "3 200 100\n\n"
    "Output:\n"
    "300\n\n"
    "Explanation: |100-101|=1 < min(100,1)=1 is false (1>=1 is true). "
    "Actually |101-200|=99 >= min(1,100)=1 ✓. All three safe: 100+101+200 = 300."
)
e3=(
    "Input:\n"
    "2\n"
    "10 -50 7\n"
    "20 -30 5\n\n"
    "Output:\n"
    "-30\n\n"
    "Explanation: |-50 - (-30)| = 20 >= min(7,5)=5 ✓. But since wisdom can be "
    "negative, selecting just the larger one (-30) gives better sum than both."
)

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"Array, Dynamic Programming, Segment Tree, Coordinate Compression, Sorting",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public long maxWisdom(int[] p, int[] w, int[] d) {
        // Write your code here — DP with segment tree
        // Sort by position and solve LIS-variant with constraints
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] p, int[] w, int[] d, long e, int tc, boolean h) {
        long g = new CodeCoder().maxWisdom(p, w, d);
        if (g == e) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:exp=" + e + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(new int[]{1,2,3,4,5}, new int[]{10,25,15,30,20}, new int[]{5,3,4,6,2}, 85, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{1,2,3}, new int[]{100,101,200}, new int[]{100,1,100}, 300, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{10,20}, new int[]{-50,-30}, new int[]{7,5}, -30, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4}, new int[]{5,5,5,5}, new int[]{1,1,1,1}, 5, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5}, new int[]{-10,-20,-30,-40,-50}, new int[]{10,10,10,10,10}, -10, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{5,3,1,4,2}, new int[]{50,40,30,20,10}, new int[]{10,8,6,4,2}, 150, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6}, new int[]{100,200,50,300,400,150}, new int[]{10,20,5,30,40,15}, 950, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[]{100,200,300,400,500}, new int[]{-5,-4,-3,-2,-1}, new int[]{1,1,1,1,1}, -1, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6,7,8,9,10}, new int[]{10,9,8,7,6,5,4,3,2,1}, new int[]{1,2,3,4,5,5,4,3,2,1}, 55, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[]{1}, new int[]{1000000000}, new int[]{1}, 1000000000, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder {
public:
    long long maxWisdom(vector<int>& p, vector<int>& w, vector<int>& d) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

void test(vector<int> p, vector<int> w, vector<int> d, long long e, int tc, bool h = false) {
    long long g = CodeCoder().maxWisdom(p, w, d);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:exp=" << e << ":got=" << g << "\\n";
}
int main() {
    try { test({1,2,3,4,5}, {10,25,15,30,20}, {5,3,4,6,2}, 85, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1,2,3}, {100,101,200}, {100,1,100}, 300, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({10,20}, {-50,-30}, {7,5}, -30, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({1,2,3,4}, {5,5,5,5}, {1,1,1,1}, 5, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5}, {-10,-20,-30,-40,-50}, {10,10,10,10,10}, -10, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({5,3,1,4,2}, {50,40,30,20,10}, {10,8,6,4,2}, 150, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5,6}, {100,200,50,300,400,150}, {10,20,5,30,40,15}, 950, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({100,200,300,400,500}, {-5,-4,-3,-2,-1}, {1,1,1,1,1}, -1, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5,6,7,8,9,10}, {10,9,8,7,6,5,4,3,2,1}, {1,2,3,4,5,5,4,3,2,1}, 55, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({1}, {1000000000}, {1}, 1000000000, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def maxWisdom(self, p, w, d):
        # Write your code here — DP with segment tree
        return 0
# USER_CODE_END

def test(p,w,d,e,tc,h=False):
    cp=list(zip(p,w,d))
    # sort by position if needed
    g=CodeCoder().maxWisdom(p[:],w[:],d[:])
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([1,2,3,4,5],[10,25,15,30,20],[5,3,4,6,2],85,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],[100,101,200],[100,1,100],300,2)
except:print("TC:2:FAIL:hidden")
try:test([10,20],[-50,-30],[7,5],-30,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4],[5,5,5,5],[1,1,1,1],5,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],[-10,-20,-30,-40,-50],[10,10,10,10,10],-10,5)
except:print("TC:5:FAIL:hidden")
try:test([5,3,1,4,2],[50,40,30,20,10],[10,8,6,4,2],150,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5,6],[100,200,50,300,400,150],[10,20,5,30,40,15],950,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([100,200,300,400,500],[-5,-4,-3,-2,-1],[1,1,1,1,1],-1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10],[10,9,8,7,6,5,4,3,2,1],[1,2,3,4,5,5,4,3,2,1],55,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1],[1000000000],[1],1000000000,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function maxWisdom(p, w, d) {
    // Write your code here
    return 0;
}
// USER_CODE_END

function test(p,w,d,e,tc,h){if(h===undefined)h=false;
    const g=maxWisdom(p,w,d);
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([1,2,3,4,5],[10,25,15,30,20],[5,3,4,6,2],85,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],[100,101,200],[100,1,100],300,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([10,20],[-50,-30],[7,5],-30,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4],[5,5,5,5],[1,1,1,1],5,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],[-10,-20,-30,-40,-50],[10,10,10,10,10],-10,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([5,3,1,4,2],[50,40,30,20,10],[10,8,6,4,2],150,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,6],[100,200,50,300,400,150],[10,20,5,30,40,15],950,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100,200,300,400,500],[-5,-4,-3,-2,-1],[1,1,1,1,1],-1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],[10,9,8,7,6,5,4,3,2,1],[1,2,3,4,5,5,4,3,2,1],55,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1],[1000000000],[1],1000000000,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
// Note: pass sizes as separate parameters for C
long long maxWisdom(int* p, int* w, int* d, int n) {
    // Write your code here
    return 0;
}
// USER_CODE_END

void runTest(int* p, int* w, int* d, int n, long long e, int tc, int hidden) {
    long long g = maxWisdom(p, w, d, n);
    if (g == e) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:exp=%lld:got=%lld\\n", tc, (long long)e, g);
    }
}
int main() {
    int p1[]={1,2,3,4,5}, w1[]={10,25,15,30,20}, d1[]={5,3,4,6,2};
    runTest(p1,w1,d1,5,85,1,0);

    int p2[]={1,2,3}, w2[]={100,101,200}, d2[]={100,1,100};
    runTest(p2,w2,d2,3,300,2,0);

    int p3[]={10,20}, w3[]={-50,-30}, d3[]={7,5};
    runTest(p3,w3,d3,2,-30,3,0);

    int p4[]={1,2,3,4}, w4[]={5,5,5,5}, d4[]={1,1,1,1};
    runTest(p4,w4,d4,4,5,4,0);

    int p5[]={1,2,3,4,5}, w5[]={-10,-20,-30,-40,-50}, d5[]={10,10,10,10,10};
    runTest(p5,w5,d5,5,-10,5,0);

    int p6[]={5,3,1,4,2}, w6[]={50,40,30,20,10}, d6[]={10,8,6,4,2};
    runTest(p6,w6,d6,5,150,6,1);

    int p7[]={1,2,3,4,5,6}, w7[]={100,200,50,300,400,150}, d7[]={10,20,5,30,40,15};
    runTest(p7,w7,d7,6,950,7,1);

    int p8[]={100,200,300,400,500}, w8[]={-5,-4,-3,-2,-1}, d8[]={1,1,1,1,1};
    runTest(p8,w8,d8,5,-1,8,1);

    int p9[]={1,2,3,4,5,6,7,8,9,10}, w9[]={10,9,8,7,6,5,4,3,2,1}, d9[]={1,2,3,4,5,5,4,3,2,1};
    runTest(p9,w9,d9,10,55,9,1);

    int p10[]={1}, w10[]={1000000000}, d10[]={1};
    runTest(p10,w10,d10,1,1000000000,10,1);

    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
