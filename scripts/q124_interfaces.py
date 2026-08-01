"""
Interfaces & Abstract Classes
===============================
Demonstrate interfaces (or abstract classes) by modeling geometric shapes.

Requirements:
  - Define a Shape interface (or abstract class) with two methods:
      double area()
      double perimeter()
  - Implement a class Circle with:
      - constructor Circle(radius)
      - area() = pi * radius^2
      - perimeter() = 2 * pi * radius
    Use pi = 3.141592653589793.
  - Implement a class Rectangle with:
      - constructor Rectangle(width, height)
      - area() = width * height
      - perimeter() = 2 * (width + height)
  - A helper describe(shape) returns the shape's area (as a double), so the
    harness can drive everything through the interface type.

Examples:
  area(Circle(1))    -> 3.141592653589793
  area(Rectangle(3,4)) -> 12.0
  perimeter(Circle(2))  -> 12.566370614359172

The harness checks area and perimeter to within 1e-6.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Interfaces & Abstract Classes"
desc=(
    "Demonstrate INTERFACES and ABSTRACT CLASSES by modeling geometric "
    "shapes.\n\n"
    "Requirements:\n"
    "- Define a Shape interface/abstract class with two methods: double area() "
    "and double perimeter().\n"
    "- Class Circle implements Shape:\n"
    "    - constructor Circle(radius)\n"
    "    - area() = pi * radius^2\n"
    "    - perimeter() = 2 * pi * radius\n"
    "    Use pi = 3.141592653589793.\n"
    "- Class Rectangle implements Shape:\n"
    "    - constructor Rectangle(width, height)\n"
    "    - area() = width * height\n"
    "    - perimeter() = 2 * (width + height)\n"
    "- A helper measure(shape, kind) where kind is \"area\" or \"perimeter\" "
    "returns the corresponding double via the interface type.\n\n"
    "For example:\n"
    "measure(Circle(1), \"area\")         -> 3.141592653589793\n"
    "measure(Rectangle(3,4), \"area\")    -> 12.0\n"
    "measure(Circle(2), \"perimeter\")    -> 12.566370614359172\n\n"
    "This tests defining an abstraction and programming to the interface "
    "(polymorphic dispatch through the base type)."
)
infmt="No textual input. The harness constructs shapes and checks area/perimeter values."
outfmt="The harness prints PASS/FAIL per test; double comparisons use a 1e-6 tolerance."
cons="No constraints — logic-only problem.\nUse pi = 3.141592653589793."
e1="Input:\n(harness)\nmeasure(Circle(1), \"area\")\n\nOutput:\n3.141592653589793"
e2="Input:\n(harness)\nmeasure(Rectangle(3,4), \"area\")\n\nOutput:\n12.0"
e3="Input:\n(harness)\nmeasure(Circle(2), \"perimeter\")\n\nOutput:\n12.566370614359172"

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"OOPS, Interfaces, Abstract Classes",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
interface Shape {
    double area();
    double perimeter();
}
class Circle implements Shape {
    // Write your code here — constructor(radius), area(), perimeter()
}
class Rectangle implements Shape {
    // Write your code here — constructor(width,height), area(), perimeter()
}
class CodeCoder {
    public double measure(Shape s, String kind) {
        if (kind.equals("perimeter")) return s.perimeter();
        return s.area();
    }
}
// USER_CODE_END

public class Main {
static void test(int shape,double r,double w,double h,String kind,double e,int tc,boolean hd){Shape s=(shape==0)?new Circle(r):new Rectangle(w,h);double g=new CodeCoder().measure(s,kind);boolean ok=Math.abs(g-e)<=1e-6;if(ok)System.out.println("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:n="+(int)r+":exp="+e+":got="+g);}
public static void main(String[] x){
try{test(0,1,0,0,"area",3.141592653589793,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(1,0,3,4,"area",12.0,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(0,2,0,0,"perimeter",12.566370614359172,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(1,0,5,2,"perimeter",14.0,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(0,0,0,0,"area",0.0,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(0,10,0,0,"area",314.1592653589793,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(1,0,7,3,"area",21.0,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(0,0.5,0,0,"area",0.7853981633974483,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(1,0,1,1,"perimeter",4.0,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(0,3,0,0,"perimeter",18.84955592153876,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Shape {
public:
    virtual double area() = 0;
    virtual double perimeter() = 0;
    virtual ~Shape() {}
};
class Circle : public Shape {
    // Write your code here — constructor(radius), area(), perimeter()
};
class Rectangle : public Shape {
    // Write your code here — constructor(width,height), area(), perimeter()
};
class CodeCoder{public:double measure(Shape* s,string kind){if(kind=="perimeter")return s->perimeter();return s->area();}};
// USER_CODE_END
void test(int shape,double r,double w,double h,string kind,double e,int tc,bool hd=false){Shape* s=(shape==0)?(Shape*)new Circle(r):(Shape*)new Rectangle(w,h);double g=CodeCoder().measure(s,kind);delete s;bool ok=fabs(g-e)<=1e-6;if(ok)cout<<"TC:"<<tc<<":PASS"<<(hd?":hidden":"")<<"\\n";else if(hd)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:exp="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(0,1,0,0,"area",3.141592653589793,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(1,0,3,4,"area",12.0,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(0,2,0,0,"perimeter",12.566370614359172,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,0,5,2,"perimeter",14.0,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(0,0,0,0,"area",0.0,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(0,10,0,0,"area",314.1592653589793,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test(1,0,7,3,"area",21.0,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test(0,0.5,0,0,"area",0.7853981633974483,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test(1,0,1,1,"perimeter",4.0,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test(0,3,0,0,"perimeter",18.84955592153876,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code='''# USER_CODE_START
import math

class Shape:
    # Abstract-ish base: area() and perimeter() raise NotImplementedError
    def area(self):
        raise NotImplementedError
    def perimeter(self):
        raise NotImplementedError

class Circle(Shape):
    # Write your code here — __init__(radius), area(), perimeter()
    pass
class Rectangle(Shape):
    # Write your code here — __init__(width,height), area(), perimeter()
    pass

class CodeCoder:
    def measure(self, shape, kind):
        if kind == "perimeter":
            return shape.perimeter()
        return shape.area()
# USER_CODE_END
def test(shape,r,w,h,kind,e,tc,hid=False):
    try:
        if shape==0: s=Circle(r)
        else: s=Rectangle(w,h)
        g=CodeCoder().measure(s,kind);ok=(abs(g-e)<=1e-6)
    except Exception:
        ok=False; g="EXC"
    print(f"TC:{tc}:PASS"+(":hidden" if hid else "") if ok else (f"TC:{tc}:FAIL:hidden" if hid else f"TC:{tc}:FAIL:exp={e}:got={g}"))
test(0,1,0,0,"area",3.141592653589793,1)
test(1,0,3,4,"area",12.0,2)
test(0,2,0,0,"perimeter",12.566370614359172,3)
test(1,0,5,2,"perimeter",14.0,4)
test(0,0,0,0,"area",0.0,5)
test(0,10,0,0,"area",314.1592653589793,6,True)
test(1,0,7,3,"area",21.0,7,True)
test(0,0.5,0,0,"area",0.7853981633974483,8,True)
test(1,0,1,1,"perimeter",4.0,9,True)
test(0,3,0,0,"perimeter",18.84955592153876,10,True)'''

js_code='''// USER_CODE_START
class Shape {
    area() { throw new Error("abstract"); }
    perimeter() { throw new Error("abstract"); }
}
class Circle extends Shape {
    // Write your code here — constructor(radius), area(), perimeter()
}
class Rectangle extends Shape {
    // Write your code here — constructor(width,height), area(), perimeter()
}
function measure(shape, kind) {
    if (kind === "perimeter") return shape.perimeter();
    return shape.area();
}
// USER_CODE_END
function test(shape,r,w,h,kind,e,tc,hd){if(hd===undefined)hd=false;let g,ok=false;try{const s=(shape===0)?new Circle(r):new Rectangle(w,h);g=measure(s,kind);ok=(Math.abs(g-e)<=1e-6);}catch(err){g="EXC";}if(ok)console.log("TC:"+tc+":PASS"+(hd?":hidden":""));else if(hd)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:exp="+e+":got="+g);}
try{test(0,1,0,0,"area",3.141592653589793,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(1,0,3,4,"area",12.0,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(0,2,0,0,"perimeter",12.566370614359172,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,0,5,2,"perimeter",14.0,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(0,0,0,0,"area",0.0,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(0,10,0,0,"area",314.1592653589793,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(1,0,7,3,"area",21.0,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(0,0.5,0,0,"area",0.7853981633974483,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(1,0,1,1,"perimeter",4.0,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(0,3,0,0,"perimeter",18.84955592153876,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <math.h>

/* C has no interfaces; simulate with structs carrying a type tag. */
// USER_CODE_START
typedef struct { int type; double radius; double w; double h; } Shape;
Shape makeCircle(double r){Shape s;s.type=0;s.radius=r;s.w=s.h=0;return s;}
Shape makeRect(double w,double h){Shape s;s.type=1;s.radius=0;s.w=w;s.h=h;return s;}
double area(Shape s){
    if(s.type==0)return 3.141592653589793*s.radius*s.radius;
    return s.w*s.h;
}
double perimeter(Shape s){
    if(s.type==0)return 2*3.141592653589793*s.radius;
    return 2*(s.w+s.h);
}
// USER_CODE_END

void runTest(int shape,double r,double w,double h,const char* kind,double e,int tc,int hd){
    Shape s=(shape==0)?makeCircle(r):makeRect(w,h);
    double g=(kind[0]=='p')?perimeter(s):area(s);
    if(fabs(g-e)<=1e-6){if(hd)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hd)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:exp=%.6f:got=%.6f\\n",tc,e,g);}
}
int main(){
    runTest(0,1,0,0,"area",3.141592653589793,1,0);
    runTest(1,0,3,4,"area",12.0,2,0);
    runTest(0,2,0,0,"perimeter",12.566370614359172,3,0);
    runTest(1,0,5,2,"perimeter",14.0,4,0);
    runTest(0,0,0,0,"area",0.0,5,0);
    runTest(0,10,0,0,"area",314.1592653589793,6,1);
    runTest(1,0,7,3,"area",21.0,7,1);
    runTest(0,0.5,0,0,"area",0.7853981633974483,8,1);
    runTest(1,0,1,1,"perimeter",4.0,9,1);
    runTest(0,3,0,0,"perimeter",18.84955592153876,10,1);
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
