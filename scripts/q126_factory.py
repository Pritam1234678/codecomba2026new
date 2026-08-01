"""
Factory Pattern
=================
Implement the Factory design pattern for creating shapes.

Requirements:
  - An interface/abstract class Shape with a method double area().
  - Class Circle implements Shape (constructor takes radius): area = pi*r^2
    with pi = 3.141592653589793.
  - Class Rectangle implements Shape (width, height): area = w*h.
  - Class Triangle implements Shape (base, height): area = 0.5*b*h.
  - A ShapeFactory with a static method createShape(type, a, b) that returns:
      - "circle"    -> Circle(a)
      - "rectangle" -> Rectangle(a, b)
      - "triangle"  -> Triangle(a, b)
      - anything else -> null (or None / -1)

Examples:
  ShapeFactory.createShape("circle", 1, 0).area()      -> 3.141592653589793
  ShapeFactory.createShape("rectangle", 3, 4).area()   -> 12.0
  ShapeFactory.createShape("triangle", 6, 4).area()    -> 12.0

The harness calls the factory and checks the returned shape's area().

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Factory Pattern"
desc=(
    "Implement the FACTORY design pattern for creating geometric shapes "
    "without exposing the creation logic.\n\n"
    "Requirements:\n"
    "- An interface/abstract class Shape with a method double area().\n"
    "- Circle(radius): area = pi * radius^2 with pi = 3.141592653589793.\n"
    "- Rectangle(width, height): area = width * height.\n"
    "- Triangle(base, height): area = 0.5 * base * height.\n"
    "- A ShapeFactory with a static createShape(type, a, b) method that "
    "returns:\n"
    "    * \"circle\"    -> a Circle with radius a\n"
    "    * \"rectangle\" -> a Rectangle(a, b)\n"
    "    * \"triangle\"  -> a Triangle(a, b)\n"
    "    * any other type -> null/None (no shape).\n\n"
    "For example:\n"
    "ShapeFactory.createShape(\"circle\", 1, 0).area()    -> 3.141592653589793\n"
    "ShapeFactory.createShape(\"rectangle\", 3, 4).area() -> 12.0\n"
    "ShapeFactory.createShape(\"triangle\", 6, 4).area()  -> 12.0\n\n"
    "This tests centralizing object creation behind a factory method and "
    "returning the correct subclass based on a type string."
)
infmt="No textual input. The harness calls the factory with type strings and parameters, then checks area()."
outfmt="The harness prints PASS/FAIL per test; double comparisons use a 1e-6 tolerance."
cons="No constraints — design-pattern problem.\nUnknown types return null/None."
e1="Input:\n(harness)\ncreateShape(\"circle\",1,0).area()\n\nOutput:\n3.141592653589793"
e2="Input:\n(harness)\ncreateShape(\"rectangle\",3,4).area()\n\nOutput:\n12.0"
e3="Input:\n(harness)\ncreateShape(\"triangle\",6,4).area()\n\nOutput:\n12.0"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,8.0,512,"HARD",True,"OOPS, Design Patterns, Factory",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
interface Shape {
    double area();
}
class Circle implements Shape {
    // Write your code here — constructor(radius), area()
}
class Rectangle implements Shape {
    // Write your code here — constructor(width,height), area()
}
class Triangle implements Shape {
    // Write your code here — constructor(base,height), area()
}
class ShapeFactory {
    public static Shape createShape(String type, double a, double b) {
        // Write your code here — return the right shape or null
        return null;
    }
}
class CodeCoder {
    public double run(String type, double a, double b) {
        Shape s = ShapeFactory.createShape(type, a, b);
        if (s == null) return -1;
        return s.area();
    }
}
// USER_CODE_END

public class Main {
static void test(String t,double a,double b,double e,int tc,boolean hd){double g=new CodeCoder().run(t,a,b);boolean ok=Math.abs(g-e)<=1e-6;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:s="+t+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test("circle",1,0,3.141592653589793,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("rectangle",3,4,12.0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("triangle",6,4,12.0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("square",5,5,-1,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("circle",0,0,0.0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("circle",10,0,314.1592653589793,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test("rectangle",7,3,21.0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test("triangle",5,8,20.0,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test("circle",0.5,0,0.7853981633974483,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test("triangle",2,3,3.0,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Shape{public:virtual double area()=0;virtual ~Shape(){}};
class Circle:public Shape{
    // Write your code here — constructor(radius), area()
};
class Rectangle:public Shape{
    // Write your code here — constructor(width,height), area()
};
class Triangle:public Shape{
    // Write your code here — constructor(base,height), area()
};
class ShapeFactory{public:static Shape* createShape(string type,double a,double b){
    // Write your code here — return the right shape or NULL
    return NULL;
}};
class CodeCoder{public:double run(string t,double a,double b){Shape* s=ShapeFactory::createShape(t,a,b);if(!s)return -1;double r=s->area();delete s;return r;}};
// USER_CODE_END
void test(string t,double a,double b,double e,int tc,bool hd=false){double g=CodeCoder().run(t,a,b);bool ok=fabs(g-e)<=1e-6;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:s="<<t<<":exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("circle",1,0,3.141592653589793,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("rectangle",3,4,12.0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("triangle",6,4,12.0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("square",5,5,-1,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("circle",0,0,0.0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("circle",10,0,314.1592653589793,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test("rectangle",7,3,21.0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test("triangle",5,8,20.0,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test("circle",0.5,0,0.7853981633974483,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test("triangle",2,3,3.0,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
import math

class Shape:
    def area(self):
        raise NotImplementedError

class Circle(Shape):
    # Write your code here — __init__(radius), area()
    pass
class Rectangle(Shape):
    # Write your code here — __init__(width,height), area()
    pass
class Triangle(Shape):
    # Write your code here — __init__(base,height), area()
    pass

class ShapeFactory:
    @staticmethod
    def createShape(type_str, a, b=0):
        # Write your code here — return the right shape or None
        return None

class CodeCoder:
    def run(self, t, a, b):
        s = ShapeFactory.createShape(t, a, b)
        if s is None:
            return -1
        return s.area()
# USER_CODE_END
def test(t,a,b,e,tc,h=False):
    try:
        g=CodeCoder().run(t,a,b);ok=(abs(g-e)<=1e-6)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if h else "") if ok else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:s={t}:exp={e}:got={g}"))
test("circle",1,0,3.141592653589793,1)
test("rectangle",3,4,12.0,2)
test("triangle",6,4,12.0,3)
test("square",5,5,-1,4)
test("circle",0,0,0.0,5)
test("circle",10,0,314.1592653589793,6,True)
test("rectangle",7,3,21.0,7,True)
test("triangle",5,8,20.0,8,True)
test("circle",0.5,0,0.7853981633974483,9,True)
test("triangle",2,3,3.0,10,True)'''

js_code='''// USER_CODE_START
class Shape { area() { throw new Error("abstract"); } }
class Circle extends Shape {
    // Write your code here — constructor(radius), area()
}
class Rectangle extends Shape {
    // Write your code here — constructor(width,height), area()
}
class Triangle extends Shape {
    // Write your code here — constructor(base,height), area()
}
const ShapeFactory = {
    createShape(type, a, b) {
        // Write your code here — return the right shape or null
        return null;
    }
};
function run(t, a, b) {
    const s = ShapeFactory.createShape(t, a, b);
    if (s === null) return -1;
    return s.area();
}
// USER_CODE_END
function test(t,a,b,e,tc,h){if(h===undefined)h=false;let g,ok=false;try{g=run(t,a,b);ok=(Math.abs(g-e)<=1e-6);}catch(err){g="EXC";}if(ok)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:s="+t+":exp="+e+":got="+g);}
try{test("circle",1,0,3.141592653589793,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("rectangle",3,4,12.0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("triangle",6,4,12.0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("square",5,5,-1,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("circle",0,0,0.0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("circle",10,0,314.1592653589793,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test("rectangle",7,3,21.0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test("triangle",5,8,20.0,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test("circle",0.5,0,0.7853981633974483,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test("triangle",2,3,3.0,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <math.h>

/* C has no classes; simulate the factory with tagged structs + free. */
// USER_CODE_START
typedef struct { int type; double a; double b; } Shape;
Shape* makeCircle(double r){Shape* s=(Shape*)malloc(sizeof(Shape));s->type=0;s->a=r;s->b=0;return s;}
Shape* makeRect(double w,double h){Shape* s=(Shape*)malloc(sizeof(Shape));s->type=1;s->a=w;s->b=h;return s;}
Shape* makeTri(double base,double h){Shape* s=(Shape*)malloc(sizeof(Shape));s->type=2;s->a=base;s->b=h;return s;}
double area(Shape* s){
    if(s->type==0)return 3.141592653589793*s->a*s->a;
    if(s->type==1)return s->a*s->b;
    return 0.5*s->a*s->b;
}
Shape* createShape(const char* type,double a,double b){
    // Write your code here — return the right shape or NULL
    return NULL;
}
// USER_CODE_END

void runTest(const char* t,double a,double b,double e,int tc,int hd){
    Shape* s=createShape(t,a,b);
    double g=(s==NULL)?-1:area(s);
    free(s);
    if(fabs(g-e)<=1e-6){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:s=%s:exp=%.6f:got=%.6f\\n",tc,t,e,g);}
}
int main(){
    runTest("circle",1,0,3.141592653589793,1,0);
    runTest("rectangle",3,4,12.0,2,0);
    runTest("triangle",6,4,12.0,3,0);
    runTest("square",5,5,-1,4,0);
    runTest("circle",0,0,0.0,5,0);
    runTest("circle",10,0,314.1592653589793,6,1);
    runTest("rectangle",7,3,21.0,7,1);
    runTest("triangle",5,8,20.0,8,1);
    runTest("circle",0.5,0,0.7853981633974483,9,1);
    runTest("triangle",2,3,3.0,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
