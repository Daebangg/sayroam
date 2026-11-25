import type { CandidateResult } from "@/hooks/matching.types";

type Candidate = CandidateResult["candidates"][number];

const similarStyle: NonNullable<Candidate["travelStyleMatch"]> = {
    score: 93.75,
    category: "SIMILAR",
    items: [
        {
            code: "planning",
            myScore: 2,
            candidateScore: 2,
            difference: 0,
            similarity: "SAME",
        },
        {
            code: "daily-pace",
            myScore: 2,
            candidateScore: 2,
            difference: 0,
            similarity: "SAME",
        },
        {
            code: "time-together",
            myScore: 4,
            candidateScore: 3,
            difference: 1,
            similarity: "SIMILAR",
        },
        {
            code: "spending",
            myScore: 3,
            candidateScore: 3,
            difference: 0,
            similarity: "SAME",
        },
    ],
};

const styleToDiscuss: NonNullable<Candidate["travelStyleMatch"]> = {
    score: 87.5,
    category: "NEEDS_DISCUSSION",
    items: [
        {
            code: "planning",
            myScore: 2,
            candidateScore: 2,
            difference: 0,
            similarity: "SAME",
        },
        {
            code: "daily-pace",
            myScore: 2,
            candidateScore: 4,
            difference: 2,
            similarity: "SOMEWHAT_DIFFERENT",
        },
        {
            code: "time-together",
            myScore: 4,
            candidateScore: 4,
            difference: 0,
            similarity: "SAME",
        },
        {
            code: "spending",
            myScore: 3,
            candidateScore: 3,
            difference: 0,
            similarity: "SAME",
        },
    ],
};

const multipleDifferences: NonNullable<Candidate["travelStyleMatch"]> = {
    category: "NEEDS_DISCUSSION",
    score: 68.75,
    items: styleToDiscuss.items.map((item) =>
        item.code === "planning"
            ? { ...item, candidateScore: 5, difference: 3, similarity: "DIFFERENT" }
            : item,
    ),
};

const featuredCandidates: Candidate[] = [
    {
        searchId: 31,
        updatedAt: "2026-12-11T15:30:00Z",
        nickname: "모래",
        profileImageUrl: "/brand/avatars/explorer.svg",
        age: 27,
        gender: "FEMALE",
        title: "제주 동쪽, 바다 보며 걸어요",
        introduction: "아침엔 올레길을 걷고 오후엔 작은 책방에 들르고 싶어요.",
        startDate: "2026-12-17",
        endDate: "2026-12-21",
        overlapStartDate: "2026-12-18",
        overlapEndDate: "2026-12-21",
        overlapDays: 4,
        travelStyleMatch: similarStyle,
    },
    {
        searchId: 42,
        updatedAt: "2026-12-12T15:30:00Z",
        nickname: "파도",
        profileImageUrl: "/brand/avatars/traveler.svg",
        age: null,
        gender: null,
        title: "성산에서 시작하는 오름 산책",
        introduction:
            "아침에는 성산에서 일출을 보고 해안길을 따라 천천히 걸으려고 해요. 작은 책방이나 조용한 카페를 만나면 잠시 쉬어 가고, 점심은 시장에서 각자 먹고 싶은 걸 골라 나눠요. 오후에는 날씨를 보고 오름에 가거나 바닷가에서 사진을 찍어요. 혼자 보내고 싶은 시간도 존중하고, 걷는 속도나 출발 시간은 만나기 전에 편하게 이야기해요. 가볍게 인사부터 나눠도 좋아요.",
        startDate: "2026-12-19",
        endDate: "2026-12-23",
        overlapStartDate: "2026-12-19",
        overlapEndDate: "2026-12-21",
        overlapDays: 3,
        travelStyleMatch: styleToDiscuss,
    },
    {
        searchId: 53,
        updatedAt: "2026-12-13T15:30:00Z",
        nickname: "윤슬",
        profileImageUrl: "/brand/avatars/poet.svg",
        age: 29,
        gender: null,
        title: "김녕에서 함께 필름 사진 찍어요",
        introduction: "골목을 걷다가 해 질 무렵에는 해변에서 사진을 찍고 싶어요.",
        startDate: "2026-12-20",
        endDate: "2026-12-22",
        overlapStartDate: "2026-12-20",
        overlapEndDate: "2026-12-21",
        overlapDays: 2,
        travelStyleMatch: multipleDifferences,
    },
];

const morePosts = [
    [
        "솔",
        "세화 해변에서 아침 산책",
        "해가 뜨면 바닷가를 걷고 근처에서 따뜻한 국수를 먹어요.",
        "sunrise",
    ],
    ["감귤", "구좌 당근밭 옆 자전거길", "평탄한 길을 따라 자주 쉬면서 달리려고 해요.", "flight"],
    [
        "우도씨",
        "우도에서 보내는 느긋한 하루",
        "첫 배를 타고 들어가 땅콩 아이스크림을 먹으러 가요.",
        "sand",
    ],
    ["온유", "동백 보러 같이 갈 분", "겨울 동백이 핀 정원에서 서로 사진을 찍어줘요.", "cat"],
    ["마루", "김녕에서 월정리까지", "바닷길을 걷다가 마음에 드는 카페에서 쉬어요.", "sea"],
    [
        "단비",
        "비 오는 날엔 제주 책방",
        "작은 서점 두 곳을 골랐어요. 좋아하는 책 이야기도 나눠요.",
        "poet",
    ],
    ["보름", "민속오일장 점심 산책", "장날에 맞춰 만나서 제철 간식을 맛보고 싶어요.", "rabbit"],
    ["차차", "곶자왈 숲길 한 바퀴", "짧은 숲길을 천천히 걸을 분을 찾아요.", "explorer"],
    [
        "도담",
        "일몰을 기다리는 필름 산책",
        "해 질 무렵 포구에서 각자 좋아하는 풍경을 담아요.",
        "sunset",
    ],
    ["초록", "차밭에서 따뜻한 차 한 잔", "오후에는 차밭 주변을 가볍게 걸어요.", "coast"],
    ["해솔", "아침에 만나는 제주 돌담길", "조용한 골목을 따라 걷고 같이 아침을 먹어요.", "sunrise"],
    ["오후", "함덕에서 파도 소리 듣기", "바닷가에 앉아 쉬어 가는 일정이에요.", "dusk"],
    [
        "별하",
        "맑은 밤에 별 보러 가요",
        "저녁을 먹고 불빛이 적은 곳에서 밤하늘을 보고 싶어요.",
        "night",
    ],
    ["여울", "제주 도예 공방 나들이", "작은 그릇을 만들고 동네 전시도 둘러봐요.", "traveler"],
    ["루리", "떠나기 전 바다 앞 브런치", "마지막 날 오전에 여유롭게 밥 한 끼 나눠요.", "dog"],
] as const;

const sampleGenders = ["FEMALE", null, "MALE"] as const;

export const roamMateCandidates: CandidateResult["candidates"] = [
    ...featuredCandidates,
    ...morePosts.map(([nickname, title, introduction, avatar], index) => ({
        gender: sampleGenders[index % sampleGenders.length] ?? null,
        travelStyleMatch: index % 2 === 0 ? similarStyle : styleToDiscuss,
        searchId: 100 + index,
        updatedAt: "2026-12-14T15:30:00Z",
        nickname,
        title,
        introduction,
        profileImageUrl: `/brand/avatars/${avatar}.svg`,
        age: index % 3 === 0 ? null : 24 + index,
        startDate: "2026-12-21",
        endDate: "2026-12-23",
        overlapStartDate: "2026-12-21",
        overlapEndDate: "2026-12-21",
        overlapDays: 1,
    })),
];
