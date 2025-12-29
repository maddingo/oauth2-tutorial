'use client';

import { useEffect, useState, useRef, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { exchangeCodeForToken, fetchUserInfo, validateState } from '@/lib/oauth';

function CallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setTokens, setUser, isAuthenticated } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const hasProcessed = useRef(false);

  useEffect(() => {
    // Prevent double execution in React Strict Mode
    if (hasProcessed.current) {
      return;
    }

    // Mark as processing to prevent re-entry
    hasProcessed.current = true;

    // Skip if already authenticated (callback already processed)
    if (isAuthenticated) {
      router.push('/');
      return;
    }

    const handleCallback = async () => {
      try {
        const code = searchParams.get('code');
        const state = searchParams.get('state');
        const errorParam = searchParams.get('error');
        const errorDescription = searchParams.get('error_description');

        console.log('OAuth2 Callback received:', {
          hasCode: !!code,
          hasState: !!state,
          hasError: !!errorParam,
        });

        // Check for authorization errors
        if (errorParam) {
          throw new Error(errorDescription || errorParam);
        }

        if (!code) {
          throw new Error('Authorization code not found');
        }

        if (!state) {
          throw new Error('State parameter not found');
        }

        // Validate state to prevent CSRF attacks
        const storedState = sessionStorage.getItem('oauth_state');
        const storedVerifier = sessionStorage.getItem('code_verifier');

        console.log('SessionStorage contents:', {
          hasStoredState: !!storedState,
          hasStoredVerifier: !!storedVerifier,
          receivedState: state,
          storedState: storedState,
        });

        if (!storedState) {
          console.error('No stored state found in sessionStorage');
          throw new Error('No stored state found - session may have expired. Please try logging in again.');
        }

        if (storedState !== state) {
          console.error('State mismatch:', { received: state, stored: storedState });
          throw new Error('Invalid state parameter - possible CSRF attack');
        }

        console.log('State validation successful, exchanging code for token...');

        // Exchange code for tokens
        const tokens = await exchangeCodeForToken(code);
        setTokens(tokens);

        const expiresIn = tokens.expires_in;
        console.log('Token exchange successful:', { expiresIn });

        // Fetch user info
        const userInfo = await fetchUserInfo(tokens.access_token);
        setUser(userInfo);

        // Redirect to home page
        router.push('/');
      } catch (err) {
        console.error('OAuth callback error:', err);
        setError(err instanceof Error ? err.message : 'An error occurred during authentication');
      }
    };

    handleCallback();
  }, [searchParams, setTokens, setUser, router, isAuthenticated]);

  if (error) {
    return (
      <main className="container">
        <h1 className="title">Authentication Error</h1>
        <div className="error">
          <p>{error}</p>
        </div>
        <button className="button" onClick={() => router.push('/')}>
          Return Home
        </button>
      </main>
    );
  }

  return (
    <main className="container">
      <div className="loading">
        <p>Completing authentication...</p>
      </div>
    </main>
  );
}

export default function CallbackPage() {
  return (
    <Suspense fallback={
      <main className="container">
        <div className="loading">
          <p>Loading...</p>
        </div>
      </main>
    }>
      <CallbackContent />
    </Suspense>
  );
}
