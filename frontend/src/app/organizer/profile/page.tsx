"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { OrganizerProfileForm } from "@/features/organizer-profile/form";
import { useAuth } from "@/features/auth/auth-context";

export default function OrganizerProfilePage() {
  const { logout } = useAuth();
  const router = useRouter();
  const signOut = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <main className="home-shell profile-with-rail">
      <aside className="home-rail" aria-label="Navegação do Squadra">
        <Link className="brand-mark" href="/home" aria-label="Início do Squadra">S</Link>
        <nav>
          <Link href="/home">Início</Link>
          <Link aria-current="page" href="/organizer/profile">Perfil</Link>
          <Link href="/teams">Times</Link>
          <Link href="/tournaments">Torneios</Link>
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sair</button>
      </aside>
      <section className="profile-page">
        <OrganizerProfileForm />
      </section>
    </main>
  );
}
