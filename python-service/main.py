from fastapi import FastAPI
import psycopg2
import psycopg2.extras

app = FastAPI()

# Conexão com o Supabase via Session Pooler (IPv4)
def get_connection():
    return psycopg2.connect(
        host="aws-1-us-east-1.pooler.supabase.com",
        port=5432,
        database="postgres",
        user="postgres.jegxfffoldxaqmeyllml",
        password="fiaschi0987"
    )


@app.get("/analise")
def analisar_produtos():
    conn = get_connection()
    cursor = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    cursor.execute("SELECT * FROM produtos")
    produtos = cursor.fetchall()
    cursor.close()
    conn.close()

    if not produtos:
        return {"mensagem": "Nenhum produto encontrado"}
    
    total = sum(p["preco"] for p in produtos)
    media = total / len(produtos)
    mais_caro = max(produtos, key=lambda p: p["preco"])
    mais_barato = min(produtos, key=lambda p: p["preco"])


    return {
        "total_produtos": len(produtos),
        "preco_total": round(total, 2),
        "preco_medio": round(media, 2),
        "produto_mais_caro": dict(mais_caro),
        "produto_mais_barato": dict(mais_barato),
        "produtos": [dict(p) for p in produtos]
    }
       


