import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

# ── PROBLEM DATA ──
title = "Contains Duplicate"
description = "Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct."
input_format = "First line contains integer n (size of array).\nSecond line contains n space-separated integers representing nums."
output_format = "Print true if any value appears at least twice, otherwise print false."
constraints = "1 ≤ n ≤ 10^5\n-10^9 ≤ nums[i] ≤ 10^9"
time_limit = 3.0
memory_limit = 256
level = "EASY"
topics = "Array, Hash Table"
example1 = "Input:\n4\n1 2 3 1\n\nOutput:\ntrue\n\nExplanation: The value 1 appears twice at indices 0 and 3."
example2 = "Input:\n4\n1 2 3 4\n\nOutput:\nfalse\n\nExplanation: All elements are distinct."
example3 = "Input:\n1\n1\n\nOutput:\nfalse\n\nExplanation: Single element array has no duplicates."

# Insert problem
cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id
""", (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, True, topics, example1, example2, example3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

# ── HARNESSES ──

java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public boolean containsDuplicate(int[] nums) {
        // Write your code here
        return false;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, boolean expected, int tc, boolean hidden) {
        boolean got = new Solution().containsDuplicate(nums);
        if (got == expected)
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(nums) + ":expected=" + expected + ":got=" + got);
    }

    public static void main(String[] a) {
        try { test(new int[]{1,2,3,1}, true, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:input=[1,2,3,1]:expected=true:got=ERR"); }
        try { test(new int[]{1,2,3,4}, false, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:input=[1,2,3,4]:expected=false:got=ERR"); }
        try { test(new int[]{1,1,1,3,3,4,3,2,4,2}, true, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:input=[1,1,1,3,3,4,3,2,4,2]:expected=true:got=ERR"); }
        try { test(new int[]{1}, false, 4, true); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{-1,-2,-3,-4,-5,5,4,3,2,1}, false, 5, true); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{1000000000,-1000000000,1000000000}, true, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    bool containsDuplicate(vector<int>& nums) {
        // Write your code here
        return false;
    }
};
// USER_CODE_END

void test(vector<int> nums, bool expected, int tc, bool hidden = false) {
    Solution sol;
    bool got = sol.containsDuplicate(nums);
    if (got == expected)
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:input=[";
        for (size_t i = 0; i < nums.size(); i++) { if (i) cout << ","; cout << nums[i]; }
        cout << "]:expected=" << (expected ? "true" : "false") << ":got=" << (got ? "true" : "false") << "\\n";
    }
}

int main() {
    try { test({1,2,3,1}, true, 1); }
    catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1,2,3,4}, false, 2); }
    catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({1,1,1,3,3,4,3,2,4,2}, true, 3); }
    catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({1}, false, 4, true); }
    catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({-1,-2,-3,-4,-5,5,4,3,2,1}, false, 5, true); }
    catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({1000000000,-1000000000,1000000000}, true, 6, true); }
    catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

py_code = '''# USER_CODE_START
class Solution:
    def containsDuplicate(self, nums):
        # Write your code here
        return False
# USER_CODE_END

def test(nums, expected, tc, hidden=False):
    got = Solution().containsDuplicate(nums)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:input={nums}:expected={expected}:got={got}")

try: test([1,2,3,1], True, 1)
except: print("TC:1:FAIL:hidden")
try: test([1,2,3,4], False, 2)
except: print("TC:2:FAIL:hidden")
try: test([1,1,1,3,3,4,3,2,4,2], True, 3)
except: print("TC:3:FAIL:hidden")
try: test([1], False, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([-1,-2,-3,-4,-5,5,4,3,2,1], False, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([1000000000,-1000000000,1000000000], True, 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

js_code = '''// USER_CODE_START
function containsDuplicate(nums) {
    // Write your code here
    return false;
}
// USER_CODE_END

function test(nums, expected, tc, hidden = false) {
    const got = containsDuplicate(nums);
    if (got === expected)
        console.log(`TC:${tc}:PASS` + (hidden ? ':hidden' : ''));
    else if (hidden)
        console.log(`TC:${tc}:FAIL:hidden`);
    else
        console.log(`TC:${tc}:FAIL:input=${JSON.stringify(nums)}:expected=${expected}:got=${got}`);
}

try { test([1,2,3,1], true, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([1,2,3,4], false, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([1,1,1,3,3,4,3,2,4,2], true, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([1], false, 4, true); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([-1,-2,-3,-4,-5,5,4,3,2,1], false, 5, true); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([1000000000,-1000000000,1000000000], true, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }'''

c_code = '''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
bool containsDuplicate(int* nums, int numsSize) {
    // Write your code here
    return false;
}
// USER_CODE_END

void test(int* nums, int n, bool expected, int tc, int hidden) {
    bool got = containsDuplicate(nums, n);
    if (got == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else {
            printf("TC:%d:FAIL:input=[", tc);
            for (int i = 0; i < n; i++) { if (i) printf(","); printf("%d", nums[i]); }
            printf("]:expected=%s:got=%s\\n", expected ? "true" : "false", got ? "true" : "false");
        }
    }
}

int main() {
    int t1[] = {1,2,3,1}; test(t1, 4, true, 1, 0);
    int t2[] = {1,2,3,4}; test(t2, 4, false, 2, 0);
    int t3[] = {1,1,1,3,3,4,3,2,4,2}; test(t3, 10, true, 3, 0);
    int t4[] = {1}; test(t4, 1, false, 4, 1);
    int t5[] = {-1,-2,-3,-4,-5,5,4,3,2,1}; test(t5, 10, false, 5, 1);
    int t6[] = {1000000000,-1000000000,1000000000}; test(t6, 3, true, 6, 1);
    return 0;
}'''

# Insert code snippets
snippets = [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]
for lang, code in snippets:
    cur.execute("""
        INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
        VALUES (%s, %s, %s, NOW(), NOW())
    """, (pid, lang, code))
    print(f"  {lang} snippet inserted")

conn.commit()
print(f"All done! Problem {title} (pid={pid}) inserted with 5 language harnesses.")

cur.close()
conn.close()
