import type { ButtonHTMLAttributes, Ref } from "react";
import { Button } from "../buttons/Button";

type MenuItemProps = ButtonHTMLAttributes<HTMLButtonElement> & { ref?: Ref<HTMLButtonElement> };

export function MenuItem({ className = "", ...props }: MenuItemProps) {
    return <Button {...props} variant="plain" className={`ui-menu-item ${className}`} />;
}
