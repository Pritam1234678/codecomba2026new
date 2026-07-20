#!/usr/bin/env bash
#
# deploy_all.sh — Build + deploy CodeCombat backend.
#
#   VM (161.118.187.201, ubuntu) → Single VM with everything:
#   API/web/SSE/compiler + judge/practice + Postgres + Valkey
#
# Usage:
#   ./deploy_all.sh            # pull latest main + build + deploy
#
set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
VM_HOST="ubuntu@161.118.187.201"
VM_KEY="cc-vm_key.pem"
VM_REPO="~/codecombat"
VM_WAR="~/app.war"

SSH_OPTS="-o StrictHostKeyChecking=no -o ConnectTimeout=20"
BRANCH="main"

# ── Colours ─────────────────────────────────────────────────────────────────
c() { printf "\033[1;36m%s\033[0m\n" "$*"; }
g() { printf "\033[1;32m%s\033[0m\n" "$*"; }
r() { printf "\033[1;31m%s\033[0m\n" "$*"; }

# ── Deploy ────────────────────────────────────────────────────────────────────
deploy() {
  c "════════════════════════════════════════════════════════"
  c " VM ($VM_HOST) — pull + build + restart"
  c "════════════════════════════════════════════════════════"
  ssh -i "$VM_KEY" $SSH_OPTS "$VM_HOST" "
    set -e
    cd $VM_REPO
    git pull origin $BRANCH
    ./mvnw -q -DskipTests clean package
    cp target/*.war $VM_WAR
    sudo systemctl restart codecombat
  "
  g "VM build done, service restarting..."
}

# ── Health check ──────────────────────────────────────────────────────────────
health() {
  for i in $(seq 1 12); do
    sleep 5
    if ssh -i "$VM_KEY" $SSH_OPTS "$VM_HOST" 'curl -sf localhost:8080/api/health >/dev/null 2>&1'; then
      g "✓ VM healthy"
      return 0
    fi
    echo "  ... VM still starting ($((i*5))s)"
  done
  r "✗ VM did NOT come healthy in 60s — check: sudo journalctl -u codecombat -n 50"
  return 1
}

# ── Main ────────────────────────────────────────────────────────────────────
deploy
health
g "════════════════════════════════════════════════════════"
g " Deploy complete."
g "════════════════════════════════════════════════════════"
