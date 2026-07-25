"""
Sort Array with 0s 1s 2s (Dutch Flag)
=========================================
Given an array arr containing only 0s, 1s, and 2s, sort it in-place.

Examples:
  arr = [2,0,2,1,1,0] → [0,0,1,1,2,2]
  arr = [0,1,2] → [0,1,2]

Dutch National Flag algorithm: low=0, mid=0, high=n-1.
  - arr[mid] == 0 → swap with low, low++, mid++
  - arr[mid] == 1 → mid++
  - arr[mid] == 2 → swap with high, high--

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sort Array with 0s 1s 2s"
desc=(
    "Given an array arr containing only 0s, 1s, and 2s, sort it in-place so that "
    "all 0s come first, then all 1s, then all 2s.\n\n"
    "For example:\n"
    "arr = [2,0,2,1,1,0] → sorted = [0,0,1,1,2,2]\n"
    "arr = [0,1,2] → sorted = [0,1,2]\n\n"
    "Use the Dutch National Flag algorithm by Edsger Dijkstra. Maintain three pointers: "
    "low (boundary of 0s), mid (current element), high (boundary of 2s).\n"
    "If arr[mid] is 0, swap with arr[low] and advance both. "
    "If arr[mid] is 1, advance mid. "
    "If arr[mid] is 2, swap with arr[high] and decrement high."
)
infmt="First line contains n.\nSecond line contains n space-separated integers (0, 1, or 2)."
outfmt="Print the sorted array as space-separated integers."
cons="1 ≤ n ≤ 10^5\narr[i] is 0, 1, or 2"
e1="Input:\n6\n2 0 2 1 1 0\n\nOutput:\n0 0 1 1 2 2"
e2="Input:\n3\n0 1 2\n\nOutput:\n0 1 2"
e3="Input:\n5\n0 0 0 0 0\n\nOutput:\n0 0 0 0 0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Sorting, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;
// USER_CODE_START
class CodeCoder {
    public void sortColors(int[] arr) {
        // Write your code here — Dutch National Flag
    }
}
// USER_CODE_END
public class Main {
static void test(int[] a,int[] e,int tc,boolean h){int[] cp=Arrays.copyOf(a,a.length);new CodeCoder().sortColors(cp);if(Arrays.equals(cp,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(cp));}
public static void main(String[] a){
try{test(new int[]{2,0,2,1,1,0},new int[]{0,0,1,1,2,2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,1,2},new int[]{0,1,2},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,0,0},new int[]{0,0,0},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{2,2,2,2},new int[]{2,2,2,2},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,0,0,2,2},new int[]{0,0,1,1,2,2},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,2,1,0,2,1,0,2,1},new int[]{0,0,0,1,1,1,2,2,2},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,0,2,1,0,2},new int[]{0,0,1,1,2,2},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{2,1,0},new int[]{0,1,2},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{0},new int[]{0},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{2,2,1,1,0,0},new int[]{0,0,1,1,2,2},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void sortColors(vector<int>& arr){}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool h=false){CodeCoder().sortColors(a);if(a==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({2,0,2,1,1,0},{0,0,1,1,2,2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,1,2},{0,1,2},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,0,0},{0,0,0},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({2,2,2,2},{2,2,2,2},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,0,0,2,2},{0,0,1,1,2,2},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,2,1,0,2,1,0,2,1},{0,0,0,1,1,1,2,2,2},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,0,2,1,0,2},{0,0,1,1,2,2},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({2,1,0},{0,1,2},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({0},{0},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({2,2,1,1,0,0},{0,0,1,1,2,2},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sortColors(self, arr):
        pass
# USER_CODE_END
def test(a,e,tc,h=False):cp=a[:];CodeCoder().sortColors(cp);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if cp==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={cp}"))
try:test([2,0,2,1,1,0],[0,0,1,1,2,2],1)
except:print("TC:1:FAIL:hidden")
try:test([0,1,2],[0,1,2],2)
except:print("TC:2:FAIL:hidden")
try:test([0,0,0],[0,0,0],3)
except:print("TC:3:FAIL:hidden")
try:test([2,2,2,2],[2,2,2,2],4)
except:print("TC:4:FAIL:hidden")
try:test([1,1,0,0,2,2],[0,0,1,1,2,2],5)
except:print("TC:5:FAIL:hidden")
try:test([0,2,1,0,2,1,0,2,1],[0,0,0,1,1,1,2,2,2],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,0,2,1,0,2],[0,0,1,1,2,2],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([2,1,0],[0,1,2],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([0],[0],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([2,2,1,1,0,0],[0,0,1,1,2,2],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sortColors(arr) { }
// USER_CODE_END
function test(a,e,tc,h){if(h===undefined)h=false;const cp=[...a];sortColors(cp);const gs=JSON.stringify(cp),es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);}
try{test([2,0,2,1,1,0],[0,0,1,1,2,2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,1,2],[0,1,2],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,0,0],[0,0,0],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([2,2,2,2],[2,2,2,2],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,0,0,2,2],[0,0,1,1,2,2],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,2,1,0,2,1,0,2,1],[0,0,0,1,1,1,2,2,2],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,0,2,1,0,2],[0,0,1,1,2,2],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([2,1,0],[0,1,2],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([0],[0],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([2,2,1,1,0,0],[0,0,1,1,2,2],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
// USER_CODE_START
void sortColors(int* arr,int n){}
// USER_CODE_END
int arrEq(int* a,int* b,int n){for(int i=0;i<n;i++)if(a[i]!=b[i])return 0;return 1;}
void run(int* a,int n,int* e,int en,int tc,int h){int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];sortColors(cp,n);if(arrEq(cp,e,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:got=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",cp[i]);}printf("]\\n");}}}
int main(){
int t1[]={2,0,2,1,1,0},e1[]={0,0,1,1,2,2};run(t1,6,e1,6,1,0);
int t2[]={0,1,2},e2[]={0,1,2};run(t2,3,e2,3,2,0);
int t3[]={0,0,0},e3[]={0,0,0};run(t3,3,e3,3,3,0);
int t4[]={2,2,2,2},e4[]={2,2,2,2};run(t4,4,e4,4,4,0);
int t5[]={1,1,0,0,2,2},e5[]={0,0,1,1,2,2};run(t5,6,e5,6,5,0);
int t6[]={0,2,1,0,2,1,0,2,1},e6[]={0,0,0,1,1,1,2,2,2};run(t6,9,e6,9,6,1);
int t7[]={1,0,2,1,0,2},e7[]={0,0,1,1,2,2};run(t7,6,e7,6,7,1);
int t8[]={2,1,0},e8[]={0,1,2};run(t8,3,e8,3,8,1);
int t9[]={0},e9[]={0};run(t9,1,e9,1,9,1);
int t10[]={2,2,1,1,0,0},e10[]={0,0,1,1,2,2};run(t10,6,e10,6,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
