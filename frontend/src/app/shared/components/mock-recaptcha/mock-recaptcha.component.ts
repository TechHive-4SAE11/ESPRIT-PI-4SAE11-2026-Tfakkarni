import { Component, EventEmitter, Output, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * MockRecaptchaComponent
 *
 * A fully offline-compatible "I'm not a robot" checkbox widget that mimics
 * the visual appearance of Google reCAPTCHA v2. This is used in the dev
 * environment to avoid ERR_NAME_NOT_RESOLVED errors caused by the browser
 * trying to reach www.google.com/recaptcha/api.js.
 *
 * It implements ControlValueAccessor so it works transparently with
 * [(ngModel)] just like the real ng-recaptcha widget.
 *
 * In a real production deployment, swap this component for <re-captcha>
 * and provide a valid RECAPTCHA_V3_SITE_KEY.
 */
@Component({
    selector: 'app-mock-recaptcha',
    standalone: true,
    imports: [CommonModule],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MockRecaptchaComponent),
            multi: true,
        },
    ],
    template: `
    <div class="mock-recaptcha-box" [class.checked]="isChecked" (click)="toggle()">
      <!-- Checkbox area -->
      <div class="recaptcha-left">
        <div class="recaptcha-checkbox" [class.verified]="isChecked">
          @if (isLoading) {
            <div class="spinner"></div>
          } @else if (isChecked) {
            <svg class="checkmark" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          }
        </div>
        <span class="recaptcha-label">I'm not a robot</span>
      </div>

      <!-- reCAPTCHA branding area -->
      <div class="recaptcha-right">
        <div class="recaptcha-logo">
          <svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg">
            <path d="M32 4 L60 52 L4 52 Z" fill="#4A90D9" opacity="0.85"/>
            <path d="M32 18 L50 50 L14 50 Z" fill="#fff" opacity="0.3"/>
            <circle cx="32" cy="38" r="8" fill="#fff" opacity="0.9"/>
          </svg>
        </div>
        <div class="recaptcha-brand-text">reCAPTCHA</div>
        <div class="recaptcha-brand-sub">Privacy - Terms</div>
      </div>
    </div>
  `,
    styles: [`
    .mock-recaptcha-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 300px;
      min-height: 74px;
      background: #f9f9f9;
      border: 1px solid #d3d3d3;
      border-radius: 3px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.08);
      padding: 0 16px 0 12px;
      cursor: pointer;
      user-select: none;
      transition: box-shadow 0.2s ease;
    }

    .mock-recaptcha-box:hover {
      box-shadow: 0 2px 8px rgba(0,0,0,0.14);
    }

    .recaptcha-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .recaptcha-checkbox {
      width: 28px;
      height: 28px;
      border: 2px solid #c1c1c1;
      border-radius: 2px;
      background: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: border-color 0.2s, background 0.2s;
    }

    .recaptcha-checkbox.verified {
      border-color: #4a4a4a;
      background: #fff;
    }

    .checkmark {
      width: 18px;
      height: 18px;
      color: #4a4a4a;
      stroke-width: 3;
    }

    .recaptcha-label {
      font-size: 14px;
      color: #4a4a4a;
      font-family: Roboto, sans-serif;
    }

    .recaptcha-right {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 2px;
    }

    .recaptcha-logo {
      width: 32px;
      height: 32px;
    }

    .recaptcha-logo svg {
      width: 100%;
      height: 100%;
    }

    .recaptcha-brand-text {
      font-size: 8px;
      color: #8d8d8d;
      font-family: Roboto, sans-serif;
      letter-spacing: 0.5px;
      font-weight: 500;
    }

    .recaptcha-brand-sub {
      font-size: 7px;
      color: #c1c1c1;
      font-family: Roboto, sans-serif;
    }

    /* Spinner animation for the brief "verifying" delay */
    .spinner {
      width: 20px;
      height: 20px;
      border: 2px solid #d3d3d3;
      border-top-color: #4A90D9;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `],
})
export class MockRecaptchaComponent implements ControlValueAccessor {
    @Output() resolved = new EventEmitter<string | null>();

    isChecked = false;
    isLoading = false;
    isDisabled = false;

    private onChange: (value: string | null) => void = () => { };
    private onTouched: () => void = () => { };

    toggle(): void {
        if (this.isDisabled || this.isLoading) return;

        if (this.isChecked) {
            // Uncheck
            this.isChecked = false;
            const val = null;
            this.onChange(val);
            this.resolved.emit(val);
        } else {
            // Simulate a brief "verifying" spinner (like the real reCAPTCHA)
            this.isLoading = true;
            setTimeout(() => {
                this.isLoading = false;
                this.isChecked = true;
                const mockToken = `mock-recaptcha-token-${Date.now()}`;
                this.onChange(mockToken);
                this.onTouched();
                this.resolved.emit(mockToken);
            }, 800);
        }
    }

    // ControlValueAccessor interface
    writeValue(value: string | null): void {
        this.isChecked = !!value;
    }

    registerOnChange(fn: (value: string | null) => void): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.isDisabled = isDisabled;
    }
}
