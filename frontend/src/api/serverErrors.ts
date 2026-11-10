const listeners = new Set<() => void>();

export function subscribeBadGateway(listener: () => void): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

export function reportBadGateway(): void {
    for (const listener of listeners) listener();
}
