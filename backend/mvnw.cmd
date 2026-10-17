@echo off
rem Lance Maven sans l'installer : il se telecharge tout seul la premiere fois.
rem Voir mvnw.ps1 pour le detail.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0mvnw.ps1" %*
