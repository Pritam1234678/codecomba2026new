"""
Inheritance & Polymorphism
============================
Implement the classic shape/animal hierarchy to demonstrate inheritance and
polymorphism.

Requirements:
  - A base class Animal with a method sound() that returns the string
    "Some sound".
  - A class Dog that INHERITS from Animal and OVERRIDES sound() to return
    "Bark".
  - A class Cat that INHERITS from Animal and OVERRIDES sound() to return
    "Meow".
  - A function/method describe(animal) that takes any Animal and returns
    animal.sound() — this demonstrates polymorphism (runtime dispatch).

Examples:
  describe(Dog())  -> "Bark"
  describe(Cat())  -> "Meow"
  describe(Animal()) -> "Some sound"

The harness builds each object and checks the sound() returned through the
polymorphic describe() call.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Inheritance & Polymorphism"
desc=(
    "Implement a small animal hierarchy to demonstrate INHERITANCE and "
    "POLYMORPHISM.\n\n"
    "Requirements:\n"
    "- Base class Animal with a method sound() returning \"Some sound\".\n"
    "- Class Dog that INHERITS from Animal and OVERRIDES sound() to return "
    "\"Bark\".\n"
    "- Class Cat that INHERITS from Animal and OVERRIDES sound() to return "
    "\"Meow\".\n"
    "- A helper describe(animal) that returns animal.sound() — the same code "
    "works for every subclass, which is polymorphism at work.\n\n"
    "For example:\n"
    "describe(Dog())  -> \"Bark\"\n"
    "describe(Cat())  -> \"Meow\"\n"
    "describe(Animal()) -> \"Some sound\"\n\n"
    "This tests overriding base-class methods and dynamic dispatch through a "
    "base-class reference/parameter."
)
infmt="No textual input. The harness instantiates each class and checks the sound() value via describe()."
outfmt="The harness prints PASS/FAIL per test based on the strings returned."
cons="No constraints — logic-only problem."
e1="Input:\n(harness)\ndescribe(Dog())\n\nOutput:\nBark"
e2="Input:\n(harness)\ndescribe(Cat())\n\nOutput:\nMeow"
e3="Input:\n(harness)\ndescribe(Animal())\n\nOutput:\nSome sound"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"OOPS, Inheritance, Polymorphism",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class Animal {
    public String sound() { return "Some sound"; }
}
class Dog extends Animal {
    // Write your code here — override sound() to return "Bark"
}
class Cat extends Animal {
    // Write your code here — override sound() to return "Meow"
}
class CodeCoder {
    public String describe(Animal a) {
        return a.sound();
    }
}
// USER_CODE_END

public class Main {
static void test(String kind,String e,int tc,boolean hd){Animal a;if(kind.equals("D"))a=new Dog();else if(kind.equals("C"))a=new Cat();else a=new Animal();String g=new CodeCoder().describe(a);if(g.equals(e))System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+kind+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test("D","Bark",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("C","Meow",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("A","Some sound",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("D","Bark",4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("C","Meow",5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("A","Some sound",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("D","Bark",7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("C","Meow",8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("D","Bark",9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("C","Meow",10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Animal{public:virtual string sound(){return "Some sound";}};
class Dog:public Animal{ /* Write your code here — override sound() -> "Bark" */ };
class Cat:public Animal{ /* Write your code here — override sound() -> "Meow" */ };
class CodeCoder{public:string describe(Animal& a){return a.sound();}};
// USER_CODE_END
void test(string kind,string e,int tc,bool hd=false){Animal* a;if(kind=="D")a=new Dog();else if(kind=="C")a=new Cat();else a=new Animal();string g=CodeCoder().describe(*a);delete a;if(g==e)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:n="<<kind<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("D","Bark",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("C","Meow",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("A","Some sound",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("D","Bark",4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("C","Meow",5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("A","Some sound",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("D","Bark",7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("C","Meow",8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("D","Bark",9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("C","Meow",10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
class Animal:
    def sound(self):
        return "Some sound"
class Dog(Animal):
    # Write your code here — override sound() to return "Bark"
    pass
class Cat(Animal):
    # Write your code here — override sound() to return "Meow"
    pass
class CodeCoder:
    def describe(self, animal):
        return animal.sound()
# USER_CODE_END
def test(kind,e,tc,h=False):
    try:
        if kind=="D": a=Dog()
        elif kind=="C": a=Cat()
        else: a=Animal()
        g=CodeCoder().describe(a);ok=(g==e)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:n={kind}:exp={e}:got={g}"))
test("D","Bark",1)
test("C","Meow",2)
test("A","Some sound",3)
test("D","Bark",4)
test("C","Meow",5)
test("A","Some sound",6,True)
test("D","Bark",7,True)
test("C","Meow",8,True)
test("D","Bark",9,True)
test("C","Meow",10,True)'''

js_code='''// USER_CODE_START
class Animal { sound() { return "Some sound"; } }
class Dog extends Animal {
    // Write your code here — override sound() to return "Bark"
}
class Cat extends Animal {
    // Write your code here — override sound() to return "Meow"
}
function describe(animal) { return animal.sound(); }
// USER_CODE_END
function test(kind,e,tc,h){if(h===undefined)h=false;let g,r=false;try{let a;if(kind==="D")a=new Dog();else if(kind==="C")a=new Cat();else a=new Animal();g=describe(a);r=(g===e);}catch(err){g="EXC";r=false;}if(r)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:n="+kind+":exp="+e+":got="+g);}
try{test("D","Bark",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("C","Meow",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("A","Some sound",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("D","Bark",4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("C","Meow",5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("A","Some sound",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("D","Bark",7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("C","Meow",8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("D","Bark",9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("C","Meow",10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>

/* C has no inheritance; simulate with a struct holding a sound string. */
// USER_CODE_START
typedef struct { char sound[32]; } Animal;
Animal makeAnimal(const char* kind) {
    Animal a;
    if(strcmp(kind,"D")==0) strcpy(a.sound,"Bark");
    else if(strcmp(kind,"C")==0) strcpy(a.sound,"Meow");
    else strcpy(a.sound,"Some sound");
    return a;
}
// USER_CODE_END

void runTest(const char* kind,const char* e,int tc,int hd){
    Animal a=makeAnimal(kind);
    if(strcmp(a.sound,e)==0){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:n=%s:exp=%s:got=%s\\n",tc,kind,e,a.sound);}
}
int main(){
    runTest("D","Bark",1,0);
    runTest("C","Meow",2,0);
    runTest("A","Some sound",3,0);
    runTest("D","Bark",4,0);
    runTest("C","Meow",5,0);
    runTest("A","Some sound",6,1);
    runTest("D","Bark",7,1);
    runTest("C","Meow",8,1);
    runTest("D","Bark",9,1);
    runTest("C","Meow",10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
