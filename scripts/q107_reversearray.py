"""
Reverse an array (using recursion)
====================================
Given an integer array arr, reverse it IN PLACE so that the first element
becomes the last and the last becomes the first, and so on. The task should be
done using recursion (swap the ends, then recurse on the middle).

Examples:
  arr = [1,2,3,4,5] -> [5,4,3,2,1]
  arr = [1,2,3]     -> [3,2,1]

Recursive approach: swap arr[l] and arr[r], then reverse(arr, l+1, r-1) until
l >= r.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(The array is reversed in place; the harness then checks the modified array.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Reverse an array"
desc=(
    "Given an integer array arr, reverse it IN PLACE — the first element "
    "becomes the last, the last becomes the first, and so on.\n\n"
    "For example:\n"
    "arr = [1,2,3,4,5] -> [5,4,3,2,1]\n"
    "arr = [1,2,3]     -> [3,2,1]\n\n"
    "Use recursion: swap the two ends of the current range and recursively "
    "reverse the inner subarray, stopping when the left index is >= the right "
    "index. Do not allocate a new array."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the reversed array (space-separated)."
cons="1 ≤ n ≤ 1000\n-10^6 ≤ arr[i] ≤ 10^6"
e1="Input:\n5\n1 2 3 4 5\n\nOutput:\n5 4 3 2 1"
e2="Input:\n3\n1 2 3\n\nOutput:\n3 2 1"
e3="Input:\n1\n7\n\nOutput:\n7"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Recursion",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public void reverse(int[] arr) {
        // Write your code here — reverse arr in place (recursively)
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean hd){int[] c=a.clone();new CodeCoder().reverse(c);boolean ok=Arrays.equals(c,e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":got="+Arrays.toString(c));}
public static void main(String[] a){
try{test(new int[]{1,2,3,4,5},new int[]{5,4,3,2,1},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3},new int[]{3,2,1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{2,1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},new int[]{6,5,4,3,2,1},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50,60,70},new int[]{70,60,50,40,30,20,10},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{3,1,2},new int[]{2,1,3},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,-2,-3},new int[]{-3,-2,-1},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,5,5},new int[]{5,5,5},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{9,8,7,6,5},new int[]{5,6,7,8,9},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void reverse(vector<int>& arr){}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool hd=false){CodeCoder().reverse(a);bool ok=(a==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:got=";for(int x:a)cout<<x<<" ";cout<<"\\n";}
int main(){
try{test({1,2,3,4,5},{5,4,3,2,1},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3},{3,2,1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2},{2,1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},{6,5,4,3,2,1},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({10,20,30,40,50,60,70},{70,60,50,40,30,20,10},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({3,1,2},{2,1,3},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,-2,-3},{-3,-2,-1},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,5,5},{5,5,5},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({9,8,7,6,5},{5,6,7,8,9},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def reverse(self, arr):
        # Write your code here — reverse arr in place
        pass
# USER_CODE_END
def test(a,e,tc,hd=False):
    c=list(a);CodeCoder().reverse(c);ok=(c==e)
    print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:arr={a}:got={c}"))
try:test([1,2,3,4,5],[5,4,3,2,1],1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],[3,2,1],2)
except:print("TC:2:FAIL:hidden")
try:test([1],[1],3)
except:print("TC:3:FAIL:hidden")
try:test([1,2],[2,1],4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6],[6,5,4,3,2,1],5)
except:print("TC:5:FAIL:hidden")
try:test([10,20,30,40,50,60,70],[70,60,50,40,30,20,10],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([3,1,2],[2,1,3],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1,-2,-3],[-3,-2,-1],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,5,5],[5,5,5],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([9,8,7,6,5],[5,6,7,8,9],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function reverse(arr) { }
// USER_CODE_END
function test(a,e,tc,hd){if(hd===undefined)hd=false;const c=a.slice();reverse(c);let ok=c.length===e.length&&c.every((v,i)=>v===e[i]);if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":got="+JSON.stringify(c));}
try{test([1,2,3,4,5],[5,4,3,2,1],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],[3,2,1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2],[2,1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6],[6,5,4,3,2,1],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([10,20,30,40,50,60,70],[70,60,50,40,30,20,10],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([3,1,2],[2,1,3],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,-2,-3],[-3,-2,-1],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,5,5],[5,5,5],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([9,8,7,6,5],[5,6,7,8,9],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
void reverse(int* arr,int n) {
    // Write your code here — reverse arr in place (recursively)
}
// USER_CODE_END

void runTest(int* a,int n,int* e,int tc,int hd){
    int buf[256];for(int i=0;i<n;i++)buf[i]=a[i];
    reverse(buf,n);
    int ok=1;for(int i=0;i<n;i++){if(buf[i]!=e[i]){ok=0;break;}}
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d\\n",tc,n);}
}
int main(){
    int e1[]={5,4,3,2,1};int a1[]={1,2,3,4,5};runTest(a1,5,e1,1,0);
    int e2[]={3,2,1};int a2[]={1,2,3};runTest(a2,3,e2,2,0);
    int e3[]={1};int a3[]={1};runTest(a3,1,e3,3,0);
    int e4[]={2,1};int a4[]={1,2};runTest(a4,2,e4,4,0);
    int e5[]={6,5,4,3,2,1};int a5[]={1,2,3,4,5,6};runTest(a5,6,e5,5,0);
    int e6[]={70,60,50,40,30,20,10};int a6[]={10,20,30,40,50,60,70};runTest(a6,7,e6,6,1);
    int e7[]={2,1,3};int a7[]={3,1,2};runTest(a7,3,e7,7,1);
    int e8[]={-3,-2,-1};int a8[]={-1,-2,-3};runTest(a8,3,e8,8,1);
    int e9[]={5,5,5};int a9[]={5,5,5};runTest(a9,3,e9,9,1);
    int e10[]={5,6,7,8,9};int a10[]={9,8,7,6,5};runTest(a10,5,e10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
