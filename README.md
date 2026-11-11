# DataServer 🚀

Sistema full stack de gerenciamento de produtos construído com Java, Python e React.

## Stack
- **Java 21 + Spring Boot 4** — API REST com paginação, busca e validação
- **Python 3.11+ + FastAPI** — microserviço de análise de dados
- **React + Vite** — interface moderna e responsiva
- **PostgreSQL + Supabase** — banco de dados na nuvem

## Funcionalidades
- Dashboard com métricas em tempo real
- Listagem paginada de produtos
- Busca por nome integrada ao banco
- Cadastro e exclusão de produtos
- Análise estatística via Python
- API pronta para automações (ex.: n8n) com CRUD completo e erros em JSON

## Arquitetura

| Serviço  | Porta | Função |
|----------|-------|--------|
| Frontend | 5173  | Interface React (fala só com o Java) |
| Java     | 8080  | API de produtos e `/relatorio` (repassa ao Python) |
| Python   | 8001  | `/analise`: estatísticas calculadas no banco |

## Como rodar
### Configuração (credenciais)
Copie o `.env.example` para `.env` na raiz do projeto e preencha as credenciais do banco.
O `.env` é lido pelo Java e pelo Python e **nunca deve ser commitado** (já está no `.gitignore`).
```bash
cp .env.example .env
```

| Variável | Obrigatória | Padrão | Uso |
|----------|-------------|--------|-----|
| `DB_HOST`, `DB_USER`, `DB_PASSWORD` | sim | — | Conexão com o Supabase |
| `DB_PORT` / `DB_NAME` | não | `5432` / `postgres` | Conexão com o Supabase |
| `DB_POOL_SIZE` | não | `5` | Máximo de conexões do Java |
| `PYTHON_DB_POOL_MAX` | não | `3` | Máximo de conexões do Python |
| `PYTHON_SERVICE_URL` | não | `http://localhost:8001` | Onde o Java encontra o Python |
| `DB_SHOW_SQL` | não | `false` | Mostra no console cada SQL do Java |

O Supabase (porta 5432, modo sessão) aceita **15 conexões no total** entre todos os
serviços. Com os padrões, Java + Python usam no máximo 8.

### Java
```bash
cd dataserver
./mvnw spring-boot:run
```
### Python
```bash
cd python-service
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --port 8001 --reload
```
### Frontend
```bash
cd frontend
npm install
npm run dev
```
Para apontar o frontend para outra URL da API, defina `VITE_API_URL` (padrão `http://localhost:8080`).

## API (Java — porta 8080)

| Método | Rota | Descrição | Respostas |
|--------|------|-----------|-----------|
| GET    | `/produtos?page=0&size=20&busca=texto` | Lista paginada (`size` de 1 a 100) | 200, 400 |
| GET    | `/produtos/{id}` | Busca um produto | 200, 404 |
| POST   | `/produtos` | Cria um produto (um `id` no corpo é ignorado) | 200, 400 |
| PUT    | `/produtos/{id}` | Atualiza nome e preço | 200, 400, 404 |
| DELETE | `/produtos/{id}` | Remove um produto | 200, 404 |
| GET    | `/relatorio` | Estatísticas (total, soma, média, mais caro e mais barato) | 200 |

Corpo do POST/PUT:
```json
{ "nome": "Teclado", "preco": 250.0 }
```
`nome` é obrigatório (até 255 caracteres) e `preco` é obrigatório e maior que zero.

Erros vêm sempre no mesmo formato:
```json
{ "status": 400, "erro": "Dados inválidos", "campos": { "preco": "O preço deve ser maior que zero" } }
```

## Testes
```bash
cd dataserver
./mvnw test          # testes da API Java

cd frontend
npm run lint         # lint do React
```
