#!/usr/bin/env bash
# Publish all four skills to ClawHub.
# Default is a dry run. Pass --live to actually publish.
# Requires: npm i -g clawhub   and   clawhub login
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="--dry-run"
if [[ "${1:-}" == "--live" ]]; then
  MODE=""
fi

python3 "$ROOT/tools/validate_skills.py" "$ROOT"

publish() {
  local slug="$1" name="$2" topics="$3"
  echo "==> $slug"
  clawhub skill publish "$ROOT/$slug" \
    --slug "$slug" \
    --name "$name" \
    --version 1.0.0 \
    --topics "$topics" \
    --changelog "Initial release" \
    $MODE
}

publish contract-redline-reviewer  "Contract Redline Reviewer"  "legal,contracts,business,review"
publish meeting-to-actions         "Meeting to Actions"         "meetings,productivity,summarize,notes"
publish pr-review-assistant        "PR Review Assistant"        "code-review,github,security,developer"
publish product-description-writer "Product Description Writer" "ecommerce,seo,copywriting,shopify"
