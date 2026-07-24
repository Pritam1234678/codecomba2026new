"""
Path Sum
==========
Given the root of a binary tree and an integer targetSum, return true if
the tree has a root-to-leaf path where the sum of all node values along
the path equals targetSum.

A leaf is a node with no children.
A path must start from root and end at a leaf — you cannot stop in the middle.

Examples:
        5
       / \
      4   8
     /   / \
    11  13  4
   /  \      \
  7    2      1

targetSum = 22 → true  (path: 5 → 4 → 11 → 2 = 22)
targetSum = 26 → true  (path: 5 → 8 → 13 = 26)

Approach: Recursively subtract the current node's value from targetSum.
If you reach a leaf and targetSum becomes 0, return true.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Path Sum"
desc=(
    "Given the root of a binary tree and an integer targetSum, return true if "
    "the tree has a root-to-leaf path where adding up all the node values equals targetSum.\n\n"
    "A leaf is a node that has no left or right children. The path MUST start at the "
    "root and end at a leaf — you cannot stop in the middle.\n\n"
    "For example:\n"
    "        5\n"
    "       / \\\n"
    "      4   8\n"
    "     /   / \\\n"
    "    11  13  4\n"
    "   /  \\      \\\n"
    "  7    2      1\n\n"
    "targetSum = 22:\n"
    "  Path 5 → 4 → 11 → 2 adds up to 5+4+11+2 = 22. Since 2 is a leaf, this is valid. → true\n\n"
    "targetSum = 26:\n"
    "  Path 5 → 8 → 13 adds up to 5+8+13 = 26. 13 is a leaf. → true\n\n"
    "targetSum = 27:\n"
    "  Path 5 → 4 → 11 → 7 = 27, 7 is a leaf. → true\n\n"
    "Approach: recursively subtract the node's value from targetSum as you traverse down. "
    "When you reach a leaf, check if the remaining sum equals 0."
)
infmt="First line contains the tree in level-order BFS format (null for empty).\nSecond line contains targetSum."
outfmt="Print 'true' if a root-to-leaf path with targetSum exists, otherwise 'false'."
cons="0 ≤ nodes ≤ 5000\n-1000 ≤ Node.val ≤ 1000\n-1000 ≤ targetSum ≤ 1000"
e1="Input:\n5 4 8 11 null 13 4 7 2 null null null 1\n22\n\nOutput:\ntrue\n\nExplanation: 5+4+11+2 = 22, and 2 is a leaf."
e2="Input:\n1 2 3\n5\n\nOutput:\nfalse\n\nExplanation: Paths are 1+2=3 and 1+3=4, neither equals 5."
e3="Input:\n\n0\n\nOutput:\nfalse\n\nExplanation: Empty tree has no path."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Tree, Binary Tree, DFS",e1,e2,e3))
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
    public boolean hasPathSum(TreeNode root, int targetSum) {
        // Write your code here — DFS, subtract value as you go
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
            i++;
            if(i<a.length&&a[i]!=null){cur.right=new TreeNode(a[i]);q.offer(cur.right);}
            i++;
        }
        return r;
    }
    static void test(Integer[] a,int ts,boolean e,int tc,boolean h){
        boolean g=new CodeCoder().hasPathSum(build(a),ts);
        if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);
    }
    public static void main(String[] a){
        try{test(new Integer[]{5,4,8,11,null,13,4,7,2,null,null,null,1},22,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{1,2,3},5,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{},0,false,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1,2},2,false,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{1,2,3},3,true,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{-2,null,-3},-5,true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{1},1,true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7},10,true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7},8,false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{0,1,1},1,true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    bool hasPathSum(TreeNode* root, int targetSum) {
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
void test(vector<int*> a,int ts,bool e,int tc,bool h=false){
    bool g=CodeCoder().hasPathSum(build(a),ts);
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0,_m2=-2,_m3=-3,_m5=-5;
int* t1[]={&_5,&_4,&_8,&_11,NULL,&_13,&_4,&_7,&_2,NULL,NULL,NULL,&_1};try{test(t1,13,22,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3},5,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},0,false,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1,&_2},2,false,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_1,&_2},1,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3},3,true,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_m2,NULL,&_m3},-5,true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_1},1,true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},10,true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},8,false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_0,&_1,&_1},1,true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def hasPathSum(self, root, targetSum):
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

def test(a,ts,e,tc,h=False):
    g=CodeCoder().hasPathSum(build(a),ts)
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([5,4,8,11,None,13,4,7,2,None,None,None,1],22,True,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],5,False,2)
except:print("TC:2:FAIL:hidden")
try:test([],0,False,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2],2,False,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2],1,True,5)
except:print("TC:5:FAIL:hidden")
try:test([-2,None,-3],-5,True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1],1,True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,5,6,7],10,True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,3,4,5,6,7],8,False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0,1,1],1,True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function hasPathSum(root,targetSum){
    return false;
}
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
function test(a,ts,e,tc,h){if(h===undefined)h=false;
    const g=hasPathSum(build(a),ts);
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([5,4,8,11,null,13,4,7,2,null,null,null,1],22,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],5,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],0,false,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2],2,false,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2],1,true,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-2,null,-3],-5,true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1],1,true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],10,true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],8,false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0,1,1],1,true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
bool hasPathSum(struct TreeNode* root, int targetSum) {
    // Write your code here
    return false;
}
// USER_CODE_END

struct TreeNode{int val;struct TreeNode *left,*right;};

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
void run(int** a,int n,int ts,bool e,int tc,int h){
    bool g=hasPathSum(build(a,n),ts);
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_0=0,_m2=-2,_m3=-3;
int* t1[]={&_5,&_4,&_8,&_11,NULL,&_13,&_4,&_7,&_2,NULL,NULL,NULL,&_1};run(t1,13,22,true,1,0);
int* t2[]={&_1,&_2,&_3};run(t2,3,5,false,2,0);
run(NULL,0,0,false,3,0);
int* t4[]={&_1,&_2};run(t4,2,2,false,4,0);
int* t5[]={&_1,&_2};run(t5,2,1,true,5,0);
int* t6[]={&_1,&_2,&_3};run(t6,3,3,true,5,0);
int* t7[]={&_m2,NULL,&_m3};run(t7,3,-5,true,6,1);
int* t8[]={&_1};run(t8,1,1,true,7,1);
int* t9[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7};run(t9,7,10,true,8,1);
run(t9,7,8,false,9,1);
int* t10[]={&_0,&_1,&_1};run(t10,3,1,true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
