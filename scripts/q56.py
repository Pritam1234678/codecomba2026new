"""
Linked List Cycle
==================
Given head of a linked list, determine if the list has a cycle in it.
A cycle exists if a node can be reached again by following next pointers.

Return true if there is a cycle, false otherwise.

Approach: Floyd's cycle detection (slow & fast pointer).
Slow moves 1 step, fast moves 2 steps. If they meet, there's a cycle.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Linked List Cycle"
desc=(
    "Given head of a linked list, determine if the list has a cycle in it.\n\n"
    "A cycle exists if a node's next pointer points to a previously visited node, "
    "creating an infinite loop. Return true if there is a cycle, false otherwise.\n\n"
    "Use Floyd's Tortoise and Hare algorithm:\n"
    "- Two pointers: slow (moves 1 step) and fast (moves 2 steps)\n"
    "- If they meet, a cycle exists\n"
    "- If fast reaches null, no cycle\n\n"
    "The input uses pos to indicate where the tail connects (-1 means no cycle)."
)
infmt="First line contains n.\nSecond line contains n space-separated integers (values).\nThird line contains integer pos (0-indexed position where tail connects, -1 for no cycle)."
outfmt="Print 'true' if the list has a cycle, otherwise 'false'."
cons="0 ≤ n ≤ 10^4\n-10^5 ≤ Node.val ≤ 10^5\n-1 ≤ pos < n"
e1="Input:\n4\n3 2 0 -4\n1\n\nOutput:\ntrue\n\nExplanation: Tail connects to index 1 (node with value 2)."
e2="Input:\n2\n1 2\n0\n\nOutput:\ntrue\n\nExplanation: Tail connects to index 0 (node with value 1)."
e3="Input:\n1\n1\n-1\n\nOutput:\nfalse\n\nExplanation: No cycle."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }

// USER_CODE_START
// class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
class CodeCoder {
    public boolean hasCycle(ListNode head) {
        // Write your code here — Floyd's cycle detection
        return false;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a, int pos) {
        if (a.length == 0) return null;
        ListNode dummy = new ListNode(0), cur = dummy;
        ListNode cycleNode = null;
        for (int i = 0; i < a.length; i++) {
            cur.next = new ListNode(a[i]);
            cur = cur.next;
            if (i == pos) cycleNode = cur;
        }
        if (pos >= 0) cur.next = cycleNode;
        return dummy.next;
    }
    static void test(int[] a, int pos, boolean exp, int tc, boolean h) {
        boolean g = new CodeCoder().hasCycle(build(a, pos));
        if (g == exp) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(a) + ":pos=" + pos + ":expected=" + exp + ":got=" + g);
    }
    public static void main(String[] a) {
        try { test(new int[]{3,2,0,-4}, 1, true, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{1,2}, 0, true, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{1}, -1, false, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{}, -1, false, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5}, -1, false, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5}, 4, true, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[]{-5,-4,-3}, 0, true, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[]{10,20}, 1, true, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[]{0}, 0, true, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[]{1,1,1,1}, 2, true, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };

// USER_CODE_START
// struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };
class CodeCoder {
public:
    bool hasCycle(ListNode* head) {
        return false;
    }
};
// USER_CODE_END

ListNode* build(vector<int>& a, int pos) {
    if (a.empty()) return NULL;
    ListNode dummy(0), *cur = &dummy;
    ListNode* cycleNode = NULL;
    for (int i = 0; i < (int)a.size(); i++) {
        cur->next = new ListNode(a[i]);
        cur = cur->next;
        if (i == pos) cycleNode = cur;
    }
    if (pos >= 0) cur->next = cycleNode;
    return dummy.next;
}
void test(vector<int> a, int pos, bool exp, int tc, bool h = false) {
    bool g = CodeCoder().hasCycle(build(a, pos));
    if (g == exp) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:expected=" << (exp?"true":"false") << ":got=" << (g?"true":"false") << "\\n";
}
int main() {
    try { test({3,2,0,-4}, 1, true, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1,2}, 0, true, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({1}, -1, false, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({}, -1, false, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5}, -1, false, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5}, 4, true, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({-5,-4,-3}, 0, true, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({10,20}, 1, true, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({0}, 0, true, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({1,1,1,1}, 2, true, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def hasCycle(self, head):
        return False
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a, pos):
    if not a: return None
    dummy = ListNode(0); cur = dummy; cycleNode = None
    for i, v in enumerate(a):
        cur.next = ListNode(v); cur = cur.next
        if i == pos: cycleNode = cur
    if pos >= 0: cur.next = cycleNode
    return dummy.next

def test(a, pos, exp, tc, hidden=False):
    g = CodeCoder().hasCycle(build(a, pos))
    if g == exp: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:Exp={exp}:Got={g}")

try: test([3,2,0,-4], 1, True, 1)
except: print("TC:1:FAIL:hidden")
try: test([1,2], 0, True, 2)
except: print("TC:2:FAIL:hidden")
try: test([1], -1, False, 3)
except: print("TC:3:FAIL:hidden")
try: test([], -1, False, 4)
except: print("TC:4:FAIL:hidden")
try: test([1,2,3,4,5], -1, False, 5)
except: print("TC:5:FAIL:hidden")
try: test([1,2,3,4,5], 4, True, 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([-5,-4,-3], 0, True, 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([10,20], 1, True, 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([0], 0, True, 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([1,1,1,1], 2, True, 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function hasCycle(head) { return false; }
// USER_CODE_END

class ListNode {
    constructor(val, next) { this.val = val; this.next = next || null; }
}
function build(a, pos) {
    if (!a.length) return null;
    let dummy = new ListNode(0), cur = dummy, cycleNode = null;
    for (let i = 0; i < a.length; i++) {
        cur.next = new ListNode(a[i]); cur = cur.next;
        if (i === pos) cycleNode = cur;
    }
    if (pos >= 0) cur.next = cycleNode;
    return dummy.next;
}
function test(a, pos, exp, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = hasCycle(build(a, pos));
    if (g === exp) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:exp=" + exp + ":got=" + g);
}
try { test([3,2,0,-4], 1, true, 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([1,2], 0, true, 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([1], -1, false, 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([], -1, false, 4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([1,2,3,4,5], -1, false, 5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([1,2,3,4,5], 4, true, 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test([-5,-4,-3], 0, true, 7, true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test([10,20], 1, true, 8, true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test([0], 0, true, 9, true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test([1,1,1,1], 2, true, 10, true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

struct ListNode { int val; struct ListNode* next; };

// USER_CODE_START
bool hasCycle(struct ListNode* head) { return false; }
// USER_CODE_END

struct ListNode* build(int* a, int n, int pos) {
    if (n == 0) return NULL;
    struct ListNode* head = malloc(sizeof(struct ListNode));
    head->val = a[0]; head->next = NULL;
    struct ListNode* cur = head, *cycleNode = NULL;
    if (pos == 0) cycleNode = head;
    for (int i = 1; i < n; i++) {
        cur->next = malloc(sizeof(struct ListNode));
        cur = cur->next; cur->val = a[i]; cur->next = NULL;
        if (i == pos) cycleNode = cur;
    }
    if (pos >= 0) cur->next = cycleNode;
    return head;
}
void runTest(int* a, int n, int pos, bool exp, int tc, int hidden) {
    bool g = hasCycle(build(a, n, pos));
    if (g == exp) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:exp=%s:got=%s\\n", tc, exp?"true":"false", g?"true":"false");
    }
}
int main() {
    int t1[]={3,2,0,-4}; runTest(t1,4,1,true,1,0);
    int t2[]={1,2}; runTest(t2,2,0,true,2,0);
    int t3[]={1}; runTest(t3,1,-1,false,3,0);
    int t4[]={}; runTest(t4,0,-1,false,4,0);
    int t5[]={1,2,3,4,5}; runTest(t5,5,-1,false,5,0);
    int t6[]={1,2,3,4,5}; runTest(t6,5,4,true,6,1);
    int t7[]={-5,-4,-3}; runTest(t7,3,0,true,7,1);
    int t8[]={10,20}; runTest(t8,2,1,true,8,1);
    int t9[]={0}; runTest(t9,1,0,true,9,1);
    int t10[]={1,1,1,1}; runTest(t10,4,2,true,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
