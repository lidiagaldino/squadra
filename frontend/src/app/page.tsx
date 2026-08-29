import Link from "next/link";

export default function HomePage() {
  return (
    <main className="public-page">
      <nav className="public-nav" aria-label="Public navigation">
        <Link className="public-brand" href="/">Squadra</Link>
        <div>
          <Link className="secondary-link" href="/login">Sign in</Link>
          <Link className="primary-link" href="/register">Join Squadra</Link>
        </div>
      </nav>

      <section className="public-hero" aria-labelledby="public-title">
        <div className="public-hero-copy">
          <p className="eyebrow">Women&apos;s football tournaments</p>
          <h1 id="public-title">Squadra</h1>
          <p className="public-lead">Your football story starts here. Play, organize and remember every tournament without losing the energy of match day.</p>
          <div className="public-actions">
            <Link className="primary-link" href="/register">Create your account</Link>
            <Link className="secondary-action" href="/login">Sign in</Link>
          </div>
        </div>

        <div className="pitch-preview" aria-label="Tournament preview">
          <div className="pitch-line" />
          <article className="fixture-card">
            <span>Next match</span>
            <strong>Estrelas FC vs Aurora</strong>
            <small>Sat • 10:30 • Court 2</small>
          </article>
          <article className="fixture-card organizer">
            <span>Organizer focus</span>
            <strong>6 registrations pending</strong>
            <small>Liga Miga closes in 2 days</small>
          </article>
        </div>
      </section>

      <section className="public-proof" aria-label="Squadra product areas">
        <article>
          <span>01</span>
          <h2>Athletes</h2>
          <p>Profiles carry position, team context, results and achievements.</p>
        </article>
        <article>
          <span>02</span>
          <h2>Teams</h2>
          <p>Rosters, invitations and tournament readiness stay in one sporting home.</p>
        </article>
        <article>
          <span>03</span>
          <h2>Organizers</h2>
          <p>Registrations, draws, schedules and results move without spreadsheets.</p>
        </article>
      </section>
    </main>
  );
}
