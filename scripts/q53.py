"""
Sliding Window Maximum
=======================
You are given an array of integers nums and an integer k (window size).
There is a sliding window of size k moving from left to right across nums.
You can only see the k numbers in the window. Each time the window moves
by one position, return the maximum of each window.

Example:
  nums = [1,3,-1,-3,5,3,6,7], k = 3
  Output: [3,3,5,5,6,7]
  Window 0: [1 3 -1] → max 3
  Window 1: [3 -1 -3] → max 3
  Window 2: [-1 -3 5] → max 5
  Window 3: [-3 5 3] → max 5
  Window 4: [5 3 6] → max 6
  Window 5: [3 6 7] → max 7

Approach: Use a deque that stores indices. For each element, remove smaller
elements from the back, then push current. Remove out-of-window from front.
The front of deque is always the max for that window.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Sliding Window Maximum"
desc=(
    "You are given an array of integers nums and an integer k representing "
    "the size of a sliding window that moves from left to right one position at a time. "
    "Return an array containing the maximum element in each window.\n\n"
    "For example:\n"
    "nums = [1,3,-1,-3,5,3,6,7], k = 3\n"
    "Window positions:\n"
    "  [1 3 -1] -3 5 3 6 7 → max = 3\n"
    "  1 [3 -1 -3] 5 3 6 7 → max = 3\n"
    "  1 3 [-1 -3 5] 3 6 7 → max = 5\n"
    "  ... → final answer = [3,3,5,5,6,7]\n\n"
    "For O(n) solution, use a deque (double-ended queue) storing indices. "
    "Maintain decreasing order of values. Remove from front if out of window."
)
infmt="First line contains n and k.\nSecond line contains n space-separated integers."
outfmt="Print n-k+1 space-separated integers — the maximum of each window."
cons="1 \u2264 k \u2264 n \u2264 10^5\n-10^4 \u2264 nums[i] \u2264 10^4"
e1="Input:\n8 3\n1 3 -1 -3 5 3 6 7\n\nOutput:\n3 3 5 5 6 7"
e2="Input:\n1 1\n1\n\nOutput:\n1"
e3="Input:\n4 2\n5 3 4 2\n\nOutput:\n5 4 4"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Queue, Sliding Window, Deque",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int[] maxSlidingWindow(int[] nums, int k) {
        // Write your code here — use a deque
        return new int[0];
    }
}
// USER_CODE_END
public class Main {
static void test(int[] n,int k,int[] e,int tc,boolean h){int[] g=new CodeCoder().maxSlidingWindow(n,k);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+Arrays.toString(n)+":k="+k+":expected="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{1,3,-1,-3,5,3,6,7},3,new int[]{3,3,5,5,6,7},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1},1,new int[]{1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,3,4,2},2,new int[]{5,4,4},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,-1},1,new int[]{1,-1},4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{2,2,2,2,2},3,new int[]{2,2,2},5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{7,6,5,4,3,2,1},3,new int[]{7,6,5,4,3},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,3,3,1,3,3,1,3},2,new int[]{3,3,3,3,3,3,3},7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{-1,-3,-5,-7,-9},2,new int[]{-1,-3,-5,-7},8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{4,3,2,1,5,6,7,8,9},4,new int[]{4,5,6,7,8,9},9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,9,8,7,6,5,4,3,2,1},5,new int[]{10,9,8,7,6},10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:vector<int> maxSlidingWindow(vector<int>& n,int k){return {};}};
// USER_CODE_END
void test(vector<int> n,int k,vector<int> e,int tc,bool h=false){auto g=CodeCoder().maxSlidingWindow(n,k);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got=[";for(int x:g)cout<<x<<",";cout<<"]:expected=[";for(int x:e)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,3,-1,-3,5,3,6,7},3,{3,3,5,5,6,7},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1},1,{1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,3,4,2},2,{5,4,4},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,-1},1,{1,-1},4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({2,2,2,2,2},3,{2,2,2},5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({7,6,5,4,3,2,1},3,{7,6,5,4,3},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,3,3,1,3,3,1,3},2,{3,3,3,3,3,3,3},7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({-1,-3,-5,-7,-9},2,{-1,-3,-5,-7},8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({4,3,2,1,5,6,7,8,9},4,{4,5,6,7,8,9},9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,9,8,7,6,5,4,3,2,1},5,{10,9,8,7,6},10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def maxSlidingWindow(self, nums, k):
        return []
# USER_CODE_END
def test(n,k,e,tc,h=False):g=CodeCoder().maxSlidingWindow(n,k);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:k={k}:expected={e}:got={g}"))
try:test([1,3,-1,-3,5,3,6,7],3,[3,3,5,5,6,7],1)
except:print("TC:1:FAIL:hidden")
try:test([1],1,[1],2)
except:print("TC:2:FAIL:hidden")
try:test([5,3,4,2],2,[5,4,4],3)
except:print("TC:3:FAIL:hidden")
try:test([1,-1],1,[1,-1],4)
except:print("TC:4:FAIL:hidden")
try:test([2,2,2,2,2],3,[2,2,2],5)
except:print("TC:5:FAIL:hidden")
try:test([7,6,5,4,3,2,1],3,[7,6,5,4,3],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,3,3,1,3,3,1,3],2,[3,3,3,3,3,3,3],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([-1,-3,-5,-7,-9],2,[-1,-3,-5,-7],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([4,3,2,1,5,6,7,8,9],4,[4,5,6,7,8,9],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,9,8,7,6,5,4,3,2,1],5,[10,9,8,7,6],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code=r'''// USER_CODE_START
function maxSlidingWindow(nums,k) { return []; }
// USER_CODE_END
function test(n,k,e,tc,h){if(h===undefined)h=false;const g=maxSlidingWindow(n,k);const gs=JSON.stringify(g);const es=JSON.stringify(e);if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+JSON.stringify(n)+":k="+k+":expected="+es+":got="+gs);}
try{test([1,3,-1,-3,5,3,6,7],3,[3,3,5,5,6,7],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1],1,[1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,3,4,2],2,[5,4,4],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,-1],1,[1,-1],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([2,2,2,2,2],3,[2,2,2],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([7,6,5,4,3,2,1],3,[7,6,5,4,3],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,3,3,1,3,3,1,3],2,[3,3,3,3,3,3,3],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([-1,-3,-5,-7,-9],2,[-1,-3,-5,-7],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([4,3,2,1,5,6,7,8,9],4,[4,5,6,7,8,9],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,9,8,7,6,5,4,3,2,1],5,[10,9,8,7,6],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code=r'''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int* maxSlidingWindow(int* n,int s,int k,int* rs){*rs=0;return NULL;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
