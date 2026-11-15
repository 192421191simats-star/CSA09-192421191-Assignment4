#!/usr/bin/env bash
# CSA09 Assignment 4 - compiles and runs every task, writing console output to TaskNN/output/*.txt.
#   ./run_all.sh          all tasks
#   ./run_all.sh 3 5      only Task03 and Task05
# GUI drivers need a display. On Linux without one they run under xvfb-run automatically.
# On Windows/macOS run the driver commands from README.md directly (a desktop is available).
set -u
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS
ROOT=$(pwd)

if [ -n "${DISPLAY:-}" ] || [ "$(uname)" = "Darwin" ]; then GUI=""; else GUI='xvfb-run -a -s "-screen 0 1400x900x24"'; fi
gui() { eval "timeout 300 $GUI java $*"; }

task1() {
  cd "$ROOT/Task01" && mkdir -p output data screenshots
  echo "== Task01"
  javac -d /tmp/csa09_t1_orig original/FeeWindow.java > output/original_compile.txt 2>&1
  mkdir -p /tmp/csa09_t1_c && cp original/FeeWindowWithConstants.java.txt /tmp/csa09_t1_c/FeeWindow.java
  javac -d /tmp/csa09_t1_c/out /tmp/csa09_t1_c/FeeWindow.java 2>&1 | sed 's|/tmp/csa09_t1_c/|original/FeeWindowWithConstants.java.txt -> |' > output/original_compile_with_constants.txt
  cp ../tools/ScreenshotKit.java .
  javac -d out $(find src -name "*.java") OriginalBehaviourDemo.java ScreenshotKit.java Task1Driver.java DispatchBenchmark.java || return 1
  java -cp out fees.FeeServiceTest > output/t1_feeservice_tests.txt
  gui -cp out Task1Driver > output/t1_driver.txt 2>&1
  java -cp out DispatchBenchmark > output/t1_dispatch_benchmark.txt
}

task2() {
  cd "$ROOT/Task02" && mkdir -p output data screenshots
  echo "== Task02"
  cp ../tools/ScreenshotKit.java .
  javac -d out *.java || return 1
  gui -cp out Task2Driver input/doctors.csv > output/t2_driver.txt 2>&1
  java -cp out DoctorLookupBenchmark > output/t2_lookup_benchmark.txt
}

task3() {
  cd "$ROOT/Task03" && mkdir -p output data screenshots
  echo "== Task03"
  cp ../tools/ScreenshotKit.java .
  javac -d out *.java || return 1
  gui -cp out Task3Driver input/catalogue.csv > output/t3_driver.txt 2>&1
}

task4() {
  cd "$ROOT/Task04" && mkdir -p output data screenshots
  echo "== Task04"
  javac -d /tmp/csa09_t4_err compile_error_demo/DrawPad.java > output/compile_error_override.txt 2>&1
  mkdir -p /tmp/csa09_t4_ok && cp compile_error_demo/DrawPadNoOverride.java.txt /tmp/csa09_t4_ok/DrawPad.java
  if javac -d /tmp/csa09_t4_ok/out /tmp/csa09_t4_ok/DrawPad.java > /tmp/csa09_t4_ok/log 2>&1; then
    echo "compile_error_demo/DrawPadNoOverride.java.txt (mouseclicked, no @Override): compiled with NO error - the defect is silent" > output/compile_no_override.txt
  else cat /tmp/csa09_t4_ok/log > output/compile_no_override.txt; fi
  cp ../tools/ScreenshotKit.java .
  javac -d out DrawPad.java DrawPadOriginal.java DrawPadGetGraphics.java DrawPadBuffered.java ScreenshotKit.java Task4Driver.java RepaintBenchmark.java || return 1
  gui -cp out Task4Driver > output/t4_driver.txt 2>&1
  java -cp out RepaintBenchmark > output/t4_repaint_benchmark.txt
}

task5() {
  cd "$ROOT/Task05" && mkdir -p output screenshots
  echo "== Task05"
  cp ../tools/ScreenshotKit.java .
  javac -d out Converter.java || return 1
  javac -d out_v1 styles/v1/*.java && javac -d out_v2 styles/v2/*.java || return 1
  javac -d out_action -cp out_v1 ConverterSharedAction.java || return 1
  javac -d out_driver -cp out:out_action:out_v1 ScreenshotKit.java Task5Driver.java || return 1
  gui -cp out_driver:out Task5Driver complete > output/t5_complete.txt 2>&1
  gui -cp out_driver:out:out_v1 Task5Driver v1 > output/t5_v1.txt 2>&1
  gui -cp out_driver:out:out_v2 Task5Driver v2 > output/t5_v2.txt 2>&1
  gui -cp out_driver:out:out_action:out_v1 Task5Driver action > output/t5_action.txt 2>&1
  { echo "Task 5.2 - change effort: lines added / removed per style (diff styles/v1 -> styles/v2)"
    for s in ConverterImplements ConverterNamedInner ConverterAnonymous ConverterLambda; do
      add=$(diff styles/v1/$s.java styles/v2/$s.java | grep -c '^>'); del=$(diff styles/v1/$s.java styles/v2/$s.java | grep -c '^<')
      printf "%-22s +%-3s -%s\n" "$s" "$add" "$del"; done
    echo; for s in ConverterImplements ConverterNamedInner ConverterAnonymous ConverterLambda; do
      echo "----- $s"; diff -u styles/v1/$s.java styles/v2/$s.java | tail -n +3; done; } > output/t5_change_effort.txt
}

task6() {
  cd "$ROOT/Task06" && mkdir -p output data screenshots
  echo "== Task06"
  cp ../tools/ScreenshotKit.java .
  javac -d out $(find src -name "*.java") || return 1
  javac -d out -cp out ScreenshotKit.java Task6Driver.java || return 1
  java -cp out campus.tools.RegistrationServiceTest > output/t6_service_tests.txt
  rm -f output/registrations_hackathon.csv
  gui -cp out Task6Driver db-demo.properties > output/t6_gui_demo.txt 2>&1
  { java -Xmx1g -Djava.awt.headless=true -cp out campus.tools.PagingBenchmark "jdbc:campusdb:bench?schema=sql/schema.sql" inmemory
    echo; echo "=== simulated network: 1 ms per statement + 5 us per row ==="
    java -Xmx1g -Djava.awt.headless=true -cp out campus.tools.PagingBenchmark "jdbc:campusdb:bench?schema=sql/schema.sql&latencyMs=1&rowMicros=5" network
  } > output/t6_paging_benchmark.txt 2>&1
}

TASKS=("$@"); [ ${#TASKS[@]} -eq 0 ] && TASKS=(1 2 3 4 5 6)
for t in "${TASKS[@]}"; do "task$t" || echo "Task $t FAILED"; done
cd "$ROOT"; echo "done - see TaskNN/output/ and TaskNN/screenshots/"
