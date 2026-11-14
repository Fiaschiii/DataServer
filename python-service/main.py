import hmac
import os
import threading
from contextlib import asynccontextmanager, contextmanager
from pathlib import Path

from dotenv import load_dotenv
from fastapi import Depends, FastAPI, Header, HTTPException
import psycopg2
import psycopg2.extras
from psycopg2.pool import ThreadedConnectionPool

# Carrega as credenciais do .env na raiz do projeto.
# Variáveis de ambiente do sistema têm prioridade sobre o .env.
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

# Máximo de conexões abertas ao mesmo tempo com o banco (o Supabase tem limite
# de conexões compartilhado com o Java). Requisições além disso esperam a vez.
MAX_CONEXOES = int(os.environ.get("PYTHON_DB_POOL_MAX", "3"))

# No Render o serviço fica público na internet: com CHAVE_INTERNA definida, o /analise
# só responde a quem mandar a mesma chave no cabeçalho X-Chave-Interna (o Java).
# Vazia (uso local) = sem exigência.
CHAVE_INTERNA = os.environ.get("CHAVE_INTERNA", "")

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


def exigir_chave_interna(x_chave_interna: str | None = Header(default=None)):
    # compare_digest: a comparação leva o mesmo tempo acerte ou erre (não vaza a chave)
    if CHAVE_INTERNA and not hmac.compare_digest(x_chave_interna or "", CHAVE_INTERNA):
        raise HTTPException(status_code=401, detail="Chave interna ausente ou inválida")


# Health check do Render: responde sem tocar no banco
@app.get("/saude")
def saude():
    return {"status": "ok"}


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


@app.get("/analise", dependencies=[Depends(exigir_chave_interna)])
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
