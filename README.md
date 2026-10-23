Here is a professional, complete `README.md` tailored specifically to your project architecture, implementation details, and OS features.

---

# 📹 Real-Time Multimedia Streaming Engine & OS Synchronization Simulator

A multi-threaded Java Swing desktop dashboard that simulates a real-time multimedia streaming pipeline using fundamental **Operating System (OS) concurrency concepts**.

This application models how a streaming media server (**Producer**) generates video data chunks, stores them in a synchronized shared memory buffer (**Bounded Buffer**), and delivers them to a media player client (**Consumer**) for real-time video rendering.

---

## 🌟 Key Features

* **Multi-Tab Dashboard Interface:** Clean, dark-themed Java Swing UI featuring dedicated control panels for the System Overview, Producer Server, Consumer Client, and Real-Time Analytics.
* **Dynamic Buffer Allocation:** Adjustable shared buffer capacity (4, 8, 10, or 16 slots) with built-in runtime safety checks to prevent memory corruption or data loss when resizing active buffers.
* **Real-Time Analytics & Visualizations:** Custom-painted graphics (`Graphics2D`) tracking metrics live:
* **Buffer Occupancy Graph:** Real-time line chart plotting slot usage against capacity over time.
* **Buffer Utilization Gauge:** Color-coded circular ring showing percentage capacity (`Empty`, `Normal`, `Full`).
* **Streaming Data Flow Pipeline:** Animated 6-stage visualization highlighting active chunk progression (`Server → Producer → Shared Buffer → Consumer → Device → Playback`).
* **Production vs. Consumption:** Dual-line cumulative performance chart comparing produced vs. consumed chunk counts.


* **Precision Performance Metrics:** Accurate time-series tracking of average production/consumption rates, peak & continuous average occupancy integrals, total thread wait durations, underflow events, and true system throughput ($\text{Chunks} / \text{sec}$).
* **Live System Telemetry Feed:** Real-time event logger recording thread wait states, chunk insertion/removal timestamps, and mutual exclusion locks.

---

## 🛠️ Operating System Concepts Implemented

1. **Producer-Consumer Problem & Mutual Exclusion (`synchronized`)**
* Uses intrinsic monitor locks (`synchronized` blocks) in `SharedBuffer.java` to guarantee that only one thread can modify the buffer at any given millisecond, completely preventing race conditions.


2. **Thread Synchronization & Coordination (`wait()` / `notifyAll()`)**
* Prevents busy-waiting (cpu polling). When the buffer is full, the **Producer** thread executes `wait()` and yields its lock. When the buffer is empty, the **Consumer** thread calls `wait()`. Signals via `notifyAll()` wake sleeping threads immediately upon state changes.


3. **Bounded Buffer & Memory Management**
* Enforces strict fixed memory bounds to model real-world video RAM/socket buffer constraints, preventing unbounded memory growth.


4. **Buffer Overflow, Underflow & Playback Buffering**
* **Overflow Prevention:** Blocks the Producer when buffer occupancy reaches 100%.
* **Underflow / Starvation Handling:** Intercepts buffer exhaustion ($0$ slots) to trigger an explicit `BUFFERING...` visual alert in the consumer interface without corrupting playback state or crashing the application.



---

## 🏗️ System Architecture

```
                       ┌─────────────────────────┐
                       │  Streaming Server Data  │
                       └────────────┬────────────┘
                                    │
                                    ▼
┌─────────────────┐    ┌─────────────────────────┐
│ Producer Task   ├───►│   Producer Thread (P)   │
└─────────────────┘    └────────────┬────────────┘
                                    │  produce() [synchronized]
                                    ▼
                       ┌─────────────────────────┐
                       │   Shared Bounded Buffer │ (Max Capacity: 4-16)
                       └────────────┬────────────┘
                                    │  consume() [synchronized]
                                    ▼
┌─────────────────┐    ┌─────────────────────────┐
│ Consumer Task   │◄───┤   Consumer Thread (C)   │
└─────────────────┘    └────────────┬────────────┘
                                    │
                                    ▼
                       ┌─────────────────────────┐
                       │ Media Player / Playback │
                       └─────────────────────────┘

```

---

## 📁 Repository Structure

All source files are structured within a single flat package for straightforward compilation:

```
producer-consumer-stream/
│
├── StreamApp.java             # Main application entry point & JTabbedPane container
├── SharedBuffer.java          # Core OS Monitor class handling synchronization & metrics
├── Chunk.java                 # Data model for streaming multimedia chunks
├── ProducerTask.java          # Runnable thread logic for video server ingestion
├── ConsumerTask.java          # Runnable thread logic for video player consumption
├── MainDashboard.java         # System Overview panel & real-time slot grid
├── ProducerPanel.java         # Server tuning panel (speed sliders & history)
├── ConsumerPanel.java         # Player rendering panel (playback canvas & underflow alerts)
└── VisualizationsPanel.java   # Real-time Analytics suite & custom Graphics2D charts

```

---

## 🚀 Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 11 or higher installed and configured in your system `PATH`.

### Compilation & Execution

1. **Clone the repository:**
```bash
git clone https://github.com/your-username/producer-consumer-stream.git
cd producer-consumer-stream

```


2. **Compile all Java source files:**
* **Windows (Command Prompt / PowerShell):**
```cmd
del *.class
javac *.java

```


* **Linux / macOS:**
```bash
rm -f *.class
javac *.java

```




3. **Run the Application:**
```bash
java StreamApp

```

---

## 🎮 How to Use

1. Launch the app to open the **Main Overview** tab.
2. Use the **Buffer Capacity Selector** drop-down menu at the top to adjust buffer size dynamically between 4, 8, 10, or 16 slots.
3. Switch to the **Producer Server** tab to adjust the generation interval (500 ms – 4000 ms) or pause/resume chunk production.
4. Switch to the **Consumer Client** tab to simulate video playback, adjust decoding speed, or observe **Buffer Starvation / Buffering** state when consumption outpaces production.
5. Navigate to the **Real-Time Analytics** tab to view live graphical feedback on buffer occupancy, pipeline stage flow, and cumulative production vs. consumption rates.
