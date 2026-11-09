import { useLocation } from "react-router";
import { getMockupScreen } from "@/routing/mockups";
import { MockupPage } from "@/pages/mockups/MockupPage";
import { BrandPage } from "./BrandPage";

export function BrandRoute() {
    const { pathname, search } = useLocation();
    const screen = getMockupScreen(pathname, search);
    return screen ? <MockupPage screen={screen} /> : <BrandPage />;
}
