// URL da API Java (pode ser trocada com VITE_API_URL no .env do frontend)
const API = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

// ---------- Sessão (token do login, guardado no navegador) ----------

const CHAVE_SESSAO = 'dataserver.sessao'

// Sessão salva: { token, email, nome, expiraEm }. Devolve null se não houver ou se já expirou.
// nome pode ser null (usuário sem nome) ou ausente (sessão salva antes do campo existir).
export function lerSessao() {
  try {
    const sessao = JSON.parse(localStorage.getItem(CHAVE_SESSAO))
    if (!sessao?.token || new Date(sessao.expiraEm) <= new Date()) return null
    return sessao
  } catch {
    return null
  }
}

export function salvarSessao({ token, email, nome = null, expiraEm }) {
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify({ token, email, nome, expiraEm }))
}

export function apagarSessao() {
  localStorage.removeItem(CHAVE_SESSAO)
}

// Chamado quando a API recusa o token (expirou ou é inválido); o App volta para o login
let aoPerderSessao = () => {}
export function quandoPerderSessao(callback) {
  aoPerderSessao = callback
}

// ---------- Requisições ----------

// fetch que envia o token e lança um Error com a mensagem da API quando a resposta não é 2xx
// (formato da API: {"status": 400, "erro": "...", "campos": {"campo": "mensagem"}})
export async function requisitar(caminho, opcoes = {}) {
  const sessao = lerSessao()
  const headers = { ...opcoes.headers }
  if (sessao) headers.Authorization = `Bearer ${sessao.token}`

  let resposta
  try {
    resposta = await fetch(`${API}${caminho}`, { ...opcoes, headers })
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
    const mensagem = detalhes ? `${titulo}: ${detalhes}` : titulo
    // 401 com token enviado = sessão perdida. (No login, sem token, o 401 é só senha errada.)
    if (resposta.status === 401 && sessao) {
      apagarSessao()
      aoPerderSessao(mensagem)
    }
    throw new Error(mensagem)
  }
  return corpo
}
