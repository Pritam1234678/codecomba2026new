"""
Find Pivot Index
=================
Given an array of integers nums, find the pivot index where the sum of all numbers
to the left of the index equals the sum of all numbers to the right of the index.
If no such index exists, return -1. The pivot index is the leftmost one.

Examples:
  nums = [1, 7, 3, 6, 5, 6] → pivot index 3
    Left sum: 1+7+3 = 11
    Right sum: 5+6 = 11

  nums = [1, 2, 3] → -1 (no pivot)
  nums = [2, 1, -1] → 0 (pivot)
    Left sum of index 0 = 0 (nothing to the left)
    Right sum: 1+(-1) = 0

Approach: Compute total sum, then iterate left to right tracking leftSum.
For each index, if leftSum == total - leftSum - nums[i], return i.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Find Pivot Index"
desc = (
    "Given an array of integers nums, find the pivot index where the sum of all "
    "numbers to the left equals the sum of all numbers to the right.\n\n"
    "If the index is at the leftmost edge (index 0), the left sum is 0. "
    "If the index is at the rightmost edge, the right sum is 0.\n"
    "Return the leftmost pivot index if found, otherwise return -1.\n\n"
    "For example:\n"
    "- nums = [1, 7, 3, 6, 5, 6]: left sum of index 3 = 1+7+3 = 11, "
    "right sum = 5+6 = 11, so pivot = 3.\n"
    "- nums = [1, 2, 3]: no such index exists, return -1.\n\n"
    "Algorithm: First compute total sum of all elements. Then iterate left to right "
    "keeping a running leftSum. For each index i, check if leftSum == total - leftSum - nums[i]. "
    "If yes, i is the pivot."
)
infmt = "First line contains integer n.\nSecond line contains n space-separated integers."
outfmt = "Print the pivot index, or -1 if none exists."
cons = (
    "1 \u2264 n \u2264 10^4\n"
    "-1000 \u2264 nums[i] \u2264 1000"
)
e1 = (
    "Input:\n"
    "6\n"
    "1 7 3 6 5 6\n\n"
    "Output:\n"
    "3\n\n"
    "Explanation: Sum left of index 3 = 1+7+3 = 11. Sum right = 5+6 = 11."
)
e2 = (
    "Input:\n"
    "3\n"
    "1 2 3\n\n"
    "Output:\n"
    "-1\n\n"
    "Explanation: No index where left sum equals right sum."
)
e3 = (
    "Input:\n"
    "3\n"
    "2 1 -1\n\n"
    "Output:\n"
    "0\n\n"
    "Explanation: Left sum of index 0 = 0, right sum = 1+(-1) = 0."
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True,
     "Array, Prefix Sum", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int pivotIndex(int[] nums) {
        // Write your code here — find the pivot index
        return -1;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int expected, int tc, boolean hidden) {
        int got = new CodeCoder().pivotIndex(nums);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(nums)
                + ":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] args) {
        try { test(new int[]{1,7,3,6,5,6}, 3, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test(new int[]{1,2,3}, -1, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test(new int[]{2,1,-1}, 0, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test(new int[]{1}, 0, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(new int[]{-1,-1,0,1,1,0}, 0, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(new int[]{1,2,3,4,5,6}, -1, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test(new int[]{0}, 0, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test(new int[]{1,2,3,2,1}, 2, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test(new int[]{-1,-1,-1,0,1,1}, 0, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test(new int[]{1, -1, 1, -1, 1, -1, 1}, 6, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    int pivotIndex(vector<int>& nums) {
        // Write your code here — find the pivot index
        return -1;
    }
};
// USER_CODE_END

void test(vector<int> nums, int expected, int tc, bool hidden = false) {
    int got = CodeCoder().pivotIndex(nums);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:expected=" << expected << ":got=" << got << "\n";
    }
}

int main() {
    try { test({1,7,3,6,5,6}, 3, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test({1,2,3}, -1, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test({2,1,-1}, 0, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test({1}, 0, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test({-1,-1,0,1,1,0}, 0, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test({1,2,3,4,5,6}, -1, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test({0}, 0, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test({1,2,3,2,1}, 2, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test({-1,-1,-1,0,1,1}, 0, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test({1,-1,1,-1,1,-1,1}, 6, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def pivotIndex(self, nums):
        # Write your code here — find the pivot index
        return -1
# USER_CODE_END

def test(nums, expected, tc, hidden=False):
    got = CodeCoder().pivotIndex(nums)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:nums={nums}:expected={expected}:got={got}")

try: test([1,7,3,6,5,6], 3, 1)
except: print("TC:1:FAIL:hidden")
try: test([1,2,3], -1, 2)
except: print("TC:2:FAIL:hidden")
try: test([2,1,-1], 0, 3)
except: print("TC:3:FAIL:hidden")
try: test([1], 0, 4)
except: print("TC:4:FAIL:hidden")
try: test([-1,-1,0,1,1,0], 0, 5)
except: print("TC:5:FAIL:hidden")
try: test([1,2,3,4,5,6], -1, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([0], 0, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([1,2,3,2,1], 2, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([-1,-1,-1,0,1,1], 0, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([1,-1,1,-1,1,-1,1], 6, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function pivotIndex(nums) {
    // Write your code here — find the pivot index
    return -1;
}
// USER_CODE_END

function test(nums, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = pivotIndex(nums);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:nums=" + JSON.stringify(nums)
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test([1,7,3,6,5,6], 3, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([1,2,3], -1, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([2,1,-1], 0, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([1], 0, 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([-1,-1,0,1,1,0], 0, 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([1,2,3,4,5,6], -1, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test([0], 0, 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test([1,2,3,2,1], 2, 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test([-1,-1,-1,0,1,1], 0, 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test([1,-1,1,-1,1,-1,1], 6, 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>

// USER_CODE_START
int pivotIndex(int* nums, int numsSize) {
    // Write your code here — find the pivot index
    return -1;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void runTest(int* nums, int n, int expected, int tc, int hidden) {
    int got = pivotIndex(nums, n);
    if (got == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:expected=%d:got=%d\n", tc, expected, got);
    }
}

int main() {
    int a1[] = {1,7,3,6,5,6};
    runTest(a1, 6, 3, 1, 0);

    int a2[] = {1,2,3};
    runTest(a2, 3, -1, 2, 0);

    int a3[] = {2,1,-1};
    runTest(a3, 3, 0, 3, 0);

    int a4[] = {1};
    runTest(a4, 1, 0, 4, 0);

    int a5[] = {-1,-1,0,1,1,0};
    runTest(a5, 6, 0, 5, 0);

    int a6[] = {1,2,3,4,5,6};
    runTest(a6, 6, -1, 6, 1);

    int a7[] = {0};
    runTest(a7, 1, 0, 7, 1);

    int a8[] = {1,2,3,2,1};
    runTest(a8, 5, 2, 8, 1);

    int a9[] = {-1,-1,-1,0,1,1};
    runTest(a9, 6, 0, 9, 1);

    int a10[] = {1,-1,1,-1,1,-1,1};
    runTest(a10, 7, 6, 10, 1);

    return 0;
}'''

for lang, code in [
    ("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code),
    ("JAVASCRIPT", js_code), ("C", c_code),
]:
    cur.execute(
        "INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) "
        "VALUES (%s, %s, %s, NOW(), NOW())",
        (pid, lang, code)
    )
conn.commit()

cur.execute(
    "SELECT language, LENGTH(solution_template) FROM code_snippets WHERE problem_id = %s ORDER BY language",
    (pid,)
)
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\nFind Pivot Index (pid={pid}) — done!")
cur.close()
conn.close()
