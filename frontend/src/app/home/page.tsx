"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth, type AuthenticatedUser } from "@/features/auth/auth-context";

type AttentionItem = {
  title: string;
  detail: string;
  tone: "warning" | "danger" | "info";
  action: string;
};

const organizerAttention: AttentionItem[] = [
  { title: "12 inscrições pendentes", detail: "Autumn League recebeu 4 novas inscrições de times hoje.", tone: "warning", action: "Revisar" },
  { title: "Check-in de arbitragem", detail: "Summer Cup precisa confirmar a arbitragem dos Campos A e B.", tone: "danger", action: "Abrir" },
  { title: "Jogos hoje", detail: "8 jogos estão agendados na linha do tempo dos torneios ativos.", tone: "info", action: "Ver" },
];

const athleteAttention: AttentionItem[] = [
  { title: "Próximo jogo", detail: "City Valkyries enfrenta Northside Utd no sábado, 24 de outubro, às 14:00.", tone: "warning", action: "Confirmar" },
  { title: "Conquista desbloqueada", detail: "Heroína do hat-trick no último jogo contra East Enders.", tone: "info", action: "Ver" },
];

export default function AuthenticatedHomePage() {
  const { user, refresh, logout } = useAuth();
  const [isRestoringSession, setIsRestoringSession] = useState(!user);

  useEffect(() => {
    if (user) return;

    refresh()
      .catch(() => undefined)
      .finally(() => setIsRestoringSession(false));
  }, [refresh, user]);

  if (isRestoringSession) return <HomeLoadingState />;
  if (!user) return <SignedOutHome />;
  return user.role === "ORGANIZER" ? <OrganizerHome user={user} onLogout={logout} /> : <AthleteHome user={user} onLogout={logout} />;
}

function HomeShell({ user, onLogout, children }: Readonly<{ user: AuthenticatedUser; onLogout(): Promise<void>; children: React.ReactNode }>) {
  const router = useRouter();
  const signOut = async () => {
    await onLogout();
    router.push("/login");
  };

  return (
    <main className="home-shell">
      <aside className="home-rail" aria-label="Navegação do Squadra">
        <Link className="brand-mark" href="/home" aria-label="Início do Squadra">S</Link>
        <nav>
          <Link aria-current="page" href="/home">Início</Link>
          <Link href={user.role === "ATHLETE" ? "/athlete/profile" : "/organizer/profile"}>Perfil</Link>
          <Link href="/teams">Times</Link>
          {user.role === "ORGANIZER" && <Link href="/tournaments">Torneios</Link>}
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sair</button>
      </aside>
      <section className="home-content">{children}</section>
    </main>
  );
}

function OrganizerHome({ user, onLogout }: Readonly<{ user: AuthenticatedUser; onLogout(): Promise<void> }>) {
  const firstName = useDisplayName(user.email);
  return (
    <HomeShell user={user} onLogout={onLogout}>
      <section className="home-hero organizer-hero">
        <div>
          <p className="eyebrow">Painel</p>
          <h1>Bom dia de jogo, {firstName}.</h1>
          <p>Acompanhe torneios ativos, inscrições pendentes e operações dos jogos de hoje em uma central de ações.</p>
        </div>
        <div className="match-chip" aria-label="Próxima atividade do torneio">
          <span>Torneios ativos</span>
          <strong>3</strong>
          <small>12 inscrições pendentes • 8 jogos hoje</small>
        </div>
      </section>

      <section className="home-grid organizer-grid">
        <Panel title="Precisa de Atenção" className="attention-panel">
          <AttentionList items={organizerAttention} />
        </Panel>

        <Panel title="Torneios Ativos">
          <div className="tournament-list">
            <TournamentOperation name="Summer Cup" status="ABERTO" detail="Início da fase de grupos • arbitragem necessária" progress="Hoje, 14:00" action="Gerenciar" />
            <TournamentOperation name="Autumn League" status="RASCUNHO" detail="Chamada final para inscrições de times" progress="Amanhã, 10:00" action="Revisar" />
            <TournamentOperation name="Winter Invitational" status="CONFIRMADO" detail="Tabela fechada para os jogos do fim de semana" progress="Jogos hoje" action="Ver" />
          </div>
        </Panel>

        <Panel title="Linha do Tempo da Gestão" className="timeline-panel">
          <ol className="activity-list">
            <li><time>Hoje, 14:00</time><span>Summer Cup - início da fase de grupos.</span></li>
            <li><time>Amanhã, 10:00</time><span>Prazo de inscrição: Autumn League.</span></li>
            <li><time>Sáb</time><span>Revisar preparação dos campos para os jogos.</span></li>
          </ol>
        </Panel>

        <section className="action-band" aria-labelledby="organizer-actions">
          <div>
            <p className="eyebrow">Próximas ações</p>
            <h2 id="organizer-actions">Central de Ações</h2>
          </div>
          <div className="action-row">
            <Link className="primary-link" href="/home">Ver tudo</Link>
            <Link className="secondary-action" href="/tournaments">Criar torneio</Link>
          </div>
        </section>
      </section>
    </HomeShell>
  );
}

function AthleteHome({ user, onLogout }: Readonly<{ user: AuthenticatedUser; onLogout(): Promise<void> }>) {
  return (
    <HomeShell user={user} onLogout={onLogout}>
      <section className="home-hero athlete-hero">
        <div>
          <p className="eyebrow">Área da atleta</p>
          <h1>Bem-vinda de volta, Elena</h1>
          <p>Próximo jogo em 2 dias. Hora de se preparar.</p>
        </div>
        <div className="score-card" aria-label="Próximo jogo">
          <span>Próximo jogo</span>
          <strong>City Valkyries vs Northside Utd</strong>
          <small>Sáb, 24 de out • 14:00</small>
        </div>
      </section>

      <section className="home-grid athlete-grid">
        <Panel title="Ações Pendentes" className="attention-panel">
          <AttentionList items={athleteAttention} />
        </Panel>

        <Panel title="Próximo Jogo">
          <article className="athlete-tournament">
            <div>
              <p className="status-pill">Sáb, 24 de out • 14:00</p>
              <h3>City Valkyries vs Northside Utd</h3>
              <p>Riverside Community Pitch.</p>
            </div>
            <Link className="secondary-action" href="/home">Confirmar</Link>
          </article>
        </Panel>

        <Panel title="Perfil da Atleta">
          <div className="result-board">
            <span>Elena R.</span><strong>12</strong><strong>8</strong><span>Gols / Assistências</span>
          </div>
          <p className="panel-note">Meio-campista • #8 • Forma recente V V E D V.</p>
        </Panel>

        <section className="progress-band" aria-labelledby="athlete-progress">
          <div>
            <p className="eyebrow">Progresso</p>
            <h2 id="athlete-progress">Feed de Atividades</h2>
          </div>
          <div className="badge-row" aria-label="Conquistas recentes">
            <span>Heroína do hat-trick</span>
            <span>Set_Pieces_Q4.pdf</span>
            <span>Summer 7s League</span>
          </div>
        </section>
      </section>
    </HomeShell>
  );
}

function Panel({ title, className, children }: Readonly<{ title: string; className?: string; children: React.ReactNode }>) {
  return (
    <section className={["home-panel", className].filter(Boolean).join(" ")} aria-labelledby={title.toLowerCase().replace(/\s+/g, "-")}>
      <h2 id={title.toLowerCase().replace(/\s+/g, "-")}>{title}</h2>
      {children}
    </section>
  );
}

function AttentionList({ items }: Readonly<{ items: AttentionItem[] }>) {
  return (
    <div className="attention-list">
      {items.map((item) => (
        <article className={`attention-item ${item.tone}`} key={item.title}>
          <div><h3>{item.title}</h3><p>{item.detail}</p></div>
          <button>{item.action}</button>
        </article>
      ))}
    </div>
  );
}

function TournamentOperation({ name, status, detail, progress, action }: Readonly<{ name: string; status: string; detail: string; progress: string; action: string }>) {
  return (
    <article className="tournament-operation">
      <div>
        <p className="status-pill">{status}</p>
        <h3>{name}</h3>
        <p>{detail}</p>
      </div>
      <div className="operation-step"><span>{progress}</span><button>{action}</button></div>
    </article>
  );
}

function HomeLoadingState() {
  return <main className="home-state"><p className="eyebrow">Squadra</p><h1>Restaurando seu contexto de jogo.</h1><p>Verificando sua sessão segura...</p></main>;
}

function SignedOutHome() {
  return <main className="home-state"><p className="eyebrow">Squadra</p><h1>Entre para ver seu início.</h1><p>Seus torneios, jogos e próximas ações aparecem depois da autenticação.</p><Link className="primary-link" href="/login">Entrar</Link></main>;
}

function useDisplayName(email: string) {
  return useMemo(() => {
    const localPart = email.split("@")[0] || "atleta";
    return localPart.split(/[._-]/)[0].replace(/^\w/, (letter) => letter.toUpperCase());
  }, [email]);
}
