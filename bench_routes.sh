#!/bin/bash
# Route latency benchmark — runs ON VM (localhost), eliminates internet RTT.
set -uo pipefail
BASE=http://localhost:8080

ADMIN_TOK=$(curl -sS -X POST "$BASE/api/auth/signin" \
  -H "Content-Type: application/json" \
  -d '{"username":"Pritammandal143","password":"Mandalp166#"}' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("accessToken") or d.get("token") or d.get("jwt") or "FAIL")')

USER_TOK=$(curl -sS -X POST "$BASE/api/auth/signin" \
  -H "Content-Type: application/json" \
  -d '{"username":"Newuser1","password":"Mandalp166#"}' \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print(d.get("accessToken") or d.get("token") or d.get("jwt") or "FAIL")' 2>/dev/null || echo "FAIL")

echo "Admin token len: ${#ADMIN_TOK}"
echo "User  token len: ${#USER_TOK}"
echo ""

bench() {
  local method="$1" path="$2" auth="$3"
  local hdr=""
  if [ "$auth" = "admin" ]; then hdr="Authorization: Bearer $ADMIN_TOK"; fi
  if [ "$auth" = "user" ];  then hdr="Authorization: Bearer $USER_TOK";  fi

  valkey-cli FLUSHDB > /dev/null 2>&1

  local cold warm1 warm2 status
  if [ -z "$hdr" ]; then
    cold=$(curl -sS -o /dev/null -w "%{time_total}|%{http_code}" -X "$method" "$BASE$path")
  else
    cold=$(curl -sS -o /dev/null -w "%{time_total}|%{http_code}" -X "$method" -H "$hdr" "$BASE$path")
  fi
  status=$(echo "$cold" | cut -d'|' -f2)
  cold=$(echo "$cold" | cut -d'|' -f1)

  if [ -z "$hdr" ]; then
    warm1=$(curl -sS -o /dev/null -w "%{time_total}" -X "$method" "$BASE$path")
    warm2=$(curl -sS -o /dev/null -w "%{time_total}" -X "$method" "$BASE$path")
  else
    warm1=$(curl -sS -o /dev/null -w "%{time_total}" -X "$method" -H "$hdr" "$BASE$path")
    warm2=$(curl -sS -o /dev/null -w "%{time_total}" -X "$method" -H "$hdr" "$BASE$path")
  fi

  # Convert to ms with 0 decimal
  cold_ms=$(python3 -c "print(int(float('$cold')*1000))")
  warm1_ms=$(python3 -c "print(int(float('$warm1')*1000))")
  warm2_ms=$(python3 -c "print(int(float('$warm2')*1000))")

  printf "%-50s %-7s %s  cold=%4sms  warm1=%4sms  warm2=%4sms\n" \
    "$path" "[$auth]" "$status" "$cold_ms" "$warm1_ms" "$warm2_ms"
}

echo "═══════════════════════════════════════════════════════════════════════════════════════════"
echo "  ROUTE BENCHMARK — cold=Valkey flushed, warm1/2=cached"
echo "═══════════════════════════════════════════════════════════════════════════════════════════"

bench GET  "/api/contests"                          none
bench GET  "/api/contests/1"                        user
bench GET  "/api/contests/1/detail"                 user
bench GET  "/api/problems"                          user
bench GET  "/api/problems/1"                        user
bench GET  "/api/problems/contest/1"                user
bench GET  "/api/problems/1/contest-status"         user
bench GET  "/api/problems/1/snippets"               user
bench GET  "/api/problems/1/snippets/JAVA"          user
bench GET  "/api/practice/problems"                 user
bench GET  "/api/practice/stats"                    user
bench GET  "/api/practice/problems/1"               user
bench GET  "/api/user/profile"                      user
bench GET  "/api/user/dashboard"                    user
bench GET  "/api/submissions/user"                  user
bench GET  "/api/submissions/user/1"                user
bench GET  "/api/admin/dashboard"                   admin
bench GET  "/api/admin/users"                       admin
bench GET  "/api/admin/users/stats"                 admin
bench GET  "/api/admin/contests"                    admin
bench GET  "/api/admin/contests/stats"              admin
bench GET  "/api/admin/contests/1/available-problems?page=0&size=10" admin
bench GET  "/api/admin/problems"                    admin
bench GET  "/api/admin/problems/contest/1"          admin
bench GET  "/api/admin/problems/1/contests"         admin
bench GET  "/api/admin/problems/1/snippets/admin"   admin
bench GET  "/api/admin/leaderboard/contest/1"       admin
bench GET  "/api/admin/duels/metrics"               admin
bench GET  "/api/admin/duels"                       admin
bench GET  "/api/admin/duels/eligible-problems"     admin
echo ""
echo "Done."
