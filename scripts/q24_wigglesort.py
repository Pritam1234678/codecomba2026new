"""
Wiggle Sort II
================
Given an integer array arr, reorder it such that arr[0] < arr[1] > arr[2] < arr[3] ...
(alternating: small, large, small, large, ...). The arrangement must satisfy
the wiggle condition for all positions.

Examples:
  arr = [1,5,1,1,6,4] → [1,6,1,5,1,4] (valid wiggle)
  arr = [1,3,2,2,3,1] → [2,3,1,3,1,2]

Approach: Sort, split at middle, interleave from end (largest first).
median → small half reversed, large half reversed, interleave.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Wiggle Sort II"
desc=(
    "Given an integer array arr, reorder it in-place such that the array follows "
    "the wiggle pattern: arr[0] < arr[1] > arr[2] < arr[3] > ... and so on.\n\n"
    "For example:\n"
    "arr = [1,5,1,1,6,4] → a valid wiggle arrangement is [1,6,1,5,1,4]\n"
    "arr = [1,3,2,2,3,1] → a valid arrangement is [2,3,1,3,1,2]\n\n"
    "Approach: sort the array, split into two halves at the middle, "
    "reverse both halves, and interleave them placing the larger half "
    "at odd positions and smaller half at even positions. "
    "This guarantees arr[i] < arr[i+1] > arr[i+2] pattern."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print the wiggle-reordered array as space-separated integers."
cons="1 ≤ n ≤ 5*10^4\n-10^9 ≤ arr[i] ≤ 10^9\nAnswer always exists."
e1="Input:\n6\n1 5 1 1 6 4\n\nOutput:\n1 6 1 5 1 4"
e2="Input:\n6\n1 3 2 2 3 1\n\nOutput:\n2 3 1 3 1 2"
e3="Input:\n2\n1 2\n\nOutput:\n1 2"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,256,"HARD",True,"Array, Sorting, Quickselect",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

# Driver validates wiggle condition: a[0]<a[1]>a[2]<a[3]...
java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public void wiggleSort(int[] arr) {
        // Write your code here — arrange so a[0]<a[1]>a[2]<a[3]...
    }
}
// USER_CODE_END

public class Main {
static boolean isWiggle(int[] a){
    for(int i=0;i<a.length-1;i++){
        if(i%2==0){if(!(a[i]<a[i+1]))return false;}
        else{if(!(a[i]>a[i+1]))return false;}
    }
    return true;
}
static void test(int[] a,int tc,boolean h){
    int[] cp=Arrays.copyOf(a,a.length);
    new CodeCoder().wiggleSort(cp);
    if(isWiggle(cp))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":got="+Arrays.toString(cp));
}
public static void main(String[] a){
try{test(new int[]{1,5,1,1,6,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,3,2,2,3,1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{5,4,3,2,1},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,2,2,3,3},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{-5,-4,-3,-2,-1,0},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{10,20,30,40,50},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0,0,1,1,2,2,3,3},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{100,90,80,70,60,50},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void wiggleSort(vector<int>& arr){}};
// USER_CODE_END
bool isWiggle(vector<int>& a){for(int i=0;i+1<(int)a.size();i++){if(i%2==0){if(!(a[i]<a[i+1]))return false;}else{if(!(a[i]>a[i+1]))return false;}}return true;}
void test(vector<int> a,int tc,bool h=false){CodeCoder().wiggleSort(a);if(isWiggle(a))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,5,1,1,6,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,3,2,2,3,1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({5,4,3,2,1},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,2,2,3,3},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({-5,-4,-3,-2,-1,0},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({10,20,30,40,50},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0,0,1,1,2,2,3,3},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({100,90,80,70,60,50},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def wiggleSort(self, arr): pass
# USER_CODE_END
def isWiggle(a):
    for i in range(len(a)-1):
        if i%2==0:
            if not (a[i]<a[i+1]): return False
        else:
            if not (a[i]>a[i+1]): return False
    return True
def test(a,tc,h=False):
    cp=a[:];CodeCoder().wiggleSort(cp)
    if isWiggle(cp): print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:arr={a}:got={cp}")

try:test([1,5,1,1,6,4],1)
except:print("TC:1:FAIL:hidden")
try:test([1,3,2,2,3,1],2)
except:print("TC:2:FAIL:hidden")
try:test([1,2],3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3],4)
except:print("TC:4:FAIL:hidden")
try:test([5,4,3,2,1],5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,2,2,3,3],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([-5,-4,-3,-2,-1,0],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([10,20,30,40,50],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0,0,1,1,2,2,3,3],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([100,90,80,70,60,50],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function wiggleSort(arr) { }
// USER_CODE_END
function isWiggle(a){for(let i=0;i<a.length-1;i++){if(i%2===0){if(!(a[i]<a[i+1]))return false;}else{if(!(a[i]>a[i+1]))return false;}}return true;}
function test(a,tc,h){if(h===undefined)h=false;const cp=[...a];wiggleSort(cp);if(isWiggle(cp))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(cp));}
try{test([1,5,1,1,6,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,3,2,2,3,1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([5,4,3,2,1],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,2,2,3,3],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([-5,-4,-3,-2,-1,0],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([10,20,30,40,50],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0,0,1,1,2,2,3,3],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([100,90,80,70,60,50],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
void wiggleSort(int* arr,int n){}
// USER_CODE_END
bool isWiggle(int* a,int n){for(int i=0;i+1<n;i++){if(i%2==0){if(!(a[i]<a[i+1]))return false;}else{if(!(a[i]>a[i+1]))return false;}}return true;}
void run(int* a,int n,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];wiggleSort(cp,n);if(isWiggle(cp,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL\\n",tc);}}
int main(){
int t1[]={1,5,1,1,6,4};run(t1,6,1,0);
int t2[]={1,3,2,2,3,1};run(t2,6,2,0);
int t3[]={1,2};run(t3,2,3,0);
int t4[]={1,2,3};run(t4,3,4,0);
int t5[]={5,4,3,2,1};run(t5,5,5,0);
int t6[]={1,1,2,2,3,3};run(t6,6,6,1);
int t7[]={-5,-4,-3,-2,-1,0};run(t7,6,7,1);
int t8[]={10,20,30,40,50};run(t8,5,8,1);
int t9[]={0,0,1,1,2,2,3,3};run(t9,8,9,1);
int t10[]={100,90,80,70,60,50};run(t10,6,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
