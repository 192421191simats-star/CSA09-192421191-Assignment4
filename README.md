# CSA09 – Programming in Java · Assignment 4 (CO4): Event-Driven GUI Design

| | |
|---|---|
| **Name** | `Prabhenjan Srikanth R` |
| **Register Number** | `192421191` |
| **Java Version** | OpenJDK 21 (compiles on JDK 17+) – check yours with `java -version` |
| **Report** | [`report/Assignment4_Report.pdf`](report/Assignment4_Report.pdf) (also `.docx` and `.md`) |

## Repository layout

Each task has its own folder, and each folder contains:

| Folder | Contents |
|---|---|
| `*.java` / `src/` | Source code |
| `input/` | Input files |
| `output/` | Console output of every run |
| `screenshots/` | Screenshots; each image has a caption naming the requirement or test it proves |
| `data/` | Raw measurements (CSV) |
| `graphs/` | Graphs |

Other folders and files at the top level:

- `tools/ScreenshotKit.java` drives the windows with `java.awt.Robot` (real clicks and keys) and captures them. A copy sits in each task folder.
- `tools/make_graphs.py` and `tools/build_report.py` rebuild the graphs and the report.
- `run_all.sh` compiles and runs everything (`./run_all.sh` for all tasks, or `./run_all.sh 4` for Task04 only).
- `git_commit_task.sh` commits task by task (see below).

## Compile and run

Run all commands from inside the task folder. On Windows, use `;` instead of `:` in `-cp`.

### Task01 – Fee-payment window: diagnosis and redesign

```bash
javac -d /tmp/orig original/FeeWindow.java     # the given code: compiler errors (recorded in output/)
javac -d out $(find src -name "*.java") OriginalBehaviourDemo.java ScreenshotKit.java Task1Driver.java DispatchBenchmark.java
java -cp out fees.FeeWindow                    # redesigned window (3 s simulated bank)
java -cp out fees.FeeServiceTest               # 12 service tests, no window
java -cp out Task1Driver                       # automated evidence + screenshots
java -cp out DispatchBenchmark                 # if-else chain vs one listener per button
```

**Result:** 10 deficiencies were shown with evidence. The original froze the window for about 2.8 s and its Receipt button cleared the form. The redesign (FeeWindow → FeeService → PaymentGateway / FeeRepository, with a SwingWorker) kept event-thread latency at 0 ms during the bank wait. 12/12 tests pass. Dispatch through the if-else chain is O(k) (17→205 ns for k=3→100); a listener per button is O(1) (about 2–4 ns).

### Task02 – Hospital appointment desk

```bash
javac -d out *.java
java -cp out AppointmentDesk input/doctors.csv
java -cp out Task2Driver input/doctors.csv     # screenshots + output/t2_event_trace.txt
java -cp out DoctorLookupBenchmark
```

**Result:** Changing the department refreshes the doctors from a Map loaded once. Booking is validated. Closing while a booking is in progress asks for confirmation (No keeps the window, Yes closes it). The full window-event trace is recorded. A Map lookup takes 0.3–2 µs per change, against more than 2 ms per database fetch.

### Task03 – Library search screen (AWT)

```bash
javac -d out *.java
java -cp out LibrarySearchScreen input/catalogue.csv
java -cp out Task3Driver input/catalogue.csv   # interactions, 3 window sizes, alternatives, layout timing
```

**Result:** Nested BorderLayout/FlowLayout panels. At 520×340, 800×520 and 1100×720 only the results area grows (520×212 → 1100×592), while the buttons keep their size. GridBagLayout behaves the same way. The null layout leaves empty space when enlarged and clips components when shrunk.

### Task04 – Draw pad forensics (Complete the Code: `DrawPad.java`)

```bash
javac -d out DrawPad.java DrawPadOriginal.java DrawPadGetGraphics.java DrawPadBuffered.java ScreenshotKit.java Task4Driver.java RepaintBenchmark.java
java -cp out DrawPad                           # click, minimise, restore: dots stay
javac -d /tmp/x compile_error_demo/DrawPad.java   # @Override + mouseclicked -> compile error
java -cp out Task4Driver
java -cp out RepaintBenchmark
```

**Result:**

- The original never counts a click, because `mouseclicked` is not `mouseClicked`.
- The `getGraphics()` version loses its dots after minimise and restore.
- The repaired version stores the points and draws them in `paint()`, so the dots survive.
- `@Override` produces "method does not override or implement a method from a supertype".
- Repainting stored points is O(n) (205 ms at 100,000 clicks); copying a BufferedImage is O(w·h) (0.4 ms).

### Task05 – Converter in four styles (Complete the Code: `Converter.java`)

```bash
javac -d out Converter.java && java -cp out Converter
javac -d out_v1 styles/v1/*.java && java -cp out_v1 ConverterLambda   # also ConverterImplements / ConverterNamedInner / ConverterAnonymous
javac -d out_v2 styles/v2/*.java && java -cp out_v2 ConverterLambda   # Enter key + dialog version
javac -d out_action -cp out_v1 ConverterSharedAction.java && java -cp out_action:out_v1 ConverterSharedAction
javac -d out_driver -cp out:out_action:out_v1 ScreenshotKit.java Task5Driver.java
java -cp out_driver:out Task5Driver complete   # also: v1 / v2 (with out_v1 / out_v2) / action
```

**Result:** 1000 INR gives 12.00 USD; 2500 INR with Enter gives 27.50 EUR; `12a` gives "Invalid amount". The change effort for the new requirement was:

| Style | Lines added | Lines removed |
|---|---|---|
| implements | 5 | 3 |
| named inner | 6 | 3 |
| anonymous | 7 | 4 |
| lambda | 4 | 2 |

One shared `Action` serves the button, the menu item, Enter and Ctrl+Enter with a single object.

### Task06 – Campus event registration (Swing + JDBC + MySQL)

```bash
javac -d out $(find src -name "*.java")
# (a) with MySQL 8 - the intended configuration
mysql -u root -p < sql/schema.sql              # creates campus_events, tables, UNIQUE key, events
#     create a user, put url/user/password in db.properties,
#     download MySQL Connector/J into lib/ (e.g. lib/mysql-connector-j-9.1.0.jar)
java -cp out:lib/mysql-connector-j-9.1.0.jar campus.App db.properties
# (b) offline demo with the in-process test-double driver (no MySQL needed)
java -cp out campus.App db-demo.properties
java -cp out campus.tools.RegistrationServiceTest               # 18 tests
javac -d out -cp out ScreenshotKit.java Task6Driver.java && java -cp out Task6Driver db-demo.properties
java -Xmx1g -cp out campus.tools.PagingBenchmark                # all-at-once vs paged, 100,000 rows
```

**Result:**

- **Layers:** ui → service → dao (JDBC `PreparedStatement`) → db, with a SwingWorker for every database call.
- **Behaviour shown:** invalid input is rejected field by field; duplicates are refused (by the service check and by the UNIQUE key); full events are refused; paging uses LIMIT/OFFSET; filtering by event and CSV export both work.
- **Tests:** 18/18 tests pass, and the GUI demo covers TC1–TC8.
- **Paging vs all-at-once:** the first page appears in 1.6 ms using 2.6 KB, against 535 ms and 13.6 MB to load everything. Loading everything on the EDT froze the window for 535 ms.

> The `campus.db.testdouble.FakeCampusDriver` driver exists only so the evidence could be produced where MySQL was not available. It answers exactly the DAO's SQL statements and enforces the same UNIQUE key. Run the application against MySQL (option a) on your own machine.

## About the screenshots

The screenshots were taken automatically on Linux, under a virtual X display (Xvfb) with **no window manager**. As a result they show no title bars, and "minimise and restore" was simulated by hiding and showing the window, which triggers the same repaint. If your faculty expects normal desktop windows, run the `TaskNDriver` programs, or the applications themselves, on your own machine and replace the images.

## Committing progressively (at least one commit per task)

```bash
bash git_commit_task.sh init https://github.com/<user>/CSA09-<RegisterNumber>-Assignment4.git
bash git_commit_task.sh 1        # Task01 ... then 2, 3, 4, 5, 6 on different days
bash git_commit_task.sh report
```

## Rebuilding graphs and the report

```bash
python3 tools/make_graphs.py
cd report && pandoc Assignment4_Report.md -o tmp.docx --resource-path=. \
  && python3 ../tools/build_report.py tmp.docx Assignment4_Report.docx Assignment4_Report.md \
  && soffice --headless --convert-to pdf Assignment4_Report.docx && rm tmp.docx
```
