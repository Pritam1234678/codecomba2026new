import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Spiral Matrix"
desc = "Given an m x n matrix, return all elements of the matrix in spiral order. Start from top-left corner, move clockwise (right, down, left, up), gradually shrinking the boundaries."
infmt = "First line contains two integers m and n.\nNext m lines each contain n space-separated integers representing the matrix."
outfmt = "Print all elements in spiral order, separated by spaces."
cons = "1 \u2264 m, n \u2264 10\n-100 \u2264 matrix[i][j] \u2264 100"
e1 = "Input:\n3 3\n1 2 3\n4 5 6\n7 8 9\n\nOutput:\n1 2 3 6 9 8 7 4 5"
e2 = "Input:\n3 4\n1 2 3 4\n5 6 7 8\n9 10 11 12\n\nOutput:\n1 2 3 4 8 12 11 10 9 5 6 7"
e3 = "Input:\n1 1\n1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Matrix, Simulation", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public List<Integer> spiralOrder(int[][] matrix) {
        // Write your code here
        return new ArrayList<>();
    }
}
// USER_CODE_END

public class Main {
    static String listToStr(List<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int x : list) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(x);
        }
        return sb.toString();
    }

    static void test(int[][] matrix, String expected, int tc, boolean hidden) {
        List<Integer> result = new Solution().spiralOrder(matrix);
        String got = listToStr(result);
        if (got.equals(expected))
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else {
            System.out.print("TC:" + tc + ":FAIL:input=");
            for (int[] row : matrix) System.out.print(Arrays.toString(row) + " ");
            System.out.println(":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] a) {
        try { test(new int[][]{{1,2,3},{4,5,6},{7,8,9}}, "1 2 3 6 9 8 7 4 5", 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12}}, "1 2 3 4 8 12 11 10 9 5 6 7", 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[][]{{1}}, "1", 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[][]{{1,2},{3,4}}, "1 2 4 3", 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[][]{{6},{9},{7}}, "6 9 7", 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[][]{{1,2,3,4,5},{6,7,8,9,10}}, "1 2 3 4 5 10 9 8 7 6", 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    vector<int> spiralOrder(vector<vector<int>>& matrix) {
        // Write your code here
        return {};
    }
};
// USER_CODE_END

string vecToStr(vector<int>& v) {
    string s;
    for (int x : v) {
        if (!s.empty()) s += " ";
        s += to_string(x);
    }
    return s;
}

void test(vector<vector<int>> matrix, string expected, int tc, bool hidden = false) {
    Solution sol;
    vector<int> result = sol.spiralOrder(matrix);
    string got = vecToStr(result);
    if (got == expected)
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\\n";
    else
        cout << "TC:" << tc << ":FAIL:expected=" << expected << ":got=" << got << "\\n";
}

int main() {
    try { test({{1,2,3},{4,5,6},{7,8,9}}, "1 2 3 6 9 8 7 4 5", 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({{1,2,3,4},{5,6,7,8},{9,10,11,12}}, "1 2 3 4 8 12 11 10 9 5 6 7", 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({{1}}, "1", 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({{1,2},{3,4}}, "1 2 4 3", 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({{6},{9},{7}}, "6 9 7", 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({{1,2,3,4,5},{6,7,8,9,10}}, "1 2 3 4 5 10 9 8 7 6", 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def spiralOrder(self, matrix):
        # Write your code here
        return []
# USER_CODE_END

def test(matrix, expected, tc, hidden=False):
    got = Solution().spiralOrder(matrix)
    got_str = " ".join(str(x) for x in got)
    if got_str == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:expected={expected}:got={got_str}")

try: test([[1,2,3],[4,5,6],[7,8,9]], "1 2 3 6 9 8 7 4 5", 1)
except: print("TC:1:FAIL:hidden")
try: test([[1,2,3,4],[5,6,7,8],[9,10,11,12]], "1 2 3 4 8 12 11 10 9 5 6 7", 2)
except: print("TC:2:FAIL:hidden")
try: test([[1]], "1", 3)
except: print("TC:3:FAIL:hidden")
try: test([[1,2],[3,4]], "1 2 4 3", 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([[6],[9],[7]], "6 9 7", 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([[1,2,3,4,5],[6,7,8,9,10]], "1 2 3 4 5 10 9 8 7 6", 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function spiralOrder(matrix) {
    // Write your code here
    return [];
}
// USER_CODE_END

function test(matrix, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = spiralOrder(matrix);
    const gotStr = got.join(" ");
    if (gotStr === expected)
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden)
        console.log("TC:" + tc + ":FAIL:hidden");
    else
        console.log("TC:" + tc + ":FAIL:expected=" + expected + ":got=" + gotStr);
}

try { test([[1,2,3],[4,5,6],[7,8,9]], "1 2 3 6 9 8 7 4 5", 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([[1,2,3,4],[5,6,7,8],[9,10,11,12]], "1 2 3 4 8 12 11 10 9 5 6 7", 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([[1]], "1", 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([[1,2],[3,4]], "1 2 4 3", 4, true); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([[6],[9],[7]], "6 9 7", 5, true); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([[1,2,3,4,5],[6,7,8,9,10]], "1 2 3 4 5 10 9 8 7 6", 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* spiralOrder(int** matrix, int matrixSize, int* matrixColSize, int* returnSize) {
    // Write your code here
    *returnSize = 0;
    return NULL;
}
// USER_CODE_END

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void runTest(int** m, int rs, int cs, int* exp, int expN, int tc) {
    int retSize;
    int* got = spiralOrder(m, rs, &cs, &retSize);
    if (retSize == expN && arrEq(got, exp, retSize))
        printf("TC:%d:PASS\\n", tc);
    else
        printf("TC:%d:FAIL:hidden\\n", tc);
}

void runTestHidden(int** m, int rs, int cs, int* exp, int expN, int tc) {
    int retSize;
    int* got = spiralOrder(m, rs, &cs, &retSize);
    if (retSize == expN && arrEq(got, exp, retSize))
        printf("TC:%d:PASS:hidden\\n", tc);
    else
        printf("TC:%d:FAIL:hidden\\n", tc);
}

int main() {
    int r0[][3] = {{1,2,3},{4,5,6},{7,8,9}};
    int* m0[] = {r0[0], r0[1], r0[2]};
    int e0[] = {1,2,3,6,9,8,7,4,5};
    runTest(m0, 3, 3, e0, 9, 1);

    int r1[][4] = {{1,2,3,4},{5,6,7,8},{9,10,11,12}};
    int* m1[] = {r1[0], r1[1], r1[2]};
    int e1[] = {1,2,3,4,8,12,11,10,9,5,6,7};
    runTest(m1, 3, 4, e1, 12, 2);

    int r2[][1] = {{1}};
    int* m2[] = {r2[0]};
    int e2[] = {1};
    runTest(m2, 1, 1, e2, 1, 3);

    int r3[][2] = {{1,2},{3,4}};
    int* m3[] = {r3[0], r3[1]};
    int e3[] = {1,2,4,3};
    runTestHidden(m3, 2, 2, e3, 4, 4);

    int r4[][1] = {{6},{9},{7}};
    int* m4[] = {r4[0], r4[1], r4[2]};
    int e4[] = {6,9,7};
    runTestHidden(m4, 3, 1, e4, 3, 5);

    int r5[][5] = {{1,2,3,4,5},{6,7,8,9,10}};
    int* m5[] = {r5[0], r5[1]};
    int e5[] = {1,2,3,4,5,10,9,8,7,6};
    runTestHidden(m5, 2, 5, e5, 10, 6);
    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets inserted for {title} (pid={pid})")

cur.close()
conn.close()
