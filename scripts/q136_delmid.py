"""
Delete the Middle Node of a Linked List
=========================================
Given the head of a singly linked list, delete the MIDDLE node of the list and
return the head. The middle node is the one at 0-based index floor(n / 2)
where n is the length of the list.

Examples:
  head = 1->3->4->7->1->2->6 (n=7) -> 1->3->4->1->2->6 (index 3 removed)
  head = 1->2->3->4 (n=4) -> 1->2->4 (index 2 removed)
  head = 2->1 (n=2) -> 2 (index 1 removed)
  head = 1 (n=1) -> null

Two-pointer technique: slow moves one step, fast moves two steps. When fast
reaches the end, slow is the middle node; keep a prev pointer to unlink it.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
deleteMiddle(head), and traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Delete the Middle Node of a Linked List"
desc=(
    "Given the head of a singly linked list, delete the MIDDLE node of the "
    "list and return the head of the updated list.\n\n"
    "The middle node is the node at 0-based index floor(n/2), where n is the "
    "length of the list.\n\n"
    "For example:\n"
    "head = 1->3->4->7->1->2->6 (n=7) -> 1->3->4->1->2->6 (index 3 removed)\n"
    "head = 1->2->3->4 (n=4)         -> 1->2->4          (index 2 removed)\n"
    "head = 2->1 (n=2)               -> 2                (index 1 removed)\n"
    "head = 1 (n=1)                  -> null (empty list)\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Use the two-pointer technique: slow "
    "advances one step while fast advances two; when fast reaches the end, "
    "slow points at the middle node — unlink it with a prev pointer. If the "
    "list has one node, return null."
)
infmt="First line contains n. Second line contains n space-separated values."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="1 ≤ n ≤ 1000\n1 ≤ val ≤ 10^6"
e1="Input:\n7\n1 3 4 7 1 2 6\n\nOutput:\n1 3 4 1 2 6"
e2="Input:\n4\n1 2 3 4\n\nOutput:\n1 2 4"
e3="Input:\n2\n2 1\n\nOutput:\n2"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Deletion, Two Pointers",e1,e2,e3,"MEDIUM",5.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Deletion, Two Pointers",e1,e2,e3))
    pid=cur.fetchone()[0]
    print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// Definition for singly-linked list. (Provided by the harness; do not edit.)
class Node {
    int val;
    Node next;
    Node(int x) { val = x; next = null; }
}

// USER_CODE_START
/**
 * Definition for singly-linked list.
 * public class Node {
 *     int val;
 *     Node next;
 *     Node(int x) { this.val = x; this.next = null; }
 * }
 */
class CodeCoder {
    public Node deleteMiddle(Node head) {
        // Write your code here — delete node at index floor(n/2)
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static List<Integer> toList(Node h){List<Integer> l=new ArrayList<>();while(h!=null){l.add(h.val);h=h.next;}return l;}
static void test(int[] a,int[] e,int tc,boolean hd){List<Integer> g=toList(new CodeCoder().deleteMiddle(build(a)));boolean ok=Arrays.equals(g.stream().mapToInt(i->i).toArray(),e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+g);}
public static void main(String[] x){
try{test(new int[]{1,3,4,7,1,2,6},new int[]{1,3,4,1,2,6},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4},new int[]{1,2,4},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{2,1},new int[]{2},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},new int[]{},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{1,2,4,5},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7},new int[]{1,2,3,5,6,7},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},new int[]{10,20,40,50},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{5},new int[]{},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1},new int[]{1,1,1,1},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},new int[]{1,2,3,5,6},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// Definition for singly-linked list. (Provided by the harness; do not edit.)
class Node {
public:
    int val;
    Node* next;
    Node(int x) : val(x), next(NULL) {}
};

// USER_CODE_START
/**
 * Definition for singly-linked list.
 * struct Node {
 *     int val;
 *     Node *next;
 *     Node(int x) : val(x), next(NULL) {}
 * };
 */
class CodeCoder {
public:
    Node* deleteMiddle(Node* head) {
        // Write your code here — delete node at index floor(n/2)
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
vector<int> toList(Node* h){vector<int> l;while(h){l.push_back(h->val);h=h->next;}return l;}
void test(vector<int> a,vector<int> e,int tc,bool hd=false){vector<int> g=toList(CodeCoder().deleteMiddle(build(a)));bool ok=(g==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}cout<<"]\\n";}}
int main(){
try{test({1,3,4,7,1,2,6},{1,3,4,1,2,6},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4},{1,2,4},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({2,1},{2},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},{},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{1,2,4,5},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7},{1,2,3,5,6,7},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40,50},{10,20,40,50},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({5},{},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,1,1,1,1},{1,1,1,1},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},{1,2,3,5,6},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# Definition for singly-linked list. (Provided by the harness; do not edit.)
class Node:
    def __init__(self, x):
        self.val = x
        self.next = None

# USER_CODE_START
# Definition for singly-linked list.
# class Node:
#     def __init__(self, x):
#         self.val = x
#         self.next = None

class CodeCoder:
    def deleteMiddle(self, head):
        # Write your code here — delete node at index floor(n/2)
        return head
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def tolist(h):
    out=[]
    while h:
        out.append(h.val);h=h.next
    return out
def test(a,e,tc,h=False):
    try:
        g=tolist(CodeCoder().deleteMiddle(build(a)));ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
test([1,3,4,7,1,2,6],[1,3,4,1,2,6],1)
test([1,2,3,4],[1,2,4],2)
test([2,1],[2],3)
test([1],[],4)
test([1,2,3,4,5],[1,2,4,5],5)
test([1,2,3,4,5,6,7],[1,2,3,5,6,7],6,True)
test([10,20,30,40,50],[10,20,40,50],7,True)
test([5],[],8,True)
test([1,1,1,1,1],[1,1,1,1],9,True)
test([1,2,3,4,5,6],[1,2,3,5,6],10,True)'''

js_code='''// Definition for singly-linked list. (Provided by the harness; do not edit.)
class Node {
    constructor(x) { this.val = x; this.next = null; }
}

// USER_CODE_START
/**
 * Definition for singly-linked list.
 * function Node(val) {
 *     this.val = val;
 *     this.next = null;
 * }
 */
function deleteMiddle(head) {
    // Write your code here — delete node at index floor(n/2)
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function tolist(h){const o=[];while(h){o.push(h.val);h=h.next;}return o;}
function test(a,e,tc,h){if(h===undefined)h=false;let g,ok=false;try{g=tolist(deleteMiddle(build(a)));ok=JSON.stringify(g)===JSON.stringify(e);}catch(err){g=["EXC"];}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test([1,3,4,7,1,2,6],[1,3,4,1,2,6],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4],[1,2,4],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([2,1],[2],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],[],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],[1,2,4,5],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],[1,2,3,5,6,7],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40,50],[10,20,40,50],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([5],[],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,1,1,1,1],[1,1,1,1],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5,6],[1,2,3,5,6],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// Definition for singly-linked list. (Provided by the harness; do not edit.)
typedef struct Node {
    int val;
    struct Node* next;
} Node;

// USER_CODE_START
/**
 * Definition for singly-linked list.
 * struct Node {
 *     int val;
 *     struct Node *next;
 * };
 */
Node* deleteMiddle(Node* head) {
    // Write your code here — delete node at index floor(n/2)
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int* e,int en,int tc,int hd){
    Node* h=deleteMiddle(build(a,n));
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:exp=[");for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}printf("]:got=[");int first=1;Node* p=h;while(p){if(!first)printf(",");printf("%d",p->val);first=0;p=p->next;}printf("]\\n");}
}
int main(){
    int a1[]={1,3,4,7,1,2,6};int e1[]={1,3,4,1,2,6};runTest(a1,7,e1,6,1,0);
    int a2[]={1,2,3,4};int e2[]={1,2,4};runTest(a2,4,e2,3,2,0);
    int a3[]={2,1};int e3[]={2};runTest(a3,2,e3,1,3,0);
    int a4[]={1};runTest(a4,1,NULL,0,4,0);
    int a5[]={1,2,3,4,5};int e5[]={1,2,4,5};runTest(a5,5,e5,4,5,0);
    int a6[]={1,2,3,4,5,6,7};int e6[]={1,2,3,5,6,7};runTest(a6,7,e6,6,6,1);
    int a7[]={10,20,30,40,50};int e7[]={10,20,40,50};runTest(a7,5,e7,4,7,1);
    int a8[]={5};runTest(a8,1,NULL,0,8,1);
    int a9[]={1,1,1,1,1};int e9[]={1,1,1,1};runTest(a9,5,e9,4,9,1);
    int a10[]={1,2,3,4,5,6};int e10[]={1,2,3,5,6};runTest(a10,6,e10,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
