"""
Coin Change
=============
You are given an integer array coins representing different coin denominations
and an integer amount representing a total amount of money.

Return the fewest number of coins that you need to make up that amount.
If that amount cannot be made up by any combination of the coins, return -1.

You may assume that you have an infinite number of each kind of coin.

Examples:
  coins = [1,2,5], amount = 11 → 3 (5+5+1)
  coins = [2], amount = 3 → -1 (cannot make 3)
  coins = [1], amount = 0 → 0

DP approach: dp[i] = minimum coins needed for amount i.
  For each coin c, dp[i] = min(dp[i], 1 + dp[i-c])

10 test cases — 5 visible, 5 hidden. Class: CodeCoder
"""
import psycopg2
conn=psycopg2.connect(host="localhost",port=5432,dbname="codecombat",user="postgres",password="postgres")
cur=conn.cursor()

title="Coin Change"
desc=(
    "You are given an integer array coins representing coins of different denominations "
    "and an integer amount representing a total amount of money.\n\n"
    "Return the fewest number of coins you need to make up that amount. "
    "If that amount cannot be made up by any combination of the coins, return -1.\n\n"
    "You may assume you have an infinite number of each kind of coin.\n\n"
    "For example:\n"
    "- coins = [1,2,5], amount = 11: 5+5+1 = 11 → 3 coins\n"
    "- coins = [2], amount = 3: impossible → -1\n\n"
    "Use DP: dp[i] = minimum coins for amount i. Initialize dp[0]=0, rest to a large value. "
    "For each amount a from 1 to amount, try every coin c and update dp[a] = min(dp[a], 1 + dp[a-c])."
)
infmt="First line contains integer n.\nSecond line contains n space-separated integers (coins).\nThird line contains integer amount."
outfmt="Print the minimum number of coins needed, or -1 if impossible."
cons="1 \u2264 n \u2264 12\n1 \u2264 coins[i] \u2264 2^31-1\n0 \u2264 amount \u2264 10^4"
e1="Input:\n3\n1 2 5\n11\n\nOutput:\n3\n\nExplanation: 11 = 5+5+1, 3 coins."
e2="Input:\n1\n2\n3\n\nOutput:\n-1\n\nExplanation: Cannot make amount 3 with only coin 2."
e3="Input:\n1\n1\n0\n\nOutput:\n0\n\nExplanation: Zero amount needs zero coins."

cur.execute("""INSERT INTO problems(title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id""",
(title,desc,infmt,outfmt,cons,5.0,256,"MEDIUM",True,"Array, Dynamic Programming, BFS",e1,e2,e3))
pid=cur.fetchone()[0]
print(f"Problem: {title} (pid={pid})")

java_code=r'''import java.util.*;

// USER_CODE_START
class CodeCoder {
    public int coinChange(int[] coins, int amount) {
        // Write your code here — DP
        return -1;
    }
}
// USER_CODE_END
public class Main {
static void test(int[] c,int a,int e,int tc,boolean h){int g=new CodeCoder().coinChange(c,a);if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:coins="+Arrays.toString(c)+":amount="+a+":expected="+e+":got="+g);}
public static void main(String[] a){
try{test(new int[]{1,2,5},11,3,1,false);}catch(Exception e){System.out.println("TC:1:FAIL:hidden");}
try{test(new int[]{2},3,-1,2,false);}catch(Exception e){System.out.println("TC:2:FAIL:hidden");}
try{test(new int[]{1},0,0,3,false);}catch(Exception e){System.out.println("TC:3:FAIL:hidden");}
try{test(new int[]{1,5,10,25},30,2,4,false);}catch(Exception e){System.out.println("TC:4:FAIL:hidden");}
try{test(new int[]{1},100,100,5,false);}catch(Exception e){System.out.println("TC:5:FAIL:hidden");}
try{test(new int[]{2,5,10,20,50},23,4,6,true);}catch(Exception e){System.out.println("TC:6:FAIL:hidden");}
try{test(new int[]{3,7},11,3,7,true);}catch(Exception e){System.out.println("TC:7:FAIL:hidden");}
try{test(new int[]{2,4,6},5,-1,8,true);}catch(Exception e){System.out.println("TC:8:FAIL:hidden");}
try{test(new int[]{1,3,4},6,2,9,true);}catch(Exception e){System.out.println("TC:9:FAIL:hidden");}
try{test(new int[]{1,2,5,10,20,50,100,200},999,18,10,true);}catch(Exception e){System.out.println("TC:10:FAIL:hidden");}
}}'''

cpp_code=r'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class CodeCoder{public:int coinChange(vector<int>& c,int a){return 0;}};
// USER_CODE_END
void test(vector<int> c,int a,int e,int tc,bool h=false){int g=CodeCoder().coinChange(c,a);if(g==e)cout<<"TC:"<<tc<<":PASS"<<(h?":hidden":"")<<"\\n";else if(h)cout<<"TC:"<<tc<<":FAIL:hidden\\n";else cout<<"TC:"<<tc<<":FAIL:expected="<<e<<":got="<<g<<"\\n";}
int main(){
try{test({1,2,5},11,3,1);}catch(...){cout<<"TC:1:FAIL:hidden\\n";}
try{test({2},3,-1,2);}catch(...){cout<<"TC:2:FAIL:hidden\\n";}
try{test({1},0,0,3);}catch(...){cout<<"TC:3:FAIL:hidden\\n";}
try{test({1,5,10,25},30,2,4);}catch(...){cout<<"TC:4:FAIL:hidden\\n";}
try{test({1},100,100,5);}catch(...){cout<<"TC:5:FAIL:hidden\\n";}
try{test({2,5,10,20,50},23,4,6,true);}catch(...){cout<<"TC:6:FAIL:hidden\\n";}
try{test({3,7},11,3,7,true);}catch(...){cout<<"TC:7:FAIL:hidden\\n";}
try{test({2,4,6},5,-1,8,true);}catch(...){cout<<"TC:8:FAIL:hidden\\n";}
try{test({1,3,4},6,2,9,true);}catch(...){cout<<"TC:9:FAIL:hidden\\n";}
try{test({1,2,5,10,20,50,100,200},999,18,10,true);}catch(...){cout<<"TC:10:FAIL:hidden\\n";}
return 0;}'''

py_code=r'''# USER_CODE_START
class CodeCoder:
    def coinChange(self, coins, amount):
        return -1
# USER_CODE_END
def test(c,a,e,tc,h=False):g=CodeCoder().coinChange(c,a);print(f"TC:{tc}:PASS"+(":hidden" if h else "") if g==e else (f"TC:{tc}:FAIL:hidden" if h else f"TC:{tc}:FAIL:coins={c}:amount={a}:expected={e}:got={g}"))
try:test([1,2,5],11,3,1)
except:print("TC:1:FAIL:hidden")
try:test([2],3,-1,2)
except:print("TC:2:FAIL:hidden")
try:test([1],0,0,3)
except:print("TC:3:FAIL:hidden")
try:test([1,5,10,25],30,2,4)
except:print("TC:4:FAIL:hidden")
try:test([1],100,100,5)
except:print("TC:5:FAIL:hidden")
try:test([2,5,10,20,50],23,4,6,hidden=True)
except:print("TC:6:FAIL:hidden")
try:test([3,7],11,3,7,hidden=True)
except:print("TC:7:FAIL:hidden")
try:test([2,4,6],5,-1,8,hidden=True)
except:print("TC:8:FAIL:hidden")
try:test([1,3,4],6,2,9,hidden=True)
except:print("TC:9:FAIL:hidden")
try:test([1,2,5,10,20,50,100,200],999,18,10,hidden=True)
except:print("TC:10:FAIL:hidden")'''

js_code=r'''// USER_CODE_START
function coinChange(coins, amount) { return -1; }
// USER_CODE_END
function test(c,a,e,tc,h){if(h===undefined)h=false;const g=coinChange(c,a);if(g===e)console.log("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)console.log("TC:"+tc+":FAIL:hidden");else console.log("TC:"+tc+":FAIL:coins="+JSON.stringify(c)+":amount="+a+":expected="+e+":got="+g);}
try{test([1,2,5],11,3,1);}catch(e){console.log("TC:1:FAIL:hidden");}
try{test([2],3,-1,2);}catch(e){console.log("TC:2:FAIL:hidden");}
try{test([1],0,0,3);}catch(e){console.log("TC:3:FAIL:hidden");}
try{test([1,5,10,25],30,2,4);}catch(e){console.log("TC:4:FAIL:hidden");}
try{test([1],100,100,5);}catch(e){console.log("TC:5:FAIL:hidden");}
try{test([2,5,10,20,50],23,4,6,true);}catch(e){console.log("TC:6:FAIL:hidden");}
try{test([3,7],11,3,7,true);}catch(e){console.log("TC:7:FAIL:hidden");}
try{test([2,4,6],5,-1,8,true);}catch(e){console.log("TC:8:FAIL:hidden");}
try{test([1,3,4],6,2,9,true);}catch(e){console.log("TC:9:FAIL:hidden");}
try{test([1,2,5,10,20,50,100,200],999,18,10,true);}catch(e){console.log("TC:10:FAIL:hidden");}'''

c_code=r'''#include <stdio.h>
// USER_CODE_START
int coinChange(int* c,int s,int a){return -1;}
// USER_CODE_END
void runTest(int* c,int s,int a,int e,int tc,int h){int g=coinChange(c,s,a);if(g==e){if(h)printf("TC:%d:PASS:hidden\\n",tc);else printf("TC:%d:PASS\\n",tc);}else{if(h)printf("TC:%d:FAIL:hidden\\n",tc);else printf("TC:%d:FAIL:expected=%d:got=%d\\n",tc,e,g);}}
int main(){
int c1[]={1,2,5};runTest(c1,3,11,3,1,0);
int c2[]={2};runTest(c2,1,3,-1,2,0);
int c3[]={1};runTest(c3,1,0,0,3,0);
int c4[]={1,5,10,25};runTest(c4,4,30,2,4,0);
int c5[]={1};runTest(c5,1,100,100,5,0);
int c6[]={2,5,10,20,50};runTest(c6,5,23,4,6,1);
int c7[]={3,7};runTest(c7,2,11,3,7,1);
int c8[]={2,4,6};runTest(c8,3,5,-1,8,1);
int c9[]={1,3,4};runTest(c9,3,6,2,9,1);
int c10[]={1,2,5,10,20,50,100,200};runTest(c10,8,999,18,10,1);
return 0;}'''

for lang,code in [("JAVA",java_code),("CPP",cpp_code),("PYTHON",py_code),("JAVASCRIPT",js_code),("C",c_code)]:
    cur.execute("INSERT INTO code_snippets(problem_id,language,solution_template,created_at,updated_at) VALUES(%s,%s,%s,NOW(),NOW())",(pid,lang,code))
conn.commit()
cur.execute("SELECT language,LENGTH(solution_template) FROM code_snippets WHERE problem_id=%s ORDER BY language",(pid,))
for lang,size in cur.fetchall(): print(f"  {lang}: {size} bytes")
print(f"\n{title} (pid={pid}) — done!")
cur.close(); conn.close()
