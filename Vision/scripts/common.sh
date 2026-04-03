#!/usr/bin/env bash
# Resolves VENV_PYTHON for the current environment (WSL vs Git Bash / Windows).
# Source this file; it sets VENV_PYTHON.

if grep -qi microsoft /proc/version 2>/dev/null; then
  VENV_PYTHON=".venv/bin/python"
else
  VENV_PYTHON=".venv/Scripts/python.exe"
fi
