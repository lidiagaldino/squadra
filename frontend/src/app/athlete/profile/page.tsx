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
      <aside className="home-rail" aria-label="Squadra navigation">
        <Link className="brand-mark" href="/home" aria-label="Squadra Home">S</Link>
        <nav>
          <Link href="/home">Home</Link>
          <Link aria-current="page" href="/athlete/profile">Profile</Link>
          <Link href="/teams">Teams</Link>
        </nav>
        <button className="text-button" onClick={() => void signOut()}>Sign out</button>
      </aside>
      <section className="profile-page">
        <AthleteProfileForm />
      </section>
    </main>
  );
}
