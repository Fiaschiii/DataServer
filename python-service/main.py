from fastapi import FastAPI

app = FastAPI()

@app.get("/analise")
def analisar_produtos():
    produtos = [
        {"id": 1, "nome": "Notebook", "preco": 3500.00},
        {"id": 2, "nome": "Mouse", "preco": 150.00},
        {"id": 3, "nome": "Teclado", "preco": 250.00},
    ]

    total = sum(p["preco"] for p in produtos)
    media = total / len(produtos)
    mais_caro = max(produtos, key=lambda p: p["preco"])

    return {
        "total_produtos": len(produtos),
        "preco_total": total,
        "preco_medio": media,
        "produto_mais_caro": mais_caro
    }