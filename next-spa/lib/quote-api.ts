export interface Quote {
    quote:  string;
    author: string;
}

/**
 * Fetch a quote from the resource server via BFF API proxy
 *
 * In the BFF pattern:
 * - Frontend calls /api/quotes (API route)
 * - API route retrieves access token from secure cookie
 * - API route proxies request to resource server with Bearer token
 * - Frontend never sees the access token (security improvement)
 */
export async function getQuote(): Promise<Quote> {
  const response = await fetch('/api/quotes', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    },
    // Include credentials to send cookies
    credentials: 'include',
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ error: 'Failed to fetch quote' }));

    // Check if token needs refresh
    if (errorData.needsRefresh) {
      throw new Error('TOKEN_EXPIRED');
    }

    console.error('Failed to fetch quote:', errorData.error);
    throw new Error(errorData.error || 'Failed to fetch quote');
  }

  return await response.json();
}
