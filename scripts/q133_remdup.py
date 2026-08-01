"""
Remove Duplicates from Sorted List
====================================
Given the head of a SORTED singly linked list, delete all duplicates so that
each value appears only ONCE, and return the head. Because the list is sorted,
duplicates are always consecutive.

Examples:
  head = 1->1->2          -> 1->2
  head = 1->1->2->3->3    -> 1->2->3
  head = 1->1->1->1       -> 1

Walk the list with a single pointer: while cur.next has the same value as cur,
skip it; otherwise advance. The relative order stays unchanged.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
deleteDuplicates(head), and traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Duplicates from Sorted List"
desc=(
    "Given the head of a SORTED singly linked list, delete all duplicate nodes "
    "so that each value appears exactly once, and return the head of the "
    "updated list.\n\n"
    "For example:\n"
    "head = 1->1->2       -> 1->2\n"
    "head = 1->1->2->3->3 -> 1->2->3\n"
    "head = 1->1->1->1    -> 1\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Because the list is sorted, all copies "
    "of a value are consecutive — walk the list and, whenever the next node "
    "has the same value as the current one, skip it (keep the first copy). "
    "The harness builds the list, calls your deleteDuplicates(head), and "
    "traverses the result to verify the order."
)
infmt="First line contains n. Second line contains n space-separated SORTED values."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="0 ≤ n ≤ 1000\n1 ≤ val ≤ 10^6\nThe list is sorted in ascending order."
e1="Input:\n3\n1 1 2\n\nOutput:\n1 2"
e2="Input:\n5\n1 1 2 3 3\n\nOutput:\n1 2 3"
e3="Input:\n4\n1 1 1 1\n\nOutput:\n1"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Deletion, Sorted List",e1,e2,e3,"EASY",3.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Deletion, Sorted List",e1,e2,e3))
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
    public Node deleteDuplicates(Node head) {
        // Write your code here — keep one copy of each value
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static void test(int[] a,int[] e,int tc,boolean hd){Node h=new CodeCoder().deleteDuplicates(build(a));boolean ok=true;for(int i=0;i<e.length;i++){if(h==null||h.val!=e[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node t=new CodeCoder().deleteDuplicates(build(a));while(t!=null){gl.add(t.val);t=t.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{1,1,2},new int[]{1,2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,1,2,3,3},new int[]{1,2,3},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,1,1},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{1,2,3,4,5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},new int[]{},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,2,2,3,3,3,4,4},new int[]{1,2,3,4},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{5,5,6,6,7,7},new int[]{5,6,7},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,10,20,20,30,30,40,40},new int[]{10,20,30,40},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,2,2,3,3,4},new int[]{2,3,4},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* deleteDuplicates(Node* head) {
        // Write your code here — keep one copy of each value
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
void test(vector<int> a,vector<int> e,int tc,bool hd=false){Node* h=CodeCoder().deleteDuplicates(build(a));bool ok=true;for(int i=0;i<(int)e.size();i++){if(h==nullptr||h->val!=e[i]){ok=false;break;}h=h->next;}if(ok&&h!=nullptr)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{Node* g=CodeCoder().deleteDuplicates(build(a));cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(Node* p=g;p!=nullptr;p=p->next){if(p!=g)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({1,1,2},{1,2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,1,2,3,3},{1,2,3},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,1,1},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{1,2,3,4,5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},{},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,2,2,3,3,3,4,4},{1,2,3,4},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({5,5,6,6,7,7},{5,6,7},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1},{1},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,10,20,20,30,30,40,40},{10,20,30,40},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,2,2,3,3,4},{2,3,4},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def deleteDuplicates(self, head):
        # Write your code here — keep one copy of each value
        return head
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def test(a,e,tc,h=False):
    try:
        hd=CodeCoder().deleteDuplicates(build(a));cur=hd;ok=True
        for v in e:
            if cur is None or cur.val!=v: ok=False; break
            cur=cur.next
        if cur is not None: ok=False
    except Exception:
        ok=False
    if ok:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:
        gl=[]
        try:
            hd2=CodeCoder().deleteDuplicates(build(a));c2=hd2
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:exp={e}:got={gl}")
test([1,1,2],[1,2],1)
test([1,1,2,3,3],[1,2,3],2)
test([1,1,1,1],[1],3)
test([1,2,3,4,5],[1,2,3,4,5],4)
test([],[],5)
test([1,1,1,2,2,3,3,3,4,4],[1,2,3,4],6,True)
test([5,5,6,6,7,7],[5,6,7],7,True)
test([1],[1],8,True)
test([10,10,20,20,30,30,40,40],[10,20,30,40],9,True)
test([2,2,2,3,3,4],[2,3,4],10,True)'''

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
function deleteDuplicates(head) {
    // Write your code here — keep one copy of each value
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function test(a,e,tc,h){if(h===undefined)h=false;let ok=true;try{let hd=deleteDuplicates(build(a));let cur=hd;for(let i=0;i<e.length;i++){if(cur===null||cur.val!==e[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(err){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let hd2=deleteDuplicates(build(a));let c2=hd2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(gl));}}
try{test([1,1,2],[1,2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,1,2,3,3],[1,2,3],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,1,1],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],[1,2,3,4,5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],[],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,2,2,3,3,3,4,4],[1,2,3,4],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([5,5,6,6,7,7],[5,6,7],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1],[1],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,10,20,20,30,30,40,40],[10,20,30,40],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,2,2,3,3,4],[2,3,4],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* deleteDuplicates(Node* head) {
    // Write your code here — keep one copy of each value
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int* e,int en,int tc,int hd){
    Node* h=deleteDuplicates(build(a,n));
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:exp=[");
        for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}
        printf("]:got=[");
        Node* g=deleteDuplicates(build(a,n));int fi=0;
        for(Node* p=g;p!=NULL;p=p->next){if(fi)printf(",");printf("%d",p->val);fi=1;}
        printf("]\\n");
    }
}
int main(){
    int a1[]={1,1,2};int e1[]={1,2};runTest(a1,3,e1,2,1,0);
    int a2[]={1,1,2,3,3};int e2[]={1,2,3};runTest(a2,5,e2,3,2,0);
    int a3[]={1,1,1,1};int e3[]={1};runTest(a3,4,e3,1,3,0);
    int a4[]={1,2,3,4,5};int e4[]={1,2,3,4,5};runTest(a4,5,e4,5,4,0);
    runTest(NULL,0,NULL,0,5,0);
    int a6[]={1,1,1,2,2,3,3,3,4,4};int e6[]={1,2,3,4};runTest(a6,10,e6,4,6,1);
    int a7[]={5,5,6,6,7,7};int e7[]={5,6,7};runTest(a7,6,e7,3,7,1);
    int a8[]={1};int e8[]={1};runTest(a8,1,e8,1,8,1);
    int a9[]={10,10,20,20,30,30,40,40};int e9[]={10,20,30,40};runTest(a9,8,e9,4,9,1);
    int a10[]={2,2,2,3,3,4};int e10[]={2,3,4};runTest(a10,6,e10,3,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
