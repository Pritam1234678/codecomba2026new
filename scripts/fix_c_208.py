import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

c_code='''#include <stdio.h>
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

cur.execute("UPDATE code_snippets SET solution_template=%s, updated_at=NOW() WHERE problem_id=208 AND language='C'",(c_code,))
print(f"PID 208 (C): {'updated' if cur.rowcount>0 else 'NOT FOUND'}")
conn.commit()
cur.close()
conn.close()
print("Done!")
