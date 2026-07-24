"""
Middle of the Linked List
==========================
Given the head of a singly linked list, return the middle node.
If there are two middle nodes (even length), return the second middle.

Examples:
  1 → 2 → 3 → 4 → 5      → middle = 3
  1 → 2 → 3 → 4 → 5 → 6  → middle = 4 (second middle)

Approach: Slow and fast pointer. Slow moves 1 step, fast moves 2 steps.
When fast reaches end, slow is at middle.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Middle of the Linked List"
desc=(
    "Given the head of a singly linked list, return the middle node.\n\n"
    "If there are two middle nodes (when the list has even length), "
    "return the second middle node.\n\n"
    "For example:\n"
    "1 → 2 → 3 → 4 → 5 → null, middle = node with value 3\n"
    "1 → 2 → 3 → 4 → 5 → 6 → null, middle = node with value 4\n\n"
    "Use the fast-slow pointer technique. Move slow by 1 step and fast by 2 steps. "
    "When fast reaches the end (or the last node), slow is at the middle."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the value of the middle node."
cons="1 ≤ n ≤ 100\n-100 ≤ Node.val ≤ 100"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n3"
e2="Input:\n6\n1 2 3 4 5 6\n\nOutput:\n4"
e3="Input:\n1\n10\n\nOutput:\n10"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }

// USER_CODE_START
// class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
class CodeCoder {
    public ListNode middleNode(ListNode head) {
        // Write your code here — slow and fast pointer
        return head;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a) {
        if (a.length == 0) return null;
        ListNode dummy = new ListNode(0), cur = dummy;
        for (int v : a) { cur.next = new ListNode(v); cur = cur.next; }
        return dummy.next;
    }
    static void test(int[] input, int expected, int tc, boolean hidden) {
        ListNode mid = new CodeCoder().middleNode(build(input));
        int got = mid != null ? mid.val : -999;
        if (got == expected)
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(input) + ":expected=" + expected + ":got=" + got);
    }
    public static void main(String[] a) {
        try { test(new int[]{1,2,3,4,5}, 3, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6}, 4, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{10}, 10, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{1,2}, 2, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{5,4,3,2,1}, 3, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6,7}, 4, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[]{-1,-2,-3}, -2, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[]{100,200,300,400}, 300, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[]{0,0,0,0,0}, 0, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6,7,8,9}, 5, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };

// USER_CODE_START
// struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };
class CodeCoder {
public:
    ListNode* middleNode(ListNode* head) {
        return head;
    }
};
// USER_CODE_END

ListNode* build(vector<int>& a) {
    ListNode dummy(0), *cur = &dummy;
    for (int v : a) { cur->next = new ListNode(v); cur = cur->next; }
    return dummy.next;
}
void test(vector<int> in, int exp, int tc, bool h = false) {
    auto m = CodeCoder().middleNode(build(in));
    int g = m ? m->val : -999;
    if (g == exp) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:expected=" << exp << ":got=" << g << "\\n";
}
int main() {
    try { test({1,2,3,4,5}, 3, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5,6}, 4, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({10}, 10, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({1,2}, 2, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({5,4,3,2,1}, 3, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5,6,7}, 4, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({-1,-2,-3}, -2, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({100,200,300,400}, 300, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({0,0,0,0,0}, 0, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5,6,7,8,9}, 5, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def middleNode(self, head):
        return head
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    dummy = ListNode(0); cur = dummy
    for v in a: cur.next = ListNode(v); cur = cur.next
    return dummy.next

def test(inp, exp, tc, hidden=False):
    m = CodeCoder().middleNode(build(inp))
    g = m.val if m else -999
    if g == exp: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:inp={inp}:exp={exp}:got={g}")

try: test([1,2,3,4,5], 3, 1)
except: print("TC:1:FAIL:hidden")
try: test([1,2,3,4,5,6], 4, 2)
except: print("TC:2:FAIL:hidden")
try: test([10], 10, 3)
except: print("TC:3:FAIL:hidden")
try: test([1,2], 2, 4)
except: print("TC:4:FAIL:hidden")
try: test([5,4,3,2,1], 3, 5)
except: print("TC:5:FAIL:hidden")
try: test([1,2,3,4,5,6,7], 4, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([-1,-2,-3], -2, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([100,200,300,400], 300, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([0,0,0,0,0], 0, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([1,2,3,4,5,6,7,8,9], 5, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function middleNode(head) {
    return head;
}
// USER_CODE_END

class ListNode {
    constructor(val, next) { this.val = val; this.next = next || null; }
}
function build(a) {
    let dummy = new ListNode(0), cur = dummy;
    for (let v of a) { cur.next = new ListNode(v); cur = cur.next; }
    return dummy.next;
}
function test(inp, exp, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const m = middleNode(build(inp));
    const g = m ? m.val : -999;
    if (g === exp) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:inp=" + JSON.stringify(inp) + ":exp=" + exp + ":got=" + g);
}
try { test([1,2,3,4,5], 3, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([1,2,3,4,5,6], 4, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([10], 10, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([1,2], 2, 4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([5,4,3,2,1], 3, 5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([1,2,3,4,5,6,7], 4, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test([-1,-2,-3], -2, 7, true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test([100,200,300,400], 300, 8, true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test([0,0,0,0,0], 0, 9, true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test([1,2,3,4,5,6,7,8,9], 5, 10, true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct ListNode { int val; struct ListNode* next; };

// USER_CODE_START
struct ListNode* middleNode(struct ListNode* head) {
    return head;
}
// USER_CODE_END

struct ListNode* build(int* a, int n) {
    if (n == 0) return NULL;
    struct ListNode* head = malloc(sizeof(struct ListNode));
    head->val = a[0]; head->next = NULL;
    struct ListNode* cur = head;
    for (int i = 1; i < n; i++) {
        cur->next = malloc(sizeof(struct ListNode));
        cur = cur->next; cur->val = a[i]; cur->next = NULL;
    }
    return head;
}
void runTest(int* in, int n, int exp, int tc, int hidden) {
    struct ListNode* h = build(in, n);
    struct ListNode* m = middleNode(h);
    int g = m ? m->val : -999;
    if (g == exp) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:expected=%d:got=%d\\n", tc, exp, g);
    }
}
int main() {
    int t1[] = {1,2,3,4,5}; runTest(t1,5,3,1,0);
    int t2[] = {1,2,3,4,5,6}; runTest(t2,6,4,2,0);
    int t3[] = {10}; runTest(t3,1,10,3,0);
    int t4[] = {1,2}; runTest(t4,2,2,4,0);
    int t5[] = {5,4,3,2,1}; runTest(t5,5,3,5,0);
    int t6[] = {1,2,3,4,5,6,7}; runTest(t6,7,4,6,1);
    int t7[] = {-1,-2,-3}; runTest(t7,3,-2,7,1);
    int t8[] = {100,200,300,400}; runTest(t8,4,300,8,1);
    int t9[] = {0,0,0,0,0}; runTest(t9,5,0,9,1);
    int t10[] = {1,2,3,4,5,6,7,8,9}; runTest(t10,9,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
