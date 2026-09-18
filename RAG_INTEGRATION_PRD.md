# Road Anomaly Detection App - RAG Integration PRD
## Comprehensive Analysis & Product Requirements Document

**Document Version:** 1.0  
**Date:** September 18, 2026  
**Status:** Analysis & Planning Phase  

---

## A. REPOSITORY ANALYSIS

### A.1 Project Overview

**Mobile Application (`road-anomaly-classification-mobile-app-main`):**
- **Purpose:** Real-time road anomaly detection system on Android using edge AI
- **Primary Function:** Detect potholes and speed bumps using accelerometer/gyroscope sensors
- **Architecture:** MVVM + Clean Architecture with Jetpack Compose UI
- **Language:** Kotlin
- **Deployment Target:** Android 7.0+ (API 24+), Target API 34

**ML Pipeline (`road-anomaly-classification-ml-pipeline-main`):**
- **Purpose:** Training and optimization of the 1D-CNN model for road anomaly classification
- **Model Type:** InceptionTime1D + Squeeze-and-Excitation (SE Block)
- **Language:** Python (PyTorch)
- **Output:** ONNX format for Android deployment
- **Performance:** Macro F1-Score = 0.86 on holdout test set

### A.2 Technology Stack

**Mobile App:**
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Language & Runtime:** Kotlin 1.9+, JVM Target 17
- **ML Inference:** ONNX Runtime Android 1.18.0 (with NNAPI hardware acceleration)
- **Database:** Room (local persistence for trips & metadata)
- **Networking:** Retrofit 2.9+ + OkHttp (for API communication)
- **Async:** Coroutines + StateFlow (Kotlin Flow API)
- **Dependency Injection:** Hilt (Android)
- **Background Tasks:** WorkManager + Foreground Services
- **Maps:** Google Maps SDK for Android (via maps-compose)
- **Preferences:** DataStore Preferences
- **Firebase:** Crashlytics & Analytics (for error reporting)
- **Build System:** Gradle 8.x with Kotlin DSL

**ML Pipeline:**
- **Language:** Python 3.13
- **ML Framework:** PyTorch 2.12
- **Export Format:** ONNX (via TorchScript)
- **HPO:** Optuna (Bayesian hyperparameter optimization)
- **Data Processing:** NumPy, Pandas, SciPy
- **Evaluation:** scikit-learn (metrics & validation)

### A.3 Project Structure

```
Mobile App (Road Anomaly Classification Android):
├── app/src/main/java/com/pemalang/roaddamage/
│   ├── data/                    # Data layer (Room DAO, Retrofit, DataStore)
│   ├── di/                      # Hilt dependency injection modules
│   ├── domain/                  # Business logic (ONNX inference, sensor fusion)
│   │   ├── OnnxModelRunner.kt   # ONNX Runtime wrapper
│   │   ├── SensorFusionProcessor.kt  # Sensor data aggregation & preprocessing
│   │   ├── ButterworthFilter.kt # Digital signal filtering
│   │   └── ThresholdConfigReader.kt  # Model threshold configuration
│   ├── model/                   # Data classes (SensorReading, Trip, AnomalyEvent)
│   ├── recording/               # Recording service & lifecycle management
│   ├── sensors/                 # Hardware sensor handlers (Accel, Gyro, GPS)
│   ├── ui/                      # Jetpack Compose screens & components
│   │   ├── screens/             # Main screens (Dashboard, TripDetail, etc)
│   │   ├── components/          # Reusable UI components
│   │   ├── navigation/          # Navigation graph setup
│   │   └── theme/               # Material Design 3 theming
│   ├── util/                    # Utility functions (Distance calculation)
│   └── work/                    # Background worker for auto-upload

ML Pipeline:
├── src/
│   ├── dataset/                 # Data preparation & ground truth generation
│   │   ├── 001_labeling.py      # Peak detection & event clustering
│   │   ├── 002_generate_shared_background.py  # Background sampling
│   │   ├── 003_build_cnn_data.py  # Windowing (200 samples = 2sec @ 100Hz)
│   │   ├── sensor_fusion.py     # Causal filtering, gravity separation
│   │   └── feature_extraction.py # 88 hand-crafted features for XGBoost
│   ├── cnn_model/               # 1D-CNN training & export
│   │   ├── 005_optuna_tune.py   # Bayesian HPO
│   │   ├── 006_train.py         # Model training with K-Fold + Holdout
│   │   ├── 007_export_onnx.py   # Export to ONNX with MobileInferenceWrapper
│   │   ├── 008_benchmark_onnx.py # Latency benchmarking
│   │   └── model.py             # InceptionTime1D architecture
│   ├── xgboost_model/           # Classical ML baseline
│   ├── utils/                   # Shared utilities & config
│   └── tests/                   # Pytest suite (31 tests)
└── evaluation/                  # Reports, confusion matrices, analysis
```

### A.4 Architecture Overview

**Mobile App - Data Flow:**

```
┌─────────────────────────────────────────────────────────────┐
│                    RECORDING SESSION                         │
├─────────────────────────────────────────────────────────────┤

1. SENSOR INPUT LAYER
   ├─ Accelerometer (100Hz sampling)
   ├─ Gyroscope (100Hz sampling)
   ├─ Gravity Sensor (preprocessed)
   ├─ Linear Acceleration (preprocessed)
   └─ GPS (variable rate)

2. SENSOR FUSION PROCESSOR
   ├─ Time-based resampling to 100Hz (10ms intervals)
   ├─ Maximum gap tolerance: 50ms (prevents hallucinations)
   ├─ Butterworth low-pass filtering (causal, zero-lookahead)
   └─ Sliding window buffer (200 samples = 2.0 seconds)

3. INFERENCE PIPELINE
   ├─ Input: [Batch=1, Channels=7, Length=200]
   │  Channels: [accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z, speed]
   ├─ ONNX Model (1D-CNN with NNAPI acceleration)
   ├─ Output: [prob_none, prob_pothole, prob_speedbump]
   └─ Threshold: Default 0.50 (argmax)

4. RECORDING & PERSISTENCE
   ├─ Real-time CSV logging (sensor readings + predictions)
   ├─ Room Database (Trip metadata, AnomalyEvent records)
   ├─ Local file storage (CSV exports)
   └─ Forward-filled prediction probabilities per sample

5. BACKGROUND UPLOAD
   ├─ WorkManager triggers on Wi-Fi connection (unmetered)
   ├─ Multipart upload: CSV file + JSON metadata
   ├─ API Endpoint: POST /api/trips/upload
   └─ Retry logic with exponential backoff

└─ Trip data available in: Room Database + Local CSV files
```

**Key Components:**

| Component | Location | Purpose | Tech |
|-----------|----------|---------|------|
| **OnnxModelRunner** | `domain/` | ONNX model initialization & inference | ONNX Runtime Android |
| **SensorFusionProcessor** | `domain/` | Sensor data aggregation, windowing, preprocessing | Kotlin Coroutines, Flow |
| **RecordingService** | `recording/` | Lifecycle management for active trips | Android Foreground Service |
| **TripDetailScreen** | `ui/screens/` | Display trip results & anomalies | Jetpack Compose |
| **WorkManager** | `work/` | Background auto-upload worker | Android WorkManager |
| **ApiService** | `data/remote/` | HTTP client for server communication | Retrofit 2 |
| **TripDao** | `data/local/` | Database queries for trips & anomalies | Room ORM |

### A.5 Existing Features

**Detection & Classification:**
- Real-time 1D-CNN inference (on-device, no cloud required)
- Multi-class classification: None / Pothole / Speed Bump
- Hardware-accelerated inference via NNAPI (< 5ms latency per window)
- Probability scores per class (forward-filled across samples)

**Recording & Data Capture:**
- 100Hz sensor sampling (accelerometer, gyroscope)
- Real-time visualization of sensor waveforms
- GPS integration with geospatial metadata (lat, lng, altitude, speed, bearing)
- CSV export format (Pandas-compatible)
- Automatic anomaly event detection and logging

**Data Management:**
- Local Room database for trip metadata
- DataStore for user settings & preferences
- File system storage for CSV data
- Query interface to retrieve trips by date/status

**User Interface:**
- Material Design 3 (Jetpack Compose)
- Dashboard with statistics (trips, distance, GPS status, pending uploads)
- Active session screen with real-time graphs
- Trip detail view with map visualization of route
- List of historical trips with filter/search
- Settings screen for sensor configuration

**Backend Integration:**
- Multipart upload to server (CSV + JSON metadata)
- Retrofit-based API client
- WorkManager for background upload with retry logic
- Upload status tracking (Pending/Uploading/Uploaded/Failed)

**Quality & Reliability:**
- Firebase Crashlytics for error reporting
- Structured logging with proper error handling
- Model threshold configuration (customizable via config file)
- Battery optimization with WakeLock management
- Sensor data validation (gap detection, resampling logic)

### A.6 Data Flow & Storage

**Sensor Data:**
- **Origin:** Android hardware sensors (Accelerometer, Gyroscope, GPS)
- **Processing:** SensorFusionProcessor (time-based resampling to 100Hz)
- **Storage:** 
  - In-memory: Ring buffer (200 samples)
  - Persistent: CSV file (per-trip data export)
  - Database: Room entities (Trip, AnomalyEvent metadata)
- **Format:** CSV with columns: timestamp, accel_x, accel_y, accel_z, gyro_x, gyro_y, gyro_z, latitude, longitude, altitude, speed, accuracy, bearing, prob_none, prob_pothole, prob_speedbump

**Model Predictions:**
- **Generated:** OnnxModelRunner (1D-CNN inference)
- **Per-window:** 3 probabilities (None, Pothole, SpeedBump)
- **Storage:** CSV forward-fill (prediction probabilities per sample), Room database for anomaly events
- **Persistence:** Linked to Trip entity

**Trip Metadata:**
- **Storage:** Room Database (trips table)
- **Schema:** tripId, userId, startTime, endTime, duration, distance, dataFilePath, uploadStatus, createdAt
- **Retrieval:** TripDao (query by trip ID, date range, upload status)

**Backup & Export:**
- **Manual Export:** User can export trip CSV from app
- **Automatic Upload:** WorkManager triggers upload on Wi-Fi
- **API Endpoint:** POST /api/trips/upload (multipart: userId, tripId, metadata JSON, CSV file)

### A.7 Backend Integration (Current State)

**API Service:**
- **Endpoint:** `POST /api/trips/upload`
- **Method:** Multipart form data
- **Parameters:**
  - `userId`: User identifier (RequestBody)
  - `tripId`: Trip identifier (RequestBody)
  - `metadata`: JSON metadata (RequestBody)
  - `file`: CSV sensor data (MultipartBody.Part)
- **Response:** `UploadResponse { success: Boolean, message: String? }`

**Current Backend Status:** **BELUM DIKETAHUI** (Unknown)
- No backend source code provided in either repository
- API endpoint URL configuration: **BELUM DIKETAHUI**
- Backend technology stack: **BELUM DIKETAHUI**
- Database schema on server: **BELUM DIKETAHUI**
- Authentication/Authorization mechanism: **BELUM DIKETAHUI**
- Current data usage on server: **BELUM DIKETAHUI**

**Assumptions about Backend:**
- Some form of REST API exists at `/api/trips/upload`
- Server accepts multipart uploads and stores trip data
- Server likely processes uploaded CSV for further analysis
- No authentication headers currently visible in ApiService (potential security gap)

### A.8 Environment Configuration

**Found in Code:**
- **Maps API Key:** Configurable via `local.properties` (MAPS_API_KEY)
- **API Base URL:** **BELUM DIKETAHUI** (not visible in provided code)
- **Server Endpoint:** Hardcoded in Retrofit configuration (not visible in provided code)

**Missing Configuration:**
- API base URL for Retrofit
- LLM API keys/endpoints (needed for RAG)
- Vector database connection details (needed for RAG)
- Authentication credentials

### A.9 Components Likely Affected by RAG Integration

**Direct Impact:**
1. **OnnxModelRunner** - May need augmentation with context retrieval for explanations
2. **TripDetailScreen** - Primary UI for displaying RAG-powered insights
3. **ApiService** - New endpoints needed for RAG queries & knowledge base management
4. **Data Layer** - New persistence for embeddings, RAG cache
5. **Domain Layer** - New RAG processor & context aggregation use cases

**Indirect Impact:**
1. **RecordingService** - May query RAG for real-time contextual info
2. **SensorFusionProcessor** - Data processing might feed RAG for enrichment
3. **Database** - Additional tables for knowledge base, embeddings, metadata
4. **Networking** - Additional HTTP calls for RAG API
5. **UI Theme** - New design patterns for displaying contextual information

**No Direct Impact:**
- Sensor hardware handlers (unchanged)
- GPS handler (unchanged)
- Local storage file system (only new RAG-specific files added)
- ONNX model format (unchanged for core inference)

---

## B. RAG OPPORTUNITY ANALYSIS

### B.1 Problems RAG Could Solve

**Problem 1: Lack of Contextual Understanding**
- **Current State:** App detects "pothole at coordinate X" but provides no context
- **User Pain:** Driver doesn't know severity, repair status, historical patterns, or if it's a known issue
- **RAG Solution:** Query knowledge base for: previous detections at same location, repair history, road condition reports, traffic patterns

**Problem 2: No Actionable Recommendations**
- **Current State:** App shows raw detection results without guidance
- **User Pain:** Drivers unsure what to do with detection (report? avoid? continue?)
- **RAG Solution:** Retrieve best practices, repair authority contacts, road maintenance schedules

**Problem 3: Explainability Gap**
- **Current State:** Model outputs probability, but user doesn't understand WHY it detected anomaly
- **User Pain:** False positives reduce trust; users can't distinguish high-confidence vs low-confidence detections
- **RAG Solution:** Retrieve similar historical cases, sensor signature patterns, contributing factors

**Problem 4: Data Isolation & Pattern Loss**
- **Current State:** Each device operates independently; patterns across fleet are not visible
- **User Pain:** Repeated reporting of same issues; no fleet-wide insights
- **RAG Solution:** Aggregate historical detections, show location hotspots, track repairs over time

**Problem 5: Maintenance Planning Gap**
- **Current State:** Authorities have raw detection data but no automated insights for maintenance prioritization
- **User Pain:** High-impact roads might be ignored; low-priority roads might be over-maintained
- **RAG Solution:** Query patterns, retrieve historical maintenance data, generate prioritization recommendations

### B.2 Use Cases for RAG Integration

**Use Case 1: Contextual Anomaly Details (User-Facing)**
```
User detects pothole on Trip Detail Screen
→ App queries RAG with: anomaly_type="pothole", location, sensor_signature
→ RAG retrieves: 
   - Similar historical detections at this location (count, dates, confidence)
   - Previous repair records (date, completion status)
   - Severity assessment (based on sensor magnitude patterns)
   - User reports associated with this road segment
→ Display contextual card: "High Severity • Reported 15x in past 6 months • Last repair: Mar 2026"
→ Actionable links: "Report to Authority", "View Repair History"
```

**Use Case 2: Real-Time Recommendations (During Active Trip)**
```
User is recording a trip
→ Model detects speed bump with 85% confidence
→ App queries RAG with: anomaly_type="speedbump", location, confidence_score
→ RAG retrieves: 
   - Speed limit for this road
   - Average speed bump height patterns
   - Traffic advisories
   - Maintenance schedule (is this expected/unexpected?)
→ Display toast: "Speed Bump Ahead (Expected) • Reduce Speed" + link to details
```

**Use Case 3: Fleet-Wide Insights (Analytics/Admin)**
```
Road authority wants to understand road condition trends
→ Admin queries RAG with: region, date_range, anomaly_types
→ RAG retrieves & aggregates:
   - Heatmap of anomaly concentrations
   - Temporal trends (worsening vs improving roads)
   - Correlation with weather, traffic, maintenance history
   - Cost-benefit analysis for repairs
→ Display dashboard: Maps showing priority repair zones, time-series charts
```

**Use Case 4: Model Explainability & Debugging**
```
User sees low-confidence prediction (50% pothole, 40% none)
→ App queries RAG with: sensor_signature, model_uncertainty_metrics
→ RAG retrieves:
   - Similar ambiguous cases from historical data (show examples)
   - Contributing factors (which sensor channels were most significant?)
   - Common confusion patterns (pothole vs pothole-like vibrations)
→ Display: "Uncertain Detection: This could be... [explain factors]"
```

**Use Case 5: Offline Support & Guidance (Pre-Trip Planning)**
```
User is planning a trip through unfamiliar area
→ User queries RAG with: route, historical interests (e.g., "show me bad roads")
→ RAG retrieves:
   - Historical anomaly hotspots on planned route
   - Expected road conditions
   - Recent maintenance or repair reports
   - Local driving advisories
→ Display: "Road Quality Preview" map overlay
```

### B.3 Knowledge Sources for RAG Knowledge Base

**Priority 1: Historical App Data (High Availability, High Relevance)**
- CSV trip data uploaded from all users
- Detected anomalies with timestamps, locations, confidence scores
- Sensor signatures (feature vectors for ML matching)
- User device/environment metadata (phone model, mounting orientation, etc)

**Priority 2: Server-Side Processed Data (Medium Availability, High Relevance)**
- Aggregated anomaly hotspots (location-based clustering)
- Repair/maintenance records (if available from authority)
- Ground truth labels (if manually verified)
- Time-series trends by location

**Priority 3: External Data Sources (Medium Availability, Medium Relevance)** - **REQUIRES EXTERNAL API INTEGRATION**
- Road condition reports (OpenStreetMap, Google Maps, local government APIs)
- Traffic data (Waze, Google Maps traffic layer)
- Weather data (precipitation, temperature - correlates with road degradation)
- Maintenance schedule data (from municipal authorities)
- Speed limits & road classification data

**Priority 4: Document-Based Knowledge (Low Availability, Medium Relevance)** - **REQUIRES INDEXING**
- Road maintenance best practices (PDFs, guidelines)
- Municipality repair procedures documentation
- Driver safety guidelines
- Sensor calibration documentation
- Model architecture papers (for explainability)

**Priority 5: Real-Time External APIs (High Latency, Medium Relevance)**
- Current traffic conditions (Google Maps API)
- Weather service (OpenWeatherMap, WeatherAPI)
- Reverse geocoding (location to address)
- Live maintenance status (municipality APIs if available)

### B.4 Proposed User Interaction Flow with RAG

**Main Trip Detail Screen (Enhanced with RAG):**

```
┌──────────────────────────────────────────────────────────────┐
│ Trip: Downtown Patrol Route                    [Back] [Share] │
├──────────────────────────────────────────────────────────────┤
│ Date: Sep 18, 2026  Duration: 45 min  Distance: 12.5 km      │
├──────────────────────────────────────────────────────────────┤
│ [MAP with route and anomaly markers]                          │
├──────────────────────────────────────────────────────────────┤
│ DETECTED ANOMALIES: 5 Total                                   │
├──────────────────────────────────────────────────────────────┤

│ 🔴 Pothole (HIGH CONFIDENCE: 92%)                             │
│    Location: Jl. Sudirman, Km 2.3                             │
│    Time: 14:23:45                                             │
│    Sensor Magnitude: 1.8G (significant vibration)             │
│                                                               │
│    [CONTEXTUAL INSIGHTS] ◄ RAG-POWERED                        │
│    • Historical: Detected 8x in past 90 days                  │
│    • Severity Trend: WORSENING (larger magnitude over time)   │
│    • Last Repair: 60 days ago                                 │
│    • Status: PRIORITY #2 in this area                         │
│    • Similar Cases: 23 (show distribution of confidence scores) │
│                                                               │
│    [Action Buttons]                                           │
│    [Report to Authority] [View History] [Details]             │

│ 🟡 Speed Bump (MEDIUM CONFIDENCE: 68%)                        │
│    Location: Jl. Pemuda, Km 5.1                               │
│    Time: 14:31:22                                             │
│    Sensor Magnitude: 1.2G                                      │
│                                                               │
│    [CONTEXTUAL INSIGHTS]                                      │
│    • Historical: Expected (known speed bump)                  │
│    • Confidence: UNCERTAIN (68% - borderline)                 │
│    • Contributing Factors: Low gyro signal, high accel z-axis │
│    • Similar Ambiguous Cases: 12 (possible sensor orientation) │
│    • Recommendation: Likely correct, but monitor orientation   │
│                                                               │
│    [Action Buttons]                                           │
│    [Confirm / Reject] [View Similar Cases] [Help]             │

└──────────────────────────────────────────────────────────────┘
```

### B.5 Distinction: RAG vs Other Approaches

**Why RAG, not just rules/queries?**

| Need | Rule-Based | Database Query | RAG | Winner |
|------|-----------|---|---|-------|
| Pattern matching ("similar cases") | ❌ Hard to define rules | ⚠️ Exact match only | ✅ Semantic similarity | **RAG** |
| Trend analysis ("worsening road") | ⚠️ Possible with logic | ✅ SQL aggregates | ✅ With LLM reasoning | **RAG** |
| Textual context ("repair notes") | ❌ No | ⚠️ Text search | ✅ Semantic search | **RAG** |
| Explainability ("why this score") | ❌ No | ❌ No | ✅ LLM generates | **RAG** |
| Threshold-based alerts | ✅ Simple | ✅ SQL triggers | ⚠️ Overkill | **Rules** |
| Free-form Q&A support | ❌ No | ❌ No | ✅ Yes | **RAG** |

**Why NOT pure LLM (no RAG)?**
- ❌ Hallucination risk: LLM might invent fake repair records or incorrect statistics
- ❌ Latency: 2-3 seconds per response (unacceptable for mobile UI)
- ❌ Cost: API calls for every interaction (expensive at scale)
- ❌ Privacy: Sending all trip data to external LLM API

**Why NOT pure database?**
- ❌ Can't do semantic similarity search
- ❌ Can't generate explanations
- ❌ Can't handle ambiguous/fuzzy queries
- ❌ Limited flexibility for new use cases

**RAG provides the sweet spot:**
- ✅ Grounded in actual data (no hallucination)
- ✅ Fast local LLM inference (if using on-device LLM)
- ✅ Semantic search + vector similarity
- ✅ Flexible reasoning over retrieved context

### B.6 Irrelevant Use Cases (Where RAG is NOT Needed)

- **Real-time model inference:** Continue using ONNX (not RAG)
- **Sensor data validation:** Use signal processing checks (not RAG)
- **Upload status tracking:** Use database queries (not RAG)
- **User settings management:** Use DataStore (not RAG)
- **Authentication:** Use auth service (not RAG)

---

## C. PROPOSED RAG ARCHITECTURE

### C.1 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ROAD ANOMALY DETECTION SYSTEM                         │
│                          WITH RAG INTEGRATION                            │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐
│   ANDROID CLIENT APP     │
├──────────────────────────┤
│ • Trip Detail Screen     │
│ • Active Session Display │
│ • Analytics Dashboard    │
└───────────┬──────────────┘
            │ HTTP REST Calls
            │ (Bearer Token Auth)
            ▼
┌──────────────────────────────────────────────────────┐
│          BACKEND SERVER (NEW or ENHANCED)            │
├──────────────────────────────────────────────────────┤
│                                                      │
│ ┌──────────────────────────────────────────┐        │
│ │  API GATEWAY & ROUTING                   │        │
│ │  • /api/trips/upload (existing)          │        │
│ │  • /api/rag/query (new)                  │        │
│ │  • /api/rag/context (new)                │        │
│ │  • /api/rag/explain (new)                │        │
│ │  • /api/anomaly/{id}/details (new)       │        │
│ └──────────────────────────────────────────┘        │
│            │                                        │
│            ▼                                        │
│ ┌──────────────────────────────────────────┐        │
│ │  RAG ORCHESTRATION LAYER                 │        │
│ │  • Query router                          │        │
│ │  • Context aggregator                    │        │
│ │  • LLM prompt builder                    │        │
│ │  • Response formatter                    │        │
│ └──────────────────────────────────────────┘        │
│            │                                        │
│     ┌──────┴──────┬──────────────┬──────────┐      │
│     │             │              │          │      │
│     ▼             ▼              ▼          ▼      │
│ ┌────────┐  ┌──────────┐  ┌─────────┐  ┌──────┐  │
│ │ LOCAL  │  │ VECTOR   │  │  SQL    │  │ LLM  │  │
│ │ LLM    │  │DATABASE  │  │DATABASE │  │ API  │  │
│ │        │  │          │  │         │  │      │  │
│ │(Ollama │  │(Pgvector,│  │(Trip,   │  │(if   │  │
│ │or      │  │FAISS, or │  │Anomaly, │  │used  │  │
│ │Llama.  │  │ Pinecone)│  │Metadata)│  │)     │  │
│ │cpp)    │  │          │  │         │  │      │  │
│ └────────┘  └──────────┘  └─────────┘  └──────┘  │
│                                                    │
│ ┌──────────────────────────────────────────┐      │
│ │  DATA LAYER                              │      │
│ │  • Trip CSV data                         │      │
│ │  • Embeddings cache                      │      │
│ │  • Processed anomaly records             │      │
│ │  • Metadata & associations               │      │
│ └──────────────────────────────────────────┘      │
│                                                    │
│ ┌──────────────────────────────────────────┐      │
│ │  EXTERNAL INTEGRATIONS (OPTIONAL)        │      │
│ │  • Google Maps API (geospatial)          │      │
│ │  • OpenWeatherMap (weather context)      │      │
│ │  • Municipality APIs (maintenance data)  │      │
│ └──────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────┘

Key RAG Components:
1. LOCAL LLM: For on-device / on-premise inference (privacy, latency)
   - Candidates: Ollama (Llama 2, Mistral, Neural Chat)
   - Alternative: Hosted closed-source API (Claude, GPT-4, Gemini)

2. VECTOR DATABASE: For semantic retrieval
   - Option A: Pgvector (PostgreSQL extension) - if SQL DB already used
   - Option B: FAISS (CPU-based, no external deps)
   - Option C: Pinecone (managed service)

3. RAG RETRIEVAL: Query execution
   - Input: User query or structured request
   - Process: Embed query → Semantic search → SQL filters → Rank results
   - Output: Top-K relevant chunks

4. LLM GENERATION: Response synthesis
   - Input: Query + Retrieved context
   - Process: Prompt construction → LLM inference
   - Output: Natural language explanation/recommendation

5. RESPONSE CACHING: Reduce latency & costs
   - Cache similar queries & responses
   - TTL-based invalidation
```

### C.2 Data Flow for RAG Workflows

**Workflow 1: User Views Trip Details (RAG-Enriched)**

```
User opens TripDetailScreen
  │
  ├─ Query anomalies from Room DB for this trip
  │   ↓ (existing functionality)
  │   Display: Basic anomaly list (type, time, location, confidence)
  │
  ├─ FOR EACH ANOMALY:
  │   ├─ Extract: anomaly_id, type, location, sensor_magnitude, confidence
  │   │
  │   ├─ HTTP POST /api/rag/context
  │   │   Request: {
  │   │     anomaly_id: "...",
  │   │     type: "pothole",
  │   │     location: {lat, lng},
  │   │     confidence: 0.92,
  │   │     sensor_features: {...}
  │   │   }
  │   │
  │   ├─ BACKEND RAG PROCESSING:
  │   │   ├─ Vector search: Find similar historical anomalies
  │   │   │   (embed sensor signature → nearest neighbors in Pgvector)
  │   │   ├─ SQL aggregates: Count detections in this location (past 90d)
  │   │   ├─ SQL query: Join with repair records (if available)
  │   │   ├─ Optional API call: Reverse geocode for address
  │   │   │
  │   │   ├─ Construct RAG context:
  │   │   │   {
  │   │   │     similar_cases: [10 most similar past detections],
  │   │   │     location_stats: {count_90d: 8, trend: "worsening"},
  │   │   │     repair_history: [...],
  │   │   │     confidence_assessment: {...}
  │   │   │   }
  │   │   │
  │   │   └─ LLM inference:
  │   │       Prompt: "Summarize this road anomaly based on context: ..."
  │   │       → LLM generates explanation & recommendations
  │   │
  │   └─ HTTP Response: {
  │         contextual_summary: "High severity, worsening trend...",
  │         similar_cases_count: 8,
  │         severity_assessment: "HIGH",
  │         recommendations: ["Report to authority", "Verify sensor orientation"],
  │         last_repair_days_ago: 60
  │       }
  │
  └─ Update UI: Display contextual card with above information
     (Show: summary, stats, action buttons)
```

**Workflow 2: Real-Time Recommendation During Active Trip**

```
ActiveSessionScreen is recording
Model detects anomaly (pothole, 0.85 confidence)
  │
  ├─ SensorFusionProcessor sends inferred data to RecordingService
  │
  ├─ RecordingService queries RAG (lightweight, quick response required)
  │   HTTP POST /api/rag/quick-context
  │   Request: {
  │     type: "pothole",
  │     location: {lat, lng},
  │     confidence: 0.85,
  │     urgency: "real-time"  ← Signals low-latency requirement
  │   }
  │   Timeout: 500ms (if exceeds, show default recommendation)
  │
  ├─ BACKEND LIGHTWEIGHT RAG:
  │   ├─ Query cached results for this location (fast)
  │   ├─ Skip slow vector search (cache-first approach)
  │   ├─ Return pre-computed context if available
  │   └─ Fallback: Return rule-based recommendation
  │
  └─ RecordingService receives response & shows toast:
     "Pothole Detected (85%) • Previous reports: 6 • Proceed with caution"
```

**Workflow 3: Data Ingestion & Embedding Pipeline**

```
[Daily/Hourly scheduled task on backend]

1. SELECT new anomalies from trips uploaded in past 24h
   
2. FOR EACH anomaly:
   ├─ Extract features:
   │   • sensor_magnitude, accel_x_variance, gyro_z_mean, etc.
   │   • Encode as embedding vector (low-dim representation)
   │   ├─ Option A: Use pretrained ML encoder (e.g., from ML pipeline)
   │   ├─ Option B: Use simple statistical features + dimensionality reduction
   │   └─ Option C: Use LLM embedding API (if available)
   │
   ├─ Store in vector DB (Pgvector):
   │   INSERT INTO anomaly_embeddings
   │   VALUES (anomaly_id, embedding_vector, metadata)
   │
   └─ Generate & cache contextual metadata:
       Aggregate stats per location: count, trend, common features, etc.

3. (Optional) Index external sources:
   ├─ Download repair reports from municipality API
   ├─ Chunk & embed documents
   └─ Store in vector DB for full-text RAG queries
```

### C.3 Retrieval & Generation Flow

**Step 1: Query Preprocessing**
```
Raw Query Input:
  type: "pothole"
  location: (6.25, 110.40)
  confidence: 0.92
  sensor_signature: [1.8, 0.3, 2.1, ...]

Preprocessing:
  ├─ Validate input
  ├─ Normalize location (geocoding check)
  ├─ Encode sensor signature as embedding vector
  ├─ Determine query intent (context vs quick vs explanation)
  └─ Output: normalized_query_embedding, metadata_filters
```

**Step 2: Multi-Source Retrieval**

```
┌─ VECTOR SEARCH (Semantic Similarity) ────────────────────┐
│  Query: Find K=5 most similar anomalies                  │
│  Execute: SELECT * FROM anomaly_embeddings               │
│           ORDER BY embedding <=> query_embedding         │
│           LIMIT 5                                        │
│  Return: Similar past detections with sensor patterns    │
└──────────────────────────────────────────────────────────┘

┌─ SQL AGGREGATION (Structured Data) ──────────────────────┐
│  Query: Location statistics                              │
│  Execute: SELECT                                         │
│    COUNT(*) as detection_count,                          │
│    AVG(sensor_magnitude) as avg_magnitude,               │
│    MAX(confidence) as max_confidence,                    │
│    MIN(created_at) as first_detection,                  │
│    DATE_TRUNC('day', created_at) as detection_date,     │
│    (current stats) - (30d ago stats) as trend            │
│  FROM anomalies                                          │
│  WHERE ST_DWithin(location, query_location, 50meters)   │
│  GROUP BY DATE_TRUNC('day', created_at)                 │
│  Return: Hotspot stats, temporal trends                  │
└──────────────────────────────────────────────────────────┘

┌─ EXTERNAL API CALLS (Optional) ──────────────────────────┐
│  If available:                                           │
│  ├─ Google Maps: Get speed limit, road type             │
│  ├─ Weather API: Get precipitation for trip date        │
│  ├─ Municipality API: Get maintenance schedule          │
│  └─ Waze/Traffic API: Get historical congestion         │
└──────────────────────────────────────────────────────────┘

Combine All Results Into RAG Context:
{
  similar_cases: [<5 most similar>],
  location_stats: {count, trend, avg_magnitude},
  external_context: {weather, road_type, speed_limit},
  metadata: {first_seen, last_repaired, priority_rank}
}
```

**Step 3: Prompt Construction & LLM Inference**

```
System Prompt:
  "You are a road condition analyst. Provide concise, actionable
   insights about detected road anomalies based on provided context.
   Always cite confidence levels and uncertainty. Avoid speculation."

Prompt Template (for explanation use case):
  """
  Detected Anomaly:
  - Type: {anomaly_type}
  - Confidence: {confidence_score}%
  - Location: {address}
  - Sensor Magnitude: {magnitude}G
  
  Historical Context:
  - Similar Cases: {count} detected
  - Location Hotspot: {stats}
  - Trend: {trend_description}
  - Last Repair: {days_ago} days ago
  
  Question: What does this anomaly indicate? Is it likely real or a false positive?
  """

LLM Response (example):
  "Based on 8 similar high-confidence detections at this location,
   this is likely a genuine pothole. The magnitude (1.8G) matches
   the severity pattern observed in previous reports. Recommend:
   1. Report to repair authority
   2. Monitor for worsening (trending worse each week)
   3. Adjust vehicle speed"
```

**Step 4: Response Formatting & Caching**

```
LLM Output: Natural language text

Format for Mobile UI:
{
  "status": "success",
  "severity": "HIGH",
  "summary": "Confirmed pothole...",
  "confidence_assessment": "High (corroborated by 8 similar cases)",
  "recommendations": [
    {icon: "report", label: "Report to Authority", action: "report://..."},
    {icon: "info", label: "View History", action: "history://..."}
  ],
  "stats": {
    "similar_cases": 8,
    "location_hotspot": true,
    "trend": "worsening",
    "days_since_repair": 60
  },
  "cache_ttl": 3600  // Re-fetch after 1 hour
}

Store Response in Cache:
  Key: "rag_context:{anomaly_id}" or "rag_context:{location}:{type}"
  TTL: 1 hour (refresh periodically)
  Invalidate when: New anomalies detected at location
```

### C.4 Android/Backend Responsibility Division

| Responsibility | Android Client | Backend Server | Rationale |
|---|---|---|---|
| **ONNX Inference** | ✅ Always | ❌ Optional | Low-latency, offline support, no server dependency |
| **Sensor Fusion** | ✅ Always | ❌ No | Real-time, hardware sensor access |
| **Trip Recording** | ✅ Always | ❌ No | Local persistence, active session management |
| **Trip Data Storage** | ✅ (Room DB) | ✅ (persistent backend) | Local for quick access, server for durability |
| **CSV Export** | ✅ File creation | ✅ Receive uploads | Client creates, server stores |
| **RAG Query Interface** | ⚠️ Invoke | ✅ Execute | Client initiates, server processes (latency-heavy) |
| **Vector Embedding** | ❌ No | ✅ Always | Expensive computation, centralized embeddings |
| **LLM Inference** | ⚠️ Optional* | ✅ Recommended | Local LLM: privacy, latency; Cloud LLM: cost, latency |
| **Embeddings Storage** | ❌ No | ✅ Always | Central repository for aggregated data |
| **Caching** | ✅ (response) | ✅ (embeddings, results) | Both layers for performance |
| **Authentication** | ✅ Obtain token | ✅ Validate token | Secure API access |
| **Rate Limiting** | ⚠️ Respect | ✅ Enforce | Server enforces; client respects |

*Optional: Small models (Llama 2 7B) can run locally, but trade-off between latency & device resources

### C.5 Recommended Architecture Options

**OPTION A: Backend-Centric RAG (Recommended for MVP)**

```
Architecture:
  Android ──HTTP─→ Backend Server
                     ├─ Postgres + Pgvector (vector DB)
                     ├─ Python FastAPI (RAG orchestration)
                     ├─ Local Ollama (or cloud LLM API)
                     └─ Cache layer (Redis)

Pros:
  ✅ All computation centralized
  ✅ Easy to update embeddings & knowledge base
  ✅ Leverage existing backend infrastructure
  ✅ Scalable (multiple server instances)
  ✅ Simple client (HTTP requests)

Cons:
  ❌ Higher server latency (network round-trip)
  ❌ Server scalability requirements increase
  ❌ Privacy concern: server sees all queries
  ❌ Dependency on server availability
  ❌ Offline mode not supported

Estimated Latency: 200-800ms (network + inference)
Estimated Server Cost: Medium (VPS or managed DB + compute)
MVP Timeline: 4-6 weeks
Tech Stack: FastAPI, Pgvector, Ollama/LLM API, Redis
```

**OPTION B: Hybrid RAG (Client + Backend)**

```
Architecture:
  Android Client
    ├─ Cache: Lightweight embeddings (JSON)
    ├─ Local vector search: FAISS (CPU, no GPU)
    ├─ Query formulation
    └─ HTTP─→ Backend (only for aggregations & LLM)
  
  Backend Server
    ├─ Postgres + Pgvector (master data)
    ├─ Generate & distribute embeddings (periodic push to clients)
    └─ LLM inference (for complex reasoning)

Pros:
  ✅ Reduced server latency (vector search local)
  ✅ Works in offline mode (with cached embeddings)
  ✅ Lower server scalability requirements
  ✅ Privacy: raw sensor data stays local
  ✅ Faster response (~100-300ms)

Cons:
  ❌ More complex client (FAISS integration)
  ❌ Embeddings must be distributed to all clients
  ❌ Android storage for embeddings (potential bloat)
  ❌ Stale embeddings if server data changes
  ❌ LLM inference still on server (no privacy benefit for that)

Estimated Latency: 100-300ms
Estimated Server Cost: Lower (less inference)
MVP Timeline: 6-8 weeks
Tech Stack: FastAPI, Pgvector, FAISS, Ollama
Android Additions: FAISS JNI bindings, embedding distribution
```

**OPTION C: Full On-Device RAG (Ambitious, Future)**

```
Architecture:
  Android Client (Standalone)
    ├─ SQLite with vector extension (or FAISS)
    ├─ Embedding model (quantized, ~50MB)
    ├─ Small LLM (Llama 2 7B quantized, ~3-4GB)
    ├─ Inference engine (ONNX Runtime for LLM too)
    └─ No server dependency for RAG

Pros:
  ✅ True offline support
  ✅ No server infrastructure needed
  ✅ Maximum privacy (all data local)
  ✅ Fastest response (<100ms)
  ✅ No latency-related UX issues

Cons:
  ❌ Huge Android app size (embeddings + LLM)
  ❌ Extreme latency issues (LLM on mobile = slow)
  ❌ High battery drain
  ❌ Limited to smaller models
  ❌ Not scalable for fleet-wide insights
  ❌ Very complex to implement

Estimated Latency: 500-2000ms (LLM inference time)
Estimated Device Cost: 2-4GB RAM, 8-10GB storage
MVP Timeline: 12+ weeks
Tech Stack: FAISS/SQLite, Ollama-Lite, ONNX Runtime
Feasibility: Low (too resource-intensive for real-time use)
```

**Recommendation: Option A (Backend-Centric RAG)**
- Clear separation of concerns
- Leverages existing backend infrastructure
- Easier iteration & updates
- Acceptable latency for user experience
- Scalable for future fleet analytics

---

## D. MVP DEFINITION

### D.1 MVP Scope

**In Scope (Must-Have for MVP):**

1. **Single Anomaly Context Retrieval**
   - Endpoint: `POST /api/rag/anomaly-context`
   - Input: anomaly_id (or type + location + timestamp)
   - Output: Contextual summary with:
     - Similar historical detections (count, time range)
     - Location hotspot statistics (past 90 days)
     - Severity assessment
     - Basic recommendations
   - LLM used: For generating 1-2 sentence summary only
   - No external API calls

2. **RAG-Enhanced Trip Detail Screen**
   - Display contextual card below each detected anomaly
   - Show: "Similar cases: X", "Severity: [HIGH/MEDIUM/LOW]", "Last detected: Xdays ago"
   - Action buttons: "Report", "View History"
   - Graceful fallback if RAG unavailable (show base info)

3. **Vector Search Backend**
   - Postgres + Pgvector for semantic similarity search
   - Embeddings generated from sensor features (mean, std, peaks, etc.)
   - K=5 retrieval for similar anomalies
   - Pre-computed embeddings (no real-time embedding generation)

4. **SQL Aggregations**
   - Location-based hotspot detection
   - Temporal trend analysis (improving vs worsening)
   - Confidence score distributions
   - No complex ML on backend (just SQL)

5. **Caching Layer**
   - Cache results for 1 hour TTL
   - Key structure: `anomaly:{id}` or `location:{lat}:{lng}:{type}`
   - Redis or in-memory cache (simple TTL map)

6. **Authentication**
   - Secure API access (bearer token, API key, or similar)
   - Backend validates requests from authorized clients

**Out of Scope (For Future Releases):**

1. ❌ Fleet-wide analytics dashboard
2. ❌ Predictive maintenance recommendations (requires historical repair data)
3. ❌ Weather/traffic integration (requires external APIs)
4. ❌ On-device LLM (too resource-intensive)
5. ❌ Multilingual support (LLM responses in user language)
6. ❌ Real-time recommendations during active trip (requires low-latency LLM)
7. ❌ Municipality maintenance schedule integration
8. ❌ Advanced explanation generation (requires multi-turn RAG)
9. ❌ User feedback loop (learning from corrections)
10. ❌ Document ingestion (uploaded PDFs, guidelines)

### D.2 MVP Limitations

1. **Single Query Only:** Each API call retrieves context for ONE anomaly
   - Future: Batch context retrieval for multiple anomalies

2. **No Real-Time Integration:** LLM inference adds latency
   - Future: Pre-computed explanations, streaming responses

3. **Location Precision:** 50-meter radius for hotspot matching
   - Future: Machine learning-based location clustering

4. **Limited Contextual Sources:** Only historical app data + basic stats
   - Future: External APIs (weather, traffic, municipality)

5. **Simple Embeddings:** Handcrafted feature vectors
   - Future: Learned embeddings from deep learning

6. **No Feedback Loop:** System doesn't learn from user corrections
   - Future: Active learning & model retraining

7. **Single User Per Device:** No multi-tenant considerations in MVP
   - Future: Role-based access control (driver, fleet manager, authority)

8. **English Only:** LLM responses in English
   - Future: Detect user locale & respond in local language

### D.3 MVP Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **RAG API Response Latency** | < 800ms (p95) | Monitor backend metrics |
| **Cache Hit Rate** | > 60% | Log cache hits/misses |
| **Contextual Summary Quality** | N/A (subjective) | User satisfaction survey |
| **False Positive Reduction** | TBD | Compare detection acceptance before/after RAG |
| **Feature Adoption** | > 40% of trips viewed | Analytics in UI |
| **System Reliability** | 99.9% uptime | Backend health monitoring |
| **Mobile App Impact** | No increase in app size | Measure APK delta |
| **Android Performance** | < 100ms UI render time | Jetpack Compose profiling |

### D.4 MVP Timeline Estimate

| Phase | Duration | Deliverables |
|-------|----------|--------------|
| **Phase 1: Backend Setup** | 1 week | Postgres + Pgvector, FastAPI scaffolding, DB schema |
| **Phase 2: Data Pipeline** | 1.5 weeks | Embedding generation, aggregation jobs, data ingestion |
| **Phase 3: RAG Core** | 2 weeks | Vector search, SQL aggregations, LLM integration, caching |
| **Phase 4: API & Testing** | 1 week | API endpoints, unit tests, integration tests |
| **Phase 5: Android Integration** | 1 week | HTTP client, UI updates, error handling, fallbacks |
| **Phase 6: Testing & Deployment** | 1 week | E2E testing, performance tuning, production deployment |
| **TOTAL** | **7-8 weeks** | MVPDeployment-ready |

### D.5 MVP Dependencies & Risks

**External Dependencies:**
- ✅ PostgreSQL 14+ (must support Pgvector extension)
- ✅ Python FastAPI framework
- ✅ Ollama or cloud LLM API (OpenAI, Anthropic, etc.)
- ✅ Redis (optional but recommended for caching)

**Risks & Mitigations:**

| Risk | Severity | Mitigation |
|------|----------|-----------|
| LLM API cost/latency | High | Use local Ollama; implement aggressive caching |
| Vector DB performance at scale | Medium | Benchmark with production data; add indexing |
| Embeddings quality | Medium | Start with simple features; iterate based on feedback |
| Privacy concerns (query logging) | Medium | Anonymize queries; implement retention policies |
| Backend scalability | Medium | Design for horizontal scaling; use async workers |

---

## E. COMPREHENSIVE PRD

### E.1 Product Overview

**Product Name:** Road Anomaly Detection with Contextual Intelligence (RAG-Enhanced)

**Product Statement:** 
Enable road infrastructure teams and drivers to understand, prioritize, and respond to detected road anomalies through AI-powered contextual insights. Augment real-time edge-AI detection (ONNX) with retrieval-augmented generation (RAG) to surface historical patterns, severity assessments, and actionable recommendations.

**Version:** 1.0 MVP

**Target Release:** Q4 2026 (8 weeks from start)

### E.2 Background & Problem Statement

**Current State:**
The Road Anomaly Detection mobile app successfully detects road anomalies (potholes, speed bumps) in real-time using on-device machine learning. However, users receive only raw detection outputs (type, confidence score, coordinates) without contextual understanding.

**Problems Identified:**

1. **Lack of Context:** Users see "pothole detected (92% confidence)" but don't understand:
   - Whether this is a known issue
   - Historical repair patterns at this location
   - Severity trends (worsening? improving?)
   - Whether other drivers experienced the same issue

2. **Low Actionability:** No guidance on what to do with detection:
   - Should I report to authority?
   - Is the confidence score trustworthy?
   - What are similar cases? Were they valid?

3. **Explainability Gap:** Why did the model make this prediction?
   - Which sensor signals contributed most?
   - Are there edge cases or confusing patterns?
   - How does this compare to misclassifications?

4. **Data Fragmentation:** Each device operates independently:
   - No fleet-wide insights
   - Repeated reporting of identical issues
   - Maintenance priorities not data-driven

5. **Limited Decision Support:** Authorities have raw data but limited insights for maintenance planning

### E.3 Goals

**Primary Goals:**
1. Provide contextual insights for each detected anomaly (similar cases, location trends)
2. Enable users to understand & trust model predictions through historical comparison
3. Reduce manual investigation effort by auto-surfacing relevant context
4. Lay foundation for future fleet-wide analytics & predictive maintenance

**Secondary Goals:**
1. Improve user confidence in anomaly detections
2. Reduce false positive distrust through contextual validation
3. Enable better data-driven decision making for road maintenance
4. Establish patterns for future model improvements

**Success Criteria:**
- RAG API responds in < 800ms (p95)
- Cache hit rate > 60%
- > 40% of users engage with contextual insights
- App performance remains unchanged (no APK bloat, no latency regression)

### E.4 Non-Goals

- **NOT:** Full on-device RAG system (too resource-intensive)
- **NOT:** Real-time contextual recommendations during active recording (MVP)
- **NOT:** Multiple language support (English only in MVP)
- **NOT:** User feedback loop / active learning
- **NOT:** Integration with external authorities (roads.gov, traffic APIs)
- **NOT:** Predictive maintenance (requires structured authority data)
- **NOT:** Replace existing anomaly detection model (ONNX inference unchanged)
- **NOT:** Fleet management dashboard (out of scope for MVP)

### E.5 Target Users

**Primary:**
- Individual drivers & motorcycle riders (personal accident risk awareness)
- Fleet managers (vehicle maintenance & route optimization)
- Road maintenance authorities (data-driven infrastructure prioritization)

**Secondary:**
- Traffic safety researchers (ground truth data for studies)
- Municipality planning teams (asset management insights)

### E.6 User Problems

| User Persona | Problem | Impact |
|---|---|---|
| **Driver** | "I detected a pothole. Should I report it? Or is it a false positive?" | Unsure if taking action or ignoring |
| **Fleet Manager** | "Which roads are worst? Where should I focus maintenance efforts?" | Can't prioritize; loses money to preventive visits |
| **Authority** | "We have 1000 anomaly reports. Which ones are real? Which are urgent?" | Overwhelmed; maintenance plans are reactive not data-driven |
| **Researcher** | "I need validated anomaly data for my study. Are these predictions reliable?" | Limited ground truth; can't verify at scale |

### E.7 Proposed Solution

**High-Level Approach:**
Integrate Retrieval-Augmented Generation (RAG) to supplement real-time anomaly detection with contextual intelligence:

1. **Retrieval Phase:** When user views a detected anomaly, query the backend for:
   - Similar historical detections (semantic similarity via vector search)
   - Aggregated statistics (count, location trends, temporal patterns)
   - Metadata (repair history, recency, confidence scores)

2. **Generation Phase:** Combine retrieved context into a coherent explanation:
   - LLM synthesizes "what this anomaly means based on history"
   - Generates actionable recommendations
   - Highlights uncertainty & confidence

3. **Presentation Phase:** Display contextual insights on Trip Detail Screen:
   - Summary card with key insights
   - Action buttons (Report, View History)
   - Expandable details for power users

**Example Flow:**
```
User taps on detected pothole
  → App queries RAG backend with anomaly details
  → Backend searches for similar cases (vector DB)
  → Backend aggregates location stats (SQL)
  → LLM generates summary: "High severity • Detected 8x in past 90 days • Worsening trend"
  → UI displays summary card with action buttons
```

### E.8 User Stories

**Story 1: Driver Gains Confidence in Detection**
```
As a driver reviewing a completed trip,
I want to see historical context for each detected anomaly,
So that I can understand if this detection is reliable and whether I should report it.

Acceptance Criteria:
- Contextual card displays below each anomaly
- Shows count of similar past detections
- Shows severity assessment based on historical magnitude trends
- Action buttons enable quick reporting/viewing history
- Falls back gracefully if RAG unavailable
```

**Story 2: Fleet Manager Identifies Priority Roads**
```
As a fleet manager,
I want to query the system for roads with worsening conditions,
So that I can prioritize maintenance & route planning.

Acceptance Criteria:
- Backend endpoint returns hotspot statistics (location-based aggregation)
- Statistics include trend indicators (improving/stable/worsening)
- Severity assessment based on detection frequency & confidence scores
- Response time < 2 seconds for regional queries
```

**Story 3: Authority Validates Crowdsourced Data**
```
As a maintenance authority,
I want to filter high-confidence anomalies corroborated by multiple detection events,
So that I can prioritize real infrastructure issues over noise.

Acceptance Criteria:
- API filters anomalies by: confidence threshold, temporal proximity, spatial clustering
- Returns aggregated metadata (how many "votes" per location)
- Confidence estimation based on similar-case patterns (how often were similar cases confirmed?)
```

**Story 4: User Understands Why Detection Happened**
```
As a driver seeing an uncertain detection (e.g., 55% confidence),
I want to see examples of similar ambiguous cases,
So that I understand if this is a known edge case or a mistake.

Acceptance Criteria:
- "Uncertain detections" endpoint shows top-K similar historical ambiguous cases
- Shows resolution of past ambiguous cases (were they confirmed or false positives?)
- Highlights ambiguity factors (e.g., "sensor orientation uncertainty", "low magnitude")
```

### E.9 User Flow

```
TRIP DETAIL SCREEN
├─ [Map with route]
├─ [Trip stats: duration, distance, speed]
├─ DETECTED ANOMALIES (5 total)
│  │
│  ├─ 🔴 POTHOLE #1 (HIGH CONFIDENCE: 92%)
│  │   Location: Jl. Sudirman, Km 2.3
│  │   Detected: 14:23:45 | Magnitude: 1.8G
│  │   [Tap to expand contextual card]
│  │
│  │   [CONTEXTUAL CARD - RAG POWERED]
│  │   ╔════════════════════════════════════╗
│  │   ║ ✅ High Severity                    ║
│  │   ║ Similar cases detected: 8          ║
│  │   ║ Location trend: WORSENING          ║
│  │   ║ Last repair: 60 days ago           ║
│  │   ║ Status: Priority #2 in this area   ║
│  │   ║                                    ║
│  │   ║ [Report to Authority] [View Hist]  ║
│  │   ║ [Dismiss] [Details]                ║
│  │   ╚════════════════════════════════════╝
│  │
│  ├─ 🟡 SPEED BUMP #2 (MEDIUM CONFIDENCE: 68%)
│  │   Location: Jl. Pemuda, Km 5.1
│  │   [Contextual Card: UNCERTAIN DETECTION]
│  │   ╔════════════════════════════════════╗
│  │   ║ ⚠️ Uncertain Confidence (68%)       ║
│  │   ║ Similar ambiguous cases: 12        ║
│  │   ║ Likely factors: Sensor orientation │
│  │   ║ Recommendations: Verify mounting   ║
│  │   ║                                    ║
│  │   ║ [Confirm] [Reject] [Help]         ║
│  │   ╚════════════════════════════════════╝
│  │
│  └─ 🟢 NONE (non-events not shown)
│
└─ [Bottom: "Uploaded" / "Pending Upload" status]
```

### E.10 Functional Requirements

#### FR1: RAG Query Interface

**Requirement:** Backend exposes HTTP REST API for anomaly context retrieval

| Endpoint | Method | Input | Output | Latency Target |
|---|---|---|---|---|
| `/api/rag/anomaly-context` | POST | {anomaly_id, type, location, confidence, sensor_features} | {summary, similar_cases, stats, recommendations} | < 800ms |
| `/api/rag/quick-context` | POST | {type, location, confidence} | {quick_summary, recommendations} | < 500ms |
| `/api/rag/location-hotspot` | POST | {lat, lng, radius_m, date_range} | {count_90d, trend, severity} | < 1000ms |
| `/api/rag/similar-cases` | POST | {sensor_embedding, topk} | [{case_id, confidence, timestamp}] | < 600ms |

**Authentication:** Bearer token (JWT or API key)

**Rate Limiting:** 10 requests/second per user (for future scalability)

#### FR2: Vector Search Backend

**Requirement:** Semantic similarity search for anomaly patterns

| Requirement | Specification |
|---|---|
| **Database** | PostgreSQL 14+ with pgvector extension |
| **Embedding Dimension** | 16-32 dimensions (simple handcrafted features) |
| **Similarity Metric** | Cosine distance (default for pgvector) |
| **Retrieval Size** | K=5 (top 5 similar cases) |
| **Indexing** | IVFFlat or HNSW for approximate nearest neighbor |
| **Update Frequency** | Daily batch (new embeddings generated from daily anomalies) |

**Handcrafted Features for Embeddings:**
- Mean acceleration magnitude
- Standard deviation of X, Y, Z channels
- Peak magnitude (max value)
- Energy (sum of squared values)
- Frequency domain: spectral energy in bands
- Temporal: duration of anomaly window
- Context: time of day, day of week

#### FR3: SQL Aggregation Queries

**Requirement:** Efficient aggregation of anomaly statistics

| Query | Purpose | Response Data |
|---|---|---|
| Location hotspot stats | Count detections within 50m | detection_count, avg_confidence, first_date, last_date |
| Temporal trend | Detect improving/worsening | count_90d, count_60d, count_30d (trend indicator) |
| Confidence distribution | Understand model uncertainty | percentiles, histogram bins |
| Similar type clustering | Find correlated issues | correlation_with_other_types |

#### FR4: LLM Integration

**Requirement:** Generate natural language explanations

| Aspect | Specification |
|---|---|
| **Model** | Ollama (local) or cloud API (OpenAI/Anthropic) |
| **Model Size** | Mistral 7B or equivalent (balance latency & quality) |
| **Max Tokens** | 150 tokens (keep response concise) |
| **Temperature** | 0.3 (deterministic, less creative) |
| **Prompt Format** | System role + structured context + question |
| **Response Parsing** | Extract actionable recommendations (regex or structured output) |

#### FR5: Caching Strategy

**Requirement:** Reduce latency & backend load

| Cache Layer | Key Format | TTL | Storage | Invalidation |
|---|---|---|---|---|
| Response Cache | `rag:{anomaly_id}` | 1 hour | Redis | Manual invalidation on new similar anomalies |
| Embedding Cache | `embed:{sensor_hash}` | 7 days | DB | Re-compute if embedding model updates |
| Hotspot Stats Cache | `hotspot:{lat}:{lng}:{type}` | 1 day | Redis | Invalidate on new anomalies at location |
| Query Log Cache | `queries:user_id:date` | 1 day | Redis | For analytics & deduplication |

#### FR6: Error Handling & Fallbacks

**Requirement:** Graceful degradation if RAG unavailable

```
Scenario: RAG service down
  Response: Return cached result (if available, even if stale)
  OR: Return basic context (count from local data only)
  OR: Display "Unable to load context" with base anomaly info
  (Do NOT block trip detail screen rendering)

Scenario: LLM timeout (> 3 seconds)
  Response: Skip LLM summary; return stats only
  OR: Return pre-computed summary from template

Scenario: Vector DB timeout
  Response: Return SQL aggregations only
  OR: Return rule-based context (common thresholds)

Scenario: Low confidence embedding (< threshold)
  Response: Return stats only, skip similarity matching
```

### E.11 RAG-Specific Requirements

**RAG Retrieval Requirements:**

| Requirement | Specification |
|---|---|
| **Context Window** | Max 2000 tokens (to fit in LLM prompt) |
| **Relevance Threshold** | Cosine similarity > 0.7 for similar cases |
| **Temporal Filter** | Only consider anomalies from past 365 days |
| **Spatial Filter** | Anomalies within 100m radius (configurable) |
| **Deduplication** | Remove duplicate anomalies at same location/time |
| **Ranking** | Sort by recency first, then similarity score |

**RAG Generation Requirements:**

| Requirement | Specification |
|---|---|
| **Summary Length** | 1-3 sentences max |
| **Tone** | Professional, data-driven, uncertain where appropriate |
| **Cite Sources** | "Based on 8 similar detections", "Located in hotspot" |
| **Avoid Hallucination** | Never invent statistics; only report retrieved data |
| **Action Items** | Always include 1-2 next steps |
| **Disclaimers** | Include confidence caveats |

**Knowledge Base Management:**

| Aspect | Requirement |
|---|---|
| **Data Retention** | Keep all anomalies > 365 days (for trend analysis) |
| **Data Quality** | Remove duplicates (same location, same time, high confidence match) |
| **Update Frequency** | Embeddings updated daily (batch job at night) |
| **Backup** | Daily backup of embeddings DB |
| **Privacy** | Anonymize user data; aggregate by location/type only |

### E.12 Knowledge Base Requirements

**Data Sources (MVP):**
1. ✅ Historical app data (trips uploaded by users)
   - Source: CSV files + metadata from backend
   - Volume: ~10k-100k anomalies (growing)
   - Schema: anomaly_id, type, location, timestamp, confidence, sensor_features

2. ✅ Aggregated statistics (computed from app data)
   - Source: SQL queries on anomaly table
   - Freshness: Daily refresh
   - Metrics: detection count by location, confidence distribution, temporal trends

3. ❌ External data (out of scope for MVP)
   - Roads authority maintenance records (future)
   - Weather data (future)
   - Traffic patterns (future)

**Data Ingestion Pipeline (MVP):**

```
1. [User uploads trip CSV + metadata]
   → Stored in backend database
   → Anomaly records inserted into anomalies table

2. [Daily scheduled job]
   a. SELECT new anomalies from past 24 hours
   b. FOR EACH anomaly:
      - Extract sensor features (mean, std, peak, etc.)
      - Compute embedding vector (handcrafted features)
      - INSERT into anomaly_embeddings table
   c. Compute aggregated hotspot statistics (SQL GROUP BY)
   d. Update cache with new statistics

3. [Cache invalidation]
   - Expire cache for affected locations
   - Clear embedding cache if model changes
```

### E.13 LLM Requirements

**Model Selection Criteria:**

| Criteria | Weight | Preference |
|---|---|---|
| **Latency** | 40% | < 500ms inference time |
| **Cost** | 30% | < $0.0001 per query (or free local) |
| **Hallucination Risk** | 20% | Factual, grounded in retrieved data |
| **Customization** | 10% | Can fine-tune or use system prompts |

**Recommended Models (in order):**

1. **Ollama (Mistral 7B) - PREFERRED**
   - Latency: ~200-500ms (local inference)
   - Cost: Free (self-hosted)
   - Hallucination: Medium (needs careful prompting)
   - Setup: Docker container

2. **Claude 3.5 (via API)**
   - Latency: 400-800ms (API call)
   - Cost: ~$0.003 per query (expensive at scale)
   - Hallucination: Low (most reliable)
   - Setup: API key configuration

3. **GPT-4 (via API)**
   - Latency: 500-1000ms
   - Cost: ~$0.01+ per query (very expensive)
   - Hallucination: Low
   - Setup: API key configuration

4. **Llama 2 (70B)**
   - Latency: 2-3 seconds (too slow)
   - Hallucination: Medium
   - Recommendation: Use only for offline MVP testing

**Prompt Engineering:**

```
SYSTEM PROMPT:
"You are a road condition analyst. Your role is to provide concise,
factual insights about detected road anomalies based on provided context.
Always ground your responses in the data provided. Acknowledge uncertainty
with confidence levels. Avoid speculation or invented details. Format
recommendations as a bullet-point action list. Keep response under 150 tokens."

CONTEXT TEMPLATE:
{
  "anomaly_type": "pothole",
  "confidence_score": 0.92,
  "location": "Jl. Sudirman, Km 2.3",
  "sensor_magnitude": 1.8,
  "similar_cases": [
    {id: "...", confidence: 0.89, timestamp: "2026-09-15"},
    {id: "...", confidence: 0.85, timestamp: "2026-09-10"},
    ...
  ],
  "location_stats": {
    "detection_count_90d": 8,
    "avg_confidence": 0.84,
    "trend": "worsening"
  }
}

PROMPT TEMPLATE:
"Analyze this detected road anomaly:

Type: {anomaly_type}
Confidence: {confidence_score}%
Location: {location}
Sensor Magnitude: {sensor_magnitude}G

Historical Context:
- Similar Cases: {len(similar_cases)} detected
- Location Stats: {detection_count_90d} detections (90 days), Trend: {trend}
- Average Confidence of Similar Cases: {avg_confidence}%

Based on this data, what does this anomaly indicate? Is this likely real?
What should the user do next?"

EXPECTED OUTPUT STRUCTURE:
"Based on {N} similar high-confidence detections at this location,
this is likely a genuine {anomaly_type}. The {magnitude_assessment}.
Recommendation: [1] Report to authority; [2] Monitor for changes."
```

### E.14 Android Application Requirements

**New API Client Methods:**

```kotlin
// In ApiService interface
@POST("api/rag/anomaly-context")
suspend fun getAnomalyContext(
    @Body request: AnomalyContextRequest
): Response<AnomalyContextResponse>

@POST("api/rag/quick-context")
suspend fun getQuickContext(
    @Body request: QuickContextRequest
): Response<QuickContextResponse>

// Request/Response data classes
data class AnomalyContextRequest(
    val anomalyId: String,
    val type: String,
    val location: Location,
    val confidence: Float,
    val sensorFeatures: Map<String, Float>
)

data class AnomalyContextResponse(
    val status: String,
    val severity: String,  // HIGH, MEDIUM, LOW
    val summary: String,
    val confidenceAssessment: String,
    val recommendations: List<Recommendation>,
    val stats: ContextStats,
    val cacheTtl: Long?
)
```

**UI Components:**

```
New Composable: RagContextCard
├─ params: AnomalyContextResponse, onAction: (String) -> Unit
├─ Layout:
│  ├─ Severity badge (color-coded: red/yellow/green)
│  ├─ Summary text (1-3 sentences)
│  ├─ Stats row (Similar cases, Trend, etc.)
│  ├─ Recommendations (clickable chips)
│  └─ Action buttons (Report, View History, Dismiss)
└─ Error state: Show cached data or basic info

Updated Composable: TripDetailScreen
├─ For each AnomalyEvent:
│  ├─ Basic info (type, time, location, confidence)
│  ├─ Loading state while fetching context
│  ├─ Display RagContextCard (on success)
│  └─ Fallback to basic info (on error)
```

**Error Handling & Caching:**

```kotlin
// In TripDetailViewModel or similar
private val ragCache = mutableMapOf<String, CachedRagContext>()

suspend fun getAnomalyContextWithFallback(
    anomalyId: String,
    type: String,
    location: Location
): AnomalyContextResponse {
    // Check cache first
    val cached = ragCache[anomalyId]
    if (cached != null && !cached.isExpired()) {
        return cached.response
    }
    
    // Try API call
    return try {
        val response = apiService.getAnomalyContext(
            AnomalyContextRequest(anomalyId, type, location, ...)
        )
        if (response.isSuccessful) {
            val data = response.body()!!
            ragCache[anomalyId] = CachedRagContext(data, System.currentTimeMillis())
            data
        } else {
            // Return stale cache or basic info
            cached?.response ?: generateBasicContext(anomalyId)
        }
    } catch (e: Exception) {
        // Network error: return cache or basic info
        Log.w("RAG", "Context fetch failed", e)
        cached?.response ?: generateBasicContext(anomalyId)
    }
}
```

**Permissions & Security:**

```
No new permissions required (all data already collected)
Security: Use bearer token for RAG API calls
         (obtain from existing auth mechanism)
```

### E.15 Backend/API Requirements

**New API Endpoints:**

| Endpoint | Responsibility | Language | Framework |
|---|---|---|---|
| `/api/rag/anomaly-context` | Main context retrieval | Python | FastAPI |
| `/api/rag/quick-context` | Low-latency version | Python | FastAPI |
| `/api/rag/location-hotspot` | Admin queries | Python | FastAPI |
| `/api/rag/similar-cases` | Vector search | Python | FastAPI |
| `/health/rag` | RAG service health | Python | FastAPI |

**Database Schema:**

```sql
-- Anomalies table (existing)
TABLE anomalies {
  id UUID PRIMARY KEY,
  trip_id UUID,
  type VARCHAR (pothole | speedbump | none),
  location GEOMETRY (Point),
  detected_at TIMESTAMP,
  confidence FLOAT,
  sensor_magnitude FLOAT,
  raw_csv_line TEXT,
  created_at TIMESTAMP
}

-- New: Anomaly embeddings for semantic search
TABLE anomaly_embeddings {
  id UUID PRIMARY KEY,
  anomaly_id UUID FOREIGN KEY,
  embedding VECTOR(32),  -- pgvector type
  feature_hash VARCHAR,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  
  INDEX embedding_idx ON embedding (embedding USING hnsw)
}

-- New: RAG context cache
TABLE rag_context_cache {
  key VARCHAR PRIMARY KEY,
  response JSONB,
  created_at TIMESTAMP,
  expires_at TIMESTAMP,
  hit_count INT DEFAULT 0,
  
  INDEX expires_at_idx
}

-- New: RAG query logs (for analytics)
TABLE rag_queries {
  id UUID PRIMARY KEY,
  user_id VARCHAR,
  query_type VARCHAR,
  request JSONB,
  response_time_ms INT,
  created_at TIMESTAMP
}
```

**Data Ingestion Job (Daily):**

```python
# Backend scheduled task (e.g., using Celery or APScheduler)
@scheduler.scheduled_task(cron='0 2 * * *')  # 2 AM daily
def update_embeddings():
    """Generate embeddings for newly uploaded anomalies"""
    
    # 1. Find new anomalies (last 24 hours)
    new_anomalies = db.query(Anomaly).filter(
        Anomaly.created_at > datetime.now() - timedelta(days=1)
    ).all()
    
    # 2. For each anomaly, generate embedding
    for anomaly in new_anomalies:
        features = extract_features(anomaly)
        embedding = embed_features(features)  # handcrafted feature vector
        
        db.insert(AnomalyEmbedding(
            anomaly_id=anomaly.id,
            embedding=embedding,
            feature_hash=hash_features(features)
        ))
    
    # 3. Compute aggregated hotspot statistics
    location_groups = db.query(Anomaly).filter(
        Anomaly.created_at > datetime.now() - timedelta(days=90)
    ).group_by(Anomaly.location).all()
    
    for group in location_groups:
        stats = {
            'count': len(group),
            'avg_confidence': mean([a.confidence for a in group]),
            'trend': compute_trend(group)
        }
        cache.set(f'hotspot:{group.location}', stats, ttl=86400)
    
    logging.info(f"Updated embeddings for {len(new_anomalies)} anomalies")
```

**API Response Examples:**

```json
POST /api/rag/anomaly-context
Request:
{
  "anomaly_id": "abc-123",
  "type": "pothole",
  "location": {"lat": 6.25, "lng": 110.40},
  "confidence": 0.92,
  "sensor_features": {
    "accel_mean": 1.8,
    "accel_std": 0.3,
    "gyro_energy": 0.2,
    ...
  }
}

Response (200 OK):
{
  "status": "success",
  "severity": "HIGH",
  "summary": "High-confidence pothole detection corroborated by 8 similar cases at this location. Magnitude suggests significant surface degradation.",
  "confidence_assessment": "High confidence. Model score (92%) correlated with historical accuracy pattern for this region.",
  "recommendations": [
    {"label": "Report to Authority", "action": "report_to_authority", "priority": 1},
    {"label": "Monitor for Worsening", "action": "track_trends", "priority": 2}
  ],
  "stats": {
    "similar_cases": 8,
    "location_hotspot": true,
    "detection_count_90d": 8,
    "avg_confidence_similar": 0.84,
    "trend": "worsening",
    "days_since_last_repair": 60,
    "priority_rank": 2
  },
  "cache_ttl": 3600
}
```

### E.16 Data Requirements

**Minimum Data Needed:**

1. **Historical Anomalies:** ~10k-100k records (from past year of app usage)
   - Schema: anomaly_id, trip_id, type, location, timestamp, confidence, sensor_features

2. **Sensor Features:** Extracted from raw CSV:
   - Mean/std of X, Y, Z accelerometer channels
   - Peak magnitudes
   - Spectral features (frequency domain)
   - Temporal features (duration, timing)

3. **Geospatial Data:** Location coordinates with ~50-100m radius clustering

4. **Metadata:** Trip context (date, time, user device, OS version)

**Data Quality Checks:**

- Remove duplicates (same location, same timestamp, < 10m distance)
- Filter out dropouts (gaps > 500ms in sensor stream)
- Validate coordinates (within expected region bounds)
- Remove test/debug trips

**Data Privacy Considerations:**

- ✅ Aggregate by location/type (no personally identifiable data in RAG)
- ✅ Anonymize user IDs (use hash, not username)
- ✅ Retention policy: Keep 365 days of anomalies (older data archived)
- ✅ Query logging: Log queries for analytics, not for tracking individuals
- ✅ Encryption: RAG cache encrypted at rest (if sensitive)

### E.17 UI/UX Requirements

**Trip Detail Screen - Enhanced with RAG:**

```
Existing Elements:
├─ [Header: Trip date, duration, distance]
├─ [Map: Route visualization with markers]
├─ [Basic stats: Avg speed, total anomalies]
├─ [Anomalies list]

New Elements (RAG):
├─ Each anomaly card:
│  ├─ Type badge + timestamp
│  ├─ Basic info: location, confidence, magnitude
│  ├─ [Loading spinner] (while fetching context)
│  ├─ [Contextual insights card]  ← NEW
│  │  ├─ Severity indicator (RED/YELLOW/GREEN)
│  │  ├─ Summary text (1-3 lines)
│  │  ├─ Stats row: "Similar: 8 | Trend: Worsening | Last repair: 60d ago"
│  │  ├─ Recommendations chips (clickable)
│  │  └─ Action buttons
│  └─ [Expand/collapse details]
```

**Color Scheme (Severity Indicator):**
- 🔴 HIGH: Red (#d32f2f) - Urgent attention
- 🟡 MEDIUM: Amber (#f57c00) - Monitor
- 🟢 LOW: Green (#388e3c) - Normal

**Typography:**
- Summary: Body text 14sp
- Stats: Caption 12sp, secondary color
- Actions: Button text 14sp, accent color

**Accessibility:**
- All interactive elements >= 48x48dp touch target
- Color + icon (not just color for severity)
- Screen reader descriptions for each component

**Dark Mode Support:**
- Use Material Design 3 dynamic colors
- Sufficient contrast in both themes (WCAG AA)

### E.18 Error & Edge Cases

**Edge Case 1: No Similar Historical Cases**

```
Scenario: User views rare anomaly type; < 3 similar cases in DB
Response: 
  {
    "severity": "UNKNOWN",
    "summary": "Rare anomaly pattern. Limited historical data for comparison.",
    "similar_cases": 0,
    "recommendations": ["Verify sensor mounting", "Manual inspection recommended"]
  }
UI Display:
  Severity: UNKNOWN (gray indicator)
  Text: "Not enough historical data. This may be unusual or a sensor fault."
```

**Edge Case 2: Location Edge (Ambiguous)**

```
Scenario: Detection at location boundary (multiple nearby clusters)
Response:
  Aggregate stats for all nearby clusters (radius 100m)
  Highlight: "Multiple issue clusters nearby"
  Stats: Show breakdown per sub-location

UI Display:
  "Pothole cluster detected in area • 12 detections across 200m radius"
  [Expand to see map of sub-clusters]
```

**Edge Case 3: Recent Repair (Data Inconsistency)**

```
Scenario: Detection at location where repair was just completed (yesterday)
Response:
  Flag: "Recent repair"
  Summary: "New pothole detected. Area repaired {days_ago} ago."
  Recommendations: ["Likely construction fault", "Verify repair quality", "Report regression"]

UI Display:
  Highlight with warning icon
  Show repair date prominently
```

**Edge Case 4: Model Confidence Very Low**

```
Scenario: Confidence score near random threshold (e.g., 51%)
Response:
  Uncertain detection
  Show reasons: "Low signal magnitude", "Ambiguous sensor pattern"
  Suggest: "Sensor calibration check", "Manual verification"

UI Display:
  Large warning: "⚠️ LOW CONFIDENCE"
  Allow user to: Confirm / Reject / Report as uncertain
```

**Edge Case 5: Stale Cache Hit**

```
Scenario: User views trip 30 days after it was recorded; cache is stale
Response:
  Return cached context (still valid)
  Add disclaimer: "Context last updated on {date}"
  Option: [Refresh for latest] button

UI Display:
  Note: "Context from {date} (stale data)" with refresh button
```

### E.19 Security & Privacy Considerations

**Data Security:**

1. **API Authentication:**
   - Require Bearer token (JWT or API key)
   - Validate token on every RAG request
   - Log unauthorized access attempts

2. **Input Validation:**
   - Validate anomaly_id format (UUID, only user's trips)
   - Validate coordinates (within service region)
   - Sanitize strings (prevent injection attacks)

3. **Rate Limiting:**
   - 10 requests/second per user
   - 1000 requests/hour per user
   - Global limit: 100k requests/hour
   - Return 429 Too Many Requests on limit

4. **Encryption:**
   - TLS 1.3 for API communication
   - Encrypt sensitive cache keys at rest
   - Sensitive query logs encrypted

**Privacy Considerations:**

1. **Data Anonymization:**
   - Do NOT expose raw user IDs in RAG responses
   - Aggregate statistics at location/type level only
   - Hash user IDs for logs

2. **Query Logging:**
   - Log RAG queries for system debugging
   - Retention: 30 days (then delete)
   - Do NOT log user identifying information

3. **Third-Party APIs:**
   - If using cloud LLM (e.g., Claude), ensure data processing agreement
   - Do NOT send raw sensor data to external APIs
   - Only send aggregated context

4. **Compliance:**
   - Comply with local data protection laws (Indonesia: PDP Law if applicable)
   - Provide data export/deletion on user request
   - Document data flows in privacy policy

### E.20 Performance Requirements

**Latency Targets:**

| Operation | Target | Threshold (Alert) |
|---|---|---|
| RAG full context retrieval | < 800ms (p95) | > 1500ms |
| Quick context retrieval | < 500ms (p95) | > 1000ms |
| Vector search (K=5) | < 200ms (p95) | > 500ms |
| LLM inference | < 300ms (p95) | > 800ms |
| Cache hit response | < 50ms (p95) | > 100ms |

**Throughput Requirements:**

- Support 100 concurrent users
- 10 requests/second per user (peak)
- Total: 1000 req/s system capacity

**Resource Requirements:**

- Backend VM: 4-8 vCPU, 16-32GB RAM
- Database: PostgreSQL with pgvector, 50GB+ storage (growing)
- Ollama (local LLM): 8GB RAM minimum (for 7B model)
- Cache (Redis): 2GB (for hot data)

**Scalability:**

- Horizontal scaling: Load balancer → multiple API instances
- Database scaling: Pg partitioning by date for embeddings
- Vector index: HNSW for fast similarity search at scale

### E.21 Evaluation & Success Metrics

**Product Metrics:**

| Metric | Target | Measurement | Owner |
|---|---|---|---|
| **Feature Adoption** | > 40% of viewed trips | Users who expand RAG context | Analytics |
| **Click-Through Rate** | > 30% on action buttons | "Report" / "View History" clicks | Analytics |
| **Repeat Queries** | > 50% for hotspot locations | Same location queried multiple times | Analytics |
| **User Satisfaction** | > 4.0/5.0 rating | In-app survey after using RAG | Product |

**Technical Metrics:**

| Metric | Target | Measurement | Owner |
|---|---|---|---|
| **API Response Latency (p95)** | < 800ms | Backend monitoring | Engineering |
| **Cache Hit Rate** | > 60% | Cache metrics | Engineering |
| **LLM Hallucination Rate** | < 5% | Manual review of LLM responses | QA |
| **False Positive Reduction** | > 10% vs baseline | Compare user acceptance before/after | Data Science |
| **System Uptime** | 99.9% | Monitoring alerts | DevOps |
| **Error Rate** | < 0.5% | Sentry / error tracking | Engineering |

**Business Metrics:**

| Metric | Target | Measurement | Owner |
|---|---|---|---|
| **User Retention** | > 80% (30-day) | Segment RAG users vs non-RAG | Product |
| **Reported Issues** | > 2x increase | Trips with "Report" button clicked | Operations |
| **Maintenance Cost Reduction** | TBD | Compare cities with/without RAG data | Operations |

### E.22 Rollout & Deployment Strategy

**Phase 1: Beta (Week 1-2)**
- Deploy to staging environment
- Test with internal team (10 users)
- Verify latency, accuracy, caching
- Collect feedback

**Phase 2: Canary Release (Week 3)**
- Deploy to 5% of production users
- Monitor error rate, latency, cache hit
- Gradually increase to 25%

**Phase 3: General Release (Week 4)**
- Full rollout to all users
- Monitor adoption metrics
- Respond to user feedback

**Rollback Plan:**
- If error rate > 2%, rollback immediately
- If latency p95 > 1.5s, disable RAG (show basic info)
- Keep fallback to non-RAG experience always available

### E.23 Future Improvements (Post-MVP)

1. **Real-Time Recommendations:** Low-latency RAG during active trip
2. **Predictive Maintenance:** Forecast road deterioration based on trends
3. **Fleet Analytics Dashboard:** Hotspot maps, route optimization
4. **Weather Integration:** Correlate anomalies with precipitation, temperature
5. **Authority Integration:** Direct report submission to maintenance systems
6. **User Feedback Loop:** Learn from user corrections/validations
7. **Multilingual Support:** Generate summaries in local languages
8. **Custom Models:** Fine-tune LLM on historical data for domain adaptation

---

## F. OPEN QUESTIONS & ASSUMPTIONS

### F.1 Open Questions (Requires Answer from Stakeholders)

**Q1: Backend Infrastructure**
- **Question:** Is there an existing backend server? If so, what's the tech stack?
- **Current Info:** Unknown (no backend source code provided)
- **Impact:** Affects database choice, LLM hosting, API design
- **Required For:** Technical design phase

**Q2: API Base URL**
- **Question:** What's the base URL for the Retrofit API client?
- **Current Info:** Hardcoded in Retrofit config (not visible in provided code)
- **Impact:** Where to deploy new RAG endpoints
- **Required For:** Integration testing

**Q3: Authentication Mechanism**
- **Question:** How is the app currently authenticating with backend? (JWT, API key, OAuth?)
- **Current Info:** Not visible in provided ApiService (no auth headers)
- **Impact:** How to secure RAG endpoints
- **Required For:** Security implementation

**Q4: User Data Ownership**
- **Question:** Can we aggregate trip data across users for RAG hotspot analysis?
- **Current Info:** Unknown (privacy/consent unclear)
- **Impact:** Whether RAG can use fleet-wide data
- **Required For:** Data governance

**Q5: Repair/Maintenance Data**
- **Question:** Does the municipality/authority have maintenance records to integrate?
- **Current Info:** Unknown
- **Impact:** Enrichment potential for RAG context
- **Required For:** Future Phase 2

**Q6: Existing Analytics Infrastructure**
- **Question:** Is there existing analytics setup? (Sentry, Datadog, Google Analytics?)
- **Current Info:** Firebase Crashlytics present, but usage unclear
- **Impact:** Where to log RAG metrics
- **Required For:** Monitoring & observability

**Q7: LLM Preference**
- **Question:** Should we use local LLM (Ollama) or cloud API (Claude, GPT-4)?
- **Current Info:** Trade-off analysis provided, but stakeholder preference unknown
- **Impact:** Cost, latency, privacy
- **Required For:** Architecture decision

**Q8: Budget & Timeline Constraints**
- **Question:** Is there a hard budget for LLM API costs or server infrastructure?
- **Current Info:** Unknown
- **Impact:** Influences LLM choice (local vs cloud)
- **Required For:** Vendor selection

**Q9: Geographic Scope**
- **Question:** What geographic region will this app operate in? (Indonesia only? Southeast Asia? Global?)
- **Current Info:** Likely Indonesia (Pemalang region mentioned in code), but uncertain
- **Impact:** Data retention policies, privacy regulations
- **Required For:** Compliance planning

**Q10: Model Version Stability**
- **Question:** Will the ONNX model (1D-CNN) be updated frequently or is it stable?
- **Current Info:** Current model is Optuna-tuned 1D-CNN, no indication of retraining schedule
- **Impact:** Embedding extraction logic stability
- **Required For:** Long-term maintenance plan

### F.2 Assumptions (Made for This PRD)

**A1: Backend Service Exists**
- **Assumption:** Some form of REST backend exists at `/api/trips/upload`
- **Basis:** ApiService has POST /api/trips/upload in code
- **Risk:** If backend doesn't exist, RAG integration is blocked
- **Mitigation:** Confirm backend existence before Phase 1

**A2: SQL Database Backend**
- **Assumption:** Backend uses relational DB (PostgreSQL or MySQL)
- **Basis:** Trip data needs to be queryable; CSV alone insufficient
- **Risk:** If backend uses NoSQL only, vector search harder to implement
- **Mitigation:** Evaluate database choice early

**A3: Trip CSV Data Quality**
- **Assumption:** Uploaded CSV data is clean (valid coordinates, proper timestamps)
- **Basis:** Mobile app has sensor validation logic
- **Risk:** If data is noisy, embeddings will be poor quality
- **Mitigation:** Implement data quality checks in ingestion pipeline

**A4: User Data Opt-In**
- **Assumption:** Users consent to aggregated data use for fleet analytics
- **Basis:** Typical app terms of service cover this
- **Risk:** Privacy concerns could reduce adoption
- **Mitigation:** Clear privacy policy; option to opt-out

**A5: Stable Sensor Characteristics**
- **Assumption:** Sensor characteristics don't vary dramatically (same phone models, mounting practices)
- **Basis:** Mobile phones have standardized sensors
- **Risk:** If users mount phones differently, sensor patterns vary wildly
- **Mitigation:** Normalize features per-device; device model metadata

**A6: LLM Accuracy Sufficient**
- **Assumption:** LLM-generated summaries are accurate enough for user trust (> 95%)
- **Basis:** Well-prompted LLM with grounded context (RAG) should be reliable
- **Risk:** Hallucinations could erode trust
- **Mitigation:** Aggressive prompt engineering; human review of sample outputs

**A7: Vector Search Relevance Threshold**
- **Assumption:** Cosine similarity > 0.7 indicates relevant case
- **Basis:** Standard for semantic similarity in NLP
- **Risk:** Threshold may be too strict or lenient for sensor data
- **Mitigation:** Empirical tuning on real data; user feedback loop

**A8:24-Hour Embedding Update Latency Acceptable**
- **Assumption:** It's okay to update embeddings once daily (not real-time)
- **Basis:** Most use cases don't require minute-by-minute updates
- **Risk:** New trips won't be in RAG context immediately
- **Mitigation:** Could increase frequency (4x daily) if needed; minimal cost

**A9: 50m Geospatial Radius Reasonable**
- **Assumption:** 50m radius captures "same location" meaningfully (roads don't overlap much)
- **Basis:** Typical road lane width ~3m, so 50m ≈ ~15 lane widths
- **Risk:** May group too many disparate locations (wide roads) or miss real clusters
- **Mitigation:** Configurable radius; monitor cluster quality

**A10: No Existing LLM Integration**
- **Assumption:** Current app doesn't use LLM (all logic is ML model or rules)
- **Basis:** No LLM dependencies in build.gradle.kts
- **Risk:** Integration complexity higher than assumed
- **Mitigation:** POC with Ollama before full integration

---

## G. IMPLEMENTATION READINESS CHECKLIST

### G.1 Requirements Clarity

- [x] Product goals well-defined
- [x] User problems & use cases validated
- [x] Success metrics specified
- [x] MVP scope clearly bounded
- [x] Out-of-scope items listed
- [x] Non-goals documented

**Status:** ✅ READY - All major product questions answered

**Blockers:** None

**Risks:** Stakeholder alignment on MVP scope (some may want more features)

**Recommendation:** Review MVP scope with stakeholders before Phase 1

### G.2 Technical Architecture

- [x] RAG architecture options presented
- [x] Android/backend responsibility division clear
- [x] Technology stack recommended
- [x] Database schema drafted
- [x] API endpoints specified
- [x] Data flow diagrams provided

**Status:** ✅ READY - Architecture is viable and detailed

**Blockers:** 
- ⚠️ Backend infrastructure unknown (need to confirm exists)
- ⚠️ LLM choice not finalized (Ollama vs cloud API)

**Risks:** If backend infrastructure is outdated, significant refactoring may be needed

**Recommendation:** Confirm backend tech stack before Phase 1; do Ollama POC in parallel

### G.3 Data Requirements

- [x] Knowledge sources identified
- [x] Data schema specified
- [x] Data quality checks defined
- [x] Privacy considerations addressed
- [x] Data retention policy proposed

**Status:** ⚠️ PARTIALLY READY - High-level plan clear, but details TBD

**Blockers:**
- ⚠️ Minimum historical data volume not verified (assuming 10k-100k anomalies available)
- ⚠️ Data quality of existing CSV uploads unknown

**Risks:** Insufficient data → poor vector embeddings → poor RAG performance

**Recommendation:** Audit existing uploaded trip data; estimate anomaly volume; assess data quality before Phase 1

### G.4 Android Application

- [x] UI/UX requirements specified
- [x] API client methods drafted
- [x] Error handling & fallbacks defined
- [x] Caching strategy proposed
- [x] Performance targets set

**Status:** ✅ READY - Clear integration points identified

**Blockers:** None immediate

**Risks:** UI rendering performance with async RAG calls

**Recommendation:** Implement loading states & skeleton screens; load RAG context asynchronously

### G.5 Backend Development

- [x] API endpoints specified
- [x] Database schema drafted
- [x] Ingestion pipeline described
- [x] Caching strategy proposed
- [x] Monitoring points identified

**Status:** ✅ READY - Backend architecture clear

**Blockers:**
- ⚠️ Need to confirm existing backend infra
- ⚠️ LLM service deployment method TBD

**Risks:** Pgvector performance at scale (millions of embeddings)

**Recommendation:** Start with FAISS for quick POC; migrate to Pgvector later if needed

### G.6 Security & Privacy

- [x] API authentication requirements defined
- [x] Rate limiting specified
- [x] Data privacy considered
- [x] Encryption approach noted
- [x] Compliance needs identified

**Status:** ⚠️ PARTIALLY READY - Framework defined, details need finalization

**Blockers:**
- ⚠️ Privacy policy review needed
- ⚠️ Data processing agreement with LLM vendor (if cloud-based) TBD

**Risks:** Privacy/compliance violations if not carefully implemented

**Recommendation:** Legal/compliance review before Phase 3 (beta release)

### G.7 Testing & QA

- [ ] Test strategy defined
- [ ] Test cases written
- [ ] QA environment setup
- [ ] Performance testing plan
- [ ] User acceptance testing plan

**Status:** ❌ NOT READY - Testing details deferred to Phase 1

**Blockers:** None

**Risks:** Insufficient test coverage → bugs in production

**Recommendation:** Define test strategy in Phase 0 (before dev starts)

### G.8 Deployment & Operations

- [ ] Deployment strategy documented
- [ ] Monitoring/alerting setup
- [ ] Rollback procedure defined
- [ ] On-call support plan
- [ ] SLA/uptime targets agreed

**Status:** ⚠️ PARTIALLY READY - Framework provided, operational details TBD

**Blockers:** None immediate

**Risks:** Production outage without proper monitoring/alerting

**Recommendation:** Finalize DevOps plan in Phase 0

### G.9 Documentation

- [x] PRD completed
- [x] Architecture documented
- [x] API specifications provided
- [ ] Developer guide written
- [ ] Deployment guide written
- [ ] Operations guide written

**Status:** ⚠️ PARTIALLY READY - High-level docs done; implementation guides TBD

**Blockers:** None

**Recommendation:** Create detailed developer guide in Phase 1

### G.10 Stakeholder Buy-In

- [ ] Product stakeholders reviewed PRD
- [ ] Engineering team estimated effort
- [ ] Budget approved
- [ ] Timeline agreed
- [ ] Risks acknowledged & mitigated

**Status:** ❌ NOT READY - Stakeholder review pending

**Blockers:** Stakeholder alignment required

**Risks:** Scope creep; timeline slippage

**Recommendation:** Present PRD to stakeholders; collect feedback; iterate

---

## CONCLUSION & NEXT STEPS

### Summary

This PRD proposes integrating Retrieval-Augmented Generation (RAG) into the Road Anomaly Detection mobile app to provide contextual intelligence for detected road anomalies. The solution addresses user pain points (lack of context, unexplainability, low actionability) by retrieving similar historical cases and generating natural language explanations powered by LLM inference.

**Key Findings:**

1. ✅ **RAG is well-suited** for this use case: grounded in actual data, solves explainability gap, no hallucination risk when properly constrained
2. ✅ **MVP is achievable** in 7-8 weeks with backend-centric architecture (recommended Option A)
3. ⚠️ **Critical unknowns** exist: backend infrastructure, LLM choice, data volume/quality
4. ⚠️ **Key dependencies**: SQL database with pgvector, local Ollama or cloud LLM API, historical anomaly data

**Recommendation:** Proceed with MVP planning contingent on:
- Confirming backend infrastructure exists & tech stack
- Auditing historical trip data volume/quality
- Finalizing LLM choice (Ollama vs cloud)
- Stakeholder alignment on scope & timeline

### Next Steps (Recommended)

**Phase 0: Validation & Planning (Week 1)**
1. ✅ Present PRD to stakeholders (product, engineering, operations)
2. ⚠️ Confirm backend infrastructure; obtain credentials/access
3. ⚠️ Query database for historical anomaly count; assess data quality
4. ⚠️ Make LLM decision (Ollama vs cloud API); set up POC environment
5. ✅ Create detailed test strategy & QA plan
6. ✅ Assign developers & start backlog refinement

**Phase 1: POC & Validation (Week 1-2)**
1. Backend: Set up Postgres + Pgvector; ingest sample anomaly data
2. Backend: Implement `/api/rag/anomaly-context` endpoint (stub responses)
3. Backend: Test vector search performance with real embeddings
4. Backend: Evaluate LLM latency (local Ollama vs cloud API)
5. Android: Implement API client; wire up UI components (with mock responses)
6. Validate latency targets & cache strategy on real data

**Phase 2-6: Full Implementation (Week 3-8)**
- (See MVP Timeline in Section D.4)

### Final Notes

**For Product Managers:**
- Focus on user value: contextual insights reduce investigation burden
- Success metric: adoption > 40% of trip views; user satisfaction > 4.0/5.0
- Plan post-MVP: fleet analytics, predictive maintenance, authority integration

**For Engineering:**
- Start with backend-centric architecture (Option A); hybrid (Option B) if performance issues
- Local Ollama gives best latency/privacy; cloud LLM if cost is no barrier
- Heavy emphasis on caching & fallbacks (RAG must not degrade UX if unavailable)
- Vector search is bottleneck; benchmark early with real data

**For Operations:**
- Plan for 99.9% uptime; implement alerting for RAG service health
- Monitor LLM latency; alert if > 1.5s (switch to fallback)
- Daily embedding update job is critical; ensure it doesn't conflict with peak load
- Test rollback procedure; have easy kill-switch to disable RAG if issues

**For Data Science:**
- Invest in embedding quality (handcrafted features initially; learned embeddings future)
- Monitor LLM hallucination rate; iterate on prompt engineering
- Track embedding drift over time; retrain if distribution shifts

---

**Document End**  
Last updated: September 18, 2026
