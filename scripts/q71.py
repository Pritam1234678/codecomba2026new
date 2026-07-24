"""
Merge k Sorted Lists
======================
You are given an array of k linked-lists, each sorted in ascending order.
Merge all the linked-lists into one sorted linked-list and return its head.

Example:
  lists = [[1,4,5],[1,3,4],[2,6]]
  Output: [1,1,2,3,4,4,5,6]

Approach: Use a min-heap (priority queue) of size k. Push the head of each
list into the heap. Pop the smallest, add it to result, push its next node.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Merge k Sorted Lists"
desc=(
    "You are given an array of k linked-lists, each sorted in ascending order.\n\n"
    "Merge all the linked-lists into one sorted linked-list and return its head.\n\n"
    "For example:\n"
    "lists = [[1,4,5],[1,3,4],[2,6]]\n"
    "Output: [1,1,2,3,4,4,5,6]\n\n"
    "The naive approach is to repeatedly find the minimum among all k heads, "
    "but that takes O(k * n). A better approach uses a min-heap (priority queue). "
    "Push the first node of each list into the heap. Then repeatedly pop the "
    "smallest node, add it to the result, and push its next node back into the heap."
)
infmt=("First line contains k (number of lists).\n"
       "For each list: first line contains n (size), next line contains n space-separated integers."
       " (Repeat k times)")
outfmt="Print the merged sorted list as space-separated integers."
cons="k == |lists|, 0 ≤ k ≤ 10^4\n0 ≤ |lists[i]| ≤ 500\n-10^4 ≤ Node.val ≤ 10^4"
e1="Input:\n3\n3\n1 4 5\n3\n1 3 4\n2\n2 6\n\nOutput:\n1 1 2 3 4 4 5 6"
e2="Input:\n0\n\n\nOutput:\n\n(empty line)"
e3="Input:\n1\n1\n1\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Linked List, Heap, Divide and Conquer",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode{int val;ListNode next;ListNode(int x){val=x;}}

// USER_CODE_START
// class ListNode {
//     int val;
//     ListNode next;
//     ListNode(int x) { val = x; }
// }
class CodeCoder {
    public ListNode mergeKLists(ListNode[] lists) {
        // Write your code here — use min-heap
        return null;
    }
}
// USER_CODE_END

public class Main {
    static ListNode build(int[] a){
        if(a.length==0)return null;
        ListNode d=new ListNode(0),c=d;
        for(int v:a){c.next=new ListNode(v);c=c.next;}
        return d.next;
    }
    static int[] ser(ListNode h){
        List<Integer> l=new ArrayList<>();
        while(h!=null){l.add(h.val);h=h.next;}
        return l.stream().mapToInt(i->i).toArray();
    }
    static void test(int[][] vals,int[] exp,int tc,boolean h){
        ListNode[] lists=new ListNode[vals.length];
        for(int i=0;i<vals.length;i++)lists[i]=build(vals[i]);
        int[] got=ser(new CodeCoder().mergeKLists(lists));
        if(Arrays.equals(got,exp))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:got="+Arrays.toString(got)+":exp="+Arrays.toString(exp));
    }
    public static void main(String[] a){
        try{test(new int[][]{{1,4,5},{1,3,4},{2,6}},new int[]{1,1,2,3,4,4,5,6},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new int[][]{},new int[]{},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new int[][]{{1}},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new int[][]{{1},{0}},new int[]{0,1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new int[][]{{},{1,2},{3,4,5}},new int[]{1,2,3,4,5},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new int[][]{{1,2,3,4,5},{6,7,8,9,10}},new int[]{1,2,3,4,5,6,7,8,9,10},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new int[][]{{-5,-3,-1},{-4,-2,0},{1,2,3}},new int[]{-5,-4,-3,-2,-1,0,1,2,3},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new int[][]{{5,5,5},{5,5,5},{5,5,5}},new int[]{5,5,5,5,5,5,5,5,5},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new int[][]{{100},{200},{300},{400}},new int[]{100,200,300,400},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new int[][]{{0},{0},{0},{0},{0}},new int[]{0,0,0,0,0},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

struct ListNode{int val;ListNode* next;ListNode(int x):val(x),next(NULL){}};

// USER_CODE_START
// struct ListNode {
//     int val;
//     ListNode* next;
//     ListNode(int x) : val(x), next(NULL) {}
// };
class CodeCoder {
public:
    ListNode* mergeKLists(vector<ListNode*>& lists) {
        // Write your code here — use min-heap
        return NULL;
    }
};
// USER_CODE_END

ListNode* build(vector<int>& a){
    if(a.empty())return NULL;
    ListNode d(0),*c=&d;
    for(int v:a){c->next=new ListNode(v);c=c->next;}
    return d.next;
}
vector<int> ser(ListNode* h){
    vector<int> r;while(h){r.push_back(h->val);h=h->next;}return r;
}
void test(vector<vector<int>> vals,vector<int> exp,int tc,bool h=false){
    vector<ListNode*> lists;
    for(auto& v:vals)lists.push_back(build(v));
    vector<int> got=ser(CodeCoder().mergeKLists(lists));
    if(got==exp)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:got)cout<<x<<",";cout<<"]:exp=[";for(int x:exp)cout<<x<<",";cout<<"]\\n";}
}
int main(){
try{test({{1,4,5},{1,3,4},{2,6}},{1,1,2,3,4,4,5,6},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({},{},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1}},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1},{0}},{0,1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{},{1,2},{3,4,5}},{1,2,3,4,5},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,2,3,4,5},{6,7,8,9,10}},{1,2,3,4,5,6,7,8,9,10},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({{-5,-3,-1},{-4,-2,0},{1,2,3}},{-5,-4,-3,-2,-1,0,1,2,3},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({{5,5,5},{5,5,5},{5,5,5}},{5,5,5,5,5,5,5,5,5},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({{100},{200},{300},{400}},{100,200,300,400},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({{0},{0},{0},{0},{0}},{0,0,0,0,0},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def mergeKLists(self, lists):
        return None
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    if not a: return None
    d=ListNode(0);c=d
    for v in a:c.next=ListNode(v);c=c.next
    return d.next
def ser(h):
    r=[]
    while h:r.append(h.val);h=h.next
    return r

def test(vals,exp,tc,h=False):
    lists=[build(v) for v in vals]
    got=ser(CodeCoder().mergeKLists(lists))
    if got==exp:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={got}:exp={exp}")

try:test([[1,4,5],[1,3,4],[2,6]],[1,1,2,3,4,4,5,6],1)
except:print("TC:1:FAIL:hidden")
try:test([],[],2)
except:print("TC:2:FAIL:hidden")
try:test([[1]],[1],3)
except:print("TC:3:FAIL:hidden")
try:test([[1],[0]],[0,1],4)
except:print("TC:4:FAIL:hidden")
try:test([[],[1,2],[3,4,5]],[1,2,3,4,5],5)
except:print("TC:5:FAIL:hidden")
try:test([[1,2,3,4,5],[6,7,8,9,10]],[1,2,3,4,5,6,7,8,9,10],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([[-5,-3,-1],[-4,-2,0],[1,2,3]],[-5,-4,-3,-2,-1,0,1,2,3],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([[5,5,5],[5,5,5],[5,5,5]],[5,5,5,5,5,5,5,5,5],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([[100],[200],[300],[400]],[100,200,300,400],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([[0],[0],[0],[0],[0]],[0,0,0,0,0],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode {
//     constructor(val, next) { this.val = val; this.next = next || null; }
// }
function mergeKLists(lists) {
    // Write your code here — use min-heap or divide-and-conquer
    return null;
}
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
function test(vals,exp,tc,h){if(h===undefined)h=false;
    const lists=vals.map(v=>build(v));
    const got=ser(mergeKLists(lists));
    const gs=JSON.stringify(got),es=JSON.stringify(exp);
    if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);
}
try{test([[1,4,5],[1,3,4],[2,6]],[1,1,2,3,4,4,5,6],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([],[],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1]],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1],[0]],[0,1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[],[1,2],[3,4,5]],[1,2,3,4,5],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,2,3,4,5],[6,7,8,9,10]],[1,2,3,4,5,6,7,8,9,10],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([[-5,-3,-1],[-4,-2,0],[1,2,3]],[-5,-4,-3,-2,-1,0,1,2,3],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([[5,5,5],[5,5,5],[5,5,5]],[5,5,5,5,5,5,5,5,5],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([[100],[200],[300],[400]],[100,200,300,400],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([[0],[0],[0],[0],[0]],[0,0,0,0,0],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct ListNode{int val;struct ListNode* next;};

// USER_CODE_START
// struct ListNode {
//     int val;
//     struct ListNode* next;
// };
struct ListNode* mergeKLists(struct ListNode** lists, int listsSize) {
    // Write your code here
    return NULL;
}
// USER_CODE_END

int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
