#!/usr/bin/env python3
"""Bulk insert remaining 64 problems (S.No 10-87) directly - no AI calls."""
import psycopg2

conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat", user="postgres", password="postgres")
cur = conn.cursor()

def gen_j(p):
    """Generate Java harness with 6 test cases."""
    s, fn = p['sig'], p['fn']
    ret, pt = s['ret'], s['pt']
    tc = s.get('tc', [])
    hf = []
    for i, t in enumerate(tc):
        tc_num = i+1
        args_str = ", ".join(t['args'])
        exp_str = t['exp_str']
        ht = "true" if t.get('hidden') else "false"
        inp_str = t.get('inp_str', args_str)
        cmp = "g==e" if ret in ("int","long","boolean","String") else "Arrays.equals(g,e)" 
        hf.append(f"try{{test({args_str},{exp_str},{tc_num},{ht});}}catch(Exception e){{System.out.println(\"TC:{tc_num}:FAIL:input={inp_str}:expected={exp_str}:got=ERR\");}}")
    main_tcs = "\n".join(hf)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public {ret} {fn}({pt}) {{ return {s['ret_default']}; }} }}
// USER_CODE_END
public class Main {{
static void test({s.get('tp','int[] n, int e')}, int tc, boolean h){{{s.get('call','int g=new Solution().'+fn+'('+s.get('targs','n')+')')};if(cmp)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+{s.get('inp','Arrays.toString(n)')}+":expected="+e+":got="+g);}}
public static void main(String[] a){{
{main_tcs}
}}}}'''

def gen_cpp(p):
    fn = p['fn']
    return f'''#include <bits/stdc++.h>
using namespace std;
// USER_CODE_START
class Solution {{ public: {p['sig']['ret']} {fn}({p['sig']['cpp_pt']}) {{ return {p['sig']['ret_default']}; }} }};
// USER_CODE_END
{p['sig'].get('cpp_code','')}
int main(){{ return 0; }}'''

# Just do direct SQL approach - minimal but correct harnesses for each problem

# Helper: insert one problem
def ins(title, desc, infmt, outfmt, cons, tl, ml, lv, tp, e1, e2, e3, j, cp, py, js, cc):
    cur.execute("INSERT INTO problems (title,description,input_format,output_format,constraints,time_limit,memory_limit,level,active,topics,example1,example2,example3) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) RETURNING id",
        (title,desc,infmt,outfmt,cons,tl,ml,lv,True,tp,e1,e2,e3))
    pid = cur.fetchone()[0]
    for lang,code in [("JAVA",j),("CPP",cp),("PYTHON",py),("JAVASCRIPT",js),("C",cc)]:
        cur.execute("INSERT INTO code_snippets (problem_id,language,solution_template,created_at,updated_at) VALUES (%s,%s,%s,NOW(),NOW())",(pid,lang,code))
    conn.commit()
    print(f"  {title} (pid={pid})")

# Template builders
def java_tmpl(fn, ret, params, tests):
    """params: [(type, name, val)...], tests: [(args list, expected, hidden)...]"""
    tcs = []
    for i, (args, exp, hidden) in enumerate(tests):
        n = i+1
        args_str = ", ".join(str(a) for a in args)
        h = "true" if hidden else "false"
        inp_str = ":".join(str(a) for a in args)
        tcs.append(f"try{{test({args_str},{exp},{n},{h});}}catch(Exception e){{System.out.println(\"TC:{n}:FAIL:input={inp_str}:expected={exp}:got=ERR\");}}")
    main_tcs = "\n".join(tcs)
    return f'''import java.util.*;
// USER_CODE_START
class Solution {{ public {ret} {fn}({', '.join(f'{t} {n}' for t,n in params)}) {{ return 0; }} }}
// USER_CODE_END
public class Main {{
static void test({', '.join(f'{t} {n}' for t,n in params)}, int e, int tc, boolean h){{int g=new Solution().{fn}({', '.join(n for _,n in params)});if(g==e)System.out.println("TC:"+tc+":PASS"+(h?":hidden":""));else if(h)System.out.println("TC:"+tc+":FAIL:hidden");else System.out.println("TC:"+tc+":FAIL:input="+{params[-1][1] if len(params)<=3 else '"inp"'}+":expected="+e+":got="+g);}}
public static void main(String[] a){{
{main_tcs}
}}}}'''

# Okay, this template approach is getting complex. Let me just write minimal harnesses for each problem.
# Each problem gets: 6 test cases, USER_CODE markers, proper comparison

# Actually, let me just run the working AI model on the VM with a screen session and no timeout.
# That's the most efficient approach.

print("Switching to AI batch generation via screen...")
cur.close()
conn.close()
import subprocess, sys
sys.exit(0)
