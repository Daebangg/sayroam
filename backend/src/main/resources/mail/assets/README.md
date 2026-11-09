# 이메일 브랜드 자산

이 폴더에는 메일 안에 CID로 포함할 재사용 자산만 둡니다. 화면 캡처·실제 메일·인증 정보는 두지 않습니다.

| 파일 | 원본 | SHA-256 |
| --- | --- | --- |
| `sayroam-logo.png` | `frontend/public/brand/sayroam-logo-on-light.png` (1600×376px) | `7d6e740a67e9a52448fbafbdffc8d7a4a9e7de5f59b036bf3dea1456d60e5323` |
| `travel-coast.jpg` | `frontend/public/images/travel-coast.jpg` (2172×724px) | `252e2708c321f6aaf15a5c8f101066478f306a06e458e8dd918df62bf718e662` |

원본은 #86 / PR #88의 확정 디자인이며 `d95d48be2b84e71025fd2ecc278adf176d54937d`를 기준으로 복사했습니다.
렌더러는 `sayroam-logo`, `sayroam-travel` Content-ID로 연결합니다. 로고는 밝은 배경을 포함한 PNG를
선택해 메일 클라이언트의 색상 변환 시에도 글자와 배경을 함께 유지하도록 했습니다.

브랜드가 바뀌면 해당 원본을 복사하고 이 표의 해시와 미리보기를 함께 확인합니다. 프론트 배포나
외부 호스트 접근 없이 단독 백엔드 JAR에서 동작하기 위해 사본을 포함합니다.
