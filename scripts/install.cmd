@echo off
rem Installs what NamBeDe needs on Windows: Python first when it is missing (with winget), then the rest with
rem scripts\deploy.py install (Docker Desktop).
rem Run it from the repo folder: scripts\install.cmd   (extra options are passed on, e.g. --dry-run)
setlocal
set "PY=py"
where py >nul 2>nul
if errorlevel 1 (
    echo Chua co Python: cai Python 3.12 bang winget...
    winget install -e --id Python.Python.3.12 --accept-source-agreements --accept-package-agreements
    if errorlevel 1 (
        echo Khong cai duoc Python. Cai tay tu https://www.python.org/downloads/ roi chay lai.
        exit /b 1
    )
    set "PY=%SystemRoot%\py.exe"
    if exist "%LOCALAPPDATA%\Programs\Python\Launcher\py.exe" set "PY=%LOCALAPPDATA%\Programs\Python\Launcher\py.exe"
)
if not "%PY%"=="py" if not exist "%PY%" (
    echo Da cai Python. Mo cua so dong lenh moi roi chay lai: scripts\install.cmd
    exit /b 0
)
"%PY%" "%~dp0deploy.py" install %*
