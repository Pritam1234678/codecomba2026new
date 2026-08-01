"""
Classes, Objects & Constructors
================================
Create a class BankAccount that models a bank account.
Requirements:
  - A constructor that takes the account holder's name and an initial balance.
  - A method deposit(amount) that adds amount to the balance and returns the
    new balance.
  - A method withdraw(amount) that subtracts amount if the balance is enough;
    otherwise it must NOT change the balance. It returns the new balance.
  - A method getBalance() that returns the current balance.

Examples:
  BankAccount("Alice", 100).deposit(50)  -> 150
  BankAccount("Bob", 100).withdraw(130)  -> 100  (insufficient, unchanged)
  BankAccount("Bob", 100).withdraw(30)   -> 70

The harness creates the object, runs deposit/withdraw calls and checks the
reported balances.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Classes, Objects & Constructors"
desc=(
    "Implement a class BankAccount that models a bank account.\n\n"
    "Requirements:\n"
    "- Constructor BankAccount(name, initialBalance) stores the account holder "
    "name and the starting balance.\n"
    "- deposit(amount): adds amount to the balance and returns the new balance.\n"
    "- withdraw(amount): subtracts amount and returns the new balance, but only "
    "if the balance is at least amount — otherwise the balance stays unchanged "
    "and the current balance is returned.\n"
    "- getBalance(): returns the current balance.\n\n"
    "For example:\n"
    "BankAccount(\"Alice\", 100).deposit(50) -> 150\n"
    "BankAccount(\"Bob\", 100).withdraw(130) -> 100 (insufficient — unchanged)\n"
    "BankAccount(\"Bob\", 100).withdraw(30)  -> 70\n\n"
    "This tests your understanding of classes, constructors (with parameters) "
    "and instance methods with state."
)
infmt="No textual input. The harness runs deposit/withdraw/getBalance calls and checks the returned balances."
outfmt="The harness prints PASS/FAIL per test based on the balance values returned."
cons="No constraints — logic-only problem.\nBalance must never go negative."
e1="Input:\n(harness)\nBankAccount(\"Alice\",100).deposit(50)\n\nOutput:\n150"
e2="Input:\n(harness)\nBankAccount(\"Bob\",100).withdraw(130)\n\nOutput:\n100"
e3="Input:\n(harness)\nBankAccount(\"Bob\",100).withdraw(30)\n\nOutput:\n70"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"OOPS, Classes, Constructors",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class BankAccount {
    // Write your code here — constructor, deposit, withdraw, getBalance
}
class CodeCoder {
    // Helper used by the harness: build an account and run a sequence.
    public int run(String name, int init, String op, int amount) {
        // 'op' is "deposit", "withdraw" or "balance"
        BankAccount acc = new BankAccount(name, init);
        if (op.equals("deposit")) return acc.deposit(amount);
        if (op.equals("withdraw")) return acc.withdraw(amount);
        return acc.getBalance();
    }
}
// USER_CODE_END

public class Main {
static void test(String n,int init,String op,int amt,int e,int tc,boolean hd){int g=new CodeCoder().run(n,init,op,amt);if(g==e)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
public static void main(String[] a){
try{test("Alice",100,"deposit",50,150,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("Bob",100,"withdraw",130,100,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("Bob",100,"withdraw",30,70,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("Carol",0,"balance",0,0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("Dave",500,"deposit",250,750,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("Eve",200,"withdraw",200,0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("Frank",1,"withdraw",2,1,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("Grace",1000,"deposit",0,1000,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("Heidi",50,"deposit",50,100,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("Ivan",0,"withdraw",1,0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class BankAccount {
    // Write your code here — constructor, deposit, withdraw, getBalance
};
class CodeCoder{public:int run(string name,int init,string op,int amt){BankAccount acc(name,init);if(op=="deposit")return acc.deposit(amt);if(op=="withdraw")return acc.withdraw(amt);return acc.getBalance();}};
// USER_CODE_END
void test(string n,int init,string op,int amt,int e,int tc,bool hd=false){int g=CodeCoder().run(n,init,op,amt);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<n<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("Alice",100,"deposit",50,150,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("Bob",100,"withdraw",130,100,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("Bob",100,"withdraw",30,70,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("Carol",0,"balance",0,0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("Dave",500,"deposit",250,750,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("Eve",200,"withdraw",200,0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("Frank",1,"withdraw",2,1,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("Grace",1000,"deposit",0,1000,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("Heidi",50,"deposit",50,100,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("Ivan",0,"withdraw",1,0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class BankAccount:
    # Write your code here — constructor, deposit, withdraw, getBalance
    pass

class CodeCoder:
    def run(self, name, init, op, amount):
        acc = BankAccount(name, init)
        if op == "deposit": return acc.deposit(amount)
        if op == "withdraw": return acc.withdraw(amount)
        return acc.getBalance()
# USER_CODE_END
def test(n,init,op,amt,e,tc,h=False):
    try:
        g=CodeCoder().run(n,init,op,amt)
        ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={n}:exp={e}:got={g}"))
test("Alice",100,"deposit",50,150,1)
test("Bob",100,"withdraw",130,100,2)
test("Bob",100,"withdraw",30,70,3)
test("Carol",0,"balance",0,0,4)
test("Dave",500,"deposit",250,750,5)
test("Eve",200,"withdraw",200,0,6,True)
test("Frank",1,"withdraw",2,1,7,True)
test("Grace",1000,"deposit",0,1000,8,True)
test("Heidi",50,"deposit",50,100,9,True)
test("Ivan",0,"withdraw",1,0,10,True)'''

js_code='''// USER_CODE_START
class BankAccount {
    // Write your code here — constructor, deposit, withdraw, getBalance
}
function run(name, init, op, amount) {
    const acc = new BankAccount(name, init);
    if (op === "deposit") return acc.deposit(amount);
    if (op === "withdraw") return acc.withdraw(amount);
    return acc.getBalance();
}
// USER_CODE_END
function test(n,init,op,amt,e,tc,h){if(h===undefined)h=false;let g,r=false;try{g=run(n,init,op,amt);r=(g===e);}catch(err){g="EXC";r=false;}if(r)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+n+":exp="+e+":got="+g);}
try{test("Alice",100,"deposit",50,150,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("Bob",100,"withdraw",130,100,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("Bob",100,"withdraw",30,70,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("Carol",0,"balance",0,0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("Dave",500,"deposit",250,750,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("Eve",200,"withdraw",200,0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("Frank",1,"withdraw",2,1,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("Grace",1000,"deposit",0,1000,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("Heidi",50,"deposit",50,100,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("Ivan",0,"withdraw",1,0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>

/* C has no classes; simulate with a struct and functions.
   The user must complete the struct + the three functions. */
// USER_CODE_START
typedef struct { int balance; } BankAccount;
BankAccount createAccount(int init) { BankAccount a; a.balance = init; return a; }
int deposit(BankAccount* a, int amt) { a->balance += amt; return a->balance; }
int withdraw(BankAccount* a, int amt) {
    // Write your code here — subtract only if balance >= amt, return new balance
    return a->balance;
}
// USER_CODE_END

void runTest(int init,const char* op,int amt,int e,int tc,int hd){
    BankAccount a=createAccount(init);
    int g;
    if(op[0]=='d')g=deposit(&a,amt);
    else if(op[0]=='w')g=withdraw(&a,amt);
    else g=a.balance;
    if(g==e){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%d:got=%d\\n",tc,e,g);}
}
int main(){
    runTest(100,"deposit",50,150,1,0);
    runTest(100,"withdraw",130,100,2,0);
    runTest(100,"withdraw",30,70,3,0);
    runTest(0,"balance",0,0,4,0);
    runTest(500,"deposit",250,750,5,0);
    runTest(200,"withdraw",200,0,6,1);
    runTest(1,"withdraw",2,1,7,1);
    runTest(1000,"deposit",0,1000,8,1);
    runTest(50,"deposit",50,100,9,1);
    runTest(0,"withdraw",1,0,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
