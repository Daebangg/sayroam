# SayRoam 공통 이메일 템플릿 (#89)

회원가입, 비밀번호 재설정, 비밀번호 변경, 회원 탈퇴 본인 확인 메일에 같은 브랜드 레이아웃을 사용합니다.
확정한 산 심볼·SayRoam 워드마크와 큐비즘 해안 일러스트를 넣고, 목적별 문구와 코드를 실제 텍스트로 표시합니다.

## 로컬 미리보기

저장소 루트에서 실행합니다. Java 21이 필요하며 Spring 서버·DB·Redis·SMTP를 시작하거나 메일을 보내지 않습니다.

```bash
./backend/gradlew -p backend emailPreview
```

`backend/build/mail-preview/index.html`을 브라우저에서 엽니다. 인증 목적 4종과 코드 없이 버튼을 사용하는
여행 안내 예시를 한국어·영어로 각각 제공하며 일반 텍스트 본문도 확인할 수 있습니다. 코드 `012345`와 `https://example.test/trips`는
검토용 예시입니다. 생성 파일은 `build/`에만 두며 화면 캡처는 PR에 첨부합니다.

## 내부 요소 바꾸기

레이아웃은 `src/main/resources/mail/base.html`, 내용은 `BrandedEmailContent`가 담당합니다.
로고·여행 그림·바탕·푸터는 공통으로 유지하고 아래 필드를 바꿉니다.

| 필드 | 용도 |
| --- | --- |
| `language` | `EmailLanguage.KO` 또는 `EN`. HTML 언어·버튼 보조 문구·푸터에도 적용 |
| `subject` | 메일 제목. 렌더러가 `[SayRoam]`을 붙임 |
| `preheader` | 수신함에서 제목 옆에 보이는 짧은 요약 |
| `category` | 본문 제목 위의 작은 분류 문구 |
| `title`, `introduction` | 본문 제목과 첫 안내 |
| `details` | 추가 안내 문단. 필요 없으면 빈 목록 |
| `highlight` | 인증 코드 같은 강조 내용. 필요 없으면 `null` |
| `action` | 문구·HTTPS 주소를 가진 버튼과 복사 가능한 대체 링크. 필요 없으면 `null` |
| `notice` | 하단 주의·보조 안내 |

```java
var content = new BrandedEmailContent(
        EmailLanguage.KO, "여행 준비 안내", "다음 여행을 함께 준비해요.", "함께 준비하는 여행",
        "우리의 다음 여행이 기다려요", "준비한 일정을 확인해 주세요.",
        List.of("동행과 함께 가고 싶은 장소를 모아 보세요."),
        null,
        new BrandedEmailContent.Action("여행 계획 확인하기", URI.create("https://example.test/trips")),
        "이 안내는 여행 준비를 위해 보냈습니다.");
RenderedEmail rendered = renderer.render(content);
```

모든 내용은 일반 텍스트로 받습니다. HTML 태그를 전달해 스타일을 바꾸지 않습니다. 렌더러가 텍스트와
링크 속성을 이스케이프하고 한 번만 치환합니다. 버튼 주소는 사용자 정보가 없는 절대 HTTPS 주소만 받습니다.
`RenderedEmail`은 제목·일반 텍스트·HTML을 함께 제공하므로 새로운 발송 목적도 같은 레이아웃을 재사용할 수 있습니다.

## 브라우저 언어 선택

가입·재설정·계정 관리의 코드 발급/재발송 요청은 브라우저가 보내는 선택적 `Accept-Language` 헤더를 읽습니다.
서버 전역 로케일이나 이전 요청의 언어를 사용하지 않고 현재 요청의 언어를 발급 서비스와 발송 포트에 명시적으로 전달합니다.

| 헤더 예시 | 발송 언어 |
| --- | --- |
| `ko-KR,ko;q=0.9,en;q=0.8` | 한국어 |
| `en-US,en;q=0.9,ko;q=0.8` | 영어 |
| `fr-FR,en;q=0.8` | 지원 언어 중 다음 선호인 영어 |
| 헤더 없음·지원 언어 없음·잘못된 값 | 한국어 |

지역 태그는 기본 언어로 연결하고 `q=0`은 선택하지 않습니다. 제목·수신함 요약·본문·코드/시간 안내·주의사항·푸터·일반 텍스트·
HTML `lang`·MIME `Content-Language`에 같은 언어를 적용합니다. 문구는 `mail/messages_ko.properties`와
`mail/messages_en.properties`에 모아 두었습니다. 공통 양식의 구조를 복제하지 않고 번역을 수정할 수 있습니다.

현재 프론트 전송 계층은 이 헤더를 덮어쓰지 않으므로 브라우저 언어가 그대로 사용됩니다. 사이트 안의 언어 선택을
브라우저 설정과 다르게 사용한다면 메일은 브라우저 설정을 따릅니다. 이 선택은 API 입력 오류의 고정 영어 메시지 정책을 바꾸지 않습니다.

## 실제 인증 메일

`VerificationEmailTemplates`에서 목적별 문구를 조합하고 `SmtpVerificationEmailSender`가 전송합니다.
발송 포트·발급·코드 검증·제한·API 계약은 그대로 사용합니다.

| 목적 | 내용 | 유효시간 설정 |
| --- | --- | --- |
| 회원가입 | 이메일 인증 코드 | `app.auth.email-verification.code-ttl-seconds` |
| 비밀번호 재설정 | 재설정 전 본인 확인, 이메일 로그인 계정·소셜 로그인 안내 | `app.auth.password-reset.code-ttl-seconds` |
| 비밀번호 변경 | 변경 전 본인 확인 | `app.auth.account-verification.code-ttl-seconds` |
| 회원 탈퇴 | 탈퇴 전 본인 확인, 코드 확인만으로 탈퇴되지 않음 | `app.auth.account-verification.code-ttl-seconds` |

표시 시간은 각 설정값을 사용합니다. 코드 수신 시점부터의 보장 시간이 아니라 **발급 요청 시점부터의 최대 시간**으로
안내하며 실제 남은 시간은 인증 화면에서 확인하도록 합니다. 기본값은 모두 300초입니다.
비밀번호 재설정 메일은 실제 이메일 계정 존재 여부를 단정하지 않습니다.

MIME 본문은 `multipart/related` 안에 `multipart/alternative`(일반 텍스트·UTF-8 HTML)와 CID 이미지 두 개를 넣습니다.
이미지는 외부 URL로 요청하지 않고 메일에 포함합니다. `setText(plain, html)` 다음 `addInline`을 호출하는 순서는
[Spring MimeMessageHelper 지침](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/mail/javamail/MimeMessageHelper.html)을 따릅니다.
로고에는 대체 텍스트를, 장식 그림에는 빈 대체 텍스트를 사용합니다. 코드·목적·안내는 이미지가 차단되어도 남습니다.

기본 디자인은 600px 이내의 한 열 표 구조와 인라인 CSS를 사용합니다. 모바일에서는 여백·제목 크기를 줄입니다.
핵심 레이아웃에 외부 폰트·JavaScript·CSS 배경 그림·flex/grid를 사용하지 않습니다. 둥근 모서리와 미디어 쿼리를
지원하지 않는 클라이언트에서는 장식 일부가 달라도 본문을 읽을 수 있습니다. 어두운 메일 앱의 강제 색상 변환은
앱마다 다르므로 실제 Gmail·Outlook·Apple Mail 수신 검증과 구분합니다.

## 자산과 확인

`mail/assets/`는 백엔드 단독 JAR에서도 사용할 수 있도록 브랜드 PNG·여행 JPEG를 포함합니다.
원본·동기화 기준은 [자산 안내](../src/main/resources/mail/assets/README.md), 시각 기준은
[프론트 스타일 가이드북](../../frontend/STYLE_GUIDE.md)을 참고합니다.

```bash
./backend/gradlew -p backend test --tests '*EmailLanguageTests' --tests '*BrandedEmailRendererTests' --tests '*SmtpVerificationEmailSenderTests'
```

텍스트 이스케이프·링크 검증·선행 0·한글·목적별 시간과 문구·MIME 직렬화·CID 이미지·SMTP 실패 매핑을 확인합니다.
실제 HTTP 필터·컨트롤러·발급 서비스가 선택한 언어를 전달하는 흐름은 `AuthIntegrationTests`에서 검증합니다.
미리보기는 실제 렌더러와 목적별 문구를 사용하지만 브라우저 검토이며 메일 앱별 수신 결과를 보장하지 않습니다.
실제 외부 수신자에게 발송하지 않습니다.
