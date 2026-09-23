"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/features/auth/auth-context";

export function TeamShell({ children }: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const { user, logout } = useAuth();

  const signOut = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <main className="home-shell team-app-shell">
      <aside className="home-rail" aria-label="Navegação do Squadra">
        <Link className="brand-mark" href="/home" aria-label="Início do Squadra">S</Link>
        <nav>
          <Link href="/home">Início</Link>
          <Link href={user?.role === "ORGANIZER" ? "/organizer/profile" : "/athlete/profile"}>Perfil</Link>
          <Link aria-current="page" href="/teams">Times</Link>
          {user?.role === "ORGANIZER" && <Link href="/tournaments">Torneios</Link>}
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sair</button>
      </aside>
      <section className="home-content team-content">{children}</section>
    </main>
  );
}
