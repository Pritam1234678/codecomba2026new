#!/usr/bin/env python3
"""Insert remaining S.No 29-30 and 31-87."""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()
cur.execute("SELECT LOWER(title) FROM problems")
ex={r[0].strip() for r in cur.fetchall()}

def ins(t,d,i,o,c,lv,tp,e1,e2,e3,j,cp,py,js,cc):
    if t.lower().strip() in ex: print(f"  SKIP {t}"); return
    tl=3.0 if lv=="EASY" else 5.0
    cur.execute("INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",(t,d,i,o,c,tl,256,lv,True,tp,e1,e2,e3))
    pid=cur.fetchone()[0]
    for lang,code in [("JAVA",j),("CPP",cp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
        cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
    conn.commit(); print(f"  {t} (pid={pid})")

# 29 - Remove Duplicates
ins("Remove Duplicates from Sorted Array","Given an integer array nums sorted in non-decreasing order, remove the duplicates in-place such that each unique element appears only once. Return the number of unique elements in nums.","First line contains integer n.\nSecond line contains n space-separated integers.","Print the number of unique elements.","1 ≤ n ≤ 3 × 10^4\n-100 ≤ nums[i] ≤ 100","EASY","Array, Two Pointers","Input:\n3\n1 1 2\n\nOutput:\n2","Input:\n10\n0 0 1 1 1 2 2 3 3 4\n\nOutput:\n5","Input:\n1\n1\n\nOutput:\n1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int removeDuplicates(int[] nums) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int e,int tc,boolean h){int g=new Solution().removeDuplicates(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,1,2},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{0,0,1,1,1,2,2,3,3,4},5,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,2,3},3,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{},0,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,1,1},1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:int removeDuplicates(vector<int>& n){return 0;}};
// USER_CODE_END
void test(vector<int> n,int e,int tc,bool h=false){int g=Solution().removeDuplicates(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({1,1,2},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,0,1,1,1,2,2,3,3,4},5,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3},3,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({},0,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1},1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def removeDuplicates(self, n): return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().removeDuplicates(n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:expected={e}:got={g}"))
try:test([1,1,2],2,1)
except:print("TC:1:FAIL:hidden")
try:test([0,0,1,1,1,2,2,3,3,4],5,2)
except:print("TC:2:FAIL:hidden")
try:test([1],1,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3],3,4,True)
except:print("TC:4:FAIL:hidden")
try:test([],0,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1],1,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function removeDuplicates(n) { return 0; }
// USER_CODE_END
function test(n,e,tc,h){const g=removeDuplicates(n);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:expected=${e}:got=${g}`);}
try{test([1,1,2],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,0,1,1,1,2,2,3,3,4],5,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3],3,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([],0,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1],1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int removeDuplicates(int* n,int s){return 0;}
// USER_CODE_END
void test(int* n,int s,int e,int tc,int h){int g=removeDuplicates(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:expected=%d:got=%d\\n",e,g);}}}
int main(){int t1[]={1,1,2};test(t1,3,2,1,0);int t2[]={0,0,1,1,1,2,2,3,3,4};test(t2,10,5,2,0);int t3[]={1};test(t3,1,1,3,0);int t4[]={1,2,3};test(t4,3,3,4,1);int t5[]={};test(t5,0,0,5,1);int t6[]={1,1,1};test(t6,3,1,6,1);return 0;}''')

# 30 - LCM
ins("LCM of Two Numbers","Given two integers a and b, find the LCM (Least Common Multiple) of the two numbers. LCM is the smallest positive integer that is divisible by both a and b.","First line contains integer a.\nSecond line contains integer b.","Print the LCM of a and b.","1 ≤ a, b ≤ 10^9","EASY","Math","Input:\n12\n8\n\nOutput:\n24","Input:\n7\n5\n\nOutput:\n35","Input:\n4\n6\n\nOutput:\n12",
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

# S.No 31-87: Quick inserts for remaining problems using array/int type patterns
import json

def j_arr(fn,tests):
    tcs=[]; i=1
    for a,e,h in tests:
        tcs.append(f"try{{test(new int[]{json.dumps(a)},{e},{i},{'true' if h else 'false'});}}catch(Exception ex){{System.out.println(\"TC:{i}:FAIL:hidden\");}}")
        i+=1
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int {fn}(int[] nums) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n,int e,int tc,boolean h){{int g=new Solution().{fn}(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

def cpp_arr(fn,tests):
    tcs=[]; i=1
    for a,e,h in tests:
        arr="{"+",".join(str(x) for x in a)+"}"
        tcs.append(f"try{{test({arr},{e},{i}{',true' if h else ''});}}catch(...){{cout<<\"TC:{i}:FAIL:hidden\\n\";}}")
        i+=1
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

def py_arr(fn,tests):
    tcs=[]
    for a,e,h in tests:
        i=len(tcs)+1
        tcs.append(f"try:test({json.dumps(a)},{e},{i},{'True' if h else 'False'})\nexcept:print(\"TC:{i}:FAIL:hidden\")")
    mt="\n".join(tcs)
    return f'''# USER_CODE_START
class Solution:
    def {fn}(self, nums): return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().{fn}(n);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:expected={{e}}:got={{g}}"))
{mt}'''

def js_arr(fn,tests):
    tcs=[]
    for a,e,h in tests:
        i=len(tcs)+1
        tcs.append(f"try{{test({json.dumps(a)},{e},{i},{'true' if h else 'false'});}}catch(e){{console.log(\"TC:{i}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''// USER_CODE_START
function {fn}(nums) {{ return 0; }}
// USER_CODE_END
function test(n,e,tc,h){{const g={fn}(n);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:expected=${{e}}:got=${{g}}`);}}
{mt}'''

def c_arr(fn,tests):
    tcs=[]
    for a,e,h in tests:
        i=len(tcs)+1
        arr=",".join(str(x) for x in a)
        tcs.append(f"int t{i}[]={{{arr}}};test(t{i},{len(a)},{e},{i},{'1' if h else '0'});")
    mt="\n".join(tcs)
    return f'''#include <stdio.h>
// USER_CODE_START
int {fn}(int* nums, int numsSize) {{ return 0; }}
// USER_CODE_END
void test(int* n,int s,int e,int tc,int h){{int g={fn}(n,s);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:expected=%d:got=%d\\n",e,g);}}}}}}
int main(){{
{mt}
return 0;}}'''

def t_arr_int(fn,tests):
    """Convenient: tests=[(arr,expected,hidden),...]"""
    return (j_arr(fn,tests),cpp_arr(fn,tests),py_arr(fn,tests),js_arr(fn,tests),c_arr(fn,tests))

# 31 - Maximum Average Subarray I (returns double)
ins("Maximum Average Subarray I","You are given an integer array nums consisting of n elements, and an integer k. Find a contiguous subarray whose length is equal to k that has the maximum average value and return this value.","First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer k.","Print the maximum average value.","1 ≤ k ≤ n ≤ 10^5\n-10^4 ≤ nums[i] ≤ 10^4","EASY","Array, Sliding Window","Input:\n6\n1 12 -5 -6 50 3\n4\n\nOutput:\n12.75","Input:\n1\n5\n1\n\nOutput:\n5.0","Input:\n4\n0 1 1 3\n2\n\nOutput:\n2.0",
'''import java.util.*;
// USER_CODE_START
class Solution { public double findMaxAverage(int[] nums, int k) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n,int k,double e,int tc,boolean h){double g=new Solution().findMaxAverage(n,k);if(Math.abs(g-e)<1e-5)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":k="+k+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,12,-5,-6,50,3},4,12.75,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{5},1,5.0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{0,1,1,3},2,2.0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{-1,-2,-3,-4},2,-1.5,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{0,0,0,0},2,0,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,5,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:double findMaxAverage(vector<int>& n,int k){return 0;}};
// USER_CODE_END
void test(vector<int> n,int k,double e,int tc,bool h=false){double g=Solution().findMaxAverage(n,k);if(abs(g-e)<1e-5)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:k="<<k<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,12,-5,-6,50,3},4,12.75,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({5},1,5.0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0,1,1,3},2,2.0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({-1,-2,-3,-4},2,-1.5,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({0,0,0,0},2,0,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,5,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def findMaxAverage(self, n, k): return 0.0
# USER_CODE_END
def test(n,k,e,tc,h=False):g=Solution().findMaxAverage(n,k);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if abs(g-e)<1e-5 else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:k={k}:expected={e}:got={g}"))
try:test([1,12,-5,-6,50,3],4,12.75,1)
except:print("TC:1:FAIL:hidden")
try:test([5],1,5.0,2)
except:print("TC:2:FAIL:hidden")
try:test([0,1,1,3],2,2.0,3)
except:print("TC:3:FAIL:hidden")
try:test([-1,-2,-3,-4],2,-1.5,4,True)
except:print("TC:4:FAIL:hidden")
try:test([0,0,0,0],2,0,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],1,5,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function findMaxAverage(n,k) { return 0; }
// USER_CODE_END
function test(n,k,e,tc,h){const g=findMaxAverage(n,k);if(Math.abs(g-e)<1e-5)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:n=${JSON.stringify(n)}:k=${k}:expected=${e}:got=${g}`);}
try{test([1,12,-5,-6,50,3],4,12.75,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([5],1,5.0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0,1,1,3],2,2.0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([-1,-2,-3,-4],2,-1.5,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([0,0,0,0],2,0,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],1,5,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <math.h>
// USER_CODE_START
double findMaxAverage(int* n,int s,int k){return 0;}
// USER_CODE_END
void test(int* n,int s,int k,double e,int tc,int h){double g=findMaxAverage(n,s,k);if(fabs(g-e)<1e-5){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:expected=%.5f:got=%.5f\\n",tc,e,g);}}
int main(){int t1[]={1,12,-5,-6,50,3};test(t1,6,4,12.75,1,0);int t2[]={5};test(t2,1,1,5.0,2,0);int t3[]={0,1,1,3};test(t3,4,2,2.0,3,0);int t4[]={-1,-2,-3,-4};test(t4,4,2,-1.5,4,1);int t5[]={0,0,0,0};test(t5,4,2,0,5,1);int t6[]={1,2,3,4,5};test(t6,5,1,5,6,1);return 0;}''')

# 32 - Minimum Size Subarray Sum
j32=[[2,3,1,2,4,3],7,2]
ins("Minimum Size Subarray Sum","Given an array of positive integers nums and a positive integer target, return the minimal length of a subarray whose sum is greater than or equal to target. If there is no such subarray, return 0 instead.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target.","Print the minimal length.","1 ≤ n ≤ 10^5\n1 ≤ nums[i], target ≤ 10^9","MEDIUM","Array, Sliding Window",
"Input:\n6\n2 3 1 2 4 3\n7\n\nOutput:\n2","Input:\n3\n1 4 4\n4\n\nOutput:\n1","Input:\n3\n1 1 1\n3\n\nOutput:\n3",
'''import java.util.*;
// USER_CODE_START
class Solution { public int minSubArrayLen(int target, int[] nums) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int t,int[] n,int e,int tc,boolean h){int g=new Solution().minSubArrayLen(t,n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:target="+t+" input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(7,new int[]{2,3,1,2,4,3},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(4,new int[]{1,4,4},1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(3,new int[]{1,1,1},3,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(11,new int[]{1,2,3,4,5},3,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(100,new int[]{1,2,3,4,5},0,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(15,new int[]{5,1,3,5,10,7,4,9,2,8},2,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:int minSubArrayLen(int t,vector<int>& n){return 0;}};
// USER_CODE_END
void test(int t,vector<int> n,int e,int tc,bool h=false){int g=Solution().minSubArrayLen(t,n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{cout<<"TC:"<<tc<<":FAIL:target="<<t<<" input=[";for(int x:n)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test(7,{2,3,1,2,4,3},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(4,{1,4,4},1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(3,{1,1,1},3,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(11,{1,2,3,4,5},3,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(100,{1,2,3,4,5},0,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(15,{5,1,3,5,10,7,4,9,2,8},2,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def minSubArrayLen(self, t, n): return 0
# USER_CODE_END
def test(t,n,e,tc,h=False):g=Solution().minSubArrayLen(t,n);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:t={t}:n={n}:expected={e}:got={g}"))
try:test(7,[2,3,1,2,4,3],2,1)
except:print("TC:1:FAIL:hidden")
try:test(4,[1,4,4],1,2)
except:print("TC:2:FAIL:hidden")
try:test(3,[1,1,1],3,3)
except:print("TC:3:FAIL:hidden")
try:test(11,[1,2,3,4,5],3,4,True)
except:print("TC:4:FAIL:hidden")
try:test(100,[1,2,3,4,5],0,5,True)
except:print("TC:5:FAIL:hidden")
try:test(15,[5,1,3,5,10,7,4,9,2,8],2,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function minSubArrayLen(t,n) { return 0; }
// USER_CODE_END
function test(t,n,e,tc,h){const g=minSubArrayLen(t,n);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:t=${t}:n=${JSON.stringify(n)}:expected=${e}:got=${g}`);}
try{test(7,[2,3,1,2,4,3],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(4,[1,4,4],1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(3,[1,1,1],3,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(11,[1,2,3,4,5],3,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(100,[1,2,3,4,5],0,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(15,[5,1,3,5,10,7,4,9,2,8],2,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int minSubArrayLen(int t,int* n,int s){return 0;}
// USER_CODE_END
void test(int t,int* n,int s,int e,int tc,int h){int g=minSubArrayLen(t,n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{printf("TC:%d:FAIL:target=%d:expected=%d:got=%d\\n",tc,t,e,g);}}}
int main(){int t1[]={2,3,1,2,4,3};test(7,t1,6,2,1,0);int t2[]={1,4,4};test(4,t2,3,1,2,0);int t3[]={1,1,1};test(3,t3,3,3,3,0);int t4[]={1,2,3,4,5};test(11,t4,5,3,4,1);int t5[]={1,2,3,4,5};test(100,t5,5,0,5,1);int t6[]={5,1,3,5,10,7,4,9,2,8};test(15,t6,10,2,6,1);return 0;}''')

print("\n=== Batch done 29-32! ===")
cur.close(); conn.close()
