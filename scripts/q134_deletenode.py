"""
Delete Node in a Linked List
==============================
You are given a reference to a NODE in a singly linked list (NOT the head).
Delete this node from the list. The given node is guaranteed NOT to be the
tail (so node.next is never null). The function returns void.

Examples:
  list = 4->5->1->9, delete node(5)  -> 4->1->9
  list = 4->5->1->9, delete node(1)  -> 4->5->9

Trick: since we have no access to the previous node, copy node.next's value
into node, then make node.next point to node.next.next. The node is
effectively removed without touching the head.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, locates the
node to delete (each value is unique), calls your deleteNode(node), and
traverses from the head to verify the remaining order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Delete Node in a Linked List"
desc=(
    "You are given a reference to a NODE of a singly linked list — NOT the "
    "head — and you must delete this node from the list. The given node is "
    "guaranteed NOT to be the tail node, so node.next is never null. The "
    "function returns void (you cannot return the new head).\n\n"
    "For example:\n"
    "list = 4->5->1->9, delete node(5) -> 4->1->9\n"
    "list = 4->5->1->9, delete node(1) -> 4->5->9\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Because you only have the node itself, "
    "copy node.next's value into node and then set node.next = node.next.next "
    "— this removes the node's successor from the chain, effectively deleting "
    "the target node in O(1). The harness builds the list, locates the target "
    "node by its value (values are unique), calls your deleteNode(node), and "
    "traverses from the head to verify the remaining order."
)
infmt="First line contains n and the value of the node to delete. Second line contains n space-separated unique values."
outfmt="The harness traverses the resulting list from the head and prints PASS/FAIL based on the order."
cons="2 ≤ n ≤ 1000\nValues are unique and the node to delete is not the tail."
e1="Input:\n4 5\n4 5 1 9\n\nOutput:\n4 1 9"
e2="Input:\n4 1\n4 5 1 9\n\nOutput:\n4 5 9"
e3="Input:\n3 2\n1 2 3\n\nOutput:\n1 3"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Deletion",e1,e2,e3,"MEDIUM",5.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Deletion",e1,e2,e3))
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
    public void deleteNode(Node node) {
        // Write your code here — node is not the tail
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static Node find(Node h,int v){while(h!=null){if(h.val==v)return h;h=h.next;}return null;}
static void test(int[] a,int del,int[] e,int tc,boolean hd){Node head=build(a);Node node=find(head,del);new CodeCoder().deleteNode(node);Node h=head;boolean ok=true;for(int i=0;i<e.length;i++){if(h==null||h.val!=e[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node cur=head;while(cur!=null){gl.add(cur.val);cur=cur.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":del="+del+":exp="+Arrays.toString(e)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{4,5,1,9},5,new int[]{4,1,9},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{4,5,1,9},1,new int[]{4,5,9},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3},2,new int[]{1,3},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4},3,new int[]{1,2,4},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{5,6,7,8},6,new int[]{5,7,8},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},4,new int[]{1,2,3,5,6,7,8,9,10},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},20,new int[]{10,30,40,50},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{7,8,9,10},9,new int[]{7,8,10},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{100,200,300,400,500},300,new int[]{100,200,400,500},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,4,6,8,10},4,new int[]{2,6,8,10},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    void deleteNode(Node* node) {
        // Write your code here — node is not the tail
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
Node* find(Node* h,int v){while(h){if(h->val==v)return h;h=h->next;}return NULL;}
 void test(vector<int> a,int del,vector<int> e,int tc,bool hd=false){Node* head=build(a);CodeCoder().deleteNode(find(head,del));Node* h=head;bool ok=true;for(int i=0;i<(int)e.size();i++){if(h==NULL||h->val!=e[i]){ok=false;break;}h=h->next;}if(ok&&h!=NULL)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:del="<<del<<":exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(Node* p=head;p!=NULL;p=p->next){if(p!=head)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({4,5,1,9},5,{4,1,9},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({4,5,1,9},1,{4,5,9},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3},2,{1,3},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4},3,{1,2,4},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({5,6,7,8},6,{5,7,8},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},4,{1,2,3,5,6,7,8,9,10},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40,50},20,{10,30,40,50},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({7,8,9,10},9,{7,8,10},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({100,200,300,400,500},300,{100,200,400,500},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,4,6,8,10},4,{2,6,8,10},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def deleteNode(self, node):
        # Write your code here — node is not the tail
        pass
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def find(h,v):
    while h:
        if h.val==v: return h
        h=h.next
    return None
def test(a,delv,e,tc,h=False):
    try:
        head=build(a);CodeCoder().deleteNode(find(head,delv));cur=head;ok=True
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
            head=build(a);CodeCoder().deleteNode(find(head,delv));c2=head
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:del={delv}:exp={e}:got={gl}")
test([4,5,1,9],5,[4,1,9],1)
test([4,5,1,9],1,[4,5,9],2)
test([1,2,3],2,[1,3],3)
test([1,2,3,4],3,[1,2,4],4)
test([5,6,7,8],6,[5,7,8],5)
test([1,2,3,4,5,6,7,8,9,10],4,[1,2,3,5,6,7,8,9,10],6,True)
test([10,20,30,40,50],20,[10,30,40,50],7,True)
test([7,8,9,10],9,[7,8,10],8,True)
test([100,200,300,400,500],300,[100,200,400,500],9,True)
test([2,4,6,8,10],4,[2,6,8,10],10,True)'''

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
function deleteNode(node) {
    // Write your code here — node is not the tail
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function find(h,v){while(h){if(h.val===v)return h;h=h.next;}return null;}
function test(a,delv,e,tc,h){if(h===undefined)h=false;let ok=true;let head;try{head=build(a);deleteNode(find(head,delv));let cur=head;for(let i=0;i<e.length;i++){if(cur===null||cur.val!==e[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(err){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let head2=build(a);deleteNode(find(head2,delv));let c2=head2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":del="+delv+":exp="+JSON.stringify(e)+":got="+JSON.stringify(gl));}}
try{test([4,5,1,9],5,[4,1,9],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([4,5,1,9],1,[4,5,9],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3],2,[1,3],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4],3,[1,2,4],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([5,6,7,8],6,[5,7,8],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],4,[1,2,3,5,6,7,8,9,10],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40,50],20,[10,30,40,50],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([7,8,9,10],9,[7,8,10],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([100,200,300,400,500],300,[100,200,400,500],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,4,6,8,10],4,[2,6,8,10],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
void deleteNode(Node* node) {
    // Write your code here — node is not the tail
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
Node* find(Node* h,int v){while(h){if(h->val==v)return h;h=h->next;}return NULL;}
void runTest(int* a,int n,int del,int* e,int en,int tc,int hd){
    Node* head=build(a,n);
    deleteNode(find(head,del));
    Node* h=head;
    int ok=1;
    for(int i=0;i<en;i++){if(h==NULL||h->val!=e[i]){ok=0;break;}h=h->next;}
    if(ok&&h!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:del=%d:exp=[",del);
        for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}
        printf("]:got=[");
        for(Node* p=head;p!=NULL;p=p->next){if(p!=head)printf(",");printf("%d",p->val);}
        printf("]\\n");
    }
}
int main(){
    int a1[]={4,5,1,9};int e1[]={4,1,9};runTest(a1,4,5,e1,3,1,0);
    int a2[]={4,5,1,9};int e2[]={4,5,9};runTest(a2,4,1,e2,3,2,0);
    int a3[]={1,2,3};int e3[]={1,3};runTest(a3,3,2,e3,2,3,0);
    int a4[]={1,2,3,4};int e4[]={1,2,4};runTest(a4,4,3,e4,3,4,0);
    int a5[]={5,6,7,8};int e5[]={5,7,8};runTest(a5,4,6,e5,3,5,0);
    int a6[]={1,2,3,4,5,6,7,8,9,10};int e6[]={1,2,3,5,6,7,8,9,10};runTest(a6,10,4,e6,9,6,1);
    int a7[]={10,20,30,40,50};int e7[]={10,30,40,50};runTest(a7,5,20,e7,4,7,1);
    int a8[]={7,8,9,10};int e8[]={7,8,10};runTest(a8,4,9,e8,3,8,1);
    int a9[]={100,200,300,400,500};int e9[]={100,200,400,500};runTest(a9,5,300,e9,4,9,1);
    int a10[]={2,4,6,8,10};int e10[]={2,6,8,10};runTest(a10,5,4,e10,4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
