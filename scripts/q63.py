"""
Diameter of Binary Tree
=========================
Given the root of a binary tree, return the length of the diameter.
The diameter is the longest path between any two nodes, measured by
the number of edges along the path. It may or may not pass through root.

Example:
    1
   / \
  2   3
 / \
4   5
Diameter = 3 (path 4→2→1→3 or 5→2→1→3, edges = 3)

Approach: For each node, compute left and right depth.
The diameter through that node = leftDepth + rightDepth.
Track max globally.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Diameter of Binary Tree"
desc=(
    "Given the root of a binary tree, return the length of the diameter.\n\n"
    "The diameter is the length of the longest path between any two nodes in the tree. "
    "This path may or may not pass through the root. The length is measured by the "
    "number of edges (NOT nodes).\n\n"
    "For example:\n"
    "        1\n"
    "       / \\\n"
    "      2   3\n"
    "     / \\\n"
    "    4   5\n"
    "The longest path is 4→2→1→3 (or 5→2→1→3) which has 3 edges, so diameter = 3.\n\n"
    "For each node, compute the depth of left and right subtrees recursively. "
    "The diameter through that node = leftDepth + rightDepth. Keep track of the "
    "maximum diameter seen across all nodes."
)
infmt="A single line containing the tree in level-order BFS format (null for empty)."
outfmt="Print the diameter (number of edges)."
cons="1 ≤ nodes ≤ 10^4\n-100 ≤ Node.val ≤ 100"
e1="Input:\n1 2 3 4 5\n\nOutput:\n3\n\nExplanation: Path 4→2→1→3 has 3 edges."
e2="Input:\n1 2\n\nOutput:\n1\n\nExplanation: Path 1→2 has 1 edge."
e3="Input:\n1\n\nOutput:\n0\n\nExplanation: Single node, 0 edges."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"EASY",True,"Tree, Binary Tree, DFS",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}

// USER_CODE_START
class CodeCoder{
    public int diameterOfBinaryTree(TreeNode root){return 0;}
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
        int g=new CodeCoder().diameterOfBinaryTree(build(a));
        if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);
    }
    public static void main(String[] a){
        try{test(new Integer[]{1,2,3,4,5},3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{1,2},1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{1},0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7},3,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,null,null,4,5,6,7},4,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,null,null,5,null,null,6,7},5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{1,2,null,3,null,4,null,5},4,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,null,null,6,7,8,9},5,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1,2,2,3,3,null,null,4,4},4,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},5,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

/* Definition for a binary tree node.
 * class TreeNode {
 *     public:
 *         int val;
 *         TreeNode *left;
 *         TreeNode *right;
 *         TreeNode(int x) : val(x), left(NULL), right(NULL) {}
 * }; */

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
    int diameterOfBinaryTree(TreeNode* root) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

class TreeNode {
public:
    int val;
    TreeNode *left, *right;
    TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

TreeNode* build(vector<int*> a) {
    if (a.empty() || !a[0]) return NULL;
    TreeNode* r = new TreeNode(*a[0]);
    queue<TreeNode*> q; q.push(r);
    int i = 1;
    while (!q.empty() && i < (int)a.size()) {
        TreeNode* cur = q.front(); q.pop();
        if (a[i]) { cur->left = new TreeNode(*a[i]); q.push(cur->left); }
        i++;
        if (i < (int)a.size() && a[i]) { cur->right = new TreeNode(*a[i]); q.push(cur->right); }
        i++;
    }
    return r;
}
void test(vector<int*> a, int e, int tc, bool h = false) {
    int g = CodeCoder().diameterOfBinaryTree(build(a));
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else cout << "TC:" << tc << ":FAIL:exp=" << e << ":got=" << g << "\\n";
}
int main() {
    int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0;
    try { test({&_1,&_2,&_3,&_4,&_5},3,1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({&_1,&_2},1,2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({&_1},0,3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},3,4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_3,NULL,NULL,&_4,&_5,&_6,&_7},4,5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_3,&_4,NULL,NULL,&_5,NULL,NULL,&_6,&_7},5,6,true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5},4,7,true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6,&_7,&_8,&_9},5,8,true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4},4,9,true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15},5,10,true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def diameterOfBinaryTree(self, root):
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
    g=CodeCoder().diameterOfBinaryTree(build(a))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([1,2,3,4,5],3,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2],1,2)
except:print("TC:2:FAIL:hidden")
try:test([1],0,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5,6,7],3,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,None,None,4,5,6,7],4,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,None,None,5,None,None,6,7],5,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,None,3,None,4,None,5],4,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,5,None,None,6,7,8,9],5,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,2,3,3,None,None,4,4],4,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],5,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function diameterOfBinaryTree(root){return 0;}
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
    const g=diameterOfBinaryTree(build(a));
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([1,2,3,4,5],3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2],1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],3,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,null,null,4,5,6,7],4,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,null,null,5,null,null,6,7],5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,null,3,null,4,null,5],4,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,5,null,null,6,7,8,9],5,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,2,3,3,null,null,4,4],4,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],5,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct TreeNode{int val;struct TreeNode *left,*right;};

/* Definition for a binary tree node.
 * struct TreeNode {
 *     int val;
 *     struct TreeNode *left;
 *     struct TreeNode *right;
 * }; */

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
int diameterOfBinaryTree(struct TreeNode* root) {
    // Write your code here
    return 0;
}
// USER_CODE_END

// ----- Driver (hidden) -----

struct TreeNode* build(int** a, int n) {
    if (!n || !a || !a[0]) return NULL;
    struct TreeNode** q = malloc(n * sizeof(struct TreeNode*));
    int front = 0, rear = 0;
    struct TreeNode* r = malloc(sizeof(struct TreeNode));
    r->val = *a[0]; r->left = r->right = NULL;
    q[rear++] = r; int i = 1;
    while (front < rear && i < n) {
        struct TreeNode* cur = q[front++];
        if (a[i]) {
            struct TreeNode* l = malloc(sizeof(struct TreeNode));
            l->val = *a[i]; l->left = l->right = NULL;
            cur->left = l; q[rear++] = l;
        }
        i++;
        if (i < n && a[i]) {
            struct TreeNode* ri = malloc(sizeof(struct TreeNode));
            ri->val = *a[i]; ri->left = ri->right = NULL;
            cur->right = ri; q[rear++] = ri;
        }
        i++;
    }
    free(q); return r;
}

void runTest(int** a, int n, int e, int tc, int hidden) {
    struct TreeNode* root = build(a, n);
    int g = diameterOfBinaryTree(root);
    if (g == e) {
        if (hidden) printf("TC:%d:PASS:hidden\\n", tc);
        else printf("TC:%d:PASS\\n", tc);
    } else {
        if (hidden) printf("TC:%d:FAIL:hidden\\n", tc);
        else printf("TC:%d:FAIL:exp=%d:got=%d\\n", tc, e, g);
    }
}

int main() {
    int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0;
    int* t1[]={&_1,&_2,&_3,&_4,&_5}; runTest(t1,5,3,1,0);
    int* t2[]={&_1,&_2}; runTest(t2,2,1,2,0);
    int* t3[]={&_1}; runTest(t3,1,0,3,0);
    int* t4[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7}; runTest(t4,7,3,4,0);
    int* t5[]={&_1,&_2,&_3,NULL,NULL,&_4,&_5,&_6,&_7}; runTest(t5,9,4,5,0);
    int* t6[]={&_1,&_2,&_3,&_4,NULL,NULL,&_5,NULL,NULL,&_6,&_7}; runTest(t6,11,5,6,1);
    int* t7[]={&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5}; runTest(t7,8,4,7,1);
    int* t8[]={&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6,&_7,&_8,&_9}; runTest(t8,11,5,8,1);
    int* t9[]={&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4}; runTest(t9,9,4,9,1);
    int* t10[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15}; runTest(t10,15,5,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
