import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Search a 2D Matrix"
desc = "Write an efficient algorithm that searches for a value target in an m x n integer matrix. The matrix has two properties: integers in each row are sorted from left to right, and the first integer of each row is greater than the last integer of the previous row."
infmt = "First line contains m and n.\nNext m lines each contain n space-separated integers.\nLast line contains the target integer."
outfmt = "Print 'true' if target exists, otherwise 'false'."
cons = "1 \u2264 m, n \u2264 100\n-10\u2074 \u2264 matrix[i][j], target \u2264 10\u2074"
e1 = "Input:\n3 4\n1 3 5 7\n10 11 16 20\n23 30 34 60\n3\n\nOutput:\ntrue"
e2 = "Input:\n3 4\n1 3 5 7\n10 11 16 20\n23 30 34 60\n13\n\nOutput:\nfalse"
e3 = "Input:\n1 1\n1\n0\n\nOutput:\nfalse"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Binary Search, Matrix", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public boolean searchMatrix(int[][] matrix, int target) {
        // Write your code here
        return false;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[][] m, int t, boolean e, int tc, boolean h) {
        boolean g = new Solution().searchMatrix(m, t);
        if (g == e) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:expected=" + e + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 3, true, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 13, false, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[][]{{1}}, 0, false, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[][]{{1}}, 1, true, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 34, true, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[][]{{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 60, true, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    bool searchMatrix(vector<vector<int>>& matrix, int target) {
        // Write your code here
        return false;
    }
};
// USER_CODE_END

void test(vector<vector<int>> m, int t, bool e, int tc, bool h = false) {
    bool g = Solution().searchMatrix(m, t);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:expected=" << (e ? "true" : "false") << ":got=" << (g ? "true" : "false") << "\\n";
}

int main() {
    try { test({{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 3, true, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 13, false, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({{1}}, 0, false, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({{1}}, 1, true, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 34, true, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({{1,3,5,7},{10,11,16,20},{23,30,34,60}}, 60, true, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def searchMatrix(self, matrix, target):
        # Write your code here
        return False
# USER_CODE_END

def test(m, t, e, tc, hidden=False):
    g = Solution().searchMatrix(m, t)
    if g == e: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:expected={e}:got={g}")

try: test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 3, True, 1)
except: print("TC:1:FAIL:hidden")
try: test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 13, False, 2)
except: print("TC:2:FAIL:hidden")
try: test([[1]], 0, False, 3)
except: print("TC:3:FAIL:hidden")
try: test([[1]], 1, True, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 34, True, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 60, True, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function searchMatrix(matrix, target) {
    // Write your code here
    return false;
}
// USER_CODE_END

function test(m, t, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = searchMatrix(m, t);
    if (g === e) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:expected=" + e + ":got=" + g);
}

try { test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 3, true, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 13, false, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([[1]], 0, false, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([[1]], 1, true, 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 34, true, 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([[1,3,5,7],[10,11,16,20],[23,30,34,60]], 60, true, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
bool searchMatrix(int** matrix, int matrixSize, int* matrixColSize, int target) {
    // Write your code here
    return false;
}
// USER_CODE_END

void runTest(int** m, int rs, int cs, int t, bool exp, int tc, int hidden) {
    bool got = searchMatrix(m, rs, &cs, t);
    if (got == exp) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:expected=%s:got=%s\\n", tc, exp ? "true" : "false", got ? "true" : "false");
    }
}

int main() {
    int r0[][4] = {{1,3,5,7},{10,11,16,20},{23,30,34,60}};
    int* m0[] = {r0[0], r0[1], r0[2]};
    runTest(m0, 3, 4, 3, true, 1, 0);

    runTest(m0, 3, 4, 13, false, 2, 0);

    int r1[][1] = {{1}};
    int* m1[] = {r1[0]};
    runTest(m1, 1, 1, 0, false, 3, 0);

    runTest(m1, 1, 1, 1, true, 4, 1);
    runTest(m0, 3, 4, 34, true, 5, 1);
    runTest(m0, 3, 4, 60, true, 6, 1);
    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
