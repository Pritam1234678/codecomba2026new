"""
Merge Two Sorted Lists
========================
Merge two sorted linked lists into one sorted list. The new list should be
made by splicing together the nodes of the first two lists.

Examples:
  list1 = [1,2,4], list2 = [1,3,5] → [1,1,2,3,4,5]
  list1 = [], list2 = [0] → [0]

Approach: Use a dummy node and compare both list values one by one.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Merge Two Sorted Lists"
desc=(
    "You are given the heads of two sorted linked lists list1 and list2.\n\n"
    "Merge the two lists into one sorted list. The list should be made by "
    "splicing together the nodes of the first two lists.\n\n"
    "Return the head of the merged linked list.\n\n"
    "For example:\n"
    "list1 = [1,2,4], list2 = [1,3,4] → result = [1,1,2,3,4,4]\n"
    "list1 = [], list2 = [0] → result = [0]\n\n"
    "Use a dummy node and two-pointer traversal."
)
infmt="First line contains n and m.\nSecond line contains n space-separated integers (list1).\nThird line contains m space-separated integers (list2)."
outfmt="Print the merged sorted list as space-separated integers."
cons="0 ≤ n,m ≤ 50\n-100 ≤ Node.val ≤ 100\nBoth lists are sorted in ascending order."
e1="Input:\n3 3\n1 2 4\n1 3 4\n\nOutput:\n1 1 2 3 4 4"
e2="Input:\n0 0\n\n\n\nOutput:\n\n(empty line)"
e3="Input:\n0 1\n\n0\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }

// USER_CODE_START
// class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }
class CodeCoder {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        return list1;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a) {
        if (a.length == 0) return null;
        ListNode d = new ListNode(0), c = d;
        for (int v : a) { c.next = new ListNode(v); c = c.next; }
        return d.next;
    }
    static int[] ser(ListNode h) {
        List<Integer> l = new ArrayList<>();
        while (h != null) { l.add(h.val); h = h.next; }
        return l.stream().mapToInt(i->i).toArray();
    }
    static void test(int[] a1, int[] a2, int[] exp, int tc, boolean h) {
        int[] got = ser(new CodeCoder().mergeTwoLists(build(a1), build(a2)));
        if (Arrays.equals(got, exp)) System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) System.out.println("TC:" + tc + ":FAIL:hidden");
        else System.out.println("TC:" + tc + ":FAIL:got=" + Arrays.toString(got) + ":exp=" + Arrays.toString(exp));
    }
    public static void main(String[] a) {
        try { test(new int[]{1,2,4}, new int[]{1,3,4}, new int[]{1,1,2,3,4,4}, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new int[]{}, new int[]{}, new int[]{}, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new int[]{}, new int[]{0}, new int[]{0}, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new int[]{5}, new int[]{1,2,3}, new int[]{1,2,3,5}, 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{1,2,3}, new int[]{}, new int[]{1,2,3}, 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{-5,-3,-1}, new int[]{-4,-2,0}, new int[]{-5,-4,-3,-2,-1,0}, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new int[]{1,1,1}, new int[]{1,1,1}, new int[]{1,1,1,1,1,1}, 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new int[]{100}, new int[]{200}, new int[]{100,200}, 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new int[]{0,5,10}, new int[]{1,2,3,4,5,6}, new int[]{0,1,2,3,4,5,5,6,10}, 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5}, new int[]{6,7,8,9,10}, new int[]{1,2,3,4,5,6,7,8,9,10}, 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode { int val; ListNode* next; ListNode(int x) : val(x), next(NULL) {} };

// USER_CODE_START
class CodeCoder {
public:
    ListNode* mergeTwoLists(ListNode* a, ListNode* b) { return a; }
};
// USER_CODE_END

ListNode* build(vector<int>& a) {
    ListNode d(0), *c = &d;
    for (int v : a) { c->next = new ListNode(v); c = c->next; }
    return d.next;
}
vector<int> ser(ListNode* h) {
    vector<int> r; while (h) { r.push_back(h->val); h = h->next; } return r;
}
void test(vector<int> a1, vector<int> a2, vector<int> e, int tc, bool h = false) {
    auto g = ser(CodeCoder().mergeTwoLists(build(a1), build(a2)));
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else { cout << "TC:" << tc << ":FAIL:got=["; for (int x : g) cout << x << ","; cout << "]\\n"; }
}
int main() {
    try { test({1,2,4},{1,3,4},{1,1,2,3,4,4},1); } catch(...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({},{},{},2); } catch(...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({},{0},{0},3); } catch(...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({5},{1,2,3},{1,2,3,5},4); } catch(...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({1,2,3},{},{1,2,3},5); } catch(...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({-5,-3,-1},{-4,-2,0},{-5,-4,-3,-2,-1,0},6,true); } catch(...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({1,1,1},{1,1,1},{1,1,1,1,1,1},7,true); } catch(...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({100},{200},{100,200},8,true); } catch(...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({0,5,10},{1,2,3,4,5,6},{0,1,2,3,4,5,5,6,10},9,true); } catch(...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({1,2,3,4,5},{6,7,8,9,10},{1,2,3,4,5,6,7,8,9,10},10,true); } catch(...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def mergeTwoLists(self, a, b):
        return a
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    d = ListNode(0); c = d
    for v in a: c.next = ListNode(v); c = c.next
    return d.next
def ser(h):
    r = []
    while h: r.append(h.val); h = h.next
    return r

def test(a1, a2, e, tc, h=False):
    g = ser(CodeCoder().mergeTwoLists(build(a1), build(a2)))
    if g == e: print(f"TC:{tc}:PASS" + (":hidden" if h else ""))
    elif h: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:got={g}:exp={e}")

try: test([1,2,4],[1,3,4],[1,1,2,3,4,4],1)
except: print("TC:1:FAIL:hidden")
try: test([],[],[],2)
except: print("TC:2:FAIL:hidden")
try: test([],[0],[0],3)
except: print("TC:3:FAIL:hidden")
try: test([5],[1,2,3],[1,2,3,5],4)
except: print("TC:4:FAIL:hidden")
try: test([1,2,3],[],[1,2,3],5)
except: print("TC:5:FAIL:hidden")
try: test([-5,-3,-1],[-4,-2,0],[-5,-4,-3,-2,-1,0],6,hidden=True)
except: print("TC:6:FAIL:hidden")
try: test([1,1,1],[1,1,1],[1,1,1,1,1,1],7,hidden=True)
except: print("TC:7:FAIL:hidden")
try: test([100],[200],[100,200],8,hidden=True)
except: print("TC:8:FAIL:hidden")
try: test([0,5,10],[1,2,3,4,5,6],[0,1,2,3,4,5,5,6,10],9,hidden=True)
except: print("TC:9:FAIL:hidden")
try: test([1,2,3,4,5],[6,7,8,9,10],[1,2,3,4,5,6,7,8,9,10],10,hidden=True)
except: print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function mergeTwoLists(a, b) { return a; }
// USER_CODE_END

class ListNode {
    constructor(val, next) { this.val = val; this.next = next || null; }
}
function build(a) {
    let d = new ListNode(0), c = d;
    for (let v of a) { c.next = new ListNode(v); c = c.next; }
    return d.next;
}
function ser(h) {
    let r = [];
    while (h) { r.push(h.val); h = h.next; }
    return r;
}
function test(a1, a2, e, tc, h) {
    if (h === undefined) h = false;
    const g = ser(mergeTwoLists(build(a1), build(a2)));
    const gs = JSON.stringify(g), es = JSON.stringify(e);
    if (gs === es) console.log("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
    else if (h) console.log("TC:" + tc + ":FAIL:hidden");
    else console.log("TC:" + tc + ":FAIL:got=" + gs + ":exp=" + es);
}
try { test([1,2,4],[1,3,4],[1,1,2,3,4,4],1); } catch(e) { console.log("TC:1:FAIL:hidden"); }
try { test([],[],[],2); } catch(e) { console.log("TC:2:FAIL:hidden"); }
try { test([],[0],[0],3); } catch(e) { console.log("TC:3:FAIL:hidden"); }
try { test([5],[1,2,3],[1,2,3,5],4); } catch(e) { console.log("TC:4:FAIL:hidden"); }
try { test([1,2,3],[],[1,2,3],5); } catch(e) { console.log("TC:5:FAIL:hidden"); }
try { test([-5,-3,-1],[-4,-2,0],[-5,-4,-3,-2,-1,0],6,true); } catch(e) { console.log("TC:6:FAIL:hidden"); }
try { test([1,1,1],[1,1,1],[1,1,1,1,1,1],7,true); } catch(e) { console.log("TC:7:FAIL:hidden"); }
try { test([100],[200],[100,200],8,true); } catch(e) { console.log("TC:8:FAIL:hidden"); }
try { test([0,5,10],[1,2,3,4,5,6],[0,1,2,3,4,5,5,6,10],9,true); } catch(e) { console.log("TC:9:FAIL:hidden"); }
try { test([1,2,3,4,5],[6,7,8,9,10],[1,2,3,4,5,6,7,8,9,10],10,true); } catch(e) { console.log("TC:10:FAIL:hidden"); }'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct ListNode { int val; struct ListNode* next; };

// USER_CODE_START
struct ListNode* mergeTwoLists(struct ListNode* a, struct ListNode* b) { return a; }
// USER_CODE_END

struct ListNode* build(int* a, int n) {
    if (!n) return NULL;
    struct ListNode* h = malloc(sizeof(struct ListNode));
    h->val = a[0]; h->next = NULL;
    struct ListNode* c = h;
    for (int i = 1; i < n; i++) {
        c->next = malloc(sizeof(struct ListNode));
        c = c->next; c->val = a[i]; c->next = NULL;
    }
    return h;
}
int* ser(struct ListNode* h, int* n) {
    *n = 0; struct ListNode* c = h;
    while (c) { (*n)++; c = c->next; }
    int* r = malloc(*n * sizeof(int)); c = h;
    for (int i = 0; i < *n; i++) { r[i] = c->val; c = c->next; }
    return r;
}
int arrEq(int* a, int* b, int n) { for (int i = 0; i < n; i++) if (a[i] != b[i]) return 0; return 1; }
void run(int* a1, int n1, int* a2, int n2, int* e, int en, int tc, int h) {
    struct ListNode* l1 = build(a1, n1), *l2 = build(a2, n2);
    struct ListNode* m = mergeTwoLists(l1, l2);
    int gn; int* g = ser(m, &gn);
    if (gn == en && arrEq(g, e, gn)) {
        if (h) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (h) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL\\n", tc);
    }
    free(g);
}
int main() {
    int a1[]={1,2,4}, a2[]={1,3,4}, e1[]={1,1,2,3,4,4}; run(a1,3,a2,3,e1,6,1,0);
    run(NULL,0,NULL,0,NULL,0,2,0);
    int a3[]={0}, e3[]={0}; run(NULL,0,a3,1,e3,1,3,0);
    int a4[]={5}, a5[]={1,2,3}, e4[]={1,2,3,5}; run(a4,1,a5,3,e4,4,4,0);
    int a6[]={1,2,3}, e5[]={1,2,3}; run(a6,3,NULL,0,e5,3,5,0);
    int a7[]={-5,-3,-1}, a8[]={-4,-2,0}, e6[]={-5,-4,-3,-2,-1,0}; run(a7,3,a8,3,e6,6,6,1);
    int a9[]={1,1,1}, a10[]={1,1,1}, e7[]={1,1,1,1,1,1}; run(a9,3,a10,3,e7,6,7,1);
    int a11[]={100}, a12[]={200}, e8[]={100,200}; run(a11,1,a12,1,e8,2,8,1);
    int a13[]={0,5,10}, a14[]={1,2,3,4,5,6}, e9[]={0,1,2,3,4,5,5,6,10}; run(a13,3,a14,6,e9,9,9,1);
    int a15[]={1,2,3,4,5}, a16[]={6,7,8,9,10}, e10[]={1,2,3,4,5,6,7,8,9,10}; run(a15,5,a16,5,e10,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
