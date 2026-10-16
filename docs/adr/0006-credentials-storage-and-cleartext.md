# 0006. Credentials in DataStore encrypted via Keystore; cleartext HTTP allowed

- Status: Accepted
- Date: 2026-10-16
- Deciders: Ayoub

## Context

Xtream credentials are a plain username/password that sit in every URL. Panels are overwhelmingly served over plain HTTP
on a custom port. The `mobile-quality-audit` skill asks for Keystore-backed secrets and TLS only; the second is impossible here.

## Options considered

1. **Preferences DataStore with the password encrypted by an AES-GCM key in the Android Keystore; allow cleartext for all hosts**: Pros: password unreadable in backups or file dumps; works with any provider. Cons: cleartext means the credentials travel unencrypted on the network, which is inherent to the protocol.
2. **EncryptedSharedPreferences (androidx.security)**: Pros: ready-made. Cons: deprecated, alpha for years.
3. **Plain DataStore**: Pros: trivial. Cons: password readable from a device backup.
4. **Cleartext only for the configured host**: Pros: tighter. Cons: the host is user-typed at runtime; network security config is static.

## Decision

We will encrypt the password with a Keystore key (`KeystoreSecretCodec`), keep the rest in DataStore, disable `allowBackup`,
and permit cleartext traffic app-wide with a comment explaining why.

## Consequences

- Positive: credentials at rest are protected; the API interceptor is the only place that writes them into a request.
- Negative: in transit they are as exposed as with any Xtream client. Logging redacts `username`/`password` query parameters.
- Follow-up: prefer `https` automatically when the panel announces an `https_port` in `server_info`.
