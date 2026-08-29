import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import HomePage from "./page";

describe("HomePage", () => {
  it("offers account entry points", () => {
    render(<HomePage />);
    expect(screen.getByRole("heading", { name: "Squadra" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /create your account/i })).toHaveAttribute("href", "/register");
    expect(screen.getAllByRole("link", { name: /sign in/i })[0]).toHaveAttribute("href", "/login");
    expect(screen.getByText(/Estrelas FC vs Aurora/i)).toBeInTheDocument();
  });
});
