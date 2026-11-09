# Sayroam 브랜드 자산

시각 기준과 공용 UI 사용법은 [스타일 가이드북](../../STYLE_GUIDE.md)을 따릅니다.
브랜드 표기는 **Sayroam**이며 파랑·하늘색·모래색·네이비의 면 분할을 사용합니다.

| 이름                                         | 용도                              | PNG 크기 |
| -------------------------------------------- | --------------------------------- | -------- |
| `sayroam-logo`                               | 산 마크 + 워드마크, 밝은 배경용   | 1064×240 |
| `sayroam-logo-light`                         | 산 마크 + 워드마크, 어두운 배경용 | 1064×240 |
| `sayroam-wordmark`, `sayroam-wordmark-light` | 워드마크, 밝은/어두운 배경용      | 760×240  |
| `sayroam-mark`, `sayroam-mark-light`         | 산 심볼                           | 512×512  |
| `sayroam-app-icon`                           | 파란 배경 앱 아이콘               | 512×512  |
| `favicon`                                    | 투명 배경의 산 심볼 파비콘        | 64×64    |

동일한 이름의 SVG 원본과 PNG 내보내기를 제공합니다. 로고·심볼·워드마크 PNG는 투명 배경입니다.
파비콘도 산 심볼만 남긴 투명 배경이며 파란 배경판이 있는 앱 아이콘과 구분합니다.
#86 참고 이미지에 맞춰 산 심볼을 기하학적인 면으로 구성하고 산세리프 워드마크를 적용했습니다.
앱에서는 [기본 로고](sayroam-logo.svg)·[밝은 로고](sayroam-logo-light.svg)와 제공한 워드마크를
그대로 사용하고 일반 텍스트나 다른 글꼴로 다시 조합하지 않습니다.
로고·워드마크의 Arial Bold 기반 글자는 SVG 경로로 변환했으므로 실행 시 해당 글꼴 파일이 필요하지 않습니다.
브랜드 보기의 워드마크 예시도 굵은 Arial·Helvetica 계열 산세리프로 통일합니다.

![Sayroam 브랜드 아이콘과 선형 변형](icon-board.png)

[아이콘 보드 SVG](icon-board.svg)·[PNG](icon-board.png)에서 실제 25종의 브랜드/선형 변형을
비교할 수 있습니다. 앱에서는 [icons.svg](icons.svg)를 공용 `Icon`으로 표시합니다.

`home`, `trips`, `matching`, `rooms`, `profile`, `settings`, `grid`, `bell`, `search`,
`bookmark`, `plus`, `minus`, `arrow`, `chevron`, `close`, `sun`, `moon`, `mail`, `filter`,
`pin`, `guide`, `check`, `flame`, `plan`, `calendar`를 제공합니다.

기본 `variant="brand"`는 파랑·하늘색·모래색을 겹친 면을 표시합니다. 작은 메타 정보나 진한
버튼 안에서는 `variant="outline"`으로 같은 의미의 선형 아이콘을 선택합니다. 모든 아이콘은
24px 좌표계를 공유하고 선형 변형은 `currentColor`를 따릅니다. 여행 진입은 내비게이션과 같은
여행가방 `trips`, 날짜 정보는 `calendar`를 사용합니다. 검색·동행·설정만 다른 아이콘 라이브러리로
교체하지 않습니다. 단축키는 그림 아이콘 대신 공용 `KeyboardShortcut` 키캡으로 표시합니다.

## 기본 프로필 아바타

![Sayroam 기본 프로필 아바타 12종](avatar-board.png)

[아바타 보드 SVG](avatar-board.svg)·[PNG](avatar-board.png)는 아래 SVG 원본의 공용 사용 기준입니다.
보드는 1280×1114px의 4열 3행 구성이며 개별 자산은 256×256 좌표계의 SVG입니다. 원형 영역 바깥은 투명하고
별도 개별 PNG는 제공하지 않습니다.

| 계열            | 자산                                                                                                                         |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 추상 풍경 4종   | [coast](avatars/coast.svg), [sand](avatars/sand.svg), [dusk](avatars/dusk.svg), [sea](avatars/sea.svg)                       |
| 큐비즘 인물 4종 | [traveler](avatars/traveler.svg), [sunrise](avatars/sunrise.svg), [poet](avatars/poet.svg), [explorer](avatars/explorer.svg) |
| 캐릭터·동물 4종 | [alien](avatars/alien.svg), [dog](avatars/dog.svg), [rabbit](avatars/rabbit.svg), [cat](avatars/cat.svg)                     |

공용 `Avatar`에서 원형 크롭으로 사용하며 내비게이션·프로필·브랜드 보기의 기본 프로필을
같은 계열로 맞춥니다. 파랑·하늘색·모래색·네이비의 큰 색면을 유지하고 인물은 비대칭 면과
단순한 눈·코로 구성합니다. 인물은 사선·옆모습·자연스러운 3/4 방향으로 시선을 다양하게 하고,
캐릭터·동물은 정면을 기준으로 표현합니다. 기하학적 면과 작은 원형 크롭에서의 주요 실루엣을 유지하고 사진이 있는 사용자의
프로필을 임의로 덮어쓰지 않습니다. 상세 사용 원칙은 스타일 가이드북을 따릅니다.

## 로그인 제공자

Google·카카오·네이버 자산은 [공식 제공자 자산](providers/README.md)을 사용합니다.
제공자 로고의 공식 형태·색상을 브랜드 팔레트로 다시 그리지 않습니다. 이 디렉터리에는 재사용
자산만 저장하며 화면 검증 캡처는 PR에 첨부합니다.
