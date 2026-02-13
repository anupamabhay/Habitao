# CI/CD Pipeline Documentation
## Habitao - Automated Testing & Deployment

**Version:** 1.0  
**Last Updated:** February 13, 2026  
**Status:** ✅ Configured and Active

---

## 1. Overview

The Habitao project uses **GitHub Actions** for continuous integration and continuous deployment (CI/CD). The pipeline automatically runs on every push and pull request to ensure code quality, test coverage, and build integrity.

### 1.1 Pipeline Goals

1. **Quality Assurance:** Catch bugs early through automated testing
2. **Code Standards:** Enforce Kotlin code style via Detekt and Ktlint
3. **Build Verification:** Ensure the app compiles successfully
4. **Fast Feedback:** Developers know within minutes if their changes break something
5. **Prevent Regressions:** Integration tests run on real Android emulators

---

## 2. Workflows

### 2.1 Main CI Workflow (`.github/workflows/ci.yml`)

**Triggers:**
- Push to `dev` or `main` branches
- Pull requests targeting `dev` or `main`

**Jobs:**

#### Job 1: Code Quality (15 min)
```yaml
Runs on: ubuntu-latest
Steps:
  1. Checkout code
  2. Set up JDK 17
  3. Run Detekt (static analysis)
  4. Run Ktlint (formatting check)
  5. Upload reports as artifacts
```

**What it checks:**
- Code complexity
- Potential bugs (null pointer, unused variables)
- Code smells
- Kotlin style violations
- Formatting issues

**Artifacts produced:**
- `detekt-reports/` - HTML reports viewable in browser

---

#### Job 2: Unit Tests (20 min)
```yaml
Runs on: ubuntu-latest
Parallel execution: Runs tests for all modules simultaneously
Steps:
  1. Checkout code
  2. Set up JDK 17
  3. Run tests for domain module
  4. Run tests for data module
  5. Run tests for feature modules (habits, tasks, routines, pomodoro)
  6. Run tests for core modules
  7. Generate JaCoCo coverage report
  8. Upload test results and coverage
```

**What it tests:**
- Domain layer (use cases, business logic)
- Data layer (repositories with mocked DAOs)
- Feature ViewModels
- Utility functions

**Coverage target:** 85% overall

**Artifacts produced:**
- `test-reports/` - JUnit XML and HTML reports
- `coverage-reports/` - JaCoCo HTML coverage reports

---

#### Job 3: Build APK (25 min)
```yaml
Runs on: ubuntu-latest
Depends on: code-quality, unit-tests (must pass first)
Steps:
  1. Checkout code
  2. Set up JDK 17
  3. Build debug APK
  4. Check APK size (warn if > 20MB)
  5. Upload APK as artifact
```

**What it verifies:**
- App compiles without errors
- All dependencies resolve correctly
- ProGuard rules are valid
- APK size is reasonable

**Artifacts produced:**
- `debug-apk/` - Installable APK for testing

---

#### Job 4: Integration Tests (45 min)
```yaml
Runs on: macos-latest (required for Android Emulator)
Depends on: code-quality
Matrix strategy: Tests on API 26 (min SDK) and API 34 (latest)
Steps:
  1. Checkout code
  2. Set up JDK 17
  3. Cache Android Virtual Device (AVD)
  4. Create AVD snapshot (if not cached)
  5. Start Android Emulator
  6. Run connectedAndroidTest
  7. Upload test results
```

**What it tests:**
- Repository + Room DAO integration (in-memory database)
- ViewModel + Use Case integration
- Compose UI components
- Database migrations

**Why two API levels?**
- API 26: Minimum SDK - ensure compatibility with oldest supported Android
- API 34: Latest SDK - test new platform features

**Artifacts produced:**
- `integration-test-reports-api26/`
- `integration-test-reports-api34/`

---

#### Job 5: Dependency Vulnerability Scan (20 min)
```yaml
Runs on: ubuntu-latest
Trigger: Weekly schedule OR manual workflow_dispatch
Steps:
  1. Checkout code
  2. Set up JDK 17
  3. Run OWASP Dependency Check
  4. Upload vulnerability report
```

**What it scans:**
- Known CVEs in dependencies
- Outdated libraries with security patches
- License compliance

**Threshold:** Fails if CVSS score ≥ 7.0

**Artifacts produced:**
- `dependency-check-report.html` - Detailed vulnerability report

---

### 2.2 PR Checks Workflow (`.github/workflows/pr-checks.yml`)

**Purpose:** Additional validation specific to pull requests

**Triggers:**
- Pull request opened/synchronized/reopened

**Jobs:**

#### Job 1: PR Validation
**Checks:**
- PR title follows conventional commits format (`feat:`, `fix:`, etc.)
- PR has a description (not empty)
- PR size is reasonable (warns if > 800 lines, fails if > 1500)

#### Job 2: Code Hygiene
**Checks:**
- Counts TODO comments (warns)
- Counts FIXME comments (fails if found)
- Encourages resolving issues before merge

#### Job 3: Test Coverage Requirement
**Checks:**
- If source files changed, test files should also change
- Warns if new code lacks tests

#### Job 4: Conflict Check
**Checks:**
- Scans for merge conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`)
- Prevents accidental commits of unresolved conflicts

#### Job 5: Auto Label
**Actions:**
- Automatically labels PRs based on changed files
- Labels: `documentation`, `gradle`, `feature: habits`, `testing`, etc.

---

## 3. Configuration Files

### 3.1 Detekt Configuration

**File:** `config/detekt/detekt.yml`

**Key rules enabled:**
- Complexity: `ComplexMethod`, `TooManyFunctions`
- Coroutines: `SuspendFunctionOnCoroutineScope`
- Exceptions: `SwallowedException`, `TooGenericExceptionCaught`
- Naming: `FunctionNaming`, `ClassNaming`
- Performance: `SpreadOperator`, `ForEachOnRange`
- Potential bugs: `UnsafeCast`, `LateinitUsage`

**Baseline:** `config/detekt/baseline.xml`
- Contains known issues at project start
- New code must not introduce more violations

### 3.2 Ktlint Configuration

**Embedded in:** `gradle/libs.versions.toml` + module `build.gradle.kts`

**Style:** Kotlin official code style

**Auto-fix:** `./gradlew ktlintFormat`

---

## 4. Using the CI/CD Pipeline

### 4.1 Local Development Workflow

**Before pushing code:**

```bash
# 1. Run code quality checks locally
./gradlew detekt ktlintCheck

# 2. Auto-fix formatting issues
./gradlew ktlintFormat

# 3. Run unit tests
./gradlew testDebugUnitTest

# 4. Build debug APK
./gradlew assembleDebug

# 5. (Optional) Run integration tests locally
./gradlew connectedDebugAndroidTest
```

**Expected outcome:** All commands should succeed before pushing.

---

### 4.2 Pull Request Workflow

**Step 1: Create Feature Branch**
```bash
git checkout dev
git pull origin dev
git checkout -b feature/habit-streak-calculation
```

**Step 2: Make Changes + Commit**
```bash
# Make your changes
git add .
git commit -m "feat(habits): add streak calculation use case"
```

**Step 3: Push and Create PR**
```bash
git push -u origin feature/habit-streak-calculation
# Go to GitHub and create PR targeting 'dev'
```

**Step 4: Wait for CI Checks**
- GitHub Actions will automatically run
- Check status at: `https://github.com/YOUR_USERNAME/habitao/actions`
- PR cannot be merged until all checks pass ✅

**Step 5: Address Failures**

If CI fails:
1. Click on failed job to see logs
2. Fix the issue locally
3. Push again (CI reruns automatically)

---

### 4.3 Viewing CI Results

#### In GitHub UI:
1. Navigate to **Actions** tab
2. Click on workflow run
3. View each job's logs and artifacts

#### Downloading Artifacts:
1. Go to failed workflow run
2. Scroll to bottom: "Artifacts" section
3. Download: `test-reports`, `detekt-reports`, `debug-apk`, etc.
4. Unzip and open `index.html` in browser

#### Example: Viewing Test Coverage
```bash
# Download coverage-reports.zip from GitHub Actions
unzip coverage-reports.zip
cd domain/build/reports/jacoco/test/html
open index.html  # or start index.html on Windows
```

---

## 5. Troubleshooting

### 5.1 Common CI Failures

#### ❌ Detekt Failures

**Error:**
```
> Task :app:detekt FAILED
Complexity - [ComplexMethod] at MainActivity.kt:45
```

**Solution:**
```kotlin
// Refactor complex function into smaller functions
// OR add to Detekt baseline if it's a known exception
./gradlew detektBaseline  # Updates baseline.xml
```

---

#### ❌ Ktlint Failures

**Error:**
```
> Task :domain:ktlintCheck FAILED
domain/src/main/kotlin/Habit.kt:12:1: 
  Exceeded max line length (120) (standard:max-line-length)
```

**Solution:**
```bash
# Auto-fix formatting
./gradlew ktlintFormat

# Or manually break the line
val habit = Habit(
    title = "Very long title that exceeds the maximum line length",
    goalCount = 8
)
```

---

#### ❌ Unit Test Failures

**Error:**
```
HabitsViewModelTest > load habits returns correct state FAILED
    Expected: [Habit(id=1)]
    Actual: []
```

**Solution:**
1. Run test locally: `./gradlew :feature:habits:testDebugUnitTest`
2. Check test logs in `feature/habits/build/reports/tests/testDebugUnitTest/index.html`
3. Fix code or test
4. Verify: `./gradlew :feature:habits:testDebugUnitTest`

---

#### ❌ Build Failures

**Error:**
```
> Task :app:compileDebugKotlin FAILED
e: Unresolved reference: HiltAndroidApp
```

**Solution:**
```kotlin
// Missing import or dependency
// Check app/build.gradle.kts has:
dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}

// Ensure @HiltAndroidApp is imported:
import dagger.hilt.android.HiltAndroidApp
```

---

#### ❌ Integration Test Timeout

**Error:**
```
> Task :data:connectedDebugAndroidTest
Tests on test(AVD) - 11 timed out after 30 minutes
```

**Solution:**
```yaml
# Increase timeout in ci.yml
timeout-minutes: 60  # Was 45

# OR optimize slow tests:
// Use Robolectric for pure logic tests instead of instrumented tests
```

---

### 5.2 Advanced Debugging

#### Run CI Locally with Act

**Install Act:**
```bash
# macOS
brew install act

# Windows (with Chocolatey)
choco install act-cli
```

**Run workflow locally:**
```bash
# Run all jobs
act

# Run specific job
act -j unit-tests

# Run with specific event
act pull_request
```

**Limitations:**
- Cannot run macOS jobs (Android emulator tests)
- Requires Docker

---

## 6. CI/CD Metrics

### 6.1 Performance Benchmarks

| Job | Average Duration | Timeout |
|-----|------------------|---------|
| Code Quality | 3-5 min | 15 min |
| Unit Tests | 8-12 min | 20 min |
| Build APK | 10-15 min | 25 min |
| Integration Tests (API 26) | 25-35 min | 45 min |
| Integration Tests (API 34) | 25-35 min | 45 min |

**Total PR validation time:** ~40 min (parallel jobs)

### 6.2 Cost Analysis

**GitHub Actions Free Tier:**
- 2,000 minutes/month for private repos
- Unlimited for public repos

**Monthly usage estimate (private repo):**
- 20 PRs × 40 min × 2 (push + PR events) = 1,600 min/month
- **Fits within free tier** ✅

---

## 7. Maintenance

### 7.1 Weekly Tasks

**Every Monday:**
- [ ] Review dependency check report (if scheduled run occurred)
- [ ] Update any vulnerable dependencies
- [ ] Check for flaky tests (tests that fail intermittently)

### 7.2 Monthly Tasks

**First of month:**
- [ ] Review CI/CD metrics (average build time, failure rate)
- [ ] Update Detekt/Ktlint rules if needed
- [ ] Check GitHub Actions usage (ensure within free tier)

### 7.3 Updating Dependencies

**When updating Gradle dependencies:**
1. Update `gradle/libs.versions.toml`
2. Run `./gradlew dependencyCheckAnalyze` locally
3. Fix any vulnerabilities before pushing
4. CI will verify changes

---

## 8. Future Enhancements

### 8.1 Planned Additions

- [ ] **Maestro E2E Tests:** Add to CI when screens are implemented
- [ ] **Performance Testing:** Macrobenchmark for startup time
- [ ] **Screenshot Tests:** Visual regression testing with Paparazzi
- [ ] **Code Coverage Badge:** Display coverage % in README
- [ ] **Automatic Release:** Deploy to Play Store on `main` branch merge

### 8.2 Potential Optimizations

- **Caching:** Cache Gradle dependencies (already implemented)
- **Matrix Testing:** Test on more API levels (API 26, 30, 34)
- **Parallel Builds:** Split feature module tests into separate jobs
- **Conditional Jobs:** Skip integration tests for docs-only changes

---

## 9. Security Considerations

### 9.1 Secrets Management

**Never commit:**
- ❌ Signing keystore files
- ❌ API keys
- ❌ Firebase `google-services.json` with sensitive data

**Use GitHub Secrets:**
```yaml
# In workflow file
- name: Sign APK
  run: ./gradlew assembleRelease
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
```

### 9.2 Dependency Scanning

**OWASP Dependency Check** runs weekly to detect:
- Known CVEs (Common Vulnerabilities and Exposures)
- Outdated libraries
- License violations

**Action on high-severity findings:**
1. Update dependency immediately
2. If no update available, assess risk
3. Document decision if accepting risk temporarily

---

## 10. Quick Reference

### 10.1 Common Commands

```bash
# Code quality
./gradlew detekt ktlintCheck ktlintFormat

# Tests
./gradlew test                      # All unit tests
./gradlew testDebugUnitTest         # Android unit tests
./gradlew connectedDebugAndroidTest # Integration tests

# Build
./gradlew assembleDebug             # Debug APK
./gradlew assembleRelease           # Release APK

# Coverage
./gradlew jacocoTestReport          # Generate coverage report
```

### 10.2 GitHub Actions URLs

- **All Workflows:** `https://github.com/USERNAME/habitao/actions`
- **CI Workflow:** `.../actions/workflows/ci.yml`
- **PR Checks:** `.../actions/workflows/pr-checks.yml`

---

## 11. Support

### 11.1 Getting Help

**If CI is broken:**
1. Check this documentation first
2. Review GitHub Actions logs
3. Search GitHub Issues for similar problems
4. Create issue with:
   - Workflow run URL
   - Error message
   - Steps to reproduce

### 11.2 Updating This Documentation

**When to update:**
- Adding new workflow jobs
- Changing timeout values
- Adding new checks
- Discovering new troubleshooting solutions

**How to update:**
```bash
# Edit this file
vim docs/06-CICD-SETUP.md

# Commit
git add docs/06-CICD-SETUP.md
git commit -m "docs: update CI/CD documentation with new job"
```

---

**Last Updated:** February 13, 2026  
**Maintained By:** Development Team  
**Next Review:** March 13, 2026

---

**End of CI/CD Documentation**
