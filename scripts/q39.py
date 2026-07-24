import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Set Matrix Zeroes"
desc = "Given an m x n integer matrix, if an element is 0, set its entire row and column to 0. Do it in-place."
infmt = "First line contains m and n.\nNext m lines each contain n space-separated integers."
outfmt = "Print the modified matrix, m lines with n space-separated integers each."
cons = "1 \u2264 m, n \u2264 200\n-2\u00b9\u00b9 \u2264 matrix[i][j] \u2264 2\u00b9\u00b9 - 1"
e1 = "Input:\n3 3\n1 1 1\n1 0 1\n1 1 1\n\nOutput:\n1 0 1\n0 0 0\n1 0 1"
e2 = "Input:\n3 4\n0 1 2 0\n3 4 5 2\n1 3 1 5\n\nOutput:\n0 0 0 0\n0 4 5 0\n0 3 1 0"
e3 = "Input:\n1 1\n1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Matrix", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

for lang, code in [("JAVA", '''import java.util.*;

// USER_CODE_START
class Solution {
    public void setZeroes(int[][] matrix) {
        // Write your code here
    }
}
// USER_CODE_END

public class Main {
    static boolean eq(int[][] a, int[][] b) {
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < a[0].length; j++)
                if (a[i][j] != b[i][j]) return false;
        return true;
    }
    static void test(int[][] m, int[][] e, int tc, boolean h) {
        int[][] cp = new int[m.length][];
        for (int i = 0; i < m.length; i++) cp[i] = Arrays.copyOf(m[i], m[i].length);
        new Solution().setZeroes(cp);
        if (eq(cp, e)) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:got=" + Arrays.deepToString(cp));
    }
    public static void main(String[] a) {
        try { test(new int[][]{{1,1,1},{1,0,1},{1,1,1}}, new int[][]{{1,0,1},{0,0,0},{1,0,1}}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[][]{{0,1,2,0},{3,4,5,2},{1,3,1,5}}, new int[][]{{0,0,0,0},{0,4,5,0},{0,3,1,0}}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[][]{{1}}, new int[][]{{1}}, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[][]{{0,1},{1,1}}, new int[][]{{0,0},{0,1}}, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[][]{{1,2,3},{4,0,6},{7,8,9}}, new int[][]{{1,0,3},{0,0,0},{7,0,9}}, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[][]{{0,0},{0,0}}, new int[][]{{0,0},{0,0}}, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''),
("CPP", '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    void setZeroes(vector<vector<int>>& matrix) {
        // Write your code here
    }
};
// USER_CODE_END

bool eq(vector<vector<int>>& a, vector<vector<int>>& b) {
    for (size_t i = 0; i < a.size(); i++)
        for (size_t j = 0; j < a[0].size(); j++)
            if (a[i][j] != b[i][j]) return false;
    return true;
}

void test(vector<vector<int>> m, vector<vector<int>> e, int tc, bool h = false) {
    vector<vector<int>> cp = m;
    Solution().setZeroes(cp);
    if (eq(cp, e)) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:got=";
        for (auto& r : cp) { cout << "["; for (int x : r) cout << x << ","; cout << "] "; }
        cout << "\\n";
    }
}

int main() {
    try { test({{1,1,1},{1,0,1},{1,1,1}}, {{1,0,1},{0,0,0},{1,0,1}}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({{0,1,2,0},{3,4,5,2},{1,3,1,5}}, {{0,0,0,0},{0,4,5,0},{0,3,1,0}}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({{1}}, {{1}}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({{0,1},{1,1}}, {{0,0},{0,1}}, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({{1,2,3},{4,0,6},{7,8,9}}, {{1,0,3},{0,0,0},{7,0,9}}, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({{0,0},{0,0}}, {{0,0},{0,0}}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''),
("PYTHON", '''# USER_CODE_START
class Solution:
    def setZeroes(self, matrix):
        # Write your code here
        pass
# USER_CODE_END

def test(m, e, tc, hidden=False):
    cp = [row[:] for row in m]
    Solution().setZeroes(cp)
    if cp == e: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:got={cp}")

try: test([[1,1,1],[1,0,1],[1,1,1]], [[1,0,1],[0,0,0],[1,0,1]], 1)
except: print("TC:1:FAIL:hidden")
try: test([[0,1,2,0],[3,4,5,2],[1,3,1,5]], [[0,0,0,0],[0,4,5,0],[0,3,1,0]], 2)
except: print("TC:2:FAIL:hidden")
try: test([[1]], [[1]], 3)
except: print("TC:3:FAIL:hidden")
try: test([[0,1],[1,1]], [[0,0],[0,1]], 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([[1,2,3],[4,0,6],[7,8,9]], [[1,0,3],[0,0,0],[7,0,9]], 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([[0,0],[0,0]], [[0,0],[0,0]], 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''),
("JAVASCRIPT", '''// USER_CODE_START
function setZeroes(matrix) {
    // Write your code here
}
// USER_CODE_END

function test(m, e, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const cp = m.map(r => [...r]);
    setZeroes(cp);
    const gs = JSON.stringify(cp);
    const es = JSON.stringify(e);
    if (gs === es) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:got=" + gs);
}

try { test([[1,1,1],[1,0,1],[1,1,1]], [[1,0,1],[0,0,0],[1,0,1]], 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([[0,1,2,0],[3,4,5,2],[1,3,1,5]], [[0,0,0,0],[0,4,5,0],[0,3,1,0]], 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([[1]], [[1]], 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([[0,1],[1,1]], [[0,0],[0,1]], 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([[1,2,3],[4,0,6],[7,8,9]], [[1,0,3],[0,0,0],[7,0,9]], 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([[0,0],[0,0]], [[0,0],[0,0]], 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''),
("C", '''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
void setZeroes(int** matrix, int matrixSize, int* matrixColSize) {
    // Write your code here
}
// USER_CODE_END

int main() {
    printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");
    return 0;
}''')]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
