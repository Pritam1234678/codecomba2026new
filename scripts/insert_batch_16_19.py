#!/usr/bin/env python3
"""Batch insert problems S.No 16-30 from Deloitte sheet."""
import psycopg2

conn = psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()
cur.execute("SELECT LOWER(title) FROM problems")
existing={r[0].strip() for r in cur.fetchall()}

def ins(t,d,i,o,c,lv,tp,e1,e2,e3,j,cp,py,js,cc):
    if t.lower().strip() in existing:
        print(f"  SKIP {t} (exists)"); return
    tl=3.0 if lv=="EASY" else 5.0
    cur.execute("INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
        (t,d,i,o,c,tl,256,lv,True,tp,e1,e2,e3))
    pid=cur.fetchone()[0]
    for lang,code in [("JAVA",j),("CPP",cp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
        cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
    conn.commit()
    print(f"  {t} (pid={pid})")

# 16 - Subarray Sum Equals K
ins("Subarray Sum Equals K",
"Given an array of integers nums and an integer k, return the total number of subarrays whose sum equals k. A subarray is a contiguous non-empty sequence of elements.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer k.",
"Print the number of subarrays with sum equal to k.",
"1 ≤ n ≤ 10^4\n-1000 ≤ nums[i] ≤ 1000\n-10^7 ≤ k ≤ 10^7","MEDIUM","Array, Hash Table",
"Input:\n3\n1 1 1\n2\n\nOutput:\n2","Input:\n3\n1 2 3\n3\n\nOutput:\n2","Input:\n2\n-1 -1\n0\n\nOutput:\n1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int subarraySum(int[] nums, int k) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int k,int e,int tc,boolean h){int g=new Solution().subarraySum(n,k);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":k="+k+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,1,1},2,2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3},3,2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{-1,-1},0,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},0,0,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-1,1,0},0,3,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,0,0,0},0,10,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:int subarraySum(vector<int>& n,int k){return 0;}};
// USER_CODE_END
void test(vector<int> n,int k,int e,int tc,bool h=false){int g=Solution().subarraySum(n,k);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:k="<<k<<":expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({1,1,1},2,2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3},3,2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({-1,-1},0,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},0,0,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-1,1,0},0,3,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,0,0,0},0,10,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def subarraySum(self, n, k): return 0
# USER_CODE_END
def test(n,k,e,tc,h=False):g=Solution().subarraySum(n,k);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:k={k}:expected={e}:got={g}"))
try:test([1,1,1],2,2,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],3,2,2)
except:print("TC:2:FAIL:hidden")
try:test([-1,-1],0,1,3)
except:print("TC:3:FAIL:hidden")
try:test([1],0,0,4,True)
except:print("TC:4:FAIL:hidden")
try:test([-1,1,0],0,3,5,True)
except:print("TC:5:FAIL:hidden")
try:test([0,0,0,0],0,10,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function subarraySum(n,k) { return 0; }
// USER_CODE_END
function test(n,k,e,tc,h){const g=subarraySum(n,k);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:k=${k}:expected=${e}:got=${g}`);}
try{test([1,1,1],2,2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],3,2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([-1,-1],0,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],0,0,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-1,1,0],0,3,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,0,0,0],0,10,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int subarraySum(int* n,int s,int k){return 0;}
// USER_CODE_END
void test(int* n,int s,int k,int e,int tc,int h){int g=subarraySum(n,s,k);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:k=%d:expected=%d:got=%d\\n",k,e,g);}}}
int main(){int t1[]={1,1,1};test(t1,3,2,2,1,0);int t2[]={1,2,3};test(t2,3,3,2,2,0);int t3[]={-1,-1};test(t3,2,0,1,3,0);int t4[]={1};test(t4,1,0,0,4,1);int t5[]={-1,1,0};test(t5,3,0,3,5,1);int t6[]={0,0,0,0};test(t6,4,0,10,6,1);return 0;}''')

# 17 - Longest Consecutive Sequence
ins("Longest Consecutive Sequence",
"Given an unsorted array of integers nums, return the length of the longest consecutive elements sequence. You must write an algorithm that runs in O(n) time.",
"First line contains integer n.\nSecond line contains n space-separated integers.",
"Print the length of the longest consecutive sequence.",
"0 ≤ n ≤ 10^5\n-10^9 ≤ nums[i] ≤ 10^9","MEDIUM","Array, Hash Table",
"Input:\n6\n100 4 200 1 3 2\n\nOutput:\n4\n\nExplanation: Longest consecutive sequence is [1,2,3,4] with length 4.","Input:\n0\n\n\nOutput:\n0","Input:\n1\n1\n\nOutput:\n1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int longestConsecutive(int[] nums) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int e,int tc,boolean h){int g=new Solution().longestConsecutive(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{100,4,200,1,3,2},4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{},0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{0,3,7,2,5,8,4,6,0,1},9,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{9,1,4,7,3,-1,0,5,8,-1,6},7,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,0,1},4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:int longestConsecutive(vector<int>& n){return 0;}};
// USER_CODE_END
void test(vector<int> n,int e,int tc,bool h=false){int g=Solution().longestConsecutive(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({100,4,200,1,3,2},4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({},0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({0,3,7,2,5,8,4,6,0,1},9,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({9,1,4,7,3,-1,0,5,8,-1,6},7,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,0,1},4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def longestConsecutive(self, n): return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().longestConsecutive(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test([100,4,200,1,3,2],4,1)
except:print("TC:1:FAIL:hidden")
try:test([],0,2)
except:print("TC:2:FAIL:hidden")
try:test([1],1,3)
except:print("TC:3:FAIL:hidden")
try:test([0,3,7,2,5,8,4,6,0,1],9,4,True)
except:print("TC:4:FAIL:hidden")
try:test([9,1,4,7,3,-1,0,5,8,-1,6],7,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,2,0,1],4,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function longestConsecutive(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){const g=longestConsecutive(n);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:expected=${e}:got=${g}`);}
try{test([100,4,200,1,3,2],4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([],0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([0,3,7,2,5,8,4,6,0,1],9,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([9,1,4,7,3,-1,0,5,8,-1,6],7,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,0,1],4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int longestConsecutive(int* n,int s){return 0;}
// USER_CODE_END
void test(int* n,int s,int e,int tc,int h){int g=longestConsecutive(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:expected=%d:got=%d\\n",e,g);}}}
int main(){int t1[]={100,4,200,1,3,2};test(t1,6,4,1,0);int t2[]={};test(t2,0,0,2,0);int t3[]={1};test(t3,1,1,3,0);int t4[]={0,3,7,2,5,8,4,6,0,1};test(t4,10,9,4,1);int t5[]={9,1,4,7,3,-1,0,5,8,-1,6};test(t5,11,7,5,1);int t6[]={1,2,0,1};test(t6,4,4,6,1);return 0;}''')

# 18 - Merge Intervals
ins("Merge Intervals",
"Given an array of intervals where intervals[i] = [starti, endi], merge all overlapping intervals, and return an array of the non-overlapping intervals that cover all the intervals in the input.",
"First line contains integer n.\nNext n lines contain two space-separated integers start and end.",
"Print each merged interval on a new line as 'start end'.",
"1 ≤ n ≤ 10^4\n0 ≤ starti ≤ endi ≤ 10^4","MEDIUM","Array, Sorting",
"Input:\n4\n1 3\n2 6\n8 10\n15 18\n\nOutput:\n1 6\n8 10\n15 18","Input:\n2\n1 4\n4 5\n\nOutput:\n1 5","Input:\n1\n1 1\n\nOutput:\n1 1",
'''import java.util.*;
// USER_CODE_START
class Solution { public String mergeIntervals(int[][] intervals) { return ""; } }
// USER_CODE_END
public class Main {
static void test(int[][] inv, String e, int tc, boolean h){String g=new Solution().mergeIntervals(inv);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else{String si="";for(var x:inv)si+="["+x[0]+","+x[1]+"] ";System.out.println("TC:"+tc+":FAIL:input="+si.trim()+":expected="+e+":got="+g);}}
public static void main(String[] a){
try{test(new int[][]{{1,3},{2,6},{8,10},{15,18}},"1 6, 8 10, 15 18",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{1,4},{4,5}},"1 5",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{1,1}},"1 1",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{{1,10},{2,3},{4,5},{6,7}},"1 10",4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{}},"",5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{5,6},{1,2},{3,4}},"1 2, 3 4, 5 6",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:string mergeIntervals(vector<vector<int>>& inv){return "";}};
// USER_CODE_END
void test(vector<vector<int>> inv,string e,int tc,bool h=false){string g=Solution().mergeIntervals(inv);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:got="<<g<<":expected="<<e<<"\\n";}}
int main(){
try{test({{1,3},{2,6},{8,10},{15,18}},"1 6, 8 10, 15 18",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{1,4},{4,5}},"1 5",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{1,1}},"1 1",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({{1,10},{2,3},{4,5},{6,7}},"1 10",4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{}},"",5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{5,6},{1,2},{3,4}},"1 2, 3 4, 5 6",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def mergeIntervals(self, inv): return ""
# USER_CODE_END
def test(inv,e,tc,h=False):g=Solution().mergeIntervals(inv);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:inv={inv}:expected={repr(e)}:got={repr(g)}"))
try:test([[1,3],[2,6],[8,10],[15,18]],"1 6, 8 10, 15 18",1)
except:print("TC:1:FAIL:hidden")
try:test([[1,4],[4,5]],"1 5",2)
except:print("TC:2:FAIL:hidden")
try:test([[1,1]],"1 1",3)
except:print("TC:3:FAIL:hidden")
try:test([[1,10],[2,3],[4,5],[6,7]],"1 10",4,True)
except:print("TC:4:FAIL:hidden")
try:test([],"",5,True)
except:print("TC:5:FAIL:hidden")
try:test([[5,6],[1,2],[3,4]],"1 2, 3 4, 5 6",6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function mergeIntervals(inv) { return ""; }
// USER_CODE_END
function test(inv,e,tc,h){const g=mergeIntervals(inv);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:inv=${JSON.stringify(inv)}:expected=${JSON.stringify(e)}:got=${JSON.stringify(g)}`);}
try{test([[1,3],[2,6],[8,10],[15,18]],"1 6, 8 10, 15 18",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[1,4],[4,5]],"1 5",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[1,1]],"1 1",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([[1,10],[2,3],[4,5],[6,7]],"1 10",4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],"",5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[5,6],[1,2],[3,4]],"1 2, 3 4, 5 6",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
void mergeIntervals(int** inv,int n,char* out){out[0]=0;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 19 - Sort an Array
ins("Sort an Array",
"Given an array of integers nums, sort the array in ascending order and return it.",
"First line contains integer n.\nSecond line contains n space-separated integers.",
"Print n space-separated integers in ascending order.",
"1 ≤ n ≤ 5 × 10^4\n-5 × 10^4 ≤ nums[i] ≤ 5 × 10^4","EASY","Array, Sorting",
"Input:\n5\n5 2 3 1 4\n\nOutput:\n1 2 3 4 5","Input:\n3\n3 2 1\n\nOutput:\n1 2 3","Input:\n1\n1\n\nOutput:\n1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int[] sortArray(int[] nums) { return nums; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int[] e,int tc,boolean h){int[] g=new Solution().sortArray(Arrays.copyOf(n,n.length));Arrays.sort(g);Arrays.sort(e);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{5,2,3,1,4},new int[]{1,2,3,4,5},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{3,2,1},new int[]{1,2,3},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-1,-5,0,3,2},new int[]{-5,-1,0,2,3},4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},new int[]{},5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1,1},new int[]{1,1,1,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:vector<int> sortArray(vector<int>& n){return n;}};
// USER_CODE_END
void test(vector<int> n,vector<int> e,int tc,bool h=false){auto g=Solution().sortArray(n);sort(g.begin(),g.end());sort(e.begin(),e.end());if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:expected=[";for(int x:e)cout<<x<<",";cout<<"]:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({5,2,3,1,4},{1,2,3,4,5},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({3,2,1},{1,2,3},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},{1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-1,-5,0,3,2},{-5,-1,0,2,3},4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},{},5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1},{1,1,1,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def sortArray(self, n): return []
# USER_CODE_END
def test(n,e,tc,h=False):g=sorted(Solution().sortArray(n));e=sorted(e);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test([5,2,3,1,4],[1,2,3,4,5],1)
except:print("TC:1:FAIL:hidden")
try:test([3,2,1],[1,2,3],2)
except:print("TC:2:FAIL:hidden")
try:test([1],[1],3)
except:print("TC:3:FAIL:hidden")
try:test([-1,-5,0,3,2],[-5,-1,0,2,3],4,True)
except:print("TC:4:FAIL:hidden")
try:test([],[],5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1],[1,1,1,1],6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function sortArray(n) { return n; }
// USER_CODE_END
function test(n,e,tc,h){const g=sortArray([...n]).sort((a,b)=>a-b);const es=JSON.stringify([...e].sort((a,b)=>a-b));if(JSON.stringify(g)===es)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:expected=${es}:got=${JSON.stringify(g)}`);}
try{test([5,2,3,1,4],[1,2,3,4,5],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([3,2,1],[1,2,3],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],[1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-1,-5,0,3,2],[-5,-1,0,2,3],4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],[],5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1],[1,1,1,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <stdlib.h>
// USER_CODE_START
int* sortArray(int* n,int s,int* rs){*rs=s;return n;}
// USER_CODE_END
int cmp(const void* a,const void* b){return *(int*)a-*(int*)b;}
void test(int* n,int s,int* e,int tc,int h){int rs;int* g=sortArray(n,s,&rs);qsort(g,rs,sizeof(int),cmp);int ok=1;for(int i=0;i<rs;i++)if(g[i]!=e[i]){ok=0;break;}if(ok){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:got=[",tc);for(int i=0;i<rs;i++){if(i)printf(",");printf("%d",g[i]);}printf("]\\n");}}}
int main(){int t1[]={5,2,3,1,4};int e1[]={1,2,3,4,5};test(t1,5,e1,1,0);int t2[]={3,2,1};int e2[]={1,2,3};test(t2,3,e2,2,0);int t3[]={1};int e3[]={1};test(t3,1,e3,3,0);int t4[]={-1,-5,0,3,2};int e4[]={-5,-1,0,2,3};test(t4,5,e4,4,1);int t5[]={};test(t5,0,NULL,5,1);int t6[]={1,1,1,1};int e6[]={1,1,1,1};test(t6,4,e6,6,1);return 0;}''')

print("\nBatch 16-19 done! Pushing to VM...")
cur.close(); conn.close()
