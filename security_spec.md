# Security Specification – Noise Live

## 1. Data Invariants
- **Authentication Mandatory**: Zero unauthenticated reads or writes across all collections.
- **PII Isolation**: User profiles (`/users/{userId}`) store public handles and social metrics only; no emails, phone numbers, or physical addresses are stored.
- **Strict Ownership & Isolation**:
  - `/notifications/{notificationId}`: Only readable, updatable, or deletable by `resource.data.userId == request.auth.uid`.
  - `/coin_transactions/{txId}`: Append-only immutable ledger (`allow update, delete: if false`) scoped strictly to `userId == request.auth.uid`.
  - `/conversations/{conversationId}` and `/conversations/{conversationId}/messages/{messageId}`: Accessible only when `request.auth.uid in resource.data.participantIds`.
  - `/posts/{postId}`, `/stories/{storyId}`, `/party_rooms/{roomId}`: Creation requires `userId == request.auth.uid` (or `hostId == request.auth.uid`), and list queries require `appScope == 'noise_live_public'`.

## 2. Dirty Dozen Payloads
1. Unauthenticated read on `/posts` or `/notifications` -> Rejected.
2. Unauthenticated write on `/users/{uid}` -> Rejected.
3. Cross-user read on `/notifications/{id}` where `userId != request.auth.uid` -> Rejected.
4. Cross-user read on `/conversations/{id}` where ` !(request.auth.uid in participantIds)` -> Rejected.
5. Cross-user read on `/coin_transactions/{id}` where `userId != request.auth.uid` -> Rejected.
6. Shadow field injection (`isAdmin: true`) on `/users/{uid}` create or update -> Rejected by `hasOnly()`.
7. Identity spoofing (`userId: 'other_user'`) on `/posts/{postId}` create -> Rejected.
8. Immutable field tampering (`createdAt` or `userId` mutation) on `/posts/{postId}` update -> Rejected.
9. Ledger tampering (`update` or `delete` on `/coin_transactions/{txId}`) -> Rejected.
10. Unfiltered query (`db.collection("notifications").get()` without `where("userId", "==", uid)`) -> Rejected.
11. Oversized string payload (> 500 chars caption) -> Rejected by `isValidPost()`.
12. Invalid enum value (`mediaType: "executable"`) on `/posts/{postId}` -> Rejected.
