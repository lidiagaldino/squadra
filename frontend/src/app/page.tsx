import Link from "next/link";
import { redirect } from "next/navigation";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/server/auth/options";

export default async function HomePage() {
  const session = await getServerSession(authOptions);
  if (session?.user) redirect("/home");

  return (
    <main className="public-page">
      <nav className="public-nav" aria-label="Navegação pública">
        <Link className="public-brand" href="/">Squadra</Link>
        <div>
          <Link className="secondary-link" href="/login">Entrar</Link>
          <Link className="primary-link" href="/register">Entrar no Squadra</Link>
        </div>
      </nav>

      <section className="public-hero" aria-labelledby="public-title">
        <div className="public-hero-copy">
          <p className="eyebrow">Futebol feminino</p>
          <h1 id="public-title">Squadra</h1>
          <p className="public-lead">Jogue. Compita. Pertença. Entre na plataforma feita para o futebol feminino amador.</p>
          <div className="public-actions">
            <Link className="primary-link" href="/register">Criar sua conta</Link>
            <Link className="secondary-action" href="/login">Entrar</Link>
          </div>
        </div>

        <div className="pitch-preview" aria-label="Prévia do torneio">
          <div className="pitch-line" />
          <article className="fixture-card">
            <span>Próximo jogo</span>
            <strong>City Valkyries vs Northside Utd</strong>
            <small>Sáb, 24 de out • 14:00</small>
          </article>
          <article className="fixture-card organizer">
            <span>Central de ações</span>
            <strong>12 inscrições pendentes</strong>
            <small>Summer Cup começa hoje</small>
          </article>
        </div>
      </section>

      <section className="public-proof" aria-label="Áreas do Squadra">
        <article>
          <span>01</span>
          <h2>Atletas</h2>
          <p>Acompanhe estatísticas, jogos e conquistas no futebol.</p>
        </article>
        <article>
          <span>02</span>
          <h2>Times</h2>
          <p>Gerencie elencos, aceite convites ou crie um novo clube.</p>
        </article>
        <article>
          <span>03</span>
          <h2>Organizadoras</h2>
          <p>Conduza cronogramas, inscrições e operações em dia de jogo.</p>
        </article>
      </section>
    </main>
  );
}
