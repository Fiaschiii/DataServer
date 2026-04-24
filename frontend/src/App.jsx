import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [produtos, setProdutos] = useState([])
  const [relatorio, setRelatorio] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('http://localhost:8080/produtos')
      .then(res => res.json())
      .then(data => setProdutos(data))

    fetch('http://localhost:8080/relatorio')
      .then(res => res.json())
      .then(data => setRelatorio(data))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <h2>Carregando...</h2>

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>DataServer</h1>

      <h2>Produtos</h2>
      <table border="1" cellPadding="8">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Preço</th>
          </tr>
        </thead>
        <tbody>
          {produtos.map(p => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.nome}</td>
              <td>R$ {p.preco.toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {relatorio && (
        <>
          <h2>Relatório (Python)</h2>
          <p>Total de produtos: {relatorio.total_produtos}</p>
          <p>Preço total: R$ {relatorio.preco_total.toFixed(2)}</p>
          <p>Preço médio: R$ {relatorio.preco_medio.toFixed(2)}</p>
          <p>Produto mais caro: {relatorio.produto_mais_caro.nome}</p>
        </>
      )}
    </div>
  )
}

export default App
