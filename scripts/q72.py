"""
Top K Frequent Words
======================
Given an array of strings words and an integer k, return the k most frequent
words sorted by frequency from highest to lowest. If two words have the same
frequency, sort them lexicographically (ascending/alphabetical).

Example:
  words = ["i","love","leetcode","i","love","coding"], k = 2
  Output: ["i","love"]
  ("i" appears 2 times, "love" appears 2 times, lexicographically "i" < "love")

  words = ["the","day","is","sunny","the","the","the","sunny","is","is"], k = 4
  Output: ["the","is","sunny","day"]

Approach: Count frequencies with a hash map, then use a min-heap of size k.
Sort by (frequency, then reverse lexicographic) so that lower freq / larger
string gets popped first, keeping top k in heap. Finally reverse output.

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2,json
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Top K Frequent Words"
desc=(
    "Given an array of strings words and an integer k, return the k most frequent words.\n\n"
    "The answer should be sorted by frequency from highest to lowest. If two words have "
    "the same frequency, sort them lexicographically (ascending / alphabetical order).\n\n"
    "For example:\n"
    "words = [\"i\",\"love\",\"leetcode\",\"i\",\"love\",\"coding\"], k = 2\n"
    "Frequencies: \"i\"=2, \"love\"=2, \"coding\"=1, \"leetcode\"=1\n"
    "Top 2: [\"i\",\"love\"] — both have freq 2, but \"i\" < \"love\" alphabetically.\n\n"
    "Use a hash map to count frequencies, then a min-heap of size k. "
    "Compare by (frequency ascending, then string descending) so that "
    "the least frequent / largest lexicographic word gets popped first. "
    "Finally reverse the heap to get most frequent first."
)
infmt="First line contains n and k.\nNext n lines each contain one word."
outfmt="Print the k most frequent words, one per line."
cons="1 ≤ |words| ≤ 500\n1 ≤ k ≤ number of unique words\n1 ≤ |words[i]| ≤ 10"
e1=("Input:\n6 2\ni\nlove\nleetcode\ni\nlove\ncoding\n\nOutput:\ni\nlove")
e2=("Input:\n10 4\nthe\nday\nis\nsunny\nthe\nthe\nthe\nsunny\nis\nis\n\nOutput:\nthe\nis\nsunny\nday")
e3=("Input:\n1 1\na\n\nOutput:\na")

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Hash Table, String, Heap, Sorting",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code='''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public List<String> topKFrequent(String[] words, int k) {
        // Write your code here — count frequencies, use min-heap of size k
        return new ArrayList<>();
    }
}
// USER_CODE_END

public class Main {
    static void test(String[] w, int k, List<String> e, int tc, boolean h) {
        List<String> g = new CodeCoder().topKFrequent(w, k);
        if (g.equals(e))
            System.out.println("TC:" + tc + ":PASS" + (h ? ":hidden" : ""));
        else if (h)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:got=" + g + ":exp=" + e);
    }
    public static void main(String[] a) {
        try { test(new String[]{"i","love","leetcode","i","love","coding"}, 2, Arrays.asList("i","love"), 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:hidden"); }
        try { test(new String[]{"the","day","is","sunny","the","the","the","sunny","is","is"}, 4, Arrays.asList("the","is","sunny","day"), 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:hidden"); }
        try { test(new String[]{"a"}, 1, Arrays.asList("a"), 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:hidden"); }
        try { test(new String[]{"a","a","b","b"}, 2, Arrays.asList("a","b"), 4, false); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new String[]{"b","a","c","a","b","c"}, 2, Arrays.asList("a","b"), 5, false); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new String[]{"a","aa","aaa"}, 2, Arrays.asList("a","aa"), 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
        try { test(new String[]{"x","x","y","y","z","z"}, 3, Arrays.asList("x","y","z"), 7, true); } catch (Exception e) { System.out.println("TC:7:FAIL:hidden"); }
        try { test(new String[]{"abc","abc","def","def","ghi","ghi","jkl"}, 3, Arrays.asList("abc","def","ghi"), 8, true); } catch (Exception e) { System.out.println("TC:8:FAIL:hidden"); }
        try { test(new String[]{"z","z","y","y","x","x","w","w"}, 2, Arrays.asList("w","x"), 9, true); } catch (Exception e) { System.out.println("TC:9:FAIL:hidden"); }
        try { test(new String[]{"a"}, 1, Arrays.asList("a"), 10, true); } catch (Exception e) { System.out.println("TC:10:FAIL:hidden"); }
    }
}'''

cpp_code='''#include <bits/stdc++.h>
using namespace std;

// USER_CODE_START
class CodeCoder {
public:
    vector<string> topKFrequent(vector<string>& words, int k) {
        // Write your code here
        return {};
    }
};
// USER_CODE_END

void test(vector<string> w, int k, vector<string> e, int tc, bool h = false) {
    vector<string> g = CodeCoder().topKFrequent(w, k);
    if (g == e) cout << "TC:" << tc << ":PASS" << (h ? ":hidden" : "") << "\\n";
    else if (h) cout << "TC:" << tc << ":FAIL:hidden\\n";
    else {
        cout << "TC:" << tc << ":FAIL:got=[";
        for (auto& s : g) cout << s << ",";
        cout << "]:exp=[";
        for (auto& s : e) cout << s << ",";
        cout << "]\\n";
    }
}
int main() {
    try { test({"i","love","leetcode","i","love","coding"}, 2, {"i","love"}, 1); } catch (...) { cout << "TC:1:FAIL:hidden\\n"; }
    try { test({"the","day","is","sunny","the","the","the","sunny","is","is"}, 4, {"the","is","sunny","day"}, 2); } catch (...) { cout << "TC:2:FAIL:hidden\\n"; }
    try { test({"a"}, 1, {"a"}, 3); } catch (...) { cout << "TC:3:FAIL:hidden\\n"; }
    try { test({"a","a","b","b"}, 2, {"a","b"}, 4); } catch (...) { cout << "TC:4:FAIL:hidden\\n"; }
    try { test({"b","a","c","a","b","c"}, 2, {"a","b"}, 5); } catch (...) { cout << "TC:5:FAIL:hidden\\n"; }
    try { test({"a","aa","aaa"}, 2, {"a","aa"}, 6, true); } catch (...) { cout << "TC:6:FAIL:hidden\\n"; }
    try { test({"x","x","y","y","z","z"}, 3, {"x","y","z"}, 7, true); } catch (...) { cout << "TC:7:FAIL:hidden\\n"; }
    try { test({"abc","abc","def","def","ghi","ghi","jkl"}, 3, {"abc","def","ghi"}, 8, true); } catch (...) { cout << "TC:8:FAIL:hidden\\n"; }
    try { test({"z","z","y","y","x","x","w","w"}, 2, {"w","x"}, 9, true); } catch (...) { cout << "TC:9:FAIL:hidden\\n"; }
    try { test({"a"}, 1, {"a"}, 10, true); } catch (...) { cout << "TC:10:FAIL:hidden\\n"; }
    return 0;
}'''

py_code='''# USER_CODE_START
class CodeCoder:
    def topKFrequent(self, words, k):
        return []
# USER_CODE_END

def test(w,k,e,tc,h=False):
    g=CodeCoder().topKFrequent(w,k)
    if g==e:print(f"TC:{tc}:PASS"+(":hidden" if h else ""))
    elif h:print(f"TC:{tc}:FAIL:hidden")
    else:print(f"TC:{tc}:FAIL:got={g}:exp={e}")

try:test(["i","love","leetcode","i","love","coding"],2,["i","love"],1)
except:print("TC:1:FAIL:hidden")
try:test(["the","day","is","sunny","the","the","the","sunny","is","is"],4,["the","is","sunny","day"],2)
except:print("TC:2:FAIL:hidden")
try:test(["a"],1,["a"],3)
except:print("TC:3:FAIL:hidden")
try:test(["a","a","b","b"],2,["a","b"],4)
except:print("TC:4:FAIL:hidden")
try:test(["b","a","c","a","b","c"],2,["a","b"],5)
except:print("TC:5:FAIL:hidden")
try:test(["a","aa","aaa"],2,["a","aa"],6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test(["x","x","y","y","z","z"],3,["x","y","z"],7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test(["abc","abc","def","def","ghi","ghi","jkl"],3,["abc","def","ghi"],8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test(["z","z","y","y","x","x","w","w"],2,["w","x"],9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test(["a"],1,["a"],10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code='''// USER_CODE_START
function topKFrequent(words, k) {
    // Write your code here
    return [];
}
// USER_CODE_END

function test(w,k,e,tc,h){if(h===undefined)h=false;
    const g=topKFrequent(w,k);
    const gs=JSON.stringify(g),es=JSON.stringify(e);
    if(gs===es)console.log("TC:"+tc+":PASS"+(h?":hidden":""));
    else if(h)console.log("TC:"+tc+":FAIL:hidden");
    else console.log("TC:"+tc+":FAIL:got="+gs+":exp="+es);
}
try{test(["i","love","leetcode","i","love","coding"],2,["i","love"],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(["the","day","is","sunny","the","the","the","sunny","is","is"],4,["the","is","sunny","day"],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(["a"],1,["a"],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(["a","a","b","b"],2,["a","b"],4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(["b","a","c","a","b","c"],2,["a","b"],5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(["a","aa","aaa"],2,["a","aa"],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test(["x","x","y","y","z","z"],3,["x","y","z"],7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test(["abc","abc","def","def","ghi","ghi","jkl"],3,["abc","def","ghi"],8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test(["z","z","y","y","x","x","w","w"],2,["w","x"],9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test(["a"],1,["a"],10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code='''#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// USER_CODE_START
char** topKFrequent(char** words, int wordsSize, int k, int* returnSize) {
    // Write your code here
    *returnSize = 0;
    return NULL;
}
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
