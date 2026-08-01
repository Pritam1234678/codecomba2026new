"""
Encapsulation & Abstraction
=============================
Demonstrate ENCAPSULATION: the internal balance of a bank account must be
private and only changed through controlled methods.

Requirements:
  - A class BankAccount with a PRIVATE field balance (not directly accessible).
  - Constructor sets the initial balance.
  - getBalance() returns the current balance.
  - setBalance(amount) sets the balance ONLY if amount >= 0; otherwise it
    leaves the balance unchanged. It returns the balance after the attempt.
  - deposit(amount) adds a positive amount to the balance (amount must be > 0;
    otherwise nothing happens) and returns the new balance.

Examples:
  BankAccount(100).setBalance(-50) -> 100  (rejected, unchanged)
  BankAccount(100).setBalance(250) -> 250  (accepted)
  BankAccount(100).deposit(0)      -> 100  (zero/negative rejected)

The harness calls these methods and verifies that invalid updates are rejected.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Encapsulation & Abstraction"
desc=(
    "Demonstrate ENCAPSULATION and ABSTRACTION by building a BankAccount whose "
    "balance is PRIVATE and can only be changed through controlled methods.\n\n"
    "Requirements:\n"
    "- A class BankAccount with a PRIVATE field balance.\n"
    "- Constructor BankAccount(initialBalance).\n"
    "- getBalance() returns the current balance.\n"
    "- setBalance(amount) sets balance = amount ONLY when amount >= 0; "
    "otherwise the balance is unchanged. Returns the balance after the "
    "attempt.\n"
    "- deposit(amount) adds amount to the balance only when amount > 0; "
    "otherwise nothing happens. Returns the new balance.\n\n"
    "For example:\n"
    "BankAccount(100).setBalance(-50) -> 100  (rejected)\n"
    "BankAccount(100).setBalance(250) -> 250  (accepted)\n"
    "BankAccount(100).deposit(0)      -> 100  (rejected)\n\n"
    "This tests hiding internal state behind methods (encapsulation) and "
    "exposing only a simple, safe interface (abstraction)."
)
infmt="No textual input. The harness calls getBalance/setBalance/deposit and checks the values returned."
outfmt="The harness prints PASS/FAIL per test based on the balance values returned."
cons="No constraints — logic-only problem.\nbalance must never be negative."
e1="Input:\n(harness)\nBankAccount(100).setBalance(-50)\n\nOutput:\n100"
e2="Input:\n(harness)\nBankAccount(100).setBalance(250)\n\nOutput:\n250"
e3="Input:\n(harness)\nBankAccount(100).deposit(0)\n\nOutput:\n100"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"OOPS, Encapsulation, Abstraction",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class BankAccount {
    private int balance;   // must stay private
    public BankAccount(int initial) { balance = initial; }
    public int getBalance() { return balance; }
    public int setBalance(int amount) {
        // Write your code here — set only if amount >= 0
        return balance;
    }
    public int deposit(int amount) {
        // Write your code here — add only if amount > 0
        return balance;
    }
}
class CodeCoder {
    public int run(String op, int init, int amt) {
        BankAccount a = new BankAccount(init);
        if (op.equals("set")) return a.setBalance(amt);
        if (op.equals("dep")) return a.deposit(amt);
        return a.getBalance();
    }
}
// USER_CODE_END

public class Main {
static void test(String op,int init,int amt,int e,int tc,boolean hd){int g=new CodeCoder().run(op,init,amt);if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+init+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("set",100,-50,100,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("set",100,250,250,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("dep",100,0,100,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("dep",50,50,100,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("get",77,0,77,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("set",0,-1,0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("dep",100,-20,100,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("set",5,0,0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("dep",0,10,10,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("set",100,100,100,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class BankAccount {
private:
    int balance;   // must stay private
public:
    BankAccount(int initial){ balance=initial; }
    int getBalance(){ return balance; }
    int setBalance(int amount){
        // Write your code here — set only if amount >= 0
        return balance;
    }
    int deposit(int amount){
        // Write your code here — add only if amount > 0
        return balance;
    }
};
class CodeCoder{public:int run(string op,int init,int amt){BankAccount a(init);if(op=="set")return a.setBalance(amt);if(op=="dep")return a.deposit(amt);return a.getBalance();}};
// USER_CODE_END
void test(string op,int init,int amt,int e,int tc,bool hd=false){int g=CodeCoder().run(op,init,amt);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<init<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("set",100,-50,100,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("set",100,250,250,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("dep",100,0,100,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("dep",50,50,100,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("get",77,0,77,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("set",0,-1,0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("dep",100,-20,100,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("set",5,0,0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("dep",0,10,10,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("set",100,100,100,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class BankAccount:
    def __init__(self, initial):
        self.__balance = initial   # private by convention
    def getBalance(self):
        return self.__balance
    def setBalance(self, amount):
        # Write your code here — set only if amount >= 0
        return self.__balance
    def deposit(self, amount):
        # Write your code here — add only if amount > 0
        return self.__balance

class CodeCoder:
    def run(self, op, init, amt):
        a = BankAccount(init)
        if op == "set": return a.setBalance(amt)
        if op == "dep": return a.deposit(amt)
        return a.getBalance()
# USER_CODE_END
def test(op,init,amt,e,tc,h=False):
    try:
        g=CodeCoder().run(op,init,amt);ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={init}:exp={e}:got={g}"))
test("set",100,-50,100,1)
test("set",100,250,250,2)
test("dep",100,0,100,3)
test("dep",50,50,100,4)
test("get",77,0,77,5)
test("set",0,-1,0,6,True)
test("dep",100,-20,100,7,True)
test("set",5,0,0,8,True)
test("dep",0,10,10,9,True)
test("set",100,100,100,10,True)'''

js_code='''// USER_CODE_START
class BankAccount {
    constructor(initial) { this._balance = initial; }  // private by convention
    getBalance() { return this._balance; }
    setBalance(amount) {
        // Write your code here — set only if amount >= 0
        return this._balance;
    }
    deposit(amount) {
        // Write your code here — add only if amount > 0
        return this._balance;
    }
}
function run(op, init, amt) {
    const a = new BankAccount(init);
    if (op === "set") return a.setBalance(amt);
    if (op === "dep") return a.deposit(amt);
    return a.getBalance();
}
// USER_CODE_END
function test(op,init,amt,e,tc,h){if(h===undefined)h=false;let g,r=false;try{g=run(op,init,amt);r=(g===e);}catch(err){g="EXC";r=false;}if(r)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+init+":exp="+e+":got="+g);}
try{test("set",100,-50,100,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("set",100,250,250,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("dep",100,0,100,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("dep",50,50,100,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("get",77,0,77,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("set",0,-1,0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("dep",100,-20,100,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("set",5,0,0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("dep",0,10,10,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("set",100,100,100,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

/* C has no private fields; simulate encapsulation via opaque accessor discipline. */
// USER_CODE_START
typedef struct { int balance; } BankAccount;
BankAccount makeAccount(int initial){ BankAccount a; a.balance=initial; return a; }
int getBalance(BankAccount* a){ return a->balance; }
int setBalance(BankAccount* a, int amount){
    // Write your code here — set only if amount >= 0
    return a->balance;
}
int deposit(BankAccount* a, int amount){
    // Write your code here — add only if amount > 0
    return a->balance;
}
// USER_CODE_END

void runTest(const char* op,int init,int amt,int e,int tc,int hd){
    BankAccount a=makeAccount(init);
    int g;
    if(op[0]=='s')g=setBalance(&a,amt);
    else if(op[0]=='d')g=deposit(&a,amt);
    else g=getBalance(&a);
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%d:exp=%d:got=%d\\n",tc,init,e,g);}
}
int main(){
    runTest("set",100,-50,100,1,0);
    runTest("set",100,250,250,2,0);
    runTest("dep",100,0,100,3,0);
    runTest("dep",50,50,100,4,0);
    runTest("get",77,0,77,5,0);
    runTest("set",0,-1,0,6,1);
    runTest("dep",100,-20,100,7,1);
    runTest("set",5,0,0,8,1);
    runTest("dep",0,10,10,9,1);
    runTest("set",100,100,100,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
