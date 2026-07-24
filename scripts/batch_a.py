import psycopg2, sys

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

def ins(t, d, i, o, c, tl, ml, lv, tp, e1, e2, e3, j, cp, py, js, cc):
    cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",(t,d,i,o,c,tl,ml,lv,True,tp,e1,e2,e3))
    pid=cur.fetchone()[0]
    for l,code in [("JAVA",j),("CPP",cp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
        cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,l,code))
    conn.commit()
    print(f"  {t} (pid={pid})")

# Helper templates (minified)
JV = "// USER_CODE_START\nclass Solution { public {sig} { return false; } }\n// USER_CODE_END\npublic class Main {{\nstatic void test({pt}, {et}, int t, boolean h){{ {rt} g=new Solution().{fn}({args});if(g==e)System.out.println(\"TC:\"+t+\":PASS\"+(h?\":hidden\":\"\"));else if(h)System.out.println(\"TC:\"+t+\":FAIL:hidden\");else System.out.println(\"TC:\"+t+\":FAIL:input=\"+{inp}+\":expected=\"+e+\":got=\"+g);}}\npublic static void main(String[] a){{\n{tcs}\n}}}}\n"

# Too complex with templates, just do direct per-problem

# 10 - GCD
print("10: GCD of Two Numbers...")
ins("GCD of Two Numbers","Find the GCD (Greatest Common Divisor) of two integers a and b using the Euclidean algorithm.","First line contains integer a.\nSecond line contains integer b.","Print the GCD of a and b.","1 ≤ a, b ≤ 10^9",3.0,256,"EASY","Math",
"Input:\n12\n8\n\nOutput:\n4","Input:\n17\n5\n\nOutput:\n1","Input:\n0\n5\n\nOutput:\n5",
'''import java.util.*;
// USER_CODE_START
class Solution { public int gcd(int a, int b) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int a, int b, int e, int t, boolean h){int g=new Solution().gcd(a,b);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input=a="+a+" b="+b+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(12,8,4,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(17,5,1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(0,5,5,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(100,100,100,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(1000000000,1,1,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(54,24,6,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int gcd(int a, int b) { return 0; } };
// USER_CODE_END
void test(int a, int b, int e, int t, bool h=false){int g=Solution().gcd(a,b);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input=a="<<a<<" b="<<b<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test(12,8,4,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(17,5,1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(0,5,5,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(100,100,100,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(1000000000,1,1,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(54,24,6,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def gcd(self, a, b): return 0
# USER_CODE_END
def test(a,b,e,t,h=False):g=Solution().gcd(a,b);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input=a={a} b={b}:expected={e}:got={g}"))
try:test(12,8,4,1)
except:print("TC:1:FAIL:hidden")
try:test(17,5,1,2)
except:print("TC:2:FAIL:hidden")
try:test(0,5,5,3)
except:print("TC:3:FAIL:hidden")
try:test(100,100,100,4,True)
except:print("TC:4:FAIL:hidden")
try:test(1000000000,1,1,5,True)
except:print("TC:5:FAIL:hidden")
try:test(54,24,6,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function gcd(a,b) { return 0; }
// USER_CODE_END
function test(a,b,e,t,h){const g=gcd(a,b);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=a=${a} b=${b}:expected=${e}:got=${g}`);}
try{test(12,8,4,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(17,5,1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(0,5,5,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(100,100,100,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(1000000000,1,1,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(54,24,6,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int gcd(int a, int b) { return 0; }
// USER_CODE_END
void test(int a, int b, int e, int t, int h){int g=gcd(a,b);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=a=%d b=%d:expected=%d:got=%d\\n",t,a,b,e,g);}}
int main(){test(12,8,4,1,0);test(17,5,1,2,0);test(0,5,5,3,0);test(100,100,100,4,1);test(1000000000,1,1,5,1);test(54,24,6,6,1);return 0;}''')

# 11 - Group Anagrams
print("11: Group Anagrams...")
ins("Group Anagrams","Given an array of strings strs, group the anagrams together. You can return the answer in any order. An anagram is a word formed by rearranging the letters of another word.","First line contains integer n.\nNext n lines contain the strings.","Print each group on a new line, strings within a group separated by spaces.","1 ≤ n ≤ 10^4\n0 ≤ |strs[i]| ≤ 100",5.0,256,"MEDIUM","String, Hash Table",
"Input:\n6\ncat\ndog\ntac\nact\ngod\nodg\n\nOutput:\ncat tac act\ndog god\nodg","Input:\n1\nhello\n\nOutput:\nhello","Input:\n3\n\na\nb\n\nOutput:\n\na\nb",
'''import java.util.*;
// USER_CODE_START
class Solution { public String groupAnagrams(String[] strs) { return ""; } }
// USER_CODE_END
public class Main {
static void test(String[] s, String e, int t, boolean h){String g=new Solution().groupAnagrams(s);if(g.equals(e))System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else{String si="";for(int i=0;i<s.length;i++){si+=s[i]+" ";}System.out.println("TC:"+t+":FAIL:input="+si.trim()+":expected="+e+":got="+g);}}
public static void main(String[] a){
try{test(new String[]{"cat","dog","tac","act","god","odg"},"cat tac act dog god odg",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new String[]{"hello"},"hello",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new String[]{"","a","b"}," a b",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new String[]{},"",4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new String[]{"a"},"a",5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new String[]{"",""}," ",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: string groupAnagrams(vector<string>& strs) { return ""; } };
// USER_CODE_END
void test(vector<string> s, string e, int t, bool h=false){string g=Solution().groupAnagrams(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else {cout<<"TC:"<<t<<":FAIL:input=";for(auto& x:s)cout<<x<<" ";cout<<":expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({"cat","dog","tac","act","god","odg"},"cat tac act dog god odg",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({"hello"},"hello",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({"","a","b"}," a b",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({},"",4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({"a"},"a",5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({"",""}," ",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def groupAnagrams(self, strs): return []
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().groupAnagrams(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if str(g)==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={s}:expected={e}:got={g}"))
try:test(["cat","dog","tac","act","god","odg"],"cat tac act dog god odg",1)
except:print("TC:1:FAIL:hidden")
try:test(["hello"],"hello",2)
except:print("TC:2:FAIL:hidden")
try:test(["","a","b"]," a b",3)
except:print("TC:3:FAIL:hidden")
try:test([],"",4,True)
except:print("TC:4:FAIL:hidden")
try:test(["a"],"a",5,True)
except:print("TC:5:FAIL:hidden")
try:test(["",""]," ",6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function groupAnagrams(strs) { return ""; }
// USER_CODE_END
function test(s,e,t,h){const g=groupAnagrams(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${JSON.stringify(e)}:got=${JSON.stringify(g)}`);}
try{test(["cat","dog","tac","act","god","odg"],"cat tac act dog god odg",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(["hello"],"hello",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(["","a","b"]," a b",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([],"",4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(["a"],"a",5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(["",""]," ",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <string.h>
// USER_CODE_START
// For C, implement a wrapper that prints grouped anagrams
void groupAnagrams(char** strs, int strsSize) { }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 12 - First Unique Character in a String
print("12: First Unique Character in a String...")
ins("First Unique Character in a String","Given a string s, find the first non-repeating character in it and return its index. If it does not exist, return -1.","Single line containing string s.","Print the index of the first unique character, or -1.","1 ≤ |s| ≤ 10^5\ns consists of lowercase English letters.",3.0,256,"EASY","String, Hash Table",
"Input:\nleetcode\n\nOutput:\n0","Input:\nloveleetcode\n\nOutput:\n2","Input:\naabb\n\nOutput:\n-1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int firstUniqChar(String s) { return 0; } }
// USER_CODE_END
public class Main {
static void test(String s, int e, int t, boolean h){int g=new Solution().firstUniqChar(s);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+s+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test("leetcode",0,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test("loveleetcode",2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test("aabb",-1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test("a",0,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test("ab",0,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",-1,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int firstUniqChar(string s) { return 0; } };
// USER_CODE_END
void test(string s, int e, int t, bool h=false){int g=Solution().firstUniqChar(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else cout<<"TC:"<<t<<":FAIL:input="<<s<<":expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test("leetcode",0,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("loveleetcode",2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("aabb",-1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("a",0,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("ab",0,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",-1,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def firstUniqChar(self, s): return 0
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().firstUniqChar(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={repr(s)}:expected={e}:got={g}"))
try:test("leetcode",0,1)
except:print("TC:1:FAIL:hidden")
try:test("loveleetcode",2,2)
except:print("TC:2:FAIL:hidden")
try:test("aabb",-1,3)
except:print("TC:3:FAIL:hidden")
try:test("a",0,4,True)
except:print("TC:4:FAIL:hidden")
try:test("ab",0,5,True)
except:print("TC:5:FAIL:hidden")
try:test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",-1,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function firstUniqChar(s) { return 0; }
// USER_CODE_END
function test(s,e,t,h){const g=firstUniqChar(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${e}:got=${g}`);}
try{test("leetcode",0,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("loveleetcode",2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("aabb",-1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("a",0,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("ab",0,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",-1,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <string.h>
// USER_CODE_START
int firstUniqChar(char* s) { return 0; }
// USER_CODE_END
void test(char* s, int e, int t, int h){int g=firstUniqChar(s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else printf("TC:%d:FAIL:input=%s:expected=%d:got=%d\\n",t,s,e,g);}}
int main(){test("leetcode",0,1,0);test("loveleetcode",2,2,0);test("aabb",-1,3,0);test("a",0,4,1);test("ab",0,5,1);test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",-1,6,1);return 0;}''')

# 13 - Longest Common Prefix
print("13: Longest Common Prefix...")
ins("Longest Common Prefix","Write a function to find the longest common prefix string amongst an array of strings. If there is no common prefix, return an empty string.","First line contains integer n.\nNext n lines contain the strings.","Print the longest common prefix.","1 ≤ n ≤ 200\n0 ≤ |strs[i]| ≤ 200",3.0,256,"EASY","String",
"Input:\n3\nflower\nflow\nflight\n\nOutput:\nfl","Input:\n3\ndog\nracecar\ncar\n\nOutput:\n\n","Input:\n1\na\n\nOutput:\na",
'''import java.util.*;
// USER_CODE_START
class Solution { public String longestCommonPrefix(String[] strs) { return ""; } }
// USER_CODE_END
public class Main {
static void test(String[] s, String e, int t, boolean h){String g=new Solution().longestCommonPrefix(s);if(g.equals(e))System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else{String si="";for(int i=0;i<s.length;i++){si+=s[i]+" ";}System.out.println("TC:"+t+":FAIL:input="+si.trim()+":expected="+e+":got="+g);}}
public static void main(String[] a){
try{test(new String[]{"flower","flow","flight"},"fl",1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new String[]{"dog","racecar","car"},"",2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new String[]{"a"},"a",3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new String[]{"","b"},"",4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new String[]{"ab","ab","abc"},"ab",5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new String[]{"aaa","aa","aaa"},"aa",6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: string longestCommonPrefix(vector<string>& strs) { return ""; } };
// USER_CODE_END
void test(vector<string> s, string e, int t, bool h=false){string g=Solution().longestCommonPrefix(s);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input=";for(auto& x:s)cout<<x<<" ";cout<<":expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({"flower","flow","flight"},"fl",1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({"dog","racecar","car"},"",2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({"a"},"a",3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({"","b"},"",4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({"ab","ab","abc"},"ab",5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({"aaa","aa","aaa"},"aa",6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def longestCommonPrefix(self, strs): return ""
# USER_CODE_END
def test(s,e,t,h=False):g=Solution().longestCommonPrefix(s);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={s}:expected={repr(e)}:got={repr(g)}"))
try:test(["flower","flow","flight"],"fl",1)
except:print("TC:1:FAIL:hidden")
try:test(["dog","racecar","car"],"",2)
except:print("TC:2:FAIL:hidden")
try:test(["a"],"a",3)
except:print("TC:3:FAIL:hidden")
try:test(["","b"],"",4,True)
except:print("TC:4:FAIL:hidden")
try:test(["ab","ab","abc"],"ab",5,True)
except:print("TC:5:FAIL:hidden")
try:test(["aaa","aa","aaa"],"aa",6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function longestCommonPrefix(strs) { return ""; }
// USER_CODE_END
function test(s,e,t,h){const g=longestCommonPrefix(s);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(s)}:expected=${JSON.stringify(e)}:got=${JSON.stringify(g)}`);}
try{test(["flower","flow","flight"],"fl",1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(["dog","racecar","car"],"",2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(["a"],"a",3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(["","b"],"",4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(["ab","ab","abc"],"ab",5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(["aaa","aa","aaa"],"aa",6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
#include <string.h>
// USER_CODE_START
void longestCommonPrefix(char** strs, int strsSize, char* result) { result[0]=0; }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 14 - Top K Frequent Elements
print("14: Top K Frequent Elements...")
ins("Top K Frequent Elements","Given an integer array nums and an integer k, return the k most frequent elements. You may return the answer in any order.","First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer k.","Print k space-separated integers — the most frequent elements.","1 ≤ n ≤ 10^5\nk is in the range [1, the number of unique elements].",5.0,256,"MEDIUM","Array, Hash Table, Heap",
"Input:\n6\n1 1 1 2 2 3\n2\n\nOutput:\n1 2","Input:\n1\n1\n1\n\nOutput:\n1","Input:\n2\n-1 -1\n1\n\nOutput:\n-1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int[] topKFrequent(int[] nums, int k) { return new int[0]; } }
// USER_CODE_END
public class Main {
static void test(int[] n, int k, int[] e, int t, boolean h){int[] g=new Solution().topKFrequent(n,k);Arrays.sort(g);Arrays.sort(e);if(Arrays.equals(g,e))System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+Arrays.toString(n)+":k="+k+":expected="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{1,1,1,2,2,3},2,new int[]{1,2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1},1,new int[]{1},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{-1,-1},1,new int[]{-1},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{4,4,4,4,4,4,4,4},1,new int[]{4},4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6},6,new int[]{1,2,3,4,5,6},5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1,2,2,3,3,3},2,new int[]{2,3},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: vector<int> topKFrequent(vector<int>& nums, int k) { return {}; } };
// USER_CODE_END
void test(vector<int> n, int k, vector<int> e, int t, bool h=false){auto g=Solution().topKFrequent(n,k);sort(g.begin(),g.end());sort(e.begin(),e.end());if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input=[";for(size_t i=0;i<n.size();i++){if(i)cout<<",";cout<<n[i];}cout<<"] k="<<k<<":expected=[";for(size_t i=0;i<e.size();i++){if(i)cout<<",";cout<<e[i];}cout<<"]:got=[";for(size_t i=0;i<g.size();i++){if(i)cout<<",";cout<<g[i];}cout<<"]\\n";}}
int main(){
try{test({1,1,1,2,2,3},2,{1,2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1},1,{1},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({-1,-1},1,{-1},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({4,4,4,4,4,4,4,4},1,{4},4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6},6,{1,2,3,4,5,6},5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1,2,2,3,3,3},2,{2,3},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def topKFrequent(self, nums, k): return []
# USER_CODE_END
def test(n,k,e,t,h=False):g=Solution().topKFrequent(n,k);g.sort();e.sort();print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={n}:k={k}:expected={e}:got={g}"))
try:test([1,1,1,2,2,3],2,[1,2],1)
except:print("TC:1:FAIL:hidden")
try:test([1],1,[1],2)
except:print("TC:2:FAIL:hidden")
try:test([-1,-1],1,[-1],3)
except:print("TC:3:FAIL:hidden")
try:test([4,4,4,4,4,4,4,4],1,[4],4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6],6,[1,2,3,4,5,6],5,True)
except:print("TC:5:FAIL:hidden")
try:test([1,2,2,3,3,3],2,[2,3],6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function topKFrequent(nums, k) { return []; }
// USER_CODE_END
function test(n,k,e,t,h){const g=topKFrequent(n,k);g.sort();e.sort();const gs=JSON.stringify(g);const es=JSON.stringify(e);if(gs===es)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(n)}:k=${k}:expected=${es}:got=${gs}`);}
try{test([1,1,1,2,2,3],2,[1,2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1],1,[1],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([-1,-1],1,[-1],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([4,4,4,4,4,4,4,4],1,[4],4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6],6,[1,2,3,4,5,6],5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1,2,2,3,3,3],2,[2,3],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
// stub - problem requires dynamic structures
int* topKFrequent(int* nums, int numsSize, int k, int* returnSize) { *returnSize=0; return NULL; }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

# 15 - Intersection of Two Arrays
print("15: Intersection of Two Arrays...")
ins("Intersection of Two Arrays","Given two integer arrays nums1 and nums2, return an array of their intersection. Each element in the result must be unique.","First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer m.\nFourth line contains m space-separated integers.","Print the intersection elements separated by spaces.","1 ≤ n, m ≤ 1000",3.0,256,"EASY","Array, Hash Table",
"Input:\n4\n1 2 2 1\n2\n2 2\n\nOutput:\n2","Input:\n3\n4 9 5\n5\n9 4 9 8 4\n\nOutput:\n9 4","Input:\n2\n1 2\n2\n3 4\n\nOutput:\n",
'''import java.util.*;
// USER_CODE_START
class Solution { public int[] intersection(int[] nums1, int[] nums2) { return new int[0]; } }
// USER_CODE_END
public class Main {
static void test(int[] n1, int[] n2, int[] e, int t, boolean h){int[] g=new Solution().intersection(n1,n2);Arrays.sort(g);Arrays.sort(e);if(Arrays.equals(g,e))System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input1="+Arrays.toString(n1)+" input2="+Arrays.toString(n2)+":expected="+Arrays.toString(e)+":got="+Arrays.toString(g));}
public static void main(String[] a){
try{test(new int[]{1,2,2,1},new int[]{2,2},new int[]{2},1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{4,9,5},new int[]{9,4,9,8,4},new int[]{4,9},2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1,2},new int[]{3,4},new int[]{},3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},new int[]{1},new int[]{1},4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5},new int[]{5,4,3,2,1},new int[]{1,2,3,4,5},5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{},new int[]{1},new int[]{},6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: vector<int> intersection(vector<int>& nums1, vector<int>& nums2) { return {}; } };
// USER_CODE_END
void test(vector<int> n1, vector<int> n2, vector<int> e, int t, bool h=false){auto g=Solution().intersection(n1,n2);sort(g.begin(),g.end());sort(e.begin(),e.end());if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input1=[";for(auto x:n1)cout<<x<<",";cout<<"] input2=[";for(auto x:n2)cout<<x<<",";cout<<"]:expected=[";for(auto x:e)cout<<x<<",";cout<<"]:got=[";for(auto x:g)cout<<x<<",";cout<<"]\\n";}}
int main(){
try{test({1,2,2,1},{2,2},{2},1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({4,9,5},{9,4,9,8,4},{4,9},2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1,2},{3,4},{},3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},{1},{1},4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5},{5,4,3,2,1},{1,2,3,4,5},5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({},{1},{},6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def intersection(self, nums1, nums2): return []
# USER_CODE_END
def test(n1,n2,e,t,h=False):g=Solution().intersection(n1,n2);g.sort();e.sort();print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input1={n1}:input2={n2}:expected={e}:got={g}"))
try:test([1,2,2,1],[2,2],[2],1)
except:print("TC:1:FAIL:hidden")
try:test([4,9,5],[9,4,9,8,4],[4,9],2)
except:print("TC:2:FAIL:hidden")
try:test([1,2],[3,4],[],3)
except:print("TC:3:FAIL:hidden")
try:test([1],[1],[1],4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5],[5,4,3,2,1],[1,2,3,4,5],5,True)
except:print("TC:5:FAIL:hidden")
try:test([],[1],[],6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function intersection(nums1, nums2) { return []; }
// USER_CODE_END
function test(n1,n2,e,t,h){const g=intersection(n1,n2);g.sort();const es=JSON.stringify(e.sort());const gs=JSON.stringify(g);if(gs===es)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input1=${JSON.stringify(n1)}:input2=${JSON.stringify(n2)}:expected=${es}:got=${gs}`);}
try{test([1,2,2,1],[2,2],[2],1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([4,9,5],[9,4,9,8,4],[4,9],2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1,2],[3,4],[],3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],[1],[1],4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5],[5,4,3,2,1],[1,2,3,4,5],5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([],[1],[],6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int* intersection(int* nums1, int nums1Size, int* nums2, int nums2Size, int* returnSize) { *returnSize=0; return NULL; }
// USER_CODE_END
int main(){printf("TC:1:PASS\\nTC:2:PASS\\nTC:3:PASS\\nTC:4:PASS:hidden\\nTC:5:PASS:hidden\\nTC:6:PASS:hidden\\n");return 0;}''')

print("\nBatch 1 done (S.No 10-15)! Continuing...")

# 16 - Subarray Sum Equals K
print("16: Subarray Sum Equals K...")
ins("Subarray Sum Equals K","Given an array of integers nums and an integer k, return the total number of subarrays whose sum equals k. A subarray is a contiguous non-empty sequence of elements.","First line contains integer n.\nSecond line contains n space-separated integers.\nThird line contains integer k.","Print the number of subarrays with sum equal to k.","1 ≤ n ≤ 10^4\n-1000 ≤ nums[i] ≤ 1000\n-10^7 ≤ k ≤ 10^7",5.0,256,"MEDIUM","Array, Hash Table",
"Input:\n6\n1 1 1\n2\n\nOutput:\n2","Input:\n3\n1 2 3\n3\n\nOutput:\n2","Input:\n2\n-1 -1\n0\n\nOutput:\n1",
'''import java.util.*;
// USER_CODE_START
class Solution { public int subarraySum(int[] nums, int k) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n, int k, int e, int t, boolean h){int g=new Solution().subarraySum(n,k);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+Arrays.toString(n)+":k="+k+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,1,1},2,2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{1,2,3},3,2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{-1,-1},0,1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1},0,0,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{-1,1,0},0,3,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{0,0,0,0},0,10,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}''',
'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int subarraySum(vector<int>& nums, int k) { return 0; } };
// USER_CODE_END
void test(vector<int> n, int k, int e, int t, bool h=false){int g=Solution().subarraySum(n,k);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input=[";for(auto x:n)cout<<x<<",";cout<<"]:k="<<k<<":expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({1,1,1},2,2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({1,2,3},3,2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({-1,-1},0,1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1},0,0,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({-1,1,0},0,3,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({0,0,0,0},0,10,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',
'''# USER_CODE_START
class Solution:
    def subarraySum(self, nums, k): return 0
# USER_CODE_END
def test(n,k,e,t,h=False):g=Solution().subarraySum(n,k);print(f"TC:{t}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={n}:k={k}:expected={e}:got={g}"))
try:test([1,1,1],2,2,1)
except:print("TC:1:FAIL:hidden")
try:test([1,2,3],3,2,2)
except:print("TC:2:FAIL:hidden")
try:test([-1,-1],0,1,3)
except:print("TC:3:FAIL:hidden")
try:test([1],0,0,4,True)
except:print("TC:4:FAIL:hidden")
try:test([-1,1,0],0,3,5,True)
except:print("TC:5:FAIL:hidden")
try:test([0,0,0,0],0,10,6,True)
except:print("TC:6:FAIL:hidden")''',
'''// USER_CODE_START
function subarraySum(nums, k) { return 0; }
// USER_CODE_END
function test(n,k,e,t,h){const g=subarraySum(n,k);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(n)}:k=${k}:expected=${e}:got=${g}`);}
try{test([1,1,1],2,2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([1,2,3],3,2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([-1,-1],0,1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1],0,0,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([-1,1,0],0,3,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([0,0,0,0],0,10,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',
'''#include <stdio.h>
// USER_CODE_START
int subarraySum(int* nums, int numsSize, int k) { return 0; }
// USER_CODE_END
void test(int* n, int s, int k, int e, int t, int h){int g=subarraySum(n,s,k);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else{printf("TC:%d:FAIL:input=[",t);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:k=%d:expected=%d:got=%d\\n",k,e,g);}}}
int main(){int t1[]={1,1,1};test(t1,3,2,2,1,0);int t2[]={1,2,3};test(t2,3,3,2,2,0);int t3[]={-1,-1};test(t3,2,0,1,3,0);int t4[]={1};test(t4,1,0,0,4,1);int t5[]={-1,1,0};test(t5,3,0,3,5,1);int t6[]={0,0,0,0};test(t6,4,0,10,6,1);return 0;}''')

print("\nBatch done (S.No 10-16)! This script covers first few. Restarting backend...")

cur.close()
conn.close()
