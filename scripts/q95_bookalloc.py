"""
Book Allocation Problem (Allocate Minimum Number of Pages)
============================================================
Given an array pages of length n (each entry is the number of pages in a book,
books are in the given order) and an integer m (number of students), assign
each student a contiguous block of books so that every book is assigned and the
MAXIMUM pages read by any student is MINIMIZED. Return that minimized maximum.
If m > n (more students than books), return -1.

Examples:
  pages = [12,34,67,90], m = 2 -> 113   (split: [12,34,67] & [90])
  pages = [10,20,30,40], m = 2 -> 60    (split: [10,20,30] & [40])

Binary search the answer in [max(pages), sum(pages)]. For a candidate cap,
greedily count the number of students needed so no student reads more than cap
pages (accumulate while cur+pages[i] <= cap, else start a new student). If
students used <= m, cap is feasible (try smaller); else we need a larger cap.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C the pages array is passed with its length n: int* pages, int n, int m.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Book Allocation Problem"
desc=(
    "You are given an array pages of n positive integers where pages[i] is the "
    "number of pages in the i-th book, and an integer m (the number of "
    "students). Assign every book to one of the m students so that:\n"
    "- each student gets a CONTIGUOUS block of books (in the given order), and\n"
    "- every book is assigned.\n"
    "Minimize the maximum number of pages any single student has to read, and "
    "return that minimized maximum value. If m > n (more students than books), "
    "return -1.\n\n"
    "For example:\n"
    "pages = [12,34,67,90], m = 2 -> 113   (split [12,34,67] and [90])\n"
    "pages = [10,20,30,40], m = 2 -> 60    (split [10,20,30] and [40])\n\n"
    "Binary search the answer in [max(pages), sum(pages)]. For a candidate "
    "capacity cap, greedily count how many students are needed so that no one "
    "reads more than cap pages. If that count <= m, cap is feasible (try a "
    "smaller one); otherwise we need a larger cap. Runs in O(n * log(sum))."
)
infmt="First line contains n (number of books) and m (number of students). Second line contains n space-separated page counts."
outfmt="Print the minimized maximum number of pages per student, or -1 if m > n."
cons="1 ≤ n ≤ 10^5\n1 ≤ m ≤ 10^5\n1 ≤ pages[i] ≤ 10^4\nReturn -1 when m > n."
e1="Input:\n4 2\n12 34 67 90\n\nOutput:\n113"
e2="Input:\n4 2\n10 20 30 40\n\nOutput:\n60"
e3="Input:\n4 5\n10 20 30 40\n\nOutput:\n-1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Binary Search",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int allocatePages(int[] pages, int m) {
        // Write your code here — binary search the minimized maximum, or -1
        return 0;
    }
}
// USER_CODE_END

public class Main {
static void test(int[] p,int m,int e,int tc,boolean hd){int r=new CodeCoder().allocatePages(p,m);if(r==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:pages="+Arrays.toString(p)+":m="+m+":exp="+e+":got="+r);}
public static void main(String[] a){
try{test(new int[]{12,34,67,90},2,113,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{10,20,30,40},2,60,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{5,5,5,5},2,10,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{25,46,28,49,24},4,71,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{15,17,20},2,32,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},3,6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},1,15,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},5,5,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{10,20,30,40},4,40,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{10,20,30,40},5,-1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int allocatePages(vector<int>& pages,int m){return 0;}};
// USER_CODE_END
void test(vector<int> p,int m,int e,int tc,bool hd=false){int r=CodeCoder().allocatePages(p,m);if(r==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<r<<"\\n";}
int main(){
try{test({12,34,67,90},2,113,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({10,20,30,40},2,60,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({5,5,5,5},2,10,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({25,46,28,49,24},4,71,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({15,17,20},2,32,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,3,4,5},3,6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5},1,15,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,3,4,5},5,5,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({10,20,30,40},4,40,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({10,20,30,40},5,-1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def allocatePages(self, pages, m):
        return 0
# USER_CODE_END
def test(p,m,e,tc,hd=False):r=CodeCoder().allocatePages(p,m);print(f"TC:{tc}:PASS"+(":hidden" if hd else "") if r==e else (f"TC:{tc}:FAIL:hidden" if hd else f"TC:{tc}:FAIL:pages={p}:m={m}:exp={e}:got={r}"))
try:test([12,34,67,90],2,113,1)
except:print("TC:1:FAIL:hidden")
try:test([10,20,30,40],2,60,2)
except:print("TC:2:FAIL:hidden")
try:test([5,5,5,5],2,10,3)
except:print("TC:3:FAIL:hidden")
try:test([25,46,28,49,24],4,71,4)
except:print("TC:4:FAIL:hidden")
try:test([15,17,20],2,32,5)
except:print("TC:5:FAIL:hidden")
try:test([1,2,3,4,5],3,6,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5],1,15,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,3,4,5],5,5,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([10,20,30,40],4,40,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([10,20,30,40],5,-1,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function allocatePages(pages, m) { return 0; }
// USER_CODE_END
function test(p,m,e,tc,hd){if(hd===undefined)hd=false;const r=allocatePages(p,m);if(r===e)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+r);}
try{test([12,34,67,90],2,113,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([10,20,30,40],2,60,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([5,5,5,5],2,10,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([25,46,28,49,24],4,71,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([15,17,20],2,32,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,3,4,5],3,6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5],1,15,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,3,4,5],5,5,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([10,20,30,40],4,40,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([10,20,30,40],5,-1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

// USER_CODE_START
int allocatePages(int* pages,int n,int m) {
    // Write your code here — return the minimized maximum, or -1
    return 0;
}
// USER_CODE_END

void runTest(int* p,int n,int m,int e,int tc,int hd){
    int r=allocatePages(p,n,m);
    if(r==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,r);}
}
int main(){
    int t1[]={12,34,67,90};runTest(t1,4,2,113,1,0);
    int t2[]={10,20,30,40};runTest(t2,4,2,60,2,0);
    int t3[]={5,5,5,5};runTest(t3,4,2,10,3,0);
    int t4[]={25,46,28,49,24};runTest(t4,5,4,71,4,0);
    int t5[]={15,17,20};runTest(t5,3,2,32,5,0);
    int t6[]={1,2,3,4,5};runTest(t6,5,3,6,6,1);
    int t7[]={1,2,3,4,5};runTest(t7,5,1,15,7,1);
    int t8[]={1,2,3,4,5};runTest(t8,5,5,5,8,1);
    int t9[]={10,20,30,40};runTest(t9,4,4,40,9,1);
    int t10[]={10,20,30,40};runTest(t10,4,5,-1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
