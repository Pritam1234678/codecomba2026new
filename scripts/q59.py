"""
Palindrome Linked List
=======================
Given head of a singly linked list, return true if it is a palindrome.

Examples:
  1 → 2 → 2 → 1 → null  → true
  1 → 2 → null           → false

Approach: Find middle (slow/fast), reverse second half, compare.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Palindrome Linked List"
desc=(
    "Given the head of a singly linked list, return true if it is a palindrome "
    "or false otherwise.\n\n"
    "A palindrome reads the same forwards and backwards. "
    "For example: 1 → 2 → 2 → 1 is a palindrome, but 1 → 2 is not.\n\n"
    "Approach: Find the middle node using slow/fast pointers, "
    "reverse the second half, then compare both halves."
)
infmt="First line contains n.\nSecond line contains n space-separated integers."
outfmt="Print 'true' if palindrome, otherwise 'false'."
cons="1 ≤ n ≤ 10^5\n0 ≤ Node.val ≤ 9"
e1="Input:\n4\n1 2 2 1\n\nOutput:\ntrue"
e2="Input:\n2\n1 2\n\nOutput:\nfalse"
e3="Input:\n1\n1\n\nOutput:\ntrue"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Linked List, Two Pointers, Stack",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

class ListNode{int val;ListNode next;ListNode(int x){val=x;}}

// USER_CODE_START
class CodeCoder{
    public boolean isPalindrome(ListNode head){return false;}
}
// USER_CODE_END

public class Main{
    static ListNode build(int[] a){
        ListNode d=new ListNode(0),c=d;
        for(int v:a){c.next=new ListNode(v);c=c.next;}
        return d.next;
    }
    static void test(int[] a,boolean e,int tc,boolean h){
        boolean g=new CodeCoder().isPalindrome(build(a));
        if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
        else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:input="+Arrays.toString(a)+":exp="+e+":got="+g);
    }
    public static void main(String[] a){
        try{test(new int[]{1,2,2,1},true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test(new int[]{1,2},false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(new int[]{1},true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test(new int[]{1,2,3,2,1},true,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(new int[]{1,2,3,4,5,6},false,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(new int[]{1,1,1,1,1},true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
        try{test(new int[]{1,2,3,4,5,4,3,2,1},true,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
        try{test(new int[]{1,2,2,3},false,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
        try{test(new int[]{1,2,3,2,1,1},false,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
        try{test(new int[]{},true,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
struct ListNode{int val;ListNode* next;ListNode(int x):val(x),next(NULL){}};

// USER_CODE_START
class CodeCoder{public:bool isPalindrome(ListNode* h){return false;}};
// USER_CODE_END

ListNode* build(vector<int>& a){
    ListNode d(0),*c=&d;
    for(int v:a){c->next=new ListNode(v);c=c->next;}
    return d.next;
}
void test(vector<int> a,bool e,int tc,bool h=false){
    bool g=CodeCoder().isPalindrome(build(a));
    if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
    else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:exp="<<(e?"true":"false")<<":got="<<(g?"true":"false")<<"\\n";
}
int main(){
try{test({1,2,2,1},true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2},false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,2,3,2,1},true,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},false,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,1,1,1,1},true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({1,2,3,4,5,4,3,2,1},true,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({1,2,2,3},false,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,2,3,2,1,1},false,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({},true,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
# class ListNode:
#     def __init__(self, val=0, next=None):
#         self.val = val; self.next = next
class CodeCoder:
    def isPalindrome(self, head):
        return False
# USER_CODE_END

class ListNode:
    def __init__(self, val=0, next=None):
        self.val = val; self.next = next

def build(a):
    d=ListNode(0);c=d
    for v in a:c.next=ListNode(v);c=c.next
    return d.next

def test(a,e,tc,h=False):
    g=CodeCoder().isPalindrome(build(a))
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:exp={e}:got={g}")

try:test([1,2,2,1],True,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2],False,2)
except:print("TC:2:FAIL:hidden")
try:test([1],True,3)
except:print("TC:3:FAIL:hidden")
try:test([1,2,3,2,1],True,4)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6],False,5)
except:print("TC:5:FAIL:hidden")
try:test([1,1,1,1,1],True,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([1,2,3,4,5,4,3,2,1],True,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([1,2,2,3],False,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,2,3,2,1,1],False,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([],True,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
// class ListNode{constructor(val,next){this.val=val;this.next=next||null;}}
function isPalindrome(head){return false;}
// USER_CODE_END

class ListNode{constructor(val,next){this.val=val;this.next=next||null;}}
function build(a){
    let d=new ListNode(0),c=d;
    for(let v of a){c.next=new ListNode(v);c=c.next;}
    return d.next;
}
function test(a,e,tc,h){if(h===undefined)h=false;
    const g=isPalindrome(build(a));
    if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);
}
try{test([1,2,2,1],true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2],false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,2,3,2,1],true,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6],false,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,1,1,1,1],true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([1,2,3,4,5,4,3,2,1],true,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([1,2,2,3],false,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,2,3,2,1,1],false,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([],true,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

struct ListNode{int val;struct ListNode* next;};

// USER_CODE_START
bool isPalindrome(struct ListNode* h){return false;}
// USER_CODE_END

struct ListNode* build(int* a,int n){
    if(!n)return NULL;
    struct ListNode* h=malloc(sizeof(struct ListNode));
    h->val=a[0];h->next=NULL;struct ListNode* c=h;
    for(int i=1;i<n;i++){c->next=malloc(sizeof(struct ListNode));c=c->next;c->val=a[i];c->next=NULL;}
    return h;
}
void run(int* a,int n,bool e,int tc,int h){
    bool g=isPalindrome(build(a,n));
    if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%s:got=%s\\n",tc,e?"true":"false",g?"true":"false");}
}
int main(){
int t1[]={1,2,2,1};run(t1,4,true,1,0);
int t2[]={1,2};run(t2,2,false,2,0);
int t3[]={1};run(t3,1,true,3,0);
int t4[]={1,2,3,2,1};run(t4,5,true,4,0);
int t5[]={1,2,3,4,5,6};run(t5,6,false,5,0);
int t6[]={1,1,1,1,1};run(t6,5,true,6,1);
int t7[]={1,2,3,4,5,4,3,2,1};run(t7,9,true,7,1);
int t8[]={1,2,2,3};run(t8,4,false,8,1);
int t9[]={1,2,3,2,1,1};run(t9,6,false,9,1);
run(NULL,0,true,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
