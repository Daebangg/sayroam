export function isValidEmail(value: string): boolean {
    const email = value.trim();
    const parts = email.split("@");
    return (
        email.length <= 254 &&
        parts.length === 2 &&
        /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+$/.test(parts[0]!) &&
        parts[0]!.length <= 64 &&
        !parts[0]!.startsWith(".") &&
        !parts[0]!.endsWith(".") &&
        !parts[0]!.includes("..") &&
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) &&
        parts[1]!
            .split(".")
            .every((label) => /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$/.test(label)) &&
        [...email].every((character) => character.charCodeAt(0) <= 127)
    );
}

export function isValidPassword(value: string): boolean {
    return value.length >= 8 && value.length <= 128 && value.trim().length > 0;
}

// 신규 비밀번호와 기존 비밀번호 로그인은 서로 다른 정책이다 (#85).
export function newPasswordRules(value: string): Readonly<{
    length: boolean;
    uppercase: boolean;
    number: boolean;
    special: boolean;
    noWhitespace: boolean;
}> {
    return {
        length: value.length >= 8 && value.length <= 24,
        uppercase: /[A-Z]/.test(value),
        number: /[0-9]/.test(value),
        special: /[\p{P}\p{S}]/u.test(value),
        noWhitespace: value.length > 0 && !/[\s\p{Z}\p{C}]/u.test(value),
    };
}

export function isValidNewPassword(value: string): boolean {
    return Object.values(newPasswordRules(value)).every(Boolean);
}

export function isValidNickname(value: string): boolean {
    const nickname = value.trim();
    return nickname.length >= 1 && nickname.length <= 50 && !/[\p{C}\p{Zl}\p{Zp}]/u.test(nickname);
}
