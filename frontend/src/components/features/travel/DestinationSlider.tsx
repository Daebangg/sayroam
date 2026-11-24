import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { useTranslation } from "react-i18next";
import { LazyMotion, domAnimation } from "motion/react";
import * as m from "motion/react-m";
import type { Swiper as SwiperInstance } from "swiper";
import { Autoplay, EffectFade } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import { Icon } from "@/components/ui";
import { DestinationPhoto } from "./DestinationPhoto";
import { destinationPhotos } from "@/data/destinationPhotos";
import { useTheme } from "@/hooks/useTheme";
import "swiper/css";
import "swiper/css/effect-fade";

const modules = [Autoplay, EffectFade];
const motionPreference = window.matchMedia("(prefers-reduced-motion: reduce)");
function subscribeToMotion(change: () => void) {
    motionPreference.addEventListener("change", change);
    return () => motionPreference.removeEventListener("change", change);
}
const getReducedMotion = () => motionPreference.matches;

export function DestinationSlider() {
    const { resolved } = useTheme();
    return <TravelPhotoSlides key={resolved} destinations={destinationPhotos[resolved]} />;
}

type TravelPhotoSlidesProps = {
    destinations: (typeof destinationPhotos)[keyof typeof destinationPhotos];
};

function TravelPhotoSlides({ destinations }: TravelPhotoSlidesProps) {
    const { t } = useTranslation();
    const reducedMotion = useSyncExternalStore(subscribeToMotion, getReducedMotion);
    const [paused, setPaused] = useState(false);
    const playing = !paused && !reducedMotion;
    const [active, setActive] = useState(0);
    const [settled, setSettled] = useState(0);
    const [firstLoaded, setFirstLoaded] = useState(false);
    const swiper = useRef<SwiperInstance | null>(null);
    const destination = destinations[active] ?? destinations[0];

    useEffect(() => {
        const autoplay = swiper.current?.autoplay;
        if (!autoplay || autoplay.running === playing) return;
        if (playing) autoplay.start();
        else autoplay.stop();
    }, [playing]);

    return (
        <>
            <div className="destination-slider" data-playing={playing} aria-hidden="true">
                <Swiper
                    modules={modules}
                    effect="fade"
                    fadeEffect={{ mode: "cross-fade" }}
                    speed={reducedMotion ? 0 : 1200}
                    autoplay={{ delay: 6000, disableOnInteraction: false }}
                    rewind
                    onSwiper={(instance) => {
                        swiper.current = instance;
                    }}
                    onSlideChange={(instance) => setActive(instance.realIndex)}
                    onSlideChangeTransitionEnd={(instance) => setSettled(instance.realIndex)}
                >
                    {destinations.map((place, index) => {
                        const adjacent =
                            index === (active + 1) % destinations.length ||
                            index === (active + destinations.length - 1) % destinations.length;
                        return (
                            <SwiperSlide key={place}>
                                {(index === active ||
                                    index === settled ||
                                    (adjacent && firstLoaded)) && (
                                    <DestinationPhoto
                                        place={place}
                                        zoom={active === index || settled === index}
                                        fetchPriority={index === active ? "high" : "low"}
                                        onError={() => setFirstLoaded(true)}
                                        onLoad={
                                            index === active && !firstLoaded
                                                ? () => setFirstLoaded(true)
                                                : undefined
                                        }
                                    />
                                )}
                            </SwiperSlide>
                        );
                    })}
                </Swiper>
            </div>
            <section
                className="destination-footer"
                aria-label={t("home.slider.label")}
                tabIndex={0}
                onFocus={() => setPaused(true)}
                onBlur={(event) => {
                    if (!event.currentTarget.contains(event.relatedTarget)) setPaused(false);
                }}
                onKeyDown={(event) => {
                    if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") return;
                    event.preventDefault();
                    if (event.key === "ArrowLeft") swiper.current?.slidePrev();
                    else swiper.current?.slideNext();
                }}
            >
                <div aria-live={playing ? "off" : "polite"} aria-atomic="true">
                    <LazyMotion features={domAnimation}>
                        <m.div
                            key={destination}
                            className="destination-caption"
                            initial={reducedMotion ? false : { opacity: 0, y: 5 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: reducedMotion ? 0 : 0.5 }}
                        >
                            <span className="destination-caption__city">
                                <Icon name="pin" variant="outline" />
                                {t(`home.slider.${destination}.city`)}
                            </span>
                            <h2>{t(`home.slider.${destination}.name`)}</h2>
                        </m.div>
                    </LazyMotion>
                </div>
            </section>
        </>
    );
}
