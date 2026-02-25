import { Injectable, signal, effect, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly _theme = signal<Theme>('light');

  readonly theme = this._theme.asReadonly();
  readonly isDark = () => this._theme() === 'dark';

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      // Read from localStorage or system preference
      const stored = localStorage.getItem('tfakkarni-theme') as Theme | null;
      if (stored === 'dark' || stored === 'light') {
        this._theme.set(stored);
      } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
        this._theme.set('dark');
      }
      this.applyTheme(this._theme());

      // React to changes
      effect(() => {
        const t = this._theme();
        this.applyTheme(t);
        localStorage.setItem('tfakkarni-theme', t);
      });
    }
  }

  toggle(): void {
    this._theme.update(t => t === 'dark' ? 'light' : 'dark');
  }

  setTheme(theme: Theme): void {
    this._theme.set(theme);
  }

  private applyTheme(theme: Theme): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }
}
