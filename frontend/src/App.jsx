import { useState, useEffect } from 'react'
import './App.css'
import { lerSessao, salvarSessao, apagarSessao, quandoPerderSessao } from './api'
import Login from './Login'
import Painel from './Painel'

// Maior atraso aceito pelo setTimeout (~24 dias); acima disso ele dispara na hora
const MAIOR_ESPERA = 2 ** 31 - 1

// Decide entre a tela de login e o painel, conforme exista uma sessão válida
function App() {
  const [sessao, setSessao] = useState(lerSessao)
  const [aviso, setAviso] = useState(null)

  // A API recusou o token (expirou, segredo trocado...): volta para o login com o motivo
  useEffect(() => {
    quandoPerderSessao(mensagem => {
      setSessao(null)
      setAviso(mensagem)
    })
  }, [])

  // Desloga sozinho quando o token vence, mesmo sem nenhum clique
  useEffect(() => {
    if (!sessao) return
    const espera = Math.min(new Date(sessao.expiraEm) - Date.now(), MAIOR_ESPERA)
    const timer = setTimeout(() => {
      apagarSessao()
      setSessao(null)
      setAviso('Sua sessão expirou. Faça login novamente.')
    }, espera)
    return () => clearTimeout(timer)
  }, [sessao])

  const entrar = (novaSessao) => {
    salvarSessao(novaSessao)
    setSessao(novaSessao)
    setAviso(null)
  }

  const sair = () => {
    apagarSessao()
    setSessao(null)
    setAviso(null)
  }

  if (!sessao) return <Login onEntrar={entrar} aviso={aviso} />

  // key: trocar de usuário recria o painel do zero (sem dados da sessão anterior)
  return <Painel key={sessao.email} usuario={sessao.email} onSair={sair} />
}

export default App
