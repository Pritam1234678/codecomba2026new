"""
Sort Elements by Decreasing Frequency
======================================
Given an integer array arr, sort it so that elements with higher frequency
appear first. When two elements have the SAME frequency, the SMALLER element
appears first. Return the sorted array.

Examples:
  arr = [1,1,2,2,2,3] -> [2,2,2,1,1,3]   (2 appears 3 times, 1 twice, 3 once)
  arr = [2,3,1,3,2]   -> [2,2,3,3,1]     (2 and 3 both twice; 2 < 3 first)

Count frequencies with a hash map, then sort with a custom comparator:
primary key = -frequency, secondary key = element value.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the result is returned via int* returnSize: int* sortFreq(int* arr, int n, int* rs).)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sort Elements by Decreasing Frequency"
desc=(
    "Given an integer array arr, sort it so that the elements with the HIGHEST "
    "frequency (count of occurrences) come first. If two elements have the "
    "same frequency, the SMALLER element must come first. Return the sorted "
    "array.\n\n"
    "For example:\n"
    "arr = [1,1,2,2,2,3] -> [2,2,2,1,1,3]  (2 appears 3 times, 1 twice, 3 once)\n"
    "arr = [2,3,1,3,2]   -> [2,2,3,3,1]    (2 and 3 both appear twice; 2 < 3 so it leads)\n\n"
    "Count the frequency of each value with a hash map, then sort the array "
    "with a custom comparator: primary key -frequency (descending), secondary "
    "key the element value itself (ascending). Complexity O(n log n)."
)
infmt="First line contains n. Second line contains n space-separated integers."
outfmt="Print the array sorted by decreasing frequency (tie -> smaller value first), space-separated."
cons="1 ≤ n ≤ 10^5\n1 ≤ arr[i] ≤ 10^5"
e1="Input:\n6\n1 1 2 2 2 3\n\nOutput:\n2 2 2 1 1 3"
e2="Input:\n5\n2 3 1 3 2\n\nOutput:\n2 2 3 3 1"
e3="Input:\n5\n1 2 3 4 5\n\nOutput:\n1 2 3 4 5"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Sorting, Hash Map",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] sortFreq(int[] arr) {
        // Write your code here — higher frequency first, tie -> smaller value
        return arr;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] a,int[] e,int tc,boolean hd){int[] g=new CodeCoder().sortFreq(a.clone());boolean ok=Arrays.equals(g,e);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":exp="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] x){
try{test(new int[]{1,1,2,2,2,3},new int[]{2,2,2,1,1,3},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2,3,1,3,2},new int[]{2,2,3,3,1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,5,4,6,4},new int[]{4,4,5,5,6},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,1,1,2,2,3,3,4},new int[]{1,1,1,2,2,3,3,4},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{9,9,9,8,8,7},new int[]{9,9,9,8,8,7},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{1,2,3,4,5},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{4,4,4,4,3,3,3,2,2,1},new int[]{4,4,4,4,3,3,3,2,2,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,5,1,5,1,3},new int[]{1,1,1,5,5,3},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{7,7,7,7,7},new int[]{7,7,7,7,7},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,10,2,10,2,10,3,3},new int[]{10,10,10,10,2,2,3,3},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> sortFreq(vector<int>& arr){return arr;}};
// USER_CODE_END
void test(vector<int> a,vector<int> e,int tc,bool hd=false){vector<int> g=CodeCoder().sortFreq(a);bool ok=(g==e);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:arr=[";for(int i=0;i<(int)a.size();i++){if(i)cout<<",";cout<<a[i];}cout<<"]:exp=[";for(int i=0;i<(int)e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(int i=0;i<(int)g.size();i++){if(i)cout<<",";cout<<g[i];}cout<<"]\\n";}}
int main(){
try{test({1,1,2,2,2,3},{2,2,2,1,1,3},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2,3,1,3,2},{2,2,3,3,1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,5,4,6,4},{4,4,5,5,6},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,1,1,2,2,3,3,4},{1,1,1,2,2,3,3,4},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({9,9,9,8,8,7},{9,9,9,8,8,7},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{1,2,3,4,5},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({4,4,4,4,3,3,3,2,2,1},{4,4,4,4,3,3,3,2,2,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,5,1,5,1,3},{1,1,1,5,5,3},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({7,7,7,7,7},{7,7,7,7,7},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,10,2,10,2,10,3,3},{10,10,10,10,2,2,3,3},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def sortFreq(self, arr):
        return arr
# USER_CODE_END
def test(a,e,tc,hd=False):g=CodeCoder().sortFreq(list(a));ok=(g==e);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if ok else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:arr={a}:exp={e}:got={g}"))
try:test([1,1,2,2,2,3],[2,2,2,1,1,3],1)
except:print("TC:1:FAIL:hidden")
try:test([2,3,1,3,2],[2,2,3,3,1],2)
except:print("TC:2:FAIL:hidden")
try:test([5,5,4,6,4],[4,4,5,5,6],3)
except:print("TC:3:FAIL:hidden")
try:test([1,1,1,2,2,3,3,4],[1,1,1,2,2,3,3,4],4)
except:print("TC:4:FAIL:hidden")
try:test([9,9,9,8,8,7],[9,9,9,8,8,7],5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],[1,2,3,4,5],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([4,4,4,4,3,3,3,2,2,1],[4,4,4,4,3,3,3,2,2,1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,5,1,5,1,3],[1,1,1,5,5,3],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([7,7,7,7,7],[7,7,7,7,7],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,10,2,10,2,10,3,3],[10,10,10,10,2,2,3,3],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function sortFreq(arr) { return arr; }
// USER_CODE_END
function test(a,e,tc,hd){if(hd===undefined)hd=false;const g=sortFreq(a.slice());let ok=g.length===e.length&&g.every((v,i)=>v===e[i]);if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:arr="+JSON.stringify(a)+":exp="+JSON.stringify(e)+":got="+JSON.stringify(g));}
try{test([1,1,2,2,2,3],[2,2,2,1,1,3],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2,3,1,3,2],[2,2,3,3,1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,5,4,6,4],[4,4,5,5,6],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,1,1,2,2,3,3,4],[1,1,1,2,2,3,3,4],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([9,9,9,8,8,7],[9,9,9,8,8,7],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],[1,2,3,4,5],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([4,4,4,4,3,3,3,2,2,1],[4,4,4,4,3,3,3,2,2,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,5,1,5,1,3],[1,1,1,5,5,3],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([7,7,7,7,7],[7,7,7,7,7],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,10,2,10,2,10,3,3],[10,10,10,10,2,2,3,3],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
int* sortFreq(int* arr,int n,int* rs) {
    // Write your code here — higher frequency first, tie -> smaller value
    *rs = 0; return NULL;
}
// USER_CODE_END

void runTest(int* a,int n,int* e,int tc,int hd){
    int rs=0;int* g=sortFreq(a,n,&rs);
    int ok=(rs==n);
    if(ok)for(int i=0;i<n;i++){if(g[i]!=e[i]){ok=0;break;}}
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else if(hd)printf("TC:%d:FAIL:hidden\\n",tc);
    else{printf("TC:%d:FAIL:arr=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",a[i]);}printf("]:exp=[");for(int i=0;i<n;i++){if(i)printf(",");printf("%d",e[i]);}printf("]:got=[");for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",g[i]);}printf("]\\n");}
    free(g);
}
int main(){
    int a1[]={1,1,2,2,2,3};int e1[]={2,2,2,1,1,3};runTest(a1,6,e1,1,0);
    int a2[]={2,3,1,3,2};int e2[]={2,2,3,3,1};runTest(a2,5,e2,2,0);
    int a3[]={5,5,4,6,4};int e3[]={4,4,5,5,6};runTest(a3,5,e3,3,0);
    int a4[]={1,1,1,2,2,3,3,4};int e4[]={1,1,1,2,2,3,3,4};runTest(a4,8,e4,4,0);
    int a5[]={9,9,9,8,8,7};int e5[]={9,9,9,8,8,7};runTest(a5,6,e5,5,0);
    int a6[]={1,2,3,4,5};int e6[]={1,2,3,4,5};runTest(a6,5,e6,6,1);
    int a7[]={4,4,4,4,3,3,3,2,2,1};int e7[]={4,4,4,4,3,3,3,2,2,1};runTest(a7,10,e7,7,1);
    int a8[]={1,5,1,5,1,3};int e8[]={1,1,1,5,5,3};runTest(a8,6,e8,8,1);
    int a9[]={7,7,7,7,7};int e9[]={7,7,7,7,7};runTest(a9,5,e9,9,1);
    int a10[]={10,10,2,10,2,10,3,3};int e10[]={10,10,10,10,2,2,3,3};runTest(a10,8,e10,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
