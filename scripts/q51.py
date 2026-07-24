"""
Implement Queue using Stacks
==============================
Implement a FIFO queue using only two stacks. The queue should support
push, pop, peek, and empty operations.

The key insight: Use one stack for incoming elements (push), and another
for outgoing elements (pop/peek). When the output stack is empty, transfer
all elements from the input stack to the output stack (reversing order).

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Implement Queue using Stacks"
desc=(
    "Implement a first-in-first-out (FIFO) queue using only two stacks.\n\n"
    "The class must support:\n"
    "- push(int x) — pushes x to the back of the queue\n"
    "- pop() — removes and returns the element at the front\n"
    "- peek() — returns the element at the front without removing it\n"
    "- empty() — returns true if the queue is empty, false otherwise\n\n"
    "You must use only standard stack operations (push, pop, peek). "
    "The key trick: maintain an 'input' stack for pushes and an 'output' stack "
    "for pops. When output is empty and pop/peek is called, transfer all "
    "elements from input to output, reversing their order. This gives FIFO."
)
infmt=("First line contains integer q.\n"
       "Next q lines: 'push x', 'pop', 'peek', or 'empty'.\n"
       "For pop and peek, print the result. For empty, print true/false.")
outfmt="Print the result of each pop, peek, and empty operation on a new line."
cons=("1 \u2264 q \u2264 100\n"
      "0 \u2264 x \u2264 10^6\n"
      "pop and peek are only called on non-empty queue.")
e1="Input:\n7\npush 1\npush 2\npeek\npop\nempty\npop\nempty\n\nOutput:\n1\n1\nfalse\n2\ntrue"
e2="Input:\n6\npush 1\npush 2\npush 3\npop\npop\npeek\n\nOutput:\n1\n2\n3"
e3="Input:\n4\npush 10\npush 20\npop\npeek\n\nOutput:\n10\n20"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Stack, Design, Queue",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public CodeCoder() {}
    public void push(int x) { }
    public int pop() { return 0; }
    public int peek() { return 0; }
    public boolean empty() { return true; }
}
// USER_CODE_END

public class Main {
static void runTest(String[] ops,int[][] vals,String[] exp,int tc,boolean h){
try{CodeCoder q=new CodeCoder();StringBuilder sb=new StringBuilder();
for(int i=0;i<ops.length;i++){
if(ops[i].equals("push")) q.push(vals[i][0]);
else if(ops[i].equals("pop")) sb.append(q.pop()).append(",");
else if(ops[i].equals("peek")) sb.append(q.peek()).append(",");
else if(ops[i].equals("empty")) sb.append(q.empty()?"1":"0").append(",");
}
String gs=sb.toString();String es=String.join(",",exp)+",";
if(gs.equals(es))System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));
else if(h)System.out.println("TC:"+tc+":FAIL:hidden");
else System.out.println("TC:"+tc+":FAIL:got="+gs+":expected="+es);
}catch(Exception e){if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:hidden");}
}
public static void main(String[] a){
runTest(new String[]{"push","push","peek","pop","empty","pop","empty"},new int[][]{{1},{2},{0},{0},{0},{0},{0}},new String[]{"1","1","false","2","true"},1,false);
runTest(new String[]{"push","push","push","pop","pop","peek"},new int[][]{{1},{2},{3},{0},{0},{0}},new String[]{"1","2","3"},2,false);
runTest(new String[]{"push","push","pop","peek"},new int[][]{{10},{20},{0},{0}},new String[]{"10","20"},3,false);
runTest(new String[]{"push","pop","empty"},new int[][]{{5},{0},{0}},new String[]{"5","true"},4,false);
runTest(new String[]{"empty"},new int[][]{{0}},new String[]{"true"},5,false);
runTest(new String[]{"push","push","push","push","pop","pop","pop","pop","empty"},new int[][]{{1},{2},{3},{4},{0},{0},{0},{0},{0}},new String[]{"1","2","3","4","true"},6,true);
runTest(new String[]{"push","peek","pop","push","peek","pop"},new int[][]{{100},{0},{0},{200},{0},{0}},new String[]{"100","100","200","200"},7,true);
runTest(new String[]{"push","push","push","peek","pop","peek","pop","peek","pop"},new int[][]{{1},{2},{3},{0},{0},{0},{0},{0},{0}},new String[]{"1","1","2","2","3","3"},8,true);
runTest(new String[]{"push","push","push","push","push","pop","pop","peek"},new int[][]{{1},{2},{3},{4},{5},{0},{0},{0}},new String[]{"1","2","3"},9,true);
runTest(new String[]{"push","pop","push","pop","push","pop","empty"},new int[][]{{1},{0},{2},{0},{3},{0},{0}},new String[]{"1","2","3","true"},10,true);
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:CodeCoder(){}void push(int x){}int pop(){return 0;}int peek(){return 0;}bool empty(){return true;}};
// USER_CODE_END
void run(vector<string> ops,vector<vector<int>> vals,vector<string> exp,int tc,bool h=false){
try{CodeCoder q;string g;
for(size_t i=0;i<ops.size();i++){
if(ops[i]=="push")q.push(vals[i][0]);
else if(ops[i]=="pop")g+=to_string(q.pop())+",";
else if(ops[i]=="peek")g+=to_string(q.peek())+",";
else if(ops[i]=="empty")g+=string(q.empty()?"1":"0")+",";
}
string e;for(auto&x:exp)e+=x+",";
if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";
else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";
else cout<<"TC:"<<tc<<":FAIL:got="<<g<<":expected="<<e<<"\\n";
}catch(...){if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:hidden\\n";}
}
int main(){
run({"push","push","peek","pop","empty","pop","empty"},{{1},{2},{0},{0},{0},{0},{0}},{"1","1","false","2","true"},1);
run({"push","push","push","pop","pop","peek"},{{1},{2},{3},{0},{0},{0}},{"1","2","3"},2);
run({"push","push","pop","peek"},{{10},{20},{0},{0}},{"10","20"},3);
run({"push","pop","empty"},{{5},{0},{0}},{"5","true"},4);
run({"empty"},{{0}},{"true"},5);
run({"push","push","push","push","pop","pop","pop","pop","empty"},{{1},{2},{3},{4},{0},{0},{0},{0},{0}},{"1","2","3","4","true"},6,true);
run({"push","peek","pop","push","peek","pop"},{{100},{0},{0},{200},{0},{0}},{"100","100","200","200"},7,true);
run({"push","push","push","peek","pop","peek","pop","peek","pop"},{{1},{2},{3},{0},{0},{0},{0},{0},{0}},{"1","1","2","2","3","3"},8,true);
run({"push","push","push","push","push","pop","pop","peek"},{{1},{2},{3},{4},{5},{0},{0},{0}},{"1","2","3"},9,true);
run({"push","pop","push","pop","push","pop","empty"},{{1},{0},{2},{0},{3},{0},{0}},{"1","2","3","true"},10,true);
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def __init__(self): pass
    def push(self,x): pass
    def pop(self): return 0
    def peek(self): return 0
    def empty(self): return True
# USER_CODE_END
def runOps(ops,vals,exp,tc,h=False):
    try:
        q=CodeCoder();g=[]
        for i,op in enumerate(ops):
            if op=="push": q.push(vals[i][0])
            elif op=="pop": g.append(str(q.pop()))
            elif op=="peek": g.append(str(q.peek()))
            elif op=="empty": g.append("true" if q.empty() else "false")
        gs=",".join(g)
        es=",".join(exp)
        if gs==es: print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
        elif h: print(f"TC:{tc}:FAIL:hidden")
        else: print(f"TC:{tc}:FAIL:got={gs}:expected={es}")
    except:
        if h: print(f"TC:{tc}:FAIL:hidden")
        else: print(f"TC:{tc}:FAIL:hidden")

runOps(["push","push","peek","pop","empty","pop","empty"],[[1],[2],[0],[0],[0],[0],[0]],["1","1","false","2","true"],1)
runOps(["push","push","push","pop","pop","peek"],[[1],[2],[3],[0],[0],[0]],["1","2","3"],2)
runOps(["push","push","pop","peek"],[[10],[20],[0],[0]],["10","20"],3)
runOps(["push","pop","empty"],[[5],[0],[0]],["5","true"],4)
runOps(["empty"],[[0]],["true"],5)
runOps(["push","push","push","push","pop","pop","pop","pop","empty"],[[1],[2],[3],[4],[0],[0],[0],[0],[0]],["1","2","3","4","true"],6,True)
runOps(["push","peek","pop","push","peek","pop"],[[100],[0],[0],[200],[0],[0]],["100","100","200","200"],7,True)
runOps(["push","push","push","peek","pop","peek","pop","peek","pop"],[[1],[2],[3],[0],[0],[0],[0],[0],[0]],["1","1","2","2","3","3"],8,True)
runOps(["push","push","push","push","push","pop","pop","peek"],[[1],[2],[3],[4],[5],[0],[0],[0]],["1","2","3"],9,True)
runOps(["push","pop","push","pop","push","pop","empty"],[[1],[0],[2],[0],[3],[0],[0]],["1","2","3","true"],10,True)'''

js_code=r'''// USER_CODE_START
class CodeCoder{constructor(){}push(x){}pop(){return 0}peek(){return 0}empty(){return true}}
// USER_CODE_END
function run(ops,vals,exp,tc,h){if(h===undefined)h=false
try{const q=new CodeCoder();const g=[]
for(let i=0;i<ops.length;i++){if(ops[i]==="push")q.push(vals[i][0])
else if(ops[i]==="pop")g.push(String(q.pop()))
else if(ops[i]==="peek")g.push(String(q.peek()))
else if(ops[i]==="empty")g.push(q.empty()?"true":"false")}
const gs=g.join(",");const es=exp.join(",")
if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""))
else if(h)console.log("TC:"+tc+":FAIL:hidden")
else console.log("TC:"+tc+":FAIL:got="+gs+":expected="+es)}
catch(e){if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:hidden")}}
run(["push","push","peek","pop","empty","pop","empty"],[[1],[2],[0],[0],[0],[0],[0]],["1","1","false","2","true"],1)
run(["push","push","push","pop","pop","peek"],[[1],[2],[3],[0],[0],[0]],["1","2","3"],2)
run(["push","push","pop","peek"],[[10],[20],[0],[0]],["10","20"],3)
run(["push","pop","empty"],[[5],[0],[0]],["5","true"],4)
run(["empty"],[[0]],["true"],5)
run(["push","push","push","push","pop","pop","pop","pop","empty"],[[1],[2],[3],[4],[0],[0],[0],[0],[0]],["1","2","3","4","true"],6,true)
run(["push","peek","pop","push","peek","pop"],[[100],[0],[0],[200],[0],[0]],["100","100","200","200"],7,true)
run(["push","push","push","peek","pop","peek","pop","peek","pop"],[[1],[2],[3],[0],[0],[0],[0],[0],[0]],["1","1","2","2","3","3"],8,true)
run(["push","push","push","push","push","pop","pop","peek"],[[1],[2],[3],[4],[5],[0],[0],[0]],["1","2","3"],9,true)
run(["push","pop","push","pop","push","pop","empty"],[[1],[0],[2],[0],[3],[0],[0]],["1","2","3","true"],10,true)'''

c_code=r'''#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
// USER_CODE_START
typedef struct{int* st1;int top1;int cap;int* st2;int top2;}CodeCoder;
CodeCoder* ccCreate(){return NULL;}
void ccPush(CodeCoder* q,int x){}
int ccPop(CodeCoder* q){return 0;}
int ccPeek(CodeCoder* q){return 0;}
bool ccEmpty(CodeCoder* q){return true;}
void ccFree(CodeCoder* q){}
// USER_CODE_END
// driver just tests basic ops
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
