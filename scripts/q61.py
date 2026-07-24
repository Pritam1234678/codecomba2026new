"""
Maximum Depth of Binary Tree
==============================
Given root of a binary tree, return its maximum depth (number of nodes along
the longest path from root to farthest leaf).

Examples:
    3
   / \
  9  20
     /  \
    15   7   → depth = 3

Approach: Recursive DFS. depth = 1 + max(leftDepth, rightDepth).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Maximum Depth of Binary Tree"
desc=(
    "Given the root of a binary tree, return its maximum depth.\n\n"
    "The maximum depth is the number of nodes along the longest path from "
    "the root node down to the farthest leaf node.\n\n"
    "For example:\n"
    "    3\n"
    "   / \\\n"
    "  9  20\n"
    "     /  \\\n"
    "    15   7\n"
    "Depth = 3 (path: 3 → 20 → 15 or 3 → 20 → 7)\n\n"
    "A leaf node has depth 1. An empty tree (null root) has depth 0.\n"
    "Use recursion: depth = 1 + max(depth(left), depth(right))."
)
infmt="A single line containing the tree in level-order BFS format (null for empty nodes)."
outfmt="Print the maximum depth."
cons="0 ≤ nodes ≤ 10^4\n-100 ≤ Node.val ≤ 100"
e1="Input:\n3 9 20 null null 15 7\n\nOutput:\n3"
e2="Input:\n1 null 2\n\nOutput:\n2"
e3="Input:\n\n\nOutput:\n0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Tree, Binary Tree, DFS, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}

// USER_CODE_START
// class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}
class CodeCoder{
    public int maxDepth(TreeNode root){return 0;}
}
// USER_CODE_END

public class Main{
    static TreeNode build(Integer[] a){
        if(a==null||a.length==0||a[0]==null)return null;
        TreeNode r=new TreeNode(a[0]);
        Queue<TreeNode> q=new LinkedList<>();q.offer(r);
        int i=1;
        while(!q.isEmpty()&&i<a.length){
            TreeNode cur=q.poll();
            if(a[i]!=null){cur.left=new TreeNode(a[i]);q.offer(cur.left);}
            i++;
            if(i<a.length&&a[i]!=null){cur.right=new TreeNode(a[i]);q.offer(cur.right);}
            i++;
        }
        return r;
    }
    static void test(Integer[] a,int e,int tc,boolean h){
        int g=new CodeCoder().maxDepth(build(a));
        if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);
    }
    public static void main(String[] a){
        try{test(new Integer[]{3,9,20,null,null,15,7},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{1,null,2},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1},1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,null,null,6},4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{1,2,null,3,null,4,null,5},5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{0,0,0,0,0,0,0,0,0,0,0,null,null,null,null},4,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{1,2,2,3,3,null,null,4,4},4,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},4,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{1,null,2,null,3,null,4,null,5},5,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
struct TreeNode{int val;TreeNode *left,*right;TreeNode(int x):val(x),left(NULL),right(NULL){}};

// USER_CODE_START
class CodeCoder{public:int maxDepth(TreeNode* r){return 0;}};
// USER_CODE_END

TreeNode* build(vector<int*> a){
    if(a.empty()||!a[0])return NULL;
    TreeNode* r=new TreeNode(*a[0]);
    queue<TreeNode*> q;q.push(r);
    int i=1;
    while(!q.empty()&&i<(int)a.size()){
        TreeNode* cur=q.front();q.pop();
        if(a[i]){cur->left=new TreeNode(*a[i]);q.push(cur->left);}
        i++;
        if(i<(int)a.size()&&a[i]){cur->right=new TreeNode(*a[i]);q.push(cur->right);}
        i++;
    }
    return r;
}
void test(vector<int*> a,int e,int tc,bool h=false){
    int g=CodeCoder().maxDepth(build(a));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";
}
int main(){
int _3=3,_9=9,_20=20,_15=15,_7=7,_1=1,_2=2,_4=4,_5=5,_6=6,_8=8,_10=10,_11=11,_12=12,_13=13,_14=14,_15_=15,_0=0;
try{test({&_3,&_9,&_20,NULL,NULL,&_15,&_7},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_1,NULL,&_2},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1},1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6},4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5},5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,NULL,NULL,NULL,NULL},4,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4},4,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15_},4,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_1,NULL,&_2,NULL,&_3,NULL,&_4,NULL,&_5},5,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def maxDepth(self, root):
        return 0
# USER_CODE_END

class TreeNode:
    def __init__(self, val=0, left=None, right=None):
        self.val = val; self.left = left; self.right = right

def build(a):
    if not a or a[0] is None: return None
    r=TreeNode(a[0]);q=[r];i=1
    while q and i<len(a):
        cur=q.pop(0)
        if a[i] is not None:cur.left=TreeNode(a[i]);q.append(cur.left)
        i+=1
        if i<len(a) and a[i] is not None:cur.right=TreeNode(a[i]);q.append(cur.right)
        i+=1
    return r

def test(a,e,tc,h=False):
    g=CodeCoder().maxDepth(build(a))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([3,9,20,None,None,15,7],3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,None,2],2,2)
except:print("TC:2:FAIL:hidden")
try:test([],0,3)
except:print("TC:3:FAIL:hidden")
try:test([1],1,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,None,None,6],4,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,None,3,None,4,None,5],5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,0,0,0,0,0,0,0,0,0,0,None,None,None,None],4,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,2,3,3,None,None,4,4],4,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],4,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,None,2,None,3,None,4,None,5],5,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function maxDepth(root){return 0;}
// USER_CODE_END

class TreeNode{
    constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
}
function build(a){
    if(!a||!a.length||a[0]===null)return null;
    let r=new TreeNode(a[0]),q=[r],i=1;
    while(q.length&&i<a.length){
        let cur=q.shift();
        if(a[i]!==null){cur.left=new TreeNode(a[i]);q.push(cur.left);}
        i++;
        if(i<a.length&&a[i]!==null){cur.right=new TreeNode(a[i]);q.push(cur.right);}
        i++;
    }
    return r;
}
function test(a,e,tc,h){if(h===undefined)h=false;
    const g=maxDepth(build(a));
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([3,9,20,null,null,15,7],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,null,2],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,null,null,6],4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,null,3,null,4,null,5],5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,0,0,0,0,0,0,0,0,0,0,null,null,null,null],4,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,2,3,3,null,null,4,4],4,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],4,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,null,2,null,3,null,4,null,5],5,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
struct TreeNode{int val;struct TreeNode *left,*right;};

// USER_CODE_START
int maxDepth(struct TreeNode* r){return 0;}
// USER_CODE_END

struct TreeNode* build(int** a,int n){
    if(!n||!a[0])return NULL;
    struct TreeNode** q=malloc(n*sizeof(struct TreeNode*));
    int front=0,rear=0;
    struct TreeNode* r=malloc(sizeof(struct TreeNode));r->val=*a[0];r->left=r->right=NULL;
    q[rear++]=r;int i=1;
    while(front<rear&&i<n){
        struct TreeNode* cur=q[front++];
        if(a[i]){struct TreeNode* l=malloc(sizeof(struct TreeNode));l->val=*a[i];l->left=l->right=NULL;cur->left=l;q[rear++]=l;}
        i++;
        if(i<n&&a[i]){struct TreeNode* ri=malloc(sizeof(struct TreeNode));ri->val=*a[i];ri->left=ri->right=NULL;cur->right=ri;q[rear++]=ri;}
        i++;
    }
    free(q);return r;
}
void run(int** a,int n,int e,int tc,int h){
    int g=maxDepth(build(a,n));
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
int _3=3,_9=9,_20=20,_15=15,_7=7,_1=1,_2=2,_4=4,_5=5,_6=6,_0=0,_8=8,_10=10,_11=11,_12=12,_13=13,_14=14,_15_=15;
int* t1[]={&_3,&_9,&_20,NULL,NULL,&_15,&_7};run(t1,7,3,1,0);
int* t2[]={&_1,NULL,&_2};run(t2,3,2,2,0);
run(NULL,0,0,3,0);
int* t4[]={&_1};run(t4,1,1,4,0);
int* t5[]={&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6};run(t5,8,4,5,0);
int* t6[]={&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5};run(t6,8,5,6,1);
int* t7[]={&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,&_0,NULL,NULL,NULL,NULL};run(t7,15,4,7,1);
int* t8[]={&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4};run(t8,9,4,8,1);
int* t9[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15_};run(t9,15,4,9,1);
int* t10[]={&_1,NULL,&_2,NULL,&_3,NULL,&_4,NULL,&_5};run(t10,9,5,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
