import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

# ── PROBLEM DATA: Merge Sorted Array ──
title = "Merge Sorted Array"
description = "You are given two integer arrays nums1 and nums2, sorted in non-decreasing order, and two integers m and n, representing the number of elements in nums1 and nums2 respectively. Merge nums1 and nums2 into a single array sorted in non-decreasing order. The final sorted array should be stored inside the array nums1. To accommodate this, nums1 has a length of m + n, where the first m elements denote the elements that should be merged, and the last n elements are set to 0 and should be ignored."
input_format = "First line contains integer m (number of actual elements in nums1).\nSecond line contains m space-separated integers representing nums1 (first m elements, rest n elements are 0).\nThird line contains integer n (number of elements in nums2).\nFourth line contains n space-separated integers representing nums2."
output_format = "Print m+n space-separated integers — the merged sorted array."
constraints = "0 ≤ m, n ≤ 200\n1 ≤ m + n ≤ 200\n-10^9 ≤ nums1[i], nums2[i] ≤ 10^9"
time_limit = 3.0
memory_limit = 256
level = "EASY"
topics = "Array, Two Pointers"
example1 = "Input:\n3\n1 2 3\n3\n2 5 6\n\nOutput:\n1 2 2 3 5 6\n\nExplanation: Merging [1,2,3] and [2,5,6] results in [1,2,2,3,5,6]."
example2 = "Input:\n1\n1\n0\n\nOutput:\n1\n\nExplanation: Merging [1] and [] results in [1]."
example3 = "Input:\n0\n\n1\n1\n\nOutput:\n1\n\nExplanation: Merging [] and [1] results in [1]."

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id
""", (title, description, input_format, output_format, constraints, time_limit, memory_limit, level, True, topics, example1, example2, example3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

# ── JAVA HARNESS ──
java_code = '''import java.util.*;

// USER_CODE_START
class Solution {
    public void merge(int[] nums1, int m, int[] nums2, int n) {
        // Write your code here
    }
}
// USER_CODE_END

public class Main {
    static int[] trim(int[] arr, int len) {
        return java.util.Arrays.copyOf(arr, len);
    }

    static void test(int[] nums1, int m, int[] nums2, int n, int[] expected, int tc, boolean hidden) {
        int[] copy = new int[nums1.length];
        System.arraycopy(nums1, 0, copy, 0, nums1.length);
        new Solution().merge(copy, m, nums2, n);
        int[] got = trim(copy, m + n);
        int[] exp = trim(expected, m + n);
        if (java.util.Arrays.equals(got, exp))
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=nums1=" + Arrays.toString(trim(nums1, m)) + " nums2=" + Arrays.toString(nums2) + ":expected=" + Arrays.toString(exp) + ":got=" + Arrays.toString(got));
    }

    public static void main(String[] a) {
        try { test(new int[]{1,2,3,0,0,0}, 3, new int[]{2,5,6}, 3, new int[]{1,2,2,3,5,6}, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:input=nums1=[1,2,3] nums2=[2,5,6]:expected=[1,2,2,3,5,6]:got=ERR"); }
        try { test(new int[]{1}, 1, new int[]{}, 0, new int[]{1}, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:input=nums1=[1] nums2=[]:expected=[1]:got=ERR"); }
        try { test(new int[]{0}, 0, new int[]{1}, 1, new int[]{1}, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:input=nums1=[] nums2=[1]:expected=[1]:got=ERR"); }
        try { test(new int[]{4,5,6,0,0,0}, 3, new int[]{1,2,3}, 3, new int[]{1,2,3,4,5,6}, 4, true); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{-1,0,0,0,0,0}, 2, new int[]{-2,1,2,3}, 4, new int[]{-2,-1,0,1,2,3}, 5, true); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{0,0,0,0,0}, 0, new int[]{1,2,3,4,5}, 5, new int[]{1,2,3,4,5}, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }
}'''

# ── CPP HARNESS ──
cpp_code = '''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class Solution {
public:
    void merge(vector<int>& nums1, int m, vector<int>& nums2, int n) {
        // Write your code here
    }
};
// USER_CODE_END

vector<int> trim(vector<int>& arr, int len) {
    return vector<int>(arr.begin(), arr.begin() + len);
}

void test(vector<int> nums1, int m, vector<int> nums2, int n, vector<int> expected, int tc, bool hidden = false) {
    Solution sol;
    sol.merge(nums1, m, nums2, n);
    vector<int> got = trim(nums1, m + n);
    vector<int> exp = trim(expected, m + n);
    if (got == exp)
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\\n";
    else if (hidden)
        cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:input=nums1=[";
        for (int i = 0; i < m; i++) { if (i) cout << ","; cout << nums1[i]; }
        cout << "] nums2=[";
        for (size_t i = 0; i < nums2.size(); i++) { if (i) cout << ","; cout << nums2[i]; }
        cout << "]:expected=[";
        for (size_t i = 0; i < exp.size(); i++) { if (i) cout << ","; cout << exp[i]; }
        cout << "]:got=[";
        for (size_t i = 0; i < got.size(); i++) { if (i) cout << ","; cout << got[i]; }
        cout << "]\\n";
    }
}

int main() {
    try { test({1,2,3,0,0,0}, 3, {2,5,6}, 3, {1,2,2,3,5,6}, 1); }
    catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1}, 1, {}, 0, {1}, 2); }
    catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({0}, 0, {1}, 1, {1}, 3); }
    catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({4,5,6,0,0,0}, 3, {1,2,3}, 3, {1,2,3,4,5,6}, 4, true); }
    catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({-1,0,0,0,0,0}, 2, {-2,1,2,3}, 4, {-2,-1,0,1,2,3}, 5, true); }
    catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({0,0,0,0,0}, 0, {1,2,3,4,5}, 5, {1,2,3,4,5}, 6, true); }
    catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    return 0;
}'''

# ── PYTHON HARNESS ──
py_code = '''# USER_CODE_START
class Solution:
    def merge(self, nums1, m, nums2, n):
        """
        Do not return anything, modify nums1 in-place instead.
        """
        pass
# USER_CODE_END

def test(nums1, m, nums2, n, expected, tc, hidden=False):
    copy = nums1[:]
    Solution().merge(copy, m, nums2, n)
    got = copy[:m+n]
    exp = expected[:m+n]
    if got == exp:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        inp1 = nums1[:m]
        print(f"TC:{tc}:FAIL:input=nums1={inp1} nums2={nums2}:expected={exp}:got={got}")

try: test([1,2,3,0,0,0], 3, [2,5,6], 3, [1,2,2,3,5,6], 1)
except: print("TC:1:FAIL:hidden")
try: test([1], 1, [], 0, [1], 2)
except: print("TC:2:FAIL:hidden")
try: test([0], 0, [1], 1, [1], 3)
except: print("TC:3:FAIL:hidden")
try: test([4,5,6,0,0,0], 3, [1,2,3], 3, [1,2,3,4,5,6], 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([-1,0,0,0,0,0], 2, [-2,1,2,3], 4, [-2,-1,0,1,2,3], 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([0,0,0,0,0], 0, [1,2,3,4,5], 5, [1,2,3,4,5], 6, hidden=True)
except: print("TC:6:FAIL:hidden")'''

# ── JAVASCRIPT HARNESS ──
js_code = '''// USER_CODE_START
function merge(nums1, m, nums2, n) {
    // Write your code here
    // nums1 is modified in-place
}
// USER_CODE_END

function test(nums1, m, nums2, n, expected, tc, hidden = false) {
    const copy = [...nums1];
    merge(copy, m, nums2, n);
    const got = copy.slice(0, m + n);
    const exp = expected.slice(0, m + n);
    const gotStr = JSON.stringify(got);
    const expStr = JSON.stringify(exp);
    if (gotStr === expStr)
        console.log(`TC:${tc}:PASS` + (hidden ? ':hidden' : ''));
    else if (hidden)
        console.log(`TC:${tc}:FAIL:hidden`);
    else
        console.log(`TC:${tc}:FAIL:input=nums1=${JSON.stringify(nums1.slice(0, m))} nums2=${JSON.stringify(nums2)}:expected=${expStr}:got=${gotStr}`);
}

try { test([1,2,3,0,0,0], 3, [2,5,6], 3, [1,2,2,3,5,6], 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test([1], 1, [], 0, [1], 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test([0], 0, [1], 1, [1], 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test([4,5,6,0,0,0], 3, [1,2,3], 3, [1,2,3,4,5,6], 4, true); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test([-1,0,0,0,0,0], 2, [-2,1,2,3], 4, [-2,-1,0,1,2,3], 5, true); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test([0,0,0,0,0], 0, [1,2,3,4,5], 5, [1,2,3,4,5], 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }'''

# ── C HARNESS ──
c_code = '''#include <stdio.h>

// USER_CODE_START
void merge(int* nums1, int nums1Size, int m, int* nums2, int nums2Size, int n) {
    // Write your code here
}
// USER_CODE_END

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void test(int* nums1, int m, int* nums2, int n, int* expected, int tc, int hidden) {
    int copy[205];
    for (int i = 0; i < m + n; i++) copy[i] = nums1[i];
    merge(copy, m + n, m, nums2, n, n);
    if (arrEq(copy, expected, m + n)) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else {
            printf("TC:%d:FAIL:input=nums1=[", tc);
            for (int i = 0; i < m; i++) { if (i) printf(","); printf("%d", nums1[i]); }
            printf("] nums2=[");
            for (int i = 0; i < n; i++) { if (i) printf(","); printf("%d", nums2[i]); }
            printf("]:expected=[");
            for (int i = 0; i < m + n; i++) { if (i) printf(","); printf("%d", expected[i]); }
            printf("]:got=[");
            for (int i = 0; i < m + n; i++) { if (i) printf(","); printf("%d", copy[i]); }
            printf("]\\n");
        }
    }
}

int main() {
    int t1[] = {1,2,3,0,0,0}; int e1[] = {1,2,2,3,5,6}; test(t1, 3, (int[]){2,5,6}, 3, e1, 1, 0);
    int t2[] = {1}; int e2[] = {1}; test(t2, 1, (int[]){}, 0, e2, 2, 0);
    int t3[] = {0}; int e3[] = {1}; test(t3, 0, (int[]){1}, 1, e3, 3, 0);
    int t4[] = {4,5,6,0,0,0}; int e4[] = {1,2,3,4,5,6}; test(t4, 3, (int[]){1,2,3}, 3, e4, 4, 1);
    int t5[] = {-1,0,0,0,0,0}; int e5[] = {-2,-1,0,1,2,3}; test(t5, 2, (int[]){-2,1,2,3}, 4, e5, 5, 1);
    int t6[] = {0,0,0,0,0}; int e6[] = {1,2,3,4,5}; test(t6, 0, (int[]){1,2,3,4,5}, 5, e6, 6, 1);
    return 0;
}'''

snippets = [("JAVA", java_code), ("CPP", cpp_code), ("PYTHON", py_code), ("JAVASCRIPT", js_code), ("C", c_code)]
for lang, code in snippets:
    cur.execute("""
        INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at)
        VALUES (%s, %s, %s, NOW(), NOW())
    """, (pid, lang, code))
    print(f"  {lang} snippet inserted")

conn.commit()
print(f"All done! {title} (pid={pid}) inserted with 5 language harnesses.")
cur.close()
conn.close()
