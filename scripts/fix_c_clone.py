import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

c_code='''#include <stdio.h>
#include <stdlib.h>

struct Node {int val;struct Node** neighbors;int numNeighbors;};

// USER_CODE_START
// struct Node {int val;struct Node** neighbors;int numNeighbors;};
struct Node* cloneGraph(struct Node* s) {
    // Write your code here — DFS with hash map
    return NULL;
}
// USER_CODE_END

struct Node* createNode(int val){
    struct Node* n=malloc(sizeof(struct Node));n->val=val;n->neighbors=NULL;n->numNeighbors=0;return n;
}
void addNeighbor(struct Node* from,struct Node* to){
    from->neighbors=realloc(from->neighbors,(from->numNeighbors+1)*sizeof(struct Node*));
    from->neighbors[from->numNeighbors++]=to;
}
struct Node* buildGraph(int** adj,int n,int* found){
    if(n==0){*found=0;return NULL;}
    struct Node** nodes=malloc(n*sizeof(struct Node*));
    for(int i=0;i<n;i++)nodes[i]=createNode(i+1);
    for(int i=0;i<n;i++){
        if(adj[i][0]==-1)continue;
        for(int j=0;adj[i][j]!=-1;j++){
            int nb=adj[i][j]-1;
            addNeighbor(nodes[i],nodes[nb]);
        }
    }
    *found=1;
    if(n>0){struct Node* r=nodes[0];free(nodes);return r;}
    free(nodes);return NULL;
}
int graphEq(struct Node* a,struct Node* b,int* visited,int vSize){
    if(!a&&!b)return 1;
    if(!a||!b)return 0;
    if(a->val!=b->val)return 0;
    if(a==b)return 1;
    for(int i=0;i<vSize;i++)if(visited[i]==a->val)return 1;
    visited[vSize]=a->val;
    if(a->numNeighbors!=b->numNeighbors)return 0;
    for(int i=0;i<a->numNeighbors;i++){
        int ok=0;
        for(int j=0;j<b->numNeighbors;j++){
            if(a->neighbors[i]->val==b->neighbors[j]->val){ok=1;break;}
        }
        if(!ok)return 0;
    }
    for(int i=0;i<a->numNeighbors;i++){
        if(!graphEq(a->neighbors[i],b->neighbors[i],visited,vSize+1))return 0;
    }
    return 1;
}
void freeGraph(struct Node* n,int* freed,int fSize){
    if(!n)return;
    for(int i=0;i<fSize;i++)if(freed[i]==n->val)return;
    freed[fSize]=n->val;
    for(int i=0;i<n->numNeighbors;i++)freeGraph(n->neighbors[i],freed,fSize+1);
    free(n->neighbors);
}
void runTest(int** adj,int n,int tc,int hidden){
    int found;struct Node* orig=buildGraph(adj,n,&found);
    struct Node* clone=cloneGraph(orig);
    int vis[1000]={0};
    int ok=graphEq(orig,clone,vis,0);
    if(ok){if(hidden)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hidden)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}
    int f2[1000]={0};freeGraph(orig,f2,0);
    int f3[1000]={0};freeGraph(clone,f3,0);
}
int main(){
    int a1[][10]={{2,4,-1},{1,3,-1},{2,4,-1},{1,3,-1}};
    int* p1[]={(int*)a1[0],(int*)a1[1],(int*)a1[2],(int*)a1[3]};
    runTest(p1,4,1,0);

    int a2[][10]={{-1}};
    int* p2[]={(int*)a2[0]};
    runTest(p2,1,2,0);

    int a3[][10]={{2,-1},{1,-1}};
    int* p3[]={(int*)a3[0],(int*)a3[1]};
    runTest(p3,2,3,0);

    int a4[][10]={{2,3,-1},{1,3,-1},{1,2,-1}};
    int* p4[]={(int*)a4[0],(int*)a4[1],(int*)a4[2]};
    runTest(p4,3,4,0);

    int a5[][10]={{2,-1},{3,-1},{1,-1}};
    int* p5[]={(int*)a5[0],(int*)a5[1],(int*)a5[2]};
    runTest(p5,3,5,0);

    int a6[][10]={{2,3,4,5,-1},{1,3,4,5,-1},{1,2,4,5,-1},{1,2,3,5,-1},{1,2,3,4,-1}};
    int* p6[]={(int*)a6[0],(int*)a6[1],(int*)a6[2],(int*)a6[3],(int*)a6[4]};
    runTest(p6,5,6,1);

    int a7[][10]={{2,-1},{1,-1},{4,-1},{3,-1}};
    int* p7[]={(int*)a7[0],(int*)a7[1],(int*)a7[2],(int*)a7[3]};
    runTest(p7,4,7,1);

    int a8[][10]={{2,4,6,-1},{1,3,5,-1},{2,4,6,-1},{1,3,5,-1},{2,4,6,-1},{1,3,5,-1}};
    int* p8[]={(int*)a8[0],(int*)a8[1],(int*)a8[2],(int*)a8[3],(int*)a8[4],(int*)a8[5]};
    runTest(p8,6,8,1);

    int a9[][10]={{2,-1},{1,-1},{2,-1},{1,-1}};
    int* p9[]={(int*)a9[0],(int*)a9[1],(int*)a9[2],(int*)a9[3]};
    runTest(p9,4,9,1);

    int a10[][10]={{2,3,-1},{1,4,-1},{1,4,-1},{2,3,-1}};
    int* p10[]={(int*)a10[0],(int*)a10[1],(int*)a10[2],(int*)a10[3]};
    runTest(p10,4,10,1);

    return 0;
}'''

cur.execute("UPDATE code_snippets SET solution_template=%s, updated_at=NOW() WHERE problem_id=165 AND language='C'",(c_code,))
print(f"C harness for Clone Graph (pid=165): {'updated' if cur.rowcount>0 else 'NOT FOUND'}")

conn.commit()
cur.close()
conn.close()
