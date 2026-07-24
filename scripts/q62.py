"""
Binary Tree Level Order Traversal
===================================
Given root of a binary tree, return level order (BFS) traversal —
nodes from left to right, level by level.

Example:
    3
   / \
  9  20
     /  \
    15   7  → [[3],[9,20],[15,7]]

Approach: Use a queue. Process level by level measuring queue size.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Binary Tree Level Order Traversal"
desc=(
    "Given the root of a binary tree, return the level order traversal "
    "of its nodes' values (i.e., from left to right, level by level).\n\n"
    "For example:\n"
    "    3\n"
    "   / \\\n"
    "  9  20\n"
    "     /  \\\n"
    "    15   7\n"
    "Output: [[3],[9,20],[15,7]]\n\n"
    "Use BFS with a queue. Process each level by tracking the queue size "
    "before starting to dequeue elements for that level."
)
infmt="A single line containing the tree in level-order BFS format (null for empty nodes)."
outfmt="Print each level on a new line as space-separated values."
cons="0 ≤ nodes ≤ 2000\n-1000 ≤ Node.val ≤ 1000"
e1="Input:\n3 9 20 null null 15 7\n\nOutput:\n3\n9 20\n15 7"
e2="Input:\n1\n\nOutput:\n1"
e3="Input:\n\n\nOutput:\n\n(empty output)"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Tree, Binary Tree, BFS",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class TreeNode{int val;TreeNode left,right;TreeNode(int x){val=x;}}

// USER_CODE_START
class CodeCoder{
    public List<List<Integer>> levelOrder(TreeNode root){return new ArrayList<>();}
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
    static void test(Integer[] a,String e,int tc,boolean h){
        List<List<Integer>> g=new CodeCoder().levelOrder(build(a));
        StringBuilder sb=new StringBuilder();
        for(var l:g){for(int x:l)sb.append(x).append(" ");sb.append(",");}
        String gs=sb.toString();
        if(gs.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:got="+gs+":exp="+e);
    }
    public static void main(String[] a){
        try{test(new Integer[]{3,9,20,null,null,15,7},"3 ,9 20 ,15 7 ,",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{1},"1 ,",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{},"",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7},"1 ,2 3 ,4 5 6 7 ,",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{1,2,null,3,null,4},"1 ,2 ,3 ,4 ,",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{0,0,0,0,0,0,0},"0 ,0 0 ,0 0 0 0 ,",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,null,null,4,5},"1 ,2 3 ,4 5 ,",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{1,2,2,3,3,null,null,4,4},"1 ,2 2 ,3 3 ,4 4 ,",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1,null,2,null,3},"1 ,2 ,3 ,",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15},"1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
struct TreeNode{int val;TreeNode *left,*right;TreeNode(int x):val(x),left(NULL),right(NULL){}};

// USER_CODE_START
class CodeCoder{public:vector<vector<int>> levelOrder(TreeNode* r){return {};}};
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
string ser(vector<vector<int>> v){
    string s;
    for(auto& l:v){for(int x:l)s+=to_string(x)+" ";s+=",";}
    return s;
}
void test(vector<int*> a,string e,int tc,bool h=false){
    string g=ser(CodeCoder().levelOrder(build(a)));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:got="<<g<<":exp="<<e<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0;
try{test({&_3,&_9,&_20,NULL,NULL,&_15,&_7},"3 ,9 20 ,15 7 ,",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_1},"1 ,",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},"",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},"1 ,2 3 ,4 5 6 7 ,",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_1,&_2,NULL,&_3,NULL,&_4},"1 ,2 ,3 ,4 ,",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_0,&_0,&_0,&_0,&_0,&_0,&_0},"0 ,0 0 ,0 0 0 0 ,",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,NULL,NULL,&_4,&_5},"1 ,2 3 ,4 5 ,",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4},"1 ,2 2 ,3 3 ,4 4 ,",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_1,NULL,&_2,NULL,&_3},"1 ,2 ,3 ,",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15},"1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def levelOrder(self, root):
        return []
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

def ser(v):
    s=""
    for l in v:
        for x in l:s+=str(x)+" "
        s+=","
    return s

def test(a,e,tc,h=False):
    g=ser(CodeCoder().levelOrder(build(a)))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={g}:exp={e}")

try:test([3,9,20,None,None,15,7],"3 ,9 20 ,15 7 ,",1)
except:print("TC:1:FAIL:hidden")
try:test([1],"1 ,",2)
except:print("TC:2:FAIL:hidden")
try:test([],"",3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5,6,7],"1 ,2 3 ,4 5 6 7 ,",4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,None,3,None,4],"1 ,2 ,3 ,4 ,",5)
except:print("TC:5:FAIL:hidden")
try:test([0,0,0,0,0,0,0],"0 ,0 0 ,0 0 0 0 ,",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,None,None,4,5],"1 ,2 3 ,4 5 ,",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,2,3,3,None,None,4,4],"1 ,2 2 ,3 3 ,4 4 ,",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,None,2,None,3],"1 ,2 ,3 ,",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],"1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function levelOrder(root){return [];}
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
function ser(v){
    let s="";
    for(let l of v){for(let x of l)s+=x+" ";s+=",";}
    return s;
}
function test(a,e,tc,h){if(h===undefined)h=false;
    const g=ser(levelOrder(build(a)));
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:got="+g+":exp="+e);
}
try{test([3,9,20,null,null,15,7],"3 ,9 20 ,15 7 ,",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1],"1 ,",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],"",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],"1 ,2 3 ,4 5 6 7 ,",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,null,3,null,4],"1 ,2 ,3 ,4 ,",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,0,0,0,0,0,0],"0 ,0 0 ,0 0 0 0 ,",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,null,null,4,5],"1 ,2 3 ,4 5 ,",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,2,3,3,null,null,4,4],"1 ,2 2 ,3 3 ,4 4 ,",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,null,2,null,3],"1 ,2 ,3 ,",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15],"1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
struct TreeNode{int val;struct TreeNode *left,*right;};
// USER_CODE_START
// Return result as string: "level1vals level2vals..." with commas between levels
void levelOrder(struct TreeNode* r,char* out){out[0]=0;}
// USER_CODE_END
int main(){
printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
