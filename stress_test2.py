#!/usr/bin/env python3
"""
Stress test v2 — mint 1000 JWTs locally (no login calls, no rate-limit),
then fire all 1000 /api/practice/run concurrently.

Requirements: pip install PyJWT psycopg2-binary
"""

import threading, time, json, uuid
import urllib.request, urllib.error
from collections import Counter
import hmac, hashlib, base64

# ── Config ────────────────────────────────────────────────────────────
BASE        = "http://localhost:8080"
PROBLEM_ID  = 38
N_USERS     = 1000
TIMEOUT     = 120

# Same secret as application — BASE64 encoded (Spring uses BASE64.decode)
JWT_SECRET_B64 = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
# The env value is treated as a raw Base64 string by Spring Decoders.BASE64
import base64 as _b64
JWT_SECRET = _b64.b64decode(JWT_SECRET_B64 + "==")   # pad if needed

# Python solution for problem 38 (Next Greater Element)
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

def b64url(b: bytes) -> str:
    return base64.urlsafe_b64encode(b).rstrip(b'=').decode()

def mint_jwt(username: str, exp_in_secs: int = 86400) -> str:
    """Mint a HS256 JWT matching Spring's JwtUtils format."""
    now = int(time.time())
    header  = b64url(json.dumps({"alg":"HS256","typ":"JWT"}).encode())
    payload = b64url(json.dumps({
        "jti": str(uuid.uuid4()),
        "sub": username,
        "iat": now,
        "exp": now + exp_in_secs
    }).encode())
    signing_input = f"{header}.{payload}".encode()
    sig = hmac.new(JWT_SECRET, signing_input, hashlib.sha256).digest()
    return f"{header}.{payload}.{b64url(sig)}"

def post_json(url, payload, token=None, timeout=60):
    data = json.dumps(payload).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read()), r.status

def main():
    print(f"Minting {N_USERS} JWTs locally (no login calls)…")
    # Fetch usernames from DB to use real subjects
    import psycopg2
    conn = psycopg2.connect(host="localhost", port=5432, dbname="codecombat",
                            user="postgres", password="postgres")
    cur = conn.cursor()
    cur.execute("SELECT username FROM users WHERE username LIKE 'dummy%%' ORDER BY id LIMIT %s", (N_USERS,))
    usernames = [r[0] for r in cur.fetchall()]
    cur.close(); conn.close()
    print(f"Got {len(usernames)} dummy usernames from DB.")

    tokens = [mint_jwt(u) for u in usernames]

    # Quick sanity check — one real request
    print("Sanity check: 1 request…")
    try:
        d, s = post_json(f"{BASE}/api/practice/run",
                         {"problemId": PROBLEM_ID, "code": USER_CODE, "language": "PYTHON"},
                         token=tokens[0], timeout=30)
        print(f"  status={s} verdict={d.get('status','?')}")
    except Exception as e:
        print(f"  sanity FAIL: {e}")
        return

    print(f"\nFiring {len(tokens)} concurrent /practice/run requests…")
    results = []
    lock = threading.Lock()

    def do_run(tok):
        t0 = time.time()
        try:
            d, status = post_json(
                f"{BASE}/api/practice/run",
                {"problemId": PROBLEM_ID, "code": USER_CODE, "language": "PYTHON"},
                token=tok, timeout=TIMEOUT
            )
            lat = (time.time() - t0) * 1000
            with lock: results.append((status, lat, "ok"))
        except urllib.error.HTTPError as e:
            lat = (time.time() - t0) * 1000
            body = e.read()[:200].decode(errors='replace')
            with lock: results.append((e.code, lat, body[:60]))
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

    print("\n" + "="*62)
    print(f"  STRESS TEST — {N_USERS} concurrent practice/run (problem {PROBLEM_ID})")
    print("="*62)
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
    sc = Counter(s for s,_,__ in results)
    print(f"\n  HTTP codes  : {dict(sc)}")
    errs = list({m for _,_,m in results if "ok" not in m and "http" not in m})[:3]
    if errs: print(f"  Errors      : {errs}")
    print("="*62)

if __name__ == "__main__":
    main()
