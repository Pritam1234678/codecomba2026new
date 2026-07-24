import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Rotate Image"
desc = "You are given an n x n 2D matrix representing an image. Rotate the image by 90 degrees clockwise in-place."
infmt = "First line contains integer n.\nNext n lines each contain n space-separated integers."
outfmt = "Print the rotated matrix, n lines with n space-separated integers each."
cons = "1 \u2264 n \u2264 20\n-1000 \u2264 matrix[i][j] \u2264 1000"
e1 = "Input:\n3\n1 2 3\n4 5 6\n7 8 9\n\nOutput:\n7 4 1\n8 5 2\n9 6 3"
e2 = "Input:\n1\n1\n\nOutput:\n1"
e3 = "Input:\n2\n1 2\n3 4\n\nOutput:\n3 1\n4 2"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Matrix", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public void rotate(int[][] matrix) {
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

    static void test(int[][] m, int[][] expected, int tc, boolean hidden) {
        int[][] copy = new int[m.length][];
        for (int i = 0; i < m.length; i++) copy[i] = Arrays.copyOf(m[i], m[i].length);
        new Solution().rotate(copy);
        if (eq(copy, expected))
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.deepToString(m) + ":expected=" + Arrays.deepToString(expected) + ":got=" + Arrays.deepToString(copy));
    }

    public static void main(String[] a) {
        try { test(new int[][]{{1,2,3},{4,5,6},{7,8,9}}, new int[][]{{7,4,1},{8,5,2},{9,6,3}}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[][]{{1}}, new int[][]{{1}}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[][]{{1,2},{3,4}}, new int[][]{{3,1},{4,2}}, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}}, new int[][]{{13,9,5,1},{14,10,6,2},{15,11,7,3},{16,12,8,4}}, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[][]{{-1,-2},{-3,-4}}, new int[][]{{-3,-1},{-4,-2}}, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[][]{{1,1,1},{2,2,2},{3,3,3}}, new int[][]{{3,2,1},{3,2,1},{3,2,1}}, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    void rotate(vector<vector<int>>& matrix) {
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

void test(vector<vector<int>> m, vector<vector<int>> expected, int tc, bool hidden = false) {
    vector<vector<int>> copy = m;
    Solution().rotate(copy);
    if (eq(copy, expected))
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:expected=";
        for (auto& r : expected) { cout << "["; for (int x : r) cout << x << ","; cout << "] "; }
        cout << ":got=";
        for (auto& r : copy) { cout << "["; for (int x : r) cout << x << ","; cout << "] "; }
        cout << "\\n";
    }
}

int main() {
    try { test({{1,2,3},{4,5,6},{7,8,9}}, {{7,4,1},{8,5,2},{9,6,3}}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({{1}}, {{1}}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({{1,2},{3,4}}, {{3,1},{4,2}}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}}, {{13,9,5,1},{14,10,6,2},{15,11,7,3},{16,12,8,4}}, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({{-1,-2},{-3,-4}}, {{-3,-1},{-4,-2}}, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({{1,1,1},{2,2,2},{3,3,3}}, {{3,2,1},{3,2,1},{3,2,1}}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def rotate(self, matrix):
        # Write your code here
        pass
# USER_CODE_END

def test(m, expected, tc, hidden=False):
    copy = [row[:] for row in m]
    Solution().rotate(copy)
    if copy == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:expected={expected}:got={copy}")

try: test([[1,2,3],[4,5,6],[7,8,9]], [[7,4,1],[8,5,2],[9,6,3]], 1)
except: print("TC:1:FAIL:hidden")
try: test([[1]], [[1]], 2)
except: print("TC:2:FAIL:hidden")
try: test([[1,2],[3,4]], [[3,1],[4,2]], 3)
except: print("TC:3:FAIL:hidden")
try: test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]], [[13,9,5,1],[14,10,6,2],[15,11,7,3],[16,12,8,4]], 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([[-1,-2],[-3,-4]], [[-3,-1],[-4,-2]], 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([[1,1,1],[2,2,2],[3,3,3]], [[3,2,1],[3,2,1],[3,2,1]], 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function rotate(matrix) {
    // Write your code here
}
// USER_CODE_END

function test(m, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const copy = m.map(r => [...r]);
    rotate(copy);
    const gs = JSON.stringify(copy);
    const es = JSON.stringify(expected);
    if (gs === es)
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden)
        console.log("TC:" + tc + ":FAIL:hidden");
    else
        console.log("TC:" + tc + ":FAIL:expected=" + es + ":got=" + gs);
}

try { test([[1,2,3],[4,5,6],[7,8,9]], [[7,4,1],[8,5,2],[9,6,3]], 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([[1]], [[1]], 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([[1,2],[3,4]], [[3,1],[4,2]], 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]], [[13,9,5,1],[14,10,6,2],[15,11,7,3],[16,12,8,4]], 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([[-1,-2],[-3,-4]], [[-3,-1],[-4,-2]], 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([[1,1,1],[2,2,2],[3,3,3]], [[3,2,1],[3,2,1],[3,2,1]], 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
void rotate(int** matrix, int matrixSize, int* matrixColSize) {
    // Write your code here
}
// USER_CODE_END

int eq(int** a, int** b, int n) {
    for (int i = 0; i < n; i++)
        for (int j = 0; j < n; j++)
            if (a[i][j] != b[i][j]) return 0;
    return 1;
}

void runTest(int** m, int n, int** exp, int tc, int hidden) {
    int size = n;
    int* colSize = &size;
    // allocate copy
    int** cp = malloc(n * sizeof(int*));
    for (int i = 0; i < n; i++) {
        cp[i] = malloc(n * sizeof(int));
        for (int j = 0; j < n; j++) cp[i][j] = m[i][j];
    }
    rotate(cp, n, colSize);
    if (eq(cp, exp, n)) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:hidden\\n", tc);
    }
    for (int i = 0; i < n; i++) free(cp[i]);
    free(cp);
}

int main() {
    int r0[][3] = {{1,2,3},{4,5,6},{7,8,9}};
    int* m0[] = {r0[0], r0[1], r0[2]};
    int e0[][3] = {{7,4,1},{8,5,2},{9,6,3}};
    int* ep0[] = {e0[0], e0[1], e0[2]};
    runTest(m0, 3, ep0, 1, 0);

    int r1[][1] = {{1}};
    int* m1[] = {r1[0]};
    int e1[][1] = {{1}};
    int* ep1[] = {e1[0]};
    runTest(m1, 1, ep1, 2, 0);

    int r2[][2] = {{1,2},{3,4}};
    int* m2[] = {r2[0], r2[1]};
    int e2[][2] = {{3,1},{4,2}};
    int* ep2[] = {e2[0], e2[1]};
    runTest(m2, 2, ep2, 3, 0);

    int r3[][4] = {{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}};
    int* m3[] = {r3[0], r3[1], r3[2], r3[3]};
    int e3[][4] = {{13,9,5,1},{14,10,6,2},{15,11,7,3},{16,12,8,4}};
    int* ep3[] = {e3[0], e3[1], e3[2], e3[3]};
    runTest(m3, 4, ep3, 4, 1);

    int r4[][2] = {{-1,-2},{-3,-4}};
    int* m4[] = {r4[0], r4[1]};
    int e4[][2] = {{-3,-1},{-4,-2}};
    int* ep4[] = {e4[0], e4[1]};
    runTest(m4, 2, ep4, 5, 1);

    int r5[][3] = {{1,1,1},{2,2,2},{3,3,3}};
    int* m5[] = {r5[0], r5[1], r5[2]};
    int e5[][3] = {{3,2,1},{3,2,1},{3,2,1}};
    int* ep5[] = {e5[0], e5[1], e5[2]};
    runTest(m5, 3, ep5, 6, 1);
    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets inserted for {title} (pid={pid})")
cur.close()
conn.close()
