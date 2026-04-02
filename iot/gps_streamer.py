import random
import time
import serial
import requests

# --- CONFIGURATION ---
USE_MOCK_DATA = True  # Set to False for real Arduino data

SERIAL_PORT = "COM4"  # Change to your Arduino port
BAUD_RATE = 9600

# ⚠️  MUST match the patient's Keycloak subject ID.
# The Angular frontend polls: dweet.cc/get/latest/dweet/for/tfk-gps-{keycloakId}
# Replace with the patient's Keycloak ID
KEYCLOAK_ID = "90ad6c94-d4e0-4d6c-99ad-5c8431a62ce8"
THING_NAME = f"tfk-gps-{KEYCLOAK_ID}"
DWEET_URL = f"https://dweet.cc/dweet/for/{THING_NAME}"

# Mock data base coordinates (Tunis, Tunisia)
MOCK_BASE_LAT = 36.807186
MOCK_BASE_LNG = 10.105372


# --- GPS FUNCTIONS ---

def get_mock_gps() -> tuple[str, str]:
    """Generate mock GPS data with small random drift around the base coordinates."""
    lat = MOCK_BASE_LAT + random.uniform(-0.001, 0.001)
    lng = MOCK_BASE_LNG + random.uniform(-0.001, 0.001)
    return f"{lat:.6f}", f"{lng:.6f}"


def send_to_dweet(lat: str, lng: str) -> None:
    """Send a GPS reading to dweet.cc via GET request with query parameters."""
    print(
        f"📍 Sending: lat={lat}, lng={lng} → {THING_NAME} ... ", end="", flush=True)
    try:
        url = f"{DWEET_URL}?lat={lat}&lng={lng}"
        response = requests.get(url, timeout=5)
        if response.status_code == 200:
            print("✅ Sent!")
        else:
            print(f"⚠️  Unexpected status: {response.status_code}")
    except requests.exceptions.Timeout:
        print("❌ Timeout — dweet.cc did not respond in time.")
    except requests.exceptions.ConnectionError:
        print("❌ Connection error — check your internet connection.")
    except requests.exceptions.RequestException as e:
        print(f"❌ Request failed: {e}")


# --- RUN MODES ---

def run_mock_mode() -> None:
    """Continuously stream simulated GPS data."""
    print("🎭 MOCK MODE — Simulating GPS movement near Tunis")
    print(f"🚀 Streaming to thing: {THING_NAME}")
    print("   Press Ctrl+C to stop.\n")

    while True:
        lat, lng = get_mock_gps()
        send_to_dweet(lat, lng)
        time.sleep(2)


def run_real_mode() -> None:
    """Stream real GPS data received from an Arduino over serial."""
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    print(f"📡 Connected to {SERIAL_PORT} at {BAUD_RATE} baud")
    print(f"🚀 Streaming to thing: {THING_NAME}")
    print("   Press Ctrl+C to stop.\n")

    last_send_time = 0.0
    last_lat: str | None = None
    last_lng: str | None = None

    try:
        while True:
            if ser.in_waiting > 0:
                try:
                    line = ser.readline().decode("utf-8", errors="ignore").strip()
                    # Expect lines like "36.807186,10.105372" — skip debug output
                    if "," in line and "DEBUG" not in line:
                        parts = line.split(",")
                        if len(parts) == 2:
                            last_lat, last_lng = parts[0].strip(
                            ), parts[1].strip()
                except Exception as e:
                    print(f"⚠️  Serial read error: {e}")

            current_time = time.time()
            if last_lat is not None and (current_time - last_send_time) >= 1:
                send_to_dweet(last_lat, last_lng)
                last_send_time = current_time

            time.sleep(0.05)
    finally:
        ser.close()
        print("🔌 Serial port closed.")


# --- ENTRY POINT ---

if __name__ == "__main__":
    try:
        if USE_MOCK_DATA:
            run_mock_mode()
        else:
            run_real_mode()
    except KeyboardInterrupt:
        print("\n🛑 Stopped by user.")
