import psycopg2
conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

title="Missing Number"
desc="Given an array nums containing n distinct numbers in the range [0, n], return the only number in the range that is missing from the array."
infmt="First line contains integer n.\nSecond line contains n space-separated integers representing nums."
outfmt="Print the missing number."
cons="1 ≤ n ≤ 10^4\n0 ≤ nums[i] ≤ n\nAll elements are distinct."
tl=3.0; ml=256; level="EASY"; topics="Array, Hash Table, Math"
e1="Input:\n3\n3 0 1\n\nOutput:\n2\n\nExplanation: n=3, numbers 0..3, nums has 3,0,1 so 2 is missing."
e2="Input:\n2\n0 1\n\nOutput:\n2\n\nExplanation: n=2, numbers 0..2, nums has 0,1 so 2 is missing."
e3="Input:\n1\n0\n\nOutput:\n1\n\nExplanation: n=1, numbers 0..1, nums has 0 so 1 is missing."

cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
(title,desc,infmt,outfmt,cons,tl,ml,level,True,topics,e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java='''import java.util.*;
// USER_CODE_START
class Solution { public int missingNumber(int[] nums) { return 0; } }
// USER_CODE_END
public class Main {
static void test(int[] n, int e, int t, boolean h){int g=new Solution().missingNumber(n);if(g==e)System.out.println("TC:"+t+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+t+":FAIL:hidden");else System.out.println("TC:"+t+":FAIL:input="+Arrays.toString(n)+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{3,0,1},2,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:input=[3,0,1]:expected=2:got=ERR");}
try{test(new int[]{0,1},2,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:input=[0,1]:expected=2:got=ERR");}
try{test(new int[]{0},1,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:input=[0]:expected=1:got=ERR");}
try{test(new int[]{9,6,4,2,3,5,7,0,1},8,4,true);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1,2,3,4,5,6,7,8,9,0},10,5,true);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{1},0,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
}}'''

cpp='''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution { public: int missingNumber(vector<int>& nums) { return 0; } };
// USER_CODE_END
void test(vector<int> n, int e, int t, bool h=false){int g=Solution().missingNumber(n);if(g==e)cout<<"TC:"<<t<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<t<<":FAIL:hidden\\n";else{cout<<"TC:"<<t<<":FAIL:input=[";for(size_t i=0;i<n.size();i++){if(i)cout<<",";cout<<n[i];}cout<<"]:expected="<<e<<":got="<<g<<"\\n";}}
int main(){
try{test({3,0,1},2,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({0,1},2,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({0},1,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({9,6,4,2,3,5,7,0,1},8,4,true);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1,2,3,4,5,6,7,8,9,0},10,5,true);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({1},0,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
return 0;}'''

py='''# USER_CODE_START
class Solution: 
    def missingNumber(self, nums): return 0
# USER_CODE_END
def test(n,e,t,h=False):g=Solution().missingNumber(n);print(f"TC:{t}:PASS"+((":hidden") if h else "") if g==e else (f"TC:{t}:FAIL:hidden" if h else f"TC:{t}:FAIL:input={n}:expected={e}:got={g}"))
try:test([3,0,1],2,1)
except:print("TC:1:FAIL:hidden")
try:test([0,1],2,2)
except:print("TC:2:FAIL:hidden")
try:test([0],1,3)
except:print("TC:3:FAIL:hidden")
try:test([9,6,4,2,3,5,7,0,1],8,4,True)
except:print("TC:4:FAIL:hidden")
try:test([1,2,3,4,5,6,7,8,9,0],10,5,True)
except:print("TC:5:FAIL:hidden")
try:test([1],0,6,True)
except:print("TC:6:FAIL:hidden")'''

js='''// USER_CODE_START
function missingNumber(nums) { return 0; }
// USER_CODE_END
function test(n,e,t,h){const g=missingNumber(n);if(g===e)console.log(`TC:${t}:PASS`+(h?':hidden':''));else if(h)console.log(`TC:${t}:FAIL:hidden`);else console.log(`TC:${t}:FAIL:input=${JSON.stringify(n)}:expected=${e}:got=${g}`);}
try{test([3,0,1],2,1,false);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([0,1],2,2,false);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([0],1,3,false);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([9,6,4,2,3,5,7,0,1],8,4,true);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1,2,3,4,5,6,7,8,9,0],10,5,true);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([1],0,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}'''

cc='''#include <stdio.h>
// USER_CODE_START
int missingNumber(int* nums, int numsSize) { return 0; }
// USER_CODE_END
void test(int* n, int s, int e, int t, int h){int g=missingNumber(n,s);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",t);else printf("TC:%d:PASS\\n",t);}else{if(h)printf("TC:%d:FAIL:hidden\\n",t);else{printf("TC:%d:FAIL:input=[",t);for(int i=0;i<s;i++){if(i)printf(",");printf("%d",n[i]);}printf("]:expected=%d:got=%d\\n",e,g);}}}
int main(){int t1[]={3,0,1};test(t1,3,2,1,0);int t2[]={0,1};test(t2,2,2,2,0);int t3[]={0};test(t3,1,1,3,0);int t4[]={9,6,4,2,3,5,7,0,1};test(t4,9,8,4,1);int t5[]={1,2,3,4,5,6,7,8,9,0};test(t5,10,10,5,1);int t6[]={1};test(t6,1,0,6,1);return 0;}'''

for lang,code in [("JAVA",java),("CPP",cpp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
    cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
print(f"All 5 snippets inserted for {title} (pid={pid})")
cur.close(); conn.close()
