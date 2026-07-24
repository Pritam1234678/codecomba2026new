"""
Reverse Linked List
====================
Given the head of a singly linked list, reverse the list and return its new head.

Example:
  Input:  1 → 2 → 3 → 4 → 5 → NULL
  Output: 5 → 4 → 3 → 2 → 1 → NULL

Iterative approach: use three pointers (prev, curr, next) to reverse links.
Recursive approach: reverse the rest, then point head->next->next = head.

The harness uses ListNode serialization: input is given as array [1,2,3,4,5],
the driver converts to ListNode, user reverses it, driver converts back to array.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reverse Linked List"
desc=(
    "Given the head of a singly linked list, reverse the list and return the new head.\n\n"
    "For example:\n"
    "Input: 1 → 2 → 3 → 4 → 5 → null\n"
    "Output: 5 → 4 → 3 → 2 → 1 → null\n\n"
    "You can solve this iteratively (using prev/curr/next pointers) or recursively. "
    "The solution must modify the links in-place and return the new head node."
)
infmt=(
    "First line contains integer n.\n"
    "Second line contains n space-separated integers representing the linked list."
)
outfmt="Print the reversed linked list as n space-separated integers."
cons="0 \u2264 n \u2264 5000\n-5000 \u2264 Node.val \u2264 5000"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n5 4 3 2 1"
e2="Input:\n1\n1\n\nOutput:\n1"
e3="Input:\n0\n\nOutput:\n\n(empty line for empty list)"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }

// USER_CODE_START
// class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
class CodeCoder {
    public ListNode reverseList(ListNode head) {
        // Write your code here — reverse the linked list
        return head;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a) {
        if (a == null || a.length == 0) return null;
        ListNode dummy = new ListNode(0), cur = dummy;
        for (int v : a) { cur.next = new ListNode(v); cur = cur.next; }
        return dummy.next;
    }
    static int[] ser(ListNode h) {
        List<Integer> list = new ArrayList<>();
        while (h != null) { list.add(h.val); h = h.next; }
        return list.stream().mapToInt(i->i).toArray();
    }
    static void test(int[] input, int[] expected, int tc, boolean hidden) {
        ListNode head = build(input);
        ListNode rev = new CodeCoder().reverseList(head);
        int[] got = ser(rev);
        if (Arrays.equals(got, expected))
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(input) + ":expected=" + Arrays.toString(expected) + ":got=" + Arrays.toString(got));
    }
    public static void main(String[] a) {
        try { test(new int[]{1,2,3,4,5}, new int[]{5,4,3,2,1}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{1}, new int[]{1}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{}, new int[]{}, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{1,2}, new int[]{2,1}, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{5,4,3,2,1}, new int[]{1,2,3,4,5}, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{10,20,30,40,50,60}, new int[]{60,50,40,30,20,10}, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[]{-5,-4,-3,-2,-1}, new int[]{-1,-2,-3,-4,-5}, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[]{0}, new int[]{0}, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[]{100,200,300}, new int[]{300,200,100}, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[]{1,1,1,1}, new int[]{1,1,1,1}, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };

// USER_CODE_START
// struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };
class CodeCoder {
public:
    ListNode* reverseList(ListNode* head) {
        // Write your code here
        return head;
    }
};
// USER_CODE_END

ListNode* build(vector<int>& a) {
    ListNode dummy(0), *cur = &dummy;
    for (int v : a) { cur->next = new ListNode(v); cur = cur->next; }
    return dummy.next;
}
vector<int> ser(ListNode* h) {
    vector<int> r; while (h) { r.push_back(h->val); h = h->next; } return r;
}
void test(vector<int> in, vector<int> exp, int tc, bool h = false) {
    auto g = ser(CodeCoder().reverseList(build(in)));
    if (g == exp) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else { cout << "TC:" << tc << ":FAIL:expected=["; for (int x : exp) cout << x << ","; cout << "]:got=["; for (int x : g) cout << x << ","; cout << "]\\n"; }
}
int main() {
    try { test({1,2,3,4,5}, {5,4,3,2,1}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({1}, {1}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({}, {}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({1,2}, {2,1}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({5,4,3,2,1}, {1,2,3,4,5}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({10,20,30,40,50,60}, {60,50,40,30,20,10}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({-5,-4,-3,-2,-1}, {-1,-2,-3,-4,-5}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({0}, {0}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({100,200,300}, {300,200,100}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({1,1,1,1}, {1,1,1,1}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val
#         self.next = next
class CodeCoder:
    def reverseList(self, head):
        # Write your code here
        return head
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    dummy = ListNode(0); cur = dummy
    for v in a: cur.next = ListNode(v); cur = cur.next
    return dummy.next

def ser(h):
    r = []
    while h: r.append(h.val); h = h.next
    return r

def test(inp, exp, tc, hidden=False):
    g = ser(CodeCoder().reverseList(build(inp)))
    if g == exp: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:inp={inp}:exp={exp}:got={g}")

try: test([1,2,3,4,5], [5,4,3,2,1], 1)
except: print("TC:1:FAIL:hidden")
try: test([1], [1], 2)
except: print("TC:2:FAIL:hidden")
try: test([], [], 3)
except: print("TC:3:FAIL:hidden")
try: test([1,2], [2,1], 4)
except: print("TC:4:FAIL:hidden")
try: test([5,4,3,2,1], [1,2,3,4,5], 5)
except: print("TC:5:FAIL:hidden")
try: test([10,20,30,40,50,60], [60,50,40,30,20,10], 6, hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([-5,-4,-3,-2,-1], [-1,-2,-3,-4,-5], 7, hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([0], [0], 8, hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([100,200,300], [300,200,100], 9, hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([1,1,1,1], [1,1,1,1], 10, hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function reverseList(head) {
    // Write your code here
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
function ser(h) {
    let r = [];
    while (h) { r.push(h.val); h = h.next; }
    return r;
}
function test(inp, exp, tc, hidden) {
    if (hidden === undefined) hidden = false;
    const g = ser(reverseList(build(inp)));
    const gs = JSON.stringify(g); const es = JSON.stringify(exp);
    if (gs === es) console.log("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
    else if (hidden) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:inp=" + JSON.stringify(inp) + ":exp=" + es + ":got=" + gs);
}
try { test([1,2,3,4,5], [5,4,3,2,1], 1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([1], [1], 2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([], [], 3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([1,2], [2,1], 4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([5,4,3,2,1], [1,2,3,4,5], 5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([10,20,30,40,50,60], [60,50,40,30,20,10], 6, true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test([-5,-4,-3,-2,-1], [-1,-2,-3,-4,-5], 7, true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test([0], [0], 8, true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test([100,200,300], [300,200,100], 9, true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test([1,1,1,1], [1,1,1,1], 10, true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct ListNode { int val; struct ListNode* next; };

// USER_CODE_START
struct ListNode* reverseList(struct ListNode* head) {
    // Write your code here
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
int* ser(struct ListNode* h, int* n) {
    *n = 0; struct ListNode* c = h;
    while (c) { (*n)++; c = c->next; }
    int* r = malloc(*n * sizeof(int)); c = h;
    for (int i = 0; i < *n; i++) { r[i] = c->val; c = c->next; }
    return r;
}
int arrEq(int* a, int* b, int n) { for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0; return 1; }
void runTest(int* in, int inN, int* exp, int expN, int tc, int hidden) {
    struct ListNode* h = build(in, inN);
    struct ListNode* r = reverseList(h);
    int n; int* g = ser(r, &n);
    if (n == expN && arrEq(g, exp, n)) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL\\n", tc);
    }
    free(g);
}
int main() {
    int t1[] = {1,2,3,4,5}; int e1[] = {5,4,3,2,1}; runTest(t1,5,e1,5,1,0);
    int t2[] = {1}; int e2[] = {1}; runTest(t2,1,e2,1,2,0);
    runTest(NULL,0,NULL,0,3,0);
    int t4[] = {1,2}; int e4[] = {2,1}; runTest(t4,2,e4,2,4,0);
    int t5[] = {5,4,3,2,1}; int e5[] = {1,2,3,4,5}; runTest(t5,5,e5,5,5,0);
    int t6[] = {10,20,30,40,50,60}; int e6[] = {60,50,40,30,20,10}; runTest(t6,6,e6,6,6,1);
    int t7[] = {-5,-4,-3,-2,-1}; int e7[] = {-1,-2,-3,-4,-5}; runTest(t7,5,e7,5,7,1);
    int t8[] = {0}; int e8[] = {0}; runTest(t8,1,e8,1,8,1);
    int t9[] = {100,200,300}; int e9[] = {300,200,100}; runTest(t9,3,e9,3,9,1);
    int t10[] = {1,1,1,1}; int e10[] = {1,1,1,1}; runTest(t10,4,e10,4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
