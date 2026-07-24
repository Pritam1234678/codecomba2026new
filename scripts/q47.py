"""
Min Stack
=========
Design a stack that supports push, pop, top, and retrieving the minimum element
in constant time O(1).

Implement the CodeCoder class:
- CodeCoder()          — initializes the stack object
- push(int val)        — pushes val onto the stack
- pop()                — removes the element on top
- top()                — returns the top element
- getMin()             — retrieves the minimum element in the stack

Common approach: Use TWO stacks — one for actual elements, one for tracking min
at each level. When pushing, compare val with current min and push the smaller.
"""
import psycopg2
import json

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                        user="postgres", password="postgres")
cur = conn.cursor()

title = "Min Stack"
desc = (
    "Design a stack that supports push, pop, top, and retrieving the "
    "minimum element in constant time.\n\n"
    "Implement the CodeCoder class:\n"
    "- CodeCoder() — initializes the stack object\n"
    "- push(int val) — pushes the element val onto the stack\n"
    "- pop() — removes the element on the top of the stack\n"
    "- top() — returns the top element of the stack\n"
    "- getMin() — retrieves the minimum element in the stack\n\n"
    "You must implement all operations in O(1) time complexity. "
    "A common trick is to use two stacks: one for the actual stack and "
    "another to track the minimum element at each level."
)
infmt = (
    "First line contains integer q (number of operations).\n"
    "Next q lines each contain an operation:\n"
    "- 'push x' — push integer x onto the stack\n"
    "- 'pop' — remove the top element\n"
    "- 'top' — print the top element\n"
    "- 'getMin' — print the current minimum"
)
outfmt = "For each 'top' and 'getMin' operation, print the result on a new line."
cons = (
    "1 \u2264 q \u2264 3 \u00d7 10^4\n"
    "-2\u00b9\u00b9 \u2264 val \u2264 2\u00b9\u00b9-1\n"
    "Operations pop, top and getMin will always be called on non-empty stacks."
)
e1 = (
    "Input:\n"
    "8\n"
    "push -2\n"
    "push 0\n"
    "push -3\n"
    "getMin\n"
    "pop\n"
    "top\n"
    "getMin\n\n"
    "Output:\n"
    "-3\n0\n-2\n\n"
    "Explanation:\n"
    "After pushes: stack = [-2,0,-3], min = -3\n"
    "getMin() → -3\n"
    "pop() → removes -3, stack = [-2,0]\n"
    "top() → 0\n"
    "getMin() → -2"
)
e2 = (
    "Input:\n"
    "5\n"
    "push 2\n"
    "push 0\n"
    "push 3\n"
    "getMin\n"
    "pop\n"
    "getMin\n\n"
    "Output:\n"
    "0\n2\n\n"
    "Explanation: Stack = [2,0,3], min=0. Pop 3, stack=[2,0], min=0. Pop 0, stack=[2], min=2."
)
e3 = (
    "Input:\n"
    "3\n"
    "push 5\n"
    "top\n"
    "getMin\n\n"
    "Output:\n"
    "5\n5"
)

cur.execute("""
    INSERT INTO problems (title, description, input_format, output_format, constraints,
        time_limit, memory_limit, level, active, topics, example1, example2, example3)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id
""", (title, desc, infmt, outfmt, cons, 5.0, 256, "MEDIUM", True,
     "Stack, Design", e1, e2, e3))
pid = cur.fetchone()[0]
print(f"Problem inserted: {title} (pid={pid})")

# ══════════════════════════════════════════════════════════════
# JAVA
# ══════════════════════════════════════════════════════════════
java_code = r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public CodeCoder() {
        // initialize your stack(s)
    }

    public void push(int val) {
        // Write your code here — O(1)
    }

    public void pop() {
        // Write your code here — O(1)
    }

    public int top() {
        // Write your code here — O(1)
        return 0;
    }

    public int getMin() {
        // Write your code here — O(1)
        return 0;
    }
}
// USER_CODE_END

public class Main {
    // Each test runs a sequence of operations and verifies outputs
    static void runTest(String[] ops, int[][] vals, String[] expected, int tc, boolean hidden) {
        try {
            CodeCoder st = new CodeCoder();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ops.length; i++) {
                switch (ops[i]) {
                    case "push": st.push(vals[i][0]); break;
                    case "pop": st.pop(); break;
                    case "top": sb.append(st.top()).append(","); break;
                    case "getMin": sb.append(st.getMin()).append(","); break;
                }
            }
            String got = sb.toString();
            String exp = String.join(",", expected) + ",";
            if (got.equals(exp))
                System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
            else if (hidden)
                System.out.println("TC:" + tc + ":FAIL:hidden");
            else
                System.out.println("TC:" + tc + ":FAIL:got=" + got + ":expected=" + exp);
        } catch (Exception e) {
            if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
            else System.out.println("TC:" + tc + ":FAIL:got=ERR");
        }
    }

    public static void main(String[] args) {
        // TC1: Basic push, getMin, pop, top, getMin
        runTest(
            new String[]{"push","push","push","getMin","pop","top","getMin"},
            new int[][]{{-2},{0},{-3},{0},{0},{0},{0}},
            new String[]{"-3","0","-2"},
            1, false
        );

        // TC2: Multiple pushes, mins changing after pops
        runTest(
            new String[]{"push","push","push","getMin","pop","getMin"},
            new int[][]{{2},{0},{3},{0},{0},{0}},
            new String[]{"0","2"},
            2, false
        );

        // TC3: Single element
        runTest(
            new String[]{"push","top","getMin"},
            new int[][]{{5},{0},{0}},
            new String[]{"5","5"},
            3, false
        );

        // TC4: Descending values
        runTest(
            new String[]{"push","push","push","getMin","pop","getMin","pop","getMin"},
            new int[][]{{3},{2},{1},{0},{0},{0},{0},{0}},
            new String[]{"1","2","3"},
            4, false
        );

        // TC5: Same values
        runTest(
            new String[]{"push","push","push","getMin","pop","getMin","pop","getMin"},
            new int[][]{{5},{5},{5},{0},{0},{0},{0},{0}},
            new String[]{"5","5","5"},
            5, false
        );

        // Hidden TC6: Large push then getMin
        runTest(
            new String[]{"push","push","getMin"},
            new int[][]{{1000000000},{-1000000000},{0}},
            new String[]{"-1000000000"},
            6, true
        );

        // TC7: Pop until empty then one element
        runTest(
            new String[]{"push","push","pop","pop","push","getMin"},
            new int[][]{{10},{20},{0},{0},{5},{0}},
            new String[]{"5"},
            7, true
        );

        // TC8: Ascending values
        runTest(
            new String[]{"push","push","push","getMin"},
            new int[][]{{1},{2},{3},{0}},
            new String[]{"1"},
            8, true
        );

        // TC9: Pop middle, min unchanged
        runTest(
            new String[]{"push","push","push","pop","getMin"},
            new int[][]{{1},{0},{2},{0},{0}},
            new String[]{"0"},
            9, true
        );

        // TC10: Many operations
        runTest(
            new String[]{"push","push","getMin","pop","getMin","push","getMin"},
            new int[][]{{-5},{10},{0},{0},{0},{-7},{0}},
            new String[]{"-5","-5","-7"},
            10, true
        );
    }
}'''

# ══════════════════════════════════════════════════════════════
# CPP
# ══════════════════════════════════════════════════════════════
cpp_code = r'''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    CodeCoder() {
        // initialize your stack(s)
    }

    void push(int val) {
        // Write your code here — O(1)
    }

    void pop() {
        // Write your code here — O(1)
    }

    int top() {
        // Write your code here — O(1)
        return 0;
    }

    int getMin() {
        // Write your code here — O(1)
        return 0;
    }
};
// USER_CODE_END

void runTest(vector<string> ops, vector<vector<int>> vals,
             vector<string> expected, int tc, bool hidden = false) {
    try {
        CodeCoder st;
        string got;
        for (size_t i = 0; i < ops.size(); i++) {
            if (ops[i] == "push") st.push(vals[i][0]);
            else if (ops[i] == "pop") st.pop();
            else if (ops[i] == "top") got += to_string(st.top()) + ",";
            else if (ops[i] == "getMin") got += to_string(st.getMin()) + ",";
        }
        string exp;
        for (auto& s : expected) exp += s + ",";
        if (got == exp)
            cout << "TC:" << tc << ":PASS" << (hidden ? ":hidden" : "") << "\n";
        else if (hidden)
            cout << "TC:" << tc << ":FAIL:hidden\n";
        else
            cout << "TC:" << tc << ":FAIL:got=" << got << ":expected=" << exp << "\n";
    } catch (...) {
        if (hidden) cout << "TC:" << tc << ":FAIL:hidden\n";
        else cout << "TC:" << tc << ":FAIL:hidden\n";
    }
}

int main() {
    try { runTest({"push","push","push","getMin","pop","top","getMin"}, {{-2},{0},{-3},{0},{0},{0},{0}}, {"-3","0","-2"}, 1); }
    catch (...) { cout << "TC:1:FAIL:hidden\n"; }

    try { runTest({"push","push","push","getMin","pop","getMin"}, {{2},{0},{3},{0},{0},{0}}, {"0","2"}, 2); }
    catch (...) { cout << "TC:2:FAIL:hidden\n"; }

    try { runTest({"push","top","getMin"}, {{5},{0},{0}}, {"5","5"}, 3); }
    catch (...) { cout << "TC:3:FAIL:hidden\n"; }

    try { runTest({"push","push","push","getMin","pop","getMin","pop","getMin"}, {{3},{2},{1},{0},{0},{0},{0},{0}}, {"1","2","3"}, 4); }
    catch (...) { cout << "TC:4:FAIL:hidden\n"; }

    try { runTest({"push","push","push","getMin","pop","getMin","pop","getMin"}, {{5},{5},{5},{0},{0},{0},{0},{0}}, {"5","5","5"}, 5); }
    catch (...) { cout << "TC:5:FAIL:hidden\n"; }

    try { runTest({"push","push","getMin"}, {{1000000000},{-1000000000},{0}}, {"-1000000000"}, 6, true); }
    catch (...) { cout << "TC:6:FAIL:hidden\n"; }

    try { runTest({"push","push","pop","pop","push","getMin"}, {{10},{20},{0},{0},{5},{0}}, {"5"}, 7, true); }
    catch (...) { cout << "TC:7:FAIL:hidden\n"; }

    try { runTest({"push","push","push","getMin"}, {{1},{2},{3},{0}}, {"1"}, 8, true); }
    catch (...) { cout << "TC:8:FAIL:hidden\n"; }

    try { runTest({"push","push","push","pop","getMin"}, {{1},{0},{2},{0},{0}}, {"0"}, 9, true); }
    catch (...) { cout << "TC:9:FAIL:hidden\n"; }

    try { runTest({"push","push","getMin","pop","getMin","push","getMin"}, {{-5},{10},{0},{0},{0},{-7},{0}}, {"-5","-5","-7"}, 10, true); }
    catch (...) { cout << "TC:10:FAIL:hidden\n"; }
    return 0;
}'''

# ══════════════════════════════════════════════════════════════
# PYTHON
# ══════════════════════════════════════════════════════════════
py_code = r'''# USER_CODE_START
class CodeCoder:
    def __init__(self):
        # initialize your stack(s)
        pass

    def push(self, val):
        # Write your code here — O(1)
        pass

    def pop(self):
        # Write your code here — O(1)
        pass

    def top(self):
        # Write your code here — O(1)
        return 0

    def getMin(self):
        # Write your code here — O(1)
        return 0
# USER_CODE_END

def runTest(ops, vals, expected, tc, hidden=False):
    try:
        st = CodeCoder()
        got = []
        for op, v in zip(ops, vals):
            if op == "push": st.push(v[0])
            elif op == "pop": st.pop()
            elif op == "top": got.append(str(st.top()))
            elif op == "getMin": got.append(str(st.getMin()))
        got_str = ",".join(got) + ","
        exp_str = ",".join(expected) + ","
        if got_str == exp_str:
            print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
        elif hidden:
            print(f"TC:{tc}:FAIL:hidden")
        else:
            print(f"TC:{tc}:FAIL:got={got_str}:expected={exp_str}")
    except:
        if hidden: print(f"TC:{tc}:FAIL:hidden")
        else: print(f"TC:{tc}:FAIL:hidden")

try: runTest(["push","push","push","getMin","pop","top","getMin"], [[-2],[0],[-3],[0],[0],[0],[0]], ["-3","0","-2"], 1)
except: print("TC:1:FAIL:hidden")
try: runTest(["push","push","push","getMin","pop","getMin"], [[2],[0],[3],[0],[0],[0]], ["0","2"], 2)
except: print("TC:2:FAIL:hidden")
try: runTest(["push","top","getMin"], [[5],[0],[0]], ["5","5"], 3)
except: print("TC:3:FAIL:hidden")
try: runTest(["push","push","push","getMin","pop","getMin","pop","getMin"], [[3],[2],[1],[0],[0],[0],[0],[0]], ["1","2","3"], 4)
except: print("TC:4:FAIL:hidden")
try: runTest(["push","push","push","getMin","pop","getMin","pop","getMin"], [[5],[5],[5],[0],[0],[0],[0],[0]], ["5","5","5"], 5)
except: print("TC:5:FAIL:hidden")
try: runTest(["push","push","getMin"], [[1000000000],[-1000000000],[0]], ["-1000000000"], 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: runTest(["push","push","pop","pop","push","getMin"], [[10],[20],[0],[0],[5],[0]], ["5"], 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: runTest(["push","push","push","getMin"], [[1],[2],[3],[0]], ["1"], 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: runTest(["push","push","push","pop","getMin"], [[1],[0],[2],[0],[0]], ["0"], 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: runTest(["push","push","getMin","pop","getMin","push","getMin"], [[-5],[10],[0],[0],[0],[-7],[0]], ["-5","-5","-7"], 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

# ══════════════════════════════════════════════════════════════
# JAVASCRIPT
# ══════════════════════════════════════════════════════════════
js_code = r'''// USER_CODE_START
class CodeCoder {
    constructor() {
        // initialize your stack(s)
    }
    push(val) {
        // Write your code here — O(1)
    }
    pop() {
        // Write your code here — O(1)
    }
    top() {
        // Write your code here — O(1)
        return 0;
    }
    getMin() {
        // Write your code here — O(1)
        return 0;
    }
}
// USER_CODE_END

function runTest(ops, vals, expected, tc, hidden) {
    if (hidden === undefined) hidden = false;
    try {
        const st = new CodeCoder();
        const got = [];
        for (let i = 0; i < ops.length; i++) {
            if (ops[i] === "push") st.push(vals[i][0]);
            else if (ops[i] === "pop") st.pop();
            else if (ops[i] === "top") got.push(String(st.top()));
            else if (ops[i] === "getMin") got.push(String(st.getMin()));
        }
        const gotStr = got.join(",") + ",";
        const expStr = expected.join(",") + ",";
        if (gotStr === expStr)
            console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            console.log("TC:" + tc + ":FAIL:hidden");
        else
            console.log("TC:" + tc + ":FAIL:got=" + gotStr + ":expected=" + expStr);
    } catch (e) {
        if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
        else console.log("TC:" + tc + ":FAIL:hidden");
    }
}

runTest(["push","push","push","getMin","pop","top","getMin"], [[-2],[0],[-3],[0],[0],[0],[0]], ["-3","0","-2"], 1);
runTest(["push","push","push","getMin","pop","getMin"], [[2],[0],[3],[0],[0],[0]], ["0","2"], 2);
runTest(["push","top","getMin"], [[5],[0],[0]], ["5","5"], 3);
runTest(["push","push","push","getMin","pop","getMin","pop","getMin"], [[3],[2],[1],[0],[0],[0],[0],[0]], ["1","2","3"], 4);
runTest(["push","push","push","getMin","pop","getMin","pop","getMin"], [[5],[5],[5],[0],[0],[0],[0],[0]], ["5","5","5"], 5);
runTest(["push","push","getMin"], [[1000000000],[-1000000000],[0]], ["-1000000000"], 6, true);
runTest(["push","push","pop","pop","push","getMin"], [[10],[20],[0],[0],[5],[0]], ["5"], 7, true);
runTest(["push","push","push","getMin"], [[1],[2],[3],[0]], ["1"], 8, true);
runTest(["push","push","push","pop","getMin"], [[1],[0],[2],[0],[0]], ["0"], 9, true);
runTest(["push","push","getMin","pop","getMin","push","getMin"], [[-5],[10],[0],[0],[0],[-7],[0]], ["-5","-5","-7"], 10, true);'''

# ══════════════════════════════════════════════════════════════
# C
# ══════════════════════════════════════════════════════════════
c_code = r'''#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

// USER_CODE_START
typedef struct {
    // Write your fields here
    int* data;
    int topIdx;
    int capacity;
} CodeCoder;

CodeCoder* codeCoderCreate() {
    CodeCoder* obj = (CodeCoder*)malloc(sizeof(CodeCoder));
    // Write your initialization here
    return obj;
}

void codeCoderPush(CodeCoder* obj, int val) {
    // Write your code here — O(1)
}

void codeCoderPop(CodeCoder* obj) {
    // Write your code here — O(1)
}

int codeCoderTop(CodeCoder* obj) {
    // Write your code here — O(1)
    return 0;
}

int codeCoderGetMin(CodeCoder* obj) {
    // Write your code here — O(1)
    return 0;
}

void codeCoderFree(CodeCoder* obj) {
    free(obj);
}
// USER_CODE_END

// ----- Driver (hidden from user) -----

void runTest(int* ops, int* vals, int n, int* outVals, int outN, int tc, int hidden) {
    CodeCoder* st = codeCoderCreate();
    int outIdx = 0;
    int ok = 1;
    for (int i = 0; i < n && ok; i++) {
        if (ops[i] == 0) codeCoderPush(st, vals[i]);
        else if (ops[i] == 1) codeCoderPop(st);
        else if (ops[i] == 2) {
            int t = codeCoderTop(st);
            if (outIdx >= outN || t != outVals[outIdx]) ok = 0;
            outIdx++;
        }
        else if (ops[i] == 3) {
            int m = codeCoderGetMin(st);
            if (outIdx >= outN || m != outVals[outIdx]) ok = 0;
            outIdx++;
        }
    }
    if (ok && outIdx == outN) {
        if (hidden) printf("TC:%d:PASS:hidden\n", tc);
        else printf("TC:%d:PASS\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\n", tc);
        else printf("TC:%d:FAIL:hidden\n", tc);
    }
    codeCoderFree(st);
}

// Op codes: 0=push, 1=pop, 2=top, 3=getMin
int main() {
    // TC1
    int o1[] = {0,0,0,3,1,2,3};
    int v1[] = {-2,0,-3,0,0,0,0};
    int e1[] = {-3,0,-2};
    runTest(o1, v1, 7, e1, 3, 1, 0);

    // TC2
    int o2[] = {0,0,0,3,1,3};
    int v2[] = {2,0,3,0,0,0};
    int e2[] = {0,2};
    runTest(o2, v2, 6, e2, 2, 2, 0);

    // TC3
    int o3[] = {0,2,3};
    int v3[] = {5,0,0};
    int e3[] = {5,5};
    runTest(o3, v3, 3, e3, 2, 3, 0);

    // TC4
    int o4[] = {0,0,0,3,1,3,1,3};
    int v4[] = {3,2,1,0,0,0,0,0};
    int e4[] = {1,2,3};
    runTest(o4, v4, 8, e4, 3, 4, 0);

    // TC5
    int o5[] = {0,0,0,3,1,3,1,3};
    int v5[] = {5,5,5,0,0,0,0,0};
    int e5[] = {5,5,5};
    runTest(o5, v5, 8, e5, 3, 5, 0);

    // TC6 hidden
    int o6[] = {0,0,3};
    int v6[] = {1000000000,-1000000000,0};
    int e6[] = {-1000000000};
    runTest(o6, v6, 3, e6, 1, 6, 1);

    // TC7 hidden
    int o7[] = {0,0,1,1,0,3};
    int v7[] = {10,20,0,0,5,0};
    int e7[] = {5};
    runTest(o7, v7, 6, e7, 1, 7, 1);

    // TC8 hidden
    int o8[] = {0,0,0,3};
    int v8[] = {1,2,3,0};
    int e8[] = {1};
    runTest(o8, v8, 4, e8, 1, 8, 1);

    // TC9 hidden
    int o9[] = {0,0,0,1,3};
    int v9[] = {1,0,2,0,0};
    int e9[] = {0};
    runTest(o9, v9, 5, e9, 1, 9, 1);

    // TC10 hidden
    int o10[] = {0,0,3,1,3,0,3};
    int v10[] = {-5,10,0,0,0,-7,0};
    int e10[] = {-5,-5,-7};
    runTest(o10, v10, 7, e10, 3, 10, 1);

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

print(f"\nMin Stack (pid={pid}) — done!")
cur.close()
conn.close()
