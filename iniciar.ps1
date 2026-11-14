# Sobe o DataServer local: Java (8080), Python (8001) e frontend (5173),
# cada um na sua janela do PowerShell. Para parar, feche as janelas (ou Ctrl+C em cada uma).
#
# Uso (na raiz do projeto):  powershell -ExecutionPolicy Bypass -File .\iniciar.ps1

$raiz = $PSScriptRoot

if (-not (Test-Path (Join-Path $raiz '.env'))) {
    Write-Host 'Arquivo .env nao encontrado na raiz. Copie o .env.example para .env e preencha.' -ForegroundColor Red
    exit 1
}

# ---------- Python ----------
$pasta = Join-Path $raiz 'python-service'
$venvPython = Join-Path $pasta 'venv\Scripts\python.exe'
if (-not (Test-Path $venvPython)) {
    Write-Host 'Criando o ambiente virtual do Python (primeira vez)...'
    & python -m venv (Join-Path $pasta 'venv')
    & $venvPython -m pip install -r (Join-Path $pasta 'requirements.txt')
}
Start-Process powershell -WorkingDirectory $pasta -ArgumentList '-NoExit', '-Command',
    "`$Host.UI.RawUI.WindowTitle = 'DataServer - Python (8001)'; & '$venvPython' -m uvicorn main:app --port 8001 --reload"

# ---------- Java ----------
$pasta = Join-Path $raiz 'dataserver'
Start-Process powershell -WorkingDirectory $pasta -ArgumentList '-NoExit', '-Command',
    "`$Host.UI.RawUI.WindowTitle = 'DataServer - Java (8080)'; .\mvnw.cmd spring-boot:run"

# ---------- Frontend ----------
$pasta = Join-Path $raiz 'frontend'
$instalar = if (Test-Path (Join-Path $pasta 'node_modules')) { '' } else { 'npm install; ' }
Start-Process powershell -WorkingDirectory $pasta -ArgumentList '-NoExit', '-Command',
    "`$Host.UI.RawUI.WindowTitle = 'DataServer - Frontend (5173)'; ${instalar}npm run dev"

Write-Host 'Subindo Java, Python e frontend em janelas separadas.' -ForegroundColor Green
Write-Host 'Quando o Java terminar de subir (Started DataserverApplication), abra http://localhost:5173'
