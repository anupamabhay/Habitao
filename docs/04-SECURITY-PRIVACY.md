# Security & Privacy Design
## Habitao - Security Architecture & Data Protection

**Version:** 1.0  
**Last Updated:** February 13, 2026  
**Compliance:** GDPR-Ready, Android Security Best Practices

---

## 1. Security Principles

### 1.1 Core Security Tenets
1. **Defense in Depth:** Multiple layers of security (encryption, permissions, obfuscation)
2. **Least Privilege:** Request only necessary permissions, scope narrowly
3. **Data Minimization:** Collect and store only essential data
4. **Secure by Default:** Security features enabled out-of-the-box
5. **Transparency:** Clear communication about data usage

### 1.2 Threat Model

| Threat | Risk Level | Mitigation |
|--------|------------|------------|
| **Local Data Theft** (device stolen) | HIGH | Encrypted storage (DataStore + Tink), Android Keystore |
| **Malware Scanning** (app memory) | MEDIUM | Obfuscation (R8 full mode), no sensitive data in memory logs |
| **Network Interception** (future cloud sync) | MEDIUM | HTTPS/TLS 1.3, certificate pinning |
| **Unauthorized Access** (no device lock) | HIGH | User education, optional app-level PIN (future) |
| **Data Leakage via Logs** | LOW | ProGuard removes logs in release builds |
| **Backup Interception** (ADB backup) | MEDIUM | `android:allowBackup="false"` or encrypted backups |

---

## 2. Data Encryption

### 2.1 Proto DataStore with Google Tink

**Status:** EncryptedSharedPreferences is **DEPRECATED** in 2026. Use Proto DataStore + Tink.

**Implementation:**

```kotlin
// Master key generation
object KeyManager {
    private const val KEYSET_NAME = "habitao_master_keyset"
    private const val PREFERENCE_FILE = "habitao_secure_prefs"
    private const val MASTER_KEY_URI = "android-keystore://habitao_master_key_2026"
    
    fun getOrCreateKeysetHandle(context: Context): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
    }
    
    fun getAead(context: Context): Aead {
        return getOrCreateKeysetHandle(context).getPrimitive(Aead::class.java)
    }
}

// Proto DataStore Serializer with encryption
object EncryptedAppSettingsSerializer : Serializer<AppSettings> {
    override val defaultValue: AppSettings = AppSettings.getDefaultInstance()
    
    private lateinit var aead: Aead
    
    fun initialize(context: Context) {
        aead = KeyManager.getAead(context)
    }
    
    override suspend fun readFrom(input: InputStream): AppSettings {
        return try {
            val encryptedBytes = input.readBytes()
            if (encryptedBytes.isEmpty()) return defaultValue
            
            val decryptedBytes = aead.decrypt(encryptedBytes, null)
            AppSettings.parseFrom(decryptedBytes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt settings")
            defaultValue
        }
    }
    
    override suspend fun writeTo(t: AppSettings, output: OutputStream) {
        val plainBytes = t.toByteArray()
        val encryptedBytes = aead.encrypt(plainBytes, null)
        output.write(encryptedBytes)
    }
}

// Usage
val Context.encryptedDataStore: DataStore<AppSettings> by dataStore(
    fileName = "app_settings.pb",
    serializer = EncryptedAppSettingsSerializer
)

class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.encryptedDataStore
    
    val settings: Flow<AppSettings> = dataStore.data
    
    suspend fun updateTheme(themeMode: ThemeMode) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setThemeMode(themeMode)
                .build()
        }
    }
}
```

### 2.2 Room Database Encryption

**Decision:** Room databases are NOT encrypted by default for performance.

**Justification:**
- Habit data is not highly sensitive (no passwords, financial data)
- Android's filesystem encryption is enabled by default on modern devices
- Encrypting Room significantly impacts query performance

**Alternative (if encryption required):**
```kotlin
// Use SQLCipher for Room encryption
val passphrase: ByteArray = getPassphraseFromKeystore()
val factory = SupportFactory(passphrase)

Room.databaseBuilder(context, HabitaoDatabase::class.java, "habitao.db")
    .openHelperFactory(factory)
    .build()
```

### 2.3 Sensitive Data Handling

**What Gets Encrypted:**
- ✅ App Settings (Proto DataStore + Tink)
- ✅ Future: User authentication tokens (if cloud sync implemented)
- ✅ Backup files (AES-256)

**What Stays Unencrypted:**
- ❌ Habit/Task/Routine data (relies on Android filesystem encryption)
- ❌ Logs (HabitLog, TaskLog) - too frequent writes, performance cost

---

## 3. Android Keystore Integration

### 3.1 Key Storage

**Never store encryption keys in:**
- ❌ SharedPreferences (even "secure" ones)
- ❌ `gradle.properties` or BuildConfig
- ❌ Hardcoded strings in source code
- ❌ Assets or resources folder

**Always store in:**
- ✅ **Android Keystore System** (hardware-backed if available)

**Implementation:**

```kotlin
object KeystoreManager {
    private const val KEY_ALIAS = "habitao_encryption_key_2026"
    
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        
        // Check if key exists
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
        
        // Generate new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // Set true for biometric unlock
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
```

---

## 4. Permissions Strategy

### 4.1 Required Permissions

**Android 12+ (API 31+):**

```xml
<manifest>
    <!-- CRITICAL: Exact alarms for habit reminders -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    
    <!-- Runtime permission for notifications -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Foreground service for Pomodoro timer -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    
    <!-- Reschedule alarms after device reboot -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    
    <!-- Optional: Vibrate on notifications -->
    <uses-permission android:name="android.permission.VIBRATE" />
    
    <!-- Future: Cloud sync -->
    <!-- <uses-permission android:name="android.permission.INTERNET" /> -->
    <!-- <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /> -->
</manifest>
```

### 4.2 Runtime Permission Flow

**Notification Permission (Android 13+):**

```kotlin
@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
        
        when {
            permissionState.status.isGranted -> {
                // All good
            }
            permissionState.status.shouldShowRationale -> {
                RationaleDialog(
                    title = "Enable Notifications",
                    message = "Habitao needs notifications to remind you about your habits and tasks.",
                    onConfirm = { permissionState.launchPermissionRequest() }
                )
            }
            else -> {
                // Permission permanently denied - show settings link
                PermanentlyDeniedDialog(
                    onOpenSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    }
                )
            }
        }
    }
}
```

**Exact Alarm Permission (Android 12+):**

```kotlin
fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService<AlarmManager>()
        
        if (alarmManager?.canScheduleExactAlarms() == false) {
            // Show rationale dialog
            showDialog(
                title = "Precise Timing Required",
                message = "For habit reminders to work at exact times, Habitao needs the 'Alarms & Reminders' permission.",
                onConfirm = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            )
        }
    }
}
```

### 4.3 Permission Rationale

| Permission | User-Facing Rationale |
|------------|----------------------|
| `POST_NOTIFICATIONS` | "Remind you about your habits and tasks at the right time" |
| `SCHEDULE_EXACT_ALARM` | "Send reminders at exactly the time you set (e.g., 7:00 AM, not 7:05 AM)" |
| `FOREGROUND_SERVICE` | "Keep Pomodoro timer running accurately even when app is in background" |
| `RECEIVE_BOOT_COMPLETED` | "Restore your reminders after device restart" |

---

## 5. Code Obfuscation & Protection

### 5.1 R8 Full Mode Configuration

**`proguard-rules.pro`:**

```proguard
# Enable R8 full mode
-allowaccessmodification
-repackageclasses

# Optimization
-optimizationpasses 5
-dontpreverify

# Keep domain models (for serialization)
-keep class com.habitao.domain.model.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep Proto DataStore models
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager.FragmentContextWrapper { *; }

# Crashlytics - keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
```

**`gradle.properties`:**
```properties
# Enable R8 full mode globally
android.enableR8.fullMode=true

# Aggressive optimization
android.enableR8.kotlin.experimental=true
```

### 5.2 Build Configuration

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Signing configuration (never commit keystore!)
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // Security: Disable automatic backup
    buildFeatures {
        buildConfig = true
    }
}
```

**AndroidManifest.xml:**
```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    tools:ignore="GoogleAppIndexingWarning">
    
    <!-- Prevent screenshots in sensitive screens (optional) -->
    <activity
        android:name=".MainActivity"
        android:windowSoftInputMode="adjustResize"
        android:screenOrientation="unspecified">
    </activity>
</application>
```

---

## 6. Network Security (Future Cloud Sync)

### 6.1 Network Security Configuration

**`res/xml/network_security_config.xml`:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <!-- Certificate Pinning for API domain -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.habitao.com</domain>
        <pin-set expiration="2027-01-01">
            <!-- Replace with actual certificate pins -->
            <pin digest="SHA-256">AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=</pin>
            <pin digest="SHA-256">BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=</pin>
        </pin-set>
    </domain-config>
    
    <!-- Allow localhost for debugging -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

**AndroidManifest.xml:**
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config">
</application>
```

### 6.2 HTTPS/TLS Configuration

**OkHttp Client (when implementing API):**

```kotlin
val client = OkHttpClient.Builder()
    .connectionSpecs(
        listOf(
            ConnectionSpec.MODERN_TLS, // TLS 1.3 preferred
            ConnectionSpec.COMPATIBLE_TLS
        )
    )
    .addInterceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "Habitao-Android/${BuildConfig.VERSION_NAME}")
            .build()
        chain.proceed(request)
    }
    .build()
```

---

## 7. Privacy & Data Handling

### 7.1 Privacy Policy Summary

**Data Collection:**
- ✅ **Local Only (MVP):** Habits, tasks, routines, Pomodoro sessions - ALL stored locally
- ✅ **No Account Required:** No email, phone, or personal identifiers collected
- ✅ **Optional Analytics:** Crash reports (Firebase Crashlytics), anonymized usage stats

**Data Sharing:**
- ❌ **No Third-Party Sharing:** User data never sold or shared
- ✅ **Export Anytime:** Users can export all data as JSON

**Data Deletion:**
- ✅ **Uninstall = Delete:** All local data deleted on app uninstall
- ✅ **In-App Deletion:** Settings → Delete All Data (irreversible)

### 7.2 GDPR Compliance (Future Cloud Sync)

**Requirements:**
- ✅ Explicit consent before collecting data
- ✅ Right to access (export function)
- ✅ Right to deletion (account deletion)
- ✅ Data portability (JSON export)
- ✅ Privacy Policy in-app and on website

**Implementation:**

```kotlin
// GDPR Consent Dialog (shown once)
@Composable
fun GDPRConsentDialog() {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        title = { Text("Data & Privacy") },
        text = {
            Column {
                Text("Habitao stores your data locally on your device.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Optional features:")
                
                var crashReporting by remember { mutableStateOf(true) }
                Row {
                    Checkbox(checked = crashReporting, onCheckedChange = { crashReporting = it })
                    Text("Send anonymous crash reports to improve the app")
                }
            }
        },
        confirmButton = {
            Button(onClick = { saveConsent(crashReporting = it) }) {
                Text("Accept")
            }
        }
    )
}
```

### 7.3 Data Export Function

```kotlin
class DataExportUseCase @Inject constructor(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val routineRepository: RoutineRepository
) {
    suspend fun exportAllData(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportData = ExportData(
                version = "1.0",
                exportedAt = Instant.now().toString(),
                habits = habitRepository.getAllHabits(),
                tasks = taskRepository.getAllTasks(),
                routines = routineRepository.getAllRoutines()
            )
            
            val json = Json.encodeToString(exportData)
            val file = File(context.getExternalFilesDir(null), "habitao_export_${System.currentTimeMillis()}.json")
            file.writeText(json)
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 8. Secure Backup Strategy

### 8.1 Local Backups

**Automated Daily Backup:**

```kotlin
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // 1. Close database
            val database = HabitaoDatabase.getInstance(applicationContext)
            database.close()
            
            // 2. Create backup directory
            val backupDir = File(applicationContext.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            
            // 3. Copy database file
            val dbFile = applicationContext.getDatabasePath("habitao.db")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupFile = File(backupDir, "habitao_backup_$timestamp.db")
            
            dbFile.copyTo(backupFile, overwrite = true)
            
            // 4. Encrypt backup
            encryptBackupFile(backupFile)
            
            // 5. Clean old backups (keep last 7)
            cleanOldBackups(backupDir, keepCount = 7)
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Backup failed")
            Result.retry()
        }
    }
    
    private fun encryptBackupFile(file: File) {
        val aead = KeyManager.getAead(applicationContext)
        val plainBytes = file.readBytes()
        val encryptedBytes = aead.encrypt(plainBytes, null)
        
        val encryptedFile = File(file.parent, "${file.name}.enc")
        encryptedFile.writeBytes(encryptedBytes)
        
        file.delete() // Remove unencrypted backup
    }
    
    private fun cleanOldBackups(backupDir: File, keepCount: Int) {
        backupDir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keepCount)
            ?.forEach { it.delete() }
    }
}
```

### 8.2 Backup Encryption

**Why Encrypt Backups:**
- Backup files stored in app-private directory can be extracted via ADB
- Android Debug Bridge (ADB) allows pulling files from device
- Encrypted backups useless without decryption key (stored in Keystore)

---

## 9. Vulnerability Scanning

### 9.1 OWASP Dependency Check

**Gradle Plugin:**

```kotlin
// build.gradle.kts (project level)
plugins {
    id("org.owasp.dependencycheck") version "9.0.0"
}

dependencyCheck {
    format = "HTML"
    failBuildOnCVSS = 7.0f // Fail build if vulnerability severity >= 7
    suppressionFile = file("owasp-suppressions.xml")
}
```

**CI Integration:**

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  pull_request:
  schedule:
    - cron: '0 0 * * 0' # Weekly on Sunday

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze
      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: build/reports/dependency-check-report.html
```

### 9.2 Static Analysis

**Detekt Security Rules:**

```yaml
# detekt.yml
security:
  active: true
  HardcodedPassword:
    active: true
  InsecureConnection:
    active: true
  WeakCryptography:
    active: true
```

---

## 10. Incident Response Plan

### 10.1 Security Vulnerability Process

**If security issue discovered:**

1. **Assess Severity:**
   - CRITICAL: Data leakage, encryption bypass
   - HIGH: Permission bypass, DoS
   - MEDIUM: Improper logging
   - LOW: Cosmetic issues

2. **Immediate Actions:**
   - Create private GitHub security advisory
   - Patch vulnerability in private branch
   - Test patch thoroughly

3. **Disclosure:**
   - Notify users via in-app notification
   - Publish security advisory after patch released
   - Release emergency update with force-update mechanism

4. **Prevention:**
   - Add regression test
   - Update security checklist
   - Review similar code patterns

### 10.2 Security Checklist (Pre-Release)

- [ ] R8 obfuscation enabled in release build
- [ ] All API keys stored in Keystore, not BuildConfig
- [ ] Sensitive logs removed (no `Log.d` with user data)
- [ ] Network security config enforces HTTPS
- [ ] Backup encryption tested and verified
- [ ] Runtime permissions properly requested and handled
- [ ] OWASP dependency check passed (no CVSS >= 7)
- [ ] Detekt security rules passed
- [ ] ProGuard rules tested (app doesn't crash in release)
- [ ] Crash reports anonymized (no PII in Crashlytics)

---

## 11. User Education

### 11.1 Security Tips (In-App)

**Settings → Security:**

- ✅ "Use a device screen lock (PIN/Pattern/Biometric) to protect your data"
- ✅ "Disable battery optimization for Habitao to ensure timely reminders"
- ✅ "Enable auto-backups to prevent data loss"
- ⚠️ "Habitao does not sync to cloud. Uninstalling will delete all data."

### 11.2 Transparency

**About → Privacy:**
- Link to privacy policy
- "View data Habitao has collected" (shows empty state for MVP)
- "Export my data" button → generates JSON file
- "Delete all data" button (with confirmation)

---

**End of Security & Privacy Document**
