"""
Alternates In Array
=====================
Given an array arr of size n, print all elements at even indices (0, 2, 4, ...)
i.e., print alternate elements starting from the first element.

Examples:
  arr = [10, 20, 30, 40, 50] → 10, 30, 50 (indices 0, 2, 4)
  arr = [5, 15, 25] → 5, 25 (indices 0, 2)

Simply loop with i = 0; i < n; i += 2.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Alternates In Array"
desc=(
    "Given an array arr of size n, print all elements at even indices "
    "(0, 2, 4, ...) — which are the alternate elements starting from the first.\n\n"
    "For example:\n"
    "arr = [10, 20, 30, 40, 50] → indices 0 (10), 2 (30), 4 (50) → output: 10 30 50\n"
    "arr = [5, 15, 25] → indices 0 (5), 2 (25) → output: 5 25\n\n"
    "Simply iterate the array with a step of 2 starting from index 0."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print alternate elements separated by spaces."
cons="1 ≤ n ≤ 1000\n-10^6 ≤ arr[i] ≤ 10^6"
e1="Input:\n5\n10 20 30 40 50\n\nOutput:\n10 30 50"
e2="Input:\n3\n5 15 25\n\nOutput:\n5 25"
e3="Input:\n1\n100\n\nOutput:\n100"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public String getAlternates(int[] arr) {
        // Write your code here — return space-separated string of alternate elements
        return "";
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,String e,int tc,boolean h){String g=new CodeCoder().getAlternates(a);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{10,20,30,40,50},"10 30 50",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5,15,25},"5 25",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{100},"100",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4},"1 3",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-5,-4,-3,-2,-1},"-5 -3 -1",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,0,0,0,0,0},"0 0 0",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1000000,2000000,3000000},"1000000 3000000",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,1,1,1,1,1,1},"1 1 1 1",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{9},"9",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,20},"10",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:string getAlternates(vector<int>& arr){return "";}};
// USER_CODE_END
void test(vector<int> a,string e,int tc,bool h=false){string g=CodeCoder().getAlternates(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:got="<<g<<":exp="<<e<<"\\n";}
int main(){
try{test({10,20,30,40,50},"10 30 50",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5,15,25},"5 25",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({100},"100",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4},"1 3",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-5,-4,-3,-2,-1},"-5 -3 -1",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,0,0,0,0,0},"0 0 0",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1000000,2000000,3000000},"1000000 3000000",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,1,1,1,1,1,1},"1 1 1 1",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({9},"9",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,20},"10",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def getAlternates(self, arr):
        return ""
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().getAlternates(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={repr(e)}:got={repr(g)}"))
try:test([10,20,30,40,50],"10 30 50",1)
except:print("TC:1:FAIL:hidden")
try:test([5,15,25],"5 25",2)
except:print("TC:2:FAIL:hidden")
try:test([100],"100",3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4],"1 3",4)
except:print("TC:4:FAIL:hidden")
try:test([-5,-4,-3,-2,-1],"-5 -3 -1",5)
except:print("TC:5:FAIL:hidden")
try:test([0,0,0,0,0,0],"0 0 0",6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1000000,2000000,3000000],"1000000 3000000",7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,1,1,1,1,1,1],"1 1 1 1",8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([9],"9",9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,20],"10",10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function getAlternates(arr) { return ""; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=getAlternates(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test([10,20,30,40,50],"10 30 50",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,15,25],"5 25",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([100],"100",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4],"1 3",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-5,-4,-3,-2,-1],"-5 -3 -1",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,0,0,0,0,0],"0 0 0",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1000000,2000000,3000000],"1000000 3000000",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,1,1,1,1,1,1],"1 1 1 1",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([9],"9",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,20],"10",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

// USER_CODE_START
void getAlternates(int* arr,int n,char* out) {
    // Write your code here — store space-separated result in 'out'
    out[0]='\\0';
}
// USER_CODE_END

void run(int* a,int n,char* e,int tc,int h){
    char out[5000]={0};getAlternates(a,n,out);
    if(strcmp(out,e)==0){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:got=%s:exp=%s\\n",tc,out,e);}
}
int main(){
int t1[]={10,20,30,40,50};run(t1,5,"10 30 50",1,0);
int t2[]={5,15,25};run(t2,3,"5 25",2,0);
int t3[]={100};run(t3,1,"100",3,0);
int t4[]={1,2,3,4};run(t4,4,"1 3",4,0);
int t5[]={-5,-4,-3,-2,-1};run(t5,5,"-5 -3 -1",5,0);
int t6[]={0,0,0,0,0,0};run(t6,6,"0 0 0",6,1);
int t7[]={1000000,2000000,3000000};run(t7,3,"1000000 3000000",7,1);
int t8[]={1,1,1,1,1,1,1};run(t8,7,"1 1 1 1",8,1);
int t9[]={9};run(t9,1,"9",9,1);
int t10[]={10,20};run(t10,2,"10",10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
