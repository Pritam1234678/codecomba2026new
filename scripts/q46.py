"""
Valid Parentheses
=================
Problem: Given a string s containing just the characters '(', ')', '{', '}', '[' and ']',
determine if the input string is valid.

A string is valid if:
1. Open brackets must be closed by the same type of brackets.
2. Open brackets must be closed in the correct order.
3. Every close bracket has a corresponding open bracket of the same type.

Examples:
  "()"     → true   (simple pair)
  "()[]{}" → true   (multiple pairs, order correct)
  "(]"     → false  (wrong type match)
  "([)]"   → false  (wrong order — closes ] before ) even though [ opened after ()
  "{[]}"   → true   (nested correctly)

10 test cases — 5 visible, 5 hidden.
Class name: CodeCoder (not Solution)
"""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Valid Parentheses"
desc = (
    "Given a string s containing just the characters '(', ')', '{', '}', '[' and ']', "
    "determine if the input string is valid.\n\n"
    "An input string is valid if:\n"
    "1. Open brackets must be closed by the same type of brackets.\n"
    "2. Open brackets must be closed in the correct order.\n"
    "3. Every close bracket has a corresponding open bracket of the same type.\n\n"
    "Use a stack data structure to solve this. When you see an open bracket, push it onto the stack. "
    "When you see a close bracket, check if the top of the stack matches it. If yes, pop; if no, return false. "
    "At the end, the stack should be empty for a valid string."
)
infmt = "Single line containing string s."
outfmt = "Print 'true' if the string is valid, otherwise 'false'."
cons = (
    "1 \u2264 |s| \u2264 10^4\n"
    "s consists of parentheses characters '()[]{}' only."
)
e1 = "Input:\n()\n\nOutput:\ntrue\n\nExplanation: '(' opens and ')' closes correctly — simple valid pair."
e2 = "Input:\n()[]{}\n\nOutput:\ntrue\n\nExplanation: Three independent valid pairs in sequence — all open brackets are closed in order."
e3 = "Input:\n(]\n\nOutput:\nfalse\n\nExplanation: '(' expects ')', but ']' is encountered instead — type mismatch."

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 3.0, 256, "EASY", True,
     "Stack, String", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

# ══════════════════════════════════════════════════════════════
# JAVA HARNESS
# ══════════════════════════════════════════════════════════════
java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public boolean isValid(String s) {
        // Write your code here — use a stack to match brackets
        return false;
    }
}
// USER_CODE_END

public class Main {
    static void test(String s, boolean expected, int tc, boolean hidden) {
        boolean got = new CodeCoder().isValid(s);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:input=" + s
                + ":expected=" + (expected ? "true" : "false")
                + ":got=" + (got ? "true" : "false"));
        }
    }

    public static void main(String[] args) {
        // Visible tests
        try { test("()", true, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test("()[]{}", true, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test("(]", false, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test("([)]", false, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test("{[]}", true, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        // Hidden tests
        try { test("", true, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test("[", false, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test("(((((((()", false, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test("((()))", true, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test("(){[()]}[{}]", true, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

# ══════════════════════════════════════════════════════════════
# CPP HARNESS
# ══════════════════════════════════════════════════════════════
cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    bool isValid(string s) {
        // Write your code here — use a stack to match brackets
        return false;
    }
};
// USER_CODE_END

void test(string s, bool expected, int tc, bool hidden = false) {
    CodeCoder obj;
    bool got = obj.isValid(s);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:input=" << s
             << ":expected=" << (expected ? "true" : "false")
             << ":got=" << (got ? "true" : "false") << "\n";
    }
}

int main() {
    try { test("()", true, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test("()[]{}", true, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test("(]", false, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test("([)]", false, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test("{[]}", true, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test("", true, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test("[", false, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test("(((((((()", false, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test("((()))", true, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test("(){[()]}[{}]", true, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

# ══════════════════════════════════════════════════════════════
# PYTHON HARNESS
# ══════════════════════════════════════════════════════════════
py_code = r'''# USER_CODE_START
class CodeCoder:
    def isValid(self, s):
        # Write your code here — use a stack to match brackets
        return False
# USER_CODE_END

def test(s, expected, tc, hidden=False):
    got = CodeCoder().isValid(s)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:input={repr(s)}:expected={expected}:got={got}")

try: test("()", True, 1)
except: print("TC:1:FAIL:hidden")
try: test("()[]{}", True, 2)
except: print("TC:2:FAIL:hidden")
try: test("(]", False, 3)
except: print("TC:3:FAIL:hidden")
try: test("([)]", False, 4)
except: print("TC:4:FAIL:hidden")
try: test("{[]}", True, 5)
except: print("TC:5:FAIL:hidden")
try: test("", True, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test("[", False, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test("(((((((()", False, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test("((()))", True, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test("(){[()]}[{}]", True, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

# ══════════════════════════════════════════════════════════════
# JAVASCRIPT HARNESS
# ══════════════════════════════════════════════════════════════
js_code = r'''// USER_CODE_START
function isValid(s) {
    // Write your code here — use a stack to match brackets
    return false;
}
// USER_CODE_END

function test(s, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = isValid(s);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:input=" + JSON.stringify(s)
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test("()", true, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test("()[]{}", true, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test("(]", false, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test("([)]", false, 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test("{[]}", true, 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test("", true, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test("[", false, 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test("(((((((()", false, 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test("((()))", true, 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test("(){[()]}[{}]", true, 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

# ══════════════════════════════════════════════════════════════
# C HARNESS
# ══════════════════════════════════════════════════════════════
c_code = r'''#include <stdio.h>
#include <stdbool.h>
#include <string.h>

// USER_CODE_START
bool isValid(char* s) {
    // Write your code here — use a stack to match brackets
    return false;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void test(char* s, bool expected, int tc, int hidden) {
    bool got = isValid(s);
    if (got == expected) {
        if (hidden)
            printf("TC:%d:PASS:hidden\n", tc);
        else
            printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) {
            printf("TC:%d:FAIL:hidden\n", tc);
        } else {
            printf("TC:%d:FAIL:input=%s:expected=%s:got=%s\n",
                   tc, s,
                   expected ? "true" : "false",
                   got ? "true" : "false");
        }
    }
}

int main() {
    // 5 Visible
    test("()", true, 1, 0);
    test("()[]{}", true, 2, 0);
    test("(]", false, 3, 0);
    test("([)]", false, 4, 0);
    test("{[]}", true, 5, 0);

    // 5 Hidden
    test("", true, 6, 1);
    test("[", false, 7, 1);
    test("(((((((()", false, 8, 1);
    test("((()))", true, 9, 1);
    test("(){[()]}[{}]", true, 10, 1);

    return 0;
}'''

# ── Insert all 5 snippets ──
for lang, code in [
    ("JAVA",       java_code),
    ("CPP",        cpp_code),
    ("PYTHON",     py_code),
    ("JAVASCRIPT", js_code),
    ("C",          c_code),
]:
    cur.execute(
        "INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) "
        "VALUES (%s, %s, %s, NOW(), NOW())",
        (pid, lang, code)
    )
conn.commit()

# ── Verify ──
cur.execute(
    "SELECT language, LENGTH(solution_template) FROM code_snippets WHERE problem_id = %s ORDER BY language",
    (pid,)
)
for lang, size in cur.fetchall():
    print(f"  {lang}: {size} bytes")

print(f"\nValid Parentheses (pid={pid}) — done!")
cur.close()
conn.close()
