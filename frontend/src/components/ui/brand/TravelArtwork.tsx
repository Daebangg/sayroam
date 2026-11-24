type TravelArtworkProps = {
    scene?: "coast" | "city" | "mountain" | "island";
    className?: string;
};

export function TravelArtwork({ scene = "mountain", className = "" }: TravelArtworkProps) {
    if (scene === "coast" || scene === "mountain")
        return (
            <img
                className={`travel-artwork ${className}`}
                src={scene === "coast" ? "/images/travel-coast.jpg" : "/images/travel-highland.jpg"}
                alt=""
            />
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
            <path d="M0 142Q100 81 177 142T320 122V200H0Z" fill="#88b7a0" />
            <path d="m0 172 138-64 182 67v25H0Z" fill="#426fa9" opacity=".35" />
            {scene === "city" ? (
                <>
                    <path
                        d="M0 148h21v-22h14v22h8v-36h17v36h12v-25h11v25h105v-23h20v23h10v-40h16v40h12v-29h18v29h56v52H0Z"
                        fill="#7492ae"
                    />
                    <path d="M0 157q85-13 163-1t157-6v50H0Z" fill="#4183b7" />
                    <g fill="none" stroke="#334e66">
                        <path d="M0 147H320" strokeWidth="5" />
                        <path
                            d="m0 146 23-20 24 20 23-20 24 20 23-20 24 20 23-20 24 20 23-20 24 20 23-20 24 20 23-20 15 13M23 126h282"
                            strokeWidth="3"
                        />
                    </g>
                    <path d="M45 149h5v18h-5zm94 0h5v15h-5zm94 0h5v19h-5z" fill="#526d80" />
                </>
            ) : (
                <>
                    <path
                        d="M0 145q50-20 90-12 31 7 65-10t73-10l20 16 32 9 40 5v13H0Z"
                        fill="#527f70"
                    />
                    <path d="M0 149q100 5 188-2t132 4v49H0Z" fill="#4e9ec4" />
                    <path
                        d="m0 164 35-12 29 5 22-12 36 9 25 15 31 4 21 16 121 11H0Z"
                        fill="#405862"
                    />
                    <path
                        d="m0 161 35-9 29 5-15 6Zm70-6 16-10 36 9-25 6Zm65 13 12 1 31 4-11 7Z"
                        fill="#68807d"
                    />
                    <path d="M0 180q85-21 171 3t149 8v9H0Z" fill="#efd6a7" />
                    <path
                        d="M175 164q34-5 63-2m18 4 28-2"
                        fill="none"
                        stroke="#d3eff1"
                        strokeWidth="3"
                        strokeLinecap="round"
                    />
                </>
            )}
        </svg>
    );
}
