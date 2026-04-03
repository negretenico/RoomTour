#!/usr/bin/env bash
set -e

VENV_DIR=".venv"

# Detect environment by structure, not by executing anything
if grep -qi microsoft /proc/version 2>/dev/null; then
  IS_WSL=true
else
  IS_WSL=false
fi

# Pick Python
if $IS_WSL; then
  PYTHON=$(command -v python3 || command -v python || true)
else
  WIN_PYTHON="/c/Users/negre/AppData/Local/Python/bin/python.exe"
  if [ -x "$WIN_PYTHON" ]; then
    PYTHON="$WIN_PYTHON"
  else
    PYTHON=$(command -v python3 || command -v python || command -v py || true)
  fi
fi

if [ -z "$PYTHON" ]; then
  echo "ERROR: no python executable found" >&2
  exit 1
fi

# Recreate venv if it was built for the wrong environment.
# A valid venv must have either Scripts/python.exe (Windows CPython) or bin/python (Unix).
# If neither exists the venv is corrupt — rebuild it.
if [ -d "$VENV_DIR" ]; then
  if [ ! -f "$VENV_DIR/Scripts/python.exe" ] && [ ! -f "$VENV_DIR/bin/python" ]; then
    echo "Corrupt or empty venv detected — rebuilding..." >&2
    rm -rf "$VENV_DIR"
  fi
fi

if [ ! -d "$VENV_DIR" ]; then
  "$PYTHON" -m venv "$VENV_DIR"
fi

# Resolve venv Python by probing actual layout — don't assume
if [ -f "$VENV_DIR/Scripts/python.exe" ]; then
  VENV_PYTHON="$VENV_DIR/Scripts/python.exe"
elif [ -f "$VENV_DIR/bin/python" ]; then
  VENV_PYTHON="$VENV_DIR/bin/python"
else
  echo "ERROR: venv was created but no python binary found in $VENV_DIR" >&2
  exit 1
fi

"$VENV_PYTHON" -m pip install -r requirements.txt
"$VENV_PYTHON" -m pip install -e .
