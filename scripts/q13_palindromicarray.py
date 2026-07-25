"""
Palindromic Array
===================
Given an array arr of size n, check if the array is palindromic —
it reads the same forwards and backwards.

Examples:
  arr = [1, 2, 3, 2, 1] → true
  arr = [1, 2, 3, 4, 5] → false

Two-pointer: left=0, right=n-1, compare while left < right.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Palindromic Array"
desc=(
    "Given an array arr of size n, determine whether it is a palindrome — meaning "
    "it reads the same forwards and backwards.\n\n"
    "For example:\n"
    "arr = [1, 2, 3, 2, 1] → true\n"
    "arr = [1, 2, 3, 4, 5] → false\n\n"
    "Use two pointers: left at index 0, right at n-1. While left < right, compare "
    "arr[left] and arr[right]. If they differ at any point, return false. "
    "If the loop completes, return true."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print 'true' if palindromic, otherwise 'false'."
cons="1 ≤ n ≤ 10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n1 2 3 2 1\n\nOutput:\ntrue"
e2="Input:\n5\n1 2 3 4 5\n\nOutput:\nfalse"
e3="Input:\n1\n42\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder {
    public boolean isPalindromic(int[] arr) { return false; }
}
// USER_CODE_END
public class Main {
static void test(int[] a,boolean e,int tc,boolean h){boolean g=new CodeCoder().isPalindromic(a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,3,2,1},true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{42},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,1,1,1},true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,2,2},false,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-5,-3,-3,-5},true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{0,1,0},true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3,4,3,2,1},true,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100,200,300,200,100},true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:bool isPalindromic(vector<int>& arr){return false;}};
// USER_CODE_END
void test(vector<int> a,bool e,int tc,bool h=false){bool g=CodeCoder().isPalindromic(a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test({1,2,3,2,1},true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3,4,5},false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({42},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,1,1,1},true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,2,2},false,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({-5,-3,-3,-5},true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({0,1,0},true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3,4,3,2,1},true,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100,200,300,200,100},true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def isPalindromic(self, arr): return False
# USER_CODE_END
def test(a,e,tc,h=False):g=CodeCoder().isPalindromic(a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,2,3,2,1],True,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3,4,5],False,2)
except:print("TC:2:FAIL:hidden")
try:test([42],True,3)
except:print("TC:3:FAIL:hidden")
try:test([1,1,1,1],True,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,2,2],False,5)
except:print("TC:5:FAIL:hidden")
try:test([-5,-3,-3,-5],True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,1,0],True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,3,2,1],True,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,3,4,5,6],False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100,200,300,200,100],True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function isPalindromic(arr) { return false; }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const g=isPalindromic(a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test([1,2,3,2,1],true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3,4,5],false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([42],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,1,1,1],true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,2,2],false,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-5,-3,-3,-5],true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,1,0],true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,3,2,1],true,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,4,5,6],false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100,200,300,200,100],true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool isPalindromic(int* arr,int n){return false;}
// USER_CODE_END
void run(int* a,int n,bool e,int tc,int h){bool g=isPalindromic(a,n);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}}
int main(){
int t1[]={1,2,3,2,1};run(t1,5,true,1,0);
int t2[]={1,2,3,4,5};run(t2,5,false,2,0);
int t3[]={42};run(t3,1,true,3,0);
int t4[]={1,1,1,1};run(t4,4,true,4,0);
int t5[]={1,2,2,2};run(t5,4,false,5,0);
int t6[]={-5,-3,-3,-5};run(t6,4,true,6,1);
int t7[]={0,1,0};run(t7,3,true,7,1);
int t8[]={1,2,3,4,3,2,1};run(t8,7,true,8,1);
int t9[]={1,2,3,4,5,6};run(t9,6,false,9,1);
int t10[]={100,200,300,200,100};run(t10,5,true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
