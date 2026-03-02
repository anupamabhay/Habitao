# Backup, Restore, and Data Sync Plan

## 1. Local Backup & Restore (Phase 1)
**Goal:** Allow users to manually export and import their data as a JSON file.
**Implementation:**
- **Export:** Create a `BackupManager` that queries all Room DAOs, serializes the data into a single JSON structure using `kotlinx.serialization`, and writes it to a file using the Android Storage Access Framework (SAF) `ACTION_CREATE_DOCUMENT`.
- **Import:** Use `ACTION_OPEN_DOCUMENT` to read the JSON file, deserialize it, clear the current Room database, and insert the imported data within a single Room transaction.

## 2. Cloud Backup (Phase 2)
**Goal:** Automatic daily backups to Google Drive.
**Implementation:**
- Integrate Google Sign-In and the Google Drive REST API.
- Use Android `WorkManager` to schedule a daily background worker.
- The worker generates the JSON backup and uploads it to the hidden App Data folder in the user's Google Drive (`drive.appdata` scope), ensuring privacy.

## 3. Real-time Sync (Phase 3)
**Goal:** Multi-device synchronization.
**Implementation:**
- **Backend:** Firebase Firestore or Supabase.
- **Data Model:** Add `updatedAt` (timestamp) and `isDeleted` (boolean/tombstone) fields to all Room entities.
- **Sync Logic:** Implement a Last-Write-Wins (LWW) conflict resolution strategy. When the app comes online, fetch all records from the backend where `updatedAt > lastSyncTime`. Push all local records where `updatedAt > lastSyncTime`.
