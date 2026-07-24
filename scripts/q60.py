"""
Intersection of Two Linked Lists
==================================
Given two singly linked lists, find the node at which they intersect.
If they don't intersect, return null.

Approach: Two-pointer. Each pointer traverses both lists.
When one reaches end, it continues from the other list's head.
They meet at intersection or both become null.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Intersection of Two Linked Lists"
desc=(
    "Given the heads of two singly linked lists headA and headB, "
    "return the node at which the two lists intersect. If the two lists "
    "have no intersection, return null.\n\n"
    "The lists intersect when they share a common suffix (same nodes by reference, "
    "not by value). The intersection node is the first node they share.\n\n"
    "For example:\n"
    "A: 4 → 1 ↘\n"
    "          8 → 4 → 5\n"
    "B: 5 → 6 → 1 ↗\n"
    "They intersect at node with value 8.\n\n"
    "Two-pointer approach: each pointer traverses both lists. When one reaches "
    "the end, switch to the other list. They meet at the intersection."
)
infmt=("First line contains nA (size of list A before intersection).\n"
       "Second line contains nA space-separated integers (A's distinct prefix).\n"
       "Third line contains nB (size of list B before intersection).\n"
       "Fourth line contains nB space-separated integers (B's distinct prefix).\n"
       "Fifth line contains m (size of common suffix).\n"
       "Sixth line contains m space-separated integers (common suffix).")
outfmt="Print the value of the intersection node, or -1 if no intersection."
cons="1 ≤ total nodes ≤ 10^4\n1 ≤ Node.val ≤ 10^5"
e1="Input:\n2\n4 1\n3\n5 6 1\n3\n8 4 5\n\nOutput:\n8"
e2="Input:\n3\n2 6 4\n1\n1 5\n0\n\n\nOutput:\n-1"
e3="Input:\n0\n\n0\n\n3\n1 2 3\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Two Pointers, Hash Table",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode{int val;ListNode next;ListNode(int x){val=x;}}

// USER_CODE_START
class CodeCoder{
    public ListNode getIntersectionNode(ListNode a,ListNode b){
        return null;
    }
}
// USER_CODE_END

public class Main{
    static ListNode build(int[] prefix,int[] suffix){
        ListNode d=new ListNode(0),c=d;
        for(int v:prefix){c.next=new ListNode(v);c=c.next;}
        ListNode suffixHead=null;
        if(suffix.length>0){
            suffixHead=new ListNode(suffix[0]);
            c.next=suffixHead;c=c.next;
            for(int i=1;i<suffix.length;i++){c.next=new ListNode(suffix[i]);c=c.next;}
        }
        return d.next;
    }
    static ListNode buildB(int[] prefix,ListNode suffixHead){
        ListNode d=new ListNode(0),c=d;
        for(int v:prefix){c.next=new ListNode(v);c=c.next;}
        c.next=suffixHead;
        return d.next;
    }
    static void test(int[] a,int[] b,int[] suf,int exp,int tc,boolean h){
        ListNode s=null;
        if(suf.length>0){s=new ListNode(suf[0]);ListNode c=s;for(int i=1;i<suf.length;i++){c.next=new ListNode(suf[i]);c=c.next;}}
        ListNode ha=build(a,suf),hb=buildB(b,s);
        ListNode g=new CodeCoder().getIntersectionNode(ha,hb);
        if((g==null&&exp==-1)||(g!=null&&g.val==exp))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:exp="+exp+":got="+(g==null?-1:g.val));
    }
    public static void main(String[] a){
        try{test(new int[]{4,1},new int[]{5,6,1},new int[]{8,4,5},8,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new int[]{2,6,4},new int[]{1,5},new int[]{},-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new int[]{},new int[]{},new int[]{1,2,3},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new int[]{1},new int[]{1},new int[]{},-1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new int[]{1,2,3},new int[]{1,2,3},new int[]{4,5},4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new int[]{0},new int[]{0,0,0,0},new int[]{0},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new int[]{100,200},new int[]{300,400,500},new int[]{600,700},600,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new int[]{10,20,30,40,50},new int[]{60,70,80,90},new int[]{},-1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new int[]{-5,-4},new int[]{-3,-2},new int[]{-1,0,1},-1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new int[]{1,1,1,1,1},new int[]{2,2,2,2,2},new int[]{3,3,3,3,3},3,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
struct ListNode{int val;ListNode* next;ListNode(int x):val(x),next(NULL){}};

// USER_CODE_START
class CodeCoder{public:ListNode* getIntersectionNode(ListNode* a,ListNode* b){return NULL;}};
// USER_CODE_END

ListNode* build(vector<int>& p,ListNode* s){
    ListNode d(0),*c=&d;
    for(int v:p){c->next=new ListNode(v);c=c->next;}
    if(s)c->next=s;
    return d.next;
}
ListNode* buildSuf(vector<int>& suf){
    if(suf.empty())return NULL;
    ListNode* h=new ListNode(suf[0]),*c=h;
    for(int i=1;i<(int)suf.size();i++){c->next=new ListNode(suf[i]);c=c->next;}
    return h;
}
void test(vector<int> pa,vector<int> pb,vector<int> suf,int exp,int tc,bool h=false){
    ListNode* s=buildSuf(suf);
    ListNode* ha=build(pa,s),*hb=build(pb,s);
    ListNode* g=CodeCoder().getIntersectionNode(ha,hb);
    int gv=(g?g->val:-1);
    if(gv==exp)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<exp<<":got="<<gv<<"\\n";
}
int main(){
try{test({4,1},{5,6,1},{8,4,5},8,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,6,4},{1,5},{},-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},{},{1,2,3},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},{1},{},-1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3},{1,2,3},{4,5},4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0},{0,0,0,0},{0},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({100,200},{300,400,500},{600,700},600,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({10,20,30,40,50},{60,70,80,90},{},-1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({-5,-4},{-3,-2},{-1,0,1},-1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,1,1,1,1},{2,2,2,2,2},{3,3,3,3,3},3,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def getIntersectionNode(self, a, b):
        return None
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def buildSuf(suf):
    if not suf: return None
    h=ListNode(suf[0]);c=h
    for v in suf[1:]:c.next=ListNode(v);c=c.next
    return h

def build(p, s):
    d=ListNode(0);c=d
    for v in p:c.next=ListNode(v);c=c.next
    if s:c.next=s
    return d.next

def test(pa,pb,suf,exp,tc,h=False):
    s=buildSuf(suf)
    ha=build(pa,s);hb=build(pb,s)
    g=CodeCoder().getIntersectionNode(ha,hb)
    gv=g.val if g else -1
    if gv==exp:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={exp}:got={gv}")

try:test([4,1],[5,6,1],[8,4,5],8,1)
except:print("TC:1:FAIL:hidden")
try:test([2,6,4],[1,5],[],-1,2)
except:print("TC:2:FAIL:hidden")
try:test([],[],[1,2,3],1,3)
except:print("TC:3:FAIL:hidden")
try:test([1],[1],[],-1,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3],[1,2,3],[4,5],4,5)
except:print("TC:5:FAIL:hidden")
try:test([0],[0,0,0,0],[0],0,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([100,200],[300,400,500],[600,700],600,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,20,30,40,50],[60,70,80,90],[],-1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([-5,-4],[-3,-2],[-1,0,1],-1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,1,1,1,1],[2,2,2,2,2],[3,3,3,3,3],3,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode{constructor(val,next){this.val=val;this.next=next||null;}}
function getIntersectionNode(a,b){return null;}
// USER_CODE_END

class ListNode{constructor(val,next){this.val=val;this.next=next||null;}}
function buildSuf(suf){
    if(!suf.length)return null;
    let h=new ListNode(suf[0]),c=h;
    for(let i=1;i<suf.length;i++){c.next=new ListNode(suf[i]);c=c.next;}
    return h;
}
function build(p,s){
    let d=new ListNode(0),c=d;
    for(let v of p){c.next=new ListNode(v);c=c.next;}
    if(s)c.next=s;
    return d.next;
}
function test(pa,pb,suf,exp,tc,h){if(h===undefined)h=false;
    const s=buildSuf(suf);
    const g=getIntersectionNode(build(pa,s),build(pb,s));
    const gv=g?g.val:-1;
    if(gv===exp)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+exp+":got="+gv);
}
try{test([4,1],[5,6,1],[8,4,5],8,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,6,4],[1,5],[],-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],[],[1,2,3],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],[1],[],-1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3],[1,2,3],[4,5],4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0],[0,0,0,0],[0],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([100,200],[300,400,500],[600,700],600,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,20,30,40,50],[60,70,80,90],[],-1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([-5,-4],[-3,-2],[-1,0,1],-1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,1,1,1,1],[2,2,2,2,2],[3,3,3,3,3],3,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
struct ListNode{int val;struct ListNode* next;};

// USER_CODE_START
struct ListNode* getIntersectionNode(struct ListNode* a,struct ListNode* b){return NULL;}
// USER_CODE_END

struct ListNode* buildSuf(int* a,int n){
    if(!n)return NULL;
    struct ListNode* h=malloc(sizeof(struct ListNode));h->val=a[0];h->next=NULL;
    struct ListNode* c=h;
    for(int i=1;i<n;i++){c->next=malloc(sizeof(struct ListNode));c=c->next;c->val=a[i];c->next=NULL;}
    return h;
}
struct ListNode* build(int* a,int n,struct ListNode* s){
    struct ListNode d;d.next=NULL;
    struct ListNode* c=&d;
    for(int i=0;i<n;i++){c->next=malloc(sizeof(struct ListNode));c=c->next;c->val=a[i];c->next=NULL;}
    if(s)c->next=s;
    return d.next;
}
void run(int* pa,int na,int* pb,int nb,int* suf,int ns,int exp,int tc,int h){
    struct ListNode* s=buildSuf(suf,ns);
    struct ListNode* g=getIntersectionNode(build(pa,na,s),build(pb,nb,s));
    int gv=g?g->val:-1;
    if(gv==exp){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,exp,gv);}
}
int main(){
int pa1[]={4,1},pb1[]={5,6,1},s1[]={8,4,5};run(pa1,2,pb1,3,s1,3,8,1,0);
int pa2[]={2,6,4},pb2[]={1,5};run(pa2,3,pb2,2,NULL,0,-1,2,0);
int s3[]={1,2,3};run(NULL,0,NULL,0,s3,3,1,3,0);
int pa4[]={1},pb4[]={1};run(pa4,1,pb4,1,NULL,0,-1,4,0);
int pa5[]={1,2,3},pb5[]={1,2,3},s5[]={4,5};run(pa5,3,pb5,3,s5,2,4,5,0);
int pa6[]={0},pb6[]={0,0,0,0},s6[]={0};run(pa6,1,pb6,4,s6,1,0,6,1);
int pa7[]={100,200},pb7[]={300,400,500},s7[]={600,700};run(pa7,2,pb7,3,s7,2,600,7,1);
int pa8[]={10,20,30,40,50},pb8[]={60,70,80,90};run(pa8,5,pb8,4,NULL,0,-1,8,1);
int pa9[]={-5,-4},pb9[]={-3,-2},s9[]={-1,0,1};run(pa9,2,pb9,2,s9,3,-1,9,1);
int pa10[]={1,1,1,1,1},pb10[]={2,2,2,2,2},s10[]={3,3,3,3,3};run(pa10,5,pb10,5,s10,5,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
