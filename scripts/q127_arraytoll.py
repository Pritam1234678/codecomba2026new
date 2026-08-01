"""
Array to Linked List
======================
Given an array arr of n integers, construct a singly linked list from it — the
first element becomes the head, the last element becomes the tail — and return
a reference to the HEAD node.

Examples:
  arr = [1,2,3,4,5] -> head node with val 1, next -> 2 -> 3 -> 4 -> 5 -> null
  arr = [1]         -> single node with val 1

The Node class is defined in the harness (hidden). See the comment inside
USER_CODE_START for its exact shape. Create one Node per element, link them in
order, and return the head. The harness walks the returned list and verifies
the values match the input array in order.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Array to Linked List"
desc=(
    "Given an integer array arr of length n, construct a singly linked list "
    "that contains the same elements in the same order — arr[0] becomes the "
    "head and arr[n-1] becomes the tail — and return a reference to the HEAD "
    "node.\n\n"
    "For example:\n"
    "arr = [1,2,3,4,5] -> head(1) -> 2 -> 3 -> 4 -> 5 -> null\n"
    "arr = [1]         -> a single node holding 1\n\n"
    "A Node type is pre-defined by the harness (hidden from you); its shape is "
    "documented in the starter comment. Create one node per element, link "
    "them in the given order, and return the head. The harness traverses the "
    "returned list and compares it with the input array."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="The harness traverses the returned linked list and prints PASS/FAIL based on the node values."
cons="1 ≤ n ≤ 1000\n1 ≤ arr[i] ≤ 10^6"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n1 2 3 4 5"
e2="Input:\n3\n10 20 30\n\nOutput:\n10 20 30"
e3="Input:\n1\n7\n\nOutput:\n7"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Construction",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

# If a problem with this title already exists, reuse it and refresh its snippets.
cur.execute("SELECT id FROM problems WHERE LOWER(title)=LOWER(%s) AND id<>%s ORDER BY id LIMIT 1",(title,pid))
existing=cur.fetchone()
if existing:
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    cur.execute("DELETE FROM problems WHERE id=%s",(pid,))
    pid=existing[0]
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))
    print(f"  (existing problem reused — pid={pid}, old snippets cleared)")
else:
    cur.execute("DELETE FROM code_snippets WHERE problem_id=%s",(pid,))

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
    public Node arrayToList(int[] arr) {
        // Write your code here — build the list and return its head
        return null;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int tc,boolean hd){Node h=new CodeCoder().arrayToList(a.clone());boolean ok=true;for(int i=0;i<a.length;i++){if(h==null||h.val!=a[i]){ok=false;break;}h=h.next;}if(ok&&h!=null)ok=false;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else{List<Integer> gl=new ArrayList<>();Node g=new CodeCoder().arrayToList(a.clone());while(g!=null){gl.add(g.val);g=g.next;}System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(a)+":got="+gl);}}
public static void main(String[] x){
try{test(new int[]{1,2,3,4,5},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10,20,30},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{7},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{100,200},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{9,8,7,6,5,4,3,2,1,0},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,1,1,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-5,-4,-3},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{42},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,4,6,8,10,12,14,16,18,20},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    Node* arrayToList(vector<int>& arr) {
        // Write your code here — build the list and return its head
        return NULL;
    }
};
// USER_CODE_END

void test(vector<int> a,int tc,bool hd=false){Node* h=CodeCoder().arrayToList(a);bool ok=true;for(int i=0;i<(int)a.size();i++){if(h==NULL||h->val!=a[i]){ok=false;break;}h=h->next;}if(ok&&h!=NULL)ok=false;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{Node* g=CodeCoder().arrayToList(a);cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:got=[";for(Node* p=g;p!=NULL;p=p->next){if(p!=g)cout<<",";cout<<p->val;}cout<<"]\\n";}}
int main(){
try{test({1,2,3,4,5},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10,20,30},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({7},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({5,4,3,2,1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({100,200},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({9,8,7,6,5,4,3,2,1,0},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,1,1,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-5,-4,-3},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({42},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,4,6,8,10,12,14,16,18,20},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
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
    def arrayToList(self, arr):
        # Write your code here — build the list and return its head
        return None
# USER_CODE_END
def test(a,tc,h=False):
    try:
        head=CodeCoder().arrayToList(list(a));cur=head;ok=True
        for v in a:
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
            hd2=CodeCoder().arrayToList(list(a));c2=hd2
            while c2: gl.append(c2.val); c2=c2.next
        except: pass
        print(f"TC:{tc}:FAIL:arr={a}:exp={a}:got={gl}")
test([1,2,3,4,5],1)
test([10,20,30],2)
test([7],3)
test([5,4,3,2,1],4)
test([100,200],5)
test([9,8,7,6,5,4,3,2,1,0],6,True)
test([1,1,1,1],7,True)
test([-5,-4,-3],8,True)
test([42],9,True)
test([2,4,6,8,10,12,14,16,18,20],10,True)'''

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
function arrayToList(arr) {
    // Write your code here — build the list and return its head
    return null;
}
// USER_CODE_END
function test(a,tc,h){if(h===undefined)h=false;let ok=true;try{let cur=arrayToList(a.slice());for(let i=0;i<a.length;i++){if(cur===null||cur.val!==a[i]){ok=false;break;}cur=cur.next;}if(cur!==null)ok=false;}catch(e){ok=false;}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else{let gl=[];try{let hd2=arrayToList(a.slice());let c2=hd2;while(c2){gl.push(c2.val);c2=c2.next;}}catch(err){}console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(a)+":got="+JSON.stringify(gl));}}
try{test([1,2,3,4,5],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10,20,30],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([7],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([5,4,3,2,1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([100,200],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([9,8,7,6,5,4,3,2,1,0],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,1,1,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-5,-4,-3],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([42],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,4,6,8,10,12,14,16,18,20],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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
Node* arrayToList(int* arr,int n) {
    // Write your code here — build the list and return its head
    return NULL;
}
// USER_CODE_END

void runTest(int* a,int n,int tc,int hd){
    Node* h=arrayToList(a,n);
    int ok=1;
    Node* cur=h;
    for(int i=0;i<n;i++){if(cur==NULL||cur->val!=a[i]){ok=0;break;}cur=cur->next;}
    if(ok&&cur!=NULL)ok=0;
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{
        printf("TC:%d:FAIL:arr=[",tc);
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:exp=[");
        for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}
        printf("]:got=[");
        Node* g=arrayToList(a,n);int fi=0;
        for(Node* p=g;p!=NULL;p=p->next){if(fi)printf(",");printf("%d",p->val);fi=1;}
        printf("]\\n");
    }
}
int main(){
    int t1[]={1,2,3,4,5};runTest(t1,5,1,0);
    int t2[]={10,20,30};runTest(t2,3,2,0);
    int t3[]={7};runTest(t3,1,3,0);
    int t4[]={5,4,3,2,1};runTest(t4,5,4,0);
    int t5[]={100,200};runTest(t5,2,5,0);
    int t6[]={9,8,7,6,5,4,3,2,1,0};runTest(t6,10,6,1);
    int t7[]={1,1,1,1};runTest(t7,4,7,1);
    int t8[]={-5,-4,-3};runTest(t8,3,8,1);
    int t9[]={42};runTest(t9,1,9,1);
    int t10[]={2,4,6,8,10,12,14,16,18,20};runTest(t10,10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
