import { useState, useEffect } from 'react'
import './App.css'

// URL da API Java (pode ser trocada com VITE_API_URL no .env do frontend)
const API = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
const TAMANHO_PAGINA = 50

// fetch que lança um Error com a mensagem da API quando a resposta não é 2xx
// (formato da API: {"status": 400, "erro": "...", "campos": {"campo": "mensagem"}})
async function requisitar(caminho, opcoes) {
  let resposta
  try {
    resposta = await fetch(`${API}${caminho}`, opcoes)
  } catch {
    throw new Error('Não foi possível conectar à API. Verifique se o backend está rodando.')
  }

  const texto = await resposta.text()
  let corpo = null
  try {
    corpo = texto ? JSON.parse(texto) : null
  } catch {
    corpo = null
  }

  if (!resposta.ok) {
    const titulo = corpo?.erro ?? `Erro ${resposta.status}`
    const detalhes = corpo?.campos ? Object.values(corpo.campos).join(' ') : ''
    throw new Error(detalhes ? `${titulo}: ${detalhes}` : titulo)
  }
  return corpo
}

const formatarPreco = valor => `R$ ${Number(valor ?? 0).toFixed(2)}`

// Identifica uma carga (página + busca + versão) para saber se a tela está atualizada
const chaveDaCarga = (page, busca, versao) => `${page}|${busca}|${versao}`

function App() {
  const [produtos, setProdutos] = useState([])
  const [totalPages, setTotalPages] = useState(0)
  const [relatorio, setRelatorio] = useState(null)
  const [erroRelatorio, setErroRelatorio] = useState(null)
  const [erroCarga, setErroCarga] = useState(null)
  const [aviso, setAviso] = useState(null)

  const [page, setPage] = useState(0)
  const [busca, setBusca] = useState('')           // busca aplicada na listagem
  const [inputBusca, setInputBusca] = useState('') // texto ainda sendo digitado
  const [versao, setVersao] = useState(0)          // incrementar força recarregar
  const [cargaConcluida, setCargaConcluida] = useState(null)

  const [nome, setNome] = useState('')
  const [preco, setPreco] = useState('')
  const [salvando, setSalvando] = useState(false)

  const primeiraCarga = cargaConcluida === null
  const atualizando = cargaConcluida !== chaveDaCarga(page, busca, versao)

  useEffect(() => {
    // Ignora respostas de cargas antigas (ex.: trocar de página rápido)
    let cancelado = false
    const params = new URLSearchParams({ page, size: TAMANHO_PAGINA, busca })

    // allSettled: se o relatório (Python) falhar, a lista de produtos ainda aparece
    Promise.allSettled([
      requisitar(`/produtos?${params}`),
      requisitar('/relatorio'),
    ]).then(([prod, rel]) => {
      if (cancelado) return

      if (prod.status === 'fulfilled') {
        const { content, totalPages: total } = prod.value
        // A página ficou além do fim (ex.: apagou o último item da última página)
        if (page > 0 && page >= total) {
          setPage(Math.max(total - 1, 0))
          return
        }
        setProdutos(content)
        setTotalPages(total)
        setErroCarga(null)
      } else {
        setErroCarga(prod.reason.message)
      }

      if (rel.status === 'fulfilled') {
        setRelatorio(rel.value)
        setErroRelatorio(null)
      } else {
        setErroRelatorio(rel.reason.message)
      }

      setCargaConcluida(chaveDaCarga(page, busca, versao))
    })

    return () => { cancelado = true }
  }, [page, busca, versao])

  const recarregar = () => setVersao(v => v + 1)

  const pesquisar = () => {
    setBusca(inputBusca.trim())
    setPage(0)
    recarregar()
  }

  const cadastrarProduto = () => {
    if (!nome.trim() || !preco) {
      setAviso('Preencha nome e preço!')
      return
    }
    setSalvando(true)
    setAviso(null)
    requisitar('/produtos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nome, preco: parseFloat(preco) })
    })
      .then(() => {
        setNome('')
        setPreco('')
        recarregar()
      })
      .catch(e => setAviso(e.message))
      .finally(() => setSalvando(false))
  }

  const deletarProduto = (id) => {
    if (!confirm('Deseja deletar este produto?')) return
    setAviso(null)
    requisitar(`/produtos/${id}`, { method: 'DELETE' })
      .catch(e => setAviso(e.message))
      .finally(recarregar) // recarrega mesmo no 404: outra pessoa pode ter apagado
  }

  if (primeiraCarga) return (
    <div className="loading">
      <div className="spinner"></div>
      <p>Carregando dados...</p>
    </div>
  )

  // Sem produtos o serviço Python responde {"mensagem": ...}, sem os campos numéricos
  const temRelatorio = relatorio?.total_produtos !== undefined

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
        {erroCarga && (
          <div className="alerta" role="alert">
            <span>{erroCarga}</span>
            <button onClick={recarregar}>Tentar novamente</button>
          </div>
        )}
        {aviso && (
          <div className="alerta" role="alert">
            <span>{aviso}</span>
            <button onClick={() => setAviso(null)}>Fechar</button>
          </div>
        )}

        <section className="metrics">
          <div className="metric-card">
            <p className="metric-label">Total de Produtos</p>
            <p className="metric-value">{temRelatorio ? relatorio.total_produtos : 0}</p>
          </div>
          <div className="metric-card">
            <p className="metric-label">Preço Total</p>
            <p className="metric-value">{formatarPreco(temRelatorio ? relatorio.preco_total : 0)}</p>
          </div>
          <div className="metric-card">
            <p className="metric-label">Preço Médio</p>
            <p className="metric-value">{formatarPreco(temRelatorio ? relatorio.preco_medio : 0)}</p>
          </div>
          <div className="metric-card">
            <p className="metric-label">Produto Mais Caro</p>
            <p className="metric-value">{temRelatorio ? relatorio.produto_mais_caro.nome : '—'}</p>
          </div>
        </section>
        {erroRelatorio && (
          <p className="metric-aviso">Relatório indisponível: {erroRelatorio}</p>
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
            value={inputBusca}
            onChange={e => setInputBusca(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') pesquisar() }}
            className="input-field"
          />
          <button className="btn-cadastrar" onClick={pesquisar}>
            Pesquisar
          </button>
        </div>

        {/* Tabela de produtos */}
        <section className={`table-section${atualizando ? ' atualizando' : ''}`}>
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
              {produtos.length === 0 && (
                <tr>
                  <td colSpan={5} className="vazio">
                    {erroCarga
                      ? 'Não foi possível carregar os produtos.'
                      : busca
                        ? `Nenhum produto encontrado para "${busca}".`
                        : 'Nenhum produto cadastrado.'}
                  </td>
                </tr>
              )}
              {produtos.map(p => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.nome}</td>
                  <td>{formatarPreco(p.preco)}</td>
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
            <span>Página {page + 1} de {Math.max(totalPages, 1)}</span>
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
