"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";

export function TournamentShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const { logout, user } = useAuth();

  const signOut = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <main className="home-shell tournament-app-shell">
      <aside className="home-rail" aria-label="Navegação do Squadra">
          <Link className="brand-mark" href="/home" aria-label="Início do Squadra">S</Link>
        <nav>
          <Link href="/home">Início</Link>
          <Link href={user?.role === "ORGANIZER" ? "/organizer/profile" : "/athlete/profile"}>Perfil</Link>
          <Link href="/teams">Times</Link>
          <Link aria-current="page" href="/tournaments">Torneios</Link>
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sair</button>
      </aside>
      <section className="home-content tournament-content">{children}</section>
    </main>
  );
}
