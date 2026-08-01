"""
Insert Node at Head / Tail
============================
Implement two operations on a singly linked list:
  - insertAtHead(head, data): inserts a new node with the given data at the
    FRONT of the list and returns the new head.
  - insertAtTail(head, data): inserts a new node with the given data at the
    END of the list and returns the (possibly new) head.

Examples:
  insertAtTail(1->2->3, 4)  -> 1->2->3->4
  insertAtTail(null, 5)     -> 5
  insertAtHead(2->3, 1)     -> 1->2->3
  insertAtHead(null, 7)     -> 7

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls the
requested operation, then traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Insert Node at Head / Tail"
desc=(
    "Implement two insertion operations on a singly linked list.\n\n"
    "Requirements:\n"
    "- insertAtHead(head, data): create a new node holding data and insert it "
    "at the FRONT of the list; return the new head.\n"
    "- insertAtTail(head, data): create a new node holding data and insert it "
    "at the END of the list; return the head (if the list was empty, the new "
    "node IS the head).\n\n"
    "For example:\n"
    "insertAtTail(1->2->3, 4) -> 1->2->3->4\n"
    "insertAtTail(null, 5)    -> 5\n"
    "insertAtHead(2->3, 1)    -> 1->2->3\n"
    "insertAtHead(null, 7)    -> 7\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. The harness builds the starting list, "
    "calls the requested operation, and traverses the returned list to verify "
    "the resulting order."
)
infmt="First line: n (initial length), op ('head' or 'tail'), data.\nSecond line: n space-separated initial values (empty line if n=0)."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="0 ≤ n ≤ 1000\n1 ≤ data, val ≤ 10^6"
e1="Input:\n3 tail 4\n1 2 3\n\nOutput:\n1 2 3 4"
e2="Input:\n0 head 7\n\nOutput:\n7"
e3="Input:\n2 head 1\n2 3\n\nOutput:\n1 2 3"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Insertion",e1,e2,e3,"EASY",3.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Insertion",e1,e2,e3))
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
    public Node insertAtHead(Node head, int data) {
        // Write your code here — insert data at front, return new head
        return head;
    }
    public Node insertAtTail(Node head, int data) {
        // Write your code here — insert data at end, return head
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static void test(int[] a,String op,int data,int[] e,int tc,boolean hd){Node h=build(a);if(op.equals("head"))h=new CodeCoder().insertAtHead(h,data);else h=new CodeCoder().insertAtTail(h,data);boolean ok=true;for(int i=0;i<e.length;i++){if(h==null||h.val!=e[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node g=build(a);if(op.equals("head"))g=new CodeCoder().insertAtHead(g,data);else g=new CodeCoder().insertAtTail(g,data);while(g!=null){gl.add(g.val);g=g.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":op="+op+":data="+data+":exp="+Arrays.toString(e)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{1,2,3},"tail",4,new int[]{1,2,3,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{},"head",7,new int[]{7},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{2,3},"head",1,new int[]{1,2,3},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5},"tail",6,new int[]{5,6},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},"tail",5,new int[]{5},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},"tail",6,new int[]{1,2,3,4,5,6},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{2,4,6,8},"head",0,new int[]{0,2,4,6,8},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1},"head",9,new int[]{9,1},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,20,30,40},"tail",50,new int[]{10,20,30,40,50},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{5,5,5},"head",5,new int[]{5,5,5,5},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* insertAtHead(Node* head, int data) {
        // Write your code here — insert data at front, return new head
        return head;
    }
    Node* insertAtTail(Node* head, int data) {
        // Write your code here — insert data at end, return head
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
 void test(vector<int> a,string op,int data,vector<int> e,int tc,bool hd=false){Node* h=build(a);if(op=="head")h=CodeCoder().insertAtHead(h,data);else h=CodeCoder().insertAtTail(h,data);bool ok=true;for(int i=0;i<(int)e.size();i++){if(h==NULL||h->val!=e[i]){ok=false;break;}h=h->next;}if(ok&&h!=NULL)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{Node* g=build(a);if(op=="head")g=CodeCoder().insertAtHead(g,data);else g=CodeCoder().insertAtTail(g,data);cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:op="<<op<<":data="<<data<<":exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(Node* p=g;p!=NULL;p=p->next){if(p!=g)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({1,2,3},"tail",4,{1,2,3,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({},"head",7,{7},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({2,3},"head",1,{1,2,3},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5},"tail",6,{5,6},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},"tail",5,{5},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},"tail",6,{1,2,3,4,5,6},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({2,4,6,8},"head",0,{0,2,4,6,8},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1},"head",9,{9,1},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,20,30,40},"tail",50,{10,20,30,40,50},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({5,5,5},"head",5,{5,5,5,5},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def insertAtHead(self, head, data):
        # Write your code here — insert data at front, return new head
        return head
    def insertAtTail(self, head, data):
        # Write your code here — insert data at end, return head
        return head
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def test(a,op,data,e,tc,h=False):
    try:
        hd=build(a)
        if op=="head": hd=CodeCoder().insertAtHead(hd,data)
        else: hd=CodeCoder().insertAtTail(hd,data)
        cur=hd;ok=True
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
            hd2=build(a)
            if op=="head": hd2=CodeCoder().insertAtHead(hd2,data)
            else: hd2=CodeCoder().insertAtTail(hd2,data)
            c2=hd2
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:op={op}:data={data}:exp={e}:got={gl}")
test([1,2,3],"tail",4,[1,2,3,4],1)
test([],"head",7,[7],2)
test([2,3],"head",1,[1,2,3],3)
test([5],"tail",6,[5,6],4)
test([],"tail",5,[5],5)
test([1,2,3,4,5],"tail",6,[1,2,3,4,5,6],6,True)
test([2,4,6,8],"head",0,[0,2,4,6,8],7,True)
test([1],"head",9,[9,1],8,True)
test([10,20,30,40],"tail",50,[10,20,30,40,50],9,True)
test([5,5,5],"head",5,[5,5,5,5],10,True)'''

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
function insertAtHead(head, data) {
    // Write your code here — insert data at front, return new head
    return head;
}
function insertAtTail(head, data) {
    // Write your code here — insert data at end, return head
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function test(a,op,data,e,tc,h){if(h===undefined)h=false;let ok=true;try{let hd=build(a);if(op==="head")hd=insertAtHead(hd,data);else hd=insertAtTail(hd,data);let cur=hd;for(let i=0;i<e.length;i++){if(cur===null||cur.val!==e[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(err){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let hd2=build(a);if(op==="head")hd2=insertAtHead(hd2,data);else hd2=insertAtTail(hd2,data);let c2=hd2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":op="+op+":data="+data+":exp="+JSON.stringify(e)+":got="+JSON.stringify(gl));}}
try{test([1,2,3],"tail",4,[1,2,3,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([],"head",7,[7],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([2,3],"head",1,[1,2,3],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5],"tail",6,[5,6],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],"tail",5,[5],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],"tail",6,[1,2,3,4,5,6],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([2,4,6,8],"head",0,[0,2,4,6,8],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1],"head",9,[9,1],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,20,30,40],"tail",50,[10,20,30,40,50],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([5,5,5],"head",5,[5,5,5,5],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* insertAtHead(Node* head, int data) {
    // Write your code here — insert data at front, return new head
    return head;
}
Node* insertAtTail(Node* head, int data) {
    // Write your code here — insert data at end, return head
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,const char* op,int data,int* e,int en,int tc,int hd){
    Node* h=build(a,n);
    if(op[0]=='h')h=insertAtHead(h,data);else h=insertAtTail(h,data);
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:op=%s:data=%d:exp=[",op,data);
        for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}
        printf("]:got=[");
        Node* g=build(a,n);
        if(op[0]=='h')g=insertAtHead(g,data);else g=insertAtTail(g,data);
        int fi=0;
        for(Node* p=g;p!=NULL;p=p->next){if(fi)printf(",");printf("%d",p->val);fi=1;}
        printf("]\\n");
    }
}
int main(){
    int a1[]={1,2,3};int e1[]={1,2,3,4};runTest(a1,3,"tail",4,e1,4,1,0);
    int e2[]={7};runTest(NULL,0,"head",7,e2,1,2,0);
    int a3[]={2,3};int e3[]={1,2,3};runTest(a3,2,"head",1,e3,3,3,0);
    int a4[]={5};int e4[]={5,6};runTest(a4,1,"tail",6,e4,2,4,0);
    int e5[]={5};runTest(NULL,0,"tail",5,e5,1,5,0);
    int a6[]={1,2,3,4,5};int e6[]={1,2,3,4,5,6};runTest(a6,5,"tail",6,e6,6,6,1);
    int a7[]={2,4,6,8};int e7[]={0,2,4,6,8};runTest(a7,4,"head",0,e7,5,7,1);
    int a8[]={1};int e8[]={9,1};runTest(a8,1,"head",9,e8,2,8,1);
    int a9[]={10,20,30,40};int e9[]={10,20,30,40,50};runTest(a9,4,"tail",50,e9,5,9,1);
    int a10[]={5,5,5};int e10[]={5,5,5,5};runTest(a10,3,"head",5,e10,4,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
