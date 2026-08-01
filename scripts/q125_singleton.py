"""
Singleton Pattern
===================
Implement the Singleton design pattern: a class Configuration that can only
ever have ONE instance.

Requirements:
  - The constructor must NOT be directly callable by outside code (private
    constructor / raise error if already exists).
  - A static method getInstance() returns the SAME instance every time it is
    called.
  - The instance keeps an integer counter; the method increment() adds 1 to
    the counter and returns the new value.
  - getCounter() returns the current counter value.

The harness calls getInstance() twice, then calls increment() once, and checks
that both references are the same object AND the counter is 1 (proving the
shared state).

Examples:
  a = getInstance(); b = getInstance(); a.increment() -> 1; b.getCounter() -> 1

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
(In C use a static pointer that is lazily initialized.)
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Singleton Pattern"
desc=(
    "Implement the Singleton design pattern with a class Configuration that "
    "can have AT MOST ONE instance in the whole program.\n\n"
    "Requirements:\n"
    "- Outside code cannot create new instances directly (private constructor "
    "in typed languages; raise an error in Python/JS if a second instance is "
    "attempted).\n"
    "- A static getInstance() method returns the SAME instance on every "
    "call.\n"
    "- The instance stores an integer counter (initially 0).\n"
    "- increment() adds 1 to the counter and returns the new value.\n"
    "- getCounter() returns the current counter value.\n\n"
    "The harness fetches the instance twice, calls increment() on the first "
    "reference, and verifies the second reference sees the same counter "
    "(proving both are the same object). For example:\n"
    "a = getInstance(); b = getInstance(); a.increment() -> 1; "
    "b.getCounter() -> 1\n\n"
    "This tests the classic lazy singleton: a static/class-level holder that "
    "is created only on the first getInstance() call."
)
infmt="No textual input. The harness calls getInstance() twice, increment() once, then compares references and the counter."
outfmt="The harness prints PASS/FAIL based on singleton identity and shared counter."
cons="No constraints — design-pattern problem."
e1="Input:\n(harness)\na=Config.getInstance(); b=Config.getInstance()\n\nOutput:\nsame instance (a is b) = true"
e2="Input:\n(harness)\na.increment()\n\nOutput:\n1"
e3="Input:\n(harness)\nb.getCounter()\n\nOutput:\n1"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"OOPS, Design Patterns, Singleton",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class Configuration {
    private static Configuration instance = null;
    private int counter = 0;
    private Configuration() {}   // private constructor
    public static Configuration getInstance() {
        // Write your code here — lazily create and return the single instance
        return instance;
    }
    public int increment() { counter++; return counter; }
    public int getCounter() { return counter; }
}
class CodeCoder {
    public int[] run() {
        Configuration a = Configuration.getInstance();
        Configuration b = Configuration.getInstance();
        int inc = a.increment();
        int ctr = b.getCounter();
        return new int[]{ (a==b)?1:0, inc, ctr };
    }
}
// USER_CODE_END

public class Main {
static void test(int same,int einc,int ector,int tc,boolean hd){int[] r=new CodeCoder().run();boolean ok=(r[0]==1&&r[1]==einc&&r[2]==ector);if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:exp="+einc+":got="+r[1]);}
public static void main(String[] x){
try{test(1,1,1,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(1,1,1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(1,1,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,1,1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(1,1,1,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(1,1,1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1,1,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(1,1,1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(1,1,1,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(1,1,1,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Configuration {
private:
    static Configuration* instance;
    int counter;
    Configuration(){ counter=0; }
public:
    static Configuration* getInstance(){
        // Write your code here — lazily create and return the single instance
        return instance;
    }
    int increment(){ return ++counter; }
    int getCounter(){ return counter; }
};
Configuration* Configuration::instance = NULL;
class CodeCoder{public:vector<int> run(){Configuration* a=Configuration::getInstance();Configuration* b=Configuration::getInstance();int inc=a->increment();int ctr=b->getCounter();vector<int> r;r.push_back(a==b?1:0);r.push_back(inc);r.push_back(ctr);return r;}};
// USER_CODE_END
void test(int einc,int ector,int tc,bool hd=false){vector<int> r=CodeCoder().run();bool ok=(r[0]==1&&r[1]==einc&&r[2]==ector);if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<einc<<":got="<<r[1]<<"\\n";}
int main(){
try{test(1,1,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(1,1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(1,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(1,1,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(1,1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(1,1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(1,1,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(1,1,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class Configuration:
    _instance = None
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance.counter = 0
        return cls._instance
    def increment(self):
        self.counter += 1
        return self.counter
    def getCounter(self):
        return self.counter

class CodeCoder:
    def run(self):
        a = Configuration()
        b = Configuration()
        a.counter = 0            # reset shared state so each test is independent
        inc = a.increment()
        ctr = b.getCounter()
        return [1 if a is b else 0, inc, ctr]
# USER_CODE_END
def test(einc,ector,tc,h=False):
    try:
        r=CodeCoder().run();ok=(r[0]==1 and r[1]==einc and r[2]==ector)
    except Exception:
        ok=False; r=[0,"EXC",0]
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:exp={einc}:got={r[1]}"))
test(1,1,1)
test(1,1,2)
test(1,1,3)
test(1,1,4)
test(1,1,5)
test(1,1,6,True)
test(1,1,7,True)
test(1,1,8,True)
test(1,1,9,True)
test(1,1,10,True)'''

js_code='''// USER_CODE_START
class Configuration {
    constructor() { this.counter = 0; }
    static getInstance() {
        // Write your code here — lazily create and return the single instance
        return null;
    }
    increment() { this.counter++; return this.counter; }
    getCounter() { return this.counter; }
}
function run() {
    const a = Configuration.getInstance();
    const b = Configuration.getInstance();
    a.counter = 0;             // reset shared state so each test is independent
    const inc = a.increment();
    const ctr = b.getCounter();
    return [a===b?1:0, inc, ctr];
}
// USER_CODE_END
function test(einc,ector,tc,h){if(h===undefined)h=false;let r,ok=false;try{r=run();ok=(r[0]===1&&r[1]===einc&&r[2]===ector);}catch(err){r=[0,"EXC",0];}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+einc+":got="+r[1]);}
try{test(1,1,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(1,1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(1,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(1,1,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(1,1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(1,1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(1,1,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(1,1,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

/* C has no classes; simulate a singleton with a static instance pointer. */
// USER_CODE_START
typedef struct { int counter; } Configuration;
static Configuration* config = NULL;
Configuration* getInstance(void){
    // Write your code here — lazily create config once and return it
    return config;
}
int increment(void){ return ++(getInstance()->counter); }
int getCounter(void){ return getInstance()->counter; }
// USER_CODE_END

void runTest(int einc,int ector,int tc,int hd){
    getInstance()->counter = 0;   // reset shared state so each test is independent
    int same=(getInstance()==getInstance())?1:0;
    int inc=increment(); int ctr=getCounter();
    int ok=(same==1&&inc==einc&&ctr==ector);
    if(ok){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,einc,inc);}
}
int main(){
    runTest(1,1,1,0);
    runTest(1,1,2,0);
    runTest(1,1,3,0);
    runTest(1,1,4,0);
    runTest(1,1,5,0);
    runTest(1,1,6,1);
    runTest(1,1,7,1);
    runTest(1,1,8,1);
    runTest(1,1,9,1);
    runTest(1,1,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
