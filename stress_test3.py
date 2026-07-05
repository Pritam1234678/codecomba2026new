#!/usr/bin/env python3
"""
stress_test3.py — LB stress test for dual-VM setup.
Mints JWTs locally, hits PUBLIC endpoint (https://api.codecoder.in/api/practice/run)
with configurable concurrency. No DB or VM SSH needed.

Usage:
  python3 stress_test3.py 100   # 100 concurrent users
  python3 stress_test3.py 500
  python3 stress_test3.py 1000
"""

import sys, time, json, hmac, hashlib, base64, uuid, threading
import urllib.request, urllib.error
from collections import Counter

# ── Config ────────────────────────────────────────────────────────────────────
BASE       = "https://api.codecoder.in"
PROBLEM_ID = 38      # Next Greater Element (problem exists in prod)
TIMEOUT    = 120     # per request timeout (s)
N_USERS    = int(sys.argv[1]) if len(sys.argv) > 1 else 200

JWT_SECRET_B64 = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
JWT_SECRET     = base64.b64decode(JWT_SECRET_B64 + "==")

# Python solution for problem 38
USER_CODE = """\
class Solution:
    def nextGreaterElement(self, nums1, nums2):
        nge = {}
        stack = []
        for n in nums2:
            while stack and stack[-1] < n:
                nge[stack.pop()] = n
            stack.append(n)
        return [nge.get(n, -1) for n in nums1]
"""

# ── JWT helper ────────────────────────────────────────────────────────────────
def b64url(b: bytes) -> str:
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

def mint_jwt(username: str) -> str:
    now = int(time.time())
    header  = b64url(json.dumps({"alg":"HS256","typ":"JWT"}).encode())
    payload = b64url(json.dumps({
        "jti": str(uuid.uuid4()),
        "sub": username,
        "iat": now,
        "exp": now + 86400
    }).encode())
    sig_input = f"{header}.{payload}".encode()
    sig = hmac.new(JWT_SECRET, sig_input, hashlib.sha256).digest()
    return f"{header}.{payload}.{b64url(sig)}"

# ── HTTP helper ───────────────────────────────────────────────────────────────
def post_json(url, body_dict, token, timeout):
    data = json.dumps(body_dict).encode()
    req  = urllib.request.Request(url, data=data, method="POST", headers={
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
    })
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read()), r.status

# ── Worker ────────────────────────────────────────────────────────────────────
results = []
lock    = threading.Lock()

def do_run(idx: int):
    username = f"dummy{(idx % 1000) + 1}"   # dummy1..dummy1000 (prod has 1000)
    token    = mint_jwt(username)
    t0       = time.time()
    try:
        d, status = post_json(
            f"{BASE}/api/practice/run",
            {"problemId": PROBLEM_ID, "code": USER_CODE, "language": "PYTHON"},
            token=token, timeout=TIMEOUT
        )
        lat = (time.time() - t0) * 1000
        verdict = d.get("status", "?")
        with lock:
            results.append((status, lat, "ok", verdict))
    except urllib.error.HTTPError as e:
        lat  = (time.time() - t0) * 1000
        body = e.read()[:120].decode(errors="replace")
        with lock:
            results.append((e.code, lat, body[:60], ""))
    except Exception as ex:
        lat = (time.time() - t0) * 1000
        with lock:
            results.append((0, lat, str(ex)[:60], ""))

# ── Main ──────────────────────────────────────────────────────────────────────
def main():
    print(f"\n{'='*62}")
    print(f"  CodeCombat dual-VM stress test — {N_USERS} concurrent users")
    print(f"  Target: {BASE}/api/practice/run  (problem {PROBLEM_ID})")
    print(f"{'='*62}")

    # Sanity check first
    print("  Sanity check (1 request)…")
    try:
        d, s = post_json(
            f"{BASE}/api/practice/run",
            {"problemId": PROBLEM_ID, "code": USER_CODE, "language": "PYTHON"},
            token=mint_jwt("dummy1"), timeout=30
        )
        print(f"  ✓ HTTP {s}  verdict={d.get('status','?')}  "
              f"passed={d.get('passed','?')}/{d.get('total','?')}")
    except Exception as e:
        print(f"  ✗ Sanity FAILED: {e}\n  Aborting.")
        return

    print(f"\n  Launching {N_USERS} threads…")
    threads = [threading.Thread(target=do_run, args=(i,), daemon=True)
               for i in range(N_USERS)]

    t_start = time.time()
    for th in threads: th.start()
    for th in threads: th.join()
    elapsed = time.time() - t_start

    # ── Stats ─────────────────────────────────────────────────────────────────
    total   = len(results)
    success = sum(1 for s,_,__,___ in results if 200 <= s < 300)
    fail    = total - success
    lats    = sorted(l for _,l,__,___ in results)
    verdicts = Counter(v for _,_,__,v in results if v)

    def pct(p):
        return lats[min(int(len(lats) * p / 100), len(lats) - 1)]

    print(f"\n{'='*62}")
    print(f"  RESULTS — {N_USERS} concurrent /practice/run")
    print(f"{'='*62}")
    print(f"  Total       : {total}")
    print(f"  2xx success : {success}  ({100*success//max(total,1)}%)")
    print(f"  Failed      : {fail}")
    print(f"  Elapsed     : {elapsed:.1f}s")
    print(f"  Throughput  : {total/max(elapsed,0.01):.1f} req/s")

    if lats:
        print(f"\n  Latency (ms):")
        print(f"    min  : {lats[0]:.0f}")
        print(f"    p50  : {pct(50):.0f}")
        print(f"    p95  : {pct(95):.0f}")
        print(f"    p99  : {pct(99):.0f}")
        print(f"    max  : {lats[-1]:.0f}")
        print(f"    mean : {sum(lats)/len(lats):.0f}")

    sc = Counter(s for s,_,__,___ in results)
    print(f"\n  HTTP codes  : {dict(sc)}")
    if verdicts:
        print(f"  Verdicts    : {dict(verdicts)}")

    errs = list({m for _,_,m,__ in results if m not in ("ok",) and len(m) > 2})[:5]
    if errs:
        print(f"  Errors      : {errs}")

    # VM distribution hint — ~50/50 round-robin in nginx
    print(f"\n  nginx load split (approx): VM1 ~{success//2} | VM2 ~{success//2}")
    print(f"{'='*62}\n")

if __name__ == "__main__":
    main()
