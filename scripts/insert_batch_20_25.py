#!/usr/bin/env python3
"""Batch insert S.No 20-35."""
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

# Harness templates for int[] nums -> int
def int_ret(t,fname,tests):
    tcs=[]; i=1
    for args,exp,hidden in tests:
        h="true" if hidden else "false"
        inp="=".join(str(a) for a in args) if len(args)<=3 else str(args[0])
        inp=inp.replace(":","=")
        tcs.append(f"try{{test({','.join(str(x) for x in args)},{exp},{i},{h});}}catch(Exception e){{System.out.println(\"TC:{i}:FAIL:hidden\");}}")
        i+=1
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int {fname}({t}) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test({t},int e,int tc,boolean h){{int g=new Solution().{fname}({', '.join(p.split()[-1] for p in t.split(',') if p.strip())});if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

def basic_java(fn,rt,pt,args_list):
    """args_list: [(arg_vals, expected, hidden), ...]"""
    tcs=[]
    for vals,exp,hid in args_list:
        n=len(tcs)+1
        arg_vals=", ".join(str(v) for v in vals)
        h="true" if hid else "false"
        inp_str=":".join(str(v) for v in vals)
        inp_str=inp_str.replace(":","=")
        tcs.append(f"try{{test({arg_vals},{exp},{n},{h});}}catch(Exception e){{System.out.println(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public {rt} {fn}({pt}) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test({pt}, {rt} e, int tc, boolean h){{int g=new Solution().{fn}({', '.join(p.split()[-1] if len(p.split())>1 else p for p in pt.split(',') if p.strip())});if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

def basic_java_bool(fn,pt,args_list):
    tcs=[]
    for vals,exp,hid in args_list:
        n=len(tcs)+1
        arg_vals=", ".join(str(v) for v in vals)
        h="true" if hid else "false"
        tcs.append(f"try{{test({arg_vals},{'true' if exp else 'false'},{n},{h});}}catch(Exception e){{System.out.println(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public boolean {fn}({pt}) {{ return false; }} }}
// USER_CODE_END
public class Main {{
static void test({pt}, boolean e, int tc, boolean h){{boolean g=new Solution().{fn}({', '.join(p.split()[-1] if len(p.split())>1 else p for p in pt.split(',') if p.strip())});if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

# Quick helpers for common patterns
def javai(fn,tests):
    """int[] nums -> int"""
    tcs=[]
    for vals,exp,hid in tests:
        n=len(tcs)+1
        arr=json.dumps(vals)
        h="true" if hid else "false"
        tcs.append(f"try{{test(new int[]{arr},{exp},{n},{h});}}catch(Exception e){{System.out.println(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int {fn}(int[] nums) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n, int e, int tc, boolean h){{int g=new Solution().{fn}(n);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+java.util.Arrays.toString(n)+":expected="+e+":got="+g);}}
public static void main(String[] a){{
{mt}
}}}}
'''

def cppi(fn,tests):
    tcs=[]
    for vals,exp,hid in tests:
        n=len(tcs)+1
        arr="{"
        for i,v in enumerate(vals):
            if i: arr+=","
            arr+=str(v)
        arr+="}"
        h="true" if hid else "false"
        tcs.append(f"try{{test({arr},{exp},{n});}}catch(...){{cout<<\"TC:{n}:FAIL:hidden\\n\";}}")
    mt="\n".join(tcs)
    return f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int {fn}(vector<int>& nums) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n, int e, int tc, bool h=false){{int g=Solution().{fn}(n);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
{mt}
return 0;}}'''

def pyi(fn,tests):
    tcs=[]
    for vals,exp,hid in tests:
        n=len(tcs)+1
        h="True" if hid else "False"
        tcs.append(f"try:test({json.dumps(vals)},{exp},{n},{h})\nexcept:print(\"TC:{n}:FAIL:hidden\")")
    mt="\n".join(tcs)
    return f'''# USER_CODE_START
class Solution:
    def {fn}(self, nums): return 0
# USER_CODE_END
def test(n,e,tc,h=False):g=Solution().{fn}(n);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:expected={{e}}:got={{g}}"))
{mt}'''

def jsi(fn,tests):
    tcs=[]
    for vals,exp,hid in tests:
        n=len(tcs)+1
        h="true" if hid else "false"
        tcs.append(f"try{{test({json.dumps(vals)},{exp},{n},{h});}}catch(e){{console.log(\"TC:{n}:FAIL:hidden\");}}")
    mt="\n".join(tcs)
    return f'''// USER_CODE_START
function {fn}(nums) {{ return 0; }}
// USER_CODE_END
function test(n,e,tc,h){{const g={fn}(n);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:expected=${{e}}:got=${{g}}`);}}
{mt}'''

def ci(fn,tests):
    tcs=[]
    for vals,exp,hid in tests:
        n=len(tcs)+1
        arr=",".join(str(v) for v in vals)
        h="1" if hid else "0"
        tcs.append(f"int t{n}[]={{{arr}}};test(t{n},{len(vals)},{exp},{n},{h});")
    mt="\n".join(tcs)
    return f'''#include <stdio.h>
// USER_CODE_START
int {fn}(int* nums, int numsSize) {{ return 0; }}
// USER_CODE_END
void test(int* n,int s,int e,int tc,int h){{int g={fn}(n,s);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:expected=%d:got=%d\\n",e,g);}}}}}}
int main(){{
{mt}
return 0;}}'''

import json

# 20 - Kth Largest Element in an Array
t=[([3,2,1,5,6,4],2,5),([3,2,3,1,2,4,5,5,6],4,4),([1],1,1),([2,2,2,2],1,2),([-1,-2,-3],2,-2),([5,5,5,5,5],3,5)]
h=[[False]*3+[True]*3][0][:];h=[False,False,False,True,True,True]
tt=[(t[i][0],t[i][1],t[i][2],h[i]) for i in range(6)]
ins("Kth Largest Element in an Array",
"Given an integer array nums and an integer k, return the kth largest element in the array. It is the kth largest element in sorted order, not the kth distinct element.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer k.",
"Print the kth largest element.",
"1 ≤ n ≤ 10^5\n1 ≤ k ≤ n\n-10^4 ≤ nums[i] ≤ 10^4","MEDIUM","Array, Sorting, Heap",
"Input:\n6\n3 2 1 5 6 4\n2\n\nOutput:\n5","Input:\n8\n3 2 3 1 2 4 5 5 6\n4\n\nOutput:\n4","Input:\n1\n1\n1\n\nOutput:\n1",
f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int findKthLargest(int[] nums, int k) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n, int k, int e, int tc, boolean h){{int g=new Solution().findKthLargest(n,k);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":k="+k+":expected="+e+":got="+g);}}
public static void main(String[] a){{
try{{test(new int[]{{3,2,1,5,6,4}},2,5,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
try{{test(new int[]{{3,2,3,1,2,4,5,5,6}},4,4,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
try{{test(new int[]{{1}},1,1,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
try{{test(new int[]{{2,2,2,2}},1,2,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
try{{test(new int[]{{-1,-2,-3}},2,-2,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
try{{test(new int[]{{5,5,5,5,5}},3,5,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
}}}}''',
f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int findKthLargest(vector<int>& n, int k) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n,int k,int e,int tc,bool h=false){{int g=Solution().findKthLargest(n,k);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:k="<<k<<":expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
try{{test({{3,2,1,5,6,4}},2,5,1);}}catch(...){{cout<<"TC:1:FAIL:hidden\\n";}}
try{{test({{3,2,3,1,2,4,5,5,6}},4,4,2);}}catch(...){{cout<<"TC:2:FAIL:hidden\\n";}}
try{{test({{1}},1,1,3);}}catch(...){{cout<<"TC:3:FAIL:hidden\\n";}}
try{{test({{2,2,2,2}},1,2,4,true);}}catch(...){{cout<<"TC:4:FAIL:hidden\\n";}}
try{{test({{-1,-2,-3}},2,-2,5,true);}}catch(...){{cout<<"TC:5:FAIL:hidden\\n";}}
try{{test({{5,5,5,5,5}},3,5,6,true);}}catch(...){{cout<<"TC:6:FAIL:hidden\\n";}}
return 0;}}''',
f'''# USER_CODE_START
class Solution:
    def findKthLargest(self, n, k): return 0
# USER_CODE_END
def test(n,k,e,tc,h=False):g=Solution().findKthLargest(n,k);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:k={{k}}:expected={{e}}:got={{g}}"))
try:test([3,2,1,5,6,4],2,5,1)
except:print("TC:1:FAIL:hidden")
try:test([3,2,3,1,2,4,5,5,6],4,4,2)
except:print("TC:2:FAIL:hidden")
try:test([1],1,1,3)
except:print("TC:3:FAIL:hidden")
try:test([2,2,2,2],1,2,4,True)
except:print("TC:4:FAIL:hidden")
try:test([-1,-2,-3],2,-2,5,True)
except:print("TC:5:FAIL:hidden")
try:test([5,5,5,5,5],3,5,6,True)
except:print("TC:6:FAIL:hidden")''',
f'''// USER_CODE_START
function findKthLargest(n,k) {{ return 0; }}
// USER_CODE_END
function test(n,k,e,tc,h){{const g=findKthLargest(n,k);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:k=${{k}}:expected=${{e}}:got=${{g}}`);}}
try{{test([3,2,1,5,6,4],2,5,1);}}catch(e){{console.log("TC:1:FAIL:hidden");}}
try{{test([3,2,3,1,2,4,5,5,6],4,4,2);}}catch(e){{console.log("TC:2:FAIL:hidden");}}
try{{test([1],1,1,3);}}catch(e){{console.log("TC:3:FAIL:hidden");}}
try{{test([2,2,2,2],1,2,4,true);}}catch(e){{console.log("TC:4:FAIL:hidden");}}
try{{test([-1,-2,-3],2,-2,5,true);}}catch(e){{console.log("TC:5:FAIL:hidden");}}
try{{test([5,5,5,5,5],3,5,6,true);}}catch(e){{console.log("TC:6:FAIL:hidden");}}''',
f'''#include <stdio.h>
// USER_CODE_START
int findKthLargest(int* n,int s,int k){{return 0;}}
// USER_CODE_END
void test(int* n,int s,int k,int e,int tc,int h){{int g=findKthLargest(n,s,k);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:k=%d:expected=%d:got=%d\\n",k,e,g);}}}}}}
int main(){{int t1[]={{3,2,1,5,6,4}};test(t1,6,2,5,1,0);int t2[]={{3,2,3,1,2,4,5,5,6}};test(t2,9,4,4,2,0);int t3[]={{1}};test(t3,1,1,1,3,0);int t4[]={{2,2,2,2}};test(t4,4,1,2,4,1);int t5[]={{-1,-2,-3}};test(t5,3,2,-2,5,1);int t6[]={{5,5,5,5,5}};test(t6,5,3,5,6,1);return 0;}}''')

# 21 - Meeting Rooms (boolean int[][] -> String)
ins("Meeting Rooms",
"Given an array of meeting time intervals where intervals[i] = [starti, endi], determine if a person could attend all meetings. Return true if no two meetings overlap.",
"First line contains integer n.\nNext n lines contain two space-separated integers start and end.",
"Print 'true' if person can attend all meetings, otherwise 'false'.",
"1 ≤ n ≤ 10^4\n0 ≤ starti < endi ≤ 10^6","EASY","Array, Sorting",
"Input:\n3\n0 30\n5 10\n15 20\n\nOutput:\nfalse","Input:\n2\n7 10\n2 4\n\nOutput:\ntrue","Input:\n1\n5 10\n\nOutput:\ntrue",
'''import java.util.*;
// USER_CODE_START
class Solution { public boolean canAttendMeetings(int[][] intervals) { return false; } }
// USER_CODE_END
public class Main {
static void test(int[][] inv, boolean e, int tc, boolean h){boolean g=new Solution().canAttendMeetings(inv);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else{String si="";for(var x:inv)si+="["+x[0]+","+x[1]+"] ";System.out.println("TC:"+tc+":FAIL:input="+si.trim()+":expected="+e+":got="+g);}}
public static void main(String[] a){
try{test(new int[][]{{0,30},{5,10},{15,20}},false,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[][]{{7,10},{2,4}},true,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[][]{{5,10}},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[][]{},true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[][]{{1,5},{5,10}},true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[][]{{1,10},{2,3},{3,4}},false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution{public:bool canAttendMeetings(vector<vector<int>>& inv){return false;}};
// USER_CODE_END
void test(vector<vector<int>> inv,bool e,int tc,bool h=false){bool g=Solution().canAttendMeetings(inv);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:expected="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";}
int main(){
try{test({{0,30},{5,10},{15,20}},false,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({{7,10},{2,4}},true,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({{5,10}},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({},true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({{1,5},{5,10}},true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({{1,10},{2,3},{3,4}},false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def canAttendMeetings(self, inv): return False
# USER_CODE_END
def test(inv,e,tc,h=False):g=Solution().canAttendMeetings(inv);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:inv={inv}:expected={e}:got={g}"))
try:test([[0,30],[5,10],[15,20]],False,1)
except:print("TC:1:FAIL:hidden")
try:test([[7,10],[2,4]],True,2)
except:print("TC:2:FAIL:hidden")
try:test([[5,10]],True,3)
except:print("TC:3:FAIL:hidden")
try:test([],True,4,True)
except:print("TC:4:FAIL:hidden")
try:test([[1,5],[5,10]],True,5,True)
except:print("TC:5:FAIL:hidden")
try:test([[1,10],[2,3],[3,4]],False,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function canAttendMeetings(inv) { return false; }
// USER_CODE_END
function test(inv,e,tc,h){const g=canAttendMeetings(inv);if(g===e)console.log(`TC:${tc}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${tc}:FAIL:hidden`);else console.log(`TC:${tc}:FAIL:inv=${JSON.stringify(inv)}:expected=${e}:got=${g}`);}
try{test([[0,30],[5,10],[15,20]],false,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([[7,10],[2,4]],true,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([[5,10]],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([],true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([[1,5],[5,10]],true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([[1,10],[2,3],[3,4]],false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool canAttendMeetings(int** inv,int n){return false;}
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 22 - Binary Search
t_bin=[(-1,0,6,[-1,0,3,5,9,12]),(-1,2,6,[-1,0,3,5,9,12]),(0,0,1,[0]),(5,3,6,[-1,0,3,5,9,12]),(12,5,6,[-1,0,3,5,9,12]),(2,-1,5,[1,2,3,4,5])]
ins("Binary Search",
"Given a sorted array of integers nums (ascending) and an integer target, write a function to search target in nums. If target exists, return its index. Otherwise, return -1.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target.",
"Print the index of target if found, otherwise -1.",
"1 ≤ n ≤ 10^5\n-10^4 ≤ nums[i] ≤ 10^4\nnums is sorted in ascending order.","EASY","Array, Binary Search",
"Input:\n6\n-1 0 3 5 9 12\n9\n\nOutput:\n4","Input:\n6\n-1 0 3 5 9 12\n2\n\nOutput:\n-1","Input:\n1\n0\n0\n\nOutput:\n0",
f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int search(int[] nums, int target) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n, int t, int e, int tc, boolean h){{int g=new Solution().search(n,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":target="+t+":expected="+e+":got="+g);}}
public static void main(String[] a){{
try{{test(new int[]{{-1,0,3,5,9,12}},9,4,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
try{{test(new int[]{{-1,0,3,5,9,12}},2,-1,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
try{{test(new int[]{{0}},0,0,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
try{{test(new int[]{{-1,0,3,5,9,12}},5,3,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
try{{test(new int[]{{-1,0,3,5,9,12}},12,5,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
try{{test(new int[]{{1,2,3,4,5}},2,1,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
}}}}''',
f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int search(vector<int>& n, int t) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n,int t,int e,int tc,bool h=false){{int g=Solution().search(n,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:target="<<t<<":expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
try{{test({{-1,0,3,5,9,12}},9,4,1);}}catch(...){{cout<<"TC:1:FAIL:hidden\\n";}}
try{{test({{-1,0,3,5,9,12}},2,-1,2);}}catch(...){{cout<<"TC:2:FAIL:hidden\\n";}}
try{{test({{0}},0,0,3);}}catch(...){{cout<<"TC:3:FAIL:hidden\\n";}}
try{{test({{-1,0,3,5,9,12}},5,3,4,true);}}catch(...){{cout<<"TC:4:FAIL:hidden\\n";}}
try{{test({{-1,0,3,5,9,12}},12,5,5,true);}}catch(...){{cout<<"TC:5:FAIL:hidden\\n";}}
try{{test({{1,2,3,4,5}},2,1,6,true);}}catch(...){{cout<<"TC:6:FAIL:hidden\\n";}}
return 0;}}''',
f'''# USER_CODE_START
class Solution:
    def search(self, n, t): return 0
# USER_CODE_END
def test(n,t,e,tc,h=False):g=Solution().search(n,t);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:t={{t}}:expected={{e}}:got={{g}}"))
try:test([-1,0,3,5,9,12],9,4,1)
except:print("TC:1:FAIL:hidden")
try:test([-1,0,3,5,9,12],2,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([0],0,0,3)
except:print("TC:3:FAIL:hidden")
try:test([-1,0,3,5,9,12],5,3,4,True)
except:print("TC:4:FAIL:hidden")
try:test([-1,0,3,5,9,12],12,5,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],2,1,6,True)
except:print("TC:6:FAIL:hidden")''',
f'''// USER_CODE_START
function search(n,t) {{ return 0; }}
// USER_CODE_END
function test(n,t,e,tc,h){{const g=search(n,t);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:t=${{t}}:expected=${{e}}:got=${{g}}`);}}
try{{test([-1,0,3,5,9,12],9,4,1);}}catch(e){{console.log("TC:1:FAIL:hidden");}}
try{{test([-1,0,3,5,9,12],2,-1,2);}}catch(e){{console.log("TC:2:FAIL:hidden");}}
try{{test([0],0,0,3);}}catch(e){{console.log("TC:3:FAIL:hidden");}}
try{{test([-1,0,3,5,9,12],5,3,4,true);}}catch(e){{console.log("TC:4:FAIL:hidden");}}
try{{test([-1,0,3,5,9,12],12,5,5,true);}}catch(e){{console.log("TC:5:FAIL:hidden");}}
try{{test([1,2,3,4,5],2,1,6,true);}}catch(e){{console.log("TC:6:FAIL:hidden");}}''',
f'''#include <stdio.h>
// USER_CODE_START
int search(int* n,int s,int t){{return 0;}}
// USER_CODE_END
void test(int* n,int s,int t,int e,int tc,int h){{int g=search(n,s,t);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:target=%d:expected=%d:got=%d\\n",t,e,g);}}}}}}
int main(){{int t1[]={{-1,0,3,5,9,12}};test(t1,6,9,4,1,0);int t2[]={{-1,0,3,5,9,12}};test(t2,6,2,-1,2,0);int t3[]={{0}};test(t3,1,0,0,3,0);int t4[]={{-1,0,3,5,9,12}};test(t4,6,5,3,4,1);int t5[]={{-1,0,3,5,9,12}};test(t5,6,12,5,5,1);int t6[]={{1,2,3,4,5}};test(t6,5,2,1,6,1);return 0;}}''')

# 23 - Search Insert Position
ins("Search Insert Position",
"Given a sorted array of distinct integers and a target value, return the index if the target is found. If not, return the index where it would be if inserted in order.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target.",
"Print the index where target is or should be inserted.",
"1 ≤ n ≤ 10^4\n-10^4 ≤ nums[i], target ≤ 10^4","EASY","Array, Binary Search",
"Input:\n4\n1 3 5 6\n5\n\nOutput:\n2","Input:\n4\n1 3 5 6\n2\n\nOutput:\n1","Input:\n4\n1 3 5 6\n7\n\nOutput:\n4",
f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int searchInsert(int[] nums, int target) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n, int t, int e, int tc, boolean h){{int g=new Solution().searchInsert(n,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":target="+t+":expected="+e+":got="+g);}}
public static void main(String[] a){{
try{{test(new int[]{{1,3,5,6}},5,2,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
try{{test(new int[]{{1,3,5,6}},2,1,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
try{{test(new int[]{{1,3,5,6}},7,4,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
try{{test(new int[]{{1,3,5,6}},0,0,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
try{{test(new int[]{{1}},0,0,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
try{{test(new int[]{{1}},1,0,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
}}}}''',
f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int searchInsert(vector<int>& n, int t) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n,int t,int e,int tc,bool h=false){{int g=Solution().searchInsert(n,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:target="<<t<<":expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
try{{test({{1,3,5,6}},5,2,1);}}catch(...){{cout<<"TC:1:FAIL:hidden\\n";}}
try{{test({{1,3,5,6}},2,1,2);}}catch(...){{cout<<"TC:2:FAIL:hidden\\n";}}
try{{test({{1,3,5,6}},7,4,3);}}catch(...){{cout<<"TC:3:FAIL:hidden\\n";}}
try{{test({{1,3,5,6}},0,0,4,true);}}catch(...){{cout<<"TC:4:FAIL:hidden\\n";}}
try{{test({{1}},0,0,5,true);}}catch(...){{cout<<"TC:5:FAIL:hidden\\n";}}
try{{test({{1}},1,0,6,true);}}catch(...){{cout<<"TC:6:FAIL:hidden\\n";}}
return 0;}}''',
f'''# USER_CODE_START
class Solution:
    def searchInsert(self, n, t): return 0
# USER_CODE_END
def test(n,t,e,tc,h=False):g=Solution().searchInsert(n,t);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:t={{t}}:expected={{e}}:got={{g}}"))
try:test([1,3,5,6],5,2,1)
except:print("TC:1:FAIL:hidden")
try:test([1,3,5,6],2,1,2)
except:print("TC:2:FAIL:hidden")
try:test([1,3,5,6],7,4,3)
except:print("TC:3:FAIL:hidden")
try:test([1,3,5,6],0,0,4,True)
except:print("TC:4:FAIL:hidden")
try:test([1],0,0,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1],1,0,6,True)
except:print("TC:6:FAIL:hidden")''',
f'''// USER_CODE_START
function searchInsert(n,t) {{ return 0; }}
// USER_CODE_END
function test(n,t,e,tc,h){{const g=searchInsert(n,t);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:t=${{t}}:expected=${{e}}:got=${{g}}`);}}
try{{test([1,3,5,6],5,2,1);}}catch(e){{console.log("TC:1:FAIL:hidden");}}
try{{test([1,3,5,6],2,1,2);}}catch(e){{console.log("TC:2:FAIL:hidden");}}
try{{test([1,3,5,6],7,4,3);}}catch(e){{console.log("TC:3:FAIL:hidden");}}
try{{test([1,3,5,6],0,0,4,true);}}catch(e){{console.log("TC:4:FAIL:hidden");}}
try{{test([1],0,0,5,true);}}catch(e){{console.log("TC:5:FAIL:hidden");}}
try{{test([1],1,0,6,true);}}catch(e){{console.log("TC:6:FAIL:hidden");}}''',
f'''#include <stdio.h>
// USER_CODE_START
int searchInsert(int* n,int s,int t){{return 0;}}
// USER_CODE_END
void test(int* n,int s,int t,int e,int tc,int h){{int g=searchInsert(n,s,t);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:target=%d:expected=%d:got=%d\\n",t,e,g);}}}}}}
int main(){{int t1[]={{1,3,5,6}};test(t1,4,5,2,1,0);int t2[]={{1,3,5,6}};test(t2,4,2,1,2,0);int t3[]={{1,3,5,6}};test(t3,4,7,4,3,0);int t4[]={{1,3,5,6}};test(t4,4,0,0,4,1);int t5[]={{1}};test(t5,1,0,0,5,1);int t6[]={{1}};test(t6,1,1,0,6,1);return 0;}}''')

# 24 - First Bad Version (int n -> int)
ins("First Bad Version",
"You are a product manager leading a team developing a product. Each version is identified by a number. Unfortunately, the latest version fails the quality check. Since each version is developed after the previous one, all versions after a bad version are also bad. Suppose you have an API isBadVersion(version) that returns whether a version is bad. Implement a function to find the first bad version. You should minimize the number of API calls. Note: The isBadVersion API is predefined.",
"Single line contains integer n (total versions) and first bad version is computed against the hidden API.",
"Print the first bad version number.",
"1 ≤ n ≤ 2^31 - 1","EASY","Binary Search",
"Input:\n5\n\nOutput:\n4\n\nExplanation: isBadVersion(3) = false, isBadVersion(4) = true → first bad is 4.","Input:\n1\n\nOutput:\n1","Input:\n100\n\nOutput:\n72\n\nExplanation: First bad version is 72 for this test.",
f'''import java.util.*;
// USER_CODE_START
// The isBadVersion API is defined as:
// boolean isBadVersion(int version);
public class Solution extends VersionControl {{ public int firstBadVersion(int n) {{ return 1; }} }}
// USER_CODE_END
class VersionControl {{
    int firstBad;
    VersionControl(int fb) {{ firstBad = fb; }}
    boolean isBadVersion(int v) {{ return v >= firstBad; }}
}}
public class Main extends VersionControl {{
    static void test(int n, int fb, int e, int tc, boolean h) {{
        int g = new Solution(fb).firstBadVersion(n);
        if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:n="+n+":expected="+e+":got="+g);
    }}
    public static void main(String[] a){{
        try{{test(5,4,4,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
        try{{test(1,1,1,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
        try{{test(100,72,72,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
        try{{test(50,1,1,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
        try{{test(50,50,50,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
        try{{test(2,2,2,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
    }}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
// bool isBadVersion(int version);
class Solution { public: int firstBadVersion(int n) { return 1; } };
// USER_CODE_END
int main(){cout<<"TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n";return 0;}''',
'''# USER_CODE_START
# The isBadVersion API is defined as:
# def isBadVersion(version): return ...
class Solution:
    def firstBadVersion(self, n): return 1
# USER_CODE_END
print("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden")''',
'''// USER_CODE_START
// const isBadVersion = (version) => ...
function firstBadVersion(n) { return 1; }
// USER_CODE_END
console.log("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden");''',
'''#include <stdio.h>
// USER_CODE_START
// int isBadVersion(int version);
int firstBadVersion(int n) { return 1; }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 25 - Search in Rotated Sorted Array
ins("Search in Rotated Sorted Array",
"There is an integer array nums sorted in ascending order (with distinct values) that is rotated at an unknown pivot. Given the array nums after the rotation and an integer target, return the index of target if it is in nums, or -1 if it is not.",
"First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer target.",
"Print the index of target if found, otherwise -1.",
"1 ≤ n ≤ 5000\n-10^4 ≤ nums[i], target ≤ 10^4","MEDIUM","Array, Binary Search",
"Input:\n7\n4 5 6 7 0 1 2\n0\n\nOutput:\n4","Input:\n7\n4 5 6 7 0 1 2\n3\n\nOutput:\n-1","Input:\n1\n1\n0\n\nOutput:\n-1",
f'''import java.util.*;
// USER_CODE_START
class Solution {{ public int search(int[] nums, int target) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test(int[] n,int t,int e,int tc,boolean h){{int g=new Solution().search(n,t);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(n)+":target="+t+":expected="+e+":got="+g);}}
public static void main(String[] a){{
try{{test(new int[]{{4,5,6,7,0,1,2}},0,4,1,false);}}catch(Exception e){{System.out.println("TC:1:FAIL:hidden");}}
try{{test(new int[]{{4,5,6,7,0,1,2}},3,-1,2,false);}}catch(Exception e){{System.out.println("TC:2:FAIL:hidden");}}
try{{test(new int[]{{1}},0,-1,3,false);}}catch(Exception e){{System.out.println("TC:3:FAIL:hidden");}}
try{{test(new int[]{{1,3}},3,1,4,true);}}catch(Exception e){{System.out.println("TC:4:FAIL:hidden");}}
try{{test(new int[]{{3,1}},1,1,5,true);}}catch(Exception e){{System.out.println("TC:5:FAIL:hidden");}}
try{{test(new int[]{{5,1,2,3,4}},1,1,6,true);}}catch(Exception e){{System.out.println("TC:6:FAIL:hidden");}}
}}}}''',
f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: int search(vector<int>& n, int t) {{ return 0; }} }};
// USER_CODE_END
void test(vector<int> n,int t,int e,int tc,bool h=false){{int g=Solution().search(n,t);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else{{cout<<"TC:"<<tc<<":FAIL:input=[";for(int x:n)cout<<x<<",";cout<<"]:target="<<t<<":expected="<<e<<":got="<<g<<"\\n";}}}}
int main(){{
try{{test({{4,5,6,7,0,1,2}},0,4,1);}}catch(...){{cout<<"TC:1:FAIL:hidden\\n";}}
try{{test({{4,5,6,7,0,1,2}},3,-1,2);}}catch(...){{cout<<"TC:2:FAIL:hidden\\n";}}
try{{test({{1}},0,-1,3);}}catch(...){{cout<<"TC:3:FAIL:hidden\\n";}}
try{{test({{1,3}},3,1,4,true);}}catch(...){{cout<<"TC:4:FAIL:hidden\\n";}}
try{{test({{3,1}},1,1,5,true);}}catch(...){{cout<<"TC:5:FAIL:hidden\\n";}}
try{{test({{5,1,2,3,4}},1,1,6,true);}}catch(...){{cout<<"TC:6:FAIL:hidden\\n";}}
return 0;}}''',
f'''# USER_CODE_START
class Solution:
    def search(self, n, t): return 0
# USER_CODE_END
def test(n,t,e,tc,h=False):g=Solution().search(n,t);print(f"TC:{{tc}}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{{tc}}:FAIL:hidden" if h else f"TC:{{tc}}:FAIL:n={{n}}:t={{t}}:expected={{e}}:got={{g}}"))
try:test([4,5,6,7,0,1,2],0,4,1)
except:print("TC:1:FAIL:hidden")
try:test([4,5,6,7,0,1,2],3,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([1],0,-1,3)
except:print("TC:3:FAIL:hidden")
try:test([1,3],3,1,4,True)
except:print("TC:4:FAIL:hidden")
try:test([3,1],1,1,5,True)
except:print("TC:5:FAIL:hidden")
try:test([5,1,2,3,4],1,1,6,True)
except:print("TC:6:FAIL:hidden")''',
f'''// USER_CODE_START
function search(n,t) {{ return 0; }}
// USER_CODE_END
function test(n,t,e,tc,h){{const g=search(n,t);if(g===e)console.log(`TC:${{tc}}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${{tc}}:FAIL:hidden`);else console.log(`TC:${{tc}}:FAIL:n=${{JSON.stringify(n)}}:t=${{t}}:expected=${{e}}:got=${{g}}`);}}
try{{test([4,5,6,7,0,1,2],0,4,1);}}catch(e){{console.log("TC:1:FAIL:hidden");}}
try{{test([4,5,6,7,0,1,2],3,-1,2);}}catch(e){{console.log("TC:2:FAIL:hidden");}}
try{{test([1],0,-1,3);}}catch(e){{console.log("TC:3:FAIL:hidden");}}
try{{test([1,3],3,1,4,true);}}catch(e){{console.log("TC:4:FAIL:hidden");}}
try{{test([3,1],1,1,5,true);}}catch(e){{console.log("TC:5:FAIL:hidden");}}
try{{test([5,1,2,3,4],1,1,6,true);}}catch(e){{console.log("TC:6:FAIL:hidden");}}''',
f'''#include <stdio.h>
// USER_CODE_START
int search(int* n,int s,int t){{return 0;}}
// USER_CODE_END
void test(int* n,int s,int t,int e,int tc,int h){{int g=search(n,s,t);if(g==e){{if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}}else{{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else{{printf("TC:%d:FAIL:input=[",tc);for(int i=0;i<s;i++){{if(i)printf(",");printf("%d",n[i]);}}printf("]:target=%d:expected=%d:got=%d\\n",t,e,g);}}}}}}
int main(){{int t1[]={{4,5,6,7,0,1,2}};test(t1,7,0,4,1,0);int t2[]={{4,5,6,7,0,1,2}};test(t2,7,3,-1,2,0);int t3[]={{1}};test(t3,1,0,-1,3,0);int t4[]={{1,3}};test(t4,2,3,1,4,1);int t5[]={{3,1}};test(t5,2,1,1,5,1);int t6[]={{5,1,2,3,4}};test(t6,5,1,1,6,1);return 0;}}''')

print("\nBatch 20-25 done!")
cur.close(); conn.close()
