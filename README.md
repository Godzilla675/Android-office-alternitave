# Office Suite - Android Document Viewer & Editor

A lightweight, feature-rich office suite for Android that provides comprehensive document viewing, editing, scanning, and conversion capabilities.

## Features

### 📄 Document Viewing
- **PDF Reader** - Full-featured PDF viewer with page navigation, zoom, and scrolling
- **Word Documents** - View DOCX/DOC files with formatting support
- **PowerPoint** - View PPTX/PPT presentations with slide navigation
- **Excel** - View XLSX/XLS spreadsheets
- **Markdown** - View and edit Markdown files with live preview

### ✏️ Document Editing

#### PDF Editor
- **Freehand Drawing** - Draw directly on PDF pages with customizable pen
- **Highlighter Tool** - Highlight text and areas with translucent colors
- **Shape Tools** - Add rectangles, circles, arrows, and lines
- **Text Annotations** - Add text notes anywhere on the document
- **Eraser Tool** - Remove annotations easily
- **Color Picker** - Choose from 24 vibrant colors
- **Stroke Width Control** - Adjust line thickness
- **Undo/Redo** - Full undo/redo support for all actions
- **Page Navigation** - Navigate between pages while preserving annotations

#### PowerPoint Editor
- **Slide Annotations** - Draw and annotate on presentation slides
- **Shape Tools** - Add rectangles, circles, arrows, and lines
- **Text Additions** - Add text annotations to slides
- **Image Insertion** - Insert images into presentations
- **Slide Management** - Add new slides to presentations
- **Color & Stroke Controls** - Customize annotation appearance
- **Undo/Redo Support** - Full editing history

#### Word Document Editor
- **Rich Text Editing** - Full WYSIWYG text editor
- **Text Formatting** - Bold, italic, underline support
- **Text Alignment** - Left, center, right alignment
- **Font Size Control** - Multiple font size options
- **Text Color** - Change text foreground color
- **Highlight Color** - Add background color to text
- **Bullet Lists** - Insert bullet point lists
- **Numbered Lists** - Insert numbered lists
- **Image Insertion** - Add images to documents
- **Undo/Redo** - Full editing history support
- **Save As DOCX** - Export documents in Word format

#### Markdown Editor
- Full markdown editor with formatting toolbar and live preview
- Formatting Support - Bold, italic, headings, lists, links, code blocks

### 🔄 File Conversion
- **PDF to DOCX** - Convert PDF documents to Word format
- **PDF to PPTX** - Convert PDF to PowerPoint presentations
- **DOCX to PDF** - Convert Word documents to PDF
- **PPTX to PDF** - Convert presentations to PDF
- **Markdown to PDF** - Export markdown as PDF
- **Text to PDF** - Convert plain text to PDF

### 📸 Document Scanner
- **Camera Scanning** - Capture documents using device camera
- **Auto Border Detection** - Automatic edge detection and cropping
- **Image Filters** - Grayscale and contrast enhancement
- **Multi-page Support** - Scan multiple pages into single document
- **Save as PDF** - Export scanned pages to PDF

### 🔍 OCR (Optical Character Recognition)
- **Google ML Kit** - Powered by Google's ML Kit Text Recognition
- **Text Extraction** - Extract text from scanned images
- **Copy to Clipboard** - Easy text copying for further use

### 📤 Sharing
- **Share Documents** - Share files with other apps
- **Open With** - Open files in external applications
- **Export Options** - Multiple format export options

## Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM with ViewBinding
- **UI**: Material Design 3
- **Navigation**: Android Navigation Component
- **PDF Viewer**: android-pdf-viewer library
- **Office Documents**: Apache POI
- **OCR**: Google ML Kit
- **Camera**: CameraX
- **Markdown**: Markwon

## Requirements

- Android 7.0 (API 24) or higher
- Camera permission for document scanning
- Storage permission for file access

## Building the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on device/emulator

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/officesuite/app/
│   │   ├── data/
│   │   │   ├── model/          # Data classes
│   │   │   └── repository/     # Document converter
│   │   ├── ocr/                # OCR functionality
│   │   ├── ui/
│   │   │   ├── converter/      # File conversion UI
│   │   │   ├── docx/           # DOCX viewer
│   │   │   ├── editor/         # Document editors (PDF, PPTX, DOCX)
│   │   │   ├── home/           # Home screen
│   │   │   ├── markdown/       # Markdown editor
│   │   │   ├── pdf/            # PDF viewer
│   │   │   ├── pptx/           # PPTX viewer
│   │   │   └── scanner/        # Document scanner
│   │   └── utils/              # Utility classes
│   └── res/
│       ├── drawable/           # Icons and drawables
│       ├── layout/             # UI layouts
│       ├── menu/               # Menu resources
│       ├── navigation/         # Navigation graph
│       └── values/             # Strings, colors, themes
```

## Permissions

The app requires the following permissions:
- `CAMERA` - For document scanning
- `READ_EXTERNAL_STORAGE` - For file access
- `WRITE_EXTERNAL_STORAGE` - For saving files
- `INTERNET` - For ML Kit model downloads

## License

This project is open source and available under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit pull requests.