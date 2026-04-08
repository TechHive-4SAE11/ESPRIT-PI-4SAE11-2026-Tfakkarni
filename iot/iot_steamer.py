import random
import time
import serial
import requests

# --- CONFIGURATION ---
# Control mock/real independently for GPS and BPM.
# Set to True to use simulated data, False to read from Arduino serial.
MOCK_GPS = True
MOCK_BPM = False

SERIAL_PORT = "COM4"  # Change to your Arduino port
BAUD_RATE = 115200     # Must match the merged Arduino sketch

# MUST match the patient's Keycloak subject ID.
# The Angular frontend polls: dweet.cc/get/latest/dweet/for/tfk-gps-{keycloakId}
# Replace with the patient's Keycloak ID
KEYCLOAK_ID = "90ad6c94-d4e0-4d6c-99ad-5c8431a62ce8"
THING_NAME = f"tfk-gps-{KEYCLOAK_ID}"
DWEET_URL = f"https://dweet.cc/dweet/for/{THING_NAME}"

# Mock data base coordinates (Tunis, Tunisia)
MOCK_BASE_LAT = 36.898713
MOCK_BASE_LNG = 10.188961
MOCK_BASE_BPM = 72

# Send interval in seconds (how often we push to dweet)
SEND_INTERVAL = 2


# --- MOCK DATA ---

def get_mock_gps() -> tuple[str, str]:
    """Generate mock GPS data with small random drift around the base coordinates."""
    lat = MOCK_BASE_LAT + random.uniform(-0.001, 0.001)
    lng = MOCK_BASE_LNG + random.uniform(-0.001, 0.001)
    return f"{lat:.6f}", f"{lng:.6f}"


def get_mock_bpm() -> int:
    """Generate mock BPM data with small random variation."""
    return MOCK_BASE_BPM + random.randint(-5, 5)


# --- DWEET SENDER ---

def send_to_dweet(lat: str | None, lng: str | None, bpm: int | None) -> None:
    """Send GPS + heartbeat data to dweet.cc. Includes whichever fields are available."""
    params = {}
    if lat is not None and lng is not None:
        params["lat"] = lat
        params["lng"] = lng
    if bpm is not None:
        params["bpm"] = bpm

    if not params:
        return  # nothing to send

    parts = []
    if "lat" in params:
        parts.append(f"lat={params['lat']}, lng={params['lng']}")
    if "bpm" in params:
        parts.append(f"bpm={params['bpm']}")
    label = ", ".join(parts)

    print(f"Sending: {label} -> {THING_NAME} ... ", end="", flush=True)
    try:
        response = requests.get(DWEET_URL, params=params, timeout=5)
        if response.status_code == 200:
            print("Sent!")
        else:
            print(f"Unexpected status: {response.status_code}")
    except requests.exceptions.Timeout:
        print("Timeout -- dweet.cc did not respond in time.")
    except requests.exceptions.ConnectionError:
        print("Connection error -- check your internet connection.")
    except requests.exceptions.RequestException as e:
        print(f"Request failed: {e}")


# --- RUN ---

def needs_serial() -> bool:
    """Return True if at least one sensor uses real Arduino data."""
    return not MOCK_GPS or not MOCK_BPM


def run() -> None:
    """Hybrid mode: each sensor can be mock or real independently."""
    gps_label = "MOCK" if MOCK_GPS else "REAL (serial)"
    bpm_label = "MOCK" if MOCK_BPM else "REAL (serial)"
    print(f"GPS: {gps_label}  |  BPM: {bpm_label}")
    print(f"Streaming to thing: {THING_NAME}")
    print("Press Ctrl+C to stop.\n")

    ser = None
    if needs_serial():
        ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
        print(f"Connected to {SERIAL_PORT} at {BAUD_RATE} baud")

    last_send_time = 0.0
    last_real_lat: str | None = None
    last_real_lng: str | None = None
    last_real_bpm: int | None = None

    try:
        while True:
            # ── Read serial if needed ────────────────────────────────
            if ser and ser.in_waiting > 0:
                try:
                    line = ser.readline().decode("utf-8", errors="ignore").strip()

                    if line.startswith("-gps-") and not MOCK_GPS:
                        payload = line[len("-gps-"):]
                        parts = payload.split(",")
                        if len(parts) == 2:
                            last_real_lat = parts[0].strip()
                            last_real_lng = parts[1].strip()
                            print(f"[GPS] lat={last_real_lat}, lng={last_real_lng}")

                    elif line.startswith("-heart-") and not MOCK_BPM:
                        payload = line[len("-heart-"):]
                        try:
                            last_real_bpm = int(payload.strip())
                            print(f"[HEART] bpm={last_real_bpm}")
                        except ValueError:
                            print(f"[HEART] Bad value: {payload!r}")

                    elif line.startswith("DEBUG"):
                        print(f"  {line}")

                except Exception as e:
                    print(f"Serial read error: {e}")

            # ── Resolve values (mock or real) ────────────────────────
            current_time = time.time()
            if (current_time - last_send_time) >= SEND_INTERVAL:
                lat, lng, bpm = None, None, None

                if MOCK_GPS:
                    lat, lng = get_mock_gps()
                else:
                    lat, lng = last_real_lat, last_real_lng

                if MOCK_BPM:
                    bpm = get_mock_bpm()
                else:
                    bpm = last_real_bpm

                if lat is not None or bpm is not None:
                    send_to_dweet(lat, lng, bpm)
                    last_send_time = current_time

            time.sleep(0.05)
    finally:
        if ser:
            ser.close()
            print("Serial port closed.")


# --- ENTRY POINT ---

if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        print("\nStopped by user.")
