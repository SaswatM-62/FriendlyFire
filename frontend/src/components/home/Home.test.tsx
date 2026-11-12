import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { Home } from "@/components/home/Home";
import { CodeInput } from "@/components/ui/CodeInput";
import { FAKE_ME } from "@/mocks/data";
import { router } from "@/test/navigation";
import { renderWithApp } from "@/test/render";
import { render } from "@testing-library/react";
import { useState } from "react";

describe("Home: start and join cards", () => {
  it("creating a room lands on the room page", async () => {
    renderWithApp(<Home me={FAKE_ME} />);

    await userEvent.click(screen.getByRole("button", { name: /Create room/ }));

    await expect.poll(() => router.push.mock.calls[0]?.[0]).toMatch(/^\/room\/[A-Z2-9]{6}$/);
  });

  it("a wrong code shows Room not found", async () => {
    renderWithApp(<Home me={FAKE_ME} />);

    await userEvent.type(screen.getByRole("textbox", { name: "Room code" }), "zzzz99");
    await userEvent.click(screen.getByRole("button", { name: "Join" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Room not found");
    expect(router.push).not.toHaveBeenCalled();
  });

  it("a full room says so", async () => {
    renderWithApp(<Home me={FAKE_ME} />);

    await userEvent.type(screen.getByRole("textbox", { name: "Room code" }), "PACKED");
    await userEvent.click(screen.getByRole("button", { name: "Join" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("That room is full");
  });

  it("joining a real room goes to it", async () => {
    renderWithApp(<Home me={FAKE_ME} />);

    await userEvent.type(screen.getByRole("textbox", { name: "Room code" }), "K7QM2X");
    await userEvent.click(screen.getByRole("button", { name: "Join" }));

    await expect.poll(() => router.push.mock.calls[0]?.[0]).toBe("/room/K7QM2X");
  });

  it("asks for all six characters before joining", async () => {
    renderWithApp(<Home me={FAKE_ME} />);

    await userEvent.type(screen.getByRole("textbox", { name: "Room code" }), "K7Q");
    await userEvent.click(screen.getByRole("button", { name: "Join" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Enter all 6 characters");
  });
});

describe("CodeInput", () => {
  function Harness() {
    const [code, setCode] = useState("");
    return <CodeInput value={code} onChange={setCode} />;
  }

  it("takes a pasted code in any case, with spaces or dashes", async () => {
    render(<Harness />);
    const input = screen.getByRole("textbox", { name: "Room code" });

    await userEvent.click(input);
    await userEvent.paste(" k7qm-2x ");

    expect(input).toHaveValue("K7QM2X");
  });
});
