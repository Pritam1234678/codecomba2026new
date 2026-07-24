"""
Evaluate Reverse Polish Notation
=================================
Evaluate the value of an arithmetic expression in Reverse Polish Notation (RPN).

Valid operators are +, -, *, and /. Each operand may be an integer or another expression.
Division between two integers should truncate toward zero (floor division for positive,
ceiling for negative — i.e., integer division in C/Java style).
RPN, also called postfix notation, puts the operator AFTER its operands.
Example: "3 4 +" means 3 + 4 = 7.

10 test cases — 5 visible, 5 hidden. Class name: CodeCoder
"""
import psycopg2, json

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Evaluate Reverse Polish Notation"
desc = (
    "Evaluate the value of an arithmetic expression in Reverse Polish Notation (RPN).\n\n"
    "Valid tokens are '+', '-', '*', '/' and integers (which can be negative).\n"
    "RPN (postfix notation) puts the operator after its operands. For each operator, "
    "pop the last two operands, apply the operation, and push the result back.\n"
    "When dividing two integers, truncate toward zero (C-style integer division).\n"
    "The expression is always valid — there will always be exactly one result.\n\n"
    "Use a stack: push integers, and when you see an operator, pop two values, "
    "apply the operator, push the result. At the end, the stack has one value — the answer."
)
infmt = (
    "First line contains integer n (number of tokens).\n"
    "Next n lines each contain one token (a string — either an integer or an operator)."
)
outfmt = "Print a single integer — the result of evaluating the RPN expression."
cons = (
    "1 \u2264 n \u2264 10^4\n"
    "Tokens are either '+', '-', '*', '/' or integers in range [-200, 200].\n"
    "The expression is always valid (exactly one result)."
)
e1 = (
    "Input:\n"
    "5\n"
    "2\n"
    "1\n"
    "+\n"
    "3\n"
    "*\n\n"
    "Output:\n"
    "9\n\n"
    "Explanation: ((2 + 1) * 3) = 9"
)
e2 = (
    "Input:\n"
    "5\n"
    "4\n"
    "13\n"
    "5\n"
    "/\n"
    "+\n\n"
    "Output:\n"
    "6\n\n"
    "Explanation: (4 + (13 / 5)) = 4 + 2 = 6 (truncated toward zero)"
)
e3 = (
    "Input:\n"
    "13\n"
    "10\n"
    "6\n"
    "9\n"
    "3\n"
    "+\n"
    "-11\n"
    "*\n"
    "/\n"
    "*\n"
    "17\n"
    "+\n"
    "5\n"
    "+\n\n"
    "Output:\n"
    "22\n\n"
    "Explanation: ((10 * (6 / ((9 + 3) * -11))) + 17) + 5 = 22"
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True,
     "Stack, Math", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int evalRPN(String[] tokens) {
        // Write your code here — use a stack for evaluation
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(String[] tokens, int expected, int tc, boolean hidden) {
        int got = new CodeCoder().evalRPN(tokens);
        if (got == expected) {
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        } else if (hidden) {
            System.out.println("TC:" + tc + ":FAIL:hidden");
        } else {
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(tokens)
                + ":expected=" + expected + ":got=" + got);
        }
    }

    public static void main(String[] args) {
        try { test(new String[]{"2","1","+","3","*"}, 9, 1, false); }
        catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }

        try { test(new String[]{"4","13","5","/","+"}, 6, 2, false); }
        catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }

        try { test(new String[]{"10","6","9","3","+","-11","*","/","*","17","+","5","+"}, 22, 3, false); }
        catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }

        try { test(new String[]{"2","3","+"}, 5, 4, false); }
        catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }

        try { test(new String[]{"18"}, 18, 5, false); }
        catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }

        try { test(new String[]{"3","-4","+"}, -1, 6, true); }
        catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }

        try { test(new String[]{"-2","-3","-"}, 1, 7, true); }
        catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }

        try { test(new String[]{"5","-2","*"}, -10, 8, true); }
        catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }

        try { test(new String[]{"-12","-3","/"}, 4, 9, true); }
        catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }

        try { test(new String[]{"3","11","5","+","-"}, -13, 10, true); }
        catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    int evalRPN(vector<string>& tokens) {
        // Write your code here — use a stack for evaluation
        return 0;
    }
};
// USER_CODE_END

void test(vector<string> tokens, int expected, int tc, bool hidden = false) {
    int got = CodeCoder().evalRPN(tokens);
    if (got == expected) {
        cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
    } else if (hidden) {
        cout << "TC:" << tc << ":FAIL:hidden\n";
    } else {
        cout << "TC:" << tc << ":FAIL:got=" << got << ":expected=" << expected << "\n";
    }
}

int main() {
    try { test({"2","1","+","3","*"}, 9, 1); } catch (...) { cout << "TC:1:FAIL:hidden\n"; }
    try { test({"4","13","5","/","+"}, 6, 2); } catch (...) { cout << "TC:2:FAIL:hidden\n"; }
    try { test({"10","6","9","3","+","-11","*","/","*","17","+","5","+"}, 22, 3); } catch (...) { cout << "TC:3:FAIL:hidden\n"; }
    try { test({"2","3","+"}, 5, 4); } catch (...) { cout << "TC:4:FAIL:hidden\n"; }
    try { test({"18"}, 18, 5); } catch (...) { cout << "TC:5:FAIL:hidden\n"; }
    try { test({"3","-4","+"}, -1, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\n"; }
    try { test({"-2","-3","-"}, 1, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\n"; }
    try { test({"5","-2","*"}, -10, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\n"; }
    try { test({"-12","-3","/"}, 4, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\n"; }
    try { test({"3","11","5","+","-"}, -13, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

py_code = r'''# USER_CODE_START
class CodeCoder:
    def evalRPN(self, tokens):
        # Write your code here — use a stack for evaluation
        return 0
# USER_CODE_END

def test(tokens, expected, tc, hidden=False):
    got = CodeCoder().evalRPN(tokens)
    if got == expected:
        print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden:
        print(f"TC:{tc}:FAIL:hidden")
    else:
        print(f"TC:{tc}:FAIL:tokens={tokens}:expected={expected}:got={got}")

try: test(["2","1","+","3","*"], 9, 1)
except: print("TC:1:FAIL:hidden")
try: test(["4","13","5","/","+"], 6, 2)
except: print("TC:2:FAIL:hidden")
try: test(["10","6","9","3","+","-11","*","/","*","17","+","5","+"], 22, 3)
except: print("TC:3:FAIL:hidden")
try: test(["2","3","+"], 5, 4)
except: print("TC:4:FAIL:hidden")
try: test(["18"], 18, 5)
except: print("TC:5:FAIL:hidden")
try: test(["3","-4","+"], -1, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test(["-2","-3","-"], 1, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test(["5","-2","*"], -10, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test(["-12","-3","/"], 4, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test(["3","11","5","+","-"], -13, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code = r'''// USER_CODE_START
function evalRPN(tokens) {
    // Write your code here — use a stack for evaluation
    return 0;
}
// USER_CODE_END

function test(tokens, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const got = evalRPN(tokens);
    if (got === expected) {
        console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    } else if (hidden) {
        console.log("TC:" + tc + ":FAIL:hidden");
    } else {
        console.log("TC:" + tc + ":FAIL:tokens=" + JSON.stringify(tokens)
            + ":expected=" + expected + ":got=" + got);
    }
}

try { test(["2","1","+","3","*"], 9, 1); } catch (e) { console.log("TC:1:FAIL:hidden"); }
try { test(["4","13","5","/","+"], 6, 2); } catch (e) { console.log("TC:2:FAIL:hidden"); }
try { test(["10","6","9","3","+","-11","*","/","*","17","+","5","+"], 22, 3); } catch (e) { console.log("TC:3:FAIL:hidden"); }
try { test(["2","3","+"], 5, 4); } catch (e) { console.log("TC:4:FAIL:hidden"); }
try { test(["18"], 18, 5); } catch (e) { console.log("TC:5:FAIL:hidden"); }
try { test(["3","-4","+"], -1, 6, true); } catch (e) { console.log("TC:6:FAIL:hidden"); }
try { test(["-2","-3","-"], 1, 7, true); } catch (e) { console.log("TC:7:FAIL:hidden"); }
try { test(["5","-2","*"], -10, 8, true); } catch (e) { console.log("TC:8:FAIL:hidden"); }
try { test(["-12","-3","/"], 4, 9, true); } catch (e) { console.log("TC:9:FAIL:hidden"); }
try { test(["3","11","5","+","-"], -13, 10, true); } catch (e) { console.log("TC:10:FAIL:hidden"); }'''

c_code = r'''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
// Evaluate the RPN expression. Returns the final integer result.
int evalRPN(char** tokens, int tokensSize) {
    // Write your code here — use a stack for evaluation
    return 0;
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

int arrEq(int* a, int* b, int n) {
    for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0;
    return 1;
}

void runTest(char** tokens, int n, int expected, int tc, int hidden) {
    int got = evalRPN(tokens, n);
    if (got == expected) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:expected=%d:got=%d\n", tc, expected, got);
    }
}

int main() {
    char* t1[] = {"2","1","+","3","*"};
    runTest(t1, 5, 9, 1, 0);

    char* t2[] = {"4","13","5","/","+"};
    runTest(t2, 5, 6, 2, 0);

    char* t3[] = {"10","6","9","3","+","-11","*","/","*","17","+","5","+"};
    runTest(t3, 13, 22, 3, 0);

    char* t4[] = {"2","3","+"};
    runTest(t4, 3, 5, 4, 0);

    char* t5[] = {"18"};
    runTest(t5, 1, 18, 5, 0);

    char* t6[] = {"3","-4","+"};
    runTest(t6, 3, -1, 6, 1);

    char* t7[] = {"-2","-3","-"};
    runTest(t7, 3, 1, 7, 1);

    char* t8[] = {"5","-2","*"};
    runTest(t8, 3, -10, 8, 1);

    char* t9[] = {"-12","-3","/"};
    runTest(t9, 3, 4, 9, 1);

    char* t10[] = {"3","11","5","+","-"};
    runTest(t10, 5, -13, 10, 1);

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

print(f"\nEvaluate Reverse Polish Notation (pid={pid}) — done!")
cur.close()
conn.close()
