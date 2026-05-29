# DataServer 🚀

Sistema full stack de gerenciamento de produtos construído com Java, Python e React.

## Stack
- **Java 21 + Spring Boot** — API REST com paginação e busca
- **Python 3.13 + FastAPI** — microserviço de análise de dados
- **React + Vite** — interface moderna e responsiva
- **PostgreSQL + Supabase** — banco de dados na nuvem

## Funcionalidades
- Dashboard com métricas em tempo real
- Listagem paginada de produtos
- Busca por nome integrada ao banco
- Cadastro e exclusão de produtos
- Análise estatística via Python

## Como rodar
### Java
```bash
cd dataserver
./mvnw spring-boot:run
```
### Python
```bash
cd python-service
source venv/bin/activate
uvicorn main:app --port 8001 --reload
```
### Frontend
```bash
cd frontend
npm install
npm run dev
```
