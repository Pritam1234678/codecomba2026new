"""
Length of Loop in Linked List
===============================
Given the head of a singly linked list that MAY contain a loop (cycle), find
and return the number of nodes in the loop. If the list has no loop, return 0.

Examples:
  head = 1->2->3->4->5, 5->2 (loop from node 5 back to node 2) -> 3
  head = 1->2->3, no loop -> 0

Detect the cycle with the fast/slow pointer technique, then count the nodes in
the loop by walking around it until you return to the meeting point.

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. The harness builds the list (optionally
closing the loop: the LAST node's next points to the node at a given index),
calls your loopLength(head), and checks the returned count.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Length of Loop in Linked List"
desc=(
    "Given the head of a singly linked list that MAY contain a loop, return "
    "the number of nodes that form the loop. If the list has no loop, return "
    "0.\n\n"
    "For example:\n"
    "head = 1->2->3->4->5 with 5->2 (the last node loops back to node 2) -> 3 "
    "(nodes 2,3,4 form the loop)\n"
    "head = 1->2->3 with no loop -> 0\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. The harness builds the list and may "
    "connect the LAST node's next pointer to the node at a given 0-based index "
    "to create the loop. Use the fast/slow pointer technique to detect the "
    "cycle, then count how many steps it takes to walk around the loop and "
    "return to the meeting point. Return 0 when no cycle exists."
)
infmt="First line contains n and pos (the 0-based index the last node links to; -1 means no loop). Second line contains n space-separated values."
outfmt="Print the number of nodes in the loop, or 0 if there is no loop."
cons="1 ≤ n ≤ 1000\n-1 ≤ pos < n\nValues are positive integers."
e1="Input:\n5 2\n1 2 3 4 5\n\nOutput:\n3"
e2="Input:\n5 -1\n1 2 3 4 5\n\nOutput:\n0"
e3="Input:\n1 0\n1\n\nOutput:\n1"

cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) ORDER BY id LIMIT 1",(title,))
row=cur.fetchone()
if row:
    pid=row[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("UPDATE problems SET description=%s,input_format=%s,output_format=%s,constraints=%s,topics=%s,example1=%s,example2=%s,example3=%s,level=%s,time_limit=%s,memory_limit=%s WHERE id=%s",
    (desc,infmt,outfmt,cons,"Linked List, Cycle Detection, Two Pointers",e1,e2,e3,"MEDIUM",5.0,256,pid))
    print(f"Problem: {title} (existing pid={pid} — refreshing)")
else:
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
    (title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Cycle Detection, Two Pointers",e1,e2,e3))
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
    public int loopLength(Node head) {
        // Write your code here — return loop length, or 0 if no loop
        return 0;
    }
}
// USER_CODE_END

public class Main {
static Node build(int[] a,int pos){if(a.length==0)return null;Node[] arr=new Node[a.length];for(int i=0;i<a.length;i++)arr[i]=new Node(a[i]);for(int i=0;i<a.length-1;i++)arr[i].next=arr[i+1];if(pos>=0)arr[a.length-1].next=arr[pos];return arr[0];}
static void test(int[] a,int pos,int e,int tc,boolean hd){int g=new CodeCoder().loopLength(build(a,pos));if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":pos="+pos+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test(new int[]{1,2,3,4,5},2,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},-1,0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},0,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3},0,3,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{10,20,30,40},1,3,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},4,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2},1,1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,10},5,5,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{5,6,7},-1,0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    int loopLength(Node* head) {
        // Write your code here — return loop length, or 0 if no loop
        return 0;
    }
};
// USER_CODE_END

Node* build(vector<int>& a,int pos){if(a.empty())return NULL;vector<Node*> arr;for(int v:a)arr.push_back(new Node(v));for(int i=0;i+1<(int)a.size();i++)arr[i]->next=arr[i+1];if(pos>=0)arr[a.size()-1]->next=arr[pos];return arr[0];}
void test(vector<int> a,int pos,int e,int tc,bool hd=false){int g=CodeCoder().loopLength(build(a,pos));if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:pos="<<pos<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,3,4,5},2,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},-1,0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},0,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3},0,3,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({10,20,30,40},1,3,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5},4,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2},1,1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,10},5,5,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({5,6,7},-1,0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def loopLength(self, head):
        # Write your code here — return loop length, or 0 if no loop
        return 0
# USER_CODE_END
def build(a,pos):
    if not a: return None
    arr=[Node(v) for v in a]
    for i in range(len(a)-1): arr[i].next=arr[i+1]
    if pos>=0: arr[-1].next=arr[pos]
    return arr[0]
def test(a,pos,e,tc,h=False):
    try:
        g=CodeCoder().loopLength(build(a,pos));ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:pos={pos}:exp={e}:got={g}"))
test([1,2,3,4,5],2,3,1)
test([1,2,3,4,5],-1,0,2)
test([1],0,1,3)
test([1,2,3],0,3,4)
test([10,20,30,40],1,3,5)
test([1,2,3,4,5],1,4,6,True)
test([1,2,3,4,5],4,1,7,True)
test([1,2],1,1,8,True)
test([1,2,3,4,5,6,7,8,9,10],5,5,9,True)
test([5,6,7],-1,0,10,True)'''

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
function loopLength(head) {
    // Write your code here — return loop length, or 0 if no loop
    return 0;
}
// USER_CODE_END
function build(a,pos){if(a.length===0)return null;const arr=a.map(v=>new Node(v));for(let i=0;i<a.length-1;i++)arr[i].next=arr[i+1];if(pos>=0)arr[a.length-1].next=arr[pos];return arr[0];}
function test(a,pos,e,tc,h){if(h===undefined)h=false;let g,ok=false;try{g=loopLength(build(a,pos));ok=(g===e);}catch(err){g="EXC";}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":pos="+pos+":exp="+e+":got="+g);}
try{test([1,2,3,4,5],2,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],-1,0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],0,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3],0,3,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([10,20,30,40],1,3,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],1,4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5],4,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2],1,1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10],5,5,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([5,6,7],-1,0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
int loopLength(Node* head) {
    // Write your code here — return loop length, or 0 if no loop
    return 0;
}
// USER_CODE_END

Node* build(int* a,int n,int pos){Node* arr[1001];for(int i=0;i<n;i++){arr[i]=(Node*)malloc(sizeof(Node));arr[i]->val=a[i];arr[i]->next=NULL;}for(int i=0;i<n-1;i++)arr[i]->next=arr[i+1];if(pos>=0)arr[n-1]->next=arr[pos];return n>0?arr[0]:NULL;}
void runTest(int* a,int n,int pos,int e,int tc,int hd){
    int g=loopLength(build(a,n,pos));
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:pos=%d:exp=%d:got=%d\\n",tc,pos,e,g);}
}
int main(){
    int t1[]={1,2,3,4,5};runTest(t1,5,2,3,1,0);
    int t2[]={1,2,3,4,5};runTest(t2,5,-1,0,2,0);
    int t3[]={1};runTest(t3,1,0,1,3,0);
    int t4[]={1,2,3};runTest(t4,3,0,3,4,0);
    int t5[]={10,20,30,40};runTest(t5,4,1,3,5,0);
    int t6[]={1,2,3,4,5};runTest(t6,5,1,4,6,1);
    int t7[]={1,2,3,4,5};runTest(t7,5,4,1,7,1);
    int t8[]={1,2};runTest(t8,2,1,1,8,1);
    int t9[]={1,2,3,4,5,6,7,8,9,10};runTest(t9,10,5,5,9,1);
    int t10[]={5,6,7};runTest(t10,3,-1,0,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
