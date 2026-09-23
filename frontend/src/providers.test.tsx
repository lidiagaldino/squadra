import { render, screen } from "@testing-library/react";
import { useQueryClient } from "@tanstack/react-query";
import { describe, expect, it, vi } from "vitest";
import { Providers } from "./providers";

vi.mock("next-auth/react", () => ({
  SessionProvider: ({ children }: Readonly<{ children: React.ReactNode }>) => <>{children}</>,
  useSession: () => ({ data: null, update: vi.fn() }),
}));

function QueryClientConsumer() {
  const client = useQueryClient();
  return <span>{client.getDefaultOptions().queries?.retry === undefined ? "ready" : "configured"}</span>;
}

describe("Providers", () => {
  it("makes a query client available to the application tree", () => {
    render(<Providers><QueryClientConsumer /></Providers>);
    expect(screen.getByText("ready")).toBeInTheDocument();
  });
});
