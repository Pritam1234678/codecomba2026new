"""
Lowest Common Ancestor of a Binary Search Tree
================================================
Given a BST, find the lowest common ancestor (LCA) of two given nodes p and q.

The LCA is the lowest node in the tree that has both p and q as descendants.
A node can be a descendant of itself.

BST property: left < root < right.
If both p and q are less than root, LCA is in left subtree.
If both are greater, LCA is in right subtree.
Otherwise, root is the LCA (one on left, one on right, or one is root itself).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Lowest Common Ancestor of a Binary Search Tree"
desc=(
    "Given a binary search tree (BST), find the lowest common ancestor (LCA) of two "
    "given nodes p and q in the BST.\n\n"
    "The LCA of two nodes p and q is the lowest node in the tree that has both p and q "
    "as descendants. A node is allowed to be a descendant of itself.\n\n"
    "Using BST property: all left subtree values < root.val < all right subtree values.\n"
    "If both p and q are less than root, LCA is in left subtree.\n"
    "If both are greater, LCA is in right subtree.\n"
    "Otherwise (p <= root <= q or q <= root <= p), root is the LCA.\n\n"
    "For example:\n"
    "        6\n"
    "       / \\\n"
    "      2   8\n"
    "     / \\ / \\\n"
    "    0  4 7  9\n"
    "      / \\\n"
    "     3   5\n"
    "LCA of 2 and 8 = 6. LCA of 2 and 4 = 2."
)
infmt="First line contains the BST in level-order BFS format.\nSecond line contains values of p and q separated by space."
outfmt="Print the value of the LCA node."
cons="1 ≤ nodes ≤ 10^4\n-10^5 ≤ Node.val ≤ 10^5\np and q exist in the BST."
e1="Input:\n6 2 8 0 4 7 9 null null 3 5\n2 8\n\nOutput:\n6"
e2="Input:\n6 2 8 0 4 7 9 null null 3 5\n2 4\n\nOutput:\n2"
e3="Input:\n2 1 3\n1 3\n\nOutput:\n2"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Tree, BST, Binary Tree",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
// class TreeNode {
//     int val;
//     TreeNode left;
//     TreeNode right;
//     TreeNode(int x) { val = x; }
// }
class CodeCoder {
    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        // Write your code here — use BST property
        return root;
    }
}
// USER_CODE_END

class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}

public class Main{
    static TreeNode build(Integer[] a){
        if(a==null||a.length==0||a[0]==null)return null;
        TreeNode r=new TreeNode(a[0]);
        Queue<TreeNode> qq=new LinkedList<>();qq.offer(r);
        int i=1;
        while(!qq.isEmpty()&&i<a.length){
            TreeNode cur=qq.poll();
            if(a[i]!=null){cur.left=new TreeNode(a[i]);qq.offer(cur.left);}
            i++;if(i<a.length&&a[i]!=null){cur.right=new TreeNode(a[i]);qq.offer(cur.right);}
            i++;
        }
        return r;
    }
    static TreeNode find(TreeNode r,int v){
        if(r==null)return null;
        if(r.val==v)return r;
        TreeNode l=find(r.left,v);if(l!=null)return l;
        return find(r.right,v);
    }
    static void test(Integer[] a,int pv,int qv,int ev,int tc,boolean h){
        TreeNode root=build(a);
        TreeNode p=find(root,pv),q=find(root,qv);
        TreeNode g=new CodeCoder().lowestCommonAncestor(root,p,q);
        int gv=(g!=null?g.val:-1);
        if(gv==ev)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:p="+pv+" q="+qv+":exp="+ev+":got="+gv);
    }
    public static void main(String[] a){
        try{test(new Integer[]{6,2,8,0,4,7,9,null,null,3,5},2,8,6,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{6,2,8,0,4,7,9,null,null,3,5},2,4,2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{2,1,3},1,3,2,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{6,2,8,0,4,7,9,null,null,3,5},4,5,4,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{6,2,8,0,4,7,9,null,null,3,5},0,9,6,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{10,5,15,3,7,12,20},5,20,10,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{10,5,15,3,7,12,20},7,12,10,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{10,5,15,3,7,12,20},3,5,5,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1},1,1,1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{5,3,8,2,4,6,9},2,9,5,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
// class TreeNode {
//     public:
//         int val;
//         TreeNode *left;
//         TreeNode *right;
//         TreeNode(int x) : val(x), left(NULL), right(NULL) {}
// };
class CodeCoder {
public:
    TreeNode* lowestCommonAncestor(TreeNode* root, TreeNode* p, TreeNode* q) {
        // Write your code here
        return root;
    }
};
// USER_CODE_END

class TreeNode{public:int val;TreeNode *left,*right;TreeNode(int x):val(x),left(NULL),right(NULL){}};

TreeNode* build(vector<int*> a){
    if(a.empty()||!a[0])return NULL;
    TreeNode* r=new TreeNode(*a[0]);queue<TreeNode*> q;q.push(r);int i=1;
    while(!q.empty()&&i<(int)a.size()){
        TreeNode* cur=q.front();q.pop();
        if(a[i]){cur->left=new TreeNode(*a[i]);q.push(cur->left);}
        i++;if(i<(int)a.size()&&a[i]){cur->right=new TreeNode(*a[i]);q.push(cur->right);}
        i++;
    }return r;
}
TreeNode* find(TreeNode* r,int v){
    if(!r)return NULL;
    if(r->val==v)return r;
    TreeNode* l=find(r->left,v);if(l)return l;
    return find(r->right,v);
}
void test(vector<int*> a,int pv,int qv,int ev,int tc,bool h=false){
    TreeNode* root=build(a);
    TreeNode* g=CodeCoder().lowestCommonAncestor(root,find(root,pv),find(root,qv));
    int gv=g?g->val:-1;
    if(gv==ev)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:p="<<pv<<" q="<<qv<<":exp="<<ev<<":got="<<gv<<"\\n";
}
int main(){
int _0=0,_1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_12=12,_15=15,_20=20;
int* t1[]={&_6,&_2,&_8,&_0,&_4,&_7,&_9,NULL,NULL,&_3,&_5};try{test(t1,11,2,8,6,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(t1,11,2,4,2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
int* t3[]={&_2,&_1,&_3};try{test(t3,3,1,3,2,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(t1,11,4,5,4,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(t1,11,0,9,6,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
int* t6[]={&_10,&_5,&_15,&_3,&_7,&_12,&_20};try{test(t6,7,5,20,10,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(t6,7,7,12,10,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(t6,7,3,5,5,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
int* t9[]={&_1};try{test(t9,1,1,1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
int* t10[]={&_5,&_3,&_8,&_2,&_4,&_6,&_9};try{test(t10,7,2,9,5,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def lowestCommonAncestor(self, root, p, q):
        return root
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

def find(r,v):
    if not r: return None
    if r.val==v: return r
    l=find(r.left,v)
    if l: return l
    return find(r.right,v)

def test(a,pv,qv,ev,tc,h=False):
    root=build(a)
    g=CodeCoder().lowestCommonAncestor(root,find(root,pv),find(root,qv))
    gv=g.val if g else -1
    if gv==ev:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:p={pv}:q={qv}:exp={ev}:got={gv}")

try:test([6,2,8,0,4,7,9,None,None,3,5],2,8,6,1)
except:print("TC:1:FAIL:hidden")
try:test([6,2,8,0,4,7,9,None,None,3,5],2,4,2,2)
except:print("TC:2:FAIL:hidden")
try:test([2,1,3],1,3,2,3)
except:print("TC:3:FAIL:hidden")
try:test([6,2,8,0,4,7,9,None,None,3,5],4,5,4,4)
except:print("TC:4:FAIL:hidden")
try:test([6,2,8,0,4,7,9,None,None,3,5],0,9,6,5)
except:print("TC:5:FAIL:hidden")
try:test([10,5,15,3,7,12,20],5,20,10,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([10,5,15,3,7,12,20],7,12,10,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,5,15,3,7,12,20],3,5,5,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1],1,1,1,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([5,3,8,2,4,6,9],2,9,5,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function lowestCommonAncestor(root,p,q){return root;}
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
        i++;if(i<a.length&&a[i]!==null){cur.right=new TreeNode(a[i]);q.push(cur.right);}
        i++;
    }
    return r;
}
function find(r,v){
    if(!r)return null;
    if(r.val===v)return r;
    let l=find(r.left,v);if(l)return l;
    return find(r.right,v);
}
function test(a,pv,qv,ev,tc,h){if(h===undefined)h=false;
    const root=build(a);
    const g=lowestCommonAncestor(root,find(root,pv),find(root,qv));
    const gv=g?g.val:-1;
    if(gv===ev)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:p="+pv+":q="+qv+":exp="+ev+":got="+gv);
}
try{test([6,2,8,0,4,7,9,null,null,3,5],2,8,6,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([6,2,8,0,4,7,9,null,null,3,5],2,4,2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([2,1,3],1,3,2,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([6,2,8,0,4,7,9,null,null,3,5],4,5,4,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([6,2,8,0,4,7,9,null,null,3,5],0,9,6,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([10,5,15,3,7,12,20],5,20,10,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([10,5,15,3,7,12,20],7,12,10,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,5,15,3,7,12,20],3,5,5,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1],1,1,1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([5,3,8,2,4,6,9],2,9,5,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

struct TreeNode{int val;struct TreeNode *left,*right;};

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
struct TreeNode* lowestCommonAncestor(struct TreeNode* root, struct TreeNode* p, struct TreeNode* q) {
    // Write your code here — BST property
    return root;
}
// USER_CODE_END

struct TreeNode* build(int** a,int n){
    if(!n||!a||!a[0])return NULL;
    struct TreeNode** q=malloc(n*sizeof(struct TreeNode*));int f=0,r=0;
    struct TreeNode* root=malloc(sizeof(struct TreeNode));root->val=*a[0];root->left=root->right=NULL;
    q[r++]=root;int i=1;
    while(f<r&&i<n){
        struct TreeNode* cur=q[f++];
        if(a[i]){struct TreeNode* l=malloc(sizeof(struct TreeNode));l->val=*a[i];l->left=l->right=NULL;cur->left=l;q[r++]=l;}
        i++;if(i<n&&a[i]){struct TreeNode* ri=malloc(sizeof(struct TreeNode));ri->val=*a[i];ri->left=ri->right=NULL;cur->right=ri;q[r++]=ri;}
        i++;
    }free(q);return root;
}
struct TreeNode* find(struct TreeNode* r,int v){
    if(!r)return NULL;
    if(r->val==v)return r;
    struct TreeNode* l=find(r->left,v);if(l)return l;
    return find(r->right,v);
}
void run(int** a,int n,int pv,int qv,int ev,int tc,int h){
    struct TreeNode* root=build(a,n);
    struct TreeNode* p=find(root,pv),*q=find(root,qv);
    struct TreeNode* g=lowestCommonAncestor(root,p,q);
    int gv=g?g->val:-1;
    if(gv==ev){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:p=%d:q=%d:exp=%d:got=%d\\n",tc,pv,qv,ev,gv);}
}
int main(){
int _0=0,_1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_12=12,_15=15,_20=20;
int* t1[]={&_6,&_2,&_8,&_0,&_4,&_7,&_9,NULL,NULL,&_3,&_5};run(t1,11,2,8,6,1,0);
run(t1,11,2,4,2,2,0);
int* t3[]={&_2,&_1,&_3};run(t3,3,1,3,2,3,0);
run(t1,11,4,5,4,4,0);
run(t1,11,0,9,6,5,0);
int* t6[]={&_10,&_5,&_15,&_3,&_7,&_12,&_20};run(t6,7,5,20,10,6,1);
run(t6,7,7,12,10,7,1);
run(t6,7,3,5,5,8,1);
int* t9[]={&_1};run(t9,1,1,1,1,9,1);
int* t10[]={&_5,&_3,&_8,&_2,&_4,&_6,&_9};run(t10,7,2,9,5,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
