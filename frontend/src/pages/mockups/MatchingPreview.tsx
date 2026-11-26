import { useState } from "react";
import { useLocation, useNavigate } from "react-router";
import {
    ACTIVE_RECRUITMENT_LIMIT,
    type MatchingConditions,
} from "@/hooks/api/matching/useCompanionMatching";
import type { MatchingRegistration } from "@/hooks/matching.types";
import { MatchingPage, type RecruitmentPreview } from "@/pages/matching/MatchingPage";
import { mockupUrl } from "@/routing/mockups";
import { RoamMatePreview } from "./RoamMatePreview";
import { roamMateCandidates } from "./roamMateCandidates";

const destinations = [
    { id: 1, name: "제주" },
    { id: 2, name: "부산" },
];
const defaultStyle = { planning: 2, "daily-pace": 2, "time-together": 4, spending: 3 };

function recruitment(
    id: number,
    conditions: MatchingConditions,
    status: MatchingRegistration["status"] = "ACTIVE",
): MatchingRegistration {
    return {
        id,
        updatedAt: new Date().toISOString(),
        destinationId: conditions.destinationId,
        startDate: conditions.startDate,
        endDate: conditions.endDate,
        title: conditions.title ?? null,
        introduction: conditions.introduction ?? null,
        status,
        legacyRegion: null,
        showAge: conditions.showAge ?? false,
        showGender: conditions.showGender ?? false,
        preferredGender: conditions.preferredGender ?? null,
        preferredMinAge: conditions.preferredMinAge ?? null,
        preferredMaxAge: conditions.preferredMaxAge ?? null,
        travelStyle: Object.fromEntries(
            Object.entries(defaultStyle).map(([code, fallback]) => [
                code,
                conditions.travelStyle?.[code as keyof typeof defaultStyle] ?? fallback,
            ]),
        ),
    };
}

const jejuPost = recruitment(1, {
    destinationId: 1,
    title: "제주, 바다 따라 천천히 걸어요",
    introduction: "함께 올레길을 걷고 작은 책방에도 들러요",
    startDate: "2026-12-18",
    endDate: "2026-12-21",
});
const busanPost = recruitment(2, {
    destinationId: 2,
    title: "부산 바닷가에서 함께 산책해요",
    introduction: "해 질 무렵 광안리를 같이 걸어요",
    startDate: "2026-12-18",
    endDate: "2026-12-21",
});

const closedPost = recruitment(
    3,
    {
        destinationId: 2,
        title: "영도 골목에서 사진 산책해요",
        introduction: "흰여울문화마을을 걷고 바다가 보이는 카페에 들러요",
        startDate: "2026-12-12",
        endDate: "2026-12-13",
    },
    "CLOSED",
);

// 목업을 나갔다 돌아와도 같은 탭에서 작성한 모집과 예시 결과를 확인할 수 있다.
let previewPosts = [jejuPost, busanPost, closedPost];

function candidatesFor(post: MatchingRegistration) {
    if (post.destinationId !== jejuPost.destinationId || post.status !== "ACTIVE") return [];
    return roamMateCandidates.flatMap((candidate) => {
        const start = post.startDate > candidate.startDate ? post.startDate : candidate.startDate;
        const end = post.endDate < candidate.endDate ? post.endDate : candidate.endDate;
        if (start > end) return [];
        return [
            {
                ...candidate,
                overlapStartDate: start,
                overlapEndDate: end,
                overlapDays: (Date.parse(end) - Date.parse(start)) / 86400000 + 1,
            },
        ];
    });
}

type Screen =
    "matching" | "matching-searching" | "matching-found" | "matching-empty" | "matching-delayed";

export function MatchingPreview({ screen }: { screen: Screen }) {
    const navigate = useNavigate();
    const location = useLocation();
    const [searches, setSearches] = useState(previewPosts);
    const limitReached =
        searches.filter((post) => post.status === "ACTIVE").length >= ACTIVE_RECRUITMENT_LIMIT;
    function update(posts: MatchingRegistration[]) {
        previewPosts = posts;
        setSearches(posts);
    }
    function status(id: number, next: MatchingRegistration["status"]) {
        if (next === "ACTIVE" && limitReached) return Promise.resolve(false);
        update(
            searches.map((post) =>
                post.id === id && post.status !== next
                    ? { ...post, status: next, updatedAt: new Date().toISOString() }
                    : post,
            ),
        );
        return Promise.resolve(true);
    }
    function open(screen: Screen, searchId: number, replace = false) {
        void navigate(mockupUrl(screen), { replace, state: { fromMatchingMain: true, searchId } });
    }
    if (screen === "matching") {
        const preview: RecruitmentPreview = {
            data: {
                searches,
                destinations,
                limitReached,
                canCreate: !limitReached,
                catalogLoading: false,
                catalogFailed: false,
                busy: false,
                failed: false,
                travelStyle: Object.entries(defaultStyle).map(([code, score]) => ({ code, score })),
                errorKey: "matchingFlow.saveError",
                getFailureReason: () =>
                    limitReached ? "matchingFlow.activeLimit" : "matchingFlow.saveError",
                reload: () => {},
                save: (body, id) => {
                    if (id === undefined && limitReached) return Promise.resolve(undefined);
                    const post = recruitment(
                        id ?? Math.max(0, ...searches.map((value) => value.id)) + 1,
                        body,
                    );
                    update([post, ...searches.filter((value) => value.id !== post.id)]);
                    return Promise.resolve(post);
                },
                close: (id) => status(id, "CLOSED"),
                resume: (id) => status(id, "ACTIVE"),
            },
            matchedIds: new Set(
                searches.filter((post) => candidatesFor(post).length > 0).map((post) => post.id),
            ),
            openResults: (id) => open("matching-found", id),
            startDiscovery: (id, origin) => open("matching-searching", id, origin !== "resume"),
        };
        return <MatchingPage preview={preview} />;
    }
    const sample =
        screen === "matching-empty" || screen === "matching-delayed" ? busanPost : jejuPost;
    const params = new URLSearchParams(location.search);
    const searchId = Number(params.get("searchId")) || location.state?.searchId || sample.id;
    const post = searches.find((post) => post.id === searchId) ?? sample;
    const candidates = candidatesFor(post);
    async function goToList(edit: boolean) {
        await navigate(mockupUrl("matching"), { replace: true, state: { matchingView: "mine" } });
        if (edit)
            await navigate(mockupUrl("matching"), {
                state: { matchingView: "find", editId: post.id },
            });
    }
    return (
        <RoamMatePreview
            key={location.key}
            screen={screen}
            requestId={Number(params.get("requestId"))}
            searchId={post.id}
            candidates={candidates}
            recruitmentTitle={post.title}
            onCloseRecruitment={() => {
                void status(post.id, "CLOSED").then(() => goToList(false));
            }}
            onEdit={() => void goToList(true)}
            onKeepRecruiting={() => void goToList(false)}
        />
    );
}
