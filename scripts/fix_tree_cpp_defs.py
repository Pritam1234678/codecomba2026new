import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

# PID 150 — Diameter (C)
c1='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
int diameterOfBinaryTree(struct TreeNode* root) { return 0; }
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
void run(int** a,int n,int e,int tc,int h){
    int g=diameterOfBinaryTree(build(a,n));
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15;
int* t1[]={&_1,&_2,&_3,&_4,&_5};run(t1,5,3,1,0);
int* t2[]={&_1,&_2};run(t2,2,1,2,0);
int* t3[]={&_1};run(t3,1,0,3,0);
int* t4[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7};run(t4,7,3,4,0);
int* t5[]={&_1,&_2,&_3,NULL,NULL,&_4,&_5,&_6,&_7};run(t5,9,4,5,0);
int* t6[]={&_1,&_2,&_3,&_4,NULL,NULL,&_5,NULL,NULL,&_6,&_7};run(t6,11,5,6,1);
int* t7[]={&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5};run(t7,8,4,7,1);
int* t8[]={&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6,&_7,&_8,&_9};run(t8,11,5,8,1);
int* t9[]={&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4};run(t9,9,4,9,1);
int* t10[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15};run(t10,15,5,10,1);
return 0;}'''

# PID 150 — Diameter (CPP)
cpp1='''#include <bits/stdc++.h>
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
    int diameterOfBinaryTree(TreeNode* root) { return 0; }
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
void test(vector<int*> a,int e,int tc,bool h=false){
    int g=CodeCoder().diameterOfBinaryTree(build(a));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15;
try{test({&_1,&_2,&_3,&_4,&_5},3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({&_1,&_2},1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({&_1},0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7},3,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,NULL,NULL,&_4,&_5,&_6,&_7},4,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,NULL,NULL,&_5,NULL,NULL,&_6,&_7},5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({&_1,&_2,NULL,&_3,NULL,&_4,NULL,&_5},4,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,NULL,NULL,&_6,&_7,&_8,&_9},5,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4},4,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15},5,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

# PID 148 — Max Depth (C)
c2='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
int maxDepth(struct TreeNode* root) { return 0; }
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

# PID 148 — Max Depth (CPP)
cpp2='''#include <bits/stdc++.h>
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
    int maxDepth(TreeNode* root) { return 0; }
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
void test(vector<int*> a,int e,int tc,bool h=false){
    int g=CodeCoder().maxDepth(build(a));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";
}
int main(){
int _3=3,_9=9,_20=20,_15=15,_7=7,_1=1,_2=2,_4=4,_5=5,_6=6,_0=0,_8=8,_10=10,_11=11,_12=12,_13=13,_14=14,_15_=15;
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

# PID 149 — Level Order (C)
c3='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
// struct TreeNode {
//     int val;
//     struct TreeNode *left;
//     struct TreeNode *right;
// };
void levelOrder(struct TreeNode* root, char* out) {
    out[0]='\\0';
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
void run(int** a,int n,char* e,int tc,int h){
    char out[20000]={0};levelOrder(build(a,n),out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:got=%s:exp=%s\\n",tc,out,e);}
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0,_20=20,_9_=9,_15_=15,_7_=7;
int* t1[]={&_3,&_9_,&_20,NULL,NULL,&_15_,&_7_};run(t1,7,"3 ,9 20 ,15 7 ,",1,0);
int* t2[]={&_1};run(t2,1,"1 ,",2,0);
run(NULL,0,"",3,0);
int* t4[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7};run(t4,7,"1 ,2 3 ,4 5 6 7 ,",4,0);
int* t5[]={&_1,&_2,NULL,&_3,NULL,&_4};run(t5,6,"1 ,2 ,3 ,4 ,",5,0);
int* t6[]={&_0,&_0,&_0,&_0,&_0,&_0,&_0};run(t6,7,"0 ,0 0 ,0 0 0 0 ,",6,1);
int* t7[]={&_1,&_2,&_3,NULL,NULL,&_4,&_5};run(t7,7,"1 ,2 3 ,4 5 ,",7,1);
int* t8[]={&_1,&_2,&_2,&_3,&_3,NULL,NULL,&_4,&_4};run(t8,9,"1 ,2 2 ,3 3 ,4 4 ,",8,1);
int* t9[]={&_1,NULL,&_2,NULL,&_3};run(t9,5,"1 ,2 ,3 ,",9,1);
int* t10[]={&_1,&_2,&_3,&_4,&_5,&_6,&_7,&_8,&_9,&_10,&_11,&_12,&_13,&_14,&_15};run(t10,15,"1 ,2 3 ,4 5 6 7 ,8 9 10 11 12 13 14 15 ,",10,1);
return 0;}'''

# PID 149 — Level Order (CPP)
cpp3='''#include <bits/stdc++.h>
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
    vector<vector<int>> levelOrder(TreeNode* root) { return {}; }
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
string ser(vector<vector<int>> v){string s;for(auto& l:v){for(int x:l)s+=to_string(x)+" ";s+=",";}return s;}
void test(vector<int*> a,string e,int tc,bool h=false){
    string g=ser(CodeCoder().levelOrder(build(a)));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:got="<<g<<":exp="<<e<<"\\n";
}
int main(){
int _1=1,_2=2,_3=3,_4=4,_5=5,_6=6,_7=7,_8=8,_9=9,_10=10,_11=11,_12=12,_13=13,_14=14,_15=15,_0=0,_20=20,_9_=9,_15_=15,_7_=7;
try{test({&_3,&_9_,&_20,NULL,NULL,&_15_,&_7_},"3 ,9 20 ,15 7 ,",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
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

for pid_lang_code in [(150,'C',c1),(150,'CPP',cpp1),(148,'C',c2),(148,'CPP',cpp2),(149,'C',c3),(149,'CPP',cpp3)]:
    pid2, lang2, code2 = pid_lang_code
    cur.execute("UPDATE code_snippets SET solution_template=%s, updated_at=NOW() WHERE problem_id=%s AND language=%s", (code2, pid2, lang2))
    print(f"PID {pid2} ({lang2}): {'updated' if cur.rowcount > 0 else 'NOT FOUND'}")

conn.commit()
cur.close()
conn.close()
print("Done!")
