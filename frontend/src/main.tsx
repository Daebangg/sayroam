import { createRoot } from "react-dom/client";
import "./i18n";
import "./styles/app.css";
import { RouterProvider } from "react-router/dom";
import { router } from "@/routing/router";

createRoot(document.getElementById("root")!).render(<RouterProvider router={router} />);
