# Office Suite - Technical Documentation

This document provides a comprehensive technical overview of the Office Suite codebase, including architecture, module breakdown, APIs, and data models. It is intended for developers working on the project.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Data Layer](#data-layer)
   - [Models](#models)
   - [Repositories](#repositories)
3. [Core Feature Modules](#core-feature-modules)
   - [PDF Module](#pdf-module)
   - [Office Documents (Word, Excel, PowerPoint)](#office-documents)
   - [Scanner & OCR](#scanner--ocr)
4. [Advanced Features](#advanced-features)
   - [AI & Intelligence](#ai--intelligence)
   - [Productivity & Writing](#productivity--writing)
   - [Platform Integration](#platform-integration)
   - [Automation & Search](#automation--search)
5. [UI Layer](#ui-layer)
6. [Cross-Cutting Concerns](#cross-cutting-concerns)
   - [Security](#security)
   - [Cloud & Storage](#cloud--storage)
   - [Performance](#performance)
   - [Accessibility](#accessibility)
   - [Analytics](#analytics)
   - [Social & Collaboration](#social--collaboration)
   - [Developer Tools](#developer-tools)

---

## Architecture Overview

The application follows the **MVVM (Model-View-ViewModel)** architectural pattern, ensuring separation of concerns and testability.

- **View**: Fragments and Activities (in `ui/` packages) are responsible for rendering the UI and handling user interactions. They observe data from ViewModels.
- **ViewModel**: Holds UI state and business logic, communicating with Repositories.
- **Model/Repository**: The `data/` package contains the data models and repositories that abstract data sources (local storage, cloud, etc.).

The project uses standard Android Jetpack components including ViewBinding and Navigation Component.

---

## Data Layer

### Models
Located in `com.officesuite.app.data.model`.

- **`DocumentFile`**: Represents a file in the system.
  - Properties: `uri`, `name`, `type` (Enum), `size`, `lastModified`, `path`.
  - **`DocumentType`**: Enum for supported file types (PDF, DOCX, PPTX, XLSX, etc.) with helper methods `fromExtension` and `fromMimeType`.
- **`ConversionResult`**: Encapsulates the result of a file conversion operation.
  - Properties: `success`, `outputPath`, `errorMessage`.
- **`ConversionOptions`**: Configuration for conversion tasks (source/target format, quality, OCR enabled).
- **`ScanResult`**: Represents the result of a document scan.
  - Contains `ScannedPage` list (bitmap, corners, extracted text) and optional `pdfPath`.

### Repositories
Located in `com.officesuite.app.data.repository`.

- **`DocumentConverter`**: Handles file format conversions using libraries like Apache POI and iText.
  - **Key Methods**:
    - `convert(inputFile, options)`: Main entry point for conversions.
    - `convertPdfToDocx`: Converts PDF pages to images and embeds them in a DOCX file.
    - `convertPdfToPptx`: Similar to above, but for PowerPoint.
    - `convertDocxToPdf`: Converts DOCX to HTML intermediate, then to PDF using iText.
    - `createSearchablePdfWithOcr`: Creates a PDF from images with invisible text layer from OCR.
    - `convertImagesToPdf`, `convertHtmlToPdf`.
- **`PreferencesRepository`**: Manages app preferences (not detailed here but implied usage).

---

## Core Feature Modules

### PDF Module
Located in `com.officesuite.app.pdf` and `com.officesuite.app.ui.pdf`.

- **`PdfViewerFragment`**: The main UI for viewing PDFs.
  - Features: Lazy loading of pages, swipe navigation, text selection/copy, search, pip mode.
  - Uses `PdfPagesAdapter` for RecyclerView-based page rendering.
- **`PdfPagesAdapter`**: Adapts PDF pages for `RecyclerView`.
  - Uses `PdfRenderer` to render pages to Bitmaps.
  - Implements caching via `MemoryManager`.
- **`ZoomableImageView`**: Custom view handling pinch-to-zoom and panning for PDF pages.
- **`PdfSecurityManager`** (`com.officesuite.app.security`): Handles PDF encryption and password protection.
  - `encryptPdf`: Applies password protection and permissions (print, copy, modify).
  - `decryptPdf`: Removes password protection.

### Office Documents
Handles Word, Excel, and PowerPoint files using Apache POI.

- **Word (`ui/docx`)**:
  - **`DocxViewerFragment`**: Converts DOCX to HTML for rendering in a `WebView`.
  - Handles text formatting (bold, italic, color), tables, and images.
- **Excel (`ui/xlsx`)**:
  - **`XlsxViewerFragment`**: Converts XLSX sheets to HTML tables for display.
  - Supports multiple sheets, cell formatting, and numeric data.
- **PowerPoint (`ui/pptx`)**:
  - **`PptxViewerFragment`**: Renders slides as images.
  - **`PresentationModeActivity`**: Full-screen presentation mode with timer and navigation controls.

### Scanner & OCR
Located in `com.officesuite.app.ui.scanner` and `com.officesuite.app.ocr`.

- **`ScannerFragment`**: Camera interface for capturing documents.
  - Supports modes: Document, QR Code, Business Card, ID Document, Receipt.
  - Features: Auto-border detection, perspective crop, filters (grayscale, contrast).
- **`DocumentBorderDetector`**: Detects document edges in a bitmap.
- **`OcrManager`**: Wrapper around Google ML Kit Text Recognition.
  - Supports multiple languages (Latin, Chinese, Japanese, Korean, Devanagari).
  - `extractText`: Returns `OcrResult` with text blocks and bounding boxes.
- **Specialized Scanners**: `BarcodeScanner`, `BusinessCardScanner`, `IdDocumentScanner`, `ReceiptScanner`.

---

## Advanced Features

### AI & Intelligence
- **`DocumentAnalyzer`** (`ai/`): (Implied) Likely handles content analysis for smart features.

### Productivity & Writing
- **`ProductivityManager`** (`productivity/`): Tracks user stats for gamification.
  - Features: Reading/Writing goals, streaks, achievement badges (e.g., "Bookworm", "Wordsmith").
  - Tracks detailed stats like total words written, documents edited.
- **`FocusModeManager`** (`writing/`): Provides distraction-free writing environment.
  - Hides system bars, dims background, enables "typewriter mode" (centering active line).
- **`MarkdownFragment`** (`ui/markdown`): Full WYSIWYG Markdown editor with live preview using `Markwon`.

### Platform Integration
Located in `com.officesuite.app.platform`.

- **`DesktopModeManager`**: Optimizes experience for Samsung DeX and ChromeOS.
  - Handles keyboard shortcuts (Ctrl+S, Ctrl+Z, etc.) and mouse gestures.
  - Manages window configuration and multi-window support.
- **`DigitalWellbeingManager`**: Tracks app usage time.
  - Features: Daily limits, break reminders, usage statistics (today/weekly).
- **`NearbyShareManager`**: Integrates with Android Nearby Share for file transfer.
- **`QuickSettingsManager`**: Manages Quick Settings tiles (Scanner, Create, Convert).
- **`VoiceCommandManager`**: Integration with Google Assistant/Voice interaction.
  - Commands: "Scan document", "Open file", "Read aloud", etc.

### Automation & Search
- **`DocumentAutomation`** (`automation/`):
  - **Mail Merge**: Merges data from XLSX into DOCX templates.
  - **Batch Processing**: Applies operations (convert, watermark, rename) to multiple files.
  - **Workflows**: Defines multi-step document processing.
- **`DocumentIndexingService`** (`search/`): Background service for full-text indexing.
  - Enables instant offline search across documents.

---

## Cross-Cutting Concerns

### Security
- **`PdfSecurityManager`**: Encrypt/Decrypt PDFs with AES encryption.

### Cloud & Storage
- **`CloudStorageManager`** (`cloud/`): Abstracts cloud providers (Google Drive, Dropbox, OneDrive).
  - Includes `LocalStorageProvider` for local file management simulating cloud structure.
  - Features: Upload, download, sync, file listing.

### Performance
- **`LazyFontManager`** (`performance/`): Loads fonts on-demand to save memory and startup time.
- **`MemoryManager`** (utils): Caching mechanism for bitmaps (used in PDF rendering).

### Accessibility
- **`AccessibilityManager`**: (Implied) Manages accessibility features like screen reader support or high contrast.

### Analytics
- **`UsageAnalyticsManager`** (`analytics/`): Tracks feature usage and user engagement privacy-compliantly.
  - Generates daily stats and weekly summaries.

### Social & Collaboration
- **`CommentThreadingManager`** (`social/`): Supports nested comments and @mentions in documents.
- **`DocumentReactionsManager`** (`social/`): Allows emoji reactions on documents.

### Developer Tools
- **`CommandPalette`** (`developer/`): A Ctrl+Shift+P style command launcher for power users and developers to access all app actions quickly.

---

## UI Layer
The UI is built using Android Fragments and ViewBinding. Key components include:
- `MainActivity`: Host activity handling navigation.
- `HomeFragment`: Dashboard with recent files and quick actions.
- `SettingsFragment`: App configuration.
- `PlatformFeaturesFragment`: Settings for DeX, Wellbeing, etc.

---
