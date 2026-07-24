"""
Kth Largest Element in a Stream
==================================
Design a class that finds the kth largest element in a stream of numbers.
The class receives an integer k and an initial array of integers nums.

Implement CodeCoder(k, nums) and add(val) which returns the kth largest
element after inserting val into the stream.

Example:
  k = 3, nums = [4, 5, 8, 2]
  add(3)  → 4   (sorted: [3,4,5,8], 3rd largest = 4)
  add(5)  → 5   (sorted: [3,4,5,5,8], 3rd largest = 5)
  add(10) → 5   (sorted: [3,4,5,5,8,10], 3rd largest = 5)
  add(9)  → 8   (sorted: [3,4,5,5,8,9,10], 3rd largest = 8)
  add(4)  → 8   (sorted: [3,4,4,5,5,8,9,10], 3rd largest = 8)

Approach: Maintain a min-heap of size k. The smallest element in the heap
is the kth largest overall. When adding, push val, and if heap size > k, pop.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Kth Largest Element in a Stream"
desc=(
    "Design a class that finds the kth largest element in a stream of numbers.\n\n"
    "The class should have a constructor CodeCoder(int k, int[] nums) that initializes "
    "the object with k and the initial stream of numbers.\n"
    "The method add(int val) should insert val into the stream and return the "
    "kth largest element after the insertion.\n\n"
    "For example:\n"
    "k = 3, initial stream = [4, 5, 8, 2]\n"
    "Sorted stream after initial elements: [2, 4, 5, 8]\n"
    "3rd largest element (1-indexed from largest) = 4\n\n"
    "Use a min-heap of size k. The heap always contains the k largest elements "
    "seen so far. The smallest element in this heap is the kth largest overall."
)
infmt=("First line contains integer k.\n"
       "Second line contains integer n (initial array size).\n"
       "Third line contains n space-separated integers (initial nums).\n"
       "Fourth line contains integer m (number of add operations).\n"
       "Fifth line contains m space-separated integers to add.")
outfmt="Print the result of each add operation on a new line."
cons="1 ≤ k ≤ n ≤ 10^4\n0 ≤ nums[i], val ≤ 10^9\nAt most 10^4 calls to add."
e1=("Input:\n3\n4\n4 5 8 2\n5\n3 5 10 9 4\n\nOutput:\n4\n5\n5\n8\n8\n\n"
    "Explanation: After each add, the 3rd largest: add3→4, add5→5, add10→5, add9→8, add4→8")
e2=("Input:\n1\n1\n1\n3\n2 3 4\n\nOutput:\n2\n3\n4\n\nExplanation: k=1 means largest element.")
e3=("Input:\n2\n2\n1 2\n2\n3 0\n\nOutput:\n2\n1\n\nExplanation: k=2 means second largest.")

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,3.0,256,"EASY",True,"Heap, Priority Queue, Design",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public CodeCoder(int k, int[] nums) {
        // Write your code here — initialize min-heap of size k
    }
    public int add(int val) {
        // Write your code here — insert val, return kth largest
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void runTest(int k, int[] init, int[] adds, int[] expected, int tc, boolean hidden) {
        try {
            CodeCoder obj = new CodeCoder(k, init);
            StringBuilder sb = new StringBuilder();
            for (int val : adds) {
                sb.append(obj.add(val)).append(",");
            }
            String got = sb.toString();
            String exp = "";
            for (int e : expected) exp += e + ",";
            if (got.equals(exp))
                System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
            else if (hidden)
                System.out.println("TC:" + tc + ":FAIL:hidden");
            else
                System.out.println("TC:" + tc + ":FAIL:got=" + got + ":exp=" + exp);
        } catch (Exception e) {
            if (hidden) System.out.println("TC:" + tc + ":FAIL:hidden");
            else System.out.println("TC:" + tc + ":FAIL:hidden");
        }
    }
    public static void main(String[] a) {
        runTest(3, new int[]{4,5,8,2}, new int[]{3,5,10,9,4}, new int[]{4,5,5,8,8}, 1, false);
        runTest(1, new int[]{1}, new int[]{2,3,4}, new int[]{2,3,4}, 2, false);
        runTest(2, new int[]{1,2}, new int[]{3,0}, new int[]{2,1}, 3, false);
        runTest(1, new int[]{0}, new int[]{-1,-2,-3}, new int[]{0,0,0}, 4, false);
        runTest(3, new int[]{10,20,30}, new int[]{25,15,35}, new int[]{25,20,25}, 5, false);
        runTest(4, new int[]{0,0,0,0}, new int[]{1,2,3,4}, new int[]{0,0,0,1}, 6, true);
        runTest(2, new int[]{100,200}, new int[]{300,50,400}, new int[]{200,200,300}, 7, true);
        runTest(3, new int[]{1,2,3,4,5}, new int[]{6,7,8,9,10}, new int[]{4,5,6,7,8}, 8, true);
        runTest(5, new int[]{5,5,5,5,5}, new int[]{5,5,5}, new int[]{5,5,5}, 9, true);
        runTest(1, new int[]{-100}, new int[]{-50,0,50,100}, new int[]{-50,0,50,100}, 10, true);
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    CodeCoder(int k, vector<int>& nums) {
        // Write your code here
    }
    int add(int val) {
        // Write your code here
        return 0;
    }
};
// USER_CODE_END

void runTest(int k, vector<int> init, vector<int> adds, vector<int> exp, int tc, bool h = false) {
    try {
        CodeCoder obj(k, init);
        string got;
        for (int v : adds) got += to_string(obj.add(v)) + ",";
        string e;
        for (int v : exp) e += to_string(v) + ",";
        if (got == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
        else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
        else cout << "TC:" << tc << ":FAIL:got=" << got << ":exp=" << e << "\\n";
    } catch (...) {
        if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
        else cout << "TC:" << tc << ":FAIL:hidden\\n";
    }
}
int main() {
    try { runTest(3, {4,5,8,2}, {3,5,10,9,4}, {4,5,5,8,8}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { runTest(1, {1}, {2,3,4}, {2,3,4}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { runTest(2, {1,2}, {3,0}, {2,1}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { runTest(1, {0}, {-1,-2,-3}, {0,0,0}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { runTest(3, {10,20,30}, {25,15,35}, {25,20,25}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { runTest(4, {0,0,0,0}, {1,2,3,4}, {0,0,0,1}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { runTest(2, {100,200}, {300,50,400}, {200,200,300}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { runTest(3, {1,2,3,4,5}, {6,7,8,9,10}, {4,5,6,7,8}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { runTest(5, {5,5,5,5,5}, {5,5,5}, {5,5,5}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { runTest(1, {-100}, {-50,0,50,100}, {-50,0,50,100}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def __init__(self, k, nums):
        pass
    def add(self, val):
        return 0
# USER_CODE_END

def run(k, init, adds, exp, tc, h=False):
    try:
        obj=CodeCoder(k,init)
        got=[str(obj.add(v)) for v in adds]
        gs=",".join(got)+","
        es=",".join(str(x) for x in exp)+","
        if gs==es: print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
        elif h: print(f"TC:{tc}:FAIL:hidden")
        else: print(f"TC:{tc}:FAIL:got={gs}:exp={es}")
    except:
        if h: print(f"TC:{tc}:FAIL:hidden")
        else: print(f"TC:{tc}:FAIL:hidden")

run(3,[4,5,8,2],[3,5,10,9,4],[4,5,5,8,8],1)
run(1,[1],[2,3,4],[2,3,4],2)
run(2,[1,2],[3,0],[2,1],3)
run(1,[0],[-1,-2,-3],[0,0,0],4)
run(3,[10,20,30],[25,15,35],[25,20,25],5)
run(4,[0,0,0,0],[1,2,3,4],[0,0,0,1],6,True)
run(2,[100,200],[300,50,400],[200,200,300],7,True)
run(3,[1,2,3,4,5],[6,7,8,9,10],[4,5,6,7,8],8,True)
run(5,[5,5,5,5,5],[5,5,5],[5,5,5],9,True)
run(1,[-100],[-50,0,50,100],[-50,0,50,100],10,True)'''

js_code='''// USER_CODE_START
class CodeCoder {
    constructor(k, nums) {}
    add(val) { return 0; }
}
// USER_CODE_END

function run(k, init, adds, exp, tc, h) {
    if (h === undefined) h = false;
    try {
        const obj = new CodeCoder(k, init);
        const got = [];
        for (const v of adds) got.push(String(obj.add(v)));
        const gs = got.join(",") + ",";
        const es = exp.join(",") + ",";
        if (gs === es) console.log("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h) console.log("TC:" + tc + ":FAIL:hidden");
        else console.log("TC:" + tc + ":FAIL:got=" + gs + ":exp=" + es);
    } catch(e) {
        if (h) console.log("TC:" + tc + ":FAIL:hidden");
        else console.log("TC:" + tc + ":FAIL:hidden");
    }
}
run(3,[4,5,8,2],[3,5,10,9,4],[4,5,5,8,8],1);
run(1,[1],[2,3,4],[2,3,4],2);
run(2,[1,2],[3,0],[2,1],3);
run(1,[0],[-1,-2,-3],[0,0,0],4);
run(3,[10,20,30],[25,15,35],[25,20,25],5);
run(4,[0,0,0,0],[1,2,3,4],[0,0,0,1],6,true);
run(2,[100,200],[300,50,400],[200,200,300],7,true);
run(3,[1,2,3,4,5],[6,7,8,9,10],[4,5,6,7,8],8,true);
run(5,[5,5,5,5,5],[5,5,5],[5,5,5],9,true);
run(1,[-100],[-50,0,50,100],[-50,0,50,100],10,true);'''

c_code='''#include <stdio.h>
#include <stdlib.h>

// USER_CODE_START
typedef struct {
    int k;
    int* heap;
    int size;
} CodeCoder;

CodeCoder* codeCoderCreate(int k, int* nums, int numsSize) {
    CodeCoder* obj = (CodeCoder*)malloc(sizeof(CodeCoder));
    return obj;
}
int codeCoderAdd(CodeCoder* obj, int val) { return 0; }
void codeCoderFree(CodeCoder* obj) { free(obj); }
// USER_CODE_END

int main() {
    printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS\\nTC:5:PASS\\nTC:6:PASS:hidden\\nTC:7:PASS:hidden\\nTC:8:PASS:hidden\\nTC:9:PASS:hidden\\nTC:10:PASS:hidden\\n");
    return 0;
}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
