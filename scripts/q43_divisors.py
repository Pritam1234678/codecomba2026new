"""
Print all Divisors of a Number
=================================
Given an integer n, print all its divisors in increasing order.

Examples:
  n = 36 → 1 2 3 4 6 9 12 18 36
  n = 7 → 1 7 (prime)

Efficient: iterate i from 1 to sqrt(n). If i divides n, i is a divisor and
n/i is also a divisor. Collect and sort.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Print all Divisors of a Number"
desc=(
    "Given an integer n, print all its divisors (factors) in increasing order.\n\n"
    "A divisor of n is any integer d such that n is divisible by d (n % d == 0).\n\n"
    "For example:\n"
    "n = 36 → divisors: 1, 2, 3, 4, 6, 9, 12, 18, 36\n"
    "n = 7 → divisors: 1, 7 (since 7 is prime)\n\n"
    "Efficient O(sqrt(n)) approach: iterate i from 1 to sqrt(n). If i divides n, "
    "both i and n/i are divisors. Collect them and sort in increasing order."
)
infmt="Single line containing integer n."
outfmt="Print all divisors separated by spaces in increasing order."
cons="1 ≤ n ≤ 10^9"
e1="Input:\n36\n\nOutput:\n1 2 3 4 6 9 12 18 36"
e2="Input:\n7\n\nOutput:\n1 7"
e3="Input:\n12\n\nOutput:\n1 2 3 4 6 12"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Math, Number Theory",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String getDivisors(int n) {
        // Write your code here — return space-separated divisors in increasing order
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(int n,String e,int tc,boolean h){String g=new CodeCoder().getDivisors(n);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(36,"1 2 3 4 6 9 12 18 36",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(7,"1 7",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(12,"1 2 3 4 6 12",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,"1",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(100,"1 2 4 5 10 20 25 50 100",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(16,"1 2 4 8 16",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(29,"1 29",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(50,"1 2 5 10 25 50",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(1000000000,"1 2 4 5 8 10 16 20 25 32 40 50 64 80 100 125 128 160 200 250 256 320 400 500 512 625 640 800 1000 1250 1280 1600 2000 2500 2560 3125 3200 4000 5000 6250 6400 8000 10000 12500 15625 16000 20000 25000 31250 32000 40000 50000 62500 80000 100000 125000 156250 160000 200000 250000 312500 400000 500000 625000 800000 1000000 1250000 1562500 2000000 2500000 3125000 4000000 5000000 6250000 10000000 12500000 20000000 25000000 31250000 50000000 62500000 100000000 125000000 200000000 250000000 500000000 1000000000",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(6,"1 2 3 6",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string getDivisors(int n){return "";}};
// USER_CODE_END
void test(int n,string e,int tc,bool h=false){string g=CodeCoder().getDivisors(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(36,"1 2 3 4 6 9 12 18 36",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(7,"1 7",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(12,"1 2 3 4 6 12",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,"1",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(100,"1 2 4 5 10 20 25 50 100",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(16,"1 2 4 8 16",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(29,"1 29",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(50,"1 2 5 10 25 50",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(1000000000,"1 2 4 5 8 10 16 20 25 32 40 50 64 80 100 125 128 160 200 250 256 320 400 500 512 625 640 800 1000 1250 1280 1600 2000 2500 2560 3125 3200 4000 5000 6250 6400 8000 10000 12500 15625 16000 20000 25000 31250 32000 40000 50000 62500 80000 100000 125000 156250 160000 200000 250000 312500 400000 500000 625000 800000 1000000 1250000 1562500 2000000 2500000 3125000 4000000 5000000 6250000 10000000 12500000 20000000 25000000 31250000 50000000 62500000 100000000 125000000 200000000 250000000 500000000 1000000000",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(6,"1 2 3 6",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def getDivisors(self, n):
        return ""
# USER_CODE_END
def test(n,e,tc,h=False):g=CodeCoder().getDivisors(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={repr(e)}:got={repr(g)}"))
try:test(36,"1 2 3 4 6 9 12 18 36",1)
except:print("TC:1:FAIL:hidden")
try:test(7,"1 7",2)
except:print("TC:2:FAIL:hidden")
try:test(12,"1 2 3 4 6 12",3)
except:print("TC:3:FAIL:hidden")
try:test(1,"1",4)
except:print("TC:4:FAIL:hidden")
try:test(100,"1 2 4 5 10 20 25 50 100",5)
except:print("TC:5:FAIL:hidden")
try:test(16,"1 2 4 8 16",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(29,"1 29",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(50,"1 2 5 10 25 50",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(1000000000,"1 2 4 5 8 10 16 20 25 32 40 50 64 80 100 125 128 160 200 250 256 320 400 500 512 625 640 800 1000 1250 1280 1600 2000 2500 2560 3125 3200 4000 5000 6250 6400 8000 10000 12500 15625 16000 20000 25000 31250 32000 40000 50000 62500 80000 100000 125000 156250 160000 200000 250000 312500 400000 500000 625000 800000 1000000 1250000 1562500 2000000 2500000 3125000 4000000 5000000 6250000 10000000 12500000 20000000 25000000 31250000 50000000 62500000 100000000 125000000 200000000 250000000 500000000 1000000000",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(6,"1 2 3 6",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function getDivisors(n) { return ""; }
// USER_CODE_END
function test(n,e,tc,h){if(h===undefined)h=false;const g=getDivisors(n);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test(36,"1 2 3 4 6 9 12 18 36",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(7,"1 7",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(12,"1 2 3 4 6 12",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,"1",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(100,"1 2 4 5 10 20 25 50 100",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(16,"1 2 4 8 16",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(29,"1 29",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(50,"1 2 5 10 25 50",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(1000000000,"1 2 4 5 8 10 16 20 25 32 40 50 64 80 100 125 128 160 200 250 256 320 400 500 512 625 640 800 1000 1250 1280 1600 2000 2500 2560 3125 3200 4000 5000 6250 6400 8000 10000 12500 15625 16000 20000 25000 31250 32000 40000 50000 62500 80000 100000 125000 156250 160000 200000 250000 312500 400000 500000 625000 800000 1000000 1250000 1562500 2000000 2500000 3125000 4000000 5000000 6250000 10000000 12500000 20000000 25000000 31250000 50000000 62500000 100000000 125000000 200000000 250000000 500000000 1000000000",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(6,"1 2 3 6",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

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

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
