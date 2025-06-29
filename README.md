# 📱 Smart Cart Android App Code Documentation

---

**Project Name**: Smart Cart – Customer Scanner App
**Platform**: Android (Jetpack Compose)
**Language**: Kotlin
**Architecture**: Single-Activity, Compose UI
**Backend**: Firebase Realtime Database (REST API Access)

---

## 📝 Project Description

The Smart Cart Android app is part of a checkout verification system designed to minimize theft in retail stores. It allows a cashier to:

* Load receipt data from Firebase
* Scan item RFID tags using the phone's NFC
* Compare scanned UIDs with expected UIDs
* Deactivate (delete) matched UIDs from Firebase

The prototype successfully demonstrates UID-based verification and removal using real NFC tags and Firebase integration.

---

The Smart Cart Android app is a critical component in a checkout verification system designed not only to reduce theft in retail environments, but also to empower the customer.

While modern supermarkets and hyper marts invest heavily in surveillance and anti-theft technologies to protect their inventory, customers are often left vulnerable to billing and bagging errors. Many walk out of stores in a state of anxiety, unsure whether all the items they paid for actually made it into their shopping bags — a frustrating experience that forces them to manually recheck everything at home.

**Smart Cart eliminates this post-purchase doubt** by allowing the cashier (or eventually, the customer) to verify all items at the point of exit using NFC-based scanning. This ensures a perfect match between billed and bagged items before leaving the store — saving time, reducing errors, and improving customer trust.

> ⚠️ **Note**:
> NFC technology operates at very short ranges (typically a few centimeters). While ideal for prototyping, it's not suitable for full-scale retail environments. A commercial version would benefit from using UHF RFID readers for long-range scanning.

Looking ahead, this system sets the foundation for:

* **Buy-Now-Pick-Up-Later** features
* **Self-verification and secure delivery** models
* **UHF RFID-based scanning using future smartphones or external devices**

---

## 📄 `MainActivity.kt` - Code Breakdown

**Class**: `MainActivity`
**Inherits from**: `AppCompatActivity`

**Key Responsibilities**:

* Set up NFC handling
* Compose UI rendering
* Fetch receipt UIDs from Firebase
* Compare UIDs scanned vs billed
* Delete UID entries from Firebase Inventory

---

## 📁 Libraries and Dependencies Used

| Library                                    | Purpose                                       |
| ------------------------------------------ | --------------------------------------------- |
| `androidx.appcompat:appcompat:1.7.1`       | Required for AppCompatActivity                |
| `androidx.activity:activity-compose`       | Enables Compose in activity                   |
| `androidx.compose.material3`               | UI components (TextField, Button, etc.)       |
| `androidx.lifecycle:lifecycle-runtime-ktx` | Lifecycle support                             |
| `org.json`                                 | Parse JSON responses from Firebase            |
| `kotlinx.coroutines`                       | For running Firebase operations in background |

> Declared in `build.gradle.kts` using a version catalog.

---

## 🔧 Major Components

### 1. **NFC Setup**

* Uses `NfcAdapter` for foreground dispatch
* Handles scanned tags in `onNewIntent()`
* Extracts UID as a hex string

### 2. **UI Composition (Jetpack Compose)**

* Text field for Receipt ID
* Button to load receipt
* Texts showing expected and scanned UID counts
* Button to verify match and trigger UID deactivation
* All state handled using `remember` and `mutableStateListOf`

### 3. **Firebase REST API Access**

* `GET` request: `/Receipts/{receiptID}/uids`
* `DELETE` request: `/Inventory/{uid}`
* Uses `HttpURLConnection` with coroutines (`Dispatchers.IO`)

### 4. **Verification Logic**

* Compares sorted, uppercase-trimmed UID lists
* Shows success/failure result in UI
* On success, removes UIDs from inventory

---

## 🚀 Execution Flow Summary

1. App launches, initializes NFC
2. User enters a Receipt ID and clicks **"Load Receipt"**
3. App sends `GET` request to Firebase and loads expected UIDs
4. User taps items using NFC
5. Scanned UIDs are captured and displayed
6. User clicks **"Verify Items"**
7. App compares scanned UIDs with expected ones
8. If they match, app deletes the UIDs from Inventory

---

## ⚙️ Build Settings Summary

* `minSdk`: 24
* `targetSdk`: 33 (tested on API 30)
* Jetpack Compose enabled with `buildFeatures.compose = true`
* Uses `PendingIntent.FLAG_MUTABLE` for compatibility

---

## 🔒 Permissions

In `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

---

## 📋 Additional Notes

* JSON response from Firebase must be an array of UIDs
* UID extraction logic:

  ```kotlin
  tag.id.joinToString("") { "%02X".format(it) }
  ```
* Compose’s `LaunchedEffect(intent)` ensures NFC tags are read on launch
* No Firebase SDK used — direct REST API calls for a lightweight app

---

## 📅 Version

**Prototype Complete**: June 2025
**Developed by**: *Visionary Coders*

---
