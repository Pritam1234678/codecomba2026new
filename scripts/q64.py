"""
Invert Binary Tree
====================
Given the root of a binary tree, invert the tree and return its root.
Inverting means swapping the left and right children of every node.

Example:
    4                   4
   / \                 / \
  2   7      →        7   2
 / \ / \             / \ / \
1  3 6 9            9  6 3  1

Approach: Recursively swap left and right children for each node.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Invert Binary Tree"
desc=(
    "Given the root of a binary tree, invert the tree and return its root.\n\n"
    "Inverting a binary tree means swapping the left and right children of every "
    "node in the tree recursively.\n\n"
    "For example:\n"
    "        4             4\n"
    "       / \\           / \\\n"
    "      2   7    →    7   2\n"
    "     / \\ / \\       / \\ / \\\n"
    "    1  3 6  9      9  6 3  1\n\n"
    "The output should be the level-order traversal of the inverted tree. "
    "A simple recursive approach: for each node, swap its children, then "
    "recursively invert the left and right subtrees."
)
infmt="A single line containing the tree in level-order BFS format (null for empty)."
outfmt="Print the level-order traversal of the inverted tree as space-separated values."
cons="0 ≤ nodes ≤ 100\n-100 ≤ Node.val ≤ 100"
e1="Input:\n4 2 7 1 3 6 9\n\nOutput:\n4 7 2 9 6 3 1"
e2="Input:\n2 1 3\n\nOutput:\n2 3 1"
e3="Input:\n\n\nOutput:\n\n(empty output)"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Tree, Binary Tree, DFS, Recursion",e1,e2,e3))
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
    public TreeNode invertTree(TreeNode root) {
        // Write your code here — swap children recursively
        return root;
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
    static String ser(TreeNode r){
        if(r==null)return"";
        StringBuilder sb=new StringBuilder();
        Queue<TreeNode> q=new LinkedList<>();q.offer(r);
        while(!q.isEmpty()){
            TreeNode cur=q.poll();
            if(cur==null){sb.append("null ");continue;}
            sb.append(cur.val).append(" ");
            q.offer(cur.left);q.offer(cur.right);
        }
        String s=sb.toString();
        while(s.endsWith("null "))s=s.substring(0,s.length()-5);
        return s.trim();
    }
    static void test(Integer[] a,String e,int tc,boolean h){
        String g=ser(new CodeCoder().invertTree(build(a)));
        if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:exp="+e+":got="+g);
    }
    public static void main(String[] a){
        try{test(new Integer[]{4,2,7,1,3,6,9},"4 7 2 9 6 3 1",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new Integer[]{2,1,3},"2 3 1",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new Integer[]{},"",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new Integer[]{1},"1",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,4,5,6,7},"1 3 2 7 6 5 4",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new Integer[]{1,2,null,3},"1 null 2 null 3",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new Integer[]{1,null,2,null,3},"1 null 2 null 3",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new Integer[]{0,0,0,0,0,0,0},"0 0 0 0 0 0 0",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new Integer[]{1,2,2,3,3,3,3},"1 2 2 3 3 3 3",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new Integer[]{1,2,3,null,null,4,5},"1 3 2 5 4",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
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
    TreeNode* invertTree(TreeNode* root) {
        // Write your code here — swap children recursively
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
string ser(TreeNode* r){
    if(!r)return"";
    string s;queue<TreeNode*> q;q.push(r);
    while(!q.empty()){
        TreeNode* cur=q.front();q.pop();
        if(!cur){s+="null ";continue;}
        s+=to_string(cur->val)+" ";q.push(cur->left);q.push(cur->right);
    }
    while(s.size()>=5&&s.substr(s.size()-5)=="null ")s=s.substr(0,s.size()-5);
    if(!s.empty()&&s.back()==' ')s.pop_back();
    return s;
}
void test(vector<int*> a,string e,int tc,bool h=false){
    string g=ser(CodeCoder().invertTree(build(a)));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_0=0,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15;
try{test({&_4,&_2,&_7,&_1,&_3,&_6,&_9},"4 7 2 9 6 3 1",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_2,&_1,&_3},"2 3 1",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},"",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1},"1",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},"1 3 2 7 6 5 4",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_1,&_2,NULL,&_3},"1 null 2 null 3",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_1,NULL,&_2,NULL,&_3},"1 null 2 null 3",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_0,&_0,&_0,&_0,&_0,&_0,&_0},"0 0 0 0 0 0 0",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_1,&_2,&_2,&_3,&_3,&_3,&_3},"1 2 2 3 3 3 3",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,NULL,NULL,&_4,&_5},"1 3 2 5 4",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class TreeNode:
#     def __init__(self, val=0, left=None, right=None):
#         self.val = val; self.left = left; self.right = right
class CodeCoder:
    def invertTree(self, root):
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

def ser(r):
    if not r: return ""
    s=[];q=[r]
    while q:
        cur=q.pop(0)
        if cur is None:s.append("null");continue
        s.append(str(cur.val))
        q.append(cur.left);q.append(cur.right)
    while s and s[-1]=="null":s.pop()
    return " ".join(s)

def test(a,e,tc,h=False):
    g=ser(CodeCoder().invertTree(build(a)))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([4,2,7,1,3,6,9],"4 7 2 9 6 3 1",1)
except:print("TC:1:FAIL:hidden")
try:test([2,1,3],"2 3 1",2)
except:print("TC:2:FAIL:hidden")
try:test([],"",3)
except:print("TC:3:FAIL:hidden")
try:test([1],"1",4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6,7],"1 3 2 7 6 5 4",5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,None,3],"1 null 2 null 3",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,None,2,None,3],"1 null 2 null 3",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0,0,0,0,0,0,0],"0 0 0 0 0 0 0",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,2,3,3,3,3],"1 2 2 3 3 3 3",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3,None,None,4,5],"1 3 2 5 4",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class TreeNode{
//     constructor(val,left,right){this.val=val;this.left=left||null;this.right=right||null;}
// }
function invertTree(root){
    return root;
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
function ser(r){
    if(!r)return"";
    let s=[],q=[r];
    while(q.length){
        let cur=q.shift();
        if(cur===null){s.push("null");continue;}
        s.push(String(cur.val));q.push(cur.left);q.push(cur.right);
    }
    while(s.length&&s[s.length-1]==="null")s.pop();
    return s.join(" ");
}
function test(a,e,tc,h){if(h===undefined)h=false;
    const g=ser(invertTree(build(a)));
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([4,2,7,1,3,6,9],"4 7 2 9 6 3 1",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,1,3],"2 3 1",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],"",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],"1",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6,7],"1 3 2 7 6 5 4",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,null,3],"1 null 2 null 3",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,null,2,null,3],"1 null 2 null 3",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0,0,0,0,0,0,0],"0 0 0 0 0 0 0",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,2,3,3,3,3],"1 2 2 3 3 3 3",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3,null,null,4,5],"1 3 2 5 4",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
struct TreeNode* invertTree(struct TreeNode* root) {
    // Write your code here — swap children recursively
    return root;
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
void ser(struct TreeNode* r,char* out){
    out[0]=0;
    if(!r)return;
    struct TreeNode** q=malloc(200*sizeof(struct TreeNode*));int f=0,rear=0;
    q[rear++]=r;
    while(f<rear){
        struct TreeNode* cur=q[f++];
        if(!cur){strcat(out,"null ");continue;}
        char buf[20];sprintf(buf,"%d ",cur->val);strcat(out,buf);
        q[rear++]=cur->left;q[rear++]=cur->right;
    }
    free(q);
    int len=strlen(out);
    while(len>=5&&strcmp(out+len-5,"null ")==0){len-=5;out[len]=0;}
    if(len>0&&out[len-1]==' ')out[len-1]=0;
}
void run(int** a,int n,char* e,int tc,int h){
    char out[20000]={0};
    struct TreeNode* root=build(a,n);
    struct TreeNode* inv=invertTree(root);
    ser(inv,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e,out);}
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_0=0;
int* t1[]={&_4,&_2,&_7,&_1,&_3,&_6,&_9};run(t1,7,"4 7 2 9 6 3 1",1,0);
int* t2[]={&_2,&_1,&_3};run(t2,3,"2 3 1",2,0);
run(NULL,0,"",3,0);
int* t4[]={&_1};run(t4,1,"1",4,0);
int* t5[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7};run(t5,7,"1 3 2 7 6 5 4",5,0);
int* t6[]={&_1,&_2,NULL,&_3};run(t6,4,"1 null 2 null 3",6,1);
int* t7[]={&_1,NULL,&_2,NULL,&_3};run(t7,5,"1 null 2 null 3",7,1);
int* t8[]={&_0,&_0,&_0,&_0,&_0,&_0,&_0};run(t8,7,"0 0 0 0 0 0 0",8,1);
int* t9[]={&_1,&_2,&_2,&_3,&_3,&_3,&_3};run(t9,7,"1 2 2 3 3 3 3",9,1);
int* t10[]={&_1,&_2,&_3,NULL,NULL,&_4,&_5};run(t10,7,"1 3 2 5 4",10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
