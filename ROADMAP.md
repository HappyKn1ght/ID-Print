# 🗺️ ID Print — Development Roadmap & v1.1 Plan

This document tracks planned upgrades, architecture decisions, and implementation notes for upcoming releases of **ID Print**.

---

## ✅ v1.0 — Initial Release (Live)
- Two-step Front & Back ID card upload flow
- Smart auto-crop + manual edge adjustment
- Clean 100% offline A4 PDF export (zero UI artifacts)
- Automated CI/CD build & release pipeline via GitHub Actions
- 24/7 automated Gmail feedback confirmation & categorized response templates

---

## 🚀 v1.1 — Planned Features & Enhancements

### 1. Privacy-First Anonymous Usage Metrics (Zero Personal Data)
**Goal:** Measure real installed devices and total ID cards printed without collecting names, device IDs, IP addresses, or document images.

- **Unique Installed Devices (`unique-installs`):**
  - Check a local `SharedPreferences` flag (`has_counted_install = false`) on app launch.
  - If `false` and internet is available, send a single empty background HTTP GET ping (`https://counterapi.dev`).
  - Once successful, set `has_counted_install = true` permanently so each phone is counted **only once**.
- **Total PDFs Generated (`pdfs-generated`):**
  - Trigger a silent background ping (`https://counterapi.dev`) each time an A4 PDF is successfully saved.
  - Fails silently if offline so PDF generation is never delayed or blocked.
- **Manifest Requirement:** Add `<uses-permission android:name="android.permission.INTERNET" />` to `AndroidManifest.xml`.

### 2. Smart 3-Category "Contact Developer" Routing
**Goal:** Automatically tag incoming user emails so Gmail filters can send tailored auto-replies and organize feedback by type.

- When tapping **Contact Developer**, show a selection dialog with 3 options:
  1. **👍 Appreciation / Working** → Subject: `[ID Print - Appreciation]`
  2. **💡 Feature Suggestion** → Subject: `[ID Print - Feature Request]`
  3. **🐞 Bug Report** → Subject: `[ID Print - Bug Report]`
- Match these exact 3 subject tags to the 3 pre-built Gmail filters and templates.

---

## 📋 Copy-Paste Prompt for Google AI Studio (When Ready to Build v1.1)

> *"Update the ID Print Android app with two new features:*
> *1. **Privacy-First Anonymous Counters:** Add `INTERNET` permission to `AndroidManifest.xml`. On app startup, check a `SharedPreferences` boolean (`is_install_counted`); if false, send an asynchronous background GET request to `https://counterapi.dev` and set the boolean to true upon success so each phone is counted only once. Also send a silent background GET request to `https://counterapi.dev` whenever a PDF is successfully saved. Ensure both network calls run on a background thread (`Dispatchers.IO`) and fail silently without errors or delays when offline.*
> *2. **Categorized Contact Developer Menu:** When the user taps 'Contact Developer' in the footer, show a clean dialog with three options: '👍 Appreciation / Working', '💡 Feature Suggestion', and '🐞 Bug Report'. Selecting an option opens their email app (`mailto:happykn1ghtr7@gmail.com`) with the exact subject pre-filled: `[ID Print - Appreciation]`, `[ID Print - Feature Request]`, or `[ID Print - Bug Report]`.*
> *Keep all image cropping and PDF generation 100% local and offline."*

---

## 🧠 Product Decisions Log (Feature Triage)
- **JPG Export Option (Deferred):** Requested by 1 user; deferred to keep the main UI focused on print shops' primary workflow (A4 PDF). Will revisit if 3+ users request it.
