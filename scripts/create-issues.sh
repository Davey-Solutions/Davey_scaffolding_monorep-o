#!/usr/bin/env bash
# Creates one GitHub issue per ticket in docs/plan/project-plan.md,
# labelled with its milestone, using the GitHub CLI.
#
# Usage:
#   gh auth login          # once, if not already authenticated
#   ./scripts/create-issues.sh [--dry-run]
#
# Idempotency: a ticket is skipped if an open or closed issue whose title
# starts with its ID (e.g. "DAV-7:") already exists.

set -euo pipefail

REPO="${REPO:-Davey-Solutions/Davey_scaffolding_monorep-o}"
PLAN="$(dirname "$0")/../docs/plan/project-plan.md"
DRY_RUN="${1:-}"

command -v gh >/dev/null || { echo "gh CLI is required: https://cli.github.com" >&2; exit 1; }
[ -f "$PLAN" ] || { echo "Plan not found: $PLAN" >&2; exit 1; }

existing_titles="$(gh issue list --repo "$REPO" --state all --limit 500 --json title --jq '.[].title' 2>/dev/null || true)"

ensure_label() {
  local label="$1"
  if [ "$DRY_RUN" = "--dry-run" ]; then return; fi
  gh label create "$label" --repo "$REPO" --color BFD4F2 --force >/dev/null
}

create_issue() {
  local id="$1" title="$2" label="$3" body="$4"
  if printf '%s\n' "$existing_titles" | grep -q "^${id}:"; then
    echo "SKIP   ${id} (issue already exists)"
    return
  fi
  if [ "$DRY_RUN" = "--dry-run" ]; then
    echo "DRY    ${id}: ${title}  [${label}]"
    return
  fi
  ensure_label "$label"
  gh issue create --repo "$REPO" \
    --title "${id}: ${title}" \
    --label "$label" \
    --body "$body" >/dev/null
  echo "CREATE ${id}: ${title}"
}

milestone=""
id=""
title=""
body=""

flush() {
  if [ -n "$id" ]; then
    create_issue "$id" "$title" "$milestone" "$body
---
From [docs/plan/project-plan.md](https://github.com/${REPO}/blob/main/docs/plan/project-plan.md) — ${milestone}."
  fi
  id=""
  title=""
  body=""
}

while IFS= read -r line; do
  case "$line" in
    "## Milestone"*)
      flush
      # "## Milestone 0 — Repository scaffolding" -> "milestone-0"
      milestone="milestone-$(printf '%s' "$line" | sed -E 's/^## Milestone ([0-9]+).*/\1/')"
      ;;
    "### DAV-"*)
      flush
      id="$(printf '%s' "$line" | sed -E 's/^### (DAV-[0-9]+):.*/\1/')"
      title="$(printf '%s' "$line" | sed -E 's/^### DAV-[0-9]+: //')"
      ;;
    *)
      if [ -n "$id" ]; then
        body="${body}${line}
"
      fi
      ;;
  esac
done < "$PLAN"
flush

echo "Done."
