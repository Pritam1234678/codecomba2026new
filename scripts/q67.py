"""
Validate Binary Search Tree
=============================
Given the root of a binary tree, determine if it is a valid BST.

A BST is valid if:
1. Left subtree of a node contains only nodes with values LESS than the node's value.
2. Right subtree contains only nodes with values GREATER than the node's value.
3. Both left and right subtrees must also be valid BSTs.

Approach: Inorder traversal gives sorted order for BST.
Or use recursive range check with min/max bounds.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Validate Binary Search Tree"
desc=(
    "Given the root of a binary tree, determine if it is a valid binary search tree (BST).\n\n"
    "A valid BST is defined as follows:\n"
    "1. The left subtree of a node contains only nodes with keys LESS THAN the node's key.\n"
    "2. The right subtree of a node contains only nodes with keys GREATER THAN the node's key.\n"
    "3. Both the left and right subtrees must also be binary search trees.\n\n"
    "For example:\n"
    "    2\n"
    "   / \\\n"
    "  1   3   → valid BST (1<2<3)\n\n"
    "    5\n"
    "   / \\\n"
    "  1   4\n"
    "     / \\\n"
    "    3   6  → invalid (4 is in right of 5 but 3<5 violates right-subtree > parent)\n\n"
    "Use recursive range check: each node must be within (min, max) bounds."
)
infmt="A single line containing the tree in level-order BFS format (null for empty)."
outfmt="Print 'true' if valid BST, otherwise 'false'."
cons="0 ≤ nodes ≤ 10^4\n-2^31 ≤ Node.val ≤ 2^31-1"
e1="Input:\n2 1 3\n\nOutput:\ntrue"
e2="Input:\n5 1 4 null null 3 6\n\nOutput:\nfalse"
e3="Input:\n\n\nOutput:\ntrue\n\nExplanation: Empty tree is valid."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Tree, BST, DFS, Binary Tree",e1,e2,e3))
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
    public boolean isValidBST(TreeNode root) {
        // Write your code here — recursive range check with min/max
        return false;
    }
}
// USER_CODE_END

class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}

public class Main{
    static TreeNode build(Integer[] a){
        if(a==null||a.length==0||a[0]==null)return null;
        TreeNode r=new TreeNode(a[0]);
        Queue<TreeNode> q=new LinkedList<>();q.offer(r);
        int i=1;
        while(!q.isEmpty()&&i<a.length){
            TreeNode cur=q.poll();
            if(a[i]!=null){cur.left=new TreeNode(a[i]);q.offer(cur.left);}
            i++;if(i<a.length&&a[i]!=null){cur.right=new TreeNode(a[i]);q.offer(cur.right);}
            i++;
        }
        return r;
    }
    static void test(Integer[] a,boolean e,int tc,boolean h){boolean g=new CodeCoder().isValidBST(build(a));if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);}
    public static void main(String[] a){
        try{test(new Integer[]{2,1,3},true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{5,1,4,null,null,3,6},false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1},true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{2,2,2},false,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{2147483647},true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{-2147483648},true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{10,5,15,null,null,6,20},false,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{3,1,5,0,2,4,6},true,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{3,1,5,0,2,4,6},true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    bool isValidBST(TreeNode* root) {
        // Write your code here
        return false;
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
void test(vector<int*> a,bool e,int tc,bool h=false){
    bool g=CodeCoder().isValidBST(build(a));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_0=0,_10=10,_15=15,_20=20,_2147483647=2147483647,_m2147483648=-2147483648;
try{test({&_2,&_1,&_3},true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_5,&_1,&_4,NULL,NULL,&_3,&_6},false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1},true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_2,&_2,&_2},false,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_2147483647},true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_m2147483648},true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_10,&_5,&_15,NULL,NULL,&_6,&_20},false,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_3,&_1,&_5,&_0,&_2,&_4,&_6},true,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_3,&_1,&_5,&_0,&_2,&_4,&_6},true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def isValidBST(self, root):
        return False
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
    g=CodeCoder().isValidBST(build(a))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([2,1,3],True,1)
except:print("TC:1:FAIL:hidden")
try:test([5,1,4,None,None,3,6],False,2)
except:print("TC:2:FAIL:hidden")
try:test([],True,3)
except:print("TC:3:FAIL:hidden")
try:test([1],True,4)
except:print("TC:4:FAIL:hidden")
try:test([2,2,2],False,5)
except:print("TC:5:FAIL:hidden")
try:test([2147483647],True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-2147483648],True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,5,15,None,None,6,20],False,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([3,1,5,0,2,4,6],True,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([3,1,5,0,2,4,6],True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function isValidBST(root){return false;}
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
function test(a,e,tc,h){if(h===undefined)h=false;const g=isValidBST(build(a));if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([2,1,3],true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,1,4,null,null,3,6],false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2,2,2],false,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2147483647],true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-2147483648],true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,5,15,null,null,6,20],false,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([3,1,5,0,2,4,6],true,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([3,1,5,0,2,4,6],true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <limits.h>

struct TreeNode{int val;struct TreeNode *left,*right;};

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
bool isValidBST(struct TreeNode* root) {
    // Write your code here
    return false;
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
void run(int** a,int n,bool e,int tc,int h){
    bool g=isValidBST(build(a,n));
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_0=0,_10=10,_15=15,_20=20,_2147483647=2147483647,_m2147483648=-2147483648;
int* t1[]={&_2,&_1,&_3};run(t1,3,true,1,0);
int* t2[]={&_5,&_1,&_4,NULL,NULL,&_3,&_6};run(t2,7,false,2,0);
run(NULL,0,true,3,0);
int* t4[]={&_1};run(t4,1,true,4,0);
int* t5[]={&_2,&_2,&_2};run(t5,3,false,5,0);
int* t6[]={&_2147483647};run(t6,1,true,6,1);
int* t7[]={&_m2147483648};run(t7,1,true,7,1);
int* t8[]={&_10,&_5,&_15,NULL,NULL,&_6,&_20};run(t8,7,false,8,1);
int* t9[]={&_3,&_1,&_5,&_0,&_2,&_4,&_6};run(t9,7,true,9,1);
int* t10[]={&_3,&_1,&_5,&_0,&_2,&_4,&_6};run(t10,7,true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
