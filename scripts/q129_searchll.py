"""
Search in a Linked List
=========================
Given the head of a singly linked list and an integer target, return 1 if the
target value is present in the list, otherwise return 0.

Examples:
  head = 1 -> 2 -> 3 -> 4 -> 5, target = 4  -> 1
  head = 1 -> 2 -> 3 -> 4 -> 5, target = 9  -> 0

Traverse from the head, comparing each node's val with the target.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list, calls your
searchList(head, target), and checks the returned 0/1.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Search in a Linked List"
desc=(
    "Given the head of a singly linked list and an integer target, return 1 if "
    "the target value exists somewhere in the list, otherwise return 0.\n\n"
    "For example:\n"
    "head = 1 -> 2 -> 3 -> 4 -> 5, target = 4 -> 1\n"
    "head = 1 -> 2 -> 3 -> 4 -> 5, target = 9 -> 0\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Walk the list from the head, comparing "
    "each node's value with the target; return 1 on the first match and 0 if "
    "you reach the end without finding it."
)
infmt="First line contains n and target. Second line contains n space-separated values."
outfmt="Print 1 if the target is present in the list, else 0."
cons="0 ≤ n ≤ 1000\n1 ≤ val, target ≤ 10^6"
e1="Input:\n5 4\n1 2 3 4 5\n\nOutput:\n1"
e2="Input:\n5 9\n1 2 3 4 5\n\nOutput:\n0"
e3="Input:\n0 7\n\nOutput:\n0"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Search, Traversal",e1,e2,e3,"EASY",3.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Search, Traversal",e1,e2,e3))
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
    public int searchList(Node head, int target) {
        // Write your code here — return 1 if found else 0
        return 0;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a){Node d=new Node(0),c=d;for(int v:a){c.next=new Node(v);c=c.next;}return d.next;}
static void test(int[] a,int t,int e,int tc,boolean hd){int g=new CodeCoder().searchList(build(a),t);if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":target="+t+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test(new int[]{1,2,3,4,5},4,1,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},9,0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{},7,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5},5,1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{10,20,30},20,1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},10,1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},11,0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,-2,-3},-2,1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{100,200},150,0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{3,3,3},3,1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    int searchList(Node* head, int target) {
        // Write your code here — return 1 if found else 0
        return 0;
    }
};
// USER_CODE_END

Node* build(vector<int>& a){Node d(0),*c=&d;for(int v:a){c->next=new Node(v);c=c->next;}return d.next;}
void test(vector<int> a,int t,int e,int tc,bool hd=false){int g=CodeCoder().searchList(build(a),t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:target="<<t<<":exp="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({1,2,3,4,5},4,1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},9,0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},7,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5},5,1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({10,20,30},20,1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},10,1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},11,0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,-2,-3},-2,1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({100,200},150,0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({3,3,3},3,1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def searchList(self, head, target):
        # Write your code here — return 1 if found else 0
        return 0
# USER_CODE_END
def build(a):
    d=Node(0);c=d
    for v in a:
        c.next=Node(v);c=c.next
    return d.next
def test(a,t,e,tc,h=False):
    try:
        g=CodeCoder().searchList(build(a),t);ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:target={t}:exp={e}:got={g}"))
test([1,2,3,4,5],4,1,1)
test([1,2,3,4,5],9,0,2)
test([],7,0,3)
test([5],5,1,4)
test([10,20,30],20,1,5)
test([1,2,3,4,5,6,7,8,9,10],10,1,6,True)
test([1,2,3,4,5,6,7,8,9,10],11,0,7,True)
test([-1,-2,-3],-2,1,8,True)
test([100,200],150,0,9,True)
test([3,3,3],3,1,10,True)'''

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
function searchList(head, target) {
    // Write your code here — return 1 if found else 0
    return 0;
}
// USER_CODE_END
function build(a){const d=new Node(0);let c=d;for(const v of a){c.next=new Node(v);c=c.next;}return d.next;}
function test(a,t,e,tc,h){if(h===undefined)h=false;let g,ok=false;try{g=searchList(build(a),t);ok=(g===e);}catch(err){g="EXC";}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":target="+t+":exp="+e+":got="+g);}
try{test([1,2,3,4,5],4,1,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],9,0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],7,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5],5,1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([10,20,30],20,1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],10,1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],11,0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,-2,-3],-2,1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([100,200],150,0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([3,3,3],3,1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
int searchList(Node* head, int target) {
    // Write your code here — return 1 if found else 0
    return 0;
}
// USER_CODE_END

Node* build(int* a,int n){Node d;d.val=0;d.next=NULL;Node* c=&d;for(int i=0;i<n;i++){Node* nd=(Node*)malloc(sizeof(Node));nd->val=a[i];nd->next=NULL;c->next=nd;c=nd;}return d.next;}
void runTest(int* a,int n,int t,int e,int tc,int hd){
    int g=searchList(build(a,n),t);
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:target=%d:exp=%d:got=%d\\n",t,e,g);}
}
int main(){
    int t1[]={1,2,3,4,5};runTest(t1,5,4,1,1,0);
    int t2[]={1,2,3,4,5};runTest(t2,5,9,0,2,0);
    runTest(NULL,0,7,0,3,0);
    int t4[]={5};runTest(t4,1,5,1,4,0);
    int t5[]={10,20,30};runTest(t5,3,20,1,5,0);
    int t6[]={1,2,3,4,5,6,7,8,9,10};runTest(t6,10,10,1,6,1);
    int t7[]={1,2,3,4,5,6,7,8,9,10};runTest(t7,10,11,0,7,1);
    int t8[]={-1,-2,-3};runTest(t8,3,-2,1,8,1);
    int t9[]={100,200};runTest(t9,2,150,0,9,1);
    int t10[]={3,3,3};runTest(t10,3,3,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
