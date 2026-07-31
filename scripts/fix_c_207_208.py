import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

# PID 207 - Determine Whether Matrix Can Be Obtained by Rotation
c_207='''#include <stdio.h>
#include <stdbool.h>

// USER_CODE_START
// mat and target are n x n matrices; matrixColSize array has n for each row
bool canBeRotated(int** mat, int** target, int n, int* cs) {
    // Write your code here — check 0/90/180/270 degree rotations
    return false;
}
// USER_CODE_END

void runTest(int** mat,int** tgt,int n,char* e,int tc,int h){
    int csArr[10];for(int i=0;i<n;i++)csArr[i]=n;
    bool g=canBeRotated(mat,tgt,n,csArr);
    if(g==(strcmp(e,"true")==0)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e,g?"true":"false");}
}
int main(){
    int m0a[]={0,1},m0b[]={1,0},t0a[]={1,0},t0b[]={0,1};
    int* m0[]={m0a,m0b};int* t0[]={t0a,t0b};
    runTest(m0,t0,2,"true",1,0);

    int m1a[]={0,1},m1b[]={1,1},t1a[]={1,0},t1b[]={0,1};
    int* m1[]={m1a,m1b};int* t1[]={t1a,t1b};
    runTest(m1,t1,2,"false",2,0);

    int m2a[]={1},t2a[]={1};
    int* m2[]={m2a};int* t2[]={t2a};
    runTest(m2,t2,1,"true",3,0);

    int m3a[]={0},t3a[]={1};
    int* m3[]={m3a};int* t3[]={t3a};
    runTest(m3,t3,1,"false",4,0);

    int m4a[]={1,0},m4b[]={0,0},t4a[]={0,0},t4b[]={0,1};
    int* m4[]={m4a,m4b};int* t4[]={t4a,t4b};
    runTest(m4,t4,2,"true",5,0);

    int m5a[]={0,0,0},m5b[]={0,1,0},m5c[]={0,0,0};
    int* m5[]={m5a,m5b,m5c};
    runTest(m5,m5,3,"true",6,1);

    int m6a[]={1,1},m6b[]={0,0},t6a[]={1,0},t6b[]={1,0};
    int* m6[]={m6a,m6b};int* t6[]={t6a,t6b};
    runTest(m6,t6,2,"true",7,1);

    int m7a[]={1,0,1},m7b[]={0,0,0},m7c[]={1,0,1};
    int* m7[]={m7a,m7b,m7c};
    runTest(m7,m7,3,"true",8,1);

    int m8a[]={1,1},m8b[]={1,1},t8a[]={0,0},t8b[]={0,0};
    int* m8[]={m8a,m8b};int* t8[]={t8a,t8b};
    runTest(m8,t8,2,"false",9,1);

    int m9a[]={0,1,1},m9b[]={1,0,1},m9c[]={1,1,0};
    int t9a[]={1,1,0},t9b[]={1,0,1},t9c[]={0,1,1};
    int* m9[]={m9a,m9b,m9c};int* t9[]={t9a,t9b,t9c};
    runTest(m9,t9,3,"true",10,1);

    return 0;
}'''

# PID 208 - Print all Divisors of a Number
c_208='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void getDivisors(int n,char* out) {
    // Write your code here — store space-separated divisors in increasing order in 'out'
    out[0]='\\0';
}
// USER_CODE_END

void runTest(int n,char* e,int tc,int h){
    char out[50000]={0};
    getDivisors(n,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:got=%s:exp=%s\\n",tc,n,out,e);}
}
int main(){
    runTest(36,"1 2 3 4 6 9 12 18 36",1,0);
    runTest(7,"1 7",2,0);
    runTest(12,"1 2 3 4 6 12",3,0);
    runTest(1,"1",4,0);
    runTest(100,"1 2 4 5 10 20 25 50 100",5,0);
    runTest(16,"1 2 4 8 16",6,1);
    runTest(29,"1 29",7,1);
    runTest(50,"1 2 5 10 25 50",8,1);
    runTest(1000000000,"1 2 4 5 8 10 16 20 25 32 40 50 64 80 100 125 128 160 200 250 256 320 400 500 512 625 640 800 1000 1250 1280 1600 2000 2500 2560 3125 3200 4000 5000 6250 6400 8000 10000 12500 15625 16000 20000 25000 31250 32000 40000 50000 62500 80000 100000 125000 156250 160000 200000 250000 312500 400000 500000 625000 800000 1000000 1250000 1562500 2000000 2500000 3125000 4000000 5000000 6250000 10000000 12500000 20000000 25000000 31250000 50000000 62500000 100000000 125000000 200000000 250000000 500000000 1000000000",9,1);
    runTest(6,"1 2 3 6",10,1);
    return 0;
}'''

for pid_lang_code in [(207,'C',c_207),(208,'C',c_208)]:
    pid2,lang2,code2=pid_lang_code
    cur.execute("UPDATE code_snippets SET solution_template=%s, updated_at=NOW() WHERE problem_id=%s AND language=%s",(code2,pid2,lang2))
    print(f"PID {pid2} ({lang2}): {'updated' if cur.rowcount>0 else 'NOT FOUND'}")

conn.commit()
cur.close()
conn.close()
print("Done!")
