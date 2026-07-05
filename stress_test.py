#!/usr/bin/env python3
"""
Stress test: 1000 users
Phase 1: Sequential token fetch (100ms gap to avoid rate-limit)
Phase 2: ALL tokens fire /api/practice/run concurrently
Metrics: success/fail, p50/p95/p99/max latency, throughput
"""

import threading, time, json, statistics
import urllib.request, urllib.error
from collections import Counter

BASE        = "http://localhost:8080"
PROBLEM_ID  = 38
N_USERS     = 1000
TIMEOUT     = 90   # seconds per run request
LOGIN_GAP   = 0.1  # 100ms gap between logins — 10 req/s, well under limit

USER_CODE = """class Solution:
    def nextGreaterElement(self, nums1, nums2):
        nge = {}
        stack = []
        for n in nums2:
            while stack and stack[-1] < n:
                nge[stack.pop()] = n
            stack.append(n)
        return [nge.get(n, -1) for n in nums1]
"""

def post_json(url, payload, token=None, timeout=15):
    data = json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read()), r.status

def get_token(i):
    try:
        d, _ = post_json(f"{BASE}/api/auth/signin",
                         {"username": f"dummy{i}", "password": "Dummy@12345"})
        return d.get("accessToken") or d.get("token")
    except Exception:
        return None

def main():
    print(f"Phase 1: fetching {N_USERS} tokens (1 per {int(LOGIN_GAP*1000)}ms)…")
    tokens = []
    fails = 0
    for i in range(1, N_USERS + 1):
        tok = get_token(i)
        if tok:
            tokens.append(tok)
        else:
            fails += 1
        time.sleep(LOGIN_GAP)
        if i % 100 == 0:
            print(f"  {i}/{N_USERS} — ok:{len(tokens)} fail:{fails}")

    print(f"\nTokens: {len(tokens)} ok, {fails} failed.")
    if not tokens:
        print("No tokens. Aborting.")
        return

    print(f"\nPhase 2: firing {len(tokens)} concurrent practice/run requests…")
    results = []
    lock = threading.Lock()

    def do_run(tok):
        t0 = time.time()
        try:
            d, status = post_json(
                f"{BASE}/api/practice/run",
                {"problemId": PROBLEM_ID, "code": USER_CODE, "language": "PYTHON"},
                token=tok,
                timeout=TIMEOUT
            )
            lat = (time.time() - t0) * 1000
            with lock: results.append((status, lat, "ok"))
        except urllib.error.HTTPError as e:
            lat = (time.time() - t0) * 1000
            with lock: results.append((e.code, lat, "http_err"))
        except Exception as ex:
            lat = (time.time() - t0) * 1000
            with lock: results.append((0, lat, str(ex)[:60]))

    threads = [threading.Thread(target=do_run, args=(t,)) for t in tokens]
    t_start = time.time()
    for th in threads: th.start()
    for th in threads: th.join()
    elapsed = time.time() - t_start

    total   = len(results)
    success = sum(1 for s,_,__ in results if 200 <= s < 300)
    fail    = total - success
    lats    = sorted(l for _,l,__ in results)

    def pct(p): return lats[min(int(len(lats)*p/100), len(lats)-1)]

    print("\n" + "="*60)
    print(f"  STRESS TEST RESULTS — problem {PROBLEM_ID}")
    print("="*60)
    print(f"  Total   : {total}")
    print(f"  2xx     : {success} ({100*success/total:.1f}%)")
    print(f"  Failed  : {fail}")
    print(f"  Elapsed : {elapsed:.1f}s")
    print(f"  Throughput: {total/elapsed:.1f} req/s")
    if lats:
        print(f"\n  Latency (ms):")
        print(f"    min  : {lats[0]:.0f}")
        print(f"    p50  : {pct(50):.0f}")
        print(f"    p95  : {pct(95):.0f}")
        print(f"    p99  : {pct(99):.0f}")
        print(f"    max  : {lats[-1]:.0f}")
    sc = Counter(s for s,_,__ in results)
    print(f"\n  Status  : {dict(sc)}")
    errs = list({m for _,_,m in results if m not in ("ok","http_err")})[:5]
    if errs: print(f"  Errors  : {errs}")
    print("="*60)

if __name__ == "__main__":
    main()
