const listeners = new Set<(path?: string) => void>();

export function subscribeBadGateway(listener: (path?: string) => void): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

export function reportBadGateway(path?: string): void {
    for (const listener of listeners) listener(path);
}
