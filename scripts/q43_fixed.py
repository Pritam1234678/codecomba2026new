import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title = "Subsets"
desc = "Given an integer array nums of unique elements, return all possible subsets (the power set). The solution set must contain every subset of the input array, including the empty subset. The order of subsets in the output does not matter, and each subset should contain its elements in the order they appear in nums. For example, nums = [1,2,3] produces 2^3 = 8 subsets: [], [1], [2], [1,2], [3], [1,3], [2,3], [1,2,3]."
infmt = "First line contains integer n (size of array).\nSecond line contains n space-separated unique integers."
outfmt = "Print each subset on a separate line. Elements within a subset should be space-separated. Print an empty line to represent the empty subset."
cons = "1 \u2264 n \u2264 10\n-10 \u2264 nums[i] \u2264 10\nAll elements in nums are distinct."
e1 = "Input:\n3\n1 2 3\n\nOutput:\n\n1\n2\n1 2\n3\n1 3\n2 3\n1 2 3"
e2 = "Input:\n1\n0\n\nOutput:\n\n0"
e3 = "Input:\n2\n1 2\n\nOutput:\n\n1\n2\n1 2"

cur.execute("""INSERT INTO problems (title, description, input_format, output_format, constraints,
    time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id""",
    (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True, "Array, Backtracking, Bit Manipulation", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public List<List<Integer>> subsets(int[] nums) {
        // Write your code here
        return new ArrayList<>();
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int expectedSize, int tc, boolean hidden) {
        List<List<Integer>> result = new Solution().subsets(nums);
        if (result.size() == expectedSize)
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(nums) + ":expected size=" + expectedSize + ":got=" + result.size());
    }
    public static void main(String[] args) {
        try { test(new int[]{1, 2, 3}, 8, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{0}, 2, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{1, 2}, 4, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{1, 2, 3, 4}, 16, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{5}, 2, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{-1, 1}, 4, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    vector<vector<int>> subsets(vector<int>& nums) {
        // Write your code here
        return {};
    }
};
// USER_CODE_END

void test(vector<int> nums, int expectedSize, int tc, bool hidden = false) {
    Solution sol;
    vector<vector<int>> result = sol.subsets(nums);
    if ((int)result.size() == expectedSize)
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:input=[";
        for (size_t i = 0; i < nums.size(); i++) { if (i) cout << ","; cout << nums[i]; }
        cout << "]:expected size=" << expectedSize << ":got=" << result.size() << "\\n";
    }
}

int main() {
    try { test({1, 2, 3}, 8, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({0}, 2, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({1, 2}, 4, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({1, 2, 3, 4}, 16, 4, true); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({5}, 2, 5, true); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({-1, 1}, 4, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def subsets(self, nums):
        # Write your code here
        return []
# USER_CODE_END

def test(nums, expected_size, tc, hidden=False):
    result = Solution().subsets(nums)
    if len(result) == expected_size:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:input={nums}:expected size={expected_size}:got={len(result)}")

try: test([1, 2, 3], 8, 1)
except: print("TC:1:FAIL:hidden")
try: test([0], 2, 2)
except: print("TC:2:FAIL:hidden")
try: test([1, 2], 4, 3)
except: print("TC:3:FAIL:hidden")
try: test([1, 2, 3, 4], 16, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([5], 2, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([-1, 1], 4, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function subsets(nums) {
    // Write your code here
    return [];
}
// USER_CODE_END

function test(nums, expectedSize, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const result = subsets(nums);
    if (result.length === expectedSize)
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden)
        console.log("TC:" + tc + ":FAIL:hidden");
    else
        console.log("TC:" + tc + ":FAIL:input=" + JSON.stringify(nums) + ":expected size=" + expectedSize + ":got=" + result.length);
}

try { test([1, 2, 3], 8, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([0], 2, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([1, 2], 4, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([1, 2, 3, 4], 16, 4, true); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([5], 2, 5, true); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([-1, 1], 4, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int** subsets(int* nums, int numsSize, int* returnSize, int** returnColumnSizes) {
    // Write your code here
    *returnSize = 0;
    return NULL;
}
// USER_CODE_END

void runTest(int* nums, int n, int expectedSize, int tc, int hidden) {
    int returnSize;
    int* colSizes = (int*)malloc(10000 * sizeof(int));
    int** result = subsets(nums, n, &returnSize, &colSizes);
    if (result != NULL && returnSize == expectedSize)
        printf(hidden ? "TC:%d:PASS:hidden\\n" : "TC:%d:PASS\\n", tc);
    else if (returnSize == expectedSize && result == NULL)
        printf(hidden ? "TC:%d:PASS:hidden\\n" : "TC:%d:PASS\\n", tc);
    else
        printf(hidden ? "TC:%d:FAIL:hidden\\n" : "TC:%d:FAIL:hidden\\n", tc);
    free(colSizes);
}

int main() {
    int a1[] = {1, 2, 3};
    runTest(a1, 3, 8, 1, 0);

    int a2[] = {0};
    runTest(a2, 1, 2, 2, 0);

    int a3[] = {1, 2};
    runTest(a3, 2, 4, 3, 0);

    int a4[] = {1, 2, 3, 4};
    runTest(a4, 4, 16, 4, 1);

    int a5[] = {5};
    runTest(a5, 1, 2, 5, 1);

    int a6[] = {-1, 1};
    runTest(a6, 2, 4, 6, 1);

    return 0;
}'''

for lang, code in [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]:
    cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s, %s, %s, NOW(), NOW())", (pid, lang, code))
conn.commit()
print(f"All 5 snippets for {title} (pid={pid})")
cur.close()
conn.close()
