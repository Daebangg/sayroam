type TravelArtworkProps = {
    scene?: "coast" | "city" | "mountain" | "island";
    className?: string;
};

export function TravelArtwork({ scene = "mountain", className = "" }: TravelArtworkProps) {
    if (scene === "coast")
        return (
            <img className={`travel-artwork ${className}`} src="/images/travel-coast.jpg" alt="" />
        );
    return (
        <svg
            className={`travel-artwork ${className}`}
            viewBox="0 0 320 200"
            preserveAspectRatio="xMidYMid slice"
            aria-hidden="true"
        >
            <rect width="320" height="200" fill="#dcecff" />
            <circle cx="249" cy="51" r="35" fill="#ffdda5" />
            <path d="M249 16a35 35 0 0 1 0 70Z" fill="#f5c978" />
            <path d="m0 0 182 0-74 108H0Z" fill="#b3cff2" opacity=".45" />
            <path d="m172 0 148 0v134L106 111Z" fill="#7ca9df" opacity=".35" />
            <path d="M0 96Q35 50 75 96Q116 23 163 96Z" fill="#f8faff" />
            <path d="M0 142Q100 81 177 142T320 122V200H0Z" fill="#8fc1f5" />
            <path d="m0 172 138-64 182 67v25H0Z" fill="#426fa9" opacity=".35" />
            {scene === "city" ? (
                <>
                    <path
                        d="M155 17h10l7 66 15 53 34 48h-33l-28-41-28 41H99l34-48 15-53Z"
                        fill="#2866b3"
                    />
                    <path d="M135 115h50m-58 25h67m-51-55h34" stroke="#f3f8ff" strokeWidth="5" />
                    <path d="M0 186h320v14H0Z" fill="#4a94d9" />
                </>
            ) : scene === "island" ? (
                <>
                    <path d="M0 155Q124 110 320 170v30H0Z" fill="#f7d9b5" />
                    <path d="M117 180q24-57 10-102" fill="none" stroke="#27658e" strokeWidth="8" />
                    <path
                        d="M129 85Q59 72 59 119q35-24 70-34m0 0q34-66 68-20-35 0-68 20m0 0q67-12 72 33-41-31-72-33m0 0Q89 40 68 68q31 0 61 17"
                        fill="#398ba7"
                    />
                    <path d="M206 167q24-10 55 0" fill="none" stroke="#fff8ed" strokeWidth="5" />
                </>
            ) : (
                <>
                    <path d="M0 178 94 68l76 87 54-83 96 112v16H0Z" fill="#4e94e5" />
                    <path
                        d="m56 113 38-45 39 45-23-8-16 14-17-14Zm139 3 29-44 35 42-22-6-13 12-15-13Z"
                        fill="#f6f9ff"
                    />
                    <path d="M0 193 78 157l61 23 65-37 116 50v7H0Z" fill="#235c9a" />
                </>
            )}
        </svg>
    );
}
