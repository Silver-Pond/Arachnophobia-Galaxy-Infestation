---

## YouTube Link: 

---

# Archanophia: Galaxy Infestation – Comprehensive Documentation

## 📖 1. Introduction
Archanophia: Galaxy Infestation is a retro-inspired, vertical-scrolling shoot ’em up that merges classic arcade intensity with modern mobile app features. Designed for high difficulty and replayability, the game offers multiple game modes, online data handling, secure authentication, biometric login, and persistent player progression. Throughout development, GitHub served as the central platform for version control, CI automation, and documentation management, ensuring a structured and reliable workflow.

---

## 🎯 2. Purpose of the Application
The purpose of this application is to deliver an immersive and challenging arcade experience enhanced by contemporary mobile app capabilities. Players compete for high scores, unlock customizable starship skins, earn trophies, and experience evolving enemy behaviour. In addition to entertainment value, this application implements key software engineering concepts such as secure authentication flows, REST-based backend interactions, real-time database handling, and offline caching.

---

## 🎨 3. Design Considerations

### **Usability**
A clean, intuitive user interface was prioritised. The app features clear navigation paths, a visually coherent insect–arachnid theme, and ease-of-use across all screens. Multi-language support, profile customization, and biometric login promote accessibility and personalization.

### **Security**
Security was a major focus. The system uses Firebase Authentication for email/password and Google SSO login. Biometric authentication is implemented through Android’s BiometricPrompt API. Biometric data never leaves the device, keeping user security aligned with Android best practices.

### **Performance**
Performance considerations included minimizing unnecessary database calls, caching offline data to reduce loading times, and using lightweight data models for high-speed gameplay scenarios. Firebase Realtime Database enables fast, low-latency retrieval of user data.

---

## 🛠️ 4. Utilisation of GitHub & GitHub Actions

GitHub acted as the central development hub, enabling:

- **Version control** for all source code, assets, and documentation  
- **Branching workflows** to isolate features and maintain project stability  
- **Pull Requests** for code review and quality assurance  
- **Issue tracking** for task management and bug-resolution  
- **Automated CI via GitHub Actions**, including:
  - Linting and static analysis of Kotlin code  
  - Automated debug builds  
  - Documentation validation  
  - Deployment verification for backend API updates  

GitHub Actions ensured consistent build reliability and reduced manual overhead during development.

---

## 🔐 5. Revised Biometric Login Implementation

### Overview
The prototype used a *dedicated biometric login fragment*.  
This has been replaced with a **modern, integrated biometric flow** embedded directly into the login logic.  
The new system is **more seamless, secure, and in line with Android UX standards**.

---

## 🔑 5.1 Google SSO + Biometrics (Updated Flow)

The updated implementation uses:

```kotlin
firebaseAuthWithGoogle(account: GoogleSignInAccount, useBiometrics: Boolean)
```

## 🔐 5.2 Email/Password Login + Biometrics (Updated Flow)

The updated email/password login system now integrates biometric authentication when the user has enabled it in the app’s Settings. This replaces the previous prototype, which pushed biometrics to a separate page. The new flow is seamless, streamlined, and consistent with modern mobile security practices.

### Updated Email Login Sequence
1. The user enters their email and password on the login screen.  
2. The app checks the stored biometric preference found in `AppSettings`.  
3. If biometrics are enabled:  
   - A `BiometricPrompt` is displayed.  
   - On successful fingerprint/face authentication, the app proceeds with Firebase email/password login.  
4. If biometrics are disabled or unavailable:  
   - Login proceeds normally without biometric checks.  
5. On successful authentication, the user’s profile data is fetched from Firebase.  
6. Login credentials and username are securely saved in `UserPrefs` for future biometric login.  
7. The player is directed to the `GameMenuFragment`.

### Improvements Over the Prototype
- No more separate biometric login fragment  
- Consistent behaviour for both login types (Google SSO & Email/Password)  
- Cleaner and easier-to-maintain authentication architecture  
- Secure credential handling for future biometric re-authentication  
- Better error handling and fallback behaviour when biometrics fail or are unsupported  

---

# 📝 6. Release Notes  
## **Version 1.0 – Current Release**

### New Features & Improvements Since Prototype

#### **Biometric Login System Overhaul**
- Removed the standalone biometric login fragment used in the prototype  
- Integrated biometric authentication directly into:
  - Google SSO login  
  - Email/Password login  
- Implemented biometric fallback behaviour for unsupported devices  
- Credentials now securely stored in `UserPrefs` for fingerprint-based quick login  

#### **Authentication Flow Enhancements**
- Centralised all Firebase authentication logic for cleaner, more maintainable code  
- Added detailed error handling for:
  - Incorrect credentials  
  - Empty email/password fields  
  - Database retrieval errors  
  - Biometric failures  
- Created faster, smoother navigation from login to the Game Menu  

#### **Data Handling Improvements**
- User biometric preference stored in `AppSettings`  
- User credentials securely saved for biometric reuse  
- Optimised Firebase database calls using single-value listeners  

#### **User Experience Improvements**
- Added click sound effects to login interactions  
- Improved login speed by eliminating unnecessary fragments  
- Reduced cognitive load with a streamlined login flow  

---

# 🏁 7. Conclusion

Archanophia: Galaxy Infestation merges classic arcade gameplay with modern mobile architecture, advanced authentication features, and persistent user data management. The redesigned biometric system marks a significant upgrade from the prototype, providing a seamless, secure, and user-friendly login experience. The integration of GitHub and GitHub Actions throughout development ensured reliable build automation, efficient version control, and stable continuous integration. The result is a polished, scalable, and user-centric mobile game application ready for delivery and further enhancement.

---

# 📦 Release Notes  
## **Version 1.0 – November 2025 Update**

This release includes major structural improvements, biometric authentication integration, UI updates, gameplay enhancements, and overall project reorganisation. Below is a detailed breakdown of changes based on commit history from **October 8, 2025 → November 18, 2025**.

---

## **November 18, 2025**
### **Updated Documentation**
- Updated the `README.md` to reflect new authentication logic, UI updates, and biometric changes.  
- Verified commit cleanup by **Silver-Pond** to maintain documentation accuracy.

---

## **November 17, 2025**
### **Biometric System Overhaul**
- **Removed `BiometricLoginFragment.kt` entirely** as part of the new integrated biometric workflow.  
- Implemented biometric authentication directly inside:
  - `LoginHubFragment.kt`
  - `LoginFragment.kt`  
- Improved accuracy, fallback logic, and user experience for biometric-enabled logins.

### **Theme & UI Enhancements**
- Updated themes to align with the new login behaviour.  
- Refined biometric-related UI components.

### **Project Structure Updates**
- Updated main directory files and `res` folder contents for better consistency.  
- Improved resource loading and fragment UI behaviour.

### **Colour & Layout Improvements**
- Updated various layout files.  
- Modified `colors.xml` for standard mode.  
- Added new `values-night/colors.xml` to ensure proper dark mode support.

---

## **November 16, 2025**
### **General Code Improvements**
- Updated additional code in `main` and `res` folders.  
- Refined fragment interactions and gameplay logic.  
- Multiple small fixes to improve stability and resource access.

### **Project Organisation**
- Created new folders to improve codebase structure and maintainability.

---

## **November 15, 2025**
### **Gameplay & Notification Updates**
- Updated `NotificationReceiver` to improve in-game notification handling.  
- Enhanced `SurvivalGameFragment` logic, including:
  - Enemy spawning improvements  
  - Survival mode stability fixes  
  - Event-handling consistency  

---

## **November 14, 2025**
### **Currency System Overhaul**
- Updated the `CurrencyManager` to support more reliable offline/online currency handling.
- Added a **new CurrencyManager implementation** with improved logic for currency accumulation.
- Updated:
  - `HighScoreManager`
  - `GameFragment`  
- Improved coin reward calculations and persistent data storage.

### **Notification System Updates**
- Enhanced the `NotificationReceiver` to ensure messages are triggered consistently across different game states.

---

## **November 9, 2025**
### **Repository Synchronisation**
- Merged remote-tracking branch `origin/main` to unify the codebase across devices and contributors.
- Ensured all local changes were reflected in the main branch.

### **String Resources Added**
- Introduced `values-strings.xml` to support:
  - Multi-language localisation  
  - Cleaner string management  
  - Future extension to South African languages
- Introduced `BootReceiver.kt` and `NotificationReceiver.kt` to support:
  - Post Notifications

---

## **October 8, 2025**
### **Initial Updates**
- Early project setup changes before major updates.  
- Minor adjustments to layouts, logic, and resource organisation.

---

# Summary of Key Improvements in This Release
- Full biometric authentication integration  
- Removal of old biometric fragment in favour of a seamless, modern login flow  
- Updated layouts, themes, colours, and resource files  
- New CurrencyManager implementation  
- Gameplay updates for survival mode  
- Notification and high-score logic improvements  
- Better project structure and maintainability  
- Updated README with correct project documentation  

---

# 🌟 Innovative Features Added

## **1. Dynamic Environmental Hazards in Survival Mode**
A new environmental hazard system has been implemented to increase gameplay challenge and variety within Survival Mode. Large web-coated asteroids now descend from random x-axis positions at fixed level intervals (Levels 1–4, 10–14, 20–24, and so on).  
These hazards introduce meaningful difficulty during early waves where fewer enemy types are present, ensuring that low-level stages remain engaging while encouraging players to adapt their movement and positioning strategies.

## **2. Unlockable and Equippable Starship Skins**
The game now features a fully functional starship skin system. Players can unlock new cosmetic skins for their ship using in-game currency earned across both Arcade Mode and Survival Mode.  
Once unlocked, skins can be equipped via the dedicated Skins page, and they appear consistently across all gameplay modes. This system enhances player personalization, offers clear progression incentives, and rewards long-term engagement.
