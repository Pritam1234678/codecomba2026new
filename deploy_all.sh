#!/usr/bin/env bash
#
# deploy_all.sh — Build + deploy CodeCombat backend to BOTH production VMs.
#
#   VM1 (92.4.78.195, ubuntu)  → API/web/SSE/compiler + shared Postgres+Valkey
#   VM2 (140.245.4.112, opc)   → dedicated judge/practice execution engine
#
# Both VMs run the SAME commit so they never drift. VM1 owns Flyway migrations
# (it boots first); VM2 just validates the schema against the shared DB.
#
# Usage:
#   ./deploy_all.sh            # pull latest main + build + deploy to both
#   ./deploy_all.sh --vm1      # deploy VM1 only
#   ./deploy_all.sh --vm2      # deploy VM2 only
#
set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
VM1_HOST="ubuntu@92.4.78.195"
VM1_KEY="oci_vm_key"
VM1_REPO="~/codecombat"
VM1_WAR="~/app.war"

VM2_HOST="opc@140.245.4.112"
VM2_KEY="ssh-key-2026-05-17(7).key"
VM2_REPO="~/codecombat"
VM2_WAR="/opt/codecombat/app.war"

SSH_OPTS="-o StrictHostKeyChecking=no -o ConnectTimeout=20"
BRANCH="main"

# ── Colours ─────────────────────────────────────────────────────────────────
c() { printf "\033[1;36m%s\033[0m\n" "$*"; }   # cyan
g() { printf "\033[1;32m%s\033[0m\n" "$*"; }   # green
r() { printf "\033[1;31m%s\033[0m\n" "$*"; }   # red

# ── Deploy VM1 ────────────────────────────────────────────────────────────────
deploy_vm1() {
  c "════════════════════════════════════════════════════════"
  c " VM1 ($VM1_HOST) — pull + build + restart"
  c "════════════════════════════════════════════════════════"
  ssh -i "$VM1_KEY" $SSH_OPTS "$VM1_HOST" "
    set -e
    cd $VM1_REPO
    git pull origin $BRANCH
    ./mvnw -q -DskipTests clean package
    cp target/*.war $VM1_WAR
    sudo systemctl restart codecombat
  "
  g "VM1 build done, service restarting..."
}

# ── Deploy VM2 ────────────────────────────────────────────────────────────────
deploy_vm2() {
  c "════════════════════════════════════════════════════════"
  c " VM2 ($VM2_HOST) — pull + build + restart"
  c "════════════════════════════════════════════════════════"
  ssh -i "$VM2_KEY" $SSH_OPTS "$VM2_HOST" "
    set -e
    cd $VM2_REPO
    git pull origin $BRANCH
    ./mvnw -q -DskipTests clean package
    sudo cp target/*.war $VM2_WAR
    sudo restorecon -v $VM2_WAR || true
    sudo systemctl restart codecombat
  "
  g "VM2 build done, service restarting..."
}

# ── Health check ──────────────────────────────────────────────────────────────
health() {
  local name="$1" key="$2" host="$3"
  for i in $(seq 1 12); do
    sleep 5
    if ssh -i "$key" $SSH_OPTS "$host" 'curl -sf localhost:8080/api/health >/dev/null 2>&1'; then
      g "✓ $name healthy"
      return 0
    fi
    echo "  ... $name still starting ($((i*5))s)"
  done
  r "✗ $name did NOT come healthy in 60s — check: sudo journalctl -u codecombat -n 50"
  return 1
}

# ── Main ────────────────────────────────────────────────────────────────────
TARGET="${1:-all}"
case "$TARGET" in
  --vm1) deploy_vm1; health "VM1" "$VM1_KEY" "$VM1_HOST" ;;
  --vm2) deploy_vm2; health "VM2" "$VM2_KEY" "$VM2_HOST" ;;
  all|"")
    # VM1 first so Flyway migrations land before VM2 validates the schema.
    deploy_vm1
    health "VM1" "$VM1_KEY" "$VM1_HOST"
    deploy_vm2
    health "VM2" "$VM2_KEY" "$VM2_HOST"
    ;;
  *) r "Unknown arg: $TARGET (use --vm1 | --vm2 | all)"; exit 1 ;;
esac

g "════════════════════════════════════════════════════════"
g " Deploy complete."
g "════════════════════════════════════════════════════════"
