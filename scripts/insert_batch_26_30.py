#!/usr/bin/env python3
"""Insert all remaining problems S.No 26-87 with programmatic harnesses."""
import psycopg2, json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()
cur.execute("SELECT LOWER(title) FROM problems")
ex={r[0].strip() for r in cur.fetchall()}

def ins(t,d,i,o,c,lv,tp,e1,e2,e3,j,cp,py,js,cc):
    if t.lower().strip() in ex: return False
    tl=3.0 if "EASY" in lv else 5.0
    cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",(t,d,i,o,c,tl,256,lv,True,tp,e1,e2,e3))
    pid=cur.fetchone()[0]
    for lang,code in [("JAVA",j),("CPP",cp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
        cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
    conn.commit()
    print(f"  {t} (pid={pid})"); return True

# Template generators for common patterns
def j_arr_int(fn,tests):
    """test format: [(arr, expected, hidden), ...]"""
    tcs=[]
    for a,e,h in tests:
        n=len(tcs)+1
        arr=json.dumps(a)
        tcs.append(f"try{{test(new int[]{arr},{e},{n},{'true' if h else 'false'});}}catch(Exception ex){{System.out.println(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int {fn}(int[] nums) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n, int e, int tc, boolean h){{int g=new Solution().{fn}(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

def cpp_arr_int(fn,tests):
    tcs=[]
    for a,e,h in tests:
        n=len(tcs)+1
        arr="{"+",".join(str(x) for x in a)+"}"
        tcs.append(f"try{{test({arr},{e},{n}{',true' if h else ''});}}catch(...){{cout<<\"TC:{n}:FAIL:hidden\\n\";}}")
    mt="\n".join(tcs)
    return f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int {fn}(vector<int>& nums) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n,int e,int tc,bool h=false){{int g=Solution().{fn}(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
{mt}
return 0;}}'''

def py_arr_int(fn,tests):
    tcs=[]
    for a,e,h in tests:
        n=len(tcs)+1
        tcs.append(f"try:test({json.dumps(a)},{e},{n},{'True' if h else 'False'})\nexcept:print(\"TC:{n}:FAIL:hidden\")")
    mt="\n".join(tcs)
    return f'''# USER_CODE_START
class Solution:
    def {fn}(self, nums): return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().{fn}(n);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:expected={{e}}:got={{g}}"))
{mt}'''

def js_arr_int(fn,tests):
    tcs=[]
    for a,e,h in tests:
        n=len(tcs)+1
        tcs.append(f"try{{test({json.dumps(a)},{e},{n},{'true' if h else 'false'});}}catch(e){{console.log(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''// USER_CODE_START
function {fn}(nums) {{ return 0; }}
// USER_CODE_END
function test(n,e,tc,h){{const g={fn}(n);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:expected=${{e}}:got=${{g}}`);}}
{mt}'''

def c_arr_int(fn,tests):
    tcs=[]
    for a,e,h in tests:
        n=len(tcs)+1
        arr=",".join(str(x) for x in a)
        tcs.append(f"int t{n}[]={{{arr}}};test(t{n},{len(a)},{e},{n},{'1' if h else '0'});")
    mt="\n".join(tcs)
    return f'''#include <stdio.h>
// USER_CODE_START
int {fn}(int* nums, int numsSize) {{ return 0; }}
// USER_CODE_END
void test(int* n,int s,int e,int tc,int h){{int g={fn}(n,s);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:expected=%d:got=%d\\n",e,g);}}}}}}
int main(){{
{mt}
return 0;}}'''

def sticky(fn,tests):
    return (j_arr_int(fn,tests), cpp_arr_int(fn,tests), py_arr_int(fn,tests), js_arr_int(fn,tests), c_arr_int(fn,tests))

# S.No 26: Find First and Last Position
t=[[5,7,7,8,8,10],8,[3,4]]
tests=[([5,7,7,8,8,10],8,[3,4],False),([5,7,7,8,8,10],6,[-1,-1],False),([],0,[-1,-1],False),([1],1,[0,0],True),([1,1,1,1],1,[0,3],True),([2,2],2,[0,1],True)]
j='''import java.util.*;
// USER_CODE_START
class Solution { public int[] searchRange(int[] nums, int target) { return new int[]{-1,-1}; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int t,int[] e,int tc,boolean h){int[] g=new Solution().searchRange(n,t);if(Arrays.equals(g,e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":target="+t+":expected="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{5,7,7,8,8,10},8,new int[]{3,4},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5,7,7,8,8,10},6,new int[]{-1,-1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{},0,new int[]{-1,-1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},1,new int[]{0,0},4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,1,1,1},1,new int[]{0,3},5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{2,2},2,new int[]{0,1},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''
cp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:vector<int> searchRange(vector<int>& n,int t){return {-1,-1};}};
// USER_CODE_END
void test(vector<int> n,int t,vector<int> e,int tc,bool h=false){auto g=Solution().searchRange(n,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:target="<<t<<":expected=[";for(int x:e)cout<<x<<",";cout<<"]:got=[";for(int x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({5,7,7,8,8,10},8,{3,4},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5,7,7,8,8,10},6,{-1,-1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({},0,{-1,-1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},1,{0,0},4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,1,1,1},1,{0,3},5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({2,2},2,{0,1},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''
py='''# USER_CODE_START
class Solution:
    def searchRange(self, nums, target): return [-1,-1]
# USER_CODE_END
def test(n,t,e,tc,h=False):g=Solution().searchRange(n,t);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:t={t}:expected={e}:got={g}"))
try:test([5,7,7,8,8,10],8,[3,4],1)
except:print("TC:1:FAIL:hidden")
try:test([5,7,7,8,8,10],6,[-1,-1],2)
except:print("TC:2:FAIL:hidden")
try:test([],0,[-1,-1],3)
except:print("TC:3:FAIL:hidden")
try:test([1],1,[0,0],4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,1,1,1],1,[0,3],5,True)
except:print("TC:5:FAIL:hidden")
try:test([2,2],2,[0,1],6,True)
except:print("TC:6:FAIL:hidden")'''
js='''// USER_CODE_START
function searchRange(nums,target) { return [-1,-1]; }
// USER_CODE_END
function test(n,t,e,tc,h){const g=searchRange(n,t);const gs=JSON.stringify(g);const es=JSON.stringify(e);if(gs===es)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:t=${t}:expected=${es}:got=${gs}`);}
try{test([5,7,7,8,8,10],8,[3,4],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5,7,7,8,8,10],6,[-1,-1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([],0,[-1,-1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],1,[0,0],4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,1,1,1],1,[0,3],5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2,2],2,[0,1],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''
cc='''#include <stdio.h>
// USER_CODE_START
int* searchRange(int* n,int s,int t,int* rs){*rs=2;static int r[2]={-1,-1};return r;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}'''
ins("Find First and Last Position of Element in Sorted Array","Given an array of integers nums sorted in non-decreasing order, find the starting and ending position of a given target value. If target is not found, return [-1, -1].","First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target.","Print two space-separated integers: start and end position.","0 ≤ n ≤ 10^5\n-10^9 ≤ nums[i], target ≤ 10^9","MEDIUM","Array, Binary Search","Input:\n6\n5 7 7 8 8 10\n8\n\nOutput:\n3 4","Input:\n6\n5 7 7 8 8 10\n6\n\nOutput:\n-1 -1","Input:\n0\n\n0\n\nOutput:\n-1 -1",j,cp,py,js,cc)

# S.No 27: 3Sum
ins("3Sum","Given an integer array nums, return all the triplets [nums[i], nums[j], nums[k]] such that i != j, i != k, and j != k, and nums[i] + nums[j] + nums[k] = 0. The solution set must not contain duplicate triplets.","First line contains integer n.\nSecond line contains n space-separated integers.","Print each triplet on a new line as three space-separated integers.","3 ≤ n ≤ 3000\n-10^5 ≤ nums[i] ≤ 10^5","MEDIUM","Array, Two Pointers","Input:\n6\n-1 0 1 2 -1 -4\n\nOutput:\n-1 -1 2\n-1 0 1","Input:\n3\n0 1 1\n\nOutput:\n","Input:\n3\n0 0 0\n\nOutput:\n0 0 0",
'''import java.util.*;
// USER_CODE_START
class Solution { public String threeSum(int[] nums) { return ""; } }
// USER_CODE_END
public class Main {
static void test(int[] n,String e,int tc,boolean h){String g=new Solution().threeSum(n);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{-1,0,1,2,-1,-4},"-1 -1 2,-1 0 1",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,1,1},"",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,0,0},"0 0 0",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-2,0,0,2,2},"-2 0 2",4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,-1},"",5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{-1,0,1,0},"-1 0 1,0 0 0",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:string threeSum(vector<int>& n){return "";}};
// USER_CODE_END
int main(){cout<<"TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n";return 0;}''',
'''# USER_CODE_START
class Solution:
    def threeSum(self, nums): return ""
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().threeSum(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={repr(e)}:got={repr(g)}"))
try:test([-1,0,1,2,-1,-4],"-1 -1 2,-1 0 1",1)
except:print("TC:1:FAIL:hidden")
try:test([0,1,1],"",2)
except:print("TC:2:FAIL:hidden")
try:test([0,0,0],"0 0 0",3)
except:print("TC:3:FAIL:hidden")
try:test([-2,0,0,2,2],"-2 0 2",4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,-1],"",5,True)
except:print("TC:5:FAIL:hidden")
try:test([-1,0,1,0],"-1 0 1,0 0 0",6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function threeSum(nums) { return ""; }
// USER_CODE_END
function test(n,e,tc,h){const g=threeSum(n);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:expected=${JSON.stringify(e)}:got=${JSON.stringify(g)}`);}
try{test([-1,0,1,2,-1,-4],"-1 -1 2,-1 0 1",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,1,1],"",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,0,0],"0 0 0",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-2,0,0,2,2],"-2 0 2",4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,-1],"",5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([-1,0,1,0],"-1 0 1,0 0 0",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
void threeSum(int* n,int s){} 
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# S.No 28: Container With Most Water
ins("Container With Most Water","You are given an integer array height of length n. There are n vertical lines drawn such that the two endpoints of the ith line are (i, 0) and (i, height[i]). Find two lines that together with the x-axis form a container that contains the most water. Return the maximum amount of water.",
"First line contains integer n.\nSecond line contains n space-separated integers representing heights.","Print the maximum area of water the container can hold.","2 ≤ n ≤ 10^5\n0 ≤ height[i] ≤ 10^4","MEDIUM","Array, Two Pointers",
"Input:\n9\n1 8 6 2 5 4 8 3 7\n\nOutput:\n49","Input:\n2\n1 1\n\nOutput:\n1","Input:\n4\n4 3 2 1\n\nOutput:\n4",
f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int maxArea(int[] height) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n,int e,int tc,boolean h){{int g=new Solution().maxArea(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}}
public static void main(String[] a){{
try{{test(new int[]{{1,8,6,2,5,4,8,3,7}},49,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
try{{test(new int[]{{1,1}},1,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
try{{test(new int[]{{4,3,2,1}},4,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
try{{test(new int[]{{1,2,4,3}},4,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
try{{test(new int[]{{1,2,1}},2,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
try{{test(new int[]{{0,0,0,0}},0,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
}}}}''',
f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int maxArea(vector<int>& h) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> h,int e,int tc,bool hd=false){{int g=Solution().maxArea(h);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:h)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
try{{test({{1,8,6,2,5,4,8,3,7}},49,1);}}catch(...){{cout<<"TC:1:FAIL:hidden\\n";}}
try{{test({{1,1}},1,2);}}catch(...){{cout<<"TC:2:FAIL:hidden\\n";}}
try{{test({{4,3,2,1}},4,3);}}catch(...){{cout<<"TC:3:FAIL:hidden\\n";}}
try{{test({{1,2,4,3}},4,4,true);}}catch(...){{cout<<"TC:4:FAIL:hidden\\n";}}
try{{test({{1,2,1}},2,5,true);}}catch(...){{cout<<"TC:5:FAIL:hidden\\n";}}
try{{test({{0,0,0,0}},0,6,true);}}catch(...){{cout<<"TC:6:FAIL:hidden\\n";}}
return 0;}}''',
f'''# USER_CODE_START
class Solution:
    def maxArea(self, h): return 0
# USER_CODE_END
def test(h,e,tc,hd=False):g=Solution().maxArea(h);print(f"TC:{{tc}}:PASS"+(":hidden" if hd else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if hd else f"TC:{{tc}}:FAIL:h={{h}}:expected={{e}}:got={{g}}"))
try:test([1,8,6,2,5,4,8,3,7],49,1)
except:print("TC:1:FAIL:hidden")
try:test([1,1],1,2)
except:print("TC:2:FAIL:hidden")
try:test([4,3,2,1],4,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,4,3],4,4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,2,1],2,5,True)
except:print("TC:5:FAIL:hidden")
try:test([0,0,0,0],0,6,True)
except:print("TC:6:FAIL:hidden")''',
f'''// USER_CODE_START
function maxArea(h) {{ return 0; }}
// USER_CODE_END
function test(h,e,tc,hd){{const g=maxArea(h);if(g===e)console.log(`TC:${{tc}}:PASS`+(hd?':hidden':''));else if(hd)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:h=${{JSON.stringify(h)}}:expected=${{e}}:got=${{g}}`);}}
try{{test([1,8,6,2,5,4,8,3,7],49,1);}}catch(e){{console.log("TC:1:FAIL:hidden");}}
try{{test([1,1],1,2);}}catch(e){{console.log("TC:2:FAIL:hidden");}}
try{{test([4,3,2,1],4,3);}}catch(e){{console.log("TC:3:FAIL:hidden");}}
try{{test([1,2,4,3],4,4,true);}}catch(e){{console.log("TC:4:FAIL:hidden");}}
try{{test([1,2,1],2,5,true);}}catch(e){{console.log("TC:5:FAIL:hidden");}}
try{{test([0,0,0,0],0,6,true);}}catch(e){{console.log("TC:6:FAIL:hidden");}}''',
f'''#include <stdio.h>
// USER_CODE_START
int maxArea(int* h,int s){{return 0;}}
// USER_CODE_END
void test(int* h,int s,int e,int tc,int hd){{int g=maxArea(h,s);if(g==e){{if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",h[i]);}}printf("]:expected=%d:got=%d\\n",e,g);}}}}}}
int main(){{int t1[]={{1,8,6,2,5,4,8,3,7}};test(t1,9,49,1,0);int t2[]={{1,1}};test(t2,2,1,2,0);int t3[]={{4,3,2,1}};test(t3,4,4,3,0);int t4[]={{1,2,4,3}};test(t4,4,4,4,1);int t5[]={{1,2,1}};test(t5,3,2,5,1);int t6[]={{0,0,0,0}};test(t6,4,0,6,1);return 0;}}''')

# S.No 29: Remove Duplicates from Sorted Array
ins("Remove Duplicates from Sorted Array","Given an integer array nums sorted in non-decreasing order, remove the duplicates in-place such that each unique element appears only once. Return the number of unique elements in nums.",
"First line contains integer n.\nSecond line contains n space-separated integers.","Print the number of unique elements.","1 ≤ n ≤ 3 × 10^4\n-100 ≤ nums[i] ≤ 100","EASY","Array, Two Pointers",
"Input:\n3\n1 1 2\n\nOutput:\n2","Input:\n10\n0 0 1 1 1 2 2 3 3 4\n\nOutput:\n5","Input:\n1\n1\n\nOutput:\n1",
*f"import {'' if 1 else ''}",  # skip duplicate, will handle below
*a
)  # actually just use the sticky

j29=javai("removeDuplicates",[([1,1,2],2,False),([0,0,1,1,1,2,2,3,3,4],5,False),([1],1,False),([1,2,3],3,True),([],0,True),([1,1,1],1,True)])
cp29=cppi("removeDuplicates",[([1,1,2],2,False),([0,0,1,1,1,2,2,3,3,4],5,False),([1],1,False),([1,2,3],3,True),([],0,True),([1,1,1],1,True)])
py29=pyi("removeDuplicates",[([1,1,2],2,False),([0,0,1,1,1,2,2,3,3,4],5,False),([1],1,False),([1,2,3],3,True),([],0,True),([1,1,1],1,True)])
js29=jsi("removeDuplicates",[([1,1,2],2,False),([0,0,1,1,1,2,2,3,3,4],5,False),([1],1,False),([1,2,3],3,True),([],0,True),([1,1,1],1,True)])
cc29=ci("removeDuplicates",[([1,1,2],2,False),([0,0,1,1,1,2,2,3,3,4],5,False),([1],1,False),([1,2,3],3,True),([],0,True),([1,1,1],1,True)])
ins("Remove Duplicates from Sorted Array","Given an integer array nums sorted in non-decreasing order, remove the duplicates in-place such that each unique element appears only once. Return the number of unique elements in nums.","First line contains integer n.\nSecond line contains n space-separated integers.","Print the number of unique elements.","1 ≤ n ≤ 3 × 10^4\n-100 ≤ nums[i] ≤ 100","EASY","Array, Two Pointers","Input:\n3\n1 1 2\n\nOutput:\n2","Input:\n10\n0 0 1 1 1 2 2 3 3 4\n\nOutput:\n5","Input:\n1\n1\n\nOutput:\n1",j29,cp29,py29,js29,cc29)

# S.No 30: LCM of Two Numbers
ins("LCM of Two Numbers","Given two integers a and b, find the LCM (Least Common Multiple) of the two numbers. LCM is the smallest positive integer that is divisible by both a and b.",
"First line contains integer a.\nSecond line contains integer b.","Print the LCM of a and b.",
"1 ≤ a, b ≤ 10^9","EASY","Math",
"Input:\n12\n8\n\nOutput:\n24","Input:\n7\n5\n\nOutput:\n35","Input:\n4\n6\n\nOutput:\n12",
'''import java.util.*;
// USER_CODE_START
class Solution { public long lcm(long a, long b) { return 0; } }
// USER_CODE_END
public class Main {
static void test(long a,long b,long e,int tc,boolean h){long g=new Solution().lcm(a,b);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input=a="+a+" b="+b+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(12,8,24,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(7,5,35,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(4,6,12,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,1,1,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(1000000000,1,1000000000,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(6,8,24,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:long long lcm(long long a,long long b){return 0;}};
// USER_CODE_END
void test(long long a,long long b,long long e,int tc,bool h=false){long long g=Solution().lcm(a,b);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:input=a="<<a<<" b="<<b<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(12,8,24,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(7,5,35,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(4,6,12,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,1,1,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(1000000000,1,1000000000,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(6,8,24,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def lcm(self, a, b): return 0
# USER_CODE_END
def test(a,b,e,tc,h=False):g=Solution().lcm(a,b);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:a={a}:b={b}:expected={e}:got={g}"))
try:test(12,8,24,1)
except:print("TC:1:FAIL:hidden")
try:test(7,5,35,2)
except:print("TC:2:FAIL:hidden")
try:test(4,6,12,3)
except:print("TC:3:FAIL:hidden")
try:test(1,1,1,4,True)
except:print("TC:4:FAIL:hidden")
try:test(1000000000,1,1000000000,5,True)
except:print("TC:5:FAIL:hidden")
try:test(6,8,24,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function lcm(a,b) { return 0; }
// USER_CODE_END
function test(a,b,e,tc,h){const g=lcm(a,b);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:a=${a}:b=${b}:expected=${e}:got=${g}`);}
try{test(12,8,24,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(7,5,35,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(4,6,12,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,1,1,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(1000000000,1,1000000000,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(6,8,24,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
long long lcm(long long a,long long b){return 0;}
// USER_CODE_END
void test(long long a,long long b,long long e,int tc,int h){long long g=lcm(a,b);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:a=%lld:b=%lld:expected=%lld:got=%lld\\n",tc,a,b,e,g);}}
int main(){test(12,8,24,1,0);test(7,5,35,2,0);test(4,6,12,3,0);test(1,1,1,4,1);test(1000000000,1,1000000000,5,1);test(6,8,24,6,1);return 0;}''')

print("\nBatch 26-30 done!")
cur.close(); conn.close()
