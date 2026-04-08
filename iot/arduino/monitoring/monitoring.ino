#include <TinyGPS++.h>
#include <SoftwareSerial.h>

// ============== GPS CONFIG ==============
// WIRING:
// GPS TX -> Arduino Pin 4
// GPS RX -> Disconnected (or Pin 3, but unused)
static const int RXPin = 4;
static const int TXPin = 3;

TinyGPSPlus gps;
SoftwareSerial ss(RXPin, TXPin);

unsigned long lastLogTime = 0;
const unsigned long LOG_INTERVAL = 30000; // 30 Seconds

// ============== PULSE CONFIG ==============
#define PULSE_PIN A0
#define SAMPLE_INTERVAL_MS 2
#define DEBUG_INTERVAL_MS  200
#define DEBUG 0

int Signal = 0;
int BPM = 0;
int IBI = 600;
unsigned long lastBeatTime = 0;
bool firstBeat = true;
bool secondBeat = false;
int rate[10];
int beatCount = 0;

float baseline = 0;
float alpha = 0.005;
int riseThreshold = 7;
int rearmThreshold = 3;

bool fired = false;

int peakDeviation = 0;
int troughDeviation = 0;

unsigned long lastSampleTime = 0;
unsigned long lastDebugTime = 0;

// ============== SETUP ==============
void setup() {
  Serial.begin(115200);
  ss.begin(9600);
  pinMode(PULSE_PIN, INPUT);

  Serial.println("DEBUG: System Started.");
  Serial.println("DEBUG: Waiting for GPS module...");

  // --- Pulse sensor baseline calibration ---
  Serial.println("DEBUG: Settling pulse sensor for 4 seconds... keep finger steady");
  long sum = 0;
  int count = 0;
  unsigned long settleStart = millis();
  while (millis() - settleStart < 2000) {
    analogRead(PULSE_PIN);
    delay(SAMPLE_INTERVAL_MS);
  }
  settleStart = millis();
  while (millis() - settleStart < 2000) {
    sum += analogRead(PULSE_PIN);
    count++;
    delay(SAMPLE_INTERVAL_MS);
  }
  baseline = (float)sum / count;

  if (DEBUG) {
    Serial.print("DEBUG: Pulse Baseline: "); Serial.println((int)baseline);
    Serial.println("---");
  }

  for (int i = 0; i < 10; i++) rate[i] = 0;
  lastBeatTime = millis();
}

// ============== LOOP ==============
void loop() {
  unsigned long now = millis();

  // -------- GPS Section --------
  // 1. Constantly feed the GPS library (non-blocking)
  while (ss.available() > 0) {
    gps.encode(ss.read());
  }

  // 2. Wiring check (runs once after 5 seconds)
  if (millis() > 5000 && gps.charsProcessed() < 10) {
    Serial.println("DEBUG: ERROR: No GPS data received! Check wiring (TX connected to Pin 4?)");
    // Don't return here — still want pulse sensor to run
  }

  // 3. GPS log every 30 seconds
  if (now - lastLogTime > LOG_INTERVAL) {
    lastLogTime = now;

    if (gps.location.isValid()) {
      Serial.print("-gps-");
      Serial.print(gps.location.lat(), 6);
      Serial.print(",");
      Serial.println(gps.location.lng(), 6);

      Serial.print("DEBUG: Satellites: ");
      Serial.println(gps.satellites.value());

    } else {
      Serial.print("DEBUG: GPS connected but no Fix yet. ");
      Serial.print("Satellites visible: ");
      Serial.println(gps.satellites.value());
      Serial.println("DEBUG: Move antenna near a window.");
    }
  }

  // -------- Pulse Sensor Section --------
  if (now - lastSampleTime < SAMPLE_INTERVAL_MS) return;
  lastSampleTime = now;

  Signal = analogRead(PULSE_PIN);
  unsigned long elapsed = now - lastBeatTime;

  baseline = baseline * (1.0 - alpha) + Signal * alpha;

  int deviation = Signal - (int)baseline;

  if (deviation > peakDeviation)   peakDeviation = deviation;
  if (deviation < troughDeviation) troughDeviation = deviation;

  if (!fired) {
    // ARMED — looking for rising edge, min 400ms between beats (150 BPM max)
    if (elapsed > 400 && deviation > riseThreshold) {
      fired = true;
      IBI = elapsed;
      lastBeatTime = now;

      if (DEBUG) {
        Serial.print("[BEAT] Sig="); Serial.print(Signal);
        Serial.print(" Base="); Serial.print((int)baseline);
        Serial.print(" Dev="); Serial.print(deviation);
        Serial.print(" IBI="); Serial.print(IBI);
      }

      if (secondBeat) {
        secondBeat = false;
        for (int i = 0; i < 10; i++) rate[i] = IBI;
        if (DEBUG) Serial.print(" [2nd]");
      }

      if (firstBeat) {
        firstBeat = false;
        secondBeat = true;
        if (DEBUG) Serial.println(" [1st-skip]");
        return;
      }

      long runningTotal = 0;
      for (int i = 0; i < 9; i++) {
        rate[i] = rate[i + 1];
        runningTotal += rate[i];
      }
      rate[9] = IBI;
      runningTotal += IBI;
      runningTotal /= 10;
      BPM = 60000 / runningTotal;
      if (BPM > 150) BPM = 0;
      if (BPM < 30)  BPM = 0;

      beatCount++;

      if (DEBUG) {
        Serial.print(" BPM="); Serial.println(BPM);
      } else {
        if (BPM > 0) {
          Serial.print("-heart-");
          Serial.println(BPM);
        }
      }
    }
  } else {
    // FIRED — wait for signal to drop below baseline before re-arming
    if (deviation < -rearmThreshold) {
      fired = false;
      if (DEBUG) {
        Serial.print("[REARM] Dev="); Serial.println(deviation);
      }
    }
  }

  // No beat for 4s — full reset
  if (elapsed > 4000) {
    firstBeat = true;
    secondBeat = false;
    fired = false;
    lastBeatTime = now;
    BPM = 0;
    if (DEBUG) Serial.println("[RESET]");
  }

  // Periodic debug
  if (DEBUG && now - lastDebugTime >= DEBUG_INTERVAL_MS) {
    lastDebugTime = now;
    Serial.print("[STATE] Sig="); Serial.print(Signal);
    Serial.print(" Base="); Serial.print((int)baseline);
    Serial.print(" Dev="); Serial.print(deviation);
    Serial.print(" Pk="); Serial.print(peakDeviation);
    Serial.print(" Tr="); Serial.print(troughDeviation);
    Serial.print(" fired="); Serial.print(fired);
    Serial.print(" elap="); Serial.print(elapsed);
    Serial.print("ms BPM="); Serial.print(BPM);
    Serial.print(" beats="); Serial.println(beatCount);
    peakDeviation = 0;
    troughDeviation = 0;
  }
}
