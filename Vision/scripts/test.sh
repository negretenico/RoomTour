#!/usr/bin/env bash
set -e

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"

"$VENV_PYTHON" -m pytest tests/ -v
