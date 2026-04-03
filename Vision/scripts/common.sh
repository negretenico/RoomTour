#!/usr/bin/env bash
# Resolves VENV_PYTHON for the current environment.
# Probes actual venv layout rather than assuming — handles WSL, Git Bash, MSYS2, and Linux CI.
# Source this file; it sets VENV_PYTHON.

if [ -f ".venv/Scripts/python.exe" ]; then
  VENV_PYTHON=".venv/Scripts/python.exe"
elif [ -f ".venv/bin/python" ]; then
  VENV_PYTHON=".venv/bin/python"
else
  echo "ERROR: .venv not found — run scripts/setup.sh first" >&2
  exit 1
fi
