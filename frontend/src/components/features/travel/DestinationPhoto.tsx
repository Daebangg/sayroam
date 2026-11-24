import type { ComponentProps } from "react";
import type { destinationPhotos } from "@/data/destinationPhotos";

type DestinationPhotoProps = Pick<ComponentProps<"img">, "fetchPriority" | "onError" | "onLoad"> & {
    place: (typeof destinationPhotos)[keyof typeof destinationPhotos][number];
    zoom?: boolean;
};

export function DestinationPhoto({ place, zoom, ...imageProps }: DestinationPhotoProps) {
    const path = `/images/destinations/${place}`;
    return (
        <picture>
            <source media="(max-aspect-ratio: 3/4)" srcSet={`${path}-mobile.webp`} />
            <img
                src={`${path}.webp`}
                srcSet={`${path}.webp 1920w, ${path}-large.webp 3840w`}
                sizes="100vw"
                alt=""
                data-zoom={zoom}
                {...imageProps}
            />
        </picture>
    );
}
