export const avatarPresets = [
    "coast",
    "sand",
    "dusk",
    "sea",
    "sunset",
    "twilight",
    "night",
    "flight",
    "traveler",
    "sunrise",
    "poet",
    "explorer",
    "alien",
    "dog",
    "rabbit",
    "cat",
] as const;

export type AvatarPreset = (typeof avatarPresets)[number];
