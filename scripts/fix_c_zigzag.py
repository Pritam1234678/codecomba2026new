import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void zigzagTraverse(int** m,int rs,int* cs,char* out) {
    // Write your code here — even rows L->R, odd rows R->L, space-separated into 'out'
    out[0]='\\0';
}
// USER_CODE_END

void runTest(int** m,int rs,int cs,char* e,int tc,int h){
    char out[20000]={0};
    zigzagTraverse(m,rs,&cs,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:got=%s:exp=%s\\n",tc,out,e);}
}
int main(){
    int r0a[]={1,2,3},r0b[]={4,5,6},r0c[]={7,8,9};
    int* m0[]={r0a,r0b,r0c};
    runTest(m0,3,3,"1 2 3 6 5 4 7 8 9",1,0);

    int r1a[]={1,2,3},r1b[]={4,5,6};
    int* m1[]={r1a,r1b};
    runTest(m1,2,3,"1 2 3 6 5 4",2,0);

    int r2a[]={5};
    int* m2[]={r2a};
    runTest(m2,1,1,"5",3,0);

    int r3a[]={1},r3b[]={2},r3c[]={3};
    int* m3[]={r3a,r3b,r3c};
    runTest(m3,3,1,"1 2 3",4,0);

    int r4a[]={1,2};
    int* m4[]={r4a};
    runTest(m4,1,2,"1 2",5,0);

    int r5a[]={1,2,3,4},r5b[]={5,6,7,8},r5c[]={9,10,11,12};
    int* m5[]={r5a,r5b,r5c};
    runTest(m5,3,4,"1 2 3 4 8 7 6 5 9 10 11 12",6,1);

    int r6a[]={-1,-2},r6b[]={-3,-4};
    int* m6[]={r6a,r6b};
    runTest(m6,2,2,"-1 -2 -4 -3",7,1);

    int r7a[]={0,0},r7b[]={0,0},r7c[]={0,0};
    int* m7[]={r7a,r7b,r7c};
    runTest(m7,3,2,"0 0 0 0 0 0",8,1);

    int r8a[]={1,2,3};
    int* m8[]={r8a};
    runTest(m8,1,3,"1 2 3",9,1);

    int r9a[]={10,20},r9b[]={30,40},r9c[]={50,60};
    int* m9[]={r9a,r9b,r9c};
    runTest(m9,3,2,"10 20 40 30 50 60",10,1);

    return 0;
}'''

cur.execute("UPDATE code_snippets SET solution_template=%s, updated_at=NOW() WHERE problem_id=206 AND language='C'",(c_code,))
print(f"ZigZag Matrix C harness (pid=206): {'updated' if cur.rowcount>0 else 'NOT FOUND'}")
conn.commit()
cur.close()
conn.close()
