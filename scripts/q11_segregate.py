"""
Segregate 0s and 1s
=====================
Given an array arr of size n containing only 0s and 1s, segregate them so that
all 0s come first followed by all 1s. The order of 0s and 1s among themselves
does not matter.

Examples:
  arr = [0, 1, 0, 1, 0] → [0, 0, 0, 1, 1]
  arr = [1, 1, 0, 0] → [0, 0, 1, 1]

Two-pointer approach: left=0, right=n-1. While left < right, if arr[left]==1 && arr[right]==0, swap.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Segregate 0s and 1s"
desc=(
    "Given an array arr of size n containing only 0s and 1s, rearrange the array so that "
    "all 0s appear first, followed by all 1s. The internal order does not matter.\n\n"
    "For example:\n"
    "arr = [0, 1, 0, 1, 0] → [0, 0, 0, 1, 1]\n"
    "arr = [1, 1, 0, 0] → [0, 0, 1, 1]\n\n"
    "Use two pointers: left = 0, right = n-1. While left < right, if arr[left] is 1 and "
    "arr[right] is 0, swap them. If arr[left] is 0, move left forward. If arr[right] is 1, "
    "move right backward."
)
infmt="First line contains n.\nSecond line contains n space-separated integers (0 or 1)."
outfmt="Print the segregated array as space-separated integers."
cons="1 ≤ n ≤ 10^5\narr[i] is either 0 or 1"
e1="Input:\n5\n0 1 0 1 0\n\nOutput:\n0 0 0 1 1"
e2="Input:\n4\n1 1 0 0\n\nOutput:\n0 0 1 1"
e3="Input:\n3\n0 0 0\n\nOutput:\n0 0 0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Array, Two Pointers",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public void segregate(int[] arr) {
        // Write your code here — two-pointer
    }
}
// USER_CODE_END

public class Main {
static boolean isSegregated(int[] a){boolean seenOne=false;for(int x:a){if(x==1)seenOne=true;if(x==0&&seenOne)return false;}return true;}
static void test(int[] a,int tc,boolean h){
    int[] cp=Arrays.copyOf(a,a.length);
    new CodeCoder().segregate(cp);
    if(isSegregated(cp))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
    else System.out.println("TC:"+tc+":FAIL:arr="+Arrays.toString(a)+":got="+Arrays.toString(cp));
}
public static void main(String[] a){
try{test(new int[]{0,1,0,1,0},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,1,0,0},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,0,0},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,1,1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{0,1},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,0,1,0,1,0},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{0,0,1,1,0,0,1,1},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{0},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{0,0,1,1,0,1,0,1,0,0},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:void segregate(vector<int>& arr){}};
// USER_CODE_END
bool isSeg(vector<int>& a){bool seen=false;for(int x:a){if(x==1)seen=true;if(x==0&&seen)return false;}return true;}
void test(vector<int> a,int tc,bool h=false){
    CodeCoder().segregate(a);
    if(isSeg(a))cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:a)cout<<x<<",";cout<<"]\\n";}
}
int main(){
try{test({0,1,0,1,0},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,1,0,0},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,0,0},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,1,1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({0,1},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,0,1,0,1,0},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({0,0,1,1,0,0,1,1},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({0},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({0,0,1,1,0,1,0,1,0,0},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def segregate(self, arr):
        pass
# USER_CODE_END
def isSeg(a):seen=False;return not any((seen and x==0) or (x==1 and not seen) or (seen:=True if x==1 else seen) for _ in[0])
# simpler: just check all 0s before 1s
def isSegregated(a):s=False;return not bool([1 for x in a if (x==1 and not s) or (s and x==0) or (x==1 and not(s:=True))])
def test(a,tc,h=False):
    cp=a[:];CodeCoder().segregate(cp)
    # check all 0s then 1s
    seenOne=False;ok=True
    for x in cp:
        if x==0 and seenOne:ok=False;break
        if x==1:seenOne=True
    if ok:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={cp}")

try:test([0,1,0,1,0],1)
except:print("TC:1:FAIL:hidden")
try:test([1,1,0,0],2)
except:print("TC:2:FAIL:hidden")
try:test([0,0,0],3)
except:print("TC:3:FAIL:hidden")
try:test([1,1,1],4)
except:print("TC:4:FAIL:hidden")
try:test([0,1],5)
except:print("TC:5:FAIL:hidden")
try:test([1,0,1,0,1,0],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([0,0,1,1,0,0,1,1],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([0],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([0,0,1,1,0,1,0,1,0,0],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function segregate(arr) { }
// USER_CODE_END
function isSeg(a){let seen=false;for(let x of a){if(x===0&&seen)return false;if(x===1)seen=true;}return true;}
function test(a,tc,h){if(h===undefined)h=false;const cp=[...a];segregate(cp);if(isSeg(cp))console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:got="+JSON.stringify(cp));}
try{test([0,1,0,1,0],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,1,0,0],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,0,0],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,1,1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([0,1],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,0,1,0,1,0],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([0,0,1,1,0,0,1,1],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([0],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([0,0,1,1,0,1,0,1,0,0],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
void segregate(int* arr,int n) {
    // Write your code here
}
// USER_CODE_END

int isSeg(int* a,int n){int s=0;for(int i=0;i<n;i++){if(a[i]==0&&s)return 0;if(a[i]==1)s=1;}return 1;}
void run(int* a,int n,int tc,int h){
    int cp[1005];for(int i=0;i<n;i++)cp[i]=a[i];
    segregate(cp,n);
    if(isSeg(cp,n)){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:got=[",tc);for(int i=0;i<n;i++){if(i)printf(",");printf("%d",cp[i]);}printf("]\\n");}}
}
int main(){
int t1[]={0,1,0,1,0};run(t1,5,1,0);
int t2[]={1,1,0,0};run(t2,4,2,0);
int t3[]={0,0,0};run(t3,3,3,0);
int t4[]={1,1,1};run(t4,3,4,0);
int t5[]={0,1};run(t5,2,5,0);
int t6[]={1,0,1,0,1,0};run(t6,6,6,1);
int t7[]={0,0,1,1,0,0,1,1};run(t7,8,7,1);
int t8[]={0};run(t8,1,8,1);
int t9[]={1};run(t9,1,9,1);
int t10[]={0,0,1,1,0,1,0,1,0,0};run(t10,10,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
