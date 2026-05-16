import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [produtos, setProdutos] = useState([])
  const [relatorio, setRelatorio] = useState(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    setLoading(true)
    fetch(`http://localhost:8080/produtos?page=${page}&size=50`)
      .then(res => res.json())
      .then(data => {
        // API paginada retorna { content: [...], totalPages: N }
        setProdutos(data.content)
        setTotalPages(data.totalPages)
      })

    fetch('http://localhost:8080/relatorio')
      .then(res => res.json())
      .then(data => setRelatorio(data))
      .finally(() => setLoading(false))
  }, [page])

  if (loading) return (
    <div className="loading">
      <div className="spinner"></div>
      <p>Carregando dados...</p>
    </div>
  )

  return (
    <div className="app">
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-icon">⬡</span>
            <span className="logo-text">DataServer</span>
          </div>
          <span className="badge">v1.0</span>
        </div>
      </header>

      <main className="main">
        {relatorio && (
          <section className="metrics">
            <div className="metric-card">
              <p className="metric-label">Total de Produtos</p>
              <p className="metric-value">{relatorio.total_produtos}</p>
            </div>
            <div className="metric-card">
              <p className="metric-label">Preço Total</p>
              <p className="metric-value">R$ {relatorio.preco_total.toFixed(2)}</p>
            </div>
            <div className="metric-card">
              <p className="metric-label">Preço Médio</p>
              <p className="metric-value">R$ {relatorio.preco_medio.toFixed(2)}</p>
            </div>
            <div className="metric-card highlight">
              <p className="metric-label">Produto Mais Caro</p>
              <p className="metric-value">{relatorio.produto_mais_caro.nome}</p>
            </div>
          </section>
        )}

        <section className="table-section">
          <div className="table-header">
            <h2>Produtos</h2>
            <span className="tag">Java API</span>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nome</th>
                <th>Preço</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {produtos.map(p => (
                <tr key={p.id}>
                  <td>#{p.id}</td>
                  <td>{p.nome}</td>
                  <td>R$ {p.preco.toFixed(2)}</td>
                  <td><span className="status">Ativo</span></td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Paginação */}
          <div className="pagination">
            <button
              onClick={() => setPage(p => p - 1)}
              disabled={page === 0}
            >
              ← Anterior
            </button>
            <span>Página {page + 1} de {totalPages}</span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
            >
              Próxima →
            </button>
          </div>
        </section>

        <footer className="footer">
          <span>DataServer 2026</span>
          <span>Made in Brazil</span>
        </footer>
      </main>
    </div>
  )
}

export default App
