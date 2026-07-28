"""
Rotate Array by One
=====================
Given an array arr of size n, rotate it to the right by one position.

Examples:
  arr = [1,2,3,4,5] → [5,1,2,3,4]
  arr = [10,20,30] → [30,10,20]

Save last element, shift all elements right by one, put last at front.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rotate Array by One"
desc=(
    "Given an array arr of size n, rotate it to the right by one position.\n"
    "The last element becomes the first, and all other elements shift right.\n\n"
    "For example:\n"
    "arr = [1,2,3,4,5] → rotated = [5,1,2,3,4]\n"
    "arr = [10,20,30] → rotated = [30,10,20]\n\n"
    "Save the last element, shift all elements one position to the right "
    "starting from the end, then place the saved element at index 0."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the rotated array as space-separated integers."
cons="1 ≤ n ≤ 10^5\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n5 1 2 3 4"
e2="Input:\n3\n10 20 30\n\nOutput:\n30 10 20"
e3="Input:\n1\n42\n\nOutput:\n42"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"MEDIUM",True,"Array",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder{public void rotateByOne(int[] arr){}}
// USER_CODE_END
public class Main{
static void test(int[] a,int[] e,int tc,boolean h){int[] cp=Arrays.copyOf(a,a.length);new CodeCoder().rotateByOne(cp);if(Arrays.equals(cp,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(cp));}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5},new int[]{5,1,2,3,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10,20,30},new int[]{30,10,20},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{42},new int[]{42},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-5,-4,-3},new int[]{-3,-5,-4},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{0,0,0},new int[]{0,0,0},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1},new int[]{1,1,1,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{5,10,15,20,25},new int[]{25,5,10,15,20},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1000000000,1000000000},new int[]{1000000000,-1000000000},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{2,1},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100,200,300,400,500,600},new int[]{600,100,200,300,400,500},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void rotateByOne(vector<int>& arr){}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){CodeCoder().rotateByOne(a);if(a==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,2,3,4,5},{5,1,2,3,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10,20,30},{30,10,20},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({42},{42},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-5,-4,-3},{-3,-5,-4},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({0,0,0},{0,0,0},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1},{1,1,1,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({5,10,15,20,25},{25,5,10,15,20},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1000000000,1000000000},{1000000000,-1000000000},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2},{2,1},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100,200,300,400,500,600},{600,100,200,300,400,500},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rotateByOne(self, arr): pass
# USER_CODE_END
def test(a,e,tc,h=False):cp=a[:];CodeCoder().rotateByOne(cp);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if cp==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={cp}"))
try:test([1,2,3,4,5],[5,1,2,3,4],1)
except:print("TC:1:FAIL:hidden")
try:test([10,20,30],[30,10,20],2)
except:print("TC:2:FAIL:hidden")
try:test([42],[42],3)
except:print("TC:3:FAIL:hidden")
try:test([-5,-4,-3],[-3,-5,-4],4)
except:print("TC:4:FAIL:hidden")
try:test([0,0,0],[0,0,0],5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1],[1,1,1,1],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([5,10,15,20,25],[25,5,10,15,20],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1000000000,1000000000],[1000000000,-1000000000],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2],[2,1],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100,200,300,400,500,600],[600,100,200,300,400,500],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rotateByOne(arr){}
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const cp=[...a];rotateByOne(cp);const gs=JSON.stringify(cp),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);}
try{test([1,2,3,4,5],[5,1,2,3,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10,20,30],[30,10,20],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([42],[42],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-5,-4,-3],[-3,-5,-4],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([0,0,0],[0,0,0],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1],[1,1,1,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([5,10,15,20,25],[25,5,10,15,20],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1000000000,1000000000],[1000000000,-1000000000],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2],[2,1],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100,200,300,400,500,600],[600,100,200,300,400,500],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void rotateByOne(int* arr,int n){}
// USER_CODE_END
int arrEq(int*a,int*b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int*a,int n,int*e,int en,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];rotateByOne(cp,n);if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int t1[]={1,2,3,4,5},e1[]={5,1,2,3,4};run(t1,5,e1,5,1,0);
int t2[]={10,20,30},e2[]={30,10,20};run(t2,3,e2,3,2,0);
int t3[]={42},e3[]={42};run(t3,1,e3,1,3,0);
int t4[]={-5,-4,-3},e4[]={-3,-5,-4};run(t4,3,e4,3,4,0);
int t5[]={0,0,0},e5[]={0,0,0};run(t5,3,e5,3,5,0);
int t6[]={1,1,1,1},e6[]={1,1,1,1};run(t6,4,e6,4,6,1);
int t7[]={5,10,15,20,25},e7[]={25,5,10,15,20};run(t7,5,e7,5,7,1);
int t8[]={-1000000000,1000000000},e8[]={1000000000,-1000000000};run(t8,2,e8,2,8,1);
int t9[]={1,2},e9[]={2,1};run(t9,2,e9,2,9,1);
int t10[]={100,200,300,400,500,600},e10[]={600,100,200,300,400,500};run(t10,6,e10,6,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
