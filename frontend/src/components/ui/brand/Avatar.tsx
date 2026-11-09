import { useState } from "react";
import type { AvatarPreset } from "./avatarPresets";

type AvatarProps = {
    preset?: AvatarPreset;
    src?: string | null;
    alt?: string;
    className?: string;
    size?: "small" | "medium" | "large";
};

function AvatarImage({
    preset = "coast",
    src,
    alt = "",
    className = "",
    size = "medium",
}: AvatarProps) {
    const [imageFailed, setImageFailed] = useState(false);
    const imageUrl = src?.trim();
    const showImage = imageUrl && !imageFailed;

    return (
        <span className={`ui-avatar ui-avatar--${size} ${className}`}>
            <img
                src={showImage ? imageUrl : `/brand/avatars/${preset}.svg`}
                alt={alt}
                decoding="async"
                onError={showImage ? () => setImageFailed(true) : undefined}
            />
        </span>
    );
}

export function Avatar(props: AvatarProps) {
    // 새 이미지 주소에는 이전 주소의 로드 실패 상태를 이어받지 않는다.
    return <AvatarImage key={props.src ?? ""} {...props} />;
}
