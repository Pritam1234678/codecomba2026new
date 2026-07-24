"""
Range Sum Query - Immutable
============================
Given an integer array nums, implement the NumArray class that supports
multiple queries of the form: sumRange(left, right) — the sum of elements
from index left to right inclusive.

Example:
  nums = [-2, 0, 3, -5, 2, -1]
  sumRange(0, 2) → 1   ((-2) + 0 + 3 = 1)
  sumRange(2, 5) → -1  (3 + (-5) + 2 + (-1) = -1)
  sumRange(0, 5) → -3  ((-2) + 0 + 3 + (-5) + 2 + (-1) = -3)

Key trick: Precompute a prefix sum array where prefix[i] = sum of nums[0..i-1].
Then sumRange(left, right) = prefix[right+1] - prefix[left].

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Range Sum Query - Immutable"
desc = (
    "Given an integer array nums, implement the CodeCoder class that supports "
    "multiple sum range queries efficiently.\n\n"
    "The class has two methods:\n"
    "- __init__(self, nums) — initializes the object with the integer array nums\n"
    "- sumRange(self, left, right) — returns the sum of elements from index left "
    "to right inclusive\n\n"
    "A naive approach would compute the sum each time in O(n), but we can do better. "
    "Precompute a prefix sum array where prefix[i] = sum of nums[0] through nums[i-1]. "
    "Then sumRange(left, right) = prefix[right+1] - prefix[left] in O(1) time.\n\n"
    "Constraints: The array is immutable (does not change between queries). "
    "There will be up to 10^4 calls to sumRange."
)
infmt = (
    "First line contains integer n.\n"
    "Second line contains n space-separated integers.\n"
    "Third line contains integer q (number of queries).\n"
    "Next q lines each contain two integers left and right."
)
outfmt = "For each query, print the sum on a new line."
cons = (
    "1 \u2264 n \u2264 10^4\n"
    "-10^5 \u2264 nums[i] \u2264 10^5\n"
    "0 \u2264 left \u2264 right < n\n"
    "At most 10^4 calls to sumRange."
)
e1 = (
    "Input:\n"
    "6\n"
    "-2 0 3 -5 2 -1\n"
    "3\n"
    "0 2\n"
    "2 5\n"
    "0 5\n\n"
    "Output:\n"
    "1\n"
    "-1\n"
    "-3\n\n"
    "Explanation:\n"
    "sumRange(0,2) = (-2)+0+3 = 1\n"
    "sumRange(2,5) = 3+(-5)+2+(-1) = -1\n"
    "sumRange(0,5) = (-2)+0+3+(-5)+2+(-1) = -3"
)
e2 = (
    "Input:\n"
    "1\n"
    "5\n"
    "1\n"
    "0 0\n\n"
    "Output:\n"
    "5\n\n"
    "Explanation: Single element array, only possible query."
)
e3 = (
    "Input:\n"
    "3\n"
    "1 2 3\n"
    "2\n"
    "1 2\n"
    "0 1\n\n"
    "Output:\n"
    "5\n"
    "3"
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True,
     "Array, Prefix Sum, Design", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public CodeCoder(int[] nums) {
        // Write your code here — store prefix sums
    }

    public int sumRange(int left, int right) {
        // Write your code here — O(1) using prefix sums
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int left, int right, int expected, int tc, boolean hidden) {
        int got = new CodeCoder(nums).sumRange(left, right);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:nums=" + Arrays.toString(nums)
                + " left=" + left + " right=" + right
                + ":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] args) {
        try { test(new int[]{-2,0,3,-5,2,-1}, 0, 2, 1, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test(new int[]{-2,0,3,-5,2,-1}, 2, 5, -1, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test(new int[]{-2,0,3,-5,2,-1}, 0, 5, -3, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test(new int[]{5}, 0, 0, 5, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(new int[]{1,2,3}, 1, 2, 5, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(new int[]{100,-50,25,-75,200}, 0, 4, 200, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test(new int[]{-5,-5,-5,-5}, 0, 3, -20, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test(new int[]{10,20,30,40,50}, 0, 0, 10, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test(new int[]{10,20,30,40,50}, 4, 4, 50, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test(new int[]{0,0,0,0,0}, 1, 3, 0, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    CodeCoder(vector<int>& nums) {
        // Write your code here — store prefix sums
    }

    int sumRange(int left, int right) {
        // Write your code here — O(1) using prefix sums
        return 0;
    }
};
// USER_CODE_END

void test(vector<int> nums, int left, int right, int expected, int tc, bool hidden = false) {
    int got = CodeCoder(nums).sumRange(left, right);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:expected=" << expected << ":got=" << got << "\n";
    }
}

int main() {
    try { test({-2,0,3,-5,2,-1}, 0, 2, 1, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test({-2,0,3,-5,2,-1}, 2, 5, -1, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test({-2,0,3,-5,2,-1}, 0, 5, -3, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test({5}, 0, 0, 5, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test({1,2,3}, 1, 2, 5, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test({100,-50,25,-75,200}, 0, 4, 200, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test({-5,-5,-5,-5}, 0, 3, -20, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test({10,20,30,40,50}, 0, 0, 10, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test({10,20,30,40,50}, 4, 4, 50, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test({0,0,0,0,0}, 1, 3, 0, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def __init__(self, nums):
        # Write your code here — store prefix sums
        pass

    def sumRange(self, left, right):
        # Write your code here — O(1) using prefix sums
        return 0
# USER_CODE_END

def test(nums, left, right, expected, tc, hidden=False):
    got = CodeCoder(nums).sumRange(left, right)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:nums={nums}:left={left}:right={right}:expected={expected}:got={got}")

try: test([-2,0,3,-5,2,-1], 0, 2, 1, 1)
except: print("TC:1:FAIL:hidden")
try: test([-2,0,3,-5,2,-1], 2, 5, -1, 2)
except: print("TC:2:FAIL:hidden")
try: test([-2,0,3,-5,2,-1], 0, 5, -3, 3)
except: print("TC:3:FAIL:hidden")
try: test([5], 0, 0, 5, 4)
except: print("TC:4:FAIL:hidden")
try: test([1,2,3], 1, 2, 5, 5)
except: print("TC:5:FAIL:hidden")
try: test([100,-50,25,-75,200], 0, 4, 200, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([-5,-5,-5,-5], 0, 3, -20, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([10,20,30,40,50], 0, 0, 10, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([10,20,30,40,50], 4, 4, 50, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([0,0,0,0,0], 1, 3, 0, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
class CodeCoder {
    constructor(nums) {
        // Write your code here — store prefix sums
    }
    sumRange(left, right) {
        // Write your code here — O(1) using prefix sums
        return 0;
    }
}
// USER_CODE_END

function test(nums, left, right, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = new CodeCoder(nums).sumRange(left, right);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:nums=" + JSON.stringify(nums)
            + ":left=" + left + ":right=" + right
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test([-2,0,3,-5,2,-1], 0, 2, 1, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([-2,0,3,-5,2,-1], 2, 5, -1, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([-2,0,3,-5,2,-1], 0, 5, -3, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([5], 0, 0, 5, 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([1,2,3], 1, 2, 5, 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([100,-50,25,-75,200], 0, 4, 200, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test([-5,-5,-5,-5], 0, 3, -20, 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test([10,20,30,40,50], 0, 0, 10, 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test([10,20,30,40,50], 4, 4, 50, 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test([0,0,0,0,0], 1, 3, 0, 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
typedef struct {
    int* prefix;   // prefix[i] = sum of nums[0..i-1]
    int n;
} CodeCoder;

CodeCoder* codeCoderCreate(int* nums, int numsSize) {
    // Write your code here — build prefix sum array
    CodeCoder* obj = (CodeCoder*)malloc(sizeof(CodeCoder));
    return obj;
}

int codeCoderSumRange(CodeCoder* obj, int left, int right) {
    // Write your code here — O(1) using prefix sums
    return 0;
}

void codeCoderFree(CodeCoder* obj) {
    free(obj);
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void runTest(int* nums, int n, int left, int right, int expected, int tc, int hidden) {
    CodeCoder* obj = codeCoderCreate(nums, n);
    int got = codeCoderSumRange(obj, left, right);
    codeCoderFree(obj);
    if (got == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:expected=%d:got=%d\n", tc, expected, got);
    }
}

int main() {
    int n1[] = {-2,0,3,-5,2,-1};
    runTest(n1, 6, 0, 2, 1, 1, 0);
    runTest(n1, 6, 2, 5, -1, 2, 0);
    runTest(n1, 6, 0, 5, -3, 3, 0);

    int n2[] = {5};
    runTest(n2, 1, 0, 0, 5, 4, 0);

    int n3[] = {1,2,3};
    runTest(n3, 3, 1, 2, 5, 5, 0);

    int n4[] = {100,-50,25,-75,200};
    runTest(n4, 5, 0, 4, 200, 6, 1);

    int n5[] = {-5,-5,-5,-5};
    runTest(n5, 4, 0, 3, -20, 7, 1);

    int n6[] = {10,20,30,40,50};
    runTest(n6, 5, 0, 0, 10, 8, 1);
    runTest(n6, 5, 4, 4, 50, 9, 1);

    int n7[] = {0,0,0,0,0};
    runTest(n7, 5, 1, 3, 0, 10, 1);

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

print(f"\nRange Sum Query - Immutable (pid={pid}) — done!")
cur.close()
conn.close()
