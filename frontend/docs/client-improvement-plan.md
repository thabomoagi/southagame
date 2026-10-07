# Client-only improvement plan

This list tracks improvements that can be made without changing the backend API or adding paid services. The current UI language, routes, and gameplay purpose should remain recognizable.

## Completed

- Q&A result feedback now uses the actual score percentage and question total.
- 30 Seconds now has a short countdown, one-word-at-a-time play, score/progress, Pass and Got it controls, optional vibration feedback, and a score summary.
- 30 Seconds now uses the existing score-round and complete-game API endpoints when identifiers are available.
- Fixed the 30 Seconds singular `/result` navigation typo by using `/results`.
- Profile pictures are resized to a maximum 512px dimension and strictly compressed below 90 KB before upload.
- Temporary avatar object URLs are revoked after image decoding.
- Bottom navigation accounts for mobile safe-area insets.
- Browser zoom is no longer disabled.
- Shared API handling now supports non-2xx responses, Spring-style `ProblemDetail`, empty/non-JSON responses, timeouts, and network failures.
- Protected routes no longer accept an expired access token when no refresh token is available.
- Non-primary avatars load lazily and decode asynchronously.
- Large immutable game payloads use Svelte 5 raw state to avoid unnecessary deep proxies.
- Buttons have a shared 48px minimum touch target, overscroll is contained, and reduced-motion preferences are respected.
- The PWA now has a precached offline navigation fallback.

## Auth and session hardening

- Use the existing session validation and refresh flow from the route guard before rendering protected screens.
- Keep the current sign-in UI and API contract; move tokens to secure HTTP-only cookies only when the backend contract is ready.
- Show a recoverable offline/session-expired state instead of immediately losing the current route.

## Backend-dependent items

- Cookie authentication cannot be enabled safely from this frontend alone. The current app uses bearer tokens, so switching to secure HttpOnly cookies requires Spring Boot `Set-Cookie`, CORS credentials, CSRF, and refresh-token changes together.
- OpenAPI-generated TypeScript types require an OpenAPI document or generated DTO package from the Spring Boot service. The current API module still has a few untyped response boundaries and should be typed when that contract is available.
- SvelteKit form actions are not a drop-in replacement for the current direct external API calls. They would require server-side proxy endpoints or moving mutations behind SvelteKit.

## Mobile reliability

- Add shared content padding for every screen that can appear behind the fixed bottom navigation.
- Test short-height phones and keyboard-open forms; top-align overflowing forms while retaining centered layouts on tall screens.
- Add `aria-busy` to loading regions and disable controls while requests are in flight.
- Preserve game state when a browser tab is backgrounded, or clearly end the round when timing cannot be trusted.
- Add reduced-motion handling for animated spinners and transitions.

## Performance and storage

- Lazy-load below-the-fold avatars and decode them asynchronously.
- Cache non-sensitive profile display data only for the current session.
- Defer or remove cosmetic IP geolocation on the profile screen.
- Keep client-side image validation before decoding and reject files that cannot meet the storage limit.

## Browser game polish

- Keep the one-word Heads Up-style interaction, but add an optional muteable Web Audio cue using browser APIs only.
- Add keyboard shortcuts as an enhancement without removing touch controls.
- Prevent accidental double actions near the end of the timer.
- Test vibration as optional feedback only; the game must remain fully usable without it.

## Verification checklist

- Run `npm run check` and `npm run build` after client changes.
- Test at 320px wide, 375px wide, and short-height mobile viewports.
- Test light/dark themes, keyboard-open forms, offline API failures, and an avatar larger than 10 MB.
- Verify that avatar upload output is WebP, no larger than 512px on either side, and below 90 KB.
