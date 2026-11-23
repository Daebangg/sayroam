import { LivingCoast } from "@/components/ui";

type RoamDiscoveryLandscapeProps = { subtle?: boolean };

export function RoamDiscoveryLandscape({ subtle = false }: RoamDiscoveryLandscapeProps) {
    return (
        <div
            className={`roam-landscape${subtle ? " roam-landscape--subtle" : ""}`}
            aria-hidden="true"
        >
            <LivingCoast initialIntensity={1.15} animated={!subtle} />
        </div>
    );
}
