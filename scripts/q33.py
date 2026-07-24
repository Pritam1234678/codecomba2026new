"""
Permutation in String
=====================
Given two strings s1 and s2, return true if s2 contains a permutation of s1.

In other words, one of s1's permutations is a substring of s2.

Examples:
  s1 = "ab", s2 = "eidbaooo" → true  (s2 contains "ba" which is a permutation of "ab")
  s1 = "ab", s2 = "eidboaoo" → false (s2 does NOT contain any permutation of "ab")

Approach: Sliding window with character frequency count.
- Count characters of s1 in a freq array.
- Slide a window of size s1.length() over s2.
- For each window, compare character counts.
- If all counts match exactly, return true.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Permutation in String"
desc = (
    "Given two strings s1 and s2, return true if s2 contains a permutation of s1.\n\n"
    "A permutation of s1 is a rearranged version of s1 using all its characters "
    "exactly once. For s2 to contain a permutation of s1, there must exist a "
    "contiguous substring of s2 that is a permutation of s1.\n\n"
    "For example:\n"
    "- s1 = \"ab\", s2 = \"eidbaooo\" → true because \"ba\" in s2 is a permutation of \"ab\".\n"
    "- s1 = \"ab\", s2 = \"eidboaoo\" → false because no substring matches.\n\n"
    "Use a sliding window approach with character frequency counts. Maintain "
    "a window of size s1.length() over s2, and check if the character counts match."
)
infmt = "First line contains string s1.\nSecond line contains string s2."
outfmt = "Print 'true' if s2 contains a permutation of s1, otherwise 'false'."
cons = (
    "1 \u2264 |s1|, |s2| \u2264 10^4\n"
    "Both strings consist of lowercase English letters only."
)
e1 = (
    "Input:\n"
    "ab\n"
    "eidbaooo\n\n"
    "Output:\n"
    "true\n\n"
    "Explanation: s2 contains \"ba\" which is a permutation of \"ab\"."
)
e2 = (
    "Input:\n"
    "ab\n"
    "eidboaoo\n\n"
    "Output:\n"
    "false\n\n"
    "Explanation: No substring of s2 is a permutation of \"ab\"."
)
e3 = (
    "Input:\n"
    "adc\n"
    "dcda\n\n"
    "Output:\n"
    "true\n\n"
    "Explanation: s2 contains \"cda\" at indices 2-4 which rearranges to \"adc\"."
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True,
     "String, Sliding Window, Hash Table", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean checkInclusion(String s1, String s2) {
        // Write your code here — sliding window with character counts
        return false;
    }
}
// USER_CODE_END

public class Main {
    static void test(String s1, String s2, boolean expected, int tc, boolean hidden) {
        boolean got = new CodeCoder().checkInclusion(s1, s2);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:s1=" + s1 + " s2=" + s2
                + ":expected=" + (expected ? "true" : "false")
                + ":got=" + (got ? "true" : "false"));
        }
    }

    public static void main(String[] args) {
        try { test("ab", "eidbaooo", true, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test("ab", "eidboaoo", false, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test("adc", "dcda", true, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test("a", "a", true, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test("a", "b", false, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test("abc", "cbaebabacd", true, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test("hello", "ooolleoooleh", false, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test("xyz", "xxyyzz", false, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test("ab", "ab", true, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test("abcd", "dcba", false, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    bool checkInclusion(string s1, string s2) {
        // Write your code here — sliding window with character counts
        return false;
    }
};
// USER_CODE_END

void test(string s1, string s2, bool expected, int tc, bool hidden = false) {
    bool got = CodeCoder().checkInclusion(s1, s2);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:s1=" << s1 << " s2=" << s2
             << ":expected=" << (expected ? "true" : "false")
             << ":got=" << (got ? "true" : "false") << "\n";
    }
}

int main() {
    try { test("ab", "eidbaooo", true, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test("ab", "eidboaoo", false, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test("adc", "dcda", true, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test("a", "a", true, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test("a", "b", false, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test("abc", "cbaebabacd", true, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test("hello", "ooolleoooleh", false, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test("xyz", "xxyyzz", false, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test("ab", "ab", true, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test("abcd", "dcba", false, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def checkInclusion(self, s1, s2):
        # Write your code here — sliding window with character counts
        return False
# USER_CODE_END

def test(s1, s2, expected, tc, hidden=False):
    got = CodeCoder().checkInclusion(s1, s2)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:s1={s1}:s2={s2}:expected={expected}:got={got}")

try: test("ab", "eidbaooo", True, 1)
except: print("TC:1:FAIL:hidden")
try: test("ab", "eidboaoo", False, 2)
except: print("TC:2:FAIL:hidden")
try: test("adc", "dcda", True, 3)
except: print("TC:3:FAIL:hidden")
try: test("a", "a", True, 4)
except: print("TC:4:FAIL:hidden")
try: test("a", "b", False, 5)
except: print("TC:5:FAIL:hidden")
try: test("abc", "cbaebabacd", True, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test("hello", "ooolleoooleh", False, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test("xyz", "xxyyzz", False, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test("ab", "ab", True, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test("abcd", "dcba", False, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function checkInclusion(s1, s2) {
    // Write your code here — sliding window with character counts
    return false;
}
// USER_CODE_END

function test(s1, s2, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = checkInclusion(s1, s2);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:s1=" + s1 + ":s2=" + s2
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test("ab", "eidbaooo", true, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test("ab", "eidboaoo", false, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test("adc", "dcda", true, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test("a", "a", true, 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test("a", "b", false, 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test("abc", "cbaebabacd", true, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test("hello", "ooolleoooleh", false, 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test("xyz", "xxyyzz", false, 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test("ab", "ab", true, 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test("abcd", "dcba", false, 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <stdbool.h>
#include <string.h>

// USER_CODE_START
bool checkInclusion(char* s1, char* s2) {
    // Write your code here — sliding window with character counts
    return false;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void runTest(char* s1, char* s2, bool expected, int tc, int hidden) {
    bool got = checkInclusion(s1, s2);
    if (got == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:s1=%s:s2=%s:expected=%s:got=%s\n",
               tc, s1, s2, expected ? "true" : "false", got ? "true" : "false");
    }
}

int main() {
    runTest("ab", "eidbaooo", true, 1, 0);
    runTest("ab", "eidboaoo", false, 2, 0);
    runTest("adc", "dcda", true, 3, 0);
    runTest("a", "a", true, 4, 0);
    runTest("a", "b", false, 5, 0);
    runTest("abc", "cbaebabacd", true, 6, 1);
    runTest("hello", "ooolleoooleh", false, 7, 1);
    runTest("xyz", "xxyyzz", false, 8, 1);
    runTest("ab", "ab", true, 9, 1);
    runTest("abcd", "dcba", false, 10, 1);
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

print(f"\nPermutation in String (pid={pid}) — done!")
cur.close()
conn.close()
