import { useState, useEffect } from 'react'
import {
  ArrowLeftIcon, ChevronLeftIcon, ChevronRightIcon, PlusIcon,
  SearchIcon, SignOutIcon, SyncIcon, TrashIcon, XIcon,
} from '@primer/octicons-react'
import { requisitar } from './api'

const TAMANHO_PAGINA = 50

// Números no padrão brasileiro: R$ 49.773.461,53 e 10.008
const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const formatoInteiro = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 })
const formatarPreco = valor => formatoMoeda.format(Number(valor ?? 0))
const formatarQuantidade = valor => formatoInteiro.format(Number(valor ?? 0))

// Identifica uma carga (página + busca + versão) para saber se a tela está atualizada
const chaveDaCarga = (page, busca, versao) => `${page}|${busca}|${versao}`

// Painel principal (só aparece logado): métricas, cadastro, busca e lista de produtos
function Painel({ nomeUsuario, email, onSair }) {
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

  // Sai da pesquisa e volta para a lista completa (a mesma de quando se entra no sistema)
  const voltarParaLista = () => {
    setInputBusca('')
    setBusca('')
    setPage(0)
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
          <div className="header-usuario">
            {/* Mostra o nome; sem nome cadastrado, o e-mail. O e-mail aparece ao passar o mouse. */}
            <span className="usuario-email" title={email}>{nomeUsuario || email}</span>
            <button className="btn-sair btn-icone" onClick={onSair}>
              <SignOutIcon className="icone anima-direita" />
              Sair
            </button>
          </div>
        </div>
      </header>

      <main className="main">
        {erroCarga && (
          <div className="alerta" role="alert">
            <span>{erroCarga}</span>
            <button className="btn-icone" onClick={recarregar}>
              <SyncIcon className="icone anima-girar" />
              Tentar novamente
            </button>
          </div>
        )}
        {aviso && (
          <div className="alerta" role="alert">
            <span>{aviso}</span>
            <button className="btn-icone" onClick={() => setAviso(null)}>
              <XIcon className="icone anima-girar-90" />
              Fechar
            </button>
          </div>
        )}

        <section className="metrics">
          <div className="metric-card">
            <p className="metric-label">Total de Produtos</p>
            <p className="metric-value">{formatarQuantidade(temRelatorio ? relatorio.total_produtos : 0)}</p>
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
            <p className="metric-value metric-texto" title={temRelatorio ? relatorio.produto_mais_caro.nome : undefined}>
              {temRelatorio ? relatorio.produto_mais_caro.nome : '—'}
            </p>
            {temRelatorio && (
              <p className="metric-detalhe">{formatarPreco(relatorio.produto_mais_caro.preco)}</p>
            )}
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
              className="btn-cadastrar btn-icone"
            >
              <PlusIcon className="icone anima-girar-90" />
              {salvando ? 'Salvando...' : 'Cadastrar'}
            </button>
          </div>
        </section>

        {/* Barra de Pesquisa */}
        <div className="search-bar">
          <input
            type="text"
            placeholder="Pesquisar produto pelo nome..."
            value={inputBusca}
            onChange={e => {
              setInputBusca(e.target.value)
              // Campo esvaziado durante uma pesquisa: volta sozinho para a lista completa
              if (busca && e.target.value.trim() === '') voltarParaLista()
            }}
            onKeyDown={e => { if (e.key === 'Enter') pesquisar() }}
            className="input-field"
          />
          <button className="btn-cadastrar btn-icone" onClick={pesquisar}>
            <SearchIcon className="icone anima-procurar" />
            Pesquisar
          </button>
          {busca && (
            <button className="btn-voltar btn-icone" onClick={voltarParaLista}>
              <ArrowLeftIcon className="icone anima-esquerda" />
              Voltar à lista
            </button>
          )}
        </div>
        {busca && (
          <p className="busca-ativa">
            Mostrando resultados para <strong>"{busca}"</strong>
          </p>
        )}

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
                  <td className="preco">{formatarPreco(p.preco)}</td>
                  <td><span className="status">Ativo</span></td>
                  <td>
                    <button
                      className="btn-deletar btn-icone"
                      onClick={() => deletarProduto(p.id)}
                    >
                      <TrashIcon className="icone anima-tremer" />
                      Deletar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="pagination">
            <button className="btn-icone" onClick={() => setPage(p => p - 1)} disabled={page === 0}>
              <ChevronLeftIcon className="icone anima-esquerda" />
              Anterior
            </button>
            <span>Página {page + 1} de {Math.max(totalPages, 1)}</span>
            <button className="btn-icone" onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>
              Próxima
              <ChevronRightIcon className="icone anima-direita" />
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

export default Painel
