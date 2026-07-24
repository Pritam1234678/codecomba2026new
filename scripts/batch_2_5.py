import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

def insert_problem(title, desc, infmt, outfmt, constraints, tl, ml, level, topics, ex1, ex2, ex3, java, cpp, py, js, c):
    cur.execute("""
        INSERT INTO problems (title, description, input_format, output_format, constraints,
            time_limit, memory_limit, level, active, topics, example1, example2, example3)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id
    """, (title, desc, infmt, outfmt, constraints, tl, ml, level, True, topics, ex1, ex2, ex3))
    pid = cur.fetchone()[0]
    for lang, code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",c)]:
        cur.execute("INSERT INTO code_snippets (problem_id, language, solution_template, created_at, updated_at) VALUES (%s,%s,%s,NOW(),NOW())", (pid, lang, code))
    conn.commit()
    print(f"  {title} (pid={pid})")

print("Inserting Missing Number (S.No 2)...")
insert_problem(
    title="Missing Number",
    desc="Given an array nums containing n distinct numbers in the range [0, n], return the only number in the range that is missing from the array.",
    infmt="First line contains integer n.\nSecond line contains n space-separated integers representing nums.",
    outfmt="Print the missing number.",
    constraints="1 ≤ n ≤ 10^4\n0 ≤ nums[i] ≤ n\nAll elements are distinct.",
    tl=3.0, ml=256, level="EASY", topics="Array, Hash Table, Math",
    ex1="Input:\n3\n3 0 1\n\nOutput:\n2\n\nExplanation: n=3, numbers 0..3, nums has 3,0,1 so 2 is missing.",
    ex2="Input:\n2\n0 1\n\nOutput:\n2\n\nExplanation: n=2, numbers 0..2, nums has 0,1 so 2 is missing.",
    ex3="Input:\n1\n0\n\nOutput:\n1\n\nExplanation: n=1, numbers 0..1, nums has 0 so 1 is missing.",

    java='''import java.util.*;

// USER_CODE_START
class Solution {
    public int missingNumber(int[] nums) {
        return 0;
    }
}
// USER_CODE_END

public class Main {
    static void test(int[] nums, int expected, int tc, boolean hidden) {
        int got = new Solution().missingNumber(nums);
        if (got == expected)
            System.out.println("TC:" + tc + ":PASS" + (hidden ? ":hidden" : ""));
        else if (hidden)
            System.out.println("TC:" + tc + ":FAIL:hidden");
        else
            System.out.println("TC:" + tc + ":FAIL:input=" + Arrays.toString(nums) + ":expected=" + expected + ":got=" + got);
    }
    public static void main(String[] a) {
        try { test(new int[]{3,0,1}, 2, 1, false); } catch (Exception e) { System.out.println("TC:1:FAIL:input=[3,0,1]:expected=2:got=ERR"); }
        try { test(new int[]{0,1}, 2, 2, false); } catch (Exception e) { System.out.println("TC:2:FAIL:input=[0,1]:expected=2:got=ERR"); }
        try { test(new int[]{0}, 1, 3, false); } catch (Exception e) { System.out.println("TC:3:FAIL:input=[0]:expected=1:got=ERR"); }
        try { test(new int[]{9,6,4,2,3,5,7,0,1}, 8, 4, true); } catch (Exception e) { System.out.println("TC:4:FAIL:hidden"); }
        try { test(new int[]{1,2,3,4,5,6,7,8,9,0}, 10, 5, true); } catch (Exception e) { System.out.println("TC:5:FAIL:hidden"); }
        try { test(new int[]{1}, 0, 6, true); } catch (Exception e) { System.out.println("TC:6:FAIL:hidden"); }
    }''',

    cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {
public:
    int missingNumber(vector<int>& nums) { return 0; }
};
// USER_CODE_END
void test(vector<int> nums, int expected, int tc, bool hidden=false) {
    int got = Solution().missingNumber(nums);
    if(got==expected) cout<<"TC:"<<tc<<":PASS"<<(hidden?":hidden":"")<<"\\n";
    else if(hidden) cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else{ cout<<"TC:"<<tc<<":FAIL:input=["; for(size_t i=0;i<nums.size();i++){if(i)cout<<",";cout<<nums[i];} cout<<"]:expected="<<expected<<":got="<<got<<"\\n";}
}
int main(){
try{test({3,0,1},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,1},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({9,6,4,2,3,5,7,0,1},8,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,0},10,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',

    py='''# USER_CODE_START
class Solution:
    def missingNumber(self, nums):
        return 0
# USER_CODE_END
def test(nums, expected, tc, hidden=False):
    got = Solution().missingNumber(nums)
    if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:input={nums}:expected={expected}:got={got}")
try: test([3,0,1], 2, 1)
except: print("TC:1:FAIL:hidden")
try: test([0,1], 2, 2)
except: print("TC:2:FAIL:hidden")
try: test([0], 1, 3)
except: print("TC:3:FAIL:hidden")
try: test([9,6,4,2,3,5,7,0,1], 8, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test([1,2,3,4,5,6,7,8,9,0], 10, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test([1], 0, 6, hidden=True)
except: print("TC:6:FAIL:hidden")''',

    js='''// USER_CODE_START
function missingNumber(nums) { return 0; }
// USER_CODE_END
function test(nums, expected, tc, hidden=false) {
    const got = missingNumber(nums);
    if(got===expected) console.log(`TC:${tc}:PASS`+(hidden?':hidden':''));
    else if(hidden) console.log(`TC:${tc}:FAIL:hidden`);
    else console.log(`TC:${tc}:FAIL:input=${JSON.stringify(nums)}:expected=${expected}:got=${got}`);
}
try{test([3,0,1],2,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,1],2,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0],1,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([9,6,4,2,3,5,7,0,1],8,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,0],10,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',

    c='''#include <stdio.h>
// USER_CODE_START
int missingNumber(int* nums, int numsSize) { return 0; }
// USER_CODE_END
void test(int* nums, int n, int expected, int tc, int hidden) {
    int got = missingNumber(nums, n);
    if(got==expected) { if(hidden) printf("TC:%d:PASS:hidden\\n",tc); else printf("TC:%d:PASS\\n",tc); }
    else { if(hidden) printf("TC:%d:FAIL:hidden\\n",tc); else { printf("TC:%d:FAIL:input=[",tc); for(int i=0;i<n;i++){if(i)printf(",");printf("%d",nums[i]);} printf("]:expected=%d:got=%d\\n",expected,got); } }
}
int main(){int t1[]={3,0,1};test(t1,3,2,1,0); int t2[]={0,1};test(t2,2,2,2,0); int t3[]={0};test(t3,1,1,3,0); int t4[]={9,6,4,2,3,5,7,0,1};test(t4,9,8,4,1); int t5[]={1,2,3,4,5,6,7,8,9,0};test(t5,10,10,5,1); int t6[]={1};test(t6,1,0,6,1); return 0;}'
)

print("Inserting Armstrong Number (S.No 3)...")
insert_problem(
    title="Armstrong Number",
    desc="Given an integer n, check if it is an Armstrong number. An Armstrong number is a number that is equal to the sum of its own digits each raised to the power of the number of digits. For example, 153 = 1^3 + 5^3 + 3^3.",
    infmt="A single line containing integer n.",
    outfmt="Print 'true' if n is an Armstrong number, otherwise 'false'.",
    constraints="1 ≤ n ≤ 10^9",
    tl=3.0, ml=256, level="EASY", topics="Math",
    ex1="Input:\n153\n\nOutput:\ntrue\n\nExplanation: 1^3 + 5^3 + 3^3 = 1 + 125 + 27 = 153",
    ex2="Input:\n123\n\nOutput:\nfalse\n\nExplanation: 1^3 + 2^3 + 3^3 = 1 + 8 + 27 = 36 ≠ 123",
    ex3="Input:\n9474\n\nOutput:\ntrue\n\nExplanation: 9^4 + 4^4 + 7^4 + 4^4 = 6561 + 256 + 2401 + 256 = 9474",

    java='''import java.util.*;
// USER_CODE_START
class Solution {
    public boolean isArmstrong(int n) { return false; }
}
// USER_CODE_END
public class Main {
    static void test(int n, boolean expected, int tc, boolean hidden) {
        boolean got = new Solution().isArmstrong(n);
        if(got==expected) System.out.println("TC:"+tc+":PASS"+(hidden?":hidden":""));
        else if(hidden) System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:input="+n+":expected="+expected+":got="+got);
    }
    public static void main(String[] a){
        try{test(153,true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:input=153:expected=true:got=ERR");}
        try{test(123,false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:input=123:expected=false:got=ERR");}
        try{test(9474,true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:input=9474:expected=true:got=ERR");}
        try{test(1,true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test(370,true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test(100,false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
    }''',

    cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {
public:
    bool isArmstrong(int n) { return false; }
};
// USER_CODE_END
void test(int n, bool expected, int tc, bool hidden=false) {
    bool got = Solution().isArmstrong(n);
    if(got==expected) cout<<"TC:"<<tc<<":PASS"<<(hidden?":hidden":"")<<"\\n";
    else if(hidden) cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:input="<<n<<":expected="<<(expected?"true":"false")<<":got="<<(got?"true":"false")<<"\\n";
}
int main(){
try{test(153,true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test(123,false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(9474,true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test(1,true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test(370,true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test(100,false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',

    py='''# USER_CODE_START
class Solution:
    def isArmstrong(self, n):
        return False
# USER_CODE_END
def test(n, expected, tc, hidden=False):
    got = Solution().isArmstrong(n)
    if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:input={n}:expected={expected}:got={got}")
try: test(153, True, 1)
except: print("TC:1:FAIL:hidden")
try: test(123, False, 2)
except: print("TC:2:FAIL:hidden")
try: test(9474, True, 3)
except: print("TC:3:FAIL:hidden")
try: test(1, True, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test(370, True, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test(100, False, 6, hidden=True)
except: print("TC:6:FAIL:hidden")''',

    js='''// USER_CODE_START
function isArmstrong(n) { return false; }
// USER_CODE_END
function test(n, expected, tc, hidden=false) {
    const got = isArmstrong(n);
    if(got===expected) console.log(`TC:${tc}:PASS`+(hidden?':hidden':''));
    else if(hidden) console.log(`TC:${tc}:FAIL:hidden`);
    else console.log(`TC:${tc}:FAIL:input=${n}:expected=${expected}:got=${got}`);
}
try{test(153,true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test(123,false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(9474,true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test(1,true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test(370,true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test(100,false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',

    c='''#include <stdio.h>
#include <stdbool.h>
// USER_CODE_START
bool isArmstrong(int n) { return false; }
// USER_CODE_END
void test(int n, bool expected, int tc, int hidden) {
    bool got = isArmstrong(n);
    if(got==expected){if(hidden)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hidden)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:input=%d:expected=%s:got=%s\\n",tc,n,expected?"true":"false",got?"true":"false");}
}
int main(){test(153,true,1,0);test(123,false,2,0);test(9474,true,3,0);test(1,true,4,1);test(370,true,5,1);test(100,false,6,1);return 0;}'
)

print("Inserting Valid Anagram (S.No 4)...")
insert_problem(
    title="Valid Anagram",
    desc="Given two strings s and t, return true if t is an anagram of s, and false otherwise. An anagram is a word formed by rearranging the letters of another word, using all the original letters exactly once.",
    infmt="First line contains string s.\nSecond line contains string t.",
    outfmt="Print 'true' if t is an anagram of s, otherwise 'false'.",
    constraints="1 ≤ |s|, |t| ≤ 5 × 10^4\ns and t consist of lowercase English letters only.",
    tl=3.0, ml=256, level="EASY", topics="String, Hash Table",
    ex1="Input:\nanagram\nnagaram\n\nOutput:\ntrue",
    ex2="Input:\nrat\ncar\n\nOutput:\nfalse",
    ex3="Input:\na\na\n\nOutput:\ntrue",

    java='''import java.util.*;
// USER_CODE_START
class Solution {
    public boolean isAnagram(String s, String t) { return false; }
}
// USER_CODE_END
public class Main {
    static void test(String s, String t, boolean expected, int tc, boolean hidden) {
        boolean got = new Solution().isAnagram(s, t);
        if(got==expected) System.out.println("TC:"+tc+":PASS"+(hidden?":hidden":""));
        else if(hidden) System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:input=s="+s+" t="+t+":expected="+expected+":got="+got);
    }
    public static void main(String[] a){
        try{test("anagram","nagaram",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test("rat","car",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test("a","a",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test("","",true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test("abc","cba",true,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
    }''',

    cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {
public:
    bool isAnagram(string s, string t) { return false; }
};
// USER_CODE_END
void test(string s, string t, bool expected, int tc, bool hidden=false) {
    bool got = Solution().isAnagram(s, t);
    if(got==expected) cout<<"TC:"<<tc<<":PASS"<<(hidden?":hidden":"")<<"\\n";
    else if(hidden) cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:input=s="<<s<<" t="<<t<<":expected="<<(expected?"true":"false")<<":got="<<(got?"true":"false")<<"\\n";
}
int main(){
try{test("anagram","nagaram",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("rat","car",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test("a","a",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("","",true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("abc","cba",true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("listen","silent",true,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',

    py='''# USER_CODE_START
class Solution:
    def isAnagram(self, s, t):
        return False
# USER_CODE_END
def test(s, t, expected, tc, hidden=False):
    got = Solution().isAnagram(s, t)
    if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:input=s={s} t={t}:expected={expected}:got={got}")
try: test("anagram","nagaram",True,1)
except: print("TC:1:FAIL:hidden")
try: test("rat","car",False,2)
except: print("TC:2:FAIL:hidden")
try: test("a","a",True,3)
except: print("TC:3:FAIL:hidden")
try: test("","",True,4,hidden=True)
except: print("TC:4:FAIL:hidden")
try: test("abc","cba",True,5,hidden=True)
except: print("TC:5:FAIL:hidden")
try: test("listen","silent",True,6,hidden=True)
except: print("TC:6:FAIL:hidden")''',

    js='''// USER_CODE_START
function isAnagram(s, t) { return false; }
// USER_CODE_END
function test(s, t, expected, tc, hidden=false) {
    const got = isAnagram(s, t);
    if(got===expected) console.log(`TC:${tc}:PASS`+(hidden?':hidden':''));
    else if(hidden) console.log(`TC:${tc}:FAIL:hidden`);
    else console.log(`TC:${tc}:FAIL:input=s=${s} t=${t}:expected=${expected}:got=${got}`);
}
try{test("anagram","nagaram",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("rat","car",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test("a","a",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("","",true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("abc","cba",true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("listen","silent",true,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',

    c='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
bool isAnagram(char* s, char* t) { return false; }
// USER_CODE_END
void test(char* s, char* t, bool expected, int tc, int hidden) {
    bool got = isAnagram(s, t);
    if(got==expected){if(hidden)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hidden)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:input=s=%s t=%s:expected=%s:got=%s\\n",tc,s,t,expected?"true":"false",got?"true":"false");}
}
int main(){test("anagram","nagaram",true,1,0);test("rat","car",false,2,0);test("a","a",true,3,0);test("","",true,4,1);test("abc","cba",true,5,1);test("listen","silent",true,6,1);return 0;}'
)

print("Inserting Valid Palindrome (S.No 5)...")
insert_problem(
    title="Valid Palindrome",
    desc="A phrase is a palindrome if, after converting all uppercase letters to lowercase and removing all non-alphanumeric characters, it reads the same forward and backward. Alphanumeric characters include letters and numbers. Given a string s, return true if it is a palindrome, otherwise false.",
    infmt="Single line containing string s.",
    outfmt="Print 'true' if palindrome, otherwise 'false'.",
    constraints="1 ≤ |s| ≤ 2 × 10^5\ns consists of printable ASCII characters.",
    tl=3.0, ml=256, level="EASY", topics="String, Two Pointers",
    ex1="Input:\nA man, a plan, a canal: Panama\n\nOutput:\ntrue\n\nExplanation: After removing non-alphanumeric and lowercasing: amanaplanacanalpanama is a palindrome.",
    ex2="Input:\nrace a car\n\nOutput:\nfalse\n\nExplanation: After cleaning: raceacar is not a palindrome.",
    ex3="Input:\n \n\nOutput:\ntrue\n\nExplanation: Empty string after cleaning is a palindrome.",

    java='''import java.util.*;
// USER_CODE_START
class Solution {
    public boolean isPalindrome(String s) { return false; }
}
// USER_CODE_END
public class Main {
    static void test(String s, boolean expected, int tc, boolean hidden) {
        boolean got = new Solution().isPalindrome(s);
        if(got==expected) System.out.println("TC:"+tc+":PASS"+(hidden?":hidden":""));
        else if(hidden) System.out.println("TC:"+tc+":FAIL:hidden");
        else System.out.println("TC:"+tc+":FAIL:input="+s+":expected="+expected+":got="+got);
    }
    public static void main(String[] a){
        try{test("A man, a plan, a canal: Panama",true,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
        try{test("race a car",false,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
        try{test(" ",true,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
        try{test("",true,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
        try{test("ab_a",true,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
        try{test("0P",false,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
    }''',

    cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {
public:
    bool isPalindrome(string s) { return false; }
};
// USER_CODE_END
void test(string s, bool expected, int tc, bool hidden=false) {
    bool got = Solution().isPalindrome(s);
    if(got==expected) cout<<"TC:"<<tc<<":PASS"<<(hidden?":hidden":"")<<"\\n";
    else if(hidden) cout<<"TC:"<<tc<<":FAIL:hidden\\n";
    else cout<<"TC:"<<tc<<":FAIL:input="<<s<<":expected="<<(expected?"true":"false")<<":got="<<(got?"true":"false")<<"\\n";
}
int main(){
try{test("A man, a plan, a canal: Panama",true,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test("race a car",false,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test(" ",true,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test("",true,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test("ab_a",true,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test("0P",false,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}''',

    py='''# USER_CODE_START
class Solution:
    def isPalindrome(self, s):
        return False
# USER_CODE_END
def test(s, expected, tc, hidden=False):
    got = Solution().isPalindrome(s)
    if got == expected: print(f"TC:{tc}:PASS" + (":hidden" if hidden else ""))
    elif hidden: print(f"TC:{tc}:FAIL:hidden")
    else: print(f"TC:{tc}:FAIL:input={repr(s)}:expected={expected}:got={got}")
try: test("A man, a plan, a canal: Panama", True, 1)
except: print("TC:1:FAIL:hidden")
try: test("race a car", False, 2)
except: print("TC:2:FAIL:hidden")
try: test(" ", True, 3)
except: print("TC:3:FAIL:hidden")
try: test("", True, 4, hidden=True)
except: print("TC:4:FAIL:hidden")
try: test("ab_a", True, 5, hidden=True)
except: print("TC:5:FAIL:hidden")
try: test("0P", False, 6, hidden=True)
except: print("TC:6:FAIL:hidden")''',

    js='''// USER_CODE_START
function isPalindrome(s) { return false; }
// USER_CODE_END
function test(s, expected, tc, hidden=false) {
    const got = isPalindrome(s);
    if(got===expected) console.log(`TC:${tc}:PASS`+(hidden?':hidden':''));
    else if(hidden) console.log(`TC:${tc}:FAIL:hidden`);
    else console.log(`TC:${tc}:FAIL:input=${JSON.stringify(s)}:expected=${expected}:got=${got}`);
}
try{test("A man, a plan, a canal: Panama",true,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test("race a car",false,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test(" ",true,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test("",true,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test("ab_a",true,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test("0P",false,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}''',

    c='''#include <stdio.h>
#include <stdbool.h>
#include <string.h>
// USER_CODE_START
bool isPalindrome(char* s) { return false; }
// USER_CODE_END
void test(char* s, bool expected, int tc, int hidden) {
    bool got = isPalindrome(s);
    if(got==expected){if(hidden)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}
    else{if(hidden)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:input=%s:expected=%s:got=%s\\n",tc,s,expected?"true":"false",got?"true":"false");}
}
int main(){test("A man, a plan, a canal: Panama",true,1,0);test("race a car",false,2,0);test(" ",true,3,0);test("",true,4,1);test("ab_a",true,5,1);test("0P",false,6,1);return 0;}'
)

print("\nAll 4 problems inserted successfully!")
cur.close()
conn.close()
