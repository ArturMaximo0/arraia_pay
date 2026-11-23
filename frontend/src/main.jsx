import React, { useCallback, useEffect, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { Html5Qrcode } from "html5-qrcode";
import "./styles.css";

async function api(path, options = {}) {
  const token = sessionStorage.getItem("arraipay_token");
  const response = await fetch(path, {
    credentials: "include",
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { "Authorization": `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });
  const body = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(body?.mensagem || body?.message || `Falha na requisicao (${response.status}).`);
    error.status = response.status;
    throw error;
  }
  return body;
}

async function apiImage(path) {
  const token = sessionStorage.getItem("arraipay_token");
  const response = await fetch(path, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.mensagem || body?.message || `Falha na requisicao (${response.status}).`);
  }
  return response.blob();
}

const money = value => Number(value || 0).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const onlyDigits = value => String(value || "").replace(/\D/g, "");
function maskCpf(value) {
  const digits = onlyDigits(value).slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}
function maskPhone(value) {
  const digits = onlyDigits(value).slice(0, 11);
  if (digits.length <= 2) return digits;
  if (digits.length <= 6) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`;
  if (digits.length <= 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
}

function Login({ onLogin }) {
  const [login, setLogin] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setErro("");
    setCarregando(true);
    try {
      const usuario = await api("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ login, senha })
      });
      sessionStorage.setItem("arraipay_token", usuario.token);
      onLogin(usuario);
    } catch (error) {
      setErro(error.message);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <main className="login-page">
      <div className="login-art">
        <span className="eyebrow">Festa boa, pagamento simples</span>
        <img className="login-logo" src="/logo-arraipay.png" alt="ArraiPay" />
        <p>Uma carteira digital para deixar a festa junina mais leve.</p>
        <div className="sun">✳</div>
      </div>
      <form className="login-card" onSubmit={submit}>
        <span className="eyebrow">Acesso da equipe</span>
        <h2>Bem-vindo!</h2>
        <p>Entre para continuar no painel.</p>
        <label>Usuario<input autoComplete="username" value={login} onChange={event => setLogin(event.target.value)} required /></label>
        <label>Senha<input type="password" autoComplete="current-password" value={senha} onChange={event => setSenha(event.target.value)} required /></label>
        {erro && <p className="notice error">{erro}</p>}
        <button className="primary" disabled={carregando}>{carregando ? "Entrando..." : "Entrar"}</button>
        <small>Demo: admin/admin123 · caixa/caixa123 · barraca/barraca123</small>
      </form>
    </main>
  );
}

function CardLookup({ card, onCard }) {
  const [codigo, setCodigo] = useState("");
  const [erro, setErro] = useState("");
  const [busy, setBusy] = useState(false);
  const [cameraAberta, setCameraAberta] = useState(false);
  const scannerRef = useRef(null);
  const scannerElementRef = useRef(null);

  useEffect(() => () => {
    if (scannerRef.current) {
      scannerRef.current.stop().catch(() => {});
      scannerRef.current.clear().catch(() => {});
    }
  }, []);

  async function validarCodigo(valor) {
    setBusy(true);
    setErro("");
    try {
      const resultado = await api("/api/v1/cartoes/validacoes", {
        method: "POST",
        body: JSON.stringify({ codigoQr: valor.trim() })
      });
      const saldo = await api(`/api/v1/cartoes/${resultado.idCartao}/saldo`);
      setCodigo(valor);
      onCard({ ...resultado, ...saldo });
    } catch (error) {
      onCard(null);
      setErro(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function validar(event) {
    event.preventDefault();
    await validarCodigo(codigo);
  }

  async function abrirCamera() {
    setErro("");
    if (!window.isSecureContext) {
      setErro("A camera precisa ser usada por HTTPS ou localhost.");
      return;
    }
    try {
      const cameras = await Html5Qrcode.getCameras();
      if (!cameras.length) throw new Error("Nenhuma camera encontrada neste aparelho.");
      const scanner = new Html5Qrcode("qr-reader");
      scannerRef.current = scanner;
      setCameraAberta(true);
      await scanner.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 240, height: 240 } },
        async decoded => {
          await scanner.stop().catch(() => {});
          scannerRef.current = null;
          setCameraAberta(false);
          await validarCodigo(decoded);
        },
        () => {}
      );
    } catch (error) {
      setCameraAberta(false);
      setErro(error.message || "Nao foi possivel abrir a camera.");
    }
  }

  async function fecharCamera() {
    if (scannerRef.current) {
      await scannerRef.current.stop().catch(() => {});
      scannerRef.current = null;
    }
    setCameraAberta(false);
  }

  return (
    <>
      <form className="inline-form" onSubmit={validar}>
        <label className="grow">Codigo do cartao<input placeholder="Leia ou digite o codigo QR" value={codigo} onChange={event => setCodigo(event.target.value)} required /></label>
        <button type="button" className="secondary camera-button" onClick={cameraAberta ? fecharCamera : abrirCamera}>{cameraAberta ? "Fechar camera" : "📷 Ler QR"}</button>
        <button className="secondary" disabled={busy}>{busy ? "Validando..." : "Validar cartao"}</button>
      </form>
      <div id="qr-reader" ref={scannerElementRef} className={cameraAberta ? "qr-reader open" : "qr-reader"} />
      {erro && <p className="notice error">{erro}</p>}
      {card && <div className="customer-banner"><div><span className="muted">Cartao de</span><strong>{card.nomeCliente}</strong></div><div><span className="muted">Saldo atual</span><strong>{money(card.saldoAtual)}</strong></div><span className="badge">{card.status}</span></div>}
    </>
  );
}

function Caixa() {
  const [card, setCard] = useState(null);
  const [valor, setValor] = useState("");
  const [forma, setForma] = useState("DINHEIRO");
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!card?.idCartao) return undefined;
    const intervalo = window.setInterval(async () => {
      try {
        const saldo = await api(`/api/v1/cartoes/${card.idCartao}/saldo`);
        setCard(atual => atual ? { ...atual, ...saldo } : atual);
      } catch (_) {}
    }, 15000);
    return () => window.clearInterval(intervalo);
  }, [card?.idCartao]);

  async function recarregar(event) {
    event.preventDefault();
    setErro("");
    setMensagem("");
    setBusy(true);
    try {
      const resultado = await api("/api/v1/recargas", {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
        body: JSON.stringify({ idCartao: card.idCartao, valor: Number(valor), formaPagamento: forma })
      });
      setMensagem(`Recarga de ${money(resultado.valor)} realizada para ${resultado.nomeCliente}.`);
      setValor("");
      const saldo = await api(`/api/v1/cartoes/${card.idCartao}/saldo`);
      setCard({ ...card, ...saldo });
    } catch (error) {
      setErro(error.message);
    } finally {
      setBusy(false);
    }
  }

  return <section className="workspace">
    <div className="page-heading"><div><span className="eyebrow">Operacao de caixa</span><h1>Adicionar saldo</h1><p>Valide o cartao e registre uma recarga.</p></div><span className="heading-icon">＋</span></div>
    <div className="panel"><h2>1. Identifique o cliente</h2><CardLookup card={card} onCard={setCard} /></div>
    {card && <form className="panel" onSubmit={recarregar}><h2>2. Registrar pagamento</h2><div className="form-grid">
      <label>Valor da recarga<input type="number" min="0.01" step="0.01" value={valor} onChange={event => setValor(event.target.value)} required /></label>
      <label>Forma de pagamento<select value={forma} onChange={event => setForma(event.target.value)}><option value="DINHEIRO">Dinheiro</option><option value="PIX">Pix</option><option value="CARTAO">Cartao</option></select></label>
    </div>{erro && <p className="notice error">{erro}</p>}{mensagem && <p className="notice success">{mensagem}</p>}<button className="primary" disabled={busy}>{busy ? "Processando..." : "Confirmar recarga"}</button></form>}
  </section>;
}

function Barraca({ usuario }) {
  const [card, setCard] = useState(null);
  const [valor, setValor] = useState("");
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!card?.idCartao) return undefined;
    const intervalo = window.setInterval(async () => {
      try {
        const saldo = await api(`/api/v1/cartoes/${card.idCartao}/saldo`);
        setCard(atual => atual ? { ...atual, ...saldo } : atual);
      } catch (_) {}
    }, 15000);
    return () => window.clearInterval(intervalo);
  }, [card?.idCartao]);

  async function vender(event) {
    event.preventDefault();
    setErro("");
    setMensagem("");
    setBusy(true);
    try {
      const resultado = await api("/api/v1/vendas/valor", {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
        body: JSON.stringify({
          idCartao: card.idCartao,
          idBarraca: usuario.barracaId,
          valor: Number(valor)
        })
      });
      setMensagem(`Venda ${resultado.idVenda} concluida: ${money(resultado.valorTotal)}.`);
      setValor("");
      const saldo = await api(`/api/v1/cartoes/${card.idCartao}/saldo`);
      setCard({ ...card, ...saldo });
    } catch (error) {
      setErro(error.message);
    } finally {
      setBusy(false);
    }
  }

  return <section className="workspace">
    <div className="page-heading"><div><span className="eyebrow">Ponto de venda</span><h1>Registrar venda</h1><p>Informe o valor total e finalize com o cartao do cliente.</p></div><span className="heading-icon">✦</span></div>
    <div className="panel"><h2>1. Identifique o cliente</h2><CardLookup card={card} onCard={setCard} /></div>
    <form className="panel" onSubmit={vender}>
      <h2>2. Informe o valor da compra</h2>
      <label>Valor da venda<input type="number" min="0.01" step="0.01" value={valor} onChange={event => setValor(event.target.value)} required /></label>
      {erro && <p className="notice error">{erro}</p>}{mensagem && <p className="notice success">{mensagem}</p>}
      <div className="checkout"><span>Total <strong>{money(valor)}</strong></span><button className="primary" disabled={!card || !valor || busy}>{busy ? "Processando..." : "Finalizar venda"}</button></div>
    </form>
  </section>;
}

function Admin() {
  const [aba, setAba] = useState("visao");
  const [dados, setDados] = useState(null);
  const [cartoes, setCartoes] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [recargas, setRecargas] = useState([]);
  const [vendas, setVendas] = useState([]);
  const [erro, setErro] = useState("");
  const [atualizando, setAtualizando] = useState(false);
  const [nomeCliente, setNomeCliente] = useState("");
  const [documentoCliente, setDocumentoCliente] = useState("");
  const [telefoneCliente, setTelefoneCliente] = useState("");
  const [clienteSelecionado, setClienteSelecionado] = useState("");
  const [mensagemCadastro, setMensagemCadastro] = useState("");
  const [clienteEditando, setClienteEditando] = useState(null);
  const [qrCartao, setQrCartao] = useState(null);
  const [qrImagemUrl, setQrImagemUrl] = useState("");
  const [carregandoQr, setCarregandoQr] = useState(false);
  const [erroQr, setErroQr] = useState("");
  const qrRequest = useRef(0);
  const [mostrarLimpezaHistorico, setMostrarLimpezaHistorico] = useState(false);
  const [senhaAdmin, setSenhaAdmin] = useState("");
  const [limpandoHistorico, setLimpandoHistorico] = useState(false);
  const [erroLimpezaHistorico, setErroLimpezaHistorico] = useState("");

  useEffect(() => {
    return () => {
      if (qrImagemUrl) URL.revokeObjectURL(qrImagemUrl);
    };
  }, [qrImagemUrl]);

  const atualizar = useCallback(async () => {
    setAtualizando(true);
    setErro("");
    try {
      const [dashboard, listaCartoes, listaClientes, listaRecargas, listaVendas] = await Promise.all([
        api("/api/v1/relatorios/dashboard"),
        api("/api/v1/cartoes"),
        api("/api/v1/clientes"),
        api("/api/v1/recargas"),
        api("/api/v1/vendas")
      ]);
      setDados(dashboard);
      setCartoes(listaCartoes);
      setClientes(Array.isArray(listaClientes) ? listaClientes : (listaClientes.content || []));
      setRecargas(listaRecargas);
      setVendas(listaVendas);
    } catch (error) {
      setErro(error.message);
    } finally {
      setAtualizando(false);
    }
  }, []);

  useEffect(() => {
    atualizar();
    const intervalo = window.setInterval(atualizar, 5000);
    return () => window.clearInterval(intervalo);
  }, [atualizar]);

  async function cadastrarCliente(event) {
    event.preventDefault();
    setErro("");
    setMensagemCadastro("");
    try {
      await api("/api/v1/clientes", {
        method: "POST",
        body: JSON.stringify({
          nome: nomeCliente,
          documento: documentoCliente || null,
          telefone: telefoneCliente || null,
          ativo: true
        })
      });
      setNomeCliente("");
      setDocumentoCliente("");
      setTelefoneCliente("");
      setMensagemCadastro("Cliente cadastrado com sucesso.");
      await atualizar();
    } catch (error) {
      setErro(error.message);
    }
  }

  async function emitirCartao(event) {
    event.preventEvent?.();
    event.preventDefault();
    setErro("");
    setMensagemCadastro("");
    try {
      await api("/api/v1/cartoes", {
        method: "POST",
        body: JSON.stringify({ idCliente: Number(clienteSelecionado) })
      });
      setClienteSelecionado("");
      setMensagemCadastro("Cartao emitido com sucesso.");
      await atualizar();
    } catch (error) {
      setErro(error.message);
    }
  }

  async function salvarEdicaoCliente(event) {
    event.preventDefault();
    setErro("");
    setMensagemCadastro("");
    try {
      await api(`/api/v1/clientes/${clienteEditando.idCliente}`, {
        method: "PATCH",
        body: JSON.stringify({
          nome: clienteEditando.nome,
          documento: maskCpf(clienteEditando.documento),
          telefone: maskPhone(clienteEditando.telefone),
          ativo: clienteEditando.ativo
        })
      });
      setClienteEditando(null);
      setMensagemCadastro("Cliente atualizado com sucesso.");
      await atualizar();
    } catch (error) {
      setErro(error.message);
    }
  }

  async function arquivarCliente(cliente) {
    if (!window.confirm(`Excluir ${cliente.nome}? O cadastro será arquivado; saldo e operações serão preservados e identificados como cliente excluído.`)) return;
    setErro("");
    setMensagemCadastro("");
    try {
      await api(`/api/v1/clientes/${cliente.idCliente}`, { method: "DELETE" });
      setMensagemCadastro("Cliente arquivado. O histórico foi preservado e identificado.");
      await atualizar();
    } catch (error) {
      setErro(error.message);
    }
  }

  async function restaurarCliente(cliente) {
    setErro("");
    setMensagemCadastro("");
    try {
      await api(`/api/v1/clientes/${cliente.idCliente}`, {
        method: "PATCH",
        body: JSON.stringify({ ativo: true })
      });
      setMensagemCadastro("Cliente restaurado.");
      await atualizar();
    } catch (error) {
      setErro(error.message);
    }
  }

  async function limparHistorico(event) {
    event.preventDefault();
    setLimpandoHistorico(true);
    setErroLimpezaHistorico("");
    try {
      const resultado = await api("/api/v1/admin/historico/limpar", {
        method: "POST",
        body: JSON.stringify({ senhaAdmin })
      });
      setMostrarLimpezaHistorico(false);
      setSenhaAdmin("");
      setMensagemCadastro(resultado.mensagem);
      await atualizar();
    } catch (error) {
      setErroLimpezaHistorico(error.message);
    } finally {
      setLimpandoHistorico(false);
    }
  }

  async function mostrarQr(cartao) {
    const requestId = ++qrRequest.current;
    setQrCartao(cartao);
    setQrImagemUrl("");
    setErroQr("");
    setCarregandoQr(true);
    setErro("");
    try {
      const imagem = await apiImage(`/api/v1/cartoes/${cartao.idCartao}/qrcode`);
      if (imagem.type !== "image/png" || imagem.size === 0) {
        throw new Error("O servidor nao retornou uma imagem QR valida.");
      }
      if (requestId === qrRequest.current) setQrImagemUrl(URL.createObjectURL(imagem));
    } catch (error) {
      if (requestId === qrRequest.current) {
        setErroQr(error.message);
      }
    } finally {
      if (requestId === qrRequest.current) setCarregandoQr(false);
    }
  }

  function fecharQr() {
    qrRequest.current++;
    setQrCartao(null);
    setQrImagemUrl("");
    setCarregandoQr(false);
    setErroQr("");
  }

  return <section className="workspace">
    <div className="page-heading"><div><span className="eyebrow">Visao geral</span><h1>Painel administrativo</h1><p>Acompanhe os resultados e atualize os dados da festa.</p></div><button className="secondary" onClick={atualizar} disabled={atualizando}>{atualizando ? "Atualizando..." : "↻ Atualizar"}</button></div>
    <nav className="admin-tabs" aria-label="Paginas administrativas">
      <button type="button" className={aba === "visao" ? "active" : ""} onClick={() => setAba("visao")}>Visao geral</button>
      <button type="button" className={aba === "clientes" ? "active" : ""} onClick={() => setAba("clientes")}>Clientes e saldos</button>
    </nav>
    {erro && <p className="notice error">{erro}</p>}
    {mensagemCadastro && <p className="notice success">{mensagemCadastro}</p>}
    {aba === "clientes" ? <>
    <div className="data-grid">
      <form className="panel" onSubmit={cadastrarCliente}>
        <h2>Cadastrar cliente</h2>
        <div className="form-grid">
          <label>Nome<input value={nomeCliente} onChange={event => setNomeCliente(event.target.value)} required /></label>
          <label>CPF<input inputMode="numeric" autoComplete="off" placeholder="000.000.000-00" value={documentoCliente} onChange={event => setDocumentoCliente(maskCpf(event.target.value))} required /></label>
          <label>Telefone<input inputMode="tel" autoComplete="tel" placeholder="(00) 00000-0000" value={telefoneCliente} onChange={event => setTelefoneCliente(maskPhone(event.target.value))} required /></label>
        </div>
        <button className="primary">Cadastrar cliente</button>
      </form>
      <form className="panel" onSubmit={emitirCartao}>
        <h2>Emitir cartao</h2>
        <label>Cliente<select value={clienteSelecionado} onChange={event => setClienteSelecionado(event.target.value)} required><option value="">Selecione um cliente</option>{clientes.map(cliente => {
          const possuiCartao = cartoes.some(cartao => cartao.idCliente === cliente.idCliente && cartao.status === "ATIVO");
          return <option key={cliente.idCliente} value={cliente.idCliente} disabled={!cliente.ativo || possuiCartao}>{cliente.nome}{!cliente.ativo ? " - excluido" : possuiCartao ? " - cartao ativo" : ""}</option>;
        })}</select></label>
        <button className="primary" style={{marginTop: "18px"}} disabled={!clienteSelecionado}>Emitir cartao</button>
      </form>
    </div>
    <div className="panel">
      <div className="section-title"><h2>Clientes e saldos</h2><span className="badge">{clientes.length}</span></div>
      {clientes.length ? <div className="table-wrap"><table><thead><tr><th>Cliente</th><th>Status</th><th>Documento</th><th>Telefone</th><th>Saldo</th><th>Cartao</th><th>Acoes</th></tr></thead><tbody>{clientes.map(cliente => {
        const cartao = cartoes.find(item => item.idCliente === cliente.idCliente);
        return <tr key={cliente.idCliente} className={cliente.ativo ? "" : "archived-row"}><td>{cliente.nome}</td><td><span className={`badge ${cliente.ativo ? "" : "inactive"}`}>{cliente.ativo ? "Ativo" : "Excluido"}</span></td><td>{cliente.documento || "—"}</td><td>{cliente.telefone || "—"}</td><td><strong>{money(cliente.saldoAtual)}</strong></td><td>{cartao ? <span className="qr-code-cell"><code>{cartao.codigoQr}</code><button className="table-action" type="button" onClick={() => mostrarQr(cartao)}>Ver QR</button></span> : "Sem cartao"}</td><td><button className="table-action" type="button" onClick={() => setClienteEditando({ ...cliente })}>Editar</button>{cliente.ativo ? <button className="table-action destructive-action" type="button" onClick={() => arquivarCliente(cliente)}>Excluir</button> : <button className="table-action" type="button" onClick={() => restaurarCliente(cliente)}>Restaurar</button>}</td></tr>;
      })}</tbody></table></div> : <p className="muted">Nenhum cliente cadastrado.</p>}
    </div>
    </> : <>
    <div className="stats-grid">
      <article className="stat"><span>Recargas realizadas</span><strong>{money(dados?.totalRecargas)}</strong><small>{dados?.quantidadeRecargas ?? "—"} operacoes</small></article>
      <article className="stat"><span>Total vendido</span><strong>{money(dados?.totalVendas)}</strong><small>{dados?.quantidadeVendas ?? "—"} vendas</small></article>
      <article className="stat"><span>Cartoes emitidos</span><strong>{cartoes.length}</strong><small>Cartoes cadastrados</small></article>
    </div>
    <div className="data-grid">
      <div className="panel"><div className="section-title"><h2>Cartoes para teste</h2><span className="badge">{cartoes.length}</span></div>{cartoes.length ? <div className="table-wrap"><table><thead><tr><th>Cliente</th><th>Codigo QR</th><th>Status</th></tr></thead><tbody>{cartoes.map(c => <tr key={c.idCartao}><td>{c.nomeCliente}</td><td className="qr-code-cell"><code>{c.codigoQr}</code><button className="table-action" type="button" onClick={() => mostrarQr(c)}>Ver QR</button></td><td><span className="badge">{c.status}</span></td></tr>)}</tbody></table></div> : <p className="muted">Nenhum cartao cadastrado.</p>}</div>
      <div className="panel"><h2>Atividade recente</h2><div className="activity-list">{[...recargas.map(r => ({ ...r, tipo: "Recarga", valorExibido: r.valor })), ...vendas.map(v => ({ ...v, tipo: "Venda", valorExibido: v.valorTotal }))].sort((a, b) => b.dataHora.localeCompare(a.dataHora)).slice(0, 8).map((item, index) => <div className={`activity ${item.clienteAtivo === false ? "archived-activity" : ""}`} key={`${item.tipo}-${item.idRecarga || item.idVenda || index}`}><span className={`activity-icon ${item.tipo.toLowerCase()}`}>{item.tipo === "Venda" ? "↗" : "＋"}</span><div><strong>{item.tipo} · {item.nomeCliente}</strong>{item.clienteAtivo === false && <span className="badge inactive">Cliente excluido</span>}<small>{new Date(item.dataHora).toLocaleString("pt-BR")}</small></div><b>{money(item.valorExibido)}</b></div>)}</div></div>
    </div>
    <div className="panel history-maintenance"><div><h2>Histórico de operações</h2><p>Remove recargas, vendas, itens e movimentações do sistema. Cadastros e saldos atuais dos clientes não serão alterados.</p></div><button className="danger-button" type="button" onClick={() => { setErroLimpezaHistorico(""); setSenhaAdmin(""); setMostrarLimpezaHistorico(true); }}>Limpar histórico de todos</button></div>
    </>}
    {clienteEditando && <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) setClienteEditando(null); }}>
      <form className="modal-card" role="dialog" aria-modal="true" aria-labelledby="editar-cliente-titulo" onSubmit={salvarEdicaoCliente}>
        <div className="section-title"><h2 id="editar-cliente-titulo">Editar cliente</h2><button className="modal-close" type="button" aria-label="Fechar" onClick={() => setClienteEditando(null)}>×</button></div>
        <label>Nome<input value={clienteEditando.nome} onChange={event => setClienteEditando(atual => ({ ...atual, nome: event.target.value }))} required /></label>
        <label>CPF<input inputMode="numeric" autoComplete="off" value={clienteEditando.documento} onChange={event => setClienteEditando(atual => ({ ...atual, documento: maskCpf(event.target.value) }))} required /></label>
        <label>Telefone<input inputMode="tel" autoComplete="tel" value={clienteEditando.telefone} onChange={event => setClienteEditando(atual => ({ ...atual, telefone: maskPhone(event.target.value) }))} required /></label>
        <label className="checkbox-label">Cliente ativo<input type="checkbox" checked={clienteEditando.ativo} onChange={event => setClienteEditando(atual => ({ ...atual, ativo: event.target.checked }))} /></label>
        <div className="modal-actions"><button className="secondary" type="button" onClick={() => setClienteEditando(null)}>Cancelar</button><button className="primary">Salvar alteracoes</button></div>
      </form>
    </div>}
    {mostrarLimpezaHistorico && <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !limpandoHistorico) setMostrarLimpezaHistorico(false); }}>
      <form className="modal-card" role="dialog" aria-modal="true" aria-labelledby="limpar-historico-titulo" onSubmit={limparHistorico}>
        <div className="section-title"><h2 id="limpar-historico-titulo">Limpar histórico de todos</h2><button className="modal-close" type="button" aria-label="Fechar" disabled={limpandoHistorico} onClick={() => setMostrarLimpezaHistorico(false)}>×</button></div>
        <p>Esta ação apaga permanentemente recargas, vendas, itens e movimentações de todos os clientes. Os cadastros e saldos atuais serão preservados.</p>
        <label>Confirme sua senha de administrador<input type="password" autoComplete="current-password" value={senhaAdmin} onChange={event => setSenhaAdmin(event.target.value)} required /></label>
        {erroLimpezaHistorico && <p className="notice error">{erroLimpezaHistorico}</p>}
        <div className="modal-actions"><button className="secondary" type="button" disabled={limpandoHistorico} onClick={() => setMostrarLimpezaHistorico(false)}>Cancelar</button><button className="danger-button" disabled={limpandoHistorico || !senhaAdmin}>{limpandoHistorico ? "Apagando..." : "Apagar histórico"}</button></div>
      </form>
    </div>}
    {qrCartao && <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) fecharQr(); }}>
      <section className="modal-card qr-modal" role="dialog" aria-modal="true" aria-labelledby="qrcode-titulo">
        <div className="section-title"><h2 id="qrcode-titulo">QR Code do cartao</h2><button className="modal-close" type="button" aria-label="Fechar" onClick={fecharQr}>×</button></div>
        <p>{qrCartao.nomeCliente}</p>
        {carregandoQr && <p className="muted">Gerando e armazenando o QR Code...</p>}
        {qrImagemUrl && <img className="qr-image" src={qrImagemUrl} alt={`QR Code do cartao de ${qrCartao.nomeCliente}`} />}
        {erroQr && <div className="notice error qr-error"><p>{erroQr}</p><button className="secondary" type="button" onClick={() => mostrarQr(qrCartao)}>Tentar novamente</button></div>}
        <code>{qrCartao.codigoQr}</code>
      </section>
    </div>}
  </section>;
}

function App() {
  const [usuario, setUsuario] = useState(null);
  const [verificando, setVerificando] = useState(true);

  useEffect(() => {
    let ativo = true;
    const verificar = () => {
      api("/api/v1/auth/me")
        .then(dados => { if (ativo) setUsuario(dados); })
        .catch(() => { if (ativo) setUsuario(null); })
        .finally(() => { if (ativo) setVerificando(false); });
    };
    verificar();
    const intervalo = window.setInterval(verificar, 60000);
    const aoRetornar = () => { if (document.visibilityState === "visible") verificar(); };
    document.addEventListener("visibilitychange", aoRetornar);
    return () => {
      ativo = false;
      window.clearInterval(intervalo);
      document.removeEventListener("visibilitychange", aoRetornar);
    };
  }, []);

  async function sair() {
    try { await api("/api/v1/auth/logout", { method: "POST" }); }
    finally {
      sessionStorage.removeItem("arraipay_token");
      setUsuario(null);
    }
  }

  if (verificando) return <main className="loading">Carregando sua sessao...</main>;
  if (!usuario) return <Login onLogin={setUsuario} />;
  const area = usuario.perfil === "ADMINISTRADOR" ? "Administracao" : usuario.perfil === "CAIXA" ? "Caixa" : "Barraca";

  return <div className="app-shell">
    <header className="topbar"><a className="brand" href="#"><img className="brand-logo" src="/logo-arraipay.png" alt="ArraiPay" /></a><div className="user-area"><span className="live-dot" />{area}<span className="user-name">{usuario.nome}</span><button className="logout" onClick={sair}>Sair</button></div></header>
    <main>{usuario.perfil === "CAIXA" && <Caixa />}{usuario.perfil === "BARRACA" && <Barraca usuario={usuario} />}{usuario.perfil === "ADMINISTRADOR" && <Admin />}</main>
    <footer>ArraiPay <span>•</span> Sistema de pagamentos da festa</footer>
  </div>;
}

createRoot(document.getElementById("root")).render(<App />);
