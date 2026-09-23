"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { AthleteProfileForm } from "@/features/athlete-profile/form";
import { useAuth } from "@/features/auth/auth-context";

export default function AthleteProfilePage() {
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
          <Link aria-current="page" href="/athlete/profile">Perfil</Link>
          <Link href="/teams">Times</Link>
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sair</button>
      </aside>
      <section className="profile-page">
        <AthleteProfileForm />
      </section>
    </main>
  );
}
