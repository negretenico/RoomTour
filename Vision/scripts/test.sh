#!/usr/bin/env bash
set -e

if grep -qi microsoft /proc/version 2>/dev/null; then
  VENV_PYTHON=".venv/bin/python"
else
  VENV_PYTHON=".venv/Scripts/python.exe"
fi

"$VENV_PYTHON" -m pytest tests/ -v
