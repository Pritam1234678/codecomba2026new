"""
Remove Linked List Elements (By Value)
========================================
Given the head of a singly linked list and an integer target, remove ALL nodes
whose value equals target, and return the (possibly new) head.

Examples:
  head = 1->2->6->3->4->5->6, target = 6 -> 1->2->3->4->5
  head = 7->7->7->7, target = 7          -> null (empty list)
  head = 1->2->3, target = 5             -> 1->2->3 (unchanged)

Use a dummy node before the head so the leading removals are easy, then walk
with a prev pointer and unlink every node whose val matches target.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
removeElements(head, target), and traverses the result to verify the order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Linked List Elements"
desc=(
    "Given the head of a singly linked list and an integer target, remove ALL "
    "nodes whose value equals target and return the head of the updated list "
    "(the head may change if the first nodes are removed).\n\n"
    "For example:\n"
    "head = 1->2->6->3->4->5->6, target = 6 -> 1->2->3->4->5\n"
    "head = 7->7->7->7, target = 7          -> null (every node removed)\n"
    "head = 1->2->3, target = 5             -> 1->2->3 (nothing removed)\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. A clean approach uses a dummy node "
    "placed before the head, then walks the list with a prev pointer, unlinking "
    "every node whose val matches target. Return dummy.next."
)
infmt="First line contains n and target. Second line contains n space-separated values."
outfmt="The harness traverses the returned list and prints PASS/FAIL based on the resulting order."
cons="0 ≤ n ≤ 1000\n1 ≤ val, target ≤ 10^6"
e1="Input:\n7 6\n1 2 6 3 4 5 6\n\nOutput:\n1 2 3 4 5"
e2="Input:\n4 7\n7 7 7 7\n\nOutput:\n(empty)"
e3="Input:\n3 5\n1 2 3\n\nOutput:\n1 2 3"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Deletion",e1,e2,e3,"EASY",3.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Deletion",e1,e2,e3))
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
    public Node removeElements(Node head, int target) {
        // Write your code here — remove all nodes with val == target
        return head;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static void test(int[] a,int t,int[] e,int tc,boolean hd){Node h=new CodeCoder().removeElements(build(a),t);boolean ok=true;for(int i=0;i<e.length;i++){if(h==null||h.val!=e[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node g=new CodeCoder().removeElements(build(a),t);while(g!=null){gl.add(g.val);g=g.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":target="+t+":exp="+Arrays.toString(e)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{1,2,6,3,4,5,6},6,new int[]{1,2,3,4,5},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{7,7,7,7},7,new int[]{},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2,3},5,new int[]{1,2,3},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{6,1,2,3},6,new int[]{1,2,3},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},9,new int[]{},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,2,2,3},2,new int[]{1,3},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{6,6,1},6,new int[]{1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{5},5,new int[]{},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},10,new int[]{1,2,3,4,5,6,7,8,9},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,2,2,1,2,2,2},2,new int[]{1},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* removeElements(Node* head, int target) {
        // Write your code here — remove all nodes with val == target
        return head;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
 void test(vector<int> a,int t,vector<int> e,int tc,bool hd=false){Node* h=CodeCoder().removeElements(build(a),t);bool ok=true;for(int i=0;i<(int)e.size();i++){if(h==NULL||h->val!=e[i]){ok=false;break;}h=h->next;}if(ok&&h!=NULL)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{Node* g=CodeCoder().removeElements(build(a),t);cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:target="<<t<<":exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(Node* p=g;p!=NULL;p=p->next){if(p!=g)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({1,2,6,3,4,5,6},6,{1,2,3,4,5},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({7,7,7,7},7,{},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2,3},5,{1,2,3},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({6,1,2,3},6,{1,2,3},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},9,{},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,2,2,3},2,{1,3},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({6,6,1},6,{1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({5},5,{},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},10,{1,2,3,4,5,6,7,8,9},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,2,2,1,2,2,2},2,{1},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def removeElements(self, head, target):
        # Write your code here — remove all nodes with val == target
        return head
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def test(a,t,e,tc,h=False):
    try:
        hd=CodeCoder().removeElements(build(a),t);cur=hd;ok=True
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
            hd2=CodeCoder().removeElements(build(a),t);c2=hd2
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:target={t}:exp={e}:got={gl}")
test([1,2,6,3,4,5,6],6,[1,2,3,4,5],1)
test([7,7,7,7],7,[],2)
test([1,2,3],5,[1,2,3],3)
test([6,1,2,3],6,[1,2,3],4)
test([],9,[],5)
test([1,2,2,2,3],2,[1,3],6,True)
test([6,6,1],6,[1],7,True)
test([5],5,[],8,True)
test([1,2,3,4,5,6,7,8,9,10],10,[1,2,3,4,5,6,7,8,9],9,True)
test([2,2,2,1,2,2,2],2,[1],10,True)'''

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
function removeElements(head, target) {
    // Write your code here — remove all nodes with val == target
    return head;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function test(a,t,e,tc,h){if(h===undefined)h=false;let ok=true;try{let hd=removeElements(build(a),t);let cur=hd;for(let i=0;i<e.length;i++){if(cur===null||cur.val!==e[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(err){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let hd2=removeElements(build(a),t);let c2=hd2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":target="+t+":exp="+JSON.stringify(e)+":got="+JSON.stringify(gl));}}
try{test([1,2,6,3,4,5,6],6,[1,2,3,4,5],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([7,7,7,7],7,[],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2,3],5,[1,2,3],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([6,1,2,3],6,[1,2,3],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],9,[],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,2,2,3],2,[1,3],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([6,6,1],6,[1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([5],5,[],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],10,[1,2,3,4,5,6,7,8,9],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,2,2,1,2,2,2],2,[1],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* removeElements(Node* head, int target) {
    // Write your code here — remove all nodes with val == target
    return head;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int t,int* e,int en,int tc,int hd){
    Node* h=removeElements(build(a,n),t);
    int ok=1;Node* cur=h;
    for(int i=0;i<en;i++){if(cur==NULL||cur->val!=e[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:target=%d:exp=[",t);
        for(int i=0;i<en;i++){if(i)printf(",");printf("%d",e[i]);}
        printf("]:got=[");
        Node* g=removeElements(build(a,n),t);int fi=0;
        for(Node* p=g;p!=NULL;p=p->next){if(fi)printf(",");printf("%d",p->val);fi=1;}
        printf("]\\n");
    }
}
int main(){
    int a1[]={1,2,6,3,4,5,6};int e1[]={1,2,3,4,5};runTest(a1,7,6,e1,5,1,0);
    int a2[]={7,7,7,7};runTest(a2,4,7,NULL,0,2,0);
    int a3[]={1,2,3};int e3[]={1,2,3};runTest(a3,3,5,e3,3,3,0);
    int a4[]={6,1,2,3};int e4[]={1,2,3};runTest(a4,4,6,e4,3,4,0);
    runTest(NULL,0,9,NULL,0,5,0);
    int a6[]={1,2,2,2,3};int e6[]={1,3};runTest(a6,5,2,e6,2,6,1);
    int a7[]={6,6,1};int e7[]={1};runTest(a7,3,6,e7,1,7,1);
    int a8[]={5};runTest(a8,1,5,NULL,0,8,1);
    int a9[]={1,2,3,4,5,6,7,8,9,10};int e9[]={1,2,3,4,5,6,7,8,9};runTest(a9,10,10,e9,9,9,1);
    int a10[]={2,2,2,1,2,2,2};int e10[]={1};runTest(a10,7,2,e10,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
