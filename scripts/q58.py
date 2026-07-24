"""
Remove Nth Node From End of List
==================================
Given head of a linked list, remove the nth node from the end of the list
and return the head.

Examples:
  head = [1,2,3,4,5], n = 2 → [1,2,3,5]
  head = [1], n = 1 → []
  head = [1,2], n = 1 → [1]

Approach: Use fast-slow pointer with a dummy node.
Fast moves n steps ahead, then slow & fast move together.
When fast reaches end, slow's next is the node to remove.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Remove Nth Node From End of List"
desc=(
    "Given the head of a linked list, remove the nth node from the end "
    "of the list and return the head.\n\n"
    "For example:\n"
    "head = [1,2,3,4,5], n = 2 → remove the second last node (value 4) → [1,2,3,5]\n"
    "head = [1], n = 1 → remove the only node → []\n"
    "head = [1,2], n = 1 → remove last node (value 2) → [1]\n\n"
    "Use a dummy node and two pointers. Move fast n steps ahead, "
    "then move both until fast hits end. Remove slow's next node."
)
infmt="First line contains n and k.\nSecond line contains n space-separated integers.\nk = position from end to remove (1-indexed)."
outfmt="Print the resulting list as space-separated integers."
cons="1 ≤ n ≤ 30\n1 ≤ k ≤ n\n0 ≤ Node.val ≤ 100"
e1="Input:\n5 2\n1 2 3 4 5\n\nOutput:\n1 2 3 5"
e2="Input:\n1 1\n1\n\nOutput:\n\n(empty line)"
e3="Input:\n2 1\n1 2\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode { int val; ListNode next; ListNode(int x) { val = x; } }

// USER_CODE_START
class CodeCoder {
    public ListNode removeNthFromEnd(ListNode head, int n) {
        return head;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a) {
        if (a.length==0) return null;
        ListNode d=new ListNode(0),c=d;
        for (int v:a){c.next=new ListNode(v);c=c.next;}
        return d.next;
    }
    static int[] ser(ListNode h){
        List<Integer> l=new ArrayList<>();
        while(h!=null){l.add(h.val);h=h.next;}
        return l.stream().mapToInt(i->i).toArray();
    }
    static void test(int[] a,int k,int[] e,int tc,boolean h){
        int[] g=ser(new CodeCoder().removeNthFromEnd(build(a),k));
        if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:got="+Arrays.toString(g)+":exp="+Arrays.toString(e));
    }
    public static void main(String[] a){
        try{test(new int[]{1,2,3,4,5},2,new int[]{1,2,3,5},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new int[]{1},1,new int[]{},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new int[]{1,2},1,new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new int[]{1,2,3},3,new int[]{2,3},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new int[]{10,20,30,40},4,new int[]{20,30,40},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new int[]{5,5,5,5,5},2,new int[]{5,5,5,5},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new int[]{1,2,3,4,5,6,7},7,new int[]{2,3,4,5,6,7},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new int[]{100,200},2,new int[]{200},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new int[]{-5,-4,-3,-2,-1},1,new int[]{-5,-4,-3,-2},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new int[]{0,1,2,3,4,5,6,7,8,9},5,new int[]{0,1,2,3,4,6,7,8,9},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode{int val;ListNode* next;ListNode(int x):val(x),next(NULL){}};

// USER_CODE_START
class CodeCoder{public:ListNode* removeNthFromEnd(ListNode* h,int n){return h;}};
// USER_CODE_END

ListNode* build(vector<int>& a){
    ListNode d(0),*c=&d;
    for(int v:a){c->next=new ListNode(v);c=c->next;}
    return d.next;
}
vector<int> ser(ListNode* h){
    vector<int> r;while(h){r.push_back(h->val);h=h->next;}return r;
}
void test(vector<int> a,int k,vector<int> e,int tc,bool h=false){
    auto g=ser(CodeCoder().removeNthFromEnd(build(a),k));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}
}
int main(){
try{test({1,2,3,4,5},2,{1,2,3,5},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1},1,{},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2},1,{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3},3,{2,3},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({10,20,30,40},4,{20,30,40},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({5,5,5,5,5},2,{5,5,5,5},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7},7,{2,3,4,5,6,7},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({100,200},2,{200},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-5,-4,-3,-2,-1},1,{-5,-4,-3,-2},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({0,1,2,3,4,5,6,7,8,9},5,{0,1,2,3,4,6,7,8,9},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def removeNthFromEnd(self, head, n):
        return head
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    d=ListNode(0);c=d
    for v in a:c.next=ListNode(v);c=c.next
    return d.next
def ser(h):
    r=[]
    while h:r.append(h.val);h=h.next
    return r

def test(a,k,e,tc,h=False):
    g=ser(CodeCoder().removeNthFromEnd(build(a),k))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={g}:exp={e}")

try:test([1,2,3,4,5],2,[1,2,3,5],1)
except:print("TC:1:FAIL:hidden")
try:test([1],1,[],2)
except:print("TC:2:FAIL:hidden")
try:test([1,2],1,[1],3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3],3,[2,3],4)
except:print("TC:4:FAIL:hidden")
try:test([10,20,30,40],4,[20,30,40],5)
except:print("TC:5:FAIL:hidden")
try:test([5,5,5,5,5],2,[5,5,5,5],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5,6,7],7,[2,3,4,5,6,7],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([100,200],2,[200],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-5,-4,-3,-2,-1],1,[-5,-4,-3,-2],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0,1,2,3,4,5,6,7,8,9],5,[0,1,2,3,4,6,7,8,9],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function removeNthFromEnd(head, n) { return head; }
// USER_CODE_END

class ListNode {
    constructor(val, next) { this.val = val; this.next = next || null; }
}
function build(a){
    let d=new ListNode(0),c=d;
    for(let v of a){c.next=new ListNode(v);c=c.next;}
    return d.next;
}
function ser(h){
    let r=[];
    while(h){r.push(h.val);h=h.next;}
    return r;
}
function test(a,k,e,tc,h){
    if(h===undefined)h=false;
    const g=ser(removeNthFromEnd(build(a),k));
    const gs=JSON.stringify(g),es=JSON.stringify(e);
    if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);
}
try{test([1,2,3,4,5],2,[1,2,3,5],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1],1,[],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2],1,[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3],3,[2,3],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([10,20,30,40],4,[20,30,40],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([5,5,5,5,5],2,[5,5,5,5],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],7,[2,3,4,5,6,7],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([100,200],2,[200],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-5,-4,-3,-2,-1],1,[-5,-4,-3,-2],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0,1,2,3,4,5,6,7,8,9],5,[0,1,2,3,4,6,7,8,9],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct ListNode{int val;struct ListNode* next;};

// USER_CODE_START
struct ListNode* removeNthFromEnd(struct ListNode* h,int n){return h;}
// USER_CODE_END

struct ListNode* build(int* a,int n){
    if(!n)return NULL;
    struct ListNode* h=malloc(sizeof(struct ListNode));
    h->val=a[0];h->next=NULL;
    struct ListNode* c=h;
    for(int i=1;i<n;i++){c->next=malloc(sizeof(struct ListNode));c=c->next;c->val=a[i];c->next=NULL;}
    return h;
}
int* ser(struct ListNode* h,int* n){
    *n=0;struct ListNode* c=h;
    while(c){(*n)++;c=c->next;}
    int* r=malloc(*n*sizeof(int));c=h;
    for(int i=0;i<*n;i++){r[i]=c->val;c=c->next;}
    return r;
}
int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int* a,int an,int k,int* e,int en,int tc,int h){
    struct ListNode* head=build(a,an);
    struct ListNode* res=removeNthFromEnd(head,k);
    int gn;int* g=ser(res,&gn);
    if(gn==en&&arrEq(g,e,gn)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}
    free(g);
}
int main(){
int t1[]={1,2,3,4,5},e1[]={1,2,3,5};run(t1,5,2,e1,4,1,0);
int t2[]={1};run(t2,1,1,NULL,0,2,0);
int t3[]={1,2},e3[]={1};run(t3,2,1,e3,1,3,0);
int t4[]={1,2,3},e4[]={2,3};run(t4,3,3,e4,2,4,0);
int t5[]={10,20,30,40},e5[]={20,30,40};run(t5,4,4,e5,3,5,0);
int t6[]={5,5,5,5,5},e6[]={5,5,5,5};run(t6,5,2,e6,4,6,1);
int t7[]={1,2,3,4,5,6,7},e7[]={2,3,4,5,6,7};run(t7,7,7,e7,6,7,1);
int t8[]={100,200},e8[]={200};run(t8,2,2,e8,1,8,1);
int t9[]={-5,-4,-3,-2,-1},e9[]={-5,-4,-3,-2};run(t9,5,1,e9,4,9,1);
int t10[]={0,1,2,3,4,5,6,7,8,9},e10[]={0,1,2,3,4,6,7,8,9};run(t10,10,5,e10,9,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
