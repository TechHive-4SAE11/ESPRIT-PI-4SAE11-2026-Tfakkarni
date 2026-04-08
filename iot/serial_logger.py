"""
Pulse Sensor Serial Logger
Reads from Arduino serial port and writes all output to pulse_log.txt in real time.

Usage:
    python serial_logger.py
    python serial_logger.py COM5
    python serial_logger.py COM5 115200

Press Ctrl+C to stop.
Requires: pip install pyserial
"""

import serial
import serial.tools.list_ports
import sys
import os
from datetime import datetime

LOG_DIR = r"C:\Users\everp\Downloads\teees"
LOG_FILE = os.path.join(LOG_DIR, "pulse_log.txt")
DEFAULT_BAUD = 115200


def find_arduino_port():
    """Auto-detect the Arduino COM port."""
    ports = serial.tools.list_ports.comports()
    for p in ports:
        desc = (p.description or "").lower()
        if any(keyword in desc for keyword in ["arduino", "ch340", "cp210", "usb serial", "usb-serial"]):
            return p.device
    # If no match, list available ports
    if ports:
        print("Available COM ports:")
        for p in ports:
            print(f"  {p.device} - {p.description}")
        return ports[0].device
    return None


def main():
    # Determine port
    if len(sys.argv) >= 2:
        port = sys.argv[1]
    else:
        port = find_arduino_port()
        if port is None:
            print("No COM port found. Plug in Arduino or specify port: python serial_logger.py COM5")
            sys.exit(1)

    baud = int(sys.argv[2]) if len(sys.argv) >= 3 else DEFAULT_BAUD

    print(f"Connecting to {port} at {baud} baud...")
    print(f"Logging to: {LOG_FILE}")
    print("Press Ctrl+C to stop.\n")

    try:
        ser = serial.Serial(port, baud, timeout=1)
    except serial.SerialException as e:
        print(f"Failed to open {port}: {e}")
        sys.exit(1)

    with open(LOG_FILE, "a", encoding="utf-8") as f:
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        header = f"\n=== Session started: {timestamp} | Port: {port} | Baud: {baud} ===\n"
        f.write(header)
        f.flush()
        print(header.strip())

        try:
            while True:
                raw = ser.readline()
                if raw:
                    line = raw.decode("utf-8", errors="replace").rstrip("\r\n")
                    print(line)
                    f.write(line + "\n")
                    f.flush()
        except KeyboardInterrupt:
            print("\nStopped by user.")
            f.write(f"=== Session ended: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} ===\n")
            f.flush()
        finally:
            ser.close()
            print(f"Log saved to: {LOG_FILE}")


if __name__ == "__main__":
    main()
