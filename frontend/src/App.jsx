import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [produtos, setProdutos] = useState([])
  const [relatorio, setRelatorio] = useState(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [nome, setNome] = useState('')
  const [preco, setPreco] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [busca, setBusca] = useState('')
  const [inputBusca, setInputBusca] = useState('')

  const carregarDados = (paginaAtual = page, buscaAtual = busca) => {
    Promise.all([
      fetch(`http://localhost:8080/produtos?page=${paginaAtual}&size=50&busca=${buscaAtual}`).then(r => r.json()),
      fetch('http://localhost:8080/relatorio').then(r => r.json())
    ]).then(([prod, rel]) => {
      setProdutos(prod.content)
      setTotalPages(prod.totalPages)
      setRelatorio(rel)
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    setLoading(true)
    carregarDados()
  }, [page])


  const cadastrarProduto = () => {
    if (!nome || !preco) return alert('Preencha nome e preço!')
    setSalvando(true)
    fetch('http://localhost:8080/produtos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nome, preco: parseFloat(preco) })
    })
      .then(() => {
        setNome('')
        setPreco('')
        carregarDados()
      })
      .finally(() => setSalvando(false))
  }

  const deletarProduto = (id) => {
    if (!confirm('Deseja deletar este produto?')) return
    fetch(`http://localhost:8080/produtos/${id}`, { method: 'DELETE' })
      .then(() => carregarDados())
  }

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
            <div className="metric-card">
              <p className="metric-label">Produto Mais Caro</p>
              <p className="metric-value">{relatorio.produto_mais_caro.nome}</p>
            </div>
          </section>
        )}

        {/* Formulário de cadastro */}
        <section className="table-section">
          <div className="table-header">
            <h2>Novo Produto</h2>
          </div>
          <div className="form-cadastro">
            <input
              type="text"
              placeholder="Nome do produto"
              value={nome}
              onChange={e => setNome(e.target.value)}
              className="input-field"
            />
            <input
              type="number"
              placeholder="Preço (ex: 199.90)"
              value={preco}
              onChange={e => setPreco(e.target.value)}
              className="input-field"
            />
            <button
              onClick={cadastrarProduto}
              disabled={salvando}
              className="btn-cadastrar"
            >
              {salvando ? 'Salvando...' : '+ Cadastrar'}
            </button>
          </div>
        </section>

        {/* Barra de Pesquisa */}
        <div className="search-bar">
          <input
            type="text"
            placeholder="Pesquisar produto pelo nome..."
            value={busca}
            onChange={e => setBusca(e.target.value)}
            onKeyDown={e => {if (e.key === 'Enter') {setPage(0); carregarDados(0, busca) } }}
            className="input-field"
          />
          <button
            className="btn-cadastrar"
            onclick={() => { setPage(0); carregarDados(0, busca) }}
          >
            Pesquisar
          </button>
        </div>

        {/* Tabela de produtos */}
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
                <th>Ação</th>
              </tr>
            </thead>
            <tbody>
              {produtos.map(p => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.nome}</td>
                  <td>R$ {p.preco.toFixed(2)}</td>
                  <td><span className="status">Ativo</span></td>
                  <td>
                    <button
                      className="btn-deletar"
                      onClick={() => deletarProduto(p.id)}
                    >
                      Deletar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>
              ← Anterior
            </button>
            <span>Página {page + 1} de {totalPages}</span>
            <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>
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