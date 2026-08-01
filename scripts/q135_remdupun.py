"""
Remove Duplicates from Unsorted List
======================================
Given the head of a singly linked list that is NOT sorted, remove all duplicate
nodes so that each value appears only ONCE (keep the FIRST occurrence of each
value), and return the head.

Examples:
  head = 1->2->3->2->1    -> 1->2->3
  head = 5->3->5->4->1    -> 5->3->4->1
  head = 1->1->1          -> 1

Use a HashSet to remember values already seen: keep the first occurrence,
skip any later node whose value is already in the set.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
removeDupUnsorted(head), and traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Duplicates from Unsorted List"
desc=(
    "Given the head of a singly linked list that is NOT sorted, delete all "
    "duplicate nodes so that each value appears exactly once. Keep the FIRST "
    "occurrence of each value and remove every later copy. Return the head.\n\n"
    "For example:\n"
    "head = 1->2->3->2->1 -> 1->2->3\n"
    "head = 5->3->5->4->1 -> 5->3->4->1\n"
    "head = 1->1->1       -> 1\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Because the list is unsorted you need a "
    "HashSet of seen values: keep the first occurrence of each value and unlink "
    "any later node whose value is already in the set. Runs in O(n) with the "
    "set, O(1) without it."
)
infmt="First line contains n. Second line contains n space-separated values (unsorted)."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="0 ≤ n ≤ 1000\n1 ≤ val ≤ 10^6"
e1="Input:\n5\n1 2 3 2 1\n\nOutput:\n1 2 3"
e2="Input:\n5\n5 3 5 4 1\n\nOutput:\n5 3 4 1"
e3="Input:\n3\n1 1 1\n\nOutput:\n1"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Deletion, Hash Set",e1,e2,e3,"MEDIUM",5.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Deletion, Hash Set",e1,e2,e3))
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
    public Node removeDupUnsorted(Node head) {
        // Write your code here — keep first occurrence of each value
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static List<Integer> toList(Node h){List<Integer> l=new ArrayList<>();while(h!=null){l.add(h.val);h=h.next;}return l;}
static void test(int[] a,int[] e,int tc,boolean hd){Node h=new CodeCoder().removeDupUnsorted(build(a));List<Integer> g=toList(h);boolean ok=Arrays.equals(g.stream().mapToInt(i->i).toArray(),e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+g);}
public static void main(String[] x){
try{test(new int[]{1,2,3,2,1},new int[]{1,2,3},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5,3,5,4,1},new int[]{5,3,4,1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,1},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{1,2,3,4,5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},new int[]{},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{2,1,2,3,1,2,4},new int[]{2,1,3,4},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{7,7,7,7},new int[]{7},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{3,1,4,1,5,9,2,6,5,3},new int[]{3,1,4,5,9,2,6},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5},new int[]{5},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,20,10,30,20,40},new int[]{10,20,30,40},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* removeDupUnsorted(Node* head) {
        // Write your code here — keep first occurrence of each value
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
vector<int> toList(Node* h){vector<int> l;while(h){l.push_back(h->val);h=h->next;}return l;}
void test(vector<int> a,vector<int> e,int tc,bool hd=false){vector<int> g=toList(CodeCoder().removeDupUnsorted(build(a)));bool ok=(g==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}cout<<"]\\n";}}
int main(){
try{test({1,2,3,2,1},{1,2,3},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5,3,5,4,1},{5,3,4,1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,1},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{1,2,3,4,5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},{},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({2,1,2,3,1,2,4},{2,1,3,4},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({7,7,7,7},{7},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({3,1,4,1,5,9,2,6,5,3},{3,1,4,5,9,2,6},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5},{5},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,20,10,30,20,40},{10,20,30,40},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def removeDupUnsorted(self, head):
        # Write your code here — keep first occurrence of each value
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
        g=tolist(CodeCoder().removeDupUnsorted(build(a)));ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
test([1,2,3,2,1],[1,2,3],1)
test([5,3,5,4,1],[5,3,4,1],2)
test([1,1,1],[1],3)
test([1,2,3,4,5],[1,2,3,4,5],4)
test([],[],5)
test([2,1,2,3,1,2,4],[2,1,3,4],6,True)
test([7,7,7,7],[7],7,True)
test([3,1,4,1,5,9,2,6,5,3],[3,1,4,5,9,2,6],8,True)
test([5],[5],9,True)
test([10,20,10,30,20,40],[10,20,30,40],10,True)'''

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
function removeDupUnsorted(head) {
    // Write your code here — keep first occurrence of each value
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function tolist(h){const o=[];while(h){o.push(h.val);h=h.next;}return o;}
function test(a,e,tc,h){if(h===undefined)h=false;let g,ok=false;try{g=tolist(removeDupUnsorted(build(a)));ok=JSON.stringify(g)===JSON.stringify(e);}catch(err){g=["EXC"];}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test([1,2,3,2,1],[1,2,3],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,3,5,4,1],[5,3,4,1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,1],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],[1,2,3,4,5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],[],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2,1,2,3,1,2,4],[2,1,3,4],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([7,7,7,7],[7],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([3,1,4,1,5,9,2,6,5,3],[3,1,4,5,9,2,6],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5],[5],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,20,10,30,20,40],[10,20,30,40],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* removeDupUnsorted(Node* head) {
    // Write your code here — keep first occurrence of each value
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int* e,int en,int tc,int hd){
    Node* h=removeDupUnsorted(build(a,n));
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:exp=[");for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}printf("]:got=[");int first=1;Node* p=h;while(p){if(!first)printf(",");printf("%d",p->val);first=0;p=p->next;}printf("]\\n");}
}
int main(){
    int a1[]={1,2,3,2,1};int e1[]={1,2,3};runTest(a1,5,e1,3,1,0);
    int a2[]={5,3,5,4,1};int e2[]={5,3,4,1};runTest(a2,5,e2,4,2,0);
    int a3[]={1,1,1};int e3[]={1};runTest(a3,3,e3,1,3,0);
    int a4[]={1,2,3,4,5};int e4[]={1,2,3,4,5};runTest(a4,5,e4,5,4,0);
    runTest(NULL,0,NULL,0,5,0);
    int a6[]={2,1,2,3,1,2,4};int e6[]={2,1,3,4};runTest(a6,7,e6,4,6,1);
    int a7[]={7,7,7,7};int e7[]={7};runTest(a7,4,e7,1,7,1);
    int a8[]={3,1,4,1,5,9,2,6,5,3};int e8[]={3,1,4,5,9,2,6};runTest(a8,10,e8,7,8,1);
    int a9[]={5};int e9[]={5};runTest(a9,1,e9,1,9,1);
    int a10[]={10,20,10,30,20,40};int e10[]={10,20,30,40};runTest(a10,6,e10,4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
