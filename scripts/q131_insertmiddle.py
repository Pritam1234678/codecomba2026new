"""
Insert in Middle of Linked List
=================================
Given the head of a singly linked list of length n and an integer data, insert
a new node with that value at the MIDDLE of the list and return the head.

Definition of middle: with 0-based indexing, the middle position is floor(n/2).
Examples:
  head = 1->2->4 (n=3), data=3  -> 1->2->3->4   (middle index floor(3/2)=1)
  head = 1->2->3->4 (n=4), data=9 -> 1->2->9->3->4 (middle index floor(4/2)=2)

Two-pointer technique: slow advances one step, fast advances two steps. When
fast reaches the end, slow is just before (or at) the insertion point — adjust
so the new node lands at index floor(n/2).

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
insertInMiddle(head, data), and traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Insert in Middle of Linked List"
desc=(
    "Given the head of a singly linked list and an integer data, insert a new "
    "node holding data at the MIDDLE of the list and return the head.\n\n"
    "With 0-based indexing the middle position is floor(n/2) where n is the "
    "current length.\n\n"
    "For example:\n"
    "head = 1->2->4 (n=3), data = 3  -> 1->2->3->4   (middle index floor(3/2)=1)\n"
    "head = 1->2->3->4 (n=4), data=9 -> 1->2->9->3->4 (middle index floor(4/2)=2)\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Use two pointers (slow and fast) or "
    "count the nodes first, then insert at the middle index. The harness "
    "builds the list, calls your insertInMiddle(head, data), and traverses the "
    "result to verify the order."
)
infmt="First line contains n and data. Second line contains n space-separated values."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="1 ≤ n ≤ 1000\n1 ≤ data, val ≤ 10^6"
e1="Input:\n3 3\n1 2 4\n\nOutput:\n1 2 3 4"
e2="Input:\n4 9\n1 2 3 4\n\nOutput:\n1 2 9 3 4"
e3="Input:\n1 7\n5\n\nOutput:\n5 7"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Insertion, Two Pointers",e1,e2,e3,"EASY",3.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Insertion, Two Pointers",e1,e2,e3))
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
    public Node insertInMiddle(Node head, int data) {
        // Write your code here — insert data at index floor(n/2)
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static void test(int[] a,int data,int[] e,int tc,boolean hd){Node h=new CodeCoder().insertInMiddle(build(a),data);boolean ok=true;for(int i=0;i<e.length;i++){if(h==null||h.val!=e[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node g=new CodeCoder().insertInMiddle(build(a),data);while(g!=null){gl.add(g.val);g=g.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":data="+data+":exp="+Arrays.toString(e)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{1,2,4},3,new int[]{1,2,3,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4},9,new int[]{1,2,9,3,4},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5},7,new int[]{5,7},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},9,new int[]{1,2,3,9,4,5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2},8,new int[]{1,8,2},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},7,new int[]{1,2,3,7,4,5,6},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50,60,70},5,new int[]{10,20,30,5,40,50,60,70},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{100,200,300,400},1,new int[]{100,200,1,300,400},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{9,9,9},9,new int[]{9,9,9,9},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1},2,new int[]{1,2},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* insertInMiddle(Node* head, int data) {
        // Write your code here — insert data at index floor(n/2)
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
 void test(vector<int> a,int data,vector<int> e,int tc,bool hd=false){Node* h=CodeCoder().insertInMiddle(build(a),data);bool ok=true;for(int i=0;i<(int)e.size();i++){if(h==NULL||h->val!=e[i]){ok=false;break;}h=h->next;}if(ok&&h!=NULL)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{Node* g=CodeCoder().insertInMiddle(build(a),data);cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:data="<<data<<":exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(Node* p=g;p!=NULL;p=p->next){if(p!=g)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({1,2,4},3,{1,2,3,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4},9,{1,2,9,3,4},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5},7,{5,7},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},9,{1,2,3,9,4,5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2},8,{1,8,2},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},7,{1,2,3,7,4,5,6},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({10,20,30,40,50,60,70},5,{10,20,30,5,40,50,60,70},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({100,200,300,400},1,{100,200,1,300,400},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({9,9,9},9,{9,9,9,9},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1},2,{1,2},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def insertInMiddle(self, head, data):
        # Write your code here — insert data at index floor(n/2)
        return head
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def test(a,data,e,tc,h=False):
    try:
        hd=CodeCoder().insertInMiddle(build(a),data);cur=hd;ok=True
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
            hd2=CodeCoder().insertInMiddle(build(a),data);c2=hd2
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:data={data}:exp={e}:got={gl}")
test([1,2,4],3,[1,2,3,4],1)
test([1,2,3,4],9,[1,2,9,3,4],2)
test([5],7,[5,7],3)
test([1,2,3,4,5],9,[1,2,3,9,4,5],4)
test([1,2],8,[1,8,2],5)
test([1,2,3,4,5,6],7,[1,2,3,7,4,5,6],6,True)
test([10,20,30,40,50,60,70],5,[10,20,30,40,5,50,60,70],7,True)
test([100,200,300,400],1,[100,200,1,300,400],8,True)
test([9,9,9],9,[9,9,9,9],9,True)
test([1],2,[1,2],10,True)'''

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
function insertInMiddle(head, data) {
    // Write your code here — insert data at index floor(n/2)
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function test(a,data,e,tc,h){if(h===undefined)h=false;let ok=true;try{let hd=insertInMiddle(build(a),data);let cur=hd;for(let i=0;i<e.length;i++){if(cur===null||cur.val!==e[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(err){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let hd2=insertInMiddle(build(a),data);let c2=hd2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":data="+data+":exp="+JSON.stringify(e)+":got="+JSON.stringify(gl));}}
try{test([1,2,4],3,[1,2,3,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4],9,[1,2,9,3,4],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5],7,[5,7],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],9,[1,2,3,9,4,5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2],8,[1,8,2],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6],7,[1,2,3,7,4,5,6],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,20,30,40,50,60,70],5,[10,20,30,40,5,50,60,70],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100,200,300,400],1,[100,200,1,300,400],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([9,9,9],9,[9,9,9,9],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1],2,[1,2],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* insertInMiddle(Node* head, int data) {
    // Write your code here — insert data at index floor(n/2)
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int data,int* e,int en,int tc,int hd){
    Node* h=insertInMiddle(build(a,n),data);
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:data=%d:exp=[",data);
        for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}
        printf("]:got=[");
        Node* g=insertInMiddle(build(a,n),data);int fi=0;
        for(Node* p=g;p!=NULL;p=p->next){if(fi)printf(",");printf("%d",p->val);fi=1;}
        printf("]\\n");
    }
}
int main(){
    int a1[]={1,2,4};int e1[]={1,2,3,4};runTest(a1,3,3,e1,4,1,0);
    int a2[]={1,2,3,4};int e2[]={1,2,9,3,4};runTest(a2,4,9,e2,5,2,0);
    int a3[]={5};int e3[]={5,7};runTest(a3,1,7,e3,2,3,0);
    int a4[]={1,2,3,4,5};int e4[]={1,2,3,9,4,5};runTest(a4,5,9,e4,6,4,0);
    int a5[]={1,2};int e5[]={1,8,2};runTest(a5,2,8,e5,3,5,0);
    int a6[]={1,2,3,4,5,6};int e6[]={1,2,3,7,4,5,6};runTest(a6,6,7,e6,7,6,1);
    int a7[]={10,20,30,40,50,60,70};int e7[]={10,20,30,40,5,50,60,70};runTest(a7,7,5,e7,8,7,1);
    int a8[]={100,200,300,400};int e8[]={100,200,1,300,400};runTest(a8,4,1,e8,5,8,1);
    int a9[]={9,9,9};int e9[]={9,9,9,9};runTest(a9,3,9,e9,4,9,1);
    int a10[]={1};int e10[]={1,2};runTest(a10,1,2,e10,2,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
