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
  { title: "6 registrations need review", detail: "Liga Miga 2026 closes registration in 2 days.", tone: "warning", action: "Review" },
  { title: "Payment issue in Adulto B", detail: "Two teams are confirmed but still awaiting fee settlement.", tone: "danger", action: "Open" },
  { title: "Draw can be generated", detail: "Sub-17 category has enough confirmed teams.", tone: "info", action: "Generate" },
];

const athleteAttention: AttentionItem[] = [
  { title: "Confirm your team invitation", detail: "Estrelas FC added you for Liga Miga 2026.", tone: "warning", action: "Respond" },
  { title: "Registration payment pending", detail: "Your tournament spot is held until Friday.", tone: "danger", action: "Pay" },
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
      <aside className="home-rail" aria-label="Squadra navigation">
        <Link className="brand-mark" href="/home" aria-label="Squadra Home">S</Link>
        <nav>
          <Link aria-current="page" href="/home">Home</Link>
          <Link href={user.role === "ATHLETE" ? "/athlete/profile" : "/organizer/profile"}>Profile</Link>
          <Link href="/teams">Teams</Link>
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sign out</button>
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
          <p className="eyebrow">Organizer Home</p>
          <h1>Good match day, {firstName}.</h1>
          <p>Three tournaments are moving. Start with the items that can block teams from playing.</p>
        </div>
        <div className="match-chip" aria-label="Next tournament activity">
          <span>Next</span>
          <strong>Adulto B draw</strong>
          <small>Today, 18:30</small>
        </div>
      </section>

      <section className="home-grid organizer-grid">
        <Panel title="Requires Attention" className="attention-panel">
          <AttentionList items={organizerAttention} />
        </Panel>

        <Panel title="Active Tournaments">
          <div className="tournament-list">
            <TournamentOperation name="Liga Miga 2026" status="OPEN" detail="36 teams • 6 pending registrations" progress="Registration" action="Manage" />
            <TournamentOperation name="Copa Delas SP" status="DRAFT" detail="4 categories • schedule missing" progress="Configuration" action="Configure" />
            <TournamentOperation name="Festival Sub-17" status="CONFIRMED" detail="12 teams • draw ready" progress="Draw" action="Generate draw" />
          </div>
        </Panel>

        <Panel title="Upcoming Activity" className="timeline-panel">
          <ol className="activity-list">
            <li><time>Today</time><span>Registration review window closes for Liga Miga.</span></li>
            <li><time>Tomorrow</time><span>Copa Delas category configuration should be completed.</span></li>
            <li><time>Sat</time><span>First match slots become available for scheduling.</span></li>
          </ol>
        </Panel>

        <section className="action-band" aria-labelledby="organizer-actions">
          <div>
            <p className="eyebrow">Next best actions</p>
            <h2 id="organizer-actions">Keep tournaments playable.</h2>
          </div>
          <div className="action-row">
            <Link className="primary-link" href="/home">Review registrations</Link>
            <Link className="secondary-action" href="/home">Create tournament</Link>
          </div>
        </section>
      </section>
    </HomeShell>
  );
}

function AthleteHome({ user, onLogout }: Readonly<{ user: AuthenticatedUser; onLogout(): Promise<void> }>) {
  const firstName = useDisplayName(user.email);
  return (
    <HomeShell user={user} onLogout={onLogout}>
      <section className="home-hero athlete-hero">
        <div>
          <p className="eyebrow">Athlete Home</p>
          <h1>{firstName}, your next match is set.</h1>
          <p>You are listed with Estrelas FC in Liga Miga 2026. Confirm the invite to lock your spot.</p>
        </div>
        <div className="score-card" aria-label="Next match">
          <span>Next match</span>
          <strong>Estrelas FC vs Aurora</strong>
          <small>Sat • 10:30 • Court 2</small>
        </div>
      </section>

      <section className="home-grid athlete-grid">
        <Panel title="Pending Actions" className="attention-panel">
          <AttentionList items={athleteAttention} />
        </Panel>

        <Panel title="My Tournaments">
          <article className="athlete-tournament">
            <div>
              <p className="status-pill">Liga Miga 2026</p>
              <h3>Estrelas FC • Intermediate</h3>
              <p>Group stage starts this weekend. Your team is 2nd on goal difference.</p>
            </div>
            <Link className="secondary-action" href="/home">View tournament</Link>
          </article>
        </Panel>

        <Panel title="Recent Result">
          <div className="result-board">
            <span>Estrelas FC</span><strong>3</strong><strong>1</strong><span>Vila Norte</span>
          </div>
          <p className="panel-note">You logged 1 assist and completed 82% of your passes.</p>
        </Panel>

        <section className="progress-band" aria-labelledby="athlete-progress">
          <div>
            <p className="eyebrow">Progress</p>
            <h2 id="athlete-progress">Building your football story.</h2>
          </div>
          <div className="badge-row" aria-label="Recent achievements">
            <span>First assist</span>
            <span>Clean sheet squad</span>
            <span>3 matches played</span>
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
  return <main className="home-state"><p className="eyebrow">Squadra</p><h1>Restoring your match context.</h1><p>Checking your secure session...</p></main>;
}

function SignedOutHome() {
  return <main className="home-state"><p className="eyebrow">Squadra</p><h1>Sign in to see your Home.</h1><p>Your tournaments, matches and next actions appear after authentication.</p><Link className="primary-link" href="/login">Sign in</Link></main>;
}

function useDisplayName(email: string) {
  return useMemo(() => {
    const localPart = email.split("@")[0] || "there";
    return localPart.split(/[._-]/)[0].replace(/^\w/, (letter) => letter.toUpperCase());
  }, [email]);
}
