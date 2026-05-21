import { useState } from 'react'
import { Link } from 'react-router-dom'
import '../App.css'

function Cadastro() {
  const [nome, setNome] = useState('')
  const [preco, setPreco] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState(null)

  const cadastrarProduto = () => {
    if (!nome || !preco) return alert('Preencha nome e preço!')
    setSalvando(true)
    fetch('http://localhost:8080/produtos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nome, preco: parseFloat(preco) })
    })
      .then(res => res.json())
      .then(() => {
        setNome('')
        setPreco('')
        setMensagem({ tipo: 'sucesso', texto: 'Produto cadastrado com sucesso!' })
        setTimeout(() => setMensagem(null), 3000)
      })
      .catch(() => setMensagem({ tipo: 'erro', texto: 'Erro ao cadastrar produto.' }))
      .finally(() => setSalvando(false))
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-icon">⬡</span>
            <span className="logo-text">DataServer</span>
          </div>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <Link to="/" className="nav-link">← Voltar ao Dashboard</Link>
            <span className="badge">v1.0</span>
          </div>
        </div>
      </header>

      <main className="main">
        <section className="table-section">
          <div className="table-header">
            <h2>Cadastrar Produto</h2>
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

          {mensagem && (
            <div className={`mensagem ${mensagem.tipo}`}>
              {mensagem.texto}
            </div>
          )}
        </section>
      </main>
    </div>
  )
}

export default Cadastro