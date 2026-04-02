import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface DweetPosition {
  lat: number;
  lng: number;
}

@Injectable({
  providedIn: 'root',
})
export class DweetService {
  private readonly http = inject(HttpClient);

  getLatestPosition(thingName: string): Observable<DweetPosition> {
    // Use Angular dev-server proxy (/dweet-proxy → https://dweet.cc) to avoid CORS
    return this.http
      .get<any>(`/dweet-proxy/get/latest/dweet/for/${encodeURIComponent(thingName)}`)
      .pipe(
        map(response => {
          // dweet.cc returns { this: "succeeded", with: [{ content: { lat, lng } }] }
          if (response?.this !== 'succeeded') {
            throw new Error(`dweet.cc: ${response?.this ?? 'unknown response'}`);
          }
          const content = response?.with?.[0]?.content;
          if (!content?.lat || !content?.lng) {
            throw new Error('No GPS data — is the Python streamer running?');
          }
          return {
            lat: Number.parseFloat(content.lat),
            lng: Number.parseFloat(content.lng),
          };
        })
      );
  }
}
