import { useState } from 'react'
import { SignInIcon } from '@primer/octicons-react'
import { requisitar } from './api'

// Tela de login: envia e-mail e senha para POST /auth/login e entrega a sessão ao App
function Login({ onEntrar, aviso }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState(null)
  const [entrando, setEntrando] = useState(false)

  const entrar = (e) => {
    e.preventDefault() // o <form> não recarrega a página; Enter também envia
    if (!email.trim() || !senha) {
      setErro('Preencha e-mail e senha.')
      return
    }
    setEntrando(true)
    setErro(null)
    requisitar('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: email.trim(), senha }),
    })
      .then(onEntrar)
      .catch(e => {
        setErro(e.message)
        setSenha('')
        setEntrando(false)
      })
  }

  // O aviso do App (ex.: sessão expirada) some assim que houver um erro novo
  const mensagem = erro ?? aviso

  return (
    <div className="login-pagina">
      <form className="login-card" onSubmit={entrar} noValidate>
        <div className="logo login-logo">
          <span className="logo-icon">⬡</span>
          <span className="logo-text">DataServer</span>
        </div>
        <p className="login-subtitulo">Entre com seu e-mail e senha</p>

        {mensagem && <div className="alerta" role="alert">{mensagem}</div>}

        <label className="login-rotulo" htmlFor="login-email">E-mail</label>
        <input
          id="login-email"
          type="email"
          autoComplete="username"
          autoFocus
          value={email}
          onChange={e => setEmail(e.target.value)}
          className="input-field"
        />

        <label className="login-rotulo" htmlFor="login-senha">Senha</label>
        <input
          id="login-senha"
          type="password"
          autoComplete="current-password"
          value={senha}
          onChange={e => setSenha(e.target.value)}
          className="input-field"
        />

        <button type="submit" disabled={entrando} className="btn-cadastrar btn-icone login-botao">
          <SignInIcon className="icone anima-direita" />
          {entrando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}

export default Login
