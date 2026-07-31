"""
Next Permutation
==================
Given an array arr of n distinct integers, rearrange it into the next
lexicographically greater permutation. If no such arrangement exists
(arr is in descending order), rearrange into ascending order (the first permutation).

Examples:
  arr = [1,2,3] → [1,3,2]
  arr = [3,2,1] → [1,2,3] (wrap around)
  arr = [1,1,5] → [1,5,1]

Algorithm (in-place, O(n)):
1. Find i from right where arr[i] < arr[i+1].
2. Find j from right where arr[j] > arr[i]. Swap.
3. Reverse arr[i+1 .. end].

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Next Permutation"
desc=(
    "Given an array arr of n integers, rearrange the numbers into the next "
    "lexicographically greater permutation of the same numbers.\n\n"
    "If such an arrangement is not possible (the array is in descending order), "
    "rearrange it into ascending order — the lowest possible permutation.\n\n"
    "For example:\n"
    "arr = [1,2,3] → next permutation is [1,3,2]\n"
    "arr = [3,2,1] → no greater permutation, wrap to [1,2,3]\n"
    "arr = [1,1,5] → next is [1,5,1]\n\n"
    "Algorithm (in-place):\n"
    "1. Find the largest index i such that arr[i] < arr[i+1] (scanning from right).\n"
    "2. Find the largest index j > i such that arr[j] > arr[i]. Swap arr[i] and arr[j].\n"
    "3. Reverse the suffix arr[i+1 ... n-1]."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the next permutation as space-separated integers."
cons="1 ≤ n ≤ 100\n0 ≤ arr[i] ≤ 100"
e1="Input:\n3\n1 2 3\n\nOutput:\n1 3 2"
e2="Input:\n3\n3 2 1\n\nOutput:\n1 2 3"
e3="Input:\n3\n1 1 5\n\nOutput:\n1 5 1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public void nextPermutation(int[] arr) {
        // Write your code here — find pivot, swap, reverse suffix
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean h){int[] cp=Arrays.copyOf(a,a.length);new CodeCoder().nextPermutation(cp);if(Arrays.equals(cp,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(cp));}
public static void main(String[] a){
try{test(new int[]{1,2,3},new int[]{1,3,2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,2,1},new int[]{1,2,3},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,1,5},new int[]{1,5,1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,3,2},new int[]{2,1,3},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{2,3,1},new int[]{3,1,2},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4},new int[]{1,2,4,3},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{4,3,2,1},new int[]{1,2,3,4},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{5,4,7,5,3,2},new int[]{5,5,2,3,4,7},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,5,1},new int[]{5,1,1},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void nextPermutation(vector<int>& arr){}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){CodeCoder().nextPermutation(a);if(a==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,2,3},{1,3,2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,2,1},{1,2,3},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,1,5},{1,5,1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},{1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,3,2},{2,1,3},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({2,3,1},{3,1,2},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4},{1,2,4,3},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({4,3,2,1},{1,2,3,4},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({5,4,7,5,3,2},{5,5,2,3,4,7},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,5,1},{5,1,1},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def nextPermutation(self, arr): pass
# USER_CODE_END
def test(a,e,tc,h=False):cp=a[:];CodeCoder().nextPermutation(cp);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if cp==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={cp}"))
try:test([1,2,3],[1,3,2],1)
except:print("TC:1:FAIL:hidden")
try:test([3,2,1],[1,2,3],2)
except:print("TC:2:FAIL:hidden")
try:test([1,1,5],[1,5,1],3)
except:print("TC:3:FAIL:hidden")
try:test([1],[1],4)
except:print("TC:4:FAIL:hidden")
try:test([1,3,2],[2,1,3],5)
except:print("TC:5:FAIL:hidden")
try:test([2,3,1],[3,1,2],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4],[1,2,4,3],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([4,3,2,1],[1,2,3,4],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([5,4,7,5,3,2],[5,5,2,3,4,7],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,5,1],[5,1,1],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function nextPermutation(arr) { }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const cp=[...a];nextPermutation(cp);const gs=JSON.stringify(cp),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);}
try{test([1,2,3],[1,3,2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,2,1],[1,2,3],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,1,5],[1,5,1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],[1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,3,2],[2,1,3],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2,3,1],[3,1,2],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4],[1,2,4,3],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([4,3,2,1],[1,2,3,4],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([5,4,7,5,3,2],[5,5,2,3,4,7],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,5,1],[5,1,1],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void nextPermutation(int* arr,int n){}
// USER_CODE_END
int arrEq(int*a,int*b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int*a,int n,int*e,int en,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];nextPermutation(cp,n);if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int t1[]={1,2,3},e1[]={1,3,2};run(t1,3,e1,3,1,0);
int t2[]={3,2,1},e2[]={1,2,3};run(t2,3,e2,3,2,0);
int t3[]={1,1,5},e3[]={1,5,1};run(t3,3,e3,3,3,0);
int t4[]={1},e4[]={1};run(t4,1,e4,1,4,0);
int t5[]={1,3,2},e5[]={2,1,3};run(t5,3,e5,3,5,0);
int t6[]={2,3,1},e6[]={3,1,2};run(t6,3,e6,3,6,1);
int t7[]={1,2,3,4},e7[]={1,2,4,3};run(t7,4,e7,4,7,1);
int t8[]={4,3,2,1},e8[]={1,2,3,4};run(t8,4,e8,4,8,1);
int t9[]={5,4,7,5,3,2},e9[]={5,5,2,3,4,7};run(t9,6,e9,6,9,1);
int t10[]={1,5,1},e10[]={5,1,1};run(t10,3,e10,3,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
