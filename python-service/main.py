import os
import threading
from contextlib import asynccontextmanager, contextmanager
from pathlib import Path

from dotenv import load_dotenv
from fastapi import FastAPI
import psycopg2
import psycopg2.extras
from psycopg2.pool import ThreadedConnectionPool

# Carrega as credenciais do .env na raiz do projeto.
# Variáveis de ambiente do sistema têm prioridade sobre o .env.
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

# Máximo de conexões abertas ao mesmo tempo com o banco (o Supabase tem limite
# de conexões compartilhado com o Java). Requisições além disso esperam a vez.
MAX_CONEXOES = int(os.environ.get("PYTHON_DB_POOL_MAX", "3"))

_pool = None
_pool_lock = threading.Lock()
# O pool do psycopg2 dá erro quando esgota em vez de esperar; o semáforo faz esperar.
_vagas = threading.BoundedSemaphore(MAX_CONEXOES)


def _get_pool():
    # Criado só no primeiro uso, para o serviço subir mesmo se o banco estiver fora
    global _pool
    with _pool_lock:
        if _pool is None:
            # minconn=1: uma conexão fica aberta e é reaproveitada; as extras
            # (até MAX_CONEXOES) são fechadas ao serem devolvidas.
            _pool = ThreadedConnectionPool(
                minconn=1,
                maxconn=MAX_CONEXOES,
                host=os.environ["DB_HOST"],
                port=int(os.environ.get("DB_PORT", "5432")),
                database=os.environ.get("DB_NAME", "postgres"),
                user=os.environ["DB_USER"],
                password=os.environ["DB_PASSWORD"],
                application_name="dataserver-python",
            )
        return _pool


@contextmanager
def conexao():
    # Empresta uma conexão do pool e sempre devolve, mesmo se a consulta falhar
    with _vagas:
        pool = _get_pool()
        conn = pool.getconn()
        try:
            yield conn
            conn.rollback()  # encerra a transação de leitura; a conexão volta limpa
        except Exception:
            # Conexão possivelmente quebrada: descarta em vez de reaproveitar
            pool.putconn(conn, close=True)
            raise
        else:
            pool.putconn(conn)


@asynccontextmanager
async def lifespan(app):
    yield
    if _pool is not None:
        _pool.closeall()


app = FastAPI(lifespan=lifespan)

# Toda a agregação é feita no banco: só uma linha volta, em vez da tabela inteira.
# Em caso de empate no preço, vale o produto de menor id.
SQL_ANALISE = """
    SELECT
        COUNT(*)   AS total_produtos,
        SUM(preco) AS preco_total,
        AVG(preco) AS preco_medio,
        (SELECT row_to_json(p) FROM (
            SELECT id, nome, preco FROM produtos ORDER BY preco DESC, id LIMIT 1) p
        ) AS produto_mais_caro,
        (SELECT row_to_json(p) FROM (
            SELECT id, nome, preco FROM produtos ORDER BY preco ASC, id LIMIT 1) p
        ) AS produto_mais_barato
    FROM produtos
"""


@app.get("/analise")
def analisar_produtos():
    with conexao() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cursor:
            cursor.execute(SQL_ANALISE)
            r = cursor.fetchone()

    if r["total_produtos"] == 0:
        return {"mensagem": "Nenhum produto encontrado"}

    return {
        "total_produtos": r["total_produtos"],
        "preco_total": round(float(r["preco_total"]), 2),
        "preco_medio": round(float(r["preco_medio"]), 2),
        "produto_mais_caro": r["produto_mais_caro"],
        "produto_mais_barato": r["produto_mais_barato"],
    }
