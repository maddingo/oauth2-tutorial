'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * Legacy callback page - no longer used with BFF pattern
 *
 * With the BFF pattern, OAuth callbacks are handled by the API route:
 * /api/auth/callback
 *
 * This page is kept for backwards compatibility but will simply redirect to home.
 * The OAuth flow now goes: Authorization Server -> /api/auth/callback -> / (home)
 */
export default function CallbackPage() {
  const router = useRouter();

  useEffect(() => {
    // This page shouldn't be accessed directly in BFF pattern
    // Redirect to home page
    console.log('Legacy callback page accessed, redirecting to home...');
    router.push('/');
  }, [router]);

  return (
    <main className="container">
      <div className="loading">
        <p>Redirecting...</p>
      </div>
    </main>
  );
}
