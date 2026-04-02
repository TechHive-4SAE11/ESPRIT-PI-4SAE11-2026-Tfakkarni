/**
 * Token Helper Utility for Testing/Development
 * Allows manual token injection via browser console
 */

export class TokenHelper {
  /**
   * Inject a token into localStorage for testing
   * Usage in console: window.__tokenHelper.inject()
   */
  static inject(): void {
    const token = prompt('Paste your access_token here:');
    if (token && token.trim()) {
      try {
        const normalizedToken = token.trim();
        localStorage.setItem('tfk_tokens', JSON.stringify({
          accessToken: normalizedToken,
          access_token: normalizedToken,
        }));
        console.log('%c✅ Token injected successfully!', 'color: green; font-weight: bold; font-size: 14px;');
        console.log('%cToken stored in localStorage["tfk_tokens"]', 'color: green;');
        this.show();
      } catch (error) {
        console.error('❌ Failed to store token:', error);
      }
    } else {
      console.log('❌ No token provided');
    }
  }

  /**
   * Display the current stored token
   * Usage in console: window.__tokenHelper.show()
   */
  static show(): void {
    try {
      // Check all possible storage locations
      let token = null;
      let source = '';

      // Try tfk_tokens with access_token
      let stored = localStorage.getItem('tfk_tokens');
      if (stored) {
        const parsed = JSON.parse(stored);
        token = parsed.access_token || parsed.accessToken;
        source = 'tfk_tokens';
      }

      // If not found, check Keycloak instance directly
      if (!token && typeof window !== 'undefined' && (window as any).__tokenHelper) {
        try {
          // This will be set after injection
          console.log('%cℹ️ No token in localStorage yet', 'color: orange;');
          console.log('%cℹ️ Available storage:', 'color: blue;');
          for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            console.log(`   - ${key}`);
          }
          return;
        } catch (e) {
          // continue
        }
      }

      if (token) {
        console.log(`%c📋 Current Token (from ${source}):`, 'color: blue; font-weight: bold;');
        console.log('%c' + token, 'color: green; font-family: monospace; word-break: break-all;');
      } else {
        console.log('%c⚠️ No token found in localStorage', 'color: orange;');
        console.log('%c💡 Possible fixes:', 'color: blue; font-weight: bold;');
        console.log('  1. Make sure you are logged in in the browser');
        console.log('  2. Run: window.__tokenHelper.inject() to manually inject a token');
        console.log('  3. Check localStorage keys with: window.__tokenHelper.listStorage()');
      }
    } catch (error) {
      console.error('❌ Error reading token:', error);
    }
  }

  /**
   * List all localStorage items for debugging
   * Usage in console: window.__tokenHelper.listStorage()
   */
  static listStorage(): void {
    console.log('%c📦 All localStorage items:', 'color: blue; font-weight: bold;');
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key) {
        const value = localStorage.getItem(key);
        console.log(`%c${key}:`, 'font-weight: bold;', value);
      }
    }
  }

  /**
   * Clear the stored token
   * Usage in console: window.__tokenHelper.clear()
   */
  static clear(): void {
    localStorage.removeItem('tfk_tokens');
    console.log('%c🗑️ Token cleared', 'color: red;');
  }

  /**
   * Test an API call with the current token
   * Usage in console: window.__tokenHelper.testApi('/api/medical-folders')
   */
  static testApi(endpoint: string): void {
    try {
      const tokens = JSON.parse(localStorage.getItem('tfk_tokens') || '{}');
      const token = tokens.accessToken || tokens.access_token || localStorage.getItem('token');

      if (!token) {
        console.error('❌ No token found. Use window.__tokenHelper.inject() first');
        return;
      }

      console.log(`%c🧪 Testing: ${endpoint}`, 'color: blue; font-weight: bold;');

      fetch(`http://localhost:9090${endpoint}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })
        .then(r => {
          console.log(`%cStatus: ${r.status}`, `color: ${r.ok ? 'green' : 'red'}`);
          return r.json();
        })
        .then(data => {
          console.log('%c✅ Response:', 'color: green; font-weight: bold;');
          console.log(data);
        })
        .catch(e => console.error('❌ Request failed:', e));
    } catch (error) {
      console.error('❌ Error:', error);
    }
  }

  /**
   * Display help
   * Usage in console: window.__tokenHelper.help()
   */
  static help(): void {
    const help = `
%c🔐 Token Helper Commands:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

%cwindow.__tokenHelper.inject()           - Inject token from Keycloak
%cwindow.__tokenHelper.show()             - Display current token
%cwindow.__tokenHelper.clear()            - Clear stored token
%cwindow.__tokenHelper.testApi('/path')   - Test API endpoint with token
%cwindow.__tokenHelper.help()             - Show this help

%c━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
%cExample: window.__tokenHelper.testApi('/api/medical-folders')
    `;
    console.log(help, 'color: purple; font-weight: bold; font-size: 14px;',
      'color: blue;',
      'color: blue;',
      'color: blue;',
      'color: blue;',
      'color: blue;',
      'color: purple; font-weight: bold;',
      'color: green;');
  }
}

/**
 * Make TokenHelper available globally in development
 */
declare global {
  interface Window {
    __tokenHelper?: typeof TokenHelper;
  }
}
