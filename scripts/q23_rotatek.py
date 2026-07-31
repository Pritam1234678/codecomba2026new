"""
Rotate Array by K steps
=========================
Given an array arr of size n and an integer k, rotate the array to the right
by k steps. k can be larger than n.

Examples:
  arr = [1,2,3,4,5,6,7], k = 3 → [5,6,7,1,2,3,4]
  arr = [-1,-100,3,99], k = 2 → [3,99,-1,-100]

Optimal approach: reverse entire array, reverse first k, reverse rest.
k = k % n first.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Rotate Array by K steps"
desc=(
    "Given an integer array arr of size n and an integer k, rotate the array "
    "to the right by k steps.\n\n"
    "For example:\n"
    "arr = [1,2,3,4,5,6,7], k = 3 → after rotation: [5,6,7,1,2,3,4]\n"
    "arr = [-1,-100,3,99], k = 2 → [3,99,-1,-100]\n\n"
    "k may be larger than n, so first compute k = k % n. "
    "Then use the reversal technique:\n"
    "1. Reverse the entire array.\n"
    "2. Reverse the first k elements.\n"
    "3. Reverse the remaining n-k elements.\n"
    "This runs in O(n) time and O(1) extra space."
)
infmt="First line contains n and k.\nSecond line contains n space-separated integers."
outfmt="Print the rotated array as space-separated integers."
cons="1 ≤ n ≤ 10^5\n0 ≤ k ≤ 10^9\n-10^9 ≤ arr[i] ≤ 10^9"
e1="Input:\n7 3\n1 2 3 4 5 6 7\n\nOutput:\n5 6 7 1 2 3 4"
e2="Input:\n4 2\n-1 -100 3 99\n\nOutput:\n3 99 -1 -100"
e3="Input:\n1 5\n42\n\nOutput:\n42"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"HARD",True,"Array, Math, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public void rotate(int[] arr, int k) {
        // Write your code here — k %= n, then reverse trick
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int k,int[] e,int tc,boolean h){int[] cp=Arrays.copyOf(a,a.length);new CodeCoder().rotate(cp,k);if(Arrays.equals(cp,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":k="+k+":exp="+Arrays.toString(e)+":got="+Arrays.toString(cp));}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5,6,7},3,new int[]{5,6,7,1,2,3,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{-1,-100,3,99},2,new int[]{3,99,-1,-100},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{42},5,new int[]{42},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},0,new int[]{1,2,3,4,5},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},7,new int[]{4,5,1,2,3},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},4,new int[]{3,4,5,6,1,2},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-1,-2,-3},1,new int[]{-3,-1,-2},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,1,1,1},100,new int[]{1,1,1,1},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50,60},6,new int[]{10,20,30,40,50,60},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,3},2,new int[]{2,3,1},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void rotate(vector<int>& arr,int k){}};
// USER_CODE_END
void test(vector<int> a,int k,vector<int> e,int tc,bool h=false){CodeCoder().rotate(a,k);if(a==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,2,3,4,5,6,7},3,{5,6,7,1,2,3,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({-1,-100,3,99},2,{3,99,-1,-100},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({42},5,{42},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,4,5},0,{1,2,3,4,5},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},7,{4,5,1,2,3},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},4,{3,4,5,6,1,2},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-1,-2,-3},1,{-3,-1,-2},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,1,1,1},100,{1,1,1,1},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,20,30,40,50,60},6,{10,20,30,40,50,60},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,3},2,{2,3,1},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def rotate(self, arr, k): pass
# USER_CODE_END
def test(a,k,e,tc,h=False):cp=a[:];CodeCoder().rotate(cp,k);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if cp==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:k={k}:exp={e}:got={cp}"))
try:test([1,2,3,4,5,6,7],3,[5,6,7,1,2,3,4],1)
except:print("TC:1:FAIL:hidden")
try:test([-1,-100,3,99],2,[3,99,-1,-100],2)
except:print("TC:2:FAIL:hidden")
try:test([42],5,[42],3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,4,5],0,[1,2,3,4,5],4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],7,[4,5,1,2,3],5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5,6],4,[3,4,5,6,1,2],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-1,-2,-3],1,[-3,-1,-2],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,1,1,1],100,[1,1,1,1],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([10,20,30,40,50,60],6,[10,20,30,40,50,60],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,3],2,[2,3,1],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function rotate(arr, k) { }
// USER_CODE_END
function test(a,k,e,tc,h){if(h===undefined)h=false;const cp=[...a];rotate(cp,k);const gs=JSON.stringify(cp),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);}
try{test([1,2,3,4,5,6,7],3,[5,6,7,1,2,3,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([-1,-100,3,99],2,[3,99,-1,-100],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([42],5,[42],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,4,5],0,[1,2,3,4,5],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],7,[4,5,1,2,3],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5,6],4,[3,4,5,6,1,2],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-1,-2,-3],1,[-3,-1,-2],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,1,1,1],100,[1,1,1,1],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,20,30,40,50,60],6,[10,20,30,40,50,60],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,3],2,[2,3,1],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void rotate(int* arr,int n,int k){}
// USER_CODE_END
int arrEq(int*a,int*b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int*a,int n,int k,int*e,int en,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];rotate(cp,n,k);if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int t1[]={1,2,3,4,5,6,7},e1[]={5,6,7,1,2,3,4};run(t1,7,3,e1,7,1,0);
int t2[]={-1,-100,3,99},e2[]={3,99,-1,-100};run(t2,4,2,e2,4,2,0);
int t3[]={42},e3[]={42};run(t3,1,5,e3,1,3,0);
int t4[]={1,2,3,4,5},e4[]={1,2,3,4,5};run(t4,5,0,e4,5,4,0);
int t5[]={1,2,3,4,5},e5[]={4,5,1,2,3};run(t5,5,7,e5,5,5,0);
int t6[]={1,2,3,4,5,6},e6[]={3,4,5,6,1,2};run(t6,6,4,e6,6,6,1);
int t7[]={-1,-2,-3},e7[]={-3,-1,-2};run(t7,3,1,e7,3,7,1);
int t8[]={1,1,1,1},e8[]={1,1,1,1};run(t8,4,100,e8,4,8,1);
int t9[]={10,20,30,40,50,60},e9[]={10,20,30,40,50,60};run(t9,6,6,e9,6,9,1);
int t10[]={1,2,3},e10[]={2,3,1};run(t10,3,2,e10,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
